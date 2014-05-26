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

import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecification;
import org.sablo.specification.property.IPropertyType;

/**
 * @author jcompagner
 * 
 * TODO: include in jenkins automatic build
 *
 */
@SuppressWarnings("nls")
public class WebComponentSpecTest
{
	@Test
	public void testDefinition() throws JSONException
	{
		String property = "name:'test',definition:'/test.js', model: {}";

		WebComponentSpecification spec = WebComponentSpecification.parseSpec(property, "sample", null, "test.path");
		Assert.assertEquals("/test.js", spec.getDefinition());
	}


	@Test
	public void testLibsWith0Enry() throws JSONException
	{
		String property = "name:'test',definition:'/test.js', libraries:[],model: {}";

		WebComponentSpecification spec = WebComponentSpecification.parseSpec(property, "sample", null, "test.path");
		String[] libs = spec.getLibraries();
		Assert.assertEquals(0, libs.length);
	}


	@Test
	public void testLibsWith1Enry() throws JSONException
	{
		String property = "name:'test',definition:'/test.js', libraries:['/test.css'],model: {}";

		WebComponentSpecification spec = WebComponentSpecification.parseSpec(property, "sample", null, "test.path");
		String[] libs = spec.getLibraries();
		Assert.assertEquals(1, libs.length);
		Assert.assertEquals(libs[0], "/test.css");
	}

	@Test
	public void testLibsWith2Enry() throws JSONException
	{
		String property = "name:'test',definition:'/test.js', libraries:['/test.css','/something.js'],model: {}";

		WebComponentSpecification spec = WebComponentSpecification.parseSpec(property, "sample", null, "test.path");
		String[] libs = spec.getLibraries();
		Assert.assertEquals(2, libs.length);
		Assert.assertEquals(libs[0], "/test.css");
		Assert.assertEquals(libs[1], "/something.js");
	}

	@Test
	public void testValueListType() throws JSONException
	{
		String property = "name:'test',definition:'/test.js', model: {mydataprovider:'dataprovider',myvaluelist:{for:'mydataprovider' , type:'valuelist'}}";

		WebComponentSpecification spec = WebComponentSpecification.parseSpec(property, "sample", null, "test.path");
		Assert.assertEquals(2, spec.getProperties().size());
		PropertyDescription pd = spec.getProperties().get("myvaluelist");
		Assert.assertNotNull(pd);
		Assert.assertTrue(pd.getType() == IPropertyType.Default.valuelist.getType());
		Assert.assertFalse(pd.isArray());

		Assert.assertEquals("mydataprovider", pd.getConfig());
	}

	@Test
	public void testFormatType() throws JSONException
	{
		String property = "name:'test',definition:'/test.js', model: {mydataprovider:'dataprovider',myformat:{for:'mydataprovider' , type:'format'}}";

		WebComponentSpecification spec = WebComponentSpecification.parseSpec(property, "sample", null, "test.path");
		Assert.assertEquals(2, spec.getProperties().size());
		PropertyDescription pd = spec.getProperties().get("myformat");
		Assert.assertNotNull(pd);
		Assert.assertTrue(pd.getType() == IPropertyType.Default.format.getType());
		Assert.assertFalse(pd.isArray());

		Assert.assertEquals("mydataprovider", pd.getConfig());
	}

	@Test
	public void testStringProperyType() throws JSONException
	{
		String property = "name:'test',definition:'/test.js', model: {myproperty:'string'}";

		WebComponentSpecification spec = WebComponentSpecification.parseSpec(property, "sample", null, "test.path");
		Assert.assertEquals(1, spec.getProperties().size());
		PropertyDescription pd = spec.getProperties().get("myproperty");
		Assert.assertNotNull(pd);
		Assert.assertTrue(pd.getType() == IPropertyType.Default.string.getType());
		Assert.assertFalse(pd.isArray());
	}

