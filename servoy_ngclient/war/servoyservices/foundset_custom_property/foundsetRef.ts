/// <reference path="../foundset_custom_property/foundset.ts" />

angular.module('foundset_ref_property', ['webSocketModule'])

.run(function ($typesRegistry: sablo.ITypesRegistryForSabloConverters) {
	$typesRegistry.registerGlobalType('foundsetRef', new FoundsetRefType(), false);
})

class FoundsetRefType implements sablo.IType<any> {
	
	constructor() {}
	
	fromServerToClient(serverJSONValue: any, currentClientValue: any, scope: angular.IScope, propertyContext: sablo.IPropertyContext): any {
		// no conversions to be done here; server does send the foundsetId and wiki documents it as such
		return serverJSONValue;
	}

	fromClientToServer(newClientData: any, oldClientData: any, scope: angular.IScope, propertyContext: sablo.IPropertyContext): any {
		if (newClientData instanceof ngclient.propertyTypes.FoundsetValue) return newClientData[ngclient.propertyTypes.FoundsetType.FOUNDSET_ID];
		return newClientData;
	}
	
	updateAngularScope(clientValue: any, componentScope: angular.IScope): void {
		// nothing to do here
	}
	
}