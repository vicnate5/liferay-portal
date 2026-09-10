import ClayButton from '@clayui/button';
import ClayIcon from '@clayui/icon';
import React from 'react';

import {SessionSummary, STATE_LABELS} from '../lib/events';
import {spritemap} from '../lib/liferay';

type Props = {
	activeId: string | null;
	busy: boolean;
	onCreate: () => void;
	onSelect: (sessionId: string) => void;
	sessions: SessionSummary[];
};

export default function SessionList({
	activeId,
	busy,
	onCreate,
	onSelect,
	sessions,
}: Props) {
	return (
		<aside className="aihub-sidebar">
			<ClayButton
				block
				disabled={busy}
				displayType="primary"
				onClick={onCreate}
			>
				<ClayIcon spritemap={spritemap()} symbol="plus" /> New project
			</ClayButton>

			<ul className="aihub-session-list">
				{sessions.map((session) => (
					<li key={session.sessionId}>
						<button
							className={`aihub-session ${
								session.sessionId === activeId
									? 'aihub-session-active'
									: ''
							}`}
							onClick={() => onSelect(session.sessionId)}
							type="button"
						>
							<span className="aihub-session-title">
								{session.title}
							</span>

							<span className="aihub-session-state">
								{STATE_LABELS[session.state] ?? session.state}
							</span>
						</button>
					</li>
				))}
			</ul>

			{sessions.length ? null : (
				<p className="aihub-sidebar-empty">
					No projects yet. Start one to begin.
				</p>
			)}
		</aside>
	);
}
