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
package com.servoy.j2db.server.headlessclient.dataui;

import java.awt.Event;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.model.AbstractWrapModel;
import org.apache.wicket.model.IComponentAssignedModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.IWrapModel;
import org.apache.wicket.model.Model;

import com.servoy.j2db.FormController;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.dataprocessing.DBValueList;
import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.dataprocessing.FoundSetListWrapper;
import com.servoy.j2db.dataprocessing.FoundSetManager;
import com.servoy.j2db.dataprocessing.SortColumn;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportDataProviderID;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.scripting.JSEvent;
import com.servoy.j2db.server.headlessclient.WebForm;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ScopesUtils;
import com.servoy.j2db.util.Utils;

/**
 * A model sorts a {@link WebCellBasedView} and keeps the sort state.
 *
 * @author jblok
 */
public class SortableCellViewHeaderGroup extends Model implements IComponentAssignedModel
{
	private static final long serialVersionUID = 1L;

	final private Map<String, Boolean> sorted = new HashMap<String, Boolean>();
	final private ServoyListView listView;
	final private AbstractBase cellview;
	final private Form form;

	public SortableCellViewHeaderGroup(Form form, final ServoyListView listView, AbstractBase cellview)
	{
		this.listView = listView;
		this.cellview = cellview;
		this.form = form;
	}

	public Boolean get(String name)
	{
		return sorted.get(name);
	}

	/**
	 * @see wicket.model.IAssignmentAwareModel#wrapOnAssignment(wicket.Component)
	 */
	public IWrapModel wrapOnAssignment(Component component)
	{
		return new WrapModel(component);
	}

	private boolean direction;


	public int getSortDirection()
	{
		return direction ? SortColumn.ASCENDING : SortColumn.DESCENDING;
	}

	public void recordSort(Map<String, Boolean> sortMap)
	{
		sorted.clear();
		sorted.putAll(sortMap);
	}

