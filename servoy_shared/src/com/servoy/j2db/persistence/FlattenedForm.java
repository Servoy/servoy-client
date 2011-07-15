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
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import com.servoy.j2db.FlattenedSolution;


/**
 * @author jcompagner
 */
public class FlattenedForm extends Form implements IFlattenedPersistWrapper<Form>
{
	private final FlattenedSolution flattenedSolution;
	private final Form form;

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

	public Form getWrappedPersist()
	{
		return form;
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

		copyPropertiesMap(form.getPropertiesMap(), false);

		List<Integer> existingIDs = new ArrayList<Integer>();
		for (Form f : allForms)
		{
			for (IPersist ip : f.getAllObjectsAsList())
			{
				Integer extendsID = new Integer(((AbstractBase)ip).getExtendsID());
				if (!existingIDs.contains(new Integer(ip.getID())) && !existingIDs.contains(extendsID))
				{
					if (((AbstractBase)ip).isOverrideOrphanElement())
					{
						// some deleted element
						continue;
					}
					boolean addScriptMethod = (ip instanceof ScriptMethod && getScriptMethod(((ScriptMethod)ip).getName()) == null);
					boolean addScriptVariable = (ip instanceof ScriptVariable && getScriptVariable(((ScriptVariable)ip).getName()) == null);
					boolean addOtherElement = (!(ip instanceof Part) && !(ip instanceof ScriptMethod) && !(ip instanceof ScriptVariable));
					if (addScriptVariable || addScriptMethod || addOtherElement)
					{
						if (ip instanceof TabPanel)
						{
							internalAddChild(new FlattenedTabPanel((TabPanel)ip));
						}
						else if (ip instanceof Portal)
						{
							internalAddChild(new FlattenedPortal((Portal)ip));
						}
						else
						{
							internalAddChild(ip);
						}
					}
				}
				if (((AbstractBase)ip).isOverrideElement() && !existingIDs.contains(extendsID))
				{
					existingIDs.add(extendsID);
				}
			}
		}

		Part prevPart = null;
		Collections.reverse(allForms); // change from sub-first to super-first
		for (Form f : allForms)
		{
			// Add parts
			Iterator<Part> parts = f.getParts();
			while (parts.hasNext())
			{
				// Sub-forms can only add parts to the bottom
				Part part = parts.next();
				if (part.isOverrideElement())
				{
					Part parentPart = null;
					Iterator<Part> it = getParts();
					while (it.hasNext())
					{
						Part temp = it.next();
						if (temp.getID() == part.getExtendsID() || temp.getExtendsID() == part.getExtendsID())
						{
							parentPart = temp;
							break;
						}
					}
					if (parentPart != null)
					{
						internalAddChild(part);
						internalRemoveChild(parentPart);
					}
				}
				else
				{
					if (prevPart == null || prevPart.getPartType() < part.getPartType() || (prevPart.getPartType() == part.getPartType() && part.canBeMoved()))
					{
						internalAddChild(part);
						prevPart = part;
					}
				}
			}
		}

		setSize(checkParts(getParts(), getSize())); // recalculate height
	}

	@Override
	public Iterator<IFormElement> getFormElementsSortedByFormIndex()
	{
		return new FormTypeIterator(getAllObjectsAsList(), new Comparator<IFormElement>()
		{
			public int compare(IFormElement element1, IFormElement element2)
			{
				Form form1 = (Form)element1.getParent();
				Form form2 = (Form)element2.getParent();
				// first sort on the hierarchy, elements of super-forms are sorted before elements of sub-forms
				if (form1 != form2)
				{
					return flattenedSolution.getFormHierarchy(form1).contains(form2) ? 1 : -1;
				}
				return element1.getFormIndex() - element2.getFormIndex();
			}
		});
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
