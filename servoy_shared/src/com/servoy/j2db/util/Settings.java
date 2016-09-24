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


import java.awt.Component;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Rectangle;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.crypto.Cipher;
import javax.swing.JFrame;

import com.servoy.j2db.J2DBGlobals;
import com.servoy.j2db.LAFManager;

import net.jcip.annotations.ThreadSafe;

@ThreadSafe
@SuppressWarnings("nls")
public final class Settings extends SortedProperties
{
	public static final long serialVersionUID = 8213681985670137977L;

	public static final String FILE_NAME;
	private static final int MAXIMIZED_INVIZIBLE_BORDER_PIXELS = 4;
	private static final String CLIENT_LOCAL_FILE_NAME = "servoy_client.properties";

	public static final int INITIAL_CLIENT_WIDTH = 800;
	public static final int INITIAL_CLIENT_HEIGHT = 600;

	public static final String TRUSTED_REMOTE_PLUGINS = "servoy.application_server.trustedRemotePlugins";
	public static final String START_AS_TEAMPROVIDER_SETTING = "servoy.application_server.startRepositoryAsTeamProvider";
	public static final boolean START_AS_TEAMPROVIDER_DEFAULT = false;
	public static final String SMARTCLIENT_ENABLE_JAVAFX = "servoy.client.javafx"; //$NON-NLS-1$
	@Deprecated
	// do not persist global maintenance mode; when running clustered this could result in entering cluster-wide maintenance mode
	// unwillingly when some app. servers were already started and working for a while and you start another app. server
	public static final String START_GLOBAL_MAINTENANCE_MODE_SETTING = "servoy.application_server.global_maintenance_mode";
	public static final String SERVER_MAINTENANCE_MODE_SETTING = "servoy.application_server.maintenance_mode";
	public static final String ALLOW_CLIENT_REPOSITORY_ACCESS_SETTING = "servoy.application_server.allowClientRepositoryAccess"; //$NON-NLS-1$
	public static final boolean ALLOW_CLIENT_REPOSITORY_ACCESS_DEFAULT = false;
	public static final String LOG_CLIENT_STATS = "servoy.log.clientstats";
	public static final String WAIT_FOR_NATIVE_STARTUP = "waitForNativeStartup";
	public static final String RMI_CONNECTION_TIMEOUT = "rmi.connection.timeout";

	public static final String TRUST_DATA_AS_HTML_SETTING = "servoy.clientTrustDataAsHtml"; //$NON-NLS-1$

	private boolean loadedFromServer = false;
	private File file;

	static
	{
		String pFile = System.getProperty("property-file");
		if (pFile == null) pFile = "servoy.properties";
		FILE_NAME = pFile;
	}

	private final static Settings me = new Settings();

	private Settings()
	{
	}

	public void addPropertyChangeListener(PropertyChangeListener listener, String sProperty)
	{
		J2DBGlobals.addPropertyChangeListener(this, sProperty, listener);
	}

	public void removePropertyChangeListener(PropertyChangeListener listener, String sProperty)
	{
		J2DBGlobals.removePropertyChangeListener(this, sProperty, listener);
	}

