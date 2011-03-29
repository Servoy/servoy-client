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
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.server.headlessclient.dataui.TemplateGenerator.TextualStyle;
import com.servoy.j2db.util.OrientationApplier;

/**
 * Layout provider for web client that enables anchoring of components.
 * 
 * @author gerzse
 */
public class AnchoredFormLayoutProvider extends AbstractFormLayoutProvider
{
	public AnchoredFormLayoutProvider(IServiceProvider sp, Solution solution, Form f, String formInstanceName)
	{
		super(sp, solution, f, formInstanceName);
	}

	@Override
	protected void fillFormLayoutCSS(TextualStyle formStyle)
	{
		formStyle.setProperty("min-width", (f.getSize().width + defaultNavigatorShift) + "px"); //$NON-NLS-1$ //$NON-NLS-2$
		formStyle.setProperty("min-height", f.getSize().height + "px"); //$NON-NLS-1$ //$NON-NLS-2$
		formStyle.setProperty("position", "absolute"); //$NON-NLS-1$ //$NON-NLS-2$
		formStyle.setProperty("top", "0px"); //$NON-NLS-1$ //$NON-NLS-2$
		formStyle.setProperty("left", "0px"); //$NON-NLS-1$ //$NON-NLS-2$
		formStyle.setProperty("right", "0px"); //$NON-NLS-1$ //$NON-NLS-2$
		formStyle.setProperty("bottom", "0px"); //$NON-NLS-1$ //$NON-NLS-2$		
	}

	@Override
	protected void fillPartLayoutCSS(TextualStyle partStyle, Part part, int spaceUsedOnlyInPrintAbove, int spaceUsedOnlyInPrintBelow)
	{
		if (orientation.equals(OrientationApplier.RTL))
		{
			partStyle.setProperty("right", defaultNavigatorShift + "px"); //$NON-NLS-1$ //$NON-NLS-2$
			partStyle.setProperty("left", "0px"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		else
		{
			partStyle.setProperty("left", defaultNavigatorShift + "px"); //$NON-NLS-1$ //$NON-NLS-2$
			partStyle.setProperty("right", "0px"); //$NON-NLS-1$ //$NON-NLS-2$
		}

		int top = f.getPartStartYPos(part.getID());
		if (part.getPartType() <= Part.BODY)
		{
			partStyle.setProperty("top", (top - spaceUsedOnlyInPrintAbove) + "px"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (part.getPartType() >= Part.BODY)
		{
			int bottom = f.getSize().height - part.getHeight();
			partStyle.setProperty("bottom", (bottom - spaceUsedOnlyInPrintBelow) + "px"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (part.getPartType() != Part.BODY)
		{
			partStyle.setProperty("height", (part.getHeight() - top) + "px"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	@Override
	protected void fillNavigatorLayoutCSS(TextualStyle navigatorStyle)
	{
		if (orientation.equals(OrientationApplier.RTL)) navigatorStyle.setProperty("right", "0px"); //$NON-NLS-1$ //$NON-NLS-2$
		else navigatorStyle.setProperty("left", "0px"); //$NON-NLS-1$ //$NON-NLS-2$
		navigatorStyle.setProperty("width", WebDefaultRecordNavigator.DEFAULT_WIDTH + "px"); //$NON-NLS-1$ //$NON-NLS-2$

		navigatorStyle.setProperty("top", "0px"); //$NON-NLS-1$ //$NON-NLS-2$
		navigatorStyle.setProperty("bottom", "0px"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public TextualStyle getLayoutForForm(int customNavigatorWidth, boolean isNavigator, boolean isInTabPanel)
	{
		TextualStyle formStyle = new TextualStyle();

		formStyle.setProperty("position", "absolute"); //$NON-NLS-1$ //$NON-NLS-2$
		if (orientation.equals(OrientationApplier.RTL))
		{
			int right = 0;
			if ((customNavigatorWidth > 0) && !isNavigator) right = customNavigatorWidth;
			formStyle.setProperty("right", right + "px"); //$NON-NLS-1$//$NON-NLS-2$
			if ((customNavigatorWidth > 0) && isNavigator) formStyle.setProperty("width", customNavigatorWidth + "px"); //$NON-NLS-1$ //$NON-NLS-2$			
			else formStyle.setProperty("left", "0px"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		else
		{
			int left = 0;
			if ((customNavigatorWidth > 0) && !isNavigator) left = customNavigatorWidth;
			formStyle.setProperty("left", left + "px"); //$NON-NLS-1$//$NON-NLS-2$
			if ((customNavigatorWidth > 0) && isNavigator) formStyle.setProperty("width", customNavigatorWidth + "px"); //$NON-NLS-1$ //$NON-NLS-2$			
			else formStyle.setProperty("right", "0px"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		formStyle.setProperty("top", "0px"); //$NON-NLS-1$ //$NON-NLS-2$
		formStyle.setProperty("bottom", "0px"); //$NON-NLS-1$ //$NON-NLS-2$		

		return formStyle;
	}

}
