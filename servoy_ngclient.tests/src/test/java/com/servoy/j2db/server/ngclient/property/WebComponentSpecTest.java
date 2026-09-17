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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sablo.InMemPackageReader;
import org.sablo.IndexPageEnhancer;
import org.sablo.specification.IYieldingType;
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
import com.servoy.j2db.server.ngclient.property.types.ValueListPropertyType;

/**
 * @author jcompagner
 *
 * TODO: include in jenkins automatic build
 *
 */
@SuppressWarnings("nls")
public class WebComponentSpecTest extends Log4JToConsoleTest
{

	@BeforeEach
	public void setup()
	{
		Types.getTypesInstance().registerTypes();
	}

	@AfterEach
	public void tearDown()
	{
		WebComponentSpecProvider.disposeInstance();
	}

	@Test
	public void testDefinition() throws JSONException
	{
		String property = "{name:'test',definition:'/test.js', model: {}}";

		WebObjectSpecification spec = WebObjectSpecification.parseSpec(property, "sample", null, null);
		Assertions.assertEquals("/test.js", spec.getDefinition());
	}


	@Test
	public void testLibsWith0Enry() throws JSONException
	{
		String property = "{name:'test',definition:'/test.js', libraries:[],model: {}}";

		WebObjectSpecification spec = WebObjectSpecification.parseSpec(property, "sample", null, null);
		JSONArray libs = spec.getLibraries();
		Assertions.assertEquals(0, libs.length());
	}


	@Test
	public void testLibsWith1Enry() throws JSONException
	{
		String property = "{name:'test',definition:'/test.js', libraries:[{name:'test', version:'1', url:'/test.css', mimetype:'text/css'}],model: {}}";

		WebObjectSpecification spec = WebObjectSpecification.parseSpec(property, "sample", null, null);
		JSONArray libs = spec.getLibraries();
		Assertions.assertEquals(1, libs.length());
		Assertions.assertEquals(libs.optJSONObject(0).optString("url"), "/test.css");
	}

	@Test
	public void testLibsWith2Enry() throws JSONException
	{
		String property = "{name:'test',definition:'/test.js', libraries:[{name:'test', version:'1', url:'/test.css', mimetype:'text/css'},{name:'something', version:'1', url:'/something.js', mimetype:'text/javascript'}],model: {}}";

		WebObjectSpecification spec = WebObjectSpecification.parseSpec(property, "sample", null, null);
		JSONArray libs = spec.getLibraries();
		Assertions.assertEquals(2, libs.length());
		Assertions.assertEquals(libs.optJSONObject(0).optString("url"), "/test.css");
		Assertions.assertEquals(libs.optJSONObject(1).optString("url"), "/something.js");
	}

	@Test
	public void testLibsWithAngularEnry() throws JSONException
	{
		String property = "{name:'test',definition:'/test.js', libraries:[{name:'angular-animate', version:'1.5.8', url:'js/angular-modules/1.9.3/angular-animate.js', mimetype:'text/javascript'},{name:'something', version:'1', url:'/something.js', mimetype:'text/javascript'}],model: {}}";

		String manifest = "Manifest-Version: 1.0\nAnt-Version: Apache Ant 1.8.4\nCreated-By: 1.7.0_05-b06 (Oracle Corporation)\n\nName: mycomponent.spec\nWeb-Component: True\n";

		HashMap<String, String> components = new HashMap<>();
		components.put("mycomponent.spec", property);
		WebComponentSpecProvider.init(new IPackageReader[] { new InMemPackageReader(manifest, components) }, null);
		WebServiceSpecProvider.init(new IPackageReader[0]);


		Object[] contributions = IndexPageEnhancer.getAllContributions(null, null, null, null);

		Assertions.assertTrue(((Collection< ? >)contributions[0]).size() == 0);
		Assertions.assertTrue(((Collection< ? >)contributions[1]).size() == 3);

		Assertions.assertTrue(((Collection< ? >)contributions[1]).contains("js/angular-modules/1.9.3/angular-animate.js"), contributions[1].toString());

		contributions = IndexPageEnhancer.getAllContributions(null, null, null, NGClientEntryFilter.CONTRIBUTION_ENTRY_FILTER);

		Assertions.assertTrue(((Collection< ? >)contributions[0]).size() == 0);
		Assertions.assertTrue(((Collection< ? >)contributions[1]).size() == 3);

		Assertions.assertTrue(((Collection< ? >)contributions[1]).contains("js/angular-modules/1.9.3/angular-animate.js"), contributions[1].toString());

	}

