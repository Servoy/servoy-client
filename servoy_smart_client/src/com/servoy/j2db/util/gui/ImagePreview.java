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


import java.awt.Dimension;
import java.awt.Graphics;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFileChooser;

import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.FileChooserUtils;
import com.servoy.j2db.util.ImageLoader;

public class ImagePreview extends JComponent implements PropertyChangeListener
{
	private ImageIcon thumbnail = null;

	public ImagePreview()
	{
		//does nothing
	}

	public ImagePreview(JFileChooser fc)
	{
		this();
		setPreferredSize(new Dimension(100, 100));
		setMaximumSize(new Dimension(100, 100));
		fc.addPropertyChangeListener(this);
	}

	public void propertyChange(PropertyChangeEvent e)
	{
		String prop = e.getPropertyName();
		if (prop.equals(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY))
		{
			File f = (File)e.getNewValue();
			if (f != null && isShowing())
			{
				try
				{
					thumbnail = ImageLoader.getIcon(FileChooserUtils.readFile(f), 90, 90, true);
				}
				catch (Exception e1)
				{
					Debug.error(e1);
				}
				repaint();
			}
		}
	}

	public void setThumbNailData(byte[] array)
	{
		if (array == null)
		{
			thumbnail = null;
		}
		else if (isShowing())
		{
			Dimension dim = getSize();
			Dimension imageSize = ImageLoader.getSize(array);
			dim.width = Math.min(dim.width - 10, imageSize.width);
			dim.height = Math.min(dim.height - 5, imageSize.height);
			thumbnail = ImageLoader.getIcon(array, dim.width, dim.height, true);
			repaint();
		}
	}

	@Override
	public void paint(Graphics g)
	{
//		if (thumbnail == null)
//		{
//			loadImage();
//		}
		if (thumbnail != null)
		{
			int x = getWidth() / 2 - thumbnail.getIconWidth() / 2;
			int y = getHeight() / 2 - thumbnail.getIconHeight() / 2;

			if (y < 0)
			{
				y = 0;
			}

			if (x < 5)
			{
				x = 5;
			}
			thumbnail.paintIcon(this, g, x, y);
		}
		super.paint(g);
	}
}
