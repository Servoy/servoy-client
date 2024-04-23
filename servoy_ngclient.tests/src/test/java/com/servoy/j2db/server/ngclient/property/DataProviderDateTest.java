/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2021 Servoy BV

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
import java.awt.print.PageFormat;
import java.net.URL;
import java.rmi.Remote;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ScheduledExecutorService;

import javax.swing.ImageIcon;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONWriter;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sablo.IChangeListener;
import org.sablo.WebComponent;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebObjectFunctionDefinition;
import org.sablo.websocket.utils.DataConversion;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.IBeanManager;
import com.servoy.j2db.IDataRendererFactory;
import com.servoy.j2db.ILAFManager;
import com.servoy.j2db.IModeManager;
import com.servoy.j2db.cmd.ICmdManager;
import com.servoy.j2db.component.ComponentFormat;
import com.servoy.j2db.dataprocessing.ClientInfo;
import com.servoy.j2db.dataprocessing.DataServerProxy;
import com.servoy.j2db.dataprocessing.IClientHost;
import com.servoy.j2db.dataprocessing.IDataServer;
import com.servoy.j2db.dataprocessing.IFoundSetManagerInternal;
import com.servoy.j2db.dataprocessing.IRecord;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.plugins.IPluginAccess;
import com.servoy.j2db.plugins.IPluginManager;
import com.servoy.j2db.scripting.IExecutingEnviroment;
import com.servoy.j2db.scripting.JSBlobLoaderBuilder;
import com.servoy.j2db.server.ngclient.IDataAdapterList;
import com.servoy.j2db.server.ngclient.INGApplication;
import com.servoy.j2db.server.ngclient.INGClientWebsocketSession;
import com.servoy.j2db.server.ngclient.INGFormManager;
import com.servoy.j2db.server.ngclient.IServoyDataConverterContext;
import com.servoy.j2db.server.ngclient.IWebFormController;
import com.servoy.j2db.server.ngclient.NGRuntimeWindowManager;
import com.servoy.j2db.server.ngclient.ServoyDataConverterContext;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.property.types.DataproviderTypeSabloValue;
import com.servoy.j2db.server.ngclient.property.types.IDataLinkedType.TargetDataLinks;
import com.servoy.j2db.server.ngclient.property.types.NGDatePropertyType;
import com.servoy.j2db.server.ngclient.utils.NGUtils;
import com.servoy.j2db.server.shared.IApplicationServer;
import com.servoy.j2db.server.shared.IApplicationServerAccess;
import com.servoy.j2db.server.shared.IUserManager;
import com.servoy.j2db.ui.ItemFactory;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.RendererParentWrapper;

/**
 * @author jcompagner
 *
 */
@SuppressWarnings("nls")
public class DataProviderDateTest
{
	private static final String THREE_AT_NIGHT_PLUS1 = "2021-11-15T03:00:00.000+01:00";
	private static final String THREE_IN_AFTERNOON_PLUS1 = "2021-11-14T15:00:00.000+01:00";

	private static final String THREE_AT_NIGHT_NEXT_DAY_PLUS1 = "2021-11-16T03:00:00.000+01:00";

	private static final String THREE_AT_NIGHT_PLUS13 = "2021-11-15T03:00:00.000+13:00";
	private static final String THREE_IN_AFTERNOON_PLUS13 = "2021-11-15T15:00:00.000+13:00";

	private static final TimeZone defaultTimeZone = TimeZone.getDefault();

	private TestDataproviderTypeSabloValue createSabloValue(boolean useLocalDate, String initialValue, String format)
	{
		ServoyDataConverterContext context = new ServoyDataConverterContext(new ServiceProvider());
		DataAdapterList dal = new DataAdapterList(context.getApplication());
		PropertyDescription pd = NGUtils.getDataProviderPropertyDescription(IColumnTypes.DATETIME, false, useLocalDate);
		TestDataproviderTypeSabloValue value = new TestDataproviderTypeSabloValue("mydate", dal, context, pd, format);
		value.setInitialValue(NGDatePropertyType.NG_INSTANCE.fromJSON(initialValue, false));
		return value;
	}

	@Before
	public void setup()
	{
		TimeZone.setDefault(TimeZone.getTimeZone("GMT+13:00"));
	}

	@After
	public void after()
	{
		TimeZone.setDefault(defaultTimeZone);
	}

	@Test
	public void testWithNullOriginalAndUseLocalDate()
	{
		TestDataproviderTypeSabloValue value = createSabloValue(true, null, null);
		value.browserUpdateReceived(THREE_AT_NIGHT_PLUS1, null);
		Assert.assertEquals(NGDatePropertyType.NG_INSTANCE.fromJSON(THREE_AT_NIGHT_PLUS13, false), value.getValue());
		JSONWriter writer = new JSONStringer();
		writer.object();
		value.toJSON(writer, "mydate", new DataConversion(), null);
		writer.endObject();
		JSONObject json = new JSONObject(writer.toString());
		Assert.assertEquals("2021-11-15T03:00", json.getString("mydate"));


		value = createSabloValue(true, null, null);
		value.browserUpdateReceived(THREE_IN_AFTERNOON_PLUS1, null);
		Assert.assertEquals(NGDatePropertyType.NG_INSTANCE.fromJSON("2021-11-14T15:00:00.000+13:00", false), value.getValue());
		writer = new JSONStringer();
		writer.object();
		value.toJSON(writer, "mydate", new DataConversion(), null);
		writer.endObject();
		json = new JSONObject(writer.toString());
		Assert.assertEquals("2021-11-14T15:00", json.getString("mydate"));
	}