	@Test
	public void testValueListType() throws JSONException
	{
		String property = "{name:'test',definition:'/test.js', model: {mydataprovider:'dataprovider',myvaluelist:{for:'mydataprovider' , type:'valuelist'}}}";

		WebObjectSpecification spec = WebObjectSpecification.parseSpec(property, "sample", null, null);
		Assertions.assertEquals(2, spec.getProperties().size());
		PropertyDescription pd = spec.getProperties().get("myvaluelist");
		Assertions.assertNotNull(pd);
		Assertions.assertTrue(pd.getType() == ((IYieldingType)(TypesRegistry.getType("valuelist"))).getPossibleYieldType());
		Assertions.assertFalse(pd.getType() instanceof CustomJSONArrayType< ? , ? >);

		Assertions.assertEquals("mydataprovider", ((ValueListConfig)pd.getConfig()).getFor());
	}

	@Test
	public void testValueListTypeLinkedToFoundset() throws JSONException
	{
		String property = "{name:'test',definition:'/test.js', model: {myfoundset:'foundset', mydataprovider:'dataprovider',myvaluelist:{for:'mydataprovider', type:'valuelist', forFoundset:'myfoundset'}}}";

		WebObjectSpecification spec = WebObjectSpecification.parseSpec(property, "sample", null, null);
		Assertions.assertEquals(3, spec.getProperties().size());
		PropertyDescription pd = spec.getProperties().get("myvaluelist");
		Assertions.assertNotNull(pd);
		Assertions.assertTrue(pd.getType() == TypesRegistry.getType("valuelist"));
		Assertions.assertTrue(pd.getType() instanceof FoundsetLinkedPropertyType);
		Assertions.assertTrue(((IYieldingType)(pd.getType())).getPossibleYieldType() == ValueListPropertyType.INSTANCE);
	}

	@Test
	public void testFormatTypeAsArray() throws JSONException
	{
		String property = "{name:'test',definition:'/test.js', model: {mydataprovider:'dataprovider',myformat:{for:['mydataprovider'] , type:'format'}}}";

		WebObjectSpecification spec = WebObjectSpecification.parseSpec(property, "sample", null, null);
		Assertions.assertEquals(2, spec.getProperties().size());
		PropertyDescription pd = spec.getProperties().get("myformat");
		Assertions.assertNotNull(pd);
		Assertions.assertTrue(pd.getType() == TypesRegistry.getType("format"));
		Assertions.assertFalse(pd.getType() instanceof CustomJSONArrayType< ? , ? >);

		Assertions.assertEquals("mydataprovider", ((String[])pd.getConfig())[0]);
	}

	@Test
	public void testFormatTypeAsString() throws JSONException
	{
		String property = "{name:'test',definition:'/test.js', model: {mydataprovider:'dataprovider',myformat:{for:'mydataprovider' , type:'format'}}}";

		WebObjectSpecification spec = WebObjectSpecification.parseSpec(property, "sample", null, null);
		Assertions.assertEquals(2, spec.getProperties().size());
		PropertyDescription pd = spec.getProperties().get("myformat");
		Assertions.assertNotNull(pd);
		Assertions.assertTrue(pd.getType() == TypesRegistry.getType("format"));
		Assertions.assertFalse(pd.getType() instanceof CustomJSONArrayType< ? , ? >);

		Assertions.assertEquals("mydataprovider", ((String[])pd.getConfig())[0]);
	}

	@Test
	public void testStringProperyType() throws JSONException
	{
		String property = "{name:'test',definition:'/test.js', model: {myproperty:'string'}}";

		WebObjectSpecification spec = WebObjectSpecification.parseSpec(property, "sample", null, null);
		Assertions.assertEquals(1, spec.getProperties().size());
		PropertyDescription pd = spec.getProperties().get("myproperty");
		Assertions.assertNotNull(pd);
		Assertions.assertTrue(pd.getType() == ServoyStringPropertyType.INSTANCE);
		Assertions.assertFalse(pd.getType() instanceof CustomJSONArrayType< ? , ? >);
	}

