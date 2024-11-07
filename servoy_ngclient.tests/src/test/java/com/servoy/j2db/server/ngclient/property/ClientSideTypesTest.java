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
import java.util.Queue;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.sablo.InMemPackageReader;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import com.servoy.base.solutionmodel.IBaseSMPart;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.util.ServoyException;

/**
 * @author acostescu
 */
@SuppressWarnings("nls")
public class ClientSideTypesTest extends AbstractSolutionTest
{

	@Override
	protected InMemPackageReader getTestComponents() throws IOException
	{
		InputStream is = getClass().getResourceAsStream("ClientSideTypesTestComponents.manifest");
		byte[] bytes = new byte[is.available()];
		is.read(bytes);
		String manifest = new String(bytes);
		is.close();

		is = getClass().getResourceAsStream("ClientSideTypesTest-component1.spec");
		bytes = new byte[is.available()];
		is.read(bytes);
		String comp1 = new String(bytes);
		is.close();

		is = getClass().getResourceAsStream("ClientSideTypesTest-component2.spec");
		bytes = new byte[is.available()];
		is.read(bytes);
		String comp2 = new String(bytes);
		is.close();

		HashMap<String, String> components = new HashMap<>();
		components.put("component1.spec", comp1);
		components.put("component2.spec", comp2);
		InMemPackageReader inMemPackageReader = new InMemPackageReader(manifest, components);
		return inMemPackageReader;
	}

	@Override
	protected InMemPackageReader getTestServices() throws IOException
	{
		InputStream is = getClass().getResourceAsStream("ClientSideTypesTestServices.manifest");
		byte[] bytes = new byte[is.available()];
		is.read(bytes);
		String manifest = new String(bytes);
		is.close();

		is = getClass().getResourceAsStream("ClientSideTypesTest-service1.spec");
		bytes = new byte[is.available()];
		is.read(bytes);
		String service1 = new String(bytes);
		is.close();

		HashMap<String, String> services = new HashMap<>();
		services.put("service1.spec", service1);
		InMemPackageReader inMemPackageReader = new InMemPackageReader(manifest, services);
		return inMemPackageReader;
	}

