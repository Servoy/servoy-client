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

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import javax.swing.border.Border;
import javax.swing.text.Document;

import org.apache.wicket.Component;

import com.servoy.base.util.ITagResolver;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IScriptExecuter;
import com.servoy.j2db.component.ComponentFormat;
import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.dataprocessing.IEditListener;
import com.servoy.j2db.ui.IComponent;
import com.servoy.j2db.ui.IEventExecutor;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.IFormattingComponent;
import com.servoy.j2db.ui.ILabel;
import com.servoy.j2db.ui.IProviderStylePropertyChanges;
import com.servoy.j2db.ui.IStylePropertyChanges;
import com.servoy.j2db.ui.ISupportSimulateBounds;
import com.servoy.j2db.ui.ISupportSimulateBoundsProvider;
import com.servoy.j2db.ui.ISupportWebBounds;
import com.servoy.j2db.ui.scripting.AbstractRuntimeField;
import com.servoy.j2db.ui.scripting.IFormatScriptComponent;
import com.servoy.j2db.ui.scripting.RuntimeDataField;
import com.servoy.j2db.util.IDelegate;
import com.servoy.j2db.util.ISupplyFocusChildren;
import com.servoy.j2db.util.PersistHelper;

/**
 * Represents a component based on a text field to which it adds more (sub)components for added functionality.
 * For example calendar, spinner.
 *
 * @author jcompagner
 */
