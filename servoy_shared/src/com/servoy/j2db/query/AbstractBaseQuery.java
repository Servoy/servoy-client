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
package com.servoy.j2db.query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import com.servoy.base.query.BaseQueryTable;
import com.servoy.base.query.IBaseSQLCondition;
import com.servoy.j2db.util.TypePredicate;
import com.servoy.j2db.util.serialize.IWriteReplace;
import com.servoy.j2db.util.serialize.ReplacedObject;
import com.servoy.j2db.util.visitor.DeepCloneVisitor;
import com.servoy.j2db.util.visitor.IVisitable;
import com.servoy.j2db.util.visitor.IVisitor;
import com.servoy.j2db.util.visitor.IVisitor.VisitorResult;
import com.servoy.j2db.util.visitor.ReplaceVisitor;
import com.servoy.j2db.util.visitor.SearchVisitor;

/**
 * Base class for all DML classes.
 *
 * @author rgansevles
 *
 */
public abstract class AbstractBaseQuery implements ISQLQuery
{
	private static final long serialVersionUID = 1L;
	// This one is not final by choice! it has to touch this class first so that the classmapping hashmap is initialized
	public static String QUERY_SERIALIZE_DOMAIN = "Q"; //$NON-NLS-1$

	protected String comment;

	static
	{
		Map<Class< ? extends IWriteReplace>, Short> classMapping = new HashMap<Class< ? extends IWriteReplace>, Short>();

		classMapping.put(AndCondition.class, Short.valueOf((short)1));
		classMapping.put(BooleanCondition.class, Short.valueOf((short)2));
		classMapping.put(ColumnType.class, Short.valueOf((short)3));
		classMapping.put(CompareCondition.class, Short.valueOf((short)4));
		classMapping.put(CustomCondition.class, Short.valueOf((short)5));
		classMapping.put(OrCondition.class, Short.valueOf((short)6));
		classMapping.put(Placeholder.class, Short.valueOf((short)7));
		classMapping.put(TablePlaceholderKey.class, Short.valueOf((short)8));
		classMapping.put(QueryAggregate.class, Short.valueOf((short)9));
		classMapping.put(QueryColumn.class, Short.valueOf((short)10));
		classMapping.put(QueryColumnValue.class, Short.valueOf((short)11));
		classMapping.put(QueryCustomElement.class, Short.valueOf((short)12));
		classMapping.put(QueryCustomJoin.class, Short.valueOf((short)13));
		classMapping.put(QueryCustomSelect.class, Short.valueOf((short)14));
		classMapping.put(QueryCustomSort.class, Short.valueOf((short)15));
		classMapping.put(QueryCustomUpdate.class, Short.valueOf((short)16));
		classMapping.put(QueryDelete.class, Short.valueOf((short)17));
		classMapping.put(QueryFunction.class, Short.valueOf((short)18));
		classMapping.put(QueryInsert.class, Short.valueOf((short)19));
		classMapping.put(QueryJoin.class, Short.valueOf((short)20));
		classMapping.put(QuerySelect.class, Short.valueOf((short)21));
		classMapping.put(QuerySort.class, Short.valueOf((short)22));
		classMapping.put(QueryTable1.class, Short.valueOf((short)-1)); // Legacy QueryData without dataSource, needed to deserialize old xml
		classMapping.put(QueryTable.class, Short.valueOf((short)23));
		classMapping.put(QueryUpdate.class, Short.valueOf((short)24));
		classMapping.put(SetCondition.class, Short.valueOf((short)25));
		classMapping.put(ExistsCondition.class, Short.valueOf((short)26));
		classMapping.put(ObjectPlaceholderKey.class, Short.valueOf((short)27));
		classMapping.put(QueryCompositeJoin.class, Short.valueOf((short)28));
		classMapping.put(QueryFilter.class, Short.valueOf((short)29));
		classMapping.put(TableExpression.class, Short.valueOf((short)30));
		classMapping.put(DerivedTable.class, Short.valueOf((short)31));

		ReplacedObject.installClassMapping(QUERY_SERIALIZE_DOMAIN, classMapping);
	}

	public static void initialize()
	{
		// this method triggers the static code above.
	}

	/**
	 * @return the comment
	 */
	public String getComment()
	{
		return comment;
	}

	/**
	 * @param comment the comment to set
	 */
	public void setComment(String comment)
	{
		this.comment = comment;
	}

	@Override
	public Object shallowClone() throws CloneNotSupportedException
	{
		return super.clone();
	}

	public static <T> T acceptVisitor(T o, IVisitor visitor)
	{
		if (o == null)
		{
			return null;
		}

		Object v = visitor.visit(o);
		T o2;
		if (v instanceof VisitorResult)
		{
			o2 = (T)((VisitorResult)v).object;
			if (!((VisitorResult)v).continueTraversal)
			{
				return o2;
			}
		}
		else
		{
			o2 = (T)v;
			if (o2 != o)
			{
				// the visitor returned something else then the input, visting stops.
				return o2;
			}
		}

		if (o2 instanceof List)
		{
			for (int i = 0; i < ((List)o2).size(); i++)
			{
				((List<Object>)o2).set(i, acceptVisitor(((List)o2).get(i), visitor));
			}
		}

		else if (o2 instanceof Map)
		{
			Iterator<Map.Entry> it = ((Map)o2).entrySet().iterator();
			while (it.hasNext())
			{
				Map.Entry entry = it.next();
				entry.setValue(acceptVisitor(entry.getValue(), visitor));
			}
		}

		else if (o2 instanceof Object[])
		{
			for (int i = 0; i < ((Object[])o2).length; i++)
			{
				((Object[])o2)[i] = acceptVisitor(((Object[])o2)[i], visitor);
			}
		}

		else if (o2 instanceof IVisitable)
		{
			((IVisitable)o2).acceptVisitor(visitor);
		}

		return o2;
	}

