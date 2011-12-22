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

import java.awt.Dimension;
import java.awt.Insets;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;

import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.persistence.ISupportTextSetup;
import com.servoy.j2db.ui.IStylePropertyChanges;
import com.servoy.j2db.ui.IStylePropertyChangesRecorder;
import com.servoy.j2db.util.ComponentFactoryHelper;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.FixedStyleSheet;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.gui.ISupportCustomBorderInsets;

/**
 * This class records the changes for wicket components/beans in ajax mode.
 * It has a {@link #setChanged()} method for marking the component for render and helper methods for generating the right css properties like location and font
 * <p>
 * when calling {@link #setChanged()} on it the component will be re rendered the next time a (ajax) request comes in
 * This can be the ajax polling behavior that every page of a servoy application has if ajax mode is enabled.
 * </p>
 * When setChanged() is called or any other helper method you have to call {@link #setRendered()} when the component is rendered again
 * else it will be re rendered for every coming request. This can be done by calling {@link #setRendered()} from the {@link Component#onAfterRender()}
 * that the wicket component needs to override.
 * <p>
 * the helper methods should be called from javascript methods so that changes done by javascript are reflected in the browser.
 * </p>
 * @author jcompagner
 * @since 5.0
 */
public class ChangesRecorder implements IStylePropertyChangesRecorder
{
	private static final ConcurrentMap<Integer, String> SIZE_STRINGS = new ConcurrentHashMap<Integer, String>();

	private final Properties changedProperties = new Properties();
	private String bgcolor;
	private Insets defaultBorder;
	private Insets defaultPadding;
	private boolean changed;
	private boolean valueChanged;
	private IStylePropertyChanges additionalChangesRecorder;

	/**
	 * default constructor if the component doesnt have default border or padding.
	 */
	public ChangesRecorder()
	{
		this(null, null);
	}

	/**
	 * use this constructor if the component for which this change recorder is made has a default border or padding in the browser.
	 * So that size calculations will take that into account.
	 * 
	 * @param defaultBorder
	 * @param defaultPadding
	 */
	public ChangesRecorder(Insets defaultBorder, Insets defaultPadding)
	{
		this.defaultBorder = defaultBorder;
		this.defaultPadding = defaultPadding;
	}

	public void setDefaultBorderAndPadding(Insets defaultBorder, Insets defaultPadding)
	{
		this.defaultBorder = defaultBorder;
		this.defaultPadding = defaultPadding;
	}

	/** Additional changes recorder for inner components that may change requiring the entire component to be rendered
	 * 
	 * @param additionalChangesRecorder the additionalChangesRecorder to set
	 */
	public void setAdditionalChangesRecorder(IStylePropertyChanges additionalChangesRecorder)
	{
		this.additionalChangesRecorder = additionalChangesRecorder;
	}

	/**
	 * @return All the current css properties of this component
	 */
	public Properties getChanges()
	{
		return changedProperties;
	}


	/**
	 * Adds all the css properties to the changed set and calls setChanged()
	 * 
	 * @param changes
	 */
	public void setChanges(Properties changes)
	{
		changedProperties.putAll(changes);
		setChanged();
	}


	/**
	 * Adds the background-color css property for the given color to the changed properties set.
	 * 
	 * @param bgcolor
	 */
	public void setBgcolor(String bgcolor)
	{
		if (!Utils.equalObjects(this.bgcolor, bgcolor))
		{
			setChanged();
			this.bgcolor = bgcolor;
			if (bgcolor == null)
			{
				changedProperties.remove("background-color"); //$NON-NLS-1$
			}
			else
			{
				changedProperties.put("background-color", bgcolor); //$NON-NLS-1$
			}
		}
	}

	/**
	 * Adds the color css property for the given color to the changed properties set.
	 * 
	 * @param clr
	 */
	public void setFgcolor(String clr)
	{
		setChanged();
		if (clr == null)
		{
			changedProperties.remove("color"); //$NON-NLS-1$
		}
		else
		{
			changedProperties.put("color", clr); //$NON-NLS-1$
		}
	}

	/**
	 * Adds the border css property for the given color to the changed properties set.
	 * 
	 * @param border
	 */
	public void setBorder(String border)
	{
		setChanged();
		if (border != null)
		{
			Properties properties = new Properties();
			ComponentFactoryHelper.createBorderCSSProperties(border, properties);
			changedProperties.putAll(properties);
		}
		else
		{
			changedProperties.put("border-style", "none"); //$NON-NLS-1$ //$NON-NLS-2$
			changedProperties.remove("border-width"); //$NON-NLS-1$
			changedProperties.remove("border-color"); //$NON-NLS-1$
		}
	}

