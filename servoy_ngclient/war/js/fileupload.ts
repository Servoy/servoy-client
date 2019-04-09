angular.module('servoyfileupload',['ngFileUpload', 'sabloApp'])
.controller("FileuploadController", function($scope, $uibModalInstance, Upload, $svyFileuploadUtils,$svyI18NService) {
    
    $scope.getTitleText = function() {
    	return $svyFileuploadUtils.getTitleText();
    };
    
    $scope.isMultiSelect = function() {
    	return $svyFileuploadUtils.isMultiSelect();
    }
	
	$scope.getAcceptFilter = function() {
		return $svyFileuploadUtils.getAcceptFilter();
	}

    $scope.isFileSelected = function() {
    	return $scope.uploadFiles != null; 
    }    
    
    $scope.doRemove = function(f) {
        if($scope.uploadFiles && ! $scope.uploadFiles.length)
            $scope.uploadFiles = [$scope.uploadFiles];
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
    		if($scope.uploadFiles instanceof Array) {
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
        $scope.isUploading = true;
    	$scope.errorText = "";
    	progress = 0;
    	if($scope.isFileSelected()) {
    		$scope.upload = Upload.upload({
    			url: $svyFileuploadUtils.getUploadUrl(),
    			file: $scope.getUploadFiles()
    		})
    		$scope.upload.then(function(resp) {
    			// file is uploaded successfully
    			$scope.dismiss();
    		},
    		function(resp){
    			if (resp.data) $scope.errorText = resp.data;
    			else $scope.errorText = genericError;
    	        $scope.isUploading = false;
			},
			function(evt) {
    			var current = 100.0 * evt.loaded / evt.total;
    			if (current < progress) {
    				$scope.upload.abort();
    			}
    			else progress  = current;
    		});
    	}      	
    };
    
    $scope.dismiss = function() {
    	$uibModalInstance.dismiss();
    };
})
.factory("$svyFileuploadUtils", function($uibModal,$svyI18NService){
	var uploadUrl, titleText, isMultiSelect, acceptFilter;
	return {
		open : function (url, title, multiselect, filter) {
			uploadUrl = url;
			titleText = title;
			isMultiSelect = multiselect;
			acceptFilter = filter;
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
		},
		getAcceptFilter: function() {
			return acceptFilter;
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
    	        var componentModel = modelFunction($scope);
    	        var propertyname = dataproviderString.substring(index+1);
    	        var componentname;
				var parentForm = $scope.$parent;
				
				while(parentForm && !parentForm.hasOwnProperty('formname')) {
					parentForm = parentForm.$parent;
				}

				if(parentForm) {
					if(parentForm['model']) {
						for(var key in parentForm['model']) {
							if (parentForm['model'][key] === componentModel) {
								componentname = key;
								break;
							}
						}
					}
					if (!componentname) {
						$log.error("svyFileupload, component name not found for model string: " + dataproviderString);
						return;
					}
					var formname = parentForm['formname'];
					$element.bind('click', function(event) {
						$svyFileuploadUtils.open("resources/upload/" + $sabloApplication.getClientnr() + "/" + formname + "/" + componentname + "/" + propertyname);
					});    	        
				}
				else {
					$log.error("svyFileupload, no form found for model string: " + dataproviderString);
					return;
				}
            }
            else {
            	$log.error("svyFileupload attached to a element that doesn't have the right (model.value): " + dataproviderString)
            }
        }
	};
});