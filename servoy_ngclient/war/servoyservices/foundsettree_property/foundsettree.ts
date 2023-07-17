angular.module('foundsettree_property', ['webSocketModule'])
// Date type -------------------------------------------
.run(function ($typesRegistry: sablo.ITypesRegistry) {
	// not implemented in ng1, just for ng2 at this point
	var foundsetTreeType: sablo.IType<any> =  {
			fromServerToClient: function (serverJSONValue: any, currentClientValue: any, componentScope: angular.IScope, propertyContext: sablo.IPropertyContext): String {
				return serverJSONValue;
			},

			fromClientToServer: function (newClientData: any, oldClientData: any, scope: angular.IScope, propertyContext: sablo.IPropertyContext): any {
				return null; // should never happen; this is not supported
			},
			
			updateAngularScope: function (clientValue: any, componentScope: angular.IScope): void {
				// nothing to do here
			}

		}
	
	$typesRegistry.registerGlobalType('foundsettree', foundsetTreeType, false);
})

