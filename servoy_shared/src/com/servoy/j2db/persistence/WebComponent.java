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
import java.awt.Point;
import java.beans.IntrospectionException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * @author gboros
 */
public class WebComponent extends BaseComponent implements IWebComponent
{

	private static final long serialVersionUID = 1L;

	private static final boolean sabloLoaded;

	protected static Set<String> purePersistPropertyNames;

	static
	{
		boolean loaded = false;
		try
		{
			Class.forName("org.sablo.BaseWebObject");
			loaded = true;
		}
		catch (ClassNotFoundException e)
		{
			// it's not found so sablo is not loaded
		}
		sabloLoaded = loaded;

		try
		{
			purePersistPropertyNames = RepositoryHelper.getSettersViaIntrospection(WebComponent.class).keySet();
		}
		catch (IntrospectionException e)
		{
			purePersistPropertyNames = new HashSet<String>();
			Debug.error(e);
		}
	}

	protected transient WebObjectBasicImpl webObjectImpl;

	protected WebComponent(ISupportChilds parent, int element_id, UUID uuid)
	{
		super(IRepository.WEBCOMPONENTS, parent, element_id, uuid);
		webObjectImpl = createWebObjectImpl();
	}

	public WebObjectBasicImpl getImplementation()
	{
		return webObjectImpl;
	}

	// we can't add this here! this class should never use classes from org.sablo as it might end up in smart client which doesn't have those classes, not just in ngclient
//	@Override
//	public PropertyDescription getPropertyDescription()
//	{
//		return webObjectImpl instanceof WebObjectImpl ? ((WebObjectImpl)webObjectImpl).getPropertyDescription() : null;
//	}

	protected WebObjectBasicImpl createWebObjectImpl()
	{
		return sabloLoaded ? new WebObjectImpl(this) : new WebObjectBasicImpl(this);
	}

	private void writeObject(java.io.ObjectOutputStream stream) throws java.io.IOException
	{
		stream.defaultWriteObject();
	}

	private void readObject(java.io.ObjectInputStream stream) throws java.io.IOException, ClassNotFoundException
	{
		stream.defaultReadObject();
		webObjectImpl = createWebObjectImpl();
	}

	@Override
	public IWebComponent getParentComponent()
	{
		return this;
	}

	@Override
	public void clearChanged()
	{
		super.clearChanged();
		for (IChildWebObject x : getAllPersistMappedProperties())
		{
			if (x != null && x.isChanged()) x.clearChanged();
		}
	}

	@Override
	public void updateJSON()
	{
		webObjectImpl.updateJSONFromPersistMappedProperties();
	}

	@Override
	public void setProperty(String propertyName, Object val)
	{
		if (hasPersistProperty(propertyName)) super.setProperty(propertyName, val);
		else webObjectImpl.setProperty(propertyName, val);
	}

	@Override
	protected void setPropertyInternal(String propertyName, Object val)
	{
		super.setPropertyInternal(propertyName, val);
		webObjectImpl.setProperty(propertyName, val);
	}

	@Override
	public void clearProperty(String propertyName)
	{
		if (IContentSpecConstants.PROPERTY_TYPENAME.equals(propertyName)) return;
		super.clearProperty(propertyName);
		webObjectImpl.clearProperty(propertyName);
	}

	@Override
	public Object getProperty(String propertyName)
	{
		Object value = null;
		if (webObjectImpl == null || hasPersistProperty(propertyName)) value = super.getProperty(propertyName);
		if (value == null) value = webObjectImpl.getProperty(propertyName);
		return value;
	}

	@Override
	public Object getOwnPropertyOrDefault(String propertyName)
	{
		Object value = null;
		if (webObjectImpl == null || hasPersistProperty(propertyName)) value = getOwnProperty(propertyName);
		if (value == null && webObjectImpl != null)
		{
			value = makeCopy(webObjectImpl.getOwnProperty(propertyName));
		}
		return value != null ? value : super.getOwnPropertyOrDefault(propertyName);
	}

	@Override
	public Object getPropertyDefaultValueClone(String propertyName)
	{
		return webObjectImpl != null ? webObjectImpl.getPropertyDefaultValue(propertyName) : null;
	}

	@Override
	public boolean hasProperty(String propertyName)
	{
		boolean hasIt = false;
		if (webObjectImpl == null) hasIt = super.hasProperty(propertyName);
		else
		{
			hasIt = webObjectImpl.hasProperty(propertyName);
			if (!hasIt) hasIt = super.hasProperty(propertyName);
		}
		return hasIt;
	}

	protected boolean hasPersistProperty(String propertyName)
	{
		return purePersistPropertyNames.contains(propertyName);
	}

	/**
	 * Returns all direct child typed properties or array of such typed properties.
	 */
	protected List<IChildWebObject> getAllPersistMappedProperties()
	{
		return webObjectImpl.getAllPersistMappedProperties();
	}

//	public Map<String, Object> getPersistMappedPropertiesReadOnly()
//	{
//		return webObjectImpl.getPersistMappedPropertiesReadOnly();
//	}

	public void setTypeName(String arg)
	{
		webObjectImpl.setTypeName(arg);
	}

	public String getTypeName()
	{
		return webObjectImpl.getTypeName();
	}

	/**
	 * DO NOT USE this method! Use setProperty instead.
	 * @param arg
	 */
	public void setJson(JSONObject arg)
	{
		if (arg != null && !(arg instanceof ServoyJSONObject)) throw new RuntimeException("ServoyJSONObject is needed here in order to make it serializable");
		webObjectImpl.setJson(arg);
	}

