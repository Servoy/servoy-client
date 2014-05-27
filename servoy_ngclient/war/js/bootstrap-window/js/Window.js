var Window = null;
(function($) {

    "use strict";
    var resizeConstants={
		NORTH:1,
		SOUTH:2,
		EAST:4,
		WEST:8
    };
    var resizeAnchorClasses ={
		'1':'ns-resize', // NORTH
		'2':'ns-resize', // SOUTH
		'4':'ew-resize', // EAST
		'8':'ew-resize', // WEST
		'5':'nesw-resize', // N-E
		'9':'nwse-resize', // N-W
		'6':'nwse-resize', // S-E
		'10':'nesw-resize',// S-W
		'0':''
	};
	Window = function(options) {
		options = options || {};
		var defaults = {
				selectors: {
                    handle: '.window-header',
                    title: '.window-title',
                    body: '.window-body',
                    footer: '.window-footer'
                },
                elements: {
                    handle: null,
                    title: null,
                    body: null,
                    footer: null
                },
                references: {
                    body: $('body'),
                    window: $(window)
                },
                parseHandleForTitle: true,
                title: 'No Title',
                bodyContent: '',
                footerContent: ''
            };
        this.options = $.extend(true, {}, defaults, options);
        this.initialize(this.options);
        return this;
    };

    Window.prototype.initialize = function(options) {
        var _this = this;

        if (options.fromElement) {
            if (options.fromElement instanceof jQuery) {
                this.$el = options.fromElement;
            } else if (options.fromElement instanceof Element) {
                this.$el = $(options.fromElement);
            } else if (typeof options.fromElement) {
                this.$el = $(options.fromElement);
            }
        } else if (options.template) {
            this.$el = $(options.template);
        } else {
            throw new Error("No template specified for window.");
        }

        options.elements.handle = this.$el.find(options.selectors.handle);
        options.elements.title = this.$el.find(options.selectors.title);
        options.elements.body = this.$el.find(options.selectors.body);
        options.elements.footer = this.$el.find(options.selectors.footer);
        options.elements.title.html(options.title);
        if (options.fromElement && _this.$el.find('[data-dismiss=window]').length <= 0) {
            options.elements.title.append('<button class="close" data-dismiss="window">x</button>');
        }
        if(options.bodyContent)options.elements.body.html(options.bodyContent);
        if(options.footerContent)options.elements.footer.html(options.footerContent);
        this.undock();

        this.setSticky(options.sticky);
    };

    Window.prototype.undock = function () {
        this.$el.css('visibility', 'hidden');
        this.$el.appendTo('body');
        if(!this.options.location){
			//default positioning
			this.centerWindow();        
        }else{
			//user entered options
            this.$el.css('left', this.options.location.left);
            this.$el.css('top', this.options.location.top);
        }
        if(this.options.size){
        	this.setSize(this.options.size);
        }
        if( /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent) ) {
            this.options.references.window.bind('orientationchange resize', function(event){
                _this.centerWindow();
            });
        }

        this.$el.on('touchmove',function(e){
              e.stopPropagation();
        });

        this.initHandlers();
        this.$el.hide();
        if (this.options.id) {
            this.id = this. options.id;
        } else {
            this.id = '';
        }
        this.show();
    };

    Window.prototype.show = function () {
        this.$el.css('visibility', 'visible');
        this.$el.fadeIn();
    };
    
    Window.prototype.setSize = function(size){
    	 var winBody = this.$el.find(this.options.selectors.body);
         var winHeadFootHeight = 0;
			var head = this.$el.find(this.options.selectors.handle);
         if(head){
				winHeadFootHeight += head.outerHeight();
         }
         var foot = this.$el.find(this.options.selectors.footer);
         if(foot){
				winHeadFootHeight += foot.outerHeight();
         }
         winBody.css('width', size.width - parseInt(this.$el.css("marginRight")) - parseInt(this.$el.css("marginLeft")) );
         winBody.css('height', size.height - winHeadFootHeight);    	
    };

    Window.prototype.centerWindow = function () {
        var top, left,
            bodyTop = parseInt(this.options.references.body.position().top, 10) + parseInt(this.options.references.body.css('paddingTop'), 10),
            maxHeight;
        if (!this.options.sticky) {
            left = (this.options.references.window.width() / 2) - (this.$el.width() / 2);
            top = (this.options.references.window.height() / 2) - (this.$el.height() / 2);
        } else {
            left = (this.options.references.window.width() / 2) - (this.$el.width() / 2);
            top = (this.options.references.window.height() / 2) - (this.$el.height() / 2);
        }

        if (top < bodyTop) {
            top = bodyTop;
        }
        maxHeight = ((this.options.references.window.height() - bodyTop) - (parseInt(this.options.elements.handle.css('height'), 10) + parseInt(this.options.elements.footer.css('height'), 10))) - 45;
        //this.options.elements.body.css('maxHeight', maxHeight);

        this.$el.css('left', left);
        this.$el.css('top', top);
    };

    Window.prototype.close = function() {
        var _this = this;
        this.$el.trigger('close');
        if (this.options.parent) {
            this.options.parent.clearBlocker();
            if (this.options.window_manager) {
                this.options.window_manager.setFocused(this.options.parent);
            }
        } else if (this.options.window_manager && this.options.window_manager.windows.length > 0) {
            this.options.window_manager.setNextFocused();
        }
        this.$el.fadeOut(function() {
            _this.$el.remove();
        });
        if (this.$windowTab) {
            this.$windowTab.fadeOut(400, function() {
                _this.$windowTab.remove();
            });
        }

    };

    Window.prototype.setActive = function(active) {
        if (active) {
            this.$el.addClass('active');
            if (this.$windowTab) {
                this.$windowTab.addClass('label-primary');
            }
        } else {
            this.$el.removeClass('active');
            if (this.$windowTab) {
                this.$windowTab.removeClass('label-primary');
                this.$windowTab.addClass('label-default');
            }
        }
    };

    Window.prototype.setIndex = function(index) {
        this.$el.css('zIndex', index);
    };

    Window.prototype.setWindowTab = function(windowTab) {
        this.$windowTab = windowTab;
    };
    Window.prototype.getWindowTab = function() {
        return this.$windowTab;
    };

    Window.prototype.getTitle = function() {
        return this.options.title;
    };

    Window.prototype.getElement = function() {
        return this.$el;
    };

    Window.prototype.setSticky = function(sticky) {
        this.options.sticky = sticky;
        if (sticky === false) {
            this.$el.css({
                'position': 'absolute'
            });
        } else {
            this.$el.css({
                'position': 'fixed'
            });
        }
    };

    Window.prototype.getSticky = function() {
        return this.options.sticky;
    };

    Window.prototype.setManager = function (window_manager) {
        this.options.window_manager = window_manager;
    };

    Window.prototype.initHandlers = function() {
        var _this = this;

        this.$el.find('[data-dismiss=window]').on('click', function(event) {
            if (_this.options.blocker) {
                return;
            }
            _this.close();
        });

        this.$el.off('mousedown');
        this.$el.on('mousedown', function(event) {
            if (_this.options.blocker) {
                _this.options.blocker.getElement().trigger('focused');
                _this.options.blocker.blink();
                return;
            } else {
                _this.$el.trigger('focused');
            }
            
            if (_this.$el.hasClass(lastResizeClass)) {
                $('body > *').addClass('disable-select');
                _this.resizing = true;
                _this.offset = {};
				_this.offset.x = event.pageX - _this.$el.position().left;
                _this.offset.y = event.pageY - _this.$el.position().top;
                _this.window_info = {
                    top: _this.$el.position().top,
                    left: _this.$el.position().left,
                    width: _this.$el.width(),
                    height: _this.$el.height()
                };
                
            	var offX = event.offsetX;
            	var offY = event.offsetY;
            	if (!event.offsetX){
            		// FireFox Fix
            		offX = event.originalEvent.layerX;
            		offY = event.originalEvent.layerY;
            	}
                var target = $(event.target);
				var windowOffsetX = target.offset().left - _this.$el.offset().left;
				var windowOffsetY = target.offset().top - _this.$el.offset().top;

                if (offY + windowOffsetY < 5) {
                    _this.$el.addClass('north');
                }
                if (offY + windowOffsetY > (_this.$el.height() - 5)) {
                    _this.$el.addClass('south');
                }
                if (offX + windowOffsetX < 5) {
                    _this.$el.addClass('west');
                }
                if (offX + windowOffsetY > (_this.$el.width() - 5)) {
                    _this.$el.addClass('east');
                }
            }
        });

        _this.options.references.body.on('mouseup', function () {
            _this.resizing = false;
            _this.moving = false;
            $('body > *').removeClass('disable-select');
            _this.$el.removeClass('west');
            _this.$el.removeClass('east');
            _this.$el.removeClass('north');
            _this.$el.removeClass('south');     
            var width = _this.$el.width();
            var height = _this.$el.height();
			var size = {width:width,height:height};            
			_this.$el.trigger('bswin.resize',size);            
        });
        _this.options.elements.handle.off('mousedown');
        _this.options.elements.handle.on('mousedown', function(event) {
            var handleHeight = _this.options.elements.handle.outerHeight();
            var handleWidth = _this.options.elements.handle.outerWidth();
			var offX = event.offsetX;
			var offY = event.offsetY;
        	if (!event.offsetX){
        		// FireFox Fix
        		offX = event.originalEvent.layerX;
        		offY = event.originalEvent.layerY;
        	}
            if (_this.options.blocker ||
            	offY < 5 ||
				handleHeight - offY < 5 || 
				offX < 5 ||
				handleWidth - offX < 5) {
				return;
			}
            _this.moving = true;
            _this.offset = {};
            _this.offset.x = event.pageX - _this.$el.position().left;
            _this.offset.y = event.pageY - _this.$el.position().top;
            $('body > *').addClass('disable-select');
        });
        _this.options.elements.handle.on('mouseup', function(event) {
            _this.moving = false;
            $('body > *').removeClass('disable-select');
            var pos = _this.$el.offset();         
            _this.$el.trigger('bswin.move',pos);
        });

        _this.options.references.body.on('mousemove', function(event) {
			if (_this.moving) {
                var top = _this.options.elements.handle.position().top,
                    left = _this.options.elements.handle.position().left;
                _this.$el.css('top', event.pageY - _this.offset.y);
                _this.$el.css('left', event.pageX - _this.offset.x);
            }
            if (_this.options.resizable && _this.resizing) {
                var winBody = _this.$el.find(_this.options.selectors.body);
                var winHeadFootHeight = 0;
				var head = _this.$el.find(_this.options.selectors.handle);
                if(head){
					winHeadFootHeight += head.outerHeight();
                }
                var foot = _this.$el.find(_this.options.selectors.footer);
                if(foot){
					winHeadFootHeight += foot.outerHeight();
                }
                if (_this.$el.hasClass("east")) {
					winBody.css('width', event.pageX - _this.window_info.left);
                }
                if (_this.$el.hasClass("west")) {
                    
                    _this.$el.css('left', event.pageX);
                    winBody.css('width', _this.window_info.width + (_this.window_info.left  - event.pageX));
                }
                if (_this.$el.hasClass("south")) {
					winBody.css('height', event.pageY - _this.window_info.top - winHeadFootHeight);
                }
                if (_this.$el.hasClass("north")) {
					_this.$el.css('top', event.pageY);
                    winBody.css('height', _this.window_info.height + (_this.window_info.top  - event.pageY) - winHeadFootHeight);
                }
            }
        });
        
        _this.options.references.body.on('mouseleave', function(event) { 
			_this.moving = false;
			$('body > *').removeClass('disable-select');
		});

        var lastResizeClass = '';
        this.$el.on('mousemove', function (event) {
            if (_this.options.blocker) {
                return;
            }
            if (_this.options.resizable ) {
				var resizeClassIdx = 0;
				//target can be the header or footer, and event.offsetX/Y will be relative to the header/footer .Adjust to '.window';
				var target = $(event.target);
				var windowOffsetX = target.offset().left - _this.$el.offset().left;
				var windowOffsetY = target.offset().top - _this.$el.offset().top;

            	var offX = event.offsetX;
            	var offY = event.offsetY;
            	if (!event.offsetX){
            		// FireFox Fix
            		offX = event.originalEvent.layerX;
            		offY = event.originalEvent.layerY;
            	}
                if (offY + windowOffsetY > (_this.$el.height() - 5) ) {
                    resizeClassIdx += resizeConstants.SOUTH;
                }
                if (offY + windowOffsetY< 5) {
                    resizeClassIdx += resizeConstants.NORTH;
                }
                if (offX + windowOffsetX> _this.$el.width() - 5) {
					resizeClassIdx += resizeConstants.EAST;
                }
                if (offX + windowOffsetX< 5)
                {
					resizeClassIdx += resizeConstants.WEST;
                }
				_this.$el.removeClass(lastResizeClass);
                lastResizeClass=resizeAnchorClasses[resizeClassIdx];
                _this.$el.addClass(lastResizeClass);
            }
        });
    };

    Window.prototype.resize = function (options) {
        options = options || {};
        if (options.top) {
            this.$el.css('top', options.top);
        }
        if (options.left) {
            this.$el.css('left', options.left);
        }
        this.setSize({height:options.height,width:options.width});
    };

    Window.prototype.setBlocker = function (window_handle) {
        this.options.blocker = window_handle;
        this.$el.find('.disable-shade').remove();
        var shade = '<div class="disable-shade"></div>';
        this.options.elements.body.append(shade);
        this.options.elements.body.addClass('disable-scroll');
        this.options.elements.footer.append(shade);
        this.$el.find('.disable-shade').fadeIn();
        if (!this.options.blocker.getParent()) {
            this.options.blocker.setParent(this);
        }
    };


    Window.prototype.getBlocker = function () {
        return this.options.blocker;
    };

    Window.prototype.clearBlocker = function () {
        this.options.elements.body.removeClass('disable-scroll');
        this.$el.find('.disable-shade').fadeOut(function () {
            this.remove();
        });
        delete this.options.blocker;
    };

    Window.prototype.setParent = function (window_handle) {
        this.options.parent = window_handle;
        if (!this.options.parent.getBlocker()) {
            this.options.parent.setBlocker(this);
        }
    };

    Window.prototype.getParent = function () {
        return this.options.parent;
    };

    Window.prototype.blink = function () {
        var _this = this,
            active = this.$el.hasClass('active'),

            blinkInterval = setInterval(function () {
            _this.$el.toggleClass('active');
        }, 250),
            blinkTimeout = setTimeout(function () {
            clearInterval(blinkInterval);
            if (active) {
                _this.$el.addClass('active');
            }
        }, 1000);
    };
 
    $.fn.window = function(options) {
        options = options || {};
        var newWindow,
            window_opts = $.extend({
                fromElement: this,
                selectors: {}
            }, options || {});
        if (typeof options === "object") {
            if (window_opts.selectors.handle) {
                this.find(window_opts.selectors.handle).css('cursor', 'move');
            }

            if (!$(this).hasClass('window')) {
                $(this).addClass('window');
            }
            newWindow = new Window($.extend({}, window_opts, window_opts));
            this.data('window', newWindow);
            

        } else if (typeof options === "string") {
            switch (options) {
                case "close":
                    this.data('window').close();
                    break;
                case "show":
                    this.data('window').show();
                    break;
                default:
                    break;
            }
        }


        return this;
        
    };

    $('[data-window-target]').off('click');
    $('[data-window-target]').on('click', function () {
        var $this = $(this),
            opts = {selectors:{}};
        if ($this.data('windowTitle')) {
            opts.title = $this.data('windowTitle');
        }

        if ($this.data('titleHandle')) {
            opts.selectors.title = $this.data('titleHandle');
        }

        if ($this.data('windowHandle')) {
            opts.selectors.handle = $this.data('windowHandle');
        }

        $($this.data('windowTarget')).window(opts); 
    });
}(jQuery));