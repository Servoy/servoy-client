angular.module('servoyfileupload',['ngFileUpload', 'sabloApp'])
.controller("FileuploadController", function($scope, $uibModalInstance, Upload, $svyFileuploadUtils,$svyI18NService) {
    
    $scope.getTitleText = function() {
    	return $svyFileuploadUtils.getTitleText();
    };
    
    $scope.isMultiSelect = function() {
    	return $svyFileuploadUtils.isMultiSelect();
    }
    
    $scope.isFileSelected = function() {
    	return $scope.uploadFiles != null; 
    }    
    
    $scope.doRemove = function(f) {
    	if($scope.uploadFiles.length) {
	    	var idx = $scope.getUploadFiles().indexOf(f);
	    	$scope.uploadFiles.splice(idx, 1);
    	}
    	else {
    		$scope.uploadFiles = null;
    	}
    	progress = 0;
    	$scope.errorText = "";
    }
    
    $scope.getUploadFiles = function() {
    	if($scope.uploadFiles) {
    		if($scope.uploadFiles.length) {
    			return $scope.uploadFiles;
    		}
    		else {
    			return [$scope.uploadFiles];
    		}
    	}
    	return null;
    }
    
    $scope.i18n_upload = "Upload"
	$scope.i18n_chooseFiles = "Select a file"
	$scope.i18n_cancel = "Cancel"
	$scope.i18n_selectedFiles =	"Selected files"
	$scope.i18n_nothingSelected = "Nothing selected, yet"
	$scope.i18n_remove = "Remove" 
	$scope.i18n_name = "Name" 
	var genericError = "";
		
    
    var x = $svyI18NService.getI18NMessages("servoy.filechooser.button.upload","servoy.filechooser.upload.addFile","servoy.filechooser.upload.addFiles","servoy.filechooser.selected.files","servoy.filechooser.nothing.selected","servoy.filechooser.button.remove","servoy.filechooser.label.name","servoy.button.cancel", "servoy.filechooser.error")
    x.then(function(result) {
    	$scope.i18n_upload = result["servoy.filechooser.button.upload"];
    	if ($scope.isMultiSelect())
    		$scope.i18n_chooseFiles = result["servoy.filechooser.upload.addFiles"];
    	else
    		$scope.i18n_chooseFiles = result["servoy.filechooser.upload.addFile"];
    	$scope.i18n_cancel = result["servoy.button.cancel"];
    	$scope.i18n_selectedFiles = result["servoy.filechooser.selected.files"];
    	$scope.i18n_nothingSelected = result["servoy.filechooser.nothing.selected"];
    	$scope.i18n_remove = result["servoy.filechooser.button.remove"];
    	$scope.i18n_name = result["servoy.filechooser.label.name"];
    	genericError = result["servoy.filechooser.error"];
    })
    
    $scope.errorText = "";
    var progress = 0;
    
    
    $scope.getProgress = function(postFix) {
    	if (progress)return Math.round(progress) + postFix;
    	return "";
    }
    
    $scope.doUpload = function() {
    	$scope.errorText = "";
    	progress = 0;
    	if($scope.isFileSelected()) {
    		$scope.upload = Upload.upload({
    			url: $svyFileuploadUtils.getUploadUrl(),
    			file: $scope.getUploadFiles()
    		}).progress(function(evt) {
    			var current = 100.0 * evt.loaded / evt.total;
    			if (current < progress) {
    				$scope.upload.abort();
    			}
    			else progress  = current;
    		}).success(function(data, status, headers, config) {
    			// file is uploaded successfully
    			$scope.dismiss();
    		}).error(function(status,status2){
    			if (status) $scope.errorText = status;
    			else $scope.errorText = genericError;
    		});
    	} 	        	
    };
    
    $scope.dismiss = function() {
    	$uibModalInstance.dismiss();
    };
})
.factory("$svyFileuploadUtils", function($uibModal,$svyI18NService){
	var uploadUrl, titleText, isMultiSelect;
	return {
		open : function (url, title, multiselect) {
			uploadUrl = url;
			titleText = title;
			isMultiSelect = multiselect;
			var x = $svyI18NService.getI18NMessages("servoy.filechooser.button.upload","servoy.filechooser.upload.addFile","servoy.filechooser.upload.addFiles","servoy.filechooser.selected.files","servoy.filechooser.nothing.selected","servoy.filechooser.button.remove","servoy.filechooser.label.name","servoy.button.cancel", "servoy.filechooser.error")
		    x.then(function(result) {
				$uibModal.open({
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
.directive('svyFileupload', function ($parse, $sabloApplication, $svyFileuploadUtils, $log) {
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
    	        	for(var key in parent['model']) {
    	        		if (parent['model'][key] === beanModel) {
    	        			beanname = key;
    	        			break;
    	        		}
    	        	}
    	        }
    	        
    	        if (!beanname) {
    	        	$log.error("bean name not found for model string: " + dataproviderString);
    	        	return;
    	        }
    	        
    	        var formname = parent['formname'];
    	        while (!formname) {
    	        	if (parent.$parent) {
    	        		parent = parent.$parent;
    	        		formname = parent['formname'];
    	        	}
    	        	else { 
    	        		$log.error("no form found for " + beanname + "." + propertyname);
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