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


import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.ComponentUI;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.ISupportNavigator;
import com.servoy.j2db.smart.dataui.StyledEnablePanel;
import com.servoy.j2db.util.ISkinnable;
import com.servoy.j2db.util.ISupplyFocusChildren;
import com.servoy.j2db.util.gui.NumberField;

public class Slider extends StyledEnablePanel implements ChangeListener, ActionListener, ISupplyFocusChildren<Component>
{
	/**
	 * The real slider
	 */
	private final JSlider slider;

	/**
	 * The number field
	 */
	private final NumberField field;

	/**
	 * Constructor I
	 */
	public Slider(IApplication app)
	{
		super(app);
		setName("slider_panel"); //$NON-NLS-1$
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		slider = new SkinSlider(SwingConstants.VERTICAL);//, 0, 0, 0);
		slider.setName("slider_slider"); //$NON-NLS-1$
		slider.setOpaque(false);
		Dimension d = new Dimension(ISupportNavigator.DEFAULT_NAVIGATOR_WIDTH, 80);
		slider.setPreferredSize(d);
		slider.setMaximumSize(d);
		slider.setMinimumSize(d);
		slider.setSize(d);
//		slider.setPaintTicks(true);
		slider.setPaintLabels(true);
		slider.setInverted(true);
		slider.setMinimum(1);
//		slider.setBackground(Color.green);
//		slider.setModel(new FixedDefaultBoundedRangeModel());
		slider.addChangeListener(this);
		add(slider);

		add(Box.createRigidArea(new Dimension(0, 3)));

		field = new NumberField(new Integer(0));
		field.setName("slider_numeric"); //$NON-NLS-1$
		field.setAllowNegativeValues(false);
		Dimension d1 = new Dimension(ISupportNavigator.DEFAULT_NAVIGATOR_WIDTH, 20);
		field.setPreferredSize(d1);
		field.setMinimumSize(d1);
		field.setMaximumSize(d1);
		field.setSize(d1);
		field.addActionListener(this);
		add(field);

		add(Box.createVerticalGlue());

		setBorder(BorderFactory.createEmptyBorder(10, 3, 0, 3));
//		setMaximumSize(new Dimension(DEFAULT_WIDTH,100));
		setOpaque(false);
	}

	protected class SkinSlider extends JSlider implements ISkinnable
	{
		public SkinSlider(int orientation)
		{
			super(orientation);
		}

		@Override
		public void setUI(ComponentUI ui)
		{
			super.setUI(ui);
		}
	}

	public boolean getValueIsAdjusting()
	{
		return slider.getValueIsAdjusting();
	}

	public void setMax(int index, boolean more)
	{
		//Create the label tableDictionary
		Dictionary<Integer, Component> labelTable = new Hashtable<Integer, Component>();
		Dimension d = new Dimension(40, 15);
		if (index == 0)
		{
			// do set the minimum to 0, java 6 can't have it when minimum > max label
			slider.setMinimum(0);
			Integer i = new Integer(0);
			JLabel start = new JLabel("0"); //$NON-NLS-1$
			start.setPreferredSize(d);
			start.setHorizontalAlignment(SwingConstants.RIGHT);
			labelTable.put(i, start);
//			field.setValue(i);
		}
		else
		{
			slider.setMinimum(1);
			JLabel start = new JLabel("1"); //$NON-NLS-1$
			start.setPreferredSize(d);
			start.setHorizontalAlignment(SwingConstants.RIGHT);
			labelTable.put(new Integer(1), start);
			//		labelTable.put( new Integer( 3 ), new JLabel("Slow") );
			String pre = ""; //$NON-NLS-1$
			if (more)
			{
				pre = "+"; //$NON-NLS-1$
			}
			JLabel end = new JLabel(index + pre);
			end.setPreferredSize(d);
			end.setHorizontalAlignment(SwingConstants.RIGHT);
			labelTable.put(new Integer(index), end);
		}
		slider.setMaximum(index);
		slider.setLabelTable(labelTable);
		slider.doLayout();
	}

	public int getMaximum()
	{
		return slider.getMaximum();
	}

	public void setValue(int index)
	{
		field.setValue(new Integer(index));
		slider.setValue(index);
	}

	public int getValue()
	{
		return slider.getValue();
	}

	public void addChangeListener(ChangeListener cl)
	{
		slider.addChangeListener(cl);
	}

	public void removeChangeListener(ChangeListener cl)
	{
		slider.removeChangeListener(cl);
	}

	public void stateChanged(ChangeEvent e)
	{
		Object source = e.getSource();
		if (source instanceof JSlider && !slider.getValueIsAdjusting())
		{
			field.setValue(new Integer(((JSlider)source).getValue()));
		}
	}

	public void actionPerformed(ActionEvent e)
	{
		Object source = e.getSource();
		if (source instanceof NumberField)
		{
			slider.setValue(((Integer)((NumberField)source).getValue()).intValue());
		}
	}

	public Component[] getFocusChildren()
	{
		return new Component[] { slider, field };
	}
}
