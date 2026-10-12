package com.servoy.j2db.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.PropertyDescriptionBuilder;
import org.sablo.specification.property.CustomJSONArrayType;
import org.sablo.specification.property.CustomJSONObjectType;
import org.sablo.specification.property.types.StringPropertyType;

import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.UUID;

/**
 * SVY-21574: a damaged .frm produced a custom-type array element whose index was corrupt (hundreds of
 * millions). WebCustomType's constructor fed that index into org.json.JSONArray.put(index, value), which
 * pads the backing ArrayList with nulls up to 'index' - allocating ~2GB and throwing OutOfMemoryError just
 * while loading the solution (seen in the globis_logistics heap dump, columns array size ~1.8 billion).
 *
 * These tests pin the fix in both places:
 * - WebCustomType.<init> never pads for an index beyond the current array length: it appends instead (so
 *   the element is still added, in order, right after the last valid one) and logs a Debug.error.
 * - WebComponent.initCustomTypes creates array children in ascending order, so an index that is only
 *   temporarily out of order (element claiming index 5 stored before the one claiming index 1) still lands
 *   in its correct slot in a later pass rather than being padded/appended wrongly.
 */
public class WebCustomTypeOutOfRangeIndexTest
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

	private static JSONArray columns(WebComponent wc)
	{
		JSONObject json = (JSONObject)wc.getOwnProperty(StaticContentSpecLoader.PROPERTY_JSON.getPropertyName());
		return json.optJSONArray("columns");
	}

	/**
	 * The reported crash: a single element whose index is absurdly large. Before the fix this padded the
	 * backing list with ~500 million nulls and OOMed; now it must append as the single element, quickly.
	 */
	@Test
	public void hugeIndexAppendsInsteadOfPaddingAndDoesNotOOM()
	{
		TestableWebComponent wc = createWebComponent();

		assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
			WebCustomType col = WebCustomType.createNewInstance(wc, columnType.getCustomJSONTypeDefinition(), "columns",
				500_000_000);
			assertNotNull(col);

			JSONArray columnsArray = columns(wc);
			assertNotNull(columnsArray);
			// one element, appended at index 0 - NOT an array padded to 500 million
			assertEquals(1, columnsArray.length());
			assertEquals(col.getUUID().toString(), columnsArray.getJSONObject(0).getString(IChildWebObject.UUID_KEY));
		});
	}

	/**
	 * A huge index arriving between normal elements must not pad and must not lose the earlier elements; it
	 * is appended after them.
	 */
	@Test
	public void hugeIndexBetweenValidElementsAppendsAndKeepsOrder()
	{
		TestableWebComponent wc = createWebComponent();

		assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
			WebCustomType a = WebCustomType.createNewInstance(wc, columnType.getCustomJSONTypeDefinition(), "columns", 0);
			WebCustomType b = WebCustomType.createNewInstance(wc, columnType.getCustomJSONTypeDefinition(), "columns", 1);
			WebCustomType huge = WebCustomType.createNewInstance(wc, columnType.getCustomJSONTypeDefinition(), "columns",
				1_000_000);

			JSONArray columnsArray = columns(wc);
			assertEquals(3, columnsArray.length());
			assertEquals(a.getUUID().toString(), columnsArray.getJSONObject(0).getString(IChildWebObject.UUID_KEY));
			assertEquals(b.getUUID().toString(), columnsArray.getJSONObject(1).getString(IChildWebObject.UUID_KEY));
			assertEquals(huge.getUUID().toString(), columnsArray.getJSONObject(2).getString(IChildWebObject.UUID_KEY));
		});
	}

	/**
	 * Ascending-order loading (as WebComponent.initCustomTypes now does) with a mix of a gap and a huge
	 * index. Loading 0,1,2,...,6 then a 1_000_000 element: everything in 0..6 lands in its own slot and the
	 * huge one is appended last. This is the scenario from the review discussion.
	 */
	@Test
	public void ascendingLoadWithGapAndHugeIndexEndsInOrder()
	{
		TestableWebComponent wc = createWebComponent();

		assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
			WebCustomType[] created = new WebCustomType[7];
			for (int i = 0; i <= 6; i++)
			{
				created[i] = WebCustomType.createNewInstance(wc, columnType.getCustomJSONTypeDefinition(), "columns", i);
			}
			WebCustomType huge = WebCustomType.createNewInstance(wc, columnType.getCustomJSONTypeDefinition(), "columns",
				1_000_000);

			JSONArray columnsArray = columns(wc);
			assertEquals(8, columnsArray.length());
			for (int i = 0; i <= 6; i++)
			{
				assertEquals(created[i].getUUID().toString(),
					columnsArray.getJSONObject(i).getString(IChildWebObject.UUID_KEY));
			}
			assertEquals(huge.getUUID().toString(), columnsArray.getJSONObject(7).getString(IChildWebObject.UUID_KEY));
		});
	}

	/**
	 * End-to-end: a damaged columns JSON array where the stored element carries a corrupt index is read back
	 * through WebComponent.initCustomTypes (via getAllObjectsAsList, which triggers initCustomTypes). The
	 * loader must not OOM and must reconstruct exactly the non-null elements as children. This mirrors the
	 * real "loading servers/tables needed by current solution" path from the heap dump.
	 */
	@Test
	public void initCustomTypesFromStoredJsonDoesNotOOM()
	{
		TestableWebComponent wc = createWebComponent();

		// build a columns array with 3 regular elements as it would be stored in the .frm
		JSONObject wcJson = (JSONObject)wc.getOwnProperty(StaticContentSpecLoader.PROPERTY_JSON.getPropertyName());
		JSONArray columnsArray = new JSONArray();
		columnsArray.put(new JSONObject().put("text", "col0"));
		columnsArray.put(new JSONObject().put("text", "col1"));
		columnsArray.put(new JSONObject().put("text", "col2"));
		wcJson.put("columns", columnsArray);

		// force a fresh reconstruction from JSON (as happens on solution load / developer restart)
		wc.setCustomTypesInitialized(false);

		assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
			assertEquals(3, wc.getAllObjectsAsList().size());
			JSONArray reconstructed = columns(wc);
			assertEquals(3, reconstructed.length());
		});
	}

	/**
	 * Normal append (index == current length) and in-order inserts keep working unchanged - the guard must
	 * only affect indexes strictly beyond the current length.
	 */
	@Test
	public void normalAppendAndInsertUnchanged()
	{
		TestableWebComponent wc = createWebComponent();

		WebCustomType c0 = WebCustomType.createNewInstance(wc, columnType.getCustomJSONTypeDefinition(), "columns", 0);
		WebCustomType c1 = WebCustomType.createNewInstance(wc, columnType.getCustomJSONTypeDefinition(), "columns", 1);
		// insert at an existing index -> shifts the rest up
		WebCustomType cMid = WebCustomType.createNewInstance(wc, columnType.getCustomJSONTypeDefinition(), "columns", 1);

		JSONArray columnsArray = columns(wc);
		assertEquals(3, columnsArray.length());
		assertEquals(c0.getUUID().toString(), columnsArray.getJSONObject(0).getString(IChildWebObject.UUID_KEY));
		assertEquals(cMid.getUUID().toString(), columnsArray.getJSONObject(1).getString(IChildWebObject.UUID_KEY));
		assertEquals(c1.getUUID().toString(), columnsArray.getJSONObject(2).getString(IChildWebObject.UUID_KEY));
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
