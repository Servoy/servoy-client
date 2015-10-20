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
package com.servoy.j2db.util.gui;



import java.awt.event.ActionListener;

import javax.swing.DefaultButtonModel;

public class FixedDefaultButtonModel extends DefaultButtonModel
{
/*    public void addChangeListener(ChangeListener l) 
	{
		if (super.getChangeListeners().length == 0)
		{
			super.addChangeListener(l);
		}
    }*/
    public void addActionListener(ActionListener l) 
	{
		if (getActionListeners().length == 0)
		{
			super.addActionListener(l);
		}
    }
/*    public void addItemListener(ItemListener l) 
	{
		if (super.getItemListeners().length == 0)
		{
			super.addItemListener(l);
		}
    }*/
    
    /**
     * Returns an array of all the action listeners
     * registered on this <code>DefaultButtonModel</code>.
     *
     * @return all of this model's <code>ActionListener</code>s 
     *         or an empty
     *         array if no action listeners are currently registered
     *
     * @see #addActionListener
     * @see #removeActionListener
     *
     * @since 1.4
     */
    public ActionListener[] getActionListeners() {
        return (ActionListener[])listenerList.getListeners(
                ActionListener.class);
    }

}
