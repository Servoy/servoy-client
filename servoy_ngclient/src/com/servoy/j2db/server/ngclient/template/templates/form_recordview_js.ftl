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

${registerMethod}("${name}", function($scope,$servoyInternal,$sabloApplication,$timeout,$formService,$windowService,$log,$propertyWatchUtils,$applicationService,$q,$templateCache,$compile, $uiBlocker) {
	if ($log.debugEnabled) $log.debug("svy * ftl; form '${name}' - scope create: " + $scope.$id);

	var beans = {
			<#list baseComponents as bc>
				'${bc.name}': ${bc.propertiesString}<#if bc_has_next>,</#if>
			</#list>
			}

	var beanTypes = {
			<#list baseComponents as bc>
				'${bc.name}': '${bc.typeName}'<#if bc_has_next>,</#if>
			</#list>
			}
	var executingEvents = [];
	
	var parentSizes = ${containerSizesString}
	var formProperties = ${propertiesString}
	var formState = $servoyInternal.initFormState("${name}", beans, beanTypes, formProperties, $scope, false, parentSizes);
	formState.resolving = true;
	formState.absoluteLayout = formProperties.absoluteLayout[''];
	if ($log.debugEnabled) $log.debug("svy * ftl; resolving = true for form = ${name}");
	if (${hasRuntimeData()} && !formState.model[''] ) {
		formState.model[''] =  ${runtimePropertiesString}
		delete formState.initializing;
    }
	// clear the beans to release some memory.
	beans = {};
	$scope.model = formState.model;
	$scope.api = formState.api;
	$scope.layout = formState.layout;
	$scope.formStyle = formState.style;
	$scope.formProperties = formState.properties;
	$scope.absoluteLayout = formProperties.absoluteLayout[''];
	$scope.formname = "${name}";

	<#list parts as part>
	$scope.${part.name}Style = ${part.style};
	</#list>

	var getExecutor = function(beanName, eventType) {
		var callExecutor = function(args, rowId) {
			if ($scope.model && $scope.model[beanName])
			{
				if($uiBlocker.shouldBlockDuplicateEvents(beanName, $scope.model[beanName], eventType, rowId))
				{
					// reject execution
					console.log("Prevented duplicate  execution of: "+eventType +" on "+beanName);
					return $q.resolve(null);
				}
				var promise = $sabloApplication.getExecutor("${name}").on(beanName,eventType,null,args,rowId);
				promise.finally(function(){
					$uiBlocker.eventExecuted(beanName, $scope.model[beanName], eventType, rowId);
				});
				return promise;
			}
			return $sabloApplication.getExecutor("${name}").on(beanName,eventType,null,args,rowId);
		}
		var wrapper = function() {
			return callExecutor(arguments, null);
		}
		wrapper.selectRecordHandler = function(rowId) {
			return function () { return callExecutor(arguments, rowId); }
		}
		return wrapper;
	}

	var servoyApi = function(beanname) {
		return {
			formWillShow: function(formname,relationname,formIndex) {
				return $formService.formWillShow(formname,true,$scope.formname,beanname,relationname,formIndex);
			},
			hideForm: function(formname,relationname,formIndex,formNameThatWillShow,relationnameThatWillBeShown,formIndexThatWillBeShown) {
				return $formService.hideForm(formname,$scope.formname,beanname,relationname,formIndex,formNameThatWillShow,relationnameThatWillBeShown,formIndexThatWillBeShown);
			},
			getFormUrl: function(formUrl) {
				return $windowService.getFormUrl(formUrl);
			},
			startEdit: function(propertyName) {
				$servoyInternal.pushEditingStarted($scope.formname, beanname, propertyName);
			},
			apply: function(propertyName) {
				$servoyInternal.pushDPChange("${name}", beanname, propertyName);
			},
			callServerSideApi: function(methodName,args) {
				return $servoyInternal.callServerSideApi("${name}", beanname, methodName, args);
			},
			getFormComponentElements: function(propertyName, formComponentValue) {
				return $compile($templateCache.get(formComponentValue.uuid))($scope);
			},
			isInDesigner: function() {
				return false;
			},
			trustAsHtml: function() {
				return $applicationService.trustAsHtml($scope.model ? $scope.model[beanname] : undefined);
			},
			isInAbsoluteLayout: function(){
				return $scope.absoluteLayout;
			}
		}
	}

	$scope.handlers = {
	<#list baseComponents as bc>
		'${bc.name}': {"svy_servoyApi":servoyApi('${bc.name}')<#list bc.handlers as handler>,${handler}:getExecutor('${bc.name}', '${handler}')</#list>}<#if bc_has_next>,</#if>
	</#list>
	}


	var wrapper = function(beanName) {
		return function(newvalue,oldvalue,property) {
				if(oldvalue === newvalue) return;
				$servoyInternal.sendChanges(newvalue,oldvalue, "${name}", beanName,property);
		}
	}

	var watches = {};

	formState.handlers = $scope.handlers;

	formState.addWatches = function (beanNames) {
	    // always first remove the existing watches if there are any.
		formState.removeWatches(beanNames);
		if (beanNames) {
		 	for (var beanName in beanNames) {
		 		watches[beanName] =	$propertyWatchUtils.watchDumbPropertiesForComponent($scope, beanTypes[beanName], $scope.model[beanName], wrapper(beanName));
			}
		}
		else {
		<#list baseComponents as bc>
			watches['${bc.name}'] = $propertyWatchUtils.watchDumbPropertiesForComponent($scope, beanTypes['${bc.name}'], $scope.model['${bc.name}'], wrapper('${bc.name}'));
		</#list>
		}
	}

	formState.removeWatches = function (beanNames) {
		if (Object.getOwnPropertyNames(watches).length == 0) return false;
		if (beanNames) {
		 	for (var beanName in beanNames) {
				if (watches[beanName]) for (unW in watches[beanName]) watches[beanName][unW]();
			}
		} else {
			for (var beanName in watches) {
				for (unW in watches[beanName]) watches[beanName][unW]();
			}
		}
		return true;
	}

	formState.getScope = function() { return $scope; }

	formState.addWatches();
	
	var formStateWrapper = wrapper('');
	$scope.$watch("formProperties", function(newValue, oldValue) {
		formStateWrapper(newValue, oldValue, undefined);
	}, true);
	
	function getContainer(containername) {
		var query = $("div.svy-layoutcontainer[svy-name='" + containername + "']");
		if (query.length == 0) return null;
		if (query.length == 1) return query;
		var parents = query.closest("div.svy-form");
		
		for(var i = 0;i<parents.length;i++) {
			if ($(parents[i]).filter("[ng-controller='${name}']").length != 0) return $(query[i]); 
		}
		
	}
	$scope.$watch("model[''].containers", function(newValue, oldValue) {
		if (!newValue) return;
		if (newValue.removed) {
		  for (var containername in newValue.removed) {
                var container = getContainer( containername);
                if (container) {
                    var classArray = newValue.removed[containername];
                    classArray.forEach(function (classname) { container.removeClass(classname); });
                }
            }
		}
		if (oldValue && oldValue.removed) {
			 for (var containername in oldValue.removed) {
                var container = getContainer( containername);
                if (container) {
				 	var classesToAddBackIn = oldValue.removed[containername];
				 	if (newValue.removed[containername]) {
				 		var stillToRemove = newValue.removed[containername];
				 		classesToAddBackIn = classesToAddBackIn.filter(function(value) {return stillToRemove.indexOf(value) == -1});
				 	}
				 	classesToAddBackIn.forEach(function (classname) { container.addClass(classname); });
			 	}
			 }
		}
		if (newValue.added) {
		  for (var containername in newValue.added) {
                var container = getContainer( containername);
                if (container) {
                    var classArray = newValue.added[containername];
                    classArray.forEach(function (classname) { container.addClass(classname); });
                }
            }
		}
		if (oldValue && oldValue.added) {
			 for (var containername in oldValue.added) {
                var container = getContainer( containername);
                if (container) {
				 	var classesToRemove = oldValue.added[containername];
				 	if (newValue.added[containername]) {
				 		var stillToAdd = newValue.added[containername];
				 		classesToRemove = classesToRemove.filter(function(value) {return stillToAdd.indexOf(value) == -1});
				 	}
				 	classesToRemove.forEach(function (classname) { container.removeClass(classname); });
				}
			 }
		}
 	}, true);

	$scope.$watch("model[''].cssstyles", function(newValue, oldValue) {
		if (!newValue) return;
		for (var containername in newValue) {
	        var container = getContainer( containername);
	        if (container) {
	           var stylesMap = newValue[containername];
	           for (var key in stylesMap)
		       {
		          container.css(key,stylesMap[key]);
		       }
	        }
	    }
	}, true);
	
	var destroyListenerUnreg = $scope.$on("$destroy", function() {
		if ($log.debugEnabled) $log.debug("svy * ftl; form '${name}' - scope destroyed: " + $scope.$id);
		destroyListenerUnreg();
		$sabloApplication.updateScopeForState("${name}", null);
		if (formState && formState.removeWatches) {
			if (!$scope.hiddenDivFormDiscarded) {
				formState.removeWatches();
				delete formState.removeWatches;
				delete formState.getScope;
				delete formState.addWatches;
				delete formState.handlers;
				for(var key in formState.api) {
					formState.api[key] = {};
				}
				$sabloApplication.unResolveFormState("${name}"); // this also clears "resolving" not just "resolved"
				if ($log.debugEnabled) $log.debug("svy * ftl; form controller scope destroy is letting server know that form is now UNresolved: '${name}'");
				$sabloApplication.callService('formService', 'formUnloaded', { formname: "${name}" }, true);				
			} else if ($log.debugEnabled) $log.debug("svy * ftl; hidden div form controller scope destroy will not clear much of formState as hiddenDiv load was interrupted by a real location load and gave prio to that real location: '${name}'");
			
			delete $scope.hiddenDivFormDiscarded;
			formState = null;
		}
		else $log.info("svy * ftl; form '${name}' - scope destroy cannot find a formState with watches to clean: " + formState + (formState ? ", " + formState.removeWatches : ""));
	});
	
	
	<#list templates?keys as prop>
   		$templateCache.put('${prop}',"${templates[prop]}"); 
	</#list> 
});
