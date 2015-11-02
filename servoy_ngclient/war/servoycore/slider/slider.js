angular.module('servoycoreSlider',['servoy','ui.slider']).directive('servoycoreSlider', function() {
    return {
           restrict : 'E',
           scope : {
        	   model: '=svyModel',
        	   svyServoyapi: "=",
        	   handlers: "=svyHandlers"
           },
           templateUrl : 'servoycore/slider/slider.html',
           controller: function($scope, $element, $attrs, $parse, $timeout) {
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
        	   if ($scope.options.max || $scope.options.min) {
        	     $scope.sliderValue = $scope.model.dataProviderID;
        	   }
        	   $scope.$watch('model.dataProviderID', function() {
        	   		// a watch on the dataprovider to push it to the ui value when dataprovider from server is changed
        		   // set it through a timeout so that it is always later then a max or min setting if that is done at the same time
         	      $timeout(function(){
         	      	$scope.sliderValue = $scope.model.dataProviderID;
         	      });
        	   })
        	   
        	   $scope.$watch('sliderValue', function(newVal) {
        	    if ($scope.model.dataProviderID  !=  $scope.sliderValue) {
        	   		// the ui value is changed, push it into the dataprovider model and call apply
        	     	$scope.model.dataProviderID = $scope.sliderValue;
        	     	$scope.svyServoyapi.apply('dataProviderID')
        	     }
        	    });
        	   
        	   $element.slider($scope.options);
        	   
           },
           link: function($scope, $element, $attrs)
           {
        	   var style = 
        	   {
        	     'position': 'absolute',
        	     'z-index': 2,
        	     'border': 'none',
        	     'cursor': 'pointer'
        	   }
        	   var slider_handle = $element.find('.ui-slider-handle');
        	   if (slider_handle)
        	   {
        		   if (slider_handle.parent().hasClass('ui-slider-vertical'))
        		   {
        			   style.background = 'url(servoycore/slider/css/images/handle-vertical.png) no-repeat';
        			   style.width = '15px';
        			   style.height = '24px';
        			   style.left = '-3px';
        			   style['margin-left'] = '0';
        			   style['margin-top'] = '-0.6em';
        		   }
        		   else
        		   {
        			   style.background = 'url(servoycore/slider/css/images/handle.png) no-repeat';
        			   style.width = '25px';
        			   style.height = '16px';
        			   style.top = '-4px';
        			   style['margin-left'] = '-0.6em';        			   
        		   }
        		   slider_handle.css(style);
        	   }
        	   
        	   var slider_range = $element.find('.ui-slider-range');
        	   if (slider_range) slider_range.css({background: '#ccc url(servoycore/slider/css/images/ui-bg_highlight-soft_75_cccccc_1x100.png) 50% 50% repeat-x'})
        	   
           },
           replace: true
        }
 	})