	@Test
	public void testMultiplyProperies() throws JSONException
	{
		String property = "name:'test',definition:'/test.js', model: {myproperty:'string',prop2:'boolean',prop3:'int',prop4:'date'}";

		WebComponentSpecification spec = WebComponentSpecification.parseSpec(property, "sample", null, "test.path");
		Assert.assertEquals(spec.getProperties().toString(), 4, spec.getProperties().size());
		PropertyDescription pd = spec.getProperties().get("myproperty");
		Assert.assertNotNull(pd);
		Assert.assertTrue(pd.getType() == IPropertyType.Default.string.getType());
		Assert.assertFalse(pd.isArray());
		pd = spec.getProperties().get("prop2");
		Assert.assertNotNull(pd);
		Assert.assertTrue(pd.getType() == IPropertyType.Default.bool.getType());
		Assert.assertFalse(pd.isArray());
		pd = spec.getProperties().get("prop3");
		Assert.assertNotNull(pd);
		Assert.assertTrue(pd.getType() == IPropertyType.Default.intnumber.getType());
		Assert.assertFalse(pd.isArray());
		pd = spec.getProperties().get("prop4");
		Assert.assertNotNull(pd);
		Assert.assertTrue(pd.getType() == IPropertyType.Default.date.getType());
		Assert.assertFalse(pd.isArray());
	}

	@Test
	public void testArrayStringProperyType() throws JSONException
	{
		String property = "name:'test',definition:'/test.js', model: {myproperty:'string[]'}";

		WebComponentSpecification spec = WebComponentSpecification.parseSpec(property, "sample", null, "test.path");
		Assert.assertEquals(1, spec.getProperties().size());
		PropertyDescription pd = spec.getProperties().get("myproperty");
		Assert.assertNotNull(pd);
		Assert.assertTrue(pd.getType() == IPropertyType.Default.string.getType());
		Assert.assertTrue(pd.isArray());
	}

	@Test
	public void testOwnTypeProperyType() throws JSONException
	{
		String property = "name:'test',definition:'/test.js', model: {myproperty:'mytype'}, types: {mytype:{model:{typeproperty:'string'}}}";

		WebComponentSpecification spec = WebComponentSpecification.parseSpec(property, "sample", null, "test.path");
		Assert.assertEquals(1, spec.getProperties().size());
		PropertyDescription pd = spec.getProperties().get("myproperty");
		Assert.assertNotNull(pd);
		Assert.assertNotNull(pd.getType().getCustomJSONTypeDefinition());
		Object config = pd.getConfig();
		Assert.assertFalse(pd.isArray());

		PropertyDescription wct = pd.getType().getCustomJSONTypeDefinition();
		Assert.assertEquals("mytype", wct.getName());
		Assert.assertEquals(1, wct.getProperties().size());
		PropertyDescription pd2 = wct.getProperty("typeproperty");
		Assert.assertNotNull(pd2);
		Assert.assertTrue(pd2.getType() == IPropertyType.Default.string.getType());
		Assert.assertFalse(pd2.isArray());
	}

	@Test
	public void testArrayOwnTypeProperyType() throws JSONException
	{
		String property = "name:'test',definition:'/test.js', model: {myproperty:'mytype[]'}, types: {mytype:{model:{typeproperty:'string'}}}";

		WebComponentSpecification spec = WebComponentSpecification.parseSpec(property, "sample", null, "test.path");
		Assert.assertEquals(1, spec.getProperties().size());
		PropertyDescription pd = spec.getProperties().get("myproperty");
		Assert.assertNotNull(pd);
		Assert.assertNotNull(pd.getType().getCustomJSONTypeDefinition());
		Object config = pd.getConfig();
		Assert.assertTrue(pd.isArray());

		PropertyDescription wct = pd.getType().getCustomJSONTypeDefinition();
		Assert.assertEquals("mytype", wct.getName());
		Assert.assertEquals(1, wct.getProperties().size());
		PropertyDescription pd2 = wct.getProperty("typeproperty");
		Assert.assertNotNull(pd2);
		Assert.assertTrue(pd2.getType() == IPropertyType.Default.string.getType());
		Assert.assertFalse(pd2.isArray());

	}

