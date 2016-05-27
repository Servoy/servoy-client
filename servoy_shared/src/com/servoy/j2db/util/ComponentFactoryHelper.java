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
import java.awt.Component;
import java.awt.Font;
import java.awt.Insets;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.swing.BorderFactory;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.border.TitledBorder;

import org.xhtmlrenderer.css.constants.CSSName;

import com.servoy.j2db.IScriptExecuter;
import com.servoy.j2db.J2DBGlobals;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.ui.RenderEventExecutor;
import com.servoy.j2db.util.gui.RoundedBorder;
import com.servoy.j2db.util.gui.SpecialMatteBorder;

/**
 * Helper class.
 *
 * @author jblok
 */
public class ComponentFactoryHelper
{
	public static final String LINE_BORDER = "LineBorder"; //$NON-NLS-1$
	public static final String COMPOUND_BORDER = "CompoundBorder"; //$NON-NLS-1$
	public static final String BEVEL_BORDER = "BevelBorder"; //$NON-NLS-1$
	public static final String ETCHED_BORDER = "EtchedBorder"; //$NON-NLS-1$
	public static final String TITLED_BORDER = "TitledBorder"; //$NON-NLS-1$
	public static final String MATTE_BORDER = "MatteBorder"; //$NON-NLS-1$
	public static final String SPECIAL_MATTE_BORDER = "SpecialMatteBorder"; //$NON-NLS-1$
	public static final String ROUNDED_BORDER = "RoundedBorder"; //$NON-NLS-1$
	public static final String EMPTY_BORDER = "EmptyBorder"; //$NON-NLS-1$

