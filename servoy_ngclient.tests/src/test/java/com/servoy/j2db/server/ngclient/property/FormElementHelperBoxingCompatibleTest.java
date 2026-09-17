/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2025 Servoy BV

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

import java.awt.Dimension;
import java.awt.Point;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.sablo.InMemPackageReader;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.PropertyDescriptionBuilder;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.server.ngclient.FormElementHelper;
import com.servoy.j2db.server.ngclient.INGFormElement;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.Utils;

/**
 * SVY-21470 - regression test that pins the boxing-aware legacy guard in
 * {@link FormElementHelper#generateFormComponentPersists}: a form-component child
 * whose per-child JSON carries a per-instance override {@code visible = false} (a
 * {@code java.lang.Boolean} against the primitive-{@code boolean}
 * {@link BaseComponent#setVisible(boolean)} setter) must NOT be dropped by the
 * incompatible-legacy-property guard introduced in {@code c446933c1}; the generated
 * child clone must report {@code getVisible() == false}.
 *
 * <p>Against the regressed (non-boxing-aware) guard, {@code boolean.class} is not
 * assignable from {@code Boolean.class} and {@code Boolean} is not a {@code Number},
 * so the guard skipped the override and the clone kept its default {@code visible = true}.
 * {@link #testVisibleFalseOverrideReachesGeneratedChildClone()} pins the fix and would
 * fail if {@code isBoxingCompatible} were reverted.
 *
 * @author ai
 */
@SuppressWarnings("nls")
public class FormElementHelperBoxingCompatibleTest extends AbstractSolutionTest
{
	private static final String CONTAINED_FORM_PROPERTY = "containedForm";
	private static final String CHILD_NAME = "child1";

	private Form formComponentForm;
	private WebComponent container;

	@Override
	protected InMemPackageReader getTestComponents() throws IOException
	{
		String manifest = readResource("FormElementHelperCustomPropertiesTest.manifest");
		String spec = readResource("FormElementHelperCustomPropertiesTest-fcecontainer.spec");

		HashMap<String, String> components = new HashMap<>();
		components.put("fcecontainer.spec", spec);
		return new InMemPackageReader(manifest, components);
	}

	private String readResource(String name) throws IOException
	{
		try (InputStream is = getClass().getResourceAsStream(name))
		{
			return new String(Utils.getBytesFromInputStream(is));
		}
	}

	@Override
	protected void fillTestSolution() throws RepositoryException
	{
		// the (real) form component form with a single field child; its clone is what
		// generateFormComponentPersists produces and the visible override must reach.
		formComponentForm = solution.createNewForm(validator, null, "fcform", null, false, new Dimension(600, 400));
		formComponentForm.setFormComponent(Boolean.TRUE);
		formComponentForm.setNavigatorID(Form.NAVIGATOR_NONE);
		Field child = formComponentForm.createNewField(new Point(0, 0));
		child.setName(CHILD_NAME);

		// the main form carrying the form-component container element used as
		// formComponentContainerElement (its identity/name/form are all that the
		// method reads from it).
		Form mainForm = solution.createNewForm(validator, null, "main", null, false, new Dimension(600, 400));
		mainForm.setNavigatorID(Form.NAVIGATOR_NONE);
		container = mainForm.createNewWebComponent("container", "fce-container");
	}

	@Override
	protected void setupData() throws ServoyException
	{
	}

	private static Method getGenerateFormComponentPersistsMethod() throws NoSuchMethodException
	{
		Method method = FormElementHelper.class.getDeclaredMethod("generateFormComponentPersists", INGFormElement.class,
			PropertyDescription.class, JSONObject.class, Form.class, FlattenedSolution.class);
		method.setAccessible(true);
		return method;
	}

	@SuppressWarnings("unchecked")
	private List<IFormElement> invokeGenerateFormComponentPersists(JSONObject formElementValue) throws Exception
	{
		FlattenedSolution fs = client.getFlattenedSolution();
		INGFormElement containerElement = FormElementHelper.INSTANCE.getFormElement(container, fs, null, false);
		PropertyDescription pd = new PropertyDescriptionBuilder().withName(CONTAINED_FORM_PROPERTY).build();

		return (List<IFormElement>)getGenerateFormComponentPersistsMethod().invoke(null, containerElement, pd, formElementValue,
			formComponentForm, fs);
	}

