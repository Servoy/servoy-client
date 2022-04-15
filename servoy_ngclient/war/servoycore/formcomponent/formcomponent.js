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
           function getModelMinWidth() {
               return $scope.model.minWidth !== undefined ? $scope.model.minWidth : $scope.model.width; // width is deprecated in favor of minWidth but they do the same thing;
           }
            
           function getModelMinHeight() {
               return $scope.model.minHeight !== undefined ? $scope.model.minHeight : $scope.model.height; // height is deprecated in favor of minHeight but they do the same thing;
           }
            
     	   function createContent() {
     		   $element.empty();
     		   var newValue = $scope.model.containedForm;
     		   if (newValue) {
     			   if ($scope.model.visible)
     			   {
     				   var elements = $scope.svyServoyapi.getFormComponentElements("containedForm", newValue);

     				   // set the styleClass
     				   var styleClass = "svy-formcomponent";
     				   if ($scope.model.styleClass) styleClass += " " + $scope.model.styleClass;

                       var formComponentContentIsAbsolute = newValue.absoluteLayout;
                       var containerFormOfFormComponentIsAbsolute = $scope.svyServoyapi.isInAbsoluteLayout();
     				   if (formComponentContentIsAbsolute) {
                           // see if they are set explicitly through model
         				   var minHeight = getModelMinHeight();
         				   var minWidth = getModelMinWidth();
         				   var widthExplicitlySet;

                           // use form design height/width if not set directly on the component
     					   if (!minHeight) minHeight = newValue.formHeight;
     					   if (!minWidth) {
                               widthExplicitlySet = false;
                               minWidth = newValue.formWidth;
                           } else widthExplicitlySet = true;
     					   
     					   var template = "<div style='position:relative;";
     					   if (minHeight) { // should always be truthy
                                template += "min-height:" + minHeight + "px;";
                                if (containerFormOfFormComponentIsAbsolute) template += "height:100%;"; // allow anchoring to bottom in anchored form + anchored form component
                           }
     					   if (minWidth) { // should always be truthy
                                template += "min-width:" + minWidth + "px;";
                                if (!containerFormOfFormComponentIsAbsolute && widthExplicitlySet) {
                                    // if container is in a responsive form, content is anchored and width model property is explicitly set
                                    // then we assume that developer wants to really set width of the form component so it can put multiple of them inside
                                    // for example a 12grid column; that means they should not simply be div / block elements; we change float as well
                                    template += "float:left;";
                                } 
                           }
     					   template += "'";
     					   template += " class='svy-wrapper " + styleClass + "'";
     					   template += "></div>";
     					   var div = $compile(template)($scope);
     					   div.append(elements);
     					   $element.append(div);
     				   } else  {
                           // form component content is responsive; we don't set width/height/wrapper div
     					   // add the styleClass to the angular data-x element
     					   if (styleClass) $element.addClass(styleClass);
     					   $element.append(elements);
     				   }
     			   }	   
     		   }
     		   else {
     			   $element.html("<div>FormComponentContainer, select a form</div>");
     		   }
     	   }
     	   $scope.$watch("model.containedForm", function() { 
     		  createContent();
     	   });
     	  $scope.$watch("model.visible", function(newValue,oldValue) {
  	  		if (newValue !== oldValue)
  	  		{
  	  			createContent();	
			}	
		  });
     	   if ($scope.svyServoyapi.isInDesigner()) {
     		   var previousWidth = getModelMinWidth();
     		   var previousHeigth = getModelMinHeight();
     		   var minWidthChanged = function() { 
                  if (previousWidth != getModelMinWidth()) {
                      createContent();
                      previousWidth = getModelMinWidth();
                  }
               };
               var minHeightChanged = function() { 
                  if (previousHeigth != getModelMinHeight()) {
                      createContent();
                      previousHeigth = getModelMinHeight();
                  }
               };
     		   $scope.$watch("model.width", minWidthChanged);
               $scope.$watch("model.minWidth", minWidthChanged);
               $scope.$watch("model.height", minHeightChanged);
               $scope.$watch("model.minHeight", minHeightChanged);
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