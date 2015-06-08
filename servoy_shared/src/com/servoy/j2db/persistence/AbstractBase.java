/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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
package com.servoy.j2db.persistence;


import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONException;

import com.servoy.j2db.persistence.ContentSpec.Element;
import com.servoy.j2db.persistence.StaticContentSpecLoader.TypedProperty;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.JSONWrapperMap;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;


/**
 * Abstract base class used by all IPersist classes If a sub class implements ISupportChild\IPersistConeble more methods are already provided
 *
 * @author jblok
 */
@SuppressWarnings("nls")
public abstract class AbstractBase implements IPersist
{

	private static final String[] OVERRIDE_PATH = new String[] { "override" }; //$NON-NLS-1$
	public static final int DEFAULT_INT = ContentSpec.ZERO.intValue(); // == ContentSpec.getJavaClassMemberDefaultValue(IRepository.INTEGER)

	/*
	 * Attributes for IPersist
	 */
	protected UUID uuid;
	protected int element_id;
	protected int revision = 1;
	protected boolean isChanged = true;
	protected ISupportChilds parent;
	protected int type;

	/*
	 * All 1-n providers for this class
	 */
	private List<IPersist> allobjects = null;
	private transient Map<UUID, IPersist> allobjectsMap = null;

	private Map<String, Object> propertiesMap = new HashMap<String, Object>();
	private Map<String, Object> bufferPropertiesMap = null;

	/*
	 * Attributes, do not change default values do to repository default_textual_classvalue
	 */
	protected transient JSONWrapperMap jsonCustomProperties = null;

/*
 * _____________________________________________________________ Declaration and definition of constructors
 */
	public AbstractBase(int type, ISupportChilds parent, int element_id, UUID uuid)
	{
		this.type = type;
		this.parent = parent;
		this.element_id = element_id;
		this.uuid = uuid;
	}

	public Map<String, Object> getPropertiesMap()
	{
		return new HashMap<String, Object>(propertiesMap);
	}

	public void clearProperty(String propertyName)
	{
		if (!propertiesMap.containsKey(propertyName))
		{
			return;
		}
		isChanged = true;

		// call the setter with content spec default so any cached data is cleared
		Element element = StaticContentSpecLoader.getContentSpec().getPropertyForObjectTypeByName(getTypeID(), propertyName);
		setProperty(propertyName, element == null ? null : element.getDefaultClassValue());

		propertiesMap.remove(propertyName);
		if (bufferPropertiesMap != null)
		{
			bufferPropertiesMap.remove(propertyName); // the setProperty above might set (wrongly) default value in bufferPropertiesMap as well during import
		}
	}

	public boolean hasProperty(String propertyName)
	{
		return propertiesMap.containsKey(propertyName) || (bufferPropertiesMap != null && bufferPropertiesMap.containsKey(propertyName));
	}

	public void copyPropertiesMap(Map<String, Object> newProperties, boolean overwriteMap)
	{
		if (overwriteMap && propertiesMap.size() > 0)
		{
			// remove properties that are not in newProperties
			for (String key : propertiesMap.keySet().toArray(new String[propertiesMap.size()]))
			{
				if (newProperties == null || !newProperties.containsKey(key))
				{
					clearProperty(key);
				}
			}
		}


		// apply the new properties
		if (newProperties != null)
		{
			try
			{
				startBufferUseForProperties();
				Iterator<Entry<String, Object>> iterator = newProperties.entrySet().iterator();
				while (iterator.hasNext())
				{
					Entry<String, Object> next = iterator.next();
					setProperty(next.getKey(), next.getValue());
				}
			}
			finally
			{
				applyPropertiesBuffer();
			}
		}

	}

	public void setProperty(String propertyName, Object val)
	{
		try
		{
			Map<String, Method> methods = RepositoryHelper.getSettersViaIntrospection(this);
			if (methods.containsKey(propertyName))
			{
				methods.get(propertyName).invoke(this, new Object[] { val });
			}
			else
			{
				Debug.error("No introspection method found for property:" + propertyName + ", on persist " + toString()); //$NON-NLS-1$//$NON-NLS-2$
			}
		}
		catch (Exception ex)
		{
			Debug.error(ex);
		}
	}

