/// <reference path="../../../typings/angularjs/angular.d.ts" />
/// <reference path="../../../typings/sablo/sablo.d.ts" />

angular.module('foundset_viewport_module', ['webSocketModule'])
//Viewport reuse code module -------------------------------------------
.factory("$viewportModule", function($sabloConverters: sablo.ISabloConverters, $foundsetTypeConstants, $sabloUtils: sablo.ISabloUtils, $typesRegistry: sablo.ITypesRegistryForSabloConverters) {
	return new ngclient.propertyTypes.ViewportService($sabloConverters, $foundsetTypeConstants, $sabloUtils, $typesRegistry);
});

namespace ngclient.propertyTypes {
	
	export class ViewportService {
		private static readonly CONVERSIONS = "viewportConversions"; // data conversion info
		private static readonly CHANGED_IN_LINKED_PROPERTY = 9;
		private static readonly DATAPROVIDER_KEY = "dp";
		private static readonly VALUE_KEY = "value";
		
		public static readonly MAIN_TYPE = "mT";
		private static readonly COL_TYPES = "cT";
		private static readonly CELL_TYPES = "eT";
		private static readonly FOR_ROW_IDXS = "i";
		
		constructor(private readonly sabloConverters: sablo.ISabloConverters,
				private readonly foundsetTypeConstants,
				private readonly sabloUtils: sablo.ISabloUtils,
				private readonly typesRegistry: sablo.ITypesRegistryForSabloConverters) {}
	
		private addDataWatchToCell(columnName /*can be null*/, idx, viewPort, internalState, componentScope, dumbWatchType) {
			if (componentScope) {
				const queueChange = function queueChange(newData, oldData) {
					const r = {};
	
					if (angular.isDefined(internalState[this.foundsetTypeConstants.FOR_FOUNDSET_PROPERTY])) {
						r[this.foundsetTypeConstants.ROW_ID_COL_KEY] = internalState[this.foundsetTypeConstants.FOR_FOUNDSET_PROPERTY]().viewPort.rows[idx][this.foundsetTypeConstants.ROW_ID_COL_KEY];
					} else r[this.foundsetTypeConstants.ROW_ID_COL_KEY] = viewPort[idx][this.foundsetTypeConstants.ROW_ID_COL_KEY]; // if it doesn't have internalState[this.foundsetTypeConstants.FOR_FOUNDSET_PROPERTY] then it's probably the foundset property's viewport directly which has those in the viewport
					r[ViewportService.DATAPROVIDER_KEY] = columnName;
					r[ViewportService.VALUE_KEY] = newData;
	
					// convert new data if necessary
					const clientSideTypes = internalState[ViewportService.CONVERSIONS] ? internalState[ViewportService.CONVERSIONS][idx] : undefined;
					if (clientSideTypes && (!columnName || clientSideTypes[columnName]))
						r[ViewportService.VALUE_KEY] = this.sabloConverters.convertFromClientToServer(r[ViewportService.VALUE_KEY], columnName ? clientSideTypes[columnName] : clientSideTypes, oldData);
					else r[ViewportService.VALUE_KEY] = this.sabloUtils.convertClientObject(r[ViewportService.VALUE_KEY]);
	
					internalState.requests.push({viewportDataChanged: r});
					if (internalState.changeNotifier) internalState.changeNotifier();
				}
	
				const getCellValue = function getCellValue() { 
					return columnName == null ? viewPort[idx] : viewPort[idx][columnName]
				}; // viewport row can be just a value or an object of key/value pairs
	
				if (getCellValue() && getCellValue()[this.sabloConverters.INTERNAL_IMPL] && getCellValue()[this.sabloConverters.INTERNAL_IMPL].setChangeNotifier) {
					// smart property value
	
					// watch for change-by reference if needed
					if (typeof (dumbWatchType) !== 'undefined') internalState.unwatchData[idx].push(
							componentScope.$watch(getCellValue, (newData, oldData) => {
								if (newData !== oldData) { /* this doesn't seem to work correctly for 2 identical Date objects in Chrome when debugging; but it should */
									queueChange(newData, oldData);
								}
							})
					);
	
					// we don't care to check below for dumbWatchType because some types (see foundset) need to send internal protocol messages even if they are not watched/changeable on server
					getCellValue()[this.sabloConverters.INTERNAL_IMPL].setChangeNotifier(() => {
						if (getCellValue()[this.sabloConverters.INTERNAL_IMPL].isChanged()) queueChange(getCellValue(), getCellValue());
					});
				} else if (typeof (dumbWatchType) !== 'undefined') {
					// deep watch for change-by content / dumb value
					internalState.unwatchData[idx].push(
							componentScope.$watch(getCellValue, (newData, oldData) => {
								if (newData !== oldData) { /* this doesn't seem to work correctly for 2 identical Date objects in Chrome when debugging; but it should */
									let changed = false;
									if (typeof newData == "object") {
										const clientSideTypes = internalState[ViewportService.CONVERSIONS] ? internalState[ViewportService.CONVERSIONS][idx] : undefined;
										if (this.sabloUtils.isChanged(newData, oldData, clientSideTypes ? (columnName ? clientSideTypes[columnName] : clientSideTypes) : undefined)) {
											changed = true;
										}
									} else {
										changed = true;
									}
									if (changed) queueChange(newData, oldData);
								}
							}, dumbWatchType)
					);
				}
			}
		}
	
