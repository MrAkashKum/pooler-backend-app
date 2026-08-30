#!/usr/bin/env node

import fs from 'node:fs';
import path from 'node:path';

const [specPath, outputDir = 'doc'] = process.argv.slice(2);
if (!specPath) {
  console.error('Usage: node scripts/generate-postman.mjs OPENAPI_JSON [OUTPUT_DIR]');
  process.exit(1);
}

const spec = JSON.parse(fs.readFileSync(specPath, 'utf8'));
const schemaUrl = 'https://schema.getpostman.com/json/collection/v2.1.0/collection.json';
const methods = ['get', 'post', 'put', 'patch', 'delete', 'head', 'options'];

function resolve(ref) {
  if (!ref?.startsWith('#/')) return {};
  return ref.slice(2).split('/').reduce((value, key) => value?.[key.replaceAll('~1', '/').replaceAll('~0', '~')], spec) || {};
}

function sample(schema = {}, seen = new Set()) {
  if (schema.$ref) {
    if (seen.has(schema.$ref)) return {};
    return sample(resolve(schema.$ref), new Set([...seen, schema.$ref]));
  }
  if (schema.example !== undefined) return schema.example;
  if (schema.default !== undefined) return schema.default;
  if (schema.enum?.length) return schema.enum[0];
  if (schema.oneOf?.length) return sample(schema.oneOf[0], seen);
  if (schema.anyOf?.length) return sample(schema.anyOf[0], seen);
  if (schema.type === 'array') return [sample(schema.items, seen)];
  if (schema.type === 'boolean') return true;
  if (schema.type === 'integer' || schema.type === 'number') return schema.minimum ?? 0;
  if (schema.type === 'string') {
    if (schema.format === 'email') return 'user@example.com';
    if (schema.format === 'password') return 'ChangeMe123!';
    if (schema.format === 'date-time') return new Date(0).toISOString();
    if (schema.format === 'date') return '2026-01-01';
    return schema.pattern ? 'example' : 'string';
  }
  const properties = schema.properties || {};
  return Object.fromEntries(Object.entries(properties)
    .filter(([, value]) => !value.readOnly)
    .map(([key, value]) => [key, sample(value, seen)]));
}

function variableFor(name) {
  const aliases = {
    id: 'userId', entityId: 'userId', contactEntityId: 'contactEntityId',
    locationEntityId: 'locationEntityId', invitationEntityId: 'invitationEntityId',
    invitationId: 'invitationEntityId', rideEntityId: 'rideEntityId', threadId: 'threadId',
    messageId: 'messageId', fileId: 'fileId', sessionId: 'sessionId', feedbackEntityId: 'feedbackEntityId'
  };
  return aliases[name] || name;
}

function postmanPath(apiPath) {
  return apiPath.replaceAll(/{([^}]+)}/g, (_, name) => `{{${variableFor(name)}}}`);
}

function parametersFor(pathItem, operation) {
  return [...(pathItem.parameters || []), ...(operation.parameters || [])]
    .map(parameter => parameter.$ref ? resolve(parameter.$ref) : parameter);
}

function requestBody(operation) {
  const body = operation.requestBody?.$ref ? resolve(operation.requestBody.$ref) : operation.requestBody;
  if (!body?.content) return {};
  const multipart = body.content['multipart/form-data'];
  if (multipart) {
    const schema = multipart.schema?.$ref ? resolve(multipart.schema.$ref) : multipart.schema || {};
    return {
      body: {
        mode: 'formdata',
        formdata: Object.entries(schema.properties || {}).map(([key, value]) => ({
          key,
          type: value.format === 'binary' ? 'file' : 'text',
          ...(value.format === 'binary' ? { src: [] } : { value: String(sample(value)) })
        }))
      }
    };
  }
  const content = body.content['application/json'] || Object.values(body.content)[0];
  if (!content) return {};
  return {
    header: [{ key: 'Content-Type', value: 'application/json' }],
    body: { mode: 'raw', raw: JSON.stringify(sample(content.schema || {}), null, 2), options: { raw: { language: 'json' } } }
  };
}

function testsFor(apiPath, method) {
  const lines = [
    "pm.test('Response status is successful or expected client error', () => pm.expect(pm.response.code).to.be.below(500));"
  ];
  if (apiPath.endsWith('/auth/login') || apiPath.endsWith('/auth/google') || apiPath.endsWith('/auth/apple') || apiPath.endsWith('/auth/refresh')) {
    lines.push(
      "const json = pm.response.json();",
      "const data = json.data || json;",
      "if (data.accessToken) pm.environment.set('accessToken', data.accessToken);",
      "if (data.refreshToken) pm.environment.set('refreshToken', data.refreshToken);",
      "if (data.sessionToken) pm.environment.set('sessionToken', data.sessionToken);"
    );
  }
  if (method === 'post') {
    lines.push(
      "try { const json = pm.response.json(); const data = json.data || json;",
      "const mappings = { entityId: 'userId', contactEntityId: 'contactEntityId', locationEntityId: 'locationEntityId', invitationEntityId: 'invitationEntityId', rideEntityId: 'rideEntityId', threadId: 'threadId', messageId: 'messageId', feedbackEntityId: 'feedbackEntityId' };",
      "Object.entries(mappings).forEach(([field, variable]) => { if (data[field]) pm.environment.set(variable, data[field]); }); } catch (_) {}"
    );
  }
  return [{ listen: 'test', script: { type: 'text/javascript', exec: lines } }];
}

