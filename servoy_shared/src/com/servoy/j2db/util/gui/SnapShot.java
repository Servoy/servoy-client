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
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;

import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;

import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ImageLoader;
import com.servoy.j2db.util.JpegEncoder;

public class SnapShot
{
	/**
	 * Create an jpg image blob from a live component (or existing image blob to scale it).
	 * @param window parent window to use for offscreen drawings
	 * @param obj the imageblob-bytearray,ImageIcon,Image or Component
	 * @param width desired width (-1 = leave as is)
	 * @param height desired height (-1 = leave as is)
	 * @return the jpg image blob
	 */
	public static byte[] createJPGImage(Window window, Object obj, int width, int height)
	{
		ImageIcon imageIcon = null;
		Component comp = null;
		if (obj instanceof Component)
		{
			comp = (Component)obj;
			final Component finalComp = comp;
			final BufferedImage buf = new BufferedImage(comp.getWidth(), comp.getHeight(), BufferedImage.TYPE_INT_RGB);
			Runnable run = new Runnable()
			{
				public void run()
				{
					Graphics g2 = buf.getGraphics();
					finalComp.printAll(g2);
				}
			};
			if (SwingUtilities.isEventDispatchThread())
			{
				run.run();
			}
			else
			{
				try
				{
					SwingUtilities.invokeAndWait(run);
				}
				catch (Exception e)
				{
					Debug.error(e);
				}
			}
			imageIcon = new ImageIcon(buf);
//			if (width == -1) width = comp.getWidth();
//			if (height == -1) height = comp.getHeight();
		}
		else if (obj instanceof byte[])
		{
//			imageIcon = new ImageIcon(ImageLoader.getBufferedImage((byte[])obj,width,height,true));
			imageIcon = ImageLoader.getIcon((byte[])obj, width, height, true);
		}
		else if (obj instanceof Image)
		{
			imageIcon = new ImageIcon((Image)obj);
		}
		else if (obj instanceof ImageIcon)
		{
			imageIcon = (ImageIcon)obj;
		}
		if (imageIcon != null)
		{
			if (comp == null)
			{
				comp = window;
//				imageIcon.setImageObserver(comp);
			}
			imageIcon = ImageLoader.resizeImageIcon(imageIcon, width, height, true);

//			int orgWidth = image.getWidth(comp);
//			int orgHeight = image.getHeight(comp);
//			double max = 1;
//			if (width == -1 && height > 0)
//			{
//				max = Math.ceil((float)orgHeight)/Math.abs(height);
//			}
//			if (height == -1 && width > 0) 
//			{
//				max = Math.ceil((float)orgWidth)/Math.abs(width);
//			}
//			if (max != 1)
//			{
//				image = image.getScaledInstance((int)(orgWidth/max), (int)(orgHeight/max), Image.SCALE_SMOOTH);
//			}

			if (imageIcon.getImage() instanceof RenderedImage)
			{
				return ImageLoader.getByteArray("image/jpeg", (RenderedImage)imageIcon.getImage());
			}
			else
			{
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				JpegEncoder encoder = new JpegEncoder(comp, imageIcon.getImage(), 100, baos);
				encoder.compress();
				return baos.toByteArray();
			}
		}
		return null;
	}
}
