angular.module('servoydefaultHtmlarea',['servoy','ui.tinymce']).directive('servoydefaultHtmlarea', function() {  
	return {
		restrict: 'E',
		scope: {
			model: "=svyModel",
			handlers: "=svyHandlers",
			api: "=svyApi",
			svyServoyapi: "="
		},
		controller: function($scope, $element, $attrs) {
			$scope.style = {width:'100%',height:'100%',overflow:'hidden'}     
			$scope.findMode = false;
			//evaluated by ui-tinymce directive
			$scope.tinyConfig ={
					/*overwrite ui-tinymce setup routine()*/
					setup: function(ed){
						editor = ed;
						$scope.editor = editor;
						$scope.$watch('model.editable',function (newVal,oldVal){    			   		
							if(oldVal != newVal){
								ed.getBody().setAttribute('contenteditable', newVal);
							}    			   		
						})
						ed.on('blur ExecCommand', function () {    				 
							$scope.model.dataProviderID = '<html><body>'+ed.getContent()+'</body></html>'
							$scope.$apply(function(){    	                	
								$scope.svyServoyapi.apply('dataProviderID');
							})    	                
						});
						
						 ed.on('click',function(e) {
							 $scope.handlers.onActionMethodID(createEvent(e));
					     });
						 ed.on('focus',function(e) {
							 $scope.handlers.onFocusGainedMethodID(createEvent(e));
					     });
						 ed.on('blur',function(e) {
							 $scope.handlers.onFocusLostMethodID(createEvent(e));
					     });
						 
						 var createEvent = function(e)
						 {
							 var ev = new MouseEvent(e.type);
							 ev.initMouseEvent(e.type, e.bubbles, e.cancelable,null,e.detail, e.screenX, e.screenY, e.clientX, e.clientY, e.ctrlKey, e.altKey, e.shiftKey, e.metaKey, e.button, null);
							 return ev;
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

		},
		templateUrl: 'servoydefault/htmlarea/htmlarea.html'
	};
}).run(function(uiTinymceConfig){
	var ServoyTinyMCESettings = {
			menubar : false,
			statusbar : false,
			plugins: 'tabindex resizetocontainer',
			tabindex: 'element',
			toolbar: 'fontselect fontsizeselect | bold italic underline | superscript subscript | undo redo |alignleft aligncenter alignright alignjustify | styleselect | outdent indent bullist numlist'
	}
	angular.extend(uiTinymceConfig,ServoyTinyMCESettings)
})

