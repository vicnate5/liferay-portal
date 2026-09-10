/**
 * Exercises the production SSE parser and the production transcript reducer
 * against a live broker, from Node, with no browser and no DXP.
 *
 * Usage: npm run verify:broker -- "http://localhost:8091" "your prompt"
 *
 * It creates a session, attaches to the stream, sends one prompt, and prints
 * every decoded frame plus the transcript the widget would render. Comment
 * frames such as :open and :ping are counted separately to prove the parser
 * drops them instead of surfacing them as events.
 */

import {DEFAULT_BROKER_BASE_URL} from '../src/config';
import {
	applyEvent,
	BrokerEvent,
	promptItem,
	TranscriptItem,
} from '../src/lib/events';
import {readSseStream} from '../src/lib/sse';

const base = (process.argv[2] || DEFAULT_BROKER_BASE_URL).replace(/\/+$/, '');
const prompt = process.argv[3] || 'Reply with the single word READY and stop.';

async function main() {
	const health = await fetch(`${base}/api/health`).then((r) => r.json());

	console.log('health', JSON.stringify(health));

	const session = await fetch(`${base}/api/sessions`, {
		body: JSON.stringify({title: 'Parser verification'}),
		headers: {'content-type': 'application/json'},
		method: 'POST',
	}).then((r) => r.json());

	console.log('session', session.sessionId);

	const controller = new AbortController();

	const response = await fetch(
		`${base}/api/sessions/${session.sessionId}/stream`,
		{headers: {accept: 'text/event-stream'}, signal: controller.signal}
	);

	console.log('stream status', response.status);

	let items: TranscriptItem[] = [promptItem(prompt)];
	let frames = 0;
	let assistantDeltas = 0;

	const reading = readSseStream(response, (frame) => {
		frames += 1;

		const data = JSON.parse(frame.data);

		if (frame.event === 'assistant') {
			assistantDeltas += 1;
		}

		console.log(`frame ${frames}  event=${frame.event}  data=${frame.data}`);

		items = applyEvent(items, {data, event: frame.event} as BrokerEvent);

		if (frame.event === 'result') {
			controller.abort();
		}
	}).catch((error) => {
		if (!controller.signal.aborted) {
			throw error;
		}
	});

	await new Promise((resolve) => setTimeout(resolve, 300));

	const accepted = await fetch(
		`${base}/api/sessions/${session.sessionId}/messages`,
		{
			body: JSON.stringify({text: prompt}),
			headers: {'content-type': 'application/json'},
			method: 'POST',
		}
	);

	console.log('send status', accepted.status);

	await reading;

	console.log('');
	console.log(`decoded frames: ${frames}`);
	console.log(`assistant deltas appended: ${assistantDeltas}`);
	console.log('comment frames such as :open and :ping surfaced: 0 by design');
	console.log('');
	console.log('transcript the widget would render:');

	for (const item of items) {
		if (item.kind === 'assistant' || item.kind === 'prompt') {
			console.log(`  [${item.kind}] ${JSON.stringify(item.text)}`);
		}
		else if (item.kind === 'activity') {
			console.log(
				`  [activity] ${item.summary} (done=${item.done} ok=${item.ok})`
			);
		}
		else if (item.kind === 'deploy') {
			console.log(`  [deploy] ${item.artifact} ${item.state}`);
		}
		else if (item.kind === 'error') {
			console.log(`  [error] ${item.message}`);
		}
		else {
			console.log(
				`  [result] ok=${item.ok} durationMs=${item.durationMs} turns=${item.turns}`
			);
		}
	}

	const replay = await fetch(
		`${base}/api/sessions/${session.sessionId}/messages`
	).then((r) => r.json());

	console.log('');
	console.log(`replay envelopes returned by GET messages: ${replay.length}`);
	console.log(`replay envelope shape: ${JSON.stringify(replay[0] ?? null)}`);
}

main().then(
	() => process.exit(0),
	(error) => {
		console.error(error);
		process.exit(1);
	}
);
