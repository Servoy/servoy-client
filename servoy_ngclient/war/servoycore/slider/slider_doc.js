/*
 * Servoy Core Slider Component.
 * A component for selecting a value or range of values using a draggable handle.
 */

/**
 * Determines if slider movements are animated.
 */
var animate;

/**
 * Specifies the data provider that holds the current slider value.
 */
var dataProviderID;

/**
 * Indicates whether the slider is enabled for user interaction.
 */
var enabled;

/**
 * Defines the upper limit of the slider.
 */
var max;

/**
 * Defines the lower limit of the slider.
 */
var min;

/**
 * Sets the slider orientation as either horizontal or vertical.
 */
var orientation;

/**
 * Configures the slider to highlight either the minimum or maximum side.
 */
var range;

/**
 * Determines the step interval between selectable slider values.
 */
var step;

/**
 * Controls the slider's visibility.
 */
var visible;

var handlers = {
    /**
     * Called when the slider handle drag operation starts.
     * 
     * @param {JSEvent} event The event object for the start event.
     * @param {Number} value - The current slider value at the start of the drag.
     */
    onStartMethodID: function() {},

    /**
     * Called repeatedly as the slider handle is dragged.
     * 
     * @param {JSEvent} event The event object for the slide event.
     * @param {Number} value The current slider value during the drag.
     */
    onSlideMethodID: function() {},

    /**
     * Called when the slider handle drag operation stops.
     * 
     * @param {JSEvent} event The event object for the stop event.
     * @param {Number} value - The final slider value after dragging.
     */
    onStopMethodID: function() {},

    /**
     * Called when the slider is initialized.
     * 
     * @param {JSEvent} event  The event object for the create event.
     */
    onCreateMethodID: function() {},

    /**
     * Called when the slider value changes.
     * 
     * @param {JSEvent} event  The event object for the change event.
     */
    onChangeMethodID: function() {}
};