	/**
	 * Load the config file from file or url(in case of webstart)
	 */
	public synchronized void loadFromServer(URL base) throws Exception
	{
		loadedFromServer = true;
		URL configfile = null;
		String profileName = System.getProperty("servoy.profilename"); //$NON-NLS-1$
		if (profileName != null)
		{
			configfile = new URL(base, FILE_NAME + "?profilename=" + profileName); //$NON-NLS-1$
		}
		else
		{
			configfile = new URL(base, FILE_NAME);
		}


		//load from local
		file = new File(System.getProperty("user.home"), J2DBGlobals.CLIENT_LOCAL_DIR + CLIENT_LOCAL_FILE_NAME); //$NON-NLS-1$
		if (file.exists())
		{
			FileInputStream fis = new FileInputStream(file);
			load(fis);
			fis.close();
		}

		String currentLnf = getProperty("selectedlnf"); //$NON-NLS-1$
		String currentNumberFormat = getProperty("locale.numberformat"); //$NON-NLS-1$
		String currentIntegerformat = getProperty("locale.integerformat"); //$NON-NLS-1$
		String currentDateFormat = getProperty("locale.dateformat"); //$NON-NLS-1$
		String currentUseSystemPrintDialog = getProperty("useSystemPrintDialog"); //$NON-NLS-1$

		// Setting all system property entries of the settings.
		Iterator iterator = keySet().iterator();
		while (iterator.hasNext())
		{
			String property = (String)iterator.next();
			if (property.startsWith("system.property.")) //$NON-NLS-1$
			{
				iterator.remove();
			}
		}

		removePureServerValues();

		//load from server (so it can be overridden from server)
		try
		{
			InputStream is = configfile.openStream();
			load(is);
			is.close();
		}
		catch (Exception ex)
		{
			Debug.error(ex);
		}

		String twoWay = getProperty(base.getHost() + base.getPort() + "SocketFactory.useTwoWaySocket"); //$NON-NLS-1$
		boolean serverTwoWay = Utils.getAsBoolean(getProperty("SocketFactory.useTwoWaySocket")); //$NON-NLS-1$
		if (twoWay == null || !serverTwoWay)
		{
			setProperty(base.getHost() + base.getPort() + "SocketFactory.useTwoWaySocket", String.valueOf(serverTwoWay)); //$NON-NLS-1$
		}

		if (currentIntegerformat != null)
		{
			setProperty("locale.integerformat", currentIntegerformat); //$NON-NLS-1$
		}
		if (currentNumberFormat != null)
		{
			setProperty("locale.numberformat", currentNumberFormat); //$NON-NLS-1$
		}
		if (currentDateFormat != null)
		{
			setProperty("locale.dateformat", currentDateFormat); //$NON-NLS-1$
		}
		if (currentUseSystemPrintDialog != null)
		{
			setProperty("useSystemPrintDialog", currentUseSystemPrintDialog); //$NON-NLS-1$
		}

		boolean pushLnfToMac = Utils.getAsBoolean(getProperty("pushLnfToMac", "false")); //$NON-NLS-1$ //$NON-NLS-2$
		if (!LAFManager.isUsingAppleLAF() || pushLnfToMac)
		{
			String lnf = getProperty("selectedlnf"); //$NON-NLS-1$
			String theme = getProperty("lnf.theme"); //$NON-NLS-1$
			String font = getProperty("font"); //$NON-NLS-1$
			// keep the lnf the user has selected
			if (lnf != null && getProperty(base.getHost() + base.getPort() + "_selectedlnf") == null) //$NON-NLS-1$
			{
				setProperty(base.getHost() + base.getPort() + "_selectedlnf", lnf); //$NON-NLS-1$
			}
			if (theme != null && getProperty(base.getHost() + base.getPort() + "_lnf.theme") == null) //$NON-NLS-1$
			{
				setProperty(base.getHost() + base.getPort() + "_lnf.theme", theme); //$NON-NLS-1$
			}
			if (font != null && getProperty(base.getHost() + base.getPort() + "_font") == null) //$NON-NLS-1$
			{
				setProperty(base.getHost() + base.getPort() + "_font", font); //$NON-NLS-1$
			}
		}
		else if (currentLnf != null)
		{
			setProperty("selectedlnf", currentLnf); //$NON-NLS-1$
		}
		else
		{
			remove("selectedlnf"); //$NON-NLS-1$
		}
		applySystemProperties();
	}

	public synchronized void loadFromURL(URL url) throws IOException
	{
		InputStream is = url.openStream();
		if (is != null)
		{
			load(is);
			is.close();
			applySystemProperties();
		}
	}

	public synchronized void loadFromFile(File file) throws IOException
	{
		this.file = file;
		Debug.log("Loading " + FILE_NAME + " from " + file.getAbsolutePath()); //$NON-NLS-1$ //$NON-NLS-2$
		if (file.exists())
		{
			FileInputStream fis = new FileInputStream(file);
			load(fis);
			fis.close();
			if (size() == 0)
			{
				Debug.log("Nothing loaded, size is 0, trying again"); //$NON-NLS-1$
				synchronized (this)
				{
					try
					{
						this.wait(1500);
					}
					catch (InterruptedException e)
					{
						// well we just can't wait
					}
				}
				fis = new FileInputStream(file);
				load(fis);
				fis.close();
				if (size() == 0)
				{
					Debug.log("Loading - Failed (size == 0)"); //$NON-NLS-1$
				}
			}
			applySystemProperties();
			Debug.log("Loading - Done"); //$NON-NLS-1$
		}
		else
		{
			Debug.log("Loading - Failed (not found)"); //$NON-NLS-1$
		}
	}

	private void applySystemProperties()
	{
		// Setting all system property entries of the settings.
		Iterator<Object> iterator = keySet().iterator();
		while (iterator.hasNext())
		{
			String property = iterator.next().toString();
			if (property.startsWith("system.property."))
			{
				System.setProperty(property.substring(16), getProperty(property));
			}
			else if (property.startsWith("sablo."))
			{
				System.setProperty(property, getProperty(property));
			}
		}
	}

