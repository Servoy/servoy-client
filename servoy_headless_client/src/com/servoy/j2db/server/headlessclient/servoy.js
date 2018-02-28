if (typeof(Servoy) == "undefined")
	Servoy = { };

Servoy.redirectingOnSolutionClose = false;

Servoy.parsePosition = function(position)
{
	if (position instanceof Number) return position;
	if (position == '') return 0;
	if (position == 'auto') return 0;
	return parseInt(position);
}

Servoy.addPositions = function(pos1, pos2)
{
	return Servoy.parsePosition(pos1) + Servoy.parsePosition(pos2);
}

function showMediaUploadPopup(url,imgid)
{
	var x = 0, y = 0;
	
	if (document.all) {
		x = window.screenTop + 100;
		y = window.screenLeft + 100;
	} else if (document.layers) {
		x = window.screenX + 100;
		y = window.screenY + 100;
	} else { // firefox, need to switch the x and y?
		y = window.screenX + 100;
		x = window.screenY + 100;
	}
	window.open(url,'upload', 'top=' + x + ',left=' + y + ',screenX=' + x + ',screenY=' + y + ',location=no,toolbar=no,menubar=no,width=380,height=140')
}

function showMediaDownloadPopup(url)
{
	var x = 0, y = 0;
	
	if (document.all) {
		x = window.screenTop + 100;
		y = window.screenLeft + 100;
	} else if (document.layers) {
		x = window.screenX + 100;
		y = window.screenY + 100;
	} else { // firefox, need to switch the x and y?
		y = window.screenX + 100;
		x = window.screenY + 100;
	}
	window.open(url,'download','top=' + x + ',left=' + y + ',screenX=' + x + ',screenY=' + y + ',location=no,toolbar=no,menubar=no,width=310,height=140,resizable=yes')
}

function setAjaxIndicatorLocation(indicatorMarkupId, x ,y)
{
	var indicator = document.getElementById(indicatorMarkupId);
	indicator.style.top = x +"px";
	indicator.style.left = y +"px";
}

function requestFocus(focusId)
{
	if (focusId != "" && focusId != null)
	{
		var toFocus = document.getElementById(focusId);
		if (toFocus != null && toFocus.focus) {
			try {
				toFocus.focus();
			} catch (ignore) {}
		}
	}
}

var focusingOnInvalidComponent = false;
function scheduleRequestFocusOnInvalid(focusId, value)
{
	if (focusingOnInvalidComponent) return;
	focusingOnInvalidComponent = true;
	window.setTimeout('focusIfUnchanged(\"' + focusId + '\", ' + value + ');' ,1);
}

function focusIfUnchanged(focusId, value)
{
	if (focusId != "" && focusId != null)
	{
		var toFocus = document.getElementById(focusId);
		if (toFocus != null && toFocus.focus && (toFocus.checked == value || toFocus.value == value)) {
			try {
				toFocus.focus();
			} catch (ignore) {}
		}
	}
	focusingOnInvalidComponent = false;
}

var clickedElementId = null;
function testDoubleClickId(elementId)
{
	if (elementId == clickedElementId)
	{
		Wicket.Log.info("element id already clicked: " + clickedElementId);
		return false;
	} 
	clickedElementId = elementId;
	return Wicket.$(elementId) != null;
}

function clearDoubleClickId(elementId)
{
	if (elementId == clickedElementId)
	{
		clickedElementId = null;
	}
	else
	{
		Wicket.Log.info("element id was already another: " + clickedElementId + ", " + elementId + " tried to clear it");
	}
}

var focusedValue = null;
var focusedElement = null;
var focusedElementCaretPosition = 0;
var focusedElementSelection = null;
var ignoreFocusGained = null;
function storeValueAndCursorBeforeUpdate()
{
	focusedElement = Wicket.Focus.getFocusedElement();
	if (typeof(focusedElement) != "undefined" && focusedElement != null)
	{
		var focusedElementType = focusedElement.getAttribute("type");
		if(focusedElementType != "button" && focusedElementType != "submit")
		{
			if(focusedElementType == "text" || focusedElementType == "textarea")
			{
				focusedElementCaretPosition = Servoy.Utils.doGetCaretPosition(focusedElement, true);
				focusedElementSelection = Servoy.Utils.getSelection(focusedElement);
			}
			ignoreFocusGained = focusedElement.id;
			var valueChangedId = null;
			for (var i=0;i<arguments.length;i++)
			{
				if(arguments[i] == focusedElement.id)
				{
					valueChangedId = arguments[i];
					break;
				}
			}
			if (!valueChangedId)
			{
				focusedValue = focusedElement.value;
				// We need this trick because in Safari 4.0.4 it seems that the Ajax refresh is not done atomically.
				focusedElement.SERVOY_DONT_UPDATE_THIS_BECAUSE_ITS_NOT_CHANGED = true;
			}
			else
			{
				focusedElement = null;
			}
		}
		else
		{
			focusedElement = null;
		}
	}
	else
	{
		focusedElement = null;
	}	
}
 
function restoreValueAndCursorAfterUpdate()
{
	var element = Wicket.Focus.getFocusedElement();
	if (focusedElement != null && element != null  
		&& element.id == focusedElement.id 
		&& typeof(element) != "undefined")
	{
		var sameValueWithFormat = (element.displayValue && element.editValue && element.value == element.displayValue && focusedValue == element.editValue );
		if(element.value != focusedValue && !sameValueWithFormat)
		{
			if (element.SERVOY_DONT_UPDATE_THIS_BECAUSE_ITS_NOT_CHANGED)
			{
				element.SERVOY_DONT_UPDATE_THIS_BECAUSE_ITS_NOT_CHANGED = false;
			}
			else 
			{
				element.value = focusedValue;
			}
		}
		else
		{
			if (element != focusedElement)
			{
				// only restore cursor and selection if element was replaced by AJAX
				var focusedElementType = focusedElement.getAttribute("type");
				if(focusedElementType == "text" || focusedElementType == "textarea")
				{
					// setting caret position will fire onfocus in chrome, so
					// remove it temporaly
					var temp = element.onfocus;
					element.onfocus = null;
					if (focusedElementSelection && focusedElementSelection.begin != focusedElementSelection.end)
					{
						if (sameValueWithFormat && focusedElementSelection.begin == 0 && focusedElementSelection.end == focusedValue.length)
						{
							// select all
							focusedElementSelection.end = element.value.length;
						}
						Servoy.Utils.createSelection(element,focusedElementSelection.begin,focusedElementSelection.end);
					}
					else
					{
						Servoy.Utils.doSetCaretPosition(element, focusedElementCaretPosition);
					}
					setTimeout(function() { element.onfocus = temp; }, 0);	
				}
			}
		}
	}
	focusedElement = null;
}

function testEnterKey(e, script) 
{
	return testKeyPressed(e, script,13);
}

function filterBackKey(e) 
{
	return testKeyPressed(e, null,8);
}

var delta = 500;
var lastKeypressTime = 0;

function testKeyPressed(e, script,keyCode) 
{
     var code;
     
     if (!e) e = window.event;
     if (!e) return true;
     if (e.keyCode) code = e.keyCode;
     else if (e.which) code = e.which;
     
     if(code==keyCode)
     {
    	if (keyCode == 38)
    	{
    		//up arrow was pressed
    		Servoy.Utils.downArrow = false;
    	}
    	else if (keyCode == 40)
    	{
    		// down arrow key pressed
	  		Servoy.Utils.downArrow = true;
    	}
    	else if (keyCode == 13)
    	{
    		// prevent double enter (action) trigger
    		var thisKeypressTime = new Date();
    		var doubleEnter = ( thisKeypressTime - lastKeypressTime <= delta );
    		lastKeypressTime = thisKeypressTime;
    		if ( doubleEnter )
    		{
    			return false;
    		}
    	}
        if (script) script();
	    return false;
     }
     return true;
}

function getStyle(_elem, _style)
{
	var computedStyle;
 	if (typeof _elem.currentStyle != 'undefined')
		computedStyle = _elem.currentStyle;
	else
		computedStyle = document.defaultView.getComputedStyle(_elem, null);
	return computedStyle[_style];
}

function getStyleNumeric(elem, style)
{
	var computedStyle = getStyle(elem, style);
	var numeric = parseInt(computedStyle);
	return isNaN(numeric) ? 0 : numeric;
}

function rearrageTabsInTabPanel(tabPanelId)
{
	var tabPanel = document.getElementById(tabPanelId);
	if (tabPanel)
	{
		var originalTabHolders = new Array();
		var allTabsTogether = new Array();
		var rowMargin;
		var markedFirstTab;
		var formContainer;		
		
		var focusedTabLink;
		var focusedElement = Wicket.Focus.getFocusedElement();
		if(focusedElement && focusedElement.className && focusedElement.className == 'tablink')
		{
			focusedTabLink = focusedElement;
		}
		
		for (var i=0; i < tabPanel.childNodes.length; i++)
		{
			var tabHolder = tabPanel.childNodes[i];
			if ((tabHolder.nodeType == 1) && (tabHolder.nodeName.toLowerCase() == "div"))
		   	{
	   			if (tabHolder.getAttribute("tabholder") == "true")
	   			{
					if (!rowMargin) rowMargin = getStyleNumeric(tabHolder, 'paddingLeft') + getStyleNumeric(tabHolder, 'paddingRight');
					originalTabHolders.push(tabHolder);
					for (var j=tabHolder.childNodes.length-1; j>=0; j--)
					{
						var tab = tabHolder.childNodes[j];
						if (tab.nodeName.toLowerCase() == "div") {
							var thisAnch = tab.getElementsByTagName("a")[0];
							if (thisAnch)
							{
								if (thisAnch.getAttribute("firsttab") == "true")
									markedFirstTab = tab;
								allTabsTogether.unshift(tab);
							}
						}
					}
				}
				else
				{
					if (!formContainer) formContainer = tabHolder;
				}
		  	}	
		}

		if (markedFirstTab)
		{
			while (allTabsTogether[0] != markedFirstTab)
				allTabsTogether.push(allTabsTogether.shift());
		}
		
		var rows = new Array();
		var selectedRowIndex = -1; 
		if (allTabsTogether.length > 0)
		{		
			if(tabPanel.clientWidth < 1)	// sometimes IE6,IE7 wrongly return 0 for clientWidth; delay rearrange in this case 
			{
				setTimeout(function(){rearrageTabsInTabPanel(tabPanelId);}, 500);
				return;
			}				
		
			var currentRow;
			var widthUntilNow = rowMargin; 
			var isFirst = true;
			for (var i=0; i<allTabsTogether.length; i++)
			{
				var tab = allTabsTogether[i];
				var anch = tab.getElementsByTagName("a")[0];
				var anchMargin = getStyleNumeric(anch, 'marginLeft') + getStyleNumeric(anch, 'marginRight');
				var anchExtra = 2; // 2px seem to be added to offsetWidth of each anchor, don't know why!?

				if (isFirst)
				{
					currentRow = new Array();
					isFirst = false;
				}
				else if (widthUntilNow + anchMargin + anchExtra + anch.offsetWidth >= tabPanel.clientWidth)
				{
					widthUntilNow = rowMargin;
					rows.push(currentRow);
					currentRow = new Array();
				}

				widthUntilNow += anchMargin + anchExtra + anch.offsetWidth;
				currentRow.push(tab);

				if ((tab.className == "selected_tab") || (tab.className == "disabled_selected_tab"))
					selectedRowIndex = rows.length;
			}
			rows.push(currentRow);
		}
		
		if (selectedRowIndex == -1) selectedRowIndex = 0; // If we have no selected row, then just take the first row.
		
		if (rows.length >= 1)
		{		
			var newFirstRow = (selectedRowIndex - 1 + rows.length) % rows.length;

			var domRows = new Array();
			for (var i=0; i<rows.length; i++)
			{
				var domRow = document.createElement("div");
				domRow.className = "tabs";
				domRow.setAttribute("tabholder", "true");
				if (i == selectedRowIndex)
				{
					domRow.style.marginTop = "0px";
					domRow.style.paddingTop = "0px";
				}
				else if (i == newFirstRow)
				{
					domRow.style.borderBottom = "0px";
					domRow.style.marginBottom = "0px";
					domRow.style.paddingBottom = "0px";
				}
				else
				{
					domRow.style.marginTop = "0px";
					domRow.style.marginBottom = "0px";
					domRow.style.borderBottom = "0px";
					domRow.style.paddingTop = "0px";
					domRow.style.paddingBottom = "0px";
				}
				for (var j=0; j<rows[i].length; j++)
				{
					var tab = rows[i][j];
					tab.parentNode.removeChild(tab);
					domRow.appendChild(tab);
				}
				domRows.push(domRow);
			}

			var arrInOrder = new Array();
			var firstRow = originalTabHolders[0];
			for (var i=selectedRowIndex; i<rows.length; i++)
			{
				arrInOrder.unshift(domRows[i]);
				tabPanel.insertBefore(domRows[i], firstRow);
				firstRow = domRows[i];
			}
			for (var i=0; i<selectedRowIndex; i++)
			{
				arrInOrder.unshift(domRows[i]);
				tabPanel.insertBefore(domRows[i], firstRow);
				firstRow = domRows[i];
			}

			for (var i=0; i<originalTabHolders.length; i++)
				tabPanel.removeChild(originalTabHolders[i]);

			var bottomOfLastRow = 0;
			for (var i=0; i<arrInOrder.length; i++)
			{
				var tabHolder = arrInOrder[i];
				tabHolder.style.top = bottomOfLastRow + "px";
				bottomOfLastRow += tabHolder.offsetHeight + getStyleNumeric(tabHolder, 'marginTop') + getStyleNumeric(tabHolder, 'marginBottom');
			} 
			formContainer.style.top = bottomOfLastRow + "px";
		}
		tabPanel.style.visibility = 'inherit';
		
		if(focusedTabLink)
		{
			Wicket.Focus.lastFocusId = focusedTabLink.id;
			if(typeof(Wicket.Focus.lastFocusId) != "undefined" && Wicket.Focus.lastFocusId != "" && Wicket.Focus.lastFocusId != null)
			{
				var toFocus = Wicket.$(Wicket.Focus.lastFocusId);
				if(toFocus != null && typeof(toFocus) != "undefined")
				{
					toFocus.focus();
				}
			}
		}
	}
}

