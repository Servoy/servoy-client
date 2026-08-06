package com.servoy.j2db.persistence;

import static org.junit.Assert.*;

import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.PropertyDescriptionBuilder;
import org.sablo.specification.property.CustomJSONArrayType;
import org.sablo.specification.property.CustomJSONObjectType;

import org.sablo.specification.property.types.StringPropertyType;

import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.persistence.ContentSpec;

public class WebCustomTypeAddChildTest
{
	private DummySolution solution;
	private Form form;
	private PropertyDescription columnArrayPd;
	private PropertyDescription columnPd;
	private PropertyDescription nestedSinglePd;
	private CustomJSONObjectType<Object, Object> columnType;
	private CustomJSONObjectType<Object, Object> nestedType;
	private PropertyDescription componentPd;

	@Before
	public void setUp() throws RepositoryException
	{
		solution = new DummySolution();
		solution.setChangeHandler(new ChangeHandler(new AbstractPersistFactory()
		{
			@Override
			public void initClone(IPersist clone, IPersist objToClone, boolean flattenOverrides)
			{
			}

			@Override
			protected IPersist createRootObject(UUID rootObjectUUID)
			{
				return null;
			}

			@Override
			protected ContentSpec loadContentSpec()
			{
				return null;
			}
		}));
		form = (Form)solution.getChangeHandler().createNewObject(solution, IRepository.FORMS);

		nestedType = new CustomJSONObjectType<>("test.nested", null);
		PropertyDescription nestedDef = new PropertyDescriptionBuilder()
			.withName("test.nested")
			.withType(StringPropertyType.INSTANCE)
			.build();
		nestedType.setCustomJSONDefinition(nestedDef);

		nestedSinglePd = new PropertyDescriptionBuilder()
			.withName("singleNested")
			.withType(nestedType)
			.build();

		columnType = new CustomJSONObjectType<>("test.column", null);
		PropertyDescription columnDef = new PropertyDescriptionBuilder()
			.withName("test.column")
			.withType(StringPropertyType.INSTANCE)
			.withProperty("singleNested", nestedSinglePd)
			.build();
		columnType.setCustomJSONDefinition(columnDef);

		CustomJSONArrayType<Object, Object> columnArrayType = new CustomJSONArrayType<>(
			new PropertyDescriptionBuilder()
				.withName("columns")
				.withType(columnType)
				.build());

		columnArrayPd = new PropertyDescriptionBuilder()
			.withName("columns")
			.withType(columnArrayType)
			.build();

		columnPd = new PropertyDescriptionBuilder()
			.withName("singleColumn")
			.withType(columnType)
			.build();

		componentPd = new PropertyDescriptionBuilder()
			.withName("testComponent")
			.withType(StringPropertyType.INSTANCE)
			.withProperty("columns", columnArrayPd)
			.withProperty("singleColumn", columnPd)
			.build();
	}

	private TestableWebComponent createWebComponent()
	{
		TestableWebComponent wc = new TestableWebComponent(form, UUID.randomUUID(), componentPd);
		wc.setJson(new ServoyJSONObject());
		wc.setTypeName("test.component");
		return wc;
	}

	@Test
	public void testConstructorCreatesJsonForArrayProperty()
	{
		TestableWebComponent wc = createWebComponent();

		WebCustomType customType = WebCustomType.createNewInstance(wc, columnType.getCustomJSONTypeDefinition(), "columns", 0);
		assertNotNull(customType);
		assertNotNull(customType.getFullJsonInFrmFile());

		JSONObject wcJson = (JSONObject)wc.getOwnProperty(StaticContentSpecLoader.PROPERTY_JSON.getPropertyName());
		JSONArray columnsArray = wcJson.optJSONArray("columns");
		assertNotNull(columnsArray);
		assertEquals(1, columnsArray.length());
		assertEquals(customType.getUUID().toString(), columnsArray.getJSONObject(0).getString(IChildWebObject.UUID_KEY));
	}

	@Test
	public void testConstructorCreatesJsonForSingleProperty()
	{
		TestableWebComponent wc = createWebComponent();

		WebCustomType singleCustomType = WebCustomType.createNewInstance(wc, columnType.getCustomJSONTypeDefinition(), "singleColumn", -1);
		assertNotNull(singleCustomType);

		JSONObject wcJson = (JSONObject)wc.getOwnProperty(StaticContentSpecLoader.PROPERTY_JSON.getPropertyName());
		JSONObject singleColumnJson = wcJson.optJSONObject("singleColumn");
		assertNotNull(singleColumnJson);
		assertEquals(singleCustomType.getUUID().toString(), singleColumnJson.getString(IChildWebObject.UUID_KEY));
	}

