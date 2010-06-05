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
package com.servoy.j2db.server.headlessclient.dataui;

import org.apache.wicket.AbortException;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.navigation.paging.AjaxPagingNavigation;
import org.apache.wicket.ajax.markup.html.navigation.paging.AjaxPagingNavigationIncrementLink;
import org.apache.wicket.ajax.markup.html.navigation.paging.AjaxPagingNavigationLink;
import org.apache.wicket.ajax.markup.html.navigation.paging.AjaxPagingNavigator;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.navigation.paging.IPageable;
import org.apache.wicket.markup.html.navigation.paging.IPagingLabelProvider;
import org.apache.wicket.markup.html.navigation.paging.PagingNavigation;

import com.servoy.j2db.server.headlessclient.TabIndexHelper;

/**
 * @author jcompagner
 *
 */
public class ServoyAjaxPagingNavigator extends AjaxPagingNavigator implements ISupportWebTabSeq
{
	private static final long serialVersionUID = 1L;

	private int tabIndex;

	/**
	 * @param id
	 * @param pageable
	 */
	public ServoyAjaxPagingNavigator(String id, IPageable pageable)
	{
		super(id, pageable);
	}

	public void setTabIndex(int i)
	{
		this.tabIndex = i;
	}

	/**
	 * @see wicket.ajax.markup.html.navigation.paging.AjaxPagingNavigator#newPagingNavigationIncrementLink(java.lang.String, wicket.markup.html.navigation.paging.IPageable, int)
	 */
	@Override
	protected Link newPagingNavigationIncrementLink(String id, IPageable pageable, int increment)
	{
		Link rez = new AjaxPagingNavigationIncrementLink(id, pageable, increment)
		{
			private static final long serialVersionUID = 1L;

			/**
			 * @see wicket.ajax.markup.html.navigation.paging.AjaxPagingNavigationIncrementLink#onClick(wicket.ajax.AjaxRequestTarget)
			 */
			@Override
			public void onClick(AjaxRequestTarget target)
			{
				super.onClick(target);
				Page page = findPage();
				if (page != null)
				{
					WebEventExecutor.generateResponse(target, page);
				}
				else throw new AbortException();
			}
		};
		TabIndexHelper.setUpTabIndexAttributeModifier(rez, tabIndex);
		return rez;
	}

	/**
	 * @see org.apache.wicket.ajax.markup.html.navigation.paging.AjaxPagingNavigator#newPagingNavigationLink(java.lang.String, org.apache.wicket.markup.html.navigation.paging.IPageable, int)
	 */
	@Override
	protected Link newPagingNavigationLink(String id, IPageable pageable, int pageNumber)
	{
		Link rez = new ServoyAjaxPagingNavigationLink(id, pageable, pageNumber);
		TabIndexHelper.setUpTabIndexAttributeModifier(rez, tabIndex);
		return rez;
	}

	/**
	 * @see wicket.ajax.markup.html.navigation.paging.AjaxPagingNavigator#newNavigation(wicket.markup.html.navigation.paging.IPageable, wicket.markup.html.navigation.paging.IPagingLabelProvider)
	 */
	@Override
	protected PagingNavigation newNavigation(IPageable pageable, IPagingLabelProvider labelProvider)
	{
		return new AjaxPagingNavigation("navigation", pageable, labelProvider)
		{
			private static final long serialVersionUID = 1L;

			/**
			 * @see wicket.ajax.markup.html.navigation.paging.AjaxPagingNavigation#newPagingNavigationLink(java.lang.String, wicket.markup.html.navigation.paging.IPageable, int)
			 */
			@Override
			protected Link newPagingNavigationLink(String id, IPageable pageable, int pageIndex)
			{
				Link rez = new ServoyAjaxPagingNavigationLink(id, pageable, pageIndex);
				TabIndexHelper.setUpTabIndexAttributeModifier(rez, tabIndex);
				return rez;
			}
		};
	}

	/**
	 * @author jcompagner
	 *
	 */
	private final class ServoyAjaxPagingNavigationLink extends AjaxPagingNavigationLink
	{
		private static final long serialVersionUID = 1L;

		/**
		 * @param id
		 * @param pageable
		 * @param pageNumber
		 */
		private ServoyAjaxPagingNavigationLink(String id, IPageable pageable, int pageNumber)
		{
			super(id, pageable, pageNumber);
		}

		/**
		 * @see wicket.ajax.markup.html.navigation.paging.AjaxPagingNavigationLink#onClick(wicket.ajax.AjaxRequestTarget)
		 */
		@Override
		public void onClick(AjaxRequestTarget target)
		{
			super.onClick(target);
			Page page = findPage();
			if (page != null)
			{
				WebEventExecutor.generateResponse(target, page);
			}
			else throw new AbortException();
		}
	}

}
