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

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.util.HashMap;

/**
 * @author jblok
 */
public class StaticContentSpecLoader
{
	public static final int PROTECTION_PASSWORD = 281;

	public static final TypedProperty<Boolean> PROPERTY_ALLOWBREAKACROSSPAGEBOUNDS = new TypedProperty<Boolean>("allowBreakAcrossPageBounds"); //$NON-NLS-1$
	public static final TypedProperty<Boolean> PROPERTY_ALLOWCREATIONRELATEDRECORDS = new TypedProperty<Boolean>("allowCreationRelatedRecords"); //$NON-NLS-1$
	public static final TypedProperty<Boolean> PROPERTY_ALLOWPARENTDELETEWHENHAVINGRELATEDRECORDS = new TypedProperty<Boolean>(
		"allowParentDeleteWhenHavingRelatedRecords"); //$NON-NLS-1$
	public static final TypedProperty<Boolean> PROPERTY_CLOSEONTABS = new TypedProperty<Boolean>("closeOnTabs"); //$NON-NLS-1$
	public static final TypedProperty<Boolean> PROPERTY_DELETERELATEDRECORDS = new TypedProperty<Boolean>("deleteRelatedRecords"); //$NON-NLS-1$
	public static final TypedProperty<Boolean> PROPERTY_DISCARDREMAINDERAFTERBREAK = new TypedProperty<Boolean>("discardRemainderAfterBreak"); //$NON-NLS-1$
	public static final TypedProperty<Boolean> PROPERTY_DISPLAYSTAGS = new TypedProperty<Boolean>("displaysTags"); //$NON-NLS-1$
	public static final TypedProperty<Boolean> PROPERTY_DUPLICATERELATEDRECORDS = new TypedProperty<Boolean>("duplicateRelatedRecords"); //$NON-NLS-1$
	public static final TypedProperty<Boolean> PROPERTY_EDITABLE = new TypedProperty<Boolean>("editable"); //$NON-NLS-1$
	public static final TypedProperty<Boolean> PROPERTY_ENABLED = new TypedProperty<Boolean>("enabled"); //$NON-NLS-1$
	public static final TypedProperty<Boolean> PROPERTY_EXISTSINDB = new TypedProperty<Boolean>("existsInDB"); //$NON-NLS-1$
	public static final TypedProperty<Boolean> PROPERTY_LOCKED = new TypedProperty<Boolean>("locked"); //$NON-NLS-1$
	public static final TypedProperty<Boolean> PROPERTY_MULTILINE = new TypedProperty<Boolean>("multiLine"); //$NON-NLS-1$
	public static final TypedProperty<Boolean> PROPERTY_PAGEBREAKBEFORE = new TypedProperty<Boolean>("pageBreakBefore"); //$NON-NLS-1$
	public static final TypedProperty<Boolean> PROPERTY_PRINTABLE = new TypedProperty<Boolean>("printable"); //$NON-NLS-1$
	public static final TypedProperty<Boolean> PROPERTY_REORDERABLE = new TypedProperty<Boolean>("reorderable"); //$NON-NLS-1$
	public static final TypedProperty<Boolean> PROPERTY_RESIZEBLE = new TypedProperty<Boolean>("resizeble"); //$NON-NLS-1$
	public static final TypedProperty<Boolean> PROPERTY_RESIZABLE = new TypedProperty<Boolean>("resizable"); //$NON-NLS-1$
	public static final TypedProperty<Boolean> PROPERTY_RESTARTPAGENUMBER = new TypedProperty<Boolean>("restartPageNumber"); //$NON-NLS-1$
	public static final TypedProperty<Boolean> PROPERTY_SCROLLTABS = new TypedProperty<Boolean>("scrollTabs"); //$NON-NLS-1$
	public static final TypedProperty<Boolean> PROPERTY_SELECTONENTER = new TypedProperty<Boolean>("selectOnEnter"); //$NON-NLS-1$
	public static final TypedProperty<Boolean> PROPERTY_SHOWCLICK = new TypedProperty<Boolean>("showClick"); //$NON-NLS-1$
	public static final TypedProperty<Boolean> PROPERTY_SHOWFOCUS = new TypedProperty<Boolean>("showFocus"); //$NON-NLS-1$
	public static final TypedProperty<Boolean> PROPERTY_SHOWHORIZONTALLINES = new TypedProperty<Boolean>("showHorizontalLines"); //$NON-NLS-1$
	public static final TypedProperty<Boolean> PROPERTY_SHOWINMENU = new TypedProperty<Boolean>("showInMenu"); //$NON-NLS-1$
	public static final TypedProperty<Boolean> PROPERTY_SHOWVERTICALLINES = new TypedProperty<Boolean>("showVerticalLines"); //$NON-NLS-1$
	public static final TypedProperty<Boolean> PROPERTY_SINKWHENLAST = new TypedProperty<Boolean>("sinkWhenLast"); //$NON-NLS-1$
	public static final TypedProperty<Boolean> PROPERTY_SORTABLE = new TypedProperty<Boolean>("sortable"); //$NON-NLS-1$
	public static final TypedProperty<Boolean> PROPERTY_TRANSPARENT = new TypedProperty<Boolean>("transparent"); //$NON-NLS-1$
	public static final TypedProperty<Boolean> PROPERTY_USENEWFORMINSTANCE = new TypedProperty<Boolean>("useNewFormInstance"); //$NON-NLS-1$
	public static final TypedProperty<Boolean> PROPERTY_USERTF = new TypedProperty<Boolean>("useRTF"); //$NON-NLS-1$
	public static final TypedProperty<Boolean> PROPERTY_USESUI = new TypedProperty<Boolean>("usesUI"); //$NON-NLS-1$
	public static final TypedProperty<Boolean> PROPERTY_USETABLEFILTER = new TypedProperty<Boolean>("useTableFilter"); //$NON-NLS-1$
	public static final TypedProperty<Boolean> PROPERTY_VISIBLE = new TypedProperty<Boolean>("visible"); //$NON-NLS-1$
	public static final TypedProperty<Dimension> PROPERTY_INTERCELLSPACING = new TypedProperty<Dimension>("intercellSpacing"); //$NON-NLS-1$
	public static final TypedProperty<Insets> PROPERTY_MARGIN = new TypedProperty<Insets>("margin"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_ENCAPSULATION = new TypedProperty<Integer>("encapsulation"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_ADDEMPTYVALUE = new TypedProperty<Integer>("addEmptyValue"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_ANCHORS = new TypedProperty<Integer>("anchors"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_BLOBID = new TypedProperty<Integer>("blobId"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_CONTAINSFORMID = new TypedProperty<Integer>("containsFormID"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_DISPLAYTYPE = new TypedProperty<Integer>("displayType"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_EXTENDSFORMID = new TypedProperty<Integer>("extendsFormID"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_EXTENDSID = new TypedProperty<Integer>("extendsID"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_FALLBACKVALUELISTID = new TypedProperty<Integer>("fallbackValueListID"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_FIRSTFORMID = new TypedProperty<Integer>("firstFormID"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_FORMINDEX = new TypedProperty<Integer>("formIndex"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_HEIGHT = new TypedProperty<Integer>("height"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_HORIZONTALALIGNMENT = new TypedProperty<Integer>("horizontalAlignment"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_IMAGEMEDIAID = new TypedProperty<Integer>("imageMediaID"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_JOINTYPE = new TypedProperty<Integer>("joinType"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_LINENUMBEROFFSET = new TypedProperty<Integer>("lineNumberOffset"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_LINESIZE = new TypedProperty<Integer>("lineSize"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_LOGINFORMID = new TypedProperty<Integer>("loginFormID"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_MEDIAOPTIONS = new TypedProperty<Integer>("mediaOptions"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_NAVIGATORID = new TypedProperty<Integer>("navigatorID"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_ONACTIONMETHODID = new TypedProperty<Integer>("onActionMethodID"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_ONAFTERDELETEMETHODID = new TypedProperty<Integer>("onAfterDeleteMethodID"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_ONAFTERINSERTMETHODID = new TypedProperty<Integer>("onAfterInsertMethodID"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_ONAFTERUPDATEMETHODID = new TypedProperty<Integer>("onAfterUpdateMethodID"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_ONCLOSEMETHODID = new TypedProperty<Integer>("onCloseMethodID"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_ONDATABROADCASTMETHODID = new TypedProperty<Integer>("onDataBroadcastMethodID"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_ONDATACHANGEMETHODID = new TypedProperty<Integer>("onDataChangeMethodID"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_ONDELETEALLRECORDSCMDMETHODID = new TypedProperty<Integer>("onDeleteAllRecordsCmdMethodID"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_ONDELETEMETHODID = new TypedProperty<Integer>("onDeleteMethodID"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_ONDELETERECORDCMDMETHODID = new TypedProperty<Integer>("onDeleteRecordCmdMethodID"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_ONDOUBLECLICKMETHODID = new TypedProperty<Integer>("onDoubleClickMethodID"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_ONDRAGENDMETHODID = new TypedProperty<Integer>("onDragEndMethodID"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_ONDRAGMETHODID = new TypedProperty<Integer>("onDragMethodID"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_ONDRAGOVERMETHODID = new TypedProperty<Integer>("onDragOverMethodID"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_ONDROPMETHODID = new TypedProperty<Integer>("onDropMethodID"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_ONDUPLICATERECORDCMDMETHODID = new TypedProperty<Integer>("onDuplicateRecordCmdMethodID"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_ONELEMENTFOCUSGAINEDMETHODID = new TypedProperty<Integer>("onElementFocusGainedMethodID"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_ONELEMENTFOCUSLOSTMETHODID = new TypedProperty<Integer>("onElementFocusLostMethodID"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_ONERRORMETHODID = new TypedProperty<Integer>("onErrorMethodID"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_ONFINDCMDMETHODID = new TypedProperty<Integer>("onFindCmdMethodID"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_ONFOCUSGAINEDMETHODID = new TypedProperty<Integer>("onFocusGainedMethodID"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_ONFOCUSLOSTMETHODID = new TypedProperty<Integer>("onFocusLostMethodID"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_ONHIDEMETHODID = new TypedProperty<Integer>("onHideMethodID"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_ONINITMETHODID = new TypedProperty<Integer>("onInitMethodID"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_ONINSERTMETHODID = new TypedProperty<Integer>("onInsertMethodID"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_ONINVERTRECORDSCMDMETHODID = new TypedProperty<Integer>("onInvertRecordsCmdMethodID"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_ONLOADMETHODID = new TypedProperty<Integer>("onLoadMethodID"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_ONNEWRECORDCMDMETHODID = new TypedProperty<Integer>("onNewRecordCmdMethodID"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_ONNEXTRECORDCMDMETHODID = new TypedProperty<Integer>("onNextRecordCmdMethodID"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_ONOMITRECORDCMDMETHODID = new TypedProperty<Integer>("onOmitRecordCmdMethodID"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_ONOPENMETHODID = new TypedProperty<Integer>("onOpenMethodID"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_ONPREVIOUSRECORDCMDMETHODID = new TypedProperty<Integer>("onPreviousRecordCmdMethodID"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_ONPRINTPREVIEWCMDMETHODID = new TypedProperty<Integer>("onPrintPreviewCmdMethodID"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_ONRECORDEDITSTARTMETHODID = new TypedProperty<Integer>("onRecordEditStartMethodID"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_ONRECORDEDITSTOPMETHODID = new TypedProperty<Integer>("onRecordEditStopMethodID"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_ONRECORDSELECTIONMETHODID = new TypedProperty<Integer>("onRecordSelectionMethodID"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_ONRENDERMETHODID = new TypedProperty<Integer>("onRenderMethodID"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_ONRESIZEMETHODID = new TypedProperty<Integer>("onResizeMethodID"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_ONRIGHTCLICKMETHODID = new TypedProperty<Integer>("onRightClickMethodID"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_ONSEARCHCMDMETHODID = new TypedProperty<Integer>("onSearchCmdMethodID"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_ONSHOWALLRECORDSCMDMETHODID = new TypedProperty<Integer>("onShowAllRecordsCmdMethodID"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_ONSHOWMETHODID = new TypedProperty<Integer>("onShowMethodID"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_ONSHOWOMITTEDRECORDSCMDMETHODID = new TypedProperty<Integer>("onShowOmittedRecordsCmdMethodID"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_ONSORTCMDMETHODID = new TypedProperty<Integer>("onSortCmdMethodID"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_ONTABCHANGEMETHODID = new TypedProperty<Integer>("onTabChangeMethodID"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_ONCHANGEMETHODID = new TypedProperty<Integer>("onChangeMethodID"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_ONUNLOADMETHODID = new TypedProperty<Integer>("onUnLoadMethodID"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_ONUPDATEMETHODID = new TypedProperty<Integer>("onUpdateMethodID"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_OPERATOR = new TypedProperty<Integer>("operator"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_PAGEBREAKAFTEROCCURRENCE = new TypedProperty<Integer>("pageBreakAfterOccurrence"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_PAPERPRINTSCALE = new TypedProperty<Integer>("paperPrintScale"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_PARTTYPE = new TypedProperty<Integer>("partType"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_PRINTSLIDING = new TypedProperty<Integer>("printSliding"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_RESOURCETYPE = new TypedProperty<Integer>("resourceType"); //$NON-NLS-1$;
	public static final TypedProperty<Integer> PROPERTY_RETURNDATAPROVIDERS = new TypedProperty<Integer>("returnDataProviders"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_ROLLOVERCURSOR = new TypedProperty<Integer>("rolloverCursor"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_ROLLOVERIMAGEMEDIAID = new TypedProperty<Integer>("rolloverImageMediaID"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_ROTATION = new TypedProperty<Integer>("rotation"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_ROUNDEDRADIUS = new TypedProperty<Integer>("roundedRadius"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_ROWHEIGHT = new TypedProperty<Integer>("rowHeight"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_SCROLLBARS = new TypedProperty<Integer>("scrollbars"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_SEQUENCE = new TypedProperty<Integer>("sequence"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_SHAPETYPE = new TypedProperty<Integer>("shapeType"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_SHOWDATAPROVIDERS = new TypedProperty<Integer>("showDataProviders"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_TABORIENTATION = new TypedProperty<Integer>("tabOrientation"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_TABSEQ = new TypedProperty<Integer>("tabSeq"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_TEXTORIENTATION = new TypedProperty<Integer>("textOrientation"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_TYPE = new TypedProperty<Integer>("type"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_VALUELISTID = new TypedProperty<Integer>("valuelistID"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_VALUELISTTYPE = new TypedProperty<Integer>("valueListType"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_VARIABLETYPE = new TypedProperty<Integer>("variableType"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_VERTICALALIGNMENT = new TypedProperty<Integer>("verticalAlignment"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_VIEW = new TypedProperty<Integer>("view"); //$NON-NLS-1$
	public static final TypedProperty<java.awt.Color> PROPERTY_BACKGROUND = new TypedProperty<java.awt.Color>("background"); //$NON-NLS-1$
	public static final TypedProperty<java.awt.Color> PROPERTY_FOREGROUND = new TypedProperty<java.awt.Color>("foreground"); //$NON-NLS-1$
	public static final TypedProperty<java.awt.Color> PROPERTY_SELECTEDTABCOLOR = new TypedProperty<java.awt.Color>("selectedTabColor"); //$NON-NLS-1$
	public static final TypedProperty<java.awt.Dimension> PROPERTY_SIZE = new TypedProperty<java.awt.Dimension>("size"); //$NON-NLS-1$
	public static final TypedProperty<Integer> PROPERTY_WIDTH = new TypedProperty<Integer>("width"); //$NON-NLS-1$
	public static final TypedProperty<java.awt.Point> PROPERTY_LOCATION = new TypedProperty<java.awt.Point>("location"); //$NON-NLS-1$
	public static final TypedProperty<String> PROPERTY_ALIASES = new TypedProperty<String>("aliases"); //$NON-NLS-1$
	public static final TypedProperty<String> PROPERTY_BEANCLASSNAME = new TypedProperty<String>("beanClassName"); //$NON-NLS-1$
	public static final TypedProperty<String> PROPERTY_BEANXML = new TypedProperty<String>("beanXML"); //$NON-NLS-1$
	public static final TypedProperty<String> PROPERTY_BORDERTYPE = new TypedProperty<String>("borderType"); //$NON-NLS-1$
	public static final TypedProperty<String> PROPERTY_COMMENT = new TypedProperty<String>("comment"); //$NON-NLS-1$
	public static final TypedProperty<String> PROPERTY_CONTENT = new TypedProperty<String>("content"); //$NON-NLS-1$;
	public static final TypedProperty<String> PROPERTY_CSSTEXT = new TypedProperty<String>("CSSText"); //$NON-NLS-1$
	public static final TypedProperty<String> PROPERTY_CUSTOMPROPERTIES = new TypedProperty<String>("customProperties"); //$NON-NLS-1$
	public static final TypedProperty<String> PROPERTY_CUSTOMVALUES = new TypedProperty<String>("customValues"); //$NON-NLS-1$
	public static final TypedProperty<String> PROPERTY_DATAPROVIDERID1 = new TypedProperty<String>("dataProviderID1"); //$NON-NLS-1$
	public static final TypedProperty<String> PROPERTY_DATAPROVIDERID2 = new TypedProperty<String>("dataProviderID2"); //$NON-NLS-1$
	public static final TypedProperty<String> PROPERTY_DATAPROVIDERID3 = new TypedProperty<String>("dataProviderID3"); //$NON-NLS-1$
	public static final TypedProperty<String> PROPERTY_DATAPROVIDERID = new TypedProperty<String>("dataProviderID"); //$NON-NLS-1$
	public static final TypedProperty<String> PROPERTY_DATAPROVIDERIDTOAGGREGATE = new TypedProperty<String>("dataProviderIDToAggregate"); //$NON-NLS-1$
	public static final TypedProperty<String> PROPERTY_DATASOURCE = new TypedProperty<String>("dataSource"); //$NON-NLS-1$
	public static final TypedProperty<String> PROPERTY_DECLARATION = new TypedProperty<String>("declaration"); //$NON-NLS-1$
	public static final TypedProperty<String> PROPERTY_DEFAULTPAGEFORMAT = new TypedProperty<String>("defaultPageFormat"); //$NON-NLS-1$
	public static final TypedProperty<String> PROPERTY_DEFAULTVALUE = new TypedProperty<String>("defaultValue"); //$NON-NLS-1$
	public static final TypedProperty<String> PROPERTY_FONTTYPE = new TypedProperty<String>("fontType"); //$NON-NLS-1$
	public static final TypedProperty<String> PROPERTY_FOREIGNCOLUMNNAME = new TypedProperty<String>("foreignColumnName"); //$NON-NLS-1$
	public static final TypedProperty<String> PROPERTY_FOREIGNDATASOURCE = new TypedProperty<String>("foreignDataSource"); //$NON-NLS-1$
	public static final TypedProperty<String> PROPERTY_FORMAT = new TypedProperty<String>("format"); //$NON-NLS-1$
	public static final TypedProperty<String> PROPERTY_GROUPBYDATAPROVIDERIDS = new TypedProperty<String>("groupbyDataProviderIDs"); //$NON-NLS-1$
	public static final TypedProperty<String> PROPERTY_GROUPID = new TypedProperty<String>("groupID"); //$NON-NLS-1$
	public static final TypedProperty<String> PROPERTY_I18NDATASOURCE = new TypedProperty<String>("i18nDataSource"); //$NON-NLS-1$
	public static final TypedProperty<String> PROPERTY_INITIALSORT = new TypedProperty<String>("initialSort"); //$NON-NLS-1$
	public static final TypedProperty<String> PROPERTY_LABELFOR = new TypedProperty<String>("labelFor"); //$NON-NLS-1$
	public static final TypedProperty<String> PROPERTY_METHODCODE = new TypedProperty<String>("methodCode"); //$NON-NLS-1$
	public static final TypedProperty<String> PROPERTY_MIMETYPE = new TypedProperty<String>("mimeType"); //$NON-NLS-1$
	public static final TypedProperty<String> PROPERTY_MNEMONIC = new TypedProperty<String>("mnemonic"); //$NON-NLS-1$
	public static final TypedProperty<String> PROPERTY_MODULESNAMES = new TypedProperty<String>("modulesNames"); //$NON-NLS-1$
	public static final TypedProperty<String> PROPERTY_NAMEDFOUNDSET = new TypedProperty<String>("namedFoundSet"); //$NON-NLS-1$
	public static final TypedProperty<String> PROPERTY_NAME = new TypedProperty<String>("name"); //$NON-NLS-1$
	public static final TypedProperty<String> PROPERTY_PARAMETERS = new TypedProperty<String>("parameters"); //$NON-NLS-1$
	public static final TypedProperty<String> PROPERTY_POINTS = new TypedProperty<String>("points"); //$NON-NLS-1$
	public static final TypedProperty<String> PROPERTY_PRIMARYDATAPROVIDERID = new TypedProperty<String>("primaryDataProviderID"); //$NON-NLS-1$
	public static final TypedProperty<String> PROPERTY_PRIMARYDATASOURCE = new TypedProperty<String>("primaryDataSource"); //$NON-NLS-1$
	public static final TypedProperty<String> PROPERTY_RELATIONNAME = new TypedProperty<String>("relationName"); //$NON-NLS-1$
	public static final TypedProperty<String> PROPERTY_ROWBGCOLORCALCULATION = new TypedProperty<String>("rowBGColorCalculation"); //$NON-NLS-1$
	public static final TypedProperty<String> PROPERTY_SEPARATOR = new TypedProperty<String>("separator"); //$NON-NLS-1$
	public static final TypedProperty<String> PROPERTY_SORTOPTIONS = new TypedProperty<String>("sortOptions"); //$NON-NLS-1$
	public static final TypedProperty<String> PROPERTY_STYLECLASS = new TypedProperty<String>("styleClass"); //$NON-NLS-1$
	public static final TypedProperty<String> PROPERTY_STYLENAME = new TypedProperty<String>("styleName"); //$NON-NLS-1$
	public static final TypedProperty<String> PROPERTY_TEXT = new TypedProperty<String>("text"); //$NON-NLS-1$
	public static final TypedProperty<String> PROPERTY_TITLETEXT = new TypedProperty<String>("titleText"); //$NON-NLS-1$
	public static final TypedProperty<String> PROPERTY_TOOLTIPTEXT = new TypedProperty<String>("toolTipText"); //$NON-NLS-1$

	// deprecated or metadata properties
	private static final TypedProperty<String> PROPERTY_SERVERNAME = new TypedProperty<String>("serverName"); //$NON-NLS-1$;
	private static final TypedProperty<String> PROPERTY_TABLENAME = new TypedProperty<String>("tableName"); //$NON-NLS-1$;
	private static final TypedProperty<Point> PROPERTY_POINT1 = new TypedProperty<Point>("point1"); //$NON-NLS-1$;
	private static final TypedProperty<Point> PROPERTY_POINT2 = new TypedProperty<Point>("point2"); //$NON-NLS-1$;
	private static final TypedProperty<String> PROPERTY_PRIMARYSERVERNAME = new TypedProperty<String>("primaryServerName"); //$NON-NLS-1$;
	private static final TypedProperty<String> PROPERTY_FOREIGNSERVERNAME = new TypedProperty<String>("foreignServerName"); //$NON-NLS-1$;
	private static final TypedProperty<String> PROPERTY_PRIMARYTABLENAME = new TypedProperty<String>("primaryTableName"); //$NON-NLS-1$;
	private static final TypedProperty<String> PROPERTY_FOREIGNTABLENAME = new TypedProperty<String>("foreignTableName"); //$NON-NLS-1$;
	private static final TypedProperty<Integer> PROPERTY_STATEMENTTYPEID = new TypedProperty<Integer>("statementTypeID"); //$NON-NLS-1$;
	private static final TypedProperty<Integer> PROPERTY_SQLTYPE = new TypedProperty<Integer>("sqlType"); //$NON-NLS-1$;
	private static final TypedProperty<String> PROPERTY_SQLTEXT = new TypedProperty<String>("sqlText"); //$NON-NLS-1$;
	private static final TypedProperty<String> PROPERTY_RELATIONNMNAME = new TypedProperty<String>("relationNMName"); //$NON-NLS-1$;
	private static final TypedProperty<Boolean> PROPERTY_USESEPARATEFOUNDSET = new TypedProperty<Boolean>("useSeparateFoundSet"); //$NON-NLS-1$;
	private static final TypedProperty<String> PROPERTY_I18NTABLENAME = new TypedProperty<String>("i18nTableName"); //$NON-NLS-1$;
	private static final TypedProperty<String> PROPERTY_I18NSERVERNAME = new TypedProperty<String>("i18nServerName"); //$NON-NLS-1$;
	private static final TypedProperty<Boolean> PROPERTY_MUSTAUTHENTICATE = new TypedProperty<Boolean>("mustAuthenticate"); //$NON-NLS-1$;
	private static final TypedProperty<Integer> PROPERTY_SOLUTIONTYPE = new TypedProperty<Integer>("solutionType"); //$NON-NLS-1$;
	private static final TypedProperty<String> PROPERTY_PROTECTIONPASSWORD = new TypedProperty<String>("protectionPassword"); //$NON-NLS-1$;

	private static HashMap<Integer, ContentSpec> csMap = new HashMap<Integer, ContentSpec>();

	private static final ContentSpec allCs = getContentSpecChanges(0);

	public static ContentSpec getContentSpec()
	{
		return allCs;
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
			cs.new Element(1, IRepository.BEANS, PROPERTY_NAME.getPropertyName(), IRepository.STRING);
			cs.new Element(2, IRepository.BEANS, PROPERTY_BEANXML.getPropertyName(), IRepository.STRING);
			cs.new Element(3, IRepository.BEANS, PROPERTY_BEANCLASSNAME.getPropertyName(), IRepository.STRING);
			cs.new Element(4, IRepository.BEANS, PROPERTY_USESUI.getPropertyName(), IRepository.BOOLEAN);
			cs.new Element(5, IRepository.BEANS, PROPERTY_LOCATION.getPropertyName(), IRepository.POINT);
			cs.new Element(6, IRepository.BEANS, PROPERTY_SIZE.getPropertyName(), IRepository.DIMENSION);
			cs.new Element(7, IRepository.BEANS, PROPERTY_PRINTABLE.getPropertyName(), IRepository.BOOLEAN, Boolean.TRUE);
			cs.new Element(8, IRepository.BEANS, PROPERTY_FORMINDEX.getPropertyName(), IRepository.INTEGER);
			cs.new Element(9, IRepository.FIELDS, PROPERTY_ONACTIONMETHODID.getPropertyName(), IRepository.ELEMENTS);
			cs.new Element(10, IRepository.FIELDS, PROPERTY_ANCHORS.getPropertyName(), IRepository.INTEGER);
			cs.new Element(11, IRepository.FIELDS, PROPERTY_BACKGROUND.getPropertyName(), IRepository.COLOR);
			cs.new Element(12, IRepository.FIELDS, PROPERTY_FOREGROUND.getPropertyName(), IRepository.COLOR);
			cs.new Element(13, IRepository.FIELDS, PROPERTY_NAME.getPropertyName(), IRepository.STRING);
			cs.new Element(14, IRepository.FIELDS, PROPERTY_TOOLTIPTEXT.getPropertyName(), IRepository.STRING);
			cs.new Element(15, IRepository.FIELDS, PROPERTY_DATAPROVIDERID.getPropertyName(), IRepository.STRING);
			cs.new Element(16, IRepository.FIELDS, PROPERTY_BORDERTYPE.getPropertyName(), IRepository.BORDER);
			cs.new Element(17, IRepository.FIELDS, PROPERTY_EDITABLE.getPropertyName(), IRepository.BOOLEAN, Boolean.TRUE);
			cs.new Element(18, IRepository.FIELDS, PROPERTY_FONTTYPE.getPropertyName(), IRepository.FONT);
			cs.new Element(19, IRepository.FIELDS, PROPERTY_FORMAT.getPropertyName(), IRepository.STRING);

			//is not longer used, reuse this property if needed (it is hidden in the app)
			cs.new Element(20, IRepository.FIELDS, PROPERTY_USERTF.getPropertyName(), IRepository.BOOLEAN);

			cs.new Element(21, IRepository.FIELDS, PROPERTY_SIZE.getPropertyName(), IRepository.DIMENSION);
			cs.new Element(22, IRepository.FIELDS, PROPERTY_ONFOCUSGAINEDMETHODID.getPropertyName(), IRepository.ELEMENTS);
			cs.new Element(23, IRepository.FIELDS, PROPERTY_ONFOCUSLOSTMETHODID.getPropertyName(), IRepository.ELEMENTS);
			cs.new Element(24, IRepository.FIELDS, PROPERTY_LOCATION.getPropertyName(), IRepository.POINT);
			cs.new Element(25, IRepository.FIELDS, PROPERTY_DISPLAYTYPE.getPropertyName(), IRepository.INTEGER);
			cs.new Element(26, IRepository.FIELDS, PROPERTY_SCROLLBARS.getPropertyName(), IRepository.INTEGER);
			cs.new Element(27, IRepository.FIELDS, PROPERTY_SELECTONENTER.getPropertyName(), IRepository.BOOLEAN);
			cs.new Element(28, IRepository.FIELDS, PROPERTY_TABSEQ.getPropertyName(), IRepository.INTEGER);
			cs.new Element(29, IRepository.FIELDS, PROPERTY_TRANSPARENT.getPropertyName(), IRepository.BOOLEAN);
			cs.new Element(30, IRepository.FIELDS, PROPERTY_HORIZONTALALIGNMENT.getPropertyName(), IRepository.INTEGER, ContentSpec.MINUS_ONE);
			cs.new Element(31, IRepository.FIELDS, PROPERTY_VERTICALALIGNMENT.getPropertyName(), IRepository.INTEGER, ContentSpec.MINUS_ONE);
			cs.new Element(32, IRepository.FIELDS, PROPERTY_VALUELISTID.getPropertyName(), IRepository.ELEMENTS);
			cs.new Element(33, IRepository.FIELDS, PROPERTY_PRINTABLE.getPropertyName(), IRepository.BOOLEAN, Boolean.TRUE);
			cs.new Element(34, IRepository.FIELDS, PROPERTY_FORMINDEX.getPropertyName(), IRepository.INTEGER);
			cs.new Element(35, IRepository.FIELDS, PROPERTY_MARGIN.getPropertyName(), IRepository.INSETS);
			cs.new Element(36, IRepository.FORMS, PROPERTY_SERVERNAME.getPropertyName(), IRepository.SERVERS).flagAsDeprecated();
			cs.new Element(37, IRepository.FORMS, PROPERTY_NAME.getPropertyName(), IRepository.STRING);
			cs.new Element(38, IRepository.FORMS, PROPERTY_SIZE.getPropertyName(), IRepository.DIMENSION);
			cs.new Element(39, IRepository.FORMS, PROPERTY_SHOWINMENU.getPropertyName(), IRepository.BOOLEAN);
			cs.new Element(40, IRepository.FORMS, PROPERTY_STYLENAME.getPropertyName(), IRepository.STYLES);
			cs.new Element(41, IRepository.FORMS, PROPERTY_TABLENAME.getPropertyName(), IRepository.TABLES).flagAsDeprecated();
			cs.new Element(42, IRepository.FORMS, PROPERTY_BACKGROUND.getPropertyName(), IRepository.COLOR);
			cs.new Element(43, IRepository.FORMS, PROPERTY_VIEW.getPropertyName(), IRepository.INTEGER);
			cs.new Element(44, IRepository.FORMS, PROPERTY_PAPERPRINTSCALE.getPropertyName(), IRepository.INTEGER);
			cs.new Element(45, IRepository.FORMS, PROPERTY_NAVIGATORID.getPropertyName(), IRepository.ELEMENTS);
			cs.new Element(46, IRepository.TABLENODES, PROPERTY_TABLENAME.getPropertyName(), IRepository.TABLES).flagAsDeprecated();
			cs.new Element(47, IRepository.TABLENODES, PROPERTY_SERVERNAME.getPropertyName(), IRepository.SERVERS).flagAsDeprecated();
			cs.new Element(48, IRepository.GRAPHICALCOMPONENTS, PROPERTY_NAME.getPropertyName(), IRepository.STRING);
			cs.new Element(49, IRepository.GRAPHICALCOMPONENTS, PROPERTY_TOOLTIPTEXT.getPropertyName(), IRepository.STRING);
			cs.new Element(50, IRepository.GRAPHICALCOMPONENTS, PROPERTY_DATAPROVIDERID.getPropertyName(), IRepository.STRING);
			cs.new Element(51, IRepository.GRAPHICALCOMPONENTS, PROPERTY_ONACTIONMETHODID.getPropertyName(), IRepository.ELEMENTS);
			cs.new Element(52, IRepository.GRAPHICALCOMPONENTS, PROPERTY_ANCHORS.getPropertyName(), IRepository.INTEGER);
			cs.new Element(53, IRepository.GRAPHICALCOMPONENTS, PROPERTY_BACKGROUND.getPropertyName(), IRepository.COLOR);
			cs.new Element(54, IRepository.GRAPHICALCOMPONENTS, PROPERTY_FOREGROUND.getPropertyName(), IRepository.COLOR);
			cs.new Element(55, IRepository.GRAPHICALCOMPONENTS, PROPERTY_FONTTYPE.getPropertyName(), IRepository.FONT);
			cs.new Element(56, IRepository.GRAPHICALCOMPONENTS, PROPERTY_SIZE.getPropertyName(), IRepository.DIMENSION);
			cs.new Element(57, IRepository.GRAPHICALCOMPONENTS, PROPERTY_LOCATION.getPropertyName(), IRepository.POINT);
			cs.new Element(58, IRepository.GRAPHICALCOMPONENTS, PROPERTY_TEXT.getPropertyName(), IRepository.STRING);
			cs.new Element(59, IRepository.GRAPHICALCOMPONENTS, PROPERTY_VERTICALALIGNMENT.getPropertyName(), IRepository.INTEGER, ContentSpec.MINUS_ONE);
			cs.new Element(60, IRepository.GRAPHICALCOMPONENTS, PROPERTY_HORIZONTALALIGNMENT.getPropertyName(), IRepository.INTEGER, ContentSpec.MINUS_ONE);
			cs.new Element(61, IRepository.GRAPHICALCOMPONENTS, PROPERTY_BORDERTYPE.getPropertyName(), IRepository.BORDER);
			cs.new Element(62, IRepository.GRAPHICALCOMPONENTS, PROPERTY_TRANSPARENT.getPropertyName(), IRepository.BOOLEAN);
			cs.new Element(63, IRepository.GRAPHICALCOMPONENTS, PROPERTY_IMAGEMEDIAID.getPropertyName(), IRepository.ELEMENTS);
			cs.new Element(64, IRepository.GRAPHICALCOMPONENTS, PROPERTY_ROLLOVERIMAGEMEDIAID.getPropertyName(), IRepository.ELEMENTS);
			cs.new Element(65, IRepository.GRAPHICALCOMPONENTS, PROPERTY_PRINTABLE.getPropertyName(), IRepository.BOOLEAN, Boolean.TRUE);
			cs.new Element(66, IRepository.GRAPHICALCOMPONENTS, PROPERTY_FORMINDEX.getPropertyName(), IRepository.INTEGER);
			cs.new Element(67, IRepository.GRAPHICALCOMPONENTS, PROPERTY_MARGIN.getPropertyName(), IRepository.INSETS);
			cs.new Element(68, IRepository.LINES, PROPERTY_FOREGROUND.getPropertyName(), IRepository.COLOR);
			cs.new Element(69, IRepository.LINES, PROPERTY_LINESIZE.getPropertyName(), IRepository.INTEGER);
			cs.new Element(70, IRepository.LINES, PROPERTY_POINT1.getPropertyName(), IRepository.POINT);
			cs.new Element(71, IRepository.LINES, PROPERTY_POINT2.getPropertyName(), IRepository.POINT);
			cs.new Element(72, IRepository.LINES, PROPERTY_FORMINDEX.getPropertyName(), IRepository.INTEGER);
			cs.new Element(73, IRepository.SHAPES, PROPERTY_FOREGROUND.getPropertyName(), IRepository.COLOR);
			cs.new Element(74, IRepository.SHAPES, PROPERTY_LINESIZE.getPropertyName(), IRepository.INTEGER);
			cs.new Element(75, IRepository.SHAPES, PROPERTY_LOCATION.getPropertyName(), IRepository.POINT);
			cs.new Element(76, IRepository.SHAPES, PROPERTY_TRANSPARENT.getPropertyName(), IRepository.BOOLEAN);
			cs.new Element(77, IRepository.SHAPES, PROPERTY_SIZE.getPropertyName(), IRepository.DIMENSION);
			cs.new Element(78, IRepository.SHAPES, PROPERTY_FORMINDEX.getPropertyName(), IRepository.INTEGER);
			cs.new Element(79, IRepository.PARTS, PROPERTY_ALLOWBREAKACROSSPAGEBOUNDS.getPropertyName(), IRepository.BOOLEAN);
			cs.new Element(80, IRepository.PARTS, PROPERTY_DISCARDREMAINDERAFTERBREAK.getPropertyName(), IRepository.BOOLEAN);
			cs.new Element(81, IRepository.PARTS, PROPERTY_GROUPBYDATAPROVIDERIDS.getPropertyName(), IRepository.STRING);
			cs.new Element(82, IRepository.PARTS, PROPERTY_HEIGHT.getPropertyName(), IRepository.INTEGER);
			cs.new Element(83, IRepository.PARTS, PROPERTY_PAGEBREAKBEFORE.getPropertyName(), IRepository.BOOLEAN);
			cs.new Element(84, IRepository.PARTS, PROPERTY_PAGEBREAKAFTEROCCURRENCE.getPropertyName(), IRepository.INTEGER);
			cs.new Element(85, IRepository.PARTS, PROPERTY_PARTTYPE.getPropertyName(), IRepository.INTEGER);
			cs.new Element(86, IRepository.PARTS, PROPERTY_RESTARTPAGENUMBER.getPropertyName(), IRepository.BOOLEAN);
			cs.new Element(87, IRepository.PARTS, PROPERTY_SEQUENCE.getPropertyName(), IRepository.INTEGER);
			cs.new Element(88, IRepository.PARTS, PROPERTY_BACKGROUND.getPropertyName(), IRepository.COLOR);
			cs.new Element(89, IRepository.PORTALS, PROPERTY_SORTABLE.getPropertyName(), IRepository.BOOLEAN);
			cs.new Element(90, IRepository.PORTALS, PROPERTY_FORMINDEX.getPropertyName(), IRepository.INTEGER);
			cs.new Element(91, IRepository.PORTALS, PROPERTY_ANCHORS.getPropertyName(), IRepository.INTEGER);
			cs.new Element(92, IRepository.PORTALS, PROPERTY_FOREGROUND.getPropertyName(), IRepository.COLOR);
			cs.new Element(93, IRepository.PORTALS, PROPERTY_BACKGROUND.getPropertyName(), IRepository.COLOR);
			cs.new Element(94, IRepository.PORTALS, PROPERTY_BORDERTYPE.getPropertyName(), IRepository.BORDER);
			cs.new Element(95, IRepository.PORTALS, PROPERTY_LOCATION.getPropertyName(), IRepository.POINT);
			cs.new Element(96, IRepository.PORTALS, PROPERTY_SIZE.getPropertyName(), IRepository.DIMENSION);
			cs.new Element(97, IRepository.PORTALS, PROPERTY_NAME.getPropertyName(), IRepository.STRING);
			cs.new Element(98, IRepository.PORTALS, PROPERTY_RELATIONNAME.getPropertyName(), IRepository.STRING);
			cs.new Element(99, IRepository.PORTALS, PROPERTY_REORDERABLE.getPropertyName(), IRepository.BOOLEAN);
			cs.new Element(100, IRepository.PORTALS, PROPERTY_RESIZEBLE.getPropertyName(), IRepository.BOOLEAN).flagAsDeprecated();
			cs.new Element(101, IRepository.PORTALS, PROPERTY_MULTILINE.getPropertyName(), IRepository.BOOLEAN);
			cs.new Element(102, IRepository.PORTALS, PROPERTY_ROWHEIGHT.getPropertyName(), IRepository.INTEGER);
			cs.new Element(103, IRepository.PORTALS, PROPERTY_SHOWVERTICALLINES.getPropertyName(), IRepository.BOOLEAN);
			cs.new Element(104, IRepository.PORTALS, PROPERTY_SHOWHORIZONTALLINES.getPropertyName(), IRepository.BOOLEAN);
			cs.new Element(105, IRepository.PORTALS, PROPERTY_INTERCELLSPACING.getPropertyName(), IRepository.DIMENSION);
			cs.new Element(106, IRepository.PORTALS, PROPERTY_PRINTABLE.getPropertyName(), IRepository.BOOLEAN, Boolean.TRUE);
			cs.new Element(107, IRepository.RECTSHAPES, PROPERTY_FOREGROUND.getPropertyName(), IRepository.COLOR);
			cs.new Element(108, IRepository.RECTSHAPES, PROPERTY_BACKGROUND.getPropertyName(), IRepository.COLOR);
			cs.new Element(109, IRepository.RECTSHAPES, PROPERTY_LINESIZE.getPropertyName(), IRepository.INTEGER);
			cs.new Element(110, IRepository.RECTSHAPES, PROPERTY_ROUNDEDRADIUS.getPropertyName(), IRepository.INTEGER);
			cs.new Element(111, IRepository.RECTSHAPES, PROPERTY_TRANSPARENT.getPropertyName(), IRepository.BOOLEAN);
			cs.new Element(112, IRepository.RECTSHAPES, PROPERTY_LOCATION.getPropertyName(), IRepository.POINT);
			cs.new Element(113, IRepository.RECTSHAPES, PROPERTY_SIZE.getPropertyName(), IRepository.DIMENSION);
			cs.new Element(114, IRepository.RECTSHAPES, PROPERTY_BORDERTYPE.getPropertyName(), IRepository.BORDER);
			cs.new Element(115, IRepository.RECTSHAPES, PROPERTY_PRINTABLE.getPropertyName(), IRepository.BOOLEAN, Boolean.TRUE);
			cs.new Element(116, IRepository.RECTSHAPES, PROPERTY_NAME.getPropertyName(), IRepository.STRING);
			cs.new Element(117, IRepository.RECTSHAPES, PROPERTY_FORMINDEX.getPropertyName(), IRepository.INTEGER);
			cs.new Element(118, IRepository.RECTSHAPES, PROPERTY_CONTAINSFORMID.getPropertyName(), IRepository.ELEMENTS);
			cs.new Element(119, IRepository.TABPANELS, PROPERTY_ANCHORS.getPropertyName(), IRepository.INTEGER);
			cs.new Element(120, IRepository.TABPANELS, PROPERTY_BACKGROUND.getPropertyName(), IRepository.COLOR);
			cs.new Element(121, IRepository.TABPANELS, PROPERTY_FOREGROUND.getPropertyName(), IRepository.COLOR);
			cs.new Element(122, IRepository.TABPANELS, PROPERTY_LOCATION.getPropertyName(), IRepository.POINT);
			cs.new Element(123, IRepository.TABPANELS, PROPERTY_TABORIENTATION.getPropertyName(), IRepository.INTEGER);
			cs.new Element(124, IRepository.TABPANELS, PROPERTY_NAME.getPropertyName(), IRepository.STRING);
			cs.new Element(125, IRepository.TABPANELS, PROPERTY_SIZE.getPropertyName(), IRepository.DIMENSION);
			cs.new Element(126, IRepository.TABPANELS, PROPERTY_BORDERTYPE.getPropertyName(), IRepository.BORDER);
			cs.new Element(127, IRepository.TABPANELS, PROPERTY_PRINTABLE.getPropertyName(), IRepository.BOOLEAN, Boolean.TRUE);
			cs.new Element(128, IRepository.TABPANELS, PROPERTY_FORMINDEX.getPropertyName(), IRepository.INTEGER);
			cs.new Element(129, IRepository.TABPANELS, PROPERTY_FONTTYPE.getPropertyName(), IRepository.FONT);
			cs.new Element(130, IRepository.TABS, PROPERTY_CONTAINSFORMID.getPropertyName(), IRepository.ELEMENTS);
			cs.new Element(131, IRepository.TABS, PROPERTY_LOCATION.getPropertyName(), IRepository.POINT);
			cs.new Element(132, IRepository.TABS, PROPERTY_RELATIONNAME.getPropertyName(), IRepository.STRING);
			cs.new Element(133, IRepository.TABS, PROPERTY_TEXT.getPropertyName(), IRepository.STRING);
			cs.new Element(134, IRepository.TABS, PROPERTY_NAME.getPropertyName(), IRepository.STRING);
			cs.new Element(135, IRepository.TABS, PROPERTY_IMAGEMEDIAID.getPropertyName(), IRepository.ELEMENTS);
			cs.new Element(136, IRepository.TABS, PROPERTY_TOOLTIPTEXT.getPropertyName(), IRepository.STRING);
			cs.new Element(137, IRepository.METHODS, PROPERTY_NAME.getPropertyName(), IRepository.STRING);
			cs.new Element(138, IRepository.METHODS, PROPERTY_METHODCODE.getPropertyName(), IRepository.STRING).flagAsDeprecated();
			cs.new Element(139, IRepository.METHODS, PROPERTY_SHOWINMENU.getPropertyName(), IRepository.BOOLEAN);
			cs.new Element(140, IRepository.SCRIPTCALCULATIONS, PROPERTY_NAME.getPropertyName(), IRepository.STRING);
			cs.new Element(141, IRepository.SCRIPTCALCULATIONS, PROPERTY_METHODCODE.getPropertyName(), IRepository.STRING).flagAsDeprecated();
			cs.new Element(142, IRepository.RELATIONS, PROPERTY_NAME.getPropertyName(), IRepository.STRING);
			cs.new Element(143, IRepository.RELATIONS, PROPERTY_PRIMARYSERVERNAME.getPropertyName(), IRepository.SERVERS).flagAsDeprecated(); //$NON-NLS-2$
			cs.new Element(144, IRepository.RELATIONS, PROPERTY_FOREIGNSERVERNAME.getPropertyName(), IRepository.SERVERS).flagAsDeprecated(); //$NON-NLS-2$
			cs.new Element(145, IRepository.RELATIONS, PROPERTY_PRIMARYTABLENAME.getPropertyName(), IRepository.TABLES).flagAsDeprecated(); //$NON-NLS-2$
			cs.new Element(146, IRepository.RELATIONS, PROPERTY_FOREIGNTABLENAME.getPropertyName(), IRepository.TABLES).flagAsDeprecated(); //$NON-NLS-2$
			cs.new Element(147, IRepository.RELATIONS, PROPERTY_DELETERELATEDRECORDS.getPropertyName(), IRepository.BOOLEAN);
			cs.new Element(148, IRepository.RELATIONS, PROPERTY_ALLOWCREATIONRELATEDRECORDS.getPropertyName(), IRepository.BOOLEAN);
			cs.new Element(149, IRepository.RELATIONS, PROPERTY_EXISTSINDB.getPropertyName(), IRepository.BOOLEAN);
			cs.new Element(150, IRepository.RELATION_ITEMS, PROPERTY_PRIMARYDATAPROVIDERID.getPropertyName(), IRepository.STRING);
			cs.new Element(151, IRepository.RELATION_ITEMS, PROPERTY_FOREIGNCOLUMNNAME.getPropertyName(), IRepository.STRING);

			//is not longer used, reuse these properties if needed (it is hidden in the app)
			cs.new Element(152, IRepository.STATEMENTS, PROPERTY_STATEMENTTYPEID.getPropertyName(), IRepository.INTEGER);
			cs.new Element(153, IRepository.STATEMENTS, PROPERTY_SQLTYPE.getPropertyName(), IRepository.INTEGER);
			cs.new Element(154, IRepository.STATEMENTS, PROPERTY_SQLTEXT.getPropertyName(), IRepository.STRING);

			cs.new Element(155, IRepository.VALUELISTS, PROPERTY_NAME.getPropertyName(), IRepository.STRING);
			cs.new Element(156, IRepository.VALUELISTS, PROPERTY_VALUELISTTYPE.getPropertyName(), IRepository.INTEGER);
			cs.new Element(157, IRepository.VALUELISTS, PROPERTY_RELATIONNAME.getPropertyName(), IRepository.STRING);
			cs.new Element(158, IRepository.VALUELISTS, PROPERTY_CUSTOMVALUES.getPropertyName(), IRepository.STRING);
			cs.new Element(159, IRepository.VALUELISTS, PROPERTY_SERVERNAME.getPropertyName(), IRepository.SERVERS).flagAsDeprecated();
			cs.new Element(160, IRepository.VALUELISTS, PROPERTY_TABLENAME.getPropertyName(), IRepository.TABLES).flagAsDeprecated();
			cs.new Element(161, IRepository.VALUELISTS, PROPERTY_DATAPROVIDERID1.getPropertyName(), IRepository.STRING);
			cs.new Element(162, IRepository.VALUELISTS, PROPERTY_DATAPROVIDERID2.getPropertyName(), IRepository.STRING);
			cs.new Element(163, IRepository.VALUELISTS, PROPERTY_DATAPROVIDERID3.getPropertyName(), IRepository.STRING);
			cs.new Element(164, IRepository.VALUELISTS, PROPERTY_SHOWDATAPROVIDERS.getPropertyName(), IRepository.INTEGER);
			cs.new Element(165, IRepository.VALUELISTS, PROPERTY_RETURNDATAPROVIDERS.getPropertyName(), IRepository.INTEGER);
			cs.new Element(166, IRepository.VALUELISTS, PROPERTY_SEPARATOR.getPropertyName(), IRepository.STRING);
			cs.new Element(167, IRepository.VALUELISTS, PROPERTY_SORTOPTIONS.getPropertyName(), IRepository.STRING);
			cs.new Element(168, IRepository.SCRIPTVARIABLES, PROPERTY_NAME.getPropertyName(), IRepository.STRING);
			cs.new Element(169, IRepository.SCRIPTVARIABLES, PROPERTY_VARIABLETYPE.getPropertyName(), IRepository.INTEGER, IColumnTypes.TEXT);
			cs.new Element(170, IRepository.AGGREGATEVARIABLES, PROPERTY_NAME.getPropertyName(), IRepository.STRING);
			cs.new Element(171, IRepository.AGGREGATEVARIABLES, PROPERTY_TYPE.getPropertyName(), IRepository.INTEGER);
			cs.new Element(172, IRepository.AGGREGATEVARIABLES, PROPERTY_DATAPROVIDERIDTOAGGREGATE.getPropertyName(), IRepository.STRING);
			cs.new Element(173, IRepository.FIELDS, PROPERTY_DISPLAYSTAGS.getPropertyName(), IRepository.BOOLEAN);
			cs.new Element(174, IRepository.GRAPHICALCOMPONENTS, PROPERTY_DISPLAYSTAGS.getPropertyName(), IRepository.BOOLEAN);
			cs.new Element(175, IRepository.RECTSHAPES, PROPERTY_SHAPETYPE.getPropertyName(), IRepository.INTEGER);
			cs.new Element(176, IRepository.RECTSHAPES, PROPERTY_ANCHORS.getPropertyName(), IRepository.INTEGER);
			cs.new Element(177, IRepository.SHAPES, PROPERTY_BACKGROUND.getPropertyName(), IRepository.COLOR);
			cs.new Element(178, IRepository.SHAPES, PROPERTY_POINTS.getPropertyName(), IRepository.STRING);
			cs.new Element(179, IRepository.SHAPES, PROPERTY_PRINTABLE.getPropertyName(), IRepository.BOOLEAN, Boolean.TRUE);
			cs.new Element(180, IRepository.SHAPES, PROPERTY_SHAPETYPE.getPropertyName(), IRepository.INTEGER);
			cs.new Element(181, IRepository.SHAPES, PROPERTY_GROUPID.getPropertyName(), IRepository.STRING);
			cs.new Element(182, IRepository.SHAPES, PROPERTY_LOCKED.getPropertyName(), IRepository.BOOLEAN);
			cs.new Element(183, IRepository.RECTSHAPES, PROPERTY_GROUPID.getPropertyName(), IRepository.STRING);
			cs.new Element(184, IRepository.RECTSHAPES, PROPERTY_LOCKED.getPropertyName(), IRepository.BOOLEAN);
			cs.new Element(185, IRepository.GRAPHICALCOMPONENTS, PROPERTY_GROUPID.getPropertyName(), IRepository.STRING);
			cs.new Element(186, IRepository.GRAPHICALCOMPONENTS, PROPERTY_LOCKED.getPropertyName(), IRepository.BOOLEAN);
			cs.new Element(187, IRepository.PORTALS, PROPERTY_GROUPID.getPropertyName(), IRepository.STRING);
			cs.new Element(188, IRepository.PORTALS, PROPERTY_LOCKED.getPropertyName(), IRepository.BOOLEAN);
			cs.new Element(189, IRepository.FIELDS, PROPERTY_GROUPID.getPropertyName(), IRepository.STRING);
			cs.new Element(190, IRepository.FIELDS, PROPERTY_LOCKED.getPropertyName(), IRepository.BOOLEAN);
			cs.new Element(191, IRepository.BEANS, PROPERTY_GROUPID.getPropertyName(), IRepository.STRING);
			cs.new Element(192, IRepository.BEANS, PROPERTY_LOCKED.getPropertyName(), IRepository.BOOLEAN);
			cs.new Element(193, IRepository.TABPANELS, PROPERTY_GROUPID.getPropertyName(), IRepository.STRING);
			cs.new Element(194, IRepository.TABPANELS, PROPERTY_LOCKED.getPropertyName(), IRepository.BOOLEAN);
			cs.new Element(195, IRepository.TABS, PROPERTY_GROUPID.getPropertyName(), IRepository.STRING);
			cs.new Element(196, IRepository.TABS, PROPERTY_LOCKED.getPropertyName(), IRepository.BOOLEAN);
			cs.new Element(197, IRepository.TABS, PROPERTY_FOREGROUND.getPropertyName(), IRepository.COLOR);
			cs.new Element(198, IRepository.TABS, PROPERTY_BACKGROUND.getPropertyName(), IRepository.COLOR);
			cs.new Element(199, IRepository.TABPANELS, PROPERTY_SELECTEDTABCOLOR.getPropertyName(), IRepository.COLOR);
			cs.new Element(200, IRepository.VALUELISTS, PROPERTY_RELATIONNMNAME.getPropertyName(), IRepository.STRING).flagAsDeprecated();
			cs.new Element(201, IRepository.FORMS, PROPERTY_ONLOADMETHODID.getPropertyName(), IRepository.ELEMENTS);
			cs.new Element(202, IRepository.FORMS, PROPERTY_ONSHOWMETHODID.getPropertyName(), IRepository.ELEMENTS);
			cs.new Element(203, IRepository.FORMS, PROPERTY_ONHIDEMETHODID.getPropertyName(), IRepository.ELEMENTS);
			cs.new Element(204, IRepository.FORMS, PROPERTY_INITIALSORT.getPropertyName(), IRepository.STRING);
			cs.new Element(205, IRepository.FORMS, PROPERTY_ONRECORDSELECTIONMETHODID.getPropertyName(), IRepository.ELEMENTS);
			cs.new Element(206, IRepository.FORMS, PROPERTY_ONRECORDEDITSTOPMETHODID.getPropertyName(), IRepository.ELEMENTS);
			cs.new Element(207, IRepository.SCRIPTCALCULATIONS, PROPERTY_TYPE.getPropertyName(), IRepository.INTEGER);
			cs.new Element(208, IRepository.FIELDS, PROPERTY_ONDATACHANGEMETHODID.getPropertyName(), IRepository.ELEMENTS);
			cs.new Element(209, IRepository.PORTALS, PROPERTY_INITIALSORT.getPropertyName(), IRepository.STRING);
			cs.new Element(210, IRepository.FORMS, PROPERTY_ONRECORDEDITSTARTMETHODID.getPropertyName(), IRepository.ELEMENTS);
			cs.new Element(211, IRepository.FIELDS, PROPERTY_TEXT.getPropertyName(), IRepository.STRING);
			//is int becouse maybe we want in future 'add empty value:' PROPERTY_NEVER.getPropertyName(),"always","only by new Record"
			cs.new Element(212, IRepository.VALUELISTS, PROPERTY_ADDEMPTYVALUE.getPropertyName(), IRepository.INTEGER);
		}

		//ADDS FOR REPOSITORY VERSION 14 OR LOWER
		if (old_repository_version < 14)
		{
			cs.new Element(213, IRepository.GRAPHICALCOMPONENTS, PROPERTY_ROTATION.getPropertyName(), IRepository.INTEGER);
			cs.new Element(214, IRepository.PORTALS, PROPERTY_SCROLLBARS.getPropertyName(), IRepository.INTEGER);
			cs.new Element(215, IRepository.FORMS, PROPERTY_SCROLLBARS.getPropertyName(), IRepository.INTEGER);
			cs.new Element(216, IRepository.FORMS, PROPERTY_DEFAULTPAGEFORMAT.getPropertyName(), IRepository.STRING);

			cs.new Element(217, IRepository.FORMS, PROPERTY_ONNEWRECORDCMDMETHODID.getPropertyName(), IRepository.ELEMENTS);
			cs.new Element(218, IRepository.FORMS, PROPERTY_ONDUPLICATERECORDCMDMETHODID.getPropertyName(), IRepository.ELEMENTS);
			cs.new Element(219, IRepository.FORMS, PROPERTY_ONDELETERECORDCMDMETHODID.getPropertyName(), IRepository.ELEMENTS);
			cs.new Element(220, IRepository.FORMS, PROPERTY_ONFINDCMDMETHODID.getPropertyName(), IRepository.ELEMENTS);
			cs.new Element(221, IRepository.FORMS, PROPERTY_ONSHOWALLRECORDSCMDMETHODID.getPropertyName(), IRepository.ELEMENTS);
			cs.new Element(222, IRepository.FORMS, PROPERTY_ONOMITRECORDCMDMETHODID.getPropertyName(), IRepository.ELEMENTS);
			cs.new Element(223, IRepository.FORMS, PROPERTY_ONSHOWOMITTEDRECORDSCMDMETHODID.getPropertyName(), IRepository.ELEMENTS);
			cs.new Element(224, IRepository.FORMS, PROPERTY_ONINVERTRECORDSCMDMETHODID.getPropertyName(), IRepository.ELEMENTS);
		}

		//ADDS FOR REPOSITORY VERSION 15 OR LOWER
		if (old_repository_version < 15)
		{
			cs.new Element(225, IRepository.GRAPHICALCOMPONENTS, PROPERTY_PRINTSLIDING.getPropertyName(), IRepository.INTEGER);
			cs.new Element(226, IRepository.PORTALS, PROPERTY_PRINTSLIDING.getPropertyName(), IRepository.INTEGER);
			cs.new Element(227, IRepository.TABPANELS, PROPERTY_PRINTSLIDING.getPropertyName(), IRepository.INTEGER);
			cs.new Element(228, IRepository.FIELDS, PROPERTY_PRINTSLIDING.getPropertyName(), IRepository.INTEGER);
			cs.new Element(229, IRepository.SHAPES, PROPERTY_PRINTSLIDING.getPropertyName(), IRepository.INTEGER);
			cs.new Element(230, IRepository.RECTSHAPES, PROPERTY_PRINTSLIDING.getPropertyName(), IRepository.INTEGER);

			cs.new Element(231, IRepository.FORMS, PROPERTY_BORDERTYPE.getPropertyName(), IRepository.BORDER);
			cs.new Element(232, IRepository.FORMS, PROPERTY_USESEPARATEFOUNDSET.getPropertyName(), IRepository.BOOLEAN).flagAsDeprecated();

			cs.new Element(233, IRepository.FORMS, PROPERTY_ONSORTCMDMETHODID.getPropertyName(), IRepository.ELEMENTS);
			cs.new Element(234, IRepository.FORMS, PROPERTY_ONDELETEALLRECORDSCMDMETHODID.getPropertyName(), IRepository.ELEMENTS);
			cs.new Element(235, IRepository.FORMS, PROPERTY_ONPRINTPREVIEWCMDMETHODID.getPropertyName(), IRepository.ELEMENTS);
		}

		if (old_repository_version < 16)
		{
			cs.new Element(236, IRepository.TABPANELS, PROPERTY_TRANSPARENT.getPropertyName(), IRepository.BOOLEAN);
			cs.new Element(237, IRepository.RELATION_ITEMS, PROPERTY_OPERATOR.getPropertyName(), IRepository.INTEGER);
		}

		if (old_repository_version < 17)
		{
			cs.new Element(238, IRepository.PORTALS, PROPERTY_TRANSPARENT.getPropertyName(), IRepository.BOOLEAN);
			cs.new Element(239, IRepository.GRAPHICALCOMPONENTS, PROPERTY_MEDIAOPTIONS.getPropertyName(), IRepository.INTEGER);
			cs.new Element(240, IRepository.GRAPHICALCOMPONENTS, PROPERTY_TABSEQ.getPropertyName(), IRepository.INTEGER);
			cs.new Element(241, IRepository.GRAPHICALCOMPONENTS, PROPERTY_ROLLOVERCURSOR.getPropertyName(), IRepository.INTEGER);
			cs.new Element(242, IRepository.FORMS, PROPERTY_TITLETEXT.getPropertyName(), IRepository.STRING);

			cs.new Element(243, IRepository.SCRIPTVARIABLES, PROPERTY_DEFAULTVALUE.getPropertyName(), IRepository.STRING);

			cs.new Element(244, IRepository.SOLUTIONS, PROPERTY_TITLETEXT.getPropertyName(), IRepository.STRING);
			cs.new Element(245, IRepository.SOLUTIONS, PROPERTY_FIRSTFORMID.getPropertyName(), IRepository.ELEMENTS);
			cs.new Element(246, IRepository.SOLUTIONS, PROPERTY_ONOPENMETHODID.getPropertyName(), IRepository.ELEMENTS);
			cs.new Element(247, IRepository.SOLUTIONS, PROPERTY_ONCLOSEMETHODID.getPropertyName(), IRepository.ELEMENTS);

			//decoi property is in real live the master_password_hash!!
			//OBSOLETE:			cs.new Element(248,IRepository.SOLUTIONS,PROPERTY_LOADOPTIONS.getPropertyName(),IRepository.STRING); 
		}

		if (old_repository_version < 20)
		{
			cs.new Element(249, IRepository.PORTALS, PROPERTY_ROWBGCOLORCALCULATION.getPropertyName(), IRepository.STRING);
			cs.new Element(250, IRepository.FORMS, PROPERTY_ROWBGCOLORCALCULATION.getPropertyName(), IRepository.STRING);

			cs.new Element(251, IRepository.GRAPHICALCOMPONENTS, PROPERTY_STYLECLASS.getPropertyName(), IRepository.STRING);
			cs.new Element(252, IRepository.TABPANELS, PROPERTY_STYLECLASS.getPropertyName(), IRepository.STRING);
			cs.new Element(253, IRepository.PORTALS, PROPERTY_STYLECLASS.getPropertyName(), IRepository.STRING);
			cs.new Element(254, IRepository.FIELDS, PROPERTY_STYLECLASS.getPropertyName(), IRepository.STRING);

			cs.new Element(255, IRepository.SOLUTIONS, PROPERTY_I18NTABLENAME.getPropertyName(), IRepository.TABLES).flagAsDeprecated();
			cs.new Element(256, IRepository.SOLUTIONS, PROPERTY_I18NSERVERNAME.getPropertyName(), IRepository.SERVERS).flagAsDeprecated();
			cs.new Element(257, IRepository.BEANS, PROPERTY_ANCHORS.getPropertyName(), IRepository.INTEGER);
			cs.new Element(258, IRepository.TABPANELS, PROPERTY_SCROLLTABS.getPropertyName(), IRepository.BOOLEAN);

			cs.new Element(259, IRepository.GRAPHICALCOMPONENTS, PROPERTY_SHOWCLICK.getPropertyName(), IRepository.BOOLEAN, Boolean.TRUE);
			cs.new Element(260, IRepository.GRAPHICALCOMPONENTS, PROPERTY_SHOWFOCUS.getPropertyName(), IRepository.BOOLEAN, Boolean.TRUE);
		}

		if (old_repository_version < 22)
		{
			cs.new Element(261, IRepository.FORMS, PROPERTY_STYLECLASS.getPropertyName(), IRepository.STRING);
		}

		if (old_repository_version < 24)
		{
			cs.new Element(262, IRepository.FORMS, PROPERTY_ONPREVIOUSRECORDCMDMETHODID.getPropertyName(), IRepository.ELEMENTS);
			cs.new Element(263, IRepository.FORMS, PROPERTY_ONNEXTRECORDCMDMETHODID.getPropertyName(), IRepository.ELEMENTS);
			cs.new Element(264, IRepository.SOLUTIONS, PROPERTY_MODULESNAMES.getPropertyName(), IRepository.STRING);
		}
		if (old_repository_version < 25)
		{
			cs.new Element(265, IRepository.FORMS, PROPERTY_ONSEARCHCMDMETHODID.getPropertyName(), IRepository.ELEMENTS);
			cs.new Element(266, IRepository.RELATIONS, PROPERTY_DUPLICATERELATEDRECORDS.getPropertyName(), IRepository.BOOLEAN);
			cs.new Element(267, IRepository.RELATIONS, PROPERTY_INITIALSORT.getPropertyName(), IRepository.STRING);
			cs.new Element(268, IRepository.VALUELISTS, PROPERTY_USETABLEFILTER.getPropertyName(), IRepository.BOOLEAN);
		}

		if (old_repository_version < 27)
		{
			cs.new Element(269, IRepository.SOLUTIONS, PROPERTY_ONERRORMETHODID.getPropertyName(), IRepository.ELEMENTS);
			cs.new Element(270, IRepository.SOLUTIONS, PROPERTY_LOGINFORMID.getPropertyName(), IRepository.ELEMENTS);
			cs.new Element(271, IRepository.PARTS, PROPERTY_SINKWHENLAST.getPropertyName(), IRepository.BOOLEAN);
			cs.new Element(272, IRepository.BEANS, PROPERTY_ONACTIONMETHODID.getPropertyName(), IRepository.ELEMENTS);
			cs.new Element(273, IRepository.BEANS, PROPERTY_PARAMETERS.getPropertyName(), IRepository.STRING);
		}

		if (old_repository_version < 28)
		{
			cs.new Element(275, IRepository.SOLUTIONS, PROPERTY_MUSTAUTHENTICATE.getPropertyName(), IRepository.BOOLEAN).flagAsMetaData();
			cs.new Element(276, IRepository.MEDIA, PROPERTY_NAME.getPropertyName(), IRepository.STRING);
			cs.new Element(277, IRepository.MEDIA, PROPERTY_BLOBID.getPropertyName(), IRepository.BLOBS);
			cs.new Element(278, IRepository.MEDIA, PROPERTY_MIMETYPE.getPropertyName(), IRepository.STRING);
			cs.new Element(279, IRepository.SOLUTIONS, PROPERTY_SOLUTIONTYPE.getPropertyName(), IRepository.INTEGER).flagAsMetaData();
			cs.new Element(280, IRepository.STYLES, PROPERTY_CSSTEXT.getPropertyName(), IRepository.STRING);
			cs.new Element(PROTECTION_PASSWORD, IRepository.SOLUTIONS, PROPERTY_PROTECTIONPASSWORD.getPropertyName(), IRepository.STRING).flagAsMetaData();
		}

		if (old_repository_version < 29)
		{
			cs.new Element(282, IRepository.FORMS, PROPERTY_ALIASES.getPropertyName(), IRepository.STRING);
			cs.new Element(283, IRepository.TABPANELS, PROPERTY_CLOSEONTABS.getPropertyName(), IRepository.BOOLEAN);
			cs.new Element(284, IRepository.RELATIONS, PROPERTY_ALLOWPARENTDELETEWHENHAVINGRELATEDRECORDS.getPropertyName(), IRepository.BOOLEAN, Boolean.TRUE);
			cs.new Element(285, IRepository.TABLENODES, PROPERTY_ONINSERTMETHODID.getPropertyName(), IRepository.ELEMENTS);
			cs.new Element(286, IRepository.TABLENODES, PROPERTY_ONUPDATEMETHODID.getPropertyName(), IRepository.ELEMENTS);
			cs.new Element(287, IRepository.TABLENODES, PROPERTY_ONDELETEMETHODID.getPropertyName(), IRepository.ELEMENTS);
			cs.new Element(288, IRepository.GRAPHICALCOMPONENTS, PROPERTY_LABELFOR.getPropertyName(), IRepository.STRING);
			cs.new Element(289, IRepository.GRAPHICALCOMPONENTS, PROPERTY_MNEMONIC.getPropertyName(), IRepository.STRING);
			cs.new Element(290, IRepository.TABPANELS, PROPERTY_ONTABCHANGEMETHODID.getPropertyName(), IRepository.ELEMENTS).flagAsDeprecated(377);

			cs.new Element(291, IRepository.TABS, PROPERTY_USENEWFORMINSTANCE.getPropertyName(), IRepository.BOOLEAN);
			cs.new Element(292, IRepository.RELATIONS, PROPERTY_JOINTYPE.getPropertyName(), IRepository.INTEGER);
		}

		if (old_repository_version < 30)
		{
			cs.new Element(293, IRepository.SOLUTIONS, PROPERTY_TEXTORIENTATION.getPropertyName(), IRepository.INTEGER);
		}
		if (old_repository_version < 31)
		{
			cs.new Element(294, IRepository.FORMS, PROPERTY_TRANSPARENT.getPropertyName(), IRepository.BOOLEAN);
			cs.new Element(295, IRepository.FORMS, PROPERTY_EXTENDSFORMID.getPropertyName(), IRepository.ELEMENTS).flagAsDeprecated(379);
			cs.new Element(296, IRepository.GRAPHICALCOMPONENTS, PROPERTY_ONDOUBLECLICKMETHODID.getPropertyName(), IRepository.ELEMENTS);
			cs.new Element(297, IRepository.GRAPHICALCOMPONENTS, PROPERTY_ONRIGHTCLICKMETHODID.getPropertyName(), IRepository.ELEMENTS);
		}
		if (old_repository_version < 32)
		{
			cs.new Element(298, IRepository.FORMS, PROPERTY_ONUNLOADMETHODID.getPropertyName(), IRepository.ELEMENTS);
		}
		if (old_repository_version < 35)
		{
			cs.new Element(299, IRepository.BEANS, PROPERTY_TABSEQ.getPropertyName(), IRepository.INTEGER);
			cs.new Element(300, IRepository.PORTALS, PROPERTY_TABSEQ.getPropertyName(), IRepository.INTEGER);
			cs.new Element(301, IRepository.TABPANELS, PROPERTY_TABSEQ.getPropertyName(), IRepository.INTEGER);
		}

		if (old_repository_version < 36)
		{
			cs.new Element(302, IRepository.FORMS, PROPERTY_ONDRAGMETHODID.getPropertyName(), IRepository.ELEMENTS);
			cs.new Element(303, IRepository.FORMS, PROPERTY_ONDRAGOVERMETHODID.getPropertyName(), IRepository.ELEMENTS);
			cs.new Element(304, IRepository.FORMS, PROPERTY_ONDROPMETHODID.getPropertyName(), IRepository.ELEMENTS);
			cs.new Element(305, IRepository.FORMS, PROPERTY_ONELEMENTFOCUSGAINEDMETHODID.getPropertyName(), IRepository.ELEMENTS);

			cs.new Element(306, IRepository.SOLUTIONS, PROPERTY_ONDATABROADCASTMETHODID.getPropertyName(), IRepository.ELEMENTS);

			cs.new Element(307, IRepository.TABLENODES, PROPERTY_ONAFTERINSERTMETHODID.getPropertyName(), IRepository.ELEMENTS);
			cs.new Element(308, IRepository.TABLENODES, PROPERTY_ONAFTERUPDATEMETHODID.getPropertyName(), IRepository.ELEMENTS);
			cs.new Element(309, IRepository.TABLENODES, PROPERTY_ONAFTERDELETEMETHODID.getPropertyName(), IRepository.ELEMENTS);

			cs.new Element(310, IRepository.FIELDS, PROPERTY_ONRIGHTCLICKMETHODID.getPropertyName(), IRepository.ELEMENTS);

			cs.new Element(311, IRepository.METHODS, PROPERTY_LINENUMBEROFFSET.getPropertyName(), IRepository.INTEGER);

			//new properties which makes others depreciated 
			cs.new Element(312, IRepository.METHODS, PROPERTY_DECLARATION.getPropertyName(), IRepository.STRING);
			cs.new Element(313, IRepository.FORMS, PROPERTY_NAMEDFOUNDSET.getPropertyName(), IRepository.STRING);
			cs.new Element(314, IRepository.FORMS, PROPERTY_DATASOURCE.getPropertyName(), IRepository.DATASOURCES);
			cs.new Element(315, IRepository.RELATIONS, PROPERTY_PRIMARYDATASOURCE.getPropertyName(), IRepository.DATASOURCES); //$NON-NLS-2$
			cs.new Element(316, IRepository.RELATIONS, PROPERTY_FOREIGNDATASOURCE.getPropertyName(), IRepository.DATASOURCES); //$NON-NLS-2$
			cs.new Element(317, IRepository.VALUELISTS, PROPERTY_DATASOURCE.getPropertyName(), IRepository.DATASOURCES);
			cs.new Element(318, IRepository.SOLUTIONS, PROPERTY_I18NDATASOURCE.getPropertyName(), IRepository.DATASOURCES);
			cs.new Element(319, IRepository.TABLENODES, PROPERTY_DATASOURCE.getPropertyName(), IRepository.DATASOURCES);

			//custom properties support
			cs.new Element(320, IRepository.FORMS, PROPERTY_CUSTOMPROPERTIES.getPropertyName(), IRepository.STRING);
			cs.new Element(321, IRepository.BEANS, PROPERTY_CUSTOMPROPERTIES.getPropertyName(), IRepository.STRING);
			cs.new Element(322, IRepository.PORTALS, PROPERTY_CUSTOMPROPERTIES.getPropertyName(), IRepository.STRING);
			cs.new Element(323, IRepository.TABPANELS, PROPERTY_CUSTOMPROPERTIES.getPropertyName(), IRepository.STRING);
			cs.new Element(324, IRepository.TABS, PROPERTY_CUSTOMPROPERTIES.getPropertyName(), IRepository.STRING);
			cs.new Element(325, IRepository.GRAPHICALCOMPONENTS, PROPERTY_CUSTOMPROPERTIES.getPropertyName(), IRepository.STRING);
			cs.new Element(326, IRepository.RELATIONS, PROPERTY_CUSTOMPROPERTIES.getPropertyName(), IRepository.STRING);
			cs.new Element(327, IRepository.SHAPES, PROPERTY_CUSTOMPROPERTIES.getPropertyName(), IRepository.STRING);
			cs.new Element(328, IRepository.RECTSHAPES, PROPERTY_CUSTOMPROPERTIES.getPropertyName(), IRepository.STRING);
			cs.new Element(329, IRepository.VALUELISTS, PROPERTY_CUSTOMPROPERTIES.getPropertyName(), IRepository.STRING);
			cs.new Element(330, IRepository.LINES, PROPERTY_CUSTOMPROPERTIES.getPropertyName(), IRepository.STRING);
			cs.new Element(331, IRepository.PARTS, PROPERTY_CUSTOMPROPERTIES.getPropertyName(), IRepository.STRING);
			cs.new Element(332, IRepository.METHODS, PROPERTY_CUSTOMPROPERTIES.getPropertyName(), IRepository.STRING);
			cs.new Element(333, IRepository.SCRIPTVARIABLES, PROPERTY_CUSTOMPROPERTIES.getPropertyName(), IRepository.STRING);
			cs.new Element(334, IRepository.SCRIPTCALCULATIONS, PROPERTY_CUSTOMPROPERTIES.getPropertyName(), IRepository.STRING);
			cs.new Element(335, IRepository.TABLENODES, PROPERTY_CUSTOMPROPERTIES.getPropertyName(), IRepository.STRING);
			cs.new Element(336, IRepository.AGGREGATEVARIABLES, PROPERTY_CUSTOMPROPERTIES.getPropertyName(), IRepository.STRING);
			cs.new Element(337, IRepository.SOLUTIONS, PROPERTY_CUSTOMPROPERTIES.getPropertyName(), IRepository.STRING);
			cs.new Element(338, IRepository.STYLES, PROPERTY_CUSTOMPROPERTIES.getPropertyName(), IRepository.STRING);
			cs.new Element(339, IRepository.MEDIA, PROPERTY_CUSTOMPROPERTIES.getPropertyName(), IRepository.STRING);
			cs.new Element(340, IRepository.FIELDS, PROPERTY_CUSTOMPROPERTIES.getPropertyName(), IRepository.STRING);
			cs.new Element(341, IRepository.RELATION_ITEMS, PROPERTY_CUSTOMPROPERTIES.getPropertyName(), IRepository.STRING);

			cs.new Element(342, IRepository.PORTALS, PROPERTY_ONDRAGMETHODID.getPropertyName(), IRepository.ELEMENTS);
			cs.new Element(343, IRepository.PORTALS, PROPERTY_ONDRAGOVERMETHODID.getPropertyName(), IRepository.ELEMENTS);
			cs.new Element(344, IRepository.PORTALS, PROPERTY_ONDROPMETHODID.getPropertyName(), IRepository.ELEMENTS);

			cs.new Element(345, IRepository.FORMS, PROPERTY_ONELEMENTFOCUSLOSTMETHODID.getPropertyName(), IRepository.ELEMENTS);
			cs.new Element(346, IRepository.SCRIPTCALCULATIONS, PROPERTY_DECLARATION.getPropertyName(), IRepository.STRING);
			cs.new Element(347, IRepository.SCRIPTCALCULATIONS, PROPERTY_LINENUMBEROFFSET.getPropertyName(), IRepository.INTEGER);
		}

		if (old_repository_version < 37)
		{
			cs.new Element(348, IRepository.VALUELISTS, PROPERTY_FALLBACKVALUELISTID.getPropertyName(), IRepository.ELEMENTS);
			cs.new Element(349, IRepository.SOLUTIONS, PROPERTY_ONINITMETHODID.getPropertyName(), IRepository.ELEMENTS);
			cs.new Element(350, IRepository.FORMS, PROPERTY_ONRESIZEMETHODID.getPropertyName(), IRepository.ELEMENTS);
			cs.new Element(351, IRepository.TEMPLATES, PROPERTY_RESOURCETYPE.getPropertyName(), IRepository.INTEGER);
			cs.new Element(352, IRepository.TEMPLATES, PROPERTY_CONTENT.getPropertyName(), IRepository.STRING);
			cs.new Element(353, IRepository.TEMPLATES, PROPERTY_CUSTOMPROPERTIES.getPropertyName(), IRepository.STRING);
		}

		// 38 is for workspace update
		if (old_repository_version < 39)
		{
			cs.new Element(354, IRepository.FORMS, PROPERTY_ONDRAGENDMETHODID.getPropertyName(), IRepository.ELEMENTS);
			cs.new Element(355, IRepository.PORTALS, PROPERTY_ONDRAGENDMETHODID.getPropertyName(), IRepository.ELEMENTS);
			cs.new Element(356, IRepository.FORMS, PROPERTY_ENCAPSULATION.getPropertyName(), IRepository.INTEGER);
			cs.new Element(357, IRepository.SCRIPTVARIABLES, PROPERTY_COMMENT.getPropertyName(), IRepository.STRING);
			cs.new Element(358, IRepository.FORMS, PROPERTY_ONRENDERMETHODID.getPropertyName(), IRepository.ELEMENTS);
			cs.new Element(359, IRepository.PORTALS, PROPERTY_ONRENDERMETHODID.getPropertyName(), IRepository.ELEMENTS);
			cs.new Element(360, IRepository.FIELDS, PROPERTY_ONRENDERMETHODID.getPropertyName(), IRepository.ELEMENTS);
			cs.new Element(361, IRepository.GRAPHICALCOMPONENTS, PROPERTY_ONRENDERMETHODID.getPropertyName(), IRepository.ELEMENTS);

			cs.new Element(362, IRepository.BEANS, PROPERTY_VISIBLE.getPropertyName(), IRepository.BOOLEAN, Boolean.TRUE);
			cs.new Element(363, IRepository.PORTALS, PROPERTY_VISIBLE.getPropertyName(), IRepository.BOOLEAN, Boolean.TRUE);
			cs.new Element(364, IRepository.TABPANELS, PROPERTY_VISIBLE.getPropertyName(), IRepository.BOOLEAN, Boolean.TRUE);
			cs.new Element(365, IRepository.GRAPHICALCOMPONENTS, PROPERTY_VISIBLE.getPropertyName(), IRepository.BOOLEAN, Boolean.TRUE);
			cs.new Element(366, IRepository.SHAPES, PROPERTY_VISIBLE.getPropertyName(), IRepository.BOOLEAN, Boolean.TRUE);
			cs.new Element(367, IRepository.RECTSHAPES, PROPERTY_VISIBLE.getPropertyName(), IRepository.BOOLEAN, Boolean.TRUE);
			cs.new Element(368, IRepository.FIELDS, PROPERTY_VISIBLE.getPropertyName(), IRepository.BOOLEAN, Boolean.TRUE);

			cs.new Element(369, IRepository.BEANS, PROPERTY_ENABLED.getPropertyName(), IRepository.BOOLEAN, Boolean.TRUE);
			cs.new Element(370, IRepository.PORTALS, PROPERTY_ENABLED.getPropertyName(), IRepository.BOOLEAN, Boolean.TRUE);
			cs.new Element(371, IRepository.TABPANELS, PROPERTY_ENABLED.getPropertyName(), IRepository.BOOLEAN, Boolean.TRUE);
			cs.new Element(372, IRepository.GRAPHICALCOMPONENTS, PROPERTY_ENABLED.getPropertyName(), IRepository.BOOLEAN, Boolean.TRUE);
			cs.new Element(373, IRepository.SHAPES, PROPERTY_ENABLED.getPropertyName(), IRepository.BOOLEAN, Boolean.TRUE);
			cs.new Element(374, IRepository.RECTSHAPES, PROPERTY_ENABLED.getPropertyName(), IRepository.BOOLEAN, Boolean.TRUE);
			cs.new Element(375, IRepository.FIELDS, PROPERTY_ENABLED.getPropertyName(), IRepository.BOOLEAN, Boolean.TRUE);

			cs.new Element(376, IRepository.PORTALS, PROPERTY_RESIZABLE.getPropertyName(), IRepository.BOOLEAN);
			cs.new Element(377, IRepository.TABPANELS, PROPERTY_ONCHANGEMETHODID.getPropertyName(), IRepository.ELEMENTS);
		}

		if (old_repository_version < 40)
		{
			cs.new Element(378, IRepository.SHAPES, PROPERTY_ANCHORS.getPropertyName(), IRepository.INTEGER);
			cs.new Element(379, IRepository.FORMS, PROPERTY_EXTENDSID.getPropertyName(), IRepository.ELEMENTS);
			cs.new Element(380, IRepository.BEANS, PROPERTY_EXTENDSID.getPropertyName(), IRepository.ELEMENTS);
			cs.new Element(381, IRepository.FIELDS, PROPERTY_EXTENDSID.getPropertyName(), IRepository.ELEMENTS);
			cs.new Element(382, IRepository.GRAPHICALCOMPONENTS, PROPERTY_EXTENDSID.getPropertyName(), IRepository.ELEMENTS);
			cs.new Element(383, IRepository.PARTS, PROPERTY_EXTENDSID.getPropertyName(), IRepository.ELEMENTS);
			cs.new Element(384, IRepository.PORTALS, PROPERTY_EXTENDSID.getPropertyName(), IRepository.ELEMENTS);
			cs.new Element(385, IRepository.TABS, PROPERTY_EXTENDSID.getPropertyName(), IRepository.ELEMENTS);
			cs.new Element(386, IRepository.TABPANELS, PROPERTY_EXTENDSID.getPropertyName(), IRepository.ELEMENTS);
			cs.new Element(387, IRepository.RECTSHAPES, PROPERTY_EXTENDSID.getPropertyName(), IRepository.ELEMENTS);
			cs.new Element(388, IRepository.SHAPES, PROPERTY_EXTENDSID.getPropertyName(), IRepository.ELEMENTS);
		}

		if (old_repository_version < 41)
		{
			cs.new Element(389, IRepository.GRAPHICALCOMPONENTS, PROPERTY_FORMAT.getPropertyName(), IRepository.STRING);
		}
		//##add property adds here

		/*
		 * cs.new Element(262,IRepository.PARTS,"styleClass",IRepository.STRING); Element(267,IRepository.FORMS,"viewOptions",IRepository.String); //maybe put
		 * in existing view property Element(269,IRepository.GRAPHICALCOMPONENTS,"fontOptions",IRepository.STRING));//condensed,strike
		 * trough,underline,outline,drop shadow
		 */
		return cs;
	}

	public static class TypedProperty<T>
	{
		private final String name;

		public TypedProperty(String name)
		{
			this.name = name;
		}

		public String getPropertyName()
		{
			return name;
		}
	}
}
