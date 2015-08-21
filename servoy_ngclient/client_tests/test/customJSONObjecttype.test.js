describe("Test custom_object_property suite", function() {

	var sabloConverters;
	var $scope;
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
		module('custom_json_object_property');
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
		});

		// mock timout
//		jasmine.clock().install();
	});

	afterEach(function() {
//		jasmine.clock().uninstall();
	});

	describe("custom_object_property with dumb values suite; pushToServer not set (so reject)", function() {
		beforeEach(function() {
			serverValue = {
					"vEr": 1,
					"v": 
					{
						"relationName": null,
						"text": "pers_edit_rv",
						"mnemonic": null,
						"name": null,
						"active": true,
						"containsFormId": "pers_edit_rv",
						"disabled": false
					}
			};

			var template = '<div></div>';
			$compile(template)($scope);
			realClientValue = sabloConverters.convertFromServerToClient(serverValue,'JSON_obj', undefined, $scope, componentModelGetter);
			realClientValue[iS].setChangeNotifier(function () { changeNotified = true });
			$scope.$digest();
		});


		it("Should not send value updates for when pushToServer is not specified", function() {
			// *** initial size no viewport
			realClientValue.text = "some_modified_text"; 
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
					"v": 
					{
						"relationName": null,
						"text": "pers_edit_rv",
						"mnemonic": null,
						"name": null,
						"active": true,
						"containsFormId": "pers_edit_rv",
						"disabled": false
					}
			};

			var template = '<div></div>';
			$compile(template)($scope);
			realClientValue = sabloConverters.convertFromServerToClient(serverValue,'JSON_obj', undefined, $scope, componentModelGetter);
			realClientValue[iS].setChangeNotifier(function () { changeNotified = true });
			$scope.$digest();
		});

		it("Should not send value updates for when pushToServer is not specified", function() {
			// *** initial size no viewport
			realClientValue.text = "some_modified_text"; 
			$scope.$digest();

			expect(getAndClearNotified()).toEqual(true);
			expect(realClientValue[iS].isChanged()).toEqual(true);
			expect(sabloConverters.convertFromClientToServer(realClientValue, 'JSON_obj', realClientValue)).toEqual(
					{ vEr: 1, u: [ { k: 'text', v: 'some_modified_text' } ] }
			);

			expect(getAndClearNotified()).toEqual(false);
			expect(realClientValue[iS].isChanged()).toEqual(false);
		});

	});

//	describe("custom+object_property with smart values suite; pushToServer not set (so reject)", function() {
//	beforeEach(function() {
//	serverValue = {?};

//	var template = '<div></div>';
//	$compile(template)($scope);
//	realClientValue = sabloConverters.convertFromServerToClient(serverValue,'JSON_obj', undefined, $scope, componentModelGetter);
//	realClientValue[iS].setChangeNotifier(function () { changeNotified = true });
//	$scope.$digest();
//	});


//	it("...", function() {
//	});
//	});

//	describe("custom_object_property with smart values suite; pushToServer set to shallow", function() {
//	beforeEach(function() {
//	serverValue = {?};

//	var template = '<div></div>';
//	$compile(template)($scope);
//	realClientValue = sabloConverters.convertFromServerToClient(serverValue,'JSON_obj', undefined, $scope, componentModelGetter);
//	realClientValue[iS].setChangeNotifier(function () { changeNotified = true });
//	$scope.$digest();
//	});

//	it("...", function() {
//	});

//	});

});
