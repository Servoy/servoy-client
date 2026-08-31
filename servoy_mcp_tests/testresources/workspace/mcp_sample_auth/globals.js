/**
 * The tokens this authenticator accepts, and who each one stands for.
 *
 * Hardcoded on purpose: this is a test authenticator. A real one would look the token
 * up in Servoy Cloud, in an identity provider, or in a table of API keys. Nothing about
 * the MCP server changes when it does - the server never reads the token, it only asks
 * this module who the bearer is.
 *
 * @type {Object}
 *
 * @properties={typeid:35,uuid:"2D442E81-D96F-4C4B-A82E-EC2209A63FA0",variableType:-4}
 */
var KNOWN_TOKENS = {
	'mcp-demo-token-alice': {
		userName: 'alice',
		userUid: 'alice-uid',
		permissions: ['mcp_user'],
		tenant: 'acme'
	},
	'mcp-demo-token-bob': {
		userName: 'bob',
		userUid: 'bob-uid',
		permissions: ['mcp_user'],
		tenant: 'globex'
	}
};

/**
 * Identifies the bearer of an MCP token.
 *
 * The MCP server calls this with no client id, the same way the stateless login page does,
 * and reads back the user and the tenant this method establishes. Returning without calling
 * security.login() is how this module says no: the server then answers the agent with 401.
 *
 * @param {String} arg unused - always null on this path
 * @param {Object} parameters carries userToken, the bearer token exactly as the agent sent it
 *
 * @return {Object} a message for the caller, readable by the agent whether or not it was let in
 *
 * @properties={typeid:24,uuid:"F74D482A-0D22-4258-9F5B-FC77E7D37970"}
 */
function onOpen(arg, parameters) {
	var token = parameters ? parameters.userToken : null;
	if (!token) {
		return { error: 'No userToken was supplied' };
	}

	var identity = KNOWN_TOKENS[token];
	if (!identity) {
		// deliberately vague: an agent should not learn which tokens exist by guessing
		return { error: 'Unknown token' };
	}

	if (!security.login(identity.userName, identity.userUid, identity.permissions)) {
		return { error: 'Could not log in ' + identity.userName };
	}

	// what makes queries come back filtered for this tenant and no other
	security.setTenantValue(identity.tenant);

	return { user: identity.userName, tenant: identity.tenant };
}
