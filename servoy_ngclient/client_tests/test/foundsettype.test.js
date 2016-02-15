describe("Test foundset_custom_property suite", function() {

	beforeEach(module('servoy'));
	beforeEach(module('foundset_viewport_module')); // for foundset and viewport conversions
	beforeEach(module('foundset_custom_property'));
	beforeEach(module('sabloApp')); // for 'date' conversions

	var sabloConverters;
	var foundsetTypeConstants;
	var $scope;
	var iS;

	var angularEquality = function(first, second) {
		return angular.equals(first, second);
	};

	beforeEach(inject(function(_$sabloConverters_, _$compile_, _$rootScope_, _$foundsetTypeConstants_){
		jasmine.addCustomEqualityTester(angularEquality);
		// The injector unwraps the underscores (_) from around the parameter
		//names when matching
		sabloConverters = _$sabloConverters_;
		iS = sabloConverters.INTERNAL_IMPL;
		foundsetTypeConstants = _$foundsetTypeConstants_;
		$compile = _$compile_;

		$scope = _$rootScope_.$new();
	}));

	// var CHANGE = 0;
	// var INSERT = 1;
	// var DELETE = 2;

	// conversion info for full viewport update:     serverJSONValue[UPDATE_PREFIX + VIEW_PORT][CONVERSIONS][ROWS]
	// conversion info for granular viewport update: serverJSONValue[UPDATE_PREFIX + VIEW_PORT][CONVERSIONS][UPDATE_PREFIX + ROWS]
	// conversion info for full value update:        serverJSONValue[VIEW_PORT][CONVERSIONS]
	
	
	describe("Wrapper suite for async testing where needed", function() { // async hack because of recent timeout being added to sending selected index
		var serverValue;
		var originalTimeout;
		var realClientValue;
		var componentModelGetter;
		var changeNotified = false;
		
		var someDate = new Date();
		var someDateMs = someDate.getTime();
		
		function getAndClearNotified() {
			var tm = changeNotified;
			changeNotified = false;
			return tm;
		};

		beforeEach(function() {
			originalTimeout = jasmine.DEFAULT_TIMEOUT_INTERVAL;
			jasmine.DEFAULT_TIMEOUT_INTERVAL = 1000;
		});

		afterEach(function() {
			jasmine.DEFAULT_TIMEOUT_INTERVAL = originalTimeout;
		});
		
		it("Should not send change of int value to server when no pushToServer is specified for property", function() {
			serverValue = {
					"serverSize": 0,
					"selectedRowIndexes": [],
					"multiSelect": false,
					"viewPort": {
						"startIndex": 0,
						"size": 0,
						"rows": [{
							 "d": someDateMs, "i": 1234, "_svyRowId": "5.10643;_0"
						 }]
					}
			};

			var template = '<div></div>';
			$compile(template)($scope);
			realClientValue = sabloConverters.convertFromServerToClient(serverValue,'foundset', $scope.model, $scope, componentModelGetter);
			realClientValue[iS].setChangeNotifier(function () { changeNotified = true });
			$scope.$digest();
			expect(getAndClearNotified()).toEqual(false);

			// so no "w": false in server received value...
			var tmp = realClientValue.viewPort.rows[0].i;
			realClientValue.viewPort.rows[0].i = 4321234;
			$scope.$digest();
			expect(getAndClearNotified()).toEqual(false);
			expect(realClientValue[iS].isChanged()).toEqual(false);
			realClientValue.viewPort.rows[0].i = tmp;
		});
		
		it("Will get template dummy value", function() {
			serverValue = {
					"serverSize": 0,
					"selectedRowIndexes": [],
					"multiSelect": false,
					"viewPort": {
						"startIndex": 0,
						"size": 0,
						"rows": []
					}
			};

			var template = '<div></div>';
			$compile(template)($scope);
			realClientValue = sabloConverters.convertFromServerToClient(serverValue,'foundset', $scope.model, $scope, componentModelGetter);
			realClientValue[iS].setChangeNotifier(function () { changeNotified = true });
			$scope.$digest();
		});

		it("Should get viewport size then request viewport and change selection", function() {
			// *** initial size no viewport
			var updateValue = {
					"serverSize": 6,
					"w": false,
					"selectedRowIndexes":[0],
					"multiSelect": false,
					"viewPort": 
					{
						"startIndex": 0,
						"size": 0,
						"rows": []
					}
			};
			realClientValue = sabloConverters.convertFromServerToClient(updateValue,'foundset', realClientValue, $scope, componentModelGetter);
			$scope.$digest();
			realClientValue[iS].setChangeNotifier(function () { changeNotified = true });
			expect(realClientValue).toEqual(angular.copy(updateValue));

			// *** request and receive new viewport (all records in this case)
			realClientValue.loadRecordsAsync(0,6);
			expect(getAndClearNotified()).toEqual(true);
			expect(realClientValue[iS].isChanged()).toEqual(true);
			expect(sabloConverters.convertFromClientToServer(realClientValue, 'foundset', realClientValue)).toEqual(
					[{"newViewPort":{"startIndex":0,"size":6}}]
			);
			expect(getAndClearNotified()).toEqual(false);
			expect(realClientValue[iS].isChanged()).toEqual(false);

			realClientValue = sabloConverters.convertFromServerToClient({
				"upd_viewPort": 
				{
					"startIndex": 0,
					"size": 6,
					"conversions": {"rows":{"0":{"d":"Date"},"1":{"d":"Date"},"2":{"d":"Date"},"4":{"d":"Date"},"5":{"d":"Date"}}},
					"rows": 
						[
						 {
							 "d": someDateMs, "i": 1234, "_svyRowId": "5.10643;_0"
						 },

						 {
							 "d": someDateMs, "i": 1234, "_svyRowId": "5.10692;_1"
						 },

						 {
							 "d": someDateMs, "i": 1234, "_svyRowId": "5.10702;_2"
						 },

						 {
							 "d": null, "i": 1234, "_svyRowId": "5.10835;_3"
						 },

						 {
							 "d": someDateMs, "i": 1234, "_svyRowId": "5.10952;_4"
						 },

						 {
							 "d": someDateMs, "i": 1234, "_svyRowId": "5.11011;_5"
						 }
						 ]
				}
			},'foundset', realClientValue, $scope, componentModelGetter);
			$scope.$digest(); // so that watches are aware of new values and keep them as old ones
			expect(realClientValue).toEqual({
				"serverSize": 6,
				"selectedRowIndexes": 
					[
					 0
					 ],

					 "multiSelect": false,
					 "viewPort": 
					 {
						 "startIndex": 0,
						 "size": 6,
						 "rows": 
							 [
							  {
								  "d": someDate, "i": 1234, "_svyRowId": "5.10643;_0"
							  },

							  {
								  "d": someDate, "i": 1234, "_svyRowId": "5.10692;_1"
							  },

							  {
								  "d": someDate, "i": 1234, "_svyRowId": "5.10702;_2"
							  },

							  {
								  "d": null, "i": 1234, "_svyRowId": "5.10835;_3"
							  },

							  {
								  "d": someDate, "i": 1234, "_svyRowId": "5.10952;_4"
							  },

							  {
								  "d": someDate, "i": 1234, "_svyRowId": "5.11011;_5"
							  }
							  ]
					 }
			});

			// *** Selection change from Client
			realClientValue.selectedRowIndexes[0] = 1;
			$scope.$digest();
		});

		it("Waits for selection to trigger notification", function(done) {
			function fn()
			{
				if (getAndClearNotified()) {
					done();
				}
				setTimeout(fn, 50);
			}
			fn();
		});
		
		it("The selection change notification sent", function() {
			expect(realClientValue[iS].isChanged()).toEqual(true);
			expect(sabloConverters.convertFromClientToServer(realClientValue, 'foundset', realClientValue)).toEqual(
					[{"newClientSelection":[1]}]
			);
			expect(getAndClearNotified()).toEqual(false);
			expect(realClientValue[iS].isChanged()).toEqual(false);
		});
		
		
		it("Should get selection update from server", function() {
			// *** initial size no viewport
			realClientValue = sabloConverters.convertFromServerToClient({"upd_selectedRowIndexes":[2]},'foundset', realClientValue, $scope, componentModelGetter);
			$scope.$digest();
			expect(realClientValue.selectedRowIndexes.length).toEqual(1);
			expect(realClientValue.selectedRowIndexes[0]).toEqual(2);
		});
		
		it("Should insert 2 before selection (server); this is a special case where foundset automatically expands viewport if viewport was showing whole foundset (an optimisation for scroll views)", function() {
			realClientValue = sabloConverters.convertFromServerToClient({
				"upd_serverSize": 8,
				"upd_selectedRowIndexes": [4],
				"upd_viewPort": 
				{
					"startIndex": 0,
					"size": 8,
					"conversions": {"upd_rows":{"2":{"rows":{"0":{"d":"Date"}}},"3":{"rows":{"0":{"d":"Date"}}}}},
					"upd_rows": 
					[
						{
							"rows": [{"d": null, "i": 1234, "_svyRowId": "5.11078;_1"}],
							"startIndex": 1,
							"endIndex": 7,
							"type": 1
						},
						{
							"rows": [{"d": null, "i": 1234, "_svyRowId": "5.11078;_1"}],
							"startIndex": 1,
							"endIndex": 1,
							"type": 0
						},
						{
							"rows": [{"d": someDateMs, "i": 1234, "_svyRowId": "5.11079;_2"}],
							"startIndex": 2,
							"endIndex": 8,
							"type": 1
						},
						{
							"rows": [{"d": someDateMs, "i": 1234, "_svyRowId": "5.11079;_2"}],
							"startIndex": 2,
							"endIndex": 2,
							"type": 0
						}
					]
				}
			},'foundset', realClientValue, $scope, componentModelGetter);
			$scope.$digest();
			expect(realClientValue).toEqual({
				"serverSize": 8,
				"selectedRowIndexes": 
				[
					4
				],

				"multiSelect": false,
				"viewPort": 
				{
					"startIndex": 0,
					"size": 8,
					"rows": 
					[
						{
							"d": someDate, "i": 1234, "_svyRowId": "5.10643;_0"
						},

						{
							"d": null, "i": 1234, "_svyRowId": "5.11078;_1"
						},

						{
							"d": someDate, "i": 1234, "_svyRowId": "5.11079;_2"
						},

						{
							"d": someDate, "i": 1234, "_svyRowId": "5.10692;_1"
						},

						{
							"d": someDate, "i": 1234, "_svyRowId": "5.10702;_2"
						},

						{
							"d": null, "i": 1234, "_svyRowId": "5.10835;_3"
						},

						{
							"d": someDate, "i": 1234, "_svyRowId": "5.10952;_4"
						},

						{
							"d": someDate, "i": 1234, "_svyRowId": "5.11011;_5"
						}
					]
				}
			});
			expect(realClientValue.selectedRowIndexes[0]).toEqual(4);
		});
		
		it("Should remove the inserted 2 (server) and one more (1-3)", function() {
			realClientValue = sabloConverters.convertFromServerToClient({
				"upd_serverSize": 5,
				"upd_selectedRowIndexes": 
				[
					1
				],

				"upd_viewPort": 
				{
					"startIndex": 0,
					"size": 5,
					"upd_rows": 
					[
						{
							"rows": 
							[
								
							],

							"startIndex": 1,
							"endIndex": 1,
							"type": 2
						},

						{
							"rows": 
							[
								
							],

							"startIndex": 1,
							"endIndex": 1,
							"type": 2
						},

						{
							"rows": 
							[
								
							],

							"startIndex": 1,
							"endIndex": 1,
							"type": 2
						}
					]
				}
			},'foundset', realClientValue, $scope, componentModelGetter);
			$scope.$digest();
			expect(realClientValue).toEqual({
				"serverSize": 5,
				"selectedRowIndexes": 
				[
					1
				],

				"multiSelect": false,
				"viewPort": 
				{
					"startIndex": 0,
					"size": 5,
					"rows": 
					[
						{
							"d": someDate, "i": 1234, "_svyRowId": "5.10643;_0"
						},

						{
							"d": someDate, "i": 1234, "_svyRowId": "5.10702;_2"
						},

						{
							"d": null, "i": 1234, "_svyRowId": "5.10835;_3"
						},

						{
							"d": someDate, "i": 1234, "_svyRowId": "5.10952;_4"
						},

						{
							"d": someDate, "i": 1234, "_svyRowId": "5.11011;_5"
						}
					]
				}
			});
		});

		it("Foundset changed completely (relation & parent record changed for example on server - to something that is larger then we want to cache; so we will request smaller viewport)", function() {
			realClientValue = sabloConverters.convertFromServerToClient({
				"upd_serverSize": 12,
				"upd_selectedRowIndexes": 
				[
					0
				],

				"upd_viewPort": 
				{
					"startIndex": 0,
					"size": 0,
					"rows": 
					[
						
					]
				}
			},'foundset', realClientValue, $scope, componentModelGetter);
			$scope.$digest();
			expect(realClientValue.serverSize).toEqual(12);
			expect(realClientValue.selectedRowIndexes[0]).toEqual(0);
			expect(realClientValue.viewPort.size).toEqual(0);
			expect(realClientValue.viewPort.rows.length).toEqual(0);
			
			// *** request and receive new viewport (all except 3 records at the end)
			realClientValue.loadRecordsAsync(0,9);
			expect(getAndClearNotified()).toEqual(true);
			expect(realClientValue[iS].isChanged()).toEqual(true);
			expect(sabloConverters.convertFromClientToServer(realClientValue, 'foundset', realClientValue)).toEqual(
					[{"newViewPort":{"startIndex":0,"size":9}}]
			);
			expect(getAndClearNotified()).toEqual(false);
			expect(realClientValue[iS].isChanged()).toEqual(false);
			
			// *** viewport comes from server
			realClientValue = sabloConverters.convertFromServerToClient({
				"upd_viewPort": 
				{
					"startIndex": 0,
					"size": 9,
					"conversions": {"rows":{"0":{"d":"Date"},"1":{"d":"Date"},"2":{"d":"Date"},"3":{"d":"Date"},"4":{"d":"Date"},"5":{"d":"Date"},"6":{"d":"Date"},"7":{"d":"Date"},"8":{"d":"Date"}}},
					"rows": 
					[
						{
							"d": someDateMs, "i": 1234, "_svyRowId": "5.10350;_0"
						},

						{
							"d": someDateMs, "i": 1234, "_svyRowId": "5.11110;_1"
						},

						{
							"d": someDateMs, "i": 1234, "_svyRowId": "5.11111;_2"
						},

						{
							"d": someDateMs, "i": 1234, "_svyRowId": "5.11108;_3"
						},

						{
							"d": someDateMs, "i": 1234, "_svyRowId": "5.11109;_4"
						},

						{
							"d": someDateMs, "i": 1234, "_svyRowId": "5.11106;_5"
						},

						{
							"d": someDateMs, "i": 1234, "_svyRowId": "5.11107;_6"
						},

						{
							"d": someDateMs, "i": 1234, "_svyRowId": "5.11104;_7"
						},

						{
							"d": someDateMs, "i": 1234, "_svyRowId": "5.11105;_8"
						}
					]
				}
			},'foundset', realClientValue, $scope, componentModelGetter);
			$scope.$digest();
			expect(realClientValue).toEqual({
				"serverSize": 12,
				"selectedRowIndexes": 
				[
					0
				],

				"multiSelect": false,
				"viewPort": 
				{
					"startIndex": 0,
					"size": 9,
					"rows": 
					[
						{
							"d": someDate, "i": 1234, "_svyRowId": "5.10350;_0"
						},

						{
							"d": someDate, "i": 1234, "_svyRowId": "5.11110;_1"
						},

						{
							"d": someDate, "i": 1234, "_svyRowId": "5.11111;_2"
						},

						{
							"d": someDate, "i": 1234, "_svyRowId": "5.11108;_3"
						},

						{
							"d": someDate, "i": 1234, "_svyRowId": "5.11109;_4"
						},

						{
							"d": someDate, "i": 1234, "_svyRowId": "5.11106;_5"
						},

						{
							"d": someDate, "i": 1234, "_svyRowId": "5.11107;_6"
						},

						{
							"d": someDate, "i": 1234, "_svyRowId": "5.11104;_7"
						},

						{
							"d": someDate, "i": 1234, "_svyRowId": "5.11105;_8"
						}
					]
				}
			});
		});
		
		it("Should insert 2 at index 1 (now viewport stays the same as bounds but 2 get inserted and 2 from bottom get removed)", function() {
			realClientValue = sabloConverters.convertFromServerToClient({
				"upd_serverSize": 14,
				"upd_selectedRowIndexes": 
				[
					2
				],
				"upd_viewPort": 
				{
					"conversions": {"upd_rows":{"0":{"rows":{"0":{"d":"Date"}}},"1":{"rows":{"0":{"d":"Date"}}},"2":{"rows":{"0":{"d":"Date"}}},"3":{"rows":{"0":{"d":"Date"}}}}},
					"upd_rows": 
					[
						{
							"rows": 
							[
								{
									"d": someDateMs, "i": 1234, "_svyRowId": "5.1112;_1"
								}
							],

							"startIndex": 0,
							"endIndex": 9,
							"type": 1
						},

						{
							"rows": 
							[
								{
									"d": someDateMs, "i": 1234, "_svyRowId": "5.11112;_1"
								}
							],

							"startIndex": 0,
							"endIndex": 0,
							"type": 0
						},

						{
							"rows": 
							[
								{
									"d": someDateMs, "i": 1234, "_svyRowId": "5.11113;_2"
								}
							],

							"startIndex": 1,
							"endIndex": 9,
							"type": 1
						},

						{
							"rows": 
							[
								{
									"d": someDateMs, "i": 1234, "_svyRowId": "5.11113;_2"
								}
							],

							"startIndex": 1,
							"endIndex": 1,
							"type": 0
						}
					]
				}
			},'foundset', realClientValue, $scope, componentModelGetter);
			$scope.$digest();
			expect(realClientValue).toEqual({
				"serverSize": 14,
				"selectedRowIndexes": 
				[
					2
				],

				"multiSelect": false,
				"viewPort": 
				{
					"startIndex": 0,
					"size": 9,
					"rows": 
					[
						{
							"d": someDate, "i": 1234, "_svyRowId": "5.11112;_1"
						},

						{
							"d": someDate, "i": 1234, "_svyRowId": "5.11113;_2"
						},

						{
							"d": someDate, "i": 1234, "_svyRowId": "5.10350;_0"
						},

						{
							"d": someDate, "i": 1234, "_svyRowId": "5.11110;_1"
						},

						{
							"d": someDate, "i": 1234, "_svyRowId": "5.11111;_2"
						},

						{
							"d": someDate, "i": 1234, "_svyRowId": "5.11108;_3"
						},

						{
							"d": someDate, "i": 1234, "_svyRowId": "5.11109;_4"
						},

						{
							"d": someDate, "i": 1234, "_svyRowId": "5.11106;_5"
						},

						{
							"d": someDate, "i": 1234, "_svyRowId": "5.11107;_6"
						},
					]
				}
			});
		});
		
		it("Should insert at last position (but still part of foundset)", function() {
			realClientValue = sabloConverters.convertFromServerToClient({
				"upd_serverSize": 15,
				"upd_viewPort": 
				{
					"conversions": {"upd_rows":{"0":{"rows":{"0":{"d":"Date"}}},"1":{"rows":{"0":{"d":"Date"}}}}},
					"upd_rows": 
					[
						{
							"rows": 
							[
								{
									"d": someDateMs, "i": 1234, "_svyRowId": "5.11115;_29"
								}
							],

							"startIndex": 8,
							"endIndex": 9,
							"type": 1
						},

						{
							"rows": 
							[
								{
									"d": someDateMs, "i": 1234, "_svyRowId": "5.11115;_29"
								}
							],

							"startIndex": 8,
							"endIndex": 8,
							"type": 0
						}
					]
				}
			},'foundset', realClientValue, $scope, componentModelGetter);
			$scope.$digest();
			expect(realClientValue).toEqual({
				"serverSize": 15,
				"selectedRowIndexes": 
				[
					2
				],

				"multiSelect": false,
				"viewPort": 
				{
					"startIndex": 0,
					"size": 9,
					"rows": 
					[
						{
							"d": someDate, "i": 1234, "_svyRowId": "5.11112;_1"
						},

						{
							"d": someDate, "i": 1234, "_svyRowId": "5.11113;_2"
						},

						{
							"d": someDate, "i": 1234, "_svyRowId": "5.10350;_0"
						},

						{
							"d": someDate, "i": 1234, "_svyRowId": "5.11110;_1"
						},

						{
							"d": someDate, "i": 1234, "_svyRowId": "5.11111;_2"
						},

						{
							"d": someDate, "i": 1234, "_svyRowId": "5.11108;_3"
						},

						{
							"d": someDate, "i": 1234, "_svyRowId": "5.11109;_4"
						},

						{
							"d": someDate, "i": 1234, "_svyRowId": "5.11106;_5"
						},

						{
							"d": someDate, "i": 1234, "_svyRowId": "5.11115;_29"
						},
					]
				}
			});
		});
		
		it("Should delete last position of viewport (new record should be received in it's place)", function() {
			realClientValue = sabloConverters.convertFromServerToClient({
				"upd_serverSize": 14,
				"upd_viewPort": 
				{
					"conversions": {"upd_rows":{"0":{"rows":{"0":{"d":"Date"}}}}},
					"upd_rows": 
					[
						{
							"rows": 
							[
								{
									"d": someDateMs, "i": 1234, "_svyRowId": "5.11106;_29"
								}
							],

							"startIndex": 8,
							"endIndex": 8,
							"type": 2
						}
					]
				}
			},'foundset', realClientValue, $scope, componentModelGetter);
			$scope.$digest();
			expect(realClientValue).toEqual({
				"serverSize": 14,
				"selectedRowIndexes": 
				[
					2
				],

				"multiSelect": false,
				"viewPort": 
				{
					"startIndex": 0,
					"size": 9,
					"rows": 
					[
						{
							"d": someDate, "i": 1234, "_svyRowId": "5.11112;_1"
						},

						{
							"d": someDate, "i": 1234, "_svyRowId": "5.11113;_2"
						},

						{
							"d": someDate, "i": 1234, "_svyRowId": "5.10350;_0"
						},

						{
							"d": someDate, "i": 1234, "_svyRowId": "5.11110;_1"
						},

						{
							"d": someDate, "i": 1234, "_svyRowId": "5.11111;_2"
						},

						{
							"d": someDate, "i": 1234, "_svyRowId": "5.11108;_3"
						},

						{
							"d": someDate, "i": 1234, "_svyRowId": "5.11109;_4"
						},

						{
							"d": someDate, "i": 1234, "_svyRowId": "5.11106;_5"
						},

						{
							"d": someDate, "i": 1234, "_svyRowId": "5.11106;_29"
						},
					]
				}
			});
		});
		
		it("Should delete first position (new record should be received in it's place)", function() {
			realClientValue = sabloConverters.convertFromServerToClient({
				"upd_serverSize": 13,
				"upd_selectedRowIndexes": 
				[
					1
				],

				"upd_viewPort": 
				{
					"conversions": {"upd_rows":{"0":{"rows":{"0":{"d":"Date"}}}}},
					"upd_rows": 
					[
						{
							"rows": 
							[
								{
									"d": someDateMs, "i": 1234, "_svyRowId": "5.11107;_29"
								}
							],

							"startIndex": 0,
							"endIndex": 0,
							"type": 2
						}
					]
				}
			},'foundset', realClientValue, $scope, componentModelGetter);
			$scope.$digest();
			expect(realClientValue).toEqual({
				"serverSize": 13,
				"selectedRowIndexes": 
				[
					1
				],

				"multiSelect": false,
				"viewPort": 
				{
					"startIndex": 0,
					"size": 9,
					"rows": 
					[
						{
							"d": someDate, "i": 1234, "_svyRowId": "5.11113;_2"
						},

						{
							"d": someDate, "i": 1234, "_svyRowId": "5.10350;_0"
						},

						{
							"d": someDate, "i": 1234, "_svyRowId": "5.11110;_1"
						},

						{
							"d": someDate, "i": 1234, "_svyRowId": "5.11111;_2"
						},

						{
							"d": someDate, "i": 1234, "_svyRowId": "5.11108;_3"
						},

						{
							"d": someDate, "i": 1234, "_svyRowId": "5.11109;_4"
						},

						{
							"d": someDate, "i": 1234, "_svyRowId": "5.11106;_5"
						},

						{
							"d": someDate, "i": 1234, "_svyRowId": "5.11106;_29"
						},
						
						{
							"d": someDate, "i": 1234, "_svyRowId": "5.11107;_29"
						}

					]
				}
			});
		});
		it("Should scroll down to bottom of foundset - viewport needs to be expanded )", function() {
			realClientValue.loadExtraRecordsAsync(4);
			$scope.$digest();
			expect(getAndClearNotified()).toEqual(true);
			expect(realClientValue[iS].isChanged()).toEqual(true);
			expect(sabloConverters.convertFromClientToServer(realClientValue, 'foundset', realClientValue)).toEqual(
					[{"loadExtraRecords":4}]
			);
			expect(getAndClearNotified()).toEqual(false);
			expect(realClientValue[iS].isChanged()).toEqual(false);
			
			realClientValue = sabloConverters.convertFromServerToClient({
				"upd_viewPort": 
				{
					"startIndex": 0,
					"size": 13,
					"conversions": {"upd_rows":{"0":{"rows":{"0":{"d":"Date"},"1":{"d":"Date"},"2":{"d":"Date"},"3":{"d":"Date"}}}}},
					"upd_rows": 
					[
						{
							"rows": 
							[
								{
									"d": someDateMs, "i": 1234, "_svyRowId": "5.10610;_9"
								},

								{
									"d": someDateMs, "i": 1234, "_svyRowId": "5.10631;_10"
								},

								{
									"d": someDateMs, "i": 1234, "_svyRowId": "5.10787;_11"
								},

								{
									"d": someDateMs, "i": 1234, "_svyRowId": "5.10832;_12"
								},
							],

							"startIndex": 9,
							"endIndex": 13,
							"type": 1
						}
					]
				}
			},'foundset', realClientValue, $scope, componentModelGetter);
			$scope.$digest();
			expect(realClientValue).toEqual({
				"serverSize": 13,
				"selectedRowIndexes": 
				[
					1
				],

				"multiSelect": false,
				"viewPort": 
				{
					"startIndex": 0,
					"size": 13,
					"rows": 
					[
						{
							"d": someDate, "i": 1234, "_svyRowId": "5.11113;_2"
						},

						{
							"d": someDate, "i": 1234, "_svyRowId": "5.10350;_0"
						},

						{
							"d": someDate, "i": 1234, "_svyRowId": "5.11110;_1"
						},

						{
							"d": someDate, "i": 1234, "_svyRowId": "5.11111;_2"
						},

						{
							"d": someDate, "i": 1234, "_svyRowId": "5.11108;_3"
						},

						{
							"d": someDate, "i": 1234, "_svyRowId": "5.11109;_4"
						},

						{
							"d": someDate, "i": 1234, "_svyRowId": "5.11106;_5"
						},

						{
							"d": someDate, "i": 1234, "_svyRowId": "5.11106;_29"
						},
						
						{
							"d": someDate, "i": 1234, "_svyRowId": "5.11107;_29"
						},
						
						{
							"d": someDate, "i": 1234, "_svyRowId": "5.10610;_9"
						},

						{
							"d": someDate, "i": 1234, "_svyRowId": "5.10631;_10"
						},

						{
							"d": someDate, "i": 1234, "_svyRowId": "5.10787;_11"
						},

						{
							"d": someDate, "i": 1234, "_svyRowId": "5.10832;_12"
						},
					]
				}
			});
			//Should send change of date value to server
			$scope.$digest();
			var newD = realClientValue.viewPort.rows[12].d = new Date(new Date().getTime() + 1);
			$scope.$digest();
			expect(getAndClearNotified()).toEqual(true);
			expect(realClientValue[iS].isChanged()).toEqual(true);
			expect(sabloConverters.convertFromClientToServer(realClientValue, 'foundset', realClientValue)).toEqual(
					[ { viewportDataChanged: { _svyRowId: '5.10832;_12', dp: 'd', value: newD.getTime() } } ]
			);
			expect(getAndClearNotified()).toEqual(false);
			expect(realClientValue[iS].isChanged()).toEqual(false);
			
			//Should send change of int value to server
			realClientValue.viewPort.rows[0].i = 4321;
			$scope.$digest();
			expect(getAndClearNotified()).toEqual(true);
			expect(realClientValue[iS].isChanged()).toEqual(true);
			expect(sabloConverters.convertFromClientToServer(realClientValue, 'foundset', realClientValue)).toEqual(
					[ { viewportDataChanged: { _svyRowId: '5.11113;_2', dp: 'i', value: 4321 } } ]
			);
			expect(getAndClearNotified()).toEqual(false);
			expect(realClientValue[iS].isChanged()).toEqual(false);
			
			//Should send sort to server
			var newD = realClientValue.sort([{"name":'i', "direction":"asc"}, {"name":'d', "direction":"desc"}]);
			$scope.$digest();
			expect(getAndClearNotified()).toEqual(true);
			expect(realClientValue[iS].isChanged()).toEqual(true);
			expect(sabloConverters.convertFromClientToServer(realClientValue, 'foundset', realClientValue)).toEqual(
					[ { sort: [ { name: 'i', direction: 'asc' }, { name: 'd', direction: 'desc' } ] } ]
			);
			expect(getAndClearNotified()).toEqual(false);
			expect(realClientValue[iS].isChanged()).toEqual(false);;
		});
	});

});
