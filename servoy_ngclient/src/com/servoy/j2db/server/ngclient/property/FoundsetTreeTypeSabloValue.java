/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2023 Servoy BV

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.mozilla.javascript.NativeArray;
import org.sablo.IChangeListener;
import org.sablo.IWebObjectContext;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.specification.property.ISmartPropertyValue;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.IFoundSetManagerInternal;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.ISwingFoundSet;
import com.servoy.j2db.dataprocessing.RelatedFoundSet;
import com.servoy.j2db.dataprocessing.SortColumn;
import com.servoy.j2db.persistence.IRelation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.server.ngclient.IContextProvider;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;

/**
 * @author lvostinar
 *
 */
public class FoundsetTreeTypeSabloValue implements ISmartPropertyValue, TableModelListener
{
	private final String ID_KEY = "id";
	private final String VALUE_KEY = "value";
	private final String HANDLED = "handledID";
	private final String CHILDREN = "getChildren";
	private final String UPDATESELECTION = "updateSelection";
	private final String UPDATECHECKBOX = "updateCheckboxValue";
	private final String LEVEL = "level";

	private Long handledIDForResponse;
	private String parentID;
	private int parentLevel;
	private Map<String, Boolean> newCheckedValues = null;
	private boolean initialized = false;

	private boolean autorefresh = true;
	public ArrayList<IFoundSetInternal> roots = new ArrayList<IFoundSetInternal>();
	public Map<String, FoundsetTreeBinding> bindings = new HashMap<String, FoundsetTreeBinding>();
	private Object[] selectionPath;
	private IChangeListener changeMonitor;
	private int levelVisible = 0;
	private boolean levelVisibility;
	private IFoundSetManagerInternal fsm;

	@Override
	public void attachToBaseObject(IChangeListener changeMonitor, IWebObjectContext webObjectContext)
	{
		this.changeMonitor = changeMonitor;
		this.autorefresh = Utils.getAsBoolean(webObjectContext.getProperty("autoRefresh"));
		this.fsm = ((IContextProvider)webObjectContext.getUnderlyingWebObject()).getDataConverterContext().getApplication().getFoundSetManager();
	}

	@Override
	public void detach()
	{
		this.changeMonitor = null;
		bindings.values().stream().forEach(binding -> {
			binding.foundsets.stream().filter(ISwingFoundSet.class::isInstance).map(ISwingFoundSet.class::cast).forEach(foundset -> {
				foundset.removeTableModelListener(this);
			});
		});
		this.bindings.clear();
		this.roots.clear();
	}

	public void flagChanged()
	{
		this.changeMonitor.valueChanged();
	}

	public void toJSON(JSONWriter writer, String key, IBrowserConverterContext dataConverterContext) throws IllegalArgumentException, JSONException
	{
		this.initialized = true;
		if (key != null) writer.key(key);
		if (this.parentID != null)
		{
			writer.object();
			if (handledIDForResponse != null)
			{
				writer.key(HANDLED);
				writer.object().key(ID_KEY).value(handledIDForResponse.longValue()).key(VALUE_KEY).value(true).endObject();
			}
			writer.key(CHILDREN);
			String[] foundsetAndRecordPK = this.parentID.split("_");
			List<Map<String, Object>> relChildren = null;
			if (foundsetAndRecordPK.length == 2)
			{
				IFoundSetInternal foundset = fsm.findFoundset(Utils.getAsInteger(foundsetAndRecordPK[0]));
				if (foundset != null)
				{
					FoundsetTreeBinding relatedBinding = bindings.get(foundset.getDataSource());
					if (relatedBinding.relationInfos.size() > 0 && foundset.getSize() > 0)
					{
						int recordIndex = foundset.getRecordIndex(foundsetAndRecordPK[1], 0);
						if (recordIndex != -1)
						{
							IRecordInternal record = foundset.getRecord(recordIndex);
							if (record != null)
							{
								relChildren = getRelatedFoundsetData(this.getRelatedFoundsets(relatedBinding, record, true),
									this.parentLevel);
							}
						}
					}
				}
			}
			if (relChildren == null)
			{
				relChildren = new ArrayList<>();
			}
			if (handledIDForResponse == null) writer.object().key(this.parentID);
			JSONUtils.toBrowserJSONFullValue(writer, null, relChildren, null, dataConverterContext);
			if (handledIDForResponse == null) writer.endObject();
			writer.endObject();

			this.parentID = null;
			this.handledIDForResponse = null;
			this.parentLevel = -1;
		}
		else if (this.newCheckedValues != null)
		{
			writer.object();
			JSONUtils.toBrowserJSONFullValue(writer, "newCheckedValues", this.newCheckedValues, null, dataConverterContext);
			writer.endObject();
			this.newCheckedValues = null;
		}
		else
		{
			List<Map<String, Object>> newJavaValueForJSON = getJavaValueForJSON();
			JSONUtils.toBrowserJSONFullValue(writer, null, newJavaValueForJSON, null, dataConverterContext);
		}
	}

