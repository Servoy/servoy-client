servoyModule.directive('svyTextfield', function($servoy) {  
    return {
      restrict: 'E',
      transclude: true,
      scope: {
        model: "=svyModel",
        api: "=svyApi"
      },
      controller: function($scope, $element, $attrs) {
    	 // fill in the api defined in the spec file
    	 $scope.api.onDataChangeCallback = function(event, returnval) {
    		 if(!returnval) {
    			 $element[0].focus();
    		 }
    	 },
    	 $scope.api.requestFocus = function() { 
    		  $element[0].focus()
    	 },
    	 $scope.api.getSelectedText = function() { 
    		 var elem = $element[0];
    		 return elem.value.substr(elem.selectionStart, elem.selectionEnd - elem.selectionStart);
    	 }
    	 $scope.api.setSelection = function(start, end) { 
    		 var elem = $element[0];
    		 if (elem.createTextRange) {
    		      var selRange = elem.createTextRange();
    		      selRange.collapse(true);
    		      selRange.moveStart('character', start);
    		      selRange.moveEnd('character', end);
    		      selRange.select();
    		      elem.focus();
    		 } else if (elem.setSelectionRange) {
    		    	elem.focus();
    		    	elem.setSelectionRange(start, end);
    		 } else if (typeof elem.selectionStart != 'undefined') {
    		    	elem.selectionStart = start;
    		    	elem.selectionEnd = end;
    		    	elem.focus();
    		 } 
    	 }
      },
      templateUrl: 'servoydefault/textfield/textfield.html',
      replace: true
    };
  })

  
  
  
  
