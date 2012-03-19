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
package com.servoy.j2db.server.headlessclient;

import java.util.Properties;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;

import com.servoy.j2db.server.headlessclient.dataui.TemplateGenerator;
import com.servoy.j2db.ui.IProviderStylePropertyChanges;
import com.servoy.j2db.ui.IStylePropertyChanges;
import com.servoy.j2db.util.IDelegate;
import com.servoy.j2db.util.ISupplyFocusChildren;

/**
 * @author jcompagner
 * 
 */
public class WrapperContainer extends WebMarkupContainer implements IDelegate<Component>, IProviderStylePropertyChanges, ISupplyFocusChildren<Component>
{

	private final Component wrappedComponent;
	private final IStylePropertyChanges stylePropertyChanges = new WrapperStylePropertyChanges();

	/**
	 * @param id
	 */
	public WrapperContainer(String id, Component wrappedComponent)
	{
		super(id);
		this.wrappedComponent = wrappedComponent;
		add(wrappedComponent);
		setOutputMarkupPlaceholderTag(true);
	}

	/**
	 * @see com.servoy.j2db.util.IDelegate#getDelegate()
	 */
	public Component getDelegate()
	{
		return wrappedComponent;
	}

	/**
	 * @see org.apache.wicket.Component#onAfterRender()
	 */
	@Override
	protected void onAfterRender()
	{
		super.onAfterRender();
		stylePropertyChanges.setRendered();
	}

	/**
	 * @see com.servoy.j2db.ui.IProviderStylePropertyChanges#getStylePropertyChanges()
	 */
	public IStylePropertyChanges getStylePropertyChanges()
	{
		return stylePropertyChanges;
	}

	public Component[] getFocusChildren()
	{
		return new Component[] { wrappedComponent };
	}

	private class WrapperStylePropertyChanges implements IStylePropertyChanges
	{
		private Properties changes = new Properties();
		private boolean changed = false;

		/**
		 * @see com.servoy.j2db.ui.IStylePropertyChanges#getChanges()
		 */
		public Properties getChanges()
		{
			createChanges();
			return changes;
		}

		/**
		 * 
		 */
		private void createChanges()
		{
			if (wrappedComponent instanceof IProviderStylePropertyChanges &&
				((IProviderStylePropertyChanges)wrappedComponent).getStylePropertyChanges().isChanged())
			{
				Properties componentChanges = ((IProviderStylePropertyChanges)wrappedComponent).getStylePropertyChanges().getChanges();
				// remove the positions and sizes from the real component (should always stay 0,0)
				Object left = componentChanges.remove("left"); //$NON-NLS-1$
				if (left != null) changes.put("left", left); //$NON-NLS-1$ 
				Object top = componentChanges.remove("top"); //$NON-NLS-1$
				if (top != null) changes.put("top", top); //$NON-NLS-1$ 

				// copy over the widths and heights
				Object offsetWidth = componentChanges.remove("offsetWidth"); //$NON-NLS-1$
				if (offsetWidth != null) changes.put("offsetWidth", offsetWidth); //$NON-NLS-1$ 
				Object offsetHeight = componentChanges.remove("offsetHeight"); //$NON-NLS-1$
				if (offsetHeight != null) changes.put("offsetHeight", offsetHeight); //$NON-NLS-1$ 

				Object width = componentChanges.remove("width"); //$NON-NLS-1$
				if (width != null) changes.put("width", width); //$NON-NLS-1$ 
				Object height = componentChanges.remove("height"); //$NON-NLS-1$
				if (height != null) changes.put("height", height); //$NON-NLS-1$ 

				boolean visibilityChanged = false;
				if (wrappedComponent.isVisible())
				{
					if ("none".equals(changes.get("display"))) //$NON-NLS-1$ //$NON-NLS-2$
					{
						changes.remove("display"); //$NON-NLS-1$
						visibilityChanged = true;
					}
				}
				else
				{
					changes.put("display", "none"); //$NON-NLS-1$ //$NON-NLS-2$
					visibilityChanged = true;
				}

				changed = changes.size() > 0 || visibilityChanged;
			}
		}

		/**
		 * @see com.servoy.j2db.ui.IStylePropertyChanges#isChanged()
		 */
		public boolean isChanged()
		{
			createChanges();
			return changed;
		}

		/**
		 * @see com.servoy.j2db.ui.IStylePropertyChanges#isValueChanged()
		 */
		public boolean isValueChanged()
		{
			return false;
		}

		/**
		 * @see com.servoy.j2db.ui.IStylePropertyChanges#setChanged()
		 */
		public void setChanged()
		{
			createChanges();
			changed = true;
		}

		/**
		 * @see com.servoy.j2db.ui.IStylePropertyChanges#setChanges(java.util.Properties)
		 */
		public void setChanges(Properties changes)
		{
			this.changes = changes == null ? new Properties() : changes;
			changed = true;
		}

		/**
		 * @see com.servoy.j2db.ui.IStylePropertyChanges#setRendered()
		 */
		public void setRendered()
		{
			changed = false;
			if (!wrappedComponent.isVisible() && wrappedComponent instanceof IProviderStylePropertyChanges)
			{
				((IProviderStylePropertyChanges)wrappedComponent).getStylePropertyChanges().setRendered();
			}
		}

		/**
		 * @see com.servoy.j2db.ui.IStylePropertyChanges#setValueChanged()
		 */
		public void setValueChanged()
		{
			setChanged();
		}

	}

	@Override
	public String getMarkupId()
	{
		return wrappedComponent.getMarkupId() + TemplateGenerator.WRAPPER_SUFFIX;
	}

	@Override
	public boolean isVisible()
	{
		return wrappedComponent != null ? wrappedComponent.isVisible() : super.isVisible();
	}
}
