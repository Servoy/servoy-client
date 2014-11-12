var controllerProvider;
angular.module('servoyApp', ['sabloApp', 'servoy','webStorageModule','servoy-components', 'webSocketModule','servoyWindowManager','pasvaz.bindonce']).config(function($controllerProvider) {
	controllerProvider = $controllerProvider;
}).factory('$servoyInternal', function ($rootScope, webStorage, $anchorConstants, $q, $solutionSettings, $window, $sessionService, $sabloConverters, $sabloUtils, $sabloApplication) {
	   
	   var deferredProperties = {};
	   
	   var getComponentChanges = function(now, prev, beanConversionInfo, beanLayout, parentSize, changeNotifier, componentScope) {
		
		 var changes = $sabloApplication.getComponentChanges(now, prev, beanConversionInfo, parentSize, changeNotifier, componentScope)
		 if (changes.location || changes.size || changes.visible || changes.anchors) {
			   if (beanLayout) {
				   applyBeanData(now, beanLayout, changes, parentSize, changeNotifier, undefined, undefined, componentScope);
			   }
		   }
		 return changes
	   };
	   
	   var sendChanges = function(now, prev, formname, beanname) {
		   $sabloApplication.getFormState(formname).then(function (formState) {
			   var changes = getComponentChanges(now, prev, $sabloUtils.getInDepthProperty($sabloApplication.getFormStatesConversionInfo(), formname, beanname),
					   formState.layout[beanname], formState.properties.designSize, $sabloApplication.getChangeNotifier(formname, beanname), formState.getScope());
			   if (Object.getOwnPropertyNames(changes).length > 0) {
				   $sabloApplication.callService('formService', 'dataPush', {formname:formname,beanname:beanname,changes:changes}, true)
			   }
		   })
	   };
	   
	   var applyBeanData = function(beanModel, beanLayout, beanData, containerSize, changeNotifier, beanConversionInfo, newConversionInfo, componentScope) {
		   
		   $sabloApplication.applyBeanData(beanModel, beanData, containerSize, changeNotifier, beanConversionInfo, newConversionInfo, componentScope)
		   applyBeanLayout(beanModel, beanLayout, beanData, containerSize)
	   }
	   
	   var applyBeanLayout = function(beanModel, beanLayout, beanData, containerSize) {
		   
		   //beanData.anchors means anchors changed or must be initialized
		   if (beanData.anchors && containerSize && $solutionSettings.enableAnchoring) {
			   var anchoredTop = (beanModel.anchors & $anchorConstants.NORTH) != 0; // north
			   var anchoredRight = (beanModel.anchors & $anchorConstants.EAST) != 0; // east
			   var anchoredBottom = (beanModel.anchors & $anchorConstants.SOUTH) != 0; // south
			   var anchoredLeft = (beanModel.anchors & $anchorConstants.WEST) != 0; //west
			   
			   var runtimeChanges = beanData.size != undefined || beanData.location != undefined;
			   
			   if (!anchoredLeft && !anchoredRight) anchoredLeft = true;
			   if (!anchoredTop && !anchoredBottom) anchoredTop = true;
			   
			   if (anchoredTop)
			   {
				   if (beanLayout.top == undefined || runtimeChanges && beanModel.location != undefined) beanLayout.top = beanModel.location.y + 'px';
			   }
			   else delete beanLayout.top;
			   
			   if (anchoredBottom)
			   {
				   if (beanLayout.bottom == undefined) {
					   beanLayout.bottom = (beanModel.partHeight ? beanModel.partHeight : containerSize.height) - beanModel.location.y - beanModel.size.height;
					   if(beanModel.offsetY) {
						   beanLayout.bottom = beanLayout.bottom - beanModel.offsetY;
					   }
					   beanLayout.bottom = beanLayout.bottom + "px";
				   }
			   }
			   else delete beanLayout.bottom;
			   
			   if (!anchoredTop || !anchoredBottom) beanLayout.height = beanModel.size.height + 'px';
			   else delete beanLayout.height;
			   
			   if (anchoredLeft)
			   {
				   if ( $solutionSettings.ltrOrientation)
				   {
					   if (beanLayout.left == undefined || runtimeChanges && beanModel.location != undefined)
					   {	
						   beanLayout.left =  beanModel.location.x + 'px';
					   }
				   }
				   else
				   {
					   if (beanLayout.right == undefined || runtimeChanges && beanModel.location != undefined)
					   {	
						   beanLayout.right =  beanModel.location.x + 'px';
					   }
				   }
			   }
			   else if ( $solutionSettings.ltrOrientation)
			   {
				   delete beanLayout.left;
			   }
			   else
			   {
				   delete beanLayout.right;
			   }
			   
			   if (anchoredRight)
			   {
				   if ( $solutionSettings.ltrOrientation)
				   {
					   if (beanLayout.right == undefined) beanLayout.right = (containerSize.width - beanModel.location.x - beanModel.size.width) + "px";
				   }
				   else
				   {
					   if (beanLayout.left == undefined) beanLayout.left = (containerSize.width - beanModel.location.x - beanModel.size.width) + "px";
				   }
			   }
			   else if ( $solutionSettings.ltrOrientation)
			   {
				   delete beanLayout.right;
			   }
			   else
			   {
				   delete beanLayout.left;
			   }
			   
			   if (!anchoredLeft || !anchoredRight) beanLayout.width = beanModel.size.width + 'px';
			   else delete beanLayout.width;
		   }
		   
		   //we set the following properties iff the bean doesn't have anchors
		   if (!beanModel.anchors || !$solutionSettings.enableAnchoring)
		   {
			   if (beanModel.location)
			   {
				   if ( $solutionSettings.ltrOrientation)
				   {
					   beanLayout.left = beanModel.location.x+'px';
				   }
				   else
				   {
					   beanLayout.right = beanModel.location.x+'px';
				   }
				   beanLayout.top = beanModel.location.y+'px';
			   }
			   
			   if (beanModel.size)
			   {
				   beanLayout.width = beanModel.size.width+'px';
				   beanLayout.height = beanModel.size.height+'px';
			   }
		   }
		   
		   if (beanModel.visible != undefined)
		   {
			   if (beanModel.visible == false)
			   {
				   beanLayout.display = 'none';
			   }
			   else
			   {
				   delete beanLayout.display;
			   }
		   }
	   }
		   
	   function connect() {
		   // maybe do this with defer ($q)
		   var solName = decodeURIComponent((new RegExp('[?|&]s=' + '([^&;]+?)(&|#|;|$)').exec($window.location.search)||[,""])[1].replace(/\+/g, '%20'))||null
		   if (!solName) $solutionSettings.solutionName  = /.*\/(\w+)\/.*/.exec($window.location.pathname)[1];
		   else $solutionSettings.solutionName  = solName;
		   $solutionSettings.windowName = webStorage.session.get("windowid");
		   var wsSession = $sabloApplication.connect('/solutions/'+$solutionSettings.solutionName, [webStorage.session.get("sessionid"), $solutionSettings.windowName, $solutionSettings.solutionName])
		   wsSession.onMessageObject(function (msg, conversionInfo) {
			   // data got back from the server
			   for(var formname in msg.forms) {
				   // current model
				   if (!$sabloApplication.hasFormstateLoaded(formname)) continue;
				   // if the formState is on the server but not here anymore, skip it. 
				   // this can happen with a refresh on the browser.
				   $sabloApplication.getFormState(formname).then(getFormMessageHandler(formname, msg, conversionInfo));
			   }
			   
			   function getFormMessageHandler(formname, msg, conversionInfo) {
				   return function (formState) {
						   var formModel = formState.model;
						   var layout = formState.layout;
						   var newFormData = msg.forms[formname];
	
						   for (var beanname in newFormData) {
							   // copy over the changes, skip for form properties (beanname empty)
							   if (beanname != '') {
								   if (formModel[beanname]!= undefined && (newFormData[beanname].size != undefined ||  newFormData[beanname].location != undefined)) {	
									   //size or location were changed at runtime, we need to update components with anchors
									   newFormData[beanname].anchors = formModel[beanname].anchors;
								   }
	
								   applyBeanLayout(formModel[beanname], layout[beanname], newFormData[beanname], formState.properties.designSize)
								   
								   for (var defProperty in deferredProperties) {
									   for(var key in newFormData[beanname]) {
										   if (defProperty == (formname + "_" + beanname + "_" + key)) {
											   deferredProperties[defProperty].resolve(newFormData[beanname][key]);
											   delete deferredProperties[defProperty];
										   }
									   }
								   } 
							   }
						   }
			   		}
			   }
			   
			   if (msg.sessionid) {
				   webStorage.session.add("sessionid",msg.sessionid);
			   }
			   if (msg.windowid) {
				   $solutionSettings.windowName = msg.windowid;
				   webStorage.session.add("windowid",msg.windowid);
			   }
		   });
		   
		   wsSession.onopen(function(evt) {
			   // update the main app window with the right size
			   wsSession.callService("$windowService", "resize", {size:{width:$window.innerWidth,height:$window.innerHeight}},true);  
		   });
	   }
	   
	   return {
		   connect: connect,
		   // used by custom property component[] to implement nested component logic
		   applyBeanData: applyBeanData,
		   getComponentChanges: getComponentChanges,
		   
		   initFormState: function(formName, beanDatas, formProperties, formScope) {
			   
			   var state = $sabloApplication.initFormState(formName, beanDatas, formProperties, formScope)
			   
			   if (!state || state.layout) return state; // already initialized
			   
			   var layout = state.layout = {};
			   state.style = {
					   left: "0px",
					   top: "0px",
					   minWidth : formProperties.size.width + "px",
					   minHeight : formProperties.size.height + "px",
					   right: "0px",
					   bottom: "0px",
					   border: formProperties.border};
			   state.properties = formProperties;

			   for (var beanName in beanDatas) {
				   // initialize with design data
				   layout[beanName] = { position: 'absolute' }
				   
				   var newBeanConversionInfo = beanDatas[beanName].conversions;
				   var beanConversionInfo = newBeanConversionInfo ? $sabloUtils.getOrCreateInDepthProperty($sabloApplication.getFormStatesConversionInfo(), formName, beanName) : undefined; // we could do a get instead of undefined, but normally that value is not needed if the new conversion info is undefined
				   
				   applyBeanData(state.model[beanName], layout[beanName], beanDatas[beanName], formProperties.designSize, $sabloApplication.getChangeNotifier(formName, beanName), beanConversionInfo, newBeanConversionInfo, formScope)
			   }

			   return state;
		   },

		   // used by form template js
		   sendChanges: sendChanges,

		   // for example components that use nested elements/components such as portal can give here the new value
		   // based on the way they feed the model to child components - so they can use other objects then server known models
		   pushDPChange: function(formname, beanname, property, componentModel, rowId) {
			   $sabloApplication.getFormState(formname).then(function (formState) {
				   var changes = {}

				   if (componentModel) {
					   // probably a nested component (inside another component); the component might even be linked to a different foundset
					   //changes[property] = $sabloUtils.convertClientObject(componentModel[property]);
					   if (rowId){
						   changes.rowId = rowId;
					   } else if (componentModel.rowId) {
						   changes.rowId = componentModel.rowId;
					   }
				   }
				   // default model, simple direct form child component
				   var formStatesConversionInfo = $sabloApplication.getFormStatesConversionInfo()
				   var conversionInfo = (formStatesConversionInfo[formname] ? formStatesConversionInfo[formname][beanname] : undefined);

				   if (conversionInfo && conversionInfo[property]) {
					   changes[property] = $sabloConverters.convertFromClientToServer(formState.model[beanname][property], conversionInfo[property], undefined);
				   } else {
					   changes[property] = $sabloUtils.convertClientObject(formState.model[beanname][property]);
				   }
				   $sabloApplication.callService('formService', 'svyPush', {formname:formname,beanname:beanname,property:property,changes:changes}, true)
			   });
		   },

		   setFindMode: function(formName, findMode, editable){
			   $sabloApplication.getFormState(formName).then(function (formState) {
				   for (beanName in formState.model)
				   {
				   		if (beanName != '') 
				   		{
				   			if (formState.api[beanName] && formState.api[beanName].setFindMode)
				   			{
				   				formState.api[beanName].setFindMode(findMode, editable);
				   			}
				   			else
				   			{
				   				if (findMode)
							    {
								   formState.model[beanName].svy_readOnlyBeforeFindMode = formState.model[beanName].readOnly;
								   formState.model[beanName].readOnly = true;
							    }
							    else
							    {
								   formState.model[beanName].readOnly = formState.model[beanName].svy_readOnlyBeforeFindMode;
							    }
				   			}
							formState.getScope().$apply();
				   		}
				   }
			   });
		   }
	   }
}).directive('svyAutosave',  function ($sabloApplication) {
    return {
        restrict: 'A',
        link: function (scope, element, attrs) {
        	element.on('click', function(event) {
        		if (event.target.tagName.toLowerCase() == 'div')
        		{
        			$sabloApplication.callService("applicationServerService", "autosave",{}, true);
        		}
        	});
        }
      };
}).directive('svyImagemediaid',  function ($parse,$timeout) {
    return {
        restrict: 'A',
        link: function (scope, element, attrs) {     
        	
        	var rollOverImgStyle = null; 
        	var imgStyle = null;
        	var clearStyle ={ width:'0px',
        					  height:'0px',
        					  backgroundImage:''}
        	
        	scope.$watch(attrs.svyImagemediaid,function(newVal){
        		if (newVal.visible)
        		{
        			// the value from model may be incorrect so take value from ui
            		var setImageStyle = function(){
            			var componentSize = {width: element[0].parentNode.parentNode.offsetWidth,height: element[0].parentNode.parentNode.offsetHeight};
                		var image = null;
                 		  var mediaOptions = scope.$eval('model.mediaOptions');
                		if(newVal.rollOverImg){ 
                		  rollOverImgStyle= parseImageOptions( newVal.rollOverImg, mediaOptions, componentSize);
                		}else {
                		  rollOverImgStyle = null
                		}
                		if(newVal.img){
                		  imgStyle =parseImageOptions( newVal.img, mediaOptions, componentSize)
                  		  element.css(imgStyle)
                		}else {
                		  imgStyle = null;
                		} 	
            		}
            		angular.element(element[0]).ready(setImageStyle);
//            		if (element[0].parentNode.parentNode.offsetWidth >0 && element[0].parentNode.parentNode.offsetHeight >0)
//            		{
//            			//dom is ready
//            			setImageStyle();
//            		}
//            		else
//            		{
//            			$timeout(setImageStyle,200);
//            		}
        		}
        	}, true)
        	
        	
       	function parseImageOptions(image,mediaOptions ,componentSize){
        	  var bgstyle = {};
        	  bgstyle['background-image'] = "url('" + image + "')"; 
       		  bgstyle['background-repeat'] = "no-repeat";
       		  bgstyle['background-position'] = "left";
       		  bgstyle['display'] = "inline-block";
       		  bgstyle['vertical-align'] = "middle"; 
       		  if(mediaOptions == undefined) mediaOptions = 14; // reduce-enlarge & keep aspect ration
       		  var mediaKeepAspectRatio = mediaOptions == 0 || ((mediaOptions & 8) == 8);

       		  // default  img size values
       		  var imgWidth = 16;
       		  var imgHeight = 16;
       		  
       		  if (image.indexOf('imageWidth=') > 0 && image.indexOf('imageHeight=') > 0)
       		  {
       			  var vars = {};
       			  var parts = image.replace(/[?&]+([^=&]+)=([^&]*)/gi,    
       					  function(m,key,value) {
       				  vars[key] = value;
       			  });
       			  imgWidth = vars['imageWidth'];
       			  imgHeight = vars['imageHeight'];
       		  }
       		  
       		  var widthChange = imgWidth / componentSize.width;
       		  var heightChange = imgHeight / componentSize.height;
       		  
       		  if (widthChange > 1.01 || heightChange > 1.01 || widthChange < 0.99 || heightChange < 0.99) // resize needed
       		  {
 						if ((mediaOptions & 6) == 6) // reduce-enlarge
	    				{
	    					if (mediaKeepAspectRatio)
	    					{
	    						if (widthChange > heightChange)
	    						{
	    							imgWidth = imgWidth / widthChange;
	    							imgHeight = imgHeight / widthChange;
	    						}
	    						else
	    						{
	    							imgWidth = imgWidth / heightChange;
	    							imgHeight = imgHeight / heightChange;
	    						}
	    					}
	    					else
	    					{
	    						imgWidth = componentSize.width;
	    						imgHeight = componentSize.height;
	    					}
	    				}        			  
	  					else if ((mediaOptions & 2) == 2) // reduce
   					{
   						if (widthChange > 1.01 && heightChange > 1.01)
   						{
   							if (mediaKeepAspectRatio)
   							{
   								if (widthChange > heightChange)
   								{
   									imgWidth = imgWidth / widthChange;
   									imgHeight = imgHeight / widthChange;
   								}
   								else
   								{
   									imgWidth = imgWidth / heightChange;
   									imgHeight = imgHeight / heightChange;
   								}
   							}
   							else
   							{
	    						imgWidth = componentSize.width;
	    						imgHeight = componentSize.height;
   							}
   						}
   						else if (widthChange > 1.01)
   						{
   							imgWidth = imgWidth / widthChange;
   							if (mediaKeepAspectRatio)
   							{
   								imgHeight = imgHeight / widthChange;
   							}
   							else
   							{
	    						imgHeight = componentSize.height;
   							}
   						}
   						else if (heightChange > 1.01)
   						{
   							imgHeight = imgHeight / heightChange;
   							if (mediaKeepAspectRatio)
   							{
   								imgWidth = imgWidth / heightChange;
   							}
   							else
   							{
	    						imgWidth = componentSize.width;
   							}
   						}
   					}
   					else if ((mediaOptions & 4) == 4) // enlarge
   					{
   						if (widthChange < 0.99 && heightChange < 0.99)
   						{
   							if (mediaKeepAspectRatio)
   							{
   								if (widthChange > heightChange)
   								{
   									imgWidth = imgWidth / widthChange;
   									imgHeight = imgHeight / widthChange;
   								}
   								else
   								{
   									imgWidth = imgWidth / heightChange;
   									imgHeight = imgHeight / heightChange;
   								}
   							}
   							else
   							{
	    						imgWidth = componentSize.width;
	    						imgHeight = componentSize.height;
   							}
   						}
   						else if (widthChange < 0.99)
   						{
   							imgWidth = imgWidth / widthChange;
   							if (mediaKeepAspectRatio)
   							{
   								imgHeight = imgHeight / widthChange;
   							}
   							else
   							{
	    						imgHeight = componentSize.height;
   							}
   						}
   						else if (heightChange < 0.99)
   						{
   							imgHeight = imgHeight / heightChange;
   							if (mediaKeepAspectRatio)
   							{
   								imgWidth = imgWidth / heightChange;
   							}
   							else
   							{
	    						imgWidth = componentSize.width;
   							}
   						}
   					}
       		  }	  
       		  
       		  bgstyle['background-size'] = mediaKeepAspectRatio ? "contain" : "100% 100%";
   			  bgstyle['width'] = Math.round(imgWidth) + "px";
       		  bgstyle['height'] = Math.round(imgHeight) + "px";
        		        		
       		  return bgstyle;
        	}
        	//get component root node
        	var componentRoot =null;
        	componentRoot= element;
			while(!componentRoot.isolateScope() && componentRoot.size() > 0){
				componentRoot = componentRoot.parent();
        	}
        	componentRoot.hover(function(){
        		//over
        		if(rollOverImgStyle){
        			element.css(rollOverImgStyle)
        		}
        	},function(){
        		//out
        		if(imgStyle){
        			element.css(imgStyle)
        		}else{
        			element.css(clearStyle)
        		}        		
        	})
         }
    }
})
.directive('svyLayoutUpdate', function($window,$timeout) {
    return {
      restrict: 'A', // only activate on element attribute
      controller: function($scope, $element, $attrs) {
    	  var compModel;
    	  if($attrs['svyLayoutUpdate'].length == 0) {
    		  compModel = $scope.formProperties;
    	  } else {
    		  //compModel = $scope.model[$attrs['svyLayoutUpdate']];
    	  }
    	  if (!compModel) return; // not found, maybe a missing bean

    	  if(($attrs['svyLayoutUpdate'].length == 0) || compModel.anchors) {
        	  var resizeTimeoutID = null;
        	  $window.addEventListener('resize',function() { 
        		  if(resizeTimeoutID) $timeout.cancel(resizeTimeoutID);
        		  resizeTimeoutID = $timeout( function() {
        			  if(compModel.visible) {
	        			  if(compModel.location) {
	        				  compModel.location.x = $element.offset().left;
	        				  compModel.location.y = $element.offset().top;
	        			  }
	        			  if(compModel.size) {
	            			  compModel.size.width = $element.width();
	            			  compModel.size.height = $element.height();  
	        			  }
        			  }
        		  }, 1000);
        	  });
    	  }
      }
    };   
}).directive('svyFormload',  function ($timeout, $sabloApplication, $windowService, $rootScope) {
    return {
        restrict: 'A',
        link: function (scope, element, attrs) {
        	var formname = scope.formname;
			$timeout(function() {
				// notify that the form has been loaded
				// NOTE: this call cannot be make as a service call, as a service call may
				// already be blocked and waiting for the formload event
				$sabloApplication.sendRequest({cmd:'formloaded',formname:formname})
				if($windowService.getFormUrl(formname) == $rootScope.updatingFormUrl) {
					$rootScope.updatingFormUrl = '';
				}
				scope.formProperties.size.width = element.prop('offsetWidth');
				scope.formProperties.size.height = element.prop('offsetHeight');
			},0);
        }
      }
}).value("$solutionSettings",  {
	mainForm: {},
	navigatorForm: {width:0},
	solutionTitle: "",
	defaultNavigatorState: {max:0,currentIdx:0,form:'<none>'},
	styleSheetPath: undefined,
	ltrOrientation : true,
	enableAnchoring: true
}).controller("MainController", function($scope, $solutionSettings, $servoyInternal, $windowService,$rootScope,webStorage) {
	$servoyInternal.connect();
	$scope.solutionSettings = $solutionSettings;
	$scope.getMainFormUrl = function() {
		return $solutionSettings.mainForm.templateURL?$windowService.getFormUrl($solutionSettings.mainForm.templateURL):"";
	}
	$scope.getNavigatorFormUrl = function() {
		if ( $solutionSettings.navigatorForm.templateURL && $solutionSettings.navigatorForm.templateURL.lastIndexOf("default_navigator_container.html") == -1) {
			return $windowService.getFormUrl($solutionSettings.navigatorForm.templateURL);
		}
		return $solutionSettings.navigatorForm.templateURL;
	}

	$scope.getSessionProblemView = function(){
		if($solutionSettings.noLicense) return $solutionSettings.noLicense.viewUrl;
		if($solutionSettings.maintenanceMode) return $solutionSettings.maintenanceMode.viewUrl;
		if($solutionSettings.sessionExpired) return $solutionSettings.sessionExpired.viewUrl;
		if($solutionSettings.internalServerError) return $solutionSettings.internalServerError.viewUrl;		
		return null;
	}
	
	$scope.getNavigatorStyle = function(ltrOrientation) {
		var orientationVar = ltrOrientation ? 'left':'right';
		var style = {'position':'absolute','top':'0px','bottom':'0px','width':$solutionSettings.navigatorForm.size.width+'px'}
		style[orientationVar] = '0px';
		return style;
	}
	$scope.getFormStyle = function(ltrOrientation) {
		var orientationVar1 = ltrOrientation ? 'right':'left';
		var orientationVar2 = ltrOrientation ? 'left':'right';
		var style = {'position':'absolute','top':'0px','bottom':'0px'}
		style[orientationVar1] = '0px';
		style[orientationVar2] = $solutionSettings.navigatorForm.size.width+'px';
		return style;
	}
}).controller("NoLicenseController",['$scope','$solutionSettings','$timeout','$window' ,function($scope, $solutionSettings,$timeout,$window) {
	
	$scope.redirectUrl = $solutionSettings.noLicense.redirectUrl;
	
	if($solutionSettings.noLicense.redirectTimeout >=0){
		$timeout(function(){			
			$window.location = $solutionSettings.noLicense.redirectUrl;
		},$solutionSettings.noLicense.redirectTimeout*1000)
	}
}]).controller("SessionExpiredController",['$scope','$solutionSettings',function($scope, $solutionSettings) {
	
	$scope.redirectUrl = $solutionSettings.sessionExpired.redirectUrl;

}])
.controller("InternalServerErrorController",['$scope','$solutionSettings',function($scope, $solutionSettings) {
	
	$scope.error = $solutionSettings.internalServerError

}])
.controller("MaintenanceModeController",['$scope','$solutionSettings','$timeout','$window' ,function($scope, $solutionSettings,$timeout,$window) {
	
	$scope.redirectUrl = $solutionSettings.maintenanceMode.redirectUrl;
	
	if($solutionSettings.maintenanceMode.redirectTimeout >=0){
		$timeout(function(){			
			$window.location = $solutionSettings.maintenanceMode.redirectUrl;
		},$solutionSettings.maintenanceMode.redirectTimeout*1000)
	}
}])
.controller("LoginController", function($scope, $modalInstance, $sabloApplication, $rootScope, webStorage) {
	$scope.model = {'remember' : true };
	$scope.doLogin = function() {
		var promise = $sabloApplication.callService("applicationServerService", "login", {'username' : $scope.model.username, 'password' : $scope.model.password, 'remember': $scope.model.remember}, false);
		promise.then(function(ok) {
			if(ok) {
				if(ok.username) webStorage.local.add('servoy_username', ok.username);
				if(ok.password) webStorage.local.add('servoy_password', ok.password);
				$modalInstance.close(ok);
			} else {
				$scope.model.message = 'Invalid username or password, try again';
			}
    	})
	}	
})
.factory('$sessionService',['$solutionSettings','$window','$rootScope',function($solutionSettings,$window,$rootScope){
	
	return {
		expireSession : function (sessionExpired){
			var exp = { 
					viewUrl: 'templates/sessionExpiredView.html',
					redirectUrl : $window.location.href
			}
			if(sessionExpired.viewUrl)	exp.viewUrl= sessionExpired.viewUrl;

			$solutionSettings.sessionExpired = exp;
			if (!$rootScope.$$phase) $rootScope.$digest();
		},
		setNoLicense: function (noLicense){
			var noLic = {
					viewUrl : 'templates/serverTooBusyView.html',
					redirectUrl : $window.location.href,
					redirectTimeout : -1
			}
			if(noLicense.viewUrl) noLic.viewUrl = noLicense.viewUrl 
			if(noLicense.redirectUrl) noLic.redirectUrl = noLicense.redirectUrl;
			if(noLicense.redirectTimeout) noLic.redirectTimeout = noLicense.redirectTimeout;

			$solutionSettings.noLicense = noLic;
			if (!$rootScope.$$phase) $rootScope.$digest();
		},
		setMaintenanceMode: function (maintenanceMode){
			var ment = {
					viewUrl : 'templates/maintenanceView.html',
					redirectUrl : $window.location.href,
					redirectTimeout : 0
			}
			if(maintenanceMode.viewUrl) ment.viewUrl = mentenanceMode.viewUrl 
			if(msg.maintenanceMode.redirectUrl)	ment.redirectUrl = maintenanceMode.redirectUrl;
			if(msg.maintenanceMode.redirectTimeout)	ment.redirectTimeout = maintenanceMode.redirectTimeout;

			$solutionSettings.maintenanceMode = ment;
			if (!$rootScope.$$phase) $rootScope.$digest();
		},
		setInternalServerError: function(internalServerError){
			var error = {viewUrl:'templates/serverInternalErrorView.html'}
			if(internalServerError.viewUrl)  error.viewUrl = internalServerError.viewUrl;
			if(internalServerError.stack) error.stack = internalServerError.stack;

			$solutionSettings.internalServerError = error;		
			if (!$rootScope.$$phase) $rootScope.$digest();
		}
	}
}])
.factory("$applicationService",['$window','$timeout','webStorage','$modal', '$sabloApplication','$solutionSettings','$rootScope', function($window,$timeout,webStorage,$modal,$sabloApplication,$solutionSettings,$rootScope) {
	var showDefaultLoginWindow = function() {
			$modal.open({
        	  templateUrl: '/templates/login.html',
         	  controller: 'LoginController',
       	      windowClass: 'login-window',
      	      backdrop: 'static',
      	      keyboard: false
			});				
		}
	return {
		setStyleSheet: function(path) {
			$solutionSettings.styleSheetPath = path;
			if (!$rootScope.$$phase) $rootScope.$digest();
		},
		getUserProperty: function(key) {
			var json = webStorage.local.get("userProperties");
			if (json) {
				return JSON.parse(json)[key];
			}
			return null;
		},
		setUserProperty: function(key,value) {
			var obj = {}
			var json = webStorage.local.get("userProperties");
			if (json) {
				obj = JSON.parse(json);
			}
			if (value == null) delete obj[key]
			else obj[key] = value;
			webStorage.local.add("userProperties", JSON.stringify(obj))
		},
		getUserPropertyNames: function() {
			var json = webStorage.local.get("userProperties");
			if (json) {
				return Object.getOwnPropertyNames(JSON.parse(json));
			}
			return [];
		},
		showUrl:function(url,target,targetOptions,timeout){
		    	 if(!target) target ='_blank';
		    	 if(!timeout) timeout = 0;	    	 
			    	$timeout(function(){
			    		$window.open(url,target,targetOptions)
			    	},timeout*1000)	    	
		},
		setStatusText:function(text){
			$window.status = text;  	
		},
		getScreenSize:function() {
			  if ($window.screen) {
				  return{width: $window.screen.width, height: $window.screen.height, orientation:$window.orientation};
			  }
			return null;
		},
		getLocation:function() {
			return $window.location.href;
		},
		getUserAgentAndPlatform:function() {
			return {userAgent:$window.navigator.userAgent,platform:$window.navigator.platform};
		},
		getUtcOffsetsAndLocale:function() {
			// TODO this navigator.language is not really the correct language (its the browser installed language not the preferred language thats in the accept header)
			// But chrome also doesn't send that header in the websocket request so we also can't get it from there: https://code.google.com/p/chromium/issues/detail?id=174956
			// a hack could be: http://stackoverflow.com/questions/1043339/javascript-for-detecting-browser-language-preference
			return {locale:$window.navigator.language?$window.navigator.language:$window.navigator.browserLanguage,utcOffset:(new Date(new Date().getFullYear(), 0, 1, 0, 0, 0, 0).getTimezoneOffset() / -60),utcDstOffset:(new Date(new Date().getFullYear(), 6, 1, 0, 0, 0, 0).getTimezoneOffset() / -60)};
		},
		showInfoPanel: function(url,w,h,t,closeText)
		{
			var infoPanel=document.createElement("div");
			infoPanel.innerHTML="<iframe marginheight=0 marginwidth=0 scrolling=no frameborder=0 src='"+url+"' width='100%' height='"+(h-25)+"'></iframe><br><a href='#' onClick='javascript:document.getElementById(\"infoPanel\").style.display=\"none\";return false;'>"+closeText+"</a>";
			infoPanel.style.zIndex="2147483647";  
			infoPanel.id="infoPanel"; 
			var width = window.innerWidth || document.body.offsetWidth; 
			infoPanel.style.position = "absolute"; 
			infoPanel.style.left = ((width - w) - 30) + "px";  
			infoPanel.style.top = "10px"; 
			infoPanel.style.height= h+"px"; 
			infoPanel.style.width= w+"px";
			document.body.appendChild(infoPanel);
			setTimeout('document.getElementById(\"infoPanel\").style.display=\"none\"',t);
		},
		showDefaultLogin: function() {
			if(webStorage.local.get('servoy_username') && webStorage.local.get('servoy_password')) {
				var promise = $sabloApplication.callService("applicationServerService", "login", {'username' : webStorage.local.get('servoy_username'), 'password' : webStorage.local.get('servoy_password'), 'encrypted': true}, false);
				promise.then(function(ok) {
					if(!ok) {
						webStorage.local.remove('servoy_username');
						webStorage.local.remove('servoy_password');
						showDefaultLoginWindow();
					}
		    	})				
			} else {
				showDefaultLoginWindow();
			}		
		}
	}
	
}])
.run(function($window, $sabloApplication) {
	$window.executeInlineScript = function(formname, script, params) {
		$sabloApplication.callService("formService", "executeInlineScript", {'formname' : formname, 'script' : script, 'params' : params},true)
	}
})