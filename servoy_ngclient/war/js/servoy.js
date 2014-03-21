angular.module('servoy',['servoyformat','servoytooltip','servoyfileupload'])
.directive('ngBlur', ['$parse', function($parse) {
    return function(scope, element, attr) {
        var fn = $parse(attr['ngBlur']);
        element.bind('blur', function(event) {
          scope.$apply(function() {
            fn(scope, {$event:event});
          });
        });
      }
}]).value("$swingModifiers" ,{
                      SHIFT_MASK : 1,
                      CTRL_MASK : 2,
                      META_MASK : 4,
                      ALT_MASK : 8,
                      ALT_GRAPH_MASK : 32,
                      BUTTON1_MASK : 16,
                      BUTTON2_MASK : 8,
                      META_MASK : 4,
                      SHIFT_DOWN_MASK : 64,
                      CTRL_DOWN_MASK : 128,
                      META_DOWN_MASK : 256,
                      ALT_DOWN_MASK : 512,
                      BUTTON1_DOWN_MASK : 1024,
                      BUTTON2_DOWN_MASK : 2048,
                      DOWN_MASK : 4096,
                      ALT_GRAPH_DOWN_MASK : 8192
}).value("$anchorConstants", {
                      NORTH : 1,
                      EAST : 2,
                      SOUTH : 4,
                      WEST : 8
}).factory("$utils",function($rootScope) {
	
	// internal function
	function getPropByStringPath(o, s) {
		s = s.replace(/\[(\w+)\]/g, '.$1'); // convert indexes to properties
		s = s.replace(/^\./, '');           // strip a leading dot
		var a = s.split('.');
		while (a.length) {
			var n = a.shift();
			if (n in o) {
				o = o[n];
			} else {
					return;
			}
			return o;
		}
	}
	
	var SCROLLBARS_WHEN_NEEDED = 0;
	var VERTICAL_SCROLLBAR_AS_NEEDED = 1;
	var VERTICAL_SCROLLBAR_ALWAYS = 2;
	var VERTICAL_SCROLLBAR_NEVER = 4;
	var HORIZONTAL_SCROLLBAR_AS_NEEDED = 8;
	var HORIZONTAL_SCROLLBAR_ALWAYS = 16;
	var HORIZONTAL_SCROLLBAR_NEVER = 32;
	
	return{
		
		/** this function can be used in filters .It accepts a string jsonpath the property to test for null. 
    	Example: "item in  model.valuelistID  | filter:notNull('realValue')"*/
		notNull : function (propPath){
			return function(item) {
				return !(getPropByStringPath(item,propPath) === null)
			}
		},
	    autoApplyStyle: function(scope,element,modelToWatch,cssPropertyName){
				      	  scope.$watch(modelToWatch,function(newVal,oldVal){
				      		  if(!newVal) {element.css(cssPropertyName,''); return;}
				      		  if(typeof newVal != 'object'){ //for cases with direct values instead of json string background and foreground
				      			var obj ={}
				      			obj[cssPropertyName] = newVal;
				      			newVal = obj;
				      		  } 
				    	      element.css(cssPropertyName,'')
				    		  element.css(newVal)
				    	  })
	    				},
		getScrollbarsStyleObj:function (scrollbars){
				     var style = {}; 
				        if ((scrollbars & HORIZONTAL_SCROLLBAR_NEVER) == HORIZONTAL_SCROLLBAR_NEVER)
						{
							style.overflowX = "hidden";
						}
						else if ((scrollbars & HORIZONTAL_SCROLLBAR_ALWAYS) == HORIZONTAL_SCROLLBAR_ALWAYS)
						{
							style.overflowX = "scroll";
						}
						else
						{
							style.overflowX = "auto";
						}
				    
						if ((scrollbars & VERTICAL_SCROLLBAR_NEVER) == VERTICAL_SCROLLBAR_NEVER)
						{
							style.overflowY = "hidden"; 
						}
						else if ((scrollbars & VERTICAL_SCROLLBAR_ALWAYS) == VERTICAL_SCROLLBAR_ALWAYS)
						{
							style.overflowY = "scroll"; //$NON-NLS-1$
						}
						else
						{
							style.overflowY = "auto"; //$NON-NLS-1$
						}
			
					return style;
		}
	}
}).directive('ngOnChange', function($parse){
    return function(scope, elm, attrs){       
        var onChangeFunction = $parse(attrs['ngOnChange']);
        elm.bind("change", function(event) {
            scope.$apply(function() {
            	onChangeFunction(scope, { $cmd: event });
            })});
    };
}).directive('svyAutoapply', function($injector,$parse,$log) {
    return {
      restrict: 'A', // only activate on element attribute
      require: '?ngModel', // get a hold of NgModelController
      link: function(scope, element, attrs, ngModel) {
        if(!ngModel) return; // do nothing if no ng-model

        var dataproviderString = attrs.ngModel;
        var index = dataproviderString.indexOf('.');
        if (index > 0) {
	        var modelString = dataproviderString.substring(0,index);
	        var modelFunction = $parse(modelString);
	        var beanModel = modelFunction(scope);
	        var propertyname = dataproviderString.substring(index+1);
	        var beanname;
	        var parent = scope.$parent;
	        
	        if(beanModel.svy_cn === undefined) {
		        for(key in parent.model) {
		        	if (parent.model[key] === beanModel) {
		        		beanname = key;
		        		break;
		        	}
		        }	
	        } else {
	        	beanname = beanModel.svy_cn;
	        }
	        
	        if (!beanname) {
	        	$log.error("bean name not found for model string: " + dataproviderString);
	        	return;
	        }
	        var formname = parent.formname;
	        while (!formname) {
	        	if (parent) {
	        		parent = parent.$parent;
	        		formname = parent.formname;
	        	}
	        	else { 
	        		$log.error("no form found for " + bean + "." + propertyname);
	        		return;
	        	}
	        }
	        
    		if ($injector.has("$servoyInternal")) {
    			var $servoyInternal = $injector.get("$servoyInternal");
		        // Listen for change events to enable binding
		        element.bind('change', function() {
		        	// model has not been updated yet
		        	setTimeout(function() { 
		        		$servoyInternal.push(formname,beanname,propertyname,modelFunction(scope))
		        	}, 0);
		        });
		        // Listen for start edit
	  	        element.bind('focus', function() {
	  	        	setTimeout(function() { 
		        		$servoyInternal.callService("formService", "startEdit", {formname:formname,beanname:beanname,property:propertyname})
	  	        	}, 0);
	  	        });
    		}
        }
        else {
        	$log.error("svyAutoapply attached to a element that doesn't have the right ngmodel (model.value): " + dataproviderString)
        }
      }
    };
}).directive('svyBorder',  function () {
    return {
        restrict: 'A',
        link: function (scope, element, attrs) {
        	  scope.$watch(attrs.svyBorder,function(newVal){
        		  if(typeof newVal !== 'object' || newVal == null) {element.css('border',''); return;}
        		  if(newVal.type == "TitledBorder"){
        			  element.wrap('<fieldset style="padding:5px;margin:0px;border:1px solid silver;width:100%;height:100%"></fieldset>')
        			  element.parent().prepend("<legend align='"+newVal.titleJustiffication+"' style='border-bottom:0px; margin:0px;width:auto;color:"+
        					  newVal.color+";font:"+newVal.font+"'>"+newVal.title+"</legend>")
        			  // TODO unwrap fieldset if borderType changes for example from TitledBorder to LineBorder
        		  }else if(newVal.borderStyle){ 
        			  element.css('border','')
        			  element.css(newVal.borderStyle)
        		  }
        	  })

        }
      };
}).directive('svyMargin',  function ($utils,$parse) {
    return {
        restrict: 'A',
        link: function (scope, element, attrs) {
        	var marginModelObj= $parse(attrs.svyMargin)(scope);
        	if(marginModelObj){ //only design time property, no watch
                element.css(marginModelObj);
        	}
        }
      };
})
.directive('svyFont',  function ($utils) {
    return {
        restrict: 'A',
        link: function (scope, element, attrs) {
        	$utils.autoApplyStyle(scope,element,attrs.svyFont,'font')
        }
      }
})
.directive('svyBackground',  function ($utils) {
    return {
        restrict: 'A',
        link: function (scope, element, attrs) {
        	$utils.autoApplyStyle(scope,element,attrs.svyBackground,'backgroundColor')
        }
      }
})
.directive('svyForeground',  function ($utils) {
    return {
        restrict: 'A',
        link: function (scope, element, attrs) {
        	$utils.autoApplyStyle(scope,element,attrs.svyForeground,'color')
        }
      }
})
.directive('svyScrollbars',  function ($utils,$parse) {
    return {
        restrict: 'A',
        link: function (scope, element, attrs) {
        	var scrollbarsModelObj= $parse(attrs.svyScrollbars)(scope);
        	if(scrollbarsModelObj){ //only design time property, no watch
                element.css($utils.getScrollbarsStyleObj(scrollbarsModelObj));
        	}
         }
    }
})
.factory("$apifunctions", function (){
	
	return {
		
	    getSelectedText: function (elem){
	    	return function(){
	    		return elem.value.substr(elem.selectionStart, elem.selectionEnd - elem.selectionStart);
	    	}
	    },
	    setSelection: function (elem){
	    	return function(start, end) { 
	    		 if (elem.createTextRange) {
	    		      var selRange = elem.createTextRange();
	    		      selRange.collapse(true);
	    		      selRange.moveStart('character', start);
	    		      selRange.moveEnd('character', end);
	    		      selRange.select();
	    		      elem.focus();
	    		 } else if (elem.setSelectionRange) {
	    		    	elem.focus();
	    		    	elem.setSelectionRange(start, end);
	    		 } else if (typeof elem.selectionStart != 'undefined') {
	    		    	elem.selectionStart = start;
	    		    	elem.selectionEnd = end;
	    		    	elem.focus();
	    		 } 
	    	 }
	    }
	}
	
	
})
.filter('htmlFilter', ['$sce', function($sce){
	  return function(input) {
		  if (input && input.indexOf('<body') >=0 && input.lastIndexOf('</body') >=0)
		  {
			  input = input.substring(input.indexOf('<body')+6,input.lastIndexOf('</body'));
		  }
		  return $sce.trustAsHtml(input);;
	 };
}]);