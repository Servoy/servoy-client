/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

import java.awt.Dimension;
import java.awt.Point;
import java.io.IOException;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.sablo.InMemPackageReader;
import org.sablo.WebComponent;

import com.servoy.j2db.persistence.DummyValidator;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.server.ngclient.IWebFormController;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.UUID;

/**
 * @author jcompagner
 *
 */
@SuppressWarnings("nls")
public class EventCallTest extends AbstractSolutionTest
{
	private static final String uuid = UUID.randomUUID().toString();

	@Override
	protected InMemPackageReader getTestComponents() throws IOException
	{
		return null;
	}

	@Override
	protected void setupData() throws ServoyException
	{
	}

	@Override
	protected void fillTestSolution() throws ServoyException
	{
		Form form = solution.createNewForm(validator, null, "test", null, false, new Dimension(600, 400));
		form.setNavigatorID(-2);
		form.createNewScriptVariable(DummyValidator.INSTANCE, "testVar", IColumnTypes.INTEGER);
		GraphicalComponent button = form.createNewGraphicalComponent(new Point(10, 10));
		button.setShowClick(true);
		button.setShowFocus(true);
		ScriptMethod sm = form.createNewScriptMethod(DummyValidator.INSTANCE, "test");
		sm.setDeclaration("function test() {testVar = 10}");
		button.setOnActionMethodID(sm.getID());

		form.createNewScriptVariable(DummyValidator.INSTANCE, "testVar2", IColumnTypes.TEXT);
		GraphicalComponent button2 = form.createNewGraphicalComponent(new Point(10, 10));
		button2.setName(uuid);
		button2.setShowClick(true);
		button2.setShowFocus(true);
		ScriptMethod sm2 = form.createNewScriptMethod(DummyValidator.INSTANCE, "test2");
		sm2.setDeclaration("function test2() {testVar2 = elements['" + uuid + "'].getName()}");
		button2.setOnActionMethodID(sm2.getID());

	}


	@Test
	public void callButtonWithNoName()
	{
		IWebFormController form = (IWebFormController)client.getFormManager().showFormInCurrentContainer("test");
		Assert.assertNotNull(form);
		// fake incoming request for button click
		endpoint.incoming(
			"{\"service\":\"formService\",\"methodname\":\"executeEvent\",\"args\":{\"formname\":\"test\",\"beanname\":\"svy_3\",\"event\":\"onActionMethodID\",\"args\":[{\"type\":\"event\",\"eventName\":\"onActionMethodID\",\"modifiers\":0,\"timestamp\":1430912492641,\"x\":362,\"y\":207}],\"changes\":{}},\"cmsgid\":2}",
			true);

		Object object = form.getFormScope().get("testVar");
		Assert.assertEquals(10, ((Number)object).longValue());

	}


	@Test
	public void callButtonWithUUIDName()
	{
		IWebFormController form = (IWebFormController)client.getFormManager().showFormInCurrentContainer("test");
		Assert.assertNotNull(form);
		Collection<WebComponent> components = form.getFormUI().getComponents();
		String beanName = null;
		for (WebComponent webComponent : components)
		{
			String rawName = ((WebFormComponent)webComponent).getFormElement().getRawName();
			if (rawName != null && rawName.equals(uuid))
			{
				beanName = webComponent.getName();
				break;
			}
		}
		Assert.assertNotNull(beanName);
		// fake incoming request for button click
		endpoint.incoming(
			"{\"service\":\"formService\",\"methodname\":\"executeEvent\",\"args\":{\"formname\":\"test\",\"beanname\":\"" +
				beanName +
				"\",\"event\":\"onActionMethodID\",\"args\":[{\"type\":\"event\",\"eventName\":\"onActionMethodID\",\"modifiers\":0,\"timestamp\":1430912492641,\"x\":362,\"y\":207}],\"changes\":{}},\"cmsgid\":2}",
			true);

		Object object = form.getFormScope().get("testVar2");
		Assert.assertEquals(uuid, object);

	}

}
