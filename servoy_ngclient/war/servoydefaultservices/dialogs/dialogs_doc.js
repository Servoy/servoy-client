/**
 * Shows an error dialog with the specified title, message and a customizable set of buttons.
 *
 * @example
 * //show dialog
 * var thePressedButton = plugins.dialogs.showErrorDialog('Title', 'Value not allowed','OK', 'Cancel');
 *
 * @param {String} dialogTitle Dialog title.
 * @param {String} dialogMessage Dialog message.
 * @param {String...} buttonsText variable arguments of button texts..
 * 
 * @return {string} pressed button or null if closed by escape key
 */
function showErrorDialog(dialogTitle,dialogMessage,buttonsText) {}

/**
 *  Shows an info dialog with the specified title, message and a customizable set of buttons.
 *
 * @example
 * //show dialog
 * var thePressedButton = plugins.dialogs.showInfoDialog('Title', 'Value not allowed','OK', 'Cancel');
 *
 * @param {String} dialogTitle Dialog title.
 * @param {String} dialogMessage Dialog message.
 * @param {String...} buttonsText variable arguments of button texts..
 */
function showInfoDialog(dialogTitle,dialogMessage,buttonsText) {}

/**
 * Shows a question dialog with the specified title, message and a customizable set of buttons.
 *
 * @example
 * //show dialog
 * var thePressedButton = plugins.dialogs.showQuestionDialog('Title', 'Value not allowed','OK', 'Cancel');
 *
 * @param {String} dialogTitle Dialog title.
 * @param {String} dialogMessage Dialog message.
 * @param {String...} buttonsText variable arguments of button texts..
 * 
 * @return {String}
 */
function showQuestionDialog(dialogTitle,dialogMessage,buttonsText) {}

/**
 * Shows an input dialog where the user can enter data. Returns the entered data, or nothing when canceled.
 *
 * @example
 * //show input dialog ,returns nothing when canceled
 * var typedInput = plugins.dialogs.showInputDialog('Specify','Your name');
 * 
 * @param {String} dialogTitle Dialog title.
 * @param {String} dialogMessage Dialog message.
 * @param {String} initialValue 
 * @param {String} inputType the type of the input field, one of: color, date, datetime-local, email, month, number, password, tel, text, time, url, week
 * 
 * @return {String}
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
 * @param {String} dialog_title
 * @param {String} msg
 * @param {String...|Array<String>} options
 */
function showSelectDialog(dialogTitle,dialogMessage,options) { }

/**
* Shows a message dialog with the specified title, message and a customizable set of buttons.
* Returns pressed button text, in case window is closed without pressing any button return value depends on client type.
*
* @example
* //show dialog
* var thePressedButton = plugins.dialogs.showWarningDialog('Title', 'Value not allowed','OK', 'Cancel');
*
* @param {String} dialogTitle Dialog title.
* @param {String} dialogMessage Dialog message.
* @param {String...} buttonsText variable arguments of button texts.
*
* @return pressed button text
*/
function showWarningDialog(dialogTitle,dialogMessage,buttonsText) {}
