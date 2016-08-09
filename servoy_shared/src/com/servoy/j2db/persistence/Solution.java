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
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.base.util.DataSourceUtilsBase;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.FilteredIterator;
import com.servoy.j2db.util.IFilter;
import com.servoy.j2db.util.SortedList;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * A solution also root object for meta data
 *
 * @author jblok
 */
@ServoyDocumented(category = ServoyDocumented.DESIGNTIME, typeCode = IRepository.SOLUTIONS)
public class Solution extends AbstractRootObject implements ISupportChilds, ICloneable, ISupportUpdateableName, IMediaProvider
{
	// iterating & changing this map's contents will happen in synchronize blocks (the easier way would
	// be using the ConcurrentHashMap as before, but a bug in Terracotta does not allow ConcurrentHashMaps to be serialized/deserialized); when the bug is solved
	// sync blocks can be reverted to using ConcurrentHashMap. See http://jira.terracotta.org/jira/browse/CDV-1377
	public static final SerializableRuntimeProperty<HashMap<String, Style>> PRE_LOADED_STYLES = new SerializableRuntimeProperty<HashMap<String, Style>>()
	{
		private static final long serialVersionUID = 1L;
	};

	/**
	 * Store scope names when runnning in developer based in global js file names, this includes empty scopes.
	 * When not set (like running in appserver), fallback to scopes used in objects, this does not include empty scopes.
	 */
	public static final RuntimeProperty<String[]> SCOPE_NAMES = new RuntimeProperty<String[]>()
	{
	};

	/**
	 * Key for caching default messages defined for the solution.
	 */
	public static final RuntimeProperty<Map<String, String>> DEFAULT_MESSAGES = new RuntimeProperty<Map<String, String>>()
	{
	};

	public static final long serialVersionUID = 7758101764309127685L;

	public final static int TEXT_ORIENTATION_DEFAULT = 0;
	public final static int TEXT_ORIENTATION_LEFT_TO_RIGHT = 1;
	public final static int TEXT_ORIENTATION_RIGHT_TO_LEFT = 2;
	public final static int TEXT_ORIENTATION_LOCALE_SPECIFIC = 3;

	/*
	 * IPersist Attributes
	 */
	Solution(IRepository repository, SolutionMetaData metaData)
	{
		super(repository, metaData);
	}

	public SolutionMetaData getSolutionMetaData()
	{
		return (SolutionMetaData)getMetaData();
	}

	@Override
	public IRootObject getRootObject()
	{
		return this;
	}

	/*
	 * _____________________________________________________________ Methods for Form handling
	 */

	public Form getForm(int id)
	{
		return selectById(getForms(null, false), id);
	}

	public Iterator<Form> getForms(Table basedOnTable, boolean sort)
	{
		return getForms(getAllObjectsAsList(),
			basedOnTable == null ? null : DataSourceUtils.createDBTableDataSource(basedOnTable.getServerName(), basedOnTable.getName()), sort);
	}

	public static Iterator<Form> getForms(List<IPersist> childs, final String datasource, boolean sort)
	{
		Iterator<Form> retval = new TypeIterator<Form>(childs, IRepository.FORMS, new IFilter<Form>()
		{
			public boolean match(Object f)
			{
				return datasource == null || (f instanceof Form && datasource.equals(((Form)f).getDataSource()));
			}
		});
		if (sort)
		{
			return Utils.asSortedIterator(retval, NameComparator.INSTANCE);
		}
		return retval;
	}

	public Iterator<Form> getAllNormalForms(boolean sort)
	{
		return getForms(false, sort);
	}

	public Iterator<Form> getAllComponentForms(boolean sort)
	{
		return getForms(true, sort);
	}

	private Iterator<Form> getForms(final boolean reference, boolean sort)
	{
		List<IPersist> childs = getAllObjectsAsList();
		Iterator<Form> retval = new TypeIterator<Form>(childs, IRepository.FORMS, new IFilter<Form>()
		{
			public boolean match(Object f)
			{
				return (f instanceof Form && reference == ((Form)f).isFormComponent().booleanValue());
			}
		});
		if (sort)
		{
			return Utils.asSortedIterator(retval, NameComparator.INSTANCE);
		}
		return retval;
	}

	public Form getForm(String name)
	{
		return selectByName(getForms(null, false), name);
	}

	public Form createNewForm(IValidateName validator, Style style, String formName, String dataSource, boolean show_in_menu, Dimension size)
		throws RepositoryException
	{
		String name = formName == null ? "untitled" : formName; //$NON-NLS-1$
		// Check if name is in use.
		validator.checkName(name, 0, new ValidatorSearchContext(IRepository.FORMS), false);

		Form f = (Form)getChangeHandler().createNewObject(this, IRepository.FORMS);
		// Set all the required properties.
		f.setName(name);
		f.setDataSource(dataSource);
		f.setShowInMenu(show_in_menu);
		if (size != null) f.setSize(size);
		if (style != null) f.setStyleName(style.getName());
		addChild(f);
		if (getFirstFormID() == 0) setFirstFormID(f.getID());
		if (getSolutionType() == SolutionMetaData.MOBILE)
		{
			f.setNavigatorID(Form.NAVIGATOR_NONE);
		}
		return f;
	}

