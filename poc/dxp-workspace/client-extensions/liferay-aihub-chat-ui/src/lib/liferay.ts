/**
 * The slice of the portal global this client extension relies on, plus a
 * standalone fallback so the bundle does not explode when it is loaded outside
 * DXP, for example in a storybook or a plain HTML harness.
 */

export type OAuth2UserAgentApplication = {
	clientId: string;
	homePageURL: string;
	redirectURIs: string[];
};

export type OAuth2Client = {
	fetch: (url: string, options?: RequestInit) => Promise<unknown>;
};

type SessionStorageTypes = {NECESSARY: string};

type LiferayGlobal = {
	OAuth2?: {
		getUserAgentApplication?: (
			externalReferenceCode: string
		) => OAuth2UserAgentApplication | undefined;
	};
	OAuth2Client?: {
		FromUserAgentApplication: (
			externalReferenceCode: string,
			debug?: boolean
		) => OAuth2Client | Promise<OAuth2Client>;
	};
	ThemeDisplay?: {
		getPathThemeImages: () => string;
		getPortalURL: () => string;
		isSignedIn: () => boolean;
	};
	Util?: {
		SessionStorage?: {
			TYPES: SessionStorageTypes;
			getItem: (key: string, type?: string) => string | null;
		};
	};
	authToken?: string;
};

declare global {
	interface Window {
		Liferay?: LiferayGlobal;
	}
}

export const Liferay: LiferayGlobal =
	(typeof window !== 'undefined' && window.Liferay) || {};

export function spritemap(): string {
	const images = Liferay.ThemeDisplay?.getPathThemeImages?.();

	return images ? `${images}/clay/icons.svg` : '/o/admin-theme/images/clay/icons.svg';
}
