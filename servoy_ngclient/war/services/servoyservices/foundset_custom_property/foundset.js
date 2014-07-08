angular.module('foundset_custom_property', ['webSocketModule'])
// Foundset type -------------------------------------------
.value("$foundsetTypeConstants", {
	ROW_ID_COL_KEY: '_svyRowId'
})
.run(function ($sabloConverters, $rootScope, $foundsetTypeConstants) {
	var UPDATE_PREFIX = "upd_"; // prefixes keys when only partial updates are send for them
	var CONVERSIONS = "conversions"; // data conversion info

	var SERVER_SIZE = "serverSize";
	var SELECTED_ROW_INDEXES = "selectedRowIndexes";
	var MULTI_SELECT = "multiSelect";
	var VIEW_PORT = "viewPort";
	var START_INDEX = "startIndex";
	var SIZE = "size";
	var ROWS = "rows";
	
	var CHANGE = 0;
	var INSERT = 1;
	var DELETE = 2;
	
	var NO_OP = "noOP";
	
	function addDataWatchesToRows(foundsetValue) {
		var i;
		for (i = foundsetValue.viewPort.rows.length - 1; i >= 0; i--) {
			var unwatchRowFuncs = []; 
			foundsetValue.__unwatchData[foundsetValue.viewPort.rows[i][$foundsetTypeConstants.ROW_ID_COL_KEY]] = unwatchRowFuncs;
			var dataprovider;
			for (dataprovider in foundsetValue.viewPort.rows[i]) {
				if (dataprovider !== $foundsetTypeConstants.ROW_ID_COL_KEY) unwatchRowFuncs.push(
						$rootScope.$watch(function() { return foundsetValue.viewPort.rows[i][dataprovider]; }, function (newData, oldData) {
							if (newData !== oldData) {
								var changed = false;
								
//								if (foundsetValue.__ignoreSelectedChange) {
//									if (foundsetValue.__ignoreSelectedChange.length == newSel.length) {
//										var i;
//										for (i = 0; i < foundsetValue.__ignoreSelectedChange.length; i++)
//											if (foundsetValue.__ignoreSelectedChange[i] !== newSel[i]) { changed = true; break; }
//									} else changed = true;
//									foundsetValue.__ignoreSelectedChange = null;
//								} else changed = true;
//
//								if (changed) {
//									if (!foundsetValue.requests) foundsetValue.requests = [];
//									foundsetValue.requests.push({newClientSelection: newSel});
//									if (foundsetValue.changeNotifier) foundsetValue.changeNotifier();
//								}
							}
						})
				);
			}
		}
	};

	function removeDataWatchesFromRows(foundsetValue) {
		if (foundsetValue.__unwatchData) {
			var pk;
			for (pk in foundsetValue.__unwatchData) {
				var i;
				for (i = foundsetValue.__unwatchData[pk].length - 1; i >= 0; i--)
					foundsetValue.__unwatchData[pk][i]();
			}
			delete foundsetValue.__unwatchData;
		}
	};
	
	$sabloConverters.registerCustomPropertyHandler('foundset', {
		fromServerToClient: function (serverJSONValue, currentClientValue) {
			var newValue = currentClientValue;
			
			// see if this is an update or whole value and handle it
			if (!serverJSONValue) {
				newValue = serverJSONValue;
			} else {
				// check for updates
				var updates = false;
				if (angular.isDefined(serverJSONValue[UPDATE_PREFIX + SERVER_SIZE])) {
					currentClientValue[SERVER_SIZE] = serverJSONValue[UPDATE_PREFIX + SERVER_SIZE]; // currentClientValue should always be defined in this case
					updates = true;
				}
				if (angular.isDefined(serverJSONValue[UPDATE_PREFIX + SELECTED_ROW_INDEXES])) {
					currentClientValue[SELECTED_ROW_INDEXES] = serverJSONValue[UPDATE_PREFIX + SELECTED_ROW_INDEXES];
					currentClientValue.__ignoreSelectedChange = currentClientValue[SELECTED_ROW_INDEXES]; // don't send back to server selection that came from server
					updates = true;
				}
				if (angular.isDefined(serverJSONValue[UPDATE_PREFIX + VIEW_PORT])) {
					updates = true;
					var v = serverJSONValue[UPDATE_PREFIX + VIEW_PORT];
					if (angular.isDefined(v[START_INDEX])) {
						currentClientValue[VIEW_PORT][START_INDEX] = v[START_INDEX];
					}
					if (angular.isDefined(v[SIZE])) {
						currentClientValue[VIEW_PORT][SIZE] = v[SIZE];
					}
					if (angular.isDefined(v[ROWS])) {
						currentClientValue[VIEW_PORT][ROWS] = v[ROWS];
						if (v[CONVERSIONS]) $sabloConverters.convertFromServerToClient(currentClientValue[VIEW_PORT][ROWS], v[CONVERSIONS][ROWS]);
					} else if (angular.isDefined(v[UPDATE_PREFIX + ROWS])) {
						// partial row updates (remove/insert/update)
						var rowUpdates = v[UPDATE_PREFIX + ROWS]; // array of
						if (v[CONVERSIONS]) $sabloConverters.convertFromServerToClient(v[UPDATE_PREFIX + ROWS], v[CONVERSIONS][UPDATE_PREFIX + ROWS]);
						
						// {
						//   "rows": rowData, // array again
						//   "startIndex": ...,
						//   "endIndex": ...,
						//   "type": ... // ONE OF CHANGE = 0; INSERT = 1; DELETE = 2;
						// }
						
						// apply them one by one
						var i;
						var j;
						var rows = currentClientValue[VIEW_PORT][ROWS];
						for (i = 0; i < rowUpdates.length; i++) {
							var rowUpdate = rowUpdates[i];
							if (rowUpdate.type == CHANGE) {
								for (j = rowUpdate.startIndex; j <= rowUpdate.endIndex; j++) rows[j] = rowUpdate.rows[j - rowUpdate.startIndex];
							} else if (rowUpdate.type == INSERT) {
								for (j = rowUpdate.rows.length - 1; j >= 0 ; j--) rows.splice(rowUpdate.startIndex, 0, rowUpdate.rows[j]);
								// insert might have made obsolete some records in cache; remove those
								if (rows.length > currentClientValue[VIEW_PORT].size) rows.splice(currentClientValue[VIEW_PORT].size, rows.length - currentClientValue[VIEW_PORT].size);
							} else if (rowUpdate.type == DELETE) {
								rows.splice(rowUpdate.startIndex, rowUpdate.endIndex - rowUpdate.startIndex + 1);
								for (j = 0; j < rowUpdate.rows.length; j++) rows.push(rowUpdate.rows[j]);
							}
						}
					}
				}
				// if it's a no-op, ignore it (sometimes server asks a prop. to send changes even though it has none to send)
				if (!updates && serverJSONValue[NO_OP] !== 0) {
					newValue = serverJSONValue; // not updates - so whole thing received
					if (newValue[VIEW_PORT][CONVERSIONS]) $sabloConverters.convertFromServerToClient(newValue[VIEW_PORT][ROWS], newValue[VIEW_PORT][CONVERSIONS][ROWS]);
					// initialize the property value; make it 'smart'
					newValue.loadRecordsAsync = function(startIndex, size) {
						if (!this.requests) this.requests = [];
						this.requests.push({newViewPort: {startIndex : startIndex, size : size}});
						if (this.changeNotifier) this.changeNotifier();
					};
					newValue.loadExtraRecordsAsync = function(negativeOrPositiveCount) {
						if (!this.requests) this.requests = [];
						this.requests.push({loadExtraRecords: negativeOrPositiveCount});
						if (this.changeNotifier) this.changeNotifier();
					};
					newValue.setChangeNotifier = function(changeNotifier) {
						this.changeNotifier = changeNotifier; 
					}
					newValue.isChanged = function() { return this.requests && (this.requests.length > 0); }
					
					newValue.__ignoreSelectedChange = newValue[SELECTED_ROW_INDEXES]; // ignore initial watch change
					// watch for client selection changes and send them to server
					newValue.__unwatchSelection = $rootScope.$watchCollection(function() { return newValue[SELECTED_ROW_INDEXES]; }, function (newSel) {
						var changed = false;
						if (newValue.__ignoreSelectedChange) {
							if (newValue.__ignoreSelectedChange.length == newSel.length) {
								var i;
								for (i = 0; i < newValue.__ignoreSelectedChange.length; i++)
									if (newValue.__ignoreSelectedChange[i] !== newSel[i]) { changed = true; break; }
							} else changed = true;
							newValue.__ignoreSelectedChange = null;
						} else changed = true;

						if (changed) {
							if (!newValue.requests) newValue.requests = [];
							newValue.requests.push({newClientSelection: newSel});
							if (newValue.changeNotifier) newValue.changeNotifier();
						}
					});
					
					// watch for client dataProvider changes and send them to server
					newValue.__unwatchData = {}; // { rowPk: [unwatchDataProvider1Func, ...], ... }
					addDataWatchesToRows(newValue);
				}
				
			}				 
			if (angular.isDefined(currentClientValue) && newValue != currentClientValue) {
				// the client side object will change completely, and the old one probably has watches defined...
				// unregister those
				
				if (currentClientValue.__unwatchSelection) {
					currentClientValue.__unwatchSelection();
					delete currentClientValue.__unwatchSelection;
				}
				removeDataWatchesFromRows(currentClientValue);
			}
				
			return newValue;
		},

		fromClientToServer: function(newClientData, oldClientData) {
			if (newClientData && newClientData.isChanged()) {
				var tmp = newClientData.requests;
				newClientData.requests = null;
				return tmp;
			}
			return [];
		}
	});
})
