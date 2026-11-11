---
type: srs
epic: "[[09 Release gate]]"
---

# 15 Final acceptance

Part of [[00 SRS index]]. This is the definition of the finished product. [[09 Release gate]] cannot be marked Done until every line here is true.

## The release candidate

1. A new clone on Windows starts with `up.bat` and no `.env`.
2. The studio, API docs, and grid console open on the documented ports.
3. The quotes demo runs on Chrome and ends PASSED, or a local fixture demo runs PASSED if the public site is unreachable and that fallback is documented.
4. A forced bad selector ends FAILED on that step, with URL, error, and screenshot, and leaves no grid session behind.
5. The user picks an element on the quotes page or the fixture and the filled step passes.
6. Scroll, Hover, and Select pass on the fixture. Every other action in [[09 Action requirements]] is either passed or explicitly deferred by editing that note and this list together.
7. A schedule fires once and the run appears in the studio.
8. A webhook with `wait=true` returns 200 or 422 matching the run, and a bad token returns 401.
9. JUnit XML for that run opens and names the steps.
10. Reopening the run after a browser refresh still shows steps and screenshots.
11. README warnings match the security behavior. Local defaults still boot.
12. [[Requirement register]] has no Must row left in Not started or Partial.
13. [[00 Board]] statuses match the notes.

## Must versus later-in-this-product

Every FR and NFR in this SRS is Must for the final product except:

- FR-ACT-05 through FR-ACT-09 may be deferred only by a written line in [[09 Action requirements]] and a matching line in [[09 Release gate]]. Scroll, Hover, and Select may not be deferred.
- FR-SEC-02 may be satisfied by a documented off-by-default gate, as long as the README forbids a raw public deploy.
- Video, recorder, and pixel diff are out of scope, not deferred musts.

## Not acceptance

- The code existing on `main`.
- A compose file that has not been booted.
- A palette name with no handler.
- A passing unit test that never opened a browser, used alone, for a step that claims grid behavior.
