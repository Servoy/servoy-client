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
package com.servoy.j2db.util;

import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.StringTokenizer;

import javax.swing.BorderFactory;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.text.AttributeSet;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.CSS;
import javax.swing.text.html.StyleSheet;

import com.servoy.j2db.util.gui.SpecialMatteBorder;

/**
 * @author Jan Blok
 *
 * To change this generated comment go to 
 * Window>Preferences>Java>Code Generation>Code Template
 */
public class FixedStyleSheet extends StyleSheet
{
	public FixedStyleSheet()
	{
		super();
	}

	/*
	 * @see javax.swing.text.StyleContext#getFont(java.lang.String, int, int)
	 */
	@Override
	public Font getFont(AttributeSet a)
	{
		Font font = super.getFont(a);
		Object family = a.getAttribute(CSS.Attribute.FONT_FAMILY);
		if (family != null && family.toString().indexOf(font.getName()) == -1)
		{
			font = PersistHelper.createFont(family.toString(), font.getStyle(), font.getSize());
		}
		return font;
	}

//	is not possible :-(, the Boxpainter constructor is not visible
//	public BoxPainter getBoxPainter(AttributeSet a)
//	{
//		return new BoxPainter(a, css, this);
//	}

	public Insets getMargin(AttributeSet a)
	{
		if (a.getAttribute(CSS.Attribute.MARGIN) != null || a.getAttribute(CSS.Attribute.MARGIN_TOP) != null)
		{
			float top = getLength(CSS.Attribute.MARGIN_TOP, a);
			float bottom = getLength(CSS.Attribute.MARGIN_BOTTOM, a);
			float left = getLength(CSS.Attribute.MARGIN_LEFT, a);
			float right = getLength(CSS.Attribute.MARGIN_RIGHT, a);

			top = makeSizeSave(top);
			right = makeSizeSave(right);
			bottom = makeSizeSave(bottom);
			left = makeSizeSave(left);

			return new Insets((int)top, (int)left, (int)bottom, (int)right);
		}
		return null;
	}

