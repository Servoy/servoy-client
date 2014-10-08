angular.module('servoydefaultHtmlarea',['servoy','ui.tinymce']).directive('servoydefaultHtmlarea', function() {  
	return {
		restrict: 'E',
		transclude: true,
		scope: {
			model: "=svyModel",
			handlers: "=svyHandlers",
			api: "=svyApi"
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
						$scope.$watch('model.dataProviderID',function (newVal,oldVal){    			   		
							if(newVal && oldVal!=newVal){
								ed.setContent(newVal)
							}    			   		
						})
						$scope.$watch('model.editable',function (newVal,oldVal){    			   		
							if(oldVal != newVal){
								ed.getBody().setAttribute('contenteditable', newVal);
							}    			   		
						})
						ed.on('blur ExecCommand', function () {    				 
							$scope.model.dataProviderID = '<html><head></head><body>'+ed.getContent()+'</body></html>'
							$scope.$apply(function(){    	                	
								$scope.handlers.svy_apply('dataProviderID');
							})    	                
						});
					}
			}

			$scope.api.setScroll = function(x, y) {
				$($scope.editor.getWin()).scrollLeft(x);
				$($scope.editor.getWin()).scrollTop(y);
			}

			$scope.api.getScrollX = function() {
				return $($scope.editor.getWin()).scrollLeft();
			}

			$scope.api.getScrollY = function() {
				return $($scope.editor.getWin()).scrollTop();
			}

			$scope.api.replaceSelectedText = function (s){
				var selection = $scope.editor.selection;
				//useless 'getContent' call, do not remove though, setContent will not work if removed
				selection.getContent();
				selection.setContent(s);
			}
			$scope.api.selectAll = function () {
				var ed = $scope.editor;
				ed.selection.select(ed.getBody(), true);
			}

			// special method that servoy calls when this component goes into find mode.
			$scope.api.setFindMode = function(findMode, editable) {
				$scope.findMode = findMode;
				if (findMode)
				{
					$scope.wasEditable = $scope.model.editable;
					if (!$scope.model.editable) $scope.model.editable = editable;
				}
				else
				{
					$scope.model.editable = $scope.wasEditable;
				}
			};

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

