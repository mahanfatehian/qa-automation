---
type: srs
status: baseline
---

# SRS index

BlueprintQA Software Requirements Specification, version 1.0. This is the requirement baseline for the final product. [[00 Board]] is the work board. If a task is not traced to a requirement here, it is not in the final product until this index is updated.

## How the vault connects

- [[Start here]] is the front door.
- This note is the requirement map.
- [[Requirement register]] is the checklist of every ID.
- [[Development sequence]] is the only allowed build order.
- Each epic in `00 Tracker` implements a slice of these requirements.
- [[Map.canvas]] is the same map as a canvas.

## Document control

| Field | Value |
| --- | --- |
| Product | BlueprintQA |
| Version | 1.0 |
| Status | Baseline for final-product development |
| Date | 2026-11-11 |
| Owner | mahanfatehian |
| Repo | https://github.com/mahanfatehian/qa-automation |

## Read in this order

1. [[01 Introduction]]
2. [[02 Definitions]]
3. [[03 Product scope]]
4. [[04 Users and scenarios]]
5. [[05 System requirements]]
6. [[06 Blueprint requirements]]
7. [[07 Engine requirements]]
8. [[08 Picker requirements]]
9. [[09 Action requirements]]
10. [[10 Automation requirements]]
11. [[11 Feedback requirements]]
12. [[12 Quality and security]]
12b. [[Speed and feel]] is the priority constraint. Read it before building [[Element selection engine]].
13. [[13 Interfaces and data]]
14. [[14 Out of scope]]
15. [[15 Final acceptance]]
16. [[Requirement register]]
17. [[Development sequence]]

## Requirement rules

- A requirement uses shall.
- Each ID is stable. Do not renumber. Add a new ID to retire a meaning.
- Status in the register is one of: Not started, Partial, Not proven, Done.
- Partial means code exists. Not proven means it has not been shown on a running stack.
- Done is only set when the acceptance line is true on a running stack.