	/**
	 *
	 */
	private void removePureServerValues()
	{
		// remove it always reload this from the server.
		remove("SocketFactory.rmiClientFactory"); //$NON-NLS-1$
		remove("SocketFactory.http.tunnel.encryption"); //$NON-NLS-1$
		remove("SocketFactory.tunnelConnectionMode"); //$NON-NLS-1$

		remove("servoy.branding"); //$NON-NLS-1$
		remove("servoy.branding.windowtitle"); //$NON-NLS-1$
		remove("servoy.branding.loadingimage"); //$NON-NLS-1$
		remove("servoy.branding.windowicon"); //$NON-NLS-1$
		remove("servoy.allowSolutionBrowsing"); //$NON-NLS-1$

		remove("servoy.disable.record.insert.reorder"); //$NON-NLS-1$

		remove("servoy.client.tracing"); //$NON-NLS-1$

	}

	/**
	 * Remove old or moved settings.
	 */
	private void removeObsoleteSettings()
	{
		// remove settings that are now saved in the eclipse workspace
		remove("showConfirmationDialogWhenErrors"); //$NON-NLS-1$
		remove("showConfirmationDialogWhenWarnings"); //$NON-NLS-1$
		remove("servoy.maxSampleDataTableRowCount"); //$NON-NLS-1$
		remove("designer.preferdMetrics"); //$NON-NLS-1$
		remove("designer.stepSize"); //$NON-NLS-1$
		remove("copyPasteOffset"); //$NON-NLS-1$
		remove("guidesize"); //$NON-NLS-1$
		remove("gridcolor"); //$NON-NLS-1$
		remove("gridsize"); //$NON-NLS-1$
		remove("pointsize"); //$NON-NLS-1$
		remove("showGrid"); //$NON-NLS-1$
		remove("snapToGrid"); //$NON-NLS-1$
		remove("saveEditorState"); //$NON-NLS-1$
	}

	private static final String enc_prefix = "encrypted:"; //$NON-NLS-1$

	@Override
	public synchronized void load(InputStream inStream) throws IOException
	{
		//1 load them in
		super.load(inStream);

		//2 convert properties to unencrypted form
		try
		{
			Cipher desCipher = Cipher.getInstance("DESede"); //$NON-NLS-1$
			desCipher.init(Cipher.DECRYPT_MODE, SecuritySupport.getCryptKey(this));

			Iterator it = entrySet().iterator();
			while (it.hasNext())
			{
				Map.Entry entry = (Map.Entry)it.next();
				if (entry.getKey().toString().toLowerCase().indexOf("password") != -1) //$NON-NLS-1$
				{
					String val = entry.getValue().toString();
					if (val.startsWith(enc_prefix))
					{
						String val_val = val.substring(enc_prefix.length());
						byte[] array_val = Utils.decodeBASE64(val_val);
						entry.setValue(new String(desCipher.doFinal(array_val)));
					}
				}
			}
		}
		catch (Exception e)
		{
			Debug.error(e);
			throw new IOException(e.getMessage());
		}
	}

	@Override
	public synchronized void store(OutputStream out, String header) throws IOException
	{
		//1 convert properties to encrypted form
		Properties to_save = new SortedProperties(); //make background list, we want to keep this unencripted
		try
		{
			SecuritySupport.clearCryptKey();
			Cipher desCipher = Cipher.getInstance("DESede"); //$NON-NLS-1$
			desCipher.init(Cipher.ENCRYPT_MODE, SecuritySupport.getCryptKey(this));

			Iterator it = entrySet().iterator();
			while (it.hasNext())
			{
				Map.Entry entry = (Map.Entry)it.next();
				String key = entry.getKey().toString();
				String val = entry.getValue().toString();
				if (!val.startsWith(enc_prefix) && key.toLowerCase().indexOf("password") != -1) //$NON-NLS-1$
				{
					byte[] array_val = entry.getValue().toString().getBytes();
					String new_val = Utils.encodeBASE64(desCipher.doFinal(array_val));
					val = enc_prefix + new_val;
				}
				to_save.put(key, val);
			}
		}
		catch (Exception e)
		{
			throw new IOException(e.getMessage());
		}

		//2 store them
		to_save.store(out, header);
	}

