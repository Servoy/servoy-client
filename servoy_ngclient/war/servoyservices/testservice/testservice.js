angular.module('testservice',['servoy'])
.factory("testservice",function($window,$services) {
	var state = $services.getServiceState('testservice');
	return {
		talk: function() {
			alert("talk: " + state.text);
			state.text = "something else"
		}
	}
})
.run(function($rootScope,$services)
{
	var scope = $rootScope.$new(true);
	scope.state = $services.getServiceState('testservice');
	scope.$watch('state', function(newvalue,oldvalue) {
		// handle state changes
	}, true);
})