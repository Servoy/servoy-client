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

import com.servoy.j2db.IApplication;
import com.servoy.j2db.ui.IEventExecutor;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.ILabel;
import com.servoy.j2db.ui.IScriptTextEditorMethods;

/**
 * 
 * Dummy class that just extends {@link WebDataLabel} so that RTF fields map on this class.
 * Doesnt have an implementation.
 * 
 * @author jcompagner
 * 
 */
public class WebDataRtfField extends WebDataLabel implements IFieldComponent, IScriptTextEditorMethods
{
	/**
	 * @param application
	 * @param id
	 */
	public WebDataRtfField(IApplication application, String id)
	{
		super(application, id);
	}

	private static final long serialVersionUID = 1L;
	private int dataType;
	private String format;

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

	public String[] js_getLabelForElementNames()
	{
		return new String[0];
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

	/**
	 * @see com.servoy.j2db.ui.IScriptTextEditorMethods#js_getAsPlainText()
	 */
	public String js_getAsPlainText()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @see com.servoy.j2db.ui.IScriptTextEditorMethods#js_getBaseURL()
	 */
	public String js_getBaseURL()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @see com.servoy.j2db.ui.IScriptTextEditorMethods#js_getURL()
	 */
	public String js_getURL()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @see com.servoy.j2db.ui.IScriptTextEditorMethods#js_setBaseURL(java.lang.String)
	 */
	public void js_setBaseURL(String url)
	{
		// TODO Auto-generated method stub

	}

	/**
	 * @see com.servoy.j2db.ui.IScriptTextEditorMethods#js_setURL(java.lang.String)
	 */
	public void js_setURL(String url)
	{
		// TODO Auto-generated method stub

	}

	/**
	 * @see com.servoy.j2db.ui.IScriptReadOnlyMethods#js_isReadOnly()
	 */
	public boolean js_isReadOnly()
	{
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * @see com.servoy.j2db.ui.IScriptReadOnlyMethods#js_setReadOnly(boolean)
	 */
	public void js_setReadOnly(boolean b)
	{
		// TODO Auto-generated method stub

	}

	/**
	 * @see com.servoy.j2db.ui.IScriptScrollableMethods#js_getScrollX()
	 */
	public int js_getScrollX()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * @see com.servoy.j2db.ui.IScriptScrollableMethods#js_getScrollY()
	 */
	public int js_getScrollY()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * @see com.servoy.j2db.ui.IScriptScrollableMethods#js_setScroll(int, int)
	 */
	public void js_setScroll(int x, int y)
	{
		// TODO Auto-generated method stub

	}

	/**
	 * @see com.servoy.j2db.ui.IScriptTextInputMethods#js_getCaretPosition()
	 */
	public int js_getCaretPosition()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * @see com.servoy.j2db.ui.IScriptTextInputMethods#js_getSelectedText()
	 */
	public String js_getSelectedText()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @see com.servoy.j2db.ui.IScriptTextInputMethods#js_replaceSelectedText(java.lang.String)
	 */
	public void js_replaceSelectedText(String s)
	{
		// TODO Auto-generated method stub

	}

	/**
	 * @see com.servoy.j2db.ui.IScriptTextInputMethods#js_selectAll()
	 */
	public void js_selectAll()
	{
		// TODO Auto-generated method stub

	}

	/**
	 * @see com.servoy.j2db.ui.IScriptTextInputMethods#js_setCaretPosition(int)
	 */
	public void js_setCaretPosition(int pos)
	{
		// TODO Auto-generated method stub

	}

	/**
	 * @see com.servoy.j2db.ui.IScriptInputMethods#js_getDataProviderID()
	 */
	@Override
	public String js_getDataProviderID()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @see com.servoy.j2db.ui.IScriptInputMethods#js_isEditable()
	 */
	public boolean js_isEditable()
	{
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * @see com.servoy.j2db.ui.IScriptInputMethods#js_requestFocus(java.lang.Object[])
	 */
	public void js_requestFocus(Object[] vargs)
	{
		// TODO Auto-generated method stub

	}

	/**
	 * @see com.servoy.j2db.ui.IScriptInputMethods#js_setEditable(boolean)
	 */
	public void js_setEditable(boolean b)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public String toString()
	{
		return js_getElementType() + "(web)[name:" + js_getName() + ",x:" + js_getLocationX() + ",y:" + js_getLocationY() + ",width:" + js_getWidth() + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 
			",height:" + js_getHeight() + ",value:" + getDefaultModelObjectAsString() + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

}
