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
package com.servoy.j2db.scripting.solutionmodel;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportExtendsID;
import com.servoy.j2db.solutionmodel.ISMHasUUID;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.UUID;

/**
 * @author jcompagner
 *
 * @param <T>
 *
 */
public class JSBase<T extends AbstractBase> implements ISMHasUUID
{
	private T baseComponent;
	private final IJSParent< ? > parent;
	private boolean isCopy;

	public JSBase(IJSParent< ? > parent, T baseComponent, boolean isNew)
	{
		this.parent = parent;
		this.baseComponent = baseComponent;
		this.isCopy = isNew;
	}

	@SuppressWarnings("unchecked")
	public final T getBaseComponent(boolean forModification)
	{
		if (forModification)
		{
			checkModification();
		}
		else if (!isCopy)
		{
			// as long as the component is not already a copy, we have to get the real one
			// so that changes to other instances of this componnet that points to the same persist instance are seen in this one.
			T child = (T)parent.getSupportChild().getChild(baseComponent.getUUID());
			baseComponent = child != null ? child : baseComponent;
		}
		return baseComponent;
	}

	public IJSParent< ? > getJSParent()
	{
		return parent;
	}

	/**
	 * Returns the UUID of this component.
	 *
	 * @sample
	 * var button_uuid = solutionModel.getForm("my_form").getButton("my_button").getUUID();
	 * application.output(button_uuid.toString());
	 */
	@JSFunction
	public UUID getUUID()
	{
		return baseComponent.getUUID();
	}

	@SuppressWarnings("unchecked")
	public final void checkModification()
	{
		// first call parent, so the parent will make a copy.
		parent.checkModification();
		AbstractBase tempPersist = baseComponent;
		if (!isCopy)
		{
			// then get the replace the item with the item of the copied relation.
			baseComponent = (T)parent.getSupportChild().getChild(baseComponent.getUUID());
			isCopy = true;
		}
		baseComponent = (T)getOverridePersistIfNeeded(tempPersist, baseComponent, getJSParent());
	}

	public static AbstractBase getOverridePersistIfNeeded(AbstractBase persist, AbstractBase finalPersist, IJSParent parent)
	{
		AbstractBase baseComponent = finalPersist;
		if (persist != null && persist.getAncestor(IRepository.FORMS) != null)
		{
			IJSParent< ? > jsparent = parent;
			while (jsparent != null)
			{
				if (jsparent.getSupportChild() instanceof Form) break;
				jsparent = jsparent.getJSParent();
			}
			if (jsparent != null && jsparent.getSupportChild() instanceof Form &&
				!jsparent.getSupportChild().getUUID().equals(persist.getAncestor(IRepository.FORMS).getUUID()))
			{
				// inherited persist
				try
				{
					IPersist parentPersist = persist;
					while (PersistHelper.getSuperPersist((ISupportExtendsID)parentPersist) != null)
					{
						parentPersist = PersistHelper.getSuperPersist((ISupportExtendsID)parentPersist);
					}
					baseComponent = (AbstractBase)persist.cloneObj(parent.getSupportChild(), false, null, false, false, false);
					baseComponent.copyPropertiesMap(null, true);
					((ISupportExtendsID)baseComponent).setExtendsID(parentPersist.getID());
				}
				catch (Exception ex)
				{
					Debug.error(ex);
				}
			}
		}
		return baseComponent;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((baseComponent == null) ? 0 : baseComponent.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		JSBase< ? > other = (JSBase< ? >)obj;
		if (baseComponent == null)
		{
			if (other.baseComponent != null) return false;
		}
		else if (!baseComponent.getUUID().equals(other.baseComponent.getUUID())) return false;
		return true;
	}
}
