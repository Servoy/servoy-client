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

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.UUID;


/**
 * @author jcompagner
 */
public class FlattenedForm extends Form
{
	private static BeanInfo beanInfo;
	private final FlattenedSolution flattenedSolution;
	private final Form form;

	static
	{
		try
		{
			beanInfo = Introspector.getBeanInfo(Form.class);
		}
		catch (IntrospectionException e)
		{
			Debug.error("Error loading bean info for form", e);
		}
	}

	/**
	 * @param parent
	 * @param element_id
	 * @param uuid
	 */
	public FlattenedForm(FlattenedSolution flattenedSolution, Form form)
	{
		super(form.getParent(), form.getID(), form.getUUID());
		this.flattenedSolution = flattenedSolution;
		this.form = form;

		fill();

	}

	public List<Form> getAllForms()
	{
		return flattenedSolution.getFormHierarchy(form);
	}

	/**
	 * @param flattenedSolution
	 * @param form
	 * @throws RepositoryException
	 */
	private void fill()
	{
		List<Form> allForms = flattenedSolution.getFormHierarchy(form);
		Collections.reverse(allForms); // change from sub-first to super-first

		Part prevPart = null;
		Map<String, ScriptVariable> formVariables = new HashMap<String, ScriptVariable>();
		Map<String, ScriptMethod> formMethods = new HashMap<String, ScriptMethod>();
		Map<UUID, IPersist> persists = new HashMap<UUID, IPersist>();

		copyPropertiesMap(form.getPropertiesMap(), false);

		for (Form f : allForms)
		{
			for (IPersist ip : f.getAllObjectsAsList())
			{
				// Parts are added below
				if (!(ip instanceof Part))
				{
					internalAddChild(ip);
					if (ip instanceof ScriptVariable)
					{
						ScriptVariable var = (ScriptVariable)ip;
						formVariables.put(var.getName(), var);
					}
					else if (ip instanceof ScriptMethod)
					{
						ScriptMethod met = (ScriptMethod)ip;
						formMethods.put(met.getName(), met);
					}
					persists.put(ip.getUUID(), ip);
				}
			}
			// Add parts
			Iterator<Part> parts = f.getParts();
			while (parts.hasNext())
			{
				// Sub-forms can only add parts to the bottom
				Part part = parts.next();
				if (prevPart != null && (prevPart.getPartType() > part.getPartType() || (prevPart.getPartType() == part.getPartType() && !part.canBeMoved())))
				{
					// check if override
					if (!part.isOverrideElement() || getChild(part.getUUID()) == null) continue;
				}
				prevPart = part;
				internalAddChild(part);
				persists.put(part.getUUID(), part);
			}
		}

		// remove overridden form variables and methods
		Iterator<IPersist> allIt = getAllObjects();
		List<IPersist> remove = new ArrayList<IPersist>();
		List<IPersist> add = new ArrayList<IPersist>();
		while (allIt.hasNext())
		{
			IPersist ip = allIt.next();
			if (ip instanceof ScriptVariable)
			{
				ScriptVariable var = (ScriptVariable)ip;
				if (formVariables.get(var.getName()) != var)
				{
					// was overridden in a sub-form, remove this one
					remove.add(var);
				}
			}
			else if (ip instanceof ScriptMethod)
			{
				ScriptMethod met = (ScriptMethod)ip;
				if (formMethods.get(met.getName()) != met)
				{
					// was overridden in a sub-form, remove this one
					remove.add(met);
				}
			}
			if (persists.get(ip.getUUID()) != ip)
			{
				// was overridden in a sub-form, remove this one
				remove.add(ip);
			}
			else if (((AbstractBase)ip).isOverrideElement())
			{
				boolean parentFound = false;
				for (Form f : allForms)
				{
					if (f.getChild(ip.getUUID()) != null && f.getChild(ip.getUUID()) != ip)
					{
						parentFound = true;
					}
				}
				if (!parentFound) remove.add(ip);
				else if (ip instanceof TabPanel)
				{
					remove.add(ip);
					add.add(new FlattenedTabPanel((TabPanel)ip));
				}
				else if (ip instanceof Portal)
				{
					remove.add(ip);
					add.add(new FlattenedPortal((Portal)ip));
				}
			}
		}

		for (IPersist var : remove)
		{
			internalRemoveChild(var);
		}
		for (IPersist var : add)
		{
			internalAddChild(var);
		}

		setSize(checkParts(getParts(), getSize())); // recalculate height
	}

	/**
	 * Called only in develop time.
	 */
	public void reload()
	{
		internalClearAllObjects();
		fill();
	}

	@Override
	public IPersist getSuperPersist()
	{
		return form.getExtendsForm();
	}
}
