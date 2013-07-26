/**
 * called by SigleFileUpload
 */
function addSFUInputChangeListener() {
	
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

}