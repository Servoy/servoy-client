var index = 1;
var menuId = 1;
var menuArgumentsInternal = {}; // menuId => [Menu, Arguments];

var MenuItem = {
    doClick: function() {
        this.callback();
    },

    getText: function() {
        return this.text;
    },

    setMethod: function(callback, args) {
        this.callback = callback;
        if (args !== undefined) {
            this.methodArguments = args;
        }
    },

    setAccelarator: function(accelerator) {
        this.accelerator = accelerator;
    },

    setEnabled: function(enabled) {
        this.enabled = enabled;
    },

    isEnabled: function() {
        if (this.enabled == undefined)
            return true;
        return this.enabled;
    },

    setIcon: function(icon) {
        if (icon && icon.slice(0, 2) === "fa") {
            this.fa_icon = icon;
        }
        else {
            this.icon = icon;
        }
    },

    setMnemonic: function(mnemonic) {
        this.mnemonic = mnemonic;
    },

    setText: function(text) {
        this.text = text;
    },

    setVisible: function(visible) {
        this.visible = visible;
    },

    set methodArguments(args) {
        menuArgumentsInternal[this.id] = [this, args];
    },

    get methodArguments() {
        if (!menuArgumentsInternal || !menuArgumentsInternal[this.id])
            return null;
        return menuArgumentsInternal[this.id][1];
    },

    setSelected: function(selected) {
        this.selected = selected;
    },

    getSelected: function() {
        if (this.selected == undefined)
            return false;
        return this.selected;
    },

    setName: function(name) {
        this.name = name;
    },

    getName: function() {
        if (this.name == undefined)
            return null;
        return this.name;
    },

    setBackgroundColor: function(color) {
        this.backgroundColor = color;
    },

    setForegroundColor: function(color) {
        this.foregroundColor = color;
    },

    putClientProperty: function(key, value) {
        this.key = value;
    },

    getClientProperty: function(key) {
        if (this.key == undefined) return null;
        return this.key;
    }
};

