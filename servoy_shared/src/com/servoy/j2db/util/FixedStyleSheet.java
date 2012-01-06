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
import java.util.Enumeration;
import java.util.StringTokenizer;

import javax.swing.BorderFactory;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.text.AttributeSet;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.CSS;
import javax.swing.text.html.CSS.Attribute;
import javax.swing.text.html.StyleSheet;

import org.xhtmlrenderer.css.constants.CSSName;

import com.servoy.j2db.util.gui.CustomBevelBorder;
import com.servoy.j2db.util.gui.CustomEtchedBorder;
import com.servoy.j2db.util.gui.RoundedBorder;
import com.servoy.j2db.util.gui.SpecialMatteBorder;

/**
 * @author jblok
 * @deprecated only keep for backwards compatibility of plugins
 * Enhanced/fixed subclass for proper behaviour.
 */
@Deprecated
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
				else if (Arrays.asList(IStyleSheet.BORDER_STYLES).contains(tok))
				{
					borderStyle = tok;
				}
				// If "transparent" then transparent.
				else if (tok.equals(IStyleSheet.COLOR_TRANSPARENT))
				{
					borderColor = IStyleSheet.COLOR_TRANSPARENT;
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

		if (borderStyle == null)
		{
			// TODO this should be fixed when we compile against java 7.
			// java 7 expands the complete border:
			Enumeration< ? > attributes = a.getAttributeNames();
			while (attributes.hasMoreElements())
			{
				Object element = attributes.nextElement();
				if (element != null && element.toString().startsWith("border-top-style"))
				{
					borderStyle = a.getAttribute(element);
					break;
				}
			}
		}
		if (borderStyle == null && (borderColor != null || top >= 0 || right >= 0 || bottom >= 0 || left >= 0))
		{
			borderStyle = IStyleSheet.BORDER_STYLE_SOLID;
		}
		if (borderStyle != null)
		{
			Color[] colors = getBorderColor(borderColor, a);
			String bstyle = borderStyle.toString();
			if (a.isDefined(CSSName.BORDER_TOP_LEFT_RADIUS.toString()))
			{
				top = makeSizeSave(top);
				right = makeSizeSave(right);
				bottom = makeSizeSave(bottom);
				left = makeSizeSave(left);
				colors = expandColors(colors);
				b = new RoundedBorder(top, left, bottom, right, colors[0], colors[3], colors[2], colors[1]);

				float[] radius = new float[8];
				parseCornerBorderRadius(a, radius, 0, CSSName.BORDER_TOP_LEFT_RADIUS.toString());
				parseCornerBorderRadius(a, radius, 1, CSSName.BORDER_TOP_RIGHT_RADIUS.toString());
				parseCornerBorderRadius(a, radius, 2, CSSName.BORDER_BOTTOM_RIGHT_RADIUS.toString());
				parseCornerBorderRadius(a, radius, 3, CSSName.BORDER_BOTTOM_LEFT_RADIUS.toString());
				((RoundedBorder)b).setRoundingRadius(radius);
				((RoundedBorder)b).setBorderStyles(new String[] { (String)a.getAttribute(CSSName.BORDER_TOP_STYLE.toString()), (String)a.getAttribute(CSSName.BORDER_LEFT_STYLE.toString()), (String)a.getAttribute(CSSName.BORDER_BOTTOM_STYLE.toString()), (String)a.getAttribute(CSSName.BORDER_RIGHT_STYLE.toString()) });
			}
			else if (bstyle.equals(IStyleSheet.BORDER_STYLE_INSET) || bstyle.equals(IStyleSheet.BORDER_STYLE_OUTSET))
			{
				int style = BevelBorder.LOWERED;
				if (bstyle.equals(IStyleSheet.BORDER_STYLE_OUTSET)) style = BevelBorder.RAISED;

				Insets customBorderInsets = null;
				if (top != -1.0 || right != -1.0 || bottom != -1.0 || left != -1.0)
				{
					top = makeSizeSave(top);
					right = makeSizeSave(right);
					bottom = makeSizeSave(bottom);
					left = makeSizeSave(left);
					customBorderInsets = new Insets((int)top, (int)left, (int)bottom, (int)right);
				}

				if (colors != null && colors.length > 0)
				{
					if (colors.length == 1)
					{
						// this tries to do the same thing as the web does..
						b = customBorderInsets != null ? new CustomBevelBorder(style, colors[0].brighter().brighter(), colors[0].darker().darker(),
							customBorderInsets) : new BevelBorder(style, colors[0].brighter().brighter(), colors[0].darker().darker());
					}
					if (colors.length == 2)
					{
						b = customBorderInsets != null ? new CustomBevelBorder(style, colors[0], colors[1], customBorderInsets) : new BevelBorder(style,
							colors[0], colors[1]);
					}
					else if (colors.length > 3)
					{
						b = customBorderInsets != null ? new CustomBevelBorder(style, colors[0], colors[1], colors[2], colors[3], customBorderInsets)
							: new BevelBorder(style, colors[0], colors[1], colors[2], colors[3]);
					}
				}
				else
				{
					b = customBorderInsets != null ? new CustomBevelBorder(style, customBorderInsets) : new BevelBorder(style);
				}
			}
			else if (bstyle.equals(IStyleSheet.BORDER_STYLE_NONE))
			{
				b = BorderFactory.createEmptyBorder();
			}
			else if (bstyle.equals(IStyleSheet.BORDER_STYLE_DOUBLE))
			{
				top = makeSizeSave(top);
				right = makeSizeSave(right);
				bottom = makeSizeSave(bottom);
				left = makeSizeSave(left);

				b = BorderFactory.createEmptyBorder((int)top, (int)left, (int)bottom, (int)right);
			}
			else if (bstyle.equals(IStyleSheet.BORDER_STYLE_GROOVE) || bstyle.equals(IStyleSheet.BORDER_STYLE_RIDGE))
			{
				int style = EtchedBorder.LOWERED;
				if (bstyle.equals(IStyleSheet.BORDER_STYLE_RIDGE)) style = EtchedBorder.RAISED;

				Insets customBorderInsets = null;
				if (top != -1.0 || right != -1.0 || bottom != -1.0 || left != -1.0)
				{
					top = makeSizeSave(top);
					right = makeSizeSave(right);
					bottom = makeSizeSave(bottom);
					left = makeSizeSave(left);
					customBorderInsets = new Insets((int)top, (int)left, (int)bottom, (int)right);
				}

				if (colors != null && colors.length > 0)
				{
					if (colors.length == 1)
					{
						// this tries to do the same thing as the web does..
						b = customBorderInsets != null ? new CustomEtchedBorder(style, colors[0].brighter().brighter(), colors[0].darker().darker(),
							customBorderInsets) : new EtchedBorder(style, colors[0].brighter().brighter(), colors[0].darker().darker());
					}
					if (colors.length == 2)
					{
						b = customBorderInsets != null ? new CustomEtchedBorder(style, colors[0], colors[1], customBorderInsets) : new EtchedBorder(style,
							colors[0], colors[1]);
					}
				}
				else
				{
					b = customBorderInsets != null ? new CustomEtchedBorder(style, customBorderInsets) : new EtchedBorder(style);
				}
			}
			else if (bstyle.equals(IStyleSheet.BORDER_STYLE_SOLID) || bstyle.equals(IStyleSheet.BORDER_STYLE_DOTTED) ||
				bstyle.equals(IStyleSheet.BORDER_STYLE_DASHED))
			{
//				int bw = (int) getLength(CSS.Attribute.BORDER_WIDTH, a);

//				int colorCount = (colors == null ? 0 : colors.length);
				colors = expandColors(colors);
//				Object obj = a.getAttribute(CSS.Attribute.BORDER_WIDTH);
				if (bstyle.equals(IStyleSheet.BORDER_STYLE_SOLID))
				{
					if (borderColor != null && IStyleSheet.COLOR_TRANSPARENT.equals(borderColor.toString()))
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
				else if (bstyle.equals(IStyleSheet.BORDER_STYLE_DASHED))
				{
					if (top <= 0 && left <= 0 && bottom <= 0 && right <= 0)
					{
						top = left = bottom = right = 1;
					}
					b = new SpecialMatteBorder(top, left, bottom, right, colors[0], colors[3], colors[2], colors[1]);
					((SpecialMatteBorder)b).setDashPattern(new float[] { 3, 3 });
				}
				else if (bstyle.equals(IStyleSheet.BORDER_STYLE_DOTTED))
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

	private void parseCornerBorderRadius(AttributeSet a, float[] values, int index, String attributeName)
	{
		String value = a.getAttribute(attributeName).toString().trim();
		StringTokenizer tokenizer = new StringTokenizer(value, " ");
		if (tokenizer.countTokens() == 2)
		{
			values[index] = getLength(tokenizer.nextElement());
			values[index + 4] = getLength(tokenizer.nextElement());
		}
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
	@SuppressWarnings("nls")
	private Color[] getBorderColor(Object obj, AttributeSet a)
	{
		if (obj != null)
		{
			ArrayList<Color> colors = new ArrayList<Color>();
			String val = obj.toString();
			StringTokenizer tk = new StringTokenizer(val, " ");
			while (tk.hasMoreTokens())
			{
				String token = tk.nextToken();
				colors.add(PersistHelper.createColor(token));
			}
			return colors.toArray(new Color[colors.size()]);
		}
		else
		{
			// TODO this should be fixed when we compile against java 7.
			Color top = null;
			Color right = null;
			Color bottom = null;
			Color left = null;
			// java 7 expands the complete border:
			Enumeration< ? > attributes = a.getAttributeNames();
			while (attributes.hasMoreElements())
			{
				Object element = attributes.nextElement();
				if (element.toString().equals("border-top-color"))
				{
					top = PersistHelper.createColor(a.getAttribute(element).toString());
				}
				else if (element.toString().equals("border-right-color"))
				{
					right = PersistHelper.createColor(a.getAttribute(element).toString());
				}
				else if (element.toString().equals("border-bottom-color"))
				{
					bottom = PersistHelper.createColor(a.getAttribute(element).toString());
				}
				else if (element.toString().equals("border-left-color"))
				{
					left = PersistHelper.createColor(a.getAttribute(element).toString());
				}
			}
			if (top != null && right != null && bottom != null && left != null)
			{
				return new Color[] { top, right, bottom, left };
			}
			else
			{
				ArrayList<Color> colors = new ArrayList<Color>();
				if (top != null) colors.add(top);
				if (right != null) colors.add(right);
				if (bottom != null) colors.add(bottom);
				if (left != null) colors.add(left);
				if (colors.size() > 0) return colors.toArray(new Color[colors.size()]);
			}
		}
		return null;
	}

	/**
	 * Get the length/width in px/pt
	 * @param key
	 * @param a
	 * @return -1 if undefined
	 */
	public float getLength(Object obj)
	{
		if (obj != null)
		{
			String val = obj.toString();
			if (val.endsWith("px") || val.endsWith("pt")) //$NON-NLS-1$ //$NON-NLS-2$
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
		return getHAlign((String)obj);
	}

	public int getHAlign(String val)
	{
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
		return -1;
	}

	public int getVAlign(AttributeSet a)
	{
		Object obj = a.getAttribute(CSS.Attribute.VERTICAL_ALIGN);
		return getVAlign((String)obj);
	}

	public int getVAlign(String val)
	{
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
		return -1;
	}

	@Override
	public void addCSSAttribute(MutableAttributeSet attr, CSS.Attribute key, String value)
	{
		String newValue = expandColorValue(key, value);
		if (key == CSS.Attribute.BACKGROUND_COLOR && IStyleSheet.COLOR_TRANSPARENT.equalsIgnoreCase(newValue)) attr.addAttribute(key, newValue);
		else super.addCSSAttribute(attr, key, newValue);
	}

	/*
	 * If the attribute is a color property then check whether it is in sort format or not. Short format means #abc which should expand to #aabbcc. If in short
	 * format then expand it.
	 */
	protected String expandColorValue(CSS.Attribute key, String value)
	{
		if (key != null && key.toString().matches(".*color.*") && value.startsWith("#") && value.length() == 4) //$NON-NLS-1$//$NON-NLS-2$
		{
			return new StringBuilder("#").append(value.charAt(1)).append(value.charAt(1)).append(value.charAt(2)).append(value.charAt(2)).append( //$NON-NLS-1$
				value.charAt(3)).append(value.charAt(3)).toString();
		}
		else return value;
	}

	public boolean hasBorder(AttributeSet s)
	{
		return hasAttributes(s, ServoyStyleSheet.borderAttributes);
	}

	public boolean hasMargin(AttributeSet s)
	{
		return hasAttributes(s, ServoyStyleSheet.marginAttributes);
	}

	public boolean hasFont(AttributeSet s)
	{
		return hasAttributes(s, ServoyStyleSheet.fontAttributes);
	}

	private boolean hasAttributes(AttributeSet s, Attribute[] attrs)
	{
		for (Attribute a : attrs)
		{
			if (s.getAttribute(a) != null) return true;
		}
		return false;
	}

	@Override
	public Color getBackground(AttributeSet a)
	{
		Object sbackground_color = a.getAttribute(CSS.Attribute.BACKGROUND_COLOR);
		if (sbackground_color != null)
		{
			if (!IStyleSheet.COLOR_TRANSPARENT.equals(sbackground_color.toString()))
			{
				return super.getBackground(a);
			}
		}
		return null;
	}
}