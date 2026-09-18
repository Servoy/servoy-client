package com.servoy.j2db.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.PropertyDescriptionBuilder;
import org.sablo.specification.property.CustomJSONArrayType;
import org.sablo.specification.property.CustomJSONObjectType;
import org.sablo.specification.property.types.StringPropertyType;

import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.UUID;

/**
 * Regression tests for SVY-21475: tab names in tab panels were lost after a Developer restart
 * because {@link WebCustomType#getName()} read the legacy {@code AbstractBase} propertiesMap
 * instead of the JSON-backed generic property path, and {@code "name"} was excluded from the
 * JSON-backed path by being included in {@code purePersistPropertyNames}.
 * <p>
 * Mirrors the scaffolding used by {@link WebCustomTypeAddChildTest} (DummySolution,
 * TestableWebComponent, PropertyDescriptionBuilder-built custom types), but simulates a
 * "Developer restart" by round-tripping the parent's own JSON through {@code setJson(...)},
 * which internally triggers {@code initCustomTypes()} and reconstructs brand-new
 * {@code WebCustomType} child instances purely from JSON - exactly what happens when Eclipse
 * restarts and reloads a solution from disk.
 */
class WebCustomTypeNamePropertyTest
{
	private DummySolution solution;
	private Form form;
	private CustomJSONObjectType<Object, Object> tabType;
	private PropertyDescription componentPd;

	@BeforeEach
	void setUp() throws RepositoryException
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

		// mirror the real tab.name/tab.text sub-properties from bootstrapcomponents-tabpanel /
		// servoydefault-tabpanel tabpanel.spec
		PropertyDescription namePd = new PropertyDescriptionBuilder()
			.withName(StaticContentSpecLoader.PROPERTY_NAME.getPropertyName())
			.withType(StringPropertyType.INSTANCE)
			.build();
		PropertyDescription textPd = new PropertyDescriptionBuilder()
			.withName("text") //$NON-NLS-1$
			.withType(StringPropertyType.INSTANCE)
			.build();

		tabType = new CustomJSONObjectType<>("test.tab", null); //$NON-NLS-1$
		PropertyDescription tabDef = new PropertyDescriptionBuilder()
			.withName("test.tab") //$NON-NLS-1$
			.withType(StringPropertyType.INSTANCE)
			.withProperty(StaticContentSpecLoader.PROPERTY_NAME.getPropertyName(), namePd)
			.withProperty("text", textPd) //$NON-NLS-1$
			.build();
		tabType.setCustomJSONDefinition(tabDef);

		PropertyDescription tabArrayPd = new PropertyDescriptionBuilder()
			.withName("tabs") //$NON-NLS-1$
			.withType(new CustomJSONArrayType<>(
				new PropertyDescriptionBuilder()
					.withName("tabs") //$NON-NLS-1$
					.withType(tabType)
					.build()))
			.build();