	public void fromJSON(JSONObject newJSONValue)
	{
		if (newJSONValue.has(CHILDREN))
		{
			this.handledIDForResponse = Long.valueOf(newJSONValue.getLong(ID_KEY));
			this.parentID = newJSONValue.optString(CHILDREN);
			this.parentLevel = newJSONValue.optInt(LEVEL);
			this.changeMonitor.valueChanged();
		}
		else if (newJSONValue.has(UPDATESELECTION))
		{
			JSONArray ids = newJSONValue.optJSONArray(UPDATESELECTION);
			List<String> newids = new ArrayList<String>();
			if (ids != null)
			{
				for (int i = 0; i < ids.length(); i++)
				{
					String pkhash = ids.getString(i).split("_")[1];
					newids.add(pkhash.substring(pkhash.indexOf('.') + 1, pkhash.length() - 1));
				}
			}
			this.selectionPath = newids.toArray();
		}
		else if (newJSONValue.has(UPDATECHECKBOX))
		{
			String id = newJSONValue.optString(UPDATECHECKBOX);
			boolean value = newJSONValue.optBoolean("value");
			String[] foundsetAndRecordPK = id.split("_");
			if (foundsetAndRecordPK.length == 2)
			{
				IFoundSetInternal foundset = fsm.findFoundset(Utils.getAsInteger(foundsetAndRecordPK[0]));
				if (foundset != null)
				{
					FoundsetTreeBinding binding = bindings.get(foundset.getDataSource());
					if (foundset.getSize() > 0)
					{
						int recordIndex = foundset.getRecordIndex(foundsetAndRecordPK[1], 0);
						if (recordIndex != -1)
						{
							IRecordInternal record = foundset.getRecord(recordIndex);
							if (binding.checkboxvaluedataprovider != null && record != null && record.startEditing())
							{
								((ISwingFoundSet)record.getParentFoundSet()).removeTableModelListener(this);
								record.setValue(binding.checkboxvaluedataprovider, value);
								((ISwingFoundSet)record.getParentFoundSet()).addTableModelListener(this);
							}
							else
							{
								List<Object> checkedValues = binding.checkboxValues != null ? new ArrayList(Arrays.asList(binding.checkboxValues))
									: new ArrayList<>();
								if (value)
								{
									if (!checkedValues.stream().anyMatch(item -> Utils.equalObjects(item, record.getPK()[0])))
									{
										checkedValues.add(record.getPK()[0]);
									}
								}
								else
								{
									checkedValues.removeIf(item -> Utils.equalObjects(item, record.getPK()[0]));
								}
								binding.checkboxValues = checkedValues.toArray();
							}
						}
					}
				}
			}
		}
	}

