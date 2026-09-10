/**
 * Single source of truth for every value the chat UI needs to reach the broker.
 * Nothing else in this project hard codes a broker address.
 */

// The broker base URL default. This literal exists in exactly one place.
// Override it per page by setting the broker-url attribute on the custom
// element, for example <liferay-aihub-chat-ui broker-url="http://host:8091" />.
export const DEFAULT_BROKER_BASE_URL = 'http://localhost:8091';

// External reference code of the oauth-user-agent client extension that issues
// the DXP OAuth2 token for the broker. Must match the yaml key of
// client-extensions/liferay-aihub-oauth.
export const OAUTH_USER_AGENT_ERC = 'liferay-aihub-oauth';

export const ELEMENT_NAME = 'liferay-aihub-chat-ui';
