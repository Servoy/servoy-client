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
		 * Note: We deprecated this api because browsers removed support for custom messages of beforeunload event. Now most browsers display a standard message.
		 *
		 * @param {string} message the message to show when the user navigates away, null if nothing should be shown anymore.
		 * @deprecated
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
		},

		/**
		 * Call this when a solution can handle mobile device layouts (responsive design, can handle nicely
		 * width < height). This call is equivalent to calling setViewportMetaForMobileAwareSites(plugins.htmlHeaders.VIEWPORT_MOBILE_DEFAULT).<br/><br/>
		 * 
		 * It should be what most solutions that are able layout correctly on smaller mobile screens need; it will still allow the user to zoom-in
		 * and zoom-out. 
		 */
		setViewportMetaDefaultForMobileAwareSites: function()
		{
			// implemented in ngclientutils_server.js
		},

		/**
		 * Call this when a solution can handle mobile device layouts (responsive design, can handle nicely
		 * width < height). It will tell the device via the "viewport" meta header that it doesn't need to
		 * automatically zoom-out and use a big viewport to allow the page to display correctly as it would
		 * on a desktop.<br/><br/>
		 *
		 * 'viewportDefType' can be one of:<br/>
		 * <ul>
		 * <li>plugins.ngclientutils.VIEWPORT_MOBILE_DEFAULT - will show content correctly, allow zoom-in and
		 *                      zoom-out; the generated meta tag will be
		 *                      <meta name="viewport" content="width=device-width, initial-scale=1.0" /></li>
		 * <li>plugins.ngclientutils.VIEWPORT_MOBILE_DENY_ZOOM - will show content correctly, denies zoom-in
		 *                      and zoom-out; the generated meta tag will be
		 *                      <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0" /></li>
		 * <li>plugins.ngclientutils.VIEWPORT_MOBILE_DENY_ZOOM_OUT - will show content correctly, allows zoom-in
		 *                      but denies zoom-out; the generated meta tag will be
		 *                      <meta name="viewport" content="width=device-width, initial-scale=1.0, minimum-scale=1.0" /></li>
		 * <li>plugins.ngclientutils.VIEWPORT_MOBILE_DENY_ZOOM_IN - will show content correctly, denies zoom-in
		 *                      but allows zoom-out; the generated meta tag will be
		 *                      <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0" /></li>
		 * </ul><br/>
		 * This method actually uses replaceHeaderTag. For example plugins.ngclientutils.VIEWPORT_MOBILE_DEFAULT would call
		 * <pre>  replaceHeaderTag("meta", "name", "viewport", {
		 *        tagName: "meta",
		 *        attrs: [ { name: "name", value: "viewport" },
		 *                 { name: "content", value: "width=device-width, initial-scale=1.0" } ]
		 *  });</pre>
		 *
		 * @param {int} viewportDefType one of the constants listed above.
		 */
		setViewportMetaForMobileAwareSites: function(viewportDefType)
		{
			// implemented in ngclientutils_server.js
		},

		/**
		 * Utility method for manipulating 'contributedTags' array. It searches for an existing 'tag'
		 * that has the given 'tagName' and attribute ('attrNameToFind' & 'attrValueToFind'). If found
		 * it will replace it with 'newTag'. If not found it will just append 'newTag' to 'contributedTags'.<br/><br/>
		 * 
		 * NOTE: this call will only replace/remove tags that were added via this plugin/service, not others that were previously present in the DOM.
		 * 
		 * @param {string} tagName the tag name to find for replacement.
		 * @param {string} attrNameToFind the attribute to find on that tag name for replacement. If null it will just find the first by 'tagName' and use that one.
		 * @param {string} attrValueToFind the value the given attribute must have to match for replacement.
		 * @param {tag} newTag the new tag that replaces the old one. If null/undefined it will just remove what it finds.
		 * @return {tag} the tag that was removed if any.
		 */
		replaceHeaderTag: function(tagName, attrNameToFind, attrValueToFind, newTag)
		{
			// implemented in ngclientutils_server.js
		}
	}
}])
.run(["$services","$window",function($services,$window)
		{
			var scope = $services.getServiceScope('ngclientutils');
			
			// bind-once watch to wait for the server value to come (become defined) in case of a browser page refresh
			scope.$watch("model.contributedTags", function(newVal, oldVal) {
				if (newVal) {
					var tags = scope.model.contributedTags;

					// remove old ones if any
					angular.element("head > [hhsManagedTag]").remove();

					// create new ones
					if (tags) {
						var headEl = angular.element("head");
						for (var i = 0; i < tags.length; i++) {
							var tag = tags[i];
							if (tag) {
								var el = $window.document.createElement(tag.tagName);
								if (tag.attrs) for (var j = 0; j < tag.attrs.length; j++) {
									el.setAttribute(tag.attrs[j].name, tag.attrs[j].value);
								}
								el.setAttribute("hhsManagedTag", "");
								headEl.append(el);
							}
						}
					}
				}
			},true);
		}]);
