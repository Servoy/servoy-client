angular.module('window',['servoy'])
.factory("window",function($window,$services) {
	var state = $services.getServiceState('window');
	return {
		talk: function() {
			alert("talk nothing ");
		}
	}
})