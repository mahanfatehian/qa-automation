def starter_graph() -> dict:
    return {
        "maxSteps": 500,
        "variables": {},
        "nodes": [
            {
                "id": "start",
                "type": "START",
                "position": {"x": 80, "y": 200},
                "data": {
                    "nodeType": "START",
                    "title": "Start browser",
                    "browser": "chrome",
                    "headless": False,
                    "windowWidth": 1280,
                    "windowHeight": 800,
                },
            }
        ],
        "edges": [],
    }


def demo_graph() -> dict:
    nodes = [
        {
            "id": "start",
            "type": "START",
            "position": {"x": 40, "y": 220},
            "data": {
                "nodeType": "START",
                "title": "Start browser",
                "browser": "chrome",
                "headless": False,
                "windowWidth": 1280,
                "windowHeight": 800,
            },
        },
        {
            "id": "open",
            "type": "NAVIGATE",
            "position": {"x": 340, "y": 220},
            "data": {
                "nodeType": "NAVIGATE",
                "title": "Open page",
                "url": "http://control-plane:8000/fixtures/lab.html",
                "timeoutSeconds": 20,
            },
        },
        {
            "id": "find",
            "type": "SEARCH_ELEMENT",
            "position": {"x": 640, "y": 220},
            "data": {
                "nodeType": "SEARCH_ELEMENT",
                "title": "Find element",
                "strategy": "CSS",
                "query": "[data-testid='intro']",
                "timeoutSeconds": 12,
            },
        },
        {
            "id": "read",
            "type": "EXTRACT_CONTENT",
            "position": {"x": 960, "y": 220},
            "data": {
                "nodeType": "EXTRACT_CONTENT",
                "title": "Read text",
                "extractType": "INNER_TEXT",
                "saveAsVariable": "quoteText",
            },
        },
        {
            "id": "branch",
            "type": "IF_CONDITION",
            "position": {"x": 1280, "y": 220},
            "data": {
                "nodeType": "IF_CONDITION",
                "title": "If / else",
                "conditionType": "TEXT_CONTAINS",
                "rightValue": "ready",
            },
        },
        {
            "id": "check",
            "type": "ASSERT",
            "position": {"x": 1600, "y": 80},
            "data": {
                "nodeType": "ASSERT",
                "title": "Check",
                "operator": "NOT_EMPTY",
                "expected": "",
            },
        },
    ]
    edges = [
        _exec("e1", "start", "open"),
        _exec("e2", "open", "find"),
        _exec("e3", "find", "read"),
        _exec("e4", "read", "branch"),
        _exec("e5", "branch", "check", "exec_true", "exec_in"),
        _data("d1", "find", "read", "element_out", "element_in"),
        _data("d2", "read", "branch", "text_out", "text_in"),
        _data("d3", "read", "check", "text_out", "text_in"),
    ]
    return {"maxSteps": 500, "variables": {}, "nodes": nodes, "edges": edges}


def _exec(edge_id: str, source: str, target: str, source_handle: str = "exec_out", target_handle: str = "exec_in") -> dict:
    return {
        "id": edge_id,
        "source": source,
        "target": target,
        "sourceHandle": source_handle,
        "targetHandle": target_handle,
        "type": "EXEC",
    }


def _data(edge_id: str, source: str, target: str, source_handle: str, target_handle: str) -> dict:
    return {
        "id": edge_id,
        "source": source,
        "target": target,
        "sourceHandle": source_handle,
        "targetHandle": target_handle,
        "type": "DATA",
    }