	/*
	 * _____________________________________________________________ Methods for Relation handling
	 */

	/**
	 * When basedOnTable is specified it also returns global relations (based entirely on globals)
	 */
	public Iterator<Relation> getRelations(ITable basedOnTable, boolean isPrimaryTable, boolean sort) throws RepositoryException
	{
		return getRelations(getRepository(), getAllObjectsAsList(), basedOnTable, isPrimaryTable, sort);
	}

	/**
	 * Get all relations
	 */
	public Iterator<Relation> getRelations(boolean sort)
	{
		return getRelations(getAllObjectsAsList(), sort);
	}

	/**
	 * Get all relations.
	 *
	 * @param childs
	 * @param sort
	 */
	public static Iterator<Relation> getRelations(List<IPersist> childs, boolean sort)
	{
		Iterator<Relation> rels = new TypeIterator<Relation>(childs, IRepository.RELATIONS);
		if (sort)
		{
			return Utils.asSortedIterator(rels, NameComparator.INSTANCE);
		}
		return rels;
	}

	/**
	 * Get all datasources for the table that are valid references to this table in all duplicate servers
	 * @throws RepositoryException
	 */
	public static List<String> getTableDataSources(IRepository repository, ITable table) throws RepositoryException
	{
		if (table == null)
		{
			return null;
		}

		return getTableDataSources(repository, table.getDataSource());
	}

	private static List<String> getTableDataSources(IRepository repository, String dataSource) throws RepositoryException
	{
		if (dataSource == null) return null;
		String[] stn = DataSourceUtilsBase.getDBServernameTablename(dataSource);
		List<String> dataSources = new ArrayList<String>();

		if (repository == null || stn == null) // inmem or rep == null
		{
			dataSources.add(dataSource);
		}
		else
		{
			String[] serverNames;
			try
			{
				serverNames = repository.getDuplicateServerNames(stn[0]);
			}
			catch (RemoteException e)
			{
				throw new RepositoryException("Could not get relations", e); //$NON-NLS-1$
			}
			if (serverNames.length == 1)
			{
				// no duplicates or an inmem table
				dataSources.add(dataSource);
			}
			else
			{
				// db tables with duplicate servers
				for (String serverName : serverNames)
				{
					dataSources.add(DataSourceUtils.createDBTableDataSource(serverName, stn[1]));
				}
			}
		}

		return dataSources;
	}

	/**
	 * Get relations based on the table. When basedOnTable is null and isPrimaryTable is true only global relations are returned.
	 *
	 * @param childs
	 * @param basedOnTable
	 * @param isPrimaryTable
	 * @param sort
	 * @throws RepositoryException
	 */
	public static Iterator<Relation> getRelations(IRepository repository, List<IPersist> childs, ITable basedOnTable, boolean isPrimaryTable, boolean sort)
		throws RepositoryException

	{
		return getRelations(repository, childs, basedOnTable, isPrimaryTable, sort, true, false, false);
	}

	/**
	 * Get relations based on the table. When basedOnTable is null and isPrimaryTable is true only global relations are returned.
	 *
	 * @param childs
	 * @param basedOnTable
	 * @param isPrimaryTable
	 * @param addGlobalsAsWellWhenPrimary when isPrimaryTable == true, it will return global relations as well.
	 * @param onlyGlobalsWhenForeign when isPrimaryTable == false, it will return only global relations that match basedOnTable.
	 * @param onlyLiteralsWhenForeign when isPrimaryTable == false, it will return only literal relations that match basedOnTable.
	 * @param sort
	 * @throws RepositoryException
	 */
	public static Iterator<Relation> getRelations(IRepository repository, List<IPersist> childs, ITable basedOnTable, boolean isPrimaryTable, boolean sort,
		boolean addGlobalsAsWellWhenPrimary, boolean onlyGlobalsWhenForeign, boolean onlyLiteralsWhenForeign) throws RepositoryException
	{
		Iterator<Relation> retval = getRelations(childs, false);

		List<Relation> filtered = new ArrayList<Relation>();

		List<String> dataSources = getTableDataSources(repository, basedOnTable); // null when table is null

		if (dataSources == null) dataSources = Collections.emptyList();

		while (retval.hasNext())
		{
			Relation r = retval.next();
			if (isPrimaryTable)
			{
				if (r.isGlobal())
				{
					if (addGlobalsAsWellWhenPrimary) filtered.add(r);
				}
				else if (dataSources.contains(r.getPrimaryDataSource()))
				{
					filtered.add(r);
				}
			}
			else
			{
				if (dataSources.contains(r.getForeignDataSource()))
				{
					if ((!onlyGlobalsWhenForeign && !onlyLiteralsWhenForeign) || (onlyGlobalsWhenForeign && r.isGlobal()) ||
						(onlyLiteralsWhenForeign && r.isLiteral())) filtered.add(r);
				}
			}
		}
		if (sort)
		{
			Collections.sort(filtered, NameComparator.INSTANCE);
		}
		return filtered.iterator();
	}

