/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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

import java.applet.Applet;
import java.applet.AppletContext;
import java.applet.AudioClip;
import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.servoy.j2db.IApplication;

/**
 * Applet context impl
 * @author jblok 
 */
public class AppletController implements AppletContext
{
	private final IApplication app;
	private Map<String, Applet> applets = new HashMap<String, Applet>();

	//		private Map imageCache = new Hashtable();

	public AppletController(IApplication app)
	{
		this.app = app;
	}

	public void add(Applet a)
	{
		applets.put(a.getName(), a);
	}

	public void clear()
	{
		applets = new HashMap<String, Applet>();
		//			imageCache = new Hashtable();
	}

	public AudioClip getAudioClip(URL url)
	{
		// We don't currently support audio clips in the Beans.instantiate
		// applet context
		return null;
	}

	public synchronized Image getImage(URL url)
	{
		//			Object o = imageCache.get(url);
		//			if (o != null) {
		//				return (Image) o;
		//			}
		//			try {
		//				o = url.getContent();
		//				if (o == null) {
		//					return null;
		//				}
		//				if (o instanceof Image) {
		//					imageCache.put(url, o);
		//					return (Image) o;
		//				}
		//				// Otherwise it must be an ImageProducer.
		//				Image img = target.createImage((java.awt.image.ImageProducer) o);
		//				imageCache.put(url, img);
		//				return img;
		//
		//			} catch (Exception ex) {
		return null;
		//			}
	}

	public Applet getApplet(String name)
	{
		return applets.get(name);
	}

	public Enumeration<Applet> getApplets()
	{
		return Collections.enumeration(applets.values());
	}

	public void showDocument(URL url)
	{
		app.showURL(url.toString(), null, null, 0, true);
	}

	public void showDocument(URL url, String target)
	{
		app.showURL(url.toString(), null, null, 0, true);
	}

	public void showStatus(String status)
	{
		app.setStatusText(status, null);
	}

	public void setStream(String key, InputStream stream) throws IOException
	{
		// We do nothing.
	}

	public InputStream getStream(String key)
	{
		// We do nothing.
		return null;
	}

	public Iterator<String> getStreamKeys()
	{
		// We do nothing.
		return null;
	}
}