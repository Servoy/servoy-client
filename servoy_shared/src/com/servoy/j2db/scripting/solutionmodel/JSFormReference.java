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

package com.servoy.j2db.scripting.solutionmodel;

import java.awt.Dimension;
import java.awt.Point;

import org.mozilla.javascript.annotations.JSGetter;
import org.mozilla.javascript.annotations.JSSetter;

import com.servoy.base.solutionmodel.IBaseSMForm;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormReference;

/**
 * @author lvostinar
 *
 */
@SuppressWarnings("nls")
@ServoyDocumented(category = ServoyDocumented.RUNTIME, scriptingName = "JSFormReference")
public class JSFormReference implements IJSParent<FormReference>
{
	private final IApplication application;
	protected FormReference formReference;
	private final IJSParent< ? > parent;
	private boolean isCopy = false;

	public JSFormReference(IJSParent< ? > parent, IApplication application, FormReference formReference)
	{
		this.application = application;
		this.formReference = formReference;
		this.parent = parent;
	}

	public FormReference getSupportChild()
	{
		return formReference;
	}

	public IJSParent< ? > getJSParent()
	{
		return parent;
	}

	@Override
	public void checkModification()
	{
		parent.checkModification();
		if (!isCopy)
		{
			// then get the replace the item with the item of the copied relation.
			formReference = (FormReference)parent.getSupportChild().getChild(formReference.getUUID());
			isCopy = true;
		}
	}

	/**
	 * Get/Set the Form Reference contained form.
	 *
	 * @sample
	 * formReference.containsForm = someForm;
	 */
	@JSGetter
	public JSForm getContainsForm()
	{
		Form form = application.getFlattenedSolution().getForm(formReference.getContainsFormID());
		if (form != null)
		{
			return application.getScriptEngine().getSolutionModifier().instantiateForm(form, false);
		}
		return null;
	}

	@JSSetter
	public void setContainsForm(IBaseSMForm form)
	{
		checkModification();
		if (form == null)
		{
			formReference.setContainsFormID(0);
		}
		else
		{
			JSForm jsForm = (JSForm)form;
			if (!formReference.canAddFormReference(application.getFlattenedSolution(), jsForm.form))
			{
				throw new RuntimeException("Cannot set contains form to " + form.getName() + ", it leads to form reference cycle.");
			}
			formReference.setContainsFormID(jsForm.getSupportChild().getID());
		}
	}

	/**
	 * Get/Set the Form Reference name.
	 *
	 * @sample
	 * formReference.name = 'myreference';
	 */
	@JSGetter
	public String getName()
	{
		return formReference.getName();
	}

	@JSSetter
	public void setName(String arg)
	{
		checkModification();
		formReference.setName(arg);
	}

	/**
	 * Get/Set the Form Reference x location.
	 * @sample
	 * formReference.x = 100;
	 */
	@JSGetter
	public int getX()
	{
		return formReference.getLocation().x;
	}

	@JSSetter
	public void setX(int x)
	{
		checkModification();
		formReference.setLocation(new Point(x, formReference.getLocation().y));
	}

	/**
	 * Get/Set the Form Reference y location.
	 * @sample
	 * formReference.y = 100;
	 */
	@JSGetter
	public int getY()
	{
		return formReference.getLocation().y;
	}

	@JSSetter
	public void setY(int y)
	{
		checkModification();
		formReference.setLocation(new Point(formReference.getLocation().x, y));
	}

	/**
	 * The width in pixels of the form reference.
	 *
	 * @sample
	 * formReference.width = 100;
	 */
	@JSGetter
	public int getWidth()
	{
		return formReference.getSize().width;
	}

	@JSSetter
	public void setWidth(int width)
	{
		checkModification();
		formReference.setSize(new Dimension(width, formReference.getSize().height));
	}

	/**
	 * The height in pixels of the form reference.
	 *
	 * @sample
	 * formReference.height = 100;
	 */
	@JSGetter
	public int getHeight()
	{
		return formReference.getSize().height;
	}

	@JSSetter
	public void setHeight(int height)
	{
		checkModification();
		formReference.setSize(new Dimension(formReference.getSize().width, height));
	}
}
