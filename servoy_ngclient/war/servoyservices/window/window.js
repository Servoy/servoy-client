angular.module('window',['servoy'])
.factory("window",function($window,$services,$compile,$formService,$windowService,$sabloApplication,$timeout) {
	var scope = $services.getServiceScope('window');
	return {

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
				else
				{
					translatedShortcut +=shortcutParts[i];
				}
			}
			return translatedShortcut;
		},
		getCallbackFunction: function(shortcutcombination)
		{
			return function(e){
				
				var targetEl;
				if(e.target) targetEl=e.target;
				else if(e.srcElement) targetEl=e.srcElement;
				var pushedChanges = false;
				for (var j = 0;j< scope.model.shortcuts.length;j++)
				{
					if (scope.model.shortcuts[j].shortcut == shortcutcombination )
					{
						var callback = scope.model.shortcuts[j].callback;
						var contextFilter = scope.model.shortcuts[j].contextFilter;
						var args = scope.model.shortcuts[j].arguments;
						
						var form;
						var parent = targetEl;
						var targetElNameChain = new Array();
						var contextMatch = false;
						while (parent) {
							form = parent.getAttribute("ng-controller");
							if (form) {
								if (contextFilter && form != contextFilter && form != 'MainController') break;
								contextMatch = true;
								break;
							}
							if(parent.getAttribute("name")) targetElNameChain.push(parent.getAttribute("name"));
							parent = parent.parentNode;
						}
						
						if (!contextMatch) continue;
						
						var jsEvent = {svyType: 'JSEvent', eventType: shortcutcombination};
						if(form != 'MainController') {
							jsEvent.formName = form;
							var formScope = angular.element(parent).scope();
							for (var i = 0; i < targetElNameChain.length; i++) {
								if(formScope.model[targetElNameChain[i]]) {
									jsEvent.elementName = targetElNameChain[i];
									break;
								}
							}
						}
						
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
						$timeout(function() {
							$window.executeInlineScript(callback.formname,callback.script,argsWithEvent);
						},10);
					}	
				}	
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
		 * 
		 */
		showFormPopup : function(component,form,width,height)
		{
			$formService.formWillShow(form, true);
			scope.getFormUrl = function(){
				return $windowService.getFormUrl(form);
			};
			scope.loadSize = function(){
				$sabloApplication.getFormState(form).then(function(formState){
					var css = {};
					css["min-width"] = formState.properties.designSize.width+"px";
					css["min-height"] = formState.properties.designSize.height+"px";
					$('#formpopup').css(css);
		    	})
			};
			var body = $('body');
			var style = 'position:absolute;z-index:999;';
			if (width && width > 0)
			{
				style+= 'width:'+width+'px;'
			}
			if (height && height > 0)
			{
				style+= 'height:'+height+'px;'
			}
			var left = $( window ).width() /2;
			var top = $( window ).height() /2;
			if (component)
			{
				var position = $('#'+component).offset();
				left = position.left;
				top = position.top+$('#'+component).outerHeight();
			}
			style+= 'left:'+left+'px;';
			style+= 'top:'+top+'px;';
			var popup = $compile('<div id=\'formpopup\' style="'+style+'" ng-include="getFormUrl()" onload="loadSize()" onclick="event.stopPropagation()"></div>')(scope);
			scope.popupElement = popup;
			body.on('click',this.cancelFormPopup);
			body.append(popup);
		},
		/**
		 * Close the current form popup panel.
		 * @example 
		 * plugins.window.cancelFormPopup();
		 * 
		 */
		cancelFormPopup : function()
		{
			$('body').off('click',this.cancelFormPopup);
			if (scope.model.popupform)
			{
				$formService.hideForm(scope.model.popupform.form);
			}
			var popup = angular.element("#formpopup");
			if (popup)
			{
				if (scope.model.popupform){
					var formState = $sabloApplication.getFormStateEvenIfNotYetResolved(scope.model.popupform.form);
					if (formState) {
						formState.getScope().$destroy();
					}
				}
				popup.remove();
			}
			scope.model.popupform = null;
			if(scope.popupElement) {
				scope.popupElement.remove();
				scope.popupElement = null;
			}
		},
		generateMenu: function(items,oMenu)
		{
			for (var j=0;j<items.length;j++)
			{
				var groupCount = 1;
				if (items[j])
				{
					var text = items[j].text;
					if (items[j].visible == false || !text) continue;
					var cssClass = items[j].cssClass;
					if (cssClass == 'img_checkbox' && items[j].selected != true)
					{
						// not selected checkbox
						cssClass = null;
					}
					if (cssClass == 'img_radio_off' && items[j].selected == true)
					{
						// selected radio
						cssClass = "img_radio_on";
					}
					if (items[j].icon || cssClass)
					{
						var htmltext = "<html><table><tr>";
						if (items[j].cssClass) htmltext += "<td class=\"" + cssClass + "\">&nbsp;&nbsp;&nbsp;&nbsp;</td>"; 
						if (items[j].icon) htmltext += "<td><img src=\"" + items[j].icon + "\" style=\"border:none\"/></td>";
						htmltext += "<td>" + text + "</td>"; 
						htmltext += "</tr></table></html>";
						text = htmltext;
					}
					var mi = new YAHOO.widget.MenuItem(text);
					if (items[j].callback) {
						mi.cfg.setProperty('onclick', {fn:function(index) {return function(){
							var args = [index, -1, items[index].selected, null, items[index].text];
							if(items[index].methodArguments && items[index].methodArguments.length) {
								args = args.concat(items[index].methodArguments);
							}
							$window.executeInlineScript(items[index].callback.formname,items[index].callback.script,args)
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
						var menu = new YAHOO.widget.Menu(items[j].text);
						this.generateMenu(items[j].items,menu);
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
					shortcut.add(translatedShortcut,window.getCallbackFunction(newvalue.shortcuts[i].shortcut),{'propagate':true,'disable_in_input':false});
				}	
			}
		}
		if (newvalue && newvalue.popupform && !oldvalue.popupform)
		{
			window.showFormPopup(newvalue.popupform.component,newvalue.popupform.form,newvalue.popupform.width,newvalue.popupform.height);
		}
		if (newvalue && newvalue.popupMenuShowCommand)
		{
			var oMenu = new YAHOO.widget.Menu('basicmenu',{zIndex : 1000});
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
							var parentReg = YAHOO.util.Dom.getRegion(element.offsetParent);
							var jsCompReg = YAHOO.util.Dom.getRegion(element);
							oMenu.render(document.body);
							var oMenuReg = YAHOO.util.Dom.getRegion(document.getElementById(oMenu.id));
							if(element.offsetParent && (jsCompReg.top + newvalue.popupMenuShowCommand.y + (oMenuReg.bottom - oMenuReg.top) > parentReg.top + (parentReg.bottom - parentReg.top)) && 
									(jsCompReg.top + newvalue.popupMenuShowCommand.y - (oMenuReg.bottom - oMenuReg.top) > parentReg.top))
							{
								oMenu.moveTo(jsCompReg.left  + newvalue.popupMenuShowCommand.x, jsCompReg.top -(oMenuReg.bottom - oMenuReg.top));
							}
							else
							{
								oMenu.moveTo(jsCompReg.left  + newvalue.popupMenuShowCommand.x, jsCompReg.top + newvalue.popupMenuShowCommand.y);
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
	}, true);
})
