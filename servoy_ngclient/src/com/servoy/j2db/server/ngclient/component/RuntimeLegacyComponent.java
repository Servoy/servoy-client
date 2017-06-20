/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

package com.servoy.j2db.server.ngclient.component;

import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.sablo.WebComponent;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebObjectSpecification;

import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.persistence.RectShape;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.TabPanel;
import com.servoy.j2db.scripting.IInstanceOf;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.property.types.DataproviderPropertyType;
import com.servoy.j2db.server.ngclient.property.types.NGConversions;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.ISabloComponentToRhino;
import com.servoy.j2db.server.ngclient.property.types.ValueListTypeSabloValue;
import com.servoy.j2db.server.ngclient.template.FormTemplateGenerator;
import com.servoy.j2db.ui.runtime.IRuntimeComponent;
import com.servoy.j2db.util.Utils;

/**
 * Runtime component for legacy scripting methods from default Servoy components.
 *
 * @author lvostinar
 *
 */
@SuppressWarnings("nls")
public class RuntimeLegacyComponent implements Scriptable, IInstanceOf
{
	private final WebFormComponent component;
	private final PutPropertyCallable putCallable;
	private final GetPropertyCallable getCallable;
	private final static Map<String, String> ScriptNameToSpecName;
	private final static Set<String> LegacyApiNames;
	private Map<Object, Object> clientProperties;
	private final WebObjectSpecification webComponentSpec;
	private Scriptable parentScope;

	static
	{
		ScriptNameToSpecName = new HashMap<String, String>();
		ScriptNameToSpecName.put("bgcolor", StaticContentSpecLoader.PROPERTY_BACKGROUND.getPropertyName());
		ScriptNameToSpecName.put("fgcolor", StaticContentSpecLoader.PROPERTY_FOREGROUND.getPropertyName());
		ScriptNameToSpecName.put("width", StaticContentSpecLoader.PROPERTY_SIZE.getPropertyName());
		ScriptNameToSpecName.put("height", StaticContentSpecLoader.PROPERTY_SIZE.getPropertyName());
		ScriptNameToSpecName.put("locationX", StaticContentSpecLoader.PROPERTY_LOCATION.getPropertyName());
		ScriptNameToSpecName.put("locationY", StaticContentSpecLoader.PROPERTY_LOCATION.getPropertyName());
		ScriptNameToSpecName.put("border", StaticContentSpecLoader.PROPERTY_BORDERTYPE.getPropertyName());
		ScriptNameToSpecName.put("font", StaticContentSpecLoader.PROPERTY_FONTTYPE.getPropertyName());
		ScriptNameToSpecName.put("imageURL", StaticContentSpecLoader.PROPERTY_IMAGEMEDIAID.getPropertyName());
		ScriptNameToSpecName.put("rolloverImageURL", StaticContentSpecLoader.PROPERTY_ROLLOVERIMAGEMEDIAID.getPropertyName());
		ScriptNameToSpecName.put("valueListItems", StaticContentSpecLoader.PROPERTY_VALUELISTID.getPropertyName());
		ScriptNameToSpecName.put("valueListName", StaticContentSpecLoader.PROPERTY_VALUELISTID.getPropertyName());
		ScriptNameToSpecName.put("titleText", "text");

		LegacyApiNames = new HashSet<>();
		LegacyApiNames.add("putClientProperty");
		LegacyApiNames.add("getLabelForElementNames");
		LegacyApiNames.add("getLabelForElementName");
		LegacyApiNames.add("getElementType");
		LegacyApiNames.add("getName");
		LegacyApiNames.add("getValueListName");
		LegacyApiNames.add("getDesignTimeProperty");
		LegacyApiNames.add("getLocationX");
		LegacyApiNames.add("getLocationY");
		LegacyApiNames.add("getWidth");
		LegacyApiNames.add("getHeight");
		LegacyApiNames.add("getDataProviderID");
	}

	public RuntimeLegacyComponent(WebFormComponent component)
	{
		this.component = component;
		setParentScope(component.getDataConverterContext().getApplication().getScriptEngine().getSolutionScope());
		putCallable = new PutPropertyCallable(this);
		getCallable = new GetPropertyCallable(this);
		this.webComponentSpec = component.getFormElement().getWebComponentSpec();
	}