		// 1. dumbWatchMarkers when simpleRowValue === true means (undefined - no watch needed, true/false - deep/shallow watch)
		// 2. dumbWatchMarkers when simpleRowValue === false means (undefined - watch all cause there is no marker information,
		//                                                          { col1: true; col2: false } means watch type needed for each column and no watches for the ones not mentioned,
		//                                                          true/false directly means deep/shallow watch for all columns)
		private addDataWatchesToRow(idx, viewPort, internalState, componentScope, simpleRowValue/*not key/value pairs in each row*/, dumbWatchMarkers) {
			if (!angular.isDefined(internalState.unwatchData)) internalState.unwatchData = {};
			internalState.unwatchData[idx] = [];
			if (simpleRowValue) {
				this.addDataWatchToCell(null, idx, viewPort, internalState, componentScope, dumbWatchMarkers);
			} else {
				for (const columnName in viewPort[idx]) {
					if (columnName !== this.foundsetTypeConstants.ROW_ID_COL_KEY) 
						this.addDataWatchToCell(columnName, idx, viewPort, internalState, componentScope,
								(typeof dumbWatchMarkers === 'boolean' ? dumbWatchMarkers : (dumbWatchMarkers ? dumbWatchMarkers[columnName] : true)));
				}
			}
		}
	
		public addDataWatchesToRows(viewPort, internalState, componentScope, simpleRowValue/*not key/value pairs in each row*/, dumbWatchMarkers) {
			for (let i = viewPort.length - 1; i >= 0; i--) {
				this.addDataWatchesToRow(i, viewPort, internalState, componentScope, simpleRowValue, dumbWatchMarkers);
			}
		}
	
		private removeDataWatchesFromRow(idx, internalState) {
			if (internalState.unwatchData && internalState.unwatchData[idx]) {
				for (let j = internalState.unwatchData[idx].length - 1; j >= 0; j--)
					internalState.unwatchData[idx][j]();
				delete internalState.unwatchData[idx];
			}
		}
	
		public removeDataWatchesFromRows(rowCount, internalState) {
			for (let i = rowCount - 1; i >= 0; i--) {
				this.removeDataWatchesFromRow(i, internalState);
			}
		}
		
		// IMPORTANT: This comment should always match the comment and impl of ViewportClientSideTypes.getClientSideTypes() java method and the code in foundsetLinked.ts -> generateWholeViewportFromOneValue.
		// 
		// This is what we get as conversion info from server for viewports or viewport updates; indexes are relative to the received data (either full viewport ot viewport update data).
		// Basically the type of one cell in received data is the one in CELL_TYPES of COL_TYPES that matches FOR_ROW_IDXS; if that is not present it falls back to the main CONVERSION_CL_SIDE_TYPE_KEY
		// in that column, if that is not present it falls back to MAIN_TYPE. If it's missing completely then no data in the viewport needs client side conversions.
		//
		// * "_T": {
		// *  "mT": "date",
		// *  "cT": {
		// *     "b": { "_T": null},
		// *     "c": {`
		// *         "eT":
		// *           [
		// *             { "_T": null, "i": [4] },
		// *             { "_T": "zyx", "i": [2,5,9] },
		// *             {"_T": "xyz", "i": [0] }
		// *           ]
		// *       }
		// *   }
		// * }
		// * or if it is a single column (like for foundset linked properties it will be something like)
		// * "_T": {
		// *  "mT": "date",
		// *  "cT": {
		// *         "eT":
		// *           [
		// *             { "_T": null, "i": [4] },
		// *             { "_T": "zyx", "i": [2,5,9] },
		// *             {"_T": "xyz", "i": [0] }
		// *           ]
		// *   }
		// * }
		// *
		// * where
		// *   ISabloConverters.CONVERSION_CL_SIDE_TYPE_KEY   == "_T"
		// *   ViewportService.MAIN_TYPE       == "mT"
		// *   ViewportService.COL_TYPES       == "cT"
		// *   ViewportService.CELL_TYPES      == "eT"
		// *   ViewportService.FOR_ROW_IDXS    == "i"
		