	public Relation getRelation(String name)
	{
		Iterator<Relation> it = getRelations(getAllObjectsAsList(), false);
		while (it.hasNext())
		{
			Relation sm = it.next();
			if (sm.getName().equals(name))
			{
				return sm;
			}
		}
		return null;
	}

	public Relation createNewRelation(IValidateName validator, String relationName, int joinType) throws RepositoryException
	{
		String name = relationName == null ? "untitled" : relationName; //$NON-NLS-1$
		//check if name is in use
		validator.checkName(name, 0, new ValidatorSearchContext(IRepository.RELATIONS), true);
		Relation obj = (Relation)getChangeHandler().createNewObject(this, IRepository.RELATIONS);
		obj.setJoinType(joinType);
		//set all the required properties

		obj.setName(name);
		addChild(obj);
		return obj;
	}

	public Relation createNewRelation(IValidateName validator, String name, String primaryDataSource, String foreignDataSource, int joinType)
		throws RepositoryException
	{
		Relation obj = createNewRelation(validator, name, joinType);
		obj.setPrimaryDataSource(primaryDataSource);
		obj.setForeignDataSource(foreignDataSource);
		return obj;
	}

	/*
	 * _____________________________________________________________ Methods for ValueList handling
	 */
	public Iterator<ValueList> getValueLists(boolean sort)
	{
		return getValueLists(getAllObjectsAsList(), sort);
	}

	public static Iterator<ValueList> getValueLists(List<IPersist> childs, boolean sort)
	{
		Iterator<ValueList> vls = new TypeIterator<ValueList>(childs, IRepository.VALUELISTS);
		if (sort)
		{
			return Utils.asSortedIterator(vls, NameComparator.INSTANCE);
		}
		return vls;
	}

	public ValueList getValueList(int id)
	{
		return selectById(getValueLists(false), id);
	}

	public ValueList getValueList(String name)
	{
		return selectByName(getValueLists(false), name);
	}

	public ValueList createNewValueList(IValidateName validator, String vlName) throws RepositoryException
	{
		String name = vlName == null ? "untitled" : vlName; //$NON-NLS-1$

		//check if name is in use
		validator.checkName(name, 0, new ValidatorSearchContext(IRepository.VALUELISTS), false);

		ValueList obj = (ValueList)getChangeHandler().createNewObject(this, IRepository.VALUELISTS);
		//set all the required properties

		obj.setName(name);

		addChild(obj);
		return obj;
	}

	/*
	 * _____________________________________________________________ Methods for ScriptVariable handling
	 */
	public Iterator<ScriptVariable> getScriptVariables(boolean sort)
	{
		return getScriptVariables(getAllObjectsAsList(), null, sort);
	}

	public Iterator<ScriptVariable> getScriptVariables(String scopeName, boolean sort)
	{
		return getScriptVariables(getAllObjectsAsList(), scopeName, sort);
	}

	public static Iterator<ScriptVariable> getScriptVariables(List<IPersist> childs, final String scopeName, boolean sort)
	{
		Iterator<ScriptVariable> vars = new TypeIterator<ScriptVariable>(childs, IRepository.SCRIPTVARIABLES, new IFilter<ScriptVariable>()
		{
			public boolean match(Object o)
			{
				return scopeName == null || (o instanceof ISupportScope && scopeName.equals(((ISupportScope)o).getScopeName()));
			}
		});
		if (sort)
		{
			return Utils.asSortedIterator(vars, NameComparator.INSTANCE);
		}
		return Utils.asSortedIterator(vars, LineNumberComparator.INSTANCE);
	}

	public ScriptVariable getScriptVariable(String scopeName, String name)
	{
		return selectByName(getScriptVariables(scopeName, false), name);
	}

	public ScriptVariable createNewScriptVariable(IValidateName validator, String scopeName, String varName, int variableType) throws RepositoryException
	{
		String name = varName == null ? "untitled" : varName; //$NON-NLS-1$

		boolean hit = false;
		int[] types = Column.allDefinedTypes;
		for (int element : types)
		{
			if (variableType == element)
			{
				hit = true;
				break;
			}
		}
		if (!hit)
		{
			throw new RepositoryException("unknow variable type: " + variableType); //$NON-NLS-1$
		}
		//check if name is in use
		validator.checkName(name, 0, new ValidatorSearchContext(scopeName, IRepository.SCRIPTVARIABLES), false);
		ScriptVariable obj = (ScriptVariable)getChangeHandler().createNewObject(this, IRepository.SCRIPTVARIABLES);
		//set all the required properties

		obj.setName(name);
		obj.setVariableType(variableType);
		obj.setScopeName(scopeName);
		addChild(obj);
		return obj;
	}

	/*
	 * _____________________________________________________________ Methods for ScriptCalculation/TableNode handling
	 */
	public Iterator<ScriptCalculation> getScriptCalculations(ITable basedOnTable, boolean sort) throws RepositoryException
	{
		return getScriptCalculations(getTableNodes(basedOnTable), sort).iterator();
	}

