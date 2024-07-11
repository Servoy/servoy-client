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

import java.awt.Event;

import org.apache.wicket.Component;

import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.dataprocessing.IDisplay;
import com.servoy.j2db.dataprocessing.IFoundSet;
import com.servoy.j2db.server.headlessclient.WebForm;
import com.servoy.j2db.ui.BaseEventExecutor;
import com.servoy.j2db.ui.IEventExecutor;
import com.servoy.j2db.ui.IProviderStylePropertyChanges;

/**
 * The event executor that handles the events for a webclient.
 *
 * @author jcompagner
 */
public class WebEventExecutor extends BaseEventExecutor
{
	private final Component component;
	private final boolean useAJAX;

	public WebEventExecutor(Component c, boolean useAJAX)
	{
		this.component = c;
		this.useAJAX = useAJAX;
	}

	@Override
	public void setValidationEnabled(boolean b)
	{
		super.setValidationEnabled(b);
		if (component instanceof IProviderStylePropertyChanges)
		{
			((IProviderStylePropertyChanges)component).getStylePropertyChanges().setChanged();
		}
	}

	/**
	 * @see com.servoy.j2db.ui.BaseEventExecutor#setActionCmd(java.lang.String, Object[])
	 */
	@Override
	public void setActionCmd(String id, Object[] args)
	{
		super.setActionCmd(id, args);
	}

	@Override
	public void setDoubleClickCmd(String id, Object[] args)
	{
		super.setDoubleClickCmd(id, args);
	}

	@Override
	public void setRightClickCmd(String id, Object[] args)
	{
		super.setRightClickCmd(id, args);
	}

	/**
	 * Convert JS modifiers to AWT/Swing modifiers (used by Servoy event)
	 *
	 * @param webModifiers
	 * @return
	 */
	public static int convertModifiers(int webModifiers)
	{
		if (webModifiers == IEventExecutor.MODIFIERS_UNSPECIFIED) return IEventExecutor.MODIFIERS_UNSPECIFIED;

		// see function Servoy.Utils.getModifiers() in servoy.js
		int awtModifiers = 0;
		if ((webModifiers & 1) != 0) awtModifiers |= Event.CTRL_MASK;
		if ((webModifiers & 2) != 0) awtModifiers |= Event.SHIFT_MASK;
		if ((webModifiers & 4) != 0) awtModifiers |= Event.ALT_MASK;
		if ((webModifiers & 8) != 0) awtModifiers |= Event.META_MASK;

		return awtModifiers;
	}

	private static boolean isIndexSelected(IFoundSet fs, int index)
	{
		if (fs instanceof FoundSet)
		{
			FoundSet fsObj = (FoundSet)fs;
			for (int selectedIdx : fsObj.getSelectedIndexes())
			{
				if (selectedIdx == index) return true;
			}
		}
		return fs.getSelectedIndex() == index;
	}

	@Override
	protected String getFormName()
	{
		return getFormName(component);
	}

	@Override
	protected String getFormName(Object display)
	{
		WebForm form = ((Component)display).findParent(WebForm.class);
		if (form == null)
		{
			return null;
		}
		return form.getController().getName();
	}

	@SuppressWarnings("nls")
	private static void updateDragAttachOutput(Object component, StringBuilder sbAttachDrag, StringBuilder sbAttachDrop, boolean hasDragEvent,
		boolean hasDropEvent)
	{
		StringBuilder sb = null;
		if (hasDragEvent && (component instanceof WebBaseLabel || component instanceof WebBaseButton || component instanceof WebBaseSubmitLink ||
			((component instanceof IDisplay) && ((IDisplay)component).isReadOnly()))) sb = sbAttachDrag;
		else if (hasDropEvent) sb = sbAttachDrop;

		if (sb != null)
		{
			sb.append('\'');
			sb.append(((Component)component).getMarkupId());
			sb.append("',");
		}
	}

	@Override
	protected String getElementName(Object display)
	{
		String name = super.getElementName(display);
		return name;
	}

	@Override
	protected Object getSource(Object display)
	{
		return super.getSource(display);
	}

}