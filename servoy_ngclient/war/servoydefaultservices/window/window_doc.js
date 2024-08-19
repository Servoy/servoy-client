/**
 * Creates a new popup menu, either empty, or initialized from an existing JSMenu.
 * 
* @example
* // create a popup menu
* var menu = plugins.window.createPopupMenu();
* // add a menu item
* menu.addMenuItem("an entry", feedback);
*
* if (event.getSource()) {
*  // display the popup over the component which is the source of the event
*  menu.show(event.getSource());
*  // display the popup over the components, at specified coordinates relative to the component
*  //menu.show(event.getSource(), 10, 10);
*  // display the popup at specified coordinates relative to the main window
*  //menu.show(100, 100);
* }
* 
 * @param {JSMenu} [jsmenu] The JSMenu whose structure will be used to initialize the popup menu.
 * @param {Function} [callback] The menu item click handler that will be set on all popup menu items
 * 
 * @return Popup
 */
 function createPopupMenu(jsmenu, callback) {}

/**
 * Create a shortcut.
 *
 * @example
 * // this plugin uses the java keystroke parser
 * // see http://java.sun.com/j2se/1.5.0/docs/api/javax/swing/KeyStroke.html#getKeyStroke(java.lang.String)
 * // global handler
 * plugins.window.createShortcut('control shift I', scopes.globals.handleOrdersShortcut);
 * // global handler with a form context filter
 * plugins.window.createShortcut('control shift I', scopes.globals.handleOrdersShortcut, 'frm_contacts');
 * // form method called when shortcut is used
 * plugins.window.createShortcut('control RIGHT', forms.frm_contacts.handleMyShortcut);
 * // form method called when shortcut is used and arguments are passed to the method
 * plugins.window.createShortcut('control RIGHT', forms.frm_contacts.handleMyShortcut, new Array(argument1, argument2));
 * // Passing the method argument as a string prevents unnecessary form loading
 * //plugins.window.createShortcut('control RIGHT', 'frm_contacts.handleMyShortcut', new Array(argument1, argument2));
 * // Passing the method as a name and the contextFilter set so that this shortcut only trigger on the form 'frm_contacts'.
 * plugins.window.createShortcut('control RIGHT', 'frm_contacts.handleMyShortcut', 'frm_contacts', new Array(argument1, argument2));
 * // Num Lock and Substract shortcuts
 * plugins.window.createShortcut("NUMPAD8", handleMyShortcut);
 * plugins.window.createShortcut("SUBTRACT", handleMyShortcut);
 * // remove global shortcut and form-level shortcut
 * plugins.window.removeShortcut('menu 1');
 * plugins.window.removeShortcut('control RIGHT', 'frm_contacts');
 * // consuming they keystroke so that a default browser event will not happen
 * plugins.window.createShortcut('F4', scopes.globals.handleOrdersShortcut, 'frm_contacts', null, true);
 * // shortcut handlers are called with an JSEvent argument
 * ///*
 * // * Handle keyboard shortcut.
 * // *
 * // * @param {JSEvent} event the event that triggered the action
 * // *&#47;
 * //function handleShortcut(event)
 * //{
 * //  application.output(event.getType()) // returns 'menu 1'
 * //  application.output(event.getFormName()) // returns 'frm_contacts'
 * //  application.output(event.getElementName()) // returns 'contact_name_field' or null when no element is selected
 * //}
 * // NOTES:
 * // 1) shortcuts will not override existing operating system or browser shortcuts,
 * // choose your shortcuts carefully to make sure they work in all clients.
 * // 2) always use lower-case letters for modifiers (shift, control, etc.), otherwise createShortcut will fail.
 *
 * @param {String} shortcut
 * @param {String} methodName scopes.scopename.methodname or formname.methodname String to target the method to execute
 * @param {String} contextFilter form or element name ( ng only - specified by formName.elementName); only triggers the shortcut when on this form/element
 * @param arguments
 * @param {Boolean} consumeEvent if true then the shotcut will consume the event and the default browser behavior will not be executed (default false)
 */
function createShortcut(shortcut, methodName, contextFilter, arguments, consumeEvent) {}



/**
 * Remove a shortcut.
 *
 * @exampleas js_createShortcut(String, String, String, Object[],boolean)
 *
 * @param {String} shortcut
 */
