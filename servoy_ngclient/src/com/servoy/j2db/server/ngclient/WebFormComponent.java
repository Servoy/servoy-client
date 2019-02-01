package com.servoy.j2db.server.ngclient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.sablo.Container;
import org.sablo.IEventHandler;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebObjectFunctionDefinition;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.specification.property.IPropertyType;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils.IToJSONConverter;

import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.server.ngclient.component.EventExecutor;
import com.servoy.j2db.server.ngclient.property.DataproviderConfig;
import com.servoy.j2db.server.ngclient.property.INGWebObjectContext;
import com.servoy.j2db.server.ngclient.property.types.NGConversions;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IDesignToFormElement;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementDefaultValueToSabloComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementToSabloComponent;
import com.servoy.j2db.util.Utils;

/**
 * Servoy extension to work with webcomponents on a form
 * @author jcompagner
 */
@SuppressWarnings("nls")
public class WebFormComponent extends Container implements IContextProvider, INGWebObjectContext
{
	public static final String TAG_SCOPE = "scope";

	private final Map<IWebFormUI, Integer> visibleForms = new HashMap<IWebFormUI, Integer>();
	private FormElement formElement;

	protected IDataAdapterList dataAdapterList;

	protected ComponentContext componentContext;

	private boolean isInvalidState;

	public WebFormComponent(String name, FormElement fe, IDataAdapterList dataAdapterList)
	{
		super(name, WebComponentSpecProvider.getSpecProviderState().getWebComponentSpecification(fe.getTypeName()), true);

		this.formElement = fe;
		this.dataAdapterList = dataAdapterList;

		properties.put("svyMarkupId", ComponentFactory.getMarkupId(dataAdapterList.getForm().getName(), name));
	}

	public FormElement getFormElement()
	{
		return formElement;
	}

	/**
	 * Only used in designer to update this component to the latest desgin form element.
	 *
	 * @param formElement the formElement to set
	 */
	public void setFormElement(FormElement formElement)
	{
		this.formElement = formElement;
	}

	/**
	 * @param componentContext the componentContext to set
	 */
	public void setComponentContext(ComponentContext componentContext)
	{
		this.componentContext = componentContext;
	}

	/**
	 * @return the componentContext
	 */
	public ComponentContext getComponentContext()
	{
		return componentContext;
	}

	// TODO get rid of this! it should not be needed once all types are implemented properly (I think usually wrapped types use it, and they could keep
	// this value in their internal state if needed)
	public Object getInitialProperty(String propertyName)
	{
		// NGConversions.INSTANCE.applyConversion3(...) could be used here as this is currently wrong value type, but
		// it's better to remove it altogether as previous comment says; anyway right now I think the types that call/use this method don't have conversion 3 defined
		return formElement.getPropertyValue(propertyName);
	}

	public void add(String eventType, int functionID)
	{
		addEventHandler(eventType, new FormcomponentEventHandler(eventType, functionID));
	}

	@Override
	public IEventHandler getEventHandler(String eventType)
	{
		IEventHandler handler = super.getEventHandler(eventType);
		if (handler == null)
		{
			throw new IllegalArgumentException("Unknown event '" + eventType + "' for component " + this);
		}
		return handler;
	}

	@Override
	public String toString()
	{
		return "<'" + getName() + "' of parent " + getParent() + ">";
	}

	public void updateVisibleForm(IWebFormUI form, boolean visible, int formIndex)
	{
		if (!visible)
		{
			visibleForms.remove(form);
			form.setParentContainer(null);
		}
		else if (!visibleForms.containsKey(form))
		{
			form.setParentContainer(this);
			visibleForms.put(form, Integer.valueOf(formIndex));
		}
	}

	public boolean notifyVisible(boolean visible, List<Runnable> invokeLaterRunnables, Set<IWebFormController> childFormsThatWereNotified)
	{
		// TODO if there are multiply forms visible and only 1 is reporting that it can't be made invisible
		// what to do with that state? Should it be rollbacked? Should everything be made visible again?
		// see also WebFormUI
		boolean retValue = true;
		for (IWebFormUI webUI : visibleForms.keySet())
		{
			IWebFormController fc = webUI.getController();
			childFormsThatWereNotified.add(fc);
			retValue = retValue && fc.notifyVisible(visible, invokeLaterRunnables);
		}

		if (!visible && retValue)
		{
			visibleForms.clear();
		}
		return retValue;
	}

