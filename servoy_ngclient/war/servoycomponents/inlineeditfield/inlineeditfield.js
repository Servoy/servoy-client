angular.module('servoycomponentsInlineeditfield',['servoy']).directive('w', function($timeout) {  
    return {
      restrict: 'E',
      scope: {
    	  model: "=svyModel",
    	  svyApply: '='
      },
      link: function($scope, $element, $attrs) {
         $scope.editModel = {}
         $scope.editMode = false;
         var isMouseDownOnButton = false;
         
         var inputField = $element[0].querySelector('input'); // needed to request focus to the input field .. selector can change if it is a textarea
          
         $scope.edit = function(){
            $scope.editMode = true;
            $scope.editModel = angular.copy($scope.model.dataproviderid);
            $timeout(function(){inputField.focus();},0);
         };
         $scope.applyChanges = function(){
        	isMouseDownOnButton = false;
            $scope.model.dataproviderid = $scope.editModel;
            $scope.svyApply('dataproviderid');
            $scope.exitEditMode();
         };
         /* used to detect if a click is being done on "ok" or "cancel" */
         $scope.setMouseDown =  function(state){
           isMouseDownOnButton = state;
         }
         /* copies the value from the model input to the edit model discarding what was se before in the edit model  */
         $scope.exitEditMode = function($event){   
             $timeout(function() { // blur is fired before any mousedown click event, Postpone to  detect if the blur comes from a click on "ok"/"cancel"
                 if(!isMouseDownOnButton){
	                 $scope.editMode = false;
	                 $scope.editModel = angular.copy($scope.model.dataproviderid);
                }
            },0);
         };
      },
      templateUrl: 'servoycomponents/inlineeditfield/inlineeditfield.html'
    };
  })

  
  
  
  
