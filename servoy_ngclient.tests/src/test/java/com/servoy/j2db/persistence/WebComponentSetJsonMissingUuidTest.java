package com.servoy.j2db.persistence;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
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
 * SVY-21677: Servoy Developer hard-hangs (one ForkJoinPool worker pegging a core indefinitely while holding
 * the Eclipse workspace resource lock) when a web component's persist-mapped custom-object array is re-read
 * on the hot setJson path with array elements that have no svyUUID.
 *
 * Root cause: WebComponent.setJson's customTypesInitialized (else if arg != null) array branch looked up the
 * child by a null UUID (getChild(null) == null) and called WebCustomType.createNewInstance(..., i, null). A
 * null uuid made the constructor mint a fresh random UUID, never match an existing element, and take the
 * shift-and-insert path - growing the live array by one element every pass. Because the loop re-read
 * arr.length() each iteration, i never reached the end: an unbounded loop that pegged a core and grew the
 * heap.
 *
 * The fix mirrors the cold initCustomTypes() back-fill on the hot path: compute childUUID once, and when it
 * is null mint UUID.randomUUID(), write it back into the element's JSON, use it for the getChild lookup and
 * pass it to createNewInstance. The array length is also snapshotted before the loop (defence-in-depth). With
 * a stable uuid the array self-heals instead of growing, the deserialize terminates, and a later reload
 * reuses the same children instead of duplicating them.
 *
 * These tests drive the hot setJson path (customTypesInitialized == true) with UUID-less custom-object array
 * (and object) elements, under a preemptive timeout so the pre-fix infinite loop fails fast rather than
 * hanging the suite.
 */
public class WebComponentSetJsonMissingUuidTest
{
	private DummySolution solution;
	private Form form;
	private CustomJSONObjectType<Object, Object> columnType;
	private PropertyDescription componentPd;

	@BeforeEach
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

		CustomJSONObjectType<Object, Object> tabObjectType = new CustomJSONObjectType<>("test.tab", null);
		tabObjectType.setCustomJSONDefinition(new PropertyDescriptionBuilder()
			.withName("test.tab")
			.withType(StringPropertyType.INSTANCE)
			.build());
		PropertyDescription tabObjectPd = new PropertyDescriptionBuilder()
			.withName("tab")
			.withType(tabObjectType)
			.build();

