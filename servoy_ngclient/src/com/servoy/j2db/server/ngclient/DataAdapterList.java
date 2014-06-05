package com.servoy.j2db.server.ngclient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;

import org.json.JSONException;
import org.json.JSONObject;
import org.sablo.WebComponent;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.PropertyType;
import org.sablo.specification.WebComponentApiDefinition;
import org.sablo.websocket.ConversionLocation;

import com.servoy.base.persistence.constants.IColumnTypeConstants;
import com.servoy.base.util.ITagResolver;
import com.servoy.j2db.IApplication;
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
import com.servoy.j2db.server.ngclient.component.DesignConversion;
import com.servoy.j2db.server.ngclient.component.EventExecutor;
import com.servoy.j2db.server.ngclient.property.DataproviderConfig;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.HtmlUtils;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.ScopesUtils;
import com.servoy.j2db.util.Text;


public class DataAdapterList implements IModificationListener, ITagResolver, IDataAdapterList
{
	/**componentPropertiesWithTagExpression : keeps track of properties with tags from a bean (properties of type 'tagstring') */
	private final Map<WebFormComponent, List<String>> componentPropertiesWithTagExpression = new HashMap<>();
	private final Map<String, List<Pair<WebFormComponent, String>>> recordDataproviderToComponent = new HashMap<>();
	private final Map<FormElement, Map<String, String>> beanToDataHolder = new HashMap<>();
	private final IWebFormController formController;
	private final EventExecutor executor;
	private final WeakHashMap<IWebFormController, String> relatedForms = new WeakHashMap<>();

	private IRecordInternal record;
	private boolean findMode;
	private boolean settingRecord;

	public DataAdapterList(IWebFormController formController)
	{
		this.formController = formController;
		this.executor = new EventExecutor(formController);
	}

	/**
	 * @return the application
	 */
	public final INGApplication getApplication()
	{
		return formController.getApplication();
	}

	public final IWebFormController getForm()
	{
		return formController;
	}

	@Override
	public Object executeEvent(WebComponent webComponent, String event, int eventId, Object[] args)
	{
		return executor.executeEvent(webComponent, event, eventId, args);
	}

//	@Override
//	public Object invokeApi(WebComponentApiDefinition apiDefinition, String componentName, Object[] args)
//	{
//		// TODO will by name be always enough, what happens exactly when we are in a tableview so having multiply of the same name..
//		INGClientWebsocketSession clientSession = getApplication().getWebsocketSession();
//		Form form = formController.getForm();
//		clientSession.touchForm(form, formController.getName(), false);
//		return clientSession.invokeApi(apiDefinition, formController.getName(), componentName, args);
//	}

	@Override
	public Object executeInlineScript(String script, JSONObject args)
	{
		String decryptedScript = HTMLTagsConverter.decryptInlineScript(script, args);
		return decryptedScript != null ? formController.eval(decryptedScript) : null;
	}

	public void add(WebFormComponent component, Map<String, String> hm)
	{
		for (Entry<String, String> entry : hm.entrySet())
		{
			add(component, entry.getKey(), entry.getValue());
		}
	}

	public void addRelatedForm(IWebFormController form, String relation)
	{
		form.setParentFormController(formController);
		relatedForms.put(form, relation);
	}

	public void removeRelatedForm(IWebFormController form)
	{
		form.setParentFormController(null);
		relatedForms.remove(form);
	}

	public void add(WebFormComponent component, String recordDataProvider, String beanDataProvider)
	{
		List<Pair<WebFormComponent, String>> list = recordDataproviderToComponent.get(recordDataProvider);
		if (list == null)
		{
			list = new ArrayList<Pair<WebFormComponent, String>>();
			recordDataproviderToComponent.put(recordDataProvider, list);
		}
		list.add(new Pair<WebFormComponent, String>(component, beanDataProvider));

		Map<String, String> map = beanToDataHolder.get(component.getFormElement());
		if (map == null)
		{
			map = new HashMap<>();
			beanToDataHolder.put(component.getFormElement(), map);
		}
		map.put(beanDataProvider, recordDataProvider);
	}

