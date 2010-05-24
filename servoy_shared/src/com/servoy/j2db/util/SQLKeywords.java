/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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
package com.servoy.j2db.util;


/**
 * @author jblok
 */
public class SQLKeywords
{
	//SQL Related
	public final static String[] keywords = new String[] { //

	// SQL related
	"truncate", //$NON-NLS-1$
	"type", //$NON-NLS-1$
	"number", //$NON-NLS-1$
	"index", //$NON-NLS-1$
	"modify", //$NON-NLS-1$
	"cobol", //$NON-NLS-1$ 
	"fortran", //$NON-NLS-1$
	"pascal", //$NON-NLS-1$
	"pl1", //$NON-NLS-1$
	"system", //$NON-NLS-1$
	"password", //$NON-NLS-1$
	"uuid", //$NON-NLS-1$
	"version",//problem for sybase //$NON-NLS-1$
	"release",//problem for sybase //$NON-NLS-1$

	//begin, hypersonic small list special words
	"cached", //$NON-NLS-1$
	"datetime", //$NON-NLS-1$
	"limit", //$NON-NLS-1$
	"longvarbinary", //$NON-NLS-1$
	"longvarchar", //$NON-NLS-1$
	"object", //$NON-NLS-1$
	"other", //$NON-NLS-1$
	"temp", //$NON-NLS-1$
	"text", //$NON-NLS-1$
	"varchar_ignorecase", //$NON-NLS-1$
	//end, hypersonic small list special words
	//official ansi SQL 92 reserved list
	"absolute", //$NON-NLS-1$
	"action", //$NON-NLS-1$
	"add", //$NON-NLS-1$
	"all", //$NON-NLS-1$
	"allocate", //$NON-NLS-1$
	"alter", //$NON-NLS-1$
	"and", //$NON-NLS-1$
	"any", //$NON-NLS-1$
	"are", //$NON-NLS-1$
	"as", //$NON-NLS-1$
	"asc", //$NON-NLS-1$
	"assertion", //$NON-NLS-1$
	"at", //$NON-NLS-1$
	"authorization", //$NON-NLS-1$
	"avg", //$NON-NLS-1$
	"begin", //$NON-NLS-1$
	"between", //$NON-NLS-1$
	"bit", //$NON-NLS-1$
	"bit_length", //$NON-NLS-1$
	"both", //$NON-NLS-1$
	"by", //$NON-NLS-1$
	"cascade", //$NON-NLS-1$
	"cascaded", //$NON-NLS-1$
	"case", //$NON-NLS-1$
	"cast", //$NON-NLS-1$
	"catalog", //$NON-NLS-1$
	"char", //$NON-NLS-1$
	"character", //$NON-NLS-1$
	"char_length", //$NON-NLS-1$
	"character_length", //$NON-NLS-1$
	"check", //$NON-NLS-1$
	"close", //$NON-NLS-1$
	"coalesce", //$NON-NLS-1$
	"collate", //$NON-NLS-1$
	"collation", //$NON-NLS-1$
	"column", //$NON-NLS-1$
	"commit", //$NON-NLS-1$
	"connect", //$NON-NLS-1$
	"connection", //$NON-NLS-1$
	"constraint", //$NON-NLS-1$
	"constraints", //$NON-NLS-1$
	"continue", //$NON-NLS-1$
	"convert", //$NON-NLS-1$
	"corresponding", //$NON-NLS-1$
	"count", //$NON-NLS-1$
	"create", //$NON-NLS-1$
	"cross", //$NON-NLS-1$
	"current", //$NON-NLS-1$
	"current_date", //$NON-NLS-1$
	"current_time", //$NON-NLS-1$
	"current_timestamp", //$NON-NLS-1$
	"current_user", //$NON-NLS-1$
	"cursor", //$NON-NLS-1$
	"date", //$NON-NLS-1$
	"day", //$NON-NLS-1$
	"deallocate", //$NON-NLS-1$
	"dec", //$NON-NLS-1$
	"decimal", //$NON-NLS-1$
	"declare", //$NON-NLS-1$
	"default", //$NON-NLS-1$
	"deferrable", //$NON-NLS-1$
	"deferred", //$NON-NLS-1$
	"delete", //$NON-NLS-1$
	"desc", //$NON-NLS-1$
	"describe", //$NON-NLS-1$
	"descriptor", //$NON-NLS-1$
	"diagnostics", //$NON-NLS-1$
	"disconnect", //$NON-NLS-1$
	"distinct", //$NON-NLS-1$
	"domain", //$NON-NLS-1$
	"double", //$NON-NLS-1$
	"drop", //$NON-NLS-1$
	"else", //$NON-NLS-1$
	"end", //$NON-NLS-1$
	"end-exec", //$NON-NLS-1$
	"escape", //$NON-NLS-1$
	"except", //$NON-NLS-1$
	"exception", //$NON-NLS-1$
	"exec", //$NON-NLS-1$
	"execute", //$NON-NLS-1$
	"exists", //$NON-NLS-1$
	"external", //$NON-NLS-1$
	"extract", //$NON-NLS-1$
	"false", //$NON-NLS-1$
	"fetch", //$NON-NLS-1$
	"first", //$NON-NLS-1$
	"float", //$NON-NLS-1$
	"for", //$NON-NLS-1$
	"foreign", //$NON-NLS-1$
	"found", //$NON-NLS-1$
	"from", //$NON-NLS-1$
	"full", //$NON-NLS-1$
	"get", //$NON-NLS-1$
	"global", //$NON-NLS-1$
	"go", //$NON-NLS-1$
	"goto", //$NON-NLS-1$
	"grant", //$NON-NLS-1$
	"group", //$NON-NLS-1$
	"having", //$NON-NLS-1$
	"hour", //$NON-NLS-1$
	"identity", //$NON-NLS-1$
	"immediate", //$NON-NLS-1$
	"in", //$NON-NLS-1$
	"indicator", //$NON-NLS-1$
	"initially", //$NON-NLS-1$
	"inner", //$NON-NLS-1$
	"input", //$NON-NLS-1$
	"insensitive", //$NON-NLS-1$
	"insert", //$NON-NLS-1$
	"int", //$NON-NLS-1$
	"integer", //$NON-NLS-1$
	"intersect", //$NON-NLS-1$
	"interval", //$NON-NLS-1$
	"into", //$NON-NLS-1$
	"is", //$NON-NLS-1$
	"isolation", //$NON-NLS-1$
	"join", //$NON-NLS-1$
	"key", //$NON-NLS-1$
	"language", //$NON-NLS-1$
	"last", //$NON-NLS-1$
	"leading", //$NON-NLS-1$
	"left", //$NON-NLS-1$
	"level", //$NON-NLS-1$
	"like", //$NON-NLS-1$
	"local", //$NON-NLS-1$
	"lower", //$NON-NLS-1$
	"match", //$NON-NLS-1$
	"max", //$NON-NLS-1$
	"min", //$NON-NLS-1$
	"minute", //$NON-NLS-1$
	"module", //$NON-NLS-1$
	"month", //$NON-NLS-1$
	"names", //$NON-NLS-1$
	"national", //$NON-NLS-1$
	"natural", //$NON-NLS-1$
	"nchar", //$NON-NLS-1$
	"next", //$NON-NLS-1$
	"no", //$NON-NLS-1$
	"not", //$NON-NLS-1$
	"null", //$NON-NLS-1$
	"nullif", //$NON-NLS-1$
	"numeric", //$NON-NLS-1$
	"octet_length", //$NON-NLS-1$
	"of", //$NON-NLS-1$
	"on", //$NON-NLS-1$
	"only", //$NON-NLS-1$
	"open", //$NON-NLS-1$
	"option", //$NON-NLS-1$
	"or", //$NON-NLS-1$
	"order", //$NON-NLS-1$
	"outer", //$NON-NLS-1$
	"output", //$NON-NLS-1$
	"overlaps", //$NON-NLS-1$
	"pad", //$NON-NLS-1$
	"partial", //$NON-NLS-1$
	"position", //$NON-NLS-1$
	"precision", //$NON-NLS-1$
	"prepare", //$NON-NLS-1$
	"preserve", //$NON-NLS-1$
	"primary", //$NON-NLS-1$
	"prior", //$NON-NLS-1$
	"privileges", //$NON-NLS-1$
	"procedure", //$NON-NLS-1$
	"public", //$NON-NLS-1$
	"read", //$NON-NLS-1$
	"real", //$NON-NLS-1$
	"references", //$NON-NLS-1$
	"relative", //$NON-NLS-1$
	"restrict", //$NON-NLS-1$
	"revoke", //$NON-NLS-1$
	"right", //$NON-NLS-1$
	"rollback", //$NON-NLS-1$
	"rows", //$NON-NLS-1$
	"schema", //$NON-NLS-1$
	"scroll", //$NON-NLS-1$
	"second", //$NON-NLS-1$
	"section", //$NON-NLS-1$
	"select", //$NON-NLS-1$
	"session", //$NON-NLS-1$
	"session_user", //$NON-NLS-1$
	"set", //$NON-NLS-1$
	"show",//$NON-NLS-1$
	"size", //$NON-NLS-1$
	"smallint", //$NON-NLS-1$
	"some", //$NON-NLS-1$
	"space", //$NON-NLS-1$
	"sql", //$NON-NLS-1$
	"sqlcode", //$NON-NLS-1$
	"sqlerror", //$NON-NLS-1$
	"sqlstate", //$NON-NLS-1$
	"substring", //$NON-NLS-1$
	"sum", //$NON-NLS-1$
	"system_user", //$NON-NLS-1$
	"table", //$NON-NLS-1$
	"temporary", //$NON-NLS-1$
	"then", //$NON-NLS-1$
	"time", //$NON-NLS-1$
	"timestamp", //$NON-NLS-1$
	"timezone_hour", //$NON-NLS-1$
	"timezone_minute", //$NON-NLS-1$
	"to", //$NON-NLS-1$
	"trailing", //$NON-NLS-1$
	"transaction", //$NON-NLS-1$
	"translate", //$NON-NLS-1$
	"translation", //$NON-NLS-1$
	"trim", //$NON-NLS-1$
	"true", //$NON-NLS-1$
	"union", //$NON-NLS-1$
	"unique", //$NON-NLS-1$
	"unknown", //$NON-NLS-1$
	"update", //$NON-NLS-1$
	"upper", //$NON-NLS-1$
	"usage", //$NON-NLS-1$
	"user", //$NON-NLS-1$
	"using", //$NON-NLS-1$
	"value", //$NON-NLS-1$
	"values", //$NON-NLS-1$
	"varchar", //$NON-NLS-1$
	"varying", //$NON-NLS-1$
	"view", //$NON-NLS-1$
	"when", //$NON-NLS-1$
	"whenever", //$NON-NLS-1$
	"where", //$NON-NLS-1$
	"with", //$NON-NLS-1$
	"work", //$NON-NLS-1$
	"write", //$NON-NLS-1$
	"year", //$NON-NLS-1$
	"zone", //$NON-NLS-1$

	//	Firebird
	"active", //$NON-NLS-1$
	"admin", //$NON-NLS-1$
	"after", //$NON-NLS-1$
	"ascending", //$NON-NLS-1$
	"auto", //$NON-NLS-1$
	"base_name", //$NON-NLS-1$
	"before", //$NON-NLS-1$
	"bigint", //$NON-NLS-1$
	"blob", //$NON-NLS-1$
	"break", //$NON-NLS-1$
	"cache", //$NON-NLS-1$
	"check_point_length", //$NON-NLS-1$
	"computed", //$NON-NLS-1$
	"conditional", //$NON-NLS-1$
	"connection_id", //$NON-NLS-1$
	"containing", //$NON-NLS-1$
	"cstring", //$NON-NLS-1$
	"current_role", //$NON-NLS-1$
	"database", //$NON-NLS-1$
	"debug", //$NON-NLS-1$
	"descending", //$NON-NLS-1$
	"do", //$NON-NLS-1$
	"entry_point", //$NON-NLS-1$
	"exit", //$NON-NLS-1$
	"file", //$NON-NLS-1$
	"filter", //$NON-NLS-1$
	"free_it", //$NON-NLS-1$
	"function", //$NON-NLS-1$
	"gdscode", //$NON-NLS-1$
	"generator", //$NON-NLS-1$
	"gen_id", //$NON-NLS-1$
	"group_commit_wait_time", //$NON-NLS-1$
	"if", //$NON-NLS-1$
	"inactive", //$NON-NLS-1$
	"index", //$NON-NLS-1$
	"input_type", //$NON-NLS-1$
	"lock", //$NON-NLS-1$
	"logfile", //$NON-NLS-1$
	"log_buffer_size", //$NON-NLS-1$
	"long", //$NON-NLS-1$
	"manual", //$NON-NLS-1$
	"maximum_segment", //$NON-NLS-1$
	"merge", //$NON-NLS-1$
	"message", //$NON-NLS-1$
	"module_name", //$NON-NLS-1$
	"nulls", //$NON-NLS-1$
	"num_log_buffers", //$NON-NLS-1$
	"output_type", //$NON-NLS-1$
	"overflow", //$NON-NLS-1$
	"page", //$NON-NLS-1$
	"pages", //$NON-NLS-1$
	"page_size", //$NON-NLS-1$
	"parameter", //$NON-NLS-1$
	"password", //$NON-NLS-1$
	"plan", //$NON-NLS-1$
	"post_event", //$NON-NLS-1$
	"protected", //$NON-NLS-1$
	"raw_partitions", //$NON-NLS-1$
	"rdb$db_key", //$NON-NLS-1$
	"record_version", //$NON-NLS-1$
	"recreate", //$NON-NLS-1$
	"reserv", //$NON-NLS-1$
	"reserving", //$NON-NLS-1$
	"retain", //$NON-NLS-1$
	"returning_values", //$NON-NLS-1$
	"returns", //$NON-NLS-1$
	"role", //$NON-NLS-1$
	"rows_affected", //$NON-NLS-1$
	"savepoint", //$NON-NLS-1$
	"segment", //$NON-NLS-1$
	"shadow", //$NON-NLS-1$
	"shared", //$NON-NLS-1$
	"singular", //$NON-NLS-1$
	"skip", //$NON-NLS-1$
	"snapshot", //$NON-NLS-1$
	"sort", //$NON-NLS-1$
	"stability", //$NON-NLS-1$
	"starting", //$NON-NLS-1$
	"starts", //$NON-NLS-1$
	"statistics", //$NON-NLS-1$
	"sub_type", //$NON-NLS-1$
	"suspend", //$NON-NLS-1$
	"transaction_id", //$NON-NLS-1$
	"trigger", //$NON-NLS-1$
	"variable", //$NON-NLS-1$
	"wait", //$NON-NLS-1$
	"weekday", //$NON-NLS-1$
	"while", //$NON-NLS-1$
	"yearday", //$NON-NLS-1$
	//firebird 1.5
	"current_connection", //$NON-NLS-1$
	"current_transaction", //$NON-NLS-1$
	"row_count", //$NON-NLS-1$
	"abs", //$NON-NLS-1$
	"boolean", //$NON-NLS-1$
	"skip", //$NON-NLS-1$ 
	"structural", //$NON-NLS-1$ 
	"deleting", //$NON-NLS-1$
	"inserting", //$NON-NLS-1$
	"leave", //$NON-NLS-1$
	"statement", //$NON-NLS-1$
	"updating", //$NON-NLS-1$
	"percent", //$NON-NLS-1$
	"temporary", //$NON-NLS-1$
	"ties", //$NON-NLS-1$

	// Sybase ASA
	"backup", //$NON-NLS-1$
	"bigint", //$NON-NLS-1$
	"binary", //$NON-NLS-1$
	"bottom", //$NON-NLS-1$
	"break", //$NON-NLS-1$
	"call", //$NON-NLS-1$
	"capability", //$NON-NLS-1$
	"char_convert", //$NON-NLS-1$
	"checkpoint", //$NON-NLS-1$
	"comment", //$NON-NLS-1$
	"compressed", //$NON-NLS-1$
	"contains", //$NON-NLS-1$
	"cube", //$NON-NLS-1$
	"dbspace", //$NON-NLS-1$
	"deleting", //$NON-NLS-1$
	"do", //$NON-NLS-1$
	"dynamic", //$NON-NLS-1$
	"elseif", //$NON-NLS-1$
	"encrypted", //$NON-NLS-1$
	"endif", //$NON-NLS-1$
	"existing", //$NON-NLS-1$
	"externlogin", //$NON-NLS-1$
	"forward", //$NON-NLS-1$
	"holdlock", //$NON-NLS-1$
	"identified", //$NON-NLS-1$
	"if", //$NON-NLS-1$
	"index", //$NON-NLS-1$
	"inout", //$NON-NLS-1$
	"inserting", //$NON-NLS-1$
	"install", //$NON-NLS-1$
	"instead", //$NON-NLS-1$
	"integrated", //$NON-NLS-1$
	"iq", //$NON-NLS-1$
	"lock", //$NON-NLS-1$
	"login", //$NON-NLS-1$
	"long", //$NON-NLS-1$
	"membership", //$NON-NLS-1$
	"message", //$NON-NLS-1$
	"mode", //$NON-NLS-1$
	"modify", //$NON-NLS-1$
	"new", //$NON-NLS-1$
	"noholdlock", //$NON-NLS-1$
	"notify", //$NON-NLS-1$
	"off", //$NON-NLS-1$
	"options", //$NON-NLS-1$
	"others", //$NON-NLS-1$
	"out", //$NON-NLS-1$
	"over", //$NON-NLS-1$
	"passthrough", //$NON-NLS-1$
	"print", //$NON-NLS-1$
	"proc", //$NON-NLS-1$
	"publication", //$NON-NLS-1$
	"raiserror", //$NON-NLS-1$
	"readtext", //$NON-NLS-1$
	"reference", //$NON-NLS-1$
	"release", //$NON-NLS-1$
	"remote", //$NON-NLS-1$
	"remove", //$NON-NLS-1$
	"rename", //$NON-NLS-1$
	"reorganize", //$NON-NLS-1$
	"resource", //$NON-NLS-1$
	"restore", //$NON-NLS-1$
	"return", //$NON-NLS-1$
	"rollup", //$NON-NLS-1$
	"save", //$NON-NLS-1$
	"savepoint", //$NON-NLS-1$
	"schedule", //$NON-NLS-1$
	"sensitive", //$NON-NLS-1$
	"setuser", //$NON-NLS-1$
	"share", //$NON-NLS-1$
	"start", //$NON-NLS-1$
	"stop", //$NON-NLS-1$
	"subtrans", //$NON-NLS-1$
	"subtransaction", //$NON-NLS-1$
	"synchronize", //$NON-NLS-1$
	"syntax_error", //$NON-NLS-1$
	"tinyint", //$NON-NLS-1$
	"top", //$NON-NLS-1$
	"tran", //$NON-NLS-1$
	"trigger", //$NON-NLS-1$
	"truncate", //$NON-NLS-1$
	"tsequal", //$NON-NLS-1$
	"unsigned", //$NON-NLS-1$
	"updating", //$NON-NLS-1$
	"validate", //$NON-NLS-1$
	"varbinary", //$NON-NLS-1$
	"variable", //$NON-NLS-1$
	"wait", //$NON-NLS-1$
	"waitfor", //$NON-NLS-1$
	"while", //$NON-NLS-1$
	"with_lparen", //$NON-NLS-1$
	"writetext", //$NON-NLS-1$

	// MySql
	"ignore", //$NON-NLS-1$
	"load", //$NON-NLS-1$

	// PostgreSQL
	"domains", //$NON-NLS-1$

	// Oracle
	"uid", //$NON-NLS-1$
	"length", //$NON-NLS-1$
	};

	public static boolean checkIfKeyword(String name)
	{
		if (name == null) return false;
		String lname = name.trim().toLowerCase();
		for (String kw : keywords)
		{
			if (kw.equals(lname))
			{
				return true;
			}
		}

		return false;
	}
}