	/**
	 * @param propertyName
	 * @param val
	 */
	private void setPropertyInternal(String propertyName, Object val)
	{
		Boolean newPropAndWasChanged = null;
		if (propertiesMap.containsKey(propertyName))
		{
			if (!StaticContentSpecLoader.PROPERTY_NAME.getPropertyName().equals(propertyName))
			{
				checkForChange(propertiesMap.get(propertyName), val);
			}
			else
			{
				checkForNameChange((String)propertiesMap.get(propertyName), (String)val);
			}
		}
		else
		{
			newPropAndWasChanged = Boolean.valueOf(isChanged);
			isChanged = true;
		}
		if (bufferPropertiesMap != null)
		{
			bufferPropertiesMap.put(propertyName, val);
		}
		else
		{
			if (!hasProperty(StaticContentSpecLoader.PROPERTY_EXTENDSID.getPropertyName()) ||
				(this instanceof ISupportExtendsID && Utils.equalObjects(
					Integer.valueOf(((ISupportExtendsID)this).getExtendsID()),
					StaticContentSpecLoader.getContentSpec().getPropertyForObjectTypeByName(getTypeID(),
						StaticContentSpecLoader.PROPERTY_EXTENDSID.getPropertyName()).getDefaultClassValue())))
			{
				Element element = StaticContentSpecLoader.getContentSpec().getPropertyForObjectTypeByName(getTypeID(), propertyName);
				if (element != null && Utils.equalObjects(val, element.getDefaultClassValue()))
				{
					if (newPropAndWasChanged != null)
					{
						// changed was set because property was added, now we remove property again, revert to previous changed state
						isChanged = newPropAndWasChanged.booleanValue();
					}
					propertiesMap.remove(propertyName);
					return;
				}
			}

			propertiesMap.put(propertyName, val);
		}
	}

	public void startBufferUseForProperties()
	{
		if (bufferPropertiesMap == null)
		{
			bufferPropertiesMap = new HashMap<String, Object>();
		}
	}

	public void applyPropertiesBuffer()
	{
		if (bufferPropertiesMap != null)
		{
			Map<String, Object> tempMap = new HashMap<String, Object>(bufferPropertiesMap);
			bufferPropertiesMap = null;
			// apply extendsId first
			if (tempMap.containsKey(StaticContentSpecLoader.PROPERTY_EXTENDSID.getPropertyName()))
			{
				setPropertyInternal(StaticContentSpecLoader.PROPERTY_EXTENDSID.getPropertyName(),
					tempMap.get(StaticContentSpecLoader.PROPERTY_EXTENDSID.getPropertyName()));
				tempMap.remove(StaticContentSpecLoader.PROPERTY_EXTENDSID.getPropertyName());
			}
			for (String propertyName : tempMap.keySet())
			{
				setPropertyInternal(propertyName, tempMap.get(propertyName));
			}

		}
	}

	public Object getProperty(String propertyName)
	{
		Object value = null;
		if (bufferPropertiesMap != null && bufferPropertiesMap.containsKey(propertyName))
		{
			value = bufferPropertiesMap.get(propertyName);
		}
		else if (propertiesMap.containsKey(propertyName))
		{
			value = propertiesMap.get(propertyName);
		}
		else if (!StaticContentSpecLoader.PROPERTY_CUSTOMPROPERTIES.getPropertyName().equals(propertyName) && this instanceof ISupportExtendsID &&
			PersistHelper.isOverrideElement((ISupportExtendsID)this))
		{
			IPersist superPersist = PersistHelper.getSuperPersist((ISupportExtendsID)this);
			if (superPersist != null)
			{
				return ((AbstractBase)superPersist).getProperty(propertyName);
			}

			Element element = StaticContentSpecLoader.getContentSpec().getPropertyForObjectTypeByName(getTypeID(), propertyName);
			if (element != null) value = element.getDefaultClassValue();
		}
		else
		{
			// content spec default value
			Element element = StaticContentSpecLoader.getContentSpec().getPropertyForObjectTypeByName(getTypeID(), propertyName);
			if (element != null) value = element.getDefaultClassValue();
		}


		if (value instanceof Insets)
		{
			return new Insets(((Insets)value).top, ((Insets)value).left, ((Insets)value).bottom, ((Insets)value).right);
		}
		if (value instanceof Dimension)
		{
			return new Dimension((Dimension)value);
		}
		if (value instanceof Point)
		{
			return new Point((Point)value);
		}
		return value;
	}

