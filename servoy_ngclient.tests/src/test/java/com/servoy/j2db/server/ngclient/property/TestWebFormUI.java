/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2015 Servoy BV

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

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.print.PrinterJob;
import java.util.Collection;
import java.util.List;

import javax.swing.border.Border;

import org.json.JSONException;
import org.json.JSONWriter;
import org.sablo.WebComponent;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.websocket.utils.JSONUtils.IToJSONConverter;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.JSDataSet;
import com.servoy.j2db.server.ngclient.DataAdapterList;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.IDataAdapterList;
import com.servoy.j2db.server.ngclient.IServoyDataConverterContext;
import com.servoy.j2db.server.ngclient.IWebFormController;
import com.servoy.j2db.server.ngclient.IWebFormUI;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.component.RuntimeWebComponent;

/**
 * @author gboros
 *
 */
public class TestWebFormUI implements IWebFormUI
{
	private final IDataAdapterList dataAdapterList;

	public TestWebFormUI(IWebFormController formController)
	{
		dataAdapterList = new DataAdapterList(formController);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IBasicFormUI#showYesNoQuestionDialog(com.servoy.j2db.IApplication, java.lang.String, java.lang.String)
	 */
	@Override
	public boolean showYesNoQuestionDialog(IApplication application, String dlgMessage, String string)
	{
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IBasicFormUI#getContainerName()
	 */
	@Override
	public String getContainerName()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IBasicFormUI#printPreview(boolean, boolean, int, java.awt.print.PrinterJob)
	 */
	@Override
	public void printPreview(boolean showDialogs, boolean printCurrentRecordOnly, int zoomFactor, PrinterJob printerJob)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IBasicFormUI#print(boolean, boolean, boolean, java.awt.print.PrinterJob)
	 */
	@Override
	public void print(boolean showDialogs, boolean printCurrentRecordOnly, boolean showPrinterSelectDialog, PrinterJob printerJob)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IBasicFormUI#printXML(boolean)
	 */
	@Override
	public String printXML(boolean printCurrentRecordOnly)
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IBasicFormUI#showSortDialog(com.servoy.j2db.IApplication, java.lang.String)
	 */
	@Override
	public void showSortDialog(IApplication application, String options)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IBasicFormUI#getFormWidth()
	 */
	@Override
	public int getFormWidth()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IBasicFormUI#getPartHeight(int)
	 */
	@Override
	public int getPartHeight(int partType)
	{
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IBasicFormUI#getFormContext()
	 */
	@Override
	public JSDataSet getFormContext()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IBasicFormUI#changeFocusIfInvalid(java.util.List)
	 */
	@Override
	public void changeFocusIfInvalid(List<Runnable> invokeLaterRunnables)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IBasicFormUI#prepareForSave(boolean)
	 */
	@Override
	public void prepareForSave(boolean looseFocus)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.IComponent#setComponentEnabled(boolean)
	 */
	@Override
	public void setComponentEnabled(boolean enabled)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.IComponent#isEnabled()
	 */
	@Override
	public boolean isEnabled()
	{
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.IComponent#setComponentVisible(boolean)
	 */
	@Override
	public void setComponentVisible(boolean visible)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.server.ngclient.IWebFormUI#notifyVisible(boolean, java.util.List)
	 */
	@Override
	public boolean notifyVisible(boolean visible, List<Runnable> invokeLaterRunnables)
	{
		return true;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.IComponent#isVisible()
	 */
	@Override
	public boolean isVisible()
	{
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.IComponent#setLocation(java.awt.Point)
	 */
	@Override
	public void setLocation(Point location)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.IComponent#getLocation()
	 */
	@Override
	public Point getLocation()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.IComponent#setSize(java.awt.Dimension)
	 */
	@Override
	public void setSize(Dimension size)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.IComponent#getSize()
	 */
	@Override
	public Dimension getSize()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.IComponent#setForeground(java.awt.Color)
	 */
	@Override
	public void setForeground(Color foreground)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.IComponent#getForeground()
	 */
	@Override
	public Color getForeground()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.IComponent#setBackground(java.awt.Color)
	 */
	@Override
	public void setBackground(Color background)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.IComponent#getBackground()
	 */
	@Override
	public Color getBackground()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.IComponent#setFont(java.awt.Font)
	 */
	@Override
	public void setFont(Font font)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.IComponent#getFont()
	 */
	@Override
	public Font getFont()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.IComponent#setBorder(javax.swing.border.Border)
	 */
	@Override
	public void setBorder(Border border)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.IComponent#getBorder()
	 */
	@Override
	public Border getBorder()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.IComponent#setName(java.lang.String)
	 */
	@Override
	public void setName(String name)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.IComponent#getName()
	 */
	@Override
	public String getName()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.IComponent#setOpaque(boolean)
	 */
	@Override
	public void setOpaque(boolean opaque)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.IComponent#isOpaque()
	 */
	@Override
	public boolean isOpaque()
	{
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.IComponent#setCursor(java.awt.Cursor)
	 */
	@Override
	public void setCursor(Cursor cursor)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.IComponent#setToolTipText(java.lang.String)
	 */
	@Override
	public void setToolTipText(String tooltip)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.IComponent#getToolTipText()
	 */
	@Override
	public String getToolTipText()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.IComponent#getId()
	 */
	@Override
	public String getId()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IView#setModel(com.servoy.j2db.dataprocessing.IFoundSetInternal)
	 */
	@Override
	public void setModel(IFoundSetInternal fs)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IView#start(com.servoy.j2db.IApplication)
	 */
	@Override
	public void start(IApplication app)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IView#stop()
	 */
	@Override
	public void stop()
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IView#editCellAt(int)
	 */
	@Override
	public boolean editCellAt(int row)
	{
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IView#stopUIEditing(boolean)
	 */
	@Override
	public boolean stopUIEditing(boolean looseFocus)
	{
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IView#isEditing()
	 */
	@Override
	public boolean isEditing()
	{
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IView#requestFocus()
	 */
	@Override
	public void requestFocus()
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IView#ensureIndexIsVisible(int)
	 */
	@Override
	public void ensureIndexIsVisible(int index)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IView#setEditable(boolean)
	 */
	@Override
	public void setEditable(boolean findMode)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IView#isDisplayingMoreThanOneRecord()
	 */
	@Override
	public boolean isDisplayingMoreThanOneRecord()
	{
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IView#getVisibleRect()
	 */
	@Override
	public Rectangle getVisibleRect()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IView#setVisibleRect(java.awt.Rectangle)
	 */
	@Override
	public void setVisibleRect(Rectangle scrollPosition)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.ISupportRowBGColorScript#setRowBGColorScript(java.lang.String, java.util.List)
	 */
	@Override
	public void setRowBGColorScript(String bgColorScript, List<Object> args)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.ISupportRowBGColorScript#getRowBGColorScript()
	 */
	@Override
	public String getRowBGColorScript()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.ISupportRowBGColorScript#getRowBGColorArgs()
	 */
	@Override
	public List<Object> getRowBGColorArgs()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.util.IDestroyable#destroy()
	 */
	@Override
	public void destroy()
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.sablo.IChangeListener#valueChanged()
	 */
	@Override
	public void valueChanged()
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.server.ngclient.IWebFormUI#writeAllComponentsProperties(org.json.JSONWriter, org.sablo.websocket.utils.JSONUtils.IToJSONConverter)
	 */
	@Override
	public boolean writeAllComponentsProperties(JSONWriter w, IToJSONConverter<IBrowserConverterContext> converter) throws JSONException
	{
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.server.ngclient.IWebFormUI#getWebComponent(java.lang.String)
	 */
	@Override
	public WebFormComponent getWebComponent(String name)
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.server.ngclient.IWebFormUI#getComponents()
	 */
	@Override
	public Collection<WebComponent> getComponents()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.server.ngclient.IWebFormUI#getDataAdapterList()
	 */
	@Override
	public IDataAdapterList getDataAdapterList()
	{
		return dataAdapterList;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.server.ngclient.IWebFormUI#init()
	 */
	@Override
	public void init()
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.server.ngclient.IWebFormUI#setReadOnly(boolean)
	 */
	@Override
	public void setReadOnly(boolean readOnly)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.server.ngclient.IWebFormUI#setParentContainer(com.servoy.j2db.server.ngclient.WebFormComponent)
	 */
	@Override
	public void setParentContainer(WebFormComponent parentContainer)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.server.ngclient.IWebFormUI#getParentWindowName()
	 */
	@Override
	public String getParentWindowName()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.server.ngclient.IWebFormUI#setParentWindowName(java.lang.String)
	 */
	@Override
	public void setParentWindowName(String parentWindowName)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.server.ngclient.IWebFormUI#getDataConverterContext()
	 */
	@Override
	public IServoyDataConverterContext getDataConverterContext()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.server.ngclient.IWebFormUI#contributeComponentToElementsScope(com.servoy.j2db.server.ngclient.FormElement,
	 * org.sablo.specification.WebComponentSpecification, com.servoy.j2db.server.ngclient.WebFormComponent)
	 */
	@Override
	public void contributeComponentToElementsScope(FormElement fe, WebObjectSpecification componentSpec, WebFormComponent component)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.server.ngclient.IWebFormUI#getController()
	 */
	@Override
	public IWebFormController getController()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.server.ngclient.IWebFormUI#getParentContainer()
	 */
	@Override
	public Object getParentContainer()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RuntimeWebComponent getRuntimeWebComponent(String name)
	{
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void removeComponentFromElementsScope(FormElement element, WebObjectSpecification webComponentSpec, WebFormComponent childComponent)
	{
		// TODO Auto-generated method stub

	}

}