var Menu = {
    addMenuItem: function(text, callback, icon, mnemonic, enabled, align) {
        var newItem = Object.create(MenuItem);
        newItem.id = menuId++ + '';
        newItem.text = text;
        newItem.callback = callback;
        if (icon && icon.slice(0, 2) === "fa") {
            newItem.fa_icon = icon;
        }
        else {
            newItem.icon = icon;
        }
        newItem.mnemonic = mnemonic;
        newItem.align = align;
        newItem.enabled = enabled;
        newItem = this.items[this.items.push(newItem) - 1]; // we set and get it back to return as that instruments the value and makes it change-aware (be able to send granular updates to browser);
        menuArgumentsInternal[newItem.id] = [newItem, null];
        return newItem;
    },

    addCheckBox: function(text, callback, icon, mnemonic, enabled, align) {
        var checkbox = this.addMenuItem(text, callback, icon, mnemonic, enabled, align);
        checkbox.cssClass = "img_checkbox";
        return checkbox;
    },

    addRadioButton: function(text, callback, icon, mnemonic, enabled, align) {
        var radio = this.addMenuItem(text, callback, icon, mnemonic, enabled, align);
        radio.cssClass = "img_radio_off";
        return radio;
    },

    addRadioGroup: function() {

    },

    addSeparator: function(index) {
        if (index) {
            this.items.splice(index, 0, null);
        }
        else {
            this.items.push(null);
        }
    },

    addMenu: function(text) {
        var newMenu = Object.create(Menu);
        newMenu.text = text;
        newMenu.items = new Array();
        return this.items[this.items.push(newMenu) - 1]; // we set and get it back to return as that instruments the value and makes it change-aware (be able to send granular updates to browser);
        //		return newMenu;
    },

    getCheckBox: function(index) {
        return this.items[index];
    },

    getRadioButton: function(index) {
        return this.items[index];
    },

    getMenu: function(index) {
        return this.items[index];
    },

    getItem: function(index) {
        return this.items[index];
    },

    getItemCount: function() {
        return this.items.length;
    },

    getItemIndexByText: function(text) {
        for (var i = 0; i < this.items.length; i++) {
            if (this.items[i] && this.items[i].text == text)
                return i;
        }
    },

    removeAllItems: function() {
        return this.items.splice(0, this.items.length);
    },

    removeItem: function(indexes) {
        this.items = this.items.filter(function(element) {
            if (indexes && indexes.indexOf(this.items.indexOf(element)) >= 0) return false;
            return true;
        });
    },

    putClientProperty: function(key, value) {
        this.key = value;
    },

    getClientProperty: function(key) {
        if (this.key == undefined) return null;
        return this.key;
    },

    show: function(component, x, y, positionTop) {
        // this belongs to popup only but cannot assign directly on instance because then it is sent to client
        var command = { 'popupName': this.name };
        if (component == undefined) //show()
        {
            command.x = 0;
            command.y = 0;
        }
        else if (x == undefined && y == undefined) {//show(component) or show(event)
            if (component instanceof JSEvent) {
                component = component.getSource();
            }
            command.elementId = component.svyMarkupId;
            command.height = component.height;
            command.positionTop = false;
        }
        else if (x != undefined && y == undefined) {
            if (x === false || x === true) { //show(component, positionTop)
                command.elementId = component.svyMarkupId;
                command.height = component.height;
                command.positionTop = x;
            } else //show(x, y)
            {
                command.x = component;
                command.y = x;
            }
        } else { //show(component, x, y [, positionTop])
            command.elementId = component.svyMarkupId;
            command.x = x;
            command.y = y;
            command.height = component.height;
            if (positionTop == undefined) {
                command.positionTop = false;
            } else {
                command.positionTop = positionTop;
            }
        }
        $scope.model.popupMenuShowCommand = command;
    },

    setEnabled: function(enabled) {
        this.enabled = enabled;
    },

    isEnabled: function() {
        if (this.enabled == undefined)
            return true;
        return this.enabled;
    },

    setIcon: function(icon) {
        if (icon && icon.slice(0, 2) === "fa") {
            this.fa_icon = icon;
        }
        else {
            this.icon = icon;
        }
    },

    setMnemonic: function(mnemonic) {
        this.mnemonic = mnemonic;
    },

    setText: function(text) {
        this.text = text;
    },

    setVisible: function(visible) {
        this.visible = visible;
    },

    setBackgroundColor: function(color) {
        this.backgroundColor = color;
    },

    setForegroundColor: function(color) {
        this.foregroundColor = color;
    }
}

$scope.api.createFormPopup = function(form) {
    return {
        _width: undefined,
        _height: undefined,
        _x: undefined,
        _y: undefined,
        _showBackdrop: undefined,
        _doNotCloseOnClickOutside: undefined,
        _component: undefined,
        _scope: undefined,
        _dataprovider: undefined,
        _onClose: undefined,
		_parent: undefined,

        width: function(val) {
            if (val == undefined) return this._width;
            this._width = val;
            return this;
        },
        height: function(val) {
            if (val == undefined) return this._height;
            this._height = val;
            return this;
        },
        x: function(val) {
            if (val == undefined) return this._x;
            this._x = val;
            return this;
        },
        y: function(val) {
            if (val == undefined) return this._y;
            this._y = val;
            return this;
        },
        showBackdrop: function(val) {
            if (val == undefined) return this._showBackdrop;
            this._showBackdrop = val;
            return this;
        },
        doNotCloseOnClickOutside: function(val) {
            if (val == undefined) return this._doNotCloseOnClickOutside;
            this._doNotCloseOnClickOutside = val;
            return this;
        },
        onClose: function(val) {
            if (val == undefined) return this._onClose;
            this._onClose = val;
            return this;
        },
        scope: function(val) {
            if (val == undefined) return this._scope;
            this._scope = val;
            return this;
        },
        dataprovider: function(val) {
            if (val == undefined) return this._dataprovider;
            this._dataprovider = val;
            return this;
        },
        component: function(val) {
            if (val == undefined) return this._component;
            this._component = val;
            return this;
        },
		createFormPopup: function(val) {
			if (val == undefined) return this._parent;
			if (isPopupFormVisible(val, this._parent)) {
				console.warn("Form '" + val._formname_ +"' will not be displayed, because is already visible in another popup form!");
				return null;
			}
			var child = $scope.api.createFormPopup(val);
			child._parent = {
				component: this._component,
				form: form,
				width: this._width,
				height: this._height,
				x: this._x,
				y: this._y,
				showBackdrop: this._showBackdrop,
				doNotCloseOnClickOutside: this._doNotCloseOnClickOutside,
				onClose: this._onClose,
				parent: this._parent
			};
			return child;
		},
        show: function() {
            $scope.api.showFormPopup(this._component, form, this._scope, this._dataprovider, this._width, this._height, this._x, this._y, this._showBackdrop, this._doNotCloseOnClickOutside, this._onClose, this._parent);
        },
		cancel: function() {
			if (this._parent) {
				$scope.api.cancelForm(this._parent.form);
				// update model
				$scope.model.popupform = {};
				$scope.model.popupform.component = this._parent.component;
				$scope.model.popupform.form = this._parent.form;
				$scope.model.popupform.width = this._parent.width;
				$scope.model.popupform.height = this._parent.height;
				$scope.model.popupform.x = this._parent.x;
				$scope.model.popupform.y = this._parent.y;
				$scope.model.popupform.showBackdrop = this._parent.showBackdrop;
				$scope.model.popupform.doNotCloseOnClickOutside = this._parent.doNotCloseOnClickOutside;
				$scope.model.popupform.onClose = this._parent.onClose;
				$scope.model.popupform.parent = this._parent.parent;
			} else {
				$scope.api.cancelFormPopupInternal(true);
				$scope.clearPopupForm();
			}
		}
    }
}

