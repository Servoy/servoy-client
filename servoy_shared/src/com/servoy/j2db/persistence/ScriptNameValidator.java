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


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.keyword.Ident;

/**
 * @author jcompagner,jblok
 */
public class ScriptNameValidator implements IValidateName
{
	private final FlattenedSolution solutionRoot;//can be null

	public ScriptNameValidator()
	{
		this(null);//used for reserved names only
	}

	public ScriptNameValidator(FlattenedSolution root)
	{
		solutionRoot = root;
	}

	/**
	 * skip_element_id is used in case of a rename
	 */
	public void checkName(String nameToCheck, UUID skip_element_uuid, ValidatorSearchContext searchContext, boolean sqlRelated) throws RepositoryException
	{
		List<String> warnings = new ArrayList<>();
		if (nameToCheck == null || nameToCheck.length() == 0)
		{
			warnings.add("The name is undefined please specify correct name"); //$NON-NLS-1$
		}
		if (Ident.checkIfKeyword(nameToCheck))
		{
			warnings.add("there is a servoy or javascript keyword with name " + nameToCheck); //$NON-NLS-1$
		}
		if (Ident.checkIfReservedOSWord(nameToCheck))
		{
			warnings.add(nameToCheck + " is a reserved word on some operating systems"); //$NON-NLS-1$
		}

//		try
//		{
//			if (sqlRelated)
//			{
//				IServer server = getServer(searchContext.getObject());
//				String databaseType = server == null ? null : server.getDatabaseType();
//
//				if (SQLKeywords.checkIfKeyword(nameToCheck, databaseType))
//				{
//					warnings.add("there is a SQL keyword with name " + nameToCheck + " for database type " + databaseType);
//				}
//			}
//		}
//		catch (RemoteException e)
//		{
//			throw new RepositoryException(e);
//		}

		if (nameToCheck.contains(" ")) //$NON-NLS-1$
		{
			warnings.add("there is a space in the name '" + nameToCheck + "' this is not allowed"); //$NON-NLS-1$ //$NON-NLS-2$
		}

		if (solutionRoot != null && solutionRoot.getSolution().getSolutionType() == SolutionMetaData.MOBILE &&
			Ident.checkIfReservedBrowserWindowObjectWord(nameToCheck))
		{
			if (searchContext.getType() == IRepository.COLUMNS || searchContext.getType() == IRepository.SCRIPTCALCULATIONS ||
				searchContext.getType() == IRepository.RELATIONS || searchContext.getType() == IRepository.SCRIPTVARIABLES ||
				searchContext.getType() == IRepository.METHODS)
				warnings.add(nameToCheck + " is a reserved window object word in the (mobile)browser"); //$NON-NLS-1$
		}

		Object obj = findDuplicate(nameToCheck, skip_element_uuid, searchContext);
		if (obj instanceof ScriptVariable)
		{
			if (searchContext.getObject() instanceof Form)
			{
				warnings.add("The variable with name '" + nameToCheck + "' already exists"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			else
			{
				warnings.add("The name '" + nameToCheck + "' already exist as a scope variable"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		if (obj instanceof ScriptMethod)
		{
			if (searchContext.getObject() instanceof Form)
			{
				warnings.add("The method with name '" + nameToCheck + "' already exists for form " + ((Form)searchContext.getObject()).getName()); //$NON-NLS-1$ //$NON-NLS-2$
			}
			if (searchContext.getObject() instanceof TableNode)
			{
				warnings.add("The method with name '" + nameToCheck + "' already exists as a foundset method for data source " + //$NON-NLS-1$//$NON-NLS-2$
					((TableNode)searchContext.getObject()).getDataSource());
			}
			else
			{
				warnings.add("The name '" + nameToCheck + "' already exists as a scope method"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		if (obj instanceof Form)
		{
			warnings.add("The name '" + nameToCheck + "' already exists as another form"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (obj instanceof Relation)
		{
			warnings.add("The name '" + nameToCheck + "' already exists as a relation name"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (obj instanceof AggregateVariable)
		{
			warnings.add("The name '" + nameToCheck + "' already exists as an aggregate"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (obj instanceof Column)
		{
			warnings.add("The name '" + nameToCheck + "' already exists as a column"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (obj instanceof ScriptCalculation)
		{
			warnings.add("The name '" + nameToCheck + "' already exists as a scriptcalculation"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (obj instanceof Media)
		{
			warnings.add("The name '" + nameToCheck + "' already exists as media in " + ((Media)obj).getRootObject().getName()); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (obj instanceof ValueList)
		{
			warnings.add("The name '" + nameToCheck + "' already exists as a valuelist"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (obj instanceof Menu)
		{
			warnings.add("The name '" + nameToCheck + "' already exists as a menu"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (obj instanceof IFormElement)
		{
			warnings.add("The element '" + nameToCheck + "' already exists on the form"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (obj instanceof FormElementGroup)
		{
			warnings.add("The group '" + nameToCheck + "' already exists on the form"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (obj instanceof Tab)
		{
			warnings.add("The tab '" + nameToCheck + "' already exists on the tab panel"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (obj instanceof LayoutContainer)
		{
			warnings.add("The layout container '" + nameToCheck + "' already exists on the form"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (obj instanceof MenuItem)
		{
			warnings.add("The menu item '" + nameToCheck + "' already exists"); //$NON-NLS-1$ //$NON-NLS-2$
		}

		if (!warnings.isEmpty())
		{
			throw new NamevalidationException(warnings);
		}
	}

	/**
	 * @param object
	 * @return
	 * @throws RepositoryException
	 */
	private IServer getServer(Object object) throws RepositoryException
	{
		if (solutionRoot == null)
		{
			return null;
		}

		if (object instanceof IServer)
		{
			return (IServer)object;
		}
		if (object instanceof ITable)
		{
			return solutionRoot.getServer(((ITable)object).getDataSource());
		}
		if (object instanceof TableNode)
		{
			return solutionRoot.getServer(((TableNode)object).getDataSource());
		}

		if (object instanceof IColumn)
		{
			return getServer(((IColumn)object).getTable());
		}
		if (object instanceof IPersist)
		{
			return getServer(((IPersist)object).getParent());
		}

		return null;
	}

	public Object findDuplicate(final String nameToCheck, final UUID skip_element_UUID, ValidatorSearchContext searchContext) throws RepositoryException
	{
		if (solutionRoot == null)
		{
			return null;
		}
		if (nameToCheck == null)
		{
			Debug.error("Name to check is null for element uuid " + skip_element_UUID); //$NON-NLS-1$
			return null;
		}
		if (searchContext == null)
		{
			return null;
		}

		//Test for script calculations
		if (searchContext.getObject() instanceof TableNode || searchContext.getObject() instanceof Table)
		{
			Object obj = testRelations(nameToCheck, skip_element_UUID);
			if (obj != null) return obj;

			ITable table;
			if (searchContext.getObject() instanceof Table)
			{
				table = (Table)searchContext.getObject();
			}
			else
			{
				table = solutionRoot.getTable(((TableNode)searchContext.getObject()).getDataSource());
			}

			// Test table dataproviders
			obj = testTableProviders(table, nameToCheck, skip_element_UUID, searchContext.getType() == IRepository.COLUMNS,
				searchContext.getType() == IRepository.SCRIPTCALCULATIONS);
			if (obj != null) return obj;

			if (searchContext.getType() != IRepository.METHODS)
			{
				// Test forms that uses those tables...
				for (Form f : Utils.iterate(solutionRoot.getForms(table, false)))
				{
					obj = testFormScripts(f, nameToCheck, skip_element_UUID);
					if (obj != null) return obj;
				}
			}

			List<ScriptMethod> foundsetMethods = solutionRoot.getFoundsetMethods(table, false);
			for (ScriptMethod scriptMethod : foundsetMethods)
			{
				if (nameToCheck.equals(scriptMethod.getName()) && !scriptMethod.getUUID().equals(skip_element_UUID))
				{
					return scriptMethod;
				}
			}
		}

		// Test the global levels. (form names and relations)null
		else if ((searchContext.getType() == IRepository.SCRIPTVARIABLES || searchContext.getType() == IRepository.METHODS) &&
			!(searchContext.getObject() instanceof Form))
		{
			if (searchContext.getObject() instanceof String)
			{
				// search context string is scopeName
				String scope = (String)searchContext.getObject();
				ScriptVariable sgv = solutionRoot.getScriptVariable(scope, nameToCheck);
				if (sgv != null && !sgv.getUUID().equals(skip_element_UUID))
				{
					return sgv;
				}

				ScriptMethod sm = solutionRoot.getScriptMethod(scope, nameToCheck);
				if (sm != null && !sm.getUUID().equals(skip_element_UUID))
				{
					return sm;
				}
			}
		}

		if (searchContext.getType() == IRepository.FORMS)
		{
			Form form = solutionRoot.getForm(nameToCheck);
			if (form != null && !form.getUUID().equals(skip_element_UUID))
			{
				return form;
			}
		}

		if (searchContext.getType() == IRepository.LAYOUTCONTAINERS)
		{
			return ((IPersist)searchContext.getObject()).getAncestor(IRepository.FORMS).acceptVisitor(o -> {
				if (o instanceof LayoutContainer && nameToCheck.equalsIgnoreCase(((LayoutContainer)o).getName()) &&
					!((LayoutContainer)o).getUUID().equals(skip_element_UUID))
				{
					return o;
				}
				return IPersistVisitor.CONTINUE_TRAVERSAL;
			});
		}

		if (searchContext.getObject() instanceof Form && searchContext.getType() != IRepository.ELEMENTS && searchContext.getType() != IRepository.FORMS)
		{
			Object obj = testRelations(nameToCheck, skip_element_UUID);
			if (obj != null) return obj;

			// It's a form method
			// First test form scripts
			obj = testFormScripts((Form)searchContext.getObject(), nameToCheck, skip_element_UUID);
			if (obj != null) return obj;

			// It's a form vars
			// First test formvars
			obj = testFormVars((Form)searchContext.getObject(), nameToCheck, skip_element_UUID);
			if (obj != null) return obj;

			// Test table dataproviders
			obj = testTableProviders(solutionRoot.getTable(((Form)searchContext.getObject()).getDataSource()), nameToCheck, skip_element_UUID, false, false);
			if (obj != null) return obj;
		}

		if (searchContext.getType() == IRepository.RELATIONS)
		{
			Object obj = testRelations(nameToCheck, skip_element_UUID);
			if (obj != null) return obj;

			Iterator<Form> forms;

			// if there is an object set in the content which is a string then that is the primary datasource of this relation.
			Object primaryDataSource = searchContext.getObject();
			if (primaryDataSource instanceof String)
			{
				forms = solutionRoot.getForms((String)primaryDataSource, false);
			}
			else
			{
				forms = solutionRoot.getForms(false);
			}
			for (Form form : Utils.iterate(forms))
			{
				obj = testFormScripts(form, nameToCheck, skip_element_UUID);
				if (obj != null) return obj;
			}
		}

		if (searchContext.getType() == IRepository.VALUELISTS)
		{
			ValueList vl = solutionRoot.getValueList(nameToCheck);
			if (vl != null && !vl.getUUID().equals(skip_element_UUID))
			{
				return vl;
			}
		}

		if (searchContext.getType() == IRepository.MENUS)
		{
			Menu menu = solutionRoot.getMenu(nameToCheck);
			if (menu != null && !menu.getUUID().equals(skip_element_UUID))
			{
				return menu;
			}
		}

		if (searchContext.getType() == IRepository.MENU_ITEMS)
		{
			if (searchContext.getObject() instanceof List list && list.size() > 0)
			{
				if (list.get(1) != null)
				{ // this is a menu or a menu item
					if (list.get(1) instanceof MenuItem menuItem)
					{
						if (nameToCheck.equalsIgnoreCase(menuItem.getName()) && !menuItem.getUUID().equals(skip_element_UUID))
						{
							return menuItem;
						}

						return findMenuItemByName(getAncestorMenu(list.get(1)), nameToCheck, skip_element_UUID);
					}
					return findMenuItemByName(list.get(1), nameToCheck, skip_element_UUID);
				}
			}
		}

		if (searchContext.getType() == IRepository.MEDIA)
		{
			Media media = solutionRoot.getMedia(nameToCheck);
			if (media != null && !media.getUUID().equals(skip_element_UUID))
			{
				return media;
			}
		}

		if (searchContext.getType() == IRepository.ELEMENTS)
		{
			Iterator< ? extends IPersist> childrenIterator = ((ISupportChilds)searchContext.getObject()).getAllObjects();
			if (searchContext.getObject() instanceof AbstractContainer)
			{
				childrenIterator = ((AbstractContainer)searchContext.getObject()).getFlattenedObjects(null).iterator();
			}
			for (IPersist persist : Utils.iterate(childrenIterator))
			{
				if (persist instanceof IFormElement && !persist.getUUID().equals(skip_element_UUID))
				{
					if (persist instanceof ISupportName && nameToCheck.equalsIgnoreCase(((ISupportName)persist).getName()))
					{
						return persist;
					}
					if (nameToCheck.equalsIgnoreCase(((IFormElement)persist).getGroupID()))
					{
						return new FormElementGroup(((IFormElement)persist).getGroupID(), solutionRoot, (Form)persist.getParent());
					}
				}
			}
		}

		if (searchContext.getType() == IRepository.TABS)
		{
			for (IPersist persist : Utils.iterate(((ISupportChilds)searchContext.getObject()).getAllObjects()))
			{
				if (persist instanceof Tab && nameToCheck.equalsIgnoreCase(((Tab)persist).getName()) && !persist.getUUID().equals(skip_element_UUID))
				{
					return persist;
				}
			}
		}

		return null;
	}

	private MenuItem findMenuItemByName(Object object, String nameToCheck, UUID uuid)
	{
		if (object instanceof MenuItem menuItem)
		{
			if (nameToCheck.equalsIgnoreCase(menuItem.getName()) && !menuItem.getUUID().equals(uuid))
			{
				return menuItem;
			}
			Iterator<IPersist> children = menuItem.getAllObjects();
			while (children.hasNext())
			{
				IPersist child = children.next();
				MenuItem result = findMenuItemByName(child, nameToCheck, uuid);
				if (result != null)
				{
					return result;
				}
			}
		}
		if (object instanceof Menu menu)
		{
			Iterator<IPersist> children = menu.getAllObjects();
			while (children.hasNext())
			{
				IPersist child = children.next();

				MenuItem result = findMenuItemByName(child, nameToCheck, uuid);
				if (result != null)
				{
					return result;
				}
			}
		}
		return null;
	}


	private Menu getAncestorMenu(Object object)
	{
		if (object == null) return null;

		if (object instanceof Menu menu)
		{
			return menu;
		}
		else if (object instanceof MenuItem menuItem)
		{
			return getAncestorMenu(menuItem.getParent());
		}

		return null;
	}

	/**
	 * Method testTableProviders.
	 *
	 * @param table
	 * @param next
	 * @param id
	 * @param b
	 * @param b1
	 */
	private Object testTableProviders(ITable table, String next, UUID uuid, boolean isColumn, boolean isCalculation)
	{
		if (table == null) return null;
		String dataProviderID = null;
		for (AggregateVariable av : Utils.iterate(solutionRoot.getAggregateVariables(table, false)))
		{
			dataProviderID = av.getDataProviderID();
			if (dataProviderID == null)
			{
				Debug.warn("Aggregate variable with null dataProviderID/name found in table " + table.getDataSource());
			}
			else if (dataProviderID.equals(next) && !av.getUUID().equals(uuid))
			{
				return av;
			}
		}
		if (!isCalculation)
		{
			for (Column column : table.getColumns())
			{
				if (column.getDataProviderID().equals(next) && !column.getUUID().equals(uuid))
				{
					return column;
				}
			}
		}
		if (!isColumn)
		{
			for (ScriptCalculation sc : Utils.iterate(solutionRoot.getScriptCalculations(table, false)))
			{
				dataProviderID = sc.getDataProviderID();
				if (dataProviderID == null)
				{
					Debug.warn("Script calculation with null dataProviderID/name found in table " + table.getDataSource());
				}
				else if (dataProviderID.equals(next) && !sc.getUUID().equals(uuid))
				{
					return sc;
				}
			}
		}
		return null;
	}

	private Object testRelations(String name, UUID uuid)
	{
		Relation r = solutionRoot.getRelation(name);
		if (r != null && !r.getUUID().equals(uuid)) // relations with mixed name casing are allowed at runtime with solution model but to avoid confusions do not allow names equalIgnoreCase in this case either
		{
			return r;
		}
		return null;
	}

	private Object testFormScripts(Form form, String next, UUID uuid)
	{
		ScriptMethod sm = form.getScriptMethod(next);
		if (sm != null && !sm.getUUID().equals(uuid))
		{
			return sm;
		}
		return null;
	}

	private Object testFormVars(Form form, String next, UUID uuid)
	{
		ScriptVariable sm = form.getScriptVariable(next);
		if (sm != null && !sm.getUUID().equals(uuid))
		{
			return sm;
		}
		return null;
	}
}