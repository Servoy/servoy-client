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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.beans.IntrospectionException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;

import com.servoy.j2db.server.shared.IUnresolvedUUIDResolver;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Internalize;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.TreeBidiMap;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;


/**
 * @author jcompagner
 *
 */
public abstract class AbstractPersistFactory implements IPersistFactory
{
	protected final TreeBidiMap<UUID, Integer> uuid_element_id_map; //uuid->element_id
	private ContentSpec contentSpec = null;

	/**
	 *
	 */
	public AbstractPersistFactory()
	{
		super();
		uuid_element_id_map = new TreeBidiMap<UUID, Integer>();
	}

	public abstract void initClone(IPersist clone, IPersist objToClone, boolean flattenOverrides) throws RepositoryException;

	public int getElementIdForUUID(UUID uuid) throws RepositoryException
	{
		Integer element_id = uuid_element_id_map.get(uuid);
		if (element_id != null)
		{
			return element_id.intValue();
		}

		int resolved = resolveIdForElementUuid(uuid);
		element_id = new Integer(resolved);
		if (!uuid_element_id_map.containsKey(uuid) && !uuid_element_id_map.containsValue(element_id))
		{
			uuid_element_id_map.put(uuid, element_id);
		}
		return resolved;
	}

	public abstract int resolveIdForElementUuid(UUID id) throws RepositoryException;

	/*
	 * id from uuid lookup methods
	 */

	public int getElementIdForUUIDString(String uuid) throws RepositoryException
	{
		int id = 0;
		if (uuid != null)
		{
			uuid = uuid.trim();
			if (uuid.indexOf('-') > 0)
			{
				// This is really a UUID. Convert it to an element id.
				return getElementIdForUUID(UUID.fromString(uuid));
			}
			else
			{
				// This is not a UUID but a degenerate value; parse it and ensure it is less than or equal to 0.
				try
				{
					id = Integer.parseInt(uuid);
					if (id > 0)
					{
						throw new RepositoryException("invalid degenerate UUID value " + id);
					}
				}
				catch (NumberFormatException e)
				{
					throw new RepositoryException("invalid UUID format " + uuid);
				}
			}
		}
		return id;
	}

	/**
	 * Create a repositoy object like Form,fields,portals,beans,etc.
	 *
	 * @param parent the parent
	 * @param objectTypeId the type
	 * @param elementId the element_id for creation
	 * @return the created object
	 */
	public IPersist createObject(ISupportChilds parent, int objectTypeId, int elementId, UUID uuid) throws RepositoryException
	{
		IPersist object = null;
		switch (objectTypeId)
		{
			case IRepository.FORMS :
				object = new Form(parent, elementId, uuid);
				break;

			case IRepository.LAYOUTCONTAINERS :
				object = new LayoutContainer(parent, elementId, uuid);
				break;

			case IRepository.FORMREFERENCE :
				object = new FormReference(parent, elementId, uuid);
				break;

			case IRepository.GRAPHICALCOMPONENTS :
				object = new GraphicalComponent(parent, elementId, uuid);
				break;

			case IRepository.FIELDS :
				object = new Field(parent, elementId, uuid);
				break;

			case IRepository.PORTALS :
				object = new Portal(parent, elementId, uuid);
				break;

			case IRepository.TABPANELS :
				object = new TabPanel(parent, elementId, uuid);
				break;

			case IRepository.TABS :
				object = new Tab(parent, elementId, uuid);
				break;

			case IRepository.SHAPES :
				object = new Shape(parent, elementId, uuid);
				break;

			case IRepository.BEANS :
				object = new Bean(parent, elementId, uuid);
				break;

			case IRepository.RELATIONS :
				object = new Relation(parent, elementId, uuid);
				break;

			case IRepository.METHODS :
				object = new ScriptMethod(parent, elementId, uuid);
				break;

			case IRepository.SCRIPTCALCULATIONS :
				object = new ScriptCalculation(parent, elementId, uuid);
				break;

			case IRepository.AGGREGATEVARIABLES :
				object = new AggregateVariable(parent, elementId, uuid);
				break;

			case IRepository.VALUELISTS :
				object = new ValueList(parent, elementId, uuid);
				break;

			case IRepository.SCRIPTVARIABLES :
				object = new ScriptVariable(parent, elementId, uuid);
				break;

			case IRepository.RELATION_ITEMS :
				object = new RelationItem(parent, elementId, uuid);
				break;

			case IRepository.RECTSHAPES :
				object = new RectShape(parent, elementId, uuid);
				break;

			case IRepository.PARTS :
				object = new Part(parent, elementId, uuid);
				break;

			case IRepository.TABLENODES :
				object = new TableNode(parent, elementId, uuid);
				break;

			case IRepository.MEDIA :
				object = new Media(parent, elementId, uuid);
				break;

			case IRepository.WEBCOMPONENTS :
				object = new WebComponent(parent, elementId, uuid);
				break;

			case IRepository.SOLUTIONS :
			case IRepository.STYLES :
			case IRepository.TEMPLATES :
				object = createRootObject(elementId);
				break;

			default :
				throw new RepositoryException("cannot create object with type id=" + objectTypeId + ", type does not exist"); //$NON-NLS-1$ //$NON-NLS-2$
		}

		// Save the uuid to element id mapping.
		if (!uuid_element_id_map.containsKey(uuid))
		{
			uuid_element_id_map.put(uuid, new Integer(elementId));
		}

		return object;
	}