function makeRequest(apiPath, pathItem, method, operation) {
  const parameters = parametersFor(pathItem, operation);
  const query = parameters.filter(p => p.in === 'query').map(p => ({
    key: p.name,
    value: p.example !== undefined ? String(p.example) : String(sample(p.schema || {})),
    disabled: !p.required,
    description: p.description || undefined
  }));
  const urlPath = postmanPath(apiPath);
  const rawUrl = `{{baseUrl}}${urlPath}${query.length ? `?${query.filter(q => !q.disabled).map(q => `${q.key}=${encodeURIComponent(q.value)}`).join('&')}` : ''}`;
  const body = requestBody(operation);
  const protectedRoute = Array.isArray(operation.security) && operation.security.length > 0;
  return {
    name: `${method.toUpperCase()} ${operation.summary || apiPath}`,
    event: testsFor(apiPath, method),
    request: {
      method: method.toUpperCase(),
      ...(!protectedRoute ? { auth: { type: 'noauth' } } : {}),
      header: body.header || [],
      ...(body.body ? { body: body.body } : {}),
      url: { raw: rawUrl, host: ['{{baseUrl}}'], path: urlPath.slice(1).split('/'), ...(query.length ? { query } : {}) },
      description: operation.description || operation.summary || ''
    }
  };
}

const folders = new Map();
for (const [apiPath, pathItem] of Object.entries(spec.paths || {})) {
  for (const method of methods) {
    const operation = pathItem[method];
    if (!operation) continue;
    const tag = operation.tags?.[0] || 'Other';
    if (!folders.has(tag)) folders.set(tag, []);
    folders.get(tag).push(makeRequest(apiPath, pathItem, method, operation));
  }
}

const collection = {
  info: {
    _postman_id: 'c2a4f6d8-cab5-4c2b-8e2a-pooler-full-002',
    name: 'Pooler / Hoppo API (Complete)',
    description: `Generated from the deployed OpenAPI specification. Contains ${[...folders.values()].flat().length} operations. Select the Pooler Local or Pooler Staging environment before running requests.`,
    schema: schemaUrl
  },
  auth: { type: 'bearer', bearer: [{ key: 'token', value: '{{accessToken}}', type: 'string' }] },
  event: [{ listen: 'prerequest', script: { type: 'text/javascript', exec: [
    "pm.request.headers.upsert({ key: 'X-Device-Id', value: pm.environment.get('deviceId') || 'postman-device-001' });",
    "pm.request.headers.upsert({ key: 'X-Platform', value: pm.environment.get('platform') || 'ANDROID' });",
    "pm.request.headers.upsert({ key: 'X-App-Version', value: pm.environment.get('appVersion') || '1.0.0' });",
    "if (pm.environment.get('sessionToken')) pm.request.headers.upsert({ key: 'X-Session-Token', value: pm.environment.get('sessionToken') });",
    "if (pm.environment.get('fcmToken')) pm.request.headers.upsert({ key: 'X-FCM-Token', value: pm.environment.get('fcmToken') });"
  ] } }],
  variable: [{ key: 'baseUrl', value: 'http://localhost:8888/pooler-backend' }],
  item: [...folders.entries()].sort(([a], [b]) => a.localeCompare(b)).map(([name, item]) => ({ name, item }))
};

const authFolder = collection.item.find(folder => folder.name === 'Authentication');
const authCollection = {
  ...collection,
  info: { ...collection.info, _postman_id: 'a18d521f-e854-483f-a9d6-pooler-auth-002', name: 'Pooler / Hoppo Authentication API' },
  item: authFolder ? [authFolder] : []
};

const environmentVariables = baseUrl => [
  ['baseUrl', baseUrl, 'default'], ['accessToken', '', 'secret'], ['refreshToken', '', 'secret'],
  ['sessionToken', '', 'secret'], ['userId', '', 'default'], ['deviceId', 'postman-device-001', 'default'],
  ['platform', 'ANDROID', 'default'], ['appVersion', '1.0.0', 'default'], ['fcmToken', 'postman-fcm-token', 'secret'],
  ['contactEntityId', '', 'default'], ['locationEntityId', '', 'default'], ['invitationEntityId', '', 'default'],
  ['rideEntityId', '', 'default'], ['sessionId', '', 'default'], ['threadId', '', 'default'],
  ['messageId', '', 'default'], ['fileId', '', 'default'], ['feedbackEntityId', '', 'default']
].map(([key, value, type]) => ({ key, value, type, enabled: true }));

function environment(id, name, baseUrl) {
  return { id, name, values: environmentVariables(baseUrl), _postman_variable_scope: 'environment', _postman_exported_at: new Date().toISOString(), _postman_exported_using: 'Pooler Postman generator' };
}

fs.mkdirSync(outputDir, { recursive: true });
fs.writeFileSync(path.join(outputDir, 'Pooler-API.postman_collection.json'), `${JSON.stringify(collection, null, 2)}\n`);
fs.writeFileSync(path.join(outputDir, 'Pooler-Auth-API.postman_collection.json'), `${JSON.stringify(authCollection, null, 2)}\n`);
fs.writeFileSync(path.join(outputDir, 'Pooler-Local.postman_environment.json'), `${JSON.stringify(environment('pooler-local-env-002', 'Pooler — Local', 'http://localhost:8888/pooler-backend'), null, 2)}\n`);
fs.writeFileSync(path.join(outputDir, 'Pooler-Staging.postman_environment.json'), `${JSON.stringify(environment('pooler-staging-env-001', 'Pooler — Staging (GCP)', 'https://pooler-backend-663018144709.asia-southeast1.run.app/pooler-backend'), null, 2)}\n`);
