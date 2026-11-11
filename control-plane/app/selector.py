import re

from bs4 import BeautifulSoup, Tag

_DYNAMIC_ID = re.compile(r"(\d{4,}|^:|ember|react|__|.{25,})", re.I)


def synthesize(html: str, fingerprint: dict) -> list[dict]:
    soup = BeautifulSoup(html or "", "html.parser")
    node = _locate(soup, fingerprint.get("path") or []) or _locate_attrs(soup, fingerprint)
    if node is None:
        return _from_fingerprint(fingerprint)
    ranked: list[dict] = []
    seen: set[str] = set()

    def add(kind: str, value: str, rank: int) -> None:
        key = f"{kind}:{value}"
        if not value or key in seen:
            return
        seen.add(key)
        ranked.append({"kind": kind, "value": value, "rank": rank})

    tag = node.name or fingerprint.get("tag") or "*"
    testid_attr, testid = _testid(node)
    if testid and _unique(soup, f'[{testid_attr}="{_q(testid)}"]'):
        add("CSS", f'[{testid_attr}="{_q(testid)}"]', 1)
    role = _attr(node, "role")
    aria = _attr(node, "aria-label")
    if role and aria and _unique(soup, f'{tag}[role="{_q(role)}"][aria-label="{_q(aria)}"]'):
        add("CSS", f'{tag}[role="{_q(role)}"][aria-label="{_q(aria)}"]', 2)
    name = _attr(node, "name")
    if name and _unique(soup, f'{tag}[name="{_q(name)}"]'):
        add("NAME" if _unique(soup, f'[name="{_q(name)}"]') else "CSS", name if _unique(soup, f'[name="{_q(name)}"]') else f'{tag}[name="{_q(name)}"]', 2)
    if aria and _unique(soup, f'{tag}[aria-label="{_q(aria)}"]'):
        add("CSS", f'{tag}[aria-label="{_q(aria)}"]', 3)
    placeholder = _attr(node, "placeholder")
    if placeholder and _unique(soup, f'{tag}[placeholder="{_q(placeholder)}"]'):
        add("CSS", f'{tag}[placeholder="{_q(placeholder)}"]', 4)
    href = _attr(node, "href")
    if tag == "a" and href and len(href) < 180 and _unique(soup, f'a[href="{_q(href)}"]'):
        add("CSS", f'a[href="{_q(href)}"]', 5)
    element_id = _attr(node, "id")
    if _stable_id(element_id) and _unique(soup, f'[id="{_q(element_id)}"]'):
        add("ID", element_id, 6)
    own = " ".join(node.find_all(string=True, recursive=False)).strip()
    own = re.sub(r"\s+", " ", own)[:80]
    if 2 <= len(own) <= 60 and _text_unique(soup, own):
        add("TEXT_CONTAINS", own, 7)
    short = _short_css(soup, node)
    if short:
        add("CSS", short, 8)
    xpath = _xpath(node)
    if xpath:
        add("XPATH", xpath, 9)
    return ranked


def _locate(soup: BeautifulSoup, path: list) -> Tag | None:
    if not path:
        return None
    current: Tag | BeautifulSoup = soup.find("html") or soup
    for part in path:
        if not isinstance(part, dict):
            return None
        tag = str(part.get("tag") or "").lower()
        nth = int(part.get("nth") or 1)
        if not tag or not isinstance(current, Tag) and current is not soup:
            return None
        matches = [child for child in current.find_all(tag, recursive=False) if isinstance(child, Tag)]
        if nth < 1 or nth > len(matches):
            return None
        current = matches[nth - 1]
    return current if isinstance(current, Tag) else None


def _locate_attrs(soup: BeautifulSoup, fingerprint: dict) -> Tag | None:
    tag = str(fingerprint.get("tag") or "").lower()
    if not tag:
        return None
    candidates = soup.find_all(tag)
    for key, attr in (("id", "id"), ("name", "name"), ("testid", "data-testid")):
        value = str(fingerprint.get(key) or "")
        if not value:
            continue
        found = [node for node in candidates if node.get(attr) == value]
        if len(found) == 1:
            return found[0]
    text = re.sub(r"\s+", " ", str(fingerprint.get("text") or "")).strip()
    if text:
        found = [node for node in candidates if text in re.sub(r"\s+", " ", node.get_text(" ", strip=True))]
        if len(found) == 1:
            return found[0]
    return None