function removeShortcut(shortcut) {}

/**
 * @clonedesc js_removeShortcut(String)
 * @exampleas js_removeShortcut(String)
 * @param {String} shortcut
 * @param {String} contextFilter form or element name ( ng only - specified by formName.elementName); only triggers the shortcut when on this form/element
 * @return
 */
function removeShortcut(shortcut, contextFilter) {}

/**
 * Show a form as popup panel, where the closeFormPopup can pass return a value to a dataprovider in the specified scope.
 * Can show relative to a component or at specified coordinates.
 * Show on specified location and backdrop is only supported in NGClient.
 *
 * @example
 * // Show a form as popup panel, where the closeFormPopup can pass return a value to a dataprovider in the specified scope.
 * plugins.window.showFormPopup(null,forms.orderPicker,foundset.getSelectedRecord(),"order_id");
 * // plugins.window.showFormPopup(null,forms.orderPicker,foundset.getSelectedRecord(),"order_id",-1,-1,100,100,true, false, onClose);
 * //
 * // function onClose(event) {application.output("Popup closed");}
 *
 * @param {RuntimeComponent} elementToShowRelatedTo element to show related to or null to center in screen
 * @param {RuntimeForm} form the form to show
 * @param {Object} scope the scope to put retval into
 * @param {String} dataproviderID the dataprovider of scope to fill
 * @param {Number} width popup width
 * @param {Number} height popup height
 * @param {Number} x popup x location
 * @param {Number} y popup y location
 * @param {Boolean} showBackdrop whatever to show a dimmed backdrop under the popup
 * @param {Boolean} doNotCloseOnClickOutside whether to close on not close the popup on clicking outside
 * @param {Function} onClose a callback function that is being triggered once the formpopup window is being closed
 */
function showFormPopup(elementToShowRelatedTo, form, scope, dataproviderID, width, height, x, y,
    showBackdrop, doNotCloseOnClickOutside, onClose) {}

    /**
     * Show a form as popup panel, where the closeFormPopup can pass return a value to a dataprovider in the specified scope. 
     * Can show relative to a component or at specified coordinates.
     * Show on specified location is only supported in NGClient.
     *
     * @example
     * // Show a form as popup panel, where the closeFormPopup can pass return a value to a dataprovider in the specified scope.
     * plugins.window.showFormPopup(null,forms.orderPicker,foundset.getSelectedRecord(),"order_id");
     * // plugins.window.showFormPopup(null,forms.orderPicker,foundset.getSelectedRecord(),"order_id",-1,-1,100,100);
     * // do call closeFormPopup(ordervalue) from the orderPicker form
     *
     * @param {RuntimeComponent} elementToShowRelatedTo element to show related to or null to center in screen
     * @param {RuntimeForm} form the form to show
     * @param {Object} scope the scope to put retval into
     * @param {String} dataproviderID the dataprovider of scope to fill
     * @param {Number} width popup width
     * @param {Number} height popup height
     * @param {Number} x popup x location
     * @param {Number} y popup y location
     */
function createFormPopup(form) {}
/**
 * Close the current form popup panel and assign the value to the configured data provider.
 * 
 * @example
 * // Show a form as popup panel, where the closeFormPopup can pass return a value to a dataprovider in the specified scope.
 * plugins.window.showFormPopup(null,forms.orderPicker,foundset.getSelectedRecord(),"order_id");
 * // plugins.window.showFormPopup(null,forms.orderPicker,foundset.getSelectedRecord(),"order_id",-1,-1,100,100,true, false, onClose);
 * //
 * // function onClose(event) {application.output("Popup closed");}
 *
 * @param {Object} retval return value for data provider
 */
function closeFormPopup(retval) {}

/**
 * Close/cancels the current form popup panel without assigning a value to the configured data provider.
 * 
 * @example
 * // Show a form as popup panel, where the closeFormPopup can pass return a value to a dataprovider in the specified scope.
 * plugins.window.showFormPopup(null,forms.orderPicker,foundset.getSelectedRecord(),"order_id");
 * // plugins.window.showFormPopup(null,forms.orderPicker,foundset.getSelectedRecord(),"order_id",-1,-1,100,100,true, false, onClose);
 * //
 * // function onClose(event) {application.output("Popup closed");}
 */
function cancelFormPopup() {}

