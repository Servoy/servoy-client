angular.module('servoyWindowManager',[])
.factory('$servoyWindowManager', ['$timeout', '$rootScope','$http','$q','$templateCache','$injector','$controller','$compile',
                           function($timeout, $rootScope,$http,$q ,$templateCache,$injector,$controller,$compile) {
	
	var WM = new WindowManager();
	 
	return {
		open : function (windowOptions) {
		        var dialogResultDeferred = $q.defer();
	            var dialogOpenedDeferred = $q.defer();

	         //prepare an instance of a window to be injected into controllers and returned to a caller
	            var windowInstance = {
	              bsWindowInstance:null,		
	              result: dialogResultDeferred.promise,
	              opened: dialogOpenedDeferred.promise,
	              close: function (result) {
	            	  //$modalStack.close(modalInstance, reason);
	              },
	              dismiss: function (reason) {
	            	  windowInstance.bsWindowInstance.close();	              
	              }
	            };
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
	                ctrlLocals.$windowInstance = windowInstance;
	                angular.forEach(windowOptions.resolve, function (value, key) {
	                  ctrlLocals[key] = tplAndVars[resolveIter++];
	                });

	                $controller(windowOptions.controller, ctrlLocals);
	              }  	        	  
	           var isModal = (ctrlLocals.windowType == 1);
	        	var win = WM.createWindow({
	        		id:ctrlLocals.windowName,
	                template: tplAndVars[0],
	                title: "Loading...",
	                bodyContent: "Loading...",
		            isModal:isModal 
	            })
	            var compiledWin = $compile(win.$el)(windowScope);
	        	//set servoy managed window Instance
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
	           
	        return windowInstance;
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
}]);