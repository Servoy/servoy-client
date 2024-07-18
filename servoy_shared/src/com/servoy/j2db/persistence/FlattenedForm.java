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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;


/**
 * @author jcompagner
 */
public class FlattenedForm extends Form implements IFlattenedPersistWrapper<Form>
{

	private static final long serialVersionUID = 1L;

	private final Map<UUID, IPersist> extendsMap = new HashMap<>();
	private final Map<String, Object> allProperties = new HashMap<>();

	public static final Comparator<IFormElement> FORM_INDEX_WITH_HIERARCHY_COMPARATOR = new Comparator<IFormElement>()
	{
		public int compare(IFormElement element1, IFormElement element2)
		{
			Form form1 = getFormIndexContext(element1);
			Form form2 = getFormIndexContext(element2);
			// first sort on the hierarchy, elements of super-forms are sorted before elements of sub-forms
			if (!Utils.equalObjects(form1, form2))
			{
				boolean isChildForm = hasFormInHierarchy(form1, form2);
				if (isChildForm == hasFormInHierarchy(form2, form1))
				{
					// how can this happen, transitivity is not respected
					Debug.error("Cannot sort elements, transitivity is not respected for forms:" + form1 + " and " + form2);
				}
				return isChildForm ? 1 : -1;
			}
			return element1.getFormIndex() - element2.getFormIndex();
		}
	};

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
	 * @return the form
	 */
	public Form getForm()
	{
		return form;
	}

	public static Form getWrappedForm(Form form)
	{
		return form instanceof FlattenedForm ? ((FlattenedForm)form).getForm() : form;
	}

	@Override
	public void setExtendsID(int arg)
	{
		// override the Form.setExtendsID so that the persist fire is not happening
		// and the extends form is not get right aways.
		setTypedProperty(StaticContentSpecLoader.PROPERTY_EXTENDSID, arg);
	}

	@Override
	public Form getExtendsForm()
	{
		// if the extends form is asked for now look it up if there is one
		if (getExtendsID() > 0)
		{
			return flattenedSolution.getForm(getExtendsID());
		}
		return null;
	}

	@Override
	public Map<String, Object> getFlattenedPropertiesMap()
	{
		return new HashMap<String, Object>(allProperties);
	}

	private void fill()
	{
		List<Form> allForms = flattenedSolution.getFormHierarchy(form);

		copyPropertiesMap(form.getPropertiesMap(), false);

		// caches for the duplicate (over the form hierarchy) methods/variables, so that only
		// the first method based on its name is added.
		Set<String> methods = new HashSet<String>(64);
		Set<String> variables = new HashSet<String>(64);
		List<Integer> existingIDs = new ArrayList<Integer>();
		//first fill in the map to be sure is complete, can we improve this ?
		for (Form f : allForms)
		{
			boolean responsiveLayout = f.isResponsiveLayout();
			for (IPersist persist : f.getAllObjectsAsList())
			{
				if (persist instanceof ISupportExtendsID && ((ISupportExtendsID)persist).getExtendsID() > 0 && responsiveLayout)
				{
					IPersist p = PersistHelper.getSuperPersist((ISupportExtendsID)persist);
					if (p != null)
					{
						extendsMap.put(p.getUUID(), persist);
					}
				}
			}
		}
		for (Form f : allForms)
		{
			boolean responsiveLayout = f.isResponsiveLayout();
			for (IPersist persist : f.getAllObjectsAsList())
			{
				if (persist instanceof ISupportExtendsID && ((ISupportExtendsID)persist).getExtendsID() > 0 && responsiveLayout)
				{
					IPersist p = PersistHelper.getSuperPersist((ISupportExtendsID)persist);
					if (p != null && !p.getParent().getUUID().equals(getUUID()))
					{
						IPersist topPersist = p;
						while (((ISupportExtendsID)topPersist).getExtendsID() > 0)
						{
							IPersist superPersist = PersistHelper.getSuperPersist((ISupportExtendsID)topPersist);
							if (superPersist != null)
							{
								topPersist = superPersist;
							}
							else
							{
								Debug.error("Persist: " + topPersist + " has a super persist set, but that is not found");
								break;
							}
						}
						// only skip it if is override from other place in hierarchy; if real top container use it
						if (!(topPersist.getParent() instanceof Form)) continue;
					}
				}
				IPersist ip = extendsMap.containsKey(persist.getUUID()) ? extendsMap.get(persist.getUUID()) : persist;
				Integer extendsID = (ip instanceof ISupportExtendsID) ? new Integer(((ISupportExtendsID)ip).getExtendsID()) : Integer.valueOf(-1);
				if (!existingIDs.contains(new Integer(ip.getID())) && !existingIDs.contains(extendsID))
				{
					if (ip instanceof ISupportExtendsID && PersistHelper.isOverrideOrphanElement((ISupportExtendsID)ip))
					{
						// some deleted element
						continue;
					}
					boolean addScriptMethod = (ip instanceof ScriptMethod && methods.add(((ScriptMethod)ip).getName()));
					boolean addScriptVariable = (ip instanceof ScriptVariable && variables.add(((ScriptVariable)ip).getName()));
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
						else if (ip instanceof CSSPositionLayoutContainer)
						{
							internalAddChild(new FlattenedCSSPositionLayoutContainer(this, (CSSPositionLayoutContainer)ip));
						}
						else if (ip instanceof LayoutContainer)
						{
							internalAddChild(new FlattenedLayoutContainer(this, (LayoutContainer)ip));
						}
						else
						{
							internalAddChild(ip);
						}
					}
				}
				if (PersistHelper.isOverrideElement(ip) && !existingIDs.contains(extendsID))
				{
					existingIDs.add(extendsID);
				}
			}
		}