var onFocusModifiers = 0;
var radioCheckInputMouseDown;

function eventCallback(el, strEvent, callbackUrl, event)
{
	if(strEvent != "focus" && Wicket.Focus.refocusLastFocusedComponentAfterResponse && !Wicket.Focus.focusSetFromServer) return true;
	
	if (ignoreFocusGained && ignoreFocusGained == el.id)
	{
		ignoreFocusGained = null;
		return true;
	}
	var modifiers;
	
	if(strEvent == "focus")
	{
		if(el.className && el.className == 'radioCheckInput' && radioCheckInputMouseDown) return false;

		modifiers = onFocusModifiers;
		onFocusModifiers = 0;			

		var delayedCall = el.onclick;

		if(modifiers > 0)
		{
			Wicket.Focus.lastFocusId = null;
			el.blur();
		}
		
		if(delayedCall)
		{
				var thisEl = el;
				setTimeout(function()
				{
					var wcall=wicketAjaxGet
					(					
						callbackUrl+'&event='+strEvent+'&id='+thisEl.id+'&modifiers='+modifiers,
						null,
						function() { onAjaxError(); }.bind(thisEl),
						function() { return Wicket.$(thisEl.id) != null; }.bind(thisEl)
					);		
				}, 300);				
		}
		else
		{
			var wcall=wicketAjaxGet
			(					
				callbackUrl+'&event='+strEvent+'&id='+el.id+'&modifiers='+modifiers,
				null,
				function() { onAjaxError(); }.bind(el),
				function() { return Wicket.$(el.id) != null; }.bind(el)
			);						
		}
		
		return false;
	}
	else
	{
		modifiers = Servoy.Utils.getModifiers(event);
		var wcall=wicketAjaxGet
		(					
			callbackUrl+'&event='+strEvent+'&id='+el.id+'&modifiers='+modifiers,
			null,
			function() { onAjaxError(); }.bind(el),
			function() { return Wicket.$(el.id) != null; }.bind(el)
		);
		return !wcall;
	}	
}

function postEventCallback(el, strEvent, callbackUrl, event, blockRequest)
{
	if(strEvent == "blur")
	{	
		ignoreFocusGained = null;
	}
	if(strEvent != "focus" && strEvent != "blur" && Wicket.Focus.refocusLastFocusedComponentAfterResponse && !Wicket.Focus.focusSetFromServer) return true;

	var modifiers;
	if(strEvent == "focus")
	{
		if(el.className && el.className == 'radioCheckInput' && radioCheckInputMouseDown) return false;
	
		modifiers = onFocusModifiers;
		onFocusModifiers = 0;

		var delayedCall = el.onclick;

		// if it has display/editvalues then test if the current value is the displayValue. if so only a get instead of a post. 
		if (Wicket.$(el.id).displayValue && Wicket.$(el.id).value == Wicket.$(el.id).displayValue)
		{
			if(modifiers > 0)
			{
				Wicket.Focus.lastFocusId = null;
				el.blur();
			}
			
			if(delayedCall)
			{
				var thisEl = el;
				setTimeout(function()
				{
					var wcall=wicketAjaxGet
					(
						callbackUrl+'&nopostdata=true&event='+strEvent+'&id='+thisEl.id+'&modifiers='+modifiers,
						null,
						function() { onAjaxError(); }.bind(thisEl),
						function() { onAjaxCall(); return Wicket.$(thisEl.id) != null; }.bind(thisEl)
					);
				}, 300);
			}
			else
			{
				var wcall=wicketAjaxGet
				(
					callbackUrl+'&nopostdata=true&event='+strEvent+'&id='+el.id+'&modifiers='+modifiers,
					null,
					function() { onAjaxError(); }.bind(el),
					function() { onAjaxCall(); return Wicket.$(el.id) != null; }.bind(el)
				);
			}
			return false;
		}
		
		if(modifiers > 0)
		{
			Wicket.Focus.lastFocusId = null;
			el.blur();
		}
		
		if(delayedCall)
		{
				var thisEl = el;
				setTimeout(function()
				{
					var wcall=wicketAjaxPost
					(
						callbackUrl+'&event='+strEvent+'&id='+thisEl.id+'&modifiers='+modifiers,
						wicketSerialize(Wicket.$(thisEl.id)),
						null,
						function() { onAjaxError(); }.bind(thisEl),
						function() { onAjaxCall(); return Wicket.$(thisEl.id) != null; }.bind(thisEl)
					);
				}, 300);						
		}
		else
		{
			var wcall=wicketAjaxPost
			(
				callbackUrl+'&event='+strEvent+'&id='+el.id+'&modifiers='+modifiers,
				wicketSerialize(Wicket.$(el.id)),
				null,
				function() { onAjaxError(); }.bind(el),
				function() { onAjaxCall(); return Wicket.$(el.id) != null; }.bind(el)
			);
		}
		return false;
	}
	else
	{
		modifiers = Servoy.Utils.getModifiers(event);
		// if it has display/editvalues then test if the current value is the displayValue. if so only a get instead of a post. 
		if (blockerOn || (Wicket.$(el.id).displayValue && Wicket.$(el.id).value == Wicket.$(el.id).displayValue))
		{
			var wcall=wicketAjaxGet
			(
				callbackUrl+'&nopostdata=true&event='+strEvent+'&id='+el.id+'&modifiers='+modifiers,
				null,
				function() { onAjaxError(); }.bind(el),
				function() { onAjaxCall(); return Wicket.$(el.id) != null; }.bind(el)
			);
			return !wcall;
		}
		
		var currentValue = Wicket.$(el.id).value;
		var wcall=wicketAjaxPost
		(
			callbackUrl+'&event='+strEvent+'&id='+el.id+'&modifiers='+modifiers,
			wicketSerialize(Wicket.$(el.id)),
			function() { if(blockRequest) hideBlocker(); }.bind(el),
			function() { onAjaxError(); if(blockRequest) hideBlocker(); }.bind(el),
			function() {onAjaxCall(); if(Wicket.$(el.id) != null){ if(blockRequest) { onABCDelayed();} return true;} else return false; 
		 }.bind(el)
		);
		return !wcall;
	}	
}

function focusMousedownCallback(e)
{
	if(!e) e = window.event;
	onFocusModifiers = Servoy.Utils.getModifiers(e);
	// if right click, set the alt key flag, as we need to handle
	// this case when changing the selection
	if((e.which && e.which == 3) || (e.button && e.button == 2))
	{
		onFocusModifiers += 4
	}	
}

var lastStyleSheetsNumber = false;

function testStyleSheets()
{
 	if(document.styleSheets.length >= 29)
 	{
 		// prevent infinite cycles
  		if (!lastStyleSheetsNumber )
  		{
  			Wicket.Log.info("Too many stylesheets loaded, IE doesn't support this.");
  			return;
  		}
  		window.location.reload();
 	} 
 	else 
 	{
  		lastStyleSheetsNumber = true;
 	}
}

function setStatusText(str)
{
	window.status = str;
}

function showInfoPanel(url,w,h,t,closeText)
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
}

