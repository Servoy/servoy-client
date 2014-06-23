angular.module('custom_properties', ['webSocketModule']).run(function ($sabloConverters) {
	$sabloConverters.registerCustomPropertyHandler('Date', {
		fromServerToClient: function (serverJSONValue, currentClientValue) {
			return new Date(serverJSONValue);
		},
		
		fromClientToServer: function(newClientData, oldClientData) {
			return newClientData.getTime();
		}
	});

	$sabloConverters.registerCustomPropertyHandler('foundset', {
		fromServerToClient: function (serverJSONValue, currentClientValue) {
			var newValue = currentClientValue;
//			if (currentClientValue) {
//				// updated received; handle them
//				// TODO ac
//			} else {
			// initialize the property value; make it 'smart'
				newValue = serverJSONValue;
				if (newValue) {
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
//			}
			
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
