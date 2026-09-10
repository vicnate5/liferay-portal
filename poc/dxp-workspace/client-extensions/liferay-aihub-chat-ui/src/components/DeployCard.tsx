import ClayIcon from '@clayui/icon';
import ClayProgressBar from '@clayui/progress-bar';
import React from 'react';

import {spritemap} from '../lib/liferay';

type Props = {
	artifact: string;
	path: string;
	state: 'copied' | 'failed' | 'started';
};

/**
 * A deploy takes tens of seconds. Without a loud, moving indicator the demo
 * reads as frozen, so this card is deliberately the most prominent element in
 * the transcript while the deploy runs.
 */
export default function DeployCard({artifact, path, state}: Props) {
	if (state === 'started') {
		return (
			<div className="aihub-deploy aihub-deploy-running">
				<div className="aihub-deploy-head">
					<ClayIcon spritemap={spritemap()} symbol="upload" />

					<strong>Publishing {artifact} to your site</strong>
				</div>

				<ClayProgressBar value={100} warn={false} />

				<p className="aihub-deploy-note">
					This usually takes about half a minute. You can keep reading
					while it finishes.
				</p>
			</div>
		);
	}

	if (state === 'failed') {
		return (
			<div className="aihub-deploy aihub-deploy-failed">
				<div className="aihub-deploy-head">
					<ClayIcon spritemap={spritemap()} symbol="exclamation-full" />

					<strong>Publishing {artifact} did not finish</strong>
				</div>

				<p className="aihub-deploy-note">
					The assistant will explain what went wrong in the message
					below.
				</p>
			</div>
		);
	}

	return (
		<div className="aihub-deploy aihub-deploy-done">
			<div className="aihub-deploy-head">
				<ClayIcon spritemap={spritemap()} symbol="check-circle-full" />

				<strong>{artifact} is live on your site</strong>
			</div>

			{path ? <p className="aihub-deploy-note">Delivered as {path}</p> : null}
		</div>
	);
}
