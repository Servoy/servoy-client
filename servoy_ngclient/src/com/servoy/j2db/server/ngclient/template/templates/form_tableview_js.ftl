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
	
	var tmpModel;
	if ($scope.model['']) {
  		tmpModel = $scope.model[''];
  		$scope.model[''] = [];
	}
	$scope.cellRender = function(row, columnName, columnModel) { 
		var cellModel = row.getProperty(columnName);
		if (!cellModel.svyInit) {
			function CellData() {
			}
			CellData.prototype = columnModel;
			cd = new CellData();
			cd.svy_pk = row.getProperty("_svy_pk");
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
	 
	$scope.model[''].currentPage = 1;
	$scope.$watch('model..currentPage', function (newVal, oldVal) {
		if (newVal !== oldVal) {
		$timeout(function() {
			$scope.pagingOptions.currentPage = newVal;
			});
		}
	}, false);
	
	$scope.pagingOptions = { pageSizes: [${pageSize}], pageSize: ${pageSize}, currentPage: 1};
	$scope.$watch('pagingOptions', function (newVal, oldVal) {
		if (newVal !== oldVal && newVal.currentPage !== $scope.model[''].currentPage) {
			$servoy.sendRequest(JSON.stringify({cmd:'requestdata',formname:'${name}',currentPage:newVal.currentPage}));
		}
	}, true);
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
					$scope.model[''].rows = rows;;
				}
				else if (item.type == 2) {
					// delete, first make copy by concatting the new rows;
					rows = rows.concat(updatedRows);
					rows.splice(currentRow,item.endIndex-currentRow);
					$scope.model[''].rows = rows;
				}
				else { // change
					for (var i=0;i<updatedRows.length;i++) {
						var sourceRow = updatedRows[i];
						var targetRow = rows[currentRow++];
						for(var p in sourceRow) {
							for(var k in sourceRow[p]) {
								targetRow[p][k] = sourceRow[p][k];
							}
							targetRow[p].svy_pk = sourceRow._svy_pk; 
						}
					}
				}
			}
			$scope.model[''].updatedRows = null;
		}
	}, false);
	var wrapper = function (handler, svy_pk) {
		return function() {
		    var recordHandler = handler.selectRecordHandler(svy_pk)
			return recordHandler.apply(recordHandler.this, arguments);
		}
	}
	$scope.cellHandler = function(row,columnName, columnHandler) {
		var cellModel = row.getProperty(columnName); 
		var cellHandler = cellModel.svyCellHandler;
		if (!cellHandler) {
			cellHandler = {};
			for(var p in columnHandler) {
				cellHandler[p] = wrapper(columnHandler[p], cellModel.svy_pk);
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
		if (oldVal.length > 0 && (newVal[0]._svy_pk != oldVal[0]._svy_pk)) {
			$servoy.sendRequest(JSON.stringify({cmd:'datapush',formname:'${name}',changes:{svy_pk:newVal[0]._svy_pk}}));
		}
	}, true);

	$scope.$watch('model..selectedIndex', function (newVal, oldVal) {
		if (newVal !== oldVal) {
		$timeout(function() {
			$scope.grid${controllerName}.selectItem($scope.model[''].selectedIndex, true);
			});
		}
	}, false);

	$scope.grid${controllerName} = {
	data: 'model..rows',
	enableCellSelection: true,
	enableRowSelection: true,
	selectedItems: $scope.selections,
	multiSelect: false,
	enablePaging: true,
	showFooter: true,
	headerRowHeight: ${headerHeight},
	totalServerItems: 'model..totalRows',
	pagingOptions: $scope.pagingOptions,
	primaryKey: '_svy_pk',
	columnDefs: [
	<#list bodyComponents as bc>
		{
			<#if bc.label??>
			headerCellTemplate: '<${bc.label.tagname} headerCell name="${bc.label.name}" svy-model="model.${bc.label.name}" svy-api="api.${bc.label.name}" svy-handlers="handlers.${bc.label.name}" svy-apply="handlers.${bc.label.name}.svy_apply" svy-servoyApi="handlers.${bc.label.name}.svy_servoyApi"/>'
			<#else>
			displayName: '${bc.name}'
			</#if>,
			cellTemplate: '<${bc.tagname} name="${bc.name}" svy-model="cellRender(row, \'${bc.name}\', model.${bc.name})" svy-api="cellApiWrapper(api.${bc.name},row.getProperty(\'${bc.name}\'))" svy-handlers="cellHandler(row,\'${bc.name}\',handlers.${bc.name})" svy-apply="applyWrapper(handlers.${bc.name}.svy_apply,row.getProperty(\'${bc.name}\'))"/>'
		}<#if bc_has_next>,</#if>
	</#list>
		]
	};
	if (tmpModel)
	{
		$timeout(function() {
			// first get on the same level as the paint events.
			$timeout(function() {
				// this will be after the paint events.		
		    	$scope.model[''] = tmpModel;
			},10);
		},10);
	}

</#macro> 
<@form_js/> 