	private static BaseComponent findChildClone(List<IFormElement> generated)
	{
		for (IFormElement element : generated)
		{
			// the template name is <containerName>$<pd>$<childName>; match on the suffix
			if (element instanceof BaseComponent && element.getName() != null && element.getName().endsWith(CHILD_NAME))
			{
				return (BaseComponent)element;
			}
		}
		return null;
	}

	/**
	 * The core SVY-21470 regression assertion: a per-instance override
	 * {@code visible = false} (a Boolean against the primitive-boolean setVisible
	 * setter) is applied to the generated child clone. Fails if the guard skips the
	 * Boolean override (the pre-fix, non-boxing-aware behaviour).
	 */
	@Test
	public void testVisibleFalseOverrideReachesGeneratedChildClone() throws Exception
	{
		JSONObject childJson = new JSONObject();
		childJson.put("visible", false);
		JSONObject formElementValue = new JSONObject();
		formElementValue.put(CHILD_NAME, childJson);

		List<IFormElement> generated = invokeGenerateFormComponentPersists(formElementValue);
		Assertions.assertEquals(1, generated.size(), "exactly one child persist must be generated for the form component");

		BaseComponent childClone = findChildClone(generated);
		Assertions.assertNotNull(childClone, "the generated child persist must be a BaseComponent named after " + CHILD_NAME);

		Assertions.assertFalse(childClone.getVisible(),
			"the visible=false Boolean override must be applied to the primitive-boolean setter, not dropped by the legacy guard");
	}

	/**
	 * A genuinely incompatible legacy value (a String for the {@code size} setter,
	 * which takes a Dimension) is still skipped without throwing, and its presence
	 * does NOT prevent the {@code visible = false} Boolean override next to it from
	 * being applied - the guard's intent from {@code c446933c1} is preserved while the
	 * boxing-aware fix still lands the primitive-boolean override.
	 */
	@Test
	public void testIncompatibleLegacyValueSkippedWhileVisibleFalseStillApplied() throws Exception
	{
		JSONObject childJson = new JSONObject();
		childJson.put("size", "not-a-dimension");
		childJson.put("visible", false);
		JSONObject formElementValue = new JSONObject();
		formElementValue.put(CHILD_NAME, childJson);

		List<IFormElement> generated = invokeGenerateFormComponentPersists(formElementValue);
		Assertions.assertEquals(1, generated.size(), "exactly one child persist must be generated even with an incompatible legacy property");

		BaseComponent childClone = findChildClone(generated);
		Assertions.assertNotNull(childClone, "the generated child persist must be a BaseComponent named after " + CHILD_NAME);

		Assertions.assertFalse(childClone.getVisible(),
			"the incompatible size String must be skipped without preventing the visible=false override from being applied");
	}

	/**
	 * Guards the default: a form-component child with no per-instance visible
	 * override keeps its default {@code visible = true}. This is the control that
	 * proves the false result in {@link #testVisibleFalseOverrideReachesGeneratedChildClone()}
	 * comes from the override and not from an unrelated default flip.
	 */
	@Test
	public void testNoVisibleOverrideKeepsDefaultTrue() throws Exception
	{
		JSONObject childJson = new JSONObject();
		JSONObject formElementValue = new JSONObject();
		formElementValue.put(CHILD_NAME, childJson);

		List<IFormElement> generated = invokeGenerateFormComponentPersists(formElementValue);
		Assertions.assertEquals(1, generated.size(), "exactly one child persist must be generated for the form component");

		BaseComponent childClone = findChildClone(generated);
		Assertions.assertNotNull(childClone, "the generated child persist must be a BaseComponent named after " + CHILD_NAME);

		Assertions.assertTrue(childClone.getVisible(), "with no visible override the generated child clone must keep its default visible=true");
	}
}
