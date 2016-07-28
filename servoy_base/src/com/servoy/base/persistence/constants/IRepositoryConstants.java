/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.base.persistence.constants;

/**
 * @author acostescu
 *
 */
public interface IRepositoryConstants
{

	/**
	 * Final repository object_types
	 */
	public static final int RELEASES = 1;
	public static final int BLOBS = 2;
	public static final int FORMS = 3;
	public static final int FIELDS = 4;
	public static final int INSETS = 5;
	public static final int PORTALS = 6;
	public static final int GRAPHICALCOMPONENTS = 7; //button,image,label replacement
	public static final int SERVERS = 8;
	public static final int DATASOURCES = 9;
	public static final int STYLES = 10;
	public static final int TEMPLATES = 11;
	public static final int BEANS = 12;
	public static final int ELEMENTS = 13;
	public static final int ELEMENT_PROPERTIES = 14;
	public static final int TABS = 15;
	public static final int TABPANELS = 16;
	public static final int LINES = 17;
	public static final int SHAPES = 18;//obsolete should be removed!!, but that will break repository updates
	public static final int PARTS = 19;
	public static final int RECTSHAPES = 21;
	public static final int RELATIONS = 22;
	public static final int RELATION_ITEMS = 23;
	public static final int METHODS = 24;
	public static final int STATEMENTS = 25;
	public static final int INTEGER = 26;
	public static final int COLOR = 27;
	public static final int POINT = 28;
	public static final int STRING = 29;
	public static final int DIMENSION = 30;
	public static final int FONT = 31;
	public static final int BOOLEAN = 32;
	public static final int BORDER = 33;
	public static final int VALUELISTS = 34;
	public static final int SCRIPTVARIABLES = 35;
	public static final int SCRIPTCALCULATIONS = 36;
	public static final int MEDIA = 37;
	public static final int COLUMNS = 38; //SYNC_IDS called before, needed columns type for Ivalidatename searchcontext type
	public static final int TABLENODES = 39; // better name whould be datasource node
	public static final int AGGREGATEVARIABLES = 40;
	public static final int USER_PROPERTIES = 41;
	public static final int LOGS = 42;
	public static final int SOLUTIONS = 43;
	public static final int TABLES = 44;
	public static final int STATS = 45;
	public static final int LAYOUTCONTAINERS = 46;
	public static final int WEBCOMPONENTS = 47;
	public static final int WEBCUSTOMTYPES = 48;
	public static final int JSON = 49;
}
