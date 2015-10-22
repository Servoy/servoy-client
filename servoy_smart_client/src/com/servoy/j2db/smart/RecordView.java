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
package com.servoy.j2db.smart;


import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.FocusManager;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.IView;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.ISaveConstants;
import com.servoy.j2db.dataprocessing.ISwingFoundSet;
import com.servoy.j2db.dataprocessing.PrototypeState;
import com.servoy.j2db.smart.dataui.DataRenderer;
import com.servoy.j2db.smart.dataui.StyledEnablePanel;
import com.servoy.j2db.util.EnablePanel;
import com.servoy.j2db.util.model.IEditListModel;

/**
 * The recordview controller from mvc architecture
 * 
 * @author jblok
 */
public class RecordView extends EnablePanel implements ChangeListener, ListDataListener, IView, ListSelectionListener
{
	private Slider slider;
	private ISwingFoundSet model;
	private DataRenderer renderer;
	private final IApplication application;
	private boolean isAdjusting;

	public RecordView(IApplication app)
	{
		application = app;
		isAdjusting = false;
		setLayout(new BorderLayout());
		setOpaque(false);
		getSliderComponent();
	}

	public void destroy()
	{
		if (model != null)
		{
			model.removeListDataListener(this);
			model.getSelectionModel().removeListSelectionListener(this);
		}
		removeAll();
	}

	private void setModelInternal(IEditListModel m)
	{
		if (m == model)
		{
			return;//no change
		}
		isAdjusting = true;
		if (model != null)
		{
			model.removeListDataListener(this);
			model.getSelectionModel().removeListSelectionListener(this);
			slider.setMax(0, false);
		}
		model = (ISwingFoundSet)m;
		if (model != null)
		{
			slider.setMax(model.getSize(), model.hadMoreRows());
			model.addListDataListener(this);
			model.getSelectionModel().addListSelectionListener(this);
//			setSelectedIndex(model.getSelectedIndex());
		}
		isAdjusting = false;

		if (model != null)
		{
			valueChanged(null); //make sure data is set.
		}
	}

	public void setCellRenderer(DataRenderer r)
	{
		renderer = r;
	}

	public void start(final IApplication app)
	{
		if (renderer != null)
		{
			renderer.setRenderer(false);
			add(renderer, BorderLayout.CENTER);
			validate();
		}
		slider.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseReleased(MouseEvent e)
			{
				if (isEnabled())
				{
					app.getFoundSetManager().getEditRecordList().stopEditing(false);
				}
			}
		});
	}

	public void stop()
	{
		if (renderer != null)
		{
			remove(renderer);
		}
		setModel(null);
	}

	public StyledEnablePanel getSliderComponent()
	{
		if (slider == null)
		{
			slider = new Slider(application);
			slider.addChangeListener(this);
//			slider.setMax(model.getSize());
		}
		return slider;
	}