/**
 * This is the base class for all menu items.
 */
function BaseMenuItem() {
    /**
     * Script the selection (emulate a mouse click) of the item.
     *
     * @example
     * // add a new menu to the menubar
     * var menubar = plugins.window.getMenuBar();
     * var menu = menubar.addMenu();
     * menu.text = "New Menu";
     * // alternatively create a popup menu
     * //var menu = plugins.window.createPopupMenu();
     * 
     * // add a menu item
     * var entry = menu.addMenuItem("menu entry", feedback);
     * // alternatively add a checkbox
     * //var entry = menu.addCheckBox("menu entry", feedback);
     * // or alternatively add a radiobutton
     * //var entry = menu.addRadioButton("menu entry", feedback);
     * 
     * // simulate a click on the entry
     * entry.doClick();
     */
    this.doClick = function() {}
    
    /**
     * Set the method for the menu item/checkbox/radiobutton.
     *
     * @example
     * // add a new menu to the menubar
     * var menubar = plugins.window.getMenuBar();
     * var menu = menubar.addMenu();
     * menu.text = "New Menu";
     * // alternatively create a popup menu
     * //var menu = plugins.window.createPopupMenu();
     * 
     * // add a menu item at the first position in the menu
     * var entry = menu.addMenuItem(0);
     * // alternatively add a checkbox at the first position
     * //var entry = menu.addCheckBox(0);
     * // or alternatively add a radiobutton at the first position
     * //var entry = menu.addRadioButton(0);
     * 
     * // disable the newly added entry
     * entry.enabled = false;
     * // give a name to the entry (the name is not visible anywhere)
     * entry.name = "my_name";
     * // make the entry selected (affects checkboxes and radiobuttons)
     * entry.selected = true;
     * // set the text of the entry
     * entry.text = "menu entry";
     * // set the callback method
     * entry.setMethod(feedback);
     * // set the arguments to be sent to the callback method
     * // (an array of elements which will be passed as arguments 5, 6 and so on to the callback method)
     * // the first 5 arguments are fixed: 
     * //   [0] item index
     * //   [1] parent item index
     * //   [2] isSelected boolean
     * //   [3] parent menu text
     * //   [4] menu text
     * entry.methodArguments = [17, "data"];
     * 
     * @param {Function} method
     * @param {Array} arguments
     * 
     * @returns BaseMenuItem
     */
    this.setMethod = function(method, arguments) {}


    /**
      * Set the accelerator key of the menu item/checkbox/radiobutton.
      *
      * @example
      * // add a new menu to the menubar
      * var menubar = plugins.window.getMenuBar();
      * var menu = menubar.addMenu();
      * menu.text = "New Menu";
      * // alternatively create a popup menu
      * //var menu = plugins.window.createPopupMenu();
      * 
      * // add a menu item
      * var entry = menu.addMenuItem("menu entry", feedback);
      * // alternatively add a checkbox
      * //var entry = menu.addCheckBox("menu entry", feedback);
      * // or alternatively add a radiobutton
      * //var entry = menu.addRadioButton("menu entry", feedback);
      * 
      * // define an accelerator for the menu entry
      * entry.setAccelerator("ctrl alt Y");
      * // also define a mnemonic
      * entry.setMnemonic("y");
      * // set a custom background color
      * entry.setBackgroundColor("#111111");
      * // set a custom foreground color
      * entry.setForegroundColor("#EE5555");
      * // set an icon
      * entry.setIcon("media:///yourimage.gif");
      * 
      * @param {String} accelerator
      * 
      * @returns BaseMenuItem
      */
     this.setAccelerator = function(accelerator) {}
     
     /**
       * Set the icon of the menu item/checkbox/radiobutton.
       *
       * @example
       * // add a new menu to the menubar
       * var menubar = plugins.window.getMenuBar();
       * var menu = menubar.addMenu();
       * menu.text = "New Menu";
       * // alternatively create a popup menu
       * //var menu = plugins.window.createPopupMenu();
       * 
       * // add a menu item
       * var entry = menu.addMenuItem("menu entry", feedback);
       * // alternatively add a checkbox
       * //var entry = menu.addCheckBox("menu entry", feedback);
       * // or alternatively add a radiobutton
       * //var entry = menu.addRadioButton("menu entry", feedback);
       * 
       * // define an accelerator for the menu entry
       * entry.setAccelerator("ctrl alt Y");
       * // also define a mnemonic
       * entry.setMnemonic("y");
       * // set a custom background color
       * entry.setBackgroundColor("#111111");
       * // set a custom foreground color
       * entry.setForegroundColor("#EE5555");
       * // set an icon
       * entry.setIcon("media:///yourimage.gif");
       * 
       * @param {Object} icon
       * 
       * @returns BaseMenuItem
       */
      this.setIcon = function(icon) {}
      
       /**
         * Set the icon of the menu item/checkbox/radiobutton.
         *
         * @example
         * // add a new menu to the menubar
         * var menubar = plugins.window.getMenuBar();
         * var menu = menubar.addMenu();
         * menu.text = "New Menu";
         * // alternatively create a popup menu
         * //var menu = plugins.window.createPopupMenu();
         * 
         * // add a menu item
         * var entry = menu.addMenuItem("menu entry", feedback);
         * // alternatively add a checkbox
         * //var entry = menu.addCheckBox("menu entry", feedback);
         * // or alternatively add a radiobutton
         * //var entry = menu.addRadioButton("menu entry", feedback);
         * 
         * // define an accelerator for the menu entry
         * entry.setAccelerator("ctrl alt Y");
         * // also define a mnemonic
         * entry.setMnemonic("y");
         * // set a custom background color
         * entry.setBackgroundColor("#111111");
         * // set a custom foreground color
         * entry.setForegroundColor("#EE5555");
         * // set an icon
         * entry.setIcon("media:///yourimage.gif");
         * 
         * @param {String} icon
         * 
         * @returns BaseMenuItem
         */
        this.setMnemonic = function(mnemonic) {}
        
        /**
          * Set the item visible.
          *
          * @example
          * // add a new menu to the menubar
          * var menubar = plugins.window.getMenuBar();
          * var menu = menubar.addMenu();
          * menu.text = "New Menu";
          * // alternatively create a popup menu
          * //var menu = plugins.window.createPopupMenu();
          * 
          * // add a menu item
          * var entry_one = menu.addMenuItem("an entry", feedback);
          * // add a checkbox
          * var entry_two = menu.addCheckBox("another entry", feedback);
          * // add a radiobutton
          * var entry_three = menu.addRadioButton("yet another entry", feedback);
          * 
          * // hide the menu item
          * entry_one.setVisible(false);
          * // make sure the checkbox is visible
          * entry_two.setVisible(true);
          * // hide the radiobutton
          * entry_three.setVisible(false);
          * 
          * @param {Boolean} visible
          * 
          * @returns BaseMenuItem
          */
         this.setVisible = function(visible) {}
         
         /**
          * Set the background color of the menu item/checkbox/radiobutton.
          * 
         * @param {String} bgColor
         * 
          */
         this.setBackgroundColor = function(bgColor) {}

         /**
          * Set the foreground color of the menu item/checkbox/radiobutton.
          * 
         * @param {String} fgColor
         * 
          */
         this.setForegroundColor = function(fgColor) {}

         /**
          * Sets the value for the specified client property key of the menu item/checkbox/radiobutton.
          *
         * @example
         * // add a new menu to the menubar
         * var menubar = plugins.window.getMenuBar();
         * var menu = menubar.addMenu();
         * menu.text = "New Menu";
         * // alternatively create a popup menu
         * //var menu = plugins.window.createPopupMenu();
         * 
         * // add a menu item
         * var entry = menu.addMenuItem("menu entry", feedback);
         * // alternatively add a checkbox
         * //var entry = menu.addCheckBox("menu entry", feedback);
         * // or alternatively add a radiobutton
         * //var entry = menu.addRadioButton("menu entry", feedback);
         * 
         * // NOTE: Depending on the operating system, a user interface property name may be available.
         * // set the tooltip of the menu item/checkbox/radiobutton via client properties
         * // keep the original tooltip in a form or global variable
         * originalTooltip = entry.getClientProperty("ToolTipText");
         * entry.putClientProperty("ToolTipText", "changed tooltip");
         * 
         * // later restore the original tooltip from the variable
         * //var menubar = plugins.window.getMenuBar();
         * //var menuIndex = menubar.getMenuIndexByText("New Menu");
         * //var menu = menubar.getMenu(menuIndex);
         * //var entry = menu.getItem(0);
         * //entry.putClientProperty("ToolTipText", originalTooltip);
          * 
          * @param {Object} key
          * @param {Object} value
          */
         this.putClientProperty = function(key, value) {}

         /**
          * Gets the specified client property for the menu item/checkbox/radiobutton based on a key.
          *
          * @sample
          * // add a new menu to the menubar
          * var menubar = plugins.window.getMenuBar();
          * var menu = menubar.addMenu();
          * menu.text = "New Menu";
          * // alternatively create a popup menu
          * //var menu = plugins.window.createPopupMenu();
          * 
          * // add a menu item
          * var entry = menu.addMenuItem("menu entry", feedback);
          * // alternatively add a checkbox
          * //var entry = menu.addCheckBox("menu entry", feedback);
          * // or alternatively add a radiobutton
          * //var entry = menu.addRadioButton("menu entry", feedback);
          * 
          * // NOTE: Depending on the operating system, a user interface property name may be available.
          * // set the tooltip of the menu item/checkbox/radiobutton via client properties
          * // keep the original tooltip in a form or global variable
          * originalTooltip = entry.getClientProperty("ToolTipText");
          * entry.putClientProperty("ToolTipText", "changed tooltip");
          * 
          * // later restore the original tooltip from the variable
          * //var menubar = plugins.window.getMenuBar();
          * //var menuIndex = menubar.getMenuIndexByText("New Menu");
          * //var menu = menubar.getMenu(menuIndex);
          * //var entry = menu.getItem(0);
          * //entry.putClientProperty("ToolTipText", originalTooltip);
          * 
          * @param {Object} key
          */
         this.getClientProperty = function(key){}
}

