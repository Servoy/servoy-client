package com.servoy.j2db.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.sql.Types;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.servoy.base.persistence.IBaseColumn;
import com.servoy.base.query.IBaseSQLCondition;
import com.servoy.j2db.Messages;
import com.servoy.j2db.dataprocessing.IFoundSetManagerInternal;
import com.servoy.j2db.query.ColumnType;
import com.servoy.j2db.util.UUID;

/**
 * SVY-21511 - Accept a UUID array key type against a native-UUID (MEDIA + UUID_COLUMN) foreign column in relation key-type validation.
 *
 * <p>Exercises the real {@link Relation#checkKeyTypes(IDataProviderHandler)} array branch by building a live {@link Relation} with
 * {@link RelationItem} children, a stub {@link IDataProviderHandler} that resolves the primary scope variable and the foreign
 * {@link Column}, and asserting the accept/reject decisions for the operators {@code =}/{@code !}/{@code in}. Also covers the pure
 * {@link ArgumentType#valueOf(String)} constant lookup for {@code Array<UUID>}.</p>
 *
 * @author generated
 */
public class RelationUuidArrayKeyTypeTest
{
	private static final String PRIMARY_DS = "db:/testserver/primarytable";
	private static final String FOREIGN_DS = "db:/testserver/foreigntable";
	private static final String SCOPE_VAR = "scopes.jobs.applicationJobs";
	private static final String FOREIGN_COLUMN = "job_uuid";

	/**
	 * Builds a Relation with a single RelationItem: scope var {@code SCOPE_VAR} {op} {@code FOREIGN_COLUMN}.
	 * The primary variable is a MEDIA {@link ScriptVariable} carrying the given {@code @type} runtime property (may be null).
	 * The foreign column is created with the given sql type and optionally flagged as a native UUID column.
	 */
	private static String runCheck(int operator, String primaryTypeProperty, int foreignSqlType, boolean foreignUuidFlag) throws RepositoryException
	{
		DummySolution solution = newSolution();
		Relation relation = (Relation)solution.getChangeHandler().createNewObject(solution, IRepository.RELATIONS);
		relation.setName("globals_to_build$buildingorqueued$application");
		relation.setPrimaryDataSource(PRIMARY_DS);
		relation.setForeignDataSource(FOREIGN_DS);

		RelationItem item = (RelationItem)solution.getChangeHandler().createNewObject(relation, IRepository.RELATION_ITEMS);
		item.setPrimaryDataProviderID(SCOPE_VAR);
		item.setForeignColumnName(FOREIGN_COLUMN);
		item.setOperator(operator);
		relation.internalAddChild(item);

		ScriptVariable primary = (ScriptVariable)solution.getChangeHandler().createNewObject(solution, IRepository.SCRIPTVARIABLES);
		primary.setName("applicationJobs");
		primary.setVariableType(IColumnTypes.MEDIA);
		if (primaryTypeProperty != null)
		{
			primary.setSerializableRuntimeProperty(IScriptProvider.TYPE, primaryTypeProperty);
		}

		Table foreignTable = new Table("testserver", "foreigntable", true, ITable.TABLE, null, null);
		Column foreignColumn = new Column(foreignTable, FOREIGN_COLUMN, ColumnType.getInstance(foreignSqlType, 0, 0), true);
		if (foreignUuidFlag)
		{
			foreignColumn.setFlag(IBaseColumn.UUID_COLUMN, true);
		}
		foreignTable.addColumn(foreignColumn);

		StubDataProviderHandler handler = new StubDataProviderHandler(primary, foreignTable);
		return relation.checkKeyTypes(handler);
	}

	@Nested
	class UuidArrayAgainstNativeUuidColumn
	{
		@Test
		@DisplayName("AC1: Array<UUID> primary vs native-UUID column with 'in' is accepted")
		void arrayUuidInAccepted() throws RepositoryException
		{
			assertNull(runCheck(IBaseSQLCondition.IN_OPERATOR, "Array<UUID>", Types.VARBINARY, true));
		}