	@Test
	public void testArrayOwnTypeProperyTypeAsArray() throws JSONException
	{
		String property = "name:'test',definition:'/test.js', model: {myproperty:'mytype[]'}, types: {mytype:{model:{typeproperty:'string[]'}}}";

		WebComponentSpecification spec = WebComponentSpecification.parseSpec(property, "sample", null, "test.path");
		Assert.assertEquals(1, spec.getProperties().size());
		PropertyDescription pd = spec.getProperties().get("myproperty");
		Assert.assertNotNull(pd);
		Assert.assertNotNull(pd.getType().getCustomJSONTypeDefinition());
		Object config = pd.getConfig();
		Assert.assertTrue(pd.isArray());

		PropertyDescription wct = pd.getType().getCustomJSONTypeDefinition();
		Assert.assertEquals("mytype", wct.getName());
		Assert.assertEquals(1, wct.getProperties().size());
		PropertyDescription pd2 = wct.getProperty("typeproperty");
		Assert.assertNotNull(pd2);
		Assert.assertTrue(pd2.getType() == IPropertyType.Default.string.getType());
		Assert.assertTrue(pd2.isArray());

	}

	@Test
	public void testOwnTypeProperyTypeRefernceInOtherOwnType() throws JSONException
	{
		String property = "name:'test',definition:'/test.js', model: {myproperty:'mytype'}, types: {mytype:{model:{typeproperty:'mytype2'}},mytype2:{model:{typeproperty:'string'}}}";

		WebComponentSpecification spec = WebComponentSpecification.parseSpec(property, "sample", null, "test.path");
		Assert.assertEquals(1, spec.getProperties().size());
		PropertyDescription pd = spec.getProperties().get("myproperty");
		Assert.assertNotNull(pd);
		Assert.assertNotNull(pd.getType().getCustomJSONTypeDefinition());
		Object config = pd.getConfig();
		Assert.assertTrue(config instanceof PropertyDescription);
		Assert.assertFalse(pd.isArray());

		PropertyDescription wct = pd.getType().getCustomJSONTypeDefinition();
		Assert.assertEquals("mytype", wct.getName());
		Assert.assertEquals(1, wct.getProperties().size());
		PropertyDescription pd2 = wct.getProperties().get("typeproperty");
		Assert.assertNotNull(pd2);
		Assert.assertNotNull(pd2.getType().getCustomJSONTypeDefinition());
		Assert.assertFalse(pd2.isArray());

		config = pd2.getConfig();
		PropertyDescription wct2 = pd.getType().getCustomJSONTypeDefinition();
		Assert.assertEquals("mytype2", wct2.getName());
		Assert.assertEquals(1, wct2.getProperties().size());
		PropertyDescription pd3 = wct2.getProperty("typeproperty");
		Assert.assertNotNull(pd3);
		Assert.assertTrue(pd3.getType() == IPropertyType.Default.string.getType());
		Assert.assertFalse(pd3.isArray());
	}

	@Test
	public void testNames() throws JSONException
	{
		String property = "name:'test',definition:'/test.js'";
		WebComponentSpecification spec = WebComponentSpecification.parseSpec(property, "sample", null, "test.path");
		Assert.assertEquals("test", spec.getName());
		Assert.assertEquals("test", spec.getDisplayName());
		Assert.assertEquals("sample", spec.getPackageName());
		Assert.assertEquals("sample:test", spec.getFullName());

		property = "name:'test', displayName: 'A Test',definition:'/test.js'";
		spec = WebComponentSpecification.parseSpec(property, "sample", null, "test.path");
		Assert.assertEquals("test", spec.getName());
		Assert.assertEquals("A Test", spec.getDisplayName());
		Assert.assertEquals("sample", spec.getPackageName());
		Assert.assertEquals("sample:test", spec.getFullName());
	}
}
