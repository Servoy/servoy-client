servoyModule.factory("$formatterUtils",function($filter){     // to remove
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
		
		partchedFrmt = partchedFrmt.replaceAll('¤','$');
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
				break;
			}
		}
		return ret; 
	}
	// internal function
	function formatDate(data ,servoyFormat){
		if(!servoyFormat ) return data;
		var ret = servoyFormat.replaceAll('z','Z'); //timezones
		ret = ret.replaceAll('S+','sss') // milliseconds
		ret = ret.replaceAll('a+','a') 
		
		if(ret.indexOf('D')!= -1){
			// day in year 
			ret = ret.replaceAll('D',getDayInYear(data))
		}
		ret= $filter('date')(data,ret);
		
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
			var kk = /.*?(k+).*/.exec(servoyFormat)[1];
			var leadingZero =''
			if(kk.length>1 && (''+hours).length ==1){
				leadingZero = '0'
			}
			ret = ret.replaceAll('k+',leadingZero +hours)
		}
		if(ret.indexOf('K')!= -1){
			//Hour in am/pm (0-11)
			var hours =  ''+data.getHours()%12
			var KK = /.*?(K+).*/.exec(servoyFormat)[1];
			if(KK.length >1 && hours.length ==1){
				hours = '0'+hours;
			}
			ret = ret.replaceAll('K+',hours)
		}

		return ret 
	}
	function formatInternal(data,servoyFormatObj){
		if((!servoyFormatObj) || (!servoyFormatObj.type) || (!data) ) return data;
		if(servoyFormatObj.type == "NUMBER"){
			 return formatNumbers(data,servoyFormatObj.display);
		}else if(servoyFormatObj.type == "TEXT"){
			return formatText(data,servoyFormatObj.display);
		}else if(servoyFormatObj.type == "DATETIME"){
			return formatDate(data,servoyFormatObj.display);
		}
		
		return data;
		
	}
	return{
		
		format : formatInternal ,
		
		unformat : function(data ,servoyFormat){
			if((!servoyFormat)||(!servoyFormat.type) || (!data) ) return data;
			if(servoyFormat.type == "NUMBER"){
				return unformatNumbers(data,servoyFormat.display);
			}else if(servoyFormat.type == "TEXT"){
				return data;
			}else if(servoyFormat.type == "DATETIME"){
				//unformatting is handled by calendar widget
			}
			return data;
		}
	}
}).filter('formatFilter', function($formatterUtils){  /* this filter is used for display only*/
	  return function(input,servoyFormat) {
		  var ret = input;
		  try{
			  ret = $formatterUtils.format(input,servoyFormat);
		  }catch(e){
			  console.log(e);
			  //TODO decide what to do when string doesn't correspod to format
		  }
		  return ret;
	 };
}).directive("svyFormat", ["$formatterUtils","$parse",function ($formatterUtils,$parse){
	return {
		  require: 'ngModel',
		  link: function($scope, element, attrs, ngModelController) {
			  
		    //convert data from view format to model format
		    ngModelController.$parsers.push(function(viewValue) {
		    	var svyFormat = $parse(attrs['svyFormat'])($scope)
		    	var data = viewValue
		    	try{
		      	var data =  $formatterUtils.unformat(viewValue,svyFormat);
		    	}catch(e){
		    		console.log(e)
			      	//TODO set error state 
			      	//ngModelController.$error ..
		    	}
		      return data; //converted
		    });
		    //convert data from model format to view format
		    ngModelController.$formatters.push(function(modelValue) {
		    	var svyFormat = $parse(attrs['svyFormat'])($scope)
		        var data = modelValue;
		    	try {
		    		data = $formatterUtils.format(modelValue,svyFormat);
		    	}catch(e){
		    		console.log(e)
			      	//TODO set error state 
			      	//ngModelController.$error ..
		    	}
		      return data; //converted
		    });
		  }
		}	
}]);
