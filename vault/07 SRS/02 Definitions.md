---
type: srs
---

# 02 Definitions

Part of [[00 SRS index]].

| Term | Meaning |
| --- | --- |
| Blueprint | One saved test graph: nodes, edges, max steps, variables. |
| Step | One node. It has a type, fields, and pins. |
| Execution pin | White diamond pin. `exec_in`, `exec_out`, `exec_true`, `exec_false`, `exec_loop_body`, `exec_completed`. Orders work. |
| Data pin | Colored round pin. Passes an element, text, or boolean. Does not order work. |
| Element pin | `element_in`, `element_out`, `current_element`. A live browser element. Not stored in the database. |
| Run | One execution of one blueprint. Status is QUEUED, RUNNING, PASSED, FAILED, or ERROR. |
| Trigger | Why the run started: MANUAL, CRON, INTERVAL, or WEBHOOK. |
| Grid | Selenium Grid 4 hub plus browser nodes. |
| Control plane | The Python FastAPI service. |
| Runner | The Java 21 service that drives the browser. |
| Studio | The React blueprint canvas. |
| Picker | The tool that fills a step's selector from a click on the page. It is not a second editor. |
| Probe | A one-shot check of a selector the user already typed. |
| Fixture | A local HTML page used to prove steps without depending on a public site. |
| Final product | The state described in [[15 Final acceptance]]. Not the current `main` branch. |

## Pin colors

| Kind | Color | Handles |
| --- | --- | --- |
| Execution | white | `exec_*` |
| Element | green | `element_in`, `element_out`, `current_element` |
| Text | blue | `text_in`, `text_out`, `extracted_value` |
| Boolean | amber | `bool_in`, `bool_out` |

## Step types in the final product

Start, Open page, Find, Click, Type, Read, If / else, Repeat for each, Wait, Check, Scroll, Hover, Select, Press key, Enter frame, Leave frame, Switch tab, Alert, Upload, Go back.

Names in the studio are the plain names. Type IDs in JSON stay the codes in [[13 Interfaces and data]].