		@Test
		@DisplayName("AC1: Array<UUID> primary vs native-UUID column with '=' is accepted")
		void arrayUuidEqualsAccepted() throws RepositoryException
		{
			assertNull(runCheck(IBaseSQLCondition.EQUALS_OPERATOR, "Array<UUID>", Types.VARBINARY, true));
		}

		@Test
		@DisplayName("AC1: Array<UUID> primary vs native-UUID column with '!' is accepted")
		void arrayUuidNotAccepted() throws RepositoryException
		{
			assertNull(runCheck(IBaseSQLCondition.NOT_OPERATOR, "Array<UUID>", Types.VARBINARY, true));
		}

		@Test
		@DisplayName("AC2: Array<String> primary vs native-UUID column is accepted (reporter's original type)")
		void arrayStringAccepted() throws RepositoryException
		{
			assertNull(runCheck(IBaseSQLCondition.IN_OPERATOR, "Array<String>", Types.VARBINARY, true));
		}

		@Test
		@DisplayName("AC3: plain untyped Array primary vs native-UUID column is accepted (permissive path preserved)")
		void plainArrayAccepted() throws RepositoryException
		{
			assertNull(runCheck(IBaseSQLCondition.IN_OPERATOR, "Array", Types.VARBINARY, true));
		}

		@Test
		@DisplayName("AC4: Array<Number> primary vs native-UUID column is still rejected")
		void arrayNumberRejected() throws RepositoryException
		{
			// checkKeyTypes returns a non-null "...does not match..." message string for a mismatch (exact text
			// depends on the resolved message bundle, so only the non-null contract is asserted here).
			assertNotNull(runCheck(IBaseSQLCondition.IN_OPERATOR, "Array<Number>", Types.VARBINARY, true),
				"Array<Number> against a UUID column must remain a mismatch");
		}
	}

	@Nested
	class GateOnNativeUuidColumn
	{
		@Test
		@DisplayName("Guard: Array<UUID> vs plain binary MEDIA column WITHOUT the UUID flag is rejected")
		void arrayUuidAgainstPlainMediaRejected() throws RepositoryException
		{
			assertNotNull(runCheck(IBaseSQLCondition.IN_OPERATOR, "Array<UUID>", Types.VARBINARY, false),
				"the fix must be gated on isUUID(foreign), not on MEDIA alone");
		}

		@Test
		@DisplayName("Guard: Array<String> vs plain binary MEDIA column WITHOUT the UUID flag is rejected")
		void arrayStringAgainstPlainMediaRejected() throws RepositoryException
		{
			assertNotNull(runCheck(IBaseSQLCondition.IN_OPERATOR, "Array<String>", Types.VARBINARY, false),
				"Array<String> is only widened for a genuine native-UUID column");
		}
	}

	@Nested
	class ScalarTextVsUuidStillRejected
	{
		@Test
		@DisplayName("AC5: scalar TEXT primary vs native-UUID column (mem_assign_users_to_users shape) is still rejected")
		void scalarTextVsUuidRejected() throws RepositoryException
		{
			// no array @type property -> falls through the array branch to the plain primaryType != foreignType check.
			// primary MEDIA is used above for arrays; here we need a genuine scalar TEXT primary vs MEDIA foreign.
			String result = runScalar(IColumnTypes.TEXT, Types.VARBINARY, true);
			assertNotNull(result, "scalar TEXT vs native-UUID MEDIA must remain a mismatch");
		}
	}

	@Nested
	class ExistingAcceptancesUnchanged
	{
		@Test
		@DisplayName("AC6: Array<String> primary vs TEXT column is still accepted")
		void arrayStringVsTextAccepted() throws RepositoryException
		{
			assertNull(runCheck(IBaseSQLCondition.IN_OPERATOR, "Array<String>", Types.VARCHAR, false));
		}

