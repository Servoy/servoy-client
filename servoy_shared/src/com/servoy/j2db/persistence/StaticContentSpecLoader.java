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
package com.servoy.j2db.persistence;

import java.util.HashMap;

/**
 * @author jblok
 */
public class StaticContentSpecLoader
{
	public static final int PROTECTION_PASSWORD = 281;

	private static HashMap<Integer, ContentSpec> csMap = new HashMap<Integer, ContentSpec>();

	public static ContentSpec getContentSpec()
	{
		return getContentSpecChanges(0);
	}

	@SuppressWarnings("nls")
	public static ContentSpec getContentSpecChanges(int old_repository_version)
	{
		ContentSpec cs = csMap.get(old_repository_version);
		if (cs != null) return cs;

		cs = new ContentSpec();

		csMap.put(old_repository_version, cs);
		//begin property creation
		if (old_repository_version == 0)
		{
			//	fill content_spec
			cs.new Element(1, IRepository.BEANS, "name", IRepository.STRING);
			cs.new Element(2, IRepository.BEANS, "beanXML", IRepository.STRING);
			cs.new Element(3, IRepository.BEANS, "beanClassName", IRepository.STRING);
			cs.new Element(4, IRepository.BEANS, "usesUI", IRepository.BOOLEAN);
			cs.new Element(5, IRepository.BEANS, "location", IRepository.POINT);
			cs.new Element(6, IRepository.BEANS, "size", IRepository.DIMENSION);
			cs.new Element(7, IRepository.BEANS, "printable", IRepository.BOOLEAN, Boolean.TRUE);
			cs.new Element(8, IRepository.BEANS, "formIndex", IRepository.INTEGER);
			cs.new Element(9, IRepository.FIELDS, "onActionMethodID", IRepository.ELEMENTS);
			cs.new Element(10, IRepository.FIELDS, "anchors", IRepository.INTEGER);
			cs.new Element(11, IRepository.FIELDS, "background", IRepository.COLOR);
			cs.new Element(12, IRepository.FIELDS, "foreground", IRepository.COLOR);
			cs.new Element(13, IRepository.FIELDS, "name", IRepository.STRING);
			cs.new Element(14, IRepository.FIELDS, "toolTipText", IRepository.STRING);
			cs.new Element(15, IRepository.FIELDS, "dataProviderID", IRepository.STRING);
			cs.new Element(16, IRepository.FIELDS, "borderType", IRepository.BORDER);
			cs.new Element(17, IRepository.FIELDS, "editable", IRepository.BOOLEAN, Boolean.TRUE);
			cs.new Element(18, IRepository.FIELDS, "fontType", IRepository.FONT);
			cs.new Element(19, IRepository.FIELDS, "format", IRepository.STRING);

			//is not longer used, reuse this property if needed (it is hidden in the app)
			cs.new Element(20, IRepository.FIELDS, "useRTF", IRepository.BOOLEAN);

			cs.new Element(21, IRepository.FIELDS, "size", IRepository.DIMENSION);
			cs.new Element(22, IRepository.FIELDS, "onFocusGainedMethodID", IRepository.ELEMENTS);
			cs.new Element(23, IRepository.FIELDS, "onFocusLostMethodID", IRepository.ELEMENTS);
			cs.new Element(24, IRepository.FIELDS, "location", IRepository.POINT);
			cs.new Element(25, IRepository.FIELDS, "displayType", IRepository.INTEGER);
			cs.new Element(26, IRepository.FIELDS, "scrollbars", IRepository.INTEGER);
			cs.new Element(27, IRepository.FIELDS, "selectOnEnter", IRepository.BOOLEAN);
			cs.new Element(28, IRepository.FIELDS, "tabSeq", IRepository.INTEGER);
			cs.new Element(29, IRepository.FIELDS, "transparent", IRepository.BOOLEAN);
			cs.new Element(30, IRepository.FIELDS, "horizontalAlignment", IRepository.INTEGER, ContentSpec.MINUS_ONE);
			cs.new Element(31, IRepository.FIELDS, "verticalAlignment", IRepository.INTEGER, ContentSpec.MINUS_ONE);
			cs.new Element(32, IRepository.FIELDS, "valuelistID", IRepository.ELEMENTS);
			cs.new Element(33, IRepository.FIELDS, "printable", IRepository.BOOLEAN, Boolean.TRUE);
			cs.new Element(34, IRepository.FIELDS, "formIndex", IRepository.INTEGER);
			cs.new Element(35, IRepository.FIELDS, "margin", IRepository.INSETS);
			cs.new Element(36, IRepository.FORMS, "serverName", IRepository.SERVERS).flagAsDeprecated();
			cs.new Element(37, IRepository.FORMS, "name", IRepository.STRING);
			cs.new Element(38, IRepository.FORMS, "size", IRepository.DIMENSION);
			cs.new Element(39, IRepository.FORMS, "showInMenu", IRepository.BOOLEAN);
			cs.new Element(40, IRepository.FORMS, "styleName", IRepository.STYLES);
			cs.new Element(41, IRepository.FORMS, "tableName", IRepository.TABLES).flagAsDeprecated();
			cs.new Element(42, IRepository.FORMS, "background", IRepository.COLOR);
			cs.new Element(43, IRepository.FORMS, "view", IRepository.INTEGER);
			cs.new Element(44, IRepository.FORMS, "paperPrintScale", IRepository.INTEGER);
			cs.new Element(45, IRepository.FORMS, "navigatorID", IRepository.ELEMENTS);
			cs.new Element(46, IRepository.TABLENODES, "tableName", IRepository.TABLES).flagAsDeprecated();
			cs.new Element(47, IRepository.TABLENODES, "serverName", IRepository.SERVERS).flagAsDeprecated();
			cs.new Element(48, IRepository.GRAPHICALCOMPONENTS, "name", IRepository.STRING);
			cs.new Element(49, IRepository.GRAPHICALCOMPONENTS, "toolTipText", IRepository.STRING);
			cs.new Element(50, IRepository.GRAPHICALCOMPONENTS, "dataProviderID", IRepository.STRING);
			cs.new Element(51, IRepository.GRAPHICALCOMPONENTS, "onActionMethodID", IRepository.ELEMENTS);
			cs.new Element(52, IRepository.GRAPHICALCOMPONENTS, "anchors", IRepository.INTEGER);
			cs.new Element(53, IRepository.GRAPHICALCOMPONENTS, "background", IRepository.COLOR);
			cs.new Element(54, IRepository.GRAPHICALCOMPONENTS, "foreground", IRepository.COLOR);
			cs.new Element(55, IRepository.GRAPHICALCOMPONENTS, "fontType", IRepository.FONT);
			cs.new Element(56, IRepository.GRAPHICALCOMPONENTS, "size", IRepository.DIMENSION);
			cs.new Element(57, IRepository.GRAPHICALCOMPONENTS, "location", IRepository.POINT);
			cs.new Element(58, IRepository.GRAPHICALCOMPONENTS, "text", IRepository.STRING);
			cs.new Element(59, IRepository.GRAPHICALCOMPONENTS, "verticalAlignment", IRepository.INTEGER, ContentSpec.MINUS_ONE);
			cs.new Element(60, IRepository.GRAPHICALCOMPONENTS, "horizontalAlignment", IRepository.INTEGER, ContentSpec.MINUS_ONE);
			cs.new Element(61, IRepository.GRAPHICALCOMPONENTS, "borderType", IRepository.BORDER);
			cs.new Element(62, IRepository.GRAPHICALCOMPONENTS, "transparent", IRepository.BOOLEAN);
			cs.new Element(63, IRepository.GRAPHICALCOMPONENTS, "imageMediaID", IRepository.ELEMENTS);
			cs.new Element(64, IRepository.GRAPHICALCOMPONENTS, "rolloverImageMediaID", IRepository.ELEMENTS);
			cs.new Element(65, IRepository.GRAPHICALCOMPONENTS, "printable", IRepository.BOOLEAN, Boolean.TRUE);
			cs.new Element(66, IRepository.GRAPHICALCOMPONENTS, "formIndex", IRepository.INTEGER);
			cs.new Element(67, IRepository.GRAPHICALCOMPONENTS, "margin", IRepository.INSETS);
			cs.new Element(68, IRepository.LINES, "foreground", IRepository.COLOR);
			cs.new Element(69, IRepository.LINES, "lineSize", IRepository.INTEGER);
			cs.new Element(70, IRepository.LINES, "point1", IRepository.POINT);
			cs.new Element(71, IRepository.LINES, "point2", IRepository.POINT);
			cs.new Element(72, IRepository.LINES, "formIndex", IRepository.INTEGER);
			cs.new Element(73, IRepository.SHAPES, "foreground", IRepository.COLOR);
			cs.new Element(74, IRepository.SHAPES, "lineSize", IRepository.INTEGER);
			cs.new Element(75, IRepository.SHAPES, "location", IRepository.POINT);
			cs.new Element(76, IRepository.SHAPES, "transparent", IRepository.BOOLEAN);
			cs.new Element(77, IRepository.SHAPES, "size", IRepository.DIMENSION);
			cs.new Element(78, IRepository.SHAPES, "formIndex", IRepository.INTEGER);
			cs.new Element(79, IRepository.PARTS, "allowBreakAcrossPageBounds", IRepository.BOOLEAN);
			cs.new Element(80, IRepository.PARTS, "discardRemainderAfterBreak", IRepository.BOOLEAN);
			cs.new Element(81, IRepository.PARTS, "groupbyDataProviderIDs", IRepository.STRING);
			cs.new Element(82, IRepository.PARTS, "height", IRepository.INTEGER);
			cs.new Element(83, IRepository.PARTS, "pageBreakBefore", IRepository.BOOLEAN);
			cs.new Element(84, IRepository.PARTS, "pageBreakAfterOccurrence", IRepository.INTEGER);
			cs.new Element(85, IRepository.PARTS, "partType", IRepository.INTEGER);
			cs.new Element(86, IRepository.PARTS, "restartPageNumber", IRepository.BOOLEAN);
			cs.new Element(87, IRepository.PARTS, "sequence", IRepository.INTEGER);
			cs.new Element(88, IRepository.PARTS, "background", IRepository.COLOR);
			cs.new Element(89, IRepository.PORTALS, "sortable", IRepository.BOOLEAN);
			cs.new Element(90, IRepository.PORTALS, "formIndex", IRepository.INTEGER);
			cs.new Element(91, IRepository.PORTALS, "anchors", IRepository.INTEGER);
			cs.new Element(92, IRepository.PORTALS, "foreground", IRepository.COLOR);
			cs.new Element(93, IRepository.PORTALS, "background", IRepository.COLOR);
			cs.new Element(94, IRepository.PORTALS, "borderType", IRepository.BORDER);
			cs.new Element(95, IRepository.PORTALS, "location", IRepository.POINT);
			cs.new Element(96, IRepository.PORTALS, "size", IRepository.DIMENSION);
			cs.new Element(97, IRepository.PORTALS, "name", IRepository.STRING);
			cs.new Element(98, IRepository.PORTALS, "relationName", IRepository.STRING);
			cs.new Element(99, IRepository.PORTALS, "reorderable", IRepository.BOOLEAN);
			cs.new Element(100, IRepository.PORTALS, "resizeble", IRepository.BOOLEAN);
			cs.new Element(101, IRepository.PORTALS, "multiLine", IRepository.BOOLEAN);
			cs.new Element(102, IRepository.PORTALS, "rowHeight", IRepository.INTEGER);
			cs.new Element(103, IRepository.PORTALS, "showVerticalLines", IRepository.BOOLEAN);
			cs.new Element(104, IRepository.PORTALS, "showHorizontalLines", IRepository.BOOLEAN);
			cs.new Element(105, IRepository.PORTALS, "intercellSpacing", IRepository.DIMENSION);
			cs.new Element(106, IRepository.PORTALS, "printable", IRepository.BOOLEAN, Boolean.TRUE);
			cs.new Element(107, IRepository.RECTSHAPES, "foreground", IRepository.COLOR);
			cs.new Element(108, IRepository.RECTSHAPES, "background", IRepository.COLOR);
			cs.new Element(109, IRepository.RECTSHAPES, "lineSize", IRepository.INTEGER);
			cs.new Element(110, IRepository.RECTSHAPES, "roundedRadius", IRepository.INTEGER);
			cs.new Element(111, IRepository.RECTSHAPES, "transparent", IRepository.BOOLEAN);
			cs.new Element(112, IRepository.RECTSHAPES, "location", IRepository.POINT);
			cs.new Element(113, IRepository.RECTSHAPES, "size", IRepository.DIMENSION);
			cs.new Element(114, IRepository.RECTSHAPES, "borderType", IRepository.BORDER);
			cs.new Element(115, IRepository.RECTSHAPES, "printable", IRepository.BOOLEAN, Boolean.TRUE);
			cs.new Element(116, IRepository.RECTSHAPES, "name", IRepository.STRING);
			cs.new Element(117, IRepository.RECTSHAPES, "formIndex", IRepository.INTEGER);
			cs.new Element(118, IRepository.RECTSHAPES, "containsFormID", IRepository.ELEMENTS);
			cs.new Element(119, IRepository.TABPANELS, "anchors", IRepository.INTEGER);
			cs.new Element(120, IRepository.TABPANELS, "background", IRepository.COLOR);
			cs.new Element(121, IRepository.TABPANELS, "foreground", IRepository.COLOR);
			cs.new Element(122, IRepository.TABPANELS, "location", IRepository.POINT);
			cs.new Element(123, IRepository.TABPANELS, "tabOrientation", IRepository.INTEGER);
			cs.new Element(124, IRepository.TABPANELS, "name", IRepository.STRING);
			cs.new Element(125, IRepository.TABPANELS, "size", IRepository.DIMENSION);
			cs.new Element(126, IRepository.TABPANELS, "borderType", IRepository.BORDER);
			cs.new Element(127, IRepository.TABPANELS, "printable", IRepository.BOOLEAN, Boolean.TRUE);
			cs.new Element(128, IRepository.TABPANELS, "formIndex", IRepository.INTEGER);
			cs.new Element(129, IRepository.TABPANELS, "fontType", IRepository.FONT);
			cs.new Element(130, IRepository.TABS, "containsFormID", IRepository.ELEMENTS);
			cs.new Element(131, IRepository.TABS, "location", IRepository.POINT);
			cs.new Element(132, IRepository.TABS, "relationName", IRepository.STRING);
			cs.new Element(133, IRepository.TABS, "text", IRepository.STRING);
			cs.new Element(134, IRepository.TABS, "name", IRepository.STRING);
			cs.new Element(135, IRepository.TABS, "imageMediaID", IRepository.ELEMENTS);
			cs.new Element(136, IRepository.TABS, "toolTipText", IRepository.STRING);
			cs.new Element(137, IRepository.METHODS, "name", IRepository.STRING);
			cs.new Element(138, IRepository.METHODS, "methodCode", IRepository.STRING).flagAsDeprecated();
			cs.new Element(139, IRepository.METHODS, "showInMenu", IRepository.BOOLEAN);
			cs.new Element(140, IRepository.SCRIPTCALCULATIONS, "name", IRepository.STRING);
			cs.new Element(141, IRepository.SCRIPTCALCULATIONS, "methodCode", IRepository.STRING).flagAsDeprecated();
			cs.new Element(142, IRepository.RELATIONS, "name", IRepository.STRING);
			cs.new Element(143, IRepository.RELATIONS, "primaryServerName", IRepository.SERVERS).flagAsDeprecated(); //$NON-NLS-2$
			cs.new Element(144, IRepository.RELATIONS, "foreignServerName", IRepository.SERVERS).flagAsDeprecated(); //$NON-NLS-2$
			cs.new Element(145, IRepository.RELATIONS, "primaryTableName", IRepository.TABLES).flagAsDeprecated(); //$NON-NLS-2$
			cs.new Element(146, IRepository.RELATIONS, "foreignTableName", IRepository.TABLES).flagAsDeprecated(); //$NON-NLS-2$
			cs.new Element(147, IRepository.RELATIONS, "deleteRelatedRecords", IRepository.BOOLEAN);
			cs.new Element(148, IRepository.RELATIONS, "allowCreationRelatedRecords", IRepository.BOOLEAN);
			cs.new Element(149, IRepository.RELATIONS, "existsInDB", IRepository.BOOLEAN);
			cs.new Element(150, IRepository.RELATION_ITEMS, "primaryDataProviderID", IRepository.STRING);
			cs.new Element(151, IRepository.RELATION_ITEMS, "foreignColumnName", IRepository.STRING);

			//is not longer used, reuse these properties if needed (it is hidden in the app)
			cs.new Element(152, IRepository.STATEMENTS, "statementTypeID", IRepository.INTEGER);
			cs.new Element(153, IRepository.STATEMENTS, "sqlType", IRepository.INTEGER);
			cs.new Element(154, IRepository.STATEMENTS, "sqlText", IRepository.STRING);

			cs.new Element(155, IRepository.VALUELISTS, "name", IRepository.STRING);
			cs.new Element(156, IRepository.VALUELISTS, "valueListType", IRepository.INTEGER);
			cs.new Element(157, IRepository.VALUELISTS, "relationName", IRepository.STRING);
			cs.new Element(158, IRepository.VALUELISTS, "customValues", IRepository.STRING);
			cs.new Element(159, IRepository.VALUELISTS, "serverName", IRepository.SERVERS).flagAsDeprecated();
			cs.new Element(160, IRepository.VALUELISTS, "tableName", IRepository.TABLES).flagAsDeprecated();
			cs.new Element(161, IRepository.VALUELISTS, "dataProviderID1", IRepository.STRING);
			cs.new Element(162, IRepository.VALUELISTS, "dataProviderID2", IRepository.STRING);
			cs.new Element(163, IRepository.VALUELISTS, "dataProviderID3", IRepository.STRING);
			cs.new Element(164, IRepository.VALUELISTS, "showDataProviders", IRepository.INTEGER);
			cs.new Element(165, IRepository.VALUELISTS, "returnDataProviders", IRepository.INTEGER);
			cs.new Element(166, IRepository.VALUELISTS, "separator", IRepository.STRING);
			cs.new Element(167, IRepository.VALUELISTS, "sortOptions", IRepository.STRING);
			cs.new Element(168, IRepository.SCRIPTVARIABLES, "name", IRepository.STRING);
			cs.new Element(169, IRepository.SCRIPTVARIABLES, "variableType", IRepository.INTEGER, String.valueOf(IColumnTypes.TEXT));
			cs.new Element(170, IRepository.AGGREGATEVARIABLES, "name", IRepository.STRING);
			cs.new Element(171, IRepository.AGGREGATEVARIABLES, "type", IRepository.INTEGER);
			cs.new Element(172, IRepository.AGGREGATEVARIABLES, "dataProviderIDToAggregate", IRepository.STRING);
			cs.new Element(173, IRepository.FIELDS, "displaysTags", IRepository.BOOLEAN);
			cs.new Element(174, IRepository.GRAPHICALCOMPONENTS, "displaysTags", IRepository.BOOLEAN);
			cs.new Element(175, IRepository.RECTSHAPES, "shapeType", IRepository.INTEGER);
			cs.new Element(176, IRepository.RECTSHAPES, "anchors", IRepository.INTEGER);
			cs.new Element(177, IRepository.SHAPES, "background", IRepository.COLOR);
			cs.new Element(178, IRepository.SHAPES, "points", IRepository.STRING);
			cs.new Element(179, IRepository.SHAPES, "printable", IRepository.BOOLEAN, Boolean.TRUE);
			cs.new Element(180, IRepository.SHAPES, "shapeType", IRepository.INTEGER);
			cs.new Element(181, IRepository.SHAPES, "groupID", IRepository.STRING);
			cs.new Element(182, IRepository.SHAPES, "locked", IRepository.BOOLEAN);
			cs.new Element(183, IRepository.RECTSHAPES, "groupID", IRepository.STRING);
			cs.new Element(184, IRepository.RECTSHAPES, "locked", IRepository.BOOLEAN);
			cs.new Element(185, IRepository.GRAPHICALCOMPONENTS, "groupID", IRepository.STRING);
			cs.new Element(186, IRepository.GRAPHICALCOMPONENTS, "locked", IRepository.BOOLEAN);
			cs.new Element(187, IRepository.PORTALS, "groupID", IRepository.STRING);
			cs.new Element(188, IRepository.PORTALS, "locked", IRepository.BOOLEAN);
			cs.new Element(189, IRepository.FIELDS, "groupID", IRepository.STRING);
			cs.new Element(190, IRepository.FIELDS, "locked", IRepository.BOOLEAN);
			cs.new Element(191, IRepository.BEANS, "groupID", IRepository.STRING);
			cs.new Element(192, IRepository.BEANS, "locked", IRepository.BOOLEAN);
			cs.new Element(193, IRepository.TABPANELS, "groupID", IRepository.STRING);
			cs.new Element(194, IRepository.TABPANELS, "locked", IRepository.BOOLEAN);
			cs.new Element(195, IRepository.TABS, "groupID", IRepository.STRING);
			cs.new Element(196, IRepository.TABS, "locked", IRepository.BOOLEAN);
			cs.new Element(197, IRepository.TABS, "foreground", IRepository.COLOR);
			cs.new Element(198, IRepository.TABS, "background", IRepository.COLOR);
			cs.new Element(199, IRepository.TABPANELS, "selectedTabColor", IRepository.COLOR);
			cs.new Element(200, IRepository.VALUELISTS, "relationNMName", IRepository.STRING).flagAsDeprecated();
			cs.new Element(201, IRepository.FORMS, "onLoadMethodID", IRepository.ELEMENTS);
			cs.new Element(202, IRepository.FORMS, "onShowMethodID", IRepository.ELEMENTS);
			cs.new Element(203, IRepository.FORMS, "onHideMethodID", IRepository.ELEMENTS);
			cs.new Element(204, IRepository.FORMS, "initialSort", IRepository.STRING);
			cs.new Element(205, IRepository.FORMS, "onRecordSelectionMethodID", IRepository.ELEMENTS);
			cs.new Element(206, IRepository.FORMS, "onRecordEditStopMethodID", IRepository.ELEMENTS);
			cs.new Element(207, IRepository.SCRIPTCALCULATIONS, "type", IRepository.INTEGER);
			cs.new Element(208, IRepository.FIELDS, "onDataChangeMethodID", IRepository.ELEMENTS);
			cs.new Element(209, IRepository.PORTALS, "initialSort", IRepository.STRING);
			cs.new Element(210, IRepository.FORMS, "onRecordEditStartMethodID", IRepository.ELEMENTS);
			cs.new Element(211, IRepository.FIELDS, "text", IRepository.STRING);
			//is int becouse maybe we want in future 'add empty value:' "never","always","only by new Record"
			cs.new Element(212, IRepository.VALUELISTS, "addEmptyValue", IRepository.INTEGER);
		}

		//ADDS FOR REPOSITORY VERSION 14 OR LOWER
		if (old_repository_version < 14)
		{
			cs.new Element(213, IRepository.GRAPHICALCOMPONENTS, "rotation", IRepository.INTEGER);
			cs.new Element(214, IRepository.PORTALS, "scrollbars", IRepository.INTEGER);
			cs.new Element(215, IRepository.FORMS, "scrollbars", IRepository.INTEGER);
			cs.new Element(216, IRepository.FORMS, "defaultPageFormat", IRepository.STRING);

			cs.new Element(217, IRepository.FORMS, "onNewRecordCmdMethodID", IRepository.ELEMENTS);
			cs.new Element(218, IRepository.FORMS, "onDuplicateRecordCmdMethodID", IRepository.ELEMENTS);
			cs.new Element(219, IRepository.FORMS, "onDeleteRecordCmdMethodID", IRepository.ELEMENTS);
			cs.new Element(220, IRepository.FORMS, "onFindCmdMethodID", IRepository.ELEMENTS);
			cs.new Element(221, IRepository.FORMS, "onShowAllRecordsCmdMethodID", IRepository.ELEMENTS);
			cs.new Element(222, IRepository.FORMS, "onOmitRecordCmdMethodID", IRepository.ELEMENTS);
			cs.new Element(223, IRepository.FORMS, "onShowOmittedRecordsCmdMethodID", IRepository.ELEMENTS);
			cs.new Element(224, IRepository.FORMS, "onInvertRecordsCmdMethodID", IRepository.ELEMENTS);
		}

		//ADDS FOR REPOSITORY VERSION 15 OR LOWER
		if (old_repository_version < 15)
		{
			cs.new Element(225, IRepository.GRAPHICALCOMPONENTS, "printSliding", IRepository.INTEGER);
			cs.new Element(226, IRepository.PORTALS, "printSliding", IRepository.INTEGER);
			cs.new Element(227, IRepository.TABPANELS, "printSliding", IRepository.INTEGER);
			cs.new Element(228, IRepository.FIELDS, "printSliding", IRepository.INTEGER);
			cs.new Element(229, IRepository.SHAPES, "printSliding", IRepository.INTEGER);
			cs.new Element(230, IRepository.RECTSHAPES, "printSliding", IRepository.INTEGER);

			cs.new Element(231, IRepository.FORMS, "borderType", IRepository.BORDER);
			cs.new Element(232, IRepository.FORMS, "useSeparateFoundSet", IRepository.BOOLEAN).flagAsDeprecated();

			cs.new Element(233, IRepository.FORMS, "onSortCmdMethodID", IRepository.ELEMENTS);
			cs.new Element(234, IRepository.FORMS, "onDeleteAllRecordsCmdMethodID", IRepository.ELEMENTS);
			cs.new Element(235, IRepository.FORMS, "onPrintPreviewCmdMethodID", IRepository.ELEMENTS);
		}

		if (old_repository_version < 16)
		{
			cs.new Element(236, IRepository.TABPANELS, "transparent", IRepository.BOOLEAN);
			cs.new Element(237, IRepository.RELATION_ITEMS, "operator", IRepository.INTEGER);
		}

		if (old_repository_version < 17)
		{
			cs.new Element(238, IRepository.PORTALS, "transparent", IRepository.BOOLEAN);
			cs.new Element(239, IRepository.GRAPHICALCOMPONENTS, "mediaOptions", IRepository.INTEGER);
			cs.new Element(240, IRepository.GRAPHICALCOMPONENTS, "tabSeq", IRepository.INTEGER);
			cs.new Element(241, IRepository.GRAPHICALCOMPONENTS, "rolloverCursor", IRepository.INTEGER);
			cs.new Element(242, IRepository.FORMS, "titleText", IRepository.STRING);

			cs.new Element(243, IRepository.SCRIPTVARIABLES, "defaultValue", IRepository.STRING);

			cs.new Element(244, IRepository.SOLUTIONS, "titleText", IRepository.STRING);
			cs.new Element(245, IRepository.SOLUTIONS, "firstFormID", IRepository.ELEMENTS);
			cs.new Element(246, IRepository.SOLUTIONS, "onOpenMethodID", IRepository.ELEMENTS);
			cs.new Element(247, IRepository.SOLUTIONS, "onCloseMethodID", IRepository.ELEMENTS);

			//decoi property is in real live the master_password_hash!!
			//OBSOLETE:			cs.new Element(248,IRepository.SOLUTIONS,"loadOptions",IRepository.STRING); 
		}

		if (old_repository_version < 20)
		{
			cs.new Element(249, IRepository.PORTALS, "rowBGColorCalculation", IRepository.STRING);
			cs.new Element(250, IRepository.FORMS, "rowBGColorCalculation", IRepository.STRING);

			cs.new Element(251, IRepository.GRAPHICALCOMPONENTS, "styleClass", IRepository.STRING);
			cs.new Element(252, IRepository.TABPANELS, "styleClass", IRepository.STRING);
			cs.new Element(253, IRepository.PORTALS, "styleClass", IRepository.STRING);
			cs.new Element(254, IRepository.FIELDS, "styleClass", IRepository.STRING);

			cs.new Element(255, IRepository.SOLUTIONS, "i18nTableName", IRepository.TABLES).flagAsDeprecated();
			cs.new Element(256, IRepository.SOLUTIONS, "i18nServerName", IRepository.SERVERS).flagAsDeprecated();
			cs.new Element(257, IRepository.BEANS, "anchors", IRepository.INTEGER);
			cs.new Element(258, IRepository.TABPANELS, "scrollTabs", IRepository.BOOLEAN);

			cs.new Element(259, IRepository.GRAPHICALCOMPONENTS, "showClick", IRepository.BOOLEAN, Boolean.TRUE);
			cs.new Element(260, IRepository.GRAPHICALCOMPONENTS, "showFocus", IRepository.BOOLEAN, Boolean.TRUE);
		}

		if (old_repository_version < 22)
		{
			cs.new Element(261, IRepository.FORMS, "styleClass", IRepository.STRING);
		}

		if (old_repository_version < 24)
		{
			cs.new Element(262, IRepository.FORMS, "onPreviousRecordCmdMethodID", IRepository.ELEMENTS);
			cs.new Element(263, IRepository.FORMS, "onNextRecordCmdMethodID", IRepository.ELEMENTS);
			cs.new Element(264, IRepository.SOLUTIONS, "modulesNames", IRepository.STRING);
		}
		if (old_repository_version < 25)
		{
			cs.new Element(265, IRepository.FORMS, "onSearchCmdMethodID", IRepository.ELEMENTS);
			cs.new Element(266, IRepository.RELATIONS, "duplicateRelatedRecords", IRepository.BOOLEAN);
			cs.new Element(267, IRepository.RELATIONS, "initialSort", IRepository.STRING);
			cs.new Element(268, IRepository.VALUELISTS, "useTableFilter", IRepository.BOOLEAN);
		}

		if (old_repository_version < 27)
		{
			cs.new Element(269, IRepository.SOLUTIONS, "onErrorMethodID", IRepository.ELEMENTS);
			cs.new Element(270, IRepository.SOLUTIONS, "loginFormID", IRepository.ELEMENTS);
			cs.new Element(271, IRepository.PARTS, "sinkWhenLast", IRepository.BOOLEAN);
			cs.new Element(272, IRepository.BEANS, "onActionMethodID", IRepository.ELEMENTS);
			cs.new Element(273, IRepository.BEANS, "parameters", IRepository.STRING);
		}

		if (old_repository_version < 28)
		{
			cs.new Element(275, IRepository.SOLUTIONS, "mustAuthenticate", IRepository.BOOLEAN).flagAsMetaData();
			cs.new Element(276, IRepository.MEDIA, "name", IRepository.STRING);
			cs.new Element(277, IRepository.MEDIA, "blobId", IRepository.BLOBS);
			cs.new Element(278, IRepository.MEDIA, "mimeType", IRepository.STRING);
			cs.new Element(279, IRepository.SOLUTIONS, "solutionType", IRepository.INTEGER).flagAsMetaData();
			cs.new Element(280, IRepository.STYLES, "CSSText", IRepository.STRING);
			cs.new Element(PROTECTION_PASSWORD, IRepository.SOLUTIONS, "protectionPassword", IRepository.STRING).flagAsMetaData();
		}

		if (old_repository_version < 29)
		{
			cs.new Element(282, IRepository.FORMS, "aliases", IRepository.STRING);
			cs.new Element(283, IRepository.TABPANELS, "closeOnTabs", IRepository.BOOLEAN);
			cs.new Element(284, IRepository.RELATIONS, "allowParentDeleteWhenHavingRelatedRecords", IRepository.BOOLEAN, Boolean.TRUE);
			cs.new Element(285, IRepository.TABLENODES, "onInsertMethodID", IRepository.ELEMENTS);
			cs.new Element(286, IRepository.TABLENODES, "onUpdateMethodID", IRepository.ELEMENTS);
			cs.new Element(287, IRepository.TABLENODES, "onDeleteMethodID", IRepository.ELEMENTS);
			cs.new Element(288, IRepository.GRAPHICALCOMPONENTS, "labelFor", IRepository.STRING);
			cs.new Element(289, IRepository.GRAPHICALCOMPONENTS, "mnemonic", IRepository.STRING);
			cs.new Element(290, IRepository.TABPANELS, "onTabChangeMethodID", IRepository.ELEMENTS);

			cs.new Element(291, IRepository.TABS, "useNewFormInstance", IRepository.BOOLEAN);
			cs.new Element(292, IRepository.RELATIONS, "joinType", IRepository.INTEGER);
		}

		if (old_repository_version < 30)
		{
			cs.new Element(293, IRepository.SOLUTIONS, "textOrientation", IRepository.INTEGER);
		}
		if (old_repository_version < 31)
		{
			cs.new Element(294, IRepository.FORMS, "transparent", IRepository.BOOLEAN);
			cs.new Element(295, IRepository.FORMS, "extendsFormID", IRepository.ELEMENTS);
			cs.new Element(296, IRepository.GRAPHICALCOMPONENTS, "onDoubleClickMethodID", IRepository.ELEMENTS);
			cs.new Element(297, IRepository.GRAPHICALCOMPONENTS, "onRightClickMethodID", IRepository.ELEMENTS);
		}
		if (old_repository_version < 32)
		{
			cs.new Element(298, IRepository.FORMS, "onUnLoadMethodID", IRepository.ELEMENTS);
		}
		if (old_repository_version < 35)
		{
			cs.new Element(299, IRepository.BEANS, "tabSeq", IRepository.INTEGER);
			cs.new Element(300, IRepository.PORTALS, "tabSeq", IRepository.INTEGER);
			cs.new Element(301, IRepository.TABPANELS, "tabSeq", IRepository.INTEGER);
		}

		if (old_repository_version < 36)
		{
			cs.new Element(302, IRepository.FORMS, "onDragMethodID", IRepository.ELEMENTS);
			cs.new Element(303, IRepository.FORMS, "onDragOverMethodID", IRepository.ELEMENTS);
			cs.new Element(304, IRepository.FORMS, "onDropMethodID", IRepository.ELEMENTS);
			cs.new Element(305, IRepository.FORMS, "onElementFocusGainedMethodID", IRepository.ELEMENTS);

			cs.new Element(306, IRepository.SOLUTIONS, "onDataBroadcastMethodID", IRepository.ELEMENTS);

			cs.new Element(307, IRepository.TABLENODES, "onAfterInsertMethodID", IRepository.ELEMENTS);
			cs.new Element(308, IRepository.TABLENODES, "onAfterUpdateMethodID", IRepository.ELEMENTS);
			cs.new Element(309, IRepository.TABLENODES, "onAfterDeleteMethodID", IRepository.ELEMENTS);

			cs.new Element(310, IRepository.FIELDS, "onRightClickMethodID", IRepository.ELEMENTS);

			cs.new Element(311, IRepository.METHODS, "lineNumberOffset", IRepository.INTEGER);

			//new properties which makes others depreciated 
			cs.new Element(312, IRepository.METHODS, "declaration", IRepository.STRING);
			cs.new Element(313, IRepository.FORMS, "namedFoundSet", IRepository.STRING);
			cs.new Element(314, IRepository.FORMS, "dataSource", IRepository.DATASOURCES);
			cs.new Element(315, IRepository.RELATIONS, "primaryDataSource", IRepository.DATASOURCES); //$NON-NLS-2$
			cs.new Element(316, IRepository.RELATIONS, "foreignDataSource", IRepository.DATASOURCES); //$NON-NLS-2$
			cs.new Element(317, IRepository.VALUELISTS, "dataSource", IRepository.DATASOURCES);
			cs.new Element(318, IRepository.SOLUTIONS, "i18nDataSource", IRepository.DATASOURCES);
			cs.new Element(319, IRepository.TABLENODES, "dataSource", IRepository.DATASOURCES);

			//custom properties support
			cs.new Element(320, IRepository.FORMS, "customProperties", IRepository.STRING);
			cs.new Element(321, IRepository.BEANS, "customProperties", IRepository.STRING);
			cs.new Element(322, IRepository.PORTALS, "customProperties", IRepository.STRING);
			cs.new Element(323, IRepository.TABPANELS, "customProperties", IRepository.STRING);
			cs.new Element(324, IRepository.TABS, "customProperties", IRepository.STRING);
			cs.new Element(325, IRepository.GRAPHICALCOMPONENTS, "customProperties", IRepository.STRING);
			cs.new Element(326, IRepository.RELATIONS, "customProperties", IRepository.STRING);
			cs.new Element(327, IRepository.SHAPES, "customProperties", IRepository.STRING);
			cs.new Element(328, IRepository.RECTSHAPES, "customProperties", IRepository.STRING);
			cs.new Element(329, IRepository.VALUELISTS, "customProperties", IRepository.STRING);
			cs.new Element(330, IRepository.LINES, "customProperties", IRepository.STRING);
			cs.new Element(331, IRepository.PARTS, "customProperties", IRepository.STRING);
			cs.new Element(332, IRepository.METHODS, "customProperties", IRepository.STRING);
			cs.new Element(333, IRepository.SCRIPTVARIABLES, "customProperties", IRepository.STRING);
			cs.new Element(334, IRepository.SCRIPTCALCULATIONS, "customProperties", IRepository.STRING);
			cs.new Element(335, IRepository.TABLENODES, "customProperties", IRepository.STRING);
			cs.new Element(336, IRepository.AGGREGATEVARIABLES, "customProperties", IRepository.STRING);
			cs.new Element(337, IRepository.SOLUTIONS, "customProperties", IRepository.STRING);
			cs.new Element(338, IRepository.STYLES, "customProperties", IRepository.STRING);
			cs.new Element(339, IRepository.MEDIA, "customProperties", IRepository.STRING);
			cs.new Element(340, IRepository.FIELDS, "customProperties", IRepository.STRING);
			cs.new Element(341, IRepository.RELATION_ITEMS, "customProperties", IRepository.STRING);

			cs.new Element(342, IRepository.PORTALS, "onDragMethodID", IRepository.ELEMENTS);
			cs.new Element(343, IRepository.PORTALS, "onDragOverMethodID", IRepository.ELEMENTS);
			cs.new Element(344, IRepository.PORTALS, "onDropMethodID", IRepository.ELEMENTS);

			cs.new Element(345, IRepository.FORMS, "onElementFocusLostMethodID", IRepository.ELEMENTS);
			cs.new Element(346, IRepository.SCRIPTCALCULATIONS, "declaration", IRepository.STRING);
			cs.new Element(347, IRepository.SCRIPTCALCULATIONS, "lineNumberOffset", IRepository.INTEGER);
		}

		if (old_repository_version < 37)
		{
			cs.new Element(348, IRepository.VALUELISTS, "fallbackValueListID", IRepository.ELEMENTS);
			cs.new Element(349, IRepository.SOLUTIONS, "onInitMethodID", IRepository.ELEMENTS);
			cs.new Element(350, IRepository.FORMS, "onResizeMethodID", IRepository.ELEMENTS);
			cs.new Element(351, IRepository.TEMPLATES, "resourceType", IRepository.INTEGER);
			cs.new Element(352, IRepository.TEMPLATES, "content", IRepository.STRING);
			cs.new Element(353, IRepository.TEMPLATES, "customProperties", IRepository.STRING);
		}

		if (old_repository_version < 38)
		{
			cs.new Element(354, IRepository.FORMS, "onDragEndMethodID", IRepository.ELEMENTS);
			cs.new Element(355, IRepository.PORTALS, "onDragEndMethodID", IRepository.ELEMENTS);
			cs.new Element(356, IRepository.FORMS, "access", IRepository.INTEGER);
			cs.new Element(357, IRepository.SCRIPTVARIABLES, "access", IRepository.INTEGER);
			cs.new Element(358, IRepository.METHODS, "access", IRepository.INTEGER);
			cs.new Element(359, IRepository.SCRIPTVARIABLES, "comment", IRepository.STRING);
			cs.new Element(360, IRepository.FORMS, "onRenderMethodID", IRepository.ELEMENTS);
			cs.new Element(361, IRepository.PORTALS, "onRenderMethodID", IRepository.ELEMENTS);
			cs.new Element(362, IRepository.FIELDS, "onRenderMethodID", IRepository.ELEMENTS);
			cs.new Element(363, IRepository.GRAPHICALCOMPONENTS, "onRenderMethodID", IRepository.ELEMENTS);
		}

		//##add property adds here

		/*
		 * cs.new Element(262,IRepository.PARTS,"styleClass",IRepository.STRING); Element(267,IRepository.FORMS,"viewOptions",IRepository.String); //maybe put
		 * in existing view property Element(269,IRepository.GRAPHICALCOMPONENTS,"fontOptions",IRepository.STRING));//condensed,strike
		 * trough,underline,outline,drop shadow
		 */
		return cs;
	}
}
