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

import java.awt.Component;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.IStylePropertyChangesRecorder;
import com.servoy.j2db.ui.runtime.IRuntimeComponent;
import com.servoy.j2db.ui.runtime.IRuntimePassword;
import com.servoy.j2db.util.ComponentFactoryHelper;

/**
 * Scriptable password component.
 * 
 * @author lvostinar
 * @since 6.0
 */
public class RuntimeDataPassword extends AbstractRuntimeField<IFieldComponent> implements IRuntimePassword
{
	public RuntimeDataPassword(IStylePropertyChangesRecorder jsChangeRecorder, IApplication application)
	{
		super(jsChangeRecorder, application);
	}

	public String getElementType()
	{
		return IRuntimeComponent.PASSWORD;
	}

	public boolean isEditable()
	{
		return getComponent().isEditable();
	}

	public void setEditable(boolean b)
	{
		getComponent().setEditable(b);
		getChangesRecorder().setChanged();
	}

	@Override
	public void setBorder(String spec)
	{
		Border border = ComponentFactoryHelper.createBorder(spec);
		Border oldBorder = getComponent().getBorder();
		if (getComponent() instanceof Component && oldBorder instanceof CompoundBorder && ((CompoundBorder)oldBorder).getInsideBorder() != null)
		{
			Insets insets = ((CompoundBorder)oldBorder).getInsideBorder().getBorderInsets((Component)getComponent());
			getComponent().setBorder(
				BorderFactory.createCompoundBorder(border, BorderFactory.createEmptyBorder(insets.top, insets.left, insets.bottom, insets.right)));
		}
		else
		{
			getComponent().setBorder(border);
		}
		getChangesRecorder().setBorder(spec);
	}

	@Override
	public String getValueString()
	{
		return "password: *******"; //$NON-NLS-1$
	}

}
