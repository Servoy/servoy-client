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

import java.util.Iterator;
import java.util.Map;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.model.AbstractWrapModel;
import org.apache.wicket.model.IComponentAssignedModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.IWrapModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.string.AppendingStringBuffer;

import com.servoy.j2db.ui.IComponent;
import com.servoy.j2db.ui.IProviderStylePropertyChanges;


/**
 * This model/AttributeModifier can be attached to a {@link IComponent} (Wicket component) so that it 
 * will track its style changes through the {@link IProviderStylePropertyChanges} if the Wicket component does implement that one.
 * 
 * @author jcompagner
 * @since 5.0
 */
public class StyleAttributeModifierModel extends Model implements IComponentAssignedModel
{
	private static final long serialVersionUID = 1L;

	/**
	 * The attribute modifier instance to add to the behavior list of your Wicket component that implements {@link IProviderStylePropertyChanges}
	 */
	public static AttributeModifier INSTANCE = new StyleAppendingModifier(new StyleAttributeModifierModel());

	private StyleAttributeModifierModel()
	{
		super();

	}

	/**
	 * @see wicket.model.IAssignmentAwareModel#wrapOnAssignment(wicket.Component)
	 */
	public IWrapModel wrapOnAssignment(Component component)
	{
		return new WrapModel(component);
	}

	class WrapModel extends AbstractWrapModel
	{
		private static final long serialVersionUID = 1L;
		private final Component component;

		WrapModel(Component component)
		{
			this.component = component;
		}

		/**
		 * @see wicket.model.IWrapModel#getNestedModel()
		 */
		public IModel getWrappedModel()
		{
			return StyleAttributeModifierModel.this;
		}

		@Override
		public void detach()
		{
			StyleAttributeModifierModel.this.detach();
		}

		/**
		 * @see wicket.model.IModel#getObject()
		 */
		@Override
		public Object getObject()
		{
			if (component instanceof IProviderStylePropertyChanges)
			{
				Map<Object, Object> map = ((IProviderStylePropertyChanges)component).getStylePropertyChanges().getChanges();
				if (map.size() > 0)
				{
					AppendingStringBuffer asb = new AppendingStringBuffer();
					boolean skipColor = false;
					if (component.getFeedbackMessage() != null)
					{
						skipColor = true;
						asb.append("color: #FF0000;"); //$NON-NLS-1$
					}
					Iterator<Map.Entry<Object, Object>> it = map.entrySet().iterator();
					while (it.hasNext())
					{
						Map.Entry<Object, Object> entry = it.next();
						String property = (String)entry.getKey();
						if (skipColor && property.equals("color")) continue; //$NON-NLS-1$
						String value = (String)entry.getValue();
						asb.append(property);
						asb.append(": "); //$NON-NLS-1$
						asb.append(value);
						asb.append(";"); //$NON-NLS-1$
					}
					return asb;
				}
				else if (component.getFeedbackMessage() != null)
				{
					return "color: #FF0000;"; //$NON-NLS-1$
				}
			}
			else if (component.getFeedbackMessage() != null)
			{
				return "color: #FF0000;"; //$NON-NLS-1$
			}
			return null;
		}
	}
}
