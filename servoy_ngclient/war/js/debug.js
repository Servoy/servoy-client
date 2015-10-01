angular.module('servoyApp')
.run(function ($applicationService,$sabloApplication)
{
	function ctrlLCallback(event)
	{
		var element = angular.element(event.srcElement);
		var parent = element.closest('svy-formload');
		if (parent[0] == undefined) parent = $(document.activeElement).closest('svy-formload');
		if (parent[0] == undefined && element[0] == document.body) parent = element.find('svy-formload'); //main form
		if (parent[0] !== undefined)
		{
			var formname = parent[0].getAttribute('formname');
			$sabloApplication.callService("developerService", "openFormInDesigner", {'formname' : formname},true);
		}
		else
		{
			$log.error("svy * Unexpected: cannot find the form to open in designer, focused element: "+ event.srcElement.getAttribute('name'));
		}
	}
	window.shortcut.add("CTRL+L", ctrlLCallback);
});