function isPopupFormVisible(form, parent) {
	if ($scope.model.popupform) {
		var popup = $scope.model.popupform;
		while(popup) {
			if (popup.form == form._formname_ && parent && popup.parent.form == parent.form) {
				return true;
			}
			popup = popup.parent;
		}
	}
	return false;
}

$scope.api.getFormPopup = function(form) {
	if ($scope.model.popupform) {
		var popup = $scope.model.popupform;
		while(popup) {
			if (popup.form == form._formname_) {
				var popupForm = $scope.api.createFormPopup(form);
				popupForm._width = popup.width;
				popupForm._height = popup.height;
				popupForm._x = popup.x;
				popupForm._y = popup.y;
				popupForm._showBackdrop = popup.showBackdrop;
				popupForm._doNotCloseOnClickOutside = popup.doNotCloseOnClickOutside;
				popupForm._component = popup.component;
				popupForm._scope = popup.scope;
				popupForm._dataprovider = popup.dataprovider;
				popupForm._onClose = popup.onClose;
				popupForm._parent = popup.parent;
				return popupForm;
			}
			popup = popup.parent;		
		}
	}
	return null;
}

$scope.api.createPopupMenu = function(jsmenu, callback) {
    var popupName = jsmenu ? jsmenu.getName() : 'popupmenu' + index++;
    var popup = Object.create(Menu);
    popup.name = popupName;
    popup.items = new Array();
    if (jsmenu) {
        popup.cssClass = jsmenu.getStyleClass();
        addJSMenuItems(popup, jsmenu.getMenuItemsWithSecurity(), callback, jsmenu ? jsmenu.getSelectedItem() : null);
    }
    if (!$scope.model.popupMenus)
        $scope.model.popupMenus = [];

    return $scope.model.popupMenus[$scope.model.popupMenus.push(popup) - 1]; // we set and get it back to return as that instruments the value and makes it change-aware (be able to send granular updates to browser); as window plugin doesn't have API to get the popup menu after it was created, users must get this change-aware version from the beginning as this is the only thing they get and they need to hold on to it
    //	return popup;
}

