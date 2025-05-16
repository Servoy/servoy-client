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

package com.servoy.j2db.scripting.solutionmodel;

import java.util.concurrent.atomic.AtomicInteger;

import org.mozilla.javascript.annotations.JSFunction;
import org.mozilla.javascript.annotations.JSGetter;
import org.mozilla.javascript.annotations.JSSetter;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.AbstractContainer;
import com.servoy.j2db.persistence.CSSPositionLayoutContainer;
import com.servoy.j2db.persistence.CSSPositionUtils;
import com.servoy.j2db.persistence.FlattenedCSSPositionLayoutContainer;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.LayoutContainer;
import com.servoy.j2db.scripting.IJavaScriptType;
import com.servoy.j2db.util.PersistHelper;

/**
 * @author lvostinar
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, scriptingName = "JSResponsiveLayoutContainer")
@ServoyClientSupport(mc = false, wc = false, sc = false, ng = true)
public class JSResponsiveLayoutContainer extends JSBase<CSSPositionLayoutContainer>
	implements IJavaScriptType, IJSParent<CSSPositionLayoutContainer>, IBaseLayoutContainer, ISupportResponsiveLayoutContainer, IBaseContainer
{
	private final IApplication application;
	private final AtomicInteger id = new AtomicInteger();

	public JSResponsiveLayoutContainer(IJSParent< ? > parent, IApplication application, CSSPositionLayoutContainer cssLayoutContainer, boolean isNew)
	{
		super(parent, cssLayoutContainer, isNew);
		this.application = application;
	}

	public int getNextId()
	{
		return id.incrementAndGet();
	}

	@Override
	public CSSPositionLayoutContainer getSupportChild()
	{
		return getBaseComponent(false);
	}

	/**
	 * CSS position is a replacement for anchoring system making it more intuitive to place a component.
	 * CSS position should be set on form, an absolute position form can either work with anchoring or with css position.
	 * This is only working in NGClient.
	 *
	 * @sample
	 * var label = form.newLabel('Label', -1);
	 * label.cssPosition.r("10").b("10").w("20%").h("30px")
	 *
	 * @return The CSS position of the component.
	 */
	@JSGetter
	@ServoyClientSupport(ng = true, wc = false, sc = false, mc = false)
	public ICSSPosition getCssPosition()
	{
		return new JSCSSPosition(this);
	}

	@JSSetter
	@ServoyClientSupport(ng = true, wc = false, sc = false, mc = false)
	public void setCssPosition(ICSSPosition cssPosition)
	{
		//cannot assign for now
	}


	/**
	 * Create a new responsive layout container. The position is used to determine the generated order in html markup.
	 * @sample
	 * var container = container.newResponsiveLayoutContainer(1);
	 * @param position the position of responsive layout container object in its parent container
	 * @return the new responsive layout container
	 */
	@ServoyClientSupport(mc = false, ng = true, wc = false, sc = false)
	@JSFunction
	public JSResponsiveLayoutContainer newResponsiveLayoutContainer(int position)
	{
		try
		{
			CSSPositionLayoutContainer layoutContainer = getBaseComponent(true).createNewCSSPositionLayoutContainer();
			layoutContainer.setPackageName("servoycore");
			layoutContainer.setSpecName("servoycore-responsivecontainer");
			CSSPositionUtils.setLocation(layoutContainer, position, position);
			return application.getScriptEngine().getSolutionModifier().createResponsiveLayoutContainer(this, layoutContainer, true);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public LayoutContainer getLayoutContainer()
	{
		return getBaseComponent(false);
	}

	@Override
	public IApplication getApplication()
	{
		return application;
	}

	@Override
	public AbstractContainer getContainer()
	{
		return getBaseComponent(false);
	}

	@Override
	public AbstractContainer getFlattenedContainer()
	{
		LayoutContainer lc = getLayoutContainer();
		return (FlattenedCSSPositionLayoutContainer)PersistHelper.getFlattenedPersist(application.getFlattenedSolution(),
			(Form)lc.getAncestor(IRepository.FORMS),
			lc);
	}

	@Override
	public String toString()
	{
		return "JSResponsiveLayoutContainer[" + getLayoutContainer().getTagType() + ", attributes: " + getLayoutContainer().getMergedAttributes() + "]";
	}
}