	private List<Map<String, Object>> getJavaValueForJSON() // TODO this should return TypedData<List<Map<String, Object>>> instead
	{
		List<Map<String, Object>> newJavaValueForJSON = new ArrayList<Map<String, Object>>();
		for (IFoundSetInternal root : roots)
		{
			FoundsetTreeBinding binding = bindings.get(root.getDataSource());
			// load all records
			root.getRecord(5000);
			for (int i = 0; i < root.getSize(); i++)
			{
				Map<String, Object> recordData = new HashMap<String, Object>();
				IRecordInternal record = root.getRecord(i);
				recordData.put("id", root.getID() + "_" + record.getPKHashKey());
				recordData.put("name", record.getValue(binding.textdataprovider));
				recordData.put("datasource", root.getDataSource());
				recordData.put("level", 1);
				boolean hasCheckbox = false;
				if (binding.checkboxvaluedataprovider != null)
				{
					hasCheckbox = Utils.getAsBoolean(record.getValue(binding.hascheckboxdataprovider));
				}
				else if (binding.hasCheckboxValue != null)
				{
					hasCheckbox = this.containsValue(binding.hasCheckboxValue, record.getPK()[0]);
				}
				else
				{
					hasCheckbox = binding.checkboxValues != null;
				}
				recordData.put("hascheckbox", hasCheckbox);
				recordData.put("checkboxautoselectschildren", binding.checkboxAutoselectsChildren);
				if (hasCheckbox)
				{
					boolean checkboxValue = false;
					if (binding.checkboxvaluedataprovider != null)
					{
						checkboxValue = Utils.getAsBoolean(record.getValue(binding.checkboxvaluedataprovider));
					}
					else if (binding.checkboxValues != null)
					{
						checkboxValue = this.containsValue(binding.checkboxValues, record.getPK()[0]);
					}
					recordData.put("checked", checkboxValue);
				}
				if (binding.dataproviders != null)
				{
					for (String propertyid : binding.dataproviders.keySet())
					{
						recordData.put(propertyid, record.getValue(binding.dataproviders.get(propertyid)));
					}
				}
				newJavaValueForJSON.add(recordData);
				ArrayList<IFoundSetInternal> relatedFoundsets = getRelatedFoundsets(binding, record, false);
				if (relatedFoundsets.size() > 0)
				{
					if (this.levelVisibility && this.levelVisible >= 1)
					{
						List<Map<String, Object>> relChildren = getRelatedFoundsetData(relatedFoundsets, 1);
						if (relChildren.size() > 0)
						{
							recordData.put("hasChildren", true);
							recordData.put("children", relChildren);
						}
						else
						{
							recordData.put("hasChildren", false);
						}
					}
					else
					{
						recordData.put("hasChildren", false);
						for (IFoundSetInternal relFoundset : relatedFoundsets)
						{
							if (relFoundset.getSize() > 0)
							{
								recordData.put("hasChildren", true);
								break;
							}
						}
					}
				}
				else
				{
					recordData.put("hasChildren", false);
				}
				if (this.selectionPath != null && this.selectionPath.length == 1 &&
					Utils.equalObjects(this.selectionPath[0], record.getPK()[0]))
				{
					recordData.put("active", true);
				}
			}
			this.addFoundsetToBinding(root, binding);
		}
		return newJavaValueForJSON;
	}

	private ArrayList<IFoundSetInternal> getRelatedFoundsets(FoundsetTreeBinding binding, IRecordInternal record, boolean addChangeListener)
	{
		ArrayList<IFoundSetInternal> relatedFoundsets = new ArrayList<IFoundSetInternal>();
		if (binding.relationInfos.size() > 0)
		{
			for (String relationName : binding.relationInfos.keySet())
			{
				List<SortColumn> sortColumns = null;
				if (binding.childsortdataprovider != null)
				{
					IRelation relation = fsm.getRelation(relationName);
					if (relation != null)
					{
						Object sortString = record.getValue(binding.childsortdataprovider);
						try
						{
							sortColumns = fsm.getSortColumns(fsm.getTable(relation.getForeignDataSource()),
								sortString != null ? sortString.toString() : null);
						}
						catch (RepositoryException e)
						{
							Debug.error(e);
						}
					}
				}
				IFoundSetInternal relFoundset = record.getRelatedFoundSet(relationName, sortColumns);
				if (relFoundset != null)
				{
					relatedFoundsets.add(relFoundset);
					if (addChangeListener) this.addFoundsetToBinding(relFoundset, binding);
				}
			}
		}
		return relatedFoundsets;
	}

