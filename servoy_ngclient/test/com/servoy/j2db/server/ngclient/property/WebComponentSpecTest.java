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

package com.servoy.j2db.server.ngclient.property;

import java.util.Collection;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sablo.InMemPackageReader;
import org.sablo.IndexPageEnhancer;
import org.sablo.specification.Package.IPackageReader;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.specification.WebServiceSpecProvider;
import org.sablo.specification.property.CustomJSONArrayType;
import org.sablo.specification.property.CustomJSONPropertyType;
import org.sablo.specification.property.ICustomType;
import org.sablo.specification.property.types.BooleanPropertyType;
import org.sablo.specification.property.types.IntPropertyType;
import org.sablo.specification.property.types.TypesRegistry;

import com.servoy.j2db.server.ngclient.NGClientEntryFilter;
import com.servoy.j2db.server.ngclient.property.types.NGDatePropertyType;
import com.servoy.j2db.server.ngclient.property.types.ServoyStringPropertyType;
import com.servoy.j2db.server.ngclient.property.types.Types;

/**
 * @author jcompagner
 *
 * TODO: include in jenkins automatic build
 *
 */
@SuppressWarnings("nls")
public class WebComponentSpecTest
{

	@Before
	public void setup()
	{
		Types.getTypesInstance().registerTypes();
	}

	@After
	public void tearDown()
	{
		WebComponentSpecProvider.disposeInstance();
	}

	@Test
	public void testDefinition() throws JSONException
	{
		String property = "{name:'test',definition:'/test.js', model: {}}";

		WebObjectSpecification spec = WebObjectSpecification.parseSpec(property, "sample", null);
		Assert.assertEquals("/test.js", spec.getDefinition());
	}


	@Test
	public void testLibsWith0Enry() throws JSONException
	{
		String property = "{name:'test',definition:'/test.js', libraries:[],model: {}}";

		WebObjectSpecification spec = WebObjectSpecification.parseSpec(property, "sample", null);
		JSONArray libs = spec.getLibraries();
		Assert.assertEquals(0, libs.length());
	}


	@Test
	public void testLibsWith1Enry() throws JSONException
	{
		String property = "{name:'test',definition:'/test.js', libraries:[{name:'test', version:'1', url:'/test.css', mimetype:'text/css'}],model: {}}";

		WebObjectSpecification spec = WebObjectSpecification.parseSpec(property, "sample", null);
		JSONArray libs = spec.getLibraries();
		Assert.assertEquals(1, libs.length());
		Assert.assertEquals(libs.optJSONObject(0).optString("url"), "/test.css");
	}

	@Test
	public void testLibsWith2Enry() throws JSONException
	{
		String property = "{name:'test',definition:'/test.js', libraries:[{name:'test', version:'1', url:'/test.css', mimetype:'text/css'},{name:'something', version:'1', url:'/something.js', mimetype:'text/javascript'}],model: {}}";

		WebObjectSpecification spec = WebObjectSpecification.parseSpec(property, "sample", null);
		JSONArray libs = spec.getLibraries();
		Assert.assertEquals(2, libs.length());
		Assert.assertEquals(libs.optJSONObject(0).optString("url"), "/test.css");
		Assert.assertEquals(libs.optJSONObject(1).optString("url"), "/something.js");
	}

	@Test
	public void testLibsWithAngularEnry() throws JSONException
	{
		String property = "{name:'test',definition:'/test.js', libraries:[{name:'angular-animate', version:'1.5.8', url:'js/angular-modules/1.5.8/angular-animate.js', mimetype:'text/javascript'},{name:'something', version:'1', url:'/something.js', mimetype:'text/javascript'}],model: {}}";

		String manifest = "Manifest-Version: 1.0\nAnt-Version: Apache Ant 1.8.4\nCreated-By: 1.7.0_05-b06 (Oracle Corporation)\n\nName: mycomponent.spec\nWeb-Component: True\n";

		HashMap<String, String> components = new HashMap<>();
		components.put("mycomponent.spec", property);
		WebComponentSpecProvider.init(new IPackageReader[] { new InMemPackageReader(manifest, components) });
		WebServiceSpecProvider.init(new IPackageReader[0]);


		Object[] contributions = IndexPageEnhancer.getAllContributions(null, null, null);

		Assert.assertTrue(((Collection< ? >)contributions[0]).size() == 0);
		Assert.assertTrue(((Collection< ? >)contributions[1]).size() == 3);

		Assert.assertTrue(contributions[1].toString(), ((Collection< ? >)contributions[1]).contains("js/angular-modules/1.5.8/angular-animate.js"));

		contributions = IndexPageEnhancer.getAllContributions(null, null, NGClientEntryFilter.CONTRIBUTION_ENTRY_FILTER);

		Assert.assertTrue(((Collection< ? >)contributions[0]).size() == 0);
		Assert.assertTrue(((Collection< ? >)contributions[1]).size() == 3);

		Assert.assertTrue(contributions[1].toString(), ((Collection< ? >)contributions[1]).contains("js/angular-modules/1.6.3/angular-animate.js"));

	}

