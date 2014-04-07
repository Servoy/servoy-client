// Currently only needed for click event 
// if we need more events we should add CasperJS as a dependency  and remove this
if (navigator.userAgent.toLowerCase().substr('phantom')) {
  // Patch since PhantomJS does not implement click() on HTMLElement. In some 
  // cases we need to execute the native click on an element. However, jQuery's 
  // $.fn.click() does not dispatch to the native function on <a> elements, so we
  // can't use it in our implementations: $el[0].click() to correctly dispatch.
  if (!HTMLElement.prototype.click) {
    HTMLElement.prototype.click = function() {
      var ev = document.createEvent('MouseEvent');
      ev.initMouseEvent(
          'click',
          /*bubble*/true, /*cancelable*/true,
          window, null,
          0, 0, 0, 0, /*coordinates*/
          false, false, false, false, /*modifier keys*/
          0/*button=left*/, null
      );
      this.dispatchEvent(ev);
    };
  }
}
/* ALL browsers custom event functions . 
 * TODO replace jquery/jqlite mouseover() etc with these functions instead of adding them to the html element prototype*/
(function(){
	var events =['mouseover','mouseout']
	
	events.forEach(function(eventName){
		 HTMLElement.prototype['trigger'+ eventName]= function() {
		    if( document.createEvent ) {
		      var ev = document.createEvent('MouseEvent');
		      ev.initMouseEvent(
		          eventName,
		          /*bubble*/true, /*cancelable*/true,
		          window, null,
		          0, 0, 0, 0, /*coordinates*/
		          false, false, false, false, /*modifier keys*/
		          0/*button=left*/, null
		      );
		      this.dispatchEvent(ev);
		    }else if( document.createEventObject ) {
	            elem.fireEvent('on'+eventName);
	        }
		 };		
	})
})()