	public int getFormIndex(IWebFormUI form)
	{
		return visibleForms.containsKey(form) ? visibleForms.get(form).intValue() : -1;
	}

	public IServoyDataConverterContext getDataConverterContext()
	{
		return new ServoyDataConverterContext(dataAdapterList.getForm());
	}

	public IWebFormUI[] getVisibleForms()
	{
		return visibleForms.keySet().toArray(new IWebFormUI[visibleForms.size()]);
	}

	@Override
	public void dispose()
	{
		super.dispose();
	}

	public boolean isDesignOnlyProperty(String propertyName)
	{
		return isDesignOnlyProperty(specification.getProperty(propertyName));
	}

	public static boolean isDesignOnlyProperty(PropertyDescription propertyDescription)
	{
		return propertyDescription != null && "design".equals(propertyDescription.getTag(TAG_SCOPE));
	}

	/**
	 * TODO What does this mean 'private'?
	 */
	public boolean isPrivateProperty(String propertyName)
	{
		return isPrivateProperty(specification.getProperty(propertyName));
	}

	/**
	 * TODO What does this mean 'private'?
	 */
	public static boolean isPrivateProperty(PropertyDescription propertyDescription)
	{
		return propertyDescription != null && "private".equals(propertyDescription.getTag(TAG_SCOPE));
	}

	public class FormcomponentEventHandler implements IEventHandler
	{
		private final String eventType;
		private final int functionID;

		public FormcomponentEventHandler(String eventType, int functionID)
		{
			this.eventType = eventType;
			this.functionID = functionID;
		}

		@Override
		public Object executeEvent(Object[] args)
		{
			// verify if component is accessible due to security options
			IPersist persist = formElement.getPersistIfAvailable();
			if (persist != null)
			{
				int access = dataAdapterList.getApplication().getFlattenedSolution().getSecurityAccess(persist.getUUID());
				if (!((access & IRepository.ACCESSIBLE) != 0))
					throw new RuntimeException("Security error. Component '" + getProperty("name") + "' is not accessible.");
			}
			if (Utils.equalObjects(eventType, StaticContentSpecLoader.PROPERTY_ONFOCUSGAINEDMETHODID.getPropertyName()) &&
				(formElement.getForm().getOnElementFocusGainedMethodID() > 0) && formElement.getForm().getOnElementFocusGainedMethodID() != functionID)
			{
				dataAdapterList.executeEvent(WebFormComponent.this, eventType, formElement.getForm().getOnElementFocusGainedMethodID(), args);
			}
			else if (Utils.equalObjects(eventType, StaticContentSpecLoader.PROPERTY_ONFOCUSLOSTMETHODID.getPropertyName()) &&
				(formElement.getForm().getOnElementFocusLostMethodID() > 0) && formElement.getForm().getOnElementFocusLostMethodID() != functionID)
			{
				dataAdapterList.executeEvent(WebFormComponent.this, eventType, formElement.getForm().getOnElementFocusLostMethodID(), args);
			}

			Object executeEventReturn = dataAdapterList.executeEvent(WebFormComponent.this, eventType, functionID, args);

			if (Utils.equalObjects(eventType, StaticContentSpecLoader.PROPERTY_ONDATACHANGEMETHODID.getPropertyName()) &&
				(formElement.getForm().getOnElementDataChangeMethodID() > 0) && formElement.getForm().getOnElementDataChangeMethodID() != functionID)
			{
				boolean isValueValid = !Boolean.FALSE.equals(executeEventReturn) &&
					!(executeEventReturn instanceof String && ((String)executeEventReturn).length() > 0);
				if (isValueValid)
				{
					executeEventReturn = dataAdapterList.executeEvent(WebFormComponent.this, eventType, formElement.getForm().getOnElementDataChangeMethodID(),
						args);
				}
			}

			return executeEventReturn;
		}
	}

	@Override
	public boolean markPropertyAsChangedByRef(String key)
	{
		boolean modified = super.markPropertyAsChangedByRef(key);
		return modified;
	}

	@Override
	public boolean clearChangedStatusForProperty(String key)
	{
		boolean modified = super.clearChangedStatusForProperty(key);
		return modified;
	}

