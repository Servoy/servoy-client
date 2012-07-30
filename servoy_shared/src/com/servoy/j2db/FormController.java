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
package com.servoy.j2db;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.print.PageFormat;
import java.awt.print.PrinterJob;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.print.attribute.Size2DSyntax;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.JavaMembers;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Wrapper;

import com.servoy.j2db.cmd.ICmdManagerInternal;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.dataprocessing.BufferedDataSet;
import com.servoy.j2db.dataprocessing.DataAdapterList;
import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.dataprocessing.FoundSetEvent;
import com.servoy.j2db.dataprocessing.FoundSetManager;
import com.servoy.j2db.dataprocessing.IDataSet;
import com.servoy.j2db.dataprocessing.IDisplay;
import com.servoy.j2db.dataprocessing.IFoundSet;
import com.servoy.j2db.dataprocessing.IFoundSetEventListener;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.IFoundSetListener;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.ISaveConstants;
import com.servoy.j2db.dataprocessing.ISwingFoundSet;
import com.servoy.j2db.dataprocessing.JSDataSet;
import com.servoy.j2db.dataprocessing.PrototypeState;
import com.servoy.j2db.dataprocessing.RelatedFoundSet;
import com.servoy.j2db.dataprocessing.SortColumn;
import com.servoy.j2db.dataprocessing.TagResolver;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.ArgumentType;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportScriptProviders;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.MethodArgument;
import com.servoy.j2db.persistence.MethodTemplate;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.StaticContentSpecLoader.TypedProperty;
import com.servoy.j2db.querybuilder.IQueryBuilder;
import com.servoy.j2db.querybuilder.impl.QBSelect;
import com.servoy.j2db.scripting.CreationalPrototype;
import com.servoy.j2db.scripting.DefaultScope;
import com.servoy.j2db.scripting.ElementScope;
import com.servoy.j2db.scripting.FormScope;
import com.servoy.j2db.scripting.GlobalScope;
import com.servoy.j2db.scripting.IExecutingEnviroment;
import com.servoy.j2db.scripting.IScriptSupport;
import com.servoy.j2db.scripting.IScriptableProvider;
import com.servoy.j2db.scripting.ITwoNativeJavaObject;
import com.servoy.j2db.scripting.InstanceJavaMembers;
import com.servoy.j2db.scripting.JSApplication.FormAndComponent;
import com.servoy.j2db.scripting.JSEvent;
import com.servoy.j2db.scripting.JSWindow;
import com.servoy.j2db.scripting.RuntimeWindow;
import com.servoy.j2db.scripting.ScriptEngine;
import com.servoy.j2db.scripting.SelectedRecordScope;
import com.servoy.j2db.scripting.SolutionScope;
import com.servoy.j2db.ui.IComponent;
import com.servoy.j2db.ui.IDataRenderer;
import com.servoy.j2db.ui.ISupportRowStyling;
import com.servoy.j2db.ui.ISupportSecuritySettings;
import com.servoy.j2db.ui.runtime.IRuntimeComponent;
import com.servoy.j2db.util.ComponentFactoryHelper;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.IStyleRule;
import com.servoy.j2db.util.IStyleSheet;
import com.servoy.j2db.util.ITagResolver;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.ScopesUtils;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.TabSequenceHelper;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * Representation of a form
 * 
 * @author jblok, jcompagner
 */
public class FormController implements IForm, ListSelectionListener, TableModelListener, IFoundSetListener, IPrepareForSave, IFoundSetEventListener
{
	//Place holder class for the JavaScript FromController obj, all javascript calls must be delegated to the FormController
	//It's a pity that this class can't be a inner class, prohibit by JS calling structure(delegation would then not needed)
	@ServoyDocumented(category = ServoyDocumented.RUNTIME, publicName = "controller", scriptingName = "controller")
	public static class JSForm
	{
		private static final long serialVersionUID = 1L;

		private FormController formController;

		public JSForm()//required by JS lib, but never called by us.
		{
		}

		public JSForm(FormController formController)//required by JS lib
		{
			this.formController = formController;
			jsConstructor(formController);
		}

		public void jsConstructor(Object obj)
		{
			this.formController = (FormController)obj;
		}

		public void destroy()
		{
			formController = null;
		}

		public void checkDestroyed()
		{
			if (formController == null)
			{
				throw new RuntimeException("Accessing unloaded (destroyed) form"); //$NON-NLS-1$
			}
		}

		public FormController getFormPanel()
		{
			return formController;
		}

		/**
		 * Returns the JSWindow that the form is shown in, or null if the form is not currently showing in a window.
		 * 
		 * @sample
		 * var currentWindow = controller.getWindow();
		 * if (currentWindow != null) {
		 * 	currentWindow.title = 'We have a new title';
		 * } else {
		 * 	currentWindow = application.createWindow("Window Name", JSWindow.WINDOW, null);
		 * 	currentWindow(650, 700, 450, 350);
		 * 	currentWindow = "Window Title";
		 * 	controller.show(currentWindow);
		 * }
		 * 
		 * @return the JSWindow that the form is shown in, or null if the form is not currently showing in a window.
		 */
		public JSWindow js_getWindow()
		{
			checkDestroyed();
			// can't test if the form is visible, in onhide the form controller is already made not visible.
			// but it is still in a visible dialog.
			//if (formController.isFormVisible())
			{
				String name = formController.getContainerName();
				if (name != null)
				{
					if (IApplication.APP_WINDOW_NAME.equals(name)) name = null;
					RuntimeWindow w = formController.getApplication().getRuntimeWindowManager().getWindow(name);
					return (w != null && w.isVisible()) ? w.getJSWindow() : null;
				}
			}
			return null;
		}

		/**
		 * Shows the form (makes the form visible)
		 * This function does not affect the form foundset in any way.
		 * 
		 * @sample
		 * // show the form in the current window/dialog
		 * %%prefix%%controller.show();
		 * // show the form in newly created named modal dialog
		 * var w = application.createWindow("mydialog", JSWindow.MODAL_DIALOG);
		 * %%prefix%%controller.show(w);
		 * // show the form in an existing window/dialog
		 * var w = application.getWindow("mydialog"); // use null name for main app. window
		 * %%prefix%%controller.show(w);
		 * // or %%prefix%%controller.show("mydialog");
		 * //show the form in the main window
		 * //%%prefix%%controller.show(null);
		 * 
		 * @see com.servoy.j2db.scripting.JSApplication#js_createWindow(String, int)
		 * @see com.servoy.j2db.scripting.JSApplication#js_getWindow(String)
		 *
		 */
		// Deprecated implementation:
		// Shows the form (makes the form visible), optionally shown in the specified (modal or not) dialog.
		// @sample
		// //show the form in the current window/dialog
		// %%prefix%%controller.show();
		// //show the form in the named modal dialog 
		// //%%prefix%%controller.show('mydialog',true);
		//
		// @param dialogName optional the dialog/window name
		// @param modal optional boolean indicating modality for dialogs; default value is false
		public void js_show() throws ServoyException
		{
			js_show((JSWindow)null);
		}

		/**
		 * @clonedesc js_show()
		 * @sampleas js_show()
		 * 
		 * @param window the window in which this form should be shown, specified by the name of an existing window
		 * 
		 * @throws ServoyException
		 */
		public void js_show(String window) throws ServoyException
		{
			checkDestroyed();
			formController.showForm(window, null);
		}

		/**
		 * @clonedesc js_show()
		 * @sampleas js_show()
		 * 
		 * @param window the window in which this form should be shown, given as a window object
		 * 
		 * @throws ServoyException
		 */
		public void js_show(JSWindow window) throws ServoyException
		{
			checkDestroyed();
			formController.showForm(window, null);
		}

		/**
		 * @param window the window in which this form should be shown
		 * @param isModal true for a modal window, false otherwise
		 * 
		 * @throws ServoyException
		 * 
		 * @deprecated replaced by show(Object)
		 */
		@Deprecated
		public void js_show(Object window, Object isModal) throws ServoyException
		{
			checkDestroyed();
			formController.showForm(window, new Object[] { isModal });
		}

		/**
		 * @throws ServoyException
		 * 
		 * @deprecated Obsolete method, use foundset object instead.
		 */
		@Deprecated
		public void js_showRelatedRecords(Object[] args) throws ServoyException//obsolete,will not show in script editor
		{
			if (args != null && args.length > 1)
			{
				showRecords(args[0], args[1], args.length > 2 ? new Object[] { args[2] } : null);
			}
		}

		/**
		 * Load data into the form and shows the form, is a shortcut for the functions 'loadRecords' and 'show'.
		 *
		 * @sample
		 * %%prefix%%controller.showRecords(foundset);
		 * // load foundset & show the form in newly created named modal dialog
		 * var w = application.createWindow("mydialog", JSWindow.MODAL_DIALOG);
		 * %%prefix%%controller.showRecords(foundset, w);
		 * // load foundset & show the form in an existing window/dialog
		 * var w = application.getWindow("mydialog"); // use null name for main app. window
		 * %%prefix%%controller.showRecords(foundset, w);
		 * //%%prefix%%controller.showRecords(foundset, "mydialog");
		 * 
		 * @param foundset the foundset to load before showing the form.
		 */
		// Deprecated implementation:
		// //show the form in the named modal dialog 
		// //%%prefix%%controller.show(foundset, 'mydialog', true);
		// @param data the foundset/pkdataset/singleNumber_pk/UUIDpk to load before showing the form
		// @param dialogName optional the dialog name
		// @param modal optional boolean indicating modality for dialogs; default value is false
		//
		public void js_showRecords(FoundSet foundset) throws ServoyException
		{
			showRecords(foundset, null, null);
		}

		/**
		 * @clonedesc js_showRecords(FoundSet)
		 * @sampleas js_showRecords(FoundSet)
		 * 
		 * @see com.servoy.j2db.scripting.JSApplication#js_createWindow(String, int)
		 * @see com.servoy.j2db.scripting.JSApplication#js_getWindow(String)
		 * @see com.servoy.j2db.FormController$JSForm#js_loadRecords(FoundSet)
		 * @see com.servoy.j2db.FormController$JSForm#js_show(Object)
		 * 
		 * @param foundset the foundset to load before showing the form.
		 * @param window the window in which this form should be shown, specified by the name of an existing window.
		 */
		public void js_showRecords(FoundSet foundset, String window) throws ServoyException
		{
			showRecords(foundset, window, null);
		}

		/**
		 * @clonedesc js_showRecords(FoundSet)
		 * @sampleas js_showRecords(FoundSet)
		 * 
		 * @see com.servoy.j2db.scripting.JSApplication#js_createWindow(String, int)
		 * @see com.servoy.j2db.scripting.JSApplication#js_getWindow(String)
		 * @see com.servoy.j2db.FormController$JSForm#js_loadRecords(FoundSet)
		 * @see com.servoy.j2db.FormController$JSForm#js_show(Object)
		 * 
		 * @param foundset the foundset to load before showing the form.
		 * @param window the window in which this form should be shown, given as a window object.
		 */
		public void js_showRecords(FoundSet foundset, JSWindow window) throws ServoyException
		{
			showRecords(foundset, window, null);
		}

		/**
		 * @clonedesc js_showRecords(FoundSet)
		 * @sampleas js_showRecords(FoundSet)
		 * 
		 * @param pkdataset the pkdataset to load before showing the form.
		 */
		public void js_showRecords(JSDataSet pkdataset) throws ServoyException
		{
			showRecords(pkdataset, null, null);
		}

		/**
		 * @clonedesc js_showRecords(FoundSet)
		 * @sampleas js_showRecords(FoundSet)
		 * 
		 * @see com.servoy.j2db.scripting.JSApplication#js_createWindow(String, int)
		 * @see com.servoy.j2db.scripting.JSApplication#js_getWindow(String)
		 * @see com.servoy.j2db.FormController$JSForm#js_loadRecords(JSDataSet)
		 * @see com.servoy.j2db.FormController$JSForm#js_show(Object)
		 *  
		 * @param pkdataset the pkdataset to load before showing the form.
		 * @param window the window in which this form should be shown, specified by the name of an existing window.
		 */
		public void js_showRecords(JSDataSet pkdataset, String window) throws ServoyException
		{
			showRecords(pkdataset, window, null);
		}

		/**
		 * @clonedesc js_showRecords(FoundSet)
		 * @sampleas js_showRecords(FoundSet)
		 * 
		 * @see com.servoy.j2db.scripting.JSApplication#js_createWindow(String, int)
		 * @see com.servoy.j2db.scripting.JSApplication#js_getWindow(String)
		 * @see com.servoy.j2db.FormController$JSForm#js_loadRecords(JSDataSet)
		 * @see com.servoy.j2db.FormController$JSForm#js_show(Object)
		 *  
		 * @param pkdataset the pkdataset to load before showing the form.
		 * @param window the window in which this form should be shown, given as a window object.
		 */
		public void js_showRecords(JSDataSet pkdataset, JSWindow window) throws ServoyException
		{
			showRecords(pkdataset, window, null);
		}

		/**
		 * @clonedesc js_showRecords(FoundSet)
		 * @sampleas js_showRecords(FoundSet)
		 * 
		 * @see com.servoy.j2db.scripting.JSApplication#js_createWindow(String, int)
		 * @see com.servoy.j2db.scripting.JSApplication#js_getWindow(String)
		 * @see com.servoy.j2db.FormController$JSForm#js_loadRecords(String)
		 * @see com.servoy.j2db.FormController$JSForm#js_show(Object)
		 *  
		 * @param query the query to load before showing the form.
		 */
		public void js_showRecords(QBSelect query) throws ServoyException
		{
			showRecords(query, null, null);
		}

		/**
		 * @clonedesc js_showRecords(FoundSet)
		 * @sampleas js_showRecords(FoundSet)
		 * 
		 * @see com.servoy.j2db.scripting.JSApplication#js_createWindow(String, int)
		 * @see com.servoy.j2db.scripting.JSApplication#js_getWindow(String)
		 * @see com.servoy.j2db.FormController$JSForm#js_loadRecords(String)
		 * @see com.servoy.j2db.FormController$JSForm#js_show(Object)
		 *  
		 * @param query the query to load before showing the form.
		 * @param window the window in which this form should be shown, specified by the name of an existing window.
		 */
		public void js_showRecords(QBSelect query, String window) throws ServoyException
		{
			showRecords(query, window, null);
		}

		/**
		 * @clonedesc js_showRecords(FoundSet)
		 * @sampleas js_showRecords(FoundSet)
		 * 
		 * @see com.servoy.j2db.scripting.JSApplication#js_createWindow(String, int)
		 * @see com.servoy.j2db.scripting.JSApplication#js_getWindow(String)
		 * @see com.servoy.j2db.FormController$JSForm#js_loadRecords(String)
		 * @see com.servoy.j2db.FormController$JSForm#js_show(Object)
		 *  
		 * @param query the query to load before showing the form.
		 * @param window the window in which this form should be shown, given as a window object.
		 */
		public void js_showRecords(QBSelect query, JSWindow window) throws ServoyException
		{
			showRecords(query, window, null);
		}

		/**
		 * @clonedesc js_showRecords(FoundSet)
		 * @sampleas js_showRecords(FoundSet)
		 * 
		 * @param singleNumber_pk the singleNumber_pk to load before showing the form.
		 */
		public void js_showRecords(Number singleNumber_pk) throws ServoyException
		{
			showRecords(singleNumber_pk, null, null);
		}

		/**
		 * @clonedesc js_showRecords(FoundSet)
		 * @sampleas js_showRecords(FoundSet)
		 * 
		 * @see com.servoy.j2db.scripting.JSApplication#js_createWindow(String, int)
		 * @see com.servoy.j2db.scripting.JSApplication#js_getWindow(String)
		 * @see com.servoy.j2db.FormController$JSForm#js_loadRecords(Number)
		 * @see com.servoy.j2db.FormController$JSForm#js_show(Object)
		 *  
		 * @param singleNumber_pk the singleNumber_pk to load before showing the form.
		 * @param window the window in which this form should be shown, specified by the name of an existing window.
		 */
		public void js_showRecords(Number singleNumber_pk, String window) throws ServoyException
		{
			showRecords(singleNumber_pk, window, null);
		}

		/**
		 * @clonedesc js_showRecords(FoundSet)
		 * @sampleas js_showRecords(FoundSet)
		 * 
		 * @see com.servoy.j2db.scripting.JSApplication#js_createWindow(String, int)
		 * @see com.servoy.j2db.scripting.JSApplication#js_getWindow(String)
		 * @see com.servoy.j2db.FormController$JSForm#js_loadRecords(Number)
		 * @see com.servoy.j2db.FormController$JSForm#js_show(Object)
		 *  
		 * @param singleNumber_pk the singleNumber_pk to load before showing the form.
		 * @param window the window in which this form should be shown, given as a window object
		 */
		public void js_showRecords(Number singleNumber_pk, JSWindow window) throws ServoyException
		{
			showRecords(singleNumber_pk, window, null);
		}

		/**
		 * @clonedesc js_showRecords(FoundSet)
		 * @sampleas js_showRecords(FoundSet)
		 * 
		 * @param query the query to load before showing the form.
		 */
		public void js_showRecords(String query) throws ServoyException
		{
			showRecords(query, null, null);
		}

		/**
		 * @clonedesc js_showRecords(FoundSet)
		 * @sampleas js_showRecords(FoundSet)
		 * 
		 * @param query the query to load before showing the form.
		 * @param argumentsArray the array of arguments for the query
		 */
		public void js_showRecords(String query, Object[] argumentsArray) throws ServoyException
		{
			showRecords(query, argumentsArray, null, null);
		}

		/**
		 * @clonedesc js_showRecords(FoundSet)
		 * @sampleas js_showRecords(FoundSet)
		 * 
		 * @see com.servoy.j2db.scripting.JSApplication#js_createWindow(String, int)
		 * @see com.servoy.j2db.scripting.JSApplication#js_getWindow(String)
		 * @see com.servoy.j2db.FormController$JSForm#js_loadRecords(String)
		 * @see com.servoy.j2db.FormController$JSForm#js_show(Object)
		 *  
		 * @param query the query to load before showing the form.
		 * @param window the window in which this form should be shown, specified by the name of an existing window.
		 */
		public void js_showRecords(String query, String window) throws ServoyException
		{
			showRecords(query, window, null);
		}

		/**
		 * @clonedesc js_showRecords(FoundSet)
		 * @sampleas js_showRecords(FoundSet)
		 * 
		 * @see com.servoy.j2db.scripting.JSApplication#js_createWindow(String, int)
		 * @see com.servoy.j2db.scripting.JSApplication#js_getWindow(String)
		 * @see com.servoy.j2db.FormController$JSForm#js_loadRecords(String)
		 * @see com.servoy.j2db.FormController$JSForm#js_show(Object)
		 *  
		 * @param query the query to load before showing the form.
		 * @param argumentsArray the array of arguments for the query
		 * @param window the window in which this form should be shown, specified by the name of an existing window.
		 */
		public void js_showRecords(String query, Object[] argumentsArray, String window) throws ServoyException
		{
			showRecords(query, argumentsArray, window, null);
		}

		/**
		 * @clonedesc js_showRecords(FoundSet)
		 * @sampleas js_showRecords(FoundSet)
		 * 
		 * @see com.servoy.j2db.scripting.JSApplication#js_createWindow(String, int)
		 * @see com.servoy.j2db.scripting.JSApplication#js_getWindow(String)
		 * @see com.servoy.j2db.FormController$JSForm#js_loadRecords(String)
		 * @see com.servoy.j2db.FormController$JSForm#js_show(Object)
		 *  
		 * @param query the query to load before showing the form.
		 * @param window the window in which this form should be shown, given as a window object
		 */
		public void js_showRecords(String query, JSWindow window) throws ServoyException
		{
			showRecords(query, window, null);
		}

		/**
		 * @clonedesc js_showRecords(FoundSet)
		 * @sampleas js_showRecords(FoundSet)
		 * 
		 * @see com.servoy.j2db.scripting.JSApplication#js_createWindow(String, int)
		 * @see com.servoy.j2db.scripting.JSApplication#js_getWindow(String)
		 * @see com.servoy.j2db.FormController$JSForm#js_loadRecords(String)
		 * @see com.servoy.j2db.FormController$JSForm#js_show(Object)
		 *  
		 * @param query the query to load before showing the form.
		 * @param argumentsArray the array of arguments for the query
		 * @param window the window in which this form should be shown, given as a window object
		 */
		public void js_showRecords(String query, Object[] argumentsArray, JSWindow window) throws ServoyException
		{
			showRecords(query, argumentsArray, window, null);
		}

		/**
		 * @clonedesc js_showRecords(FoundSet)
		 * @sampleas js_showRecords(FoundSet)
		 * 
		 * @param UUIDpk the UUIDpk to load before showing the form.
		 */
		public void js_showRecords(UUID UUIDpk) throws ServoyException
		{
			showRecords(UUIDpk, null, null);
		}

		/**
		 * @clonedesc js_showRecords(FoundSet)
		 * @sampleas js_showRecords(FoundSet)
		 * 
		 * @see com.servoy.j2db.scripting.JSApplication#js_createWindow(String, int)
		 * @see com.servoy.j2db.scripting.JSApplication#js_getWindow(String)
		 * @see com.servoy.j2db.FormController$JSForm#js_loadRecords(UUID)
		 * @see com.servoy.j2db.FormController$JSForm#js_show(Object) 
		 * 
		 * @param UUIDpk the UUIDpk to load before showing the form.
		 * @param window the window in which this form should be shown, specified by the name of an existing window.
		 */
		public void js_showRecords(UUID UUIDpk, String window) throws ServoyException
		{
			showRecords(UUIDpk, window, null);
		}

