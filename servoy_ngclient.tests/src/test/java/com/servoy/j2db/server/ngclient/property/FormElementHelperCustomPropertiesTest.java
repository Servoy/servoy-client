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
import java.util.Map;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
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
 * SVY-21469 §5.3 - regression test that pins the actual 26.9 fix in
 * {@link FormElementHelper#generateFormComponentPersists}: a form-component
 * child whose per-child JSON carries a legacy loose-string {@code customProperties}
 * ({@code "attributes:{ data-Target:\"dashboard-health\" }"}) must NOT be skipped
 * by the incompatible-legacy-property guard; instead it must be merged via
 * {@code setCustomProperties} so that the generated child persist's
 * {@link BaseComponent#getAttributes()} delivers {@code data-Target}.
 *
 * <p>Against the regressed guard (which skipped {@code customProperties} because a
 * String is not assignable to the {@code setCustomProperties(JSONObject)} setter
 * parameter) the merge branch is never reached, {@code getAttributes()} stays empty,
 * and {@link #testLegacyStringCustomPropertiesReachesGeneratedChildAttributes()}
 * fails.
 *
 * @author ai
 */
@SuppressWarnings("nls")
public class FormElementHelperCustomPropertiesTest extends AbstractSolutionTest
{
	private static final String LEGACY_CUSTOM_PROPERTIES = "attributes:{ data-Target:\"dashboard-health\" }";
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
		// generateFormComponentPersists produces and the customProperties must reach.
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
	 * The core regression assertion: a legacy String customProperties on the FC
	 * child's per-child JSON ends up on the generated child persist's attributes.
	 * Fails if generateFormComponentPersists skips String customProperties.
	 */
	@Test
	public void testLegacyStringCustomPropertiesReachesGeneratedChildAttributes() throws Exception
	{
		JSONObject childJson = new JSONObject();
		childJson.put("customProperties", LEGACY_CUSTOM_PROPERTIES);
		JSONObject formElementValue = new JSONObject();
		formElementValue.put(CHILD_NAME, childJson);

		List<IFormElement> generated = invokeGenerateFormComponentPersists(formElementValue);
		Assert.assertEquals("exactly one child persist must be generated for the form component", 1, generated.size());

		BaseComponent childClone = findChildClone(generated);
		Assert.assertNotNull("the generated child persist must be a BaseComponent named after " + CHILD_NAME, childClone);

		Map<String, String> attributes = childClone.getAttributes();
		Assert.assertFalse("legacy string customProperties must have been merged into the child's attributes (not skipped)",
			attributes.isEmpty());
		Assert.assertEquals("the merged attributes must expose data-Target", "dashboard-health", attributes.get("data-Target"));
	}

	/**
	 * The guard's intended behaviour is preserved: a genuinely incompatible legacy
	 * property (a String value for the {@code size} setter, which takes a
	 * Dimension) is still skipped without throwing, while the legacy customProperties
	 * next to it is still merged and delivered.
	 */
	@Test
	public void testIncompatibleLegacyPropertyIsSkippedWhileCustomPropertiesStillMerged() throws Exception
	{
		JSONObject childJson = new JSONObject();
		childJson.put("size", "not-a-dimension");
		childJson.put("customProperties", LEGACY_CUSTOM_PROPERTIES);
		JSONObject formElementValue = new JSONObject();
		formElementValue.put(CHILD_NAME, childJson);

		List<IFormElement> generated = invokeGenerateFormComponentPersists(formElementValue);
		Assert.assertEquals("exactly one child persist must be generated even with an incompatible legacy property", 1,
			generated.size());

		BaseComponent childClone = findChildClone(generated);
		Assert.assertNotNull("the generated child persist must be a BaseComponent named after " + CHILD_NAME, childClone);
		Assert.assertEquals("the incompatible size String must not corrupt the customProperties merge", "dashboard-health",
			childClone.getAttributes().get("data-Target"));
	}
}
