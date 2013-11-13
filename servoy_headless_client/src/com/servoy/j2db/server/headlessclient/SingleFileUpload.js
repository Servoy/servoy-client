/**
 * called by SigleFileUpload
 */
function addSFUInputChangeListener(translatedMessage) {
	
	var upload = document.getElementById("upload");
	upload.addEventListener("change", function () {
		if (upload.files && upload.files.length > 0) {
			if (typeof upload.files[0].lastModifiedDate === 'undefined') {
				//the browser doesn\'t support the lastModifiedDate property so last modified date will not be available');
			} else {
				var actionURL = upload.form.getAttribute("action");
				if(actionURL.indexOf('&last_modified_') > 0){
					actionURL =actionURL.substring(0,actionURL.indexOf('&last_modified_'));
				}
				actionURL+="&last_modified_"+upload.getAttribute("name")+"_"+encodeURIComponent(upload.files[0].name)+"="+upload.files[0].lastModifiedDate.getTime();
				upload.form.setAttribute("action",actionURL)
			}
		}
	});
	var button = document.getElementById("filebutton");
	button.addEventListener("click", function() {
		var elements_to_hide = button.parentNode.children;
       	for(var i =0; i<elements_to_hide.length; i++) {
       		elements_to_hide[i].hidden = true;
       	 }
		var temp_upload_message = document.createElement('span');
		temp_upload_message.id = "temp_message";
      	temp_upload_message.textContent = translatedMessage;
       	button.parentNode.appendChild(temp_upload_message);
	});
}