	@Override
	protected void fillTestSolution() throws ServoyException
	{
		try
		{
			Form form = solution.createNewForm(validator, null, "dummyForm", null, false, new Dimension(600, 400));
			form.setNavigatorID(-1);
			form.createNewPart(IBaseSMPart.BODY, 5);
			solution.setFirstFormID(form.getID()); // just a dummy form as first form so that it doesn't initialize our first form too soon, when solution is first shown (the test form will still get populated with design-time content at the beginning of tests)

			form = solution.createNewForm(validator, null, "comp1Form", null, false, new Dimension(600, 400));
			form.setNavigatorID(-1);
			form.createNewPart(IBaseSMPart.BODY, 5);
			form.createNewWebComponent("comp1", "component1");

			form = solution.createNewForm(validator, null, "comp2Form", null, false, new Dimension(600, 400));
			form.setNavigatorID(-1);
			form.createNewPart(IBaseSMPart.BODY, 5);
			form.createNewWebComponent("comp2", "component2");

			form = solution.createNewForm(validator, null, "comp12Form", null, false, new Dimension(600, 400));
			form.setNavigatorID(-1);
			form.createNewPart(IBaseSMPart.BODY, 5);
			form.createNewWebComponent("comp1", "component1");
			form.createNewWebComponent("comp2", "component2");

			form = solution.createNewForm(validator, null, "comp3Form", null, false, new Dimension(600, 400));
			form.setNavigatorID(-1);
			form.createNewPart(IBaseSMPart.BODY, 5);
			form.createNewWebComponent("comp3", "component3");
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
	}

	@Test
	public void serviceClientSideTypeShoulAlwaysGetSent()
	{
		Queue<String> sentTextMessages = endpoint.getSession().getBasicRemote().getAndClearSentTextMessages();
		Assert.assertTrue(sentTextMessages.size() > 0);
		// searching for something like 2#{"msg":{},"services":[{"call":"setServiceClientSideConversionTypes","args":...,"name":"$sabloService"}]}
		String msg;
		boolean found = false;
		while (!found && ((msg = sentTextMessages.poll()) != null))
		{
			int indexOfHash = msg.indexOf("#");
			if (indexOfHash >= 0)
			{
				JSONObject msgJSON = new JSONObject(msg.substring(indexOfHash + 1));
				if (msgJSON.has("serviceApis") && msgJSON.getJSONArray("serviceApis").length() == 1 &&
					msgJSON.getJSONArray("serviceApis").getJSONObject(0).getString("name").equals("$typesRegistry") &&
					msgJSON.getJSONArray("serviceApis").getJSONObject(0).getString("call").equals("setServiceClientSideSpecs"))
				{
					JSONObject args = msgJSON.getJSONArray("serviceApis").getJSONObject(0).getJSONArray("args").getJSONObject(0);
					// check that our test service client side PDs are sent correctly
					JSONAssert.assertEquals(new JSONObject(
						"{\"p\":{\"arrayOfCustomType\":[\"JSON_arr\",[\"JSON_obj\",\"service1.mytype\"]],\"customType\":[\"JSON_obj\",\"service1.mytype\"],\"octWithCustomTypeAllow\":{\"t\":[\"JSON_obj\",\"service1.octWithCustomType\"],\"s\":1},\"someDate\":{\"t\":\"svy_date\",\"s\":1},\"stringArray\":[\"JSON_arr\",{\"t\":null,\"s\":2}],\"arrayOfCustomTypeWithAllow\":{\"t\":[\"JSON_arr\",[\"JSON_obj\",\"service1.mytype\"]],\"s\":1},\"arrayWithOctAllowAndShallowEl\":{\"t\":[\"JSON_arr\",{\"t\":[\"JSON_obj\",\"service1.octWithCustomType\"],\"s\":2}],\"s\":1}},\"ftd\":{\"JSON_obj\":{\"service1.mytype\":{\"someComponent\":\"component\"},\"service1.octWithCustomType\":{\"customType\":{\"t\":[\"JSON_obj\",\"service1.mytype\"],\"s\":3},\"someString\":{\"s\":0}}}}}"),
						args.getJSONObject("service1"), JSONCompareMode.NON_EXTENSIBLE);
					found = true;
				}
			}
		}

		Assert.assertTrue("Service client side types should have been sent initially and contain the service client side types", found);
	}

	@Test
	public void componentClientSideTypesShoulGetSentWhenNeeded()
	{
		// show first form and see second component spec. being sent to client
		client.getFormManager().showFormInCurrentContainer("comp1Form");

		Queue<String> sentTextMessages = endpoint.getSession().getBasicRemote().getAndClearSentTextMessages();
		Assert.assertTrue(sentTextMessages.size() > 0);
		// searching for something like 2#{"msg":{},"services":[{"call":"setServiceClientSideConversionTypes","args":...,"name":"$sabloService"}]}
		String msg;
		boolean found = false;
		while (!found && ((msg = sentTextMessages.poll()) != null))
		{
			int indexOfHash = msg.indexOf("#");
			if (indexOfHash >= 0)
			{
				JSONObject msgJSON = new JSONObject(msg.substring(indexOfHash + 1));
				if (msgJSON.has("serviceApis") && msgJSON.getJSONArray("serviceApis").length() == 2 &&
					msgJSON.getJSONArray("serviceApis").getJSONObject(0).getString("name").equals("$typesRegistry") &&
					msgJSON.getJSONArray("serviceApis").getJSONObject(0).getString("call").equals("addComponentClientSideSpecs"))
				{
					JSONObject args = msgJSON.getJSONArray("serviceApis").getJSONObject(0).getJSONArray("args").getJSONObject(0);
					// check that our test service client side PDs are sent correctly
					JSONAssert.assertEquals(new JSONObject(
						"{\"p\":{\"arrayOfCustomType\":[\"JSON_arr\",[\"JSON_obj\",\"component1.mytype\"]],\"customType\":[\"JSON_obj\",\"component1.mytype\"],\"octWithCustomTypeAllow\":{\"s\":1,\"t\":[\"JSON_obj\",\"component1.octWithCustomType\"]},\"someDate\":{\"s\":1,\"t\":\"svy_date\"},\"stringArray\":[\"JSON_arr\",{\"t\":null,\"s\":2}],\"arrayOfCustomTypeWithAllow\":{\"s\":1,\"t\":[\"JSON_arr\",[\"JSON_obj\",\"component1.mytype\"]]},\"arrayWithOctAllowAndShallowEl\":{\"s\":1,\"t\":[\"JSON_arr\",{\"t\":[\"JSON_obj\",\"component1.octWithCustomType\"],\"s\":2}]}},\"ftd\":{\"JSON_obj\":{\"component1.octWithCustomType\":{\"customType\":{\"s\":3,\"t\":[\"JSON_obj\",\"component1.mytype\"]},\"someString\":{\"s\":0}},\"component1.mytype\":{\"someComponent\":\"component\"}}}}"),
						args.getJSONObject("component1"), JSONCompareMode.NON_EXTENSIBLE);
					found = true;
				}
			}
		}

		Assert.assertTrue("Component client side types should have been sent when a form using that component was shown", found);

		// OK, now show second form and see second component spec being sent to client
		client.getFormManager().showFormInCurrentContainer("comp2Form");

		sentTextMessages = endpoint.getSession().getBasicRemote().getAndClearSentTextMessages();
		Assert.assertTrue(sentTextMessages.size() > 0);
		// searching for something like 2#{"msg":{},"services":[{"call":"setServiceClientSideConversionTypes","args":...,"name":"$sabloService"}]}
		found = false;
		while (!found && ((msg = sentTextMessages.poll()) != null))
		{
			int indexOfHash = msg.indexOf("#");
			if (indexOfHash >= 0)
			{
				JSONObject msgJSON = new JSONObject(msg.substring(indexOfHash + 1));
				if (msgJSON.has("serviceApis") && msgJSON.getJSONArray("serviceApis").length() == 2 &&
					msgJSON.getJSONArray("serviceApis").getJSONObject(0).getString("name").equals("$typesRegistry") &&
					msgJSON.getJSONArray("serviceApis").getJSONObject(0).getString("call").equals("addComponentClientSideSpecs"))
				{
					JSONObject args = msgJSON.getJSONArray("serviceApis").getJSONObject(0).getJSONArray("args").getJSONObject(0);
					// check that our test service client side PDs are sent correctly
					JSONAssert.assertEquals(new JSONObject(
						"{\"p\":{\"arrayOfCustomType\":[\"JSON_arr\",[\"JSON_obj\",\"component2.mytype\"]],\"customType\":[\"JSON_obj\",\"component2.mytype\"],\"octWithCustomTypeAllow\":{\"s\":1,\"t\":[\"JSON_obj\",\"component2.octWithCustomType\"]},\"someDate\":{\"s\":1,\"t\":\"svy_date\"},\"stringArray\":[\"JSON_arr\",{\"t\":null,\"s\":2}],\"arrayOfCustomTypeWithAllow\":{\"s\":1,\"t\":[\"JSON_arr\",[\"JSON_obj\",\"component2.mytype\"]]},\"arrayWithOctAllowAndShallowEl\":{\"s\":1,\"t\":[\"JSON_arr\",{\"t\":[\"JSON_obj\",\"component2.octWithCustomType\"],\"s\":2}]}},\"ftd\":{\"JSON_obj\":{\"component2.octWithCustomType\":{\"customType\":{\"s\":3,\"t\":[\"JSON_obj\",\"component2.mytype\"]},\"someString\":{\"s\":0}},\"component2.mytype\":{\"someComponent\":\"component\"}}}}"),
						args.getJSONObject("component2"), JSONCompareMode.NON_EXTENSIBLE);
					found = true;
				}
			}
		}

		Assert.assertTrue("Component client side types should have been sent when a form using that component was shown", found);

		// OK now show a third form that has both components - they are both already on client so it shouldn't send anything anymore
		client.getFormManager().showFormInCurrentContainer("comp12Form");

		sentTextMessages = endpoint.getSession().getBasicRemote().getAndClearSentTextMessages();
		found = false;
		while (!found && ((msg = sentTextMessages.poll()) != null))
		{
			int indexOfHash = msg.indexOf("#");
			if (indexOfHash >= 0)
			{
				JSONObject msgJSON = new JSONObject(msg.substring(indexOfHash + 1));
				if (msgJSON.has("services") && msgJSON.getJSONArray("services").length() == 1 &&
					msgJSON.getJSONArray("services").getJSONObject(0).getString("name").equals("$sabloService") &&
					msgJSON.getJSONArray("services").getJSONObject(0).getString("call").equals("addComponentClientSideConversionTypes"))
				{
					found = true;
				}
			}
		}

		Assert.assertFalse("Component client side types should have been sent already previously; and they should not be sent again", found);

		// OK now show a fourth form that has a component with no client side types
		client.getFormManager().showFormInCurrentContainer("comp3Form");

		sentTextMessages = endpoint.getSession().getBasicRemote().getAndClearSentTextMessages();
		found = false;
		while (!found && ((msg = sentTextMessages.poll()) != null))
		{
			int indexOfHash = msg.indexOf("#");
			if (indexOfHash >= 0)
			{
				JSONObject msgJSON = new JSONObject(msg.substring(indexOfHash + 1));
				if (msgJSON.has("serviceApis") && msgJSON.getJSONArray("serviceApis").length() == 1 &&
					msgJSON.getJSONArray("serviceApis").getJSONObject(0).getString("name").equals("$sabloService") &&
					msgJSON.getJSONArray("serviceApis").getJSONObject(0).getString("call").equals("addComponentClientSideConversionTypes"))
				{
					found = true;
				}
			}
		}

		Assert.assertFalse("Component client side types should have been sent already previously; and they should not be sent again", found);
	}

}
