/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

import java.util.Map;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IRepository;

/**
 * A <b>form</b> is an object that provides an UI and/or contains business logic; it's the basic user interface object in Servoy.<br/>
 * The UI of a Form is built up of Form Parts, which in turn contain Elements (and layout containers in case of advanced layout).<br/><br/>
 *
 * Forms are also a unit of scope in Servoy, meaning that forms can have variables and methods attributed to them.
 *
 * @author acostache
 *
 */
@ServoyDocumented(category = ServoyDocumented.DESIGNTIME, publicName = "Form", scriptingName = "Form", realClass = Form.class, typeCode = IRepository.FORMS)
@ServoyClientSupport(mc = true, sc = true, wc = true)
public class DocsForm extends Form implements IBaseDocsComponent
{

	private static final long serialVersionUID = 1L;

	DocsForm()
	{
		super(null, null);
	}

	@ServoyClientSupport(mc = false, wc = true, sc = true)
	@Override
	public Map<String, Object> getDesignTimeProperties()
	{
		return null;
	}

	@Override
	public void setDesignTimeProperties(Map<String, Object> map)
	{
	}

}
