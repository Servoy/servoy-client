angular.module('foundset_manager',['sabloApp'])
.factory("foundset_manager",function($sabloApplication, $services, $q) {
	var scope = $services.getServiceScope('foundset_manager');
	
	function getFoundSetFromScope(foundsethash) {
		if(scope.model.foundsets) {
			for(var i = 0; i < scope.model.foundsets.length; i++) {
				if(scope.model.foundsets[i].foundsethash == foundsethash) {
					return scope.model.foundsets[i].foundset;
				}
			}
		}
		return null;
	}

	return {
		getFoundSet: function(foundsethash, dataproviders, sort) {
			var deferred = $q.defer();
			var foundset = getFoundSetFromScope(foundsethash);
			if(!foundset) {
				if(!scope.model.foundsets) scope.model.foundsets = [];
				var foundsetWatch = scope.$watch('model.foundsets', function(newVal, oldVal){
					var foundset = getFoundSetFromScope(foundsethash);
					if(foundset) {
						foundsetWatch();
						deferred.resolve(foundset);
					}
			    });
				$sabloApplication.callService("$foundsetManager", "getFoundSet", {foundsethash: foundsethash, dataproviders: dataproviders, sort: sort}, false);
			}
			else {
				deferred.resolve(foundset);
			}
			return deferred.promise;
		},
		getRelatedFoundSetHash: function(foundsethash, rowid, relation) {
			return $sabloApplication.callService("$foundsetManager", "getRelatedFoundSetHash", {foundsethash: foundsethash, rowid: rowid, relation: relation}, false);
		},
		updateFoundSetRow: function(foundsethash, rowid, dataproviderid, value) {
			return $sabloApplication.callService("$foundsetManager", "updateFoundSetRow", {foundsethash: foundsethash, rowid: rowid, dataproviderid: dataproviderid, value: value}, false);
		},
		addFoundSetChangeCallback: function(foundsethash, callback) {
			if(scope.model.foundsets) {
				for(var i = 0; i < scope.model.foundsets.length; i++) {
					if(scope.model.foundsets[i].foundsethash == foundsethash) {
						return scope.$watch('model.foundsets[' + i + '].foundset.viewPort.rows', function(newValue, oldValue) {
							if(newValue !== oldValue) {
								callback();
							}
						}, true);
					}
				}
			}
			return null;
		}
	}
})
