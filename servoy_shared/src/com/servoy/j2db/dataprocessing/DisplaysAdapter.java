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
package com.servoy.j2db.dataprocessing;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.mozilla.javascript.Scriptable;

import com.servoy.j2db.ApplicationException;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.ISmartClientApplication;
import com.servoy.j2db.component.ComponentFormat;
import com.servoy.j2db.component.INullableAware;
import com.servoy.j2db.dataprocessing.ValueFactory.DbIdentValue;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.scripting.GlobalScope;
import com.servoy.j2db.scripting.IScriptable;
import com.servoy.j2db.scripting.IScriptableProvider;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.ISupportOnRender;
import com.servoy.j2db.ui.ISupportOnRenderCallback;
import com.servoy.j2db.ui.scripting.AbstractRuntimeRendersupportComponent;
import com.servoy.j2db.ui.scripting.IFormatScriptComponent;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.IDestroyable;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.ScopesUtils;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.Utils;

/**
 * This adapter is a kind of model between the display(s) and the state.
 *
 * @author jblok
 */
public class DisplaysAdapter implements IDataAdapter, IEditListener, TableModelListener, IDestroyable, ListSelectionListener
{
	private boolean adjusting;

	//holds list of all displays
	private final List<IDisplayData> displays;

	//the state (=model) where to get/set the data
	private IRecordInternal record;
	private List<IRecordInternal> relatedData;// null when not related, list of currently listening records when related

	//representing dataprovider
	private final String dataProviderID;

	private final IApplication application;

	private final DataAdapterList dal;

	DisplaysAdapter(IApplication app, DataAdapterList dal, String dataProviderID, IDisplayData display)
	{
		application = app;
		this.dal = dal;
		displays = new ArrayList<IDisplayData>(1);//normally one, this is first display
		addDisplay(display);
		this.dataProviderID = dataProviderID;
		if (dataProviderID == null || ScopesUtils.isVariableScope(dataProviderID) || dataProviderID.indexOf('.') < 0)
		{
			relatedData = null; // not related
		}
		else
		{
			relatedData = Collections.emptyList();
		}
	}

	/**
	 * Push new state in this data adapter
	 *
	 * @param state the state to work with.
	 */
	public void setRecord(IRecordInternal state)
	{
		this.record = state;

		Object obj = null;
		if (dataProviderID != null)
		{
			Pair<String, String> scope = ScopesUtils.getVariableScope(dataProviderID);
			if (scope.getLeft() != null)
			{
				GlobalScope gs = application.getScriptEngine().getScopesScope().getGlobalScope(scope.getLeft());
				obj = gs == null ? null : gs.get(scope.getRight());
			}
			else if (relatedData != null)
			{
				if (state != null) obj = state.getValue(dataProviderID);
			}
			else if (dal.getFormScope() != null /* design component */ && dal.getFormScope().has(dataProviderID, dal.getFormScope()))
			{
				obj = dal.getFormScope().get(dataProviderID);
			}
			else if (state != null)
			{
				obj = state.getValue(dataProviderID);
			}

			// if display value is null but is for count/avg/sum aggregate set it to 0, as
			// it means that the foundset has no records, so count/avg/sum is 0;
			if (obj == null && dal.isCountOrAvgOrSumAggregateDataProvider(this)) obj = Integer.valueOf(0);
		}
		if (obj == Scriptable.NOT_FOUND)
		{
			obj = null;
		}
		setValueToDisplays(obj);
	}

	public void deregister()
	{
		if (relatedData != null)
		{
			for (IRecordInternal rec : relatedData)
			{
				rec.removeModificationListener(this);
				((ISwingFoundSet)rec.getParentFoundSet()).removeTableModelListener(this);
				((ISwingFoundSet)rec.getParentFoundSet()).getSelectionModel().removeListSelectionListener(this);

			}
		}
	}

	private void reregister()
	{
		if (record == null)
		{
			return;
		}

		// get the new records were are depending on
		IRecordInternal currRecord = record;
		String[] parts = dataProviderID.split("\\."); //$NON-NLS-1$

		// similar code as the loop below is also in class DataproviderTypeSabloValue - just in case future fixes need to apply to both places
		List<IRecordInternal> newRelated = new ArrayList<IRecordInternal>(parts.length - 1);
		for (int i = 0; currRecord != null && i < parts.length - 1; i++)
		{
			Object value = currRecord.getValue(parts[i]);
			if (value instanceof ISwingFoundSet)
			{
				currRecord = ((ISwingFoundSet)value).getRecord(((ISwingFoundSet)value).getSelectedIndex());
				if (currRecord == null) currRecord = ((ISwingFoundSet)value).getPrototypeState();
				newRelated.add(currRecord);
			}
			else
			{
				currRecord = null;
			}
		}

		if (!newRelated.equals(relatedData))
		{
			deregister();

			relatedData = newRelated;

			// register
			for (IRecordInternal rec : relatedData)
			{
				rec.addModificationListener(this);
				((ISwingFoundSet)rec.getParentFoundSet()).addTableModelListener(this);
				((ISwingFoundSet)rec.getParentFoundSet()).getSelectionModel().addListSelectionListener(this);
			}
		}
	}

