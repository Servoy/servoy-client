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
package com.servoy.j2db.util;



import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.table.AbstractTableModel;

/**
 * @author Jan Blok
 */
public class SeparatedASCIIImportTableModel extends AbstractTableModel
{
	public static final String NONE_QUALIFIER = "<none>"; //$NON-NLS-1$
	public static final String SINGLE_QUOTE_QUALIFIER = "'"; //$NON-NLS-1$
	public static final String DUBBLE_QUOTE_QUALIFIER = "\""; //$NON-NLS-1$
	
	private String separator = ""; //$NON-NLS-1$
	private String qualifier = ""; //$NON-NLS-1$
	private boolean headerRow = false;
	private List items;

	private String[] lineCache0;

	private String[] lineCacheN;
	private int cacheForLine = -1;
	private static boolean lastLineNotComplete;

	public SeparatedASCIIImportTableModel()
	{
		super();
	}
	
	public String setList(List items, boolean headerRowIncluded)
	{
		fireTableRowsDeleted(0, getRowCount()-1);
		this.items = items;
		lineCacheN = null;
		cacheForLine = -1;
		headerRow = headerRowIncluded;
		lastLineNotComplete = false;
		String str = parseLines();
		fireTableRowsInserted(0, getRowCount()-1);
		return str;
	}
	
	public String parseLines()
	{
		for (int i = 0; i < getRowCount(); i++)
		{
			getValueAt(i, 0);
			if(lineCacheN == null)
			{
				if(lastLineNotComplete)
				{
					return (String) items.remove(items.size()-1);
				}
				return null;
			}
			if(headerRow)
				items.set(i+1, lineCacheN);
			else
				items.set(i, lineCacheN);
		}
		return null;
	}

