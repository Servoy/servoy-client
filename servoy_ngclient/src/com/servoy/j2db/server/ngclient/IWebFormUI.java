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

package com.servoy.j2db.server.ngclient;

import java.util.Collection;
import java.util.List;

import org.json.JSONException;
import org.json.JSONWriter;
import org.sablo.IChangeListener;
import org.sablo.WebComponent;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.websocket.utils.JSONUtils.IToJSONConverter;

import com.servoy.j2db.IBasicFormUI;
import com.servoy.j2db.IView;
import com.servoy.j2db.server.ngclient.component.RuntimeWebComponent;

/**
 * @author jcompagner
 *
 */
public interface IWebFormUI extends IBasicFormUI, IView, IChangeListener
{

	boolean writeAllComponentsProperties(JSONWriter w, IToJSONConverter<IBrowserConverterContext> converter) throws JSONException;

	WebFormComponent getWebComponent(String name);

	Collection<WebComponent> getComponents();

	IDataAdapterList getDataAdapterList();

	void init();

	void setReadOnly(boolean readOnly);

	void setParentContainer(WebFormComponent parentContainer);

	String getParentWindowName();

	void setParentWindowName(String parentWindowName);

	Object getParentContainer();

	IServoyDataConverterContext getDataConverterContext();

	void contributeComponentToElementsScope(FormElement fe, WebObjectSpecification componentSpec, WebFormComponent component);

	void removeComponentFromElementsScope(FormElement element, WebObjectSpecification webComponentSpec, WebFormComponent childComponent);

	RuntimeWebComponent getRuntimeWebComponent(String name);

	@Override
	public IWebFormController getController();

	boolean notifyVisible(boolean visible, List<Runnable> invokeLaterRunnables);
}
