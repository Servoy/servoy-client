servoyModule.directive('signaturefield', function() {
        return {
           restrict : 'E',
           scope : {
        	   model: '=svyModel',
        	   svyApply: '='
           },
           templateUrl : 'webcomponents/signaturefield/signaturefield.html',
           controller: function($scope, $element, $attrs, $parse) {
               var options = {
                   drawOnly : true,
                   lineColour : "#fff",
                    onDrawEnd: function() {
                     var api = $('.sigPad').signaturePad();
                  	 $scope.$apply(function () {
                  		 $scope.model.signatureValue = api.getSignatureString();
                  	 });
                  	 $scope.svyApply('signatureValue');
                   }
               };
               
               $scope.$watch('model.signatureValue', function() {
                   if($scope.model.signatureValue) $('.sigPad').signaturePad(options).regenerate($scope.model.signatureValue);
               })
               
               $scope.$watch(function() {
                    return $element.find('canvas').attr('width');
               }, function() {
                    if($scope.model.signatureValue) $('.sigPad').signaturePad(options).regenerate($scope.model.signatureValue);
               })

               $scope.$watch(function() {
                    return $element.find('canvas').attr('height');
               }, function() {
                    if($scope.model.signatureValue) $('.sigPad').signaturePad(options).regenerate($scope.model.signatureValue);
               })
               
               $('.sigPad').signaturePad(options);
               
               $scope.clear = function() {
               		$scope.model.signatureValue = null;
               }
           },
           replace: true
        }
    })