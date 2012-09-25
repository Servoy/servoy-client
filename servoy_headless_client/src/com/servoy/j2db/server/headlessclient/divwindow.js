// this code is meant to extend modal.js from Wicket ModalWindow so as to allow non-modal windows

if (typeof(Wicket.Window) == "undefined") {
	// this is not good... Wicket.Window should already be defined in order for this to work!
	Wicket.Log.error("Cannot load DivWindow js because ModalWindow js is not yet loaded!");
	throw new Error("Cannot load DivWindow js because ModalWindow js is not yet loaded!");   
}

if (Wicket.Object.extendClass == null) {
	Wicket.Object.extendClass = function(subclass, supercclass, overrides) {
		if (! supercclass || ! subclass) {
			Wicket.Log.error("Class extend failed, please check that all dependencies are included.");
			throw new Error("Class extend failed, please check that all dependencies are included.");
		}

		var F = function() {};
		F.prototype = supercclass.prototype;
		subclass.prototype = new F();
		subclass.prototype._super = supercclass.prototype;

		if (overrides) {
			for (var i in overrides) {
				subclass.prototype[i] = overrides[i];
			}
		}
	}
}

Wicket.DivWindow = Wicket.Class.create();

Wicket.Object.extendClass(Wicket.DivWindow, Wicket.Window, {

	parentModalWindow: null,
	closed: false,
	onResizeTimer: null,
	onMoveTimer: null,

	// override
	initialize: function(settings, windowId) {
		this._super.initialize.call(this, settings);
		this.windowId = windowId;
		this.settings = Wicket.Object.extend({
			modal: true, /* false for non-modal window */
			storeBounds: true, /* false if the bounds shouldn't be stored */
			initialX: -1,
			initialY: -1,
			onMove: function() { }, /* called when window is moved */
			onResize: function() { }, /* called when window is resized */
			boundEventsDelay: 300 /* if <= 0, then all drag operations on window bounds will update imediately; if > 0 bound events will be sent after this timeout in ms  */
		}, this.settings);
	},
	
	moving: function() {
		this._super.moving.call(this);
		// update server side location
		if (this.onMoveTimer) clearTimeout(this.onMoveTimer);
		if (this.settings.boundEventsDelay > 0) {
			var fun = function() {
			  this.settings.onMove(this.left_, this.top_);
			}
			fun = fun.bind(this);
		 	this.onMoveTimer = setTimeout(fun, this.settings.boundEventsDelay);
		}
		else {
			this.onMoveTimer = null;
			this.settings.onMove(this.left_, this.top_);
		}
	},
	
	resizing: function() {	
		this._super.resizing.call(this);
		// update server side size
		if (this.onResizeTimer) clearTimeout(this.onResizeTimer);
		if (this.settings.boundEventsDelay > 0) {
		   var fun = function() {
			  this.settings.onResize(this.width, this.height);
		   }
		   fun = fun.bind(this);
		   this.onResizeTimer = setTimeout(fun, this.settings.boundEventsDelay);
		}
		else {
			this.onResizeTimer = null;
			this.settings.onResize(this.width, this.height);
		}
	},
	
	show: function() {
		this._super.show.call(this);
		
		// initialize bounds
		this.refreshBounds(true);
	},
	
	refreshBounds: function(internalEvent) {
		this.width = parseInt(this.window.style.width, 10);
		this.height = parseInt(this.content.style.height, 10);
		this.left_ = parseInt(this.window.style.left, 10);
		this.top_ =  parseInt(this.window.style.top, 10);
		
		this.settings.onMove(this.left_, this.top_, internalEvent);
		this.settings.onResize(this.width, this.height, internalEvent);
	},
	
	createDOM: function() {
		this._super.createDOM.call(this);
		
		// tofront for multiple non-modal windows
		this.classElement.onmousedown = this.caption.onmousedown = this.top.onmousedown = this.topLeft.onmousedown =
		this.topRight.onmousedown = this.bottomLeft.onmousedown = this.bottomRight.onmousedown = this.bottom.onmousedown =
		this.left.onmousedown = this.right.onmousedown = function() {
			this.toFront();
		}.bind(this);
	},
	
	// override
	createMask: function() {
		if (this.settings.modal) {
			if (this.settings.mask == "transparent")
				this.mask = new Wicket.DivWindow.Mask(true);
			else if (this.settings.mask == "semi-transparent")
				this.mask = new Wicket.DivWindow.Mask(false);
						
			if (typeof(this.mask) != "undefined") {
				this.mask.show();
			}		
		}
	},

	// override
	destroyMask: function() {
		if (this.settings.modal) {
			this.mask.hide();	
			this.mask = null;
		}
	},

	// override
	adjustOpenWindowZIndexesOnShow: function() {
		if (this.settings.modal) {
			this.window.style.zIndex = Wicket.Window.Mask.zIndex + 1;
		
			// move all non-modal windows with the same parent under mask
			var maxZIndexDelta = 0;
			for (var w in Wicket.DivWindow.openWindows) {
				var win = Wicket.DivWindow.openWindows[w];
				if (!win.settings.modal && win.parentModalWindow == this.parentModalWindow) {
					if (maxZIndexDelta < (parseInt(win.window.style.zIndex) - Wicket.Window.Mask.zIndex)) {
						maxZIndexDelta = parseInt(win.window.style.zIndex) - Wicket.Window.Mask.zIndex;
					}
				}
			}
			
			// slide down on Z axis all windows that were previously under the mask to make room for the new ones
			for (var w in Wicket.DivWindow.openWindows) {
				var win = Wicket.DivWindow.openWindows[w];
				if (parseInt(win.window.style.zIndex) < Wicket.Window.Mask.zIndex) {
					win.window.style.zIndex = parseInt(win.window.style.zIndex) - maxZIndexDelta - 1;
				}
			}
			
			if (maxZIndexDelta > 0) {
				// move these non-modal windows under the mask
				for (var w in Wicket.DivWindow.openWindows) {
					var win = Wicket.DivWindow.openWindows[w];
					if (!win.settings.modal && win.parentModalWindow == this.parentModalWindow) {
						win.window.style.zIndex = parseInt(win.window.style.zIndex) - maxZIndexDelta - 1;
					}
				}
			}

			// move parent modal window under mask
			if (this.parentModalWindow != null) {
				this.parentModalWindow.window.style.zIndex = Wicket.Window.Mask.zIndex - maxZIndexDelta; // non-active modal window should be below it's child non-modal windows
			}
		} else {
			// show on top of other possible non-modal windows
			var nextZIndex = Wicket.Window.Mask.zIndex + 2;
			for (var w in Wicket.DivWindow.openWindows) {
				var win = Wicket.DivWindow.openWindows[w];
				if (!win.settings.modal && win.parentModalWindow == this.parentModalWindow) {
					if (nextZIndex <= parseInt(win.window.style.zIndex)) {
						nextZIndex = parseInt(win.window.style.zIndex) + 1;
					}
				}
			}
			this.window.style.zIndex = nextZIndex;
		}
	},

	// override
	adjustOpenWindowsStatusOnShow: function() {
	    this._super.adjustOpenWindowsStatusOnShow.call(this);
		this.parentModalWindow = Wicket.DivWindow.currentModalWindow;
		if (this.settings.modal) {
			Wicket.DivWindow.currentModalWindow = this;
		}

		Wicket.DivWindow.openWindows[this.windowId] = this;
	},

	// override
	canCloseInternal: function() {
		return true; // if there are other windows on-top, those will be closed as well
	},
	
	// it might be useful to allow other AJAX scripts to execute before actually closing the window
	// because those scripts might be executing on a response from the window itself, so it would be wrong do unload the window while scripts still execute
	closeAfterTimeout: function(w, timeout)
	{
		if (!w) w = window;
		if (w === window) {
			// when executing in root window
			window.setTimeout(this.close.bind(this), timeout); // force param = undefined
		} else {
			// when executing in this window's (that is about to be closed) request, postpone then move call to root window, in order to generate the closed callback correctly in Opera
			w.setTimeout(this.closeAfterTimeout.bind(this, window, 1), timeout);
		}
	},

	// override
	close: function(force) {
		// make a copy of open windows, cause we will iterate on it while possibly closing some windows
		var allWindows = new Array();
		for (var w in Wicket.DivWindow.openWindows) {
			allWindows.push(Wicket.DivWindow.openWindows[w]);
		}
		this.closeInternal(force, allWindows);
	},

	closeInternal: function(force, allWindows) {
		if (this.closed) return;
		// close child windows first if necessary (first modals then non-modals)
		if (this.settings.modal) {
			for (var i = 0; i < allWindows.length; i++) {
				var w = allWindows[i];
				if (!w.closed && w.settings.modal && w.parentModalWindow == this) {
					w.closeInternal(false, allWindows); // try nicely (to trigger onClose)
					if (!w.closed) w.closeInternal(true, allWindows); // force then
				}
			}
			for (var i = 0; i < allWindows.length; i++) {
				var w = allWindows[i];
				if (!w.closed && !w.settings.modal && w.parentModalWindow == this) {
					w.closeInternal(false, allWindows); // try nicely (to trigger onClose)
					if (!w.closed) w.closeInternal(true, allWindows); // force then
				}
			}
		}

		// do close the window
		this._super.close.call(this, force);
	},
	
	setPosition: function(x, y, width, height) {
		this.window.style.left = x;
		this.window.style.top = y;
		this.window.style.width = width;
		this.content.style.height = height;
		this.savePosition(x, y, width, height);
		
		this.refreshBounds();
	},
	
	savePositionAs: function(x, y, width, height) {
		if (typeof(this.settings.cookieId) != "undefined" &&  this.settings.cookieId != null) {
			this.findPositionString(true);
			
			if (this.settings.storeBounds) {
				if (cookie == null || cookie.length == 0)
					cookie = "";
				else
					cookie = cookie + "|";
			
				var cookie = this.settings.cookieId;
				cookie += "::";
			
				cookie += x + ",";
				cookie += y;
				if (this.settings.resizable) {
					cookie += "," + width + ",";
					cookie += height;
				}
					
				var rest = Wicket.Cookie.get(this.cookieKey);
				if (rest != null) {
					cookie += "|" + rest;
				}
				Wicket.Cookie.set(this.cookieKey, cookie, this.cookieExp);
			}
		};
	},
	
	deletePosition: function() {
		if (typeof(this.settings.cookieId) != "undefined" &&  this.settings.cookieId != null) {
			this.findPositionString(true);
		};
	},
	
	loadPosition: function() {
		if (this.settings.initialX != -1 && this.settings.initialY != -1) {
			this.window.style.left = this.settings.initialX + "px";
			this.window.style.top = this.settings.initialY + "px";
			this.window.style.width = this.settings.width + (this.settings.resizable ? "px" : this.settings.widthUnit);;
			this.window.style.height = this.settings.height + (this.settings.resizable ? "px" : this.settings.heightUnit);
		}
		if (typeof(this.settings.cookieId) != "undefined" && this.settings.cookieId != null && this.settings.storeBounds) {
			
			var string = this.findPositionString(false);
			
			if (string != null) {
				var array = string.split("::");
				var positions = array[1].split(",");
				if (positions.length == 4) {
					this.window.style.left = positions[0];
					this.window.style.top = positions[1];
					if (this.settings.resizable) {
						this.window.style.width = positions[2];
						this.content.style.height = positions[3];
					}
				} else if (positions.length == 2) {
					this.window.style.left = positions[0];
					this.window.style.top = positions[1];
				}
			}
		}

		var w = window.innerWidth || Wicket.Window.getViewportWidth();
		var h = window.innerHeight || Wicket.Window.getViewportHeight();
		
		if ( (parseInt(this.window.style.left) + parseInt(this.window.style.width)) > parseInt(w) ||
		 (parseInt(this.window.style.top) + parseInt(this.content.style.height)) > parseInt(h) ) {
			this.center();
		}
	},

	// override
	adjustOpenWindowsStatusAndZIndexesOnClose: function() {
		delete Wicket.DivWindow.openWindows[this.windowId];
		if (this.settings.modal) {
			// adjust Z-indexes
			// - for previous modalWindow
			if (this.parentModalWindow != null) {
				this.parentModalWindow.window.style.zIndex = Wicket.Window.Mask.zIndex + 1; // active modal window is always mask + 1
			}
			// - for non-modal windows that are on top of previous modalWindow
			var maxZIndexDelta = 0;
			for (var w in Wicket.DivWindow.openWindows) {
				var win = Wicket.DivWindow.openWindows[w];
				if (!win.settings.modal && win.parentModalWindow == this.parentModalWindow) {
					if (maxZIndexDelta < (Wicket.Window.Mask.zIndex - parseInt(win.window.style.zIndex))) {
						maxZIndexDelta = Wicket.Window.Mask.zIndex - parseInt(win.window.style.zIndex);
					}
				}
			}
			if (maxZIndexDelta > 0) {
				// move these non-modal windows above the mask
				for (var w in Wicket.DivWindow.openWindows) {
					var win = Wicket.DivWindow.openWindows[w];
					if (!win.settings.modal && win.parentModalWindow == this.parentModalWindow) {
						win.window.style.zIndex = parseInt(win.window.style.zIndex) + maxZIndexDelta + 2; // active non-modal windows always start at mask + 2
					}
				}
			}

			// cleanup and restore currentModalWindow
			Wicket.DivWindow.currentModalWindow = this.parentModalWindow;
			this.parentModalWindow = null;
		}
		
		this.closed = true;
	},

	toFront: function() {
		// windows that are under the mask cannot come to front completely as they are masked by a modal dialog; also modal dialogs cannot appear on top of child non-modal ones
		var currentZIndex = parseInt(this.window.style.zIndex);
		if (!this.settings.modal) {
			var toBackCount = 0;
			for (var w in Wicket.DivWindow.openWindows) {
				var win = Wicket.DivWindow.openWindows[w];
				if (!win.settings.modal && win.parentModalWindow == this.parentModalWindow && parseInt(win.window.style.zIndex) > currentZIndex) {
					win.window.style.zIndex = parseInt(win.window.style.zIndex) - 1;
					toBackCount++;
				}
			}
			this.window.style.zIndex = currentZIndex + toBackCount;
		}
	},

	toBack: function() {
		// non-modal dialogs cannot appear below parent modal ones
		var currentZIndex = parseInt(this.window.style.zIndex);
		if (!this.settings.modal) {
			var toFrontCount = 0;
			for (var w in Wicket.DivWindow.openWindows) {
				var win = Wicket.DivWindow.openWindows[w];
				if (!win.settings.modal && win.parentModalWindow == this.parentModalWindow && parseInt(win.window.style.zIndex) < currentZIndex) {
					win.window.style.zIndex = parseInt(win.window.style.zIndex) + 1;
					toFrontCount++;
				}
			}
			this.window.style.zIndex = currentZIndex - toFrontCount;
		}
	}

});

