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
package com.servoy.j2db;

import java.io.IOException;
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

import com.servoy.j2db.dataprocessing.IFoundSetManagerInternal;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.AbstractRepository;
import com.servoy.j2db.persistence.AggregateVariable;
import com.servoy.j2db.persistence.Bean;
import com.servoy.j2db.persistence.ChangeHandler;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.ColumnWrapper;
import com.servoy.j2db.persistence.FlattenedForm;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IActiveSolutionHandler;
import com.servoy.j2db.persistence.IColumn;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IDataProviderHandler;
import com.servoy.j2db.persistence.IDataProviderLookup;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistListener;
import com.servoy.j2db.persistence.IPersistVisitor;
import com.servoy.j2db.persistence.IRelationProvider;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.ISupportScriptProviders;
import com.servoy.j2db.persistence.ISupportUpdateableName;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.RootObjectMetaData;
import com.servoy.j2db.persistence.RootObjectReference;
import com.servoy.j2db.persistence.RuntimeProperty;
import com.servoy.j2db.persistence.ScriptCalculation;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptNameValidator;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.ServerProxy;
import com.servoy.j2db.persistence.SimplePersistFactory;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.persistence.Style;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.persistence.TableNode;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.server.shared.IFlattenedSolutionDebugListener;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * Runtime object which represents the entire solution, flattened, containing modules, caching stuff
 * 
 * @author jcompagner,jblok
 */
public class FlattenedSolution implements IPersistListener, IDataProviderHandler, IRelationProvider, ISupportScriptProviders
{
	private static RuntimeProperty<AbstractBase> CLONE_PROPERTY = new RuntimeProperty<AbstractBase>()
	{
	};

	private SolutionMetaData mainSolutionMetaData;
	private Solution mainSolution;
	private Solution[] modules;
	private FlattenedSolution loginFlattenedSolution;

	private Solution copySolution = null;

	private Map<Object, Integer> securityAccess;

	private Map<Table, Map<String, IDataProvider>> allProvidersForTable = null; //table -> Map(dpname,dp) ,runtime var
	private Map<String, IDataProvider> globalProviders = null; //global -> dp ,runtime var


	private Map<String, Relation> relationCacheByName = null;
	private Map<String, ScriptMethod> scriptMethodCacheByName = null;
	private Map<String, Form> formCacheByName = null;
	private Map<String, ScriptVariable> scriptVariableCacheByName = null;
	private Map<String, ValueList> valuelistCacheByName = null;
	private final List<IPersist> removedPersist = new ArrayList<IPersist>(3);

	private Map<Bean, Object> beanDesignInstances;

	private final Map<String, Set<String>> liveForms = new HashMap<String, Set<String>>();
	private final Map<String, Set<Object>> changedForms = new HashMap<String, Set<Object>>();

	private HashMap<String, Style> all_styles; // concurrent modification exceptions can happen; see comments from Solution->PRE_LOADED_STYLES.
	private HashMap<String, Style> user_created_styles; // concurrent modification exceptions shouldn't happen with current implementation for this map; no need to sync
	private SimplePersistFactory persistFactory;
	private IFlattenedSolutionDebugListener debugListener;

	/**
	 */
	public FlattenedSolution()
	{
	}

	/**
	 * @param solution
	 * @param activeSolutionHandler
	 * @throws RemoteException
	 * @throws RepositoryException
	 */
	public FlattenedSolution(SolutionMetaData solutionMetaData, IActiveSolutionHandler activeSolutionHandler) throws RepositoryException, RemoteException
	{
		setSolution(solutionMetaData, true, true, activeSolutionHandler);
	}

	@SuppressWarnings("unchecked")
	public <T extends AbstractBase> T createPersistCopy(T persist)
	{
		T copy = (T)getSolutionCopy().getChild(persist.getUUID());
		if (copy != null) return copy;

		if (persist.getRuntimeProperty(CLONE_PROPERTY) != null)
		{
			throw new RuntimeException("Persist " + persist + " already a clone"); //$NON-NLS-1$ //$NON-NLS-2$
		}

		copy = (T)persist.clonePersist();
		copy.setRuntimeProperty(CLONE_PROPERTY, persist);
		copySolution.addChild(copy);
		flush(persist);
		return copy;
	}

	@SuppressWarnings({ "unchecked", "nls" })
	public <T extends AbstractBase> T clonePersist(T persist, String newName, ISupportChilds newParent)
	{
		T clone = (T)persist.clonePersist();
		if (newParent != null)
		{
			newParent.addChild(clone);
		}
		if (clone instanceof ISupportUpdateableName)
		{
			try
			{
				((ISupportUpdateableName)clone).updateName(new ScriptNameValidator(this), newName);
			}
			catch (Exception e)
			{
				if (newParent != null)
				{
					newParent.removeChild(clone);
				}
				throw new RuntimeException("name '" + newName + "' invalid for the clone of " + ((ISupportName)persist).getName() + ", error: " +
					e.getMessage());
			}
		}
		clone.acceptVisitor(new IPersistVisitor()
		{
			public Object visit(IPersist o)
			{
				if (o instanceof AbstractBase)
				{
					((AbstractBase)o).resetUUID();
				}
				return IPersistVisitor.CONTINUE_TRAVERSAL;
			}
		});
		// give the form a new id so that assignments to tabpanels do get a new id.
		// can't do this for all elements because form.onloadid points then to something that doesn't exisits.
		if (clone instanceof Form)
		{
			try
			{
				((Form)clone).setID(getPersistFactory().getNewElementID(clone.getUUID()));
			}
			catch (RepositoryException e)
			{
				Debug.error("Couldnt set a new id on form " + clone, e);
			}
		}
		flush(persist);
		return clone;
	}

	/**
	 * @param form
	 */
	public void deletePersistCopy(AbstractBase persist, boolean revertToOriginal)
	{
		if (copySolution != null)
		{
			copySolution.removeChild(persist);
			flush(persist);
		}

		// if it has a clone persist property then it was cloned (and there is an original)
		// or revert was called on an original object then just ignore it.
		boolean exists = persist.getRuntimeProperty(CLONE_PROPERTY) != null || getAllObjectsAsList().indexOf(persist) != -1;
		if (!exists && revertToOriginal)
		{
			throw new RuntimeException("Cant revert " + persist + " to original, because there is no original"); //$NON-NLS-1$//$NON-NLS-2$
		}
		else if (exists && !revertToOriginal)
		{
			removedPersist.add(persist);
		}
	}