		@Test
		@DisplayName("AC6: Array<Number> primary vs NUMBER column is still accepted")
		void arrayNumberVsNumberAccepted() throws RepositoryException
		{
			assertNull(runCheck(IBaseSQLCondition.IN_OPERATOR, "Array<Number>", Types.NUMERIC, false));
		}

		@Test
		@DisplayName("AC6: Array<Number> primary vs INTEGER column is still accepted")
		void arrayNumberVsIntegerAccepted() throws RepositoryException
		{
			assertNull(runCheck(IBaseSQLCondition.IN_OPERATOR, "Array<Number>", Types.INTEGER, false));
		}
	}

	@Nested
	class CalculationUuidType
	{
		@Test
		@DisplayName("SVY-21710: MEDIA calc with '@type {UUID}' vs native-UUID column is accepted")
		void mediaCalcTypeTagAccepted() throws RepositoryException
		{
			assertNull(runCalc(IColumnTypes.MEDIA, calcDeclaration("@type {UUID}"), Types.VARBINARY, true));
		}

		@Test
		@DisplayName("SVY-21710: MEDIA calc with '@return {UUID}' vs native-UUID column is accepted")
		void mediaCalcReturnTagAccepted() throws RepositoryException
		{
			assertNull(runCalc(IColumnTypes.MEDIA, calcDeclaration("@return {UUID}"), Types.VARBINARY, true));
		}

		@Test
		@DisplayName("SVY-21710: MEDIA calc without marker vs native-UUID column is accepted (MEDIA equals MEDIA in the final type check)")
		void mediaCalcWithoutMarkerAccepted() throws RepositoryException
		{
			assertNull(runCalc(IColumnTypes.MEDIA, calcDeclaration(null), Types.VARBINARY, true));
		}

		@Test
		@DisplayName("SVY-21701 regression: TEXT calc without marker vs native-UUID column is still accepted")
		void textCalcWithoutMarkerStillAccepted() throws RepositoryException
		{
			assertNull(runCalc(IColumnTypes.TEXT, calcDeclaration(null), Types.VARBINARY, true));
		}

		@Test
		@DisplayName("SVY-21710: TEXT calc with '@type {UUID}' vs native-UUID column is accepted via the TEXT-calc allowance")
		void textCalcWithTypeTagAccepted() throws RepositoryException
		{
			assertNull(runCalc(IColumnTypes.TEXT, calcDeclaration("@type {UUID}"), Types.VARBINARY, true));
		}

		@Test
		@DisplayName("SVY-21710: MEDIA calc with '@type {UUID}' vs non-UUID MEDIA column is accepted by the plain MEDIA == MEDIA type check (unchanged behaviour)")
		void mediaCalcVsPlainMediaAcceptedByTypeEquality() throws RepositoryException
		{
			// the explicit UUID branch is gated on isUUID(foreign) and is not taken for a plain MEDIA column, but the final
			// primaryType != foreignType check passes because both map to MEDIA (same as for a calc without the marker)
			assertNull(runCalc(IColumnTypes.MEDIA, calcDeclaration("@type {UUID}"), Types.VARBINARY, false));
		}

		@Test
		@DisplayName("SVY-21710: TEXT calc with '@type {UUID}' vs non-UUID MEDIA column is rejected (the TEXT-calc allowance is gated on a native-UUID column)")
		void textCalcWithTagVsPlainMediaRejected() throws RepositoryException
		{
			assertNotNull(runCalc(IColumnTypes.TEXT, calcDeclaration("@type {UUID}"), Types.VARBINARY, false));
		}

		@Test
		@DisplayName("SVY-21710: MEDIA calc with '@type {UUID}' vs a VARCHAR (TEXT) non-UUID column is rejected with the type-mismatch message")
		void mediaUuidCalcVsTextColumnRejected() throws RepositoryException
		{
			assertTypeMismatch(runCalc(IColumnTypes.MEDIA, calcDeclaration("@type {UUID}"), Types.VARCHAR, false), IColumnTypes.TEXT);
		}

