angular.module('servoydefaultHtmlarea',['servoy','ui.tinymce']).directive('servoydefaultHtmlarea', function($apifunctions, $sabloConstants, $svyProperties,$applicationService, $timeout, $sabloApplication) {  
	return {
		restrict: 'E',
		scope: {
			model: "=svyModel",
			handlers: "=svyHandlers",
			api: "=svyApi",
			svyServoyapi: "="
		},
		controller: function($scope, $element) {
			var lastServerValueAsSeenByTinyMCEContent;
            function setEditable(ed) {
                if (!$scope.init) return;
                var editable = $scope.model.editable && !$scope.model.readOnly && $scope.model.enabled;
                editable ? ed.setMode('design') : ed.setMode('readonly')
                ed.getBody().setAttribute('contenteditable', editable);
            }
			$scope.findMode = false;
			$scope.tinyValue = !$scope.svyServoyapi.isInDesigner() && $scope.model.dataProviderID ? $scope.model.dataProviderID : '' ;
			$scope.init = false;
			//evaluated by ui-tinymce directive
			$scope.tinyConfig ={
					/*overwrite ui-tinymce setup routine()*/
					setup: function(ed){
						$scope.editor = ed;
						ed.settings.height = "100%";
						ed.on('init', function() {
							$scope.init = true;
							setEditable(ed);
							const editable = $scope.model.editable && !$scope.model.readOnly && $scope.model.enabled;
							if(editable){
								ed.settings.readonly = 0;				
							}
							else{
								ed.settings.readonly = 1;
							}							
							
							if (!$scope.svyServoyapi.isInDesigner())
							{
								// see comment below where lastServerValueAsSeenByTinyMCEContent is set in dataProviderID watch
								ed.setContent($scope.tinyValue || '');
								lastServerValueAsSeenByTinyMCEContent = ed.getContent(); 
							}
							$svyProperties.setScrollbars($(ed.getBody()), $scope.model.scrollbars);
						});
						$scope.$watch('model.editable',function (newVal,oldVal){
							if(oldVal != newVal){
								setEditable(ed);
							}    			   		
						})
						$scope.$watch('model.enabled',function (newVal,oldVal){
                            if(oldVal != newVal){
                                setEditable(ed);
                            }                       
                        })
                        $scope.$watch('model.readOnly',function (newVal,oldVal){
                            if(oldVal != newVal){
                                setEditable(ed);
                            }                       
                        })
						$scope.$watch('model.dataProviderID', function(newVal, oldVal) {
							if (!$scope.svyServoyapi.isInDesigner())
							{
								// only update tinyValue (and content) on init or if content really changed, so we don't lose current selection
								if (!$scope.init || newVal != '<html><body>' + ed.getContent() + '</body></html>') {
									$scope.tinyValue = newVal;

									if ($scope.init) {
										// tinyValue set above would do that later but we need to set it right now so we can see the value of ed.getContent() for this input;
										// for example DP is "aaa" then we set content and tinyValue to "aaa" but ed.getContent() will return "<p>aaa</p>" and when we loose focus,
										// if the user didn't change the value we need to know that so that we don't send a DP change to server for no reason (see SVY-12158 for unwanted side effects of that)
										ed.setContent(newVal|| '');
										lastServerValueAsSeenByTinyMCEContent = ed.getContent();
									}
								}
							}
						})

						ed.on('blur ExecCommand', function () {
							var edContent = ed.getContent();
							if (lastServerValueAsSeenByTinyMCEContent != edContent) {
								$scope.model.dataProviderID = '<html><body>' + edContent + '</body></html>';
								lastServerValueAsSeenByTinyMCEContent = edContent;
								$scope.$apply(function() {
									$scope.svyServoyapi.apply('dataProviderID');
								});
							}
						});
						
						 ed.on('click',function(e) {
							 if ($scope.handlers.onActionMethodID)
							 {
								 $scope.handlers.onActionMethodID(createEvent(e));
							 } 
					     });
						 ed.on('contextmenu',function(e) {
							if ($scope.handlers.onRightClickMethodID)
							{
								$scope.handlers.onRightClickMethodID(createEvent(e));
								e.preventDefault();
							} 
						 });
						 ed.on('focus',function(e) {
						  if ($scope.mustExecuteOnFocusGainedMethod !== false && $scope.handlers.onFocusGainedMethodID)
						  {
							 $scope.handlers.onFocusGainedMethodID(createEvent(e));
						  }
					     });
						 ed.on('blur',function(e) {
							 if ($scope.handlers.onFocusLostMethodID)
							 {
								 $scope.handlers.onFocusLostMethodID(createEvent(e));
							 }
					     });
						 
						 var createEvent = function(e)
						 {
							 var ev = new MouseEvent(e.type);
							 ev.initMouseEvent(e.type, e.bubbles, e.cancelable,null,e.detail, e.screenX, e.screenY, e.clientX, e.clientY, e.ctrlKey, e.altKey, e.shiftKey, e.metaKey, e.button, null);
							 return ev;
						 }
					}
			}
            var locale = $sabloApplication.getLocale();
            if (locale && locale.language) {
                $scope.tinyConfig.language = locale.language;
            }
			// app level configuration
			var defaultConfiguration = $applicationService.getUIProperty("config");
			if (defaultConfiguration)
			{
				try
				{
					defaultConfiguration = JSON.parse(defaultConfiguration);
				}
				catch(e)
				{
					console.error(e)
				}
				for (var key in defaultConfiguration)
				{
					if (defaultConfiguration.hasOwnProperty(key))
					{
						var value = defaultConfiguration[key]
						if (key === "plugins")
						{
							value += " tabindex";
						}
						$scope.tinyConfig[key] = value;
					}
				}
			}
			
			// element level configuration
			var configuration = $scope.model.clientProperty ? $scope.model.clientProperty['config'] : null;
			if (configuration)
			{
				try
				{
					configuration = JSON.parse(configuration);
				}
				catch(e)
				{
					console.error(e)
				}
				for (var key in configuration)
				{
					if (configuration.hasOwnProperty(key))
					{
						var value = configuration[key];
						if (key === "plugins")
						{
							value += " tabindex";
						}
						$scope.tinyConfig[key] = value;
					}
				}
			}
			
			/**
	      	 * Sets the scroll location of an element. It takes as input the X (horizontal) and Y (vertical) coordinates - starting from the TOP LEFT side of the screen - only for an element where the height of the element is greater than the height of element content
	      	 * NOTE: getScrollX() can be used with getScrollY() to return the current scroll location of an element; then use the X and Y coordinates with the setScroll function to set a new scroll location. 
	      	 * For Example:
	      	 * //returns the X and Y coordinates
	      	 * var x = forms.company.elements.myarea.getScrollX();
	      	 * var y = forms.company.elements.myarea.getScrollY();
	     	 * //sets the new location
	      	 * forms.company.elements.myarea.setScroll(x+10,y+10);
	      	 * @example
	      	 * %%prefix%%%%elementName%%.setScroll(200,200);
	      	 *
	      	 * @param x the X coordinate of the htmlarea scroll location in pixels
	      	 * @param y the Y coordinate of the htmlarea scroll location in pixels
	      	 */
			$scope.api.setScroll = function(x, y) {
				$($scope.editor.getWin()).scrollLeft(x);
				$($scope.editor.getWin()).scrollTop(y);
			}

			/**
	      	  * Returns the x scroll location of specified element - only for an element where height of element is less than the height of element content. 
	      	  * NOTE: getScrollX() can be used with getScrollY() to set the scroll location of an element using the setScroll function. 
	      	  * For Example:
	      	  * //returns the X and Y scroll coordinates
	      	  * var x = forms.company.elements.myarea.getScrollX();
	      	  * var y = forms.company.elements.myarea.getScrollY(); 
	      	  * //sets the new scroll location
	      	  * forms.company.elements.myarea.setScroll(x+10,y+10);
	      	  * @example
	      	  * var x = %%prefix%%%%elementName%%.getScrollX();
	      	  * 
	       	  * @return The x scroll location in pixels.
	       	  */
			$scope.api.getScrollX = function() {
				return $($scope.editor.getWin()).scrollLeft();
			}

			/**
	           * Returns the y scroll location of specified element - only for an element where height of element is less than the height of element content.
	           * NOTE: getScrollY() can be used with getScrollX() to set the scroll location of an element using the setScroll function. For Example:
	           * //returns the X and Y scroll coordinates
	           * var x = forms.company.elements.myarea.getScrollX();
	           * var y = forms.company.elements.myarea.getScrollY();
	           * //sets the new scroll location
	           * forms.company.elements.myarea.setScroll(x+10,y+10);
	           * @example
	           * var y = %%prefix%%%%elementName%%.getScrollY(); 
	           * @return The y scroll location in pixels.
	           */
			$scope.api.getScrollY = function() {
				return $($scope.editor.getWin()).scrollTop();
			}

			/**
			 * Replaces the selected text; if no text has been selected, the replaced value will be inserted at the last cursor position.
			 * @example %%prefix%%%%elementName%%.replaceSelectedText('John');
			 * @param s The replacement text.
			 */
			$scope.api.replaceSelectedText = function (s){
				var selection = $scope.editor.selection;
				//useless 'getContent' call, do not remove though, setContent will not work if removed
				selection.getContent();
				selection.setContent(s);
				
				var edContent = $scope.editor.getContent();
				if (lastServerValueAsSeenByTinyMCEContent != edContent) {
					$scope.model.dataProviderID = '<html><body>' + edContent + '</body></html>'
					lastServerValueAsSeenByTinyMCEContent = edContent;
					$scope.svyServoyapi.apply('dataProviderID');
				}
			}
			/**
			 * Selects all the contents of the Html Area.
			 * @example %%prefix%%%%elementName%%.selectAll();
			 */
			$scope.api.selectAll = function () {
				var ed = $scope.editor;
				ed.selection.select(ed.getBody(), true);
			}
			
			 /**
			 * Returns the currently selected text in the specified Html Area. 
			 * @example var my_text = %%prefix%%%%elementName%%.getSelectedText();
			 * @return {String} The selected text in the Html Area.
			 */
			$scope.api.getSelectedText = function() {
				return $scope.editor.selection.getContent();
			}
			
			/**
			 * Gets the plain text for the formatted Html Area.
			 * @example var my_text = %%prefix%%%%elementName%%.getAsPlainText();
			 * @return the plain text
			 */
			$scope.api.getAsPlainText = function() {
				return $scope.editor.getContent().replace(/<[^>]*>/g, '');
			}
			
	        /**
	    	* Set the focus to this Html Area.
	    	* @example %%prefix%%%%elementName%%.requestFocus();
			* @param mustExecuteOnFocusGainedMethod (optional) if false will not execute the onFocusGained method; the default value is true
	    	*/
			$scope.api.requestFocus = function(mustExecuteOnFocusGainedMethod) {
				$scope.mustExecuteOnFocusGainedMethod = mustExecuteOnFocusGainedMethod;
				if ($scope.editor.initialized)
				{
					$scope.editor.focus();
					delete $scope.mustExecuteOnFocusGainedMethod;
				}
				else {
					$timeout(function(){
						$scope.api.requestFocus(mustExecuteOnFocusGainedMethod);
					},10);
				}
			}
			
			$scope.api.getWidth = $apifunctions.getWidth($element[0]);
			$scope.api.getHeight = $apifunctions.getHeight($element[0]);
			$scope.api.getLocationX = $apifunctions.getX($element[0]);
			$scope.api.getLocationY = $apifunctions.getY($element[0]);		
			
			var element = $element.children().first();
			var className = null;
			Object.defineProperty($scope.model, 	$sabloConstants.modelChangeNotifier, {
				configurable : true,
				value : function(property, value) {
					switch (property) {
					case "borderType":
						$svyProperties.setBorder(element, value);
						break;
					case "background":
					case "transparent":
						$svyProperties.setCssProperty(element, "backgroundColor", $scope.model.transparent ? "transparent" : $scope.model.background);
						break;
					case "foreground":
						$svyProperties.setCssProperty(element, "color", value);
						break;
					case "fontType":
						$svyProperties.setCssProperty(element, "font", value);
						break;
					case "margin":
						if (value)
							element.css(value);
						break;
					case "styleClass":
						if (className)
							element.removeClass(className);
						className = value;
						if (className)
							element.addClass(className);
						break;
					}
				}
			});
			$svyProperties.createTooltipState(element, function() { return $scope.model.toolTipText });
			var destroyListenerUnreg = $scope.$on("$destroy", function() {
				destroyListenerUnreg();
				delete $scope.model[$sabloConstants.modelChangeNotifier];
			});
			// data can already be here, if so call the modelChange function so
			// that it is initialized correctly.
			var modelChangFunction = $scope.model[$sabloConstants.modelChangeNotifier];
			for (var key in $scope.model) {
				modelChangFunction(key, $scope.model[key]);
			}
			
		},
		templateUrl: 'servoydefault/htmlarea/htmlarea.html'
	};
}).run(function(uiTinymceConfig){
	var ServoyTinyMCESettings = {
			menubar : false,
			statusbar : false,
			readonly: 0,
			plugins: 'tabindex',
			tabindex: 'element',
			toolbar: 'fontselect fontsizeselect | bold italic underline | superscript subscript | undo redo |alignleft aligncenter alignright alignjustify | styleselect | outdent indent bullist numlist'
	}
	angular.extend(uiTinymceConfig,ServoyTinyMCESettings)
})

