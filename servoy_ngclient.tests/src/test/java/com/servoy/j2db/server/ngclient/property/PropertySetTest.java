/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2015 Servoy BV

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

package com.servoy.j2db.server.ngclient.property;

import java.awt.Color;
import java.awt.Dimension;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.sablo.InMemPackageReader;
import org.sablo.WebComponent;
import org.sablo.services.server.FormServiceHandler;

import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.server.ngclient.IWebFormController;
import com.servoy.j2db.util.ServoyException;

/**
 * @author jcompagner
 *
 */
public class PropertySetTest extends AbstractSolutionTest
{
	@Override
	protected void setupData() throws ServoyException
	{
	}

	@Override
	protected void fillTestSolution() throws ServoyException
	{
		Form form = solution.createNewForm(validator, null, "test", null, false, new Dimension(600, 400));
		form.setNavigatorID(-2);
		com.servoy.j2db.persistence.WebComponent bean = form.createNewWebComponent("mycustombean", "mycomponent");
		bean.setProperty("background", Color.black);
	}

	@Override
	protected InMemPackageReader getTestComponents() throws IOException
	{
		InputStream is = getClass().getResourceAsStream("PropertyTests.manifest");
		byte[] bytes = new byte[is.available()];
		is.read(bytes);
		String manifest = new String(bytes);
		is.close();

		is = getClass().getResourceAsStream("mycomponent.spec");
		bytes = new byte[is.available()];
		is.read(bytes);
		String comp1 = new String(bytes);
		is.close();

		HashMap<String, String> components = new HashMap<>();
		components.put("mycomponent.spec", comp1);
		InMemPackageReader inMemPackageReader = new InMemPackageReader(manifest, components);
		return inMemPackageReader;
	}

	@Test
	public void setColorProperty() throws Exception
	{
		IWebFormController form = (IWebFormController)client.getFormManager().showFormInCurrentContainer("test");
		Assert.assertNotNull(form);
		Collection<WebComponent> components = form.getFormUI().getComponents();
		Assert.assertEquals(1, components.size());
		WebComponent comp = components.iterator().next();

		Assert.assertEquals(Color.BLACK, comp.getProperty("background"));

		JSONObject json = new JSONObject();
		json.put("formname", "test");
		json.put("beanname", comp.getName());
		JSONObject changes = new JSONObject();
		changes.put("background", "#0000FF");
		json.put("changes", changes);

		FormServiceHandler.INSTANCE.executeMethod("dataPush", json);

		// should be changed.
		Assert.assertEquals(Color.BLUE, comp.getProperty("background"));

		changes.put("background", "#FF0000");
		JSONObject oldvalues = new JSONObject();
		oldvalues.put("background", "#0000FF");
		json.put("oldvalues", oldvalues);

		FormServiceHandler.INSTANCE.executeMethod("dataPush", json);

		// should be changed, old value was really the old value.
		Assert.assertEquals(Color.RED, comp.getProperty("background"));

		changes.put("background", "#00FF00");

		// should not be changed, still RED
		FormServiceHandler.INSTANCE.executeMethod("dataPush", json);
		Assert.assertEquals(Color.RED, comp.getProperty("background"));
	}
}
