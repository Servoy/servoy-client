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

import java.util.List;

import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.navigation.paging.IPageable;
import org.apache.wicket.model.IModel;
import org.apache.wicket.version.undo.Change;


/**
 * @author gboros
 */
public abstract class ServoyListView<T> extends ListView<T> implements IPageable
{
	private static final long serialVersionUID = 1L;

	private boolean isPageableMode;

	/** The page to show. */
	private int currentPage;

	/** Number of rows per page of the list view. */
	private int rowsPerPage;

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
	public ServoyListView(final String id, final IModel< ? extends List< ? extends T>> model, int rowsPerPage)
	{
		super(id, model);
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

			addStateChange(new RowsPerPageChange(this.rowsPerPage));
			this.rowsPerPage = rowsPerPage;
		}
	}

	/**
	 * @see org.apache.wicket.markup.html.list.ListView#getViewSize()
	 */
	@Override
	public int getViewSize()
	{
		if (isPageableMode)
		{
			if (getDefaultModelObject() != null)
			{
				super.setStartIndex(getCurrentPage() * getRowsPerPage());
				super.setViewSize(getRowsPerPage());
			}
		}

		return super.getViewSize();
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

			addStateChange(new CurrentPageChange(this.currentPage));
			this.currentPage = currentPage;
		}
	}


	@Override
	public ListView<T> setStartIndex(int startIndex)
	{
		return isPageableMode ? this : super.setStartIndex(startIndex);
	}


	@Override
	public ListView<T> setViewSize(int size) throws UnsupportedOperationException
	{
		return isPageableMode ? this : super.setViewSize(size);
	}

	public void setPageabeMode(boolean pageableMode)
	{
		this.isPageableMode = pageableMode;
	}

	public boolean isPageableMode()
	{
		return isPageableMode;
	}

	/**
	 * Records the changing of the current page.
	 */
	private class CurrentPageChange extends Change
	{
		private static final long serialVersionUID = 1L;

		/** the former 'current' page. */
		private final int currentPage;

		/**
		 * Construct.
		 * 
		 * @param currentPage
		 *            the former 'current' page
		 */
		CurrentPageChange(int currentPage)
		{
			this.currentPage = currentPage;
		}

		/**
		 * @see org.apache.wicket.version.undo.Change#undo()
		 */
		@Override
		public void undo()
		{
			setCurrentPage(currentPage);
		}

		/**
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString()
		{
			return "CurrentPageChange[currentPage: " + currentPage + "]";
		}
	}

	/**
	 * Records the changing of the number of rows per page.
	 */
	private class RowsPerPageChange extends Change
	{
		private static final long serialVersionUID = 1L;

		/** the former number of rows per page. */
		private final int rowsPerPage;

		/**
		 * Construct.
		 * 
		 * @param rowsPerPage
		 *            the former number of rows per page
		 */
		RowsPerPageChange(int rowsPerPage)
		{
			this.rowsPerPage = rowsPerPage;
		}

		/**
		 * @see org.apache.wicket.version.undo.Change#undo()
		 */
		@Override
		public void undo()
		{
			setRowsPerPage(rowsPerPage);
		}

		/**
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString()
		{
			return "RowsPerPageChange[component: " + getPath() + ", prefix: " + rowsPerPage + "]";
		}
	}

}