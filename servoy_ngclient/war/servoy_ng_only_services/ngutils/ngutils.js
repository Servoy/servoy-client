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
		 * Set whether browser default warning message will be shown when the browser tab is closed or the users navigates away, 
		 * this can be used to let users know they have data modifications that are not yet saved. 
		 *
		 * @param {boolean} showConfirmation boolean for whether to show confirmation message
		 */
		setOnUnloadConfirmation: function(showConfirmation)
		{
			// the message is ignored lately by browsers
			confirmMessage = 'You have unsaved data. Are you sure you want to quit?';
			if (showConfirmation) {
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
		},
		
		/**
		 * Utility method for manipulating form style classes. 
		 * It will add a style class to a certain form, similar as a design style class would work.
		 * 
		 * @param {string} formname the form name to add to.
		 * @param {string} styleclass the styleclass to be added to form tag.
		 */
		addFormStyleClass: function(formname,styleclass)
		{
			// implemented in ngutils_server.js
		},
		
		/**
		 * Utility method for manipulating form style classes. 
		 * It will get styleclasses assigned to a certain form, multiple styleclasses are separated by space.
		 * 
		 * NOTE: this call will only get style classes that were added via this plugin/service, not others that were previously set at design time or via solution model.
		 * 
		 * @param {string} formname the form name to get style classes.
		 * @return {string} the styleclass of that form.
		 */
		getFormStyleClass: function(formname)
		{
			// implemented in ngutils_server.js
		},
		
		/**
		 * Utility method for manipulating form style classes. 
		 * It will remove a styleclasse assigned to a certain form.
		 * 
		 * NOTE: this call will only remove style classes that were added via this plugin/service, not others that were previously set at design time or via solution model.
		 * 
		 * @param {string} formname the form name to remove from.
		 * @param {string} styleclass the styleclass to be removed from form tag.
		 */
		removeFormStyleClass: function(formname,styleclass)
		{
			// implemented in ngutils_server.js
		},
		
		/**
		 * Utility method for manipulating any DOM element's style classes. 
		 * It will add the given class to the DOM element identified via the jQuery selector param.
		 * 
		 * NOTE: This operation is not persistent; it executes client-side only; so for example when the browser is reloaded (F5/Ctrl+F5) by the user classes added by this method are lost.
		 * If you need this to be persistent - you can do that directly via server side scripting elements.myelement.addStyleClass(...) if the DOM element is a Servoy component. If the DOM element is
		 * not a component then you probably lack something in terms of UI and you could build what you need as a new custom component or use another approach/set of components when building the UI.
		 * 
		 * @param {string} jQuerySelector the jQuery selector string that is used to find the DOM element.
		 * @param {string} className the class to be added to the element.
		 */
		addClassToDOMElement: function(jQuerySelector, className) {
			$(jQuerySelector).addClass(className);
		},

		/**
		 * Utility method for manipulating any DOM element's style classes. 
		 * It will remove the given class from the DOM element identified via the jQuery selector param.
		 * 
		 * NOTE: This operation is not persistent; it executes client-side only; so for example when the browser is reloaded (F5/Ctrl+F5) by the user classes removed by this method are lost;
		 * If you need this to be persistent - you can do that directly via server side scripting elements.myelement.removeStyleClass(...) if the DOM element is a Servoy component. If the DOM element it is
		 * not a component then you probably lack something in terms of UI and you could build what you need as a new custom component or use another approach/set of components when building the UI.
		 * 
		 * @param {string} jQuerySelector the jQuery selector string that is used to find the DOM element.
		 * @param {string} className the class to be added to the element.
		 */
		removeClassFromDOMElement: function(jQuerySelector, className) {
			$(jQuerySelector).removeClass(className);
		}
	}
}])
.directive('svyFormClassUpdate', function($services) {
	return {
		restrict: 'A', 
		controller: function($scope, $element, $attrs) {
			var scope = $services.getServiceScope('ngclientutils');
			if (scope.model.styleclasses)
			{
				var formname = $element.attr('ng-controller');
				if (formname && scope.model.styleclasses[formname])
				{
					var arr = scope.model.styleclasses[formname].split(" ");
					for (var j=0;j<arr.length;j++)
					{
						if (!$element.hasClass(arr[j]))
						{
							$element.addClass(arr[j]);
						}
					}
				}
			}	
		}
	};   
})
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
			
			scope.$watch("model.styleclasses", function(newVal, oldVal) {
				if (newVal) {
					var keys = Object.keys(newVal);
					for (var i = 0;i< keys.length;i++)
					{ 
						var el = $("div[ng-controller='"+keys[i]+"']");
						if (el.length >0)
						{
							var arr = newVal[keys[i]].split(" ");
							for (var j=0;j<arr.length;j++)
							{
								if (!el.hasClass(arr[j]))
								{
									el.addClass(arr[j]);
								}
							}
						}	
					}
					if (oldVal)
					{
						var keys = Object.keys(oldVal)
						for (var i = 0;i< keys.length;i++)
						{ 
							var formname = keys[i];
							var arr = oldVal[formname].split(" ");
							var newStyle = newVal[formname];
							var el = null;
							for (var j=0;j<arr.length;j++)
							{
								if (!newStyle || newStyle.split(" ").indexOf(arr[j]) < 0)
								{
									if (!el)
									{
										el =  $("div[ng-controller='"+formname+"']");
									}	
									el.removeClass(arr[j]);
								}	
							}
						}
					}
				}
			},true);
		}]);
