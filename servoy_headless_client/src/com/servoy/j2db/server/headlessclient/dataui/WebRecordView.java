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


import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.IView;
import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;


/**
 * Web version of RecordView showing one record at the time
 * 
 * @author jcompagner
 */
public class WebRecordView extends WebMarkupContainer implements IView
{
	private static final long serialVersionUID = 1L;
	private IApplication application;
	private String bgColorScript;
	private List<Object> bgColorArgs;

	/**
	 * @param id
	 */
	public WebRecordView(String name)
	{
		super(name);
	}


	/**
	 * @see com.servoy.j2db.IView#setModel(com.servoy.j2db.dataprocessing.ISwingFoundSet)
	 */
	public void setModel(IFoundSetInternal fs)
	{
		//ignore, using pull models
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.IView#start(com.servoy.j2db.IApplication)
	 */
	public void start(IApplication app)
	{
		//nothing to start
		application = app;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.IView#stop()
	 */
	public void stop()
	{
		// TODO Auto-generated method stub
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.IView#editCellAt(int)
	 */
	public boolean editCellAt(int i)
	{
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.IView#stopUIEditing()
	 */
	public boolean stopUIEditing(final boolean looseFocus)
	{
		Object hasInvalidValue = visitChildren(IDisplayData.class, new IVisitor()
		{
			public Object component(Component component)
			{
				if (!((IDisplayData)component).stopUIEditing(looseFocus))
				{
					return Boolean.TRUE;
				}
				return IVisitor.CONTINUE_TRAVERSAL;
			}
		});

		return hasInvalidValue != Boolean.TRUE;
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.IView#isEditing()
	 */
	public boolean isEditing()
	{
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.IView#destroy()
	 */
	public void destroy()
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.IView#setRowBGColorCalculation(java.lang.String)
	 */
	public void setRowBGColorScript(String bgColorCalc, List<Object> args)
	{
		this.bgColorScript = bgColorCalc;
		this.bgColorArgs = args;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.IView#getRowBGColorCalculation()
	 */
	public String getRowBGColorScript()
	{
		return bgColorScript;
	}

	public List<Object> getRowBGColorArgs()
	{
		return bgColorArgs;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.IView#requestFocus()
	 */
	public void requestFocus()
	{
		//TODO:not possible?
	}

	public void ensureIndexIsVisible(int index)
	{
	}

	public boolean isDisplayingMoreThanOneRecord()
	{
		return false;
	}

	public void setEditable(boolean findMode)
	{
	}
}