	@Test
	public void testNestedCustomTypeInsertedIntoParentJson()
	{
		TestableWebComponent wc = createWebComponent();

		PropertyDescription parentPdWithNested = new PropertyDescriptionBuilder()
			.withName("columnDef")
			.withType(StringPropertyType.INSTANCE)
			.withProperty("singleNested", nestedSinglePd)
			.build();

		WebCustomType parentCustomType = WebCustomType.createNewInstance(wc, parentPdWithNested, "singleColumn", -1);
		WebCustomType childCustomType = WebCustomType.createNewInstance(parentCustomType,
			nestedType.getCustomJSONTypeDefinition(), "singleNested", -1);

		JSONObject parentJson = parentCustomType.getFullJsonInFrmFile();
		assertNotNull(parentJson);
		JSONObject nestedJson = parentJson.optJSONObject("singleNested");
		assertNotNull(nestedJson);
		assertEquals(childCustomType.getUUID().toString(), nestedJson.getString(IChildWebObject.UUID_KEY));
	}

	@Test
	public void testNestedCustomTypeArrayInsertedIntoParentJson()
	{
		TestableWebComponent wc = createWebComponent();

		PropertyDescription nestedArrayPd = new PropertyDescriptionBuilder()
			.withName("nestedItems")
			.withType(new CustomJSONArrayType<>(
				new PropertyDescriptionBuilder()
					.withName("nestedItems")
					.withType(nestedType)
					.build()))
			.build();

		PropertyDescription parentPdWithArray = new PropertyDescriptionBuilder()
			.withName("columnDef")
			.withType(StringPropertyType.INSTANCE)
			.withProperty("nestedItems", nestedArrayPd)
			.build();

		WebCustomType parentCustomType = WebCustomType.createNewInstance(wc, parentPdWithArray, "singleColumn", -1);
		WebCustomType child = WebCustomType.createNewInstance(parentCustomType,
			nestedType.getCustomJSONTypeDefinition(), "nestedItems", 0);

		JSONObject parentJson = parentCustomType.getFullJsonInFrmFile();
		JSONArray nestedArray = parentJson.optJSONArray("nestedItems");
		assertNotNull(nestedArray);
		assertEquals(1, nestedArray.length());
		assertEquals(child.getUUID().toString(), nestedArray.getJSONObject(0).getString(IChildWebObject.UUID_KEY));
	}

	@Test
	public void testMultipleLevelsOfNesting()
	{
		TestableWebComponent wc = createWebComponent();

		CustomJSONObjectType<Object, Object> level2Type = new CustomJSONObjectType<>("test.level2", null);
		level2Type.setCustomJSONDefinition(
			new PropertyDescriptionBuilder().withName("level2Def").withType(StringPropertyType.INSTANCE).build());

		PropertyDescription level2Pd = new PropertyDescriptionBuilder()
			.withName("level2")
			.withType(level2Type)
			.build();

		CustomJSONObjectType<Object, Object> level1Type = new CustomJSONObjectType<>("test.level1", null);
		PropertyDescription level1Def = new PropertyDescriptionBuilder()
			.withName("level1Def")
			.withType(StringPropertyType.INSTANCE)
			.withProperty("level2", level2Pd)
			.build();
		level1Type.setCustomJSONDefinition(level1Def);

		PropertyDescription level1Pd = new PropertyDescriptionBuilder()
			.withName("level1")
			.withType(level1Type)
			.build();

		PropertyDescription rootPd = new PropertyDescriptionBuilder()
			.withName("columnDef")
			.withType(StringPropertyType.INSTANCE)
			.withProperty("level1", level1Pd)
			.build();

		WebCustomType level0 = WebCustomType.createNewInstance(wc, rootPd, "singleColumn", -1);
		WebCustomType level1 = WebCustomType.createNewInstance(level0, level1Type.getCustomJSONTypeDefinition(), "level1", -1);
		WebCustomType level2 = WebCustomType.createNewInstance(level1, level2Type.getCustomJSONTypeDefinition(), "level2", -1);

		JSONObject level0Json = level0.getFullJsonInFrmFile();
		JSONObject level1Json = level0Json.optJSONObject("level1");
		assertNotNull(level1Json);
		assertEquals(level1.getUUID().toString(), level1Json.getString(IChildWebObject.UUID_KEY));

		JSONObject level1OwnJson = level1.getFullJsonInFrmFile();
		JSONObject level2Json = level1OwnJson.optJSONObject("level2");
		assertNotNull(level2Json);
		assertEquals(level2.getUUID().toString(), level2Json.getString(IChildWebObject.UUID_KEY));
	}

