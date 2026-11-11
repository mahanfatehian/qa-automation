EXEC_HANDLES = {
    "exec_in",
    "exec_out",
    "exec_true",
    "exec_false",
    "exec_loop_body",
    "exec_completed",
}

PIN_TYPES = {
    "element_out": "ELEMENT",
    "element_in": "ELEMENT",
    "current_element": "ELEMENT",
    "text_out": "STRING",
    "extracted_value": "STRING",
    "text_in": "STRING",
    "bool_out": "BOOLEAN",
    "bool_in": "BOOLEAN",
}

KNOWN_TYPES = {
    "START",
    "NAVIGATE",
    "SEARCH_ELEMENT",
    "CLICK",
    "TYPE_TEXT",
    "EXTRACT_CONTENT",
    "IF_CONDITION",
    "LOOP_ELEMENTS",
    "WAIT",
    "ASSERT",
    "SCROLL",
    "HOVER",
    "SELECT_OPTION",
    "PRESS_KEY",
    "ENTER_FRAME",
    "LEAVE_FRAME",
    "SWITCH_TAB",
    "ALERT",
    "UPLOAD",
    "GO_BACK",
    "GO_FORWARD",
    "VERIFY",
    "SET_CHECKBOX",
    "DOUBLE_CLICK",
    "RIGHT_CLICK",
    "REFRESH",
    "COOKIE",
    "SCREENSHOT",
    "DRAG_DROP",
}


class GraphError(ValueError):
    def __init__(self, errors: list[str]):
        self.errors = errors
        super().__init__("; ".join(errors))


def semantic_type(node: dict) -> str:
    data = node.get("data") or {}
    return str(data.get("nodeType") or node.get("type") or "")


def validate_graph(graph: dict, *, require_start: bool = True) -> None:
    if not isinstance(graph, dict):
        raise GraphError(["Graph must be an object with nodes and edges"])
    nodes = graph.get("nodes") or []
    edges = graph.get("edges") or []
    errors: list[str] = []
    if not isinstance(nodes, list) or not isinstance(edges, list):
        raise GraphError(["nodes and edges must be lists"])

    ids: list[str] = []
    starts = 0
    for node in nodes:
        if not isinstance(node, dict) or not node.get("id"):
            errors.append("Every step needs an id")
            continue
        node_id = str(node["id"])
        if node_id in ids:
            errors.append(f"Duplicate step id {node_id}")
        ids.append(node_id)
        kind = semantic_type(node)
        if kind not in KNOWN_TYPES:
            errors.append(f"Unknown step type {kind or 'empty'} on {node_id}")
        if kind == "START":
            starts += 1

    if require_start and starts != 1:
        errors.append("A test needs exactly one Start browser step")

    known = set(ids)
    for edge in edges:
        if not isinstance(edge, dict):
            errors.append("An edge is not an object")
            continue
        source = str(edge.get("source") or "")
        target = str(edge.get("target") or "")
        if source not in known or target not in known:
            errors.append(f"Connection {source} -> {target} points at a missing step")
            continue
        source_handle = edge.get("sourceHandle") or "exec_out"
        target_handle = edge.get("targetHandle") or "exec_in"
        source_exec = source_handle in EXEC_HANDLES
        target_exec = target_handle in EXEC_HANDLES
        if source_exec != target_exec:
            errors.append(f"Cannot connect flow pin {source_handle} to data pin {target_handle}")
            continue
        if source_exec:
            continue
        left = PIN_TYPES.get(source_handle)
        right = PIN_TYPES.get(target_handle)
        if left is None or right is None:
            errors.append(f"Unknown data pin {source_handle} -> {target_handle}")
        elif left != right:
            errors.append(f"Incompatible data pins {source_handle} ({left}) -> {target_handle} ({right})")

    max_steps = graph.get("maxSteps", 500)
    try:
        max_steps = int(max_steps)
    except (TypeError, ValueError):
        errors.append("maxSteps must be a number")
    else:
        if max_steps < 1 or max_steps > 5000:
            errors.append("maxSteps must be between 1 and 5000")

    if errors:
        raise GraphError(errors)
