/**
 * Obtains a DXP OAuth2 bearer token for the broker through the
 * oauth-user-agent client extension, and degrades to anonymous when the token
 * cannot be obtained. The broker can run with authentication disabled, so a
 * missing token is a degraded mode rather than a failure.
 *
 * Two facts drive this implementation, both verified live against a running
 * DXP on 2026-09-08:
 *
 * 1. There is no `Liferay.OAuth2Client` global. The client ships as an ES
 *    module named `@liferay/oauth2-provider-web/client` in the portal import
 *    map, and it must be imported.
 * 2. `FromUserAgentApplication` returns a Promise, and the client it resolves
 *    to exposes only `fetch` publicly. That `fetch` parses the body as JSON and
 *    rejects on any non 2xx, so it cannot read the streaming body of
 *    GET /api/sessions/{id}/stream, nor tolerate the 202 from POST messages or
 *    the 204 from interrupt. The raw bearer token is therefore required, and
 *    `_getOrRequestToken` is the only accessor for it.
 *
 * `_getOrRequestToken` is private API. This is a POC shim. A production design
 * needs either a public token accessor on the portal side or a same origin
 * proxy that holds the token server side.
 */

import {OAUTH_USER_AGENT_ERC} from '../config';

const OAUTH_MODULE = '@liferay/oauth2-provider-web/client';

export type AuthState = {
	mode: 'anonymous' | 'authenticated';
	reason?: string;
	token: string | null;
};

type TokenResponse = {
	access_token?: string;
	expires_after_ms?: number;
};

type OAuth2ClientModule = {
	FromUserAgentApplication: (erc: string) => Promise<{
		_getOrRequestToken?: () => Promise<TokenResponse>;
	}>;
};

const ANONYMOUS: AuthState = {mode: 'anonymous', token: null};

let cached: Promise<AuthState> | null = null;

async function resolveAuth(): Promise<AuthState> {
	let module: OAuth2ClientModule;

	try {
		module = (await import(
			/* webpackIgnore: true */ OAUTH_MODULE
		)) as OAuth2ClientModule;
	}
	catch {
		return {
			...ANONYMOUS,
			reason: `The portal OAuth2 client module ${OAUTH_MODULE} is not available on this page`,
		};
	}

	try {
		const client = await module.FromUserAgentApplication(
			OAUTH_USER_AGENT_ERC
		);

		if (typeof client?._getOrRequestToken !== 'function') {
			return {
				...ANONYMOUS,
				reason: 'The portal OAuth2 client exposes no token accessor',
			};
		}

		const response = await client._getOrRequestToken();

		if (!response?.access_token) {
			return {
				...ANONYMOUS,
				reason: 'The portal did not issue an access token',
			};
		}

		return {mode: 'authenticated', token: response.access_token};
	}
	catch (error) {
		return {
			...ANONYMOUS,
			reason:
				error instanceof Error
					? error.message
					: 'The authorization flow failed',
		};
	}
}

export function getAuth(): Promise<AuthState> {
	if (!cached) {
		cached = resolveAuth();
	}

	return cached;
}

/** Drop the cached token so the next call repeats the authorization flow. */
export function resetAuth() {
	cached = null;
}
