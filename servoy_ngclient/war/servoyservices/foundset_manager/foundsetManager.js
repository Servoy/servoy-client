angular.module('foundset_manager',['sabloApp'])
.factory("$foundsetManager",function($sabloApplication) {
	return {
		getFoundSet: function(foundsethash, dataproviders, sort) {
			return $sabloApplication.callService("$foundsetService", "getFoundSet", {foundsethash: foundsethash, dataproviders: dataproviders, sort: sort}, false);
		},
		getRelatedFoundSetHash: function(foundsethash, rowid, relation) {
			return $sabloApplication.callService("$foundsetService", "getRelatedFoundSetHash", {foundsethash: foundsethash, rowid: rowid, relation: relation}, false);
		},
		updateFoundSetRow: function(foundsethash, rowid, dataproviderid, value) {
			return $sabloApplication.callService("$foundsetService", "updateFoundSetRow", {foundsethash: foundsethash, rowid: rowid, dataproviderid: dataproviderid, value: value}, false);
		}
	}
})
