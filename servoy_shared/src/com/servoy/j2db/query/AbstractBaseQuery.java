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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.servoy.j2db.util.serialize.IWriteReplace;
import com.servoy.j2db.util.serialize.ReplacedObject;
import com.servoy.j2db.util.visitor.DeepCloneVisitor;
import com.servoy.j2db.util.visitor.IVisitable;
import com.servoy.j2db.util.visitor.IVisitor;
import com.servoy.j2db.util.visitor.IVisitor.VistorResult;
import com.servoy.j2db.util.visitor.ReplaceVisitor;

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
		classMapping.put(QueryTable.class, Short.valueOf((short)23));
		classMapping.put(QueryUpdate.class, Short.valueOf((short)24));
		classMapping.put(SetCondition.class, Short.valueOf((short)25));
		classMapping.put(ExistsCondition.class, Short.valueOf((short)26));
		classMapping.put(ObjectPlaceholderKey.class, Short.valueOf((short)27));
		classMapping.put(QueryCompositeJoin.class, Short.valueOf((short)28));

		ReplacedObject.installClassMapping(QUERY_SERIALIZE_DOMAIN, classMapping);
	}

	public static void initialize()
	{
		// this method triggers the static code above.
	}


	public Object shallowClone() throws CloneNotSupportedException
	{
		return super.clone();
	}

	static int hashCode(Object[] array)
	{
		final int PRIME = 31;
		if (array == null) return 0;
		int result = 1;
		for (Object element : array)
		{
			result = PRIME * result + (element == null ? 0 : element.hashCode());
		}
		return result;
	}

	static int hashCode(int[] array)
	{
		final int PRIME = 31;
		if (array == null) return 0;
		int result = 1;
		for (int element : array)
		{
			result = PRIME * result + element;
		}
		return result;
	}


	public static boolean arrayEquals(Object value1, Object value2)
	{
		if (value1 == null)
		{
			return value2 == null;
		}
		if (value1 instanceof Object[][] && value2 instanceof Object[][] && ((Object[][])value1).length == ((Object[][])value2).length)
		{
			Object[][] array = (Object[][])value1;
			for (int i = 0; i < array.length; i++)
			{
				if (!Arrays.equals(array[i], ((Object[][])value2)[i]))
				{
					return false;
				}
			}
			return true;
		}
		if (value1 instanceof Object[] && value2 instanceof Object[])
		{
			return Arrays.equals((Object[])value1, (Object[])value2);
		}
		return value1.equals(value2);
	}

	private static int arrays_hashCode(Object a[])
	{
		if (a == null) return 0;

		int result = 1;

		for (Object element : a)
			result = 31 * result + (element == null ? 0 : element.hashCode());

		return result;
	}

	public static int arrayHashcode(Object value)
	{
		if (value == null)
		{
			return 0;
		}
		if (value instanceof Object[][])
		{
			int hash = 0;
			Object[][] array = (Object[][])value;
			for (Object[] element : array)
			{
				hash = 31 * hash + arrays_hashCode(element);
			}
		}
		else if (value instanceof Object[])
		{
			return arrays_hashCode((Object[])value);
		}
		return value.hashCode();
	}

	public static String toString(Object o)
	{
		if (o == null)
		{
			return "<null>"; //$NON-NLS-1$
		}

		if (o instanceof Object[])
		{
			Object[] array = (Object[])o;
			StringBuffer sb = new StringBuffer().append('[');
			for (int i = 0; i < array.length; i++)
			{
				if (i > 0)
				{
					sb.append(',');
				}
				sb.append(toString(array[i]));
			}
			return sb.append(']').toString();
		}

		if (o instanceof int[])
		{
			int[] array = (int[])o;
			StringBuffer sb = new StringBuffer().append('[');
			for (int i = 0; i < array.length; i++)
			{
				if (i > 0)
				{
					sb.append(',');
				}
				sb.append(array[i]);
			}
			return sb.append(']').toString();
		}

		if (o instanceof byte[])
		{
			return "0x" + new String(encodeHex((byte[])o)); //$NON-NLS-1$
		}

		return o.toString();
	}

	/**
	 * Used building output as Hex
	 */
	private static final char[] DIGITS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

	/**
	 * Converts an array of bytes into an array of characters representing the hexidecimal values of each byte in order. The returned array will be double the
	 * length of the passed array, as it takes two characters to represent any given byte.
	 * 
	 * @param data a byte[] to convert to Hex characters
	 * @return A char[] containing hexidecimal characters
	 */
	public static char[] /* Hex. */encodeHex(byte[] data)
	{
		int l = data.length;

		char[] out = new char[l << 1];

		// two characters form the hex value.
		for (int i = 0, j = 0; i < l; i++)
		{
			out[j++] = DIGITS[(0xF0 & data[i]) >>> 4];
			out[j++] = DIGITS[0x0F & data[i]];
		}

		return out;
	}


	public static <T> T acceptVisitor(T o, IVisitor visitor)
	{
		if (o == null)
		{
			return null;
		}

		Object v = visitor.visit(o);
		T o2;
		if (v instanceof VistorResult)
		{
			o2 = (T)((VistorResult)v).object;
			if (!((VistorResult)v).continueTraversal)
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
		return acceptVisitor(o, new DeepCloneVisitor());
	}

	/**
	 * Relink from orgTable to newTable. Correct references to table aliases.
	 * 
	 * @param orgTable
	 * @param newTable
	 * @param o
	 * @return
	 */
	public static <T> T relinkTable(QueryTable orgTable, QueryTable newTable, T o)
	{
		return AbstractBaseQuery.acceptVisitor(o, new ReplaceVisitor(orgTable, newTable, true));
	}

	/**
	 * Find all occurrences of tables with the given name, regardless of the alias.
	 * 
	 * @param name
	 * @return set of QueryTable
	 */
	public List<QueryTable> findTable(String name)
	{
		TableFinder visitor = new TableFinder(name);
		acceptVisitor(visitor);
		return visitor.getTables();
	}

	/**
	 * Replace references to orgTable with newTable.
	 * 
	 * @param orgTable
	 * @param newTable
	 */
	public boolean relinkTable(QueryTable orgTable, QueryTable newTable)
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
		PlaceHolderFinder phf = new PlaceHolderFinder(key);
		visitable.acceptVisitor(phf);
		List<Object> placeHolders = phf.getPlaceHolders();
		if (placeHolders.size() == 0)
		{
			return null;
		}
		return (Placeholder)placeHolders.get(0);
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

	/**
	 * Visitor class for finding tables by name regardless of alias.
	 * 
	 * @author rgansevles
	 * 
	 */
	public static class TableFinder implements IVisitor
	{
		private final String name;
		private final List<QueryTable> tables = new ArrayList<QueryTable>();

		/**
		 * @param name
		 */
		public TableFinder(String name)
		{
			this.name = name;
		}

		public List<QueryTable> getTables()
		{
			return tables;
		}

		public Object visit(Object o)
		{
			if (o instanceof QueryTable)
			{
				QueryTable t = (QueryTable)o;
				if (t.getName().equals(name))
				{
					tables.add(t);
				}
			}
			return o;
		}
	}

	/**
	 * Visitor class for finding place holders by key.
	 * 
	 * @author rgansevles
	 * 
	 */
	public static class PlaceHolderFinder implements IVisitor
	{
		private final TablePlaceholderKey key;
		private final List<Object> placeHolders = new ArrayList<Object>();

		PlaceHolderFinder(TablePlaceholderKey key)
		{
			this.key = key;
		}

		List<Object> getPlaceHolders()
		{
			return placeHolders;
		}

		public Object visit(Object o)
		{
			if (o instanceof Placeholder && (key.equals(((Placeholder)o).getKey())))
			{
				placeHolders.add(o);
			}
			return o;
		}
	}


	public static List<String> getInvalidRangeConditions(ISQLCondition condition)
	{
		List<String> invalidRangeConditions = new ArrayList<String>();

		if (condition != null)
		{
			CompareConditionVisitor betweenConditionVisitor = new AbstractBaseQuery.CompareConditionVisitor(ISQLCondition.BETWEEN_OPERATOR);
			AbstractBaseQuery.acceptVisitor(condition, betweenConditionVisitor);
			List<CompareCondition> rangeConditions = betweenConditionVisitor.getResult();

			IQuerySelectValue[] ccKey;
			Object ccOperand2;
			for (CompareCondition cc : rangeConditions)
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


	static class CompareConditionVisitor implements IVisitor
	{
		private final int compareCondition;
		private final List<CompareCondition> returnCompareConditions = new ArrayList<CompareCondition>();

		CompareConditionVisitor(int compareCondition)
		{
			this.compareCondition = compareCondition;
		}

		public Object visit(Object o)
		{
			if (o instanceof CompareCondition && ((CompareCondition)o).getOperator() == compareCondition)
			{
				returnCompareConditions.add((CompareCondition)o);
			}

			return o;
		}

		List<CompareCondition> getResult()
		{
			return returnCompareConditions;
		}
	}
}
