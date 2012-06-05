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
package com.servoy.j2db.scripting.info;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.scripting.IPrefixedConstantsObject;

/**
 * @author jcompagner
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class WEBCONSTANTS implements IPrefixedConstantsObject
{

	/**
	 * Property that can be set using application.putClientProperty(), it sets the servoy_web_client_default.css location directory in the templates dir for this client
	 *
	 * @sample
	 * // set this if you want to set the css path to the servoy_web_client_default.css file that you can specify per client
	 * // by default this file resides in '/servoy-webclient/templates/default/servoy_web_client_default.css'
	 * // and you will override the 'default' in that url so setting it to myclient1 will result in:
	 * // by default this file resides in '/servoy-webclient/templates/myclient1/servoy_web_client_default.css'
	 * application.putClientProperty(APP_WEB_PROPERTY.WEBCLIENT_TEMPLATES_DIR, 'myclient1');
	 */
	public static final String WEBCLIENT_TEMPLATES_DIR = "templates.dir"; //$NON-NLS-1$

	public String getPrefix()
	{
		return "APP_WEB_PROPERTY";
	}
}