	@Override
	public boolean isInstance(String name)
	{
		return name.equals(getRuntimeName());
	}

	private String getRuntimeName()
	{
		IPersist persist = component.getFormElement().getPersistIfAvailable();
		if (persist instanceof GraphicalComponent)
		{
			boolean noData = ((GraphicalComponent)persist).getDataProviderID() == null && !((GraphicalComponent)persist).getDisplaysTags();
			if (com.servoy.j2db.component.ComponentFactory.isButton((GraphicalComponent)persist))
			{
				return noData ? "RuntimeButton" : "RuntimeDataButton";
			}
			return noData ? "RuntimeLabel" : "RuntimeDataLabel";
		}
		if (persist instanceof Field)
		{
			switch (((Field)persist).getDisplayType())
			{
				case Field.COMBOBOX :
					return "RuntimeComboBox";
				case Field.TEXT_FIELD :
				case Field.TYPE_AHEAD :
					return "RuntimeTextField";
				case Field.RADIOS :
					return FormTemplateGenerator.isSingleValueComponent((IFormElement)persist) ? "RuntimeRadio" : "RuntimeRadios";
				case Field.CHECKS :
					return FormTemplateGenerator.isSingleValueComponent((IFormElement)persist) ? "RuntimeCheck" : "RuntimeChecks";
				case Field.CALENDAR :
					return "RuntimeCalendar";
				case Field.TEXT_AREA :
					return "RuntimeTextArea";
				case Field.PASSWORD :
					return "RuntimePassword";
				case Field.SPINNER :
					return "RuntimeSpinner";
				case Field.LIST_BOX :
				case Field.MULTISELECT_LISTBOX :
					return ";RuntimeListBox";
				case Field.IMAGE_MEDIA :
					return "RuntimeMediaField";
				case Field.HTML_AREA :
					return "RuntimeHtmlArea";
			}
		}
		if (persist instanceof TabPanel)
		{
			int orient = ((TabPanel)persist).getTabOrientation();
			if (orient == TabPanel.SPLIT_HORIZONTAL || orient == TabPanel.SPLIT_VERTICAL) return "RuntimeSplitPane";
			if (orient == TabPanel.ACCORDION_PANEL) return "RuntimeAccordionPanel";
			return "RuntimeTabPanel";
		}
		if (persist instanceof Portal)
		{
			return "RuntimePortal";
		}
		return null;
	}

	@Override
	public String getClassName()
	{
		return null;
	}

	@Override
	public Object get(String name, Scriptable start)
	{
		if ("putClientProperty".equals(name))
		{
			putCallable.setProperty("clientProperty");
			return putCallable;
		}

		if ("addStyleClass".equals(name) || "removeStyleClass".equals(name))
		{
			final String nameFinal = name;
			return new Callable()
			{
				@Override
				public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args)
				{
					if (webComponentSpec.getProperty(StaticContentSpecLoader.PROPERTY_STYLECLASS.getPropertyName()) != null)
					{
						String styleClass = (String)component.getProperty(StaticContentSpecLoader.PROPERTY_STYLECLASS.getPropertyName());
						if (args != null && args.length > 0 && args[0] != null)
						{
							if (styleClass == null) styleClass = "";
							if ("addStyleClass".equals(nameFinal))
							{
								if (!Pattern.compile("(?<!\\S)\\b" + Pattern.quote(args[0].toString()) + "\\b(?!\\S)").matcher(styleClass).find())
								{
									styleClass = styleClass + " " + args[0];
								}
							}
							else
							{
								styleClass = styleClass.replaceAll("(?<!\\S)\\b" + Pattern.quote(args[0].toString()) + "\\b(?!\\S)", "");
							}
							component.setProperty(StaticContentSpecLoader.PROPERTY_STYLECLASS.getPropertyName(), styleClass);
						}
					}
					return null;
				}
			};
		}

		if (name.startsWith("get") || name.startsWith("is") || name.startsWith("set"))
		{
			String newName = generatePropertyName(name);
			if (name.startsWith("set"))
			{
				putCallable.setProperty(newName);
				return putCallable;
			}
			else
			{
				getCallable.setProperty(newName);
				return getCallable;
			}
		}
		boolean isReadonly = false;
		if (name.equals("readOnly"))
		{
			isReadonly = true;
			name = StaticContentSpecLoader.PROPERTY_EDITABLE.getPropertyName();
		}

