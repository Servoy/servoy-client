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
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.util.ImageLoader;
import com.servoy.j2db.util.Utils;


public class MyImageIcon implements Icon
{
	private final JComponent comp;
	private byte[] iconArray;
	private Dimension succededScaledDimension;
	private ImageIcon original;
	private ImageIcon scaled;
	private final boolean reduce;
	private final boolean enlarge;
	private final boolean keepAspect;
	private final List images;
	private final IApplication application;

	public MyImageIcon(IApplication app, JComponent comp, ImageIcon icon, int mediaOption)
	{
		this(app, comp, mediaOption);

		this.original = icon;
		this.scaled = icon;
		this.iconWidth = icon.getIconWidth();
		this.iconHeight = icon.getIconHeight();
	}

	public MyImageIcon(IApplication app, JComponent comp, byte[] iconArray, int mediaOption)
	{
		this(app, comp, mediaOption);

		this.iconArray = iconArray;
		Dimension dim = ImageLoader.getSize(iconArray);
		this.iconWidth = dim.width;
		this.iconHeight = dim.height;
	}

	private MyImageIcon(IApplication app, JComponent comp, int mediaOption)
	{
		this.application = app;
		this.comp = comp;
		this.reduce = (mediaOption & 2) == 2;
		this.enlarge = (mediaOption & 4) == 4;
		this.keepAspect = (mediaOption & 8) == 8;

		images = new ArrayList(3);
	}

	public ImageIcon getScaledIcon(int width, int height)
	{
		if (width > 0 || height > 0)
		{
			return checkResize(this.scaled, new Dimension(width, height));
		}
		else
		{
			return scaled;
		}
	}

	// for developer... figicon painting the original imageicon is going wrong second time
	boolean generated = false;
	private double iconWidth;
	private double iconHeight;

	public void generatedScaledIcon()
	{
		if (!generated && scaled != null)
		{
			Image image = scaled.getImage().getScaledInstance(scaled.getIconWidth(), scaled.getIconHeight(), Image.SCALE_SMOOTH);
			scaled = new ImageIcon(image);
			generated = true;
		}
	}

	public void addImageIcon(MyImageIcon other)
	{
		images.add(other);
	}

	public boolean removeImageIcon(MyImageIcon other)
	{
		return images.remove(other);
	}


	private ImageIcon checkResize(ImageIcon prevScaled, Dimension size)
	{
		boolean resize = succededScaledDimension == null ? true : !succededScaledDimension.equals(size);
		double widthChange = 0;
		double heightChange = 0;

		double width = size.getWidth();
		double height = size.getHeight();

		// if it doesn't fit then resize == true
		// check then if we have to,or can, resize the original.
		if (resize)
		{
			succededScaledDimension = size;
			widthChange = iconWidth / width;
			heightChange = iconHeight / height;
			resize = widthChange > 1.01 || heightChange > 1.01 || widthChange < 0.99 || heightChange < 0.99;
			if (resize)
			{
				if (!(reduce && enlarge))
				{
					if (reduce)
					{
						if (widthChange <= 1 && heightChange <= 1)
						{
							// may only reduce, so no resize
							resize = false;
						}
						else if (widthChange > 1.01 && heightChange > 1.01)
						{
							if (keepAspect)
							{
								if (widthChange > heightChange)
								{
									width = (int)(iconWidth / widthChange);
									height = (int)(iconHeight / widthChange);
								}
								else
								{
									width = (int)(iconWidth / heightChange);
									height = (int)(iconHeight / heightChange);
								}
							}
						}
						else if (widthChange > 1.01)
						{
							width = (int)(iconWidth / widthChange);
							if (keepAspect)
							{
								height = (int)(iconHeight / widthChange);
							}
						}
						else if (heightChange > 1.01)
						{
							height = (int)(iconHeight / heightChange);
							if (keepAspect)
							{
								width = (int)(iconWidth / heightChange);
							}
						}
					}
					else if (enlarge)
					{
						if (widthChange >= 1 && heightChange >= 1)
						{
							// may only reduce, so no resize
							resize = false;
						}
						else if (widthChange < 0.99 && heightChange < 0.99)
						{
							if (keepAspect)
							{
								if (widthChange > heightChange)
								{
									width = (int)(iconWidth / widthChange);
									height = (int)(iconHeight / widthChange);
								}
								else
								{
									width = (int)(iconWidth / heightChange);
									height = (int)(iconHeight / heightChange);
								}
							}
						}
						else if (widthChange < 0.99)
						{
							width = (int)(iconWidth / widthChange);
							if (keepAspect)
							{
								height = (int)(iconHeight / widthChange);
							}
						}
						else if (heightChange < 0.99)
						{
							height = (int)(iconHeight / heightChange);
							if (keepAspect)
							{
								width = (int)(iconWidth / heightChange);
							}
						}
					}
					else
					{
						resize = false;
					}
				}
				else
				{
					if (keepAspect)
					{
						if (widthChange > heightChange)
						{
							width = (int)(iconWidth / widthChange);
							height = (int)(iconHeight / widthChange);
						}
						else
						{
							width = (int)(iconWidth / heightChange);
							height = (int)(iconHeight / heightChange);
						}
						// Recheck with aspect! Because component size can be bigger then the image with aspect
						if (prevScaled != null)
						{
							widthChange = prevScaled.getIconWidth() / width;
							heightChange = prevScaled.getIconHeight() / height;

							resize = !((widthChange > 0.99 && widthChange < 1.01) || (heightChange < 1.01 && heightChange > 0.99));
						}
					}
				}
			}
		}
		if (resize)
		{
			if (iconArray != null)
			{
				prevScaled = ImageLoader.getIcon(iconArray, width <= 0 ? 1 : (int)width, height <= 0 ? 1 : (int)height, keepAspect);
			}
			else if (original != null)
			{
				prevScaled = new ImageIcon(
					original.getImage().getScaledInstance(width <= 0 ? 1 : (int)width, height <= 0 ? 1 : (int)height, Image.SCALE_SMOOTH));
			}
			else
			{
				prevScaled = new ImageIcon(scaled.getImage().getScaledInstance(width <= 0 ? 1 : (int)width, height <= 0 ? 1 : (int)height, Image.SCALE_SMOOTH));
			}
		}
		if (prevScaled == null)
		{
			prevScaled = ImageLoader.getIcon(iconArray, -1, -1, true);
		}
		return prevScaled;
	}

