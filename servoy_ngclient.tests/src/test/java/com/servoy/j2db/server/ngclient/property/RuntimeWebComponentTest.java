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
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;

import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.sablo.InMemPackageReader;

import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.scripting.FormScope;
import com.servoy.j2db.server.ngclient.IWebFormController;
import com.servoy.j2db.server.ngclient.component.RhinoMapOrArrayWrapper;
import com.servoy.j2db.util.ServoyException;

/**
 * @author jcompagner
 *
 */
@SuppressWarnings("nls")
public class RuntimeWebComponentTest extends AbstractSolutionTest
{

	@Override
	protected InMemPackageReader getTestComponents() throws IOException
	{
		InputStream is = getClass().getResourceAsStream("RuntimeWebComponentTest.manifest");
		byte[] bytes = new byte[is.available()];
		is.read(bytes);
		String manifest = new String(bytes);
		is.close();

		is = getClass().getResourceAsStream("RuntimeWebComponentTest-mycomponent.spec");
		bytes = new byte[is.available()];
		is.read(bytes);
		String comp1 = new String(bytes);
		is.close();

		HashMap<String, String> components = new HashMap<>();
		components.put("mycomponent.spec", comp1);
		InMemPackageReader inMemPackageReader = new InMemPackageReader(manifest, components);
		return inMemPackageReader;
	}

	@Override
	protected void fillTestSolution() throws ServoyException
	{
		try
		{
			Form form = solution.createNewForm(validator, null, "testForm", null, false, new Dimension(600, 400));
			form.createNewWebComponent("testComponent", "my-component");
			form.setNavigatorID(-1);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
			throw new ServoyException();
		}
	}


	@Override
	protected void setupData() throws ServoyException
	{
		// we don't use data yet for this test
	}

	@Test
	public void arrayPropAccessThroughGetterAndSetter() throws Exception
	{
		IWebFormController form = (IWebFormController)client.getFormManager().showFormInCurrentContainer("testForm");
		Assert.assertNotNull(form);
		FormScope formScope = form.getFormScope();

		Context cx = Context.enter();
		try
		{
			// CHECK INITIAL DEFAULT VALUE FROM SPEC
			RhinoMapOrArrayWrapper stringArrayProp = (RhinoMapOrArrayWrapper)cx.evaluateString(formScope, "elements.testComponent.getStringArray()",
				"Evaluation Test Script", 1, null); // this used to fail with an exception when RuntimeLegacyComponent gave null scope in getter code
			Assert.assertArrayEquals(new String[] { "a", "b", "c" }, ((List<String>)stringArrayProp.getWrappedValue()).toArray());

			// same check as above but directly inside Rhino
			Boolean testResult = (Boolean)cx.evaluateString(formScope,
				"(elements.testComponent.getStringArray().length === 3) && elements.testComponent.getStringArray().every(function(this_i, i) { return this_i == [\"a\", \"b\", \"c\"][i] } ) ",
				"Evaluation Test Script", 1, null);
			Assert.assertTrue(testResult.booleanValue());

			// ADD ELEMENT TO EXISTING VALUE
			cx.evaluateString(formScope, "elements.testComponent.getStringArray().push(\"d\") ", "Evaluation Test Script", 1, null);

			// CHECK CHANGED VALUE
			stringArrayProp = (RhinoMapOrArrayWrapper)cx.evaluateString(formScope, "elements.testComponent.getStringArray()", "Evaluation Test Script", 1,
				null);
			Assert.assertArrayEquals(new String[] { "a", "b", "c", "d" }, ((List<String>)stringArrayProp.getWrappedValue()).toArray());

			// same check as above but directly inside Rhino
			testResult = (Boolean)cx.evaluateString(formScope,
				"(elements.testComponent.getStringArray().length === 4) && elements.testComponent.getStringArray().every(function(this_i, i) { return this_i == [\"a\", \"b\", \"c\", \"d\"][i] } ) ",
				"Evaluation Test Script", 1, null);
			Assert.assertTrue(testResult.booleanValue());

			// ASSIGN DIFFERENT ARRAY BY REF TO PROPERTY
			cx.evaluateString(formScope, "elements.testComponent.setStringArray(['1', '2', '3'])", "Evaluation Test Script", 1, null);

			// CHECK NEW VALUE
			stringArrayProp = (RhinoMapOrArrayWrapper)cx.evaluateString(formScope, "elements.testComponent.getStringArray()", "Evaluation Test Script", 1,
				null);
			Assert.assertArrayEquals(new String[] { "1", "2", "3" }, ((List<String>)stringArrayProp.getWrappedValue()).toArray());

			// same check as above but directly inside Rhino
			testResult = (Boolean)cx.evaluateString(formScope,
				"(elements.testComponent.getStringArray().length === 3) && elements.testComponent.getStringArray().every(function(this_i, i) { return this_i == [\"1\", \"2\", \"3\"][i] } ) ",
				"Evaluation Test Script", 1, null);
			Assert.assertTrue(testResult.booleanValue());
		}
		finally
		{
			Context.exit();
		}
	}

