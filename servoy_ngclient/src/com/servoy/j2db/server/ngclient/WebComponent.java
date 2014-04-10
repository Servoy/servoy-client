package com.servoy.j2db.server.ngclient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import org.json.JSONException;
import org.json.JSONObject;

import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.dataprocessing.LookupListModel;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.server.ngclient.component.WebComponentApiDefinition;
import com.servoy.j2db.server.ngclient.property.PropertyDescription;
import com.servoy.j2db.server.ngclient.property.PropertyType;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.SortedList;
import com.servoy.j2db.util.Utils;


public class WebComponent implements ListDataListener
{
	private final String name;
	protected final Map<String, Object> properties = new HashMap<>();
	private final Map<String, Integer> events = new HashMap<>();
	protected IDataAdapterList dataAdapterList;

	private final Set<String> changedProperties = new HashSet<>(3);
	private final FormElement formElement;
	private final IWebFormUI parentForm;
	private final Map<IWebFormUI, Integer> visibleForms = new HashMap<IWebFormUI, Integer>();
	// list of all tabseq properties ordered by design time value; tabseq will be updated with runtime value
	private final List<Pair<String, Integer>> calculatedTabSequence = new ArrayList<Pair<String, Integer>>();
	// the next available tab sequence number (after this component and all its subtree)
	protected int nextAvailableTabSequence;

	protected WebComponent(String name, Form form)
	{
		this(name, new FormElement(form), null, null);
	}

