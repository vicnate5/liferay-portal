import ClayIcon from '@clayui/icon';
import React, {useState} from 'react';

import {spritemap} from '../lib/liferay';

type Props = {
	done: boolean;
	ok?: boolean;
	preview?: string;
	summary: string;
};

/**
 * One line of plain language for a tool call. The raw tool payload stays hidden
 * behind the toggle, and it is rendered as preformatted text rather than JSON
 * so a non developer is never handed a serialized object.
 */
export default function ActivityLine({done, ok, preview, summary}: Props) {
	const [open, setOpen] = useState(false);

	const icon = !done ? 'time' : ok === false ? 'exclamation-full' : 'check-circle';

	return (
		<div className={`aihub-activity ${done ? '' : 'aihub-activity-running'}`}>
			<button
				aria-expanded={open}
				className="aihub-activity-toggle"
				disabled={!preview}
				onClick={() => setOpen(!open)}
				type="button"
			>
				<ClayIcon spritemap={spritemap()} symbol={icon} />

				<span className="aihub-activity-summary">{summary}</span>

				{preview ? (
					<ClayIcon
						spritemap={spritemap()}
						symbol={open ? 'angle-down' : 'angle-right'}
					/>
				) : null}
			</button>

			{open && preview ? (
				<pre className="aihub-activity-detail">{preview}</pre>
			) : null}
		</div>
	);
}