	/**
	 * @see javax.swing.Icon#paintIcon(java.awt.Component, java.awt.Graphics, int, int)
	 */
	public void paintIcon(Component c, Graphics g, int x, int y)
	{
		if (scaled == null)
		{
			scaled = ImageLoader.getIcon(iconArray, c.getWidth(), c.getHeight(), keepAspect);
		}
		ImageIcon paint = null;
		boolean isPrinting = Utils.getAsBoolean(application.getRuntimeProperties().get("isPrinting"));
		if (isPrinting && iconArray != null)
		{
			paint = ImageLoader.getIcon(iconArray, -1, -1, keepAspect);
		}
		else
		{
			if (isPrinting && original != null)
			{
				paint = original;
			}
			else
			{
				paint = scaled;
			}
		}

		if (paint == null) return;

		AffineTransform at = ((Graphics2D)g).getTransform();
		AffineTransform save = (AffineTransform)at.clone();
		try
		{
			if (isPrinting && scaled != null)
			{
				double sx = (double)scaled.getIconWidth() / (double)paint.getIconWidth();
				double sy = (double)scaled.getIconHeight() / (double)paint.getIconHeight();
				x = (int)(x * ((double)paint.getIconWidth() / (double)scaled.getIconWidth()));
				y = (int)(y * ((double)paint.getIconHeight() / (double)scaled.getIconHeight()));
				at.scale(sx, sy);
				((Graphics2D)g).setTransform(at);
			}
			paint.paintIcon(c, g, x, y);
		}
		finally
		{
			((Graphics2D)g).setTransform(save);
		}
	}

	/**
	 * @see javax.swing.Icon#getIconWidth()
	 */
	public int getIconWidth()
	{
		Dimension dim = comp.getSize();
		if (dim.height == 0 || dim.width == 0)
		{
			return 1;
		}
		for (int i = 0; i < images.size(); i++)
		{
			((MyImageIcon)images.get(i)).getIconWidth();

		}
		scaled = checkResize(scaled, dim);
		if (scaled == null)
		{
			return 1;
		}
		return scaled.getIconWidth();
	}

	/**
	 * @see javax.swing.Icon#getIconHeight()
	 */
	public int getIconHeight()
	{
		Dimension dim = comp.getSize();
		if (dim.height == 0 || dim.width == 0)
		{
			return 1;
		}
		if (scaled == null)
		{
			return 1;
		}
		return scaled.getIconHeight();
	}

	/**
	 * @param newImage
	 * 
	 */
	public void flush()
	{
		if (scaled instanceof ImageIcon)
		{
			ImageIcon imageIcon = scaled;
			imageIcon.getImage().flush();
			imageIcon.setImageObserver(null);
		}
		if (original instanceof ImageIcon)
		{
			ImageIcon imageIcon = original;
			imageIcon.getImage().flush();
			imageIcon.setImageObserver(null);
		}
		for (int i = 0; i < images.size(); i++)
		{
			MyImageIcon icon = (MyImageIcon)images.get(i);
			icon.flush();
		}
	}

	public ImageIcon getOriginal()
	{
		return original;
	}

	public Image getImage()
	{
		if (scaled != null) return scaled.getImage();
		else if (original != null) return original.getImage();
		return null;
	}

}