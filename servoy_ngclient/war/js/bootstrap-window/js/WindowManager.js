var WindowManager = null;// jshint ignore:line
(function($) {
    "use strict";
    var zIndexIncrement =100;
    WindowManager = function(options) {
        this.windows = [];
        options = options || {};
        this.modalStack = [];
        this.initialize(options);
        return this;
    };

    WindowManager.prototype.findWindowByID = function(id) {
        var returnValue = null;
        $.each(this.windows, function(index, window) {
            console.log(arguments);
            if (window.id === id) {
                returnValue = window;
            }
        });
        return returnValue;
    };

    WindowManager.prototype.destroyWindow = function(window_handle) {
        var _this = this;

        this.removeModal(window_handle);
        $.each(this.windows, function(index, window) {
            if (window === window_handle) {
                _this.windows.splice(index, 1);
                _this.resortWindows();
            }
        });
    };

    WindowManager.prototype.resortWindows = function() {
        var startZIndex = 900;
        $.each(this.windows, function(index, window) {

            window.setIndex(startZIndex + index*zIndexIncrement);
        });
        if(this.modalStack.length>0){
			//update modal backdrop z-index.
			var lastWindowZindex = parseInt(this.modalStack[this.modalStack.length-1].$el.css('z-index'));
			var backdrop = $('.modal-backdrop');
			if(backdrop)backdrop.css('z-index',lastWindowZindex-1);
        }
    };

    WindowManager.prototype.setFocused = function(focused_window) {
        var focusedWindowIndex;
        while (focused_window.getBlocker()) {
            focused_window = focused_window.getBlocker();
        }
        $.each(this.windows, function(index, windowHandle) {
            windowHandle.setActive(false);
            if (windowHandle === focused_window) {
                focusedWindowIndex = index;
            }
        });
        if(this.modalStack.indexOf(focused_window) == -1){
			this.windows.push(this.windows.splice(focusedWindowIndex, 1)[0]);
        }        
        focused_window.setActive(true);
        this.resortWindows();

    };
    /**
     * moves the winow to the back of the stack (stops at the first modal window)
     * */
    WindowManager.prototype.sendToBack = function(window) {
    	 //move the BS window instance to the front of the array
    	 var from = this.windows.indexOf(window)
    	 var toWindow = this.modalStack.length > 0 ? this.modalStack[this.modalStack.length-1] : null;
    	 var to = toWindow ? this.windows.indexOf(toWindow)+1: 0;
    	 this.windows.splice(to/*to*/, 0, this.windows.splice(from, 1)[0]);
    	 this.setFocused(this.windows[this.windows.length-1]);
    };

    WindowManager.prototype.initialize = function(options) {
        this.options = options;
        if (this.options.container) {
            $(this.options.container).addClass('window-pane');
        }
        if(!this.options.backdrop){
            this.options.modalBackdrop =$("<div class='modal-backdrop fade' style='z-index:899'></div>");
        }
    };

    WindowManager.prototype.setNextFocused = function () {
        this.setFocused(this.windows[this.windows.length-1]);
    };

    WindowManager.prototype.addWindow = function(window_object) {
        var _this = this;
        window_object.getElement().on('focused', function(event) {
            _this.setFocused(window_object);
        });
        window_object.getElement().on('close', function() {
            _this.destroyWindow(window_object);
            if (window_object.getWindowTab()) {
                window_object.getWindowTab().remove();
            }

        });

        if (this.options.container) {
            window_object.setWindowTab($('<span class="label label-default">' + window_object.getTitle() + '<button class="close">x</button></span>'));
            window_object.getWindowTab().find('.close').on('click', function(event) {
                window_object.close();
            });
            window_object.getWindowTab().on('click', function(event) {
                _this.setFocused(window_object);
                if (window_object.getSticky()) {
                    window.scrollTo(0, window_object.getElement().position().top);
                }

            });

            $(this.options.container).append(window_object.getWindowTab());
        }

        this.windows.push(window_object);
        window_object.setManager(this);
        this.setFocused(window_object);
        return window_object;
    };

    WindowManager.prototype.createWindow = function(window_options) {
        var _this = this;
        var final_options = Object.create(window_options);
        if (this.options.windowTemplate && !final_options.template) {
            final_options.template = this.options.windowTemplate;
        }
        $.extend( final_options ,this.options);
        var newWindow = new Window(final_options);
        if(final_options.isModal) {
			this.addModal(newWindow);
         }                  
        return this.addWindow(newWindow);
    };
    
    WindowManager.prototype.addModal = function(windowObj) {
		/*PRIVATE FUNCTION*/
		if(this.modalStack.length  === 0) {
			$('body').append(windowObj.options.modalBackdrop.clone());
			setTimeout(function(){
				$('.modal-backdrop').addClass('in');  // TODO make this code only for default backdrop (if none is supplied) , in an event like 'onShow(win)'
			},50);
		}
		this.modalStack.push(windowObj);
	};
	WindowManager.prototype.removeModal = function(windowObj) {
		/*PRIVATE FUNCTION*/
		var i = this.modalStack.indexOf(windowObj);
			if(i != -1) {
				this.modalStack.splice(i, 1);
				//also remove from dom with animation
				if(this.modalStack.length === 0){
					var backdrop = $('.modal-backdrop');  // TODO make this code only for default backdrop (if none is supplied) , in an event like 'onRemove(win)'
					backdrop.removeClass('in');
					setTimeout(function(){ 
						backdrop.remove();
					},50);
				}
			}
	};
}(jQuery));
