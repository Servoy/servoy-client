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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;
import java.util.StringTokenizer;

import javax.swing.border.AbstractBorder;

import com.servoy.j2db.util.Utils;

/**
 * Special border for advanced bordering 
 * @author jblok
 */
public class SpecialMatteBorder extends AbstractBorder
{
	private static final int line_join = BasicStroke.JOIN_BEVEL;//JOIN_MITER;
	private static final int line_cap = BasicStroke.CAP_BUTT;
	private final float top, left, bottom, right;
	private final Color topColor, leftColor, bottomColor, rightColor;
	private float roundingRadius = 0f;
	private float[] dashPattern;

	public SpecialMatteBorder(float top, float left, float bottom, float right, Color topColor, Color leftColor, Color bottomColor, Color rightColor)
	{
		this.top = top;
		this.left = left;
		this.bottom = bottom;
		this.right = right;

		this.topColor = topColor;
		this.leftColor = leftColor;
		this.bottomColor = bottomColor;
		this.rightColor = rightColor;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof SpecialMatteBorder)
		{
			SpecialMatteBorder other = (SpecialMatteBorder)obj;
			boolean retval = (other.top == top && other.left == left && other.bottom == bottom && other.right == right);
			if (retval)
			{
				if (topColor != null)
				{
					retval = topColor.equals(other.topColor);
				}
				else
				{
					retval = topColor == null && other.topColor == null;
				}
			}
			if (retval)
			{
				if (leftColor != null)
				{
					retval = leftColor.equals(other.leftColor);
				}
				else
				{
					retval = leftColor == null && other.leftColor == null;
				}
			}
			if (retval)
			{
				if (bottomColor != null)
				{
					retval = bottomColor.equals(other.bottomColor);
				}
				else
				{
					retval = bottomColor == null && other.bottomColor == null;
				}
			}
			if (retval)
			{
				if (rightColor != null)
				{
					retval = rightColor.equals(other.rightColor);
				}
				else
				{
					retval = rightColor == null && other.rightColor == null;
				}
			}
			return retval;
		}
		else
		{
			return false;
		}
	}

	/**
	 * Paints the matte border.
	 */
	@Override
	public void paintBorder(Component c, Graphics g, int x, int y, int width, int height)
	{
		Color oldColor = g.getColor();
		Stroke oldStroke = ((Graphics2D)g).getStroke();
		g.translate(x, y);
		try
		{
			if (roundingRadius > 0)
			{
				((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				float lineWidth = top;
				if (dashPattern == null)
				{
					((Graphics2D)g).setStroke(new BasicStroke(lineWidth, line_cap, line_join));
				}
				else
				{
					((Graphics2D)g).setStroke(new BasicStroke(lineWidth, line_cap, line_join, 1f, dashPattern, 0f));
				}

				float halfLW = lineWidth / 2f;
				Shape shape = null;
				if (roundingRadius == 100f)
				{
					shape = new Ellipse2D.Float(halfLW, halfLW, width - lineWidth, height - lineWidth);

				}
				else
				{
					shape = new RoundRectangle2D.Float(halfLW, halfLW, width - lineWidth, height - lineWidth, roundingRadius, roundingRadius);
				}

				g.setColor(topColor);
				((Graphics2D)g).draw(shape);
			}
			// Does not work correctly on macos java 1.5.0_07, left and right lines are shifted 1 pixel to the right......
//			else if (top == left && top == bottom && top == right &&
//					 topColor != null && topColor.equals(leftColor) && topColor.equals(bottomColor) && topColor.equals(rightColor))
//			{
//				if (top >= 0f)
//				{
//					float lineWidth = top;
//					if (dashPattern == null)
//					{
//						((Graphics2D)g).setStroke(new BasicStroke(lineWidth,line_cap,line_join));
//					}
//					else
//					{
//						((Graphics2D)g).setStroke(new BasicStroke(lineWidth,line_cap,line_join,1f,dashPattern,0f));
//					}
//	
//					g.setColor(topColor);
//					float halfLW = lineWidth/2f;
//					Rectangle2D.Float rect = new Rectangle2D.Float(halfLW, halfLW, width-lineWidth, height-lineWidth);
//					((Graphics2D)g).draw(rect);
//				}
//			}
			else
			{
				((Graphics2D)g).setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);

				if (top > 0f && topColor != null)
				{
					if (dashPattern == null && top == 1f)
					{
						((Graphics2D)g).setStroke(new BasicStroke(top, line_cap, line_join));
						g.setColor(topColor);
						g.drawLine(0, 0, width - 1, 0);
					}
					else
					{
						float lineWidth = top;
						float halfLW = lineWidth / 2f;
						if (dashPattern == null)
						{
							((Graphics2D)g).setStroke(new BasicStroke(lineWidth, line_cap, line_join));
						}
						else
						{
							((Graphics2D)g).setStroke(new BasicStroke(lineWidth, line_cap, line_join, 1f, dashPattern, 0f));
						}
						g.setColor(topColor);
						Line2D.Float line = new Line2D.Float(0f, halfLW, width, halfLW);
						((Graphics2D)g).draw(line);
					}
				}
				if (left > 0f && leftColor != null)
				{
					if (dashPattern == null && left == 1f)
					{
						((Graphics2D)g).setStroke(new BasicStroke(left, line_cap, line_join));
						g.setColor(leftColor);
						g.drawLine(0, (int)top, 0, height - 1);
					}
					else
					{
						float lineWidth = left;
						float halfLW = lineWidth / 2f;
						if (dashPattern == null)
						{
							((Graphics2D)g).setStroke(new BasicStroke(lineWidth, line_cap, line_join));
						}
						else
						{
							((Graphics2D)g).setStroke(new BasicStroke(lineWidth, line_cap, line_join, 1f, dashPattern, 0f));
						}
						g.setColor(leftColor);
						Line2D.Float line = new Line2D.Float(halfLW, top, halfLW, height - bottom);
						((Graphics2D)g).draw(line);
					}
				}
				if (bottom > 0f && bottomColor != null)
				{
					if (dashPattern == null && bottom == 1f)
					{
						((Graphics2D)g).setStroke(new BasicStroke(bottom, line_cap, line_join));
						g.setColor(bottomColor);
						g.drawLine(0, height - 1, width - 1, height - 1);
					}
					else
					{
						float lineWidth = bottom;
						float halfLW = lineWidth / 2f;
						if (dashPattern == null)
						{
							((Graphics2D)g).setStroke(new BasicStroke(lineWidth, line_cap, line_join));
						}
						else
						{
							((Graphics2D)g).setStroke(new BasicStroke(lineWidth, line_cap, line_join, 1f, dashPattern, 0f));
						}
						g.setColor(bottomColor);
						Line2D.Float line = new Line2D.Float(0f, height - halfLW, width, height - halfLW);
						((Graphics2D)g).draw(line);
					}
				}
				if (right > 0f && rightColor != null)
				{
					if (dashPattern == null && right == 1f)
					{
						((Graphics2D)g).setStroke(new BasicStroke(right, line_cap, line_join));
						g.setColor(rightColor);
						g.drawLine(width - 1, (int)top, width - 1, height - 1);
					}
					else
					{
						float lineWidth = right;
						float halfLW = lineWidth / 2f;
						if (dashPattern == null)
						{
							((Graphics2D)g).setStroke(new BasicStroke(lineWidth, line_cap, line_join));
						}
						else
						{
							((Graphics2D)g).setStroke(new BasicStroke(lineWidth, line_cap, line_join, 1f, dashPattern, 0f));
						}
						g.setColor(rightColor);
						Line2D.Float line = new Line2D.Float(width - halfLW, top, width - halfLW, height - bottom);
						((Graphics2D)g).draw(line);
					}
				}
			}
		}
		finally
		{
			g.translate(-x, -y);
			g.setColor(oldColor);
			((Graphics2D)g).setStroke(oldStroke);
		}
	}

	public Color getTopColor()
	{
		return topColor;
	}

	public Color getLeftColor()
	{
		return leftColor;
	}

	public Color getBottomColor()
	{
		return bottomColor;
	}

	public Color getRightColor()
	{
		return rightColor;
	}

	public float getBottom()
	{
		return bottom;
	}

	public float getLeft()
	{
		return left;
	}

	public float getRight()
	{
		return right;
	}

	public float getTop()
	{
		return top;
	}

	@Override
	public Insets getBorderInsets(Component c, Insets insets)
	{
		return applySpaceToInsets(insets, c);
	}

	@Override
	public Insets getBorderInsets(Component c)
	{
		Insets retval = new Insets(0, 0, 0, 0);
		applySpaceToInsets(retval, c);
		return retval;
	}

	private Insets applySpaceToInsets(Insets i, Component c)
	{
		// if c is null we are probably in wc
		float halfRadius = (c != null) ? roundingRadius / 8f : 0f;
		i.top = (int)Math.ceil(top + halfRadius);
		i.left = (int)Math.ceil(left + halfRadius);
		i.bottom = (int)Math.ceil(bottom + halfRadius);
		i.right = (int)Math.ceil(right + halfRadius);
		return i;
	}

	@Override
	public boolean isBorderOpaque()
	{
		return (roundingRadius != 0f);
	}

	/**
	 * @return
	 */
	public float getRoundingRadius()
	{
		return roundingRadius;
	}

	/**
	 * @param f
	 */
	public void setRoundingRadius(float f)
	{
		roundingRadius = f;
	}

	/**
	 * @return
	 */
	public float[] getDashPattern()
	{
		return dashPattern;
	}

	/**
	 * @param a
	 */
	public void setDashPattern(float[] a)
	{
		dashPattern = a;
	}

	public static String createDashString(float[] fs)
	{
		StringBuffer sb = new StringBuffer();
		if (fs != null)
		{
			for (int i = 0; i < fs.length; i++)
			{
				sb.append(fs[i]);
				if (i < fs.length - 1) sb.append(";"); //$NON-NLS-1$

			}
		}
		return sb.toString();
	}

	public static float[] createDash(String dash)
	{
		if (dash == null || dash.trim().length() == 0) return null;

		dash = Utils.stringReplace(dash, ",", ";");//safety  //$NON-NLS-1$//$NON-NLS-2$
		StringTokenizer tk = new StringTokenizer(dash, ";"); //$NON-NLS-1$
		float[] retval = new float[tk.countTokens()];
		int i = 0;
		boolean atLeastOneTokenNotZero = false;
		while (tk.hasMoreTokens())
		{
			retval[i] = Utils.getAsFloat(tk.nextToken());
			if (retval[i] != 0) atLeastOneTokenNotZero = true;
			i++;
		}
		return atLeastOneTokenNotZero ? retval : null;
	}
}
