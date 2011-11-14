/**
 * Numpad decimal separator plugin for jQuery.
 * 
 * With this jQuery plugin you can configure what character to use for the numpad decimal separator.
 * 
 * @author Gert Nuyens
 * @version 1.1.2 
 * 
 * Dual licensed under the MIT (MIT-LICENSE.txt) 
 * 	or GPL Version 2 licenses (GPL-LICENSE.txt).
 * 
 * Dependencies: - jQuery (http://jquery.com)
 * 
 * jQuery extension functions: 
 * - numpadDecSeparator this function has three options: 
 * 	1) separator: the separator to use when a user presses the numpad decimal separator key. 
 * 		Only use this when the option useRegionalSettings is false. 
 * 		Defaults to ','. You can also use one of the predifined variables.
 * 	2) useRegionalSettings: when a user presses the numpad decimal separator key it will use 
 * 		the regional options of the operating system. 
 * 		This only works in firefox and ie!!! 
 * 		Defaults to false.  
 * 	3) predefinedVariables: the default predifined variables are:
 * 		- SPACE => will output space
 * 		- COMMA => will output comma
 * - numpadDecSeparator('unbind') this function will unbind the numpadDecSeparator
 * - numpadDecSeparator('version') static function which returns the current version of the plugin
 * - numpadDecSeparator('mergeDefaults') static function:
 * 		with this function you can override some or all the default options, the provided options will be
 * 		merged with the default options
 * 
 * Examples: 
 * $(".amount").numpadDecSeparator();
 * $(".amount").numpadDecSeparator({separator: ','}); this is the same as $(".amount").numpadDecSeparator({separator: 'COMMA'});
 * $(".amount").numpadDecSeparator({separator: ' '}); this is the same as $(".amount").numpadDecSeparator({separator: 'SPACE'});
 * $(".amount").numpadDecSeparator({useRegionalSettings: true});
 * 
 * $(".amount").numpadDecSeparator('unbind');
 * 
 * $.fn.numpadDecSeparator('version');
 * 
 * $.fn.numpadDecSeparator('mergeDefaults', {separator: "SPACE"});
 * $.fn.numpadDecSeparator('mergeDefaults', {useRegionalSettings: true});
 * $.fn.numpadDecSeparator('mergeDefaults', {predefinedVariables: {APOSTROPHE: "'"}});
 * var newDefaults = {
 *	separator : ' ',
 *	useRegionalSettings : true,
 *	predefinedVariables: {SPACE: " "}
 * };
 * $.fn.numpadDecSeparator('mergeDefaults', newDefaults);
 **/
(function($) {
	var methods = {
		init : function(options) {
			return this.each(function() {
				var keydownCode = '', $this = $(this), data = $this
						.data('numpadDecSeparator');
				if (!$this.attr("readonly") && !data) {
					$(this).data('numpadDecSeparator', {
						target : $this
					});
					$this.bind('keydown.numpadDecSeparator', function(event) {
						keydownCode = event.keyCode;
					}).bind(
							'keypress.numpadDecSeparator',
							function(event) {
								if (_numericPadPeriodPressed(keydownCode)
										&& !event.shiftKey && !event.ctrlKey
										&& !event.altKey) {
									_replaceSelectedVal(this,
											_getSeparator(options));
									event.preventDefault();
								}
							});
				}
			});
		},
		unbind : function() {
			return this.each(function() {
				var $this = $(this), data = $this.data('numpadDecSeparator');
				$this.unbind('.numpadDecSeparator');
				$this.removeData('numpadDecSeparator');
			});
		},
		version : function() {
			return "1.1.2";
		},
		mergeDefaults : function(defaultsToMerge) {
			$.extend($.fn.numpadDecSeparator.defaults, defaultsToMerge);
		}
	};
	$.fn.numpadDecSeparator = function(methodOrOptions) {
		var settings;
		// Method calling logic
		if (methods[methodOrOptions]) {
			return methods[methodOrOptions].apply(this, Array.prototype.slice
					.call(arguments, 1));
		} else if (typeof methodOrOptions === 'object' || !methodOrOptions) {
			if (methodOrOptions) {
				settings = $.extend({}, $.fn.numpadDecSeparator.defaults, methodOrOptions);
			}
			return methods.init.call(this, settings ? settings : $.fn.numpadDecSeparator.defaults);
		} else {
			$.error('Method ' + methodOrOptions
					+ ' does not exist on jQuery.numpadDecSeparator');
		}
	};
	
	$.fn.numpadDecSeparator.defaults = {
		separator : ',',
		useRegionalSettings : false,
		predefinedVariables: {SPACE: " ", COMMA: ","}
	};
	
	function _decimalSeparator() {
		var n = 1.1;
		n = n.toLocaleString().substring(1, 2);
		return n;
	}
	function _numericPadPeriodPressed(keydownCode) {
		return $.browser.opera ? 78 == keydownCode : 110 == keydownCode;
	}
	function _getSeparator(settings) {
		return settings.useRegionalSettings ? _decimalSeparator()
				: _determineSeparator(settings);
	}
	
	function _determineSeparator(settings){
		return settings.predefinedVariables[settings.separator] ?
				settings.predefinedVariables[settings.separator] : settings.separator;
	}
	function _replaceSelectedVal(input, text) {
		if ('selectionStart' in input) {
			var start = input.selectionStart + 1;
			input.value = input.value.substr(0, input.selectionStart)
					+ text
					+ input.value
							.substr(input.selectionEnd, input.value.length);
			input.selectionStart = start;
			input.selectionEnd = start;
			input.focus();
		} else if (document.selection) {
			input.focus();
			var sel = document.selection.createRange();
			sel.text = text;
			// Move selection start and end to 0 position
			sel.moveStart('character', -input.value.length);

			// Move selection start and end to desired position
			sel.moveStart('character', sel.text.length);
			sel.moveEnd('character', 0);
			sel.select();
		} else {
			input.value += text;
		}
	}
})(jQuery);