		if (component.isDesignOnlyProperty(name) || component.isPrivateProperty(name))
		{
			// cannot get design only or private properties; make an exception for dp properties, should be able to get the dpid
			if (!(component.getSpecification().getProperty(name).getType() instanceof DataproviderPropertyType))
			{
				return Scriptable.NOT_FOUND;
			}
		}

		Object value = convertValue(name, component.getProperty(convertName(name)), webComponentSpec.getProperties().get(convertName(name)), start);

		if (isReadonly && value instanceof Boolean)
		{
			return !((Boolean)value).booleanValue();
		}

		return value;
	}

	protected String generatePropertyName(String name)
	{
		String newName = name.substring(name.startsWith("is") ? 2 : 3);
		// Make the bean property name.
		char ch0 = newName.charAt(0);
		if (Character.isUpperCase(ch0))
		{
			if (newName.length() == 1)
			{
				newName = newName.toLowerCase();
			}
			else
			{
				char ch1 = newName.charAt(1);
				if (!Character.isUpperCase(ch1))
				{
					newName = Character.toLowerCase(ch0) + newName.substring(1);
				}
			}
		}
		return newName;
	}

	@Override
	public Object get(int index, Scriptable start)
	{
		return null;
	}

	@Override
	public boolean has(String name, Scriptable start)
	{
		if (component.isDesignOnlyProperty(name) || component.isPrivateProperty(name)) return false;
		if (name.equals("readOnly") || LegacyApiNames.contains(name) || ScriptNameToSpecName.containsKey(name)) return true;
		if (webComponentSpec.getApiFunction(name) != null) return true;
		if (webComponentSpec.getProperty(name) != null) return true;
		if (name.startsWith("get") || name.startsWith("is") || name.startsWith("set"))
		{
			String newName = generatePropertyName(name);
			if (webComponentSpec.getProperty(newName) != null) return true;
		}
		if ("addStyleClass".equals(name) || "removeStyleClass".equals(name))
		{
			if (webComponentSpec.getProperty(StaticContentSpecLoader.PROPERTY_STYLECLASS.getPropertyName()) != null) return true;
		}
		return false;
	}

	@Override
	public boolean has(int index, Scriptable start)
	{
		return false;
	}

	@Override
	public void put(String name, Scriptable start, Object value)
	{
		if (name.equals("readOnly"))
		{
			name = StaticContentSpecLoader.PROPERTY_EDITABLE.getPropertyName();
			if (value instanceof Boolean)
			{
				value = !((Boolean)value).booleanValue();
			}
		}
		name = convertName(name);
		if (component.isDesignOnlyProperty(name) && !StaticContentSpecLoader.PROPERTY_VALUELISTID.getPropertyName().equals(name))
		{
			// cannot set design only or private properties
			return;
		}

		Object previousVal = component.getProperty(name);
		Object val = NGConversions.INSTANCE.convertRhinoToSabloComponentValue(value, previousVal, webComponentSpec.getProperties().get(name), component);

		if (val != previousVal) component.setProperty(name, val);
		// force size & location push as that maybe different on the client (if form anchored or table columns were changed as width or location)
		if ("size".equals(name) || "location".equals(name))
		{
			component.flagPropertyAsDirty(name, true);
		}
	}

	@Override
	public void put(int index, Scriptable start, Object value)
	{

	}

	@Override
	public void delete(String name)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void delete(int index)
	{

	}

	@Override
	public Scriptable getPrototype()
	{
		return null;
	}

	@Override
	public void setPrototype(Scriptable prototype)
	{

	}

	@Override
	public Scriptable getParentScope()
	{
		return parentScope;
	}

	@Override
	public void setParentScope(Scriptable parent)
	{
		parentScope = parent;
	}

	@Override
	public Object[] getIds()
	{
		return LegacyApiNames.toArray();
	}

	@Override
	public Object getDefaultValue(Class< ? > hint)
	{
		return null;
	}

	@Override
	public boolean hasInstance(Scriptable instance)
	{
		return false;
	}

	private String convertName(String name)
	{
		if (ScriptNameToSpecName.containsKey(name))
		{
			return ScriptNameToSpecName.get(name);
		}
		return name;
	}

	private Object convertValue(String name, Object value, PropertyDescription pd, Scriptable start)
	{
		if ("width".equals(name) && value instanceof Dimension)
		{
			return Integer.valueOf(((Dimension)value).width);
		}
		if ("height".equals(name) && value instanceof Dimension)
		{
			return Integer.valueOf(((Dimension)value).height);
		}
		if ("locationX".equals(name) && value instanceof Point)
		{
			return Integer.valueOf(((Point)value).x);
		}
		if ("locationY".equals(name) && value instanceof Point)
		{
			return Integer.valueOf(((Point)value).y);
		}
		if (pd != null && pd.getType() instanceof ISabloComponentToRhino< ? >)
		{
			return ((ISabloComponentToRhino)pd.getType()).toRhinoValue(value, pd, component, start);
		}
		return value;
	}

	private boolean needsValueConversion(String name)
	{
		if ("width".equals(name) || "height".equals(name) || "locationX".equals(name) || "locationY".equals(name))
		{
			return true;
		}
		return false;
	}

	public void putClientProperty(Object key, Object value)
	{
		if (clientProperties == null)
		{
			clientProperties = new HashMap<Object, Object>();
		}
		clientProperties.put(key, value);
	}

	public Object getClientProperty(Object key)
	{
		if (clientProperties == null) return null;
		return clientProperties.get(key);
	}

	private abstract class PropertyCallable extends BaseFunction
	{
		protected final Scriptable scriptable;
		protected String propertyName;

		public PropertyCallable(Scriptable scriptable)
		{
			this.scriptable = scriptable;
		}

		public void setProperty(String propertyName)
		{
			this.propertyName = propertyName;
		}
	}

	private class PutPropertyCallable extends PropertyCallable
	{
		public PutPropertyCallable(Scriptable scriptable)
		{
			super(scriptable);
		}

		@Override
		public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args)
		{
			Object value = args;
			if (args != null && args.length == 1)
			{
				value = args[0];
			}
			if ("clientProperty".equals(propertyName) && args != null && args.length >= 2)
			{
				putClientProperty(args[0], args[1]);
				value = clientProperties;
			}
//			if (StaticContentSpecLoader.PROPERTY_LOCATION.getPropertyName().equals(propertyName))
//			{
//				scriptable.put(StaticContentSpecLoader.PROPERTY_ANCHORS.getPropertyName(), null, Integer.valueOf(IAnchorConstants.DEFAULT));
//			}
			scriptable.put(propertyName, null, value);
			return null;
		}
	}

	private class GetPropertyCallable extends PropertyCallable
	{
		public GetPropertyCallable(Scriptable scriptable)
		{
			super(scriptable);
		}

		@Override
		public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args)
		{
			if (propertyName.equals("absoluteFormLocationY") && component.getFormElement().getPersistIfAvailable() instanceof IFormElement) //$NON-NLS-1$
			{
				int y = ((Integer)scriptable.get("locationY", null)).intValue();
				IFormElement fe = (IFormElement)component.getFormElement().getPersistIfAvailable();
				Form f = component.getFormElement().getForm();
				Part p = f.getPartAt(fe.getLocation().y);
				int partStartY = 0;
				if (p != null)
				{
					partStartY = f.getPartStartYPos(p.getID());
				}
				return Integer.valueOf(partStartY + y);
			}

			if ("clientProperty".equals(propertyName) && args != null && args.length > 0)
			{
				return getClientProperty(args[0]);
			}
			if ("dataProviderID".equals(propertyName))
			{
				// return the design value string (so the dataprovider name, not its value)
				return component.getFormElement().getPropertyValue(propertyName);
			}
			if ("labelForElementName".equals(propertyName))
			{
				if (component.getFormElement().getPersistIfAvailable() instanceof GraphicalComponent)
				{
					GraphicalComponent gc = (GraphicalComponent)component.getFormElement().getPersistIfAvailable();
					String labelFor = gc.getLabelFor();
					if (labelFor != null)
					{
						WebComponent comp = component.getParent().getComponent(labelFor);
						if (comp != null)
						{
							return comp.getName();
						}
					}
				}
				return null;
			}
			if ("labelForElementNames".equals(propertyName))
			{
				if (component.getFormElement().getPersistIfAvailable() instanceof Field)
				{
					Field field = (Field)component.getFormElement().getPersistIfAvailable();
					String name = field.getName();
					if (name != null)
					{
						List<String> labelFors = new ArrayList<String>();
						for (WebComponent comp : component.getParent().getComponents())
						{
							if (comp instanceof WebFormComponent)
							{
								WebFormComponent c = (WebFormComponent)comp;
								if (c.getFormElement().getPersistIfAvailable() instanceof GraphicalComponent &&
									Utils.equalObjects(name, ((GraphicalComponent)c.getFormElement().getPersistIfAvailable()).getLabelFor()) &&
									((GraphicalComponent)c.getFormElement().getPersistIfAvailable()).getName() != null)
								{
									labelFors.add(c.getName());
								}
							}
						}
						return labelFors.toArray(new String[0]);
					}
				}
				return new String[0];
			}
			if ("elementType".equals(propertyName))
			{
				// return the design value string (so the dataprovider name, not its value)
				IPersist persist = component.getFormElement().getPersistIfAvailable();
				if (persist instanceof GraphicalComponent)
				{
					if (com.servoy.j2db.component.ComponentFactory.isButton((GraphicalComponent)persist))
					{
						return IRuntimeComponent.BUTTON;
					}
					return IRuntimeComponent.LABEL;
				}
				if (persist instanceof Field)
				{
					switch (((Field)persist).getDisplayType())
					{
						case Field.COMBOBOX :
							return IRuntimeComponent.COMBOBOX;
						case Field.TEXT_FIELD :
							return IRuntimeComponent.TEXT_FIELD;
						case Field.RADIOS :
							return IRuntimeComponent.RADIOS;
						case Field.CHECKS :
							return IRuntimeComponent.CHECK;
						case Field.CALENDAR :
							return IRuntimeComponent.CALENDAR;
						case Field.TYPE_AHEAD :
							return IRuntimeComponent.TYPE_AHEAD;
						case Field.TEXT_AREA :
							return IRuntimeComponent.TEXT_AREA;
						case Field.PASSWORD :
							return IRuntimeComponent.PASSWORD;
						case Field.SPINNER :
							return IRuntimeComponent.SPINNER;
						case Field.LIST_BOX :
							return IRuntimeComponent.LISTBOX;
						case Field.MULTISELECT_LISTBOX :
							return IRuntimeComponent.MULTISELECT_LISTBOX;
						case Field.IMAGE_MEDIA :
							return IRuntimeComponent.IMAGE_MEDIA;
						case Field.HTML_AREA :
							return IRuntimeComponent.HTML_AREA;
					}
				}
				if (persist instanceof TabPanel)
				{
					int orient = ((TabPanel)persist).getTabOrientation();
					if (orient == TabPanel.SPLIT_HORIZONTAL || orient == TabPanel.SPLIT_VERTICAL) return IRuntimeComponent.SPLITPANE;
					if (orient == TabPanel.ACCORDION_PANEL) return IRuntimeComponent.ACCORDIONPANEL;
					return IRuntimeComponent.TABPANEL;
				}
				if (persist instanceof Portal)
				{
					return IRuntimeComponent.PORTAL;
				}
				if (persist instanceof RectShape)
				{
					return IRuntimeComponent.RECTANGLE;
				}
			}

			if ("name".equals(propertyName))
			{
				IPersist persist = component.getFormElement().getPersistIfAvailable();
				if (persist instanceof ISupportName)
				{
					String jsName = ((ISupportName)persist).getName();
					if (jsName != null && jsName.startsWith(ComponentFactory.WEB_ID_PREFIX)) jsName = null;
					return jsName;
				}
				return null;
			}

			if (propertyName.equals("designTimeProperty") && args != null && args.length > 0 &&
				component.getFormElement().getPersistIfAvailable() instanceof AbstractBase)
			{
				return Utils.parseJSExpression(((AbstractBase)component.getFormElement().getPersistIfAvailable()).getCustomDesignTimeProperty((String)args[0]));
			}

			if ("valueListName".equals(propertyName))
			{
				Object vl = component.getProperty(convertName(propertyName));
				if (vl != null)
				{
					ValueListTypeSabloValue value = (ValueListTypeSabloValue)vl;
					if (value.getValueList() != null) return value.getValueList().getName();
				}
			}
			return scriptable.get(propertyName, null);
		}
	}
}
