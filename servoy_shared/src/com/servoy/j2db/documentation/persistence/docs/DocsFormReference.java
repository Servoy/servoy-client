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

package com.servoy.j2db.documentation.persistence.docs;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.FormReference;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.util.UUID;

/**
 * @author lvostinar
 *
 */
@ServoyDocumented(category = ServoyDocumented.DESIGNTIME, publicName = "Form Reference", scriptingName = "FormReference", realClass = FormReference.class, typeCode = IRepository.FORMREFERENCE)
@ServoyClientSupport(ng = true, sc = false, wc = false)
public class DocsFormReference extends FormReference
{
	private static final long serialVersionUID = 1L;


	protected DocsFormReference(ISupportChilds parent, int element_id, UUID uuid)
	{
		super(parent, element_id, uuid);
	}

}
