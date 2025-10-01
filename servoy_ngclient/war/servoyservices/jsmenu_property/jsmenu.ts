angular.module('jsmenu_property', ['webSocketModule'])
// JSMenu type -------------------------------------------
.run(function ($typesRegistry: sablo.ITypesRegistry) {
	// not implemented in ng1, just for Titanium NG at this point
	var jsMenuType: sablo.IType<any> =  {
			fromServerToClient: function (serverJSONValue: any, _currentClientValue: any, _componentScope: angular.IScope, _propertyContext: sablo.IPropertyContext): String {
				return serverJSONValue;
			},

			fromClientToServer: function (_newClientData: any, _oldClientData: any, _scope: angular.IScope, _propertyContext: sablo.IPropertyContext): any {
				return null; // should never happen; this is not supported
			},
			
			updateAngularScope: function (_clientValue: any, _componentScope: angular.IScope): void {
				// nothing to do here
			}

		};
	
	$typesRegistry.registerGlobalType('JSMenu', jsMenuType, false);
})

