
///////////////////////////////////////////////////////////////////////////
//
// This plugin sets a tabindex on the TinyMCE iframe. Since the iframe
// is the editable region of TinyMCE, this enables tab to be used to
// move in and out of TinyMCE using normal tabindex rules.
//
// Settings
//
// tabindex: number | ['element']
//
//    tabindex for the iframe or -1 for no tabindex
//    'element' to copy the value from the element replaced by TinyMCE
//
// Written by Andrew Keller, Traction Software Inc.
// https://github.com/andykellr/tinymce-plugin-tabindex
//
///////////////////////////////////////////////////////////////////////////

/*global tinymce:true */

tinymce.PluginManager.add('tabindex', function(editor) {

	editor.on('PostRender', function() {
		var iframe, tabindex = editor.settings.tabindex || 'element';
		
		// -1 => don't set a tabindex
		if (tabindex === -1) {
                        return;
                }

		// 'element' => grab the tabindex from the element
		if (tabindex === 'element') {
			tabindex = editor.dom.getAttrib(editor.getElement(), 'tabindex', null);
		}
		
		// make sure we have a tabindex
		if (!tabindex) {
                        return;
                }
		
		// get the iframe so we can set the tabindex
		iframe = document.getElementById(editor.id + "_ifr");
		
		// make sure we have an iframe
		if (!iframe) {
                        return;
                }

		editor.dom.setAttrib(iframe, 'tabindex', tabindex);
	});

});
