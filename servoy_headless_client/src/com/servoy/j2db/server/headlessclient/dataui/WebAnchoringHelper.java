/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

 This program is free software; you can redistribute it and/or modify it under
 the terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along
 with this program; if not, see http://www.gnu.org/licenses or write to the Free
 Software Foundation,Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
 */

package com.servoy.j2db.server.headlessclient.dataui;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Hashtable;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.model.AbstractReadOnlyModel;

import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.ISupportAnchors;
import com.servoy.j2db.server.headlessclient.WrapperContainer;
import com.servoy.j2db.ui.ISupportWebBounds;
import com.servoy.j2db.util.IAnchorConstants;

/**
 * Helper class for web anchoring
 * @author gboros
 */
public class WebAnchoringHelper
{

	public static Component getWrapperComponent(Component comp, IFormElement obj, int start, Dimension panelSize, boolean leftToRight)
	{
		MarkupContainer compWrapper = new WrapperContainer(ComponentFactory.getWebID(null, obj) + TemplateGenerator.WRAPPER_SUFFIX, comp);
		Point l = (obj).getLocation();
		Dimension s = (obj).getSize();
		int anchors = 0;
		if (obj instanceof ISupportAnchors) anchors = ((ISupportAnchors)obj).getAnchors();
		int offsetWidth = s.width;
		int offsetHeight = s.height;
		if (comp instanceof ISupportWebBounds)
		{
			Rectangle b = ((ISupportWebBounds)comp).getWebBounds();
			offsetWidth = b.width;
			offsetHeight = b.height;
		}
		final String styleToReturn = WebAnchoringHelper.computeWrapperDivStyle(l.y, l.x, offsetWidth, offsetHeight, s.width, s.height, anchors, start, start +
			panelSize.height, panelSize.width, leftToRight);
		// first the default
		compWrapper.add(new StyleAppendingModifier(new AbstractReadOnlyModel<String>()
		{
			@Override
			public String getObject()
			{
				return styleToReturn;
			}
		}));
		// then the style t hat can be set on the wrapped component
		compWrapper.add(StyleAttributeModifierModel.INSTANCE);
		// TODO: this needs to be done in a cleaner way. See what is the relation between
		// margin, padding and border when calculating the websize in ChangesRecorder vs. TemplateGenerator.
		// Looks like one of the three is not taken into account during calculations. For now decided to remove
		// the margin and leave the padding and border.
		comp.add(new StyleAppendingModifier(new AbstractReadOnlyModel<String>()
		{
			@Override
			public String getObject()
			{
				return "margin: 0px;"; //$NON-NLS-1$
			}
		}));
		compWrapper.add(comp);

		return compWrapper;
	}

	public static boolean needsWrapperDivForAnchoring(Field field)
	{
		// this needs to be in sync with DesignModeBehavior.needsWrapperDivForAnchoring(String type)
		return (field.getDisplayType() == Field.PASSWORD) || (field.getDisplayType() == Field.TEXT_AREA) || (field.getDisplayType() == Field.COMBOBOX) ||
			(field.getDisplayType() == Field.TYPE_AHEAD) || (field.getDisplayType() == Field.TEXT_FIELD) || (field.getDisplayType() == Field.LIST_BOX) ||
			(field.getDisplayType() == Field.MULTI_SELECTION_LIST_BOX) || (field.getDisplayType() == Field.HTML_AREA && field.getEditable());
	}

	private static String computeWrapperDivStyle(int top, int left, int width, int height, int offsetWidth, int offsetHeight, int anchorFlags, int partStartY,
		int partEndY, int partWidth, boolean leftToRight)
	{
		Hashtable<String, String> style = new Hashtable<String, String>();
		if (top != -1) style.put("top", top + "px"); //$NON-NLS-1$ //$NON-NLS-2$
		if (left != -1) style.put("left", left + "px"); //$NON-NLS-1$ //$NON-NLS-2$
		if (width != -1) style.put("width", width + "px"); //$NON-NLS-1$ //$NON-NLS-2$
		if (height != -1) style.put("height", height + "px"); //$NON-NLS-1$ //$NON-NLS-2$

		boolean anchoredTop = (anchorFlags & IAnchorConstants.NORTH) != 0;
		boolean anchoredRight = (anchorFlags & IAnchorConstants.EAST) != 0;
		boolean anchoredBottom = (anchorFlags & IAnchorConstants.SOUTH) != 0;
		boolean anchoredLeft = (anchorFlags & IAnchorConstants.WEST) != 0;

		if (!anchoredLeft && !anchoredRight) anchoredLeft = true;
		if (!anchoredTop && !anchoredBottom) anchoredTop = true;

		int deltaLeft = leftToRight ? 0 : offsetWidth - width;
		int deltaRight = leftToRight ? offsetWidth - width : 0;
		int deltaBottom = offsetHeight - height;

		if (anchoredTop) style.put("top", (top - partStartY) + "px"); //$NON-NLS-1$ //$NON-NLS-2$
		else style.remove("top"); //$NON-NLS-1$
		if (anchoredBottom) style.put("bottom", (partEndY - top - offsetHeight + deltaBottom) + "px"); //$NON-NLS-1$ //$NON-NLS-2$
		if (!anchoredTop || !anchoredBottom) style.put("height", height + "px"); //$NON-NLS-1$ //$NON-NLS-2$
		else style.remove("height"); //$NON-NLS-1$
		if (anchoredLeft) style.put("left", (left + deltaLeft) + "px"); //$NON-NLS-1$ //$NON-NLS-2$
		else style.remove("left"); //$NON-NLS-1$
		if (anchoredRight) style.put("right", (partWidth - left - offsetWidth + deltaRight) + "px"); //$NON-NLS-1$ //$NON-NLS-2$
		if (!anchoredLeft || !anchoredRight) style.put("width", width + "px"); //$NON-NLS-1$ //$NON-NLS-2$
		else style.remove("width"); //$NON-NLS-1$
		style.put("position", "absolute"); //$NON-NLS-1$ //$NON-NLS-2$

		StringBuffer sb = new StringBuffer();
		for (String key : style.keySet())
		{
			String value = style.get(key);
			sb.append(key);
			sb.append(": "); //$NON-NLS-1$
			sb.append(value);
			sb.append("; "); //$NON-NLS-1$
		}
		return sb.toString();
	}
}