	/**
	 * Sets the background-color css property to transparent if the boolean is true, 
	 * if false then it test if it has to set the bgcolor or remove the background-color property
	 * 
	 * @param transparent
	 */
	public void setTransparent(boolean transparent)
	{
		setChanged();
		if (transparent)
		{
			changedProperties.put("background-color", FixedStyleSheet.COLOR_TRANSPARENT); //$NON-NLS-1$ 
		}
		else if (bgcolor != null)
		{
			changedProperties.put("background-color", bgcolor); //$NON-NLS-1$
		}
		else
		{
			changedProperties.remove("background-color"); //$NON-NLS-1$
		}
	}

	/**
	 * Sets the x,y location css properties to the changed set.
	 * 
	 * @param x
	 * @param y
	 */
	public void setLocation(int x, int y)
	{
		setChanged();
		changedProperties.put("left", getSizeString(x)); //$NON-NLS-1$ 
		changedProperties.put("top", getSizeString(y)); //$NON-NLS-1$ 
	}

	/**
	 * @param width
	 * @param height
	 */
	public void setSize(int width, int height, Border border, Insets margin, int fontSize)
	{
		setSize(width, height, border, margin, fontSize, false, SwingConstants.CENTER);
	}

	public void setSize(int width, int height, Border border, Insets margin, int fontSize, boolean isButton, int valign)
	{
		setChanged();
		calculateWebSize(width, height, border, margin, fontSize, changedProperties, isButton, valign);
	}

	public Dimension calculateWebSize(int width, int height, Border border, Insets margin, int fontSize, Properties properties)
	{
		return calculateWebSize(width, height, border, margin, fontSize, properties, false, SwingConstants.CENTER);
	}

	public Dimension calculateWebSize(int width, int height, Border border, Insets margin, int fontSize, Properties properties, boolean isButton, int valign)
	{
		if (properties != null)
		{
			properties.put("offsetWidth", getSizeString(width)); //$NON-NLS-1$ 
			properties.put("offsetHeight", getSizeString(height)); //$NON-NLS-1$ 
		}
		Insets insets = getPaddingAndBorder(height, border, margin, fontSize, properties, isButton, valign);
		int realWidth = width;
		int realheight = height;
		// for <button> tags the border is drawn inside the component, regardless of the box model
		if (insets != null && !isButton)
		{
			realWidth -= (insets.left + insets.right);
			realheight -= (insets.top + insets.bottom);
		}

		if (properties != null)
		{
			properties.put("width", getSizeString(realWidth)); //$NON-NLS-1$
			properties.put("height", getSizeString(realheight)); //$NON-NLS-1$ 
		}
		return new Dimension(realWidth, realheight);
	}

	public Insets getPaddingAndBorder(int height, Border border, Insets margin, int fontSize, Properties properties)
	{
		return getPaddingAndBorder(height, border, margin, fontSize, properties, false, SwingConstants.CENTER);
	}

	/**
	 * @param height
	 * @param border
	 * @param margin
	 * @param fontSize
	 * @param properties
	 * @return
	 */
	@SuppressWarnings("nls")
	public Insets getPaddingAndBorder(int height, Border border, Insets margin, int fontSize, Properties properties, boolean isButton, int valign)
	{
		Insets insets = null;
		Insets borderMargin = margin;
		if (border != null)
		{
			// labels do have compound borders where margin and border are stored in.
			if (border instanceof CompoundBorder)
			{
				Insets marginInside = ((CompoundBorder)border).getInsideBorder().getBorderInsets(null);
				borderMargin = TemplateGenerator.sumInsets(borderMargin, marginInside);
				Border ob = ((CompoundBorder)border).getOutsideBorder();
				if (ob instanceof ISupportCustomBorderInsets)
				{
					insets = ((ISupportCustomBorderInsets)ob).getCustomBorderInsets();
				}
				else
				{
					insets = ob.getBorderInsets(null);
				}
			}
			else if (border instanceof ISupportCustomBorderInsets)
			{
				insets = ((ISupportCustomBorderInsets)border).getCustomBorderInsets();
			}
			else
			{
				try
				{
					insets = border.getBorderInsets(null);
				}
				catch (Exception ex)
				{
					insets = defaultBorder;
					Debug.error(ex);
				}
			}
		}
		else
		{
			insets = defaultBorder;
		}
		Insets padding = borderMargin;
		if (padding == null) padding = defaultPadding;
		if (properties != null)
		{
			Insets borderAndPadding = TemplateGenerator.sumInsets(insets, padding);
			int innerHeight = height;
			if (borderAndPadding != null) innerHeight -= borderAndPadding.top + borderAndPadding.bottom;
			int bottomPaddingExtra = 0;
			if (isButton && valign != ISupportTextSetup.CENTER)
			{
				bottomPaddingExtra = innerHeight;
			}
			if (padding == null)
			{
				properties.put("padding-top", "0px");
				properties.put("padding-right", "0px");
				properties.put("padding-left", "0px");
				properties.put("padding-bottom", getSizeString(bottomPaddingExtra));
			}
			else
			{
				properties.put("padding-top", getSizeString(padding.top));
				properties.put("padding-right", getSizeString(padding.right));
				properties.put("padding-left", getSizeString(padding.left));
				properties.put("padding-bottom", getSizeString((bottomPaddingExtra + padding.bottom)));
			}
		}

		if (insets == null) insets = padding;
		else
		{
			insets = TemplateGenerator.sumInsets(insets, padding);
		}
		return insets;
	}