	public boolean hasCopy(IPersist persist)
	{
		return copySolution != null && copySolution.getChild(persist.getUUID()) != null;
	}


	public Solution getSolutionCopy()
	{
		if (mainSolution == null || copySolution != null) return copySolution;

		try
		{
			SimplePersistFactory factory = getPersistFactory();

			copySolution = factory.createDummyCopy(mainSolution);
			copySolution.setChangeHandler(new ChangeHandler(factory));
			copySolution.getChangeHandler().addIPersistListener(this);
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return copySolution;
	}

	protected int getMaxID()
	{
		final int[] maxId = new int[1];
		IPersistVisitor visitor = new IPersistVisitor()
		{
			public Object visit(IPersist o)
			{
				if (maxId[0] < o.getID())
				{
					maxId[0] = o.getID();
				}
				return IPersistVisitor.CONTINUE_TRAVERSAL;
			}
		};
		mainSolution.acceptVisitor(visitor);
		if (modules != null)
		{
			for (Solution module : modules)
			{
				module.acceptVisitor(visitor);
			}
		}
		int max = maxId[0];
		if (loginFlattenedSolution != null)
		{
			int loginMax = loginFlattenedSolution.getMaxID();
			if (loginMax > max)
			{
				max = loginMax;
			}
		}
		return max;
	}

	private SimplePersistFactory getPersistFactory()
	{
		if (persistFactory == null)
		{
			persistFactory = new SimplePersistFactory(getMaxID());
		}
		return persistFactory;
	}

	public Style createStyleCopy(Style style)
	{
		if (mainSolution == null && loginFlattenedSolution == null) return null;

		if (user_created_styles == null) user_created_styles = new HashMap<String, Style>();
		Style copy = user_created_styles.get(style.getName());
		if (copy == null)
		{
			copy = new Style(style.getRepository(), style.getRootObjectMetaData());
			copy.setContent(style.getContent());
			user_created_styles.put(copy.getName(), copy);
		}
		return copy;
	}


	public Style createStyle(String name, String content)
	{
		if (mainSolution == null)
		{
			if (loginFlattenedSolution == null)
			{
				return null;
			}
			return loginFlattenedSolution.createStyle(name, content);
		}

		if (all_styles.containsKey(name)) throw new RuntimeException("Style with name '" + name + "' already exists"); //$NON-NLS-1$ //$NON-NLS-2$
		if (user_created_styles == null) user_created_styles = new HashMap<String, Style>();
		if (user_created_styles.containsKey(name)) throw new RuntimeException("Style with name '" + name + "' already exists"); //$NON-NLS-1$ //$NON-NLS-2$

		RootObjectMetaData rootObjectMetaData = new RootObjectMetaData(-1, UUID.randomUUID(), name, IRepository.STYLES, 1, 1);
		rootObjectMetaData.setName(name);
		Style style = new Style(mainSolution.getRepository(), rootObjectMetaData);
		style.setContent(content);
		user_created_styles.put(name, style);
		return style;
	}

	/**
	 * @param style
	 * @return
	 */
	public boolean isUserStyle(Style style)
	{
		if (mainSolution == null && loginFlattenedSolution != null)
		{
			return loginFlattenedSolution.isUserStyle(style);
		}
		if (style == null || user_created_styles == null) return false;
		return user_created_styles.containsKey(style.getName());
	}

	/**
	 * @return
	 */
	public Solution getSolution()
	{
		if (mainSolution == null && loginFlattenedSolution != null)
		{
			return loginFlattenedSolution.getSolution();
		}
		return mainSolution;
	}

	public boolean isMainSolutionLoaded()
	{
		return mainSolution != null;
	}

	public void setSolution(SolutionMetaData sol, boolean loadLoginSolution, boolean loadMainSolution, IActiveSolutionHandler activeSolutionHandler)
		throws RepositoryException, RemoteException
	{
		if (sol == null) throw new IllegalArgumentException("use close method!"); //$NON-NLS-1$

		copySolution = null;
		persistFactory = null;
		mainSolution = null;
		user_created_styles = null;
		all_styles = null;
		mainSolutionMetaData = sol;

		if (loadLoginSolution)
		{
			// get login solution
			Solution[] loginSolutionAndModules = activeSolutionHandler.loadLoginSolutionAndModules(mainSolutionMetaData);
			if (loginSolutionAndModules != null && loginSolutionAndModules.length > 0)
			{
				if (loginFlattenedSolution == null)
				{
					loginFlattenedSolution = new FlattenedSolution();
				}
				loginFlattenedSolution.setSolutionAndModules(loginSolutionAndModules[0].getName(), loginSolutionAndModules);
			}
		}

		// get regular solution if available
		if (loadMainSolution && activeSolutionHandler.haveRepositoryAccess()) // local or already access to repository
		{
			// Note: referencedModules includes main solution
			List<RootObjectReference> referencedModules = activeSolutionHandler.getRepository().getActiveSolutionModuleMetaDatas(
				mainSolutionMetaData.getRootObjectId());
			if (referencedModules != null && referencedModules.size() != 0)
			{
				List<RootObjectMetaData> solutionAndModuleMetaDatas = new ArrayList<RootObjectMetaData>();
				for (int idx = 0; idx < referencedModules.size(); idx++)
				{
					RootObjectReference moduleReference = referencedModules.get(idx);
					RootObjectMetaData moduleMetaData = moduleReference.getMetaData();
					if (moduleMetaData == null)
					{
						throw new RepositoryException(Messages.getString("servoy.client.module.notfound", new Object[] { moduleReference.toString() })); //$NON-NLS-1$
					}
					solutionAndModuleMetaDatas.add(moduleMetaData);
				}

				Solution[] mods = activeSolutionHandler.loadActiveSolutions(solutionAndModuleMetaDatas.toArray(new RootObjectMetaData[solutionAndModuleMetaDatas.size()]));
				setSolutionAndModules(mainSolutionMetaData.getName(), mods);
			}
		}
	}

	private void setSolutionAndModules(String mainSolutionName, Solution[] mods) throws RemoteException
	{
		Map<String, Solution> modulesMap = new HashMap<String, Solution>(); //name -> solution

		// Note: mods includes main solution

		for (Solution module : mods)
		{
			if (module != null)
			{
				if (mainSolutionName.equals(module.getName()))
				{
					mainSolution = module;
				}
				else if (!modulesMap.containsKey(module.getName()))
				{
					modulesMap.put(module.getName(), module);
				}
			}
		}

		if (mainSolution != null)
		{
			if (mainSolution.getChangeHandler() != null)
			{
				mainSolution.getChangeHandler().addIPersistListener(this);
			}
			for (Solution module : modulesMap.values())
			{
				combineServerProxies(mainSolution.getServerProxies(), module.getServerProxies());
			}

			all_styles = mainSolution.getSerializableRuntimeProperty(Solution.PRE_LOADED_STYLES);
			if (all_styles == null)
			{
				// this should normally not happen, because when solutions are loaded this property is set (by the repository)
				all_styles = new HashMap<String, Style>();
				mainSolution.setSerializableRuntimeProperty(Solution.PRE_LOADED_STYLES, all_styles);
			}
		}

		modules = modulesMap.values().toArray(new Solution[modulesMap.size()]);

		for (Solution s : modules)
		{
			HashMap<String, Style> modStyles = s.getSerializableRuntimeProperty(Solution.PRE_LOADED_STYLES);
			if (modStyles != null)
			{
				synchronized (modStyles)
				{
					synchronized (all_styles) // the two syncs should not cause deadlock because the module's lock is always acquired first (so another thread cannot come and do it backwards) 
					{
						all_styles.putAll(modStyles);
					}
				}
			}
			if (s.getChangeHandler() != null)
			{
				s.getChangeHandler().addIPersistListener(this);
			}
		}
	}

	public void clearLoginSolution(IActiveSolutionHandler handler) throws IOException
	{
		if (loginFlattenedSolution != null)
		{
			loginFlattenedSolution.close(handler);
			loginFlattenedSolution = null;
			flushAllCachedData();
		}
	}

	public SolutionMetaData getMainSolutionMetaData()
	{
		return mainSolutionMetaData;
	}

	private void combineServerProxies(Map<String, IServer> orgs, Map<String, IServer> additional) throws RemoteException
	{
		synchronized (additional)
		{
			synchronized (orgs) // the two syncs should not cause deadlock because the module's lock is always acquired first (so another thread cannot come and do it backwards)
			{
				Iterator<IServer> it = additional.values().iterator();
				while (it.hasNext())
				{
					IServer addObject = it.next();
					IServer orgObject = orgs.get(addObject.getName());
					if (addObject instanceof ServerProxy)
					{
						ServerProxy add = (ServerProxy)addObject;
						if (orgObject == null)
						{
							orgs.put(add.getName(), add);
						}
						else if (orgObject instanceof ServerProxy)
						{
							ServerProxy org = (ServerProxy)orgObject;
							org.combineTables(add);
						}
					}
				}
			}
		}
	}

	/*
	 * Get a flattened form from this flattened solution.
	 * 
	 * <p>When the form does not have a parent, the form itself is returned
	 * 
	 * TODO: cache flattened forms for performance
	 */
	public Form getFlattenedForm(IPersist persist) throws RepositoryException
	{
		if (persist == null)
		{
			return null;
		}
		Form form = (Form)persist.getAncestor(IRepository.FORMS);
		if (form == null || form.getExtendsFormID() == 0)
		{
			// no form or nothing to flatten
			return form;
		}
		return new FlattenedForm(this, form);
	}

	/**
	 * Check whether a form can be instantiated.
	 * 
	 * @param form
	 * @return
	 */
	public boolean formCanBeInstantiated(Form form)
	{
		if (form != null)
		{
			// forms without parts are abstract
			List<Form> formHierarchy = new ArrayList<Form>();
			formHierarchy.add(form);
			Form f = form;
			while (true)
			{
				Iterator<IPersist> allObjects = f.getAllObjects();
				while (allObjects.hasNext())
				{
					if (allObjects.next() instanceof Part)
					{
						return true;
					}
				}
				if (form instanceof FlattenedForm || f.getExtendsFormID() == 0)
				{
					// no (more) hierarchy to investigate
					break;
				}
				f = getForm(f.getExtendsFormID());
				if (f == null || formHierarchy.contains(f) /* prevent cycles */)
				{
					break;
				}
				formHierarchy.add(f);
			}
		}
		return false;
	}

	int allObjectsSize = 256;
	List<IPersist> allObjectscache = null;

	public List<IPersist> getAllObjectsAsList()
	{
		if (mainSolution == null)
		{
			if (loginFlattenedSolution != null)
			{
				return loginFlattenedSolution.getAllObjectsAsList();
			}
			return Collections.emptyList();
		}
		if (allObjectscache == null)
		{
			List<IPersist> retval = new ArrayList<IPersist>(allObjectsSize);
			if (copySolution != null)
			{
				fillList(retval, copySolution);
			}
			fillList(retval, mainSolution);
			getAllModuleObjects(retval);
			if (loginFlattenedSolution != null)
			{
				retval.addAll(loginFlattenedSolution.getAllObjectsAsList());
			}
			allObjectsSize = retval.size();
			retval.removeAll(removedPersist);
			allObjectscache = retval;
		}
		return allObjectscache;
	}

	private void getAllModuleObjects(List<IPersist> copyInto)
	{
		if (modules != null)
		{
			for (Solution s : modules)
			{
				if (s != mainSolution)
				{
					fillList(copyInto, s);
				}
			}
		}
		return;
	}

	/**
	 * @param copyInto
	 * @param s
	 */
	private void fillList(List<IPersist> copyInto, Solution s)
	{
		List<IPersist> allObjectsAsList = s.getAllObjectsAsList();
		for (IPersist persist : allObjectsAsList)
		{
			if (copySolution == null || copySolution == s || copySolution.getChild(persist.getUUID()) == null)
			{
				copyInto.add(persist);
			}
		}
	}


	public void addSecurityAccess(Map<Object, Integer> sp)
	{
		addSecurityAccess(sp, true);
	}

	public void overrideSecurityAccess(Map<Object, Integer> sp)
	{
		addSecurityAccess(sp, false);
	}

	public void addSecurityAccess(Map<Object, Integer> sp, boolean combineIfExisting)
	{
		if (securityAccess == null)
		{
			securityAccess = sp;
		}
		else if (sp != null)
		{
			Iterator<Map.Entry<Object, Integer>> iterator = sp.entrySet().iterator();
			while (iterator.hasNext())
			{
				Map.Entry<Object, Integer> entry = iterator.next();
				Object elementUID = entry.getKey();
				int newValue = entry.getValue().intValue();
				Integer currentValue = securityAccess.get(elementUID);
				if (currentValue != null && combineIfExisting)
				{
					newValue |= currentValue.intValue();
				}
				securityAccess.put(elementUID, new Integer(newValue));
			}
		}
	}

	public void clearSecurityAccess()
	{
		if (securityAccess != null)
		{
			securityAccess.clear();
			securityAccess = null;
		}
	}

	public int getSecurityAccess(Object element_id)
	{
		if (securityAccess == null) return -1;

		Integer i = securityAccess.get(element_id);
		if (i == null)
		{
			return -1;
		}
		else
		{
			return i.intValue();
		}
	}

	//commit all user data
	public void close(IActiveSolutionHandler handler) throws IOException
	{
		flushAllCachedData();

		if (loginFlattenedSolution != null)
		{
			loginFlattenedSolution.close(handler);
		}
		loginFlattenedSolution = null;

		if (mainSolution != null)
		{
			if (mainSolution.getChangeHandler() != null)
			{
				mainSolution.getChangeHandler().removeIPersistListener(this);
			}
			if (handler != null) handler.saveActiveSolution(mainSolution);//try disksave in client
			mainSolution = null;
		}

		if (modules != null)
		{
			for (Solution element : modules)
			{
				if (handler != null) handler.saveActiveSolution(element);//try disksave in client
				if (element.getChangeHandler() != null)
				{
					element.getChangeHandler().removeIPersistListener(this);
				}
			}
			modules = null;
		}

		mainSolutionMetaData = null;

		changedForms.clear();
		liveForms.clear();
		designFormName = null;

		copySolution = null;
		persistFactory = null;
		removedPersist.clear();

		allObjectsSize = 256;
	}

	public synchronized IDataProvider getGlobalDataProvider(String id) throws RepositoryException
	{
		if (id == null) return null;

		if (globalProviders == null) globalProviders = new HashMap<String, IDataProvider>();

		IDataProvider retval = globalProviders.get(id);
		if (retval != null)
		{
			return retval;
		}

		int indx = id.lastIndexOf('.'); // in case of multi-level relations we have more that 1 dot
		if (indx > 0 && !id.startsWith(ScriptVariable.GLOBAL_DOT_PREFIX))
		{
			String rel_name = id.substring(0, indx);
			String col = id.substring(indx + 1);

			Relation[] relations = getRelationSequence(rel_name);
			if (relations == null)
			{
				return null;
			}

			Relation r = relations[relations.length - 1];
			Column[] cols = r.getForeignColumns();
			if (cols == null || cols.length == 0) return null;

			IDataProvider c = getDataProviderForTable(r.getForeignTable(), col);

			if (c == null) return null;

			if (r != null && c instanceof IColumn)
			{
				ColumnWrapper rc = new ColumnWrapper((IColumn)c, relations);
				globalProviders.put(id, rc);
				return rc;
			}
			else
			{
				globalProviders.put(id, c);
				return c;
			}
		}

		//search all objects,will return globals
		Iterator<ScriptVariable> it = getScriptVariables(false);
		while (it.hasNext())
		{
			IPersist p = it.next();
			if (p instanceof IDataProvider && ((IDataProvider)p).getDataProviderID().equals(id))
			{
				globalProviders.put(((IDataProvider)p).getDataProviderID(), (IDataProvider)p);
				retval = (IDataProvider)p;
			}
		}
		return retval;
	}

	/**
	 * Returns script calculations and aggregates and columns
	 */
	public IDataProvider getDataProviderForTable(Table table, String dataProviderID) throws RepositoryException
	{
		if (table == null || dataProviderID == null) return null;
		// check for calculations first because of stored calculations
		Map<String, IDataProvider> dps = getAllDataProvidersForTable(table);
		IDataProvider dataProvider = null;
		if (dps != null)
		{
			dataProvider = dps.get(dataProviderID);
		}
		if (dataProvider != null) return dataProvider;
		Column column = table.getColumn(Utils.generateNormalizedName(dataProviderID));
		if (column != null)
		{
			return column;
		}
		return null;
	}

	public synchronized Map<String, IDataProvider> getAllDataProvidersForTable(Table table) throws RepositoryException
	{
		if (table == null) return null;
		if (allProvidersForTable == null) allProvidersForTable = new HashMap<Table, Map<String, IDataProvider>>();

		Map<String, IDataProvider> dataProvidersMap = allProvidersForTable.get(table);
		if (dataProvidersMap == null)
		{
			dataProvidersMap = new HashMap<String, IDataProvider>();

			//1) first the columns
			Iterator<Column> columns = table.getColumns().iterator();
			while (columns.hasNext())
			{
				Column col = columns.next();
				ColumnInfo ci = col.getColumnInfo();
				if (ci != null && ci.isExcluded())
				{
					continue;
				}
				dataProvidersMap.put(col.getDataProviderID(), col);
			}

			//2) last the scriptcalculations and aggregates so the overlap the columns in case of stored calcs			
			Iterator<TableNode> tableNodes = getTableNodes(table);
			while (tableNodes.hasNext())
			{
				TableNode tableNode = tableNodes.next();
				if (tableNode != null)
				{
					Iterator<IPersist> it2 = tableNode.getAllObjects();
					while (it2.hasNext())
					{
						IPersist persist = it2.next();
						if (persist instanceof IDataProvider)
						{
							dataProvidersMap.put(((IDataProvider)persist).getDataProviderID(), (IDataProvider)persist);
						}
					}
				}
			}
			allProvidersForTable.put(table, dataProvidersMap);
		}
		return dataProvidersMap;
	}

	public synchronized void flushDataProvidersForPersist(IPersist persist)
	{
		if (persist == null || allProvidersForTable == null) return;

		TableNode tableNode = (TableNode)persist.getAncestor(IRepository.TABLENODES);
		if (tableNode != null && tableNode.getDataSource() != null) // it might reach this code from the persist created event - where data source is not yet set
		{
			try
			{
				Table table = tableNode.getTable();
				if (table != null)
				{
					allProvidersForTable.remove(table);
				}
			}
			catch (RepositoryException e)
			{
				Debug.log(e);
			}
		}
	}

	public synchronized void flushDataProvidersForTable(Table table)
	{
		if (table != null && allProvidersForTable != null)
		{
			allProvidersForTable.remove(table);
		}
	}

	public Collection<Style> flushUserStyles()
	{
		if (user_created_styles != null)
		{
			Collection<Style> col = user_created_styles.values();
			user_created_styles = null;
			return col;
		}
		return Collections.emptyList();
	}

	public synchronized void flushAllCachedData()
	{
		globalProviders = null;
		allProvidersForTable = null;
		relationCacheByName = null;
		scriptVariableCacheByName = null;
		scriptMethodCacheByName = null;
		formCacheByName = null;
		valuelistCacheByName = null;
		dataProviderLookups = null;
		all_styles = null;
		beanDesignInstances = null;

		allObjectscache = null;
	}

	class FormAndTableDataProviderLookup implements IDataProviderLookup
	{
		private Map<String, IDataProvider> formProviders = null; //cache for recurring lookup

		private final Table tableDisplay; // may be null

		private final Form form;

		FormAndTableDataProviderLookup(Form form, Table td)
		{
			this.form = form;
			tableDisplay = td;
			formProviders = Collections.synchronizedMap(new HashMap<String, IDataProvider>());
		}

		public IDataProvider getDataProvider(String id) throws RepositoryException
		{
			if (id == null) return null;
			IDataProvider retval = getGlobalDataProvider(id);
			if (retval == null)
			{
				retval = formProviders.get(id);
				if (retval != null)
				{
					return retval;
				}

				retval = getDataProviderForTable(getTable(), id);
				if (retval != null)
				{
					formProviders.put(id, retval);
				}
				else
				{
					retval = form.getScriptVariable(id);
					if (retval != null)
					{
						formProviders.put(id, retval);
					}
				}
			}
			return retval;
		}

		/**
		 * get the table (may be null)
		 */
		public Table getTable() throws RepositoryException
		{
			return tableDisplay;
		}
	}

	private Map<IPersist, IDataProviderLookup> dataProviderLookups;

	public synchronized void flushDataProviderLookups(final IPersist p)
	{
		if (dataProviderLookups != null)
		{
			if (p instanceof Form)
			{
				// flush all forms to be sure it affects parent/child forms as well
				Iterator<IPersist> it = dataProviderLookups.keySet().iterator();
				while (it.hasNext())
				{
					if (it.next() instanceof Form) it.remove();
				}
			}
			else
			{
				dataProviderLookups.remove(p);
			}
		}
	}

	public synchronized IDataProviderLookup getDataproviderLookup(IFoundSetManagerInternal foundSetManager, final IPersist p)
	{
		if (dataProviderLookups == null) dataProviderLookups = new HashMap<IPersist, IDataProviderLookup>();
		IDataProviderLookup retval = dataProviderLookups.get(p);
		if (retval == null)
		{
			if (p instanceof Form)
			{
				ITable t = null;
				try
				{
					if (foundSetManager == null)
					{
						t = ((Form)p).getTable();
					}
					else
					{
						t = foundSetManager.getTable(((Form)p).getDataSource());
					}
				}
				catch (RepositoryException e)
				{
					Debug.error(e);
				}
				retval = new FormAndTableDataProviderLookup((Form)p, (Table)t);
			}
			else if (p instanceof Portal)
			{
				Table t = null;
				try
				{
					Relation[] relations = getRelationSequence(((Portal)p).getRelationName());
					if (relations == null)
					{
						return null;
					}
					t = relations[relations.length - 1].getForeignTable();
				}
				catch (RepositoryException e)
				{
					Debug.error(e);
				}
				retval = new FormAndTableDataProviderLookup((Form)p.getParent(), t);
			}
			else
			//solution
			{
				retval = new IDataProviderLookup()
				{
					public IDataProvider getDataProvider(String id) throws RepositoryException
					{
						return getGlobalDataProvider(id);
					}

					public Table getTable() throws RepositoryException
					{
						return null;
					}
				};
			}

			dataProviderLookups.put(p, retval);
		}
		return retval;
	}

	public Solution[] getModules()
	{
		return modules;
	}

	private void createRelationCache()
	{
		try
		{
			relationCacheByName = new HashMap<String, Relation>();

			Iterator<Relation> it = getRelations(false);
			while (it.hasNext())
			{
				Relation r = it.next();
				if (r != null)
				{
					relationCacheByName.put(r.getName(), r);
				}
			}
		}
		catch (RepositoryException ex)
		{
			Debug.error(ex);
		}
	}

	public synchronized Relation getRelation(String name)
	{
		if (name.indexOf('.') >= 0)
		{
			// if we get here the relation name should be analyzed using getRelationSequence(name).
			Debug.log("Unexpected relation name lookup", new Exception(name)); //$NON-NLS-1$
			return null;
		}
		if (relationCacheByName == null)
		{
			createRelationCache();
		}
		return relationCacheByName.get(name);
	}

	/**
	 * Get relations from string rel1.rel2. ... .reln
	 * 
	 * @param name
	 * @return
	 */
	public synchronized Relation[] getRelationSequence(String name)
	{
		if (name == null)
		{
			return null;
		}
		String[] parts = name.split("\\."); //$NON-NLS-1$
		Relation[] seq = new Relation[parts.length];
		Relation prev = null;
		for (int i = 0; i < parts.length; i++)
		{
			Relation relation = getRelation(parts[i]);
			if (relation == null || prev != null &&
				(prev.getForeignDataSource() == null || !prev.getForeignDataSource().equals(relation.getPrimaryDataSource())))
			{
				return null;
			}
			seq[i] = relation;
			prev = relation;
		}
		return seq;
	}


	private void flushRelations()
	{
		relationCacheByName = null;
	}

	private void flushValuelists()
	{
		valuelistCacheByName = null;
	}

	private void flushForms()
	{
		formCacheByName = null;
	}

	private void flushScriptMethods()
	{
		scriptMethodCacheByName = null;
	}

	private void flushScriptVariables()
	{
		scriptVariableCacheByName = null;
		globalProviders = null;
	}

	/**
	 * Search for a persist by UUID in the main solution and all modules.
	 * 
	 * @param uuid
	 * @return
	 */
	public IPersist searchPersist(UUID uuid)
	{
		if (mainSolution != null)
		{
			IPersist persist = AbstractRepository.searchPersist(mainSolution, uuid);
			if (persist != null)
			{
				return persist;
			}
			if (modules != null)
			{
				for (Solution module : modules)
				{
					persist = AbstractRepository.searchPersist(module, uuid);
					if (persist != null)
					{
						return persist;
					}
				}
			}
		}
		if (loginFlattenedSolution != null)
		{
			return loginFlattenedSolution.searchPersist(uuid);
		}
		return null;
	}

	public void iPersistChanged(IPersist persist)
	{
		flush(persist);
	}

	public void iPersistCreated(IPersist persist)
	{
		flush(persist);
	}

	public void iPersistRemoved(IPersist persist)
	{
		flush(persist);
	}

	/**
	 * @param persist
	 */
	private void flush(IPersist persist)
	{
		if (persist instanceof Relation) flushRelations();
		if (persist instanceof ValueList) flushValuelists();
		if (persist instanceof Form) flushForms();
		if (persist instanceof ScriptMethod) flushScriptMethods();
		if (persist instanceof ScriptVariable) flushScriptVariables();
		flushDataProvidersForPersist(persist);
		flushDataProviderLookups(persist);

		allObjectscache = null;

		if (loginFlattenedSolution != null)
		{
			loginFlattenedSolution.flush(persist);
		}
	}

	public synchronized Style getStyle(String name)
	{
		if (user_created_styles != null)
		{
			Style style = user_created_styles.get(name);
			if (style != null) return style;
		}
		if (loginFlattenedSolution != null)
		{
			return loginFlattenedSolution.getStyle(name);
		}
		Style style = all_styles.get(name);
		if (style == null)
		{
			try
			{
				style = (Style)mainSolution.getRootObject().getRepository().getActiveRootObject(name, IRepository.STYLES);
				if (style != null)
				{
					synchronized (all_styles)
					{
						all_styles.put(name, style);
					}
				}
			}
			catch (Exception e)
			{
				Debug.error("error loading style '" + name + "'", e); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}

		return style;
	}

	public Style getStyleForForm(Form f)
	{
		return getStyleForForm(f, null);
	}

	public Style getStyleForForm(Form f, Map<String, String> overridenStyles)
	{
		if (loginFlattenedSolution != null)
		{
			return loginFlattenedSolution.getStyleForForm(f, overridenStyles);
		}
		synchronized (this)
		{
			String style_name = f.getStyleName();
			if (style_name != null)
			{
				String overridden = null;
				if (overridenStyles != null && (overridden = overridenStyles.get(style_name)) != null)
				{
					style_name = overridden;
				}
				if (user_created_styles != null)
				{
					Style style = user_created_styles.get(style_name);
					if (style != null) return style;
				}
				Style style = all_styles.get(style_name);
				if (style != null)
				{
					return style;
				}
				if (overridden != null)
				{
					try
					{
						style = (Style)f.getRootObject().getRepository().getActiveRootObject(overridden, IRepository.STYLES);
						if (style != null)
						{
							synchronized (all_styles)
							{
								all_styles.put(style.getName(), style);
							}
							return style;
						}
					}
					catch (Exception e)
					{
						Debug.error("Couldn't get the new style " + overridden + " for the overridden one:" + f.getStyleName()); //$NON-NLS-1$ //$NON-NLS-2$
						Debug.error(e);
					}

				}
			}
			Style s = loadStyleForForm(f);
			if (s != null)
			{
				synchronized (all_styles)
				{
					all_styles.put(s.getName(), s);
				}
			}
			return s;
		}
	}

	public static Style loadStyleForForm(Form f)
	{
		try
		{
			String style_name = f.getStyleName();
			if (!(f instanceof FlattenedForm))
			{
				Form extendedForm;
				int extended_form_id = f.getExtendsFormID();

				List<RootObjectReference> modulesMetaData = null;

				// We keep track of visited forms, to avoid infinite loop.
				Set<Integer> visited = new HashSet<Integer>();

				while (style_name == null && extended_form_id > 0)
				{
					visited.add(new Integer(extended_form_id));

					// if this is hit here with a normal (not flattened form) that has an extend.
					// and that form is a solution modal form. the f.getSolution() doesn't have to find it.
					// because f.getSolution() is the copy solution thats inside the FlattenedSolution.
					// that one doesn't have any other forms. But normally all forms should be flattened. 
					extendedForm = f.getSolution().getForm(extended_form_id);
					if (extendedForm == null) // check for module form
					{
						if (modulesMetaData == null)
						{
							modulesMetaData = f.getSolution().getRepository().getActiveSolutionModuleMetaDatas(f.getSolution().getID());
						}

						for (RootObjectReference moduleMetaData : modulesMetaData)
						{
							Solution module = (Solution)f.getSolution().getRepository().getActiveRootObject(moduleMetaData.getMetaData().getRootObjectId());
							extendedForm = module.getForm(extended_form_id);
							if (extendedForm != null)
							{
								break;
							}
						}
					}

					if (extendedForm == null)
					{
						break;//in case referring to no longer existing form
					}
					style_name = extendedForm.getStyleName();
					extended_form_id = extendedForm.getExtendsFormID();
					if (visited.contains(new Integer(extended_form_id)))
					{
						break;//in case of cycle in form inheritance hierarchy
					}
				}

				if (style_name == null) return null;

				Style s = (Style)f.getSolution().getRepository().getActiveRootObject(style_name, IRepository.STYLES);//preload the style at the server
				return s;
			}
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return null;
	}

	public synchronized void setBeanDesignInstance(Bean b, Object arg)
	{
		if (beanDesignInstances == null) beanDesignInstances = new HashMap<Bean, Object>();

		beanDesignInstances.put(b, arg);
	}

	public Object getBeanDesignInstance(Bean b)
	{
		return beanDesignInstances == null ? null : beanDesignInstances.get(b);
	}

	public Iterator<ScriptMethod> getScriptMethods(boolean sort)
	{
		return Solution.getScriptMethods(getAllObjectsAsList(), sort);
	}

	public ScriptMethod getScriptMethod(int methodId)
	{
		return Solution.getScriptMethod(getScriptMethods(false), methodId);
	}

	public ScriptMethod getScriptMethod(String methodName)
	{
		if (scriptMethodCacheByName == null)
		{
			scriptMethodCacheByName = new HashMap<String, ScriptMethod>();
			Iterator<ScriptMethod> scriptMethods = getScriptMethods(false);
			while (scriptMethods.hasNext())
			{
				ScriptMethod scriptMethod = scriptMethods.next();
				scriptMethodCacheByName.put(scriptMethod.getName(), scriptMethod);
			}

		}
		return scriptMethodCacheByName.get(methodName);
	}

	public Iterator<ScriptVariable> getScriptVariables(boolean sort)
	{
		return Solution.getScriptVariables(getAllObjectsAsList(), sort);
	}

	public ScriptVariable getScriptVariable(String variableName)
	{
		if (scriptVariableCacheByName == null)
		{
			scriptVariableCacheByName = new HashMap<String, ScriptVariable>();
			Iterator<ScriptVariable> scriptVariable = getScriptVariables(false);
			while (scriptVariable.hasNext())
			{
				ScriptVariable scriptMethod = scriptVariable.next();
				scriptVariableCacheByName.put(scriptMethod.getName(), scriptMethod);
			}

		}
		return scriptVariableCacheByName.get(variableName);
	}

	protected IRepository getRepository()
	{
		if (mainSolution != null)
		{
			return mainSolution.getRepository();
		}
		if (loginFlattenedSolution != null)
		{
			return loginFlattenedSolution.getRepository();
		}

		return null;
	}

	public Iterator<TableNode> getTableNodes(ITable table) throws RepositoryException
	{
		return Solution.getTableNodes(getRepository(), getAllObjectsAsList(), table);
	}

	public Iterator<Relation> getRelations(boolean sort) throws RepositoryException
	{
		return Solution.getRelations(getAllObjectsAsList(), sort);
	}

	public Iterator<Relation> getRelations(ITable filterOnTable, boolean isPrimaryTable, boolean sort) throws RepositoryException
	{
		return Solution.getRelations(getRepository(), getAllObjectsAsList(), filterOnTable, isPrimaryTable, sort);
	}

	public Iterator<ValueList> getValueLists(boolean sort)
	{
		return Solution.getValueLists(getAllObjectsAsList(), sort);
	}

	public ValueList getValueList(int id)
	{
		return AbstractBase.selectById(getValueLists(false), id);
	}

	public synchronized ValueList getValueList(String name)
	{
		if (valuelistCacheByName == null)
		{
			valuelistCacheByName = new HashMap<String, ValueList>();

			Iterator<ValueList> valuelists = getValueLists(false);
			while (valuelists.hasNext())
			{
				ValueList valuelist = valuelists.next();
				valuelistCacheByName.put(valuelist.getName(), valuelist);
			}
		}
		return valuelistCacheByName.get(name);
	}

	public Iterator<Form> getForms(ITable basedOnTable, boolean sort)
	{
		return Solution.getForms(getAllObjectsAsList(),
			basedOnTable == null ? null : DataSourceUtils.createDBTableDataSource(basedOnTable.getServerName(), basedOnTable.getName()), sort);
	}

	public Iterator<Form> getForms(String datasource, boolean sort)
	{
		return Solution.getForms(getAllObjectsAsList(), datasource, sort);
	}

	public Iterator<Form> getForms(boolean sort)
	{
		return Solution.getForms(getAllObjectsAsList(), null, sort);
	}

	public Form getForm(int id)
	{
		return AbstractBase.selectById(getForms(false), id);
	}

	public Form getForm(String name)
	{
		if (formCacheByName == null)
		{
			formCacheByName = new HashMap<String, Form>();

			Iterator<Form> forms = getForms(false);
			while (forms.hasNext())
			{
				Form form = forms.next();
				formCacheByName.put(form.getName(), form);
			}
		}
		return formCacheByName.get(name);
	}

	/**
	 * @param form
	 * @return form hierarchy list [ form, form.super, form.super.super, ... ]
	 */
	public List<Form> getFormHierarchy(Form form)
	{
		List<Form> formHierarchy = new ArrayList<Form>();
		formHierarchy.add(form);
		Form f = form;
		while (f != null && f.getExtendsFormID() != 0)
		{
			f = getForm(f.getExtendsFormID());
			if (f == null || formHierarchy.contains(f) /* prevent cycles */)
			{
				break;
			}
			formHierarchy.add(f);
		}
		return formHierarchy;
	}

	public List<Form> getDirectlyInheritingForms(Form parentForm)
	{
		List<Form> inheritingForms = new ArrayList<Form>();

		Iterator<Form> sameTableFormsIte = getForms(false);
		while (sameTableFormsIte.hasNext())
		{
			Form sameTableForm = sameTableFormsIte.next();
			if (sameTableForm.getExtendsFormID() == parentForm.getID())
			{
				inheritingForms.add(sameTableForm);
			}
		}

		return inheritingForms;
	}

	public Iterator<AggregateVariable> getAggregateVariables(ITable basedOnTable, boolean sort) throws RepositoryException
	{
		return Solution.getAggregateVariables(getTableNodes(basedOnTable), sort);
	}

	public ScriptCalculation getScriptCalculation(String name, ITable basedOnTable) throws RepositoryException
	{
		ScriptCalculation calc = null;
		Iterator<ScriptCalculation> it = getScriptCalculations(basedOnTable, false);
		while (it.hasNext())
		{
			ScriptCalculation tempCalc = it.next();
			if (tempCalc.getName() != null && tempCalc.getName().equals(name))
			{
				calc = tempCalc;
				break;
			}
		}
		return calc;
	}

	public Iterator<ScriptCalculation> getScriptCalculations(ITable basedOnTable, boolean sort) throws RepositoryException
	{
		return Solution.getScriptCalculations(getTableNodes(basedOnTable), sort);
	}

	public Iterator<Media> getMedias(boolean sort)
	{
		return Solution.getMedias(getAllObjectsAsList(), sort);
	}

	public Media getMedia(int id)
	{
		return AbstractBase.selectById(getMedias(false), id);
	}

	public Media getMedia(String name)
	{
		return AbstractBase.selectByName(getMedias(false), name);
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@SuppressWarnings("nls")
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("FlattenedSolution of soluton: ");
		if (mainSolution == null)
		{
			sb.append("<NONE>");
		}
		else
		{
			sb.append(mainSolution.getName());
			sb.append(" having modules: ").append(Arrays.toString(modules));
		}
		if (loginFlattenedSolution != null)
		{
			sb.append(" Login solution: ").append(loginFlattenedSolution.toString());
		}
		return sb.toString();
	}

	/**
	 * @param form
	 * @param namedInstance
	 */
	public void deregisterLiveForm(Form form, String namedInstance)
	{
		synchronized (liveForms)
		{
			if (form instanceof FlattenedForm)
			{
				for (Form f : ((FlattenedForm)form).getAllForms())
				{
					deregisterLiveForm(f, namedInstance);
				}
			}
			else
			{
				Set<String> instances = liveForms.get(form.getName());
				if (instances != null)
				{
					instances.remove(namedInstance);
					if (instances.size() == 0)
					{
						liveForms.remove(form.getName());
					}
				}
			}

			// Test forms if they now are ok. (form they build on when they where changed is now destroyed)
			Iterator<String> it = changedForms.keySet().iterator();
			while (it.hasNext())
			{
				String fName = it.next();
				if (!liveForms.containsKey(fName)) it.remove();
			}
		}

	}

	/**
	 * @param form
	 */
	public void registerLiveForm(Form form, String namedInstance)
	{
		synchronized (liveForms)
		{
			if (form instanceof FlattenedForm)
			{
				for (Form f : ((FlattenedForm)form).getAllForms())
				{
					registerLiveForm(f, namedInstance);
				}
			}
			else
			{
				Set<String> instances = liveForms.get(form.getName());
				if (instances == null)
				{
					instances = new HashSet<String>();
					liveForms.put(form.getName(), instances);
				}
				instances.add(namedInstance);
			}
		}
	}

	/**
	 * @param form
	 * @param application 
	 */
	public void registerChangedForm(Form form)
	{
		synchronized (liveForms)
		{
			if (liveForms.containsKey(form.getName()) && !isInDesign(form) && !changedForms.containsKey(form.getName()))
			{
				// if liveForms does contain the form then remember it.
				Set<Object> infos = new HashSet<Object>();
				changedForms.put(form.getName(), infos);
				if (debugListener != null)
				{
					debugListener.addDebugInfo(infos);
				}
			}
		}
	}

	public void registerDebugListener(IFlattenedSolutionDebugListener listener)
	{
		this.debugListener = listener;
	}

	public IFlattenedSolutionDebugListener getDebugListener()
	{
		return debugListener;
	}

	/**
	 * 
	 */
	public void checkStateForms(IServiceProvider provider)
	{
		synchronized (liveForms)
		{
			if (changedForms.size() > 0)
			{
				StringBuilder sb = new StringBuilder();
				sb.append("The form(s) that are stale (can also be a parent form if form inheritance is used) are: "); //$NON-NLS-1$
				for (String fName : changedForms.keySet())
				{
					Set<String> set = liveForms.get(fName);
					sb.append(fName);
					sb.append("["); //$NON-NLS-1$
					for (String instanceName : set)
					{
						sb.append(instanceName);
						sb.append(',');
					}
					sb.setLength(sb.length() - 1);
					sb.append("]"); //$NON-NLS-1$
					Set<Object> infos = changedForms.get(fName);
					if (infos != null && infos.size() == 2)
					{
						sb.append("("); //$NON-NLS-1$
						sb.append(infos.toArray()[0] instanceof Integer ? infos.toArray()[1] : infos.toArray()[0]);
						sb.append(":"); //$NON-NLS-1$
						sb.append(infos.toArray()[0] instanceof Integer ? infos.toArray()[0] : infos.toArray()[1]);
						sb.append(")"); //$NON-NLS-1$
					}
					sb.append(","); //$NON-NLS-1$
				}
				sb.setLength(sb.length() - 1);
				changedForms.clear();
				provider.reportError("Stale form(s) detected, form(s) were altered by the solution model without destroying them first", sb.toString()); //$NON-NLS-1$
			}
		}

	}

	private String designFormName = null;

	/**
	 * Called by the Template generator to generate a record view html when the form is in design and in TABLEVIEW
	 * @param f
	 * @return
	 */
	public boolean isInDesign(Form f)
	{
		if (f == null) return designFormName != null;

		return f.getName().equals(designFormName);
	}

	/**
	 * @param form
	 */
	public void setInDesign(Form form)
	{
		if (form == null)
		{
			designFormName = null;
		}
		else if (designFormName != null && !form.getName().equals(designFormName))
		{
			throw new IllegalStateException("cant have 2 forms in design mode"); //$NON-NLS-1$
		}
		else
		{
			designFormName = form.getName();
		}
	}
}
