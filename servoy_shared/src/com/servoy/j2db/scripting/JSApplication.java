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
package com.servoy.j2db.scripting;

import java.applet.Applet;
import java.applet.AudioClip;
import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import javax.print.DocFlavor;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.swing.AbstractAction;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeError;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.base.scripting.api.IJSApplication;
import com.servoy.j2db.ApplicationException;
import com.servoy.j2db.BasicFormController;
import com.servoy.j2db.ClientState;
import com.servoy.j2db.ClientVersion;
import com.servoy.j2db.ExitScriptException;
import com.servoy.j2db.FormController;
import com.servoy.j2db.FormManager;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IBasicFormManager;
import com.servoy.j2db.IFormController;
import com.servoy.j2db.IFormUIInternal;
import com.servoy.j2db.INGClientApplication;
import com.servoy.j2db.ISmartClientApplication;
import com.servoy.j2db.RuntimeWindowManager;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.dataprocessing.BufferedDataSet;
import com.servoy.j2db.dataprocessing.ClientInfo;
import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.dataprocessing.JSDataSet;
import com.servoy.j2db.dataprocessing.LookupValueList;
import com.servoy.j2db.dnd.DRAGNDROP;
import com.servoy.j2db.dnd.JSDNDEvent;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.plugins.IClientPlugin;
import com.servoy.j2db.scripting.info.APPLICATION_TYPES;
import com.servoy.j2db.scripting.info.CLIENTDESIGN;
import com.servoy.j2db.scripting.info.ELEMENT_TYPES;
import com.servoy.j2db.scripting.info.LOGGINGLEVEL;
import com.servoy.j2db.scripting.info.NGCONSTANTS;
import com.servoy.j2db.scripting.info.UICONSTANTS;
import com.servoy.j2db.scripting.info.WEBCONSTANTS;
import com.servoy.j2db.ui.IComponent;
import com.servoy.j2db.ui.IScriptRenderMethodsWithOptionalProps;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ILogLevel;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.SwingHelper;
import com.servoy.j2db.util.TimezoneUtils;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.gui.SnapShot;

