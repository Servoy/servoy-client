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

import java.util.HashMap;
import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.model.AbstractWrapModel;
import org.apache.wicket.model.IComponentInheritedModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.IWrapModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.mozilla.javascript.Scriptable;

import com.servoy.j2db.ApplicationException;
import com.servoy.j2db.dataprocessing.DataAdapterList;
import com.servoy.j2db.dataprocessing.FindState;
import com.servoy.j2db.dataprocessing.IDisplay;
import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.dataprocessing.IRecord;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.TagResolver;
import com.servoy.j2db.dataprocessing.ValueFactory.DbIdentValue;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.scripting.FormScope;
import com.servoy.j2db.scripting.GlobalScope;
import com.servoy.j2db.server.headlessclient.WebClientSession;
import com.servoy.j2db.server.headlessclient.WebForm;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ITagResolver;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.Text;
import com.servoy.j2db.util.Utils;

/**
 * A model that holds 1 {@link IRecord}.
 * 
 * @author jcompagner
 */
public abstract class RecordItemModel extends LoadableDetachableModel implements IComponentInheritedModel
{
	private static final Object NONE = new Object();

	private final Map<Component, Object> lastRenderedValues = new HashMap<Component, Object>();
	public Object lastInvalidValue = NONE;


	/**
	 * @see wicket.model.IModel#getNestedModel()
	 */
	public IModel getNestedModel()
	{
		return null;
	}

	/**
	 * @see wicket.model.LoadableDetachableModel#load()
	 */
	@Override
	protected Object load()
	{
		IRecordInternal rec = getRecord();
		if (rec == null)
		{
			Debug.trace("no record found", new RuntimeException()); //$NON-NLS-1$
		}
		return rec;
	}

	protected abstract IRecordInternal getRecord();

	public IWrapModel wrapOnInheritance(Component component)
	{
		return new WrapModel(component);
	}


	class WrapModel extends AbstractWrapModel implements ITagResolver
	{
		private static final long serialVersionUID = 1L;

		private final Component component;

		WrapModel(Component component)
		{
			this.component = component;
			if (component instanceof IDisplayData) ((IDisplayData)component).setTagResolver(this);
		}

		/**
		 * @see wicket.model.IWrapModel#getNestedModel()
		 */
		public IModel getWrappedModel()
		{
			return RecordItemModel.this;
		}

		@Override
		public void detach()
		{
			RecordItemModel.this.detach();
		}

		/**
		 * @see wicket.model.IModel#getObject()
		 */
		@Override
		public Object getObject()
		{
			if (component instanceof WebRect)
			{
				// special check
				return "";
			}

			if ((component instanceof IDisplayData) && !((IDisplayData)component).isValueValid() && lastInvalidValue != NONE)
			{
				return lastInvalidValue;
			}

			String dataProviderID = getDataProviderID(component);
			if (dataProviderID == null) return null;

			Object value = getValue(component, dataProviderID);

			if (((IDisplayData)component).needEntireState())
			{
				if (value instanceof String)
				{
					value = Text.processTags((String)value, this);
				}
				// Tooltip should also be pulled! (component.getTooltip())
				//						if (tooltip != null)
				//						{
				//						setToolTipText(Text.processTags(tooltip, resolver));
				//						}
			}


			if (component instanceof IResolveObject)
			{
				value = ((IResolveObject)component).resolveDisplayValue(value);
			}

			return value;
		}

		/**
		 * @see wicket.model.IModel#setObject(java.lang.Object)
		 */
		@Override
		public void setObject(Object obj)
		{
			if (component instanceof IDisplay)
			{
				// ignore fields that are read only or disabled
				IDisplay display = (IDisplay)component;
				if (display.isReadOnly() || !display.isEnabled()) return;
			}
			if (component instanceof IDisplayData)
			{
				((IDisplayData)component).setTagResolver(this);
			}
			String dataProviderID = getDataProviderID(component);
			if (dataProviderID == null) return;

			if (!((IDisplayData)component).isValueValid() || !Utils.equalObjects(lastRenderedValues.get(component), obj))
			{
				lastRenderedValues.put(component, obj); // this is normally called as a result of a change in the browser (so component in browser shows this value already); if this is called manually from server side code, setChanged() might also be needed on that component separately when it needs to be rendered back to the browser;
				// this is needed not to interfere with components that use lots of JS like type-aheads when field contents change;
				// if the field uses a formatter for example that would display the value different then it parsed it, setChanged() should be manually called (see FormatConverter use of StateFullSimpleDateFormat)

				setValue(component, dataProviderID, obj);
			}
		}

		public String getStringValue(String name)
		{
			IRecordInternal currentRecord = (IRecordInternal)RecordItemModel.this.getObject();
			WebForm webForm = component.findParent(WebForm.class);
			if (webForm != null)
			{
				FormScope fs = webForm.getController().getFormScope();
				Object value = DataAdapterList.getValueObject(currentRecord, fs, name);
				return TagResolver.formatObject(value, webForm.getController().getApplication().getSettings());
			}
			return null;
		}
	}

