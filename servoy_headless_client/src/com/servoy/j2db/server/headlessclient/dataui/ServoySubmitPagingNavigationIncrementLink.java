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
package com.servoy.j2db.server.headlessclient.dataui;

import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IFormSubmittingComponent;
import org.apache.wicket.markup.html.navigation.paging.IPageable;
import org.apache.wicket.markup.html.navigation.paging.PagingNavigationIncrementLink;
import org.apache.wicket.util.string.PrependingStringBuffer;

import com.servoy.j2db.server.headlessclient.ServoyForm;

/**
 * The none ajax paging increment link (>) of a {@link WebCellBasedView}
 * 
 * @author jcompagner
 */
public class ServoySubmitPagingNavigationIncrementLink extends PagingNavigationIncrementLink implements IFormSubmittingComponent
{
	private static final long serialVersionUID = 1L;
	private Form form;

	/**
	 * @param id
	 * @param pageable
	 * @param increment
	 */
	public ServoySubmitPagingNavigationIncrementLink(String id, IPageable pageable, int increment)
	{
		super(id, pageable, increment);
	}

	/**
	 * @see org.apache.wicket.markup.html.form.IFormSubmittingComponent#getDefaultFormProcessing()
	 */
	public boolean getDefaultFormProcessing()
	{
		return true;
	}

	/**
	 * @see org.apache.wicket.markup.html.form.IFormSubmittingComponent#getForm()
	 */
	public Form getForm()
	{
		if (this.form != null)
		{
			return this.form;
		}
		else
		{
			return findParent(ServoyForm.class);
		}
	}

	/**
	 * @see org.apache.wicket.markup.html.form.IFormSubmittingComponent#getInputName()
	 */
	public String getInputName()
	{

		// TODO: This is a copy & paste from the FormComponent class. 
		String id = getId();
		final PrependingStringBuffer inputName = new PrependingStringBuffer(id.length());
		Component c = this;
		while (true)
		{
			inputName.prepend(id);
			c = c.getParent();
			if (c == null || (c instanceof Form && ((Form)c).isRootForm()) || c instanceof Page)
			{
				break;
			}
			inputName.prepend(Component.PATH_SEPARATOR);
			id = c.getId();
		}
		return inputName.toString();
	}

	/**
	 * @see org.apache.wicket.markup.html.form.IFormSubmittingComponent#onSubmit()
	 */
	public void onSubmit()
	{
		onClick();
	}

	/**
	 * @inheritDoc
	 * @see org.apache.wicket.Component#onComponentTag(org.apache.wicket.markup.ComponentTag)
	 */
	@Override
	protected void onComponentTag(ComponentTag tag)
	{
		super.onComponentTag(tag);
		// If we're disabled
		if (!isLinkEnabled())
		{
			disableLink(tag);
		}
		else
		{
			if (tag.getName().equalsIgnoreCase("a"))
			{
				tag.put("href", "#");
			}
			tag.put("onclick", getTriggerJavaScript());
		}
	}

	/**
	 * The javascript which trigges this link.
	 * 
	 * TODO: This is a copy & paste from Button
	 * 
	 * @return The javascript
	 */
	protected final String getTriggerJavaScript()
	{
		if (getForm() != null)
		{
			// find the root form - the one we are really going to submit
			ServoyForm root = (ServoyForm)getForm().getRootForm();
			StringBuffer sb = new StringBuffer(100);
			sb.append("var e=document.getElementById('");
			sb.append(root.getHiddenField());
			sb.append("'); e.name=\'");
			sb.append(getInputName());
			sb.append("'; e.value='x';");
			sb.append("var f=document.getElementById('");
			sb.append(root.getMarkupId());
			sb.append("');");
			if (true)
			{
				if (getForm() != root)
				{
					sb.append("var ff=document.getElementById('");
					sb.append(getForm().getMarkupId());
					sb.append("');");
				}
				else
				{
					sb.append("var ff=f;");
				}
				sb.append("if (ff.onsubmit != undefined) { if (ff.onsubmit()==false) return false; }");
			}
			sb.append("f.submit();e.value='';e.name='';return false;");
			return sb.toString();
		}
		else
		{
			return null;
		}
	}
}
