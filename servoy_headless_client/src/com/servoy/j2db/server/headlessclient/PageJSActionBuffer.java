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
package com.servoy.j2db.server.headlessclient;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;

/**
 * A keeper of JS (and other) actions to be applied on a page on an AJAX request or header response.
 * It keeps JS actions ordered as added in each set except for triggerAjaxUpdate that will be added at the end (see SVY-1328), no matter when it's called.
 * 
 * @author acostescu
 */
@SuppressWarnings("nls")
public class PageJSActionBuffer
{

	private final List<PageAction> buffer1 = new ArrayList<PageAction>();
	private final List<TriggerAjaxUpdateAction> buffer2 = new ArrayList<TriggerAjaxUpdateAction>();

	public static interface PageAction
	{

		/**
		 * Applies this page action to the given target, on a ajax request from given page.
		 * @param target
		 * @param pageName the name of the page that generated the ajax request. Null if it's the page that of the action buffer itself.
		 * @return true if the action (was completed successfully and) should be removed from the action buffer. If this action cannot be executed on an ajax request target, returns false.
		 */
		boolean apply(AjaxRequestTarget target, String pageName);

		/**
		 * Applies this page action to the given header response (for example onLoad javascript) of it's own page.
		 * @param response the header response.
		 * @return true if the action (was completed successfully and) should be removed from the action buffer. If this action cannot be executed on a header response returns false.
		 */
		boolean apply(IHeaderResponse response);

	}

	/**
	 * Page action that just needs to render some javascript.
	 * It can perform the action on the onLoad of a page as well, not just by ajax request target.
	 */
	public static class JSChangeAction implements PageAction
	{

		private final String js;

		public JSChangeAction(String js)
		{
			this.js = js;
		}

		public boolean apply(AjaxRequestTarget target, String pageName)
		{
			if (pageName == null) target.appendJavascript(js); // only if req. comes from current page
			return true;
		}

		public boolean apply(IHeaderResponse response)
		{
			response.renderOnLoadJavascript(js);
			return true;
		}

	}

	public static class DivDialogAction implements PageAction
	{

		public static final int OP_SHOW = 1;
		public static final int OP_CLOSE = 2;
		public static final int OP_DIALOG_ADDED_OR_REMOVED = 3;
		public static final int OP_TO_FRONT = 4;
		public static final int OP_TO_BACK = 5;
		public static final int OP_SET_BOUNDS = 6;
		public static final int OP_SAVE_BOUNDS = 7;
		public static final int OP_RESET_BOUNDS = 8;

		private final ServoyDivDialog divDialog;
		private final int operation;
		private final Object[] parameters;

		public DivDialogAction(ServoyDivDialog divDialog, int operation)
		{
			this(divDialog, operation, null);
		}

		public DivDialogAction(ServoyDivDialog divDialog, int operation, Object[] parameters)
		{
			this.divDialog = divDialog;
			this.operation = operation;
			this.parameters = parameters;
		}

		public boolean apply(AjaxRequestTarget target, String pageName)
		{
			if (pageName == null || divDialog == null || pageName.equals(divDialog.getPageMapName())) // pageName != null if a dialog close is applied on the child's ajax request although it's added to the paren't action buffer
			{
				switch (operation)
				{
					case DivDialogAction.OP_SHOW :
						if (!divDialog.isShown())
						{
							divDialog.setPageMapName((String)parameters[0]);
							divDialog.show(target);
						}
						break;
					case DivDialogAction.OP_CLOSE :
						if (divDialog.isShown())
						{
							divDialog.close(target);
						}
						break;
					case DivDialogAction.OP_TO_FRONT :
						if (divDialog.getPageMapName() != null && divDialog.isShown())
						{
							divDialog.toFront(target);
						}
						break;
					case DivDialogAction.OP_TO_BACK :
						if (divDialog.getPageMapName() != null && divDialog.isShown())
						{
							divDialog.toBack(target);
						}
						break;
					case DivDialogAction.OP_DIALOG_ADDED_OR_REMOVED :
						target.addComponent((WebMarkupContainer)parameters[0]);
						break;
					case DivDialogAction.OP_SET_BOUNDS :
						divDialog.setBounds(target, (Integer)(parameters[0]), (Integer)parameters[1], (Integer)parameters[2], (Integer)parameters[3]);
						break;
					case DivDialogAction.OP_SAVE_BOUNDS :
						divDialog.saveBounds(target);
						break;
					case DivDialogAction.OP_RESET_BOUNDS :
						DivWindow.deleteStoredBounds(target, (String)parameters[0]);
				}
				return true;
			}
			return false;
		}

		public boolean apply(IHeaderResponse response)
		{
			return false;
		}

	}

	public static class TriggerAjaxUpdateAction extends JSChangeAction
	{

		private final MainPage page;

		public TriggerAjaxUpdateAction(MainPage page, String triggerScript)
		{
			super(triggerScript);
			this.page = page;
		}

		public MainPage getPage()
		{
			return page;
		}

	}

	public synchronized void addAction(PageAction a)
	{
		buffer1.add(a);
	}

	public void apply(AjaxRequestTarget target)
	{
		apply(target, null, null);
	}

	public synchronized void apply(AjaxRequestTarget target, PageJSActionBuffer toBeAppliedAsWell, String toBeAppliedAsWellPageName)
	{
		Iterator<PageAction> it = buffer1.iterator();
		while (it.hasNext())
		{
			if (it.next().apply(target, null)) it.remove();
		}

		if (toBeAppliedAsWell != null)
		{
			it = toBeAppliedAsWell.buffer1.iterator();
			while (it.hasNext())
			{
				if (it.next().apply(target, toBeAppliedAsWellPageName)) it.remove();
			}
		}

		Iterator<TriggerAjaxUpdateAction> it1 = buffer2.iterator();
		while (it1.hasNext())
		{
			if (it1.next().apply(target, null)) it1.remove();
		}
	}

	public synchronized void apply(IHeaderResponse headerResponse)
	{
		Iterator<PageAction> it = buffer1.iterator();
		while (it.hasNext())
		{
			if (it.next().apply(headerResponse)) it.remove();
		}

		Iterator<TriggerAjaxUpdateAction> it1 = buffer2.iterator();
		while (it1.hasNext())
		{
			if (it1.next().apply(headerResponse)) it1.remove();
		}
	}

	public synchronized void clear()
	{
		buffer1.clear();
		buffer2.clear();
	}

	public synchronized void triggerAjaxUpdate(MainPage mainPage, String triggerScript)
	{
		if (!hasAjaxUpdateTrigger(mainPage)) buffer2.add(new TriggerAjaxUpdateAction(mainPage, triggerScript));
	}

	public synchronized boolean hasAjaxUpdateTrigger(MainPage mainPage)
	{
		boolean alreadyThere = false;
		for (TriggerAjaxUpdateAction a : buffer2)
		{
			if (a.getPage() == mainPage)
			{
				alreadyThere = true;
				break;
			}
		}
		return alreadyThere;
	}

}