		/**
		 * See comment above for what we get from server. We expand that / translate it to conversion info for each non-null cell conversion so that any future updates to the viewport can
		 * easily be merged (that is why we don't keep the server unexpanded format on client which could be used directly to determine each cell's type - to avoid complicated merges when we receive granular updates).<br/><br/>
		 * 
		 * This method also remembers the types and applies the conversions to given array of rows, returning the array of rows converted.<br/>
		 * The types are stored at indexes shifted with startIdxInViewportForRowsToBeConverted; old values are taken from the oldViewportRows shifted with the same number. 
		 * 
		 * @param rowsToBeConverted the data from server for received rows or row granular updates array.
		 * @return the converted-to-client rows for rowsToBeConverted param; it's actually the same reference as given param - to which any server->client conversions were also applied.
		 */
		private expandTypeInfoAndApplyConversions(serverConversionInfo: object, rowsToBeConverted: any[], startIdxInViewportForRowsToBeConverted: number,
		                             oldViewportRows: any[],  internalState, componentScope: angular.IScope, propertyContext: sablo.IPropertyContext,
		                             fullRowUpdates: boolean): any[] {
		                                                                                                                             
			if (serverConversionInfo && rowsToBeConverted) {
				rowsToBeConverted.forEach((rowData, index) => {
					if (rowData instanceof Object) {
						// with columns; so a foundset prop rows'
						let rowConversions = (fullRowUpdates || !internalState[ViewportService.CONVERSIONS] ? undefined : internalState[ViewportService.CONVERSIONS][startIdxInViewportForRowsToBeConverted + index]);
						Object.keys(rowData).forEach(columnName => {
							let cellConversion = this.getCellTypeFromServer(serverConversionInfo, index, columnName);
							
							// ignore null or undefined type of cell; otherwise remember it in expanded
							if (cellConversion) {
								if (! rowConversions) rowConversions = {};
								rowConversions[columnName] = cellConversion;
							} else if (rowConversions && rowConversions[columnName]) delete rowConversions[columnName];

							rowsToBeConverted[index][columnName] = this.sabloConverters.convertFromServerToClient(rowsToBeConverted[index][columnName],
									cellConversion,
									oldViewportRows ? (oldViewportRows[startIdxInViewportForRowsToBeConverted + index] ? (oldViewportRows[startIdxInViewportForRowsToBeConverted + index][columnName]) : undefined) : undefined,
									componentScope, propertyContext);
						});
						if (rowConversions && Object.keys(rowConversions).length == 0) rowConversions = undefined; // in case all conversion infos from one row were deleted due to the update
						this.updateRowTypes(startIdxInViewportForRowsToBeConverted + index, internalState, rowConversions);
					} else {
						// without columns, so a foundset linked prop rows'
						let cellConversion = this.getCellTypeFromServer(serverConversionInfo, index); // this is the whole row in this case (only one cell)
						
						rowsToBeConverted[index] = this.sabloConverters.convertFromServerToClient(rowsToBeConverted[index],
								cellConversion, oldViewportRows ? oldViewportRows[startIdxInViewportForRowsToBeConverted + index] : undefined, componentScope, propertyContext);
						this.updateRowTypes(startIdxInViewportForRowsToBeConverted + index, internalState, cellConversion);
					}
				});
			}
			
			return rowsToBeConverted;
		}
		