		componentPd = new PropertyDescriptionBuilder()
			.withName("testComponent")
			.withType(StringPropertyType.INSTANCE)
			.withProperty("columns", columnArrayPd)
			.withProperty("tab", tabObjectPd)
			.build();
	}

	private TestableWebComponent createWebComponent()
	{
		TestableWebComponent wc = new TestableWebComponent(form, UUID.randomUUID(), componentPd);
		wc.setJson(new ServoyJSONObject());
		wc.setTypeName("test.component");
		return wc;
	}

	/**
	 * Build a ServoyJSONObject holding a 'columns' array of 'count' plain objects, none of which carry a
	 * svyUUID - exactly the agent-written .frm shape that triggers the bug.
	 */
	private static ServoyJSONObject jsonWithUuidlessColumns(int count)
	{
		JSONArray columnsArray = new JSONArray();
		for (int i = 0; i < count; i++)
		{
			columnsArray.put(new JSONObject().put("text", "col" + i));
		}
		ServoyJSONObject json = new ServoyJSONObject();
		json.put("columns", columnsArray);
		return json;
	}

	private static JSONArray storedColumns(WebComponent wc)
	{
		JSONObject json = (JSONObject)wc.getOwnProperty(StaticContentSpecLoader.PROPERTY_JSON.getPropertyName());
		return json.optJSONArray("columns");
	}

	@Nested
	class ArrayBranch
	{
		/**
		 * The reported hang: an array whose elements have no svyUUID, re-read on the hot setJson path with
		 * customTypesInitialized == true. Before the fix this grew the array by one element per loop pass and
		 * spun forever (the loop re-read arr.length() each iteration). It must now terminate, leave the array
		 * length unchanged (no growth), stamp every element with a svyUUID, and create exactly one child per
		 * element.
		 */
		@Test
		@DisplayName("deserialize of UUID-less array terminates with no growth, stable ids, one child each")
		void deserializeTerminatesNoGrowth()
		{
			TestableWebComponent wc = createWebComponent();
			wc.setCustomTypesInitialized(true);

			ServoyJSONObject json = jsonWithUuidlessColumns(4);

			assertTimeoutPreemptively(Duration.ofSeconds(10), () -> wc.setJson(json));

			JSONArray stored = storedColumns(wc);
			assertNotNull(stored, "columns array must still be present");

			Set<String> uuids = new HashSet<>();
			for (int i = 0; i < stored.length(); i++)
			{
				String uuid = stored.getJSONObject(i).optString(IChildWebObject.UUID_KEY, null);
				assertNotNull(uuid, "element " + i + " must carry a stable svyUUID");
				uuids.add(uuid);
			}

			List<IPersist> children = wc.getAllObjectsAsList();
			assertAll(
				() -> assertEquals(4, stored.length(), "array length must be unchanged (no growth)"),
				() -> assertEquals(4, uuids.size(), "every element must have a distinct svyUUID"),
				() -> assertEquals(4, children.size(), "exactly one child per array element (no duplicates)"));
		}

		/**
		 * The second deserialize of the same JSON (now carrying the minted UUIDs) must reuse the existing
		 * children: same count, same UUIDs - getChild resolves instead of allocating new WebCustomTypes.
		 */
		@Test
		@DisplayName("second deserialize reuses children: same count, same UUIDs")
		void secondDeserializeReusesChildren()
		{
			TestableWebComponent wc = createWebComponent();
			wc.setCustomTypesInitialized(true);

			ServoyJSONObject json = jsonWithUuidlessColumns(3);

			assertTimeoutPreemptively(Duration.ofSeconds(10), () -> wc.setJson(json));

			JSONArray afterFirst = storedColumns(wc);
			Set<String> firstUuids = new HashSet<>();
			for (int i = 0; i < afterFirst.length(); i++)
			{
				firstUuids.add(afterFirst.getJSONObject(i).getString(IChildWebObject.UUID_KEY));
			}
			int firstChildCount = wc.getAllObjectsAsList().size();

			// feed the same (now UUID-stamped) json back in
			assertTimeoutPreemptively(Duration.ofSeconds(10), () -> wc.setJson(json));

			JSONArray afterSecond = storedColumns(wc);
			Set<String> secondUuids = new HashSet<>();
			for (int i = 0; i < afterSecond.length(); i++)
			{
				secondUuids.add(afterSecond.getJSONObject(i).getString(IChildWebObject.UUID_KEY));
			}

			assertAll(
				() -> assertEquals(3, afterSecond.length(), "length still unchanged on reload"),
				() -> assertEquals(firstUuids, secondUuids, "UUIDs reused, not regenerated"),
				() -> assertEquals(firstChildCount, wc.getAllObjectsAsList().size(),
					"child count unchanged - children reused, not duplicated"));
		}

		/**
		 * Elements that already carry a svyUUID keep it (the back-fill only fires for null uuids), and no
		 * growth occurs either.
		 */
		@Test
		@DisplayName("existing svyUUIDs are preserved and the array does not grow")
		void existingUuidsPreserved()
		{
			TestableWebComponent wc = createWebComponent();
			wc.setCustomTypesInitialized(true);

			String keptUuid = UUID.randomUUID().toString();
			JSONArray columnsArray = new JSONArray();
			columnsArray.put(new JSONObject().put("text", "kept").put(IChildWebObject.UUID_KEY, keptUuid));
			columnsArray.put(new JSONObject().put("text", "missing"));
			ServoyJSONObject json = new ServoyJSONObject();
			json.put("columns", columnsArray);

			assertTimeoutPreemptively(Duration.ofSeconds(10), () -> wc.setJson(json));

			JSONArray stored = storedColumns(wc);
			assertAll(
				() -> assertEquals(2, stored.length(), "no growth"),
				() -> assertEquals(keptUuid, stored.getJSONObject(0).getString(IChildWebObject.UUID_KEY),
					"pre-existing uuid preserved"),
				() -> assertNotNull(stored.getJSONObject(1).optString(IChildWebObject.UUID_KEY, null),
					"missing uuid back-filled"),
				() -> assertNotEquals(keptUuid, stored.getJSONObject(1).getString(IChildWebObject.UUID_KEY),
					"back-filled uuid is distinct"));
		}
	}

	@Nested
	class ObjectBranch
	{
		/**
		 * The symmetric single-object branch: a custom-object property with no svyUUID gets one minted and
		 * written back on the hot setJson path. The object branch cannot grow, but the back-fill gives it a
		 * stable id just like the array branch.
		 */
		@Test
		@DisplayName("single custom-object property without svyUUID gets a stable one")
		void objectPropertyGetsBackfilledUuid()
		{
			TestableWebComponent wc = createWebComponent();
			wc.setCustomTypesInitialized(true);

			ServoyJSONObject json = new ServoyJSONObject();
			json.put("tab", new JSONObject().put("text", "firstTab"));

			assertTimeoutPreemptively(Duration.ofSeconds(10), () -> wc.setJson(json));

			JSONObject stored = (JSONObject)wc.getOwnProperty(StaticContentSpecLoader.PROPERTY_JSON.getPropertyName());
			JSONObject tab = stored.optJSONObject("tab");
			assertNotNull(tab, "tab object must still be present");
			String uuid = tab.optString(IChildWebObject.UUID_KEY, null);

			assertAll(
				() -> assertNotNull(uuid, "object branch must back-fill a svyUUID"),
				() -> assertEquals(1, wc.getAllObjectsAsList().size(), "exactly one child for the object"));
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
