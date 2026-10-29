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

import java.util.Date;

import org.json.JSONException;
import org.json.JSONWriter;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.FormElementContext;
import com.servoy.j2db.server.ngclient.utils.NGUtils;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.ScopesUtils;

/**
 * Fabricates the synthetic sample values used by the stateless form-template render when sample-data mode is on
 * (SVY-21610); see {@link FormElement#hasSampleData()}.
 *
 * <p>
 * <b>Deterministic by contract.</b> The {@code getFormLayout} agent loop is <i>render, change one thing, render
 * again, diff</i>, so two consecutive renders of an unchanged form MUST produce byte-identical values. Every value
 * here is therefore derived from a stable hash of the dataprovider name (plus the row index for grid rows) - never
 * from {@link Math#random()}, a counter or the current time.
 * </p>
 *
 * <p>
 * Resolution order for a dataprovider: a form or scope <b>variable</b> contributes its <i>real</i> initial value
 * ({@link ScriptVariable#getInitValue()}, which is also why an initialiser is preferable to assigning in
 * {@code onLoad} - the initialiser shows up at design time); a dataprovider on a component that also has a
 * <b>valuelist</b> contributes a real value matching the valuelist sample the designer-default hook writes; a
 * <b>column</b> contributes a value fabricated per its column type. Anything unresolvable - and MEDIA, which is out
 * of scope - contributes {@code null}, meaning "write nothing", which is exactly the pre-SVY-21610 behaviour.
 * </p>
 *
 * <p>
 * <b>Not handled here:</b> {@code dataset} and {@code foundsettree} typed properties. Neither
 * {@link DatasetPropertyType} nor the foundset-tree type has any design-time/template projection to extend, so
 * components backed by one (the extra-components treeview / dbtreeview) keep their own placeholder data instead.
 * </p>
 *
 * @author Servoy
 */
public final class SampleDataProvider
{
	/**
	 * Number of synthetic rows a sample foundset reports. Enough to show row striping, selection styling and column
	 * alignment without bloating the generated JSON.
	 *
	 * <p>
	 * <b>Invariant:</b> a sample foundset must report {@code serverSize == viewPort.rows.length == SAMPLE_ROW_COUNT}
	 * and {@code hasMoreRows == false}. If a grid believes more rows exist it calls
	 * {@code foundset.loadExtraRecordsAsync()}, whose promise can never resolve without a websocket session, and the
	 * grid hangs in a loading state.
	 * </p>
	 */
	public static final int SAMPLE_ROW_COUNT = 3;

	/**
	 * Fixed reference instant for DATETIME samples: 2026-01-01T00:00:00Z. A constant, not "now", so renders are
	 * reproducible.
	 */
	private static final long SAMPLE_DATE_MILLIS = 1767225600000L;

	/**
	 * Words used for TEXT samples, picked by a stable hash of the dataprovider name. Readable in a specimen sheet and
	 * short enough not to overflow a default-width field.
	 */
	private static final String[] SAMPLE_WORDS = new String[] { "Sample", "Example", "Lorem", "Ipsum", "Dolor", "Amet", "Consectetur", "Adipiscing", "Elit", "Tempor", "Labore", "Magna" };

	private SampleDataProvider()
	{
	}

