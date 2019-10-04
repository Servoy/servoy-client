angular.module('servoycoreFormcomponent',['servoy']).directive('servoycoreFormcomponent', ['$compile', '$sabloConstants',function($compile, $sabloConstants) {
    return {
        restrict : 'E',
        scope : {
     	   model: '=svyModel',
     	   api : "=svyApi",
     	   svyServoyapi: "=",
     	   handlers: "=svyHandlers"
        },
        controller: function($scope, $element, $attrs)
        {
     	   function createContent() {
     		   $element.empty();
     		   var newValue = $scope.model.containedForm;
     		   if (newValue) {       			   
     			   var elements = $scope.svyServoyapi.getFormComponentElements("containedForm", newValue);
     			   var height = $scope.model.height;
     			   var width = $scope.model.width;
     			   
     			   // set the styleClass
     			   var styleClass = "svy-formcomponent";
     			   if ($scope.model.styleClass) styleClass += " " + $scope.model.styleClass;
     			   
     			   if (newValue.absoluteLayout) {
	        			   if (!height) height = newValue.formHeight;
	        			   if (!width) width = newValue.formWidth;
     			   }
     			   if (height || width) { 	// is absolute. Why not to use newValue.absoluteLayout !?
     				   var template = "<div style='position:relative;";
     				   if (height) template += "height:" +height + "px;"
     				   if (width) template += "width:" +width + "px;"
     				   template += "'";
     				   template += " class='svy-wrapper " + styleClass + "'";
     				   template += "></div>";
     				   var div = $compile(template)($scope);
     				   div.append(elements);
     				   $element.append(div);
     			   } else  {	// is responsive
     				   // add the styleClass to the angular data-x element
     			   	   if (styleClass) $element.addClass(styleClass);
     				   $element.append(elements);
     			   }
     		   }
     		   else {
     			   $element.html("<div>FormComponentContainer, select a form</div>");
     		   }
     	   }
     	   $scope.$watch("model.containedForm", function() { 
     		  createContent();
     	   });
     	   if ($scope.svyServoyapi.isInDesigner()) {
     		   var previousWidth = $scope.model.width;
     		   var previousHeigth = $scope.model.height;
     		   $scope.$watch("model.width", function() { 
     			  if (previousWidth != $scope.model.width) {
	             		  createContent();
	             		  previousWidth = $scope.model.width;
     			  }
          	   });
     		   $scope.$watch("model.height", function() { 
      			  if (previousHeigth != $scope.model.height) {
               		  createContent();
               		  previousHeigth = $scope.model.height;
     			  }
           	   });
     	   }
     },
		link:  function($scope, $element, $attrs) {
			
			var className = $scope.model.styleClass;
			Object.defineProperty($scope.model, $sabloConstants.modelChangeNotifier, {
						configurable: true,
						value: function(property, value) {
							switch (property) {
							case "styleClass":
								if ($scope.model.containedForm) {
									if ( $scope.model.containedForm.absoluteLayout) {
										// TODO does not work for responsive forms
										var div = $element.children()[0];
										if (div) {
											var wrapper = $(div);
											if (wrapper.hasClass('svy-wrapper')) {
												if (className) wrapper.removeClass(className);
												className = value;
												if (className) wrapper.addClass(className);
											}
										}
									} else {
										/* Considerations:
										 * Adding the styleClass to the parent element may mess up the parent div layout. Also it breaks the encapsulation
										 * The actual content of the form-component may have multiple divs, what do i do, i add the styleclass to each div !?
										 * What would break if i add the styleClass to the data-forcomponent-container element ?
										 *
										 * Let's think of some extreme use cases
										 * - display: block|inline|none
										 * - height|width
										 * - styleClass .hidden
										 *
										 * 	The developer is responsible to wrap the containedForm into the proper div and style it properly.
										 * 	The display|height|width type should not be applied to the parent itself but to the content of the contained form. e.g. .hidden-formcomponent > * { display: none }
										 * 	Alternatevely the containedForm should be contained by a div, without other sibilings. e.g. row > col > div > containedForm
										 * */
										
										// add it to the data-formcomponent-container
										if (className) $element.removeClass(className);
										className = value;
										if (className) $element.addClass(className);
									}
								}
								break;
							}
						}
					});
			var destroyListenerUnreg = $scope.$on("$destroy", function() {
				destroyListenerUnreg();
				delete $scope.model[$sabloConstants.modelChangeNotifier];
			});
			
			// data can already be here, if so call the modelChange function so that it is initialized correctly.
			var modelChangeFunction = $scope.model[$sabloConstants.modelChangeNotifier];
			for (var key in $scope.model) {
				modelChangeFunction(key,$scope.model[key]);
			}
		}
 }
}])