if (typeof(Servoy.TableView) == "undefined")
{
	Servoy.TableView = 
	{	
		setRowStyleEl: function(el, bgcolor, fgcolor, fontStyle, fontWeight, fontSize, fontFamily, borderStyle, borderWidth, borderColor, isListView, inDepth)
		{//bgcolor and fgcolor comes directly as inline string: "background-color:#AAA;"
			var elChildren = el.childNodes;
			var elChildrenLen = elChildren.length;
	
			if (el.tagName && !(el.tagName.toLowerCase() == "td" && elChildrenLen == 1 && !elChildren[0].tagName))
			{
				// ignore the tableview filler (last column) 
				if(el.attributes['id'] && (!isListView || (el.attributes['class'] && (el.attributes['class'].value == 'listViewItem'))))
				{				   
					if(bgcolor == ''){
					    el.style.backgroundColor = bgcolor;
					}else{
						var styleAttrVal = el.getAttribute('style');
						if (bgcolor.indexOf("rgba") != -1 && el.tagName.toLowerCase() == "td")
						{
							// do not apply semi transparent color, has to be applied only once (on element)
							el.setAttribute('style',styleAttrVal+';background-color:transparent;');
						}
						else
						{
							el.setAttribute('style',styleAttrVal+';' +bgcolor);
						}
					}
					if(fgcolor == ''){
					    el.style.color = fgcolor;
					}else{
						var styleAttrVal = el.getAttribute('style');
						el.setAttribute('style',styleAttrVal+';'+fgcolor);
					}
										 
					el.style.fontStyle = fontStyle;
					el.style.fontWeight = fontWeight;
					el.style.fontSize = fontSize;
					el.style.fontFamily = fontFamily;

					if(el.tagName && el.tagName.toLowerCase() != "td")
					{
						var bStyleTop = "";
						var bStyleBottom = "";
						var bStyleLeft = "";
						var bStyleRight = "";
						var changing = true;
						
						if (!el.styleBackup) el.styleBackup = {};
						if(borderWidth != '' || borderStyle != '' || borderColor != '')
						{
							var bordersWidth = borderWidth.split(' ');
							var defaultBorderWidth = bordersWidth.length > 0 ? bordersWidth[0] + " " : '' 
							
							var borderColors = borderColor.split(' ');
							var defaultBorderColor = borderColors.length > 0 ? " " + borderColors[0] : '' 
							
							bStyleTop = defaultBorderWidth + borderStyle + defaultBorderColor;
							bStyleRight = (bordersWidth.length > 1 ? bordersWidth[1] + " " : defaultBorderWidth) + borderStyle + (borderColors.length > 1 ? " " + borderColors[1] : defaultBorderColor);							
							bStyleBottom = (bordersWidth.length > 2 ? bordersWidth[2] + " " : defaultBorderWidth) + borderStyle + (borderColors.length > 2 ? " " + borderColors[2] : defaultBorderColor);
							bStyleLeft = (bordersWidth.length > 3 ? bordersWidth[3] + " " : defaultBorderWidth) + borderStyle + (borderColors.length > 3 ? " " + borderColors[3] : defaultBorderColor);
						} else {
							// restore previous style if any
							changing = false;
							if (el.styleBackup.bStyleTop) bStyleTop = el.styleBackup.bStyleTop;
							if (el.styleBackup.bStyleRight) bStyleRight = el.styleBackup.bStyleRight;
							if (el.styleBackup.bStyleBottom) bStyleBottom = el.styleBackup.bStyleBottom;
							if (el.styleBackup.bStyleLeft) bStyleLeft = el.styleBackup.bStyleLeft;
						}
						if (changing) el.styleBackup.bStyleTop = el.style.borderTop;
						el.style.borderTop = bStyleTop;
						if (changing) el.styleBackup.bStyleBottom = el.style.borderBottom;
						el.style.borderBottom = bStyleBottom;
						
						if(Servoy.TableView.isInFirstTD(el))// ||( jQuery.browser.msie && jQuery.browser.version < 9 ))
						{
							if (changing) el.styleBackup.bStyleLeft = el.style.borderLeft;
							el.style.borderLeft = bStyleLeft;
						}
						else if(Servoy.TableView.isInLastTD(el))
						{
							if (changing) el.styleBackup.bStyleRight = el.style.borderRight;
							el.style.borderRight = bStyleRight;
						}
					}
				}
			}
			if (inDepth)
			{
				var continueInDepth = (el.className != 'tableviewcell');
				for(var i = 0; i < elChildrenLen; i++)
				{
					if(elChildren[i].tagName)
					{	
						Servoy.TableView.setRowStyleEl(elChildren[i], bgcolor, fgcolor, fontStyle, fontWeight, fontSize, fontFamily, borderStyle, borderWidth, borderColor, isListView, continueInDepth);
					}
				}
			}
		},

		setRowStyle: function(rowId, bgcolor, fgcolor, fontStyle, fontWeight, fontSize, fontFamily, borderStyle, borderWidth, borderColor, isListView)
		{
			var rowEl = document.getElementById(rowId);
			if(rowEl)
			{   
			   var rowChildren = $(rowEl).children();
			   var rowChildrenLen = $(rowEl).children().size();			    

				for(var i = 0; i < rowChildrenLen; i++)
				{
					if(rowChildren[i].tagName)
					{
					   if(!$.isArray(bgcolor)){
						 Servoy.TableView.setRowStyleEl(rowChildren[i], bgcolor, fgcolor, fontStyle, fontWeight, fontSize, fontFamily, borderStyle, borderWidth, borderColor, isListView, true);
						}else {
						 Servoy.TableView.setRowStyleEl(rowChildren[i], bgcolor[i], fgcolor[i], fontStyle[i], fontWeight[i], fontSize[i], fontFamily[i], borderStyle[i], borderWidth[i], borderColor[i], isListView, true);
						}
					}
				}				
			}
		},

		setTableColumnWidthEl: function(tableElement, classid, width)
		{
			if(YAHOO.util.Dom.hasClass(tableElement, classid) && tableElement.style)
			{
				tableElement.style["width"] = "" + width + "px";
				return;
			}
		
			var tableElementChildren = tableElement.childNodes;
			if(tableElementChildren)
			{
				for(var i = 0; i < tableElementChildren.length; i++)
					Servoy.TableView.setTableColumnWidthEl(tableElementChildren[i], classid, width);
			}
		},

		setTableColumnWidth: function(table, classid, width)
		{
			var tableEl = document.getElementById(table);
			if(tableEl)
				Servoy.TableView.setTableColumnWidthEl(tableEl, classid, width);
		},
		
		appendRowsTimer: null,
		isAppendingRows: false,
		currentScrollTop: new Array(),
		hasTopBuffer: new Array(),
		hasBottomBuffer: new Array(),
		keepLoadedRows : false,
		topPhHeight: new Array(),
		scrollCallback: new Array(),
		selectIndexTimer:null,
		

		appendRows: function(rowContainerBodyId, rows, newRowsCount, rowsCountToRemove, scrollDiff, hasTopBuffer, hasBottomBuffer,clearAllRows)
		{	
			var rowContainerBodyEl = document.getElementById(rowContainerBodyId);
			
			var row, rowHeight = 0, topPhChanged = false;
			var $tableBody = $('#' + rowContainerBodyId);
			if(scrollDiff > 0)
			{
				
				if((rowsCountToRemove > 0||clearAllRows )&& !Servoy.TableView.keepLoadedRows)
				{
					row =$tableBody.children('tr:first');
					var topPhHeight = 0;
					if(row.attr('id') == 'topPh')
					{
						if(!clearAllRows)topPhHeight = row.height();
						row.remove();
					}
					
					for(var i = 0; i < rowsCountToRemove; i++)
					{ 
						row = $tableBody.children('tr:first');
						if(rowHeight == 0) rowHeight = row.height();
						topPhHeight += rowHeight;
						if(row !=null)row.remove();
					}
					if(clearAllRows){
						row =null;						
						while($tableBody.children().size()>0 ){
							row = $tableBody.children('tr:first');
							row.remove();
						}
					}
					Servoy.TableView.topPhHeight[rowContainerBodyId] = topPhHeight;
					topPhChanged = true;
					$('#' + rowContainerBodyId).prepend("<tr id='topPh' height='" + topPhHeight + "'></tr>");
				}
				
				if(newRowsCount > 0) $('#' + rowContainerBodyId).append(rows);
			}
			else
			{
				row = $('#' + rowContainerBodyId).children('tr:last');
				rowHeight = row.height();
			
				if(rowsCountToRemove == -1)	// remove all
				{
					while((row = $tableBody.children('tr:last')).length > 0 && (row.attr('id') != 'topPh')) row.remove();
				}
				else
				{
					for(var i = 0; i < rowsCountToRemove; i++)
					{ 
						row = $tableBody.children('tr:last');
						row.remove();
					}
				}
			
				if(newRowsCount > 0)
				{
					var topPhHeight = 0;

					row = $tableBody.children('tr:first');
						
					if(row.attr('id') == 'topPh')
					{
						topPhHeight = row.height();
						row.remove();
					}
					
					topPhHeight -= rowHeight * newRowsCount;
					
					$tableBody.prepend(rows);
					if(topPhHeight > 0)
					{
						Servoy.TableView.topPhHeight[rowContainerBodyId] = topPhHeight;
						$tableBody.prepend("<tr id='topPh' height='" + topPhHeight + "'></tr>");
						if(Servoy.TableView.currentScrollTop[rowContainerBodyId] < topPhHeight)
						{
							Servoy.TableView.currentScrollTop[rowContainerBodyId] = topPhHeight;
						}
					}
					else
					{
						Servoy.TableView.topPhHeight[rowContainerBodyId] = 0;
					}
					topPhChanged = true;
				}
			}

			setTimeout(function() {
				$tableBody.scrollTop(Servoy.TableView.currentScrollTop[rowContainerBodyId]);
				Servoy.TableView.hasTopBuffer[rowContainerBodyId] = hasTopBuffer;
				Servoy.TableView.hasBottomBuffer[rowContainerBodyId] = hasBottomBuffer;
				Servoy.TableView.isAppendingRows = false;				
			}, 0);
			
			if(topPhChanged)
			{
				setTimeout(function() { Servoy.TableView.updateScrollTopPlaceholder(rowContainerBodyEl, Servoy.TableView.scrollCallback[rowContainerBodyId], Servoy.TableView.topPhHeight[rowContainerBodyId]);}, 0);
			}
		},

		needToUpdateRowsBuffer: function(rowContainerBodyId)
		{
			if(Servoy.TableView.isAppendingRows || (!Servoy.TableView.hasTopBuffer[rowContainerBodyId] && !Servoy.TableView.hasBottomBuffer[rowContainerBodyId]))
			{
				return 0;
			}

			var scrollTop = $('#' + rowContainerBodyId).scrollTop()
			var scrollDiff = scrollTop - Servoy.TableView.currentScrollTop[rowContainerBodyId];
			if(scrollDiff == 0 || (Servoy.TableView.keepLoadedRows && (scrollDiff < 0))) return 0;
			Servoy.TableView.currentScrollTop[rowContainerBodyId] = scrollTop;

			var rowContainerBodyEl = document.getElementById(rowContainerBodyId);			
			var clientHeight = rowContainerBodyEl.clientHeight;
			var scrollHeight = rowContainerBodyEl.scrollHeight;
			var bufferedRows = scrollHeight - scrollTop - clientHeight;

			if(scrollDiff > 0)
			{
				Servoy.TableView.isAppendingRows = Servoy.TableView.hasBottomBuffer[rowContainerBodyId] && (bufferedRows < clientHeight);
			}
			else
			{
				var row = $('#' + rowContainerBodyId).children('tr:first');
				var topPhHeight = 0;
				if(row.attr('id') == 'topPh')
				{
					topPhHeight = row.height(); 
				}
				Servoy.TableView.isAppendingRows = Servoy.TableView.hasTopBuffer[rowContainerBodyId] && (scrollTop - topPhHeight < clientHeight);
			}
			
			// get the second row for height
			var secondRow = $('#' + rowContainerBodyId).children('tr').eq(1);
			var rowHeight = secondRow.height();

			var nrRows = scrollDiff / rowHeight;
			var nrRowAbs = Math.ceil(Math.abs(nrRows));
			nrRows = nrRows < 0 ? -nrRowAbs : nrRowAbs;
			return Servoy.TableView.isAppendingRows ? nrRows : 0;
		},
		
		scrollIntoView : function (rowContainerBodyId, delay, scroll){
			delay = typeof delay !== 'undefined' ? delay : 1000;
			if(Servoy.TableView.selectIndexTimer) clearTimeout(Servoy.TableView.selectIndexTimer);
			Servoy.TableView.selectIndexTimer = setTimeout(function (){$('#'+rowContainerBodyId).scrollTop(scroll);} , delay);
		},
		
		scrollToTop: function(rowContainerBodyId)
		{
        		if(Servoy.TableView.currentScrollTop[rowContainerBodyId] > 0)
        		{
        			if(Servoy.TableView.topPhHeight[rowContainerBodyId])
        			{
        				$('#' + rowContainerBodyId).prepend("<tr id='topPh' height='" + Servoy.TableView.topPhHeight[rowContainerBodyId] + "'></tr>");
        			}
        			$('#' + rowContainerBodyId).scrollTop(Servoy.TableView.currentScrollTop[rowContainerBodyId]);
        		}
		},

		scrollHeader: function(headerId, rowContainerBodyId)
		{
			$('#' + headerId).css('left', -$('#' + rowContainerBodyId).scrollLeft() + 'px');
		},

		isInFirstTD: function(el)
		{
			var targetTD = YAHOO.util.Dom.getAncestorBy (el , function(el)
			{
				return el.tagName && el.tagName.toLowerCase() == "td";
			});				
		
			if(targetTD)
			{
				var nextTD = YAHOO.util.Dom.getPreviousSiblingBy(targetTD, function(el)
				{
					return el.tagName && el.tagName.toLowerCase() == "td";
				});
				
				if(!nextTD) return true;
			}
		
			return false;
		},
		
		isInLastTD: function(el)
		{
			var targetTD = YAHOO.util.Dom.getAncestorBy (el , function(el)
			{
				return el.tagName && el.tagName.toLowerCase() == "td";
			});				
		
			if(targetTD)
			{
				var nextTD = YAHOO.util.Dom.getNextSiblingBy(targetTD, function(el)
				{
					return el.tagName && el.tagName.toLowerCase() == "td" && el.id;
				});
				
				if(!nextTD) return true;
			}
		
			return false;
		},
		
		updateScrollTopPlaceholder: function(scrollEl, callbackUrl, newValue)
		{
			wicketAjaxGet
			(					
				callbackUrl+'&topPh='+newValue,
				null,
				function() { onAjaxError(); }.bind(scrollEl),
				function() { return Wicket.$(scrollEl.id) != null; }.bind(scrollEl)
			);
		}
	};
}