def _short_css(soup: BeautifulSoup, node: Tag) -> str:
    parts: list[str] = []
    current: Tag | None = node
    while isinstance(current, Tag) and current.name not in {None, "[document]", "html"}:
        anchor = _anchor(current)
        if anchor and current is not node:
            selector = " > ".join([anchor, *reversed(parts)])
            return selector if _unique(soup, selector) else ""
        parts.append(f"{current.name}:nth-of-type({_nth(current)})")
        current = current.parent if isinstance(current.parent, Tag) else None
    selector = " > ".join(reversed(parts))
    return selector if selector and _unique(soup, selector) else ""


def _testid(node: Tag) -> tuple[str, str]:
    for attr in ("data-testid", "data-test", "data-cy", "data-qa"):
        value = _attr(node, attr)
        if value:
            return attr, value
    return "", ""


def _anchor(node: Tag) -> str:
    testid_attr, testid = _testid(node)
    if testid:
        return f'[{testid_attr}="{_q(testid)}"]'
    element_id = _attr(node, "id")
    if _stable_id(element_id):
        return f'[id="{_q(element_id)}"]'
    name = _attr(node, "name")
    if name:
        return f'{node.name}[name="{_q(name)}"]'
    return ""


def _xpath(node: Tag) -> str:
    parts: list[str] = []
    current: Tag | None = node
    while isinstance(current, Tag) and current.name not in {None, "[document]"}:
        parts.append(f"{current.name}[{_nth(current)}]")
        current = current.parent if isinstance(current.parent, Tag) else None
    return "/" + "/".join(reversed(parts)) if parts else ""


def _nth(node: Tag) -> int:
    parent = node.parent
    if not isinstance(parent, Tag):
        return 1
    same = [child for child in parent.find_all(node.name, recursive=False) if isinstance(child, Tag)]
    return same.index(node) + 1 if node in same else 1


def _text_unique(soup: BeautifulSoup, text: str) -> bool:
    needle = text.lower()
    found = [node for node in soup.find_all(True) if needle in re.sub(r"\s+", " ", node.get_text(" ", strip=True)).lower()]
    return len(found) == 1


def _unique(soup: BeautifulSoup, selector: str) -> bool:
    try:
        return len(soup.select(selector)) == 1
    except Exception:
        return False


def _from_fingerprint(fingerprint: dict) -> list[dict]:
    ranked: list[dict] = []
    tag = str(fingerprint.get("tag") or "*").lower() or "*"
    testid = str(fingerprint.get("testid") or "")
    name = str(fingerprint.get("name") or "")
    aria = str(fingerprint.get("aria") or "")
    placeholder = str(fingerprint.get("placeholder") or "")
    element_id = str(fingerprint.get("id") or "")
    if testid:
        ranked.append({"kind": "CSS", "value": f'[data-testid="{_q(testid)}"]', "rank": 1})
    if name:
        ranked.append({"kind": "CSS", "value": f'{tag}[name="{_q(name)}"]', "rank": 2})
    if aria:
        ranked.append({"kind": "CSS", "value": f'{tag}[aria-label="{_q(aria)}"]', "rank": 3})
    if placeholder:
        ranked.append({"kind": "CSS", "value": f'{tag}[placeholder="{_q(placeholder)}"]', "rank": 4})
    if _stable_id(element_id):
        ranked.append({"kind": "ID", "value": element_id, "rank": 6})
    return ranked


def _stable_id(value: str) -> bool:
    if not value or len(value) > 40 or value.startswith(":"):
        return False
    if _DYNAMIC_ID.search(value):
        return False
    if re.search(r"\d", value) and re.fullmatch(r"[A-Za-z0-9_-]{6,}", value):
        return False
    return True


def _attr(node: Tag, name: str) -> str:
    value = node.get(name) or ""
    return value if isinstance(value, str) else " ".join(value)


def _q(value: str) -> str:
    return value.replace("\\", "\\\\").replace('"', '\\"')
