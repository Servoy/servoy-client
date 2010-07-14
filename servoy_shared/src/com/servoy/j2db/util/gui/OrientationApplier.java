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
package com.servoy.j2db.util.gui;

import java.awt.Component;
import java.awt.ComponentOrientation;
import java.util.Locale;

import org.apache.wicket.AttributeModifier;

import com.servoy.j2db.persistence.Solution;

/**
 * Utility class that is able to set the orientation of all kinds of components to one of the constants
 * defined in {@link Solution}.
 * @author acostescu
 */
public class OrientationApplier
{
	public static final String LTR = "ltr"; //$NON-NLS-1$
	public static final String RTL = "rtl"; //$NON-NLS-1$

	/**
	 * Sets the orientation to an AWT component.
	 * @param comp the component.
	 * @param l the locale (needed if the orientation is {@link Solution#TEXT_ORIENTATION_LOCALE_SPECIFIC}).
	 * @param orientation the orientation to set. One of the constants defined in {@link Solution}.
	 */
	public static void setOrientationToAWTComponent(Component comp, Locale l, int orientation)
	{
		/*
		 * swing bug # 4701238 - says that, unfortunately, some components such as JLabel and JButton decide their text orientation based on the type of the
		 * first character in the text, while others (JTextArea, JEditorPane...) based on the component orientation - inconsistent behavior, but hopefully
		 * solved in the future in swing
		 */

		switch (orientation)
		{
			case Solution.TEXT_ORIENTATION_DEFAULT :
				comp.applyComponentOrientation(ComponentOrientation.UNKNOWN);
				break;
			case Solution.TEXT_ORIENTATION_LEFT_TO_RIGHT :
				comp.applyComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
				break;
			case Solution.TEXT_ORIENTATION_RIGHT_TO_LEFT :
				comp.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
				break;
			case Solution.TEXT_ORIENTATION_LOCALE_SPECIFIC :
				comp.applyComponentOrientation(ComponentOrientation.getOrientation(l));
				break;
		}
	}

	/**
	 * Calculates & returns the value for the "dir" attribute that must be added to HTML tags for the
	 * given orientation/locale value pair. If no such attribute must be added,
	 * returns {@link AttributeModifier#VALUELESS_ATTRIBUTE_REMOVE}.
	 * @param l the locale (needed if the orientation is {@link Solution#TEXT_ORIENTATION_LOCALE_SPECIFIC}).
	 * @param orientation the orientation to set. One of the constants defined in {@link Solution}.
	 * @return the value for the "dir" attribute that must be added to HTML tags for the
	 * given orientation/locale value pair ("ltr", "rtl"). If no such attribute must be added, returns {@link AttributeModifier#VALUELESS_ATTRIBUTE_REMOVE}.
	 */
	public static String getHTMLContainerOrientation(Locale l, int orientation)
	{
		String value = AttributeModifier.VALUELESS_ATTRIBUTE_REMOVE;

		switch (orientation)
		{
			// case Solution.TEXT_ORIENTATION_DEFAULT: attribute must not be present - so no change
			case Solution.TEXT_ORIENTATION_LEFT_TO_RIGHT :
				value = LTR;
				break;
			case Solution.TEXT_ORIENTATION_RIGHT_TO_LEFT :
				value = RTL;
				break;
			case Solution.TEXT_ORIENTATION_LOCALE_SPECIFIC :
				value = (ComponentOrientation.getOrientation(l).isLeftToRight()) ? LTR : RTL;
				break;
		}
		return value;
	}

}