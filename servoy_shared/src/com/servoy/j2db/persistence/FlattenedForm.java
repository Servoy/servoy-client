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
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.ContentSpec.Element;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.JSONWrapperMap;
import com.servoy.j2db.util.Utils;


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
	 * @throws RepositoryException
	 */
	public FlattenedForm(FlattenedSolution flattenedSolution, Form form) throws RepositoryException
	{
		super(form.getParent(), form.getID(), form.getUUID());
		this.flattenedSolution = flattenedSolution;
		this.form = form;

		fill();

		// name can only be set once and only from the form itself.
		this.setName(form.getName());
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
	private void fill() throws RepositoryException
	{
		List<Form> allForms = flattenedSolution.getFormHierarchy(form);
		Collections.reverse(allForms); // change from sub-first to super-first

		Part prevPart = null;
		Map<String, ScriptVariable> formVariables = new HashMap<String, ScriptVariable>();
		Map<String, ScriptMethod> formMethods = new HashMap<String, ScriptMethod>();
		int maxWidth = -1;
		for (Form f : allForms)
		{
			try
			{
				copyFormValues(f);
			}
			catch (IllegalAccessException e)
			{
				throw new RepositoryException(e);
			}
			catch (InvocationTargetException e)
			{
				throw new RepositoryException(e);
			}
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
				}
			}
			if (f.getWidth() > maxWidth) maxWidth = f.getWidth();
			// Add parts
			Iterator<Part> parts = f.getParts();
			while (parts.hasNext())
			{
				// Sub-forms can only add parts to the bottom
				Part part = parts.next();
				if (prevPart != null && (prevPart.getPartType() > part.getPartType() || (prevPart.getPartType() == part.getPartType() && !part.canBeMoved())))
				{
					// skip this part, cannot override
					continue;
				}
				prevPart = part;
				internalAddChild(part);
			}
		}

		// remove overridden form variables and methods
		Iterator<IPersist> allIt = getAllObjects();
		List<IPersist> remove = new ArrayList<IPersist>();
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
		}

		for (IPersist var : remove)
		{
			internalRemoveChild(var);
		}

		checkParts(); // recalculate height
		setWidth(maxWidth);
	}

	/**
	 * @param f
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 * @throws Exception
	 */
	private void copyFormValues(Form f) throws IllegalAccessException, InvocationTargetException
	{
		ContentSpec contentSpec = StaticContentSpecLoader.getContentSpec();
		PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
		for (PropertyDescriptor propertyDescriptor : propertyDescriptors)
		{
			if ("name".equals(propertyDescriptor.getName()) || "customProperties".equals(propertyDescriptor.getName())) //$NON-NLS-1$ //$NON-NLS-2$
			{
				continue;
			}
			Element element = contentSpec.getPropertyForObjectTypeByName(IRepository.FORMS, propertyDescriptor.getName());
			if (element != null && !element.isDeprecated() && !element.isMetaData())
			{
				Object value = propertyDescriptor.getReadMethod().invoke(f, (Object[])null);
				if (!Utils.equalObjects(value, element.getDefaultClassValue()))
				{
					propertyDescriptor.getWriteMethod().invoke(this, new Object[] { value });
				}
			}
		}
		setCustomPropertiesMap(JSONWrapperMap.mergeMaps(getCustomPropertiesMap(), f.getCustomPropertiesMap()));
	}

	@Override
	public Iterator<IPersist> getAllObjectsSortedByFormIndex()
	{
		return new FormTypeIterator(getAllObjectsAsList(), new Comparator<IPersist>()
		{
			public int compare(IPersist persist1, IPersist persist2)
			{
				Form form1 = (Form)persist1.getParent();
				Form form2 = (Form)persist2.getParent();
				// first sort on the hierarchy, elements of super-forms are sorted before elements of sub-forms
				if (form1 != form2)
				{
					return flattenedSolution.getFormHierarchy(form1).contains(form2) ? 1 : -1;
				}
				if (persist1 instanceof IFormElement && persist2 instanceof IFormElement)
				{
					return ((IFormElement)persist1).getFormIndex() - ((IFormElement)persist2).getFormIndex();
				}
				return 0;
			}
		});
	}

	/**
	 * Called only in develop time.
	 */
	public void reload()
	{
		internalClearAllObjects();
		try
		{
			fill();
		}
		catch (RepositoryException e)
		{
			Debug.error(e);
		}
	}
}
