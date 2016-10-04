var controllerProvider;
angular.module('servoyApp', ['sabloApp', 'servoy','webStorageModule','servoy-components', 'webSocketModule','servoyWindowManager',
                             'pasvaz.bindonce', 'ngSanitize', 'pascalprecht.translate']

).config(['$controllerProvider', '$logProvider', '$translateProvider', function($controllerProvider, $logProvider, $translateProvider) {
	controllerProvider = $controllerProvider;
	$logProvider.debugEnabled(false);
	
	// TODO: check if this does not break some translated values
	$translateProvider.useSanitizeValueStrategy('sanitize');
	$translateProvider.preferredLanguage('servoy-i18n');
    $translateProvider.useLoader('translateFilterServoyI18Loader');
    $translateProvider.useMissingTranslationHandler('translateFilterServoyI18nMessageLoader');
    $translateProvider.forceAsyncReload(true);
	
}]).factory('$servoyInternal', function ($rootScope, webStorage, $anchorConstants, $q, $solutionSettings, $window, $sessionService, $sabloConverters, $sabloUtils, $sabloApplication, $applicationService, $utils,$foundsetTypeConstants) {

	function getComponentChanges(now, prev, beanConversionInfo, beanLayout, parentSize, property, beanModel) {

		var changes = $sabloApplication.getComponentChanges(now, prev, beanConversionInfo, parentSize, property)
		// TODO: visibility must be based on properties of type visible, not on property name
		if (changes.location || changes.size || changes.visible || changes.anchors) {
			if (beanLayout) {
				applyBeanLayout(beanModel, beanLayout, changes, parentSize, false);
			}
		}
		return changes;
	}

	function sendChanges(now, prev, formname, beanname, property) {
		$sabloApplication.getFormStateWithData(formname).then(function (formState) {
			var beanConversionInfo = $sabloUtils.getInDepthProperty($sabloApplication.getFormStatesConversionInfo(), formname, beanname);
			var changes = getComponentChanges(now, prev, beanConversionInfo, formState.layout[beanname], formState.properties.designSize, property, formState.model[beanname]);
			if (Object.getOwnPropertyNames(changes).length > 0) {
				// if this is a simple property change without any special conversions then then push the old value.
				if (angular.isDefined(property) && !(beanConversionInfo && beanConversionInfo[property])) {
					var oldvalues ={};
					oldvalues[property] = $sabloUtils.convertClientObject(prev)
					$sabloApplication.callService('formService', 'dataPush', {formname:formname,beanname:beanname,changes:changes,oldvalues:oldvalues}, true)
				}
				else {
					$sabloApplication.callService('formService', 'dataPush', {formname:formname,beanname:beanname,changes:changes}, true)
				}
			}
		})
	}
	
	function applyBeanData(beanModel, beanLayout, beanData, containerSize, changeNotifierGenerator, beanConversionInfo, newConversionInfo, componentScope) {

		$sabloApplication.applyBeanData(beanModel, beanData, containerSize, changeNotifierGenerator, beanConversionInfo, newConversionInfo, componentScope)
		applyBeanLayout(beanModel, beanLayout, beanData, containerSize, true)
	}

	function applyBeanLayout(beanModel, beanLayout, beanData, containerSize, isApplyBeanData) {

		var runtimeChanges = !isApplyBeanData && ( beanData.size != undefined || beanData.location != undefined );
		//beanData.anchors means anchors changed or must be initialized
		if ((beanData.anchors || runtimeChanges) && containerSize && $solutionSettings.enableAnchoring) {
			var anchoredTop = (beanModel.anchors & $anchorConstants.NORTH) != 0; // north
			var anchoredRight = (beanModel.anchors & $anchorConstants.EAST) != 0; // east
			var anchoredBottom = (beanModel.anchors & $anchorConstants.SOUTH) != 0; // south
			var anchoredLeft = (beanModel.anchors & $anchorConstants.WEST) != 0; //west

			if (!anchoredLeft && !anchoredRight) anchoredLeft = true;
			if (!anchoredTop && !anchoredBottom) anchoredTop = true;

			if (anchoredTop || runtimeChanges)
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

			if (anchoredLeft || runtimeChanges)
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
		if (!solName) $solutionSettings.solutionName  = /.*\/([\$\w]+)\/.*/.exec($window.location.pathname)[1];
		else $solutionSettings.solutionName  = solName;
		$solutionSettings.windowName = $sabloApplication.getWindowId();
		var recordingPrefix;
		if ($window.location.search.indexOf("svy_record=true") > -1) {
			recordingPrefix = "/recording/websocket";
			
		}
		var wsSession = $sabloApplication.connect('/solutions/'+$solutionSettings.solutionName, {solution:$solutionSettings.solutionName}, recordingPrefix)
		wsSession.onMessageObject(function (msg, conversionInfo) {
			// data got back from the server
			for(var formname in msg.forms) {
				// current model
				if (!$sabloApplication.hasFormState(formname)) {
					// if the form is not yet on the client ,wait for it and then apply it
					$sabloApplication.getFormState(formname).then(getFormMessageHandler(formname, msg, conversionInfo), 
							function(err) { $log.error("Error getting form state (svy) when trying to handle msg. from server: " + err); });
				}
				else {
					getFormMessageHandler(formname, msg, conversionInfo)($sabloApplication.getFormStateEvenIfNotYetResolved(formname));
				}
			}
			
			function setFindMode(beanData)
			{
				if (beanData['findmode'])
				{
					if (window.shortcut.all_shortcuts['ENTER'] !== undefined)
					{
						beanData.hasEnterShortcut = true; 
					}
					else
					{
						function performFind(event)
						{
							var element = angular.element(event.srcElement ? event.srcElement : event.target);									
							if(element && element.attr('ng-model'))
							{
								var dataproviderString = element.attr('ng-model');
								var index = dataproviderString.indexOf('.');
								if (index > 0) 
								{
									var modelString = dataproviderString.substring(0,index);
									var propertyname = dataproviderString.substring(index+1);
									var svyServoyApi = $utils.findAttribute(element, element.scope(), "svy-servoyApi");
									if (svyServoyApi && svyServoyApi.apply) 
									{
										svyServoyApi.apply(propertyname);
									}
								}
							}
							
							$sabloApplication.callService("formService", "performFind", {'formname' : formname, 'clear' : true, 'reduce': true, 'showDialogOnNoResults':true},true);
						}
						window.shortcut.add('ENTER', performFind);
					}
				}
				else if (beanData['findmode'] == false && !beanData.hasEnterShortcut)
				{
					window.shortcut.remove('ENTER');
				}
			}

			function getFormMessageHandler(formname, msg, conversionInfo) {
				return function (formState) {
					var formModel = formState.model;
					var layout = formState.layout;
					var newFormData = msg.forms[formname];

					for (var beanname in newFormData) {
						// copy over the changes, skip for form properties (beanname empty)
						var beanData = newFormData[beanname];
						if (beanname != '') {
							var beanModel = formModel[beanname];
							if (beanModel != undefined && (beanData.size != undefined ||  beanData.location != undefined)) {	
								//size or location were changed at runtime, we need to update components with anchors
								beanData.anchors = beanModel.anchors;
							}
							applyBeanLayout(beanModel, layout[beanname],beanData, formState.properties.designSize, false)
						}
						else if (beanData['findmode'] !== undefined)
						{
							setFindMode(beanData);
						}

					}
				}
			}

			if (msg.sessionid && recordingPrefix) {
				var btn = $window.document.createElement("A");        // Create a <button> element
				btn.href = "solutions/" + msg.sessionid + ".recording";
				btn.target = "_blank";
				btn.style.position= "absolute";
				btn.style.right = "0px";
				btn.style.bottom = "0px";
				var t = $window.document.createTextNode("Download"); 
				btn.appendChild(t);                                // Append the text to <button>
				$window.document.body.appendChild(btn); 
			}
			if (msg.windowid) {
				$solutionSettings.windowName = msg.windowid;
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

		initFormState: function(formName, beanDatas, formProperties, formScope, resolve, parentSizes) {

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

						applyBeanData(state.model[beanName], layout[beanName], beanDatas[beanName],parentSizes && parentSizes[beanName] ? parentSizes[beanName] : formProperties.designSize, $sabloApplication.getChangeNotifierGenerator(formName, beanName), beanConversionInfo, newBeanConversionInfo, formScope)
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
						applyBeanLayout(formState.model[beanName], formState.layout[beanName], initialFormData[beanName], formState.properties.designSize, false)
					}
				}
			});
		},

		// used by form template js
		sendChanges: sendChanges,
		
		callServerSideApi: function(formname, beanname, methodName, args) {
			return $sabloApplication.callService('formService', 'callServerSideApi', {formname:formname,beanname:beanname,methodName:methodName,args:args})
		},

		pushDPChange: function(formname, beanname, property) {
			var formState = $sabloApplication.getFormStateEvenIfNotYetResolved(formname);
			var changes = {}

			// default model, simple direct form child component
			var formStatesConversionInfo = $sabloApplication.getFormStatesConversionInfo()
			var conversionInfo = (formStatesConversionInfo[formname] ? formStatesConversionInfo[formname][beanname] : undefined);
			var rowid = null;
			if (conversionInfo && conversionInfo[property]) {
				changes[property] = $sabloConverters.convertFromClientToServer(formState.model[beanname][property], conversionInfo[property], undefined);
			} else {
				var dpValue = null;
				if (property.indexOf('.') > 0)
				{
					//nested property
					dpValue = eval('formState.model[beanname].'+property)
					if (property.endsWith("]"))
					{
						var index = parseInt(property.substring(property.lastIndexOf('[')+1,property.length-1));
						var dpPath =  property.substring(0,property.lastIndexOf('['));
						var dpArray = eval('formState.model[beanname].'+dpPath);
						if (dpArray && dpArray[$sabloConverters.INTERNAL_IMPL] && dpArray[$sabloConverters.INTERNAL_IMPL][$foundsetTypeConstants.FOR_FOUNDSET_PROPERTY])
						{
							var foundset = dpArray[$sabloConverters.INTERNAL_IMPL][$foundsetTypeConstants.FOR_FOUNDSET_PROPERTY]();
							if (foundset)
							{
								rowid = foundset.viewPort.rows[index][$foundsetTypeConstants.ROW_ID_COL_KEY];
							}	
						}	
					}	
				}
				else
				{
					dpValue = formState.model[beanname][property];
				}	
				changes[property] = $sabloUtils.convertClientObject(dpValue);
			}
			var dpChange = {formname:formname,beanname:beanname,property:property,changes:changes};
			if (rowid)
			{
				dpChange.rowid = rowid;
			}	
			$sabloApplication.callService('formService', 'svyPush', dpChange, true)
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
				// if first show of this form in browser window then request initial data (dataproviders and such);
				if (notifyFormVisibility) $sabloApplication.callService('formService', 'formvisibility', {formname:formname,visible:true,parentForm:parentForm,bean:beanName,relation:relationname,formIndex:formIndex}, true);
				if (formState.initializing && !formState.initialDataRequested) $servoyInternal.requestInitialData(formname, formState);
			});
		},
		hideForm: function(formname,parentForm,beanName,relationname,formIndex,formnameThatWillBeShown,relationnameThatWillBeShown,formIndexThatWillBeShown) {
			if (!formname) {
				throw new Error("formname is undefined");
			}
			return $sabloApplication.callService('formService', 'formvisibility', {formname:formname,visible:false,parentForm:parentForm,bean:beanName,relation:relationname,formIndex:formIndex,show:{formname:formnameThatWillBeShown,relation:relationnameThatWillBeShown,formIndex:formIndexThatWillBeShown}});
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
				// clicking on part element or grid stuff triggers autosave
				if ((event.target == element[0]) || (event.target.parentNode == element[0]) || $(event.target).is('[class^="ui-grid"]'))
				{
					if($(event.target).closest("div[svy-autosave]")[0] == element[0])
					{
						// only execute for closest form
						$sabloApplication.callService("applicationServerService", "autosave",{}, true);
					}	
				}
			});
		}
	};
}).directive('svyImagemediafield',  function () {
	return {
		restrict: 'A',
		link: function (scope, element, attrs) {
			element.on('load', function(event) {
				var alignStyle = {top: '0px', left: '0px'};
				var horizontalAlignment = scope.$eval('model.horizontalAlignment');
				var imageHeight = element[0].clientHeight;
				var imageWidth = element[0].clientWidth;
				// vertical align cennter
				var height = element[0].parentNode.clientHeight;
				if (height > imageHeight) alignStyle.top = (height - imageHeight) / 2+'px';
				// horizontal align (default left)
				var width = element[0].parentNode.clientWidth;
				if (width > imageWidth) {
					if (horizontalAlignment == 0 /*SwingConstants.CENTER*/) alignStyle.left = (width - imageWidth) / 2+'px';
					else if (horizontalAlignment == 4 /*SwingConstants.RIGHT*/) alignStyle.left = (width - imageWidth)+'px';
					else
					{
						if ((element[0].parentNode.childNodes > 1) && (imageHeight + 34 < height)) alignStyle.left ='51px';
					}
				}
				element.css(alignStyle);
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
					var componentSize = {width: $(element[0].parentNode.parentNode).width(),height: $(element[0].parentNode.parentNode).height()};
					var mediaOptions = scope.$eval('model.mediaOptions');
					if(media.rollOverImg){ 
						rollOverImgStyle= parseImageOptions( media.rollOverImg, mediaOptions, componentSize);
					}else {
						rollOverImgStyle = null
					}
					if(media.img){
						imgStyle =parseImageOptions( media.img, mediaOptions, componentSize)
						if(media.updateParentSize && (imgStyle.height == (componentSize.height + 'px'))) {
							// if exact image, just make sure it shows well; vertical align center can have rounding issues
							$(element[0].parentNode).css({height: imgStyle.height, top: 0, transform: 'none'});
							imgStyle['vertical-align'] = "top";
						}
						element.css(imgStyle)
					}else {
						imgStyle = null;
						if(media.updateParentSize) {
							$(element[0].parentNode).css({height: ''});
						}						
						element.css(clearStyle);
					} 
				}
			}

			if (scope.model && scope.model.anchors && $solutionSettings.enableAnchoring) {
				if ((((scope.model.anchors & $anchorConstants.NORTH) != 0) && ((scope.model.anchors & $anchorConstants.SOUTH) != 0)) || (((scope.model.anchors & $anchorConstants.EAST) != 0) && ((scope.model.anchors & $anchorConstants.WEST) != 0)))
				{
					// anchored image, add resize listener
					var resizeTimeoutID = null;
					$window.addEventListener('resize',setImageStyle);
					scope.$on("dialogResize",setImageStyle);
				}
			}
			scope.$watch(attrs.svyImagemediaid,function(newVal) {
				media = newVal;
				var componentSize = {width: element[0].parentNode.parentNode.offsetWidth,height: element[0].parentNode.parentNode.offsetHeight};
				if (componentSize.width > 0 && componentSize.height > 0 )
					angular.element(element[0]).ready(setImageStyle);
				else if (media && media.visible) { 
					angular.element(element[0]).ready($timeout(function(){setImageStyle();},0,false));
				}
			}, true)


			function parseImageOptions(image,mediaOptions,componentSize){
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
				var onResize = function() {
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
				}
				$window.addEventListener('resize', onResize);
				$scope.$on("$destroy", function() {
					$window.removeEventListener('resize', onResize);
				})
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
				if(!inHiddenDiv) {
					// skip nested forms
					if(tElem.closest("[hiddendiv]").length) {
						tElem.empty();
						blocked = true;
					}
				}
				if (!blocked && formState && (formState.resolving || $sabloApplication.hasResolvedFormState(formName))) {
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
						// else it is already loaded in hidden div but now it wants to load in non-hidden div; so this has priority; allow it; here getScope() is the scope provided by the hidden div load; the new one was not yet contributed
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

					// we used to do the rest of the following code in a $timeout to make sure components (that use templateURL so they load async) did register their APIs in their link methods;
					// but now the API call code will wait itself for a while so that the APIs to be contributed by components; all other usages of 'resolved' form states don't need the actual
					// components to be fully loaded, and avoiding that $timeout makes the app. run faster + avoids the form being already hidden before it executes
					
					if ($log.debugEnabled) $log.debug("svy * svyFormload is now resolving form: " + formName);
					var resolvedFormState = $sabloApplication.resolveFormState(formName);

					if (resolvedFormState) {
						if ($log.debugEnabled) $log.debug("svy * svyFormload is letting server know that form in now resolved: " + formName);
						$sabloApplication.callService('formService', 'formLoaded', { formname: formName }, true)

						function initializeFormSizes()
						{
							var formWidth = element.prop('offsetWidth');
							var formHeight = element.prop('offsetHeight');
							// for some reason, height is not 0 in firefox for svy-formload tag; should we just assume the structure and remove this test altogether?
							if (formWidth === 0 || formHeight === 0)
							{
								var formWidth = element.children().prop('offsetWidth');
								var formHeight = element.children().prop('offsetHeight');
							}
		
							resolvedFormState.properties.size.width = formWidth; // formState.properties == formState.getScope().formProperties here
							resolvedFormState.properties.size.height = formHeight;

						}
						
						// also update this size in a timeout as here it seems element bounds are not yet stable (sometimes they are 0,0 sometimes height is wrong)
						$timeout(function() {
							// check again (because of the use of $timeout above) if the form was shown in a real location meanwhile in which case we must not rely on these DOM elements anymore
							if (inHiddenDiv && formState && formState.blockPostLinkInHidden) {
								delete formState.blockPostLinkInHidden; // the form began to load in a real location; don't resolve it in hidden div anymore
								return;
							}
							
							initializeFormSizes();
							if ($log.debugEnabled) $log.debug("svy * in postLink TOut of svyFormload (" + formName + "): resolvedFormState.properties.size = (" + resolvedFormState.properties.size.width + ", " + resolvedFormState.properties.size.height + ")");
						});
					}
				}
			}
		}
	}
}).factory('translateFilterServoyI18Loader', ['$q', function ($q) {
   return function (options) {
       // return empty translation, translateFilterServoyI18nMessageLoader is used as missingTranslationHandler
       return $q.when({});
    };
}]).factory('translateFilterServoyI18nMessageLoader', ['$svyI18NService', '$q', function ($svyI18NService, $q) {
    // use servoy i18n as loader for the translate filter
	return function(key) {
		return $svyI18NService.getI18NMessage(key);
	}
	}]
).value("$solutionSettings",  {
	mainForm: {},
	navigatorForm: {width:0},
	solutionTitle: "",
	styleSheetPaths: [],
	ltrOrientation : true,
	enableAnchoring: true
}).controller("MainController", function($scope, $solutionSettings, $servoyInternal, $windowService, $rootScope, webStorage, $sabloApplication, $applicationService, $svyI18NService) {
	$servoyInternal.connect();

	// initialize locale client side
	var locale = webStorage.session.get("locale");
	if (!locale) {
		locale = $sabloApplication.getLocale();
	} else {
		var array = locale.split('-');
		locale = { language : array[0], country : array[1] };
	}
	$applicationService.setLocale(locale.language, locale.country, true);

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
.factory('$sessionService',['$solutionSettings','$window','$rootScope','$servoyWindowManager',function($solutionSettings,$window,$rootScope,$servoyWindowManager){

	return {
		expireSession : function (sessionExpired){
			$servoyWindowManager.destroyAllDialogs();
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
					redirectTimeout : -1
			}
			if(maintenanceMode.viewUrl) ment.viewUrl = mentenanceMode.viewUrl 
			if(maintenanceMode.redirectUrl)	ment.redirectUrl = maintenanceMode.redirectUrl;
			if(maintenanceMode.redirectTimeout)	ment.redirectTimeout = maintenanceMode.redirectTimeout;

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
//	}]).factory("$anchoringUtils", [ function() {
//	var NORTH = 1;
//	var EAST = 2;
//	var SOUTH = 4;
//	var WEST = 8;
//	var DEFAULT = NORTH + WEST;
//	var ALL = NORTH + WEST + EAST + SOUTH;

//	function shouldWatchSizeForAnchors(anchors) {
//	if (!anchors) return false;
//	return ((anchors & NORTH) && (anchors & SOUTH)) || ((anchors & EAST) && (anchors & WEST));
//	}

//	function shouldWatchLocationForAnchors(anchors) {
//	if (!anchors) return true; // TODO change this to false for default if watch is not needed in that case (top-left)
//	return !(anchors & WEST) || !(anchors & NORTH);
//	}

//	return {
//	NORTH: NORTH,
//	EAST: EAST,
//	SOUTH: SOUTH,
//	WEST: WEST,
//	DEFAULT: DEFAULT,
//	ALL: ALL,

//	// depending on how a component is anchored we know if it can dynamically update it's location, in which case it needs to be watched/sent to server (for rhino access)
//	shouldWatchLocationForAnchors: shouldWatchLocationForAnchors,

//	// depending on how a component is anchored we know if it can dynamically update it's size, in which case it needs to be watched/sent to server (for rhino access)
//	shouldWatchSizeForAnchors: shouldWatchSizeForAnchors,

//	getBoundsPropertiesToWatch: function getBoundsPropertiesToWatch(componentModel) {
//	var ret = {};
//	if (shouldWatchLocationForAnchors(componentModel.anchors)) ret['location'] = true; // deep watch
//	if (shouldWatchSizeForAnchors(componentModel.anchors)) ret['size'] = true; // deep watch
//	return ret;
//	}

//	};
}]).factory("$applicationService",['$window','$timeout','webStorage','$modal','$sabloApplication','$solutionSettings','$rootScope','$svyFileuploadUtils','$locale','$svyI18NService','$log','$translate', function($window,$timeout,webStorage,$modal,$sabloApplication,$solutionSettings,$rootScope,$svyFileuploadUtils,$locale,$svyI18NService,$log,$translate) {
	var showDefaultLoginWindow = function() {
		$modal.open({
			templateUrl: 'templates/login.html',
			controller: 'LoginController',
			windowClass: 'login-window',
			backdrop: 'static',
			keyboard: false
		});				
	}
	var uiProperties;
	function getUiProperties() {
		if (!angular.isDefined(uiProperties)) {
			var json = webStorage.session.get("uiProperties");
			if (json) {
				uiProperties = JSON.parse(json);
			} else {
				uiProperties = {};
			}
		}
		return uiProperties;
	}
	
	var userProperties;
	function getUserProperties() {
		if (!angular.isDefined(userProperties)) {
			var json = webStorage.session.get("userProperties");
			if (json) {
				userProperties = JSON.parse(json);
			} else {
				userProperties = {};
			}
		}
		return userProperties;
	}
	
	function trustAsHtml(beanModel) {
		
		if (beanModel && beanModel.clientProperty && angular.isDefined(beanModel.clientProperty.trustDataAsHtml))
		{
			return beanModel.clientProperty.trustDataAsHtml;
		}
		
		return getUiProperties()["trustDataAsHtml"];
	}
	
	return {
		setStyleSheets: function(paths) {
			$solutionSettings.styleSheetPaths = paths;
			if (!$rootScope.$$phase) $rootScope.$digest();
		},
		getUserProperty: function(key) {
			return getUserProperties()[key];
		},
		setUserProperty: function(key,value) {
			var userProps = getUserProperties();
			if (value == null) delete userProps[key];
			else userProps[key] = value;
			webStorage.local.add("userProperties", JSON.stringify(userProps))
		},
		getUIProperty: function(key) {
			return getUiProperties()[key];
		},
		setUIProperty: function(key,value) {
			var uiProps = getUiProperties();
			if (value == null) delete uiProps[key];
			else uiProps[key] = value;
			webStorage.session.add("uiProperties", JSON.stringify(uiProps))
		},
		getUserPropertyNames: function() {
			return Object.getOwnPropertyNames(getUserProperties());
		},
		showMessage: function(message) {
			$window.alert(message);
		},
		showUrl:function(url,target,targetOptions,timeout){
			if(!target) target ='_blank';
			if(!timeout) timeout = 0;	    	 
			$timeout(function() {
				if(url.indexOf('resources/dynamic') === 0 && target === '_self') {
					var ifrm = document.getElementById('srv_downloadframe');
					if (ifrm) {
						ifrm.src = url;
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
			var locale = $sabloApplication.getLocale();
			this.setAngularLocale(locale.language);
			var userAgent = this.getUserAgentAndPlatform();
			return {
				userAgent : userAgent.userAgent,
				platform : userAgent.platform,
				locale : locale.full,
				utcOffset : (new Date(new Date().getFullYear(), 0, 1, 0, 0, 0, 0).getTimezoneOffset() / -60),utcDstOffset:(new Date(new Date().getFullYear(), 6, 1, 0, 0, 0, 0).getTimezoneOffset() / -60)
			};
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
		setLocale : function(language, country, initializing) {
			try{
                $translate.refresh();
				$svyI18NService.flush();
				this.setAngularLocale(language);
				numeral.language((language + '-' + country).toLowerCase());
				if (!initializing) webStorage.session.add("locale", (language + '-' + country).toLowerCase());
			} catch(e) {
				try {
					numeral.language(language + '-' + country);
					if (!initializing) webStorage.session.add("locale", language + '-' + country);
				} catch(e2) {
					try {
						//try it with just the language part
						numeral.language(language);
						if (!initializing) webStorage.session.add("locale", language);
					} catch(e3) {
						try {
							//try it with just the language part but lowercase
							numeral.language(language.toLowerCase());
							if (!initializing) webStorage.session.add("locale", language);
						} catch(e4) {
							try {
								//try to duplicate the language in case it's only defined like that
								numeral.language(language.toLowerCase() + "-" + language.toLowerCase()); // nl-nl for example is defined but browser only says 'nl' (this won't work for all languages for example "en-en" I don't think even exists)
								if (!initializing) webStorage.session.add("locale", language);
							} catch(e5) {
								// we can't find a suitable locale defined in languages.js; get the needed things from server (Java knows more locales)
								// and create the locate info from that
								promise = $sabloApplication.callService("i18nService", "generateLocaleForNumeralJS", country ? {'language' : language, 'country' : country} : {'language' : language}, false);
								// TODO should we always do this (get stuff from server side java) instead of trying first to rely on numeral.js and languages.js provided langs?
								var numeralLanguage = language + (country ? '-' + country : "");
								promise.then(function(numeralLocaleInfo) {
									if ($log.debugEnabled) $log.debug("Locale '" + numeralLanguage + "' not found in client js lib, but it was constructed based on server Java locale-specific information: " + JSON.stringify(numeralLocaleInfo));
									numeralLocaleInfo.ordinal = function (number) {
										return ".";
									};
									numeral.language(numeralLanguage, numeralLocaleInfo);
									numeral.language(numeralLanguage);
									if (!initializing) {
										webStorage.session.add("locale", numeralLanguage);
										$sabloApplication.setLocale({ language : language, country : country });
									}
								}, function(reason) {
									$log.warn("Cannot properly handle locale '" + numeralLanguage + "'. It is not available in js libs and it could not be loaded from server...");
								});
							}
						}
					}
				}
			}
			var lang = webStorage.session.get("locale");
			if (lang) {
				var array = language.split('-');
				$sabloApplication.setLocale({ language : array[0], country : array[1] });
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
		},
		trustAsHtml: trustAsHtml
	}

}])
.run(function($window, $sabloApplication) {
	$window.executeInlineScript = function(formname, script, params) {
		return $sabloApplication.callService("formService", "executeInlineScript", {'formname' : formname, 'script' : script, 'params' : params}, false)
	}
});
