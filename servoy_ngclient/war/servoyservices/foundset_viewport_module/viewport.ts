/// <reference path="../../../typings/angularjs/angular.d.ts" />
/// <reference path="../../../typings/sablo/sablo.d.ts" />

angular.module('foundset_viewport_module', ['webSocketModule'])
//Viewport reuse code module -------------------------------------------
.factory("$viewportModule", function($sabloConverters: sablo.ISabloConverters, $foundsetTypeConstants: foundsetType.FoundsetTypeConstants, $sabloUtils: sablo.ISabloUtils,
		$typesRegistry: sablo.ITypesRegistryForSabloConverters, $pushToServerUtils: sablo.IPushToServerUtils) {
	return new ngclient.propertyTypes.ViewportService($sabloConverters, $foundsetTypeConstants, $sabloUtils, $typesRegistry, $pushToServerUtils);
});

namespace ngclient.propertyTypes {
	
	export class ViewportService {
        
        // this key/column should be stored as $foundsetTypeConstants.ROW_ID_COL_KEY in the actual row, but this key is sent from server when the foundset property is sending
        // just a partial update, but some of the columns that did change are also pks so they do affect the pk hash; client uses this to distiguish between a full
        // update of a row and a partial update of a row; so if update has $foundsetTypeConstants.ROW_ID_COL_KEY it will consider it to be a full update,
        // and if it has either ROW_ID_COL_KEY_PARTIAL_UPDATE or no rowID then it is a partial update of a row (only some of the columns in that row have changed) 
        private static readonly ROW_ID_COL_KEY_PARTIAL_UPDATE = "_svyRowId_p";

        private static readonly DATAPROVIDER_KEY = "dp";
		private static readonly VALUE_KEY = "value";
		
		public static readonly MAIN_TYPE = "mT";
		private static readonly COL_TYPES = "cT";
		private static readonly CELL_TYPES = "eT";
		private static readonly FOR_ROW_IDXS = "i";
		
		constructor(private readonly sabloConverters: sablo.ISabloConverters,
				private readonly foundsetTypeConstants: foundsetType.FoundsetTypeConstants,
				private readonly sabloUtils: sablo.ISabloUtils,
				private readonly typesRegistry: sablo.ITypesRegistryForSabloConverters,
				private readonly pushToServerUtils: sablo.IPushToServerUtils) {}
	
		private addDataWatchToCell(columnName: string/*can be null*/, idx: number, viewPort: any[], internalState: InternalStateForViewport, componentScope: angular.IScope, propertyContext: sablo.IPropertyContext) {
            const setChangeNotifierIfSmartProperty = (value: any): boolean => {
                if (value && value[this.sabloConverters.INTERNAL_IMPL] && value[this.sabloConverters.INTERNAL_IMPL].setChangeNotifier) {
                    // we don't care to check below for dumbWatchType because some types (see foundset) need to send internal protocol messages even if they are not watched/changeable on server
                    value[this.sabloConverters.INTERNAL_IMPL].setChangeNotifier(() => {
                        if (value[this.sabloConverters.INTERNAL_IMPL].isChanged()) queueChange(value, value);
                    });
                    return true;
                }
                return false;
            }
        
			const queueChange = (newData: any, oldData: any) => {
				const r = {};

				if (angular.isDefined(internalState.forFoundset)) {
					r[this.foundsetTypeConstants.ROW_ID_COL_KEY] = internalState.forFoundset().viewPort.rows[idx][this.foundsetTypeConstants.ROW_ID_COL_KEY];
				} else r[this.foundsetTypeConstants.ROW_ID_COL_KEY] = viewPort[idx][this.foundsetTypeConstants.ROW_ID_COL_KEY];
				r[ViewportService.DATAPROVIDER_KEY] = columnName;
				r[ViewportService.VALUE_KEY] = newData;

				// convert new data if necessary
				const clientSideTypes = internalState.viewportTypes ? internalState.viewportTypes[idx] : undefined;
				const clientSideType = (clientSideTypes ? (columnName ? clientSideTypes[columnName] : clientSideTypes) : undefined); 
				r[ViewportService.VALUE_KEY] = this.sabloConverters.convertFromClientToServer(r[ViewportService.VALUE_KEY], clientSideType, oldData, componentScope, propertyContext);
				
                // set/update change notifier just in case a new full value was set into a smart property type that needs a changeNotifier for that specific property
				setChangeNotifierIfSmartProperty(newData);

				internalState.requests.push( { viewportDataChanged: r } );
				if (internalState.changeNotifier) internalState.changeNotifier();
			}

			const getCellValue = function getCellValue() { 
				return columnName == null ? viewPort[idx] : viewPort[idx][columnName]
			}; // viewport row can be just a value or an object of key/value pairs
	
            if (componentScope) {
				if (setChangeNotifierIfSmartProperty(getCellValue())) {
					// smart property value
	
            		// watch smart value for change-by reference if needed (if in spec it is either shallow or deep (which would be weird))
					if (propertyContext.getPushToServerCalculatedValue().value >= this.pushToServerUtils.shallow.value) internalState.unwatchData[idx].push(
							componentScope.$watch(getCellValue, (newData, oldData) => {
								if (newData !== oldData) { /* this doesn't seem to work correctly for 2 identical Date objects in Chrome when debugging; but it should */
									queueChange(newData, oldData);
								}
							})
					);
				} else if (propertyContext.getPushToServerCalculatedValue().value >= this.pushToServerUtils.shallow.value) {
					// deep or shallow watch for dumb values
					internalState.unwatchData[idx].push(
							componentScope.$watch(getCellValue, (newData, oldData) => {
								if (newData !== oldData) { /* this doesn't seem to work correctly for 2 identical Date objects in Chrome when debugging; but it should */
									let changed = false;
									if (typeof newData == "object") {
										const clientSideTypes = internalState.viewportTypes ? internalState.viewportTypes[idx] : undefined;
										if (this.sabloUtils.isChanged(newData, oldData, clientSideTypes ? (columnName ? clientSideTypes[columnName] : clientSideTypes) : undefined)) {
											changed = true;
										}
									} else {
										changed = true;
									}
									if (changed) queueChange(newData, oldData);
								}
							}, propertyContext.getPushToServerCalculatedValue() == this.pushToServerUtils.shallow ? false : true)
					);
				}
			} else setChangeNotifierIfSmartProperty(getCellValue());
		}
	
