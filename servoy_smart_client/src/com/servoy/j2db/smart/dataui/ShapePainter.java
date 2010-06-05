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
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Stroke;

import javax.swing.JComponent;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.Shape;
import com.servoy.j2db.ui.IComponent;
import com.servoy.j2db.util.Utils;

/**
 * @author 	jblok
 */
public class ShapePainter extends JComponent implements IComponent
{
	protected IApplication application;
	protected int SPLINE_THRESH = 2;
	protected Polygon _curve;
	
	protected int[] _xknots;
	protected int[] _yknots;
	protected int _nknots;
	
	protected int _npoints;
	protected int[] _xpoints;
	protected int[] _ypoints;

	protected int _juncX, _juncY;
	
	protected int type;
	protected int lineWidth;
	protected Polygon poly;
	protected boolean mustDoTranslate;
	
	public ShapePainter(IApplication application,int type,int lineWidth,Polygon p)
	{
		super();
		this.application = application;
		this.type = type;
		this.lineWidth = lineWidth;
		this.poly = new Polygon(p.xpoints,p.ypoints,p.npoints);
		mustDoTranslate = true;
		setBackground(Color.white);
		setForeground(Color.black);

	}
	
	/**
	 * Fix for bad font rendering (bad kerning == strange spacing) in java 1.5
	 * see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5097047
	 */
	public FontMetrics getFontMetrics(Font font)
	{
		if (application != null)//getFontMetrics can be called in the constructor super call before application is assigned
		{
			boolean isPrinting = Utils.getAsBoolean(application.getRuntimeProperties().get("isPrinting"));
			if (isPrinting)
			{
				Graphics g = (Graphics) application.getRuntimeProperties().get("printGraphics");
				if (g != null)
				{
					return g.getFontMetrics(font);
				}
			}
		}
		return super.getFontMetrics(font);
	}
	
	public void setLocation(int x ,int y)
	{
		super.setLocation(x-lineWidth,y-lineWidth);
		translate();
	}
	
	public void setBounds(int x ,int y,int w,int h)
	{
		super.setBounds(x-lineWidth,y-lineWidth,w+(2*lineWidth),h+(2*lineWidth));//add lineWidth otherwise clipping cuts in big lineWiths
		translate();
	}

	private void translate()
	{
		if (mustDoTranslate)
		{
			Rectangle r = poly.getBounds();
			
			poly.translate(-r.x, -r.y);
			poly.translate(3*lineWidth, 3*lineWidth);

			mustDoTranslate = false;
			if(type == Shape.SPLINE && poly.npoints != 0)
			{
				_npoints = poly.npoints;
				_xpoints = poly.xpoints;
				_ypoints = poly.ypoints;
	
				_xknots = new int[_npoints*4-1];
	
				_yknots =new int[_npoints*4-1];

				setSpline();
			}
		}
	}

	public void paint(Graphics g) 
	{
		boolean _filled = isOpaque();
		Color _fillColor = getBackground();
		Color _lineColor = getForeground();
		
		Stroke oldStroke = ((Graphics2D)g).getStroke();
		((Graphics2D)g).setStroke(new BasicStroke(lineWidth));
//		java.awt.Shape oldClip = g.getClip();
//		if (lineWidth > 1)
//		{
//			g.setClip(null); breaks parent clipping
//		}
		if (type == Shape.LINE)
		{
			if (lineWidth > 0  && _lineColor != null && poly.xpoints.length >1 && poly.ypoints.length > 1) 
			{
				g.setColor(_lineColor);
				g.drawLine(poly.xpoints[0],poly.ypoints[0],poly.xpoints[1],poly.ypoints[1]);
			}
		}
		else if(type == Shape.SPLINE)
		{
			if (_npoints == 2) drawStraight(g);
			else { drawCurve(g); }
		}
		else //if (type == Shape.POLYGON || type == Shape.INK || type == Shape.SPLINE)
		{
			if (poly.npoints > 0)
			{
				if (_filled  && _fillColor != null) 
				{
					g.setColor(_fillColor);
					g.fillPolygon(poly);
				}
				if (lineWidth > 0  && _lineColor != null) 
				{
					g.setColor(_lineColor);
					g.drawPolyline(poly.xpoints, poly.ypoints, poly.npoints);
				}
			}
		}
		((Graphics2D)g).setStroke(oldStroke);
//		g.setClip(oldClip);
	}