Wicket.DivWindow.Mask = Wicket.Class.create();

Wicket.Object.extendClass(Wicket.DivWindow.Mask, Wicket.Window.Mask, {

	disableCoveredContent: function() {
		// we disable user interaction for all non-modal windows with the same parent modal window as current (new) modal
		// window and also for the parent modal window (if not null)
		var currentModalWindow = Wicket.DivWindow.currentModalWindow;
		
		var coveredDocuments = new Array();
		for (var w in Wicket.DivWindow.openWindows) {
			var win = Wicket.DivWindow.openWindows[w];
			if (win != currentModalWindow && win.parentModalWindow == currentModalWindow.parentModalWindow) {
				var doc = win.getContentDocument();
				if (! this.arrayContainsElement(coveredDocuments, doc)) {
					coveredDocuments.push(doc);
				}
			}
		}
		
		if (currentModalWindow.parentModalWindow != null && !this.arrayContainsElement(coveredDocuments, currentModalWindow.parentModalWindow.getContentDocument())) {
			coveredDocuments.push(currentModalWindow.parentModalWindow.getContentDocument());
		} else if (!this.arrayContainsElement(coveredDocuments, document)) {
			// no parent modal window; disable main document
			coveredDocuments.push(document);
		}
		
		for (var i = 0; i < coveredDocuments.length; i++) {
			this.doDisable(coveredDocuments[i], currentModalWindow);
		}
	},

	arrayContainsElement: function(array, element) {
		for (var i = 0; i < array.length; i++) {
			if (array[i] == element) return true;
		}
		return false;
	}
	
});

Wicket.DivWindow.currentModalWindow = null;
Wicket.DivWindow.openWindows = { }; // windowId: DivWindow object pairs

Wicket.DivWindow.create = function(settings, windowId, currentWindowIsInIframe) {
	var win = Wicket.DivWindow.get(currentWindowIsInIframe);
	// create and return instance
	return new win(settings, windowId);
}

Wicket.DivWindow.get = function(isIframe) {
	var win;
	// if it is an iframe window...
	if (isIframe) {
		// attempt to get class from parent
		try {
			win = window.parent.Wicket.DivWindow;
		} catch (ignore) {}
	}

	// no parent...
	if (typeof(win) == "undefined") {
		win = Wicket.DivWindow;
	}
	return win;
}

Wicket.DivWindow.deletePosition = function(windowName) {
	var settings = new Object();
	settings.cookieId = windowName;
	var win = Wicket.DivWindow.create(settings);
	win.findPositionString(true);
}