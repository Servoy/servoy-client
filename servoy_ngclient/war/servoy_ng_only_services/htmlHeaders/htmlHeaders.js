angular.module('htmlHeaders', [ 'servoy' ])
.factory("htmlHeaders",function($services) {
	var scope = $services.getServiceScope('htmlHeaders');
	return {

		/**
		 * Call this when a solution can handle mobile device layouts (responsive design, can handle nicely
		 * width < height). This call is equivalent to calling setViewportMetaForMobileAwareSites(plugins.htmlHeaders.VIEWPORT_MOBILE_DEFAULT).<br/><br/>
		 * 
		 * It should be what most solutions that are able layout correctly on smaller mobile screens need; it will still allow the user to zoom-in
		 * and zoom-out. 
		 */
		setViewportMetaDefaultForMobileAwareSites: function()
		{
			// implemented in htmlHeaders_server.js
		},

		/**
		 * Call this when a solution can handle mobile device layouts (responsive design, can handle nicely
		 * width < height). It will tell the device via the "viewport" meta header that it doesn't need to
		 * automatically zoom-out and use a big viewport to allow the page to display correctly as it would
		 * on a desktop.<br/><br/>
		 *
		 * 'viewportDefType' can be one of:<br/>
		 * <ul>
		 * <li>plugins.htmlHeaders.VIEWPORT_MOBILE_DEFAULT - will show content correctly, allow zoom-in and
		 *                      zoom-out; the generated meta tag will be
		 *                      <meta name="viewport" content="width=device-width, initial-scale=1.0" /></li>
		 * <li>plugins.htmlHeaders.VIEWPORT_MOBILE_DENY_ZOOM - will show content correctly, denies zoom-in
		 *                      and zoom-out; the generated meta tag will be
		 *                      <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0" /></li>
		 * <li>plugins.htmlHeaders.VIEWPORT_MOBILE_DENY_ZOOM_OUT - will show content correctly, allows zoom-in
		 *                      but denies zoom-out; the generated meta tag will be
		 *                      <meta name="viewport" content="width=device-width, initial-scale=1.0, minimum-scale=1.0" /></li>
		 * <li>plugins.htmlHeaders.VIEWPORT_MOBILE_DENY_ZOOM_IN - will show content correctly, denies zoom-in
		 *                      but allows zoom-out; the generated meta tag will be
		 *                      <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0" /></li>
		 * </ul><br/>
		 * This method actually uses replaceHeaderTag. For example plugins.htmlHeaders.VIEWPORT_MOBILE_DEFAULT would call
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
			// implemented in htmlHeaders_server.js
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
			// implemented in htmlHeaders_server.js
		},

		/**
		 * All the methods that change 'contributedTags' as well as manipulating manually the 'contributedTags' property
		 * will not really change the head section of the HTML page right away. In order to do that call this method.
		 * It will look at 'contributedTags' and apply it to the DOM.<br/><br/>
		 * 
		 * This way you can do multiple header changes before really changing the DOM - and apply those all at once.
		 * It also helps avoid some angular property watches browser-side. (for performance reasons)
		 */
		applyHeaderTags: function()
		{
			var tags = scope.model.contributedTags;

			// remove old ones if any
			$("head > [hhsManagedTag]").remove();

			// create new ones
			if (tags) {
				var headEl = $("head");
				for (var i = 0; i < tags.length; i++) {
					var tag = tags[i];
					if (tag) {
						var el = document.createElement(tag.tagName);
						if (tag.attrs) for (var j = 0; j < tag.attrs.length; j++) {
							el.setAttribute(tag.attrs[j].name, tag.attrs[j].value);
						}
						el.setAttribute("hhsManagedTag", "");
						headEl.append(el);
					}
				}
			}
		}

	}
})
.run(function($services, htmlHeaders)
{
	var scope = $services.getServiceScope('htmlHeaders');
	
	// bind-once watch to wait for the server value to come (become defined) in case of a browser page refresh
	var removeWatch = scope.$watch("model.contributedTags", function(newVal, oldVal) {
		if (newVal) {
			htmlHeaders.applyHeaderTags();
			removeWatch();
		}
	});
});
