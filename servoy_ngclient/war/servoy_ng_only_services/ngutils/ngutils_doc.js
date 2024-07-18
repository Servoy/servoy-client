
/**
 * This will register a callback that will be triggered on all history/window popstate events (back,forward but also next main form).
 * If this is registered then we will try to block the application from going out of the application.
 * The callback gets 1 argument and that is the hash of the url, that represents at this time the form where the back button would go to.
 * If this hash argument is an empty string then that means the backbutton was hit to the first loaded page and we force a forward again.
 * 
 * @param {function} callback
 */
function setBackActionCallback(callback) {
	// implemented server side.
}
/**
 * This will return the user agent string of the clients browser.
 */
function getUserAgent()
{
}

/**
 * Set the message that will be shown when the browser tab is closed or the users navigates away, 
 * this can be used to let users know they have data modifications that are not yet saved. 
 * Note: We deprecated this api because browsers removed support for custom messages of beforeunload event. Now most browsers display a standard message.
 *
 * @param {string} message the message to show when the user navigates away, null if nothing should be shown anymore.
 * @deprecated
 */
function setOnUnloadConfirmationMessage(message)
{
}

/**
 * Set whether browser default warning message will be shown when the browser tab is closed or the users navigates away, 
 * this can be used to let users know they have data modifications that are not yet saved. 
 *
 * @param {boolean} showConfirmation boolean for whether to show confirmation message
 */
function setOnUnloadConfirmation(showConfirmation)
{
}

/**
 * Call this when a solution can handle mobile device layouts (responsive design, can handle nicely
 * width < height). This call is equivalent to calling setViewportMetaForMobileAwareSites(plugins.htmlHeaders.VIEWPORT_MOBILE_DEFAULT).<br/><br/>
 * 
 * It should be what most solutions that are able layout correctly on smaller mobile screens need; it will still allow the user to zoom-in
 * and zoom-out. 
 */
function setViewportMetaDefaultForMobileAwareSites()
{
}

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
 *        attrs: [ { name: "name", value: "viewport" }
 *                 { name: "content", value: "width=device-width, initial-scale=1.0" } ]
 *  });</pre>
 *
 * @param {int} viewportDefType one of the constants listed above.
 */
function setViewportMetaForMobileAwareSites(viewportDefType)
{
}

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
function replaceHeaderTag(tagName, attrNameToFind, attrValueToFind, newTag)
{
}

/**
 * Utility method for manipulating form style classes. 
 * It will add a style class to a certain form, similar as a design style class would work.
 * 
 * @param {string} formname the form name to add to.
 * @param {string} styleclass the styleclass to be added to form tag.
 */
function addFormStyleClass(formname,styleclass)
{
}

/**
 * Utility method for manipulating form style classes. 
 * It will get styleclasses assigned to a certain form, multiple styleclasses are separated by space.
 * 
 * NOTE: this call will only get style classes that were added via this plugin/service, not others that were previously set at design time or via solution model.
 * 
 * @param {string} formname the form name to get style classes.
 * @return {string} the styleclass of that form.
 */
function getFormStyleClass(formname)
{
}

/**
 * Utility method for manipulating form style classes. 
 * It will remove a styleclasse assigned to a certain form.
 * 
 * NOTE: this call will only remove style classes that were added via this plugin/service, not others that were previously set at design time or via solution model.
 * 
 * @param {string} formname the form name to remove from.
 * @param {string} styleclass the styleclass to be removed from form tag.
 */
function removeFormStyleClass(formname,styleclass)
{
}

