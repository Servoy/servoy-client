package com.servoy.j2db.server.ngclient;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import com.servoy.base.persistence.constants.IColumnTypeConstants;
import com.servoy.base.util.ITagResolver;
import com.servoy.j2db.IFormController;
import com.servoy.j2db.dataprocessing.IDataAdapter;
import com.servoy.j2db.dataprocessing.IModificationListener;
import com.servoy.j2db.dataprocessing.IRecord;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.dataprocessing.ModificationEvent;
import com.servoy.j2db.dataprocessing.TagResolver;
import com.servoy.j2db.persistence.AggregateVariable;
import com.servoy.j2db.persistence.ColumnWrapper;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IDataProviderLookup;
import com.servoy.j2db.query.QueryAggregate;
import com.servoy.j2db.server.ngclient.component.EventExecutor;
import com.servoy.j2db.server.ngclient.component.WebComponentApiDefinition;
import com.servoy.j2db.server.ngclient.property.PropertyDescription;
import com.servoy.j2db.server.ngclient.property.PropertyType.DataproviderConfig;
import com.servoy.j2db.server.ngclient.utils.JSONUtils;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.ScopesUtils;
import com.servoy.j2db.util.Text;


public class DataAdapterList implements IModificationListener, ITagResolver, IDataAdapterList
{
	/**componentPropertiesWithTagExpression : keeps track of properties with tags from a bean (properties of type 'tagstring') */
	private final Map<WebComponent, List<String>> componentPropertiesWithTagExpression = new HashMap<>();
	private final Map<String, List<Pair<WebComponent, String>>> recordDataproviderToComponent = new HashMap<>();
	private final Map<FormElement, Map<String, String>> beanToDataHolder = new HashMap<>();
	private final INGApplication application;
	private final IFormController formController;
	private final EventExecutor executor;

	private IRecordInternal record;
	private boolean findMode;