/**
 * Application object from the SOM to handle all JS application calls
 *
 * @author jblok
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, publicName = "Application", scriptingName = "application")
public class JSApplication implements IReturnedTypesProvider, IJSApplication
{
	static
	{
		ScriptObjectRegistry.registerReturnedTypesProviderForClass(JSApplication.class, new IReturnedTypesProvider()
		{
			public Class< ? >[] getAllReturnedTypes()
			{
				return getAllReturnedTypesInternal();
			}
		});
	}

	private IApplication application;

	public JSApplication(IApplication app)
	{
		application = app;
	}

	public Class< ? >[] getAllReturnedTypes()
	{
		return getAllReturnedTypesInternal();
	}

	private static Class< ? >[] getAllReturnedTypesInternal()
	{
		return new Class< ? >[] { APPLICATION_TYPES.class, CLIENTDESIGN.class, DRAGNDROP.class, ELEMENT_TYPES.class, IScriptRenderMethodsWithOptionalProps.class, JSDNDEvent.class, JSEvent.class, JSRenderEvent.class, JSWindow.class, LOGGINGLEVEL.class, UICONSTANTS.class, UUID.class, WEBCONSTANTS.class, NGCONSTANTS.class };
	}

	@Deprecated
	private final Stack<FormAndComponent> selectedFormAndComponent = new Stack<FormAndComponent>();

	@Deprecated
	private FormAndComponent getTriggerNames()
	{
		if (!selectedFormAndComponent.isEmpty())
		{
			return selectedFormAndComponent.peek();
		}
		Debug.error("Shouldn't happen that the trigger element names are empty, is this called in a script that is triggered by solution startup?");
		return new FormAndComponent(null, null);
	}

	@Deprecated
	public void pushLastNames(FormAndComponent formAndComponent)
	{
		selectedFormAndComponent.push(formAndComponent);
	}

	@Deprecated
	public void popLastStackNames(FormAndComponent formAndComponent)
	{
		if (!selectedFormAndComponent.isEmpty() && selectedFormAndComponent.peek() == formAndComponent)
		{
			selectedFormAndComponent.pop();
		}
	}

	private void checkAuthorized() throws ServoyException
	{
		if (application.getApplicationServerAccess() == null)
		{
			// no access to application yet, have to log in first
			throw new ServoyException(ServoyException.CLIENT_NOT_AUTHORIZED);
		}
	}

	/**
	 * Get the names of the used client licenses (as strings in array).
	 *
	 * @sample var array = application.getLicenseNames();
	 *
	 * @return Client licenses names
	 */
	public String[] js_getLicenseNames() throws ServoyException
	{
		checkAuthorized();
		try
		{
			return application.getApplicationServerAccess().getLicenseNames();
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return null;
	}

	/**
	 * Get the active user count on the server (can be limited to current solution).
	 *
	 * @deprecated As of release 2.x, replaced by {@link #getActiveClientCount(boolean)}.
	 */
	@Deprecated
	public int js_getActiveUserCount(boolean currentSolutionOnly) throws ServoyException
	{
		return js_getActiveClientCount(currentSolutionOnly);
	}

	/**
	 * Get the active user count on the server (can be limited to current solution).
	 *
	 * @sample var count = application.getActiveClientCount(true);
	 *
	 * @param currentSolutionOnly Boolean (true) to get the active user count on server only to the current solution
	 *
	 * @return Active user count on the server
	 */
	public int js_getActiveClientCount(boolean currentSolutionOnly) throws ServoyException
	{
		checkAuthorized();
		try
		{
			int sol_id = 0;
			if (currentSolutionOnly)
			{
				if (application.getFlattenedSolution().isMainSolutionLoaded())
				{
					sol_id = application.getSolution().getSolutionID();
				}
				else
				{
					sol_id = application.getFlattenedSolution().getMainSolutionMetaData().getRootObjectId();
				}
			}
			return application.getApplicationServerAccess().getActiveClientCount(sol_id);
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return -1;
	}

	/**
	 * Get the parameters which are provided by startup.
	 * It returns an array with 2 elements, a string that is the startup argument and a map containing all named startup arguments, or
	 * null if there is no argument passed
	 *
	 * @deprecated As of release 6.0, method was deprecated because startup arguments are received as arguments to solution onOpen event.
	 *
	 * @sample
	 * var args_array = application.getStartupArguments();
	 * // the first element in the array is the 'argument' value from the startup
	 * var argument = args_array[0];
	 * // the second element is a map  containing all the named startup arguments
	 * var startupArgumentObj = args_array[1];
	 * var arg1 = startupArgumentObj['arg1_name'];
	 * var arg2 = startupArgumentObj['arg2_name'];
	 *
	 * @return Array with 2 elements, a string that is the startup argument and a map containing all named startup arguments,
	 * or null if there is no argument passed
	 */
	@Deprecated
	public Object[] js_getStartupArguments()
	{
		if (application instanceof ClientState)
		{
			Object[] startupArguments = ((ClientState)application).getPreferedSolutionMethodArguments();
			if (startupArguments != null && startupArguments.length > 1 && startupArguments[1] instanceof StartupArguments)
			{
				startupArguments[1] = ((StartupArguments)startupArguments[1]).toJSMap();
			}

			return startupArguments;
		}
		return null;
	}

	/**
	 * Get the current date (with hour/minutes/seconds set to zero).
	 *
	 * @deprecated As of release 5.0, replaced by {@link utils#timestampToDate(Object)}.
	 */
	@Deprecated
	public Date js_getDateStamp()
	{
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());
		Utils.applyMinTime(calendar);
		return calendar.getTime();
	}

	/**
	 * Adds a string of client information which gets stored on the server,
	 * and can be viewed on the Clients page of Servoy Server Administration Console.
	 *
	 * The new piece of client information is added on behalf of the running
	 * Servoy client.
	 *
	 * This function can be called more than once, if you want to add multiple
	 * lines of client information.
	 *
	 * NOTE:
	 * This function can also be used with the function <em>getClientCountForInfo</em>
	 * to count the number of clients with matching addditional client information.
	 *
	 * @sample
	 * application.addClientInfo('SaaS company name');
	 * application.addClientInfo('For any issues call +31-SA-AS');
	 *
	 * @param info A line of text to be added as additional client information
	 *             on behalf of the running Servoy client.
	 */
	public void js_addClientInfo(String info)
	{
		if (application instanceof ClientState)
		{
			((ClientState)application).addClientInfo(info);
		}
	}

	/**
	 * Removes a string of client information which is stored on the server and
	 * previously was added using the application.addClientInfo('client info')
	 *
	 * This function can be called more than once, if you want to delete multiple
	 * lines of client information.
	 *
	 * @sample
	 * var removed = application.removeClientInfo('SaaS company name');
	 *
	 * @param info A line of text to be removed from the client information
	 *             on behalf of the running Servoy client.
	 * @return boolean indicator if info was removed successfully
	 */
	public boolean js_removeClientInfo(String info)
	{
		if (application instanceof ClientState)
		{
			return ((ClientState)application).removeClientInfo(info);
		}
		return false;
	}

	/**
	 * Removes all names given to the client via the admin page.
	 *
	 * @sample application.removeAllClientInfo();
	 */
	public void js_removeAllClientInfo()
	{
		if (application instanceof ClientState)
		{
			((ClientState)application).removeAllClientInfo();
		}
	}

	/**
	 * Gets the count for all clients displaying the same additional information
	 * in the Clients page of Servoy Server Administration Console.
	 *
	 * @sample
	 * var count = application.getClientCountForInfo('SaaS company name');
	 * application.output('Including yourself, there are ' + count + ' client(s) running on behalf of the company.');
	 *
	 * @param info The additional client info string to search for.
	 *
	 * @return Number of clients
	 */
	public int js_getClientCountForInfo(String info) throws ServoyException
	{
		checkAuthorized();
		try
		{
			return application.getApplicationServerAccess().getClientCountForInfo(info);
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return -1;
	}

	/**
	 * @deprecated As of release 5.0, replaced by JSEvent functionality; event object is received as parameter to event method.
	 */
	@Deprecated
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public String js_getMethodTriggerElementName()
	{
		Object src = getTriggerNames().src;
		if (src instanceof IComponent && ((IComponent)src).getName() != null && !((IComponent)src).getName().startsWith(ComponentFactory.WEB_ID_PREFIX))
		{
			return ((IComponent)src).getName();
		}
		return null;
	}

	/**
	 * @deprecated As of release 5.0, replaced by JSEvent functionality; event object is received as parameter to event method.
	 */
	@Deprecated
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public String js_getMethodTriggerFormName()
	{
		return getTriggerNames().formName;
	}

	/**
	 * Returns true if the solution is running in the developer.
	 *
	 * @deprecated As of release 3.0, replaced by {@link #isInDeveloper()}.
	 */
	@Deprecated
	public boolean js_isRunningInDeveloper()
	{
		return application.isInDeveloper();
	}

	/**
	 * Returns true if the solution is running in the developer.
	 *
	 * @sample var flag = application.isInDeveloper();
	 *
	 * @return Boolean (true) if the solution is running in the developer, (false) otherwise
	 */
	@JSFunction
	public boolean isInDeveloper()
	{
		return application.isInDeveloper();
	}

	/**
	 * Returns the name of the current solution.
	 *
	 * @sample var solutionName = application.getSolutionName();
	 *
	 * @return Current solution name
	 */
	public String js_getSolutionName()
	{
		return application.getSolutionName();
	}

	/**
	 * Get the solution release number.
	 *
	 * @sample var release = application.getSolutionRelease();
	 *
	 * @return Current solution release number
	 */
	public int js_getSolutionRelease()
	{
		return application.getSolution().getReleaseNumber();
	}


	/**
	 * Get all persistent user property names.
	 *
	 * @sample
	 * // display all user properties
	 * allPropertyNames = application.getUserPropertyNames();
	 * for(var i = 0; i < allPropertyNames.length; i++)
	 * {
	 * 	application.output(allPropertyNames[i] + " = " + application.getUserProperty(allPropertyNames[i]));
	 * }
	 *
	 * @return Array of all user property names
	 */
	public String[] js_getUserPropertyNames()
	{
		return application.getUserPropertyNames();
	}

	/**
	 * Get a persistent user property.
	 *
	 * @sample var value = application.getUserProperty('showOrders');
	 *
	 * @param name Name of the property
	 *
	 * @return Property value
	 */
	@JSFunction
	public String getUserProperty(String name)
	{
		return application.getUserProperty(name);
	}

	/**
	 * Sets a user property for this client: <br>
	 * For headless clients(including Batch Processors and Authentication clients) the user property is stored in memory and will be lost upon client restart.
	 * <br>
	 * For Web Client the user property will be stored in a persistent cookie
	 * <br>
	 * For Smart Client it will be stored in a properties file on the client machine.
	 *
	 * @sample application.setUserProperty('showOrders','1');
	 *
	 * @param name Name of the user property
	 * @param value New value of the user property
	 */
	@JSFunction
	public void setUserProperty(String name, String value)
	{
		application.setUserProperty(name, value);
	}

	/**
	 *
	 * @param name Name of the UI property
	 * @param value New value of the UI property
	 *
	 * @return Boolean (true) if the UI property was set with the new value
	 *
	 * @deprecated use putClientProperty(Object,Object)
	 */
	@Deprecated
	public boolean js_setUIProperty(Object name, Object value)
	{
		return js_putClientProperty(name, value);
	}

	/**
	 * Sets a UI property.
	 *
	 * @sample
	 * //Only use this function from the solution on open method!
	 * //In smart client, use this to set javax.swing.UIDefaults properties.
	 * application.putClientProperty('ToolTip.hideAccelerator', true)
	 * //To change the comboboxes selection background color, do this:
	 * application.putClientProperty('ComboBox.selectionBackground', new Packages.javax.swing.plaf.ColorUIResource(java.awt.Color.RED))
	 *
	 * //In web client, use this to change the template directory.
	 * //To change the default dir of templates/default to templates/green_skin, do this:
	 * application.putClientProperty('templates.dir','green_skin');
	 *
	 * @param name Name of the client property
	 * @param value New value of the client property
	 *
	 * @return Boolean (true) if the client property was set with the new value
	 */
	public boolean js_putClientProperty(Object name, Object value)
	{
		if (name == null) return false;
		return application.putClientProperty(name, value);
	}

	/**
	 * @clonedesc js_putClientProperty(Object,Object)
	 *
	 * @sampleas js_putClientProperty(Object,Object)
	 *
	 * @param name Name of the client property
	 *
	 * @return the property value for the given name/key, null of nothing was found
	 */
	public Object js_getClientProperty(Object name)
	{
		return application.getClientProperty(name);
	}

	/**
	 * Gets the name of the current Look And Feel specified in Application Preferences.
	 *
	 * @sample var laf = application.getCurrentLookAndFeelName();
	 *
	 * @return Current Look And Feel
	 */
	public String js_getCurrentLookAndFeelName()
	{
		LookAndFeel landf = UIManager.getLookAndFeel();
		if (landf != null)
		{
			return landf.getName();
		}
		return "";
	}

	/**
	 * Overrides one style with another. In NGClient, it overrides the original stylesheet media defined on a solution with another media.
	 *
	 * @sample
	 * // Smart Client/Web Client usage
	 * //This function will only have effect on  forms not yet created, so solution onLoad is the best place to override'
	 * //For example overriding the use of default/designed style anywhere in the solution from 'mystyle' to 'mystyle_mac'
	 * application.overrideStyle('mystyle','mystyle_mace')//in this case both styles should have about the same classes
	 *
	 * //NGClient usage
	 * application.overrideStyle('oldstylesheet.css','mystylesheets/newstylesheet.css');
	 *
	 * @param originalStyleName Name of the style to override
	 * @param newStyleName Name of the new style
	 */
	@ServoyClientSupport(ng = true, wc = true, sc = true)
	public void js_overrideStyle(String originalStyleName, String newStyleName)
	{
		if (application instanceof INGClientApplication)
		{
			((INGClientApplication)application).overrideStyleSheet(originalStyleName, newStyleName);
		}
		else
		{
			ComponentFactory.overrideStyle(originalStyleName, newStyleName);
		}
	}

	/**
	 * Fill a custom type valuelist with values from array(s) or dataset.
	 *
	 * NOTE: if you modify values for checkbox/radio field, note that having one value in valuelist is a special case, so switching between one value and 0/multiple values may have side effects
	 * NOTE: This is expensive operation, which triggers refresh of all visible forms. Over usage of this method may inflict performance issues.
	 *
	 * @sample
	 * //set display values (return values will be same as display values)
	 * application.setValueListItems('my_en_types',new Array('Item 1', 'Item 2', 'Item 3'));
	 * //set display values and return values (which are stored in dataprovider)
	 * //application.setValueListItems('my_en_types',new Array('Item 1', 'Item 2', 'Item 3'),new Array(10000,10010,10456));
	 * //set display values and return values converted to numbers
	 * //application.setValueListItems('my_en_types',new Array('Item 1', 'Item 2', 'Item 3'),new Array('10000','10010', '10456'), true);
	 * //do query and fill valuelist (see databaseManager for full details of queries/dataset)
	 * //var query = 'select display_value,optional_real_value from test_table';
	 * //var dataset = databaseManager.getDataSetByQuery(databaseManager.getDataSourceServerName(controller.getDataSource()), query, null, 25);
	 *
	 * //application.setValueListItems('my_en_types',dataset);
	 *
	 * @param name Name of the valuelist
	 * @param displayValues Display values array
	 */
	@JSFunction
	public void setValueListItems(String name, Object[] displayValues)
	{
		js_setValueListItems(name, displayValues, null, Boolean.FALSE);
	}

	/**
	 * @clonedesc setValueListItems(String,Object[])
	 *
	 * @sampleas setValueListItems(String,Object[])
	 *
	 * @param name Name of the valuelist
	 * @param displayValues Display values array
	 * @param autoconvert Boolean (true) if display values and return values should be converted to numbers
	 */
	public void js_setValueListItems(String name, Object[] displayValues, Boolean autoconvert)
	{
		js_setValueListItems(name, displayValues, null, autoconvert);
	}

	/**
	 * @clonedesc setValueListItems(String,Object[])
	 *
	 * @sampleas setValueListItems(String,Object[])
	 *
	 * @param name Name of the valuelist
	 * @param displayValues Display values array
	 * @param realValues Real values array
	 */
	@JSFunction
	public void setValueListItems(String name, Object[] displayValues, Object[] realValues)
	{
		js_setValueListItems(name, displayValues, realValues, Boolean.FALSE);
	}

	/**
	 * @clonedesc setValueListItems(String,Object[])
	 *
	 * @sampleas setValueListItems(String,Object[])
	 *
	 * @param name Name of the valuelist
	 * @param dataset Dataset with display/real values
	 */
	public void js_setValueListItems(String name, JSDataSet dataset)
	{
		js_setValueListItems(name, dataset, Boolean.FALSE);
	}

	/**
	 * @clonedesc setValueListItems(String,Object[])
	 *
	 * @sampleas setValueListItems(String,Object[])
	 *
	 * @param name Name of the valuelist
	 * @param dataset Dataset with display/real values
	 * @param autoconvert Boolean (true) if display values and return values should be converted to numbers
	 */
	public void js_setValueListItems(String name, JSDataSet dataset, Boolean autoconvert)
	{
		if (dataset != null && dataset.getDataSet() != null && dataset.getDataSet().getColumnCount() > 0)
		{
			Object[] displayValues = new Object[dataset.getDataSet().getRowCount()];
			boolean hasRealValues = (dataset.getDataSet().getColumnCount() >= 2);
			Object[] realValues = null;
			if (hasRealValues)
			{
				realValues = new Object[dataset.getDataSet().getRowCount()];
			}
			for (int i = 0; i < dataset.getDataSet().getRowCount(); i++)
			{
				Object[] row = dataset.getDataSet().getRow(i);
				displayValues[i] = row[0];
				if (hasRealValues) realValues[i] = row[1];
			}
			js_setValueListItems(name, displayValues, realValues, autoconvert);
		}
	}

	/**
	 * @clonedesc setValueListItems(String,Object[])
	 *
	 * @sampleas setValueListItems(String,Object[])
	 *
	 * @param name
	 * @param displayValues Display values array
	 * @param realValues Real values array
	 * @param autoconvert Boolean (true) if display values and return values should be converted to numbers
	 */
	public void js_setValueListItems(String name, Object[] displayValues, Object[] realValues, Boolean autoconvert)
	{
		boolean _autoconvert = Utils.getAsBoolean(autoconvert);
		application.setValueListItems(name, displayValues, realValues, _autoconvert);
	}

	/**
	 * Get all the valuelist names as array.
	 *
	 * @sample var array = application.getValueListNames();
	 *
	 * @return Array with all valuelist names
	 */
	public String[] js_getValueListNames()
	{
		List<String> retval = new ArrayList<String>();
		try
		{
			Iterator<ValueList> it = application.getFlattenedSolution().getValueLists(true);
			while (it.hasNext())
			{
				ValueList vl = it.next();
				retval.add(vl.getName());
			}
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return retval.toArray(new String[retval.size()]);
	}

	/**
	 * Get all values from a custom or database type value list as dataset (with columns displayValue,realValue).
	 * NOTE: this doesn't return a value for a valuelist that depends on a database relation or is a global method valuelist.
	 *
	 * @sample
	 * //Note:see databaseManager.JSDataSet for full details of dataset
	 * var dataset = application.getValueListItems('my_en_types');
	 * //example to calc a strange total
	 * global_total = 0;
	 * for( var i = 1 ; i <= dataset.getMaxRowIndex() ; i++ )
	 * {
	 * 	global_total = global_total + dataset.getValue(i,1);
	 * }
	 * //example to assign to dataprovider
	 * //employee_salary = dataset.getValue(1,1)
	 *
	 * @param name Name of the valuelist
	 *
	 * @return DataSet with valuelist's display values and real values
	 */
	public JSDataSet js_getValueListItems(String name)
	{
		try
		{
			ValueList vl = application.getFlattenedSolution().getValueList(name);
			if (vl != null)
			{
				// TODO should getValueListItems not specify type and format??
				IValueList valuelist = ComponentFactory.getRealValueList(application, vl, true, Types.OTHER, null, null);
				if (valuelist != null)
				{
					List<Object[]> rows = new ArrayList<Object[]>();
					for (int i = 0; i < valuelist.getSize(); i++)
					{
						rows.add(new Object[] { valuelist.getElementAt(i), valuelist.getRealElementAt(i) });
					}
					return new JSDataSet(application, new BufferedDataSet(new String[] { "displayValue", "realValue" }, //$NON-NLS-1$ //$NON-NLS-2$
						rows));
				}
			}
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return null;
	}

	/**
	 * @deprecated As of release 3.0, replaced by {@link #getValueListDisplayValue(String, Object)}.
	 */
	@Deprecated
	public Object js_getValuelistDisplayValue(String name, Object realValue)
	{
		return js_getValueListDisplayValue(name, realValue);
	}

	/**
	 * Retrieve a valuelist display-value for a real-value.
	 * NOTE: this doesn't return a value for a valuelist that depends on a database relation or is a global method valuelist.
	 *
	 * @sample var displayable_status = application.getValueListDisplayValue('case_status',status);
	 *
	 * @param name Name of the valuelist
	 *
	 * @param realValue Real value of the valuelist
	 *
	 * @return Display value of the real value from the valuelist
	 */
	public Object js_getValueListDisplayValue(String name, Object realValue)
	{
		try
		{
			ValueList vl = application.getFlattenedSolution().getValueList(name);
			if (vl != null)
			{
				// TODO should getValueListItems not specify type and format??
				IValueList valuelist = ComponentFactory.getRealValueList(application, vl, true, Types.OTHER, null, null);
				if (valuelist != null)
				{
					int index = valuelist.realValueIndexOf(realValue);
					if (index != -1)
					{
						return valuelist.getElementAt(index);
					}
					else
					{
						// maybe is missing because of 500 limit of valuelist, look it up
						LookupValueList lvl = new LookupValueList(vl, application,
							ComponentFactory.getFallbackValueList(application, null, Types.OTHER, null, vl), null);
						index = lvl.realValueIndexOf(realValue, false);
						if (index != -1)
						{
							return lvl.getElementAt(index);
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return null;
	}

	/**
	 * Retrieve a valuelist as array, to get real-values for display-values.
	 * NOTE: this doesn't return a value for a valuelist that depends on a database relation or is a global method valuelist.
	 *
	 * @sample
	 * var packet_types = application.getValueListArray('packet_types');
	 * if (a_realValue == packet_types['displayValue'])
	 * {
	 * }
	 *
	 * @param name The name of the valuelist
	 *
	 * @return Named array for the valuelist
	 */
	public NativeArray js_getValueListArray(String name)
	{
		try
		{
			ValueList vl = application.getFlattenedSolution().getValueList(name);
			if (vl != null)
			{
				// TODO should getValueListItems not specify type and format??
				IValueList valuelist = ComponentFactory.getRealValueList(application, vl, true, Types.OTHER, null, null);
				if (valuelist != null)
				{
					// TODO check if this works
					NativeArray retval = (NativeArray)Context.getCurrentContext().newArray(application.getScriptEngine().getSolutionScope(), 0);
					retval.setPrototype(ScriptableObject.getClassPrototype(application.getScriptEngine().getSolutionScope(), "Array"));
					for (int i = 0; i < valuelist.getSize(); i++)
					{
						Object obj = valuelist.getElementAt(i);
						if (obj == null) continue;

						String strObj = null;
						int index = i;
						if (valuelist.hasRealValues())
						{
							strObj = obj.toString();
							// test to see if the key is an indexable number, apply same logic as in ScriptRuntime.getElem(Object obj, Object id, Scriptable scope)
							long indexTest = indexFromString(strObj);
							if (indexTest >= 0)
							{
								index = (int)indexTest;
								strObj = null;
							}
						}
						if (strObj == null)
						{
							retval.put(index, retval, valuelist.getRealElementAt(i));
						}
						else
						{
							retval.put(strObj, retval, valuelist.getRealElementAt(i));
						}
					}
					return retval;
				}
			}
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return null;
	}

	// Return -1L if str is not an index or the index value as lower 32
	// bits of the result
	// Logic copied from ScriptRuntime
	private static long indexFromString(String str)
	{
		// It must be a string.

		// The length of the decimal string representation of
		//  Integer.MAX_VALUE, 2147483647
		final int MAX_VALUE_LENGTH = 10;

		int len = str.length();
		if (len > 0)
		{
			int i = 0;
			boolean negate = false;
			int c = str.charAt(0);
			if (c == '-')
			{
				if (len > 1)
				{
					c = str.charAt(1);
					i = 1;
					negate = true;
				}
			}
			c -= '0';
			if (0 <= c && c <= 9 && len <= (negate ? MAX_VALUE_LENGTH + 1 : MAX_VALUE_LENGTH))
			{
				// Use negative numbers to accumulate index to handle
				// Integer.MIN_VALUE that is greater by 1 in absolute value
				// then Integer.MAX_VALUE
				int index = -c;
				int oldIndex = 0;
				i++;
				if (index != 0)
				{
					// Note that 00, 01, 000 etc. are not indexes
					while (i != len && 0 <= (c = str.charAt(i) - '0') && c <= 9)
					{
						oldIndex = index;
						index = 10 * index - c;
						i++;
					}
				}
				// Make sure all characters were consumed and that it couldn't
				// have overflowed.
				if (i == len && (oldIndex > (Integer.MIN_VALUE / 10) ||
					(oldIndex == (Integer.MIN_VALUE / 10) && c <= (negate ? -(Integer.MIN_VALUE % 10) : (Integer.MAX_VALUE % 10)))))
				{
					return 0xFFFFFFFFL & (negate ? index : -index);
				}
			}
		}
		return -1L;
	}

	/**
	 * Make a toolbar visible or invisible.
	 *
	 * @sample
	 * //example: hide the text toolbar
	 * application.setToolbarVisible('text',false);
	 *
	 * @param name Name of the toolbar
	 * @param visible Visibility of the toolbar
	 */
	@ServoyClientSupport(ng = false, mc = false, wc = false, sc = true)
	public void js_setToolbarVisible(String name, boolean visible)
	{
		if (application instanceof ISmartClientApplication)
		{
			((ISmartClientApplication)application).getToolbarPanel().setToolbarVisible(name, visible);
		}
	}

	/**
	 * Get the screen width in pixels.
	 *
	 * @sample var width = application.getScreenWidth();
	 *
	 * @return Screen width
	 */
	public int js_getScreenWidth()
	{
		return application.getScreenSize().width;
	}

	/**
	 * Get the screen height in pixels.
	 *
	 * @sample var height = application.getScreenHeight();
	 *
	 * @return Screen height
	 */
	public int js_getScreenHeight()
	{
		return application.getScreenSize().height;
	}

	private RuntimeWindow getUserWindow(String windowName)
	{
		RuntimeWindow w = null;
		if (windowName == null)
		{
			// no name specified; use default dialog if it is showing, or else the main application window
			w = application.getRuntimeWindowManager().getWindow(FormManager.DEFAULT_DIALOG_NAME);
			if (w == null || (!w.isVisible()))
			{
				w = application.getRuntimeWindowManager().getWindow(null); // main app. frame
			}
		}
		else
		{
			// we use the window with the given name, if found
			w = application.getRuntimeWindowManager().getWindow(windowName);
		}

		return w;
	}

	/**
	 * Get the window X location in pixels. If windowName is not specified or null, it will use either the default dialog (if it is shown) or the main application window.
	 *
	 * @deprecated As of release 6.0, dialogs/windows API has been rewritten (based in JSWindow objects).
	 *
	 * @sample var x = application.getWindowX('customerDialog');
	 *
	 * @param windowName optional Window name
	 *
	 * @return Window X location
	 */
	@Deprecated
	public int js_getWindowX(Object[] args)
	{
		if ((args.length != 0) && (args.length != 1)) return 0;

		String dialogName = null;
		if (args.length == 1 && args[0] instanceof String)
		{
			dialogName = replaceFailingCharacters((String)args[0]);
		}
		RuntimeWindow w = getUserWindow(dialogName);
		return w != null ? w.getX() : 0;

	}

	/**
	 * Get the window Y location in pixels. If windowName is not specified or null, it will use either the default dialog (if it is shown) or the main application window.
	 *
	 * @deprecated As of release 6.0, dialogs/windows API has been rewritten (based in JSWindow objects).
	 *
	 * @sample var y = application.getWindowY('customerDialog');
	 *
	 * @param windowName optional Name of the window
	 *
	 * @return Window Y location
	 */
	@Deprecated
	public int js_getWindowY(Object[] args)
	{
		if ((args.length != 0) && (args.length != 1)) return 0;

		String dialogName = null;
		if (args.length == 1 && args[0] instanceof String)
		{
			dialogName = replaceFailingCharacters((String)args[0]);
		}

		RuntimeWindow w = getUserWindow(dialogName);
		return w != null ? w.getY() : 0;
	}

	/**
	 * Get the window width in pixels. If windowName is not specified or null, it will use either the default dialog (if it is shown) or the main application window.
	 *
	 * @deprecated As of release 6.0, dialogs/windows API has been rewritten (based in JSWindow objects).
	 *
	 * @sample var width = application.getWindowWidth('customerDialog');
	 *
	 * @param windowName optional Name of the window
	 *
	 * @return Window width
	 */
	@Deprecated
	public int js_getWindowWidth(Object[] args)
	{
		if ((args.length != 0) && (args.length != 1)) return 0;

		String dialogName = null;
		if (args.length == 1 && args[0] instanceof String)
		{
			dialogName = replaceFailingCharacters((String)args[0]);
		}
		RuntimeWindow w = getUserWindow(dialogName);
		return w != null ? w.getWidth() : 0;
	}

	/**
	 * Get the window height in pixels. If windowName is not specified or null, it will use either the default dialog (if it is shown) or the main application window.
	 *
	 * @deprecated As of release 6.0, dialogs/windows API has been rewritten (based in JSWindow objects).
	 *
	 * @sample var height = application.getWindowHeight('customerDialog');
	 *
	 * @param windowName optional Name of the window
	 *
	 * @return Window height
	 */
	@Deprecated
	public int js_getWindowHeight(Object[] args)
	{
		if ((args.length != 0) && (args.length != 1)) return 0;

		String dialogName = null;
		if (args.length == 1 && args[0] instanceof String)
		{
			dialogName = replaceFailingCharacters((String)args[0]);
		}
		RuntimeWindow w = getUserWindow(dialogName);
		return w != null ? w.getHeight() : 0;
	}

	/**
	 * Set the window size. If windowName is not specified or null, it will resize either the default dialog (if it is shown) or the main application window.
	 *
	 * @deprecated As of release 6.0, dialogs/windows API has been rewritten (based in JSWindow objects).
	 *
	 * @sample application.setWindowSize(400,400,'customerDialog');
	 *
	 * @param width Window new width
	 *
	 * @param height Window new height
	 *
	 * @param windowName optional Name of the window
	 */
	@Deprecated
	public void js_setWindowSize(Object[] args)
	{
		if ((args.length != 2) && (args.length != 3)) return;

		int width = 0;
		int height = 0;
		String windowName = null;

		if (args[0] instanceof Number)
		{
			width = ((Number)args[0]).intValue();
		}
		if (args[1] instanceof Number)
		{
			height = ((Number)args[1]).intValue();
		}
		if (args.length == 3)
		{
			if (args[2] instanceof String)
			{
				windowName = (String)args[2];
			}
		}

		windowName = replaceFailingCharacters(windowName);
		RuntimeWindow w = getUserWindow(windowName);

		if (w != null)
		{
			w.setSize(width, height);
		}
	}

	/**
	 * Set the window location. If windowName is not specified or null, it will use either the default dialog (if it is shown) or the main application window.
	 *
	 * @deprecated As of release 6.0, dialogs/windows API has been rewritten (based in JSWindow objects).
	 *
	 * @sample application.setWindowLocation(10,10,'customerDialog');
	 *
	 * @param x Window new X location
	 *
	 * @param y Window new Y location
	 *
	 * @param windowName optional Name of the window
	 */
	@Deprecated
	public void js_setWindowLocation(Object[] args)
	{
		if ((args.length != 2) && (args.length != 3)) return;

		int x = -1;
		int y = -1;
		String windowName = null;

		if (args[0] instanceof Number)
		{
			x = ((Number)args[0]).intValue();
		}
		if (args[1] instanceof Number)
		{
			y = ((Number)args[1]).intValue();
		}
		if (args.length == 3)
		{
			if (args[2] instanceof String)
			{
				windowName = (String)args[2];
			}
		}

		windowName = replaceFailingCharacters(windowName);
		RuntimeWindow w = getUserWindow(windowName);

		if (w != null)
		{
			w.setLocation(x, y);
		}
	}

	/**
	 * Set the status area value.
	 *
	 * NOTE: Most modern browsers do not support status bar and status text anymore. This method sets status property of the window using javascript.
	 *
	 * @sample application.setStatusText('Your status text','Your status tooltip text');
	 *
	 * @param text New status text
	 * @param tooltip Status tooltip text
	 */
	@ServoyClientSupport(ng = false, mc = false, wc = true, sc = true)
	public void js_setStatusText(String text, String tooltip)
	{
		application.setStatusText(text != null ? text : "", tooltip);//$NON-NLS-1$
	}

	/**
	 * Set the status area value.
	 *
	 * @sample application.setStatusText('Your status text');
	 *
	 * @param text New status text
	 */
	@ServoyClientSupport(ng = false, mc = false, wc = true, sc = true)
	public void js_setStatusText(String text)
	{
		js_setStatusText(text, null);
	}

	/**
	 * Get all the printer names in an array.
	 *
	 * @sample var printersArray = application.getPrinters();
	 *
	 * @return All printer names
	 */
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public String[] js_getPrinters()
	{
		DocFlavor flavor = DocFlavor.SERVICE_FORMATTED.PAGEABLE;
		PrintRequestAttributeSet pras = new HashPrintRequestAttributeSet();
		PrintService[] printServices = PrintServiceLookup.lookupPrintServices(flavor, pras);
		if (printServices == null) return new String[0];
		String[] retval = new String[printServices.length];
		for (int i = 0; i < printServices.length; i++)
		{
			retval[i] = printServices[i].getName();
		}
		return retval;
	}

	/**
	 * Gets the HTTP server url.
	 *
	 * @description-mc
	 * Gets the application server URL for mobile client to connect to.
	 *
	 * @sample var url = application.getServerURL();
	 *
	 * @return HTTP server URL
	 */
	@JSFunction
	public String getServerURL()
	{
		String url = application.getServerURL().toString();
		// if it doesn't end with / add it
		// it is needed in a context (the urls should be http://hostname:port/context/)
		// and it has to be the same in the developer
		if (!url.endsWith("/")) //$NON-NLS-1$
		{
			url += '/';
		}

		return url;
	}

	/**
	 * Get the application type.
	 *
	 * @sample
	 * var type = application.getApplicationType();
	 * //see application type contstant
	 *
	 * @return Constant application type
	 */
	@JSFunction
	public int getApplicationType()
	{
		return application.getApplicationType();
	}

	/**
	 * Closes the currently open solution and optionally opens another solution, calling a specified global method with the specified arguments.
	 * If the user has been logged in, this function keeps the user logged in and in the newly open solution, the login is skipped and the solution goes straight to the first form.
	 * If you want to go to a different url, you need to call application.showURL(url) before calling application.closeSolution() (this is only applicable for Web Client).
	 * An alternative option is security.logout() which also does a log out for the user (for solutions that require authentication).
	 *
	 * @sample
	 * //application.showURL('http://www.servoy.com', '_self');  //Web Client only
	 * application.closeSolution();
	 * //close current solution, open solution 'solution_name', call global method 'global_method_name' with argument 'my_argument'.
	 * //if the user has been logged in, he will stay logged in
	 * //application.closeSolution('solution_name','global_method_name','my_argument');
	 * //Note: specifying a solution will not work in the Developer due to debugger dependencies
	 * //specified solution should be of compatible type with client (normal type or client specific(Smart client only/Web client only) type )
	 *
	 */
	public void js_closeSolution()
	{
		js_closeSolution(null, null, null);
	}

	/**
	 * @clonedesc js_closeSolution()
	 *
	 * @sampleas js_closeSolution()
	 *
	 * @param solutionToLoad Name of the solution to load
	 */
	public void js_closeSolution(String solutionToLoad)
	{
		js_closeSolution(solutionToLoad, null, null);
	}

	/**
	 * @clonedesc js_closeSolution()
	 *
	 * @sampleas js_closeSolution()
	 *
	 * @param solutionToLoad Name of the solution to load
	 * @param methodName Name of the global method to call
	 */
	public void js_closeSolution(String solutionToLoad, String methodName)
	{
		js_closeSolution(solutionToLoad, methodName, null);
	}

	/**
	 * @clonedesc js_closeSolution()
	 *
	 * @sampleas js_closeSolution()
	 *
	 * @param solutionToLoad Name of the solution to load
	 * @param methodName Name of the global method to call
	 * @param methodArgument Argument passed to the global method
	 */
	public void js_closeSolution(final String solutionToLoad, final String methodName, final Object methodArgument)
	{
		application.invokeAndWait(new Runnable()
		{
			public void run()
			{
				application.closeSolution(false, solutionToLoad != null || methodName != null || methodArgument != null
					? new Object[] { solutionToLoad, methodName, methodArgument } : null);
			}
		});
		//application.closeSolution(false, solution_to_open_args);
		throw new ExitScriptException("Solution closed");
	}

	/**
	 * @deprecated As of release 3.0, replaced by {@link security#logout(Object[])}.
	 */
	@Deprecated
	public void js_logout()
	{
		if (!application.isInDeveloper() || testMagicProperty())
		{
			application.logout(null);
		}
		else
		{
			application.reportInfo("Solution tried to logout");
		}
	}

	/**
	 * Stop and exit application.
	 *
	 * @sample
	 * // exit application
	 * application.exit();
	 */
	public void js_exit()
	{
		if (!application.isInDeveloper() || testMagicProperty())
		{
			((ClientState)application).shutDown(false);
			throw new ExitScriptException("Application exit");
		}
		else
		{
			application.reportInfo("Solution tried to close the developer");
		}
	}

	private boolean testMagicProperty()
	{
		// can inhibit setting visibility of toolbars, js_logout, js_exit, and window js_setSize, js_setLocation
		return Utils.getAsBoolean(application.getSettings().getProperty("window.resize.location.enabled", "true"));
	}

	/**
	 * @deprecated As of release 6.1.4. Use overload with array arguments for ex: executeProgramInBackground("c:\\Users\\myself\\myapp.exe", ["arg1", "arg2", "arg3"], ["MY_ENV_VAR=something"], "/home/myself/");
	 * Execute a program in the background. Specify the cmd as you would do in a console.
	 *
	 * @sample
	 * // "#" is divider between program args, environment vars and startdir
	 * // For Windows systems:
	 * // Runs a binary located in the user's home directory. The application will run in the current working
	 * // directory, which in general is the one where Servoy was started from.
	 * application.executeProgramInBackground("c:\\Users\\myself\\myapp.exe", "arg1", "arg2", "arg3");
	 * // The same as above, but run the application in the user's home directory.
	 * application.executeProgramInBackground("c:\\Users\\myself\\myapp.exe", "arg1", "arg2", "arg3", "#", "#", "c:\\Users\\myself\\");
	 * // The same as above, but also set an environment variable for the called program.
	 * application.executeProgramInBackground("c:\\Users\\myself\\myapp.exe", "arg1", "arg2", "arg3", "#", "MY_ENV_VAR=something", "#", "c:\\Users\\myself\\");
	 * // For non-Windows systems:
	 * application.executeProgramInBackground("/home/myself/myapp", "arg1", "arg2", "arg3");
	 * application.executeProgramInBackground("/home/myself/myapp", "arg1", "arg2", "arg3", "#", "#", "/home/myself/");
	 * application.executeProgramInBackground("/home/myself/myapp", "arg1", "arg2", "arg3", "#", "MY_ENV_VAR=something", "#", "/home/myself/myapp");
	 * // Open a file with the default application associated with it. (on Windows)
	 * application.executeProgramInBackground("rundll32.exe", "url.dll,FileProtocolHandler", "filename");
	 * // Open a file with the default application associated with it. (on Linux)
	 * application.executeProgramInBackground("xdg-open", "filename");
	 * // Open a file with the default application associated with it. (on MacOS)
	 * application.executeProgramInBackground("open", "filename");
	 * // Open a file with a specific application (on MacOS).
	 * application.executeProgramInBackground("open", "-a", "OpenOffice.org.app", "filename.doc");
	 *
	 * @param programName Name (fullpath) of the program to execute
	 * @param arg1 optional Argument
	 * @param arg2 optional Argument
	 * @param argN optional Argument
	 * @param # optional Divider between program args and environment vars
	 * @param environmentvar1 optional Environment variable
	 * @param environmentvarN optional Environment variable
	 * @param # optional Divider between program environment vars and startdir
	 * @param startdirectory optional Program start directory
	 */
	@Deprecated
	public void js_executeProgramInBackground(final String... vargs)
	{
		if (vargs == null || vargs.length == 0) return;

		application.getScheduledExecutor().execute(new Runnable()
		{
			public void run()
			{
				try
				{
					js_executeProgram(vargs);
				}
				catch (ApplicationException e)
				{
					application.handleException(application.getI18NMessage("servoy.error.executing.program") + vargs[0], e);
				}
			}
		});
	}

	/**
	 * @deprecated As of release 6.1.4. Use overload with array arguments, for ex: executeProgram("c:\\Users\\myself\\myapp.exe", ["arg1", "arg2", "arg3"], ["MY_ENV_VAR=something"], "/home/myself/");
	 * Execute a program and returns output. Specify the cmd as you would do in a console.
	 *
	 * @sample
	 * // "#" is divider between program args, environment vars and startdir
	 * // For Windows systems:
	 * // Runs a binary located in the user's home directory. The application will run in the current working
	 * // directory, which in general is the one where Servoy was started from.
	 * application.executeProgram("c:\\Users\\myself\\myapp.exe", "arg1", "arg2", "arg3");
	 * // The same as above, but run the application in the user's home directory.
	 * application.executeProgram("c:\\Users\\myself\\myapp.exe", "arg1", "arg2", "arg3", "#", "#", "c:\\Users\\myself\\");
	 * // The same as above, but also set an environment variable for the called program.
	 * application.executeProgram("c:\\Users\\myself\\myapp.exe", "arg1", "arg2", "arg3", "#", "MY_ENV_VAR=something", "#", "c:\\Users\\myself\\");
	 * // For non-Windows systems:
	 * application.executeProgram("/home/myself/myapp", "arg1", "arg2", "arg3");
	 * application.executeProgram("/home/myself/myapp", "arg1", "arg2", "arg3", "#", "#", "/home/myself/");
	 * application.executeProgram("/home/myself/myapp", "arg1", "arg2", "arg3", "#", "MY_ENV_VAR=something", "#", "/home/myself/myapp");
	 * // Open a file with the default application associated with it. (on Windows)
	 * application.executeProgram("rundll32.exe", "url.dll,FileProtocolHandler", "filename");
	 * // Open a file with the default application associated with it. (on Linux)
	 * application.executeProgram("xdg-open", "filename");
	 * // Open a file with the default application associated with it. (on MacOS)
	 * application.executeProgram("open", "filename");
	 * // Open a file with a specific application (on MacOS).
	 * application.executeProgram("open", "-a", "OpenOffice.org.app", "filename.doc");
	 *
	 * @param programName Name (fullpath) of the program to execute
	 * @param arg1 optional Argument
	 * @param arg2 optional Argument
	 * @param argN optional Argument
	 * @param # optional Divider between program args and environment vars
	 * @param environmentvar1 optional Environment variable
	 * @param environmentvarN optional Environment variable
	 * @param # optional Divider between program environment vars and startdir
	 * @param startdirectory optional Program start directory
	 *
	 * @return The output generated by the program execution.
	 */
	@Deprecated
	public String js_executeProgram(String... vargs) throws ApplicationException
	{
		if (vargs == null || vargs.length == 0) return "";

		StringBuilder output = new StringBuilder();
		try
		{
			List<String> cmdArgs = new ArrayList<String>();
			List<String> envArgs = new ArrayList<String>();
			List<String> dirArgs = new ArrayList<String>();
			List<String> currentArgs = cmdArgs;
			for (Object element : vargs)
			{
				String arg = (element == null ? "" : element.toString()); //$NON-NLS-1$
				if ("#".equals(arg) && currentArgs == cmdArgs)
				{
					currentArgs = envArgs;
				}
				else if (("#".equals(arg) || "|".equals(arg)) && currentArgs == envArgs)
				{
					//not sure why the | is in the if...according to docs/sample it should not be there
					currentArgs = dirArgs;
				}
				else
				{
					currentArgs.add(arg);
				}
			}

			String line = null;
			String[] cmdArgsArray = cmdArgs.toArray(new String[cmdArgs.size()]);
			String[] envArgsArray = (envArgs.size() == 0 ? null : envArgs.toArray(new String[envArgs.size()]));
			File startDir = (dirArgs.size() == 0 ? null : new File(dirArgs.get(0).toString()));

			Process myProcess = Runtime.getRuntime().exec(cmdArgsArray, envArgsArray, startDir);
			BufferedReader in = new BufferedReader(new InputStreamReader(myProcess.getInputStream()));
			while ((line = in.readLine()) != null)
			{
				output.append(line);
				output.append("\n");
			}
			in.close();
			setLastErrorCode(0);//clear any error
		}
		catch (Exception e)
		{
			setLastErrorCode(ServoyException.EXECUTE_PROGRAM_FAILED);
			throw new ApplicationException(ServoyException.EXECUTE_PROGRAM_FAILED, e);
//			application.reportError(application.getI18NMessage("servoy.error.executing.program") + vargs[0], e);
		}
		return output.toString();
	}

	/**
	 * Execute a program and returns output. Specify the cmd as you would do in a console.
	 *
	 * @sample
	 * // For Windows systems:
	 * // Runs a binary located in the user's home directory. The application will run in the current working
	 * // directory, which in general is the one where Servoy was started from.
	 * application.executeProgram("c:\\Users\\myself\\myapp.exe", ["arg1", "arg2", "arg3"]);
	 * // The same as above, but run the application in the user's home directory.
	 * application.executeProgram("c:\\Users\\myself\\myapp.exe", ["arg1", "arg2", "arg3"], null, "c:\\Users\\myself\\");
	 * // The same as above, but also set an environment variable for the called program.
	 * application.executeProgram("c:\\Users\\myself\\myapp.exe", ["arg1", "arg2", "arg3"], ["MY_ENV_VAR=something"], "c:\\Users\\myself\\");
	 * // For non-Windows systems:
	 * application.executeProgram("/home/myself/myapp", ["arg1", "arg2", "arg3"]);
	 * application.executeProgram("/home/myself/myapp", ["arg1", "arg2", "arg3"], null, "/home/myself/");
	 * application.executeProgram("/home/myself/myapp", ["arg1", "arg2", "arg3"], ["MY_ENV_VAR=something"], "/home/myself/");
	 * // Open a file with the default application associated with it. (on Windows)
	 * application.executeProgram("rundll32.exe", ["url.dll,FileProtocolHandler", "filename"]);
	 * // Open a file with the default application associated with it. (on Linux)
	 * application.executeProgram("xdg-open", ["filename"]);
	 * // Open a file with the default application associated with it. (on MacOS)
	 * application.executeProgram("open", ["filename"]);
	 * // Open a file with a specific application (on MacOS).
	 * application.executeProgram("open", ["-a", "OpenOffice.org.app", "filename.doc"]);
	 *
	 *
	 * @param program (fullpath) of the program to execute
	 * @param params an array of strings as program arguments
	 * @param environmentVars array of strings, each element of which has environment variable settings in the format name=value, or null if the subprocess should inherit the environment of the current process.
	 * @param startDir the working directory of the subprocess, or null if the subprocess should inherit the working directory of the current process.
	 * @return The output generated by the program execution.
	 * @throws ApplicationException
	 */
	public String js_executeProgram(String program, String[] params, String[] environmentVars, String startDir) throws ApplicationException
	{

		StringBuilder output = new StringBuilder();
		try
		{
			//startDir can be null or any string
			File _startDir = (startDir == null ? null : new File(startDir));
			String[] commandWithParams = Utils.arrayJoin(new String[] { program }, params);

			Process myProcess = Runtime.getRuntime().exec(commandWithParams, environmentVars, _startDir);
			BufferedReader in = new BufferedReader(new InputStreamReader(myProcess.getInputStream()));
			String line = null;
			while ((line = in.readLine()) != null)
			{
				output.append(line);
				output.append("\n");
			}
			in.close();
			setLastErrorCode(0);//clear any error
		}
		catch (Exception e)
		{
			setLastErrorCode(ServoyException.EXECUTE_PROGRAM_FAILED);
			//throw new ApplicationException(ServoyException.EXECUTE_PROGRAM_FAILED, e);
			ApplicationException applicationException = new ApplicationException(ServoyException.EXECUTE_PROGRAM_FAILED, e);
			JavaScriptException javaScriptException = new JavaScriptException(applicationException, "", 0);
			throw javaScriptException;
		}
		return output.toString();
	}

	/**
	 * @clonedesc js_executeProgram(String,String[],String[],String)
	 *
	 * @sampleas js_executeProgram(String,String[],String[],String)
	 *
	 * @param program (fullpath) of the program to execute
	 * @param params an array of strings as program arguments
	 * @param environmentVars array of strings, each element of which has environment variable settings in the format name=value, or null if the subprocess should inherit the environment of the current process.
	 * @return The output generated by the program execution.
	 * @throws ApplicationException
	 */
	public String js_executeProgram(String program, String[] params, String[] environmentVars) throws ApplicationException
	{
		return js_executeProgram(program, params, environmentVars, null);
	}

	/**
	 * @clonedesc js_executeProgram(String,String[],String[],String)
	 *
	 * @sampleas js_executeProgram(String,String[],String[],String)
	 *
	 * @param program (fullpath) of the program to execute
	 * @param params an array of strings as program arguments
	 * @return The output generated by the program execution.
	 * @throws ApplicationException
	 */
	public String js_executeProgram(String program, String[] params) throws ApplicationException
	{
		return js_executeProgram(program, params, null, null);
	}

	/**
	 * @clonedesc js_executeProgram(String,String[],String[],String)
	 *
	 * @sampleas js_executeProgram(String,String[],String[],String)
	 *
	 * @param program (fullpath) of the program to execute
	 * @return The output generated by the program execution.
	 * @throws ApplicationException
	 */
	public String js_executeProgram(String program) throws ApplicationException
	{
		return js_executeProgram(program, null, null, null);
	}

	/**
	 * Execute a program in the background. Specify the cmd as you would do in a console.
	 *
	 * @sample
	 * // For Windows systems:
	 * // Runs a binary located in the user's home directory. The application will run in the current working
	 * // directory, which in general is the one where Servoy was started from.
	 * application.executeProgramInBackground("c:\\Users\\myself\\myapp.exe", ["arg1", "arg2", "arg3"]);
	 * // The same as above, but run the application in the user's home directory.
	 * application.executeProgramInBackground("c:\\Users\\myself\\myapp.exe", ["arg1", "arg2", "arg3"], null, "c:\\Users\\myself\\");
	 * // The same as above, but also set an environment variable for the called program.
	 * application.executeProgramInBackground("c:\\Users\\myself\\myapp.exe", ["arg1", "arg2", "arg3"], ["MY_ENV_VAR=something"], "c:\\Users\\myself\\");
	 * // For non-Windows systems:
	 * application.executeProgramInBackground("/home/myself/myapp", ["arg1", "arg2", "arg3"]);
	 * application.executeProgramInBackground("/home/myself/myapp", ["arg1", "arg2", "arg3"], null, "/home/myself/");
	 * application.executeProgramInBackground("/home/myself/myapp", ["arg1", "arg2", "arg3"], ["MY_ENV_VAR=something"], "/home/myself/");
	 * // Open a file with the default application associated with it. (on Windows)
	 * application.executeProgramInBackground("rundll32.exe", ["url.dll,FileProtocolHandler", "filename"]);
	 * // Open a file with the default application associated with it. (on Linux)
	 * application.executeProgramInBackground("xdg-open", ["filename"]);
	 * // Open a file with the default application associated with it. (on MacOS)
	 * application.executeProgramInBackground("open", ["filename"]);
	 * // Open a file with a specific application (on MacOS).
	 * application.executeProgram("open", ["-a", "OpenOffice.org.app", "filename.doc"]);
	 *
	 *
	 * @param program (fullpath) of the program to execute
	 * @param params an array of strings as program arguments
	 * @param environmentVars array of strings, each element of which has environment variable settings in the format name=value, or null if the subprocess should inherit the environment of the current process.
	 * @param startDir the working directory of the subprocess, or null if the subprocess should inherit the working directory of the current process.
	 * @throws ApplicationException
	 */
	public void js_executeProgramInBackground(final String program, final String[] params, final String[] environmentVars, final String startDir)
	{
		application.getScheduledExecutor().execute(new Runnable()
		{
			public void run()
			{
				try
				{
					js_executeProgram(program, params, environmentVars, startDir);
				}
				catch (ApplicationException e)
				{
					application.handleException(application.getI18NMessage("servoy.error.executing.program") + program, e);
				}
			}
		});
	}

	/**
	 * @clonedesc js_executeProgramInBackground(String,String[],String[],String)
	 *
	 * @sampleas js_executeProgramInBackground(String,String[],String[],String)
	 *
	 * @param program (fullpath) of the program to execute
	 * @param params an array of strings as program arguments
	 * @param environmentVars array of strings, each element of which has environment variable settings in the format name=value, or null if the subprocess should inherit the environment of the current process.
	 * @throws ApplicationException
	 */
	public void js_executeProgramInBackground(final String program, final String[] params, final String[] environmentVars) throws ApplicationException
	{
		js_executeProgramInBackground(program, params, environmentVars, null);
	}

	/**
	 * @clonedesc js_executeProgramInBackground(String,String[],String[],String)
	 *
	 * @sampleas js_executeProgramInBackground(String,String[],String[],String)
	 *
	 * @param program (fullpath) of the program to execute
	 * @param params an array of strings as program arguments
	 * @throws ApplicationException
	 */
	public void js_executeProgramInBackground(final String program, final String[] params) throws ApplicationException
	{
		js_executeProgramInBackground(program, params, null, null);
	}

	/**
	 * @clonedesc js_executeProgramInBackground(String,String[],String[],String)
	 *
	 * @sampleas js_executeProgramInBackground(String,String[],String[],String)
	 *
	 * @param program (fullpath) of the program to execute
	 *
	 * @throws ApplicationException
	 */
	public void js_executeProgramInBackground(String program) throws ApplicationException
	{
		js_executeProgramInBackground(program, null, null, null);
	}


	/**
	 * Produces a "beep" sound; commonly used to indicate an error or warning dialog.
	 *
	 * @sample application.beep();
	 */
	@ServoyClientSupport(ng = false, mc = false, wc = false, sc = true)
	public void js_beep()
	{
		if (application instanceof ISmartClientApplication)
		{
			((ISmartClientApplication)application).beep();
		}
	}

	/**
	 * Play a sound (AU file, an AIFF file, a WAV file, and a MIDI file).
	 *
	 * @sample application.playSound('media:///click.wav');
	 *
	 * @param url URL of the sound file
	 */
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public void js_playSound(final String url)
	{
		application.getScheduledExecutor().execute(new Runnable()
		{
			public void run()
			{
				try
				{
					URL u = new URL(url);
					AudioClip clip = Applet.newAudioClip(u);
					clip.play();
				}
				catch (Exception e)
				{
					Debug.error(e);
				}
			}
		});
	}

	/**
	 * Shows an URL in a browser.
	 *
	 * @sample
	 * application.showURL('http://www.example.com');
	 *
	 * //webclient specific additional parameters...
	 * //2nd parameter: target frame or named dialog/window, so its possible to control in which (internal) frame or dialog the url is loaded, '_self' is current window,'_blank' is new dialog, '_top' is main window
	 * //3rd parameter: dialog options used when a dialog is specified, example: 'height=200,width=400,status=yes,toolbar=no,menubar=no,location=no'
	 * //3rd or 4th parameter: a timeout in seconds when the url should be shown, immediately/0 is default'
	 *
	 * @param url URL to show
	 *
	 * @return Boolean (true) if URL was shown
	 */
	public boolean js_showURL(String url)
	{
		return js_showURL(url, "_blank", null, 0); //$NON-NLS-1$
	}

	/**
	 * @clonedesc js_showURL(String)
	 *
	 * @sampleas js_showURL(String)
	 *
	 * @param url URL to show
	 * @param webclientTarget Target frame or named dialog/window
	 *
	 * @return Boolean (true) if URL was shown
	 */
	public boolean js_showURL(String url, String webclientTarget)
	{
		return js_showURL(url, webclientTarget, null, 0);
	}

	/**
	 * @clonedesc js_showURL(String)
	 *
	 * @sampleas js_showURL(String)
	 *
	 * @param url URL to show
	 * @param webclientTarget Target frame or named dialog/window
	 * @param webclientTargetOptions Dialog options used when a dialog is specified / a timeout in seconds when the url should be shown
	 *
	 * @return Boolean (true) if URL was shown
	 */
	public boolean js_showURL(String url, String webclientTarget, String webclientTargetOptions)
	{
		return js_showURL(url, webclientTarget, webclientTargetOptions, 0);
	}

	/**
	 * @clonedesc js_showURL(String)
	 *
	 * @sampleas js_showURL(String)
	 *
	 * @param url URL to show
	 * @param webclientTarget Target frame or named dialog/window
	 * @param timeout A timeout in seconds when the url should be shown
	 *
	 * @return Boolean (true) if URL was shown
	 */
	public boolean js_showURL(String url, String webclientTarget, Number timeout)
	{
		return js_showURL(url, webclientTarget, null, timeout);
	}

	/**
	 * @clonedesc js_showURL(String)
	 *
	 * @sampleas js_showURL(String)
	 *
	 * @param url URL to show
	 * @param webclientTarget Target frame or named dialog/window
	 * @param webclientTargetOptions Dialog options used when a dialog is specified / a timeout in seconds when the url should be shown
	 * @param timeout A timeout in seconds when the url should be shown
	 *
	 * @return Boolean (true) if URL was shown
	 */
	public boolean js_showURL(String url, String webclientTarget, String webclientTargetOptions, Number timeout)
	{
		int _timeout = Utils.getAsInteger(timeout);
		return application.showURL(url, webclientTarget, webclientTargetOptions, _timeout, true);
	}

	/**
	 * @deprecated As of release 2.x, replaced by {@link plugins.mail#sendMail(Object[])}.
	 */
	@Deprecated
	public boolean js_sendMail(Object[] args)
	{
		IClientPlugin cp = application.getPluginManager().getPlugin(IClientPlugin.class, "mail"); //$NON-NLS-1$
		if (cp != null)
		{
			IScriptable so = cp.getScriptObject();
			if (so != null)
			{
				try
				{
					Method m = so.getClass().getMethod("js_sendMail", getClassArrayFromObjectArgs(args)); //$NON-NLS-1$
					Boolean b = (Boolean)m.invoke(so, args);
					return b.booleanValue();
				}
				catch (Exception e)
				{
					Debug.error(e);
				}
			}
		}
		application.reportError("Send mail failed",
			"For sending mail the mail plugin is needed\nNote this method is deprecated, use the plugin directly in your code");
		return false;
	}

	/**
	 * Get the name of the localhost.
	 *
	 * @sample var hostName = application.getHostName();
	 *
	 * @return Name of the localhost
	 */
	public String js_getHostName()
	{
		ClientInfo ci = application.getClientInfo();
		return ci.getHostName();
	}

	/**
	 * Get the clients' IP address.
	 *
	 * @sample var ip = application.getIPAddress();
	 *
	 * @return IP address of the client
	 */
	public String js_getIPAddress()
	{
		ClientInfo ci = application.getClientInfo();
		return ci.getHostAddress();
	}

	/**
	 * Sets a string object in the clipboard.
	 *
	 * @sample application.setClipboardContent('test');
	 *
	 * @param string New content of the clipboard
	 */
	@ServoyClientSupport(ng = false, mc = false, wc = false, sc = true)
	public void js_setClipboardContent(Object string)
	{
		if (application instanceof ISmartClientApplication && string instanceof String)
		{
			((ISmartClientApplication)application).setClipboardContent((String)string);
		}
	}

	/**
	 * Gets a string from the clipboard, null if not a string or empty.
	 *
	 * @sample var fromClipboard = application.getClipboardString();
	 *
	 * @return The string from the clipboard
	 */
	@ServoyClientSupport(ng = false, mc = false, wc = false, sc = true)
	public String js_getClipboardString()
	{
		if (application instanceof ISmartClientApplication)
		{
			return ((ISmartClientApplication)application).getClipboardString();
		}
		return null;
	}

	/**
	 * Output something on the out stream. (if running in debugger view output console tab)
	 *
	 * @sample
	 * // log level is used to determine how/if to log in servoy_log.txt; for smart client java out and err streams are used
	 * application.output('my very important trace msg');// default log level: info
	 *
	 * @param msg Object to send to output stream
	 */
	@JSFunction
	public void output(Object msg)
	{
		js_output(msg, ILogLevel.INFO);
	}

	/**
	 * Output something on the out stream. (if running in debugger view output console tab)
	 *
	 * @sample
	 * // log level is used to determine how/if to log in servoy_log.txt; for smart client java out and err streams are used
	 * application.output('my very important msg',LOGGINGLEVEL.ERROR);// log level: error
	 *
	 * @param msg Object to send to output stream
	 * @param level the log level where it should log to.
	 */
	public void js_output(Object msg, int level)
	{
		if (msg instanceof Object[])
		{
			application.output(Utils.getScriptableString((Object[])msg), level);
		}
		else if (msg instanceof NativeError)
		{
			application.output(msg.toString() + '\n' + ((NativeError)msg).getStack(), level);
		}
		else if (msg instanceof Scriptable)
		{
			application.output(Utils.getScriptableString((Scriptable)msg), level);
		}
		else
		{
			application.output(msg, level);
		}
	}

	/**
	 * Undo last action (if possible).
	 *
	 * @sample application.undo();
	 */
	@ServoyClientSupport(ng = false, wc = false, sc = true)
	public void js_undo()
	{
		AbstractAction cmd = (AbstractAction)application.getCmdManager().getRegisteredAction("cmdundo"); //$NON-NLS-1$
		if (cmd != null)
		{
			cmd.actionPerformed(null);
		}
	}

	/**
	 * Redo last action (if possible).
	 *
	 * @sample application.redo();
	 */
	@ServoyClientSupport(ng = false, wc = false, sc = true)
	public void js_redo()
	{
		AbstractAction cmd = (AbstractAction)application.getCmdManager().getRegisteredAction("cmdredo"); //$NON-NLS-1$
		if (cmd != null)
		{
			cmd.actionPerformed(null);
		}
	}

	/**
	 * Returns a date object initialized in client with current date and time.
	 * This should be used instead of new Date() for webclients when the clients are in different times zones then the server.
	 * Then this call will really return a time that is the locals webclients time.
	 *
	 * @sample var clienttime = application.getTimeStamp();
	 *
	 * @return Current time at the client
	 */
	public Date js_getTimeStamp()
	{
		return TimezoneUtils.getClientDate(application);
	}

	/**
	 * Returns a date object initialized on server with current date and time.
	 *
	 * @sample var servertime = application.getServerTimeStamp();
	 *
	 * @return Server time
	 */
	public Date js_getServerTimeStamp() throws Exception
	{
		return application.getClientHost().getServerTime(application.getClientID());
	}

	/**
	 * Get a new UUID (also known as GUID)
	 *
	 * @sample var new_uuid = application.getNewUUID();
	 *
	 * @deprecated As of release 5.0, replaced by {@link #getUUID()}.
	 */
	@Deprecated
	public String js_getNewUUID()
	{
		return UUID.randomUUID().toString();
	}

	/**
	 * Get a new UUID object (also known as GUID) or convert the parameter (that can be string or byte array) to an UUID object. A table column marked as UUID will work with such objects.
	 *
	 * @sample
	 * var new_uuid_object = application.getUUID(); // generate new uuid object
	 * var uuid_object1 = application.getUUID(new_uuid_object.toString()); // convert a string representing an uuid to an uuid object
	 * var uuid_object2 = application.getUUID(new_uuid_object.toBytes());  // convert a byte array representing an uuid to an uuid object
	 *
	 * @return The new UUID object
	 */
	public UUID js_getUUID()
	{
		return Utils.getAsUUID(UUID.randomUUID().toString(), false);
	}


	/**
	 * Get a new UUID object (also known as GUID) or convert the parameter (that can be string or byte array) to an UUID object. A table column marked as UUID will work with such objects.
	 *
	 * @sample
	 * var new_uuid_object = application.getUUID(); // generate new uuid object
	 * var uuid_object1 = application.getUUID(new_uuid_object.toString()); // convert a string representing an uuid to an uuid object
	 * var uuid_object2 = application.getUUID(new_uuid_object.toBytes());  // convert a byte array representing an uuid to an uuid object
	 *
	 * @param uuidString String representing an uuid
	 *
	 * @return The new UUID object
	 */
	public UUID js_getUUID(String uuidString)
	{
		if (uuidString == null) return js_getUUID();
		return Utils.getAsUUID(uuidString, false);
	}

	/**
	 * Get a new UUID object (also known as GUID) or convert the parameter (that can be string or byte array) to an UUID object. A table column marked as UUID will work with such objects.
	 *
	 * @sample
	 * var new_uuid_object = application.getUUID(); // generate new uuid object
	 * var uuid_object1 = application.getUUID(new_uuid_object.toString()); // convert a string representing an uuid to an uuid object
	 * var uuid_object2 = application.getUUID(new_uuid_object.toBytes());  // convert a byte array representing an uuid to an uuid object
	 *
	 * @param byteArray Byte array representing an uuid
	 *
	 * @return The new UUID object
	 */
	public UUID js_getUUID(byte[] byteArray)
	{
		if (byteArray == null) return js_getUUID();
		return Utils.getAsUUID(byteArray, false);
	}

	/**
	 * Returns the application version.
	 *
	 * @sample application.getVersion();
	 *
	 * @return Application version
	 */
	public String js_getVersion()
	{
		return ClientVersion.getVersion();
	}

	/**
	 * Show the form specified by the parameter, that can be a name (is case sensitive!) or a form object.
	 * This will show the form in the active/currently focused window. So when called from a form in a dialog the dialog will show the form.
	 *
	 * @sample application.showForm('MyForm');
	 *
	 * @param form Form object or name
	 */
	public void js_showForm(Object form)
	{
		String f = null;
		if (form instanceof BasicFormController)
		{
			f = ((BasicFormController)form).getName();
		}
		else if (form instanceof FormScope)
		{
			f = ((FormScope)form).getFormController().getName();
		}
		else if (form instanceof BasicFormController.JSForm)
		{
			f = ((BasicFormController.JSForm)form).getFormPanel().getName();
		}
		else if (form instanceof String)
		{
			f = (String)form;
		}
		if (f != null)
		{
			application.getFormManager().showFormInCurrentContainer(f);
		}
	}

	/**
	 * Test if the form is currently showing in a window.
	 *
	 * @deprecated As of release 6.0, dialogs/windows API has been rewritten (based in JSWindow objects).
	 *
	 * @sample
	 * if(application.isFormInWindow(forms.formname))
	 * {
	 * 	//close window
	 * }
	 *
	 * @param form Form object or name
	 *
	 * @return Boolean (true) if the form is showing in a window, (false) otherwise
	 *
	 */
	@Deprecated
	public boolean js_isFormInWindow(Object form)
	{
		IFormController fp = getFormController(form);

		// can't test if the form is visible, in onhide the form controller is already made not visible.
		// but it is still in a visible dialog.
		if (fp != null/* && fp.isFormVisible() */)
		{
			if (fp.getFormUI() instanceof IFormUIInternal) return ((IFormUIInternal< ? >)fp.getFormUI()).isFormInWindow();
			else Debug.warn("Use of no-longer-supported deprecated method detected: application.isFormInWindow. Returning false."); //$NON-NLS-1$
		}
		return false;
	}

	/**
	 * @deprecated As of release 6.0, dialogs/windows API has been rewritten (based in JSWindow objects).
	 */
	@Deprecated
	public boolean js_isFormInDialog(Object form)
	{
		return js_isFormInWindow(form);
	}

	private IFormController getFormController(Object form)
	{
		IFormController fp = null;
		if (form instanceof BasicFormController)
		{
			fp = (BasicFormController)form;
		}
		else if (form instanceof FormScope)
		{
			fp = ((FormScope)form).getFormController();
		}
		else if (form instanceof BasicFormController.JSForm)
		{
			fp = ((BasicFormController.JSForm)form).getFormPanel();
		}
		else if (form instanceof String)
		{
			fp = application.getFormManager().leaseFormPanel((String)form);
		}
		return fp;
	}

	/**
	 * Creates a new window that can be used for displaying forms. Initially the window is not visible.
	 * If there is already a window with the given name, it will be closed and destroyed prior to creating the new window.
	 * Use the form controller show() and showRecords() methods in order to show a form in this window.
	 *
	 * @sample
	 * // create and show a window, with specified title, initial location and size
	 * // type of the window can be one of JSWindow.DIALOG, JSWindow.MODAL_DIALOG, JSWindow.WINDOW
	 * // If parentWindow is not specified, the current window will be used as parent; parentWindow parameter is only used by dialogs
	 * var win = application.createWindow("windowName", JSWindow.WINDOW);
	 * win.setInitialBounds(10, 10, 300, 300);
	 * win.title = "This is a window";
	 * controller.show(win);
	 * // create and show a non-modal dialog with default initial bounds/title
	 * var nmd = application.createWindow("nonModalDialogName", JSWindow.DIALOG);
	 * controller.showRecords(15, nmd); // 15 is a single-number pk in this case
	 *
	 * @param windowName the name of the window. Should not be null.
	 * @param type the type of the window. Can be one of JSWindow.DIALOG, JSWindow.MODAL_DIALOG, JSWindow.WINDOW.
	 * @return the newly created window.
	 */
	public JSWindow js_createWindow(String windowName, int type)
	{
		return application.getRuntimeWindowManager().createWindow(replaceFailingCharacters(windowName), type, null).getJSWindow();
	}

	/**
	 * Creates a new window that can be used for displaying forms. Initially the window is not visible.
	 * If there is already a window with the given name, it will be closed and destroyed prior to creating the new window.
	 * Use the form controller show() and showRecords() methods in order to show a form in this window.
	 *
	 * @sample
	 * // create and show a window, with specified title, initial location and size
	 * var win = application.createWindow("windowName", JSWindow.WINDOW);
	 * win.setInitialBounds(10, 10, 300, 300);
	 * win.title = "This is a window";
	 * controller.show(win);
	 * // create and show a non-modal dialog with default initial bounds/title
	 * var nmd = application.createWindow("nonModalDialogName", JSWindow.DIALOG);
	 * controller.showRecords(15, nmd); // 15 is a single-number pk in this case
	 *
	 * @param windowName the name of the window. Should not be null.
	 * @param type the type of the window. Can be one of JSWindow.DIALOG, JSWindow.MODAL_DIALOG, JSWindow.WINDOW.
	 * @param parentWindow the parent JSWindow object. If it is not specified, the current window will be used as parent. This parameter is only used by dialogs.
	 * @return the newly created window.
	 */
	public JSWindow js_createWindow(String windowName, int type, JSWindow parentWindow)
	{
		return application.getRuntimeWindowManager().createWindow(replaceFailingCharacters(windowName), type, parentWindow).getJSWindow();
	}

	/**
	 * Get a window by window name. When not supplying a name, the main application window is grabbed.
	 *
	 * @sample
	 * // close and dispose window resources
	 * var win = application.getWindow("someWindowName");
	 * if (win != null) {
	 * 	win.destroy();
	 * }
	 *
	 * @param name the name of the window. If not specified, the main application JSWindow will be returned.
	 * @return the JSWindow with the specified name, or null if no such window exists.
	 */
	public JSWindow js_getWindow(String name)
	{
		if (name != null)
		{
			RuntimeWindow jw = application.getRuntimeWindowManager().getWindow(replaceFailingCharacters(name));
			return jw != null ? jw.getJSWindow() : null;
		}
		else
		{
			// we must return the JSWindow wrapper for main application frame
			return application.getRuntimeWindowManager().getWindow(null).getJSWindow();
		}
	}

	/**
	 * Get the main application window. This is the window that is created first for this client.
	 *
	 * In a smart client this is always just the first started window where the solution is loaded in.
	 * In a webclient the user may open the same solution in a new tab in the same browser. In that case the main solution
	 * window will always be the first opened tab, even if that one was already closed.
	 * application.getActiveWindow() will always return the currently active/focused window or dialog.
	 * If you need the window of the current top-level form, controller.getWindow() of that form will
	 * always return the correct window.
	 *
	 * @sample
	 * // close and dispose window resources
	 * var mainAppWindow = application.getWindow();
	 *
	 * @return the main application JSWindow.
	 */
	public JSWindow js_getWindow()
	{
		return js_getWindow(null);
	}

	/**
	 * This gets the currently focused active window; this can be the main application window or a modal dialog.
	 * For a webclient getWindow() can return the main window that is not really the main for the current tab in the browser
	 * that can return the previous tab that a user could have opened. For this method is better suited because this
	 * will give you the actual tab in the browser. Another call would be form.controller.getWindow() of a form that you know in which window it resides.
	 *
	 * @sample
	 * // get the currently active/focused window or dialog
	 * var activeWindow = application.getActiveWindow();
	 *
	 * @return the current active/focussed window.
	 */
	public JSWindow js_getActiveWindow()
	{
		return application.getRuntimeWindowManager().getCurrentWindow().getJSWindow();
	}

	/**
	 * Show the specified form in a dialog. (NOTE: x, y, width, height are initial bounds - applied only the fist time a dialog is shown)
	 *
	 * NOTE:
	 * In the Smart Client, no code is executed after the function showFormInDialog <em>if the dialog is modal</em>.
	 *
	 * NOTE:
	 * x, y, width and height coordinates are only applied the first time the specified dialog is shown.
	 * Use APP_UI_PROPERTY.DIALOG_FULL_SCREEN for these values when the dialog should be full-screen.
	 *
	 * @deprecated As of release 6.0, dialogs/windows API has been rewritten (based in JSWindow objects)
	 *
	 * @sample
	 * //Show the specified form in a modal dialog, on default initial location and size (x,y,w,h)
	 * //application.showFormInDialog(forms.contacts);
	 * //Note: No code is executed after the showFormInDialog until the dialog is closed if it is created as a modal dialog.
	 * //Show the specified form in a non-modal dialog with a specified name, on default initial location and size (x,y,w,h)
	 * //application.showFormInDialog(forms.contacts,'contactsdialog',false);
	 * //Show the specified form in a modal dialog, at a specified initial location and size with custom title, not resizable but with text toolbar
	 * application.showFormInDialog(forms.contacts,100,80,500,300,'my own dialog title',false,true,'mydialog',true);
	 *
	 * @param form The form to be shown in the dialog.
	 *
	 * @param x optional The "x" coordinate of the dialog.
	 *
	 * @param y optional The "y" coordinate of the dialog.
	 *
	 * @param width optional The width of the dialog.
	 *
	 * @param height optional The height of the dialog.
	 *
	 * @param dialogTitle optional The title of the dialog.
	 *
	 * @param resizable optional <em>true</em> if the dialog size should be modifiable; <em>false</em> if not.
	 *
	 * @param showTextToolbar optional <em>true</em> to add a text toolbar; <em>false</em> to not add a text toolbar.
	 *
	 * @param windowName optional The name of the window; defaults to "dialog" if nothing is specified. Window and dialog names share the same namespace.
	 *
	 * @param modal optional <em>true</em> if the dialog should be modal; <em>false</em> if not. Defaults to <em>true</em>.
	 */
	@Deprecated
	public void js_showFormInDialog(Object[] args) throws ServoyException
	{
		if (args != null && args.length >= 1)
		{
			Object form = args[0];
			int x = -1;
			int y = -1;
			int w = 0;
			int h = 0;
			String title = null;
			boolean resizeble = true;
			boolean showTextToolbar = false;
			Boolean closeAll = Boolean.FALSE;
			boolean legacyBehaviour = false;
			boolean modal = true;
			String dialogName = null;

			// special short cut, args: 'form,name [, modal]'
			if (args.length >= 2 && args[1] instanceof String)
			{
				dialogName = (String)args[1];
				if (args.length >= 3 && args[2] instanceof Boolean) modal = ((Boolean)args[2]).booleanValue();
				else modal = true;
			}
			else
			{
				if (args.length >= 2 && args[1] instanceof Number) x = ((Number)args[1]).intValue();
				if (args.length >= 3 && args[2] instanceof Number) y = ((Number)args[2]).intValue();
				if (args.length >= 4 && args[3] instanceof Number) w = ((Number)args[3]).intValue();
				if (args.length >= 5 && args[4] instanceof Number) h = ((Number)args[4]).intValue();
				if (args.length >= 6 && args[5] instanceof String) title = args[5].toString();
				if (args.length >= 7 && args[6] instanceof Boolean) resizeble = ((Boolean)args[6]).booleanValue();
				if (args.length >= 8 && args[7] instanceof Boolean) showTextToolbar = ((Boolean)args[7]).booleanValue();
				if (args.length >= 9 && args[8] instanceof Boolean) closeAll = (Boolean)args[8];

				// up to here the arguments are legacy v3 arguments (closeAll is only supported if dialogName is not supplied)

				if (args.length >= 9 && args[8] instanceof String) dialogName = (String)args[8];
				else legacyBehaviour = true;
				if (args.length >= 10 && args[9] instanceof Boolean) modal = ((Boolean)args[9]).booleanValue();
				else if (dialogName != null) modal = true;
			}
			dialogName = replaceFailingCharacters(dialogName);

			String f = null;
			if (form instanceof BasicFormController)
			{
				f = ((BasicFormController)form).getName();
			}
			else if (form instanceof FormScope)
			{
				f = ((FormScope)form).getFormController().getName();
			}
			else if (form instanceof BasicFormController.JSForm)
			{
				f = ((BasicFormController.JSForm)form).getFormPanel().getName();
			}
			else if (form instanceof String)
			{
				f = (String)form;
			}
			if (f != null)
			{
				Form frm = application.getFlattenedSolution().getForm(f);
				IBasicFormManager fm = application.getFormManager();
				if (frm == null && fm.isPossibleForm(f)) frm = fm.getPossibleForm(f);
				if (!application.getFlattenedSolution().formCanBeInstantiated(frm))
				{
					// abstract form
					throw new ApplicationException(ServoyException.ABSTRACT_FORM);
				}

				Rectangle rect = new Rectangle(x, y, w, h);
				fm.showFormInDialog(f, rect, title, resizeble, showTextToolbar, closeAll.booleanValue() || !legacyBehaviour, modal, dialogName);
			}
		}
	}

	/**
	 * Show the specified form in a window. (NOTE: x, y, width, height are initial bounds - applied only the fist time a window is shown)
	 *
	 * NOTE:
	 * Forms in windows cannot be modal. They are more independent then dialogs, even non-modal ones. For example in SC, a non-modal dialog will always
	 * be shown on top of the parent window and it will not have a separate entry in the OS window manager (for example Windows taskbar).
	 *
	 * NOTE:
	 * x, y, width and height coordinates are only applied the first time the specified window is shown.
	 * Use APP_UI_PROPERTY.FULL_SCREEN for these values when the window should be full-screen.
	 *
	 * @deprecated As of release 6.0, dialogs/windows API has been rewritten (based in JSWindow objects)
	 *
	 * @sample
	 * //Show the specified form in a window, on default initial location and size
	 * //application.showFormInWindow(forms.contacts);
	 * //Show the specified form in a window with a specified name, on default initial location and size
	 * //application.showFormInWindow(forms.contacts,'contactsWindow');
	 * //Show the specified form in a window, at a specified initial location and size with custom title, not resizable but with text toolbar
	 * application.showFormInWindow(forms.contacts,100,80,500,300,'my own window title',false,true,'mywindow');
	 *
	 * @param form The form to be shown in the dialog.
	 * @param x optional The "x" coordinate of the dialog.
	 * @param y optional The "y" coordinate of the dialog.
	 * @param width optional The width of the dialog.
	 * @param height optional The height of the dialog.
	 * @param dialogTitle optional The title of the dialog.
	 * @param resizable optional <em>true</em> if the dialog size should be modifiable; <em>false</em> if not.
	 * @param showTextToolbar optional <em>true</em> to add a text toolbar; <em>false</em> to not add a text toolbar.
	 * @param windowName optional The name of the window; defaults to "dialog" if nothing is specified. Window and dialog names share the same namespace.
	 */
	@Deprecated
	public void js_showFormInWindow(Object[] args) throws ServoyException
	{
		//if (application.getMainApplicationFrame() == null) return;

		if (args != null && args.length >= 1)
		{
			Object form = args[0];
			int x = -1;
			int y = -1;
			int w = 0;
			int h = 0;
			String title = null;
			boolean resizeble = true;
			boolean showTextToolbar = false;
			String windowName = null;

			// special short cut, args: 'form,name'
			if (args.length >= 2 && args[1] instanceof String)
			{
				windowName = (String)args[1];
			}
			else
			{
				if (args.length >= 2 && args[1] instanceof Number) x = ((Number)args[1]).intValue();
				if (args.length >= 3 && args[2] instanceof Number) y = ((Number)args[2]).intValue();
				if (args.length >= 4 && args[3] instanceof Number) w = ((Number)args[3]).intValue();
				if (args.length >= 5 && args[4] instanceof Number) h = ((Number)args[4]).intValue();
				if (args.length >= 6 && args[5] instanceof String) title = args[5].toString();
				if (args.length >= 7 && args[6] instanceof Boolean) resizeble = ((Boolean)args[6]).booleanValue();
				if (args.length >= 8 && args[7] instanceof Boolean) showTextToolbar = ((Boolean)args[7]).booleanValue();
				if (args.length >= 9 && args[8] instanceof String) windowName = (String)args[8];
			}
			windowName = replaceFailingCharacters(windowName);

			String formName = null;
			if (form instanceof BasicFormController)
			{
				formName = ((BasicFormController)form).getName();
			}
			else if (form instanceof FormScope)
			{
				formName = ((FormScope)form).getFormController().getName();
			}
			else if (form instanceof BasicFormController.JSForm)
			{
				formName = ((FormController.JSForm)form).getFormPanel().getName();
			}
			else if (form instanceof String)
			{
				formName = (String)form;
			}
			if (formName != null)
			{
				Form frm = application.getFlattenedSolution().getForm(formName);
				IBasicFormManager fm = application.getFormManager();
				if (frm == null && fm.isPossibleForm(formName)) frm = fm.getPossibleForm(formName);
				if (!application.getFlattenedSolution().formCanBeInstantiated(frm))
				{
					// abstract form
					throw new ApplicationException(ServoyException.ABSTRACT_FORM);
				}

				Rectangle bounds = new Rectangle(x, y, w, h);
				fm.showFormInFrame(formName, bounds, title, resizeble, showTextToolbar, windowName);
			}
		}
	}

	/**
	 * Close all visible windows (except main application window). Returns true if operation was successful.
	 *
	 * @sample
	 * var win = application.createWindow("aWindowName", JSWindow.WINDOW, null);
	 * win.setInitialBounds(10, 10, 300, 300);
	 * win.title = "This is a window";
	 * controller.show(win);
	 *
	 * var win2 = application.createWindow("anotherWindowName", JSWindow.WINDOW, null);
	 * win2.setInitialBounds(100, 100, 300, 300);
	 * win2.title = "This is another window";
	 * controller.show(win2);
	 *
	 * var qdialog = plugins.dialogs.showQuestionDialog("QuestionDialog","Do you want to close the windows?","Yes","No");
	 * if (qdialog == "Yes") {
	 * 	application.closeAllWindows();
	 * 	controller.show(null);
	 * }
	 *
	 * @return Boolean true if all windows were closed and false otherwise.
	 */
	public boolean js_closeAllWindows()
	{
		return application.getRuntimeWindowManager().closeFormInWindow(null, true);
	}

	/**
	 * Close the dialog with the given name (call this method to hide the form shown with 'showFormInDialog'). If (true) is passed, then all the windows will be closed. If the name is missing or null, the default dialog will be closed.
	 *
	 * @deprecated As of release 6.0, dialogs/windows API has been rewritten (based in JSWindow objects).
	 *
	 * @sample
	 * application.closeFormDialog(); // closes the current dialog
	 * //application.closeFormDialog('dialogname'); //closes the dialog with the specific name
	 *
	 * @param dialogName/closeAll optional Name of the dialog to close, or (true) to close all open dialogs.
	 *
	 * @return Boolean (true) if the dialog(s) were closed, (false) otherwise
	 */
	@Deprecated
	public boolean js_closeFormDialog(Object[] vargs)
	{
		return js_closeForm(vargs);
	}

	/**
	 * Close the dialog/window with the given name (call this method to hide the form shown with 'showFormInDialog' or 'showFormInWindow'). If (true) is passed, then all the windows/dialogs will be closed. If the name is missing or null, the default dialog/window will be closed.
	 *
	 * @deprecated As of release 6.0, dialogs/windows API has been rewritten (based in JSWindow objects).
	 *
	 * @sample
	 * application.closeForm(); // closes the current dialog/window
	 * //application.closeForm('windowOrDialogName'); //closes the dialog/window with this specific name
	 *
	 * @param windowOrDialogName/closeAll optional Name of the dialog/window to close, or (true) to close all open dialogs/windows.
	 *
	 * @return Boolean (true) if the dialog(s)/window(s) were closed, (false) otherwise
	 */
	@Deprecated
	public boolean js_closeForm(Object[] vargs)
	{
		String dialogName = null;

		RuntimeWindowManager wm = application.getRuntimeWindowManager();
		boolean all = false;
		if (vargs != null && vargs.length >= 1)
		{
			if (vargs[0] instanceof Boolean)
			{
				// legacy V3 boolean argument, not supported when dialogName is supplied (new behavior)
				all = ((Boolean)vargs[0]).booleanValue();
				if (all)
				{
					return wm.closeFormInWindow("", true);
				}
			}
			else if (vargs[0] instanceof String)
			{
				dialogName = replaceFailingCharacters((String)vargs[0]);
				all = true;
			}
		}
		boolean closed = wm.closeFormInWindow(dialogName, all);
		if (closed)
		{
			//make sure editing is stopped and changes are reflected in other forms
			application.getFoundSetManager().getEditRecordList().stopEditing(false);
		}
		return closed;
	}

	private String replaceFailingCharacters(String dialogName)
	{
		if (dialogName == null)
		{
			return null;
		}
		return dialogName.replace(' ', '_').replace(':', '_');
	}

	/**
	 * Show the calendar, returns selected date or null if canceled. Initial value and date format can be also specified.
	 *
	 * @sample var selectedDate = application.showCalendar();
	 *
	 * @return Selected date or null if canceled
	 */
	@ServoyClientSupport(ng = false, mc = false, wc = false, sc = true)
	public Date js_showCalendar()
	{
		return js_showCalendar(null, null);
	}

	/**
	 * @clonedesc js_showCalendar()
	 *
	 * @sampleas js_showCalendar()
	 *
	 * @param selectedDate Default selected date
	 *
	 * @return Selected date or null if canceled
	 */
	@ServoyClientSupport(ng = false, mc = false, wc = false, sc = true)
	public Date js_showCalendar(Date selectedDate)
	{
		return js_showCalendar(selectedDate, null);
	}

	/**
	 * @clonedesc js_showCalendar()
	 *
	 * @sampleas js_showCalendar()
	 *
	 * @param dateFormat Date format
	 *
	 * @return Selected date or null if canceled
	 */
	@ServoyClientSupport(ng = false, mc = false, wc = false, sc = true)
	public Date js_showCalendar(String dateFormat)
	{
		return js_showCalendar(null, dateFormat);
	}

	/**
	 * @clonedesc js_showCalendar()
	 *
	 * @sampleas js_showCalendar()
	 *
	 * @param selectedDate Default selected date
	 * @param dateFormat Date format
	 *
	 * @return Selected date or null if canceled
	 */
	@ServoyClientSupport(ng = false, mc = false, wc = false, sc = true)
	public Date js_showCalendar(Date selectedDate, String dateFormat)
	{
		if (application instanceof ISmartClientApplication)
		{
			return ((ISmartClientApplication)application).showCalendar(dateFormat, selectedDate);
		}
		return null;
	}

	/**
	 * Show the colorChooser. Returned value is in format #RRGGBB or null if canceled.
	 *
	 * @sample var selectedColor = application.showColorChooser();
	 *
	 * @return selected color or null if canceled
	 */
	@ServoyClientSupport(ng = false, mc = false, wc = false, sc = true)
	public String js_showColorChooser()
	{
		return js_showColorChooser(null);
	}

	/**
	 * @clonedesc js_showColorChooser()
	 *
	 * @sampleas js_showColorChooser()
	 *
	 * @param colorString Default color
	 *
	 * @return selected color or null if canceled
	 */
	@ServoyClientSupport(ng = false, mc = false, wc = false, sc = true)
	public String js_showColorChooser(String colorString)
	{
		if (application instanceof ISmartClientApplication)
		{
			return ((ISmartClientApplication)application).showColorChooser(colorString);
		}

		return null;
	}

	/**
	 * Show the font chooser dialog. Returns the selected font. Can specify a default font.
	 *
	 * @sample
	 * var selectedFont = application.showFontChooser();
	 * elements.myfield.font = selectedFont
	 *
	 * @return selected font
	 */
	@ServoyClientSupport(ng = false, mc = false, wc = false, sc = true)
	public String js_showFontChooser()
	{
		return js_showFontChooser(null);
	}

	/**
	 * @clonedesc js_showFontChooser()
	 *
	 * @sampleas js_showFontChooser()
	 *
	 * @param defaultFont Default font
	 *
	 * @return selected font
	 */
	@ServoyClientSupport(ng = false, mc = false, wc = false, sc = true)
	public String js_showFontChooser(String defaultFont)
	{
		if (application instanceof ISmartClientApplication)
		{
			return ((ISmartClientApplication)application).showFontChooser(defaultFont);
		}

		return null;
	}

	/**
	 * Opens the i18n dialog so users can change translations. Returns the key selected by the user (not it's translation)  or null if cancel is pressed. Optional parameters specify the initial selections in the dialog.
	 *
	 * @sample application.showI18NDialog("servoy.button.close", "en");
	 *
	 * @return selected I18N key or null if cancel is pressed
	 */
	@ServoyClientSupport(ng = false, mc = false, wc = false, sc = true)
	public String js_showI18NDialog()
	{
		return js_showI18NDialog(null, null);
	}

	/**
	 * @clonedesc js_showI18NDialog()
	 *
	 * @sampleas js_showI18NDialog()
	 *
	 * @param keyToSelect Default selected key
	 *
	 * @return selected I18N key or null if cancel is pressed
	 */
	@ServoyClientSupport(ng = false, mc = false, wc = false, sc = true)
	public String js_showI18NDialog(String keyToSelect)
	{
		return js_showI18NDialog(keyToSelect, null);
	}

	/**
	 * @clonedesc js_showI18NDialog()
	 *
	 * @sampleas js_showI18NDialog()
	 *
	 * @param keyToSelect Default selected key
	 *
	 * @param languageToSelect Default selected language
	 *
	 * @return selected I18N key or null if cancel is pressed
	 */
	@ServoyClientSupport(ng = false, mc = false, wc = false, sc = true)
	public String js_showI18NDialog(String keyToSelect, String languageToSelect)
	{
		if (application instanceof ISmartClientApplication)
		{
			return ((ISmartClientApplication)application).showI18NDialog(keyToSelect, languageToSelect);
		}
		return null;
	}

	/**
	 * @deprecated As of release 3.0, replaced by {@link plugins.images#getImage(Object)}.
	 */
	@Deprecated
	public byte[] js_createThumbnailJPGImage(Object obj, int width, int height)
	{
		return js_createJPGImage(obj, width, height);
	}

	/**
	 * @deprecated As of release 3.0, replaced by {@link plugins.images#getImage(Object)}.
	 */
	@Deprecated
	public byte[] js_createJPGImage(Object obj, int width, int height)
	{
		return SnapShot.createJPGImage(((ISmartClientApplication)application).getMainApplicationFrame(), obj, width, height);
	}

	/**
	 * Updates the UI (painting). If in a script an element changed and the script continues doing
	 * things, you can give an number in ms how long this can take.
	 * Warning: this gives the UI time to paint, but this also means that it will give the ui time to respond to all other events,
	 * so if a user keeps clicking on other stuff this will also be handled right away inside this call.
	 *
	 * NOTE:In NGClient, this method will send to browser all outstanding changes. If called too often (with many changes), can cause performance issues.
	 * @sample
	 * application.updateUI(500);
	 * //continue doing things
	 */
	@ServoyClientSupport(ng = true, mc = false, sc = true, wc = false)
	public void js_updateUI()
	{
		js_updateUI(100);
	}

	/**
	 * @clonedesc js_updateUI()
	 *
	 * @sampleas js_updateUI()
	 *
	 * @param milliseconds How long the update should take in milliseconds
	 */
	@ServoyClientSupport(ng = true, mc = false, sc = true, wc = false)
	public void js_updateUI(int milliseconds)
	{
		application.updateUI(milliseconds < 100 ? 100 : milliseconds);
	}

	/**
	 * Returns the user name when logged in ('unknown' when not logged in)
	 *
	 * @sample var uname = application.getUserName();
	 *
	 * @deprecated As of release 2.0, replaced by {@link security#getUserName(Object[])}.
	 */
	@Deprecated
	public String js_getUserName()
	{
		String name = null;
		try
		{
			String n = application.getUserName();
			if (n != null && n.length() != 0) name = n;
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return name;
	}

	/**
	 * Returns the name of the operating system.
	 *
	 * @sample var osname = application.getOSName();
	 *
	 * @return Name of the operating system
	 */
	@SuppressWarnings("nls")
	public String js_getOSName()
	{
		return application.getClientOSName();
	}

	/**
	 * Set if numpad enter should behave like focus next.
	 *
	 * @sample application.setNumpadEnterAsFocusNextEnabled(true);
	 *
	 * @param enabled Boolean (true) if numpad enter should behave like focus next
	 */
	@ServoyClientSupport(ng = false, mc = false, wc = false, sc = true)
	public void js_setNumpadEnterAsFocusNextEnabled(boolean enabled)
	{
		if (application instanceof ISmartClientApplication)
		{
			((ISmartClientApplication)application).setNumpadEnterAsFocusNextEnabled(enabled);
		}
	}

	/**
	 * Enable/disable the auto save when clicking anywhere on a form and the focus get lost.
	 *
	 * @sample application.setFocusLostSaveEnabled(false);
	 *
	 * @deprecated  As of release 3.0, replaced by {@link databaseManager#setAutoSave(boolean)}.
	 */
	@Deprecated
	public void js_setFocusLostSaveEnabled(boolean b)
	{
		application.getFoundSetManager().getEditRecordList().setAutoSave(b);
	}

	private boolean didLastPrintPreviewPrint = false;

	public void setDidLastPrintPreviewPrint(boolean b)
	{
		didLastPrintPreviewPrint = b;
	}

	/**
	 * Check if the last printpreview did print.
	 *
	 * @sample
	 * //attached this method to onShow on the form being shown after printpreview
	 * //set a global called scopes.globals.showPrintPreview to 1 in the onPrintPreview method
	 * if (scopes.globals.showPrintPreview == 1)
	 * {
	 * 	scopes.globals.showPrintPreview = 0;//clear for next time
	 * 	if (application.isLastPrintPreviewPrinted())
	 * 	{
	 * 		plugins.dialogs.showInfoDialog('Alert',  'There is printed in printpreview',  'OK')
	 * 	}
	 * }
	 *
	 * @return Boolean (true) is the last print preview did print, (false) otherwise
	 */
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public boolean js_isLastPrintPreviewPrinted()
	{
		return didLastPrintPreviewPrint;
	}

	/**
	 * Sleep for specified time (in milliseconds).
	 *
	 * @sample
	 * //Sleep for 3 seconds
	 * application.sleep(3000);
	 *
	 * @param ms Sleep time in milliseconds
	 */
	public void js_sleep(int ms)
	{
		try
		{
			long startTime = System.currentTimeMillis();
			long stopTime = startTime + ms;

			long timeToWait = ms;
			while (timeToWait > 0)
			{
				if (timeToWait > 100)
				{
					SwingHelper.dispatchEvents(100);//make sure screen is updated (if there is a screen)
				}
				timeToWait = stopTime - System.currentTimeMillis();
				if (timeToWait > 100)
				{
					Thread.sleep(100);
					timeToWait -= 100;
				}
			}
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
	}

	/**
	 * @deprecated  As of release 2.x, replaced by {@link plugins.file#createTempFile(String, String)}.
	 */
	@Deprecated
	public Object js_createTempFile(String prefix, String suffix)
	{
		IClientPlugin cp = application.getPluginManager().getPlugin(IClientPlugin.class, "file"); //$NON-NLS-1$
		if (cp != null)
		{
			IScriptable so = cp.getScriptObject();
			if (so != null)
			{
				try
				{
					Method m = so.getClass().getMethod("js_createTempFile", new Class[] { String.class, String.class }); //$NON-NLS-1$
					Object obj = m.invoke(so, new Object[] { prefix, suffix });
					if (obj != null) return obj.toString();
					return null;
				}
				catch (Exception e)
				{
					Debug.error(e);
				}
			}
		}
		application.reportError("Create temp file failed",
			"For this operation the file plugin is needed\nNote this method is deprecated, use the plugin directly in your code");
		return null;
	}

	/**
	* @deprecated  As of release 2.x, replaced by {@link plugins.file#writeTXTFile(Object[])}.
	 */
	@Deprecated
	public boolean js_writeTXTFile(Object[] args)
	{
		IClientPlugin cp = application.getPluginManager().getPlugin(IClientPlugin.class, "file"); //$NON-NLS-1$
		if (cp != null)
		{
			IScriptable so = cp.getScriptObject();
			if (so != null)
			{
				try
				{
					Method m = so.getClass().getMethod("js_writeTXTFile", getClassArrayFromObjectArgs(args)); //$NON-NLS-1$
					Boolean obj = (Boolean)m.invoke(so, args);
					if (obj != null) return obj.booleanValue();
					return false;
				}
				catch (Exception e)
				{
					Debug.error(e);
				}
			}
		}
		application.reportError("Writing to text file failed",
			"For this operation the file plugin is needed\nNote this method is deprecated, use the plugin directly in your code");
		return false;
	}

	/**
	 * @deprecated  As of release 2.x, replaced by {@link plugins.file#writeXMLFile(Object, String)}.
	 */
	@Deprecated
	public boolean js_writeXMLFile(String fileName, String xml)
	{
		IClientPlugin cp = application.getPluginManager().getPlugin(IClientPlugin.class, "file"); //$NON-NLS-1$
		if (cp != null)
		{
			IScriptable so = cp.getScriptObject();
			if (so != null)
			{
				try
				{
					Method m = so.getClass().getMethod("js_writeXMLFile", new Class[] { String.class, String.class }); //$NON-NLS-1$
					Boolean obj = (Boolean)m.invoke(so, new Object[] { fileName, xml });
					if (obj != null) return obj.booleanValue();
					return false;
				}
				catch (Exception e)
				{
					Debug.error(e);
				}
			}
		}
		application.reportError("Writing to xml file failed",
			"For this operation the file plugin is needed\nNote this method is deprecated, use the plugin directly in your code");
		return false;
	}

	/**
	 * @deprecated  As of release 2.x, replaced by {@link plugins.file#writeFile(Object, byte[])}.
	 */
	@Deprecated
	public boolean js_writeFile(String fileName, byte[] data)
	{
		IClientPlugin cp = application.getPluginManager().getPlugin(IClientPlugin.class, "file"); //$NON-NLS-1$
		if (cp != null)
		{
			IScriptable so = cp.getScriptObject();
			if (so != null)
			{
				try
				{
					Method m = so.getClass().getMethod("js_writeFile", new Class[] { String.class, byte[].class }); //$NON-NLS-1$
					Boolean obj = (Boolean)m.invoke(so, new Object[] { fileName, data });
					if (obj != null) return obj.booleanValue();
					return false;
				}
				catch (Exception e)
				{
					Debug.error(e);
				}
			}
		}
		application.reportError("Writing to file failed",
			"For this operation the file plugin is needed\nNote this method is deprecated, use the plugin directly in your code");
		return false;
	}

	/**
	 * @deprecated  As of release 2.x, replaced by {@link plugins.file#readTXTFile(Object[])}.
	 */
	@Deprecated
	public String js_readTXTFile(Object[] args)
	{
		IClientPlugin cp = application.getPluginManager().getPlugin(IClientPlugin.class, "file"); //$NON-NLS-1$
		if (cp != null)
		{
			IScriptable so = cp.getScriptObject();
			if (so != null)
			{
				try
				{
					Method m = so.getClass().getMethod("js_readTXTFile", getClassArrayFromObjectArgs(args)); //$NON-NLS-1$
					Object obj = m.invoke(so, args);
					if (obj != null) return obj.toString();
					return null;
				}
				catch (Exception e)
				{
					Debug.error(e);
				}
			}
		}
		application.reportError("Reading from file failed",
			"For this operation the file plugin is needed\nNote this method is deprecated, use the plugin directly in your code");
		return null;
	}

	/**
	 * @deprecated  As of release 2.x, replaced by {@link plugins.file#showFileOpenDialog(Object[])}.
	 */
	@Deprecated
	public String js_showFileOpenDialog()
	{
		IClientPlugin cp = application.getPluginManager().getPlugin(IClientPlugin.class, "file"); //$NON-NLS-1$
		if (cp != null)
		{
			IScriptable so = cp.getScriptObject();
			if (so != null)
			{
				try
				{
					Method m = so.getClass().getMethod("js_showFileOpenDialog", new Class[] { }); //$NON-NLS-1$
					Object obj = m.invoke(so, (Object[])null);
					if (obj != null) return obj.toString();
					return null;
				}
				catch (Exception e)
				{
					Debug.error(e);
				}
			}
		}
		application.reportError("Showing file open dialog failed",
			"For this operation the file plugin is needed\nNote this method is deprecated, use the plugin directly in your code");
		return null;
	}

	/**
	 * @deprecated  As of release 2.x, replaced by {@link plugins.file#showFileSaveDialog(Object[])}.
	 */
	@Deprecated
	public String js_showFileSaveDialog(Object[] args)
	{
		IClientPlugin cp = application.getPluginManager().getPlugin(IClientPlugin.class, "file"); //$NON-NLS-1$
		if (cp != null)
		{
			IScriptable so = cp.getScriptObject();
			if (so != null)
			{
				try
				{
					Method m = so.getClass().getMethod("js_showFileSaveDialog", getClassArrayFromObjectArgs(args)); //$NON-NLS-1$
					Object obj = m.invoke(so, args);
					if (obj != null) return obj.toString();
					return null;
				}
				catch (Exception e)
				{
					Debug.error(e);
				}
			}
		}
		application.reportError("Showing file save dialog failed",
			"For this operation the file plugin is needed\nNote this method is deprecated, use the plugin directly in your code");
		return null;
	}

	private Class[] getClassArrayFromObjectArgs(Object[] args)
	{
		if (args == null) return null;
		Class[] clz = new Class[args.length];
		for (int i = 0; i < args.length; i++)
		{
			clz[i] = (args[i] != null ? args[i].getClass() : Object.class);
		}
		return clz;
	}

	/**
	 * @deprecated  As of release 2.x, replaced by {@link plugins.file#showDirectorySelectDialog(Object[])}.
	 */
	@Deprecated
	public String js_showDirectorySelectDialog(Object[] args)
	{
		IClientPlugin cp = application.getPluginManager().getPlugin(IClientPlugin.class, "file"); //$NON-NLS-1$
		if (cp != null)
		{
			IScriptable so = cp.getScriptObject();
			if (so != null)
			{
				try
				{
					Method m = so.getClass().getMethod("js_showDirectorySelectDialog", getClassArrayFromObjectArgs(args)); //$NON-NLS-1$
					Object obj = m.invoke(so, args);
					if (obj != null) return obj.toString();
					return null;
				}
				catch (Exception e)
				{
					Debug.error(e);
				}
			}
		}
		application.reportError("Showing directory select dialog failed",
			"For this operation the file plugin is needed\nNote this method is deprecated, use the plugin directly in your code");
		return null;
	}

	/**
	 * @deprecated  As of release 2.x, replaced by {@link plugins.file#readFile(Object[])}.
	 */
	@Deprecated
	public byte[] js_readFile(Object[] args)
	{
		IClientPlugin cp = application.getPluginManager().getPlugin(IClientPlugin.class, "file"); //$NON-NLS-1$
		if (cp != null)
		{
			IScriptable so = cp.getScriptObject();
			if (so != null)
			{
				try
				{
					Method m = so.getClass().getMethod("js_readFile", getClassArrayFromObjectArgs(args)); //$NON-NLS-1$
					return (byte[])m.invoke(so, args);
				}
				catch (Exception e)
				{
					Debug.error(e);
				}
			}
		}
		application.reportError("Reading file failed",
			"For this operation the file plugin is needed\nNote this method is deprecated, use the plugin directly in your code");
		return null;
	}

	public void setLastKeyModifiers(int m)
	{
		lastKeyModifiers = m;
	}

	private int lastKeyModifiers = 0;

	/**
	 * Returns the last key modifiers of last action (shift = 1,ctrl = 2,meta = 4,alt = 8)
	 *
	 * @deprecated As of release 5.0, replaced by JSEvent functionality; event object is received as parameter to event method.
	 *
	 * @sample
	 * //get the last key modifiers of last user action (shift = 1,ctrl = 2,meta = 4,alt = 8)
	 * var m = application.getLastKeyModifiers();
	 * if ( (m & 1) == 1)
	 * {
	 * 	//do shift action
	 * }
	 *
	 */
	@Deprecated
	public int js_getLastKeyModifiers()
	{
		return lastKeyModifiers;
	}

	/**
	 * Create a new form instance.
	 *
	 * @sample
	 * var ok = application.createNewFormInstance('orders','orders_view');
	 * if (ok)
	 * {
	 * 	var dialog = application.createWindow("myDialog", JSWindow.DIALOG);
	 *  dialog.show('orders_view')
	 * 	//forms['orders_view'].controller.show()
	 * 	//forms.xyz.elements.myTabPanel.addTab(forms['orders_view'])
	 * 	//forms['orders_view'].elements.mylabel.setLocation(10,20)
	 * }
	 *
	 * @param designFormName Name of the design form
	 * @param newInstanceScriptName Name of the new form instance
	 *
	 * @return Boolean (true) if the instance was created succesfully, (false) otherwise
	 */
	public boolean js_createNewFormInstance(String designFormName, String newInstanceScriptName)
	{
		return application.getFormManager().createNewFormInstance(designFormName, newInstanceScriptName);
	}

	/**
	 * @deprecated
	 */
	@Deprecated
	public boolean isCapturingErrors()
	{
		return isCapturingErrors;
	}

	/**
	 * @deprecated
	 */
	@Deprecated
	public void setLastErrorCode(int code)
	{
		lastErrorCode = code;
	}

	private boolean isCapturingErrors = false;
	private int lastErrorCode = 0;

	/**
	 * Returns the last error code or 0 if no error has happend, calling also clears errorCode
	 *
	 * @deprecated As of release 3.0, replaced by onError solution event.
	 *
	 * @sample
	 * var error = application.getLastErrorCode();
	 * //if (error != 0) showErrorDialog();
	 */
	@Deprecated
	public int js_getLastErrorCode()
	{
		int retval = lastErrorCode;
		lastErrorCode = 0;
		return retval;
	}

	/**
	 * Enable or disable the error capture, if enabled you can use getLastErrorCode()
	 *
	 * @deprecated As of release 3.0, replaced by onError solution event.
	 *
	 * @sample
	 * //turn on error capture
	 * application.setErrorCapture(true);
	 */
	@Deprecated
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public void js_setErrorCapture(boolean cap)
	{
		isCapturingErrors = cap;
	}

	/**
	 * Sets the application server URL for mobile client to connect to.
	 *
	 * @sample
	 * // if server URL is not set at mobile solution export, setServerURL should be called from custom login form, before authentication is done
	 * application.setServerURL('http://localhost:8080');
	 *
	 * @param applicationServerURL The URL to connect.
	 */
	@JSFunction
	public void setServerURL(String applicationServerURL)
	{
		//only for mobile
	}

	@Override
	public String toString()
	{
		return "JSApplication[Standard functions]";
	}

	/** Container for element that triggers current event method
	 *
	 * @author rgansevles
	 *
	 */
	public static class FormAndComponent
	{
		public final Object src;
		public final String formName;

		public FormAndComponent(Object src, String formName)
		{
			this.src = src;
			this.formName = formName;
		}
	}

	public void destroy()
	{
		this.application = null;
	}
}