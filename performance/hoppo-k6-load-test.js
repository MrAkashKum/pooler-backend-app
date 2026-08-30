import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { SharedArray } from 'k6/data';
import exec from 'k6/execution';

const baseUrl = (__ENV.BASE_URL || 'http://localhost:8888/pooler-backend').replace(/\/$/, '');
const profile = (__ENV.LOAD_PROFILE || 'smoke').toLowerCase();
const requestTimeout = __ENV.REQUEST_TIMEOUT || '10s';
const invalidLoginEmail = __ENV.INVALID_LOGIN_EMAIL || 'invalid-login@example.invalid';
const invalidLoginPassword = __ENV.INVALID_LOGIN_PASSWORD || 'InvalidLoginOnly@123';
const loadLocationLabel = __ENV.LOAD_LOCATION_LABEL || 'Load test point';
const loadLocationAddress = __ENV.LOAD_LOCATION_ADDRESS || 'Load test location';
const loadSafetyCategory = __ENV.LOAD_SAFETY_CATEGORY || 'LOAD_TEST_RECORD';
const loadSafetyDetails = __ENV.LOAD_SAFETY_DETAILS || 'Load test safety-report create-path validation.';
const loadDestinationAddress = __ENV.LOAD_DESTINATION_ADDRESS || 'Load test destination';

const profiles = {
  smoke: {
    scenarios: {
      public_health: { executor: 'constant-vus', vus: 5, duration: '1m', exec: 'publicHealth' },
      auth_validation: { executor: 'constant-arrival-rate', rate: 2, timeUnit: '1m', duration: '1m', preAllocatedVUs: 2, exec: 'authValidation' },
      rider_journey: { executor: 'constant-vus', vus: 5, duration: '1m', exec: 'riderJourney' },
    },
  },
  '1k': {
    scenarios: {
      public_health: { executor: 'ramping-vus', stages: [{ duration: '2m', target: 250 }, { duration: '6m', target: 1000 }, { duration: '2m', target: 0 }], exec: 'publicHealth' },
      rider_journey: { executor: 'ramping-vus', stages: [{ duration: '2m', target: 250 }, { duration: '6m', target: 1000 }, { duration: '2m', target: 0 }], exec: 'riderJourney' },
      auth_validation: { executor: 'constant-arrival-rate', rate: 20, timeUnit: '1m', duration: '10m', preAllocatedVUs: 20, exec: 'authValidation' },
    },
  },
  '5k': {
    scenarios: {
      public_health: { executor: 'ramping-vus', stages: [{ duration: '5m', target: 1000 }, { duration: '10m', target: 5000 }, { duration: '5m', target: 0 }], exec: 'publicHealth' },
      rider_journey: { executor: 'ramping-vus', stages: [{ duration: '5m', target: 1000 }, { duration: '10m', target: 5000 }, { duration: '5m', target: 0 }], exec: 'riderJourney' },
      auth_validation: { executor: 'constant-arrival-rate', rate: 50, timeUnit: '1m', duration: '20m', preAllocatedVUs: 50, exec: 'authValidation' },
    },
  },
  '10k': {
    scenarios: {
      public_health: { executor: 'ramping-vus', stages: [{ duration: '10m', target: 2500 }, { duration: '15m', target: 10000 }, { duration: '10m', target: 0 }], exec: 'publicHealth' },
      rider_journey: { executor: 'ramping-vus', stages: [{ duration: '10m', target: 2500 }, { duration: '15m', target: 10000 }, { duration: '10m', target: 0 }], exec: 'riderJourney' },
      auth_validation: { executor: 'constant-arrival-rate', rate: 100, timeUnit: '1m', duration: '35m', preAllocatedVUs: 100, exec: 'authValidation' },
    },
  },
};

export const options = {
  ...(profiles[profile] || profiles.smoke),
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500', 'p(99)<1200'],
    'checks{type:contract}': ['rate>0.99'],
  },
};

const users = new SharedArray('staging-riders', () => {
  if (!__ENV.USER_CSV) {
    return [];
  }
  const rows = open(__ENV.USER_CSV).trim().split(/\r?\n/);
  const headers = rows.shift().split(',').map((value) => value.trim());
  return rows.filter(Boolean).map((row) => {
    const values = row.split(',').map((value) => value.trim());
    return Object.fromEntries(headers.map((header, index) => [header, values[index] || '']));
  });
});

function pickUser() {
  if (users.length === 0) {
    return null;
  }
  return users[exec.vu.idInTest % users.length];
}

function correlationId(label) {
  return `k6-${profile}-${label}-${exec.scenario.name}-${exec.vu.idInTest}-${exec.scenario.iterationInTest}`;
}

