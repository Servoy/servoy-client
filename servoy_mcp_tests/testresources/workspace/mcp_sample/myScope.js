 /**
 * Returns a greeting for the given name.
 *
 * @Tool
 *
 * @param {String} name the name to greet
 *
 * @return {String}
 *
 * @properties={typeid:24,uuid:"376DB686-E2B7-414B-B2E9-B3B71DE1F8C5"}
 */
function test_scope_function(name) {
      return 'Hello, ' + name + '!';
}

/**
 * Adds two numbers. Exercises the Number parameter type.
 *
 * @Tool
 *
 * @param {Number} first the first number
 * @param {Number} second the second number
 *
 * @return {Number}
 *
 * @properties={typeid:24,uuid:"579FE897-1369-4480-B8EB-FC572FFCBA88"}
 */
function add(first, second) {
	return Number(first) + Number(second);
}

/**
 * Echoes back what it was given, so it is visible what actually arrives in the
 * scope function. Exercises the JSON parameter type and an optional parameter.
 *
 * @Tool
 *
 * @param {JSON} payload any JSON value
 * @param {String} [note] an optional note
 *
 * @return {String}
 *
 * @properties={typeid:24,uuid:"123F280B-0A70-4EBA-8A88-966F3BA350EB"}
 */
function echo(payload, note) {
	return JSON.stringify({
		payload: payload,
		payloadType: typeof payload,
		note: note === undefined ? null : note
	});
}

/**
 * Reports who the call is running as. This is the one that proves the bearer token
 * reached the client session: without the login the user is empty.
 *
 * @Tool
 *
 * @return {String}
 *
 * @properties={typeid:24,uuid:"C0E8D37A-4C1B-40A6-85EA-2B9799EF91B1"}
 */
function whoAmI() {
	return JSON.stringify({
		userName: security.getUserName(),
		userUID: security.getUserUID(),
		// getPermissions() hands back a JSDataSet, and JSON.stringify makes nonsense of one -
		// the column is what a reader wants anyway
		permissions: security.getPermissions().getColumnAsArray(2),
		tenant: security.getTenantValue()
	});
}

/**
 * Deliberately declares an unsupported parameter type. A tool parameter must be
 * String, Number or JSON, so this one must NOT appear in tools/list, and the server
 * log must say why.
 *
 * @Tool
 *
 * @param {Date} when a date, which is not a supported tool parameter type
 *
 * @return {String}
 *
 * @properties={typeid:24,uuid:"38BC3DF7-146C-48B7-B2F5-1FB0EFC3B94B"}
 */
function unsupportedParameterType(when) {
	return 'this tool should never be published: ' + when;
}