	@Test
	public void testWithOriginalAndGettingANull()
	{
		TestDataproviderTypeSabloValue value = createSabloValue(false, THREE_IN_AFTERNOON_PLUS13, "dd-MM-yyy");
		value.browserUpdateReceived(null, null);
		Assert.assertNull(value.getValue());
	}

	@Test
	public void testWithOriginalAndUseLocalDate()
	{
		TestDataproviderTypeSabloValue value = createSabloValue(true, THREE_IN_AFTERNOON_PLUS13, null);
		value.browserUpdateReceived(THREE_AT_NIGHT_PLUS1, null);
		Assert.assertEquals(NGDatePropertyType.NG_INSTANCE.fromJSON(THREE_AT_NIGHT_PLUS13, false), value.getValue());
		JSONWriter writer = new JSONStringer();
		writer.object();
		value.toJSON(writer, "mydate", new DataConversion(), null);
		writer.endObject();
		JSONObject json = new JSONObject(writer.toString());
		Assert.assertEquals("2021-11-15T03:00", json.getString("mydate"));


		value = createSabloValue(true, THREE_IN_AFTERNOON_PLUS13, null);
		value.browserUpdateReceived(THREE_IN_AFTERNOON_PLUS1, null);
		Assert.assertEquals(NGDatePropertyType.NG_INSTANCE.fromJSON("2021-11-14T15:00:00.000+13:00", false), value.getValue());
		writer = new JSONStringer();
		writer.object();
		value.toJSON(writer, "mydate", new DataConversion(), null);
		writer.endObject();
		json = new JSONObject(writer.toString());
		Assert.assertEquals("2021-11-14T15:00", json.getString("mydate"));
	}

	@Test
	public void testWithOriginalAndUseLocalDateAndFormatting()
	{
		TestDataproviderTypeSabloValue value = createSabloValue(true, THREE_IN_AFTERNOON_PLUS13, "dd-MM-yyy");
		value.browserUpdateReceived(THREE_AT_NIGHT_PLUS1, null);
		Assert.assertEquals(NGDatePropertyType.NG_INSTANCE.fromJSON(THREE_IN_AFTERNOON_PLUS13, false), value.getValue());
		JSONWriter writer = new JSONStringer();
		writer.object();
		value.toJSON(writer, "mydate", new DataConversion(), null);
		writer.endObject();
		JSONObject json = new JSONObject(writer.toString());
		Assert.assertEquals("2021-11-15T15:00", json.getString("mydate"));


		value = createSabloValue(true, THREE_IN_AFTERNOON_PLUS13, "dd-MM-yyy");
		value.browserUpdateReceived(THREE_IN_AFTERNOON_PLUS1, null);
		Assert.assertEquals(NGDatePropertyType.NG_INSTANCE.fromJSON("2021-11-14T15:00:00.000+13:00", false), value.getValue());
		writer = new JSONStringer();
		writer.object();
		value.toJSON(writer, "mydate", new DataConversion(), null);
		writer.endObject();
		json = new JSONObject(writer.toString());
		Assert.assertEquals("2021-11-14T15:00", json.getString("mydate"));

		value = createSabloValue(true, THREE_AT_NIGHT_PLUS13, "dd-MM-yyy");
		value.browserUpdateReceived(THREE_IN_AFTERNOON_PLUS1, null);
		Assert.assertEquals(NGDatePropertyType.NG_INSTANCE.fromJSON("2021-11-14T03:00:00.000+13:00", false), value.getValue());
		writer = new JSONStringer();
		writer.object();
		value.toJSON(writer, "mydate", new DataConversion(), null);
		writer.endObject();
		json = new JSONObject(writer.toString());
		Assert.assertEquals("2021-11-14T03:00", json.getString("mydate"));
	}

	@Test
	public void testWithNullOriginalAndNotUseLocalDate()
	{
		TestDataproviderTypeSabloValue value = createSabloValue(false, null, null);
		value.browserUpdateReceived(THREE_AT_NIGHT_PLUS1, null);
		Assert.assertEquals(NGDatePropertyType.NG_INSTANCE.fromJSON(THREE_IN_AFTERNOON_PLUS13, false), value.getValue());
		JSONWriter writer = new JSONStringer();
		writer.object();
		value.toJSON(writer, "mydate", new DataConversion(), null);
		writer.endObject();
		JSONObject json = new JSONObject(writer.toString());
		Assert.assertEquals("2021-11-15T15:00+13:00", json.getString("mydate"));

		value = createSabloValue(false, null, null);
		value.browserUpdateReceived(THREE_IN_AFTERNOON_PLUS1, null);
		Assert.assertEquals(NGDatePropertyType.NG_INSTANCE.fromJSON(THREE_AT_NIGHT_PLUS13, false), value.getValue());
		writer = new JSONStringer();
		writer.object();
		value.toJSON(writer, "mydate", new DataConversion(), null);
		writer.endObject();
		json = new JSONObject(writer.toString());
		Assert.assertEquals("2021-11-15T03:00+13:00", json.getString("mydate"));
	}