	private String getDataProviderID(final Component component)
	{
		String dataProviderID = null;
		if (component instanceof IDisplayData)
		{
			dataProviderID = ((IDisplayData)component).getDataProviderID();
		}
		return dataProviderID;
	}

	public void updateRenderedValue(Component comp)
	{
		lastRenderedValues.put(comp, comp.getDefaultModelObject());
	}

	public Object getLastRenderedValue(Component comp)
	{
		return lastRenderedValues.get(comp);
	}

	/**
	 * @param obj
	 * @param dataProviderID
	 * @param prevValue
	 */
	public void setValue(Component component, String dataProviderID, Object obj)
	{
		boolean ownComponentsValue = dataProviderID.endsWith(getDataProviderID(component));
		Object prevValue = null;
		if (ownComponentsValue && component instanceof IResolveObject)
		{
			obj = ((IResolveObject)component).resolveRealValue(obj);
		}
		IRecordInternal record = (IRecordInternal)RecordItemModel.this.getObject();
		WebForm webForm = component.findParent(WebForm.class);
		FormScope fs = webForm.getController().getFormScope();
		try
		{

			if (dataProviderID.startsWith(ScriptVariable.GLOBAL_DOT_PREFIX))
			{
				if (record == null)
				{
					String restName = dataProviderID.substring(ScriptVariable.GLOBAL_DOT_PREFIX.length());
					GlobalScope gs = webForm.getController().getApplication().getScriptEngine().getSolutionScope().getGlobalScope();
					prevValue = gs.put(restName, obj);
				}
				else
				{
					//does an additional fire in foundset!
					prevValue = record.getParentFoundSet().setDataProviderValue(dataProviderID, obj);
				}
			}
			else if (fs.has(dataProviderID, fs))
			{
				prevValue = fs.get(dataProviderID);
				fs.put(dataProviderID, obj);
			}
			else
			{
				if (record != null && record.startEditing())
				{
					try
					{
						prevValue = record.getValue(dataProviderID);
						record.setValue(dataProviderID, obj);
					}
					catch (IllegalArgumentException e)
					{
						Debug.trace(e);
						((WebClientSession)Session.get()).getWebClient().handleException(null, new ApplicationException(ServoyException.INVALID_INPUT, e));
						Object stateValue = record.getValue(dataProviderID);
						if (!Utils.equalObjects(prevValue, stateValue))
						{
							// reset display to changed value in validator method
							obj = stateValue;
						}
						if (ownComponentsValue)
						{
							((IDisplayData)component).setValueValid(false, prevValue);
						}
						return;
					}

					if (ownComponentsValue && record instanceof FindState)
					{
						((FindState)record).setFormat(dataProviderID, ((IDisplayData)component).getFormat());
					}
				}
			}
			// this can be called on another dataprovider id then the component (media upload)
			// then dont call notify 
			if (ownComponentsValue)
			{
				((IDisplayData)component).notifyLastNewValueWasChange(prevValue, obj);
			}
		}
		finally
		{
			// this can be called on another dataprovider id then the component (media upload)
			// then touch the lastInvalidValue
			if (ownComponentsValue)
			{
				if (((IDisplayData)component).isValueValid())
				{
					lastInvalidValue = NONE;
				}
				else
				{
					lastInvalidValue = obj;
				}
			}
		}
		return;
	}

	/**
	 * Gets the value for dataProviderID in the context of this record and the form determined using given component. The component is only used for getting the
	 * form, otherwise it does not affect the returned value.
	 * 
	 * @param component the component used to determine the form (in case of global or form variables).
	 * @param dataProviderID the data provider id pointing to a data provider in the record, a form or a global variable.
	 * @return the value.
	 */
	public Object getValue(Component component, String dataProviderID)
	{
		Object value = null;
		WebForm webForm = component.findParent(WebForm.class);
		FormScope fs = webForm.getController().getFormScope();
		IRecord record = (IRecord)RecordItemModel.this.getObject();
		if (dataProviderID.startsWith(ScriptVariable.GLOBAL_DOT_PREFIX))
		{
			String restName = dataProviderID.substring(ScriptVariable.GLOBAL_DOT_PREFIX.length());
			GlobalScope gs = webForm.getController().getApplication().getScriptEngine().getSolutionScope().getGlobalScope();
			value = gs.get(restName);
		}
		else if (record != null)
		{
			value = record.getValue(dataProviderID);
		}

		if (value == Scriptable.NOT_FOUND && fs != null && fs.has(dataProviderID, fs)) // how can fs be null.
		{
			value = fs.get(dataProviderID);
		}

		if (value instanceof DbIdentValue)
		{
			value = ((DbIdentValue)value).getPkValue();
		}

		if (value == Scriptable.NOT_FOUND)
		{
			value = null;
		}

		return value;
	}

}