if (typeof(Servoy.DD) == "undefined")
{
	Servoy.DD =
	{
		isDragStarted: false,
		isDragging: false,
		isRestartTimerNeeded: false,
		currentElement: new Array(),
		dropCallback: new Array(),
		mouseDownEvent: null,		
		klEsc: null,

		dragStarted: function()
		{
			var focusedElement = Wicket.Focus.getFocusedElement();
			if (typeof(focusedElement) != "undefined" && focusedElement != null)
			{
				focusedElement.blur();
			}
			
			Servoy.DD.isDragging = true;
			Servoy.DD.isDragStarted = true;
			Servoy.DD.isRestartTimerNeeded = false;
			if (Servoy.DD.klEsc)
				Servoy.DD.klEsc.enable();
		},
	
		dragStopped: function()
		{
			if (Servoy.DD.klEsc)
				Servoy.DD.klEsc.disable();
			Servoy.DD.isDragging = false;			
			if(window.restartTimer && Servoy.DD.isRestartTimerNeeded)
			{
				window.restartTimer();
			}
		},

		cancelDrag: function()
		{
			if(Servoy.DD.mouseDownEvent != null)
			{
				YAHOO.util.DragDropMgr.stopDrag(Servoy.DD.mouseDownEvent, false);
			}
		},
	
		attachDrag: function (array, callback , bUseProxy, bResizeProxyFrame, bXConstraint, bYConstraint)
		{
			YAHOO.util.DDM.mode = YAHOO.util.DDM.INTERSECT;
			if(Servoy.DD.klEsc == null)
			{
				Servoy.DD.klEsc = new YAHOO.util.KeyListener(document, {keys:27}, {fn:Servoy.DD.cancelDrag,scope:Servoy.DD,correctScope:true }, "keyup" );
			}
			

			for(var i = 0; i < array.length; i++)
			{
				var dd;
				if(bUseProxy)
				{
					dd = new YAHOO.util.DDProxy(array[i], null, {resizeFrame : bResizeProxyFrame, centerFrame : true});
					dd.setDragElId("servoydndproxy");
				}
				else
					dd = new YAHOO.util.DD(array[i]);

				Servoy.DD.disableSelection(document.getElementById(dd.id));

				if(bXConstraint)
				{
					dd.setXConstraint(0, 0);
				}
				if(bYConstraint)
				{
					dd.setYConstraint(0, 0);
				}			
					
				dd.onMouseDown = function(e) {
					// this is for popup menu close; dd eats the event and the menu remains open
					// make sure we close it
					var element = document.getElementById("basicmenu");
					if (element && element.parentNode)
					{
						element.parentNode.removeChild(element);
					}	
				};									

				dd.on('b4MouseDownEvent', function(ev)
				{
					var dragTarget = YAHOO.util.Event.getTarget(ev);
					if(Servoy.DD.isTargetDraggable(dragTarget))
					{
						// we do want the click for input, so it can be edited;
						// the mouse down is also needed in listview to set the selection
						YAHOO.util.DDM.preventDefault = false;
						Servoy.DD.mouseDownEvent = ev;
						return true;
					}
					return false;
				}, dd, true);
				
				dd.on('b4StartDragEvent', function()
				{
					// needed to avoid 'text' dragging
					YAHOO.util.DDM.preventDefault = true;
					
					var x = YAHOO.util.Event.getPageX(Servoy.DD.mouseDownEvent);
					var y = YAHOO.util.Event.getPageY(Servoy.DD.mouseDownEvent);
					var m = Servoy.Utils.getModifiers(Servoy.DD.mouseDownEvent);

					if(this.id)
					{
						if(this.id.indexOf("headerColumnTable") == 0)
						{
							Servoy.DD.setupHoverElements(this.id, x);
						}
						else if(this.id.indexOf("resizeBar") == 0)
						{
							Servoy.DD.setupResizeElement(this.id, x);
						}
					}
					wicketAjaxGet(callback + '&a=aStart&xc=' + x + '&yc=' + y + '&m=' + m + '&draggableID=' + this.id);
					Servoy.DD.dragStarted();
					return true;
				}, dd, true);				

				dd.onDrag = function(ev)
				{
					if(Servoy.DD.resizeElContainers)
					{
						Servoy.DD.doColumnResize(ev);
					}
				};

				dd.endDrag = function(e) {
					if (bUseProxy && bResizeProxyFrame && dd && dd.getDragEl())
					{
						// for some reason, yui doesn't set size to some default value so old values remain
						// these values are taken from MainPage.html, modify in both places
						YAHOO.util.Dom.setStyle( dd.getDragEl(), "width", "20px" );
						YAHOO.util.Dom.setStyle( dd.getDragEl(), "height", "10px" );
					}
					Servoy.DD.clearResize();
					Servoy.DD.clearHover();
					Servoy.DD.dragStopped();
					Servoy.DD.currentElement = new Array();
					//reset preventDefault to 'true'
					if(!YAHOO.util.DDM.preventDefault) YAHOO.util.DDM.preventDefault = true;
					var x = YAHOO.util.Event.getPageX(e);
					var y = YAHOO.util.Event.getPageY(e);				
					var m = Servoy.Utils.getModifiers(e);	
					wicketAjaxGet(callback + '&a=aEnd&xc=' + x + '&yc=' + y + '&m=' + m + '&draggableID=' + this.id);
				};
					
				dd.onDragEnter = function(ev, targetid) {
					var target;					
					for(var i in targetid)
					{						
						target = targetid[i].id;
						if(Servoy.DD.isHiddenByCurrentElement(target))
						{
							var currentTarget = Servoy.DD.currentElement[Servoy.DD.currentElement.length - 1];
							Servoy.DD.currentElement[Servoy.DD.currentElement.length - 1] = target;
							Servoy.DD.currentElement[Servoy.DD.currentElement.length] = currentTarget;
						}
						else
						{
							Servoy.DD.currentElement[Servoy.DD.currentElement.length] = target;
						}
					}
 
 					if(target == Servoy.DD.currentElement[Servoy.DD.currentElement.length - 1])
 					{
 						Servoy.DD.setHoverEl(target);
 						var m = Servoy.Utils.getModifiers(ev);
						wicketAjaxGet(Servoy.DD.dropCallback[target] + '&a=aHover&draggableID=' + this.id + '&m=' + m + '&targetID=' + target);
					}
				};

				dd.onDragOut = function(ev, targetid)
				{
					if(Servoy.DD.hoverEl)
					{
						for(var i in targetid)
						{						
							var el = targetid[i].getEl();
							if(Servoy.DD.hoverEl == el.id) Servoy.DD.setHoverEl(null);
						}
					}					
					if(Servoy.DD.currentElement.length > 0)
					{
						var hoverTarget = null;
						var newCurrentElement = new Array();
						var currentIdx = 0;
						for(var i in Servoy.DD.currentElement)
						{
							var shouldRemove = false;
							for(var j in targetid)
							{
								if(Servoy.DD.currentElement[i] == targetid[j].id)
								{
									shouldRemove = true;
									if(hoverTarget == null && currentIdx > 0 )
									{
										hoverTarget = Servoy.DD.currentElement[currentIdx - 1];
									}
								}
							}
							if(!shouldRemove)
							{
								newCurrentElement[newCurrentElement.length] = Servoy.DD.currentElement[i];
							}
							currentIdx++;
						}
						Servoy.DD.currentElement = newCurrentElement;
						if(hoverTarget != null && Servoy.DD.currentElement.length > 0 && hoverTarget == Servoy.DD.currentElement[Servoy.DD.currentElement.length-1])
						{
							Servoy.DD.setHoverEl(hoverTarget);
							var m = Servoy.Utils.getModifiers(ev);
							wicketAjaxGet(Servoy.DD.dropCallback[hoverTarget] + '&a=aHover&draggableID=' + this.id + '&m=' + m + '&targetID=' + hoverTarget);
						}
					}
				};

				dd.onDragDrop = function(ev, targetid) {
					if (Servoy.DD.isDragStarted)
					{
						targetid = targetid[targetid.length-1].id
						if (Servoy.DD.currentElement.length > 0)
						{
							targetid = Servoy.DD.currentElement[Servoy.DD.currentElement.length-1];
						}
						
						var x = YAHOO.util.Event.getPageX(ev);
						var y = YAHOO.util.Event.getPageY(ev);			
						var m = Servoy.Utils.getModifiers(ev);		
						wicketAjaxGet(Servoy.DD.dropCallback[targetid] + '&a=aDrop&xc=' + x + '&yc=' + y + '&m=' + m + '&draggableID=' + this.id  + '&targetID=' + targetid);
						Servoy.DD.isDragStarted = false;
					}
				};

				dd.onDragOver = function(ev, targetid) {
					if(Servoy.DD.hoverEl)
					{
						for(var i in targetid)
						{						
							var el = targetid[i].getEl();
							if(Servoy.DD.hoverEl == el.id)
							{
								var moveOffset = YAHOO.util.Event.getPageX(ev) - Servoy.DD.moveElXPosition ;
								if(Servoy.DD.moveElWidth / 2 < Math.abs(moveOffset))
								{								
									var moveBorderStyle = "2px solid red";
									var offsetX = YAHOO.util.Event.getPageX(ev) - Servoy.DD.hoverElContainerXPosition;
									var usedBorder = (offsetX < Servoy.DD.hoverElContainerWidth / 2) ? "border-left" : "border-right";
									
									var currentBorder = YAHOO.util.Dom.getStyle(Servoy.DD.hoverElContainer, usedBorder);
									if(currentBorder != moveBorderStyle)
									{
										if(usedBorder == "border-left")
										{
											YAHOO.util.Dom.setStyle(Servoy.DD.hoverElContainer, "border-right", Servoy.DD.hoverElContainerBorder == null ? "" : Servoy.DD.hoverElContainerBorder);
										}
										else
										{
											YAHOO.util.Dom.setStyle(Servoy.DD.hoverElContainer, "border-left", "");
										}
										
										YAHOO.util.Dom.setStyle(Servoy.DD.hoverElContainer, usedBorder, moveBorderStyle);
									}
								}
								else
								{
									YAHOO.util.Dom.setStyle(Servoy.DD.hoverElContainer, "border-right", Servoy.DD.hoverElContainerBorder == null ? "" : Servoy.DD.hoverElContainerBorder);
									YAHOO.util.Dom.setStyle(Servoy.DD.hoverElContainer, "border-left", "");
								}
							}
						}
					}
				};

				Servoy.DD.dropCallback[array[i]] = callback;	
			}
		},

		attachDrop: function(array, callback)
		{
			for(var i = 0; i < array.length; i++)
			{
				var ddtarget = new YAHOO.util.DDTarget(array[i]);
				Servoy.DD.dropCallback[array[i]] = callback;
			}
		},
		
		isTargetDraggable: function(target)
		{
			var targetAncestor;
			if(Servoy.DD.isDragDropContainer(target))
			{
				targetAncestor = target;
			}
			else
			{
				targetAncestor = YAHOO.util.Dom.getAncestorBy ( target , function(el)
				{
					return Servoy.DD.isDragDropContainer(el);
				});
			}

			if(targetAncestor)
			{
				var ddByID = YAHOO.util.DragDropMgr.getDDById(targetAncestor.id);
				return ddByID && !(ddByID instanceof YAHOO.util.DDTarget);
			}

			return false;
		},
		
		disableSelection: function(target) {
			if(typeof target.style.webkitUserSelect!="undefined") //Chrome,Safari
				target.style.webkitUserSelect="none";
			else if (typeof target.style.MozUserSelect!="undefined") //Firefox
				target.style.MozUserSelect="none";
			else if(typeof target.style.MsUserSelect!="undefined") //IE 10+
				target.style.MsUserSelect="none";
			else if(typeof target.onselectstart!="undefined")
				target.onselectstart = function () { return false; }; //old IE
			else if(typeof target.style.unselectable!="undefined") //Opera
				target.style.unselectable="on";
		},
		
		isDragDropContainer: function(target)
		{
			return YAHOO.util.DragDropMgr.isDragDrop(target.id) || YAHOO.util.Dom.hasClass(target, 'formpart') || YAHOO.util.Dom.hasClass(target, 'tabpanel');
		},
		
		isHiddenByCurrentElement: function(target)
		{
			if(Servoy.DD.currentElement.length > 1)
			{
				var currentElementID = Servoy.DD.currentElement[Servoy.DD.currentElement.length - 1];
				var currentElementParentID = YAHOO.util.Dom.get(currentElementID).parentNode.id;
				var targetAncestor;
				var targetElement = YAHOO.util.Dom.get(target);
				if(targetElement.parentNode.id == currentElementParentID)
				{
					targetAncestor = targetElement;
				}
				else
				{
					targetAncestor = YAHOO.util.Dom.getAncestorBy (targetElement , function(el)
					{
						return currentElementParentID == el.parentNode.id;
					});				
				}
				if(targetAncestor && currentElementID != targetAncestor.id)
				{
					var coveringCurrentElement = YAHOO.util.Dom.getNextSiblingBy(targetAncestor, function(el)
					{
						return el.id == currentElementID;
					});
					
					if(coveringCurrentElement) return true;
				}				
			}

			return false;
		},

		hoverEl: null,
		hoverElContainer: null,
		hoverElContainerBorder: null,
		hoverElContainerWidth: null,
		hoverElContainerXPosition: null,
		hoverElements: null,
		moveElWidth: null,
		moveElXPosition: null,
		
		setHoverEl: function(el)
		{
			if(!el || Servoy.DD.isHoverElement(el))
			{
				if(Servoy.DD.hoverEl)
				{
					YAHOO.util.Dom.setStyle(Servoy.DD.hoverElContainer, "border-left", "");
					YAHOO.util.Dom.setStyle(Servoy.DD.hoverElContainer, "border-right", Servoy.DD.hoverElContainerBorder == null ? "" : Servoy.DD.hoverElContainerBorder);
				}
				Servoy.DD.hoverEl = el;
				if(el)
				{
					Servoy.DD.hoverElContainer = YAHOO.util.Dom.getAncestorByTagName(el, "th");
					if(Servoy.DD.hoverElContainer)
					{
						Servoy.DD.hoverElContainerBorder = YAHOO.util.Dom.getStyle(Servoy.DD.hoverElContainer, "border-right");
						Servoy.DD.hoverElContainerWidth = YAHOO.util.Dom.getStyle(Servoy.DD.hoverElContainer, "width");
						Servoy.DD.hoverElContainerWidth = parseInt(Servoy.DD.hoverElContainerWidth.substring(0, Servoy.DD.hoverElContainerWidth.indexOf("px")));
						Servoy.DD.hoverElContainerXPosition = YAHOO.util.Dom.getX(Servoy.DD.hoverElContainer);
					}
				}
				else
				{
					Servoy.DD.hoverElContainer = null;
					Servoy.DD.hoverElContainerBorder = null;
					Servoy.DD.hoverElContainerWidth = null;
					Servoy.DD.hoverElContainerXPosition = null;
				}
			}
		},
		
		clearHover: function()
		{
			Servoy.DD.setHoverEl(null);
			Servoy.DD.hoverElements = null;
			Servoy.DD.moveElXPosition = null;
			Servoy.DD.moveElWidth = null;
		},
		
		setupHoverElements: function(dragEl, x)
		{
			Servoy.DD.moveElXPosition = x;
			Servoy.DD.moveElWidth = YAHOO.util.Dom.getStyle(dragEl, "width");
			Servoy.DD.moveElWidth = parseInt(Servoy.DD.moveElWidth.substring(0, Servoy.DD.moveElWidth.indexOf("px")));			

			var column = YAHOO.util.Dom.getAncestorByTagName(dragEl, "th");
			if(column)
			{
				Servoy.DD.hoverElements = new Array();
				var siblingColumn = column;
				
				while((siblingColumn = YAHOO.util.Dom.getPreviousSiblingBy(siblingColumn, function(el) { return el.tagName && el.tagName.toLowerCase() == "th"; })) != null)
				{
					var siblingColumnTable = Servoy.DD.getHoverColumnTable(siblingColumn);
					if(siblingColumnTable != null)
					{
						Servoy.DD.hoverElements[Servoy.DD.hoverElements.length] = siblingColumnTable.id;
					}
					else
					{
						break;
					}
				}
				siblingColumn = column;
				while((siblingColumn = YAHOO.util.Dom.getNextSiblingBy(siblingColumn, function(el) { return el.tagName && el.tagName.toLowerCase() == "th"; })) != null)
				{
					var siblingColumnTable = Servoy.DD.getHoverColumnTable(siblingColumn);
					if(siblingColumnTable != null)
					{
						Servoy.DD.hoverElements[Servoy.DD.hoverElements.length] = siblingColumnTable.id;
					}
					else
					{
						break;
					}
				}
			}
		},
		
		isHoverElement: function(el)
		{
			if(el && el.indexOf("headerColumnTable") == 0 && Servoy.DD.hoverElements)
			{
				for(var i in Servoy.DD.hoverElements)
				{
					if(el == Servoy.DD.hoverElements[i]) return true;
				}
			}

			return false;
		},
		
		getHoverColumnTable: function(column)
		{
			var siblingColumnTable = YAHOO.util.Dom.getFirstChildBy(column, function(el) { return el.tagName && el.tagName.toLowerCase() == "table"; });
			if(siblingColumnTable && siblingColumnTable.id && siblingColumnTable.id.indexOf("headerColumnTable") == 0)
			{
				return siblingColumnTable;
			}
			else
			{
				return null;
			}
		},
		
		resizeEl: null,
		resizeElXPosition: null,
		resizeElContainers: null,
		resizeElContainersWidth: null,
		
		setupResizeElement: function(el, x)
		{
			Servoy.DD.resizeEl = el;
			Servoy.DD.resizeElXPosition = x;
			Servoy.DD.resizeElContainers = new Array();
			Servoy.DD.resizeElContainersWidth = new Array();
			
			
			var cellClass = YAHOO.util.Dom.getAttribute(el, "class");
			cellClass = cellClass.substring(7);	// the cellClass is appended with resize_
			
			Servoy.DD.resizeElContainers[Servoy.DD.resizeElContainers.length] = YAHOO.util.Dom.getAncestorByTagName(el, "th"); 
			Servoy.DD.resizeElContainers[Servoy.DD.resizeElContainers.length] = YAHOO.util.Dom.getAncestorByTagName(el, "table");
			if(Servoy.DD.resizeElContainers[0])
			{
				// get header elements
				var columnDivs = YAHOO.util.Dom.getElementsBy(function(el) { return true; }, "div", Servoy.DD.resizeElContainers[0]);
				for(var i in columnDivs)
				{
					Servoy.DD.resizeElContainers[Servoy.DD.resizeElContainers.length] = columnDivs[i];
				}
				
				// get table cell elements
				var theTable = YAHOO.util.Dom.getAncestorByTagName(Servoy.DD.resizeElContainers[0], "table");
				var cellElements = YAHOO.util.Dom.getElementsByClassName(cellClass, null, theTable);
				for(var i in cellElements)
				{
					Servoy.DD.resizeElContainers[Servoy.DD.resizeElContainers.length] = cellElements[i];
				}
			}
		
			for(var i in Servoy.DD.resizeElContainers)
			{
				if(Servoy.DD.resizeElContainers[i])
				{
					Servoy.DD.resizeElContainersWidth[i] = YAHOO.util.Dom.getStyle(Servoy.DD.resizeElContainers[i], "width");					
					Servoy.DD.resizeElContainersWidth[i] = parseInt(Servoy.DD.resizeElContainersWidth[i].substring(0, Servoy.DD.resizeElContainersWidth[i].indexOf("px")));
				}
			}	 
		},
		
		clearResize: function()
		{
			Servoy.DD.resizeEl = null;
			Servoy.DD.resizeElXPosition = null;
			Servoy.DD.resizeElContainers = null;
			Servoy.DD.resizeElContainersWidth = null;
		},
		
		doColumnResize: function(ev)
		{		
			var offsetX = YAHOO.util.Event.getPageX(ev) - Servoy.DD.resizeElXPosition;
			for(var i in Servoy.DD.resizeElContainers)
			{
				YAHOO.util.Dom.setStyle(Servoy.DD.resizeElContainers[i], "width", Servoy.DD.resizeElContainersWidth[i] + offsetX + "px");
			}
		}
	};
}


/* Tooltip fctions */

var tipInitialTimeout, tipTimeout;

