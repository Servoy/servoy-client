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

package com.servoy.j2db.server.ngclient.property;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;
import java.awt.Point;
import java.io.IOException;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sablo.InMemPackageReader;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebObjectSpecification.PushToServerEnum;
import org.sablo.specification.property.BrowserConverterContext;
import org.sablo.websocket.TypedData;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.server.ngclient.IWebFormController;
import com.servoy.j2db.server.ngclient.IWebFormUI;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.property.types.ReadonlyPropertyType;
import com.servoy.j2db.server.ngclient.property.types.ReadonlySabloValue;
import com.servoy.j2db.util.ServoyException;

@SuppressWarnings("nls")
class ReadonlySabloValueTest extends AbstractSolutionTest
{
	private static final String FORM_NAME = "readonlyForm";
	private static final String CHECK_NAME = "chk";
	private static final String TEXTAREA_NAME = "txt";
	private static final String READONLY = "readOnly";
	private static final String ENABLED = "enabled";
	private static final String EDITABLE = "editable";

	@Override
	protected InMemPackageReader getTestComponents() throws IOException
	{
		return null;
	}

	@Override
	protected void fillTestSolution() throws ServoyException
	{
		Form form = solution.createNewForm(validator, null, FORM_NAME, null, false, new Dimension(600, 400));
		form.setNavigatorID(Form.NAVIGATOR_NONE);

		Field check = form.createNewField(new Point(0, 0));
		check.setName(CHECK_NAME);
		check.setDisplayType(Field.CHECKS);

		Field textArea = form.createNewField(new Point(0, 50));
		textArea.setName(TEXTAREA_NAME);
		textArea.setDisplayType(Field.TEXT_AREA);
	}

	@Override
	protected void setupData() throws ServoyException
	{
	}

	private IWebFormUI showForm()
	{
		IWebFormController controller = (IWebFormController)client.getFormManager().showFormInCurrentContainer(FORM_NAME);
		return controller.getFormUI();
	}

	private static void putReadOnlyInProperties(WebFormComponent wc, boolean readOnly)
	{
		PropertyDescription pd = wc.getFormElement().getWebComponentSpec().getProperty(READONLY);
		wc.setProperty(READONLY,
			ReadonlyPropertyType.INSTANCE.toSabloComponentValue(Boolean.valueOf(readOnly), (ReadonlySabloValue)wc.getProperty(READONLY), pd, wc));
	}

	private static JSONObject changesAsJSON(WebFormComponent wc) throws JSONException
	{
		TypedData<Map<String, Object>> changes = wc.getAndClearChanges();
		String json = JSONUtils.writeChanges(changes.content, changes.contentType, new BrowserConverterContext(wc, PushToServerEnum.allow));
		return new JSONObject(json);
	}

	@Test
	@DisplayName("re-enabling a checkbox whose readOnly was sent re-sends readOnly=false (SVY-21783)")
	void reEnablingCheckResendsReadOnlyFalse() throws JSONException
	{
		WebFormComponent wc = showForm().getWebComponent(CHECK_NAME);
		putReadOnlyInProperties(wc, false);
		wc.setProperty(ENABLED, Boolean.FALSE);
		wc.getAndClearChanges();

		wc.setProperty(ENABLED, Boolean.TRUE);
		JSONObject changes = changesAsJSON(wc);

		assertAll(
			() -> assertTrue(changes.has(READONLY), "readOnly must be re-sent after enabled changed, changes were: " + changes),
			() -> assertFalse(changes.optBoolean(READONLY, true), "readOnly must be false once enabled is true, changes were: " + changes),
			() -> assertTrue(changes.optBoolean(ENABLED, false), "enabled must be sent as true, changes were: " + changes));
	}

	@Test
	@DisplayName("disabling a checkbox whose readOnly is in the properties sends readOnly=true")
	void disablingCheckSendsReadOnlyTrue() throws JSONException
	{
		WebFormComponent wc = showForm().getWebComponent(CHECK_NAME);
		putReadOnlyInProperties(wc, false);
		wc.getAndClearChanges();

		wc.setProperty(ENABLED, Boolean.FALSE);
		JSONObject changes = changesAsJSON(wc);

		assertAll(
			() -> assertTrue(changes.has(READONLY), "readOnly must be sent after enabled changed, changes were: " + changes),
			() -> assertTrue(changes.optBoolean(READONLY, false), "readOnly must be true once enabled is false, changes were: " + changes));
	}

	@Test
	@DisplayName("enabled change does not add readOnly to the changes when readOnly only lives as a default value")
	void enabledChangeSkipsDefaultOnlyReadOnly() throws JSONException
	{
		WebFormComponent wc = showForm().getWebComponent(CHECK_NAME);
		boolean readOnlyInProperties = wc.getProperties().content.containsKey(READONLY);
		wc.getAndClearChanges();

		wc.setProperty(ENABLED, Boolean.FALSE);
		JSONObject changes = changesAsJSON(wc);

		assertAll(
			() -> assertFalse(readOnlyInProperties, "precondition: readOnly should only be a default value right after the form is shown"),
			() -> assertTrue(changes.has(ENABLED), "enabled must be sent, changes were: " + changes),
			() -> assertFalse(changes.has(READONLY), "a default-only readOnly must not be added to the changes, changes were: " + changes));
	}

