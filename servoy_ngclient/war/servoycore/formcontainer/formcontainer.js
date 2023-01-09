angular.module('servoycoreFormcontainer',['servoy']).directive('servoycoreFormcontainer', ['$sabloApplication', '$q', function($sabloApplication, $q) {  

	return {
		restrict: 'E',
		scope: {
			model: "=svyModel",
			svyServoyapi: "=",
			handlers: "=svyHandlers",
			api: "=svyApi"
		},
		controller: function($scope, $element, $attrs) {

			var realContainedForm;
			var formWillShowCalled;

			function setRealContainedForm (formname, relationname) {
				if ($scope.model.visible) {
					if (formWillShowCalled != formname && formname) {
						formWillShowCalled = formname;
						if ($scope.model.waitForData) {
							$q.when($scope.svyServoyapi.formWillShow(formname, relationname)).then(function() {
								realContainedForm = formname;
							});
						} else {
							$scope.svyServoyapi.formWillShow(formname, relationname);
							realContainedForm = formname;
						}
					}
				} else {
					// panel is not visible; don't ask server to show child form as that would generate an exception on server
					realContainedForm = formWillShowCalled = undefined;
				}
			}

			$scope.getActiveTabUrl = function() {
				if (realContainedForm)
				{
					return $scope.svyServoyapi.getFormUrl(realContainedForm)
				}  
				setRealContainedForm($scope.model.containedForm, $scope.model.relationName);

				return "";
			}

			setRealContainedForm($scope.model.containedForm, $scope.model.relationName);

			$scope.$watch("model.containedForm", function(newValue,oldValue) {
				if (newValue !== oldValue)
				{
					if (oldValue) {
						formWillShowCalled = newValue;
						$scope.svyServoyapi.hideForm(oldValue,null,null,newValue,$scope.model.relationName,null).then(function(ok) {
							realContainedForm = $scope.model.containedForm;
						})
					}
					else if (newValue) {
						setRealContainedForm(newValue, $scope.model.relationName);
					}
				}	
			});

			$scope.$watch("model.visible", function(newValue,oldValue) {
				if ($scope.model.containedForm && newValue !== oldValue)
				{
					formWillShowCalled = realContainedForm = undefined;
					if (newValue)
					{
						setRealContainedForm($scope.model.containedForm, $scope.model.relationName);
					}
					else
					{
						$scope.svyServoyapi.hideForm($scope.model.containedForm);
					}	
				}	
			});

			$scope.getContainerStyle = function() {
                var style = {position:"relative"}
				var height = 0;
				if ($scope.model.height && $scope.model.height !== '0')
				{
					height = $scope.model.height
				}
				else if ($scope.model.containedForm && $sabloApplication.hasFormStateWithData($scope.model.containedForm))
				{
					// for absolute form default height is design height, for responsive form default height is 0
					var formState = $sabloApplication.getFormStateEvenIfNotYetResolved($scope.model.containedForm);
					if (formState && formState.absoluteLayout)
					{
						height = formState.properties.designSize.height; 
					}	  
				}
                if (height > 0)
                {
                    style.minHeight = height+"px";
                }	  
				return style;
			}

			$scope.showEditorHint = function()
			{
				return !$scope.model.containedForm && $element[0].getAttribute("svy-id") !== null;
			}

		},
		templateUrl: 'servoycore/formcontainer/formcontainer.html'
	};
}]);