	@Test
	public void testValueListType() throws JSONException
	{
		String property = "{name:'test',definition:'/test.js', model: {mydataprovider:'dataprovider',myvaluelist:{for:'mydataprovider' , type:'valuelist'}}}";

		WebObjectSpecification spec = WebObjectSpecification.parseSpec(property, "sample", null);
		Assert.assertEquals(2, spec.getProperties().size());
		PropertyDescription pd = spec.getProperties().get("myvaluelist");
		Assert.assertNotNull(pd);
		Assert.assertTrue(pd.getType() == TypesRegistry.getType("valuelist"));
		Assert.assertFalse(pd.getType() instanceof CustomJSONArrayType< ? , ? >);

		Assert.assertEquals("mydataprovider", ((ValueListConfig)pd.getConfig()).getFor());
	}

	@Test
	public void testFormatTypeAsArray() throws JSONException
	{
		String property = "{name:'test',definition:'/test.js', model: {mydataprovider:'dataprovider',myformat:{for:['mydataprovider'] , type:'format'}}}";

		WebObjectSpecification spec = WebObjectSpecification.parseSpec(property, "sample", null);
		Assert.assertEquals(2, spec.getProperties().size());
		PropertyDescription pd = spec.getProperties().get("myformat");
		Assert.assertNotNull(pd);
		Assert.assertTrue(pd.getType() == TypesRegistry.getType("format"));
		Assert.assertFalse(pd.getType() instanceof CustomJSONArrayType< ? , ? >);

		Assert.assertEquals("mydataprovider", ((String[])pd.getConfig())[0]);
	}

	@Test
	public void testFormatTypeAsString() throws JSONException
	{
		String property = "{name:'test',definition:'/test.js', model: {mydataprovider:'dataprovider',myformat:{for:'mydataprovider' , type:'format'}}}";

		WebObjectSpecification spec = WebObjectSpecification.parseSpec(property, "sample", null);
		Assert.assertEquals(2, spec.getProperties().size());
		PropertyDescription pd = spec.getProperties().get("myformat");
		Assert.assertNotNull(pd);
		Assert.assertTrue(pd.getType() == TypesRegistry.getType("format"));
		Assert.assertFalse(pd.getType() instanceof CustomJSONArrayType< ? , ? >);

		Assert.assertEquals("mydataprovider", ((String[])pd.getConfig())[0]);
	}

	@Test
	public void testStringProperyType() throws JSONException
	{
		String property = "{name:'test',definition:'/test.js', model: {myproperty:'string'}}";

		WebObjectSpecification spec = WebObjectSpecification.parseSpec(property, "sample", null);
		Assert.assertEquals(1, spec.getProperties().size());
		PropertyDescription pd = spec.getProperties().get("myproperty");
		Assert.assertNotNull(pd);
		Assert.assertTrue(pd.getType() == ServoyStringPropertyType.INSTANCE);
		Assert.assertFalse(pd.getType() instanceof CustomJSONArrayType< ? , ? >);
	}