	@Test
	@DisplayName("disabling the form re-sends readOnly of a child checkbox")
	void formEnabledChangeResendsChildReadOnly() throws JSONException
	{
		IWebFormUI formUI = showForm();
		WebFormComponent wc = formUI.getWebComponent(CHECK_NAME);
		putReadOnlyInProperties(wc, false);
		wc.getAndClearChanges();

		formUI.setComponentEnabled(false);
		JSONObject changes = changesAsJSON(wc);

		assertAll(
			() -> assertTrue(changes.has(READONLY), "child readOnly must be re-sent after the form enabled changed, changes were: " + changes),
			() -> assertTrue(changes.optBoolean(READONLY, false), "child readOnly must reflect the disabled parent, changes were: " + changes));
	}

	@Test
	@DisplayName("editable change on a textarea re-sends readOnly")
	void editableChangeResendsReadOnly() throws JSONException
	{
		WebFormComponent wc = showForm().getWebComponent(TEXTAREA_NAME);
		putReadOnlyInProperties(wc, false);
		wc.getAndClearChanges();

		wc.setProperty(EDITABLE, Boolean.FALSE);
		JSONObject afterDisable = changesAsJSON(wc);
		wc.setProperty(EDITABLE, Boolean.TRUE);
		JSONObject afterEnable = changesAsJSON(wc);

		assertAll(
			() -> assertTrue(afterDisable.optBoolean(READONLY, false), "readOnly must be sent as true after editable=false, changes were: " + afterDisable),
			() -> assertTrue(afterEnable.has(READONLY), "readOnly must be re-sent after editable=true, changes were: " + afterEnable),
			() -> assertFalse(afterEnable.optBoolean(READONLY, true), "readOnly must be false after editable=true, changes were: " + afterEnable));
	}

	@Test
	@DisplayName("editable change during a find mode change marks readOnly but keeps the saved editable value")
	void findModeChangeMarksReadOnlyButKeepsSavedValue() throws JSONException
	{
		WebFormComponent wc = showForm().getWebComponent(TEXTAREA_NAME);
		putReadOnlyInProperties(wc, false);
		ReadonlySabloValue value = (ReadonlySabloValue)wc.getProperty(READONLY);
		boolean savedBefore = value.getOldOppositeOfValue();
		wc.getAndClearChanges();

		value.setFindModeChange(true);
		wc.setProperty(EDITABLE, Boolean.FALSE);
		value.setFindModeChange(false);
		JSONObject changes = changesAsJSON(wc);

		assertAll(
			() -> assertTrue(savedBefore, "precondition: the saved editable value should be true"),
			() -> assertTrue(changes.optBoolean(READONLY, false), "readOnly must still be sent during a find mode change, changes were: " + changes),
			() -> assertEquals(savedBefore, value.getOldOppositeOfValue(), "a find mode change must not overwrite the saved editable value"));
	}

	@Test
	@DisplayName("a replaced readOnly value is detached and no longer tracks editable changes")
	void replacedReadOnlyValueIsDetached()
	{
		WebFormComponent wc = showForm().getWebComponent(TEXTAREA_NAME);
		putReadOnlyInProperties(wc, false);
		ReadonlySabloValue oldValue = (ReadonlySabloValue)wc.getProperty(READONLY);
		boolean oldSaved = oldValue.getOldOppositeOfValue();

		putReadOnlyInProperties(wc, false);
		ReadonlySabloValue newValue = (ReadonlySabloValue)wc.getProperty(READONLY);

		assertDoesNotThrow(() -> wc.setProperty(EDITABLE, Boolean.FALSE), "changing editable after a readOnly replace must not throw");
		assertAll(
			() -> assertNotSame(oldValue, newValue, "precondition: readOnly must have been replaced"),
			() -> assertEquals(oldSaved, oldValue.getOldOppositeOfValue(), "the detached value must no longer listen to editable changes"),
			() -> assertFalse(newValue.getOldOppositeOfValue(), "the new value must track the editable change"),
			() -> assertFalse(oldValue.getValue(), "a detached value has no context and falls back to readOnly=false"));
	}

	@Test
	@DisplayName("form readOnly true disables the checkbox and readOnly false restores enabled")
	void formReadOnlyToggleStillDisablesAndRestoresEnabled() throws JSONException
	{
		IWebFormUI formUI = showForm();
		WebFormComponent wc = formUI.getWebComponent(CHECK_NAME);

		formUI.setReadOnly(true);
		Object enabledWhileReadOnly = wc.getProperty(ENABLED);
		boolean readOnlyWhileReadOnly = ((ReadonlySabloValue)wc.getProperty(READONLY)).getValue();
		formUI.setReadOnly(false);
		Object enabledAfterRestore = wc.getProperty(ENABLED);
		JSONObject changes = changesAsJSON(wc);

		assertAll(
			() -> assertEquals(Boolean.FALSE, enabledWhileReadOnly, "readOnly=true must disable the checkbox"),
			() -> assertTrue(readOnlyWhileReadOnly, "readOnly must be true while the form is read-only"),
			() -> assertEquals(Boolean.TRUE, enabledAfterRestore, "readOnly=false must restore enabled"),
			() -> assertFalse(changes.optBoolean(READONLY, true), "readOnly must be sent as false after the restore, changes were: " + changes));
	}
}
