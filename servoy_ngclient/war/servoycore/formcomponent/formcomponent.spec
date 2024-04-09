{
	"name": "servoycore-formcomponent",
	"displayName": "FormComponentContainer",
	"categoryName": "Form Containers",
	"version": 1,
	"icon": "servoycore/formcomponent/formcomponent.png",
	"definition": "servoycore/formcomponent/formcomponent.js", 
	"libraries": [{"name":"servoycore-formcomponent-css", "version":"1.0", "url":"servoycore/formcomponent/formcomponent.css", "mimetype":"text/css"}],
	"keywords": [],
	"model":
	{
		"containedForm": "formcomponent",
		"styleClass" : { "type" :"styleclass","default": "svy-formcomponent"},
        "minWidth" : {"type" :"int", "tags": { "scope" :"design", "doc": "When <b>form component content is &lt;anchored&gt;</b>, it will set on the wrapper div:<ul><li><b>min-width</b> - if <b>parent/containing form is &lt;anchored&gt; as well;</b></li><li><b>min-width</b> and <b>float: left;</b> - if the <b>parent/containing form is &lt;responsive&gt;</b>; this way you can put multiple such form components with fixed width inside the same 12grid column container for example.</li></ul>IGNORED when <b>form component content is &lt;responsive&gt;</b>." }},
        "minHeight" :{"type" :"int", "tags": { "scope" :"design", "doc": "When <b>form component content is &lt;anchored&gt;</b>, it will set <b>min-height</b> on the wrapper div.<br/>If <b>parent/containing form is &lt;anchored&gt; as well</b> it will also set <b>height: 100%</b> (needed for anchoring to bottom).<br/><br/>IGNORED when <b>form component content is &lt;responsive&gt;</b>." }},
        "width" : {"type" :"int", "deprecated": "use minWidth instead", "tags": { "scope" :"design", "doc": "When <b>form component content is &lt;anchored&gt;</b>, it will set on the wrapper div:<ul><li><b>min-width</b> - if <b>parent/containing form is &lt;anchored&gt; as well;</b></li><li><b>min-width</b> and <b>float: left;</b> - if the <b>parent/containing form is &lt;responsive&gt;</b>; this way you can put multiple such form components with fixed width inside the same 12grid column container for example.</li></ul>IGNORED when <b>form component content is &lt;responsive&gt;</b>." }},
        "height" :{"type" :"int", "deprecated": "use minHeight instead", "tags": { "scope" :"design", "doc": "When <b>form component content is &lt;anchored&gt;</b>, it will set <b>min-height</b> on the wrapper div.<br/>If <b>parent/containing form is &lt;anchored&gt; as well</b> it will also set <b>height: 100%</b> (needed for anchoring to bottom).<br/><br/>IGNORED when <b>form component content is &lt;responsive&gt;</b>." }},
		"visible" : "visible"
	}
}