	protected abstract IPersist createRootObject(int elementId) throws RepositoryException;

	/**
	 * Converter method to convert String in object
	 *
	 * @param type_id the type
	 * @param s the string
	 * @return the object
	 */
	public Object convertArgumentStringToObject(int typeId, String s) throws RepositoryException
	{
		Object retval = null;
		switch (typeId)
		{
			case IRepository.DIMENSION :
				retval = PersistHelper.createDimension(s);
				break;

			case IRepository.ELEMENTS :
				retval = Integer.valueOf(getElementIdForUUIDString(s));
				break;

			case IRepository.BLOBS :
			case IRepository.INTEGER :
				retval = Integer.valueOf(Utils.getAsInteger(s));
				break;

			case IRepository.BORDER :
				retval = s;
				break;

			case IRepository.COLOR :
				retval = PersistHelper.createColor(s);
				break;

			case IRepository.POINT :
				retval = PersistHelper.createPoint(s);
				break;

			case IRepository.INSETS :
				retval = PersistHelper.createInsets(s);
				break;

			case IRepository.STRING :
			case IRepository.STYLES :
			case IRepository.TEMPLATES :
			case IRepository.SERVERS :
			case IRepository.TABLES :
			case IRepository.DATASOURCES :
				retval = s;
				break;

			case IRepository.FONT :
				retval = s;//PersistHelper.createFont(s); fonts must be created in client
				break;

			case IRepository.BOOLEAN :
				retval = Boolean.valueOf(Utils.getAsBoolean(s));
				break;
			case IRepository.JSON :
				try
				{
					retval = new ServoyJSONObject(s, false);
				}
				catch (JSONException ex)
				{
					Debug.error(ex);
				}
				break;
			default :
				throw new RepositoryException("type with id=" + typeId + " does not exist");
		}

		retval = Internalize.intern(retval);

		return retval;
	}


	public Map<String, Method> getSettersViaIntrospection(Object obj) throws IntrospectionException
	{
		return RepositoryHelper.getSettersViaIntrospection(obj);
	}

	public ContentSpec getContentSpec() throws RepositoryException
	{
		if (contentSpec == null)
		{
			synchronized (this)
			{
				contentSpec = loadContentSpec();
			}
		}
		return contentSpec;
	}


	public synchronized void flushContentSpec()
	{
		contentSpec = null;
	}

	protected abstract ContentSpec loadContentSpec() throws RepositoryException;

	public String convertObjectToArgumentString(int typeId, Object obj) throws RepositoryException
	{
		return convertObjectToArgumentString(typeId, obj, -1, -1, -1, null);
	}

