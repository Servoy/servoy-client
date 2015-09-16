angular.module('servoyfileupload',['angularFileUpload', 'sabloApp'])
.controller("FileuploadController", function($scope, $modalInstance, $upload, $svyFileuploadUtils,$svyI18NService) {
    
    $scope.getTitleText = function() {
    	return $svyFileuploadUtils.getTitleText();
    };
    
    $scope.isMultiSelect = function() {
    	return $svyFileuploadUtils.isMultiSelect();
    }
    
    $scope.isFileSelected = function() {
    	return $scope.uploadFiles && $scope.uploadFiles.length; 
    }    
    
    $scope.doRemove = function(f) {
    	var idx = $scope.uploadFiles.indexOf(f);
    	$scope.uploadFiles.splice(idx, 1);
    }
    
    $scope.i18n_upload = "Upload"
	$scope.i18n_chooseFiles = "Add more files"
	$scope.i18n_cancel = "Cancel"
	$scope.i18n_selectedFiles =	"Selected files"
	$scope.i18n_nothingSelected = "Nothing selected, yet"
	$scope.i18n_remove = "Remove" 
	$scope.i18n_name = "Name" 
		
    
    var x = $svyI18NService.getI18NMessages("servoy.filechooser.button.upload","servoy.filechooser.upload.addMoreFiles","servoy.filechooser.selected.files","servoy.filechooser.nothing.selected","servoy.filechooser.button.remove","servoy.filechooser.label.name","servoy.button.cancel")
    x.then(function(result) {
    	$scope.i18n_upload = result["servoy.filechooser.button.upload"];
    	$scope.i18n_chooseFiles = result["servoy.filechooser.upload.addMoreFiles"];
    	$scope.i18n_cancel = result["servoy.button.cancel"];
    	$scope.i18n_selectedFiles = result["servoy.filechooser.selected.files"];
    	$scope.i18n_nothingSelected = result["servoy.filechooser.nothing.selected"];
    	$scope.i18n_remove = result["servoy.filechooser.button.remove"];
    	$scope.i18n_name = result["servoy.filechooser.label.name"];
    })
    
    $scope.doUpload = function() {
    	if($scope.isFileSelected()) {
    		$scope.upload = $upload.upload({
    			url: $svyFileuploadUtils.getUploadUrl(),
    			file: $scope.uploadFiles
    		}).progress(function(evt) {
    			console.log('percent: ' + parseInt(100.0 * evt.loaded / evt.total));
    		}).success(function(data, status, headers, config) {
    			// file is uploaded successfully
    			$scope.dismiss();
    		});
    		//.error(...)
    		//.then(success, error, progress);
    	} 	        	
    };
    
    $scope.dismiss = function() {
    	$modalInstance.dismiss();
    };
})
.factory("$svyFileuploadUtils", function($modal,$svyI18NService){
	var uploadUrl, titleText, isMultiSelect;
	return {
		open : function (url, title, multiselect) {
			uploadUrl = url;
			titleText = title;
			isMultiSelect = multiselect;
			var x = $svyI18NService.getI18NMessages("servoy.filechooser.button.upload","servoy.filechooser.upload.addMoreFiles","servoy.filechooser.selected.files","servoy.filechooser.nothing.selected","servoy.filechooser.button.remove","servoy.filechooser.label.name","servoy.button.cancel")
		    x.then(function(result) {
				$modal.open({
		        	templateUrl: 'templates/upload.html',
		        	controller: 'FileuploadController'
		        });
		    })
		},
		getUploadUrl: function() {
			return uploadUrl;
		},
		getTitleText: function() {
			return titleText;
		},
		isMultiSelect: function() {
			return isMultiSelect;
		}
	}
})
.directive('svyFileupload', function ($parse, $sabloApplication, $svyFileuploadUtils) {
	return {
		restrict: 'A',
        link: function ($scope, $element, $attrs) {
        	
            var dataproviderString = $attrs['svyFileupload'];
            var index = dataproviderString.indexOf('.');
            if (index > 0) {
    	        var modelString = dataproviderString.substring(0,index);
    	        var modelFunction = $parse(modelString);
    	        var beanModel = modelFunction($scope);
    	        var propertyname = dataproviderString.substring(index+1);
    	        var beanname;
    	        var parent = $scope.$parent;

    	        beanname = $element.attr("name");
    	        if (! beanname) {
    	        	var nameParentEl = $element.parents("[name]").first(); 
    	        	if (nameParentEl) beanname = nameParentEl.attr("name");
    	        }
    	        if (! beanname) {
    	        	for(key in parent.model) {
    	        		if (parent.model[key] === beanModel) {
    	        			beanname = key;
    	        			break;
    	        		}
    	        	}
    	        }
    	        
    	        if (!beanname) {
    	        	$log.error("bean name not found for model string: " + dataproviderString);
    	        	return;
    	        }
    	        
    	        var formname = parent.formname;
    	        while (!formname) {
    	        	if (parent.$parent) {
    	        		parent = parent.$parent;
    	        		formname = parent.formname;
    	        	}
    	        	else { 
    	        		$log.error("no form found for " + bean + "." + propertyname);
    	        		return;
    	        	}
    	        }

    	        $element.bind('click', function(event) {
    	        	$svyFileuploadUtils.open("resources/upload/" + $sabloApplication.getSessionId() + "/" + formname + "/" + beanname + "/" + propertyname);
    	        });    	        
            }
            else {
            	$log.error("svyFileupload attached to a element that doesn't have the right (model.value): " + dataproviderString)
            }
        }
	};
});