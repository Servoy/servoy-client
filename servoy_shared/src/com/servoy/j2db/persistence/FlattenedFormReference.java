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
import java.util.List;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.StaticContentSpecLoader.TypedProperty;
import com.servoy.j2db.util.Debug;

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
		if (formReference.getContainsFormID() > 0)
		{
			Form referenceForm = flattenedSolution.getForm(formReference.getContainsFormID());
			if (referenceForm != null)
			{
				referenceForm = flattenedSolution.getFlattenedForm(referenceForm);
				if (referenceForm != null)
				{
					for (IPersist originalElementFromReferencedForm : referenceForm.getAllObjectsAsList())
					{
						if (originalElementFromReferencedForm instanceof Part)
						{
							continue;
						}
						if (originalElementFromReferencedForm instanceof LayoutContainer &&
							((LayoutContainer)originalElementFromReferencedForm).getExtendsID() > 0)
						{
							internalAddChild(new FlattenedLayoutContainer(flattenedSolution, (LayoutContainer)originalElementFromReferencedForm));
						}
						else if (originalElementFromReferencedForm instanceof FormReference)
						{
							internalAddChild(new FlattenedFormReference(flattenedSolution, (FormReference)originalElementFromReferencedForm));
						}
						else
						{
							IPersist newPersist;
							try
							{
								String qualifier = formReference.getName();
								if (qualifier == null)
								{

									qualifier = formReference.getUUID().toString().replaceAll("-", "_");
									if (Character.isDigit(qualifier.charAt(0)))
									{
										qualifier = "_" + qualifier;
									}
								}
								List<IPersist> allObjectsAsList = formReference.getAllObjectsAsList();
								boolean alreadyAdded = false;
								for (IPersist iPersist : allObjectsAsList)
								{
									if (iPersist instanceof ISupportExtendsID)
									{
										if (((ISupportExtendsID)iPersist).getExtendsID() == originalElementFromReferencedForm.getID())
										{
											internalAddChild(iPersist);
											alreadyAdded = true;
											break;
										}
									}
								}
								if (!alreadyAdded)
								{
									newPersist = ((AbstractBase)originalElementFromReferencedForm).cloneObj(formReference, false, null, false, false, false);
									((AbstractBase)newPersist).copyPropertiesMap(null, true);
									((ISupportExtendsID)newPersist).setExtendsID(originalElementFromReferencedForm.getID());
									adjustName((AbstractBase)originalElementFromReferencedForm, (AbstractBase)newPersist, qualifier);
									internalAddChild(newPersist);
								}
							}
							catch (RepositoryException e)
							{
								Debug.error(e);
							}
						}
					}
				}
			}
		}
	}

	/**Adjusts the name of the element by prepending it with the name of the form reference
	 * @param newPersist
	 * @param newPersist2
	 */
	private void adjustName(AbstractBase originalElementFromReferencedForm, AbstractBase newPersist, String formReferenceName)
	{
		Object property = originalElementFromReferencedForm.getProperty(IContentSpecConstants.PROPERTY_NAME);
		if (property != null)
		{
			newPersist.setProperty(IContentSpecConstants.PROPERTY_NAME, formReferenceName + "_" + property.toString());
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
		fill();
		return getAllObjectsAsList();
	}
}