	/**
	 * Converter method to convert object in string
	 *
	 * @param type_id the type
	 * @param obj the object
	 * @return the string
	 */
	public String convertObjectToArgumentString(int typeId, Object obj, int elementId, int revision, int contentId, IUnresolvedUUIDResolver resolver)
		throws RepositoryException
	{
		if (obj == null) return null;
		String retval = null;
		switch (typeId)
		{
			case IRepository.BORDER :
				retval = (String)obj;
				break;

			case IRepository.INSETS :
				retval = PersistHelper.createInsetsString((Insets)obj);
				break;

			case IRepository.DIMENSION :
				retval = PersistHelper.createDimensionString((Dimension)obj);
				break;

			case IRepository.COLOR :
				retval = PersistHelper.createColorString((Color)obj);
				break;

			case IRepository.POINT :
				retval = PersistHelper.createPointString((Point)obj);
				break;

			case IRepository.FONT :
				retval = (String)obj;
				break;

			case IRepository.ELEMENTS :
				retval = getUUIDStringForElementId(((Integer)obj).intValue(), elementId, revision, contentId, resolver);
				break;

			case IRepository.STRING :
			case IRepository.INTEGER :
			case IRepository.BOOLEAN :
			case IRepository.STYLES :
			case IRepository.TEMPLATES :
			case IRepository.SERVERS :
			case IRepository.TABLES :
			case IRepository.DATASOURCES :
			case IRepository.BLOBS :
			case IRepository.JSON :
				retval = obj.toString();
				break;

			default :
				throw new RepositoryException("type with id=" + typeId + " does not exist");
		}
		return retval;
	}


	public String getUUIDStringForElementId(int id, int elementId, int revision, int contentId, IUnresolvedUUIDResolver resolver) throws RepositoryException
	{
		UUID uuid = getUUIDForElementId(id, elementId, revision, contentId, resolver);
		if (uuid != null)
		{
			return uuid.toString();
		}
		return String.valueOf(id);
	}

	/*
	 * uuid from id lookup methods
	 */

	public UUID getUUIDForElementId(int id, int elementId, int revision, int contentId, IUnresolvedUUIDResolver resolver) throws RepositoryException
	{
		if (id == IRepository.UNRESOLVED_ELEMENT)
		{
			if (resolver != null)
			{
				UUID uid = resolver.resolve(elementId, revision, contentId);
				if (uid != null)
				{
					return uid;
				}
			}
			return IRepository.UNRESOLVED_UUID;
		}
		if (id > 0)
		{
			UUID uuid = resolveUUIDForElementId(id);
			if (uuid == null)
			{
				uuid = uuid_element_id_map.getKey(new Integer(id));
			}
			return uuid;
		}
		// This is a degenerate reference.
		return null;
	}

	public abstract UUID resolveUUIDForElementId(int id) throws RepositoryException;

	public static Map<Object, Object> resetUUIDSRecursively(IPersist persist, final IPersistFactory persistFactory, final boolean flagChanged)
	{
		final Map<Object, Object> updatedElementIds = new HashMap<>();
		persist.acceptVisitor(new IPersistVisitor()
		{
			public Object visit(IPersist o)
			{
				if (o instanceof AbstractBase)
				{
					UUID current = o.getUUID();
					((AbstractBase)o).resetUUID();
					try
					{
						int newElementID = persistFactory.getNewElementID(o.getUUID());
						updatedElementIds.put(current.toString(), o.getUUID().toString());
						updatedElementIds.put(Integer.valueOf(o.getID()), Integer.valueOf(newElementID));
						((AbstractBase)o).setID(newElementID);
						if (flagChanged) o.flagChanged();
					}
					catch (RepositoryException e)
					{
						Debug.log(e);
					}
				}
				return IPersistVisitor.CONTINUE_TRAVERSAL;
			}
		});
		return updatedElementIds;
	}

	public void clearUUIDMap()
	{
		uuid_element_id_map.clear();
	}

}
