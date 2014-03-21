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
      
       
       var editor = null;
       //evaluated by ui-tinymce directive
       $scope.tinyConfig ={
    		   /*overwrite ui-tinymce setup routine()*/
    		   setup: function(ed){
    			   	  editor = ed;
    			   	  
    			   	$scope.$watch('model.dataProviderID',function (newVal,oldVal){    			   		
    			   		if(newVal && oldVal!=newVal){
    			   			input =newVal;
    			  		  if (input && input.indexOf('<body') >=0 && input.lastIndexOf('</body') >=0)
    					  {
    						  input = input.substring(input.indexOf('<body')+6,input.lastIndexOf('</body'));
    					  }
    			   		  ed.setContent(input)
    			   		}    			   		
    			   	})
    			    ed.on('blur ExecCommand', function () {    				 
    	                $scope.model.dataProviderID = ed.getContent()
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
	
	console.log("aaaaa");
	var ServoyTinyMCESettings = {
		menubar : false,
		statusbar : false,
		plugins: 'tabindex resizetocontainer',
		tabindex: 'element',
		toolbar: 'fontselect fontsizeselect | bold italic underline | superscript subscript | undo redo |alignleft aligncenter alignright alignjustify | styleselect | outdent indent bullist numlist'
	}

	angular.extend(uiTinymceConfig,ServoyTinyMCESettings)
})