	//inform all displays about a change
	private void setValueToDisplays(Object obj)
	{
		if (relatedData != null)
		{
			reregister();
		}
		Object val = obj;
		if (val instanceof DbIdentValue)
		{
			val = ((DbIdentValue)val).getPkValue();
		}

		for (IDisplayData display : displays)
		{
			Object value = null;
			if (display.needEntireState())
			{
				display.setTagResolver(dal);
				if (display.getDataProviderID() != null)
				{
					value = dal.getValueObject(record, display.getDataProviderID());
				}
			}
			else
			{
				value = val;
			}

			if (!findMode)
			{
				// use UI converter to convert from record value to UI value
				value = ComponentFormat.applyUIConverterToObject(display, value, dataProviderID, application.getFoundSetManager());
			}

			display.setValueObject(value);

			// when the data-provider for this check box is a non-null integer column,
			// we must force it to take the value 0 (so it can be saved in the database);
			// in some cases we do not have a editProvider (table view - renderer component)
			// and we use the explicitly set "record" to commit the changed value;
			// similar code exists for web client check boxes
			if (!findMode && value == null && display instanceof INullableAware && !((INullableAware)display).getAllowNull() &&
				display instanceof IFieldComponent && ((IFieldComponent)display).getScriptObject() instanceof IFormatScriptComponent &&
				((IFormatScriptComponent)((IFieldComponent)display).getScriptObject()).getComponentFormat() != null &&
				((IFormatScriptComponent)((IFieldComponent)display).getScriptObject()).getComponentFormat().dpType == IColumnTypes.INTEGER &&
				display.getDataProviderID() != null && record != null && record.startEditing() &&
				!(record instanceof PrototypeState && !ScopesUtils.isVariableScope(display.getDataProviderID()))) // ignore PrototypeState if not global
			{
				// NOTE: when a UI converter is defined, the converter should handle this
				if (((IFormatScriptComponent)((IFieldComponent)display).getScriptObject()).getComponentFormat().parsedFormat.getUIConverterName() == null)
				{
					record.setValue(display.getDataProviderID(), Integer.valueOf(0));
				}
			}
		}
	}

	/**
	 * focus listener implementation, notifies the state that is started editing
	 */
	public void startEdit(IDisplayData display)
	{
		if (record != null)
		{
			startEdit(dal, display, record);
		}
	}

	public static void startEdit(DataAdapterList dal, IDisplay display, IRecordInternal state)
	{
		final IApplication application = dal.getApplication();
		dal.setCurrentDisplay(display);
		boolean isGlobal = false;
		boolean isColumn = true;
		if (display instanceof IDisplayData)
		{
			String dataProviderID = ((IDisplayData)display).getDataProviderID();
			isGlobal = dataProviderID != null && ScopesUtils.isVariableScope(dataProviderID);

			if (!isGlobal && dataProviderID != null)
			{
				String[] parts = dataProviderID.split("\\."); //$NON-NLS-1$
				IRecordInternal currState = state;
				for (int i = 0; i < parts.length - 1; i++)
				{
					IFoundSetInternal foundset = currState.getRelatedFoundSet(parts[i]);
					if (foundset == null)
					{
						break;
					}
					Relation r = application.getFoundSetManager().getApplication().getFlattenedSolution().getRelation(parts[i]);
					currState = foundset.getRecord(foundset.getSelectedIndex());
					if (currState == null)
					{
						if (r != null && r.getAllowCreationRelatedRecords())
						{
							try
							{
								currState = foundset.getRecord(foundset.newRecord(0, true));
							}
							catch (ServoyException se)
							{
								application.reportError(se.getLocalizedMessage(), se);
							}
						}
						else
						{
							final ApplicationException ae = new ApplicationException(ServoyException.NO_RELATED_CREATE_ACCESS, new Object[] { parts[i] });
							// unfocus the current field, otherwise when the dialog is closed focus is set back to this field and the same error recurs ad infinitum.
							application.looseFocus();
							application.invokeLater(new Runnable()
							{
								public void run()
								{
									application.handleException(null, ae); // ApplicationException knows how to translate this null into an i18n message
								}
							});
						}
					}
					if (currState == null) return;
				}

				isColumn = currState.getParentFoundSet().getSQLSheet().getColumnIndex(parts[parts.length - 1]) != -1 ||
					currState.getParentFoundSet().getSQLSheet().containsCalculation(dataProviderID);
			}
		}
		if (isGlobal || !isColumn || state.startEditing()) //globals are always allowed to set in datarenderers
		{
			//bit ugly should use property event here
			if (application instanceof ISmartClientApplication) ((ISmartClientApplication)application).updateInsertModeIcon(display);
		}
		else
		{
			//loose focus first
			//don't transfer focus to menu bar.. (macosx)
			//application.getMainApplicationFrame().getJMenuBar().requestFocus();
			application.looseFocus();
			application.reportWarningInStatus(application.getI18NMessage("servoy.foundSet.error.noModifyAccess")); //$NON-NLS-1$
		}
	}

