angular.module('servoy',['servoyformat','servoytooltip','servoyfileupload','ui.bootstrap'])
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
}).value("$scrollbarConstants", {
	SCROLLBARS_WHEN_NEEDED : 0,
	VERTICAL_SCROLLBAR_AS_NEEDED : 1,
	VERTICAL_SCROLLBAR_ALWAYS : 2,
	VERTICAL_SCROLLBAR_NEVER : 4,
	HORIZONTAL_SCROLLBAR_AS_NEEDED : 8,
	HORIZONTAL_SCROLLBAR_ALWAYS : 16,
	HORIZONTAL_SCROLLBAR_NEVER : 32
}).value("$foundsetConstants", {
    CALL_ON_ONE_SELECTED_RECORD_IF_TEMPLATE : 0,
    CALL_ON_ALL_RECORDS_IF_TEMPLATE : 1
}).factory("$utils",function($rootScope,$scrollbarConstants) {
	
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
	
	function testKeyPressed(e, keyCode) 
	{
	     var code;
	     
	     if (!e) e = window.event;
	     if (!e) return false;
	     if (e.keyCode) code = e.keyCode;
	     else if (e.which) code = e.which;
	     return code==keyCode;
	}
	
	return{
		
		/** this function can be used in filters .It accepts a string jsonpath the property to test for null. 
    	Example: "item in  model.valuelistID  | filter:notNullOrEmpty('realValue')"*/
		notNullOrEmpty : function (propPath){
			return function(item) {
				var propByStringPath = getPropByStringPath(item,propPath); 
				return !(propByStringPath === null || propByStringPath == '')
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
				        if ((scrollbars & $scrollbarConstants.HORIZONTAL_SCROLLBAR_NEVER) == $scrollbarConstants.HORIZONTAL_SCROLLBAR_NEVER)
						{
							style.overflowX = "hidden";
						}
						else if ((scrollbars & $scrollbarConstants.HORIZONTAL_SCROLLBAR_ALWAYS) == $scrollbarConstants.HORIZONTAL_SCROLLBAR_ALWAYS)
						{
							style.overflowX = "scroll";
						}
						else
						{
							style.overflowX = "auto";
						}
				    
						if ((scrollbars & $scrollbarConstants.VERTICAL_SCROLLBAR_NEVER) == $scrollbarConstants.VERTICAL_SCROLLBAR_NEVER)
						{
							style.overflowY = "hidden"; 
						}
						else if ((scrollbars & $scrollbarConstants.VERTICAL_SCROLLBAR_ALWAYS) == $scrollbarConstants.VERTICAL_SCROLLBAR_ALWAYS)
						{
							style.overflowY = "scroll"; //$NON-NLS-1$
						}
						else
						{
							style.overflowY = "auto"; //$NON-NLS-1$
						}
			
					return style;
		},
		attachEventHandler: function($parse,element,scope,svyEventHandler,domEvent, filterFunction) {
		    var functionReferenceString = svyEventHandler;
			var index = functionReferenceString.indexOf('(');
			if (index != -1) functionReferenceString = functionReferenceString.substring(0,index);
			if( scope.$eval(functionReferenceString) ) {
			   var fn = $parse(svyEventHandler);
     		   element.on(domEvent, function(event) {
     		   	   if (!filterFunction || filterFunction(event)) {
	              		scope.$apply(function() {
	               		 fn(scope, {$event:event});
	              		});
	              	}
	            });
    		}
		},
		testEnterKey: function(e) 
		{
			return testKeyPressed(e,13);
		},
		bindTwoWayObjectProperty : function (a, propertyNameA, b, propertyNameB, useObjectEquality, scope) {
			var toWatchA = (scope ? "a." + propertyNameA : function() { return a[propertyNameA] });
			var toWatchB = (scope ? "b." + propertyNameB : function() { return b[propertyNameB] });

			if (!scope) scope = $rootScope;
			return [
			        scope.$watch(toWatchA, function (newValue, oldValue, scope) { if (newValue !== oldValue) b[propertyNameB] = a[propertyNameA] }, useObjectEquality),
			        scope.$watch(toWatchB, function (newValue, oldValue, scope) { if (newValue !== oldValue) a[propertyNameA] = b[propertyNameB] }, useObjectEquality)
			];
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
}).directive('svyAutoapply', function($servoyInternal,$parse,$log) {
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

    	    // TODO deprecate svy_cn? remove from codebase if possible
	        if(beanModel.svy_cn === undefined) {
	        	beanname = element.attr("name");
		        if (! beanname) {
			        var nameParentEl = element.parents("[name]").first(); 
		        	if (nameParentEl) beanname = nameParentEl.attr("name");
		        }
		        if (! beanname) {
		        	for(key in parent.model) {
		        		if (parent.model[key] === beanModel) {
		        			beanname = key;
		        			break;
		        		}
		        	}
		        }
	        } else {
	        	beanname = beanModel.svy_cn;
	        }
	        
	        if (!beanname) {
	        	$log.error("[svy-autoapply] bean name not found for model string: " + dataproviderString);
	        	return;
	        }
	        
	        var formname = parent.formname;
	        var formParentScope = parent;
	        while (!formname) {
	        	formParentScope = formParentScope.$parent;
	        	if (formParentScope) {
	        		formname = formParentScope.formname;
	        	}
	        	else { 
	        		$log.error("[svy-autoapply] no form found for " + beanname + "." + propertyname);
	        		return;
	        	}
	        }

	        // search for svy-apply attribute on element, within parents (svy-autoapply could be used on a child DOM element of the web component)
	        var svyApply;
	        var svyApplyAttrValue = element.attr("svy-apply");
	        if (! svyApplyAttrValue) {
		        var applyParentEl = element.parents("[svy-apply]").first(); 
	        	if (applyParentEl) svyApplyAttrValue = applyParentEl.attr("svy-apply");
	        }
	        if (svyApplyAttrValue) {
	        	svyApply = parent.$eval(svyApplyAttrValue);
	        }
	        
		        // Listen for change events to enable binding
		     element.bind('change', function() {
		        	// model has not been updated yet
		        	setTimeout(function() { 
		        		var beanModel = modelFunction(scope);
		        		// use svyApply rather then pushChange because svyApply might get intercepted by components such as portals
		        		// that have nested child web components
		    	        if (svyApply) {
		    	        	svyApply(propertyname);
		    	        } else {
		    	        	// this shouldn't happen (svy-apply not being set on a web-component...)
			        		if (beanModel) $servoyInternal.pushChange(formname,beanname,propertyname,beanModel[propertyname],beanModel.rowId);
			        		else $servoyInternal.pushChangeDefault(formname,beanname,propertyname);
		    	        }
		        	}, 0);
		     });
		     // Listen for start edit
	  	     element.bind('focus', function() {
	  	        	setTimeout(function() { 
		        		$servoyInternal.callService("formService", "startEdit", {formname:formname,beanname:beanname,property:propertyname})
	  	        	}, 0);
	  	     });

        }
        else {
        	$log.error("svyAutoapply attached to a element that doesn't have the right ngmodel (model.value): " + dataproviderString)
        }
      }
    };
}).directive('svyEnter',  function ($parse,$utils) {
    return {
        restrict: 'A',
        link: function (scope, element, attrs) {
        	$utils.attachEventHandler($parse,element,scope,attrs.svyEnter,'keydown', $utils.testEnterKey);
        }
      };
}).directive('svyClick',  function ($parse,$utils) {
    return {
        restrict: 'A',
        link: function (scope, element, attrs) {
        	$utils.attachEventHandler($parse,element,scope,attrs.svyClick,'click');
        }
      };
}).directive('svyDblclick',  function ($parse,$utils) {
    return {
        restrict: 'A',
        link: function (scope, element, attrs) {
        	$utils.attachEventHandler($parse,element,scope,attrs.svyDblclick,'dblclick');
        }
      };
}).directive('svyRightclick',  function ($parse,$utils) {
    return {
        restrict: 'A',
        link: function (scope, element, attrs) {
        	$utils.attachEventHandler($parse,element,scope,attrs.svyRightclick,'contextmenu');
        }
      };
}).directive('svyFocusgained',  function ($parse,$utils) {
    return {
        restrict: 'A',
        link: function (scope, element, attrs) {
        	$utils.attachEventHandler($parse,element,scope,attrs.svyFocusgained,'focus');
        }
      };
}).directive('svyFocuslost',  function ($parse,$utils) {
    return {
        restrict: 'A',
        link: function (scope, element, attrs) {
        	$utils.attachEventHandler($parse,element,scope,attrs.svyFocuslost,'blur');
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
.directive('svyHorizontaldirection',  function ($parse,$scrollbarConstants) {
    return {
        restrict: 'A',
        link: function (scope, element, attrs) {
        	var scrollbarsModelObj= $parse(attrs.svyHorizontaldirection)(scope);
        	if ((scrollbarsModelObj & $scrollbarConstants.VERTICAL_SCROLLBAR_NEVER) == $scrollbarConstants.VERTICAL_SCROLLBAR_NEVER) // vertical scrollbar never
			{
        		element.css('float','left');
        		element.css('margin-right','2px');
			}
         }
    }
})
.directive('svyMnemonic',  function ($utils,$parse) {
    return {
        restrict: 'A',
        link: function (scope, element, attrs) {
        	var letter= $parse(attrs.svyMnemonic)(scope);
        	if(letter){ //only design time property, no watch
                element.attr('accesskey',letter);
        	}
         }
    }
})
.directive('svyImagemediaid',  function ($utils,$parse) {
    return {
        restrict: 'A',
        link: function (scope, element, attrs) {     
        	scope.$watch(attrs.svyImagemediaid,function(newVal){
        		if(newVal.img){
          		  var bgstyle = {};
        		  bgstyle['background-image'] = "url('" + newVal.img + "')"; 
        		  bgstyle['background-repeat'] = "no-repeat";
        		  bgstyle['background-position'] = "left";
        		  bgstyle['display'] = "inline-block";
        		  bgstyle['vertical-align'] = "middle"; 
        		  var imageWidth = $parse('model.imageWidth')(scope);
        		  var imageHeight = $parse('model.imageHeight')(scope);
        		  var mediaOptions = $parse('model.mediaOptions')(scope);
        		  if(mediaOptions == undefined) mediaOptions = 14; // reduce-enlarge & keep aspect ration
        		  var mediaKeepAspectRatio = mediaOptions == 0 || ((mediaOptions & 8) == 8);

        		  // default  img size values
        		  var imgWidth = 16;
        		  var imgHeight = 16;
        		  
        		  if (newVal.img.indexOf('imageWidth=') > 0 && newVal.img.indexOf('imageHeight=') > 0)
        		  {
        			  var vars = {};
        			  var parts = newVal.img.replace(/[?&]+([^=&]+)=([^&]*)/gi,    
        					  function(m,key,value) {
        				  vars[key] = value;
        			  });
        			  imgWidth = vars['imageWidth'];
        			  imgHeight = vars['imageHeight'];
        		  }
        		  
        		  var widthChange = imgWidth / newVal.width;
        		  var heightChange = imgHeight / newVal.height;
        		  
        		  if (widthChange > 1.01 || heightChange > 1.01 || widthChange < 0.99 || heightChange < 0.99) // resize needed
        		  {
  						if ((mediaOptions & 6) == 6) // reduce-enlarge
	    				{
	    					if (mediaKeepAspectRatio)
	    					{
	    						if (widthChange > heightChange)
	    						{
	    							imgWidth = imgWidth / widthChange;
	    							imgHeight = imgHeight / widthChange;
	    						}
	    						else
	    						{
	    							imgWidth = imgWidth / heightChange;
	    							imgHeight = imgHeight / heightChange;
	    						}
	    					}
	    					else
	    					{
	    						imgWidth = newVal.width;
	    						imgHeight = newVal.height;
	    					}
	    				}        			  
	  					else if ((mediaOptions & 2) == 2) // reduce
    					{
    						if (widthChange > 1.01 && heightChange > 1.01)
    						{
    							if (mediaKeepAspectRatio)
    							{
    								if (widthChange > heightChange)
    								{
    									imgWidth = imgWidth / widthChange;
    									imgHeight = imgHeight / widthChange;
    								}
    								else
    								{
    									imgWidth = imgWidth / heightChange;
    									imgHeight = imgHeight / heightChange;
    								}
    							}
    							else
    							{
    	    						imgWidth = newVal.width;
    	    						imgHeight = newVal.height;
    							}
    						}
    						else if (widthChange > 1.01)
    						{
    							imgWidth = imgWidth / widthChange;
    							if (mediaKeepAspectRatio)
    							{
    								imgHeight = imgHeight / widthChange;
    							}
    							else
    							{
    								imgHight = newVal.height;
    							}
    						}
    						else if (heightChange > 1.01)
    						{
    							imgHeight = imgHeight / heightChange;
    							if (mediaKeepAspectRatio)
    							{
    								imgWidth = imgWidth / heightChange;
    							}
    							else
    							{
    								imgWidth = newVal.width;
    							}
    						}
    					}
    					else if ((mediaOptions & 4) == 4) // enlarge
    					{
    						if (widthChange < 0.99 && heightChange < 0.99)
    						{
    							if (mediaKeepAspectRatio)
    							{
    								if (widthChange > heightChange)
    								{
    									imgWidth = imgWidth / widthChange;
    									imgHeight = imgHeight / widthChange;
    								}
    								else
    								{
    									imgWidth = imgWidth / heightChange;
    									imgHeight = imgHeight / heightChange;
    								}
    							}
    							else
    							{
    	    						imgWidth = newVal.width;
    	    						imgHight = newVal.height;
    							}
    						}
    						else if (widthChange < 0.99)
    						{
    							imgWidth = imgWidth / widthChange;
    							if (mediaKeepAspectRatio)
    							{
    								imgHeight = imgHeight / widthChange;
    							}
    							else
    							{
    								imgHight = newVal.height;
    							}
    						}
    						else if (heightChange < 0.99)
    						{
    							imgHeight = imgHeight / heightChange;
    							if (mediaKeepAspectRatio)
    							{
    								imgWidth = imgWidth / heightChange;
    							}
    							else
    							{
    								imgWidth = newVal.width;
    							}
    						}
    					}
        		  }	  
        		  
        		  bgstyle['background-size'] = mediaKeepAspectRatio ? "contain" : "100% 100%";
    			  bgstyle['width'] = Math.round(imgWidth) + "px";
        		  bgstyle['height'] = Math.round(imgHeight) + "px";
        		  element.css(bgstyle)
        		}else{
        			element.css('background-image','');
        		}        		
        	}, true)
         }
    }
})
.directive('svyTextrotation',  function ($utils,$parse) {
	// DESIGN TIME ONLY
    return {
        restrict: 'A',
        link: function (scope, element, attrs) {  
        	var rotation= $parse(attrs.svyTextrotation)(scope);
            if (rotation && rotation != 0)
            {
          	  var r = 'rotate(' + rotation + 'deg)';
          	  var style ={}
          	  style['-moz-transform'] = r;
          	  style['-webkit-transform'] = r;
          	  style['-o-transform'] = r;
          	  style['-ms-transform'] = r;
          	  style['transform'] = r;
          	  style['position'] = 'absolute';
          	  if (rotation == 90 || rotation == 270)
          	  {
          		 style['width'] = scope.model.size.height+'px';
          	     style['height'] = scope.model.size.width+'px';
          		 style['left'] =  (scope.model.size.width -scope.model.size.height)/2 +'px';
          		 style['top'] = (scope.model.size.height -scope.model.size.width)/2 +'px';
          	  }
          	 //setTimeout(function(){ // temporary fix until case with ImageMediaID will be fixed (will probably not use bagckgroun-image)
          		element.css(style);  
          	  //},30)
            }
         }
    }
})
.directive('svyHorizontalalignment',  function ($utils,$parse) {
	// DESIGN TIME ONLY
    return {
        restrict: 'A',
        link: function (scope, element, attrs) {  
        	var halign= $parse(attrs.svyHorizontalalignment)(scope);
        	var style ={}
        	if (halign == 0)
        	{
        		 style['text-align'] = 'center';
        	}
        	else if (halign == 4)
        	{
        		style['text-align'] = 'right';
        	}
        	else
        	{
        		style['text-align'] = 'left';
        	}
            element.css(style);
         }
    }
})
.directive('svyVerticalalignment',  function ($utils,$parse) {
	// DESIGN TIME ONLY
    return {
        restrict: 'A',
        link: function (scope, element, attrs) { 
        	// see http://zerosixthree.se/vertical-align-anything-with-just-3-lines-of-css/
        	// do we need preserve-3d ?
        	var halign= $parse(attrs.svyVerticalalignment)(scope);
        	var style ={}
        	if (halign == 1)
        	{
        		 style['top'] = 0;
        	}
        	else if (halign == 3)
        	{
        		style['bottom'] = 0;
        	}
        	else
        	{
        		style['top'] = '50%';
        		style['transform'] = 'translateY(-50%)';
        	}
            element.css(style);
         }
    }
})
.directive('svyTabseq',  function () {
	return {
		restrict: 'A',
		link: function (scope, element, attrs) {
			scope.$watch(attrs.svyTabseq,function(newVal){
				element.attr('tabIndex',newVal ? newVal : 0);
			})
		}
	};
})
.directive('svySelectonenter',  function ($timeout) {
	return {
		restrict: 'A',
		link: function (scope, element, attrs) {
			if (scope.$eval(attrs.svySelectonenter))
			{
				element.bind('focus', function() {
					$timeout(function() {
						element[0].select(); 
					},0);
				});
			}
		}
	};
})
.directive('svyRollovercursor',  function ($timeout) {
	return {
		restrict: 'A',
		link: function (scope, element, attrs) {
			if (scope.$eval(attrs.svyRollovercursor) == 12 /* hand_cursor */)
			{
				element.css('cursor','pointer');
			}
		}
	};
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
}]).filter('mnemonicletterFilter', function($sce){  /* this filter is used for display only*/
	  return function(input,letter) {
		  if(letter && input) return $sce.trustAsHtml(input.replace(letter, '<u>'+letter+'</u>'));
		  if(input) {return $sce.trustAsHtml(''+input);}
		  return input
	 };
}).directive('svyFormatvldisplay',['$parse', function($parse){
	//it is similar to svy-format
	return{
		restrict:'A',
		require: 'ng-Model',
		link: function(scope,element,attrs,ngModelController){
			var vlAccessor= $parse(attrs.svyFormatvldisplay)
		    ngModelController.$formatters.push(function(dpValue){
		    	var valueList = vlAccessor(scope);	
		    		 if(valueList){
		    			 for (var i=0;i<valueList.length;i++)
						  {  
							  if(valueList[i].realValue == dpValue) return valueList[i].displayValue;
						  }
		    		 }		    		 		    		 
		     	     return dpValue;
		     });
		}
	}
}]).directive('svyFormstyle',  function () {
    return {
        restrict: 'A',
        link: function (scope, element, attrs) {
        	element.css({position:'absolute'});
        	scope.$watch(attrs.svyFormstyle, function(newVal) {
        		if (newVal)
        		{
        			if(isInContainer(scope)){
        				delete newVal.minWidth
        				delete newVal.minHeight
        			}
        			element.css(newVal)
        		}	
        	})
        }
      }
    // checks if formProperties on the scope exists
   function isInContainer(scope){
		var parent = scope.$parent;
		while(parent){
			if(parent.formProperties && parent.formStyle) return true
			parent =parent.$parent
		}
		return false
	}	
})
.directive("svyComponentWrapper", ['$compile', function ($compile) {
		return {
			priority: 1000,
			//replace: true,
			//transclude: false,
			restrict: 'E',
			//scope: false,
			compile: function compile(tElement, tAttrs, transclude) {
				var templateFragment = " ";
				angular.forEach(tAttrs.$attr, function(value, key) {
					if (key != 'tagname') templateFragment += ' ' + key + '="' + tAttrs[key] + '"';
				});
				templateFragment += "/>";
			
				return function (scope, element, attr, controller, transcludeFn) {
					var tagName = scope.$eval(tAttrs.tagname);
					var templateElement = angular.element('<' + tagName + templateFragment);
					templateElement.append(tElement.html());
					var el = $compile(templateElement)(scope);
					element.replaceWith(el);
				}
			}
		};
	}]
)
.factory('$svyNGEvents', ['$timeout', '$rootScope', function($timeout, $rootScope) {
	var requestAnimationFrame = window.requestAnimationFrame || window.mozRequestAnimationFrame || window.webkitRequestAnimationFrame || window.msRequestAnimationFrame;
	 
	return {
		
		/** Sometimes you want to execute code after the DOM is processed already by Angular; for example if a component directive
  			is using jQuery plugins/code to manipulate / hide / replace DOM that is populated with Angular. That is the purpose of this function.
  			It will try to execute the given function before the browser render happens - only once. */
		afterNGProcessedDOM : function (fn, doApply) {
			if (requestAnimationFrame) {
				if (doApply) {
					window.requestAnimationFrame(function (scope) {
						$rootScope.$apply(fn);
					});
				} else window.requestAnimationFrame(fn);
			} else $timeout(fn, 0, doApply); // it could produce flicker, but better then nothing
		}
	}
}])