function showtip(event,message,initialDelay, dismissDelay)
{
	var e = event;
	if(!e) e = window.event;
	
	var targ;
	if(e.target) targ = e.target;
	else if (e.srcElement) targ = e.srcElement;
	if(targ.nodeType == 3) // defeat Safari bug
		targ = targ.parentNode;

	if(targ.tagName && targ.tagName.toLowerCase() == "option")	// stop tooltip if over option element
	{
		hidetip();
		return;
	}

	var m = document.getElementById('mktipmsg');

	m.innerHTML = message;
	m.style.zIndex = 1000;
	m.style.width = "";
	m.style.overflow = "hidden";
	
	tipmousemove(e);

	if(window.addEventListener)
	{
		document.addEventListener('mousemove', tipmousemove, false);
	}
	else
	{
		document.attachEvent('mousemove', tipmousemove);
	}
	tipInitialTimeout = setTimeout("adjustAndShowTooltip("+dismissDelay+");", initialDelay);
}

function adjustAndShowTooltip(dismissDelay)
{
	var x = 0;
	var y = 0;
		
	if(tipmousemouveEventX || tipmousemouveEventY)
	{
		if(tipmousemouveEventIsPage)
		{
			x = tipmousemouveEventX;
			y = tipmousemouveEventY;
		}
		else
		{
			x = tipmousemouveEventX + document.body.scrollLeft + document.documentElement.scrollLeft;
			y = tipmousemouveEventY + document.body.scrollTop + document.documentElement.scrollTop;
		}
	}

	var wWidth = 0, wHeight = 0;
  	if( typeof( window.innerWidth ) == 'number' )
  	{
    	//Non-IE
    	wWidth = window.innerWidth;
    	wHeight = window.innerHeight;
  	}
  	else if( document.documentElement && ( document.documentElement.clientWidth || document.documentElement.clientHeight ) )
  	{
    	//IE 6+ in 'standards compliant mode'
    	wWidth = document.documentElement.clientWidth;
    	wHeight = document.documentElement.clientHeight;
  	}
  	else if( document.body && ( document.body.clientWidth || document.body.clientHeight ) )
  	{
    	//IE 4 compatible
    	wWidth = document.body.clientWidth;
    	wHeight = document.body.clientHeight;
  	}

	m = document.getElementById('mktipmsg');
	// first just set it on position 0px and "show" it
	// this will result in the best possible view of the tooltip.
	m.style.left = "0px";	
	m.style.top = "0px";		
	m.style.display = "block";

    // by default set it on position x+10 (and y+10)
	var left = x + 10;
	// if now the total lenght doesn't fit anymore then extract it (moves to left)
	if (left + m.offsetWidth > wWidth) {
		left -= (left + m.offsetWidth - wWidth);
		// the complete tooltip is bigger then the whole page
		if (left < 0) left = 0;
	}
	m.style.left = left  + "px";
	
	var top = y + 10;
	// if total height bigger then the height then place it completely above
	// the mouse position.
	if (top + m.offsetHeight > wHeight) {
		top = y - 10 - m.offsetHeight;
	}
	m.style.top = top  + "px";

	tipTimeout = setTimeout("hidetip();", dismissDelay);
}

function hidetip()
{
	if(window.removeEventListener)
	{
		window.removeEventListener('mousemove', tipmousemove, false);
	}
	else
	{
		window.detachEvent('mousemove', tipmousemove);
	}
	clearTimeout(tipInitialTimeout);
	clearTimeout(tipTimeout);
	var m;
	m = document.getElementById('mktipmsg');
	m.style.display = "none";
}

var tipmousemouveEventX, tipmousemouveEventY, tipmousemouveEventIsPage;
function tipmousemove(e)
{
	if(e.pageX || e.pageY)
	{
		tipmousemouveEventIsPage = true;
		tipmousemouveEventX = e.pageX;
		tipmousemouveEventY = e.pageY;
	}
	else if(e.clientX || e.clientY)
	{
		tipmousemouveEventIsPage = false;
		tipmousemouveEventX = e.clientX;
		tipmousemouveEventY = e.clientY;
	}	
}

var previousText;
function onAjaxError()
{
	var indicator = document.getElementById('indicator');
	if ( indicator.innerText)
	{
		previousText = indicator.innerText;
		indicator.innerText = "Error calling server"; 
	}
	else
	{
		previousText = indicator.textContent;
		indicator.textContent = "Error calling server"; 
	}
	clickedElementId = null;	
	wicketShow('indicator');
	hideBlocker();
}

var blockerTimeout = null;
var blockerOn = false;

function onABC() {
	blockerOn = true;
	wicketShow('blocker');
	$('body').addClass('blocker');
	var e=wicketGet('blocker');
	if (e != null) e.focus();
	onAjaxCall();
}

function onABCDelayed() {
	if(blockerTimeout) clearTimeout(blockerTimeout);
	blockerTimeout = setTimeout(function() { onABC();}, 500)
}

function hideBlocker()
{
	blockerOn = false;
	if(blockerTimeout)
	{
		clearTimeout(blockerTimeout);
		blockerTimeout = null;
	}
	$('body').removeClass('blocker'); 
	return wicketHide('blocker');
}

function onAjaxCall()
{
	if (previousText)
	{
		var indicator = document.getElementById('indicator');
		if ( indicator.innerText)
		{
			indicator.innerText = previousText;
		}
		else
		{
			indicator.textContent = previousText;
		}
	}
}

function getRootServoyFrame()
{
	var mywindow = window;
	var win;
	while (typeof(mywindow.parent) != "undefined" && mywindow != mywindow.parent) {
		try {
			win = mywindow.parent.Wicket.Window;
		} catch (ignore) { win = null; }
		if (typeof(win) != "undefined" && win != null) {
			mywindow = mywindow.parent
		} else {
			break
		}
	}
	return mywindow;
}

function showurl(url, timeout, onRootFrame, useIFrame, pageExpiredRedirect)
{
	var mywindow = window;

	if (onRootFrame || useIFrame) {
		mywindow = getRootServoyFrame();
	}

	if (useIFrame) {
		var ifrm = mywindow.frames['srv_downloadframe'];
		if (ifrm) {
			ifrm.location = url;
		} else {
			ifrm = document.createElement("IFRAME");
			ifrm.setAttribute("src", url);
			ifrm.setAttribute('id', 'srv_downloadframe');
			ifrm.setAttribute('name', 'srv_downloadframe');
			ifrm.style.width = 0 + "px";
			ifrm.style.height = 0 + "px";
			mywindow.document.body.appendChild(ifrm);
		}
	} else {
		if (!(mywindow.Servoy.redirectingOnSolutionClose && pageExpiredRedirect)) {
			mywindow.setTimeout("window.document.location.href='" + url + "'", timeout);
		}
	}
}

function getPreferredTableSize(startElementId)
{
	var el = document.getElementById(startElementId);
	if (el && el.clientWidth > 0 && el.clientHeight > 0) {
		return [el.clientWidth, el.clientHeight];
	}

    return null;
}

function getPreferredComponentSize(startElementId)
{
	var el = document.getElementById(startElementId);
	if(el)
	{
		return [el.clientWidth,el.clientHeight];
	}
	return null;
}
var validationFailedId = null;
function setValidationFailed(id)
{
	validationFailedId = id;
}

function isValidationFailed(id)
{
	return (validationFailedId != null && validationFailedId != id);
}

function choiceKeyboardHandler(event) {
	var kc = wicketKeyCode(Wicket.fixEvent(event));
 	switch (kc) {
		case 32: // space
			this.click();
			break;
		case 38: // up
		case 40: // down
			var thisDiv = this.parentNode;
			var choiceElement = thisDiv.parentNode;
			var children = choiceElement.getElementsByTagName('div');
			var thisIndex = -1;
			for (var i=0; i<children.length; i++) {
				if (children[i] == thisDiv) {
					thisIndex = i;
					break;
				}
			}
			if (thisIndex != -1) {
				var nextIndex = thisIndex;
				if (kc == 38) nextIndex--;
				else nextIndex++;
				if (nextIndex < 0) nextIndex += children.length;
				if (nextIndex >= children.length) nextIndex -= children.length;
				children[nextIndex].focus();
			}
			break;
	}
	return null;
}

function attachChoiceHandlers(markupid, callbackscript) {
	var choiceElement = document.getElementById(markupid);
	var children = choiceElement.getElementsByTagName('div');
	if (children) {
 		for(var x = 0; x < children.length; x++) {
 			var child = children[x];
 			var inputs = child.getElementsByTagName('input');
 			var inp = inputs[0];
    		Wicket.Event.add(child, 'keydown', choiceKeyboardHandler.bind(inp));
			Wicket.Event.add(inp, 'click', callbackscript);
 		}
	}
}

function fixTabIndexes(markupid) {
	var choiceElement = document.getElementById(markupid);
	var children = choiceElement.getElementsByTagName('input');
	if (children) {
		for( var x = 0; x < children.length; x++ ) {
			children[x].tabIndex = -1;
 		}
	}
}

