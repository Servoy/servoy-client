/// <reference path="../../../lib/jquery-1.2.6.js" />
/*
	Masked Input plugin for jQuery
	Copyright (c) 2007-2009 Josh Bush (digitalbush.com)
	Licensed under the MIT license (http://digitalbush.com/projects/masked-input-plugin/#license) 
	Version: 1.2.2 (03/09/2009 22:39:06)
*/
(function($) {
	
	function getPasteEvent() {
	    var el = document.createElement('input'),
	        name = 'onpaste';
	    el.setAttribute(name, '');
	    return (typeof el[name] === 'function')?'paste':'input'; 
	}
	
	var pasteEventName = getPasteEvent() + ".mask";
	var iPhone = (window.orientation != undefined);

	$.mask = {
		//Predefined character definitions
		definitions: {
			'#': "[0-9]",
			'U': "[A-Z]",
			'L': "[a-z]",
			'A': "[A-Za-z0-9]",
			'?': "[A-Za-z]",
			'*': ".",
			'H': "[A-F0-9]"
		},
		converters: {
			'U': function(c){return c.toUpperCase()},
			'L': function(c){return c.toLowerCase()},
			'H': function(c){return c.toUpperCase()}
		}
	};

	$.fn.extend({
		//Helper Function for Caret positioning
		caret: function(begin, end) {
			if (this.length == 0) return;
			if (typeof begin == 'number') {
				end = (typeof end == 'number') ? end : begin;
				return this.each(function() {
					if (this.setSelectionRange) {
						this.focus();
						this.setSelectionRange(begin, end);
					} else if (this.createTextRange) {
						var range = this.createTextRange();
						range.collapse(true);
						range.moveEnd('character', end);
						range.moveStart('character', begin);
						range.select();
					}
				});
			} else {
				if (this[0].setSelectionRange) {
					begin = this[0].selectionStart;
					end = this[0].selectionEnd;
				} else if (document.selection && document.selection.createRange) {
					var range = document.selection.createRange();
					begin = 0 - range.duplicate().moveStart('character', -100000);
					end = begin + range.text.length;
				}
				return { begin: begin, end: end };
			}
		},
		unmask: function() { return this.trigger("unmask"); },
		mask: function(mask, settings) {
			if (!mask && this.length > 0) {
				var input = $(this[0]);
				var tests = input.data("tests");
				return $.map(input.data("buffer"), function(c, i) {
					return tests[i] ? c : null;
				}).join('');
			}
			settings = $.extend({
				placeholder: "_",
				completed: null
			}, settings);

			var defs = $.mask.definitions;
			var tests = [];
			var converts = $.mask.converters;
			var converters = [];
			var partialPosition = mask.length;
			var firstNonMaskPos = null;
			var len = mask.length;
			
			var skipNextMask = false;
			var filteredMask = '';

			$.each(mask.split(""), function(i, c) {
//				if (c == '?') {
//					len--;
//					partialPosition = i;
//				} else 
                if (!skipNextMask && c == "'") {
                	skipNextMask = true;
                	len--;
                	partialPosition--;
                }
                else {
					if (!skipNextMask && defs[c]) {
						if (c == '*' && settings.allowedCharacters) {
							tests.push(new RegExp('[' + settings.allowedCharacters + ']'));
						}
						else {
							tests.push(new RegExp(defs[c]));
						}
						if(firstNonMaskPos==null)
							firstNonMaskPos =  tests.length - 1;
					} else {
						tests.push(null);
						skipNextMask = false;
					}
					converters.push(converts[c]);
					filteredMask += c;
				}
			});

			return this.each(function() {
				var input = $(this);
				var buffer = $.map(filteredMask.split(""), function(c, i) { return tests[i] ? getPlaceHolder(i) : c });
				var ignore = false;  			//Variable for ignoring control keys
				var focusText = input.val();

				input.data("buffer", buffer).data("tests", tests);

				function seekNext(pos) {
					while (++pos <= len && !tests[pos]);
					return pos;
				};

				function seekPrevious(pos) {
					while (--pos >= 0 && !tests[pos]);
					return pos;
				};
				
				function getPlaceHolder(i)
				{
					return settings.placeholder.length > 1?settings.placeholder.charAt(i):settings.placeholder;
				};

				function shiftL(pos) {
					while (!tests[pos] && --pos >= 0);
					for (var i = pos; i < len; i++) {
						if (tests[i]) {
							buffer[i] = getPlaceHolder(i);
							var j = seekNext(i);
							if (j < len && tests[i].test(buffer[j])) {
								buffer[i] = buffer[j];
							} else
								break;
						}
					}
					writeBuffer();
					input.caret(Math.max(firstNonMaskPos, pos));
				};
				
				function clear(pos,caretAddition) {
					while (!tests[pos] && --pos >= 0);
					if (tests[pos]) {
						buffer[pos] = getPlaceHolder(pos);
					}
					writeBuffer();
					if (caretAddition != 0) {
						var nextPos = pos + caretAddition;
						while ( nextPos >=0 && nextPos < len ) {
							if (tests[nextPos]) {
								pos = nextPos;
								break;
							}
							nextPos = nextPos + caretAddition;
						}
					}
					input.caret(Math.max(firstNonMaskPos, pos));
				};

				function shiftR(pos) {
					for (var i = pos; i < len; i++) {
						c = getPlaceHolder(i);
						if (tests[i]) {
							var j = seekNext(i);
							var t = buffer[i];
							buffer[i] = c;
							if (j < len && tests[j].test(t))
								c = t;
							else
								break;
						}
					}
				};

				function keydownEvent(e) {
					var pos = $(this).caret();
					var k = e.keyCode;
					ignore = (k < 16 || (k > 16 && k < 32) || (k > 32 && k < 41));

					if (k == 37) {
						var nextValidChar = seekPrevious(pos.begin);
						if (nextValidChar != pos.begin) {
							input.caret(nextValidChar);
							return false;
						}
						return;
					} else if (k == 39) {
						var nextValidChar = seekNext(pos.begin);
						if (nextValidChar != pos.begin) {
							input.caret(nextValidChar);
							return false;
						}
						return;
					}
					var nextValidChar = seekNext(pos.begin - 1) - pos.begin;
					if (nextValidChar > 0) {
						pos.begin += nextValidChar;
						pos.end += nextValidChar;
					}
					
					//backspace, delete, and escape get special treatment
					if (k == 8 || k == 46 || (iPhone && k == 127)) {
						if (pos.begin == pos.end) {
							clear(pos.begin + (k == 46 ? 0 : -1),(k == 46 ? 1 : 0));
						} else {
							clearBuffer(pos.begin, pos.end);
							writeBuffer();
							input.caret(Math.max(firstNonMaskPos, pos.begin));
						}
						return false;
					} else if (k == 27) {//escape
						input.val(focusText);
						input.caret(0, checkVal());
						return false;
					} else if (pos.begin != pos.end && !ignore) {
						clearBuffer(pos.begin, pos.end);
					}
				};

				function keypressEvent(e) {
					if (ignore) {
						ignore = false;
						//Fixes Mac FF bug on backspace
						return (e.keyCode == 8) ? false : null;
					}
					e = e || window.event;
					var k = e.charCode || e.keyCode || e.which;
					var pos = $(this).caret();

					if (e.ctrlKey || e.altKey || e.metaKey) {//Ignore
						return true;
					} else if ((k >= 32 && k <= 125) || k > 186) {//typeable characters
						var p = seekNext(pos.begin - 1);
						if (p < len) {
							var c = String.fromCharCode(k);
							if (converters[p]) {
								c = converters[p](c);
							}
							if (tests[p].test(c)) {
//								shiftR(p);
								buffer[p] = c;
								writeBuffer();
								var next = seekNext(p);
								$(this).caret(next);
								if (settings.completed && next == len)
									settings.completed.call(input);
							}
						}
					}
					return false;
				};

				function clearBuffer(start, end) {
					for (var i = start; i < end && i < len; i++) {
						if (tests[i])
							buffer[i] = getPlaceHolder(i);
					}
				};

				function writeBuffer() { return input.val(buffer.join('')).val(); };

				function checkVal(allow) {
					//try to place characters where they belong
					var test = input.val();
					var lastMatch = -1;
					var firstError = -1;
					for (var i = 0, pos = 0; i < len; i++) {
						if (tests[i]) {
							buffer[i] = getPlaceHolder(i);
							while (pos++ < test.length) {
								var c = test.charAt(pos - 1);
								// if the char is the place holder then dont shift..
								if (c == buffer[i]) {
								   if (firstError == -1) firstError = i;
								   break;
								}
								if (tests[i].test(c)) {
									buffer[i] = c;
									lastMatch = i;
									break;
								}
							}
							if (pos > test.length)
								break;
						} else if (buffer[i] == test.charAt(pos) && i!=partialPosition) {
							pos++;
//							lastMatch = i;
						} 
					}
					if (!allow && lastMatch + 1 < partialPosition) {
						input.val("");
						clearBuffer(0, len);
					} else if (allow && lastMatch == -1) {
						input.val("");
						clearBuffer(0, len);
					} else if (allow || lastMatch + 1 >= partialPosition) {
						writeBuffer();
						if (!allow) input.val(input.val().substring(0, lastMatch + 1));
					}
					return firstError != -1?firstError:(partialPosition ? i : firstNonMaskPos);
				};

				if (!input.attr("readonly"))
					input
					.one("unmask", function() {
						input
							.unbind(".mask")
							.removeData("buffer")
							.removeData("tests");
					})
					.bind("focus.mask", function() {
						focusText = input.val();
						var caret = input.caret();
						var pos = checkVal(true);
						writeBuffer();
						setTimeout(function() {
							if (pos != filteredMask.length) {
								input.caret(pos);
							}
							else input.caret(caret.begin);
						}, 0);
					})
					.bind("blur.mask", function() {
						checkVal(true);
						if (input.val() != focusText)
							input.change();
					})
					.bind("keydown.mask", keydownEvent)
					.bind("keypress.mask", keypressEvent)
					.bind(pasteEventName, function() {
						setTimeout(function() { input.caret(checkVal(true)); }, 0);
					});

				checkVal(true); //Perform initial check for existing values
			});
		}
	});
})(jQuery);