	public Border getBorder(AttributeSet a)
	{
		Border b = null;
		Object unexpandedBorder = a.getAttribute(CSS.Attribute.BORDER);
		//TODO: how should we handle unexpandedBorder... is flaw in cssparser 

		Object o = a.getAttribute(CSS.Attribute.BORDER_STYLE);
		if (o != null)
		{
			Color[] colors = getBorderColor(a);
			String bstyle = o.toString();
			if (bstyle.equals("inset") || bstyle.equals("outset")) //$NON-NLS-1$
			{
				int style = BevelBorder.LOWERED;
				if (bstyle.equals("outset")) style = BevelBorder.RAISED;
				if (colors != null && colors.length > 0)
				{
					if (colors.length == 1)
					{
						// this tries to do the same thing as the web does..
						b = BorderFactory.createBevelBorder(style, colors[0].brighter().brighter(), colors[0].darker().darker());
					}
					if (colors.length == 2)
					{
						b = BorderFactory.createBevelBorder(style, colors[0], colors[1]);
					}
					else if (colors.length > 3)
					{
						b = BorderFactory.createBevelBorder(style, colors[0], colors[1], colors[2], colors[3]);
					}
				}
				else
				{
					b = BorderFactory.createBevelBorder(style);
				}
			}
			else if (bstyle.equals("none")) //$NON-NLS-1$
			{
				b = BorderFactory.createEmptyBorder();
			}
			else if (bstyle.equals("groove")) //$NON-NLS-1$
			{
				b = BorderFactory.createEtchedBorder();
			}
			else if (bstyle.equals("solid") || bstyle.equals("dotted") || bstyle.equals("dashed")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			{
//				int bw = (int) getLength(CSS.Attribute.BORDER_WIDTH, a);
				float top = getLength(CSS.Attribute.BORDER_TOP_WIDTH, a);
				float right = getLength(CSS.Attribute.BORDER_RIGHT_WIDTH, a);
				float bottom = getLength(CSS.Attribute.BORDER_BOTTOM_WIDTH, a);
				float left = getLength(CSS.Attribute.BORDER_LEFT_WIDTH, a);

				int colorCount = (colors == null ? 0 : colors.length);
				colors = expandColors(colors);
//				Object obj = a.getAttribute(CSS.Attribute.BORDER_WIDTH);
				if (bstyle.equals("solid")) //$NON-NLS-1$
				{
					Object sborder_color = a.getAttribute(CSS.Attribute.BORDER_COLOR);
					if (sborder_color != null && "transparent".equals(sborder_color.toString())) //$NON-NLS-1$
					{
						top = makeSizeSave(top);
						right = makeSizeSave(right);
						bottom = makeSizeSave(bottom);
						left = makeSizeSave(left);
						b = BorderFactory.createEmptyBorder((int)top, (int)left, (int)bottom, (int)right);
					}
					else if (colors != null)
					{
						if (top == -1f && right == -1f && bottom == -1f && left == -1f)//is undefined, make 1 px
						{
							b = BorderFactory.createLineBorder(colors[0]);
						}
						else if (top == 0f && right == 0f && bottom == 0f && left == 0f)//is contradiction solid but width 0 make empty border
						{
							b = BorderFactory.createEmptyBorder();
						}
						else
						{
							top = makeSizeSave(top);
							right = makeSizeSave(right);
							bottom = makeSizeSave(bottom);
							left = makeSizeSave(left);
							b = new SpecialMatteBorder(top, left, bottom, right, colors[0], colors[3], colors[2], colors[1]);
						}
					}
				}
				else if (bstyle.equals("dashed")) //$NON-NLS-1$
				{
					if (top <= 0 && left <= 0 && bottom <= 0 && right <= 0)
					{
						top = left = bottom = right = 1;
					}
					b = new SpecialMatteBorder(top, left, bottom, right, colors[0], colors[3], colors[2], colors[1]);
					((SpecialMatteBorder)b).setDashPattern(new float[] { 3, 3 });
				}
				else if (bstyle.equals("dotted")) //$NON-NLS-1$
				{
					if (top <= 0 && left <= 0 && bottom <= 0 && right <= 0)
					{
						top = left = bottom = right = 1;
					}
					b = new SpecialMatteBorder(top, left, bottom, right, colors[0], colors[3], colors[2], colors[1]);
					((SpecialMatteBorder)b).setDashPattern(new float[] { 1, 1 });
				}
			}
		}
		return b;
	}

	private float makeSizeSave(float f)
	{
		if (f < 0f) f = 0f;
		return f;
	}

	private Color[] expandColors(Color[] colors)
	{
		if (colors == null || colors.length == 0)
		{
			colors = new Color[] { Color.black, Color.black };
		}
		else if (colors.length == 1)
		{
			colors = new Color[] { colors[0], colors[0] };
		}

		Color topC = colors[0];
		Color rightC = colors[1];
		Color bottomC = colors[0];
		Color leftC = colors[1];

		if (colors.length > 2)
		{
			bottomC = colors[2];
		}
		if (colors.length > 3)
		{
			leftC = colors[3];
		}

		return new Color[] { topC, rightC, bottomC, leftC };
	}

	/**
	 * Fetches the color to use for borders.  This will either be
	 * the value specified by the border-color attribute (which
	 * is not inherited), or it will default to the color attribute
	 * (which is inherited).
	 */
	Color[] getBorderColor(AttributeSet a)
	{
		Object obj = a.getAttribute(CSS.Attribute.BORDER_COLOR);
		if (obj != null)
		{
			ArrayList colors = new ArrayList();
			String val = obj.toString();
			StringTokenizer tk = new StringTokenizer(val, " "); //$NON-NLS-1$
			while (tk.hasMoreTokens())
			{
				String token = tk.nextToken();
				colors.add(PersistHelper.createColor(token));
			}
			return (Color[])colors.toArray(new Color[colors.size()]);
		}
		return null;
	}

	/**
	 * Get the length/width in px
	 * @param key
	 * @param a
	 * @return -1 if undefined
	 */
	public float getLength(CSS.Attribute key, AttributeSet a)
	{
		Object obj = a.getAttribute(key);
		if (obj != null)
		{
			String val = obj.toString();
			if (val.endsWith("px")) //$NON-NLS-1$
			{
				val = val.substring(0, val.length() - 2);
			}
			return Utils.getAsFloat(val);
		}
		return -1f;
	}

	public int getHAlign(AttributeSet a)
	{
		Object obj = a.getAttribute(CSS.Attribute.TEXT_ALIGN);
		if (obj != null)
		{
			String val = obj.toString();
			if ("left".equals(val)) //$NON-NLS-1$
			{
				return SwingConstants.LEFT;
			}
			else if ("center".equals(val)) //$NON-NLS-1$
			{
				return SwingConstants.CENTER;
			}
			else if ("right".equals(val)) //$NON-NLS-1$
			{
				return SwingConstants.RIGHT;
			}
			else if ("justify".equals(val)) //$NON-NLS-1$
			{
				return SwingConstants.CENTER;
			}
		}
		return -1;
	}

	public int getVAlign(AttributeSet a)
	{
		Object obj = a.getAttribute(CSS.Attribute.VERTICAL_ALIGN);
		if (obj != null)
		{
			String val = obj.toString();
			if ("top".equals(val)) //$NON-NLS-1$
			{
				return SwingConstants.TOP;
			}
			else if ("text-top".equals(val)) //$NON-NLS-1$
			{
				return SwingConstants.TOP;
			}
			else if ("super".equals(val)) //$NON-NLS-1$
			{
				return SwingConstants.TOP;
			}
			else if ("middle".equals(val)) //$NON-NLS-1$
			{
				return SwingConstants.CENTER;
			}
			else if ("baseline".equals(val)) //$NON-NLS-1$
			{
				return SwingConstants.CENTER;
			}
			else if ("bottom".equals(val)) //$NON-NLS-1$
			{
				return SwingConstants.BOTTOM;
			}
			else if ("text-bottom".equals(val)) //$NON-NLS-1$
			{
				return SwingConstants.BOTTOM;
			}
			else if ("sub".equals(val)) //$NON-NLS-1$
			{
				return SwingConstants.BOTTOM;
			}
		}
		return -1;
	}

	@Override
	public void addCSSAttribute(MutableAttributeSet attr, CSS.Attribute key, String value)
	{
		if (key == CSS.Attribute.BACKGROUND_COLOR && "transparent".equalsIgnoreCase(value)) attr.addAttribute(key, value);
		else super.addCSSAttribute(attr, key, value);
	}
}