/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2016 Servoy BV

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

import java.awt.Point;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.util.UUID;

/**
 * @author lvostinar
 *
 */
public class FormReference extends AbstractContainer implements IFormElement, IContainsFormID
{

	protected FormReference(ISupportChilds parent, int element_id, UUID uuid)
	{
		super(IRepository.FORMREFERENCE, parent, element_id, uuid);
	}

	/**
	 * Set the containsFormID
	 *
	 * @param arg the containsFormID
	 */
	public void setContainsFormID(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_CONTAINSFORMID, arg);
	}

	/**
	 * @clonedesc com.servoy.base.solutionmodel.IBaseSMTab#getContainsForm()
	 */
	public int getContainsFormID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_CONTAINSFORMID).intValue();
	}

	public void setLocation(java.awt.Point arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_LOCATION, arg);
	}

	public java.awt.Point getLocation()
	{
		java.awt.Point point = getTypedProperty(StaticContentSpecLoader.PROPERTY_LOCATION);
		if (point == null)
		{
			point = new Point(10, 10);
		}
		return point;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IFormElement#setFormIndex(int)
	 */
	@Override
	public void setFormIndex(int arg)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IFormElement#getFormIndex()
	 */
	@Override
	public int getFormIndex()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IFormElement#getGroupID()
	 */
	@Override
	public String getGroupID()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IFormElement#setGroupID(java.lang.String)
	 */
	@Override
	public void setGroupID(String arg)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IFormElement#getLocked()
	 */
	@Override
	public boolean getLocked()
	{
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IFormElement#setLocked(boolean)
	 */
	@Override
	public void setLocked(boolean arg)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IFormElement#getVisible()
	 */
	@Override
	public boolean getVisible()
	{
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IFormElement#setVisible(boolean)
	 */
	@Override
	public void setVisible(boolean arg)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IFormElement#getEnabled()
	 */
	@Override
	public boolean getEnabled()
	{
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IFormElement#setEnabled(boolean)
	 */
	@Override
	public void setEnabled(boolean arg)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public String toString()
	{
		String name = getName();
		return name == null ? "FormReference" : name;
	}

	public static Set<String> detectFormReferenceCycles(FlattenedSolution flattenedSolution, Form form)
	{
		if (form == null) return new HashSet<String>();
		return detectFormReferenceCycles(flattenedSolution, form, new HashSet<Form>(), form.getName());
	}

	private static Set<String> detectFormReferenceCycles(FlattenedSolution flattenedSolution, Form form, Set<Form> forms, String path)
	{
		Set<String> paths = new HashSet<String>();
		forms.add(form);
		Iterator<FormReference> refs = form.getFormReferences();
		while (refs.hasNext())
		{
			FormReference ref = refs.next();
			if (ref.getContainsFormID() > 0)
			{
				Form containedForm = flattenedSolution.getForm(ref.getContainsFormID());
				path = path + "-> references '" + containedForm.getName() + "'";
				if (forms.contains(containedForm))
				{
					paths.add(path);
					return paths;
				}
				else
				{
					paths.addAll(detectFormReferenceCycles(flattenedSolution, containedForm, new HashSet<Form>(forms), path));
				}

			}
		}

		Form f = form;
		while (f.getExtendsForm() != null)
		{
			path = path + " ->extends '" + f.getExtendsForm().getName() + "'";
			if (forms.contains(f))
			{
				if (form != f)
				{
					paths.add(path);
					return paths;
				}
			}
			else
			{
				paths.addAll(detectFormReferenceCycles(flattenedSolution, f.getExtendsForm(), new HashSet<Form>(forms), path));
			}

			f = f.getExtendsForm();
		}
		return paths;
	}

	public boolean canAddFormReference(final FlattenedSolution flattenedSolution, Form toAdd)
	{
		final Form form = (Form)getAncestor(IRepository.FORMS);
		final Set<Form> exclude = new HashSet<Form>();
		flattenedSolution.getFlattenedForm(toAdd).acceptVisitor(new IPersistVisitor()
		{
			@Override
			public Object visit(IPersist o)
			{
				if (o instanceof FormReference && ((FormReference)o).getContainsFormID() > 0)
				{
					Form containedForm = flattenedSolution.getForm(((FormReference)o).getContainsFormID());
					exclude.add(containedForm);
				}
				return IPersistVisitor.CONTINUE_TRAVERSAL;
			}
		});
		Iterator<Form> forms = flattenedSolution.getForms(false);
		while (forms.hasNext())
		{
			Form possibleParentForm = forms.next();
			if (flattenedSolution.getFormHierarchy(possibleParentForm).contains(toAdd))
			{
				exclude.add(possibleParentForm);
			}
		}
		return !exclude.contains(form);
	}
}
