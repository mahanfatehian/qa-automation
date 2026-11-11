# Board

Work board for the final product. Requirements live in [[00 SRS index]] and [[Requirement register]]. Build order lives in [[Development sequence]]. If this board and the register disagree on status, update both in the same change. GitHub shows a short copy in [[TRACKER]].

The app in `main` is a first cut, not the finished product. It has not been booted.

## Status

| Epic | Status | Blocks |
| --- | --- | --- |
| [[01 Prove the stack]] | Not started | everything |
| [[02 Studio you can trust]] | Partial | daily use |
| [[03 Engine correctness]] | Partial | every run |
| [[04 Element picker]] | Not started | no-code authoring |
| [[05 Browser actions]] | Not started | real page tests |
| [[06 Automatic runs and CI]] | Partial | unattended runs |
| [[07 Feedback pack]] | Partial | knowing why a run failed |
| [[08 Public hardening]] | Not started | putting it on the internet |
| [[09 Release gate]] | Not started | calling it finished |

## Order

1. Prove the stack.
2. Studio you can trust, in parallel with engine correctness once boot works.
3. Feedback pack, enough that a failed demo is diagnosable.
4. Element picker.
5. Browser actions, scroll first.
6. Automatic runs proven from a webhook, not only from the modal.
7. Public hardening.
8. Release gate.

Recorder stays out of this order until the picker exists. See [[04 Element picker]].

## Product boundary

- One canvas. Blueprint pins only.
- A step must be a browser-test action a QA engineer can explain in one sentence.
- Cron, interval, and webhook are how a blueprint runs when nobody is at the canvas. They are not a second product.
- See [[Blueprint not n8n]] and [[Direction correction]].
