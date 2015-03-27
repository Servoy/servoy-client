angular.module('servoydefaultPortal',['sabloApp','servoy','ui.grid','ui.grid.selection','ui.grid.moveColumns','ui.grid.resizeColumns','ui.grid.infiniteScroll','ui.grid.cellNav'])
.directive('servoydefaultPortal', ["$sabloUtils", '$utils', '$foundsetTypeConstants', '$componentTypeConstants', 
                                   '$timeout', '$solutionSettings', '$anchorConstants', 
                                   'gridUtil','uiGridConstants','$scrollbarConstants',"uiGridMoveColumnService",
                                   function($sabloUtils, $utils, $foundsetTypeConstants, $componentTypeConstants, 
                                		   $timeout, $solutionSettings, $anchorConstants,
                                		   gridUtil, uiGridConstants, $scrollbarConstants, uiGridMoveColumnService) {  
	return {
		restrict: 'E',
		scope: {
			model: "=svyModel",
			handlers: "=svyHandlers",
			api: "=svyApi",
			servoyApi: "=svyServoyapi"
		},
		link: function($scope, $element, $attrs) {
			// START TEST MODELS

			// rowProxyObjects[pk][elementIndex].
			//                                  .mergedCellModel  - is the actual cell element svyModel cache
			//                                  .cellApi          - is the actual cell element API cache
			//                                  .cellHandlers     - is the actual cell element handlers cache
			//                                  .cellServoyApi 	  - is the actual cell element Servoy Api cache
			var rowProxyObjects = {};

			$scope.pageSize = 25;

			$scope.foundset = null;
			var elements = $scope.model.childElements;
			$scope.$watch('model.relatedFoundset', function(newVal, oldVal) {
				if (!$scope.foundset && newVal && newVal.viewPort && newVal.viewPort.size > 0){
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

			function disposeOfRowProxies(rowProxy) {
				for (var elIdx in rowProxy) {
					if (rowProxy[elIdx].unwatchFuncs) {
						rowProxy[elIdx].unwatchFuncs.forEach(function (f) { f(); });
					}
					if (rowProxy[elIdx].propertyUnwatchFuncs) {
						var propertyUnwatchFuncs = rowProxy[elIdx].propertyUnwatchFuncs;
						for (var propKey in propertyUnwatchFuncs) {
							propertyUnwatchFuncs[propKey].forEach(function (f) { f(); });
						};
					}
				}
			}

			var oldCellAPIs;
			$scope.$watchCollection('model.childElements', function(newVal, oldVal) {
				elements = $scope.model.childElements;
				if (newVal != oldVal) {
					// either a component was added/removed/changed or the whole array changed
					// we can optimize this in the future but for now just dump all model/api/handlers for them to get auto recreated
					for (var someKey in rowProxyObjects)
						disposeOfRowProxies(rowProxyObjects[someKey]);
					
					// add back apis cause incomming are probably fresh uninitialized ones {}
					// and ui-grid tends to reuse existing components which have no reason to re-populate their api object...
					var oldIndexToNewIndex = {};
					for (var newIdx in newVal) {
						var newElemName = newVal[newIdx].name;
						for (var oldIdx in oldVal) {
							if (newElemName === oldVal[oldIdx].name) {
								oldIndexToNewIndex[oldIdx] = newIdx;
								break;
							}
						}
					}
					oldCellAPIs = {};
					for (var rowId in rowProxyObjects) {
						var rowProxies = rowProxyObjects[rowId];
						oldCellAPIs[rowId] = {};
						for (var columnIdx in rowProxies) {
							if (rowProxies[columnIdx].cellApi && angular.isDefined(oldIndexToNewIndex[columnIdx])) oldCellAPIs[rowId][oldIndexToNewIndex[columnIdx]] = rowProxies[columnIdx].cellApi;
						}
					}
					
					rowProxyObjects = {};
				}
			})

//			function onAllCells(f) {
//				for (var pk in rowProxyObjects)
//					for (var elementIndex in rowProxyObjects[pk])
//						f(rowProxyObjects[pk][elementIndex], pk, elementIndex);
//			}
//			
			$scope.rowHeight = $scope.model.rowHeight;

			var rowTemplate = ''
			var rowWidth = 0;

			$scope.columnDefinitions = [];
			
			function getColumnTitle(idx) {
				var columnTitle = null;
				if ($scope.model.columnHeaders && idx < $scope.model.columnHeaders.length)
				{
					columnTitle = $scope.model.columnHeaders[idx];
				}	
				if (!columnTitle) columnTitle = elements[idx].model.text;
	//			if (!columnTitle) {
	//				// TODO use beautified dataProvider id or whatever other clients use as default, not directly the dataProvider id
	//				if (el.foundsetConfig && el.foundsetConfig.recordBasedProperties && el.foundsetConfig.recordBasedProperties.length > 0) {
	//					columnTitle = el.foundsetConfig.recordBasedProperties[0];
	//					if (columnTitle && columnTitle.indexOf('.') >= 0) {
	//						columnTitle = columnTitle.substring(columnTitle.lastIndexOf('.'));
	//					}
	//				}
	//				if (!columnTitle) columnTitle = "";
	//			}
				if (!columnTitle) columnTitle = "";
				return columnTitle;
			}

			if (elements)
			{
				for (var idx = 0; idx < elements.length; idx++) {
					var el = elements[idx]; 
					var elY = el.model.location.y - $scope.model.location.y;
					var elX = el.model.location.x - $scope.model.location.x;
					
					var columnTitle = getColumnTitle(el.componentIndex ? el.componentIndex : idx);

					var portal_svy_name = $element[0].getAttribute('data-svy-name');
					var cellTemplate = '<' + el.componentDirectiveName + ' name="' + el.name
						+ '" svy-model="grid.appScope.getMergedCellModel(row, ' + idx
						+ ')" svy-api="grid.appScope.cellApiWrapper(row, ' + idx
						+ ')" svy-handlers="grid.appScope.cellHandlerWrapper(row, ' + idx
						+ ')" svy-servoyApi="grid.appScope.cellServoyApiWrapper(row, ' + idx + ')"';
					if (portal_svy_name) cellTemplate += " data-svy-name='" + portal_svy_name + "." + el.name + "'";
					cellTemplate += '/>';
					if($scope.model.multiLine) { 
						if($scope.rowHeight == undefined || (!$scope.model.rowHeight && ($scope.rowHeight < elY + el.model.size.height))) {
							$scope.rowHeight = $scope.model.rowHeight ? $scope.model.rowHeight : elY + el.model.size.height;
						}
						if (rowWidth < (elX + el.model.size.width) ) {
							rowWidth = elX + el.model.size.width;
						}
						rowTemplate = rowTemplate + '<div ng-style="grid.appScope.getMultilineComponentWrapperStyle(' + idx + ')" >' + cellTemplate + '</div>';
					}
					else {
						if($scope.rowHeight == undefined || ($scope.model.rowHeight == 0 && $scope.rowHeight < el.model.size.height)) {
							$scope.rowHeight = el.model.size.height;
						}
						var isResizable = ((el.model.anchors & $anchorConstants.EAST) != 0) && ((el.model.anchors & $anchorConstants.WEST) != 0) 
						var isMovable = ((el.model.anchors & $anchorConstants.NORTH) === 0) || ((el.model.anchors & $anchorConstants.SOUTH) === 0) 
						var isSortable = $scope.model.sortable && el.foundsetConfig.recordBasedProperties.length > 0; // TODO update uigrid when recordBasedProperties change 
						$scope.columnDefinitions.push({
							name:el.name,
							displayName: columnTitle,
							cellTemplate: cellTemplate,
							visible: el.model.visible,
							width: el.model.size.width,
							minWidth: el.model.size.width,
							cellEditableCondition: false,
							enableColumnMoving: isMovable,
							enableColumnResizing: isResizable,
							enableColumnMenu: isSortable,
							enableSorting:isSortable,
							enableHiding: false,
							allowCellFocus: false,
							svyColumnIndex: el.componentIndex ? el.componentIndex : idx
						});					
						updateColumnDefinition($scope, idx);
					}
				}
			}	


			if($scope.model.multiLine) {
				$scope.columnDefinitions.push({
					width: rowWidth,
					cellTemplate: rowTemplate,
					name: "unique",
					cellEditableCondition: false,
					allowCellFocus: false
				});
			}

			function updateColumnDefinition(scope, idx) {
				scope.$watch('model.childElements[' + idx + '].model.visible', function (newVal, oldVal) {
					scope.columnDefinitions[idx].visible = scope.model.childElements[idx].model.visible;
				}, false);
				
				scope.$watch('model.childElements[' + idx + '].model.size.width', function (newVal, oldVal) {
					if(newVal !== oldVal)
					{
						scope.columnDefinitions[idx].width = scope.model.childElements[idx].model.size.width;
					}
				}, false);

				scope.$watchCollection('model.columnHeaders', function (newVal, oldVal) {
					if(newVal !== oldVal && newVal)
					{
						for (var idx = 0; idx < scope.columnDefinitions.length; idx++)
							scope.columnDefinitions[idx].displayName = getColumnTitle(scope.columnDefinitions[idx].displayName.svyColumnIndex ? scope.columnDefinitions[idx].displayName.svyColumnIndex : idx);
						
						scope.gridApi.grid.buildColumns();
					}
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
			var rowCache = {};
			function rowIdToViewportRelativeRowIndex(rowId) {
				var result = rowCache[rowId];
				var rows = $scope.foundset.viewPort.rows;
				if (result === undefined || !rows[result] || rows[result][$foundsetTypeConstants.ROW_ID_COL_KEY] !== rowId) {
					rowCache = {};
					for (var i = rows.length - 1; i >= 0; i--)
						rowCache[rows[i][$foundsetTypeConstants.ROW_ID_COL_KEY]] = i;
					result = rowCache[rowId];
				}
				return result;
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
						elLayout.bottom = containerModel.size.height - elModel.location.y - elModel.size.height + "px";
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
						elLayout.left = (elModel.location.x - $scope.model.location.x) + 'px';
					}
					else
					{
						elLayout.right = (elModel.location.x - $scope.model.location.x) + 'px';
					}
					elLayout.top = (elModel.location.y - $scope.model.location.y) + 'px';
					elLayout.width = elModel.size.width + 'px';
					elLayout.height = elModel.size.height + 'px';		   
				}

				if (typeof elModel.visible !== "undefined" && !elModel.visible) elLayout.display = 'none'; // undefined is considered visible by default

				return elLayout;
			}

			// merges component model and modelViewport (for record dependent properties like dataprovider/tagstring/...) the cell's element's model
			$scope.getMergedCellModel = function(ngGridRow, elementIndex) {
				// TODO - can we avoid using ngGrid undocumented "row.entity"? that is what ngGrid uses internally as model for default cell templates...
				var rowId = ngGridRow.entity[$foundsetTypeConstants.ROW_ID_COL_KEY];
				
				if(rowIdToViewportRelativeRowIndex(rowId) < 0) {
					return {}
				}
				
				var cellProxies = getOrCreateElementProxies(rowId, elementIndex);
				var cellModel = cellProxies.mergedCellModel;
				
				if (!cellModel) {
					var element = elements[elementIndex];

					var key;
					var cellData = {};

					// some properties that have default values might only be sent later to client;
					// if that happens, we need to then bind them two-way as well
					if (!angular.isDefined(cellProxies.unwatchFuncs)) {
						cellProxies.unwatchFuncs = [];
					}
					if (!angular.isDefined(cellProxies.propertyUnwatchFuncs)) {
						cellProxies.propertyUnwatchFuncs = {};
					}

					function updateTwoWayBindings(listWithProperties, oldList) {
						if (oldList === listWithProperties) return;
						var k;
						for (k in listWithProperties) {
							if (!cellProxies.propertyUnwatchFuncs[k]) {
								// skip this for special - row-by-row changing properties; it is handled separately later in code
								var skip = false;
								if (elements[elementIndex].foundsetConfig && elements[elementIndex].foundsetConfig.recordBasedProperties) {
									for (var i in elements[elementIndex].foundsetConfig.recordBasedProperties) {
										skip = (elements[elementIndex].foundsetConfig.recordBasedProperties[i] === k);
										if (skip) break;
									}
								}

								if (!skip) {
									// copy initial values
									if (angular.isUndefined(cellData[k])) cellData[k] = elements[elementIndex].model[k];
									else if (angular.isUndefined(elements[elementIndex].model[k])) elements[elementIndex].model[k] = cellData[k];

									// 2 way data link between element model and the merged cell model
									// it is a bit strange here as 1 element model will be 2 way bound to N cell models
									cellProxies.propertyUnwatchFuncs[k] = $utils.bindTwoWayObjectProperty(cellData, k, elements[elementIndex].model, k, false, $scope);
								}
							} 
						}
					};

					// properties like tagstring and dataprovider are not set directly on the component but are more linked to the current record
					// so we will take these from the foundset record and apply them to child elements
					if (element.foundsetConfig && element.foundsetConfig.recordBasedProperties) {
						for (var i in element.foundsetConfig.recordBasedProperties) {
							var propertyName = element.foundsetConfig.recordBasedProperties[i];
							if (angular.isDefined(element.modelViewport) && angular.isDefined(element.modelViewport[rowIdToViewportRelativeRowIndex(rowId)]))
								cellData[propertyName] = element.modelViewport[rowIdToViewportRelativeRowIndex(rowId)][propertyName];
							// 2 way data link between element record based properties from modelViewport and the merged cell model
							cellProxies.unwatchFuncs = cellProxies.unwatchFuncs.concat($utils.bindTwoWayObjectProperty(cellData, propertyName, elements, [elementIndex, "modelViewport", function() { return rowIdToViewportRelativeRowIndex(rowId); }, propertyName], false, $scope));
						}
					}

					updateTwoWayBindings(element.model);
					cellProxies.unwatchFuncs.push($scope.$watchCollection(function() { return elements[elementIndex].model; }, updateTwoWayBindings));
					cellProxies.unwatchFuncs.push($scope.$watchCollection(function() { return cellData; }, updateTwoWayBindings));

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
					else if (elements[elementIndex].foundsetConfig && elements[elementIndex].foundsetConfig.apiCallTypes) {
						callOnOneSelectedCellOnly = (elements[elementIndex].foundsetConfig.apiCallTypes[apiFunctionName] != $componentTypeConstants.CALL_ON_ONE_SELECTED_ROW);
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
			
			// function setElementFindMode(element,findMode, editable)
			// {
			// 	if (element.api.setFindMode)
			// 	{
			// 		if (!findMode && element.model.svy_wasEditable == undefined) return;
			// 		var isEditable;
			// 		if (findMode)
			// 		{
			// 			element.model.svy_wasEditable = element.model.editable;
			// 			isEditable = editable;
			// 		}
			// 		else
			// 		{
			// 			isEditable = element.model.svy_wasEditable;
			// 			delete element.model.svy_wasEditable;
			// 		}
			// 		element.api.setFindMode(findMode, isEditable);
			// 	}
			// 	else
			// 	{
			// 		if (!findMode && element.model.svy_readOnlyBeforeFindMode == undefined) return;
			// 		if (findMode)
			// 		{
			// 			element.model.svy_readOnlyBeforeFindMode = element.model.readOnly;
			// 			element.model.readOnly = !editable;
			// 		}
			// 		else
			// 		{
			// 			element.model.readOnly = element.model.svy_readOnlyBeforeFindMode;
			// 			delete element.model.svy_readOnlyBeforeFindMode;
			// 		}
			// 	}

			// }

			// function setElementsFindMode(findMode, editable)
			// {
			// 	for (var i =0; i < $scope.model.childElements.length; i++)
			// 	{
			// 		setElementFindMode($scope.model.childElements[i], findMode, editable);
			// 	}
			// }
			
			// cells provide API calls; one API call (from server) should execute on all cells of that element's column.
			// so any API provided by a cell is added to the server side controlled API object; when server calls that API method,
			// it will execute on all cells
			$scope.cellApiWrapper = function(ngGridRow, elementIndex) {
				var rowId = ngGridRow.entity[$foundsetTypeConstants.ROW_ID_COL_KEY];
				if(rowIdToViewportRelativeRowIndex(rowId) < 0) {
					return {}
				}
				
				var cellProxies = getOrCreateElementProxies(rowId, elementIndex);
				
				if (!cellProxies.cellApi) {

					if (!angular.isDefined(cellProxies.unwatchFuncs)) cellProxies.unwatchFuncs = [];
					var columnApi = elements[elementIndex].api;
					cellProxies.cellApi = {}; // new cell API object
					cellProxies.unwatchFuncs.push($scope.$watchCollection(function() { return cellProxies.cellApi; }, function(newCellAPI) {
						updateColumnAPIFromCell(columnApi, newCellAPI, elementIndex);
						if ($scope.model.svy_findMode!=undefined)
						{
							setElementFindMode($scope.model.childElements[elementIndex], $scope.model.svy_findMode, $scope.model.svy_editable);
						}
					}));
					
					// if the elements array changed, and due to the fact that ui-grid reuses components, re-apply old cellAPIs if available
					// this is rarely the case, but it can happen
					oldCellApi = $sabloUtils.getInDepthProperty(oldCellAPIs, rowId, elementIndex);
					if (angular.isDefined(oldCellApi)) {
						cellProxies.cellApi = oldCellApi;
						updateColumnAPIFromCell(columnApi, cellProxies.cellApi, elementIndex);

						delete oldCellAPIs[rowId][elementIndex];
						var l = 0;
						for (var tmp in oldCellAPIs[rowId]) { l++; break; }
						if (l === 0) delete oldCellAPIs[rowId];;
						l = 0;
						for (var tmp in oldCellAPIs) { l++; break; }
						if (l === 0) oldCellAPIs = undefined;
					}
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

			$scope.cellServoyApiWrapper = function(ngGridRow, elementIndex) {
				var rowId = ngGridRow.entity[$foundsetTypeConstants.ROW_ID_COL_KEY];
				if(rowIdToViewportRelativeRowIndex(rowId) < 0) {
					return {}
				}
				
				var cellProxies = getOrCreateElementProxies(rowId, elementIndex);

				if (!cellProxies.cellServoyApi) {
					var columnServoyApi = elements[elementIndex].servoyApi;
					cellProxies.cellServoyApi = {
							formWillShow: $scope.servoyApi.formWillShow,
							hideForm: $scope.servoyApi.hideForm,
							setFormEnabled: $scope.servoyApi.setFormEnabled,
							setFormReadOnly: $scope.servoyApi.setFormReadOnly,
							getFormUrl: $scope.servoyApi.getFormUrl,
							startEdit: function(property) {
								return columnServoyApi.startEdit(property,rowId);
							},
							apply: function(property)
							{
								return columnServoyApi.apply(property,cellProxies.mergedCellModel,rowId);
							}
					}
				}
				return cellProxies.cellServoyApi;
			}

			var updatingGridSelection = false;
			
			// bind foundset.selectedRowIndexes to what nggrid has to offer
			function updateFoundsetSelectionFromGrid(newNGGridSelectedItems) {
				if (updatingGridSelection) return;
				if (newNGGridSelectedItems.length == 0 && $scope.model.relatedFoundset.serverSize > 0) return; // we shouldn't try to set no selection if there are records; it will be an invalid request as server doesn't allow that
				// update foundset object selection when it changes in ngGrid
				var tmpSelectedRowIdxs = {};
				for (var idx = 0; idx < newNGGridSelectedItems.length; idx++) {
					var absRowIdx = rowIdToAbsoluteRowIndex(newNGGridSelectedItems[idx][$foundsetTypeConstants.ROW_ID_COL_KEY]);
					if (!isRowIndexSelected(absRowIdx)) $scope.foundset.selectedRowIndexes.push(absRowIdx);
					tmpSelectedRowIdxs['_' + absRowIdx] = true;
				}
				for (var idx = $scope.foundset.selectedRowIndexes.length-1; idx >= 0; idx--) {
					if (!tmpSelectedRowIdxs['_' + $scope.foundset.selectedRowIndexes[idx]]) {
						// here we also handle the case when in multiselect there are records selected outside of viewport
						// in that case if CTRL was pressed then this $watch should not remove those records from selection
						// TODO if CTRL was not pressed in multi-selection, the outside-of-viewPort-selectedIndexes should be cleared as well - but not by this $watch code
						if (!$scope.foundset.multiSelect || isInViewPort(idx)) $scope.foundset.selectedRowIndexes.splice(idx, 1);
					}
				}
				updateGridSelectionFromFoundset(false);
				// it is important that at the end of this function, the two arrays are in sync; otherwise, watch loops may happen
			}
			var updateGridSelectionFromFoundset = function(scrollToSelection) {
				$scope.$evalAsync(function () {
					if ($scope.foundset)
					{
						var rows = $scope.foundset.viewPort.rows;
						updatingGridSelection = true; 
						if (rows.length > 0 && $scope.foundset.selectedRowIndexes.length > 0) {
							var scrolledToSelection = !scrollToSelection;
							var oldSelection = $scope.gridApi.selection.getSelectedRows();
							// if first item in the old selection is the same as the first in the new selection,
							// then ignore scrolling
							if(oldSelection.length > 0 && rows[$scope.foundset.selectedRowIndexes[0]] &&
									(oldSelection[0]._svyRowId == rows[$scope.foundset.selectedRowIndexes[0]]._svyRowId)) {
								scrolledToSelection = true;
							}
							
							for (var idx = 0;  idx < $scope.foundset.selectedRowIndexes.length; idx++) {
								var rowIdx = $scope.foundset.selectedRowIndexes[idx];
								if (isInViewPort(rowIdx)) {
									$scope.gridApi.selection.selectRow(rows[rowIdx]);
									if(!scrolledToSelection) {
										scrolledToSelection = true;
										$timeout(function() { $scope.gridApi.cellNav.scrollTo(rows[rowIdx]); }, 0);
									}
								} else if(!scrolledToSelection) {
									var nrRecordsToLoad = 0;
									if(rowIdx < $scope.foundset.viewPort.startIndex) {
										nrRecordsToLoad = rowIdx - $scope.foundset.viewPort.startIndex;
									} else {
										nrRecordsToLoad = rowIdx - $scope.foundset.viewPort.size + 1;
									}
									$scope.foundset.loadExtraRecordsAsync(nrRecordsToLoad);
									break;
								}
							}
						} else if (rows.length > 0) {
							$scope.gridApi.selection.selectRow(rows[0]);
							$scope.gridApi.cellNav.scrollTo($scope, rows[0], null);
						}
						updatingGridSelection = false;
					}
				});
				// it is important that at the end of this function, the two arrays are in sync; otherwise, watch loops may happen
			};
			$scope.$watchCollection('foundset.selectedRowIndexes', function() { updateGridSelectionFromFoundset(true) });
			
			$scope.gridOptions = {
					data: 'foundset.viewPort.rows',
					enableRowSelection: true,
					enableRowHeaderSelection: false,
					excludeProperties: [$foundsetTypeConstants.ROW_ID_COL_KEY],
					multiSelect: false,
					modifierKeysToMultiSelect: true,
					noUnselect: true,
					enableColumnMoving : $scope.model.reorderable,
					enableVerticalScrollbar: uiGridConstants.scrollbars.WHEN_NEEDED,
					enableHorizontalScrollbar: uiGridConstants.scrollbars.WHEN_NEEDED,
					followSourceArray:true,
					useExternalSorting: true,
					primaryKey: $foundsetTypeConstants.ROW_ID_COL_KEY, // not currently documented in ngGrid API but is used internally and useful - see ngGrid source code
					rowTemplate: 'svy-ui-grid/ui-grid-row',
					columnDefs: $scope.columnDefinitions,
					rowHeight: $scope.rowHeight ? $scope.rowHeight : 20,
					showHeader:$scope.model.headerHeight != 0 && !$scope.model.multiLine,
					headerRowHeight: $scope.model.multiLine ? 0 : $scope.model.headerHeight,
					rowIdentity: function(o) {
						return o._svyRowId;
					},
					rowEquality: function(row1,row2) {
						return row1._svyRowId == row2._svyRowId; 
					}
			};
			
			if ($scope.model.scrollbars & $scrollbarConstants.VERTICAL_SCROLLBAR_ALWAYS)
					$scope.gridOptions.enableVerticalScrollbar = uiGridConstants.scrollbars.ALWAYS;
			else if ( $scope.model.scrollbars & $scrollbarConstants.VERTICAL_SCROLLBAR_NEVER)
					$scope.gridOptions.enableVerticalScrollbar = uiGridConstants.scrollbars.NEVER;
			
			if ($scope.model.scrollbars & $scrollbarConstants.HORIZONTAL_SCROLLBAR_ALWAYS)
					$scope.gridOptions.enableHorizontalScrollbar = uiGridConstants.scrollbars.ALWAYS;
			else if ( $scope.model.scrollbars & $scrollbarConstants.HORIZONTAL_SCROLLBAR_NEVER)
					$scope.gridOptions.enableHorizontalScrollbar = uiGridConstants.scrollbars.NEVER;
			
			$scope.gridOptions.onRegisterApi = function( gridApi ) {
				$scope.gridApi = gridApi;
				$scope.gridApi.grid.registerDataChangeCallback(function() {
					updateGridSelectionFromFoundset(true);
				},[uiGridConstants.dataChange.ROW]);
				gridApi.selection.on.rowSelectionChanged($scope,function(row){
					updateFoundsetSelectionFromGrid(gridApi.selection.getSelectedRows())
				});
				gridApi.infiniteScroll.on.needLoadMoreData($scope,function(){
					$scope.foundset.loadExtraRecordsAsync(Math.min($scope.pageSize, $scope.foundset.serverSize - $scope.foundset.viewPort.size));
				});
			    gridApi.infiniteScroll.on.needLoadMoreDataTop($scope,function(){
			    	$scope.gridApi.infiniteScroll.dataLoaded();
				});
				gridApi.colMovable.on.columnPositionChanged ($scope, function(colDef, originalPosition, newPosition) {
					var reorderedColumns = $scope.gridApi.grid.columns;
					for (var k = 0; k < reorderedColumns.length; k++) {
						for(var i = 0; i < $scope.model.childElements.length; i++) {
							if(reorderedColumns[k].name == $scope.model.childElements[i].name) {
								$scope.model.childElements[i].model.location.x = k;
								break;
							}
						}
					}
				});
				gridApi.core.on.sortChanged ($scope, function( grid, sortColumns ) {
					// call the server (through foundset type)
					var columns = [];
					for (var i = 0; i < sortColumns.length; i++)
					{
						columns[i] = {name: sortColumns[i].name, direction : sortColumns[i].sort.direction};
					}
					$scope.foundset.sort(columns);
				});
				gridApi.colResizable.on.columnSizeChanged ($scope, function(colDef, deltaChange) {
					for(var i = 0; i < $scope.model.childElements.length; i++) {
						if(colDef.name == $scope.model.childElements[i].name) {
							$scope.model.childElements[i].model.size.width += deltaChange;
							break;
						}
					}
				});

				var requestViewPortSize = -1;
				function testNumberOfRows() {
					if ($scope.foundset)
					{
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
				}
				
				function layoutColumnsAndGrid() {
					$scope.gridApi.grid.gridWidth = gridUtil.elementWidth($element);
					$scope.gridApi.grid.gridHeight = gridUtil.elementHeight($element);
					if (!$scope.model.multiLine && (($scope.model.scrollbars & $scrollbarConstants.HORIZONTAL_SCROLLBAR_NEVER) == $scrollbarConstants.HORIZONTAL_SCROLLBAR_NEVER))
					{
						var totalWidth = 0;
						var resizeWidth = 0;
						for(var i = 0; i < $scope.model.childElements.length; i++) 
						{
							totalWidth += $scope.model.childElements[i].model.size.width;
							var isResizable = (($scope.model.childElements[i].model.anchors & $anchorConstants.EAST) != 0) && (($scope.model.childElements[i].model.anchors & $anchorConstants.WEST) != 0);
							if (isResizable)
							{
								resizeWidth += $scope.model.childElements[i].model.size.width;
							}
						}
						totalWidth = $scope.gridApi.grid.gridWidth - totalWidth;
					    if (typeof($scope.gridApi.grid.scrollbarWidth) !== 'undefined' && $scope.gridApi.grid.scrollbarWidth !== undefined && $scope.gridApi.grid.scrollbarWidth > 0) {
					    	totalWidth = totalWidth - $scope.gridApi.grid.scrollbarWidth;
					    }
						if (resizeWidth > 0 && totalWidth > 0)
						{
							for(var i = 0; i < $scope.model.childElements.length; i++) 
							{
								var isResizable = (($scope.model.childElements[i].model.anchors & $anchorConstants.EAST) != 0) && (($scope.model.childElements[i].model.anchors & $anchorConstants.WEST) != 0);
								if (isResizable)
								{
									// calculate new width based on weight
									var elemWidth = $scope.model.childElements[i].model.size.width;
									var newWidthDelta = elemWidth * totalWidth / resizeWidth;
									$scope.gridApi.grid.columns[i].width = elemWidth + newWidthDelta;
								}
							}
						}
					}
					$scope.gridApi.grid.refreshCanvas(true);
				}
				
				$timeout(function(){
					layoutColumnsAndGrid();
					
					// watch for resize and re-layout when needed - but at most once every 200 ms
					function justToIsolateScope() {
						var minRelayoutPeriodPassed = true;
						var pendingLayout = false;
						var elementSize = [ gridUtil.elementWidth($element), gridUtil.elementHeight($element) ];
						var timeoutPromise = null;
						function getNewSize() {
							var newSize = [ gridUtil.elementWidth($element), gridUtil.elementHeight($element) ];
							if (newSize[0] != elementSize[0] || newSize[1] != elementSize[1]){
								elementSize = newSize;
								$scope.$apply();
							}
						}
						$scope.$watch(function() { 
							if (timeoutPromise) $timeout.cancel(timeoutPromise);
							timeoutPromise = $timeout(getNewSize,500, false);
							return elementSize;
						  }, 
							function(oldV, newV) {
							if (oldV != newV) {
								// the portal resized (browser window resize or split pane resize for example)
								if (pendingLayout) return; // will layout later anyway
								if (minRelayoutPeriodPassed) {
									layoutColumnsAndGrid();
									
									minRelayoutPeriodPassed = false;
									function wait200ms() {
										if (pendingLayout) {
											pendingLayout = false;
											layoutColumnsAndGrid();
											$timeout(wait200ms, 200);
										} else minRelayoutPeriodPassed = true;
									}
									$timeout(wait200ms, 200);
								} else pendingLayout = true;
							}
						})
					}
					justToIsolateScope();
					
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
						rowCache = {};
						// check to see if we have obsolete columns in rowProxyObjects[...] - and clean them up + remove two way binding and any other watches
						var newRowIDs = {};
						if ($scope.foundset && $scope.foundset.viewPort && $scope.foundset.viewPort.rows) {
							for (var idx in $scope.foundset.viewPort.rows) {
								newRowIDs[$scope.foundset.viewPort.rows[idx][$foundsetTypeConstants.ROW_ID_COL_KEY]] = true;
							}
						}
						for (var oldRowId in rowProxyObjects) {
							if (!newRowIDs[oldRowId]) {
								disposeOfRowProxies(rowProxyObjects[oldRowId]);
								var oldCellApi = $sabloUtils.getInDepthProperty(oldCellAPIs, oldRowId);
								if (angular.isDefined(oldCellApi)) delete oldCellAPIs[oldRowId];
								delete rowProxyObjects[oldRowId];
							}
						}

						// allow nggrid to update it's model / selected items and make sure selection didn't fall/remain on a wrong item because of that update...
						updateGridSelectionFromFoundset(true);
						$scope.gridApi.infiniteScroll.dataLoaded();
					});
				},0)
			};
			$scope.styleClass = 'svyPortalGridStyle';

			function linkHandlerToRowIdWrapper(handler, rowId) {
				return function() {
					var entity = $scope.foundset.viewPort.rows[rowIdToViewportRelativeRowIndex(rowId)]
					$scope.gridApi.selection.selectRow(entity); // TODO for multiselect - what about modifiers such as CTRL? then it might be false
					updateFoundsetSelectionFromGrid($scope.gridApi.selection.getSelectedRows()); // otherwise the watch/digest will update the foundset selection only after the handler was triggered...
					var recordHandler = handler.selectRecordHandler(rowId)
					return recordHandler.apply(recordHandler, arguments);
				}
			}
			// each handler at column level gets it's rowId from the cell's wrapper handler below (to
			// make sure that the foundset's selection is correct server-side when cell handler triggers)
			$scope.cellHandlerWrapper = function(ngGridRow, elementIndex) {
				var rowId = ngGridRow.entity[$foundsetTypeConstants.ROW_ID_COL_KEY];
				if(rowIdToViewportRelativeRowIndex(rowId) < 0) {
					return {}
				}
				
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
			

			// // special method that servoy calls when this component goes into find mode.
			// $scope.api.setFindMode = function(findMode, editable) {				
			// 	$scope.model.svy_findMode = findMode;
			// 	$scope.model.svy_editable = editable;
			// 	setElementsFindMode($scope.model.svy_findMode, $scope.model.svy_editable);
			// };
		},
		templateUrl: 'servoydefault/portal/portal.html',
		replace: true
	};
}])
.run(['$templateCache', function($templateCache) {
	
  $templateCache.put('svy-ui-grid/ui-grid-row',
    "<div sablo-tabseq=\"rowRenderIndex + 1\" sablo-tabseq-config=\"{container: true}\"><div ng-repeat=\"(colRenderIndex, col) in colContainer.renderedColumns track by col.colDef.name\" class=\"ui-grid-cell\" ng-class=\"{ 'ui-grid-row-header-cell': col.isRowHeader }\" ui-grid-cell></div></div>"
  );

}]);
