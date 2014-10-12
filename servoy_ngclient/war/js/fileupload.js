angular.module('servoyfileupload',['webStorageModule','angularFileUpload']).directive('svyFileupload', function ($modal, $upload, $window, $parse, webStorage) {
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
    	        
    	        // TODO deprecate svy_cn? remove from codebase if possible
    	        if(beanModel.svy_cn === undefined) {
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
    	        } else {
    	        	beanname = beanModel.svy_cn;
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
    	        
    	        $scope.uploadURL = "resources/upload/" + webStorage.session.get("sessionid") + "/" + formname + "/" + beanname + "/" + propertyname;
    	        $scope.uploadFile = null;
    	        $scope.modalInstance = null;
            	
    	        $element.bind('click', function(event) {
    	        	$scope.modalInstance = $modal.open({
    	        			templateUrl: 'templates/upload.html',
    	        			scope: $scope
    	        		});
    	        });

    	        $scope.onFileSelect = function($files) {
    	        	if($files.length > 0) {
    	        		$scope.uploadFile = $files[0];
    	        	}
    	        };
    	        
    	        $scope.doUpload = function() {
    	        	if($scope.uploadFile) {
    	        		$scope.upload = $upload.upload({
    	        			url: $scope.uploadURL,
    	        			file: $scope.uploadFile
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
    	        	$scope.modalInstance.dismiss();
    	        };    	        
            }
            else {
            	$log.error("svyFileupload attached to a element that doesn't have the right (model.value): " + dataproviderString)
            }
        }
	};
});