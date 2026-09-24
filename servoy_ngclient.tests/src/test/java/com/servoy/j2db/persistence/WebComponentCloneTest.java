package com.servoy.j2db.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
 * Regression tests for SVY-21257: duplicating an AG Grid table (a WebComponent with an
 * array-of-custom-object <code>columns</code> property) must give the pasted copy brand-new
 * <code>svyUUID</code>s for every column, and must never mutate the source component's columns.
 *
 * The initial fix (resetting <code>customTypesInitialized</code> before the resetUUID visitor in
 * {@link WebComponent#cloneObj}) was insufficient for the override / shared-reference path: when a
 * component with an <code>extendsID</code> is cloned, cloneObj stores the flattened JSON on the
 * clone, and that flattened JSON (from {@code PersistHelper.getFlattenedJSON}) shares nested column
 * JSONObject instances with the source. The subsequent <code>resetUUID()</code> then mutated those
 * shared instances, overwriting the SOURCE column svyUUIDs with the clone's new ones -&gt; duplicate
 * UUID errors. The fix deep-clones the flattened JSON before storing it on the clone.
 *
 * Lives beside {@code WebComponentCloneMapIsolationTest} (SVY-21282) and reuses the same
 * {@code DummySolution}/{@code AbstractPersistFactory} scaffolding, with the factory producing a
 * {@code TestableWebComponent} for WEBCOMPONENTS so the clone keeps its test property description
 * and the full {@code initCustomTypes()} -&gt; {@code resetUUID()} flow runs without OSGi.
 *
 * @author opencode
 */
@DisplayName("WebComponent.cloneObj - SVY-21257 duplicate UUID regeneration for custom type children")
public class WebComponentCloneTest
{
	private DummySolution solution;
	private Form sourceForm;
	private Form targetForm;
	private PropertyDescription componentPd;

	@BeforeEach
	public void setUp() throws RepositoryException
	{
		solution = new DummySolution();
		solution.setChangeHandler(new ChangeHandler(new AbstractPersistFactory()
		{
			@Override
			public void initClone(IPersist clone, IPersist objToClone, boolean flattenOverrides) throws RepositoryException
			{
				RepositoryHelper.initClone(clone, objToClone, flattenOverrides);
			}

			@Override
			public IPersist createObject(ISupportChilds parent, int objectTypeId, UUID uuid) throws RepositoryException
			{
				if (objectTypeId == IRepository.WEBCOMPONENTS)
				{
					return new TestableWebComponent(parent, uuid, componentPd);
				}
				return super.createObject(parent, objectTypeId, uuid);
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

		CustomJSONObjectType<Object, Object> columnType = new CustomJSONObjectType<>("columns.column", null);
		PropertyDescription columnDef = new PropertyDescriptionBuilder()
			.withName("columns.column")
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
			.withName("aggrid-groupingtable")
			.withType(StringPropertyType.INSTANCE)
			.withProperty("columns", columnArrayPd)
			.build();

		sourceForm = (Form)solution.getChangeHandler().createNewObject(solution, IRepository.FORMS);
		solution.addChild(sourceForm);

		targetForm = (Form)solution.getChangeHandler().createNewObject(solution, IRepository.FORMS);
		solution.addChild(targetForm);
	}

	private TestableWebComponent createWebComponentWithColumns(ISupportChilds parent, int columnCount)
	{
		TestableWebComponent wc = new TestableWebComponent(parent, UUID.randomUUID(), componentPd);
		wc.setTypeName("aggrid-groupingtable");

		ServoyJSONObject json = new ServoyJSONObject();
		JSONArray columnsJson = new JSONArray();
		for (int i = 0; i < columnCount; i++)
		{
			JSONObject col = new JSONObject();
			col.put(IChildWebObject.UUID_KEY, UUID.randomUUID().toString());
			col.put("dataprovider", "col_" + i);
			col.put("headerTitle", "Column " + i);
			columnsJson.put(col);
		}
		json.put("columns", columnsJson);
		wc.setJson(json);
		return wc;
	}

	private Set<UUID> collectChildUUIDs(WebComponent wc)
	{
		Set<UUID> uuids = new HashSet<>();
		for (IPersist child : wc.getAllObjectsAsList())
		{
			if (child instanceof WebCustomType)
			{
				uuids.add(child.getUUID());
			}
		}
		return uuids;
	}

	private Set<String> collectJsonColumnUUIDs(WebComponent wc)
	{
		Set<String> uuids = new HashSet<>();
		JSONObject json = wc.getJson();
		if (json != null)
		{
			JSONArray cols = json.optJSONArray("columns");
			if (cols != null)
			{
				for (int i = 0; i < cols.length(); i++)
				{
					JSONObject c = cols.optJSONObject(i);
					if (c != null) uuids.add(c.optString(IChildWebObject.UUID_KEY, null));
				}
			}
		}
		return uuids;
	}

	@Nested
	@DisplayName("real svyCloud/billingHistory.frm case: duplicate plain aggrid within same form")
	class RealFormDuplicate
	{
		/**
		 * Mirrors the reported repro: copy the 8-column aggrid-groupingtable and paste it into the
		 * same form. ElementFactory.copyComponent calls cloneObj with changeName/changeChildNames/
		 * flattenOverrides all true. The clone's column svyUUIDs (child persists and stored JSON)
		 * must all differ from the source's; the source must be untouched.
		 */
		@Test
		@DisplayName("pasted clone gets new svyUUIDs, source unchanged")
		void duplicateInSameFormGetsNewUUIDs() throws Exception
		{
			TestableWebComponent original = createWebComponentWithColumns(sourceForm, 8);
			sourceForm.addChild(original);

			// realize the source's custom types first (as happens in the editor once the form is
			// rendered): each WebCustomType child then holds, as its own PROPERTY_JSON, the very
			// same JSONObject instance that lives in the source component's stored columns array.
			original.getAllObjectsAsList();

			Set<String> sourceJsonUUIDsBefore = collectJsonColumnUUIDs(original);
			Set<UUID> sourceChildUUIDsBefore = collectChildUUIDs(original);
			assertEquals(8, sourceChildUUIDsBefore.size(), "source should have 8 columns");

			IPersist clone = original.cloneObj(sourceForm, true, null, true, true, true);
			assertInstanceOf(WebComponent.class, clone);
			WebComponent wcClone = (WebComponent)clone;

			Set<String> cloneJsonUUIDs = collectJsonColumnUUIDs(wcClone);
			Set<UUID> cloneChildUUIDs = collectChildUUIDs(wcClone);
			Set<String> sourceJsonUUIDsAfter = collectJsonColumnUUIDs(original);

			assertEquals(8, cloneChildUUIDs.size(), "clone should have 8 columns");
			assertEquals(sourceJsonUUIDsBefore, sourceJsonUUIDsAfter, "source column svyUUIDs must not change");

			for (String cloneUUID : cloneJsonUUIDs)
			{
				assertFalse(sourceJsonUUIDsAfter.contains(cloneUUID),
					"clone column svyUUID " + cloneUUID + " must not equal any SOURCE column svyUUID (duplicate!)");
			}

			Set<String> cloneChildUUIDStrings = new HashSet<>();
			for (UUID u : cloneChildUUIDs)
				cloneChildUUIDStrings.add(u.toString());
			assertEquals(cloneChildUUIDStrings, cloneJsonUUIDs, "clone child svyUUIDs must match clone stored JSON svyUUIDs");
		}
	}

	@Nested
	@DisplayName("override component whose flattened JSON shares column instances with the source")
	class SharedReferenceOverrideClone
	{
		/**
		 * The assertion that failed before the deep-clone fix: the flattened JSON shared column
		 * instances with the source, so the clone's resetUUID() mutated the source's svyUUIDs.
		 */
		@Test
		@DisplayName("clone JSON column svyUUIDs differ from source column svyUUIDs")
		void cloneUUIDsDifferFromSource() throws Exception
		{
			TestableWebComponent original = createWebComponentWithColumns(sourceForm, 3);
			original.setExtendsID(UUID.randomUUID().toString());
			sourceForm.addChild(original);

			JSONObject sourceJson = original.getJson();
			JSONArray sourceColumns = sourceJson.getJSONArray("columns");
			Set<String> sourceUUIDsBefore = collectJsonColumnUUIDs(original);

			ServoyJSONObject flattenedJson = new ServoyJSONObject();
			JSONArray flattenedColumns = new JSONArray();
			for (int i = 0; i < sourceColumns.length(); i++)
			{
				// share the reference (no deep copy) - matches PersistHelper.getFlattenedJSON behaviour
				flattenedColumns.put(sourceColumns.getJSONObject(i));
			}
			flattenedJson.put("columns", flattenedColumns);
			original.setOverrideFlattenedJson(flattenedJson);

			IPersist clone = original.cloneObj(targetForm, true, null, false, false, false);
			assertInstanceOf(WebComponent.class, clone);
			WebComponent wcClone = (WebComponent)clone;

			Set<String> cloneJsonUUIDs = collectJsonColumnUUIDs(wcClone);
			Set<String> sourceUUIDsAfter = collectJsonColumnUUIDs(original);

			assertEquals(3, cloneJsonUUIDs.size(), "clone should have 3 column svyUUIDs in JSON");
			assertEquals(sourceUUIDsBefore, sourceUUIDsAfter, "source column svyUUIDs must not change after cloning");

			for (String cloneUUID : cloneJsonUUIDs)
			{
				assertFalse(sourceUUIDsAfter.contains(cloneUUID),
					"clone column svyUUID " + cloneUUID + " must not equal any SOURCE column svyUUID (duplicate!)");
			}
		}

		/**
		 * Intra-clone isolation guard: the clone's stored JSON svyUUIDs match its own child persists.
		 */
		@Test
		@DisplayName("clone child persist svyUUIDs match its own stored JSON svyUUIDs")
		void cloneChildUUIDsMatchStoredJson() throws Exception
		{
			TestableWebComponent original = createWebComponentWithColumns(sourceForm, 2);
			original.setExtendsID(UUID.randomUUID().toString());
			sourceForm.addChild(original);

			JSONArray sourceColumns = original.getJson().getJSONArray("columns");
			ServoyJSONObject flattenedJson = new ServoyJSONObject();
			JSONArray flattenedColumns = new JSONArray();
			for (int i = 0; i < sourceColumns.length(); i++)
			{
				flattenedColumns.put(sourceColumns.getJSONObject(i));
			}
			flattenedJson.put("columns", flattenedColumns);
			original.setOverrideFlattenedJson(flattenedJson);

			IPersist clone = original.cloneObj(targetForm, true, null, false, false, false);
			WebComponent wcClone = (WebComponent)clone;

			Set<UUID> childUUIDs = collectChildUUIDs(wcClone);
			Set<String> jsonUUIDs = collectJsonColumnUUIDs(wcClone);

			assertNotNull(wcClone.getJson());
			assertEquals(2, childUUIDs.size(), "clone should have 2 child persists");
			assertEquals(childUUIDs.size(), jsonUUIDs.size(), "same number of svyUUIDs in children and JSON");
			for (UUID childUUID : childUUIDs)
			{
				assertTrue(jsonUUIDs.contains(childUUID.toString()),
					"clone child svyUUID " + childUUID + " must be present in the clone's stored JSON");
			}
		}
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
		private JSONObject overrideFlattenedJson;

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

		void setOverrideFlattenedJson(JSONObject json)
		{
			this.overrideFlattenedJson = json;
		}

		@Override
		public JSONObject getFlattenedJson()
		{
			if (overrideFlattenedJson != null)
			{
				return overrideFlattenedJson;
			}
			return super.getFlattenedJson();
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
