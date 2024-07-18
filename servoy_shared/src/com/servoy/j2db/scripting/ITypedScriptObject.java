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
package com.servoy.j2db.scripting;

import com.servoy.j2db.documentation.ClientSupport;
import com.servoy.j2db.documentation.IObjectDocumentation;
import com.servoy.j2db.documentation.IParameter;

public interface ITypedScriptObject extends IScriptObject
{
	IParameter[] getParameters(String methodName, Class< ? >[] argTypes);

	public String getSignature(String methodName, Class< ? >[] argTypes);

	public String getJSTranslatedSignature(String methodName, Class< ? >[] argTypes);

	public String getSample(String methodName, Class< ? >[] argTypes);

	public String getSample(String methodName, ClientSupport csp);

	public String getSample(String methodName, Class< ? >[] argTypes, ClientSupport csp);

	public String getToolTip(String methodName, Class< ? >[] argTypes);

	public String getToolTip(String methodName, ClientSupport csp);

	public String getToolTip(String methodName, Class< ? >[] argTypes, ClientSupport csp);

	public boolean isDeprecated(String methodName, Class< ? >[] argTypes);

	public boolean isSpecial(String propertyName);

	public String getDeprecatedText(String methodName, Class< ? >[] argTypes);

	public abstract String getReturnDescription(String methodName, Class< ? >[] argTypes);

	public abstract Class< ? > getReturnedType(String methodName, Class< ? >[] argTypes);

	public default IObjectDocumentation getObjectDocumentation()
	{
		return null;
	}
}
