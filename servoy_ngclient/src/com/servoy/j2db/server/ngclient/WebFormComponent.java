package com.servoy.j2db.server.ngclient;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONWriter;
import org.sablo.Container;
import org.sablo.IEventHandler;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils.IToJSONConverter;

import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementDefaultValueToSabloComponent;
import com.servoy.j2db.util.Utils;

/**
 * Servoy extension to work with webcomponents on a form
 * @author jcompagner
 */
@SuppressWarnings("nls")
public class WebFormComponent extends Container implements IContextProvider
{
	public static final String TAG_SCOPE = "scope";

	private final Map<IWebFormUI, Integer> visibleForms = new HashMap<IWebFormUI, Integer>();
	private FormElement formElement;

	protected IDataAdapterList dataAdapterList;

	protected PropertyChangeSupport propertyChangeSupport;
	protected ComponentContext componentContext;
	private IDirtyPropertyListener dirtyPropertyListener;

	public WebFormComponent(String name, FormElement fe, IDataAdapterList dataAdapterList)
	{
		super(name, WebComponentSpecProvider.getSpecProviderState().getWebComponentSpecification(fe.getTypeName()));
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

	@Override
	protected void onPropertyChange(String propertyName, Object oldValue, Object propertyValue)
	{
		super.onPropertyChange(propertyName, oldValue, propertyValue);

		if (propertyChangeSupport != null) propertyChangeSupport.firePropertyChange(propertyName, oldValue, propertyValue);
	}

	/**
	 * These listeners will be triggered when the property changes by reference.
	 */
	public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener)
	{
		if (propertyChangeSupport == null) propertyChangeSupport = new PropertyChangeSupport(this);
		if (propertyName == null) propertyChangeSupport.addPropertyChangeListener(listener);
		else propertyChangeSupport.addPropertyChangeListener(propertyName, listener);
	}

	public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener)
	{
		if (propertyChangeSupport != null)
		{
			if (propertyName == null) propertyChangeSupport.removePropertyChangeListener(listener);
			else propertyChangeSupport.removePropertyChangeListener(propertyName, listener);
		}
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
		return "<" + getName() + ">";
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
		propertyChangeSupport = null;
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

	/**
	 * @param dirtyPropertyListener set the listeners that is called when {@link WebFormComponent#flagPropertyAsDirty(String) is called
	 */
	public void setDirtyPropertyListener(IDirtyPropertyListener dirtyPropertyListener)
	{
		this.dirtyPropertyListener = dirtyPropertyListener;
	}

	@Override
	public boolean flagPropertyAsDirty(String key, boolean dirty)
	{
		boolean modified = super.flagPropertyAsDirty(key, dirty);
		if (modified && dirtyPropertyListener != null) dirtyPropertyListener.propertyFlaggedAsDirty(key, dirty);
		return modified;
	}

	private boolean isWritingComponentProperties;

	public boolean isWritingComponentProperties()
	{
		return isWritingComponentProperties;
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

	@Override
	public Object getRawPropertyValue(String propertyName, boolean getDefaultFromSpecAsWellIfNeeded)
	{
		String[] parts = propertyName.split("\\.");
		String firstProperty = parts[0];
		if (firstProperty.indexOf('[') > 0)
		{
			firstProperty = firstProperty.substring(0, firstProperty.indexOf('['));
		}
		PropertyDescription propertyDesc = specification.getProperty(firstProperty);
		if (propertyDesc != null && propertyDesc.getType() instanceof IFormElementDefaultValueToSabloComponent)
		{
			return super.getRawPropertyValue(propertyName, false);
		}
		return super.getRawPropertyValue(propertyName, getDefaultFromSpecAsWellIfNeeded);
	}

	/**
	 * @return the dataAdapterList
	 */
	public IDataAdapterList getDataAdapterList()
	{
		return dataAdapterList;
	}

}
