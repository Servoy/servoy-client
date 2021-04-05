angular.module('date_custom_property', ['webSocketModule'])
// Date type -------------------------------------------
.run(function ($typesRegistry: sablo.ITypesRegistry) {
	var dateType: sablo.IType<Date> =  {
			fromServerToClient: function (serverJSONValue: any, currentClientValue: Date, componentScope: angular.IScope, propertyContext: sablo.IPropertyContext): Date {
				var dateObj = moment(serverJSONValue).toDate();
				return dateObj;
			},

			fromClientToServer: function (newClientData: Date, oldClientData: Date, scope: angular.IScope, propertyContext: sablo.IPropertyContext): any {
				if (!newClientData) return null;

				var r = newClientData;
				if (typeof newClientData === 'string' || typeof newClientData === 'number') r = new Date(newClientData as number);
				if (isNaN(r.getTime())) throw new Error("Invalid date/time value: " + newClientData)// what should happen in this scenario , should we return null;
				return moment(r).format();
			},
			
			updateAngularScope: function (clientValue: Date, componentScope: angular.IScope): void {
				// nothing to do here
			}
			
		}
	
	$typesRegistry.registerGlobalType('svy_date', dateType, false);
	// also set the default conversion (overwrite it)
	$typesRegistry.registerGlobalType('Date', dateType, false);
})
