/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2026 Servoy BV

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

package com.servoy.j2db.server.ngclient.property.types;

import static org.junit.jupiter.api.Assertions.assertNull;

import java.awt.Dimension;
import java.io.IOException;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.sablo.InMemPackageReader;
import org.sablo.specification.PropertyDescription;

import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.scripting.FormScope;
import com.servoy.j2db.server.ngclient.IWebFormController;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.property.AbstractSolutionTest;
import com.servoy.j2db.util.ServoyException;

/**
 * SVY-21667 - 3.3a "Single log per rejected value": closes the gap left by
 * {@link FormComponentPropertyTypeAsFormComponentValueTest} (which only pins
 * {@link FormComponentPropertyType#asFormComponentValue(Object, String, String, String)} itself) by exercising the real
 * {@code toSabloComponentValue} / {@code toRhinoValue} interaction on a genuine {@code servoycore-formcomponent}
 * {@link WebFormComponent} built from a malformed <code>.frm</code>-style raw value.
 * <p>
 * The scenario: a (List) Form Component's <code>containedForm</code> property holds a bare form-name string (not the
 * canonical <code>{ "svy_form": "&lt;uuid&gt;" }</code> object) that nonetheless resolves to a real form via
 * {@link FormComponentPropertyType#getForm(Object, com.servoy.j2db.FlattenedSolution)}. Two things must hold:
 * <ol>
 * <li>{@code toSabloComponentValue} must reject it with {@code null} (its own "no value" convention), not with the raw
 * still-bad string - otherwise every later Rhino read would re-run the guard and re-log the same already-rejected value.</li>
 * <li>{@code toRhinoValue}, given that {@code null} sablo value, must short-circuit on its {@code webComponentValue == null}
 * branch and return {@code null} directly, without re-invoking {@code getForm}/{@code asFormComponentValue}.</li>
 * </ol>
 * As in the sibling test, the single {@code Debug.error} log line itself is not asserted (no mocking framework is wired
 * into {@code servoy_ngclient.tests} - confirmed by reading its MANIFEST.MF, which lists no Mockito/equivalent bundle);
 * the observable return-value behaviour is what pins the acceptance criteria here.
 */
class FormComponentPropertyTypeSabloRhinoGuardTest extends AbstractSolutionTest
{
	private static final String MAIN_FORM_NAME = "fc_owner_form";
	private static final String TARGET_FORM_NAME = "fc_target_form";
	private static final String FC_ELEMENT_NAME = "fc1";

	@Override
	protected void setupData() throws ServoyException
	{
	}

	@Override
	protected void fillTestSolution() throws ServoyException
	{
		Form targetForm = solution.createNewForm(validator, null, TARGET_FORM_NAME, null, false, new Dimension(600, 400));
		targetForm.setNavigatorID(Form.NAVIGATOR_NONE);

		Form mainForm = solution.createNewForm(validator, null, MAIN_FORM_NAME, null, false, new Dimension(600, 400));
		mainForm.setNavigatorID(Form.NAVIGATOR_NONE);

		WebComponent fc = mainForm.createNewWebComponent(FC_ELEMENT_NAME, "servoycore-formcomponent");
		// SVY-21667: a bare form-name string instead of the canonical { "svy_form": "<uuid>" } shape; it resolves via
		// getForm(...) (lenient String/UUID lookup) but must be rejected by the asFormComponentValue(...) guard.
		fc.setProperty("containedForm", TARGET_FORM_NAME);
	}

	@Override
	protected InMemPackageReader getTestComponents() throws IOException
	{
		// null: rely on the real servoycore/ package (loaded by AbstractSolutionTest.buildSolution), which already
		// contains "servoycore-formcomponent" with its real "containedForm" formcomponent-typed property.
		return null;
	}

	@Nested
	class ToSabloComponentValueRejection
	{
		/**
		 * Acceptance criterion (1): building the real component from the malformed persisted value must store a
		 * sablo value of {@code null} for "containedForm" - not the raw bad string - so that every later consumer
		 * ({@code toRhinoValue}, {@code FoundsetLinkedPropertyType}, {@code ComponentFactory.fillProperty}, ...) sees
		 * this type's own "no value" convention instead of re-discovering and re-rejecting the same bad value.
		 */
		@Test
		void rejectedFormComponentValueIsStoredAsNullNotAsTheRawBadString() throws Exception
		{
			IWebFormController form = (IWebFormController)client.getFormManager().showFormInCurrentContainer(MAIN_FORM_NAME);
			WebFormComponent webComponent = form.getFormUI().getWebComponent(FC_ELEMENT_NAME);

			Object sabloValue = webComponent.getProperty("containedForm");

			assertNull(sabloValue,
				"toSabloComponentValue must reject the bare form-name string with null, not pass the raw value through");
		}
	}

	@Nested
	class ToRhinoValueShortCircuit
	{
		/**
		 * Acceptance criterion (2): a direct call to {@code toRhinoValue} with a {@code null} webComponentValue (the
		 * exact value {@code toSabloComponentValue} produces for the rejected property, per the test above) must
		 * short-circuit to {@code null} on its dedicated {@code webComponentValue == null} branch - the one added by
		 * SVY-21667 - without falling into the {@code else} branch that re-runs {@code getForm}/
		 * {@code asFormComponentValue}.
		 */
		@Test
		void nullWebComponentValueShortCircuitsToNullWithoutReRunningTheGuard() throws Exception
		{
			IWebFormController form = (IWebFormController)client.getFormManager().showFormInCurrentContainer(MAIN_FORM_NAME);
			WebFormComponent webComponent = form.getFormUI().getWebComponent(FC_ELEMENT_NAME);
			PropertyDescription pd = webComponent.getSpecification().getProperty("containedForm");

			Context cx = Context.enter();
			try
			{
				Object rhinoValue = FormComponentPropertyType.INSTANCE.toRhinoValue(webComponent.getProperty("containedForm"), pd, webComponent,
					cx.initStandardObjects());

				assertNull(rhinoValue, "toRhinoValue must return null directly for a null (already-rejected) webComponentValue");
			}
			finally
			{
				Context.exit();
			}
		}

		/**
		 * End-to-end proof that the short-circuit is reached through the real scripting read path (Rhino property
		 * get on {@code elements.fc1.containedForm}, via {@code RuntimeWebComponent.get} ->
		 * {@code NGConversions.convertSabloComponentToRhinoValue} -> {@code toRhinoValue}), not just when called
		 * directly: reading the rejected property from script must not throw and must surface as {@code null}.
		 */
		@Test
		void scriptingReadOfRejectedPropertyReturnsNullThroughTheRealRhinoPath() throws Exception
		{
			IWebFormController form = (IWebFormController)client.getFormManager().showFormInCurrentContainer(MAIN_FORM_NAME);
			FormScope formScope = form.getFormScope();

			Context cx = Context.enter();
			try
			{
				Object scriptedValue = cx.evaluateString(formScope, "elements." + FC_ELEMENT_NAME + ".containedForm", "Evaluation Test Script", 1, null);

				assertNull(scriptedValue, "reading the rejected property from script must not throw and must surface as null, not NOT_FOUND or a bad value");
			}
			finally
			{
				Context.exit();
			}
		}
	}
}
