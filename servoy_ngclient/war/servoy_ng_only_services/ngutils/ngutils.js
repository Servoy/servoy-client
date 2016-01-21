angular.module('ngclientutils', [ 'servoy' ])
.factory("ngclientutils",["$services","$window",function($services, $window) {
	var scope = $services.getServiceScope('ngclientutils');
	var confirmMessage = null
	var beforeUnload =  function(e) {
		(e || window.event).returnValue = confirmMessage; //Gecko + IE
		return confirmMessage; //Gecko + Webkit, Safari, Chrome etc.
	};
	
	return {

		/**
		 * This will return the user agent string of the clients browser.
		 */
		getUserAgent: function()
		{
			return $window.navigator.userAgent;
		},

		/**
		 * Set the message that will be shown when the browser tab is closed or the users navigates away, 
		 * this can be used to let users know they have data modifications that are not yet saved.
		 *
		 * @param {string} message the message to show when the user navigates away, null if nothing should be shown anymore.
		 */
		setOnUnloadConfirmationMessage: function(message)
		{
			confirmMessage = message;
			if (confirmMessage) {
				// duplicate add's of the same type and function are ignored 
				// if an existing message would be updated with a new one
				$window.window.addEventListener("beforeunload", beforeUnload);
			}
			else {
				$window.window.removeEventListener("beforeunload", beforeUnload);
			}
		}
	}
}]);