	protected final void sort(final String name, final WebCellBasedView view, int modifiers)
	{
		direction = Utils.getAsBoolean(sorted.get(name));
		direction = !direction;

		try
		{
			Iterator<IPersist> it = cellview.getAllObjects();
			while (it.hasNext())
			{
				IPersist element = it.next();
				if (element instanceof ISupportName && element instanceof ISupportDataProviderID)
				{
					if (name.equals(ComponentFactory.getWebID(form, element)))
					{
						FoundSet fs = ((FoundSetListWrapper)listView.getList()).getFoundSet();
						if (fs != null)
						{
							WebForm wf = listView.findParent(WebForm.class);
							FormController fc = null;
							if (wf != null) fc = wf.getController();
							GraphicalComponent gc = (GraphicalComponent)view.labelsFor.get(((ISupportName)element).getName());
							int labelForOnActionMethodId = 0;
							if (gc != null)
							{
								labelForOnActionMethodId = gc.getOnActionMethodID();
							}
							if (fc != null && labelForOnActionMethodId > 0)
							{ //execute on action
								JSEvent event = new JSEvent();
								event.setType(JSEvent.EventType.action);
								event.setFormName(view.getDataAdapterList().getFormController().getName());
								event.setModifiers(modifiers);
								event.setElementName(gc.getName());
								fc.executeFunction(String.valueOf(labelForOnActionMethodId),
									Utils.arrayMerge((new Object[] { event }),
										Utils.parseJSExpressions(gc.getFlattenedMethodArguments("onActionMethodID"))), //$NON-NLS-1$
									true, null, false, "onActionMethodID"); //$NON-NLS-1$
							}
							String id = ((ISupportDataProviderID)element).getDataProviderID();
							if (id != null)
							{
								if (cellview instanceof Portal && !ScopesUtils.isVariableScope(id))
								{
									int idx = id.lastIndexOf('.');
									if (idx > 0)
									{
										id = id.substring(idx + 1);
									}
								}

								IDataProvider dataProvider = null;
								if (fc != null)
								{
									dataProvider = fs.getFoundSetManager().getApplication().getFlattenedSolution().getDataproviderLookup(
										fs.getFoundSetManager(), fc.getForm()).getDataProvider(id);
								}
								if (!(fc != null && labelForOnActionMethodId > 0))
								{ // in case there is no onAction definned
									if (cellview instanceof Portal || fc == null || fc.getForm().getOnSortCmdMethodID() == 0)
									{
										List<String> sortingProviders = null;
										try
										{
											sortingProviders = DBValueList.getShowDataproviders(
												fs.getFoundSetManager().getApplication().getFlattenedSolution().getValueList(
													((ISupportDataProviderID)element).getValuelistID()),
												(Table)fs.getTable(), dataProvider == null ? id : dataProvider.getDataProviderID(), fs.getFoundSetManager());
										}
										catch (RepositoryException ex)
										{
											Debug.error(ex);
										}

										if (sortingProviders == null)
										{
											// no related sort, use sort on dataProviderID instead
											sortingProviders = Collections.singletonList(dataProvider == null ? id : dataProvider.getDataProviderID());
										}

										List<SortColumn> list = (modifiers & Event.SHIFT_MASK) != 0 ? fs.getSortColumns() : new ArrayList<SortColumn>();
										for (String sortingProvider : sortingProviders)
										{
											FoundSetManager fsm = (FoundSetManager)fs.getFoundSetManager();
											SortColumn sc = fsm.getSortColumn(fs.getTable(), sortingProvider, false);
											if (sc != null && sc.getColumn().getDataProviderType() != IColumnTypes.MEDIA)
											{
												for (SortColumn oldColumn : list)
												{
													if (oldColumn.equalsIgnoreSortorder(sc))
													{
														sc = oldColumn;
														break;
													}
												}
												if (!list.contains(sc)) list.add(sc);
												sc.setSortOrder(direction ? SortColumn.ASCENDING : SortColumn.DESCENDING);
											}
											fs.sort(list, false);
										}
									}
									else if (fc != null && fc.getForm().getOnSortCmdMethodID() != -1)
									{
										JSEvent event = new JSEvent();
										event.setType(JSEvent.EventType.none);
										event.setFormName(view.getDataAdapterList().getFormController().getName());
										event.setModifiers(modifiers);
										fc.executeFunction(String.valueOf(fc.getForm().getOnSortCmdMethodID()),
											Utils.arrayMerge(
												(new Object[] { dataProvider == null ? id
													: dataProvider.getDataProviderID(), Boolean.valueOf(direction), event }),
												Utils.parseJSExpressions(fc.getForm().getFlattenedMethodArguments("onSortCmdMethodID"))), //$NON-NLS-1$
											true, null, false, "onSortCmdMethodID"); //$NON-NLS-1$
									}
								}
								if ((modifiers & Event.SHIFT_MASK) == 0)
								{
									sorted.clear();
								}
								sorted.put(name, new Boolean(direction));

								listView.setCurrentPage(0);
							}
						}
						break;
					}
				}
			}
			listView.modelChanged();
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
	}

	class WrapModel extends AbstractWrapModel
	{
		private static final long serialVersionUID = 1L;
		private final Component component;

		WrapModel(Component component)
		{
			this.component = component;

		}

		/**
		 * @see wicket.model.IWrapModel#getNestedModel()
		 */
		public IModel getWrappedModel()
		{
			return SortableCellViewHeaderGroup.this;
		}

		@Override
		public void detach()
		{
			SortableCellViewHeaderGroup.this.detach();
		}

		/**
		 * @see wicket.model.IModel#getObject()
		 */
		@Override
		public Object getObject()
		{
			Boolean dir = sorted.get(component.getParent().getId());
			if (dir != null)
			{
				return (dir.booleanValue() ? "orderAsc" : "orderDesc"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			return null;
		}
	}
}