	public static List<ScriptCalculation> getScriptCalculations(Iterator<TableNode> tablenodes, boolean sort)
	{
		List<ScriptCalculation> retval = null;
		if (sort)
		{
			retval = new SortedList<ScriptCalculation>(NameComparator.INSTANCE);
		}
		else
		{
			retval = new ArrayList<ScriptCalculation>();
		}
		while (tablenodes.hasNext())
		{
			TableNode tableNode = tablenodes.next();
			Iterator<ScriptCalculation> scriptCalculations = tableNode.getScriptCalculations();
			while (scriptCalculations.hasNext())
			{
				retval.add(scriptCalculations.next());
			}
		}
		return retval;
	}

	public TableNode getOrCreateTableNode(String dataSource) throws RepositoryException
	{
		Iterator<TableNode> it = getTableNodes(dataSource);
		if (it.hasNext())
		{
			return it.next();
		}
		//create
		return createNewTableNode(dataSource);
	}

	public ScriptCalculation createNewScriptCalculation(IValidateName validator, ITable table, String name, String userTemplate) throws RepositoryException
	{
		return getOrCreateTableNode(table.getDataSource()).createNewScriptCalculation(validator, name, userTemplate, table);
	}

	public ScriptMethod createNewFoundsetMethod(IValidateName validator, String dataSource, String name, String userTemplate) throws RepositoryException
	{
		return getOrCreateTableNode(dataSource).createNewFoundsetMethod(validator, name, userTemplate);
	}

	public static List<ScriptMethod> getFoundsetMethods(Iterator<TableNode> tablenodes, boolean sort)
	{
		List<ScriptMethod> retval = null;
		if (sort)
		{
			retval = new SortedList<ScriptMethod>(NameComparator.INSTANCE);
		}
		else
		{
			retval = new ArrayList<ScriptMethod>();
		}
		while (tablenodes.hasNext())
		{
			TableNode tableNode = tablenodes.next();
			Iterator<ScriptMethod> methods = tableNode.getFoundsetMethods(false);
			while (methods.hasNext())
			{
				retval.add(methods.next());
			}
		}
		return retval;
	}

	public AggregateVariable createNewAggregateVariable(IValidateName validator, ITable table, String name, int aggType, String dataProviderIDToAggregate)
		throws RepositoryException
	{
		return getOrCreateTableNode(table.getDataSource()).createNewAggregateVariable(validator, name, aggType, dataProviderIDToAggregate, table);
	}

	public Iterator<AggregateVariable> getAggregateVariables(ITable basedOnTable, boolean sort) throws RepositoryException
	{
		return getAggregateVariables(getTableNodes(basedOnTable), sort);
	}

	public static Iterator<AggregateVariable> getAggregateVariables(Iterator<TableNode> tableNodes, boolean sort)
	{
		List<AggregateVariable> retval = null;
		if (sort)
		{
			retval = new SortedList<AggregateVariable>(NameComparator.INSTANCE);
		}
		else
		{
			retval = new ArrayList<AggregateVariable>();
		}
		while (tableNodes.hasNext())
		{
			TableNode tableNode = tableNodes.next();
			Iterator<AggregateVariable> aggregateVariables = tableNode.getAggregateVariables();
			while (aggregateVariables.hasNext())
			{
				retval.add(aggregateVariables.next());
			}
		}
		return retval.iterator();
	}

	//normally there is only one returned, but if using modules there can be multiple
	public Iterator<TableNode> getTableNodes(ITable table) throws RepositoryException
	{
		return getTableNodes(getRepository(), getAllObjectsAsList(), table);
	}

	public Iterator<TableNode> getTableNodes(String dataSource)
	{
		return getTableNodes(getAllObjectsAsList(), dataSource);
	}

	public static Iterator<TableNode> getTableNodes(List<IPersist> childs, final String dataSource)
	{
		if (dataSource == null)
		{
			return Collections.<TableNode> emptyList().iterator();
		}
		return new FilteredIterator<TableNode>(new TypeIterator<TableNode>(childs, IRepository.TABLENODES), new IFilter<TableNode>()
		{
			public boolean match(Object o)
			{
				return dataSource.equals(((TableNode)o).getDataSource());
			}
		});
	}

	public static Iterator<TableNode> getTableNodes(IRepository repository, List<IPersist> childs, ITable table) throws RepositoryException
	{
		List<TableNode> retval = new ArrayList<TableNode>();
		if (table != null)
		{
			List<String> dataSources = getTableDataSources(repository, table);

			Iterator<TableNode> it1 = new TypeIterator<TableNode>(childs, IRepository.TABLENODES);
			while (it1.hasNext())
			{
				TableNode node = it1.next();
				if (dataSources.contains(node.getDataSource()))
				{
					retval.add(node);
				}
			}
		}
		return retval.iterator();
	}

	public static boolean areDataSourcesCompatible(IRepository repository, String dataSource1, String dataSource2)
	{
		if (Utils.stringSafeEquals(dataSource1, dataSource2)) return true;

		// check other datasources of the same table
		List<String> compatibleDataSources = null;
		try
		{
			compatibleDataSources = Solution.getTableDataSources(repository, dataSource2); // null when table is null
		}
		catch (RepositoryException e)
		{
			Debug.trace(e);
		}
		if (compatibleDataSources != null && compatibleDataSources.contains(dataSource1))
		{
			return true;
		}
		return false;
	}

