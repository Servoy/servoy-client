/// <reference path="../foundset_custom_property/foundset.ts" />

angular.module('dataset_property', ['webSocketModule'])

.run(function ($typesRegistry: sablo.ITypesRegistryForSabloConverters, $sabloConverters: sablo.ISabloConverters) {
	$typesRegistry.registerGlobalType('dataset', new DatasetType($typesRegistry, $sabloConverters), false);
})

class DatasetType implements sablo.IType<any> {
	
	private static readonly VALUE_KEY = "v";
	private static readonly TYPES_KEY = "t";
	private static readonly INCLUDES_COLUMN_NAMES_KEY = "i";

	constructor(private readonly typesRegistry: sablo.ITypesRegistryForSabloConverters,
			private readonly sabloConverters: sablo.ISabloConverters) {}
	
	fromServerToClient(serverJSONValue: any, currentClientValue: any, scope: angular.IScope, propertyContext: sablo.IPropertyContext): any {
		let datasetValue: any = serverJSONValue;

		if (datasetValue) {
			let columnTypesFromServer: { [columnIndex: string]: sablo.ITypeFromServer } = datasetValue[DatasetType.TYPES_KEY];
			let columnTypes: { [columnIndex: string]: sablo.IType<any> };
			
			datasetValue = datasetValue[DatasetType.VALUE_KEY];
			
			if (columnTypesFromServer) {
				// find the actual client side types
				columnTypes = {};
				for (let colIdx of Object.getOwnPropertyNames(columnTypesFromServer))
					columnTypes[colIdx] = this.typesRegistry.getAlreadyRegisteredType(columnTypesFromServer[colIdx]);
			}
				
			let rowNo = (datasetValue[DatasetType.INCLUDES_COLUMN_NAMES_KEY] ? 1 : 0); // first row might be just the column names; those don't need any server-to-client conversions and shouldn't use column conversions on them
			while (rowNo < datasetValue.length) {
				let row:[any] = datasetValue[rowNo];
				row.forEach((cellValue: any, columnIndex: number) => {
					// apply either default conversion or the one from spec (for each column) if present
					row[columnIndex] = this.sabloConverters.convertFromServerToClient(cellValue, columnTypes ? columnTypes[columnIndex] : undefined, undefined,
			        		undefined, undefined, scope, propertyContext);
				});
				rowNo++;
			}
		}
		return datasetValue;
	}

	fromClientToServer(newClientData: any, oldClientData: any, scope: angular.IScope, propertyContext: sablo.IPropertyContext): any {
		// not supported
		return newClientData;
	}
	
	updateAngularScope(clientValue: any, componentScope: angular.IScope): void {
		// nothing to do here
	}
	
}