var index = 1;

var MenuItem = {
	doClick: function()
	{
		this.callback();
	},
	
	getText: function()
	{
		return this.text;
	},
	
	setMethod: function(callback,args)
	{
		this.callback = callback;
		this.args = args;
	},
	
	setAccelarator: function(accelerator)
	{
		this.accelerator = accelerator;
	},
	
	setEnabled: function(enabled)
	{
		this.enabled = enabled;
	},
	
	isEnabled: function()
	{
		if (this.enabled == undefined)
			return true;
		return this.enabled;
	},
	
	setIcon: function(icon)
	{
		this.icon = icon;
	},
	
	setMnemonic: function(mnemonic)
	{
		this.mnemonic = mnemonic;
	},
	
	setText: function(text)
	{
		this.text = text;
	},
	
	setVisible: function(visible)
	{
		this.visible = visible;
	},
	
	setMethodArguments: function(args)
	{
		this.args = args;
	},
	
	getMethodArguments: function()
	{
		if (this.args == undefined)
			return null;
		return this.args;
	},
	
	setSelected: function(selected)
	{
		this.selected = selected;
	},
	
	getSelected: function()
	{
		if (this.selected == undefined)
			return false;
		return this.selected;
	},
	
	setName: function(name)
	{
		this.name = name;
	},
	
	getName: function()
	{
		if (this.name == undefined)
			return null;
		return this.name;
	},
	
	setBackgroundColor: function(color)
	{
		this.backgroundColor = color;
	},
	
	setForegroundColor: function(color)
	{
		this.foregroundColor = color;
	},
	
	putClientProperty: function(key,value)
	{
		this.key = value;
	},
	
	getClientProperty: function(key)
	{
		if (this.key == undefined) return null;
		return this.key;
	}
};

var Menu = {
	addMenuItem : function(text,callback,icon,mnemonic,enabled,align)
	{
		var newItem = Object.create(MenuItem);;
		newItem.text = text;
		newItem.callback = callback;
		newItem.icon = icon;
		newItem.mnemonic = mnemonic;
		newItem.align = align;
		newItem.enabled = enabled;
		this.items.push(newItem);
		return newItem;
	},
	
	addCheckBox : function(text,callback,icon,mnemonic,enabled,align)
	{
		var checkbox = this.addMenuItem(text,callback,icon,mnemonic,enabled,align);
		checkbox.cssClass = "img_checkbox";
		return checkbox;
	},
	
	addRadioButton : function(text,callback,icon,mnemonic,enabled,align)
	{
		var radio = this.addMenuItem(text,callback,icon,mnemonic,enabled,align);
		radio.cssClass = "img_radio_off";
		return radio;
	},
	
	addRadioGroup : function()
	{
		
	},
	
	addSeparator: function(index)
	{
		if (index)
		{
			this.items.splice(index,0,null);
		}
		else
		{
			this.items.push(null);
		}
	},
	
	addMenu: function(text)
	{
		var newMenu = Object.create(Menu);
		newMenu.text = text;
		newMenu.items = new Array();
		this.items.push(newMenu);
		return newMenu;
	},
	
	getCheckBox: function(index)
	{
		return this.items[index];
	},
	
	getRadioButton: function(index)
	{
		return this.items[index];
	},
	
	getMenu: function(index)
	{
		return this.items[index];
	},
	
	getItem: function(index)
	{
		return this.items[index];
	},
	
	getItemCount: function()
	{
		return this.items.length;
	},
	
	getItemIndexByText: function(text)
	{
		for (var i=0;i<items.length;i++)
		{
			if (items[i] && items[i].text == text)
				return i;
		}
	},
	
	removeAllItems: function()
	{
		return this.items.splice(0,this.items.length);
	},
	
	removeItem:  function(indexes)
	{
		this.items = this.items.filter(function(element){
			if (indexes && indexes.indexOf(this.items.indexOf(element)) >= 0) return false;
			return true;
		});
	},
	
	putClientProperty: function(key,value)
	{
		this.key = value;
	},
	
	getClientProperty: function(key)
	{
		if (this.key == undefined) return null;
		return this.key;
	},
	
	show : function(component,x,y)
	{
		// this belongs to popup only but cannot assign directly on instance because then it is sent to client
		var command = {'popupName': this.name};
		if (component == undefined)
		{
			command.x = 0;
			command.y = 0;
		}
		else if (x == undefined && y == undefined)
		{
			command.elementId = component.markupId;
			command.x = 0;
			command.y = component.height;
		}
		else if (x != undefined && y == undefined)
		{
			command.x = component;
			command.y = x;
		}
		else
		{
			command.elementId = component.markupId;
			command.x = x;
			command.y = y;
		}
		$scope.model.popupMenuShowCommand = command;
	}
}
$scope.api.createPopupMenu = function() {
	var popupName = 'popupmenu'+index;
	index++;
	var popup = Object.create(Menu);
	popup.name = popupName;
	popup.items = new Array();
	if (!$scope.model.popupMenus)
		$scope.model.popupMenus = [];
	// dirty hack to mark property changed
	var menus = $scope.model.popupMenus.slice(0);
	menus.push(popup);
	$scope.model.popupMenus = menus;
	return popup;
}