	public TableNode createNewTableNode(String dataSource) throws RepositoryException
	{
		TableNode obj = (TableNode)getChangeHandler().createNewObject(this, IRepository.TABLENODES);
		//set all the required properties

		obj.setDataSource(dataSource);
		addChild(obj);
		return obj;
	}


	/*
	 * _____________________________________________________________ Methods from this class
	 */


	public int getSolutionID()
	{
		return getID();
	}

	// do not need a set
	public void updateName(IValidateName validator, String arg) throws RepositoryException
	{
		validator.checkName(arg, getID(), new ValidatorSearchContext(this, IRepository.SOLUTIONS), false);
		//checkForNameChange(getRootObjectMetaData().getName(), arg);
		getRootObjectMetaData().setName(arg);
	}

	/*
	 * _____________________________________________________________ Runtime methods from this class
	 */

	/*
	 * _____________________________________________________________ Global Script Methods from this class
	 */

	public ScriptMethod getScriptMethod(int id)
	{
		return selectById(getScriptMethods(null, false), id);
	}

	public ScriptMethod getScriptMethod(String scopeName, String name)
	{
		return selectByName(getScriptMethods(scopeName, false), name);
	}

	public Iterator<ScriptMethod> getScriptMethods(String scopeName, boolean sort)
	{
		return getScriptMethods(getAllObjectsAsList(), scopeName, sort);
	}

	public static Iterator<ScriptMethod> getScriptMethods(List<IPersist> childs, final String scopeName, boolean sort)
	{
		Iterator<ScriptMethod> methods = new TypeIterator<ScriptMethod>(childs, IRepository.METHODS, new IFilter<ScriptMethod>()
		{
			public boolean match(Object o)
			{
				return scopeName == null || (o instanceof ISupportScope && scopeName.equals(((ISupportScope)o).getScopeName()));
			}
		});
		if (sort)
		{
			return Utils.asSortedIterator(methods, NameComparator.INSTANCE);
		}
		return methods;
	}

	public ScriptMethod createNewGlobalScriptMethod(IValidateName validator, String scopeName, String scriptName) throws RepositoryException
	{
		String name = scriptName == null ? "untitled" : scriptName; //$NON-NLS-1$
		validator.checkName(name, 0, new ValidatorSearchContext(scopeName, IRepository.METHODS), false);

		ScriptMethod obj = (ScriptMethod)getChangeHandler().createNewObject(this, IRepository.METHODS);

		//set all the required properties
		obj.setName(name);
		obj.setScopeName(scopeName);
		addChild(obj);
		return obj;
	}

	/*------------------------------------------------------------------------------------------------------------------------
	 * SECURITY
	 *
	 * Notes: These methods do not need to use the solution transaction to read the data.
	 */

	/**
	 * Flag that tells if authentication is needed in order to access the solution.
	 * If unchecked, the Smart Client will always require authentication, regardless of this setting.
	 * If checked, authentication is required, and either a provided loginSolution or otherwise the default Servoy login mechanism will be used.
	 * If default Servoy login mechanism is used, the "servoy.webclient.basic.authentication" setting on the Admin Page can be used to enable the use of the standard browser basic authentication.
	 */
	@ServoyClientSupport(ng = true, mc = true, wc = true, sc = true)
	public boolean getMustAuthenticate()
	{
		return getSolutionMetaData().getMustAuthenticate();
	}

	/**
	 * Sets the mustAuthenticate.
	 *
	 * @return Returns a boolean
	 */
	// Needed for solution properties editing
	public void setMustAuthenticate(boolean mustAuthenticate)
	{
		getSolutionMetaData().setMustAuthenticate(mustAuthenticate);
	}

	/*------------------------------------------------------------------------------------------------------------------------
	 * LISTENERS

	public void iPersistChanged(IPersist persist)
	{
		getChangeHandler().fireIPersistChanged(persist);
	}
	public void iPersistCreated(IPersist persist)
	{
		getChangeHandler().fireIPersistCreated(persist);
	}
	public void iPersistRemoved(IPersist persist)
	{
		getChangeHandler().fireIPersistRemoved(persist);
	}
	 */

	/*------------------------------------------------------------------------------------------------------------------------
	 * MEDIA HANDLING
	 */

	public Media createNewMedia(IValidateName validator, String name) throws RepositoryException
	{
		return createNewMedia(validator, name, 0);
	}

	public Media createNewMedia(IValidateName validator, String name, int skip_element_id_for_name_check) throws RepositoryException
	{
		if (name == null)
		{
			throw new RepositoryException("Cannot create media without a name"); //$NON-NLS-1$
		}

		// Check if name is in use.
		validator.checkName(name, skip_element_id_for_name_check, new ValidatorSearchContext(IRepository.MEDIA), false);

		Media m = (Media)getChangeHandler().createNewObject(this, IRepository.MEDIA);
		// Set all the required properties.
		m.setName(name);
//		m.setMediaData(mediaData);
//		m.setMimeType(mimeType);
		addChild(m);
		return m;
	}

