package com.servoy.j2db.server.headlessclient.dataui;

import java.awt.Insets;
import java.awt.Point;

import com.servoy.j2db.persistence.IAnchorConstants;
import com.servoy.j2db.persistence.ISupportAnchors;
import com.servoy.j2db.persistence.ISupportBounds;

class DefaultCSSBoundsHandler implements ICSSBoundsHandler
{
	public static final DefaultCSSBoundsHandler INSTANCE = new DefaultCSSBoundsHandler();

	public void applyBounds(ISupportBounds component, TextualStyle styleObj, Insets ins, int partStartY, int partEndY, int partWidth,
		boolean enableAnchoring, Point locationModifier)
	{
		int y = component.getLocation().y;
//			if (ins != null) y += ins.top;
		int x = component.getLocation().x;
//			if (ins != null) x += ins.left;
		if (locationModifier != null)
		{
			y = Math.max(y + locationModifier.y, 0);
			x = Math.max(x + locationModifier.x, 0);
		}
		int w = component.getSize().width;
		if (ins != null) w -= (ins.left + ins.right);
		int h = component.getSize().height;
		if (ins != null) h -= (ins.top + ins.bottom);

		int anchorFlags = 0;
		if (component instanceof ISupportAnchors) anchorFlags = ((ISupportAnchors)component).getAnchors();

		createBounds(styleObj, y, x, w, h, component.getSize().width, component.getSize().height, anchorFlags, partStartY, partEndY, partWidth,
			enableAnchoring);
	}

	protected void createBounds(TextualStyle styleObj, int top, int left, int width, int height, int offsetWidth, int offsetHeight, int anchorFlags,
		int partStartY, int partEndY, int partWidth, boolean enableAnchoring)
	{
		if (top != -1) styleObj.setProperty("top", top + "px");
		if (left != -1) styleObj.setProperty("left", left + "px");
		if (width != -1) styleObj.setProperty("width", width + "px");
		if (height != -1) styleObj.setProperty("height", height + "px");

		if (enableAnchoring)
		{
			boolean anchoredTop = (anchorFlags & IAnchorConstants.NORTH) != 0;
			boolean anchoredRight = (anchorFlags & IAnchorConstants.EAST) != 0;
			boolean anchoredBottom = (anchorFlags & IAnchorConstants.SOUTH) != 0;
			boolean anchoredLeft = (anchorFlags & IAnchorConstants.WEST) != 0;

			if (!anchoredLeft && !anchoredRight) anchoredLeft = true;
			if (!anchoredTop && !anchoredBottom) anchoredTop = true;

			if (anchoredTop) styleObj.setProperty("top", (top) + "px");
			else styleObj.remove("top");
			if (anchoredBottom) styleObj.setProperty("bottom", (partEndY - partStartY - top - offsetHeight) + "px");
			else styleObj.remove("bottom");
			if (!anchoredTop || !anchoredBottom) styleObj.setProperty("height", height + "px");
			else styleObj.remove("height");
			if (anchoredLeft) styleObj.setProperty("left", left + "px");
			else styleObj.remove("left");
			if (anchoredRight) styleObj.setProperty("right", (partWidth - left - offsetWidth) + "px");
			else styleObj.remove("right");
			if (!anchoredLeft || !anchoredRight) styleObj.setProperty("width", width + "px");
			else styleObj.remove("width");
			styleObj.setProperty("position", "absolute");
		}
		else
		{
			if ((top != -1) || (left != -1)) styleObj.setProperty("position", "absolute");
		}
	}
}
