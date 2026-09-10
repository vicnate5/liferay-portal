/**
 * The broker event contract, as declared in poc/CONTRACT.md, plus the reducer
 * that turns a stream of events into the transcript the reader sees.
 */

export type SessionState =
	| 'deploying'
	| 'error'
	| 'idle'
	| 'thinking'
	| 'working';

export type SessionSummary = {
	createdAt: string;
	sessionId: string;
	state: SessionState;
	title: string;
};

export type AssistantPayload = {text: string};
export type ToolUsePayload = {id: string; input: unknown; name: string};
export type ToolResultPayload = {id: string; ok: boolean; preview: string};
export type StatusPayload = {state: SessionState};
export type DeployPayload = {
	artifact: string;
	path: string;
	state: 'copied' | 'failed' | 'started';
};
export type ResultPayload = {
	costUsd: number;
	durationMs: number;
	ok: boolean;
	turns: number;
};
export type ErrorPayload = {message: string};

export type BrokerEvent =
	| {data: AssistantPayload; event: 'assistant'}
	| {data: DeployPayload; event: 'deploy'}
	| {data: ErrorPayload; event: 'error'}
	| {data: ResultPayload; event: 'result'}
	| {data: StatusPayload; event: 'status'}
	| {data: ToolResultPayload; event: 'tool_result'}
	| {data: ToolUsePayload; event: 'tool_use'};

export type TranscriptItem =
	| {
			done: boolean;
			key: string;
			kind: 'activity';
			ok?: boolean;
			preview?: string;
			summary: string;
			toolId: string;
			toolName: string;
	  }
	| {artifact: string; key: string; kind: 'deploy'; path: string; state: DeployPayload['state']}
	| {key: string; kind: 'assistant'; text: string}
	| {key: string; kind: 'error'; message: string}
	| {key: string; kind: 'prompt'; text: string}
	| {
			costUsd: number;
			durationMs: number;
			key: string;
			kind: 'result';
			ok: boolean;
			turns: number;
	  };

let counter = 0;

function nextKey(prefix: string) {
	counter += 1;

	return `${prefix}-${counter}`;
}

export function promptItem(text: string): TranscriptItem {
	return {key: nextKey('prompt'), kind: 'prompt', text};
}

/**
 * Fold one broker event into the transcript. Assistant deltas append to the
 * trailing assistant bubble, tool results merge into the activity line opened
 * by their tool_use, and a deploy update replaces the matching deploy card so
 * the reader sees one moving progress row rather than three stacked rows.
 */
export function applyEvent(
	items: TranscriptItem[],
	frame: BrokerEvent
): TranscriptItem[] {
	if (frame.event === 'assistant') {
		const last = items[items.length - 1];

		if (last && last.kind === 'assistant') {
			const merged: TranscriptItem = {
				...last,
				text: last.text + frame.data.text,
			};

			return [...items.slice(0, -1), merged];
		}

		return [
			...items,
			{key: nextKey('assistant'), kind: 'assistant', text: frame.data.text},
		];
	}

	if (frame.event === 'tool_use') {
		return [
			...items,
			{
				done: false,
				key: nextKey('activity'),
				kind: 'activity',
				summary: describeToolUse(frame.data),
				toolId: frame.data.id,
				toolName: frame.data.name,
			},
		];
	}

	if (frame.event === 'tool_result') {
		const index = findLastIndex(
			items,
			(item) =>
				item.kind === 'activity' &&
				item.toolId === frame.data.id &&
				!item.done
		);

		if (index === -1) {
			return items;
		}

		const target = items[index];

		if (target.kind !== 'activity') {
			return items;
		}

		const updated: TranscriptItem = {
			...target,
			done: true,
			ok: frame.data.ok,
			preview: frame.data.preview,
		};

		return [...items.slice(0, index), updated, ...items.slice(index + 1)];
	}

	if (frame.event === 'deploy') {
		const index = findLastIndex(
			items,
			(item) =>
				item.kind === 'deploy' &&
				item.artifact === frame.data.artifact &&
				item.state !== 'copied' &&
				item.state !== 'failed'
		);

		const card: TranscriptItem = {
			artifact: frame.data.artifact,
			key: index === -1 ? nextKey('deploy') : items[index].key,
			kind: 'deploy',
			path: frame.data.path,
			state: frame.data.state,
		};

		if (index === -1) {
			return [...items, card];
		}

		return [...items.slice(0, index), card, ...items.slice(index + 1)];
	}

	if (frame.event === 'result') {
		return [
			...items,
			{
				costUsd: frame.data.costUsd,
				durationMs: frame.data.durationMs,
				key: nextKey('result'),
				kind: 'result',
				ok: frame.data.ok,
				turns: frame.data.turns,
			},
		];
	}

	if (frame.event === 'error') {
		return [
			...items,
			{key: nextKey('error'), kind: 'error', message: frame.data.message},
		];
	}

	return items;
}

function findLastIndex<T>(list: T[], predicate: (value: T) => boolean) {
	for (let i = list.length - 1; i >= 0; i--) {
		if (predicate(list[i])) {
			return i;
		}
	}

	return -1;
}

/**
 * Turn a raw tool call into one plain sentence. The reader is not a developer,
 * so no tool name, no file path argument dump, and never raw JSON.
 */
export function describeToolUse(payload: ToolUsePayload): string {
	const input = (payload.input ?? {}) as Record<string, unknown>;
	const text = (key: string) =>
		typeof input[key] === 'string' ? (input[key] as string) : '';

	const fileName = (value: string) => {
		const parts = value.split('/').filter(Boolean);

		return parts.length ? parts[parts.length - 1] : value;
	};

	switch (payload.name) {
		case 'Bash': {
			const command = text('command');

			if (command.includes('gradlew') && command.includes('deploy')) {
				return 'Building and deploying the application';
			}

			if (command.includes('gradlew')) {
				return 'Compiling the project';
			}

			return text('description') || 'Running a build step';
		}
		case 'Edit':
		case 'MultiEdit':
		case 'NotebookEdit':
			return `Updating ${fileName(text('file_path')) || 'a project file'}`;
		case 'Glob':
		case 'Grep':
			return 'Looking through the project';
		case 'Read':
			return `Reading ${fileName(text('file_path')) || 'a project file'}`;
		case 'Skill':
			return `Applying the ${text('skill') || 'Liferay'} guidance`;
		case 'Task':
			return 'Delegating part of the work';
		case 'TodoWrite':
			return 'Updating its plan';
		case 'WebFetch':
		case 'WebSearch':
			return 'Checking the documentation';
		case 'Write':
			return `Creating ${fileName(text('file_path')) || 'a project file'}`;
		default:
			return 'Working on the project';
	}
}

export const STATE_LABELS: Record<SessionState, string> = {
	deploying: 'Deploying to your site',
	error: 'Stopped on an error',
	idle: 'Ready',
	thinking: 'Thinking',
	working: 'Working on it',
};