	@Test
	public void testWithAfternoonDateOriginal1AndNotUseLocalDateWithddmmyyyFormat()
	{
		TestDataproviderTypeSabloValue value = createSabloValue(false, THREE_IN_AFTERNOON_PLUS13, "dd-MM-yyy");
		JSONWriter writer = new JSONStringer();
		writer.object();
		value.toJSON(writer, "mydate", new DataConversion(), null);
		writer.endObject();
		JSONObject json = new JSONObject(writer.toString());
		Assert.assertEquals("2021-11-15T15:00+13:00", json.getString("mydate"));

		value.browserUpdateReceived("2021-11-16T15:00+01:00", null);
		Assert.assertEquals(new Date(NGDatePropertyType.NG_INSTANCE.fromJSON(THREE_IN_AFTERNOON_PLUS13, false).getTime() + (24 * 60 * 60 * 1000)),
			value.getValue());

		writer = new JSONStringer();
		writer.object();
		value.toJSON(writer, "mydate", new DataConversion(), null);
		writer.endObject();
		json = new JSONObject(writer.toString());
		Assert.assertEquals("2021-11-16T15:00+13:00", json.getString("mydate"));
	}

	@Test
	public void testWithAfternoonDateOriginal2AndNotUseLocalDateWithddmmyyyFormat()
	{
		TestDataproviderTypeSabloValue value = createSabloValue(false, THREE_IN_AFTERNOON_PLUS13, "dd-MM-yyy");
		JSONWriter writer = new JSONStringer();
		writer.object();
		value.toJSON(writer, "mydate", new DataConversion(), null);
		writer.endObject();
		JSONObject json = new JSONObject(writer.toString());
		Assert.assertEquals("2021-11-15T15:00+13:00", json.getString("mydate"));

		value.browserUpdateReceived(THREE_AT_NIGHT_NEXT_DAY_PLUS1, null);
		Assert.assertEquals(new Date(NGDatePropertyType.NG_INSTANCE.fromJSON(THREE_IN_AFTERNOON_PLUS13, false).getTime() + (24 * 60 * 60 * 1000)),
			value.getValue());

		writer = new JSONStringer();
		writer.object();
		value.toJSON(writer, "mydate", new DataConversion(), null);
		writer.endObject();
		json = new JSONObject(writer.toString());
		Assert.assertEquals("2021-11-16T15:00+13:00", json.getString("mydate"));
	}

	@Test
	public void testWithAfternoonDateOriginal1AndNotUseLocalDateWithoutFormat()
	{
		TestDataproviderTypeSabloValue value = createSabloValue(false, THREE_IN_AFTERNOON_PLUS13, null);
		JSONWriter writer = new JSONStringer();
		writer.object();
		value.toJSON(writer, "mydate", new DataConversion(), null);
		writer.endObject();
		JSONObject json = new JSONObject(writer.toString());
		Assert.assertEquals("2021-11-15T15:00+13:00", json.getString("mydate"));

		// without a format the date is taken as is, no merges.
		value.browserUpdateReceived("2021-11-16T15:00+01:00", null);
		Assert.assertEquals(new Date(NGDatePropertyType.NG_INSTANCE.fromJSON("2021-11-16T15:00+01:00", false).getTime()),
			value.getValue());

		writer = new JSONStringer();
		writer.object();
		value.toJSON(writer, "mydate", new DataConversion(), null);
		writer.endObject();
		json = new JSONObject(writer.toString());
		Assert.assertEquals("2021-11-17T03:00+13:00", json.getString("mydate"));
	}

	@Test
	public void testWithAfternoonDateOriginal2AndNotUseLocalDateWithoutFormat()
	{
		TestDataproviderTypeSabloValue value = createSabloValue(false, THREE_IN_AFTERNOON_PLUS13, null);
		JSONWriter writer = new JSONStringer();
		writer.object();
		value.toJSON(writer, "mydate", new DataConversion(), null);
		writer.endObject();
		JSONObject json = new JSONObject(writer.toString());
		Assert.assertEquals("2021-11-15T15:00+13:00", json.getString("mydate"));

		value.browserUpdateReceived(THREE_AT_NIGHT_NEXT_DAY_PLUS1, null);
		Assert.assertEquals(new Date(NGDatePropertyType.NG_INSTANCE.fromJSON(THREE_IN_AFTERNOON_PLUS13, false).getTime() + (24 * 60 * 60 * 1000)),
			value.getValue());

		writer = new JSONStringer();
		writer.object();
		value.toJSON(writer, "mydate", new DataConversion(), null);
		writer.endObject();
		json = new JSONObject(writer.toString());
		Assert.assertEquals("2021-11-16T15:00+13:00", json.getString("mydate"));
	}

	@Test
	public void testWithNightDateOriginal1AndNotUseLocalDateWithddmmyyyFormat()
	{
		TestDataproviderTypeSabloValue value = createSabloValue(false, THREE_AT_NIGHT_PLUS13, "dd-MM-yyy");
		JSONWriter writer = new JSONStringer();
		writer.object();
		value.toJSON(writer, "mydate", new DataConversion(), null);
		writer.endObject();
		JSONObject json = new JSONObject(writer.toString());
		Assert.assertEquals("2021-11-15T03:00+13:00", json.getString("mydate"));

		value.browserUpdateReceived("2021-11-15T15:00+01:00", null);
		Assert.assertEquals(new Date(NGDatePropertyType.NG_INSTANCE.fromJSON(THREE_AT_NIGHT_PLUS13, false).getTime() + (24 * 60 * 60 * 1000)),
			value.getValue());

		writer = new JSONStringer();
		writer.object();
		value.toJSON(writer, "mydate", new DataConversion(), null);
		writer.endObject();
		json = new JSONObject(writer.toString());
		Assert.assertEquals("2021-11-16T03:00+13:00", json.getString("mydate"));
	}