	@Test
	public void testAddChildOverridePreservesExplicitUuid()
	{
		TestableWebComponent wc = createWebComponent();

		PropertyDescription parentPdWithNested = new PropertyDescriptionBuilder()
			.withName("columnDef")
			.withType(StringPropertyType.INSTANCE)
			.withProperty("singleNested", nestedSinglePd)
			.build();

		WebCustomType parentCustomType = WebCustomType.createNewInstance(wc, parentPdWithNested, "singleColumn", -1);

		UUID childUuid = UUID.randomUUID();
		WebCustomType childCustomType = WebCustomType.createNewInstance(parentCustomType,
			nestedType.getCustomJSONTypeDefinition(), "singleNested", -1, childUuid);

		JSONObject parentJson = parentCustomType.getFullJsonInFrmFile();
		JSONObject nestedJson = parentJson.optJSONObject("singleNested");
		assertNotNull(nestedJson);
		assertEquals(childUuid.toString(), nestedJson.getString(IChildWebObject.UUID_KEY));
	}

	@Test
	public void testNestedArrayMultipleElementsPreserveOrder()
	{
		TestableWebComponent wc = createWebComponent();

		PropertyDescription nestedArrayPd = new PropertyDescriptionBuilder()
			.withName("nestedItems")
			.withType(new CustomJSONArrayType<>(
				new PropertyDescriptionBuilder()
					.withName("nestedItems")
					.withType(nestedType)
					.build()))
			.build();

		PropertyDescription parentPdWithArray = new PropertyDescriptionBuilder()
			.withName("columnDef")
			.withType(StringPropertyType.INSTANCE)
			.withProperty("nestedItems", nestedArrayPd)
			.build();

		WebCustomType parentCustomType = WebCustomType.createNewInstance(wc, parentPdWithArray, "singleColumn", -1);
		WebCustomType child0 = WebCustomType.createNewInstance(parentCustomType,
			nestedType.getCustomJSONTypeDefinition(), "nestedItems", 0);
		WebCustomType child1 = WebCustomType.createNewInstance(parentCustomType,
			nestedType.getCustomJSONTypeDefinition(), "nestedItems", 1);

		JSONObject parentJson = parentCustomType.getFullJsonInFrmFile();
		JSONArray nestedArray = parentJson.optJSONArray("nestedItems");
		assertNotNull(nestedArray);
		assertEquals(2, nestedArray.length());
		assertEquals(child0.getUUID().toString(), nestedArray.getJSONObject(0).getString(IChildWebObject.UUID_KEY));
		assertEquals(child1.getUUID().toString(), nestedArray.getJSONObject(1).getString(IChildWebObject.UUID_KEY));
	}

	@Test
	public void testSetPropertyOnNestedCustomTypePersistsInJson()
	{
		TestableWebComponent wc = createWebComponent();

		PropertyDescription textPd = new PropertyDescriptionBuilder()
			.withName("text")
			.withType(org.sablo.specification.property.types.StringPropertyType.INSTANCE)
			.build();

		PropertyDescription parentPdWithNested = new PropertyDescriptionBuilder()
			.withName("columnDef")
			.withType(StringPropertyType.INSTANCE)
			.withProperty("singleNested", nestedSinglePd)
			.build();

		PropertyDescription nestedDefWithText = new PropertyDescriptionBuilder()
			.withName("nestedDef")
			.withType(nestedType)
			.withProperty("text", textPd)
			.build();

		WebCustomType parentCustomType = WebCustomType.createNewInstance(wc, parentPdWithNested, "singleColumn", -1);
		WebCustomType childCustomType = WebCustomType.createNewInstance(parentCustomType,
			nestedDefWithText, "singleNested", -1);

		childCustomType.setProperty("text", "hello");

		JSONObject childJson = childCustomType.getFullJsonInFrmFile();
		assertEquals("hello", childJson.optString("text"));
	}

