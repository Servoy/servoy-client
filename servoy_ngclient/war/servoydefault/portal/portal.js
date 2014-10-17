angular.module('servoydefaultPortal',['servoy','ui.grid' ,'ui.grid.edit','ui.grid.selection','ui.grid.resizeColumns','ui.grid.infiniteScroll'])
.directive('servoydefaultPortal', ['$utils', '$foundsetTypeConstants', '$componentTypeConstants', '$timeout', '$solutionSettings', '$anchorConstants', 'gridUtil',
                                   function($utils, $foundsetTypeConstants, $componentTypeConstants, $timeout, $solutionSettings, $anchorConstants,gridUtil) {  
    return {
      restrict: 'E',
      scope: {
    	  	model: "=svyModel",
    	  	handlers: "=svyHandlers",
       		api: "=svyApi"
      },
      link: function($scope, $element, $attrs) {
    	  // START TEST MODELS
//    	  var textFieldApi = {}; 
//    	  var preparedActionHandler = function(args, rowId) {
//				alert('clicked:\n  selected = ' + $scope.model.relatedFoundset.selectedRowIndexes
// 						+ '\n  Text field value (Row 2): ' + $scope.model.childElements[1].modelViewport[1].dataProviderID
// 						+ '\n  Selected text in text field (for selected row): ' + $scope.model.childElements[1].api.getSelectedText()
// 						+ '\n  Button 2 text: ' + $scope.model.childElements[0].model.text);
//				alert('rowID received as argument for on action = ' + rowId);
// 				$scope.model.childElements[0].model.text += ' ***';
// 				$scope.model.childElements[1].api.requestFocus();
// 				$timeout(function() {
// 					$scope.model.childElements[1].api.setSelection(1,3);
// 				}, 5000);
//    	  };
//    	  preparedActionHandler.selectRecordHandler = function(rowId) {
//  				return function () { return preparedActionHandler(arguments, rowId); }
//    	  }
//    	  
//    	  $scope.model = {
//    			  "location":{"x":10,"y":116},
//    			  "size":{"width":727,"height":335},
//    			  childElements: [
//    			             	{
//    			             		componentDirectiveName: "svy-button",
//    			             		name: 'svy_1073741969',
//    			             		model: {
//    			             			"enabled":true,
//    			             			"text":"show dialog",
//    			             			"visible":true,
//    			             			"location":{"x":647,"y":264},
//    			             			"size":{"width":113,"height":20},
//    			             			"tabSeq":8,
//    			             			"name":"svy_1073741969",
//    			             			"text":"Some button"
//    			             		},
//    			             		handlers: {
//    			             			onActionMethodID: preparedActionHandler
//    			             		},
//    			             		api: {},
//    			             		apply: function(){}
//    			             	},
//    			             	{
//    			             		componentDirectiveName: "svy-textfield",
//    			             		name: 'datatextfield1c',
//    			             		model: {
//    			             			"enabled":true,
//    			             			"visible":true,
//    			             			"location":{"x":246,"y":4},
//    			             			"background":"#ff8000",
//    			             			"toolTipText":"This is the datatextfield tooltip",
//    			             			"editable":true,
//    			             			"size":{"width":100,"height":20},
//    			             			"tabSeq":1,
//    			             			"name":"datatextfield1c",
//    			             		},
//  	  							modelViewport: [
//       								{ _svyRowId: 'someRowIdHASH1', dataProviderID: "Bubu" },
//       								{ _svyRowId: 'someRowIdHASH2', dataProviderID: "Yogy" },
//       								{ _svyRowId: 'someRowIdHASH3', dataProviderID: "Ranger" },
//       								{ _svyRowId: 'someRowIdHASH4', dataProviderID: "Watcher" },
//       								{ _svyRowId: 'someRowIdHASH5', dataProviderID: "Hatcher" }
//									],
//    			             		api: textFieldApi,
//    			             		apply: function(property, componentModel, rowId) {
//    			            			// $servoyInternal.pushDPChange("product", "datatextfield1c", property, componentModel, rowId);
//    			             			alert("Apply called with: (" + rowId + ", " + property + ", " + componentModel[property] + ")");
//    			            		},
//    			             		handlers: {},
//    			             		forFoundset: {
//    			             			apiCallTypes: {
//    			             				setValueListItems: $componentTypeConstants.CALL_ON_ALL_RECORDS_IF_TEMPLATE,
//    			             			}
//    			             		}
//    			             	}
//    			            ],
//    			  relatedFoundset: {
//    				  serverSize: 44,
//    				  viewPort: {
//    					  startIndex: 15,
//    					  size: 5,
//    					  rows: [
//									// empty values here as data is available as part of component 'modelViewport' now; will be as shown below if foundset is explicitly requested for columns at designtime
//    					         	{ /*_svyRowId: 'someRowIdHASH1', nameColumn: "Bubu"*/ },
//    					         	{ /*_svyRowId: 'someRowIdHASH2', nameColumn: "Yogy"*/ },
//    					         	{ /*_svyRowId: 'someRowIdHASH3', nameColumn: "Ranger"*/ },
//    					         	{ /*_svyRowId: 'someRowIdHASH4', nameColumn: "Watcher"*/ },
//    					         	{ /*_svyRowId: 'someRowIdHASH5', nameColumn: "Hatcher"*/ }
//    					  ],
//    					  loadRecordsAsync: function(startIndex, size) {
//    						  alert('Load async requested: ' + startIndex + ', ' + size);
//    						  $scope.model.relatedFoundset.viewPort.startIndex = startIndex;
//    						  $scope.model.relatedFoundset.viewPort.rows =  [
//    						          					         	{ _svyRowId: 'someRowIdHASH6', nameColumn: "ABC Bubu" },
//    						        					         	{ _svyRowId: 'someRowIdHASH7', nameColumn: "ABC Yogy" },
//    						        					         	{ _svyRowId: 'someRowIdHASH8', nameColumn: "ABC Ranger" },
//    						        					         	{ _svyRowId: 'someRowIdHASH9', nameColumn: "ABC Watcher" },
//    						        					         	{ _svyRowId: 'someRowIdHASH10', nameColumn: "ABC Hatcher" }
//    						        					  ];
//    					  },
//    					  loadExtraRecordsAsync: function(negativeOrPositiveCount) {
//    						  // TODO implement
//    					  }
//    				  },
//    				  selectedRowIndexes: [16], // can be out of viewPort as well
//    				  multiSelect: false,
//    			  }
//    	  };
//    	  
//    	  $timeout(function() {
//    		  if (textFieldApi.requestFocus) textFieldApi.requestFocus();
//    	  }, 5000);
    	  // END TESTING MODELS
   	  
    	  // TODO clear entries in this cache when the foundset records change (don't keep obsolete pks in there!)
    	  // rowProxyObjects[pk][elementIndex].
    	  //                                  .mergedCellModel  - is the actual cell element svyModel cache
    	  //                                  .cellApi          - is the actual cell element API cache
    	  //                                  .cellHandlers     - is the actual cell element handlers cache
    	  //                                  .cellApplyHandler - is the actual cell element apply handler cache
    	  var rowProxyObjects = {};
    	  
    	  $scope.pageSize = 25;
    	  
    	  $scope.foundset = null;
    	  var elements = $scope.model.childElements;
    	  $scope.$watch('model.relatedFoundset', function(newVal, oldVal) {
    		  if (!$scope.foundset && newVal.viewPort.size > 0){
    		  	// this is the first time after a tab switch, there is data but the portal is not showing yet.
    		 	$scope.foundset = {viewPort:{rows:[]}}
    		  	$timeout(function() {
    		  		$timeout(function() {
    		  			$scope.foundset = newVal;
    		  		},1);
    		  	},1);
    		  }
    		  else {
    		  	$scope.foundset = newVal;
    		  }
    	  })
    	  $scope.$watchCollection('model.childElements', function(newVal, oldVal) {
    		  elements = $scope.model.childElements;
    		  if (newVal != oldVal) {
    			  // either a component was added/removed or the whole array changed
    			  
    			  // reset handlers so that the new ones will be used
    			  onAllCells(function(cellProxy, pk, elementIndex) { delete cellProxy.cellHandlers; });
    			  // add back apis cause incomming are probably fresh uninitialized ones {}
    			  onAllCells(function(cellProxy, pk, elementIndex) { updateColumnAPIFromCell(elements[elementIndex].api, cellProxy.cellApi, elementIndex); });
    		  }
    	  })
    	  
    	  function onAllCells(f) {
    		  for (var pk in rowProxyObjects)
    			  for (var elementIndex in rowProxyObjects[pk])
    				  f(rowProxyObjects[pk][elementIndex], pk, elementIndex);
    	  }
    	  
    	  $scope.rowHeight = $scope.model.rowHeight;
    	  
    	  var rowTemplate = ''
    	  $scope.columnDefinitions = [];
    	  for (var idx = 0; idx < elements.length; idx++) {
    		  var el = elements[idx]; 
    		  var elY = el.model.location.y - $scope.model.location.y;
    		  var columnTitle = el.model.text;
    		  if (!columnTitle) {
    			  // TODO use beautified dataProvider id or whatever other clients use as default, not directly the dataProvider id
    			  if (el.forFoundset && el.forFoundset.dataLinks && el.forFoundset.dataLinks.length > 0) {
    				  columnTitle = el.forFoundset.dataLinks[0].dataprovider;
    				  if (columnTitle && columnTitle.indexOf('.') >= 0) {
    					  columnTitle = columnTitle.substring(columnTitle.lastIndexOf('.'));
    				  }
    			  }
    			  if (!columnTitle) columnTitle = "";
    		  }

    		  var cellTemplate = '<' + el.componentDirectiveName + ' name="' + el.name + '" svy-model="getExternalScopes().getMergedCellModel(row, ' + idx + ')" svy-api="getExternalScopes().cellApiWrapper(row, ' + idx + ')" svy-handlers="getExternalScopes().cellHandlerWrapper(row, ' + idx + ')" svy-apply="getExternalScopes().cellApplyHandlerWrapper(row, ' + idx + ')"/>' 
    		  if($scope.model.multiLine) { 
    			  if($scope.rowHeight == undefined || (!$scope.model.rowHeight && ($scope.rowHeight < elY + el.model.size.height))) {
    				  $scope.rowHeight = $scope.model.rowHeight ? $scope.model.rowHeight : elY + el.model.size.height;
    			  }
    			  rowTemplate = rowTemplate + '<div ng-style="getExternalScopes().getMultilineComponentWrapperStyle(' + idx + ')" >' + cellTemplate + '</div>';
    		  }
    		  else {
    			  if($scope.rowHeight == undefined || $scope.rowHeight < el.model.size.height) {
    				  $scope.rowHeight = el.model.size.height;
    			  }
        		  $scope.columnDefinitions.push({
        			  name:el.name,
        			  displayName: columnTitle,
        			  cellTemplate: cellTemplate,
        			  visible: el.model.visible,
        			  width: el.model.size.width
        		  });

        		  updateColumnVisibility($scope, idx);
    		  }
    	  }
    	  
    	  if($scope.model.multiLine) {
    		  $scope.columnDefinitions.push({
    			  width: '100%',
    			  cellTemplate: rowTemplate 
    		  });
    	  }
    	  
    	  function updateColumnVisibility(scope, idx) {
    		  scope.$watch('model.childElements[' + idx + '].model.visible', function (newVal, oldVal) {
    			  scope.columnDefinitions[idx].visible = scope.model.childElements[idx].model.visible;
    		  }, false);
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
    		  for (var i = $scope.foundset.viewPort.rows.length - 1; i >= 0; i--)
    			  if ($scope.foundset.viewPort.rows[i][$foundsetTypeConstants.ROW_ID_COL_KEY] === rowId) return i;
    		  return -1;
    	  }

    	  function rowIdToAbsoluteRowIndex(rowId) {
    		  return viewPortToAbsolute(rowIdToViewportRelativeRowIndex(rowId));
    	  }

    	  function viewPortToAbsolute(rowIndex) {
    		  return rowIndex >= 0 ? rowIndex + $scope.foundset.viewPort.startIndex : -1;
    	  }

    	  function isRowIndexSelected(rowIndex) {
    		  return $scope.foundset.selectedRowIndexes.indexOf(rowIndex) >= 0;
    	  }
    	  
    	  function isInViewPort(absoluteRowIndex) {
			  return (absoluteRowIndex >= $scope.foundset.viewPort.startIndex && absoluteRowIndex < ($scope.foundset.viewPort.startIndex + $scope.foundset.viewPort.size));
    	  }
    	  
    	  $scope.exScope = {};
    	  
    	  $scope.exScope.getMultilineComponentWrapperStyle = function(elementIndex) {
				var elModel = elements[elementIndex].model;
				var containerModel = $scope.model;
				var elLayout = {position: 'absolute'};

				if(elModel.anchors && $solutionSettings.enableAnchoring) {
					var anchoredTop = (elModel.anchors & $anchorConstants.NORTH) != 0; // north
					var anchoredRight = (elModel.anchors & $anchorConstants.EAST) != 0; // east
					var anchoredBottom = (elModel.anchors & $anchorConstants.SOUTH) != 0; // south
					var anchoredLeft = (elModel.anchors & $anchorConstants.WEST) != 0; //west

					if (!anchoredLeft && !anchoredRight) anchoredLeft = true;
					if (!anchoredTop && !anchoredBottom) anchoredTop = true;

					if (anchoredTop)
					{
						elLayout.top = elModel.location.y + 'px';
					}

					if (anchoredBottom)
					{
						elLayout.bottom = containerModel.size.height - elModel.location.y - elModel.size.height;
						if(elModel.offsetY) {
							elLayout.bottom = elLayout.bottom - elModel.offsetY;
						}
						elLayout.bottom = elLayout.bottom + "px";
					}

					if (!anchoredTop || !anchoredBottom) elLayout.height = elModel.size.height + 'px';

					if (anchoredLeft)
					{
						if ($solutionSettings.ltrOrientation)
						{
							elLayout.left =  elModel.location.x + 'px';
						}
						else
						{
							elLayout.right =  elModel.location.x + 'px';
						}
					}

					if (anchoredRight)
					{
						if ($solutionSettings.ltrOrientation)
						{
							elLayout.right = (containerModel.size.width - elModel.location.x - elModel.size.width) + "px";
						}
						else
						{
							elLayout.left = (containerModel.size.width - elModel.location.x - elModel.size.width) + "px";
						}
					}

					if (!anchoredLeft || !anchoredRight) elLayout.width = elModel.size.width + 'px';
				}
				else {
					if($solutionSettings.ltrOrientation)
					{
						elLayout.left = elModel.location.x + 'px';
					}
					else
					{
						elLayout.right = elModel.location.x + 'px';
					}
					elLayout.top = elModel.location.y + 'px';
					elLayout.width = elModel.size.width + 'px';
					elLayout.height = elModel.size.height + 'px';		   
				}

    	    	if (typeof elModel.visible !== "undefined" && !elModel.visible) elLayout.display = 'none'; // undefined is considered visible by default
    		  
				return elLayout;
    	  }
    	  
    	  // merges foundset record dataprovider/tagstring properties into the element's model
    	  $scope.exScope.getMergedCellModel = function(ngGridRow, elementIndex) {
    		  var cellProxies = getOrCreateElementProxies(ngGridRow.entity[$foundsetTypeConstants.ROW_ID_COL_KEY], elementIndex);
    		  var cellModel = cellProxies.mergedCellModel;
    		  
    		  // TODO - can we avoid using ngGrid undocumented "row.entity"? that is what ngGrid uses internally as model for default cell templates...
    		  cellProxies.rowEntity = ngGridRow.entity; // so that 2 way bindings below work even if the instance of
    		  // 'ngGridRow.entity' changes (for example a 'CHANGE' row update from server could do that)
    		  // instance from the one that was when the cached proxy for that pkHash got created
    			  
    		  if (!cellModel) {
        		  var element = elements[elementIndex];

        		  var key;
    			  var cellData = {};
    			  
    			  // some properties that have default values might only be sent later to client;
    			  // if that happens, we need to then bind them two-way as well
    			  cellProxies.unwatchProperty = {};

    			  function updateTwoWayBindings(listWithProperties) {
    				  var k;
    				  for (k in listWithProperties) {
    					  if (!cellProxies.unwatchProperty[k]) {
    						  // skip this for special - row-by-row changing properties; it is handled separately later in code
    						  var skip = false;
    						  if (elements[elementIndex].forFoundset) {
    							  for (var i in elements[elementIndex].forFoundset.dataLinks) {
    								  skip = (elements[elementIndex].forFoundset.dataLinks[i].propertyName == k);
    								  if (skip) break;
    							  }
    						  }

    						  if (!skip) {
    							  // 2 way data link between element model and the merged cell model
    							  // it is a bit strange here as 1 element model will be 2 way bound to N cell models
    							  cellProxies.unwatchProperty[k] = $utils.bindTwoWayObjectProperty(cellData, k, elements[elementIndex].model, k, false, $scope);
    							  
    							  // copy initial values
    							  if (angular.isUndefined(cellData[k])) cellData[k] = elements[elementIndex].model[k];
    							  else if (angular.isUndefined(elements[elementIndex].model[k])) elements[elementIndex].model[k] = cellData[k];
    						  }
    					  } 
    				  }
    			  };
    			  $scope.$watchCollection(function() { return elements[elementIndex].model; }, updateTwoWayBindings);
    			  $scope.$watchCollection(function() { return cellData; }, updateTwoWayBindings);
    			  
    			  // properties like tagstring and dataprovider are not set directly on the component but are more linked to the current record
    			  // so we will take these from the foundset record and apply them to child elements
    			  if (element.forFoundset) {
    				  for (var i in element.forFoundset.dataLinks) {
    					  var propertyName = element.forFoundset.dataLinks[i].propertyName;
    					  var dataprovider = element.forFoundset.dataLinks[i].dataprovider;
    					  cellData[propertyName] = ngGridRow.entity[dataprovider];
    					  // 2 way data link between element separate properties from foundset and the merged cell model
    					  $utils.bindTwoWayObjectProperty(cellData, propertyName, cellProxies, ["rowEntity", dataprovider], false, $scope);
    				  }
    			  }
    			  
    			  
    			  updateTwoWayBindings(element.model);
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
    			  var callOnOneSelectedCellOnly = true;
    			  if (apiFunctionName == 'setFindMode')
    			  {
    				  callOnOneSelectedCellOnly = false;
    			  }
    			  else if (elements[elementIndex].forFoundset && elements[elementIndex].forFoundset.apiCallTypes) {
    				  callOnOneSelectedCellOnly = (elements[elementIndex].forFoundset.apiCallTypes[apiFunctionName] != $componentTypeConstants.CALL_ON_ONE_SELECTED_ROW);
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
    	  $scope.exScope.cellApiWrapper = function(ngGridRow, elementIndex) {
    		  var cellProxies = getOrCreateElementProxies(ngGridRow.entity[$foundsetTypeConstants.ROW_ID_COL_KEY], elementIndex);

    		  if (!cellProxies.cellApi) {
        		  var columnApi = elements[elementIndex].api;
        		  cellProxies.cellApi = {}; // new cell API object
        		  $scope.$watchCollection(function() { return cellProxies.cellApi; }, function(newCellAPI) {
        			  updateColumnAPIFromCell(columnApi, newCellAPI, elementIndex);
        		  });
    		  }
    		  return cellProxies.cellApi;
    	  }
    	  
    	  function updateColumnAPIFromCell(columnApi, cellAPI, elementIndex) {
			  // update column API object with new cell available methods
			  for (var p in cellAPI) {
				  if (!columnApi[p]) columnApi[p] = linkAPIToAllCellsInColumn(p, elementIndex);
			  }
			  for (var p in columnApi) {
				  if (!cellAPI[p]) delete columnApi[p];
			  }
    	  }

    	  $scope.exScope.cellApplyHandlerWrapper = function(ngGridRow, elementIndex) {
    		  var rowId = ngGridRow.entity[$foundsetTypeConstants.ROW_ID_COL_KEY];
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
    	  function updateFoundsetSelectionFromGrid(newNGGridSelectedItems) {
    		  if (newNGGridSelectedItems.length == 0 && $scope.model.relatedFoundset.serverSize > 0) return; // we shouldn't try to set no selection if there are records; it will be an invalid request as server doesn't allow that
			  // update foundset object selection when it changes in ngGrid
    		  var tmpSelectedRowIdxs = {};
			  for (var idx = 0; idx < newNGGridSelectedItems.length; idx++) {
				  var absRowIdx = rowIdToAbsoluteRowIndex(newNGGridSelectedItems[idx][$foundsetTypeConstants.ROW_ID_COL_KEY]);
				  if (!isRowIndexSelected(absRowIdx)) $scope.foundset.selectedRowIndexes.push(absRowIdx);
				  tmpSelectedRowIdxs['_' + absRowIdx] = true;
			  }
			  for (var idx = 0; idx < $scope.foundset.selectedRowIndexes.length; idx++) {
				  if (!tmpSelectedRowIdxs['_' + $scope.foundset.selectedRowIndexes[idx]]) {
					  // here we also handle the case when in multiselect there are records selected outside of viewport
					  // in that case if CTRL was pressed then this $watch should not remove those records from selection
					  // TODO if CTRL was not pressed in multi-selection, the outside-of-viewPort-selectedIndexes should be cleared as well - but not by this $watch code
					  if (!$scope.foundset.multiSelect || isInViewPort(idx)) $scope.foundset.selectedRowIndexes.splice(idx, 1);
				  }
			  }
			  // it is important that at the end of this function, the two arrays are in sync; otherwise, watch loops may happen
		  }
    	  var updateGridSelectionFromFoundset = function() {
    		  $scope.$evalAsync(function () { 
	    		  var rows = $scope.foundset.viewPort.rows;
	    		  if (rows.length > 0 && $scope.foundset.selectedRowIndexes.length > 0) {
		    		  for (var idx = 0;  idx < $scope.foundset.selectedRowIndexes.length; idx++) {
		          		  var rowIdx = $scope.foundset.selectedRowIndexes[idx];
		          		  if (isInViewPort(rowIdx)) $scope.gridApi.selection.selectRow(rows[rowIdx]);
		          	  }
	    		  } else if (rows.length > 0) {
	    			  $scope.gridApi.selection.selectRow(rows[0]);
	    		  }
    		  });
			  // it is important that at the end of this function, the two arrays are in sync; otherwise, watch loops may happen
		  };
    	  $scope.$watchCollection('foundset.selectedRowIndexes', updateGridSelectionFromFoundset);

    	  $scope.gridOptions = {
    			  data: 'foundset.viewPort.rows',
    			  enableRowSelection: true,
    			  enableRowHeaderSelection: false,
    			  multiSelect: false,
    			  noUnselect: true,
    			  enableScrollbars: true,
    			  useExternalSorting: true,
    			  primaryKey: $foundsetTypeConstants.ROW_ID_COL_KEY, // not currently documented in ngGrid API but is used internally and useful - see ngGrid source code
    			  columnDefs: $scope.columnDefinitions,
    			  headerRowHeight: $scope.model.multiLine ? 0 : $scope.model.headerHeight,
    			  rowHeight: $scope.rowHeight?$scope.rowHeight:20
    	  };
    	  $scope.gridOptions.onRegisterApi = function( gridApi ) {
    		  $scope.gridApi = gridApi;
    		  gridApi.selection.on.rowSelectionChanged($scope,function(row){
    		        updateFoundsetSelectionFromGrid(gridApi.selection.getSelectedRows())
    		  });
    		  gridApi.infiniteScroll.on.needLoadMoreData($scope,function(){
			     $scope.foundset.loadExtraRecordsAsync(Math.min($scope.pageSize, $scope.foundset.serverSize - $scope.foundset.viewPort.size));
			  });
    		  gridApi.core.on.sortChanged( $scope, function( grid, sortColumns ) {
    			   // cal the server (through foundset type)
    			  // $scope.foundset.sort(sortColumns[0], sortColumns[0].sort.direction == uiGridConstants.ASC);
    		      });
    		  var requestViewPortSize = -1;
    		  function testNumberOfRows() {
       			if (requestViewPortSize == -1 && $scope.foundset.serverSize > $scope.foundset.viewPort.size) {
          			 var numberOfRows = $scope.gridApi.grid.gridHeight/$scope.gridOptions.rowHeight;
          			 if ($scope.foundset.viewPort.size  == 0) {
          				 // its a full reload because viewPort size = 0
          				requestViewPortSize = 0;
          				$scope.foundset.loadRecordsAsync(0, Math.min($scope.foundset.serverSize, numberOfRows+$scope.pageSize));
          			 }
          			 else if ($scope.foundset.viewPort.size < numberOfRows) {
          				 // only add extra recors
          				requestViewPortSize = $scope.foundset.viewPort.size;
	   	         		$scope.foundset.loadExtraRecordsAsync(Math.min($scope.foundset.serverSize- $scope.foundset.viewPort.size, (numberOfRows + $scope.pageSize) - $scope.foundset.viewPort.size));
	   	         	 }
          		 }
       		 }
    		  $timeout(function(){
         		 $scope.gridApi.grid.gridWidth = gridUtil.elementWidth($element);
         		 $scope.gridApi.grid.gridHeight = gridUtil.elementHeight($element);
         		 $scope.gridApi.grid.refreshCanvas(true);
         		 testNumberOfRows();
         		 // reset what ui-grid did if somehow the row height was smaller then the elements height because it didn't layout yet
         		 $element.children(".svyPortalGridStyle").height('');
         		 
           	   $scope.$watch('foundset.multiSelect', function(newVal, oldVal) {
           		  // if the server foundset changes and that results in startIndex change (as the viewport will follow first record), see if the page number needs adjusting
           		  $scope.gridApi.selection.setMultiSelect(newVal);
           	  });

           	  // size can change serverside if records get deleted by someone else and there are no other records to fill the viewport with (by sliding)
           	  $scope.$watch('foundset.viewPort.size', function(newVal, oldVal) {
           		  if (requestViewPortSize != newVal) requestViewPortSize = -1;
           		  testNumberOfRows();
           	  });

           	  $scope.$watch('foundset.serverSize', function(newVal, oldVal) {
           		testNumberOfRows();
           	  });
           	  
           	  $scope.$watchCollection('foundset.viewPort.rows', function() {
           		  // TODO check to see if we have obsolete columns in rowProxyObjects[...] - and clean them up + remove two way binding watches (we have to keep somewhere references to the removal functions when we register row watches)
           		  // allow nggrid to update it's model / selected items and make sure selection didn't fall/remain on a wrong item because of that update...
           		  updateGridSelectionFromFoundset();
           		  $scope.gridApi.infiniteScroll.dataLoaded();
           	  });
    		  },0)
    	  };
    	  $scope.styleClass = 'svyPortalGridStyle';
    	  
    	  function linkHandlerToRowIdWrapper(handler, rowId) {
    		  return function() {
    			  $scope.gridOptions.selectItem(rowIdToViewportRelativeRowIndex(rowId), true); // TODO for multiselect - what about modifiers such as CTRL? then it might be false
    			  updateFoundsetSelectionFromGrid(gridApi.selection.getSelectedRows()); // otherwise the watch/digest will update the foundset selection only after the handler was triggered...
    			  var recordHandler = handler.selectRecordHandler(rowId)
    			  return recordHandler.apply(recordHandler, arguments);
    		  }
    	  }
    	  // each handler at column level gets it's rowId from the cell's wrapper handler below (to
    	  // make sure that the foundset's selection is correct server-side when cell handler triggers)
    	  $scope.cellHandlerWrapper = function(ngGridRow, elementIndex) {
    		  var rowId = ngGridRow.entity[$foundsetTypeConstants.ROW_ID_COL_KEY];
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
    	  
    	 // special method that servoy calls when this component goes into find mode.
    	// special method that servoy calls when this component goes into find mode.
       	 $scope.api.setFindMode = function(findMode, editable) {
       		 for (var i =0; i < $scope.model.childElements.length; i++)
       		 {
       			if ($scope.model.childElements[i].api.setFindMode)
       			{
       				var isEditable;
       				if (findMode)
       				{
 	      				$scope.model.childElements[i].model.wasEditable = $scope.model.childElements[i].model.editable;
 	      				isEditable = editable;
       				}
       				else
       				{
       					isEditable = $scope.model.childElements[i].model.wasEditable;
       				}
       				$scope.model.childElements[i].api.setFindMode(findMode, isEditable);
       			}
       			else
       			{
       				if (findMode)
       				{
 	      				$scope.model.childElements[i].model.readOnlyBeforeFindMode = $scope.model.childElements[i].model.readOnly;
 	      				$scope.model.childElements[i].model.readOnly = !editable;
       				}
       				else
       				{
       					$scope.model.childElements[i].model.readOnly = $scope.model.childElements[i].model.readOnlyBeforeFindMode;
       				}
       			}
       		 }
       	 };
       },
      templateUrl: 'servoydefault/portal/portal.html',
      replace: true
    };
}]);