	public Iterator<Media> getMedias(boolean sort)
	{
		return getMedias(getAllObjectsAsList(), sort);
	}

	public static Iterator<Media> getMedias(List<IPersist> childs, boolean sort)
	{
		Iterator<Media> medias = new TypeIterator<Media>(childs, IRepository.MEDIA);
		if (sort)
		{
			return Utils.asSortedIterator(medias, NameComparator.INSTANCE);
		}
		return medias;
	}

	public Media getMedia(int media_id)
	{
		return selectById(getMedias(false), media_id);
	}

	public Media getMedia(String name)
	{
		return selectByName(getMedias(false), name);
	}


	public String getProtectionPassword()
	{
		return getSolutionMetaData().getProtectionPassword();
	}

	/**
	 * @param alreadyKnownSolutions, can be null or a map (name -> solution)
	 * @return a list of RootObjectReference
	 */
	public List<RootObjectReference> getReferencedModules(Map<String, Solution> alreadyKnownSolutions) throws RepositoryException
	{
		List<RootObjectReference> referencedModules = new ArrayList<RootObjectReference>();
		String moduleNames = getModulesNames();
		if (moduleNames != null)
		{
			StringTokenizer tk = new StringTokenizer(moduleNames, ";,"); //$NON-NLS-1$
			int count = tk.countTokens();
			if (count > 0)
			{
				while (tk.hasMoreTokens())
				{
					try
					{
						SolutionMetaData metaData = null;
						int releaseNumber = -1;
						String moduleDescriptor = tk.nextToken();
						int i = moduleDescriptor.indexOf(':');
						String name = null;
						UUID moduleUuid = null;
						if (i != -1)
						{
							releaseNumber = Integer.parseInt(moduleDescriptor.substring(i + 1));
							moduleDescriptor = moduleDescriptor.substring(0, i);
						}
						if (alreadyKnownSolutions == null || !alreadyKnownSolutions.containsKey(moduleDescriptor))
						{
							if (moduleDescriptor.indexOf('-') != -1)
							{
								// A uuid reference.
								moduleUuid = UUID.fromString(moduleDescriptor);
								metaData = (SolutionMetaData)getRepository().getRootObjectMetaData(moduleUuid);
								if (metaData != null) name = metaData.getName();
							}
							else
							{
								// A module name; for backwards compatibility.
								name = moduleDescriptor;
								metaData = (SolutionMetaData)getRepository().getRootObjectMetaData(name, IRepository.SOLUTIONS);
								if (metaData != null) moduleUuid = metaData.getRootObjectUuid();
							}
							referencedModules.add(new RootObjectReference(name, moduleUuid, metaData, releaseNumber));
						}
					}
					catch (Exception e)
					{
						throw new RepositoryException(e);
					}
				}
			}
		}
		return referencedModules;
	}

	public Map<String, Solution> getReferencedModulesRecursive(Map<String, Solution> result) throws RepositoryException
	{
		List<RootObjectReference> referencedModules = getReferencedModules(null);
		Iterator<RootObjectReference> iterator = referencedModules.iterator();
		while (iterator.hasNext())
		{
			RootObjectReference moduleReference = iterator.next();
			RootObjectMetaData metaData = moduleReference.getMetaData();
			if (metaData != null)
			{
				try
				{
					Solution module = (Solution)getRepository().getActiveRootObject(metaData.getRootObjectId());
					if (!result.containsKey(metaData.getName()))
					{
						result.put(metaData.getName(), module);
						module.getReferencedModulesRecursive(result);
					}
				}
				catch (RemoteException e)
				{
					throw new RepositoryException(e);
				}
			}
		}
		return result;
	}

	/**
	 * Add the login solution as module by first removing exiting login solutions from the modules list
	 * @throws RepositoryException
	 */
	public void setLoginSolutionName(String loginSolutionName) throws RepositoryException
	{
		List<RootObjectReference> referencedModules = getReferencedModules(null);
		List<String> newModules = new ArrayList<String>();
		for (RootObjectReference moduleReference : referencedModules)
		{
			RootObjectMetaData metaData = moduleReference.getMetaData();
			if (metaData instanceof SolutionMetaData && ((SolutionMetaData)metaData).getSolutionType() != SolutionMetaData.LOGIN_SOLUTION)
			{
				newModules.add(((SolutionMetaData)metaData).getName());
			}
		}

		if (loginSolutionName != null) newModules.add(loginSolutionName);
		String newModulesNames = null;

		if (newModules.size() > 0)
		{
			StringBuilder sb = new StringBuilder();
			for (String module : newModules)
			{
				if (sb.length() > 0)
				{
					sb.append(',');
				}
				sb.append(module);
			}
			newModulesNames = sb.toString();
		}
		setModulesNames(newModulesNames);
	}