function commonHeaders(platform = 'ANDROID', token = '') {
  const headers = {
    'Content-Type': 'application/json',
    'X-Platform': platform || 'ANDROID',
    'X-App-Version': __ENV.APP_VERSION || '1.0.1',
    'X-Device-Id': `k6-device-${exec.vu.idInTest}`,
    'X-Correlation-ID': correlationId('hoppo'),
  };
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }
  return headers;
}

function post(path, body, headers) {
  return http.post(`${baseUrl}${path}`, JSON.stringify(body), { headers, timeout: requestTimeout });
}

function get(path, headers) {
  return http.get(`${baseUrl}${path}`, { headers, timeout: requestTimeout });
}

function jsonValue(response, path) {
  try {
    return response.json(path);
  } catch {
    return null;
  }
}

function tokenFor(user) {
  if (!user) {
    return '';
  }
  if (user.accessToken) {
    return user.accessToken;
  }
  if (!user.email || !user.password) {
    return '';
  }
  const response = post('/api/v1/auth/login', {
    email: user.email,
    password: user.password,
    platform: user.platform || 'ANDROID',
  }, commonHeaders(user.platform || 'ANDROID'));

  check(response, {
    'login returns token': (res) => res.status === 200 && Boolean(jsonValue(res, 'data.accessToken')),
  }, { type: 'contract' });

  return jsonValue(response, 'data.accessToken') || '';
}

export function publicHealth() {
  group('public health', () => {
    const response = get('/api/v1/public/health', commonHeaders('WEB'));
    check(response, {
      'health is ok': (res) => res.status === 200,
      'health envelope success': (res) => jsonValue(res, 'success') === true,
    }, { type: 'contract' });
  });
  sleep(1);
}

export function authValidation() {
  group('auth validation and trace envelope', () => {
    const response = post('/api/v1/auth/login', {
      email: invalidLoginEmail,
      password: invalidLoginPassword,
      platform: 'WEB',
    }, commonHeaders('WEB'));
    check(response, {
      'invalid login rejected safely': (res) => [401, 429].includes(res.status),
      'error has trace or is rate-limited': (res) => res.status === 429 || Boolean(jsonValue(res, 'traceId')),
    }, { type: 'contract' });
  });
  sleep(1);
}

export function riderJourney() {
  const user = pickUser();
  const token = tokenFor(user);
  if (!token) {
    publicHealth();
    return;
  }

  const platform = user.platform || (exec.vu.idInTest % 2 === 0 ? 'ANDROID' : 'IOS');
  const headers = commonHeaders(platform, token);
  const latitude = Number(user.latitude || '1.278278') + (exec.vu.idInTest % 10) * 0.0001;
  const longitude = Number(user.longitude || '103.802449') + (exec.vu.idInTest % 10) * 0.0001;
  const destinationLatitude = Number(user.destinationLatitude || '1.283400');
  const destinationLongitude = Number(user.destinationLongitude || '103.860700');

  group('authenticated rider journey', () => {
    check(get('/api/v1/discovery/status', headers), {
      'discovery status available': (res) => [200, 404].includes(res.status),
    }, { type: 'contract' });

    check(post('/api/v1/locations', {
      alias: 'CUSTOM',
      label: `${loadLocationLabel} ${exec.vu.idInTest}`,
      address: loadLocationAddress,
      latitude,
      longitude,
    }, headers), {
      'location create accepted or duplicate-safe': (res) => [200, 201, 400, 409].includes(res.status),
    }, { type: 'contract' });

    check(get('/api/v1/locations', headers), {
      'location list available': (res) => res.status === 200,
    }, { type: 'contract' });

    check(post('/api/v1/safety-reports', {
      category: loadSafetyCategory,
      details: loadSafetyDetails,
      contactAllowed: false,
      latitude,
      longitude,
    }, headers), {
      'safety report create available': (res) => [200, 201, 400, 429].includes(res.status),
    }, { type: 'contract' });

    check(http.put(`${baseUrl}/api/v1/discovery/toggle`, JSON.stringify({
      mode: 'ON',
      currentLatitude: latitude,
      currentLongitude: longitude,
      destinationLatitude,
      destinationLongitude,
      destinationAddress: loadDestinationAddress,
    }), { headers, timeout: requestTimeout }), {
      'discovery toggle available': (res) => [200, 400, 401, 429].includes(res.status),
    }, { type: 'contract' });

    check(post('/api/v1/discovery/nearby', {
      latitude,
      longitude,
      destinationLatitude,
      destinationLongitude,
      radiusKm: 5,
    }, headers), {
      'nearby search available': (res) => [200, 400, 429].includes(res.status),
    }, { type: 'contract' });
  });
  sleep(Math.random() * 2);
}