		private getCellTypeFromServer(serverConversionInfo, rowIndex: number, columnName?) {
			const PROCESSED_CELL_TYPES = "pct"; // just to turn stuff like [{ "_T": "zyx", "i": [2,3] }, {"_T": "xyz", "i": [9] }] into an easier to use { 2: "zyx", 3: "zyx", 9: "xyz" }
			const mainType = serverConversionInfo[ViewportService.MAIN_TYPE];
			const columnTypes = serverConversionInfo[ViewportService.COL_TYPES];
			
			let cellConversion;
			if (columnTypes) {
				let colType = (columnName ? columnTypes[columnName] : columnTypes); // foundset (multi col) or foundset linked (single col) data
				if (colType) {
						let processed = colType[PROCESSED_CELL_TYPES];
						if (!processed) {
							processed = colType[PROCESSED_CELL_TYPES] = {};
							if (colType[ViewportService.CELL_TYPES])
								colType[ViewportService.CELL_TYPES].forEach(
										value => value[ViewportService.FOR_ROW_IDXS].forEach(
												ri => processed[ri] = value[this.sabloConverters.CONVERSION_CL_SIDE_TYPE_KEY]));
						}
						
						cellConversion = processed[rowIndex]; // look at cell type; null is a valid type in what server can send so we check with === undefined
						if (cellConversion === undefined) cellConversion = colType[this.sabloConverters.CONVERSION_CL_SIDE_TYPE_KEY]; // fallback to main column type
						if (cellConversion === undefined) cellConversion = mainType; // fallback to main type
				} else cellConversion = mainType;
			} else cellConversion = mainType;
			
			if (cellConversion) return this.typesRegistry.getAlreadyRegisteredType(cellConversion);
			
			return cellConversion;
		}
	
		/**
		 * @param types can be one IType<?>  or an object of IType<?> for each column on that row.
		 */
		private updateRowTypes(idx, internalState, types : sablo.IType<any> | { [ colName: string ]: sablo.IType<any> }) {
			if (angular.isUndefined(internalState[ViewportService.CONVERSIONS])) {
				internalState[ViewportService.CONVERSIONS] = {};
			}
			internalState[ViewportService.CONVERSIONS][idx] = types;
		}
	
		/**
		 * I will update the whole viewport. More precisely it will apply all server-to-client conversions directly on given viewPortUpdate param and return it.
		 * @param viewPort old viewport
		 * @param viewPortUpdate the whole viewport update value from server
		 * 
		 * @return the given viewPortUpdate with conversions applied to it
		 */
		public updateWholeViewport(viewPort: any[], internalState, viewPortUpdate: any[], viewPortUpdateConversions: object, componentScope: angular.IScope, propertyContext: sablo.IPropertyContext): any[] {
			// update conversion info; expand what we get from server to be easy to use on client (like main type and main column type from JSON are kept at cell level)
			internalState[ViewportService.CONVERSIONS] = {};
			return this.expandTypeInfoAndApplyConversions(viewPortUpdateConversions, viewPortUpdate, 0, viewPort, internalState, componentScope, propertyContext, true);
		}
	
