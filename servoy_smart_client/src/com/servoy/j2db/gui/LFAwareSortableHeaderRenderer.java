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
package com.servoy.j2db.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.plaf.UIResource;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.smart.TableView;
import com.servoy.j2db.smart.dataui.CellAdapter;
import com.servoy.j2db.ui.IComponent;
import com.servoy.j2db.util.IStyleRule;
import com.servoy.j2db.util.IStyleSheet;
import com.servoy.j2db.util.ImageLoader;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.gui.MyImageIcon;

/***
 * Header renderer for table component(tableview or portal); used for normal header and labelfor header
 * 
 *  @author lvostinar
 */
public class LFAwareSortableHeaderRenderer extends DefaultTableCellRenderer implements IComponent, UIResource
{
	private final TableView parentTable;
	private final CellAdapter cellAdapter;
	private final int columnIndex;

	private ImageIcon arrowDown = null;
	private ImageIcon arrowUp = null;
	private final int defaultHorizontalTextPosition;
	protected final GraphicalComponent gc;
	private static Border defaultBorder;
	private static Color defaultFgColor;
	private static Color defaultBgColor;
	private static Font defaultFont;
	private Border border;
	private Insets margin;

	public LFAwareSortableHeaderRenderer(IApplication app, TableView parentTable, CellAdapter cellAdapter, ImageIcon arrowUp, ImageIcon arrowDown,
		GraphicalComponent gc, Form formForStyles)
	{
		super();
		setBorder(null);
		this.parentTable = parentTable;
		this.cellAdapter = cellAdapter;
		this.columnIndex = cellAdapter.getModelIndex();

		this.arrowUp = arrowUp;
		this.arrowDown = arrowDown;

		this.defaultHorizontalTextPosition = getHorizontalTextPosition();
		this.gc = gc;

		setHorizontalAlignment(CENTER);
		setVerticalAlignment(CENTER);

		if (gc != null)
		{
			int style_halign = -1;
			int style_valign = -1;
			Pair<IStyleSheet, IStyleRule> styleInfo = ComponentFactory.getStyleForBasicComponent(app, gc, formForStyles);
			if (styleInfo != null)
			{
				IStyleSheet ss = styleInfo.getLeft();
				IStyleRule s = styleInfo.getRight();
				if (ss != null && s != null)
				{
					style_valign = ss.getVAlign(s);
					style_halign = ss.getHAlign(s);
					if (ss.hasMargin(s))
					{
						margin = ss.getMargin(s);
					}
				}
			}
			int halign = gc.getHorizontalAlignment();
			if (halign != -1)
			{
				setHorizontalAlignment(halign);
			}
			else if (style_halign != -1)
			{
				setHorizontalAlignment(style_halign);
			}
			int valign = gc.getVerticalAlignment();
			if (valign != -1)
			{
				setVerticalAlignment(valign);
			}
			else if (style_valign != -1)
			{
				setVerticalAlignment(style_valign);
			}

			int mediaId = gc.getImageMediaID();
			if (mediaId > 0)
			{
				Icon icon = null;
				if (gc.getMediaOptions() != 1)
				{
					icon = new MyImageIcon(app, this, ComponentFactory.loadIcon(app.getFlattenedSolution(), new Integer(mediaId)), gc.getMediaOptions());
				}
				else
				{
					icon = ImageLoader.getIcon(ComponentFactory.loadIcon(app.getFlattenedSolution(), new Integer(mediaId)), 0, 0, true);
				}
				if (icon != null) setIcon(icon);
			}
			if (gc != null && gc.getText() != null && gc.getText().length() > 0)
			{
				String text = gc.getText();
				text = app.getI18NMessageIfPrefixed(text);
				parentTable.getColumnModel().getColumn(columnIndex).setHeaderValue(text);
			}
			setToolTipText(app.getI18NMessageIfPrefixed(gc.getToolTipText()));
			if (gc != null && gc.getMargin() != null) margin = gc.getMargin();
		}
	}

