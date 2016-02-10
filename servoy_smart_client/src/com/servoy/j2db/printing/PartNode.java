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
package com.servoy.j2db.printing;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.servoy.base.query.BaseQueryTable;
import com.servoy.base.query.IBaseSQLCondition;
import com.servoy.base.util.ITagResolver;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.dataprocessing.FoundSetManager;
import com.servoy.j2db.dataprocessing.IDataServer;
import com.servoy.j2db.dataprocessing.IDataSet;
import com.servoy.j2db.dataprocessing.IDisplay;
import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.ISwingFoundSet;
import com.servoy.j2db.dataprocessing.SQLGenerator;
import com.servoy.j2db.dataprocessing.SortColumn;
import com.servoy.j2db.dataprocessing.SubSummaryFoundSet;
import com.servoy.j2db.persistence.AggregateVariable;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.query.AbstractBaseQuery;
import com.servoy.j2db.query.CompareCondition;
import com.servoy.j2db.query.IQuerySelectValue;
import com.servoy.j2db.query.IQuerySort;
import com.servoy.j2db.query.ISQLTableJoin;
import com.servoy.j2db.query.Placeholder;
import com.servoy.j2db.query.QueryAggregate;
import com.servoy.j2db.query.QueryColumn;
import com.servoy.j2db.query.QuerySelect;
import com.servoy.j2db.query.QuerySort;
import com.servoy.j2db.query.TablePlaceholderKey;
import com.servoy.j2db.smart.dataui.DataRenderer;
import com.servoy.j2db.ui.IDisplayTagText;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.RendererParentWrapper;
import com.servoy.j2db.util.Text;

/**
 * Node used in chain
 *
 * @author jblok
 */
public class PartNode
{
	private PartNode child;//child node if any
	private final Part part;//the part this node is meant for (can be null in case of virtual body)
	private final DataRenderer renderer;// the renderer (can be null in case of virtual body)
	private final List<AggregateVariable> allAggregates;//all aggregates used for group by
	private final SortColumn[] sortColumns;//sort columns subtracted from all sort columns to be able to make related(!) query
	private final RendererParentWrapper renderParent;

	//it is possible to have a leading and trailing part on same column(s)
	private boolean isLeadingAndTrailingSubsummary = false;//must be true for leading and trailing on same column
	private Part second_part; //always the trailing part if IS_LEADING_AND_TRIALING_SUBSUMMARY
	private DataRenderer second_renderer; //always the trailing part renderer if IS_LEADING_AND_TRIALING_SUBSUMMARY
	private final IApplication application;

	void setSecondPartAsTrailingRenderer(Part p, DataRenderer r) throws RepositoryException
	{
		isLeadingAndTrailingSubsummary = true;
		second_part = p;
		second_renderer = r;
		if (second_renderer != null)
		{
			getAggregatesFromRenderer(allAggregates, (Form)p.getParent(), second_renderer);
		}
	}

	private void getAggregatesFromRenderer(final List<AggregateVariable> aggregates, final Form f, DataRenderer a_renderer) throws RepositoryException
	{
		Map<IPersist, IDisplay> allFields = a_renderer.getFieldComponents();
		Iterator<IDisplay> it = allFields.values().iterator();
		while (it.hasNext())
		{
			IDisplay display = it.next();
			if (display instanceof IDisplayData)
			{
				String dataProviderID = ((IDisplayData)display).getDataProviderID();
				if (dataProviderID != null)
				{
					IDataProvider dp = application.getFlattenedSolution().getDataproviderLookup(application.getFoundSetManager(), f).getDataProvider(
						dataProviderID);
					if (dp instanceof AggregateVariable)
					{
						if (!aggregates.contains(dp)) aggregates.add((AggregateVariable)dp);
					}
				}
				else if (display instanceof IDisplayTagText)
				{
					String tagText = ((IDisplayTagText)display).getTagText();
					Text.processTags(tagText, new ITagResolver()
					{
						public String getStringValue(String name)
						{
							try
							{
								IDataProvider dp = application.getFlattenedSolution().getDataproviderLookup(application.getFoundSetManager(), f).getDataProvider(
									name);
								if (dp instanceof AggregateVariable)
								{
									if (!aggregates.contains(dp)) aggregates.add((AggregateVariable)dp);
								}
							}
							catch (Exception e)
							{
								Debug.error(e);
							}
							return null;
						}
					});
				}
			}
		}
	}

