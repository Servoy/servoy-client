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

package com.servoy.j2db.persistence;

import com.servoy.base.persistence.constants.IContentSpecConstantsBase;


/**
 * @author acostescu
 */
public interface IContentSpecConstants extends IContentSpecConstantsBase
{

	public static final String PROPERTY_ALLOWBREAKACROSSPAGEBOUNDS = "allowBreakAcrossPageBounds"; //$NON-NLS-1$
	public static final String PROPERTY_ALLOWCREATIONRELATEDRECORDS = "allowCreationRelatedRecords"; //$NON-NLS-1$
	public static final String PROPERTY_ALLOWPARENTDELETEWHENHAVINGRELATEDRECORDS = "allowParentDeleteWhenHavingRelatedRecords"; //$NON-NLS-1$
	public static final String PROPERTY_CLOSEONTABS = "closeOnTabs"; //$NON-NLS-1$
	public static final String PROPERTY_DELETERELATEDRECORDS = "deleteRelatedRecords"; //$NON-NLS-1$
	public static final String PROPERTY_DISCARDREMAINDERAFTERBREAK = "discardRemainderAfterBreak"; //$NON-NLS-1$
	public static final String PROPERTY_DISPLAYSTAGS = "displaysTags"; //$NON-NLS-1$
	public static final String PROPERTY_DUPLICATERELATEDRECORDS = "duplicateRelatedRecords"; //$NON-NLS-1$
	public static final String PROPERTY_EDITABLE = "editable"; //$NON-NLS-1$
	public static final String PROPERTY_ENABLED = "enabled"; //$NON-NLS-1$
	public static final String PROPERTY_EXISTSINDB = "existsInDB"; //$NON-NLS-1$
	public static final String PROPERTY_LOCKED = "locked"; //$NON-NLS-1$
	public static final String PROPERTY_MULTILINE = "multiLine"; //$NON-NLS-1$
	public static final String PROPERTY_PAGEBREAKBEFORE = "pageBreakBefore"; //$NON-NLS-1$
	public static final String PROPERTY_PRINTABLE = "printable"; //$NON-NLS-1$
	public static final String PROPERTY_REORDERABLE = "reorderable"; //$NON-NLS-1$
	public static final String PROPERTY_RESIZEBLE = "resizeble"; //$NON-NLS-1$
	public static final String PROPERTY_RESIZABLE = "resizable"; //$NON-NLS-1$
	public static final String PROPERTY_RESTARTPAGENUMBER = "restartPageNumber"; //$NON-NLS-1$
	public static final String PROPERTY_SCROLLTABS = "scrollTabs"; //$NON-NLS-1$
	public static final String PROPERTY_SELECTONENTER = "selectOnEnter"; //$NON-NLS-1$
	public static final String PROPERTY_SHOWCLICK = "showClick"; //$NON-NLS-1$
	public static final String PROPERTY_SHOWFOCUS = "showFocus"; //$NON-NLS-1$
	public static final String PROPERTY_SHOWHORIZONTALLINES = "showHorizontalLines"; //$NON-NLS-1$
	public static final String PROPERTY_SHOWINMENU = "showInMenu"; //$NON-NLS-1$
	public static final String PROPERTY_SHOWVERTICALLINES = "showVerticalLines"; //$NON-NLS-1$
	public static final String PROPERTY_SINKWHENLAST = "sinkWhenLast"; //$NON-NLS-1$
	public static final String PROPERTY_SORTABLE = "sortable"; //$NON-NLS-1$
	public static final String PROPERTY_TRANSPARENT = "transparent"; //$NON-NLS-1$
	public static final String PROPERTY_USENEWFORMINSTANCE = "useNewFormInstance"; //$NON-NLS-1$
	public static final String PROPERTY_USERTF = "useRTF"; //$NON-NLS-1$
	public static final String PROPERTY_USESUI = "usesUI"; //$NON-NLS-1$
	public static final String PROPERTY_USETABLEFILTER = "useTableFilter"; //$NON-NLS-1$
	public static final String PROPERTY_VISIBLE = "visible"; //$NON-NLS-1$
	public static final String PROPERTY_INTERCELLSPACING = "intercellSpacing"; //$NON-NLS-1$
	public static final String PROPERTY_MARGIN = "margin"; //$NON-NLS-1$
	public static final String PROPERTY_SELECTIONMODE = "selectionMode"; //$NON-NLS-1$ //
	public static final String PROPERTY_ENCAPSULATION = "encapsulation"; //$NON-NLS-1$
	public static final String PROPERTY_ADDEMPTYVALUE = "addEmptyValue"; //$NON-NLS-1$
	public static final String PROPERTY_ANCHORS = "anchors"; //$NON-NLS-1$
	public static final String PROPERTY_BLOBID = "blobId"; //$NON-NLS-1$
	public static final String PROPERTY_CONTAINSFORMID = "containsFormID"; //$NON-NLS-1$
	public static final String PROPERTY_EXTENDSFORMID = "extendsFormID"; //$NON-NLS-1$
	public static final String PROPERTY_EXTENDSID = "extendsID"; //$NON-NLS-1$
	public static final String PROPERTY_FALLBACKVALUELISTID = "fallbackValueListID"; //$NON-NLS-1$
	public static final String PROPERTY_FIRSTFORMID = "firstFormID"; //$NON-NLS-1$
	public static final String PROPERTY_FORMINDEX = "formIndex"; //$NON-NLS-1$
	public static final String PROPERTY_HEIGHT = "height"; //$NON-NLS-1$
	public static final String PROPERTY_HORIZONTALALIGNMENT = "horizontalAlignment"; //$NON-NLS-1$
	public static final String PROPERTY_IMAGEMEDIAID = "imageMediaID"; //$NON-NLS-1$
	public static final String PROPERTY_JOINTYPE = "joinType"; //$NON-NLS-1$
	public static final String PROPERTY_LINENUMBEROFFSET = "lineNumberOffset"; //$NON-NLS-1$
	public static final String PROPERTY_LINESIZE = "lineSize"; //$NON-NLS-1$
	public static final String PROPERTY_LOGINFORMID = "loginFormID"; //$NON-NLS-1$
	public static final String PROPERTY_MEDIAOPTIONS = "mediaOptions"; //$NON-NLS-1$
	public static final String PROPERTY_NAVIGATORID = "navigatorID"; //$NON-NLS-1$
	public static final String PROPERTY_ONAFTERCREATEMETHODID = "onAfterCreateMethodID"; //$NON-NLS-1$
	public static final String PROPERTY_ONAFTERDELETEMETHODID = "onAfterDeleteMethodID"; //$NON-NLS-1$
	public static final String PROPERTY_ONAFTERFINDMETHODID = "onAfterFindMethodID"; //$NON-NLS-1$
	public static final String PROPERTY_ONAFTERINSERTMETHODID = "onAfterInsertMethodID"; //$NON-NLS-1$
	public static final String PROPERTY_ONAFTERSEARCHMETHODID = "onAfterSearchMethodID"; //$NON-NLS-1$
	public static final String PROPERTY_ONAFTERUPDATEMETHODID = "onAfterUpdateMethodID"; //$NON-NLS-1$
	public static final String PROPERTY_ONCLOSEMETHODID = "onCloseMethodID"; //$NON-NLS-1$
	public static final String PROPERTY_ONCREATEMETHODID = "onCreateMethodID"; //$NON-NLS-1$
	public static final String PROPERTY_ONDATABROADCASTMETHODID = "onDataBroadcastMethodID"; //$NON-NLS-1$
	public static final String PROPERTY_ONDATACHANGEMETHODID = "onDataChangeMethodID"; //$NON-NLS-1$
	public static final String PROPERTY_ONDELETEALLRECORDSCMDMETHODID = "onDeleteAllRecordsCmdMethodID"; //$NON-NLS-1$
	public static final String PROPERTY_ONDELETEMETHODID = "onDeleteMethodID"; //$NON-NLS-1$
	public static final String PROPERTY_ONDELETERECORDCMDMETHODID = "onDeleteRecordCmdMethodID"; //$NON-NLS-1$
	public static final String PROPERTY_ONDOUBLECLICKMETHODID = "onDoubleClickMethodID"; //$NON-NLS-1$
	public static final String PROPERTY_ONDRAGENDMETHODID = "onDragEndMethodID"; //$NON-NLS-1$
	public static final String PROPERTY_ONDRAGMETHODID = "onDragMethodID"; //$NON-NLS-1$
	public static final String PROPERTY_ONDRAGOVERMETHODID = "onDragOverMethodID"; //$NON-NLS-1$
	public static final String PROPERTY_ONDROPMETHODID = "onDropMethodID"; //$NON-NLS-1$
	public static final String PROPERTY_ONDUPLICATERECORDCMDMETHODID = "onDuplicateRecordCmdMethodID"; //$NON-NLS-1$
	public static final String PROPERTY_ONELEMENTFOCUSGAINEDMETHODID = "onElementFocusGainedMethodID"; //$NON-NLS-1$
	public static final String PROPERTY_ONELEMENTFOCUSLOSTMETHODID = "onElementFocusLostMethodID"; //$NON-NLS-1$
	public static final String PROPERTY_ONERRORMETHODID = "onErrorMethodID"; //$NON-NLS-1$
	public static final String PROPERTY_ONFINDCMDMETHODID = "onFindCmdMethodID"; //$NON-NLS-1$
	public static final String PROPERTY_ONFINDMETHODID = "onFindMethodID"; //$NON-NLS-1$
	public static final String PROPERTY_ONFOCUSGAINEDMETHODID = "onFocusGainedMethodID"; //$NON-NLS-1$
	public static final String PROPERTY_ONFOCUSLOSTMETHODID = "onFocusLostMethodID"; //$NON-NLS-1$
	public static final String PROPERTY_ONINITMETHODID = "onInitMethodID"; //$NON-NLS-1$
	public static final String PROPERTY_ONINSERTMETHODID = "onInsertMethodID"; //$NON-NLS-1$
	public static final String PROPERTY_ONINVERTRECORDSCMDMETHODID = "onInvertRecordsCmdMethodID"; //$NON-NLS-1$
	public static final String PROPERTY_ONNEWRECORDCMDMETHODID = "onNewRecordCmdMethodID"; //$NON-NLS-1$
	public static final String PROPERTY_ONNEXTRECORDCMDMETHODID = "onNextRecordCmdMethodID"; //$NON-NLS-1$
	public static final String PROPERTY_ONOMITRECORDCMDMETHODID = "onOmitRecordCmdMethodID"; //$NON-NLS-1$
	public static final String PROPERTY_ONOPENMETHODID = "onOpenMethodID"; //$NON-NLS-1$
	public static final String PROPERTY_ONPREVIOUSRECORDCMDMETHODID = "onPreviousRecordCmdMethodID"; //$NON-NLS-1$
	public static final String PROPERTY_ONPRINTPREVIEWCMDMETHODID = "onPrintPreviewCmdMethodID"; //$NON-NLS-1$
	public static final String PROPERTY_ONRECORDEDITSTARTMETHODID = "onRecordEditStartMethodID"; //$NON-NLS-1$
	public static final String PROPERTY_ONRECORDEDITSTOPMETHODID = "onRecordEditStopMethodID"; //$NON-NLS-1$
	public static final String PROPERTY_ONRENDERMETHODID = "onRenderMethodID"; //$NON-NLS-1$
	public static final String PROPERTY_ONRESIZEMETHODID = "onResizeMethodID"; //$NON-NLS-1$
	public static final String PROPERTY_ONRIGHTCLICKMETHODID = "onRightClickMethodID"; //$NON-NLS-1$
	public static final String PROPERTY_ONSEARCHCMDMETHODID = "onSearchCmdMethodID"; //$NON-NLS-1$
	public static final String PROPERTY_ONSEARCHMETHODID = "onSearchMethodID"; //$NON-NLS-1$
	public static final String PROPERTY_ONSHOWALLRECORDSCMDMETHODID = "onShowAllRecordsCmdMethodID"; //$NON-NLS-1$
	public static final String PROPERTY_ONSHOWOMITTEDRECORDSCMDMETHODID = "onShowOmittedRecordsCmdMethodID"; //$NON-NLS-1$
	public static final String PROPERTY_ONSORTCMDMETHODID = "onSortCmdMethodID"; //$NON-NLS-1$
	public static final String PROPERTY_ONTABCHANGEMETHODID = "onTabChangeMethodID"; //$NON-NLS-1$
	public static final String PROPERTY_ONCHANGEMETHODID = "onChangeMethodID"; //$NON-NLS-1$
	public static final String PROPERTY_ONUNLOADMETHODID = "onUnLoadMethodID"; //$NON-NLS-1$
	public static final String PROPERTY_ONUPDATEMETHODID = "onUpdateMethodID"; //$NON-NLS-1$
	public static final String PROPERTY_OPERATOR = "operator"; //$NON-NLS-1$
	public static final String PROPERTY_PAGEBREAKAFTEROCCURRENCE = "pageBreakAfterOccurrence"; //$NON-NLS-1$
	public static final String PROPERTY_PAPERPRINTSCALE = "paperPrintScale"; //$NON-NLS-1$
	public static final String PROPERTY_PARTTYPE = "partType"; //$NON-NLS-1$
	public static final String PROPERTY_PRINTSLIDING = "printSliding"; //$NON-NLS-1$
	public static final String PROPERTY_RESOURCETYPE = "resourceType"; //$NON-NLS-1$;
	public static final String PROPERTY_RETURNDATAPROVIDERS = "returnDataProviders"; //$NON-NLS-1$
	public static final String PROPERTY_ROLLOVERCURSOR = "rolloverCursor"; //$NON-NLS-1$
	public static final String PROPERTY_ROLLOVERIMAGEMEDIAID = "rolloverImageMediaID"; //$NON-NLS-1$
	public static final String PROPERTY_ROTATION = "rotation"; //$NON-NLS-1$
	public static final String PROPERTY_ROUNDEDRADIUS = "roundedRadius"; //$NON-NLS-1$
	public static final String PROPERTY_ROWHEIGHT = "rowHeight"; //$NON-NLS-1$
	public static final String PROPERTY_SCROLLBARS = "scrollbars"; //$NON-NLS-1$
	public static final String PROPERTY_SEQUENCE = "sequence"; //$NON-NLS-1$
	public static final String PROPERTY_SHAPETYPE = "shapeType"; //$NON-NLS-1$
	public static final String PROPERTY_SHOWDATAPROVIDERS = "showDataProviders"; //$NON-NLS-1$
	public static final String PROPERTY_TABORIENTATION = "tabOrientation"; //$NON-NLS-1$
	public static final String PROPERTY_TABSEQ = "tabSeq"; //$NON-NLS-1$
	public static final String PROPERTY_TEXTORIENTATION = "textOrientation"; //$NON-NLS-1$
	public static final String PROPERTY_TYPE = "type"; //$NON-NLS-1$
	public static final String PROPERTY_VALUELISTID = "valuelistID"; //$NON-NLS-1$
	public static final String PROPERTY_VALUELISTTYPE = "valueListType"; //$NON-NLS-1$
	public static final String PROPERTY_VARIABLETYPE = "variableType"; //$NON-NLS-1$
	public static final String PROPERTY_VERTICALALIGNMENT = "verticalAlignment"; //$NON-NLS-1$
	public static final String PROPERTY_BACKGROUND = "background"; //$NON-NLS-1$
	public static final String PROPERTY_FOREGROUND = "foreground"; //$NON-NLS-1$
	public static final String PROPERTY_SELECTEDTABCOLOR = "selectedTabColor"; //$NON-NLS-1$
	public static final String PROPERTY_WIDTH = "width"; //$NON-NLS-1$
	public static final String PROPERTY_ALIASES = "aliases"; //$NON-NLS-1$
	public static final String PROPERTY_BEANCLASSNAME = "beanClassName"; //$NON-NLS-1$
	public static final String PROPERTY_BEANXML = "beanXML"; //$NON-NLS-1$
	public static final String PROPERTY_BORDERTYPE = "borderType"; //$NON-NLS-1$
	public static final String PROPERTY_COMMENT = "comment"; //$NON-NLS-1$
	public static final String PROPERTY_CONTENT = "content"; //$NON-NLS-1$;
	public static final String PROPERTY_CSSTEXT = "CSSText"; //$NON-NLS-1$
	public static final String PROPERTY_CUSTOMVALUES = "customValues"; //$NON-NLS-1$
	public static final String PROPERTY_DATAPROVIDERID1 = "dataProviderID1"; //$NON-NLS-1$
	public static final String PROPERTY_DATAPROVIDERID2 = "dataProviderID2"; //$NON-NLS-1$
	public static final String PROPERTY_DATAPROVIDERID3 = "dataProviderID3"; //$NON-NLS-1$
	public static final String PROPERTY_DATAPROVIDERIDTOAGGREGATE = "dataProviderIDToAggregate"; //$NON-NLS-1$
	public static final String PROPERTY_DECLARATION = "declaration"; //$NON-NLS-1$
	public static final String PROPERTY_DEFAULTPAGEFORMAT = "defaultPageFormat"; //$NON-NLS-1$
	public static final String PROPERTY_DEFAULTVALUE = "defaultValue"; //$NON-NLS-1$
	public static final String PROPERTY_FONTTYPE = "fontType"; //$NON-NLS-1$
	public static final String PROPERTY_FOREIGNCOLUMNNAME = "foreignColumnName"; //$NON-NLS-1$
	public static final String PROPERTY_FOREIGNDATASOURCE = "foreignDataSource"; //$NON-NLS-1$
	public static final String PROPERTY_FORMAT = "format"; //$NON-NLS-1$
	public static final String PROPERTY_GROUPBYDATAPROVIDERIDS = "groupbyDataProviderIDs"; //$NON-NLS-1$
	public static final String PROPERTY_GROUPID = "groupID"; //$NON-NLS-1$
	public static final String PROPERTY_I18NDATASOURCE = "i18nDataSource"; //$NON-NLS-1$
	public static final String PROPERTY_INITIALSORT = "initialSort"; //$NON-NLS-1$
	public static final String PROPERTY_LABELFOR = "labelFor"; //$NON-NLS-1$
	public static final String PROPERTY_METHODCODE = "methodCode"; //$NON-NLS-1$
	public static final String PROPERTY_MIMETYPE = "mimeType"; //$NON-NLS-1$
	public static final String PROPERTY_MNEMONIC = "mnemonic"; //$NON-NLS-1$
	public static final String PROPERTY_MODULESNAMES = "modulesNames"; //$NON-NLS-1$
	public static final String PROPERTY_NAMEDFOUNDSET = "namedFoundSet"; //$NON-NLS-1$
	public static final String PROPERTY_NAME = "name"; //$NON-NLS-1$
	public static final String PROPERTY_PARAMETERS = "parameters"; //$NON-NLS-1$
	public static final String PROPERTY_POINTS = "points"; //$NON-NLS-1$
	public static final String PROPERTY_PRIMARYDATAPROVIDERID = "primaryDataProviderID"; //$NON-NLS-1$
	public static final String PROPERTY_PRIMARYDATASOURCE = "primaryDataSource"; //$NON-NLS-1$
	public static final String PROPERTY_ROWBGCOLORCALCULATION = "rowBGColorCalculation"; //$NON-NLS-1$
	public static final String PROPERTY_SCOPENAME = "scopeName"; //$NON-NLS-1$
	public static final String PROPERTY_SEPARATOR = "separator"; //$NON-NLS-1$
	public static final String PROPERTY_SORTOPTIONS = "sortOptions"; //$NON-NLS-1$
	public static final String PROPERTY_STYLECLASS = "styleClass"; //$NON-NLS-1$
	public static final String PROPERTY_STYLENAME = "styleName"; //$NON-NLS-1$
	public static final String PROPERTY_TITLETEXT = "titleText"; //$NON-NLS-1$
	public static final String PROPERTY_TOOLTIPTEXT = "toolTipText"; //$NON-NLS-1$
	public static final String PROPERTY_TEXTROTATION = "textRotation"; //$NON-NLS-1$

}