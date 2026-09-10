import ClayLabel from '@clayui/label';
import ClayLoadingIndicator from '@clayui/loading-indicator';
import React from 'react';

import {SessionState, STATE_LABELS} from '../lib/events';

const DISPLAY: Record<
	SessionState,
	'danger' | 'info' | 'secondary' | 'success' | 'warning'
> = {
	deploying: 'warning',
	error: 'danger',
	idle: 'success',
	thinking: 'info',
	working: 'info',
};

type Props = {
	state: SessionState;
};

export default function StatusIndicator({state}: Props) {
	const busy = state === 'thinking' || state === 'working' || state === 'deploying';

	return (
		<span className="aihub-status">
			{busy ? <ClayLoadingIndicator displayType="secondary" size="sm" /> : null}

			<ClayLabel displayType={DISPLAY[state]}>{STATE_LABELS[state]}</ClayLabel>
		</span>
	);
}
