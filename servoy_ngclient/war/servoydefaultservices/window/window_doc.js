
/**
 * Creates a new popup menu, either empty, or initialized from an existing JSMenu.
 * 
* @example
* // create a popup menu
* // var menu = plugins.window.createPopupMenu(menus.getMenu('contextMenu'), feedback);
* var menu = plugins.window.createPopupMenu();
* 
* // add a menu item
* menu.addMenuItem("an entry", callback);
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
 * @param {JSMenu} [menu] The JSMenu whose structure will be used to initialize the popup menu.
 * @param {Function} [callback] The menu item click handler that will be set on all popup menu items. The callback signature is: function(itemIndex:number, parentIndex: number, isSelected: boolean, parentMenu: String, menu: String)
 * 
 * @return {CustomType<window.Popup>} A new popup menu instance, optionally initialized with the provided JSMenu structure and callback function.
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
 * @param {String} shortcut The keyboard shortcut combination (e.g., 'control shift I', 'F4', 'NUMPAD8') to trigger the action.
 * @param {Function} callback scopes.scopename.methodname or formname.methodname String to target the method to execute
 * @param {String} [contextFilter] Form or element name ( ng only - specified by formName.elementName); only triggers the shortcut when on this form/element
 * @param {Array<Object>} [arguments] An optional array of arguments to pass to the method when the shortcut is executed.
 * @param {Boolean} [consumeEvent] If true then the shotcut will consume the event and the default browser behavior will not be executed (default false)
 * 
 * @return {Boolean} True if the shortcut was successfully created; otherwise, false.
 */
function createShortcut(shortcut, callback, contextFilter, arguments, consumeEvent) {}



/**
 * Remove a shortcut.
 *
 * @exampleas js_createShortcut(String, String, String, Object[],boolean)
 *
 * @param {String} shortcut The keyboard shortcut combination to be removed.
 */
function removeShortcut(shortcut) {}

/**
 * @clonedesc js_removeShortcut(String)
 * @exampleas js_removeShortcut(String)
 * @param {String} shortcut The keyboard shortcut combination to be removed.
 * @param {String} [contextFilter] form or element name ( ng only - specified by formName.elementName); only triggers the shortcut when on this form/element
 * 
 * @return {Boolean} True if the shortcut was successfully removed; otherwise, false.
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
 * @param {Runtimecomponent} component Element to show related to or null to center in screen
 * @param {Form} form the form to show
 * @param {Object} scope the scope to put retval into
 * @param {String} dataProviderID the dataprovider of scope to fill
 * @param {Number} [width] popup width
 * @param {Number} [height] popup height
 * @param {Number} [x] popup x location
 * @param {Number} [y] popup y location
 * @param {Boolean} [showBackdrop] whatever to show a dimmed backdrop under the popup
 * @param {Boolean} [doNotCloseOnClickOutside] whether to close on not close the popup on clicking outside
 * @param {Function} [onClose] a callback function that is being triggered once the formpopup window is being closed
 */
function showFormPopup(component, form, scope, dataproviderID, width, height, x, y,
    showBackdrop, doNotCloseOnClickOutside, onClose) {}

/**
 * Create a form popup that can be filled with data and shown.
 *
 * @example
 * plugins.window.createFormPopup(forms.orderPicker).show();
 *
 * @param {Form} form The form to show
 *
 * @return {CustomType<window.FormPopup>} A FormPopup instance that can be populated with data and displayed.
 */
function createFormPopup(form) {}


/**
 *  It will return a FormPopup with the form passed as an argument or null.
 *
 * @example
 * plugins.window.getFormPopup(forms.orderPicker)
 *
 * @param {Form} form The form instance to be used in the popup.
 * 
 * @return {CustomType<window.FormPopup>} A FormPopup instance containing the specified form, or null if the popup could not be created.
 */
function getFormPopup(form) {}

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
 *  @param {Object} retval Return value for data provider
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
 * This is configuration object to show a FormPopup, created by the call createFormPopup(form)
 */