		componentPd = new PropertyDescriptionBuilder()
			.withName("testTabPanelComponent") //$NON-NLS-1$
			.withType(StringPropertyType.INSTANCE)
			.withProperty("tabs", tabArrayPd) //$NON-NLS-1$
			.build();
	}

	private TestableWebComponent createWebComponent()
	{
		TestableWebComponent wc = new TestableWebComponent(form, UUID.randomUUID(), componentPd);
		wc.setJson(new ServoyJSONObject());
		wc.setTypeName("test.tabpanel"); //$NON-NLS-1$
		return wc;
	}

	@Nested
	class SameSessionReadAfterWrite
	{
		@Test
		void testSetPropertyThenGetPropertyReturnsValue()
		{
			TestableWebComponent wc = createWebComponent();
			WebCustomType tab = WebCustomType.createNewInstance(wc, tabType.getCustomJSONTypeDefinition(), "tabs", 0); //$NON-NLS-1$

			tab.setProperty(StaticContentSpecLoader.PROPERTY_NAME.getPropertyName(), "myTabName"); //$NON-NLS-1$

			assertEquals("myTabName", tab.getProperty(StaticContentSpecLoader.PROPERTY_NAME.getPropertyName())); //$NON-NLS-1$
		}

		@Test
		void testSetNameThenGetNameReturnsValue()
		{
			TestableWebComponent wc = createWebComponent();
			WebCustomType tab = WebCustomType.createNewInstance(wc, tabType.getCustomJSONTypeDefinition(), "tabs", 0); //$NON-NLS-1$

			tab.setName("myTabName"); //$NON-NLS-1$

			assertEquals("myTabName", tab.getName()); //$NON-NLS-1$
		}
	}

	@Nested
	class AfterDeveloperRestartSimulation
	{
		@Test
		void testGetPropertyNameSurvivesRestart()
		{
			TestableWebComponent wc = createWebComponent();
			WebCustomType tab = WebCustomType.createNewInstance(wc, tabType.getCustomJSONTypeDefinition(), "tabs", 0); //$NON-NLS-1$
			tab.setProperty(StaticContentSpecLoader.PROPERTY_NAME.getPropertyName(), "myTabName"); //$NON-NLS-1$
			tab.setProperty("text", "myTabText"); //$NON-NLS-1$ //$NON-NLS-2$

			// simulate a Developer restart: capture the parent's own JSON and feed it back into
			// setJson(), which internally calls initCustomTypes() and rebuilds brand-new
			// WebCustomType child instances purely from JSON, just like reloading a solution.
			JSONObject wcOwnJson = (JSONObject)wc.getOwnProperty(StaticContentSpecLoader.PROPERTY_JSON.getPropertyName());
			wc.setJson(wcOwnJson);

			List<IPersist> reloadedChildren = wc.getAllObjectsAsList();
			assertEquals(1, reloadedChildren.size());
			WebCustomType reloadedTab = (WebCustomType)reloadedChildren.get(0);

			// this is the assertion that failed before the fix (returned null)
			assertEquals("myTabName", reloadedTab.getProperty(StaticContentSpecLoader.PROPERTY_NAME.getPropertyName())); //$NON-NLS-1$
		}

		@Test
		void testGetNameSurvivesRestart()
		{
			TestableWebComponent wc = createWebComponent();
			WebCustomType tab = WebCustomType.createNewInstance(wc, tabType.getCustomJSONTypeDefinition(), "tabs", 0); //$NON-NLS-1$
			tab.setName("myTabName"); //$NON-NLS-1$
			tab.setProperty("text", "myTabText"); //$NON-NLS-1$ //$NON-NLS-2$

			JSONObject wcOwnJson = (JSONObject)wc.getOwnProperty(StaticContentSpecLoader.PROPERTY_JSON.getPropertyName());
			wc.setJson(wcOwnJson);

			List<IPersist> reloadedChildren = wc.getAllObjectsAsList();
			assertEquals(1, reloadedChildren.size());
			WebCustomType reloadedTab = (WebCustomType)reloadedChildren.get(0);

			// this is the assertion that failed before the fix (returned null)
			assertEquals("myTabName", reloadedTab.getName()); //$NON-NLS-1$
		}

		@Test
		void testOrdinarySubPropertyStillRoundTripsAfterRestart()
		{
			// no-regression guard on the rest of getProperty/setProperty
			TestableWebComponent wc = createWebComponent();
			WebCustomType tab = WebCustomType.createNewInstance(wc, tabType.getCustomJSONTypeDefinition(), "tabs", 0); //$NON-NLS-1$
			tab.setName("myTabName"); //$NON-NLS-1$
			tab.setProperty("text", "myTabText"); //$NON-NLS-1$ //$NON-NLS-2$

			JSONObject wcOwnJson = (JSONObject)wc.getOwnProperty(StaticContentSpecLoader.PROPERTY_JSON.getPropertyName());
			wc.setJson(wcOwnJson);

			WebCustomType reloadedTab = (WebCustomType)wc.getAllObjectsAsList().get(0);

			assertNotNull(reloadedTab.getProperty("text")); //$NON-NLS-1$
			assertEquals("myTabText", reloadedTab.getProperty("text")); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private static class DummySolution extends AbstractRootObject implements ISupportChilds
	{
		DummySolution()
		{
			super(null, new RootObjectMetaData(UUID.randomUUID(), "testSolution", IRepository.SOLUTIONS, 0, 0)); //$NON-NLS-1$
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