/**
 * Utility method for manipulating any DOM element's style classes. 
 * It will add the given class to the DOM element identified via the css selector param.
 * 
 * NOTE: This operation is not persistent; it executes client-side only; so for example when the browser is reloaded (F5/Ctrl+F5) by the user classes added by this method are lost.
 * If you need this to be persistent - you can do that directly via server side scripting elements.myelement.addStyleClass(...) if the DOM element is a Servoy component. If the DOM element is
 * not a component then you probably lack something in terms of UI and you could build what you need as a new custom component or use another approach/set of components when building the UI.
 * 
 * @param {string} cssSelector the css selector string that is used to find the DOM element.
 * @param {string} className the class to be added to the element.
 */
function addClassToDOMElement(cssSelector, className) {
}

/**
 * Utility method for manipulating any DOM element's style classes. 
 * It will remove the given class from the DOM element identified via the css selector param.
 * 
 * NOTE: This operation is not persistent; it executes client-side only; so for example when the browser is reloaded (F5/Ctrl+F5) by the user classes removed by this method are lost;
 * If you need this to be persistent - you can do that directly via server side scripting elements.myelement.removeStyleClass(...) if the DOM element is a Servoy component. If the DOM element it is
 * not a component then you probably lack something in terms of UI and you could build what you need as a new custom component or use another approach/set of components when building the UI.
 * 
 * @param {string} cssSelector the css selector string that is used to find the DOM element.
 * @param {string} className the class to be added to the element.
 */
function removeClassFromDOMElement(cssSelector, className) {
}

/**
 * Print a document from specific URL. This will open browser specific print dialog. 
 * 
 * NOTE: url should be from same domain, otherwise a temp file on server should be created and served
 * 
 * @sample
 *  // if url is not from same domain we must create a temporary file
 * 	var file = plugins.http.getMediaData(url);  
 *	var remoteFileName = application.getUUID().toString() + '.pdf'; 
 *	var remoteFile = plugins.file.convertToRemoteJSFile('/'+remoteFileName) 
 *	remoteFile.setBytes(file,true);  //Convert the remote file to a url, and print it
 *	var remoteUrl = plugins.file.getUrlForRemoteFile('/'+remoteFileName);  
 *	plugins.ngclientutils.printDocument(remoteUrl)
 * @param {string} url The URL of document to be printed.
 */
function printDocument(url)
{
}

/**
 * Retrieves the screen location of a specific element. Returns the location as point (object with x and y properties).
 * 
 * @param {string} component the component to retrieve location for.
 * @return {point} the location of the component.
 */
function getAbsoluteLocation(component)
{
}

/**
 * Set lang attribute on html tag.
 * 
 * @param {boolean} showConfirmation boolean for whether to show confirmation message
 */
function setLangAttribute(lang)
{
}

/**
 * Move the scrollbar to the position of the given anchorSelector.
 * The target anchorSelector can be a Servoy Form, Layout Container or element in a responsive form or any element in a form.
 * You can use styleClass as selector.
 * For example: you can add 'scroll-element' to an element of the form.
 * Examples of usage: 
 * - plugins.ngclientutils.scrollIntoView(".toScroll-To");
 * - plugins.ngclientutils.scrollIntoView(".toScroll-To", { behavior: "smooth", block: "start", inline: "nearest" });
 
 * @param anchorSelector {string} the selector to which the scrollbar should be moved to.
 * @param scrollIntoViewOptions option argument used for scrolling animation (example:  { behavior: "smooth", block: "start", inline: "nearest" }).
 */
function scrollIntoView( anchorSelector, scrollIntoViewOptions ) {
}

/**
 * Move the scrollbar to top position of the given selector.
 * The target selector can be a Servoy Form, Layout Container or element in a responsive form or any element in a form.
 * You can use styleClass as selector.
 * For example: you can add 'scroll-element' to an element of the form.
 * Examples of usage: 
 * - plugins.ngclientutils.scrollToTop(".toScroll-To");
 
 * @param selector {string} the selector to which the scrollbar should be moved to top.
 */
function scrollToTop(selector) {
}

/**
 * This method removes the arguments from the client url. This is used for bookmark url to be correct or for back button behavior.
 */
function removeArguments()
{
}