//	public void setUI(ComponentUI ui)
//	{
//		super.setUI(ui);
//		if (slider != null) slider.setUI(ui);
//	}

	public void stateChanged(ChangeEvent e)
	{
		if (!isAdjusting && !slider.getValueIsAdjusting() && model != null)
		{
			if (model.getSize() != 0)
			{
				int row = slider.getValue();
				if (row > 0)
				{
					final int modelSelection = model.getSelectionModel().getSelectedRow();
					if (modelSelection != row - 1)
					{
						if ((application.getFoundSetManager().getEditRecordList().stopIfEditing(model) & (ISaveConstants.STOPPED + ISaveConstants.AUTO_SAVE_BLOCKED)) != 0)
						{
							model.getSelectionModel().setSelectedRow(row - 1);
						}
						else
						{
							SwingUtilities.invokeLater(new Runnable()
							{
								public void run()
								{
									slider.setValue(modelSelection + 1);
								}
							});
						}
					}
				}
			}
			else
			{
				model.getSelectionModel().setSelectedRow(-1);
			}
		}
	}

	private void syncSliderAndRefresh()
	{
		// check if the model wasn't already cleared.
		if (model == null) return;
		isAdjusting = true;
		int index = model.getSelectedIndex();
		if (slider.getValue() - 1 != index)
		{
			slider.setValue(index + 1);
		}
		// if the same refresh to be sure!!
		if (renderer != null)
		{
			if (index != -1)
			{
				Object value = model.getElementAt(index);//minus 1 for the slider
				renderer.getListCellRendererComponent(this, value, index, false, true);
			}
			else
			{
				PrototypeState state = null;
				if (model != null)
				{
					state = ((IFoundSetInternal)model).getPrototypeState();
				}
				else
				{
					state = new PrototypeState(null);
				}

				renderer.getListCellRendererComponent(this, state, -1, false, true);
			}
		}
		isAdjusting = false;
	}

	public void contentsChanged(ListDataEvent e)
	{
		if (slider != null && model != null)
		{
			// refresh
			int selectedIndex = model.getSelectedIndex();
			if (e.getIndex0() <= selectedIndex && selectedIndex <= e.getIndex1())
			{
				syncSliderAndRefresh();
			}
		}
	}

	public void intervalAdded(ListDataEvent e)
	{
		if (slider != null && model != null)
		{
			int selectedIndex = model.getSelectedIndex();
			if (e.getIndex0() <= selectedIndex)
			{
				syncSliderAndRefresh();
			}
			slider.setMax(model.getSize(), model.hadMoreRows());
		}
	}

	public void intervalRemoved(ListDataEvent e)
	{
		if (slider != null && model != null)
		{
			int selectedIndex = model.getSelectedIndex();
			if (e.getIndex0() <= selectedIndex)
			{
				syncSliderAndRefresh();
			}
			slider.setMax(model.getSize(), model.hadMoreRows());
		}
	}

//	private void fireListSelectionChanged(int index)
//	{
//		if(isAdjusting) return;
//		ListSelectionEvent e = null;
//		for(int l = 0 ; l < listeners.size() ; l++)
//		{
//			if (e == null) e = new ListSelectionEvent(this,index,index,false);
//			ListSelectionListener lsl = (ListSelectionListener)listeners.get(l);
//			lsl.valueChanged(e);
//		}
//	}
//	public void addListSelectionListener(ListSelectionListener listener)
//	{
//		listeners.add(listener);
//	}
//	public void removeListSelectionListener(ListSelectionListener listener)
//	{
//		listeners.remove(listener);
//	}
//	private ArrayList listeners = new ArrayList(1);


	/**
	 * @see com.servoy.j2db.IView#editCellAt(int)
	 */
	public boolean editCellAt(int i)
	{
		model.setSelectedIndex(i);
		FocusManager.getCurrentManager().focusNextComponent(renderer);
		return true;
	}

	/**
	 * @see com.servoy.j2db.IView#setModel(com.servoy.j2db.dataprocessing.IFoundSetInternal)
	 */
	public void setModel(IFoundSetInternal fs)
	{
		if (fs instanceof IEditListModel)
		{
			setModelInternal((IEditListModel)fs);
		}
		else if (fs == null)
		{
			setModelInternal((IEditListModel)null);
		}
	}

	public boolean stopUIEditing(boolean looseFocus)
	{
		if (renderer != null)
		{
			return renderer.stopUIEditing(looseFocus);
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
	 */
	public void valueChanged(ListSelectionEvent e)
	{
		if (e != null && e.getValueIsAdjusting()) return;
		syncSliderAndRefresh();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.IController#isEditing()
	 */
	public boolean isEditing()
	{
		// TODO what to return here? Is recordview always in editing or never??
		return false;
	}


	private String bgColorScript;
	private List<Object> bgColorArgs;

	public String getRowBGColorScript()
	{
		return bgColorScript;
	}

	public List<Object> getRowBGColorArgs()
	{
		return bgColorArgs;
	}

	public void setRowBGColorScript(String bgColorScript, List<Object> args)
	{
		this.bgColorScript = bgColorScript;
		this.bgColorArgs = args;
	}

	public void ensureIndexIsVisible(int index)
	{
		//ignore not needed in record view
	}

	public boolean isDisplayingMoreThanOneRecord()
	{
		return false;
	}

	public void setEditable(boolean findMode)
	{
		//TODO: done elsewhere?
	}

	public void setVisibleRect(Rectangle scrollPosition)
	{
		if (isVisible())
		{
			scrollRectToVisible(scrollPosition);
		}
	}
}
