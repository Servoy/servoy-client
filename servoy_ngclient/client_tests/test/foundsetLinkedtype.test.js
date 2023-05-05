describe("Test foundset_linked_property suite", function() {

	var sabloConverters;
	var typesRegistry;
	var pushToServerUtils;
	var $scope;
	var iS;

	var propertyContext;

	var serverValue;
	var realClientValue;

	var changeNotified = false;

	function getAndClearNotified() {
		var tm = changeNotified;
		changeNotified = false;
		return tm;
	};

	beforeEach(function() {
        sessionStorage.removeItem('svy_session_lock');
		module('servoy');
		module('foundset_linked_property');
		module('sabloApp');

		inject(function(_$sabloConverters_, _$compile_, _$rootScope_, _$typesRegistry_, _$pushToServerUtils_) {
			var angularEquality = function(first, second) {
				return angular.equals(first, second);
			};

			jasmine.addCustomEqualityTester(angularEquality);
			// The injector unwraps the underscores (_) from around the parameter
			// names when matching
			sabloConverters = _$sabloConverters_;
			typesRegistry = _$typesRegistry_;
			pushToServerUtils = _$pushToServerUtils_;
			iS = sabloConverters.INTERNAL_IMPL;
			$compile = _$compile_;

			$scope = _$rootScope_.$new();
			
            fslinkedType = typesRegistry.getAlreadyRegisteredType('fsLinked');
		});

		// mock timout
//		jasmine.clock().install();
	});

	afterEach(function() {
//		jasmine.clock().uninstall();
	});

	describe("foundsetLinked_property with dumb values and simple values suite; pushToServer not set (so reject)", function() {
		beforeEach(function() {
			var myfoundset = {
					"serverSize": 0,
					"selectedRowIndexes": [],
					"multiSelect": false,
					"viewPort": 
					{
						"startIndex": 0,
						"size": 2,
						"rows": [ { "_svyRowId" : "bla bla" }, { "_svyRowId" : "har har" } ]
					}
			};

			serverValue = { forFoundset: "myfoundset" };

			var template = '<div></div>';
			$compile(template)($scope);
			
            propertyContext = {
                getProperty: function(propertyName) { return ({ myfoundset: myfoundset })[propertyName]; },
                getPushToServerCalculatedValue: function() { return pushToServerUtils.reject; },
                isInsideModel: true
            };
			
			realClientValue = sabloConverters.convertFromServerToClient(serverValue, fslinkedType, undefined, undefined, undefined, $scope, propertyContext);
			realClientValue[iS].setChangeNotifier(function () { changeNotified = true });
			$scope.$digest();

			serverValue = { forFoundset: "myfoundset", sv: ":) --- static string ***" };

			realClientValue = sabloConverters.convertFromServerToClient(serverValue, fslinkedType, realClientValue, undefined, undefined, $scope, propertyContext);
			$scope.$digest();

			expect(getAndClearNotified()).toEqual(false);
			expect(realClientValue[iS].isChanged()).toEqual(false);
			expect(realClientValue).toEqual([ ':) --- static string ***', ':) --- static string ***' ]);
		});


		it("Should not send value updates for when pushToServer is not specified", function() {
			// *** initial size no viewport
			realClientValue[1] = "I am changed but shouldn't be sent"; 
			$scope.$digest();

			expect(getAndClearNotified()).toEqual(false);
			expect(realClientValue[iS].isChanged()).toEqual(false);
		});

	});

	describe("foundsetLinked_property with dumb values and simple values suite; pushToServer set to shallow", function() {
		beforeEach(function() {
			var myfoundset = {
					"serverSize": 0,
					"selectedRowIndexes": [],
					"multiSelect": false,
					"viewPort": 
					{
						"startIndex": 0,
						"size": 2,
						"rows": [ { "_svyRowId" : "bla bla" }, { "_svyRowId" : "har har" } ]
					}
			};

			serverValue = { forFoundset: "myfoundset", w: false };

			var template = '<div></div>';
			$compile(template)($scope);
			
            propertyContext = {
                getProperty: function(propertyName) { return ({ myfoundset: myfoundset })[propertyName]; },
                getPushToServerCalculatedValue: function() { return pushToServerUtils.shallow; },
                isInsideModel: true
            };

			realClientValue = sabloConverters.convertFromServerToClient(serverValue, fslinkedType, undefined, undefined, undefined, $scope, propertyContext);
			realClientValue[iS].setChangeNotifier(function () { changeNotified = true });
			$scope.$digest();

			serverValue = { sv: ":) --- static string ***" };

			realClientValue = sabloConverters.convertFromServerToClient(serverValue, fslinkedType, realClientValue, undefined, undefined, $scope, propertyContext);
			$scope.$digest();

			expect(getAndClearNotified()).toEqual(false);
			expect(realClientValue[iS].isChanged()).toEqual(false);
			expect(realClientValue).toEqual([ ':) --- static string ***', ':) --- static string ***' ]);

		});

		it("Should not send value updates for when pushToServer is not specified", function() {
			// *** initial size no viewport
			realClientValue[0] = "I am really changed and I should be sent"; 
			$scope.$digest();

			expect(getAndClearNotified()).toEqual(true);
			expect(realClientValue[iS].isChanged()).toEqual(true);
			expect(sabloConverters.convertFromClientToServer(realClientValue, fslinkedType, realClientValue, $scope, propertyContext)).toEqual(
					[ { propertyChange: 'I am really changed and I should be sent' } ]
			);

			expect(getAndClearNotified()).toEqual(false);
			expect(realClientValue[iS].isChanged()).toEqual(false);
		});

	});


	describe("foundsetLinked_property with dumb values and foundset linked values suite; pushToServer not set (so reject)", function() {
		beforeEach(function() {
			var myfoundset = {
					"serverSize": 10,
					"selectedRowIndexes": [],
					"multiSelect": false,
					"viewPort": 
					{
						"startIndex": 0,
						"size": 6,
						"rows": [ { "_svyRowId" : "bla bla" },
						          { "_svyRowId" : "har har" },
						          { "_svyRowId" : "bl bl" },
						          { "_svyRowId" : "ha ha" },
						          { "_svyRowId" : "b b" },
						          { "_svyRowId" : "h h" } ]
					}
			};
			
			propertyContext = {
                getProperty: function(propertyName) { return ({ myfoundset: myfoundset })[propertyName]; },
                getPushToServerCalculatedValue: function() { return pushToServerUtils.reject; },
                isInsideModel: true
            };

			serverValue = { forFoundset: "myfoundset" };

			var template = '<div></div>';
			$compile(template)($scope);
			realClientValue = sabloConverters.convertFromServerToClient(serverValue, fslinkedType, undefined, undefined, undefined, $scope, propertyContext);
			realClientValue[iS].setChangeNotifier(function () { changeNotified = true });
			$scope.$digest();

			serverValue = {
					"forFoundset": "myfoundset",
					"vp": [ 10643, 10702, 10835, 10952, 11011, 11081 ]
			};

			realClientValue = sabloConverters.convertFromServerToClient(serverValue, fslinkedType, realClientValue, undefined, undefined, $scope, propertyContext);
			$scope.$digest();

			expect(getAndClearNotified()).toEqual(false);
			expect(realClientValue[iS].isChanged()).toEqual(false);
			expect(realClientValue).toEqual([ 10643, 10702, 10835, 10952, 11011, 11081 ]);
		});


		it("Should not send value updates for when pushToServer is not specified", function() {
			// *** initial size no viewport
			realClientValue[2] = 100001010; 
			$scope.$digest();

			expect(getAndClearNotified()).toEqual(false);
			expect(realClientValue[iS].isChanged()).toEqual(false);
		});

	});

	describe("foundsetLinked_property with dumb values and foundset linked values suite; pushToServer set to shallow", function() {
		beforeEach(function() {
			var myfoundset = {
					"serverSize": 10,
					"selectedRowIndexes": [],
					"multiSelect": false,
					"viewPort": 
					{
						"startIndex": 0,
						"size": 6,
						"rows": [ { "_svyRowId" : "bla bla" },
						          { "_svyRowId" : "har har" },
						          { "_svyRowId" : "bl bl" },
						          { "_svyRowId" : "ha ha" },
						          { "_svyRowId" : "b b" },
						          { "_svyRowId" : "h h" } ]
					}
			};
			
            propertyContext = {
                getProperty: function(propertyName) { return ({ myfoundset: myfoundset })[propertyName]; },
                getPushToServerCalculatedValue: function() { return pushToServerUtils.shallow; },
                isInsideModel: true
            };

			serverValue = { forFoundset: "myfoundset", w: false };

			var template = '<div></div>';
			$compile(template)($scope);
			realClientValue = sabloConverters.convertFromServerToClient(serverValue, fslinkedType, undefined, undefined, undefined, $scope, propertyContext);
			realClientValue[iS].setChangeNotifier(function () { changeNotified = true });
			$scope.$digest();

			serverValue = {
					"forFoundset": "myfoundset",
					"vp": [ 10643, 10702, 10835, 10952, 11011, 11081 ]
			};

			realClientValue = sabloConverters.convertFromServerToClient(serverValue, fslinkedType, realClientValue, undefined, undefined, $scope, propertyContext);
			$scope.$digest();

			expect(getAndClearNotified()).toEqual(false);
			expect(realClientValue[iS].isChanged()).toEqual(false);
			expect(realClientValue).toEqual([ 10643, 10702, 10835, 10952, 11011, 11081 ]);

		});

		it("Should send value updates for when pushToServer >= shallow", function() {
			// *** initial size no viewport
			realClientValue[3] = 1010101010; 
			$scope.$digest();

			expect(getAndClearNotified()).toEqual(true);
			expect(realClientValue[iS].isChanged()).toEqual(true);
			expect(sabloConverters.convertFromClientToServer(realClientValue, fslinkedType, realClientValue, $scope, propertyContext)).toEqual(
					[ { viewportDataChanged: { _svyRowId: 'ha ha', dp: null, value: 1010101010 } } ]
			);

			expect(getAndClearNotified()).toEqual(false);
			expect(realClientValue[iS].isChanged()).toEqual(false);
		});

	});

});
