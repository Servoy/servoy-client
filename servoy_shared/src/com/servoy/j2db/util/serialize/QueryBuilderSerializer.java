/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.j2db.util.serialize;

import java.io.StringWriter;
import java.io.Writer;

import org.jabsorb.serializer.AbstractSerializer;
import org.jabsorb.serializer.MarshallException;
import org.jabsorb.serializer.ObjectMatch;
import org.jabsorb.serializer.SerializerState;
import org.jabsorb.serializer.UnmarshallException;
import org.json.JSONException;
import org.json.JSONObject;

import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.query.AbstractBaseQuery;
import com.servoy.j2db.query.QuerySelect;
import com.servoy.j2db.querybuilder.impl.QBFactory;
import com.servoy.j2db.querybuilder.impl.QBSelect;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.CompactWriter;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * Responsible for serializing QBSelect objects
 * 
 * @author rgansevles
 * 
 * @since 6.1
 */
public class QueryBuilderSerializer extends AbstractSerializer
{
	/**
	 * Unique serialisation id.
	 */
	private final static long serialVersionUID = 2;

	/**
	 * The classes that this can serialise
	 */
	private final static Class[] _serializableClasses = new Class[] { QBSelect.class };

	/**
	 * The class that this serialises to
	 */
	private final static Class[] _JSONClasses = new Class[] { JSONObject.class };

	private final IQueryBuilderFactoryProvider queryBuilderFactoryProvider;

	private XStream xStream;

	public QueryBuilderSerializer(IQueryBuilderFactoryProvider queryBuilderFactoryProvider)
	{
		this.queryBuilderFactoryProvider = queryBuilderFactoryProvider;
	}

	public Class[] getSerializableClasses()
	{
		return _serializableClasses;
	}

	public Class[] getJSONClasses()
	{
		return _JSONClasses;
	}

	public Object marshall(SerializerState state, Object p, Object o) throws MarshallException
	{
		if (!(o instanceof QBSelect))
		{
			throw new MarshallException("QueryBuilderSerializer cannot marshall class " + o.getClass());
		}

		QBSelect qbSelect = (QBSelect)o;

		QuerySelect query;
		try
		{
			query = qbSelect.getQuery(false);
		}
		catch (RepositoryException e)
		{
			throw new MarshallException(e.getMessage(), e);
		}

		String xml;
		// make sure that queries are serialized in full, standard serialization optimizations result in missing data that cannot be resolved
		AbstractBaseQuery.pushQueryFullSerialization(true);
		try
		{
			Writer writer = new StringWriter();
			getXstream().marshal(query, new CompactWriter(writer));
			xml = writer.toString();
		}
		finally
		{
			AbstractBaseQuery.popQueryFullSerialization();
		}

		JSONObject obj = new JSONObject();
		try
		{
			if (ser.getMarshallClassHints())
			{
				obj.put("javaClass", o.getClass().getName());
			}
			obj.put("query", xml); // required
			obj.put("datasource", qbSelect.getDataSource()); // required
			obj.put("alias", qbSelect.getTableAlias()); // optional
		}
		catch (JSONException e)
		{
			throw new MarshallException(e.getMessage(), e);
		}
		return obj;
	}

	public ObjectMatch tryUnmarshall(SerializerState state, Class clazz, Object o) throws UnmarshallException
	{
		if (queryBuilderFactoryProvider.getQueryBuilderFactory() == null)
		{
			throw new UnmarshallException("QueryBuilderSerializer needs query builder factory");
		}
		JSONObject jso = (JSONObject)o;
		String java_class;
		try
		{
			java_class = jso.getString("javaClass");
		}
		catch (JSONException e)
		{
			throw new UnmarshallException("no type hint", e);
		}
		if (java_class == null)
		{
			throw new UnmarshallException("no type hint");
		}
		if (!(java_class.equals(QBSelect.class.getName())))
		{
			throw new UnmarshallException("not a QBSelect");
		}
		state.setSerialized(o, ObjectMatch.OKAY);
		return ObjectMatch.OKAY;
	}

	public Object unmarshall(SerializerState state, Class clazz, Object o) throws UnmarshallException
	{
		QBFactory queryBuilderFactory = queryBuilderFactoryProvider.getQueryBuilderFactory();
		if (queryBuilderFactory == null)
		{
			throw new UnmarshallException("QueryBuilderSerializer needs query builder factory");
		}
		JSONObject jso = (JSONObject)o;
		String xml;
		String dataSource;
		String alias;
		try
		{
			xml = jso.getString("query"); // required
			dataSource = jso.getString("datasource"); // required
			alias = jso.optString("alias", null); // optional
		}
		catch (JSONException e)
		{
			throw new UnmarshallException("Could not get the query in QueryBuilderSerializer", e);
		}
		if (jso.has("javaClass"))
		{
			try
			{
				clazz = Class.forName(jso.getString("javaClass"));
			}
			catch (ClassNotFoundException e)
			{
				throw new UnmarshallException(e.getMessage(), e);
			}
			catch (JSONException e)
			{
				throw new UnmarshallException("Could not find javaClass", e);
			}
		}
		Object returnValue = null;
		if (QBSelect.class.equals(clazz))
		{
			QuerySelect query = (QuerySelect)getXstream().fromXML(xml);
			returnValue = queryBuilderFactory.createSelect(dataSource, alias, query);
		}
		if (returnValue == null)
		{
			throw new UnmarshallException("invalid class " + clazz);
		}
		state.setSerialized(o, returnValue);
		return returnValue;
	}

	/**
	 * @return
	 */
	private XStream getXstream()
	{
		if (xStream == null)
		{
			xStream = new XStream(new DomDriver());
			xStream.alias("replacer", ReplacedObject.class);
			for (Class< ? extends IWriteReplace> cls : ReplacedObject.getDomainClasses(AbstractBaseQuery.QUERY_SERIALIZE_DOMAIN))
			{
				xStream.alias(cls.getSimpleName(), cls);
			}
		}
		return xStream;
	}
}
