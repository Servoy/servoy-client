/// <reference path="../../typings/angularjs/angular.d.ts" />
/// <reference path="../../typings/numeraljs/numeraljs.d.ts" />
/// <reference path="../../typings/defaults/window.d.ts" />
/// <reference path="../../typings/sablo/sablo.d.ts" />
/// <reference path="../../typings/sablo/sablo_app.d.ts" />
/// <reference path="../../typings/servoy/servoy.d.ts" />
/// <reference path="../../typings/servoy/component.d.ts" />
/// <reference path="../../typings/servoy/foundset.d.ts" />

angular.module('servoy', ['sabloApp', 'servoyformat', 'servoytooltip', 'servoyfileupload', 'servoyalltemplates', 'ui.bootstrap', 'webSocketModule'])
    .config(["$provide", function($provide) {
        var decorator = function($delegate, $injector) {
            // this call can modify "args" (it converts them to be sent to server)
            function callServerSideApiInternal(serviceName, methodName, args) {
                const $typesRegistry: sablo.ITypesRegistry = $injector.get("$typesRegistry");
                const $sabloConverters: sablo.ISabloConverters = $injector.get("$sabloConverters");

                const serviceSpec: sablo.IWebObjectSpecification = $typesRegistry.getServiceSpecification(serviceName);
                const apiSpec = serviceSpec?.getApiFunction(methodName);

                if (args && args.length) for (var i = 0; i < args.length; i++) {
                    args[i] = $sabloConverters.convertFromClientToServer(args[i], apiSpec?.getArgumentType(i), undefined, undefined, $injector.get("$sabloUtils").PROPERTY_CONTEXT_FOR_OUTGOING_ARGS_AND_RETURN_VALUES);
                }

                const promise = $injector.get("$sabloApplication").callService('applicationServerService', 'callServerSideApi', { service: serviceName, methodName: methodName, args: args });

                return $injector.get("$webSocket").wrapPromiseToPropagateCustomRequestInfoInternal(promise, promise.then(function successCallback(serviceCallResult) {
                    return $sabloConverters.convertFromServerToClient(serviceCallResult, apiSpec?.returnType,
                        undefined, undefined, undefined, null, $injector.get("$sabloUtils").PROPERTY_CONTEXT_FOR_INCOMMING_ARGS_AND_RETURN_VALUES);
                }));

                // in case of a reject/errorCallback we just let it propagate to caller
            };
            function callResolveWhenReady(resolve){
                if ($(document).find('svy-formload').length > 0) {
                    resolve();
                }
                else{
                    setTimeout(callResolveWhenReady, 200, resolve)
                }
            }
            
            $delegate.callServerSideApi = function(serviceName, methodName, args) {
                if ($(document).find('svy-formload').length > 0) {
                    return callServerSideApiInternal(serviceName, methodName, args);
                }
                else{
                    return new Promise(resolve => setTimeout(callResolveWhenReady, 200, resolve)).then(function(){
                         return callServerSideApiInternal(serviceName, methodName, args);
                    });
                }
            };
            return $delegate;
        };
        $provide.decorator("$services", decorator);
    }])
    .value("$anchorConstants", {
        NORTH: 1,
        EAST: 2,
        SOUTH: 4,
        WEST: 8
    }).value("$scrollbarConstants", {
        SCROLLBARS_WHEN_NEEDED: 0,
        VERTICAL_SCROLLBAR_AS_NEEDED: 1,
        VERTICAL_SCROLLBAR_ALWAYS: 2,
        VERTICAL_SCROLLBAR_NEVER: 4,
        HORIZONTAL_SCROLLBAR_AS_NEEDED: 8,
        HORIZONTAL_SCROLLBAR_ALWAYS: 16,
        HORIZONTAL_SCROLLBAR_NEVER: 32
    }).value("$clientPropertyConstants", {
        WINDOW_BRANDING_ICON_32: "window.branding.icon.32",
        WINDOW_BRANDING_ICON_192: "window.branding.icon.192"
    }).factory("$svyAttributesService", function() {

        var attributeListeners: Array<servoy.IAttributesListener> = [];

        return <servoy.IAttributesService>{
            addListener: function(listener) {
                attributeListeners.push(listener);
            },
            getListeners: function() {
                return attributeListeners;
            }
        }
    }).factory("$utils", function($rootScope: angular.IRootScopeService, $timeout: angular.ITimeoutService, $svyProperties: servoy.IServoyProperties, $sabloApplication: sablo.ISabloApplication, $svyI18NService: servoy.IServoyI18NService, $log: angular.ILogService) {

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

        function testKeyPressed(e, keyCode) {
            var code;

            if (!e) e = window.event;
            if (!e) return false;
            if (e.keyCode) code = e.keyCode;
            else if (e.which) code = e.which;
            return code == keyCode;
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
            if (!attrValue) {
                var parentEl = element.parents("[" + attributeName + "]").first();
                if (parentEl && parentEl.length) {
                    attrValue = parentEl.attr(attributeName);
                    while (parentEl && parentEl.length && !parentEl.scope()) parentEl = parentEl.parent();
                    if (parentEl && parentEl.length) correctScope = parentEl.scope();
                }
            }
            if (attrValue) {
                return correctScope.$eval(attrValue);
            }
        };

        function getI18NCalendarMessages(datePicker) {
            var tooltips = datePicker.tooltips();
            var x = $svyI18NService.getI18NMessages("servoy.datetimepicker.today", "servoy.datetimepicker.clear", "servoy.datetimepicker.close",
                "servoy.datetimepicker.selectmonth", "servoy.datetimepicker.prevmonth", "servoy.datetimepicker.nextmonth",
                "servoy.datetimepicker.selectyear", "servoy.datetimepicker.prevyear", "servoy.datetimepicker.nextyear",
                "servoy.datetimepicker.selectdecade", "servoy.datetimepicker.prevdecade", "servoy.datetimepicker.nextdecade",
                "servoy.datetimepicker.prevcentury", "servoy.datetimepicker.nextcentury",
                "servoy.datetimepicker.pickhour", "servoy.datetimepicker.incrementhour", "servoy.datetimepicker.decrementhour",
                "servoy.datetimepicker.pickminute", "servoy.datetimepicker.incrementminute", "servoy.datetimepicker.decrementminute",
                "servoy.datetimepicker.picksecond", "servoy.datetimepicker.incrementsecond", "servoy.datetimepicker.decrementsecond",
                "servoy.datetimepicker.toggleperiod", "servoy.datetimepicker.selecttime");

            x.then(function(result) {
                if (result["servoy.datetimepicker.today"] != "!servoy.datetimepicker.today!") tooltips.today = result["servoy.datetimepicker.today"];
                if (result["servoy.datetimepicker.clear"] != "!servoy.datetimepicker.clear!") tooltips.clear = result["servoy.datetimepicker.clear"];
                if (result["servoy.datetimepicker.close"] != "!servoy.datetimepicker.close!") tooltips.close = result["servoy.datetimepicker.close"];
                if (result["servoy.datetimepicker.selectmonth"] != "!servoy.datetimepicker.selectmonth!") tooltips.selectMonth = result["servoy.datetimepicker.selectmonth"];
                if (result["servoy.datetimepicker.prevmonth"] != "!servoy.datetimepicker.prevmonth!") tooltips.prevMonth = result["servoy.datetimepicker.prevmonth"];
                if (result["servoy.datetimepicker.nextmonth"] != "!servoy.datetimepicker.nextmonth!") tooltips.nextMonth = result["servoy.datetimepicker.nextmonth"];
                if (result["servoy.datetimepicker.selectyear"] != "!servoy.datetimepicker.selectyear!") tooltips.selectYear = result["servoy.datetimepicker.selectyear"];
                if (result["servoy.datetimepicker.prevyear"] != "!servoy.datetimepicker.prevyear!") tooltips.prevYear = result["servoy.datetimepicker.prevyear"];
                if (result["servoy.datetimepicker.nextyear"] != "!servoy.datetimepicker.nextyear!") tooltips.nextYear = result["servoy.datetimepicker.nextyear"];
                if (result["servoy.datetimepicker.selectdecade"] != "!servoy.datetimepicker.selectdecade!") tooltips.selectDecade = result["servoy.datetimepicker.selectdecade"];
                if (result["servoy.datetimepicker.prevdecade"] != "!servoy.datetimepicker.prevdecade!") tooltips.prevDecade = result["servoy.datetimepicker.prevdecade"];
                if (result["servoy.datetimepicker.nextdecade"] != "!servoy.datetimepicker.nextdecade!") tooltips.nextDecade = result["servoy.datetimepicker.nextdecade"];
                if (result["servoy.datetimepicker.prevcentury"] != "!servoy.datetimepicker.prevcentury!") tooltips.prevCentury = result["servoy.datetimepicker.prevcentury"];
                if (result["servoy.datetimepicker.nextcentury"] != "!servoy.datetimepicker.nextcentury!") tooltips.nextCentury = result["servoy.datetimepicker.nextcentury"];
                if (result["servoy.datetimepicker.pickhour"] != "!servoy.datetimepicker.pickhour!") tooltips.pickHour = result["servoy.datetimepicker.pickhour"];
                if (result["servoy.datetimepicker.incrementhour"] != "!servoy.datetimepicker.incrementhour!") tooltips.incrementHour = result["servoy.datetimepicker.incrementhour"];
                if (result["servoy.datetimepicker.decrementhour"] != "!servoy.datetimepicker.decrementhour!") tooltips.decrementHour = result["servoy.datetimepicker.decrementhour"];
                if (result["servoy.datetimepicker.pickminute"] != "!servoy.datetimepicker.pickminute!") tooltips.pickMinute = result["servoy.datetimepicker.pickminute"];
                if (result["servoy.datetimepicker.incrementminute"] != "!servoy.datetimepicker.incrementminute!") tooltips.incrementMinute = result["servoy.datetimepicker.incrementminute"];
                if (result["servoy.datetimepicker.decrementminute"] != "!servoy.datetimepicker.decrementminute!") tooltips.decrementMinute = result["servoy.datetimepicker.decrementminute"];
                if (result["servoy.datetimepicker.picksecond"] != "!servoy.datetimepicker.picksecond!") tooltips.pickSecond = result["servoy.datetimepicker.picksecond"];
                if (result["servoy.datetimepicker.incrementsecond"] != "!servoy.datetimepicker.incrementsecond!") tooltips.incrementSecond = result["servoy.datetimepicker.incrementsecond"];
                if (result["servoy.datetimepicker.decrementsecond"] != "!servoy.datetimepicker.decrementsecond!") tooltips.decrementSecond = result["servoy.datetimepicker.decrementsecond"];
                if (result["servoy.datetimepicker.toggleperiod"] != "!servoy.datetimepicker.toggleperiod!") tooltips.togglePeriod = result["servoy.datetimepicker.toggleperiod"];
                if (result["servoy.datetimepicker.selecttime"] != "!servoy.datetimepicker.selecttime!") tooltips.selectTime = result["servoy.datetimepicker.selecttime"];
                datePicker.tooltips(tooltips);

            })
        };

        return <servoy.IUtils>{

            /** this function can be used in filters .It accepts a string jsonpath the property to test for null. 
            Example: "item in  model.valuelistID  | filter:notNullOrEmpty('realValue')"*/
            notNullOrEmpty: function(propPath) {
                return function(item) {
                    var propByStringPath = getPropByStringPath(item, propPath);
                    return !(propByStringPath === null || propByStringPath === '')
                }
            },

            notNullOrEmptyValueListItem: function() {
                return function(item: { displayValue: any; realValue: any }) {
                    if (item) {
                        return !(item.displayValue === null || item.displayValue === '')
                            || !(item.realValue === null || item.realValue === '')
                    }
                    return false;
                }
            },

            autoApplyStyle: function(scope, element, modelToWatch, cssPropertyName) {
                scope.$watch(modelToWatch, function(newVal, oldVal) {
                    $svyProperties.setCssProperty(element, cssPropertyName, newVal);
                })
            },


            getEventHandler: function($parse, scope, svyEventHandler) {
                var functionReferenceString = svyEventHandler;
                if (functionReferenceString) {
                    var index = functionReferenceString.indexOf('(');
                    if (index != -1) functionReferenceString = functionReferenceString.substring(0, index);
                    if (scope.$eval(functionReferenceString)) {
                        return $parse(svyEventHandler);
                    }
                }
                return null;
            },
            attachEventHandler: function($parse, element, scope, svyEventHandler, domEvent, filterFunction, timeout, returnFalse, doSvyApply, dataproviderString, preHandlerCallback) {
                var fn = this.getEventHandler($parse, scope, svyEventHandler)
                if (fn) {
                    element.on(domEvent, function(event) {
                        if (!filterFunction || filterFunction(event)) {

                            function executeHandler() {
                                if (preHandlerCallback) {
                                    preHandlerCallback();
                                }
                                //svyApply before calling the handler
                                if (doSvyApply && dataproviderString) {
                                    var index = dataproviderString.indexOf('.');
                                    if (index > 0) {
                                        var propertyname = dataproviderString.substring(index + 1);
                                        var svyServoyApi = findAttribute(element, scope.$parent, "svy-servoyApi");
                                        if (svyServoyApi && svyServoyApi.apply) {
                                            svyServoyApi.apply(propertyname);
                                        }
                                    }
                                }
                                fn(scope, { $event: event });
                            };
                            // always use timeout or evalAsync because this event could be triggered by a angular call (requestFocus) thats already in a digest cycle.
                            if (!timeout)
                                scope.$evalAsync(executeHandler);
                            else
                                $timeout(executeHandler, timeout);

                            if (returnFalse) return false;
                            return true;
                        }
                    });
                }
            },
            testEnterKey: function(e) {
                return testKeyPressed(e, 13);
            },

            bindTwoWayObjectProperty: function(a, propertyNameA: string, b, propertyNameB: string, useObjectEquality: boolean, scope: angular.IScope): [() => void, () => void] {
                var toWatchA = getInDepthWatchExpression(a, propertyNameA);
                var toWatchB = getInDepthWatchExpression(b, propertyNameB);
                var setA = getInDepthSetter(a, propertyNameA);
                var setB = getInDepthSetter(b, propertyNameB);

                if (!scope) scope = $rootScope;
                return [
                    scope.$watch(toWatchA, function(newValue, oldValue, scope) {
                        var nV = (newValue instanceof Date) ? newValue.getTime() : newValue;
                        var oV = (oldValue instanceof Date) ? oldValue.getTime() : oldValue;
                        if (nV !== oV) {
                            setB(newValue);
                        }
                    }, useObjectEquality),
                    scope.$watch(toWatchB, function(newValue, oldValue, scope) {
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
            findAttribute: function(element, parent, attributeName) {
                return findAttribute(element, parent, attributeName);
            },

            createJSEvent: function(event, eventType, contextFilter, contextFilterElement) {
                if (!event) {
                    if (contextFilter || contextFilterElement) return null;
                    $log.error("createJSEvent: event is undefined, returning default event");
                    return { svyType: 'JSEvent', eventType: eventType, "timestamp": new Date().getTime() };
                }
                var targetEl = event;
                if (event.target) targetEl = event.target;
                else if (event.srcElement) targetEl = event.srcElement;

                var form;
                var parent = targetEl;
                var targetElNameChain = new Array();
                var contextMatch = false;
                while (parent && parent.getAttribute) {
                    form = parent.getAttribute("ng-controller");
                    if (form) {
                        //global shortcut or context match
                        var shortcuthit = !contextFilter || (contextFilter && form == contextFilter);
                        if (!shortcuthit) break;
                        contextMatch = true;
                        break;
                    }
                    if (parent.getAttribute("name")) targetElNameChain.push(parent.getAttribute("name"));
                    parent = parent.parentNode;
                }
                if (!form || form == 'MainController') {
                    // form not found, search for an active dialog
                    var formInDialog = $('.svy-dialog.window.active').find("svy-formload").attr("formname");
                    if (formInDialog) form = formInDialog;
                }

                if (!contextMatch) return null;

                var jsEvent = { svyType: 'JSEvent', eventType: eventType, "timestamp": new Date().getTime() };

                var modifiers = (event.altKey ? 8 : 0) | (event.shiftKey ? 1 : 0) | (event.ctrlKey ? 2 : 0) | (event.metaKey ? 4 : 0);
                jsEvent['modifiers'] = modifiers;
                jsEvent['x'] = event.pageX;
                jsEvent['y'] = event.pageY;


                if (form != 'MainController') {
                    jsEvent['formName'] = form;
                    var formScope = angular.element(parent).scope();
                    for (var i = 0; i < targetElNameChain.length; i++) {
                        if (formScope['model'][targetElNameChain[i]]) {
                            jsEvent['elementName'] = targetElNameChain[i];
                            break;
                        } else {
                            // form component element
                            const nameParts = targetElNameChain[i].split('$');
                            if (nameParts.length === 3) {
                                jsEvent['elementName'] = targetElNameChain[i];
                                break;
                            }
                        }
                    }

                    if (contextFilterElement && (contextFilterElement != jsEvent['elementName'])) {
                        return null;
                    }
                }
                return jsEvent;
            },

            generateUploadUrl: function(formname, beanname, propertyName) {
                return "resources/upload/" + $sabloApplication.getClientnr() +
                    (formname ? "/" + formname : "") +
                    (beanname ? "/" + beanname : "") +
                    (propertyName ? "/" + propertyName : "");
            },

            generateServiceUploadUrl: function(serviceName, apiFunctionName) {
                // svy_services should be in sync with MediaResourceServlet.SERVICE_UPLOAD
                return "resources/upload/" + $sabloApplication.getClientnr() + "/svy_services/" + serviceName + "/" + apiFunctionName;
            },

            getI18NCalendarMessages: function(datePicker: object) {
                return getI18NCalendarMessages(datePicker);
            }
        }
    }).factory("$svyProperties", function($svyTooltipUtils, $timeout: angular.ITimeoutService, $scrollbarConstants, $svyUIProperties) {
        return <servoy.IServoyProperties>{
            setBorder: function(element, newVal) {
                if (typeof newVal !== 'object' || newVal == null) { element.css('border', ''); return; }

                if (element.parent().is("fieldset")) {
                    $(element.parent()).replaceWith($(element));//unwrap fieldset
                }
                if (newVal.type == "TitledBorder") {
                    element.wrap('<fieldset style="padding:5px;margin:0px;border:1px solid silver;width:100%;height:100%"></fieldset>')
                    var x = element.parent().prepend("<legend align='" + newVal.titleJustiffication + "' style='border-bottom:0px; margin:0px;width:auto;color:" +
                        newVal.color + "'>" + newVal.title + "</legend>")
                    if (newVal.font) x.children("legend").css(newVal.font);
                } else if (newVal.borderStyle) {
                    element.css('border', '')
                    element.css(newVal.borderStyle)
                }
            },
            setCssProperty: function(element, cssPropertyName, newVal) {
                if (!newVal) { element.css(cssPropertyName, ''); return; }
                if (typeof newVal != 'object') { //for cases with direct values instead of json string background and foreground
                    var obj = {}
                    obj[cssPropertyName] = newVal;
                    newVal = obj;
                }
                element.css(cssPropertyName, '')
                element.css(newVal)
            },
            setHorizontalAlignment: function(element, halign) {
                if (halign != -1) {
                    var style = {}
                    if (halign == 0) {
                        style['text-align'] = 'center';
                    }
                    else if (halign == 4) {
                        style['text-align'] = 'right';
                    }
                    else {
                        style['text-align'] = 'left';
                    }
                    element.css(style);
                }
            },
            setHorizontalAlignmentFlexbox: function(element, halign) {
                if (halign != -1) {
                    var style = {}
                    if (halign == 0) {
                        style['-ms-flex-pack'] = 'center';
                        style['justify-content'] = 'center';
                    }
                    else if (halign == 4) {
                        style['-ms-flex-pack'] = 'end';
                        style['justify-content'] = 'flex-end';
                    }
                    else {
                        style['-ms-flex-pack'] = 'start';
                        style['justify-content'] = 'flex-start';
                    }
                    element.css(style);
                }
            },
            setVerticalAlignment: function(element, valign) {
                var style = {}
                if (valign == 1) {
                    style['top'] = 0;
                }
                else if (valign == 3) {
                    style['top'] = '100%';
                    style['transform'] = 'translateY(-100%)';
                }
                else {
                    style['top'] = '50%';
                    style['transform'] = 'translateY(-50%)';
                }
                element.css(style);
            },
            setRotation: function(element, scope, rotation) {
                if (rotation && rotation != 0) {
                    var r = 'rotate(' + rotation + 'deg)';
                    var style = {}
                    style['-moz-transform'] = r;
                    style['-webkit-transform'] = r;
                    style['-o-transform'] = r;
                    style['-ms-transform'] = r;
                    style['transform'] = r;
                    style['position'] = 'absolute';
                    if (rotation == 90 || rotation == 270) {
                        style['width'] = scope.model.size.height + 'px';
                        style['height'] = scope.model.size.width + 'px';
                        style['left'] = (scope.model.size.width - scope.model.size.height) / 2 + 'px';
                        style['top'] = (scope.model.size.height - scope.model.size.width) / 2 + 'px';
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
                    }, 0);
                });
            },
            getScrollbarsStyleObj: function(scrollbars) {
                var style = {};
                if ((scrollbars & $scrollbarConstants.HORIZONTAL_SCROLLBAR_NEVER) == $scrollbarConstants.HORIZONTAL_SCROLLBAR_NEVER) {
                    style['overflowX'] = "hidden";
                }
                else if ((scrollbars & $scrollbarConstants.HORIZONTAL_SCROLLBAR_ALWAYS) == $scrollbarConstants.HORIZONTAL_SCROLLBAR_ALWAYS) {
                    style['overflowX'] = "scroll";
                }
                else {
                    style['overflowX'] = "auto";
                }

                if ((scrollbars & $scrollbarConstants.VERTICAL_SCROLLBAR_NEVER) == $scrollbarConstants.VERTICAL_SCROLLBAR_NEVER) {
                    style['overflowY'] = "hidden";
                }
                else if ((scrollbars & $scrollbarConstants.VERTICAL_SCROLLBAR_ALWAYS) == $scrollbarConstants.VERTICAL_SCROLLBAR_ALWAYS) {
                    style['overflowY'] = "scroll"; //$NON-NLS-1$
                }
                else {
                    style['overflowY'] = "auto"; //$NON-NLS-1$
                }

                return style;
            },
            setScrollbars: function(element, value) {
                element.css(this.getScrollbarsStyleObj(value));
            },
            createTooltipState: function(element, value) {
                var tooltip = value;
                var initialDelay = $svyUIProperties.getUIProperty("tooltipInitialDelay");
                if (initialDelay === null || isNaN(initialDelay)) initialDelay = 750;
                var dismissDelay = $svyUIProperties.getUIProperty("tooltipDismissDelay");
                if (dismissDelay === null || isNaN(dismissDelay)) dismissDelay = 5000;

                function doShow(event) {
                    var tooltipText = typeof tooltip === 'function' ? tooltip() : tooltip;
                    if (tooltipText) {
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
    }).directive('ngOnChange', function($parse: angular.IParseService) {
        return function(scope, elm, attrs) {
            var onChangeFunction = $parse(attrs['ngOnChange']);
            elm.bind("change", function(event) {
                scope.$apply(function() {
                    onChangeFunction(scope, { $cmd: event });
                })
            });
        };
    }).directive('svyAutoapply', function($sabloApplication: sablo.ISabloApplication, $parse: angular.IParseService, $log: angular.ILogService, $utils: servoy.IUtils) {
        return {
            restrict: 'A', // only activate on element attribute
            require: '?ngModel', // get a hold of NgModelController
            link: function(scope, element, attrs, ngModel: angular.INgModelController) {
                if (!ngModel || element.attr("svy-autoapply-disabled")) return; // do nothing if no ng-model
                var dataproviderString = attrs['ngModel'];
                var index = 0;

                var splitDB = dataproviderString.split('.');
                var scopeObj = scope;
                var modelFound = false;
                for (var i = 0; (i < splitDB.length) && scopeObj; i++) {
                    scopeObj = scopeObj[splitDB[i]];
                    index += splitDB[i].length + (i ? 1 : 0);
                    if (scopeObj instanceof sablo_app.Model) {
                        modelFound = true;
                        break;
                    }
                }

                if (!modelFound) {
                    index = dataproviderString.indexOf('.');
                }

                if (index > 0) {
                    var modelString = dataproviderString.substring(0, index);
                    var modelFunction = $parse(modelString);
                    var beanModel = modelFunction(scope);
                    var propertyname = dataproviderString.substring(index + 1);
                    var beanname;
                    var parent = scope.$parent;

                    beanname = element.attr("name");
                    if (!beanname) {
                        var nameParentEl = element.parents("[name]").first();
                        if (nameParentEl) beanname = nameParentEl.attr("name");
                    }
                    if (!beanname) {
                        for (var key in parent['model']) {
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
                        scope.$evalAsync(function() {
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
                        scope.$evalAsync(function() {
                            var svyServoyApi = $utils.findAttribute(element, parent, "svy-servoyApi");
                            if (svyServoyApi && svyServoyApi.startEdit) {
                                svyServoyApi.startEdit(propertyname);
                            } else {
                                // this shouldn't happen (svy-servoyApi.startEdit not being set on a web-component...)
                                if (!formName) formName = searchForFormName();
                                $sabloApplication.callService("formService", "startEdit", { formname: formName, beanname: beanname, property: propertyname }, true)
                            }
                        });
                    });

                }
                else {
                    $log.error("svyAutoapply attached to a element that doesn't have the right ngmodel (model.value): " + dataproviderString)
                }
            }
        };
    }).directive('svyEnter', function($parse: angular.IParseService, $utils: servoy.IUtils) {
        return {
            restrict: 'A',
            link: function(scope: angular.IScope, element: JQuery, attrs) {
                $utils.attachEventHandler($parse, element, scope, attrs['svyEnter'], 'keydown', $utils.testEnterKey, 100, false, true, attrs['ngModel'] ? attrs['ngModel'] : element.parent().attr('ng-model'));
            }
        };
    }).directive('svyChange', function($parse: angular.IParseService, $utils: servoy.IUtils) {
        return {
            restrict: 'A',
            link: function(scope, element, attrs) {
                // timeout needed for angular to update model first
                $utils.attachEventHandler($parse, element, scope, attrs['svyChange'], 'change', null, 100);
            }
        };
    }).directive('svyClick', function($parse: angular.IParseService, $utils: servoy.IUtils) {
        return {
            restrict: 'A',
            link: function(scope, element, attrs) {
                var dblClickFunction = $utils.getEventHandler($parse, scope, attrs['svyDblclick'])
                if (dblClickFunction) {
                    // special handling when double click is also present
                    var fn = $utils.getEventHandler($parse, scope, attrs['svyClick'])
                    if (fn) {
                        element.on('click', function(event) {
                            if (element['timerID']) {
                                clearTimeout(element['timerID']);
                                element['timerID'] = null;
                                //double click, do nothing
                            }
                            else {
                                element['timerID'] = setTimeout(function() {
                                    element['timerID'] = null;
                                    scope.$apply(function() {
                                        fn(scope, { $event: event });
                                    });
                                }, 250)
                            }
                        });
                    }
                }
                else {
                    $utils.attachEventHandler($parse, element, scope, attrs['svyClick'], 'click');
                }
            }
        };
    }).directive('svyDblclick', function($parse: angular.IParseService, $utils: servoy.IUtils) {
        return {
            restrict: 'A',
            link: function(scope, element, attrs) {
                $utils.attachEventHandler($parse, element, scope, attrs['svyDblclick'], 'dblclick');
            }
        };
    }).directive('svyRightclick', function($parse: angular.IParseService, $utils: servoy.IUtils) {
        return {
            restrict: 'A',
            link: function(scope, element, attrs) {
                $utils.attachEventHandler($parse, element, scope, attrs['svyRightclick'], 'contextmenu', null, null, true);
            }
        };
    }).directive('svyFocusgained', function($parse: angular.IParseService, $utils: servoy.IUtils) {
        return {
            restrict: 'A',
            link: function(scope, element, attrs) {
                $utils.attachEventHandler($parse, element, scope, attrs['svyFocusgained'], 'focus');
            }
        };
    }).directive('svyFocuslost', function($parse: angular.IParseService, $utils: servoy.IUtils) {
        return {
            restrict: 'A',
            link: function(scope, element, attrs) {
                $utils.attachEventHandler($parse, element, scope, attrs['svyFocuslost'], 'blur');
            }
        };
    }).directive('svyBorder', function($svyProperties: servoy.IServoyProperties) {
        return {
            restrict: 'A',
            link: function(scope, element, attrs) {
                scope.$watch(attrs['svyBorder'], function(newVal) {
                    $svyProperties.setBorder(element, newVal);
                }, true)

            }
        };
    }).directive('svyMargin', function($utils: servoy.IUtils, $parse: angular.IParseService) {
        return {
            restrict: 'A',
            link: function(scope, element, attrs) {
                var marginModelObj = $parse(attrs['svyMargin'])(scope);
                if (marginModelObj) { //only design time property, no watch
                    element.css(marginModelObj);
                }
            }
        };
    })
    .directive('svyFont', function($utils: servoy.IUtils) {
        return {
            restrict: 'A',
            link: function(scope, element, attrs) {
                $utils.autoApplyStyle(scope, element, attrs['svyFont'], 'font')
            }
        }
    })
    .directive('svyBackground', function($utils: servoy.IUtils) {
        return {
            restrict: 'A',
            link: function(scope, element, attrs) {
                $utils.autoApplyStyle(scope, element, attrs['svyBackground'], 'backgroundColor')
            }
        }
    })
    .directive('svyForeground', function($utils: servoy.IUtils) {
        return {
            restrict: 'A',
            link: function(scope, element, attrs) {
                $utils.autoApplyStyle(scope, element, attrs['svyForeground'], 'color')
            }
        }
    })
    .directive('svyScrollbars', function($svyProperties: servoy.IServoyProperties, $parse: angular.IParseService, $scrollbarConstants) {
        return {
            restrict: 'A',
            link: function(scope, element, attrs) {
                var scrollbarsModelObj = $parse(attrs['svyScrollbars'])(scope);
                if (scrollbarsModelObj === $scrollbarConstants.SCROLLBARS_WHEN_NEEDED || scrollbarsModelObj === null || scrollbarsModelObj === undefined) {
                    // default value, add from css, not inline style
                    element.addClass('svy-overflow-auto');
                }
                else {
                    element.css($svyProperties.getScrollbarsStyleObj(scrollbarsModelObj));
                }
            }
        }
    })
    .directive('svyHorizontaldirection', function($parse: angular.IParseService, $scrollbarConstants) {
        return {
            restrict: 'A',
            link: function(scope, element, attrs) {
                var scrollbarsModelObj = $parse(attrs['svyHorizontaldirection'])(scope);
                if ((scrollbarsModelObj & $scrollbarConstants.VERTICAL_SCROLLBAR_NEVER) == $scrollbarConstants.VERTICAL_SCROLLBAR_NEVER) // vertical scrollbar never
                {
                    element.css('float', 'left');
                    element.css('margin-right', '2px');
                }
            }
        }
    })
    .directive('svyMnemonic', function($utils: servoy.IUtils, $parse: angular.IParseService) {
        return {
            restrict: 'A',
            link: function(scope, element, attrs) {
                var letter = $parse(attrs['svyMnemonic'])(scope);
                if (letter) { //only design time property, no watch
                    element.attr('accesskey', letter);
                }
            }
        }
    })
    .directive('svyAttributes', function($parse: angular.IParseService, $svyAttributesService: servoy.IAttributesService) {
        return {
            restrict: 'A',
            link: function(scope, element, attrs) {
                var attributes = $parse(attrs['svyAttributes'])(scope);
                if (attributes) { //only design time property, no watch
                    for (var key in attributes) {
                        element.attr(key, attributes[key]);
                    }
                    var listeners = $svyAttributesService.getListeners();
                    if (listeners) {
                        for (var i = 0; i < listeners.length; i++) {
                            listeners[i].attributesAdded(element, attributes, scope, attrs);
                        }
                    }
                }
            }
        }
    })
    .directive('svyTextrotation', function($utils: servoy.IUtils, $parse: angular.IParseService, $svyProperties: servoy.IServoyProperties) {
        // DESIGN TIME ONLY
        return {
            restrict: 'A',
            link: function(scope: angular.IScope & { model: { size: { height: number, width: number } } }, element: JQuery, attrs) {
                var rotation = $parse(attrs['svyTextrotation'])(scope);
                $svyProperties.setRotation(element, scope, rotation);
            }
        }
    })
    .directive('svyHorizontalalignment', function($utils: servoy.IUtils, $parse: angular.IParseService, $svyProperties: servoy.IServoyProperties) {
        // DESIGN TIME ONLY
        return {
            restrict: 'A',
            link: function(scope, element, attrs) {
                var halign = $parse(attrs['svyHorizontalalignment'])(scope);
                $svyProperties.setHorizontalAlignment(element, halign);
            }
        }
    })
    .directive('svyVerticalalignment', function($utils: servoy.IUtils, $parse: angular.IParseService, $svyProperties: servoy.IServoyProperties) {
        // DESIGN TIME ONLY
        return {
            restrict: 'A',
            link: function(scope, element, attrs) {
                // see http://zerosixthree.se/vertical-align-anything-with-just-3-lines-of-css/
                // do we need preserve-3d ?
                var valign = $parse(attrs['svyVerticalalignment'])(scope);
                $svyProperties.setVerticalAlignment(element, valign);
            }
        }
    })
    .directive('svySelectonenter', function($timeout: angular.ITimeoutService, $svyProperties: servoy.IServoyProperties) {
        return {
            restrict: 'A',
            link: function(scope, element, attrs) {
                if (scope.$eval(attrs['svySelectonenter'])) {
                    $svyProperties.addSelectOnEnter(element);
                }
            }
        };
    })
    .directive('svyRollovercursor', function($timeout: angular.ITimeoutService) {
        return {
            restrict: 'A',
            link: function(scope, element, attrs) {
                if (scope.$eval(attrs['svyRollovercursor']) == 12 /* hand_cursor */) {
                    element.css('cursor', 'pointer');
                }
                else {
                    element.css('cursor', 'default');
                }
            }
        };
    })
    .directive('svyFormComponent', function($utils, $compile: angular.ICompileService, $templateCache, $foundsetTypeConstants: foundsetType.FoundsetTypeConstants, $sabloConstants, $timeout: angular.ITimeoutService, $webSocket: sablo.IWebSocket, $applicationService, $anchorConstants, $formService) {
        return {
            restrict: 'A',
            scope: {
                svyFormComponent: "=svyFormComponent",
                foundset: "=foundset",
                responsivePageSize: "=responsivePageSize",
                pageLayout: "=pageLayout",
                selectionClass: "=selectionClass",
                rowStyleClass: "=rowStyleClass",
                rowStyleClassDataprovider: "=rowStyleClassDataprovider",
                paginationStyleClass: "=paginationStyleClass",
                selectionChangedHandler: "="
            },
            link: function(scope: any, element, attrs) {
                interface IServoyScopeInternal extends servoy.IServoyScope {
                    createdChildElements?: number;
                    rowID?: object;
                }

                let svyServoyApi = scope.svyServoyapi ? scope.svyServoyapi : scope.$parent.svyServoyapi;
                if (!svyServoyApi) svyServoyApi = $utils.findAttribute(element, scope.$parent, "svy-servoyApi");
                if (svyServoyApi.isInDesigner()) {
                    // in designer just show it as a normal form component
                    const newValue = scope.svyFormComponent
                    if (newValue) {
                        element.empty();
                        const elements = svyServoyApi.getFormComponentElements(newValue.startName, newValue);

                        if (newValue.absoluteLayout) {
                            const height = newValue.formHeight;
                            const width = newValue.formWidth;
                            var template = "<div style='position:relative;";
                            if (height) template += "height:" + height + "px;"
                            if (width) template += "width:" + width + "px;"
                            template += "'";
                            template += "></div>";
                            var div = $compile(template)(scope);
                            div.append(elements);
                            element.append(div);
                        } else { // is responsive
                            element.append(elements);
                        }
                    }
                }
                else {
                    let selectionChangedByKey = false;
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

                    scope.onRowClick = function(rowID) {
                        for (let i = 0; i < scope.foundset.viewPort.rows.length; i++) {
                            if (scope.foundset.viewPort.rows[i][$foundsetTypeConstants.ROW_ID_COL_KEY] == rowID) {
                                scope.foundset.requestSelectionUpdate([i + scope.foundset.viewPort.startIndex]);
                                break;
                            }
                        }
                    }

                    scope.getRowStyleClassDataprovider = function(rowID) {
                        for (let i = 0; i < scope.foundset.viewPort.rows.length; i++) {
                            if (scope.foundset.viewPort.rows[i][$foundsetTypeConstants.ROW_ID_COL_KEY] == rowID) {
                                return scope.rowStyleClassDataprovider && i < scope.rowStyleClassDataprovider.length ? scope.rowStyleClassDataprovider[i] : '';
                            }
                        }
                        return '';
                    }

                    scope.onKeydown = function(event, rowID) {
                        const keycode = event.originalEvent.keyCode;
                        if (!getFoundset().multiSelect && keycode == 38 || keycode == 40) {
                            let selectedRowIndex = getFoundset().selectedRowIndexes[0]; // it starts from 0
                            if (keycode == 38) { // keyup
                                // move to the previous page if the first element (not from the first page) is selected
                                if (page != 0 && selectedRowIndex / (page) == scope.responsivePageSize) {
                                    scope.moveLeft();
                                }
                                selectedRowIndex--;
                            } else if (keycode == 40) { // keydown
                                selectedRowIndex++;
                                // move to the next page if the last element (not from the last page) is selected
                                if (selectedRowIndex / (page + 1) == scope.responsivePageSize) {
                                    scope.moveRight();
                                }
                            }
                            // do not move the selection for the first or last element 
                            if (selectedRowIndex >= 0 && selectedRowIndex < getFoundset().serverSize) {
                                scope.foundset.requestSelectionUpdate([selectedRowIndex]);
                                selectionChangedByKey = true;
                            }
                        }
                    }

                    const parent = element.parent();
                    const rowToModel: Array<IServoyScopeInternal> = [];
                    const pager = $compile(angular.element("<div class='svyPagination' ng-class=\"paginationStyleClass !== undefined ?paginationStyleClass : ''\"><div style='text-align:center;cursor:pointer;visibility:hidden;display:inline;padding:3px;white-space:nowrap;vertical-align:middle;background-color:rgb(255, 255, 255, 0.6);' ng-click='firstPage()' ><i class='glyphicon glyphicon-backward'></i></div><div style='text-align:center;cursor:pointer;visibility:hidden;display:inline;padding:3px;white-space:nowrap;vertical-align:middle;background-color:rgb(255, 255, 255, 0.6);' ng-click='moveLeft()' ><i class='glyphicon glyphicon-chevron-left'></i></div><div style='text-align:center;cursor:pointer;visibility:hidden;display:inline;padding:3px;white-space:nowrap;vertical-align:middle;background-color:rgb(255, 255, 255, 0.6);' ng-click='moveRight()'><i class='glyphicon glyphicon-chevron-right'></i></div></div>"))(scope);

                    let template = null;
                    function copyRecordProperties(childElement, rowModel, viewportIndex, notify) {
                        if (childElement.foundsetConfig && childElement.foundsetConfig.recordBasedProperties) {
                            childElement.foundsetConfig.recordBasedProperties.forEach((value) => {
                                rowModel[value] = childElement.modelViewport[viewportIndex][value];
                                if (notify) {
                                    rowToModel[viewportIndex].model[childElement.name][$sabloConstants.modelChangeNotifier](value, childElement.modelViewport[viewportIndex][value]);
                                }
                            });
                        }
                    };

                    function destroyScopes(array: Array<servoy.IServoyScope>) {
                        array.forEach(scope => scope.$destroy());
                        array.length = 0;
                    }

                    function createRows() {
                        numberOfCells = svyServoyApi.isInAbsoluteLayout() && scope.svyFormComponent.absoluteLayout ? 0 : scope.responsivePageSize;
                        if (numberOfCells <= 0) {
                            if (svyServoyApi.isInAbsoluteLayout() && scope.svyFormComponent.absoluteLayout) {
                                const parentWidth = parent.outerWidth();
                                const parentHeight = parent.outerHeight();
                                const height = scope.svyFormComponent.formHeight;
                                const width = scope.svyFormComponent.formWidth;
                                const numberOfColumns = (scope.pageLayout == 'listview') ? 1 : Math.floor(parentWidth / width);
                                const numberOfRows = Math.floor(parentHeight / height);
                                numberOfCells = numberOfRows * numberOfColumns;
                                // always just render 1
                                if (numberOfCells < 1) numberOfCells = 1;
                            }
                            else {
                                if (!svyServoyApi.isInAbsoluteLayout()) {
                                    parent.append(angular.element("<span>responsivePageSize property must be set when using a list form component in a responsive form</span>"));
                                }
                                else if (!scope.svyFormComponent.absoluteLayout) {
                                    parent.append(angular.element("<span>responsivePageSize property must be set when using a list form component with a responsive containedForm</span>"));
                                }
                                return;
                            }
                        }
                        const startIndex = page * numberOfCells;
                        if (scope.foundset.viewPort.startIndex != startIndex) {
                            scope.foundset.loadRecordsAsync(startIndex, numberOfCells);
                        } else {
                            destroyScopes(rowToModel);
                            parent.children("[svy-form-component-clone]").remove();
                            const maxRows = Math.min(numberOfCells, scope.foundset.viewPort.rows.length);
                            for (let i = 0; i < maxRows; i++) {
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
                        scope.foundset.setPreferredViewportSize(numberOfCells);
                    }
                    function updatePagingControls() {
                        const pagerChildren = pager.children();
                        pagerChildren.css("cursor", "pointer");
                        pagerChildren.first().css("visibility", page > 0 ? "visible" : "hidden");
                        pagerChildren.eq(1).css("visibility", page > 0 ? "visible" : "hidden");
                        const showNext = scope.foundset.hasMoreRows || (scope.foundset.serverSize - (page * numberOfCells + Math.min(numberOfCells, scope.foundset.viewPort.rows.length))) > 0;
                        pagerChildren.last().css("visibility", showNext ? "visible" : "hidden");
                    }


                    function createChildElementForRow(index: number, row: IServoyScopeInternal, childElement: componentType.ComponentPropertyValue) {
                        function Model() {
                        }
                        Model.prototype = childElement.model;

                        function ServoyApi(rowModel, row: IServoyScopeInternal) {
                            this.apply = (property) => {
                                childElement.servoyApi.apply(property, rowModel, row.rowID);
                            }
                            this.startEdit = (property) => {
                                childElement.servoyApi.startEdit(property, row.rowID)
                            }
                            this.trustAsHtml = () => {
                                return $applicationService.trustAsHtml(rowModel);
                            }
                            this.formWillShow = function(formname, relationname, formIndex) {
                                return $formService.formWillShow(formname, true, svyServoyApi.getFormName(), childElement.name, relationname, formIndex);
                            }
                            this.hideForm = function(formname, relationname, formIndex, formNameThatWillShow, relationnameThatWillBeShown, formIndexThatWillBeShown) {
                                return $formService.hideForm(formname, svyServoyApi.getFormName(), childElement.name, relationname, formIndex, formNameThatWillShow, relationnameThatWillBeShown, formIndexThatWillBeShown);
                            }

                            this.getFormComponentElements = (propertyName, formComponentValue) => {
                                return $compile($templateCache.get(formComponentValue.uuid))(row);
                            }
                        }
                        ServoyApi.prototype = svyServoyApi;

                        function Handlers(handlers, rowModel, row: IServoyScopeInternal) {
                            this.svy_servoyApi = new ServoyApi(rowModel, row);
                            for (var key in handlers) {
                                this[key] = handlers[key].selectRecordHandler(function() { return row.rowID })
                            }
                        }

                        const rowModel = new Model()

                        const simpleName = childElement.name;

                        copyRecordProperties(childElement, rowModel, index, false);
                        row.model[simpleName] = rowModel;
                        row.handlers[simpleName] = new Handlers(childElement.handlers, rowModel, row);
                        row.api[simpleName] = {};
                        if (childElement['_api'] == undefined) {
                            childElement['_api'] = [];
                        }
                        childElement['_api'][index] = row.api[simpleName];
                        if (scope.svyFormComponent.absoluteLayout) {

                            var elementLayout = { position: "absolute" };
                            if (scope.svyFormComponent.useCssPosition) {
                                elementLayout = childElement.model.cssPosition;
                            }
                            else {
                                var anchoredTop = (childElement.model.anchors & $anchorConstants.NORTH) != 0; // north
                                var anchoredRight = (childElement.model.anchors & $anchorConstants.EAST) != 0; // east
                                var anchoredBottom = (childElement.model.anchors & $anchorConstants.SOUTH) != 0; // south
                                var anchoredLeft = (childElement.model.anchors & $anchorConstants.WEST) != 0; //west

                                if (!anchoredLeft && !anchoredRight) anchoredLeft = true;
                                if (!anchoredTop && !anchoredBottom) anchoredTop = true;
                                if (anchoredLeft) {
                                    elementLayout['left'] = childElement.model.location.x + "px";
                                }
                                if (anchoredRight) {
                                    elementLayout['right'] = (scope.svyFormComponent.formWidth - childElement.model.location.x - childElement.model.size.width) + "px";
                                }
                                else {
                                    elementLayout['width'] = childElement.model.size.width + "px";
                                }
                                if (anchoredTop) {
                                    elementLayout['top'] = childElement.model.location.y + "px";
                                }
                                if (anchoredBottom) {
                                    elementLayout['bottom'] = (scope.svyFormComponent.formHeight - childElement.model.location.y - childElement.model.size.height) + "px"
                                }
                                else {
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
                        const row = scope.$new(false, scope) as IServoyScopeInternal;
                        rowToModel[index] = row;
                        row.model = {}
                        row.api = {};
                        row.layout = {};
                        row.handlers = {}
                        row.createdChildElements = 0;
                        row.rowID = rowId;

                        const clone = element.clone();

                        // as we do the $compile below we don't want to create an svy-form-component directive instance for each row
                        clone.attr("svy-form-component-clone", clone.attr("svy-form-component"));
                        clone.removeAttr("svy-form-component");
                        // this compile (that happens before row contents are added to "clone") is only for ng-click to work on each rows container div, not the actual scope of the row (which is created and used for compilation in createChildElementForRow)
                        clone.attr("ng-click", "onRowClick(rowID)");
                        clone.attr("ng-keydown", "onKeydown($event, rowID)");

                        if (scope.rowStyleClass) {
                            clone.attr("class", clone.attr("class") + " " + scope.rowStyleClass);
                        }
                        if (scope.rowStyleClassDataprovider) {
                            clone.attr("ng-class", "getRowStyleClassDataprovider(rowID)");
                        }
                        $compile(clone)(row);

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
                            for (var j = 0; j < scope.svyFormComponent.childElements.length; j++) {
                                createChildElementForRow(index, row, scope.svyFormComponent.childElements[j] as componentType.ComponentPropertyValue);
                            }
                        } else {
                            // this is probably a foundset viewport insert; the components that are record linked will themselves populate the child elements in the component listeners code later
                            // so just add the ones that are not record linked here because those do not have viewportChangeListeners added, and insert can add them right away, as they don't have viewport specific data
                            for (var j = 0; j < scope.svyFormComponent.childElements.length; j++) {
                                const childElement = scope.svyFormComponent.childElements[j] as componentType.ComponentPropertyValue;
                                if (!childElement.foundsetConfig || !childElement.foundsetConfig.recordBasedProperties || !(childElement.foundsetConfig.recordBasedProperties.length > 0)) {
                                    createChildElementForRow(index, row, scope.svyFormComponent.childElements[j] as componentType.ComponentPropertyValue);
                                }
                            }
                        }
                    }

                    function updateChildElementsAPI(selectedRow) {
                        const viewportIndex = selectedRow - scope.foundset.viewPort.startIndex;
                        for (let i = 0; i < scope.svyFormComponent.childElements.length; i++) {
                            if (viewportIndex < scope.svyFormComponent.childElements[i]['_api'].length) {
                                scope.svyFormComponent.childElements[i].api = scope.svyFormComponent.childElements[i]['_api'][viewportIndex];
                            }
                        }
                    }

                    function updateSelection(newValue, oldValue?) {
                        if (scope.selectionClass) {
                            let children = parent.children();
                            if (oldValue) {
                                for (let k = 0; k < oldValue.length; k++) {
                                    let idx = oldValue[k] - scope.foundset.viewPort.startIndex;
                                    if (idx > -1 && idx < children.length - 1) {
                                        $(parent.children()[idx + 1]).removeClass(scope.selectionClass);
                                    }
                                }
                            }
                            else {
                                for (let k = 1; k < children.length; k++) {
                                    $(parent.children()[k]).removeClass(scope.selectionClass);
                                }
                            }
                            for (let k = 0; k < newValue.length; k++) {
                                let idx = newValue[k] - scope.foundset.viewPort.startIndex;
                                if (idx > -1 && idx < children.length - 1) {
                                    $(parent.children()[idx + 1]).addClass(scope.selectionClass);
                                }
                            }
                        }
                        if (newValue.length > 0) updateChildElementsAPI(newValue[0]);
                        // update the focus when the selection was changed using key up or down
                        let selectedRowIndex = getFoundset().selectedRowIndexes[0];
                        const element = parent.children()[(page > 0) ? ++selectedRowIndex - scope.responsivePageSize * page : ++selectedRowIndex];
                        if (element && !element.contains(document.activeElement) && selectionChangedByKey && !element.className.includes("svyPagination")) {
                            element.focus();
                            selectionChangedByKey = false;
                        }
                    }

                    function createRowsAndSetSelection() {
                        createRows();
                        var selectedRowsIndexes = getFoundset().selectedRowIndexes;
                        if (selectedRowsIndexes.length) {
                            updateSelection(selectedRowsIndexes);
                        }
                    }

                    if (scope.foundset && scope.foundset.viewPort && scope.foundset.viewPort.rows
                        && scope.svyFormComponent && scope.svyFormComponent.childElements) {
                        element.empty();
                        parent.empty();
                        parent.append(pager);
                        template = $compile($templateCache.get(scope.svyFormComponent.uuid));
                        const propertyInName = scope.svyFormComponent.startName

                        const height = scope.svyFormComponent.formHeight;
                        const width = scope.svyFormComponent.formWidth;

                        if (scope.pageLayout == 'listview') {
                            element.css("width", "100%");
                        }
                        if (scope.svyFormComponent.absoluteLayout) {
                            element.css("position", "relative")
                            element.css("height", height + "px")
                            if (scope.pageLayout != 'listview') {
                                element.css("width", width + "px");
                            }
                        }

                        // this listener works in tandem with childElement.addViewportChangeListener(...) listeners 
                        foundsetListener = getFoundset().addChangeListener(function(changes) {
                            // check to see what actually changed server-side and update what is needed in browser
                            let shouldUpdatePagingControls = false;
                            if (changes.viewportRowsCompletelyChanged) {
                                createRows();
                                if (changes.selectedRowIndexesChanged)
                                    updateSelection(changes.selectedRowIndexesChanged.newValue, changes.selectedRowIndexesChanged.oldValue);
                                return;
                            } else if (changes.fullValueChanged) {
                                scope.foundset = changes.fullValueChanged.newValue; // the new value by ref would be updated in scope automatically only later otherwise and we use that in code
                                createRows();
                                if (changes.selectedRowIndexesChanged)
                                    updateSelection(changes.selectedRowIndexesChanged.newValue, changes.selectedRowIndexesChanged.oldValue);
                                return;
                            } else if (changes.viewportRowsUpdated) {
                                const updates = changes.viewportRowsUpdated.updates;
                                updates.forEach((value) => {
                                    // we handle here just row deletes and inserts (insert blank row); the data changes will be handled in each component viewport change listener (see code below)
                                    if (value.type == $foundsetTypeConstants.ROWS_INSERTED) {
                                        for (let k = value.startIndex; k <= value.endIndex; k++) {
                                            rowToModel.splice(k, 0, null);
                                            createRow(k, false);
                                        }
                                        shouldUpdatePagingControls = true;
                                    }
                                    else if (value.type == $foundsetTypeConstants.ROWS_DELETED) {
                                        const endIndex = Math.min(rowToModel.length - 1, value.endIndex); // -1 because value.endIndex is including this call can happen because of a removed of the view port if the view port was bigger then cells that we render.
                                        for (let k = value.startIndex; k <= endIndex; k++) {
                                            destroyScopes(rowToModel.splice(value.startIndex, 1));
                                            parent.children()[value.startIndex + 1].remove(); // + 1 is due to pager that is the first div
                                        }
                                        shouldUpdatePagingControls = true;
                                    }
                                    else if (value.type == $foundsetTypeConstants.ROWS_CHANGED) {
                                        //check if rowid was changed 
                                        for (let k = value.startIndex; k <= value.endIndex; k++) {
                                            let elScope = rowToModel[k] as IServoyScopeInternal;
                                            elScope.rowID = scope.foundset.viewPort.rows[k][$foundsetTypeConstants.ROW_ID_COL_KEY];
                                        }
                                    }
                                })
                            }

                            if (changes.serverFoundsetSizeChanged) shouldUpdatePagingControls = true;

                            if (shouldUpdatePagingControls) updatePagingControls();

                            if (changes.viewPortSizeChanged && getFoundset().serverSize > 0 && (page * numberOfCells >= getFoundset().serverSize) && getFoundset().viewPort.size == 0 && numberOfCells > 0) {
                                // if we were on last page (or some page) and probably due to a delete there are no longer records for that page, adjust page number to show last available page
                                page = Math.floor((getFoundset().serverSize - 1) / numberOfCells);
                                createRows(); // make sure new page is loaded and shown
                            } else {
                                let viewportSizeAfterShiftingIsDone = getFoundset().viewPort.size;
                                if (changes.viewPortStartIndexChanged) {
                                    // an insert/delete before current page made viewport start index no longer match page start index; adjust
                                    const shiftedPageDelta = page * numberOfCells - getFoundset().viewPort.startIndex; // can be negative (insert) or positive(delete)
                                    if (shiftedPageDelta != 0) {
                                        const wantedVPSize = getFoundset().viewPort.size;
                                        const wantedVPStartIndex = page * numberOfCells;
                                        const serverSize = getFoundset().serverSize;

                                        // so shifting means loading "shiftedPageDelta" more/less in one end of the viewport and "shiftedPageDelta" less/more at the other end

                                        // when load extra would request more records after, there might not be enough records in the foundset (deleted before)
                                        let loadExtraCorrected = shiftedPageDelta;
                                        if (loadExtraCorrected > 0 /*so shift right*/ && wantedVPStartIndex + wantedVPSize > serverSize)
                                            loadExtraCorrected -= (wantedVPStartIndex + wantedVPSize - serverSize);
                                        if (loadExtraCorrected != 0) {
                                            getFoundset().loadExtraRecordsAsync(loadExtraCorrected, true);
                                            viewportSizeAfterShiftingIsDone += Math.abs(loadExtraCorrected);
                                        }

                                        // load less if it happens at the end - might need to let more records slide-in the viewport if available (insert before)
                                        let loadLessCorrected = shiftedPageDelta;
                                        if (loadLessCorrected < 0 /*so shift left*/ && wantedVPSize < numberOfCells && wantedVPStartIndex + wantedVPSize < serverSize) // 
                                            loadLessCorrected += Math.min(serverSize - wantedVPStartIndex - wantedVPSize, numberOfCells - wantedVPSize);
                                        if (loadLessCorrected != 0) {
                                            getFoundset().loadLessRecordsAsync(loadLessCorrected, true);
                                            viewportSizeAfterShiftingIsDone -= Math.abs(loadLessCorrected);
                                        }
                                    }
                                    updateSelection(getFoundset().selectedRowIndexes);
                                }

                                // ok now we know startIndex is corrected if needed already; check is size needs to be corrected as well
                                if (changes.viewPortSizeChanged) {
                                    // see if the new viewport size is larger or smaller then expected

                                    // sometimes - due to custom components and services that show forms but they do not properly wait for the formWillShow promise to resolve
                                    // before showing the form in the DOM - list form component might end up showing in a container that changed size so numberOfCells is now different
                                    // (let's say decreased) but having old foundset viewport data (meanwhile solution server side might have changed foundset); then what happened
                                    // is that browser-side list-form-component requested less records based on old foundset data while server-side already had only 1 or 2 records now
                                    // in foundset => it got back a viewport of size 0

                                    // so although this would not normally happen (viewport size getting changed incorrectly as if the component requested that) we check this to be
                                    // resilient to such components/services as well; for example popupWindow used to show forms quickly before getting the updates from server before showing
                                    // (a second show of a pop-up window with decreased size and also less records in the foundset); there are other components that could do this for example
                                    // bootstrap tabless panel with waitForData property set to false

                                    const vpStartIndexForCurrentCalcs = page * numberOfCells; // this might have already been requested in previous code; might not be the actual present one in browser
                                    const vpSizeForCurrentCalcs = viewportSizeAfterShiftingIsDone; // this might have already been requested in previous code; might not be the actual present one in browser

                                    const deltaSize = numberOfCells - vpSizeForCurrentCalcs;
                                    if (deltaSize > 0) {
                                        // we could show more records then currently in viewport; see if more are available
                                        const availableExtraRecords = getFoundset().serverSize - (vpStartIndexForCurrentCalcs + vpSizeForCurrentCalcs)
                                        if (availableExtraRecords > 0) getFoundset().loadExtraRecordsAsync(Math.min(deltaSize, availableExtraRecords), true);
                                    } else if (deltaSize < 0) {
                                        // we need to show less records; deltaSize is already negative; so it will load less from end of viewport 
                                        getFoundset().loadLessRecordsAsync(deltaSize, true);
                                    } // else it's already ok
                                }

                                getFoundset().notifyChanged(); // let foundset send it's pending requests to server if any
                            }

                            if (changes.selectedRowIndexesChanged) {
                                updateSelection(changes.selectedRowIndexesChanged.newValue, changes.selectedRowIndexesChanged.oldValue);
                                if (scope.selectionChangedHandler) {
                                    var e = { target: parent[0] };
                                    scope.selectionChangedHandler($utils.createJSEvent(e, "onselectionchanged"));
                                }
                            }
                            // TODO any other types of changes that need handling here?
                        });

                        for (let j = 0; j < scope.svyFormComponent.childElements.length; j++) {
                            const childElement = scope.svyFormComponent.childElements[j] as componentType.ComponentPropertyValue;
                            if (childElement.name.indexOf(propertyInName) != 0) throw "The child name " + childElement.name + " should start with " + propertyInName;
                            const simpleName = childElement.name;
                            if (childElement.foundsetConfig && childElement.foundsetConfig.recordBasedProperties && childElement.foundsetConfig.recordBasedProperties.length > 0) {
                                componentListeners.push(childElement.addViewportChangeListener((change) => {
                                    // make sure the child element listeners are executed later, after the foundset change listener had a chance to process inserts and deletes
                                    $webSocket.addIncomingMessageHandlingDoneTask(function() {
                                        if (change.viewportRowsUpdated) {
                                            const updates = change.viewportRowsUpdated.updates;
                                            updates.forEach((value) => {
                                                if (value.type == $foundsetTypeConstants.ROWS_CHANGED) {
                                                    for (let k = value.startIndex; k <= value.endIndex; k++) {
                                                        const row = rowToModel[k] as servoy.IServoyScope;
                                                        copyRecordProperties(childElement, row.model[simpleName], k, true);
                                                    }
                                                } else if (value.type == $foundsetTypeConstants.ROWS_INSERTED) {
                                                    // the actual 'row' was created when foundset change listener executed before; we just need to create the child element related stuff
                                                    for (let k = value.startIndex; k <= value.endIndex; k++) {
                                                        const row = rowToModel[k];
                                                        row.rowID = scope.foundset.viewPort.rows[k][$foundsetTypeConstants.ROW_ID_COL_KEY];
                                                        createChildElementForRow(k, row, scope.svyFormComponent.childElements[j] as componentType.ComponentPropertyValue);
                                                    }
                                                    updateSelection(getFoundset().selectedRowIndexes);
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
                                {
                                    configurable: true, value: function(property, value) {
                                        let rowLevelChangeNotifierThatMightGetInherited = childElement.model[$sabloConstants.modelChangeNotifier];
                                        rowToModel.some((row) => {
                                            let cellLevelChangeNotifier = row.model[simpleName][$sabloConstants.modelChangeNotifier];
                                            if (cellLevelChangeNotifier && cellLevelChangeNotifier != rowLevelChangeNotifierThatMightGetInherited) { // if the component does not define a change notifier it will probably have the inherited proto one from the row; we don't want to fire the inherited row notifier - that would result in a stack overflow
                                                cellLevelChangeNotifier(property, value);
                                            }
                                            else return true; // if there is no change modifier at the first row then we can skip looping as the rest probably don't have it either
                                        })
                                    }
                                });
                        }
                        if (scope.responsivePageSize == 0) {
                            let lastNumberOfCellsToLoadThatWasUsed = 0;
                            let lastChangeTime = 0;
                            const NUMBER_OF_CELLS_CHANGE_TIMEOUT = 500;
                            let resizeTimeoutPromise: angular.IPromise<void>;

                            // this watch will also be called when resizing browser window due to the $timeout in servoy_app.ts -> svyLayoutUpdate, not just initially
                            scope.$watch(() => { // watches for "lastNumberOfCellsToLoadThatWasUsed" returned below
                                const parentWidth = parent.outerWidth();
                                const parentHeight = parent.outerHeight();
                                const height = scope.svyFormComponent.formHeight;
                                const width = scope.svyFormComponent.formWidth;
                                const numberOfColumns = (scope.pageLayout == 'listview') ? 1 : Math.floor(parentWidth / width);
                                const numberOfRows = Math.floor(parentHeight / height);
                                const numberOfCells = numberOfRows * numberOfColumns;
                                let currentTime: number;

                                if (lastNumberOfCellsToLoadThatWasUsed != numberOfCells) {
                                    // we will need to update that lastNumberOfCellsToLoadThatWasUsed; but do it max once every NUMBER_OF_CELLS_CHANGE_TIMEOUT ms
                                    if (((currentTime = new Date().getTime()) - lastChangeTime) > NUMBER_OF_CELLS_CHANGE_TIMEOUT) {
                                        lastNumberOfCellsToLoadThatWasUsed = numberOfCells;
                                        lastChangeTime = currentTime;
                                        if (resizeTimeoutPromise) {
                                            $timeout.cancel(resizeTimeoutPromise);
                                            resizeTimeoutPromise = undefined;
                                        }
                                    } else if (!resizeTimeoutPromise) {
                                        resizeTimeoutPromise = $timeout(function() {
                                            // nothing to do here; it will just make sure to trigger an angular digest that will call the $watch above again - to not miss handling latest lastNumberOfCellsToLoadThatWasUsed
                                            resizeTimeoutPromise = undefined;
                                        }, NUMBER_OF_CELLS_CHANGE_TIMEOUT);
                                    }
                                }

                                return lastNumberOfCellsToLoadThatWasUsed;
                            }, (newValue) => {
                                createRowsAndSetSelection();
                            });
                        }
                        else {
                            createRowsAndSetSelection();
                        }
                    }
                }
            }
        };
    })
    .factory("$apifunctions", function() {

        return {

            getSelectedText: function(elem) {
                // selectionStart/End seems to be lost/clear when the element looses focus (chrome and IE but firefox works fine)
                // so we keep save those during blur			
                var iSelectionStart;
                var iSelectionEnd;
                $(elem).on('blur', function() {
                    iSelectionStart = elem.selectionStart;
                    iSelectionEnd = elem.selectionEnd;
                });

                return function() {
                    var startPos = !$(elem).is(":focus") && iSelectionStart != undefined ? iSelectionStart : elem.selectionStart;
                    var endPos = !$(elem).is(":focus") && iSelectionEnd != undefined ? iSelectionEnd : elem.selectionEnd;

                    return elem.value.substr(startPos, endPos - startPos);
                }
            },
            selectAll: function(elem) {
                return function() {
                    elem.select();
                }
            },
            replaceSelectedText: function(elem) {
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
            setSelection: function(elem) {
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
            getWidth: function(elem) {
                return function() {
                    return $(elem.parentNode).width();
                }
            },
            getHeight: function(elem) {
                return function() {
                    return $(elem.parentNode).height();
                }
            },
            getX: function(elem) {
                return function() {
                    return $(elem.parentNode).position().left;
                }
            },
            getY: function(elem) {
                return function() {
                    return $(elem.parentNode).position().top;
                }
            }
        }
    })
    .filter('htmlFilter', function() {
        return function(input) {
            if (input && input.indexOf && input.indexOf('<body') >= 0 && input.lastIndexOf('</body') >= 0) {
                input = input.substring(input.indexOf('<body') + 6, input.lastIndexOf('</body'));
            }
            return input;
        };
    }).filter('mnemonicletterFilter', function() {  /* this filter is used for display only*/
        return function(input, letter) {
            if (letter && input) return input.replace(letter, '<u>' + letter + '</u>');
            return input
        };
    }).directive('svyFormatvldisplay', ['$parse', function($parse: angular.IParseService) {
        //it is similar to svy-format
        return {
            restrict: 'A',
            require: 'ng-Model',
            link: function(scope, element, attrs, ngModelController: angular.INgModelController) {
                var vlAccessor = $parse(attrs['svyFormatvldisplay'])
                ngModelController.$formatters.push(function(dpValue) {
                    var valueList = vlAccessor(scope);
                    if (valueList) {
                        for (var i = 0; i < valueList.length; i++) {
                            if (valueList[i].realValue == dpValue) return valueList[i].displayValue;
                        }
                    }
                    return dpValue;
                });
            }
        }
    }]).directive('svyFormstyle', ['$parse', function($parse: angular.IParseService) {
        return {
            restrict: 'A',
            link: function(scope, element, attrs) {
                element.css({ position: 'absolute' });
                function applyStyle(newVal) {
                    if (newVal) {
                        if (scope["formProperties"] && !scope["formProperties"]["hasExtraParts"] && isInContainer(scope)) {
                            delete newVal["minWidth"];
                            delete newVal["minHeight"];
                        }
                        element.css(newVal);
                    }
                }
                applyStyle($parse(attrs['svyFormstyle'])(scope));
                scope.$watch(attrs['svyFormstyle'], applyStyle);
            }
        }

        // checks if formProperties on the scope exists	
        function isInContainer(scope) {
            var parent = scope.$parent;
            while (parent) {
                if (parent.formProperties && parent.formStyle) return true;
                parent = parent.$parent;
            }
            return false;
        }
    }]).directive('svyNgStyle', ['$parse', function($parse: angular.IParseService) {
        // similar to ng-style just it adds the style a bit earlier, during its link phase
        return {
            restrict: 'A',
            link: function(scope, element, attrs) {
                function applyStyle(newVal) {
                    element.css(newVal);
                }
                applyStyle($parse(attrs['svyNgStyle'])(scope));
                scope.$watch(attrs['svyNgStyle'], applyStyle);
            }
        }
    }]).directive("svyDecimalKeyConverter", [function() {
        return {
            restrict: 'A',
            link: function(scope, element, attrs) {
                var unreg = scope.$watch("model.format.type", function(newVal) {
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
                        element.on("keydown", function(event) {
                            if (event.which == 110) {
                                var caretPos = element[0]['selectionStart'];
                                var startString = element.val().slice(0, caretPos);
                                var endString = element.val().slice(element[0]['selectionEnd'], element.val().length);
                                element.val(startString + numeral.localeData().delimiters.decimal + endString);
                                var inputElement = (element[0] as HTMLInputElement);
                                if (inputElement.type === "text") setCaretPosition(element[0], caretPos + 1); // '+1' puts the caret after the input
                                event.preventDefault ? event.preventDefault() : event.returnValue = false; //for IE8
                            }
                        });
                    }
                })
            }
        }
    }]).directive("svyComponentWrapper", ['$compile', function($compile: angular.ICompileService) {
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
                        + componentTypedPropertyValue + '.model" svy-api="' + componentTypedPropertyValue + '.api" svy-handlers="'
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

                return function(scope, element, attr, controller, transcludeFn) {
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
    ).factory('$svyNGEvents', ['$timeout', '$rootScope', function($timeout: angular.ITimeoutService, $rootScope: angular.IRootScopeService) {
        var requestAnimationFrame = window.requestAnimationFrame || window.mozRequestAnimationFrame || window.webkitRequestAnimationFrame || window.msRequestAnimationFrame;

        return {

            /** Sometimes you want to execute code after the DOM is processed already by Angular; for example if a component directive
                    is using jQuery plugins/code to manipulate / hide / replace DOM that is populated with Angular. That is the purpose of this function.
                    It will try to execute the given function before the browser render happens - only once. */
            afterNGProcessedDOM: function(fn, doApply) {
                if (requestAnimationFrame) {
                    if (doApply) {
                        requestAnimationFrame(function(scope) {
                            $rootScope.$apply(fn);
                        });
                    } else requestAnimationFrame(fn);
                } else $timeout(fn, 0, doApply); // it could produce flicker, but better then nothing
            }
        }
    }]).factory("$svyI18NService", ['$sabloApplication', '$q', '$webSocket', function($sabloApplication: sablo.ISabloApplication, $q: angular.IQService, $webSocket: sablo.IWebSocket) {
        var cachedMessages = {};
        var cachedPromises: { [s: string]: { promise?: angular.IPromise<{}>; value?: any } } = {};
        var defaultTranslations = {};
        return <servoy.IServoyI18NService>{
            addDefaultTranslations: function(translations) {
                angular.extend(defaultTranslations, translations);
            },
            getI18NMessages: function() {
                var retValue = {};
                var serverKeys = {};
                var serverKeysCounter = 0;
                for (var i = 0; i < arguments.length; i++) {
                    if (cachedMessages[arguments[i]] != null) {
                        retValue[arguments[i]] = cachedMessages[arguments[i]];
                    }
                    else {
                        serverKeys[serverKeysCounter++] = arguments[i];
                    }
                }
                if (serverKeysCounter > 0) {
                    const promise = $sabloApplication.callService("i18nService", "getI18NMessages", serverKeys, false);
                    return $webSocket.wrapPromiseToPropagateCustomRequestInfoInternal(promise, promise.then(function(result: any) {
                        for (var key in result) {
                            cachedMessages[key] = result[key];
                            retValue[key] = result[key];
                        }
                        return retValue;
                    }, function(error) {
                        return $q.reject(error);
                    }))
                }
                else {
                    var defered = $q.defer()
                    defered.resolve(retValue);
                    return defered.promise;
                }
            },
            getI18NMessage: function(key) {

                if (!cachedPromises[key]) {
                    let promise = $sabloApplication.callService("i18nService", "getI18NMessages", { 0: key }, false);
                    promise = $webSocket.wrapPromiseToPropagateCustomRequestInfoInternal(promise, promise.
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
                                if (!promise['reject']) {
                                    delete cachedPromises[key]; // try again later
                                }
                                return $q.reject(error);
                            }
                        ));
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
                var value = getUiProperties()[key];
                if (value === undefined) {
                    value = null;
                }
                return value;
            },
            setUIProperty: function(key, value) {
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