	/**
	 * Returns a deterministic sample value for the given dataprovider, or {@code null} when nothing should be written
	 * (unresolvable dataprovider, MEDIA, or no sample-data context).
	 *
	 * @param dataProviderID the design-time dataprovider id, e.g. {@code "orderid"}, {@code "customer_to_orders.freight"} or {@code "globals.myVar"}
	 * @param rowIndex 0-based row index, so a grid's rows can differ from each other; 0 for a plain (non-foundset) field
	 * @param formElementContext the context of the component the property belongs to
	 */
	public static SampleValue getSampleValue(String dataProviderID, int rowIndex, FormElementContext formElementContext)
	{
		if (dataProviderID == null || formElementContext == null) return null;
		FormElement formElement = formElementContext.getFormElement();
		if (formElement == null) return null;

		FlattenedSolution fs = formElementContext.getFlattenedSolution();
		if (fs == null) fs = formElement.getFlattendSolution();
		if (fs == null) return null;

		// 1. a variable contributes its real initial value - no fabrication needed
		ScriptVariable variable = findVariable(dataProviderID, formElement, fs);
		if (variable != null)
		{
			Object initValue = variable.getInitValue();
			// an empty string is what getInitValue() returns for a TEXT variable with no default; fall through to a
			// fabricated sample in that case so the field still shows something
			if (initValue != null && !"".equals(initValue))
			{
				return new SampleValue(initValue, propertyDescriptionFor(Column.mapToDefaultType(variable.getVariableType())));
			}
			return new SampleValue(getSampleValueForType(Column.mapToDefaultType(variable.getVariableType()), dataProviderID, rowIndex),
				propertyDescriptionFor(Column.mapToDefaultType(variable.getVariableType())));
		}

		// 2. a dataprovider backed by a valuelist takes a real value matching the valuelist sample that
		// ValueListPropertyType.toDesignerDefaultJSONValue writes (realValue 1 -> displayValue "Item1")
		if (hasValuelist(formElement))
		{
			return new SampleValue(Integer.valueOf((rowIndex % SAMPLE_ROW_COUNT) + 1), NGUtils.INTEGER_DATAPROVIDER_CACHED_PD);
		}

		// 3. otherwise fabricate per the column's type
		int columnType = getColumnType(dataProviderID, formElement, fs);
		Object value = getSampleValueForType(columnType, dataProviderID, rowIndex);
		return value == null ? null : new SampleValue(value, propertyDescriptionFor(columnType));
	}

	/**
	 * Writes a sample value the same way a real client writes a dataprovider value: through
	 * {@link JSONUtils.FullValueToJSONConverter} with the type's {@link NGUtils} property description, so a value that
	 * needs a client-side conversion (notably a {@link Date}, which must arrive as {@code {_T:"svy_date", v:...}})
	 * carries its type marker and the client's {@code ConverterService} turns it back into a real {@code Date}. A
	 * plain string/number is written as-is.
	 */
	public static JSONWriter writeSampleValue(JSONWriter writer, String key, SampleValue sampleValue) throws JSONException
	{
		if (sampleValue == null || sampleValue.value == null) return writer;
		if (sampleValue.propertyDescription == null)
		{
			JSONUtils.addKeyIfPresent(writer, key);
			return writer.value(sampleValue.value);
		}
		return JSONUtils.FullValueToJSONConverter.INSTANCE.toJSONValue(writer, key, sampleValue.value, sampleValue.propertyDescription, null);
	}

	/** The {@link NGUtils} cached dataprovider property description for a Servoy column type, or {@code null}. */
	private static PropertyDescription propertyDescriptionFor(int columnType)
	{
		return NGUtils.getDataProviderPropertyDescription(columnType, false, false);
	}

	/**
	 * A sample value plus the property description its conversion to the browser needs. Kept together because a
	 * {@link Date} is only usable client-side when written with its client-side type.
	 */
	public static final class SampleValue
	{
		public final Object value;
		public final PropertyDescription propertyDescription;

		SampleValue(Object value, PropertyDescription propertyDescription)
		{
			this.value = value;
			this.propertyDescription = propertyDescription;
		}
	}

	/**
	 * Finds the form or scope variable {@code dataProviderID} refers to, or {@code null} when it is not a variable.
	 * A variable with an initialiser ({@code var x = 'Value';}) therefore contributes its real value to the render;
	 * one assigned only in {@code onLoad} cannot, since {@code onLoad} does not run in a stateless render (SVY-21610).
	 */
	private static ScriptVariable findVariable(String dataProviderID, FormElement formElement, FlattenedSolution fs)
	{
		if (ScopesUtils.isVariableScope(dataProviderID))
		{
			Pair<String, String> scope = ScopesUtils.getVariableScope(dataProviderID);
			return scope != null ? fs.getScriptVariable(scope.getLeft(), scope.getRight()) : null;
		}
		return formElement.getForm() != null ? formElement.getForm().getScriptVariable(dataProviderID) : null;
	}