	PartNode(FormPreviewPanel fpp, Part p, DataRenderer r, RendererParentWrapper renderParent, SortColumn[] scs) throws Exception
	{
		application = fpp.getApplication();
		sortColumns = scs;
		part = p;
		renderer = r;
		this.renderParent = renderParent;

		//get aggregates from part
		allAggregates = new ArrayList<AggregateVariable>();
		if (renderer != null)
		{
			getAggregatesFromRenderer(allAggregates, (Form)p.getParent(), renderer);
		}
	}

	/**
	 * Sets the child.
	 *
	 * @param child The child to set
	 */
	public void setChild(PartNode child)
	{
		this.child = child;
	}

	public PartNode getChild()
	{
		return child;
	}

	public SortColumn[] getSortColumns()
	{
		return sortColumns;
	}

	public boolean isLeading()
	{
		return (part.getPartType() == Part.LEADING_SUBSUMMARY);
	}

	public List<DataRendererDefinition> process(FormPreviewPanel fpp, FoundSet fs, Table table, QuerySelect sqlString) throws Exception
	{
		//Selection model must be in print mode to be able to set the selection to -1  . Otherwise is not allowed by the selectionModel
		((ISwingFoundSet)fs).getSelectionModel().hideSelectionForPrinting();

		FoundSet rootSet = (FoundSet)fs.copy(false);//this is needed because we must keep sql the same in foundset during printing
		foundSets.add(rootSet);

		IApplication app = fpp.getApplication();
		List<DataRendererDefinition> list = new ArrayList<DataRendererDefinition>();//retval
		if (part != null && (part.getPartType() == Part.LEADING_SUBSUMMARY || part.getPartType() == Part.TRAILING_SUBSUMMARY || isLeadingAndTrailingSubsummary))
		{
			QuerySelect newSQLString = AbstractBaseQuery.deepClone(sqlString);

			IDataServer server = app.getDataServer();

			//build the sql parts  based on sort columns
			ArrayList<IQuerySelectValue> selectCols = new ArrayList<IQuerySelectValue>();
			ArrayList<QueryColumn> groupbyCols = new ArrayList<QueryColumn>();
			ArrayList<QuerySort> sortbyCols = new ArrayList<QuerySort>();
			for (SortColumn element : sortColumns)
			{
				BaseQueryTable queryTable = sqlString.getTable();
				Relation[] relations = element.getRelations();
				if (relations != null)
				{
					for (Relation relation : relations)
					{
						ISQLTableJoin join = (ISQLTableJoin)sqlString.getJoin(queryTable, relation.getName());
						if (join == null)
						{
							Debug.log("Missing relation " + relation.getName() + " in join condition for form on table " + table.getName()); //$NON-NLS-1$ //$NON-NLS-2$
						}
						else
						{
							queryTable = join.getForeignTable();
						}
					}
				}

				Column column = (Column)element.getColumn();
				QueryColumn queryColumn = new QueryColumn(queryTable, column.getID(), column.getSQLName(), column.getType(), column.getLength(),
					column.getScale(), column.getFlags());
				selectCols.add(queryColumn);
				groupbyCols.add(queryColumn);
				sortbyCols.add(new QuerySort(queryColumn, element.getSortOrder() == SortColumn.ASCENDING));
			}

			//make sql
			for (int i = 0; i < allAggregates.size(); i++)
			{
				AggregateVariable ag = allAggregates.get(i);
				selectCols.add(new QueryAggregate(ag.getType(), new QueryColumn(newSQLString.getTable(), -1, ag.getColumnNameToAggregate(),
					ag.getDataProviderType(), ag.getLength(), 0, ag.getFlags()), ag.getName()));
			}

			newSQLString.setColumns(selectCols);
			newSQLString.setGroupBy(groupbyCols);
			ArrayList<IQuerySort> oldSort = newSQLString.getSorts();
			newSQLString.setSorts(sortbyCols);//fix the sort (if columns not are selected of used in groupby they cannot be used in sort)

			FoundSetManager foundSetManager = ((FoundSetManager)app.getFoundSetManager());
			String transaction_id = foundSetManager.getTransactionID(table.getServerName());
			IDataSet data = server.performQuery(app.getClientID(), table.getServerName(), transaction_id, newSQLString,
				foundSetManager.getTableFilterParams(table.getServerName(), newSQLString), false, 0, foundSetManager.pkChunkSize * 4, IDataServer.PRINT_QUERY);
			SubSummaryFoundSet newSet = new SubSummaryFoundSet(app.getFoundSetManager(), rootSet, sortColumns, allAggregates, data, table);//create a new FoundSet with 'data' and with right 'table', 'where','whereArgs'

			newSQLString.setSorts(oldSort);//restore the sort for child body parts

			//make new where for use in sub queries
			for (int i = 0; i < sortbyCols.size(); i++)
			{
				QueryColumn sc = (QueryColumn)(sortbyCols.get(i)).getColumn();
				newSQLString.addCondition(SQLGenerator.CONDITION_SEARCH, new CompareCondition(IBaseSQLCondition.EQUALS_OPERATOR, sc, new Placeholder(
					new TablePlaceholderKey(sc.getTable(), '#' + sc.getName()))));
			}

			int count = newSet.getSize();
			for (int ii = 0; ii < count; ii++)
			{
				QuerySelect newSQLStringCopy = AbstractBaseQuery.deepClone(newSQLString);//make copy for setting sort column

				//handle the child first, this puts the rootset in the right state! for use of related(!) fields in the subsums
				//THIS is EXTREMELY important for correct printing, see also SubSummaryFoundSet.queryForRelatedFoundSet
				List<DataRendererDefinition> childRetval = null;
				IFoundSetInternal curLeafFoundSet = null;
				if (child != null)
				{
					for (int i = 0; i < sortbyCols.size(); i++)
					{
						QueryColumn sc = (QueryColumn)(sortbyCols.get(i)).getColumn();
						TablePlaceholderKey placeholderKey = new TablePlaceholderKey(sc.getTable(), '#' + sc.getName());
						if (!newSQLStringCopy.setPlaceholderValue(placeholderKey, data.getRow(ii)[i]))
						{
							Debug.error(new RuntimeException("Could not set placeholder " + placeholderKey + " in query " + newSQLStringCopy + "-- continuing")); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
						}
					}
					childRetval = child.process(fpp, rootSet, table, newSQLStringCopy);
					curLeafFoundSet = child.getCurrentLeafFoundSet();
				}

				SubSummaryFoundSet.PrintState state = (SubSummaryFoundSet.PrintState)newSet.getRecord(ii);
				state.setDelegate(curLeafFoundSet);

				if (part.getPartType() == Part.LEADING_SUBSUMMARY)
				{
					state.doAggregatesLookup();
					list.add(new DataRendererDefinition(fpp, renderParent, part, renderer, state));
				}

				if (childRetval != null)
				{
					list.addAll(childRetval);
				}

				if (isLeadingAndTrailingSubsummary)
				{
					state.doAggregatesLookup();
					list.add(new DataRendererDefinition(fpp, renderParent, second_part, second_renderer, state));
				}
				else if (part.getPartType() == Part.TRAILING_SUBSUMMARY)
				{
					state.doAggregatesLookup();
					list.add(new DataRendererDefinition(fpp, renderParent, part, renderer, state));
				}
			}
		}
		else
		//for handeling (virtual) body part
		{
			rootSet.browseAll(sqlString);
			int count = app.getFoundSetManager().getFoundSetCount(rootSet);
			for (int ii = 0; ii < count; ii++)
			{
				currentLeafFoundSet = rootSet;
				list.add(new DataRendererDefinition(fpp, renderParent, part, renderer, rootSet, ii));
			}
		}
		return list;
	}

