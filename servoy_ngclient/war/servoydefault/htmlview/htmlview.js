angular.module('servoydefaultHtmlview',['servoy']).directive('servoydefaultHtmlview', function() {  
    return {
      restrict: 'E',
      scope: {
      	model: "=svyModel",
      	api: "=svyApi",
      	handlers: "=svyHandlers"
      },
      link: function($scope, $element, $attrs,ngModelController) {
       $scope.style = {width:'100%',height:'100%',overflow:'auto'}
       $scope.bgstyle = {left:'0',right:'0',top:'0',height:'100%',position:'relative',display:'block',};
       
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
      templateUrl: 'servoydefault/htmlview/htmlview.html'
 };
})