/**
 * The checbox api. 
 * extends BaseMenuItem
 */
function CheckBox() {
    
}

/**
 * The base menu for all menu types.
 */
function BaseMenu() {
    /**
       * Add a checkbox at the selected index (starting at 0) or at the end.
       *
       * @example
       * // add a new menu to the menubar
       * var menubar = plugins.window.getMenuBar();
       * var menu = menubar.addMenu();
       * menu.text = "New Menu";
       * // alternatively create a popup menu
       * //var menu = plugins.window.createPopupMenu();
       *
       * // when you don't define an index the checkbox will be added at the last position
       * // this is what you usually do to build a new menu
       * // minimum settings are the text and method
       * // the method can be a global or form method
       * // be sure to enter the method WITHOUT '()' at the end
       * menu.addCheckBox("checkbox", feedback_checkbox);
       * // add a checkbox with an icon
       * menu.addCheckBox("checkbox with icon", feedback_checkbox, "media:///yourimage.gif");
       * //var pic_bytes = plugins.file.readFile("/path/to/image.jpg");
       * //menu.addCheckBox("checkbox with icon", feedback_checkbox, pic_bytes);
       * // add a checkbox with a mnemonic
       * menu.addCheckBox("checkbox with mnemonic", feedback_checkbox, "media:///yourimage.gif", "c");
       * // add a disabled checkbox
       * menu.addCheckBox("checkbox disabled", feedback_checkbox, "media:///yourimage.gif", "d", false);
       * // add a checkbox with text aligned to the right
       * menu.addCheckBox("align right", feedback_checkbox, null, null, true, MenuItem.ALIGN_RIGHT);
       *
       * // add a checkbox at a given index (checkbox properties must be configured after creation)
       * // indexes start at 0 (zero) so index 2 is in fact position 3
       * var chk = menu.addCheckBox(2);
       * chk.text = "checkbox at index";
       * chk.setMethod(feedback_checkbox);
       *
       * @param {String} [name] the checkbox text; this can be also html if enclosed between html tags
       * @param {Function} [feedback_item] the feedback function
       * @param {Object} [icon] the checkbox icon (can be an image URL or the image content byte array)
       * @param {String} [mnemonic] the checkbox mnemonic
       * @param {Boolean} [enabled] the enabled state of the checkbox
       * @param {Number} [align] the alignment type
       *
       * @return CheckBox
       */
      this.addCheckBox = function(name, feedback_item, icon, mnemonic, enabled, align) {};
}
/**
 * This is the the object that creates a popupmenu
 * extends BaseMenu
 */
function Popup() {
 
}
