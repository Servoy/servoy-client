/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

package com.servoy.j2db.server.ngclient.property;

import java.awt.print.PrinterJob;
import java.util.List;
import java.util.Map;

import com.servoy.j2db.BasicFormController.JSForm;
import com.servoy.j2db.FormController.RuntimeSupportScriptProviders;
import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.dataprocessing.IFoundSet;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.ISupportScriptProviders;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.scripting.CreationalPrototype;
import com.servoy.j2db.scripting.FormScope;
import com.servoy.j2db.server.ngclient.INGApplication;
import com.servoy.j2db.server.ngclient.IWebFormController;
import com.servoy.j2db.server.ngclient.IWebFormUI;
import com.servoy.j2db.server.ngclient.component.RuntimeWebComponent;
import com.servoy.j2db.ui.runtime.IRuntimeComponent;
import com.servoy.j2db.util.ServoyException;

/**
 * @author jcompagner
 *
 */
public class TestFormController implements IWebFormController
{

	private final Form form;
	private final INGApplication application;
	private final IWebFormUI webFormUI;

	public TestFormController(Form form, INGApplication application)
	{
		this.form = form;
		this.webFormUI = new TestWebFormUI(this);
		this.application = application;
	}

	@Override
	public String getName()
	{
		return form.getName();
	}