	@Test
	public void testWithNightDateOriginal2AndNotUseLocalDateWithddmmyyyFormat()
	{
		TestDataproviderTypeSabloValue value = createSabloValue(false, THREE_AT_NIGHT_PLUS13, "dd-MM-yyy");
		JSONWriter writer = new JSONStringer();
		writer.object();
		value.toJSON(writer, "mydate", new DataConversion(), null);
		writer.endObject();
		JSONObject json = new JSONObject(writer.toString());
		Assert.assertEquals("2021-11-15T03:00+13:00", json.getString("mydate"));

		value.browserUpdateReceived("2021-11-15T03:00+01:00", null);
		Assert.assertEquals(new Date(NGDatePropertyType.NG_INSTANCE.fromJSON(THREE_AT_NIGHT_PLUS13, false).getTime() + (24 * 60 * 60 * 1000)),
			value.getValue());

		writer = new JSONStringer();
		writer.object();
		value.toJSON(writer, "mydate", new DataConversion(), null);
		writer.endObject();
		json = new JSONObject(writer.toString());
		Assert.assertEquals("2021-11-16T03:00+13:00", json.getString("mydate"));
	}

	@Test
	public void testWithNightDateOriginal2AndNotUseLocalDateWithddMMyyyHHmmFormat()
	{
		TestDataproviderTypeSabloValue value = createSabloValue(false, THREE_AT_NIGHT_PLUS13, "dd-MM-yyy HH:mm");
		JSONWriter writer = new JSONStringer();
		writer.object();
		value.toJSON(writer, "mydate", new DataConversion(), null);
		writer.endObject();
		JSONObject json = new JSONObject(writer.toString());
		Assert.assertEquals("2021-11-15T03:00+13:00", json.getString("mydate"));

		value.browserUpdateReceived("2021-11-15T03:00+01:00", null); // 12 hour more
		Assert.assertEquals(new Date(NGDatePropertyType.NG_INSTANCE.fromJSON(THREE_AT_NIGHT_PLUS13, false).getTime() + (12 * 60 * 60 * 1000)),
			value.getValue());

		writer = new JSONStringer();
		writer.object();
		value.toJSON(writer, "mydate", new DataConversion(), null);
		writer.endObject();
		json = new JSONObject(writer.toString());
		Assert.assertEquals("2021-11-15T15:00+13:00", json.getString("mydate"));
	}

	@Test
	public void testWithNightDateOriginal2AndUseLocalDateWithddMMyyyHHmmFormat()
	{
		TestDataproviderTypeSabloValue value = createSabloValue(true, THREE_AT_NIGHT_PLUS13, "dd-MM-yyy HH:mm");
		JSONWriter writer = new JSONStringer();
		writer.object();
		value.toJSON(writer, "mydate", new DataConversion(), null);
		writer.endObject();
		JSONObject json = new JSONObject(writer.toString());
		Assert.assertEquals("2021-11-15T03:00", json.getString("mydate"));

		value.browserUpdateReceived("2021-11-15T03:00+01:00", null); // 12 hour more
		Assert.assertEquals(new Date(NGDatePropertyType.NG_INSTANCE.fromJSON(THREE_AT_NIGHT_PLUS13, false).getTime()),
			value.getValue());

		value.browserUpdateReceived("2021-11-15T15:00+01:00", null); // 12 hour more
		Assert.assertEquals(new Date(NGDatePropertyType.NG_INSTANCE.fromJSON(THREE_AT_NIGHT_PLUS13, false).getTime() + (12 * 60 * 60 * 1000)),
			value.getValue());

		writer = new JSONStringer();
		writer.object();
		value.toJSON(writer, "mydate", new DataConversion(), null);
		writer.endObject();
		json = new JSONObject(writer.toString());
		Assert.assertEquals("2021-11-15T15:00", json.getString("mydate"));
	}

	@Test
	public void testWithNightDateOriginalAndNotUseLocalDateWithHHFormat()
	{
		TestDataproviderTypeSabloValue value = createSabloValue(false, THREE_AT_NIGHT_PLUS13, "HH");
		JSONWriter writer = new JSONStringer();
		writer.object();
		value.toJSON(writer, "mydate", new DataConversion(), null);
		writer.endObject();
		JSONObject json = new JSONObject(writer.toString());
		Assert.assertEquals("2021-11-15T03:00+13:00", json.getString("mydate"));

		value.browserUpdateReceived("2021-11-14T03:00+01:00", null);
		Assert.assertEquals(new Date(NGDatePropertyType.NG_INSTANCE.fromJSON(THREE_AT_NIGHT_PLUS13, false).getTime() - (12 * 60 * 60 * 1000)),
			value.getValue());

		writer = new JSONStringer();
		writer.object();
		value.toJSON(writer, "mydate", new DataConversion(), null);
		writer.endObject();
		json = new JSONObject(writer.toString());
		Assert.assertEquals("2021-11-14T15:00+13:00", json.getString("mydate"));
	}

