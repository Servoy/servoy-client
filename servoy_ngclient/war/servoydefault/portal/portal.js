angular.module('svyPortal',['servoy']).directive('svyPortal', ['$utils', '$foundsetConstants', '$timeout', function($utils, $foundsetConstants, /*timeout can be removed if it was only used for testing*/ $timeout) {  
    return {
      restrict: 'E',
      scope: {
    	  	model: "=svyModel",
    	  	handlers: "=svyHandlers",
       		api: "=svyApi"
      },
      controller: function($scope, $element, $attrs) {
    	  // START TEST MODELS
    	  var textFieldApi = {}; 
    	  var preparedActionHandler = function(args, rowId) {
				alert('clicked:\n  selected = ' + $scope.model.foundset.selectedRowIndexes
 						+ '\n  Text field value (Row 2): ' + $scope.model.foundset.viewPort.rows[1].nameColumn
 						+ '\n  Selected text in text field (for selected row): ' + $scope.model.elements[1].api.getSelectedText()
 						+ '\n  Button 2 text: ' + $scope.model.elements[0].model.text);
				alert('rowID received as argument for on action = ' + rowId);
 				$scope.model.elements[0].model.text += '.';
 				$scope.model.elements[1].api.requestFocus();
 				$timeout(function() {
 					$scope.model.elements[1].api.setSelection(1,3);
 				}, 5000);
    	  };
    	  preparedActionHandler.selectRecordHandler = function(rowId) {
  				return function () { return preparedActionHandler(arguments, rowId); }
    	  }
    	  
    	  $scope.model = {
    			  "location":{"x":10,"y":116},
    			  "size":{"width":727,"height":335},
    			  elements: [
    			             	{
    			             		componentDirectiveName: "svy-button",
    			             		name: 'svy_1073741969',
    			             		model: {
    			             			"enabled":true,
    			             			"text":"show dialog",
    			             			"visible":true,
    			             			"location":{"x":647,"y":264},
    			             			"size":{"width":113,"height":20},
    			             			"tabSeq":8,
    			             			"name":"svy_1073741969",
    			             			"titleText":"Some button"
    			             		},
    			             		handlers: {
    			             			onActionMethodID: preparedActionHandler
    			             		},
    			             		api: {},
    			             		apply: function(){}
    			             	},
    			             	{
    			             		componentDirectiveName: "svy-textfield",
    			             		name: 'datatextfield1c',
    			             		model: {
    			             			"enabled":true,
    			             			"visible":true,
    			             			"location":{"x":246,"y":4},
    			             			"background":"#ff8000",
    			             			"toolTipText":"This is the datatextfield tooltip",
    			             			"editable":true,
    			             			"size":{"width":100,"height":20},
    			             			"tabSeq":1,
    			             			"name":"datatextfield1c",
    			             		},
    			             		api: textFieldApi,
    			             		apply: function(property, componentModel, rowId) {
    			            			// $servoyInternal.pushDPChange("product", "datatextfield1c", property, componentModel, rowId);
    			             			alert("Apply called with: (" + rowId + ", " + property + ", " + componentModel[property] + ")");
    			            		},
    			             		handlers: {},
    			             		forFoundset: {
    			             			dataLinks: [
    			             			            { propertyName: "dataProviderID", dataprovider: "nameColumn" }
    			             			],
    			             			apiCallTypes: {
    			             				requestFocus: $foundsetConstants.CALL_ON_ONE_SELECTED_ROW,
    			             				getSelectedText: $foundsetConstants.CALL_ON_ONE_SELECTED_ROW
    			             			}
    			             		}
    			             	}
    			            ],
    			  foundset: {
    				  serverSize: 44,
    				  viewPort: {
    					  startIndex: 15,
    					  size: 5,
    					  rows: [
    					         	{ _svyRowId: 'someRowIdHASH1', nameColumn: "Bubu" },
    					         	{ _svyRowId: 'someRowIdHASH2', nameColumn: "Yogy" },
    					         	{ _svyRowId: 'someRowIdHASH3', nameColumn: "Ranger" },
    					         	{ _svyRowId: 'someRowIdHASH4', nameColumn: "Watcher" },
    					         	{ _svyRowId: 'someRowIdHASH5', nameColumn: "Hatcher" }
    					  ],
    					  loadRecordsAsync: function(startIndex, size) {
    						  alert('Load async requested: ' + startIndex + ', ' + size);
    					  },
    					  loadExtraRecords: function(negativeOrPositiveCount) {
    						  // TODO implement
    					  }
    				  },
    				  selectedRowIndexes: [16], // can be out of viewPort as well
    				  multiSelect: false,
    			  }
    	  };
    	  
    	  $timeout(function() {
    		  if (textFieldApi.requestFocus) textFieldApi.requestFocus();
    	  }, 5000);
    	  // END TESTING MODELS
    	  
    	  var ROW_ID_COL_KEY = '_svyRowId';
    	  var recordsPerPage = 5; // TODO make this dynamic based on available display area!
    	  
    	  // TODO clear entries in this cache when the foundset records change (don't keep obsolete pks in there!)
    	  // rowProxyObjects[pk][elementIndex].
    	  //                                  .mergedCellModel  - is the actual cell element svyModel cache
    	  //                                  .cellApi          - is the actual cell element API cache
    	  //                                  .cellHandlers     - is the actual cell element handlers cache
    	  //                                  .cellApplyHandler - is the actual cell element apply handler cache
    	  var rowProxyObjects = {};
    	  
    	  var foundset = $scope.model.foundset;
    	  var elements = $scope.model.elements;
    	  
    	  $scope.pagingOptions = {
    			  pageSizes: [recordsPerPage, 50, 100, 200, 500, 1000],
    			  pageSize: recordsPerPage,
    			  currentPage: Math.floor(foundset.viewPort.startIndex / recordsPerPage + 1),
    	  };
    	  
    	  function getPageCount() {
    		  return Math.ceil(foundset.serverSize / $scope.pagingOptions.pageSize);
    	  }
    	  
    	  $scope.$watch('pagingOptions.currentPage', function(newVal, oldVal) {
    		  if (newVal !== oldVal) {
    			  // user requested another page; get it from server
    			  foundset.viewPort.loadRecordsAsync((newVal - 1) * $scope.pagingOptions.pageSize, $scope.pagingOptions.pageSize);
    			  // TODO show some loading feedback to the user until these records are received
    		  }
    	  });

    	  $scope.$watch('pagingOptions.pageSize', function(newVal, oldVal) {
    		  if (newVal !== oldVal) {
    			  // user requested another page size; get items from server (could be optimised I guess if page size is lower)
    			  var startIdx = $scope.pagingOptions.currentPage - 1;
    			  foundset.viewPort.loadRecordsAsync(startIdx * oldVal, newVal);
    			  $scope.pagingOptions.currentPage = Math.floor(startIdx / newVal + 1);
    			  // TODO show some loading feedback to the user until these records are received
    		  }
    	  });

    	  var columnDefinitions = [];
    	  for (var idx in elements) {
    		  var el = elements[idx]; 
    		  var columnTitle = el.model.titleText;
    		  if (!columnTitle) {
    			  // TODO use beautified dataProvider id or whatever other clients use as default, not directly the dataProvider id
    			  if (el.forFoundset && el.forFoundset.dataLinks && el.forFoundset.dataLinks.length > 0) columnTitle = el.forFoundset.dataLinks[0].dataprovider;
    			  if (!columnTitle) columnTitle = "";
    		  }
    		  columnDefinitions.push({
    			  displayName: columnTitle,
    			  cellTemplate: '<' + el.componentDirectiveName + ' name="' + el.name + '" svy-model="getMergedCellModel(row, ' + idx + ')" svy-api="cellApiWrapper(row, ' + idx + ')" svy-handlers="cellHandlerWrapper(row, ' + idx + ')" svy-apply="cellApplyHandlerWrapper(row, ' + idx + ')"/>'
    		  });
    	  }
    	  
    	  function getOrCreateRowProxies(rowId) {
    		  var proxies = rowProxyObjects[rowId];
    		  if (!proxies) rowProxyObjects[rowId] = proxies = [];
    		  return proxies;
    	  }

    	  function getOrCreateElementProxies(rowId, elementIndex) {
    		  var rowProxies = getOrCreateRowProxies(rowId);
    		  if (!rowProxies[elementIndex]) rowProxies[elementIndex] = {};
    		  return rowProxies[elementIndex];
    	  }
    	  
    	  function rowIdToViewportRelativeRowIndex(rowId) {
    		  for (var i = foundset.viewPort.rows.length - 1; i >= 0; i--)
    			  if (foundset.viewPort.rows[i][ROW_ID_COL_KEY] === rowId) return i;
    		  return -1;
    	  }

    	  function rowIdToAbsoluteRowIndex(rowId) {
    		  return viewPortToAbsolute(rowIdToViewportRelativeRowIndex(rowId));
    	  }

    	  function viewPortToAbsolute(rowIndex) {
    		  return rowIndex >= 0 ? rowIndex + foundset.viewPort.startIndex : -1;
    	  }

    	  function absoluteToViewPort(rowIndex) {
    		  return rowIndex - foundset.viewPort.startIndex;
    	  }

    	  function isRowIndexSelected(rowIndex) {
    		  return foundset.selectedRowIndexes.indexOf(rowIndex) >= 0;
    	  }
    	  
    	  function isInViewPort(absoluteRowIndex) {
    		  return (absoluteRowIndex >= foundset.viewPort.startIndex && absoluteRowIndex < (foundset.viewPort.startIndex + foundset.viewPort.size));
    	  }
    	  
    	  // merges foundset record dataprovider/tagstring properties into the element's model
    	  $scope.getMergedCellModel = function(ngGridRow, elementIndex) {
    		  var cellProxies = getOrCreateElementProxies(ngGridRow.getProperty(ROW_ID_COL_KEY), elementIndex);
    		  var cellModel = cellProxies.mergedCellModel;
    			  
    		  if (!cellModel) {
        		  var element = elements[elementIndex];

        		  function CellData() {
    			  }
    			  CellData.prototype = element.model;
    			  var cellData = new CellData();
    			  
    			  // properties like tagstring and dataprovider are not set directly on the component but are more linked to the current record
    			  // so we will take these from the foundset record and apply them to child elements
    			  if (element.forFoundset) {
    				  for (var i in element.forFoundset.dataLinks) {
    					  var propertyName = element.forFoundset.dataLinks[i].propertyName;
    					  var dataprovider = element.forFoundset.dataLinks[i].dataprovider;
    					  cellData[propertyName] = ngGridRow.getProperty(dataprovider);
    					  // 2 way data link between element separate properties from foundset and the merged cell model
    					  $utils.bindTwoWayObjectProperty(cellData, propertyName, ngGridRow.entity, dataprovider); // TODO - can we avoid using ngGrid undocumented "row.entity"? that is what ngGrid uses internally as model for default cell templates...
    				  }
    			  }
    			  
    			  for (var p in element.model) {
    				  // 2 way data link between element model and the merged cell model
    				  // it is a bit strange here as 1 element model will be 2 way bound to N cell models
        			  $utils.bindTwoWayObjectProperty(cellData, p, element.model, p);
    			  }
    			  
    			  cellProxies.mergedCellModel = cellModel = cellData;
    		  } 
    		  return cellModel;
    	  }
    	  
    	  function linkAPIToAllCellsInColumn(apiFunctionName, elementIndex) {
    		  // returns a column level API function that will call that API func. on all cell elements of that column
    		  return function()
    		  {
    			  var retVal;
    			  var retValForSelectedStored = false;
    			  var retValForSelectedCell;
    			  var callOnOneSelectedCellOnly = false;
    			  if (elements[elementIndex].forFoundset && elements[elementIndex].forFoundset.apiCallTypes) {
    				  callOnOneSelectedCellOnly = (elements[elementIndex].forFoundset.apiCallTypes[apiFunctionName] == $foundsetConstants.CALL_ON_ONE_SELECTED_ROW);
    			  }
    			  
				  // so if callOnOneSelectedCellOnly is true, then it will be called only once for one of the selected rows;
				  // otherwise it will be called on the entire column, and the return value will be of a selected row if possible
    			  for (var rowId in rowProxyObjects) {
    				  var cellProxies = rowProxyObjects[rowId][elementIndex];
    				  if (cellProxies && cellProxies.cellApi && cellProxies.cellApi[apiFunctionName]) {
    					  var rowIsSelected = isRowIndexSelected(rowIdToAbsoluteRowIndex(rowId));
    					  if (rowIsSelected || (!callOnOneSelectedCellOnly)) retVal = cellProxies.cellApi[apiFunctionName].apply(cellProxies.cellApi[apiFunctionName], arguments);
    					  
    					  // give back return value from a selected row cell if possible
    					  if (!retValForSelectedStored && rowIsSelected) {
    						  retValForSelectedCell = retVal;
    						  retValForSelectedStored = true;
    						  if (callOnOneSelectedCellOnly) break; // it should only get called once on first selected row
    					  }
    				  }
    			  }
    			  return retValForSelectedStored ? retValForSelectedCell : retVal;
    		  }
    	  }
    	  
    	  // cells provide API calls; one API call (from server) should execute on all cells of that element's column.
    	  // so any API provided by a cell is added to the server side controlled API object; when server calls that API method,
    	  // it will execute on all cells
    	  $scope.cellApiWrapper = function(ngGridRow, elementIndex) {
    		  var cellProxies = getOrCreateElementProxies(ngGridRow.getProperty(ROW_ID_COL_KEY), elementIndex);

    		  if (!cellProxies.cellApi) {
        		  var columnApi = elements[elementIndex].api;
        		  cellProxies.cellApi = {}; // new cell API object
        		  $scope.$watchCollection(function() { return cellProxies.cellApi; }, function(newCellAPI, oldCellAPI) {
        			  // update column API object with new cell available methods
        			  for (var p in newCellAPI) {
        				  if (!columnApi[p]) columnApi[p] = linkAPIToAllCellsInColumn(p, elementIndex);
        			  }
        			  for (var p in columnApi) {
        				  if (!newCellAPI[p]) delete columnApi[p];
        			  }
        		  });
    		  }
    		  return cellProxies.cellApi;
    	  }

    	  $scope.cellApplyHandlerWrapper = function(ngGridRow, elementIndex) {
    		  var rowId = ngGridRow.getProperty(ROW_ID_COL_KEY);
    		  var cellProxies = getOrCreateElementProxies(rowId, elementIndex);

    		  if (!cellProxies.cellApplyHandler) {
        		  var columnApplyHandler = elements[elementIndex].apply;
        		  cellProxies.cellApplyHandler = function(property) {
        			  return columnApplyHandler(property,cellProxies.mergedCellModel,rowId);
        		  };
    		  }
    		  return cellProxies.cellApplyHandler;
    	  }

    	  // bind foundset.selectedRowIndexes to what nggrid has to offer
    	  var selectedItemsProxy = []; // keeps entire foundset row items from model.foundset.viewPort.rows
    	  for (var rowIdx = 0;  rowIdx < foundset.viewPort.rows.length; rowIdx++) {
    		  if (isRowIndexSelected(viewPortToAbsolute(rowIdx))) selectedItemsProxy.push(foundset.viewPort.rows[rowIdx]);
    	  }
    	  function updateFoundsetSelectionFromGrid(newNGGridSelectedItems) {
			  // update foundset object selection when it changes in ngGrid
    		  var tmpSelectedRowIdxs = {};
			  for (var rowIdxInSelected = 0; rowIdxInSelected < newNGGridSelectedItems.length; rowIdxInSelected++) {
				  var absRowIdx = rowIdToAbsoluteRowIndex(newNGGridSelectedItems[rowIdxInSelected][ROW_ID_COL_KEY]);
				  if (!isRowIndexSelected(absRowIdx)) foundset.selectedRowIndexes.push(absRowIdx);
				  tmpSelectedRowIdxs['_' + absRowIdx] = true;
			  }
			  for (var rowIdxInSelected = 0; rowIdxInSelected < foundset.selectedRowIndexes.length; rowIdxInSelected++) {
				  if (!tmpSelectedRowIdxs['_' + foundset.selectedRowIndexes[rowIdxInSelected]]) {
					  // here we also handle the case when in multiselect there are records selected outside of viewport
					  // in that case if CTRL was pressed then this $watch should not remove those records from selection
					  // TODO if CTRL was not pressed in multi-selection, the outside-of-viewPort-selectedIndexes should be cleared as well - but not by this $watch code
					  if (!foundset.multiSelect || isInViewPort(rowIdxInSelected)) foundset.selectedRowIndexes.splice(rowIdxInSelected, 1);
				  }
			  }
			  // it is important that at the end of this function, the two arrays are in sync; otherwise, watch loops may happen
		  }
    	  $scope.$watchCollection(function() { return selectedItemsProxy; }, updateFoundsetSelectionFromGrid);
    	  $scope.$watchCollection('model.foundset.selectedRowIndexes', function(newFSSelectedItems) {
			  // update ngGrid selection when it changes in foundset
    		  var tmpSelectedRowIdxs = {};
			  for (var rowIdxInSelected = 0; rowIdxInSelected < selectedItemsProxy.length; rowIdxInSelected++) {
				  var absRowIdx = rowIdToAbsoluteRowIndex(selectedItemsProxy[rowIdxInSelected][ROW_ID_COL_KEY]);
				  if (newFSSelectedItems.indexOf(absRowIdx) < 0) selectedItemsProxy.splice(rowIdxInSelected, 1);
				  tmpSelectedRowIdxs['_' + absRowIdx] = true;
			  }
			  for (var rowIdxInSelected = 0; rowIdxInSelected < newFSSelectedItems.length; rowIdxInSelected++) {
				  var rowIdx = newFSSelectedItems[rowIdxInSelected];
				  if (isInViewPort(rowIdx) && !tmpSelectedRowIdxs['_' + rowIdx]) {
	    			  selectedItemsProxy.push(foundset.viewPort.rows[absoluteToViewPort(rowIdx)]);
				  }
			  }
			  // it is important that at the end of this function, the two arrays are in sync; otherwise, watch loops may happen
		  });

    	  $scope.gridOptions = {
    			  data: 'model.foundset.viewPort.rows',
    			  enableCellSelection: true,
    			  enableRowSelection: true,
    			  selectedItems: selectedItemsProxy,
    			  multiSelect: foundset.multiSelect,
    			  enablePaging: true,
    			  showFooter: (getPageCount() > 1),
    			  totalServerItems: 'model.foundset.serverSize',
    			  pagingOptions: $scope.pagingOptions,
    			  primaryKey: ROW_ID_COL_KEY, // not currently documented in ngGrid API but is used internally and useful - see ngGrid source code
    			  columnDefs: columnDefinitions
    	  };
    	  
    	  function linkHandlerToRowIdWrapper(handler, rowId) {
    		  return function() {
    			  $scope.gridOptions.selectItem(rowIdToViewportRelativeRowIndex(rowId), true); // TODO for multiselect - what about modifiers such as CTRL? then it might be false
    			  updateFoundsetSelectionFromGrid(selectedItemsProxy); // otherwise the watch/digest will update the foundset selection only after the handler was triggered...
    			  var recordHandler = handler.selectRecordHandler(rowId)
    			  return recordHandler.apply(recordHandler, arguments);
    		  }
    	  }
    	  // each handler at column level gets it's rowId from the cell's wrapper handler below (to
    	  // make sure that the foundset's selection is correct server-side when cell handler triggers)
    	  $scope.cellHandlerWrapper = function(ngGridRow, elementIndex) {
    		  var rowId = ngGridRow.getProperty(ROW_ID_COL_KEY);
    		  var cellProxies = getOrCreateElementProxies(rowId, elementIndex);

    		  if (!cellProxies.cellHandlers) {
        		  var columnHandlers = elements[elementIndex].handlers;
        		  cellProxies.cellHandlers = {};
    			  for (var p in columnHandlers) {
    				  cellProxies.cellHandlers[p] = linkHandlerToRowIdWrapper(columnHandlers[p], rowId);
    			  }
    			  
    		  }
    		  return cellProxies.cellHandlers;
    	  }
      },
      templateUrl: 'servoydefault/portal/portal.html',
      replace: true
    };
}]);