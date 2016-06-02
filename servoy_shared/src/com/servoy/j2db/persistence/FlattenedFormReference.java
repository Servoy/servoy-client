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
import java.util.ArrayList;
import java.util.List;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.StaticContentSpecLoader.TypedProperty;
import com.servoy.j2db.util.PersistHelper;

/**
 * @author lvostinar
 *
 */
public class FlattenedFormReference extends FormReference implements IFlattenedPersistWrapper<FormReference>
{
	private static final long serialVersionUID = 1L;

	private final FormReference formReference;
	private final FlattenedSolution flattenedSolution;

	public FlattenedFormReference(FlattenedSolution flattenedSolution, FormReference formReference)
	{
		super(formReference.getParent(), formReference.getID(), formReference.getUUID());
		this.formReference = formReference;
		this.flattenedSolution = flattenedSolution;
		fill();
	}

	@Override
	public FormReference getWrappedPersist()
	{
		return formReference;
	}

	private void fill()
	{
		internalClearAllObjects();
		List<IPersist> children = new ArrayList<IPersist>();//PersistHelper.getHierarchyChildren(formReference);
		if (formReference.getContainsFormID() > 0)
		{
			Form referenceForm = flattenedSolution.getForm(formReference.getContainsFormID());
			if (referenceForm != null)
			{
				referenceForm = flattenedSolution.getFlattenedForm(referenceForm);
				if (referenceForm != null)
				{
					children.addAll(referenceForm.getAllObjectsAsList());
				}
			}
		}
		for (IPersist realChild : formReference.getAllObjectsAsList())
		{
			if (realChild instanceof IFormElement || realChild instanceof AbstractContainer)
			{
				children.add(realChild);
				IPersist p = realChild;
				while ((p = PersistHelper.getSuperPersist((ISupportExtendsID)p)) != null)
				{
					children.remove(p);
				}
			}
		}
		for (IPersist child : children)
		{
			if (child instanceof LayoutContainer && ((LayoutContainer)child).getExtendsID() > 0)
			{
				internalAddChild(new FlattenedLayoutContainer(flattenedSolution, (LayoutContainer)child));
			}
			else if (child instanceof FormReference)
			{
				internalAddChild(new FlattenedFormReference(flattenedSolution, (FormReference)child));
			}
			else if (child instanceof IFormElement || child instanceof AbstractContainer)
			{
				internalAddChild(child);
			}
		}
	}

	@Override
	protected void internalRemoveChild(IPersist obj)
	{
		formReference.internalRemoveChild(obj);
		fill();
	}

	@Override
	public void addChild(IPersist obj)
	{
		formReference.addChild(obj);
		fill();
	}

	@Override
	public Field createNewField(Point location) throws RepositoryException
	{
		return formReference.createNewField(location);
	}

	@Override
	public GraphicalComponent createNewGraphicalComponent(Point location) throws RepositoryException
	{
		return formReference.createNewGraphicalComponent(location);
	}

	@Override
	<T> T getTypedProperty(TypedProperty<T> property)
	{
		return formReference.getTypedProperty(property);
	}

	@Override
	<T> void setTypedProperty(TypedProperty<T> property, T value)
	{
		formReference.setTypedProperty(property, value);
	}

	@Override
	public int hashCode()
	{
		return formReference.hashCode();
	}

	@Override
	public List<IPersist> getHierarchyChildren()
	{
		return getAllObjectsAsList();
	}
}