if (typeof(Servoy.Utils) == "undefined")
{
	Servoy.Utils=
	{
		clickTimer: null,
		clickTimerRunning: false,
		downArrow: null,
		scrollTimer: null,
		doSelectTimer: null,
		
		startClickTimer: function(f)
		{
			if(!Servoy.Utils.clickTimerRunning)
			{
				Servoy.Utils.clickTimerRunning = true;
				Servoy.Utils.clickTimer = setTimeout(f, 200);
			}		
		},
		
		setLabelChildHeight: function(elemid, valign)
		{			
			setTimeout(function(){Servoy.Utils.setLabelChildHeightEx(elemid, valign);},50);
		},
		
		setLabelChildHeightEx: function(elemid, valign) 
		{
		  var elem = document.getElementById(elemid);
		  var child = document.getElementById(elemid + "_lb");
		  var paddTop;
		  var paddBottom;
		  var ua = window.navigator.userAgent
	      var msie = ua.indexOf("MSIE ");	  
		  
		  if (elem)
		  {
	      	  if (msie > 0) // IE
	      	  {
	      		  if (elem.currentStyle)
	      		  {
	      			  paddTop = elem.currentStyle.paddingTop.replace("px","");
	      			  paddBottom = elem.currentStyle.paddingBottom.replace("px","");
	      		  }
	      		  else
	      		  {
	      			  // should never happen
	      			  Wicket.Log.info("currentStyle of element is null: " + elemid);
	      			  paddTop = 0;
	      			  paddBottom = 0;
	      		  }
	      	  }
	      	  else // other browsers 
	      	  {
	      		  paddTop = parseInt(window.getComputedStyle(elem, null).paddingTop.replace("px","")); 
	      		  paddBottom = parseInt(window.getComputedStyle(elem, null).paddingBottom.replace("px","")); 
			  }
		  }
		  if(elem && child)
		  {
			var elemHeight =  elem.clientHeight;
			var childHeight = child.clientHeight;
	
			var top; 
	
			if(valign == 1)			// ISupportTextSetup.TOP
			{
				top = paddTop;
			}
			else if(valign == 3)	// ISupportTextSetup.BOTTOM
			{
				// buttons have special bottom padding set with the element height, for handling rendering issues,
				// so ignore it when setting the top on them
				if(child.parentNode.tagName == 'BUTTON')
				{
					top = elemHeight - childHeight;	
				}
				else
				{
					top = elemHeight - childHeight - paddBottom;
				}
			}
			else					// ISupportTextSetup.CENTER
			{
				top = Math.floor((elemHeight - childHeight)/2);
			}
			
			var ssFound = false;
			if(child.className)
			{
			  var ss = document.styleSheets;
			  var clsName = "." + child.className;
			  
			  for (var i=0; i<ss.length; i++)
			  {
				  if(ss[i].href != null) continue;
				  var rules = ss[i].cssRules || ss[i].rules;
	
				  for (var j=0; j<rules.length; j++)
				  {
					  if (rules[j].selectorText == clsName)
					  {
						  ssFound = true;
						  if(rules[j].style.visibility != 'inherit') rules[j].style.visibility = 'inherit';
						  var vTop = top + "px";
						  if(rules[j].style.top != vTop) rules[j].style.top = vTop;
		                  return ;
					  }
				  }
			  }
			}
			
			child.style.top = top + "px";
			child.style.visibility = 'inherit';			
			if($(child).children().length > 0) $(child).children()[0].style.visibility = "inherit"
		  }		  
		},
		
		appendToInlineStylesheetForIE: function(stylesheetId, newContent)
		{
			  var ss = document.styleSheets;
			  for (var i=0; i<ss.length; i++)
			  {
				  if(ss[i].href != null && ss[i].href != '') continue;
				  if(ss[i].cssText && ss[i].rules.length > 0 && stylesheetId.indexOf(ss[i].rules[0].selectorText) == 0)
				  {
					  ss[i].cssText = ss[i].cssText + newContent;
					  return;
				  }
			  }
			  
			  var newStyle= document.createElement('style');
              newStyle.type= "text/css";
			  if(newStyle.styleSheet)
			  {
				  newStyle.styleSheet.cssText = stylesheetId + ' ' + newContent;
				  document.getElementsByTagName('head')[0].appendChild(newStyle);
			  }
		},
		
		stopClickTimer: function()
		{
			clearTimeout(Servoy.Utils.clickTimer);
			Servoy.Utils.clickTimerRunning = false;
		},
		
		getSelectedText: function(id)
		{
		    var sel = null;
		    var textarea = document.getElementById(id);
		    if(textarea) {
				// code for IE
			    if (document.selection) {
					textarea.focus();
					sel = document.selection.createRange().text;
			    }
			    else {
					// code for Mozilla
					var start = textarea.selectionStart;
					var end = textarea.selectionEnd;
					sel = textarea.value.substring(start, end);
				}
		    }
			return sel;
		},
		
		getSelection: function(oField){
		 // Initialize
	     var begin = 0;
	     var end = 0;
	
	     // IE Support
	     if (document.selection) { 
	
	       // Set focus on the element
	       oField.focus ();
	  
	       // To get cursor position, get empty selection range
	       var oSel = document.selection.createRange ();
	  
	       // Move selection start to 0 position
			begin = 0 - oSel.duplicate().moveStart('character', -100000);
			end = begin + oSel.text.length;
	     }
	
	     // Firefox support
	     else if (oField.selectionStart || oField.selectionStart == '0') {
			begin = oField.selectionStart;
			end = oField.selectionEnd;
		}

	     // Return results
		  return { begin: begin, end: end };
		},
		createSelection: function(field,start, end){
			if( field.createTextRange ) {
			     var selRange = field.createTextRange();
			     selRange.collapse(true);
			     selRange.moveStart('character', start);
			     selRange.moveEnd('character', end);
			     selRange.select();
			     field.focus();
			 } else if( field.setSelectionRange ) {
			     field.focus();
			     field.setSelectionRange(start, end);
			 } else if( typeof field.selectionStart != 'undefined' ) {
			    field.selectionStart = start;
			    field.selectionEnd = end;
			    field.focus();
			 }
		},
	   /*
	    **  Returns the caret (cursor) position of the specified text field.
	    **  Return value range is 0-oField.length.
	    */
	   doGetCaretPosition: function(oField, doNotDuplicateTextRangeOnGet) {
	     // Initialize
	     var iCaretPos = 0;
	
	     // IE Support
	     if (document.selection) { 
	
	       // Set focus on the element
	       oField.focus ();
	  
	       // To get cursor position, get empty selection range
	       var oSel = document.selection.createRange ();
	  
	       // Move selection start to 0 position
	       if(doNotDuplicateTextRangeOnGet)
	    	   oSel.moveStart ('character', -oField.value.length);
	       else
	    	   oSel.duplicate().moveStart ('character', -oField.value.length);
	       // The caret position is selection length
	       iCaretPos = oSel.text.length;
	     }
	
	     // Firefox support
	     else if (oField.selectionStart || oField.selectionStart == '0')
	       iCaretPos = oField.selectionStart;
	     // Return results
	     return (iCaretPos);
	   },
	   
	   /**
	    * replaced the selected text with the given text.
	    */ 
	   replaceSelectedText: function(id,text) {
		    // code for IE
		    var textarea = document.getElementById(id);
		    if (document.selection) {
				textarea.focus();
				var sel = document.selection.createRange();
				// Finally replace the value of the selected text with this new replacement one
				sel.text = text;
		    }
		    else {
			    // code for Mozilla
				var len = textarea.value.length;
				var start = textarea.selectionStart;
				var end = textarea.selectionEnd;
				var sel = textarea.value.substring(start, end);
				var replace = text;
				 
				// Here we are replacing the selected text with this one
				textarea.value = textarea.value.substring(0,start) + replace + textarea.value.substring(end,len);
			 }
		 },
	
	
	   /*
 	    **  Sets the caret (cursor) position of the specified text field.
	    **  Valid positions are 0-oField.length.
	    */
	   doSetCaretPosition: function(oField, iCaretPos) {
	
	     // IE Support
	     if (document.selection) { 
	
	       // Set focus on the element
	       oField.focus ();
	  
	       // Create empty selection range
	       var oSel = document.selection.createRange ();
	  		oSel.collapse(true);
	       // Move selection start and end to 0 position
	       oSel.moveStart ('character', -oField.value.length);
	       oSel.moveEnd ('character', -oField.value.length);
	  
	       // Move selection start and end to desired position
	       oSel.moveStart ('character', iCaretPos);
	       oSel.moveEnd ('character', 0);
	       oSel.select ();
	     }
	
	     // Firefox support
	     else if (oField.selectionStart || oField.selectionStart == '0') {
	       oField.selectionStart = iCaretPos;
	       oField.selectionEnd = iCaretPos;
	       oField.focus ();
	     }
	  },
	  
	  fixMediaLocation: function(elementId,horizontalAlignment){
		var element = document.getElementById(elementId);
		if (element.childNodes.length > 0)
		 	element = element.childNodes[element.childNodes.length-1];
		var imageHeight = element.clientHeight;
		var height = element.parentNode.clientHeight;
		if (height > imageHeight) element.style.top = (height - imageHeight) / 2+'px';
		var imageWidth = element.clientWidth;
		var width = element.parentNode.clientWidth;
		if (width > imageWidth) {
			if (horizontalAlignment == 0 /*SwingConstants.CENTER*/) element.style.left = (width - imageWidth) / 2+'px';
			else if (horizontalAlignment == 4 /*SwingConstants.RIGHT*/) element.style.left = (width - imageWidth)+'px';
			else
			{
				if ((element.parentNode.childNodes > 1) && (imageHeight + 34 < height)) element.style.left ='51px';
			}
		}
		element.style.visibility = 'inherit';
	  },
	  
	  getXY: function(oElement)
	  {
		var iReturnValue = new Array();
		iReturnValue[0] = 0;
		iReturnValue[1] = 0;
		while( oElement != null )
		{
			iReturnValue[0] += oElement.offsetLeft - oElement.scrollLeft + oElement.clientLeft;
			iReturnValue[1] += oElement.offsetTop - oElement.scrollTop + oElement.clientTop;
			oElement = oElement.offsetParent;
		}
		return iReturnValue;
	  },
	  
	  getModifiers: function(e) 
	  {
		if (!e) e = window.event;
		if (!e) return 0;

		var modifiers = 0;
		if(e.ctrlKey)	modifiers += 1
		if(e.shiftKey)	modifiers += 2
		if(e.altKey)	modifiers += 4
		if(e.metaKey)	modifiers += 8

		return modifiers;
	  },  
	  
	  getActionParams: function(e,globalLocation)
	  {
		e = Wicket.fixEvent(e);
	  	var elem;
  		if (e.target)
		{
			elem = e.target;
		}
		else if (e.srcElement)
		{
			elem = e.srcElement;
		}
  		
  		var elXY = Servoy.Utils.getXY(elem);
  		var mx = e.offsetX ? e.offsetX : ((e.pageX ? e.pageX : e.clientX + document.body.scrollLeft + document.documentElement.scrollLeft) - elXY[0]);
  		var my = e.offsetY ? e.offsetY : ((e.pageY ? e.pageY : e.clientY + document.body.scrollTop + document.documentElement.scrollTop) - elXY[1]);
  		var url = '&modifiers='+Servoy.Utils.getModifiers(e)+'&mx=' + mx + '&my=' + my;
  		if (globalLocation)
  		{
  			url += '&glx='+ (e.clientX + document.body.scrollLeft + document.documentElement.scrollLeft) + '&gly=' + (e.clientY + document.body.scrollTop + document.documentElement.scrollTop);
  		}
  		return url;
	  },

	  isChrome : navigator.userAgent.toLowerCase().indexOf('chrome') > -1,
	  
	  isSafari : navigator.userAgent.toLowerCase().indexOf('safari') > -1,
	  
	  isFirefox : navigator.userAgent.toLowerCase().indexOf('firefox') > -1,
	  
	  doSelect: function(el) 
	  {
		  if (validationFailedId == null || validationFailedId == el.id)
		  {
			  if(Servoy.Utils.doSelectTimer) clearTimeout(Servoy.Utils.doSelectTimer);
			  Servoy.Utils.doSelectTimer = setTimeout(function() {
				  if (el === Wicket.Focus.getFocusedElement())
				  {
					  el.select();
				  }
			  }, 200);
		  }
	  },
	  removeFormCssLink: function(id) 
	  {
		  var cssLink = document.getElementById(id);
		  if (cssLink) {
			  setTimeout(function(){
				  var headID = document.getElementsByTagName("head")[0];
				  headID.removeChild(cssLink);
			  },200);
		  }
	  },
	  attachChoiceEvents: function(id) 
	  {
		  var choiceElement = document.getElementById(id);
		  var children = choiceElement.getElementsByTagName('div');
		  if (children) {
		 		for(var x = 0; x < children.length; x++) {
		 			var child = children[x];
		 			var inputs = child.getElementsByTagName('input');
		 			var inp = inputs[0];
					Wicket.Event.add(inp, 'click', function(e){e.target.parentNode.focus();});
		 		}
			}
	  },
	  
	  checkWebFormHeights: function() {
		  $(".webform").each(function( index ) {
			  if ($(this).height()  == 0) {
				  var parentHeight = parseInt($(this).parent().height(), 10);
				  if (parentHeight > 0) {
					  $(this).css("height" , parentHeight - parseInt($(this).css("top"), 10));
				  }
			  }
		  });
	  },
	  
	  testArrowKey : function(e, script,viewID) {
	  	var upArrowPressed = testKeyPressed(e, script,38);
	  	var downArrowPressed = testKeyPressed(e, script,40);
	  	if (!upArrowPressed || !downArrowPressed)
	  	{
	  		// arrow pressed
	  		var focusedElement = Wicket.Focus.getFocusedElement();
			if (typeof(focusedElement) != "undefined" && focusedElement != null && focusedElement.id != viewID)
			{
				focusedElement.blur();
			}
			// if table will be replaced by wicket, make it restore focus to the view so that we can continue traversal
			Wicket.Focus.lastFocusId = viewID;
			requestFocus(viewID);
	  	}
	  	return upArrowPressed && downArrowPressed;
	  },
	  
	  getArrowParams: function(e) {
		return '&downArrow='+(Servoy.Utils.downArrow ? "true" : "false");
	  },
	  
	  getScrollPosition: function(elId) {
		  var jqEl = $('#' + elId);
		  return {x: jqEl.scrollLeft(), y: jqEl.scrollTop()};
	  },
	  
	  setScrollPosition: function(elId, x, y) {
		  var jqEl = $('#' + elId);
		  jqEl.scrollLeft(x);
		  jqEl.scrollTop(y);		  
	  },
	  
	  onScroll: function(elId, callbackUrl) {
    	  if(Servoy.Utils.scrollTimer) clearTimeout(Servoy.Utils.scrollTimer);
    	  Servoy.Utils.scrollTimer = setTimeout(function() {
        	  wicketAjaxGet
        	  (					
        			  callbackUrl+'&locationX='+Servoy.Utils.getScrollPosition(elId).x+'&locationY='+Servoy.Utils.getScrollPosition(elId).y,
        			  null,
        			  function() { onAjaxError(); }.bind($('#' + elId).get(0)),
        			  function() { return Wicket.$(elId) != null; }.bind($('#' + elId).get(0))
        	  );
    	  }, 500);
	  },
	  
	  nop: function(){}
	}
}


if (typeof(Servoy.Validation) == "undefined")
{
	Servoy.Validation=
	{
		displayEditFormOnFocus: new Array(),
		displayEditFormOnBlur: new Array(),
	
		imposeMaxLength: function(obj, mlength)
		{
			if (obj.getAttribute && obj.value.length>mlength)
				obj.value=obj.value.substring(0,mlength)
		},
		
		numbersonly: function(e, decimal, decimalChar, groupingChar, currencyChar, percentChar,obj,mlength) 
		{
			var key;
			var keychar;
			
			if (window.event) {
			   key = window.event.keyCode;
			}
			else if (e) {
			   key = e.which;
			}
			else {
			   return true;
			}
			
			if ((key==null) || (key==0) || (key==8) ||  (key==9) || (key==13) || (key==27) 
					|| (e.ctrlKey && key==97) || (e.ctrlKey && key==99) || (e.ctrlKey && key==118) || (e.ctrlKey && key==120)) { //added CTRL-A, X, C and V
			   return true;
			}

			keychar = String.fromCharCode(key);

			if (mlength > 0 && obj && obj.value)
			{
				var counter = 0;
				if (("0123456789").indexOf(keychar) != -1) counter++;
				var stringLength = obj.value.length;
				for(var i=0;i<stringLength;i++)
				{
				   if (("0123456789").indexOf(obj.value.charAt(i)) != -1) counter++;
				}
				var selectedTxt = Servoy.Utils.getSelectedText(obj.id);
				if(selectedTxt) counter = counter - selectedTxt.length; 
				if (counter > mlength) return false;
			}
			
			if ((("-0123456789").indexOf(keychar) > -1)) {
			   return true;
			}
			else if (decimal && (keychar == decimalChar)) { 
			  return true;
			}
			else if (keychar == groupingChar) { 
			  return true;
			}
			else if (keychar == currencyChar) { 
			  return true;
			}
			else if (keychar == percentChar) { 
			  return true;
			}
			return false;
		},
		
		attachDisplayEditFormat: function(elementId, displayValue,editValue)
		{
			var element = document.getElementById(elementId);
			element.displayValue = displayValue;
			element.editValue = editValue;
			
			Servoy.Validation.displayEditFormOnFocus[elementId] = function(e)
			{
				if (this.editValue != '')
				{
					var caret = Servoy.Utils.doGetCaretPosition(this);
					var pos = Servoy.Utils.getSelection(this);
					var selectAll = null;
					if (pos && pos.begin != pos.end && pos.begin == 0 && pos.end == this.value.length)
					{
						selectAll = true;
					}
					this.value = this.editValue;
					if (selectAll)
					{
						Servoy.Utils.doSelect(this);
					}
					else
					{
						Servoy.Utils.doSetCaretPosition(this,caret);
					}
					if (this.displayValue == '')
					{
						setTimeout(function(){
							// we have null value and display format, select it so that it is erased when editing
							// in some browsers doesn't work directly onfocus, execute with settimeout 
							if(element.setSelectionRange){
								element.setSelectionRange(0, 9999);
							}else{
								element.select();
							}							
						 },0);
					}
				}
			};
			Servoy.Validation.displayEditFormOnBlur[elementId] = function(e)
			{
				if (this.editValue != '' && this.value == this.editValue)
				{
					this.value = this.displayValue;
				}
			};
			Wicket.Event.add(element, "focus", Servoy.Validation.displayEditFormOnFocus[elementId]);
			Wicket.Event.add(element, "blur", Servoy.Validation.displayEditFormOnBlur[elementId]);
		},
		
		detachDisplayEditFormat: function(elementId)
		{
			if(Servoy.Validation.displayEditFormOnFocus[elementId] || Servoy.Validation.displayEditFormOnBlur[elementId])
			{
				var element = document.getElementById(elementId);
	
				if(element)
				{
					if(Servoy.Validation.displayEditFormOnFocus[elementId])
					{
						if(element.removeEventListener)
						{
							element.removeEventListener('focus', Servoy.Validation.displayEditFormOnFocus[elementId], false);
						}
						else if(element.detachEvent)
						{
							element.detachEvent('onfocus', Servoy.Validation.displayEditFormOnFocus[elementId]);
						}
						Servoy.Validation.displayEditFormOnFocus[elementId] = null;
					}
		
					if(Servoy.Validation.displayEditFormOnBlur[elementId])
					{
						if(element.removeEventListener)
						{
							element.removeEventListener('blur', Servoy.Validation.displayEditFormOnBlur[elementId], false);
						}
						else if (element.detachEvent)
						{
							element.detachEvent('onblur', Servoy.Validation.displayEditFormOnBlur[elementId]);
						}
						Servoy.Validation.displayEditFormOnBlur[elementId] = null;
					}
				}
			}
		},
		
		changeCase: function(element,e,upper, maxLength)
		{
			e = e || window.event;
			var k = e.keyCode || e.charCode || e.which;
			if (e.ctrlKey || e.altKey || e.metaKey) {//Ignore
				return true;
			} else if ((k >= 65 && k <= 122) || k > 186) {//letter characters
			
				var c = String.fromCharCode(k);
				var buffer = element.value.split("");
				var pos = Servoy.Utils.getSelection(element);
				if (pos.begin != pos.end)
				{
					buffer.splice(pos.begin,pos.end-pos.begin);
				}
				var caret = pos.begin;
				if (caret != buffer.length)
				{
					buffer.splice(caret,0,upper?c.toUpperCase():c.toLowerCase());
				}
				else
				{
					buffer[caret] = upper?c.toUpperCase():c.toLowerCase();
				}
				var value = buffer.join('');
				if (!maxLength || value.length <= maxLength) {
					element.value = buffer.join('');
					Servoy.Utils.doSetCaretPosition(element,caret+1);
				}
				return false;
			}
		},
		
		pasteHandler: function(e, f)
		{
			var cp = e.value.length - Servoy.Utils.doGetCaretPosition(e);
			setTimeout(function(){f(e);Servoy.Utils.doSetCaretPosition(e,e.value.length-cp)}, 50);			
		}
	};
}

