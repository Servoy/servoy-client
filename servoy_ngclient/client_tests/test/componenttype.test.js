describe("Test component_custom_property suite", function() {

  beforeEach(module('servoy'));
  beforeEach(module('foundset_viewport_module'));
  beforeEach(module('component_custom_property'));

  var sabloConverters;
  var $scope;
  var serverValue;
  var converted;

  beforeEach(inject(function(_$sabloConverters_, _$compile_, _$rootScope_){
    // The injector unwraps the underscores (_) from around the parameter
    //names when matching
    sabloConverters = _$sabloConverters_;
    $scope = _$rootScope_.$new();
    $compile = _$compile_;
    serverValue = {
      n:false,
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

    var template = '<div"></div>';
    $compile(template)($scope);
    converted = sabloConverters.convertFromServerToClient(serverValue,'component', $scope.model, $scope);
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
    converted.handlers.onActionMethodID({},0);
    expect(converted.__internalState.isChanged()).toBe(true);
    expect(converted.__internalState.requests[0].handlerExec.eventType).toBe('onActionMethodID');
  });

  it("should add a startEdit request after startEdit has been called", function() {
    expect(converted.__internalState.isChanged()).toBe(false);
    converted.servoyApi.startEdit('myproperty',0);
    expect(converted.__internalState.isChanged()).toBe(true);
    expect(converted.__internalState.requests[0].svyStartEdit.pn).toBe('myproperty');
  });

  it("should add an apply request", function() {
    expect(converted.__internalState.isChanged()).toBe(false);
    converted.servoyApi.apply('text',$scope.model,0);
    expect(converted.__internalState.isChanged()).toBe(true);
    expect(converted.__internalState.requests[0].svyApply['pn']).toBe('text');
    expect(converted.__internalState.requests[0].svyApply['v']).toBe('buitonn');
  });


  it("should handle an incremental update", function() {
    var updateValue = {
      propertyUpdates: {
        model : {
          text: "updatedButtonText"
        }
      }
    };
    sabloConverters.convertFromServerToClient(updateValue,'component', serverValue, $scope);
    expect(serverValue.model.text).toBe('updatedButtonText');
  });

  it("should handle an initial data set", function() {
    var updateValue = {
      propertyUpdates: {
        model : {
          format: {type:"TEXT"},
        },
        model_vp : [
          {_svyRowId:'1', dataProviderID:'book1'},
          {_svyRowId:'2', dataProviderID:'book2'}
        ]
      }
    };
    var converted = sabloConverters.convertFromServerToClient(updateValue,'component', serverValue, $scope);

    expect(converted.modelViewport[1].dataProviderID).toBe('book2');

    var updateValue = {
      propertyUpdates: {
        model : {
          text: "updatedButtonText"
        }
      }
    };

    converted = sabloConverters.convertFromServerToClient(updateValue,'component', converted, $scope);
    expect(converted.model.text).toBe('updatedButtonText');

  });


  it("should handle an incremental update", function() {
    var updateValue = {
      propertyUpdates: {
        model : {
          text: "updatedButtonText"
        }
      }
    };
    sabloConverters.convertFromServerToClient(updateValue,'component', serverValue, $scope);
    expect(serverValue.model.text).toBe('updatedButtonText');

  });

  it("should handle an initial data with conversions ", function() {
    var updateValue = {
      propertyUpdates: {
                        conversions : {
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
                                  {_svyRowId:'1', dataProviderID:1141240331660},
                                  {_svyRowId:'2', dataProviderID:1141240331661}
                        ]
      }
    };
    var converted = sabloConverters.convertFromServerToClient(updateValue,'component', serverValue, $scope);
    expect(converted.modelViewport[0].dataProviderID.toString()).toBe('Wed Mar 01 2006 21:12:11 GMT+0200 (EET)');
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
