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


import java.util.Iterator;
import java.util.List;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.keyword.Ident;
import com.servoy.j2db.util.keyword.SQLKeywords;

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
	public void checkName(String nameToCheck, int skip_element_id, ValidatorSearchContext searchContext, boolean sqlRelated) throws RepositoryException
	{
		if (nameToCheck == null || nameToCheck.length() == 0)
		{
			throw new RepositoryException("The name is undefined please specify correct name"); //$NON-NLS-1$
		}
		if (Ident.checkIfKeyword(nameToCheck))
		{
			throw new RepositoryException("there is a keyword with name " + nameToCheck); //$NON-NLS-1$
		}
		if (Ident.checkIfReservedOSWord(nameToCheck))
		{
			throw new RepositoryException(nameToCheck + " is a reserved word on some operating systems"); //$NON-NLS-1$
		}
		if (sqlRelated && SQLKeywords.checkIfKeyword(nameToCheck))
		{
			throw new RepositoryException("there is a SQL keyword with name " + nameToCheck); //$NON-NLS-1$
		}
		if (nameToCheck.contains(" ")) //$NON-NLS-1$
		{
			throw new RepositoryException("there is a space in the name '" + nameToCheck + "' this is not allowed"); //$NON-NLS-1$ //$NON-NLS-2$
		}

		if (solutionRoot != null && solutionRoot.getSolution().getSolutionType() == SolutionMetaData.MOBILE &&
			Ident.checkIfReservedBrowserWindowObjectWord(nameToCheck))
		{
			if (searchContext.getType() == IRepository.COLUMNS || searchContext.getType() == IRepository.SCRIPTCALCULATIONS ||
				searchContext.getType() == IRepository.RELATIONS || searchContext.getType() == IRepository.SCRIPTVARIABLES ||
				searchContext.getType() == IRepository.METHODS)
				throw new RepositoryException(nameToCheck + " is a reserved window object word in the (mobile)browser"); //$NON-NLS-1$
		}

		Object obj = findDuplicate(nameToCheck, skip_element_id, searchContext);
		if (obj == null) return;
		if (obj instanceof ScriptVariable)
		{
			if (searchContext.getObject() instanceof Form)
			{
				throw new RepositoryException("The variable with name '" + nameToCheck + "' already exists"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			throw new RepositoryException("The name '" + nameToCheck + "' already exist as a scope variable"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (obj instanceof ScriptMethod)
		{
			if (searchContext.getObject() instanceof Form)
			{
				throw new RepositoryException(
					"The method with name '" + nameToCheck + "' already exists for form " + ((Form)searchContext.getObject()).getName()); //$NON-NLS-1$ //$NON-NLS-2$
			}
			if (searchContext.getObject() instanceof TableNode)
			{
				throw new RepositoryException("The method with name '" + nameToCheck + "' already exists as a foundset method for data source " + //$NON-NLS-1$//$NON-NLS-2$
					((TableNode)searchContext.getObject()).getDataSource());
			}
			throw new RepositoryException("The name '" + nameToCheck + "' already exists as a scope method"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (obj instanceof Form)
		{
			throw new RepositoryException("The name '" + nameToCheck + "' already exists as another form"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (obj instanceof Relation)
		{
			throw new RepositoryException("The name '" + nameToCheck + "' already exists as a relation name"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (obj instanceof AggregateVariable)
		{
			throw new RepositoryException("The name '" + nameToCheck + "' already exists as an aggregate"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (obj instanceof Column)
		{
			throw new RepositoryException("The name '" + nameToCheck + "' already exists as a column"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (obj instanceof ScriptCalculation)
		{
			throw new RepositoryException("The name '" + nameToCheck + "' already exists as a scriptcalculation"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (obj instanceof Media)
		{
			throw new RepositoryException("The name '" + nameToCheck + "' already exists as media in " + ((Media)obj).getRootObject().getName()); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (obj instanceof ValueList)
		{
			throw new RepositoryException("The name '" + nameToCheck + "' already exists as a valuelist"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (obj instanceof IFormElement)
		{
			throw new RepositoryException("The element '" + nameToCheck + "' already exists on the form"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (obj instanceof FormElementGroup)
		{
			throw new RepositoryException("The group '" + nameToCheck + "' already exists on the form"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (obj instanceof Tab)
		{
			throw new RepositoryException("The tab '" + nameToCheck + "' already exists on the tab panel"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	public Object findDuplicate(String nameToCheck, int skip_element_id, ValidatorSearchContext searchContext) throws RepositoryException
	{
		if (solutionRoot == null)
		{
			return null;
		}
		if (nameToCheck == null)
		{
			Debug.error("Name to check is null for element id " + skip_element_id); //$NON-NLS-1$
			return null;
		}
		if (searchContext == null)
		{
			return null;
		}

		//Test for script calculations
		if (searchContext.getObject() instanceof TableNode || searchContext.getObject() instanceof Table)
		{
			ITable table;
			if (searchContext.getObject() instanceof Table)
			{
				table = (Table)searchContext.getObject();
			}
			else
			{
				table = ((TableNode)searchContext.getObject()).getTable();
			}

			Object obj = testRelations(nameToCheck, skip_element_id);
			if (obj != null) return obj;

			// Test table dataproviders
			obj = testTableProviders(table, nameToCheck, skip_element_id, searchContext.getType() == IRepository.COLUMNS,
				searchContext.getType() == IRepository.SCRIPTCALCULATIONS);
			if (obj != null) return obj;

			if (searchContext.getType() != IRepository.METHODS)
			{
				// Test forms that uses those tables...
				for (Form f : Utils.iterate(solutionRoot.getForms(table, false)))
				{
					obj = testFormScripts(f, nameToCheck, skip_element_id);
					if (obj != null) return obj;
				}
			}

			List<ScriptMethod> foundsetMethods = solutionRoot.getFoundsetMethods(table, false);
			for (ScriptMethod scriptMethod : foundsetMethods)
			{
				if (nameToCheck.equals(scriptMethod.getName()) && scriptMethod.getID() != skip_element_id)
				{
					return scriptMethod;
				}
			}
		}

		// Test the global levels. (form names and relations)null
		else if ((searchContext.getType() == IRepository.SCRIPTVARIABLES || searchContext.getType() == IRepository.METHODS) &&
			!(searchContext.getObject() instanceof Form))
		{
			for (ScriptVariable sgv : Utils.iterate(solutionRoot.getScriptVariables(false)))
			{
				if (nameToCheck.equals(sgv.getName()) && sgv.getID() != skip_element_id &&
					(searchContext.getObject() instanceof String && searchContext.getObject().equals(sgv.getScopeName()))) // search context string is scopeName
				{
					return sgv;
				}
			}
			for (ScriptMethod sm : Utils.iterate(solutionRoot.getScriptMethods(false)))
			{
				if (nameToCheck.equals(sm.getName()) && sm.getID() != skip_element_id &&
					(searchContext.getObject() instanceof String && searchContext.getObject().equals(sm.getScopeName()))) // search context string is scopeName
				{
					return sm;
				}
			}
		}

		if (searchContext.getType() == IRepository.FORMS)
		{
			for (Form form : Utils.iterate(solutionRoot.getForms(false)))
			{
				if (nameToCheck.equalsIgnoreCase(form.getName()) && form.getID() != skip_element_id)
				{
					return form;
				}
			}
		}

		if (searchContext.getObject() instanceof Form && searchContext.getType() != IRepository.ELEMENTS && searchContext.getType() != IRepository.FORMS)
		{
			Object obj = testRelations(nameToCheck, skip_element_id);
			if (obj != null) return obj;

			// It's a form method
			// First test form scripts
			obj = testFormScripts((Form)searchContext.getObject(), nameToCheck, skip_element_id);
			if (obj != null) return obj;

			// It's a form vars
			// First test formvars
			obj = testFormVars((Form)searchContext.getObject(), nameToCheck, skip_element_id);
			if (obj != null) return obj;

			// Test table dataproviders
			obj = testTableProviders(solutionRoot.getTable(((Form)searchContext.getObject()).getDataSource()), nameToCheck, skip_element_id, false, false);
			if (obj != null) return obj;
		}

		if (searchContext.getType() == IRepository.RELATIONS)
		{
			// TODO new or name change of relations must be figured out. (Now only testing form script of every form)
			Object obj = testRelations(nameToCheck, skip_element_id);
			if (obj != null) return obj;

			for (Form form : Utils.iterate(solutionRoot.getForms(false)))
			{
				obj = testFormScripts(form, nameToCheck, skip_element_id);
				if (obj != null) return obj;
			}
		}

		if (searchContext.getType() == IRepository.VALUELISTS)
		{
			for (ValueList vl : Utils.iterate(solutionRoot.getValueLists(false)))
			{
				if (nameToCheck.equalsIgnoreCase(vl.getName()) && vl.getID() != skip_element_id)
				{
					return vl;
				}
			}
		}

		if (searchContext.getType() == IRepository.MEDIA)
		{
			for (Media media : Utils.iterate(solutionRoot.getMedias(false)))
			{
				if (nameToCheck.equalsIgnoreCase(media.getName()) && media.getID() != skip_element_id)
				{
					return media;
				}
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
				if (persist instanceof IFormElement && persist.getID() != skip_element_id)
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
				if (persist instanceof Tab && nameToCheck.equalsIgnoreCase(((Tab)persist).getName()) && persist.getID() != skip_element_id)
				{
					return persist;
				}
			}
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
	private Object testTableProviders(ITable table, String next, int id, boolean isColumn, boolean isCalculation) throws RepositoryException
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
			else if (dataProviderID.equals(next) && av.getID() != id)
			{
				return av;
			}
		}
		if (!isCalculation)
		{
			for (Column column : table.getColumns())
			{
				if (column.getDataProviderID().equals(next) && column.getID() != id)
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
				else if (dataProviderID.equals(next) && sc.getID() != id)
				{
					return sc;
				}
			}
		}
		return null;
	}

	private Object testRelations(String name, int id) throws RepositoryException
	{
		for (Relation r : Utils.iterate(solutionRoot.getRelations(false)))
		{
			if (r.getName().equalsIgnoreCase(name) && r.getID() != id) // relations with mixed name casing are allowed at runtime with solution model but to avoid confusions do not allow names equalIgnoreCase in this case either
			{
				return r;
			}
		}
		return null;
	}

	private Object testFormScripts(Form form, String next, int id)
	{
		ScriptMethod sm = form.getScriptMethod(next);
		if (sm != null && sm.getID() != id)
		{
			return sm;
		}
		return null;
	}

	private Object testFormVars(Form form, String next, int id)
	{
		ScriptVariable sm = form.getScriptVariable(next);
		if (sm != null && sm.getID() != id)
		{
			return sm;
		}
		return null;
	}
}