if (typeof(Servoy.Resize) == "undefined")
{
	Servoy.Resize=
	{
		resizeTimer : null,
		callback : null,
		orientationCallback : null,
		
		onOrientationChange: function ()
		{
			if(Servoy.Resize.orientationCallback)
			{
				var ajaxCall = Servoy.Resize.orientationCallback;
				ajaxCall = ajaxCall + "&orientation=" + window.orientation
				ajaxCall = "wicketAjaxGet('" + ajaxCall + "')";
				setTimeout(ajaxCall,0);
			}
		},
		
		onWindowResize: function ()
		{
			if (Servoy.Resize.resizeTimer) 
				clearTimeout(Servoy.Resize.resizeTimer);

			if(Servoy.Resize.callback)
			{
				var ajaxCall = Servoy.Resize.callback;
				var divs = document.getElementsByTagName("div");
				for (var i=0;i<divs.length;i++)
				{
					if (divs[i].id)
					{
						if (divs[i].id.match("^sfh_") == "sfh_")
						{
							ajaxCall = ajaxCall + "&" + divs[i].id + "="+divs[i].offsetHeight
						}
						else if (divs[i].id.match("^sfw_") == "sfw_")
						{
							ajaxCall = ajaxCall + "&" + divs[i].id + "="+divs[i].offsetWidth
						}
					} 
				}
				ajaxCall = "wicketAjaxGet('" + ajaxCall + "')";
				Servoy.Resize.resizeTimer = setTimeout(ajaxCall,300);
			}
			else
			{
				// shouldn't happen, seems callback was not set yet, wait a bit
				Wicket.Log.info("onResize called when callback is null");
				Servoy.Resize.resizeTimer = setTimeout(Servoy.Resize.onWindowResize,300);
			}
		}
	}
}
if (typeof(Servoy.Rollover) == "undefined")
{
	Servoy.Rollover=
	{
		imgUrl : null,
		
		onMouseOver: function (elementId,imageUrl)
		{
			var el = document.getElementById(elementId);
			if (el)
			{
				imgUrl = el.src;
				Servoy.Rollover.setImageSrc(el, imageUrl);
			}
		},
		
		onMouseOut: function (elementId)
		{
			var el = document.getElementById(elementId);
			if (el)
			{
				Servoy.Rollover.setImageSrc(el, imgUrl);
			}
		},
		
		setImageSrc: function (el, imgURL)
		{
			if(imgURL)
			{
				// get w= and h= from el
				// we need to make sure width/height are from displayed element, see anchorlayout.js (there width/height is changed)
				var currentSrc = el.src;
				var w = "", h = "";
				var i;
				if((i = currentSrc.indexOf("w=")) != -1)
				{
					while(i < currentSrc.length && currentSrc[i] != '&') w = w + currentSrc[i++];
					imgURL = imgURL.replace(/w=[\d]+/, w);
				}
				
				if((i = currentSrc.indexOf("h=")) != -1)
				{
					while(i < currentSrc.length && currentSrc[i] != '&') h = h + currentSrc[i++];
					imgURL = imgURL.replace(/h=[\d]+/, h);
				}
			}

			el.src = imgURL;
		}
	}
}

if (typeof(Servoy.ClientDesign) == "undefined")
{
	Servoy.ClientDesign = 
	{
		selectedResizeElement : new Array(),
		selectedElementId : new Array(),
		selectedElementPosition : new Array(),
		designableElementsArray : null,
		callbackurl : null,
		mouseSelectTime : new Date(),
		mouseDownEvent : null,
		mouseX : 0,
		mouseY : 0,
		clickTimer : null,
		clickCount : 0,
		clickEvent : null,
		clickElement : null,
		
		mouseSelect: function(e)
		{
			Servoy.ClientDesign.mouseDownEvent = e;
			Servoy.ClientDesign.clickCount++;
			if(!Servoy.ClientDesign.clickTimer)
			{
				Servoy.ClientDesign.clickEvent = e;
				Servoy.ClientDesign.clickTimer = window.setTimeout(function()
				{
					Servoy.ClientDesign.selectElement(Servoy.ClientDesign.clickEvent, Servoy.ClientDesign.clickCount > 1);
				}, 200);
			}
		},
		
		mouseUp: function(e)
		{
			Servoy.ClientDesign.mouseDownEvent = null;
		},

		clearClickTimer: function()
		{
			Servoy.ClientDesign.clickTimer = null;
			Servoy.ClientDesign.clickCount = 0;
			Servoy.ClientDesign.clickEvent = null;
			Servoy.ClientDesign.clickElement = null;
		},
		
		selectElement: function(e, isDblClick)
		{
			var elem;
			if (!e)
			{
				e = window.event;
			}
			
			var isRightClick = e.button && e.button == 2;
			
			if (e.target)
			{
				elem = e.target;
			}
			else if (e.srcElement)
			{
				elem = e.srcElement;
			}
			
			if (elem.nodeType == 3) // defeat Safari bug
			{
				elem = elem.parentNode;
			}
			
			// get the enclosing button
			if(elem.nodeName == 'SPAN' && elem.parentNode && elem.parentNode.nodeName == "BUTTON")
			{
				elem = elem.parentNode;
			}
			
			//get enclosing wrapper to work on
			if (elem.id && elem.parentNode && elem.parentNode.id && elem.parentNode.id.indexOf('_wrapper')>0)
			{
				elem = elem.parentNode;
			}
			
			// get the parent label for label span & img tags
			if(elem.id && (elem.id.indexOf('_lb')>0 || elem.id.indexOf('_img')>0))
			{
				var lbElem = elem;
				while(lbElem && lbElem.className != 'label')
				{
					lbElem = lbElem.parentNode;
				}
				if(lbElem && lbElem.className == 'label') elem = lbElem;
			}
			
			// get enclosing div for composite field
			if(elem.id && elem.id.indexOf('compositefield')>0)
			{
				var dateElem = elem;
				while(dateElem && dateElem.className != 'field')
				{
					dateElem = dateElem.parentNode;
				}
				if(dateElem && dateElem.className == 'field') elem = dateElem;			
			}
			
			// get enclosing div for date checkbox
			if(elem.id && (elem.id.indexOf('check_')>-1 || elem.id.indexOf('text_')>-1))
			{
				var checkElem = elem;
				while(checkElem && checkElem.className != 'check')
				{
					checkElem = checkElem.parentNode;
				}
				if(checkElem && checkElem.className == 'check') elem = checkElem;			
			}
			
			// get tabpanel of form
			if(elem.className == 'formpart')
			{
				var tabpanelElem = elem;
				while(tabpanelElem && tabpanelElem.className != 'tabpanel')
				{
					tabpanelElem = tabpanelElem.parentNode;
				}
				if(tabpanelElem && tabpanelElem.className == 'tabpanel') elem = tabpanelElem;
			}

			if (!elem.id)
			{
				elem = elem.parentNode;
			}
			if (Servoy.ClientDesign.selectedResizeElement.length > 0)
			{
				//deselect old yui elements
				Servoy.ClientDesign.destroyResizeElements();
			}
			
			if (elem.id)
			{
				wicketAjaxGet(Servoy.ClientDesign.callbackurl+'&a=aSelect&xc=' + elem.style.left + '&yc=' + elem.style.top + '&draggableID=' + elem.id + '&isDblClick=' + isDblClick + '&isRightClick=' + isRightClick+ '&isCtrlKey=' + e.ctrlKey);
			}
			else Servoy.ClientDesign.clearClickTimer();
		},
		
		destroyResizeElements: function()
		{
			for(var i = 0; i < Servoy.ClientDesign.selectedResizeElement.length; i++)
				Servoy.ClientDesign.selectedResizeElement[i].destroy();
			Servoy.ClientDesign.selectedResizeElement.length = 0;
		},
		
		topBottomSwitchEl : null,
		leftRightSwitchEl : null,

		attachElement: function(elem)
		{
			if(Servoy.ClientDesign.topBottomSwitchEl && Servoy.ClientDesign.topBottomSwitchEl.offsetParent)
			{
				var offsetBottom = Servoy.ClientDesign.topBottomSwitchEl.offsetParent.offsetHeight - Servoy.ClientDesign.topBottomSwitchEl.offsetTop - Servoy.ClientDesign.topBottomSwitchEl.offsetHeight;
				YAHOO.util.Dom.setStyle(Servoy.ClientDesign.topBottomSwitchEl, "top", "");
				YAHOO.util.Dom.setStyle(Servoy.ClientDesign.topBottomSwitchEl, "bottom", offsetBottom + "px");
			}
			
			if(Servoy.ClientDesign.leftRightSwitchEl && Servoy.ClientDesign.leftRightSwitchEl.offsetParent)
			{
				var offsetRight = Servoy.ClientDesign.leftRightSwitchEl.offsetParent.offsetWidth - Servoy.ClientDesign.leftRightSwitchEl.offsetLeft - Servoy.ClientDesign.leftRightSwitchEl.offsetWidth;
				YAHOO.util.Dom.setStyle(Servoy.ClientDesign.leftRightSwitchEl, "left", "");
				YAHOO.util.Dom.setStyle(Servoy.ClientDesign.leftRightSwitchEl, "right", offsetRight + "px");			
			}
			
			var elementDescription = Servoy.ClientDesign.designableElementsArray[elem.id];
			if (elementDescription)
			{
				var topStyle = YAHOO.util.Dom.getStyle(elem, "top");
				var bottomStyle = YAHOO.util.Dom.getStyle(elem, "bottom");
				var leftStyle = YAHOO.util.Dom.getStyle(elem, "left");
				var rightStyle = YAHOO.util.Dom.getStyle(elem, "right");

				if((!topStyle || topStyle.indexOf("px") == -1) && bottomStyle)
				{
					Servoy.ClientDesign.topBottomSwitchEl = elem;
					YAHOO.util.Dom.setStyle(elem, "top", elem.offsetTop + "px");
				}
				if((!leftStyle || leftStyle.indexOf("px") == -1) && rightStyle)
				{
					Servoy.ClientDesign.leftRightSwitchEl = elem;
					YAHOO.util.Dom.setStyle(elem, "left", elem.offsetLeft + "px");
				}
				
				//apply YUI resize on elem
				var resize = new YAHOO.util.Resize(elem,
				{
					handles: (elementDescription[1] ? elementDescription[1] : 'all'),
					knobHandles: true,
					wrapPadding: elementDescription[0],
					proxy: true,
					wrap: true,
					draggable: true,
					animate: false
				});

				// adjust the wrapper width/heigh
				var wrappingEl = elem;
				var enclosedElements = wrappingEl.getElementsByTagName('input');
				if(enclosedElements.length > 0 && enclosedElements[0].getAttribute("type") == "text") wrappingEl = enclosedElements[0];
				enclosedElements = wrappingEl.getElementsByTagName('textarea');
				if(enclosedElements.length > 0) wrappingEl = enclosedElements[0];
				var wrapEl = resize.getWrapEl();
				YAHOO.util.Dom.setStyle(wrapEl, 'width', wrappingEl.offsetWidth + "px");
				YAHOO.util.Dom.setStyle(wrapEl, 'height', wrappingEl.offsetHeight + "px");
				
				// set the focus on the form, so if any key event if fired, the target will
				// be the parent form - this is needed for shortcuts to work correctly
				var form = YAHOO.util.Dom.getAncestorBy ( wrapEl , function(el)
				{
					return el.className && el.className == 'servoywebform';
				});
				if(form) form.focus();

				resize.dd.endDrag = function(args) 
				{ 
					Servoy.DD.dragStopped(); 
					return true; 
				};
				
				resize.dd.startDrag = function(x,y)	
				{
					var element = document.getElementById(this.id);
					this.setYConstraint(element.offsetTop,element.offsetParent.scrollHeight-element.offsetTop-element.offsetHeight);
					this.setXConstraint(element.offsetLeft,element.offsetParent.scrollWidth-element.offsetLeft-element.offsetWidth);
					
					Servoy.ClientDesign.selectedElementPosition.length = 0;
					var wrapEl, top, left;
					for(var i = 0; i < Servoy.ClientDesign.selectedResizeElement.length; i++)
					{
						wrapEl = Servoy.ClientDesign.selectedResizeElement[i].getWrapEl();
						top = YAHOO.util.Dom.getStyle(wrapEl, "top");
						top = parseInt(top.replace("px","")); 
						left = YAHOO.util.Dom.getStyle(wrapEl, "left");
						left = parseInt(left.replace("px",""));
						Servoy.ClientDesign.selectedElementPosition[i] = new Array(top, left);
					}
					wicketAjaxGet(Servoy.ClientDesign.callbackurl+'&a=aStart&xc=' + element.style.left + '&yc=' + element.style.top + '&draggableID=' + this.id);
					Servoy.DD.dragStarted();
				};
				
				resize.dd.onDrag = function(ev)
				{
					var offsetX = ev.clientX - Servoy.ClientDesign.mouseX;
					var offsetY = ev.clientY - Servoy.ClientDesign.mouseY;
					var resizeEl, newTop, newLeft;
					for(var i = 0; i < Servoy.ClientDesign.selectedResizeElement.length; i++)
					{
						resizeEl = Servoy.ClientDesign.selectedResizeElement[i].getWrapEl();
						if(resizeEl.id != this.id)
						{
							YAHOO.util.Dom.setStyle(resizeEl, "top", Servoy.ClientDesign.selectedElementPosition[i][0] + offsetY + "px");
							YAHOO.util.Dom.setStyle(resizeEl, "left", Servoy.ClientDesign.selectedElementPosition[i][1] + offsetX + "px");
						}
					}
				};
				
				resize.dd.on('mouseDownEvent', function(ev, targetid)
				{
					if(!Servoy.ClientDesign.clickTimer)
					{
						Servoy.ClientDesign.clickEvent = ev;
						Servoy.ClientDesign.mouseX = ev.clientX;
						Servoy.ClientDesign.mouseY = ev.clientY;
					}
				});
				
				resize.dd.on('mouseUpEvent', function(ev, targetid)	
				{
					Servoy.ClientDesign.clickCount++;
					if(!Servoy.ClientDesign.clickTimer)
					{
						Servoy.ClientDesign.clickElement = this;
						Servoy.ClientDesign.clickTimer = window.setTimeout(function()
						{		
							var element = document.getElementById(Servoy.ClientDesign.clickElement.id);
							var urlCommonParams = 'xc=' + Servoy.addPositions(element.offsetParent.style.left, element.style.left) + '&yc=' + Servoy.addPositions(element.offsetParent.style.top,element.style.top) + '&draggableID=' + Servoy.ClientDesign.clickElement.id;
							var url;
							if(Servoy.DD.isDragStarted)
							{
								url = Servoy.ClientDesign.callbackurl+'&a=aDrop&' + urlCommonParams  + '&targetID=' + targetid;
								Servoy.ClientDesign.destroyResizeElements();
								Servoy.DD.isDragStarted = false;
							}
							else
							{
								if (Servoy.ClientDesign.selectedResizeElement.length > 0)
								{
									//deselect old yui elements
									Servoy.ClientDesign.destroyResizeElements();
								}
								var isRightClick = Servoy.ClientDesign.clickEvent.button && Servoy.ClientDesign.clickEvent.button == 2;
								url = Servoy.ClientDesign.callbackurl+'&a=aSelect&' + urlCommonParams + '&isDblClick=' + (Servoy.ClientDesign.clickCount > 1) + '&isRightClick=' + isRightClick+ '&isCtrlKey=' + Servoy.ClientDesign.clickEvent.ctrlKey; 								
							}
							wicketAjaxGet(url);
						}, 200);
					}
				});

				resize.on('beforeResize', function(args) 
				{
					if (Servoy.ClientDesign.selectedResizeElement.length > 1)
					{
						return false;
					}
					return true;
				});
				
				resize.on('endResize', function(args) 
				{
					var url = Servoy.ClientDesign.callbackurl+'&a=aResize&draggableID=' + this._wrap.id + '&resizeHeight=' + args.height + '&resizeWidth=' + args.width + '&xc=' + this._wrap.style.left + '&yc=' + this._wrap.style.top;
					Servoy.ClientDesign.destroyResizeElements();
					wicketAjaxGet(url);
				});
				Servoy.ClientDesign.selectedElementId.push(elem.id);
				Servoy.ClientDesign.selectedResizeElement.push(resize);
				
				if(Servoy.ClientDesign.mouseDownEvent)
				{
					Servoy.ClientDesign.mouseX = Servoy.ClientDesign.mouseDownEvent.clientX;
					Servoy.ClientDesign.mouseY = Servoy.ClientDesign.mouseDownEvent.clientY;
					resize.dd.handleMouseDown(Servoy.ClientDesign.mouseDownEvent, resize.dd);
				}
			}
			else
			{
				Servoy.ClientDesign.topBottomSwitchEl = null;
				Servoy.ClientDesign.leftRightSwitchEl = null;
			}
		},
		
		attach: function (array,url)
		{
			Servoy.ClientDesign.designableElementsArray = array;
			var el, enclosedEl;
			for(var elID in Servoy.ClientDesign.designableElementsArray)
			{
				el = document.getElementById(elID);
				Servoy.ClientDesign.addDragCursor(el);
				Servoy.ClientDesign.addDragCursor(el.getElementsByTagName('input'));
				Servoy.ClientDesign.addDragCursor(el.getElementsByTagName('textarea'));
				Servoy.ClientDesign.addDragCursor(el.getElementsByTagName('button'));
			}
			Servoy.ClientDesign.callbackurl = url;
			Servoy.ClientDesign.clearClickTimer();
			Wicket.Event.add(document.body, "mousedown", Servoy.ClientDesign.mouseSelect);
			Wicket.Event.add(document.body, "mouseup", Servoy.ClientDesign.mouseUp);
			document.body.oncontextmenu = function(e) { e.preventDefault();}
			var Dom = YAHOO.util.Dom,Event = YAHOO.util.Event; //to load stuff?
		},
		
		addDragCursor: function(el)
		{
			if(el.length)
			{
				for(var i = 0; i < el.length; i++) Servoy.ClientDesign.addDragCursor(el[i]);
			}
			else
			{
				YAHOO.util.Dom.addClass(el, 'yui-draggable');
			}
		},
		
		attachElements: function(elements)
		{
		   Servoy.ClientDesign.selectedElementId.length = 0;
		   for(var i = 0; i < elements.length; i++) Servoy.ClientDesign.attachElement(document.getElementById(elements[i]));
		},
		
		reattach: function()
		{
			window.setTimeout(function()
			{
			   Servoy.ClientDesign.attachElements(Servoy.ClientDesign.selectedElementId.slice());
			   Servoy.ClientDesign.clearClickTimer();
			}
			,0);
		},
		
		hideSelected: function(elemId)
		{
			if(Servoy.ClientDesign.isSelected(elemId) && Servoy.ClientDesign.selectedResizeElement.length > 0)
			{
				Servoy.ClientDesign.destroyResizeElements();
				Servoy.ClientDesign.selectedElementId.length = 0;
				
				YAHOO.util.Dom.setStyle(YAHOO.util.Dom.get(elemId), 'display', 'none');
			}
		},
		
		refreshSelected: function(elemId)
		{
			if (Servoy.ClientDesign.selectedResizeElement.length > 0 && Servoy.ClientDesign.isSelected(elemId))
			{
				var updatedEl = document.getElementById(elemId);
				Servoy.ClientDesign.destroyResizeElements();
				
				var oldEl = document.getElementById(elemId);
				oldEl.parentNode.replaceChild(updatedEl, oldEl);

				Servoy.ClientDesign.reattach();
			}
		},
		
		isSelected: function(elemId)
		{
			var isSelected = false;
			for(var i = 0; i < Servoy.ClientDesign.selectedElementId.length; i++)
			{
				if(Servoy.ClientDesign.selectedElementId[i] == elemId)
				{
					isSelected = true;
					break;
				}
			}		
			return isSelected;
		}
	};
}	

