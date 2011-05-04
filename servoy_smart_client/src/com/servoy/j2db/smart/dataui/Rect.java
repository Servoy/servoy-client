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
package com.servoy.j2db.smart.dataui;


import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;

import javax.swing.JComponent;
import javax.swing.UIDefaults;
import javax.swing.UIManager;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.persistence.RectShape;
import com.servoy.j2db.scripting.IReturnedTypesProvider;
import com.servoy.j2db.ui.IDataRenderer;
import com.servoy.j2db.ui.IRect;
import com.servoy.j2db.ui.IScriptBaseMethods;
import com.servoy.j2db.util.ComponentFactoryHelper;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.Utils;

/**
 * Rectangular shape
 * @author jblok
 */
public class Rect extends JComponent implements IReturnedTypesProvider, IRect
{
	protected IApplication application;
	protected int radius = 16;
	protected int lineWidth = 0;
	protected int type;

	public Rect()
	{
	} // for scripting purposes

	public Rect(IApplication application, int type)
	{
		super();
		this.application = application;
		this.type = type;
		if (type != RectShape.BORDER_PANEL)
		{
			setBackground(Color.white);
			setForeground(Color.black);
		}
	}

	/**
	 * Fix for bad font rendering (bad kerning == strange spacing) in java 1.5 see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5097047
	 */
	@Override
	public FontMetrics getFontMetrics(Font font)
	{
		if (application != null)//getFontMetrics can be called in the constructor super call before application is assigned
		{
			boolean isPrinting = Utils.getAsBoolean(application.getRuntimeProperties().get("isPrinting"));
			if (isPrinting)
			{
				Graphics g = (Graphics)application.getRuntimeProperties().get("printGraphics");
				if (g != null)
				{
					return g.getFontMetrics(font);
				}
			}
		}
		return super.getFontMetrics(font);
	}

	@Override
	public void paint(Graphics g)
	{
		boolean _filled = isOpaque();
		Color _fillColor = getBackground();
		Color _lineColor = getForeground();
		int halfLW = lineWidth / 2; //Math.round(((float)lineWidth)/2f);
		Color defaultColor = null;
		if (_filled)
		{
			Container parent = getParent();
			if (_fillColor == null || (parent != null && _fillColor != null && _fillColor == parent.getBackground()))
			{
				// if the fill color is actually from parent use the default bg color of the laf
				UIDefaults uiDefaults = UIManager.getLookAndFeel().getDefaults();
				if (uiDefaults != null)
				{
					defaultColor = uiDefaults.getColor("Label.background");
				}
				if (defaultColor != null)
				{
					_fillColor = defaultColor;
				}
			}
		}

		int _x = 0;//getX();
		int _y = 0;//getY();
		int _h = getHeight();
		int _w = getWidth();

		Stroke oldStroke = ((Graphics2D)g).getStroke();
		((Graphics2D)g).setStroke(new BasicStroke(lineWidth));

		if (type == RectShape.BORDER_PANEL)
		{
			if (_filled && _fillColor != null)
			{
				g.setColor(_fillColor);
				g.fillRect(_x, _y, getWidth(), getHeight());
			}
		}
		else if (type == RectShape.ROUNDED_RECTANGLE)
		{
			if (_filled && _fillColor != null)
			{
				g.setColor(_fillColor);
				g.fillRoundRect(_x + halfLW, _y + halfLW, _w - (lineWidth), _h - (lineWidth)/* _x, _y, _w, _h */, radius, radius);
			}
			if (lineWidth > 0 && _lineColor != null)
			{
				g.setColor(_lineColor);
				g.drawRoundRect(_x + halfLW, _y + halfLW, _w - (lineWidth), _h - (lineWidth), radius, radius);
			}
		}
		else if (type == RectShape.OVAL)
		{
			if (_filled && _fillColor != null)
			{
				g.setColor(_fillColor);
				g.fillOval(_x, _y, _w, _h);
			}
			if (lineWidth > 0 && _lineColor != null)
			{
				g.setColor(_lineColor);
				g.drawOval(_x + halfLW, _y + halfLW, _w - (lineWidth), _h - (lineWidth));
			}
		}
		else
		{
			if (_filled && _fillColor != null)
			{
				g.setColor(_fillColor);
				g.fillRect(_x + halfLW, _y + halfLW, _w - (lineWidth), _h - (lineWidth));
			}
			if (lineWidth > 0 && _lineColor != null)
			{
				g.setColor(_lineColor);
//				if (!getDashed()) 
				{
					g.drawRect(_x + halfLW, _y + halfLW, _w - (lineWidth), _h - (lineWidth));
				}
//				else 
//				{
//					int phase = 0;
//					phase = drawDashedLine(g, phase, _x, _y, _x + _w, _y);
//					phase = drawDashedLine(g, phase, _x + _w, _y, _x + _w, _y + _h);
//					phase = drawDashedLine(g, phase, _x + _w, _y + _h, _x, _y + _h);
//					phase = drawDashedLine(g, phase, _x, _y + _h, _x, _y);
//				}
			}
//			super.paint(g);
		}
		((Graphics2D)g).setStroke(oldStroke);
		super.paint(g);
	}

