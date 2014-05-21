angular.module('servoyWindowManager',[])	// TODO Refactor so that window is a component with handlers
.factory('$servoyWindowManager', ['$timeout', '$rootScope','$http','$q','$templateCache','$injector','$controller','$compile',
                           function($timeout, $rootScope,$http,$q ,$templateCache,$injector,$controller,$compile) {
	var WM = new WindowManager();
	var winInstances = {}
	return {
		instances: winInstances,
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
    
}]).factory("$windowService", function($servoyWindowManager, $log, $rootScope, $solutionSettings, $window, $servoyInternal,webStorage) {
	var instances = $servoyWindowManager.instances;
	var formTemplateUrls = {};
	
	return {
		create: function (name,type){
			if(!instances[name]){
				var win = 
					{name:name,
					 type:type,
					 title:'',
					 opacity:1,
					 undecorated:false,
					 bsWindowInstance:null,  // bootstrap-window instance , available only after creation 
				     hide: function (result) {
				    	 win.bsWindowInstance.close();
				    	 if(!this.storeBounds){
				    		  delete this.location;
				    		  delete this.size;
				    	 }
				     },
				     setLocation:function(location){
				    	 this.location = location;
				    	 if(win.bsWindowInstance){
				    		 win.bsWindowInstance.$el.css('left',this.location.x+'px');
					    	 win.bsWindowInstance.$el.css('top',this.location.y+'px'); 
				    	 }				    	 
				     },
				     setSize:function(size){
				    	 this.size = size;
				    	 if(win.bsWindowInstance){
				    		 win.bsWindowInstance.$el.css('width',this.size.width+'px');
					    	 win.bsWindowInstance.$el.css('height',this.size.height+'px'); 
				    	 }				    	 
				     },
				     onResize:function($event,size){
				    	win.size = size;  
				    	//storage.add(JSON.stringify(instances));				    	
				    	$servoyInternal.callService("$windowService", "resize", {name:win.name,size:win.size});
				     },
				     onMove:function($event,location){
				    	 win.location = location;
				    	 $servoyInternal.callService("$windowService", "move", {name:win.name,location:win.location});
				     }
					};
				
				instances[name] = win;
				return win;
			}
			
		},
		show: function(name,arg) {	
			var instance = instances[name];
			if (instance) {
				instance.formSize = arg.formSize;
				$servoyWindowManager.open({
					templateUrl: "templates/dialog.html",
					controller: "DialogInstanceCtrl",
					windowClass: "tester",
					windowInstance:instance
				}).then(function(){
					instance.bsWindowInstance.$el.on('bswin.resize',instance.onResize)
					instance.bsWindowInstance.$el.on('bswin.move',instance.onMove)
				});
				instance.form = arg.form;
			}
			else {
				$log.error("Trying to show window with name: '" + name + "' which is not created.");
			}
		},
		hide:function(name){
			var instance = instances[name];
			if (instance) {
				instance.hide();
			}else {
				$log.error("Trying to hide window : '" + name + "' which is not created.");
			}
		},
		dismiss: function(name) {
			var instance = instances[name];
			if (instance) {
				instance.hide();
				delete instances[name];
			}else{
				$log.error("Trying to destroy window : '" + name + "' which is not created.");
			}
		},
		switchForm: function(name,mainForm,navigatorForm) {		
        	$rootScope.$apply(function() { // TODO treat multiple windows case
        		if($solutionSettings.windowName == name) { // main window form switch
        			$solutionSettings.mainForm = mainForm;
        			$solutionSettings.navigatorForm = navigatorForm;
        		}
    		})
		},
		setTitle: function(name,title) {
        	$rootScope.$apply(function() {
				if(instances[name] && instances[name].type!= 2){
					instances[name].title =title;
	    		}else{
	    			$solutionSettings.solutionTitle = title;
	    		}
			});
		},
		setInitialBounds:function(name,initialBounds){
			if(instances[name]){
				instances[name].initialBounds = initialBounds;
			}			
		},
		setStoreBounds:function(name,storeBounds){
			if(instances[name]){
				instances[name].storeBounds = storeBounds;
			}
		},
		resetBounds:function(name){
			if(instances[name]){
				instances[name].storeBounds = false;				
			}
		},
		setLocation:function(name,location){
			if(instances[name]){
				instances[name].setLocation(location);				
			}
		},		
		setSize:function(name,size){
			if(instances[name]){
				instances[name].setSize(size);				
			}
		},	
		setUndecorated:function(name,undecorated){
			if(instances[name]){
				instances[name].undecorated = undecorated;				
			}
		},
		setOpacity:function(name,opacity){
			if(instances[name]){
				instances[name].opacity = opacity;				
			}
		},
		setResizable:function(name,resizable){
			if(instances[name]){
				instances[name].resizable = resizable;				
			}
		},
		setTransparent:function(name,transparent){
			if(instances[name]){
				instances[name].transparent = transparent;				
			}
		},
		toFront:function(name){
			if(instances[name]){
				//TODO tofront				
			}
		},
		toBack:function(name){
			if(instances[name]){
				//TODO toback				
			}
		},
		reload: function() {
			$rootScope.$apply(function() {
        		$window.location.reload(true);
    		})
		},
		updateController: function(formName,controllerCode, realFormUrl, forceLoad) {
			$rootScope.$apply(function() {
				$servoyInternal.clearformState(formName)
				eval(controllerCode);
				formTemplateUrls[formName] = realFormUrl;
				if(forceLoad) $rootScope.updatingFormUrl = realFormUrl;
			});
		},
 		getFormUrl: function(formName) {
			var realFormUrl = formTemplateUrls[formName];
			if (realFormUrl == null) {
					$servoyInternal.callService("$windowService", "touchForm", {name:formName});
			}
			return realFormUrl;
		},
	}
	
}).controller("DialogInstanceCtrl", function ($scope, windowInstance,$windowService, $servoyInternal) {
	//$scope.title = title;
	//$scope.windowName = windowName;
	//$scope.formSize =formSize;
	$scope.win =  windowInstance
	$scope.getFormUrl = function() {
		return $windowService.getFormUrl(windowInstance.form)
	}
	$scope.isUndecorated = function(){
		return $scope.win.undecorated || ($scope.win.opacity<0.99)
	}
	$scope.getFormSize = function(){
		return {'width':'200px'}
	}
	$servoyInternal.setFormVisibility(windowInstance.form,true);
	
	$scope.cancel = function () {
		var promise = $servoyInternal.callService("$windowService", "windowClosing", {window:windowInstance.name});
		promise.then(function(ok) {
    		if (ok) {
    			$windowService.hide(windowInstance.name);
    		}
    	})
	};
});