/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2025 Servoy BV

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

package com.servoy.j2db.debug;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.servoy.j2db.BasicFormController;
import com.servoy.j2db.IFormController;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.server.ngclient.NGFormManager;

public class DebugNGFormMananger extends NGFormManager implements DebugUtils.DebugUpdateFormSupport
{
	public DebugNGFormMananger(DebugNGClient app)
	{
		super(app);
	}

	public void updateForm(Form form)
	{
		boolean isNew = !possibleForms.containsValue(form);
		boolean isDeleted = false;
		if (!isNew)
		{
			isDeleted = !((AbstractBase)form.getParent()).getAllObjectsAsList().contains(form);
		}
		updateForm(form, isNew, isDeleted);
	}

	/**
	 * @param form
	 * @param isNew
	 * @param isDeleted
	 */
	private void updateForm(Form form, boolean isNew, boolean isDeleted)
	{
		if (isNew)
		{
			addForm(form, false);
		}
		else if (isDeleted)
		{
			Iterator<Entry<String, Form>> iterator = possibleForms.entrySet().iterator();
			while (iterator.hasNext())
			{
				Map.Entry<String, Form> entry = iterator.next();
				if (entry.getValue().equals(form))
				{
					iterator.remove();
					IFormController tmp = getCachedFormController(entry.getKey());
					if (tmp != null)
					{
						tmp.destroy();
						removeFormController((BasicFormController)tmp); // form was deleted in designer; remove it's controller from cached/already used forms
					}
				}
			}
		}
		else
		{
			// just changed
			if (possibleForms.get(form.getName()) == null)
			{
				// name change, first remove the form
				updateForm(form, false, true);
				// then add it back in
				updateForm(form, true, false);
			}
		}
	}
}