	private List<Map<String, Object>> getRelatedFoundsetData(ArrayList<IFoundSetInternal> relatedFoundsets, int parentlevel)
	{
		List<Map<String, Object>> relChildren = new ArrayList<Map<String, Object>>();
		if (relatedFoundsets != null)
		{
			for (IFoundSetInternal relatedFoundset : relatedFoundsets)
			{
				FoundsetTreeBinding relatedBinding = bindings.get(relatedFoundset.getDataSource());
				for (int j = 0; j < relatedFoundset.getSize(); j++)
				{
					Map<String, Object> relRecordData = new HashMap<String, Object>();
					IRecordInternal relRecord = relatedFoundset.getRecord(j);
					relRecordData.put("id", relatedFoundset.getID() + "_" + relRecord.getPKHashKey());
					relRecordData.put("name", relRecord.getValue(relatedBinding.textdataprovider));
					relRecordData.put("datasource", relatedFoundset.getDataSource());
					relRecordData.put("level", parentlevel + 1);
					boolean relhasCheckbox = false;
					if (relatedBinding.checkboxvaluedataprovider != null)
					{
						relhasCheckbox = Utils.getAsBoolean(relRecord.getValue(relatedBinding.hascheckboxdataprovider));
					}
					else if (relatedBinding.hasCheckboxValue != null)
					{
						relhasCheckbox = this.containsValue(relatedBinding.hasCheckboxValue, relRecord.getPK()[0]);
					}
					else
					{
						relhasCheckbox = relatedBinding.checkboxValues != null;
					}
					relRecordData.put("hascheckbox", relhasCheckbox);
					relRecordData.put("checkboxautoselectschildren", relatedBinding.checkboxAutoselectsChildren);
					if (relhasCheckbox)
					{
						boolean checkboxValue = false;
						if (relatedBinding.checkboxvaluedataprovider != null)
						{
							checkboxValue = Utils.getAsBoolean(relRecord.getValue(relatedBinding.checkboxvaluedataprovider));
						}
						else if (relatedBinding.checkboxValues != null)
						{
							checkboxValue = this.containsValue(relatedBinding.checkboxValues, relRecord.getPK()[0]);
						}
						relRecordData.put("checked", checkboxValue);
					}
					if (relatedBinding.dataproviders != null)
					{
						for (String propertyid : relatedBinding.dataproviders.keySet())
						{
							relRecordData.put(propertyid, relRecord.getValue(relatedBinding.dataproviders.get(propertyid)));
						}
					}
					relRecordData.put("hasChildren", false);
					if (this.selectionPath != null && this.selectionPath.length == parentlevel + 1 &&
						Utils.equalObjects(this.selectionPath[this.selectionPath.length - 1], relRecord.getPK()[0]))
					{
						relRecordData.put("active", true);
					}
					if (relatedBinding.relationInfos.size() > 0)
					{

						ArrayList<IFoundSetInternal> childRelatedFoundsets = getRelatedFoundsets(relatedBinding, relRecord, false);
						if (childRelatedFoundsets.size() > 0)
						{
							if (this.levelVisibility && this.levelVisible >= parentlevel + 1)
							{
								List<Map<String, Object>> innerRelChildren = getRelatedFoundsetData(childRelatedFoundsets, parentlevel + 1);
								if (relChildren.size() > 0)
								{
									relRecordData.put("hasChildren", true);
									relRecordData.put("children", innerRelChildren);
								}
								else
								{
									relRecordData.put("hasChildren", false);
								}
							}
							else
							{
								for (IFoundSetInternal childFoundset : childRelatedFoundsets)
								{
									if (childFoundset.getSize() > 0)
									{
										relRecordData.put("hasChildren", true);
										break;
									}
								}
							}
						}

					}
					relChildren.add(relRecordData);
				}
			}
		}
		return relChildren;
	}

	/**
	 * @param selectionPath the selectionPath to set
	 */
	public void setSelectionPath(Object[] selectionPath)
	{
		this.selectionPath = selectionPath;
	}

	/**
	 * @return the selectionPath
	 */
	public Object[] getSelectionPath()
	{
		return selectionPath;
	}

	public NativeArray getNodeIDArray(Object[] pkarray)
	{
		List<String> nodes = new ArrayList<String>();
		for (IFoundSetInternal root : roots)
		{
			if (addNodeID(root, nodes, pkarray, 0))
			{
				break;
			}
		}
		return new NativeArray(nodes.toArray(new String[0]));
	}

	public List<Object> getCheckedPks(String datasource)
	{
		List<Object> checkedPks = new ArrayList<Object>();
		FoundsetTreeBinding binding = bindings.get(datasource);
		if (binding != null)
		{
			if (binding.checkboxvaluedataprovider != null)
			{
				for (IFoundSetInternal foundset : binding.foundsets)
				{
					for (int i = 0; i < foundset.getSize(); i++)
					{
						IRecordInternal record = foundset.getRecord(i);
						if (Utils.getAsBoolean(record.getValue(binding.checkboxvaluedataprovider)) && !checkedPks.contains(record.getPK()[0]))
						{
							checkedPks.add(record.getPK()[0]);
						}
					}
				}
			}
			else if (binding.checkboxValues != null)
			{
				checkedPks.addAll(Arrays.asList(binding.checkboxValues));
			}
		}
		return checkedPks;
	}

