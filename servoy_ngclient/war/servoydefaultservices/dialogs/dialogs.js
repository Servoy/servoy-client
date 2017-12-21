angular.module('dialogs',['servoy'])
.factory("dialogs",function($q,$sabloTestability) {
	return {
		/**
		 * Shows an error dialog with the specified title, message and a customizable set of buttons.
		 *
		 * @example
		 * //show dialog
		 * var thePressedButton = plugins.dialogs.showErrorDialog('Title', 'Value not allowed','OK');
		 *
		 * @param dialogTitle Dialog title.
		 * @param dialogMessage Dialog message.
		 * @param buttonsText Array of button texts.
		 * 
		 * @return {string} pressed button or null if closed by escape key
		 */
		showErrorDialog: function(dialogTitle,dialogMessage,buttonsText) {
			return this.showDialog(dialogTitle,dialogMessage,buttonsText,'type-error');
		},
		/**
		 *  Shows an info dialog with the specified title, message and a customizable set of buttons.
		 *
		 * @example
		 * //show dialog
		 * var thePressedButton = plugins.dialogs.showInfoDialog('Title', 'Value not allowed','OK');
		 *
		 * @param dialogTitle Dialog title.
		 * @param dialogMessage Dialog message.
		 * @param buttonsText Array of button texts.
		 */
		showInfoDialog: function(dialogTitle,dialogMessage,buttonsText) {
			return this.showDialog(dialogTitle,dialogMessage,buttonsText,'type-info');
		},
		/**
		 * Shows a question dialog with the specified title, message and a customizable set of buttons.
		 *
		 * @example
		 * //show dialog
		 * var thePressedButton = plugins.dialogs.showQuestionDialog('Title', 'Value not allowed','OK');
		 *
		 * @param dialogTitle Dialog title.
		 * @param dialogMessage Dialog message.
		 * @param buttonsText Array of button texts.
		 * 
		 * @return {String}
		 */
		showQuestionDialog: function(dialogTitle,dialogMessage,buttonsText) {
			return this.showDialog(dialogTitle,dialogMessage,buttonsText,'type-question');
		},
		/**
		 * Shows an input dialog where the user can enter data. Returns the entered data, or nothing when canceled.
		 *
		 * @example
		 * //show input dialog ,returns nothing when canceled
		 * var typedInput = plugins.dialogs.showInputDialog('Specify','Your name');
		 * 
		 * @param dialogTitle Dialog title.
		 * @param dialogMessage Dialog message.
		 * @param initialValue 
		 * 
		 * @return {String}
		 */
		showInputDialog: function(dialogTitle,dialogMessage,initialValue) {
			if (!dialogTitle) dialogTitle = "Enter value";
			var dialogOpenedDeferred = $q.defer();
			var dialogOptions = {
					  callback: function(value) {  dialogOpenedDeferred.resolve(value); },
					  closeButton: false,
					  title: dialogTitle,
					  message: dialogMessage,
					  value: initialValue
					};
			var dialog = bootbox.prompt(dialogOptions);
			$sabloTestability.block(true);
			dialog.on("shown.bs.modal", function() {
				$sabloTestability.block(false);
			})
			return dialogOpenedDeferred.promise;
		},
		/**
		 * Shows a selection dialog, where the user can select an entry from a list of options. Returns the selected entry, or nothing when canceled.
		 *
		 * @example
		 * //show select,returns nothing when canceled
		 * var selectedValue = plugins.dialogs.showSelectDialog('Select','please select a name','jan','johan','sebastiaan');
		 * //also possible to pass array with options
		 * //var selectedValue = plugins.dialogs.showSelectDialog('Select','please select a name', new Array('jan','johan','sebastiaan'));
		 *
		 * @param dialog_title
		 * @param msg
		 * @param options
		 */
		showSelectDialog: function(dialogTitle,dialogMessage,options) {
			if (!dialogTitle) dialogTitle = "Select value";
			for (var i=0;i<options.length;i++)
			{
				var text = options[i];
				var option = {};
				option.value = text;
				option.text = text;
				options[i] = option
			}	
			var dialogOpenedDeferred = $q.defer();
			var dialogOptions = {
					  callback: function(value) {  dialogOpenedDeferred.resolve(value); },
					  closeButton: false,
					  title: dialogTitle,
					  message: dialogMessage,
					  inputType: 'select',
					  inputOptions: options
					};
			var dialog = bootbox.prompt(dialogOptions);
			$sabloTestability.block(true);
			dialog.on("shown.bs.modal", function() {
				$sabloTestability.block(false);
			})
			return dialogOpenedDeferred.promise;
		},
		showWarningDialog: function(dialogTitle,dialogMessage,buttonsText) {
			return this.showDialog(dialogTitle,dialogMessage,buttonsText,'type-warning');
		},
		showDialog: function(dialogTitle,dialogMessage,buttonsText,extraClass)
		{
			var dialogOpenedDeferred = $q.defer();
			var dialogOptions = {
					  buttons: {
					  },
					  closeButton: false,
					  onEscape: function(){  dialogOpenedDeferred.resolve(null) }
					};
			if (dialogTitle) dialogOptions.title = dialogTitle;
			dialogOptions.message = dialogMessage ? dialogMessage : " ";
			if (extraClass) dialogOptions.className = extraClass;
			if (!buttonsText)
			{
				buttonsText = ["OK"]
			}
			else if (!Array.isArray(buttonsText))
			{
				buttonsText = [buttonsText];
			}
			if (buttonsText)
			{
				for (var i=0;i<buttonsText.length;i++)
				{
					var buttonModel = {
						      label: this.escapeString(buttonsText[i], true),
						      callback: function(event) {
						    	 dialogOpenedDeferred.resolve(event.target.innerHTML);
						      }
					}
					if (i==0) buttonModel.className =  "btn-primary";
					else buttonModel.className =  "btn-default";
					dialogOptions.buttons[this.escapeString(buttonsText[i], false)] = buttonModel;
				}	
			}	
			var dialog = bootbox.dialog(dialogOptions);
			$sabloTestability.block(true);
			dialog.on("shown.bs.modal", function() {
				$sabloTestability.block(false);
			})
			return dialogOpenedDeferred.promise;
		},
		escapeString: function(str, forDisplay) {
      		return str.replace(/&/g, forDisplay ? '&amp;' : 'amp').
			  replace(/</g, forDisplay ? '&lt;' : 'lt').
			  replace(/>/g, forDisplay ? '&gt;' : 'gt').
			  replace(/'/g, forDisplay ? '&apos;' : 'apos').
			  replace(/"/g, forDisplay ? '&quot;' : 'quot');
		}
	}
})
