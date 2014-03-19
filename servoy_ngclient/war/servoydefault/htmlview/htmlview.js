servoyModule.directive('svyHtmlview', function() {  
    return {
      restrict: 'E',
      transclude: true,
      scope: {
      	model: "=svyModel",
      	api: "=svyApi"
      },
      link: function($scope, $element, $attrs,ngModelController) {
       $scope.style = {width:'100%',height:'100%',overflow:'hidden'}
       $scope.bgstyle = {left:'0',right:'0',top:'0',height:'100%',position:'relative',display:'block',};
      },
      templateUrl: 'servoydefault/htmlview/htmlview.html',
      replace: true
    };
})