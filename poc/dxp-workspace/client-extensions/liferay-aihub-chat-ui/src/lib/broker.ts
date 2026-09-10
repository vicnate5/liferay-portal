/**
 * HTTP client for the broker described in poc/CONTRACT.md.
 *
 * Every call carries Authorization: Bearer <token> when a token is available.
 * The token is never placed in a query string, which is the reason this client
 * reads the event stream with fetch instead of EventSource.
 */

import {DEFAULT_BROKER_BASE_URL} from '../config';
import {getAuth} from './auth';
import {BrokerEvent, SessionSummary} from './events';
import {readSseStream, SseFrame} from './sse';

export type Health = {
	agent: string;
	status: string;
	version: string;
	workspace: string;
};

export class BrokerClient {
	readonly baseURL: string;

	constructor(baseURL?: string | null) {
		this.baseURL = (baseURL || DEFAULT_BROKER_BASE_URL).replace(/\/+$/, '');
	}

	async createSession(title: string): Promise<SessionSummary> {
		const created = await this._json<{
			createdAt: string;
			sessionId: string;
			title: string;
		}>('POST', '/api/sessions', {title});

		return {...created, state: 'idle'};
	}

	async health(): Promise<Health> {
		return this._json<Health>('GET', '/api/health');
	}

	async interrupt(sessionId: string): Promise<void> {
		await this._send(
			'POST',
			`/api/sessions/${encodeURIComponent(sessionId)}/interrupt`
		);
	}

	async listSessions(): Promise<SessionSummary[]> {
		return this._json<SessionSummary[]>('GET', '/api/sessions');
	}

	/**
	 * Replay of persisted events for a reload. The broker returns the same
	 * {event, data} envelopes that travel on the stream, so the transcript
	 * reducer consumes both without a second code path.
	 */
	async replay(sessionId: string): Promise<BrokerEvent[]> {
		return this._json<BrokerEvent[]>(
			'GET',
			`/api/sessions/${encodeURIComponent(sessionId)}/messages`
		);
	}

	async sendMessage(sessionId: string, text: string): Promise<void> {
		await this._send(
			'POST',
			`/api/sessions/${encodeURIComponent(sessionId)}/messages`,
			{text}
		);
	}

	/**
	 * Open the event stream and call onEvent for every contract frame.
	 * Comment frames such as :open and :ping never reach onEvent because the
	 * parser drops any frame without a data field.
	 */
	async stream(
		sessionId: string,
		onEvent: (event: BrokerEvent) => void,
		signal: AbortSignal
	): Promise<void> {
		const response = await fetch(
			`${this.baseURL}/api/sessions/${encodeURIComponent(sessionId)}/stream`,
			{
				headers: {
					accept: 'text/event-stream',
					...(await this._authHeaders()),
				},
				signal,
			}
		);

		if (!response.ok) {
			throw new Error(
				`The broker refused the event stream with status ${response.status}`
			);
		}

		await readSseStream(response, (frame: SseFrame) => {
			let data: unknown;

			try {
				data = JSON.parse(frame.data);
			}
			catch {
				return;
			}

			onEvent({data, event: frame.event} as BrokerEvent);
		});
	}

	private async _authHeaders(): Promise<Record<string, string>> {
		const auth = await getAuth();

		return auth.token ? {authorization: `Bearer ${auth.token}`} : {};
	}

	private async _json<T>(
		method: string,
		path: string,
		body?: unknown
	): Promise<T> {
		const response = await this._send(method, path, body);

		return (await response.json()) as T;
	}

	private async _send(
		method: string,
		path: string,
		body?: unknown
	): Promise<Response> {
		const response = await fetch(`${this.baseURL}${path}`, {
			body: body === undefined ? undefined : JSON.stringify(body),
			headers: {
				accept: 'application/json',
				...(body === undefined
					? {}
					: {'content-type': 'application/json'}),
				...(await this._authHeaders()),
			},
			method,
		});

		if (!response.ok) {
			throw new Error(await describeFailure(response));
		}

		return response;
	}
}

async function describeFailure(response: Response): Promise<string> {
	if (response.status === 401) {
		return 'The broker rejected the credentials for this page';
	}

	if (response.status === 409) {
		return 'That session is still busy. Wait for the current turn to finish';
	}

	let detail = '';

	try {
		const text = await response.text();
		const parsed = text ? (JSON.parse(text) as {message?: string}) : null;

		detail = parsed?.message ?? text;
	}
	catch {
		detail = '';
	}

	return detail
		? `The broker returned ${response.status}: ${detail}`
		: `The broker returned ${response.status}`;
}
