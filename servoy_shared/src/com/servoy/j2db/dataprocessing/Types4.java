package com.servoy.j2db.dataprocessing;

/**
 * SQL types added in JDBC 4.0 (java 1.6).
 * 
 * @author rgansevles
 *
 */
public class Types4
{
	/**
	 * The constant in the Java programming language, sometimes referred to
	 * as a type code, that identifies the generic SQL type <code>ROWID</code>
	 * 
	 * @since 1.6
	 *
	 */
	public final static int ROWID = -8;

	/**
	 * The constant in the Java programming language, sometimes referred to
	 * as a type code, that identifies the generic SQL type <code>NCHAR</code>
	 *
	 * @since 1.6
	 */
	public static final int NCHAR = -15;

	/**
	 * The constant in the Java programming language, sometimes referred to
	 * as a type code, that identifies the generic SQL type <code>NVARCHAR</code>.
	 *
	 * @since 1.6
	 */
	public static final int NVARCHAR = -9;

	/**
	 * The constant in the Java programming language, sometimes referred to
	 * as a type code, that identifies the generic SQL type <code>LONGNVARCHAR</code>.
	 *
	 * @since 1.6
	 */
	public static final int LONGNVARCHAR = -16;

	/**
	 * The constant in the Java programming language, sometimes referred to
	 * as a type code, that identifies the generic SQL type <code>NCLOB</code>.
	 *
	 * @since 1.6
	 */
	public static final int NCLOB = 2011;

	/**
	 * The constant in the Java programming language, sometimes referred to
	 * as a type code, that identifies the generic SQL type <code>XML</code>.
	 *
	 * @since 1.6 
	 */
	public static final int SQLXML = 2009;

}
