package com.servoy.j2db.server.ngclient;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import org.json.JSONException;
import org.sablo.IChangeListener;
import org.sablo.WebComponent;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IComplexPropertyValue;
import org.sablo.specification.property.types.TypesRegistry;
import org.sablo.websocket.ConversionLocation;

import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.dataprocessing.LookupListModel;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.server.ngclient.property.IServoyAwarePropertyValue;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.SortedList;
import com.servoy.j2db.util.Utils;

/**
 * Servoy extension to work with webcomponents on a form
 * @author jcompagner
 */
@SuppressWarnings("nls")
public class WebFormComponent extends WebComponent implements ListDataListener, IContextProvider
{
	private final Map<String, Integer> events = new HashMap<>(); //event name mapping to persist id
	private final FormElement formElement;
	private final Map<IWebFormUI, Integer> visibleForms = new HashMap<IWebFormUI, Integer>();

	// list of all tabseq properties ordered by design time value; tabseq will be updated with runtime value
	private final List<Pair<String, Integer>> calculatedTabSequence = new ArrayList<Pair<String, Integer>>();

	protected IDataAdapterList dataAdapterList;

	// the next available tab sequence number (after this component and all its subtree)
	protected int nextAvailableTabSequence;
	protected PropertyChangeSupport propertyChangeSupport;
	protected IWebFormUI parentForm;

