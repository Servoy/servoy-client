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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.javascript.Scriptable;
import org.sablo.WebComponent;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentApiDefinition;
import org.sablo.specification.property.types.TypesRegistry;

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
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.query.QueryAggregate;
import com.servoy.j2db.scripting.FormScope;
import com.servoy.j2db.scripting.GlobalScope;
import com.servoy.j2db.scripting.ScopesScope;
import com.servoy.j2db.server.ngclient.component.DesignConversion;
import com.servoy.j2db.server.ngclient.component.EventExecutor;
import com.servoy.j2db.server.ngclient.component.RhinoConversion;
import com.servoy.j2db.server.ngclient.property.DataproviderConfig;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.ScopesUtils;
import com.servoy.j2db.util.Text;


public class DataAdapterList implements IModificationListener, ITagResolver, IDataAdapterList
{
	private final Map<String, Map<WebFormComponent, List<String>>> dataProviderToComponentWithTags = new HashMap<>();
	private final Map<String, List<Pair<WebFormComponent, String>>> recordDataproviderToComponent = new HashMap<>();
	private final Map<FormElement, Map<String, String>> beanToDataHolder = new HashMap<>();
	private final IWebFormController formController;
	private final EventExecutor executor;
	private final WeakHashMap<IWebFormController, String> relatedForms = new WeakHashMap<>();

	private IRecordInternal record;
	private boolean findMode;
	private boolean settingRecord;

