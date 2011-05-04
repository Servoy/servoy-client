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

import com.servoy.j2db.IApplication;
import com.servoy.j2db.scripting.IScriptable;
import com.servoy.j2db.ui.IEventExecutor;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.ILabel;
import com.servoy.j2db.ui.scripting.RuntimeRTFArea;
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
	private final RuntimeRTFArea scriptable;

	/**
	 * @param application
	 * @param id
	 */
	public WebDataRtfField(IApplication application, String id)
	{
		super(application, id);
		this.scriptable = new RuntimeRTFArea(this, new ChangesRecorder(null, TemplateGenerator.DEFAULT_LABEL_PADDING), application, null);
	}

	@Override
	public IScriptable getScriptObject()
	{
		return this.scriptable;
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
		// TODO Auto-generated method stub

	}

	/**
	 * @see com.servoy.j2db.ui.IFieldComponent#setChangeCmd(java.lang.String, Object[])
	 */
	public void setChangeCmd(String changeCmd, Object[] args)
	{
		// TODO Auto-generated method stub

	}

	/**
	 * @see com.servoy.j2db.ui.IFieldComponent#setEditable(boolean)
	 */
	public void setEditable(boolean editable)
	{
		// TODO Auto-generated method stub

	}

	public void addLabelFor(ILabel label)
	{
		// TODO void for now
	}

	@Override
	public IEventExecutor getEventExecutor()
	{
		return null;
	}

	/**
	 * @see com.servoy.j2db.ui.IFieldComponent#setEnterCmds(java.lang.String, Object[][])
	 */
	public void setEnterCmds(String[] enterCmds, Object[][] args)
	{
		// TODO Auto-generated method stub

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
		// TODO Auto-generated method stub

	}

	/**
	 * @see com.servoy.j2db.ui.IFieldComponent#setMargin(java.awt.Insets)
	 */
	public void setMargin(Insets margin)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ui.IFieldComponent#getMargin()
	 */
	public Insets getMargin()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ui.IFieldComponent#isEditable()
	 */
	public boolean isEditable()
	{
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * @see com.servoy.j2db.ui.IFieldComponent#setMaxLength(int)
	 */
	public void setMaxLength(int maxLength)
	{
		// TODO Auto-generated method stub

	}

	/**
	 * @see com.servoy.j2db.ui.IFieldComponent#setSelectOnEnter(boolean)
	 */
	public void setSelectOnEnter(boolean selectOnEnter)
	{
		// TODO Auto-generated method stub

	}


	@Override
	public String toString()
	{
		return scriptable.js_getElementType() +
			"(web)[name:" + scriptable.js_getName() + ",x:" + scriptable.js_getLocationX() + ",y:" + scriptable.js_getLocationY() + ",width:" + scriptable.js_getWidth() + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 
			",height:" + scriptable.js_getHeight() + ",value:" + getDefaultModelObjectAsString() + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
