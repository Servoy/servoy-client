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
<#macro form_js_body>Abstract macro</#macro>
<#macro form_js>
${registerMethod}("${controllerName}", function($scope, $servoyInternal,$timeout,$windowService) {

	var beans = {
	<#list baseComponents as bc>
		'${bc.name}': ${bc.propertiesString}<#if bc_has_next>,</#if>
	</#list>
	}

	<#list baseComponents as bc>
		<#list bc.valuelistProperties as vlprop>
	beans['${bc.name}'].${vlprop}_filter = function(filterString) {
		if (filterString) {
			return $servoyInternal.filterList('${name}','${bc.name}','${vlprop}',filterString);
		}
		return $scope.model['${bc.name}'].${vlprop}
	}
	    </#list>
	</#list>

	var formProperties = ${propertiesString}
	var formState = $servoyInternal.initFormState("${name}", beans, formProperties);
	<#if viewType == "RECORD_VIEW">
		// send the special request initial data for this form 
		// this can also make the form (IFormUI instance) on the server if that is not already done
		$servoyInternal.callService('formService', 'initialrequestdata', {formname:'${name}'},true)
	</#if>
	$scope.model = formState.model;
	$scope.api = formState.api;
	$scope.layout = formState.layout;
	$scope.formStyle = formState.style;
	$scope.formProperties = formState.properties;
	$scope.formname = "${name}";

	<#list parts as part>
	$scope.${part.name}Style = ${part.style};
	</#list>
	
	var getExecutor = function(beanName,eventType) {
		var callExecutor = function(args, rowId) {
			return $servoyInternal.getExecutor("${name}").on(beanName,eventType,null,args,rowId);
		}
		var wrapper = function() {
			return callExecutor(arguments, null);
		}
		wrapper.selectRecordHandler = function(rowId) {
			return function () { return callExecutor(arguments, rowId); }
		}
		return wrapper;
	}

	var getApply = function(beanname) {
		var wrapper = function(property, componentModel, rowId) {
			$servoyInternal.pushDPChange("${name}", beanname, property, componentModel, rowId);
		}
		return wrapper;
	}

	var servoyApi = function(beanname) {
		return {
			showForm: function(formname,relationname,formIndex) {
				// first just touch the form here so it doesn't happen later on again.
				$windowService.touchForm(formname)
				$servoyInternal.callService('formService', 'formvisibility', {formname:formname,visible:true,parentForm:$scope.formname,bean:beanname,relation:relationname,formIndex:formIndex}, true);
			},
			hideForm: function(formname,relationname,formIndex) {
				return $servoyInternal.callService('formService', 'formvisibility', {formname:formname,visible:false,parentForm:$scope.formname,bean:beanname,relation:relationname,formIndex:formIndex});
			},
			setFormEnabled: function(formname, enabled) {
				$servoyInternal.callService('formService', 'formenabled', {formname:formname,enabled:enabled},true)
			},
			setFormReadOnly: function(formname, readOnly) {
				$servoyInternal.callService('formService', 'formreadOnly', {formname:formname,readOnly:readOnly},true)
			},
			getFormUrl: function(formUrl) {
				return $windowService.getFormUrl(formUrl);
			}
		}	
	}

	$scope.handlers = {
	<#list baseComponents as bc>
		'${bc.name}': {"svy_apply":getApply('${bc.name}'),"svy_servoyApi":servoyApi('${bc.name}')<#list bc.handlers as handler>,${handler}:getExecutor('${bc.name}', '${handler}')</#list>}<#if bc_has_next>,</#if>
	</#list>
	}
	
	formState.handlers = $scope.handlers;
	
	var wrapper = function(beanName) {
		return function(newvalue,oldvalue) {
				if(oldvalue === newvalue) return;
				$servoyInternal.sendChanges(newvalue,oldvalue, "${name}", beanName);
		}
	}
	
	var watches = {};

	formState.addWatches = function (beanNames) {
		if (beanNames) {
		 	for (var beanName in beanNames) {
			 	watches[beanName] = $scope.$watch("model." + beanName, wrapper(beanName), true);
			}
		}
		else {
		<#list parts as part>
			<#if (part.baseComponents)??>
				<#list part.baseComponents as bc><#-- TODO refine this watch; it doesn't need to go deep into complex properties as those handle their own changes! -->
					watches['${bc.name}'] = $scope.$watch("model.${bc.name}", wrapper('${bc.name}'), true);
				</#list>
			</#if>
		</#list>
		}
	}
	
	formState.removeWatches = function (beanNames) {
		if (Object.getOwnPropertyNames(watches).length == 0) return false;
		
		if (beanNames) {
		 	for (var beanName in beanNames) {
			 	if (watches[beanName]) watches[beanName]();
			}
		}
		else {
			 for (var beanName in watches) {
			 	watches[beanName]();
			 }
		}
		return true;
	}
	
	formState.$digest = function () {
		$scope.$digest();
	}
	
<@form_js_body/> 
});
</#macro>
