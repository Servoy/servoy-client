angular.module('dialogs',['servoy'])
.factory("dialogs",function($q) {
	return {
		showErrorDialog: function(dialogTitle,dialogMessage,buttonsText) {
			if (arguments.length > 3)
			{
				var buttons = [];
				for (var i=2;i<arguments.length;i++)
				{
					buttons.push(arguments[i]);
				}
				buttonsText = buttons;
			}	
			return this.showDialog(dialogTitle,dialogMessage,buttonsText,'type-error');
		},
		showInfoDialog: function(dialogTitle,dialogMessage,buttonsText) {
			if (arguments.length > 3)
			{
				var buttons = [];
				for (var i=2;i<arguments.length;i++)
				{
					buttons.push(arguments[i]);
				}
				buttonsText = buttons;
			}		
			return this.showDialog(dialogTitle,dialogMessage,buttonsText,'type-info');
		},
		showQuestionDialog: function(dialogTitle,dialogMessage,buttonsText) {
			if (arguments.length > 3)
			{
				var buttons = [];
				for (var i=2;i<arguments.length;i++)
				{
					buttons.push(arguments[i]);
				}
				buttonsText = buttons;
			}		
			return this.showDialog(dialogTitle,dialogMessage,buttonsText,'type-question');
		},
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
			bootbox.prompt(dialogOptions);
			return dialogOpenedDeferred.promise;
		},
		showSelectDialog: function(dialogTitle,dialogMessage,options) {
			if (!dialogTitle) dialogTitle = "Select value";
			if (arguments.length > 3)
			{
				var temp = [];
				for (var i=2;i<arguments.length;i++)
				{
					temp.push(arguments[i]);
				}
				options = temp;
			}
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
			bootbox.prompt(dialogOptions);
			return dialogOpenedDeferred.promise;
		},
		showWarningDialog: function(dialogTitle,dialogMessage,buttonsText) {
			if (arguments.length > 3)
			{
				var buttons = [];
				for (var i=2;i<arguments.length;i++)
				{
					buttons.push(arguments[i]);
				}
				buttonsText = buttons;
			}		
			return this.showDialog(dialogTitle,dialogMessage,buttonsText,'type-warning');
		},
		showDialog: function(dialogTitle,dialogMessage,buttonsText,extraClass)
		{
			var dialogOpenedDeferred = $q.defer();
			var dialogOptions = {
					  buttons: {
					  },
					  closeButton: false
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
						      label: buttonsText[i],
						      callback: function(event) {
						    	 dialogOpenedDeferred.resolve(event.target.innerText);
						      }
					}
					if (i==0) buttonModel.className =  "btn-primary";
					else buttonModel.className =  "btn-default";
					dialogOptions.buttons[buttonsText[i]] = buttonModel;
				}	
			}	
			bootbox.dialog(dialogOptions);
			return dialogOpenedDeferred.promise;
		}
	}
})
