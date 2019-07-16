/// <reference path="../../typings/angularjs/angular.d.ts" />
/// <reference path="../../typings/numeraljs/numeraljs.d.ts" />
/// <reference path="../../typings/defaults/window.d.ts" />
/// <reference path="../../typings/sablo/sablo.d.ts" />
/// <reference path="../../typings/sablo/sablo_app.d.ts" />
/// <reference path="../../typings/servoy/servoy.d.ts" />
/// <reference path="../../typings/servoy/component.d.ts" />
/// <reference path="../../typings/servoy/foundset.d.ts" />

angular.module('servoy',['sabloApp','servoyformat','servoytooltip','servoyfileupload','servoyalltemplates','ui.bootstrap'])
.config(["$provide", function ($provide) {
	var decorator = function($delegate, $injector) {
		// this call can modify "args" (it converts them to be sent to server)
		$delegate.callServerSideApi = function(serviceName, methodName, args) {
			// it would be nice to know here the argument and return types; for now just do default conversion (so that dates & types that use $sabloUtils.DEFAULT_CONVERSION_TO_SERVER_FUNC work correctly)
			if (args && args.length) for (var i = 0; i < args.length; i++) {
				args[i] = $injector.get("$sabloUtils").convertClientObject(args[i]); // TODO should be $sabloConverters.convertFromClientToServer(now, beanConversionInfo[property] ?, undefined);
			}
        	return $injector.get("$sabloApplication").callService('applicationServerService', 'callServerSideApi', {service:serviceName,methodName:methodName,args:args})
        };
        return $delegate;
    };
    $provide.decorator("$services", decorator);
}])
.value("$anchorConstants", {
	NORTH : 1,
	EAST : 2,
	SOUTH : 4,
	WEST : 8
}).value("$scrollbarConstants", {
	SCROLLBARS_WHEN_NEEDED : 0,
	VERTICAL_SCROLLBAR_AS_NEEDED : 1,
	VERTICAL_SCROLLBAR_ALWAYS : 2,
	VERTICAL_SCROLLBAR_NEVER : 4,
	HORIZONTAL_SCROLLBAR_AS_NEEDED : 8,
	HORIZONTAL_SCROLLBAR_ALWAYS : 16,
	HORIZONTAL_SCROLLBAR_NEVER : 32
}).value("$clientPropertyConstants", {
	WINDOW_BRANDING_ICON_32 : "window.branding.icon.32",
	WINDOW_BRANDING_ICON_192 : "window.branding.icon.192"
}).factory("$utils", function($rootScope: angular.IRootScopeService, $timeout: angular.ITimeoutService, $svyProperties: servoy.IServoyProperties, $sabloApplication: sablo.ISabloApplication) {

	// internal function
	function getPropByStringPath(o, s) {
		s = s.replace(/\[(\w+)\]/g, '.$1'); // convert indexes to properties
		s = s.replace(/^\./, '');           // strip a leading dot
		var a = s.split('.');
		while (a.length) {
			var n = a.shift();
			if (n in o) {
				o = o[n];
			} else {
				return;
			}
			return o;
		}
	}

	function testKeyPressed(e, keyCode) 
	{
		var code;

		if (!e) e = window.event;
		if (!e) return false;
		if (e.keyCode) code = e.keyCode;
		else if (e.which) code = e.which;
		return code==keyCode;
	}

	// expression for angular scope.$watch that can watch 1 item multiple levels deep in an object
	function getInDepthWatchExpression(parentObj, propertyNameOrArrayOfNestedNamesOrFuncs) {
		var expression;
		if ($.isArray(propertyNameOrArrayOfNestedNamesOrFuncs)) {
			expression = function() {
				var r = parentObj;
				var i = 0;
				while (i < propertyNameOrArrayOfNestedNamesOrFuncs.length && angular.isDefined(r)) {
					var locator = propertyNameOrArrayOfNestedNamesOrFuncs[i];
					if (typeof locator == "function") locator = locator();
					r = r[locator];
					i++;
				}
				return r;
			}
		}
		else expression = function() { return parentObj[propertyNameOrArrayOfNestedNamesOrFuncs] };

		return expression;
	};

	function getInDepthSetter(parentObj, propertyNameOrArrayOfNestedNamesOrFuncs) {
		var setterFunc;
		if ($.isArray(propertyNameOrArrayOfNestedNamesOrFuncs)) {
			setterFunc = function(newValue) {
				var r = parentObj;
				var i = 0;
				while (i < propertyNameOrArrayOfNestedNamesOrFuncs.length - 1 && angular.isDefined(r)) {
					var locator = propertyNameOrArrayOfNestedNamesOrFuncs[i];
					if (typeof locator == "function") locator = locator();

					r = r[locator];
					i++;
				}
				if (angular.isDefined(r)) {
					var locator = propertyNameOrArrayOfNestedNamesOrFuncs[propertyNameOrArrayOfNestedNamesOrFuncs.length - 1];
					if (typeof locator == "function") locator = locator();

					r[locator] = newValue;
				}
				// else auto-create path?
			}
		}
		else setterFunc = function(newValue) { parentObj[propertyNameOrArrayOfNestedNamesOrFuncs] = newValue };

		return setterFunc;
	};

	function findAttribute(element, parent, attributeName) {
		var correctScope = parent;
		var attrValue = element.attr(attributeName);
		if (! attrValue) {
			var parentEl = element.parents("[" + attributeName + "]").first(); 
			if (parentEl) {
				attrValue = parentEl.attr(attributeName);
				while (parentEl && !parentEl.scope()) parentEl = parentEl.parent();
				if (parentEl) correctScope = parentEl.scope();
			}
		}
		if (attrValue) {
			return correctScope.$eval(attrValue);
		}
	};
	
	return <servoy.IUtils> {

		/** this function can be used in filters .It accepts a string jsonpath the property to test for null. 
    	Example: "item in  model.valuelistID  | filter:notNullOrEmpty('realValue')"*/
		notNullOrEmpty : function (propPath){
			return function(item) {
				var propByStringPath = getPropByStringPath(item,propPath); 
				return !(propByStringPath === null || propByStringPath === '')
			}
		},
		autoApplyStyle: function(scope,element,modelToWatch,cssPropertyName){
			scope.$watch(modelToWatch,function(newVal,oldVal){
				$svyProperties.setCssProperty(element,cssPropertyName,newVal);
			})
		},
		
		
		getEventHandler: function($parse,scope,svyEventHandler)
		{
			var functionReferenceString = svyEventHandler;
			if (functionReferenceString)
			{
				var index = functionReferenceString.indexOf('(');
				if (index != -1) functionReferenceString = functionReferenceString.substring(0,index);
				if( scope.$eval(functionReferenceString) ) {
					return $parse(svyEventHandler);
				}
			}
			return null;
		},
		attachEventHandler: function($parse,element,scope,svyEventHandler,domEvent, filterFunction,timeout,returnFalse, doSvyApply, dataproviderString, preHandlerCallback) {
			var fn = this.getEventHandler($parse,scope,svyEventHandler)
			if (fn)
			{
				element.on(domEvent, function(event) {
					if (!filterFunction || filterFunction(event)) {
						
						function executeHandler(){
							if(preHandlerCallback) {
								preHandlerCallback();
							}
							//svyApply before calling the handler
							if(doSvyApply && dataproviderString)
							{
								var index = dataproviderString.indexOf('.');
								if (index > 0) {
									var propertyname = dataproviderString.substring(index+1);
									var svyServoyApi = findAttribute(element, scope.$parent, "svy-servoyApi");
									if (svyServoyApi && svyServoyApi.apply) {
										svyServoyApi.apply(propertyname);
									}
								}
							}
							fn(scope, {$event:event});
						};
						// always use timeout or evalAsync because this event could be triggered by a angular call (requestFocus) thats already in a digest cycle.
						if (!timeout) 
							scope.$evalAsync(executeHandler);
						else
							$timeout(executeHandler,timeout);
						
						if (returnFalse) return false;
						return true;
					}
				}); 
			}
		},
		testEnterKey: function(e) 
		{
			return testKeyPressed(e,13);
		},
		
		bindTwoWayObjectProperty: function(a, propertyNameA: string, b, propertyNameB: string, useObjectEquality: boolean, scope: angular.IScope): [ () => void, () => void] {
			var toWatchA = getInDepthWatchExpression(a, propertyNameA);
			var toWatchB = getInDepthWatchExpression(b, propertyNameB);
			var setA = getInDepthSetter(a, propertyNameA);
			var setB = getInDepthSetter(b, propertyNameB);

			if (!scope) scope = $rootScope;
			return [
			        scope.$watch(toWatchA, function (newValue, oldValue, scope) {
			        	var nV = (newValue instanceof Date) ? newValue.getTime() : newValue; 
			        	var oV = (oldValue instanceof Date) ? oldValue.getTime() : oldValue;
			        	if (nV !== oV) {
			        		setB(newValue);	
			        	}
			        }, useObjectEquality),
			        scope.$watch(toWatchB, function (newValue, oldValue, scope) {
			        	var nV = (newValue instanceof Date) ? newValue.getTime() : newValue; 
			        	var oV = (oldValue instanceof Date) ? oldValue.getTime() : oldValue;
			        	if (nV !== oV) {
			        		setA(newValue);	
			        	}
			        }, useObjectEquality)
			        ];
		}
		,
		// search for svy-servoyApi attribute on element, within parents (svy-autoapply could be used on a child DOM element of the web component)
		findAttribute: function (element, parent, attributeName) {
			return findAttribute(element, parent, attributeName);
		},
		
		createJSEvent : function(event,eventType,contextFilter,contextFilterElement) {
			var targetEl = event;
			if (event.target) targetEl = event.target;
			else if (event.srcElement) targetEl = event.srcElement;
			
			var form;
			var parent = targetEl;
			var targetElNameChain = new Array();
			var contextMatch = false;
			while (parent) {
				form = parent.getAttribute("ng-controller");
				if (form) {
					//global shortcut or context match
					var shortcuthit = !contextFilter || (contextFilter && form == contextFilter);
					if (!shortcuthit) break;
					contextMatch = true;
					break;
				}
				if(parent.getAttribute("name")) targetElNameChain.push(parent.getAttribute("name"));
				parent = parent.parentNode;
			}
			if (!form || form == 'MainController')  {
				// form not found, search for an active dialog
				var formInDialog = $('.svy-dialog.window.active').find("svy-formload").attr("formname");
				if (formInDialog) form = formInDialog;
			}
			
			if (!contextMatch) return null;
			
			var jsEvent = {svyType: 'JSEvent', eventType: eventType, "timestamp":new Date().getTime()};
			
			var modifiers = (event.altKey ? 8 : 0) | (event.shiftKey ? 1 : 0) | (event.ctrlKey ? 2 : 0) | (event.metaKey ? 4 : 0);
			jsEvent['modifiers'] = modifiers;
			jsEvent['x'] = event.pageX;
			jsEvent['y'] = event.pageY;

			
			if(form != 'MainController') {
				jsEvent['formName'] = form;
				var formScope = angular.element(parent).scope();
				for (var i = 0; i < targetElNameChain.length; i++) {
					if(formScope['model'][targetElNameChain[i]]) {
						jsEvent['elementName'] = targetElNameChain[i];
						break;
					}
				}
				
				if(contextFilterElement && (contextFilterElement != jsEvent['elementName'])) {
					return null;
				}
			}
			return jsEvent;
		},
        
        generateUploadUrl: function(formname ,beanname, propertyName){
            return "resources/upload/" + $sabloApplication.getClientnr() + 
	            (formname ? "/" + formname : "") + 
	    		(beanname ? "/" + beanname : "") + 
				(propertyName ? "/" + propertyName : "");
        }
	}
}).factory("$svyProperties",function($svyTooltipUtils, $timeout:angular.ITimeoutService, $scrollbarConstants, $svyUIProperties) {
	return <servoy.IServoyProperties> {
		setBorder: function(element,newVal) {
			if(typeof newVal !== 'object' || newVal == null) {element.css('border',''); return;}

			if (element.parent().is("fieldset")){ 
				$(element.parent()).replaceWith($(element));//unwrap fieldset
			}
			if(newVal.type == "TitledBorder"){
				element.wrap('<fieldset style="padding:5px;margin:0px;border:1px solid silver;width:100%;height:100%"></fieldset>')
				var x = element.parent().prepend("<legend align='"+newVal.titleJustiffication+"' style='border-bottom:0px; margin:0px;width:auto;color:"+
						newVal.color+"'>"+newVal.title+"</legend>")
				if (newVal.font) x.children("legend").css(newVal.font);
			}else if(newVal.borderStyle){ 
				element.css('border','')
				element.css(newVal.borderStyle)
			}
		},
		setCssProperty: function(element, cssPropertyName,newVal) {
			if(!newVal) {element.css(cssPropertyName,''); return;}
			if(typeof newVal != 'object'){ //for cases with direct values instead of json string background and foreground
				var obj ={}
				obj[cssPropertyName] = newVal;
				newVal = obj;
			} 
			element.css(cssPropertyName,'')
			element.css(newVal)
		},
		setHorizontalAlignment: function(element,halign) {
			if (halign != -1)
			{
				var style ={}
				if (halign == 0)
				{
					style['text-align'] = 'center';
				}
				else if (halign == 4)
				{
					style['text-align'] = 'right';
				}
				else
				{
					style['text-align'] = 'left';
				}
				element.css(style);
			}
		},
		setHorizontalAlignmentFlexbox: function(element,halign) {
			if (halign != -1)
			{
				var style ={}
				if (halign == 0)
				{
					style['-ms-flex-pack'] = 'center';
					style['justify-content'] = 'center';
				}
				else if (halign == 4)
				{
					style['-ms-flex-pack'] = 'end';
					style['justify-content'] = 'flex-end';
				}
				else
				{
					style['-ms-flex-pack'] = 'start';
					style['justify-content'] = 'flex-start';
				}
				element.css(style);
			}
		},
		setVerticalAlignment: function(element,valign) {
			var style ={}
			if (valign == 1)
			{
				style['top'] = 0;
			}
			else if (valign == 3)
			{
				style['top'] = '100%';
				style['transform'] = 'translateY(-100%)';
			}
			else
			{
				style['top'] = '50%';
				style['transform'] = 'translateY(-50%)';
			}
			element.css(style);
		},
		setRotation: function(element,scope,rotation) {
			if (rotation && rotation != 0)
			{
				var r = 'rotate(' + rotation + 'deg)';
				var style ={}
				style['-moz-transform'] = r;
				style['-webkit-transform'] = r;
				style['-o-transform'] = r;
				style['-ms-transform'] = r;
				style['transform'] = r;
				style['position'] = 'absolute';
				if (rotation == 90 || rotation == 270)
				{
					style['width'] = scope.model.size.height+'px';
					style['height'] = scope.model.size.width+'px';
					style['left'] =  (scope.model.size.width -scope.model.size.height)/2 +'px';
					style['top'] = (scope.model.size.height -scope.model.size.width)/2 +'px';
				}
				//setTimeout(function(){ // temporary fix until case with ImageMediaID will be fixed (will probably not use bagckgroun-image)
				element.css(style);  
				//},30)
			}
		},
		addSelectOnEnter: function(element) {
			element.on('focus', function() {
				$timeout(function() {
					if (element.is(":focus"))
						(<HTMLInputElement>element[0]).select(); 
				},0);
			});
		},
		getScrollbarsStyleObj:function (scrollbars){
			var style = {}; 
			if ((scrollbars & $scrollbarConstants.HORIZONTAL_SCROLLBAR_NEVER) == $scrollbarConstants.HORIZONTAL_SCROLLBAR_NEVER)
			{
				style['overflowX'] = "hidden";
			}
			else if ((scrollbars & $scrollbarConstants.HORIZONTAL_SCROLLBAR_ALWAYS) == $scrollbarConstants.HORIZONTAL_SCROLLBAR_ALWAYS)
			{
				style['overflowX'] = "scroll";
			}
			else
			{
				style['overflowX'] = "auto";
			}

			if ((scrollbars & $scrollbarConstants.VERTICAL_SCROLLBAR_NEVER) == $scrollbarConstants.VERTICAL_SCROLLBAR_NEVER)
			{
				style['overflowY'] = "hidden"; 
			}
			else if ((scrollbars & $scrollbarConstants.VERTICAL_SCROLLBAR_ALWAYS) == $scrollbarConstants.VERTICAL_SCROLLBAR_ALWAYS)
			{
				style['overflowY'] = "scroll"; //$NON-NLS-1$
			}
			else
			{
				style['overflowY'] = "auto"; //$NON-NLS-1$
			}

			return style;
		},
		setScrollbars: function(element, value) {
			element.css(this.getScrollbarsStyleObj(value));
		},
		createTooltipState: function(element,value) {
			var tooltip =  value;
			var initialDelay = $svyUIProperties.getUIProperty("tooltipInitialDelay");
			if(initialDelay === null || isNaN(initialDelay)) initialDelay = 750;
			var dismissDelay = $svyUIProperties.getUIProperty("tooltipDismissDelay"); 
			if(dismissDelay=== null || isNaN(dismissDelay)) dismissDelay = 5000;

			function doShow(event) {
				var tooltipText = typeof tooltip === 'function' ? tooltip() : tooltip;
				if(tooltipText) {
					$svyTooltipUtils.showTooltip(event, tooltipText, initialDelay, dismissDelay);
				}
	        }
			function doHide(event) {
	        	$svyTooltipUtils.hideTooltip();
	        }
			function register() {
				element.on('mouseover', doShow);
		        element.on('mouseout', doHide);
				element.on('click', doHide);
				element.on('contextmenu', doHide);
			}
			if (tooltip) {
		        register();
			}
			return function(newValue) {
				if (newValue && !tooltip) {
					register();
				}
				else if (!newValue && tooltip) {
					element.off('mouseover', doShow);
			        element.off('mouseout', doHide);
					element.off('click', doHide);
					element.off('contextmenu', doHide);
				}
				tooltip = newValue;
			}
		}
	}
}).directive('ngOnChange', function($parse:angular.IParseService){
	return function(scope, elm, attrs){       
		var onChangeFunction = $parse(attrs['ngOnChange']);
		elm.bind("change", function(event) {
			scope.$apply(function() {
				onChangeFunction(scope, { $cmd: event });
			})});
	};
}).directive('svyAutoapply', function($sabloApplication: sablo.ISabloApplication, $parse: angular.IParseService, $log: angular.ILogService, $utils: servoy.IUtils) {
	return {
		restrict: 'A', // only activate on element attribute
		require: '?ngModel', // get a hold of NgModelController
		link: function(scope, element, attrs, ngModel: angular.INgModelController) {
			if(!ngModel || element.attr("svy-autoapply-disabled")) return; // do nothing if no ng-model
			var dataproviderString = attrs['ngModel'];
			var index = 0;

			var splitDB = dataproviderString.split('.');
			var scopeObj = scope;
			var modelFound = false;
			for(var i = 0; (i < splitDB.length) && scopeObj; i++) {
				scopeObj = scopeObj[splitDB[i]];
				index += splitDB[i].length + (i ? 1 : 0);
				if(scopeObj instanceof sablo_app.Model) {
					modelFound = true;
					break;
				}
			}

			if(!modelFound) {
				index = dataproviderString.indexOf('.');
			}

			if (index > 0) {
				var modelString = dataproviderString.substring(0,index);
				var modelFunction = $parse(modelString);
				var beanModel = modelFunction(scope);
				var propertyname = dataproviderString.substring(index+1);
				var beanname;
				var parent = scope.$parent;

				beanname = element.attr("name");
				if (! beanname) {
					var nameParentEl = element.parents("[name]").first(); 
					if (nameParentEl) beanname = nameParentEl.attr("name");
				}
				if (! beanname) {
					for(var key in parent['model']) {
						if (parent['model'][key] === beanModel) {
							beanname = key;
							break;
						}
					}
				}

				if (!beanname) {
					$log.error("[svy-autoapply] bean name not found for model string: " + dataproviderString);
					return;
				}

				function searchForFormName() {
					var formname = parent['formname'];
					var formParentScope = parent;
					while (!formname) {
						formParentScope = formParentScope.$parent;
						if (formParentScope) {
							formname = formParentScope['formname'];
						}
						else { 
							$log.error("[svy-autoapply] no form found for " + beanname + "." + propertyname + ". It might have been recreated/detached or not yet attached to angular scope chain.");
							return;
						}
					}
					return formname;
				}

				var formName = null;
				// Listen for change events to enable binding
				element.bind('change', function() {
					scope.$evalAsync ( function() {
						// model maybe is not updated yet, commit it now.
						ngModel.$commitViewValue();
						var svyServoyApi = $utils.findAttribute(element, parent, "svy-servoyApi");
						// use svy apply rather then pushChange because svy apply might get intercepted by components such as portals
						// that have nested child web components
						if (svyServoyApi && svyServoyApi.apply) {
							svyServoyApi.apply(propertyname);
						} else {
							// this shouldn't happen (svy-apply not being set on a web-component...)
							$log.error("cannot apply new value");
						}
					});
				});

				// Listen for start edit
				element.bind('focus', function() {
					scope.$evalAsync ( function() {
						var svyServoyApi = $utils.findAttribute(element, parent, "svy-servoyApi");
						if (svyServoyApi && svyServoyApi.startEdit) {
							svyServoyApi.startEdit(propertyname);
						} else {
							// this shouldn't happen (svy-servoyApi.startEdit not being set on a web-component...)
							if (!formName) formName = searchForFormName(); 
							$sabloApplication.callService("formService", "startEdit", {formname:formName,beanname:beanname,property:propertyname},true)
						}
					});
				});

			}
			else {
				$log.error("svyAutoapply attached to a element that doesn't have the right ngmodel (model.value): " + dataproviderString)
			}
		}
	};
}).directive('svyEnter',  function ($parse:angular.IParseService,$utils:servoy.IUtils) {
	return {
		restrict: 'A',
		link: function (scope:angular.IScope, element:JQuery, attrs) {
			$utils.attachEventHandler($parse,element,scope,attrs['svyEnter'],'keydown', $utils.testEnterKey, 100, false, true, attrs['ngModel'] ? attrs['ngModel'] : element.parent().attr('ng-model'));
		}
	};
}).directive('svyChange',  function ($parse:angular.IParseService,$utils:servoy.IUtils) {
	return {
		restrict: 'A',
		link: function (scope, element, attrs) {
			// timeout needed for angular to update model first
			$utils.attachEventHandler($parse,element,scope,attrs['svyChange'],'change',null,100);
		}
	};
}).directive('svyClick',  function ($parse:angular.IParseService,$utils:servoy.IUtils) {
	return {
		restrict: 'A',
		link: function (scope, element, attrs) {
			var dblClickFunction = $utils.getEventHandler($parse,scope,attrs['svyDblclick'])
			if (dblClickFunction)
			{
				// special handling when double click is also present
				var fn = $utils.getEventHandler($parse,scope,attrs['svyClick'])
				if (fn)
				{
					element.on('click', function(event) {
						if(element['timerID']){
							clearTimeout(element['timerID']);
							element['timerID']=null;
							//double click, do nothing
						}
						else{
							element['timerID']=setTimeout(function(){
								element['timerID']=null;
								scope.$apply(function() {
									fn(scope, {$event:event});
								});
							},250)}
					});
				}	
			}
			else
			{
				$utils.attachEventHandler($parse,element,scope,attrs['svyClick'],'click');
			}
		}
	};
}).directive('svyDblclick',  function ($parse:angular.IParseService,$utils:servoy.IUtils) {
	return {
		restrict: 'A',
		link: function (scope, element, attrs) {
			$utils.attachEventHandler($parse,element,scope,attrs['svyDblclick'],'dblclick');
		}
	};
}).directive('svyRightclick',  function ($parse:angular.IParseService,$utils:servoy.IUtils) {
	return {
		restrict: 'A',
		link: function (scope, element, attrs) {
			$utils.attachEventHandler($parse,element,scope,attrs['svyRightclick'],'contextmenu',null,null,true);
		}
	};
}).directive('svyFocusgained',  function ($parse:angular.IParseService,$utils:servoy.IUtils) {
	return {
		restrict: 'A',
		link: function (scope, element, attrs) {
			$utils.attachEventHandler($parse,element,scope,attrs['svyFocusgained'],'focus');
		}
	};
}).directive('svyFocuslost',  function ($parse:angular.IParseService,$utils:servoy.IUtils) {
	return {
		restrict: 'A',
		link: function (scope, element, attrs) {
			$utils.attachEventHandler($parse,element,scope,attrs['svyFocuslost'],'blur');
		}
	};
}).directive('svyBorder',  function ($svyProperties:servoy.IServoyProperties) {
	return {
		restrict: 'A',
		link: function (scope, element, attrs) {
			scope.$watch(attrs['svyBorder'],function(newVal){
				$svyProperties.setBorder(element,newVal);
			}, true)

		}
	};
}).directive('svyMargin',  function ($utils:servoy.IUtils,$parse:angular.IParseService) {
	return {
		restrict: 'A',
		link: function (scope, element, attrs) {
			var marginModelObj= $parse(attrs['svyMargin'])(scope);
			if(marginModelObj){ //only design time property, no watch
				element.css(marginModelObj);
			}
		}
	};
})
.directive('svyFont',  function ($utils:servoy.IUtils) {
	return {
		restrict: 'A',
		link: function (scope, element, attrs) {
			$utils.autoApplyStyle(scope,element,attrs['svyFont'],'font')
		}
	}
})
.directive('svyBackground',  function ($utils:servoy.IUtils) {
	return {
		restrict: 'A',
		link: function (scope, element, attrs) {
			$utils.autoApplyStyle(scope,element,attrs['svyBackground'],'backgroundColor')
		}
	}
})
.directive('svyForeground',  function ($utils:servoy.IUtils) {
	return {
		restrict: 'A',
		link: function (scope, element, attrs) {
			$utils.autoApplyStyle(scope,element,attrs['svyForeground'],'color')
		}
	}
})
.directive('svyScrollbars',  function ($svyProperties:servoy.IServoyProperties,$parse:angular.IParseService,$scrollbarConstants) {
	return {
		restrict: 'A',
		link: function (scope, element, attrs) {
			var scrollbarsModelObj= $parse(attrs['svyScrollbars'])(scope);
			if (scrollbarsModelObj === $scrollbarConstants.SCROLLBARS_WHEN_NEEDED || scrollbarsModelObj === null || scrollbarsModelObj === undefined)
			{
				// default value, add from css, not inline style
				element.addClass('svy-overflow-auto');
			}
			else
			{
				element.css($svyProperties.getScrollbarsStyleObj(scrollbarsModelObj));
			}	
		}
	}
})
.directive('svyHorizontaldirection',  function ($parse:angular.IParseService,$scrollbarConstants) {
	return {
		restrict: 'A',
		link: function (scope, element, attrs) {
			var scrollbarsModelObj= $parse(attrs['svyHorizontaldirection'])(scope);
			if ((scrollbarsModelObj & $scrollbarConstants.VERTICAL_SCROLLBAR_NEVER) == $scrollbarConstants.VERTICAL_SCROLLBAR_NEVER) // vertical scrollbar never
			{
				element.css('float','left');
				element.css('margin-right','2px');
			}
		}
	}
})
.directive('svyMnemonic',  function ($utils:servoy.IUtils,$parse:angular.IParseService) {
	return {
		restrict: 'A',
		link: function (scope, element, attrs) {
			var letter= $parse(attrs['svyMnemonic'])(scope);
			if(letter){ //only design time property, no watch
				element.attr('accesskey',letter);
			}
		}
	}
})

