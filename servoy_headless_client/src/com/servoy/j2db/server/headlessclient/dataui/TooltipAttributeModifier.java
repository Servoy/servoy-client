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
package com.servoy.j2db.server.headlessclient.dataui;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IComponentAssignedModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.IWrapModel;
import org.apache.wicket.model.Model;

import com.servoy.j2db.ui.IComponent;

/**
 * This AttributeModifier will display the tooltip of an {@link IComponent#getToolTipText()} in the browser.
 * add the instance to the behavior list of your wicket component.
 * 
 * @author jcompagner
 * @since 5.0
 */
public class TooltipAttributeModifier extends AttributeModifier
{
	private static final long serialVersionUID = 1L;

	/**
	 * The attribute modifier instance to add to the behavior list of your Wicket {@link IComponent} for displaying its tooltip
	 */
	public static final TooltipAttributeModifier INSTANCE = new TooltipAttributeModifier();

	/**
	 * Construct.
	 * 
	 * @param attribute
	 * @param replaceModel
	 */
	private TooltipAttributeModifier()
	{
		super("onmouseover", true, new TooltipModel());
	}

	@Override
	public void bind(final Component component)
	{
		super.bind(component);
		component.add(new SimpleAttributeModifier("onmouseout", "hidetip();")
		{
			@Override
			public boolean isEnabled(Component component)
			{
				if (component instanceof IComponent || component instanceof SortableCellViewHeader)
				{
					String tooltip = null;
					if (component instanceof IComponent)
					{
						tooltip = ((IComponent)component).getToolTipText();
					}
					if (component instanceof SortableCellViewHeader)
					{
						tooltip = ((SortableCellViewHeader)component).getToolTipText();
					}
					if (tooltip == null)
					{
						return false;
					}
				}
				return super.isEnabled(component);
			}
		});
	}

	private static class TooltipModel extends Model implements IComponentAssignedModel
	{
		private static final long serialVersionUID = 1L;

		/**
		 * @see org.apache.wicket.model.IComponentAssignedModel#wrapOnAssignment(org.apache.wicket.Component)
		 */
		public IWrapModel wrapOnAssignment(Component component)
		{
			return new TooltipWrapModel(component);
		}

		private class TooltipWrapModel extends AbstractReadOnlyModel implements IWrapModel
		{
			private static final long serialVersionUID = 1L;

			private final Component component;

			/**
			 * @param component
			 */
			public TooltipWrapModel(Component component)
			{
				this.component = component;
			}

			/**
			 * @see org.apache.wicket.model.AbstractReadOnlyModel#getObject()
			 */
			@Override
			public Object getObject()
			{
				if (component instanceof IComponent || component instanceof SortableCellViewHeader)
				{
					String tooltip = null;
					if (component instanceof IComponent)
					{
						tooltip = ((IComponent)component).getToolTipText();
					}
					if (component instanceof SortableCellViewHeader)
					{
						tooltip = ((SortableCellViewHeader)component).getToolTipText();
					}
					if (tooltip != null)
					{
						tooltip = tooltip.replace("\r\n", " ");
						tooltip = tooltip.replace('\n', ' ');
						tooltip = tooltip.replace("\\", "\\\\");
						tooltip = tooltip.replace("\'", "\\\'");
						return "showtip(event, '" + tooltip + "');";
					}
				}
				return null;
			}

			/**
			 * @see org.apache.wicket.model.IWrapModel#getWrappedModel()
			 */
			public IModel getWrappedModel()
			{
				return TooltipModel.this;
			}

		}
	}

}