		private addDataWatchesToRow(idx: number, viewPort: any[], internalState: InternalStateForViewport, componentScope: angular.IScope, propertyContextCreator: sablo.IPropertyContextCreator, simpleRowValue: boolean/*not key/value pairs in each row*/) {
			if (!angular.isDefined(internalState.unwatchData)) internalState.unwatchData = {};
			internalState.unwatchData[idx] = [];
			if (simpleRowValue) {
				this.addDataWatchToCell(null, idx, viewPort, internalState, componentScope, propertyContextCreator.withPushToServerFor(undefined));
			} else {
				for (const columnName in viewPort[idx]) {
					if (columnName !== this.foundsetTypeConstants.ROW_ID_COL_KEY) {
						this.addDataWatchToCell(columnName, idx, viewPort, internalState, componentScope, propertyContextCreator.withPushToServerFor(columnName));
					}
				}
			}
		}
	
		public addDataWatchesToRows(viewPort: any[], internalState: InternalStateForViewport, componentScope: angular.IScope, propertyContextCreator: sablo.IPropertyContextCreator, simpleRowValue: boolean/*not key/value pairs in each row*/) {
			for (let i = viewPort.length - 1; i >= 0; i--) {
				this.addDataWatchesToRow(i, viewPort, internalState, componentScope, propertyContextCreator, simpleRowValue);
			}
		}
	
		private removeDataWatchesFromRow(idx: number, internalState: InternalStateForViewport) {
			if (internalState.unwatchData && internalState.unwatchData[idx]) {
				for (let j = internalState.unwatchData[idx].length - 1; j >= 0; j--)
					internalState.unwatchData[idx][j]();
				delete internalState.unwatchData[idx];
			}
		}
	
