# 01 Prove the stack

Status: Not started

Spec: [[05 System requirements]]. Register: [[Requirement register]]. Order: step 1 in [[Development sequence]].

## Outcome

A clean machine can clone the repo, start the stack with one command and no `.env`, open the studio, run the quotes demo, and see a pass or a diagnosed fail.

Until this is true, no later epic is "done."

## Why this is first

The control plane, runner, studio, and compose file are written. None of them have been built or run together in this repo. A green file tree is not a product.

## Current evidence

- `docker-compose.yml` defines Postgres, Redis, Selenium Hub, Chrome nodes, control plane, runner, and frontend.
- `up.bat` and `make up` are the intended commands.
- Java has not been compiled here.
- The frontend has not been typechecked or built here.
- The demo blueprint has never been executed on the grid.

## Work

- [ ] `docker compose up --build -d --scale chrome-node=2` completes on this Windows machine with no `.env`.
- [ ] Control plane `/health` returns ok.
- [ ] Runner `/health` returns UP.
- [ ] Grid `http://localhost:4444/status` shows at least one Chrome node.
- [ ] Studio loads at `http://localhost:3000` and lists the seeded quotes blueprint.
- [ ] Java image builds. Fix compile errors. Do not stub the handlers.
- [ ] Frontend image builds. Fix TypeScript errors in the canvas and save payload.
- [ ] Run the seeded demo from the studio.
- [ ] Record the result: pass, or the exact failing step, URL, and screenshot.
- [ ] If the demo fails because of a product bug, fix it in this epic. If the public site is down, record that and use a local HTML fixture instead.
- [ ] Confirm `docker compose down` and `up.bat` again still come up without hand edits.

## Done when

One command starts the stack, the studio opens, and the demo blueprint finishes with a visible pass or a visible diagnosed fail. The command and the result are written at the bottom of this note.

## Depends on

Nothing.

## Not in this task

New steps, the picker, auth, or visual redesign.