	@Test
	public void arrayPropDirectAccess() throws Exception
	{
		IWebFormController form = (IWebFormController)client.getFormManager().showFormInCurrentContainer("testForm");
		Assert.assertNotNull(form);
		FormScope formScope = form.getFormScope();

		Context cx = Context.enter();
		try
		{
			// CHECK INITIAL DEFAULT VALUE FROM SPEC
			RhinoMapOrArrayWrapper stringArrayProp = (RhinoMapOrArrayWrapper)cx.evaluateString(formScope, "elements.testComponent.stringArray",
				"Evaluation Test Script", 1, null);
			Assert.assertArrayEquals(new String[] { "a", "b", "c" }, ((List<String>)stringArrayProp.getWrappedValue()).toArray());

			// same check as above but directly inside Rhino
			Boolean testResult = (Boolean)cx.evaluateString(formScope,
				"(elements.testComponent.stringArray.length === 3) && elements.testComponent.stringArray.every(function(this_i, i) { return this_i == [\"a\", \"b\", \"c\"][i] } ) ",
				"Evaluation Test Script", 1, null);
			Assert.assertTrue(testResult.booleanValue());

			// ADD ELEMENT TO EXISTING VALUE
			cx.evaluateString(formScope, "elements.testComponent.getStringArray().push(\"d\") ", "Evaluation Test Script", 1, null);

			// CHECK CHANGED VALUE
			stringArrayProp = (RhinoMapOrArrayWrapper)cx.evaluateString(formScope, "elements.testComponent.stringArray", "Evaluation Test Script", 1, null);
			Assert.assertArrayEquals(new String[] { "a", "b", "c", "d" }, ((List<String>)stringArrayProp.getWrappedValue()).toArray());

			// same check as above but directly inside Rhino
			testResult = (Boolean)cx.evaluateString(formScope,
				"(elements.testComponent.stringArray.length === 4) && elements.testComponent.stringArray.every(function(this_i, i) { return this_i == [\"a\", \"b\", \"c\", \"d\"][i] } ) ",
				"Evaluation Test Script", 1, null);
			Assert.assertTrue(testResult.booleanValue());

			// ASSIGN DIFFERENT ARRAY BY REF TO PROPERTY
			cx.evaluateString(formScope, "elements.testComponent.stringArray = ['1', '2', '3']", "Evaluation Test Script", 1, null);

			// CHECK NEW VALUE
			stringArrayProp = (RhinoMapOrArrayWrapper)cx.evaluateString(formScope, "elements.testComponent.stringArray", "Evaluation Test Script", 1, null);
			Assert.assertArrayEquals(new String[] { "1", "2", "3" }, ((List<String>)stringArrayProp.getWrappedValue()).toArray());

			// same check as above but directly inside Rhino
			testResult = (Boolean)cx.evaluateString(formScope,
				"(elements.testComponent.stringArray.length === 3) && elements.testComponent.stringArray.every(function(this_i, i) { return this_i == [\"1\", \"2\", \"3\"][i] } ) ",
				"Evaluation Test Script", 1, null);
			Assert.assertTrue(testResult.booleanValue());
		}
		finally
		{
			Context.exit();
		}
	}

}