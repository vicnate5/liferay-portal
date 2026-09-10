import React from 'react';
import {createRoot, Root} from 'react-dom/client';

import App from './App';
import {ELEMENT_NAME} from './config';

import './styles/index.css';

class AIHubChatElement extends HTMLElement {
	private _root?: Root;

	connectedCallback() {
		this._root = createRoot(this);
		this._root.render(
			<App brokerURL={this.getAttribute('broker-url')} />
		);
	}

	disconnectedCallback() {
		this._root?.unmount();
		this._root = undefined;
	}
}

if (!customElements.get(ELEMENT_NAME)) {
	customElements.define(ELEMENT_NAME, AIHubChatElement);
}