	/**
	 * @see com.servoy.j2db.dataui.IRect#getLineWidth()
	 */
	public int getLineWidth()
	{
		return lineWidth;
	}

	/**
	 * @see com.servoy.j2db.dataui.IRect#getRadius()
	 */
	public int getRadius()
	{
		return radius;
	}

	/**
	 * @see com.servoy.j2db.dataui.IRect#setLineWidth(int)
	 */
	public void setLineWidth(int lineWidth)
	{
		this.lineWidth = lineWidth;
	}

	/**
	 * @see com.servoy.j2db.dataui.IRect#setRadius(int)
	 */
	public void setRadius(int radius)
	{
		this.radius = radius;
	}


	/*
	 * bgcolor---------------------------------------------------
	 */
	@Override
	public Color getBackground()
	{
		Color color = super.getBackground();
		if (color == null)
		{
			Container con = getParent();
			if (con != null)
			{
				color = con.getBackground();
			}
			// laurian: i think laf default bg color should be returned in this case
			if (color == null) color = Color.white;
		}
		return color;
	}

	public String js_getBgcolor()
	{
		return PersistHelper.createColorString(getBackground());
	}

	public void js_setBgcolor(String clr)
	{
		setBackground(PersistHelper.createColor(clr));
	}


	/*
	 * fgcolor---------------------------------------------------
	 */
	@Override
	public Color getForeground()
	{
		Color color = super.getForeground();
		if (color == null)
		{
			color = Color.black;
		}
		return color;
	}

	public String js_getFgcolor()
	{
		return PersistHelper.createColorString(getForeground());
	}

	public void js_setFgcolor(String clr)
	{
		setForeground(PersistHelper.createColor(clr));
	}


	public void js_setBorder(String spec)
	{
		setBorder(ComponentFactoryHelper.createBorder(spec));
	}

	public String js_getBorder()
	{
		return ComponentFactoryHelper.createBorderString(getBorder());
	}

	/*
	 * visible---------------------------------------------------
	 */
	public boolean js_isVisible()
	{
		return isVisible();
	}

	public void js_setVisible(boolean b)
	{
		setVisible(b);
	}

	public void setComponentVisible(boolean b_visible)
	{
		setVisible(b_visible);
	}


	/*
	 * opaque---------------------------------------------------
	 */
	public boolean js_isTransparent()
	{
		return !isOpaque();
	}

	public void js_setTransparent(boolean b)
	{
		setOpaque(!b);
		repaint();
	}


	/*
	 * enabled---------------------------------------------------
	 */
	public boolean js_isEnabled()
	{
		return isEnabled();
	}

	public void js_setEnabled(boolean b)
	{
		setComponentEnabled(b);
	}

	public void setComponentEnabled(boolean enabled)
	{
		setEnabled(enabled);
	}


	/*
	 * tooltip---------------------------------------------------
	 */
	public void js_setToolTipText(String txt)
	{
		setToolTipText(txt);
	}

	public String js_getToolTipText()
	{
		return getToolTipText();
	}


	/*
	 * location---------------------------------------------------
	 */
	public int js_getLocationX()
	{
		return getLocation().x;
	}

	public int js_getLocationY()
	{
		return getLocation().y;
	}

	/**
	 * @see com.servoy.j2db.ui.IScriptBaseMethods#js_getAbsoluteFormLocationY()
	 */
	public int js_getAbsoluteFormLocationY()
	{
		Container parent = getParent();
		while ((parent != null) && !(parent instanceof IDataRenderer))
		{
			parent = parent.getParent();
		}
		if (parent != null)
		{
			return ((IDataRenderer)parent).getYOffset() + getLocation().y;
		}
		return getLocation().y;
	}

	public void js_setLocation(int x, int y)
	{
		setLocation(x, y);
	}

	/*
	 * client properties for ui---------------------------------------------------
	 */

	public void js_putClientProperty(Object key, Object value)
	{
		putClientProperty(key, value);
	}

	public Object js_getClientProperty(Object key)
	{
		return getClientProperty(key);
	}


	/*
	 * size---------------------------------------------------
	 */
	public void js_setSize(int x, int y)
	{
		setSize(x, y);
	}

	public int js_getWidth()
	{
		return getSize().width;
	}

	public int js_getHeight()
	{
		return getSize().height;
	}


	/*
	 * jsmethods---------------------------------------------------
	 */
	public String js_getElementType()
	{
		return IScriptBaseMethods.RECTANGLE;
	}

	public void js_setFont(String spec)
	{
		setFont(PersistHelper.createFont(spec));
	}

	public String js_getFont()
	{
		return PersistHelper.createFontString(getFont());
	}

	public String js_getName()
	{
		String jsName = getName();
		if (jsName != null && jsName.startsWith(ComponentFactory.WEB_ID_PREFIX)) jsName = null;
		return jsName;
	}

	public Class< ? >[] getAllReturnedTypes()
	{
		return null;
	}

	public String getId()
	{
		return (String)getClientProperty("Id");
	}
}