	/**
	 * focus listener implementation, set value to state and possible other displays, and notify other datalisteners about changes
	 */
	public void commitEdit(IDisplayData display)
	{
		if (dataProviderID == null) return;
		Object obj = Utils.removeJavascripLinkFromDisplay(display, null);
		Object prevValue = null;
		boolean valueWasConverted = false;
		if (!findMode)
		{
			// use UI converter to convert from UI value to record value
			Object converted = ComponentFormat.applyUIConverterFromObject(display, obj, dataProviderID, application.getFoundSetManager());
			valueWasConverted = obj != converted;
			obj = converted;
		}

		Pair<String, String> scope = ScopesUtils.getVariableScope(dataProviderID);
		if (scope.getLeft() != null)
		{
			adjusting = true;
			try
			{
				if (record == null)
				{
					prevValue = application.getScriptEngine().getScopesScope().getGlobalScope(scope.getLeft()).put(scope.getRight(), obj);
				}
				else
				{
					//does an additional fire in foundset!
					prevValue = record.getParentFoundSet().setDataProviderValue(dataProviderID, obj);
				}
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
			finally
			{
				adjusting = false;
			}
		}
		else if (dal.getFormScope() != null && dal.getFormScope().has(dataProviderID, dal.getFormScope()))
		{
			prevValue = dal.getFormScope().get(dataProviderID);
			dal.getFormScope().put(dataProviderID, obj);
		}
		else if (record != null && (record.isEditing() || record.getParentFoundSet().getSQLSheet().getColumnIndex(dataProviderID) == -1))
		{
			// If object == "" and previous == null don't update value
			if (obj != null && obj.equals("")) //$NON-NLS-1$
			{
				if (record.getValue(dataProviderID) == null)
				{
					return;
				}
			}
			try
			{
				adjusting = true;
				prevValue = record.getValue(dataProviderID);
				record.setValue(dataProviderID, obj);
			}
			catch (IllegalArgumentException e)
			{
				Debug.trace(e);
				application.handleException(null, new ApplicationException(ServoyException.INVALID_INPUT, e));
				Object stateValue = record.getValue(dataProviderID);
				if (Utils.equalObjects(prevValue, stateValue))
				{
					// reset display to typed value
					setValueToDisplays(obj);
				}
				else
				{
					// reset display to changed value in validator method
					setValueToDisplays(stateValue);
				}
				display.setValueValid(false, prevValue);
				return;
			}
			finally
			{
				adjusting = false;
			}

			if (record instanceof FindState)
			{
				if (display instanceof IScriptableProvider && ((IScriptableProvider)display).getScriptObject() instanceof IFormatScriptComponent &&
					((IFormatScriptComponent)((IScriptableProvider)display).getScriptObject()).getComponentFormat() != null)
				{
					((FindState)record).setFormat(dataProviderID,
						((IFormatScriptComponent)((IScriptableProvider)display).getScriptObject()).getComponentFormat().parsedFormat);
				}

				// findstate doesn't inform others...
				if (!Utils.equalObjects(prevValue, obj))
				{
					// do call notifyLastNewValue changed so that the onChangeEvent will be fired and called when attached.
					display.notifyLastNewValueWasChange(prevValue, obj);//to trigger onChangeMethod (not all displays have own property change impl)
				}
				prevValue = obj;
			}
		}

		boolean changed = !Utils.equalObjects(prevValue, obj);
		if (!changed)
		{
			// value was changed back to original value manually after an invalid input
			display.setValueValid(true, null);
		}
		if (changed || valueWasConverted)
		{
			adjusting = true;
			try
			{
				// fireDataChange to possible listeners
				if (changed)
				{
					fireModificationEvent(obj);
					display.notifyLastNewValueWasChange(prevValue, obj);//to trigger onChangeMethod (not all displays have own property change impl)
				}

				// check if the DataAdapterList is now destroyed (because of recreateUI in the onchange method)
				// ignore the rest if it is destroyed.
				if (!dal.isDestroyed())
				{
					// onDataChange(==notifyLastNewValueWasChange) call can have changed the value
					if (dal.getFormScope() != null && dal.getFormScope().has(dataProviderID, dal.getFormScope()))
					{
						obj = dal.getFormScope().get(dataProviderID);
					}
					else if (record != null)
					{
						obj = record.getValue(dataProviderID);
					}
					setValueToDisplays(obj);// we also want to reset the value in the current display if changed by script
				}
			}
			finally
			{
				adjusting = false;
			}
		}
	}

	private boolean findMode = false;

	public void setFindMode(boolean b)
	{
		findMode = b;
		for (IDisplayData display : displays)
		{
			// skip form variables
			if (dal.getFormScope() == null || display.getDataProviderID() == null ||
				dal.getFormScope().get(display.getDataProviderID()) == Scriptable.NOT_FOUND)
			{
				display.setValidationEnabled(!b);
			}
		}
	}

	/*
	 * _____________________________________________________________ DataListener
	 */
	private final List<IDataAdapter> listeners = new ArrayList<IDataAdapter>(3);

	public void addDataListener(IDataAdapter l)
	{
		if (!listeners.contains(l) && l != this) listeners.add(l);
	}

	public void removeDataListener(IDataAdapter listener)
	{
		listeners.remove(listener);
	}

	private void fireModificationEvent(Object value)
	{
		ModificationEvent e = null;
		if (listeners != null && listeners.size() != 0)
		{
			Iterator<IDataAdapter> it = listeners.iterator();
			while (it.hasNext())
			{
				if (e == null) e = new ModificationEvent(dataProviderID, value, record);
				IDataAdapter listener = it.next();
				listener.displayValueChanged(e);
			}
		}
	}

	/**
	 * Add a display to this adapter
	 *
	 * @param display the display to add
	 */
	public void addDisplay(IDisplayData display)
	{
		displays.add(display);
	}

	public String getDataProviderID()
	{
		return dataProviderID;
	}

	public void displayValueChanged(ModificationEvent event)
	{
		valueChanged(event);
	}

	/*
	 * _____________________________________________________________ JavaScriptModificationlistener
	 */
	public void valueChanged(ModificationEvent e)
	{
		if (!adjusting)
		{
			try
			{
				adjusting = true;
				Object obj = null;
				boolean formVariable = false;
				if (dataProviderID != null)
				{
					if (dal.getFormScope().has(dataProviderID, dal.getFormScope()))
					{
						formVariable = true;
						obj = dal.getFormScope().get(dataProviderID);
					}
					else if (record != null)
					{
						obj = record.getValue(dataProviderID);
						if (obj == Scriptable.NOT_FOUND)
						{
							obj = null;
						}
						// have to do this because for calcs in calcs. Better was to had a check for previous value.
						fireModificationEvent(obj);
					}
				}
				// do not set value for form variable except when really changed
				if (!formVariable || dataProviderID == null || dataProviderID.equals(e.getName()))
				{
					setValueToDisplays(obj);
				}
			}
			finally
			{
				adjusting = false;
			}
		}
		// do fire on render on all components for record change
		for (IDisplayData displayData : displays)
		{
			if (displayData instanceof ISupportOnRender && displayData instanceof IScriptableProvider)
			{
				IScriptable so = ((IScriptableProvider)displayData).getScriptObject();
				if (so instanceof AbstractRuntimeRendersupportComponent && ((ISupportOnRenderCallback)so).getRenderEventExecutor().hasRenderCallback())
				{
					String componentDataproviderID = ((AbstractRuntimeRendersupportComponent)so).getDataProviderID();
					if (e.getRecord() != null || (e.getName() != null && e.getName().equals(componentDataproviderID)))
					{
						((ISupportOnRender)displayData).fireOnRender(true);
					}
				}
			}
		}
	}

	@Override
	public String toString()
	{
		return "DisplaysAdapter " + dataProviderID + " with " + displays.size() + " displays,  hash " + hashCode(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public void tableChanged(TableModelEvent e)
	{
		ISwingFoundSet source = (ISwingFoundSet)e.getSource();
		if (record != null && relatedData != null && e.getFirstRow() >= source.getSelectedIndex() && source.getSelectedIndex() <= e.getLastRow())
		{
			setValueToDisplays(record.getValue(dataProviderID));
		}
	}

	public void destroy()
	{
		deregister();
	}

	public void valueChanged(ListSelectionEvent e)
	{
		setValueToDisplays(record.getValue(dataProviderID));
	}
}