	public void addTaggedProperty(WebFormComponent component, String beanTaggedProperty)
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
		if (settingRecord)
		{
			if (record != this.record)
			{
				throw new IllegalStateException("Record " + record + " is being set on DAL when record: " + this.record + " is being processed");
			}
			return;
		}
		try
		{
			settingRecord = true;
			if (this.record != null)
			{
				this.record.removeModificationListener(this);
			}
			this.record = (IRecordInternal)record;
			if (this.record != null)
			{
				pushRecordValues(fireChangeEvent, false);
				this.record.addModificationListener(this);
			}

			for (IWebFormController form : relatedForms.keySet())
			{
				if (form.isFormVisible())
				{
					form.loadRecords(record != null ? record.getRelatedFoundSet(relatedForms.get(form)) : null);
				}
			}
		}
		finally
		{
			settingRecord = false;
		}
	}

	public IRecordInternal getRecord()
	{
		return record;
	}

	private void pushRecordValues(boolean fireChangeEvent, boolean fireOnDataChange)
	{
		boolean changed = false;
		for (Entry<String, List<Pair<WebFormComponent, String>>> entry : recordDataproviderToComponent.entrySet())
		{
			Object value = com.servoy.j2db.dataprocessing.DataAdapterList.getValueObject(this.record, formController.getFormScope(), entry.getKey());
			Object oldValue;
			boolean isPropertyChanged;
			WebFormComponent wc;
			String property;
			String onDataChange, onDataChangeCallback;
			for (Pair<WebFormComponent, String> pair : entry.getValue())
			{
				wc = pair.getLeft();
				property = pair.getRight();
				oldValue = wc.getProperty(property);
				isPropertyChanged = wc.setProperty(property, value, ConversionLocation.SERVER);
				onDataChange = ((DataproviderConfig)wc.getFormElement().getWebComponentSpec().getProperty(property).getConfig()).getOnDataChange();
				if (fireOnDataChange && onDataChange != null && wc.hasEvent(onDataChange) && isPropertyChanged)
				{
					JSONObject event = EventExecutor.createEvent(onDataChange);
					Object returnValue = wc.executeEvent(onDataChange, new Object[] { oldValue, value, event });
					onDataChangeCallback = ((DataproviderConfig)wc.getFormElement().getWebComponentSpec().getProperty(property).getConfig()).getOnDataChangeCallback();
					if (onDataChangeCallback != null)
					{
						wc.invokeApi(onDataChangeCallback, new Object[] { event, returnValue });
					}
				}
				changed = isPropertyChanged || changed;
			}
		}

		//evaluate tagged properties
		for (WebFormComponent component : componentPropertiesWithTagExpression.keySet())
		{
			for (String taggedProp : componentPropertiesWithTagExpression.get(component))
			{
				String initialPropValue = (String)component.getInitialProperty(taggedProp);
				String tagValue = Text.processTags(initialPropValue, DataAdapterList.this);
				changed = component.setProperty(taggedProp, tagValue, ConversionLocation.SERVER) || changed;
			}
		}

		// valuelist update
		Collection<WebComponent> webComponents = formController.getFormUI().getComponents();
		// TODO how to handle nested components through custom component[] property types for example? - those are not listed in formUI
		Object valuelist;
		for (WebComponent comp : webComponents)
		{
			WebFormComponent wc = (WebFormComponent)comp;
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
			getApplication().getChangeListener().valueChanged();
		}

	}

	@Override
	public void valueChanged(ModificationEvent e)
	{
		// TODO can this be only for the modification ?
		pushRecordValues(true, true);
	}

	public void pushChanges(WebFormComponent webComponent, String beanProperty)
	{
		pushChanges(webComponent, beanProperty, webComponent.getProperty(beanProperty));
	}

	public void pushChanges(WebFormComponent webComponent, String beanProperty, Object newValue)
	{
		Map<String, String> map = beanToDataHolder.get(webComponent.getFormElement());
		if (map == null)
		{
			Debug.log("apply called on a property that is not bound to a dataprovider: " + beanProperty + ", value: " + newValue + " of component: " +
				webComponent);
			return;
		}
		String property = map.get(beanProperty);
		// TODO should this always be tried? (Calendar field has no push for edit, because it doesn't use svyAutoApply)
		// but what if it was a global or form variable?
		if (record.startEditing())
		{
			Object oldValue = com.servoy.j2db.dataprocessing.DataAdapterList.setValueObject(record, formController.getFormScope(), property, newValue);
			String onDataChange = ((DataproviderConfig)webComponent.getFormElement().getWebComponentSpec().getProperty(beanProperty).getConfig()).getOnDataChange();
			if (onDataChange != null && webComponent.hasEvent(onDataChange))
			{
				JSONObject event = EventExecutor.createEvent(onDataChange);
				Object returnValue = webComponent.executeEvent(onDataChange, new Object[] { oldValue, newValue, event });
				String onDataChangeCallback = ((DataproviderConfig)webComponent.getFormElement().getWebComponentSpec().getProperty(beanProperty).getConfig()).getOnDataChangeCallback();
				if (onDataChangeCallback != null)
				{
					webComponent.invokeApi(onDataChangeCallback, new Object[] { event, returnValue });
				}
			}
		}
	}

	public void startEdit(WebFormComponent webComponent, String property)
	{
		Object dataProvider = beanToDataHolder.get(webComponent.getFormElement()).get(property);
		if (dataProvider != null && !ScopesUtils.isVariableScope(dataProvider.toString()))
		{
			record.startEditing();
		}
	}

	public String getStringValue(String name)
	{
		String stringValue = TagResolver.formatObject(getValueObject(record, name), getApplication().getLocale(), getApplication().getSettings());
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

	public Object convertToJavaObject(FormElement fe, String propertyName, Object propertyValue, ConversionLocation sourceOfValue, Object oldValue)
		throws JSONException
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
		}
		return NGClientForJsonConverter.toJavaObject(propertyValue, fe.getWebComponentSpec().getProperty(propertyName), new ServoyDataConverterContext(
			getForm()), sourceOfValue, oldValue);
	}

	@Override
	public Object convertFromJavaObjectToString(FormElement fe, String propertyName, Object propertyValue)
	{
		if (findMode)
		{
			return propertyValue;
		}
		PropertyDescription propertyDescription = fe.getWebComponentSpec().getProperties().get(propertyName);
		if (propertyDescription == null) propertyDescription = fe.getWebComponentSpec().getHandlers().get(propertyName);
		return DesignConversion.toStringObject(propertyValue, propertyDescription.getType());
	}

	@Override
	public void setFindMode(boolean findMode)
	{
		this.findMode = findMode;
		Set<WebFormComponent> webcomponents = new HashSet<>();
		for (List<Pair<WebFormComponent, String>> lst : recordDataproviderToComponent.values())
		{
			for (Pair<WebFormComponent, String> pair : lst)
			{
				webcomponents.add(pair.getLeft());
			}
		}

		boolean editable = !Boolean.TRUE.equals(getApplication().getClientProperty(IApplication.LEAVE_FIELDS_READONLY_IN_FIND_MODE));
		WebComponentApiDefinition findModeCall = new WebComponentApiDefinition("setFindMode");
		findModeCall.addParameter(new PropertyDescription("mode", new PropertyType("boolean")));
		findModeCall.addParameter(new PropertyDescription("editable", new PropertyType("boolean")));
		Object[] args = new Object[] { Boolean.valueOf(findMode), Boolean.valueOf(editable) };
		for (WebFormComponent webComponent : webcomponents)
		{
			webComponent.invokeApi(findModeCall, args);
		}
	}
}
