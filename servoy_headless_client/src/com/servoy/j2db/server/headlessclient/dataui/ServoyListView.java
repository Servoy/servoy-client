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

import java.util.Collections;
import java.util.List;

import org.apache.wicket.Component;


/**
 * @author gboros
 */
public abstract class ServoyListView<T> extends Component
{
	private static final long serialVersionUID = 1L;

	private boolean isPageableMode;

	/** The page to show. */
	private int currentPage;

	/** Number of rows per page of the list view. */
	private int rowsPerPage;

	private int startIndex;

	private int viewSize;

	/**
	 * Constructor
	 *
	 * @param id
	 *            See Component
	 * @param model
	 *            See Component
	 * @param rowsPerPage
	 *            Number of rows to show on a page
	 */
	public ServoyListView(final String id, int rowsPerPage)
	{
		super(id);
		this.rowsPerPage = rowsPerPage;
	}

	/**
	 * Gets the index of the current page being displayed by this list view.
	 *
	 * @return Returns the currentPage.
	 */
	public final int getCurrentPage()
	{
		if (isPageableMode)
		{
			// If first cell is out of range, bring page back into range
			while ((currentPage > 0) && ((currentPage * rowsPerPage) >= getList().size()))
			{
				currentPage--;
			}

			return currentPage;
		}
		return 0;
	}

	protected List getList()
	{
		return Collections.emptyList();
	}

	/**
	 * Gets the number of pages in this list view.
	 *
	 * @return The number of pages in this list view
	 */
	public final int getPageCount()
	{
		return isPageableMode ? ((getList().size() + rowsPerPage) - 1) / rowsPerPage : 1;
	}

	/**
	 * Gets the maximum number of rows on each page.
	 *
	 * @return the maximum number of rows on each page.
	 */
	public final int getRowsPerPage()
	{
		return isPageableMode ? rowsPerPage : getViewSize();
	}

	/**
	 * Sets the maximum number of rows on each page.
	 *
	 * @param rowsPerPage
	 *            the maximum number of rows on each page.
	 */
	public final void setRowsPerPage(int rowsPerPage)
	{
		if (isPageableMode)
		{
			if (rowsPerPage < 0)
			{
				rowsPerPage = 0;
			}

			this.rowsPerPage = rowsPerPage;
		}
	}

	public int getViewSize()
	{
		return viewSize;
	}

	/**
	 * Sets the current page that this list view should show.
	 *
	 * @param currentPage
	 *            The currentPage to set.
	 */
	public final void setCurrentPage(int currentPage)
	{
		if (isPageableMode)
		{
			if (currentPage < 0)
			{
				currentPage = 0;
			}

			int pageCount = getPageCount();
			if ((currentPage > 0) && (currentPage >= pageCount))
			{
				currentPage = pageCount - 1;
			}

			this.currentPage = currentPage;
		}
	}


	public ServoyListView<T> setStartIndex(int startIndex)
	{
		this.startIndex = startIndex;
		return this;
	}


	public ServoyListView<T> setViewSize(int viewSize) throws UnsupportedOperationException
	{
		this.viewSize = viewSize;
		return this;
	}

	public void setPageabeMode(boolean pageableMode)
	{
		this.isPageableMode = pageableMode;
	}

	public boolean isPageableMode()
	{
		return isPageableMode;
	}
}