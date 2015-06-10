angular.module('servoy',['sabloApp','servoyformat','servoytooltip','servoyfileupload','ui.bootstrap'])
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
}).factory("$utils",function($rootScope,$scrollbarConstants,$timeout) {

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
	
	return{

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
				if(!newVal) {element.css(cssPropertyName,''); return;}
				if(typeof newVal != 'object'){ //for cases with direct values instead of json string background and foreground
					var obj ={}
					obj[cssPropertyName] = newVal;
					newVal = obj;
				} 
				element.css(cssPropertyName,'')
				element.css(newVal)
			})
		},
		
		getScrollbarsStyleObj:function (scrollbars){
			var style = {}; 
			if ((scrollbars & $scrollbarConstants.HORIZONTAL_SCROLLBAR_NEVER) == $scrollbarConstants.HORIZONTAL_SCROLLBAR_NEVER)
			{
				style.overflowX = "hidden";
			}
			else if ((scrollbars & $scrollbarConstants.HORIZONTAL_SCROLLBAR_ALWAYS) == $scrollbarConstants.HORIZONTAL_SCROLLBAR_ALWAYS)
			{
				style.overflowX = "scroll";
			}
			else
			{
				style.overflowX = "auto";
			}

			if ((scrollbars & $scrollbarConstants.VERTICAL_SCROLLBAR_NEVER) == $scrollbarConstants.VERTICAL_SCROLLBAR_NEVER)
			{
				style.overflowY = "hidden"; 
			}
			else if ((scrollbars & $scrollbarConstants.VERTICAL_SCROLLBAR_ALWAYS) == $scrollbarConstants.VERTICAL_SCROLLBAR_ALWAYS)
			{
				style.overflowY = "scroll"; //$NON-NLS-1$
			}
			else
			{
				style.overflowY = "auto"; //$NON-NLS-1$
			}

			return style;
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
		attachEventHandler: function($parse,element,scope,svyEventHandler,domEvent, filterFunction,timeout,returnFalse, doSvyApply, dataproviderString) {
			var fn = this.getEventHandler($parse,scope,svyEventHandler)
			if (fn)
			{
				element.on(domEvent, function(event) {
					if (!filterFunction || filterFunction(event)) {
						if (!timeout) timeout = 0;
						// always use timeout because this event could be triggered by a angular call (requestFocus) thats already in a digest cycle.
						$timeout(function(){
							//svyApply before calling the handler
							if(doSvyApply)
							{
								var index = dataproviderString.indexOf('.');
								if (index > 0) {
									var modelString = dataproviderString.substring(0,index);
									var modelFunction = $parse(modelString);
									var beanModel = modelFunction(scope);
									var propertyname = dataproviderString.substring(index+1);
									var svyServoyApi = findAttribute(element, scope.$parent, "svy-servoyApi");
									if (svyServoyApi && svyServoyApi.apply) {
										svyServoyApi.apply(propertyname);
									}
								}
							}
							
							fn(scope, {$event:event});
						},timeout);
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
		bindTwoWayObjectProperty: function (a, propertyNameA, b, propertyNameB, useObjectEquality, scope) {
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
		}
	}
}).directive('ngOnChange', function($parse){
	return function(scope, elm, attrs){       
		var onChangeFunction = $parse(attrs['ngOnChange']);
		elm.bind("change", function(event) {
			scope.$apply(function() {
				onChangeFunction(scope, { $cmd: event });
			})});
	};
}).directive('svyAutoapply', function($sabloApplication, $parse, $log, $utils) {
	return {
		restrict: 'A', // only activate on element attribute
		require: '?ngModel', // get a hold of NgModelController
		link: function(scope, element, attrs, ngModel) {
			if(!ngModel || element.attr("svy-autoapply-disabled")) return; // do nothing if no ng-model

			var dataproviderString = attrs.ngModel;
			var index = dataproviderString.indexOf('.');
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
					for(key in parent.model) {
						if (parent.model[key] === beanModel) {
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
					var formname = parent.formname;
					var formParentScope = parent;
					while (!formname) {
						formParentScope = formParentScope.$parent;
						if (formParentScope) {
							formname = formParentScope.formname;
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
					// model has not been updated yet
					setTimeout(function() { 
						var svyServoyApi = $utils.findAttribute(element, parent, "svy-servoyApi");
						// use svy apply rather then pushChange because svy apply might get intercepted by components such as portals
						// that have nested child web components
						if (svyServoyApi && svyServoyApi.apply) {
							svyServoyApi.apply(propertyname);
						} else {
							// this shouldn't happen (svy-apply not being set on a web-component...)
							$log.error("cannot apply new value");
						}
					}, 0, parent, element);
				});

				// Listen for start edit
				element.bind('focus', function() {
					setTimeout(function() {
						var svyServoyApi = $utils.findAttribute(element, parent, "svy-servoyApi");
						if (svyServoyApi && svyServoyApi.startEdit) {
							svyServoyApi.startEdit(propertyname);
						} else {
							// this shouldn't happen (svy-servoyApi.startEdit not being set on a web-component...)
							if (!formName) formName = searchForFormName(); 
							$sabloApplication.callService("formService", "startEdit", {formname:formName,beanname:beanname,property:propertyname},true)
						}
					}, 0, parent, element);
				});

			}
			else {
				$log.error("svyAutoapply attached to a element that doesn't have the right ngmodel (model.value): " + dataproviderString)
			}
		}
	};
}).directive('svyEnter',  function ($parse,$utils) {
	return {
		restrict: 'A',
		link: function (scope, element, attrs) {
			$utils.attachEventHandler($parse,element,scope,attrs.svyEnter,'keydown', $utils.testEnterKey, 100, false, true, attrs.ngModel);
		}
	};
}).directive('svyChange',  function ($parse,$utils) {
	return {
		restrict: 'A',
		link: function (scope, element, attrs) {
			// timeout needed for angular to update model first
			$utils.attachEventHandler($parse,element,scope,attrs.svyChange,'change',null,100);
		}
	};
}).directive('svyClick',  function ($parse,$utils) {
	return {
		restrict: 'A',
		link: function (scope, element, attrs) {
			var dblClickFunction = $utils.getEventHandler($parse,scope,attrs.svyDblclick)
			if (dblClickFunction)
			{
				// special handling when double click is also present
				var fn = $utils.getEventHandler($parse,scope,attrs.svyClick)
				element.on('click', function(event) {
					if(element.timerID){
						clearTimeout(element.timerID);
						element.timerID=null;
						//double click, do nothing
					}
					else{
						element.timerID=setTimeout(function(){
							element.timerID=null;
							scope.$apply(function() {
								fn(scope, {$event:event});
							});
						},250)}
					return false;
				}); 
			}
			else
			{
				$utils.attachEventHandler($parse,element,scope,attrs.svyClick,'click');
			}
		}
	};
}).directive('svyDblclick',  function ($parse,$utils) {
	return {
		restrict: 'A',
		link: function (scope, element, attrs) {
			$utils.attachEventHandler($parse,element,scope,attrs.svyDblclick,'dblclick');
		}
	};
}).directive('svyRightclick',  function ($parse,$utils) {
	return {
		restrict: 'A',
		link: function (scope, element, attrs) {
			$utils.attachEventHandler($parse,element,scope,attrs.svyRightclick,'contextmenu',null,null,true);
		}
	};
}).directive('svyFocusgained',  function ($parse,$utils) {
	return {
		restrict: 'A',
		link: function (scope, element, attrs) {
			$utils.attachEventHandler($parse,element,scope,attrs.svyFocusgained,'focus');
		}
	};
}).directive('svyFocuslost',  function ($parse,$utils) {
	return {
		restrict: 'A',
		link: function (scope, element, attrs) {
			$utils.attachEventHandler($parse,element,scope,attrs.svyFocuslost,'blur', null, 100);
		}
	};
}).directive('svyBorder',  function () {
	return {
		restrict: 'A',
		link: function (scope, element, attrs) {
			scope.$watch(attrs.svyBorder,function(newVal){
				if(typeof newVal !== 'object' || newVal == null) {element.css('border',''); return;}

				if (element.parent().is("fieldset")){ 
					$(element.parent()).replaceWith($(element));//unwrap fieldset
				}
				if(newVal.type == "TitledBorder"){
					element.wrap('<fieldset style="padding:5px;margin:0px;border:1px solid silver;width:100%;height:100%"></fieldset>')
					var x = element.parent().prepend("<legend align='"+newVal.titleJustiffication+"' style='border-bottom:0px; margin:0px;width:auto;color:"+
							newVal.color+"'>"+newVal.title+"</legend>")
					x.children("legend").css(newVal.font);
				}else if(newVal.borderStyle){ 
					element.css('border','')
					element.css(newVal.borderStyle)
				}
			}, true)

		}
	};
}).directive('svyMargin',  function ($utils,$parse) {
	return {
		restrict: 'A',
		link: function (scope, element, attrs) {
			var marginModelObj= $parse(attrs.svyMargin)(scope);
			if(marginModelObj){ //only design time property, no watch
				element.css(marginModelObj);
			}
		}
	};
})
.directive('svyFont',  function ($utils) {
	return {
		restrict: 'A',
		link: function (scope, element, attrs) {
			$utils.autoApplyStyle(scope,element,attrs.svyFont,'font')
		}
	}
})
.directive('svyBackground',  function ($utils) {
	return {
		restrict: 'A',
		link: function (scope, element, attrs) {
			$utils.autoApplyStyle(scope,element,attrs.svyBackground,'backgroundColor')
		}
	}
})
.directive('svyForeground',  function ($utils) {
	return {
		restrict: 'A',
		link: function (scope, element, attrs) {
			$utils.autoApplyStyle(scope,element,attrs.svyForeground,'color')
		}
	}
})
.directive('svyScrollbars',  function ($utils,$parse) {
	return {
		restrict: 'A',
		link: function (scope, element, attrs) {
			var scrollbarsModelObj= $parse(attrs.svyScrollbars)(scope);
			element.css($utils.getScrollbarsStyleObj(scrollbarsModelObj));
		}
	}
})
.directive('svyHorizontaldirection',  function ($parse,$scrollbarConstants) {
	return {
		restrict: 'A',
		link: function (scope, element, attrs) {
			var scrollbarsModelObj= $parse(attrs.svyHorizontaldirection)(scope);
			if ((scrollbarsModelObj & $scrollbarConstants.VERTICAL_SCROLLBAR_NEVER) == $scrollbarConstants.VERTICAL_SCROLLBAR_NEVER) // vertical scrollbar never
			{
				element.css('float','left');
				element.css('margin-right','2px');
			}
		}
	}
})
.directive('svyMnemonic',  function ($utils,$parse) {
	return {
		restrict: 'A',
		link: function (scope, element, attrs) {
			var letter= $parse(attrs.svyMnemonic)(scope);
			if(letter){ //only design time property, no watch
				element.attr('accesskey',letter);
			}
		}
	}
})

.directive('svyTextrotation',  function ($utils,$parse) {
	// DESIGN TIME ONLY
	return {
		restrict: 'A',
		link: function (scope, element, attrs) {  
			var rotation= $parse(attrs.svyTextrotation)(scope);
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
		}
	}
})
.directive('svyHorizontalalignment',  function ($utils,$parse) {
	// DESIGN TIME ONLY
	return {
		restrict: 'A',
		link: function (scope, element, attrs) {  
			var halign= $parse(attrs.svyHorizontalalignment)(scope);
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
		}
	}
})
.directive('svyVerticalalignment',  function ($utils,$parse) {
	// DESIGN TIME ONLY
	return {
		restrict: 'A',
		link: function (scope, element, attrs) { 
			// see http://zerosixthree.se/vertical-align-anything-with-just-3-lines-of-css/
			// do we need preserve-3d ?
			var halign= $parse(attrs.svyVerticalalignment)(scope);
			var style ={}
			if (halign == 1)
			{
				style['top'] = 0;
			}
			else if (halign == 3)
			{
				style['bottom'] = 0;
			}
			else
			{
				style['top'] = '50%';
				style['transform'] = 'translateY(-50%)';
			}
			element.css(style);
		}
	}
})
.directive('svySelectonenter',  function ($timeout) {
	return {
		restrict: 'A',
		link: function (scope, element, attrs) {
			if (scope.$eval(attrs.svySelectonenter))
			{
				element.on('focus', function() {
					$timeout(function() {
						if (element.is(":focus"))
							element[0].select(); 
					},0);
				});
			}
		}
	};
})
.directive('svyRollovercursor',  function ($timeout) {
	return {
		restrict: 'A',
		link: function (scope, element, attrs) {
			if (scope.$eval(attrs.svyRollovercursor) == 12 /* hand_cursor */)
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
.factory("$apifunctions", function (){

	return {

		getSelectedText: function (elem){
			return function(){
				return elem.value.substr(elem.selectionStart, elem.selectionEnd - elem.selectionStart);
			}
		},
		selectAll: function (elem){
			return function(){
				elem.select();
			}
		},
		replaceSelectedText:  function (elem){
			return function(s) {
				if (typeof elem.selectionStart != 'undefined') {
					var startPos = elem.selectionStart;
					var endPos = elem.selectionEnd;
					var beginning = elem.value.substring(0, startPos);
					var end = elem.value.substring(endPos);
					elem.value = beginning + s + end;
					elem.selectionStart = startPos;
					elem.selectionEnd = startPos + s.length;
					elem.focus();
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
				return $(elem.parentNode).offset().left;
			}
		},
		getY: function (elem){
			return function(){
				return $(elem.parentNode).offset().top;
			}
		}		
	}
})
.filter('htmlFilter', ['$sce', function($sce){
	return function(input) {
		if (input && input.indexOf('<body') >=0 && input.lastIndexOf('</body') >=0)
		{
			input = input.substring(input.indexOf('<body')+6,input.lastIndexOf('</body'));
		}
		return $sce.trustAsHtml(input);;
	};
}]).filter('mnemonicletterFilter', function($sce){  /* this filter is used for display only*/
	return function(input,letter) {
		if(letter && input) return $sce.trustAsHtml(input.replace(letter, '<u>'+letter+'</u>'));
		if(input) {return $sce.trustAsHtml(''+input);}
		return input
	};
}).directive('svyFormatvldisplay',['$parse', function($parse){
	//it is similar to svy-format
	return{
		restrict:'A',
		require: 'ng-Model',
		link: function(scope,element,attrs,ngModelController){
			var vlAccessor= $parse(attrs.svyFormatvldisplay)
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
			scope.$watch(attrs.svyFormstyle, function(newVal) {
				if (newVal)
				{
					element.css(newVal)
				}	
			})
		}
	}
}).directive("svyComponentWrapper", ['$compile', function ($compile) {
	return {
		priority: 1000,
		//replace: true,
		//transclude: false,
		restrict: 'E',
		//scope: false,
		compile: function compile(tElement, tAttrs, transclude) {
			var templateFragment;
			if (angular.isDefined(tAttrs.componentPropertyValue)) {
				// automatically add standard attributes for webcomponent based on 'component' property value content
				var componentTypedPropertyValue = tAttrs.componentPropertyValue;
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
				if (angular.isDefined(attr.componentPropertyValue)) {
					// automatically get tagName of webcomponent from 'component' property value content
					tagName = scope.$eval(attr.componentPropertyValue + ".componentDirectiveName");
				} else {
					// more generic - attribute specified tag name
					tagName = scope.$eval(tAttrs.tagname);
				}
				var templateElement = angular.element('<' + tagName + templateFragment);
				templateElement.append(tElement.html());
				var el = $compile(templateElement)(scope);
				element.replaceWith(el);
			}
		}
	};
}]
).factory('$svyNGEvents', ['$timeout', '$rootScope', function($timeout, $rootScope) {
	var requestAnimationFrame = window.requestAnimationFrame || window.mozRequestAnimationFrame || window.webkitRequestAnimationFrame || window.msRequestAnimationFrame;

	return {

		/** Sometimes you want to execute code after the DOM is processed already by Angular; for example if a component directive
  			is using jQuery plugins/code to manipulate / hide / replace DOM that is populated with Angular. That is the purpose of this function.
  			It will try to execute the given function before the browser render happens - only once. */
		afterNGProcessedDOM : function (fn, doApply) {
			if (requestAnimationFrame) {
				if (doApply) {
					window.requestAnimationFrame(function (scope) {
						$rootScope.$apply(fn);
					});
				} else window.requestAnimationFrame(fn);
			} else $timeout(fn, 0, doApply); // it could produce flicker, but better then nothing
		}
	}
}])
