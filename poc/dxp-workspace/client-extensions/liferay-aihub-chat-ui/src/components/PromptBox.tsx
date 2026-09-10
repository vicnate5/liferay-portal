import ClayButton from '@clayui/button';
import ClayForm, {ClayInput} from '@clayui/form';
import ClayIcon from '@clayui/icon';
import React, {useState} from 'react';

import {spritemap} from '../lib/liferay';

type Props = {
	busy: boolean;
	disabled: boolean;
	onInterrupt: () => void;
	onSubmit: (text: string) => void;
};

export default function PromptBox({
	busy,
	disabled,
	onInterrupt,
	onSubmit,
}: Props) {
	const [text, setText] = useState('');

	const send = () => {
		const value = text.trim();

		if (!value || busy || disabled) {
			return;
		}

		setText('');
		onSubmit(value);
	};

	return (
		<ClayForm
			className="aihub-prompt"
			onSubmit={(event) => {
				event.preventDefault();
				send();
			}}
		>
			<ClayInput
				aria-label="Describe what you want built"
				component="textarea"
				disabled={disabled}
				onChange={(event) => setText(event.target.value)}
				onKeyDown={(event) => {
					if (event.key === 'Enter' && !event.shiftKey) {
						event.preventDefault();
						send();
					}
				}}
				placeholder="Describe what you want on your site"
				value={text}
			/>

			<div className="aihub-prompt-actions">
				{busy ? (
					<ClayButton
						displayType="secondary"
						onClick={onInterrupt}
						type="button"
					>
						<ClayIcon spritemap={spritemap()} symbol="times-circle" /> Stop
					</ClayButton>
				) : (
					<ClayButton disabled={disabled || !text.trim()} type="submit">
						Send
					</ClayButton>
				)}
			</div>
		</ClayForm>
	);
}