function FormPopup() {

    /**
     * Returns the component.
     *
     * @example
     * plugins.window.createFormPopup(forms.orderPicker).component();
     *
     * @return {RuntimeComponent} The FormPopup itself if it's used as a setter or the component if no argument is given
     */
    this.component = function() {}
	
	/**
	  * Set component form popup will be shown relative to. If null, will use coordinates or show at screen center.
	  *
	  * @example
	  * plugins.window.createFormPopup(forms.orderPicker).component(elements.myelement).show();
	  *
	  * @param {RuntimeComponent} component the form to show
	  *
	  * @return {CustomType<window.FormPopup>} The FormPopup itself if it's used as a setter or the component if no argument is given
	  */
	  this.component_component = function(component) {}

    /**
     Returns the width.
     *
     * @example
     * plugins.window.createFormPopup(forms.orderPicker).width(100).show();
     *
     * @return {Number} The FormPopup itself if it's used as a setter or the width if no argument is given
     *
     */
    this.width = function() {}
	
	/**
	  * Set form popup width. If not set, form design width will be used.
	  *
	  * @example
	  * plugins.window.createFormPopup(forms.orderPicker).width(100).show();
	  *
	  * @param {Number} width form popup width
	  *
	  * @return {CustomType<window.FormPopup>} The FormPopup itself if it's used as a setter or the width if no argument is given
	  *
	  */
	 this.width_width = function(width) {}

    /**
     * Returns the height.
     *
     * @example
     * plugins.window.createFormPopup(forms.orderPicker).height(100).show();
     *
     * @return {Number} The FormPopup itself if it's used as a setter or the height if no argument is given
     */
    this.height = function() {}

	/**
	  * Set form popup height. If not set, form design height will be used.
	  *
	  * @example
	  * plugins.window.createFormPopup(forms.orderPicker).height(100).show();
	  *
	  * @param {Number} height form popup height
	  *
	  * @return {CustomType<window.FormPopup>} The FormPopup itself if it's used as a setter or the height if no argument is given
	  */
	 this.height_height = function(height) {}

    /**
     * Returns the x.
     *
     * @example
     * plugins.window.createFormPopup(forms.orderPicker).x(100).show();
     *
     * @return {Number} The FormPopup itself if it's used as a setter or the x if no argument is given
     *
     */
    this.x = function() {}
	
	/**
	  * Set form popup x location. The priority sequence for location is: related element, set location, center of screen.
	  *
	  * @example
	  * plugins.window.createFormPopup(forms.orderPicker).x(100).show();
	  *
	  * @param {Number} x form popup x location
	  *
	  * @return {CustomType<window.FormPopup>} The FormPopup itself if it's used as a setter or the x if no argument is given
	  *
	  */
	 this.x_x = function(x) {}	

    /**
     * Returns the y value
     *
     * @example
     * plugins.window.createFormPopup(forms.orderPicker).y(100).show();
     *
     * @return {Number} The FormPopup itself if it's used as a setter or the y value if no argument is given
     *
     */
    this.y = function() {}

	/**
	  * Set form popup y location. The priority sequence for location is: related element, set location, center of screen.
	  *
	  * @example
	  * plugins.window.createFormPopup(forms.orderPicker).y(100).show();
	  *
	  * @param {Number} y form popup y location
	  *
	  * @return {CustomType<window.FormPopup>} The FormPopup itself if it's used as a setter or the y value if no argument is given
	  *
	  */
	 this.y_y = function(y) {}	
	
    /**
     * Returns the backdrop value
     *
     * @example
     * plugins.window.createFormPopup(forms.orderPicker).showBackdrop(true).show();
     *
     * @return {Boolean} The FormPopup itself if it's used as a setter or the showBackdrop value if no argument is given
     *
     */
    this.showBackdrop = function(showBackdrop) {}
	
	/**
	  * Set whether backdrop will be shown. Default value is false.
	  *
	  * @example
	  * plugins.window.createFormPopup(forms.orderPicker).showBackdrop(true).show();
	  *
	  * @param {Boolean} showBackdrop form popup showBackdrop
	  *
	  * @return {CustomType<window.FormPopup>} The FormPopup itself if it's used as a setter or the showBackdrop value if no argument is given
	  *
	  */
	 this.showBackdrop_showBackdrop = function(showBackdrop) {}

    /**
     * Returns the datprovider value
     *
     * @example
     * plugins.window.createFormPopup(forms.orderPicker).dataprovider('myid').scope(foundset.getSelectedRecord()).show();
     *
     * @return {String} The FormPopup itself if it's used as a setter or the dataprovider value if no argument is given
     *
     */
    this.dataprovider = function() {}
	
	/**
	  * Set form popup dataprovider that will be set. If this is set, also scope needs to be specified.
	  *
	  * @example
	  * plugins.window.createFormPopup(forms.orderPicker).dataprovider('myid').scope(foundset.getSelectedRecord()).show();
	  *
	  * @param {String} dataprovider form popup dataprovider
	  *
	  * @return {CustomType<window.FormPopup>} The FormPopup itself if it's used as a setter or the dataprovider value if no argument is given
	  *
	  */
	 this.dataprovider_dataprovider = function(dataprovider) {}

    /**
     * Returns the onClose function value
     *
     * @example
     * plugins.window.createFormPopup(forms.orderPicker).dataprovider('myid').onClose(closePopupFunction).show();
     *
     * @return {Function} The FormPopup itself if it's used as a setter or the function value if no argument is given
     *
     */
    this.onClose = function() {}
	
	/**
	  * Set the onclose function that is called when the closeFormPopup is called.
	  * This onClose will get a JSEvent as the first argument, and the return value that is given to the closeFormPopup(retvalue) call.
	  *
	  * @example
	  * plugins.window.createFormPopup(forms.orderPicker).dataprovider('myid').onClose(closePopupFunction).show();
	  *
	  * @param {Function} onClose Function that needs to be called when closed
	  *
	  * @return {CustomType<window.FormPopup>} The FormPopup itself if it's used as a setter or the function value if no argument is given
	  *
	  */
	 this.onClose_onClose = function(func) {}

    /**
     * Returns the scope object
     *
     * @example
     * plugins.window.createFormPopup(forms.orderPicker).dataprovider('myid').scope(foundset.getSelectedRecord()).show();
     *
     * @return {Object} The FormPopup itself if it's used as a setter or the scope object if no argument is given
     *
     */
    this.scope = function() {}
	
	/**
	  * Set form popup scope that will be modified. If this is set, also dataprovider needs to be specified.
	  *
	  * @example
	  * plugins.window.createFormPopup(forms.orderPicker).dataprovider('myid').scope(foundset.getSelectedRecord()).show();
	  *
	  * @param {Object} scope Form popup scope to modify
	  *
	  * @return {CustomType<window.FormPopup>} The FormPopup itself if it's used as a setter or the scope object if no argument is given
	  *
	  */
	 this.scope_scope = function(scope) {}

    /**
     * Show form popup using parameters that were set
     *
     * @example
     * plugins.window.createFormPopup(forms.orderPicker).x(100).y(100).width(100).height(100).showBackdrop(true).show();
     *
     */
    this.show = function() {}
	
	/**
	 * Close/Cancel the current form and his children if applicable.
	 *
	 * @example
	 * plugins.window.getFormPopup(forms.orderPicker).cancel();
	 *
	 */
	this.cancel = function() {}
	
    /**
     * Create a form popup that can be filled with data and shown.
     *
     * @example
     * plugins.window.createFormPopup(forms.orderPicker).show();
     *
     * @param {form} form the form to show
     *
     * @return {CustomType<window.FormPopup>} FormPopup
     */
	this.createFormPopup = function(form) {}
}