	public WebComponent(String name, FormElement fe, IDataAdapterList dataAdapterList, IWebFormUI parentForm)
	{
		this.name = name;
		this.formElement = fe;
		this.dataAdapterList = dataAdapterList;
		this.parentForm = parentForm;
		properties.put("name", name);
		if (fe.getLabel() != null)
		{
			properties.put("markupId", ComponentFactory.getMarkupId(fe.getForm().getName(), name));
		}
		if (fe.getWebComponentSpec(false) != null)
		{
			Map<String, PropertyDescription> tabSeqProps = fe.getWebComponentSpec().getProperties(PropertyType.tabseq);
			List<PropertyDescription> sortedList = new SortedList(new Comparator<PropertyDescription>()
			{

				@Override
				public int compare(PropertyDescription o1, PropertyDescription o2)
				{
					int tabSeq1 = Utils.getAsInteger(WebComponent.this.getInitialProperty(o1.getName()));
					int tabSeq2 = Utils.getAsInteger(WebComponent.this.getInitialProperty(o2.getName()));
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
				calculatedTabSequence.add(new Pair(pd.getName(), Utils.getAsInteger(getInitialProperty(pd.getName()))));
			}
			nextAvailableTabSequence = getMaxTabSequence() + 1;
			if (fe.getPersist() instanceof GraphicalComponent && ((GraphicalComponent)fe.getPersist()).getOnActionMethodID() <= 0)
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


	/**
	 * putting new data in recording changes.
	 * 
	 * @param propertyName
	 * @param propertyValue
	 */
	public boolean putProperty(String propertyName, Object propertyValue)
	{
		try
		{
			propertyValue = convertValue(propertyName, propertyValue);
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		String ownProperty = propertyName;
		String lastProperty = propertyName;
		Map<String, Object> map = properties;
		String[] split = propertyName.split("\\.");
		if (split.length > 1)
		{
			ownProperty = split[0];
			for (int i = 0; i < split.length - 1; i++)
			{
				Map<String, Object> propertyMap = (Map<String, Object>)map.get(split[i]);
				if (propertyMap == null)
				{
					propertyMap = new HashMap<>();
					map.put(split[i], propertyMap);
				}
				map = propertyMap;
			}
			lastProperty = split[split.length - 1];
		}
		if (map.containsKey(lastProperty))
		{
			Object oldValue = map.put(lastProperty, propertyValue);
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
			if (!Utils.equalObjects(propertyValue, oldValue))
			{
				changedProperties.add(ownProperty);
				return true;
			}
		}
		else
		{
			map.put(lastProperty, propertyValue);
			if (propertyValue instanceof IValueList)
			{
				((IValueList)propertyValue).addListDataListener(this);
			}
			else if (propertyValue instanceof LookupListModel)
			{
				((LookupListModel)propertyValue).addListDataListener(this);
			}
			changedProperties.add(ownProperty);
			return true;
		}
		return false;
	}

	public Object getProperty(String propertyName)
	{
		return properties.get(propertyName);
	}

	public Object getConvertedPropertyWithDefault(String propertyName, boolean designValue)
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
		return dataAdapterList != null ? dataAdapterList.convertFromJavaObjectToString(formElement, propertyName, value) : value;
	}

	public Object getInitialProperty(String propertyName)
	{
		return formElement.getPropertyWithDefault(propertyName);
	}

	private Object convertValue(String propertyName, Object propertyValue) throws JSONException
	{
		return dataAdapterList != null ? dataAdapterList.convertToJavaObject(formElement, propertyName, propertyValue) : propertyValue;
	}

	/**
	 * put property from the outside world, not recording changes.
	 * converting to the right type.
	 * @param propertyName
	 * @param propertyValue can be a JSONObject or array or primitive.
	 */
	public void putBrowserProperty(String propertyName, Object propertyValue) throws JSONException
	{
		// currently we keep Java objects in here; we could switch to having only json objects in here is it make things quicker
		// (then whenever a server-side value is put in the map, convert it via JSONUtils.toJSONValue())
		//TODO remove this when hierarchical tree structure comes into play (only needed for ) 
		if (propertyValue instanceof JSONObject)
		{
			Iterator it = ((JSONObject)propertyValue).keys();
			while (it.hasNext())
			{
				String key = (String)it.next();
				properties.put(propertyName + '.' + key, ((JSONObject)propertyValue).get(key));
			}
		}// end TODO REMOVE 
		properties.put(propertyName, convertValue(propertyName, propertyValue));
	}

	public String getName()
	{
		return name;
	}

	public Map<String, Object> getChanges()
	{
		if (changedProperties.size() > 0)
		{
			Map<String, Object> changes = new HashMap<>();
			for (String propertyName : changedProperties)
			{
				changes.put(propertyName, properties.get(propertyName));
			}
			changedProperties.clear();
			return changes;
		}
		return Collections.emptyMap();
	}

	public Map<String, Object> getProperties()
	{
		changedProperties.clear();
		return properties;
	}

	public void add(String eventType, int functionID)
	{
		events.put(eventType, Integer.valueOf(functionID));
	}

	public boolean hasEvent(String eventType)
	{
		return events.containsKey(eventType);
	}

	public Object execute(String eventType, Object[] args)
	{
		Integer eventId = events.get(eventType);
		if (eventId != null)
		{
			return dataAdapterList.execute(this, eventType, eventId.intValue(), args);
		}
		throw new IllegalArgumentException("Unknown event '" + eventType + "' for component " + this);
	}

	public Object executeApi(WebComponentApiDefinition apiDefinition, Object[] args)
	{
		return dataAdapterList.executeApi(apiDefinition, getName(), args);
	}

	@Override
	public String toString()
	{
		return "<" + name + ">";
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
				changedProperties.add(entry.getKey());
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
				if (parentForm != null)
				{
					parentForm.recalculateTabIndex(nextAvailableTabSequence, new TabSequencePropertyWithComponent(this, calculatedTabSequence.get(0).getLeft()));
				}
			}
		}
	}

	public void recalculateTabSequence(int availableSequence)
	{
		if (nextAvailableTabSequence < availableSequence)
		{
			// go up in the tree
			if (parentForm != null)
			{
				parentForm.recalculateTabIndex(availableSequence, new TabSequencePropertyWithComponent(this, calculatedTabSequence.get(0).getLeft()));
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
		putProperty(propertyName, Integer.valueOf(tabSequence));
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
}
