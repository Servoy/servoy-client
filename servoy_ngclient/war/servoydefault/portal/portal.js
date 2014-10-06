angular.module('svyPortal',['servoy']).directive('svyPortal', ['$utils', '$foundsetTypeConstants', '$componentTypeConstants', '$timeout', '$solutionSettings', '$anchorConstants', function($utils, $foundsetTypeConstants, $componentTypeConstants, /*timeout can be removed if it was only used for testing*/ $timeout, $solutionSettings, $anchorConstants) {  
    return {
      restrict: 'E',
      scope: {
    	  	model: "=svyModel",
    	  	handlers: "=svyHandlers",
       		api: "=svyApi"
      },
      controller: function($scope, $element, $attrs) {
    	  // START TEST MODELS
//    	  var textFieldApi = {}; 
//    	  var preparedActionHandler = function(args, rowId) {
//				alert('clicked:\n  selected = ' + $scope.model.relatedFoundset.selectedRowIndexes
// 						+ '\n  Text field value (Row 2): ' + $scope.model.relatedFoundset.viewPort.rows[1].nameColumn
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
//    			             		api: textFieldApi,
//    			             		apply: function(property, componentModel, rowId) {
//    			            			// $servoyInternal.pushDPChange("product", "datatextfield1c", property, componentModel, rowId);
//    			             			alert("Apply called with: (" + rowId + ", " + property + ", " + componentModel[property] + ")");
//    			            		},
//    			             		handlers: {},
//    			             		forFoundset: {
//    			             			dataLinks: [
//    			             			            { propertyName: "dataProviderID", dataprovider: "nameColumn" }
//    			             			],
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
//    					         	{ _svyRowId: 'someRowIdHASH1', nameColumn: "Bubu" },
//    					         	{ _svyRowId: 'someRowIdHASH2', nameColumn: "Yogy" },
//    					         	{ _svyRowId: 'someRowIdHASH3', nameColumn: "Ranger" },
//    					         	{ _svyRowId: 'someRowIdHASH4', nameColumn: "Watcher" },
//    					         	{ _svyRowId: 'someRowIdHASH5', nameColumn: "Hatcher" }
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
    	  
    	  var foundset = $scope.model.relatedFoundset;
    	  var elements = $scope.model.childElements;
    	  $scope.$watch('model.relatedFoundset', function(newVal, oldVal) {
    		  foundset = newVal;
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
    	  
    	  $scope.pagingOptions = {
    			  pageSizes: [1, 5, 10, 20, 50, 100, 200, 500, 1000],
    			  pageSize: -1,
    			  currentPage: 1
    	  }
    	  
    	  function getCurrentPage() {
    		  //pagesize not yet initialized
    		  if ($scope.pagingOptions.pageSize < 0) return 1;
    		  // if the server foundset changes and that results in startIndex change (as the viewport will follow first record), see if the page number needs adjusting
    		  var currentPage = Math.floor(foundset.viewPort.startIndex / $scope.pagingOptions.pageSize) + 1;
    		  if (foundset.viewPort.size > 0 && foundset.viewPort.startIndex % $scope.pagingOptions.pageSize !== 0) {
    			  // for example if you have 5 per page, viewPort startIndex 5, size 5 (so page 2), foundset size 1000
    			  // and then on server records 3-4 get deleted, you will end up with viewPort startIndex 3, size 5
    			  // because the viewPort property type tries to follow the first record; we want to do the same
    			  // so show the same page contents instead of auto-switching page to 1 and showing 3 new records and only 2 of the ones
    			  // that were previously shown to the user; in this case we need to show page 2 (not 1) to allow user to
    			  // go to first 3 records;
    			  // something similar may happen when records get added before the current startIndex
    			  currentPage++;
    		  }
    		  return currentPage;
    	  }
    	  
    	  function updatePageCount() {
    		  if ($scope.pagingOptions.pageSize <0) return;
    		  var count = Math.ceil(foundset.serverSize / $scope.pagingOptions.pageSize);
    		  // for example if you have 5 per page, viewPort startIndex 5, size 5 (so page 2), foundset size 1000
    		  // and then on server records 3-4 get deleted, you will end up with viewPort startIndex 3, size 5
    		  // because the viewPort property type tries to follow the first record; we want to do the same
    		  // so show the same page contents instead of auto-switching page to 1 and showing 3 new records and only 2 of the ones
    		  // that were previously shown to the user; in this case we need to show page 2 (not 1) to allow user to
    		  // go to first 3 records; this also implies that we have an extra page sometimes
    		  var emptySpaceOnPreviousPage = foundset.viewPort.startIndex % $scope.pagingOptions.pageSize;
    		  if (emptySpaceOnPreviousPage !== 0) {
    			  var wouldBeRecordsOnLastPage = foundset.serverSize % $scope.pagingOptions.pageSize;
    			  if (wouldBeRecordsOnLastPage == 0) wouldBeRecordsOnLastPage = $scope.pagingOptions.pageSize;
    			  if (emptySpaceOnPreviousPage + wouldBeRecordsOnLastPage > $scope.pagingOptions.pageSize) count++;
    			  else emptySpaceOnPreviousPage = 0;
    		  }
    		  
    		  var multiPage = (count > 1);
    		  if ($scope.gridOptions) {
    			  $scope.gridOptions.$gridScope.showFooter = multiPage;
    			  $scope.gridOptions.$gridScope.enablePaging = multiPage;
    			  $scope.gridOptions.$gridScope.footerRowHeight = multiPage ?  55 : 0; // we need to calculate needed size somehow
    		  }
    		  // TODO this artificialServerSize does make page count show well, but it obviously breaks total size
    		  // so it's disabled for now...
//    		  $scope.artificialServerSize = foundset.serverSize + emptySpaceOnPreviousPage;
    		  $scope.artificialServerSize = foundset.serverSize;
    	  }
    	  
    	  $scope.$watch('pagingOptions.currentPage', function(newVal, oldVal) {
    		  if (newVal !== getCurrentPage()) { // we are using getCurrentPage() here instead of oldVal because of the special situations that rise from not changing user view if records get added/removed before viewPort startIndex
    			  // user requested another page; get it from server
				  var startIdx = (newVal - 1) * $scope.pagingOptions.pageSize;
    			  if (startIdx < foundset.serverSize) foundset.loadRecordsAsync(startIdx, Math.min(foundset.serverSize - startIdx, $scope.pagingOptions.pageSize));
    			  // TODO show some loading feedback to the user until these records are received
    		  }
    	  });

    	  $scope.$watch('pagingOptions.pageSize', function(newVal, oldVal) {
    		  if (newVal !== oldVal) {
    			  // user requested another page size; get items from server (could be optimised I guess if page size is lower)
    			  var startIdx = $scope.pagingOptions.currentPage ? $scope.pagingOptions.currentPage - 1 : 0;
    			  foundset.loadRecordsAsync(startIdx * oldVal, newVal);
    			  $scope.pagingOptions.currentPage = Math.floor(startIdx / newVal + 1);
    			  updatePageCount();
    			  // TODO show some loading feedback to the user until these records are received
    		  }
    	  });
    	  
    	  // startIndex can change serverSize if a record was deleted from before it; server viewport follows first record, not first index
    	  $scope.$watch('model.relatedFoundset.viewPort.startIndex', function(newVal, oldVal) {
    		  // if the server foundset changes and that results in startIndex change (as the viewport will follow first record), see if the page number needs adjusting
    		  $scope.pagingOptions.currentPage = getCurrentPage();
    		  updatePageCount();
    	  });

    	  // size can change serverside if records get deleted by someone else and there are no other records to fill the viewport with (by sliding)
    	  $scope.$watch('model.relatedFoundset.viewPort.size', function(newVal, oldVal) {
    		  if (newVal === 0) {
    			  // For example when the for record changed, resulting in a related foundset change, viewPort will have to be requested again
    			  // starting from 0 (it is set serverside to 0 - 0 as server doesn't know what we now need);
    			  // The same can happen if all records in the viewport get deleted and there are no more available to fill the gap...
    			  var startReq = Math.floor(Math.min(foundset.viewPort.startIndex, Math.max(0, foundset.serverSize - 1)) / $scope.pagingOptions.pageSize) * $scope.pagingOptions.pageSize;
    			  $scope.model.relatedFoundset.loadRecordsAsync(startReq, Math.min(foundset.serverSize - startReq, $scope.pagingOptions.pageSize));
    		  }
    	  });

    	  $scope.$watch('model.relatedFoundset.serverSize', function(newVal, oldVal) {
    		  if (newVal > 0 && foundset.viewPort.size === 0 && $scope.pagingOptions.pageSize > 0) {
    			  // initial new foundset show
    			  $scope.model.relatedFoundset.loadRecordsAsync(0, Math.min(newVal, $scope.pagingOptions.pageSize));
    		  } else if (foundset.viewPort.startIndex + foundset.viewPort.size < newVal && foundset.viewPort.size < $scope.pagingOptions.pageSize) $scope.model.relatedFoundset.loadExtraRecordsAsync(Math.min(newVal - foundset.viewPort.startIndex - foundset.viewPort.size, $scope.pagingOptions.pageSize - foundset.viewPort.size));
    		  updatePageCount();
    	  });
    	  
    	  $scope.$watchCollection('model.relatedFoundset.viewPort.rows', function() {
    		  // TODO check to see if we have obsolete columns in rowProxyObjects[...] - and clean them up + remove two way binding watches (we have to keep somewhere references to the removal functions when we register row watches)
    		  // allow nggrid to update it's model / selected items and make sure selection didn't fall/remain on a wrong item because of that update...
    		  $scope.$evalAsync(function () { updateGridSelectionFromFoundset(); });
    	  });
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

    		  var cellTemplate = '<' + el.componentDirectiveName + ' name="' + el.name + '" svy-model="getMergedCellModel(row, ' + idx + ')" svy-api="cellApiWrapper(row, ' + idx + ')" svy-handlers="cellHandlerWrapper(row, ' + idx + ')" svy-apply="cellApplyHandlerWrapper(row, ' + idx + ')"/>' 
    		  if($scope.model.multiLine) { 
    			  if($scope.rowHeight == undefined || (!$scope.model.rowHeight && ($scope.rowHeight < elY + el.model.size.height))) {
    				  $scope.rowHeight = $scope.model.rowHeight ? $scope.model.rowHeight : elY + el.model.size.height;
    			  }
    			  rowTemplate = rowTemplate + '<div ng-style="getMultilineComponentWrapperStyle(' + idx + ')" >' + cellTemplate + '</div>';
    		  }
    		  else {
    			  if($scope.rowHeight == undefined || $scope.rowHeight < el.model.size.height) {
    				  $scope.rowHeight = el.model.size.height;
    			  }
        		  $scope.columnDefinitions.push({
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
    		  for (var i = foundset.viewPort.rows.length - 1; i >= 0; i--)
    			  if (foundset.viewPort.rows[i][$foundsetTypeConstants.ROW_ID_COL_KEY] === rowId) return i;
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
    	  
    	  $scope.getMultilineComponentWrapperStyle = function(elementIndex) {
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
    	  $scope.getMergedCellModel = function(ngGridRow, elementIndex) {
    		  var cellProxies = getOrCreateElementProxies(ngGridRow.getProperty($foundsetTypeConstants.ROW_ID_COL_KEY), elementIndex);
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
    					  cellData[propertyName] = ngGridRow.getProperty(dataprovider);
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
    			  if (elements[elementIndex].forFoundset && elements[elementIndex].forFoundset.apiCallTypes) {
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
    	  $scope.cellApiWrapper = function(ngGridRow, elementIndex) {
    		  var cellProxies = getOrCreateElementProxies(ngGridRow.getProperty($foundsetTypeConstants.ROW_ID_COL_KEY), elementIndex);

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

    	  $scope.cellApplyHandlerWrapper = function(ngGridRow, elementIndex) {
    		  var rowId = ngGridRow.getProperty($foundsetTypeConstants.ROW_ID_COL_KEY);
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
    	  var selectedItemsProxy = []; // keeps entire foundset row items from model.relatedFoundset.viewPort.rows
    	  var unreg = $scope.$on('ngGridEventData', function(){
        	  for (var idx = 0;  idx < foundset.selectedRowIndexes.length; idx++) {
        		  var rowIdx = foundset.selectedRowIndexes[idx];
        		  if (isInViewPort(rowIdx)) $scope.gridOptions.selectRow(absoluteToViewPort(rowIdx), true);
        	  }
        	  unreg(); // deregister, as this was only meant to execute as initial show
    	  });
    	  function updateFoundsetSelectionFromGrid(newNGGridSelectedItems) {
    		  if (selectedItemsProxy.length == 0 && $scope.model.relatedFoundset.serverSize > 0) return; // we shouldn't try to set no selection if there are records; it will be an invalid request as server doesn't allow that
			  // update foundset object selection when it changes in ngGrid
    		  var tmpSelectedRowIdxs = {};
			  for (var idx = 0; idx < newNGGridSelectedItems.length; idx++) {
				  var absRowIdx = rowIdToAbsoluteRowIndex(newNGGridSelectedItems[idx][$foundsetTypeConstants.ROW_ID_COL_KEY]);
				  if (!isRowIndexSelected(absRowIdx)) foundset.selectedRowIndexes.push(absRowIdx);
				  tmpSelectedRowIdxs['_' + absRowIdx] = true;
			  }
			  for (var idx = 0; idx < foundset.selectedRowIndexes.length; idx++) {
				  if (!tmpSelectedRowIdxs['_' + foundset.selectedRowIndexes[idx]]) {
					  // here we also handle the case when in multiselect there are records selected outside of viewport
					  // in that case if CTRL was pressed then this $watch should not remove those records from selection
					  // TODO if CTRL was not pressed in multi-selection, the outside-of-viewPort-selectedIndexes should be cleared as well - but not by this $watch code
					  if (!foundset.multiSelect || isInViewPort(idx)) foundset.selectedRowIndexes.splice(idx, 1);
				  }
			  }
			  // it is important that at the end of this function, the two arrays are in sync; otherwise, watch loops may happen
		  }
    	  var updateGridSelectionFromFoundset = function(newFSSelectedItems) {
			  // update ngGrid selection when it changes in foundset
    		  if (!newFSSelectedItems) newFSSelectedItems = foundset.selectedRowIndexes;
    			  
    		  var tmpSelectedRowIdxs = {};
			  for (var idx = 0; idx < selectedItemsProxy.length; idx++) {
				  var absRowIdx = rowIdToAbsoluteRowIndex(selectedItemsProxy[idx][$foundsetTypeConstants.ROW_ID_COL_KEY]);
				  if (absRowIdx >= 0) {
					  if (newFSSelectedItems.indexOf(absRowIdx) < 0) {
						  $scope.gridOptions.selectItem(absoluteToViewPort(absRowIdx), false); // it seems nggrid doesn't really watch the selection array so we have to do this manually
					  }
				  } // else probably the foundset model was changed and ngGrid didn't run it's watches yet to update selectedItems
				  tmpSelectedRowIdxs['_' + absRowIdx] = true;
			  }
			  for (var idx = 0; idx < newFSSelectedItems.length; idx++) {
				  var rowIdx = newFSSelectedItems[idx];
				  if (isInViewPort(rowIdx) && !tmpSelectedRowIdxs['_' + rowIdx]) {
					  $scope.gridOptions.selectItem(absoluteToViewPort(rowIdx), true); // it seems nggrid doesn't really watch the selection array so we have to do this manually
				  }
			  }
			  // it is important that at the end of this function, the two arrays are in sync; otherwise, watch loops may happen
		  };
    	  $scope.$watchCollection(function() { return selectedItemsProxy; }, updateFoundsetSelectionFromGrid);
    	  $scope.$watchCollection('model.relatedFoundset.selectedRowIndexes', updateGridSelectionFromFoundset);

    	  updatePageCount();
    	  $scope.gridOptions = {
    			  data: 'model.relatedFoundset.viewPort.rows',
    			  enableCellSelection: true,
    			  enableRowSelection: true,
    			  enableColumnResize: true,
    			  selectedItems: selectedItemsProxy,
    			  multiSelect: foundset.multiSelect,
    			  enablePaging: false,
    			  showFooter: false,
    			  totalServerItems: 'artificialServerSize', // we sometimes fake a page for the sake of following the first selected record with viewport - so we can't use real size here; see updatePageCount()
    			  pagingOptions: $scope.pagingOptions,
    			  primaryKey: $foundsetTypeConstants.ROW_ID_COL_KEY, // not currently documented in ngGrid API but is used internally and useful - see ngGrid source code
    			  columnDefs: 'columnDefinitions',
    			  headerRowHeight: $scope.model.multiLine ? 0 : $scope.model.headerHeight,
    			  rowHeight: $scope.rowHeight?$scope.rowHeight:20
    	  };
    	  $scope.styleClass = 'svyPortalGridStyle';
    	  
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
    		  var rowId = ngGridRow.getProperty($foundsetTypeConstants.ROW_ID_COL_KEY);
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
      link: function (scope, element, attrs) {
    	  var sc = element.find('.svyPortalGridStyle').scope();
    	  scope.$watch(function() { return sc.viewportDimHeight() }, function(newViewportHeight) {
    		  scope.pagingOptions.pageSize = (scope.rowHeight == 0) ? 1 : Math.max(Math.floor(newViewportHeight / scope.rowHeight), 1);
    		  scope.pagingOptions.pageSizes = [scope.pagingOptions.pageSize]; // TODO can we make it scoll if we allow clients to change page size manually to other values as well?
    	  });
      },
      templateUrl: 'servoydefault/portal/portal.html',
      replace: true
    };
}]);
