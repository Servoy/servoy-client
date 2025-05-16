/**
 * Servoy Core Form Container Component embeds a form within a container that supports both anchored and responsive layouts.
 */
/**
 * Defines the animation to be applied when the form container is shown or hidden.
 */
var animation;

/**
 * Specifies the form to be embedded within the container.
 */
var containedForm;

/**
 * Specifies the relation name used to link the container to a data source.
 */
var relationName;

/**
 * When <code>true</code>, the form is rendered when all its latest data is loaded from the server. When <code>false</code>, the form is rendered faster, but could show stale data (not a problem when the form shown does not show dynamic data)
 */
var waitForData;

/**
 * Specifies a CSS style class for custom styling of the form container.
 */
var styleClass;

/**
 * Minimum height of the form container, should be used for responsive forms. Can be 100% (to take parent container height) or a number (in pixels).
 */
var height;

/**
 * Tab sequence number of form containers is used for all nested components in the main form.
 */
var tabSeq;

/**
 * Controls the visibility of the form container.
 */
var visible;


