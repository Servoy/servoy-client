angular.module('servoyformat',[]).factory("$formatterUtils",function($filter){     // to remove
	// add replace all to String 
	String.prototype.replaceAll = function (find, replace) {
	    var str = this;
	    return str.replace(new RegExp(find, 'g'), replace);
	};
	String.prototype.insert = function (index, string) {
		  if (index > 0)
		    return this.substring(0, index) + string + this.substring(index, this.length);
		  else
		    return string + this;
	};
	
	getWeek = function(dateArg) {
		  var date = new Date(dateArg.getTime());
		   date.setHours(0, 0, 0, 0);
		  // Thursday in current week decides the year.
		  date.setDate(date.getDate() + 3 - (date.getDay() + 6) % 7);
		  // January 4 is always in week 1.
		  var week1 = new Date(date.getFullYear(), 0, 4);
		  // Adjust to Thursday in week 1 and count number of weeks from date to week1.
		  return 1 + Math.round(((date.getTime() - week1.getTime()) / 86400000
		                        - 3 + (week1.getDay() + 6) % 7) / 7);
		}
	getDayInYear = function (date){
		var start = new Date(date.getFullYear(), 0, 0);
		var diff = date - start;
		var oneDay = 1000 * 60 * 60 * 24;
		return  day = Math.floor(diff / oneDay);
	}
	
	
	
	// internal function
	function formatNumbers(data , servoyFormat){
		if(!servoyFormat ) return data
		var partchedFrmt=  servoyFormat;   // patched format for numeraljs format
		
		//scientific notation case
		if(servoyFormat.indexOf('E') >-1){
			var frmt = /([0#.,]+)+E0+.*/.exec(servoyFormat)[1];
			var integerDigits = 0;
			var fractionalDigits = 0;
			var countIntegerState=true;
			for(var i=0; i<frmt.length; i++)
			{
				var chr = frmt[i];
				if(chr == '.') {countIntegerState = false; continue;}
				if(chr == ',') continue;
				if(countIntegerState){
					integerDigits++;
				}
				else{
					fractionalDigits++;
				}
			}
			return new Number(data).toExponential(integerDigits+fractionalDigits);

		}
		//treat percents and per thousants
		var centIndex = -1;
		var milIndex = -1;
		var MILLSIGN = '\u2030' //‰
		if(servoyFormat.indexOf("%") >-1){
			data *= 100;
			centIndex = partchedFrmt.indexOf("%")
			partchedFrmt = partchedFrmt.replaceAll("%","p"); // p doesn't mean anything in numeraljs
			
		}else if(servoyFormat.indexOf(MILLSIGN) >-1){
			data *= 1000;
			milIndex = partchedFrmt.indexOf(MILLSIGN)
			partchedFrmt = partchedFrmt.replaceAll(MILLSIGN,"p");
		}
		if(servoyFormat.indexOf("-") > -1) data *=-1;	
		
		partchedFrmt = partchedFrmt.replaceAll('\u00A4','$');
		partchedFrmt = partchedFrmt.replaceAll('(#+)','[$1]');
		partchedFrmt = partchedFrmt.replaceAll('#','0');

		var ret = numeral(data).format(partchedFrmt);
		
		if(centIndex>-1) ret = ret.insert(centIndex,'%')
		if(milIndex>-1) ret = ret.insert(milIndex,MILLSIGN)
		return ret;

	}
	// internal function
	function unformatNumbers(data , format){// todo throw error when not coresponding to format (reimplement with state machine)
		//treat scientiffic numbers
		if(data.toLowerCase().indexOf('e')>-1){
			return new Number(data).valueOf()
		}
		
		var multFactor =1;
		if(format.indexOf('-')>-1){
			if(data.indexOf('-')!= data.lastIndexOf('-')){ // double minus case
				multFactor = 1;
			}else{
				multFactor =-1;				
			}
		}
		var MILLSIGN =  '\u2030'
		if(format.indexOf(MILLSIGN)>-1){
			multFactor *= 0.001
		}
		var ret =  numeral().unformat(data)
		ret *=multFactor;
		return ret
	}
	
	// internal function
	function formatText(data ,servoyFormat){
		if(!servoyFormat ) return data;
		var error = "input string not corresponding to format : "+data +" , "+ servoyFormat
		var ret =''
		var isEscaping = false;
		var offset = 0;
		for(var i=0; i<servoyFormat.length; i++)
		{
			var formatChar = servoyFormat[i];
			var dataChar = data[i-offset];
			if(dataChar == undefined) break;
			if(isEscaping  && formatChar!= "'"){
				if(dataChar!=formatChar) throw error
				ret+= dataChar;
				continue;
			}
			switch (formatChar) {
			case "'":
			    isEscaping != isEscaping;
			    offset++;
				break;
			case 'U':
				if(dataChar.match(/[a-zA-Z]/) == null) throw error
				ret+= dataChar.toUpperCase()
				break;
			case 'L':
				if(dataChar.match(/[a-zA-Z]/) == null) throw error
				ret+= dataChar.toLowerCase()
				break;
			case 'A':
				if(dataChar.match(/[0-9a-zA-Z]/) == null) throw error
				ret+= dataChar;
				break;
			case '?':
				if(dataChar.match(/[a-zA-Z]/) == null) throw error
				ret+= dataChar
				break;
			case '*':
				ret+= dataChar;
				break;
			case 'H':
				if(dataChar.match(/[0-9a-fA-F]/) == null) throw error
				ret+= dataChar.toUpperCase();
				break;
			case '#':
				if(dataChar.match(/[\d]/) == null) throw error
				ret+= dataChar;
				break;
			default:
				 ret+= formatChar; 
				 if(formatChar != dataChar) offset++;
				break;
			}
		}
		return ret; 
	}
	// internal function
	function formatDate(data ,dateFormat){
		if(!dateFormat ) return data;
		var ret = dateFormat.replaceAll('z','Z'); //timezones
		ret = ret.replaceAll('S+','sss') // milliseconds
		ret = ret.replaceAll('a+','a') 
		
		if(ret.indexOf('D')!= -1){
			// day in year 
			ret = ret.replaceAll('D',getDayInYear(data))
		}
		ret= $filter('date')(data,ret);
		var weekday = ["Sunday","Monday", "Tuesday", "Wednesday", "Thursday", "Friday","Saturday"];
		if(ret.indexOf('E')!=-1){
			var last = ret.lastIndexOf('E');
			var first = ret.indexOf('E') ;
			var nrChars = last - first + 1;
			if (nrChars >= 1 && nrChars <= 3) ret = ret.substring(0,first-1) + ' ' + (weekday[data.getDay()]).substring(0, 3) + ret.substring(last + 1,ret.length); 
			if (nrChars >= 4 && nrChars <= 6) ret = ret.substring(0,first-1) + ' ' + weekday[data.getDay()] + ret.substring(last + 1,ret.length); 
		}		
		if(ret.indexOf('G')!= -1){
			// 	Era designator
			var AD_BC_border= new Date('1/1/1').setFullYear(0);
			var eraDesignator =  AD_BC_border < data ? 'AD': 'BC'
			ret = ret.replaceAll('G',eraDesignator)
		}
		
		// week in year
		if(ret.indexOf('w')!= -1)ret = ret.replaceAll('w',getWeek(data))
		if(ret.indexOf('W')!= -1){
			// week in month
			var monthBeginDate = new Date(data)
			monthBeginDate.setDate(1)
			var currentWeek = getWeek(data);
			var monthBeginWeek = getWeek(monthBeginDate);
			ret = ret.replaceAll('W',currentWeek - monthBeginWeek+1);
		}
		if(ret.indexOf('k')!= -1){
			//Hour in day (1-24)
			var hours =  data.getHours()
			if (hours == 0 ) hours = 24;
			var kk = /.*?(k+).*/.exec(dateFormat)[1];
			var leadingZero =''
			if(kk.length>1 && (''+hours).length ==1){
				leadingZero = '0'
			}
			ret = ret.replaceAll('k+',leadingZero +hours)
		}
		if(ret.indexOf('K')!= -1){
			//Hour in am/pm (0-11)
			var hours =  ''+data.getHours()%12
			var KK = /.*?(K+).*/.exec(dateFormat)[1];
			if(KK.length >1 && hours.length ==1){
				hours = '0'+hours;
			}
			ret = ret.replaceAll('K+',hours)
		}

		return ret
	}

	return{
		
		format : function (data,servoyFormat,type){
			if((!servoyFormat) || (!type) || (!data) ) return data;
			if((type == "NUMBER") || (type == "INTEGER")){
				 return formatNumbers(data,servoyFormat);
			}else if(type == "TEXT"){
				return formatText(data,servoyFormat);
			}else if(type == "DATETIME"){
				return formatDate(data,servoyFormat);
			}			
			return data;			
		},
		
		unformat : function(data ,servoyFormat,type){
			if((!servoyFormat)||(!type) || (!data) ) return data;
			if((type == "NUMBER") || (type == "INTEGER")){
				return unformatNumbers(data,servoyFormat);
			}else if(type == "TEXT"){
				return data;
			}else if(type == "DATETIME"){
				// some compatibility issues, see http://momentjs.com/docs/ and http://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html
				servoyFormat = servoyFormat.replaceAll('d','D');
				servoyFormat = servoyFormat.replaceAll('y','Y');
				// use moment.js from calendar component
				return moment(data,servoyFormat).toDate();
			}
			return data;
		}
	}
}).filter('formatFilter', function($formatterUtils){  /* this filter is used for display only*/
	  return function(input,servoyFormat,type) {
		  var ret = input;
		  try{
			  // TODO apply servoy default formatting from properties file here
			  if(input instanceof Date && !servoyFormat) {
				  servoyFormat = 'MM/dd/yyyy hh:mm aa';
				  type = 'DATETIME';
			  }
			  
			  if (((typeof input === 'string') || (input instanceof String)) && type !== "TEXT") return input;

			  ret = $formatterUtils.format(input,servoyFormat,type);
		  }catch(e){
			  console.log(e);
			  //TODO decide what to do when string doesn't correspod to format
		  }
		  return ret;
	 };
}).directive("svyFormat", ["$formatterUtils","$parse","$utils",function ($formatterUtils,$parse, $utils){
	return {
		  require: 'ngModel',
		  priority: 1,
		  link: function($scope, element, attrs, ngModelController) {
			 var useMask = false;
			 $scope.$watch(attrs['svyFormat'], function(newVal, oldVal){
				 if (newVal !== oldVal) {
					 var formatters = ngModelController.$formatters; 
					 var idx = formatters.length;
				     var viewValue = ngModelController.$modelValue;
				     while (idx--) {
				        viewValue = formatters[idx](viewValue);
				     }
		             ngModelController.$viewValue = viewValue;
		             ngModelController.$render();
				 }
			 })
			 
			 element.on('focus',function(){
				 var svyFormat = $scope.$eval(attrs['svyFormat'])
				 if(svyFormat && !$scope.model.findmode){
					 if(svyFormat.edit && svyFormat.isMask) {
							 var settings = {};
							 settings.placeholder = svyFormat.placeHolder ? svyFormat.placeHolder : " ";
							 if (svyFormat.allowedCharacters)
								 settings.allowedCharacters = svyFormat.allowedCharacters;
							 
							 element.mask(svyFormat.edit,settings);
					 }else if(svyFormat.edit){
						 $scope.$evalAsync(function(){
							 ngModelController.$setViewValue(modelToView(ngModelController.$modelValue))
							 ngModelController.$render();
						 })
					 }		
				 }				 			  
			 })
			 element.on('blur',function(){
				 var svyFormat = $scope.$eval(attrs['svyFormat'])
				 if(svyFormat && !$scope.model.findmode){
					 if(svyFormat.edit && svyFormat.isMask) element.unmask();
					 $scope.$evalAsync(function(){
						 ngModelController.$setViewValue(modelToView(ngModelController.$modelValue))
						 ngModelController.$render();
					 })	
				 }				 
			 })

			 element.on('keypress',function(e){
				 var svyFormat = $scope.$eval(attrs['svyFormat'])
				 if(svyFormat && !$scope.model.findmode && !attrs['typeahead']){
					 if(svyFormat.type == "INTEGER"){
						 return numbersonly(e, false, svyFormat.decimalSeparator, svyFormat.groupingSeparator, svyFormat.currencySymbol, svyFormat.percent, element, svyFormat.maxLength);
					 } else if(svyFormat.type == "NUMBER" || ((svyFormat.type == "TEXT") && svyFormat.isNumberValidator)){
						 return numbersonly(e, true, svyFormat.decimalSeparator, svyFormat.groupingSeparator, svyFormat.currencySymbol, svyFormat.percent, element, svyFormat.maxLength);
					 }
				 }
				 return true;
			 })
			 
			 //convert data from view format to model format
		    ngModelController.$parsers.push(viewToModel);
		    //convert data from model format to view format
		    ngModelController.$formatters.push(modelToView);
		    
		    var callChangeOnBlur = null;
		    var enterKeyCheck = function(e) {
				if (callChangeOnBlur && $utils.testEnterKey(e)) {
					callChangeOnBlur();
				}
			}
		    
		    function viewToModel(viewValue) {
		    	var svyFormat = $scope.$eval(attrs['svyFormat'])
		    	var data = viewValue
		    	if(svyFormat && !$scope.model.findmode){
			    	var format = null;
			    	var type = svyFormat ? svyFormat.type: null;
			    	format = svyFormat.display? svyFormat.display : svyFormat.edit 
			    	if(element.is(":focus"))format = svyFormat.edit	
			    	try{
			      	  var data =  $formatterUtils.unformat(viewValue,format,type);
			    	}catch(e){
			    		console.log(e)
				      	//TODO set error state 
				      	//ngModelController.$error ..
			    	}
			    	if (svyFormat.type == "TEXT" && (svyFormat.uppercase || svyFormat.lowercase)) {
			    		var currentData = data;
			    		if (svyFormat.uppercase) data = data.toUpperCase();
			    		else if(svyFormat.lowercase) data = data.toLowerCase();
			    		// if the data is really changed then this pushes it back to the interface
			    		// problem is that then a dom onchange event will not happen..
			    		// we must fire a change event then in the onblur.
			    		if (currentData !== data) {
			    			var caret = element[0].selectionStart;
				    		if (!callChangeOnBlur) {
				    			callChangeOnBlur = function() {
				    				element.change();
				    				element.off("blur", callChangeOnBlur);
				    				element.off("keypress", enterKeyCheck);
				    			}
				    			element.on("blur", callChangeOnBlur);
				    			element.on("keydown",enterKeyCheck);
				    		}
				    		ngModelController.$viewValue = data;
				    		ngModelController.$render();
				    	    element[0].selectionStart = caret;
				    		element[0].selectionEnd = caret;
			    		}
			    		else {
			    			// this will be a change that will be recorded by the dom element itself
			    			if (callChangeOnBlur) {
			    				element.off("blur", callChangeOnBlur);
			    				element.off("keypress", enterKeyCheck);
								callChangeOnBlur = null;
			    			}
			    		}
					}
		    	}
		    	return data; //converted
		    }
		    
		    function modelToView(modelValue) {
		    	var svyFormat = $scope.$eval(attrs['svyFormat'])
		    	var data = modelValue;
		    	if(svyFormat && !$scope.model.findmode){
			    	var format = null;
			    	var type = svyFormat ? svyFormat.type: null;
			    	format = svyFormat.display? svyFormat.display : svyFormat.edit 
			    	if(element.is(":focus"))format = svyFormat.edit
			    	try {
			    		data = $formatterUtils.format(modelValue,format,type);
			    	}catch(e){
			    		console.log(e)
				      	//TODO set error state 
				      	//ngModelController.$error ..
			    	}
			    	if (data && svyFormat.type == "TEXT") {
			    		if (svyFormat.uppercase) data = data.toUpperCase();
			    		else if(svyFormat.lowercase) data = data.toLowerCase();
					}
		    	}
		    	return data; //converted
		    }
		    
			function numbersonly(e, decimal, decimalChar, groupingChar, currencyChar, percentChar,obj,mlength) 
			{
				var key;
				var keychar;
				
				if (window.event) {
				   key = window.event.keyCode;
				}
				else if (e) {
				   key = e.which;
				}
				else {
				   return true;
				}
				
				if ((key==null) || (key==0) || (key==8) ||  (key==9) || (key==13) || (key==27) 
						|| (e.ctrlKey && key==97) || (e.ctrlKey && key==99) || (e.ctrlKey && key==118) || (e.ctrlKey && key==120)) { //added CTRL-A, X, C and V
				   return true;
				}

				keychar = String.fromCharCode(key);

				if (mlength > 0 && obj && obj.value)
				{
					var counter = 0;
					if (("0123456789").indexOf(keychar) != -1) counter++;
					var stringLength = obj.value.length;
					for(var i=0;i<stringLength;i++)
					{
					   if (("0123456789").indexOf(obj.value.charAt(i)) != -1) counter++;
					}
					var selectedTxt = Servoy.Utils.getSelectedText(obj.id);
					if(selectedTxt) counter = counter - selectedTxt.length; 
					if (counter > mlength) return false;
				}
				
				if ((("-0123456789").indexOf(keychar) > -1)) {
				   return true;
				}
				else if (decimal && (keychar == decimalChar)) { 
				  return true;
				}
				else if (keychar == groupingChar) { 
				  return true;
				}
				else if (keychar == currencyChar) { 
				  return true;
				}
				else if (keychar == percentChar) { 
				  return true;
				}
				return false;
			}
		  }
		}	
}]);