	public static String createBorderString(Object currentBorder)
	{
		String retval = null;
		if (currentBorder != null)
		{
			if (currentBorder instanceof CompoundBorder)
			{
				Border oborder = ((CompoundBorder)currentBorder).getOutsideBorder();
				Border iborder = ((CompoundBorder)currentBorder).getInsideBorder();
				retval = COMPOUND_BORDER + ","; //$NON-NLS-1$
				retval += ";" + createBorderString(oborder); //$NON-NLS-1$
				retval += ";" + createBorderString(iborder) + ";"; //$NON-NLS-1$ //$NON-NLS-2$
			}
			else if (currentBorder instanceof BevelBorder)
			{
				BevelBorder border = (BevelBorder)currentBorder;
				int type = border.getBevelType();
				retval = BEVEL_BORDER + "," + type; //$NON-NLS-1$
				if (border.getHighlightInnerColor() != null || border.getHighlightOuterColor() != null || border.getShadowInnerColor() != null ||
					border.getShadowOuterColor() != null)
				{
					retval += "," + PersistHelper.createColorString(border.getHighlightOuterColor()); //$NON-NLS-1$
					retval += "," + PersistHelper.createColorString(border.getHighlightInnerColor()); //$NON-NLS-1$
					retval += "," + PersistHelper.createColorString(border.getShadowOuterColor()); //$NON-NLS-1$
					retval += "," + PersistHelper.createColorString(border.getShadowInnerColor()); //$NON-NLS-1$
				}
			}
			else if (currentBorder instanceof EtchedBorder)
			{
				EtchedBorder border = (EtchedBorder)currentBorder;
				int type = border.getEtchType();
				Color hi = border.getHighlightColor();
				Color sh = border.getShadowColor();
				retval = ETCHED_BORDER + "," + type + "," + PersistHelper.createColorString(hi) + "," + PersistHelper.createColorString(sh); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
			else if (currentBorder instanceof LineBorder)
			{
				LineBorder border = (LineBorder)currentBorder;
				int thick = border.getThickness();
				Color lineColor = border.getLineColor();
				retval = LINE_BORDER + "," + thick + "," + PersistHelper.createColorString(lineColor); //$NON-NLS-1$ //$NON-NLS-2$
			}
			else if (currentBorder instanceof TitledBorder)
			{
				TitledBorder border = (TitledBorder)currentBorder;
				String s = border.getTitle();
				s = Utils.stringReplace(s, ",", "|"); //escape //$NON-NLS-1$ //$NON-NLS-2$
				Font f = border.getTitleFont();
				Color c = border.getTitleColor();
				retval = TITLED_BORDER + "," + s; //$NON-NLS-1$

				int justification = border.getTitleJustification();
				int position = border.getTitlePosition();
				if (justification != 0 || position != 0 || f != null || c != null)
				{
					retval += "," + justification + "," + position; //$NON-NLS-1$ //$NON-NLS-2$
					if (f != null)
					{
						retval += "," + PersistHelper.createFontString(f); //$NON-NLS-1$
						if (c != null)
						{
							retval += "," + PersistHelper.createColorString(c); //$NON-NLS-1$
						}
					}
				}
			}
			else if (currentBorder instanceof SpecialMatteBorder)
			{
				SpecialMatteBorder border = (SpecialMatteBorder)currentBorder;
				retval = ((border instanceof RoundedBorder) ? ROUNDED_BORDER : SPECIAL_MATTE_BORDER) + "," + border.getTop() + "," + border.getRight() + "," + //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
					border.getBottom() + "," + border.getLeft(); //$NON-NLS-1$
				retval += "," + PersistHelper.createColorString(border.getTopColor()); //$NON-NLS-1$
				retval += "," + PersistHelper.createColorString(border.getRightColor()); //$NON-NLS-1$
				retval += "," + PersistHelper.createColorString(border.getBottomColor()); //$NON-NLS-1$
				retval += "," + PersistHelper.createColorString(border.getLeftColor()); //$NON-NLS-1$
				if (border instanceof RoundedBorder)
				{
					retval += "," + ((RoundedBorder)border).getRoundingRadiusString(); //$NON-NLS-1$
					retval += "," + ((RoundedBorder)border).getBorderStylesString(); //$NON-NLS-1$
				}
				else
				{
					retval += "," + border.getRoundingRadius(); //$NON-NLS-1$
					retval += "," + SpecialMatteBorder.createDashString(border.getDashPattern()); //$NON-NLS-1$
				}
			}
			else if (currentBorder instanceof MatteBorder)
			{
				MatteBorder border = (MatteBorder)currentBorder;
				Insets i = ComponentFactoryHelper.getBorderInsetsForNoComponent(border);
				Color lineColor = border.getMatteColor();
				retval = MATTE_BORDER + "," + i.top + "," + i.right + "," + i.bottom + "," + i.left + "," + PersistHelper.createColorString(lineColor); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
			}
			else if (currentBorder instanceof EmptyBorder)
			{
				EmptyBorder border = (EmptyBorder)currentBorder;
				Insets i = ComponentFactoryHelper.getBorderInsetsForNoComponent(border);
				retval = EMPTY_BORDER + "," + i.top + "," + i.right + "," + i.bottom + "," + i.left; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			}
			else
			{
				retval = "<select>"; //$NON-NLS-1$
			}
		}
		return retval;
	}

	public static Border createBorder(String s)
	{
		return createBorder(s, false);
	}

	public static Border createBorder(String s, boolean design)
	{
		Border currentBorder = null;
		if (s != null)
		{
			StringTokenizer tk = new StringTokenizer(s, ","); //$NON-NLS-1$
			if (tk.hasMoreTokens())
			{
				try
				{
					String type = tk.nextToken();
					if (type.equals(COMPOUND_BORDER))
					{
						StringTokenizer tk2 = new StringTokenizer(s, ";"); //$NON-NLS-1$
						tk2.nextToken();//skip 'CompoundBorder,' token
						String s_oborder = tk2.nextToken();
						Border oborder = createBorder(s_oborder);
						String s_iborder = tk2.nextToken();
						Border iborder = createBorder(s_iborder);
						currentBorder = BorderFactory.createCompoundBorder(oborder, iborder);
					}
					else if (type.equals(EMPTY_BORDER))
					{
						int top = Utils.getAsInteger(tk.nextToken());
						int right = Utils.getAsInteger(tk.nextToken());
						int bottom = Utils.getAsInteger(tk.nextToken());
						int left = Utils.getAsInteger(tk.nextToken());
						currentBorder = BorderFactory.createEmptyBorder(top, left, bottom, right);
					}
					else if (type.equals(BEVEL_BORDER))
					{
						int beveltype = Utils.getAsInteger(tk.nextToken());
						if (tk.hasMoreTokens())
						{
							Color highlightO = PersistHelper.createColor(tk.nextToken());
							Color highlightI = PersistHelper.createColor(tk.nextToken());
							Color shadowO = PersistHelper.createColor(tk.nextToken());
							Color shadowI = PersistHelper.createColor(tk.nextToken());

							currentBorder = BorderFactory.createBevelBorder(beveltype, highlightO, highlightI, shadowO, shadowI);
						}
						else
						{
							currentBorder = BorderFactory.createBevelBorder(beveltype);
						}
					}
					else if (type.equals(ETCHED_BORDER))
					{
						int beveltype = Utils.getAsInteger(tk.nextToken());
						Color highlight = PersistHelper.createColor(tk.nextToken());
						Color shadow = PersistHelper.createColor(tk.nextToken());
						currentBorder = BorderFactory.createEtchedBorder(beveltype, highlight, shadow);
					}
					else if (type.equals(LINE_BORDER))
					{
						int thick = Utils.getAsInteger(tk.nextToken());
						currentBorder = BorderFactory.createLineBorder(PersistHelper.createColor(tk.nextToken()), thick);
					}
					else if (type.equals(TITLED_BORDER))
					{
						String title = tk.nextToken();
						title = Utils.stringReplace(title, "|", ",");//unescape //$NON-NLS-1$ //$NON-NLS-2$
						int justification = 0;
						int position = 0;
						Font font = null;
						Color color = null;
						if (tk.hasMoreTokens())
						{
							justification = Utils.getAsInteger(tk.nextToken());
							position = Utils.getAsInteger(tk.nextToken());
							if (tk.hasMoreTokens())
							{
								font = PersistHelper.createFont(tk.nextToken() + "," + tk.nextToken() + "," + tk.nextToken());//we know a font has 3 parameters ALSO separated with ',' //$NON-NLS-1$ //$NON-NLS-2$
								if (tk.hasMoreTokens())
								{
									color = PersistHelper.createColor(tk.nextToken());
								}
							}
						}

						if (design)
						{
							currentBorder = BorderFactory.createTitledBorder(title);
						}
						else
						{
							currentBorder = BorderFactory.createTitledBorder(J2DBGlobals.getServiceProvider() != null
								? J2DBGlobals.getServiceProvider().getI18NMessageIfPrefixed(title) : title);
						}
						((TitledBorder)currentBorder).setTitleJustification(justification);
						((TitledBorder)currentBorder).setTitlePosition(position);
						if (font != null) ((TitledBorder)currentBorder).setTitleFont(font);
						if (color != null) ((TitledBorder)currentBorder).setTitleColor(color);

//						if (font == null)
//						{
//							currentBorder = BorderFactory.createTitledBorder(null,title,justification,position);
//						}
//						else
//						{
//							if (font != null && color != null)
//							{
//								currentBorder = BorderFactory.createTitledBorder(null,title,justification,position,font,color);
//							}
//							else
//							{
//								currentBorder = BorderFactory.createTitledBorder(null,title,justification,position,font);
//							}
//						}
					}
					else if (type.equals(MATTE_BORDER))
					{
						int top = Utils.getAsInteger(tk.nextToken());
						int right = Utils.getAsInteger(tk.nextToken());
						int bottom = Utils.getAsInteger(tk.nextToken());
						int left = Utils.getAsInteger(tk.nextToken());
						Color color = Color.black;
						if (tk.hasMoreElements()) color = PersistHelper.createColor(tk.nextToken());
						currentBorder = BorderFactory.createMatteBorder(top, left, bottom, right, color);
					}
					else if (type.equals(SPECIAL_MATTE_BORDER) || type.equals(ROUNDED_BORDER))
					{
						float top = Utils.getAsFloat(tk.nextToken());
						float right = Utils.getAsFloat(tk.nextToken());
						float bottom = Utils.getAsFloat(tk.nextToken());
						float left = Utils.getAsFloat(tk.nextToken());
						Color topColor = PersistHelper.createColor(tk.nextToken());
						Color rightColor = PersistHelper.createColor(tk.nextToken());
						Color bottomColor = PersistHelper.createColor(tk.nextToken());
						Color leftColor = PersistHelper.createColor(tk.nextToken());
						if (type.equals(SPECIAL_MATTE_BORDER))
						{
							currentBorder = new SpecialMatteBorder(top, left, bottom, right, topColor, leftColor, bottomColor, rightColor);
						}
						else
						{
							currentBorder = new RoundedBorder(top, left, bottom, right, topColor, leftColor, bottomColor, rightColor);
						}
						if (tk.hasMoreTokens())
						{
							if (type.equals(SPECIAL_MATTE_BORDER))
							{
								((SpecialMatteBorder)currentBorder).setRoundingRadius(Utils.getAsFloat(tk.nextToken()));
							}
							else
							{
								((RoundedBorder)currentBorder).setRoundingRadius(tk.nextToken());
							}
						}
						if (tk.hasMoreTokens())
						{
							if (type.equals(SPECIAL_MATTE_BORDER))
							{
								((SpecialMatteBorder)currentBorder).setDashPattern(SpecialMatteBorder.createDash(tk.nextToken()));
							}
							else
							{
								((RoundedBorder)currentBorder).setBorderStyles(tk.nextToken());
							}
						}
					}
					else
					{
						currentBorder = BorderFactory.createEtchedBorder();
					}
				}
				catch (Exception ex)
				{
					Debug.error(ex);
					return null;
				}
			}
			else
			{
				currentBorder = BorderFactory.createEtchedBorder();
			}
		}
		return currentBorder;
	}

	public static Insets createBorderCSSProperties(String s, Properties style)
	{
		if (s == null)
		{
			// no border specified
			return null;
		}
		else
		{
			StringTokenizer tk = new StringTokenizer(s, ","); //$NON-NLS-1$
			if (tk.hasMoreTokens())
			{
				try
				{
					String type = tk.nextToken();
					if (type.equals(COMPOUND_BORDER))
					{
						StringTokenizer tk2 = new StringTokenizer(s, ";"); //$NON-NLS-1$
						tk2.nextToken();//skip 'CompoundBorder,' token
						String s_oborder = tk2.nextToken();
						return createBorderCSSProperties(s_oborder, style);
					}
					else if (type.equals(EMPTY_BORDER))
					{
						int top = Utils.getAsInteger(tk.nextToken());
						int right = Utils.getAsInteger(tk.nextToken());
						int bottom = Utils.getAsInteger(tk.nextToken());
						int left = Utils.getAsInteger(tk.nextToken());
						if (top != 0 && right != 0 && bottom != 0 && left != 0)
						{
							StringBuffer pad = new StringBuffer();
							pad.append(top);
							pad.append("px "); //$NON-NLS-1$
							pad.append(right);
							pad.append("px "); //$NON-NLS-1$
							pad.append(bottom);
							pad.append("px "); //$NON-NLS-1$
							pad.append(left);
							pad.append("px"); //$NON-NLS-1$
							style.setProperty("padding", pad.toString()); //$NON-NLS-1$
						}
						style.setProperty("border-style", "none"); //$NON-NLS-1$ //$NON-NLS-2$
						return new Insets(top, left, bottom, right);
					}
					else if (type.equals(BEVEL_BORDER) || type.equals(ETCHED_BORDER))
					{
						int beveltype = Utils.getAsInteger(tk.nextToken());
						if (tk.hasMoreTokens())
						{
							Color highlightO = null;
							Color highlightI = null;
							Color shadowO = null;
							Color shadowI = null;
							if (type.equals(BEVEL_BORDER))
							{
								highlightO = PersistHelper.createColor(tk.nextToken());
								highlightI = PersistHelper.createColor(tk.nextToken());
								shadowO = PersistHelper.createColor(tk.nextToken());
								shadowI = PersistHelper.createColor(tk.nextToken());
							}
							else
							{
								highlightO = PersistHelper.createColor(tk.nextToken());
								highlightI = highlightO;
								shadowO = PersistHelper.createColor(tk.nextToken());
								shadowI = shadowO;
							}
							if (beveltype == BevelBorder.LOWERED)
							{
								if (PersistHelper.createColorString(shadowO) != null)
								{
									StringBuffer pad = new StringBuffer();
									pad.append(PersistHelper.createColorString(shadowO));
									pad.append(' ');
									pad.append(PersistHelper.createColorString(highlightI));
									pad.append(' ');
									pad.append(PersistHelper.createColorString(highlightO));
									pad.append(' ');
									pad.append(PersistHelper.createColorString(shadowI));
									style.setProperty("border-color", pad.toString()); //$NON-NLS-1$
								}
								if (type.equals(BEVEL_BORDER))
								{
									style.setProperty("border-style", "inset"); //$NON-NLS-1$ //$NON-NLS-2$
								}
								else
								{
									style.setProperty("border-style", "groove"); //$NON-NLS-1$ //$NON-NLS-2$
								}
							}
							else
							{
								if (PersistHelper.createColorString(highlightO) != null)
								{
									StringBuffer pad = new StringBuffer();
									pad.append(PersistHelper.createColorString(highlightO));
									if (PersistHelper.createColorString(shadowO) != null)
									{
										pad.append(' ');
										pad.append(PersistHelper.createColorString(shadowI));
										pad.append(' ');
										pad.append(PersistHelper.createColorString(shadowO));
										pad.append(' ');
										pad.append(PersistHelper.createColorString(highlightI));
									}
									style.setProperty("border-color", pad.toString()); //$NON-NLS-1$
								}
								if (type.equals(BEVEL_BORDER))
								{
									style.setProperty("border-style", "outset"); //$NON-NLS-1$ //$NON-NLS-2$
								}
								else
								{
									style.setProperty("border-style", "ridge"); //$NON-NLS-1$ //$NON-NLS-2$
								}
							}
							return null;//TODO waht are the insets?
						}
						else
						{
							style.setProperty("border-style", (beveltype == BevelBorder.LOWERED ? "inset" : "outset")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							return null;//TODO waht are the insets?
						}
					}
					else if (type.equals(LINE_BORDER))
					{
						int thick = Utils.getAsInteger(tk.nextToken());
						style.setProperty("border-style", "solid"); //$NON-NLS-1$ //$NON-NLS-2$
						style.setProperty("border-width", thick + "px"); //$NON-NLS-1$ //$NON-NLS-2$
						style.setProperty("border-color", tk.nextToken()); //$NON-NLS-1$
						return new Insets(thick, thick, thick, thick);
					}
					else if (type.equals(TITLED_BORDER))
					{
						// ignore here
						return null;
					}
					else if (type.equals(MATTE_BORDER))
					{
						int top = Utils.getAsInteger(tk.nextToken());
						int right = Utils.getAsInteger(tk.nextToken());
						int bottom = Utils.getAsInteger(tk.nextToken());
						int left = Utils.getAsInteger(tk.nextToken());
						Color c = Color.black;
						String colorString = null;
						if (tk.hasMoreElements())
						{
							colorString = tk.nextToken();
							c = PersistHelper.createColor(colorString);
						}
						style.setProperty("border-style", "solid"); //$NON-NLS-1$ //$NON-NLS-2$
						StringBuffer pad = new StringBuffer();
						pad.append(top);
						pad.append("px "); //$NON-NLS-1$
						pad.append(right);
						pad.append("px "); //$NON-NLS-1$
						pad.append(bottom);
						pad.append("px "); //$NON-NLS-1$
						pad.append(left);
						pad.append("px"); //$NON-NLS-1$
						style.setProperty("border-width", pad.toString()); //$NON-NLS-1$
						style.setProperty("border-color", c != null ? PersistHelper.createColorString(c) : colorString); //$NON-NLS-1$
						return new Insets(top, left, bottom, right);
					}
					else if (type.equals(SPECIAL_MATTE_BORDER) || type.equals(ROUNDED_BORDER))
					{
						float top = Utils.getAsFloat(tk.nextToken());
						float right = Utils.getAsFloat(tk.nextToken());
						float bottom = Utils.getAsFloat(tk.nextToken());
						float left = Utils.getAsFloat(tk.nextToken());

						StringBuffer tmp = new StringBuffer();
						tmp.append(Math.round(top));
						tmp.append("px "); //$NON-NLS-1$
						tmp.append(Math.round(right));
						tmp.append("px "); //$NON-NLS-1$
						tmp.append(Math.round(bottom));
						tmp.append("px "); //$NON-NLS-1$
						tmp.append(Math.round(left));
						tmp.append("px"); //$NON-NLS-1$
						style.setProperty("border-width", tmp.toString()); //$NON-NLS-1$

						String topColor = PersistHelper.createColorString(PersistHelper.createColor(tk.nextToken()));
						String rightColor = PersistHelper.createColorString(PersistHelper.createColor(tk.nextToken()));
						String bottomColor = PersistHelper.createColorString(PersistHelper.createColor(tk.nextToken()));
						String leftColor = PersistHelper.createColorString(PersistHelper.createColor(tk.nextToken()));

//						style.setProperty(CSSName.BORDER_TOP_COLOR.toString(), topColor);
//						style.setProperty(CSSName.BORDER_RIGHT_COLOR.toString(), rightColor);
//						style.setProperty(CSSName.BORDER_BOTTOM_COLOR.toString(), bottomColor);
//						style.setProperty(CSSName.BORDER_LEFT_COLOR.toString(), leftColor);

						tmp.setLength(0);
						tmp.append(topColor);
						tmp.append(" "); //$NON-NLS-1$
						tmp.append(rightColor);
						tmp.append(" "); //$NON-NLS-1$
						tmp.append(bottomColor);
						tmp.append(" "); //$NON-NLS-1$
						tmp.append(leftColor);
						tmp.append(" "); //$NON-NLS-1$
						style.setProperty(CSSName.BORDER_COLOR_SHORTHAND.toString(), tmp.toString());

						style.setProperty("border-style", "solid"); //$NON-NLS-1$ //$NON-NLS-2$
						if (tk.hasMoreTokens())
						{
							String roundedBorder = tk.nextToken();
							String[] styles = new String[4];
							int index = 0;
							StringTokenizer roundedTokenizer = new StringTokenizer(roundedBorder, ";"); //$NON-NLS-1$
							while (roundedTokenizer.hasMoreTokens())
							{
								int width = Utils.getAsInteger(roundedTokenizer.nextToken());
								styles[index] = (styles[index] != null ? (styles[index] + " " + width + "px") : (width + "px")); //$NON-NLS-1$
								index = (index + 1) % 4;
							}
							style.setProperty(CSSName.BORDER_TOP_LEFT_RADIUS.toString(), styles[0]);
							style.setProperty(CSSName.BORDER_TOP_RIGHT_RADIUS.toString(), styles[1] != null ? styles[1] : styles[0]);
							style.setProperty(CSSName.BORDER_BOTTOM_RIGHT_RADIUS.toString(), styles[2] != null ? styles[2] : styles[0]);
							style.setProperty(CSSName.BORDER_BOTTOM_LEFT_RADIUS.toString(), styles[3] != null ? styles[3] : styles[0]);
						}
						if (tk.hasMoreTokens())
						{
							String borderStyle = tk.nextToken().trim();
							if (borderStyle.length() > 0)
							{
								if (type.equals(SPECIAL_MATTE_BORDER))
								{
									style.setProperty("border-style", borderStyle.equals("1.0;1.0") ? "dotted" : "dashed"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
								}
								else
								{
									style.remove("border-style"); //$NON-NLS-1$
									String[] styles = RoundedBorder.createBorderStyles(borderStyle);
									style.setProperty("border-top-style", styles[0]); //$NON-NLS-1$
									style.setProperty("border-left-style", styles[1]); //$NON-NLS-1$
									style.setProperty("border-bottom-style", styles[2]); //$NON-NLS-1$
									style.setProperty("border-right-style", styles[3]); //$NON-NLS-1$
								}
							}
						}
						return new Insets(Math.round(top), Math.round(left), Math.round(bottom), Math.round(right));
					}
					else
					{
						return null;
					}
				}
				catch (Exception ex)
				{
					Debug.error(ex);
					return null;
				}
			}
			else
			{
				return null;
			}
		}
	}

	public static void addPortalOnRenderCallback(Portal portal, RenderEventExecutor renderEventExecutor, IPersist obj, IScriptExecuter se)
	{
		int onRenderMethodID = 0;
		AbstractBase onRenderPersist = null;
		if (obj instanceof Field)
		{
			onRenderMethodID = ((Field)obj).getOnRenderMethodID();
			onRenderPersist = ((Field)obj);
		}
		else if (obj instanceof GraphicalComponent)
		{
			onRenderMethodID = ((GraphicalComponent)obj).getOnRenderMethodID();
			onRenderPersist = ((GraphicalComponent)obj);
		}
		if (onRenderMethodID <= 0)
		{
			onRenderMethodID = portal.getOnRenderMethodID();
			onRenderPersist = portal;
		}
		if (onRenderMethodID > 0) renderEventExecutor.setRenderCallback(Integer.toString(onRenderMethodID),
			Utils.parseJSExpressions(onRenderPersist.getFlattenedMethodArguments("onRenderMethodID"))); //$NON-NLS-1$
		else renderEventExecutor.setRenderCallback(null, null);

		renderEventExecutor.setRenderScriptExecuter(se);
	}

	public static Insets getBorderInsetsForNoComponent(Border border)
	{
		// in java 7 calling getBorderInsets on TitleBorder with a null component throws NPE,
		// so let call that with a dummy component
		if (border instanceof TitledBorder)
		{
			return border.getBorderInsets(new Component()
			{
			});
		}

		return border.getBorderInsets(null);
	}

	public static int getTitledBorderHeight(Border border)
	{
		if (border instanceof TitledBorder)
		{
			int fontSize = ((TitledBorder)border).getTitleFont() != null ? ((TitledBorder)border).getTitleFont().getSize() : 11;
			return fontSize + 4; // add the legend height
		}
		return 0;
	}
}
