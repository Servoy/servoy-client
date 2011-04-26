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
var ignoreFocusGained = null;
function storeValueBeforeUpdate()
{

	focusedElement = Wicket.Focus.getFocusedElement();
	if (typeof(focusedElement) != "undefined" && focusedElement != null
	 && focusedElement.type != "button" && focusedElement.type != "submit")
	{
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
					for (var j=tabHolder.childNodes.length-1; j>=0; j--)
					{
						var tab = tabHolder.childNodes[j];
						if (tab.nodeName.toLowerCase() == "div") {
							var thisAnch = tab.getElementsByTagName("a")[0];
							if (thisAnch.getAttribute("firsttab") == "true")
								markedFirstTab = tab;
							allTabsTogether.unshift(tab);
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
					if(strEvent == "blur")
					{
						ignoreFocusGained = null;
					}
					if(Wicket.Focus.refocusLastFocusedComponentAfterResponse && !Wicket.Focus.focusSetFromServer) return true;
					if(!e) e = window.event;
					var modifiers;
					if(strEvent == "focus")
					{
						// skip onFocus callback if the component has been re-focused after a response					
						modifiers = onFocusModifiers;
						onFocusModifiers = 0;
					}
					else
					{
						modifiers = Servoy.Utils.getModifiers(e);
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
					if(Wicket.Focus.refocusLastFocusedComponentAfterResponse && !Wicket.Focus.focusSetFromServer) return true;
					if (ignoreFocusGained && ignoreFocusGained == this.id)
					{
						ignoreFocusGained = null;
						return true;
					}
					if(!e) e = window.event;
					var modifiers;
					if(strEvent == "focus")
					{
						// skip onFocus callback if the component has been re-focused after a response
						modifiers = onFocusModifiers;
						onFocusModifiers = 0;
					}
					else
					{
						modifiers = Servoy.Utils.getModifiers(e);
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
			if(strEvent == "blur")
			{
				var b = el.onblur; 
				el.onblur = function(event)
				{ 
					if (b) { b.apply(this,[event]);}
					callback.apply(this,[event]);
				}
			}
			else
			{
				Wicket.Event.add(el, strEvent, callback)
			}
			if(strEvent == "focus")
			{
				var mousedownCallback = function(e)
				{
					if(!e) e = window.event;
					onFocusModifiers = Servoy.Utils.getModifiers(e);
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
		setRowStyleEl: function(el, bgcolor, fgcolor, fontStyle, fontWeight, fontSize, fontFamily, inDepth)
		{
			var elChildren = el.childNodes;
			var elChildrenLen = elChildren.length;
	
			if (!(el.tagName.toLowerCase() == "td" && elChildrenLen == 1 && !elChildren[0].tagName))
			{
				// ignore the tableview filler (last column) 
				if(el.attributes['id'])
				{
					el.style.backgroundColor = bgcolor;
					el.style.color = fgcolor;
					el.style.fontStyle = fontStyle;
					el.style.fontWeight = fontWeight;
					el.style.fontSize = fontSize;
					el.style.fontFamily = fontFamily;
				}
			}
			if (inDepth)
			{
				var continueInDepth = (el.className != 'tableviewcell');
				for(var i = 0; i < elChildrenLen; i++)
				{
					if(elChildren[i].tagName)			
						Servoy.TableView.setRowStyleEl(elChildren[i], bgcolor, fgcolor, fontStyle, fontWeight, fontSize, fontFamily, continueInDepth);
				}
			}
		},

		setRowStyle: function(rowId, bgcolor, fgcolor, fontStyle, fontWeight, fontSize, fontFamily)
		{
			var rowEl = document.getElementById(rowId);
			if(rowEl)
			{
				var rowChildren = rowEl.childNodes;
				var rowChildrenLen = rowChildren.length;
				
				for(var i = 0; i < rowChildrenLen; i++)
				{
					if(rowChildren[i].tagName)			
						Servoy.TableView.setRowStyleEl(rowChildren[i], bgcolor, fgcolor, fontStyle, fontWeight, fontSize, fontFamily, true);
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
		isConstrainSet: false,

		dragStarted: function()
		{
			Servoy.DD.isDragging = true;
			Servoy.DD.isDragStarted = true;
			Servoy.DD.isRestartTimerNeeded = false;
			Servoy.DD.klEsc.enable();
		},
	
		dragStopped: function()
		{
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
	
		attachDrag: function (array, callback , bUseProxy, bXConstraint, bYConstraint)
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
					dd = new YAHOO.util.DDProxy(array[i], null, {resizeFrame : false, centerFrame : true});
				else
					dd = new YAHOO.util.DD(array[i]);

				if(bXConstraint)
				{
					dd.setXConstraint(0, 0);
					Servoy.DD.isConstrainSet = true;
					
				}
				if(bYConstraint)
				{
					dd.setYConstraint(0, 0);
					Servoy.DD.isConstrainSet = true;
				}			
					
				dd.onMouseDown = function(e) {
					requestFocus(this.id);
				};									

				dd.on('b4MouseDownEvent', function(ev)
				{
					if(Servoy.DD.isTargetDraggable(YAHOO.util.Event.getTarget(ev)))
					{
						Servoy.DD.mouseDownEvent = ev;
						return true;
					}
					return false;
				}, dd, true);
				
				dd.on('b4StartDragEvent', function()
				{
					var x = YAHOO.util.Event.getPageX(Servoy.DD.mouseDownEvent);
					var y = YAHOO.util.Event.getPageY(Servoy.DD.mouseDownEvent);
//					if(!Servoy.DD.isConstrainSet)
//					{
//						var clientRegion = YAHOO.util.Dom.getClientRegion();
//						var elXY = YAHOO.util.Dom.getXY(this.getEl());
//						this.setXConstraint(elXY[0], clientRegion.width - elXY[0]);
//						this.setYConstraint(elXY[1], clientRegion.height - elXY[1]);											
//					}					
					wicketAjaxGet(callback + '&a=aStart&xc=' + x + '&yc=' + y + '&draggableID=' + this.id);
					Servoy.DD.dragStarted();
					return true;
				}, dd, true);				

				dd.endDrag = function(e) {
					Servoy.DD.dragStopped();
					Servoy.DD.currentElement = new Array();
					var x = YAHOO.util.Event.getPageX(e);
					var y = YAHOO.util.Event.getPageY(e);					
					wicketAjaxGet(callback + '&a=aEnd&xc=' + x + '&yc=' + y + '&draggableID=' + this.id);
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
						wicketAjaxGet(Servoy.DD.dropCallback[target] + '&a=aHover&draggableID=' + this.id + '&targetID=' + target);
					}
				};

				dd.onDragOut = function(ev, targetid)
				{
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
							wicketAjaxGet(Servoy.DD.dropCallback[hoverTarget] + '&a=aHover&draggableID=' + this.id + '&targetID=' + hoverTarget);
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
						wicketAjaxGet(Servoy.DD.dropCallback[targetid] + '&a=aDrop&xc=' + x + '&yc=' + y + '&draggableID=' + this.id  + '&targetID=' + targetid);
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
				return YAHOO.util.DragDropMgr.isDragDrop(targetAncestor.id);
			}

			return false;
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
	m.style.left = x + 20  + "px";	
	m.style.top = y - 4 + "px";		
	m.style.zIndex = 203;
	m.style.width = "";
	m.style.overflow = "hidden";
	setTimeout("adjustAndShowTooltip("+x+","+wWidth +","+y+","+wHeight+");", 0);
	tipTimeout = setTimeout("hidetip();", 5000);
}

function adjustAndShowTooltip(x, wWidth, y, wHeight)
{
	m = document.getElementById('mktipmsg');
	m.style.display = "block";
	var tooltipOffsetWidth = x + 20 + m.offsetWidth; 

	if(wWidth < tooltipOffsetWidth)
	{
		var newLeft = x - 20 -m.offsetWidth;
		if(newLeft < 0)
		{
			newLeft = 0;
			m.style.width = x - 20 + "px";
		}
		m.style.left = newLeft  + "px";
	}

	var tooltipOffsetHeight = y - 4 + m.offsetHeight
	if(wHeight < tooltipOffsetHeight)
	{
		var newTop = y - 4 - m.offsetHeight;
		m.style.top = newTop  + "px";
	}
}

function hidetip()
{
	clearTimeout(tipTimeout);
	var m;
	m = document.getElementById('mktipmsg');
	m.style.display = "none";
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
function showurl(url,timeout,closeDialog)
{
	var win;
	var mywindow = window;

	if(closeDialog)
	{
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
	}

	mywindow.setTimeout(mywindow.document.location.href=url,timeout)
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
	  },
	  
	  getXY: function(oElement)
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
	  
	  getActionParams: function(e)
	  {
		var src = e.target;	// get the target element
		// for IE
		if(!src)
			src = e.srcElement;
		return '&modifiers='+Servoy.Utils.getModifiers(e)+'&mx=' + ((e.pageX ? e.pageX : e.clientX + document.body.scrollLeft + document.documentElement.scrollLeft) - Servoy.Utils.getXY(src)[0]) + '&my=' + ((e.pageY ? e.pageY : e.clientY + document.body.scrollLeft + document.documentElement.scrollLeft) - Servoy.Utils.getXY(src)[1]);
	  },
	  
	  redirectKeepingScrolls: function()
	  {
	  	var url = arguments[0];
		for (var i=1;i<arguments.length;i++)
		{
			var element = document.getElementById(arguments[i]);
			if (element && (element.scrollLeft > 0 || element.scrollTop > 0))
			{
				url += '&scroll_'+arguments[i]+'='+element.scrollLeft+'_'+element.scrollTop
			}
		}
		window.location = url;
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

			if ((key==null) || (key==0) || (key==8) ||  (key==9) || (key==13) || (key==27) ) {
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
			
			var focusCallback = function(e)
			{
				if (this.editValue != '')
				{
					var caret = Servoy.Utils.doGetCaretPosition(this);
					this.value = this.editValue;
					Servoy.Utils.doSetCaretPosition(this,caret);
				}
			};
			var blurCallback = function(e)
			{
				if (this.editValue != '' && this.value == this.editValue)
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
		}
	}
}
if (typeof(Servoy.Rollover) == "undefined")
{
	Servoy.Rollover=
	{
		bgUrl : null,
		bgPosition : null,
		bgRepeat : null,
		
		onMouseOver: function (elementId,imageUrl,position,repeat)
		{
			var el = document.getElementById(elementId);
			bgUrl = el.style.backgroundImage;
			bgPosition = el.style.backgroundPosition;
			bgRepeat = el.style.backgroundRepeat;
			el.style.backgroundImage = imageUrl;
			el.style.backgroundPosition = position;
			el.style.backgroundRepeat = repeat;
		},
		
		onMouseOut: function (elementId)
		{
			var el = document.getElementById(elementId);
			el.style.backgroundImage = bgUrl;
			el.style.backgroundPosition = bgPosition;
			el.style.backgroundRepeat = bgRepeat;
		}
	}
}

if (typeof(Servoy.ClientDesign) == "undefined")
{
	Servoy.ClientDesign = 
	{
		selectedResizeElement : null,
		selectedElementId : null,
		designableElementsArray : null,
		callbackurl : null,
		
		selectElement: function(e)
		{
			var elem;
			if (!e)
			{
				e = window.event;
			}
			
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
			
			//get enclosing wrapper to work on
			if (elem.id && elem.parentNode && elem.parentNode.id && elem.parentNode.id.match(elem.id+'_wrapper'))
			{
				elem = elem.parentNode;
			}
			
			if (Servoy.ClientDesign.selectedResizeElement != null)
			{
				//deselect old yui elements
				Servoy.ClientDesign.selectedResizeElement.destroy()
				Servoy.ClientDesign.selectedResizeElement = null;
			}
			
			if (elem.id)
			{
				Servoy.ClientDesign.attachElement(elem);
			}
		},
		
		attachElement: function(elem)
		{
			var elementDescription = Servoy.ClientDesign.designableElementsArray[elem.id];
			if (elementDescription)
			{
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

				resize.dd.endDrag = function(args) 
				{ 
					Servoy.DD.dragStopped(); 
					return true; 
				};
				
				resize.dd.startDrag = function(x,y)	
				{
					var element = document.getElementById(this.id);
					this.setYConstraint(element.offsetTop,element.offsetParent.offsetHeight-element.offsetTop-element.offsetHeight);
					this.setXConstraint(element.offsetLeft,element.offsetParent.offsetWidth-element.offsetLeft-element.offsetWidth);
					wicketAjaxGet(Servoy.ClientDesign.callbackurl+'&a=aStart&xc=' + element.style.left + '&yc=' + element.style.top + '&draggableID=' + this.id);
					Servoy.DD.dragStarted();
				};

				resize.dd.on('mouseUpEvent', function(ev, targetid)	
				{
					var element = document.getElementById(this.id);
					var url = Servoy.ClientDesign.callbackurl+'&a=aDrop&xc=' + Servoy.addPositions(element.offsetParent.style.left, element.style.left) + '&yc=' + Servoy.addPositions(element.offsetParent.style.top,element.style.top) + '&draggableID=' + this.id  + '&targetID=' + targetid
					Servoy.ClientDesign.selectedResizeElement.destroy()
					Servoy.ClientDesign.selectedResizeElement = null;
					var parentLeft = wicketAjaxGet(url);
				});

				resize.on('beforeResize', function(args) 
				{
					return true;
				});
				
				resize.on('endResize', function(args) 
				{
					var url = Servoy.ClientDesign.callbackurl+'&a=aResize&draggableID=' + this._wrap.id + '&resizeHeight=' + args.height + '&resizeWidth=' + args.width + '&xc=' + this._wrap.style.left + '&yc=' + this._wrap.style.top;
					Servoy.ClientDesign.selectedResizeElement.destroy()
					Servoy.ClientDesign.selectedResizeElement = null;
					wicketAjaxGet(url);
				});
				Servoy.ClientDesign.selectedElementId = elem.id;
				Servoy.ClientDesign.selectedResizeElement = resize;
			}
		},
		
		attach: function (array,url)
		{
			Servoy.ClientDesign.designableElementsArray = array;
			Servoy.ClientDesign.callbackurl = url;
			Wicket.Event.add(document.body, "mousedown", Servoy.ClientDesign.selectElement);
			var Dom = YAHOO.util.Dom,Event = YAHOO.util.Event; //to load stuff?
		},
		
		reattach: function()
		{
			window.setTimeout(function()
			{
			   if (Servoy.ClientDesign.selectedElementId && Servoy.ClientDesign.selectedResizeElement == null)
			   {
			   		Servoy.ClientDesign.attachElement(document.getElementById(Servoy.ClientDesign.selectedElementId));
			   }
			}
			,0);
		}
	};
}	

if (typeof(Servoy.HTMLEdit) == "undefined")
{
	Servoy.HTMLEdit = 
	{
		attach: function (elem)
		{
			if (elem == null) return;
			
		    var Dom = YAHOO.util.Dom,
		        Event = YAHOO.util.Event;
		    
		    var iframe = document.getElementById(elem.id+'_iframe')
		    var readonly = false;
		    var editor_url;
		    if (elem.readOnly || elem.disabled)
		    {
		    	readonly=true;
		    }
		    if (iframe == null)
		    {
				iframe = document.createElement('iframe');
				iframe.id = elem.id+'_iframe';
				iframe.frameborder = '0';
				iframe.scrolling = 'no'
				editor_url = 'resources/yui/sv_editor.html';
				if (readonly)
					editor_url += "?readonly=true"
				elem.parentNode.appendChild(iframe);
			}
			else
			{
				editor_url = 'resources/yui/sv_editor.html?rnd='+Math.floor(Math.random()*100000);
				if (readonly)
					editor_url += "&readonly=true"
			}
		    editor_url = editor_url + '#'+ elem.id;
		    iframe.src = editor_url;
            var xy = Dom.getXY(elem);
            Dom.setXY(iframe, xy);
            var w = Dom.getStyle(elem, 'width');
            Dom.setStyle(iframe, 'width', w);
            var h = Dom.getStyle(elem, 'height');
            Dom.setStyle(iframe, 'height', h);

            Dom.setStyle(iframe, 'display', 'inline');
			Dom.setStyle(elem, 'display', 'none');
		}
	};
}