import unittest

from app.demo import demo_graph, starter_graph
from app.validation import GraphError, validate_graph


class ValidationTests(unittest.TestCase):
    def test_demo_and_starter_are_valid(self) -> None:
        validate_graph(demo_graph())
        validate_graph(starter_graph())

    def test_rejects_missing_start(self) -> None:
        with self.assertRaises(GraphError):
            validate_graph({"nodes": [], "edges": []})

    def test_rejects_incompatible_pins(self) -> None:
        graph = starter_graph()
        graph["nodes"].append(
            {
                "id": "find",
                "type": "SEARCH_ELEMENT",
                "data": {"nodeType": "SEARCH_ELEMENT"},
            }
        )
        graph["edges"].append(
            {
                "source": "find",
                "target": "start",
                "sourceHandle": "element_out",
                "targetHandle": "exec_in",
            }
        )
        with self.assertRaises(GraphError):
            validate_graph(graph)


if __name__ == "__main__":
    unittest.main()
