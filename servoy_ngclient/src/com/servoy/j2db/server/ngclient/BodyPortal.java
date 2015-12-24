/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

package com.servoy.j2db.server.ngclient;

import com.servoy.j2db.FormController;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.Portal;

/**
 * Special portal component that is used to render body part (can be tableview or listview).
 * @author gboros
 *
 */
public class BodyPortal extends Portal
{

	private static final long serialVersionUID = 1L;

	private final Form form;
	private final boolean tableview;

	public BodyPortal(Form form)
	{
		super(null, 0, null);
		this.form = form;
		this.tableview = (form.getView() == FormController.TABLE_VIEW || form.getView() == FormController.LOCKED_TABLE_VIEW);
	}

	public Form getForm()
	{
		return form;
	}

	@Override
	public boolean equals(Object o)
	{
		if (form == null) return ((o instanceof BodyPortal) && (((BodyPortal)o).getForm() == null));
		if (o instanceof BodyPortal) return form.equals(((BodyPortal)o).getForm());
		return false;
	}

	public boolean isTableview()
	{
		return tableview;
	}

	@Override
	public int hashCode()
	{
		return form == null ? 0 : form.hashCode();
	}
}
