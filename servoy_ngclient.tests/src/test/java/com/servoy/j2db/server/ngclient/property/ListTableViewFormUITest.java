/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2024 Servoy BV

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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.sablo.InMemPackageReader;
import org.sablo.WebComponent;

import com.servoy.base.solutionmodel.IBaseSMPart;
import com.servoy.j2db.FormController;
import com.servoy.j2db.IForm;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.FormElementHelper;
import com.servoy.j2db.server.ngclient.IWebFormController;
import com.servoy.j2db.server.ngclient.ServoyDataConverterContext;
import com.servoy.j2db.server.ngclient.WebFormUI;
import com.servoy.j2db.util.ServoyException;

/**
 * SVY-21548 regression: the dead `servoycore-portal` / `BodyPortal` / `WebListFormUI` server-side generation path
 * was removed, so list-view and table-view forms now initialise through a plain {@link WebFormUI} (via
 * {@link IWebFormController#getFormUI()} / `WebFormController.initFormUI()`) instead of the deleted `WebListFormUI`.
 * <p>
 * The removed path ultimately produced the `ConcurrentHashMap.putIfAbsent(formElement, null)` NPE because
 * {@code FormElementHelper.createBodyPortalFormElement()} returned {@code null} for the unregistered
 * {@code servoycore-portal} spec. These tests assert that a list-view and a table-view form:
 * <ul>
 * <li>initialise and render through a plain {@link WebFormUI} (not {@code WebListFormUI}) without any NPE,</li>
 * <li>expose their body components (a record-view-style render), and</li>
 * <li>never cause {@link FormElementHelper#getFormElement} to return {@code null} for their body persists -
 * the invariant the reverted null-guard now relies on.</li>
 * </ul>
 *
 * @author Servoy
 */
@SuppressWarnings("nls")
public class ListTableViewFormUITest extends AbstractSolutionTest
{
	private static final String LIST_FORM = "listViewForm";
	private static final String TABLE_FORM = "tableViewForm";
	private static final String RECORD_FORM = "recordViewForm";

	private static final String LIST_FIELD = "listField";
	private static final String TABLE_FIELD = "tableField";
	private static final String RECORD_FIELD = "recordField";

	@Override
	protected InMemPackageReader getTestComponents() throws IOException
	{
		return null;
	}

	@Override
	protected void setupData() throws ServoyException
	{
	}

	@Override
	protected void fillTestSolution() throws ServoyException
	{
		// a plain dummy first form so the solution does not auto-initialise one of the test forms too early
		Form firstForm = solution.createNewForm(validator, null, "dummyFirstForm", null, false, new Dimension(600, 400));
		firstForm.setNavigatorID(Form.NAVIGATOR_NONE);
		firstForm.createNewPart(IBaseSMPart.BODY, 5);
		solution.setFirstFormID(firstForm.getUUID().toString());

		createBodyPartFormWithField(LIST_FORM, LIST_FIELD, IForm.LIST_VIEW);
		createBodyPartFormWithField(TABLE_FORM, TABLE_FIELD, FormController.TABLE_VIEW);
		createBodyPartFormWithField(RECORD_FORM, RECORD_FIELD, IForm.RECORD_VIEW);
	}

	private void createBodyPartFormWithField(String formName, String fieldName, int view) throws ServoyException
	{
		Form form = solution.createNewForm(validator, null, formName, null, false, new Dimension(600, 400));
		form.setNavigatorID(Form.NAVIGATOR_NONE);
		form.setView(view);
		// a BODY part is what used to route the form to WebListFormUI + BodyPortal for list/table view
		form.createNewPart(IBaseSMPart.BODY, 5);
		Field field = form.createNewField(new Point(5, 5));
		field.setDisplayType(Field.TEXT_FIELD);
		field.setName(fieldName);
	}

	@Test
	public void listViewFormInitialisesThroughPlainWebFormUIWithoutNPE()
	{
		assertFormRendersThroughWebFormUI(LIST_FORM, LIST_FIELD, IForm.LIST_VIEW);
	}

	@Test
	public void tableViewFormInitialisesThroughPlainWebFormUIWithoutNPE()
	{
		assertFormRendersThroughWebFormUI(TABLE_FORM, TABLE_FIELD, FormController.TABLE_VIEW);
	}

	@Test
	public void recordViewFormStillInitialisesThroughPlainWebFormUI()
	{
		// regression guard: the record-view case was always a plain WebFormUI; it must stay that way
		assertFormRendersThroughWebFormUI(RECORD_FORM, RECORD_FIELD, IForm.RECORD_VIEW);
	}

	/**
	 * Shows the form through the form manager - which runs `WebFormController.initFormUI()` and
	 * {@link WebFormUI#init()} - and asserts it rendered through a plain {@link WebFormUI} with its body
	 * field present and no NPE from the removed BodyPortal null path.
	 */
	private void assertFormRendersThroughWebFormUI(String formName, String fieldName, int expectedView)
	{
		IWebFormController controller = (IWebFormController)client.getFormManager().showFormInCurrentContainer(formName);
		Assertions.assertNotNull(controller, "form controller for " + formName + " should be created");

		// the view type is preserved on the form (so this really is a list/table/record-view form)...
		Assertions.assertEquals(expectedView, controller.getForm().getView(), "form view type for " + formName);

		// ...but regardless of view type it must render through a PLAIN WebFormUI (WebListFormUI is deleted)
		Assertions.assertTrue(controller.getFormUI() instanceof WebFormUI,
			"list/table/record-view form must render through a plain WebFormUI; got: " +
				(controller.getFormUI() == null ? "null" : controller.getFormUI().getClass().getName()));

		// the body field must be contributed to the form UI (record-view-style render of the body components)
		Collection<WebComponent> components = controller.getFormUI().getComponents();
		boolean found = false;
		for (WebComponent wc : components)
		{
			if (fieldName.equals(wc.getName()))
			{
				found = true;
				break;
			}
		}
		Assertions.assertTrue(found,
			"body field '" + fieldName + "' should be rendered by the WebFormUI for " + formName + "; components: " + components);
	}

	@Test
	public void getFormElementNeverReturnsNullForListViewBodyPersist() throws RepositoryException
	{
		assertGetFormElementNonNullForBodyPersists(LIST_FORM);
	}

	@Test
	public void getFormElementNeverReturnsNullForTableViewBodyPersist() throws RepositoryException
	{
		assertGetFormElementNonNullForBodyPersists(TABLE_FORM);
	}

	/**
	 * Directly exercises the invariant the reverted SVY-21548 null-guard now relies on:
	 * {@link FormElementHelper#getFormElement} (and {@link FormElementHelper#getFormElements}) never return
	 * {@code null} for a normal body persist. Before the fix a list/table-view form appended a synthetic
	 * `BodyPortal` whose `createBodyPortalFormElement` returned {@code null}; with that path gone, every
	 * element produced here must be non-null.
	 */
	private void assertGetFormElementNonNullForBodyPersists(String formName) throws RepositoryException
	{
		Form form = solution.getForm(formName);
		Assertions.assertNotNull(form, "form " + formName + " should exist");

		ServoyDataConverterContext context = new ServoyDataConverterContext(client);

		List<IPersist> persists = new ArrayList<>();
		form.getAllObjects().forEachRemaining(persists::add);

		List<FormElement> formElements = FormElementHelper.INSTANCE.getFormElements(persists.iterator(), context);
		Assertions.assertFalse(formElements.isEmpty(), "form " + formName + " should produce at least one form element");
		for (FormElement fe : formElements)
		{
			Assertions.assertNotNull(fe, "getFormElements must not contain a null element for " + formName);
		}
	}
}