	@SuppressWarnings("unchecked")
	<T> T getTypedProperty(TypedProperty<T> property)
	{
		return (T)getProperty(property.getPropertyName());
	}

	protected void setTypedProperty(TypedProperty<Integer> property, int value)
	{
		setPropertyInternal(property.getPropertyName(), Integer.valueOf(value));
	}

	protected void setTypedProperty(TypedProperty<Boolean> property, boolean value)
	{
		setPropertyInternal(property.getPropertyName(), Boolean.valueOf(value));
	}

	<T> void setTypedProperty(TypedProperty<T> property, T value)
	{
		if (value instanceof Integer)
		{
			Integer intValue = (Integer)value;
			if (property == StaticContentSpecLoader.PROPERTY_TABSEQ && intValue.intValue() < 1 && intValue.intValue() != ISupportTabSeq.DEFAULT &&
				intValue.intValue() != ISupportTabSeq.SKIP)
			{
				return;//irrelevant value from editor
			}
			if (property == StaticContentSpecLoader.PROPERTY_ROTATION && intValue.intValue() > 360)
			{
				return;
			}
		}
		if (value instanceof String && property.getPropertyName().toLowerCase().endsWith("datasource")) //$NON-NLS-1$
		{
			setPropertyInternal(property.getPropertyName(), ((String)value).intern());
		}
		else
		{
			setPropertyInternal(property.getPropertyName(), value);
		}
	}

	<T> void clearTypedProperty(TypedProperty<T> property)
	{
		if (!propertiesMap.containsKey(property.getPropertyName()))
		{
			return;
		}
		isChanged = true;

		propertiesMap.remove(property.getPropertyName());
	}

	/*
	 * _____________________________________________________________ Methods from IPersist
	 */

	public Object acceptVisitor(IPersistVisitor visitor)
	{
		Object retval = visitor.visit(this);
		if (retval == IPersistVisitor.CONTINUE_TRAVERSAL && this instanceof ISupportChilds)
		{
			Iterator<IPersist> it = getAllObjects();
			while ((retval == IPersistVisitor.CONTINUE_TRAVERSAL || retval == IPersistVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER) && it.hasNext())
			{
				retval = it.next().acceptVisitor(visitor);
			}
		}
		return (retval == IPersistVisitor.CONTINUE_TRAVERSAL || retval == IPersistVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER || retval == IPersistVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_UP)
			? null : retval;
	}

	public Object acceptVisitorDepthFirst(IPersistVisitor visitor) throws RepositoryException
	{
		Object retval = IPersistVisitor.CONTINUE_TRAVERSAL;
		if (this instanceof ISupportChilds)
		{
			Iterator<IPersist> it = getAllObjects();
			while (it.hasNext() &&
				(retval == IPersistVisitor.CONTINUE_TRAVERSAL || retval == IPersistVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER || retval == IPersistVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_UP))
			{
				IPersist visitee = it.next();
				retval = visitee.acceptVisitorDepthFirst(visitor);
			}
		}
		if (retval == IPersistVisitor.CONTINUE_TRAVERSAL || retval == IPersistVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER)
		{
			retval = visitor.visit(this);
		}
		return (retval == IPersistVisitor.CONTINUE_TRAVERSAL || retval == IPersistVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER || retval == IPersistVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_UP)
			? null : retval;
	}

	void clearParent()
	{
		parent = null;
	}

	public void setParent(ISupportChilds parent)
	{
		this.parent = parent;
	}