	/**
	 * Get the first module that is also a login solution.
	 * @throws RepositoryException
	 */
	public String getLoginSolutionName() throws RepositoryException
	{
		List<RootObjectReference> referencedModules = getReferencedModules(null);
		for (RootObjectReference moduleReference : referencedModules)
		{
			RootObjectMetaData metaData = moduleReference.getMetaData();
			if (metaData instanceof SolutionMetaData && ((SolutionMetaData)metaData).getSolutionType() == SolutionMetaData.LOGIN_SOLUTION)
			{
				return metaData.getName();
			}
		}

		return null;
	}

	/**
	 * The custom CSS used by the solution (a MEDIA lib entry). It can reference other media resources (even additional .css through relative '@import' statements).
	 * For NGClient - this CSS will be available directly in the browser.
	 */
	@ServoyClientSupport(ng = true, mc = false, wc = false, sc = false)
	public int getStyleSheetID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_STYLESHEET).intValue();
	}

	public void setStyleSheetID(int styleSheetMediaID)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_STYLESHEET, Integer.valueOf(styleSheetMediaID));
	}

	/**
	 * Get all scope names defined in this solution, includes globals scope.
	 */
	public Collection<String> getScopeNames()
	{
		String[] runtimeScopeNames = getRuntimeProperty(Solution.SCOPE_NAMES);
		if (runtimeScopeNames != null)
		{
			// set from developer based on global js file names
			return Arrays.asList(runtimeScopeNames);
		}

		// scope names not set from developer, use scope names found in solution objects
		Set<String> scopeNames = new HashSet<String>();
		scopeNames.add(ScriptVariable.GLOBAL_SCOPE);
		for (IPersist persist : getAllObjectsAsList())
		{
			if (persist instanceof ISupportScope)
			{
				String scopeName = ((ISupportScope)persist).getScopeName();
				if (scopeName != null)
				{
					scopeNames.add(scopeName);
				}
			}
		}
		return scopeNames;
	}

	/**
	 * Require authentication if mustAuthenticate flag is set or a loginSolution is defined.
	 */
	public boolean requireAuthentication()
	{
		try
		{
			return getMustAuthenticate() || getLoginSolutionName() != null;
		}
		catch (RepositoryException e)
		{
			Debug.error(e);
			return true; // on the safe side
		}
	}

	/**
	 * The first form that loads when a solution is deployed.
	 *
	 * NOTE: If the Login form is specified, then the firstForm is the first form that will load next after the loginForm.
	 */
	@ServoyClientSupport(ng = true, mc = true, wc = true, sc = true)
	public int getFirstFormID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_FIRSTFORMID).intValue();
	}

	/**
	 * The method that is executed when a solution closes. The default is -none-.
	 *
	 * @templatedescription Callback method for when solution is closed, force boolean argument tells if this is a force (not stoppable) close or not.
	 * @templatename onSolutionClose
	 * @templatetype Boolean
	 * @templateparam Boolean force if false then solution close can be stopped by returning false
	 * @templateaddtodo
	 * @templatecode
	 * return true
	 */
	public int getOnCloseMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONCLOSEMETHODID).intValue();
	}

	/**
	 * The method that is executed when a solution opens. The default is -none-.
	 *
	 * @templatedescription Callback method for when solution is opened
	 * When deeplinking into solutions, the argument part of the deeplink url will be passed in as the first argument
	 * All query parameters + the argument of the deeplink url will be passed in as the second argument
	 * For more information on deeplinking, see the chapters on the different Clients in the Deployment Guide.
	 * @templatename onSolutionOpen
	 * @templateaddtodo
	 * @templateparam String arg startup argument part of the deeplink url with which the Client was started
	 * @templateparam Object<Array<String>> queryParams all query parameters of the deeplink url with which the Client was started
	 */
	@ServoyClientSupport(ng = true, mc = true, wc = true, sc = true)
	public int getOnOpenMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONOPENMETHODID).intValue();
	}

	/**
	 * The menu bar title of a solution.
	 */
	public String getTitleText()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_TITLETEXT);
	}

	/**
	 * @param i
	 */
	public void setLoginFormID(int i)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_LOGINFORMID, i);
	}

	/**
	 * The name of the login form that loads when a solution is deployed.
	 */
	@ServoyClientSupport(ng = true, mc = true, wc = true, sc = true)
	public int getLoginFormID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_LOGINFORMID).intValue();
	}

	/**
	 * @param i
	 */
	public void setOnErrorMethodID(int i)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONERRORMETHODID, i);
	}

	/**
	 * The method that is executed when a solution opens and an error occurs. The default is -none.
	 *
	 * @templatedescription Callback method for when an error occurred (the error can be a JavaScript or Servoy Java error).
	 * @templatename onError
	 * @templatetype Boolean
	 * @templateparam * ex exception to handle
	 * @templateaddtodo
	 * @templatecode
	 * application.output('Exception Object: '+ex)
	 * application.output('MSG: '+ex.getMessage())
	 * if (ex instanceof ServoyException)
	 * {
	 *  /** @type {ServoyException} *&#47;
	 *  var servoyException = ex;
	 * 	application.output("is a ServoyException")
	 * 	application.output("Errorcode: "+servoyException.getErrorCode())
	 *  //var trace = servoyException.getStackTrace();
	 * 	if (servoyException.getErrorCode() == ServoyException.SAVE_FAILED)
	 * 	{
	 *  		plugins.dialogs.showErrorDialog( 'Error',  'It seems you did not fill in a required field', 'OK');
	 *  		//Get the failed records after a save
	 *  		var array = databaseManager.getFailedRecords()
	 *  		for( var i = 0 ; i < array.length ; i++ )
	 *  		{
	 * 			  var record = array[i];
	 * 			  application.output(record.exception);
	 * 			  if (record.exception instanceof DataException)
	 * 			  {
	 *              /** @type {DataException} *&#47;
	 *              var dataException = record.exception;
	 * 				application.output('SQL: '+dataException.getSQL())
	 * 				application.output('SQLState: '+dataException.getSQLState())
	 * 				application.output('VendorErrorCode: '+dataException.getVendorErrorCode())
	 * 			  }
	 * 		}
	 * 		return false
	 * 	}
	 * }
	 * //if returns false or no return, error is not reported to client; if returns true error is reported
	 * //by default error report means logging the error, in smart client an error dialog will also show up
	 * return true
	 */
	public int getOnErrorMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONERRORMETHODID).intValue();
	}

	/**
	 * @param i
	 */
	public void setFirstFormID(int i)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_FIRSTFORMID, i);
	}

	/**
	 * @param i
	 */
	public void setOnCloseMethodID(int i)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONCLOSEMETHODID, i);
	}

	/**
	 * @param i
	 */
	public void setOnOpenMethodID(int i)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONOPENMETHODID, i);
	}

	/**
	 * @param text
	 */
	public void setTitleText(String string)
	{
		String text = string;
		if (text != null && (text.equals("<default>") || text.equals(""))) //$NON-NLS-1$//$NON-NLS-2$
		{
			text = null;
		}
		setTypedProperty(StaticContentSpecLoader.PROPERTY_TITLETEXT, text);
	}

	/**
	 * The i18n database server connection and database table that stores the i18n keys for a solution.
	 */
	@ServoyClientSupport(mc = true, wc = true, sc = true)
	public String getI18nDataSource()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_I18NDATASOURCE);
	}

	public void setI18nDataSource(String dataSource)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_I18NDATASOURCE, dataSource);
	}

	public String getI18nServerName()
	{
		String[] stn = DataSourceUtilsBase.getDBServernameTablename(getI18nDataSource());
		return stn == null ? null : stn[0];
	}

	public void setI18nServerName(String serverName)
	{
		setI18nDataSource(DataSourceUtils.createDBTableDataSource("".equals(serverName) ? null : serverName, getI18nTableName())); //$NON-NLS-1$
	}

	public String getI18nTableName()
	{
		String[] stn = DataSourceUtilsBase.getDBServernameTablename(getI18nDataSource());
		return stn == null ? null : stn[1];
	}

	public void setI18nTableName(String tableName)
	{
		setI18nDataSource(DataSourceUtils.createDBTableDataSource(getI18nServerName(), "".equals(tableName) ? null : tableName)); //$NON-NLS-1$
	}

	/**
	 * The type of a solution; can be "Normal" (non-module), "Module", "Web client only", "Smart client only",
	 * "Login", "Authenticator", "Pre-import hook module", "Post-import hook module", "Mobile".
	 * These constants are defined in SolutionMetaData class.
	 */
	@ServoyClientSupport(ng = true, mc = true, wc = true, sc = true)
	public int getSolutionType()
	{
		return getSolutionMetaData().getSolutionType();
	}

	// Needed for solution properties editing
	public void setSolutionType(int type)
	{
		getSolutionMetaData().setSolutionType(type);
	}

	/**
	 * The list of modules that have been added to a solution.
	 */
	@ServoyClientSupport(ng = true, mc = true, wc = true, sc = true)
	public String getModulesNames()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_MODULESNAMES);
	}

	public void setModulesNames(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_MODULESNAMES, arg);
	}

	/**
	 * The direction that text is displayed.
	 *
	 * Options include:
	 * DEFAULT
	 * left to right
	 * right to left
	 * locale specific
	 */
	@ServoyClientSupport(ng = true, wc = true, sc = true)
	public int getTextOrientation()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_TEXTORIENTATION).intValue();
	}

	public void setTextOrientation(int o)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_TEXTORIENTATION, o);
	}

	/**
	 * Method that is executed when data broadcast occurs. The default is -none-.
	 *
	 * @templatedescription Callback method for data broadcast
	 * @templatename onDataBroadcast
	 * @templateparam String dataSource table data source
	 * @templateparam Number action see SQL_ACTION_TYPES constants
	 * @templateparam JSDataSet pks affected primary keys
	 * @templateparam Boolean cached data was cached
	 * @templateaddtodo
	 */
	public int getOnDataBroadcastMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONDATABROADCASTMETHODID).intValue();
	}

	public void setOnDataBroadcastMethodID(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONDATABROADCASTMETHODID, arg);
	}

	public int getOnInitMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONINITMETHODID).intValue();
	}

	public void setOnInitMethodID(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONINITMETHODID, arg);
	}

}
