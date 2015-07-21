var controllerProvider;
angular.module('servoyApp', ['sabloApp', 'servoy','webStorageModule','servoy-components', 'webSocketModule','servoyWindowManager','pasvaz.bindonce', 'ngSanitize']).config(function($controllerProvider,$logProvider) {
	controllerProvider = $controllerProvider;
	$logProvider.debugEnabled(false);
}).factory('$servoyInternal', function ($rootScope, webStorage, $anchorConstants, $q, $solutionSettings, $window, $sessionService, $sabloConverters, $sabloUtils, $sabloApplication) {

	var latestApplyCall = {};

	var getComponentChanges = function(now, prev, beanConversionInfo, beanLayout, parentSize, changeNotifierGenerator, componentScope, property) {

		var changes = $sabloApplication.getComponentChanges(now, prev, beanConversionInfo, parentSize, changeNotifierGenerator, componentScope, property)
		// TODO: visibility must be based on properties of type visible, not on property name
		if (changes.location || changes.size || changes.visible || changes.anchors) {
			if (beanLayout) {
				applyBeanData(now, beanLayout, changes, parentSize, changeNotifierGenerator, undefined, undefined, componentScope);
			}
		}
		return changes;
	};

	var sendChanges = function(now, prev, formname, beanname, property) {
		$sabloApplication.getFormStateWithData(formname).then(function (formState) {
			var changes = getComponentChanges(now, prev, $sabloUtils.getInDepthProperty($sabloApplication.getFormStatesConversionInfo(), formname, beanname),
					formState.layout[beanname], formState.properties.designSize, $sabloApplication.getChangeNotifierGenerator(formname, beanname), formState.getScope(),property);
			if (Object.getOwnPropertyNames(changes).length > 0) {
				$sabloApplication.callService('formService', 'dataPush', {formname:formname,beanname:beanname,changes:changes}, true)
			}
		})
	};

	var applyBeanData = function(beanModel, beanLayout, beanData, containerSize, changeNotifierGenerator, beanConversionInfo, newConversionInfo, componentScope) {

		$sabloApplication.applyBeanData(beanModel, beanData, containerSize, changeNotifierGenerator, beanConversionInfo, newConversionInfo, componentScope)
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
		var isAnchoredTopLeftBeanModel = !beanModel.anchors || (beanModel.anchors == ($anchorConstants.NORTH + $anchorConstants.WEST));
		if (isAnchoredTopLeftBeanModel || !$solutionSettings.enableAnchoring)
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

		// TODO: visibility must be based on properties of type visible, not on property name
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
		$solutionSettings.windowName = $sabloApplication.getWindowId()
		var wsSession = $sabloApplication.connect('/solutions/'+$solutionSettings.solutionName, [$sabloApplication.getSessionId(), $sabloApplication.getWindowName(), $sabloApplication.getWindowId()], {solution:$solutionSettings.solutionName})
		wsSession.onMessageObject(function (msg, conversionInfo) {
			// data got back from the server
			for(var formname in msg.forms) {
				// current model
				if (!$sabloApplication.hasFormState(formname)) continue;
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
							var beanData = newFormData[beanname];
							var beanModel = formModel[beanname];
							if (beanModel != undefined && (beanData.size != undefined ||  beanData.location != undefined)) {	
								//size or location were changed at runtime, we need to update components with anchors
								beanData.anchors = beanModel.anchors;
							}
							if (latestApplyCall && latestApplyCall.formname == formname && latestApplyCall.beanname == beanname && beanData[latestApplyCall.property] != undefined)
								latestApplyCall = {};
							applyBeanLayout(beanModel, layout[beanname],beanData, formState.properties.designSize)
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

		initFormState: function(formName, beanDatas, formProperties, formScope, resolve) {

			var hasFormState = $sabloApplication.hasFormState(formName);
			
			var state = $sabloApplication.initFormState(formName, beanDatas, formProperties, formScope,resolve);

			if (state) {
				if (!hasFormState) {
					var layout = state.layout = {};
					var formStyle = {
							left: "0px",
							top: "0px",
							right: "0px",
							bottom: "0px"};
					if (formProperties.borderType)
					{
						var borderStyle = formProperties.borderType.borderStyle;
						for (var key in borderStyle)
						{
							formStyle[key] = borderStyle[key];
						}	
					}
					if (formProperties.transparent)
					{
						formStyle['background-color'] = 'transparent';
					}	
					state.style = formStyle;

					if(formProperties.addMinSize)
					{
						state.style.minWidth = formProperties.size.width + "px";
						state.style.minHeight = formProperties.size.height + "px";
					}	   
					state.properties = formProperties;

					for (var beanName in beanDatas) {
						// initialize with design data
						layout[beanName] = { position: 'absolute' }

						var newBeanConversionInfo = beanDatas[beanName].conversions;
						var beanConversionInfo = newBeanConversionInfo ? $sabloUtils.getOrCreateInDepthProperty($sabloApplication.getFormStatesConversionInfo(), formName, beanName) : $sabloUtils.getInDepthProperty($sabloApplication.getFormStatesConversionInfo(), formName, beanName);

						applyBeanData(state.model[beanName], layout[beanName], beanDatas[beanName], formProperties.designSize, $sabloApplication.getChangeNotifierGenerator(formName, beanName), beanConversionInfo, newBeanConversionInfo, formScope)
					}
				} else {
					// already initialized in the past; just make sure 'smart' properties use the correct (new) scope
					$sabloApplication.updateScopeForState(formName, formScope, state);
					return state;
				}
			}

			return state;
		},

		requestInitialData: function(formname, formState) {
			if (formState.initializing) $sabloApplication.requestInitialData(formname, function(initialFormData,formState) {
				for (var beanName in initialFormData) {
					if (beanName != '') {
						applyBeanLayout(formState.model[beanName], formState.layout[beanName], initialFormData[beanName], formState.properties.designSize)
					}
				}
			});
		},

		// used by form template js
		sendChanges: sendChanges,

		pushDPChange: function(formname, beanname, property) {
			$sabloApplication.getFormStateWithData(formname).then(function (formState) {
				var changes = {}

				// default model, simple direct form child component
				var formStatesConversionInfo = $sabloApplication.getFormStatesConversionInfo()
				var conversionInfo = (formStatesConversionInfo[formname] ? formStatesConversionInfo[formname][beanname] : undefined);

				if (conversionInfo && conversionInfo[property]) {
					changes[property] = $sabloConverters.convertFromClientToServer(formState.model[beanname][property], conversionInfo[property], undefined);
				} else {
					changes[property] = $sabloUtils.convertClientObject(formState.model[beanname][property]);
				}
				if (latestApplyCall.formname === formname && latestApplyCall.beanname === beanname && latestApplyCall.property === property && angular.equals(changes,latestApplyCall.changes)) {
					return;
				}
				latestApplyCall.formname = formname;
				latestApplyCall.beanname = beanname;
				latestApplyCall.property = property;
				latestApplyCall.changes = changes;
				$sabloApplication.callService('formService', 'svyPush', latestApplyCall, true)
			});
		}
	}
}).factory("$formService",function($sabloApplication,$servoyInternal,$rootScope,$log) {
	return {
		formWillShow: function(formname,notifyFormVisibility,parentForm,beanName,relationname,formIndex) {
			if ($log.debugEnabled) $log.debug("svy * Form " + formname + " is preparing to show. Notify server needed: " + notifyFormVisibility);
			if ($rootScope.updatingFormName === formname) {
				if ($log.debugEnabled) $log.debug("svy * Form " + formname + " was set in hidden div. Clearing out hidden div.");
				$rootScope.updatingFormUrl = ''; // it's going to be shown; remove it from hidden DOM
				$rootScope.updatingFormName = null;
			}

			if (!formname) {
				throw new Error("formname is undefined");
			}
			$sabloApplication.getFormState(formname).then(function (formState) {
				// if first show of this form in browser window then request initial data (dataproviders and such)
				if (notifyFormVisibility) $sabloApplication.callService('formService', 'formvisibility', {formname:formname,visible:true,parentForm:parentForm,bean:beanName,relation:relationname,formIndex:formIndex}, true);
				if (formState.initializing && !formState.initialDataRequested) $servoyInternal.requestInitialData(formname, formState);
			});
		},
		hideForm: function(formname,parentForm,beanName,relationname,formIndex) {
			if (!formname) {
				throw new Error("formname is undefined");
			}
			return $sabloApplication.callService('formService', 'formvisibility', {formname:formname,visible:false,parentForm:parentForm,bean:beanName,relation:relationname,formIndex:formIndex});
		},
		/**
		 * Use for going back and forward in history.
		 * @param formname is the form to show 
		 */
		goToForm: function(formname)
		{
			if (!formname) {
				throw new Error("formname is undefined");
			}
			return $sabloApplication.callService('formService', 'gotoform', {formname:formname});
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
}).directive('svyImagemediaid',  function ($parse,$timeout,$solutionSettings,$anchorConstants,$window) {
	return {
		restrict: 'A',
		link: function (scope, element, attrs) {     

			var rollOverImgStyle = null; 
			var imgStyle = null;
			var media = null;
			var clearStyle ={ width:'0px',
					height:'0px',
					backgroundImage:''}


			var setImageStyle = function(){
				if (media && media.visible)
				{
					// the value from model may be incorrect so take value from ui
					var componentSize = {width: element[0].parentNode.parentNode.offsetWidth,height: element[0].parentNode.parentNode.offsetHeight};
					var mediaOptions = scope.$eval('model.mediaOptions');
					if(media.rollOverImg){ 
						rollOverImgStyle= parseImageOptions( media.rollOverImg, mediaOptions, componentSize);
					}else {
						rollOverImgStyle = null
					}
					if(media.img){
						imgStyle =parseImageOptions( media.img, mediaOptions, componentSize)
						element.css(imgStyle)
					}else {
						imgStyle = null;
					} 
				}
			}
			
			if (scope.model && scope.model.anchors && $solutionSettings.enableAnchoring) {
				if (((scope.model.anchors & $anchorConstants.NORTH != 0) && (scope.model.anchors & $anchorConstants.SOUTH != 0)) || ((scope.model.anchors & $anchorConstants.EAST != 0) && (scope.model.anchors & $anchorConstants.WEST != 0)))
				{
					// anchored image, add resize listener
					var resizeTimeoutID = null;
					$window.addEventListener('resize',setImageStyle);
				}
			}
			scope.$watch(attrs.svyImagemediaid,function(newVal) {
				media = newVal;
				angular.element(element[0]).ready(setImageStyle);
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
				if(rollOverImgStyle){
					if(imgStyle){
						element.css(imgStyle)
					}else{
						element.css(clearStyle)
					} 
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
			var isForm = $attrs['svyLayoutUpdate'].length == 0; 
			if(isForm) {
				compModel = $scope.formProperties;
			} else {
				//compModel = $scope.model[$attrs['svyLayoutUpdate']];
			}
			if (!compModel) return; // not found, maybe a missing bean

			if(isForm || compModel.anchors) {
				var resizeTimeoutID = null;
				$window.addEventListener('resize',function() { 
					if(resizeTimeoutID) $timeout.cancel(resizeTimeoutID);
					resizeTimeoutID = $timeout( function() {
						// TODO: visibility must be based on properties of type visible, not on property name
						if(isForm || compModel.visible) {
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
}).directive('svyFormload',  function ($timeout, $servoyInternal, $sabloApplication, $windowService, $rootScope, $log) {
	return {
		restrict: 'E',
		compile: function(tElem, tAttrs){
			var formName = tAttrs.formname; 
			if ($log.debugEnabled) $log.debug("svy * compile svyFormload for form = " + formName);

			// it sometimes happens that this gets called from a div that is detatched from the real page body somewhere in parents - and
			// top-most $scope of parents is also destroyed already, although child scopes are not marked as destroyed; (this looks like an angular bug actually)
			// for example sometimes when a recreateUI of the main form happens (and main form URL changes while the form remains the same);
			// we should ignore such situations...
			var blocked = false;
			var formState = $sabloApplication.getFormStateEvenIfNotYetResolved(formName);
			var someAncestor = tElem.get(0).parentNode;
			while (someAncestor && someAncestor !== window.document) someAncestor = someAncestor.parentNode;
			if (someAncestor === window.document) {
				var inHiddenDiv = (tElem.parent().attr("hiddendiv") === "true");
				if (formState && (formState.resolving || $sabloApplication.hasResolvedFormState(formName))) {
					if ($log.debugEnabled) $log.debug("svy * Template will discard hidden div; resolving = " + formState.resolving + ", resolved = " + $sabloApplication.hasResolvedFormState(formName) +
							", name = " + formName + ", parentScopeIsOfHiddenDiv = " + inHiddenDiv);
					// someone already loaded or is loading this form....
					if ($rootScope.updatingFormName === formName) {
						// in updatingFormUrl must be cleared as this form will show or is already showing elsewhere
						$rootScope.updatingFormUrl = '';
						delete $rootScope.updatingFormName;
					} 
					else {
						// make sure the resolving state is deleted then so it corrects itself.
						delete formState.resolving;
						$log.error("svy * Unexpected: a form is being loaded twice at the same time(" + formName + ")");
					}

					if (inHiddenDiv) {
						tElem.empty(); // don't allow loading stuff further in this directive - so effectively blocks the form from loading in $rootScope.updatingFormUrl
						blocked = true;
						// so the form is already (being) loaded in non-hidden div; hidden div can relax; it shouldn't load form twice
					} else {
						// else it is already loaded in hidden div but now it wants to load in non-hidden div; so this has priority; allow it
						if (formState.getScope) formState.getScope().hiddenDivFormDiscarded = true; // skip altering form state on hidden form scope destroy (as destroy might happen after the other place loads the form); new load will soon resolve the form again if it hasn't already at that time
						if ($sabloApplication.hasResolvedFormState(formName)) $sabloApplication.unResolveFormState(formName);
						else formState.blockPostLinkInHidden = true;
					}
				}
			} else {
				tElem.empty(); // don't allow loading stuff further in this directive - it's probably being loaded in a floating DOM - not linked to page document // TODO how can we detect invalid case but still allow intentional loading of forms in floating DOM? we cant access nicely ancestor $scope.$$destroyed property from here
				blocked = true;
			}

			return {
				pre: function(scope, element, attrs) {},
				post: function(scope, element, attrs) {
					if ($log.debugEnabled) $log.debug("svy * postLink svyFormload for form = " + formName + ", hidden: " + inHiddenDiv);
					if (blocked) return; // the form load was blocked by that tElem.empty() a few lines above (form is already loaded elsewhere)
					if (inHiddenDiv && formState && formState.blockPostLinkInHidden) {
						delete formState.blockPostLinkInHidden; // the form began to load in a real location; don't resolve it in hidden div anymore
						return;
					}
					if ($log.debugEnabled) $log.debug("svy * svyFormload will resolve = " + formName);

					$timeout(function() {
						var resolvedFormState = $sabloApplication.resolveFormState(formName);
						if (resolvedFormState) {
							$sabloApplication.callService('formService', 'formLoaded', { formname: formName }, true)
							var formWidth = element.prop('offsetWidth');
							var formHeight = element.prop('offsetHeight');
							if (formWidth === 0 && formHeight === 0)
							{
								var formWidth = element.children().prop('offsetWidth');
								var formHeight = element.children().prop('offsetHeight');
							}	
							resolvedFormState.properties.size.width = formWidth; // formState.properties == formState.getScope().formProperties here
							resolvedFormState.properties.size.height = formHeight;
						}
					});
				}
			}
		}
	}
}).value("$solutionSettings",  {
	mainForm: {},
	navigatorForm: {width:0},
	solutionTitle: "",
	styleSheetPath: undefined,
	ltrOrientation : true,
	enableAnchoring: true
}).controller("MainController", function($scope, $solutionSettings, $servoyInternal, $windowService,$rootScope,webStorage, $sabloApplication) {
	$servoyInternal.connect();
	$scope.solutionSettings = $solutionSettings;
	$scope.getMainFormUrl = function() {
		return $solutionSettings.mainForm.name?$windowService.getFormUrl($solutionSettings.mainForm.name):"";
	}
	$scope.getNavigatorFormUrl = function() {
		var templateURLOrFormName = $solutionSettings.navigatorForm.name; // can be directly default nav. url or if not the name of the navigator form
		if (templateURLOrFormName && templateURLOrFormName.lastIndexOf("default_navigator_container.html") == -1) {
			return $windowService.getFormUrl($solutionSettings.navigatorForm.name);
		}
		return $solutionSettings.navigatorForm.name;
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
			if(sessionExpired.redirectUrl)	exp.redirectUrl= sessionExpired.redirectUrl;

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
//}]).factory("$anchoringUtils", [ function() {
//	var NORTH = 1;
//	var EAST = 2;
//	var SOUTH = 4;
//	var WEST = 8;
//	var DEFAULT = NORTH + WEST;
//	var ALL = NORTH + WEST + EAST + SOUTH;
//	
//	function shouldWatchSizeForAnchors(anchors) {
//		if (!anchors) return false;
//		return ((anchors & NORTH) && (anchors & SOUTH)) || ((anchors & EAST) && (anchors & WEST));
//	}
//	
//	function shouldWatchLocationForAnchors(anchors) {
//		if (!anchors) return true; // TODO change this to false for default if watch is not needed in that case (top-left)
//		return !(anchors & WEST) || !(anchors & NORTH);
//	}
//	
//	return {
//		NORTH: NORTH,
//		EAST: EAST,
//		SOUTH: SOUTH,
//		WEST: WEST,
//		DEFAULT: DEFAULT,
//		ALL: ALL,
//		
//		// depending on how a component is anchored we know if it can dynamically update it's location, in which case it needs to be watched/sent to server (for rhino access)
//		shouldWatchLocationForAnchors: shouldWatchLocationForAnchors,
//
//		// depending on how a component is anchored we know if it can dynamically update it's size, in which case it needs to be watched/sent to server (for rhino access)
//		shouldWatchSizeForAnchors: shouldWatchSizeForAnchors,
//		
//		getBoundsPropertiesToWatch: function getBoundsPropertiesToWatch(componentModel) {
//			var ret = {};
//			if (shouldWatchLocationForAnchors(componentModel.anchors)) ret['location'] = true; // deep watch
//			if (shouldWatchSizeForAnchors(componentModel.anchors)) ret['size'] = true; // deep watch
//			return ret;
//		}
//		
//	};
}]).factory("$applicationService",['$window','$timeout','webStorage','$modal','$sabloApplication','$solutionSettings','$rootScope','$svyFileuploadUtils','$locale', function($window,$timeout,webStorage,$modal,$sabloApplication,$solutionSettings,$rootScope,$svyFileuploadUtils,$locale) {
	var showDefaultLoginWindow = function() {
		$modal.open({
			templateUrl: 'templates/login.html',
			controller: 'LoginController',
			windowClass: 'login-window',
			backdrop: 'static',
			keyboard: false
		});				
	}
	return {
		setStyleSheet: function(path) {
			if (angular.isDefined(path)) {
				$solutionSettings.styleSheetPath = path +"?t="+Date.now();
			} else {
				delete $solutionSettings.styleSheetPath;
			}
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
		getUIProperty: function(key) {
			var json = webStorage.session.get("uiProperties");
			if (json) {
				return JSON.parse(json)[key];
			}
			return null;
		},
		setUIProperty: function(key,value) {
			var obj = {}
			var json = webStorage.session.get("uiProperties");
			if (json) {
				obj = JSON.parse(json);
			}
			if (value == null) delete obj[key]
			else obj[key] = value;
			webStorage.session.add("uiProperties", JSON.stringify(obj))
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
			$timeout(function() {
				if(url.indexOf('resources/dynamic') === 0 && target === '_self') {
					var ifrm = $window.frames['srv_downloadframe'];
					if (ifrm) {
						ifrm.location = url;
					}
					else {
						ifrm = document.createElement("IFRAME");
						ifrm.setAttribute("src", url);
						ifrm.setAttribute('id', 'srv_downloadframe');
						ifrm.setAttribute('name', 'srv_downloadframe');
						ifrm.style.width = 0 + "px";
						ifrm.style.height = 0 + "px";
						$window.document.body.appendChild(ifrm);
					}
				}
				else {
					$window.open(url,target,targetOptions);
				}
			}, timeout*1000)
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
			 var language  = this.getLanguage().split("-")[0];
			 this.setAngularLocale(language);
		     return {locale:this.getLanguage(),utcOffset:(new Date(new Date().getFullYear(), 0, 1, 0, 0, 0, 0).getTimezoneOffset() / -60),utcDstOffset:(new Date(new Date().getFullYear(), 6, 1, 0, 0, 0, 0).getTimezoneOffset() / -60)};
		},
		setAngularLocale : function(language){
			 var fileref= $window.document.createElement('script');
		     fileref.setAttribute("type","text/javascript");
		     fileref.setAttribute("src", "js/angular_i18n/angular-locale_"+language+".js");
		     fileref.onload = function () {
		    	 var localInjector = angular.injector(['ngLocale']),
		         externalLocale = localInjector.get('$locale');
		    	 angular.forEach(externalLocale, function(value, key) {
		    			$locale[key] = externalLocale[key];
		    	 });
		     }
		     $window.document.getElementsByTagName("head")[0].appendChild(fileref);
		},
		getLanguage:function() {
			// this returns first one of the languages array if the browser supports this (Chrome and FF) else it falls back to language or userLanguage (IE, and IE seems to return the right one from there) 
			return navigator.languages? navigator.languages[0] : (navigator.language || navigator.userLanguage);
		},
		setLocale:function(language,country) {
			try{
				this.setAngularLocale(language);
				numeral.language((language + '-' + country).toLowerCase());
				webStorage.session.add("locale", (language + '-' + country).toLowerCase())
			} catch(e) {
				try {
					numeral.language(language + '-' + country);
					webStorage.session.add("locale", language + '-' + country)
				} catch(e2) {
					try {
						//try it with just the language part
						numeral.language(language);
						webStorage.session.add("locale", language)
					} catch(e3) {}
				}
			}
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
		},
		showFileOpenDialog: function(title, multiselect) {
			$svyFileuploadUtils.open("resources/upload/" + $sabloApplication.getSessionId(), title, multiselect);
		},
		getSolutionName: function() {
			return $solutionSettings.solutionName;
		}
	}

}])
.run(function($window, $sabloApplication, $applicationService,webStorage) {
	$window.executeInlineScript = function(formname, script, params) {
		$sabloApplication.callService("formService", "executeInlineScript", {'formname' : formname, 'script' : script, 'params' : params},true)
	}
	var language = webStorage.session.get("locale");
	if (!language) language = $applicationService.getLanguage();
	// fix that only nl-nl is added but not just nl
	numeral.language('nl',numeral.languageData('nl-nl'));
	try{ 
		numeral.language(language);
	} catch(e) {
		var index = language.indexOf("-");
		if (index != -1) {
			try {
				//try it with just the language part
				numeral.language(language.substring(0,index));
			} catch(e2) {}
		}
	}
})
