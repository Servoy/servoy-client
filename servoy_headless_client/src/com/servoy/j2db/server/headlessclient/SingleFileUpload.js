/**
 * called by SigleFileUpload
*/
function addInputChangeListener(){
var upload = document.getElementById("upload");
upload.addEventListener("change", function () {
	if (element.files && this.files.length > 0) {
       if (typeof this.files[0].lastModifiedDate === 'undefined') {
                     //the browser doesn\'t support the lastModifiedDate property so last modified date will not be available');
            } else {
            	var actionURL = this.form.getAttribute("action");
            	if(actionURL.indexOf('&last_modified_') > 0){
				     actionURL =actionURL.substring(0,actionURL.indexOf('&last_modified_'));
            	}
                actionURL+="&last_modified_"+this.getAttribute("name")+"="+this.files[0].lastModifiedDate.getTime();
				this.form.setAttribute("action",actionURL)
            }
    }
});

}