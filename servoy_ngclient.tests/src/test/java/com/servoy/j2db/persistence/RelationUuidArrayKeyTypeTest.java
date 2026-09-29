package com.servoy.j2db.persistence;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.sql.Types;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.servoy.base.persistence.IBaseColumn;
import com.servoy.base.query.IBaseSQLCondition;
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

		StubDataProviderHandler(IDataProvider globalProvider, ITable foreignTable)
		{
			this.globalProvider = globalProvider;
			this.foreignTable = foreignTable;
		}

		@Override
		public IDataProviderLookup getDataproviderLookup(IFoundSetManagerInternal foundSetManager, IPersist p)
		{
			return null;
		}

		@Override
		public IDataProvider getDataProviderForTable(ITable table, String dataProviderID)
		{
			return null;
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
