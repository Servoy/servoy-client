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

import static com.servoy.base.query.IQueryConstants.INNER_JOIN;
import static com.servoy.base.query.IQueryConstants.LEFT_OUTER_JOIN;
import static com.servoy.base.util.DataSourceUtilsBase.getDBServernameTablename;
import static com.servoy.base.util.DataSourceUtilsBase.isCompleteDBbServerTable;
import static com.servoy.j2db.util.DataSourceUtils.createDBTableDataSource;
import static com.servoy.j2db.util.DataSourceUtils.getInmemDataSourceName;
import static com.servoy.j2db.util.DataSourceUtils.isSameServer;

import java.io.PrintWriter;
import java.io.StringWriter;
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
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.json.JSONObject;

import com.servoy.base.persistence.constants.IValueListConstants;
import com.servoy.base.query.IBaseSQLCondition;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.dataprocessing.DBValueList;
import com.servoy.j2db.dataprocessing.IFoundSetManagerInternal;
import com.servoy.j2db.dataprocessing.IGlobalValueEntry;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.AbstractPersistFactory;
import com.servoy.j2db.persistence.AbstractRepository;
import com.servoy.j2db.persistence.AggregateVariable;
import com.servoy.j2db.persistence.Bean;
import com.servoy.j2db.persistence.ChangeHandler;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.ColumnWrapper;
import com.servoy.j2db.persistence.ContentSpec.Element;
import com.servoy.j2db.persistence.EnumDataProvider;
import com.servoy.j2db.persistence.FlattenedForm;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IActiveSolutionHandler;
import com.servoy.j2db.persistence.ICloneable;
import com.servoy.j2db.persistence.IColumn;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IDataProviderHandler;
import com.servoy.j2db.persistence.IDataProviderLookup;
import com.servoy.j2db.persistence.IItemChangeListener;
import com.servoy.j2db.persistence.IMediaProvider;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistVisitor;
import com.servoy.j2db.persistence.IRelationProvider;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.IScriptElement;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.ISupportScope;
import com.servoy.j2db.persistence.ISupportScriptProviders;
import com.servoy.j2db.persistence.ISupportUpdateableName;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.Menu;
import com.servoy.j2db.persistence.NameComparator;
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
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.Style;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.persistence.TableNode;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.query.AndCondition;
import com.servoy.j2db.query.CompareCondition;
import com.servoy.j2db.query.ISQLTableJoin;
import com.servoy.j2db.query.ObjectPlaceholderKey;
import com.servoy.j2db.query.Placeholder;
import com.servoy.j2db.query.QueryColumn;
import com.servoy.j2db.query.QueryCompositeJoin;
import com.servoy.j2db.query.QueryJoin;
import com.servoy.j2db.query.QueryTable;
import com.servoy.j2db.scripting.info.EventType;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.IteratorChain;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.ScopesUtils;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.keyword.Ident;

/**
 * Runtime object which represents the entire solution, flattened, containing modules, caching stuff
 *
 * @author jcompagner,jblok
 */
public class FlattenedSolution implements IItemChangeListener<IPersist>, IDataProviderHandler, IRelationProvider, ISupportScriptProviders, IMediaProvider
{
	private static RuntimeProperty<AbstractBase> CLONE_PROPERTY = new RuntimeProperty<AbstractBase>()
	{
	};

	private volatile SolutionMetaData mainSolutionMetaData;
	private volatile Solution mainSolution;
	private volatile Solution[] modules;
	private volatile FlattenedSolution loginFlattenedSolution;
	private volatile FlattenedSolution parentFlattenedSolution;

	private volatile Solution copySolution = null;

	private volatile Set<Object> overridenSecurityIds;
	private volatile Pair<ConcurrentMap<Object, Integer>, Set<Object>> securityAccess;

	private volatile ConcurrentMap<ITable, Map<String, IDataProvider>> allProvidersForTable = null; //table -> Map(dpname,dp) ,runtime var
	private final ConcurrentMap<String, IDataProvider> globalProviders = new ConcurrentHashMap<String, IDataProvider>(64, 9f, 16); //global -> dp ,runtime var

	private final List<String> deletedStyles = Collections.synchronizedList(new ArrayList<String>(3));

	// 2 synchronized on liveForms object.
	private final Map<String, Set<String>> liveForms = new HashMap<String, Set<String>>();
	private final Map<String, ChangedFormData> changedForms = new HashMap<String, ChangedFormData>();

	private volatile ConcurrentHashMap<String, Style> all_styles; // concurrent modification exceptions can happen; see comments from Solution->PRE_LOADED_STYLES.
	private volatile ConcurrentHashMap<String, Style> user_created_styles; // concurrent modification exceptions shouldn't happen with current implementation for this map; no need to sync

	private volatile SimplePersistFactory persistFactory;

	private final ConcurrentMap<Form, FlattenedForm[]> flattenedFormCache;
	private volatile ConcurrentMap<Bean, Object> beanDesignInstances;

	private volatile ISolutionModelPersistIndex index;

	private VariantsHandler variantsHandler;

	private EncryptionHandler encryptionHandler;
	private Map<String, EventType> eventTypes;

	/**
	 * @param cacheFlattenedForms turn flattened form caching on when flushFlattenedFormCache() will also be called.
	 */
	public FlattenedSolution(boolean cacheFlattenedForms)
	{
		flattenedFormCache = cacheFlattenedForms ? new ConcurrentHashMap<Form, FlattenedForm[]>() : null;
	}

	public FlattenedSolution()
	{
		this(true);
	}

	/**
	 * @param solution
	 * @param activeSolutionHandler
	 * @throws RemoteException
	 * @throws RepositoryException
	 */
	public FlattenedSolution(SolutionMetaData solutionMetaData, IActiveSolutionHandler activeSolutionHandler) throws RepositoryException, RemoteException
	{
		this(true);
		setSolution(solutionMetaData, true, true, activeSolutionHandler);
	}

	@SuppressWarnings("unchecked")
	public <T extends AbstractBase> T createPersistCopy(T persist)
	{
		if (mainSolution == null && loginFlattenedSolution != null)
		{
			return loginFlattenedSolution.createPersistCopy(persist);
		}

		// if a copy is taken or created then it is for modifying it.
		// so update the last modified time.
		getSolutionCopy().updateLastModifiedTime();

		T copy = (T)getSolutionCopy().getChild(persist.getUUID());
		if (copy != null) return copy;

		if (persist.getRuntimeProperty(CLONE_PROPERTY) != null)
		{
			throw new RuntimeException("Persist " + persist + " already a clone"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (persist instanceof Form && ((Form)persist).isFormComponent().booleanValue())
		{
			throw new RuntimeException("Form " + persist + " can not be altered by solution model because it is a reference form"); //$NON-NLS-1$ //$NON-NLS-2$
		}

		copy = (T)persist.clonePersist(copySolution);
		copy.setRuntimeProperty(CLONE_PROPERTY, persist);
		flush(persist);
		if (persist instanceof Form)
		{
			refreshSuperForms(persist);
		}
		return copy;
	}

	@SuppressWarnings({ "unchecked", "nls" })
	public <T extends AbstractBase> T clonePersist(T persist, String newName, AbstractBase newParent)
	{
		T clone = (T)persist.clonePersist(persist.getParent() == newParent ? null : newParent);
		final Map<Object, Object> updatedElementIds = AbstractPersistFactory.resetUUIDSRecursively(clone, getPersistFactory(), false);
		if (persist.getParent() == newParent)
		{
			newParent.addChild(clone);
		}
		// make sure that this persist is not seen as a copy a real persist/form
		clone.setRuntimeProperty(CLONE_PROPERTY, null);
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
				throw new RuntimeException(
					"name '" + newName + "' invalid for the clone of " + ((ISupportName)persist).getName() + ", error: " + e.getMessage());
			}
		}
		if (clone instanceof ISupportChilds)
		{
			clone.acceptVisitor(new IPersistVisitor()
			{
				public Object visit(IPersist o)
				{
					if (o instanceof AbstractBase)
					{
						Map<String, Object> propertiesMap = ((AbstractBase)o).getPropertiesMap();
						for (Map.Entry<String, Object> entry : propertiesMap.entrySet())
						{
							Object elementUUID = updatedElementIds.get(entry.getValue());
							if (elementUUID instanceof String)
							{
								Element element = StaticContentSpecLoader.getContentSpec().getPropertyForObjectTypeByName(o.getTypeID(), entry.getKey());
								if (element.getTypeID() == IRepository.ELEMENTS) ((AbstractBase)o).setProperty(entry.getKey(), elementUUID);
							}
							else if (entry.getValue() instanceof JSONObject)
							{
								updateElementReferences((JSONObject)entry.getValue(), updatedElementIds);
							}

						}
					}
					return null;
				}
			});
		}
		if (securityAccess != null)
		{
			ConcurrentMap<Object, Integer> securityValues = securityAccess.getLeft();
			for (Object elementUUID : new HashSet(securityValues.keySet()))
			{
				if (updatedElementIds.containsKey(elementUUID.toString()))
				{
					UUID uuid = Utils.getAsUUID(updatedElementIds.get(elementUUID.toString()), false);
					if (uuid != null)
					{
						securityValues.put(uuid, securityValues.get(elementUUID));
					}
				}
			}
		}
		flush(persist);
		getIndex().reload();
		return clone;
	}