		public removeDataWatchesFromRows(rowCount: number, internalState: InternalStateForViewport) {
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
         * @param simpleRowValue true if each row in this viewport is a single value and false if each row in this viewport has columns / multiple values
		 * @return the converted-to-client rows for rowsToBeConverted param; it's actually the same reference as given param - to which any server->client conversions were also applied.
		 */
		private expandTypeInfoAndApplyConversions(serverConversionInfo: object, defaultColumnTypes: sablo.IWebObjectSpecification, rowsToBeConverted: any[],
		                             startIdxInViewportForRowsToBeConverted: number, oldViewportRows: any[],  internalState: InternalStateForViewport, componentScope: angular.IScope,
		                             propertyContextCreator: sablo.IPropertyContextCreator, simpleRowValue: boolean, fullRowUpdates: boolean): any[] {
		                                                                                                                             
			if (serverConversionInfo && rowsToBeConverted) {
				rowsToBeConverted.forEach((rowData, index) => {
					if (simpleRowValue) {
                        // without columns, so a foundset linked prop's rows
                        let cellConversion = this.getCellTypeFromServer(serverConversionInfo, index); // this is the whole row in this case (only one cell)
                        // defaultColumnTypes should be null here because it's not a component prop's viewport so no need to check for it
                        
                        rowsToBeConverted[index] = this.sabloConverters.convertFromServerToClient(rowsToBeConverted[index],
                                cellConversion, oldViewportRows ? oldViewportRows[startIdxInViewportForRowsToBeConverted + index] : undefined,
                                undefined /*dynamic types are already handled via serverConversionInfo here*/, undefined, componentScope, propertyContextCreator.withPushToServerFor(undefined));
                        this.updateRowTypes(startIdxInViewportForRowsToBeConverted + index, internalState, cellConversion);
					} else {
                        // with columns; so a foundset prop's rows or a component type prop's rows
                        let rowConversions = (fullRowUpdates || !internalState.viewportTypes ? undefined : internalState.viewportTypes[startIdxInViewportForRowsToBeConverted + index]);
                        Object.keys(rowData).forEach(columnName => {
                            let cellConversion: sablo.IType<any> = this.getCellTypeFromServer(serverConversionInfo, index, columnName);
                            if (!cellConversion && defaultColumnTypes) cellConversion = defaultColumnTypes.getPropertyType(columnName);
                            
                            // ignore null or undefined type of cell; otherwise remember it in expanded
                            if (cellConversion) {
                                if (! rowConversions) rowConversions = {};
                                rowConversions[columnName] = cellConversion;
                            } else if (rowConversions && rowConversions[columnName]) delete rowConversions[columnName];

                            rowData[columnName] = this.sabloConverters.convertFromServerToClient(rowsToBeConverted[index][columnName],
                                    cellConversion,
                                    oldViewportRows ? (oldViewportRows[startIdxInViewportForRowsToBeConverted + index] ? (oldViewportRows[startIdxInViewportForRowsToBeConverted + index][columnName]) : undefined) : undefined,
                                    undefined /*dynamic types are already handled via serverConversionInfo here*/, undefined, componentScope, propertyContextCreator.withPushToServerFor(columnName));
                        });
                        if (rowData[ViewportService.ROW_ID_COL_KEY_PARTIAL_UPDATE] !== undefined) {
                            // see comment of ROW_ID_COL_KEY_PARTIAL_UPDATE
                            rowData[this.foundsetTypeConstants.ROW_ID_COL_KEY] = rowData[ViewportService.ROW_ID_COL_KEY_PARTIAL_UPDATE];
                            delete rowData[ViewportService.ROW_ID_COL_KEY_PARTIAL_UPDATE];
                        }

                        if (rowConversions && Object.keys(rowConversions).length == 0) rowConversions = undefined; // in case all conversion infos from one row were deleted due to the update
                        this.updateRowTypes(startIdxInViewportForRowsToBeConverted + index, internalState, rowConversions);
					}
				});
			}
			
			return rowsToBeConverted;
		}
		
		private getCellTypeFromServer(serverConversionInfo: object, rowIndex: number, columnName?: string): sablo.IType<any> {
			const PROCESSED_CELL_TYPES = "pct"; // just to turn stuff like [{ "_T": "zyx", "i": [2,3] }, {"_T": "xyz", "i": [9] }] into an easier to use { 2: "zyx", 3: "zyx", 9: "xyz" }
			const mainType = serverConversionInfo[ViewportService.MAIN_TYPE];
			const columnTypes = serverConversionInfo[ViewportService.COL_TYPES];
			
			let cellConversion: sablo.ITypeFromServer;
			if (columnTypes) {
				let colType = (columnName ? columnTypes[columnName] : columnTypes); // foundset (multi col) or foundset linked (single col) data
				if (colType) {
						let processed = colType[PROCESSED_CELL_TYPES];
						if (!processed) {
							processed = colType[PROCESSED_CELL_TYPES] = {};
							if (colType[ViewportService.CELL_TYPES])
								colType[ViewportService.CELL_TYPES].forEach(
										(value: any) => value[ViewportService.FOR_ROW_IDXS].forEach(
												(ri: any) => processed[ri] = value[this.sabloConverters.CONVERSION_CL_SIDE_TYPE_KEY]));
						}
						
						cellConversion = processed[rowIndex]; // look at cell type; null is a valid type in what server can send so we check with === undefined
						if (cellConversion === undefined) cellConversion = colType[this.sabloConverters.CONVERSION_CL_SIDE_TYPE_KEY]; // fallback to main column type
						if (cellConversion === undefined) cellConversion = mainType; // fallback to main type
				} else cellConversion = mainType;
			} else cellConversion = mainType;
			
			if (cellConversion) return this.typesRegistry.getAlreadyRegisteredType(cellConversion);
			
			return undefined;
		}
	
		/**
		 * @param types can be one IType<?>  or an object of IType<?> for each column on that row.
		 */
		private updateRowTypes(idx: number, internalState: InternalStateForViewport, types : sablo.IType<any> | { [ colName: string ]: sablo.IType<any> }) {
			if (angular.isUndefined(internalState.viewportTypes)) {
				internalState.viewportTypes = {};
			}
			internalState.viewportTypes[idx] = types;
		}
	
		/**
		 * It will update the whole viewport. More precisely it will apply all server-to-client conversions directly on given viewPortUpdate param and return it.
		 * @param oldViewPort old viewport
		 * @param viewPortUpdate the whole viewport update value from server
		 * @param defaultColumnTypes only used for component type viewports - where the default column (component property) types (so non-dynamic types) are already known. All other viewports should give null here.
		 * @param simpleRowValue true if each row in this viewport is a single value and false if each row in this viewport has columns / multiple values
		 * 
		 * @return the given viewPortUpdate with conversions applied to it
		 */
		public updateWholeViewport(oldViewPort: any[], internalState: InternalStateForViewport, viewPortUpdate: any[], viewPortUpdateConversions: object, defaultColumnTypes: sablo.IWebObjectSpecification,
		                           componentScope: angular.IScope, propertyContextCreator: sablo.IPropertyContextCreator, simpleRowValue: boolean): any[] {
			// update conversion info; expand what we get from server to be easy to use on client (like main type and main column type from JSON are kept at cell level)
			internalState.viewportTypes = {};
			return this.expandTypeInfoAndApplyConversions(viewPortUpdateConversions, defaultColumnTypes, viewPortUpdate, 0, oldViewPort, internalState, componentScope, propertyContextCreator, simpleRowValue, true);
		}
	
		// see comment above, before updateWholeViewport()
		public updateViewportGranularly(viewPort: any[], internalState: InternalStateForViewport, rowUpdates: any[], defaultColumnTypes: sablo.IWebObjectSpecification, componentScope: angular.IScope,
		         propertyContext: sablo.IPropertyContextCreator, simpleRowValue: boolean/*not key/value pairs in each row*/, rowCreator?: (rawRowData: any) => any): void {
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
					
					const convertedRowChangeData = this.expandTypeInfoAndApplyConversions(rowUpdate[this.sabloConverters.CONVERSION_CL_SIDE_TYPE_KEY], defaultColumnTypes,
							rowUpdate.rows, rowUpdate.startIndex, viewPort, internalState, componentScope, propertyContext,
							simpleRowValue, wholeRowUpdates);
					
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
					if (internalState.viewportTypes) {
						// shift conversion info of other rows if needed (that is an object with number keys, can't use array splice directly)
                        for (let j = viewPort.length - 1; j >= rowUpdate.startIndex; j--) {
                            if (internalState.viewportTypes[j]) internalState.viewportTypes[j + rowUpdate.rows.length] = internalState.viewportTypes[j];
                            else if (internalState.viewportTypes[j + rowUpdate.rows.length]) delete internalState.viewportTypes[j + rowUpdate.rows.length];

                            delete internalState.viewportTypes[j];
                        }
					}
					const convertedRowChangeData = this.expandTypeInfoAndApplyConversions(rowUpdate[this.sabloConverters.CONVERSION_CL_SIDE_TYPE_KEY], defaultColumnTypes,
							rowUpdate.rows, rowUpdate.startIndex, null, internalState, componentScope, propertyContext,
							simpleRowValue, true);
	
					if (rowCreator) convertedRowChangeData.forEach((value, index) => convertedRowChangeData[index] = rowCreator(value));
					viewPort.splice(rowUpdate.startIndex, 0, ...convertedRowChangeData);
					
					rowUpdate.removedFromVPEnd = 0; // prepare rowUpdate for listener notifications; starting with Servoy 8.4 'removedFromVPEnd' is deprecated and always 0 as server-side code will add a separate delete operation as necessary
					rowUpdate.endIndex = rowUpdate.startIndex + rowUpdate.rows.length - 1; // prepare rowUpdate.endIndex for listener notifications
				} else if (rowUpdate.type == this.foundsetTypeConstants.ROWS_DELETED) {
					const oldLength = viewPort.length;
					var numberOfDeletedRows = rowUpdate.endIndex - rowUpdate.startIndex + 1;
					if (internalState.viewportTypes) {
						// delete conversion info for deleted rows and shift left what is after deletion
                        for (let j = rowUpdate.startIndex; j <= rowUpdate.endIndex; j++)
                          delete internalState.viewportTypes[j];
                        for (let j = rowUpdate.endIndex + 1; j < oldLength; j++) {
                            if (internalState.viewportTypes[j]) internalState.viewportTypes[j - numberOfDeletedRows] = internalState.viewportTypes[j];
                            else if (internalState.viewportTypes[j - numberOfDeletedRows]) delete internalState.viewportTypes[j - numberOfDeletedRows];

                            delete internalState.viewportTypes[j];
                        }
					}
					viewPort.splice(rowUpdate.startIndex, numberOfDeletedRows);
					
					rowUpdate.appendedToVPEnd = 0; // prepare rowUpdate for listener notifications; starting with Servoy 8.4 'appendedToVPEnd' is deprecated and always 0 as server-side code will add a separate insert operation as necessary
				}
				
				delete rowUpdate[this.sabloConverters.CONVERSION_CL_SIDE_TYPE_KEY];
				delete rowUpdate.rows; // prepare rowUpdate for listener notifications
			}
		}
	
		
		/**
		 * This will not remove/add viewport watches (use removeDataWatchesFromRows/addDataWatchesToRows for that) - it will just forward 'updateAngularScope' to viewport values that need it.
		 */
		public updateAngularScope(viewPort: any[], internalState: InternalStateForViewport, componentScope: angular.IScope, simpleRowValue: boolean/*not key/value pairs in each row*/) {
			for (let i = viewPort.length - 1; i >= 0; i--) {
				const clientSideTypes = internalState.viewportTypes ? internalState.viewportTypes[i] : undefined;
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
		
		public getClientSideTypeFor(rowId: any, columnName: string, internalState: InternalStateForViewport, viewPortRowsInCaseCallerIsActualFoundsetProp?: any[]): sablo.IType<any> {
			// find the index for this rowId
			let idx = -1;
			if (angular.isDefined(internalState[this.foundsetTypeConstants.FOR_FOUNDSET_PROPERTY])) {
				idx = internalState[this.foundsetTypeConstants.FOR_FOUNDSET_PROPERTY]().viewPort.rows.findIndex((val: any) => {
					return val[this.foundsetTypeConstants.ROW_ID_COL_KEY] == rowId;
				});
			} else {
				// if it doesn't have internalState[this.foundsetTypeConstants.FOR_FOUNDSET_PROPERTY] then it's probably the foundset property's viewport directly which has those in the viewport
				idx = viewPortRowsInCaseCallerIsActualFoundsetProp.findIndex((val: any) => {
					return val[this.foundsetTypeConstants.ROW_ID_COL_KEY] == rowId;
				});
			}
			
			let clientSideType:sablo.IType<any> = undefined;
			if (idx >= 0) {
				const clientSideTypes = internalState.viewportTypes ? internalState.viewportTypes[idx] : undefined;
				if (clientSideTypes && (!columnName || clientSideTypes[columnName]))
					clientSideType = <sablo.IType<any>> (columnName ? clientSideTypes[columnName] : clientSideTypes);
			}
			return clientSideType;
		}
	
	}
	
	// this class if currently copied over to all places where it needs to be implemented to avoid load order problems with js file that could happen on ng1 (like foundset or component type files being loaded in browser before viewport => runtime error because this class could not be found)
    // make sure you keep all the copies in sync	
	// ng2 will be smarter about load order and doesn't have to worry about this
	abstract class InternalStateForViewport implements sablo.ISmartPropInternalState {
        forFoundset?: () => FoundsetValue;
        
        viewportTypes: any; // TODO type this
        unwatchData: { [idx: number]: Array<() => void> };
        
        requests: Array<any> = [];
        
        // inherited from sablo.ISmartPropInternalState
        changeNotifier: () => void;
        
        
        constructor(public readonly webSocket: sablo.IWebSocket,
                        public readonly componentScope: angular.IScope, public readonly changeListeners: Array<(values: any) => void> = []) {}
        
        setChangeNotifier(changeNotifier: () => void): void {
            this.changeNotifier = changeNotifier;
        }
         
        isChanged(): boolean {
            return this.requests && (this.requests.length > 0);
        }
        
        fireChanges(changes: any) {
            for (let i = 0; i < this.changeListeners.length; i++) {
                this.webSocket.setIMHDTScopeHintInternal(this.componentScope);
                this.changeListeners[i](changes);
                this.webSocket.setIMHDTScopeHintInternal(undefined);
            }
        }

    }
	
}