		@Test
		@DisplayName("SVY-21710: MEDIA calc with '@type {UUID}' vs an INTEGER column is rejected with the type-mismatch message")
		void mediaUuidCalcVsIntegerColumnRejected() throws RepositoryException
		{
			assertTypeMismatch(runCalc(IColumnTypes.MEDIA, calcDeclaration("@type {UUID}"), Types.INTEGER, false), IColumnTypes.INTEGER);
		}

		@Test
		@DisplayName("SVY-21710: MEDIA calc with '@returns {UUID}' vs native-UUID column is accepted")
		void mediaCalcReturnsTagAccepted() throws RepositoryException
		{
			assertNull(runCalc(IColumnTypes.MEDIA, calcDeclaration("@returns {UUID}"), Types.VARBINARY, true));
		}

		@Test
		@DisplayName("SVY-21710: ScriptCalculation.getFlags has UUID_COLUMN for '@returns {UUID}'")
		void flagsForReturnsTag() throws RepositoryException
		{
			assertEquals(IBaseColumn.NORMAL_COLUMN | IBaseColumn.UUID_COLUMN,
				newCalc(newSolution(), IColumnTypes.MEDIA, calcDeclaration("@returns {UUID}")).getFlags());
		}

		@Test
		@DisplayName("SVY-21710: INTEGER calc vs native-UUID column is rejected")
		void integerCalcVsUuidRejected() throws RepositoryException
		{
			assertNotNull(runCalc(IColumnTypes.INTEGER, calcDeclaration(null), Types.VARBINARY, true));
		}

		@Test
		@DisplayName("SVY-21710: INTEGER calc with '@type {UUID}' vs native-UUID column is rejected")
		void integerCalcWithTypeTagRejected() throws RepositoryException
		{
			assertNotNull(runCalc(IColumnTypes.INTEGER, calcDeclaration("@type {UUID}"), Types.VARBINARY, true));
		}

		@Test
		@DisplayName("SVY-21710: isUUID is true for a MEDIA calc with '@type {UUID}'")
		void isUuidTrueForMediaCalcWithTag() throws Exception
		{
			assertTrue(invokeIsUuid(IColumnTypes.MEDIA, calcDeclaration("@type {UUID}")));
		}

		@Test
		@DisplayName("SVY-21710: isUUID is true for a MEDIA calc with '@return {UUID}'")
		void isUuidTrueForMediaCalcWithReturnTag() throws Exception
		{
			assertTrue(invokeIsUuid(IColumnTypes.MEDIA, calcDeclaration("@return {UUID}")));
		}

		@Test
		@DisplayName("SVY-21710: isUUID is false for a MEDIA calc without marker")
		void isUuidFalseForMediaCalcWithoutTag() throws Exception
		{
			assertFalse(invokeIsUuid(IColumnTypes.MEDIA, calcDeclaration(null)));
		}

		@Test
		@DisplayName("SVY-21710: isUUID is false for a TEXT calc with a stray '@type {UUID}' (MEDIA guard)")
		void isUuidFalseForTextCalcWithTag() throws Exception
		{
			assertFalse(invokeIsUuid(IColumnTypes.TEXT, calcDeclaration("@type {UUID}")));
		}

		@Test
		@DisplayName("SVY-21710: ScriptCalculation.getFlags has UUID_COLUMN for '@type {UUID}'")
		void flagsForTypeTag() throws RepositoryException
		{
			assertEquals(IBaseColumn.NORMAL_COLUMN | IBaseColumn.UUID_COLUMN,
				newCalc(newSolution(), IColumnTypes.MEDIA, calcDeclaration("@type {UUID}")).getFlags());
		}

		@Test
		@DisplayName("SVY-21710: ScriptCalculation.getFlags has UUID_COLUMN for '@return {UUID}'")
		void flagsForReturnTag() throws RepositoryException
		{
			assertEquals(IBaseColumn.NORMAL_COLUMN | IBaseColumn.UUID_COLUMN,
				newCalc(newSolution(), IColumnTypes.MEDIA, calcDeclaration("@return {UUID}")).getFlags());
		}

