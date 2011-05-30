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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.swing.BorderFactory;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.text.AttributeSet;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.CSS;
import javax.swing.text.html.CSS.Attribute;
import javax.swing.text.html.StyleSheet;

import com.servoy.j2db.util.gui.SpecialMatteBorder;

/**
 * @author jblok
 *
 * Enhanced/fixed subclass for proper behaviour.
 */
public class FixedStyleSheet extends StyleSheet implements IStyleSheet
{
	private static final String BORDER_COLOR_TRANSPARENT = "transparent"; //$NON-NLS-1$
	private static final String BORDER_STYLE_DASHED = "dashed"; //$NON-NLS-1$
	private static final String BORDER_STYLE_DOTTED = "dotted"; //$NON-NLS-1$
	private static final String BORDER_STYLE_GROOVE = "groove"; //$NON-NLS-1$
	private static final String BORDER_STYLE_INSET = "inset"; //$NON-NLS-1$
	private static final String BORDER_STYLE_NONE = "none"; //$NON-NLS-1$
	private static final String BORDER_STYLE_OUTSET = "outset"; //$NON-NLS-1$
	private static final String BORDER_STYLE_SOLID = "solid"; //$NON-NLS-1$
	private static final String[] BORDER_STYLES = new String[] { BORDER_STYLE_DASHED, BORDER_STYLE_DOTTED, BORDER_STYLE_GROOVE, BORDER_STYLE_INSET, BORDER_STYLE_NONE, BORDER_STYLE_OUTSET, BORDER_STYLE_SOLID };

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
		if (hasMargin(a))
		{
			float top = getLength(a.getAttribute(CSS.Attribute.MARGIN_TOP));
			float bottom = getLength(a.getAttribute(CSS.Attribute.MARGIN_BOTTOM));
			float left = getLength(a.getAttribute(CSS.Attribute.MARGIN_LEFT));
			float right = getLength(a.getAttribute(CSS.Attribute.MARGIN_RIGHT));

			return new Insets(top < 0 ? 0 : (int)top, left < 0 ? 0 : (int)left, bottom < 0 ? 0 : (int)bottom, right < 0 ? 0 : (int)right);
		}
		return null;
	}

	public Border getBorder(AttributeSet a)
	{
		Border b = null;

		// Initial values.
		Object borderStyle = null;
		Object borderColor = null;
		float top = getLength(null);
		float right = getLength(null);
		float bottom = getLength(null);
		float left = getLength(null);

		// Try to use the unexpanded "border" attribute, if any.
		Object unexpandedBorder = a.getAttribute(CSS.Attribute.BORDER);
		if (unexpandedBorder != null)
		{
			StringTokenizer st = new StringTokenizer(unexpandedBorder.toString());
			// Some heuristics here. May be too permissive, but should work in the
			// general case.
			while (st.hasMoreTokens())
			{
				String tok = st.nextToken();
				// If we got digits in the token, then it refers to border size.
				if (tok.matches("^[0-9]+.*")) //$NON-NLS-1$
				{
					top = right = bottom = left = getLength(tok);
				}
				// If it is a border style keyword, use it accordingly. 
				else if (Arrays.asList(BORDER_STYLES).contains(tok))
				{
					borderStyle = tok;
				}
				// If "transparent" then transparent.
				else if (tok.equals(BORDER_COLOR_TRANSPARENT))
				{
					borderColor = BORDER_COLOR_TRANSPARENT;
				}
				// Otherwise assume it is a color.
				else
				{
					Color c = PersistHelper.createColor(tok);
					if (c != null) borderColor = tok;
				}
			}
		}

		// If any specific properties are set, they will just override what was
		// extracted from the unexpanded "border" attribute.
		if (a.isDefined(CSS.Attribute.BORDER_STYLE)) borderStyle = a.getAttribute(CSS.Attribute.BORDER_STYLE);
		if (a.isDefined(CSS.Attribute.BORDER_COLOR)) borderColor = a.getAttribute(CSS.Attribute.BORDER_COLOR);
		if (a.isDefined(CSS.Attribute.BORDER_TOP_WIDTH)) top = getLength(a.getAttribute(CSS.Attribute.BORDER_TOP_WIDTH));
		if (a.isDefined(CSS.Attribute.BORDER_RIGHT_WIDTH)) right = getLength(a.getAttribute(CSS.Attribute.BORDER_RIGHT_WIDTH));
		if (a.isDefined(CSS.Attribute.BORDER_BOTTOM_WIDTH)) bottom = getLength(a.getAttribute(CSS.Attribute.BORDER_BOTTOM_WIDTH));
		if (a.isDefined(CSS.Attribute.BORDER_LEFT_WIDTH)) left = getLength(a.getAttribute(CSS.Attribute.BORDER_LEFT_WIDTH));

		if (borderStyle != null)
		{
			Color[] colors = getBorderColor(borderColor);
			String bstyle = borderStyle.toString();
			if (bstyle.equals(BORDER_STYLE_INSET) || bstyle.equals(BORDER_STYLE_OUTSET))
			{
				int style = BevelBorder.LOWERED;
				if (bstyle.equals(BORDER_STYLE_OUTSET)) style = BevelBorder.RAISED;
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
			else if (bstyle.equals(BORDER_STYLE_NONE))
			{
				b = BorderFactory.createEmptyBorder();
			}
			else if (bstyle.equals(BORDER_STYLE_GROOVE))
			{
				b = BorderFactory.createEtchedBorder();
			}
			else if (bstyle.equals(BORDER_STYLE_SOLID) || bstyle.equals(BORDER_STYLE_DOTTED) || bstyle.equals(BORDER_STYLE_DASHED))
			{
//				int bw = (int) getLength(CSS.Attribute.BORDER_WIDTH, a);

//				int colorCount = (colors == null ? 0 : colors.length);
				colors = expandColors(colors);
//				Object obj = a.getAttribute(CSS.Attribute.BORDER_WIDTH);
				if (bstyle.equals(BORDER_STYLE_SOLID))
				{
					if (borderColor != null && BORDER_COLOR_TRANSPARENT.equals(borderColor.toString()))
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
				else if (bstyle.equals(BORDER_STYLE_DASHED))
				{
					if (top <= 0 && left <= 0 && bottom <= 0 && right <= 0)
					{
						top = left = bottom = right = 1;
					}
					b = new SpecialMatteBorder(top, left, bottom, right, colors[0], colors[3], colors[2], colors[1]);
					((SpecialMatteBorder)b).setDashPattern(new float[] { 3, 3 });
				}
				else if (bstyle.equals(BORDER_STYLE_DOTTED))
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
	private Color[] getBorderColor(Object obj)
	{
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
	public float getLength(Object obj)
	{
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
		if (key == CSS.Attribute.BACKGROUND_COLOR && BORDER_COLOR_TRANSPARENT.equalsIgnoreCase(value)) attr.addAttribute(key, value);
		else super.addCSSAttribute(attr, key, value);
	}

	public static Attribute[] borderAttributes = new Attribute[] { CSS.Attribute.BORDER, CSS.Attribute.BORDER_BOTTOM, CSS.Attribute.BORDER_BOTTOM_WIDTH, CSS.Attribute.BORDER_COLOR, CSS.Attribute.BORDER_LEFT, CSS.Attribute.BORDER_LEFT_WIDTH, CSS.Attribute.BORDER_RIGHT, CSS.Attribute.BORDER_RIGHT_WIDTH, CSS.Attribute.BORDER_STYLE, CSS.Attribute.BORDER_TOP, CSS.Attribute.BORDER_TOP_WIDTH, CSS.Attribute.BORDER_WIDTH };
	private static Attribute[] marginAttributes = new Attribute[] { CSS.Attribute.MARGIN, CSS.Attribute.MARGIN_BOTTOM, CSS.Attribute.MARGIN_LEFT, CSS.Attribute.MARGIN_RIGHT, CSS.Attribute.MARGIN_TOP };
	private static Attribute[] fontAttributes = new Attribute[] { CSS.Attribute.FONT, CSS.Attribute.FONT_FAMILY, CSS.Attribute.FONT_SIZE, CSS.Attribute.FONT_STYLE, CSS.Attribute.FONT_VARIANT, CSS.Attribute.FONT_WEIGHT };

	public boolean hasBorder(AttributeSet s)
	{
		return hasAttributes(s, borderAttributes);
	}

	public boolean hasMargin(AttributeSet s)
	{
		return hasAttributes(s, marginAttributes);
	}

	public boolean hasFont(AttributeSet s)
	{
		return hasAttributes(s, fontAttributes);
	}

	private boolean hasAttributes(AttributeSet s, Attribute[] attrs)
	{
		for (Attribute a : attrs)
		{
			if (s.getAttribute(a) != null) return true;
		}
		return false;
	}

	/**
	 * Cache for style classes per lookupname.
	 */
	private Map<Pair<String, String>, String[]> styleClassesCache;

	/**
	 * Get cached style classes for lookupName and formStyleClass
	 * @param lookupName
	 * @param formStyleClass
	 * @return
	 */
	public String[] getCachedStyleClasses(String lookupName, String formStyleClass)
	{
		if (styleClassesCache == null)
		{
			return null;
		}
		return styleClassesCache.get(new Pair<String, String>(lookupName, formStyleClass));
	}

	public void setCachedStyleClasses(String lookupName, String formStyleClass, String[] styleClasses)
	{
		if (styleClassesCache == null)
		{
			styleClassesCache = new HashMap<Pair<String, String>, String[]>();
		}
		styleClassesCache.put(new Pair<String, String>(lookupName, formStyleClass), styleClasses);
	}
}