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
package com.servoy.j2db.smart.scripting;


import java.awt.Component;
import java.util.HashMap;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableModel;

import org.mozilla.javascript.Function;
import org.mozilla.javascript.JavaMembers;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Wrapper;

import com.servoy.j2db.scripting.TwoNativeJavaMethod;
import com.servoy.j2db.smart.ListView;
import com.servoy.j2db.smart.TableView;
import com.servoy.j2db.smart.dataui.CellAdapter;
import com.servoy.j2db.smart.dataui.PortalComponent;

/**
 * This object is made for the FormPanel elements so that both the Renderer object and the Editor object will be called for there requests.
 *
 * Also the list view should be repainted. So that setEnabled/Color will be seen by the listview.
 *
 * @author jcompagner
 */
public class TwoNativeJavaObject extends NativeJavaObject
{
	private final NativeJavaObject javaObject2;
	private final Component listView;
	private final HashMap methodWrappers;
	private String executingFunction;

	/**
	 * @param scope
	 * @param javaObject
	 * @param members
	 */
	public TwoNativeJavaObject(Scriptable scope, Object javaObject, NativeJavaObject javaObject2, JavaMembers members, Component listView)
	{
		super(scope, javaObject, members);
		this.javaObject2 = javaObject2;
		this.listView = listView;
		this.methodWrappers = new HashMap();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.mozilla.javascript.NativeJavaObject#get(java.lang.String, org.mozilla.javascript.Scriptable)
	 */
	@Override
	public Object get(String name, Scriptable start)
	{
		Object returnObject = null;
		try
		{
			returnObject = super.get(name, start);
		}
		finally
		{
			if (returnObject instanceof Function)
			{
				if ("replaceSelectedText".equals(name)) return returnObject; //$NON-NLS-1$
				if (!"requestFocus".equals(name)) //$NON-NLS-1$
				{
					executingFunction = name;
					TwoNativeJavaMethod methodWrapper = (TwoNativeJavaMethod)methodWrappers.get(returnObject);
					if (methodWrapper == null)
					{
						methodWrapper = new TwoNativeJavaMethod(javaObject2, (Function)returnObject, listView);
						methodWrappers.put(returnObject, methodWrapper);
					}
					returnObject = methodWrapper;
				}
				else
				{
					final JComponent uiComponent = (JComponent)((javaObject instanceof Wrapper) ? ((Wrapper)javaObject).unwrap() : javaObject);
					if (listView instanceof TableView)
					{
						final TableView tv = (TableView)listView;
						tv.requestFocus();
						SwingUtilities.invokeLater(new Runnable()
						{
							public void run()
							{
								final int selectedRow = tv.getSelectedRow();
								for (int i = 0; i < tv.getColumnCount(); i++)
								{
									if (((CellAdapter)tv.getCellEditor(selectedRow, i)).getEditor() == uiComponent)
									{
										final int currentColumn = i;
										tv.setColumnSelectionInterval(currentColumn, currentColumn);
										tv.editCellAt(selectedRow, currentColumn);
										break;
									}
								}
							}
						});
					}
					else if (listView instanceof ListView)
					{
						if (listView.isEnabled())
						{
							// if list is not enabled, all controls are disabled or readonly, no sense to start edit and request focus
							// also when clicking and disabled we don't do anything
							((ListView)listView).editCellAt((((ListView)listView).getSelectedIndex()));
							uiComponent.requestFocus();
							SwingUtilities.invokeLater(new Runnable()
							{
								public void run()
								{
									if (!((ListView)listView).isEditing()) ((ListView)listView).editCellAt((((ListView)listView).getSelectedIndex()));
								}
							});
						}
					}
					else if (listView instanceof PortalComponent)
					{
						((PortalComponent)listView).editCellFor(uiComponent);
					}
				}
			}
			else
			{
				javaObject2.get(name, start);
			}
		}
		return returnObject;
	}

	@Override
	public Object unwrap()
	{
		// only go into edit if this is the event dispatch thread.
		// else debugger can block. If scripts will get its own thread this has to be invokeLater()
		if (SwingUtilities.isEventDispatchThread())
		{

			// when this component is unwrapped, NOT due to a function call on it,  (the reference is given to the java world)
			// or the location or width is requested we should make sure that it goes in editmode so that the component is in a valid
			// state. For example popupmenu tries to show an popup on the components location.
			if (executingFunction == null || executingFunction.startsWith("getLocation") || executingFunction.startsWith("getWidth")) //$NON-NLS-1$ //$NON-NLS-2$
			{
				final JComponent uiComponent = (JComponent)((javaObject instanceof Wrapper) ? ((Wrapper)javaObject).unwrap() : javaObject);
				if (listView instanceof TableView)
				{
					TableView tv = (TableView)listView;
					TableCellEditor cellEditor = tv.getCellEditor();
					if (!(cellEditor instanceof CellAdapter && ((CellAdapter)cellEditor).getEditor() == uiComponent))
					{
						int selectedRow = tv.getSelectedRow();
						for (int i = 0; i < tv.getColumnCount(); i++)
						{
							if (((CellAdapter)tv.getCellEditor(selectedRow, i)).getEditor() == uiComponent)
							{
								TableModel tm = tv.getModel();
								if (tm != null && tm.getRowCount() > 0)
								{
									if (tv.isCellEditable(selectedRow, i))
									{
										tv.setColumnSelectionInterval(i, i);
										tv.editCellAt(selectedRow, i);
									}
									else
									{
										// bounds can be modified even if readonly when moving columns
										((Component)uiComponent).setBounds(tv.getCellRect(selectedRow, i, false));
									}
								}
								break;
							}
						}
					}
				}
				else if (listView instanceof ListView)
				{
					((ListView)listView).editCellAt((((ListView)listView).getSelectedIndex()));
				}
				else if (listView instanceof PortalComponent)
				{
					((PortalComponent)listView).editCellFor(uiComponent);
				}
			}
			else executingFunction = null;
		}

		return super.unwrap();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.mozilla.javascript.NativeJavaObject#put(java.lang.String, org.mozilla.javascript.Scriptable, java.lang.Object)
	 */
	@Override
	public void put(String name, Scriptable start, Object value)
	{
		try
		{
			super.put(name, start, value);
			listView.repaint();
		}
		catch (RuntimeException e)
		{
			throw e;
		}
		finally
		{
			javaObject2.put(name, start, value);
		}
	}

}
