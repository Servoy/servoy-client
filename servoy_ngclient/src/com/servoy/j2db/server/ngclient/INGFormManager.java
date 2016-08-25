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

package com.servoy.j2db.server.ngclient;

import com.servoy.j2db.IBasicFormManager;
import com.servoy.j2db.persistence.Solution;

/**
 * @author jcompagner
 *
 */
public interface INGFormManager extends IBasicFormManager
{
	void makeSolutionSettings(Solution solution);

	IWebFormController getForm(String name);

	IWebFormController getCachedFormController(String formName);

	IWebFormController getCurrentForm();

	void setCurrentControllerJS(IWebFormController currentController);

	/**
	 * @param formName
	 * @return
	 */
	IWebFormController getFormAndSetCurrentWindow(String formName);
}