		@Test
		@DisplayName("SVY-21710: ScriptCalculation.getFlags is NORMAL_COLUMN for '@return {String}'")
		void flagsForOtherReturnTag() throws RepositoryException
		{
			assertEquals(IBaseColumn.NORMAL_COLUMN, newCalc(newSolution(), IColumnTypes.TEXT, calcDeclaration("@return {String}")).getFlags());
		}

		@Test
		@DisplayName("SVY-21710: ScriptCalculation.getFlags is NORMAL_COLUMN without any marker")
		void flagsWithoutMarker() throws RepositoryException
		{
			assertEquals(IBaseColumn.NORMAL_COLUMN, newCalc(newSolution(), IColumnTypes.MEDIA, calcDeclaration(null)).getFlags());
		}

		@Test
		@DisplayName("SVY-21710: ScriptCalculation.getFlags ignores a '{UUID}' return tag that is outside the first JSDoc block")
		void flagsIgnoresReturnTagOutsideFirstBlock() throws RepositoryException
		{
			String declaration = "/**\n * some text\n */\nfunction calc_uuid() {\n\t/** @return {UUID} */\n\treturn null;\n}";
			assertEquals(IBaseColumn.NORMAL_COLUMN, newCalc(newSolution(), IColumnTypes.MEDIA, declaration).getFlags());
		}

		@Test
		@DisplayName("SVY-21710: ScriptVariable.getFlags handles '@type {UUID}' as the last line without trailing newline")
		void scriptVariableFlagsWithoutTrailingNewline()
		{
			assertEquals(IBaseColumn.NORMAL_COLUMN | IBaseColumn.UUID_COLUMN, ScriptVariable.getFlags("/** @type {UUID}*/"));
			assertEquals(IBaseColumn.NORMAL_COLUMN | IBaseColumn.UUID_COLUMN, ScriptVariable.getFlags("/**\n * @type {UUID}*/"));
			assertEquals(IBaseColumn.NORMAL_COLUMN | IBaseColumn.UUID_COLUMN, ScriptVariable.getFlags("/**\n * @type {UUID} */"));
		}

		@Test
		@DisplayName("SVY-21710: ScriptVariable.getFlags is NORMAL_COLUMN for null and for a non-UUID type")
		void scriptVariableFlagsNonUuid()
		{
			assertEquals(IBaseColumn.NORMAL_COLUMN, ScriptVariable.getFlags(null));
			assertEquals(IBaseColumn.NORMAL_COLUMN, ScriptVariable.getFlags("/**\n * @type {String}*/"));
		}
	}

	/**
	 * Asserts the result is the 'servoy.relation.error.typeDoesntMatch' message (names both key data providers), not the generic
	 * 'servoy.relation.error' that is returned when a data provider cannot be resolved.
	 */
	private static void assertTypeMismatch(String message, int expectedForeignType)
	{
		assertNotNull(message);
		assertNotEquals(Messages.getString("servoy.relation.error"), message);
		assertEquals(Messages.getString("servoy.relation.error.typeDoesntMatch",
			new Object[] { "calc_uuid (" + Column.getDisplayTypeString(IColumnTypes.MEDIA) + ")", "job_uuid (" +
				Column.getDisplayTypeString(expectedForeignType) + ")" }),
			message);
	}

	private static String calcDeclaration(String tagLine)
	{
		return "/**\n" + (tagLine != null ? " * " + tagLine + "\n *\n" : "") +
			" * @properties={type:-4,typeid:36,uuid:\"2D0B5A0C-8E1B-4C55-9A53-7C7B2E3D1A11\"}\n */\n" +
			"function calc_uuid() {\n\treturn null;\n}";
	}

	private static ScriptCalculation newCalc(DummySolution solution, int calcType, String declaration) throws RepositoryException
	{
		TableNode tableNode = (TableNode)solution.getChangeHandler().createNewObject(solution, IRepository.TABLENODES);
		tableNode.setDataSource(PRIMARY_DS);
		tableNode.setTable(new Table("testserver", "primarytable", true, ITable.TABLE, null, null));
		ScriptCalculation calc = (ScriptCalculation)solution.getChangeHandler().createNewObject(tableNode, IRepository.SCRIPTCALCULATIONS);
		calc.setName("calc_uuid");
		calc.setType(calcType);
		calc.setDeclaration(declaration);
		return calc;
	}