	@Test
	public void testRemoveNestedChildFromArrayClearsJson()
	{
		TestableWebComponent wc = createWebComponent();

		PropertyDescription nestedArrayPd = new PropertyDescriptionBuilder()
			.withName("singleNested")
			.withType(new CustomJSONArrayType<>(
				new PropertyDescriptionBuilder()
					.withName("singleNested")
					.withType(nestedType)
					.build()))
			.build();

		PropertyDescription parentPdWithNested = new PropertyDescriptionBuilder()
			.withName("columnDef")
			.withType(StringPropertyType.INSTANCE)
			.withProperty("singleNested", nestedArrayPd)
			.build();

		WebCustomType parentCustomType = WebCustomType.createNewInstance(wc, parentPdWithNested, "singleColumn", -1);
		WebCustomType childCustomType = WebCustomType.createNewInstance(parentCustomType,
			nestedType.getCustomJSONTypeDefinition(), "singleNested", 0);

		JSONObject parentJson = parentCustomType.getFullJsonInFrmFile();
		JSONArray arr = parentJson.optJSONArray("singleNested");
		assertNotNull(arr);
		assertEquals(1, arr.length());

		parentCustomType.removeChild(childCustomType);

		arr = parentJson.optJSONArray("singleNested");
		assertEquals(0, arr.length());
	}

	@Test
	public void testRemoveFromNestedArrayClearsJsonEntry()
	{
		TestableWebComponent wc = createWebComponent();

		PropertyDescription nestedArrayPd = new PropertyDescriptionBuilder()
			.withName("nestedItems")
			.withType(new CustomJSONArrayType<>(
				new PropertyDescriptionBuilder()
					.withName("nestedItems")
					.withType(nestedType)
					.build()))
			.build();

		PropertyDescription parentPdWithArray = new PropertyDescriptionBuilder()
			.withName("columnDef")
			.withType(StringPropertyType.INSTANCE)
			.withProperty("nestedItems", nestedArrayPd)
			.build();

		WebCustomType parentCustomType = WebCustomType.createNewInstance(wc, parentPdWithArray, "singleColumn", -1);
		WebCustomType child0 = WebCustomType.createNewInstance(parentCustomType,
			nestedType.getCustomJSONTypeDefinition(), "nestedItems", 0);
		WebCustomType child1 = WebCustomType.createNewInstance(parentCustomType,
			nestedType.getCustomJSONTypeDefinition(), "nestedItems", 1);

		JSONObject parentJson = parentCustomType.getFullJsonInFrmFile();
		JSONArray nestedArray = parentJson.optJSONArray("nestedItems");
		assertEquals(2, nestedArray.length());

		parentCustomType.removeChild(child0);

		nestedArray = parentJson.optJSONArray("nestedItems");
		assertEquals(1, nestedArray.length());
		assertEquals(child1.getUUID().toString(), nestedArray.getJSONObject(0).getString(IChildWebObject.UUID_KEY));
	}

	@Test
	public void testWebComponentAddChildDirectArrayStillWorks()
	{
		TestableWebComponent wc = createWebComponent();

		WebCustomType col0 = WebCustomType.createNewInstance(wc, columnType.getCustomJSONTypeDefinition(), "columns", 0);
		WebCustomType col1 = WebCustomType.createNewInstance(wc, columnType.getCustomJSONTypeDefinition(), "columns", 1);

		JSONObject wcJson = (JSONObject)wc.getOwnProperty(StaticContentSpecLoader.PROPERTY_JSON.getPropertyName());
		JSONArray columnsArray = wcJson.optJSONArray("columns");
		assertNotNull(columnsArray);
		assertEquals(2, columnsArray.length());
		assertEquals(col0.getUUID().toString(), columnsArray.getJSONObject(0).getString(IChildWebObject.UUID_KEY));
		assertEquals(col1.getUUID().toString(), columnsArray.getJSONObject(1).getString(IChildWebObject.UUID_KEY));
	}

