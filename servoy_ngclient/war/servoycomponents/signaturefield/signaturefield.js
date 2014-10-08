angular.module('servoycomponentsSignaturefield',['servoy']).directive('servoycomponentsSignaturefield', function() {
        return {
           restrict : 'E',
           scope : {
        	   model: '=svyModel',
        	   svyApply: '='
           },
           templateUrl : 'servoycomponents/signaturefield/signaturefield.html',
           controller: function($scope, $element, $attrs, $parse) {
               var options = {
                   drawOnly : true,
                   lineColour : "#fff",
                    onDrawEnd: function() {
                  	 $scope.$apply(function () {
                  		 $scope.model.signatureValue = $scope.signatureApi.getSignatureString();
                  	 });
                  	 $scope.svyApply('signatureValue');
                   }
               };
               
               $scope.$watch('model.signatureValue', function() {
                   if($scope.model.signatureValue) $scope.signatureApi.regenerate($scope.model.signatureValue);
                   else $scope.signatureApi.clearCanvas();
               })
               
               $scope.$watch(function() {
                    return $element.find('canvas').attr('width');
               }, function() {
                    if($scope.model.signatureValue) $scope.signatureApi.regenerate($scope.model.signatureValue);
               })

               $scope.$watch(function() {
                    return $element.find('canvas').attr('height');
               }, function() {
                    if($scope.model.signatureValue) $scope.signatureApi.regenerate($scope.model.signatureValue);
               })
               
               $scope.signatureApi = $element.signaturePad(options);
               
               $scope.clear = function() {
            	   $scope.model.signatureValue = null;
               }
           }
        }
    })