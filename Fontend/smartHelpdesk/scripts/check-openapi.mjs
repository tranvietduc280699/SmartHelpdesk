import assert from 'node:assert/strict'
import { readFileSync, readdirSync } from 'node:fs'
import { resolve } from 'node:path'

const spec = JSON.parse(readFileSync('public/swagger/openapi.json', 'utf8'))
const controllerDir = resolve(process.argv[2] || '../../Backend/KITS-BZCOM-BE', 'src/main/java/org/example/besmarthelpdesk/controller')
const actual = [], documented = [], ids = new Set()
for (const file of readdirSync(controllerDir).filter(name => name.endsWith('.java'))) {
  const source = readFileSync(resolve(controllerDir, file), 'utf8')
  const mapping = source.match(/@RequestMapping\(([^)]+)\)/)
  assert(mapping, `Missing base mapping: ${file}`)
  const prefixes = [...mapping[1].matchAll(/"([^"]+)"/g)].map(match => match[1])
  for (const match of source.matchAll(/@(Get|Post|Patch|Put|Delete)Mapping(?:\("([^"]*)"\))?/g)) {
    for (const prefix of prefixes) actual.push(`${match[1].toLowerCase()} ${prefix}${match[2] || ''}`)
  }
}
for (const [path, methods] of Object.entries(spec.paths)) for (const [method, operation] of Object.entries(methods)) {
  documented.push(`${method} /api${path}`)
  assert(!ids.has(operation.operationId), `Duplicate operationId ${operation.operationId}`)
  ids.add(operation.operationId)
  assert(Object.keys(operation.responses).some(status => /^2\d\d$/.test(status)))
  for (const [, name] of path.matchAll(/\{(\w+)\}/g)) assert(operation.parameters?.some(parameter => parameter.name === name && parameter.in === 'path' && parameter.required), `Missing parameter ${path}:${name}`)
}
assert.deepEqual(documented.sort(), actual.sort(), 'Swagger operations differ from backend controllers')
function check(value) {
  if (!value || typeof value !== 'object') return
  if (value.$ref) assert(value.$ref.split('/').slice(1).reduce((node, key) => node?.[key], spec), `Unresolved ${value.$ref}`)
  for (const child of Object.values(value)) check(child)
}
check(spec)
for (const [name, schema] of Object.entries(spec.components.schemas)) if (schema.type === 'object') assert(Object.keys(schema.properties || {}).length, `Empty DTO ${name}`)
assert.deepEqual(spec.paths['/companies'].get.security, [])
assert.equal(spec.paths['/members'].get['x-permission'], 'ADMIN')
assert(spec.components.schemas.AlertType.enum.includes('REQUEST_REGISTERED'))
console.info(`OpenAPI checks passed: ${documented.length} operations match backend controllers; all references resolve.`)
