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

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.Bean;
import com.servoy.j2db.scripting.IJavaScriptType;

/**
 * @author lvostinar
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class JSBean extends JSComponent<Bean> implements IJavaScriptType
{

	/**
	 * @param parent
	 * @param baseComponent
	 * @param isNew
	 */
	public JSBean(IJSParent parent, Bean baseComponent, boolean isNew)
	{
		super(parent, baseComponent, isNew);
	}

	/**
	 * The bean class name.
	 * 
	 * @sample
	 * var bean = form.getBean('mybean');
	 * application.output(bean.className);
	 */
	public String js_getClassName()
	{
		return getBaseComponent(false).getBeanClassName();
	}

	public void js_setClassName(String className)
	{
		getBaseComponent(true).setBeanClassName(className);
	}

	/**
	 * @sameas com.servoy.j2db.scripting.solutionmodel.JSComponent#js_getBackground()
	 */
	@Override
	@Deprecated
	public String js_getBackground()
	{
		return super.js_getBackground();
	}

	/**
	 * @sameas com.servoy.j2db.scripting.solutionmodel.JSComponent#js_getBorderType()
	 */
	@Override
	@Deprecated
	public String js_getBorderType()
	{
		return super.js_getBorderType();
	}

	/**
	 * @sameas com.servoy.j2db.scripting.solutionmodel.JSComponent#js_getFontType()
	 */
	@Override
	@Deprecated
	public String js_getFontType()
	{
		return super.js_getFontType();
	}

	/**
	 * @sameas com.servoy.j2db.scripting.solutionmodel.JSComponent#js_getForeground()
	 */
	@Override
	@Deprecated
	public String js_getForeground()
	{
		// TODO Auto-generated method stub
		return super.js_getForeground();
	}

	/**
	 * @sameas com.servoy.j2db.scripting.solutionmodel.JSComponent#js_getPrintSliding()
	 */
	@Override
	@Deprecated
	public int js_getPrintSliding()
	{
		return super.js_getPrintSliding();
	}

	/**
	 * @sameas com.servoy.j2db.scripting.solutionmodel.JSComponent#js_getStyleClass()
	 */
	@Override
	@Deprecated
	public String js_getStyleClass()
	{
		return super.js_getStyleClass();
	}

	/**
	 * @sameas com.servoy.j2db.scripting.solutionmodel.JSComponent#js_getTransparent()
	 */
	@Override
	@Deprecated
	public boolean js_getTransparent()
	{
		return super.js_getTransparent();
	}

	@SuppressWarnings("nls")
	@Override
	public String toString()
	{
		return "JSBean[name:" + js_getName() + ",classname:" + js_getClassName() + ']';
	}
}
