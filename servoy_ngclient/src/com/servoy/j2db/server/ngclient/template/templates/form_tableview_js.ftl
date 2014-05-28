<#--
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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
-->
<#include "form_js_base.ftl"> 
<#macro form_js_body>
    
    var tmpRows;
	if ($scope.model['']) {
  		tmpRows = $scope.model[''].rows;
  		$scope.model[''].rows = [];
	}
	$scope.cellRender = function(row, columnName, columnModel) { 
		var cellModel = row.getProperty(columnName);
		if (!cellModel.svyInit) {
			function CellData() {
			}
			CellData.prototype = columnModel;
			cd = new CellData();
			cd.rowId = row.getProperty("_svyRowId");
			for(var p in cellModel) {
				cd[p] = cellModel[p]; 
			} 
			cd.svyInit = true;
			$scope.model[''].rows[row.rowIndex][columnName] = cd;
			cellModel = row.getProperty(columnName);
			
		} 
		return cellModel; 
	}
	if (!$scope.model['']) $scope.model[''] = {};
	 
	if (!$scope.model[''].currentPage) $scope.model[''].currentPage = 1;
	$scope.$watch('model..currentPage', function (newVal, oldVal) {
		if (newVal !== oldVal) {
			$scope.pagingOptions.currentPage = newVal;
		}
	}, false);
	
	$scope.pagingOptions = {pageSize: 0, currentPage: $scope.model[''].currentPage};
	if ($scope.model[''].pageSize) $scope.pagingOptions.pageSize = $scope.model[''].pageSize;
	$scope.$watch('pagingOptions.pageSize', function (newVal, oldVal) {
		if (newVal !== oldVal) {
			$scope.model[''].pageSize = newVal;
			$servoyInternal.callService('formService','requestdata',{formname:'${name}',pageSize:newVal});
		}
	}, false);
	$scope.$watch('pagingOptions.currentPage', function (newVal, oldVal) {
		if ($scope.model[''].currentPage != newVal) {
			$timeout(function(){ 
			    if ($scope.pagingOptions.currentPage == newVal) {
			    	$servoyInternal.callService('formService','requestdata',{formname:'${name}',currentPage:newVal});
				}
				else {
				console.log("not the same");
				}
			},200);
		}
	}, false);
	$scope.$watch('model..updatedRows', function (newVal, oldVal) {
		if (newVal && newVal !== oldVal) {
			var rows = $scope.model[''].rows;
			for(var key in newVal) {
				var item = newVal[key];
				var currentRow = item.startIndex;
				var updatedRows = item.rows;
				if (item.type == 1) {
					// insert, first make copy
					rows = rows.slice();
					for (var i=0;i<updatedRows.length;i++) {
						var sourceRow = updatedRows[i];
						rows.splice(currentRow++,0,sourceRow);
					}
					rows.splice(rows.length-updatedRows.length,updatedRows.length);
					// assign the new value
					$scope.model[''].rows = rows;
				}
				else if (item.type == 2) {
					// delete, first make copy
					if (updatedRows)
					{
						rows = rows.concat(updatedRows);
					}
					else
					{
						rows = rows.slice();
					}
					rows.splice(currentRow,item.endIndex-currentRow);
					$scope.model[''].rows = rows;
				}
				else { // change
					for (var i=0;i<updatedRows.length;i++) {
						var sourceRow = updatedRows[i];
						var targetRow = rows[currentRow++];
						// add new foundSet entries
						if(targetRow == null ) {
							rows[currentRow-1] = updatedRows[i]
						}else{
							for(var p in sourceRow) {
								for(var k in sourceRow[p]) {
									targetRow[p][k] = sourceRow[p][k];
								}
								targetRow[p]._svyRowId = sourceRow._svyRowId; 
							}	
						}						
					}
				}
			}
			$scope.model[''].updatedRows = null;
		}
	}, false);
	var wrapper = function (handler, rowId) {
		return function() {
		    var recordHandler = handler.selectRecordHandler(rowId)
			return recordHandler.apply(recordHandler.this, arguments);
		}
	}
	$scope.cellHandler = function(row,columnName, columnHandler) {
		var cellModel = row.getProperty(columnName); 
		var cellHandler = cellModel.svyCellHandler;
		if (!cellHandler) {
			cellHandler = {};
			for(var p in columnHandler) {
				cellHandler[p] = wrapper(columnHandler[p], cellModel.rowId);
			}
			cellModel.svyCellHandler = cellHandler;
		}
		return cellHandler;
	}
	$scope.applyWrapper = function(applyHandler, cellModel) {
		var wrapper = cellModel.svyApplyHandler;
		if (!wrapper) {
			wrapper = function(property) {
				return applyHandler(property,cellModel);
			}
			cellModel.svyApplyHandler = wrapper;
		}
		return wrapper;
	}
	
	$scope.cellApiWrapper = function(apiHandler, cellModel) {
		var cellApi = cellModel.svyApiHandler;
		if (!cellApi) {
			cellApi = {}
			cellModel.svyApiHandler = cellApi;
		}
		return cellApi;
	}
	
	$scope.selections = []; 
	$scope.$watch('selections', function (newVal, oldVal) {
		if (oldVal.length > 0 && (newVal[0]._svyRowId != oldVal[0]._svyRowId)) {
			$servoyInternal.sendRequest({cmd:'datapush',formname:'${name}',changes:{rowId:newVal[0]._svyRowId}});
		}
	}, true);

	$scope.$watch('model..selectedIndex', function (newVal, oldVal) {
		if (newVal !== oldVal) {
		$timeout(function() {
			$scope.grid${controllerName}.selectItem($scope.model[''].selectedIndex, true);
			});
		}
	}, false);
	
	var firstTime = true;
	$scope.$watch('model..totalRows', function (newVal, oldVal) {	
		if (angular.isUndefined(oldVal))
		{
			$scope.grid${controllerName}.$gridScope.pagingOptions.pageSize = Math.ceil(($scope.grid${controllerName}.$gridScope.viewportDimHeight()-2)/(${rowHeight}+1)); //border is 1px
			$timeout(function() {
				$scope.$apply();
			});
		}	
		if (firstTime || newVal !== oldVal)
		{
			firstTime = false;
			 var currentPageSize = $scope.grid${controllerName}.$gridScope.viewportDimHeight() /  $scope.grid${controllerName}.$gridScope.rowHeight;
			 var show =  newVal > currentPageSize;
			 $scope.grid${controllerName}.$gridScope.enablePaging = show;
			 $scope.grid${controllerName}.$gridScope.showFooter = show;
			 if ($scope.grid${controllerName}.$gridScope.config)
			 {
			 	$scope.grid${controllerName}.$gridScope.footerRowHeight = show ? $scope.grid${controllerName}.$gridScope.config.footerRowHeight : 0;
			 }
		}
	}, false);
	
	$scope.columnDefs = new Array(); 
	<#assign i = 0>
	<#list bodyComponents as bc>
		$scope.columnDefs[${i}] = {
			<#if bc.label??>
			headerCellTemplate: '<${bc.label.tagname} headerCell name="${bc.label.name}" svy-model="model.${bc.label.name}" svy-api="api.${bc.label.name}" svy-handlers="handlers.${bc.label.name}" svy-apply="handlers.${bc.label.name}.svy_apply" svy-servoyApi="handlers.${bc.label.name}.svy_servoyApi"/>'
			<#else>
			displayName: '${bc.name}'
			</#if>,
			cellTemplate: '<${bc.tagname} name="${bc.name}" svy-model="cellRender(row, \'${bc.name}\', model.${bc.name})" svy-api="cellApiWrapper(api.${bc.name},row.getProperty(\'${bc.name}\'))" svy-handlers="cellHandler(row,\'${bc.name}\',handlers.${bc.name})" svy-apply="applyWrapper(handlers.${bc.name}.svy_apply,row.getProperty(\'${bc.name}\'))"/>',
			<#if bc.properties.size??>
			width: ${bc.properties.size.width},
			</#if>
			<#if bc.properties.visible??>
			visible: ${bc.properties.visible?c},
			</#if>
		};
		<#if bc.properties.visible??>
			$scope.$watch('model.${bc.name}.visible', function (newVal, oldVal) {
				$scope.columnDefs[${i}].visible = newVal;
			}, false);
		</#if>
		<#assign i = i + 1>
	</#list>

	$scope.grid${controllerName} = {
	data: 'model..rows',
	enableCellSelection: true,
	enableRowSelection: true,
	selectedItems: $scope.selections,
	multiSelect: false,
	enablePaging: false,
	showFooter: false,
	headerRowHeight: ${headerHeight},
	rowHeight: ${rowHeight},
	totalServerItems: 'model..totalRows',
	pagingOptions: $scope.pagingOptions,
	primaryKey: '_svyRowId',
	columnDefs: 'columnDefs'
	};

	if (tmpRows)
	{
		$timeout(function() {
			// first get on the same level as the paint events.
			$timeout(function() {
				if ($scope.model[''].rows.length == 0) {
					// this will be after the paint events.		
		    		$scope.model[''].rows = tmpRows;
		    	}
			},10);
		},10);
	}
</#macro> 
<@form_js/> 