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
package com.servoy.j2db.ui;


import java.awt.Insets;
import java.util.List;

import com.servoy.j2db.IScriptExecuter;
import com.servoy.j2db.dataprocessing.ICanNeedEntireState;
import com.servoy.j2db.scripting.IScriptableProvider;

/**
 * @author jblok
 */
public interface IFieldComponent extends ICanNeedEntireState, ISupportSecuritySettings, IComponent, ISupportEventExecutor, IScriptableProvider
{
	public void setToolTipText(String tooltip);

	public void setTitleText(String text);

	public void setDataProviderID(String dataProviderID);

	public void setOpaque(boolean opaque);

	public void setSelectOnEnter(boolean selectOnEnter);

	public void setEditable(boolean editable);

	public void addScriptExecuter(IScriptExecuter scriptExecuter);

	public void setEnterCmds(String[] enterCmds, Object[][] args);

	public void setLeaveCmds(String[] leaveCmds, Object[][] args);

	public void setActionCmd(String actionCmd, Object[] args);

	public void setRightClickCommand(String rightClickCmd, Object[] args);

	public void setChangeCmd(String changeCmd, Object[] args);

	public void setMaxLength(int maxLength);

	public void setMargin(Insets margin);

	public void setHorizontalAlignment(int horizontalAlignment);

	public void addLabelFor(ILabel label);

	public List<ILabel> getLabelsFor();

	public String getTitleText();

	public int getAbsoluteFormLocationY();

	public void requestFocusToComponent();

	public void setReadOnly(boolean b);

	public Insets getMargin();

	public boolean isEditable();
}