	@Override
	protected boolean markPropertyContentsUpdated(String key)
	{
		if (isInvalidState())
		{
			DataproviderConfig dataproviderConfig = DataAdapterList.getDataproviderConfig(this, key);
			if (dataproviderConfig != null && dataproviderConfig.getOnDataChangeCallback() != null)
			{
				WebObjectFunctionDefinition function = DataAdapterList.createWebObjectFunction(dataproviderConfig.getOnDataChangeCallback());
				JSONObject event = EventExecutor.createEvent(dataproviderConfig.getOnDataChangeCallback(),
					dataAdapterList.getForm().getFormModel().getSelectedIndex());
				invokeApi(function, new Object[] { event, null, null });
				setInvalidState(false);
			}
		}

		boolean modified = super.markPropertyContentsUpdated(key);
		return modified;
	}

	private boolean isWritingComponentProperties;

	public boolean isWritingComponentProperties()
	{
		return isWritingComponentProperties;
	}

	public void setInvalidState(boolean state)
	{
		isInvalidState = state;
	}

	public boolean isInvalidState()
	{
		return isInvalidState;
	}

	@Override
	protected boolean writeComponentProperties(JSONWriter w, IToJSONConverter<IBrowserConverterContext> converter, String nodeName,
		DataConversion clientDataConversions) throws JSONException
	{
		try
		{
			isWritingComponentProperties = true;
			return super.writeComponentProperties(w, converter, nodeName, clientDataConversions);
		}
		finally
		{
			isWritingComponentProperties = false;
		}
	}

	public IDataAdapterList getDataAdapterList()
	{
		return dataAdapterList;
	}

	@Override
	public Object getDefaultFromPD(PropertyDescription propertyDesc)
	{
		return getDefaultFromPropertyDescription(propertyDesc);
	}

	public static Object getDefaultFromPropertyDescription(PropertyDescription propertyDesc)
	{
		// THIS METHOD IS CALLED FOR SERVICES AS WELL NOT JUST FOR COMPONENTS

		Object defaultPropertyValue = null;


		if (propertyDesc.hasDefault())
		{
			// short story here; if a web component .spec file defines a "defaultValue" then that is interpreted as a "design value"
			// so before reaching our WebFormComponent it will pass through 2 conversions: design-to-formElement (IDesignToFormElement) and formElement-to-sablo (IFormElementToSabloComponent);
			// if the type implements any of the two we cannot just return the pd.getDefaultValue() here as it will probably lead to a classcast exception (it is meant to be converted before reaching runtime and it will be converted but one property at a time after creating the runtime web component in ComponentFactory);
			// whoever wants the value will have to wait for it to be converted and set on the WebFormComponent before using it - so one property then can't just get another property and expect it to have default value prepared already

			// but if it doesn't implement any of those 2 conversions and a default is available in spec, then it can be returned here faster (passed through default conversions) - as it would reach the WebFormComponent un-converted anyway
			// we could just return null here always (because the value will be set soon when creating the runtime web component, it's just a matter of which properties are set first after being converted),
			// but then properties like NGEnabledProperty type which ask for webObject.isEnabled on parents right away - which then could look into enabled type properties and expect a boolean (so not null) wouldn't work in the same way; those properties don't want to wait... they want to be able to get other propertie's default boolean values right away

			IPropertyType< ? > type = propertyDesc.getType();
			if (!(type instanceof IDesignToFormElement || type instanceof IFormElementToSabloComponent))
			{
				return NGConversions.INSTANCE.defaultDesignToFormElementValue(propertyDesc.getDefaultValue()); // there is currently no 'default' conversion from formElementToSablo - we would need to do that here as well otherwise
			}
		}
		else
		{
			// so it doesn't have a default set in .spec; see if we can return type default or not then
			if (!(propertyDesc.getType() instanceof IFormElementDefaultValueToSabloComponent))
			{
				defaultPropertyValue = propertyDesc.getType().defaultValue(propertyDesc);
			} // else the form-element-default-to-sablo will generate the default value; don't get default from type (as this getDefaultFromPD might get called before IFormElementDefaultValueToSabloComponent had
				// a chance to execute - if a property that depends on this property is initialized first and asks for this property's value)
		}

		return defaultPropertyValue;
	}

}