		/**
		 * @clonedesc js_showRecords(FoundSet)
		 * @sampleas js_showRecords(FoundSet)
		 * 
		 * @see com.servoy.j2db.scripting.JSApplication#js_createWindow(String, int)
		 * @see com.servoy.j2db.scripting.JSApplication#js_getWindow(String)
		 * @see com.servoy.j2db.FormController$JSForm#js_loadRecords(UUID)
		 * @see com.servoy.j2db.FormController$JSForm#js_show(Object) 
		 * 
		 * @param UUIDpk the UUIDpk to load before showing the form.
		 * @param window the window in which this form should be shown, given as a window object.
		 */
		public void js_showRecords(UUID UUIDpk, JSWindow window) throws ServoyException
		{
			showRecords(UUIDpk, window, null);
		}

		/**
		 * @clonedesc js_showRecords(FoundSet)
		 * @sampleas js_showRecords(FoundSet)
		 * 
		 * @param foundset the foundset to load before showing the form
		 * @param dialogName the name of the dialog in which to show the form
		 * @param modal true for modal dialog, false for non-modal
		 * 
		 * @throws ServoyException
		 * 
		 * @deprecated see showRecords(FoundSet, String) or showRecords(FoundSet, JSWindow)  
		 */
		@Deprecated
		public void js_showRecords(Object foundset, Object dialogName, Object modal) throws ServoyException
		{
			showRecords(foundset, dialogName, new Object[] { modal });
		}

		private void showRecords(Object data, Object window, Object[] windowArgs) throws ServoyException
		{
			showRecords(data, null, window, windowArgs);
		}

		private void showRecords(Object data, Object[] dataArgs, Object window, Object[] windowArgs) throws ServoyException
		{
			checkDestroyed();
			formController.show(data, dataArgs, window, windowArgs);
		}

		/**
		 * Show all records in database.
		 *
		 * @deprecated Obsolete method, use foundset object instead.
		 */
		@Deprecated
		public void js_showAllRecords() throws ServoyException//obsolete,will not show in script editor
		{
			checkDestroyed();
			formController.loadAllRecordsImpl(true);
		}

		/**
		 * Loads all accessible records from the datasource into the form foundset.
		 * When the form contains a related foundset it will be replaced by a default foundset on same datasource.
		 * 
		 * Notes: 
		 * -the default foundset is always limited by filters, if databaseManager.addFoundSetFilterParam function is used.
		 * -typical use is loading the normal foundset again after form usage in a related tabpanel
		 *
		 * @sample %%prefix%%controller.loadAllRecords();
		 * 
		 * @see com.servoy.j2db.dataprocessing.JSDatabaseManager#js_addTableFilterParam(String, String, String, String, Object, String)
		 * @return true if successful
		 */
		public boolean js_loadAllRecords() throws ServoyException
		{
			checkDestroyed();
			return formController.loadAllRecordsImpl(true);
		}

		/**
		 * Load records via a (related) foundset, primary key (dataset/number/uuid) or query into the form.
		 * 
		 * Load records can be used in 5 different ways
		 * 1) to load a (related)foundset into the form.
		 * the form will no longer share the default foundset with forms of the same datasource, use loadAllRecords to restore the default foundset 
		 * controller.loadRecords(order_to_orderdetails);
		 * 
		 * 2) to load a primary key dataset, will remove related sort!
		 * var dataset = databaseManager.getDataSetByQuery(...);
		 * controller.loadRecords(dataset);
		 * 
		 * 3) to load a single record by primary key, will remove related sort! (pk should be a number or UUID)
		 * controller.loadRecords(123);
		 * or
		 * controller.loadRecords(application.getUUID('6b5e2f5d-047e-45b3-80ee-3a32267b1f20'));
		 * 
		 * 4) to reload all last related records again, if for example after a search in related tabpanel
		 * controller.loadRecords();
		 * 
		 * 5) to load records in to the form based on a query (also known as 'Form by query')
		 * controller.loadRecords(sqlstring,parameters);
		 * limitations/requirements for sqlstring are:
		 * -must start with 'select'
		 * -the selected columns must be the (Servoy Form) table primary key columns (alphabetically ordered like 'select a_id, b_id,c_id ...')
		 * -can contain '?' which are replaced with values from the array supplied to parameters argument
		 *  if the sqlstring contains an 'order by' clause, the records will be sorted accordingly and additional constraints apply:
		 * -must contain 'from' keyword
		 * -the 'from' must be a comma separated list of table names
		 * -must at least select from the table used in Servoy Form
		 * -cannot contain 'group by', 'having' or 'union'
		 * -all columns must be fully qualified like 'orders.order_id'
		 * 
		 * @sample
		 * //Load records can be used in 5 different ways
		 * //1) to load a (related)foundset into the form.
		 * //the form will no longer share the default foundset with forms of the same datasource, use loadAllRecords to restore the default foundset 
		 * //%%prefix%%controller.loadRecords(order_to_orderdetails);
		 * 
		 * //2) to load a primary key dataset, will remove related sort!
		 * //var dataset = databaseManager.getDataSetByQuery(...);
		 * // dataset must match the table primary key columns (alphabetically ordered)
		 * //%%prefix%%controller.loadRecords(dataset);
		 * 
		 * //3) to load a single record by primary key, will remove related sort! (pk should be a number or UUID)
		 * //%%prefix%%controller.loadRecords(123);
		 * //%%prefix%%controller.loadRecords(application.getUUID('6b5e2f5d-047e-45b3-80ee-3a32267b1f20'));
		 * 
		 * //4) to reload all last related records again, if for example after a search in related tabpanel
		 * //%%prefix%%controller.loadRecords();
		 * 
		 * //5) to load records in to the form based on a query (also known as 'Form by query')
		 * //%%prefix%%controller.loadRecords(sqlstring,parameters);
		 * //limitations/requirements for sqlstring are:
		 * //-must start with 'select'
		 * //-the selected columns must be the (Servoy Form) table primary key columns (alphabetically ordered like 'select a_id, b_id,c_id ...')
		 * //-can contain '?' which are replaced with values from the array supplied to parameters argument
		 * // if the sqlstring contains an 'order by' clause, the records will be sorted accordingly and additional constraints apply:
		 * //-must contain 'from' keyword
		 * //-the 'from' must be a comma separated list of table names
		 * //-must at least select from the table used in Servoy Form
		 * //-cannot contain 'group by', 'having' or 'union'
		 * //-all columns must be fully qualified like 'orders.order_id'
		 * 
		 * @return true if successful
		 * @see com.servoy.j2db.dataprocessing.FoundSet#js_loadRecords()
		 */
		public boolean js_loadRecords() throws ServoyException
		{
			checkDestroyed();
			return formController.loadAllRecordsImpl(false);
		}

		/**
		 * @clonedesc js_loadRecords()
		 * @sampleas js_loadRecords()
		 * @param foundset to load
		 * @return true if successful
		 */
		public boolean js_loadRecords(FoundSet foundset)
		{
			checkDestroyed();
			return formController.loadData(foundset, null);
		}

		/** 
		 * Method to handle old loadRecords calls with ignored argumentsaray.
		 * @param foundset 
		 * @param ignored 
		 * 
		 * @deprecated use loadRecords(FoundSet)
		 */
		@Deprecated
		public boolean js_loadRecords(FoundSet foundset, Object ignored)
		{
			return js_loadRecords(foundset);
		}

		/**
		 * @clonedesc js_loadRecords()
		 * @sampleas js_loadRecords()
		 * @param pkdataset to load
		 * @return true if successful
		 */
		public boolean js_loadRecords(IDataSet pkdataset)
		{
			checkDestroyed();
			return formController.loadData(pkdataset, null);
		}

		/** 
		 * Method to handle old loadRecords calls with ignored argumentsaray.
		 * @param pkdataset 
		 * @param ignored 
		 * 
		 * @deprecated use loadRecords(JSDataSet)
		 */
		@Deprecated
		public boolean js_loadRecords(IDataSet pkdataset, Object ignored)
		{
			return js_loadRecords(pkdataset);
		}

		/**
		 * @clonedesc js_loadRecords()
		 * @sampleas js_loadRecords()
		 * @param singlenNmber_pk to load
		 * @return true if successful
		 */
		public boolean js_loadRecords(Number singlenNmber_pk)
		{
			checkDestroyed();
			return formController.loadData(singlenNmber_pk, null);
		}

		/** 
		 * Method to handle old loadRecords calls with ignored argumentsaray.
		 * @param singlenNmber_pk 
		 * @param ignored 
		 * 
		 * @deprecated use loadRecords(Number)
		 */
		@Deprecated
		public boolean js_loadRecords(Number singlenNmber_pk, Object ignored)
		{
			return js_loadRecords(singlenNmber_pk);
		}

		/**
		 * @clonedesc js_loadRecords()
		 * @sampleas js_loadRecords()
		 * @param UUIDpk to load
		 * @return true if successful
		 */
		public boolean js_loadRecords(UUID UUIDpk)
		{
			checkDestroyed();
			return formController.loadData(UUIDpk, null);
		}

		/** 
		 * Method to handle old loadRecords calls with ignored argumentsaray.
		 * @param UUIDpk 
		 * @param ignored 
		 * 
		 * @deprecated use loadRecords(UUID)
		 */
		@Deprecated
		public boolean js_loadRecords(UUID UUIDpk, Object ignored)
		{
			return js_loadRecords(UUIDpk);
		}

		/**
		 * @clonedesc js_loadRecords()
		 * @sampleas js_loadRecords()
		 * @param queryString to load
		 * @return true if successful
		 */
		public boolean js_loadRecords(String queryString)
		{
			checkDestroyed();
			return formController.loadData(queryString, null);
		}

		/**
		 * @clonedesc js_loadRecords()
		 * @sampleas js_loadRecords()
		 * @param queryString to load
		 * @param queryArgumentsArray the arguments to replace the questions marks in the queryString
		 * @return true if successful
		 */
		public boolean js_loadRecords(String queryString, Object[] queryArgumentsArray)
		{
			checkDestroyed();
			return formController.loadData(queryString, queryArgumentsArray);
		}


		/** 
		 * Method to handle old loadRecords calls.
		 * Deprecated method to handle pre-6.1 calls to varargs function controller.loadRecords([1]), this was called with vargs=[1] in stead of vargs=[[1]].
		 * 
		 * @param vargs the arguments
		 * 
		 * @deprecated use loadRecords with single typed argument
		 */
		@Deprecated
		public boolean js_loadRecords(Object[] vargs)
		{
			if (vargs == null || vargs.length != 1)
			{
				throw new IllegalArgumentException("Cannot find function loadRecords for " + (vargs == null ? "no" : String.valueOf(vargs.length)) + " args");
			}

			Object data = vargs[0];

			if (data instanceof Wrapper)
			{
				data = ((Wrapper)data).unwrap();
			}

			if (data instanceof FoundSet || data == null) // loadRecords(fs) when foundset does not exist
			{
				return js_loadRecords((FoundSet)data);
			}
			if (data instanceof IDataSet)
			{
				return js_loadRecords((IDataSet)data);
			}
			if (data instanceof String)
			{
				return js_loadRecords((String)data);
			}
			if (data instanceof Number)
			{
				return js_loadRecords((Number)data);
			}
			if (data instanceof UUID)
			{
				return js_loadRecords((UUID)data);
			}

			throw new IllegalArgumentException("Cannot find function loadRecords for argument " + data.getClass().getName());
		}


		/**
		 * Returns the current cached record count of the current foundset. 
		 * To return the full foundset count, use: databaseManager.getFoundSetCount(...) 
		 * Tip: get the the table count of all rows in a table, use: databaseManager.getTableCount(...) 
		 *
		 * @sample
		 * for ( var i = 1 ; i <= %%prefix%%controller.getMaxRecordIndex() ; i++ )
		 * {
		 * 	%%prefix%%controller.setSelectedIndex(i);
		 * 	//do some action per record
		 * }
		 * @see com.servoy.j2db.dataprocessing.JSDatabaseManager#js_getFoundSetCount(Object)
		 * @return the max record index
		 */
		public int js_getMaxRecordIndex()
		{
			checkDestroyed();
			return formController.getFormModel().getSize();
		}

		/**
		 * Get the name of this form.
		 *
		 * @sample var formName = %%prefix%%controller.getName();
		 * @return the name
		 */
		public String js_getName()
		{
			checkDestroyed();
			return formController.getName();
		}

		/**
		 * Get the name of the window/dialog this form is displayed in.
		 * 
		 * @deprecated As of release 6.0, dialogs/windows API has been rewritten (based in JSWindow objects). Use {@link #getWindow()} instead.
		 * 
		 * @sample
		 * var dialogOrWindowName = %%prefix%%controller.getContainerName();
		 * if (dialogOrWindowName != null) {
		 * 	application.closeForm(dialogOrWindowName);
		 * }
		 * 
		 * @return the name of the window/dialog this form is displayed in. If the form is not showing in a window or dialog (other then main application frame), it returns null.
		 */
		@Deprecated
		public String js_getContainerName()
		{
			JSWindow w = js_getWindow();
			return w != null ? w.js_getName() : null;
		}

		/**
		 * Get the name of the table used.
		 * 
		 * @deprecated  As of release 5.0, replaced by {@link #getDataSource()}
		 * 
		 * @see com.servoy.j2db.dataprocessing.FoundSet#js_getDataSource()
		 */
		@Deprecated
		public String js_getTableName()
		{
			checkDestroyed();
			return formController.getForm().getTableName();
		}

		/**
		 * Get the used datasource.
		 *
		 * @sample var dataSource = %%prefix%%controller.getDataSource();
		 * @return the datasource
		 */
		public String js_getDataSource()
		{
			checkDestroyed();
			return formController.getForm().getDataSource();
		}

		/**
		 * Get the name of the server used.
		 *
		 * @deprecated  As of release 5.0, replaced by {@link #getDataSource()}
		 * 
		 */
		@Deprecated
		public String js_getServerName()
		{
			checkDestroyed();
			return formController.getForm().getServerName();
		}

		/**
		 * Returns the maximum length allowed in the specified dataprovider. 
		 *
		 * @sample %%prefix%%controller.getDataProviderMaxLength('name');
		 * @param name the dataprovider name
		 * @return the length
		 */
		public int js_getDataProviderMaxLength(String name) throws ServoyException
		{
			checkDestroyed();
			if (name != null)
			{
				IDataProvider dp = formController.getApplication().getFlattenedSolution().getDataproviderLookup(
					formController.application.getFoundSetManager(), formController.getForm()).getDataProvider(name);
				if (dp != null)
				{
					return dp.getLength();
				}
			}
			return 0;
		}

		/**
		 * @deprecated  As of release 3.0, replaced by {@link foundset#getSelectedIndex()}
		 */
		@Deprecated
		public int js_getRecordIndex()
		{
			checkDestroyed();
			return formController.getRecordIndex() + 1;
		}

		/**
		 * Sets focus to the first field of the form; based on tab order sequence.
		 *
		 * @sample %%prefix%%controller.focusFirstField();
		 * 
		 * @see focusField
		 */
		public void js_focusFirstField()
		{
			js_focusField(null, false);
		}

		/**
		 * Sets focus to a field specified by its name. 
		 * If the second parameter is set to true, then readonly fields will be skipped 
		 * (the focus will be set to the first non-readonly field located after the field with the specified name; the tab sequence is respected when searching for the non-readonly field).
		 *
		 * @sample
		 * var tabseq = %%prefix%%controller.getTabSequence();
		 * if (tabseq.length > 1) {
		 * 	// If there is more than one field in the tab sequence, 
		 * 	// focus the second one and skip over readonly fields.
		 * 	%%prefix%%controller.focusField(tabseq[1], true);
		 * }
		 * else {
		 * 	// If there is at most one field in the tab sequence, then focus
		 * 	// whatever field is first, and don't bother to skip over readonly fields.
		 * 	%%prefix%%controller.focusField(null, false);
		 * }
		 * @param fieldName the name of the field to be focussed
		 * @param skipReadonly boolean indication to skip read only fields, if the named field happens to be read only
		 */
		public void js_focusField(String fieldName, boolean skipReadonly)
		{
			checkDestroyed();
			formController.focusField(fieldName, skipReadonly);
		}

		/**
		 * Recreates the forms UI components, to reflect the latest solution model.
		 * Use this after altering the elements via solutionModel at the JSForm of this form.
		 * 
		 * @sample
		 * // get the solution model JSForm 
		 * var form = solutionModel.getForm("myForm");
		 * // get the JSField of the form 
		 * var field = form.getField("myField");
		 * // alter the field
		 * field.x = field.x + 10;
		 * // recreate the runtime forms ui to reflect the changes.
		 * %%prefix%%controller.recreateUI();
		 * 
		 * @see com.servoy.j2db.scripting.solutionmodel.JSForm
		 * 
		 * @return true if successful
		 */
		public boolean js_recreateUI()
		{
			checkDestroyed();
			return formController.recreateUI();
		}

		/**
		 * Set the tab order sequence programatically, by passing the elements references in a javascript array.
		 *
		 * @sample %%prefix%%controller.setTabSequence([%%prefix%%elements.fld_order_id, %%prefix%%elements.fld_order_amount]);
		 * @param arrayOfElements array containing the element references
		 */
		public void js_setTabSequence(Object[] arrayOfElements)
		{
			checkDestroyed();
			formController.setTabSequence(arrayOfElements);
		}

		/**
		 * Get an array with the names of the components that are part of the tab sequence. 
		 * The order of the names respects the order of the tab sequence. 
		 * Components that are not named will not appear in the returned array, although they may be in the tab sequence.
		 *
		 * @sample
		 * var tabseq = %%prefix%%controller.getTabSequence();
		 * if (tabseq.length > 1) {
		 * 	// If there is more than one field in the tab sequence, 
		 * 	// focus the second one and skip over readonly fields.
		 * 	%%prefix%%controller.focusField(tabseq[1], true);
		 * }
		 * else {
		 * 	// If there is at most one field in the tab sequence, then focus
		 * 	// whatever field is first, and don't bother to skip over readonly fields.
		 * 	%%prefix%%controller.focusField(null, false);
		 * }
		 * @return array of names
		 */
		public String[] jsFunction_getTabSequence()
		{
			checkDestroyed();
			return formController.getTabSequence();
		}

		/**
		 * @deprecated
		 */
		@Deprecated
		public void js_setRecordIndex(int i)
		{
			checkDestroyed();
			// Johan: This can't be disabled.. selected row can never be smaller or bigger then max index
			// that while loop is just illegal maybe we have to rename recordIndex to set and getSelectedIndex
			// JB: this is disabled because while (controller.recordIndex <= controller.getMaxREcordIndex()) never can get bigger than
			if (i >= 1 && i <= js_getMaxRecordIndex())
			{
				formController.setRecordIndex(i - 1);
			}
		}

		/**
		 * Gets the current record index of the current foundset.
		 *
		 * @sample
		 * //gets the current record index in the current foundset
		 * var current = %%prefix%%controller.getSelectedIndex();
		 * //sets the next record in the foundset, will be reflected in UI
		 * %%prefix%%controller.setSelectedIndex(current+1);
		 * @return the index
		 */
		public int jsFunction_getSelectedIndex()
		{
			checkDestroyed();
			return formController.getRecordIndex() + 1;
		}

		/**
		 * Sets the current record index of the current foundset.
		 *
		 * @sample
		 * //gets the current record index in the current foundset
		 * var current = %%prefix%%controller.getSelectedIndex();
		 * //sets the next record in the foundset, will be reflected in UI
		 * %%prefix%%controller.setSelectedIndex(current+1);
		 * 
		 * @param index the index to select 
		 */
		public void jsFunction_setSelectedIndex(int index) //Object[] args)
		{
			checkDestroyed();
			//int index = Utils.getAsInteger(args[0]);
			if (index >= 1 && index <= js_getMaxRecordIndex())
			{
				formController.setRecordIndex(index - 1);
			}
		}

		/**
		 * Gets or sets the enabled state of a form; also known as "grayed-out".
		 * 
		 * Notes:
		 * -A disabled element(s) cannot be selected by clicking the form.
		 * -The disabled "grayed" color is dependent on the LAF set in the Servoy Smart Client Application Preferences.
		 *
		 * @sample
		 * //gets the enabled state of the form
		 * var state = %%prefix%%controller.enabled;
		 * //enables the form for input
		 * %%prefix%%controller.enabled = true;
		 */
		public boolean js_getEnabled()
		{
			checkDestroyed();
			return formController.isEnabled();
		}

		public void js_setEnabled(boolean b)
		{
			checkDestroyed();
			formController.setComponentEnabled(b);
		}

		/**
		 * Gets or sets the read-only state of a form; also known as "editable"
		 * 
		 * Note: The field(s) in a form set as read-only can be selected and the field data can be copied to clipboard. 
		 *
		 * @sample
		 * //gets the read-only state of the form
		 * var state = %%prefix%%controller.readOnly;
		 * //sets the read-only state of the form
		 * %%prefix%%controller.readOnly = true
		 */
		public boolean js_getReadOnly()
		{
			checkDestroyed();
			return formController.isReadOnly();
		}

