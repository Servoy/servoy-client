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

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.util.Debug;
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
		if (sqlRelated && SQLKeywords.checkIfKeyword(nameToCheck))
		{
			throw new RepositoryException("there is a SQL keyword with name " + nameToCheck); //$NON-NLS-1$
		}

		Object obj = findDuplicate(nameToCheck, skip_element_id, searchContext);
		if (obj == null) return;
		if (obj instanceof ScriptVariable)
		{
			if (searchContext.getObject() instanceof Form)
			{
				throw new RepositoryException("The variable with name '" + nameToCheck + "' already exists"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			throw new RepositoryException("The name '" + nameToCheck + "' already exist as a global variable"); //$NON-NLS-1$ //$NON-NLS-2$
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
				throw new RepositoryException(
					"The method with name '" + nameToCheck + "' already exists as a foundset method for data source " + ((TableNode)searchContext.getObject()).getDataSource()); //$NON-NLS-1$ //$NON-NLS-2$
			}
			throw new RepositoryException("The name '" + nameToCheck + "' already exists as a global method"); //$NON-NLS-1$ //$NON-NLS-2$
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
			throw new RepositoryException("The name '" + nameToCheck + "' already exists as media"); //$NON-NLS-1$ //$NON-NLS-2$
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
		if (searchContext.getType() == IRepository.METHODS && searchContext.getObject() instanceof TableNode)
		{
			TableNode tn = (TableNode)searchContext.getObject();
			Table table = tn.getTable();
			Iterator<ScriptCalculation> calculations = solutionRoot.getScriptCalculations(table, false);
			while (calculations.hasNext())
			{
				ScriptCalculation sc = calculations.next();
				if (nameToCheck.equals(sc.getName()) && sc.getID() != skip_element_id)
				{
					return sc;
				}
			}
		}
		// Test the global levels. (form names and relations)null 
		if ((searchContext.getType() == IRepository.SCRIPTVARIABLES || searchContext.getType() == IRepository.METHODS) &&
			!(searchContext.getObject() instanceof Form))
		{
			Iterator<ScriptVariable> vars = solutionRoot.getScriptVariables(false);
			while (vars.hasNext())
			{
				ScriptVariable sgv = vars.next();
				if (nameToCheck.equals(sgv.getName()) && sgv.getID() != skip_element_id)
				{
					return sgv;
				}
			}
			Iterator<ScriptMethod> scripts = solutionRoot.getScriptMethods(false);
			while (scripts.hasNext())
			{
				ScriptMethod sm = scripts.next();
				if (nameToCheck.equals(sm.getName()) && sm.getID() != skip_element_id)
				{
					return sm;
				}
			}
		}
		if (searchContext.getType() == IRepository.FORMS)
		{
			Iterator<Form> it = solutionRoot.getForms(false);
			while (it.hasNext())
			{
				Form form = it.next();
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
			obj = testTableProviders(((Form)searchContext.getObject()).getTable(), nameToCheck, skip_element_id, false, false);
			if (obj != null) return obj;
		}
		if (searchContext.getObject() instanceof Table)
		{
			Object obj = testRelations(nameToCheck, skip_element_id);
			if (obj != null) return obj;

			// Test forms that uses those tables...
			Iterator<Form> it = solutionRoot.getForms((Table)searchContext.getObject(), false);
			while (it.hasNext())
			{
				Form f = it.next();
				obj = testFormScripts(f, nameToCheck, skip_element_id);
				if (obj != null) return obj;
			}
			// Test table dataproviders
			obj = testTableProviders((Table)searchContext.getObject(), nameToCheck, skip_element_id, searchContext.getType() == IRepository.COLUMNS,
				searchContext.getType() == IRepository.SCRIPTCALCULATIONS);
			if (obj != null) return obj;
		}
		if (searchContext.getType() == IRepository.RELATIONS)
		{
			// TODO new or name change of relations must be figured out. (Now only testing form script of every form)
			Object obj = testRelations(nameToCheck, skip_element_id);
			if (obj != null) return obj;

			Iterator<Form> it = solutionRoot.getForms(false);
			while (it.hasNext())
			{
				obj = testFormScripts(it.next(), nameToCheck, skip_element_id);
				if (obj != null) return obj;
			}
		}
		if (searchContext.getType() == IRepository.VALUELISTS)
		{
			Iterator<ValueList> it = solutionRoot.getValueLists(false);
			while (it.hasNext())
			{
				ValueList vl = it.next();
				if (nameToCheck.equalsIgnoreCase(vl.getName()) && vl.getID() != skip_element_id)
				{
					return vl;
				}
			}
		}
		if (searchContext.getType() == IRepository.MEDIA)
		{
			Iterator<Media> it = solutionRoot.getMedias(false);
			while (it.hasNext())
			{
				Media media = it.next();
				if (nameToCheck.equalsIgnoreCase(media.getName()) && media.getID() != skip_element_id)
				{
					return media;
				}
			}
		}
		if (searchContext.getType() == IRepository.ELEMENTS)
		{
			Iterator<IPersist> iterator = ((ISupportChilds)searchContext.getObject()).getAllObjects();
			while (iterator.hasNext())
			{
				IPersist persist = iterator.next();
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
			Iterator<IPersist> iterator = ((ISupportChilds)searchContext.getObject()).getAllObjects();
			while (iterator.hasNext())
			{
				IPersist persist = iterator.next();
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
	private Object testTableProviders(Table table, String next, int id, boolean isColumn, boolean isCalculation) throws RepositoryException
	{
		if (table == null) return null;
		Iterator<AggregateVariable> it = solutionRoot.getAggregateVariables(table, false);
		while (it.hasNext())
		{
			AggregateVariable av = it.next();
			if (av.getDataProviderID().equals(next) && av.getID() != id)
			{
				return av;
			}
		}
		if (!isCalculation)
		{
			Iterator<Column> columns = table.getColumns().iterator();
			while (columns.hasNext())
			{
				Column column = columns.next();
				if (column.getDataProviderID().equals(next) && column.getID() != id)
				{
					return column;
				}
			}
		}
		if (!isColumn)
		{
			Iterator<ScriptCalculation> calcs = solutionRoot.getScriptCalculations(table, false);
			while (calcs.hasNext())
			{
				ScriptCalculation sc = calcs.next();
				if (sc.getDataProviderID().equals(next) && sc.getID() != id)
				{
					return sc;
				}
			}
		}
		return null;
	}

	private Object testRelations(String name, int id) throws RepositoryException
	{
		Iterator<Relation> it = solutionRoot.getRelations(false);
		while (it.hasNext())
		{
			Relation r = it.next();
			if (r.getName().equals(name) && r.getID() != id)
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