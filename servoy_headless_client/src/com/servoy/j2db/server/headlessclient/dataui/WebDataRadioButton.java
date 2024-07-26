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

import javax.swing.text.Document;

import org.apache.wicket.Component;

import com.servoy.base.util.ITagResolver;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.dataprocessing.IEditListener;
import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.ui.scripting.RuntimeRadioButton;

/**
 * Represents a radiobutton field in the webbrowser.
 *
 * @author lvostinar
 *
 */
public class WebDataRadioButton extends WebBaseSelectBox
{
	public WebDataRadioButton(IApplication application, RuntimeRadioButton scriptable, String id, String text, IValueList list)
	{
		this(application, scriptable, id, text);
		onValue = list;
	}

	public WebDataRadioButton(IApplication application, RuntimeRadioButton scriptable, String id, String text)
	{
		super(application, scriptable, id, text);
	}

	public final RuntimeRadioButton getScriptObject()
	{
		return (RuntimeRadioButton)scriptable;
	}


	public void setValueObject(Object value)
	{
		((ChangesRecorder)getStylePropertyChanges()).testChanged(this, value);
		if (getStylePropertyChanges().isChanged())
		{
			// this component is going to update it's contents, without the user changing the
			// components contents; so remove invalid state if necessary
			setValueValid(true, null);
		}
	}

	@Override
	public String toString()
	{
		return getScriptObject().toString("value:" + getValueObject()); //$NON-NLS-1$
	}

	public final class MyRadioButton extends Component implements IDisplayData, WebBaseSelectBox.ISelector
	{
		private static final long serialVersionUID = 1L;
		private final WebBaseSelectBox selectBox;

		private MyRadioButton(String id, WebBaseSelectBox selectBox)
		{
			super(id);
			this.selectBox = selectBox;
		}

		/**
		 * @see wicket.Component#isEnabled()
		 */
		@Override
		public boolean isEnabled()
		{
			return WebDataRadioButton.this.isEnabled();
		}

		public void setTagResolver(ITagResolver resolver)
		{
		}

		/**
		 * @see com.servoy.j2db.dataprocessing.IDisplayData#getValueObject()
		 */
		public Object getValueObject()
		{
			return WebDataRadioButton.this.getValueObject();
		}

		/**
		 * @see com.servoy.j2db.dataprocessing.IDisplayData#setValueObject(java.lang.Object)
		 */
		public void setValueObject(Object data)
		{
			WebDataRadioButton.this.setValueObject(data);
		}

		/**
		 * @see com.servoy.j2db.dataprocessing.IDisplayData#needEditListener()
		 */
		public boolean needEditListener()
		{
			return WebDataRadioButton.this.needEditListener();
		}

		/**
		 * @see com.servoy.j2db.dataprocessing.IDisplayData#needEntireState()
		 */
		public boolean needEntireState()
		{
			return WebDataRadioButton.this.needEntireState();
		}

		/**
		 * @see com.servoy.j2db.dataprocessing.IDisplayData#setNeedEntireState(boolean)
		 */
		public void setNeedEntireState(boolean needEntireState)
		{
			WebDataRadioButton.this.setNeedEntireState(needEntireState);
		}

		/**
		 * @see com.servoy.j2db.dataprocessing.IDisplayData#addEditListener(com.servoy.j2db.dataprocessing.IEditListener)
		 */
		public void addEditListener(IEditListener editListener)
		{
			WebDataRadioButton.this.addEditListener(editListener);
		}

		/**
		 * @see com.servoy.j2db.dataprocessing.IDisplayData#getDataProviderID()
		 */
		public String getDataProviderID()
		{
			return WebDataRadioButton.this.getDataProviderID();
		}

		/**
		 * @see com.servoy.j2db.dataprocessing.IDisplayData#setDataProviderID(java.lang.String)
		 */
		public void setDataProviderID(String dataProviderID)
		{
			WebDataRadioButton.this.setDataProviderID(dataProviderID);
		}

		/**
		 * @see com.servoy.j2db.dataprocessing.IDisplayData#getDocument()
		 */
		public Document getDocument()
		{
			return WebDataRadioButton.this.getDocument();
		}

		/**
		 * @see com.servoy.j2db.dataprocessing.IDisplayData#notifyLastNewValueWasChange(java.lang.Object, java.lang.Object)
		 */
		public void notifyLastNewValueWasChange(Object oldVal, Object newVal)
		{
			WebDataRadioButton.this.notifyLastNewValueWasChange(oldVal, newVal);
		}

		public boolean isValueValid()
		{
			return WebDataRadioButton.this.isValueValid();
		}

		public void setValueValid(boolean valid, Object oldVal)
		{
			WebDataRadioButton.this.setValueValid(valid, oldVal);
		}

		/**
		 * @see com.servoy.j2db.dataprocessing.IDisplay#setValidationEnabled(boolean)
		 */
		public void setValidationEnabled(boolean b)
		{
			WebDataRadioButton.this.setValidationEnabled(b);
		}

		/**
		 * @see com.servoy.j2db.dataprocessing.IDisplay#stopUIEditing(boolean)
		 */
		public boolean stopUIEditing(boolean looseFocus)
		{
			return WebDataRadioButton.this.stopUIEditing(looseFocus);
		}

		public boolean isReadOnly()
		{
			return WebDataRadioButton.this.isReadOnly();
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.server.headlessclient.dataui.WebBaseSelectBox.ISelector#getSelectBox()
		 */
		@Override
		public WebBaseSelectBox getSelectBox()
		{
			return selectBox;
		}
	}
}