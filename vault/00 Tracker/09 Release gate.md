# 09 Release gate

Status: Not started

Spec: [[15 Final acceptance]]. Register: [[Requirement register]]. Order: step 9 in [[Development sequence]].

## Outcome

The product matches the blueprint that was asked for. A new person can clone it, run it, and understand what is intentionally missing.

## The finished product

- One command, no required `.env`.
- A blueprint canvas with execution pins and data pins.
- Steps a QA engineer can use without writing Selenium: start, open, find, click, type, read, branch, repeat, wait, check, scroll, and the rest of [[05 Browser actions]] that this gate includes.
- An element picker that fills a step from a click.
- The same blueprint runs now, on a schedule, and from a webhook.
- Every run has step status, duration, output, error, and screenshot.
- Selenium Grid is the browser cluster behind the runner.
- Python owns the API and schedules. Java owns the browser execution.
- The vault explains the boundary: not a general automation builder.

## Gate checklist

- [ ] [[01 Prove the stack]] is Done, with the command and result written in that note.
- [ ] [[02 Studio you can trust]] is Done.
- [ ] [[03 Engine correctness]] is Done.
- [ ] [[04 Element picker]] is Done on the quotes page.
- [ ] Scroll, hover, and select from [[05 Browser actions]] are Done. The remaining actions are either Done or explicitly deferred in that note.
- [ ] [[06 Automatic runs and CI]] is Done.
- [ ] [[07 Feedback pack]] is Done.
- [ ] [[08 Public hardening]] is Done for a public repo and a local deploy. A hosted SaaS is not required for this gate.
- [ ] README matches what the commands actually do.
- [ ] `vault/00 Tracker/00 Board.md` statuses match the notes.

## Done when

Every box above is checked, and a person who was not in the build can follow the README to a passing demo without asking for a missing step.

## Depends on

All earlier epics.

## Not in this task

New product ideas. If it is not in the epics above, it does not enter the gate through this note. Add a new epic first.
