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
package com.servoy.j2db.smart.scripting;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import org.mozilla.javascript.Function;

import com.servoy.j2db.FormController;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.Messages;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.scripting.LazyCompilationScope;
import com.servoy.j2db.smart.J2DBClient;

/**
 * @author jcompagner
 * 
 */
public class ScriptMenuItem extends JMenuItem implements ActionListener
{
	private static final long serialVersionUID = 1L;

	private final IApplication application;
	private final FormController formController; //is global method when null

	/**
	 * Constructor for ScriptMenuItem.
	 * 
	 * @param text
	 */
	public ScriptMenuItem(IApplication application, FormController formController, String text, int autoSortcut)
	{
		super(text);
		this.application = application;
		this.formController = formController;
		if (autoSortcut >= 0 && autoSortcut <= 9)
		{
			setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_0 + autoSortcut, J2DBClient.menuShortcutKeyMask));
		}
		addActionListener(this);
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
	 */
	public void actionPerformed(ActionEvent e)
	{
		final String name = getText();
		final LazyCompilationScope scope = (formController == null ? application.getScriptEngine().getGlobalScope() : formController.getFormScope());
		final Function function = scope.getFunctionByName(name);
		if (function != null)
		{
			application.invokeLater(new Runnable()
			{
				public void run()
				{
					try
					{
						if (formController != null)
						{
							formController.executeFunction(function, null, true);
						}
						else
						{
							// we want to execute via the current form, so we set the trigger form name
							FormController fc = (FormController)application.getFormManager().getCurrentForm();
							if (fc != null)
							{
								//there for we need to add the prefix to pass though formcontroller
								String globalName = name;
								if (!globalName.startsWith(ScriptVariable.GLOBAL_DOT_PREFIX))
								{
									globalName = ScriptVariable.GLOBAL_DOT_PREFIX + globalName;
								}
								fc.executeFunction(globalName, null, true, null, false, null);
							}
							else
							{
								//is global only and no current form
								application.getScriptEngine().executeFunction(function, scope, scope, null, false, false);
							}
						}
					}
					catch (Exception e1)
					{
						application.reportError(Messages.getString("servoy.formPanel.error.executingMethod", new Object[] { name }), e1); //$NON-NLS-1$ 
					}
				}
			});
		}
	}
}