	private boolean addNodeID(IFoundSetInternal foundset, List<String> nodeids, Object[] pkarray, int index)
	{
		IRecordInternal record = null;
		if (pkarray != null && index < pkarray.length)
		{
			record = foundset.getRecord(new Object[] { pkarray[index] });
		}
		if (record != null)
		{
			nodeids.add(foundset.getID() + "_" + record.getPKHashKey());
			if (index < pkarray.length - 1)
			{
				FoundsetTreeBinding binding = bindings.get(foundset.getDataSource());
				ArrayList<IFoundSetInternal> relatedFoundsets = getRelatedFoundsets(binding, record, true);
				if (relatedFoundsets.size() > 0)
				{
					for (IFoundSetInternal relfoundset : relatedFoundsets)
					{
						boolean added = addNodeID(relfoundset, nodeids, pkarray, index + 1);
						if (added) return true;
					}
				}
			}
			else
			{
				return true;
			}
		}
		else
		{
			nodeids.clear();
			return false;
		}
		return false;
	}

	private void addFoundsetToBinding(IFoundSetInternal foundset, FoundsetTreeBinding binding)
	{
		if (this.autorefresh && !binding.foundsets.contains(foundset))
		{
			binding.foundsets.add(foundset);
			if (foundset instanceof ISwingFoundSet)
			{
				((ISwingFoundSet)foundset).addTableModelListener(this);
			}
		}
	}

	/**
	 * @param string
	 * @param objects
	 * @param boolean1
	 */
	public void updateCheckBoxValues(String datasource, Object[] pks, Boolean checked)
	{
		if (this.initialized)
		{
			if (this.newCheckedValues == null)
			{
				this.newCheckedValues = new HashMap<>();
			}
		}
		FoundsetTreeBinding binding = bindings.get(datasource);
		if (binding != null)
		{
			List<Object> values = new ArrayList(Arrays.asList(binding.checkboxValues));
			if (checked)
			{
				for (Object pk : pks)
				{
					if (!values.contains(pk))
					{
						values.add(pk);
					}
				}
			}
			else
			{
				values.removeAll(Arrays.asList(pks));
			}
			binding.checkboxValues = values.toArray();
			if (this.newCheckedValues != null)
			{
				for (Object pk : pks)
				{
					for (IFoundSetInternal foundset : binding.foundsets)
					{
						IRecordInternal record = foundset.getRecord(new Object[] { pk });
						if (record != null)
						{
							this.newCheckedValues.put(foundset.getID() + "_" + record.getPKHashKey(), checked);
							break;
						}
					}
				}
			}
		}
		if (this.initialized && this.newCheckedValues != null && this.newCheckedValues.size() > 0)
		{
			this.changeMonitor.valueChanged();
		}
		else
		{
			this.newCheckedValues = null;
		}
	}

	@Override
	public void tableChanged(TableModelEvent e)
	{
		if (this.autorefresh && this.initialized)
		{
			IFoundSetInternal foundset = (IFoundSetInternal)e.getSource();
			if (foundset instanceof RelatedFoundSet)
			{
				List<IRecordInternal> parentRecords = ((RelatedFoundSet)foundset).getParents();
				if (parentRecords != null && parentRecords.size() > 0)
				{
					this.parentID = parentRecords.get(0).getParentFoundSet().getID() + "_" + parentRecords.get(0).getPKHashKey();
					this.changeMonitor.valueChanged();
				}
			}
			else
			{
				// a root foundset is changed, just send everything again for now
				this.changeMonitor.valueChanged();
			}
		}
	}

	private boolean containsValue(Object[] array, Object value)
	{
		return Arrays.stream(array).anyMatch(item -> Utils.equalObjects(item, value));
	}

	public static class FoundsetTreeBinding
	{
		public String textdataprovider;
		public Map<String, String> relationInfos = new HashMap<String, String>();
		public Map<String, String> dataproviders = new HashMap<String, String>();
		public String hascheckboxdataprovider;
		public String checkboxvaluedataprovider;
		public String childsortdataprovider;
		public Object[] hasCheckboxValue;
		public Object[] checkboxValues;
		public boolean checkboxAutoselectsChildren = true;
		// a cache for all foundsets loaded via this binding
		public ArrayList<IFoundSetInternal> foundsets = new ArrayList<IFoundSetInternal>();

	}

	/**
	 * @param number
	 * @param boolean1
	 */
	public void setLevelVisibility(Number level, Boolean visibility)
	{
		this.levelVisible = level.intValue();
		this.levelVisibility = visibility;
		this.changeMonitor.valueChanged();
	}

}

