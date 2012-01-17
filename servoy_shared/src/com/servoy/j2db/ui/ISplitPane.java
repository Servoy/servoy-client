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

import com.servoy.j2db.IScriptExecuter;
import com.servoy.j2db.dataprocessing.IDisplayRelatedData;
import com.servoy.j2db.scripting.FormScope;
import com.servoy.j2db.scripting.IScriptableProvider;

/**
 * Interface to which split pane components need to conform, to be handled in the same way for swing(rich client) or wicket(webclient) UI
 * 
 * @author gboros
 */
public interface ISplitPane extends IComponent, IScriptableProvider, IDisplayRelatedData
{
	IFormLookupPanel createFormLookupPanel(String name, String relationName, String formName);

	void setLeftForm(IFormLookupPanel flp);

	IFormLookupPanel getLeftForm();

	void setRightForm(IFormLookupPanel flp);

	IFormLookupPanel getRightForm();

	void setOnDividerChangeMethodCmd(String onDividerChangeMethodCmd);

	void addScriptExecuter(IScriptExecuter el);

	int getAbsoluteFormLocationY();

	boolean setForm(boolean bLeftForm, Object form, Object relation);

	FormScope getForm(boolean bLeftForm);

	void setRuntimeDividerLocation(double location);

	double getDividerLocation();

	void setDividerSize(int size);

	int getDividerSize();

	double getResizeWeight();

	void setResizeWeight(double resizeWeight);

	boolean getContinuousLayout();

	void setContinuousLayout(boolean b);

	int getFormMinSize(boolean bLeftForm);

	void setFormMinSize(boolean bLeftForm, int minSize);

	void setReadOnly(boolean b);
}
