servoyModule.factory("$formatterUtils",function($filter){     // to remove
	
	// add replace all to String 
	String.prototype.replaceAll = function (find, replace) {
	    var str = this;
	    return str.replace(new RegExp(find, 'g'), replace);
	};
	
	// internal function
	function javaFormatToNumeralsJS(servoyFormat){
		if(!servoyFormat.display ) return ''
		var str = servoyFormat.display.replaceAll('#','0');
		str = str.replaceAll('¤','$');
		return str;

	}
	
	// internal function
	function formatText(data ,servoyFormat){
		if(!servoyFormat.display ) return data;
		var error = "input string not corresponding to format"
		var ret =''
		var isEscaping = false;
		var offset = 0;
		for(var i=0; i<servoyFormat.display.length; i++)
		{
			var formatChar = servoyFormat.display[i];
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
				if(dataChar.match(/[a-fA-F]/) == null) throw error
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
		if(!servoyFormat.display ) return data;
		var str = servoyFormat.display.replaceAll('z','Z'); //year
		str =str.replaceAll('K','H')
		str = str.replaceAll('k','h')
		str = str.replaceAll('S+','.sss') // milliseconds
		str = str.replaceAll('a+','a') 
		return  $filter('date')(data,str);
	}
	
	return{

		format : function (data,servoyFormat){
			if((!servoyFormat) || (!servoyFormat.type) || (!data) ) return data;
			if(servoyFormat.type == "NUMBER"){
				var frmt = javaFormatToNumeralsJS(servoyFormat);
				return numeral(data).format(frmt);
			}else if(servoyFormat.type == "TEXT"){
				return formatText(data,servoyFormat);
			}else if(servoyFormat.type == "DATETIME"){
				return formatDate(data,servoyFormat);
			}
			
			return data;
			
		},
		
		unformat : function(data ,servoyFormat){
			if((!servoyFormat)||(!servoyFormat.type) || (!data) ) return data;
			if(servoyFormat.type == "NUMBER"){
				return numeral().unformat(data);
			}else if(servoyFormat.type == "TEXT"){
				return data;
			}else if(servoyFormat.type == "DATETIME"){
				//unformatting is handled by calendar widget
			}
			return data;
		}
	}
}).filter('formatFilter', function($formatterUtils){  /* this filter is used for display only*/
	  return function(input,javaFormat) {
		  var ret = input;
		  try{
			  ret = $formatterUtils.format(input,javaFormat);
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