/**
 * This is the base class for all menu items.
 */
function Menu() {
    /**
     * Script the selection (emulate a mouse click) of the menu.
     *
     * @example
     * // simulate a click on the popup menu
     * menu.doClick();
     */
    this.doClick = function() {}
    
    /**
     * Set the the selected menu enabled or disabled.
     *
     * @example
     * var popup = plugins.window.createPopupMenu();
     * var menu = popup.addMenu();
     * // set the menu's text
     * menu.text = "New Menu";
     * // disable the menu
     * menu.setEnabled(false);
     * // set a mnemonic
     * menu.setMnemonic("u");
     * // add an icon to the menu
     * menu.setIcon("media:///yourimage.gif");
     * 
     * @param {Boolean} enabled The enabled state; true to enable the menu, false to disable.
     */
    this.setEnabled = function(enabled) {}

   /**
    * Set the icon of the menu.
    *
    * @example
    * var popup = plugins.window.createPopupMenu();
    * var menu = popup.addMenu();
    * // set the menu's text
    * menu.text = "New Menu";
    * // disable the menu
    * menu.setEnabled(false);
    * // set a mnemonic
    * menu.setMnemonic("u");
    * // add an icon to the menu
    * menu.setIcon("media:///yourimage.gif");
    * 
    * @param {Object} icon The icon to be used, which can be a media URL or an object representing the icon.
    */
    this.setIcon = function(icon) {}

   /**
    * Set the mnemonic of the selected menu.
    *
    * @example
    * var popup = plugins.window.createPopupMenu();
    * var menu = popup.addMenu();
    * // set the menu's text
    * menu.text = "New Menu";
    * // disable the menu
    * menu.setEnabled(false);
    * // set a mnemonic
    * menu.setMnemonic("u");
    * // add an icon to the menu
    * menu.setIcon("media:///yourimage.gif");
    * 
    * @param {String} mnemonic The mnemonic key used for keyboard shortcuts.
    */
    this.setMnemonic = function(mnemonic) {}
}

/**
 * This is the base class for all menu items.
 */
