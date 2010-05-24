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
package com.servoy.j2db.server.headlessclient;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSessionBindingListener;

import com.servoy.j2db.dataprocessing.IDataSet;

/**
 * Interface to interact with a client from within a HTTP session, like a JSP page.
 * 
 * <pre>
 * ISessionBean servoy_hc = (ISessionBean)session.getAttribute(&quot;servoy&quot;);
 * if (servoy_hc == null)
 * {
 * 	//args are solution name,username,password 
 * 	servoy_hc = HeadlessClientFactory.createSessionBean(request, &quot;headless_client_demo&quot;);
 * 	session.setAttribute(&quot;servoy&quot;, servoy_hc);
 * }
 * //servoy_hc is now usable...
 * </pre>
 * 
 * Sample contextName values:<br>
 * 1) null, main form foundset<br>
 * 2) "forms.&lt;xxxxx&gt;", for tabpanel relationless form foundset<br>
 * 
 * @author Jan Blok
 * @see HeadlessClientFactory
 */
public interface ISessionBean extends IHeadlessClient, HttpSessionBindingListener
{
	/**
	 * Sets a form as main form.
	 * 
	 * @param formName the name of the form to set as main form
	 * @return true if successful
	 */
	public boolean setMainForm(String formName);

	/**
	 * Set all request parameters as dataprovider values, if the names match.
	 * 
	 * @param context This is the form name or null if the method is a global method.
	 * @param request_data the page request object
	 */
	public int setDataProviderValues(String contextName, HttpServletRequest request_data);

//	public Map getDataProviderValues(String contextName,String[] dataproviders);
//	public void setDataProviderValues(String contextName,Map dataproviders);

	/**
	 * Save all the data set via the setData methods.
	 * @deprecated use getPluginAccess().getDatabaseManager().saveData()
	 */
	@Deprecated
	public void saveData();

	/**
	 * Execute a form or global method.
	 * 
	 * @param visibleFormName the name of the form (must be visible)
	 * @param methodName the name of the method to call
	 * @param arguments to pass to the method
	 * @return the value returned by the method
	 * @deprecated use getPluginAccess().executeMethod(...)
	 */
	@Deprecated
	public Object executeMethod(String visibleFormName, String methodName, Object[] arguments) throws Exception;

	/**
	 * Get a message for a key and optional arguments
	 * 
	 * @param key
	 * @param args
	 * @return the text
	 * @deprecated use getPluginAccess().getI18NMessage(...)
	 */
	@Deprecated
	public String getI18NMessage(String key, Object[] args);

	/**
	 * Override the default used browser locale
	 * 
	 * @param l
	 */
	public void setLocale(Locale l);

	/**
	 * Get valuelist items as dataset.
	 * 
	 * @param contextName the context for this request
	 * @param valuelistName the name from the valuelist
	 * @return the dataset with valuelist values
	 */
	public IDataSet getValueListItems(String contextName, String valuelistName);
}
