import logging
from uuid import UUID

from apscheduler.schedulers.asyncio import AsyncIOScheduler
from apscheduler.triggers.cron import CronTrigger
from apscheduler.triggers.interval import IntervalTrigger
from croniter import croniter
from sqlalchemy import select

from app.database import SessionLocal
from app.models import Workflow, WorkflowSchedule, utcnow
from app.redis_bus import BusyError, enqueue_run

logger = logging.getLogger(__name__)
scheduler = AsyncIOScheduler(timezone="UTC")


def validate_schedule(schedule_type: str, cron_expression: str | None, interval_seconds: int | None) -> None:
    if schedule_type == "CRON":
        if not cron_expression or not croniter.is_valid(cron_expression):
            raise ValueError("Use a 5-field cron expression, for example 0 */6 * * *")
        return
    if schedule_type == "INTERVAL":
        if not interval_seconds or interval_seconds < 15:
            raise ValueError("Interval must be at least 15 seconds")
        return
    raise ValueError("schedule_type must be CRON or INTERVAL")


def add_job(row: WorkflowSchedule) -> None:
    job_id = str(row.id)
    existing = scheduler.get_job(job_id)
    if not row.enabled:
        if existing:
            scheduler.remove_job(job_id)
        return
    if row.schedule_type == "CRON":
        trigger = CronTrigger.from_crontab(row.cron_expression, timezone="UTC")
    else:
        trigger = IntervalTrigger(seconds=int(row.interval_seconds or 3600), timezone="UTC")
    scheduler.add_job(
        fire_schedule,
        trigger,
        id=job_id,
        args=[str(row.workflow_id), job_id],
        replace_existing=True,
        misfire_grace_time=120,
        max_instances=1,
        coalesce=True,
    )


def remove_job(schedule_id: str) -> None:
    if scheduler.get_job(schedule_id):
        scheduler.remove_job(schedule_id)


async def fire_schedule(workflow_id: str, schedule_id: str) -> None:
    async with SessionLocal() as session:
        workflow = await session.get(Workflow, UUID(workflow_id))
        row = await session.get(WorkflowSchedule, UUID(schedule_id))
        if workflow is None or row is None or not row.enabled:
            return
        try:
            await enqueue_run(session, workflow, row.schedule_type)
        except BusyError:
            logger.info("skipped overlapping schedule %s", schedule_id)
            return
        row.last_run_at = utcnow()
        await session.commit()


async def start() -> None:
    if not scheduler.running:
        scheduler.start()
    async with SessionLocal() as session:
        rows = (await session.execute(select(WorkflowSchedule).where(WorkflowSchedule.enabled.is_(True)))).scalars().all()
    for row in rows:
        try:
            add_job(row)
        except Exception:
            logger.exception("could not restore schedule %s", row.id)


def shutdown() -> None:
    if scheduler.running:
        scheduler.shutdown(wait=False)