function MenuItem() {
    /**
     * Script the selection (emulate a mouse click) of the item.
     *
     * @example
     * // create a popup menu
     * var menu = plugins.window.createPopupMenu();
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
     * @param {Function} method The callback function to be executed when the menu item is activated.
     * @param {Array<Object>} [arguments] An array of additional arguments to be passed to the callback function. The first five arguments are fixed: [0] item index, [1] parent item index, [2] isSelected boolean, [3] parent menu text, [4] menu text.
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
      * @param {String} accelerator The accelerator key string, e.g. "ctrl alt Y".
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
       * @param {Object} icon The icon to set
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
         * @param {String} mnemonic The mnemonic key to be used as a shortcut indicator for the menu item.
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
          * @param {Boolean} visible The visibility state of the menu item.
          */
         this.setVisible = function(visible) {}
         
         /**
          * Set the background color of the menu item/checkbox/radiobutton.
          * 
          * @param {String} bgColor The background color as a string (e.g., "#111111").
          * 
          */
         this.setBackgroundColor = function(bgColor) {}

         /**
          * Set the foreground color of the menu item/checkbox/radiobutton.
          * 
          * @param {String} fgColor The foreground color as a string (e.g., "#EE5555").
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
          * @param {Object} key The key for the client property.
          * @param {Object} value The value to set for the client property.
          */
         this.putClientProperty = function(key, value) {}

         /**
          * Gets the specified client property for the menu item/checkbox/radiobutton based on a key.
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
          * @param {Object} key The key for the client property.
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
 * The radio api. 
 * extends BaseMenuItem
 */
function RadioButton() {
    
}

/**
 * The base menu for all menu types.
 */
function BaseMenu() {

    /**
     *
     * Add a submenu with given name.
     *
     * @example
     * // add a new menu to the menubar
     * var menubar = plugins.window.getMenuBar();
     * var menu = menubar.addMenu();
     * menu.text = "New Menu";
     * // alternatively create a popup menu
     * //var menu = plugins.window.createPopupMenu();
     *
     * // add a first submenu
     * var submenu1 = menu.addMenu("submenu 1");
     * submenu1.addMenuItem("sub item 1 - 1", feedback_item);
     * // add a submenu as child of the first submenu
     * var submenu1_2 = submenu1.addMenu("submenu 1 - 2");
     * submenu1_2.addMenuItem("sub item 1 - 2 - 1", feedback_item);
     * // add another submenu as a child of the first submenu
     * var submenu1_3 = submenu1.addMenu("submenu 1 - 3");
     * submenu1_3.addMenuItem("sub item 1 - 3 - 1", feedback_item);
     * // add a submenu to the second submenu of the first submenu
     * var submenu1_3_2 = submenu1_2.addMenu("submenu 1 - 2 - 2");
     * submenu1_3_2.addMenuItem("sub item 1 - 2 - 2 - 1", feedback_item);
     * // add a submenu directly to the menu, at the first position
     * var submenu0 = menu.addMenu(0);
     * submenu0.text = "submenu 0";
     * submenu0.addMenuItem("sub item 0 - 1", feedback_item);
     *
     * @param {String} [name] the text of the submenu; this can be also html if enclosed between html tags
     *
     * @return Menu
     */
    this.addMenu = function(name) {}

    /**
     * Add a checkbox at the selected index (starting at 0) or at the end.
     *
     * @example
     * // create a popup menu
     * var menu = plugins.window.createPopupMenu();
     *
     * // when you don't define an index the item will be added at the last position
     * // this is what you usually do to build a new menu
     * // minimum settings are the text and method
     * // the method can be a global or form method
     * // be sure to enter the method WITHOUT '()' at the end
     * menu.addMenuItem("item", feedback_item);
     * // add an item with an icon
     * menu.addMenuItem("item with icon", feedback_item, "media:///yourimage.gif");
     * //var pic_bytes = plugins.file.readFile("/path/to/image.jpg");
     * //menu.addMenuItem("item with icon", feedback_item, pic_bytes);
     * // add an item with a mnemonic
     * menu.addMenuItem("item with mnemonic", feedback_item, "media:///yourimage.gif", "i");
     * //add an entry with fontawesome icon. Only supported in NGClient!
     * menu.addMenuItem("an entry", this.feedback, 'fas fa-trash-alt');
     * // add a disabled item
     * menu.addMenuItem("disabled item", feedback_item, "media:///yourimage.gif", "d", false);
     * // add an item with text aligned to the right
     * menu.addMenuItem("align right", feedback_item, null, null, true, SM_ALIGNMENT.RIGHT);
     *
     * // add an item at a given index (item properties must be configured after creation)
     * // indexes start at 0 (zero) so index 2 is in fact position 3
     * var item = menu.addMenuItem(2);
     * item.text = "item at index";
     * item.setMethod(feedback_item);
     *
     * @param {String} [name] the menu text; this can be also html if enclosed between html tags
     * @param {Function} [feedback_item] the feedback function
     * @param {Object} [icon] the menu icon (can be an image URL or the image content byte array)
     * @param {String} [mnemonic] the menu mnemonic
     * @param {Boolean} [enabled] the enabled state of the menu
     * @param {Number} [align] the alignment type
     *
     * @return MenuItem
     */
     this.addMenuItem = function(name, feedback_item, icon, mnemonic, enabled, align) {};

    /**
     * Add a checkbox at the selected index (starting at 0) or at the end.
     *
     * @example
     * // create a popup menu
     * var menu = plugins.window.createPopupMenu();
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
 
      /**
       * Add a radiobutton at the selected index (starting at 0) or at the end.
        *
        * @example
        * // create a popup menu
        * var menu = plugins.window.createPopupMenu();
        *
        * // when you don't define an index the radiobutton will be added at the last position
        * // this is what you usually do to build a new menu
        * // minimum settings are the text and method
        * // the method can be a global or form method
        * // be sure to enter the method WITHOUT '()' at the end
        * menu.addRadioButton("radio", feedback_radiobutton);
        * // add a radiobutton with an icon
        * menu.addRadioButton("radio with icon", feedback_radiobutton, "media:///yourimage.gif");
        * //var pic_bytes = plugins.file.readFile("/path/to/image.jpg");
        * //menu.addRadioButton("radio with icon", feedback_radiobutton, pic_bytes);
        *
        * // add a new radiobutton group
        * // a group will 'bind' all added radiobuttons after the group together
        * // as a result checking one item will uncheck the other
        * // if no group is added, a group is created automatically when the first radiobutton is added to the menu
        * // so in this case we will have two groups, one with the radiobuttons added until now and one with the ones added from now on
        * menu.addRadioGroup();
        *
        * // add a radiobutton with a mnemonic
        * menu.addRadioButton("radio with mnemonic", feedback_radiobutton, "media:///yourimage.gif", "i");
        * // add a disabled radiobutton
        * menu.addRadioButton("disabled radio", feedback_radiobutton, "media:///yourimage.gif", "d", false);
        * // add a radiobutton with text aligned to the right
        * menu.addRadioButton("align right", feedback_radiobutton, null, null, true, SM_ALIGNMENT.RIGHT);
        * // add a radiobutton at a given index (item properties must be configured after creation)
        * // indexes start at 0 (zero) so index 2 is in fact position 3
        * var rd = menu.addRadioButton(2);
        * rd.text = "radio at index";
        * rd.setMethod(feedback_item);
        *
        * @param {String} name the radio button text; this can be also html if enclosed between html tags
        * @param {Function} feedback_item the feedback function
        * @param {Object} icon the radio button icon (can be an image URL or the image content byte array)
        * @param {String} mnemonic the radio button mnemonic
        * @param {Boolean} enabled the enabled state of radio button
        * @param {Number} align the alignment type
        *
        * @return RadioButton
      */
      this.addRadioButton = function(name, feedback_item, icon, mnemonic, enabled, align) {};


      /**
       * Add a radiogroup for radiobuttons. A radiogroup groups together all radiobuttons that are added
       * after the group is added. From all radiobuttons that belong to the same radiogroup only one can be
       * checked at a time.
       *
       * If no radiogroup is added, one is created automatically when the first radiobutton is added.
       *
      * @example
      * // create a popup menu
      * var menu = plugins.window.createPopupMenu();
      *
      * // when you don't define an index the radiobutton will be added at the last position
      * // this is what you usually do to build a new menu
      * // minimum settings are the text and method
      * // the method can be a global or form method
      * // be sure to enter the method WITHOUT '()' at the end
      * menu.addRadioButton("radio", feedback_radiobutton);
      * // add a radiobutton with an icon
      * menu.addRadioButton("radio with icon", feedback_radiobutton, "media:///yourimage.gif");
      * //var pic_bytes = plugins.file.readFile("/path/to/image.jpg");
      * //menu.addRadioButton("radio with icon", feedback_radiobutton, pic_bytes);
      *
      * // add a new radiobutton group
      * // a group will 'bind' all added radiobuttons after the group together
      * // as a result checking one item will uncheck the other
      * // if no group is added, a group is created automatically when the first radiobutton is added to the menu
      * // so in this case we will have two groups, one with the radiobuttons added until now and one with the ones added from now on
      * menu.addRadioGroup();
      *
      * // add a radiobutton with a mnemonic
      * menu.addRadioButton("radio with mnemonic", feedback_radiobutton, "media:///yourimage.gif", "i");
      * // add a disabled radiobutton
      * menu.addRadioButton("disabled radio", feedback_radiobutton, "media:///yourimage.gif", "d", false);
      * // add a radiobutton with text aligned to the right
      * menu.addRadioButton("align right", feedback_radiobutton, null, null, true, SM_ALIGNMENT.RIGHT);
      * // add a radiobutton at a given index (item properties must be configured after creation)
      * // indexes start at 0 (zero) so index 2 is in fact position 3
      * var rd = menu.addRadioButton(2);
      * rd.text = "radio at index";
      * rd.setMethod(feedback_item);
      */
      this.addRadioGroup = function() {};
      
      
    /**
     * Add the separator at the selected index (starting at 0) or at the end (empty).
     *
     * @example
     * // create a popup menu
     * var menu = plugins.window.createPopupMenu();
     *
     * // add an item and a checkbox
     * menu.addMenuItem("item", feedback_item);
     * menu.addCheckBox("checkbox", feedback_checkbox);
     * // add a separator
     * menu.addSeparator();
     * // add a radiobutton. it will be separated from the rest of the control by the separator
     * menu.addRadioButton("radio", feedback_radiobutton);
     * // add another separator between the item and the checkbox
     * menu.addSeparator(1);
     * 
     * @param {Number} [index] the index where to add the separator; if not defined, the separator is added at the end
     *
     */
    this.addSeparator = function(index) {}
    

    /**
     * Get the checkbox at the selected index (starting at 0).
     *
     * @example
     * // a popup menu
     * var menu = plugins.window.createPopupMenu();
     *
     * // add two radiobuttons
     * menu.addRadioButton("radio one", feedback_radiobutton);
     * menu.addRadioButton("radio two", feedback_radiobutton);
     * // add a menu item, with a separator before it
     * menu.addSeparator();
     * menu.addMenuItem("item", feedback_item);
     * // add a checkbox, with a separator before it
     * menu.addSeparator();
     * menu.addCheckBox("check", feedback_checkbox);
     * // add a submenu with an item under it
     * var submenu = menu.addMenu("submenu");
     * submenu.addMenuItem("subitem", feedback_item);
     *
     * // depending on some state, update the entries in the menu
     * var some_state = true;
     * if (some_state) {
     *  // select the first radiobutton
     *  menu.getRadioButton(0).selected = true;
     * } else {
     *  // select the first radiobutton
     *  menu.getRadioButton(1).selected = true;
     * }
     * // enable/disable the menu item
     * // remember to include the separators also when counting the index
     * menu.getItem(3).enabled = !some_state;
     * // select/unselect the checkbox
     * // remember to include the separators also when counting the index
     * menu.getCheckBox(5).selected = some_state;
     * // change the text of the submenu and its item
     * application.output(menu.getItemCount());
     * if (some_state) {
     *  menu.getMenu(6).text = "some state";
     *  menu.getMenu(6).getItem(0).text = "some text";
     * }
     * else {
     *  menu.getMenu(6).text = "not some state";
     *  menu.getMenu(6).getItem(0).text = "other text";
     * }
     *
     * @param {Number} index
     * 
     * @return CheckBox 
     *
     */
    this.getCheckBox = function(index) {}

    /**
     * Get the item at the selected index (starting at 0).
     *
     * @example
     * // a popup menu
     * var menu = plugins.window.createPopupMenu();
     *
     * // add two radiobuttons
     * menu.addRadioButton("radio one", feedback_radiobutton);
     * menu.addRadioButton("radio two", feedback_radiobutton);
     * // add a menu item, with a separator before it
     * menu.addSeparator();
     * menu.addMenuItem("item", feedback_item);
     * // add a checkbox, with a separator before it
     * menu.addSeparator();
     * menu.addCheckBox("check", feedback_checkbox);
     * // add a submenu with an item under it
     * var submenu = menu.addMenu("submenu");
     * submenu.addMenuItem("subitem", feedback_item);
     *
     * // depending on some state, update the entries in the menu
     * var some_state = true;
     * if (some_state) {
     *  // select the first radiobutton
     *  menu.getRadioButton(0).selected = true;
     * } else {
     *  // select the first radiobutton
     *  menu.getRadioButton(1).selected = true;
     * }
     * // enable/disable the menu item
     * // remember to include the separators also when counting the index
     * menu.getItem(3).enabled = !some_state;
     * // select/unselect the checkbox
     * // remember to include the separators also when counting the index
     * menu.getCheckBox(5).selected = some_state;
     * // change the text of the submenu and its item
     * application.output(menu.getItemCount());
     * if (some_state) {
     *  menu.getMenu(6).text = "some state";
     *  menu.getMenu(6).getItem(0).text = "some text";
     * }
     * else {
     *  menu.getMenu(6).text = "not some state";
     *  menu.getMenu(6).getItem(0).text = "other text";
     * }
     *
     * @param {Number} index
     * 
     * @return MenuItem
     */
    this.getItem = function(index) {}

    /**
     * Get the number of items in the menu.
     *
     * @example
     * // REMARK: indexes start at 0, disabled items, non visible items and seperators are counted also
     * // REMARK: this is especially important when getting items by the index
     * // create a popup menu
     * var menu = plugins.window.createPopupMenu();
     *
     * // add two radiobuttons
     * menu.addRadioButton("radio one", feedback_radiobutton);
     * menu.addRadioButton("radio two", feedback_radiobutton);
     * // add a checkbox
     * menu.addCheckBox("check", feedback_checkbox);
     * // add a menu item
     * menu.addMenuItem("item", feedback_item);
     * // add another menu item
     * menu.addMenuItem("item 2", feedback_item);
     *
     * // remove the last item
     * menu.removeItem(menu.getItemCount() - 1);
     * 
     * @return Number
     */
    this.getItemCount = function() {}

    /**
     * Retrieve the index of the item by text.
     *
     * @example
     * // create a popup menu
     * var menu = plugins.window.createPopupMenu();
     *
     * // add two radiobuttons
     * menu.addRadioButton("radio one", feedback_radiobutton);
     * menu.addRadioButton("radio two", feedback_radiobutton);
     * // add a checkbox
     * menu.addCheckBox("check", feedback_checkbox);
     * // add a menu item
     * menu.addMenuItem("item", feedback_item);
     * // add another menu item
     * menu.addMenuItem("item 2", feedback_item);
     *
     * // find the index of the checkbox
     * var idx = menu.getItemIndexByText("check");
     * // remove the checkbox by its index
     * menu.removeItem(idx);
     * // remove both radiobuttons by their indices
     * menu.removeItem([0, 1]);
     * // remove all remaining entries
     * menu.removeAllItems();
     * // add back an item
     * menu.addMenuItem("new item", feedback_item);
     *
     * @param {String} text
     * 
     * @return {Number}
     */
    this.getItemIndexByText = function(text) {}


    /**
     * Get the radiobutton at the selected index (starting at 0).
     *
     * @example
     * // a popup menu
     * var menu = plugins.window.createPopupMenu();
     *
     * // add two radiobuttons
     * menu.addRadioButton("radio one", feedback_radiobutton);
     * menu.addRadioButton("radio two", feedback_radiobutton);
     * // add a menu item, with a separator before it
     * menu.addSeparator();
     * menu.addMenuItem("item", feedback_item);
     * // add a checkbox, with a separator before it
     * menu.addSeparator();
     * menu.addCheckBox("check", feedback_checkbox);
     * // add a submenu with an item under it
     * var submenu = menu.addMenu("submenu");
     * submenu.addMenuItem("subitem", feedback_item);
     *
     * // depending on some state, update the entries in the menu
     * var some_state = true;
     * if (some_state) {
     *  // select the first radiobutton
     *  menu.getRadioButton(0).selected = true;
     * } else {
     *  // select the first radiobutton
     *  menu.getRadioButton(1).selected = true;
     * }
     * // enable/disable the menu item
     * // remember to include the separators also when counting the index
     * menu.getItem(3).enabled = !some_state;
     * // select/unselect the checkbox
     * // remember to include the separators also when counting the index
     * menu.getCheckBox(5).selected = some_state;
     * // change the text of the submenu and its item
     * application.output(menu.getItemCount());
     * if (some_state) {
     *  menu.getMenu(6).text = "some state";
     *  menu.getMenu(6).getItem(0).text = "some text";
     * }
     * else {
     *  menu.getMenu(6).text = "not some state";
     *  menu.getMenu(6).getItem(0).text = "other text";
     * }
     *
     * @param {Number} index
     * 
     * @return RadioButton
     */
    this.getRadioButton = function(index) {}

    /**
     * Get the submenu at the selected index (starting at 0).
     *
     * @example
     * // a popup menu
     * var menu = plugins.window.createPopupMenu();
     *
     * // add two radiobuttons
     * menu.addRadioButton("radio one", feedback_radiobutton);
     * menu.addRadioButton("radio two", feedback_radiobutton);
     * // add a menu item, with a separator before it
     * menu.addSeparator();
     * menu.addMenuItem("item", feedback_item);
     * // add a checkbox, with a separator before it
     * menu.addSeparator();
     * menu.addCheckBox("check", feedback_checkbox);
     * // add a submenu with an item under it
     * var submenu = menu.addMenu("submenu");
     * submenu.addMenuItem("subitem", feedback_item);
     *
     * // depending on some state, update the entries in the menu
     * var some_state = true;
     * if (some_state) {
     *  // select the first radiobutton
     *  menu.getRadioButton(0).selected = true;
     * } else {
     *  // select the first radiobutton
     *  menu.getRadioButton(1).selected = true;
     * }
     * // enable/disable the menu item
     * // remember to include the separators also when counting the index
     * menu.getItem(3).enabled = !some_state;
     * // select/unselect the checkbox
     * // remember to include the separators also when counting the index
     * menu.getCheckBox(5).selected = some_state;
     * // change the text of the submenu and its item
     * application.output(menu.getItemCount());
     * if (some_state) {
     *  menu.getMenu(6).text = "some state";
     *  menu.getMenu(6).getItem(0).text = "some text";
     * }
     * else {
     *  menu.getMenu(6).text = "not some state";
     *  menu.getMenu(6).getItem(0).text = "other text";
     * }
     *
     * @param {Number} index
     * 
     * @retun Menu
     */
    this.getMenu= function(index) {}


    /**
     * Remove all items from the menu.
     *
     * @example
     * menu.removeAllItems();
     */
    this.removeAllItems = function() {}

    /**
     * Remove the item(s) at the selected index/indices.
     *
     * @example
     * menu.removeItem([1,4,6]);
     *
     * @param {Array} index array of one or moe indexes corresponding to items to remove
     */
    this.removeItem = function(index) {}
    
    /**
     * Sets the value for the specified element client property key.
     *
     * @example
     * // create a popup menu
     * var menu = plugins.window.createPopupMenu();
     *
     * // add an item to the menu
     * menu.addMenuItem("item", feedback_item);
     *
     * // set the tooltip of the menu via client properties
     * // keep the original tooltip in a form or global variable
     * originalTooltip = menu.getClientProperty("ToolTipText");
     * menu.putClientProperty("ToolTipText", "changed tooltip");
     *
     * // later restore the original tooltip from the variable
     * menu.putClientProperty("ToolTipText", originalTooltip);
     *
     * @param {Object} key
     * @param {Object} value
     */
    this.putClientProperty = function( key, value) {}

    /**
     * Gets the specified client property for the element based on a key.
     *
     * @example
     * // create a popup menu
     * var menu = plugins.window.createPopupMenu();
     *
     * // add an item to the menu
     * menu.addMenuItem("item", feedback_item);
     *
     * // set the tooltip of the menu via client properties
     * // keep the original tooltip in a form or global variable
     * originalTooltip = menu.getClientProperty("ToolTipText");
     * menu.putClientProperty("ToolTipText", "changed tooltip");
     *
     * // later restore the original tooltip from the variable
     * menu.putClientProperty("ToolTipText", originalTooltip);
     *
     * @param {Objec} key
     * 
     * @return Object
     */
    this.getClientProperty = function(key) {}
}

/**
 * This is the the object that creates a popupmenu
 * extends BaseMenu
 */
function Popup() {
 
    /**
     * Show the popup menu at the specified location.
     * If positionTop is true, and there is enough room available, then popup menu's bottom - left corner is ending at the specified coordinates;
     * x, y values are relative to top-left corner of the component.
     * By default, positionTop is false.
     * If there is not enough space above or under the component, the behavior is undefined (the browser will decide how menu is displayed)
     *
     * @example
     * // create a popup menu
     * var menu = plugins.window.createPopupMenu();
     * // add a menu item
     * menu.addMenuItem("an entry", feedback);
     *
     * if (event.getSource()) {
     *  // display the popup over the component which is the source of the event, 
     *  // at specified coordinates relative to the component
     *  menu.show(event.getSource(), 10, 10, true);
     * }
     *
     * @param {Runtimecomponent} component The component where this popup is linked in
     * @param {Number} x The x of the popup 
     * @param {Number} y The y coordinate of the popup
     * @param {Boolean} [positionTop] The positionTop where to show the popup (default false)
     */
    this.show = function(component, x, y, positionTop) { }
	
	/**
	 * Show the popup menu relative to the specified component.
     * If there is not enough space above or under the component, the behavior is undefined (the browser will decide how menu is displayed)
     * @example
	 * // create a popup menu
	 * var menu = plugins.window.createPopupMenu();
	 * // add a menu item
	 * menu.addMenuItem("an entry", feedback);
	 *
	 * if (event.getSource()) {
     *  // display the popup over the component which is the source of the event
     *  menu.show(event.getSource(), true);
	 * }
	 * 
	 * @param {Runtimecomponent} component The component where this popup is linked in
	 * @param {Boolean} [positionTop] The positionTop where to show the popup (default false)
     */
	this.show_component_positionTop = function(component, positionTop) { }
	
	/**
	 * Show the popup menu at the specified location
	 *
	 * @example
	 * // create a popup menu
	 * var menu = plugins.window.createPopupMenu();
	 * // add a menu item
	 * menu.addMenuItem("an entry", feedback);
	 *
	 * if (event.getSource()) {
	 *  // display the popup at specified coordinates
	 *  menu.show(10, 10);
	 * }
	 *
	 * @param {Number} x The x of the popup 
	 * @param {Number} y The y coordinate of the popup
	 */
	this.show_x_y = function(x, y) { }
	
	/**
	 * Show the popup menu relative to the event's source
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
	 * }
	 *
	 * @param {JSEvent} event event related to Popup 
	 */
	this.show_event = function(event) { }
}

var svy_types = {

  /**
 * Represents a Menu component.
 * Extends BaseMenu to provide a popup menu with display text and basic actions.
 */
  Menu: {
    /**
     * The text label displayed for the menu.
     */
    text: null,
  },
    
  /**
   * MenuItem type defines public properties and server-side API for menu items.
   */
  MenuItem: {
      /**
      * The unique name for the menu item.
      */
      name: null,
      /**
      * An array of arguments to pass to the callback when the menu item is activated.
      */
      methodArguments: null,
      /**
       * The display text shown for the menu item.
       */
      text: null,
      /**
       * Indicates whether the menu item is selected.
       */
      selected: null,
      /**
       * Indicates whether the menu item is enabled.
       */
      enabled: null
  },

  /**
   * Popup type defines public properties and server-side API for popup menus.
   */
  Popup: {
    /**
     * CSS class for custom styling of the popup.
     */
    cssClass: null
  }
}
