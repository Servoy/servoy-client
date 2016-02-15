describe("Test array_custom_property suite", function() {

	var sabloConverters;
	var $scope;
	var $rootScope;
	var iS;

	var componentModelGetter; // can be used if needed; now undefined

	var serverValue;
	var realClientValue;

	var changeNotified = false;

	function getAndClearNotified() {
		var tm = changeNotified;
		changeNotified = false;
		return tm;
	};

	beforeEach(function() {
		module('servoy');
		module('custom_json_array_property');
		module('sabloApp');

		angular.module('pushToServerData', ['pushToServer']);

		inject(function(_$sabloConverters_, _$compile_, _$rootScope_) {
			var angularEquality = function(first, second) {
				return angular.equals(first, second);
			};

			jasmine.addCustomEqualityTester(angularEquality);
			// The injector unwraps the underscores (_) from around the parameter
			// names when matching
			sabloConverters = _$sabloConverters_;
			iS = sabloConverters.INTERNAL_IMPL;
			$compile = _$compile_;

			if (!$scope) $scope = _$rootScope_.$new();
			$rootScope = _$rootScope_;
		});

		// mock timout
//		jasmine.clock().install();
	});

	afterEach(function() {
//		jasmine.clock().uninstall();
	});

	describe("array_custom_property with dumb values suite; pushToServer not set (so reject)", function() {
		beforeEach(function() {
			serverValue = {
					"vEr": 1,
					"v": [ 1, 2, 3, 4 ]
			};

			var template = '<div></div>';
			$compile(template)($scope);
			realClientValue = sabloConverters.convertFromServerToClient(serverValue,'JSON_arr', undefined, $scope, componentModelGetter);
			realClientValue[iS].setChangeNotifier(function () { changeNotified = true });
			$scope.$digest();
		});


		it("Should not send value updates for when pushToServer is not specified", function() {
			// *** initial size no viewport
			realClientValue[2] = 100; 
			$scope.$digest();

			expect(getAndClearNotified()).toEqual(false);
			expect(realClientValue[iS].isChanged()).toEqual(false);
		});

	});

	describe("array_custom_property with dumb values suite; pushToServer set to shallow", function() {
		beforeEach(function() {
			serverValue = {
					"vEr": 1,
					"w": false,
					"v": [ 1, 2, 3, 4 ]
			};

			var template = '<div></div>';
			$compile(template)($scope);
			realClientValue = sabloConverters.convertFromServerToClient(serverValue,'JSON_arr', undefined, $scope, componentModelGetter);
			realClientValue[iS].setChangeNotifier(function () { changeNotified = true });
			$scope.$digest();
		});

		it("Should not send value updates for when pushToServer is not specified", function() {
			// *** initial size no viewport
			realClientValue[2] = 100; 
			$rootScope.$digest();

			expect(getAndClearNotified()).toEqual(true);
			expect(realClientValue[iS].isChanged()).toEqual(true);
			expect(sabloConverters.convertFromClientToServer(realClientValue, 'JSON_arr', realClientValue)).toEqual(
					{ vEr: 1, u: [ { i: '2', v: 100 } ] }
			);

			expect(getAndClearNotified()).toEqual(false);
			expect(realClientValue[iS].isChanged()).toEqual(false);
		});

	});

//	describe("array_custom_property with smart values suite; pushToServer not set (so reject)", function() {
//	beforeEach(function() {
//	serverValue = {
//	"vEr": 1,
//	"v": []/*?*/,
//	"conversions": 
//	{
//	"0": "component",
//	}
//	};

//	var template = '<div></div>';
//	$compile(template)($scope);
//	realClientValue = sabloConverters.convertFromServerToClient(serverValue,'JSON_arr', undefined, $scope, componentModelGetter);
//	realClientValue[iS].setChangeNotifier(function () { changeNotified = true });
//	$scope.$digest();
//	});


//	it("...", function() {
//	});
//	});

//	describe("array_custom_property with smart values suite; pushToServer set to shallow", function() {
//	beforeEach(function() {
//	serverValue = {
//	"vEr": 1,
//	"v": []/*?*/,
//	"conversions": 
//	{
//	"0": "component",
//	}
//	};

//	var template = '<div></div>';
//	$compile(template)($scope);
//	realClientValue = sabloConverters.convertFromServerToClient(serverValue,'JSON_arr', undefined, $scope, componentModelGetter);
//	realClientValue[iS].setChangeNotifier(function () { changeNotified = true });
//	$scope.$digest();
//	});

//	it("...", function() {
//	});

//	});

});
