import ClayAlert from '@clayui/alert';
import React, {useCallback, useEffect, useMemo, useRef, useState} from 'react';

import {getAuth} from './lib/auth';
import {BrokerClient} from './lib/broker';
import {
	applyEvent,
	BrokerEvent,
	promptItem,
	SessionState,
	SessionSummary,
	TranscriptItem,
} from './lib/events';
import PromptBox from './components/PromptBox';
import SessionList from './components/SessionList';
import StatusIndicator from './components/StatusIndicator';
import Transcript from './components/Transcript';

type Props = {
	brokerURL?: string | null;
};

const BUSY: SessionState[] = ['deploying', 'thinking', 'working'];

export default function App({brokerURL}: Props) {
	const broker = useMemo(() => new BrokerClient(brokerURL), [brokerURL]);

	const [sessions, setSessions] = useState<SessionSummary[]>([]);
	const [activeId, setActiveId] = useState<string | null>(null);
	const [items, setItems] = useState<TranscriptItem[]>([]);
	const [state, setState] = useState<SessionState>('idle');
	const [notice, setNotice] = useState<string | null>(null);
	const [authNotice, setAuthNotice] = useState<string | null>(null);
	const [loading, setLoading] = useState(true);

	const abortRef = useRef<AbortController | null>(null);

	const busy = BUSY.includes(state);

	const ingest = useCallback((event: BrokerEvent) => {
		if (event.event === 'status') {
			setState(event.data.state);
		}

		setItems((current) => applyEvent(current, event));
	}, []);

	// Report the authentication mode once. An anonymous mode is legitimate,
	// since the broker can run with authentication disabled.

	useEffect(() => {
		let live = true;

		getAuth().then((auth) => {
			if (live && auth.mode === 'anonymous') {
				setAuthNotice(
					auth.reason
						? `Signed in access is not available: ${auth.reason}. Continuing without a token.`
						: 'Continuing without a token.'
				);
			}
		});

		return () => {
			live = false;
		};
	}, []);

	useEffect(() => {
		let live = true;

		broker
			.listSessions()
			.then((list) => {
				if (!live) {
					return;
				}

				setSessions(list);

				if (list.length && !activeId) {
					setActiveId(list[0].sessionId);
				}
			})
			.catch((error: Error) => live && setNotice(error.message))
			.finally(() => live && setLoading(false));

		return () => {
			live = false;
		};

		// activeId is intentionally excluded. This effect seeds the list once.
		// eslint-disable-next-line react-hooks/exhaustive-deps
	}, [broker]);

	// Replay the persisted transcript, then attach to the live stream. Replay
	// runs first so a reload lands on the same transcript the reader left.

	useEffect(() => {
		if (!activeId) {
			setItems([]);

			return;
		}

		const controller = new AbortController();

		abortRef.current = controller;

		let live = true;

		setItems([]);
		setState('idle');

		broker
			.replay(activeId)
			.then((history) => {
				if (!live) {
					return;
				}

				let next: TranscriptItem[] = [];
				let lastState: SessionState = 'idle';

				for (const event of history) {
					if (event.event === 'status') {
						lastState = event.data.state;
					}

					next = applyEvent(next, event);
				}

				setItems(next);
				setState(lastState);

				return broker.stream(activeId, ingest, controller.signal);
			})
			.catch((error: Error) => {
				if (live && controller.signal.aborted === false) {
					setNotice(error.message);
				}
			});

		return () => {
			live = false;
			controller.abort();
		};
	}, [activeId, broker, ingest]);

	useEffect(() => {
		setSessions((current) =>
			current.map((session) =>
				session.sessionId === activeId ? {...session, state} : session
			)
		);
	}, [activeId, state]);

	const createSession = async () => {
		setNotice(null);

		try {
			const created = await broker.createSession(
				`Project ${sessions.length + 1}`
			);

			setSessions((current) => [created, ...current]);
			setActiveId(created.sessionId);
		}
		catch (error) {
			setNotice((error as Error).message);
		}
	};

	const submit = async (text: string) => {
		if (!activeId) {
			return;
		}

		setNotice(null);
		setItems((current) => [...current, promptItem(text)]);
		setState('thinking');

		try {
			await broker.sendMessage(activeId, text);
		}
		catch (error) {
			setState('idle');
			setNotice((error as Error).message);
		}
	};

	const interrupt = async () => {
		if (!activeId) {
			return;
		}

		try {
			await broker.interrupt(activeId);
			setState('idle');
		}
		catch (error) {
			setNotice((error as Error).message);
		}
	};

	return (
		<div className="aihub-root">
			<header className="aihub-header">
				<h2 className="aihub-title">AI Hub Workspace</h2>

				<StatusIndicator state={state} />
			</header>

			<div className="aihub-body">
				<SessionList
					activeId={activeId}
					busy={loading}
					onCreate={createSession}
					onSelect={setActiveId}
					sessions={sessions}
				/>

				<main className="aihub-main">
					{authNotice ? (
						<ClayAlert displayType="info" title="Note">
							{authNotice}
						</ClayAlert>
					) : null}

					{notice ? (
						<ClayAlert
							displayType="danger"
							onClose={() => setNotice(null)}
							title="Error"
						>
							{notice}
						</ClayAlert>
					) : null}

					<Transcript items={items} />

					<PromptBox
						busy={busy}
						disabled={!activeId}
						onInterrupt={interrupt}
						onSubmit={submit}
					/>
				</main>
			</div>
		</div>
	);
}
