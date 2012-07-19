/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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

package com.servoy.j2db.ui.scripting;

import java.awt.Component;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.HashSet;
import java.util.Set;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.ui.IComponent;
import com.servoy.j2db.ui.IScriptRenderMethods;
import com.servoy.j2db.ui.IStylePropertyChangesRecorder;
import com.servoy.j2db.ui.ISupportOnRenderCallback;
import com.servoy.j2db.ui.RenderEventExecutor;
import com.servoy.j2db.ui.RenderableWrapper;
import com.servoy.j2db.util.IDelegate;

/**
  * Abstract scriptable component class for {@link IComponent} support onRrender events.
  * 
 * @author rgansevles
 * 
 * @since 6.1
 *
 */
@SuppressWarnings("nls")
public abstract class AbstractRuntimeRendersupportComponent<C extends IComponent> extends AbstractRuntimeBaseComponent<C> implements IScriptRenderMethods,
	ISupportOnRenderCallback
{

	private static final Set<String> propertiesToListenTo = new HashSet<String>();
	static
	{
		propertiesToListenTo.add(RenderableWrapper.PROPERTY_BGCOLOR);
		propertiesToListenTo.add(RenderableWrapper.PROPERTY_ENABLED);
		propertiesToListenTo.add(RenderableWrapper.PROPERTY_BORDER);
		propertiesToListenTo.add(RenderableWrapper.PROPERTY_FGCOLOR);
		propertiesToListenTo.add(RenderableWrapper.PROPERTY_FONT);
		propertiesToListenTo.add(RenderableWrapper.PROPERTY_TOOLTIP);
		propertiesToListenTo.add(RenderableWrapper.PROPERTY_TRANSPARENT);
		propertiesToListenTo.add(RenderableWrapper.PROPERTY_VISIBLE);
	}


	private final RenderableWrapper renderable;
	private final RenderEventExecutor renderEventExecutor;

	/**
	 * @param jsChangeRecorder
	 * @param application
	 */
	public AbstractRuntimeRendersupportComponent(IStylePropertyChangesRecorder jsChangeRecorder, IApplication application)
	{
		super(jsChangeRecorder, application);
		renderable = new RenderableWrapper(this);
		renderEventExecutor = new RenderEventExecutor(this);
	}

	@Override
	public void setComponent(C component, IPersist persist)
	{
		super.setComponent(component, persist);
		if (component instanceof Component)
		{
			Component focusComponent = (Component)component;
			if (component instanceof IDelegate< ? >)
			{
				Object delegate = ((IDelegate< ? >)component).getDelegate();
				if (delegate instanceof Component) focusComponent = (Component)delegate;
			}

			focusComponent.addFocusListener(new FocusListener()
			{
				public void focusLost(FocusEvent e)
				{
					getRenderEventExecutor().setRenderStateChanged();
					getRenderEventExecutor().fireOnRender(false);
				}

				public void focusGained(FocusEvent e)
				{
					getRenderEventExecutor().setRenderStateChanged();
					getRenderEventExecutor().fireOnRender(true);
				}
			});
		}
	}

	public RenderEventExecutor getRenderEventExecutor()
	{
		return renderEventExecutor;
	}

	public IScriptRenderMethods getRenderable()
	{
		return renderable;
	}

	public void setRenderableStateChanged()
	{
		getChangesRecorder().setChanged();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ui.scripting.AbstractRuntimeBaseComponent#propertyChanged(java.lang.String, java.lang.Object, java.lang.Object)
	 */
	@Override
	protected void propertyChanged(String propertyName, Object newValue, Object oldValue)
	{
		if (propertiesToListenTo.contains(propertyName))
		{
			if (!renderEventExecutor.isOnRenderExecuting()) renderable.clearProperty(propertyName);
			fireOnRender();
		}
	}

	protected void fireOnRender()
	{
		if (renderEventExecutor != null && !renderEventExecutor.isOnRenderExecuting())
		{
			renderEventExecutor.setRenderStateChanged();
			renderEventExecutor.fireOnRender(getComponent() instanceof Component ? ((Component)getComponent()).hasFocus() : false);
		}
	}
}
