types: {

	foundset: {
		foundsetSelector: 'string',
		dataProviders: 'dataprovider[]'
	},

	component: {
		typeName: 'string',
		definition: 'componentDef',
		apiCallTypes: 'callType[]'
	},

	callType: {
		functionName: 'string',
		callOn: {type: 'int', values: [{ALL_RECORDS_IF_TEMPLATE: 0}, {ONE_SELECTED_RECORD_IF_TEMPLATE: 1}], default: 0}, 
	}

}
