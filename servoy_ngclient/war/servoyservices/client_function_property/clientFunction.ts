angular.module('client_function_property', ['webSocketModule'])
// Date type -------------------------------------------
.run(function ($typesRegistry: sablo.ITypesRegistry) {
	// we are here in NG1 code
	// "clientfunction" type is an enhancement only for NG2 (to avoid an eval on a tagstring prop)
	// in NG1 it remains and operates just like a "tagstring" property type serverside (and clientside it's just a string; probably the component/service will do an eval on in)
	// so we have nothing to convert here; this type is just added because server side sends type information for this property
	var clientFunctionType: sablo.IType<String> =  {
			fromServerToClient: function (serverJSONValue: String, currentClientValue: String, componentScope: angular.IScope, propertyContext: sablo.IPropertyContext): String {
				return serverJSONValue;
			},

			fromClientToServer: function (newClientData: String, oldClientData: String, scope: angular.IScope, propertyContext: sablo.IPropertyContext): any {
				return null; // should never happen; this is not supported
			},
			
			updateAngularScope: function (clientValue: String, componentScope: angular.IScope): void {
				// nothing to do here
			}

		}
	
	$typesRegistry.registerGlobalType('clientfunction', clientFunctionType, false);
})