	/**
	 * Whether the component owning this dataprovider also declares a valuelist property that carries a value, in which
	 * case the displayed text comes from the valuelist rather than from the dataprovider itself.
	 */
	private static boolean hasValuelist(FormElement formElement)
	{
		WebObjectSpecification spec = formElement.getWebComponentSpec(false);
		if (spec == null) return false;
		for (PropertyDescription pd : spec.getProperties(ValueListPropertyType.INSTANCE))
		{
			if (formElement.getRawPropertyValue(pd.getName()) != null) return true;
		}
		return false;
	}

	/**
	 * Resolves the column type of {@code dataProviderID} against the form's datasource, following a relation sequence
	 * for a related dataprovider. Returns {@code -1} when it cannot be resolved (a calculation on an unavailable
	 * table, a typo, an unreachable server).
	 */
	private static int getColumnType(String dataProviderID, FormElement formElement, FlattenedSolution fs)
	{
		try
		{
			String dataSource = formElement.getForm() != null ? formElement.getForm().getDataSource() : null;
			String columnName = dataProviderID;

			int lastDot = dataProviderID.lastIndexOf('.');
			if (lastDot > 0 && lastDot < dataProviderID.length() - 1)
			{
				Relation[] relations = fs.getRelationSequence(dataProviderID.substring(0, lastDot));
				if (relations == null || relations.length == 0) return -1;
				dataSource = relations[relations.length - 1].getForeignDataSource();
				columnName = dataProviderID.substring(lastDot + 1);
			}
			if (dataSource == null) return -1;

			ITable table = fs.getTable(dataSource);
			if (table == null) return -1;

			IDataProvider dataProvider = fs.getDataProviderForTable(table, columnName);
			if (dataProvider == null) return -1;

			// Column.getDataProviderType() already maps the SQL type to a default Servoy type, and ColumnWrapper
			// delegates to it, so this one call covers columns, aggregates and calculations alike. A real client would
			// additionally honour column converters via IFoundSetManager.getConvertedTypeForColumn(...), but this render
			// has no application/foundset manager - and a converter cannot change the shape of a sample value enough to
			// matter for a layout check.
			return dataProvider.getDataProviderType();
		}
		catch (RepositoryException | RuntimeException e)
		{
			// an unresolvable dataprovider must never break the render; it simply gets no sample value
			Debug.trace("SVY-21610: can't resolve the column type of '" + dataProviderID + "' for sample data", e);
			return -1;
		}
	}

	/**
	 * Fabricates the value for a resolved column type. MEDIA and unresolved types deliberately get {@code null}
	 * (sample media is out of scope), which keeps the pre-SVY-21610 "write nothing" behaviour for those.
	 *
	 * <p>
	 * Public as the testable seam of this class: it is the whole of the fabrication logic and needs no
	 * {@code FormElement}, flattened solution or workspace, so the determinism contract can be pinned by a plain
	 * unit test.
	 * </p>
	 *
	 * @param columnType a {@link IColumnTypes} constant, or anything else for "unresolved"
	 * @param dataProviderID seeds the value, so two dataproviders of the same type differ but each is stable
	 * @param rowIndex 0-based row index within a sample foundset viewport
	 */
	public static Object getSampleValueForType(int columnType, String dataProviderID, int rowIndex)
	{
		int seed = stableSeed(dataProviderID, rowIndex);
		switch (columnType)
		{
			case IColumnTypes.TEXT :
				return SAMPLE_WORDS[seed % SAMPLE_WORDS.length];
			case IColumnTypes.INTEGER :
				return Integer.valueOf(seed % 1000);
			case IColumnTypes.NUMBER :
				return Double.valueOf((seed % 100000) / 100d);
			case IColumnTypes.DATETIME :
				// one day apart per row, so a grid column shows distinguishable dates
				return new Date(SAMPLE_DATE_MILLIS + (rowIndex * 86400000L));
			default :
				// MEDIA (out of scope) and anything unresolved
				return null;
		}
	}

	/**
	 * A stable, non-negative seed for a dataprovider/row pair. {@link String#hashCode()} is specified by the Java
	 * language, so this is reproducible across JVMs and restarts - which {@link Object#hashCode()} would not be.
	 */
	private static int stableSeed(String dataProviderID, int rowIndex)
	{
		int hash = dataProviderID.hashCode() + (rowIndex * 31);
		return Math.abs(hash == Integer.MIN_VALUE ? 0 : hash);
	}
}
