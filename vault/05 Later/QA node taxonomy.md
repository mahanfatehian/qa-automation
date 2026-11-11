---
type: consideration
status: active
---

# QA node taxonomy

Needed consideration for the final product. A blueprint is not only a procedure chain. A QA automation graph needs four kinds of steps, and each kind needs the configurations Selenium already has, without making the user write code.

This is not a later idea that can be dropped. [[09 Action requirements]] and [[07 Engine requirements]] cover part of it. This note is the full list so development does not stop at Click and Check.

## 1. Search, not only XPath

Every step that looks at the page shall be able to find by:

- XPath
- CSS
- id
- name
- link text and partial link text
- tag name
- visible text contains
- regex over visible DOM text

The Find step stores the element on the green pin so later steps do not search again. Later steps may still search themselves when no pin is connected.

## 2. Inputs, not only Type

A tester fills forms. The graph needs:

- Type text, clear first, press Enter, `{{variable}}`
- Dropdown select by visible text or value
- Checkbox and radio set checked or unchecked
- File upload
- Press key
- Double click, right click, hover
- Drag and drop from one element to another

## 3. Verify and assert, not only a text check

A test is not done because the click happened. The graph needs checks that fail the run:

- Text equals, contains, regex, not empty. This is the Check step.
- Element is visible
- Element is enabled
- Element is selected
- Attribute equals
- Element count equals
- URL equals or contains
- Title contains

Those element and page checks are the Verify step. Verify is not a procedure. It is a gate. It has a boolean pin so If / else can branch on it, and it fails the run when the check is false.

## 4. Session configuration

Start already chooses browser, headless, and window size. The graph also needs:

- Cookies: set, read, delete
- Refresh
- Back, and later forward
- Frame enter and leave
- Tab switch
- Alert accept or dismiss
- An explicit screenshot step, in addition to the automatic shot after every step

## Rule

If Selenium can do it as a normal browser assertion or input, it belongs as a blueprint step with fields, not as a code box. Do not add Slack, email, or generic HTTP nodes to satisfy this list.

The palette groups these as Given, When, and Then. That is the procedure shape, not a Cucumber runtime. See [[Given When Then]].

See [[Development sequence]] step 6 and [[Requirement register]].
