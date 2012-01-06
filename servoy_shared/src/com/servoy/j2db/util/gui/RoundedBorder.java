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
import java.util.Arrays;
import java.util.StringTokenizer;

import com.servoy.j2db.util.Utils;

/**
 * @author lvostinar
 * @since 6.1
 */
public class RoundedBorder extends SpecialMatteBorder
{
	private final float[] radius = new float[] { 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f };
	private String[] borderStyles = new String[] { "solid", "solid", "solid", "solid" }; //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$//$NON-NLS-4$

	public RoundedBorder(float top, float left, float bottom, float right, Color topColor, Color leftColor, Color bottomColor, Color rightColor)
	{
		super(top, left, bottom, right, topColor, leftColor, bottomColor, rightColor);
	}

	public float[] getRadius()
	{
		return radius;
	}

	public String getRoundingRadiusString()
	{
		StringBuilder sb = new StringBuilder();
		for (float o : radius)
		{
			if (sb.length() > 0)
			{
				sb.append(";"); //$NON-NLS-1$
			}
			sb.append(o);
		}
		return sb.toString();
	}

	/**
	 * @param f
	 */
	public void setRoundingRadius(String radiusProperties)
	{
		int index = 0;
		StringTokenizer roundedTokenizer = new StringTokenizer(radiusProperties, ";"); //$NON-NLS-1$
		if (roundedTokenizer.countTokens() == 1)
		{
			Arrays.fill(radius, Utils.getAsFloat(roundedTokenizer.nextToken()));
		}
		else
		{
			while (roundedTokenizer.hasMoreTokens())
			{
				if (index < radius.length)
				{
					radius[index++] = Utils.getAsFloat(roundedTokenizer.nextToken());
				}
			}
		}
		super.setRoundingRadius(radius[0]);
	}

	public void setRoundingRadius(float[] radiusProperties)
	{
		if (radiusProperties != null)
		{
			Arrays.fill(radius, radiusProperties[0]);
			for (int i = 0; i < radiusProperties.length; i++)
			{
				if (i < radius.length)
				{
					radius[i] = radiusProperties[i];
				}
			}
		}
		super.setRoundingRadius(radius[0]);
	}

	public String[] getBorderStyles()
	{
		return borderStyles;
	}

	public void setBorderStyles(String[] styles)
	{
		if (styles != null && styles.length > 0)
		{
			Arrays.fill(borderStyles, styles[0]);
			for (int i = 0; i < styles.length; i++)
			{
				if (i < borderStyles.length)
				{
					borderStyles[i] = styles[i];
				}
			}
		}
	}

	public void setBorderStyles(String styles)
	{
		borderStyles = createBorderStyles(styles);
	}

	public String getBorderStylesString()
	{
		StringBuilder sb = new StringBuilder();
		for (String style : borderStyles)
		{
			if (sb.length() > 0)
			{
				sb.append(";"); //$NON-NLS-1$
			}
			sb.append(style);
		}
		return sb.toString();
	}

	public static String[] createBorderStyles(String styles)
	{
		int index = 0;
		String[] borderStyles = new String[4];
		StringTokenizer roundedTokenizer = new StringTokenizer(styles, ";"); //$NON-NLS-1$
		while (roundedTokenizer.hasMoreTokens())
		{
			if (index < borderStyles.length)
			{
				borderStyles[index++] = roundedTokenizer.nextToken();
			}
		}
		return borderStyles;
	}
}