	public DataAdapterList(INGApplication application, IFormController formController)
	{
		this.application = application;
		this.formController = formController;
		this.executor = new EventExecutor(application, formController);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.server.ngclient.IDataAdapterList#execute(com.servoy.j2db.server.ngclient.WebComponent, java.lang.String, java.lang.Object)
	 */
	@Override
	public Object execute(WebComponent webComponent, String event, int eventId, Object[] args)
	{
		return executor.execute(webComponent, event, eventId, args);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.server.ngclient.IDataAdapterList#executeApi(com.servoy.j2db.server.ngclient.component.WebComponentApiDefinition, java.lang.String,
	 * java.lang.Object[])
	 */
	@Override
	public Object executeApi(WebComponentApiDefinition apiDefinition, String elementName, Object[] args)
	{
		// TODO will by name be always enough, what happens exactly when we are in a tableview so having multiply of the same name..
		return application.getActiveWebSocketClientEndpoint().executeApi(apiDefinition, formController.getName(), elementName, args);
	}

	public void add(WebComponent component, Map<String, String> hm)
	{
		for (Entry<String, String> entry : hm.entrySet())
		{
			add(component, entry.getKey(), entry.getValue());
		}
	}

	public void add(WebComponent component, String recordDataProvider, String beanDataProvider)
	{
		List<Pair<WebComponent, String>> list = recordDataproviderToComponent.get(recordDataProvider);
		if (list == null)
		{
			list = new ArrayList<Pair<WebComponent, String>>();
			recordDataproviderToComponent.put(recordDataProvider, list);
		}
		list.add(new Pair<WebComponent, String>(component, beanDataProvider));

		Map<String, String> map = beanToDataHolder.get(component.getFormElement());
		if (map == null)
		{
			map = new HashMap<>();
			beanToDataHolder.put(component.getFormElement(), map);
		}
		map.put(beanDataProvider, recordDataProvider);
	}

	public void addTaggedProperty(WebComponent component, String beanTaggedProperty)
	{
		List<String> props = componentPropertiesWithTagExpression.get(component);
		if (props == null)
		{
			props = new ArrayList<>();
			componentPropertiesWithTagExpression.put(component, props);
		}
		props.add(beanTaggedProperty);
	}

	public void setRecord(IRecord record, boolean fireChangeEvent)
	{
		if (this.record != null)
		{
			this.record.removeModificationListener(this);
		}
		this.record = (IRecordInternal)record;
		if (this.record != null)
		{
			this.record.addModificationListener(this);
			pushRecordValues(fireChangeEvent, false);
		}
	}

	/**
	 * @return the record
	 */
	public IRecordInternal getRecord()
	{
		return record;
	}

	/**
	 * 
	 */
	private void pushRecordValues(boolean fireChangeEvent, boolean fireOnDataChange)
	{
		boolean changed = false;
		for (Entry<String, List<Pair<WebComponent, String>>> entry : recordDataproviderToComponent.entrySet())
		{
			Object value = com.servoy.j2db.dataprocessing.DataAdapterList.getValueObject(this.record, formController.getFormScope(), entry.getKey());
			Object oldValue;
			boolean isPropertyChanged;
			WebComponent wc;
			String property;
			String onDataChange, onDataChangeCallback;
			for (Pair<WebComponent, String> pair : entry.getValue())
			{
				wc = pair.getLeft();
				property = pair.getRight();
				oldValue = wc.getProperty(property);
				isPropertyChanged = wc.putProperty(property, value);
				onDataChange = ((DataproviderConfig)wc.getFormElement().getWebComponentSpec().getProperties().get(property).getConfig()).getOnDataChange();
				if (fireOnDataChange && onDataChange != null && wc.hasEvent(onDataChange) && isPropertyChanged)
				{
					JSONObject event = EventExecutor.createEvent(onDataChange);
					Object returnValue = wc.execute(onDataChange, new Object[] { oldValue, value, event });
					onDataChangeCallback = ((DataproviderConfig)wc.getFormElement().getWebComponentSpec().getProperties().get(property).getConfig()).getOnDataChangeCallback();
					if (onDataChangeCallback != null)
					{
						wc.executeApi(new WebComponentApiDefinition(onDataChangeCallback), new Object[] { event, returnValue });
					}
				}
				changed = isPropertyChanged || changed;
			}
		}

		//evaluate tagged properties
		for (WebComponent component : componentPropertiesWithTagExpression.keySet())
		{
			for (String taggedProp : componentPropertiesWithTagExpression.get(component))
			{
				String initialPropValue = (String)component.getInitialProperty(taggedProp);
				String tagValue = Text.processTags(initialPropValue, DataAdapterList.this);
				changed = component.putProperty(taggedProp, tagValue) || changed;
			}
		}

		// valuelist update
		Map<String, WebComponent> webComponents = ((IWebFormUI)formController.getFormUI()).getWebComponents();
		Object valuelist;
		for (WebComponent wc : webComponents.values())
		{
			for (String valuelistProperty : wc.getFormElement().getValuelistProperties())
			{
				if ((valuelist = wc.getProperty(valuelistProperty)) instanceof IValueList)
				{
					((IValueList)valuelist).fill(record);
					changed = true;
				}
			}
		}


		if (fireChangeEvent && changed)
		{
			application.getChangeListener().valueChanged();
		}

	}

	@Override
	public void valueChanged(ModificationEvent e)
	{
		// TODO can this be only for the modification ?
		pushRecordValues(true, true);
	}

	public void pushChanges(WebComponent webComponent, String beanProperty)
	{
		Object value = webComponent.getProperty(beanProperty);

		Map<String, String> map = beanToDataHolder.get(webComponent.getFormElement());
		String property = map.get(beanProperty);
		// TODO should this always be tried? (Calendar field has no push for edit, because it doesn't use svyAutoApply)
		// but what if it was a global or form variable?
		if (record.startEditing())
		{
			Object oldValue = com.servoy.j2db.dataprocessing.DataAdapterList.setValueObject(record, formController.getFormScope(), property, value);
			String onDataChange = ((DataproviderConfig)webComponent.getFormElement().getWebComponentSpec().getProperties().get(beanProperty).getConfig()).getOnDataChange();
			if (onDataChange != null && webComponent.hasEvent(onDataChange))
			{
				JSONObject event = EventExecutor.createEvent(onDataChange);
				Object returnValue = webComponent.execute(onDataChange, new Object[] { oldValue, value, event });
				String onDataChangeCallback = ((DataproviderConfig)webComponent.getFormElement().getWebComponentSpec().getProperties().get(beanProperty).getConfig()).getOnDataChangeCallback();
				if (onDataChangeCallback != null)
				{
					webComponent.executeApi(new WebComponentApiDefinition(onDataChangeCallback), new Object[] { event, returnValue });
				}
			}
		}
	}

	public void startEdit(WebComponent webComponent, String property)
	{
		Object dataProvider = beanToDataHolder.get(webComponent.getFormElement()).get(property);
		if (dataProvider != null && !ScopesUtils.isVariableScope(dataProvider.toString()))
		{
			record.startEditing();
		}
	}

	public String getStringValue(String name)
	{
		String stringValue = TagResolver.formatObject(getValueObject(record, name), application.getLocale(), application.getSettings());
		return processValue(stringValue, name, null); // TODO last param ,IDataProviderLookup, should be implemented
	}

	public static String processValue(String stringValue, String dataProviderID, IDataProviderLookup dataProviderLookup)
	{
		if (stringValue == null)
		{
			if ("selectedIndex".equals(dataProviderID) || isCountOrAvgOrSumAggregateDataProvider(dataProviderID, dataProviderLookup)) //$NON-NLS-1$
			{
				return "0"; //$NON-NLS-1$
			}
		}
		return stringValue;
	}

	// helper method; not static because needs form scope
	public Object getValueObject(IRecord record, String dataProviderId)
	{
		return record.getValue(dataProviderId); //TODO scopes support
		//	return getValueObject(record, getFormScope(), dataProviderId);
	}

	public boolean isCountOrAvgOrSumAggregateDataProvider(IDataAdapter dataAdapter)
	{
		return isCountOrAvgOrSumAggregateDataProvider(dataAdapter.getDataProviderID(), null);
	}

	private static boolean isCountOrAvgOrSumAggregateDataProvider(String dataProvider, IDataProviderLookup dataProviderLookup)
	{
		try
		{
			if (dataProviderLookup == null)
			{
				return false;
			}
			IDataProvider dp = dataProviderLookup.getDataProvider(dataProvider);
			if (dp instanceof ColumnWrapper)
			{
				dp = ((ColumnWrapper)dp).getColumn();
			}
			if (dp instanceof AggregateVariable)
			{
				int aggType = ((AggregateVariable)dp).getType();
				return aggType == QueryAggregate.COUNT || aggType == QueryAggregate.AVG || aggType == QueryAggregate.SUM;
			}
		}
		catch (Exception ex)
		{
			Debug.error(ex);
		}

		return false;
	}

	public Object convertToJavaObject(FormElement fe, String propertyName, Object propertyValue) throws JSONException
	{
		if (propertyValue == JSONObject.NULL) return null;
		String dataproviderID = null;
		Map<String, String> recordDataproviderMapping = beanToDataHolder.get(fe);
		if (recordDataproviderMapping != null) dataproviderID = recordDataproviderMapping.get(propertyName);

		if (dataproviderID != null)
		{
			if (findMode)
			{
				return propertyValue;
			}
			// TODO should globals or formscope be checked for that variable? (currently the conversion will be done in the put(String,value) of the scope itself.
			int columnType = record.getParentFoundSet().getTable().getColumnType(dataproviderID);
			if (columnType == IColumnTypeConstants.DATETIME && propertyValue instanceof Long) return new Date(((Long)propertyValue).longValue());
			return propertyValue;
		}
		return JSONUtils.toJavaObject(propertyValue, fe.getWebComponentSpec().getProperties().get(propertyName).getType());
	}

	@Override
	public Object convertFromJavaObjectToString(FormElement fe, String propertyName, Object propertyValue)
	{
		if (findMode)
		{
			return propertyValue;
		}
		PropertyDescription propertyDescription = fe.getWebComponentSpec().getProperties().get(propertyName);
		if (propertyDescription == null) propertyDescription = fe.getWebComponentSpec().getEvents().get(propertyName);
		return JSONUtils.toStringObject(propertyValue, propertyDescription.getType());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.server.ngclient.IDataAdapterList#setFindMode(boolean)
	 */
	@Override
	public void setFindMode(boolean findMode)
	{
		this.findMode = findMode;
		Set<WebComponent> webcomponents = new HashSet<>();
		for (List<Pair<WebComponent, String>> lst : recordDataproviderToComponent.values())
		{
			for (Pair<WebComponent, String> pair : lst)
			{
				webcomponents.add(pair.getLeft());
			}
		}
		WebComponentApiDefinition apiDef = new WebComponentApiDefinition("setFindMode");
		Object[] args = new Object[] { findMode ? Boolean.TRUE : Boolean.FALSE };
		for (WebComponent webComponent : webcomponents)
		{
			executeApi(apiDef, webComponent.getName(), args);
		}
	}
}
