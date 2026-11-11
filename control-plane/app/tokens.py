import secrets


def token_ok(expected: str, provided: str) -> bool:
    if not expected or not provided or len(expected) != len(provided):
        return False
    return secrets.compare_digest(expected, provided)
