angular.module('servoyWindowManager',[])	// TODO Refactor so that window is a component with handlers
.factory('$servoyWindowManager', ['$timeout', '$rootScope','$http','$q','$templateCache','$injector','$controller','$compile','WindowType',
                           function($timeout, $rootScope,$http,$q ,$templateCache,$injector,$controller,$compile,WindowType) {
	var WM = new WindowManager();
	var winInstances = {}
	return {
		BSWindowManager: WM,
		instances: winInstances,
		open : function (windowOptions) {
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
	           var isModal = (ctrlLocals.windowInstance.type == WindowType.MODAL_DIALOG);
	           
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
	           //-1 means default size and location(center)
	           if(!location || (location.x <0 && location.y <0)) location=centerWindow(windowInstance.form.size)
	           if(!size || size.width<0 || size.height<0) size =null;
	           
	           //convert servoy x,y to library top , left
	           var loc = {left:location.x,top:location.y}

	           var compiledWin = $compile( tplAndVars[0])(windowScope);
	        //create the bs window instance
	        	var win = WM.createWindow({
	        		id:windowInstance.name,
	        		fromElement: compiledWin,
	                title: "Loading...",
	                resizable:!!windowInstance.resizable,
	                location:loc,
	                size:size,
		            isModal:isModal 
	            })
	            
	        	//set servoy managed bootstrap-window Instance
	        	windowInstance.bsWindowInstance =win;
	          },function resolveError(reason) {
	        	  	dialogOpenedDeferred.reject(reason);
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
    
}]).factory("$windowService", function($servoyWindowManager, $log, $rootScope, $solutionSettings,$solutionSettings, $window, $timeout, $servoyInternal,webStorage,WindowType) {
	var instances = $servoyWindowManager.instances;
	var formTemplateUrls = {};
	var storage = webStorage.local;
	var sol = $solutionSettings.solutionName+'.'
	
	// track main app window size change
	var mwResizeTimeoutID;
	$window.addEventListener('resize',function() { 
		if(mwResizeTimeoutID) $timeout.cancel(mwResizeTimeoutID);
		mwResizeTimeoutID = $timeout( function() {
			$servoyInternal.callService("$windowService", "resize", {size:{width:$window.innerWidth,height:$window.innerHeight}},true);
		}, 500);
	});
	
	return {
		create: function (name,type){
			// dispose old one
			if(instances[name]){
				
			}
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
				    	 if(this.storeBounds) storage.add(sol+name+'.storedBounds.location',location)
				     },
				     setSize:function(size){
				    	 this.size = size;
				    	 if(win.bsWindowInstance){
				    		 win.bsWindowInstance.setSize(size);				    		 
				    	 }				    	 
				    	 if(this.storeBounds) storage.add(sol+name+'.storedBounds.size',size)
				     },
				     onResize:function($event,size){
				    	win.size = size;				    	
				    	if(win.storeBounds) storage.add(sol+name+'.storedBounds.size',size)
				    	$servoyInternal.callService("$windowService", "resize", {name:win.name,size:win.size},true);
				     },
				     onMove:function($event,location){
				    	 win.location = {x:location.left,y:location.top};
				    	 if(win.storeBounds) storage.add(sol+name+'.storedBounds.location',win.location)
				    	 $servoyInternal.callService("$windowService", "move", {name:win.name,location:win.location},true);
				     },
				     toFront:function(){
				    	 $servoyWindowManager.BSWindowManager.setFocused(this.bsWindowInstance)
				     },
				     toBack:function(){
				    	 $servoyWindowManager.BSWindowManager.sendToBack(this.bsWindowInstance)
				     },
				     clearBounds: function(){
				    	 storage.remove(sol+name+'.storedBounds.location')
				    	 storage.remove(sol+name+'.storedBounds.size')
				     }
					};
				
				instances[name] = win;
				return win;
			}
			
		},
		show: function(name,form, title) {	
			var instance = instances[name];
			if (instance) {
				if(instance.bsWindowInstance){
					// do nothing switchform will switch the form.
					return;
				} 
				instance.title = title;
				if(instance.storeBounds){
					instance.size = storage.get(sol+name+'.storedBounds.size')
					instance.location =  storage.get(sol+name+'.storedBounds.location')					
				}
				$servoyWindowManager.open({
					templateUrl: "templates/dialog.html",
					controller: "DialogInstanceCtrl",
					windowClass: "tester",
					windowInstance:instance
				}).then(function(){
					instance.bsWindowInstance.$el.on('bswin.resize',instance.onResize)
					instance.bsWindowInstance.$el.on('bswin.move',instance.onMove)
				},function(reason){
					throw reason;
				})
				if(instance.form.name != form) throw 'switchform should set the instances state before showing it'
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
		destroy: function(name) {
			var instance = instances[name];
			if (instance) {
				delete instances[name];
			}else{
				$log.error("Trying to destroy window : '" + name + "' which is not created.");
			}
		},
		switchForm: function(name,form,navigatorForm) {		
        		if(instances[name] && instances[name].type != WindowType.WINDOW){
        			instances[name].form = form;
        			instances[name].navigatorForm = navigatorForm;    			
        		}
        		else if($solutionSettings.windowName == name) { // main window form switch
        			$solutionSettings.mainForm = form;
        			$solutionSettings.navigatorForm = navigatorForm;
        		}
        		if (!$rootScope.$$phase) $rootScope.$digest();
		},
		setTitle: function(name,title) {
				if(instances[name] && instances[name].type!= WindowType.WINDOW){
					instances[name].title =title;
	    		}else{
	    			$solutionSettings.solutionTitle = title;
	    			if (!$rootScope.$$phase) $rootScope.$digest();
	    		}
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
				instances[name].clearBounds()
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
				instances[name].toFront();				
			}
		},
		toBack:function(name){
			if(instances[name]){
				instances[name].toBack();				
			}
		},
		reload: function() {
        		$window.location.reload(true);
		},
		updateController: function(formName,controllerCode, realFormUrl, forceLoad) {
				$servoyInternal.clearformState(formName)
				eval(controllerCode);
				formTemplateUrls[formName] = realFormUrl;
				if(forceLoad) $rootScope.updatingFormUrl = realFormUrl;
				if (!$rootScope.$$phase) $rootScope.$digest();
		},
		touchForm: function(formName) {
			var realFormUrl = formTemplateUrls[formName];
			if (realFormUrl == null) {
					formTemplateUrls[formName] = "";
					if (!$rootScope.$$phase) $rootScope.$digest();
			}
		},
 		getFormUrl: function(formName) {
			var realFormUrl = formTemplateUrls[formName];
			if (realFormUrl == null) {
					formTemplateUrls[formName] = "";
					$servoyInternal.callService("$windowService", "touchForm", {name:formName},true);
			}
			else if (realFormUrl.length == 0)
			{
				// waiting for updateForm to come
				return null;
			}
			return realFormUrl;
		}
	}
	
}).value('WindowType',{
	DIALOG:0,
	MODAL_DIALOG:1,
	WINDOW:2
}).controller("DialogInstanceCtrl", function ($scope, windowInstance,$windowService, $servoyInternal) {

	// these scope variables can be accessed by child scopes
	// for example the default navigator watches 'win' to see if it changed the current form
	$scope.win =  windowInstance
	$scope.getFormUrl = function() {
		return $windowService.getFormUrl(windowInstance.form.name)
	}
	$scope.getNavigatorFormUrl = function() {
		if (windowInstance.navigatorForm.templateURL && windowInstance.navigatorForm.templateURL.lastIndexOf("default_navigator_container.html") == -1) {
			return $windowService.getFormUrl(windowInstance.navigatorForm.templateURL);
		}
		return windowInstance.navigatorForm.templateURL;
	}
	
	$scope.isUndecorated = function(){
		return $scope.win.undecorated || ($scope.win.opacity<1)
	}
	
	$scope.getBodySize = function(){
		var win = $scope.win;
		var width = win.size ? win.size.width:win.form.size.width;
		var height = win.form.size.height;
		if(!win.size && win.navigatorForm.size){
			width += win.navigatorForm.size.width;
		}
		return {'width':width+'px','height':height+'px'}
	}
	$servoyInternal.callService('formService', 'formvisibility', {formname:windowInstance.form.name,visible:true})
	
	$scope.cancel = function () {
		var promise = $servoyInternal.callService("$windowService", "windowClosing", {window:windowInstance.name},false);
		promise.then(function(ok) {
    		if (ok) {
    			$windowService.hide(windowInstance.name);
    		}
    	})
	};
});