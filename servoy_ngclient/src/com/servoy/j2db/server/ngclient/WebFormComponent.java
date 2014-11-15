package com.servoy.j2db.server.ngclient;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.sablo.Container;
import org.sablo.IEventHandler;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecProvider;

import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.server.ngclient.property.IServoyAwarePropertyValue;
import com.servoy.j2db.server.ngclient.property.types.IDataLinkedType;

/**
 * Servoy extension to work with webcomponents on a form
 * @author jcompagner
 */
@SuppressWarnings("nls")
public class WebFormComponent extends Container implements IContextProvider
{
	private final Map<IWebFormUI, Integer> visibleForms = new HashMap<IWebFormUI, Integer>();
	private FormElement formElement;

	protected IDataAdapterList dataAdapterList;

	protected PropertyChangeSupport propertyChangeSupport;
	protected IWebFormUI parentForm;
	protected ComponentContext componentContext;

	public WebFormComponent(String name, FormElement fe, IDataAdapterList dataAdapterList)
	{
		super(name, WebComponentSpecProvider.getInstance().getWebComponentSpecification(fe.getTypeName()));
		this.formElement = fe;
		this.dataAdapterList = dataAdapterList;

		properties.put("svyMarkupId", ComponentFactory.getMarkupId(dataAdapterList.getForm().getName(), name));
		for (PropertyDescription pd : fe.getWebComponentSpec().getProperties().values())
		{
			if (pd.getType() instanceof IDataLinkedType)
			{
				((DataAdapterList)dataAdapterList).addRecordAwareComponent(this);
				break;
			}
		}
	}

	/**
	 * @return
	 */
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

	public Object getConvertedPropertyWithDefault(String propertyName, boolean designValue, boolean convertValue)
	{
		// TODO remove this when possible; once all types keep their own design value if they need it and there's no need for that conversion
		// if it's implemented already in the type itself, this method should be removable
		Object value = null;
		if (!designValue && properties.containsKey(propertyName))
		{
			value = getProperty(propertyName);
		}
		else
		{
			value = getInitialProperty(propertyName);
		}
		return dataAdapterList != null && convertValue ? dataAdapterList.convertFromJavaObjectToString(formElement, propertyName, value) : value;
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
	protected Object convertPropertyValue(String propertyName, Object oldValue, Object propertyValue) throws JSONException
	{
		return dataAdapterList != null ? dataAdapterList.convertToJavaObject(getFormElement(), propertyName, propertyValue) : propertyValue;
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

	/*
	 * (non-Javadoc)
	 *
	 * @see org.sablo.BaseWebObject#getEventHandler(java.lang.String)
	 */
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

	public int getFormIndex(IWebFormUI form)
	{
		return visibleForms.containsKey(form) ? visibleForms.get(form).intValue() : -1;
	}

	public IServoyDataConverterContext getDataConverterContext()
	{
		return new ServoyDataConverterContext(dataAdapterList.getForm());
	}

	/**
	 * Notifies this component that the record or dataprovider values that it displays have changed.
	 */
	public void pushDataProviderOrRecordChanged(IRecordInternal record, String dataProvider, boolean isFormDP, boolean isGlobalDP, boolean fireChangeEvent)
	{
		for (String pN : getAllPropertyNames(true))
		{
			Object x = getProperty(pN);
			if (x instanceof IServoyAwarePropertyValue) ((IServoyAwarePropertyValue)x).dataProviderOrRecordChanged(record, dataProvider, isFormDP, isGlobalDP,
				fireChangeEvent);
		}
	}

	@Override
	public void dispose()
	{
		propertyChangeSupport = null;
		((DataAdapterList)dataAdapterList).removeRecordAwareComponent(this);
		super.dispose();
	}

	public boolean isDesignOnlyProperty(String propertyName)
	{
		PropertyDescription description = specification.getProperty(propertyName);
		return isDesignOnlyProperty(description);
	}

	public static boolean isDesignOnlyProperty(PropertyDescription propertyDescription)
	{
		if (propertyDescription != null)
		{
			return "design".equals(propertyDescription.getScope());
		}
		return false;
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
				if (!((access & IRepository.ACCESSIBLE) != 0)) throw new RuntimeException("Security error. Component '" + getProperty("name") +
					"' is not accessible.");
			}

			return dataAdapterList.executeEvent(WebFormComponent.this, eventType, functionID, args);
		}
	}

}
