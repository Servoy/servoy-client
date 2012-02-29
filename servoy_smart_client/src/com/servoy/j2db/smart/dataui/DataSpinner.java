/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.j2db.smart.dataui;

import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.ScrollPaneConstants;
import javax.swing.event.ListDataEvent;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.scripting.AbstractRuntimeValuelistComponent;


/**
 * Spinner-like smart client component. For now it is based on DataChoice implementation but if we will want it to be
 * editable we will have to make it similar to editable combobox but based on JSpinner.
 * @author acostescu
 */
public class DataSpinner extends DataChoice
{

	private boolean willEnsureSelectedIsVisible = false;

	public DataSpinner(IApplication app, AbstractRuntimeValuelistComponent<IFieldComponent> scriptable, IValueList vl)
	{
		super(app, scriptable, vl, Field.SPINNER);
		setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener()
		{
			public void adjustmentValueChanged(AdjustmentEvent e)
			{
				list.setElementAt(Boolean.TRUE, enclosedComponent.locationToIndex(getViewport().getViewPosition()));
			}
		});
		addComponentListener(new ComponentAdapter()
		{
			@Override
			public void componentShown(ComponentEvent e)
			{
				pinCellHeight();
			}

			@Override
			public void componentResized(ComponentEvent e)
			{
				pinCellHeight();
			}
		});
	}

	private void pinCellHeight()
	{
		enclosedComponent.setFixedCellHeight(getViewport().getHeight() - getViewport().getInsets().top - getViewport().getInsets().bottom);
		enclosedComponent.validate();
	}

	@Override
	public void setVerticalScrollBarPolicy(int policy)
	{
		// spinner always shows vertical scroll bar
	}

	@Override
	protected void setMultiValueSelect()
	{
		list.setMultiValueSelect(false);
	}

	@Override
	public void contentsChanged(ListDataEvent e)
	{
		super.contentsChanged(e);

		// update selected index
		if (enclosedComponent.getSelectedIndex() != list.getSelectedRow()) enclosedComponent.setSelectedIndex(e.getIndex0());

		// avoid doing this needlessly, currently this method gets called at least one for each list item when selection changes from outside the component
		if (willEnsureSelectedIsVisible == false)
		{
			willEnsureSelectedIsVisible = true;
			application.invokeLater(new Runnable()
			{
				public void run()
				{
					willEnsureSelectedIsVisible = false;
					enclosedComponent.ensureIndexIsVisible(enclosedComponent.getSelectedIndex());
				}
			});
		}
	}

	@Override
	protected boolean shouldPaintSelection()
	{
		return false;
	}

}