	@Test
	public void testWebComponentRemoveChildDirectArray()
	{
		TestableWebComponent wc = createWebComponent();

		WebCustomType col0 = WebCustomType.createNewInstance(wc, columnType.getCustomJSONTypeDefinition(), "columns", 0);
		WebCustomType col1 = WebCustomType.createNewInstance(wc, columnType.getCustomJSONTypeDefinition(), "columns", 1);

		wc.removeChild(col0);

		JSONObject wcJson = (JSONObject)wc.getOwnProperty(StaticContentSpecLoader.PROPERTY_JSON.getPropertyName());
		JSONArray columnsArray = wcJson.optJSONArray("columns");
		assertNotNull(columnsArray);
		assertEquals(1, columnsArray.length());
		assertEquals(col1.getUUID().toString(), columnsArray.getJSONObject(0).getString(IChildWebObject.UUID_KEY));
	}

	@Test
	public void testSetWebComponentPropertyOnNestedCustomType()
	{
		TestableWebComponent wc = createWebComponent();

		PropertyDescription nestedArrayPd = new PropertyDescriptionBuilder()
			.withName("nestedItems")
			.withType(new CustomJSONArrayType<>(
				new PropertyDescriptionBuilder()
					.withName("nestedItems")
					.withType(nestedType)
					.build()))
			.build();

		PropertyDescription parentPdWithArray = new PropertyDescriptionBuilder()
			.withName("columnDef")
			.withType(StringPropertyType.INSTANCE)
			.withProperty("nestedItems", nestedArrayPd)
			.build();

		WebCustomType parentCustomType = WebCustomType.createNewInstance(wc, parentPdWithArray, "columns", 0);

		WebCustomType nested0 = WebCustomType.createNewInstance(parentCustomType,
			nestedType.getCustomJSONTypeDefinition(), "nestedItems", 0);
		WebCustomType nested1 = WebCustomType.createNewInstance(parentCustomType,
			nestedType.getCustomJSONTypeDefinition(), "nestedItems", 1);

		IChildWebObject[] newChildren = new IChildWebObject[] { nested0, nested1 };
		com.servoy.j2db.util.PersistHelper.setWebComponentProperty(parentCustomType, "nestedItems", newChildren);

		JSONObject parentJson = parentCustomType.getFullJsonInFrmFile();
		JSONArray nestedArray = parentJson.optJSONArray("nestedItems");
		assertNotNull(nestedArray);
		assertEquals(2, nestedArray.length());
		assertEquals(nested0.getUUID().toString(), nestedArray.getJSONObject(0).getString(IChildWebObject.UUID_KEY));
		assertEquals(nested1.getUUID().toString(), nestedArray.getJSONObject(1).getString(IChildWebObject.UUID_KEY));
	}

	@Test
	public void testGetChildWithNullReturnsNull()
	{
		TestableWebComponent wc = createWebComponent();
		WebCustomType.createNewInstance(wc, columnType.getCustomJSONTypeDefinition(), "columns", 0);
		assertNull(wc.getChild(null));
	}

	@Test
	public void testGetChildNullOnEmptyComponent()
	{
		TestableWebComponent wc = createWebComponent();
		assertNull(wc.getChild(null));
	}

	@Test
	public void testClonePersistWithColumns()
	{
		TestableWebComponent wc = createWebComponent();
		WebCustomType.createNewInstance(wc, columnType.getCustomJSONTypeDefinition(), "columns", 0);
		WebCustomType.createNewInstance(wc, columnType.getCustomJSONTypeDefinition(), "columns", 1);
		WebCustomType.createNewInstance(wc, columnType.getCustomJSONTypeDefinition(), "columns", 2);
		WebComponent clonedWc = (WebComponent)wc.clonePersist(null);
		assertEquals(3, clonedWc.getAllObjectsAsList().size());
	}

	@Test
	public void testClonedChildrenShareParentJsonEntries()
	{
		TestableWebComponent wc = createWebComponent();
		WebCustomType.createNewInstance(wc, columnType.getCustomJSONTypeDefinition(), "columns", 0);
		WebCustomType.createNewInstance(wc, columnType.getCustomJSONTypeDefinition(), "columns", 1);

		WebComponent clonedWc = (WebComponent)wc.clonePersist(null);
		JSONObject clonedJson = (JSONObject)clonedWc.getOwnProperty(
			StaticContentSpecLoader.PROPERTY_JSON.getPropertyName());
		JSONArray clonedColumns = clonedJson.optJSONArray("columns");
		assertNotNull(clonedColumns);

		java.util.List<IPersist> children = clonedWc.getAllObjectsAsList();
		assertEquals(2, children.size());
		for (int i = 0; i < children.size(); i++)
		{
			WebCustomType child = (WebCustomType)children.get(i);
			assertEquals(child.getUUID().toString(),
				clonedColumns.getJSONObject(i).getString(IChildWebObject.UUID_KEY));
		}
	}