	private Component lfComponent = null;
	private Object lfValue = null;

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
	{
		if (gc != null && gc.getImageMediaID() == 0)
		{
			if (!parentTable.getCurrentSortColumn().keySet().contains(new Integer(columnIndex)) || !parentTable.shouldDisplaySortIcons())
			{
				this.setIcon(null);
			}
			else
			{
				if (parentTable.getCurrentSortColumn().get(new Integer(columnIndex)).booleanValue()) this.setIcon(arrowDown);
				else this.setIcon(arrowUp);
				this.setHorizontalTextPosition(SwingConstants.LEADING);
			}
		}
		else
		{
			this.setHorizontalTextPosition(defaultHorizontalTextPosition);
		}
		TableCellRenderer lfAwareRenderer = table.getTableHeader().getDefaultRenderer();

		if (lfAwareRenderer != null)
		{
			// Ask the renderer to do the rendering for us.
			if (lfComponent == null || !Utils.equalObjects(value, lfValue))
			{
				// cache value, this is an expensive operation into the look and feel renderer.
				lfComponent = lfAwareRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				lfValue = value;
			}

			if (defaultFgColor == null) defaultFgColor = ((JLabel)lfComponent).getForeground();
			if (defaultBgColor == null) defaultBgColor = ((JLabel)lfComponent).getBackground();
			if (defaultFont == null) defaultFont = ((JLabel)lfComponent).getFont();
			if (lfComponent instanceof JLabel)
			{
				if (defaultBorder == null) defaultBorder = ((JLabel)lfComponent).getBorder();
			}
			// If the returned component supports icons (is a JLabel), then add the needed icon to it.
			// Usually the returned component is a JLabel.
			if (gc == null)
			{
				Color styleBgColor = cellAdapter.getHeaderBgColor(parentTable);
				if (styleBgColor != null) lfComponent.setBackground(styleBgColor);
				else lfComponent.setBackground(defaultBgColor);

				Color styleFgColor = cellAdapter.getHeaderFgColor(parentTable);
				if (styleFgColor != null) lfComponent.setForeground(styleFgColor);
				else lfComponent.setForeground(defaultFgColor);

				Font styleFont = cellAdapter.getHeaderFont(parentTable);
				if (styleFont != null) lfComponent.setFont(styleFont);
				else lfComponent.setFont(defaultFont);

				if (lfComponent instanceof JLabel)
				{
					JLabel label = (JLabel)lfComponent;
					if (styleBgColor != null)
					{
						label.setOpaque(true);
					}
					else
					{
						label.setOpaque(false);
					}
					label.setToolTipText(null);
					Dimension preferredSize = label.getPreferredSize();
					if (!parentTable.getCurrentSortColumn().keySet().contains(columnIndex))
					{
						if (label.getIcon() != null) label.setIcon(null);
					}
					else if (parentTable.shouldDisplaySortIcons())
					{
						if (parentTable.getCurrentSortColumn().get(new Integer(columnIndex)).booleanValue())
						{
							if (label.getIcon() != arrowDown) label.setIcon(arrowDown);
						}
						else if (label.getIcon() != arrowUp) label.setIcon(arrowUp);
					}
					if (!"".equals(value)) label.setPreferredSize(preferredSize);
					if (label.getHorizontalTextPosition() != SwingConstants.LEADING) label.setHorizontalTextPosition(SwingConstants.LEADING);
					// If the text consists only of spaces, trim it down.
					// It seems that when we set a label to have no text, the text that arrives is " ", not "",
					// and this generates some "..." to be displayed if the width of the column is small.
					String text = ""; //$NON-NLS-1$
					if (value != null && value.toString().trim().length() > 0) text = value.toString();
					if (!text.equals(label.getText())) label.setText(text);
				}
			}
			else
			{
				if (getBackground() != null && !Utils.equalObjects(getBackground(), lfComponent.getBackground())) lfComponent.setBackground(getBackground());
				if (!Utils.equalObjects(getForeground(), lfComponent.getForeground())) lfComponent.setForeground(getForeground());
				if (!Utils.equalObjects(getFont(), lfComponent.getFont())) lfComponent.setFont(getFont());
				if (lfComponent instanceof JLabel)
				{
					JLabel label = (JLabel)lfComponent;
					// If the text consists only of spaces, trim it down.
					// It seems that when we set a label to have no text, the text that arrives is " ", not "",
					// and this generates some "..." to be displayed if the width of the column is small.
					String text = ""; //$NON-NLS-1$
					if (value != null && value.toString().trim().length() > 0) text = value.toString();
					if (!text.equals(label.getText())) label.setText(text);
					if (!Utils.equalObjects(getIcon(), label.getIcon())) label.setIcon(getIcon());
					if (label.isOpaque() != isOpaque()) label.setOpaque(isOpaque());
					if (!Utils.equalObjects(getToolTipText(), label.getToolTipText())) label.setToolTipText(getToolTipText());
					if (label.getHorizontalTextPosition() != SwingConstants.LEADING) label.setHorizontalTextPosition(SwingConstants.LEADING);
				}
				// take the height of the first column label
				if (column == 0) lfComponent.setPreferredSize(new Dimension(lfComponent.getPreferredSize().width, (int)gc.getSize().getHeight()));
			}

			if (lfComponent instanceof JLabel)
			{
				JLabel label = (JLabel)lfComponent;
				label.setHorizontalAlignment(getHorizontalAlignment());
				label.setVerticalAlignment(getVerticalAlignment());
				if (border == null)
				{
					Border styleBorder = cellAdapter.getHeaderBorder(parentTable);
					if (styleBorder != null) ((JLabel)lfComponent).setBorder(styleBorder);
					border = (getBorder() != null) ? getBorder() : (styleBorder != null && gc == null ? styleBorder : defaultBorder);

					if (margin != null)
					{
						border = BorderFactory.createCompoundBorder(border,
							BorderFactory.createEmptyBorder(margin.top, margin.left, margin.bottom, margin.right));
					}
				}
				label.setBorder(border);
			}

			return lfComponent;
		}
		else
		{
			return this;
		}
	}

	public void setComponentEnabled(boolean enabled)
	{
		setEnabled(enabled);
	}

	public void setComponentVisible(boolean enabled)
	{
		setVisible(enabled);
	}

	public String getId()
	{
		return null;
	}

	public int getOnActionMethodID()
	{
		if (gc != null) return gc.getOnActionMethodID();
		return 0;
	}

	public List<Object> getFlattenedMethodArguments(String methodKey)
	{
		if (methodKey != null && gc != null)
		{
			return gc.getFlattenedMethodArguments(methodKey);
		}
		return null;
	}

	@Override
	public String getName()
	{
		if (gc != null) return gc.getName();
		return null;
	}
}