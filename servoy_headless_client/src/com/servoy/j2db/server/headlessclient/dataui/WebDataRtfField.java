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
package com.servoy.j2db.server.headlessclient.dataui;

import java.awt.Insets;
import java.util.List;

import javax.swing.JEditorPane;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.ui.IEventExecutor;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.ILabel;
import com.servoy.j2db.ui.scripting.AbstractRuntimeTextEditor;
import com.servoy.j2db.util.Text;

/**
 * 
 * Dummy class that just extends {@link WebDataLabel} so that RTF fields map on this class.
 * Doesnt have an implementation.
 * 
 * @author jcompagner
 * 
 */
public class WebDataRtfField extends WebDataLabel implements IFieldComponent
{
	private static final long serialVersionUID = 1L;
	private int dataType;
	private String format;

	/**
	 * @param application
	 * @param id
	 */
	public WebDataRtfField(IApplication application, AbstractRuntimeTextEditor<IFieldComponent, JEditorPane> scriptable, String id)
	{
		super(application, scriptable, id);
		((ChangesRecorder)scriptable.getChangesRecorder()).setDefaultBorderAndPadding(null, TemplateGenerator.DEFAULT_LABEL_PADDING);
	}

	/**
	 * @see com.servoy.j2db.ui.IFieldComponent#getDataType()
	 */
	public int getDataType()
	{
		return dataType;
	}

	/**
	 * @see com.servoy.j2db.ui.IFieldComponent#setActionCmd(java.lang.String, Object[])
	 */
	public void setActionCmd(String actionCmd, Object[] args)
	{
	}

	/**
	 * @see com.servoy.j2db.ui.IFieldComponent#setChangeCmd(java.lang.String, Object[])
	 */
	public void setChangeCmd(String changeCmd, Object[] args)
	{

	}

	/**
	 * @see com.servoy.j2db.ui.IFieldComponent#setEditable(boolean)
	 */
	public void setEditable(boolean editable)
	{

	}

	public void addLabelFor(ILabel label)
	{
		// TODO void for now
	}

	/**
	 * @see com.servoy.j2db.ui.IFieldComponent#setEnterCmds(java.lang.String, Object[][])
	 */
	public void setEnterCmds(String[] enterCmds, Object[][] args)
	{
		IEventExecutor eventExecutor = getEventExecutor();
		if (eventExecutor instanceof WebEventExecutor) ((WebEventExecutor)eventExecutor).setEnterCmds(enterCmds, args);
	}

	/**
	 * @see com.servoy.j2db.ui.IFieldComponent#setFormat(int, java.lang.String)
	 */
	public void setFormat(int dataType, String format)
	{
		this.dataType = dataType;
		this.format = format;
	}

	/**
	 * @see com.servoy.j2db.ui.IFieldComponent#setLeaveCmds(String[], Object[][])
	 */
	public void setLeaveCmds(String[] leaveCmds, Object[][] args)
	{
		IEventExecutor eventExecutor = getEventExecutor();
		if (eventExecutor instanceof WebEventExecutor) ((WebEventExecutor)eventExecutor).setLeaveCmds(leaveCmds, args);
	}

	/**
	 * @see com.servoy.j2db.ui.IFieldComponent#setMargin(java.awt.Insets)
	 */
	public void setMargin(Insets margin)
	{
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ui.IFieldComponent#getMargin()
	 */
	public Insets getMargin()
	{
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ui.IFieldComponent#isEditable()
	 */
	public boolean isEditable()
	{
		return false;
	}

	/**
	 * @see com.servoy.j2db.ui.IFieldComponent#setMaxLength(int)
	 */
	public void setMaxLength(int maxLength)
	{
	}

	/**
	 * @see com.servoy.j2db.ui.IFieldComponent#setSelectOnEnter(boolean)
	 */
	public void setSelectOnEnter(boolean selectOnEnter)
	{
	}


	@Override
	public String toString()
	{
		return getScriptObject().toString("value:" + getDefaultModelObjectAsString()); //$NON-NLS-1$ 
	}

	public List<ILabel> getLabelsFor()
	{
		return null;
	}

	public String getTitleText()
	{
		return Text.processTags(titleText, resolver);
	}

	public void requestFocus(Object[] vargs)
	{

	}

	public void setReadOnly(boolean b)
	{

	}

}