	public int getID()
	{
		return element_id;
	}

	/*
	 * only called when cloning to set the clone on a new id.
	 */
	public void setID(int id)
	{
		element_id = id;
	}

	public void setRevisionNumber(int revision)
	{
		this.revision = revision;
		clearChanged();
	}

	public int getRevisionNumber()
	{
		return revision;
	}

	public boolean isChanged()
	{
		return isChanged;
	}

	public void flagChanged()
	{
		this.isChanged = true;
	}

	public void clearChanged()
	{
		isChanged = false;
		setRuntimeProperty(NameChangeProperty, null);
	}

	public IRootObject getRootObject()
	{
		return parent.getRootObject();
	}

	public ISupportChilds getParent()
	{
		return parent;
	}

	/*
	 * _____________________________________________________________ Methods from ISupportChilds only visible when subclasses implement ISupportChilds
	 */
	public void removeChild(IPersist obj)
	{
		internalRemoveChild(obj);
		if (getRootObject().getChangeHandler() != null)
		{
			getRootObject().getChangeHandler().fireIPersistRemoved(obj);
		}
	}

	protected void internalRemoveChild(IPersist obj)
	{
		if (allobjects != null)
		{
			allobjects.remove(obj);
			if (allobjectsMap != null && obj != null)
			{
				allobjectsMap.remove(obj.getUUID());
			}
		}
	}

	public void addChild(IPersist obj)
	{
		internalAddChild(obj);
		if (getRootObject().getChangeHandler() != null)
		{
			getRootObject().getChangeHandler().fireIPersistCreated(obj);
		}
		if (obj instanceof AbstractBase && this instanceof ISupportChilds)
		{
			((AbstractBase)obj).setParent((ISupportChilds)this);
		}
	}

	public void internalAddChild(IPersist obj)
	{
		if (allobjects == null)
		{
			allobjects = Collections.synchronizedList(new ArrayList<IPersist>(3));
		}
		allobjects.add(obj);
		if (allobjectsMap != null && obj != null)
		{
			allobjectsMap.put(obj.getUUID(), obj);
		}
	}

	public <T extends IPersist> Iterator<T> getObjects(int tp)
	{
		return new TypeIterator<T>(getAllObjectsAsList(), tp);
	}

	public Iterator<IPersist> getAllObjects()
	{
		return getAllObjectsAsList().iterator();
	}

	public final List<IPersist> getAllObjectsAsList()
	{
		return allobjects == null ? Collections.<IPersist> emptyList() : Collections.unmodifiableList(allobjects);
	}

	public void internalClearAllObjects()
	{
		allobjects = null;
		allobjectsMap = null;
	}

	private void flushAllObjectsMap()
	{
		allobjectsMap = null;
	}

	public IPersist getChild(UUID childUuid)
	{
		if (allobjectsMap == null && allobjects != null && allobjects.size() > 0)
		{
			allobjectsMap = new ConcurrentHashMap<UUID, IPersist>(allobjects.size(), 0.9f, 16);
			for (IPersist persist : allobjects)
			{
				if (persist != null)
				{
					allobjectsMap.put(persist.getUUID(), persist);
				}
			}
		}
		return allobjectsMap == null ? null : allobjectsMap.get(childUuid);
	}

	public Iterator<IPersist> getAllObjects(Comparator< ? super IPersist> c)
	{
		return Utils.asSortedIterator(getAllObjects(), c);
	}

