/// <reference path="../foundset_custom_property/foundset.ts" />

angular.module('record_property', ['webSocketModule'])

.run(function ($typesRegistry: sablo.ITypesRegistryForSabloConverters, $foundsetTypeConstants: foundsetType.FoundsetTypeConstants) {
	$typesRegistry.registerGlobalType('record', new RecordType($foundsetTypeConstants), false);
})

class RecordType implements sablo.IType<any> {
	
	constructor(private foundsetTypeConstants: foundsetType.FoundsetTypeConstants) {}
	
	fromServerToClient(serverJSONValue: any, currentClientValue: any, scope: angular.IScope, propertyContext: sablo.IPropertyContext): any {
		// no conversions to be done here; server does support sending records to client but just as references via hashes / and pk hints
		// that currently are not automatically transformed on client into a RowValue instance...
		return serverJSONValue;
	}

	fromClientToServer(newClientData: any, oldClientData: any, scope: angular.IScope, propertyContext: sablo.IPropertyContext): any {
		if (newClientData instanceof ngclient.propertyTypes.RowValue) {
			var recordRef = {};
			recordRef[this.foundsetTypeConstants.ROW_ID_COL_KEY] = newClientData.getId();
			recordRef[ngclient.propertyTypes.FoundsetType.FOUNDSET_ID] = newClientData.getFoundset().getId();
			return recordRef;
		}
		return newClientData;
	}
	
	updateAngularScope(clientValue: any, componentScope: angular.IScope): void {
		// nothing to do here
	}
	
}