		public void js_setReadOnly(boolean b)
		{
			checkDestroyed();
			formController.setReadOnly(b);
		}

		/**
		 * @sameas com.servoy.j2db.dataprocessing.FoundSet#js_find()
		 * @sample
		 * if (%%prefix%%controller.find()) //find will fail if autosave is disabled and there are unsaved records
		 * {
		 * 	columnTextDataProvider = 'a search value'
		 * 	columnNumberDataProvider = '>10'
		 * 	columnDateDataProvider = '31-12-2010|dd-MM-yyyy'
		 * 	%%prefix%%controller.search()
		 * }
		 */
		public boolean js_find() throws ServoyException
		{
			checkDestroyed();
			return formController.findImpl(true);
		}

		/**
		 * @sameas com.servoy.j2db.dataprocessing.FoundSet#js_search()
		 * @sample
		 * var recordCount = %%prefix%%controller.search();
		 * //var recordCount = %%prefix%%controller.search(false,false); //to extend foundset
		 */
		public int js_search() throws ServoyException
		{
			return js_search(true, true);
		}

		/**
		 * @sameas com.servoy.j2db.dataprocessing.FoundSet#js_search(boolean)
		 * 
		 * @param clearLastResults boolean, clear previous search, default true  
		 * 
		 * @return the recordCount
		 */
		public int js_search(boolean clearLastResults) throws ServoyException
		{
			return js_search(clearLastResults, true);
		}

		/**
		 * @sameas com.servoy.j2db.dataprocessing.FoundSet#js_search(boolean,boolean)
		 * 
		 * @param clearLastResults boolean, clear previous search, default true  
		 * @param reduceSearch boolean, reduce (true) or extend (false) previous search results, default true
		 * 
		 * @return the recordCount
		 */
		public int js_search(boolean clearLastResults, boolean reduceSearch) throws ServoyException
		{
			checkDestroyed();
			int nfound = formController.performFindImpl(clearLastResults, reduceSearch, false);
			return nfound < 0 ? 0/* blocked */: nfound;
		}

		/**
		 * @deprecated  As of release 5.0, replaced by {@link #showPrintPreview()}
		 */
		@Deprecated
		public void js_printPreview(Object[] vargs) //obsolete,will not show in script editor
		{
			boolean printCurrentRecordOnly = false;
			PrinterJob printerJob = null;
			int zoomFactor = 100;
			if (vargs != null)
			{
				if (vargs.length >= 1)
				{
					printCurrentRecordOnly = Utils.getAsBoolean(vargs[0]);
				}
				if (vargs.length >= 2 && vargs[1] instanceof PrinterJob)
				{
					//for real printing
					printerJob = (PrinterJob)vargs[1];
				}
				if (vargs.length >= 3)
				{
					zoomFactor = Utils.getAsInteger(vargs[2]);
					if (zoomFactor <= 0)
					{
						zoomFactor = 100;
					}
				}
			}

			js_showPrintPreview(printCurrentRecordOnly, printerJob, zoomFactor);
		}

		/**
		 * Show this form in print preview.
		 *
		 * @sample
		 * //shows this form (with foundset records) in print preview
		 * %%prefix%%controller.showPrintPreview();
		 * //to print preview current record only
		 * //%%prefix%%controller.showPrintPreview(true);
		 * //to print preview current record only with 125% zoom factor; 
		 * //%%prefix%%controller.showPrintPreview(true, null, 125);
		 *
		 */
		public void js_showPrintPreview()
		{
			js_showPrintPreview(false, null, 100);
		}

		/**
		 * @clonedesc js_showPrintPreview()
		 * @sampleas js_showPrintPreview()
		 * @param printCurrentRecordOnly to print the current record only
		 */
		public void js_showPrintPreview(boolean printCurrentRecordOnly)
		{
			js_showPrintPreview(printCurrentRecordOnly, null, 100);
		}

		/**
		 * @clonedesc js_showPrintPreview()
		 * @sampleas js_showPrintPreview()
		 *
		 * @param printCurrentRecordOnly to print the current record only
		 * @param printerJob print to plugin printer job, see pdf printer plugin for example (incase print is used from printpreview)
		 * 
		 */
		public void js_showPrintPreview(boolean printCurrentRecordOnly, PrinterJob printerJob)
		{
			js_showPrintPreview(printCurrentRecordOnly, printerJob, 100);
		}

		/**
		 * @clonedesc js_showPrintPreview()
		 * @sampleas js_showPrintPreview()
		 *
		 * @param printCurrentRecordOnly to print the current record only
		 * @param printerJob print to plugin printer job, see pdf printer plugin for example (incase print is used from printpreview)
		 * @param zoomFactor a specified number value from 10-400
		 */
		public void js_showPrintPreview(boolean printCurrentRecordOnly, PrinterJob printerJob, int zoomFactor)
		{
			checkDestroyed();
			formController.printPreview(false, printCurrentRecordOnly, zoomFactor <= 0 ? 100 : zoomFactor, printerJob);
		}

		/**
		 * Print this form with current foundset, without preview.
		 *
		 * @sample
		 * //print this form (with foundset records)
		 * %%prefix%%controller.print();
		 * //print only current record (no printerSelectDialog) to pdf plugin printer
		 * //%%prefix%%controller.print(true,false,plugins.pdf_output.getPDFPrinter('c:/temp/out.pdf'));
		 *
		 */
		public void js_print()
		{
			js_print(false, true, null);
		}

		/**
		 * @clonedesc js_print()
		 * @sampleas js_print()
		 * @param printCurrentRecordOnly to print the current record only
		 */
		public void js_print(boolean printCurrentRecordOnly)
		{
			js_print(printCurrentRecordOnly, true, null);
		}

		/**
		 * @clonedesc js_print()
		 * @sampleas js_print()
		 * @param printCurrentRecordOnly to print the current record only
		 * @param showPrinterSelectDialog boolean to show the printer select dialog (default printer is normally used)  
		 */
		public void js_print(boolean printCurrentRecordOnly, boolean showPrinterSelectDialog)
		{
			js_print(printCurrentRecordOnly, showPrinterSelectDialog, null);
		}

		/**
		 * @clonedesc js_print()
		 * @sampleas js_print()
		 * @param printCurrentRecordOnly to print the current record only
		 * @param showPrinterSelectDialog boolean to show the printer select dialog (default printer is normally used)  
		 * @param printerJob print to plugin printer job, see pdf printer plugin for example
		 */
		public void js_print(boolean printCurrentRecordOnly, boolean showPrinterSelectDialog, PrinterJob printerJob)
		{
			checkDestroyed();
			formController.print(false, printCurrentRecordOnly, showPrinterSelectDialog, printerJob);
		}

		/**
		 * Print this form with current foundset records to xml format.
		 *
		 * @sample
		 * //TIP: see also plugins.file.writeXMLFile(...)
		 * var xml = %%prefix%%controller.printXML();
		 * //print only current record 
		 * //var xml = %%prefix%%controller.printXML(true);
		 *
		 * @return the XML 
		 */
		public String js_printXML()
		{
			return js_printXML(false);
		}

		/**
		 * @clonedesc js_printXML()
		 * @sampleas js_printXML()
		 *
		 * @param printCurrentRecordOnly to print the current record only
		 * @return the XML 
		 */
		public String js_printXML(boolean printCurrentRecordOnly)
		{
			checkDestroyed();
			return formController.printXML(printCurrentRecordOnly);
		}

		/**
		 * Get duplicate of current foundset, can be used by loadRecords again
		 * 
		 * @deprecated  As of release 3.0, replaced by {@link foundset#duplicateFoundSet()}
		 * 
		 * @sample
		 * var dupFoundset = %%prefix%%controller.duplicateFoundSet();
		 * %%prefix%%controller.find();
		 * //search some fields
		 * var count = %%prefix%%controller.search();
		 * if (count == 0)
		 * {
		 * 	plugins.dialogs.showWarningDialog('Alert', 'No records found','OK');
		 * 	controller.loadRecords(dupFoundset);
		 * }
		 *
		 */
		@Deprecated
		public Object js_duplicateFoundSet() throws ServoyException//can be used by loadRecords Again
		{
			checkDestroyed();
			return formController.getFormModel().copy(false);
		}

		/**
		 * Delete current selected record, deletes mulitple selected records incase the foundset is using multiselect.
		 *
		 * @sample
		 * var success = %%prefix%%controller.deleteRecord();
		 * @return false incase of related foundset having records and orphans records are not allowed by the relation
		 */
		public boolean js_deleteRecord() throws ServoyException
		{
			checkDestroyed();
			return formController.deleteRecordImpl();
		}

		/**
		 * Deletes all records in foundset, resulting in empty foundset.
		 *
		 * @sample
		 * var success = %%prefix%%controller.deleteAllRecords();
		 * @return false incase of related foundset having records and orphans records are not allowed by the relation
		 */
		public boolean js_deleteAllRecords() throws ServoyException
		{
			checkDestroyed();
			return formController.deleteAllRecordsImpl();
		}

		/**
		 * Sets this form in designmode with one or more callback methods. 
		 *
		 * @sampleas jsFunction_setDesignMode(boolean)
		 *
		 * @param ondrag org.mozilla.javascript.Function onDrag method reference 
		 */
		public void jsFunction_setDesignMode(Function onDrag)
		{
			jsFunction_setDesignMode(onDrag, null, null, null);
		}

		/**
		 * Sets this form in designmode with one or more callback methods. 
		 *
		 * @sampleas jsFunction_setDesignMode(boolean)
		 *
		 * @param ondrag org.mozilla.javascript.Function onDrag method reference 
		 * @param ondrop org.mozilla.javascript.Function onDrop method reference 
		 */
		public void jsFunction_setDesignMode(Function onDrag, Function onDrop)
		{
			jsFunction_setDesignMode(onDrag, onDrop, null, null);
		}

		/**
		 * Sets this form in designmode with one or more callback methods. 
		 *
		 * @sampleas jsFunction_setDesignMode(boolean)
		 *
		 * @param ondrag org.mozilla.javascript.Function onDrag method reference 
		 * @param ondrop org.mozilla.javascript.Function onDrop method reference 
		 * @param onselect org.mozilla.javascript.Function onSelect method reference
		 */
		public void jsFunction_setDesignMode(Function onDrag, Function onDrop, Function onSelect)
		{
			jsFunction_setDesignMode(onDrag, onDrop, onSelect, null);
		}

		/**
		 * Sets this form in designmode with one or more callback methods. 
		 *
		 * @sampleas jsFunction_setDesignMode(boolean)
		 *
		 * @param ondrag org.mozilla.javascript.Function onDrag method reference 
		 * @param ondrop org.mozilla.javascript.Function onDrop method reference 
		 * @param onselect org.mozilla.javascript.Function onSelect method reference
		 * @param onresize org.mozilla.javascript.Function onResize method reference
		 */
		public void jsFunction_setDesignMode(Function onDrag, Function onDrop, Function onSelect, Function onResize)
		{
			checkDestroyed();
			formController.setDesignMode(new DesignModeCallbacks(new Function[] { onDrag, onDrop, onSelect, onResize }, formController.application));
		}

		/**
		 * Sets this form in designmode with param true, false will return to normal browse/edit mode.
		 *
		 * @sample
		 * var form = forms["selectedFormName"];
		 * if (!form.controller.getDesignMode())
		 * {
		 * 	// Set the current form in designmode with no callbacks
		 * 	form.controller.setDesignMode(true);
		 * 	// Set the current form in designmode with callbacks
		 * 	// where onDrag, onDrop, onSelect, onResize are names of form methods (not from "selectedFormName" form)
		 * 	// form.controller.setDesignMode(onDrag, onDrop, onSelect, onResize);
		 * }
		 * //Set the current form out of designmode (to normal browse)
		 * //form.controller.setDesignMode(false);
		 *
		 * @param designMode boolean sets form in design mode if true, false ends design mode.  
		 */
		public void jsFunction_setDesignMode(boolean designMode)
		{
			checkDestroyed();
			formController.setDesignMode(designMode ? new DesignModeCallbacks(new Object[0], formController.application) : null);
		}

		/**
		 * Returns the state of this form designmode.
		 *
		 * @sample
		 * var success = %%prefix%%controller.getDesignMode();
		 * 
		 * @return the design mode state (true/fase)
		 */
		public boolean jsFunction_getDesignMode()
		{
			checkDestroyed();
			return formController.getDesignMode();
		}

		/**
		 * Create a new record in the form foundset.
		 *
		 * @sample
		 * // foreign key data is only filled in for equals (=) relation items 
		 * %%prefix%%controller.newRecord();//default adds on top
		 * //%%prefix%%controller.newRecord(false); //adds at bottom
		 * //%%prefix%%controller.newRecord(2); //adds as second record
		 * 
		 * @return true if succesful
		 */
		public boolean js_newRecord() throws ServoyException
		{
			return js_newRecord(true);
		}

		/**
		 * @clonedesc js_newRecord()
		 * @sampleas js_newRecord()
		 * @param insertOnTop boolean true adds the new record as the topmost record
		 * @return true if successful
		 */
		public boolean js_newRecord(boolean insertOnTop) throws ServoyException
		{
			checkDestroyed();
			return formController.newRecordImpl(insertOnTop ? 0 : Integer.MAX_VALUE);
		}

		/**
		 * @clonedesc js_newRecord()
		 * @sampleas js_newRecord()
		 * @param location adds at specified index
		 * @return true if successful
		 */
		public boolean js_newRecord(int location) throws ServoyException
		{
			checkDestroyed();
			int indexToAdd = location - 1;
			if (indexToAdd >= 0)
			{
				return formController.newRecordImpl(indexToAdd);
			}

			return false;
		}

		/**
		 * @deprecated  As of release 3.0, replaced by {@link databaseManager#saveData()}
		 */
		@Deprecated
		public boolean js_saveData()
		{
			checkDestroyed();
			return formController.application.getFoundSetManager().getEditRecordList().stopEditing(true) == ISaveConstants.STOPPED;
		}

		/**
		 * Duplicate current record or record at index in the form foundset.
		 *
		 * @sample
		 * %%prefix%%controller.duplicateRecord(); //duplicate the current record, adds on top
		 * //%%prefix%%controller.duplicateRecord(false); //duplicate the current record, adds at bottom
		 * //%%prefix%%controller.duplicateRecord(1,2); //duplicate the first record as second record
		 * 
		 * @return true if succesful
		 */
		public boolean js_duplicateRecord() throws ServoyException
		{
			return js_duplicateRecord(true);
		}

		/**
		 * @clonedesc js_duplicateRecord()
		 * @sampleas js_duplicateRecord()
		 * @param location boolean true adds the new record as the topmost record
		 * @return true if successful
		 */
		public boolean js_duplicateRecord(boolean location) throws ServoyException
		{
			checkDestroyed();
			return formController.duplicateRecordImpl(location ? 0 : Integer.MAX_VALUE);
		}

		/**
		 * @clonedesc js_duplicateRecord()
		 * @sampleas js_duplicateRecord()
		 * @param location adds at specified index
		 * @return true if successful
		 */
		public boolean js_duplicateRecord(int location) throws ServoyException
		{
			checkDestroyed();
			int indexToAdd = location - 1;
			if (indexToAdd >= 0)
			{
				return formController.duplicateRecordImpl(indexToAdd);
			}
			return false;
		}

		public void js_setView(int i)
		{
			checkDestroyed();
			if (i >= 0 && i < 2)
			{
				formController.setView(i);
			}
		}

		/**
		 * Get/Set the current type of view of this form.
		 *
		 * @sample
		 * //gets the type of view for this form
		 * var view = %%prefix%%controller.view;
		 * //sets the form to Record view
		 * %%prefix%%controller.view = 0;//RECORD_VIEW
		 * //sets the form to List view
		 * %%prefix%%controller.view = 1;//LIST_VIEW
		 */
		public int js_getView()
		{
			checkDestroyed();
			return formController.getView();
		}

		/**
		 * Gets the forms context where it resides, returns a dataset of its structure to the main controller.
		 * Note: can't be called in onload, because no context is yet available at this time.
		 *
		 * @sample
		 * //dataset columns: [containername(1),formname(2),tabpanel or beanname(3),tabname(4),tabindex(5)]
		 * //dataset rows: mainform(1) -> parent(2)  -> current form(3) (when 3 forms deep)
		 * /** @type {JSDataSet} *&#47;
		 * var dataset = %%prefix%%controller.getFormContext();
		 * if (dataset.getMaxRowIndex() > 1) 
		 * {
		 * 	// form is in a tabpanel
		 * 	var parentFormName = dataset.getValue(1,2)
		 * }
		 * @return the dataset with form context
		 * @see com.servoy.j2db.dataprocessing.JSDataSet 
		 */
		public JSDataSet js_getFormContext()
		{
			checkDestroyed();
			return formController.getFormUI().getFormContext();
		}

		/**
		 * Omit current record in form foundset, to be shown with loadOmittedRecords.
		 * 
		 * Note: The omitted records are discarded when these functions are executed: loadAllRecords, loadRecords(dataset), loadRecords(sqlstring), invert 
		 *
		 * @sample var success = %%prefix%%controller.omitRecord();
		 * @return true if successful
		 * 
		 * @see com.servoy.j2db.FormController$JSForm#js_loadOmittedRecords()
		 */
		public boolean js_omitRecord() throws ServoyException
		{
			checkDestroyed();
			return formController.omitRecordImpl();
		}

		/**
		 * @deprecated  As of release 3.0, replaced by {@link foundset#loadOmittedRecords()}
		 */
		@Deprecated
		public boolean js_showOmittedRecords() throws ServoyException//obsolete,will not show in script editor
		{
			checkDestroyed();
			return formController.showOmittedRecordsImpl();
		}

		/**
		 * Loads the records that are currently omitted in the form foundset.
		 *
		 * @sample %%prefix%%controller.loadOmittedRecords();
		 * @return true if successful
		 */
		public boolean js_loadOmittedRecords() throws ServoyException
		{
			checkDestroyed();
			return formController.showOmittedRecordsImpl();
		}

		/**
		 * Inverts the current foundset against all rows of the current table; all records that are not in the foundset will become the current foundset.
		 *
		 * @sample %%prefix%%controller.invertRecords();
		 * @return true if successful
		 */
		public boolean js_invertRecords() throws ServoyException
		{
			checkDestroyed();
			return formController.invertRecordsImpl();
		}

		/**
		 * Copy current record to variable and clipboard.
		 * 
		 * @deprecated As of release 3.0, replaced by {@link application#setClipboardContent(Object)}.
		 * 
		 * @sample var recorddata = %%prefix%%controller.copyRecord();
		 */
		@Deprecated
		public String js_copyRecord()
		{
			checkDestroyed();
			return formController.copyRecord();
		}

		/**
		 * Copy all records from foundset to variable and clipboard
		 * 
		 * @deprecated As of release 3.0, replaced by {@link application#setClipboardContent(Object)}.
		 * 
		 * @sample var recorddata = %%prefix%%controller.copyAllRecords();
		 */
		@Deprecated
		public String js_copyAllRecords()
		{
			checkDestroyed();
			return formController.copyAllRecords();
		}

		/**
		 * Show the sort dialog to the user a preselection sortString can be passed, to sort the form foundset.
		 * TIP: You can use the Copy button in the developer Select Sorting Fields dialog to get the needed syntax string for the desired sort fields/order. 
		 * 
		 * @sample %%prefix%%controller.sortDialog('columnA desc,columnB asc'); 
		 */
		public void js_sortDialog()
		{
			checkDestroyed();
			formController.showSortDialog(null);
		}

		/**
		 * @clonedes js_sortDialog()
		 * @sampleas js_sortDialog()
		 *
		 * @param sortString the specified columns (and sort order) 
		 */
		public void js_sortDialog(String sortString)
		{
			checkDestroyed();
			formController.showSortDialog(sortString);
		}

		/**
		 * Sorts the form foundset based on the given sort string. 
		 * TIP: You can use the Copy button in the developer Select Sorting Fields dialog to get the needed syntax string for the desired sort fields/order. 
		 *
		 * @sample %%prefix%%controller.sort('columnA desc,columnB asc');
		 *
		 * @param sortString the specified columns (and sort order)
		 */
		public void js_sort(String sortString)
		{
			js_sort(sortString, false);
		}

		/**
		 * @clonedesc js_sort(String)
		 * @sampleas js_sort(String)
		 *
		 * @param sortString the specified columns (and sort order)
		 * @param defer the "sortString" will be just stored, without performing a query on the database (the actual sorting will be deferred until the next data loading action).
		 */
		public void js_sort(String sortString, boolean defer)
		{
			checkDestroyed();
			formController.sort(sortString, defer);
		}

		/**
		 * Performs a relookup for the current foundset record dataproviders.
		 * Lookups are defined in the dataprovider (columns) auto-enter setting and are normally performed over a relation upon record creation.
		 *
		 * @sample %%prefix%%controller.relookup();
		 */
		public void js_relookup()
		{
			checkDestroyed();
			formController.reLookupValues();
		}

