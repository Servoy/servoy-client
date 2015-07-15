/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

package com.servoy.j2db;

import java.awt.Rectangle;
import java.util.Iterator;
import java.util.List;

import javax.swing.Action;

import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.util.SafeArrayList;

/**
 * @author jcompagner
 *
 */
public interface IBasicFormManager extends IFormManager
{
	public static final String NO_TITLE_TEXT = "-none-"; //$NON-NLS-1$

	public List<IFormController> getCachedFormControllers();

	public List<IFormController> getCachedFormControllers(Form form);

	/**
	 * @param name
	 */
	IFormController showFormInMainPanel(String name);

	/**
	 * @param name
	 */
	IFormController showFormInCurrentContainer(String name);

	/**
	 * @param name
	 * @param rect
	 * @param title
	 * @param resizeble
	 * @param showTextToolbar
	 * @param b
	 * @param modal
	 * @param window
	 */
	void showFormInDialog(String name, Rectangle rect, String title, boolean resizeble, boolean showTextToolbar, boolean b, boolean modal, String window);

	/**
	 * @param name
	 * @param rect
	 * @param title
	 * @param resizeble
	 * @param showTextToolbar
	 * @param window
	 */
	void showFormInFrame(String name, Rectangle rect, String title, boolean resizeble, boolean showTextToolbar, String window);

	/**
	 * @return
	 */
	Iterator<String> getPossibleFormNames();

	/**
	 * Get clear of the login form
	 */
	void clearLoginForm();

	public IFormController getCurrentMainShowingFormController();

	/**
	 * @param windowName
	 * @return
	 */
	IBasicMainContainer getMainContainer(String windowName);

	/**
	 * @param mContainer
	 * @return
	 */
	History getHistory(IBasicMainContainer mContainer);

	/**
	 * @return
	 */
	IBasicMainContainer getCurrentContainer();

	Form getPossibleForm(String name);


	/**
	 * @param name
	 * @return
	 */
	boolean isPossibleForm(String name);

	/**
	 * @param name
	 * @return
	 */
	IFormController leaseFormPanel(String name);

	/**
	 * @param formName
	 * @param container
	 * @param title
	 * @param closeAll
	 * @param dialogName
	 * @return
	 */
	IFormController showFormInContainer(String formName, IBasicMainContainer container, String title, boolean closeAll, String dialogName);

	void removeFormController(BasicFormController fp);

	public static class History
	{
		private static final int DEFAULT_HISTORY_SIZE = 10;
		private SafeArrayList<String> list;
		private int index = -1;
		private int length = 0;
		private final IApplication application;
		private final IBasicMainContainer container;
		private boolean buttonsEnabled = true;
		private int size = DEFAULT_HISTORY_SIZE;

		public History(IApplication application, IBasicMainContainer container)
		{
			this.application = application;
			this.container = container;
			list = new SafeArrayList<String>(DEFAULT_HISTORY_SIZE + 1);
		}

		/**
		 * @param string
		 */
		public boolean removeForm(String formName)
		{
			int i = list.indexOf(formName);
			if (i != -1 && !removeIndex(i))
			{
				return false;
			}
			return application.getFormManager().destroyFormInstance(formName);
		}

		/**
		 * @param i
		 */
		public boolean removeIndex(int i)
		{
			// removing the last form, nothing else to show
			if (length == 1 && i == 0)
			{
				clear(); // sets the buttons and index
				return true;
			}

			// if the currently shown item is removed, show the one before it
			if (i == index && !go(i == 0 ? 1 : -1))
			{
				// could not hide, do nothing
				return false;
			}

			list.remove(i);
			length--;

			if (i < index)
			{
				index--;
			}
			if (buttonsEnabled)
			{
				enableButtons(index != 0, index != length - 1);
			}
			return true;
		}

		/**
		 * @param i
		 */
		public boolean go(int i)
		{
			int idx = index + i;
			if (idx >= length || idx < 0)
			{
				return false;
			}
			String f = list.get(idx);
			if (f == null)
			{
				return false;
			}

			int saveIndex = index;
			index = idx; // must set index now to prevent add() from adding same form twice
			IFormController fc = application.getFormManager().showFormInContainer(f, container, null, true,
				application.getRuntimeWindowManager().getCurrentWindowName());
			if (fc == null || !fc.getName().equals(f))
			{
				index = saveIndex;
				return false;
			}
			if (buttonsEnabled)
			{
				enableButtons(index != 0, index != length - 1);
			}
			return true;
		}

		/**
		 * Enable or disable the backward and forward button, only if not in a dialog
		 */
		private void enableButtons(boolean enableBackward, boolean enableForWard)
		{
			// buttons are currently only used in the main window, not in dialogs
			if (application.getFormManager() instanceof FormManager && container == ((FormManager)application.getFormManager()).getMainContainer(null))
			{
				Action back = application.getCmdManager().getRegisteredAction("cmdhistoryback"); //$NON-NLS-1$
				if (back != null) back.setEnabled(enableBackward);
				Action forward = application.getCmdManager().getRegisteredAction("cmdhistoryforward"); //$NON-NLS-1$
				if (forward != null) forward.setEnabled(enableForWard);
			}
		}

		/**
		 * @param i
		 */
		public String getFormName(int i)
		{
			return list.get(i);
		}

		/**
		 *
		 */
		public boolean getButtonsEnabled()
		{
			return buttonsEnabled;
		}

		/**
		 * @param b
		 */
		public void setButtonsEnabled(boolean b)
		{
			if (!b)
			{
				enableButtons(false, false);
			}
			buttonsEnabled = b;
		}

		public int getIndex()
		{
			return index;
		}

		public int getFormIndex(String formName)
		{
			return list.indexOf(formName);
		}

		/**
		 *
		 */
		public void clear()
		{
			clear(size);
		}

		public void clear(int newSize)
		{
			if (length > 0)
			{
				list = new SafeArrayList<String>(20);
				index = -1;
				length = 0;

				enableButtons(false, false);
			}
			size = newSize;
		}

		public void add(String obj)
		{
			if (obj.equals(list.get(index))) return;

			if (length > 0 && buttonsEnabled)
			{
				enableButtons(true, false);
			}

			index++;
			list.set(index, obj);
			length = index + 1;

			if (length == size)
			{
				list.remove(0);
				length--;
				index--;
			}
		}

		public int getLength()
		{
			return length;
		}
	}

	/**
	 * @param form
	 * @param b
	 */
	public void addForm(Form form, boolean selected);

	/**
	 * @param form
	 */
	public boolean removeForm(Form form);

	/**
	 * @param formName
	 * @return
	 */
	public boolean destroyFormInstance(String formName);

	/**
	 * @param designFormName
	 * @param newInstanceScriptName
	 * @return
	 */
	public boolean createNewFormInstance(String designFormName, String newInstanceScriptName);

	/**
	 * @return
	 */
	public boolean isCurrentTheMainContainer();

}
