package com.servoy.j2db.persistence;

import static org.junit.Assert.*;

import java.util.Map;

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

public class WebComponentCloneMapIsolationTest
{
	private DummySolution solution;
	private Form form;
	private PropertyDescription componentPd;
	private CustomJSONObjectType<Object, Object> columnType;

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

		columnType = new CustomJSONObjectType<>("test.column", null);
		PropertyDescription columnDef = new PropertyDescriptionBuilder()
			.withName("test.column")
			.withType(StringPropertyType.INSTANCE)
			.build();
		columnType.setCustomJSONDefinition(columnDef);

		CustomJSONArrayType<Object, Object> columnArrayType = new CustomJSONArrayType<>(
			new PropertyDescriptionBuilder()
				.withName("columns")
				.withType(columnType)
				.build());

		PropertyDescription columnArrayPd = new PropertyDescriptionBuilder()
			.withName("columns")
			.withType(columnArrayType)
			.build();

		componentPd = new PropertyDescriptionBuilder()
			.withName("testComponent")
			.withType(StringPropertyType.INSTANCE)
			.withProperty("columns", columnArrayPd)
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
	public void testCloneSucceedsWithoutNpeWhenCustomTypesLackSvyUUID()
	{
		TestableWebComponent wc = createWebComponent();

		JSONObject wcJson = (JSONObject)wc.getOwnProperty(StaticContentSpecLoader.PROPERTY_JSON.getPropertyName());
		JSONArray columnsArray = new JSONArray();
		columnsArray.put(new JSONObject().put("text", "col1"));
		columnsArray.put(new JSONObject().put("text", "col2"));
		wcJson.put("columns", columnsArray);

		WebComponent clonedWc = (WebComponent)wc.clonePersist(null);
		assertNotNull(clonedWc);
		assertEquals(2, clonedWc.getAllObjectsAsList().size());
	}

	@Test
	public void testModifyingClonePropertiesDoesNotMutateSource()
	{
		TestableWebComponent wc = createWebComponent();
		WebCustomType.createNewInstance(wc, columnType.getCustomJSONTypeDefinition(), "columns", 0);

		Map<String, Object> sourceMapBefore = wc.getPropertiesMap();
		int sourcePropertyCountBefore = sourceMapBefore.size();

		WebComponent clonedWc = (WebComponent)wc.clonePersist(null);

		ServoyJSONObject newJson = new ServoyJSONObject();
		newJson.put("extraProp", "extraValue");
		clonedWc.setProperty(StaticContentSpecLoader.PROPERTY_JSON.getPropertyName(), newJson);

		Map<String, Object> sourceMapAfter = wc.getPropertiesMap();
		assertEquals("Source properties map must not be affected by changes to clone",
			sourcePropertyCountBefore, sourceMapAfter.size());
	}

	@Test
	public void testModifyingSourcePropertiesDoesNotMutateClone()
	{
		TestableWebComponent wc = createWebComponent();
		WebCustomType.createNewInstance(wc, columnType.getCustomJSONTypeDefinition(), "columns", 0);

		WebComponent clonedWc = (WebComponent)wc.clonePersist(null);
		Map<String, Object> cloneMapBefore = clonedWc.getPropertiesMap();
		int clonePropertyCountBefore = cloneMapBefore.size();

		ServoyJSONObject newJson = new ServoyJSONObject();
		newJson.put("anotherProp", "anotherValue");
		wc.setProperty(StaticContentSpecLoader.PROPERTY_JSON.getPropertyName(), newJson);

		Map<String, Object> cloneMapAfter = clonedWc.getPropertiesMap();
		assertEquals("Clone properties map must not be affected by changes to source",
			clonePropertyCountBefore, cloneMapAfter.size());
	}

	@Test
	public void testSourceAndCloneHaveDistinctMapInstances()
	{
		TestableWebComponent wc = createWebComponent();
		WebCustomType.createNewInstance(wc, columnType.getCustomJSONTypeDefinition(), "columns", 0);

		WebComponent clonedWc = (WebComponent)wc.clonePersist(null);

		assertNotSame("Source and clone must have independent propertiesMap instances",
			wc.getPropertiesMap(), clonedWc.getPropertiesMap());
	}

	@Test
	public void testWebCustomTypeChildrenInCloneHaveIndependentPropertiesMap()
	{
		TestableWebComponent wc = createWebComponent();
		WebCustomType.createNewInstance(wc, columnType.getCustomJSONTypeDefinition(), "columns", 0);

		WebComponent clonedWc = (WebComponent)wc.clonePersist(null);

		WebCustomType sourceChild = (WebCustomType)wc.getAllObjectsAsList().get(0);
		WebCustomType clonedChild = (WebCustomType)clonedWc.getAllObjectsAsList().get(0);

		assertNotSame("WebCustomType children in clone must have independent propertiesMap",
			sourceChild.getPropertiesMap(), clonedChild.getPropertiesMap());
	}

	@Test
	public void testSetJsonNullDoesNotThrowWhenCustomTypesInitialized()
	{
		TestableWebComponent wc = createWebComponent();
		WebCustomType.createNewInstance(wc, columnType.getCustomJSONTypeDefinition(), "columns", 0);
		wc.setCustomTypesInitialized(true);

		wc.setJson(null);
	}

	@Test
	public void testSetJsonNullDoesNotThrowWhenCustomTypesNotInitialized()
	{
		TestableWebComponent wc = createWebComponent();
		wc.setCustomTypesInitialized(false);

		wc.setJson(null);
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