function addJSMenuItems(popupmenu, jsmenuitems, callback, selectedItem) {
    if (popupmenu && jsmenuitems) {
        for (var i = 0; i < jsmenuitems.length; i++) {
            var jsmenuitem = jsmenuitems[i];
            if (jsmenuitem.getSubMenuItemsWithSecurity().length > 0) {
                var newmenu = popupmenu.addMenu(jsmenuitem.getMenuText());
                newmenu.cssClass = jsmenuitem.getStyleClass();
                addJSMenuItems(newmenu, jsmenuitem.getSubMenuItemsWithSecurity(), callback)
            } else {
                var newmenuitem = popupmenu.addMenuItem(jsmenuitem.getMenuText(), callback, jsmenuitem.getIconStyleClass(), null, jsmenuitem.getEnabledWithSecurity(), null);
                newmenuitem.cssClass = jsmenuitem.getStyleClass();
                newmenuitem.methodArguments = jsmenuitem.getCallbackArguments();
                if (selectedItem && jsmenuitem == selectedItem) {
                    newmenuitem.setSelected(true);
                }
            }
        }
    }
}

$scope.api.closeFormPopup = function(retval) {
    if ($scope.model.popupform) {
        if ($scope.scope && $scope.dataProviderID) {
            $scope.scope[$scope.dataProviderID] = retval;
        }
        $scope.model.popupform.retval = retval;
		$scope.api.cancelFormPopupInternal(true);
		if (!($scope.model.popupform && $scope.model.popupform.onClose)) {
			$scope.clearPopupForm();
		}
    }
}

$scope.formPopupClosed = function(event) {
    if ($scope.model.popupform && $scope.model.popupform.onClose) {
        $scope.model.popupform.onClose(event, $scope.model.popupform.retval);
	}
    $scope.model.popupform = null;
}

$scope.executeMenuItem = function(menuItemId, itemIndex, parentItemIndex, isSelected, parentMenuText, menuText) {
    var menuAndArgs = menuArgumentsInternal[menuItemId];
    var fixedArgs = new Array(itemIndex, parentItemIndex, isSelected, parentMenuText, menuText);
    var allArgs = fixedArgs.concat(menuAndArgs[1]);
    menuAndArgs[0].callback.apply(null, allArgs);
}

$scope.api.showFormPopup = function(component, form, dataproviderScope, dataproviderID, width, height, x, y, showBackdrop, doNotCloseOnClickOutside, onClose, parent) {
	if ($scope.model.popupform && !parent) {
        $scope.api.cancelFormPopupInternal(true);
    }
    $scope.model.popupform = {};
    $scope.model.popupform.component = component;
    $scope.model.popupform.form = form;
    $scope.scope = dataproviderScope;
    $scope.dataProviderID = dataproviderID;
    $scope.model.popupform.width = width;
    $scope.model.popupform.height = height;
    $scope.model.popupform.x = x;
    $scope.model.popupform.y = y;
    $scope.model.popupform.showBackdrop = showBackdrop;
    $scope.model.popupform.doNotCloseOnClickOutside = doNotCloseOnClickOutside;
    $scope.model.popupform.onClose = onClose;
	$scope.model.popupform.parent = parent;
}

$scope.api.createShortcut = function(shortcut, callback, contextFilter, arguments, consumeEvent) {
    if (Array.isArray(contextFilter) && !arguments) {
        arguments = contextFilter;
        contextFilter = null;
    }
    if (contextFilter == undefined) {
        contextFilter = null;
    }
    if (arguments == undefined) {
        arguments = null;
    }
    if (consumeEvent == undefined) {
        consumeEvent = false;
    }
    if (!$scope.model.shortcuts) $scope.model.shortcuts = [];
    $scope.api.removeShortcut(shortcut, contextFilter)
    $scope.model.shortcuts.push({ 'shortcut': shortcut, 'callback': callback, 'contextFilter': contextFilter, 'arguments': arguments, 'consumeEvent': consumeEvent });
    return true;
}

$scope.api.removeShortcut = function(shortcut, contextFilter) {
    if (contextFilter == undefined) {
        contextFilter = null;
    }
    for (var i = 0; i < $scope.model.shortcuts.length; i++) {
        if ($scope.model.shortcuts[i].shortcut == shortcut && $scope.model.shortcuts[i].contextFilter === contextFilter) {
            $scope.model.shortcuts.splice(i, 1);
            break;
        }
    }
    return true;
}

$scope.api.cleanup = function() {
    $scope.model.popupMenus = null;
    $scope.model.popupMenuShowCommand = null;
    $scope.model.shortcuts = null;
    $scope.model.popupform = null;
}

$scope.clearPopupForm = function() {
    $scope.model.popupform = null;
}