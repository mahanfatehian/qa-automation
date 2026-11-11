# BlueprintQA tracker

The product is not finished. The requirement baseline is `vault/07 SRS/`. The build order is `vault/07 SRS/Development sequence.md`. The work notes are in `vault/00 Tracker/`.

Open `vault/` as an Obsidian vault. Start at `Start here.md`. The canvas map is `vault/Map.canvas`.

Rule for every task: stay a blueprint. White pins order steps. Colored pins pass an element, text, or boolean. Do not turn this into a general automation builder.

## Order

Do these in order. Do not start 04 until 01 has a passing demo.

| # | Task | Status | Note |
| --- | --- | --- | --- |
| 01 | [Prove the stack](vault/00%20Tracker/01%20Prove%20the%20stack.md) | Not started | Code exists. Nothing has been booted or proven. |
| 02 | [Studio you can trust](vault/00%20Tracker/02%20Studio%20you%20can%20trust.md) | Partial | Canvas, save, run, and schedule UI exist. History and failure paths do not. |
| 03 | [Engine correctness](vault/00%20Tracker/03%20Engine%20correctness.md) | Partial | Core steps are coded. Edge cases and a real demo pass are not proven. |
| 04 | [Element picker](vault/00%20Tracker/04%20Element%20picker.md) | Not started | Probe-by-typed-selector only. No click-to-pick. |
| 05 | [Browser actions](vault/00%20Tracker/05%20Browser%20actions.md) | Not started | Scroll, hover, select, keys, frames, tabs, alerts, upload are missing. |
| 06 | [Automatic runs and CI](vault/00%20Tracker/06%20Automatic%20runs%20and%20CI.md) | Partial | Cron, interval, webhook, and JUnit exist. Not proven from a real pipeline. |
| 07 | [Feedback pack](vault/00%20Tracker/07%20Feedback%20pack.md) | Partial | Live color and screenshots exist. History replay and failure context are thin. |
| 08 | [Public hardening](vault/00%20Tracker/08%20Public%20hardening.md) | Not started | Local defaults only. No auth, no migrations, no test suite. |
| 09 | [Release gate](vault/00%20Tracker/09%20Release%20gate.md) | Not started | The definition of the finished product. |

## Done means

A QA engineer clones the repo, runs one command, draws or loads a blueprint, runs it on the grid, sees which step failed and the screenshot, and can schedule that same blueprint. They do not write Selenium code. They can pick an element instead of hand-writing every XPath.

## How to update

When a checkbox in an epic note is finished, mark it `[x]` in that note and change the status word on this board. Do not mark an epic Done because the code was written. Mark it Done when the "Done when" section of that note is true.
