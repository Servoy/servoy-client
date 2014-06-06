angular.module('testservice',['servoy'])
.factory("testservice",function($window,$services) {
	var state = $services.getServiceState('testservice');
	return {
		talk: function() {
			alert("talk: " + state.text);
		}
	}
})