	private void updateElementReferences(JSONObject json, Map<Object, Object> updatedElementIds)
	{
		Iterator<String> it = json.keys();
		while (it.hasNext())
		{
			String key = it.next();
			Object elementId = updatedElementIds.get(json.get(key));
			if (elementId != null)
			{
				json.put(key, elementId);
			}
			else if (json.get(key) instanceof JSONObject)
			{
				updateElementReferences((JSONObject)json.get(key), updatedElementIds);
			}
		}
	}

	public void deletePersistCopy(AbstractBase persist, boolean revertToOriginal)
	{
		if (mainSolution == null && loginFlattenedSolution != null)
		{
			loginFlattenedSolution.deletePersistCopy(persist, revertToOriginal);
			return;
		}

		if (copySolution != null)
		{
			copySolution.removeChild(persist);
		}
		flush(persist);

		if (persist instanceof Form)
		{
			refreshSuperForms(persist);
		}

		// if it has a clone persist property then it was cloned (and there is an original)
		// or revert was called on an original object then just ignore it.
		AbstractBase realPersist = persist.getRuntimeProperty(CLONE_PROPERTY);
		boolean exists = realPersist != null || getAllObjectsAsList().indexOf(persist) != -1;
		if (exists && !revertToOriginal)
		{
			if (realPersist != null)
			{
				getIndex().addRemoved(realPersist);
				flush(realPersist);
			}
			else
			{
				getIndex().addRemoved(persist);
			}
			flush(persist);
		}
	}

	protected ISolutionModelPersistIndex getIndex()
	{
		if (index == null && mainSolution != null)
		{
			// just sync on a private field (not the this! so there are no deadlocks)
			synchronized (CLONE_PROPERTY)
			{
				if (index == null)
				{
					index = createPersistIndex();
				}
			}

			flushExtendsStuff();

		}
		return index != null ? index : loginFlattenedSolution != null ? loginFlattenedSolution.getIndex() : new EmptyPersistIndex();
	}

	/**
	 *
	 */
	protected void flushExtendsStuff()
	{
		// implemented by the real client FS and the DeveloperFS
	}

	public void addToRemovedPersists(AbstractBase persist)
	{
		AbstractBase realPersist = persist.getRuntimeProperty(CLONE_PROPERTY);
		if (realPersist != null)
		{
			getIndex().addRemoved(realPersist);
		}
		else
		{
			getIndex().addRemoved(persist);
		}
	}

	public boolean hasCopy(IPersist persist)
	{
		if (mainSolution == null && loginFlattenedSolution != null)
		{
			return loginFlattenedSolution.hasCopy(persist);
		}

		return copySolution != null && copySolution.getChild(persist.getUUID()) != null;
	}

	public void updatePersistInSolutionCopy(final IPersist persist)
	{
		if (mainSolution == null && loginFlattenedSolution != null)
		{
			loginFlattenedSolution.updatePersistInSolutionCopy(persist);
			return;
		}

		if (copySolution == null) return;

		IPersist copyPersist = (IPersist)copySolution.acceptVisitor(new IPersistVisitor()
		{
			public Object visit(IPersist o)
			{
				if (o.getUUID().equals(persist.getUUID()))
				{
					return o;
				}
				return IPersistVisitor.CONTINUE_TRAVERSAL;
			}
		});

		if (copyPersist != null)
		{
			((AbstractBase)copyPersist).copyPropertiesMap(((AbstractBase)persist).getPropertiesMap(), false);
			ISupportChilds parent = copyPersist.getParent();
			flush(persist);
			if (parent instanceof Form)
			{
				((Form)parent).setLastModified(System.currentTimeMillis());
			}
			else if (copyPersist instanceof Form)
			{
				((Form)copyPersist).setLastModified(System.currentTimeMillis());
			}
		}
		else if (persist.getParent() != null && !(persist.getParent() instanceof Solution))
		{
			ISupportChilds copyParent = (ISupportChilds)copySolution.acceptVisitor(new IPersistVisitor()
			{
				public Object visit(IPersist o)
				{
					if (o.getUUID().equals(persist.getParent().getUUID()))
					{
						return o;
					}
					return IPersistVisitor.CONTINUE_TRAVERSAL;
				}
			});

			if (copyParent != null)
			{
				if (persist instanceof ICloneable)
				{
					((ICloneable)persist).clonePersist((AbstractBase)copyParent);
				}
				else
				{
					copyParent.addChild(persist);
				}
				flush(persist);
				if (copyParent instanceof Form)
				{
					((Form)copyParent).setLastModified(System.currentTimeMillis());
				}

			}
		}
	}

	public Solution getSolutionCopy()
	{
		return getSolutionCopy(true);
	}