	public static <T> T deepClone(T o)
	{
		return deepClone(o, false);
	}

	public static <T> T deepClone(T o, boolean cloneImmutables)
	{
		return acceptVisitor(o, DeepCloneVisitor.createDeepCloneVisitor(cloneImmutables));
	}

	/**
	 * Relink from orgTable to newTable. Correct references to table aliases.
	 *
	 * @param orgTable
	 * @param newTable
	 * @param o
	 * @return
	 */
	public static <T> T relinkTable(BaseQueryTable orgTable, BaseQueryTable newTable, T o)
	{
		return AbstractBaseQuery.acceptVisitor(o, new ReplaceVisitor(orgTable, newTable, true));
	}

	/**
	 * Replace references to orgTable with newTable.
	 *
	 * @param orgTable
	 * @param newTable
	 */
	public boolean relinkTable(BaseQueryTable orgTable, BaseQueryTable newTable)
	{
		ReplaceVisitor replaceVisitor = new ReplaceVisitor(orgTable, newTable, true);
		acceptVisitor(replaceVisitor);
		return replaceVisitor.found();
	}


	public Placeholder getPlaceholder(TablePlaceholderKey key)
	{
		return getPlaceholder(this, key);
	}

	public static Placeholder getPlaceholder(IVisitable visitable, TablePlaceholderKey key)
	{
		return AbstractBaseQuery.<Placeholder> searchOne(visitable, new TypePredicate<>(Placeholder.class, o -> key.equals(o.getKey()))).orElse(null);
	}

	public boolean setPlaceholderValue(TablePlaceholderKey key, Object value)
	{
		return setPlaceholderValue(this, key, value);
	}

	public static boolean setPlaceholderValue(IVisitable visitable, TablePlaceholderKey key, Object value)
	{
		PlaceHolderSetter phs = new PlaceHolderSetter(key, value);
		visitable.acceptVisitor(phs);
		return phs.hasUpdated();
	}

	public static List<String> getInvalidRangeConditions(ISQLCondition condition)
	{
		List<String> invalidRangeConditions = new ArrayList<>();

		if (condition != null)
		{
			List<CompareCondition> betweenConditions = search(condition,
				new TypePredicate<>(CompareCondition.class, o -> o.getOperator() == IBaseSQLCondition.BETWEEN_OPERATOR));
			IQuerySelectValue[] ccKey;
			Object ccOperand2;
			for (CompareCondition cc : betweenConditions)
			{
				ccKey = cc.getKeys();
				ccOperand2 = cc.getOperand2();

				if (ccKey != null && ccKey.length > 0 && ccOperand2 != null && ccOperand2 instanceof Object[]) // be safe
				{
					String columnName = ccKey[0].getColumn().getName();
					Object[] values = (Object[])ccOperand2;

					if (values.length > 1 && values[0] instanceof Comparable && values[1] instanceof Comparable &&
						((Comparable<Object>)values[0]).compareTo(values[1]) > 0)
					{
						invalidRangeConditions.add(columnName);
					}
				}
			}
		}

		return invalidRangeConditions;
	}

	public static <T> List<T> search(Object o, Predicate<Object> filter)
	{
		SearchVisitor<T> search = new SearchVisitor<>(filter);
		acceptVisitor(o, search);
		return search.getFound();
	}

	public static <T> Optional<T> searchOne(Object o, Predicate<Object> filter)
	{
		SearchVisitor<T> search = new SearchVisitor<>(filter);
		acceptVisitor(o, search);
		return search.getFound().stream().findAny();
	}

	/**
	 * Visitor class for setting place holder value.
	 *
	 * @author rgansevles
	 *
	 */
	public static class PlaceHolderSetter implements IVisitor
	{
		private final TablePlaceholderKey key;
		private final Object value;
		private boolean hasUpdated = false;

		/**
		 * @param key
		 * @param value
		 */
		public PlaceHolderSetter(TablePlaceholderKey key, Object value)
		{
			this.key = key;
			this.value = value;
		}

		public boolean hasUpdated()
		{
			return hasUpdated;
		}

		public Object visit(Object o)
		{
			if (o instanceof Placeholder && key.equals(((Placeholder)o).getKey()))
			{
				((Placeholder)o).setValue(value);
				hasUpdated = true;
			}
			return o;
		}

		public Object getKey()
		{
			return key;
		}

		public Object getValue()
		{
			return value;
		}
	}

	///////// serialization ////////////////

	/**
	 * Return serialized replacement for this query.
	 *
	 * @return
	 */
	public abstract Object writeReplace();
}
