/**
 *  Change default file upload implementation to try to use HTML5 input 'multiple' attribute 
 *      [for multiple file upload]
 *         Andrei Costescu
 */
function MultipleFileUploadInterceptor(multiSelector)
{
	var oldAddElement = multiSelector.addElement;
	multiSelector.addElement = function(element)
	{
		if(element.tagName.toLowerCase() == 'input' && element.type.toLowerCase() == 'file') {
			element.multiple = "multiple";
		}
		oldAddElement.call(this, element);
	}.bind(multiSelector);
	
	multiSelector.addListRow = function( element ){

		// Row div
		var new_row = document.createElement('tr');
		var contentsColumn = document.createElement('td');
		var buttonColumn = document.createElement('td');

		// Delete button
		var new_row_button = document.createElement( 'input' );
		new_row_button.type = 'button';
		new_row_button.value = this.delete_label;

		// References
		new_row.element = element;

		// Delete function
		new_row_button.onclick= function(){

			// Remove element from form
			this.parentNode.parentNode.element.parentNode.removeChild( this.parentNode.parentNode.element );

			// Remove this row from the list
			this.parentNode.parentNode.parentNode.removeChild( this.parentNode.parentNode );

			// Decrement counter
			this.parentNode.parentNode.element.multi_selector.count--;

			// Re-enable input element (if it's disabled)
			this.parentNode.parentNode.element.multi_selector.current_element.disabled = false;

			// Appease Safari
			//    without it Safari wants to reload the browser window
			//    which nixes your already queued uploads
			return false;
		};

		// Set row value
		contentsColumn.innerHTML = this.getOnlyFileNames(element);
		new_row.appendChild( contentsColumn );
		
		// Add button
		buttonColumn.innerHTML = "&nbsp&nbsp&nbsp";
		buttonColumn.appendChild( new_row_button );
		new_row.appendChild( buttonColumn );

		// Add it to the list
		this.list_target.appendChild( new_row );
		
	}.bind(multiSelector);
	
	multiSelector.getOnlyFileNames = function(inputElement)
	{
		if (inputElement.files && inputElement.files.length > 0)
		{
			var files = inputElement.files;
			var retVal = "";
			for (var i = 0; i < files.length; i++)
			{
				retVal += this.getOnlyFileName(files[i].name) + '<br>';
			}
			return retVal.slice(0, retVal.length - 4);
		}
		else
		{
			return this.getOnlyFileName(inputElement.value);
		}
	}.bind(multiSelector);

	multiSelector.getOnlyFileName = function(stringValue)
	{
		var separatorIndex1 = stringValue.lastIndexOf('\\');
		var separatorIndex2 = stringValue.lastIndexOf('/');
		separatorIndex1 = Math.max(separatorIndex1, separatorIndex2);
		return separatorIndex1 >= 0 ? stringValue.slice(separatorIndex1 + 1, stringValue.length) : stringValue;
	}.bind(multiSelector);
	
	return multiSelector;
}