/*
 * A service for working quickly with simple dialogs in NG/Titanium client.<br/>
 * For more complex modal windows/popups you can use use modal windows from the "application" node or popups offered by the "window" plugin.
 */

/**
 * Shows an error dialog with the specified title, message and a customizable set of buttons.
 *
 * @example
 * //show dialog
 * var thePressedButton = plugins.dialogs.showErrorDialog('Title', 'Value not allowed','OK', 'Cancel');
 * //show dialog with focus on second button (index 1)
 * var thePressedButton = plugins.dialogs.showErrorDialog('Title', 'Value not allowed','OK', 'Cancel', 1);
 *
 * @param {String} dialogTitle Dialog title.
 * @param {String} dialogMessage Dialog message.
 * @param {String...} [buttonsText] variable arguments of button texts. The last argument can be a number specifying the index (0-based) of the button to focus and highlight.
 * 
 * @return {String} pressed button or null if closed by escape key
 */
function showErrorDialog(dialogTitle,dialogMessage,buttonsText) {}

/**
 *  Shows an info dialog with the specified title, message and a customizable set of buttons.
 *
 * @example
 * //show dialog
 * var thePressedButton = plugins.dialogs.showInfoDialog('Title', 'Value not allowed','OK', 'Cancel');
 * //show dialog with focus on second button (index 1)
 * var thePressedButton = plugins.dialogs.showInfoDialog('Title', 'Value not allowed','OK', 'Cancel', 1);
 *
 * @param {String} dialogTitle Dialog title.
 * @param {String} [dialogMessage] Dialog message.
 * @param {String...} [buttonsText] variable arguments of button texts. The last argument can be a number specifying the index (0-based) of the button to focus and highlight.
 * 
 * @return {String} The text of the button that was pressed by the user, or null if the dialog was closed without selection.
 */
function showInfoDialog(dialogTitle,dialogMessage,buttonsText) {}

/**
 * Shows a question dialog with the specified title, message and a customizable set of buttons.
 *
 * @example
 * //show dialog
 * var thePressedButton = plugins.dialogs.showQuestionDialog('Title', 'Value not allowed','OK', 'Cancel');
 * //show dialog with focus on second button (index 1)
 * var thePressedButton = plugins.dialogs.showQuestionDialog('Title', 'Value not allowed','OK', 'Cancel', 1);
 *
 * @param {String} dialogTitle Dialog title.
 * @param {String} dialogMessage Dialog message.
 * @param {String...} [buttonsText] variable arguments of button texts. The last argument can be a number specifying the index (0-based) of the button to focus and highlight.
 * 
 * @return {String} The text of the button that was pressed by the user.
 */
function showQuestionDialog(dialogTitle,dialogMessage,buttonsText) {}

/**
 * Shows an input dialog where the user can enter data. Returns the entered data, or nothing when canceled.
 *
 * @example
 * //show input dialog ,returns nothing when canceled
 * var typedInput = plugins.dialogs.showInputDialog('Specify','Your name');
 * 
 * @param {String} [dialogTitle] Dialog title.
 * @param {String} [dialogMessage] Dialog message.
 * @param {String} [initialValue] The default value pre-filled in the input field when the dialog opens.
 * @param {String} [inputType] the type of the input field, one of: color, date, datetime-local, email, month, number, password, tel, text, time, url, week
 * 
 * @return {String} The user-entered value if confirmed, or null if the dialog was canceled.
 */
function showInputDialog(dialogTitle,dialogMessage,initialValue,inputType) {}

/**
 * Shows a selection dialog, where the user can select an entry from a list of options. Returns the selected entry, or nothing when canceled.
 *
 * @example
 * //show select,returns nothing when canceled
 * var selectedValue = plugins.dialogs.showSelectDialog('Select','please select a name','jan','johan','sebastiaan');
 * //also possible to pass array with options
 * //var selectedValue = plugins.dialogs.showSelectDialog('Select','please select a name', new Array('jan','johan','sebastiaan'));
 *
 * @param {String} dialogTitle The title of the selection dialog.
 * @param {String} dialogMessage The message or prompt displayed in the dialog.
 * @param {String...} options A list of selectable options, either as individual strings or an array of strings.
 * 
 * @return {String} The selected option from the list, or null if the dialog was canceled.
 */
function showSelectDialog(dialogTitle,dialogMessage,options) { }

/**
* Shows a message dialog with the specified title, message and a customizable set of buttons.
* Returns pressed button text, in case window is closed without pressing any button return value depends on client type.
*
* @example
* //show dialog
* var thePressedButton = plugins.dialogs.showWarningDialog('Title', 'Value not allowed','OK', 'Cancel');
* //show dialog with focus on second button (index 1)
* var thePressedButton = plugins.dialogs.showWarningDialog('Title', 'Value not allowed','OK', 'Cancel', 1);
*
* @param {String} dialogTitle Dialog title.
* @param {String} dialogMessage Dialog message.
* @param {String...} [buttonsText] variable arguments of button texts. The last argument can be a number specifying the index (0-based) of the button to focus and highlight.
*
* @return {String} pressed button text
*/
function showWarningDialog(dialogTitle,dialogMessage,buttonsText) {}