	/**
	 * @see java.lang.Object#equals(Object)
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (obj == this) return true;
		if (obj != null && obj.getClass() == getClass())
		{
			AbstractBase abstractBase = (AbstractBase)obj;
			if (abstractBase.getUUID().equals(uuid))
			{
				if (getParent() != null && abstractBase.getParent() != null)
				{
					return getParent().equals(abstractBase.getParent());
				}
				return true;
			}
		}
		return false;
	}

	private final int hashCodeNr = new Object().hashCode();

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		// Use separate hashcode object so hashCode() is stable for cloned object, when used as key in map a clone should match.
		return hashCodeNr;
	}

	protected void checkForNameChange(String oldValue, String newValue)
	{
		if (oldValue == null && newValue == null) return;
		checkForChange(oldValue, newValue);
		if ((oldValue == null && newValue != null) || !oldValue.equals(newValue))
		{
			if (getRuntimeProperty(NameChangeProperty) == null) //only do once, first time
			{
				if (oldValue == null)
				{
					setRuntimeProperty(NameChangeProperty, ""); //$NON-NLS-1$
				}
				else
				{
					setRuntimeProperty(NameChangeProperty, oldValue);
				}
			}
		}
	}

	public static final RuntimeProperty<String> NameChangeProperty = new RuntimeProperty<String>()
	{
	};


	public static final SerializableRuntimeProperty<HashMap<UUID, Integer>> UUIDToIDMapProperty = new SerializableRuntimeProperty<HashMap<UUID, Integer>>()
	{
		private static final long serialVersionUID = 1L;
	};

	public static final SerializableRuntimeProperty<HashMap<String, String>> UnresolvedPropertyToValueMapProperty = new SerializableRuntimeProperty<HashMap<String, String>>()
	{
		private static final long serialVersionUID = 1L;
	};

	private void checkForChange(Object oldValue, Object newValue)
	{
		if (isChanged) return;//no need to check

		boolean retval = false;

		//null checks
		if (oldValue == null)
		{
			retval = (newValue != null);
		}
		else
		{
			retval = (!oldValue.equals(newValue));
		}

		if (retval) isChanged = true;
	}

	/**
	 * @see java.lang.Object#clone()
	 */
	public IPersist clonePersist()
	{
		try
		{
			AbstractBase cloned = (AbstractBase)super.clone();
			cloned.allobjectsMap = null;
			cloned.propertiesMap = new HashMap<String, Object>();
			cloned.copyPropertiesMap(getPropertiesMap(), true);
			if (cloned.allobjects != null)
			{
				cloned.allobjects = Collections.synchronizedList(new ArrayList<IPersist>(allobjects.size()));
				for (IPersist persist : allobjects)
				{
					if (persist instanceof ICloneable)
					{
						IPersist clonePersist = ((ICloneable)persist).clonePersist();
						cloned.addChild(clonePersist);
//						cloned.allobjects.add(clonePersist);
					}
					else
					{
						cloned.allobjects.add(persist);
					}
				}
			}
			return cloned;
		}
		catch (CloneNotSupportedException e)
		{
			throw new RuntimeException(e);
		}
	}


	/**
	 * Make a clone of the current obj (also makes new repository entry)
	 *
	 * @return a clone from this object
	 */
	public IPersist cloneObj(ISupportChilds newParent, boolean deep, IValidateName validator, boolean changeName, boolean changeChildNames,
		boolean flattenOverrides) throws RepositoryException
	{
		ChangeHandler changeHandler = null;
		if (newParent != null)
		{
			changeHandler = newParent.getRootObject().getChangeHandler();
		}
		else
		{
			changeHandler = getRootObject().getChangeHandler();
		}
		if (changeHandler == null)
		{
			throw new RepositoryException("cannot clone/copy without change handler"); //$NON-NLS-1$
		}
		AbstractBase clone = (AbstractBase)changeHandler.cloneObj(this, newParent, flattenOverrides);
		if (changeName && clone instanceof ISupportUpdateableName && ((ISupportUpdateableName)clone).getName() != null)
		{
			int random = new Random().nextInt(1024);
			String newName = ((ISupportUpdateableName)clone).getName() + "_copy" + random; //$NON-NLS-1$
			((ISupportUpdateableName)clone).updateName(validator, newName);
		}
		if (clone instanceof ISupportChilds)//do deep clone
		{
			clone.allobjectsMap = null;
			if (deep && allobjects != null)
			{
				clone.allobjects = Collections.synchronizedList(new ArrayList<IPersist>(allobjects.size()));
				Iterator<IPersist> it = this.getAllObjects();
				while (it.hasNext())
				{
					IPersist element = it.next();
					if (element instanceof IPersistCloneable)
					{
						((IPersistCloneable)element).cloneObj((ISupportChilds)clone, deep, validator, changeChildNames, changeChildNames, flattenOverrides);
					}
				}
			}
			else
			{
				clone.allobjects = null;//clear so they are not shared due to native clone !
			}
		}
		return clone;
	}

