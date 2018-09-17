describe("Test component_custom_property suite", function() {

	beforeEach(function() {
		module('servoy');
		module('foundset_viewport_module');
		module('component_custom_property');
		module('sabloApp');
		angular.module('pushToServerData', ['pushToServer']);
	});

	var sabloConverters;
	var foundsetTypeConstants;
	var $scope;
	var serverValue;
	var converted;
	var componentModelGetter;

	beforeEach(inject(function(_$sabloConverters_, _$compile_, _$rootScope_, _$foundsetTypeConstants_, $propertyWatchesRegistry){
		// The injector unwraps the underscores (_) from around the parameter
		//names when matching
		sabloConverters = _$sabloConverters_;
		foundsetTypeConstants = _$foundsetTypeConstants_;
		$scope = _$rootScope_.$new();
		$compile = _$compile_;
		serverValue = {
				componentDirectiveName:'component',
				handlers:{
					onActionMethodID:"anyIDisGood"
				},
				model:{
					text:"buitonn",
					location: {x:1,y:2}
				},
				forFoundset : {
					recordBasedProperties: ['dataProviderID']
				}
		};
		serverValue[foundsetTypeConstants.FOR_FOUNDSET_PROPERTY] = "myFoundset";
		var componentModel = {
				myFoundset: { viewPort: { rows: [ {_svyRowId: 123},
				                                  {_svyRowId: 321},
				                                  {_svyRowId: 132},
				                                  {_svyRowId: 231},
				                                  {_svyRowId: 111},
				                                  {_svyRowId: 222},
				                                  {_svyRowId: 333} ] } }
		}
		componentModelGetter = function(prop) { return componentModel[prop]; };

		var template = '<div></div>';
		$compile(template)($scope);
		$propertyWatchesRegistry.setAutoWatchPropertiesList("components",{"component" : { "text" : false, "recordDependentText" : true }});
		converted = sabloConverters.convertFromServerToClient(serverValue,'component', $scope.model, $scope, componentModelGetter);
		$scope.$digest();
	}));

	it("should add requests when we change the model", function() {
		converted.model.text = "button";
		$scope.$digest();
		expect(converted.__internalState.isChanged()).toBe(true);
		expect(converted.__internalState.requests[0].propertyChanges.text).toBe("button");
	});

	it("should add requests when we call a handler", function() {
		expect(converted.__internalState.isChanged()).toBe(false);
		converted.handlers.onActionMethodID.selectRecordHandler(123)({ dog: 'S' });
		expect(converted.__internalState.isChanged()).toBe(true);
		expect(converted.__internalState.requests[0].handlerExec.eventType).toBe('onActionMethodID');
		expect(converted.__internalState.requests[0].handlerExec.rowId).toBe(123);
		expect(converted.__internalState.requests[0].handlerExec.args[0].dog).toBe('S');
	});

	it("should add a startEdit request after startEdit has been called", function() {
		expect(converted.__internalState.isChanged()).toBe(false);
		converted.servoyApi.startEdit('myproperty');
		expect(converted.__internalState.isChanged()).toBe(true);
		expect(converted.__internalState.requests[0].svyStartEdit.pn).toBe('myproperty');
		expect(converted.__internalState.requests[0].svyStartEdit[foundsetTypeConstants.ROW_ID_COL_KEY]).toBe(undefined);
		converted.servoyApi.startEdit('myproperty', 132);
		expect(converted.__internalState.isChanged()).toBe(true);
		expect(converted.__internalState.requests[1].svyStartEdit.pn).toBe('myproperty');
		expect(converted.__internalState.requests[1].svyStartEdit[foundsetTypeConstants.ROW_ID_COL_KEY]).toBe(132);
	});

	it("should add an apply request", function() {
		expect(converted.__internalState.isChanged()).toBe(false);
		converted.servoyApi.apply('text',$scope.model);
		expect(converted.__internalState.isChanged()).toBe(true);
		expect(converted.__internalState.requests[0].svyApply['pn']).toBe('text');
		expect(converted.__internalState.requests[0].svyApply['v']).toBe('buitonn');
		expect(converted.__internalState.requests[0].svyApply[foundsetTypeConstants.ROW_ID_COL_KEY]).toBe(undefined);
		converted.servoyApi.apply('text',$scope.model, 321);
		expect(converted.__internalState.isChanged()).toBe(true);
		expect(converted.__internalState.requests[1].svyApply['pn']).toBe('text');
		expect(converted.__internalState.requests[1].svyApply['v']).toBe('buitonn');
		expect(converted.__internalState.requests[1].svyApply[foundsetTypeConstants.ROW_ID_COL_KEY]).toBe(321);
	});

	it("should handle an incremental update", function() {
		var updateValue = {
				propertyUpdates: {
					model : {
						text: "updatedButtonText"
					}
				}
		};
		var tmp = sabloConverters.convertFromServerToClient(updateValue,'component', serverValue, $scope, componentModelGetter);
		expect(tmp).toBe(serverValue);
		expect(serverValue.model.text).toBe('updatedButtonText');
	});

	it("should handle viewport full updates, incremental updates and incremental client changes", function() {
		// full viewport update
		var updateValue = {
				propertyUpdates: {
					model_vp : [
					            { dataProviderID1:'book1', dataProviderID2: 1141240331660, recordDependentText: 'aha1' },
					            { dataProviderID1:'book2', dataProviderID2: 1141240331661, recordDependentText: 'aha2' },
					            { dataProviderID1:'book3', dataProviderID2: 1141240331662, recordDependentText: 'aha3' },
					            { dataProviderID1:'book4', dataProviderID2: 1141240331663, recordDependentText: 'aha4' }
					            ],
					svy_types : {
						model_vp: {
							"0": { dataProviderID2: 'Date' },
							"1": { dataProviderID2: 'Date' },
							"2": { dataProviderID2: 'Date' },
							"3": { dataProviderID2: 'Date' }
						}
					}
				}
		};
		var tmp = sabloConverters.convertFromServerToClient(updateValue,'component', converted, $scope, componentModelGetter);
		expect(tmp).toBe(converted);
		expect(converted.modelViewport[2].dataProviderID1).toBe('book3');
		expect(typeof converted.modelViewport[2].dataProviderID2).toBe('object');
		expect(converted.modelViewport[2].dataProviderID2.getTime()).toBe(1141240331662);

		// simulate 2 client changes of one cell each in the viewport and check that correct things will be sent to browser
		$scope.$digest();
		expect(converted.__internalState.isChanged()).toBe(false);
		
		converted.modelViewport[0].recordDependentText = 'modified aha1'; // watched property
		converted.modelViewport[3].dataProviderID2 = new Date(11412403316761); // not watched property
		
		expect(converted.__internalState.isChanged()).toBe(false);
		$scope.$digest();
		expect(converted.__internalState.isChanged()).toBe(true);
		
		var result = sabloConverters.convertFromClientToServer(converted, 'component', undefined);
		
		expect(result.length).toEqual(1);
		expect(result[0]).toEqual({ viewportDataChanged: { _svyRowId: 123, dp: 'recordDependentText', value: 'modified aha1' } });
		expect(converted.__internalState.isChanged()).toBe(false);

		// now for incremental changes:
		var CHANGE = 0;
		var INSERT = 1;
		var DELETE = 2;

		// incremental CHANGE from server (2 full rows and one cell in another row)
		updateValue = {
				propertyUpdates: {
					model_vp_ch : [ {
						rows: [ {dataProviderID1:'book3 Modified', dataProviderID2: 1141240331669, recordDependentText:'modified aha3'} ],
						startIndex: 2,
						endIndex: 2,
						type: CHANGE
					}, {
						rows: [ {dataProviderID1:'book1 Modified', dataProviderID2: 1141240331668} ],
						startIndex: 0,
						endIndex: 0,
						type: CHANGE
					}, {
						rows: [ { dataProviderID2: 1141240331667 } ],
						startIndex: 1,
						endIndex: 1,
						type: CHANGE
					} ],
					svy_types : {
						model_vp_ch: {
							"0": { rows: { "0" : { dataProviderID2: 'Date' } } },
							"1": { rows: { "0" : { dataProviderID2: 'Date' } } },
							"2": { rows: { "0" : { dataProviderID2: 'Date' } } },
						}
					}
				}
		};
		
		var tmp = sabloConverters.convertFromServerToClient(updateValue,'component', converted, $scope, componentModelGetter);
		expect(tmp).toBe(converted);
		expect(converted.modelViewport[2].dataProviderID1).toBe('book3 Modified');
		expect(typeof converted.modelViewport[2].dataProviderID2).toBe('object');
		expect(converted.modelViewport[2].dataProviderID2.getTime()).toBe(1141240331669);
		expect(converted.modelViewport[0].dataProviderID1).toBe('book1 Modified');
		expect(typeof converted.modelViewport[0].dataProviderID2).toBe('object');
		expect(converted.modelViewport[0].dataProviderID2.getTime()).toBe(1141240331668);
		expect(converted.modelViewport[1].dataProviderID1).toBe('book2');
		expect(typeof converted.modelViewport[1].dataProviderID2).toBe('object');
		expect(converted.modelViewport[1].dataProviderID2.getTime()).toBe(1141240331667);
		
		// incremental INSERT from server (4 full rows: 2 by 1 and 1 by 2) and DELETE (remove one previously existing row)
		updateValue = {
				propertyUpdates: {
					
					model_vp_ch : [ {
						rows: [ {dataProviderID1:'book2.1 inserted', dataProviderID2: 1141240331670, recordDependentText:'aha2.1'} ],
						startIndex: 2,
						endIndex: 5, // actually this means 'new viewport size' for INSERTS
						type: INSERT
					}, {
						rows: [ {dataProviderID1:'book5 inserted', dataProviderID2: 1141240331671, recordDependentText:'aha5'} ],
						startIndex: 4,
						endIndex: 6, // actually this means 'new viewport size' for INSERTS
						type: INSERT
					}, {
						rows: [ {dataProviderID1:'book0.1 inserted', dataProviderID2: 1141240331672, recordDependentText:'aha0.1'},
						        {dataProviderID1:'book0.2 inserted', dataProviderID2: 1141240331673, recordDependentText:'aha0.2'} ],
						startIndex: 0,
						endIndex: 8, // actually this means 'new viewport size' for INSERTS
						type: INSERT
					}, {
						rows: [ {dataProviderID1:'book 6 replacing 1 and 2 inserted', dataProviderID2: 1141240331674, recordDependentText:'aha6'} ],
						startIndex: 2,
						endIndex: 3, // so we delete the initial rows 'book1' and 'book2', and they will get replaced with 1 new row
						type: DELETE
					} ],
					
					svy_types : {
						model_vp_ch: {
							"0": { rows: { "0" : { dataProviderID2: 'Date' } } },
							"1": { rows: { "0" : { dataProviderID2: 'Date' } } },
							"2": { rows: { "0" : { dataProviderID2: 'Date' }, "1" : { dataProviderID2: 'Date' } } },
							"3": { rows: { "0" : { dataProviderID2: 'Date' } } }
						}
					}
				}
		};
		
		var tmp = sabloConverters.convertFromServerToClient(updateValue, 'component', converted, $scope, componentModelGetter);
		
		expect(tmp).toBe(converted);
		expect(converted.modelViewport.length).toBe(7);

		expect(converted.modelViewport[0].dataProviderID1).toBe('book0.1 inserted');
		expect(typeof converted.modelViewport[0].dataProviderID2).toBe('object');
		expect(converted.modelViewport[0].dataProviderID2.getTime()).toBe(1141240331672);
		
		expect(converted.modelViewport[1].dataProviderID1).toBe('book0.2 inserted');
		expect(typeof converted.modelViewport[1].dataProviderID2).toBe('object');
		expect(converted.modelViewport[1].dataProviderID2.getTime()).toBe(1141240331673);

		expect(converted.modelViewport[1].recordDependentText).toBe('aha0.2');
		expect(typeof converted.modelViewport[1].recordDependentText).toBe('string');

		expect(converted.modelViewport[2].dataProviderID1).toBe('book2.1 inserted');
		expect(typeof converted.modelViewport[2].dataProviderID2).toBe('object');
		expect(converted.modelViewport[2].dataProviderID2.getTime()).toBe(1141240331670);

		expect(converted.modelViewport[3].dataProviderID1).toBe('book3 Modified');
		expect(typeof converted.modelViewport[3].dataProviderID2).toBe('object');
		expect(converted.modelViewport[3].dataProviderID2.getTime()).toBe(1141240331669);
		
		expect(converted.modelViewport[3].recordDependentText).toBe('modified aha3');
		expect(typeof converted.modelViewport[3].recordDependentText).toBe('string');

		expect(converted.modelViewport[4].dataProviderID1).toBe('book5 inserted');
		expect(typeof converted.modelViewport[4].dataProviderID2).toBe('object');
		expect(converted.modelViewport[4].dataProviderID2.getTime()).toBe(1141240331671);

		expect(converted.modelViewport[5].dataProviderID1).toBe('book4');
		expect(typeof converted.modelViewport[5].dataProviderID2).toBe('object');
		expect(converted.modelViewport[5].dataProviderID2.getTime()).toBe(11412403316761);

		expect(converted.modelViewport[6].dataProviderID1).toBe('book 6 replacing 1 and 2 inserted');
		expect(typeof converted.modelViewport[6].dataProviderID2).toBe('object');
		expect(converted.modelViewport[6].dataProviderID2.getTime()).toBe(1141240331674);
		
		// simulate 2 more client changes of one cell each in the viewport and check that correct things will be sent to browser
		// this is in order to test that watches are still registered correctly after granular update/insert/delete
		$scope.$digest();
		expect(converted.__internalState.isChanged()).toBe(false);

		converted.modelViewport[5].recordDependentText = 'client modified aha4';
		converted.modelViewport[2].dataProviderID2 = new Date(11412403316760);
		
		expect(converted.__internalState.isChanged()).toBe(false);
		$scope.$digest();
		expect(converted.__internalState.isChanged()).toBe(true);
		
		var result = sabloConverters.convertFromClientToServer(converted, 'component', undefined);
		expect(result.length).toEqual(1);

		expect(result[0]).toEqual({ viewportDataChanged: { _svyRowId: 222, dp: 'recordDependentText', value: 'client modified aha4' } });
		expect(converted.__internalState.isChanged()).toBe(false);
	});

	it("should handle an initial data set", function() {
		var updateValue = {
				propertyUpdates: {
					model_vp : [
					            {dataProviderID:'book1'},
					            {dataProviderID:'book2'}
					            ]
				}
		};
		var converted = sabloConverters.convertFromServerToClient(updateValue, 'component', serverValue, $scope, componentModelGetter);

		expect(converted.modelViewport[1].dataProviderID).toBe('book2');

		var updateValue = {
				propertyUpdates: {
					model : {
						text: "updatedButtonText"
					}
				}
		};

		converted = sabloConverters.convertFromServerToClient(updateValue,'component', converted, $scope, componentModelGetter);
		expect(converted.model.text).toBe('updatedButtonText');

	});

	it("should handle an initial data with conversions ", function() {
		var updateValue = {
				propertyUpdates: {
					svy_types : {
						model_vp : [
						            {
						            	dataProviderID : 'Date'
						            },
						            {
						            	dataProviderID : 'Date'
						            }
						            ]
					},
					model : {
						format: {
							display:"M/d/yy h:mm a",
							type:"DATETIME"
						}
					},
					model_vp : [
					            {dataProviderID:1141240331660},
					            {dataProviderID:1141240331661}
					            ]
				}
		};
		var converted = sabloConverters.convertFromServerToClient(updateValue,'component', serverValue, $scope, componentModelGetter);
		expect(converted.modelViewport[0].dataProviderID.getTime()).toBe(1141240331660);
	});

	it("should send back nothing if update is falsy", function() {
		var empty = sabloConverters.convertFromClientToServer(undefined,'component', undefined);
		expect(empty).toEqual([]);
	});

	it("should send back update if update contains something", function() {
		converted.model.text = "button";
		$scope.$digest();
		var result = sabloConverters.convertFromClientToServer(converted,'component', undefined);
		expect(result.length).toEqual(1);
		
		expect(result[0]).toEqual({propertyChanges:{text:'button'}});
		expect(converted.__internalState.isChanged()).toBe(false);
	});
});