	@Test
	public void testWithNightDateOriginal2AndNotUseLocalDateWithHHFormat()
	{
		TestDataproviderTypeSabloValue value = createSabloValue(false, THREE_IN_AFTERNOON_PLUS13, "HH");
		JSONWriter writer = new JSONStringer();
		writer.object();
		value.toJSON(writer, "mydate", new DataConversion(), null);
		writer.endObject();
		JSONObject json = new JSONObject(writer.toString());
		Assert.assertEquals("2021-11-15T15:00+13:00", json.getString("mydate"));

		value.browserUpdateReceived("2021-11-15T15:00+01:00", null);
		Assert.assertEquals(new Date(NGDatePropertyType.NG_INSTANCE.fromJSON(THREE_IN_AFTERNOON_PLUS13, false).getTime() + (12 * 60 * 60 * 1000)),
			value.getValue());

		writer = new JSONStringer();
		writer.object();
		value.toJSON(writer, "mydate", new DataConversion(), null);
		writer.endObject();
		json = new JSONObject(writer.toString());
		Assert.assertEquals("2021-11-16T03:00+13:00", json.getString("mydate"));
	}

	private static class TestDataproviderTypeSabloValue extends DataproviderTypeSabloValue
	{

		public TestDataproviderTypeSabloValue(String dataProviderID, IDataAdapterList dataAdapterList, IServoyDataConverterContext servoyDataConverterContext,
			PropertyDescription dpPD, String format)
		{
			super(dataProviderID, dataAdapterList, servoyDataConverterContext, dpPD);
			this.typeOfDP = dpPD;
			if (format != null)
				this.fieldFormat = ComponentFormat.getComponentFormat(format, IColumnTypes.DATETIME, servoyDataConverterContext.getApplication());
		}

		public void setInitialValue(Date date)
		{
			this.uiValue = date;
		}

	}

	private static class DataAdapterList implements IDataAdapterList
	{

		private final INGApplication application;

		public DataAdapterList(INGApplication application)
		{
			this.application = application;
		}

		@Override
		public String getStringValue(String name)
		{
			return null;
		}

		@Override
		public void pushChanges(WebFormComponent webComponent, String string)
		{
		}

		@Override
		public void pushChanges(WebFormComponent webComponent, String string, String foundsetLinkedRowID)
		{
		}

		@Override
		public void pushChanges(WebFormComponent webComponent, String string, Object newValue, String foundsetLinkedRowID)
		{
		}

