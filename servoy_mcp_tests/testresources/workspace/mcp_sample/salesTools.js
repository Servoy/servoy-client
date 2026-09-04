/**
 * Tools over real data, for SVY-21326.
 *
 * A second scope, which also proves the scan covers more than one. Where myScope
 * exercises the parameter types, these do actual work against the example_data
 * server - 91 customers, 830 orders, 77 products - so an agent has something worth
 * reasoning about rather than a greeting.
 *
 * Queries go through databaseManager.getDataSetByQuery, which needs no datasource
 * configured on the solution.
 *
 * Every tool returns JSON as text. A tool is free to return whatever it likes; if an
 * agent cannot make sense of it, that is between the tool author and the agent.
 */

/**
 * Finds customers whose company or contact name contains the given text.
 *
 * @Tool
 *
 * @param {String} nameContains part of a company or contact name, case insensitive
 * @param {Number} [maxResults] how many to return at most, defaults to 10
 *
 * @return {String}
 *
 * @properties={typeid:24,uuid:"5213DBF8-EBF1-43E0-843E-4E6C017D3053"}
 */
function findCustomers(nameContains, maxResults) {
	var limit = maxResults ? Number(maxResults) : 10;

	var dataset = databaseManager.getDataSetByQuery('example_data',
		'SELECT customerid, companyname, contactname, city, country' +
		'  FROM customers' +
		' WHERE lower(companyname) LIKE lower(?) OR lower(contactname) LIKE lower(?)' +
		' ORDER BY companyname',
		['%' + nameContains + '%', '%' + nameContains + '%'], limit);

	return JSON.stringify(toRows(dataset, ['customerId', 'company', 'contact', 'city', 'country']));
}

/**
 * Lists the orders of one customer, most recent first.
 *
 * @Tool
 *
 * @param {String} customerId the customer id, as returned by findCustomers
 * @param {Number} [maxResults] how many to return at most, defaults to 20
 *
 * @return {String}
 *
 * @properties={typeid:24,uuid:"FED8D842-552D-4899-8D25-F8C3BC2266E2"}
 */
function getCustomerOrders(customerId, maxResults) {
	var limit = maxResults ? Number(maxResults) : 20;

	var dataset = databaseManager.getDataSetByQuery('example_data',
		'SELECT o.orderid, o.orderdate, o.shippeddate, o.shipcountry,' +
		'       round(sum(d.unitprice * d.quantity * (1 - d.discount))::numeric, 2) AS total' +
		'  FROM orders o JOIN order_details d ON d.orderid = o.orderid' +
		' WHERE o.customerid = ?' +
		' GROUP BY o.orderid, o.orderdate, o.shippeddate, o.shipcountry' +
		' ORDER BY o.orderdate DESC',
		[customerId], limit);

	return JSON.stringify(toRows(dataset, ['orderId', 'orderDate', 'shippedDate', 'shipCountry', 'total']));
}

/**
 * The best selling products by revenue.
 *
 * @Tool
 *
 * @param {Number} [maxResults] how many to return at most, defaults to 10
 *
 * @return {String}
 *
 * @properties={typeid:24,uuid:"C9FF813C-651C-47DC-9E84-F0EEEEA52611"}
 */
function getTopProducts(maxResults) {
	var limit = maxResults ? Number(maxResults) : 10;

	var dataset = databaseManager.getDataSetByQuery('example_data',
		'SELECT p.productname,' +
		'       sum(d.quantity) AS units,' +
		'       round(sum(d.unitprice * d.quantity * (1 - d.discount))::numeric, 2) AS revenue' +
		'  FROM order_details d JOIN products p ON p.productid = d.productid' +
		' GROUP BY p.productname' +
		' ORDER BY revenue DESC',
		null, limit);

	return JSON.stringify(toRows(dataset, ['product', 'units', 'revenue']));
}

/**
 * Counts the rows of a table on the example_data server.
 *
 * @Tool
 *
 * @param {String} tableName the table to count
 *
 * @return {Number}
 *
 * @properties={typeid:24,uuid:"A60EE077-1E07-4033-BD2A-EE308FBCA3EA"}
 */
function countRows(tableName) {
	// the name is concatenated because a table name cannot be a bind parameter;
	// it is checked against the catalogue first so this cannot become an injection
	var known = databaseManager.getDataSetByQuery('example_data',
		"SELECT count(*) FROM information_schema.tables" +
		" WHERE table_schema = 'public' AND table_name = ?", [tableName], 1);

	if (Number(known.getValue(1, 1)) === 0) {
		throw new Error('no such table on example_data: ' + tableName);
	}

	var dataset = databaseManager.getDataSetByQuery('example_data',
		'SELECT count(*) FROM ' + tableName, null, 1);

	return Number(dataset.getValue(1, 1));
}

/**
 * Reports what the server knows about this call: the solution, the user the bearer
 * token identified, the permissions and the tenant filter.
 *
 * Useful as an agent's first call, and it is the tool that proves the token reached
 * the client session - without the login the user comes back empty.
 *
 * @Tool
 *
 * @return {String}
 *
 * @properties={typeid:24,uuid:"C3AED099-0145-4243-87CC-118D54A0E3C2"}
 */
function describeSession() {
	return JSON.stringify({
		solution: application.getSolutionName(),
		userName: security.getUserName(),
		userUID: security.getUserUID(),
		tenant: security.getTenantValue(),
		serverTime: new Date().toISOString()
	});
}

/**
 * Turns a dataset into an array of objects, so the JSON an agent receives has names
 * rather than positions.
 *
 * Not a tool: no marker, so it is never published.
 *
 * @param {JSDataSet} dataset
 * @param {Array<String>} names
 *
 * @return {Array<Object>}
 *
 * @properties={typeid:24,uuid:"8B239A8F-0595-479E-A4FF-AA12026C910F"}
 */
function toRows(dataset, names) {
	var rows = [];

	for (var r = 1; r <= dataset.getMaxRowIndex(); r++) {
		var row = {};
		for (var c = 0; c < names.length; c++) {
			var value = dataset.getValue(r, c + 1);
			row[names[c]] = value === null ? null : (value instanceof Date ? value.toISOString() : value);
		}
		rows.push(row);
	}

	return rows;
}

/**
 * Lists the demo items visible to the caller.
 *
 * The query asks for every row, with no where clause. What comes back is whatever the
 * tenant filter allows, because mcp_tenant_demo.tenant_name is a Tenant column - so this
 * tool shows, without being told anything about the user, that two agents holding two
 * different tokens see two different databases.
 *
 * It is built with QBSelect rather than a SQL string on purpose: a table filter is applied
 * to queries Servoy builds, and getDataSetByQuery with raw SQL goes straight to the
 * database, tenant column or not.
 *
 * @Tool
 *
 * @return {Array<Object>}
 *
 * @properties={typeid:24,uuid:"19C0DBD9-A97C-445F-8BBD-5EDCAAF168D3"}
 */
function listMyItems() {
	var query = databaseManager.createSelect('db:/example_data/mcp_tenant_demo');
	query.result.add(query.columns.tenant_name).add(query.columns.item_name);
	query.sort.add(query.columns.item_name.asc);

	var dataset = databaseManager.getDataSetByQuery(query, 100);

	return toRows(dataset, ['tenant_name', 'item_name']);
}
