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
@SuppressWarnings("nls")
public interface IContentSpecConstants extends IContentSpecConstantsBase
{
	public static final String PROPERTY_ALLOWBREAKACROSSPAGEBOUNDS = "allowBreakAcrossPageBounds";
	public static final String PROPERTY_ALLOWCREATIONRELATEDRECORDS = "allowCreationRelatedRecords";
	public static final String PROPERTY_ALLOWPARENTDELETEWHENHAVINGRELATEDRECORDS = "allowParentDeleteWhenHavingRelatedRecords";
	public static final String PROPERTY_CLOSEONTABS = "closeOnTabs";
	public static final String PROPERTY_DELETERELATEDRECORDS = "deleteRelatedRecords";
	public static final String PROPERTY_DISCARDREMAINDERAFTERBREAK = "discardRemainderAfterBreak";
	public static final String PROPERTY_DISPLAYSTAGS = "displaysTags";
	public static final String PROPERTY_DUPLICATERELATEDRECORDS = "duplicateRelatedRecords";
	public static final String PROPERTY_EDITABLE = "editable";
	public static final String PROPERTY_ENABLED = "enabled";
	public static final String PROPERTY_EXISTSINDB = "existsInDB";
	public static final String PROPERTY_LOCKED = "locked";
	public static final String PROPERTY_MULTILINE = "multiLine";
	public static final String PROPERTY_PAGEBREAKBEFORE = "pageBreakBefore";
	public static final String PROPERTY_PRINTABLE = "printable";
	public static final String PROPERTY_REORDERABLE = "reorderable";
	public static final String PROPERTY_RESIZEBLE = "resizeble";
	public static final String PROPERTY_RESIZABLE = "resizable";
	public static final String PROPERTY_RESTARTPAGENUMBER = "restartPageNumber";
	public static final String PROPERTY_SCROLLTABS = "scrollTabs";
	public static final String PROPERTY_SELECTONENTER = "selectOnEnter";
	public static final String PROPERTY_SHOWCLICK = "showClick";
	public static final String PROPERTY_SHOWFOCUS = "showFocus";
	public static final String PROPERTY_SHOWHORIZONTALLINES = "showHorizontalLines";
	public static final String PROPERTY_SHOWINMENU = "showInMenu";
	public static final String PROPERTY_SHOWVERTICALLINES = "showVerticalLines";
	public static final String PROPERTY_SINKWHENLAST = "sinkWhenLast";
	public static final String PROPERTY_SORTABLE = "sortable";
	public static final String PROPERTY_TRANSPARENT = "transparent";
	public static final String PROPERTY_USENEWFORMINSTANCE = "useNewFormInstance";
	public static final String PROPERTY_USERTF = "useRTF";
	public static final String PROPERTY_USESUI = "usesUI";
	public static final String PROPERTY_USETABLEFILTER = "useTableFilter";
	public static final String PROPERTY_VISIBLE = "visible";
	public static final String PROPERTY_INTERCELLSPACING = "intercellSpacing";
	public static final String PROPERTY_MARGIN = "margin";
	public static final String PROPERTY_SELECTIONMODE = "selectionMode"; //
	public static final String PROPERTY_ELEMENTID = "elementId";
	public static final String PROPERTY_ENCAPSULATION = "encapsulation";
	public static final String PROPERTY_DEPRECATED = "deprecated";
	public static final String PROPERTY_ADDEMPTYVALUE = "addEmptyValue";
	public static final String PROPERTY_ANCHORS = "anchors";
	public static final String PROPERTY_LABELS = "labels";
	public static final String PROPERTY_BLOBID = "blobId";
	public static final String PROPERTY_CONTAINSFORMID = "containsFormID";
	public static final String PROPERTY_EXTENDSFORMID = "extendsFormID";
	public static final String PROPERTY_EXTENDSID = "extendsID";
	public static final String PROPERTY_FALLBACKVALUELISTID = "fallbackValueListID";
	public static final String PROPERTY_FIRSTFORMID = "firstFormID";
	public static final String PROPERTY_FORMINDEX = "formIndex";
	public static final String PROPERTY_HEIGHT = "height";
	public static final String PROPERTY_HORIZONTALALIGNMENT = "horizontalAlignment";
	public static final String PROPERTY_IMAGEMEDIAID = "imageMediaID";
	public static final String PROPERTY_JOINTYPE = "joinType";
	public static final String PROPERTY_LINENUMBEROFFSET = "lineNumberOffset";
	public static final String PROPERTY_LINESIZE = "lineSize";
	public static final String PROPERTY_LOGINFORMID = "loginFormID";
	public static final String PROPERTY_MEDIAOPTIONS = "mediaOptions";
	public static final String PROPERTY_NAVIGATORID = "navigatorID";
	public static final String PROPERTY_ONAFTERCREATEMETHODID = "onAfterCreateMethodID";
	public static final String PROPERTY_ONAFTERDELETEMETHODID = "onAfterDeleteMethodID";
	public static final String PROPERTY_ONAFTERFINDMETHODID = "onAfterFindMethodID";
	public static final String PROPERTY_ONAFTERINSERTMETHODID = "onAfterInsertMethodID";
	public static final String PROPERTY_ONAFTERSEARCHMETHODID = "onAfterSearchMethodID";
	public static final String PROPERTY_ONAFTERUPDATEMETHODID = "onAfterUpdateMethodID";
	public static final String PROPERTY_ONFOUNDSETLOADMETHODID = "onFoundSetLoadMethodID";
	public static final String PROPERTY_ONCLOSEMETHODID = "onCloseMethodID";
	public static final String PROPERTY_ONCREATEMETHODID = "onCreateMethodID";
	public static final String PROPERTY_ONDATABROADCASTMETHODID = "onDataBroadcastMethodID";
	public static final String PROPERTY_ONDATACHANGEMETHODID = "onDataChangeMethodID";
	public static final String PROPERTY_ONDELETEALLRECORDSCMDMETHODID = "onDeleteAllRecordsCmdMethodID";
	public static final String PROPERTY_ONDELETEMETHODID = "onDeleteMethodID";
	public static final String PROPERTY_ONDELETERECORDCMDMETHODID = "onDeleteRecordCmdMethodID";
	public static final String PROPERTY_ONDOUBLECLICKMETHODID = "onDoubleClickMethodID";
	public static final String PROPERTY_ONDRAGENDMETHODID = "onDragEndMethodID";
	public static final String PROPERTY_ONDRAGMETHODID = "onDragMethodID";
	public static final String PROPERTY_ONDRAGOVERMETHODID = "onDragOverMethodID";
	public static final String PROPERTY_ONDROPMETHODID = "onDropMethodID";
	public static final String PROPERTY_ONDUPLICATERECORDCMDMETHODID = "onDuplicateRecordCmdMethodID";
	public static final String PROPERTY_ONELEMENTFOCUSGAINEDMETHODID = "onElementFocusGainedMethodID";
	public static final String PROPERTY_ONELEMENTFOCUSLOSTMETHODID = "onElementFocusLostMethodID";
	public static final String PROPERTY_ONERRORMETHODID = "onErrorMethodID";
	public static final String PROPERTY_ONFINDCMDMETHODID = "onFindCmdMethodID";
	public static final String PROPERTY_ONFINDMETHODID = "onFindMethodID";
	public static final String PROPERTY_ONFOCUSGAINEDMETHODID = "onFocusGainedMethodID";
	public static final String PROPERTY_ONFOCUSLOSTMETHODID = "onFocusLostMethodID";
	public static final String PROPERTY_ONINITMETHODID = "onInitMethodID";
	public static final String PROPERTY_ONINSERTMETHODID = "onInsertMethodID";
	public static final String PROPERTY_ONINVERTRECORDSCMDMETHODID = "onInvertRecordsCmdMethodID";
	public static final String PROPERTY_ONNEWRECORDCMDMETHODID = "onNewRecordCmdMethodID";
	public static final String PROPERTY_ONNEXTRECORDCMDMETHODID = "onNextRecordCmdMethodID";
	public static final String PROPERTY_ONOMITRECORDCMDMETHODID = "onOmitRecordCmdMethodID";
	public static final String PROPERTY_ONOPENMETHODID = "onOpenMethodID";
	public static final String PROPERTY_ONPREVIOUSRECORDCMDMETHODID = "onPreviousRecordCmdMethodID";
	public static final String PROPERTY_ONPRINTPREVIEWCMDMETHODID = "onPrintPreviewCmdMethodID";
	public static final String PROPERTY_ONRECORDEDITSTARTMETHODID = "onRecordEditStartMethodID";
	public static final String PROPERTY_ONRECORDEDITSTOPMETHODID = "onRecordEditStopMethodID";
	public static final String PROPERTY_ONRENDERMETHODID = "onRenderMethodID";
	public static final String PROPERTY_ONRESIZEMETHODID = "onResizeMethodID";
	public static final String PROPERTY_ONRIGHTCLICKMETHODID = "onRightClickMethodID";
	public static final String PROPERTY_ONSEARCHCMDMETHODID = "onSearchCmdMethodID";
	public static final String PROPERTY_ONSEARCHMETHODID = "onSearchMethodID";
	public static final String PROPERTY_ONSHOWALLRECORDSCMDMETHODID = "onShowAllRecordsCmdMethodID";
	public static final String PROPERTY_ONSHOWOMITTEDRECORDSCMDMETHODID = "onShowOmittedRecordsCmdMethodID";
	public static final String PROPERTY_ONSORTCMDMETHODID = "onSortCmdMethodID";
	public static final String PROPERTY_ONTABCHANGEMETHODID = "onTabChangeMethodID";
	public static final String PROPERTY_ONCHANGEMETHODID = "onChangeMethodID";
	public static final String PROPERTY_ONUNLOADMETHODID = "onUnLoadMethodID";
	public static final String PROPERTY_ONUPDATEMETHODID = "onUpdateMethodID";
	public static final String PROPERTY_OPERATOR = "operator";
	public static final String PROPERTY_PAGEBREAKAFTEROCCURRENCE = "pageBreakAfterOccurrence";
	public static final String PROPERTY_PAPERPRINTSCALE = "paperPrintScale";
	public static final String PROPERTY_PARTTYPE = "partType";
	public static final String PROPERTY_PRINTSLIDING = "printSliding";
	public static final String PROPERTY_RESOURCETYPE = "resourceType";;
	public static final String PROPERTY_RETURNDATAPROVIDERS = "returnDataProviders";
	public static final String PROPERTY_ROLLOVERCURSOR = "rolloverCursor";
	public static final String PROPERTY_ROLLOVERIMAGEMEDIAID = "rolloverImageMediaID";
	public static final String PROPERTY_ROTATION = "rotation";
	public static final String PROPERTY_ROUNDEDRADIUS = "roundedRadius";
	public static final String PROPERTY_ROWHEIGHT = "rowHeight";
	public static final String PROPERTY_SCROLLBARS = "scrollbars";
	public static final String PROPERTY_SEQUENCE = "sequence";
	public static final String PROPERTY_SHAPETYPE = "shapeType";
	public static final String PROPERTY_SHOWDATAPROVIDERS = "showDataProviders";
	public static final String PROPERTY_TABORIENTATION = "tabOrientation";
	public static final String PROPERTY_TABSEQ = "tabSeq";
	public static final String PROPERTY_TEXTORIENTATION = "textOrientation";
	public static final String PROPERTY_TYPE = "type";
	public static final String PROPERTY_VALUELISTID = "valuelistID";
	public static final String PROPERTY_VALUELISTTYPE = "valueListType";
	public static final String PROPERTY_VARIABLETYPE = "variableType";
	public static final String PROPERTY_VERTICALALIGNMENT = "verticalAlignment";
	public static final String PROPERTY_BACKGROUND = "background";
	public static final String PROPERTY_FOREGROUND = "foreground";
	public static final String PROPERTY_SELECTEDTABCOLOR = "selectedTabColor";
	public static final String PROPERTY_WIDTH = "width";
	public static final String PROPERTY_ALIASES = "aliases";
	public static final String PROPERTY_BEANCLASSNAME = "beanClassName";
	public static final String PROPERTY_BORDERTYPE = "borderType";
	public static final String PROPERTY_COMMENT = "comment";
	public static final String PROPERTY_CONTENT = "content";;
	public static final String PROPERTY_CSSTEXT = "CSSText";
	public static final String PROPERTY_CUSTOMVALUES = "customValues";
	public static final String PROPERTY_DATAPROVIDERID1 = "dataProviderID1";
	public static final String PROPERTY_DATAPROVIDERID2 = "dataProviderID2";
	public static final String PROPERTY_DATAPROVIDERID3 = "dataProviderID3";
	public static final String PROPERTY_DATAPROVIDERIDTOAGGREGATE = "dataProviderIDToAggregate";
	public static final String PROPERTY_DECLARATION = "declaration";
	public static final String PROPERTY_DEFAULTPAGEFORMAT = "defaultPageFormat";
	public static final String PROPERTY_DEFAULTVALUE = "defaultValue";
	public static final String PROPERTY_FONTTYPE = "fontType";
	public static final String PROPERTY_FOREIGNCOLUMNNAME = "foreignColumnName";
	public static final String PROPERTY_FOREIGNDATASOURCE = "foreignDataSource";
	public static final String PROPERTY_GROUPBYDATAPROVIDERIDS = "groupbyDataProviderIDs";
	public static final String PROPERTY_GROUPID = "groupID";
	public static final String PROPERTY_I18NDATASOURCE = "i18nDataSource";
	public static final String PROPERTY_INITIALSORT = "initialSort";
	public static final String PROPERTY_LABELFOR = "labelFor";
	public static final String PROPERTY_METHODCODE = "methodCode";
	public static final String PROPERTY_MIMETYPE = "mimeType";
	public static final String PROPERTY_MNEMONIC = "mnemonic";
	public static final String PROPERTY_MODULESNAMES = "modulesNames";
	public static final String PROPERTY_NAMEDFOUNDSET = "namedFoundSet";
	public static final String PROPERTY_NAME = "name";
	public static final String PROPERTY_COLUMNS = "columns";
	public static final String PROPERTY_PARAMETERS = "parameters";
	public static final String PROPERTY_POINTS = "points";
	public static final String PROPERTY_PRIMARYDATAPROVIDERID = "primaryDataProviderID";
	public static final String PROPERTY_PRIMARYDATASOURCE = "primaryDataSource";
	public static final String PROPERTY_ROWBGCOLORCALCULATION = "rowBGColorCalculation";
	public static final String PROPERTY_SCOPENAME = "scopeName";
	public static final String PROPERTY_SEPARATOR = "separator";
	public static final String PROPERTY_SORTOPTIONS = "sortOptions";
	public static final String PROPERTY_STYLESHEET = "styleSheetID";
	public static final String PROPERTY_STYLECLASS = "styleClass";
	public static final String PROPERTY_STYLENAME = "styleName";
	public static final String PROPERTY_TAGTYPE = "tagType";
	public static final String PROPERTY_TITLETEXT = "titleText";
	public static final String PROPERTY_TOOLTIPTEXT = "toolTipText";
	public static final String PROPERTY_TEXTROTATION = "textRotation";
	public static final String PROPERTY_LAYOUTGRID = "layoutGrid";
	public static final String PROPERTY_STYLE = "style";
	public static final String PROPERTY_CSSCLASS = "cssClasses";
	public static final String PROPERTY_JSON = "json";
	public static final String PROPERTY_TYPENAME = "typeName";
	public static final String PROPERTY_NG_READONLY_MODE = "ngReadOnlyMode";
	public static final String PROPERTY_ONELEMENTDATACHANGEMETHODID = "onElementDataChangeMethodID";
	public static final String PROPERTY_FORM_COMPONENT = "formComponent";
	public static final String PROPERTY_ATTRIBUTES = "attributes";
	public static final String PROPERTY_LAZY_LOADING = "lazyLoading";
}