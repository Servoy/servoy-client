/*
 * Form Component Component embeds a Form Component within a container-component.<br/>
 * It supports both anchored and responsive layouts.
 */

/**
 * The Form Component that is contained/to be shown by this Form Component Component. 
 */
var containedForm;

/**
 * Custom CSS class for styling the container.
 */
var styleClass;

/**
 * When <b>form component content is &lt;anchored&gt;</b>, it will set on the wrapper div:<ul><li><b>min-width</b> - if <b>parent/containing form is &lt;anchored&gt; as well;</b></li><li><b>min-width</b> and <b>float: left;</b> - if the <b>parent/containing form is &lt;responsive&gt;</b>; this way you can put multiple such form components with fixed width inside the same 12grid column container for example.</li></ul>IGNORED when <b>form component content is &lt;responsive&gt;</b>.
 */
var minWidth;

/**
 * When <b>form component content is &lt;anchored&gt;</b>, it will set <b>min-height</b> on the wrapper div.<br/>If <b>parent/containing form is &lt;anchored&gt; as well</b> it will also set <b>height: 100%</b> (needed for anchoring to bottom).<br/><br/>IGNORED when <b>form component content is &lt;responsive&gt;</b>.
 */
var minHeight;

/**
 * When <b>form component content is &lt;anchored&gt;</b>, it will set on the wrapper div:<ul><li><b>min-width</b> - if <b>parent/containing form is &lt;anchored&gt; as well;</b></li><li><b>min-width</b> and <b>float: left;</b> - if the <b>parent/containing form is &lt;responsive&gt;</b>; this way you can put multiple such form components with fixed width inside the same 12grid column container for example.</li></ul>IGNORED when <b>form component content is &lt;responsive&gt;</b>.
 */
var width;

/**
 * When <b>form component content is &lt;anchored&gt;</b>, it will set <b>min-height</b> on the wrapper div.<br/>If <b>parent/containing form is &lt;anchored&gt; as well</b> it will also set <b>height: 100%</b> (needed for anchoring to bottom).<br/><br/>IGNORED when <b>form component content is &lt;responsive&gt;</b>.
 */
var height;

/**
 * Controls the container's visibility.
 */
var visible;