	private IFoundSetInternal currentLeafFoundSet;

	public IFoundSetInternal getCurrentLeafFoundSet()
	{
		if (child != null)
		{
			return child.getCurrentLeafFoundSet();
		}
		else
		{
			return currentLeafFoundSet;
		}
	}

	@Override
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append("Node "); //$NON-NLS-1$
		sb.append((part == null ? "Virtual Body Part" : part.getEditorName())); //$NON-NLS-1$
		if (isLeadingAndTrailingSubsummary)
		{
			sb.append(" &&  "); //$NON-NLS-1$
			sb.append(second_part.getEditorName());
		}
		sb.append(" "); //$NON-NLS-1$
		sb.append(allAggregates.size());
		sb.append(" aggregates"); //$NON-NLS-1$
		sb.append("\n\t"); //$NON-NLS-1$
		if (child != null) sb.append(child);
		return sb.toString();
	}

	private final List<FoundSet> foundSets = new ArrayList<FoundSet>();

	public void removePrintedStatesFromFoundSets()
	{
		for (int i = 0; i < foundSets.size(); i++)
		{
			FoundSet fs = foundSets.get(i);
			if (fs.getSize() > 0 && fs.getSelectedIndex() == -1) fs.setSelectedIndex(0);
			fs.flushAllCachedItems();
		}
		if (child != null) child.removePrintedStatesFromFoundSets();
	}
}
