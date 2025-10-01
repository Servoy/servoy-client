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

import java.awt.Color;

import com.servoy.j2db.IForm;
import com.servoy.j2db.IScriptExecuter;
import com.servoy.j2db.dataprocessing.IDisplayRelatedData;
import com.servoy.j2db.dataprocessing.RelatedFoundSet;
import com.servoy.j2db.scripting.IScriptableProvider;

/**
 * @author jcompagner
 */

public interface ITabPanel extends IComponent, IScriptableProvider, IDisplayRelatedData
{
	void setTabLayoutPolicy(int scroll_tab_layout);

	IFormLookupPanel createFormLookupPanel(String name, String relationName, String formName);

	void addTab(String text, String iconMediaUUID, IFormLookupPanel flp, String tooltip);

	void setTabForegroundAt(int index, Color fg);

	void setTabBackgroundAt(int index, Color bg);

	void setTabEnabledAt(int index, boolean enabled);

	/**
	 * @param onTabChangeMethodID
	 */
	void setOnTabChangeMethodCmd(String onTabChangeMethodCmd, Object[] onTabChangeArgs);

	void addScriptExecuter(IScriptExecuter el);

	boolean removeTabAt(int index);

	boolean removeAllTabs();

	boolean addTab(IForm formController, String formName, String tabname, String tabText, String tooltip, String iconURL, String fg, String bg,
		String relationName, RelatedFoundSet relatedFs, int tabIndex);

	int getAbsoluteFormLocationY();

	void setReadOnly(boolean b);

	void setTabTextAt(int i, String text);

	public String getTabTextAt(int i);

	public void setMnemonicAt(int i, int mnemonic);

	public int getMnemonicAt(int i);

	public String getTabNameAt(int i);

	public String getTabFormNameAt(int i);

	public void setTabIndex(int index);

	public void setTabIndex(String name);

	public boolean isTabEnabledAt(int index);

	public int getTabIndex();

	public int getMaxTabIndex();

	public void setHorizontalAlignment(int alignment);
}
