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
package com.servoy.j2db.ui.runtime;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.scripting.IScriptable;

/**
 * Interface for all runtime components
 *
 * @author jcompagner, rgansevles
 *
 * @since 6.1
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, publicName = "RuntimeComponent", scriptingName = "RuntimeComponent")
@SuppressWarnings("nls")
@ServoyClientSupport(ng = true, mc = false, wc = true, sc = true)
public interface IRuntimeComponent extends IScriptable, HasRuntimeFgBgColor, HasRuntimeVisible, HasRuntimeEnabled, HasRuntimeBorder, HasRuntimeTransparant,
	HasRuntimeTooltip, HasRuntimeFont, HasRuntimeSize, HasRuntimeLocation, HasRuntimeClientProperty, HasRuntimeName, HasRuntimeElementType,
	HasRuntimeDesignTimeProperty, HasRuntimeFormName, HasRuntimeStyleClass
{
	// types for getElementType

	public String BUTTON = "BUTTON";
	public String CALENDAR = "CALENDAR";
	public String CHECK = "CHECK";
	public String IMAGE_MEDIA = "IMAGE_MEDIA";
	public String LABEL = "LABEL";
	public String PASSWORD = "PASSWORD";
	public String PORTAL = "PORTAL";
	public String RADIOS = "RADIOS";
	public String TABPANEL = "TABPANEL";
	public String TEXT_AREA = "TEXT_AREA";
	public String TEXT_FIELD = "TEXT_FIELD";
	public String GROUP = "GROUP";
	public String COMBOBOX = "COMBOBOX";
	public String SPLITPANE = "SPLITPANE";
	public String RECTANGLE = "RECTANGLE";
	public String HTML_AREA = "HTML_AREA";
	public String RTF_AREA = "RTF_AREA";
	public String TYPE_AHEAD = "TYPE_AHEAD";
	@Deprecated
	public String LIST_BOX = "LIST_BOX";
	public String LISTBOX = "LIST_BOX";
	public String MULTISELECT_LISTBOX = "MULTISELECT_LISTBOX";
	public String FORM = "FORM";
	public String ACCORDIONPANEL = "ACCORDIONPANEL";
	public String SPINNER = "SPINNER";

}