	@Test
	public void testClonedJsonPreservesData()
	{
		TestableWebComponent wc = createWebComponent();
		WebCustomType.createNewInstance(wc, columnType.getCustomJSONTypeDefinition(), "columns", 0);

		WebComponent clonedWc = (WebComponent)wc.clonePersist(null);
		JSONObject clonedJson = (JSONObject)clonedWc.getOwnProperty(
			StaticContentSpecLoader.PROPERTY_JSON.getPropertyName());
		assertNotNull(clonedJson);
		assertNotNull(clonedJson.optJSONArray("columns"));
		assertEquals(1, clonedJson.optJSONArray("columns").length());
	}

	@Test
	public void testClonePersistWithNestedCustomType()
	{
		TestableWebComponent wc = createWebComponent();
		WebCustomType col = WebCustomType.createNewInstance(wc, columnType.getCustomJSONTypeDefinition(), "columns", 0);
		WebCustomType.createNewInstance(col, nestedType.getCustomJSONTypeDefinition(), "singleNested", -1);
		wc.clonePersist(null);
	}

	@Test
	public void testClonedNestedChildrenHaveCorrectJsonRefs()
	{
		TestableWebComponent wc = createWebComponent();
		WebCustomType col = WebCustomType.createNewInstance(wc, columnType.getCustomJSONTypeDefinition(), "columns", 0);
		WebCustomType.createNewInstance(col, nestedType.getCustomJSONTypeDefinition(), "singleNested", -1);

		WebComponent clonedWc = (WebComponent)wc.clonePersist(null);
		java.util.List<IPersist> clonedChildren = clonedWc.getAllObjectsAsList();
		assertEquals(1, clonedChildren.size());

		WebCustomType clonedCol = (WebCustomType)clonedChildren.get(0);
		java.util.List<IPersist> nestedChildren = clonedCol.getAllObjectsAsList();
		assertEquals(1, nestedChildren.size());

		WebCustomType clonedNested = (WebCustomType)nestedChildren.get(0);
		JSONObject colJson = clonedCol.getJson();
		JSONObject nestedJsonFromParent = colJson.optJSONObject("singleNested");
		assertNotNull(nestedJsonFromParent);
		assertEquals(clonedNested.getUUID().toString(),
			nestedJsonFromParent.getString(IChildWebObject.UUID_KEY));
	}

	@Test
	public void testClonePersistWithJsonEntriesLackingSvyUUID()
	{
		TestableWebComponent wc = createWebComponent();

		JSONObject wcJson = (JSONObject)wc.getOwnProperty(StaticContentSpecLoader.PROPERTY_JSON.getPropertyName());
		JSONArray columnsArray = new JSONArray();
		columnsArray.put(new JSONObject().put("text", "col1"));
		columnsArray.put(new JSONObject().put("text", "col2"));
		columnsArray.put(new JSONObject().put("text", "col3"));
		wcJson.put("columns", columnsArray);

		WebComponent clonedWc = (WebComponent)wc.clonePersist(null);
		assertEquals(3, clonedWc.getAllObjectsAsList().size());
	}


	private static class DummySolution extends AbstractRootObject implements ISupportChilds
	{
		DummySolution()
		{
			super(null, new RootObjectMetaData(UUID.randomUUID(), "testSolution", IRepository.SOLUTIONS, 0, 0));
		}
	}

	private static class TestableWebComponent extends WebComponent
	{
		private final PropertyDescription pd;

		TestableWebComponent(ISupportChilds parent, UUID uuid, PropertyDescription pd)
		{
			super(parent, uuid);
			this.pd = pd;
		}

		@Override
		public PropertyDescription getPropertyDescription()
		{
			return pd;
		}

		void setCustomTypesInitialized(boolean value)
		{
			this.customTypesInitialized = value;
		}

		@Override
		protected void afterChildWasAdded(IPersist obj)
		{
			if (obj instanceof AbstractBase && this instanceof ISupportChilds)
			{
				((AbstractBase)obj).setParent((ISupportChilds)this);
			}
		}
	}
}