	@Test
	public void testMultiplyProperies() throws JSONException
	{
		String property = "{name:'test',definition:'/test.js', model: {myproperty:'string',prop2:'boolean',prop3:'int',prop4:'date'}}";

		WebObjectSpecification spec = WebObjectSpecification.parseSpec(property, "sample", null, null);
		Assertions.assertEquals(4, spec.getProperties().size(), spec.getProperties().toString());
		PropertyDescription pd = spec.getProperties().get("myproperty");
		Assertions.assertNotNull(pd);
		Assertions.assertTrue(pd.getType() == ServoyStringPropertyType.INSTANCE);
		Assertions.assertFalse(pd.getType() instanceof CustomJSONArrayType< ? , ? >);
		pd = spec.getProperties().get("prop2");
		Assertions.assertNotNull(pd);
		Assertions.assertTrue(pd.getType() == BooleanPropertyType.INSTANCE);
		Assertions.assertFalse(pd.getType() instanceof CustomJSONArrayType< ? , ? >);
		pd = spec.getProperties().get("prop3");
		Assertions.assertNotNull(pd);
		Assertions.assertTrue(pd.getType() == IntPropertyType.INSTANCE);
		Assertions.assertFalse(pd.getType() instanceof CustomJSONArrayType< ? , ? >);
		pd = spec.getProperties().get("prop4");
		Assertions.assertNotNull(pd);
		Assertions.assertTrue(pd.getType() == NGDatePropertyType.NG_INSTANCE);
		Assertions.assertFalse(pd.getType() instanceof CustomJSONArrayType< ? , ? >);
	}

	@Test
	public void testArrayStringProperyType() throws JSONException
	{
		String property = "{name:'test',definition:'/test.js', model: {myproperty:'string[]'}}";

		WebObjectSpecification spec = WebObjectSpecification.parseSpec(property, "sample", null, null);
		Assertions.assertEquals(1, spec.getProperties().size());
		PropertyDescription pd = spec.getProperties().get("myproperty");
		Assertions.assertNotNull(pd);
		Assertions.assertTrue(pd.getType() instanceof CustomJSONArrayType< ? , ? >);
		Assertions.assertTrue(((CustomJSONArrayType)pd.getType()).getCustomJSONTypeDefinition().getType() == ServoyStringPropertyType.INSTANCE);
	}

	@Test
	public void testOwnTypeProperyType() throws JSONException
	{
		String property = "{name:'test',definition:'/test.js', model: {myproperty:'mytype'}, types: {mytype:{model:{typeproperty:'string'}}}}";

		WebObjectSpecification spec = WebObjectSpecification.parseSpec(property, "sample", null, null);
		Assertions.assertEquals(1, spec.getProperties().size());
		PropertyDescription pd = spec.getProperties().get("myproperty");
		Assertions.assertNotNull(pd);
		Assertions.assertNotNull(((ICustomType)pd.getType()).getCustomJSONTypeDefinition());
		Object config = pd.getConfig();
		Assertions.assertFalse(pd.getType() instanceof CustomJSONArrayType< ? , ? >);

		PropertyDescription wct = ((ICustomType)pd.getType()).getCustomJSONTypeDefinition();
		Assertions.assertEquals("test.mytype", wct.getName());
		Assertions.assertEquals(1, wct.getProperties().size());
		PropertyDescription pd2 = wct.getProperty("typeproperty");
		Assertions.assertNotNull(pd2);
		Assertions.assertTrue(pd2.getType() == ServoyStringPropertyType.INSTANCE);
		Assertions.assertFalse(pd2.getType() instanceof CustomJSONArrayType< ? , ? >);
	}

	@Test
	public void testArrayOwnTypeProperyType() throws JSONException
	{
		String property = "{name:'test',definition:'/test.js', model: {myproperty:'mytype[]'}, types: {mytype:{model:{typeproperty:'string'}}}}";

		WebObjectSpecification spec = WebObjectSpecification.parseSpec(property, "sample", null, null);
		Assertions.assertEquals(1, spec.getProperties().size());
		PropertyDescription pd = spec.getProperties().get("myproperty");
		Assertions.assertNotNull(pd);
		Assertions.assertNotNull(((ICustomType)pd.getType()).getCustomJSONTypeDefinition());
		Object config = pd.getConfig();
		Assertions.assertTrue(pd.getType() instanceof CustomJSONArrayType< ? , ? >);

		PropertyDescription wct = ((CustomJSONArrayType)pd.getType()).getCustomJSONTypeDefinition();
		Assertions.assertEquals("test.mytype", wct.getType().getName());
		Assertions.assertEquals(1, wct.getProperties().size());
		wct = ((CustomJSONPropertyType)wct.getType()).getCustomJSONTypeDefinition();
		Assertions.assertEquals(1, wct.getProperties().size());
		PropertyDescription pd2 = wct.getProperty("typeproperty");
		Assertions.assertNotNull(pd2);
		Assertions.assertTrue(pd2.getType() == ServoyStringPropertyType.INSTANCE);
		Assertions.assertFalse(pd2.getType() instanceof CustomJSONArrayType< ? , ? >);

	}