	public String getColumnName(int col)
	{
		if (separator.equals("")) //$NON-NLS-1$
		{
			String line = (String) items.get(0);
			lineCache0 = new String[]{line};
			return line; 
		} 
		
		if (lineCache0 == null)
		{
			lineCache0 = parseLine(0,items,separator,qualifier);
			for (int i = 0; i < lineCache0.length; i++)
			{
				if (lineCache0[i] == null || lineCache0[i].trim().length() == 0)
				{
					lineCache0[i] = "("+(i+1)+")"; //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		}

		if (col < lineCache0.length)
		{
			String str = lineCache0[col];
			return str;	
		}

		return ""; //$NON-NLS-1$
	}

	public Object getValueAt(int row, int col)
	{
		if (headerRow) row++;
		if (row >= items.size()) return null;
		Object o = items.get(row);
		if(o instanceof String)
		{
			String line = (String) o;
			if (separator.equals("")) return line; //$NON-NLS-1$
		}
	
		if (cacheForLine != row)
		{
			cacheForLine = row;
			lineCacheN = parseLine(row,items,separator,qualifier);
			if(lineCacheN == null) return null;
		}
		if (col < lineCacheN.length)
		{
			return lineCacheN[col];	
		}
		else
		{
			return null;
		}
	}

	public int getRowCount()
	{
		if(items == null) return 0;
		if (headerRow)
		{
			return items.size() - 1;
		}
		else
		{
			return items.size();
		}
	}

	public int getColumnCount()
	{
		if (lineCache0 == null)
		{
			getColumnName(0);
		}
		return lineCache0.length;
	}

	public void setSeparator(String separator)
	{
		this.separator = separator;
		lineCache0 = null;
		cacheForLine = -1;
		fireTableStructureChanged();
	}

	public void setUseHeaderRow(boolean headerRow)
	{
		this.headerRow = headerRow;
		lineCache0 = null;
		cacheForLine = -1;
		fireTableStructureChanged();
	}

	public void setTextQualifier(String q)
	{
		if (q == null) return;
		if (!q.equals(NONE_QUALIFIER))
		{
			if (q.length() > 1)
			{
				q = q.substring(0, 1);
			}
			this.qualifier = q;
		}
		else
		{
			this.qualifier = ""; //$NON-NLS-1$
		}
		fireTableStructureChanged();
	}

	public boolean isCellEditable(int row, int col)
	{
		return false;
	}

	public void setValueAt(Object value, int row, int col)
	{
	}

	private static List tokenizeLine(int start_row,List lines,String separator,String qualifier)
	{
		if (start_row >= lines.size()) return null;
		String line = (String) lines.get(start_row);
		if (line == null) return null;
		if (line.equals("")) //$NON-NLS-1$
		{
			ArrayList al = new ArrayList();
			al.add(""); //$NON-NLS-1$
			return al;
		}
		
		ArrayList list = new ArrayList();
		StringTokenizer tk = new StringTokenizer(line, qualifier + separator, true);
		while (tk.hasMoreTokens())
		{
			list.add(tk.nextToken());
		}
		return list;
	}
		
	public static String[] parseLine(int start_row,List lines,String separator,String qualifier)
	{
		Object line = lines.get(start_row);
		if(line instanceof String[])
		{
			return (String[])line;
		}
		List list = tokenizeLine(start_row,lines,separator, qualifier);
		if (list == null) return new String[0];
		
		List retval = new ArrayList();
		for (int i = 0; i < list.size(); i++)
		{
			String element = (String)list.get(i);
			if (element.equals(qualifier)) 
			{
				i = handleQualifiedString(start_row,lines,i,list,retval,separator,qualifier);
				if(i == -1) return  null;
			}
			else if(element.equals(separator))
			{
				if(i == 0)
				{
					retval.add(null);
				}
				else if (i+1 < list.size()) 
				{
					String nextElement = (String)list.get(i+1);
					if (separator.equals(nextElement)) 
					{
						retval.add(null);
					}
				}
				else
				{
					retval.add(null);
				}
			} 
			else
			{
				retval.add(element);
			} 
		}
		String[] array = new String[retval.size()];
		retval.toArray(array);
		return array;
	}
	private static int handleQualifiedString(int start_row,List lines,int start_pos,List list,List retval,String separator,String qualifier)
	{
		StringBuffer sb = new StringBuffer();
		int i = start_pos + 1;
		boolean inString = true;
		// it is a start string but the next line starts with an /r or /n
		if(i == list.size())
		{
			list.add(""); //$NON-NLS-1$
		}
		for (; i < list.size(); i++)
		{
			String element = (String)list.get(i);
			if (qualifier.equals(element)) 
			{
				if (i+1 < list.size()) 
				{
					String nextElement = (String)list.get(i+1);
					if (nextElement.equals(qualifier)) 
					{
						sb.append(qualifier);//add single qualifier
						i++;//skip nextElement in element
						if(i+1 >= list.size() && inString)
						{
							//concat next line
							List newlist = tokenizeLine(start_row+1,lines,separator, qualifier);
							if (newlist == null)
							{
								// there should be a next line!
								lastLineNotComplete = true;
								return -1;
							}
							list.addAll(newlist);
							
							String line1 = (String) lines.get(start_row);
							String line2 = (String) lines.get(start_row+1);
							lines.set(start_row, line1+"\n"+line2); //$NON-NLS-1$
							lines.remove(start_row+1);
						}
					}
					else //end of string found
					{
						inString = !inString;
						break;
					}
				}
				else //end of line found
				{
					//do nothing
				}
			}
			else
			{
				sb.append(element);
				if (i+1 == list.size() && inString)
				{
					//concat next line
					List newlist = tokenizeLine(start_row+1,lines,separator, qualifier);
					if (newlist == null)
					{
						// there should be a next line!
						lastLineNotComplete = true;
						return -1;
					}
					list.addAll(newlist);
					
					String line1 = (String) lines.get(start_row);
					String line2 = (String) lines.get(start_row+1);
					lines.set(start_row, line1+"\n"+line2); //$NON-NLS-1$
					lines.remove(start_row+1);
				}
			}
		}
		String retString = sb.toString();
		if (retString.length() == 0) retString = null;
		retval.add(retString);
		return i;
	}
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < getColumnCount(); i++)
		{
			sb.append(getColumnName(i));
			sb.append(";"); //$NON-NLS-1$
		}
		sb.append("\n"); //$NON-NLS-1$
		for (int i = 0; i < getRowCount(); i++)
		{
			for (int j = 0; j < getColumnCount(); j++)
			{
				sb.append(getValueAt(i, j));
				sb.append(";"); //$NON-NLS-1$
			}
			sb.append("\n"); //$NON-NLS-1$
		}
		return sb.toString();
	}

	public static void main(String[] args)//for testing
	{
		List v = new ArrayList();
		v.add("\"jan,piet\"\t13\t\t\"345\"\t"); //$NON-NLS-1$
		v.add("\"tr\tude\"\t40\t\"\"\t200\t3"); //$NON-NLS-1$
		v.add("\"tr\"\"ude\"\t50\t\t20\t4"); //$NON-NLS-1$
		SeparatedASCIIImportTableModel m = new SeparatedASCIIImportTableModel();
		m.setList(v, false);
		m.setSeparator("\t"); //$NON-NLS-1$
		m.setTextQualifier(SeparatedASCIIImportTableModel.DUBBLE_QUOTE_QUALIFIER);
		System.out.println(m);
	}

	public SeparatedASCIIImportTableModel(List items)
	{
		super();
		String lastLine = setList(items, false);
		if(lastLine != null)
		{
			Debug.error("WARNING the last line of the file is not terminated correctly: " + lastLine); //$NON-NLS-1$
		}
	}
}