public abstract class WebDataCompositeTextField extends Component implements IFieldComponent, IDisplayData, IDelegate, ISupportWebBounds,
	IProviderStylePropertyChanges, ISupplyFocusChildren<Component>, IFormattingComponent,
	ISupportSimulateBoundsProvider
{
	private static final long serialVersionUID = 1L;

	public static final String AUGMENTED_FIELD_ID = "_AF"; //$NON-NLS-1$

	protected final WebDataField field;
	protected final IApplication application;
	protected boolean readOnly = false;
	protected boolean showExtraComponents = true;
	protected boolean editable;
	private Insets margin;
	private final AbstractRuntimeField<IFieldComponent> scriptable;

	private final boolean designMode = false;

	public WebDataCompositeTextField(IApplication application, AbstractRuntimeField<IFieldComponent> scriptable, String id)
	{
		super(id);
		this.application = application;
		this.scriptable = scriptable;

		RuntimeDataField fieldScriptable = new RuntimeDataField(new ChangesRecorder(), application);
		field = createTextField(fieldScriptable);
		fieldScriptable.setComponent(field, null);

		field.setIgnoreOnRender(true);

		add(field);

		// because the composite field will probably add one or more html tags tag besides the field component
		// each time that component is rendered, we must make sure that we render the whole container;
		// otherwise, each independent render of the field component will add one more unwanted tags to the composite field markup
		((ChangesRecorder)scriptable.getChangesRecorder()).setAdditionalChangesRecorder(field.getStylePropertyChanges());
	}

	protected WebDataField createTextField(RuntimeDataField fieldScriptable)
	{
		return new AugmentedTextField(application, fieldScriptable, this);
	}

	protected String getTextFieldId()
	{
		return getId() + AUGMENTED_FIELD_ID;
	}

	public AbstractRuntimeField<IFieldComponent> getScriptObject()
	{
		return scriptable;
	}

	public Component[] getFocusChildren()
	{
		return new Component[] { field };
	}

	public IStylePropertyChanges getStylePropertyChanges()
	{
		return scriptable.getChangesRecorder();
	}

	public Object getDelegate()
	{
		return field;
	}

	public Document getDocument()
	{
		return field.getDocument();
	}

	public void setMargin(Insets i)
	{
		this.margin = i;
	}

	public Insets getMargin()
	{
		return margin;
	}

	public void addScriptExecuter(IScriptExecuter el)
	{
		field.addScriptExecuter(el);
	}

	public IEventExecutor getEventExecutor()
	{
		return field.getEventExecutor();
	}

	public void setEnterCmds(String[] ids, Object[][] args)
	{
		field.setEnterCmds(ids, null);
	}

	public void setLeaveCmds(String[] ids, Object[][] args)
	{
		field.setLeaveCmds(ids, null);
	}

	public void setActionCmd(String id, Object[] args)
	{
		field.setActionCmd(id, args);
	}

	public void notifyLastNewValueWasChange(Object oldVal, Object newVal)
	{
		field.notifyLastNewValueWasChange(oldVal, newVal);
	}

	public boolean isValueValid()
	{
		return field.isValueValid();
	}

	public void setValueValid(boolean valid, Object oldVal)
	{
		field.setValueValid(valid, oldVal);
	}

	public void setChangeCmd(String id, Object[] args)
	{
		field.setChangeCmd(id, args);
	}

	public void setHorizontalAlignment(int a)
	{
		field.setHorizontalAlignment(a);
	}

	public void setMaxLength(int i)
	{
		field.setMaxLength(i);
	}

	public void addEditListener(IEditListener l)
	{
		if (field != null) field.addEditListener(l);
	}

	public void setValueObject(Object obj)
	{
		field.setValueObject(obj);
	}

	public Object getValueObject()
	{
		return null;
	}

	public boolean needEditListener()
	{
		return true;
	}

	public boolean needEntireState()
	{
		return field.needEntireState();
	}

	public void setNeedEntireState(boolean b)
	{
		field.setNeedEntireState(b);
	}

	protected ITagResolver resolver;

	public void setTagResolver(ITagResolver resolver)
	{
		this.resolver = resolver;
	}

	public void setValidationEnabled(boolean b)
	{
		field.setValidationEnabled(b);
		if (b)
		{
			if (showExtraComponents == readOnly)
			{
				showExtraComponents = !readOnly;
				getStylePropertyChanges().setChanged();
			}
		}
		else
		{
			if (!Boolean.TRUE.equals(application.getClientProperty(IApplication.LEAVE_FIELDS_READONLY_IN_FIND_MODE)))
			{
				boolean oldReadonly = readOnly;
				setReadOnly(false);
				readOnly = oldReadonly;
			}
		}
	}

	public boolean stopUIEditing(boolean looseFocus)
	{
		if (field != null) return field.stopUIEditing(looseFocus);
		return true;
	}

	public void setSelectOnEnter(boolean b)
	{
		if (field != null) field.setSelectOnEnter(b);
	}

	@Override
	public void setCursor(Cursor cursor)
	{
		// nothing here yet
	}

	@Override
	public String toString()
	{
		return scriptable != null ? scriptable.toString() : super.toString();
	}

	public void requestFocusToComponent()
	{
		field.requestFocusToComponent();
	}

	public String getDataProviderID()
	{
		return field.getDataProviderID();
	}

	public void setDataProviderID(String id)
	{
		field.setDataProviderID(id);
	}

	/*
	 * format---------------------------------------------------
	 */
	public void installFormat(ComponentFormat componentFormat)
	{
		((IFormatScriptComponent)field.getScriptObject()).setComponentFormat(componentFormat);
	}

	public boolean isEditable()
	{
		return editable;
	}

	public void setEditable(boolean b)
	{
		field.setEditable(b);
		editable = b;
	}

	public void setReadOnly(boolean b)
	{
		if (readOnly != b)
		{
			readOnly = b;
			showExtraComponents = !b;
			field.setReadOnly(b);
		}
	}

	public boolean isReadOnly()
	{
		return !showExtraComponents;
	}


	@Override
	public void setName(String n)
	{
		name = n;
		field.setName(n);
	}

	private String name;

	@Override
	public String getName()
	{
		return name;
	}


	/*
	 * border---------------------------------------------------
	 */
	private Border border;

	@Override
	public void setBorder(Border border)
	{
		this.border = border;
	}

	@Override
	public Border getBorder()
	{
		return border;
	}


	/*
	 * opaque---------------------------------------------------
	 */
	@Override
	public void setOpaque(boolean opaque)
	{
		this.opaque = opaque;
	}

	private boolean opaque;

	@Override
	public boolean isOpaque()
	{
		return opaque;
	}

	/*
	 * titleText---------------------------------------------------
	 */

	public void setTitleText(String title)
	{
		field.setTitleText(title);
	}

	public String getTitleText()
	{
		return field.getTitleText();
	}

	/*
	 * tooltip---------------------------------------------------
	 */

	@Override
	public void setToolTipText(String tip)
	{
		field.setToolTipText(tip);
	}

	@Override
	public String getToolTipText()
	{
		return field.getToolTipText();
	}

	/*
	 * font---------------------------------------------------
	 */
	private Font font;

	@Override
	public Font getFont()
	{
		return font;
	}

	@Override
	public void setFont(Font f)
	{
		font = f;
		if (f != null && field != null)
		{
			field.setFont(f);
		}
	}

	private Color background;

	@Override
	public void setBackground(Color cbg)
	{
		this.background = cbg;
	}

	@Override
	public Color getBackground()
	{
		return background;
	}


	private Color foreground;

	private ArrayList<ILabel> labels;

	@Override
	public void setForeground(Color cfg)
	{
		this.foreground = cfg;
		if (field != null)
		{
			field.getScriptObject().setFgcolor(PersistHelper.createColorString(cfg));
		}
	}

	@Override
	public Color getForeground()
	{
		return foreground;
	}

	/*
	 * visible---------------------------------------------------
	 */
	@Override
	public void setComponentVisible(boolean visible)
	{
		if (viewable || !visible)
		{
			setVisible(visible);
			if (labels != null)
			{
				for (ILabel label : labels)
				{
					label.setComponentVisible(visible);
				}
			}
		}
	}

	public void addLabelFor(ILabel label)
	{
		if (labels == null) labels = new ArrayList<ILabel>(3);
		labels.add(label);
	}

	public List<ILabel> getLabelsFor()
	{
		return labels;
	}

	@Override
	public void setComponentEnabled(final boolean b)
	{
		if (accessible || !b)
		{
			super.setEnabled(b);
			field.setEnabled(b);
			getStylePropertyChanges().setChanged();
			if (labels != null)
			{
				for (ILabel label : labels)
				{
					label.setComponentEnabled(b);
				}
			}
		}
	}

	private boolean accessible = true;

	public void setAccessible(boolean b)
	{
		if (!b) setComponentEnabled(b);
		accessible = b;
	}

	private boolean viewable = true;

	public void setViewable(boolean b)
	{
		if (!b) setComponentVisible(b);
		this.viewable = b;
	}

	public boolean isViewable()
	{
		return viewable;
	}

	/*
	 * location---------------------------------------------------
	 */
	private Point location = new Point(0, 0);

	public int getAbsoluteFormLocationY()
	{
		WebDataRenderer parent = findParent(WebDataRenderer.class);
		if (parent != null)
		{
			return parent.getYOffset() + getLocation().y;
		}
		return getLocation().y;
	}

	@Override
	public void setLocation(Point location)
	{
		this.location = location;
	}

	@Override
	public Point getLocation()
	{
		return location;
	}

	/*
	 * size---------------------------------------------------
	 */
	private Dimension size = new Dimension(0, 0);

	@Override
	public Dimension getSize()
	{
		return size;
	}

	public Rectangle getWebBounds()
	{
		Dimension d = ((ChangesRecorder)getStylePropertyChanges()).calculateWebSize(size.width, size.height, border, null, 0, null);
		return new Rectangle(location, d);
	}

	public Insets getPaddingAndBorder()
	{
		return ((ChangesRecorder)getStylePropertyChanges()).getPaddingAndBorder(size.height, border, null, 0, null);
	}


	@Override
	public void setSize(Dimension size)
	{
		this.size = size;
	}

	public void setRightClickCommand(String rightClickCmd, Object[] args)
	{
		field.setRightClickCommand(rightClickCmd, args);
	}

	public ISupportSimulateBounds getBoundsProvider()
	{
		return findParent(ISupportSimulateBounds.class);
	}

	protected class AugmentedTextField extends WebDataField
	{

		public AugmentedTextField(IApplication application, RuntimeDataField scriptable, IComponent enclosingComponent)
		{
			super(application, scriptable, getTextFieldId(), enclosingComponent);
		}

		// When the composite field is neither editable nor read-only, we want the text field to be not editable, but
		// we want to let the user change the (date) value using the extra components.
		// In this case we need the read only and filter backspace behaviors of the text field to work normally (they are enabled based
		// on the "editable" member - which is set from the composite field), but we also need the normal onChange behavior to be enabled - so as the data is updated on the server even if the text field itself is read-only.
		// Because the onChange uses accessor methods, if we overwrite those to always return editable = true and read-only = false, we should
		// get the expected behavior.
		@Override
		public boolean isReadOnly()
		{
			return false;
		}

		@Override
		public boolean isEditable()
		{
			return true;
		}

		@Override
		public String getMarkupId()
		{
			return WebDataCompositeTextField.this.getMarkupId() + "compositefield";
		}

	}

}
