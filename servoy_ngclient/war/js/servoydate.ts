angular.module('servoydate',[]).factory("$svyDateUtils", function($applicationService){
		
	function isNoDateConversion(beanModel) {
		if (beanModel && beanModel.clientProperty && angular.isDefined(beanModel.clientProperty.ngNoDateConversion)) {
			return beanModel.clientProperty.ngNoDateConversion;
		}
		return $applicationService.getUIProperty('ngNoDateConversion');
	}

	return {
		getSameAsOnServer : function (d, beanModel) {
			if(isNoDateConversion(beanModel)) {
                var newDate = new Date(d.getTime());
                var utcOffsets = $applicationService.getUtcOffsetsAndLocale();
                var utcDiff = utcOffsets.utcDstOffset - utcOffsets.remote_utcOffset;
                newDate.setHours(newDate.getHours() - utcDiff);
                return newDate;
            }
            return d;
		},
		
		getSameAsOnClient: function(d, beanModel) {
            if(isNoDateConversion(beanModel)) {
                var newDate = new Date(d.getTime());
                var utcOffsets = $applicationService.getUtcOffsetsAndLocale();
                var utcDiff = utcOffsets.utcDstOffset - utcOffsets.remote_utcOffset;
                newDate.setHours(newDate.getHours() + utcDiff);
                return newDate;
            }
            return d;
		}  
	}
});