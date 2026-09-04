import http from 'k6/http';
import { check, sleep } from 'k6';

// FAANG-Grade Load Testing Configuration (Simulating 50 concurrent users)
export const options = {
    stages: [
        { duration: '10s', target: 20 }, // Ramp-up to 20 users
        { duration: '30s', target: 50 }, // Stay at 50 users
        { duration: '10s', target: 0 },  // Ramp-down
    ],
    thresholds: {
        http_req_duration: ['p(95)<500'], // 95% of requests should be below 500ms
        http_req_failed: ['rate<0.01'],   // Error rate should be less than 1%
    },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8282';

export default function () {
    // 1. Browse Catalog (Evaluates Redis Cache-Aside read latency)
    let catalogRes = http.get(`${BASE_URL}/api/v1/books`);
    check(catalogRes, {
        'catalog status is 200': (r) => r.status === 200,
        'catalog latency < 100ms (cached)': (r) => r.timings.duration < 100,
    });

    // 2. Fetch Single Book by ID
    let bookRes = http.get(`${BASE_URL}/api/v1/books/1`);
    check(bookRes, {
        'book status is 200 or 404': (r) => r.status === 200 || r.status === 404,
    });

    sleep(0.5);
}
