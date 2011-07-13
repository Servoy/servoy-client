function layoutOneElement(elementInfo, partHeight, formsInfo, formName, currentContainer) 
{
	var element = document.getElementById(elementInfo[0]);
	if (!element)
	{
		Wicket.Log.info("Could not find element '" + elementInfo[0] + "' while anchoring page.");
		return;
	}
	var formInfo = formsInfo[formName];
	var elementHint = elementInfo[6];
	if (/TabPanel/.test(elementHint))
	{
		rearrageTabsInTabPanel(element.id);
	}	

	else if (/Label/.test(elementHint))
	{
		var imgEl = document.getElementById(element.id + "_img");
		if (imgEl)
		{
			var imageURL = imgEl.src;
			var originalImageURL = imageURL;
			imageURL = imageURL.replace(/w=[\d]+/, "w=" + element.clientWidth);
			imageURL = imageURL.replace(/h=[\d]+/, "h=" + element.clientHeight);
			if (imageURL != originalImageURL)
				imgEl.src = imageURL;
		}
	}
	if (/ImgField/.test(elementHint))
	{
		Servoy.Utils.fixMediaLocation(element.id,elementInfo[7]);
	}	
}

function layoutOneContainer(formsInfo, formName, isBrowserWindow)
{
	var formInfo = formsInfo[formName];
	var bodyPart = document.getElementById(formInfo.bodyPartId);
	if (formInfo.bodyPart && bodyPart)
	{
		for (var i=0; i<formInfo.bodyPart.length; i++)
		{
			var elementInfo = formInfo.bodyPart[i];
			layoutOneElement(elementInfo, formInfo.heights[formInfo.bodyPartId], formsInfo, formName, bodyPart);
		}
	}

	for (var id in formInfo.nonBodyParts)
	{
		var part = document.getElementById(id);
		var partElements = formInfo.nonBodyParts[id];
		for (var i=0; i<partElements.length; i++)
		{
			var elementInfo = partElements[i];
			layoutOneElement(elementInfo, formInfo.heights[id], formsInfo, formName, part);
		}
	}
}

var doingAnchorLayout = false;
function layoutEntirePageWorker()	
{	
	if (doingAnchorLayout)
	{
		Wicket.Log.info("Already doing anchoring, exiting.");
		return;
	}
	doingAnchorLayout = true;
	try
	{
		var formsInfo = getAnchoredFormsInfo();
		// First we layout only the top level form (should be only one of this kind).
		for (var i in formsInfo)
		{
			if (formsInfo[i].isTopLevelForm)
				layoutOneContainer(formsInfo, i, true);
		}
		// Next we layout all other non-top-level containers.
		for (var i in formsInfo)
		{
			if (!formsInfo[i].isTopLevelForm)
				layoutOneContainer(formsInfo, i, false);
		}
		
		for(var i in tablesPreferredHeight)
		{
			var newPreferredSize = getPreferredTableSize(i);
			if(newPreferredSize[0] != 0 && newPreferredSize[1] != 0 && 
			   (newPreferredSize[1] != tablesPreferredHeight[i]['height'] || 
			    newPreferredSize[0] != tablesPreferredHeight[i]['width']))
			{
				wicketAjaxGet(tablesPreferredHeight[i]['callback'] + "&bodyWidth=" + newPreferredSize[0] + "&bodyHeight=" + newPreferredSize[1]);
			}
		}
		
		for(var i in beansPreferredSize)
		{
			var newPreferredSize = getPreferredTableSize(i); // we can reuse from tables, it's the same thing
			if(newPreferredSize[0] != 0 && newPreferredSize[1] != 0 && 
			   (newPreferredSize[1] != beansPreferredSize[i]['height'] || 
			    newPreferredSize[0] != beansPreferredSize[i]['width']))
			{
				wicketAjaxGet(beansPreferredSize[i]['callback'] + "&width=" + newPreferredSize[0] + "&height=" + newPreferredSize[1]);
			}
		}

		for(var i in splitPanes)
		{			
			var resize = splitPanes[i]['resize']
			var resizeWeight = splitPanes[i]['resizeWeight'];
			var splitter = splitPanes[i]['splitter'];
			var left = splitPanes[i]['left'];
			var right = splitPanes[i]['right'];
			var leftMin = splitPanes[i]['leftMin'];
			var rightMin = splitPanes[i]['rightMin'];
			var dividerSize = splitPanes[i]['dividerSize'];
			var delta;
			var sizeType, posType;
			var size;
			
			if(splitPanes[i]['orient'] == 'h')
			{
				sizeType = 'width';
				posType = 'left';
				size = splitter.offsetParent.offsetWidth;
				resize.set('maxWidth', size - rightMin);
			}
			else
			{
				sizeType = 'height';
				posType = 'top';
				size = splitter.offsetParent.offsetHeight;
				resize.set('maxHeight', size - rightMin);
			}
								
			delta =  size - splitPanes[i]['currentSize'];
			splitPanes[i]['currentSize'] = size;
											
								
			var splitterSize = parseInt(YAHOO.util.Dom.getStyle(splitter, sizeType), 10) + Math.round(delta * resizeWeight);
			if(splitterSize < dividerSize + leftMin) splitterSize = dividerSize + leftMin;
			if(splitterSize > size - rightMin) splitterSize = size - rightMin;
			YAHOO.util.Dom.setStyle(splitter, sizeType, splitterSize + 'px');
			YAHOO.util.Dom.setStyle(right, posType, splitterSize + 'px');
			YAHOO.util.Dom.setStyle(left, sizeType, (splitterSize - dividerSize) + 'px');
			
			wicketAjaxGet(splitPanes[i]['callback'] + "&location=" + (splitterSize - dividerSize));
		}
		
	}
	catch (ex)
	{
		Wicket.Log.error("Error while anchoring controls: " + ex.description);
	}
	doingAnchorLayout = false;
}
	
	
var layoutTimeout;
var tablesPreferredHeight = new Array();
var beansPreferredSize = new Array();
var splitPanes = new Array();

/**
 * In IE the "onresize" event is fired too many times, so we use this timeout mechanism
 * to postpone the layout calculation until the "onresize" events stop firing.
 */
function layoutEntirePage()
{
	if (layoutTimeout)
		clearTimeout(layoutTimeout);
	layoutTimeout = setTimeout("layoutEntirePageWorker();", 200);
}