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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

/**
 * Helper class to get community ad
 * @author Jan Blok
 */
public class Ad
{
	//returns url,w,h,time_ms
	public static Object[] getAdInfo()
	{
		Object[] retval = new Object[4];
		try
		{
			URL url = new URL("https://www.servoy.com/client/ad"); //$NON-NLS-1$
			URLConnection urlConnection = url.openConnection();
			urlConnection.setConnectTimeout(5000);
			InputStream is = urlConnection.getInputStream();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String line = br.readLine();
			Utils.closeReader(br);
			String[] size_array = line.split(","); //$NON-NLS-1$
			retval[0] = url;
			retval[1] = Utils.findNumber(size_array[0]);
			retval[2] = Utils.findNumber(size_array[1]);
			retval[3] = Utils.findNumber(size_array[2]);
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return retval;
	}
}