	/**
	 * Variant of {@link #runCheck} where the primary is an unstored {@link ScriptCalculation} of the given type and declaration,
	 * resolved through {@link IDataProviderHandler#getDataProviderForTable(ITable, String)}.
	 */
	private static String runCalc(int calcType, String declaration, int foreignSqlType, boolean foreignUuidFlag) throws RepositoryException
	{
		DummySolution solution = newSolution();
		Relation relation = (Relation)solution.getChangeHandler().createNewObject(solution, IRepository.RELATIONS);
		relation.setName("calc_to_foreign");
		relation.setPrimaryDataSource(PRIMARY_DS);
		relation.setForeignDataSource(FOREIGN_DS);

		RelationItem item = (RelationItem)solution.getChangeHandler().createNewObject(relation, IRepository.RELATION_ITEMS);
		item.setPrimaryDataProviderID("calc_uuid");
		item.setForeignColumnName(FOREIGN_COLUMN);
		item.setOperator(IBaseSQLCondition.EQUALS_OPERATOR);
		relation.internalAddChild(item);

		ScriptCalculation calc = newCalc(solution, calcType, declaration);

		Table foreignTable = new Table("testserver", "foreigntable", true, ITable.TABLE, null, null);
		Column foreignColumn = new Column(foreignTable, FOREIGN_COLUMN, ColumnType.getInstance(foreignSqlType, 0, 0), true);
		if (foreignUuidFlag)
		{
			foreignColumn.setFlag(IBaseColumn.UUID_COLUMN, true);
		}
		foreignTable.addColumn(foreignColumn);

		return relation.checkKeyTypes(new StubDataProviderHandler(null, foreignTable, calc));
	}

	/**
	 * Calls the private {@code Relation.isUUID(IDataProvider)} on a calculation of the given type and declaration.
	 */
	private static boolean invokeIsUuid(int calcType, String declaration) throws Exception
	{
		DummySolution solution = newSolution();
		Relation relation = (Relation)solution.getChangeHandler().createNewObject(solution, IRepository.RELATIONS);
		ScriptCalculation calc = newCalc(solution, calcType, declaration);
		Method isUuid = Relation.class.getDeclaredMethod("isUUID", IDataProvider.class);
		isUuid.setAccessible(true);
		return ((Boolean)isUuid.invoke(relation, calc)).booleanValue();
	}

	@Nested
	class ArgumentTypeConstant
	{
		@Test
		@DisplayName("AC7: ArgumentType.valueOf(\"Array<UUID>\") returns the ArrayUUID singleton")
		void valueOfArrayUuidReturnsSingleton()
		{
			assertSame(ArgumentType.ArrayUUID, ArgumentType.valueOf("Array<UUID>"));
		}

		@Test
		@DisplayName("AC7: ArgumentType.valueOf is case-insensitive for Array<UUID>")
		void valueOfArrayUuidCaseInsensitive()
		{
			assertSame(ArgumentType.ArrayUUID, ArgumentType.valueOf("array<uuid>"));
		}
	}

