angular.module('servoyWindowManager',[])
.factory('$servoyWindowManager', ['$timeout', '$rootScope','$http','$q','$templateCache','$injector','$controller','$compile',
                           function($timeout, $rootScope,$http,$q ,$templateCache,$injector,$controller,$compile) {
	
	var WM = new WindowManager();
	 
	return {
		open : function (windowOptions) {
		        var dialogResultDeferred = $q.defer();
	            var dialogOpenedDeferred = $q.defer();

	         //prepare an instance of a window to be injected into controllers and returned to a caller
	            var windowInstance =windowOptions.windowInstance; 
	            
	        //merge and clean up options
	            windowOptions.resolve = windowOptions.resolve || {};
	        //verify options
	            if (!windowOptions.template && !windowOptions.templateUrl) {
	              throw new Error('One of template or templateUrl options is required.');
	            }
	            
	        // wait for templateURL and resolve options
	        var templateAndResolvePromise =
	                $q.all([getTemplatePromise(windowOptions)].concat(getResolvePromises(windowOptions.resolve)));
	           	        
	          templateAndResolvePromise.then(function(tplAndVars){	  
	        //initialize dialog scope and controller	  
	              var windowScope = (windowOptions.scope || $rootScope).$new();
	              windowScope.$close = windowInstance.close;
	              windowScope.$dismiss = windowInstance.dismiss;

	              var ctrlLocals = {};
	              var resolveIter = 1;

	              //controllers
	              if (windowOptions.controller) {
	                ctrlLocals.$scope = windowScope;
	                ctrlLocals.windowInstance = windowInstance;
	                angular.forEach(windowOptions.resolve, function (value, key) {
	                  ctrlLocals[key] = tplAndVars[resolveIter++];
	                });

	                $controller(windowOptions.controller, ctrlLocals);
	              }  	        	  
	           var isModal = (ctrlLocals.windowInstance.type == 1);
	           
	        //resolve initial bounds
	           var location = null;
	           var size = null;
	           if(windowInstance.initialBounds){
	        	   var bounds = windowInstance.initialBounds;
	        	   location = {x:bounds.x,
	        			       y:bounds.y};
	        	   size = {width:bounds.width,height:bounds.height}
	           }
	           if(windowInstance.location){
	        	   location = windowInstance.location;	        	   
	           }
	           if(windowInstance.size){
	        	   size = windowInstance.size;
	           }
	           if(!location){
	        	   location=centerWindow(windowInstance.formSize)
	           }
	        //create the bs window instance
	        	var win = WM.createWindow({
	        		id:windowInstance.name,
	                template: tplAndVars[0],
	                title: "Loading...",
	                bodyContent: "Loading...",
	                resizable:!!windowInstance.resizable,
	                location:location,
	                size:size,
		            isModal:isModal 
	            })
	            var compiledWin = $compile(win.$el)(windowScope);
	        	//set servoy managed bootstrap-window Instance
	        	windowInstance.bsWindowInstance =win; 
	          },function resolveError(reason) {
	            	dialogResultDeferred.reject(reason);
	          });
	        
	        //notify dialog opened or error	
	           templateAndResolvePromise.then(function () {
	                dialogOpenedDeferred.resolve(true);
	              }, function () {
	                dialogOpenedDeferred.reject(false);
	           });	        	
	           
	        return dialogOpenedDeferred.promise;
		}
	}	
	
	
//utiliy functions	
    function getTemplatePromise(options) {
        return options.template ? $q.when(options.template) :
          $http.get(options.templateUrl, {cache: $templateCache}).then(function (result) {
            return result.data;
          });
    }
    
    function getResolvePromises(resolves) {
        var promisesArr = [];
        angular.forEach(resolves, function (value) {
          if (angular.isFunction(value) || angular.isArray(value)) {
            promisesArr.push($q.when($injector.invoke(value)));
          }
        });
        return promisesArr;
     }
    function centerWindow(formSize){
    	var body = $('body');
    	var browserWindow =  $(window);
        var top, left,
            bodyTop = parseInt(body.position().top, 10) + parseInt(body.css('paddingTop'), 10);
            left = (browserWindow.width() / 2) - (formSize.width / 2);
            top = (browserWindow.height() / 2) - (formSize.height / 2);
        if (top < bodyTop) {
            top = bodyTop;
        }
       return {x:left,y:top}
    };
    
}]);