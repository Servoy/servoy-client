package com.servoy.j2db.util;

import java.net.URL;
import java.net.URLConnection;

import com.servoy.j2db.ISmartClientApplication;

/**
 * Interface that can be used for {@link ISmartClientApplication#addURLStreamHandler(String, java.net.URLStreamHandler)}
 * if the url streamhandler that you want to add has to work in the developer, you need to implement this interface.
 * 
 * @author jcompagner
 * @since 7.2
 */
public interface DeveloperURLStreamHandler
{
	/**
	 * @see "java.net.URLStreamHandler.openConnection"
	 */
	public URLConnection openConnection(URL u) throws java.io.IOException;
}