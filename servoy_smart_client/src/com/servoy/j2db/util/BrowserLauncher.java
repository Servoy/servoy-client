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

import java.io.IOException;

import edu.stanford.ejalbert.exception.BrowserLaunchingExecutionException;
import edu.stanford.ejalbert.exception.BrowserLaunchingInitializingException;
import edu.stanford.ejalbert.exception.UnsupportedOperatingSystemException;

/** This class is a wrapper around the BrowserLauncher2 library, see http://sourceforge.net/projects/browserlaunch2
 * 
 * BrowserLauncher2 is a continuation of the BrowserLauncher project, is a library that 
 * facilitatesopening a browser from a Java application and directing the browser to a 
 * supplied url. In most cases the browser opened will be the user's default browser.
 *  
 */

public class BrowserLauncher {
	
	static private edu.stanford.ejalbert.BrowserLauncher launcher = null;

	/**
	 * This class should be never be instantiated; this just ensures so.
	 */
	private BrowserLauncher() { }
	

	/**
	 * Attempts to open the default web browser to the given URL.
	 * @param url The URL to open
	 * @throws IOException If the web browser could not be located or does not run
	 */
	public static void openURL(String url) throws IOException {

		// TODO maybe we should strip the file:/// from the url if it is a file url.
		// For example Windows Picture and Fax viewer can't handle those urls.
		try {
			synchronized (BrowserLauncher.class) {
				if (launcher == null) {
					launcher = new edu.stanford.ejalbert.BrowserLauncher(null);
				}
			}			
			launcher.openURLinBrowser(url);
		} catch (BrowserLaunchingInitializingException e) {
			throw new IOException(e.getMessage());
		} catch (BrowserLaunchingExecutionException e) {
			throw new IOException(e.getMessage());
		} catch (UnsupportedOperatingSystemException e) {
			throw new IOException(e.getMessage());
		}

	}
}