	@Override
	public Form getForm()
	{
		return form;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IFormController#getFormModel()
	 */
	@Override
	public FoundSet getFormModel()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IFormController#touch()
	 */
	@Override
	public void touch()
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IFormController#getTable()
	 */
	@Override
	public ITable getTable()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IFormController#getFormScope()
	 */
	@Override
	public FormScope getFormScope()
	{
		return new FormScope(this, new ISupportScriptProviders[] { new RuntimeSupportScriptProviders(application, getForm()) });
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IFormController#recreateUI()
	 */
	@Override
	public boolean recreateUI()
	{
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IFormController#initForJSUsage(com.servoy.j2db.scripting.CreationalPrototype)
	 */
	@Override
	public JSForm initForJSUsage(CreationalPrototype creationalPrototype)
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IFormController#notifyVisible(boolean, java.util.List)
	 */
	@Override
	public boolean notifyVisible(boolean visible, List<Runnable> invokeLaterRunnables)
	{
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IFormController#destroy()
	 */
	@Override
	public void destroy()
	{
		// TODO Auto-generated method stub

	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IFormController#isDestroyed()
	 */
	@Override
	public boolean isDestroyed()
	{
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IFormController#initForJSUsage()
	 */
	@Override
	public JSForm initForJSUsage()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IFormController#isFormVisible()
	 */
	@Override
	public boolean isFormVisible()
	{
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IFormController#init()
	 */
	@Override
	public void init()
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IFormController#isFormExecutingFunction()
	 */
	@Override
	public boolean isFormExecutingFunction()
	{
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IFormController#executeOnLoadMethod()
	 */
	@Override
	public void executeOnLoadMethod()
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IFormController#notifyResized()
	 */
	@Override
	public void notifyResized()
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IFormController#executeFunction(java.lang.String, java.lang.Object[], boolean, java.lang.Object, boolean, java.lang.String)
	 */
	@Override
	public Object executeFunction(String cmd, Object[] args, boolean saveData, Object src, boolean focusEvent, String methodKey)
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IFormController#executeFunction(java.lang.String, java.lang.Object[], boolean, java.lang.Object, boolean, java.lang.String, boolean,
	 * boolean, boolean)
	 */
	@Override
	public Object executeFunction(String cmd, Object[] args, boolean saveData, Object src, boolean focusEvent, String methodKey, boolean allowFoundsetMethods,
		boolean executeWhenFieldValidationFailed, boolean throwException) throws Exception
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IFormController#wantEmptyFoundSet()
	 */
	@Override
	public boolean wantEmptyFoundSet()
	{
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IFormController#loadAllRecordsImpl(boolean)
	 */
	@Override
	public boolean loadAllRecordsImpl(boolean b) throws ServoyException
	{
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IFormController#refreshView()
	 */
	@Override
	public void refreshView()
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IFormController#setMode(int)
	 */
	@Override
	public boolean setMode(int mode)
	{
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IForm#eval(java.lang.String)
	 */
	@Override
	public Object eval(String javascript)
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IForm#setUsingAsExternalComponent(boolean)
	 */
	@Override
	public Object setUsingAsExternalComponent(boolean visibleExternal) throws ServoyException
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IForm#loadAllRecords()
	 */
	@Override
	public void loadAllRecords()
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IForm#isShowingData()
	 */
	@Override
	public boolean isShowingData()
	{
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IForm#deleteRecord()
	 */
	@Override
	public boolean deleteRecord()
	{
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IForm#deleteAllRecords()
	 */
	@Override
	public boolean deleteAllRecords()
	{
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IForm#newRecord()
	 */
	@Override
	public void newRecord()
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IForm#duplicateRecord()
	 */
	@Override
	public void duplicateRecord()
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IForm#find()
	 */
	@Override
	public void find()
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IForm#isInFindMode()
	 */
	@Override
	public boolean isInFindMode()
	{
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IForm#removeLastFound()
	 */
	@Override
	public void removeLastFound()
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IForm#performFind(boolean, boolean, boolean)
	 */
	@Override
	public int performFind(boolean clear, boolean reduce, boolean showDialogOnNoResults)
	{
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IForm#omitRecord()
	 */
	@Override
	public void omitRecord()
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IForm#showOmittedRecords()
	 */
	@Override
	public void showOmittedRecords()
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IForm#invertRecords()
	 */
	@Override
	public void invertRecords()
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IForm#showSortDialog()
	 */
	@Override
	public void showSortDialog() throws Exception
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IForm#reLookupValues()
	 */
	@Override
	public void reLookupValues()
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IForm#saveData()
	 */
	@Override
	public void saveData()
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IForm#printPreview()
	 */
	@Override
	public void printPreview()
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IForm#print(boolean, boolean, boolean, java.awt.print.PrinterJob)
	 */
	@Override
	public void print(boolean showManyRecordsDialog, boolean printCurrentRecordOnly, boolean showPrinterSelectDialog, PrinterJob printerJob)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IForm#getView()
	 */
	@Override
	public int getView()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IForm#setView(int)
	 */
	@Override
	public void setView(int v)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IForm#getDataSource()
	 */
	@Override
	public String getDataSource()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IForm#isReadOnly()
	 */
	@Override
	public boolean isReadOnly()
	{
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IForm#getElements()
	 */
	@Override
	public IRuntimeComponent[] getElements()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IForm#loadRecords(com.servoy.j2db.dataprocessing.IFoundSet)
	 */
	@Override
	public boolean loadRecords(IFoundSet foundSet)
	{
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IForm#getDesignTimeProperty(java.lang.String)
	 */
	@Override
	public Object getDesignTimeProperty(String key)
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.server.ngclient.IWebFormController#getFormUI()
	 */
	@Override
	public IWebFormUI getFormUI()
	{
		return webFormUI;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.server.ngclient.IWebFormController#getApplication()
	 */
	@Override
	public INGApplication getApplication()
	{
		return application;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.server.ngclient.IWebFormController#getFoundSet()
	 */
	@Override
	public IFoundSetInternal getFoundSet()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.server.ngclient.IWebFormController#setParentFormController(com.servoy.j2db.server.ngclient.IWebFormController)
	 */
	@Override
	public void setParentFormController(IWebFormController parentFormController)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.server.ngclient.IWebFormController#setRendering(boolean)
	 */
	@Override
	public void setRendering(boolean rendering)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.server.ngclient.IWebFormController#isRendering()
	 */
	@Override
	public boolean isRendering()
	{
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.server.ngclient.IWebFormController#setActiveNavigatorDescription(java.util.Map)
	 */
	@Override
	public void setNavigatorProperties(Map<String, Object> navigatorDescription)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.server.ngclient.IWebFormController#getActiveNavigatorDescription()
	 */
	@Override
	public Map<String, Object> getNavigatorProperties()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.server.ngclient.IWebFormController#getWebComponentElements()
	 */
	@Override
	public RuntimeWebComponent[] getWebComponentElements()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IFormController#stopUIEditing(boolean)
	 */
	@Override
	public boolean stopUIEditing(boolean looseFocus)
	{
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * @see com.servoy.j2db.IFormController#hasParentForm()
	 */
	@Override
	public boolean hasParentForm()
	{
		return false;
	}
}