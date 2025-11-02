/**
 * Integration & unit test template for Events endpoints
 * - Follows assignment requirement to have two describe groups per interface:
 *   1) unmocked (no mocking) - runs against a real server or a configured test server
 *   2) mocked - uses Jest mocks to simulate external dependencies (DB errors, etc.)
 *
 * Usage notes:
 * - To run the unmocked/integration tests you can set the environment variable
 *   RUN_INTEGRATION_TESTS=1 and make sure the backend server is running or set
 *   TEST_SERVER_URL=http://localhost:3000 to point to a running instance.
 * - Mocked tests run locally and do not require a running server.
 */

import request from 'supertest';
import { describe, test, expect } from '@jest/globals';
import dotenv from 'dotenv';

dotenv.config();
const TEST_SERVER_URL = process.env.SERVER_URL as string;

/**
 * GET /api/events (unmocked)
 * - Type: Unmocked (integration)
 * - Inputs: none (GET list)
 * - Expected status: 200 OK
 * - Expected output: { message: string, data: { events: Event[] } }
 */
describe('GET /api/events - unmocked (requires running server)', () => {
	test('returns list of events (200) when server is available', async () => {
		const res = await request(TEST_SERVER_URL).get('/api/events').set('Authorization', `Bearer ${process.env.TEST_TOKEN}`);
		console.log(res.body);
		expect(res.status).toBe(200);
		expect(res.body).toHaveProperty('message');
		expect(res.body).toHaveProperty('data');
		expect(res.body.data).toHaveProperty('events');
		expect(Array.isArray(res.body.data.events)).toBe(true);
	});
});

/**
 * GET /api/events (unmocked)
 * - Type: Unmocked (integration)
 * - Inputs: none (GET list)
 * - Expected status: 200 OK
 * - Expected output: { message: string, data: { events: Event[] } }
 */
describe('GET /api/events - unmocked (requires running server)', () => {
	test('returns list of events (200) when server is available', async () => {
		const res = await request(TEST_SERVER_URL).get('/api/events').set('Authorization', `Bearer ${process.env.TEST_TOKEN}`);
		console.log(res.body);
		expect(res.status).toBe(200);
		expect(res.body).toHaveProperty('message');
		expect(res.body).toHaveProperty('data');
		expect(res.body.data).toHaveProperty('events');
		expect(Array.isArray(res.body.data.events)).toBe(true);
	});
});


