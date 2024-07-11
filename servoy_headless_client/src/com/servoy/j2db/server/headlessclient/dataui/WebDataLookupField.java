/*
` This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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

import java.awt.Color;
import java.util.List;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.dataprocessing.CustomValueList;
import com.servoy.j2db.dataprocessing.IDisplayDependencyData;
import com.servoy.j2db.dataprocessing.IDisplayRelatedData;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.dataprocessing.LookupListChangeListener;
import com.servoy.j2db.dataprocessing.LookupListModel;
import com.servoy.j2db.dataprocessing.LookupValueList;
import com.servoy.j2db.dataprocessing.SortColumn;
import com.servoy.j2db.ui.scripting.RuntimeDataLookupField;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ScopesUtils;
import com.servoy.j2db.util.Utils;

/**
 * Represents the typeahead/lookup field in the browser.
 *
 * @author jcompagner
 */
public class WebDataLookupField extends WebDataField implements IDisplayRelatedData, IDisplayDependencyData
{

	private static final long serialVersionUID = 1L;
	private IRecordInternal parentState;
	private IRecordInternal relatedRecord;
	private LookupListModel dlm;
	protected LookupListChangeListener changeListener;

	public WebDataLookupField(IApplication application, RuntimeDataLookupField scriptable, String id, LookupValueList list)
	{
		super(application, scriptable, id, list);
		createLookupListModel(list);
	}

	public WebDataLookupField(IApplication application, RuntimeDataLookupField scriptable, String id, final String serverName, String tableName,
		String dataProviderID)
	{
		super(application, scriptable, id);
		dlm = new LookupListModel(application, serverName, tableName, dataProviderID);
	}

	public WebDataLookupField(IApplication application, RuntimeDataLookupField scriptable, String id, CustomValueList list)
	{
		super(application, scriptable, id, list);
		createCustomListModel(list);
	}

	protected void createCustomListModel(CustomValueList vList)
	{
		if (dlm != null && changeListener != null)
		{
			dlm.getValueList().removeListDataListener(changeListener);
		}
		dlm = new LookupListModel(application, vList);

		if (changeListener == null) changeListener = new LookupListChangeListener(this);
		vList.addListDataListener(changeListener);
	}

	protected void createLookupListModel(LookupValueList vlist)
	{
		if (dlm != null && changeListener != null)
		{
			dlm.getValueList().removeListDataListener(changeListener);
		}
		dlm = new LookupListModel(application, vlist);

		if (dlm.isShowValues() != dlm.isReturnValues())
		{
			try
			{
				if (changeListener == null) changeListener = new LookupListChangeListener(this);
				vlist.addListDataListener(changeListener);
			}
			catch (Exception e)
			{
				Debug.error("Error registering table listener for web lookup"); //$NON-NLS-1$
				Debug.error(e);
			}
		}
	}

	/**
	 * @see com.servoy.j2db.server.headlessclient.dataui.WebDataField#setValidationEnabled(boolean)
	 */
	@Override
	public void setValidationEnabled(boolean validation)
	{
		if (list != null && list.getFallbackValueList() != null)
		{
			IValueList vlist = list;
			if (!validation)
			{
				vlist = list.getFallbackValueList();
			}
			if (vlist instanceof CustomValueList)
			{
				createCustomListModel((CustomValueList)vlist);
			}
			else
			{
				createLookupListModel((LookupValueList)vlist);
			}
		}
		super.setValidationEnabled(validation);
	}


	@Override
	protected boolean needsFormatOnchange()
	{
		return true;
	}

	@Override
	public void setClientProperty(Object key, Object value)
	{
		if ((IApplication.TYPE_AHEAD_SHOW_POPUP_ON_FOCUS_GAIN.equals(key) || IApplication.TYPE_AHEAD_SHOW_POPUP_WHEN_EMPTY.equals(key)) &&
			!Utils.equalObjects(getScriptObject().getClientProperty(key), value))
		{
			getStylePropertyChanges().setChanged();
		}
		super.setClientProperty(key, value);
	}

	@Override
	public void setValueList(IValueList vl)
	{
		super.setValueList(vl);
		if (list instanceof CustomValueList)
		{
			createCustomListModel((CustomValueList)list);
		}
	}

