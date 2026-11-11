# Pins and steps

## Pins

| Pin | Kind | Meaning |
| --- | --- | --- |
| `exec_in` / `exec_out` | execution | Order. |
| `exec_true` / `exec_false` | execution | If / else branches. |
| `exec_loop_body` / `exec_completed` | execution | Repeat body, then the done path. |
| `element_out` / `element_in` / `current_element` | element | A live `WebElement`. Never serialized to Redis. |
| `text_out` / `text_in` / `extracted_value` | text | Strings, including `{{variable}}`. |
| `bool_out` | boolean | Condition result. |

Validation rejects execution pins wired to data pins, and element pins wired to text pins.

## Steps that exist

| Type | Studio name | What it does |
| --- | --- | --- |
| `START` | Start browser | Chrome by default, optional Firefox, headless, window size. |
| `NAVIGATE` | Open page | URL, or a text pin. Waits for `document.readyState === complete`. |
| `SEARCH_ELEMENT` | Find element | XPath, CSS, text contains, or regex over the DOM. Explicit wait. |
| `CLICK` | Click | Element pin or XPath. Scrolls into view. JS click if intercepted. |
| `TYPE_TEXT` | Type | Clears, types, optional Enter. Supports `{{variable}}`. |
| `EXTRACT_CONTENT` | Read text | Inner text, attribute, outer HTML, or regex. Can save a variable. |
| `IF_CONDITION` | If / else | Element exists, text contains/equals, or regex. |
| `LOOP_ELEMENTS` | Repeat for each | Re-finds by index each pass so elements do not go stale. |
| `WAIT` | Wait | Fixed ms, URL contains, or a short network-settle heuristic. |
| `ASSERT` | Check | Not empty, equals, contains, regex. Failure fails the blueprint. |

`maxSteps` defaults to 500 so a bad cycle cannot spin forever.

## Demo

Start → open `https://quotes.toscrape.com` → find the first quote → read text into `quoteText` → if it contains `world` → check not empty.

The element pin from Find feeds Read. The text pin from Read feeds If and Check.