		/**
		 * Add a filter parameter to limit the foundset permanently
		 * 
		 * @deprecated As of release 3.0, replaced by {@link foundset#addFoundSetFilterParam(String, String, Object, String)}.
		 * 
		 * @sample
		 * var success = %%prefix%%controller.addFoundSetFilterParam('customerid', '=', 'BLONP');//possible to add multiple
		 * %%prefix%%controller.loadAllRecords();//to make param(s) effective
		 *
		 * @param column_dataprovider_id 
		 * @param operator 
		 * @param value 
		 *
		 */
		@Deprecated
		public boolean js_addFoundSetFilterParam(String dataprovider, String operator, Object value)
		{
			checkDestroyed();
			FoundSet fs = formController.getFormModel();
			if (fs != null)
			{
				return fs.addFilterParam(null, dataprovider, operator, value);
			}
			return false;
		}

		/**
		 * Gets a value based on the specified dataprovider name. 
		 *
		 * @sample var val = %%prefix%%controller.getDataProviderValue('contact_name');
		 *
		 * @param dataProvider the dataprovider name to retieve the value for
		 * @return the dataprovider value (null if unknown dataprovider)
		 */
		public Object jsFunction_getDataProviderValue(String dataProvider)
		{
			checkDestroyed();
			FormScope formScope = formController.getFormScope();
			if (formScope != null && formScope.has(dataProvider, formScope))
			{
				return formScope.get(dataProvider, formScope);
			}
			FoundSet fs = formController.getFormModel();
			if (fs != null && fs.has(dataProvider, fs))
			{
				return fs.get(dataProvider, fs);
			}
			return null;
		}

		/**
		 * Sets the value based on a specified dataprovider name.
		 *
		 * @sample %%prefix%%controller.setDataProviderValue('contact_name','mycompany');
		 *
		 * @param dataprovider the dataprovider name to set the value for 
		 * @param value the value to set in the dataprovider 
		 */
		public void jsFunction_setDataProviderValue(String dataprovider, Object value)
		{
			checkDestroyed();
			FormScope formScope = formController.getFormScope();
			if (formScope != null && formScope.has(dataprovider, formScope))
			{
				formScope.put(dataprovider, formScope, value);
			}
			else
			{
				FoundSet fs = formController.getFormModel();
				if (fs != null)
				{
					fs.put(dataprovider, fs, value);
				}
			}
		}

		/**
		 * Set the preferred printer name to use when printing.
		 *
		 * @sample %%prefix%%controller.setPreferredPrinter('HP Laser 2200');
		 *
		 * @param printerName The name of the printer to be used when printing. 
		 */
		public void jsFunction_setPreferredPrinter(String printerName)
		{
			checkDestroyed();
			formController.setPreferredPrinterName(printerName);
		}

		/**
		 * Set the page format to use when printing.
		 *
		 * Orientation values:
		 * 0 - Landscape mode
		 * 1 - Portrait mode
		 * 
		 * Units values:
		 * 0 - millimeters
		 * 1 - inches
		 * 2 - pixels
		 * 
		 * Note: The unit specified for width, height and all margins MUST be the same.
		 *  
		 * @sample
		 * //Set page format to a custom size of 100x200 pixels with 10 pixel margins on all sides in portrait mode
		 * %%prefix%%controller.setPageFormat(100, 200, 10, 10, 10, 10);
		 * 
		 * //Set page format to a custom size of 100x200 pixels with 10 pixel margins on all sides in landscape mode
		 * %%prefix%%controller.setPageFormat(100, 200, 10, 10, 10, 10, SM_ORIENTATION.LANDSCAPE);
		 * 
		 * //Set page format to a custom size of 100x200 mm in landscape mode
		 * %%prefix%%controller.setPageFormat(100, 200, 0, 0, 0, 0, SM_ORIENTATION.LANDSCAPE, SM_UNITS.MM);
		 * 
		 * //Set page format to a custom size of 100x200 inch in portrait mode
		 * %%prefix%%controller.setPageFormat(100, 200, 0, 0, 0, 0, SM_ORIENTATION.PORTRAIT, SM_UNITS.INCH);
		 *
		 * @param width the specified width of the page to be printed.
		 * @param height the specified height of the page to be printed.
		 * @param leftmargin the specified left margin of the page to be printed.
		 * @param rightmargin the specified right margin of the page to be printed.
		 * @param topmargin the specified top margin of the page to be printed.
		 * @param bottommargin the specified bottom margin of the page to be printed.
		 */
		public void jsFunction_setPageFormat(double width, double height, double leftmargin, double rightmargin, double topmargin, double bottommargin)
		{
			jsFunction_setPageFormat(width, height, leftmargin, rightmargin, topmargin, bottommargin, PageFormat.PORTRAIT, 2);
		}

		/**
		 * @clonedesc jsFunction_setPageFormat(double, double, double, double, double, double)
		 * @sampleas jsFunction_setPageFormat(double, double, double, double, double, double)
		 * @param width the specified width of the page to be printed.
		 * @param height the specified height of the page to be printed.
		 * @param leftmargin the specified left margin of the page to be printed.
		 * @param rightmargin the specified right margin of the page to be printed.
		 * @param topmargin the specified top margin of the page to be printed.
		 * @param bottommargin the specified bottom margin of the page to be printed. 
		 * @param orientation the specified orientation of the page to be printed; the default is Portrait mode
		 */
		public void jsFunction_setPageFormat(double width, double height, double leftmargin, double rightmargin, double topmargin, double bottommargin,
			int orientation)
		{
			jsFunction_setPageFormat(width, height, leftmargin, rightmargin, topmargin, bottommargin, orientation, 2);
		}

		/**
		 * @clonedesc jsFunction_setPageFormat(double, double, double, double, double, double)
		 * @sampleas jsFunction_setPageFormat(double, double, double, double, double, double)
		 * @param width the specified width of the page to be printed.
		 * @param height the specified height of the page to be printed.
		 * @param leftmargin the specified left margin of the page to be printed.
		 * @param rightmargin the specified right margin of the page to be printed.
		 * @param topmargin the specified top margin of the page to be printed.
		 * @param bottommargin the specified bottom margin of the page to be printed. 
		 * @param orientation the specified orientation of the page to be printed; the default is Portrait mode
		 * @param units the specified units for the width and height of the page to be printed; the default is pixels
		 */
		public void jsFunction_setPageFormat(double width, double height, double leftmargin, double rightmargin, double topmargin, double bottommargin,
			int orientation, int units)
		{
			checkDestroyed();

			// translate the unit codes defined for this method to units used in the PageFormat classes
			int unitsType;
			switch (units)
			{
				case 0 :
					unitsType = Size2DSyntax.MM;
					break;
				case 1 :
					unitsType = Size2DSyntax.INCH;
					break;
				default : // pixels
					unitsType = (int)(Size2DSyntax.INCH / Utils.PPI);
					break;
			}

			PageFormat pf = Utils.createPageFormat(width, height, leftmargin, rightmargin, topmargin, bottommargin, orientation, unitsType);
			formController.setPageFormat(pf);
		}

		/**
		 * Gets the form width in pixels. 
		 *
		 * @sample var width = %%prefix%%controller.getFormWidth();
		 *
		 * @return the width in pixels
		 */
		public int js_getFormWidth()
		{
			checkDestroyed();
			return formController.getFormWidth();
		}

		/**
		 * Gets the part height in pixels. 
		 *
		 * @sample var height = %%prefix%%controller.getPartHeight(JSPart.BODY);
		 *
		 * @param partType The type of the part whose height will be returned.
		 *
		 * @return the part height in pixels
		 */
		public int js_getPartHeight(int partType)
		{
			checkDestroyed();
			return formController.getPartHeight(partType);
		}

		/**
		 * Returns the Y offset of a given part of the form. 
		 *
		 * @sample
		 * var offset = %%prefix%%controller.getPartYOffset(JSPart.BODY);
		 *
		 * @param partType The type of the part whose Y offset will be returned.
		 *
		 * @return A number holding the Y offset of the specified form part.
		 */
		public int js_getPartYOffset(int partType)
		{
			checkDestroyed();
			return formController.getPartYOffset(partType);
		}

		/** Get a design-time property of a form.
		 *
		 * @sample 
		 * var prop = forms.orders.controller.getDesignTimeProperty('myprop')	
		 */
		public Object js_getDesignTimeProperty(String key)
		{
			checkDestroyed();
			return formController.getDesignTimeProperty(key);
		}

		/**
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString()
		{
			return "Controller:" + (formController == null ? "<DESTROYED>" : formController.getName()); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	/*
	 * _____________________________________________________________ Declaration of attributes
	 */

	//see IForm for more
	public static final int TABLE_VIEW = 2;
	public static final int LOCKED_TABLE_VIEW = 3;
	public static final int LOCKED_LIST_VIEW = 4;

	public static final int FORM_EDITOR = Part.BODY;
	public static final int FORM_RENDERER = 0;

	private static final int PIN_VISIBLE = 1; // 1 is higher prio then 2
	private static final int PIN_HIDDEN = 2;

	private int currentViewType = -1;
	private IView view; //shows data (trough renderer(s))
	private FoundSet formModel; //performs the queries and passes stateobjects

	private final IDataRenderer[] dataRenderers = new IDataRenderer[Part.PART_ARRAY_SIZE]; //0 position == body_renderer

	private final IApplication application;
	private Form form;
	private Color bgColor = null;

	/**
	 * Some JavaScript related instances
	 */
	private FormScope formScope;
	private JSForm scriptableForm;// is the innerclass JSForm 
	private FormManager fm;
	private final IFormUIInternal containerImpl;
	private PageFormat pageFormat = null;
	private String namedInstance = null;

	/**
	 * Holds for each data renderer a map with <ISupportTabSeq, Component>. It is recreated each time createDataRenderers() is called. It is needed for setting
	 * the tab sequence in setView().
	 */
	private final TabSequenceHelper< ? > tabSequence;
	private DesignModeCallbacks designMode;

	public FormController(IApplication app, Form form, String namedInstance)
	{
		super();
		application = app;
		this.form = form;
		this.namedInstance = namedInstance;
		initStyles();
		fm = (FormManager)application.getFormManager();
		scriptExecuter = new ScriptExecuter(this);
		containerImpl = fm.getFormUI(this);
		pageFormat = PersistHelper.createPageFormat(form.getDefaultPageFormat());
		tabSequence = new TabSequenceHelper(containerImpl, application.getDataRenderFactory());
		app.getFlattenedSolution().registerLiveForm(form, namedInstance);
	}

	/**
	 * @return
	 */
	public boolean getDesignMode()
	{
		return designMode != null;
	}

	private int previousType = -1;

	/**
	 * @param mode
	 * @param controllerForm
	 */
	public void setDesignMode(DesignModeCallbacks callback)
	{
		this.designMode = callback;
		if (callback != null)
		{
			application.getFlattenedSolution().setInDesign(form);
			if (currentViewType == LOCKED_TABLE_VIEW || currentViewType == LIST_VIEW || currentViewType == LOCKED_LIST_VIEW)
			{
				previousType = currentViewType;
				currentViewType = RECORD_VIEW;
				recreateUI();
			}
			else
			{
				getFormUI().setDesignMode(callback);
			}
		}
		else
		{
			application.getFlattenedSolution().setInDesign(null);
			if (previousType != -1)
			{
				currentViewType = previousType;
				recreateUI();
				previousType = -1;
			}
			else
			{
				getFormUI().setDesignMode(callback);
			}
		}
	}

	private IStyleSheet stylesheet = null;
	private IStyleRule styleRule = null, styleOdd = null, styleEven = null, styleSelected = null, styleHeader = null;
	private IStyleRule bodyRule = null;

