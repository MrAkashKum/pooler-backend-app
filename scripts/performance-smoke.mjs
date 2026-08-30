#!/usr/bin/env node

const baseUrl = (process.env.BASE_URL || 'http://localhost:8888/pooler-backend').replace(/\/$/, '');
const totalRequests = Number(process.env.REQUESTS || 200);
const concurrency = Number(process.env.CONCURRENCY || 25);
const timeoutMs = Number(process.env.TIMEOUT_MS || 5000);
const invalidLoginEmail = process.env.SMOKE_INVALID_LOGIN_EMAIL || 'invalid-login@example.invalid';
const invalidLoginPassword = process.env.SMOKE_INVALID_LOGIN_PASSWORD || 'InvalidLoginOnly@123';

function percentile(sorted, p) {
  if (sorted.length === 0) return 0;
  const index = Math.min(sorted.length - 1, Math.ceil((p / 100) * sorted.length) - 1);
  return sorted[index];
}

async function timedRequest(kind, input, init = {}) {
  const started = performance.now();
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), timeoutMs);
  try {
    const response = await fetch(input, { ...init, signal: controller.signal });
    const text = await response.text();
    return {
      kind,
      ok: response.ok,
      status: response.status,
      ms: Math.round(performance.now() - started),
      traceId: response.headers.get('x-correlation-id') || '',
      errorReferenceId: response.headers.get('x-error-reference-id') || '',
      bodySnippet: text.slice(0, 120),
    };
  } catch (error) {
    return {
      kind,
      ok: false,
      status: 0,
      ms: Math.round(performance.now() - started),
      error: error?.name || 'RequestError',
    };
  } finally {
    clearTimeout(timeout);
  }
}

function headers(index) {
  return {
    'Content-Type': 'application/json',
    'X-Platform': index % 3 === 0 ? 'IOS' : index % 3 === 1 ? 'ANDROID' : 'WEB',
    'X-App-Version': process.env.APP_VERSION || '1.0.1',
    'X-Device-Id': `smoke-device-${index}`,
    'X-Correlation-ID': `smoke-${Date.now()}-${index}`,
  };
}

async function runOne(index) {
  if (index % 4 === 0) {
    return timedRequest('invalid-login', `${baseUrl}/api/v1/auth/login`, {
      method: 'POST',
      headers: headers(index),
      body: JSON.stringify({
        email: invalidLoginEmail,
        password: invalidLoginPassword,
        platform: 'WEB',
      }),
    });
  }
  return timedRequest('health', `${baseUrl}/api/v1/public/health`, {
    method: 'GET',
    headers: headers(index),
  });
}

async function runPool() {
  const results = [];
  let next = 0;
  async function worker() {
    while (next < totalRequests) {
      const index = next;
      next += 1;
      results.push(await runOne(index));
    }
  }
  await Promise.all(Array.from({ length: concurrency }, worker));
  return results;
}

const results = await runPool();
const durations = results.map((result) => result.ms).sort((a, b) => a - b);
const failures = results.filter((result) => result.status === 0 || result.status >= 500);
const expectedRejections = results.filter((result) => result.kind === 'invalid-login' && [401, 429].includes(result.status));
const healthOk = results.filter((result) => result.kind === 'health' && result.status === 200);

const summary = {
  baseUrl,
  totalRequests,
  concurrency,
  healthOk: healthOk.length,
  expectedAuthRejections: expectedRejections.length,
  serverFailures: failures.length,
  minMs: durations[0] || 0,
  p50Ms: percentile(durations, 50),
  p95Ms: percentile(durations, 95),
  p99Ms: percentile(durations, 99),
  maxMs: durations.at(-1) || 0,
  statusCounts: results.reduce((acc, result) => {
    acc[result.status] = (acc[result.status] || 0) + 1;
    return acc;
  }, {}),
};

console.log(JSON.stringify(summary, null, 2));

if (failures.length > 0) {
  console.error('Server failures detected:', JSON.stringify(failures.slice(0, 5), null, 2));
  process.exit(1);
}
