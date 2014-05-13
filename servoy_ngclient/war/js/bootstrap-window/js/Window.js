var Window = null;
(function($) {

    "use strict";
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
        options.elements.body.html(options.bodyContent);
        options.elements.footer.html(options.footerContent);
        
        this.undock();

        this.setSticky(options.sticky);
    };

    Window.prototype.undock = function () {
        this.$el.css('visibility', 'hidden');
        this.$el.appendTo('body');
        this.centerWindow();
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
        this.$el.on('mousedown', function() {
            if (_this.options.blocker) {
                _this.options.blocker.getElement().trigger('focused');
                _this.options.blocker.blink();
                return;
            } else {
                _this.$el.trigger('focused');
            }
            
            if (_this.$el.hasClass('ns-resize') || _this.$el.hasClass('ew-resize')) {
                $('body > *').addClass('disable-select');
                _this.resizing = true;
                _this.offset = {};
                _this.offset.x = event.pageX;
                _this.offset.y = event.pageY;
                _this.window_info = {
                    top: _this.$el.position().top,
                    left: _this.$el.position().left,
                    width: _this.$el.width(),
                    height: _this.$el.height()
                };

                if (event.offsetY < 5) {
                    _this.$el.addClass('north');
                }
                if (event.offsetY > (_this.$el.height() - 5)) {
                    _this.$el.addClass('south');
                }
                if (event.offsetX < 5) {
                    _this.$el.addClass('west');
                }
                if (event.offsetX > (_this.$el.width() - 5)) {
                    _this.$el.addClass('east');
                }
            }
        });

        _this.options.references.body.on('mouseup', function () {
            _this.resizing = false;
            $('body > *').removeClass('disable-select');
            _this.$el.removeClass('west');
            _this.$el.removeClass('east');
            _this.$el.removeClass('north');
            _this.$el.removeClass('south');

        });
        _this.options.elements.handle.off('mousedown');
        _this.options.elements.handle.on('mousedown', function(event) {
            if (_this.options.blocker) {
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
        });

        _this.options.references.body.on('mousemove', function(event) {
            if (_this.moving) {
                var top = _this.options.elements.handle.position().top,
                    left = _this.options.elements.handle.position().left;
                _this.$el.css('top', event.pageY - _this.offset.y);
                _this.$el.css('left', event.pageX - _this.offset.x);
            }
            if (_this.options.resizable && _this.resizing) {
                if (_this.$el.hasClass("east")) {
                    _this.$el.css('width', event.pageX - _this.window_info.left);
                }
                if (_this.$el.hasClass("west")) {
                    
                    _this.$el.css('left', event.pageX);
                    _this.$el.css('width', _this.window_info.width + (_this.window_info.left  - event.pageX));
                }
                if (_this.$el.hasClass("south")) {
                    _this.$el.css('height', event.pageY - _this.window_info.top);
                }
                if (_this.$el.hasClass("north")) {
                    _this.$el.css('top', event.pageY);
                    _this.$el.css('height', _this.window_info.height + (_this.window_info.top  - event.pageY));
                }
            }
        });
        
        _this.options.references.body.on('mouseleave', function(event) { 
			_this.moving = false;
			$('body > *').removeClass('disable-select');
		});

        this.$el.on('mousemove', function (event) {
            if (_this.options.blocker) {
                return;
            }
            if (_this.options.resizable) {
                if (event.offsetY > (_this.$el.height() - 5) || event.offsetY < 5) {
                    _this.$el.addClass('ns-resize');
                } else {
                    _this.$el.removeClass('ns-resize');
                }
                if (event.offsetX > (_this.$el.width() - 5) || event.offsetX < 5) {
                    _this.$el.addClass('ew-resize');

                } else {
                    _this.$el.removeClass('ew-resize');
                }
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
        if (options.height) {
            this.$el.css('height', options.height);
        }
        if (options.width) {
            this.$el.css('width', options.width);
        }
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