	public Solution getSolutionCopy(boolean create)
	{
		if (mainSolution == null)
		{
			if (loginFlattenedSolution != null)
			{
				return loginFlattenedSolution.getSolutionCopy(create);
			}
			return null;
		}

		if (copySolution != null || !create) return copySolution;

		try
		{
			SimplePersistFactory factory = getPersistFactory();

			copySolution = SimplePersistFactory.createDummyCopy(mainSolution);
			copySolution.setChangeHandler(new ChangeHandler(factory)
			{
				// overwrite this new Object to skip the call to rootObject.registerNewObject(object); which shouldn't be needed for solution model solutions.
				@Override
				public IPersist createNewObject(ISupportChilds parent, int object_type_id, UUID uuid) throws RepositoryException
				{
					return factory.createObject(parent, object_type_id, uuid);
				}
			});
			copySolution.getChangeHandler().addIPersistListener(this);
			getIndex().setSolutionModelSolution(copySolution);
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return copySolution;
	}

	public TableNode getSolutionCopyTableNode(String dataSource) throws RepositoryException
	{
		return getSolutionCopy().getOrCreateTableNode(dataSource);
	}


	public SimplePersistFactory getPersistFactory()
	{
		if (persistFactory == null)
		{
			persistFactory = new SimplePersistFactory();
		}
		return persistFactory;
	}


	/**
	 * @return
	 */
	public VariantsHandler getVariantsHandler()
	{
		if (variantsHandler == null)
		{
			variantsHandler = new VariantsHandler(this);
		}
		return variantsHandler;
	}

	public synchronized EncryptionHandler getEncryptionHandler()
	{
		if (encryptionHandler == null)
		{
			encryptionHandler = new EncryptionHandler();
		}
		return encryptionHandler;
	}


	public Style createStyleCopy(Style style)
	{
		if (mainSolution == null && loginFlattenedSolution == null) return null;

		if (user_created_styles == null) user_created_styles = new ConcurrentHashMap<String, Style>();
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
		if (name == null) return null;
		if (mainSolution == null)
		{
			if (loginFlattenedSolution == null)
			{
				return null;
			}
			return loginFlattenedSolution.createStyle(name, content);
		}

		if (getAllStyles().containsKey(name) && !deletedStyles.contains(name)) throw new RuntimeException("Style with name '" + name + "' already exists"); //$NON-NLS-1$ //$NON-NLS-2$
		if (user_created_styles == null) user_created_styles = new ConcurrentHashMap<String, Style>();
		if (user_created_styles.containsKey(name)) throw new RuntimeException("Style with name '" + name + "' already exists"); //$NON-NLS-1$ //$NON-NLS-2$

		RootObjectMetaData rootObjectMetaData = new RootObjectMetaData(UUID.randomUUID(), name, IRepository.STYLES, 1, 1);
		rootObjectMetaData.setName(name);
		Style style = new Style(mainSolution.getRepository(), rootObjectMetaData);
		style.setContent(content);
		user_created_styles.put(name, style);
		deletedStyles.remove(name);
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

	public long getLastModifiedTime()
	{
		if (copySolution != null) return copySolution.getLastModifiedTime();
		return getSolution().getLastModifiedTime();
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

	private boolean isLoadingSolution = false;

	public void setSolution(SolutionMetaData sol, boolean loadLoginSolution, boolean loadMainSolution, IActiveSolutionHandler activeSolutionHandler)
		throws RepositoryException, RemoteException
	{
		if (sol == null) throw new IllegalArgumentException("use close method!"); //$NON-NLS-1$

		isLoadingSolution = true;
		try
		{
			copySolution = null;
			persistFactory = null;
			mainSolution = null;
			user_created_styles = null;
			all_styles = null;
			if (index != null)
			{
				index.destroy();
				index = null;
			}
			mainSolutionMetaData = sol;

			if (loadLoginSolution)
			{
				// get login solution
				Solution[] loginSolutionAndModules = activeSolutionHandler.loadLoginSolutionAndModules(mainSolutionMetaData);
				if (loginSolutionAndModules != null && loginSolutionAndModules.length > 0)
				{
					if (loginFlattenedSolution == null)
					{
						loginFlattenedSolution = new ExtendsConfiguratingFlattenedSolution();
					}
					loginFlattenedSolution.setSolutionAndModules(loginSolutionAndModules[0].getName(), loginSolutionAndModules);
					loginFlattenedSolution.setParentSolution(this);
				}
			}

			// get regular solution if available
			if (loadMainSolution && activeSolutionHandler.haveRepositoryAccess()) // local or already access to repository
			{
				// Note: referencedModules includes main solution
				List<RootObjectReference> referencedModules = activeSolutionHandler.getRepository().getActiveSolutionModuleMetaDatas(
					mainSolutionMetaData.getRootObjectUuid());
				if (referencedModules != null && referencedModules.size() != 0)
				{
					List<RootObjectMetaData> solutionAndModuleMetaDatas = new ArrayList<RootObjectMetaData>();
					for (RootObjectReference moduleReference : referencedModules)
					{
						RootObjectMetaData moduleMetaData = moduleReference.getMetaData();
						if (moduleMetaData == null)
						{
							throw new RepositoryException(Messages.getString("servoy.client.module.notfound", new Object[] { moduleReference.toString() })); //$NON-NLS-1$
						}
						solutionAndModuleMetaDatas.add(moduleMetaData);
					}

					Solution[] mods = activeSolutionHandler.loadActiveSolutions(
						solutionAndModuleMetaDatas.toArray(new RootObjectMetaData[solutionAndModuleMetaDatas.size()]));
					setSolutionAndModules(mainSolutionMetaData.getName(), mods);
				}
			}
		}
		finally
		{
			isLoadingSolution = false;
		}
	}

	public boolean isLoadingSolution()
	{
		return isLoadingSolution;
	}

	protected void setSolutionAndModules(String mainSolutionName, Solution[] mods) throws RemoteException
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
		}

		modules = getDependencyGraphOrderedModules(modulesMap.values(), mainSolution);

		flushAllCachedData();

		Map<String, Style> allStyles = getAllStyles();

		for (Solution s : modules)
		{
			ConcurrentHashMap<String, Style> modStyles = s.getSerializableRuntimeProperty(Solution.PRE_LOADED_STYLES);
			if (modStyles != null)
			{
				allStyles.putAll(modStyles);
			}
			if (s.getChangeHandler() != null)
			{
				s.getChangeHandler().addIPersistListener(this);
			}
		}
		// everything loaded, let the index be created.
		if (index != null)
		{
			index.destroy();
			index = null;
		}
		buildEventTypesList();
	}

	/**
	 * @return
	 */
	protected ISolutionModelPersistIndex createPersistIndex()
	{
		return new SolutionModelPersistIndex(PersistIndexCache.getPersistIndex(getSolution(), getModules()));
	}

	/**
	 *
	 */
	private Map<String, Style> getAllStyles()
	{
		if (all_styles == null & mainSolution != null)
		{
			all_styles = mainSolution.getSerializableRuntimeProperty(Solution.PRE_LOADED_STYLES);
			if (all_styles == null)
			{
				// this should normally not happen, because when solutions are loaded this property is set (by the repository)
				all_styles = new ConcurrentHashMap<String, Style>();
				mainSolution.setSerializableRuntimeProperty(Solution.PRE_LOADED_STYLES, all_styles);
			}
		}
		return all_styles;
	}

	private static Solution[] getDependencyGraphOrderedModules(Collection<Solution> modules, Solution solution)
	{
		List<Solution> orderedSolutions = buildOrderedList(modules, new ArrayList<Solution>(), solution);
		if (orderedSolutions.size() > 1)
		{
			Collections.reverse(orderedSolutions);
		}
		return orderedSolutions.toArray(new Solution[orderedSolutions.size()]);
	}

	private static List<Solution> buildOrderedList(Collection<Solution> modules, List<Solution> orderedSolutions, Solution solution)
	{
		if (solution != null && solution.getModulesNames() != null)
		{
			for (String moduleName : Utils.getTokenElements(solution.getModulesNames(), ",", true)) //$NON-NLS-1$
			{
				Solution module = null;
				for (Solution sol : modules)
				{
					if (sol.getName().equals(moduleName))
					{
						module = sol;
						break;
					}
				}
				if (module != null && !orderedSolutions.contains(module))
				{
					orderedSolutions.add(module);
					buildOrderedList(modules, orderedSolutions, module);
				}
			}
		}
		return orderedSolutions;
	}

	public void clearLoginSolution(IActiveSolutionHandler handler, IServiceProvider app)
	{
		if (loginFlattenedSolution != null)
		{
			if (app instanceof IApplication)
			{
				// destroy all forms of the login solution before closing it
				List<IFormController> cachedFormControllers = ((IApplication)app).getFormManager().getCachedFormControllers();
				for (IFormController fc : cachedFormControllers)
				{
					fc.notifyVisible(false, new ArrayList<>(), true);
					fc.destroy();
				}
			}
			loginFlattenedSolution.close(handler);
			loginFlattenedSolution = null;
			flushAllCachedData();
		}
	}

	public SolutionMetaData getMainSolutionMetaData()
	{
		return mainSolutionMetaData;
	}

	private void combineServerProxies(ConcurrentMap<String, IServer> orgs, ConcurrentMap<String, IServer> additional) throws RemoteException
	{
		for (IServer addObject : additional.values())
		{
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

	/*
	 * Get a flattened form from this flattened solution.
	 *
	 * <p>When the form does not have a parent, the form itself is returned
	 */
	public Form getFlattenedForm(IPersist persist)
	{
		return getFlattenedForm(persist, true);
	}

	public Form getFlattenedForm(IPersist persist, boolean useCached)
	{
		if (mainSolution == null && loginFlattenedSolution != null)
		{
			return loginFlattenedSolution.getFlattenedForm(persist, useCached);
		}

		if (persist == null || getSolution() == null)
		{
			return null;
		}
		Form form = null;
		if (persist instanceof FlattenedForm)
		{
			form = ((FlattenedForm)persist).getForm();
		}
		else
		{
			form = (Form)persist.getAncestor(IRepository.FORMS);
		}
		if (form == null || form.getExtendsID() == null)
		{
			// no form or nothing to flatten
			return form;
		}

		// make sure is latest form
		form = getForm(form.getUUID().toString());

		if (form == null) return null; // form is already deleted?

		if (flattenedFormCache == null)
		{
			// no caching
			return createFlattenedForm(form);
		}

		// cache flattened form
		FlattenedForm[] flattedFormRef;
		synchronized (flattenedFormCache)
		{
			flattedFormRef = flattenedFormCache.get(form);
			if (flattedFormRef == null)
			{
				flattenedFormCache.put(form, flattedFormRef = new FlattenedForm[] { null });
			}
		}
		synchronized (flattedFormRef)
		{
			if (flattedFormRef[0] == null || !useCached)
			{
				flattedFormRef[0] = createFlattenedForm(form);
			}
			return flattedFormRef[0];
		}
	}

	public void flushFlattendFormCache(Form frm)
	{
		flushFlattendFormCache(frm, false);
	}

	public void flushFlattendFormCache(Form frm, boolean includingSubforms)
	{
		if (flattenedFormCache == null || frm == null) return;
		Form form = frm;
		if (frm instanceof FlattenedForm)
		{
			form = ((FlattenedForm)frm).getForm();
		}
		flattenedFormCache.remove(form);

		if (includingSubforms)
		{
			Iterator<Form> formCacheIte = flattenedFormCache.keySet().iterator();
			while (formCacheIte.hasNext())
			{
				if (FlattenedForm.hasFormInHierarchy(formCacheIte.next(), frm))
				{
					formCacheIte.remove();
				}
			}
		}
	}

	/*
	 * Create a flattened form for the form that is child of the current solution.
	 */
	public FlattenedForm createFlattenedForm(Form form)
	{
		Form myForm;
		Solution solcopy = getSolutionCopy(false);
		if (form.getParent() == getSolution() || form.getParent() == solcopy)
		{
			myForm = form;
		}
		else
		{
			myForm = null;
			if (solcopy != null)
			{
				myForm = (Form)solcopy.getChild(form.getUUID());
			}
			if (myForm == null)
			{
				myForm = (Form)getSolution().getChild(form.getUUID());
			}
			if (myForm == null && modules != null)
			{
				for (Solution mod : modules)
				{
					myForm = (Form)mod.getChild(form.getUUID());
					if (myForm != null) break;
				}
			}
			if (myForm == null)
			{
				throw new RuntimeException("Cannot find form '" + form + "' in solution '" + getSolution() + "'");
			}
		}
		try
		{
			return new FlattenedForm(this, myForm);
		}
		catch (RuntimeException ex)
		{
			Debug.error("Error while creating full hierarchy form (flattened form) for: " + (myForm != null ? myForm.getName() : "<form not found>"), ex);
			throw ex;
		}
	}

	public void flushFlattenedFormCache()
	{
		if (flattenedFormCache != null)
		{
			synchronized (flattenedFormCache)
			{
				flattenedFormCache.clear();
			}
		}
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
			if (form.isResponsiveLayout()) return true;
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
				if (form instanceof FlattenedForm || f.getExtendsID() == null)
				{
					// no (more) hierarchy to investigate
					break;
				}
				f = getForm(f.getExtendsID());
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
			getAllModuleObjects(retval);
			fillList(retval, mainSolution);
			if (loginFlattenedSolution != null)
			{
				List<IPersist> elements = loginFlattenedSolution.getAllObjectsAsList();
				for (IPersist persist : elements)
				{
					if (!retval.contains(persist)) retval.add(persist);
				}
			}
			allObjectsSize = retval.size();
			retval.removeAll(getIndex().getRemoved());
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
		if (copySolution == null || copySolution == s)
		{
			copyInto.addAll(allObjectsAsList);
		}
		else
		{
			Object[] copy = allObjectsAsList.toArray();
			for (Object o : copy)
			{
				IPersist persist = (IPersist)o;
				if (copySolution.getChild(persist.getUUID()) == null)
				{
					copyInto.add(persist);
				}
			}
		}
	}


	public Collection<String> getScopeNames()
	{
		return getScopesPerSolution().keySet();
	}

	public Collection<Pair<String, IRootObject>> getScopes()
	{
		return getScopesPerSolution().values();
	}

	private Map<String, Pair<String, IRootObject>> getScopesPerSolution()
	{
		if (mainSolution == null && loginFlattenedSolution != null)
		{
			return loginFlattenedSolution.getScopesPerSolution();
		}

		Map<String, Pair<String, IRootObject>> scopes = new HashMap<String, Pair<String, IRootObject>>();

		if (mainSolution != null)
		{
			for (String scopeName : mainSolution.getScopeNames())
			{
				scopes.put(scopeName, new Pair<String, IRootObject>(scopeName, mainSolution));
			}

			if (modules != null)
			{
				for (Solution s : modules)
				{
					if (s != mainSolution)
					{
						for (String scopeName : s.getScopeNames())
						{
							if (!scopes.containsKey(scopeName))
							{
								// TODO: check if same scope exists with different modules
								scopes.put(scopeName, new Pair<String, IRootObject>(scopeName, s));
							}
						}
					}
				}
			}
		}

		addGlobalsScope(scopes);

		return scopes;
	}

	protected void addGlobalsScope(Map<String, Pair<String, IRootObject>> scopes)
	{
		if (!scopes.containsKey(ScriptVariable.GLOBAL_SCOPE))
		{
			// make sure there is always a globals scope
			scopes.put(ScriptVariable.GLOBAL_SCOPE, new Pair<String, IRootObject>(ScriptVariable.GLOBAL_SCOPE, mainSolution));
		}
	}

	public List<Pair<String, IRootObject>> getAllScopes()
	{
		List<Pair<String, IRootObject>> scopes = new ArrayList<Pair<String, IRootObject>>();
		if (mainSolution != null)
		{
			for (String scopeName : mainSolution.getScopeNames())
			{
				scopes.add(new Pair<String, IRootObject>(scopeName, mainSolution));
			}

			if (modules != null)
			{
				for (Solution s : modules)
				{
					if (s != mainSolution)
					{
						for (String scopeName : s.getScopeNames())
						{
							scopes.add(new Pair<String, IRootObject>(scopeName, s));
						}
					}
				}
			}
		}
		return scopes;
	}

	public void addSecurityAccess(Pair<Map<Object, Integer>, Set<Object>> sp)
	{
		addSecurityAccess(sp, true);
	}

	public void overrideSecurityAccess(Map<Object, Integer> sp)
	{
		if (overridenSecurityIds == null)
		{
			overridenSecurityIds = new HashSet<>();
		}
		overridenSecurityIds.addAll(sp.keySet());
		addSecurityAccess(new Pair<Map<Object, Integer>, Set<Object>>(sp, securityAccess != null ? securityAccess.getRight() : new HashSet<>()), false);
	}

	public void addSecurityAccess(Pair<Map<Object, Integer>, Set<Object>> sp, boolean combineIfExisting)
	{
		if (sp == null || sp.getLeft() == null) return;
		// always make sure that the overridenSecurityIds are never any more in implicit mode
		if (overridenSecurityIds != null) sp.getRight().removeAll(overridenSecurityIds);
		if (securityAccess == null)
		{
			securityAccess = new Pair<ConcurrentMap<Object, Integer>, Set<Object>>(new ConcurrentHashMap<Object, Integer>(sp.getLeft()), sp.getRight());
		}
		else
		{
			// first add the implicit list to the current list, this has only effect of combineIfExisting is true (false would mean overriden and then it is already adjusted)
			securityAccess.getRight().addAll(sp.getRight());
			for (Entry<Object, Integer> entry : sp.getLeft().entrySet())
			{
				Object elementUID = entry.getKey();
				int newValue = entry.getValue().intValue();
				Integer currentValue = securityAccess.getLeft().get(elementUID);
				if (currentValue != null && combineIfExisting)
				{
					newValue |= currentValue.intValue();
				}
				securityAccess.getLeft().put(elementUID, new Integer(newValue));
			}
		}
	}

	public void clearSecurityAccess()
	{
		if (securityAccess != null)
		{
			securityAccess.getLeft().clear();
			securityAccess = null;
			overridenSecurityIds = null;
		}
	}

	public int getSecurityAccess(Object element_id, int implicitValue)
	{
		if (securityAccess == null) return -1;

		Integer i = securityAccess.getLeft().get(element_id);
		if (i == null)
		{
			//return -1;
			// we now return implicit value here
			return implicitValue;
		}
		else
		{
			// if the value is not the implicit value and an implicit value is asked for and it was registered that it should have
			// an implicit value then return the implicit value
			// so this is in implicit mode where the element was not configured in all the groups..
			if (i.intValue() != implicitValue && (implicitValue == IRepository.IMPLICIT_FORM_ACCESS || implicitValue == IRepository.IMPLICIT_TABLE_ACCESS) &&
				securityAccess.getRight().contains(element_id)) return implicitValue;
			return i.intValue();
		}
	}

	//commit all user data
	public void close(IActiveSolutionHandler handler)
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

		synchronized (liveForms)
		{
			changedForms.clear();
			liveForms.clear();
		}

		designFormName = null;

		copySolution = null;
		persistFactory = null;

		allObjectsSize = 256;

		parentFlattenedSolution = null;

		if (index != null)
		{
			index.destroy();
			index = null;
		}

	}

	private static final IDataProvider NULL = new IDataProvider()
	{
		public boolean isEditable()
		{
			return false;
		}

		public int getLength()
		{
			return 0;
		}

		public int getFlags()
		{
			return 0;
		}

		public int getDataProviderType()
		{
			return 0;
		}

		public String getDataProviderID()
		{
			return null;
		}

		public ColumnWrapper getColumnWrapper()
		{
			return null;
		}
	};

	public IDataProvider getGlobalDataProvider(String id) throws RepositoryException
	{
		return getGlobalDataProvider(id, false);
	}

	/**
	 * @param quiet don't throw exceptions if tables/servers are not found; just do best effort search
	 */
	public IDataProvider getGlobalDataProvider(String id, boolean quiet) throws RepositoryException
	{
		if (id == null) return null;

		IDataProvider retval = globalProviders.get(id);
		if (retval == null)
		{
			retval = getGlobalDataProviderEx(id, quiet);
			if (retval != null)
			{
				globalProviders.put(id, retval);
			}
			else
			{
				globalProviders.put(id, NULL);
			}
		}
		return retval == NULL ? null : retval;
	}

	private IDataProvider getGlobalDataProviderEx(String id, boolean quiet) throws RepositoryException
	{
		if (id == null) return null;

		Pair<String, String> scope = ScopesUtils.getVariableScope(id);
		if (scope.getLeft() != null /* global scope */)
		{
			//search all objects,will return globals
			ScriptVariable global = AbstractBase.selectByName(getScriptVariables(scope.getLeft(), false), scope.getRight());
			if (global != null)
			{
				return global;
			}
			// try @enum global variables
			return getEnumDataProvider(id);
		}

		int indx = id.lastIndexOf('.'); // in case of multi-level relations we have more that 1 dot
		if (indx > 0)
		{
			String rel_name = id.substring(0, indx);
			String col = id.substring(indx + 1);

			Relation[] relations = getRelationSequence(rel_name);
			if (relations == null)
			{
				return null;
			}

			Relation r = relations[relations.length - 1];

			if (quiet)
			{
				boolean missingSrv = true;

				String ds = r.getForeignDataSource();
				if (ds != null)
				{
					String[] st = getDBServernameTablename(ds);
					if (isCompleteDBbServerTable(st))
					{
						try
						{
							missingSrv = (r.getRootObject().getServer(st[0]) == null);
						}
						catch (RemoteException e)
						{
							// we are in developer here - shouldn't happen
						}
					}
				}
				if (missingSrv) return null;
			}

			// TODO if this is refactord out to be resolved outside the relation also look at the DataProviderConverter
			// the call from that class to flattenedSolution.getGlobalDataProvider(value);
			Column[] cols = r.getForeignColumns(this);
			if (cols == null || cols.length == 0) return null;

			IDataProvider c = getDataProviderForTable(getTable(r.getForeignDataSource()), col);

			if (r != null && c instanceof IColumn)
			{
				return new ColumnWrapper((IColumn)c, relations);
			}
			return c;
		}

		return null;
	}

	/**
	 * @return
	 * @throws RepositoryException
	 */
	protected IDataProvider getEnumDataProvider(String id) throws RepositoryException
	{
		// Note: this method is overridden in developer to add the correct type to EnumDataProviders
		String[] enumParts = id.split("\\."); //$NON-NLS-1$
		if (enumParts.length > 3)
		{
			IDataProvider globalDataProvider = getGlobalDataProvider(enumParts[0] + '.' + enumParts[1] + '.' + enumParts[2]);
			if (globalDataProvider instanceof ScriptVariable && ((ScriptVariable)globalDataProvider).isEnum())
			{
				return new EnumDataProvider(id, 0); // untyped
			}
		}

		return null;
	}

	/**
	 * Returns script calculations and aggregates and columns
	 */
	public IDataProvider getDataProviderForTable(ITable table, String dataProviderID) throws RepositoryException
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
		Column column = table.getColumn(Ident.generateNormalizedName(dataProviderID));
		if (column != null)
		{
			return column;
		}
		return null;
	}

	public Map<String, IDataProvider> getAllDataProvidersForTable(ITable table) throws RepositoryException
	{
		if (table == null) return null;
		synchronized (this)
		{
			if (allProvidersForTable == null) allProvidersForTable = new ConcurrentHashMap<ITable, Map<String, IDataProvider>>(64, 0.9f, 16);
		}

		Map<String, IDataProvider> dataProvidersMap = allProvidersForTable.get(table);
		if (dataProvidersMap == null)
		{
			dataProvidersMap = new HashMap<String, IDataProvider>(16, 0.9f);

			for (Column col : table.getColumns())
			{
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
			Map<String, IDataProvider> currentValue = allProvidersForTable.putIfAbsent(table, dataProvidersMap);
			if (currentValue != null) dataProvidersMap = currentValue;
		}
		return dataProvidersMap;
	}

	public void flushDataProvidersForPersist(IPersist persist)
	{
		if (persist == null || allProvidersForTable == null) return;

		TableNode tableNode = (TableNode)persist.getAncestor(IRepository.TABLENODES);
		if (tableNode != null && tableNode.getDataSource() != null) // it might reach this code from the persist created event - where data source is not yet set
		{
			ITable table = getTable(tableNode.getDataSource());
			if (table != null)
			{
				allProvidersForTable.remove(table);
			}
		}
	}

	public void flushDataProvidersForTable(ITable table)
	{
		if (table != null && allProvidersForTable != null)
		{
			allProvidersForTable.remove(table);
			flushGlobalProviders();
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
		globalProviders.clear();
		allProvidersForTable = null;
		dataProviderLookups = null;
		all_styles = null;
		beanDesignInstances = null;
		allObjectscache = null;
		flushFlattenedFormCache();
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

	public IDataProviderLookup getDataproviderLookup(IFoundSetManagerInternal foundSetManager, final IPersist p)
	{
		IDataProviderLookup retval = null;
		synchronized (this)
		{
			if (dataProviderLookups == null) dataProviderLookups = new HashMap<IPersist, IDataProviderLookup>();
			retval = dataProviderLookups.get(p);
			if (retval != null) return retval;
		}
		if (p instanceof Form)
		{
			ITable t = null;
			try
			{
				if (foundSetManager == null)
				{
					t = getTable(((Form)p).getDataSource());
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
			retval = new FormAndTableDataProviderLookup(this, (Form)p, t);
		}
		else if (p instanceof Portal)
		{
			ITable t = null;
			Relation[] relations = getRelationSequence(((Portal)p).getRelationName());
			if (relations == null)
			{
				return null;
			}
			t = getTable(relations[relations.length - 1].getForeignDataSource());
			retval = new FormAndTableDataProviderLookup(this, (Form)p.getParent(), t);
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

		synchronized (this)
		{
			dataProviderLookups.put(p, retval);
		}
		return retval;
	}

	public Solution[] getModules()
	{
		return modules;
	}

	public String[] getSolutionNames()
	{
		List<String> names = new ArrayList<String>();
		if (mainSolution != null)
		{
			names.add(mainSolution.getName());
		}
		if (modules != null)
		{
			for (Solution module : modules)
			{
				names.add(module.getName());
			}
		}
		return names.toArray(new String[0]);
	}

	public boolean hasModule(String moduleName)
	{
		if (modules != null)
		{
			for (Solution module : modules)
			{
				if (module.getName().equals(moduleName))
				{
					return true;
				}
			}
		}
		return false;
	}

	public Relation getRelation(String name)
	{
		if (name == null)
		{
			// should not happen
			Debug.log("Unexpected relation name lookup NULL", new NullPointerException("name"));
			return null;
		}
		if (name.indexOf('.') >= 0)
		{
			// if we get here the relation name should be analyzed using getRelationSequence(name).
			Debug.log("Unexpected relation name lookup", new Exception(name)); //$NON-NLS-1$
			return null;
		}
		return getIndex().getPersistByName(name, Relation.class);
	}

	/**
	 * Get relations from string rel1.rel2. ... .reln
	 *
	 * @param name
	 * @return
	 */
	public Relation[] getRelationSequence(String name)
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
			if (relation == null ||
				(prev != null && (prev.getForeignDataSource() == null || !prev.getForeignDataSource().equals(relation.getPrimaryDataSource()))))
			{
				return null;
			}
			seq[i] = relation;
			prev = relation;
		}
		return seq;
	}

	private void flushGlobalProviders()
	{
		globalProviders.clear();
	}

	public IPersist searchPersist(UUID uuid)
	{
		if (uuid == null) return null;
		return searchPersist(uuid.toString());
	}

	/**
	 * Search for a persist by UUID in the main solution and all modules.
	 *
	 * @param uuid
	 * @return
	 */
	public IPersist searchPersist(String uuid)
	{
		if (uuid == null) return null;
		IPersist persist = getIndex().getPersistByUUID(uuid);
		if (persist == null && loginFlattenedSolution != null)
		{
			return loginFlattenedSolution.searchPersist(uuid);
		}
		return persist;
	}

	/**
	 * Search for a persist in the parent tree in the main solution and all modules.
	 *
	 * @param persist
	 * @return
	 */
	public IPersist searchPersist(IPersist persist)
	{
		if (mainSolution != null)
		{
			IPersist found = AbstractRepository.searchPersist(mainSolution, persist);
			if (found != null)
			{
				return found;
			}
			if (modules != null)
			{
				for (Solution module : modules)
				{
					found = AbstractRepository.searchPersist(module, persist);
					if (found != null)
					{
						return found;
					}
				}
			}
		}
		if (loginFlattenedSolution != null)
		{
			return loginFlattenedSolution.searchPersist(persist);
		}
		return null;
	}

	public void itemChanged(IPersist persist)
	{
		flush(persist);
		if (persist instanceof Form)
		{
			Form form = (Form)persist;
			if (form.getExtendsID() != null)
			{
				if (form.getExtendsForm() == null || !form.getExtendsForm().getUUID().toBytes().equals(form.getExtendsID()))
				{
					form.setExtendsForm(getForm(form.getExtendsID()));
				}
			}
			else
			{
				form.setExtendsForm(null);
			}
		}
		if (persist instanceof Solution)
		{
			buildEventTypesList();
		}
	}

	public void itemChanged(Collection<IPersist> persists)
	{
		for (IPersist persist : persists)
		{
			itemChanged(persist);
		}
	}

	public void itemCreated(IPersist persist)
	{
		flush(persist);
		if (persist instanceof Form)
		{
			Form form = (Form)persist;
			if (form.getExtendsID() != null)
			{
				form.setExtendsForm(getForm(form.getExtendsID()));
			}
		}
	}

	public void itemRemoved(IPersist persist)
	{
		flush(persist);
	}

	/**
	 * @param persist
	 */

	public void setParentSolution(FlattenedSolution parent)
	{
		this.parentFlattenedSolution = parent;
	}

	public void flush(IPersist persist)
	{
		flush(persist, true);
	}

	public void flush(IPersist persist, boolean flushParent)
	{
		if (persist instanceof Form)
		{
			flushFlattendFormCache((Form)persist);
		}
		if (persist instanceof ScriptVariable || persist instanceof ScriptCalculation || persist instanceof AggregateVariable) flushGlobalProviders();
		flushDataProvidersForPersist(persist);
		flushDataProviderLookups(persist);

		allObjectscache = null;

		if (loginFlattenedSolution != null)
		{
			loginFlattenedSolution.flush(persist, false);
		}

		if (flushParent && parentFlattenedSolution != null)
		{
			parentFlattenedSolution.flush(persist);
		}

		if (copySolution != null) copySolution.updateLastModifiedTime();
	}

	public void removeStyle(String name)
	{
		if (name == null) return;
		if (user_created_styles != null && user_created_styles.containsKey(name))
		{
			Style style = user_created_styles.remove(name);
			if (style != null) ComponentFactory.flushStyle(null, style);
		}
		else if (!deletedStyles.contains(name))
		{
			deletedStyles.add(name);
		}
	}

	public synchronized Style getStyle(String name)
	{
		if (name == null) return null;

		if (user_created_styles != null)
		{
			Style style = user_created_styles.get(name);
			if (style != null) return style;
		}
		if (deletedStyles.contains(name))
		{
			return null;
		}
		if (loginFlattenedSolution != null)
		{
			return loginFlattenedSolution.getStyle(name);
		}
		Map<String, Style> allStyles = getAllStyles();
		Style style = allStyles.get(name);
		if (style == null)
		{
			try
			{
				style = (Style)mainSolution.getRootObject().getRepository().getActiveRootObject(name, IRepository.STYLES);
				if (style != null)
				{
					synchronized (allStyles)
					{
						allStyles.put(name, style);
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

	public Style getStyleForForm(Form f, Map<String, String> overridenStyles)
	{
		if (loginFlattenedSolution != null)
		{
			return loginFlattenedSolution.getStyleForForm(f, overridenStyles);
		}
		synchronized (this)
		{
			String style_name = f.getStyleName();
			if (deletedStyles.contains(style_name))
			{
				return null;
			}
			Map<String, Style> allStyles = getAllStyles();
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
				Style style = allStyles.get(style_name);
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
							synchronized (allStyles)
							{
								allStyles.put(style.getName(), style);
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
			Style s = loadStyleForForm(this, f);
			if (s != null)
			{
				synchronized (allStyles)
				{
					allStyles.put(s.getName(), s);
				}
			}
			return s;
		}
	}

	/**
	 * Load style for form, look for extend forms in flattenedSolution when possible
	 * @param flattenedSolution may be null, fallback to f.getSolution() plus repo load
	 * @param f
	 */
	public static Style loadStyleForForm(FlattenedSolution flattenedSolution, Form form)
	{
		try
		{
			String style_name = form.getStyleName();
			if (!(form instanceof FlattenedForm))
			{
				Form extendedForm;
				String extended_form_uuid = form.getExtendsID();

				List<RootObjectReference> modulesMetaData = null;

				// We keep track of visited forms, to avoid infinite loop.
				Set<String> visited = new HashSet<String>();

				while (style_name == null && extended_form_uuid != null)
				{
					visited.add(extended_form_uuid);

					// if this is hit here with a normal (not flattened form) that has an extend.
					// and that form is a solution modal form. the f.getSolution() doesn't have to find it.
					// because f.getSolution() is the copy solution thats inside the FlattenedSolution.
					// that one doesn't have any other forms. But normally all forms should be flattened.
					extendedForm = flattenedSolution == null ? form.getSolution().getForm(extended_form_uuid) : flattenedSolution.getForm(extended_form_uuid);
					if (flattenedSolution == null && extendedForm == null) // check for module form
					{
						if (modulesMetaData == null)
						{
							modulesMetaData = form.getSolution().getRepository().getActiveSolutionModuleMetaDatas(form.getSolution().getUUID());
						}

						for (RootObjectReference moduleMetaData : modulesMetaData)
						{
							Solution module = (Solution)form.getSolution().getRepository()
								.getActiveRootObject(moduleMetaData.getMetaData().getRootObjectUuid());
							extendedForm = module.getForm(extended_form_uuid);
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
					extended_form_uuid = extendedForm.getExtendsID();
					if (visited.contains(extended_form_uuid))
					{
						break;//in case of cycle in form inheritance hierarchy
					}
				}
			}

			if (style_name == null) return null;

			Style s = (Style)form.getSolution().getRepository().getActiveRootObject(style_name, IRepository.STYLES);//preload the style at the server
			return s;
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return null;
	}

	public synchronized void flushBeanDesignInstance(Bean b)
	{
		if (beanDesignInstances != null)
		{
			beanDesignInstances.remove(b);
		}
	}

	/**
	 * set bean design instance if not set yet.
	 * @param b
	 * @param arg
	 * @return new value or old value if instance was already set
	 */
	public synchronized Object setBeanDesignInstance(Bean b, Object arg)
	{
		if (beanDesignInstances == null)
		{
			beanDesignInstances = new ConcurrentHashMap<Bean, Object>();
		}
		else
		{
			Object object = beanDesignInstances.get(b);
			if (object != null)
			{
				return object;
			}
		}

		beanDesignInstances.put(b, arg);
		return arg;
	}

	public synchronized Object getBeanDesignInstance(Bean b)
	{
		return beanDesignInstances == null ? null : beanDesignInstances.get(b);
	}

	// return only the global script methods.
	public Iterator<ScriptMethod> getScriptMethods(String scopeName, boolean sort)
	{
		return getIndex().getGlobalScriptObjects(scopeName, sort, ScriptMethod.class);
	}

	// return only the global script methods.
	public Iterator<ScriptMethod> getScriptMethods(boolean sort)
	{
		return getIndex().getGlobalScriptObjects(null, sort, ScriptMethod.class);
	}

	// will return now all script methods through the whole solution
	public ScriptMethod getScriptMethod(String methodNameOrUUID)
	{
		if (methodNameOrUUID == null) return null;
		IPersist supportScope = getIndex().getPersistByUUID(methodNameOrUUID, ScriptMethod.class);
		if (supportScope instanceof ScriptMethod) return (ScriptMethod)supportScope;

		return getScriptMethod(null, methodNameOrUUID);
	}

	@Override
	public ScriptMethod getScriptMethod(UUID uuid)
	{
		return getScriptMethod(uuid.toString());
	}

	/**
	 * Get global script method.
	 * Scope name may be in methodName (globals.x, scopes.globals.x or scopes.myscope.x) or may be supplied in arg scopeName.
	 */
	public ScriptMethod getScriptMethod(String scopeName, String methodName)
	{
		ISupportScope elem = getGlobalScopeElement(scopeName, methodName);
		if (elem instanceof ScriptMethod)
		{
			return (ScriptMethod)elem;
		}
		return null;
	}

	private ISupportScope getGlobalScopeElement(String sc, String elementName)
	{
		if (elementName == null)
		{
			return null;
		}
		String scopeName = sc;
		String baseName = elementName;
		if (sc == null)
		{
			Pair<String, String> scope = ScopesUtils.getVariableScope(elementName);
			scopeName = scope.getLeft();
			baseName = scope.getRight();
			if (scopeName == null)
			{
				scopeName = ScriptVariable.GLOBAL_SCOPE;
			}
		}
		else
		{
			baseName = ScopesUtils.getVariableScope(elementName).getRight();
		}
		return getIndex().getSupportScope(scopeName, baseName);
	}

	// return only the global script variables.
	public Iterator<ScriptVariable> getScriptVariables(boolean sort)
	{
		return getIndex().getGlobalScriptObjects(null, sort, ScriptVariable.class);
	}

	// return only the global script variables.
	public Iterator<ScriptVariable> getScriptVariables(String scopeName, boolean sort)
	{
		return getIndex().getGlobalScriptObjects(scopeName, sort, ScriptVariable.class);
	}

	/**
	 * Get global script variable.
	 * Scope name may be in variable (globals.x, scopes.globals.x or scopes.myscope.x) or may be supplied in arg scopeName.
	 */
	public ScriptVariable getScriptVariable(String scopeName, String variableName)
	{
		ISupportScope elem = getGlobalScopeElement(scopeName, variableName);
		if (elem instanceof ScriptVariable)
		{
			return (ScriptVariable)elem;
		}
		return null;
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

	public Iterator<TableNode> getTableNodes(String dataSource)
	{
		try
		{
			List<String> tableDataSources = Solution.getTableDataSources(getRepository(), dataSource);
			if (tableDataSources.size() > 1)
			{
				IteratorChain<TableNode> ic = new IteratorChain<TableNode>();
				for (String ds : tableDataSources)
				{
					ic.addIterator(getIndex().getPersistByDatasource(ds, TableNode.class));
				}
				return ic;
			}

		}
		catch (RepositoryException e)
		{
		}
		return getIndex().getPersistByDatasource(dataSource, TableNode.class);
	}

	public Iterator<TableNode> getTableNodes(ITable table)
	{
		if (table == null) return Collections.emptyIterator();
		return getTableNodes(table.getDataSource());
	}

	public Iterator<Relation> getRelations(boolean sort) throws RepositoryException
	{
		return Solution.getRelations(getIndex().getIterableFor(Relation.class), sort);
	}

	public Iterator<Relation> getRelations(ITable filterOnTable, boolean isPrimaryTable, boolean sort, boolean addGlobalsWhenPrimary,
		boolean onlyGlobalsWhenForeign, boolean onlyLiteralsWhenForeign) throws RepositoryException
	{
		return Solution.getRelations(getRepository(), getIndex().getIterableFor(Relation.class), filterOnTable, isPrimaryTable, sort, addGlobalsWhenPrimary,
			onlyGlobalsWhenForeign, onlyLiteralsWhenForeign);
	}

	public Iterator<Relation> getRelations(ITable filterOnTable, boolean isPrimaryTable, boolean sort) throws RepositoryException
	{
		return Solution.getRelations(getRepository(), getIndex().getIterableFor(Relation.class), filterOnTable, isPrimaryTable, sort, true, false, false);
	}

	public Iterator<ValueList> getValueLists(boolean sort)
	{
		return Solution.getValueLists(getIndex().getIterableFor(ValueList.class), sort);
	}

	public ValueList getValueList(String nameOrUUID)
	{
		if (nameOrUUID == null) return null;

		ValueList vl = getIndex().getPersistByUUID(nameOrUUID, ValueList.class);
		if (vl != null) return vl;
		return getIndex().getPersistByName(nameOrUUID, ValueList.class);
	}

	public Menu getMenu(String nameOrUUID)
	{
		if (nameOrUUID == null) return null;

		Menu menu = getIndex().getPersistByUUID(nameOrUUID, Menu.class);
		if (menu != null) return menu;
		return getIndex().getPersistByName(nameOrUUID, Menu.class);
	}

	public Iterator<Menu> getMenus(boolean sort)
	{
		return Solution.getMenus(getIndex().getIterableFor(Menu.class), sort);
	}

	public Iterator<Form> getForms(ITable basedOnTable, boolean sort)
	{
		return Solution.getForms(getIndex().getIterableFor(Form.class),
			basedOnTable == null ? null : createDBTableDataSource(basedOnTable.getServerName(), basedOnTable.getName()), sort);
	}

	public Iterator<Form> getForms(String datasource, boolean sort)
	{
		return Solution.getForms(getIndex().getIterableFor(Form.class), datasource, sort);
	}

	public Iterator<Form> getForms(boolean sort)
	{
		return Solution.getForms(getIndex().getIterableFor(Form.class), null, sort);
	}

	public List<Form> getFormsForNamedFoundset(String namedFoundset)
	{
		// not used in client for now, in the future we may cache this
		List<Form> forms = new ArrayList<Form>();
		Iterator<Form> it = getIndex().getIterableFor(Form.class);
		while (it.hasNext())
		{
			Form form = it.next();
			if (Utils.equalObjects(form.getNamedFoundSet(), namedFoundset))
			{
				forms.add(form);
			}
		}
		return forms;
	}

	public Form getForm(String nameOrUUID)
	{
		if (nameOrUUID == null) return null;

		Form frm = getIndex().getPersistByUUID(nameOrUUID, Form.class);
		if (frm != null) return frm;
		return getIndex().getPersistByName(nameOrUUID, Form.class);
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
		while (f != null && f.getExtendsID() != null)
		{
			f = getForm(f.getExtendsID());
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
			if (parentForm.getUUID().toString().equals(sameTableForm.getExtendsID()))
			{
				inheritingForms.add(sameTableForm);
			}
		}

		return inheritingForms;
	}

	public Iterator<AggregateVariable> getAggregateVariables(ITable basedOnTable, boolean sort)
	{
		Iterator<AggregateVariable> aggregations = getIndex().getPersistByDatasource(basedOnTable.getDataSource(), AggregateVariable.class);
		if (sort)
		{
			return Utils.asSortedIterator(aggregations, NameComparator.INSTANCE);
		}
		return aggregations;
	}

	public ScriptCalculation getScriptCalculation(String name, ITable basedOnTable)
	{
		if (basedOnTable == null)
		{
			return null;
		}
		return getScriptCalculation(name, basedOnTable.getDataSource());
	}

	public ScriptCalculation getScriptCalculation(String name, String basedOnDataSource)
	{
		return getIndex().getPersistByDatasource(basedOnDataSource, ScriptCalculation.class, name);
	}

	public Iterator<ScriptCalculation> getScriptCalculations(ITable basedOnTable, boolean sort)
	{
		if (basedOnTable == null)
		{
			return Collections.<ScriptCalculation> emptyList().iterator();
		}
		return getScriptCalculations(basedOnTable.getDataSource(), sort);
	}

	public Iterator<ScriptCalculation> getScriptCalculations(String basedOnDataSource, boolean sort)
	{
		Iterator<ScriptCalculation> calculations = getIndex().getPersistByDatasource(basedOnDataSource, ScriptCalculation.class);
		if (sort)
		{
			return Utils.asSortedIterator(calculations, NameComparator.INSTANCE);
		}
		return calculations;
	}

	public ScriptMethod getFoundsetMethod(String name, String basedOnDataSource)
	{
		return AbstractBase.selectByName(getFoundsetMethods(basedOnDataSource, false), name);
	}

	public Iterator<ScriptMethod> getFoundsetMethods(String basedOnDatasource, boolean sort)
	{
		if (basedOnDatasource == null)
		{
			return Collections.<ScriptMethod> emptyList().iterator();
		}

		List<ScriptMethod> foundsetMethods = Solution.getFoundsetMethods(getTableNodes(basedOnDatasource), sort);
		if (copySolution != null)
		{
			Iterator<TableNode> tableNodes = copySolution.getTableNodes(basedOnDatasource);
			// if there is a copy solution with a tablenode on that table
			if (tableNodes.hasNext())
			{
				//  remove all script methods with the same name as the method which isnt the copy method itself.
				Iterator<ScriptMethod> copyMethods = tableNodes.next().getFoundsetMethods(false);
				while (copyMethods.hasNext())
				{
					ScriptMethod copyMethod = copyMethods.next();
					for (int i = foundsetMethods.size(); i-- != 0;)
					{
						ScriptMethod method = foundsetMethods.get(i);
						if (copyMethod.getName().equals(method.getName()) && copyMethod != method)
						{
							foundsetMethods.remove(i);
							break;
						}
					}
				}
			}
		}

		//remove the deleted methods from the deletedPersists
		for (ScriptMethod method : foundsetMethods)
		{
			if (getIndex().isRemoved(method))
			{
				foundsetMethods.remove(method);
			}
		}

		return foundsetMethods.iterator();
	}

	public List<ScriptMethod> getFoundsetMethods(ITable basedOnTable, boolean sort) throws RepositoryException
	{
		List<ScriptMethod> foundsetMethods = Solution.getFoundsetMethods(getTableNodes(basedOnTable), sort);
		if (copySolution != null)
		{
			Iterator<TableNode> tableNodes = copySolution.getTableNodes(basedOnTable);
			// if there is a copy solution with a tablenode on that table
			if (tableNodes.hasNext())
			{
				//  remove all script methods with the same name as the method which isnt the copy method itself.
				Iterator<ScriptMethod> copyMethods = tableNodes.next().getFoundsetMethods(false);
				while (copyMethods.hasNext())
				{
					ScriptMethod copyMethod = copyMethods.next();
					for (int i = foundsetMethods.size(); i-- != 0;)
					{
						ScriptMethod scriptCalculation = foundsetMethods.get(i);
						if (copyMethod.getName().equals(scriptCalculation.getName()) && copyMethod != scriptCalculation)
						{
							foundsetMethods.remove(i);
							break;
						}
					}
				}
			}
		}

		//remove the deleted calculation from the deletedPersists
		for (ScriptMethod method : foundsetMethods)
		{
			if (getIndex().isRemoved(method))
			{
				foundsetMethods.remove(method);
			}
		}
		return foundsetMethods;
	}

	public Iterator<Media> getMedias(boolean sort)
	{
		return Solution.getMedias(getIndex().getIterableFor(Media.class), sort);
	}

	public Media getMedia(String nameOrUUID)
	{
		if (nameOrUUID == null) return null;

		Media media = getIndex().getPersistByUUID(nameOrUUID, Media.class);
		if (media != null) return media;
		return getIndex().getPersistByName(nameOrUUID, Media.class);
	}

	/**
	 * Get the internal relation that can be used to sort on this value list using the display values.
	 * @param valueList
	 * @param callingTable
	 * @param dataProviderID
	 * @param foundSetManager
	 * @return
	 * @throws RepositoryException
	 */
	public Relation getValuelistSortRelation(ValueList valueList, Table callingTable, String dataProviderID, IFoundSetManagerInternal foundSetManager)
		throws RepositoryException
	{
		if (callingTable == null || valueList == null)
		{
			return null;
		}

		String destDataSource;
		Relation[] relationSequence;
		String relationPrefix;
		switch (valueList.getDatabaseValuesType())
		{
			case IValueListConstants.TABLE_VALUES :
				// create an internal relation
				relationSequence = null;
				relationPrefix = ""; //$NON-NLS-1$
				destDataSource = valueList.getDataSource();
				break;

			case IValueListConstants.RELATED_VALUES :
				// replace the last relation in the sequence with an internal relation
				relationSequence = getRelationSequence(valueList.getRelationName());
				if (relationSequence == null)
				{
					return null;
				}
				if (relationSequence.length > 1)
				{
					for (Relation r : relationSequence)
					{
						if (r.getJoinType() != INNER_JOIN)
						{
							// disabled related vl sorting for muti-level related VLs,
							// outer join on the intermediate tables causes extra results that influence the sorting result
							return null;
						}
					}
				}
				StringBuilder sb = new StringBuilder();
				for (Relation r : relationSequence)
				{
					sb.append('-').append(r.getName());
				}
				relationPrefix = sb.toString();

				destDataSource = relationSequence[relationSequence.length - 1].getForeignDataSource();
				break;

			default :
				return null;
		}

		if (destDataSource == null || !isSameServer(callingTable.getDataSource(), destDataSource))
		{
			// do not create a cross-server relation
			return null;
		}

		Table destTable = (Table)foundSetManager.getTable(destDataSource);
		if (destTable == null)
		{
			return null;
		}

		String relationName = Relation.INTERNAL_PREFIX + "VL-" + callingTable.getDataSource() + '-' + dataProviderID + relationPrefix + '-' + //$NON-NLS-1$
			valueList.getName() + '-';

		synchronized (this)
		{
			Column callingColumn = callingTable.getColumn(dataProviderID);
			if (callingColumn == null)
			{
				return null;
			}
			Relation relation = getRelation(relationName);
			if (relation == null)
			{
				// create in internal relation
				String dp;
				int returnValues = valueList.getReturnDataProviders();
				if ((returnValues & 1) != 0)
				{
					dp = valueList.getDataProviderID1();
				}
				else if ((returnValues & 2) != 0)
				{
					dp = valueList.getDataProviderID2();
				}
				else if ((returnValues & 4) != 0)
				{
					dp = valueList.getDataProviderID3();
				}
				else
				{
					return null;
				}

				Column destColumn = destTable.getColumn(dp);
				if (destColumn == null)
				{
					return null;
				}

				// create internal value list relation
				QueryTable callingQTable = callingTable.queryTable();
				QueryTable destQTable = destTable.queryTable();

				List<ISQLTableJoin> joins = new ArrayList<ISQLTableJoin>();
				ISQLTableJoin lastJoin = null;
				if (relationSequence == null)
				{
					// table values
					joins.add(lastJoin = new QueryJoin(relationName, callingQTable, destQTable, new AndCondition(), LEFT_OUTER_JOIN, false));

					if (valueList.getUseTableFilter()) //apply name as filter on column valuelist_name
					{
						lastJoin.getCondition().addCondition(
							new CompareCondition(IBaseSQLCondition.EQUALS_OPERATOR, new QueryColumn(destQTable, DBValueList.NAME_COLUMN), valueList.getName()));
					}
				}
				else
				{
					// related values
					QueryTable primaryQTable = callingQTable;
					for (int i = 0; i < relationSequence.length; i++)
					{
						Relation r = relationSequence[i];
						QueryTable foreignQTable;
						if (i == relationSequence.length - 1)
						{
							// last one
							foreignQTable = destQTable;
						}
						else
						{
							ITable relForeignTable = getTable(r.getForeignDataSource());
							if (relForeignTable == null)
							{
								return null;
							}
							foreignQTable = relForeignTable.queryTable();
						}
						lastJoin = foundSetManager.getSQLGenerator().createJoin(this, r, primaryQTable, foreignQTable, false, new IGlobalValueEntry()
						{
							public Object setDataProviderValue(String dpid, Object value)
							{
								return null;
							}

							public Object getDataProviderValue(String dpid)
							{
								// A value will be added when the relation is used, see SQLGenerator.createJoin
								return new Placeholder(new ObjectPlaceholderKey<int[]>(null, dpid));
							}

							public boolean containsDataProvider(String dpid)
							{
								return false;
							}
						});
						joins.add(lastJoin);
						primaryQTable = foreignQTable;
					}
				}

				// add condition for return dp id
				lastJoin.getCondition().addCondition(
					new CompareCondition(IBaseSQLCondition.EQUALS_OPERATOR, destColumn.queryColumn(destQTable), callingColumn.queryColumn(callingQTable)));

				relation = getSolutionCopy().createNewRelation(new ScriptNameValidator(this), relationName, callingTable.getDataSource(), destDataSource,
					LEFT_OUTER_JOIN);

				ISQLTableJoin join;
				if (joins.size() == 1)
				{
					join = lastJoin;
				}
				else
				{
					// combine joins
					join = new QueryCompositeJoin(relationName, joins);
				}
				relation.setRuntimeProperty(Relation.RELATION_JOIN, join);
			}

			return relation;
		}
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@SuppressWarnings("nls")
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("FlattenedSolution of solution: ");
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
				flushFlattendFormCache(form);
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
			ChangedFormData changedFormData = changedForms.get(form.getName());
			if (changedFormData != null)
			{
				changedFormData.instances.remove(namedInstance);
				if (changedFormData.instances.size() == 0)
				{
					changedForms.remove(form.getName());
				}
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
			if (liveForms.containsKey(form.getName()) && !isInDesign(form))
			{
				ChangedFormData changedFormData = changedForms.get(form.getName());
				if (changedFormData == null)
				{
					// if liveForms does contain the form then remember it.
					changedFormData = new ChangedFormData(liveForms.get(form.getName()));
					changedForms.put(form.getName(), changedFormData);
				}
				else
				{
					// only interested in the last line that altered the form.
					// the the forms that are currently live.
					changedFormData.instances.addAll(liveForms.get(form.getName()));
				}

				changedFormData.lastStack = Debug.getScriptStacktraceFromContext(""); //$NON-NLS-1$
			}
		}
	}

	/**
	 *
	 */
	@SuppressWarnings("nls")
	public void checkStateForms(IServiceProvider provider)
	{
		synchronized (liveForms)
		{
			if (changedForms.size() > 0)
			{
				StringBuilder sb = new StringBuilder();
				sb.append("The form(s) that are stale (can also be a parent form if form inheritance is used) are:\n\t"); //$NON-NLS-1$
				for (String fName : changedForms.keySet())
				{
					Set<String> set = changedForms.get(fName).instances;
					sb.append("Form name:'");
					sb.append(fName);
					sb.append("' with instances: [");
					for (String instanceName : set)
					{
						sb.append(instanceName);
						sb.append(',');
					}
					sb.setLength(sb.length() - 1);
					sb.append("]");
					String stack = changedForms.get(fName).lastStack;
					if (stack != null)
					{
						sb.append('\n');
						sb.append(stack);
					}
					sb.append("\n\t"); //$NON-NLS-1$
				}
				sb.setLength(sb.length() - 1);
				changedForms.clear();
				provider.reportError("Stale form(s) detected, form(s) were altered by the solution model without destroying them first", sb.toString()); //$NON-NLS-1$
			}
		}

	}

	public Collection<EventType> getEventTypes()
	{
		if (eventTypes != null)
		{
			return eventTypes.values();
		}
		return Collections.emptyList();
	}

	public EventType getEventType(String name)
	{
		if (eventTypes != null)
		{
			return eventTypes.get(name);
		}
		return null;
	}

	private void buildEventTypesList()
	{
		eventTypes = new HashMap<String, EventType>();
		if (mainSolution != null && mainSolution.getEventTypes() != null)
		{
			mainSolution.getEventTypes().keySet().stream()
				.forEach(name -> eventTypes.put(name, new EventType(name, mainSolution.getEventTypes().getJSONObject(name))));
		}
		if (modules != null)
		{
			for (Solution solution : modules)
			{
				if (solution.getEventTypes() != null)
				{
					solution.getEventTypes().keySet().stream().forEach(
						name -> eventTypes.put(name, new EventType(name, solution.getEventTypes().getJSONObject(name))));
				}
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

	private void refreshSuperForms(final IPersist persist)
	{
		if (persist != null)
		{
			Iterator<Form> it = getForms(false);
			while (it.hasNext())
			{
				Form childForm = it.next();
				if (childForm.getUUID().equals(persist.getUUID()) || persist.getUUID().toString().equals(childForm.getExtendsID()))
				{
					// this is an adjustment of a sub form make sure we create a copy first.
					if (!childForm.getUUID().equals(persist.getUUID()) && getSolutionCopy().getChild(childForm.getUUID()) == null)
					{
						childForm = createPersistCopy(childForm);
						registerChangedForm(childForm);
					}
					childForm.setExtendsForm(getForm(childForm.getExtendsID()));
				}
			}
		}
	}

	private static class ChangedFormData
	{
		private String lastStack;
		private final Set<String> instances;

		private ChangedFormData(Set<String> instances)
		{
			// make copy, so that this set is stable from this moment
			this.instances = new HashSet<String>(instances);
		}
	}

	public Form revertForm(String name)
	{
		Form form = getForm(name);
		if (form == null)
		{
			// if this form is not found, then it could be in the removedPersist
			// then revert the remove/delete
			for (IPersist persist : getIndex().getRemoved())
			{
				if (persist instanceof Form && name.equals(((Form)persist).getName()))
				{
					getIndex().removeRemoved(persist);
					flush(persist);
					form = getForm(name);
					break;
				}
			}
		}
		if (form != null)
		{
			deletePersistCopy(form, true);
			form = getForm(name);
			if (form != null) registerChangedForm(form);
		}
		return form;
	}

	public String getName()
	{
		Solution solution = getSolution();
		return solution != null ? solution.getName() : null;
	}

	@Override
	public ITable getTable(String dataSource)
	{
		try
		{
			if (dataSource == null) return null;
			String[] snt = getDBServernameTablename(dataSource);
			if (snt != null)
			{
				return getSolution().getServer(snt[0]).getTable(snt[1]);
			}

			// not a server/table combi, ask the current clients foundset manager
			if (J2DBGlobals.getServiceProvider() != null && J2DBGlobals.getServiceProvider().getFoundSetManager() != null)
			{
				return J2DBGlobals.getServiceProvider().getFoundSetManager().getTable(dataSource);
			}
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return null;
	}

	@Override
	public IServer getServer(String dataSource)
	{
		try
		{
			String[] snt = getDBServernameTablename(dataSource);
			if (snt != null)
			{
				return getSolution().getServer(snt[0]);
			}

			String inMemTableName = getInmemDataSourceName(dataSource);
			if (inMemTableName != null)
			{
				return getSolution().getServer(IServer.INMEM_SERVER);
			}
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return null;
	}

	public synchronized void reload()
	{
		if (mainSolution != null)
		{
			this.flushAllCachedData();
			getAllStyles();
			if (index != null)
			{
				index.destroy();
				index = null;
			}
		}
	}

	private class EmptyPersistIndex implements ISolutionModelPersistIndex
	{
		private EmptyPersistIndex()
		{
			StringWriter out = new StringWriter();
			new RuntimeException().printStackTrace(new PrintWriter(out));
			Debug.log("FlattendSolution " + FlattenedSolution.this + " index not there: " + out.toString());
		}

		@Override
		public IPersist getPersistByUUID(String uuid)
		{
			return null;
		}

		@Override
		public <T extends IPersist> T getPersistByUUID(String uuid, Class<T> clz)
		{
			return null;
		}

		@Override
		public <T extends IPersist> T getPersistByName(String name, Class<T> persistClass)
		{
			return null;
		}

		@Override
		public <T extends IPersist> Iterator<T> getPersistByDatasource(String datasource, Class<T> persistClass)
		{
			return null;
		}

		@Override
		public <T extends IPersist> T getPersistByDatasource(String datasource, Class<T> persistClass, String name)
		{
			return null;
		}

		@Override
		public IScriptElement getSupportScope(String scopeName, String baseName)
		{
			return null;
		}

		@Override
		public <T extends IScriptElement> Iterator<T> getGlobalScriptObjects(String scopeName, boolean sort, Class<T> cls)
		{
			List<T> empty = Collections.emptyList();
			return empty.iterator();
		}

		@Override
		public <T extends IPersist> Iterator<T> getIterableFor(Class<T> clz)
		{
			Set<T> set = Collections.emptySet();
			return set.iterator();
		}

		@Override
		public void destroy()
		{
		}

		@Override
		public void setSolutionModelSolution(Solution solution)
		{
		}

		@Override
		public void addRemoved(IPersist persist)
		{
		}

		@Override
		public Set<IPersist> getRemoved()
		{
			return Collections.emptySet();
		}

		@Override
		public void removeRemoved(IPersist persist)
		{
		}

		@Override
		public boolean isRemoved(IPersist persist)
		{
			return false;
		}

		@Override
		public void reload()
		{
		}
	}
}
