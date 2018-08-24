/// <reference path="../../typings/angularjs/angular.d.ts" />
/// <reference path="../../typings/numeraljs/numeraljs.d.ts" />
/// <reference path="../../typings/defaults/window.d.ts" />
/// <reference path="../../typings/sablo/sablo.d.ts" />
/// <reference path="../../typings/jquery/jquery.d.ts" />
/// <reference path="../../typings/servoy/servoy.d.ts" />
if (typeof String.endsWith != "function"){
	String.prototype.endsWith = function (suffix) {
	      return this.indexOf(suffix, this.length - suffix.length) !== -1;
	}
}
var controllerProvider : angular.IControllerProvider;
angular.module('servoyApp', ['sabloApp', 'servoy','webStorageModule','servoy-components', 'webSocketModule','servoyWindowManager',
                             'pasvaz.bindonce', 'ngSanitize', 'pascalprecht.translate']

).config(['$controllerProvider', '$translateProvider', '$qProvider', function($controllerProvider: angular.IControllerProvider, $translateProvider, $qProvider) {
	controllerProvider = $controllerProvider;
	
	// TODO: check if this does not break some translated values
	$translateProvider.useSanitizeValueStrategy('sanitize');
	$translateProvider.preferredLanguage('servoy-i18n');
    $translateProvider.useLoader('translateFilterServoyI18Loader');
    $translateProvider.useMissingTranslationHandler('translateFilterServoyI18nMessageLoader');
    $translateProvider.forceAsyncReload(true);
    
    // added for "Possibly unhandled rejection with Angular 1.5.9" - https://github.com/angular-ui/ui-router/issues/2889
    $qProvider.errorOnUnhandledRejections(false);
	
}]).factory('$servoyInternal', function ($rootScope: angular.IRootScopeService, webStorage, $anchorConstants, $webSocket: sablo.IWebSocket, $q:angular.IQService,
		$solutionSettings:servoy.SolutionSettings, $window: angular.IWindowService, $sabloConverters:sablo.ISabloConverters,
		$sabloUtils:sablo.ISabloUtils, $sabloApplication: sablo.ISabloApplication, $utils,$foundsetTypeConstants,$log: angular.ILogService, clientdesign) {
	function getComponentChanges(now, prev, beanConversionInfo, beanLayout, parentSize, property, beanModel,useAnchoring,formname) {
		var changes = $sabloApplication.getComponentChanges(now, prev, beanConversionInfo, parentSize, property)
		// TODO: visibility must be based on properties of type visible, not on property name
		if (changes.location || changes.size || changes.visible || changes.anchors) {
			if (beanLayout) {
				applyBeanLayout(beanModel, beanLayout, changes, parentSize, false,useAnchoring,formname);
			}
		}
		return changes;
	}
	function sendChanges(now, prev, formname, beanname, property) {
		$sabloApplication.getFormStateWithData(formname).then(function (formState) {
			var beanConversionInfo = $sabloUtils.getInDepthProperty($sabloApplication.getFormStatesConversionInfo(), formname, beanname);
			var changes = getComponentChanges(now, prev, beanConversionInfo, formState.layout[beanname], formState.properties.designSize, property, formState.model[beanname], !formState.properties.useCssPosition, formname);
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
	
	function applyBeanData(beanModel, beanLayout, beanData, containerSize, changeNotifierGenerator, beanConversionInfo, newConversionInfo, componentScope,useAnchoring,formname) {
		$sabloApplication.applyBeanData(beanModel, beanData, containerSize, changeNotifierGenerator, beanConversionInfo, newConversionInfo, componentScope)
		applyBeanLayout(beanModel, beanLayout, beanData, containerSize, true,useAnchoring,formname)
	}

	function applyBeanLayout(beanModel, beanLayout, beanData, containerSize, isApplyBeanData, useAnchoring, formname) {
		if (!beanLayout) return;
		var runtimeChanges = !isApplyBeanData && ( beanData.size != undefined || beanData.location != undefined );
		//beanData.anchors means anchors changed or must be initialized
		if (!clientdesign.isFormInDesign(formname) && (beanData.anchors || runtimeChanges) && useAnchoring && containerSize && $solutionSettings.enableAnchoring) {
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
		var isAnchoredTopLeftBeanModel = clientdesign.isFormInDesign(formname) || !beanModel.anchors || (beanModel.anchors == ($anchorConstants.NORTH + $anchorConstants.WEST));
		if (useAnchoring && (isAnchoredTopLeftBeanModel || !$solutionSettings.enableAnchoring))
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

	
	var findModeShortCutAdded = false;
	function connect() {
		// maybe do this with defer ($q)
		var solName = $webSocket.getURLParameter('s');
		if (!solName) $solutionSettings.solutionName  = /.*\/([\$\w]+)\/.*/.exec($webSocket.getPathname())[1];
		else $solutionSettings.solutionName  = solName;
		$solutionSettings.windowName = $sabloApplication.getWindowId();
		var recordingPrefix;
		if ($window.location.search.indexOf("svy_record=true") > -1) {
			recordingPrefix = "/recording/websocket";
			
		}
		var wsSession = $sabloApplication.connect('/solutions/'+$solutionSettings.solutionName, {solution:$solutionSettings.solutionName}, recordingPrefix)
		wsSession.onMessageObject(function (msg, conversionInfo, scopesToDigest) {
			// data got back from the server
			for(var formname in msg.forms) {
				// current model
				var formState = $sabloApplication.getFormStateEvenIfNotYetResolved(formname);
				if (typeof(formState) == 'undefined') {
					// if the form is not yet on the client ,wait for it and then apply it
					$sabloApplication.getFormState(formname).then(getFormMessageHandler(formname, msg, conversionInfo), 
							function(err) { $log.error("Error getting form state (svy) when trying to handle msg. from server: " + err); });
				}
				else {
					getFormMessageHandler(formname, msg, conversionInfo)(formState);
					if (formState.getScope) {
						// hack: if default navigator data is coming in, rootscope should be applied 
						if (msg.forms[formname]["svy_default_navigator"]) {
							scopesToDigest.putItem($rootScope);
						}
						else {
							var s = formState.getScope();
							if (s) scopesToDigest.putItem(s);
						}
					}
				}
			}
			
			function setFindMode(beanData)
			{
				if (beanData['findmode'])
				{
					if (window.shortcut.all_shortcuts['ENTER'] === undefined)
					{
						findModeShortCutAdded = true;
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
				else if (beanData['findmode'] == false && findModeShortCutAdded)
				{
					findModeShortCutAdded = false;
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
							applyBeanLayout(beanModel, layout[beanname],beanData, formState.properties.designSize, false,!formState.properties.useCssPosition, formname)
						}
						else if (beanData['findmode'] !== undefined)
						{
							setFindMode(beanData);
						}

					}
				}
			}

			if (msg.sessionid && recordingPrefix) {
				var btn = <HTMLAnchorElement>$window.document.createElement("A");        // Create a <button> element
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
	
	function getFoundsetLinkedDPInfo(propertyName, beanModel) {
		var propertyNameForServerAndRowID;
		
		if ((propertyName.indexOf('.') > 0 || propertyName.indexOf('[') > 0) && propertyName.endsWith("]"))	{
			// TODO this is a big hack - see comment in pushDPChange below
			
			// now detect somehow if this is a foundset linked dataprovider - in which case we need to provide a rowId for it to server
			var lastIndexOfOpenBracket = propertyName.lastIndexOf('[');
			var indexInLastArray = parseInt(propertyName.substring(lastIndexOfOpenBracket + 1, propertyName.length - 1));
			var dpPathWithoutArray = propertyName.substring(0, lastIndexOfOpenBracket);
			var foundsetLinkedDPValueCandidate = eval('beanModel.' + dpPathWithoutArray);
			if (foundsetLinkedDPValueCandidate && foundsetLinkedDPValueCandidate[$sabloConverters.INTERNAL_IMPL]
					&& foundsetLinkedDPValueCandidate[$sabloConverters.INTERNAL_IMPL][$foundsetTypeConstants.FOR_FOUNDSET_PROPERTY]) {
				
				// it's very likely a foundsetLinked DP: it has the internals that that property type uses; get the corresponding rowID from the foundset property that it uses
				// strip the last index as we now identify the record via rowId and server has no use for it anyway (on server side it's just a foundsetlinked property value, not an array, so property name should not contain that last index)
				propertyNameForServerAndRowID = { propertyNameForServer: dpPathWithoutArray };

				var foundset = foundsetLinkedDPValueCandidate[$sabloConverters.INTERNAL_IMPL][$foundsetTypeConstants.FOR_FOUNDSET_PROPERTY]();
				if (foundset) {
					propertyNameForServerAndRowID.rowId = foundset.viewPort.rows[indexInLastArray][$foundsetTypeConstants.ROW_ID_COL_KEY];
				}
			}	
		}
		
		return propertyNameForServerAndRowID;
	}

	clientdesign.setDesignChangeCallback(function(formname, isDesign) {
		var formState = $sabloApplication.getFormStateEvenIfNotYetResolved(formname);
		for (var beanName in formState.model) {
			if (beanName != '') {
				formState.layout[beanName] = { position: 'absolute' }
				applyBeanLayout(formState.model[beanName], formState.layout[beanName], { anchors: isDesign ? 0 : formState.model[beanName].anchors }, formState.properties.designSize, false, true, formname)
			}
		}
	});

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
						if (formProperties.absoluteLayout[''] || formProperties.absoluteLayout[beanName]) layout[beanName] = { position: 'absolute' }

						var newBeanConversionInfo = beanDatas[beanName][$sabloConverters.TYPES_KEY];
						var beanConversionInfo = newBeanConversionInfo ? $sabloUtils.getOrCreateInDepthProperty($sabloApplication.getFormStatesConversionInfo(), formName, beanName) : $sabloUtils.getInDepthProperty($sabloApplication.getFormStatesConversionInfo(), formName, beanName);

						applyBeanData(state.model[beanName], layout[beanName], beanDatas[beanName],parentSizes && parentSizes[beanName] ? parentSizes[beanName] : formProperties.designSize, $sabloApplication.getChangeNotifierGenerator(formName, beanName), beanConversionInfo, newBeanConversionInfo, formScope,!formProperties.useCssPosition, formName)
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
						applyBeanLayout(formState.model[beanName], formState.layout[beanName], initialFormData[beanName], formState.properties.designSize, false,!formState.properties.useCssPosition, formname)
					}
				}
			});
		},

		// used by form template js
		sendChanges: sendChanges,
		
		callServerSideApi: function(formname, beanname, methodName, args) {
			// it would be nice to know here the argument and return types; for now just do default conversion (so that dates & types that use $sabloUtils.DEFAULT_CONVERSION_TO_SERVER_FUNC work correctly)
			if (args && args.length) for (var i = 0; i < args.length; i++) {
				args[i] = $sabloUtils.convertClientObject(args[i]); // TODO should be $sabloConverters.convertFromClientToServer(now, beanConversionInfo[property] ?, undefined);
			}

			return $sabloApplication.callService('formService', 'callServerSideApi', {formname:formname,beanname:beanname,methodName:methodName,args:args})
		},
		
		pushEditingStarted: function(formName, beanName, propertyName) {
			var messageForServer = { formname : formName, beanname : beanName, property : propertyName };
			
			// detect if this is a foundset linked dataprovider - in which case we need to provide a rowId for it to server and trim down the last array index which identifies the row on client
			// TODO this is a big hack - see comment in pushDPChange below
			var formState = $sabloApplication.getFormStateEvenIfNotYetResolved(formName);
			var foundsetLinkedDPInfo = getFoundsetLinkedDPInfo(propertyName, formState.model[beanName]);
			if (foundsetLinkedDPInfo)	{
				if (foundsetLinkedDPInfo.rowId) messageForServer['fslRowID'] = foundsetLinkedDPInfo.rowId;
				messageForServer.property = foundsetLinkedDPInfo.propertyNameForServer;
			}	

			$sabloApplication.callService("formService", "startEdit", messageForServer, true);
		},

		pushDPChange: function(formname, beanname, property) {
			var formState = $sabloApplication.getFormStateEvenIfNotYetResolved(formname);
			var changes = {};

			var formStatesConversionInfo = $sabloApplication.getFormStatesConversionInfo();
			var conversionInfo = (formStatesConversionInfo[formname] ? formStatesConversionInfo[formname][beanname] : undefined);
			var fslRowID = null;
			if (conversionInfo && conversionInfo[property]) {
				// I think this never happens currently
				changes[property] = $sabloConverters.convertFromClientToServer(formState.model[beanname][property], conversionInfo[property], undefined);
			} else {
				var dpValue = null;

				if (property.indexOf('.') > 0 || property.indexOf('[') > 0) {
					// TODO this is a big hack - it would be nicer in the future if we have type info for all properties on the client and move
					// internal states out of the values of the properties and into a separate locations (so that we can have internal states even for primitive dataprovider types)
					// to have DP types register themselves to the apply() and startEdit() and do the apply/startEdit completely through the property itself (send property updates);
					// then we can get rid of all the custom apply code on server as well as all this pushDPChange on client
					
					// nested property; get the value correctly
					dpValue = eval('formState.model[beanname].' + property)
					
					// now detect if this is a foundset linked dataprovider - in which case we need to provide a rowId for it to server
					var foundsetLinkedDPInfo = getFoundsetLinkedDPInfo(property, formState.model[beanname]);
					if (foundsetLinkedDPInfo)	{
						fslRowID = foundsetLinkedDPInfo.rowId;
						property = foundsetLinkedDPInfo.propertyNameForServer;
					}	
				} else {
					dpValue = formState.model[beanname][property];
				}
				
				changes[property] = $sabloUtils.convertClientObject(dpValue);
			}
			
			var dpChange = {formname: formname, beanname: beanname, property: property, changes: changes};
			if (fslRowID) {
				dpChange['fslRowID'] = fslRowID;
			}

			$sabloApplication.callService('formService', 'svyPush', dpChange, true);
		}
	}
}).factory("$formService",function($sabloApplication:sablo.ISabloApplication,$servoyInternal,$rootScope: servoy.IRootScopeService,$log:sablo.ILogService,$q:angular.IQService) {
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
				if (formState.initializing && !formState.initialDataRequested) $servoyInternal.requestInitialData(formname, formState);
			});
			if (notifyFormVisibility) {
				return $sabloApplication.callService('formService', 'formvisibility', {formname:formname,visible:true,parentForm:parentForm,bean:beanName,relation:relationname,formIndex:formIndex}, false);
			}
			// dummy promise
			return $q.when(null);
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
}).directive('svyAutosave',  function ($sabloApplication:sablo.ISabloApplication) {
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
				var height = element[0].parentNode['clientHeight'];
				if (height > imageHeight) alignStyle.top = (height - imageHeight) / 2+'px';
				// horizontal align (default left)
				var width = element[0].parentNode['clientWidth'];
				if (width > imageWidth) {
					if (horizontalAlignment == 0 /*SwingConstants.CENTER*/) alignStyle.left = (width - imageWidth) / 2+'px';
					else if (horizontalAlignment == 4 /*SwingConstants.RIGHT*/) alignStyle.left = (width - imageWidth)+'px';
					else
					{
						if ((element[0].parentNode.childNodes.length > 1) && (imageHeight + 34 < height)) alignStyle.left ='51px';
					}
				}
				element.css(alignStyle);
			});
		}
	};
}).directive('svyImagemediaid',  function ($parse:angular.IParseService,$timeout:angular.ITimeoutService,$solutionSettings:servoy.SolutionSettings,$anchorConstants,$window:angular.IWindowService) {
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

			if (scope['model'] && scope['model'].anchors && $solutionSettings.enableAnchoring) {
				if ((((scope['model'].anchors & $anchorConstants.NORTH) != 0) && ((scope['model'].anchors & $anchorConstants.SOUTH) != 0)) || (((scope['model'].anchors & $anchorConstants.EAST) != 0) && ((scope['model'].anchors & $anchorConstants.WEST) != 0)))
				{
					// anchored image, add resize listener
					var resizeTimeoutID = null;
					$window.addEventListener('resize',setImageStyle);
					scope.$on("dialogResize",setImageStyle);
				}
			}
			scope.$watch(attrs['svyImagemediaid'],function(newVal) {
				media = newVal;
				var componentSize = {width: element[0].parentNode.parentNode['offsetWidth'],height: element[0].parentNode.parentNode['offsetHeight']};
				if (componentSize.width > 0 && componentSize.height > 0 )
					angular.element(element[0]).ready(setImageStyle);
				else if (media && media.visible) {
					// TODO should this below be just a timeout or a timeout call wrapped into a function?					
//					angular.element(element[0]).ready($timeout(function(){setImageStyle();},0,false));
					$timeout(function(){setImageStyle();},0,false)
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
			while(!componentRoot.isolateScope() && componentRoot.length > 0){
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
.directive('svyLayoutUpdate', function($window:angular.IWindowService,$timeout:angular.ITimeoutService) {
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
}).directive('svyFormload',  function ($timeout:angular.ITimeoutService, $sabloApplication:sablo.ISabloApplication, $windowService, $rootScope:servoy.IRootScopeService, $log:sablo.ILogService) {
	return {
		restrict: 'E',
		compile: function(tElem, tAttrs){
			var formName = tAttrs['formname'];
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
						if (formState.getScope) formState.getScope()['hiddenDivFormDiscarded'] = true; // skip altering form state on hidden form scope destroy (as destroy might happen after the other place loads the form); new load will soon resolve the form again if it hasn't already at that time
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
				post: function(scope:angular.IScope, element:JQuery, attrs:angular.IAttributes) {
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
}).factory('translateFilterServoyI18Loader', ['$q', function ($q:angular.IQService) {
   return function (options) {
       // return empty translation, translateFilterServoyI18nMessageLoader is used as missingTranslationHandler
       return $q.when({});
    };
}]).factory('translateFilterServoyI18nMessageLoader', ['$svyI18NService', function ($svyI18NService:servoy.IServoyI18NService) {
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
}).controller("MainController", function($scope:servoy.IMainControllerScope, $solutionSettings:servoy.SolutionSettings, $servoyInternal, $windowService:servoy.IWindowService, $rootScope:angular.IRootScopeService, webStorage, $sabloApplication:sablo.ISabloApplication, $applicationService) {
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

}).controller("NoLicenseController",['$scope','$solutionSettings','$timeout','$window' ,function($scope, $solutionSettings:servoy.SolutionSettings,$timeout:angular.ITimeoutService,$window:angular.IWindowService) {

	$scope.redirectUrl = $solutionSettings.noLicense.redirectUrl;

	if($solutionSettings.noLicense.redirectTimeout >=0){
		$timeout(function(){			
			$window.location.assign($solutionSettings.noLicense.redirectUrl);
		},$solutionSettings.noLicense.redirectTimeout*1000)
	}
}]).controller("SessionExpiredController",['$scope','$solutionSettings',function($scope, $solutionSettings:servoy.SolutionSettings) {

	$scope.redirectUrl = $solutionSettings.sessionExpired.redirectUrl;

}])
.controller("InternalServerErrorController",['$scope','$solutionSettings',function($scope, $solutionSettings:servoy.SolutionSettings) {

	$scope.error = $solutionSettings.internalServerError

}])
.controller("MaintenanceModeController",['$scope','$solutionSettings','$timeout','$window' ,function($scope, $solutionSettings:servoy.SolutionSettings,$timeout:angular.ITimeoutService,$window:angular.IWindowService) {

	$scope.redirectUrl = $solutionSettings.maintenanceMode.redirectUrl;

	if($solutionSettings.maintenanceMode.redirectTimeout >=0){
		$timeout(function(){			
			$window.location.assign($solutionSettings.maintenanceMode.redirectUrl);
		},$solutionSettings.maintenanceMode.redirectTimeout*1000)
	}
}])
.controller("LoginController", function($scope, $uibModalInstance, $sabloApplication:sablo.ISabloApplication, webStorage) {
	$scope.model = {'remember' : true };
	$scope.doLogin = function() {
		var promise = $sabloApplication.callService<{username:string,password:string}>("applicationServerService", "login", {'username' : $scope.model.username, 'password' : $scope.model.password, 'remember': $scope.model.remember}, false);
		promise.then(function(ok) {
			if(ok) {
				if(ok.username) webStorage.local.add('servoy_username', ok.username);
				if(ok.password) webStorage.local.add('servoy_password', ok.password);
				$uibModalInstance.close(ok);
			} else {
				$scope.model.message = 'Invalid username or password, try again';
			}
		})
	}	
})
.factory('$sessionService',['$solutionSettings','$window','$rootScope','$servoyWindowManager',function($solutionSettings:servoy.SolutionSettings,$window:angular.IWindowService,$rootScope:angular.IRootScopeService,$servoyWindowManager){

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
			if(maintenanceMode.viewUrl) ment.viewUrl = maintenanceMode.viewUrl 
			if(maintenanceMode.redirectUrl)	ment.redirectUrl = maintenanceMode.redirectUrl;
			if(maintenanceMode.redirectTimeout)	ment.redirectTimeout = maintenanceMode.redirectTimeout;

			$solutionSettings.maintenanceMode = ment;
			if (!$rootScope.$$phase) $rootScope.$digest();
		},
		setInternalServerError: function(internalServerError){
			var error = {viewUrl:'templates/serverInternalErrorView.html'}
			if(internalServerError.viewUrl)  error.viewUrl = internalServerError.viewUrl;
			if(internalServerError.stack) error['stack'] = internalServerError.stack;

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
}]).factory("$applicationService",['$window','$timeout','webStorage','$uibModal','$sabloApplication','$solutionSettings','$rootScope','$svyFileuploadUtils','$locale','$svyI18NService','$log','$translate', '$svyUIProperties',
                           function($window:angular.IWindowService,$timeout:angular.ITimeoutService,webStorage,$uibModal,$sabloApplication:sablo.ISabloApplication,$solutionSettings:servoy.SolutionSettings,$rootScope:angular.IRootScopeService,$svyFileuploadUtils,$locale,$svyI18NService:servoy.IServoyI18NService,$log:sablo.ILogService,$translate,$svyUIProperties) {
	var showDefaultLoginWindow = function() {
		$uibModal.open({
			templateUrl: 'templates/login.html',
			controller: 'LoginController',
			windowClass: 'login-window',
			backdrop: 'static',
			keyboard: false
		});				
	}
	
	var userProperties;
	function getUserProperties() {
		if (!angular.isDefined(userProperties)) {
			var json = webStorage.local.get("userProperties");
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
		
		return $svyUIProperties.getUIProperty("trustDataAsHtml");
	}
	
	function getServerURL() {
		// current remote address including the context (includes leading /)
		var context = $window.document.getElementsByTagName ("base")[0].getAttribute("href");
		return $window.location.protocol + '//'+ $window.location.host + context;
	}
	
	return {
		setStyleSheets: function(paths) {
			$solutionSettings.styleSheetPaths = paths;
			if (!$rootScope.$$phase) {
				if ($log.debugEnabled && $log.debugLevel === $log.SPAM) $log.debug("svy * Will call digest from setStyleSheets for root scope");
				$rootScope.$digest();
			}
		},
		getUserProperties: function(){
			return getUserProperties();	
		},
		setUserProperty: function(key,value) {
			var userProps = getUserProperties();
			if (value == null) delete userProps[key];
			else userProps[key] = value;
			webStorage.local.add("userProperties", JSON.stringify(userProps))
		},
		getUIProperty: function(key) {
			return $svyUIProperties.getUIProperty(key);
		},
		setUIProperty: function(key,value) {
			$svyUIProperties.setUIProperty(key, value);
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
                        ifrm.setAttribute("src", url);
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
		getUserAgentAndPlatform:function() {
			return {userAgent:$window.navigator.userAgent,platform:$window.navigator.platform};
		},
		getClientBrowserInformation:function() {
			var locale = $sabloApplication.getLocale();
			this.setAngularLocale(locale.language);
			var userAgent = this.getUserAgentAndPlatform();
			return {
				serverURL: getServerURL(),
				userAgent : userAgent.userAgent,
				platform : userAgent.platform,
				locale : locale.full,
				remote_ipaddress : window.servoy_remoteaddr,
				remote_host : window.servoy_remotehost,
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
				numeral.localeData((language + '-' + country).toLowerCase());
				numeral.locale((language + '-' + country).toLowerCase());
				if (!initializing) webStorage.session.add("locale", (language + '-' + country).toLowerCase());
			} catch(e) {
				try {
					numeral.localeData(language + '-' + country);
					numeral.locale(language + '-' + country);
					if (!initializing) webStorage.session.add("locale", language + '-' + country);
				} catch(e2) {
					try {
						//try it with just the language part
						numeral.localeData(language);
						numeral.locale(language);
						if (!initializing) webStorage.session.add("locale", language);
					} catch(e3) {
						try {
							//try it with just the language part but lowercase
							numeral.localeData(language.toLowerCase());
							numeral.locale(language.toLowerCase());
							if (!initializing) webStorage.session.add("locale", language);
						} catch(e4) {
							try {
								//try to duplicate the language in case it's only defined like that
								numeral.localeData(language.toLowerCase() + "-" + language.toLowerCase()); // nl-nl for example is defined but browser only says 'nl' (this won't work for all languages for example "en-en" I don't think even exists)
								numeral.locale(language.toLowerCase() + "-" + language.toLowerCase()); 
								if (!initializing) webStorage.session.add("locale", language);
							} catch(e5) {
								// we can't find a suitable locale defined in locales.js; get the needed things from server (Java knows more locales)
								// and create the locate info from that
								var promise = $sabloApplication.callService<NumeralJSLocale>("i18nService", "generateLocaleForNumeralJS", country ? {'language' : language, 'country' : country} : {'language' : language}, false);
								// TODO should we always do this (get stuff from server side java) instead of trying first to rely on numeral.js and locales.js provided langs?
								var numeralLanguage = language + (country ? '-' + country : "");
								promise.then(function(numeralLocaleInfo) {
									if ($log.debugEnabled) $log.debug("Locale '" + numeralLanguage + "' not found in client js lib, but it was constructed based on server Java locale-specific information: " + JSON.stringify(numeralLocaleInfo));
									numeralLocaleInfo.ordinal = function (number) {
										return ".";
									};
									numeral.register('locale',numeralLanguage,numeralLocaleInfo);
									numeral.locale(numeralLanguage);
									if (!initializing) {
										webStorage.session.add("locale", numeralLanguage);
										$sabloApplication.setLocale({ language : language, country : country , full: language + "-" + country});
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
				var array = lang.split('-');
				$sabloApplication.setLocale({ language : array[0], country : (array[1] ?  array[1] : country), full: (array[1] ?  lang :  (array[0] +"-"+country))});
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
		clearDefaultLoginCredentials: function() {
			webStorage.local.remove('servoy_username');
			webStorage.local.remove('servoy_password');
		},
		showFileOpenDialog: function(title, multiselect, acceptFilter) {
			$svyFileuploadUtils.open("resources/upload/" + $sabloApplication.getSessionId(), title, multiselect, acceptFilter);
		},
		getSolutionName: function() {
			return $solutionSettings.solutionName;
		},
		trustAsHtml: trustAsHtml
	}

}]).factory("clientdesign",['$window','$sabloApplication','$utils','$timeout', function($window,$sabloApplication,$utils,$timeout) 
{
	if (typeof addEvent != 'function')
	{
	 var addEvent = function(o, t, f, l)
	 {
	  var d = 'addEventListener', n = 'on' + t, rO = o, rT = t, rF = f, rL = l;
	  if (o[d] && !l) return o[d](t, f, false);
	  if (!o._evts) o._evts = {};
	  if (!o._evts[t])
	  {
	   o._evts[t] = o[n] ? { b: o[n] } : {};
	   o[n] = new Function('e',
	    'var r = true, o = this, a = o._evts["' + t + '"], i; for (i in a) {' +
	     'o._f = a[i]; r = o._f(e||window.event) != false && r; o._f = null;' +
	     '} return r');
	   if (t != 'unload') addEvent(window, 'unload', function() {
	    removeEvent(rO, rT, rF, rL);
	   },null);
	  }
	  if (!f._i) f._i = addEvent['_i']++;
	  o._evts[t][f._i] = f;
	 };
	 addEvent['_i'] = 1;
	 var removeEvent = function(o, t, f, l)
	 {
	  var d = 'removeEventListener';
	  if (o[d] && !l) return o[d](t, f, false);
	  if (o._evts && o._evts[t] && f._i) delete o._evts[t][f._i];
	 };
	}


	function cancelEvent(e, c)
	{
	 e.returnValue = false;
	 if (e.preventDefault) e.preventDefault();
	 if (c)
	 {
	  e.cancelBubble = true;
	  if (e.stopPropagation) e.stopPropagation();
	 }
	};

	// *** DRAG/RESIZE CODE ***
	function DragResize(myName, config)
	{
	 var props = {
	  myName: myName,                  // Name of the object.
	  enabled: true,                   // Global toggle of drag/resize.
	  handles: ['tl', 'tm', 'tr',
	   'ml', 'mr', 'bl', 'bm', 'br'], // Array of drag handles: top/mid/bot/right.
	  isElement: null,                 // Function ref to test for an element.
	  isHandle: null,                  // Function ref to test for move handle.
	  element: null,                   // The currently selected element.
	  handle: null,                  // Active handle reference of the element.
	  minWidth: 10, minHeight: 10,     // Minimum pixel size of elements.
	  minLeft: 0, maxLeft: 9999,       // Bounding box area, in pixels.
	  minTop: 0, maxTop: 9999,
	  zIndex: 1,                       // The highest Z-Index yet allocated.
	  mouseX: 0, mouseY: 0,            // Current mouse position, recorded live.
	  lastMouseX: 0, lastMouseY: 0,    // Last processed mouse positions.
	  mOffX: 0, mOffY: 0,              // A known offset between position & mouse.
	  elmX: 0, elmY: 0,                // Element position.
	  elmW: 0, elmH: 0,                // Element size.
	  allowBlur: true,                 // Whether to allow automatic blur onclick.
	  ondragfocus: null,               // Event handler functions.
	  ondragstart: null,
	  ondragmove: null,
	  ondragend: null,
	  ondragblur: null,
	  ondoubleclick: null,
	  onrightclick: null
	 };

	 for (var p in props)
	  this[p] = (typeof config[p] == 'undefined') ? props[p] : config[p];
	};


	DragResize.prototype.apply = function(node)
	{
	 // Adds object event handlers to the specified DOM node.

	 var obj = this;
	 this.node = node;
     this.mouseDownHandler =  function(e) { obj.mouseDown(e) };
     this.mouseMoveHandler = function(e) { obj.mouseMove(e) };
     this.mouseUpHandler = function(e) { obj.mouseUp(e) };
     this.doubleClickHandler = function(e) { obj.doubleClick(e) };
     this.rightClickHandler = function(e) { obj.rightClick(e) };
     addEvent(this.node, 'mousedown',this.mouseDownHandler,null );
     addEvent(this.node, 'mousemove', this.mouseMoveHandler,null );
     addEvent(this.node, 'mouseup',this.mouseUpHandler ,null );
     addEvent(this.node, 'dblclick',this.doubleClickHandler ,null );
     addEvent(this.node, 'contextmenu',this.rightClickHandler ,null );
	};
	
	DragResize.prototype.destroy = function()
    {
     removeEvent(this.node, 'mousedown',this.mouseDownHandler,null );
     removeEvent(this.node, 'mousemove', this.mouseMoveHandler,null );
     removeEvent(this.node, 'mouseup',this.mouseUpHandler ,null );
     removeEvent(this.node, 'dblclick',this.doubleClickHandler ,null );
     removeEvent(this.node, 'contextmenu',this.rightClickHandler ,null );
     this.deselect(true);
     this.enabled = false;
    };


	DragResize.prototype.select = function(newElement, newHandle,e) { 
	 // Selects an element for dragging.

	 if (!document.getElementById || !this.enabled) return;

	 // Activate and record our new dragging element.
	 if (newElement && (newElement != this.element) && this.enabled)
	 {
		 this. element = newElement;
	  // Elevate it and give it resize handles.
		 this.element.style.zIndex = ++this.zIndex;
	  // Record element attributes for mouseMove().
	  this.elmX = parseInt(this.element.style.left);
	  this.elmY = parseInt(this.element.style.top);
	  this.elmW = this.element.offsetWidth;
	  this.elmH = this.element.offsetHeight;
	  if (this.ondragfocus) this.ondragfocus(e);
	 }
	};


	DragResize.prototype.deselect = function(delHandles) { 
	 // Immediately stops dragging an element. If 'delHandles' is true, this
	 // remove the handles from the element and clears the element flag,
	 // completely resetting the .

	 if (!document.getElementById || !this.enabled) return;

	 if (delHandles && this.element)
	 {
	  if (this.ondragblur) this.ondragblur();
	  if (this.resizeHandleSet) this.resizeHandleSet(this.element, false);
	  this.element = null;
	 }

	this. handle = null;
	this. mOffX = 0;
	this. mOffY = 0;
	this.startDragging = false;
	};


	DragResize.prototype.mouseDown = function(e) { 
	 // Suitable elements are selected for drag/resize on mousedown.
	 // We also initialise the resize boxes, and drag parameters like mouse position etc.
	 if (!document.getElementById || !this.enabled) return true;

	 var elm = e.target || e.srcElement,
	  newElement = null,
	  newHandle = null,
	  hRE = new RegExp(this.myName + '-([trmbl]{2})', '');

	 while (elm)
	 {
	  // Loop up the DOM looking for matching elements. Remember one if found.
	  if (!newHandle && (hRE.test(elm.className) || this.isHandle(elm))) newHandle = elm;
	  if (this.isElement(elm)) { newElement = elm; break }
	  elm = elm.parentNode;
	  if (elm == this.node) break;
	 }

	 // If this isn't on the last dragged element, call deselect(),
	 // which will hide its handles and clear element.
	 if (this.element && (this.element != newElement) && this.allowBlur) this.deselect(true);

	 // If we have a new matching element, call select().
	 if (newElement && (!this.element || (newElement == this.element)))
	 {
	  // Stop mouse selections if we're dragging a handle.
	  if (newHandle) cancelEvent(e,null);
	  this.select(newElement, newHandle,e);
	  this.handle = newHandle;
	 }
	};


	DragResize.prototype.mouseMove = function(e) {
	 // This continually offsets the dragged element by the difference between the
	 // last recorded mouse position (mouseX/Y) and the current mouse position.
	 if (!document.getElementById || !this.enabled) return true;

	 // We always record the current mouse position.
	 this.mouseX = e.pageX || e.clientX + document.documentElement.scrollLeft;
	 this.mouseY = e.pageY || e.clientY + document.documentElement.scrollTop;
	 // Record the relative mouse movement, in case we're dragging.
	 // Add any previously stored & ignored offset to the calculations.
	 var diffX = this.mouseX - this.lastMouseX + this.mOffX;
	 var diffY = this.mouseY - this.lastMouseY + this.mOffY;
	 this.mOffX = this.mOffY = 0;
	 // Update last processed mouse positions.
	 this.lastMouseX = this.mouseX;
	 this.lastMouseY = this.mouseY;

	 // That's all we do if we're not dragging anything.
	 if (!this.handle) return true;

	 // If included in the script, run the resize handle drag routine.
	 // Let it create an object representing the drag offsets.
	 var isResize = false;
	 if (this.resizeHandleDrag && this.resizeHandleDrag(diffX, diffY))
	 {
	  isResize = true;
	  this.startDragging = true;
	 }
	 else
	 {
		  if (!this.startDragging && this.handle && this.ondragstart && (diffX != 0 || diffY != 0)) {
			  this.ondragstart(e);
			  this.startDragging = true;
		  }

	  // If the resize drag handler isn't set or returns fase (to indicate the drag was
	  // not on a resize handle), we must be dragging the whole element, so move that.
	  // Bounds check left-right...
	  var dX = diffX, dY = diffY;
	  if (this.elmX + dX < this.minLeft) this.mOffX = (dX - (diffX = this.minLeft - this.elmX));
	  else if (this.elmX + this.elmW + dX > this.maxLeft) this.mOffX = (dX - (diffX = this.maxLeft - this.elmX - this.elmW));
	  // ...and up-down.
	  if (this.elmY + dY < this.minTop) this.mOffY = (dY - (diffY = this.minTop - this.elmY));
	  else if (this.elmY + this.elmH + dY > this.maxTop) this.mOffY = (dY - (diffY = this.maxTop - this.elmY - this.elmH));
	  this.elmX += diffX;
	  this.elmY += diffY;
	 }

	 // Assign new info back to the element, with minimum dimensions.
	this.element.style.left =   this.elmX + 'px';
	this.element.style.width =  this.elmW + 'px';
	this.element.style.top =    this.elmY + 'px';
	this.element.style.height = this.elmH + 'px';

	 // Evil, dirty, hackish Opera select-as-you-drag fix.
	 if (window['opera'] && document.documentElement)
	 {
	  var oDF = document.getElementById('op-drag-fix');
	  if (!oDF)
	  {
	   oDF = document.createElement('input');
	   oDF.id = 'op-drag-fix';
	   oDF.style.display = 'none';
	   document.body.appendChild(oDF);
	  }
	  oDF.focus();
	 }

	 if (this.ondragmove) this.ondragmove(isResize);

	 // Stop a normal drag event.
	 cancelEvent(e,null);
	};


	DragResize.prototype.mouseUp = function(e) { 
	 // On mouseup, stop dragging, but don't reset handler visibility.
	 if (!document.getElementById || !this.enabled) return;

	 var hRE = new RegExp(this.myName + '-([trmbl]{2})', '');
	 if (this.startDragging && this.handle && this.ondragend) this.ondragend(hRE.test(this.handle.className),e);
	 this.deselect(false);
	 
	};
	
	DragResize.prototype.rightClick = function(e) {
		if (this.onrightclick) this.onrightclick(e); 
	}
	
	DragResize.prototype.doubleClick = function(e) {
		if (this.ondoubleclick) this.ondoubleclick(e);
	}



	/* Resize Code -- can be deleted if you're not using it. */

	DragResize.prototype.resizeHandleSet = function(elm, show) { 
	 // Either creates, shows or hides the resize handles within an element.

	 // If we're showing them, and no handles have been created, create 4 new ones.
	 if (!elm._handle_tr)
	 {
	  for (var h = 0; h < this.handles.length; h++)
	  {
	   // Create 4 news divs, assign each a generic + specific class.
	   var hDiv = document.createElement('div');
	   hDiv.className = this.myName + ' ' +  this.myName + '-' + this.handles[h];
	   elm['_handle_' + this.handles[h]] = elm.appendChild(hDiv);
	  }
	 }

	 // We now have handles. Find them all and show/hide.
	 for (var h = 0; h < this.handles.length; h++)
	 {
	  elm['_handle_' + this.handles[h]].style.visibility = show ? 'inherit' : 'hidden';
	 }
	};


	DragResize.prototype.resizeHandleDrag = function(diffX, diffY) { 
	 // Passed the mouse movement amounts. This function checks to see whether the
	 // drag is from a resize handle created above; if so, it changes the stored
	 // elm* dimensions and mOffX/Y.

	 var hClass = this.handle && this.handle.className &&
	 this.handle.className.match(new RegExp(this.myName + '-([tmblr]{2})')) ? RegExp.$1 : '';

	 // If the hClass is one of the resize handles, resize one or two dimensions.
	 // Bounds checking is the hard bit -- basically for each edge, check that the
	 // element doesn't go under minimum size, and doesn't go beyond its boundary.
	 var dY = diffY, dX = diffX, processed = false;
	 if (hClass.indexOf('t') >= 0)
	 {
		 this.rs = 1;
	  if (this.elmH - dY < this.minHeight) this.mOffY = (dY - (diffY = this.elmH - this.minHeight));
	  else if (this.elmY + dY < this.minTop) this.mOffY = (dY - (diffY = this.minTop - this.elmY));
	  this.elmY += diffY;
	  this.elmH -= diffY;
	  processed = true;
	 }
	 if (hClass.indexOf('b') >= 0)
	 {
		 this.rs = 1;
	  if (this.elmH + dY < this.minHeight) this.mOffY = (dY - (diffY = this.minHeight - this.elmH));
	  else if (this.elmY + this.elmH + dY > this.maxTop) this.mOffY = (dY - (diffY = this.maxTop - this.elmY - this.elmH));
	  this.elmH += diffY;
	  processed = true;
	 }
	 if (hClass.indexOf('l') >= 0)
	 {
		 this.rs = 1;
	  if (this.elmW - dX < this.minWidth) this.mOffX = (dX - (diffX = this.elmW - this.minWidth));
	  else if (this.elmX + dX < this.minLeft) this.mOffX = (dX - (diffX = this.minLeft - this.elmX));
	  this.elmX += diffX;
	  this.elmW -= diffX;
	  processed = true;
	 }
	 if (hClass.indexOf('r') >= 0)
	 {
		 this.rs = 1;
	  if (this.elmW + dX < this.minWidth) this.mOffX = (dX - (diffX = this.minWidth - this.elmW));
	  else if (this.elmX + this.elmW + dX > this.maxLeft) this.mOffX = (dX - (diffX = this.maxLeft - this.elmX - this.elmW));
	  this.elmW += diffX;
	  processed = true;
	 }

	 return processed;
	};
	var currentForms = {};
	var designChangeListener;
	return {
		setFormInDesign: function(formname,names) {
		    var dragresize = currentForms[formname];
		    if (dragresize) return true;
		    
			var x = $("div[ng-controller='" + formname+ "']");
			
			if (!x[0]) return false;
			
			dragresize = new DragResize('dragresize',{});
			currentForms[formname] = dragresize;
			if(designChangeListener) designChangeListener(formname, true);
			var selectElement = function(elm)
			{
			 var x = $(elm).attr("ng-style");
			 if (x && x.substring(0,7) == "layout.") {
				 var name = x.substr(7);
				 return names.indexOf(name) != -1;
			 }
			 return false;
			};
			dragresize.isElement = selectElement;
			dragresize.isHandle = selectElement;
			dragresize.ondragfocus = function(e) {
				var jsevent = $utils.createJSEvent(e,"ondrag");
				$sabloApplication.callService("clientdesign", "onselect",{element:$(dragresize.element).attr("ng-style"), formname:formname,event:jsevent} ).then(function(result) {
					if (!result) dragresize.deselect(true);
					else if (dragresize.resizeHandleSet) dragresize.resizeHandleSet(dragresize.element, true);
				});
			};
			dragresize.ondragend = function(isResize,e) {
				var jsevent = $utils.createJSEvent(e,"ondrop");
				if (isResize) $sabloApplication.callService("clientdesign", "onresize",{
					element:$(dragresize.element).attr("ng-style"),
					location: { x:$(dragresize.element).position().left, y:$(dragresize.element).position().top },
					size: { width:$(dragresize.element).outerWidth(), height:$(dragresize.element).outerHeight() },
					formname:formname,
					event:jsevent} )
				else $sabloApplication.callService("clientdesign", "ondrop",{
					element:$(dragresize.element).attr("ng-style"),
					location: { x:$(dragresize.element).position().left, y:$(dragresize.element).position().top },
					size: { width:$(dragresize.element).outerWidth(), height:$(dragresize.element).outerHeight() },
					formname:formname,
					event:jsevent} )
			};
			dragresize.ondragstart = function(e) {
				var jsevent = $utils.createJSEvent(e,"ondrag");
				$sabloApplication.callService("clientdesign", "ondrag",{element:$(dragresize.element).attr("ng-style"), formname:formname,event:jsevent} )
			};
			dragresize.ondoubleclick = function(e) {
				var jsevent = $utils.createJSEvent(e,"ondoubleclick");
				$sabloApplication.callService("clientdesign", "ondoubleclick",{element:$(dragresize.element).attr("ng-style"), formname:formname,event:jsevent} )
			};
			dragresize.onrightclick = function(e) {
				var jsevent = $utils.createJSEvent(e,"onrightclick");
				$sabloApplication.callService("clientdesign", "onrightclick",{element:$(dragresize.element).attr("ng-style"), formname:formname,event:jsevent} )
			};

			dragresize.apply(x[0]);
			return true; 
		},
		removeFormDesign: function(formname) {
		    var dragresize = currentForms[formname];
		    if (dragresize) {
		        currentForms[formname].destroy();
				delete  currentForms[formname];
				if(designChangeListener) designChangeListener(formname, false);
		        return true;
		    }
		    return false;
		},
		isFormInDesign: function(formname) {
			return formname && (currentForms[formname] != null);
		},
		setDesignChangeCallback: function(designChangeListenerCallback) {
			designChangeListener = designChangeListenerCallback;	
		},
		recreateUI: function(formname,names) {
		    var dragresize = currentForms[formname];
		    if (dragresize) {
		        currentForms[formname].destroy();
		        delete  currentForms[formname];
		        var self = this;
		        // recreate ui of the actual form must be waited on.
		        $timeout(function(){
		        	self.setFormInDesign(formname,names);
		        });
		    }
		}
	}
}])
.factory("$uiBlocker", function($services, $applicationService) {
	var executingEvents = [];
	return {
		shouldBlockDuplicateEvents: function(beanName, model, eventType, row) {
			var blockDuplicates = null;
			if (model && model.clientProperty &&  angular.isDefined(model.clientProperty.ngBlockDuplicateEvents))
			{
				blockDuplicates = model.clientProperty.ngBlockDuplicateEvents
			}
			else
			{
				blockDuplicates = $applicationService.getUIProperty("ngBlockDuplicateEvents");
			}
			if (blockDuplicates && beanName && eventType)
			{
				for (var i=0;i < executingEvents.length; i++)
				{
					if (executingEvents[i].beanName === beanName && executingEvents[i].eventType === eventType && executingEvents[i].rowId === row)
					{
						return true;
					}
				}
			}
			executingEvents[executingEvents.length] = {'beanName': beanName, 'eventType': eventType,'rowId': row};
			return false;
			
		},
		
		eventExecuted: function(beanName, model, eventType, row) {
			for (var i = 0; i < executingEvents.length; i++)
			{
				if (executingEvents[i].beanName === beanName && executingEvents[i].eventType === eventType && executingEvents[i].rowId === row)
				{
					executingEvents.splice(i,1);
					break;
				}
			}
		}
	}
})
.run(function($window, $sabloApplication:sablo.ISabloApplication) {
	$window.executeInlineScript = function(formname, script, params) {
		return $sabloApplication.callService("formService", "executeInlineScript", {'formname' : formname, 'script' : script, 'params' : params}, false)
	}
});
