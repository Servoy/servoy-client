/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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

import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.ISupportNavigator;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.OrientationApplier;

/**
 * Layout provider for web client that enables anchoring of components.
 *
 * @author gerzse
 */
@SuppressWarnings("nls")
public class AnchoredFormLayoutProvider extends AbstractFormLayoutProvider
{
	public AnchoredFormLayoutProvider(IServiceProvider sp, Solution solution, Form f, String formInstanceName)
	{
		super(sp, solution, f, formInstanceName);
	}

	@Override
	protected void fillFormLayoutCSS(TextualStyle formStyle)
	{
		formStyle.setProperty("min-width", (f.getSize().width + defaultNavigatorShift) + "px");
		formStyle.setProperty("min-height", f.getSize().height + "px");
		formStyle.setProperty("position", "absolute");
		formStyle.setProperty("top", "0px");
		formStyle.setProperty("left", "0px");
		formStyle.setProperty("right", "0px");
		formStyle.setProperty("bottom", "0px");
	}

	@Override
	protected void fillPartLayoutCSS(TextualStyle partStyle, Part part, int spaceUsedOnlyInPrintAbove, int spaceUsedOnlyInPrintBelow)
	{
		if (orientation.equals(OrientationApplier.RTL))
		{
			partStyle.setProperty("right", defaultNavigatorShift + "px");
			partStyle.setProperty("left", "0px");
		}
		else
		{
			partStyle.setProperty("left", defaultNavigatorShift + "px");
			partStyle.setProperty("right", "0px");
		}

		int top = f.getPartStartYPos(part.getUUID().toString());
		if (part.getPartType() <= Part.BODY)
		{
			partStyle.setProperty("top", (top - spaceUsedOnlyInPrintAbove) + "px");
		}
		if (part.getPartType() >= Part.BODY)
		{
			int bottom = f.getSize().height - part.getHeight();
			partStyle.setProperty("bottom", (bottom - spaceUsedOnlyInPrintBelow) + "px");
		}
		if (part.getPartType() != Part.BODY)
		{
			partStyle.setProperty("height", (part.getHeight() - top) + "px");
		}
	}

	@Override
	protected void fillNavigatorLayoutCSS(TextualStyle navigatorStyle)
	{
		if (orientation.equals(OrientationApplier.RTL)) navigatorStyle.setProperty("right", "0px");
		else navigatorStyle.setProperty("left", "0px");
		navigatorStyle.setProperty("width", ISupportNavigator.DEFAULT_NAVIGATOR_WIDTH + "px");

		navigatorStyle.setProperty("top", "0px");
		navigatorStyle.setProperty("bottom", "0px");
	}

	public TextualStyle getLayoutForForm(int customNavigatorWidth, boolean isNavigator, boolean isInTabPanel)
	{
		TextualStyle formStyle = new TextualStyle();

		formStyle.setProperty("position", "absolute");
		if (orientation.equals(OrientationApplier.RTL))
		{
			int right = 0;
			if ((customNavigatorWidth > 0) && !isNavigator) right = customNavigatorWidth;
			formStyle.setProperty("right", right + "px");
			if ((customNavigatorWidth > 0) && isNavigator) formStyle.setProperty("width", customNavigatorWidth + "px");
			else formStyle.setProperty("left", "0px");
		}
		else
		{
			int left = 0;
			if ((customNavigatorWidth > 0) && !isNavigator) left = customNavigatorWidth;
			formStyle.setProperty("left", left + "px");
			if ((customNavigatorWidth > 0) && isNavigator) formStyle.setProperty("width", customNavigatorWidth + "px");
			else formStyle.setProperty("right", "0px");
		}
		formStyle.setProperty("top", "0px");
		formStyle.setProperty("bottom", "0px");

		return formStyle;
	}

}
