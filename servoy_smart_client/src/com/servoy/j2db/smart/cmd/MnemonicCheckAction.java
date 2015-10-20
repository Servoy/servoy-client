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
package com.servoy.j2db.smart.cmd;


import javax.swing.Action;
import javax.swing.Icon;

import com.servoy.j2db.LAFManager;

/**
 * @author jcompagner
 */
public abstract class MnemonicCheckAction extends MessageTextAction
{
	public MnemonicCheckAction()
	{
		super(null);
	}

	/**
	 * @param name
	 */
	public MnemonicCheckAction(String name, String key)
	{
		super(name, key);
	}

	/**
	 * @param name
	 * @param icon
	 */
	public MnemonicCheckAction(String name, String key, Icon icon)
	{
		super(name, key, icon);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.AbstractAction#putValue(java.lang.String, java.lang.Object)
	 */
	@Override
	public void putValue(String key, Object newValue)
	{
		if (Action.MNEMONIC_KEY.equals(key) && LAFManager.isUsingAppleLAF()) return;

		super.putValue(key, newValue);
	}
}