	/**
	 * Variant of {@link #runCheck} for a scalar (non-array) primary: the primary is a ScriptVariable of the given column type with
	 * NO {@code @type} array runtime property, so the array branch is skipped and the plain primaryType != foreignType path applies.
	 */
	private static String runScalar(int primaryColumnType, int foreignSqlType, boolean foreignUuidFlag) throws RepositoryException
	{
		DummySolution solution = newSolution();
		Relation relation = (Relation)solution.getChangeHandler().createNewObject(solution, IRepository.RELATIONS);
		relation.setName("mem_assign_users_to_users");
		relation.setPrimaryDataSource(PRIMARY_DS);
		relation.setForeignDataSource(FOREIGN_DS);

		RelationItem item = (RelationItem)solution.getChangeHandler().createNewObject(relation, IRepository.RELATION_ITEMS);
		item.setPrimaryDataProviderID(SCOPE_VAR);
		item.setForeignColumnName(FOREIGN_COLUMN);
		item.setOperator(IBaseSQLCondition.EQUALS_OPERATOR);
		relation.internalAddChild(item);

		ScriptVariable primary = (ScriptVariable)solution.getChangeHandler().createNewObject(solution, IRepository.SCRIPTVARIABLES);
		primary.setName("user_uuid");
		primary.setVariableType(primaryColumnType);

		Table foreignTable = new Table("testserver", "foreigntable", true, ITable.TABLE, null, null);
		Column foreignColumn = new Column(foreignTable, FOREIGN_COLUMN, ColumnType.getInstance(foreignSqlType, 0, 0), true);
		if (foreignUuidFlag)
		{
			foreignColumn.setFlag(IBaseColumn.UUID_COLUMN, true);
		}
		foreignTable.addColumn(foreignColumn);

		StubDataProviderHandler handler = new StubDataProviderHandler(primary, foreignTable);
		return relation.checkKeyTypes(handler);
	}

	/**
	 * Minimal root object so the Relation / RelationItem / ScriptVariable have a valid parent chain without a live repository.
	 */
	/**
	 * Creates a solution wired with a {@link ChangeHandler} so that Relation/RelationItem/ScriptVariable
	 * are instantiated through the {@link AbstractPersistFactory} rather than via their package-private
	 * constructors. Direct {@code new Relation(...)} from this test fragment triggers an
	 * {@link IllegalAccessError} under real OSGi (Tycho/Jenkins) because the fragment and servoy_shared
	 * resolve {@code com.servoy.j2db.persistence} through different Equinox class loaders, so the
	 * package-private constructor is not accessible. Going through the factory (public API) avoids that.
	 */
	private static DummySolution newSolution()
	{
		DummySolution solution = new DummySolution();
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
		return solution;
	}

	private static final class DummySolution extends AbstractRootObject implements ISupportChilds
	{
		DummySolution()
		{
			super(null, new RootObjectMetaData(UUID.randomUUID(), "testSolution", IRepository.SOLUTIONS, 0, 0));
		}
	}

	/**
	 * Minimal handler that resolves the single scope-var primary and the single foreign table used by these tests.
	 * The foreign data source is a db:/ URI so {@link Relation#isDbServer()} is true and the non-per-handler cache is used.
	 */
	private static final class StubDataProviderHandler implements IDataProviderHandler
	{
		private final IDataProvider globalProvider;
		private final ITable foreignTable;
		private final IDataProvider tableDataProvider;

		StubDataProviderHandler(IDataProvider globalProvider, ITable foreignTable)
		{
			this(globalProvider, foreignTable, null);
		}

		/**
		 * @param tableDataProvider the data provider (e.g. a calculation) returned for the primary table, null for none
		 */
		StubDataProviderHandler(IDataProvider globalProvider, ITable foreignTable, IDataProvider tableDataProvider)
		{
			this.globalProvider = globalProvider;
			this.foreignTable = foreignTable;
			this.tableDataProvider = tableDataProvider;
		}

		@Override
		public IDataProviderLookup getDataproviderLookup(IFoundSetManagerInternal foundSetManager, IPersist p)
		{
			return null;
		}

		@Override
		public IDataProvider getDataProviderForTable(ITable table, String dataProviderID)
		{
			return tableDataProvider;
		}

		@Override
		public IDataProvider getGlobalDataProvider(String id)
		{
			return globalProvider;
		}

		@Override
		public Map getAllDataProvidersForTable(ITable table)
		{
			return null;
		}

		@Override
		public ITable getTable(String dataSource)
		{
			return foreignTable;
		}

		@Override
		public IServer getServer(String dataSource)
		{
			return null;
		}
	}
}
