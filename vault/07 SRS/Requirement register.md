---
type: register
---

# Requirement register

Part of [[00 SRS index]]. Update the status column when the acceptance line is proven. Do not mark Done from a code read.

Status key: Not started, Partial, Not proven, Done.

| ID | Shall | Status | Epic | Spec |
| --- | --- | --- | --- | --- |
| FR-SYS-01 | One command, no required `.env` | Not proven | [[01 Prove the stack]] | [[05 System requirements]] |
| FR-SYS-02 | Studio, API, and grid ports | Not proven | [[01 Prove the stack]] | [[05 System requirements]] |
| FR-SYS-03 | Health checks | Partial | [[01 Prove the stack]] | [[05 System requirements]] |
| FR-SYS-04 | Chrome cluster, optional Firefox | Partial | [[01 Prove the stack]] | [[05 System requirements]] |
| FR-SYS-05 | Scale Chrome nodes | Not proven | [[01 Prove the stack]] | [[05 System requirements]] |
| FR-SYS-06 | Seed quotes demo | Partial | [[01 Prove the stack]] | [[05 System requirements]] |
| FR-SYS-07 | One log command | Partial | [[01 Prove the stack]] | [[05 System requirements]] |
| FR-SYS-08 | Only the runner opens browsers | Partial | [[01 Prove the stack]] | [[05 System requirements]] |
| FR-BP-01 | Blueprint canvas | Partial | [[02 Studio you can trust]] | [[06 Blueprint requirements]] |
| FR-BP-02 | Execution pins and rejection | Partial | [[02 Studio you can trust]] | [[06 Blueprint requirements]] |
| FR-BP-03 | Data pins, no live element stored | Partial | [[02 Studio you can trust]] | [[06 Blueprint requirements]] |
| FR-BP-04 | Exactly one Start | Partial | [[02 Studio you can trust]] | [[06 Blueprint requirements]] |
| FR-BP-05 | Palette drop | Partial | [[02 Studio you can trust]] | [[06 Blueprint requirements]] |
| FR-BP-06 | Inspector fields | Partial | [[02 Studio you can trust]] | [[06 Blueprint requirements]] |
| FR-BP-07 | Save and reload fidelity | Not proven | [[02 Studio you can trust]] | [[06 Blueprint requirements]] |
| FR-BP-08 | Dirty guard | Not started | [[02 Studio you can trust]] | [[06 Blueprint requirements]] |
| FR-BP-09 | Delete blueprint | Not started | [[02 Studio you can trust]] | [[06 Blueprint requirements]] |
| FR-BP-10 | Quotes demo | Partial | [[02 Studio you can trust]] | [[06 Blueprint requirements]] |
| FR-BP-11 | Errors, Ctrl+S, Ctrl+Enter | Not started | [[02 Studio you can trust]] | [[06 Blueprint requirements]] |
| FR-BP-12 | Grid pill | Partial | [[02 Studio you can trust]] | [[06 Blueprint requirements]] |
| FR-ENG-01 | Queued graph snapshot | Partial | [[03 Engine correctness]] | [[07 Engine requirements]] |
| FR-ENG-02 | Pin walk and branches | Partial | [[03 Engine correctness]] | [[07 Engine requirements]] |
| FR-ENG-03 | Data pins into later steps | Partial | [[03 Engine correctness]] | [[07 Engine requirements]] |
| FR-ENG-04 | Variables and loop index | Partial | [[03 Engine correctness]] | [[07 Engine requirements]] |
| FR-ENG-05 | Find strategies and explicit wait | Partial | [[03 Engine correctness]] | [[07 Engine requirements]] |
| FR-ENG-06 | Click and type behavior | Partial | [[03 Engine correctness]] | [[07 Engine requirements]] |
| FR-ENG-07 | Wait modes | Partial | [[03 Engine correctness]] | [[07 Engine requirements]] |
| FR-ENG-08 | Check failure | Partial | [[03 Engine correctness]] | [[07 Engine requirements]] |
| FR-ENG-09 | Max step guard | Not proven | [[03 Engine correctness]] | [[07 Engine requirements]] |
| FR-ENG-10 | Session retry and quit | Partial | [[03 Engine correctness]] | [[07 Engine requirements]] |
| FR-ENG-11 | One browser per run | Not proven | [[03 Engine correctness]] | [[07 Engine requirements]] |
| FR-ENG-12 | Loop re-find by index | Not proven | [[03 Engine correctness]] | [[07 Engine requirements]] |
| FR-PICK-01 | Pick entry on four steps | Not started | [[04 Element picker]] | [[08 Picker requirements]] |
| FR-PICK-02 | Short-lived picker session | Not started | [[04 Element picker]] | [[08 Picker requirements]] |
| FR-PICK-03 | Ranked selectors | Not started | [[04 Element picker]] | [[08 Picker requirements]] |
| FR-PICK-04 | Write selector into the step | Not started | [[04 Element picker]] | [[08 Picker requirements]] |
| FR-PICK-05 | Typed probe remains | Partial | [[04 Element picker]] | [[08 Picker requirements]] |
| FR-PICK-06 | Picker failure writes nothing | Not started | [[04 Element picker]] | [[08 Picker requirements]] |
| FR-PICK-07 | Quotes pick then pass | Not started | [[04 Element picker]] | [[08 Picker requirements]] |
| FR-ACT-01 | Scroll | Not started | [[05 Browser actions]] | [[09 Action requirements]] |
| FR-ACT-02 | Hover | Not started | [[05 Browser actions]] | [[09 Action requirements]] |
| FR-ACT-03 | Select | Not started | [[05 Browser actions]] | [[09 Action requirements]] |
| FR-ACT-04 | Press key | Not started | [[05 Browser actions]] | [[09 Action requirements]] |
| FR-ACT-05 | Frame enter and leave | Not started | [[05 Browser actions]] | [[09 Action requirements]] |
| FR-ACT-06 | Switch tab | Not started | [[05 Browser actions]] | [[09 Action requirements]] |
| FR-ACT-07 | Alert | Not started | [[05 Browser actions]] | [[09 Action requirements]] |
| FR-ACT-08 | Upload | Not started | [[05 Browser actions]] | [[09 Action requirements]] |
| FR-ACT-09 | Go back | Not started | [[05 Browser actions]] | [[09 Action requirements]] |
| FR-ACT-10 | Palette matches handlers | Partial | [[05 Browser actions]] | [[09 Action requirements]] |
| FR-AUTO-01 | Manual run saves first | Partial | [[06 Automatic runs and CI]] | [[10 Automation requirements]] |
| FR-AUTO-02 | Cron, restored on restart | Partial | [[06 Automatic runs and CI]] | [[10 Automation requirements]] |
| FR-AUTO-03 | Interval minimum 15s | Partial | [[06 Automatic runs and CI]] | [[10 Automation requirements]] |
| FR-AUTO-04 | Pause and delete schedule | Partial | [[06 Automatic runs and CI]] | [[10 Automation requirements]] |
| FR-AUTO-05 | Schedule overlap skip | Partial | [[06 Automatic runs and CI]] | [[10 Automation requirements]] |
| FR-AUTO-06 | Webhook token and 401 | Not proven | [[06 Automatic runs and CI]] | [[10 Automation requirements]] |
| FR-AUTO-07 | wait=true status codes | Not proven | [[06 Automatic runs and CI]] | [[10 Automation requirements]] |
| FR-AUTO-08 | JUnit XML | Partial | [[06 Automatic runs and CI]] | [[10 Automation requirements]] |
| FR-AUTO-09 | Last fire time in the modal | Not started | [[06 Automatic runs and CI]] | [[10 Automation requirements]] |
| FR-AUTO-10 | CI snippet in the README | Not started | [[06 Automatic runs and CI]] | [[10 Automation requirements]] |
| FR-FB-01 | Step events | Partial | [[07 Feedback pack]] | [[11 Feedback requirements]] |
| FR-FB-02 | Run events, no stuck QUEUED | Not proven | [[07 Feedback pack]] | [[11 Feedback requirements]] |
| FR-FB-03 | Screenshot stored and fetched | Partial | [[07 Feedback pack]] | [[11 Feedback requirements]] |
| FR-FB-04 | Live step color | Partial | [[07 Feedback pack]] | [[11 Feedback requirements]] |
| FR-FB-05 | Drawer with URL and image fallback | Not started | [[07 Feedback pack]] | [[11 Feedback requirements]] |
| FR-FB-06 | Socket replay and past run | Partial | [[07 Feedback pack]] | [[11 Feedback requirements]] |
| FR-FB-07 | Safe telemetry | Partial | [[07 Feedback pack]] | [[11 Feedback requirements]] |
| FR-FB-08 | Retention documented | Not started | [[07 Feedback pack]] | [[11 Feedback requirements]] |
| NFR-01 | Second start works | Not proven | [[01 Prove the stack]] | [[12 Quality and security]] |
| NFR-02 | Windows `up.bat` | Not proven | [[01 Prove the stack]] | [[12 Quality and security]] |
| NFR-03 | Run isolation | Not proven | [[03 Engine correctness]] | [[12 Quality and security]] |
| NFR-04 | Specific errors | Partial | [[08 Public hardening]] | [[12 Quality and security]] |
| NFR-05 | Local fixture | Not started | [[03 Engine correctness]] | [[12 Quality and security]] |
| NFR-06 | No tokens in logs | Not proven | [[08 Public hardening]] | [[12 Quality and security]] |
| FR-SEC-01 | Local-only warning | Partial | [[08 Public hardening]] | [[12 Quality and security]] |
| FR-SEC-02 | Gate before non-local deploy | Not started | [[08 Public hardening]] | [[12 Quality and security]] |
| FR-SEC-03 | Probe and pick via control plane | Partial | [[08 Public hardening]] | [[12 Quality and security]] |
| FR-SEC-04 | Safe token compare | Not started | [[08 Public hardening]] | [[12 Quality and security]] |
| FR-SEC-05 | Migrations after first boot | Not started | [[08 Public hardening]] | [[12 Quality and security]] |
| FR-SEC-06 | Host tests without Docker | Partial | [[08 Public hardening]] | [[12 Quality and security]] |
