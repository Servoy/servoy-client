angular.module('svyHtmlarea',['servoy','ui.tinymce']).directive('svyHtmlarea', function() {  
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
      
       //evaluated by ui-tinymce directive
       $scope.tinyConfig ={
    		   /*overwrite ui-tinymce setup routine()*/
    		 setup: function(ed){
    			   	editor = ed;    			   	  
    			   	$scope.$watch('model.dataProviderID',function (newVal,oldVal){    			   		
    			   		if(newVal && oldVal!=newVal){
    			   		  ed.setContent(newVal)
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
      
      },
      templateUrl: 'servoydefault/htmlarea/htmlarea.html',
      replace: true
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

