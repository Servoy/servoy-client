package com.servoy.j2db.persistence;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.PropertyDescriptionBuilder;
import org.sablo.specification.property.CustomJSONArrayType;
import org.sablo.specification.property.CustomJSONObjectType;
import org.sablo.specification.property.types.StringPropertyType;

import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.UUID;

/**
 * Regression test for SVY-21423: {@link PersistHelper#getWebComponentProperty(AbstractBase, String)} must return
 * <code>null</code> (not an empty array) for an array-of-custom-object property with zero configured children, and
 * must still return the correct populated array when children exist.
 */
public class PersistHelperGetWebComponentPropertyTest
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

	@Test
	public void testEmptyArrayOfCustomObjectPropertyReturnsNull()
	{
		TestableWebComponent wc = createWebComponent();

		Object result = PersistHelper.getWebComponentProperty(wc, "columns");

		assertNull(result, "An array-of-custom-object property with zero configured children must return null, not an empty array");
	}

	@Test
	public void testNonEmptyArrayOfCustomObjectPropertyReturnsPopulatedArray()
	{
		TestableWebComponent wc = createWebComponent();

		WebCustomType col0 = WebCustomType.createNewInstance(wc, columnType.getCustomJSONTypeDefinition(), "columns", 0);
		WebCustomType col1 = WebCustomType.createNewInstance(wc, columnType.getCustomJSONTypeDefinition(), "columns", 1);

		Object result = PersistHelper.getWebComponentProperty(wc, "columns");

		assertNotNull(result, "An array-of-custom-object property with configured children must not return null");
		assertArrayEquals(new IChildWebObject[] { col0, col1 }, (IChildWebObject[])result);
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