	public int getTypeID()
	{
		return type;
	}

	public UUID getUUID()
	{
		return uuid;
	}

	public void resetUUID()
	{
		resetUUID(null);
	}

	public void resetUUID(UUID uuidParam)
	{
		if (parent instanceof AbstractBase)
		{
			((AbstractBase)parent).flushAllObjectsMap();
		}
		if (uuidParam == null) uuid = UUID.randomUUID();
		else uuid = uuidParam;
	}

	public IPersist getAncestor(int typeId)
	{
		if (getTypeID() == typeId)
		{
			return this;
		}
		if (parent == null)
		{
			return null;
		}
		return parent.getAncestor(typeId);
	}

	public MetaData getMetaData()
	{
		return null;
	}

	public static <T> T selectByName(Iterator<T> iterator, String name)
	{
		return selectByProperty(iterator, StaticContentSpecLoader.PROPERTY_NAME, name);
	}

	public static <T, P> T selectByProperty(Iterator<T> iterator, TypedProperty<P> property, P value)
	{
		if (value == null || (value instanceof String && ((String)value).trim().length() == 0)) return null;

		while (iterator.hasNext())
		{
			T n = iterator.next();
			if (n instanceof AbstractBase && value.equals(((AbstractBase)n).getProperty(property.getPropertyName())))
			{
				return n;
			}
		}
		return null;
	}

	public static <T extends IPersist> T selectById(Iterator<T> iterator, int id)
	{
		while (iterator.hasNext())
		{
			T p = iterator.next();
			if (p.getID() == id)
			{
				return p;
			}
		}
		return null;
	}