	void init()
	{
		initBorder();

		try
		{
			//create and fill all needed panels
			createDataRenderers(form.getView());

			createModel();
		}
		catch (Exception ex)
		{
			application.reportError(application.getI18NMessage("servoy.formPanel.error.setupForm") + ": " + getName(), ex); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private void initStylesAndBorder()
	{
		initStyles();
		initBorder();
	}

	private void initBorder()
	{
		Border border = ComponentFactoryHelper.createBorder(form.getBorderType());
		if (stylesheet != null && styleRule != null)
		{
			if (border == null)
			{
				border = stylesheet.getBorder(styleRule);
			}
			bgColor = stylesheet.getBackground(styleRule);
			if (bgColor != null)
			{
				containerImpl.setBackground(bgColor);
			}
		}
		if (border != null)
		{
			containerImpl.setBorder(border);
		}
	}

	private void initStyles()
	{
		Pair<IStyleSheet, IStyleRule> pairStyle = ComponentFactory.getCSSPairStyleForForm(application, form);
		if (pairStyle != null)
		{
			stylesheet = pairStyle.getLeft();
		}
		else
		{
			stylesheet = null;
		}
		if (stylesheet != null)
		{
			String lookupname = "form"; //$NON-NLS-1$

			if (form.getStyleClass() != null && !"".equals(form.getStyleClass())) //$NON-NLS-1$
			{
				String formStyleClass = form.getStyleClass();
				lookupname += '.' + formStyleClass;
			}
			styleRule = pairStyle.getRight();
			styleOdd = stylesheet.getCSSRule(lookupname + " " + ISupportRowStyling.CLASS_ODD); //$NON-NLS-1$
			styleEven = stylesheet.getCSSRule(lookupname + " " + ISupportRowStyling.CLASS_EVEN); //$NON-NLS-1$
			styleSelected = stylesheet.getCSSRule(lookupname + " " + ISupportRowStyling.CLASS_SELECTED); //$NON-NLS-1$
			styleHeader = stylesheet.getCSSRule(lookupname + " " + ISupportRowStyling.CLASS_HEADER); //$NON-NLS-1$
		}
	}

	public boolean recreateUI()
	{
		// should return false if executing from the form's execute function but
		// can happen if it is a none component thing
		// so no onfocus/ondatachange/onaction
		// of a component on the form but
		// onshow/onload that should be allowed
		//if (isFormExecutingFunction()) return false;
		Rectangle scrollPosition = null;
		if (view != null) scrollPosition = view.getVisibleRect();
		getFormUI().setDesignMode(null);
		Form f = application.getFlattenedSolution().getForm(form.getName());
		form = application.getFlattenedSolution().getFlattenedForm(f);
		initStylesAndBorder();
		int v = currentViewType;
		currentViewType = -1;
		setView(v);
		if (isFormVisible)
		{
			List<Runnable> invokeLaterRunnables = new ArrayList<Runnable>();
			IDataRenderer[] array = getDataRenderers();
			for (IDataRenderer dataRenderer : array)
			{
				if (dataRenderer != null)
				{
					dataRenderer.notifyVisible(true, invokeLaterRunnables);
				}
			}
			Utils.invokeLater(application, invokeLaterRunnables);
		}
		if (designMode != null)
		{
			application.invokeLater(new Runnable()
			{
				public void run()
				{
					getFormUI().setDesignMode(designMode);
				}
			});
		}
		getFormUI().uiRecreated();
		// make sure this form is seen as new.
		application.getFlattenedSolution().deregisterLiveForm(form, namedInstance);
		application.getFlattenedSolution().registerLiveForm(form, namedInstance);
		if (view != null && scrollPosition != null) view.setVisibleRect(scrollPosition);
		return true;

	}

	private void createDataRenderers(int viewType) throws Exception
	{
		int v = viewType;
		if (getDataSource() == null)
		{
			v = LOCKED_RECORD_VIEW; // form not based on any datasource
		}

		Part bodyPart = null;
		Map<Part, IDataRenderer> part_panels = new LinkedHashMap<Part, IDataRenderer>();
		Iterator<Part> e2 = form.getParts();
		while (e2.hasNext())
		{
			Part part = e2.next();

			Color partColor = ComponentFactory.getPartBackground(application, part, form);
			//extract the body (bgcolor)
			if (part.getPartType() == Part.BODY)
			{
				bodyPart = part;
				bgColor = partColor;
				Pair<IStyleSheet, IStyleRule> partStyle = ComponentFactory.getStyleForBasicComponent(application, part, form);
				bodyRule = partStyle != null ? partStyle.getRight() : null;
			}

			if (part.getPartType() == Part.BODY && v == FormController.LOCKED_TABLE_VIEW)
			{
				continue;//don't create part, view == part
			}

			IDataRenderer dr = application.getDataRenderFactory().getEmptyDataRenderer(ComponentFactory.getWebID(form, part), part.toString(), application,
				(part.getPartType() == Part.BODY));
			dr.initDragNDrop(this, form.getPartStartYPos(part.getID()));
			dataRenderers[part.getPartType()] = dr;
			dr.setName(part.toString());
			part_panels.put(part, dr);

			if (part.getPartType() == Part.BODY)
			{
				int onRenderMethodID = form.getOnRenderMethodID();
				if (onRenderMethodID > 0)
				{
					dr.getOnRenderComponent().getRenderEventExecutor().setRenderCallback(Integer.toString(onRenderMethodID),
						Utils.parseJSExpressions(form.getInstanceMethodArguments("onRenderMethodID")));
					dr.getOnRenderComponent().getRenderEventExecutor().setRenderScriptExecuter(getScriptExecuter());
				}
			}
			dr.setBackground(partColor);
		}

		tabSequence.clear();
		application.getDataRenderFactory().completeRenderers(application, form, scriptExecuter, part_panels, form.getSize().width, false,
			containerImpl.getUndoManager(), tabSequence);

		if (bodyPart != null && (v == FormController.LOCKED_LIST_VIEW || v == IForm.LIST_VIEW))
		{
			IDataRenderer dr = application.getDataRenderFactory().getEmptyDataRenderer(ComponentFactory.getWebID(form, bodyPart), bodyPart.toString(),
				application, true);

			int onRenderMethodID = form.getOnRenderMethodID();
			if (onRenderMethodID > 0)
			{
				dr.getOnRenderComponent().getRenderEventExecutor().setRenderCallback(Integer.toString(onRenderMethodID),
					Utils.parseJSExpressions(form.getInstanceMethodArguments("onRenderMethodID")));
				dr.getOnRenderComponent().getRenderEventExecutor().setRenderScriptExecuter(getScriptExecuter());
			}
			//apply bgcolor to renderer				
			if (bgColor != null)
			{
				dr.setBackground(bgColor);
			}

			dataRenderers[FORM_RENDERER] = dr;
			dr.setName(bodyPart.toString());
			part_panels = new LinkedHashMap<Part, IDataRenderer>();
			part_panels.put(bodyPart, dr);
			application.getDataRenderFactory().completeRenderers(application, form, scriptExecuter, part_panels, form.getSize().width, false,
				containerImpl.getUndoManager(), null);
		}

		//apply security
		int access = application.getFlattenedSolution().getSecurityAccess(form.getUUID());
		if (access != -1)
		{
			boolean b_accessible = ((access & IRepository.ACCESSIBLE) != 0);
			if (!b_accessible)
			{
				for (IDataRenderer dataRenderer : dataRenderers)
				{
					if (dataRenderer != null)
					{
						Iterator< ? extends IComponent> componentIterator = dataRenderer.getComponentIterator();
						while (componentIterator.hasNext())
						{
							IComponent c = componentIterator.next();
							if (c instanceof ISupportSecuritySettings)
							{
								if (!b_accessible) ((ISupportSecuritySettings)c).setAccessible(b_accessible);
							}
						}
					}
				}
			}
		}
	}

	public String getName()
	{
		return (namedInstance == null ? form.getName() : namedInstance);
	}

	/**
	 * Returns the name of the window/dialog in which this form is showing. In case of main application window, it will return IApplication.APP_WINDOW_NAME to differentiate from the case where
	 * no container is found. (null)
	 * @return the name of the window/dialog in which this form is showing. If there is no such dialog/window returns null. In case of main application window, it will return IApplication.APP_WINDOW_NAME to differentiate from the case where
	 * no dialog/window is found.
	 */
	public String getContainerName()
	{
		return getFormUI().getContainerName();
	}

	public IApplication getApplication()
	{
		return application;
	}

	/**
	 * @return IDataRenderer[]
	 */
	public IDataRenderer[] getDataRenderers()
	{
		return dataRenderers;
	}

	public ITagResolver getTagResolver()
	{
		for (IDataRenderer element : dataRenderers)
		{
			if (element != null)
			{
				return element.getDataAdapterList();
			}
		}
		IRecordInternal state = null;
		if (formModel != null)
		{
			int index = formModel.getSelectedIndex();
			if (index != -1)
			{
				state = formModel.getRecord(index);
			}
			if (state == null)
			{
				state = formModel.getPrototypeState();
			}
		}
		else
		{
			state = new PrototypeState(null);
		}
		return TagResolver.createResolver(state);
	}

	/*
	 * _____________________________________________________________ The methods below belong to interface IDataManipulator
	 */
	public ControllerUndoManager getUndoManager()
	{
		return containerImpl.getUndoManager();
	}

	public Form getForm()
	{
		return form;
	}

	public FoundSet getFormModel()
	{
		return formModel;
	}

	public IFoundSetInternal getFoundSet()
	{
		return getFormModel();
	}

	private boolean destroyed = false;

	public void destroy()
	{
		try
		{
			if (this.designMode != null)
			{
				setDesignMode(null);
			}
			if (form.getOnUnLoadMethodID() > 0)
			{
				executeFormMethod(StaticContentSpecLoader.PROPERTY_ONUNLOADMETHODID, new Object[] { getJSEvent(formScope) }, Boolean.TRUE, true, true);
			}
			containerImpl.destroy();

			application.getFoundSetManager().getEditRecordList().removePrepareForSave(this);
			((FoundSetManager)application.getFoundSetManager()).removeFoundSetListener(this);

			if (fm != null) fm.removeFormPanel(this);
			fm = null;

			if (formModel != null)
			{
				((ISwingFoundSet)formModel).getSelectionModel().removeListSelectionListener(this);
				((ISwingFoundSet)formModel).getSelectionModel().removeFormController(this);
				//			formModel.removeEditListener(this);
				((ISwingFoundSet)formModel).removeTableModelListener(this);
				formModel.flushAllCachedItems();//to make sure all data is gc'ed
			}
			setFormModelInternal(null);
			lastState = null;

			if (view != null) //it may not yet exist
			{
				try
				{
					view.destroy();
					view = null;
				}
				catch (Exception e)
				{
					Debug.error(e);
				}
			}

			deleteRenderers();

			SolutionScope solScope = application.getScriptEngine().getSolutionScope();
			((CreationalPrototype)solScope.get("forms", solScope)).removeFormPanel(this); //$NON-NLS-1$

			hmChildrenJavaMembers = null;
			if (formScope != null)
			{
				formScope.destroy();
			}
			formScope = null;
			if (scriptableForm != null)
			{
				scriptableForm.destroy();
			}
			scriptableForm = null;
			if (scriptExecuter != null)
			{
				scriptExecuter.destroy();
			}
			scriptExecuter = null;
			destroyed = true;
		}
		catch (Exception e)
		{
			Debug.error("Destroy error", e); //$NON-NLS-1$
		}
		finally
		{
			application.getFlattenedSolution().deregisterLiveForm(form, namedInstance);
		}
	}

	/**
	 * @return the destroyed
	 */
	public boolean isDestroyed()
	{
		return destroyed;
	}

	private void deleteRenderers()
	{
		for (int i = 0; i < dataRenderers.length; i++)
		{
			if (dataRenderers[i] != null)
			{
				try
				{
					dataRenderers[i].destroy();
				}
				catch (Exception e)
				{
					Debug.error(e);
				}
				dataRenderers[i] = null;
			}
		}
	}

	public boolean loadRecords(IFoundSet foundSet)
	{
		return loadData(foundSet, null);
	}

	public boolean loadData(Object data, Object[] args)
	{
		boolean returnValue = true;
		//		saveData(); not needed and causes stackoverflow in printing!!
		if (data == null)
		{
			try
			{
				returnValue = setModel(null);
			}
			catch (Exception e)
			{
//				application.reportError(application.getI18NMessage("servoy.formPanel.error.settingFoundset") + e.getMessage(), e); //$NON-NLS-1$
//				returnValue = false;
				throw new RuntimeException(application.getI18NMessage("servoy.formPanel.error.settingFoundset") + e.getMessage(), e); //$NON-NLS-1$
			}
		}
		else if (data instanceof FoundSet)
		{
			try
			{
				FoundSet fs = (FoundSet)data;

				boolean sourceNew = application.getFoundSetManager().isNew(fs);
				boolean sourceSeparate = !application.getFoundSetManager().isShared(fs);
				boolean sourceRelated = (fs instanceof RelatedFoundSet);
				boolean destinationSeparate = form.getUseSeparateFoundSet();
				boolean destinationRelated = (formModel instanceof RelatedFoundSet);

				if (sourceRelated || !sourceSeparate && !destinationSeparate)
				{
					returnValue = setModel(fs);
				}
				else if (sourceNew && !destinationSeparate)
				{
					returnValue = setModel(fs);
				}
				else if (!destinationRelated && destinationSeparate)
				{
					if (formModel == null)
					{
						setFormModelInternal((FoundSet)application.getFoundSetManager().getSeparateFoundSet(this, getDefaultSortColumns()));
					}
					formModel.copyFrom(fs);
					returnValue = setModel(formModel);
				}
				else
				{
					returnValue = setModel((FoundSet)fs.copy(false));
				}
			}
			catch (Exception ex)
			{
//				application.reportError(application.getI18NMessage("servoy.formPanel.error.settingFoundset") + ex.getMessage(), ex); //$NON-NLS-1$
//				returnValue = false;
				throw new RuntimeException(application.getI18NMessage("servoy.formPanel.error.settingFoundset") + ex.getMessage(), ex); //$NON-NLS-1$
			}
		}
		else if (data instanceof JSDataSet || data instanceof IDataSet || data instanceof Number || data instanceof UUID)
		{
			try
			{
				if (data instanceof Number || data instanceof UUID)
				{
					BufferedDataSet dataset = new BufferedDataSet();
					dataset.addRow(new Object[] { data });
					data = dataset;
				}
				if (formModel == null)
				{
					setFormModelInternal((FoundSet)application.getFoundSetManager().getSeparateFoundSet(this, getDefaultSortColumns()));
				}
				if (formModel.getRelationName() != null)
				{
					((FoundSetManager)application.getFoundSetManager()).giveMeFoundSet(this);
				}
				IDataSet set = null;
				if (data instanceof JSDataSet)
				{
					if (((JSDataSet)data).js_getException() != null)
					{
						throw ((JSDataSet)data).js_getException();
					}
					set = ((JSDataSet)data).getDataSet();
				}
				else
				{
					set = (IDataSet)data;
				}
				returnValue = formModel.loadExternalPKList(set);
				if (returnValue)
				{
					returnValue = setModel(formModel);
				}
			}
			catch (Exception ex)
			{
//				application.reportError(application.getI18NMessage("servoy.formPanel.error.loadingPKData") + ex.getMessage(), ex); //$NON-NLS-1$
//				returnValue = false;
				throw new RuntimeException(application.getI18NMessage("servoy.formPanel.error.settingFoundset") + ex.getMessage(), ex); //$NON-NLS-1$
			}
		}
		else if (data instanceof String || data instanceof IQueryBuilder)
		{
			try
			{
				if (formModel == null)
				{
					setFormModelInternal((FoundSet)application.getFoundSetManager().getSeparateFoundSet(this, getDefaultSortColumns()));
				}
				if (formModel.getRelationName() != null)
				{
					((FoundSetManager)application.getFoundSetManager()).giveMeFoundSet(this);
				}

				if (data instanceof String) returnValue = formModel.loadByQuery((String)data, args);
				else returnValue = formModel.loadByQuery((IQueryBuilder)data);

				if (returnValue)
				{
					returnValue = setModel(formModel);
				}
			}
			catch (Exception ex)
			{
				application.reportError(application.getI18NMessage("servoy.formPanel.error.loadingPKData") + ex.getMessage(), ex); //$NON-NLS-1$
				returnValue = false;
			}
		}
		return returnValue;
		/*
		 * this is done in setModel (when visible) and may not be done here if (formModel != null) { int selected = formModel.getSelectedIndex(); if (selected
		 * != -1) { IRecord row = formModel.getRecord(selected); refreshAllPartRenderers(row,selected,true); } else { refreshAllPartRenderers(null,-1,true); } }
		 */
	}

	public boolean show(Object data, Object window, Object[] windowArgs) throws ServoyException
	{
		return show(data, null, window, windowArgs);
	}

	public boolean show(Object data, Object[] dataArgs, Object window, Object[] windowArgs) throws ServoyException
	{
		boolean b = loadData(data, dataArgs); //notify visible will show the data and set selected record
		if (!b) return false;
		if (window != null)
		{
			showForm(window, windowArgs);
		}
		else
		{
			showForm(null, null);
		}
		return true;
	}

	private void focusFirstField()
	{
		focusField(null, false);
	}

	private void focusField(String fieldName, final boolean skipReadonly)
	{
		final Object field = tabSequence.getComponentForFocus(fieldName, skipReadonly);
		if (field != null)
		{
			// do it with invoke later so that other focus events are overridden.
			application.invokeLater(new Runnable()
			{
				public void run()
				{
					containerImpl.focusField(field);
				}
			});
		}
	}

	public void loadAllRecords()
	{
		if (form.getOnShowAllRecordsCmdMethodID() == 0)
		{
			try
			{
				loadAllRecordsImpl(true);
			}
			catch (Exception ex)
			{
				application.handleException(application.getI18NMessage("servoy.formPanel.error.showFormData"), ex); //$NON-NLS-1$
			}
		}
		else
		{
			executeFormMethod(StaticContentSpecLoader.PROPERTY_ONSHOWALLRECORDSCMDMETHODID, null, Boolean.FALSE, true, true);
		}
	}

	public boolean loadAllRecordsImpl(boolean unrelate) throws ServoyException
	{
		if (formModel == null) return false;

		if (formModel.isInitialized())
		{
			int stopped = application.getFoundSetManager().getEditRecordList().stopEditing(false);
			if (stopped != ISaveConstants.STOPPED && stopped != ISaveConstants.AUTO_SAVE_BLOCKED)
			{
				return false;
			}
		}

		if (formModel.isInFindMode() && performFindImpl(false, true, false) < 0 /* search blocked */)
		{
			return false;
		}

		if (formModel.getRelationName() == null && !form.getUseSeparateFoundSet() && !((FoundSetManager)application.getFoundSetManager()).isShared(formModel))
		{
			//make sure to place the shared foundset back if not separate.
			((FoundSetManager)application.getFoundSetManager()).giveMeFoundSet(this);
		}

		if (unrelate && formModel.getRelationName() != null)
		{
			//check if we use our own formModel, could be replaced by loadData
			((FoundSetManager)application.getFoundSetManager()).giveMeFoundSet(this);
		}

		formModel.loadAllRecords();
		return true;
	}

	/*
	 * @see com.servoy.j2db.IForm#refreshView()
	 */
	public void refreshView()
	{
		if (formModel != null)
		{
			int selected[] = null;
			if (getView() == RECORD_VIEW || getView() == LOCKED_RECORD_VIEW)
			{
				int selIdx = formModel.getSelectedIndex();
				if (selIdx != -1) selected = new int[] { selIdx };
			}
			else
			{
				selected = formModel.getSelectedIndexes();
			}
			if (selected != null && selected.length > 0)
			{
				IRecordInternal[] row = new IRecordInternal[selected.length];
				for (int i = 0; i < selected.length; i++)
					row[i] = formModel.getRecord(selected[i]);
				refreshAllPartRenderers(row);
			}
			else
			{
				refreshAllPartRenderers(null);
			}
			if (view != null && view.isDisplayingMoreThanOneRecord())
			{
				formModel.fireFoundSetChanged();
			}
		}
	}

	// Have to check it here because formEditor.getDataAdapterList().getState() can already have the new one 
	private IRecordInternal[] lastState = null;

	private void refreshAllPartRenderers(IRecordInternal[] records)
	{
		if (!isFormVisible || application.isShutDown()) return;
		boolean executeOnRecordSelect = false;
		IRecordInternal[] state = records;
		if (state == null)
		{
			if (formModel != null)
			{
				state = new IRecordInternal[] { formModel.getPrototypeState() };
			}
			else
			{
				state = new IRecordInternal[] { new PrototypeState(null) };
			}
		}
		if (dataRenderers[FORM_EDITOR] != null && !(records == null && formModel != null && formModel.getRawSize() > 0) && isStateChanged(state))
		{
			lastState = state;
			executeOnRecordSelect = true;
		}

		for (int i = FORM_RENDERER + 1; i < dataRenderers.length; i++)
		{
			IDataRenderer dataRenderer = dataRenderers[i];
			if (dataRenderer != null)
			{
				for (IRecordInternal r : state)
					dataRenderer.refreshRecord(r);
			}
		}

		if (executeOnRecordSelect)
		{
			// do this at the end because dataRenderer.refreshRecord(state) will update selection
			// for related tabs - and we should execute js code after they have been updated
			executeOnRecordSelect();
		}
	}

	private boolean isStateChanged(IRecordInternal[] state)
	{
		if (lastState == null || state == null) return !(lastState == state);
		if (lastState.length != state.length) return true;

		for (int i = 0; i < lastState.length; i++)
		{
			if (!Utils.safeEquals(lastState[i], state[i])) return true;
		}
		return false;
	}

	public void tableChanged(TableModelEvent e)
	{
		//		int index = formModel.getSelectedIndex();
		//		if (e.getFirstRow() <= index && e.getLastRow() >= index) we have to update the other renderers for non current record
		//		{
		valueChanged(null);//fire value chance because selection does not fire
		//		}			
	}

	private int lastAdjusting = -1;

	public void valueChanged(ListSelectionEvent e)
	{
		if (!isFormVisible || formModel == null) return;

		// do call getSize() so that the form model is in queried state again
		// this will be done by the getRecord() call below for the most part.
		// Except if the selection == -1 and mustQuery = true. Then the only trigger is getSize()
		formModel.getSize();

		try
		{
			// Can't test for isAdjusting!! Table view fires first isAdjusting == false and then the editor (a button) will be fired then another event with true
			if (e != null && e.getValueIsAdjusting())
			{
				// Can't do this because editor is first displayed in a table view before selection is changed
				if (lastAdjusting != formModel.getSelectedIndex())
				{
					lastAdjusting = formModel.getSelectedIndex();
					//System.out.println("calling save data" + eventCanBeNull);
					application.getFoundSetManager().getEditRecordList().stopIfEditing(formModel);
				}
				//if (!didSave) executeOnRecordSave();
			}
			else
			{
				lastAdjusting = -1;
				int[] index = null;
				if (getView() == RECORD_VIEW || getView() == LOCKED_RECORD_VIEW)
				{
					int selIdx = formModel.getSelectedIndex();
					if (selIdx != -1) index = new int[] { selIdx };
				}
				else
				{
					index = formModel.getSelectedIndexes();
				}

				IRecordInternal[] state = null;
				if (index != null && index.length > 0)
				{
					state = new IRecordInternal[index.length];
					for (int i = 0; i < index.length; i++)
						state[i] = formModel.getRecord(index[i]);
					//executeOnRecordShow();
				}
				refreshAllPartRenderers(state); //skipEditor the controller receives itself events		
			}
		}
		catch (RuntimeException ex)
		{
			if (ex.getCause() instanceof ServoyException)
			{
				getApplication().handleException(ex.getCause().getMessage(), (ServoyException)ex.getCause());
			}
			else
			{
				getApplication().reportError(ex.getMessage(), ex);
			}
		}
	}

	public void removeLastFound()
	{
		try
		{
			if (formModel != null) formModel.removeLastFound();
		}
		catch (Exception ex)
		{
			Debug.error(ex);
		}
	}

	public void showSortDialog()//this one is called by the cmd
	{
		if (form.getOnSortCmdMethodID() == 0)
		{
			showSortDialog(null);
		}
		else
		{
			executeFormMethod(StaticContentSpecLoader.PROPERTY_ONSORTCMDMETHODID, null, Boolean.TRUE, true, true);
		}
	}

	private void showSortDialog(String options)
	{
		getFormUI().showSortDialog(application, options);
	}

	public List<SortColumn> getDefaultSortColumns()
	{
		try
		{
			return ((FoundSetManager)application.getFoundSetManager()).getSortColumns(form.getDataSource(), form.getInitialSort());
		}
		catch (RepositoryException e)
		{
			Debug.error(e);
			return new ArrayList<SortColumn>();
		}
	}

	public void sort(List<SortColumn> sortColumns, boolean defer)
	{
		//		this is a semi saveData , we do NOT want the record go out of edit(!) and is updated in db
		if (application.getFoundSetManager().getEditRecordList().prepareForSave(true) != ISaveConstants.STOPPED)
		{
			if (Debug.tracing())
			{
				Debug.trace("Function not executed because stopUIEditing returned false, probably because a validation failure"); //$NON-NLS-1$
			}
			return;
		}
		try
		{
			if (formModel != null) formModel.sort(sortColumns, defer);
		}
		catch (Exception ex)
		{
			application.reportError(application.getI18NMessage("servoy.formPanel.error.sortRecords"), ex); //$NON-NLS-1$
		}
	}

	public void sort(String options, boolean defer)
	{
		List<SortColumn> sortColumns = new ArrayList<SortColumn>();
		try
		{
			sortColumns = ((FoundSetManager)application.getFoundSetManager()).getSortColumns(form.getDataSource(), options);
		}
		catch (RepositoryException e)
		{
			Debug.error(e);
		}
		sort(sortColumns, defer);
	}

	void showForm(Object window, Object[] windowArgs) throws ServoyException
	{
		if (!application.getFlattenedSolution().formCanBeInstantiated(getForm()))
		{
			// abstract form
			throw new ApplicationException(ServoyException.ABSTRACT_FORM);
		}

		if (window == null)
		{
			fm.showFormInCurrentContainer(getName());
		}
		else
		{
			if (window instanceof String)
			{
				Rectangle rect = new Rectangle(-1, -1, -1, -1);
				String title = null;
				boolean resizeble = true;
				boolean showTextToolbar = false;
				boolean modal = false;
				boolean dialog = true;
				if (windowArgs != null && windowArgs.length > 0)
				{
					if (windowArgs[0] == null)
					{
						dialog = false;
					}
					else
					{
						modal = Utils.getAsBoolean(windowArgs[0]);
					}
				}

				if (dialog)
				{
					fm.showFormInDialog(getName(), rect, title, resizeble, showTextToolbar, true, modal, (String)window);
				}
				else
				{
					fm.showFormInFrame(getName(), rect, title, resizeble, showTextToolbar, (String)window);
				}
			}
			else if (window instanceof JSWindow)
			{
				((JSWindow)window).getImpl().show(getName());
			}
			else
			{
				fm.showFormInMainPanel(getName());
			}

		}
	}

	int getRecordIndex()
	{
		return (formModel != null ? formModel.getSelectedIndex() : -1);
	}

	void setRecordIndex(int i)
	{
		if (formModel != null) formModel.setSelectedIndex(i);
	}

	public void reLookupValues()
	{
		if (formModel != null) formModel.processCopyValues(getRecordIndex());
	}

	public void showOmittedRecords()
	{
		if (form.getOnShowOmittedRecordsCmdMethodID() == 0)
		{
			try
			{
				showOmittedRecordsImpl();
			}
			catch (Exception ex)
			{
				application.handleException(application.getI18NMessage("servoy.formPanel.error.omittedRecords"), ex); //$NON-NLS-1$
			}
		}
		else
		{
			executeFormMethod(StaticContentSpecLoader.PROPERTY_ONSHOWOMITTEDRECORDSCMDMETHODID, null, Boolean.TRUE, true, true);
		}
	}

	private boolean showOmittedRecordsImpl() throws ServoyException
	{
		if (formModel != null && formModel.getTable() == null)
		{
			return false;
		}
		checkInitialized();

		if (application.getFoundSetManager().getEditRecordList().stopIfEditing(formModel) == ISaveConstants.STOPPED)
		{
			return formModel.showOmitted();
		}
		return false;
	}

	public void invertRecords()
	{
		if (form.getOnInvertRecordsCmdMethodID() == 0)
		{
			try
			{
				invertRecordsImpl();
			}
			catch (Exception ex)
			{
				application.handleException(application.getI18NMessage("servoy.formPanel.error.invertRecords"), ex); //$NON-NLS-1$
			}
		}
		else
		{
			executeFormMethod(StaticContentSpecLoader.PROPERTY_ONINVERTRECORDSCMDMETHODID, null, Boolean.TRUE, true, true);
		}
	}

	private boolean invertRecordsImpl() throws ServoyException
	{
		if (formModel != null && formModel.getTable() == null)
		{
			return false;
		}
		checkInitialized();

		if (application.getFoundSetManager().getEditRecordList().stopIfEditing(formModel) == ISaveConstants.STOPPED)
		{
			formModel.invert();
			return true;
		}
		return false;
	}

	public void omitRecord()
	{
		if (form.getOnOmitRecordCmdMethodID() == 0)
		{
			try
			{
				omitRecordImpl();
			}
			catch (Exception ex)
			{
				application.handleException(application.getI18NMessage("servoy.formPanel.error.omitRecord"), ex); //$NON-NLS-1$
			}
		}
		else
		{
			executeFormMethod(StaticContentSpecLoader.PROPERTY_ONOMITRECORDCMDMETHODID, null, Boolean.TRUE, true, true);
		}
	}

	private boolean omitRecordImpl() throws ServoyException
	{
		if (formModel != null && formModel.getTable() == null)
		{
			return false;
		}
		checkInitialized();

		int[] omitRecIdx = formModel.getSelectedIndexes();
		return formModel.omitState(omitRecIdx);
	}

	public boolean deleteRecord()
	{
		if ((formModel != null && formModel.isInFindMode()) || form.getOnDeleteRecordCmdMethodID() == 0)
		{
			try
			{
				return deleteRecordImpl();
			}
			catch (Exception ex)
			{
				application.handleException(application.getI18NMessage("servoy.formPanel.error.deleteRecord"), ex); //$NON-NLS-1$
			}
			return true;
		}
		return !Boolean.FALSE.equals(executeFormMethod(StaticContentSpecLoader.PROPERTY_ONDELETERECORDCMDMETHODID, null, Boolean.TRUE, true, true));
	}

	private boolean deleteRecordImpl() throws ServoyException
	{
		if (formModel != null && formModel.getTable() == null)
		{
			return false;
		}
		checkInitialized();

		int[] deleteRecIdx = formModel.getSelectedIndexes();

		boolean success = true;
		for (int i = deleteRecIdx.length - 1; i > -1; i--)
		{
			if (deleteRecIdx[i] >= 0) formModel.deleteRecord(deleteRecIdx[i]); // will throw exception on error
			else success = false;
		}

		return success;
	}

	public boolean deleteAllRecords()
	{
		if (form.getOnDeleteAllRecordsCmdMethodID() == 0)
		{
			int but = JOptionPane.showConfirmDialog(
				(Component)containerImpl,
				application.getI18NMessage("servoy.formPanel.deleteall.warning"), application.getI18NMessage("servoy.formPanel.deleteall.text"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE); //$NON-NLS-1$ //$NON-NLS-2$
			if (but == JOptionPane.YES_OPTION)
			{
				try
				{
					return deleteAllRecordsImpl();
				}
				catch (Exception ex)
				{
					application.handleException(application.getI18NMessage("servoy.formPanel.error.deleteRecords"), ex); //$NON-NLS-1$
				}
			}
		}
		else
		{
			executeFormMethod(StaticContentSpecLoader.PROPERTY_ONDELETEALLRECORDSCMDMETHODID, null, Boolean.TRUE, true, true);
		}
		return false;
	}

	private boolean deleteAllRecordsImpl() throws ServoyException
	{
		if (formModel != null && formModel.getTable() == null)
		{
			return false;
		}
		checkInitialized();

		formModel.deleteAllRecords();
		return true;
	}

	public void newRecord()
	{
		if ((formModel != null && formModel.isInFindMode()) || form.getOnNewRecordCmdMethodID() == 0)
		{
			try
			{
				newRecordImpl(0);
				focusFirstField();
			}
			catch (Exception e)
			{
				application.handleException(application.getI18NMessage("servoy.formPanel.error.newRecord"), e); //$NON-NLS-1$
			}
		}
		else
		{
			executeFormMethod(StaticContentSpecLoader.PROPERTY_ONNEWRECORDCMDMETHODID, null, Boolean.TRUE, true, true);
		}
	}

	private boolean newRecordImpl(int index) throws ServoyException
	{
		if (formModel != null && formModel.getTable() == null)
		{
			return false;
		}
		checkInitialized();

		if (fm.getCurrentMainShowingFormController() == this && formModel.getRelationName() != null)
		{
			//this tests if a form is main and if it is used showing related data, if so:
			// TODO disabled this for now
			//loadAllRecordsImpl(true);//unrelate !! ,otherwise a foreign key is filled in which is unwanted behavior
			// Do a copy so that the foundset itself stays exactly the same (same set of data, but the foundset is unrelated)
			setModel((FoundSet)formModel.copy(true));
		}
		int idx = formModel.newRecord(index);
		if (idx != -1)
		{
			setRecordIndex(idx);
			return true;
		}
		return false;
	}

	public void duplicateRecord()
	{
		if ((formModel != null && formModel.isInFindMode()) || form.getOnDuplicateRecordCmdMethodID() == 0)
		{
			try
			{
				duplicateRecordImpl(0);
			}
			catch (Exception ex)
			{
				application.handleException(application.getI18NMessage("servoy.formPanel.error.duplicateRecord"), ex); //$NON-NLS-1$
			}
		}
		else
		{
			executeFormMethod(StaticContentSpecLoader.PROPERTY_ONDUPLICATERECORDCMDMETHODID, null, Boolean.TRUE, true, true);
		}
	}

	private boolean duplicateRecordImpl(int indexToAdd) throws ServoyException
	{
		if (formModel != null && formModel.getTable() == null)
		{
			return false;
		}
		checkInitialized();

		if (fm.getCurrentMainShowingFormController() == this && formModel.getRelationName() != null)
		{
			//this tests if a form is main and if it is used showing related data, if so:
			// TODO disabled this for now
			//loadAllRecordsImpl(true);//unrelate !! ,otherwise a foreign key is filled in which is unwanted behavior
			// Do a copy so that the foundset itself stays exactly the same (same set of data, but the foundset is unrelated)
			setModel((FoundSet)formModel.copy(true));
		}
		int idx = formModel.duplicateRecord(getRecordIndex(), indexToAdd);
		if (idx != -1)
		{
			setRecordIndex(idx);
			return true;
		}
		return false;
	}

	public boolean isShowingData()
	{
		if (formModel == null) return false;
		return formModel.isInitialized();
	}

	public void checkInitialized()
	{
		if (!isInFindMode() && !isShowingData())
		{
			throw new RuntimeException(application.getI18NMessage("servoy.formPanel.error.formNotInitialized")); //$NON-NLS-1$
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.JComponent#requestFocus()
	 */
	public void requestFocus()
	{
		if (containerImpl instanceof JComponent)
		{
			JComponent sf = (JComponent)containerImpl; // swingForm, TODO: remove dependency on smart client
			sf.requestFocus();
		}
		else if (view != null)
		{
			view.requestFocus();
		}
	}

	public void find()
	{
		if (form.getOnFindCmdMethodID() == 0)
		{
			try
			{
				findImpl(false);
			}
			catch (Exception e)
			{
				application.handleException(application.getI18NMessage("servoy.formPanel.error.searchFailed"), e); //$NON-NLS-1$
			}
		}
		else
		{
			executeFormMethod(StaticContentSpecLoader.PROPERTY_ONFINDCMDMETHODID, null, Boolean.TRUE, true, true);
		}
	}

	private boolean findImpl(boolean invokeFromJS) throws ServoyException
	{
		if (formModel != null && formModel.getTable() == null)
		{
			return false;
		}

		//		this is a semi saveData , we do NOT want the record go out of edit(!) and is updated in db
		if (application.getFoundSetManager().getEditRecordList().prepareForSave(true) != ISaveConstants.STOPPED)
		{
			if (Debug.tracing())
			{
				Debug.trace("Function not executed because stopUIEditing returned false, probably because a validation failure"); //$NON-NLS-1$
			}
			return false;
		}
		if (application.getFoundSetManager().getEditRecordList().hasEditedRecords(formModel))
		{
			if (application.getFoundSetManager().getEditRecordList().stopEditing(false) != ISaveConstants.STOPPED)
			{
				//we cannot allow finds when there are editing records...it possible to start (related!)find on table which would possible not include editing records
				if (Debug.tracing())
				{
					Debug.trace("Tried to start a find in form '" + getName() + "', but there where records in edit mode and auto save is false"); //$NON-NLS-1$ //$NON-NLS-2$
				}
				return false;
			}

			// test again if there are edited records because there are situations (double stopEdtting calls) that the above call returns STOPPED
			// but there are still edited records.
			if (application.getFoundSetManager().getEditRecordList().hasEditedRecords(formModel))
			{
				return false;
			}
		}

		if (fm.getCurrentMainShowingFormController() == this)
		{
			if (formModel.getRelationName() != null)
			{
				((FoundSetManager)application.getFoundSetManager()).giveMeFoundSet(this);
			}
			IModeManager mm = application.getModeManager();
			if (mm.getMode() == IModeManager.FIND_MODE) //calling find when in find?
			{
				setMode(IModeManager.FIND_MODE);
			}
			else
			{
				mm.setMode(IModeManager.FIND_MODE);
			}
			if (Debug.tracing())
			{
				Debug.trace("Find started form main form: " + getName()); //$NON-NLS-1$
			}
			if (!invokeFromJS) focusFirstField();
			return true;
		}
		else
		{
			boolean b = setMode(IModeManager.FIND_MODE);
			if (b)
			{
				//in case of tabpanels!
				if (application.getCmdManager() instanceof ICmdManagerInternal)
				{
					((ICmdManagerInternal)application.getCmdManager()).ableFormRelatedFindActions(true);
				}
				if (Debug.tracing())
				{
					Debug.trace("Find started for tab form: " + getName()); //$NON-NLS-1$
				}
				if (!invokeFromJS) focusFirstField();
			}
			return b;
		}
	}

	private void exitFindMode()
	{
		// if this foundset/model is or is used by current mainShowingFormController then exit find mode
		// (because foundset keeps the find state of a FormController)
		FormController mainShowingFormController = fm.getCurrentMainShowingFormController();
		if (mainShowingFormController == null || mainShowingFormController.getFormModel() != getFormModel())
		{
			setMode(IModeManager.EDIT_MODE); //if was special return to normal from tabpanel
		}
		else
		{
			application.getModeManager().setMode(IModeManager.EDIT_MODE);
		}
	}

	public void printPreview()//called by cmd
	{
		if (form.getOnPrintPreviewCmdMethodID() == 0)
		{
			PrinterJob printerJob = null;//if null it uses the javaxp.printing PrinterJob.getPrinterJob();
			printPreview(true, false, 100, printerJob);
		}
		else
		{
			executeFormMethod(StaticContentSpecLoader.PROPERTY_ONPRINTPREVIEWCMDMETHODID, null, Boolean.TRUE, true, true);
		}
	}

	private void printPreview(boolean showDialogs, boolean printCurrentRecordOnly, int zoomFactor, PrinterJob printerJob)
	{
		getFormUI().printPreview(showDialogs, printCurrentRecordOnly, zoomFactor, printerJob);
	}

	public void print(boolean showDialogs, boolean printCurrentRecordOnly, boolean showPrinterSelectDialog, PrinterJob printerJob)//called from javascript and IForm
	{
		getFormUI().print(showDialogs, printCurrentRecordOnly, showPrinterSelectDialog, printerJob);
	}

	public String printXML(boolean printCurrentRecordOnly)
	{
		return getFormUI().printXML(printCurrentRecordOnly);
	}

	private String preferredPrinterName = null;

	public String getPreferredPrinterName()
	{
		return preferredPrinterName;
	}

	void setPreferredPrinterName(String name)
	{
		preferredPrinterName = name;
	}

	public PageFormat getPageFormat()
	{
		return pageFormat;
	}

	public void setPageFormat(PageFormat pageFormat)
	{
		this.pageFormat = pageFormat;
	}

	/**
	 * @see com.servoy.j2db.dataprocessing.IGlobalEditListener#recordEditStart(com.servoy.j2db.dataprocessing.IRecordInternal)
	 */
	public boolean recordEditStart(IRecordInternal record)
	{
		//TODO this method can be called from outside a AWT event thread!!! 
		// only execute it for our own records.
		if (record.getParentFoundSet() == formModel)
		{
			return executeOnRecordEditStart();
		}
		return true;
	}

	public boolean prepareForSave(boolean looseFocus)//save any changed data to database
	{
		// only valid if the current form is the main one.
		// because stopUIEditing already cascades.
		if (application.getFormManager() != null && application.getFormManager().getCurrentForm() == this)
		{
			if (looseFocus)
			{
				getFormUI().prepareForSave(looseFocus);
			}

			FormController navigator = ((FormManager)application.getFormManager()).getCurrentContainer().getNavigator();
			if (navigator != null)
			{
				navigator.stopUIEditing(looseFocus);
			}
			return stopUIEditing(looseFocus);
		}
		return true;
	}

	public boolean stopUIEditing(boolean looseFocus)
	{
		if (view != null && !view.stopUIEditing(looseFocus)) return false; //controller stops editor

		IDataRenderer[] array = getDataRenderers();
		for (int i = 0; i < array.length; i++)
		{
			IDataRenderer dataRenderer = array[i];
			// skip form editor because that is already done by the view.stopUIEditing()
			if (dataRenderer != null && (FormController.FORM_EDITOR != i || view == null))
			{
				if (!dataRenderer.stopUIEditing(looseFocus)) return false;
			}
		}
		if (looseFocus && form.getOnRecordEditStopMethodID() != 0)
		{
			//allow beans to store there data via method
			IRecordInternal[] records = getApplication().getFoundSetManager().getEditRecordList().getUnmarkedEditedRecords(formModel);
			for (IRecordInternal element : records)
			{
				boolean b = executeOnRecordEditStop(element);
				if (!b) return false;
			}
		}
		return true;
	}

	public String copyRecord()
	{
		if (formModel != null && formModel.getTable() == null)
		{
			return null;
		}
		checkInitialized();

		String str = formModel.getAsTabSeparated(getRecordIndex());
		if (application instanceof ISmartClientApplication)
		{
			((ISmartClientApplication)application).setClipboardContent(str);
		}
		return str;
	}

	public String copyAllRecords()
	{
		if (formModel != null && formModel.getTable() == null)
		{
			return null;
		}
		checkInitialized();

		String str = formModel.getAsTabSeparated(-1);
		if (application instanceof ISmartClientApplication)
		{
			((ISmartClientApplication)application).setClipboardContent(str);
		}
		return str;
	}

	/*
	 * _____________________________________________________________ The methods below override methods from superclass <classname>
	 */
	public boolean isFormVisible()
	{
		return isFormVisible;
	}

	public boolean isFormExecutingFunction()
	{
		return currentFormExecutingFunctionCount.get() > 0;
	}

	private boolean isFormVisible = false;
	private int lastSelectedIndex = -1;
	private final AtomicInteger currentFormExecutingFunctionCount = new AtomicInteger();

	//this method first overloaded setVisible but setVisible is not always called and had differences between jdks
	public boolean notifyVisible(boolean visible, List<Runnable> invokeLaterRunnables)
	{
		if (isFormVisible == visible || executingOnLoad) return true;
		if (formModel == null)
		{
			isFormVisible = visible;
			return true;
		}

		if (visible)
		{
			isFormVisible = true;
			application.getFoundSetManager().getEditRecordList().addPrepareForSave(this);
			int index = formModel.getSelectedIndex();
			((ISwingFoundSet)formModel).getSelectionModel().addListSelectionListener(this);
			((ISwingFoundSet)formModel).getSelectionModel().addFormController(this);
			((ISwingFoundSet)formModel).addTableModelListener(this);
			if (view != null)
			{
				view.setModel(formModel);
			}

			if (index != -1 && formModel.getSelectedIndex() != index)
			{
				formModel.setSelectedIndex(index);
			}
			else if (view != null && formModel.getSelectedIndex() != -1 && (lastSelectedIndex != formModel.getSelectedIndex()))
			{
				view.ensureIndexIsVisible(formModel.getSelectedIndex());
			}
			lastSelectedIndex = formModel.getSelectedIndex();

			showNavigator(invokeLaterRunnables);
			valueChanged(null);

			Runnable r = new Runnable()
			{
				public void run()
				{
					if (isFormVisible()) //safety
					{
						try
						{
							if (!isShowingData())
							{
								if (wantEmptyFoundSet())
								{
									if (formModel != null) formModel.clear();
								}
								else
								{
									loadAllRecordsImpl(false);
								}
							}
						}
						catch (ServoyException e)
						{
							application.handleException(application.getI18NMessage("servoy.formPanel.error.showFormData"), e); //$NON-NLS-1$
						}
						executeOnShowMethod();
					}
				}
			};
			invokeLaterRunnables.add(r);
		}
		else
		{
			// if form is in design when set to invisible, end design mode.
			if (this.designMode != null)
			{
				setDesignMode(null);
			}
			int stopped = application.getFoundSetManager().getEditRecordList().stopIfEditing(formModel);
			isFormVisible = false;
			boolean allowHide = stopped == ISaveConstants.STOPPED || stopped == ISaveConstants.AUTO_SAVE_BLOCKED;
			if (allowHide)
			{
				allowHide = executeOnHideMethod();
			}
			if (!allowHide)
			{
				isFormVisible = true;
				return false;
			}
			application.getFoundSetManager().getEditRecordList().removePrepareForSave(this);

			// if form is destroyed in onHide or editRecordStopped..
			if (formModel != null)
			{
				// -2 is a form model change before this. dont get the selected index from the new model
				if (lastSelectedIndex != -2) lastSelectedIndex = formModel.getSelectedIndex();
				formModel.flushAllCachedItems(); //make sure we are not running out of memory
			}
		}

		IDataRenderer[] array = getDataRenderers();
		for (IDataRenderer dataRenderer : array)
		{
			if (dataRenderer != null)
			{
				dataRenderer.notifyVisible(visible, invokeLaterRunnables);
			}
		}

		if (!visible)
		{
			// if form is destroyed in onHide or editRecordStopped..
			if (formModel != null)
			{
				((ISwingFoundSet)formModel).getSelectionModel().removeListSelectionListener(this);
				((ISwingFoundSet)formModel).getSelectionModel().removeFormController(this);
				((ISwingFoundSet)formModel).removeTableModelListener(this);
			}
			if (view != null)
			{
				view.setModel(null);
			}
		}

		// visibility changed; update selectionMode if necessary
		pinSelectionModeIfNecessary();
		return true;
	}

	public void notifyResized()
	{
		if (form.getOnResizeMethodID() > 0)
		{
			executeOnResize();
		}
	}

	/*
	 * _____________________________________________________________ The methods below belong to this class
	 */

	//set the mode (browse,find,preview)
	public boolean setMode(int mode)
	{
		//		this is a semi saveData , we do NOT want the record go out of edit(!) and is updated in db
		if (application.getFoundSetManager().getEditRecordList().prepareForSave(true) != ISaveConstants.STOPPED)
		{
			if (Debug.tracing())
			{
				Debug.trace("Function not executed because stopUIEditing returned false, probably because a validation failure"); //$NON-NLS-1$
			}
			return false;
		}
		if (application.getFoundSetManager().getEditRecordList().stopIfEditing(formModel) != ISaveConstants.STOPPED)
		{
			//we cannot allow finds when there are editing records...it possible to start (related!)find on table which would possible not include editing records
			if (Debug.tracing())
			{
				Debug.trace("Tried to start a find in form '" + getName() + "', but there where records in edit mode and auto save is false"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			return false;
		}

		switch (mode)
		{
			case IModeManager.FIND_MODE :
				formModel.setFindMode();
				break;

			default ://all other modes
				if (formModel != null && formModel.isInFindMode())
				{
					try
					{
						performFindImpl(false, true, false);
					}
					catch (Exception e)
					{
						application.handleException(application.getI18NMessage("servoy.formPanel.error.searchFailed"), e); //$NON-NLS-1$
					}
				}
				break;
		}
		return true;
	}

	public void propagateFindMode(boolean findMode)
	{
		if (!findMode)
		{
			application.getFoundSetManager().getEditRecordList().prepareForSave(true);
		}
		if (isReadOnly())
		{
			if (view != null)
			{
				view.setEditable(findMode);
			}
		}

		for (IDataRenderer dataRenderer : dataRenderers)
		{
			if (dataRenderer != null)
			{
				//				if (i == FORM_RENDERER || i == FORM_EDITOR)
				//				{
				// if the data renderer itself is a IDisplay then go through that one
				// see web cell based view that is used as a tableview and portal.
				if (dataRenderer instanceof IDisplay)
				{
					((IDisplay)dataRenderer).setValidationEnabled(!findMode);
				}
				else
				{
					DataAdapterList dal = dataRenderer.getDataAdapterList();
					dal.setFindMode(findMode);//disables related data en does getText instead if getValue on fields
					dataRenderer.setAllNonFieldsEnabled(!findMode);
				}
				//				}
				//				else
				//				{
				//					//dataRenderers[i].setUIEnabled(!findMode);
				//				}
			}
		}
	}

	public int performFind(final boolean clear, final boolean reduce, final boolean showDialogOnNoResults)
	{
		if (form.getOnSearchCmdMethodID() <= 0) //'-none-' has no meaning for onSearchCMD
		{
			try
			{
				return performFindImpl(clear, reduce, showDialogOnNoResults);
			}
			catch (Exception e)
			{
				application.handleException(application.getI18NMessage("servoy.formPanel.error.searchFailed"), e); //$NON-NLS-1$
			}
		}
		else if (isInFindMode())
		{
			Object o = executeFormMethod(StaticContentSpecLoader.PROPERTY_ONSEARCHCMDMETHODID,
				new Object[] { Boolean.valueOf(clear), Boolean.valueOf(reduce) }, Boolean.FALSE, true, true);
			if (o instanceof Number)
			{
				return ((Number)o).intValue();
			}
		}
		else if (Debug.tracing())
		{
			Debug.trace("Search called for form '" + getName() + "' but the form wasn't in find mode"); //$NON-NLS-1$ //$NON-NLS-2$
			// TODO should we report this as a real error to the developer??
		}
		return (formModel != null ? formModel.getSize() : 0);
	}

	public boolean isInFindMode()
	{
		if (formModel != null)
		{
			return formModel.isInFindMode();
		}
		else
		{
			return false;
		}
	}

	public int performFindImpl(final boolean clearLastResult, final boolean reduceSearch, final boolean showDialogOnNoResults) throws ServoyException
	{
		int count = 0;
		if (formModel.isInFindMode())
		{
			if (Debug.tracing())
			{
				Debug.trace("Search called for form '" + getName() + "'"); //$NON-NLS-1$//$NON-NLS-2$
			}

			// Find mode only has to stop the ui
			application.getFoundSetManager().getEditRecordList().prepareForSave(true);
			try
			{
				List<String> invalidRangeConditions = null;
				count = formModel.performFind(clearLastResult, reduceSearch, !showDialogOnNoResults, false, showDialogOnNoResults
					? invalidRangeConditions = new ArrayList<String>() : null);
				if (application.getCmdManager() instanceof ICmdManagerInternal)
				{
					((ICmdManagerInternal)application.getCmdManager()).ableFormRelatedFindActions(false);
				}
				if (count == 0 && showDialogOnNoResults)
				{
					StringBuilder invalidRangeFieldTxt = null;
					if (invalidRangeConditions.size() > 0)
					{
						invalidRangeFieldTxt = new StringBuilder();
						invalidRangeFieldTxt.append(application.getI18NMessage("servoy.formPanel.search.invalidRange")); //$NON-NLS-1$
						for (String invalidRangeField : invalidRangeConditions)
						{
							invalidRangeFieldTxt.append(invalidRangeField);
							invalidRangeFieldTxt.append(", "); //$NON-NLS-1$
						}

						invalidRangeFieldTxt.replace(invalidRangeFieldTxt.length() - 2, invalidRangeFieldTxt.length(), "\n"); //$NON-NLS-1$
					}

					String dlgMessage = application.getI18NMessage("servoy.formPanel.search.noResults"); //$NON-NLS-1$
					if (invalidRangeFieldTxt != null)
					{
						dlgMessage = invalidRangeFieldTxt.toString() + dlgMessage;
					}
					boolean userChoseYes = getFormUI().showYesNoQuestionDialog(application, dlgMessage, "Search"); //$NON-NLS-1$ 
					if (!userChoseYes)
					{
						formModel.browseAll(true);//this removes the findmode

						exitFindMode();
					}
					else
					{
						// stay in find
						focusFirstField();
					}
				}
				else if (count >= 0) // not blocked
				{
					//model is already out of find
					exitFindMode();
				}
			}
			catch (ServoyException ex)
			{
				formModel.browseAll(true); // make sure we get nicely out of find mode; something went wrong with the performFind
				exitFindMode();
				throw ex;
			}
		}
		else
		{
			if (Debug.tracing())
			{
				Debug.trace("Search called for form '" + getName() + "' but the form wasn't in find mode"); //$NON-NLS-1$ //$NON-NLS-2$
				// TODO should we report this as a real error to the developer??
			}
		}
		return count; // -1 when blocked
	}

	public int getView()
	{
		return currentViewType;
	}

	public IView getViewComponent()
	{
		return view;
	}

	//set the view (record,list,table)
	public void setView(int v)
	{
		int viewType = v;
		if (viewType == -1)
		{
			viewType = getForm().getView();
		}

		if (viewType == currentViewType)
		{
			return;//no change
		}

		if (currentViewType == LOCKED_TABLE_VIEW || currentViewType == LOCKED_LIST_VIEW || currentViewType == LOCKED_RECORD_VIEW)
		{
			return; // don't let user change
		}

		currentViewType = viewType;

		// always synch the view menu with this call
		if (application.getFormManager() instanceof IFormManagerInternal)
		{
			IFormManagerInternal sfm = application.getFormManager();
			if (sfm.getCurrentMainShowingFormController() == this)
			{
				sfm.synchViewMenu(viewType);
			}
		}

		boolean formReadOnly = ((FormManager)application.getFormManager()).isFormReadOnly(getName());

		//uninstall old view
		if (view != null)
		{
			if (formReadOnly)
			{
				containerImpl.setReadOnly(false);
			}
			view.stop();
			view = null;

			deleteRenderers();
			try
			{
				createDataRenderers(viewType);
			}
			catch (Exception e)
			{
				application.reportError(application.getI18NMessage("servoy.formPanel.error.setupForm") + ": " + getName(), e); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}

		if (bgColor != null)
		{
			containerImpl.setBackground(bgColor);
		}

		view = containerImpl.initView(application, this, viewType);


		// Do tab sequencing now.
		tabSequence.fromAbstractToNamed();

		view.start(application);
		// make sure that the formscope is created to insert the elements in it.
		getFormScope();
		if (formScope != null)
		{
			hmChildrenJavaMembers = new HashMap<String, Object[]>();
			Scriptable scope = getFormUI().makeElementsScriptObject(formScope, hmChildrenJavaMembers, dataRenderers, view);
			formScope.putWithoutFireChange("elements", scope); //$NON-NLS-1$
		}
		if (isFormVisible)
		{
			view.setModel(formModel);
		}
		view.setRowBGColorScript(form.getRowBGColorCalculation(), form.getInstanceMethodArguments("rowBGColorCalculation")); //$NON-NLS-1$
		if (view instanceof ISupportRowStyling) ((ISupportRowStyling)view).setRowStyles(stylesheet, styleOdd, styleEven, styleSelected, styleHeader);

		if (formReadOnly)
		{
			containerImpl.setReadOnly(true);
		}

	}

	public void showNavigator(List<Runnable> invokeLaterRunnables)
	{
		if (fm != null && fm.getCurrentMainShowingFormController() == this)//safety for tabpanels (those cannot have a custom controller)
		{
			ISupportNavigator navigatorSupport = fm.getCurrentContainer();
			if (navigatorSupport != null)
			{
				int form_id = form.getNavigatorID();
				if (form_id > 0)
				{
					FormController currentFC = navigatorSupport.getNavigator();
					if (currentFC == null || currentFC.getForm().getID() != form_id)//is already there
					{
						try
						{
							Form sliderDef = application.getFlattenedSolution().getForm(form_id);
							if (sliderDef != null)
							{
								if (sliderDef.getID() != form_id && sliderDef.getNavigatorID() >= 0) return;//safety a slider Form CANNOT HAVE A SLIDER

								FormController nav = fm.getFormController(sliderDef.getName(), navigatorSupport);
								ControllerUndoManager cum = null;
								if (nav != null)
								{
									nav.notifyVisible(true, invokeLaterRunnables);
									cum = nav.getUndoManager();
								}
								if (application.getCmdManager() instanceof ICmdManagerInternal)
								{
									((ICmdManagerInternal)application.getCmdManager()).setControllerUndoManager(cum);
								}
								FormController old_nav = navigatorSupport.setNavigator(nav);
								if (old_nav != null) old_nav.notifyVisible(false, invokeLaterRunnables);
							}
							else
							{
								// Form deleted??
								FormController old_nav = navigatorSupport.setNavigator(null);
								if (old_nav != null) old_nav.notifyVisible(false, invokeLaterRunnables);
							}
						}
						catch (Exception ex)
						{
							Debug.error(ex);
						}
					}
					else
					{
						// Try to lease it extra so it will be added to last used screens.
						Form sliderDef = application.getFlattenedSolution().getForm(form_id);
						if (sliderDef != null)
						{
							fm.leaseFormPanel(sliderDef.getName());
						}
					}
				}
				else if (form_id != Form.NAVIGATOR_IGNORE)//if is ignore leave previous,if not remove
				{
					FormController old_nav = navigatorSupport.setNavigator(null);
					if (old_nav != null) old_nav.notifyVisible(false, invokeLaterRunnables);
				}
			}
		}
	}

	protected void createModel()
	{
		try
		{
			((FoundSetManager)application.getFoundSetManager()).addFoundSetListener(this); //formModel = new RootSet(application);
		}
		catch (Exception ex)
		{
			application.reportError(application.getI18NMessage("servoy.formPanel.error.formData"), ex); //$NON-NLS-1$
		}
	}

	public boolean wantSharedFoundSet()
	{
		return !form.getUseSeparateFoundSet();
	}

	public String getGlobalRelationNamedFoundset()
	{
		return form.getGlobalRelationNamedFoundset();
	}

	public boolean wantEmptyFoundSet()
	{
		return form.getUseEmptyFoundSet();
	}

	public ITable getTable()
	{
		try
		{
			return form.getTable();
		}
		catch (RepositoryException ex)
		{
			Debug.error(ex);
			return null;
		}
	}

	public String getDataSource()
	{
		return form.getDataSource();
	}

	public void newValue(FoundSetEvent e)
	{
		try
		{
			setModel((FoundSet)e.getSource());
		}
		catch (Exception ex)
		{
			application.reportError("invalid foundset set in form " + getName(), ex);
		}
	}

	private boolean adjustingModel = false;

	private boolean setModel(FoundSet newModel) throws ServoyException
	{
		if (newModel == formModel || adjustingModel)
		{
			return true;//same or adjusting do nothing
		}

		ITable formTable = application.getFoundSetManager().getTable(form.getDataSource());
		if (newModel != null && ((formTable == null && newModel.getTable() != null) || (formTable != null && !formTable.equals(newModel.getTable()))))
		{
			throw new IllegalArgumentException(application.getI18NMessage(
				"servoy.formPanel.error.wrongFoundsetTable", new Object[] { newModel.getTable() == null //$NON-NLS-1$
					? "NONE" : newModel.getTable().getName(), form.getTableName() })); //$NON-NLS-1$
		}

		try
		{
			if (view != null && view.isEditing())
			{
				// TODO if save fails don't set the newModel??
				int stopped = application.getFoundSetManager().getEditRecordList().stopEditing(false);
				if (stopped != ISaveConstants.STOPPED && stopped != ISaveConstants.AUTO_SAVE_BLOCKED)
				{
					return false;
				}
			}
			adjustingModel = true;

			if (formModel != null)
			{
				try
				{
					((ISwingFoundSet)formModel).getSelectionModel().removeListSelectionListener(this);
					((ISwingFoundSet)formModel).getSelectionModel().removeFormController(this);
					((ISwingFoundSet)formModel).removeTableModelListener(this);
					formModel.flushAllCachedItems();//to make sure all data is gc'ed
				}
				catch (Exception ex)
				{
					Debug.error(ex);
				}
			}

			setFormModelInternal(newModel == null ? ((FoundSetManager)application.getFoundSetManager()).getEmptyFoundSet(this) : newModel);

			if (formScope != null)
			{
				formScope.putWithoutFireChange("foundset", formModel); //$NON-NLS-1$
				if (formScope.getPrototype() == null)
				{
					formScope.setPrototype(new SelectedRecordScope(this, formTable == null ? null : application.getScriptEngine().getTableScope(formTable)));
				}
			}
			if (isFormVisible)
			{
				((ISwingFoundSet)formModel).getSelectionModel().addListSelectionListener(this);
				((ISwingFoundSet)formModel).getSelectionModel().addFormController(this);
				((ISwingFoundSet)formModel).addTableModelListener(this);
				if (view != null) //it may not yet exist
				{
					view.setModel(formModel);
				}

				//this was former a call to aggregateChange, but now does now unwanted parent traverse...
				int[] idx = null;
				if (getView() == RECORD_VIEW || getView() == LOCKED_RECORD_VIEW)
				{
					int selIdx = formModel.getSelectedIndex();
					if (selIdx != -1) idx = new int[] { selIdx };
				}
				else
				{
					idx = formModel.getSelectedIndexes();
				}
				if (idx == null || idx.length == 0)
				{
					refreshAllPartRenderers(new IRecordInternal[] { formModel.getPrototypeState() });
				}
				else
				{
					IRecordInternal[] row = new IRecordInternal[idx.length];

					for (int i = 0; i < idx.length; i++)
						row[i] = formModel.getRecord(idx[i]);
					refreshAllPartRenderers(row);
				}
			}
		}
		finally
		{
			adjustingModel = false;
		}
		return true;
	}

	private ScriptExecuter scriptExecuter;

	private static class ScriptExecuter implements IScriptExecuter//this class is made only when swing has a mem leak we don't have one
	{
		private FormController delegate;

		ScriptExecuter(FormController fp)
		{
			delegate = fp;
		}

		public Object executeFunction(String cmd, Object[] args, boolean saveData, Object src, boolean focusEvent, String methodKey,
			boolean executeWhenFieldValidationFailed)
		{
			if (delegate != null) return delegate.executeFunction(cmd, args, saveData, src, focusEvent, methodKey, true, executeWhenFieldValidationFailed);
			return null;
		}

		public void setLastKeyModifiers(int modifiers)
		{
			if (delegate != null)
			{
				delegate.setLastKeyModifiers(modifiers);
			}
		}

		public FormController getFormController()
		{
			return delegate;
		}

		public void destroy()
		{
			delegate = null;
		}
	}

	/*
	 * _____________________________________________________________ Java script methods/implementation
	 */

	public void setLastKeyModifiers(int modifiers)
	{
		((IScriptSupport)application.getScriptEngine()).setLastKeyModifiers(modifiers);
	}

	public boolean isReadOnly()
	{
		return ((FormManager)application.getFormManager()).isFormReadOnly(getName());
	}

	public void setReadOnly(boolean b)
	{
		if (b) stopUIEditing(true);
		containerImpl.setReadOnly(b);
		((FormManager)application.getFormManager()).setFormReadOnly(getName(), b);
		if (b && containerImpl.getUndoManager() != null) containerImpl.getUndoManager().discardAllEdits();
	}

	public boolean isEnabled()
	{
		return containerImpl.isEnabled();
	}

	public void setComponentEnabled(boolean b)
	{
		containerImpl.setComponentEnabled(b);
	}

	public int getFormWidth()
	{
		return containerImpl.getFormWidth();
	}

	public int getPartHeight(int partType)
	{
		return containerImpl.getPartHeight(partType);
	}

	public int getPartYOffset(int partType)
	{
		IDataRenderer[] renderers = getDataRenderers();
		if (renderers != null && partType < renderers.length && renderers[partType] != null)
		{
			return renderers[partType].getYOffset();
		}
		return 0;
	}

	public Object getDesignTimeProperty(String key)
	{
		return Utils.parseJSExpression(getForm().getCustomDesignTimeProperty(key));
	}

	/**
	 * Initialize this FormController(or related classes/methods) to be used in javascript
	 */
	public synchronized JSForm initForJSUsage(CreationalPrototype creationalPrototype)
	{
		if (formScope == null)
		{
			try
			{
				//make scope for state delegation via prototype mechanism
				List<Form> forms = application.getFlattenedSolution().getFormHierarchy(form);
				formScope = new FormScope(this, forms.toArray(new ISupportScriptProviders[forms.size()]));
				// Set the solution scope first as parent. Will be overridden by foundset

				if (formModel != null)
				{
					ITable formTable = application.getFoundSetManager().getTable(form.getDataSource());
					formScope.setPrototype(new SelectedRecordScope(this, formTable == null ? null : application.getScriptEngine().getTableScope(formTable)));
					formScope.putWithoutFireChange("foundset", formModel); //$NON-NLS-1$
				}

				//create JS place holder for this object
				scriptableForm = new FormController.JSForm(this);

				//set parent scope
				NativeJavaObject formObject = new NativeJavaObject(formScope, scriptableForm, new InstanceJavaMembers(formScope, JSForm.class));
				formScope.putWithoutFireChange("controller", formObject); //$NON-NLS-1$

				//register the place holder 'scriptableForm' in CreationalPrototype scope
				creationalPrototype.setLocked(false);
				creationalPrototype.put(((Integer)creationalPrototype.get("length", creationalPrototype)).intValue(), creationalPrototype, formScope); //$NON-NLS-1$
				creationalPrototype.put(getName(), creationalPrototype, formScope);
				creationalPrototype.setLocked(true);

				formScope.createVars();
			}
			catch (Exception ex)
			{
				application.reportError(application.getI18NMessage("servoy.formPanel.error.initFormScope") + ": " + getName(), ex); //$NON-NLS-1$
				return null;
			}

		}
		return scriptableForm;
	}

	public Object eval(String eval_string)
	{
		if (formModel != null && formModel.isInFindMode()) return null;

		return getApplication().getScriptEngine().eval(formScope, eval_string);
	}

	public Object executeFunction(Function f, Object[] args, Scriptable scope, boolean saveData) throws Exception
	{
		return executeFunction(f, args, scope, scope, saveData, null, true, false, null, false, false, false);
	}

	public Object executeFunction(String cmd, Object[] args, boolean saveData, Object src, boolean focusEvent, String methodKey)
	{
		return executeFunction(cmd, args, saveData, src, focusEvent, methodKey, true, false);
	}

	public Object executeFunction(String cmd, Object[] args, boolean saveData, Object src, boolean focusEvent, String methodKey, boolean allowFoundsetMethods,
		boolean executeWhenFieldValidationFailed)
	{
		try
		{
			return executeFunction(cmd, args, saveData, src, focusEvent, methodKey, allowFoundsetMethods, executeWhenFieldValidationFailed, false);
		}
		catch (ApplicationException ex)
		{
			application.reportError(ex.getMessage(), null);
		}
		catch (Exception ex)
		{
			this.requestFocus();
			String name = cmd;
			int id = Utils.getAsInteger(cmd);
			if (id > 0)
			{
				name = formScope.getFunctionName(new Integer(id));
			}

			if (id <= 0 && ScopesUtils.isVariableScope(name))
			{
				application.reportError(application.getI18NMessage("servoy.formPanel.error.executingMethod", new Object[] { name }), ex); //$NON-NLS-1$ 
			}
			else
			{
				application.reportError(application.getI18NMessage("servoy.formPanel.error.executingMethod", new Object[] { getName() + "." + name }), ex); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		return null;
	}

	/**
	 * call a scriptMethod (== function)
	 * 
	 * @param cmd be the the id from the method or the name
	 * @param methodKey 
	 */
	public Object executeFunction(String cmd, Object[] args, boolean saveData, Object src, boolean focusEvent, String methodKey, boolean allowFoundsetMethods,
		boolean executeWhenFieldValidationFailed, boolean throwException) throws Exception
	{
		Object function = null;
		Scriptable scope = formScope;

		String name = cmd;
		int id = Utils.getAsInteger(cmd);
		if (id > 0)
		{
			name = formScope.getFunctionName(new Integer(id));
		}

		Pair<String, String> nameScope = ScopesUtils.getVariableScope(name);
		boolean global = nameScope != null && nameScope.getLeft() != null;
		if (id <= 0 && global)
		{
			name = nameScope.getRight();
		}
		else
		{
			function = formScope.getFunctionByName(name);
		}

		if (allowFoundsetMethods && !global && function == null && formModel != null)
		{
			// try foundset method
			ScriptMethod scriptMethod;
			if (id > 0)
			{
				scriptMethod = AbstractBase.selectById(application.getFlattenedSolution().getFoundsetMethods(getTable(), false).iterator(), id);
				if (scriptMethod != null)
				{
					name = scriptMethod.getName();
				}
			}
			if (name != null)
			{
				scope = formModel;
				function = scope.getPrototype().get(name, scope);
			}
		}

		if (function == null || function == Scriptable.NOT_FOUND)
		{
			GlobalScope globalScope = null;
			if (id > 0)
			{
				globalScope = application.getScriptEngine().getScopesScope().getGlobalScopeForFunction(new Integer(id));
			}
			else if (nameScope != null)
			{
				globalScope = application.getScriptEngine().getScopesScope().getGlobalScope(nameScope.getLeft());
			}
			if (globalScope != null)
			{
				scope = globalScope;
				if (id > 0)
				{
					name = globalScope.getFunctionName(new Integer(id));
				}
				function = globalScope.getFunctionByName(name);
			}
		}

		Function f;
		if (function instanceof Function /* else null or UniqueTag.NOT_FOUND */)
		{
			f = (Function)function;
		}
		else
		{
			if (cmd != null)
			{
				if (throwException)
				{
					throw new IllegalArgumentException("Could not find function '" + cmd + "' for form " + getName());
				}
				return null;
			}
			// sometimes executeFunction is called with cmd=null just to trigger field validation, see BaseEventExecutor.fireEventCommand()
			f = null;
		}

		if (throwException)
		{
			return executeFunction(f, args, scope, scope, saveData, src, f == null || !Utils.getAsBoolean(f.get("_AllowToRunInFind_", f)), //$NON-NLS-1$
				focusEvent, methodKey, executeWhenFieldValidationFailed, false, true);
		}
		try
		{
			return executeFunction(f, args, scope, scope, saveData, src, f == null || !Utils.getAsBoolean(f.get("_AllowToRunInFind_", f)), //$NON-NLS-1$
				focusEvent, methodKey, executeWhenFieldValidationFailed, false, false);
		}
		catch (ApplicationException ex)
		{
			application.reportError(ex.getMessage(), null);
		}
		catch (Exception ex)
		{
			this.requestFocus();
			application.reportError(application.getI18NMessage("servoy.formPanel.error.executingMethod", new Object[] { getName() + "." + name }), ex); //$NON-NLS-1$ //$NON-NLS-2$				
		}
		return null;
	}

	@SuppressWarnings("nls")
	private Object executeFunction(Function f, Object[] args, Scriptable scope, Scriptable thisObject, boolean saveData, Object src, boolean testFindMode,
		boolean focusEvent, String methodKey, boolean executeWhenFieldValidationFailed, boolean useFormAsEventSourceEventually, boolean throwException)
		throws Exception
	{
		if (!(testFindMode && isInFindMode())) //only run certain methods in find
		{
			// this is a semi saveData , we do NOT want the record go out of edit(!) and is updated in db
			if (saveData)
			{
				application.getFoundSetManager().getEditRecordList().prepareForSave(false);
			}
			if (f != null)
			{
				if (!executeWhenFieldValidationFailed &&
					Boolean.TRUE.equals(application.getRuntimeProperties().get(IServiceProvider.RT_LASTFIELDVALIDATIONFAILED_FLAG)))
				{
					if (Debug.tracing())
					{
						Debug.trace("Function not executed because a field is marked invalid");
					}
					return null;
				}

				FormAndComponent formAndComponent = getJSApplicationNames(src, f, useFormAsEventSourceEventually);
				try
				{
					currentFormExecutingFunctionCount.incrementAndGet();
					Object[] newArgs = args;
					if (formAndComponent != null)
					{
						// for use of deprecated aplication.getMethodTriggerElementName() and aplication.getMethodTriggerFormName()
						IExecutingEnviroment scriptEngine = application.getScriptEngine();
						if (scriptEngine instanceof ScriptEngine)
						{
							((ScriptEngine)scriptEngine).getJSApplication().pushLastNames(formAndComponent);
						}

						if (methodKey != null)
						{
							// add form event if needed
							MethodTemplate methodTemplate = MethodTemplate.getTemplate(null, methodKey);
							if (methodTemplate != null)
							{
								MethodArgument[] methodArguments = methodTemplate.getArguments();
								for (int i = 0; methodArguments != null && i < methodArguments.length; i++)
								{
									if (methodArguments[i].getType() == ArgumentType.JSEvent)
									{
										// method template declares an event argument
										if (args == null || args.length <= i || args[i] == null)
										{
											// no event argument there yet, insert a form event
											JSEvent event = getJSEvent(formAndComponent.src);
											if (args == null || args.length <= i)
											{
												newArgs = new Object[i + 1];
												if (args != null)
												{
													System.arraycopy(args, 0, newArgs, 0, args.length);
												}
											}
											newArgs[i] = event;
										}
										break;
									}
								}
							}
						}
					}
					if (newArgs != null && newArgs.length > 0)
					{
						for (Object newArg : newArgs)
						{
							if (newArg instanceof JSEvent)
							{
								JSEvent event = (JSEvent)newArg;
								if (formScope != null && event.js_getSource() instanceof IComponent && ((IComponent)event.js_getSource()).getName() != null)
								{
									Object elementScope = formScope.get("elements");
									if (elementScope instanceof Scriptable)
									{
										Object elementSrc = ((Scriptable)elementScope).get(((IComponent)event.js_getSource()).getName(),
											(Scriptable)elementScope);
										if (elementSrc != null)
										{
											if (elementSrc instanceof ITwoNativeJavaObject)
											{
												Object scriptable = event.js_getSource();
												if (scriptable instanceof IScriptableProvider)
												{
													scriptable = ((IScriptableProvider)scriptable).getScriptObject();
												}
												((ITwoNativeJavaObject)elementSrc).setRealObject(scriptable);
											}
											event.setSource(elementSrc);
										}
									}
								}
								break;
							}
						}
					}

					return application.getScriptEngine().executeFunction(f, scope, thisObject, newArgs, focusEvent, throwException);
				}
				finally
				{
					currentFormExecutingFunctionCount.decrementAndGet();
					if (formAndComponent != null)
					{
						IExecutingEnviroment scriptEngine = application.getScriptEngine();
						if (scriptEngine instanceof ScriptEngine)
						{
							((ScriptEngine)scriptEngine).getJSApplication().popLastStackNames(formAndComponent);
						}
					}
					// after a script clear the unchanged records so that no records keep hanging around.
					if (!focusEvent && !"onRecordEditStopMethodID".equals(methodKey) && !"onRenderMethodID".equals(methodKey))
					{
						application.getFoundSetManager().getEditRecordList().removeUnChangedRecords(false, false);
					}
				}
			}
		}
		return null;
	}

	private JSEvent getJSEvent(Object src)
	{
		JSEvent event = new JSEvent();
		event.setType(JSEvent.EventType.form);
		event.setFormName(getName());
		event.setSource(src);
		event.setElementName(src instanceof IComponent ? ((IComponent)src).getName() : null);
		return event;
	}

	/**
	 * @param src
	 * @param function
	 * @param useFormAsEventSourceEventually 
	 */
	private FormAndComponent getJSApplicationNames(Object source, Function function, boolean useFormAsEventSourceEventually)
	{
		Object src = source;
		if (src == null)
		{
			Object window = application.getRuntimeWindowManager().getCurrentWindowWrappedObject();
//			if (!(window instanceof Window) || !((Window)window).isVisible())
//			{
//				window = application.getMainApplicationFrame();
//			}

			if (window != null)
			{
				src = ((Window)window).getFocusOwner();
				while (src != null && !(src instanceof IComponent))
				{
					src = ((Component)src).getParent();
				}
				// Test if this component really comes from the the controllers UI.
				if (src instanceof Component)
				{
					Container container = ((Component)src).getParent();
					while (container != null && !(container instanceof IFormUIInternal< ? >))
					{
						container = container.getParent();
					}
					if (container != getFormUI())
					{
						// if not then this is not the trigger element for this form. 
						src = null;
					}
				}
			}
		}

		Scriptable thisObject = null;
		if (src instanceof IComponent)
		{
			Object esObj = formScope.get("elements", formScope); //$NON-NLS-1$
			if (esObj != Scriptable.NOT_FOUND)
			{
				ElementScope es = (ElementScope)esObj;
				String name = ((IComponent)src).getName();
				if (name != null && name.length() != 0)
				{
					Object o = es.get(name, es);
					if (o instanceof Scriptable)
					{
						thisObject = (Scriptable)o;
					}
				}

				if (thisObject == null)
				{
					if (name == null || name.length() == 0)
					{
						name = ComponentFactory.WEB_ID_PREFIX + System.currentTimeMillis();
						// Check Web components always have a name! Because name can't be set 
						((IComponent)src).setName(name);
					}
					Context.enter();
					InstanceJavaMembers ijm = new InstanceJavaMembers(formScope, src.getClass());
					JavaMembers jm = ijm;
					if (ijm.getFieldIds(false).size() == 0 && ijm.getMethodIds(false).size() == 0)
					{
						jm = new JavaMembers(formScope, src.getClass())
						{
							@Override
							protected boolean shouldDeleteGetAndSetMethods()
							{
								return true;
							}
						};
					}
					thisObject = new NativeJavaObject(formScope, src, jm);

					es.setLocked(false);
					es.put(name, es, thisObject);
					es.setLocked(true);
					Context.exit();
				}
			}
		}
		if (src == null && useFormAsEventSourceEventually) src = formScope;
		return new FormAndComponent(src, getName());
	}

	//	private static int isExecuting = 0;
	//	private static LinkedList executeStack = new LinkedList();

	//if the js init is not done , do!	
	public synchronized JSForm initForJSUsage()
	{
		if (formScope == null)
		{
			if (destroyed)
			{
				Debug.error("Calling initForJSUsage on a destroyed form: " + this, new RuntimeException());
				return null;
			}
			Context.enter();
			try
			{
				IExecutingEnviroment se = application.getScriptEngine();
				if (se != null)
				{
					SolutionScope solScope = application.getScriptEngine().getSolutionScope();
					initForJSUsage((CreationalPrototype)solScope.get("forms", solScope)); //$NON-NLS-1$
				}
				else
				{
					// saw this happen on MAC debug smart client - edit the value in a combobox (change it) then directly click on the close window button;
					// shutDown executed first (windowClosing) - setting solution to null, then the combo item state change (generated by focus lost) event was triggered
					Debug.log("Trying to initForJSUsage with null script engine - probably a method was about to be invoked post-shutdown", null); //$NON-NLS-1$
				}
			}
			catch (Exception ex)
			{
				application.reportError(application.getI18NMessage("servoy.formPanel.error.initScriptEngine"), ex); //$NON-NLS-1$
			}
			finally
			{
				Context.exit();
			}
		}
		return scriptableForm;
	}

	private boolean didOnload;
	private boolean executingOnLoad;

	public void executeOnLoadMethod()
	{
		if (!didOnload)
		{
			didOnload = true;
			if (form.getOnLoadMethodID() > 0)
			{
				// Set this boolean on true while executing the onload so that 
				// an onload method won't trigger the notify visible before it is finished itself.
				executingOnLoad = true;
				try
				{
					Object[] args = new Object[] { getJSEvent(formScope) };
					executeFormMethod(StaticContentSpecLoader.PROPERTY_ONLOADMETHODID, args, Boolean.FALSE, true, false /* foundset is not yet initialized */);
				}
				finally
				{
					executingOnLoad = false;
				}
			}
		}
	}

	private boolean didOnShowOnce = false;

	private void executeOnShowMethod()
	{
		if (!executingOnLoad && form.getOnShowMethodID() > 0)
		{
			Object[] args = new Object[] { Boolean.valueOf(!didOnShowOnce), getJSEvent(formScope) };//isFirstTime
			didOnShowOnce = true;
			executeFormMethod(StaticContentSpecLoader.PROPERTY_ONSHOWMETHODID, args, null, true, true);
		}
	}

	/**
	 * onHide methods are capable to stop direct hide on there parent (only), for example -a form in a tab can stop a tab switch -a main form can stop a main
	 * form switch
	 */
	private boolean executeOnHideMethod()
	{
		return form.getOnHideMethodID() == 0 ||
			!Boolean.FALSE.equals(executeFormMethod(StaticContentSpecLoader.PROPERTY_ONHIDEMETHODID, new Object[] { getJSEvent(formScope) }, null, true, true));
	}

	private boolean runningExecuteOnRecordEditStop;

	boolean executeOnRecordEditStop(IRecordInternal record) //also called on leave
	{
		if (!runningExecuteOnRecordEditStop && isFormVisible)//should only work on visible form, onXXXX means user event which are only possible when visible
		{
			try
			{
				runningExecuteOnRecordEditStop = true;
				boolean ret = !Boolean.FALSE.equals(executeFormMethod(StaticContentSpecLoader.PROPERTY_ONRECORDEDITSTOPMETHODID, new Object[] { record },
					Boolean.TRUE, true, true));
				if (ret)
				{
					// for this record, record edit saved is called successfully shouldn't happen the second time.
					getApplication().getFoundSetManager().getEditRecordList().markRecordTested(record);
				}
				return ret;
			}
			finally
			{
				runningExecuteOnRecordEditStop = false;
			}
		}
		return true;
	}

	void executeOnRecordSelect()
	{
		if (form.getTitleText() != null && this == application.getFormManager().getCurrentForm())
		{
			// If a dialog is active over the main window, then don't update the application title.
			if (((FormManager)application.getFormManager()).isCurrentTheMainContainer())
			{
				String title = form.getTitleText();
				if (title == null || title.equals("")) title = getName(); //$NON-NLS-1$
				application.setTitle(title);
			}
		}
		if (containerImpl.getUndoManager() != null) containerImpl.getUndoManager().discardAllEdits();
		if (isFormVisible)//this is added because many onrecordSelect actions are display dependent (in that case you only want the visible forms to be set) or data action which are likely on the same table so obsolete any way.
		{
			executeFormMethod(StaticContentSpecLoader.PROPERTY_ONRECORDSELECTIONMETHODID, null, Boolean.TRUE, true, true);
		}
	}

	private boolean executeOnRecordEditStart()
	{
		if (!isFormVisible())//should only work on visible form, onXXXX means user event which are only possible when visible
		{
			return true;
		}
		// saveData is false because otherwise focus is lost on stopUIEditing in ListView
		// see issue 154845
		return !Boolean.FALSE.equals(executeFormMethod(StaticContentSpecLoader.PROPERTY_ONRECORDEDITSTARTMETHODID, null, Boolean.TRUE, false, true));
	}

	private void executeOnResize()
	{
		if (isFormVisible())
		{
			executeFormMethod(StaticContentSpecLoader.PROPERTY_ONRESIZEMETHODID, null, null, true, true);
		}
	}

	public Object executeFormMethod(TypedProperty<Integer> methodProperty, Object[] args, Boolean testFindMode, boolean saveData, boolean allowFoundsetMethods)
	{
		Object ret = null;
		Integer id = ((Integer)form.getProperty(methodProperty.getPropertyName()));
		if (id.intValue() > 0 && formScope != null)
		{
			String sName = null;
			try
			{
				Object function = null;
				Scriptable scope = formScope;

				// try form method
				sName = formScope.getFunctionName(id);
				if (sName != null)
				{
					function = formScope.getFunctionByName(sName);
				}

				if (!(function instanceof Function))
				{
					// try global method
					GlobalScope globalScope = application.getScriptEngine().getScopesScope().getGlobalScopeForFunction(id);
					if (globalScope != null)
					{
						scope = globalScope;
						sName = globalScope.getFunctionName(id);
						if (sName != null)
						{
							function = globalScope.getFunctionByName(sName);
						}
					}
				}

				if (allowFoundsetMethods && !(function instanceof Function) && formModel != null)
				{
					// try foundset method
					ScriptMethod scriptMethod = AbstractBase.selectById(application.getFlattenedSolution().getFoundsetMethods(getTable(), false).iterator(),
						id.intValue());
					if (scriptMethod != null)
					{
						scope = formModel;
						function = scope.getPrototype().get(scriptMethod.getName(), scope);
					}
				}

				if (function instanceof Function)
				{
					if (testFindMode == null)
					{
						testFindMode = Boolean.valueOf(!Utils.getAsBoolean(((Function)function).get("_AllowToRunInFind_", (Function)function))); //$NON-NLS-1$
					}
					ret = executeFunction((Function)function,
						Utils.arrayMerge(args, Utils.parseJSExpressions(form.getInstanceMethodArguments(methodProperty.getPropertyName()))), scope, scope,
						saveData, null, testFindMode.booleanValue(), false, methodProperty.getPropertyName(), false, true, false);
				}
			}
			catch (Exception ex)
			{
				application.reportError(application.getI18NMessage("servoy.formPanel.error.executeMethod", new Object[] { sName }), ex); //$NON-NLS-1$
			}
		}
		return ret;
	}

	private Map<String, Object[]> hmChildrenJavaMembers;

	public Map<String, Object[]> getJavaMembers()
	{
		return hmChildrenJavaMembers;
	}

	public String getTableName()
	{
		return form.getTableName();
	}

	public String getServerName()
	{
		return form.getServerName();
	}

	public Object setUsingAsExternalComponent(boolean visibleExternal) throws ServoyException
	{
		if (visibleExternal)
		{
			initForJSUsage();
			setView(getView());
			executeOnLoadMethod();
		}
		else
		{
			// make sure that the ui will have pushed all changes
			stopUIEditing(true);
		}

		List<Runnable> invokeLaterRunnables = new ArrayList<Runnable>();
		boolean ok = notifyVisible(visibleExternal, invokeLaterRunnables);
		Utils.invokeLater(application, invokeLaterRunnables);
		if (!ok)
		{
			//TODO cannot hide...what todo?
		}
		else if (visibleExternal && getFormUI() instanceof JComponent)
		{
			// If it is a swing component, test if it has a parent.
			JComponent formUI = (JComponent)getFormUI();
			if (formUI.getParent() != null)
			{
				// remove it from the parent
				formUI.getParent().remove(formUI);
			}
		}
		// and make sure it is visible when we return it. (FixedCardLayout will set it to none visible when remove)
		getFormUI().setComponentVisible(visibleExternal);
		return (visibleExternal ? getFormUI() : null);
	}

	public void selectNextRecord()
	{
		if (form.getOnNextRecordCmdMethodID() == 0)
		{
			if ((application.getFoundSetManager().getEditRecordList().stopEditing(false) & (ISaveConstants.STOPPED + ISaveConstants.AUTO_SAVE_BLOCKED)) == 0)
			{
				return;
			}

			IFoundSetInternal fs = getFoundSet();
			int nextIndex = fs.getSelectedIndex() + 1;
			if (nextIndex >= 0 && nextIndex < fs.getSize())
			{
				final Component comp = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
				fs.setSelectedIndex(nextIndex);
				application.invokeLater(new Runnable()
				{
					public void run()
					{
						if (getView() == IForm.LOCKED_RECORD_VIEW || getView() == IForm.RECORD_VIEW)
						{
							if (comp != null) comp.requestFocus();
						}
						else
						{
							requestFocus();
						}
					}
				});
			}
		}
		else
		{
			executeFormMethod(StaticContentSpecLoader.PROPERTY_ONNEXTRECORDCMDMETHODID, null, Boolean.TRUE, true, true);
		}
	}

	public void selectPrevRecord()
	{
		if (form.getOnPreviousRecordCmdMethodID() == 0)
		{
			int edittingStoppedFlag = application.getFoundSetManager().getEditRecordList().stopEditing(false);
			if (ISaveConstants.STOPPED != edittingStoppedFlag && ISaveConstants.AUTO_SAVE_BLOCKED != edittingStoppedFlag)
			{
				return;
			}

			IFoundSetInternal fs = getFoundSet();
			int nextIndex = fs.getSelectedIndex() - 1;
			if (nextIndex >= 0)
			{
				final Component comp = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
				fs.setSelectedIndex(nextIndex);
				application.invokeLater(new Runnable()
				{
					public void run()
					{
						if (getView() == IForm.LOCKED_RECORD_VIEW || getView() == IForm.RECORD_VIEW)
						{
							if (comp != null) comp.requestFocus();
						}
						else
						{
							requestFocus();
						}
					}
				});
			}
		}
		else
		{
			executeFormMethod(StaticContentSpecLoader.PROPERTY_ONPREVIOUSRECORDCMDMETHODID, null, Boolean.TRUE, true, true);
		}
	}

	public IScriptExecuter getScriptExecuter()
	{
		return scriptExecuter;
	}

	public IFormUIInternal getFormUI()
	{
		return containerImpl;
	}

	@Override
	public String toString()
	{
		if (formModel != null)
		{
			return "FormController[form: " + getName() + ", fs size:" + Integer.toString(formModel.getSize()) + ", selected record: " + formModel.getRecord(formModel.getSelectedIndex()) + ",destroyed:" + destroyed + "]"; //$NON-NLS-1$ //$NON-NLS-2$//$NON-NLS-3$ //$NON-NLS-4$
		}
		else
		{
			return "FormController[form: " + getName() + ",destroyed:" + destroyed + "]"; //$NON-NLS-1$//$NON-NLS-2$
		}
	}

	/**
	 * @deprecated Use databasemanager.saveData()
	 */
	@Deprecated
	public void saveData()
	{
		getApplication().getFoundSetManager().getEditRecordList().stopEditing(false);//we do false here becouse the solution(developer) is in charge, not the plugin developer 		
	}

	/**
	 * 
	 */
	public void touch()
	{
		if (!isFormVisible && fm != null)
		{
			fm.touch(this);
		}
	}

	/**
	 * @return
	 */
	public FormScope getFormScope()
	{
		initForJSUsage();
		return formScope;
	}

	public void recomputeTabSequence(int baseTabSequenceIndex)
	{
		application.getDataRenderFactory().reapplyTabSequence(getFormUI(), baseTabSequenceIndex);
	}

	public void setTabSequence(Object[] arrayOfElements)
	{
		tabSequence.setRuntimeTabSequence(arrayOfElements);
	}

	public String[] getTabSequence()
	{
		return tabSequence.getNamesInTabSequence();
	}

	public void foundSetChanged(FoundSetEvent e)
	{
		if (e.getType() == FoundSetEvent.FIND_MODE_CHANGE)
		{
			// change UI to reflect form find state... that is really determined by the find state of the foundset 
			propagateFindMode(((FoundSet)e.getSource()).isInFindMode());
		}
		else if (e.getType() == FoundSetEvent.SELECTION_MODE_CHANGE)
		{
			pinSelectionModeIfNecessary(); // some other form that used to keep this foundset pinned released it
		}
	}

	private void setFormModelInternal(FoundSet newModel)
	{
		if (formModel == newModel) return;
		boolean isInFind = false;
		FoundSet mustUnpinSelectionMode = null;
		if (formModel != null)
		{
			formModel.removeFoundSetEventListener(this);
			isInFind = formModel.isInFindMode();
			mustUnpinSelectionMode = formModel;
		}
		formModel = newModel;

		if (mustUnpinSelectionMode != null)
		{
			mustUnpinSelectionMode.unpinMultiSelectIfNeeded(form.getID());
		}

		// form model change set it on -2 so that we know that we shouldnt update the selection before it is tested 
		lastSelectedIndex = -2;
		if (formModel != null)
		{
			formModel.addFoundSetEventListener(this);
			if (isInFind != formModel.isInFindMode())
			{
				propagateFindMode(formModel.isInFindMode());
			}
			pinSelectionModeIfNecessary();
		}
	}

	private void pinSelectionModeIfNecessary()
	{
		if (formModel != null)
		{
			int selectionMode = form.getSelectionMode();
			if (selectionMode != SELECTION_MODE_DEFAULT)
			{
				int pinLevel = isFormVisible ? PIN_VISIBLE : PIN_HIDDEN;
				if (selectionMode == SELECTION_MODE_SINGLE)
				{
					formModel.pinMultiSelectIfNeeded(false, form.getID(), pinLevel); // form wants to enforce single selection on the foundsets it uses
				}
				else if (selectionMode == SELECTION_MODE_MULTI)
				{
					formModel.pinMultiSelectIfNeeded(true, form.getID(), pinLevel); // form wants to enforce multi selection on the foundsets it uses
				}
			}
		} // else this form model's multiSelect is already forced by a form and this form is not visible or this form has default non-forcing behavior
	}

	public Serializable getComponentProperty(Object component, String key)
	{
		return ComponentFactory.getComponentProperty(application, component, key);
	}

	public IRuntimeComponent[] getElements()
	{
		Object elementScope = formScope == null ? null : formScope.get("elements");
		if (elementScope instanceof DefaultScope)
		{
			Object[] values = ((DefaultScope)elementScope).getValues();
			List<IRuntimeComponent> elements = new ArrayList<IRuntimeComponent>(values.length);
			for (Object value : values)
			{
				if (value instanceof Wrapper)
				{
					value = ((Wrapper)value).unwrap();
				}
				if (value instanceof IRuntimeComponent)
				{
					elements.add((IRuntimeComponent)value);
				}
			}

			return elements.toArray(new IRuntimeComponent[elements.size()]);
		}

		return new IRuntimeComponent[0];
	}

	public IStyleRule getFormStyle()
	{
		return styleRule;
	}

	public IStyleRule getBodyStyle()
	{
		return bodyRule;
	}
}