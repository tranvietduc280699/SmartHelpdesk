"""Static contract checks; run with Python 3, no external dependencies."""
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
spec = json.loads((ROOT / 'docs/openapi.json').read_text(encoding='utf-8'))


def resolve(ref):
    node = spec
    assert ref.startswith('#/'), ref
    for part in ref[2:].split('/'):
        node = node[part.replace('~1', '/').replace('~0', '~')]
    return node


def walk(value):
    if isinstance(value, dict):
        if '$ref' in value:
            resolve(value['$ref'])
        for child in value.values():
            walk(child)
    elif isinstance(value, list):
        for child in value:
            walk(child)


def check_example(value, schema):
    if '$ref' in schema:
        return check_example(value, resolve(schema['$ref']))
    if value is None and schema.get('nullable'):
        return
    for parent in schema.get('allOf', []):
        check_example(value, parent)
    if 'enum' in schema:
        assert value in schema['enum'], (value, schema)
    kind = schema.get('type')
    if kind == 'object':
        assert isinstance(value, dict)
        assert set(schema.get('required', [])) <= set(value)
        for key, item in value.items():
            check_example(item, schema.get('properties', {}).get(key, schema.get('additionalProperties', {})))
    elif kind == 'array':
        assert isinstance(value, list)
        for item in value:
            check_example(item, schema['items'])
    elif kind == 'string':
        assert isinstance(value, str), value
        assert len(value) >= schema.get('minLength', 0)
        assert len(value) <= schema.get('maxLength', float('inf'))
        if 'pattern' in schema:
            assert re.search(schema['pattern'], value)
    elif kind == 'boolean':
        assert isinstance(value, bool)
    elif kind in ('integer', 'number'):
        assert isinstance(value, (int, float)) and not isinstance(value, bool)


walk(spec)
expected = set()
for file in (ROOT / 'src/main/java/org/example/besmarthelpdesk/controller').glob('*.java'):
    source = file.read_text(encoding='utf-8')
    prefix = re.search(r'@RequestMapping\((.*?)\)', source, re.S).group(1)
    bases = re.findall(r'"([^"]+)"', prefix)
    for method, suffix in re.findall(r'@(Get|Post|Patch|Put|Delete)Mapping(?:\("([^"]*)"\))?', source):
        for base in bases:
            expected.add((method.lower(), base + suffix))
actual = set()
ids = set()
for path, methods in spec['paths'].items():
    for method, operation in methods.items():
        actual.add((method, path))
        assert operation['operationId'] not in ids
        ids.add(operation['operationId'])
        variables = set(re.findall(r'\{(\w+)\}', path))
        parameters = {p['name'] for p in operation.get('parameters', []) if p['in'] == 'path' and p['required']}
        assert variables == parameters, path
        containers = list(operation['responses'].values())
        if 'requestBody' in operation:
            containers.append(operation['requestBody'])
        for container in containers:
            if '$ref' in container:
                container = resolve(container['$ref'])
            for media in container.get('content', {}).values():
                if 'example' in media:
                    check_example(media['example'], media['schema'])
                for sample in media.get('examples', {}).values():
                    check_example(sample['value'], media['schema'])
assert actual == expected, {'missing': sorted(expected - actual), 'extra': sorted(actual - expected)}
assert len(actual) == 29
print(f'PASS: {len(actual)} operations match controllers; {len(spec["components"]["schemas"])} schemas; references, IDs, path parameters and examples valid.')