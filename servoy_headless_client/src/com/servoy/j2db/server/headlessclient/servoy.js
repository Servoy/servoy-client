if (typeof(Servoy) == "undefined")
	Servoy = { };

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
	}
	else if (document.layers) {
		x = window.screenX + 100;
		y = window.screenY + 100;
	}else {// firefox, need to switch the x and y?
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
	}
	else if (document.layers) {
		x = window.screenX + 100;
		y = window.screenY + 100;
	}else {// firefox, need to switch the x and y?
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
			} catch (ignore) {
			}
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
			} catch (ignore) {
			}
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
function storeValueBeforeUpdate()
{
	focusedElement = Wicket.Focus.getFocusedElement();
	if (typeof(focusedElement) != "undefined" && focusedElement != null
	 && focusedElement.type != "button" && focusedElement.type != "submit")
	{
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

function restoreValueAfterUpdate()
{
	var element = Wicket.Focus.getFocusedElement();
	if (focusedElement != null && element != null  
		&& element.id == focusedElement.id 
		&& typeof(element) != "undefined"
		&& element.value != focusedValue)
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
	focusedElement = null;
}

function testEnterKey(e, script) 
{
     var code;
     
     if (!e) e = window.event;
     if (!e) return true;
     if (e.keyCode) code = e.keyCode;
     else if (e.which) code = e.which;
     
     if(code==13)
     {
        if (script) script();
	    return false;
     }
     return true;
}

function filterBackKey(e) 
{
     var code;
     
     if (!e) e = window.event;
     if (!e) return true;
     if (e.keyCode) code = e.keyCode;
     else if (e.which) code = e.which;
     
     if(code==8)
     {
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
		
		for (var i=0; i < tabPanel.childNodes.length; i++)
		{
			var tabHolder = tabPanel.childNodes[i];
			if ((tabHolder.nodeType == 1) && (tabHolder.nodeName.toLowerCase() == "div"))
		   	{
	   			if (tabHolder.getAttribute("tabholder") == "true")
	   			{
					if (!rowMargin) rowMargin = getStyleNumeric(tabHolder, 'paddingLeft') + getStyleNumeric(tabHolder, 'paddingRight');
					originalTabHolders.push(tabHolder);
					var tabs = tabHolder.getElementsByTagName("div");
					for (var j=tabs.length-1; j>=0; j--)
					{
						var tab = tabs[j];
						var thisAnch = tab.getElementsByTagName("a")[0];
						if (thisAnch.getAttribute("firsttab") == "true")
							markedFirstTab = tab;
						allTabsTogether.unshift(tab);
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
		tabPanel.style.visibility = 'visible';
	}
}

var onFocusModifiers = 0;
function addListeners(strEvent, callbackUrl, ids, post)
{
	if (ids)
	{
		for (i in ids)
		{
			var el = document.getElementById(ids[i])
			var callback
			if (post)
			{
				callback = function(e)
				{
					if(!e) e = window.event;
					var modifiers;
					if(strEvent == "focus")
					{
						modifiers = onFocusModifiers;
						onFocusModifiers = 0;
					}
					else
					{
						modifiers = getModifiers(e);
					}
					// if it has display/editvalues then test if the current value is the displayValue. if so only a get instead of a post. 
					if (Wicket.$(this.id).displayValue && Wicket.$(this.id).value == Wicket.$(this.id).displayValue)
					{
						var wcall=wicketAjaxGet
						(
							callbackUrl+'&nopostdata=true&event='+strEvent+'&id='+this.id+'&modifiers='+modifiers,
							null,
							function() { onAjaxError(); }.bind(this),
							function() { onAjaxCall(); return Wicket.$(this.id) != null; }.bind(this)
						);
						return !wcall;
					}
					var wcall=wicketAjaxPost
					(
						callbackUrl+'&event='+strEvent+'&id='+this.id+'&modifiers='+modifiers,
						wicketSerialize(Wicket.$(this.id)),
						null,
						function() { onAjaxError(); }.bind(this),
						function() { onAjaxCall(); return Wicket.$(this.id) != null; }.bind(this)
					);
					return !wcall;
				}
			}
			else
			{
				callback = function(e)
				{
					if(!e) e = window.event;
					var modifiers;
					if(strEvent == "focus")
					{
						modifiers = onFocusModifiers;
						onFocusModifiers = 0;
					}
					else
					{
						modifiers = getModifiers(e);
					}					
					var wcall=wicketAjaxGet
					(					
						callbackUrl+'&event='+strEvent+'&id='+this.id+'&modifiers='+modifiers,
						null,
						function() { onAjaxError(); }.bind(this),
						function() { return Wicket.$(this.id) != null; }.bind(this)
					);
					return !wcall;
				}
			}
			Wicket.Event.add(el, strEvent, callback)
			if(strEvent == "focus")
			{
				var mousedownCallback = function(e)
				{
					if(!e) e = window.event;
					onFocusModifiers = getModifiers(e);					
				}
				Wicket.Event.add(el, "mousedown", mousedownCallback);
			}
		}
	}
}

function testStyleSheets()
{
	if(document.styleSheets.length >= 29)
	{
		window.location.reload();
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
		setRowBgColorEl: function(el, bgcolor)
		{
			var elChildren = el.childNodes;
			var elChildrenLen = elChildren.length;
	
			if (!(el.tagName.toLowerCase() == "td" && elChildrenLen == 1 && !elChildren[0].tagName))
			{
				// ignore the tableview filler (last column) 
				if(el.attributes['id'])
					el.style.backgroundColor = bgcolor;
			}
				
			for(var i = 0; i < elChildrenLen; i++)
			{
				if(elChildren[i].tagName)			
					Servoy.TableView.setRowBgColorEl(elChildren[i], bgcolor);
			}
		},

		setRowBgColor: function(rowId, bgcolor)
		{
			var rowEl = document.getElementById(rowId);
			if(rowEl)
				Servoy.TableView.setRowBgColorEl(rowEl, bgcolor);
		},

		setTableColumnWidthEl: function(tableElement, columnid, width)
		{
			if(tableElement.id && tableElement.style)
			{
				var idx = tableElement.id.indexOf(":");
		
				if(idx > 0 && idx < tableElement.id.length - 1 && tableElement.id.slice(idx + 1) == columnid)
				{
					tableElement.style["width"] = "" + width + "px";
					return;
				}
			}
		
			var tableElementChildren = tableElement.childNodes;
			if(tableElementChildren)
			{
				for(var i = 0; i < tableElementChildren.length; i++)
					Servoy.TableView.setTableColumnWidthEl(tableElementChildren[i], columnid, width);
			}
		},

		setTableColumnWidth: function(table, columnid, width)
		{
			var tableEl = document.getElementById(table);
			if(tableEl)
				Servoy.TableView.setTableColumnWidthEl(tableEl, columnid, width);
		}	
	};
}

if (typeof(Servoy.DD) == "undefined")
{
	Servoy.DD =
	{
		isDragStarted: false,
		isDragging: false,
		currentElement: new Array(),
		dropCallback: new Array(),
		
		dragStarted: function()
		{
			Servoy.DD.isDragging = true;
			Servoy.DD.isDragStarted = true;
		},
	
		dragStopped: function()
		{
			Servoy.DD.isDragging = false;
			if(window.restartTimer)
			{
				window.restartTimer();
			}
		},
	
		attachDrag: function (array, callback , bUseProxy, bXConstraint, bYConstraint)
		{
			YAHOO.util.DDM.mode = YAHOO.util.DDM.INTERSECT;
			for(var i = 0; i < array.length; i++)
			{
				var dd;
				if(bUseProxy)
					dd = new YAHOO.util.DDProxy(array[i]);
				else
					dd = new YAHOO.util.DD(array[i]);
		
				if(bXConstraint)
					dd.setXConstraint(0, 0);
				if(bYConstraint)
					dd.setYConstraint(0, 0);			
					
				dd.onMouseDown = function(e) {
					requestFocus(this.id);
				};					
					
				dd.startDrag = function(x, y) { 
					wicketAjaxGet(callback + '&a=aStart&xc=' + x + '&yc=' + y + '&draggableID=' + this.id);
					Servoy.DD.dragStarted();
				};
		
				dd.endDrag = function(e) {
					Servoy.DD.dragStopped();
					Servoy.DD.currentElement = new Array();
					wicketAjaxGet(callback + '&a=aEnd&xc=' + e.clientX + '&yc=' + e.clientY + '&draggableID=' + this.id);
				};
					
				dd.onDragEnter = function(ev, targetid) {
					var target;					
					for(var i in targetid)
					{						
						target = targetid[i].id;
						Servoy.DD.currentElement[Servoy.DD.currentElement.length] = target;
					}
 
					wicketAjaxGet(Servoy.DD.dropCallback[target] + '&a=aHover&draggableID=' + this.id + '&targetID=' + target);
				};

				dd.onDragOut = function(ev, targetid)
				{
					if(Servoy.DD.currentElement.length > 0)
					{
						Servoy.DD.currentElement.length = Math.max(0, Servoy.DD.currentElement.length - targetid.length);
					}
					if(Servoy.DD.currentElement.length > 0)
					{						
						targetid = Servoy.DD.currentElement[Servoy.DD.currentElement.length - 1];
						wicketAjaxGet(Servoy.DD.dropCallback[targetid] + '&a=aHover&draggableID=' + this.id + '&targetID=' + targetid);
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
						
						wicketAjaxGet(Servoy.DD.dropCallback[targetid] + '&a=aDrop&xc=' + ev.clientX + '&yc=' + ev.clientY + '&draggableID=' + this.id  + '&targetID=' + targetid);
						Servoy.DD.isDragStarted = false;
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
		}			
	};
}


/* Tooltip fctions */

var tipTimeout;

function showtip(e,message)
{
	var x = 0;
	var y = 0;
	var m;

	if(!e)
		var e = window.event;

	var targetParentWidth = 0;
	var targetParentHeight = 0;
	var src = e.target;	// get the target element
	
	// for IE
	if(!src)
		src = e.srcElement;
		
	if(src.parentNode)
	{
		var positionXY = getXY(src.parentNode);
		var sizeWH = getRootElementSize(src.parentNode);
		targetParentWidth = positionXY[0] + sizeWH[0];
		targetParentHeight = positionXY[1] + sizeWH[1];
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
		
	if(e.pageX || e.pageY)
	{
		x = e.pageX;
		y = e.pageY;
	}
	else
		if(e.clientX || e.clientY)
		{
			x = e.clientX + document.body.scrollLeft + document.documentElement.scrollLeft;
			y = e.clientY + document.body.scrollTop + document.documentElement.scrollTop;
		}
	m = document.getElementById('mktipmsg');

	
	m.innerHTML = message;
	m.style.display = "block";

	m.style.left = x + 20  + "px";	
	m.style.top = y - 4 + "px";		
	
	setTimeout("adjustTooltip("+x+","+targetParentWidth+","+wWidth +","+y+","+targetParentHeight+","+wHeight+");", 0);
	m.style.zIndex = 203;
	
	tipTimeout = setTimeout("hidetip();", 5000);
}

function adjustTooltip(x,targetParentWidth,wWidth,y,targetParentHeight,wHeight)
{
	m = document.getElementById('mktipmsg');
	var tooltipOffsetWidth = x + 20 + m.offsetWidth; 
	if(targetParentWidth < tooltipOffsetWidth || wWidth < tooltipOffsetWidth)
		m.style.left = x - 20 -m.offsetWidth  + "px";

	var tooltipOffsetHeight = y - 4 + m.offsetHeight
	if(targetParentHeight < tooltipOffsetHeight || wHeight < tooltipOffsetHeight)
	{
		m.style.top = y - 4 - m.offsetHeight  + "px";
	}
}

function hidetip()
{
	clearTimeout(tipTimeout);
	var m;
	m = document.getElementById('mktipmsg');
	m.style.display = "none";
}

function getXY(oElement)
{
	var iReturnValue = new Array();
	iReturnValue[0] = 0;
	iReturnValue[1] = 0;
	while( oElement != null )
	{
		iReturnValue[0] += oElement.offsetLeft;
		iReturnValue[1] += oElement.offsetTop;
		oElement = oElement.offsetParent;
	}
	return iReturnValue;
}

function getRootElementSize(oElement)
{
	var iReturnValue = new Array();
	iReturnValue[0] = 0;
	iReturnValue[1] = 0;
	while( oElement != null )
	{
		iReturnValue[0] = oElement.offsetWidth;
		iReturnValue[1] = oElement.offsetHeight;
		oElement = oElement.offsetParent;
	}
	return iReturnValue;
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
function showurl(url,timeout)
{
	var win;
	var mywindow = window;
	while (typeof(mywindow.parent)!= "undefined" && mywindow != mywindow.parent)
	{
		try {		
			win = mywindow.parent.Wicket.Window;
		} catch (ignore) {		
		}
		
		if (typeof(win) != "undefined" && typeof(win.current) != "undefined") {
			// we can't call close directly, because it will delete our window,
			// so we will schedule it as timeout for parent's window
			window.parent.setTimeout(function() {
				win.current.close();			
			}, 0);
			mywindow = mywindow.parent
		}
		else
		{
			break
		}
	}
	mywindow.setTimeout(mywindow.document.location.href=url,timeout)
}


function getModifiers(e) 
{
     if (!e) e = window.event;
     if (!e) return 0;

     var modifiers = 0;
     if(e.ctrlKey)	modifiers += 1
     if(e.shiftKey)	modifiers += 2
     if(e.altKey)	modifiers += 4
     if(e.metaKey)	modifiers += 8

     return modifiers;
}

function getPreferredTableSize(startElementId)
{
	var iReturnValue = new Array();
	iReturnValue[0] = 0;
	iReturnValue[1] = 0;

	var el = document.getElementById(startElementId);
	if(el)
	{
		iReturnValue[0] = el.clientWidth;
		iReturnValue[1] = el.clientHeight;
	}
	return iReturnValue;
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
	   /*
	    **  Returns the caret (cursor) position of the specified text field.
	    **  Return value range is 0-oField.length.
	    */
	   doGetCaretPosition: function(oField) {
	     // Initialize
	     var iCaretPos = 0;
	
	     // IE Support
	     if (document.selection) { 
	
	       // Set focus on the element
	       oField.focus ();
	  
	       // To get cursor position, get empty selection range
	       var oSel = document.selection.createRange ();
	  
	       // Move selection start to 0 position
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
	  }
	}
}


if (typeof(Servoy.Validation) == "undefined")
{
	Servoy.Validation=
	{
		imposeMaxLength: function(obj, mlength)
		{
			if (obj.getAttribute && obj.value.length>mlength)
				obj.value=obj.value.substring(0,mlength)
		},
		
		numbersonly: function(e, decimal, decimalChar, groupingChar, currencyChar, percentChar) 
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
			keychar = String.fromCharCode(key);
			
			if ((key==null) || (key==0) || (key==8) ||  (key==9) || (key==13) || (key==27) ) {
			   return true;
			}
			else if ((("-0123456789").indexOf(keychar) > -1)) {
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
			
			var focusCallback = function(e)
			{
				var caret = Servoy.Utils.doGetCaretPosition(this);
				this.value = this.editValue;
				Servoy.Utils.doSetCaretPosition(this,caret);
			};
			var blurCallback = function(e)
			{
				if (this.value == this.editValue)
				{
					this.value = this.displayValue;
				}
			};
			Wicket.Event.add(element, "focus", focusCallback);
			Wicket.Event.add(element, "blur", blurCallback);
		},
		
		changeCase: function(element,e,upper)
		{
			e = e || window.event;
			var k = e.keyCode || e.charCode || e.which;
			if (e.ctrlKey || e.altKey || e.metaKey) {//Ignore
				return true;
			} else if ((k >= 41 && k <= 125) || k > 186) {//typeable characters
			
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
				element.value = buffer.join('');
				Servoy.Utils.doSetCaretPosition(element,caret+1);
				return false;
			}
		}
	};
}

if (typeof(Servoy.Resize) == "undefined")
{
	Servoy.Resize=
	{
		resizeTimer : null,
		
		onWindowResize: function (callback)
		{
			if (Servoy.Resize.resizeTimer) 
				clearTimeout(Servoy.Resize.resizeTimer);

			var ajaxCall = callback;
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
	}
}