angular.module('slider',['servoy','ui.slider']).directive('slider', function() {
    return {
           restrict : 'E',
           transclude: true,
           scope : {
        	   model: '=svyModel',
        	   svyApply: '=',
        	   handlers: "=svyHandlers",
               api: "=svyApi"
           },
           templateUrl : 'servoycomponents/slider/slider.html',
           controller: function($scope, $element, $attrs, $parse) {
        	   
        	   var properties = ['model.max', 'model.min', 'model.step', 'model.animate', 'model.enabled', 'model.orientation', 'model.range'];
        	   angular.forEach(properties, function(property) {
        		   $scope.$watch(property, function (newVal, oldVal, scope) 
        	       {
        	       	   $scope.init();                	
        	       });
        	   });
        	   
        	   $scope.init = function()
        	   {
	        	   $scope.options =
	        	   {
	        		   animate: $scope.model.animate,
	        		   disabled: $scope.model.enabled == false,
	        		   orientation: $scope.model.orientation,
					   min: $scope.model.min,
					   max: $scope.model.max,
					   step: $scope.model.step,
					   range: $scope.model.range
	        	   };
	        	   
	        	   if ($scope.handlers.onStartMethodID)
	        	   {
	        		   function slideStart (ev, ui) {
	            		   return $scope.handlers.onStartMethodID(ev, ui.value);
	            	   }
	        		   $scope.options.start = slideStart;
	        	   }
	        	   
	        	   if ($scope.handlers.onStopMethodID)
	        	   {
	        		   function slideStop (ev, ui) {
	            		   return $scope.handlers.onStopMethodID(ev, ui.value); 
	            	   }
	        		   $scope.options.stop = slideStop;
	        	   }
	        	   
	        	   if ($scope.handlers.onSlideMethodID)
	        	   {
	        		   function slide (ev, ui) {
	            		   return $scope.handlers.onSlideMethodID(ev, ui.value);
	            	   }
	        		   $scope.options.slide = slide;
	        	   }
	        	   
	        	   if ($scope.handlers.onCreateMethodID)
	        	   {
	        		   function slideCreate (ev, ui) {
	            		   return $scope.handlers.onCreateMethodID(ev);
	            	   }
	        		   $scope.options.create = slideCreate;
	        	   }
	        	         	   
	        	   if ($scope.handlers.onChangeMethodID)
	        	   {
	        		   function slideChange (ev, ui) {
	            		   return $scope.handlers.onChangeMethodID(ev);
	            	   }
	        		   $scope.options.change = slideChange;
	        	   }
	        	   
	        	   $scope.api.setValue = function(val) {
	        		   $element.slider( "value", val );
	        	   }
	        	   
	        	   $scope.$watch('model.dataProviderID', function() {
	        		   $scope.svyApply('dataProviderID')
	        	   })
	        	   
	        	   $element.slider($scope.options);
        	   }
           },
           replace: true
        }
 	})