	private boolean isFormScopeListener;
	private boolean isGlobalScopeListener;

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
		Object jsRetVal = executor.executeEvent(webComponent, event, eventId, args);
		return RhinoConversion.convert(jsRetVal, null, null, null);
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
	public Object executeInlineScript(String script, JSONObject args, JSONArray appendingArgs)
	{
		String decryptedScript = HTMLTagsConverter.decryptInlineScript(script, args);
		if (appendingArgs != null && decryptedScript.endsWith("()"))
		{
			decryptedScript = decryptedScript.substring(0, decryptedScript.length() - 1);
			for (int i = 0; i < appendingArgs.length(); i++)
			{
				try
				{
					decryptedScript += appendingArgs.get(i);
				}
				catch (JSONException e)
				{
					Debug.error(e);
				}
				if (i < appendingArgs.length() - 1)
				{
					decryptedScript += ",";
				}
				else
				{
					decryptedScript += ")";
				}
			}
		}
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

	private void setupModificationListener(String dataprovider)
	{
		if (!isFormScopeListener && isFormDataprovider(dataprovider))
		{
			formController.getFormScope().getModificationSubject().addModificationListener(this);
			isFormScopeListener = true;
		}
		else if (!isGlobalScopeListener && isGlobalDataprovider(dataprovider))
		{
			formController.getApplication().getScriptEngine().getScopesScope().getModificationSubject().addModificationListener(this);
			isGlobalScopeListener = true;
		}
	}

	public void add(WebFormComponent component, String recordDataProvider, String beanDataProvider)
	{
		String dp = recordDataProvider;
		if (dp.startsWith(ScriptVariable.GLOBALS_DOT_PREFIX))
		{
			dp = ScriptVariable.SCOPES_DOT_PREFIX + dp;
		}
		List<Pair<WebFormComponent, String>> list = recordDataproviderToComponent.get(dp);
		if (list == null)
		{
			list = new ArrayList<Pair<WebFormComponent, String>>();
			recordDataproviderToComponent.put(dp, list);
		}
		list.add(new Pair<WebFormComponent, String>(component, beanDataProvider));

		Map<String, String> map = beanToDataHolder.get(component.getFormElement());
		if (map == null)
		{
			map = new HashMap<>();
			beanToDataHolder.put(component.getFormElement(), map);
		}
		map.put(beanDataProvider, dp);
		if (formController != null) setupModificationListener(dp);
	}

	public void addTaggedProperty(final WebFormComponent component, final String beanTaggedProperty, String propertyValue)
	{
		Text.processTags(propertyValue, new ITagResolver()
		{
			@Override
			public String getStringValue(String name)
			{
				String dp = name;
				if (dp.startsWith(ScriptVariable.GLOBALS_DOT_PREFIX))
				{
					dp = ScriptVariable.SCOPES_DOT_PREFIX + dp;
				}
				Map<WebFormComponent, List<String>> map = dataProviderToComponentWithTags.get(dp);
				if (map == null)
				{
					map = new HashMap<WebFormComponent, List<String>>();
					dataProviderToComponentWithTags.put(dp, map);
				}

				List<String> props = map.get(component);
				if (props == null)
				{
					props = new ArrayList<>();
					map.put(component, props);
				}
				props.add(beanTaggedProperty);
				if (formController != null) setupModificationListener(dp);
				return dp;
			}
		});
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
				pushChangedValues(null, fireChangeEvent, false);
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

	private boolean updateRecordDataprovider(String dataprovider, List<Pair<WebFormComponent, String>> components, boolean fireOnDataChange, boolean fireChange)
	{
		boolean changed = false;
		Object value = com.servoy.j2db.dataprocessing.DataAdapterList.getValueObject(this.record, formController.getFormScope(), dataprovider);
		if (value == Scriptable.NOT_FOUND) value = null;
		for (Pair<WebFormComponent, String> pair : components)
		{
			WebFormComponent wc = pair.getLeft();
			String property = pair.getRight();
			Object oldValue = wc.getProperty(property);
			boolean isPropertyChanged = wc.setProperty(property, value);
			if (isPropertyChanged && fireOnDataChange)
			{
				String onDataChange = ((DataproviderConfig)wc.getFormElement().getWebComponentSpec().getProperty(property).getConfig()).getOnDataChange();
				if (onDataChange != null && wc.hasEvent(onDataChange))
				{
					JSONObject event = EventExecutor.createEvent(onDataChange);
					Object returnValue = wc.executeEvent(onDataChange, new Object[] { oldValue, value, event });
					String onDataChangeCallback = ((DataproviderConfig)wc.getFormElement().getWebComponentSpec().getProperty(property).getConfig()).getOnDataChangeCallback();
					if (onDataChangeCallback != null)
					{
						wc.invokeApi(onDataChangeCallback, new Object[] { event, returnValue });
					}
				}
			}
			if (!fireChange)
			{
				// this component is currently rendering so clear all the changes
				wc.clearChanges();
			}
			changed = isPropertyChanged || changed;
		}

		return changed;
	}

	private boolean updateTagValue(Map<WebFormComponent, List<String>> components, boolean fireChange)
	{
		boolean changed = false;
		for (Map.Entry<WebFormComponent, List<String>> entry : components.entrySet())
		{
			WebFormComponent component = entry.getKey();
			for (String taggedProp : entry.getValue())
			{
				String initialPropValue = (String)component.getInitialProperty(taggedProp);
				String tagValue = Text.processTags(initialPropValue, DataAdapterList.this);
				changed = component.setProperty(taggedProp, tagValue) || changed;
			}
			if (!fireChange)
			{
				// this component is currently rendering so clear all the changes
				component.clearChanges();
			}
		}

		return changed;
	}

	protected boolean isFormDataprovider(String dataprovider)
	{
		if (dataprovider == null) return false;
		FormScope fs = formController.getFormScope();
		return fs.has(dataprovider, fs);
	}

	protected boolean isGlobalDataprovider(String dataprovider)
	{
		if (dataprovider == null) return false;
		ScopesScope ss = formController.getApplication().getScriptEngine().getScopesScope();
		Pair<String, String> scope = ScopesUtils.getVariableScope(dataprovider);
		if (scope.getLeft() != null)
		{
			GlobalScope gs = ss.getGlobalScope(scope.getLeft());
			return gs != null && gs.has(scope.getRight(), gs);
		}

		return false;
	}

	private void pushChangedValues(String dataProvider, boolean fireChangeEvent, boolean fireOnDataChange)
	{
		boolean changed = false;
		boolean isFormDP = isFormDataprovider(dataProvider);
		boolean isGlobalDP = isGlobalDataprovider(dataProvider);

		if (dataProvider == null)
		{
			for (Entry<String, List<Pair<WebFormComponent, String>>> entry : recordDataproviderToComponent.entrySet())
			{
				changed = updateRecordDataprovider(entry.getKey(), entry.getValue(), fireOnDataChange, fireChangeEvent) || changed;
			}

			for (Entry<String, Map<WebFormComponent, List<String>>> entry : dataProviderToComponentWithTags.entrySet())
			{
				changed = updateTagValue(entry.getValue(), fireChangeEvent) || changed;
			}
		}
		else
		{
			if (recordDataproviderToComponent.containsKey(dataProvider))
			{
				changed = updateRecordDataprovider(dataProvider, recordDataproviderToComponent.get(dataProvider), fireOnDataChange, fireChangeEvent);
			}

			if ((isFormDP || isGlobalDP) && dataProviderToComponentWithTags.containsKey(dataProvider))
			{
				changed = updateTagValue(dataProviderToComponentWithTags.get(dataProvider), fireChangeEvent) || changed;
			}
		}

		if (!isFormDP && !isGlobalDP)
		{
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

			// let complex properties of all web components know that the current record has changed
			for (WebComponent comp : webComponents)
			{
				WebFormComponent wc = (WebFormComponent)comp;
				changed = wc.pushRecord(record) || changed;
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
		if (getForm().isFormVisible())
		{
			pushChangedValues(e.getName(), true, false);
		}
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
		if (record == null || record.startEditing())
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
					WebComponentApiDefinition call = new WebComponentApiDefinition(onDataChangeCallback);
					call.addParameter(new PropertyDescription("event", TypesRegistry.getType("object")));
					call.addParameter(new PropertyDescription("returnValue", TypesRegistry.getType("object")));
					webComponent.invokeApi(call, new Object[] { event, returnValue });
				}
			}
		}
	}

	public void startEdit(WebFormComponent webComponent, String property)
	{
		String dataProvider = beanToDataHolder.get(webComponent.getFormElement()).get(property);
		if (record != null && dataProvider != null && !ScopesUtils.isVariableScope(dataProvider))
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
		}
		return propertyValue;
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
		return propertyDescription != null ? DesignConversion.toStringObject(propertyValue, propertyDescription.getType()) : propertyValue;
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

		for (WebComponent component : formController.getFormUI().getComponents())
		{
			if (component instanceof WebFormComponent)
			{
				WebFormComponent wfc = (WebFormComponent)component;
				if (wfc.getFormElement().getTagname().equals("data-servoydefault-portal"))
				{
					webcomponents.add(wfc);
				}
			}
		}

		boolean editable = !Boolean.TRUE.equals(getApplication().getClientProperty(IApplication.LEAVE_FIELDS_READONLY_IN_FIND_MODE));
		WebComponentApiDefinition findModeCall = new WebComponentApiDefinition("setFindMode");
		findModeCall.addParameter(new PropertyDescription("mode", TypesRegistry.getType("boolean")));
		findModeCall.addParameter(new PropertyDescription("editable", TypesRegistry.getType("boolean")));
		Object[] args = new Object[] { Boolean.valueOf(findMode), Boolean.valueOf(editable) };
		for (WebFormComponent webComponent : webcomponents)
		{
			webComponent.invokeApi(findModeCall, args);
		}
	}
}