		// see comment above, before updateWholeViewport()
		public updateViewportGranularly(viewPort: any[], internalState, rowUpdates: any[], componentScope: angular.IScope,
		         propertyContext: sablo.IPropertyContext, simpleRowValue: boolean/*not key/value pairs in each row*/, rowCreator?: (rawRowData: any) => any) {
			// partial row updates (remove/insert/update)
	
			// {
			//   "rows": rowData, // array again
			//   "startIndex": ...,
			//   "endIndex": ...,
			//   "type": ... // ONE OF CHANGE = 0; INSERT = 1; DELETE = 2;
			// }
	            
			// apply granular updates one by one
			for (let i = 0; i < rowUpdates.length; i++) {
				const rowUpdate = rowUpdates[i];
				if (rowUpdate.type == this.foundsetTypeConstants.ROWS_CHANGED) {
					const wholeRowUpdates = simpleRowValue || rowUpdate.rows[0][this.foundsetTypeConstants.ROW_ID_COL_KEY]; // if the rowUpdate rows contain '_svyRowId' then we know it's the entire/complete row object; same if it's a one value per row (foundset linked)
					
					const convertedRowChangeData = this.expandTypeInfoAndApplyConversions(rowUpdate[this.sabloConverters.CONVERSION_CL_SIDE_TYPE_KEY],
							rowUpdate.rows, rowUpdate.startIndex, viewPort, internalState, componentScope, propertyContext,
							wholeRowUpdates);
					
					convertedRowChangeData.forEach((newRowValue, rowUpdateIndex) => {
						if (wholeRowUpdates) viewPort[rowUpdate.startIndex + rowUpdateIndex] = rowCreator ? rowCreator(newRowValue) : newRowValue;
						else {
							// key/value pairs in each row and this is a partial row update (maybe just one col. in a row has changed; leave the rest as they were)
							for (const dpName in newRowValue) {
								// update value
								viewPort[rowUpdate.startIndex + rowUpdateIndex][dpName] = newRowValue[dpName];	
							}
						}
					});
				} else if (rowUpdate.type == this.foundsetTypeConstants.ROWS_INSERTED) {
					if (internalState[ViewportService.CONVERSIONS]) {
						// shift conversion info of other rows if needed (that is an object with number keys, can't use array splice directly)
						for (let j = rowUpdate.startIndex; j < viewPort.length; j++)
						{
							if (internalState[ViewportService.CONVERSIONS][j]) {
								internalState[ViewportService.CONVERSIONS][j + rowUpdate.rows.length] = internalState[ViewportService.CONVERSIONS][j];
								delete internalState[ViewportService.CONVERSIONS][j];
							}
						}	
					}
					const convertedRowChangeData = this.expandTypeInfoAndApplyConversions(rowUpdate[this.sabloConverters.CONVERSION_CL_SIDE_TYPE_KEY],
							rowUpdate.rows, rowUpdate.startIndex, null, internalState, componentScope, propertyContext,
							true);
	
					if (rowCreator) convertedRowChangeData.forEach((value, index) => convertedRowChangeData[index] = rowCreator(value));
					viewPort.splice(rowUpdate.startIndex, 0, ...convertedRowChangeData);
					
					rowUpdate.removedFromVPEnd = 0; // prepare rowUpdate for listener notifications; starting with Servoy 8.4 'removedFromVPEnd' is deprecated and always 0 as server-side code will add a separate delete operation as necessary
					rowUpdate.endIndex = rowUpdate.startIndex + rowUpdate.rows.length - 1; // prepare rowUpdate.endIndex for listener notifications
				} else if (rowUpdate.type == this.foundsetTypeConstants.ROWS_DELETED) {
					const oldLength = viewPort.length;
					if (internalState[ViewportService.CONVERSIONS]) {
						// delete conversion info for deleted rows
						for (let j = rowUpdate.startIndex; j < oldLength; j++)
						{
							if (j + (rowUpdate.endIndex - rowUpdate.startIndex) <  oldLength)
							{
								internalState[ViewportService.CONVERSIONS][j] = internalState[ViewportService.CONVERSIONS][j + (rowUpdate.endIndex - rowUpdate.startIndex)]
							}
							else
							{
								delete internalState[ViewportService.CONVERSIONS][j];
							}
						}	
					}
					viewPort.splice(rowUpdate.startIndex, rowUpdate.endIndex - rowUpdate.startIndex + 1);
					
					rowUpdate.appendedToVPEnd = 0; // prepare rowUpdate for listener notifications; starting with Servoy 8.4 'appendedToVPEnd' is deprecated and always 0 as server-side code will add a separate insert operation as necessary
				} else if (rowUpdate.type == ViewportService.CHANGED_IN_LINKED_PROPERTY) {
					// just prepare it for the foundset change listener; components will want to handle this type of change as well so we should notify them when it happens
					rowUpdate.type = this.foundsetTypeConstants.ROWS_CHANGED;
				}
				delete rowUpdate[this.sabloConverters.CONVERSION_CL_SIDE_TYPE_KEY];
				delete rowUpdate.rows; // prepare rowUpdate for listener notifications
			}
		}
	
		
		/**
		 * This will not remove/add viewport watches (use removeDataWatchesFromRows/addDataWatchesToRows for that) - it will just forward 'updateAngularScope' to viewport values that need it.
		 */
		public updateAngularScope(viewPort: any[], internalState, componentScope: angular.IScope, simpleRowValue: boolean/*not key/value pairs in each row*/) {
			for (let i = viewPort.length - 1; i >= 0; i--) {
				const clientSideTypes = internalState[ViewportService.CONVERSIONS] ? internalState[ViewportService.CONVERSIONS][i] : undefined;
				if (clientSideTypes) {
					if (simpleRowValue) {
						(<sablo.IType<any>>clientSideTypes).updateAngularScope(viewPort[i], componentScope);
					} else {
						for (const columnName in viewPort[i]) {
							if (columnName !== this.foundsetTypeConstants.ROW_ID_COL_KEY && clientSideTypes[columnName])
								clientSideTypes[columnName].updateAngularScope(viewPort[i][columnName], componentScope);
						}
					}
				}
			}
		}
	
	}
	
}