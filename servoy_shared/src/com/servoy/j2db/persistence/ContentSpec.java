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


import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import com.servoy.base.persistence.constants.IContentSpecConstantsBase;

/**
 * The <code>ContentSpec</code> class is a inmem representation of the servoy_content_spec table which can be accessed in core.
 */
public class ContentSpec
{
	public static final Integer MINUS_ONE = IContentSpecConstantsBase.MINUS_ONE;
	public static final Integer ZERO = IContentSpecConstantsBase.ZERO;
	public static final Object JAVA_DEFAULT_VALUE = IContentSpecConstantsBase.JAVA_DEFAULT_VALUE;

	public ContentSpec()
	{
		contentSpecElements = new TreeMap<Integer, Element>(new Comparator<Integer>()
		{
			// sort by content id descending
			public int compare(Integer o1, Integer o2)
			{
				return o2.compareTo(o1);
			}
		});
	}

	/**
	 * The <code>Element</code> class represents one row in the servoy_content_spec table, which represents a property of an object.
	 */
	public class Element
	{
		/**
		 * The content id, the primary key into the content spec table.
		 */
		private final int contentID;

		/**
		 * The object type id, which indicates what object type this element is a property of.
		 */
		private final int objectTypeID;

		/**
		 * The name of this property.
		 */
		private final String name;

		/**
		 * The type id of this property, which indicates what type the value of this property has.
		 */
		private final int typeID;

		/**
		 * Indicates whether or not this property is meta data. Meta data properties are stored in revision 0 of an object.
		 */
		private boolean isMetaData;

		/**
		 * Indicates whether or not this property is required.
		 */
		private boolean isRequired;

		/**
		 * Indicate if content item is deprecated; 0 - not deprecated; -1 deprecated with no replacement specified; >0 content id replacement
		 */
		private int deprecatedMoveContentID = 0;

		/**
		 * The default_textual_classvalue of this property.
		 */
		private Object defaultclassvalue;

		protected Element(int contentID, int objectTypeID, String name, int typeID)
		{
			this(contentID, objectTypeID, name, typeID, ContentSpec.JAVA_DEFAULT_VALUE);
		}

		/**
		 * Constructs a new element with the specified values.
		 *
		 * @param contentID the content id
		 * @param objectTypeID the object type id
		 * @param name the property name
		 * @param typeID the type id
		 * @param defaultValue the value to fill by default
		 */
		public Element(int contentID, int objectTypeID, String name, int typeID, Object defaultValue)
		{
			contentSpecElements.put(new Integer(contentID), this);

			this.contentID = contentID;
			this.objectTypeID = objectTypeID;
			this.name = name;
			this.typeID = typeID;

			if (defaultValue == JAVA_DEFAULT_VALUE)
			{
				this.defaultclassvalue = getJavaClassMemberDefaultValue(typeID);
			}
			else if (defaultValue != null)
			{
				this.defaultclassvalue = defaultValue;
			}
		}


		public int getContentID()
		{
			return contentID;
		}

		public int getObjectTypeID()
		{
			return objectTypeID;
		}

		public String getName()
		{
			return name;
		}

		public int getTypeID()
		{
			return typeID;
		}

		public boolean isMetaData()
		{
			return isMetaData;
		}

		public void flagAsMetaData()
		{
			isMetaData = true;
		}

		public boolean isRequired()
		{
			return isRequired;
		}

		public void flagAsRequired()
		{
			isRequired = true;
		}

		public String getDefaultTextualClassValue()
		{
			return defaultclassvalue == null ? null : defaultclassvalue.toString();
		}

		public Object getDefaultClassValue()
		{
			return defaultclassvalue;
		}


		public boolean isDeprecated()
		{
			return deprecatedMoveContentID != 0;
		}


		public void flagAsDeprecated()
		{
			flagAsDeprecated(-1);
		}

		public void flagAsDeprecated(int moveContentID)
		{
			this.deprecatedMoveContentID = moveContentID;
		}

		public int getDeprecatedMoveContentID()
		{
			return deprecatedMoveContentID;
		}
	}

	public static Object getJavaClassMemberDefaultValue(int type_id)
	{
		Object retval = null;
		switch (type_id)
		{
			case IRepository.DIMENSION :
				retval = null;
				break;

			case IRepository.MEDIA :
			case IRepository.ELEMENTS :
			case IRepository.INTEGER :
			case IRepository.BLOBS :
				retval = ZERO;
				break;

			case IRepository.BORDER :
				retval = null;
				break;

			case IRepository.COLOR :
				retval = null;
				break;

			case IRepository.POINT :
				retval = null;
				break;

			case IRepository.INSETS :
				retval = null;
				break;

			case IRepository.STRING :
			case IRepository.STYLES :
			case IRepository.TEMPLATES :
			case IRepository.SERVERS :
			case IRepository.TABLES :
			case IRepository.DATASOURCES :
				retval = null;
				break;

			case IRepository.FONT :
				retval = null;
				break;

			case IRepository.BOOLEAN :
				retval = Boolean.FALSE;
				break;

			default :
				throw new IllegalArgumentException("Type with id: " + type_id + " does not exist"); //$NON-NLS-2$
		}
		return retval;
	}