	// Draw a three-point spline using DeCasteljau algorithm
	protected void drawBezier(Graphics g,
							  int x1, int y1,
							  int x2, int y2,
							  int x3, int y3) 
	{
		int xa, ya, xb, yb, xc, yc, xp, yp;
		xa = ( x1 + x2 ) / 2;
		ya = ( y1 + y2 ) / 2;
		xc = ( x2 + x3 ) / 2;
		yc = ( y2 + y3 ) / 2;
		xb = ( xa + xc ) / 2;
		yb = ( ya + yc ) / 2;

		xp = ( x1 + xb ) / 2;
		yp = ( y1 + yb ) / 2;
		if ( Math.abs( xa - xp ) + Math.abs( ya - yp ) > SPLINE_THRESH )
			drawBezier( g, x1, y1, xa, ya, xb, yb );
		else 
		{
			g.drawLine( x1, y1, xb, yb );
			_curve.addPoint(xb, yb);
		}
		xp = ( x3 + xb ) / 2;
		yp = ( y3 + yb ) / 2;
		if ( Math.abs( xc - xp ) + Math.abs( yc - yp ) > SPLINE_THRESH )
			drawBezier( g, xb, yb, xc, yc, x3, y3 );
		else 
		{
			g.drawLine( xb, yb, x3, y3 );
			_curve.addPoint(x3, y3);
		}
	}


	protected void drawCurve(Graphics g) 
	{
		Color _fillColor = getBackground();
		Color _lineColor = getForeground();
		int nSegments = _npoints-2;
		_curve = new Polygon();
		g.setColor(_lineColor);
		for (int i=0; i<=nSegments-1; i++)	
		{
			drawBezier(g, _xknots[2*i],   _yknots[2*i],
						_xknots[2*i+1], _yknots[2*i+1],
						_xknots[2*i+2], _yknots[2*i+2]);
		}
		if (/*_filled*/ true) 
		{
			g.setColor(_fillColor);
			g.fillPolygon(_curve);	   // here the curve gets partially destroyed
			g.setColor(_lineColor);
			g.drawPolyline(_curve.xpoints, _curve.ypoints, _curve.npoints);
		}
	}


	protected void drawStraight(Graphics g) 
	{
		Color _lineColor = getForeground();
		g.setColor(_lineColor);
		g.drawLine(_xknots[0], _yknots[0], _xknots[1], _yknots[1]);
	}

	protected void setSpline() 
	{
		if (_npoints == 2) _nknots   = 2;
		else if (_npoints == 3) _nknots   = 3;
		_nknots = 2*_npoints-3;
		if (_xknots != null && _yknots != null)
		{
			if (_npoints>=4 && _nknots > 0) 
			{
				_xknots[0] = _xpoints[0];
				_yknots[0] = _ypoints[0];
				_xknots[_nknots-1] = _xpoints[_npoints-1];
				_yknots[_nknots-1] = _ypoints[_npoints-1];
				for (int i=0; i<=_npoints-4; i++) 
				{
					setJunctionPoint(_xpoints[i],   _ypoints[i],
									 _xpoints[i+1], _ypoints[i+1],
									 _xpoints[i+2], _ypoints[i+2]);
					_xknots[2*(i+1)] = _juncX;
					_yknots[2*(i+1)] = _juncY;
				}
				for(int i=1; i<=_npoints-2; i++) 
				{
					_xknots[2*i-1] = _xpoints[i];
					_yknots[2*i-1] = _ypoints[i];
				}
			}
			else if (_npoints<4) 
			{
				for (int i=0; i<_npoints; i++) 
				{
					_xknots[i] = _xpoints[i];
					_yknots[i] = _ypoints[i];
				}
			}
		}
	}

	protected void setJunctionPoint(int p1x, int p1y,
									int p2x, int p2y,
									int p3x, int p3y) 
	{
		double delta0 = dist(p1x, p1y, p2x, p2y);
		double delta1 = dist(p2x, p2y, p3x, p3y);
		double denom = delta0 + delta1;
		double _junc_t = 0;
		if (denom > 5) _junc_t = delta1 / denom;
		_juncX = (int)(_junc_t*p2x + (1-_junc_t)*p3x);
		_juncY = (int)(_junc_t*p2y + (1-_junc_t)*p3y);
	}

	private double dist(int x0, int y0, int x1, int y1) 
	{
		double dx, dy;
		dx = (double)(x0-x1);
		dy = (double)(y0-y1);
		return Math.sqrt(dx*dx+dy*dy);
	}

	public void setComponentEnabled(boolean enabled)
	{
		setEnabled(enabled);
	}
	public void setComponentVisible(boolean b_visible)
	{
		setVisible(b_visible);
	}
	public String getId() 
	{
		return (String)getClientProperty("Id");
	}
}