	/**
	 * Set the customProperties
	 *
	 * <b>Note: this call is only for (de)serialisation, use putCustomProperty to set specific custom properties </b>
	 *
	 * @param arg the customProperties
	 * @throws JSONException
	 */
	public void setCustomProperties(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_CUSTOMPROPERTIES, arg);
		jsonCustomProperties = null;
	}

	/**
	 * Get the customProperties
	 *
	 * <b>Note: this call is only for (de)serialisation, use getCustomProperty to get specific custom properties </b>
	 *
	 * @return the customProperties
	 */
	public String getCustomProperties()
	{
		if (jsonCustomProperties != null)
		{
			return jsonCustomProperties.toString();
		}
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_CUSTOMPROPERTIES);
	}

	public Object getCustomProperty(String[] path)
	{
		IPersist persist = this;
		while (persist instanceof AbstractBase)
		{
			Object customProperty = ((AbstractBase)persist).getCustomPropertyLocal(path);
			if (customProperty != null)
			{
				return customProperty;
			}
			if (persist instanceof ISupportExtendsID)
			{
				persist = PersistHelper.getSuperPersist((ISupportExtendsID)persist);
			}
			else
			{
				break;
			}
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	protected Object getCustomPropertyLocal(String[] path)
	{
		String customProperties = getTypedProperty(StaticContentSpecLoader.PROPERTY_CUSTOMPROPERTIES);

		if (customProperties == null) return null;

		if (jsonCustomProperties == null)
		{
			jsonCustomProperties = new JSONWrapperMap(customProperties);
		}
		try
		{
			Map<String, Object> map = jsonCustomProperties;
			for (int i = 0; i < path.length; i++)
			{
				if (map == null || !map.containsKey(path[i]))
				{
					return null;
				}
				Object node = ServoyJSONObject.toJava(map.get(path[i]));
				if (i == path.length - 1)
				{
					// leaf node
					return node;
				}
				map = (Map<String, Object>)node;
			}
		}
		catch (Exception ex)
		{
			Debug.error(ex);
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public Object putCustomProperty(String[] path, Object value)
	{
		String customProperties = getTypedProperty(StaticContentSpecLoader.PROPERTY_CUSTOMPROPERTIES);

		if (customProperties == null && value == null) return null;

		if (jsonCustomProperties == null)
		{
			if (customProperties != null)
			{
				jsonCustomProperties = new JSONWrapperMap(customProperties);
			}
			else
			{
				jsonCustomProperties = new JSONWrapperMap(new ServoyJSONObject());
			}
		}

		Map<String, Object> map = jsonCustomProperties;
		for (int i = 0; i < path.length - 1; i++)
		{
			if (!map.containsKey(path[i]))
			{
				if (value == null)
				{
					return null; // value not found
				}
				map.put(path[i], new ServoyJSONObject());
			}
			map = (Map<String, Object>)map.get(path[i]);

		}
		String leaf = path[path.length - 1];
		Object old = null;
		if (value == null)
		{
			old = map.remove(leaf);
			if (map.isEmpty() && path.length > 1)
			{
				// remove empty map
				putCustomProperty(Utils.arraySub(path, 0, path.length - 1), null);
			}
		}
		else
		{
			old = map.put(leaf, value);
		}
		setTypedProperty(StaticContentSpecLoader.PROPERTY_CUSTOMPROPERTIES, jsonCustomProperties.toString());
		return old;
	}

	public Map<String, Object> getCustomDesignTimeProperties()
	{
		Map<String, Object> map = (Map<String, Object>)getCustomProperty(new String[] { "design" });
		if (map == null || map.size() == 0)
		{
			return null;
		}
		return map;
	}

	/**
	 * Merge properties from this, super, ...
	 * This will always return a non-null map of properties which is a map that can be written to.
	 */
	public Map<String, Object> getMergedCustomDesignTimeProperties()
	{
		return getMergedCustomDesignTimePropertiesInternal(new HashMap<String, Object>());
	}

	private Map<String, Object> getMergedCustomDesignTimePropertiesInternal(Map<String, Object> mergedProperties)
	{
		if (this instanceof ISupportExtendsID)
		{
			IPersist superPersist = PersistHelper.getSuperPersist((ISupportExtendsID)this);
			if (superPersist instanceof AbstractBase)
			{
				((AbstractBase)superPersist).getMergedCustomDesignTimePropertiesInternal(mergedProperties);
			}
		}
		Map<String, Object> map = getCustomDesignTimeProperties();
		if (map != null)
		{
			mergedProperties.putAll(map);
		}
		return mergedProperties;
	}

	/**
	 * Unmerge properties from this, super, ... by removing elements that are defined with equal value in super persist.
	 */
	public void setUnmergedCustomDesignTimeProperties(Map<String, Object> mergedProperties)
	{
		Map<String, Object> map = mergedProperties;
		if (map != null)
		{
			IPersist superPersist = this instanceof ISupportExtendsID ? PersistHelper.getSuperPersist((ISupportExtendsID)this) : null;
			if (superPersist instanceof AbstractBase)
			{
				Map<String, Object> superMergedProperties = ((AbstractBase)superPersist).getMergedCustomDesignTimeProperties();
				if (superMergedProperties != null)
				{
					for (Entry<String, Object> superEntry : superMergedProperties.entrySet())
					{
						Object subval = map.get(superEntry.getKey());
						if (subval != null && subval.equals(superEntry.getValue()))
						{
							// unmerge values same as superpersist
							if (map == mergedProperties)
							{
								// make copy, do not modify original
								map = new HashMap<String, Object>(mergedProperties);
							}
							map.remove(superEntry.getKey());
						}
					}
				}
			}
		}

		setCustomDesignTimeProperties(map);
	}

	public Map<String, Object> setCustomDesignTimeProperties(Map<String, Object> map)
	{
		return (Map<String, Object>)putCustomProperty(new String[] { "design" }, map);
	}

	public Object getCustomDesignTimeProperty(String key)
	{
		if (key != null)
		{
			return getCustomProperty(new String[] { "design", key }); //$NON-NLS-1$
		}
		return null;
	}

	public Object putCustomDesignTimeProperty(String key, Object value)
	{
		if (key != null)
		{
			return putCustomProperty(new String[] { "design", key }, value); //$NON-NLS-1$
		}
		return null;
	}

	public Map<String, Object> getCustomMobileProperties()
	{
		Map<String, Object> map = (Map<String, Object>)getCustomProperty(new String[] { "mobile" });
		if (map == null || map.size() == 0)
		{
			return null;
		}
		return map;
	}

	public Map<String, Object> setCustomMobileProperties(Map<String, Object> map)
	{
		return (Map<String, Object>)putCustomProperty(new String[] { "mobile" }, map);
	}

	public Object getCustomMobileProperty(String key)
	{
		if (key != null)
		{
			return getCustomProperty(new String[] { "mobile", key }); //$NON-NLS-1$
		}
		return null;
	}

	public Object putCustomMobileProperty(String key, Object value)
	{
		if (key != null)
		{
			return putCustomProperty(new String[] { "mobile", key }, value); //$NON-NLS-1$
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public List<Object> getInstanceMethodArguments(String methodKey)
	{
		if (methodKey != null)
		{
			return (List<Object>)getCustomProperty(new String[] { "methods", methodKey, "arguments" }); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public List<Object> putInstanceMethodArguments(String methodKey, List<Object> args)
	{
		if (methodKey != null)
		{
			return (List<Object>)putCustomProperty(new String[] { "methods", methodKey, "arguments" }, args == null ? null : Collections.unmodifiableList(args)); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public Pair<List<Object>, List<Object>> getInstanceMethodParametersLocal(String methodKey)
	{
		if (methodKey != null)
		{
			List<Object> params = (List<Object>)getCustomPropertyLocal(new String[] { "methods", methodKey, "parameters" }); //$NON-NLS-1$ //$NON-NLS-2$
			List<Object> args = (List<Object>)getCustomPropertyLocal(new String[] { "methods", methodKey, "arguments" }); //$NON-NLS-1$ //$NON-NLS-2$
			return new Pair<List<Object>, List<Object>>(params, args);
		}
		return null;
	}

	public List<Object> putInstanceMethodParameters(String methodKey, List<Object> paramNames, List<Object> args)
	{
		if (methodKey != null)
		{
			putCustomProperty(new String[] { "methods", methodKey, "parameters" }, args == null ? null : Collections.unmodifiableList(paramNames));
			putCustomProperty(new String[] { "methods", methodKey, "arguments" }, args == null ? null : Collections.unmodifiableList(args));
		}

		return null;
	}

	public boolean hasOverrideCustomProperty()
	{
		return (getCustomProperty(OVERRIDE_PATH) != null);
	}

	public void removeOverrideCustomProperty()
	{
		putCustomProperty(OVERRIDE_PATH, null);
	}

	/** Check if this object has any overriding properties left.
	 * @return
	 */
	public boolean hasOverrideProperties()
	{
		for (Entry<String, Object> entry : propertiesMap.entrySet())
		{
			if (!StaticContentSpecLoader.PROPERTY_EXTENDSID.getPropertyName().equals(entry.getKey()))
			{
				// a non override property
				return true;
			}
		}

		// nothing else found
		return false;
	}

	/* Runtime properties */
	/** Application level meta data. */
	private PropertyEntry[] properties;

	public <T extends Serializable> T getSerializableRuntimeProperty(SerializableRuntimeProperty<T> property)
	{
		return property.get(properties);
	}

	public <T extends Serializable> void setSerializableRuntimeProperty(SerializableRuntimeProperty<T> property, T object)
	{
		properties = property.set(properties, object);
	}

	private transient PropertyEntry[] transient_properties;

	public <T extends Object> T getRuntimeProperty(RuntimeProperty<T> property)
	{
		return property.get(transient_properties);
	}

	public <T extends Object> void setRuntimeProperty(RuntimeProperty<T> property, T object)
	{
		transient_properties = property.set(transient_properties, object);
	}
}
