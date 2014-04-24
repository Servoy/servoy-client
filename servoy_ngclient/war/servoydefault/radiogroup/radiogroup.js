angular.module('svyRadiogroup',['servoy']).directive('svyRadiogroup', function($utils) {  
    return {
      restrict: 'E',
      transclude: true,
      scope: {
        model: "=svyModel",
        handlers: "=svyHandlers",
        api: "=svyApi"
      },
      controller: function($scope, $element, $attrs) {
          $scope.notNull = $utils.notNull // TODO remove the need for this
          $scope.style = {width:'100%',height:'100%'}
          angular.extend($scope.style ,$utils.getScrollbarsStyleObj($scope.model.scrollbars));
          
          $scope.api.setScroll = function(x, y) {
         	 $element.scrollLeft(x);
         	 $element.scrollTop(y);
          }
          
          $scope.api.getScrollX = function() {
         	 return $element.scrollLeft();
          }
          
          $scope.api.getScrollY = function() {
         	 return $element.scrollTop();
          }
      },
      templateUrl: 'servoydefault/radiogroup/radiogroup.html',
      replace: true
    };
  })

  
  
  
  