	@Test
	public void testMultiplyProperies() throws JSONException
	{
		String property = "{name:'test',definition:'/test.js', model: {myproperty:'string',prop2:'boolean',prop3:'int',prop4:'date'}}";

		WebObjectSpecification spec = WebObjectSpecification.parseSpec(property, "sample", null);
		Assert.assertEquals(spec.getProperties().toString(), 4, spec.getProperties().size());
		PropertyDescription pd = spec.getProperties().get("myproperty");
		Assert.assertNotNull(pd);
		Assert.assertTrue(pd.getType() == ServoyStringPropertyType.INSTANCE);
		Assert.assertFalse(pd.getType() instanceof CustomJSONArrayType< ? , ? >);
		pd = spec.getProperties().get("prop2");
		Assert.assertNotNull(pd);
		Assert.assertTrue(pd.getType() == BooleanPropertyType.INSTANCE);
		Assert.assertFalse(pd.getType() instanceof CustomJSONArrayType< ? , ? >);
		pd = spec.getProperties().get("prop3");
		Assert.assertNotNull(pd);
		Assert.assertTrue(pd.getType() == IntPropertyType.INSTANCE);
		Assert.assertFalse(pd.getType() instanceof CustomJSONArrayType< ? , ? >);
		pd = spec.getProperties().get("prop4");
		Assert.assertNotNull(pd);
		Assert.assertTrue(pd.getType() == NGDatePropertyType.NG_INSTANCE);
		Assert.assertFalse(pd.getType() instanceof CustomJSONArrayType< ? , ? >);
	}

	@Test
	public void testArrayStringProperyType() throws JSONException
	{
		String property = "{name:'test',definition:'/test.js', model: {myproperty:'string[]'}}";

		WebObjectSpecification spec = WebObjectSpecification.parseSpec(property, "sample", null);
		Assert.assertEquals(1, spec.getProperties().size());
		PropertyDescription pd = spec.getProperties().get("myproperty");
		Assert.assertNotNull(pd);
		Assert.assertTrue(pd.getType() instanceof CustomJSONArrayType< ? , ? >);
		Assert.assertTrue(((CustomJSONArrayType)pd.getType()).getCustomJSONTypeDefinition().getType() == ServoyStringPropertyType.INSTANCE);
	}

	@Test
	public void testOwnTypeProperyType() throws JSONException
	{
		String property = "{name:'test',definition:'/test.js', model: {myproperty:'mytype'}, types: {mytype:{model:{typeproperty:'string'}}}}";

		WebObjectSpecification spec = WebObjectSpecification.parseSpec(property, "sample", null);
		Assert.assertEquals(1, spec.getProperties().size());
		PropertyDescription pd = spec.getProperties().get("myproperty");
		Assert.assertNotNull(pd);
		Assert.assertNotNull(((ICustomType)pd.getType()).getCustomJSONTypeDefinition());
		Object config = pd.getConfig();
		Assert.assertFalse(pd.getType() instanceof CustomJSONArrayType< ? , ? >);

		PropertyDescription wct = ((ICustomType)pd.getType()).getCustomJSONTypeDefinition();
		Assert.assertEquals("test.mytype", wct.getName());
		Assert.assertEquals(1, wct.getProperties().size());
		PropertyDescription pd2 = wct.getProperty("typeproperty");
		Assert.assertNotNull(pd2);
		Assert.assertTrue(pd2.getType() == ServoyStringPropertyType.INSTANCE);
		Assert.assertFalse(pd2.getType() instanceof CustomJSONArrayType< ? , ? >);
	}

	@Test
	public void testArrayOwnTypeProperyType() throws JSONException
	{
		String property = "{name:'test',definition:'/test.js', model: {myproperty:'mytype[]'}, types: {mytype:{model:{typeproperty:'string'}}}}";

		WebObjectSpecification spec = WebObjectSpecification.parseSpec(property, "sample", null);
		Assert.assertEquals(1, spec.getProperties().size());
		PropertyDescription pd = spec.getProperties().get("myproperty");
		Assert.assertNotNull(pd);
		Assert.assertNotNull(((ICustomType)pd.getType()).getCustomJSONTypeDefinition());
		Object config = pd.getConfig();
		Assert.assertTrue(pd.getType() instanceof CustomJSONArrayType< ? , ? >);

		PropertyDescription wct = ((CustomJSONArrayType)pd.getType()).getCustomJSONTypeDefinition();
		Assert.assertEquals("test.mytype", wct.getType().getName());
		Assert.assertEquals(1, wct.getProperties().size());
		wct = ((CustomJSONPropertyType)wct.getType()).getCustomJSONTypeDefinition();
		Assert.assertEquals(1, wct.getProperties().size());
		PropertyDescription pd2 = wct.getProperty("typeproperty");
		Assert.assertNotNull(pd2);
		Assert.assertTrue(pd2.getType() == ServoyStringPropertyType.INSTANCE);
		Assert.assertFalse(pd2.getType() instanceof CustomJSONArrayType< ? , ? >);

	}

