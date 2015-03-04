angular.module('servoyfileupload',['angularFileUpload', 'sabloApp'])
.controller("FileuploadController", function($scope, $modalInstance, $upload, $svyFileuploadUtils) {
    
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
    
    $scope.doUpload = function() {
    	if($scope.isFileSelected()) {
    		for(var i = 0; i < $scope.uploadFiles.length; i++) {
	    		$scope.upload = $upload.upload({
	    			url: $svyFileuploadUtils.getUploadUrl(),
	    			file: $scope.uploadFiles[i]
	    		}).progress(function(evt) {
	    			console.log('percent: ' + parseInt(100.0 * evt.loaded / evt.total));
	    		}).success(function(data, status, headers, config) {
	    			// file is uploaded successfully
	    			$scope.dismiss();
	    		});
	    		//.error(...)
	    		//.then(success, error, progress);
    		}
    	} 	        	
    };
    
    $scope.dismiss = function() {
    	$modalInstance.dismiss();
    };
})
.factory("$svyFileuploadUtils", function($modal){
	var uploadUrl, titleText, isMultiSelect;
	return {
		open : function (url, title, multiselect) {
			uploadUrl = url;
			titleText = title;
			isMultiSelect = multiselect;
			$modal.open({
	        	templateUrl: 'templates/upload.html',
	        	controller: 'FileuploadController'
	        });
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