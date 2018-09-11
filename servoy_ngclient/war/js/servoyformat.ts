/// <reference path="../../typings/defaults/string.d.ts" />
/// <reference path="../../typings/momentjs/moment.d.ts" />

angular.module('servoyformat', []).factory("$formatterUtils", ['$filter', '$locale', '$utils', function($filter, $locale, $utils) { // to remove
	// add replace all to String
	String.prototype.replaceAll = function(find, replace) {
		var str = this;
		return str.replace(new RegExp(find, 'g'), replace);
	};
	String.prototype.insert = function(index, s) {
		if (index > 0)
			return this.substring(0, index) + s + this.substring(index, this.length);
		else
			return s + this;
	};

	var getWeek = function(dateArg) {
		var date = new Date(dateArg.getTime());
		date.setHours(0, 0, 0, 0);
		// Thursday in current week decides the year.
		date.setDate(date.getDate() + 3 - (date.getDay() + 6) % 7);
		// January 4 is always in week 1.
		var week1 = new Date(date.getFullYear(), 0, 4);
		// Adjust to Thursday in week 1 and count number of weeks from date to week1.
		return 1 + Math.round(((date.getTime() - week1.getTime()) / 86400000 - 3 + (week1.getDay() + 6) % 7) / 7);
	}
	var getDayInYear = function(date) {
		var start = new Date(date.getFullYear(), 0, 0);
		var diff = date - <any>start;
		var oneDay = 1000 * 60 * 60 * 24;
		return Math.floor(diff / oneDay);
	}



	// internal function
	function formatNumbers(data, servoyFormat) {
		if (!servoyFormat)
			return data
		if (data === "")
			return data;
		
		data = Number(data); // just to make sure that if it was a string representing a number we turn it into a number
		if (typeof data === 'number' && isNaN(data)) return ""; // cannot format something that is not a number
		
		var initialData = data;
		var patchedFormat = servoyFormat; // patched format for numeraljs format
		var i, j;
		var prefix = "";
		var sufix = "";
		
		if (patchedFormat.indexOf(';') > 0) {
			if (data < 0) {
				patchedFormat = patchedFormat.split(';')[1]
			} else patchedFormat = patchedFormat.split(';')[0];
		}

		if (data < 0) data *= -1;
		if (patchedFormat.indexOf('\u00A4') >= 0)
		{
			patchedFormat = patchedFormat.replaceAll('\u00A4', numeral.localeData().currency.symbol);
		}	
		if (servoyFormat.indexOf("-") >= 0 && initialData >= 0 && servoyFormat.indexOf(";") < 0)
		{
			patchedFormat = patchedFormat.replaceAll("-", "");
		}
		var MILLSIGN = '\u2030' //‰
			
		if (patchedFormat.indexOf("%") > -1 && patchedFormat.indexOf("'%'") == -1) 
		{
			data *= 100;
		} else	if (patchedFormat.indexOf(MILLSIGN) > -1 && patchedFormat.indexOf("'"+MILLSIGN+"'") == -1) 
		{
			data *= 1000;
		}
		var numberStart = patchedFormat.length;
		var index = patchedFormat.indexOf('0');
		if (index >= 0)
		{
			numberStart = index;
		}
		index = patchedFormat.indexOf('#');
		if (index >= 0 && index < numberStart)
		{
			numberStart = index;
		}
		var numberEnd =  0;
		index = patchedFormat.lastIndexOf('0');
		if (index >= 0)
		{
			numberEnd = index;
		}
		index = patchedFormat.lastIndexOf('#');
		if (index >= 0 && index > numberEnd)
		{
			numberEnd = index;
		}
		
		if (numberStart > 0)
		{
			prefix = patchedFormat.substring(0,numberStart);
		}
		
		if (numberEnd < patchedFormat.length - 1)
		{
			sufix = patchedFormat.substring(numberEnd+1);
		}
		
		patchedFormat = patchedFormat.substring(numberStart,numberEnd+1);
		var ret;
		
		prefix = prefix.replaceAll("'", "");
		sufix = sufix.replaceAll("'", "");
			
		if (servoyFormat.indexOf("-") == -1 && initialData < 0 && servoyFormat.indexOf(";") < 0)
		{
			prefix = prefix + "-";	
		}
		//scientific notation case
		if (servoyFormat.indexOf('E') > -1) {
			var frmt = /([0#.,]+)+E0+.*/.exec(patchedFormat)[1];
			var integerDigits = 0;
			var fractionalDigits = 0;
			var countIntegerState = true;
			for (i = 0; i < frmt.length; i++) {
				var chr = frmt[i];
				if (chr == '.') {
					countIntegerState = false;
					continue;
				}
				if (chr == ',') continue;
				if (countIntegerState) {
					integerDigits++;
				} else {
					fractionalDigits++;
				}
			}
			ret = new Number(data).toExponential(integerDigits + fractionalDigits);
		}
		else
		{
			// get min digits
			var minLen = 0;
			var optionalDigits = 0;
			for (i = 0; i < patchedFormat.length; i++) {
				if (patchedFormat[i] == '0') {
					minLen++;
				} else if (patchedFormat[i] == '#' && minLen === 0) {
					optionalDigits++;
				} else if (patchedFormat[i] == '.') {
					break;
				}
			}
			
			patchedFormat = patchedFormat.replaceAll('(#+)', '[$1]');
			patchedFormat = patchedFormat.replaceAll('#', '0');
			
			ret = numeral(data).format(patchedFormat);

			// set min digits
			if (minLen > 0) {
				var retSplit = ret.split(numeral.localeData().delimiters.decimal);
				for (i = 0; i < retSplit[0].length; i++) {
					if (retSplit[0][i] < '0' || retSplit[0][i] > '9') continue;
					for (j = i; j < retSplit[0].length; j++) {
						if (retSplit[0][j] >= '0' && retSplit[0][j] <= '9') continue;
						break;
					}
					var nrMissing0 = minLen - (j - i);
					if (nrMissing0 > 0) {
						ret = retSplit[0].substring(0, i);
						for (j = 0; j < nrMissing0; j++) ret += '0';
						ret += retSplit[0].substring(i);
						if (retSplit.length > 1) ret += (numeral.localeData().delimiters.decimal + retSplit[1]);
					}
					break;
				}
			}
			//fix the optional digits
			if (patchedFormat.indexOf(',') == -1 && optionalDigits > 0)
			{
				var toEliminate = 0;
				for (i =0;i<ret.length;i++)
				{
					if (ret.charAt(i) == '0')
					{
						toEliminate++;
					}
					else
					{
						break;
					}
				}
				if (toEliminate > 0)
				{
					if (toEliminate > optionalDigits)
					{
						toEliminate = optionalDigits;
					}	
					ret = ret.substring(toEliminate);
					if (ret.indexOf(numeral.localeData().delimiters.decimal) == 0)
					{
						//we eliminated too much
						ret = "0" + ret;
					}
				}
			}
		}
		return prefix + ret + sufix;
	}

	function endsWith(str, suffix) {
		return str.indexOf(suffix, str.length - suffix.length) !== -1;
	}

	// internal function
	function unformatNumbers(data, format) { // todo throw error when not coresponding to format (reimplement with state machine)
		if (data === "") return data;
		//treat scientiffic numbers
		if (data.toString().toLowerCase().indexOf('e') > -1 && !isNaN(data)) {
			return new Number(data).valueOf()
		}

		var multFactor = 1;
		var MILLSIGN = '\u2030'
		if (format.indexOf(MILLSIGN) > -1 && format.indexOf("'"+MILLSIGN+"'") == -1) {
			multFactor *= 0.001
		}
		if (format.indexOf("'%'") > -1) {
			multFactor = 100
		}
		if (format.indexOf("'") > -1)
		{
			// replace the literals
			var parts = format.split("'");
			for (var i=0;i<parts.length;i++)
			{
				if (i % 2 == 1)
				{
					data = data.replaceAll(parts[i],"");
				}
			}
		}		
		var ret = numeral(data).value();
		ret *= multFactor;
		return ret
	}

	// internal function
	function formatText(data, servoyFormat) {
		if (!servoyFormat) return data;
		var error = "input string not corresponding to format : " + data + " , " + servoyFormat
		var ret = ''
		var isEscaping = false;
		var offset = 0;
		if (data && typeof data == 'number')
		{
			data = data.toString();
		}
		for (var i = 0; i < servoyFormat.length; i++) {
			var formatChar = servoyFormat[i];
			var dataChar = data[i - offset];
			if (dataChar == undefined) break;
			if (isEscaping && formatChar != "'") {
				if (dataChar != formatChar) throw error
				ret += dataChar;
				continue;
			}
			switch (formatChar) {
				case "'":
					isEscaping != isEscaping;
					offset++;
					break;
				case 'U':
					if (dataChar.match(/[a-zA-Z\u00C0-\u00ff]/) == null) throw error
					ret += dataChar.toUpperCase()
					break;
				case 'L':
					if (dataChar.match(/[a-zA-Z\u00C0-\u00ff]/) == null) throw error
					ret += dataChar.toLowerCase()
					break;
				case 'A':
					if (dataChar.match(/[0-9a-zA-Z\u00C0-\u00ff]/) == null) throw error
					ret += dataChar;
					break;
				case '?':
					if (dataChar.match(/[a-zA-Z\u00C0-\u00ff]/) == null) throw error
					ret += dataChar
					break;
				case '*':
					ret += dataChar;
					break;
				case 'H':
					if (dataChar.match(/[0-9a-fA-F]/) == null) throw error
					ret += dataChar.toUpperCase();
					break;
				case '#':
					if (dataChar.match(/[\d]/) == null) throw error
					ret += dataChar;
					break;
				default:
					ret += formatChar;
					if (formatChar != dataChar) offset++;
					break;
			}
		}
		return ret;
	}
	// internal function
	function formatDate(data, dateFormat) {
		if (!dateFormat) return data;
		var ret = dateFormat.replaceAll('z', 'Z'); //timezones
		ret = ret.replaceAll('S+', 'sss') // milliseconds
		ret = ret.replaceAll('a+', 'a')

		if (ret.indexOf('D') != -1) {
			// day in year
			ret = ret.replaceAll('D', getDayInYear(data))
		}

		if (ret.indexOf('G') != -1) {
			// 	Era designator
			var AD_BC_border = new Date('1/1/1').setFullYear(0);
			var eraDesignator = AD_BC_border < data ? 'AD' : 'BC'
			ret = ret.replaceAll('G', eraDesignator)
		}

		// week in year
		if (ret.indexOf('w') != -1) ret = ret.replaceAll('w', getWeek(data))
		if (ret.indexOf('W') != -1) {
			// week in month
			var monthBeginDate = new Date(data)
			monthBeginDate.setDate(1)
			var currentWeek = getWeek(data);
			var monthBeginWeek = getWeek(monthBeginDate);
			ret = ret.replaceAll('W', currentWeek - monthBeginWeek + 1);
		}
        var hours;
		if (ret.indexOf('k') != -1) {
			//Hour in day (1-24)
			hours = data.getHours()
			if (hours == 0) hours = 24;
			var kk = /.*?(k+).*/.exec(dateFormat)[1];
			var leadingZero = ''
			if (kk.length > 1 && ('' + hours).length == 1) {
				leadingZero = '0'
			}
			ret = ret.replaceAll('k+', leadingZero + hours)
		}
		if (ret.indexOf('K') != -1) {
			//Hour in am/pm (0-11)
			hours = '' + data.getHours() % 12
			var KK = /.*?(K+).*/.exec(dateFormat)[1];
			if (KK.length > 1 && hours.length == 1) {
				hours = '0' + hours;
			}
			ret = ret.replaceAll('K+', hours)
		}
		ret = $filter('date')(data, ret);
		if (ret.indexOf && ret.indexOf('E') != -1) {
			var last = ret.lastIndexOf('E');
			var first = ret.indexOf('E');
			var nrChars = last - first + 1;
			if (nrChars >= 1 && nrChars <= 3) ret = ret.substring(0, first - 1) + ' ' + ($locale.DATETIME_FORMATS.DAY[data.getDay()]).substring(0, 3) + ret.substring(last + 1, ret.length);
			if (nrChars >= 4 && nrChars <= 6) ret = ret.substring(0, first - 1) + ' ' + ($locale.DATETIME_FORMATS.DAY[data.getDay()]) + ret.substring(last + 1, ret.length);
		}

		return ret.trim? ret.trim() : ret
	}

	function getCurrency(servoyFormat) {
		var currency_symbols = [
			'\u20AC', // Euro
			'\u20A1', // Costa Rican Colón
			'\u00A3', // British Pound Sterling
			'\u20AA', // Israeli New Sheqel
			'\u20B9', // Indian Rupee
			'\u00A5', // Japanese Yen
			'\u20A9', // South Korean Won
			'\u20A6', // Nigerian Naira
			'\u20B1', // Philippine Peso
			'\u007A', // Polish Zloty
			'\u20B2', // Paraguayan Guarani
			'\u0E3F', //Thai Baht
			'\u20B4', // Ukrainian Hryvnia
			'\u20AB', // Vietnamese Dong
		];
		var currency = "";
		for (var i = 0; i < currency_symbols.length; i++)
			if (servoyFormat.indexOf(currency_symbols[i]) >= 0) {
				currency = currency_symbols[i];
			}
		return currency;
	}

	function getSelectedText(elementId) {
		var sel = null;
		var textarea = document.getElementById(elementId);
		if(textarea) {
			// code for IE
			if (document['selection']) {
				textarea.focus();
				sel = document['selection'].createRange().text;
			}
			else {
				// code for Mozilla
				var start = textarea['selectionStart'];
				var end = textarea['selectionEnd'];
				sel = textarea['value'].substring(start, end);
			}
		}
		return sel;
	}

	function numbersonlyForChar(keychar, decimal, decimalChar, groupingChar, currencyChar, percentChar, obj, mlength) {
		if (mlength > 0 && obj && obj[0] && obj[0].value) {
			var counter = 0;
			if (("0123456789").indexOf(keychar) != -1) counter++;
			var stringLength = obj[0].value.length;
			for (var i = 0; i < stringLength; i++) {
				if (("0123456789").indexOf(obj[0].value.charAt(i)) != -1) counter++;
			}
			var selectedTxt = getSelectedText(obj[0].id);
			if (selectedTxt) {
				// selection will get deleted/replaced by typed key
				for (var i = 0; i < selectedTxt.length; i++) {
					if (("0123456789").indexOf(selectedTxt.charAt(i)) != -1) counter--;
				}
			}
			if (counter > mlength) return false;
		}

		if ((("-0123456789").indexOf(keychar) > -1)) {
			return true;
		} else if (decimal && (keychar == decimalChar)) {
			return true;
		} else if (keychar == groupingChar) {
			return true;
		} else if (keychar == currencyChar) {
			return true;
		} else if (keychar == percentChar) {
			return true;
		}
		return false;				
	}
	
	function numbersonly(e, decimal, decimalChar, groupingChar, currencyChar, percentChar, obj, mlength) {
		var key;
		var keychar;

		if (window.event) {
			key = window.event['keyCode'];
		} else if (e) {
			key = e.which;
		} else {
			return true;
		}

		if ((key == null) || (key == 0) || (key == 8) || (key == 9) || (key == 13) || (key == 27) || (e.ctrlKey && key == 97) || (e.ctrlKey && key == 99) || (e.ctrlKey && key ==
				118) || (e.ctrlKey && key == 120)) { //added CTRL-A, X, C and V
			return true;
		}

		keychar = String.fromCharCode(key);
		return numbersonlyForChar(keychar, decimal, decimalChar, groupingChar, currencyChar, percentChar, obj, mlength);

	}

	function testForNumbersOnly(e, keyChar, vElement, vFindMode, vCheckNumbers, vSvyFormat) {
		if (!vFindMode && vCheckNumbers) {
			if ($utils.testEnterKey(e) && e.target.tagName.toUpperCase() == 'INPUT') {
				$(e.target).blur()
			} else if (vSvyFormat.type == "INTEGER") {
				var currentLanguageNumeralSymbols = numeral.localeData();
				
				if(keyChar == undefined) {
					return numbersonly(e, false, currentLanguageNumeralSymbols.delimiters.decimal, currentLanguageNumeralSymbols.delimiters.thousands, currentLanguageNumeralSymbols.currency
							.symbol,
							vSvyFormat.percent, vElement, vSvyFormat.maxLength);							
				}
				else {
					return numbersonlyForChar(keyChar, false, currentLanguageNumeralSymbols.delimiters.decimal, currentLanguageNumeralSymbols.delimiters.thousands, currentLanguageNumeralSymbols.currency
							.symbol,
							vSvyFormat.percent, vElement, vSvyFormat.maxLength);
				}
			} else if (vSvyFormat.type == "NUMBER" || ((vSvyFormat.type == "TEXT") && vSvyFormat.isNumberValidator)) {
				var currentLanguageNumeralSymbols = numeral.localeData();
				
				if(keyChar == undefined) {
					return numbersonly(e, true, currentLanguageNumeralSymbols.delimiters.decimal, currentLanguageNumeralSymbols.delimiters.thousands, currentLanguageNumeralSymbols.currency.symbol,
						vSvyFormat.percent, vElement, vSvyFormat.maxLength);							
				}
				else {
					return numbersonlyForChar(keyChar, true, currentLanguageNumeralSymbols.delimiters.decimal, currentLanguageNumeralSymbols.delimiters.thousands, currentLanguageNumeralSymbols.currency.symbol,
						vSvyFormat.percent, vElement, vSvyFormat.maxLength);														
				}
			}
		}
		return true;
	}

	return {

		format: function(data, servoyFormat, type) {
			if ((!servoyFormat) || (!type) || ((typeof data === "number") && isNaN(data))) return data;
			if (angular.isUndefined(data) || data === null) return "";
			if ((type == "NUMBER") || (type == "INTEGER")) {
				return formatNumbers(data, servoyFormat);
			} else if (type == "TEXT") {
				return formatText(data, servoyFormat);
			} else if (type == "DATETIME") {
				return formatDate(data, servoyFormat);
			}
			return data;
		},


		unformat: function(data, servoyFormat, type, currentValue) {
			if ((!servoyFormat) || (!type) || (!data && data != 0)) return data;
			if ((type == "NUMBER") || (type == "INTEGER")) {
				return unformatNumbers(data, servoyFormat);
			} else if (type == "TEXT") {
				return data;
			} else if (type == "DATETIME") {
				if ("" === data ) return null;
				// some compatibility issues, see http://momentjs.com/docs/ and http://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html
				servoyFormat = servoyFormat.replaceAll('d', 'D');
				servoyFormat = servoyFormat.replaceAll('y', 'Y');
				// use moment.js from calendar component
				var d = moment(data, servoyFormat,true).toDate();
				// if format has not year/month/day use the one from the current model value
				// because moment will just use current date
				if(currentValue) {
					if(servoyFormat.indexOf('Y') == -1) {
						d.setFullYear(currentValue.getFullYear());
					}
					if(servoyFormat.indexOf('M') == -1) {
						d.setMonth(currentValue.getMonth());
					}
					if(servoyFormat.indexOf('D') == -1) {
						d.setDate(currentValue.getDate());
					}
				}
				return d;
			}
			return data;
		},
		createFormatState: function(element, $scope, ngModelController, checkNumbers, newValue) {
			var svyFormat = newValue;

			if (svyFormat.maxLength) {
				element.attr('maxlength', svyFormat.maxLength);
			}
			var callChangeOnBlur = null;
			var enterKeyCheck = function(e) {
				if (callChangeOnBlur && $utils.testEnterKey(e)) {
					callChangeOnBlur();
				}
			}
			var formatUtils = this;

			function viewToModel(viewValue) {
				var data = viewValue
				if (!$scope.model.findmode) {
					var format = null;
					var type = svyFormat ? svyFormat.type : null;
					format = svyFormat.display ? svyFormat.display : svyFormat.edit
					if (element.is(":focus") && svyFormat.edit && !svyFormat.isMask) format = svyFormat.edit
					try {
						var data = formatUtils.unformat(viewValue, format, type, ngModelController.$modelValue);
					} catch (e) {
						console.log(e)
							//TODO set error state
							//ngModelController.$error ..
					}
					if (svyFormat.type == "TEXT" && (svyFormat.uppercase || svyFormat.lowercase)) {
						var currentData = data;
						if (svyFormat.uppercase) data = data.toUpperCase();
						else if (svyFormat.lowercase) data = data.toLowerCase();
						// if the data is really changed then this pushes it back to the interface
						// problem is that then a dom onchange event will not happen..
						// we must fire a change event then in the onblur.
						if (currentData !== data) {
							var caret = element[0].selectionEnd;
							if (!callChangeOnBlur) {
								callChangeOnBlur = function() {
									element.change();
									element.off("blur", callChangeOnBlur);
									element.off("keydown", enterKeyCheck);
								}
								element.on("blur", callChangeOnBlur);
								element.on("keydown", enterKeyCheck);
							}
							ngModelController.$viewValue = data;
							ngModelController.$render();
							element[0].selectionStart = caret;
							element[0].selectionEnd = caret;
						} else {
							// this will be a change that will be recorded by the dom element itself
							if (callChangeOnBlur) {
								element.off("blur", callChangeOnBlur);
								element.off("keydown", enterKeyCheck);
								callChangeOnBlur = null;
							}
						}
					}
					if (svyFormat.type == "TEXT" && svyFormat.isRaw && svyFormat.isMask) {
						if (data && format && data.length === format.length){
							var ret = ''
							for (var i = 0; i < format.length; i++) {
								switch (format[i]) {
									case 'U': 
									case 'L':
									case 'A': 
									case '?':
									case '*':
									case 'H':
									case '#':
										ret += data[i]
										break;
									default:
										// ignore literal characters
										break;
								}
							}
							return ret;
						}
						
					}
				}
				return data; //converted
			}

			function modelToView(modelValue) {
				var data = modelValue;
				if (svyFormat && !$scope.model.findmode) {
					var format = null;
					var type = svyFormat ? svyFormat.type : null;
					format = svyFormat.display ? svyFormat.display : svyFormat.edit
					if (svyFormat.edit && element.is(":focus")) format = svyFormat.edit
					try {
						ngModelController.$setValidity("", true);
						data = formatUtils.format(modelValue, format, type);
					} catch (e) {
						console.log(e)
						ngModelController.$setValidity("", false);
							//TODO set error state
							//ngModelController.$error ..
					}
					if (data && svyFormat.type == "TEXT") {
						if (svyFormat.uppercase) data = data.toUpperCase();
						else if (svyFormat.lowercase) data = data.toLowerCase();
					}
				}
				return data; //converted
			}
			
			function keypress(e) {
			    isKeyPressEventFired = true;
				if(e.target.type.toUpperCase() == 'NUMBER') 
				    return e.target.maxLength > e.target.value.length && testForNumbersOnly(e, null, element, $scope.model.findmode, checkNumbers, svyFormat);
				else 
				    return testForNumbersOnly(e, null, element, $scope.model.findmode, checkNumbers, svyFormat);
                }

			function focus(e) {
				if(e.target.tagName.toUpperCase() == 'INPUT') {
					oldInputValue = element.val().substring(0, e.target.maxLength);
				}
				if (!$scope.model.findmode) {
					if (svyFormat.edit && svyFormat.isMask) {
						var settings = {};
						settings['placeholder'] = svyFormat.placeHolder ? svyFormat.placeHolder : " ";
						if (svyFormat.allowedCharacters)
							settings['allowedCharacters'] = svyFormat.allowedCharacters;

						element.mask(svyFormat.edit, settings);
					} else if (svyFormat.edit) {
						$scope.$evalAsync(function() {
							var caret = element[0].selectionEnd;	
							ngModelController.$setViewValue(modelToView(ngModelController.$modelValue))
							ngModelController.$render();
							// if we had a selectionEnd, try to set it back
							if(caret) {
								element[0].selectionStart = caret;
								element[0].selectionEnd = caret;
							}
						})
					}
				}
			}

			var cancelNextBlur = 0;

			function change() {
				cancelNextBlur += 1; // the browser just called change(), we do not want the next blur() to do anything
				if (!$scope.model.findmode) {
					if (svyFormat.edit && svyFormat.isMask) element.unmask();
					//blur needs this because we need to change to the display format even if the value didn't change
					$scope.$evalAsync(function() {
						ngModelController.$setViewValue(modelToView(ngModelController.$modelValue))
						ngModelController.$render();
					})
				}
			}

			function blur() {
				//call change so that the (view)formatting is applied even if the data was not changed - i.e. change() was not called by the browser
				if (cancelNextBlur) cancelNextBlur = 0;
				else {
					cancelNextBlur = -1; //so that the next change does not count as a change event from the browser
					change();
				}
			}

			var isKeyPressEventFired = false;
			var oldInputValue;
			
			function input(e) {
				if(!isKeyPressEventFired && e.target.tagName.toUpperCase() == 'INPUT') {
					var currentValue = element.val();
				    // get inserted chars
				    var inserted = findDelta(currentValue, oldInputValue);
				    // get removed chars
				    var removed = findDelta(oldInputValue, currentValue);
				    // determine if user pasted content
				    var pasted = inserted.length > 1 || (!inserted && !removed);
					
				    if(!pasted && !removed) {
				    	if(!testForNumbersOnly(e, inserted, element, $scope.model.findmode, checkNumbers, svyFormat)) {
				    		currentValue = oldInputValue; 
				    		element.val(currentValue);
				    	}
			        }
				    if(e.target.type.toUpperCase() == 'NUMBER'){
    				    if(currentValue.length > e.target.maxLength){
                            currentValue = currentValue.substring(0, e.target.maxLength)
                            element.val(currentValue);
                            let b =  formatNumbers(currentValue, e.target.type);
    				    }
    				    else oldInputValue = currentValue;
				    }
				    else oldInputValue = currentValue;
	                isKeyPressEventFired = false;
				}
				if(isKeyPressEventFired)
				    isKeyPressEventFired = false;

			}

			function findDelta(value, prevValue) {
				var delta = '';
				for (var i = 0; i < value.length; i++) {
					var str = value.substr(0, i) + value.substr(i + value.length - prevValue.length);
					if (str === prevValue) {
						delta = value.substr(i, value.length - prevValue.length);
					}
				}
				return delta;
			}			

			function register() {
				element.on('focus', focus)
				element.on('blur', blur)
				element.on('change', change)
				element.on('keypress', keypress)
				element.on('input', input)

				//convert data from view format to model format
				ngModelController.$parsers.push(viewToModel);
				//convert data from model format to view format
				ngModelController.$formatters.push(modelToView);
			}

			function unregister() {
				element.off('focus', focus)
				element.off('blur', blur)

				element.off('keypress', keypress)
				element.off('input', input)
				element.off('change', change)				

				var i = ngModelController.$parsers.indexOf(viewToModel);
				if (i != -1) {
					ngModelController.$parsers.splice(i, 1);
				}
				i = ngModelController.$formatters.indexOf(modelToView);
				if (i != -1) {
					ngModelController.$formatters.splice(i, 1);
				}
			}

			function applyValue() {
				var formatters = ngModelController.$formatters;
				var idx = formatters.length;
				var viewValue = ngModelController.$modelValue;
				while (idx--) {
					viewValue = formatters[idx](viewValue);
				}
				ngModelController.$viewValue = viewValue;
				ngModelController.$render();
			}
			if (svyFormat) {
				register();
				applyValue();
			}

			return function(newValue) {
				if (newValue && !svyFormat) {
					register();
				} else if (!newValue && svyFormat) {
					unregister();
				}
				svyFormat = newValue;
				if (svyFormat && svyFormat.maxLength) {
					element.attr('maxlength', svyFormat.maxLength);
				}
				applyValue();

			}
		},
		testForNumbersOnly: function (e, keyChar, vElement, vFindMode, vCheckNumbers, vSvyFormat) {
			return testForNumbersOnly(e, keyChar, vElement, vFindMode, vCheckNumbers, vSvyFormat);
		}
	}
}]).filter('formatFilter', function($formatterUtils) { /* this filter is used for display only*/
	return function(input, servoyFormat, type, fullFormat) {
		var ret = input;
		try {
			// TODO apply servoy default formatting from properties file here
			if (input instanceof Date && !servoyFormat) {
				servoyFormat = 'MM/dd/yyyy hh:mm aa';
				type = 'DATETIME';
			}

			// commented out because in case one uses a INTEGER dp + format + DB-col(INTEGER)-valuelist we might receive strings
			// instead of ints (display values of DB valuelist do .toString() on server); don't know why that is; $formatterUtils.format can handle that correctly though and it should work the same here (portal vs. extra table)
			// if (((typeof input === 'string') || (input instanceof String)) && type !== "TEXT") return input;

			ret = $formatterUtils.format(input, servoyFormat, type);
			if (ret && fullFormat && type == 'TEXT' && fullFormat.uppercase)
			{
				ret = ret.toUpperCase();
			}
			if (ret && fullFormat && type == 'TEXT' && fullFormat.lowercase)
			{
				ret = ret.toLowerCase();
			}
				
		} catch (e) {
			console.log(e);
			//TODO decide what to do when string doesn't correspod to format
		}
		return ret;
	};
}).directive("svyFormat", ["$formatterUtils", "$parse", "$utils", function($formatterUtils, $parse) {
	return {
		require: 'ngModel',
		priority: 1,
		link: function($scope, element, attrs, ngModelController) {
			var formatState = null;
			$scope.$watch(attrs['svyFormat'], function(newVal, oldVal) {
				if (newVal) {
					if (formatState)
						formatState(newVal);
					else formatState = $formatterUtils.createFormatState(element, $scope, ngModelController, !attrs['typeahead'], newVal);
				}
			})
		}
	}
}]);
