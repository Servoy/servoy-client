angular.module('custom_properties', ['webSocketModule']).run(function ($sabloConverters) {
	$sabloConverters.registerCustomPropertyHandler('Date', {
		fromServerToClient: function (serverJSONValue, currentClientValue) {
			return new Date(serverJSONValue);
		},
		
		fromClientToServer: function(newClientData, oldClientData) {
			return newClientData.getTime();
		}
	});

	var UPDATE_PREFIX = "upd_"; // prefixes keys when only partial updates are send for them

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
					} else if (angular.isDefined(v[UPDATE_PREFIX + ROWS])) {
						// partial row updates (remove/insert/update)
						var rowUpdates = v[UPDATE_PREFIX + ROWS]; // array of
						
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
								for (j = rowUpdate.startIndex; j <= rowUpdate.endIndex; j++) rows[j] = rowUpdate.rows[j];
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
					// initialize the property value; make it 'smart'
					newValue.loadRecordsAsync = function(startIndex, size) {
						this.viewPortChange = {startIndex : startIndex, size : size};
						if (this.changeNotifier) this.changeNotifier();
					};
					newValue.loadExtraRecordsAsync = function(negativeOrPositiveCount) {
						// TODO ac
					};
					newValue.setChangeNotifier = function(changeNotifier) {
						this.changeNotifier = changeNotifier; 
					}
					newValue.isChanged = function() { return this.viewPortChange != null; }
				}
			}				 
				
			return newValue;
		},

		fromClientToServer: function(newClientData, oldClientData) {
			if (newClientData && newClientData.viewPortChange) {
				var tmp = newClientData.viewPortChange;
				newClientData.viewPortChange = null;
				return {newViewPort: tmp};
			}
			return {};
		}
	});
	
	$sabloConverters.registerCustomPropertyHandler('component[]', {
		fromServerToClient: function (serverJSONValue, currentClientValue) {
			if (serverJSONValue) {
				for (var c in serverJSONValue) {
					if (!serverJSONValue[c].api) serverJSONValue[c].api = {};
				}
			}
			return serverJSONValue;
		},

		fromClientToServer: function(newClientData, oldClientData) {
			return {};
		}
	});
});