	/**
	 * The content spec elements mapped by their content id.
	 */
	private TreeMap<Integer, Element> contentSpecElements = null;

	/**
	 * A map containing all object types by object type id to maps of mapping the property name of each of their properties to their respective content spec
	 * element.
	 */
	private Map<Integer, LinkedHashMap<String, Element>> objectTypes = null;

	/**
	 * Get a content spec element by content_id.
	 *
	 * @param id the content id
	 *
	 * @return the content spec element with the specified content id
	 *
	 * @throws RepositoryException if the content spec is not loaded or no element with the specified id exists
	 */
	public Element getElementByContentID(int content_id) throws RepositoryException
	{
		if (contentSpecElements == null)
		{
			throw new RepositoryException("Content spec uninitialized");
		}
		Element element = contentSpecElements.get(new Integer(content_id));
		if (element == null)
		{
			throw new RepositoryException("Content spec element with id " + content_id + " does not exist"); //$NON-NLS-2$
		}
		return element;
	}

	/**
	 * Gets a property for the specified object type by name and returns the meta data as a content spec element.
	 *
	 * @param objectTypeID the object type id
	 * @param name the name of the property
	 *
	 * @return the element of the specified object type with the specified name or <code>null</code> if no such property of the object type exists.
	 *
	 */
	public Element getPropertyForObjectTypeByName(int objectTypeID, String name)
	{
		if (objectTypes == null)
		{
			fillObjectTypesMap();
		}
		Map<String, Element> elements = objectTypes.get(new Integer(objectTypeID));
		return elements != null ? elements.get(name) : null;
	}

	/**
	 * Gets all properties for the specified object type.
	 *
	 * @param objectTypeID the object type id
	 *
	 * @return an iterator which iterates over all elements belonging to the specified object type, or an iterator over an empty list if no such object type
	 *         exists.
	 *
	 */
	public Iterator<Element> getPropertiesForObjectType(int objectTypeID)
	{
		if (objectTypes == null)
		{
			fillObjectTypesMap();
		}
		Map<String, Element> elements = objectTypes.get(new Integer(objectTypeID));
		return elements != null ? elements.values().iterator() : Collections.<Element> emptyList().iterator();
	}

	/**
	 * Gets all the properties of all object types. sorted by content id (descending) to make sure deprecated properties are return after their replacement
	 */
	public Iterator<Element> getElements()
	{
		return contentSpecElements.values().iterator();
	}

	/**
	 * @param typeID
	 * @param content_id
	 * @param property_value
	 */
	public boolean mustUpdateValue(int content_id, String property_value) throws RepositoryException
	{
		Element element = getElementByContentID(content_id);
		String default_value = element.getDefaultTextualClassValue();

		if (default_value == property_value) return false;
		if (default_value != null && (property_value == null || property_value.trim().length() == 0))
		{
			// FIXME: The default value for this property is not null, but
			// FIXME: the property value is. Since we cannot save null values
			// FIXME: in the database, nor values consisting of only white space,
			// FIXME: don't save the value. This will cause a subsequent load
			// FIXME: to restore the value to the default value.
			// FIXME: Move this purely to save & load methods of properties in
			// FIXME: the database, to avoid it on the Java side.
			return false;
		}
		if (default_value == null) return true;
		return (!default_value.equals(property_value));
	}

	private void fillObjectTypesMap()
	{
		objectTypes = new HashMap<Integer, LinkedHashMap<String, Element>>();

		// Create the object types hash map, which for each object type
		// which has properties maps contains a map of property names to
		// content spec elements.
		Iterator<Element> iterator = getElements(); // sorted by content id desc
		while (iterator.hasNext())
		{
			Element element = iterator.next();
			Integer objectTypeID = new Integer(element.getObjectTypeID());
			LinkedHashMap<String, Element> elements = objectTypes.get(objectTypeID);
			if (elements == null)
			{
				// use LinkedHashMap to preserve sorting
				elements = new LinkedHashMap<String, Element>();
				objectTypes.put(objectTypeID, elements);
			}
			elements.put(element.getName(), element);
		}
	}

	public void clear()
	{
		objectTypes.clear();
		contentSpecElements.clear();
	}
}