	@Test
	public void testArrayOwnTypeProperyTypeAsArray() throws JSONException
	{
		String property = "{name:'test',definition:'/test.js', model: {myproperty:'mytype[]'}, types: {mytype:{model:{typeproperty:'string[]'}}}}";

		WebObjectSpecification spec = WebObjectSpecification.parseSpec(property, "sample", null);
		Assert.assertEquals(1, spec.getProperties().size());
		PropertyDescription pd = spec.getProperties().get("myproperty");
		Assert.assertNotNull(pd);
		Assert.assertNotNull(((ICustomType)pd.getType()).getCustomJSONTypeDefinition());
		Object config = pd.getConfig();
		Assert.assertTrue(pd.getType() instanceof CustomJSONArrayType< ? , ? >);

		PropertyDescription wct = ((CustomJSONArrayType)pd.getType()).getCustomJSONTypeDefinition();
		Assert.assertEquals("test.mytype", wct.getType().getName());
		Assert.assertEquals(1, wct.getProperties().size());
		wct = ((CustomJSONPropertyType)wct.getType()).getCustomJSONTypeDefinition();
		Assert.assertEquals(1, wct.getProperties().size());
		PropertyDescription pd2 = wct.getProperty("typeproperty");
		Assert.assertNotNull(pd2);
		Assert.assertTrue(pd2.getType() instanceof CustomJSONArrayType< ? , ? >);
		Assert.assertTrue(((CustomJSONArrayType)pd2.getType()).getCustomJSONTypeDefinition().getType() == ServoyStringPropertyType.INSTANCE);

	}

	@Test
	public void testOwnTypeProperyTypeRefernceInOtherOwnType() throws JSONException
	{
		String property = "{name:'test',definition:'/test.js', model: {myproperty:'mytype'}, types: {mytype:{model:{typeproperty:'mytype2'}},mytype2:{model:{typeproperty:'string'}}}}";

		WebObjectSpecification spec = WebObjectSpecification.parseSpec(property, "sample", null);
		Assert.assertEquals(1, spec.getProperties().size());
		PropertyDescription pd = spec.getProperties().get("myproperty");
		Assert.assertNotNull(pd);
		Assert.assertNotNull(((ICustomType)pd.getType()).getCustomJSONTypeDefinition());
		Object config = pd.getConfig();
		Assert.assertNull(config);
		Assert.assertFalse(pd.getType() instanceof CustomJSONArrayType< ? , ? >);

		PropertyDescription wct = ((ICustomType)pd.getType()).getCustomJSONTypeDefinition();
		Assert.assertEquals("test.mytype", wct.getName());
		Assert.assertEquals(1, wct.getProperties().size());
		PropertyDescription pd2 = wct.getProperties().get("typeproperty");
		Assert.assertNotNull(pd2);
		Assert.assertNotNull(((ICustomType)pd2.getType()).getCustomJSONTypeDefinition());
		Assert.assertFalse(pd2.getType() instanceof CustomJSONArrayType< ? , ? >);

		config = pd2.getConfig();
		Assert.assertNull(config);
		PropertyDescription wct2 = ((ICustomType)pd2.getType()).getCustomJSONTypeDefinition();
		Assert.assertEquals("test.mytype2", wct2.getName());
		Assert.assertEquals(1, wct2.getProperties().size());
		PropertyDescription pd3 = wct2.getProperty("typeproperty");
		Assert.assertNotNull(pd3);
		Assert.assertTrue(pd3.getType() == ServoyStringPropertyType.INSTANCE);
		Assert.assertFalse(pd3.getType() instanceof CustomJSONArrayType< ? , ? >);
	}

	@Test
	public void testNames() throws JSONException
	{
		String property = "{name:'test',definition:'/test.js'}";
		WebObjectSpecification spec = WebObjectSpecification.parseSpec(property, "sample", null);
		Assert.assertEquals("test", spec.getName());
		Assert.assertEquals("test", spec.getDisplayName());
		Assert.assertEquals("sample", spec.getPackageName());
		//Assert.assertEquals("sample:test", spec.getFullName());

		property = "{name:'test', displayName: 'A Test',definition:'/test.js'}";
		spec = WebObjectSpecification.parseSpec(property, "sample", null);
		Assert.assertEquals("test", spec.getName());
		Assert.assertEquals("A Test", spec.getDisplayName());
		Assert.assertEquals("sample", spec.getPackageName());
		//Assert.assertEquals("sample:test", spec.getFullName());
	}

}