	/**
	 * Store the settings
	 */
	public synchronized void save() throws Exception
	{
		if (size() == 0) return;//nothing to save
		if (file == null) return;

		if (loadedFromServer)
		{
			removePureServerValues();
			File hiddendir = new File(System.getProperty("user.home"), J2DBGlobals.CLIENT_LOCAL_DIR); //$NON-NLS-1$
			if (!hiddendir.exists()) hiddendir.mkdirs();

//			file = new File(System.getProperty("user.home"), J2DBGlobals.CLIENT_LOCAL_DIR + CLIENT_LOCAL_FILE_NAME); //$NON-NLS-1$
		}

		//no need to store those
		Object appServerDir = remove(J2DBGlobals.SERVOY_APPLICATION_SERVER_DIRECTORY_KEY);

		removeObsoleteSettings();

		FileOutputStream fis = new FileOutputStream(file);
		store(fis, "servoy"); //$NON-NLS-1$
		fis.close();

		if (appServerDir != null) put(J2DBGlobals.SERVOY_APPLICATION_SERVER_DIRECTORY_KEY, appServerDir);
	}

	/**
	 * Returns the default settings
	 */
	public static Settings getInstance()
	{
		return me;
	}

	/**
	 * Load the bounds from a certain component
	 */
	public synchronized boolean loadBounds(Component component, String solutionName)
	{
		if (component == null || component.getName() == null) return false;

		String bounds = getProperty("rect_" + (solutionName != null ? solutionName + "_" : "") + component.getName() + "_bounds"); //$NON-NLS-1$ //$NON-NLS-2$
		if (bounds != null)
		{
			Rectangle r = PersistHelper.createRectangle(bounds);

			if (component instanceof JFrame)
			{
				Object oState = get("window_state_" + component.getName()); //$NON-NLS-1$
				if (oState != null)
				{
					int state = Utils.getAsInteger(oState);
					boolean maximized = (state & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH;
					if (maximized)
					{
						// set the bounds for when the window is no longer maximized (to avoid 0 size & stuff)
						component.setLocation(r.x + MAXIMIZED_INVIZIBLE_BORDER_PIXELS, // the location is also needed in order to maximize
							r.y + MAXIMIZED_INVIZIBLE_BORDER_PIXELS); // the window on the correct display (if there is more than 1 monitor)
						component.setSize(INITIAL_CLIENT_WIDTH, INITIAL_CLIENT_HEIGHT);
					}
					((JFrame)component).setExtendedState(state);
					if (maximized)
					{
						return true;
					}
				}
			}

			try
			{
				if (UIUtils.isOnScreen(r))
				{
					component.setBounds(r);
					component.validate();
					return true;
				} // else falls of the screens

			}
			catch (Exception e)
			{
				Debug.error(e);//just in case isOnScreen() its called and fails in headless env.
			}

		}
		return false;
	}

	/**
	 * Load the bounds from a certain component
	 */
	public synchronized boolean loadBounds(Component component)
	{
		return loadBounds(component, null);
	}

	/**
	 * Save the bounds from a certain component
	 */
	public synchronized void saveBounds(Component component, String solutionName)
	{
		if (component == null || component.getName() == null) return;
		if (component instanceof JFrame)
		{
			try
			{
				Method method = component.getClass().getMethod("getExtendedState", (Class[])null); //$NON-NLS-1$
				Object o = method.invoke(component, (Object[])null);
				put("window_state_" + component.getName(), o == null ? "" : o.toString()); //$NON-NLS-1$ //$NON-NLS-2$
			}
			catch (Exception ex)
			{
			}
		}

		//in case of having a previously saved property 'point', delete it
		remove("point_" + (solutionName != null ? solutionName + "_" : "") + component.getName() + "_location");

		Point l = component.getLocation();
		Debug.trace("location of " + component.getName() + " " + l); //$NON-NLS-1$ //$NON-NLS-2$
		put("rect_" + (solutionName != null ? solutionName + "_" : "") + component.getName() + "_bounds", //$NON-NLS-1$//$NON-NLS-2$
			PersistHelper.createRectangleString(component.getBounds()));
	}

	public synchronized void saveBounds(Component component)
	{
		saveBounds(component, null);
	}

	public synchronized boolean loadLocation(Component component, String solutionName)
	{
		if (component == null || component.getName() == null) return false;

		String location = getProperty("point_" + (solutionName != null ? solutionName + "_" : "") + component.getName() + "_location"); //$NON-NLS-1$ //$NON-NLS-2$
		if (location != null)
		{
			Point l = PersistHelper.createPoint(location);

			try
			{
				Rectangle r = new Rectangle(l.x, l.y, 1, 1);
				if (UIUtils.isOnScreen(r))
				{
					component.setLocation(l);
					return true;
				} // else falls of the screens

			}
			catch (Exception e)
			{
				Debug.error(e);//just in case isOnScreen() its called and fails in headless env.
			}
		}

		return false;
	}

	/**
	 * Load the location from a certain component
	 */
	public synchronized boolean loadLocation(Component component)
	{
		return loadLocation(component, null);
	}


	public synchronized void saveLocation(Component component, String solutionName)
	{
		if (component == null || component.getName() == null) return;

		//in case of having a previously saved property 'rect', delete it
		remove("rect_" + (solutionName != null ? solutionName + "_" : "") + component.getName() + "_bounds");

		Point l = component.getLocation();
		Debug.trace("location of " + component.getName() + " " + l); //$NON-NLS-1$ //$NON-NLS-2$
		put("point_" + (solutionName != null ? solutionName + "_" : "") + component.getName() + "_location", PersistHelper.createPointString(l)); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public synchronized void saveLocation(Component component)
	{
		saveLocation(component, null);
	}

	public synchronized void deleteBounds(String componentName, String solutionName)
	{
		remove("rect_" + (solutionName != null ? solutionName + "_" : "") + componentName + "_bounds");
		remove("point_" + (solutionName != null ? solutionName + "_" : "") + componentName + "_location");
		remove("window_state_" + componentName);
	}

	public synchronized void deleteAllBounds()
	{
		ArrayList deleteList = new ArrayList();
		Enumeration e = keys();
		while (e.hasMoreElements())
		{
			String element = (String)e.nextElement();
			if (element.startsWith("rect_") || element.startsWith("window_state_") || element.startsWith("point_")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			{
				deleteList.add(element);
			}
		}
		Iterator it = deleteList.iterator();
		while (it.hasNext())
		{
			Object element = it.next();
			remove(element);
		}
	}

	@Override
	public synchronized Object remove(Object key)
	{
		Object oldValue = super.remove(key);
		if (oldValue != null)
		{
			J2DBGlobals.firePropertyChange(this, key.toString(), oldValue, null);
		}
		return oldValue;
	}

	/*
	 * @see Properties#setProperty(String, String)
	 */
	@Override
	public synchronized Object setProperty(String key, String value)
	{
		Object o = super.setProperty(key, value);
		if (o == null && value == null) return o;
		if (o != null && value != null && o.equals(value)) return o;
		J2DBGlobals.firePropertyChange(this, key, o, value);
		return o;
	}

	@Override
	//FIXME why is the default implementation not ok?
	public int hashCode()
	{
		return 1;
	}

	/**
	 * Get all properties with prefixed key
	 * @param settings
	 * @param string
	 * @return
	 */
	public Map<String, String> getPrefixedProperties(String prefix)
	{
		Map<String, String> map = null;
		for (Map.Entry<Object, Object> entry : entrySet())
		{
			if (entry.getKey() instanceof String && entry.getValue() instanceof String && ((String)entry.getKey()).startsWith(prefix))
			{
				if (map == null)
				{
					map = new HashMap<String, String>();
				}
				map.put(((String)entry.getKey()).substring(prefix.length()), (String)entry.getValue());
			}
		}
		return map;
	}

	/**
	 * Remove all properties with prefixed key
	 * @param settings
	 * @param string
	 * @return
	 */
	public void removePrefixedProperties(String prefix)
	{
		for (Object key : keySet().toArray())
		{
			if (key instanceof String && ((String)key).startsWith(prefix))
			{
				remove(key);
			}
		}
	}

	public File getFile()
	{
		return file;
	}

	public static final String USER = "user."; //$NON-NLS-1$
	public static final String DEVELOPER_USER = "developer.user."; //$NON-NLS-1$

	public void loadUserProperties(Map<String, String> userProperties)
	{
		Iterator<Object> it = this.keySet().iterator();
		while (it.hasNext())
		{
			String key = (String)it.next();
			if (key.startsWith(USER))
			{
				userProperties.put(key.substring(USER.length()), this.getProperty(key));
			}
		}
	}

	public String getUserProperty(String prefix, String name)
	{
		return getProperty(prefix + (name.length() > 255 ? name.substring(0, 255) : name));
	}

	public void setUserProperty(String prefix, String name, String value)
	{
		if (value == null)
		{
			this.remove(prefix + (name.length() > 255 ? name.substring(0, 255) : name));
		}
		else
		{
			this.setProperty(prefix + (name.length() > 255 ? name.substring(0, 255) : name), (value.length() > 255 ? value.substring(0, 255) : value));
		}
	}

	public Properties getAsProperties()
	{
		return this;
	}


	@Override
	public String getProperty(String key)
	{
		// we do no more support repository team provider
		if (Settings.START_AS_TEAMPROVIDER_SETTING.equals(key)) return Boolean.FALSE.toString();
		return super.getProperty(key);
	}
}
