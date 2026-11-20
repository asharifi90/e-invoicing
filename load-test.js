import http from 'k6/http';
import { check, sleep } from 'k6';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

export const options = {
  stages: [
    { duration: '5s', target: 10 },
    { duration: '15s', target: 25 },
    { duration: '5s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const TOKEN = __ENV.TOKEN || '';

export default function () {
  const uniqueId = uuidv4();
  const payload = JSON.stringify({
    invoiceNumber: `INV-LOAD-${uniqueId.substring(0, 8)}`,
    sellerVatNumber: 'BE0123456789',
    buyerVatNumber: 'BE9876543210',
    lines: [
      {
        description: 'Consulting',
        quantity: 2,
        unitPrice: 15.00,
        currency: 'EUR',
      },
    ],
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'Idempotency-Key': uniqueId,
      ...(TOKEN ? { Authorization: `Bearer ${TOKEN}` } : {}),
    },
  };

  const res = http.post(`${BASE_URL}/api/invoices`, payload, params);

  check(res, {
    'status is 201': (r) => r.status === 201,
  });

  sleep(0.1);
}