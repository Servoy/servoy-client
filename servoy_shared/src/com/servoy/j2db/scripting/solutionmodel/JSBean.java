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

import org.mozilla.javascript.annotations.JSGetter;
import org.mozilla.javascript.annotations.JSSetter;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.base.solutionmodel.mobile.IMobileSMBean;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.Bean;
import com.servoy.j2db.scripting.IJavaScriptType;
import com.servoy.j2db.solutionmodel.ISMBean;

/**
 * @author lvostinar
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, extendsComponent = "JSComponent")
@ServoyClientSupport(ng = false, mc = true, wc = true, sc = true)
public class JSBean extends JSComponent<Bean> implements IJavaScriptType, ISMBean, IMobileSMBean
{
	public JSBean(IJSParent< ? > parent, Bean baseComponent, boolean isNew)
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
	@JSGetter
	@ServoyClientSupport(mc = false, wc = true, sc = true)
	public String getClassName()
	{
		return getBaseComponent(false).getBeanClassName();
	}

	@JSSetter
	@ServoyClientSupport(mc = false, wc = true, sc = true)
	public void setClassName(String className)
	{
		getBaseComponent(true).setBeanClassName(className);
	}

	/**
	 * @clonedesc com.servoy.j2db.scripting.solutionmodel.JSComponent#getBackground()
	 * @sampleas com.servoy.j2db.scripting.solutionmodel.JSComponent#getBackground()
	 *
	 * @deprecated the background is handled by the bean class implementation; this solution model property is ignored
	 */
	@Override
	@Deprecated
	@JSGetter
	public String getBackground()
	{
		return super.getBackground();
	}

	/**
	 * @clonedesc com.servoy.j2db.scripting.solutionmodel.JSComponent#getBorderType()
	 * @sampleas com.servoy.j2db.scripting.solutionmodel.JSComponent#getBorderType()
	 *
	 * @deprecated the border type is handled by the bean class implementation; this solution model property is ignored
	 */
	@Override
	@Deprecated
	@JSGetter
	public String getBorderType()
	{
		return super.getBorderType();
	}

	/**
	 * @clonedesc com.servoy.j2db.scripting.solutionmodel.JSComponent#getFontType()
	 * @sampleas com.servoy.j2db.scripting.solutionmodel.JSComponent#getFontType()
	 *
	 * @deprecated the font type is handled by the bean class implementation; this solution model property is ignored
	 */
	@Override
	@Deprecated
	@JSGetter
	public String getFontType()
	{
		return super.getFontType();
	}

	/**
	 * @clonedesc com.servoy.j2db.scripting.solutionmodel.JSComponent#getForeground()
	 * @sampleas com.servoy.j2db.scripting.solutionmodel.JSComponent#getForeground()
	 *
	 * @deprecated the foreground is handled by the bean class implementation; this solution model property is ignored
	 */
	@Override
	@Deprecated
	@JSGetter
	public String getForeground()
	{
		return super.getForeground();
	}

	/**
	 * @clonedesc com.servoy.j2db.scripting.solutionmodel.JSComponent#getPrintSliding()
	 * @sampleas com.servoy.j2db.scripting.solutionmodel.JSComponent#getPrintSliding()
	 *
	 * @deprecated print sliding is handled by the bean class implementation; this solution model property is ignored
	 */
	@Override
	@Deprecated
	@JSGetter
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public int getPrintSliding()
	{
		return super.getPrintSliding();
	}

	/**
	 * @clonedesc com.servoy.j2db.scripting.solutionmodel.JSComponent#getStyleClass()
	 * @sampleas com.servoy.j2db.scripting.solutionmodel.JSComponent#getStyleClass()
	 *
	 * @deprecated the stlye class is handled by the bean class implementation; this solution model property is ignored
	 */
	@Override
	@Deprecated
	@JSGetter
	public String getStyleClass()
	{
		return super.getStyleClass();
	}

	/**
	 * @clonedesc com.servoy.j2db.scripting.solutionmodel.JSComponent#getTransparent()
	 * @sampleas com.servoy.j2db.scripting.solutionmodel.JSComponent#getTransparent()
	 *
	 * @deprecated transparency is handled by the bean class implementation; this solution model property is ignored
	 */
	@Override
	@Deprecated
	@JSGetter
	public boolean getTransparent()
	{
		return super.getTransparent();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.scripting.solutionmodel.ISMBeanx#toString()
	 */
	@SuppressWarnings("nls")
	@Override
	public String toString()
	{
		return "JSBean[name:" + getName() + ",classname:" + getClassName() + ']';
	}

	@Override
	@JSGetter
	@ServoyClientSupport(ng = false, mc = true, wc = false, sc = false)
	public String getInnerHTML()
	{
		return getBaseComponent(false).getBeanXML();
	}

	@Override
	@JSSetter
	@ServoyClientSupport(ng = false, mc = true, wc = false, sc = false)
	public void setInnerHTML(String innerHTML)
	{
		getBaseComponent(true).setBeanXML(innerHTML);
	}
}
