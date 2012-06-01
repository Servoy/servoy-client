/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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

import java.awt.Color;
import java.awt.Component;
import java.awt.Insets;

import javax.swing.border.BevelBorder;

import com.servoy.j2db.util.ComponentFactoryHelper;

/**
 * Bevel border supporting custom border insets
 * 
 * @author gboros
 *
 */
public class CustomBevelBorder extends BevelBorder implements ISupportCustomBorderInsets
{
	private final Insets customBorderInsets;
	// tries to imitate web behavior when only one color is defined
	private Color customColor = null;

	public CustomBevelBorder(int bevelType, Insets customBorderInsets)
	{
		super(bevelType);
		this.customBorderInsets = customBorderInsets;
	}

	public CustomBevelBorder(int bevelType, Color customColor, Insets customBorderInsets)
	{
		super(bevelType);
		this.customBorderInsets = customBorderInsets;
		this.customColor = customColor;
	}

	public CustomBevelBorder(int bevelType, Color highlight, Color shadow, Insets customBorderInsets)
	{
		super(bevelType, highlight, shadow);
		this.customBorderInsets = customBorderInsets;
	}

	public CustomBevelBorder(int bevelType, Color highlightOuterColor, Color highlightInnerColor, Color shadowOuterColor, Color shadowInnerColor,
		Insets customBorderInsets)
	{
		super(bevelType, highlightOuterColor, highlightInnerColor, shadowOuterColor, shadowInnerColor);
		this.customBorderInsets = customBorderInsets;
	}

	public Insets getCustomBorderInsets()
	{
		if (customBorderInsets != null)
		{
			return customBorderInsets;
		}
		return ComponentFactoryHelper.getBorderInsetsForNoComponent(this);
	}

	@Override
	public Color getHighlightInnerColor(Component c)
	{
		if (customColor != null)
		{
			return customColor.brighter().brighter();
		}
		return super.getHighlightInnerColor(c);
	}

	@Override
	public Color getHighlightOuterColor(Component c)
	{
		if (customColor != null)
		{
			return customColor.brighter().brighter().brighter();
		}
		return super.getHighlightOuterColor(c);
	}

	@Override
	public Color getShadowInnerColor(Component c)
	{
		if (customColor != null)
		{
			return customColor.darker().darker();
		}
		return super.getShadowInnerColor(c);
	}

	@Override
	public Color getShadowOuterColor(Component c)
	{
		if (customColor != null)
		{
			return customColor.darker().darker().darker();
		}
		return super.getShadowOuterColor(c);
	}
}
