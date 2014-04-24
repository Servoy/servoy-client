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
        	   var properties = ['max', 'min', 'step', 'animate', 'enabled', 'orientation', 'range'];
        	   angular.forEach(properties, function(property) {
        		   $scope.$watch("model." + property, function (newVal, oldVal, scope) 
        	       {
        	           if (newVal != undefined) {
        	       	   	$scope.options[property] = newVal;
        	       	   }
        	       });
        	   });
        	   
        	   $scope.options =
        	   {
        		   animate: $scope.model.animate || false,
        		   disabled: $scope.model.enabled == false,
        		   orientation: $scope.model.orientation || 'vertical',
				   min: $scope.model.min || 0,
				   max: $scope.model.max || 0,
				   step: $scope.model.step || 1,
				   range: $scope.model.range || 'max'
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
        	   
           },
           replace: true
        }
 	})