.directive('svyTextrotation',  function ($utils:servoy.IUtils,$parse:angular.IParseService,$svyProperties:servoy.IServoyProperties) {
	// DESIGN TIME ONLY
	return {
		restrict: 'A',
		link: function (scope:angular.IScope&{model:{size:{height:number,width:number}}}, element:JQuery, attrs) {  
			var rotation= $parse(attrs['svyTextrotation'])(scope);
			$svyProperties.setRotation(element,scope,rotation);
		}
	}
})
.directive('svyHorizontalalignment',  function ($utils:servoy.IUtils,$parse:angular.IParseService,$svyProperties:servoy.IServoyProperties) {
	// DESIGN TIME ONLY
	return {
		restrict: 'A',
		link: function (scope, element, attrs) {  
			var halign= $parse(attrs['svyHorizontalalignment'])(scope);
			$svyProperties.setHorizontalAlignment(element,halign);
		}
	}
})
.directive('svyVerticalalignment',  function ($utils:servoy.IUtils,$parse:angular.IParseService,$svyProperties:servoy.IServoyProperties) {
	// DESIGN TIME ONLY
	return {
		restrict: 'A',
		link: function (scope, element, attrs) { 
			// see http://zerosixthree.se/vertical-align-anything-with-just-3-lines-of-css/
			// do we need preserve-3d ?
			var valign= $parse(attrs['svyVerticalalignment'])(scope);
			$svyProperties.setVerticalAlignment(element,valign);
		}
	}
})
.directive('svySelectonenter',  function ($timeout:angular.ITimeoutService,$svyProperties:servoy.IServoyProperties) {
	return {
		restrict: 'A',
		link: function (scope, element, attrs) {
			if (scope.$eval(attrs['svySelectonenter']))
			{
				$svyProperties.addSelectOnEnter(element);
			}
		}
	};
})
.directive('svyRollovercursor',  function ($timeout: angular.ITimeoutService) {
	return {
		restrict: 'A',
		link: function (scope, element, attrs) {
			if (scope.$eval(attrs['svyRollovercursor']) == 12 /* hand_cursor */)
			{
				element.css('cursor','pointer');
			}
			else
			{
				element.css('cursor','default');
			}
		}
	};
})
.directive( 'svyFormComponent', function($utils, $compile: angular.ICompileService, $templateCache, $foundsetTypeConstants: foundsetType.FoundsetTypeConstants, $sabloConstants, $timeout: angular.ITimeoutService, $webSocket: sablo.IWebSocket, $anchorConstants) {
		return {
			restrict: 'A',
			scope: {
				svyFormComponent: "=svyFormComponent",
				foundset: "=foundset",
				responsivePageSize: "=responsivePageSize",
				pageLayout: "=pageLayout",
				selectionClass: "=selectionClass"
			},
			link: function( scope: any, element, attrs ) {
				let svyServoyApi = scope.svyServoyapi?scope.svyServoyapi:scope.$parent.svyServoyapi;
				if ( !svyServoyApi ) svyServoyApi = $utils.findAttribute( element, scope.$parent, "svy-servoyApi" );
				let handlers = scope.$parent.handlers;
				if ( svyServoyApi.isInDesigner() ) {
					// in designer just show it as a normal form component
					const newValue = scope.svyFormComponent
					if ( newValue ) {
						element.empty();
						const elements = svyServoyApi.getFormComponentElements(newValue.startName, newValue );

						if ( newValue.absoluteLayout ) {
							const height = newValue.formHeight;
							const width = newValue.formWidth;
							var template = "<div style='position:relative;";
							if ( height ) template += "height:" + height + "px;"
							if ( width ) template += "width:" + width + "px;"
							template += "'";
							template += "></div>";
							var div = $compile( template )( scope );
							div.append( elements );
							element.append( div );
						} else { // is responsive
							element.append( elements );
						}
					}
				}
				else {
					let foundsetListener = null;
					const componentListeners = [];
					scope.$on('$destroy', function() {
				        if (foundsetListener) foundsetListener();
				        componentListeners.forEach(value => value());
				        foundsetListener = null;
				        componentListeners.length = 0;
				     });
					let page = 0; // todo should be a model hidden object
					let numberOfCells = 0;
					
					function getFoundset() {
						return scope.foundset as foundsetType.FoundsetPropertyValue;
					}
					
					scope.firstPage = function() {
						if (page != 0) {
							pager.children().css("cursor", "progress");
							page = 0;
							createRows();
						}
					}
					scope.moveRight = function() {
						pager.children().css("cursor", "progress");
						page++;
						createRows();
					}
					scope.moveLeft = function() {
						if (page > 0) {
							pager.children().css("cursor", "progress");
							page--;
							createRows();
						}
					}

					scope.onRowClick = function(index) {
						scope.foundset.requestSelectionUpdate([index]);
					}

					const parent = element.parent();
                    const rowToModel: Array<servoy.IServoyScope> = [];
					const pager = $compile(angular.element("<div style='position:absolute;right:0px;bottom:0px;z-index:1'><div style='text-align:center;cursor:pointer;visibility:hidden;display:inline;padding:3px;white-space:nowrap;vertical-align:middle;background-color:rgb(255, 255, 255, 0.6);' ng-click='firstPage()' ><i class='glyphicon glyphicon-backward'></i></div><div style='text-align:center;cursor:pointer;visibility:hidden;display:inline;padding:3px;white-space:nowrap;vertical-align:middle;background-color:rgb(255, 255, 255, 0.6);' ng-click='moveLeft()' ><i class='glyphicon glyphicon-chevron-left'></i></div><div style='text-align:center;cursor:pointer;visibility:hidden;display:inline;padding:3px;white-space:nowrap;vertical-align:middle;background-color:rgb(255, 255, 255, 0.6);' ng-click='moveRight()'><i class='glyphicon glyphicon-chevron-right'></i></div></div>"))(scope);

                    let template = null;
					function copyRecordProperties( childElement, rowModel, viewportIndex ) {
						if ( childElement.foundsetConfig && childElement.foundsetConfig.recordBasedProperties ) {
							childElement.foundsetConfig.recordBasedProperties.forEach(( value ) => {
								rowModel[value] = childElement.modelViewport[viewportIndex][value];
							} );
						}
					};
					
					function destroyScopes(array: Array<servoy.IServoyScope>) {
					    array.forEach(scope => scope.$destroy());
					    array.length = 0;
					}

					function createRows() {
                        numberOfCells = scope.responsivePageSize;
                        if (numberOfCells == 0 ) {
                        	if (scope.svyFormComponent.absoluteLayout) {
		                        const parentWidth = parent.outerWidth();
		                        const parentHeight = parent.outerHeight();
		                        const height = scope.svyFormComponent.formHeight;
		                        const width = scope.svyFormComponent.formWidth;
		                        const numberOfColumns =  (scope.pageLayout == 'listview') ? 1 : Math.floor(parentWidth/width);
		                        const numberOfRows = Math.floor(parentHeight/height);
		                        numberOfCells  = numberOfRows * numberOfColumns;
		                        // always just render 1
		                        if (numberOfCells < 1) numberOfCells = 1;
                        	}
                        	else {
                        		parent.append(angular.element("<span>responsivePageSize property must be set when using a responsive form component</span>"));
                        		return;
                        	}
                        }
                        scope.foundset.setPreferredViewportSize(numberOfCells);
                        const startIndex = page * numberOfCells;
                        if (scope.foundset.viewPort.startIndex != startIndex) {
                        	scope.foundset.loadRecordsAsync(startIndex, numberOfCells);
                        } else {
						    destroyScopes(rowToModel);
						    parent.children("[svy-form-component-clone]" ).remove();
	                        const maxRows = Math.min(numberOfCells, scope.foundset.viewPort.rows.length);
	                        for ( let i = 0; i < maxRows; i++ ) {
	                        	createRow(i, true);
	                        }
	                        if (numberOfCells > scope.foundset.viewPort.rows.length && scope.foundset.viewPort.startIndex + scope.foundset.viewPort.size < scope.foundset.serverSize) {
	                        	scope.foundset.loadExtraRecordsAsync(Math.min(numberOfCells - scope.foundset.viewPort.rows.length, scope.foundset.serverSize - scope.foundset.viewPort.startIndex - scope.foundset.viewPort.size));
	                        }
	                        else if (scope.foundset.viewPort.size > numberOfCells) {
								// the (initial) viewport  is bigger then the numberOfCells we have created rows for, adjust the viewport to be smaller.
	                        	scope.foundset.loadLessRecordsAsync(numberOfCells - scope.foundset.viewPort.size);
	                        }
	                        
	                        updatePagingControls();
                        }
					}
					function updatePagingControls() {
                        const pagerChildren = pager.children();
                        pagerChildren.css("cursor","pointer");
    					pagerChildren.first().css("visibility", page > 0 ? "visible" : "hidden");
                        pagerChildren.eq(1).css("visibility", page > 0 ? "visible" : "hidden");
    					const showNext = scope.foundset.hasMoreRows || (scope.foundset.serverSize - (page * numberOfCells + Math.min(numberOfCells, scope.foundset.viewPort.rows.length))) > 0;
    					pagerChildren.last().css("visibility", showNext ? "visible" : "hidden");
					}
					
					interface IServoyScopeInternal extends servoy.IServoyScope {
						createdChildElements?: number;
					}
					
					function createChildElementForRow(index: number, rowId: string, row: IServoyScopeInternal, childElement: componentType.ComponentPropertyValue) {
						function Model() {
						}
						Model.prototype = childElement.model;
				
						function ServoyApi( rowModel, rowId ) {
							this.apply = ( property ) => {
								childElement.servoyApi.apply( property, rowModel, rowId );
							}
							this.startEdit = ( property ) => {
								childElement.servoyApi.startEdit( property, rowId )
							}
						}
						ServoyApi.prototype = svyServoyApi;
				
						function Handlers( handlers, rowModel, rowId ) {
							this.svy_servoyApi = new ServoyApi( rowModel, rowId );
							for (var key in handlers) {
								this[key] = handlers[key].selectRecordHandler(rowId);
							}
						}
				
						const rowModel = new Model()
				
						const simpleName = childElement.name;
						
						copyRecordProperties( childElement, rowModel, index );
						row.model[simpleName] = rowModel;
						row.handlers[simpleName] = new Handlers( childElement.handlers, rowModel, rowId );
						row.api[simpleName] = {};
						childElement['api'][index] = row.api[simpleName];
						if ( scope.svyFormComponent.absoluteLayout ) {
							
							var elementLayout = {position: "absolute"};
							if (scope.svyFormComponent.useCssPosition)
							{
								elementLayout = childElement.model.cssPosition;
							}
							else
							{
								var anchoredTop = (childElement.model.anchors & $anchorConstants.NORTH) != 0; // north
								var anchoredRight = (childElement.model.anchors & $anchorConstants.EAST) != 0; // east
								var anchoredBottom = (childElement.model.anchors & $anchorConstants.SOUTH) != 0; // south
								var anchoredLeft = (childElement.model.anchors & $anchorConstants.WEST) != 0; //west

								if (!anchoredLeft && !anchoredRight) anchoredLeft = true;
								if (!anchoredTop && !anchoredBottom) anchoredTop = true;
								if (anchoredLeft)
								{
									elementLayout['left'] = childElement.model.location.x + "px";
								}
								if (anchoredRight)
								{
									elementLayout['right'] = (scope.svyFormComponent.formWidth - childElement.model.location.x - childElement.model.size.width) + "px";
								}
								else
								{
									elementLayout['width'] = childElement.model.size.width + "px";
								}	
								if (anchoredTop)
								{
									elementLayout['top'] = childElement.model.location.y + "px";
								}
								if (anchoredBottom)
								{
									elementLayout['bottom'] = (scope.svyFormComponent.formHeight - childElement.model.location.y - childElement.model.size.height) + "px"
								}
								else
								{
									elementLayout['height'] = childElement.model.size.height + "px";
								}
							}	
							row.layout[simpleName] = elementLayout;
						}
						
						row.createdChildElements++;
						
						if (row.createdChildElements == scope.svyFormComponent.childElements.length) {
							// ok now we have all components prepared for this row; create the directives/DOM
							delete row.createdChildElements;
							
							template(row, function(cloned) {
								 $(parent.children()[index + 1]).append(cloned); // +1 for pager div
							});
						}
					}
					
					function createRow(index, createRecordLinkedComponentsAsWell) {
						const rowId = scope.foundset.viewPort.rows[index][$foundsetTypeConstants.ROW_ID_COL_KEY]
						const row = scope.$new( false, scope ) as IServoyScopeInternal;
						rowToModel[index] = row;
						row.model = {}
						row.api = {};
						row.layout = {};
						row.handlers = {}
						row.createdChildElements = 0;
						
						const clone = element.clone();
						clone.attr("svy-form-component-clone", clone.attr("svy-form-component"));
						clone.removeAttr("svy-form-component");
						clone.attr("ng-click", "onRowClick(" + (scope.foundset.viewPort.startIndex + index) + ")");
						$compile(clone)(scope);
						
						// pager div is the first div in parent; form component divs follow starting at index 1
						if (rowToModel.length - 1 == index) {
						    parent.append(clone);
					    } else if (index == 0) {
						    clone.insertAfter(pager);
						} else {
						    const child = $(parent.children()[index + 1]);
						    clone.insertBefore(child);
						}
						
						if (createRecordLinkedComponentsAsWell) {
							for ( var j = 0; j < scope.svyFormComponent.childElements.length; j++ ) {
								createChildElementForRow(index, rowId, row, scope.svyFormComponent.childElements[j] as componentType.ComponentPropertyValue);
							}
						} else {
							// this is probably a foundset viewport insert; the components that are record linked will themselves populate the child elements in the component listeners code later
							// so just add the ones that are not record linked here because those do not have viewportChangeListeners added, and insert can add them right away, as they don't have viewport specific data
							for ( var j = 0; j < scope.svyFormComponent.childElements.length; j++ ) {
								const childElement = scope.svyFormComponent.childElements[j] as componentType.ComponentPropertyValue;
								if (!childElement.foundsetConfig || !childElement.foundsetConfig.recordBasedProperties || !(childElement.foundsetConfig.recordBasedProperties.length > 0)) {
									createChildElementForRow(index, rowId, row, scope.svyFormComponent.childElements[j] as componentType.ComponentPropertyValue);
								}
							}
						}
					}
				
					function updateSelection(newValue, oldValue?) {
						let children = parent.children();
						if(oldValue) {
							for(let k = 0; k < oldValue.length; k++) {
								let idx = oldValue[k] - scope.foundset.viewPort.startIndex;
								if(idx > -1 && idx < children.length - 1) {
									$(parent.children()[idx + 1]).removeClass(scope.selectionClass);
								}
							}
						}
						else {
							for(let k = 1; k < children.length; k++) {
								$(parent.children()[k]).removeClass(scope.selectionClass);
							}
						}
						for(let k = 0; k < newValue.length; k++) {
							let idx = newValue[k] - scope.foundset.viewPort.startIndex;
							if(idx > -1 && idx < children.length - 1) {
								$(parent.children()[idx + 1]).addClass(scope.selectionClass);
							}
						}
					}

					if ( scope.foundset && scope.foundset.viewPort && scope.foundset.viewPort.rows
						&& scope.svyFormComponent && scope.svyFormComponent.childElements ) {
						element.empty();
						parent.empty();
						parent.append( pager );
						template = $compile( $templateCache.get( scope.svyFormComponent.uuid ));
						const propertyInName = scope.svyFormComponent.startName

						const height = scope.svyFormComponent.formHeight;
						const  width = scope.svyFormComponent.formWidth;

						if (scope.pageLayout == 'listview')
						{
							element.css( "width", "100%" );
						}
						if ( scope.svyFormComponent.absoluteLayout ) {
							element.css( "position", "relative" )
							element.css( "height", height + "px" )
							if (scope.pageLayout != 'listview')
							{
								element.css( "width", width + "px" );
							}	
						}
						
						// this listener works in tandem with childElement.addViewportChangeListener(...) listeners 
						foundsetListener = getFoundset().addChangeListener(function(changes) {
						    // check to see what actually changed server-side and update what is needed in browser
							let shouldUpdatePagingControls = false;
							if (changes.viewportRowsCompletelyChanged) {
								createRows();
							} else if (changes.fullValueChanged) {
								scope.foundset = changes.fullValueChanged.newValue; // the new value by ref would be updated in scope automatically only later otherwise and we use that in code
								createRows();
							} else if (changes.viewportRowsUpdated) {
								const updates = changes.viewportRowsUpdated.updates;
								updates.forEach(( value ) => {
									// we handle here just row deletes and inserts (insert blank row); the data changes will be handles in each component viewport change listener (see code below)
									if ( value.type == $foundsetTypeConstants.ROWS_INSERTED ) {
										for ( let k = value.startIndex; k <= value.endIndex; k++ ) {
											rowToModel.splice(k, 0, null);
											createRow(k, false);
										}
										shouldUpdatePagingControls = true;
									}
									else if ( value.type == $foundsetTypeConstants.ROWS_DELETED) {
										const endIndex = Math.min(rowToModel.length-1, value.endIndex); // -1 because value.endIndex is including this call can happen because of a removed of the view port if the view port was bigger then cells that we render.
										for ( let k = value.startIndex; k <= endIndex; k++ ) {
											destroyScopes(rowToModel.splice(value.startIndex, 1));
											parent.children()[value.startIndex + 1].remove(); // + 1 is due to pager that is the first div
										}
										shouldUpdatePagingControls = true;
									}
								} )
							}
							
							if (changes.serverFoundsetSizeChanged) shouldUpdatePagingControls = true;
							
							if (shouldUpdatePagingControls) updatePagingControls();
							
							if (changes.viewPortSizeChanged && getFoundset().serverSize > 0 && ((page + 1) * numberOfCells - 1 > getFoundset().serverSize) && getFoundset().viewPort.size == 0 && numberOfCells > 0) {
								// if we were on last page (or some page) and probably due to a delete there are no longer records for that page, adjust page number to show last available page
		                        page = Math.floor((getFoundset().serverSize - 1) / numberOfCells);
		                        createRows();
							} else if (changes.viewPortStartIndexChanged) {
								// an insert/delete before current page made viewport start index no longer match page start index; adjust
								const shiftedPageDelta = page * numberOfCells - getFoundset().viewPort.startIndex; // can be negative (insert) or positive(delete)
								if (shiftedPageDelta != 0) {
									const wantedVPSize = getFoundset().viewPort.size;
									const wantedVPStartIndex = page * numberOfCells;
									const serverSize = getFoundset().serverSize;
									
									// when load extra would request more records after, there might not be enough records in the foundset (deleted before)
									let loadExtraCorrected = shiftedPageDelta;
									if (loadExtraCorrected > 0 && wantedVPStartIndex + wantedVPSize > serverSize)
										loadExtraCorrected -= (wantedVPStartIndex + wantedVPSize - serverSize);
									if (loadExtraCorrected != 0) getFoundset().loadExtraRecordsAsync(loadExtraCorrected, true);
									
									// load less if it happens at the end - might need to let more records slide-in the viewport if available (insert before)
									let loadLessCorrected = shiftedPageDelta;
									if (loadLessCorrected < 0 && wantedVPSize < numberOfCells && wantedVPStartIndex + wantedVPSize < serverSize)
										loadLessCorrected += Math.min(serverSize - wantedVPStartIndex - wantedVPSize, numberOfCells - wantedVPSize);
									if (loadLessCorrected != 0) getFoundset().loadLessRecordsAsync(loadLessCorrected, true);
									
									getFoundset().notifyChanged();
								}
								if(scope.selectionClass) {
									updateSelection(getFoundset().selectedRowIndexes);
								}
							}
							
							if(changes.selectedRowIndexesChanged) {
								if(scope.selectionClass) {
									updateSelection(changes.selectedRowIndexesChanged.newValue, changes.selectedRowIndexesChanged.oldValue);
								}
								if(handlers && handlers.onSelectionChanged) {
									var e = {target: parent[0]};
									handlers.onSelectionChanged($utils.createJSEvent(e,"onselectionchanged"));
								}
							}
							// TODO any other types of changes that need handling here?
						});

						for ( let j = 0; j < scope.svyFormComponent.childElements.length; j++ ) {
							const childElement = scope.svyFormComponent.childElements[j] as componentType.ComponentPropertyValue;
							if (childElement.name.indexOf(propertyInName) != 0) throw "The child name " + childElement.name + " should start with " +  propertyInName;
							const simpleName = childElement.name;
							if (childElement.foundsetConfig && childElement.foundsetConfig.recordBasedProperties && childElement.foundsetConfig.recordBasedProperties.length > 0) {
								componentListeners.push(childElement.addViewportChangeListener((change) => {
									// make sure the child element listeners are executed later, after the foundset change listener had a chance to process inserts and deletes
									$webSocket.addIncomingMessageHandlingDoneTask(function() {
										if (change.viewportRowsUpdated) {
											const updates = change.viewportRowsUpdated.updates;
											updates.forEach(( value ) => {
												if (value.type == $foundsetTypeConstants.ROWS_CHANGED) {
													for (let k = value.startIndex; k <= value.endIndex; k++) {
														const row = rowToModel[k] as servoy.IServoyScope;
														copyRecordProperties(childElement, row.model[simpleName], k);
													}
												} else if (value.type == $foundsetTypeConstants.ROWS_INSERTED) {
													// the actual 'row' was created when foundset change listener executed before; we just need to create the child element related stuff
													for (let k = value.startIndex; k <= value.endIndex; k++) {
														const rowId = scope.foundset.viewPort.rows[k][$foundsetTypeConstants.ROW_ID_COL_KEY];
														const row = rowToModel[k];
														createChildElementForRow(k, rowId, row, scope.svyFormComponent.childElements[j] as componentType.ComponentPropertyValue);
													}
												}
											});
										}
										return [scope];
								    });
								}));
							}
							
							// TODO do we always have to attach this? Because the component maybe doesn't use that modelChangeNotifier
							// but we don't really know this when we are compiling the template.
							Object.defineProperty(childElement.model, $sabloConstants.modelChangeNotifier,
			                         { configurable: true, value: function(property,value) {
			                        	rowToModel.some((row) => {
			                        		if (row.model[simpleName][$sabloConstants.modelChangeNotifier]) {
			                        			row.model[simpleName][$sabloConstants.modelChangeNotifier](property,value);
			                        		}
			                        		else return true; // if there is no change modifier at the first row then we can skip
			                        	})
			                  }});
						}
						if (scope.responsivePageSize == 0){
							let lastValue = 0;
							let lastChangeTimed= 0;
							const NUMBER_OF_CELLS_CHANGE_TIMEOUT = 500;
							let resizeTimeoutID;
							
							// this watch will also be called when resizing browser window due to the $timeout in servoy_app.ts -> svyLayoutUpdate
							scope.$watch(() => {
				                        const parentWidth = parent.outerWidth();
				                        const parentHeight = parent.outerHeight();
				                        const height = scope.svyFormComponent.formHeight;
				                        const width = scope.svyFormComponent.formWidth;
				                        const numberOfColumns = (scope.pageLayout == 'listview') ? 1 : Math.floor(parentWidth/width);
				                        const numberOfRows = Math.floor(parentHeight/height);
				                        const numberOfCells = numberOfRows * numberOfColumns;
				                        let currentTime;
				                        if (lastValue != numberOfCells && ((currentTime = new Date().getTime()) - lastChangeTimed) > NUMBER_OF_CELLS_CHANGE_TIMEOUT) {
				                        	lastValue = numberOfCells;
				                        	lastChangeTimed = currentTime;
				                        	if (resizeTimeoutID) {
				                        		$timeout.cancel(resizeTimeoutID);
				                        		resizeTimeoutID = undefined;
				                        	}
				                        } else if (!resizeTimeoutID) {
				                        	resizeTimeoutID = $timeout( function() {
				        						// nothing to do here; it will just make sure to trigger an angular digest that will call the $watch above again - to not miss handling last change
				                        		resizeTimeoutID = undefined;
				        					}, NUMBER_OF_CELLS_CHANGE_TIMEOUT);
				                        }
				                        
							            return lastValue;
							        }, (newValue) => {
	                                    createRows();
							        });
						}
						else {
							createRows();
						}
					}
			}
		}
	};
})
.factory("$apifunctions", function (){

	return {

		getSelectedText: function (elem){
			// selectionStart/End seems to be lost/clear when the element looses focus (chrome and IE but firefox works fine)
			// so we keep save those during blur			
			var iSelectionStart;
			var iSelectionEnd;
			$(elem).on('blur', function() {
				iSelectionStart = elem.selectionStart;
				iSelectionEnd = elem.selectionEnd;
			});
			
			return function(){
				var startPos = !$(elem).is(":focus") && iSelectionStart != undefined ? iSelectionStart : elem.selectionStart;
				var endPos = !$(elem).is(":focus") && iSelectionEnd != undefined ? iSelectionEnd : elem.selectionEnd;
			
				return elem.value.substr(startPos, endPos - startPos);
			}
		},
		selectAll: function (elem){
			return function(){
				elem.select();
			}
		},
		replaceSelectedText:  function (elem){
			// selectionStart/End seems to be lost/clear when the element looses focus (chrome and IE but firefox works fine)
			// so we keep save those during blur
			var iSelectionStart;
			var iSelectionEnd;
			$(elem).on('blur', function() {
				iSelectionStart = elem.selectionStart;
				iSelectionEnd = elem.selectionEnd;
			});			
			
			return function(s) {
				var startPos = !$(elem).is(":focus") && iSelectionStart != undefined ? iSelectionStart : elem.selectionStart;
				var endPos = !$(elem).is(":focus") && iSelectionEnd != undefined ? iSelectionEnd : elem.selectionEnd;
				
				var beginning = elem.value.substring(0, startPos);
				var end = elem.value.substring(endPos);
				elem.value = beginning + s + end;
				elem.selectionStart = startPos;
				elem.selectionEnd = startPos + s.length;
				
				// fire change event
				if ("createEvent" in document) {
				    var evt = document.createEvent("HTMLEvents");
				    evt.initEvent("change", false, true);
				    elem.dispatchEvent(evt);
				}
				else {
					elem.fireEvent("onchange");	
				}
			}
		},
		setSelection: function (elem){
			return function(start, end) { 
				if (elem.createTextRange) {
					var selRange = elem.createTextRange();
					selRange.collapse(true);
					selRange.moveStart('character', start);
					selRange.moveEnd('character', end);
					selRange.select();
					elem.focus();
				} else if (elem.setSelectionRange) {
					elem.focus();
					elem.setSelectionRange(start, end);
				} else if (typeof elem.selectionStart != 'undefined') {
					elem.selectionStart = start;
					elem.selectionEnd = end;
					elem.focus();
				} 
			}
		},
		getWidth: function (elem){
			return function(){
				return $(elem.parentNode).width();
			}
		},
		getHeight: function (elem){
			return function(){
				return $(elem.parentNode).height();
			}
		},
		getX: function (elem){
			return function(){
				return $(elem.parentNode).position().left;
			}
		},
		getY: function (elem){
			return function(){
				return $(elem.parentNode).position().top;
			}
		}		
	}
})
.filter('htmlFilter', function() {
	return function(input) {
		if (input && input.indexOf && input.indexOf('<body') >=0 && input.lastIndexOf('</body') >=0)
		{
			input = input.substring(input.indexOf('<body')+6,input.lastIndexOf('</body'));
		}
		return input;
	};
}).filter('mnemonicletterFilter', function() {  /* this filter is used for display only*/
	return function(input,letter) {
		if(letter && input) return input.replace(letter, '<u>'+letter+'</u>');
		return input
	};
}).directive('svyFormatvldisplay',['$parse', function($parse:angular.IParseService){
	//it is similar to svy-format
	return{
		restrict:'A',
		require: 'ng-Model',
		link: function(scope,element,attrs,ngModelController: angular.INgModelController){
			var vlAccessor= $parse(attrs['svyFormatvldisplay'])
			ngModelController.$formatters.push(function(dpValue){
				var valueList = vlAccessor(scope);	
				if(valueList){
					for (var i=0;i<valueList.length;i++)
					{  
						if(valueList[i].realValue == dpValue) return valueList[i].displayValue;
					}
				}		    		 		    		 
				return dpValue;
			});
		}
	}
}]).directive('svyFormstyle',  function () {
	return {
		restrict: 'A',
		link: function (scope, element, attrs) {
			element.css({position:'absolute'});
			scope.$watch(attrs['svyFormstyle'], function(newVal) {
				if (newVal)
				{
					if(scope["formProperties"] && !scope["formProperties"]["hasExtraParts"] && isInContainer(scope)) {
        				delete newVal["minWidth"];
        				delete newVal["minHeight"];
        			}
					element.css(newVal);
				}	
			})
		}
	}

	// checks if formProperties on the scope exists	
	function isInContainer(scope) {
		var parent = scope.$parent;	
		while(parent) {
			if(parent.formProperties && parent.formStyle) return true;
			parent = parent.$parent;
		}	
		return false;
	}
}).directive("svyDecimalKeyConverter",[function(){
	return {
		restrict: 'A',
		link: function(scope,element,attrs) {
			var unreg = scope.$watch("model.format.type", function(newVal){
				if (newVal) unreg();
				if (newVal == "NUMBER") {
					function setCaretPosition(elem, caretPos) {
					    if (elem != null) {
					        if (elem.createTextRange) {
					            var range = elem.createTextRange();
					            range.move('character', caretPos);
					            range.select();
					        } else {
					            if (elem.selectionStart) {
					                elem.focus();
					                elem.setSelectionRange(caretPos, caretPos);
					            } else
					                elem.focus();
					        }
					    }
					}
					element.on("keydown",function(event) {
						if(event.which == 110) {
					        var caretPos = element[0]['selectionStart'];
					        var startString = element.val().slice(0, caretPos);
					        var endString = element.val().slice(element[0]['selectionEnd'], element.val().length);
					        element.val(startString + numeral.localeData().delimiters.decimal + endString);
					        setCaretPosition(element[0], caretPos+1); // '+1' puts the caret after the input
					        event.preventDefault ? event.preventDefault() : event.returnValue = false; //for IE8
						}
					});
				}
			})
		}
	}
}]).directive("svyComponentWrapper", ['$compile', function ($compile:angular.ICompileService) {
	return {
		priority: 1000,
		//replace: true,
		//transclude: false,
		restrict: 'E',
		//scope: false,
		compile: function compile(tElement, tAttrs, transclude) {
			var templateFragment;
			if (angular.isDefined(tAttrs['componentPropertyValue'])) {
				// automatically add standard attributes for webcomponent based on 'component' property value content
				var componentTypedPropertyValue = tAttrs['componentPropertyValue'];
				templateFragment = ' name="' + componentTypedPropertyValue + '.name" svy-model="'
				+ componentTypedPropertyValue + '.model" svy-api="'+ componentTypedPropertyValue + '.api" svy-handlers="'
				+ componentTypedPropertyValue + '.handlers" svy-servoyApi="'
				+ componentTypedPropertyValue + '.servoyApi"/>';
			} else {
				templateFragment = " ";
				// more generic - just forward all attributes to the new template
				angular.forEach(tAttrs.$attr, function(value, key) {
					if (key != 'tagname') templateFragment += ' ' + tAttrs.$attr[key] + '="' + tAttrs[key] + '"';
				});

				templateFragment += "/>";
			}

			return function (scope, element, attr, controller, transcludeFn) {
				var tagName;
				if (angular.isDefined(attr['componentPropertyValue'])) {
					// automatically get tagName of webcomponent from 'component' property value content
					tagName = scope.$eval(attr['componentPropertyValue'] + ".componentDirectiveName");
				} else {
					// more generic - attribute specified tag name
					tagName = scope.$eval(tAttrs['tagname']);
				}
				var templateElement = angular.element('<' + tagName + templateFragment);
				templateElement.append(tElement.html());
				var el = $compile(templateElement)(scope);
				element.replaceWith(el);
			}
		}
	};
}]
).factory('$svyNGEvents', ['$timeout', '$rootScope', function($timeout:angular.ITimeoutService, $rootScope:angular.IRootScopeService) {
	var requestAnimationFrame = window.requestAnimationFrame || window.mozRequestAnimationFrame || window.webkitRequestAnimationFrame || window.msRequestAnimationFrame;

	return {

		/** Sometimes you want to execute code after the DOM is processed already by Angular; for example if a component directive
  			is using jQuery plugins/code to manipulate / hide / replace DOM that is populated with Angular. That is the purpose of this function.
  			It will try to execute the given function before the browser render happens - only once. */
		afterNGProcessedDOM : function (fn, doApply) {
			if (requestAnimationFrame) {
				if (doApply) {
					requestAnimationFrame(function (scope) {
						$rootScope.$apply(fn);
					});
				} else requestAnimationFrame(fn);
			} else $timeout(fn, 0, doApply); // it could produce flicker, but better then nothing
		}
	}
}]).factory("$svyI18NService",['$sabloApplication','$q', function($sabloApplication:sablo.ISabloApplication, $q:angular.IQService) {
	var cachedMessages = {};
	var cachedPromises: { [s: string]: {promise?:angular.IPromise<{}>; value?:any}} = {};
	var defaultTranslations = {};
	return <servoy.IServoyI18NService> {
		addDefaultTranslations: function(translations) {
			angular.extend(defaultTranslations, translations);
		},
		getI18NMessages: function() {
			var retValue = {};
			var serverKeys = {};
			var serverKeysCounter = 0;
			for(var i =0;i<arguments.length;i++) {
				if (cachedMessages[arguments[i]] != null) {
					retValue[arguments[i]] = cachedMessages[arguments[i]];
				}
				else {
					serverKeys[serverKeysCounter++] = arguments[i];
				}
			}
			if (serverKeysCounter > 0) {
				var promiseA = $sabloApplication.callService("i18nService", "getI18NMessages", serverKeys,false);
				var promiseB = promiseA.then(function(result:any) {
					for(var key in result) {
						cachedMessages[key] = result[key];
						retValue[key] = result[key];
					}
					return retValue;
				}, function(error) {
					return $q.reject(error);
				});
				return promiseB;
			}
			else {
				var defered = $q.defer()
				defered.resolve(retValue);
				return defered.promise;
			}
		},
		getI18NMessage: function(key) {
			
			if (!cachedPromises[key]) {
				var promise = $sabloApplication.callService("i18nService", "getI18NMessages", {0: key}, false).
				   then(
						      function(result) {
						    	  if (promise['reject']) {
						    		  return $q.reject(result)
						    	  }
						    	  else {
							    	  var value = result[key];
							    	  cachedPromises[key] = {
							    			  value: value
							    	  };
							    	  return value;
							      }
						      },
						      function(error) {
						    	  if (!this.reject) {
						    		  delete cachedPromises[key]; // try again later
						    	  }
						    	  return $q.reject(error);
						      }
						   )
				cachedPromises[key] = {
					promise: promise
				};
			}
			// return the value when available otherwise {{'mykey' | translate }} does not display anything
			if (cachedPromises[key].hasOwnProperty('value')) {
				return cachedPromises[key].value
			}
			if (defaultTranslations[key]) {
				// return the default translation until we have a result from the server
				return defaultTranslations[key];
			}
			return cachedPromises[key].promise;
		},
		flush: function() {
			cachedMessages = {};
			for (var key in cachedPromises) {
				if (cachedPromises.hasOwnProperty(key) && cachedPromises[key].promise) {
					cachedPromises[key].promise['reject'] = true;
				}
			}
			cachedPromises = {};
		}
	}
}]
).factory('$svyUIProperties', ['webStorage', function(webStorage) {
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

	return {
		getUIProperty: function(key) {
			var value=getUiProperties()[key];
			if (value === undefined)
			{
				value = null;
			}
			return value;
		},
		setUIProperty: function(key,value) {
			var uiProps = getUiProperties();
			if (value == null) delete uiProps[key];
			else uiProps[key] = value;
			webStorage.session.set("uiProperties", JSON.stringify(uiProps))
		},
		setUIProperties: function(properties) {
			uiProperties = properties;
		}
	}
}])