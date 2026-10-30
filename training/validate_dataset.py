import json, sys
from pathlib import Path

path = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).parent / "data" / "manu_train.jsonl"
errors = 0
count = 0
for line_no, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
    if not line.strip():
        continue
    count += 1
    try:
        row = json.loads(line)
        messages = row["messages"]
        assert isinstance(messages, list) and len(messages) >= 2
        assert messages[0]["role"] == "system"
        assert messages[-1]["role"] == "assistant"
        assert all(m["role"] in {"system", "user", "assistant"} and m["content"] for m in messages)
    except Exception as exc:
        errors += 1
        print(f"line {line_no}: {exc}")
if errors:
    raise SystemExit(f"dataset inválido: {errors} errores")
print(f"dataset válido: {count} ejemplos")
