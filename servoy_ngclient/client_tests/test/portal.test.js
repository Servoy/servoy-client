describe("Test portal suite", function() {
    beforeEach(function(){sessionStorage.removeItem('svy_session_lock')});
	beforeEach(module('servoy'));
	beforeEach(module('servoydefaultButton'));
	
	beforeEach(module('foundset_viewport_module'));
	beforeEach(module('foundset_custom_property'));
	beforeEach(module('component_custom_property'));
	beforeEach(module('webSocketModule'));

	beforeEach(module('custom_json_array_property'));
	beforeEach(module('sabloApp'));
	beforeEach(module('ui.grid'));
	beforeEach(module('ui.grid.selection'));
	beforeEach(module('ui.grid.moveColumns'));
	beforeEach(module('ui.grid.resizeColumns'));
	beforeEach(module('ui.grid.infiniteScroll'));
	beforeEach(module('ui.grid.cellNav'));

	beforeEach(module('servoycorePortal'));

    var sabloConverters;
    var typesRegistry;
    var sabloApplication;
	var propertyContext;
	var scope;
	var portalScope;
	var element;
	var serverValue;
	var changed;
	var beanDynamicTypesHolder = {};

	beforeEach(inject(function(_$sabloConverters_, _$compile_, _$rootScope_, $timeout, $httpBackend , _$typesRegistry_, _$sabloApplication_) {
		// The injector unwraps the underscores (_) from around the parameter
		//names when matching
		sabloConverters = _$sabloConverters_;
		typesRegistry = _$typesRegistry_;
		sabloApplication = _$sabloApplication_;
		scope = _$rootScope_.$new();
		scope.model = {};
		$compile = _$compile_;
		serverValue = {
			location: {
				x: 1,
				y: 1
			},
			
			size: {
				height: 200,
				width: 201
			},

			childElements: {
				v: [{
					componentDirectiveName: "servoydefault-button",
					forFoundset: "relatedFoundset",
					foundsetConfig: {
						recordBasedProperties: ['dataProviderID']
					},
					model: {
						
						location: {
							x: 1,
							y: 1
						},
						size: {
							height: 100,
							width: 100
						},
					},
					model_vp : [
						{"dataProviderID" : 1},
						{"dataProviderID" : 2},
						{"dataProviderID" : 3}
					],
					name: "svy_1"
				}, {
					componentDirectiveName: "servoydefault-button",
					forFoundset: "relatedFoundset",
					foundsetConfig: {
						recordBasedProperties: ['dataProviderID']
					},
					model: {
						location: {
							x: 2,
							y: 2
						},
						size: {
							height: 1,
							width: 110
						},
					},
					model_vp : [
						{"dataProviderID" : 4},
						{"dataProviderID" : 5},
						{"dataProviderID" : 6}
					],
					name: "svy_2"
				}]
			},
			relatedFoundset: {
                serverSize: 8,
				selectedRowIndexes: [],
				serverSize: 3,
				viewPort: {
                    startIndex: 2,
                    size: 3,
					rows: [{
						"_svyRowId": "5.10248;2.11;_0"
					}, {
						"_svyRowId": "5.10248;2.42;_1"
					}, {
						"_svyRowId": "5.10248;2.72;_2"
					}]
				}
			}
		};

		var template = "<data-servoycore-portal svy-model='model' svy-api='api' svy-handlers='handlers' svy-servoyApi='svy_servoyApi'></data-servoycore-portal>";
		
		element = $compile(template)(scope);
        typesRegistry.addComponentClientSideSpecs({
            "data-servoycore-portal": {
                p: {
                    relatedFoundset: { t: "foundset", s: 1 },
                    childElements: { t: ["JSON_arr", "component"], s: 1 }
                }
            }
        });
		
		sabloApplication.applyBeanData(scope.model,
		      serverValue,
		      undefined, // container/form size not used in sablo
		      function() { changed = true; }, 
		      "data-servoycore-portal",
		      beanDynamicTypesHolder,
		      scope);

		scope.api = {};
		scope.handlers = {};
		scope.svy_servoyApi = {
				formWillShow: function(formname,relationname,formIndex) {
				},
				hideForm: function(formname,relationname,formIndex) {
				},
				getFormUrl: function (formId) {
				},
				apply: function(propertyName) {
				},
				startEdit: function(propertyName) {
				}
			};
		
		scope.$apply();
		$timeout.flush();
		// $httpBackend.flush();
		portalScope = scope.$$childTail;

		spyOn(portalScope, 'getMergedCellModel').and.callThrough();
		spyOn(portalScope, 'cellApiWrapper').and.callThrough();
		spyOn(portalScope, 'cellServoyApiWrapper').and.callThrough();
		spyOn(portalScope, 'cellHandlerWrapper').and.callThrough();

	}));

	it("should add back apis if childElements are changed", function() {
		scope.model.relatedFoundset.viewPort.rows[0] = {
			"one": 1
		};
		scope.$apply();
		var portalScope = scope.$$childHead;
		expect(portalScope.foundset.viewPort.rows[0]["one"]).toBe(1);
	});


	it("should update the foundset if the relatedFoundset is changed", function() {
		portalScope = scope.$$childHead;

		var newServerValueForChildElement = {
				v: [{
					forFoundset: "relatedFoundset",
					foundsetConfig: {
						recordBasedProperties: []
					},
					model: {
						location: {
							x: 2,
							y: 2
						},
						size: {
							height: 1,
							width: 110
						},
					},
					name: "svy_2"
				}]
		};
        
		var newConvertedComp = sabloConverters.convertFromServerToClient(newServerValueForChildElement, typesRegistry.getComponentSpecification("data-servoycore-portal").getPropertyType("childElements"), undefined, undefined, scope, propertyContext);
		scope.model.relatedFoundset.viewPort.rows[0] = {
			"one": 1,
			"_svyRowId": "5.11248;2.11;_0"
		};
		scope.$apply();

		scope.model.childElements.push(newConvertedComp[0]);

		scope.$apply();
		expect(portalScope.model.childElements[1].name).toBe("svy_2");
		expect(portalScope.model.childElements[1].api).toBeDefined();
	});

	it("should have column definitions for normal portal", function() {
		var portalScope = scope.$$childHead;
		expect(portalScope.model.multiLine).not.toBeDefined();
		expect(portalScope.columnDefinitions[0].cellTemplate).toBe('<servoydefault-button name="svy_1" svy-model="grid.appScope.getMergedCellModel(row, 0, rowRenderIndex, rowElementHelper)" svy-api="grid.appScope.cellApiWrapper(row, 0, rowRenderIndex, rowElementHelper)" svy-handlers="grid.appScope.cellHandlerWrapper(row, 0)" svy-servoyApi="grid.appScope.cellServoyApiWrapper(row, 0)"/>');
	});

	it("should have column definitions for multiline portal", function() {

		scope.model.multiLine = true;
		var newtemplate = "<data-servoycore-portal svy-model='model' svy-api='api' svy-handlers='handlers' svy-servoyApi='svy_servoyApi'></data-servoycore-portal>";
		$compile(newtemplate)(scope); //need to compile it again so that angular calls link and creates a multiline portal
		scope.$apply();
		portalScope = scope.$$childTail;
		expect(portalScope.columnDefinitions[0].cellTemplate).toBe('<div ng-class=\'"svy-listviewwrapper"\' ng-style="grid.appScope.getMultilineComponentWrapperStyle(0)" ><servoydefault-button name="svy_1" svy-model="grid.appScope.getMergedCellModel(row, 0, rowRenderIndex, rowElementHelper)" svy-api="grid.appScope.cellApiWrapper(row, 0, rowRenderIndex, rowElementHelper)" svy-handlers="grid.appScope.cellHandlerWrapper(row, 0)" svy-servoyApi="grid.appScope.cellServoyApiWrapper(row, 0)"/></div><div ng-class=\'"svy-listviewwrapper"\' ng-style="grid.appScope.getMultilineComponentWrapperStyle(1)" ><servoydefault-button name="svy_2" svy-model="grid.appScope.getMergedCellModel(row, 1, rowRenderIndex, rowElementHelper)" svy-api="grid.appScope.cellApiWrapper(row, 1, rowRenderIndex, rowElementHelper)" svy-handlers="grid.appScope.cellHandlerWrapper(row, 1)" svy-servoyApi="grid.appScope.cellServoyApiWrapper(row, 1)"/></div>');
		expect(portalScope.columnDefinitions[0].width).toBe(111);

	});

	it("should call appScope functions", inject(function($timeout) {
		scope.$apply();
		expect(portalScope.getMergedCellModel).toHaveBeenCalled();
		expect(portalScope.cellHandlerWrapper).toHaveBeenCalled();
		expect(portalScope.cellApiWrapper).toHaveBeenCalled();
	}));


	it("should call appScope functions", inject(function($timeout) {
		scope.$apply();
		expect(element.find( ".ng-binding" )[3].innerHTML).toBe('1');
		expect(element.find( ".ng-binding" )[4].innerHTML).toBe('4');
		expect(element.find( ".ng-binding" )[5].innerHTML).toBe('2');
		expect(element.find( ".ng-binding" )[6].innerHTML).toBe('5');
		expect(element.find( ".ng-binding" )[7].innerHTML).toBe('3');
		expect(element.find( ".ng-binding" )[8].innerHTML).toBe('6');
	}));

});