	public WebFormComponent(String name, FormElement fe, IDataAdapterList dataAdapterList)
	{
		super(fe.getTypeName(), name);
		this.formElement = fe;
		this.dataAdapterList = dataAdapterList;

		if (fe.getLabel() != null)
		{
			properties.put("markupId", ComponentFactory.getMarkupId(fe.getForm().getName(), name));
		}
		if (fe.getWebComponentSpec(false) != null)
		{
			Map<String, PropertyDescription> tabSeqProps = fe.getWebComponentSpec().getProperties(TypesRegistry.getType("tabseq"));
			List<PropertyDescription> sortedList = new SortedList<PropertyDescription>(new Comparator<PropertyDescription>()
			{

				@Override
				public int compare(PropertyDescription o1, PropertyDescription o2)
				{
					int tabSeq1 = Utils.getAsInteger(WebFormComponent.this.getInitialProperty(o1.getName()));
					int tabSeq2 = Utils.getAsInteger(WebFormComponent.this.getInitialProperty(o2.getName()));
					if (tabSeq1 != tabSeq2)
					{
						return tabSeq1 - tabSeq2;
					}
					else
					{
						return o1.getName().compareTo(o2.getName());
					}
				}
			}, tabSeqProps.values());
			for (PropertyDescription pd : sortedList)
			{
				calculatedTabSequence.add(new Pair<>(pd.getName(), Utils.getAsInteger(getInitialProperty(pd.getName()))));
			}
			nextAvailableTabSequence = getMaxTabSequence() + 1;
			if (fe.isGraphicalComponentWithNoAction())
			{
				// hack for legacy behavior
				properties.put(StaticContentSpecLoader.PROPERTY_TABSEQ.getPropertyName(), Integer.valueOf(-1));
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

	public Object getConvertedPropertyWithDefault(String propertyName, boolean designValue, boolean convertValue)
	{
		Object value = null;
		if (!designValue && properties.containsKey(propertyName))
		{
			value = properties.get(propertyName);
		}
		else
		{
			value = getInitialProperty(propertyName);
		}
		return dataAdapterList != null && convertValue ? dataAdapterList.convertFromJavaObjectToString(formElement, propertyName, value) : value;
	}

	public Object getInitialProperty(String propertyName)
	{
		return formElement.getPropertyWithDefault(propertyName);
	}

	@Override
	protected Object convertPropertyValue(String propertyName, Object oldValue, Object propertyValue, ConversionLocation sourceOfValue) throws JSONException
	{
		return (dataAdapterList != null ? dataAdapterList.convertToJavaObject(getFormElement(), propertyName, propertyValue, sourceOfValue, oldValue)
			: propertyValue);
	}

	@Override
	protected void onPropertyChange(String propertyName, Object oldValue, Object propertyValue)
	{
		if (oldValue instanceof IValueList)
		{
			((IValueList)oldValue).removeListDataListener(this);
		}
		else if (oldValue instanceof LookupListModel)
		{
			((LookupListModel)oldValue).removeListDataListener(this);
		}
		if (propertyValue instanceof IValueList)
		{
			((IValueList)propertyValue).addListDataListener(this);
		}
		else if (propertyValue instanceof LookupListModel)
		{
			((LookupListModel)propertyValue).addListDataListener(this);
		}

		if (propertyValue instanceof IComplexPropertyValue && propertyValue != oldValue)
		{
			// TODO in the future we could allow changes to be pushed more granular (JSON subtrees), not only at root property level - as we already do this type of thing in many places
			final String complexPropertyRoot = propertyName;
			if (oldValue instanceof IComplexPropertyValue)
			{
				((IComplexPropertyValue)oldValue).detach();
			}

			// a new complex property is linked to this component; initialize it
			((IComplexPropertyValue)propertyValue).attachToComponent(new IChangeListener()
			{
				@Override
				public void valueChanged()
				{
					flagPropertyChanged(complexPropertyRoot);
					// this must have happened on the event thread, in which case, after each event is fired, a check for changes happen
					// if it didn't happen on the event thread something is really wrong, cause then properties might change while
					// they are being read at the same time by the event thread
				}
			}, this);
		}

		if (propertyChangeSupport != null) propertyChangeSupport.firePropertyChange(propertyName, oldValue, propertyValue);
	}

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
		events.put(eventType, Integer.valueOf(functionID));
	}

	public boolean hasEvent(String eventType)
	{
		return events.containsKey(eventType);
	}

	/**
	 * Executes a handler on the component. If the component has accessible set to false in form security settings an error message is returned.
	 * */
	@Override
	public Object executeEvent(String eventType, Object[] args)
	{
		// verify if component is accessible due to security options
		IPersist persist = formElement.getPersistIfAvailable();
		if (persist != null)
		{
			int access = dataAdapterList.getApplication().getFlattenedSolution().getSecurityAccess(persist.getUUID());
			if (!((access & IRepository.ACCESSIBLE) != 0)) throw new RuntimeException("Security error. Component '" + this.getProperty("name") +
				"' is not accessible.");
		}

		Integer eventId = events.get(eventType);
		if (eventId != null)
		{
			return dataAdapterList.executeEvent(this, eventType, eventId.intValue(), args);
		}
		throw new IllegalArgumentException("Unknown event '" + eventType + "' for component " + this);
	}

	@Override
	public String toString()
	{
		return "<" + getName() + ">";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.event.ListDataListener#intervalAdded(javax.swing.event.ListDataEvent)
	 */
	@Override
	public void intervalAdded(ListDataEvent e)
	{
		valueListChanged(e);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.event.ListDataListener#intervalRemoved(javax.swing.event.ListDataEvent)
	 */
	@Override
	public void intervalRemoved(ListDataEvent e)
	{
		valueListChanged(e);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.event.ListDataListener#contentsChanged(javax.swing.event.ListDataEvent)
	 */
	@Override
	public void contentsChanged(ListDataEvent e)
	{
		valueListChanged(e);
	}

	/**
	 * @param e
	 */
	private void valueListChanged(ListDataEvent e)
	{
		for (Entry<String, Object> entry : properties.entrySet())
		{
			if (entry.getValue() == e.getSource())
			{
				flagPropertyChanged(entry.getKey());
			}
		}
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
			int startIndex = getMaxTabSequence();
			if (formIndex > 0)
			{
				int currentIndex = -1;
				for (IWebFormUI currentForm : visibleForms.keySet())
				{
					int index = visibleForms.get(currentForm);
					if (index < formIndex && index > currentIndex)
					{
						currentIndex = index;
						startIndex = currentForm.getNextAvailableTabSequence();
					}
				}
			}
			int maxTabIndex = form.recalculateTabIndex(startIndex, null);
			if (maxTabIndex > nextAvailableTabSequence)
			{
				// add a 50 numbers gap
				nextAvailableTabSequence = Math.max(maxTabIndex, startIndex + 50);
				// go up in the tree
				if (getParent() != null)
				{
					((IWebFormUI)getParent()).recalculateTabIndex(nextAvailableTabSequence, new TabSequencePropertyWithComponent(this,
						calculatedTabSequence.get(0).getLeft()));
				}
			}
		}
	}

	public void recalculateTabSequence(int availableSequence)
	{
		if (nextAvailableTabSequence < availableSequence)
		{
			// go up in the tree
			if (getParent() != null)
			{
				((IWebFormUI)getParent()).recalculateTabIndex(availableSequence, new TabSequencePropertyWithComponent(this,
					calculatedTabSequence.get(0).getLeft()));
			}
		}
	}

	public void setCalculatedTabSequence(int tabSequence, String propertyName)
	{
		for (Pair<String, Integer> pair : calculatedTabSequence)
		{
			if (Utils.equalObjects(propertyName, pair.getLeft()))
			{
				pair.setRight(Integer.valueOf(tabSequence));
			}
		}
		this.nextAvailableTabSequence = getMaxTabSequence() + 1;
		setProperty(propertyName, Integer.valueOf(tabSequence), ConversionLocation.SERVER);
	}

	private int getMaxTabSequence()
	{
		int maxTabSequence = -200;
		for (Pair<String, Integer> pair : calculatedTabSequence)
		{
			if (maxTabSequence < pair.getRight())
			{
				maxTabSequence = pair.getRight();
			}
		}
		return maxTabSequence;
	}

	public int getNextAvailableTabSequence()
	{
		return nextAvailableTabSequence;
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
	 * Notifies this component that the record it displays has changed.
	 * @return true if any property value changed due to the execution of this method, or false otherwise.
	 */
	public boolean pushRecord(IRecordInternal record)
	{
		boolean changed = false;
		for (Object x : properties.values())
		{
			if (x instanceof IServoyAwarePropertyValue) changed = ((IServoyAwarePropertyValue)x).pushRecord(record) || changed;
		}
		return changed;
	}

	@Override
	public void dispose()
	{
		for (Object p : getProperties().values())
		{
			if (p instanceof IComplexPropertyValue) ((IComplexPropertyValue)p).detach(); // clear any listeners/held resources
		}
		super.dispose();
	}

}