	/**
	 * Maps a trimmed value entered by the user to a value stored in the value list. The value stored in the value list may contain some trailing spaces, while
	 * the value entered by the user is trimmed.
	 *
	 * @param value
	 * @return
	 */
	@SuppressWarnings("nls")
	public String mapTrimmedToNotTrimmed(String value)
	{
		// Although the value entered by the user should be trimmed, make sure it is so.
		String trimmed = value.trim();
		try
		{
			// this is the &nbsp character we set for empty value
			if ("\u00A0".equals(trimmed) || trimmed.length() == 0) return trimmed;
			// Grab all values that start with the value entered by the user.
			String result = matchValueListValue(trimmed, false);
			if (result == null)
			{
				if (changeListener != null) dlm.getValueList().removeListDataListener(changeListener);
				try
				{
					dlm.fill(parentState, getDataProviderID(), trimmed, false);
					result = matchValueListValue(trimmed, false);
					if (result == null && list.hasRealValues())
					{
						dlm.fill(parentState, getDataProviderID(), null, false);
						//if it doesn't have real values, just keep what is typed
						// now just try to match it be start with matching instead of equals:
						result = matchValueListValue(trimmed, true);
						if (result == null && !getEventExecutor().getValidationEnabled())
						{
							result = trimmed;
						}
					}
				}
				finally
				{
					if (changeListener != null) dlm.getValueList().addListDataListener(changeListener);
				}
			}
			// If no match was found then return back the value, otherwise return the found match.
			return result == null ? trimmed : result;
		}
		catch (Exception e)
		{
			Debug.error(e);
			return trimmed;
		}
	}

	public static enum LISTVALUE
	{
		NOVALUE
	}

	public Object getValueListRealValue(String displayValue)
	{
		for (int i = 0; i < dlm.getSize(); i++)
		{
			Object display = dlm.getElementAt(i);
			if ((displayValue != null && display != null && displayValue.equals(display.toString())) || (displayValue == null && display == null))
			{
				return dlm.getRealElementAt(i);
			}
		}
		return LISTVALUE.NOVALUE;
	}

	private String matchValueListValue(String trimmed, boolean startsWith)
	{
		int size = dlm.getSize();
		// Find a match in the value list.
		String result = null;
		if (startsWith) trimmed = trimmed.toLowerCase();
		for (int i = 0; i < size; i++)
		{
			String currentValue = dlm.getElementAt(i).toString();
			if (startsWith && currentValue.trim().toLowerCase().startsWith(trimmed))
			{
				result = currentValue;
				break;
			}
			else if (currentValue.trim().equals(trimmed))
			{
				result = currentValue;
				break;
			}
		}

		return result;
	}

	/**
	 * @see com.servoy.j2db.dataprocessing.IDisplayRelatedData#setFoundSet(com.servoy.j2db.dataprocessing.IRecordInternal,
	 *      com.servoy.j2db.dataprocessing.IFoundSetInternal, boolean)
	 */
	public void setRecord(IRecordInternal parentState, boolean stopEditing)
	{
		if (this.parentState == parentState) return;
		dependencyChanged(parentState);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.dataprocessing.IDisplayDependencyData#dependencyChanged(com.servoy.j2db.dataprocessing.IRecordInternal)
	 */
	@Override
	public void dependencyChanged(IRecordInternal record)
	{
		this.parentState = record;
		if (list != null)
		{
			int index = -1;
			if (!ScopesUtils.isVariableScope(getDataProviderID()) && (getDataProviderID() != null))
			{
				index = getDataProviderID().lastIndexOf('.');
			}
			if (index == -1 || parentState == null)
			{
				list.fill(parentState);
			}
			else
			{
				IFoundSetInternal relatedFoundSet = parentState.getRelatedFoundSet(getDataProviderID().substring(0, index));
				if (relatedFoundSet == null)
				{
					this.relatedRecord = parentState.getParentFoundSet().getPrototypeState();
					list.fill(relatedRecord);
				}
				else if (relatedFoundSet.getSize() == 0)
				{
					this.relatedRecord = relatedFoundSet.getPrototypeState();
					list.fill(relatedRecord);
				}
				else
				{
					IRecordInternal relRecord = relatedFoundSet.getRecord(relatedFoundSet.getSelectedIndex());
					if (relRecord != relatedRecord)
					{
						this.relatedRecord = relRecord;
						list.fill(relatedRecord);
					}
				}
			}
		}
	}

	public String getSelectedRelationName()
	{
		if (relationName == null && list != null)
		{
			relationName = list.getRelationName();
		}
		return relationName;
	}

	private String relationName = null;

	public String[] getAllRelationNames()
	{
		String selectedRelationName = getSelectedRelationName();
		if (selectedRelationName == null)
		{
			return new String[0];
		}
		else
		{
			return new String[] { selectedRelationName };
		}
	}

	/**
	 * @see com.servoy.j2db.dataprocessing.IDisplayRelatedData#getDefaultSort()
	 */
	public List<SortColumn> getDefaultSort()
	{
		return dlm.getDefaultSort();
	}

	public void notifyVisible(boolean b, List<Runnable> invokeLaterRunnables)
	{
		//ignore
	}

	/**
	 * @see com.servoy.j2db.dataprocessing.IDisplayRelatedData#destroy()
	 */
	@Override
	public void destroy()
	{
		if (dlm != null && changeListener != null)
		{
			dlm.getValueList().removeListDataListener(changeListener);
		}
		super.destroy();
		parentState = null;
		relatedRecord = null;
	}

	@Override
	public void setBackground(Color cbg)
	{
		listColor = cbg;
		super.setBackground(cbg);
	}

	private Color listColor = null;

	private Color getListColor()
	{
		return listColor;
	}

	public void setListColor(Color listColor)
	{
		this.listColor = listColor;
	}
}
