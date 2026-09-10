import ClayAlert from '@clayui/alert';
import React, {useEffect, useRef} from 'react';

import {TranscriptItem} from '../lib/events';
import ActivityLine from './ActivityLine';
import DeployCard from './DeployCard';

type Props = {
	items: TranscriptItem[];
};

function seconds(durationMs: number) {
	return `${Math.max(1, Math.round(durationMs / 1000))}s`;
}

export default function Transcript({items}: Props) {
	const endRef = useRef<HTMLDivElement>(null);

	useEffect(() => {
		endRef.current?.scrollIntoView({block: 'end'});
	}, [items]);

	if (!items.length) {
		return (
			<div className="aihub-transcript aihub-transcript-empty">
				<p>
					Describe what you want on your site in plain words. The
					assistant builds it, publishes it, and reports back here.
				</p>
			</div>
		);
	}

	return (
		<div className="aihub-transcript">
			{items.map((item) => {
				if (item.kind === 'prompt') {
					return (
						<div className="aihub-bubble aihub-bubble-user" key={item.key}>
							{item.text}
						</div>
					);
				}

				if (item.kind === 'assistant') {
					return (
						<div
							className="aihub-bubble aihub-bubble-assistant"
							key={item.key}
						>
							{item.text.split('\n').map((line, index) => (
								<p key={index}>{line}</p>
							))}
						</div>
					);
				}

				if (item.kind === 'activity') {
					return (
						<ActivityLine
							done={item.done}
							key={item.key}
							ok={item.ok}
							preview={item.preview}
							summary={item.summary}
						/>
					);
				}

				if (item.kind === 'deploy') {
					return (
						<DeployCard
							artifact={item.artifact}
							key={item.key}
							path={item.path}
							state={item.state}
						/>
					);
				}

				if (item.kind === 'error') {
					return (
						<ClayAlert displayType="danger" key={item.key} title="Error">
							{item.message}
						</ClayAlert>
					);
				}

				return (
					<div className="aihub-result" key={item.key}>
						{item.ok ? 'Done' : 'Stopped'} in {seconds(item.durationMs)}
					</div>
				);
			})}

			<div ref={endRef} />
		</div>
	);
}
