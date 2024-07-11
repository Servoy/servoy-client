angular.module('window',['servoy'])
.factory("window",function($window,$services,$compile,$formService,$windowService,$sabloApplication,$timeout,$q,$log,$sabloTestability,$utils,$log) {
	var scope = $services.getServiceScope('window');
	scope.formPopupShown = null;
	// when a form popup is shown, we notify the server that the form will become visible but wait for the server to return any changes/data that form
    // needs to have client side (to be up to date) before really showing it in the browser; so after the form popup decides to show a form there will
	// be a req/respone cycle before the reallyShownFormURL that shows that form in the DOM is set to the new form
	scope.reallyShownFormURL = null;
	var _this = {
		translateSwingShortcut: function(shortcutcombination)
		{
			var shortcutParts = shortcutcombination.split(" ");
			var translatedShortcut = '';
			for (var i=0;i<shortcutParts.length;i++)
			{
				if (i>0)
				{
					translatedShortcut +='+';
				}
				if (shortcutParts[i] == 'control' || shortcutParts[i] == 'ctrl')
				{
					translatedShortcut +='CTRL';
				}
				else if (shortcutParts[i] == 'meta')
				{
					translatedShortcut +='META';
				}
				else if (shortcutParts[i] == 'shift')
				{
					translatedShortcut +='SHIFT';
				}
				else if (shortcutParts[i] == 'alt')
				{
					translatedShortcut +='ALT';
				}
				else if (shortcutParts[i].toLowerCase().indexOf('numpad') == 0)
				{
					//numpad0 to numpad9
					if (shortcutParts[i].length == 7)
					{
						shortcutParts[i] = shortcutParts[i].toLowerCase();
						shortcutParts[i] = shortcutParts[i].replace("numpad","numpad-");
						translatedShortcut += shortcutParts[i];
					}
					else
					{
						translatedShortcut += shortcutParts[i];
					}	
				}	
				else
				{
					translatedShortcut +=shortcutParts[i];
				}
			}
			return translatedShortcut;
		},
		getCallbackFunction: function(shortcutcombination, consumeEvent)
		{
			return function(e){
				
				var targetEl;
				if(e.target) targetEl=e.target;
				else if(e.srcElement) targetEl=e.srcElement;
				var pushedChanges = false;
				var retValue = true;
				for (var j = 0;j< scope.model.shortcuts.length;j++)
				{
					if (scope.model.shortcuts[j].shortcut == shortcutcombination )
					{	
						var callback = scope.model.shortcuts[j].callback;
						var contextFilter = null;
						var contextFilterElement = null;
						if(scope.model.shortcuts[j].contextFilter) {
							var contextFilterParts = scope.model.shortcuts[j].contextFilter.split('.');
							contextFilter = contextFilterParts[0];
							if(contextFilterParts.length > 1) {
								contextFilterElement = contextFilterParts[1];
							}
						}
						
						var jsEvent = $utils.createJSEvent(e,shortcutcombination,contextFilter,contextFilterElement);
						
						if (!jsEvent) continue;

						var args = scope.model.shortcuts[j].arguments;
						var argsWithEvent = [jsEvent];// append args
						if(args != null) {
							if(args.length) {
								argsWithEvent = argsWithEvent.concat(args);
							}
							else {
								argsWithEvent.push(args);
							}
						}
						if (!pushedChanges) $(targetEl).change();
						pushedChanges = true;
						$sabloTestability.block(true);
						$timeout(function(callback,argsWithEvent) {
							var formName = argsWithEvent[0].formName;
							if (!formName) formName = callback.formname;
							$window.executeInlineScript(formName,callback.script,argsWithEvent);
							$sabloTestability.block(false);
						},10,true,callback,argsWithEvent);
						if (retValue && consumeEvent) retValue = false;
					}	
				}
				return retValue;
			};
		},
		
		/**
		 * Show a form as popup panel.
		 * 
		 * @example
		 * plugins.window.showFormPopup(null,forms.orderPicker,200, 200);
		 * 
		 * @param component element to show related to or null to center in screen
		 * @param form the form to show
		 * @param width the popup width
		 * @param height the popup height
		 * @param x the popup x location
		 * @param y the popup y location
		 * @param showBackdrop whatever to show backdrop
		 *
		 */
		showFormPopupInternal : function(component,form,width,height,x,y,showBackdrop)
		{
			if ( $( document ).find( '[svy-window]' ).length < 1 ) {
				$( "#mainForm" ).trigger( "disableTabseq" );
			}

			$formService.formWillShow(form, true).then(function successCallback() {
				if ($log.debugEnabled) $log.debug("wnd * formWillShow resolved successfully for showFormPopup; form: " + form + ". Adding the form to DOM.");
				scope.reallyShownFormURL = $windowService.getFormUrl(form);
			}, function errorCallback(e) {
				$log.error("Error while trying to show form in form popup: " + e);
				scope.reallyShownFormURL = null;
				_this.cancelFormPopup();
			});

			scope.getFormUrl = function(){
				return scope.reallyShownFormURL;
			};
			
			scope.lastElementFocused = function( e ) {
				var lastTabIndex = parseInt( this.popupElement.closest( '#tabStop' ).attr( 'tabindex' ) );
				for(var i = 2; i < lastTabIndex; i++) {
					var newTarget = $( '[tabindex=' + i +']' );
					// if there is no focusable element in the window, then newTarget == e.target,
					// do a check here to avoid focus cycling
					if(newTarget.is(":visible") && (e.target != newTarget[0])) {
						newTarget.focus();
						break;
					}
				}
			}
			
			scope.firstElementFocused =  function( e ) {
				var lastTabIndex = parseInt( this.popupElement.closest( '#tabStop' ).attr( 'tabindex' ) );
				for(var i = lastTabIndex - 1; i > 1; i--) {
					var newTarget = $( '[tabindex=' + i + ']' );
					// if there is no focusable element in the window, then newTarget == e.target,
					// do a check here to avoid focus cycling
					if(newTarget.is(":visible") && (e.target != newTarget[0])) {
						newTarget.focus();
						break;
					}
				}
			}
			
			function getElementById(id)
			{
				var defered = $q.defer();
				var counter = 0;
				function test() 
				{
					var element = $('#'+id);
					if (element[0]) defered.resolve(element);
					else if (counter++ < 100) $timeout(test,10);
					else defered.reject();
				}
				if (!id) defered.resolve(null);
				else test();
				return defered.promise;
			};
			
			function getLeftPosition(position,popupwidth,component,oldleft)
			{
				//if necessary right align popup on related component
				if (position.left + popupwidth > $( window ).width())
				{
					var compWidth = component ? $('#'+component).outerWidth() : 0;
					if (position.left - popupwidth + compWidth > 0)
					{
						return position.left - popupwidth + compWidth;
					}
					else
					{
						// does not fit to the right or left; do the same as for top
					}
				}
				return oldleft;
			}
			
			function getTopPosition(position,popupheight,component,oldtop)
			{
				//if necessary popup on the top of the related component
				var compHeight = component ? $('#'+component).outerHeight() : 0;
				if (position.top + compHeight + popupheight > $( window ).height())
				{
					if ((position.top - popupheight) > 0)
					{
						return position.top - popupheight;
					}
					else
					{
						// there is not enough space on bottom, there is not enough space on top; just leave the old one ?
						//wc is different , see PopupPanel.html, do this way for minox menu
					}	
				}
				return oldtop;
			}
			
			scope.loadSize = function(){
				$sabloApplication.getFormState(form).then(function(formState){
					var popupwidth = width;
					if (!popupwidth || popupwidth<=0)
					{
						popupwidth = formState.properties.designSize.width;
					}
					var popupheight = height;
					if (!popupheight || popupheight<=0)
					{
						popupheight = formState.properties.designSize.height;
					}
					var css = {};
					css["width"] = popupwidth+"px";
					css["height"] = popupheight+"px";
					if (component || (x && y))
					{
						var position = component ? $('#'+component).offset() : {'left':x,'top':y};
						var left = getLeftPosition(position,popupwidth,component,null);
						if (left)
						{
							css["left"] = left+"px";
						}
						var top = getTopPosition(position,popupheight,component,null);
						if (top)
						{
							css["top"] = top+"px";
						}
					}
					else if (!component)
					{
						// calculate the real center
						css["left"] = ($( window ).width() /2 - popupwidth /2 )+"px";
						css["top"] = ($( window ).height() /2 - popupheight /2 )+"px";
					}
					$('#formpopup').css(css);
		    	})
			};
			getElementById(component).then(function(relatedElement)
			{
				var body = $('body');
				var style = 'position:absolute;z-index:1499;';
				var left = $( window ).width() /2;
				var top = $( window ).height() /2;
				var position = null;
				if (component)
				{
					var position = relatedElement.offset();
					left = position.left;
					top = position.top + relatedElement.outerHeight();
					position = relatedElement.offset();
				}
				else if (x && y)
				{
					left = x;
					top = y;
					position = {'left':x,'top':y}
				}
				// set correct position right away, do not wait for loadSize as showing it in wrong position may interfere with calculations
				// we should remove loadSize, but how to get the form size then ?
				if (width && width > 0)
				{
					style+= 'width:'+width+'px;'
					if (position)
					{
						left = getLeftPosition(position,width,component,left);
					}	
				}
				if (height && height > 0)
				{
					style+= 'height:'+height+'px;'
					if (position)
					{
						top = getTopPosition(position,height,component,top);
					}
				}
				style+= 'left:'+left+'px;';
				style+= 'top:'+top+'px;';
				var popup = $compile('<div id="tabStart" sablo-tabseq="1" ng-focus="firstElementFocused($event)"></div><div id=\'formpopup\' style="'+style+'" svyform="'+form +'"ng-include="getFormUrl()" onload="loadSize()" sablo-tabseq="2" sablo-tabseq-config="{container: true}"></div><div id="tabStop" sablo-tabseq="3" ng-focus="lastElementFocused($event)"></div>')(scope);
				scope.popupElement = popup;
				$timeout(function(){
					body.on('mouseup',formPopupBodyListener);
				},300);
				if(showBackdrop) {
					body.append('<div class="formpopup-backdrop modal-backdrop fade in" style="z-index:1498"></div>');
				}
				body.append(popup);

		 }, function()
		 {
			 $log.error('Cannot show form popup, the related element is not visible: form name "'+form+'".');
		 });
		},
		
		/**
		 * Close the current form popup panel.
		 * @example 
		 * plugins.window.cancelFormPopup();
		 * 
		 */
		cancelFormPopup : function()
		{
			_this.cancelFormPopupInternal(false)
		},
		cancelFormPopupInternal : function(disableClearPopupFormCallToServer)
		{
			$('body').off('mouseup',formPopupBodyListener);
			if (scope.formPopupShown)
			{
				$formService.hideForm(scope.formPopupShown.form);
			}
			var closeCallback =  scope.model.popupform ? scope.model.popupform.onClose : _this.onClose;
			if(closeCallback) {
				var jsEvent = $utils.createJSEvent({target:document.getElementById("formpopup")},"popupClose");
				if (jsEvent){
					jsEvent.formName = scope.formPopupShown.form;
				}
				$window.executeInlineScript(closeCallback.formname,closeCallback.script,[jsEvent]);
			}
			var popup = angular.element("#formpopup");
			if (popup)
			{
				if (scope.formPopupShown){
					var formState = $sabloApplication.getFormStateEvenIfNotYetResolved(scope.formPopupShown.form);
					if (formState && formState.getScope) {
						formState.getScope().$destroy();
					}
				}
				popup.remove();
				scope.reallyShownFormURL = null;
			}
			var backdrop = angular.element(".formpopup-backdrop");
			if (backdrop && backdrop.length) {
				backdrop.remove();
			}
			if ( $( document ).find( '[svy-window]' ).length < 1 ) {
				$( "#mainForm" ).trigger( "enableTabseq" );
			}
			if (scope.model.popupform === scope.formPopupShown) {
				scope.model.popupform = null;
				scope.formPopupShown = null;
				/*
				 * Because server side code in window_server.js checks for scope.model.popupform != null when closing a form popup it must have the correct value server-side; so
				 *     - when it is closed by a click outside the popup form area that happens to be exactly on a button that opens it again, the current method executes and
				 *       "scope.model.popupform" needs to reach server before the button click that will open the form editor again executes not after (because if it is set to null
				 *       after the reshow, it will be in a wrong state server-side); that is why we use callServerSideApi here instead of relying on a shallow watch (pushToServer in spec)
				 *       on the model property which would send the null change too late
				 *     - if one would click twice really fast on a button that shows a form popup, both those clicks are queued on the server's event queue; it shows the first time
				 *       then in server side code - when the show is called the second time it would close the first one and show it again; but in this case we must not call callServerSideApi
				 *       to set scope.model.popupform to null because that would execute after the second show is done (it is queued after it on server) and again we'd end up with a shown
				 *       form popup but a null scope.model.popupform on server which is wrong... that is the purpose of "disableClearPopupFormCallToServer" flag
				 */
				if(!disableClearPopupFormCallToServer){
					$services.callServerSideApi("window","clearPopupForm",[]);
				}
			}
			else if( scope.model.popupform ) {
				_this.showFormPopupInternal(scope.model.popupform.component,scope.model.popupform.form,scope.model.popupform.width,scope.model.popupform.height,scope.model.popupform.x,scope.model.popupform.y,scope.model.popupform.showBackdrop);
				scope.formPopupShown = scope.model.popupform;
			}
			if(scope.popupElement) {
				scope.popupElement.remove();
				scope.popupElement = null;
			}
		},
		generateMenu: function(items,oMenu)
		{
			var groupCount = 1;
			for (var j=0;j<items.length;j++)
			{
				if (items[j])
				{
					if (items[j].visible == false) continue;
					var text = items[j].text;
					if (!text) text = 'no text';
					var cssClass = items[j].cssClass;
					if (cssClass == 'img_checkbox' && items[j].selected != true)
					{
						// not selected checkbox
						cssClass = 'img_checkbox_unselected';
					}
					if (cssClass == 'img_radio_off' && items[j].selected == true)
					{
						// selected radio
						cssClass = "img_radio_on";
					}
					if (items[j].icon || cssClass || items[j].fa_icon)
					{
						var htmltext = "<html><table><tr>";
						if (items[j].cssClass) htmltext += "<td class=\"" + cssClass + "\">&nbsp;&nbsp;&nbsp;&nbsp;</td>"; 
						if (items[j].icon) htmltext += "<td><img src=\"" + items[j].icon + "\" style=\"border:none\"/></td>";
						if (items[j].fa_icon) htmltext += "<td><i class=\""+items[j].fa_icon+"\" aria-hidden=\"true\"></i></td>";
						htmltext += "<td>" + text + "</td>"; 
						htmltext += "</tr></table></html>";
						text = htmltext;
					}
					var mi = new YAHOO.widget.MenuItem(text);
					if (items[j].callback) {
						mi.cfg.setProperty('onclick', {fn:function(index) {return function(){
							$services.callServerSideApi("window","executeMenuItem",[items[index].id, index, -1, items[index].selected, null, items[index].text]);
							}}(j)
						});
					}
					if (items[j].enabled == false)
					{
						mi.cfg.setProperty('disabled', true);
					}
					if (items[j].backgroundColor)
					{
						YAHOO.util.Dom.setStyle(mi.element, 'background-color', items[j].backgroundColor);
					}
					if (items[j].foregroundColor)
					{
						var menuLabel = YAHOO.util.Dom.getElementsByClassName('yuimenuitemlabel', 'a', mi.element);
						if(menuLabel) { YAHOO.util.Dom.setStyle(menuLabel, 'color', items[j].foregroundColor)};
					}
					oMenu.addItem(mi,groupCount);
					if (items[j].items && items[j].items.length >0)
					{
						var menu = new YAHOO.widget.Menu(text);
						_this.generateMenu(items[j].items,menu);
						mi.cfg.setProperty('submenu', menu);
					}
				}
				else
				{
					// separator
					groupCount++;
				}
			}
		}
	}
	function formPopupBodyListener(event)
	{
			if (scope.formPopupShown && scope.formPopupShown.doNotCloseOnClickOutside){
				return;
			}
			var backdrop = angular.element(".formpopup-backdrop");
			if (backdrop && (backdrop.get(0) == event.target))
			{
				backdrop.remove();
				_this.cancelFormPopupInternal(false);
				return;
			}
			var mainform = angular.element(".svy-main-window-container");
			if (mainform && mainform.find(event.target).length > 0 )
			{
				_this.cancelFormPopupInternal(false);
				return;
			}
			 mainform = angular.element(".svy-dialog");
			 if (mainform && mainform.find(event.target).length > 0 )
			 {
				 _this.cancelFormPopupInternal(false);
				 return;
			 }
	}
	return _this;
})
.run(function($rootScope,$services,window,$window, $timeout)
{
	var scope = $services.getServiceScope('window');
	scope.$watch('model', function(newvalue,oldvalue) {
		if (newvalue && newvalue.shortcuts && newvalue.shortcuts.length > 0)
		{
			for (var i=0;i<newvalue.shortcuts.length;i++)
			{
				var translatedShortcut = window.translateSwingShortcut(newvalue.shortcuts[i].shortcut);
				if (!shortcut.all_shortcuts[translatedShortcut])
				{
					shortcut.add(translatedShortcut,window.getCallbackFunction(newvalue.shortcuts[i].shortcut, newvalue.shortcuts[i].consumeEvent),{'propagate':true,'disable_in_input':false});
				}	
			}
		}
		if (newvalue && newvalue.popupform && !angular.equals(oldvalue.popupform,newvalue.popupform))
		{
			if (!scope.formPopupShown) {
				window.showFormPopupInternal(newvalue.popupform.component,newvalue.popupform.form,newvalue.popupform.width,newvalue.popupform.height,newvalue.popupform.x,newvalue.popupform.y,newvalue.popupform.showBackdrop);
				scope.formPopupShown = newvalue.popupform;
			}
			else window.cancelFormPopup();
		}
		if (newvalue && newvalue.popupMenuShowCommand)
		{
			var settings = {zIndex : 1500};
			var classname;
			if (newvalue.popupMenus && newvalue.popupMenuShowCommand.popupName)
			{
				for (var i=0;i<newvalue.popupMenus.length;i++)
				{
					if (newvalue.popupMenuShowCommand.popupName == newvalue.popupMenus[i].name)
					{
						classname = newvalue.popupMenus[i].cssClass;
					}
				}
			}
			if (classname) settings['classname'] = classname;
			var oMenu = new YAHOO.widget.Menu('basicmenu',settings);
			oMenu.clearContent();
			oMenu.subscribe('hide', function (event) {
				$timeout(function(){
					scope.model.popupMenuShowCommand = null;
					oMenu.destroy();
				},1);
			});
			if (newvalue.popupMenus && newvalue.popupMenuShowCommand.popupName)
			{
				for (var i=0;i<newvalue.popupMenus.length;i++)
				{
					if (newvalue.popupMenuShowCommand.popupName == newvalue.popupMenus[i].name)
					{
						var popup = newvalue.popupMenus[i];
						window.generateMenu(popup.items,oMenu)
						var element;
						if (newvalue.popupMenuShowCommand.elementId)
						{
							element = document.getElementById(newvalue.popupMenuShowCommand.elementId);
						}
						if(element)
						{

							var jsCompReg = YAHOO.util.Dom.getRegion(element); //get element region relative to viewport
							var roomAbove = jsCompReg.top - 1;
							var roomBelow = document.documentElement.clientHeight - jsCompReg.top - newvalue.popupMenuShowCommand.height;
							oMenu.render(document.body);

							var oMenuReg = YAHOO.util.Dom.getRegion(document.getElementById(oMenu.id));
							var menuHeight = oMenuReg.bottom - oMenuReg.top;
							var xyReceived = newvalue.popupMenuShowCommand.y != undefined;
							var x = xyReceived ? newvalue.popupMenuShowCommand.x : 0;
							var y = xyReceived ? newvalue.popupMenuShowCommand.y : 0;
							
							if ((newvalue.popupMenuShowCommand.positionTop == true && menuHeight <= roomAbove) || //top position wanted
							    (newvalue.popupMenuShowCommand.positionTop == false && (menuHeight > roomBelow) && (menuHeight <= roomAbove))) {//no space below
								oMenu.moveTo(jsCompReg.left  + x, jsCompReg.top + y - menuHeight); //draw on component's top
							} else if (menuHeight <= roomBelow) { //default we are drawing below component
								oMenu.moveTo(jsCompReg.left  + x, jsCompReg.top + y + (xyReceived ? 0 : newvalue.popupMenuShowCommand.height));
							} else {//no room for popup menu so let's browser decide
								oMenu.moveTo(jsCompReg.left  + x, jsCompReg.top + 1); 
							}
						}
						else
						{
							oMenu.render(document.body);
							oMenu.moveTo(newvalue.popupMenuShowCommand.x,newvalue.popupMenuShowCommand.y);
						}
						oMenu.show();
						break;
					}
				}
			}
		}
		else
		{
			var popup = angular.element("#basicmenu");
			if (popup)
			{
				popup.remove();
			}
		}	
	}, true);
})