		@Override
		public Object executeEvent(WebComponent webComponent, String event, int eventId, Object[] args)
		{
			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.server.ngclient.IDataAdapterList#executeInlineScript(java.lang.String, org.json.JSONObject, org.json.JSONArray)
		 */
		@Override
		public Object executeInlineScript(String script, JSONObject args, JSONArray appendingArgs)
		{

			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.server.ngclient.IDataAdapterList#setRecord(com.servoy.j2db.dataprocessing.IRecord, boolean)
		 */
		@Override
		public void setRecord(IRecord record, boolean fireChangeEvent)
		{


		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.server.ngclient.IDataAdapterList#startEdit(com.servoy.j2db.server.ngclient.WebFormComponent, java.lang.String, java.lang.String)
		 */
		@Override
		public void startEdit(WebFormComponent webComponent, String property, String foundsetLinkedRowID)
		{


		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.server.ngclient.IDataAdapterList#setFindMode(boolean)
		 */
		@Override
		public void setFindMode(boolean findMode)
		{


		}

		@Override
		public INGApplication getApplication()
		{
			return application;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.server.ngclient.IDataAdapterList#getForm()
		 */
		@Override
		public IWebFormController getForm()
		{

			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.server.ngclient.IDataAdapterList#getDataProviderID(com.servoy.j2db.server.ngclient.WebFormComponent, java.lang.String)
		 */
		@Override
		public String getDataProviderID(WebFormComponent webComponent, String beanProperty)
		{

			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.server.ngclient.IDataAdapterList#addVisibleChildForm(com.servoy.j2db.server.ngclient.IWebFormController, java.lang.String,
		 * boolean)
		 */
		@Override
		public void addVisibleChildForm(IWebFormController form, String relation, boolean shouldUpdateParentFormController)
		{


		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.server.ngclient.IDataAdapterList#removeVisibleChildForm(com.servoy.j2db.server.ngclient.IWebFormController, boolean)
		 */
		@Override
		public void removeVisibleChildForm(IWebFormController form, boolean firstLevel)
		{


		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.server.ngclient.IDataAdapterList#getRelatedForms()
		 */
		@Override
		public Map<IWebFormController, String> getRelatedForms()
		{

			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.server.ngclient.IDataAdapterList#addParentRelatedForm(com.servoy.j2db.server.ngclient.IWebFormController)
		 */
		@Override
		public void addParentRelatedForm(IWebFormController form)
		{
		}


		@Override
		public void removeParentRelatedForm(IWebFormController form)
		{

		}


		@Override
		public List<IWebFormController> getParentRelatedForms()
		{

			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.server.ngclient.IDataAdapterList#getRecord()
		 */
		@Override
		public IRecordInternal getRecord()
		{

			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.server.ngclient.IDataAdapterList#getValueObject(com.servoy.j2db.dataprocessing.IRecord, java.lang.String)
		 */
		@Override
		public Object getValueObject(IRecord recordToUse, String dataProviderId)
		{

			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.server.ngclient.IDataAdapterList#destroy()
		 */
		@Override
		public void destroy()
		{


		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.server.ngclient.IDataAdapterList#notifyVisible(boolean, java.util.List, java.util.Set)
		 */
		@Override
		public void notifyVisible(boolean b, List<Runnable> invokeLaterRunnables, Set<IWebFormController> childFormsThatWereNotified)
		{


		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.server.ngclient.IDataAdapterList#stopUIEditing(boolean)
		 */
		@Override
		public boolean stopUIEditing(boolean looseFocus)
		{

			return false;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.server.ngclient.IDataAdapterList#addDataLinkedProperty(com.servoy.j2db.server.ngclient.property.IDataLinkedPropertyValue,
		 * com.servoy.j2db.server.ngclient.property.types.IDataLinkedType.TargetDataLinks)
		 */
		@Override
		public void addDataLinkedProperty(IDataLinkedPropertyValue propertyValue, TargetDataLinks dataLinks)
		{


		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.server.ngclient.IDataAdapterList#removeDataLinkedProperty(com.servoy.j2db.server.ngclient.property.IDataLinkedPropertyValue)
		 */
		@Override
		public void removeDataLinkedProperty(IDataLinkedPropertyValue propertyValue)
		{


		}

		/*
		 * (non-Javadoc)
		 *
		 * @see
		 * com.servoy.j2db.server.ngclient.IDataAdapterList#removeFindModeAwareProperty(com.servoy.j2db.server.ngclient.property.IFindModeAwarePropertyValue)
		 */
		@Override
		public void removeFindModeAwareProperty(IFindModeAwarePropertyValue propertyValue)
		{


		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.server.ngclient.IDataAdapterList#addFindModeAwareProperty(com.servoy.j2db.server.ngclient.property.IFindModeAwarePropertyValue)
		 */
		@Override
		public void addFindModeAwareProperty(IFindModeAwarePropertyValue propertyValue)
		{


		}

	}

	private static class ServiceProvider implements INGApplication
	{
		@Override
		public Object generateBrowserFunction(String functionString)
		{
			return functionString;
		}

		@Override
		public JSBlobLoaderBuilder createUrlBlobloaderBuilder(String dataprovider)
		{
			return null;
		}

		@Override
		public void invokeLater(Runnable r)
		{
		}

		@Override
		public boolean isEventDispatchThread()
		{
			return false;
		}

		@Override
		public void invokeAndWait(Runnable r)
		{
		}

		@Override
		public String getI18NMessage(String i18nKey)
		{
			return null;
		}

		@Override
		public String getI18NMessage(String i18nKey, String language, String country)
		{
			return i18nKey;
		}

		@Override
		public String getI18NMessage(String i18nKey, Object[] array)
		{
			return i18nKey;
		}

		@Override
		public String getI18NMessage(String i18nKey, Object[] array, String language, String country)
		{
			return i18nKey;
		}

		@Override
		public String getI18NMessageIfPrefixed(String i18nKey)
		{
			return i18nKey;
		}

		@Override
		public void setI18NMessage(String i18nKey, String value)
		{
		}

		@Override
		public IRepository getRepository()
		{

			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IServiceProvider#haveRepositoryAccess()
		 */
		@Override
		public boolean haveRepositoryAccess()
		{

			return false;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IServiceProvider#getApplicationServer()
		 */
		@Override
		public IApplicationServer getApplicationServer()
		{

			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IServiceProvider#getDataServer()
		 */
		@Override
		public IDataServer getDataServer()
		{

			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IServiceProvider#getClientHost()
		 */
		@Override
		public IClientHost getClientHost()
		{

			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IServiceProvider#getClientInfo()
		 */
		@Override
		public ClientInfo getClientInfo()
		{

			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IServiceProvider#getSolution()
		 */
		@Override
		public Solution getSolution()
		{

			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IServiceProvider#isSolutionLoaded()
		 */
		@Override
		public boolean isSolutionLoaded()
		{

			return false;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IServiceProvider#getFlattenedSolution()
		 */
		@Override
		public FlattenedSolution getFlattenedSolution()
		{

			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IServiceProvider#handleException(java.lang.String, java.lang.Exception)
		 */
		@Override
		public void handleException(String servoyMsg, Exception e)
		{


		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IServiceProvider#reportError(java.lang.String, java.lang.Object)
		 */
		@Override
		public void reportError(String msg, Object detail)
		{


		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IServiceProvider#reportInfo(java.lang.String)
		 */
		@Override
		public void reportInfo(String msg)
		{


		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IServiceProvider#reportWarning(java.lang.String)
		 */
		@Override
		public void reportWarning(String msg)
		{


		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IServiceProvider#getScriptEngine()
		 */
		@Override
		public IExecutingEnviroment getScriptEngine()
		{

			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IServiceProvider#getScheduledExecutor()
		 */
		@Override
		public ScheduledExecutorService getScheduledExecutor()
		{

			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IServiceProvider#reportJSError(java.lang.String, java.lang.Object)
		 */
		@Override
		public void reportJSError(String msg, Object detail)
		{


		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IServiceProvider#getUserUID()
		 */
		@Override
		public String getUserUID()
		{

			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IServiceProvider#getUserName()
		 */
		@Override
		public String getUserName()
		{

			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IServiceProvider#getClientID()
		 */
		@Override
		public String getClientID()
		{

			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IServiceProvider#getTenantValue()
		 */
		@Override
		public Object[] getTenantValue()
		{

			return null;
		}

		@Override
		public Locale getLocale()
		{
			return new Locale("nl", "NL");
		}

		@Override
		public TimeZone getTimeZone()
		{
			return TimeZone.getTimeZone("GMT+01:00");
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IServiceProvider#getSettings()
		 */
		@Override
		public Properties getSettings()
		{

			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IServiceProvider#getRuntimeProperties()
		 */
		@Override
		public Map getRuntimeProperties()
		{

			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IServiceProvider#getFoundSetManager()
		 */
		@Override
		public IFoundSetManagerInternal getFoundSetManager()
		{

			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IServiceProvider#isRunningRemote()
		 */
		@Override
		public boolean isRunningRemote()
		{

			return false;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IServiceProvider#getServerURL()
		 */
		@Override
		public URL getServerURL()
		{

			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IServiceProvider#setLocale(java.util.Locale)
		 */
		@Override
		public void setLocale(Locale locale)
		{


		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IServiceProvider#setTimeZone(java.util.TimeZone)
		 */
		@Override
		public void setTimeZone(TimeZone timeZone)
		{


		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.INGClientApplication#overrideStyleSheet(java.lang.String, java.lang.String)
		 */
		@Override
		public void overrideStyleSheet(String oldStyleSheet, String newStyleSheet)
		{


		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.INGClientApplication#setClipboardContent(java.lang.String)
		 */
		@Override
		public void setClipboardContent(String content)
		{


		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.INGClientApplication#getClipboardContent()
		 */
		@Override
		public String getClipboardContent()
		{

			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.INGClientApplication#getMediaURL(java.lang.String)
		 */
		@Override
		public String getMediaURL(String mediaName)
		{

			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IApplication#getApplicationType()
		 */
		@Override
		public int getApplicationType()
		{

			return 0;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IApplication#getClientOSName()
		 */
		@Override
		public String getClientOSName()
		{

			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IApplication#getClientPlatform()
		 */
		@Override
		public int getClientPlatform()
		{

			return 0;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IApplication#setStatusProgress(int)
		 */
		@Override
		public void setStatusProgress(int progress)
		{


		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IApplication#setStatusText(java.lang.String, java.lang.String)
		 */
		@Override
		public void setStatusText(String text, String tooltip)
		{


		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IApplication#showSolutionLoading(boolean)
		 */
		@Override
		public void showSolutionLoading(boolean loading)
		{


		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IApplication#getCmdManager()
		 */
		@Override
		public ICmdManager getCmdManager()
		{

			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IApplication#getBeanManager()
		 */
		@Override
		public IBeanManager getBeanManager()
		{

			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IApplication#getPluginManager()
		 */
		@Override
		public IPluginManager getPluginManager()
		{

			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IApplication#getModeManager()
		 */
		@Override
		public IModeManager getModeManager()
		{

			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IApplication#getUserManager()
		 */
		@Override
		public IUserManager getUserManager()
		{

			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IApplication#getApplicationName()
		 */
		@Override
		public String getApplicationName()
		{

			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IApplication#putClientProperty(java.lang.Object, java.lang.Object)
		 */
		@Override
		public boolean putClientProperty(Object name, Object val)
		{

			return false;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IApplication#getClientProperty(java.lang.Object)
		 */
		@Override
		public Object getClientProperty(Object key)
		{

			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IApplication#setTitle(java.lang.String)
		 */
		@Override
		public void setTitle(String title)
		{


		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IApplication#getLAFManager()
		 */
		@Override
		public ILAFManager getLAFManager()
		{

			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IApplication#output(java.lang.Object, int)
		 */
		@Override
		public void output(Object msg, int level)
		{


		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IApplication#authenticate(java.lang.String, java.lang.String, java.lang.Object[])
		 */
		@Override
		public Object authenticate(String authenticator_solution, String method, Object[] credentials) throws RepositoryException
		{

			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IApplication#logout(java.lang.Object[])
		 */
		@Override
		public void logout(Object[] solution_to_open_args)
		{


		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IApplication#closeSolution(boolean, java.lang.Object[])
		 */
		@Override
		public boolean closeSolution(boolean force, Object[] args)
		{

			return false;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IApplication#getItemFactory()
		 */
		@Override
		public ItemFactory getItemFactory()
		{

			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IApplication#getDataRenderFactory()
		 */
		@Override
		public IDataRendererFactory getDataRenderFactory()
		{

			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IApplication#getPluginAccess()
		 */
		@Override
		public IPluginAccess getPluginAccess()
		{

			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IApplication#getPrintingRendererParent()
		 */
		@Override
		public RendererParentWrapper getPrintingRendererParent()
		{

			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IApplication#getPageFormat()
		 */
		@Override
		public PageFormat getPageFormat()
		{

			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IApplication#setPageFormat(java.awt.print.PageFormat)
		 */
		@Override
		public void setPageFormat(PageFormat currentPageFormat)
		{


		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IApplication#clearLoginForm()
		 */
		@Override
		public void clearLoginForm()
		{


		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IApplication#getUserProperty(java.lang.String)
		 */
		@Override
		public String getUserProperty(String name)
		{

			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IApplication#setUserProperty(java.lang.String, java.lang.String)
		 */
		@Override
		public void setUserProperty(String name, String value)
		{


		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IApplication#getUserPropertyNames()
		 */
		@Override
		public String[] getUserPropertyNames()
		{

			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IApplication#handleClientUserUidChanged(java.lang.String, java.lang.String)
		 */
		@Override
		public void handleClientUserUidChanged(String userUidBefore, String userUidAfter)
		{


		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IApplication#getServerService(java.lang.String)
		 */
		@Override
		public Remote getServerService(String name)
		{

			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IApplication#setI18NMessagesFilter(java.lang.String, java.lang.String[])
		 */
		@Override
		public void setI18NMessagesFilter(String columnname, String[] value)
		{


		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IApplication#getResourceBundle(java.util.Locale)
		 */
		@Override
		public ResourceBundle getResourceBundle(Locale locale)
		{

			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IApplication#getScreenSize()
		 */
		@Override
		public Dimension getScreenSize()
		{

			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IApplication#showURL(java.lang.String, java.lang.String, java.lang.String, int, boolean)
		 */
		@Override
		public boolean showURL(String url, String target, String target_options, int timeout, boolean onRootFrame)
		{

			return false;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IApplication#isInDeveloper()
		 */
		@Override
		public boolean isInDeveloper()
		{

			return false;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IApplication#isShutDown()
		 */
		@Override
		public boolean isShutDown()
		{

			return false;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IApplication#getDataServerProxy()
		 */
		@Override
		public DataServerProxy getDataServerProxy()
		{

			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IApplication#getSolutionName()
		 */
		@Override
		public String getSolutionName()
		{

			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IApplication#looseFocus()
		 */
		@Override
		public void looseFocus()
		{


		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IApplication#updateUI(int)
		 */
		@Override
		public void updateUI(int time)
		{


		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IApplication#setValueListItems(java.lang.String, java.lang.Object[], java.lang.Object[], boolean)
		 */
		@Override
		public void setValueListItems(String name, Object[] displayValues, Object[] realValues, boolean autoconvert)
		{


		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IApplication#reportJSWarning(java.lang.String)
		 */
		@Override
		public void reportJSWarning(String msg)
		{


		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IApplication#reportJSWarning(java.lang.String, java.lang.Throwable)
		 */
		@Override
		public void reportJSWarning(String msg, Throwable t)
		{


		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IApplication#reportJSInfo(java.lang.String)
		 */
		@Override
		public void reportJSInfo(String msg)
		{


		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IApplication#blockGUII18NMessage(java.lang.String, java.lang.Object[])
		 */
		@Override
		public void blockGUII18NMessage(String key, Object... args)
		{


		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IBasicApplication#getApplicationServerAccess()
		 */
		@Override
		public IApplicationServerAccess getApplicationServerAccess()
		{

			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IBasicApplication#reportWarningInStatus(java.lang.String)
		 */
		@Override
		public void reportWarningInStatus(String s)
		{


		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.IBasicApplication#loadImage(java.lang.String)
		 */
		@Override
		public ImageIcon loadImage(String name)
		{

			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.util.IUIBlocker#blockGUI(java.lang.String)
		 */
		@Override
		public void blockGUI(String reason)
		{


		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.util.IUIBlocker#releaseGUI()
		 */
		@Override
		public void releaseGUI()
		{


		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.server.ngclient.INGApplication#getWebsocketSession()
		 */
		@Override
		public INGClientWebsocketSession getWebsocketSession()
		{

			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.server.ngclient.INGApplication#getChangeListener()
		 */
		@Override
		public IChangeListener getChangeListener()
		{

			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.server.ngclient.INGApplication#getFormManager()
		 */
		@Override
		public INGFormManager getFormManager()
		{

			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.server.ngclient.INGApplication#getRuntimeWindowManager()
		 */
		@Override
		public NGRuntimeWindowManager getRuntimeWindowManager()
		{

			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.server.ngclient.INGApplication#changesWillBeSend()
		 */
		@Override
		public void changesWillBeSend()
		{


		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.server.ngclient.INGApplication#onStartSubAction(java.lang.String, java.lang.String,
		 * org.sablo.specification.WebObjectFunctionDefinition, java.lang.Object[])
		 */
		@Override
		public Pair<Integer, Integer> onStartSubAction(String serviceName, String functionName, WebObjectFunctionDefinition apiFunction, Object[] arguments)
		{

			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.server.ngclient.INGApplication#onStopSubAction(com.servoy.j2db.util.Pair)
		 */
		@Override
		public void onStopSubAction(Pair<Integer, Integer> perfId)
		{


		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.server.ngclient.INGApplication#updateLastAccessed()
		 */
		@Override
		public void updateLastAccessed()
		{


		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.server.ngclient.INGApplication#recreateForm(com.servoy.j2db.persistence.Form, java.lang.String)
		 */
		@Override
		public void recreateForm(Form form, String name)
		{


		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.server.ngclient.INGApplication#flushRecreatedForm(com.servoy.j2db.persistence.Form, java.lang.String)
		 */
		@Override
		public void flushRecreatedForm(Form form, String formName)
		{


		}

		/*
		 * @see com.servoy.j2db.server.ngclient.INGApplication#registerClientFunction(java.lang.String)
		 */
		@Override
		public String registerClientFunction(String code)
		{

			return null;
		}


		@Override
		public Map<String, String> getClientFunctions()
		{

			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.server.ngclient.INGApplication#shutDown(boolean)
		 */
		@Override
		public void shutDown(boolean force)
		{
			// TODO Auto-generated method stub

		}

	}
}
