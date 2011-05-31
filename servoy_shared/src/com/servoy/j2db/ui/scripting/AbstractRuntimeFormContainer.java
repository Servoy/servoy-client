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

package com.servoy.j2db.ui.scripting;

import java.awt.Insets;

import javax.swing.JComponent;
import javax.swing.SwingConstants;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.dataprocessing.IDisplay;
import com.servoy.j2db.ui.IComponent;
import com.servoy.j2db.ui.IStylePropertyChangesRecorder;
import com.servoy.j2db.ui.ISupportReadOnly;
import com.servoy.j2db.util.PersistHelper;

/**
 * Abstract scriptable component that can contain forms.
 * 
 * @author lvostinar
 * @since 6.0
 */
public abstract class AbstractRuntimeFormContainer<C extends IComponent, E extends JComponent> extends AbstractRuntimeBaseComponent<C>
{
	protected E enclosingComponent;

	public AbstractRuntimeFormContainer(IStylePropertyChangesRecorder jsChangeRecorder, IApplication application)
	{
		super(jsChangeRecorder, application);
	}

	/**
	 * @param enclosingComponent the enclosingComponent to set
	 */
	public void setEnclosingComponent(E enclosingComponent)
	{
		this.enclosingComponent = enclosingComponent;
	}

	public void js_setToolTipText(String txt)
	{
		if (enclosingComponent != null)
		{
			enclosingComponent.setToolTipText(txt);
		}
		else
		{
			getComponent().setToolTipText(txt);
		}
		getChangesRecorder().setChanged();
	}

	public String js_getToolTipText()
	{
		if (enclosingComponent != null)
		{
			return enclosingComponent.getToolTipText();
		}
		return getComponent().getToolTipText();
	}

	@Override
	public void js_setFont(String spec)
	{
		if (enclosingComponent != null)
		{
			enclosingComponent.setFont(PersistHelper.createFont(spec));
		}
		else
		{
			super.js_setFont(spec);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ui.scripting.AbstractRuntimeBaseComponent#js_getFont()
	 */
	@Override
	public String js_getFont()
	{
		if (enclosingComponent != null)
		{
			return PersistHelper.createFontString(enclosingComponent.getFont());
		}
		return super.js_getFont();
	}

	@Override
	public String js_getBgcolor()
	{
		if (enclosingComponent != null)
		{
			return PersistHelper.createColorString(enclosingComponent.getBackground());
		}
		return super.js_getBgcolor();
	}

	@Override
	public void js_setBgcolor(String clr)
	{
		if (enclosingComponent != null)
		{
			enclosingComponent.setBackground(PersistHelper.createColor(clr));
		}
		else
		{
			super.js_setBgcolor(clr);
		}
	}

	@Override
	public String js_getFgcolor()
	{
		if (enclosingComponent != null)
		{
			return PersistHelper.createColorString(enclosingComponent.getForeground());
		}
		return super.js_getFgcolor();
	}

	@Override
	public void js_setFgcolor(String clr)
	{
		if (enclosingComponent != null)
		{
			enclosingComponent.setForeground(PersistHelper.createColor(clr));
		}
		else
		{
			super.js_setFgcolor(clr);
		}
	}

	@Override
	public void js_setSize(int x, int y)
	{
		super.js_setSize(x, y);
		if (getComponent() instanceof JComponent)
		{
			((JComponent)getComponent()).repaint();
		}
		getChangesRecorder().setSize(getComponent().getSize().width, getComponent().getSize().height, getComponent().getBorder(), new Insets(0, 0, 0, 0), 0,
			false, SwingConstants.TOP);
	}

	public boolean js_isReadOnly()
	{
		if (enclosingComponent instanceof ISupportReadOnly)
		{
			return ((ISupportReadOnly)enclosingComponent).isReadOnly();
		}
		return ((IDisplay)getComponent()).isReadOnly();
	}
}