if (typeof(Servoy.HTMLEdit) == "undefined")
{
	Servoy.HTMLEdit = 
	{
		scrollTimer : null,
		ServoyTinyMCESettings : {
			menubar : false,
			statusbar : false,
			plugins: 'tabindex resizetocontainer',
			tabindex: 'element',
			toolbar: 'fontselect fontsizeselect | bold italic underline | superscript subscript | undo redo |alignleft aligncenter alignright alignjustify | styleselect | outdent indent bullist numlist'
		},
		
		attach: function (wrapperId,editorId,editable,configuration,defaultConfiguration,scrollX,scrollY,scrollCallback)
		{
			Servoy.HTMLEdit.ServoyTinyMCESettings.selector = '#'+editorId;
			Servoy.HTMLEdit.ServoyTinyMCESettings.readonly = editable ? false : true;
			Servoy.HTMLEdit.ServoyTinyMCESettings.setup = function(editor) {
				var isIE11 = !!navigator.userAgent.match(/Trident.*rv\:11\./);
				if (isIE11)
				{
					// big hack for ie11 editor not being able to edit
					editor.on('focus', function(e) {
						editor.execCommand('SelectAll',false)
					});
				}
				editor.on('blur', function(e) {
					var textarea = document.getElementById(editorId);
					textarea.value = editor.getContent();
					if (typeof textarea.onsubmit == 'function')
					{
						textarea.onsubmit();
					}
					else
					{
						// strange ie8 issue
						eval(textarea.onsubmit)
					}
				});
				editor.on('init', function() {
			          $(editor.getWin()).scrollLeft(scrollX);
			          $(editor.getWin()).scrollTop(scrollY);
			          $(editor.getWin()).scroll(function(){
			        	  if(Servoy.HTMLEdit.scrollTimer) clearTimeout(Servoy.HTMLEdit.scrollTimer);
			        	  Servoy.HTMLEdit.scrollTimer = setTimeout(function() {
				        	  wicketAjaxGet
				        	  (					
				        			  scrollCallback+'&locationX='+$(editor.getWin()).scrollLeft()+'&locationY='+$(editor.getWin()).scrollTop(),
				        			  null,
				        			  function() { onAjaxError(); }.bind($('#' + wrapperId).get(0)),
				        			  function() { return Wicket.$(wrapperId) != null; }.bind($('#' + wrapperId).get(0))
				        	  );
			        	  }, 500);
			          });
				});
			};
			if (defaultConfiguration)
			{
				for (var key in defaultConfiguration)
				{
					if (defaultConfiguration.hasOwnProperty(key))
					{
						Servoy.HTMLEdit.ServoyTinyMCESettings[key] = defaultConfiguration[key];
					}
				}
			}
			if (configuration)
			{
				for (var key in configuration)
				{
					if (configuration.hasOwnProperty(key))
					{
						Servoy.HTMLEdit.ServoyTinyMCESettings[key] = configuration[key];
					}
				}
			}
			tinymce.init(Servoy.HTMLEdit.ServoyTinyMCESettings);
		},
		
		removeInvalidEditors: function()
		{
			for (edId in tinymce.editors)
			{
				var valid = false;
				if (arguments && arguments.length >0)
				{
					for (var i=0;i<arguments.length;i++)
					{
						if(arguments[i] == edId)
						{
							valid = true;
							break;
						}
					}
				}
				if (!valid)
				{
					tinymce.remove('#'+edId);
				}
			}
		}
	};
}

if (typeof(Servoy.TabCycleHandling) == "undefined")
{
	Servoy.TabCycleHandling = 
	{
		maxTabIndexElemId : null,
		minTabIndexElemId : null,
		elementsArray : null,
	
		registerListeners: function (elemIdMinTabIndex, elemIdMaxTabIndex)
		{
			Servoy.TabCycleHandling.minTabIndexElemId = elemIdMinTabIndex;
			var elem = document.getElementById(elemIdMinTabIndex);
			if (typeof(elem) != "undefined" && elem != null)
			{
				Wicket.Event.add(elem,"keydown",Servoy.TabCycleHandling.tabRewindHandler);
			}
			
			var dummyElem = document.createElement("div");
			dummyElem.innerHTML='<a href="javascript: void(0)"></a>';
			document.body.appendChild(dummyElem);
			
			Servoy.TabCycleHandling.maxTabIndexElemId = elemIdMaxTabIndex;
			elem = document.getElementById(elemIdMaxTabIndex);
			if (typeof(elem) != "undefined" && elem != null)
			{
				Wicket.Event.add(elem,"keydown",Servoy.TabCycleHandling.tabForwardHandler);
			}
		},
		
		tabForwardHandler: function (event)
		{
			event = Wicket.fixEvent(event);
			if (event.shiftKey == false && event.keyCode == 9)
			{	
				window.setTimeout(function() {
					requestFocus(Servoy.TabCycleHandling.minTabIndexElemId);
				},1);
				return true;
			}
			return false;
		},
	
		tabRewindHandler: function (event)
		{
			event = Wicket.fixEvent(event);
			if (event.shiftKey && event.keyCode == 9)
			{
				window.setTimeout(function() {
					requestFocus(Servoy.TabCycleHandling.maxTabIndexElemId);
				},1);
				return true;
			}
			return false;
		},
		
		setNewTabIndexes: function (indexesMap)
		{
			for (var i=0; i < indexesMap.length; i++)
			{
				var newIndex = indexesMap[i];
				var element = document.getElementById(newIndex[0]);
				if (element)
				{
					element.tabIndex = newIndex[1];
				}
			}
		},
		
		forceTabbingSequence: function (indexEntries)
		{
			var dummyElem = document.createElement("div");
			dummyElem.innerHTML='<a href="javascript: void(0)"></a>';
			document.body.appendChild(dummyElem);
			
			Servoy.TabCycleHandling.elementsArray = new Array();
			for (var i=0; i < indexEntries.length; i++)
			{
				var element = document.getElementById(indexEntries[i]);
				if (element)
				{
					Servoy.TabCycleHandling.elementsArray[i] = indexEntries[i];
					Wicket.Event.add(element,"keydown",Servoy.TabCycleHandling.nextElementTabHandler);
				}
			}
		},
		
		nextElementTabHandler: function (event)
		{
			event = Wicket.fixEvent(event);
			if (event.keyCode == 9)
			{
				var elem = null;
				if (event.srcElement) elem = event.srcElement;
				else if (event.target) elem = event.target;

				if (elem)
				{
					var nextIndex = -1;
					for (var i=0; i < Servoy.TabCycleHandling.elementsArray.length; i++)
					{
						var aux = document.getElementById(Servoy.TabCycleHandling.elementsArray[i]);
						if (elem == aux)
						{
							if (event.shiftKey == false)
							{
								if (i == Servoy.TabCycleHandling.elementsArray.length-1) nextIndex = 0;
								else nextIndex = i+1;
							}
							else
							{
								if (i == 0) nextIndex = Servoy.TabCycleHandling.elementsArray.length-1;
								else nextIndex = i-1;
							}
							break;
						}
					}

					if (nextIndex != -1)
					{
						window.setTimeout(function() {
							requestFocus(Servoy.TabCycleHandling.elementsArray[nextIndex]);
						},1);
						return true;
					}
				}
			}
			return false;
		}
	};
}
