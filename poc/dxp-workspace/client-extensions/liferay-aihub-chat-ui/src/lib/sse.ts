/**
 * Minimal Server Sent Events frame parser.
 *
 * EventSource cannot carry an Authorization header, so the chat UI reads the
 * broker stream with fetch and decodes the wire format here. This module is
 * deliberately free of React and of the DOM so it can be exercised directly
 * against the live broker from Node.
 *
 * Rules implemented, in line with the WHATWG event stream specification:
 *   - Frames are separated by a blank line.
 *   - A line beginning with a colon is a comment and is discarded. The broker
 *     sends :open once and :ping every fifteen seconds.
 *   - field: value, with one optional leading space stripped from the value.
 *   - Repeated data lines join with a newline.
 *   - A frame with no data lines yields nothing.
 */

export type SseFrame = {
	data: string;
	event: string;
	id?: string;
};

function parseBlock(block: string): SseFrame | null {
	let event = 'message';
	let id: string | undefined;
	const dataLines: string[] = [];

	for (const line of block.split('\n')) {
		if (line === '' || line.startsWith(':')) {
			continue;
		}

		const colon = line.indexOf(':');
		const field = colon === -1 ? line : line.slice(0, colon);
		let value = colon === -1 ? '' : line.slice(colon + 1);

		if (value.startsWith(' ')) {
			value = value.slice(1);
		}

		if (field === 'event') {
			event = value;
		}
		else if (field === 'data') {
			dataLines.push(value);
		}
		else if (field === 'id') {
			id = value;
		}
	}

	if (!dataLines.length) {
		return null;
	}

	return {data: dataLines.join('\n'), event, id};
}

export function createSseParser(onFrame: (frame: SseFrame) => void) {
	let buffer = '';

	return {
		/** Flush any trailing frame when the stream ends without a blank line. */
		end() {
			const rest = buffer.trim();

			buffer = '';

			if (rest) {
				const frame = parseBlock(rest);

				if (frame) {
					onFrame(frame);
				}
			}
		},

		push(chunk: string) {
			buffer += chunk.replace(/\r\n/g, '\n').replace(/\r/g, '\n');

			let index = buffer.indexOf('\n\n');

			while (index !== -1) {
				const block = buffer.slice(0, index);

				buffer = buffer.slice(index + 2);

				const frame = parseBlock(block);

				if (frame) {
					onFrame(frame);
				}

				index = buffer.indexOf('\n\n');
			}
		},
	};
}

/**
 * Read a streaming Response body and hand every decoded frame to onFrame.
 * Returns when the server closes the stream or the signal aborts.
 */
export async function readSseStream(
	response: Response,
	onFrame: (frame: SseFrame) => void
): Promise<void> {
	if (!response.body) {
		throw new Error('The broker returned a response without a body');
	}

	const decoder = new TextDecoder();
	const parser = createSseParser(onFrame);
	const reader = response.body.getReader();

	try {
		for (;;) {
			const {done, value} = await reader.read();

			if (done) {
				break;
			}

			parser.push(decoder.decode(value, {stream: true}));
		}

		parser.push(decoder.decode());
		parser.end();
	}
	finally {
		reader.releaseLock();
	}
}