		Part prevPart = null;
		Collections.reverse(allForms); // change from sub-first to super-first
		for (Form f : allForms)
		{
			allProperties.putAll(f.getPropertiesMap());
			// Add parts
			Iterator<Part> parts = f.getParts();
			while (parts.hasNext())
			{
				// Sub-forms can only add parts to the bottom
				Part part = parts.next();
				if (PersistHelper.isOverrideElement(part))
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

		setSize(getSize()); // recalculate height, getSize() will check the parts (is this call needed? getsize will already recalculate)
	}

	// need to override getSize() because that checks the extended parts again, which shouldn't happen for a FF.
	@Override
	public Dimension getSize()
	{
		return checkParts(getParts(), getTypedProperty(StaticContentSpecLoader.PROPERTY_SIZE));
	}

	@Override
	public Iterator<IFormElement> getFormElementsSortedByFormIndex()
	{
		return new FormTypeIterator(getAllObjectsAsList(), FORM_INDEX_WITH_HIERARCHY_COMPARATOR);
	}

	/**
	 * Called only in develop time.
	 */
	public void reload()
	{
		internalClearAllObjects();
		extendsMap.clear();
		allProperties.clear();
		fill();
	}

	public static boolean hasFormInHierarchy(Form form1, Form form2)
	{
		Form superForm = form1.getExtendsForm();
		while (superForm != null)
		{
			if (superForm.getID() == form2.getID()) return true;
			superForm = superForm.getExtendsForm();
		}
		return false;
	}

	private static Form getFormIndexContext(IFormElement element)
	{
		if (element instanceof IFlattenedPersistWrapper)
		{
			element = (IFormElement)((IFlattenedPersistWrapper)element).getWrappedPersist();
		}
		if (element.getFormIndex() == 0 || element.getExtendsID() <= 0)
		{
			return (Form)element.getAncestor(IRepository.FORMS);
		}
		IFormElement currentElement = element;
		while (currentElement != null)
		{
			if (((AbstractBase)currentElement).hasProperty(StaticContentSpecLoader.PROPERTY_FORMINDEX.getPropertyName()))
			{
				return (Form)currentElement.getParent();
			}
			currentElement = (IFormElement)PersistHelper.getSuperPersist(currentElement);
		}
		return (Form)element.getAncestor(IRepository.FORMS);
	}

	Map<UUID, IPersist> getExtendsMap()
	{
		return extendsMap;
	}

}
