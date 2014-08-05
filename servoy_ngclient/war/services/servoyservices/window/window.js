angular.module('window',['servoy'])
.factory("window",function($window,$services) {
	var state = $services.getServiceState('window');
	return {
		createShortcut: function(shortcutcombination,callback,contextFilter,args) {
			if (contextFilter instanceof Array)
			{
				args = contextFilter;
				contextFilter = null;
			}
			shortcut.add(this.translateSwingShortcut(shortcutcombination),this.getCallbackFunction(callback, contextFilter,args),{'propagate':true,'disable_in_input':false});
			if (!state.shortcuts) state.shortcuts = [];
			state.shortcuts.push({'shortcut': shortcutcombination,'callback':callback,'contextFilter':contextFilter,'arguments':args});
			return true;
		},
		removeShortcut: function(shortcutcombination) {
			shortcut.remove(this.translateSwingShortcut(shortcutcombination));
			if (state.shortcuts)
			{
				for (var i=0;i<state.shortcuts.length;i++)
				{
					if (state.shortcuts[i].shortcut == shortcutcombination)
					{
						state.shortcuts.splice(i,1);
						break;
					}
				}
			}
			return true;
		},
		translateSwingShortcut: function(shortcutcombination)
		{
			var shortcutParts = shortcutcombination.split(" ");
			var translatedShortcut = '';
			for (var i=0;i<shortcutParts.length;i++)
			{
				if (i>0)
				{
					translatedShortcut +='+';
				}
				if (shortcutParts[i] == 'control' || shortcutParts[i] == 'ctrl')
				{
					translatedShortcut +='CTRL';
				}
				else if (shortcutParts[i] == 'meta')
				{
					translatedShortcut +='META';
				}
				else if (shortcutParts[i] == 'shift')
				{
					translatedShortcut +='SHIFT';
				}
				else if (shortcutParts[i] == 'alt')
				{
					translatedShortcut +='ALT';
				}
				else
				{
					translatedShortcut +=shortcutParts[i];
				}
			}
			return translatedShortcut;
		},
		getCallbackFunction: function(callback,contextFilter,args)
		{
			return function(e){
				if (contextFilter)
				{
					var element;
					if(e.target) element=e.target;
					else if(e.srcElement) element=e.srcElement;
					var parent = element;
					while (parent)
					{
						var form = parent.getAttribute("ng-controller");
						if (form)
						{
							if (form != contextFilter && form != 'MainController') return;
							break;
						}
						parent = parent.parentNode;
					}
				}
				$window.executeInlineScript(callback.formname,callback.script,args);
			};
		}
	}
})
.run(function($rootScope,$services,window)
{
	var scope = $rootScope.$new(true);
	scope.state = $services.getServiceState('window');
	scope.$watch('state', function(newvalue,oldvalue) {
		if (newvalue && newvalue.shortcuts && newvalue.shortcuts.length > 0 && Object.keys(shortcut.all_shortcuts).length == 0)
		{
			// handle just refresh page case, need to reinstall all shortcuts again
			for (var i=0;i<newvalue.shortcuts.length;i++)
			{
				shortcut.add(window.translateSwingShortcut(newvalue.shortcuts[i].shortcut),window.getCallbackFunction(newvalue.shortcuts[i].callback,newvalue.shortcuts[i].contextFilter,newvalue.shortcuts[i].arguments),{'propagate':true,'disable_in_input':false});
			}
		}	
	}, true);
})