	public JSONObject getJson()
	{
		JSONObject x = webObjectImpl.getJson();
		try
		{
			return x == null ? x : new ServoyJSONObject(x, ServoyJSONObject.getNames(x), false, true);
		}
		catch (JSONException e)
		{
			Debug.error(e);
			return x;
		}
	}

	public JSONObject getFlattenedJson()
	{
		JSONObject json = getJson();
		IPersist superPersist = PersistHelper.getSuperPersist(this);
		if (superPersist instanceof WebComponent)
		{
			JSONObject superJson = ((WebComponent)superPersist).getFlattenedJson();
			if (superJson != null)
			{
				if (json != null)
				{
					Iterator it = json.keys();
					while (it.hasNext())
					{
						String key = (String)it.next();
						try
						{
							superJson.put(key, json.get(key));
						}
						catch (JSONException e)
						{
							Debug.error(e);
						}
					}
				}
				json = superJson;
			}
		}
		return json;
	}

	@Override
	protected void internalRemoveChild(IPersist obj)
	{
		webObjectImpl.internalRemoveChild(obj);
	}

	@Override
	public void internalAddChild(IPersist obj)
	{
		webObjectImpl.setChild(obj);
	}

	@Override
	public void setChild(IChildWebObject child)
	{
		webObjectImpl.setChild(child);
		afterChildWasAdded(child);
	}

	@Override
	public void insertChild(IChildWebObject child)
	{
		webObjectImpl.insertChild(child);
		afterChildWasAdded(child);
	}

	@Override
	public Iterator<IPersist> getAllObjects()
	{
		return webObjectImpl.getAllObjects();
	}

	@Override
	public List<IPersist> getAllObjectsAsList()
	{
		return Utils.asList(getAllObjects());
	}

	@Override
	public <T extends IPersist> Iterator<T> getObjects(int tp)
	{
		return webObjectImpl.getObjects(tp);
	}

	@Override
	public IPersist getChild(UUID childUuid)
	{
		return webObjectImpl.getChild(childUuid);
	}

	@Override
	protected void fillClone(AbstractBase cloned)
	{
		if (cloned instanceof WebComponent)
			((WebComponent)cloned).webObjectImpl = sabloLoaded ? new WebObjectImpl((WebComponent)cloned) : new WebObjectBasicImpl((WebComponent)cloned);
		super.fillClone(cloned);
	}

	@Override
	public IPersist cloneObj(ISupportChilds newParent, boolean deep, IValidateName validator, boolean changeName, boolean changeChildNames,
		boolean flattenOverrides) throws RepositoryException
	{
		IPersist clone = super.cloneObj(newParent, deep, validator, changeName, changeChildNames, flattenOverrides);
		if (deep && clone instanceof WebComponent)
		{
			List<WebCustomType> types = new ArrayList<WebCustomType>();
			clone.acceptVisitor(new IPersistVisitor()
			{
				@Override
				public Object visit(IPersist o)
				{
					if (o instanceof WebCustomType)
					{
						((WebCustomType)o).resetUUID();
						types.add((WebCustomType)o);
					}
					return IPersistVisitor.CONTINUE_TRAVERSAL;
				}
			});
			((WebComponent)clone).updateJSON();
			for (WebCustomType customType : types)
			{
				clone.getRootObject().getChangeHandler().fireIPersistChanged(customType);
			}
			// hack for cache, we put back the custom types
			this.acceptVisitor(new IPersistVisitor()
			{
				@Override
				public Object visit(IPersist o)
				{
					if (o instanceof WebCustomType)
					{
						getRootObject().getChangeHandler().fireIPersistChanged(o);
					}
					return IPersistVisitor.CONTINUE_TRAVERSAL;
				}
			});
		}
		return clone;
	}

	@Override
	public void setExtendsID(int arg)
	{
		super.setExtendsID(arg);
		webObjectImpl.reload(true);
	}

	/**
	 * returns the attribute value of the given attribute name.
	 * these attributes will be generated on the tag.
	 *
	 * @param name
	 * @return the value of the attribute
	 */
	public String getAttribute(String name)
	{
		Object value = getCustomProperty(new String[] { IContentSpecConstants.PROPERTY_ATTRIBUTES, name });
		if (value instanceof String) return (String)value;
		return null;
	}

	@Override
	public java.awt.Dimension getSize()
	{
		Dimension size = getTypedProperty(StaticContentSpecLoader.PROPERTY_SIZE);
		if (size == null)
		{
			Object defaultSize = getPropertyDefaultValueClone(StaticContentSpecLoader.PROPERTY_SIZE.getPropertyName());
			if (defaultSize instanceof JSONObject)
			{
				return new java.awt.Dimension(((JSONObject)defaultSize).optInt("width"), ((JSONObject)defaultSize).optInt("height")); //$NON-NLS-1$ //$NON-NLS-2$
			}
			return new java.awt.Dimension(140, 20);
		}
		return size;
	}

	@Override
	public java.awt.Point getLocation()
	{
		java.awt.Point point = getTypedProperty(StaticContentSpecLoader.PROPERTY_LOCATION);
		if (point == null)
		{
			Object defaultLocation = getPropertyDefaultValueClone(StaticContentSpecLoader.PROPERTY_LOCATION.getPropertyName());
			if (defaultLocation instanceof JSONObject)
			{
				return new Point(((JSONObject)defaultLocation).optInt("x"), ((JSONObject)defaultLocation).optInt("y")); //$NON-NLS-1$ //$NON-NLS-2$
			}
			point = new Point(10, 10);
		}
		return point;
	}

	@Override
	public String toString()
	{
		return getClass().getSimpleName() + " -> " + webObjectImpl.toString(); //$NON-NLS-1$
	}
}