	public Insets getPadding(Border border, Insets margin)
	{
		Insets borderMargin = margin;
		if (border != null)
		{
			if (border instanceof CompoundBorder)
			{
				Insets marginInside = ((CompoundBorder)border).getInsideBorder().getBorderInsets(null);
				borderMargin = TemplateGenerator.sumInsets(borderMargin, marginInside);
			}
		}
		return (borderMargin == null ? defaultPadding : borderMargin);
	}

	/**
	 * @param spec
	 */
	public void setFont(String spec)
	{
		setChanged();
		Pair<String, String>[] props = PersistHelper.createFontCSSProperties(spec);
		if (props != null)
		{
			for (Pair<String, String> element : props)
			{
				if (element == null) continue;
				changedProperties.put(element.getLeft(), element.getRight());
			}
		}
		else
		{
			changedProperties.remove("font-family"); //$NON-NLS-1$
			changedProperties.remove("font-size"); //$NON-NLS-1$
			changedProperties.remove("font-style"); //$NON-NLS-1$
			changedProperties.remove("font-weight"); //$NON-NLS-1$
		}
	}

	/**
	 * @param visible
	 */
	public void setVisible(boolean visible)
	{
		setChanged();
		if (visible)
		{
			changedProperties.remove("display"); //$NON-NLS-1$
		}
		else
		{
			changedProperties.put("display", "none"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	/**
	 *  Call this method from the {@link Component#onBeforeRender()} call to let the  change recorder know it has been rendered.
	 */
	public void setRendered()
	{
		changed = false;
		valueChanged = false;
		if (additionalChangesRecorder != null)
		{
			additionalChangesRecorder.setRendered();
		}
	}

	/**
	 * Returns true if this change recorder is changed and its component will be rendered the next time.
	 * 
	 * @see com.servoy.j2db.ui.IStylePropertyChanges#isChanged()
	 */
	public boolean isChanged()
	{
		return changed || (additionalChangesRecorder != null && additionalChangesRecorder.isChanged());
	}

	/**
	 * returns true if its component model object is changed 
	 * 
	 * @see com.servoy.j2db.ui.IStylePropertyChanges#isValueChanged()
	 */
	public boolean isValueChanged()
	{
		return valueChanged;
	}

	/**
	 * Set the change flag to true so that the component will be rendered the next time.
	 * 
	 */
	public void setChanged()
	{
		changed = true;
	}

	/**
	 * sets the value changed to true so that servoy knows that it is the value object that is changed.
	 * 
	 * @see com.servoy.j2db.ui.IStylePropertyChanges#setValueChanged()
	 */
	public void setValueChanged()
	{
		valueChanged = true;
		changed = true;
	}

	/**
	 *  Helper method to see if the value is changed.
	 *  
	 * @param component
	 * @param value
	 */
	public void testChanged(Component component, Object value)
	{
		IModel model = component.getInnermostModel();

		if (model instanceof RecordItemModel)
		{
			Object o = ((RecordItemModel)model).getLastRenderedValue(component);
			if (component instanceof IDisplayData && ((IDisplayData)component).getDataProviderID() == null)
			{
				// we don't have a mechanism to detect if the text has changed
				// both oldvalue and newvalue will always be null
				changed = true;
			}
			else if (!Utils.equalObjects(o, value))
			{
				changed = true;
				valueChanged = true;
			}
		}
	}

	private static String getSizeString(int size)
	{
		Integer integer = Integer.valueOf(size);
		String string = SIZE_STRINGS.get(integer);
		if (string == null)
		{
			string = size + "px"; //$NON-NLS-1$
			SIZE_STRINGS.put(integer, string);
		}
		return string;
	}
}