	@Test
	public void testArrayOwnTypeProperyTypeAsArray() throws JSONException
	{
		String property = "{name:'test',definition:'/test.js', model: {myproperty:'mytype[]'}, types: {mytype:{model:{typeproperty:'string[]'}}}}";

		WebObjectSpecification spec = WebObjectSpecification.parseSpec(property, "sample", null, null);
		Assertions.assertEquals(1, spec.getProperties().size());
		PropertyDescription pd = spec.getProperties().get("myproperty");
		Assertions.assertNotNull(pd);
		Assertions.assertNotNull(((ICustomType)pd.getType()).getCustomJSONTypeDefinition());
		Object config = pd.getConfig();
		Assertions.assertTrue(pd.getType() instanceof CustomJSONArrayType< ? , ? >);

		PropertyDescription wct = ((CustomJSONArrayType)pd.getType()).getCustomJSONTypeDefinition();
		Assertions.assertEquals("test.mytype", wct.getType().getName());
		Assertions.assertEquals(1, wct.getProperties().size());
		wct = ((CustomJSONPropertyType)wct.getType()).getCustomJSONTypeDefinition();
		Assertions.assertEquals(1, wct.getProperties().size());
		PropertyDescription pd2 = wct.getProperty("typeproperty");
		Assertions.assertNotNull(pd2);
		Assertions.assertTrue(pd2.getType() instanceof CustomJSONArrayType< ? , ? >);
		Assertions.assertTrue(((CustomJSONArrayType)pd2.getType()).getCustomJSONTypeDefinition().getType() == ServoyStringPropertyType.INSTANCE);

	}

	@Test
	public void testOwnTypeProperyTypeRefernceInOtherOwnType() throws JSONException
	{
		String property = "{name:'test',definition:'/test.js', model: {myproperty:'mytype'}, types: {mytype:{model:{typeproperty:'mytype2'}},mytype2:{model:{typeproperty:'string'}}}}";

		WebObjectSpecification spec = WebObjectSpecification.parseSpec(property, "sample", null, null);
		Assertions.assertEquals(1, spec.getProperties().size());
		PropertyDescription pd = spec.getProperties().get("myproperty");
		Assertions.assertNotNull(pd);
		Assertions.assertNotNull(((ICustomType)pd.getType()).getCustomJSONTypeDefinition());
		Object config = pd.getConfig();
		Assertions.assertNull(config);
		Assertions.assertFalse(pd.getType() instanceof CustomJSONArrayType< ? , ? >);

		PropertyDescription wct = ((ICustomType)pd.getType()).getCustomJSONTypeDefinition();
		Assertions.assertEquals("test.mytype", wct.getName());
		Assertions.assertEquals(1, wct.getProperties().size());
		PropertyDescription pd2 = wct.getProperties().get("typeproperty");
		Assertions.assertNotNull(pd2);
		Assertions.assertNotNull(((ICustomType)pd2.getType()).getCustomJSONTypeDefinition());
		Assertions.assertFalse(pd2.getType() instanceof CustomJSONArrayType< ? , ? >);

		config = pd2.getConfig();
		Assertions.assertNull(config);
		PropertyDescription wct2 = ((ICustomType)pd2.getType()).getCustomJSONTypeDefinition();
		Assertions.assertEquals("test.mytype2", wct2.getName());
		Assertions.assertEquals(1, wct2.getProperties().size());
		PropertyDescription pd3 = wct2.getProperty("typeproperty");
		Assertions.assertNotNull(pd3);
		Assertions.assertTrue(pd3.getType() == ServoyStringPropertyType.INSTANCE);
		Assertions.assertFalse(pd3.getType() instanceof CustomJSONArrayType< ? , ? >);
	}

	@Test
	public void testNames() throws JSONException
	{
		String property = "{name:'test',definition:'/test.js'}";
		WebObjectSpecification spec = WebObjectSpecification.parseSpec(property, "sample", null, null);
		Assertions.assertEquals("test", spec.getName());
		Assertions.assertEquals("test", spec.getDisplayName());
		Assertions.assertEquals("sample", spec.getPackageName());
		//Assertions.assertEquals("sample:test", spec.getFullName());

		property = "{name:'test', displayName: 'A Test',definition:'/test.js'}";
		spec = WebObjectSpecification.parseSpec(property, "sample", null, null);
		Assertions.assertEquals("test", spec.getName());
		Assertions.assertEquals("A Test", spec.getDisplayName());
		Assertions.assertEquals("sample", spec.getPackageName());
		//Assertions.assertEquals("sample:test", spec.getFullName());
	}

}
