import unittest

from app.selector import synthesize


class SelectorTests(unittest.TestCase):
    def test_stable_id_does_not_need_html(self) -> None:
        ranked = synthesize("", {"tag": "button", "id": "submit", "path": []})
        self.assertEqual(ranked[0]["kind"], "ID")
        self.assertEqual(ranked[0]["value"], "submit")

    def test_path_css_is_instant_when_the_page_has_no_stable_attr(self) -> None:
        ranked = synthesize(
            "",
            {
                "tag": "button",
                "path": [
                    {"tag": "body", "nth": 1},
                    {"tag": "main", "nth": 1},
                    {"tag": "button", "nth": 2},
                ],
            },
        )
        self.assertTrue(any(item["value"] == "body:nth-of-type(1) > main:nth-of-type(1) > button:nth-of-type(2)" for item in ranked))

    def test_dynamic_id_is_not_trusted(self) -> None:
        ranked = synthesize("", {"tag": "div", "id": "react-123456", "path": []})
        self.assertFalse(any(item["kind"] == "ID" for item in ranked))

    def test_exact_named_element_beats_an_ancestor_testid(self) -> None:
        ranked = synthesize(
            "",
            {
                "tag": "dds-icon",
                "name": "action-delete",
                "path": [
                    {"tag": "div", "nth": 1, "testid": "quantity-box"},
                    {"tag": "dds-icon", "nth": 1, "name": "action-delete"},
                ],
            },
        )
        self.assertEqual(ranked[0]["value"], 'dds-icon[name="action-delete"]')
        self.assertFalse(any(item["value"] == '[data-testid="quantity-box"]' for item in ranked))
