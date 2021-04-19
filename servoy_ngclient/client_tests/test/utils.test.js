'use strict';

/* jasmine specs for filters go here */

describe('servoy $utils', function() {

  beforeEach(function() {
    module('servoy');
  })

  describe("test attach handler", function() {
	  	var called = {};
	  	var $scope = null;
		var element = {
				on: function(event,func) {
					called.event = event;
					called.func = func;
				}
		}
		beforeEach(function() {
            sessionStorage.removeItem('svy_session_lock');
			called = {};
			 inject(function(_$rootScope_){
		    	  $scope = _$rootScope_.$new();
		    	  $scope.handlers = {};
		  	  })
		});
	    it("it should not register anything", function() {
	    	inject(function($utils,$parse){
	    		$utils.attachEventHandler($parse,element,$scope,"handlers.onActionMethodID($event)",'click');
	    		expect( called.event).toBe(undefined);
	    	})
	    });
	    it("it should register", function() {
	    	inject(function($utils,$parse){
	    		$scope.handlers.onActionMethodID = function() {}
	    		$utils.attachEventHandler($parse,element,$scope,"handlers.onActionMethodID($event)",'click');
	    		expect( called.event).toBe("click");
	    	})
	    });
	});
}); 