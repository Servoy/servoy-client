<#ftl output_format="JavaScript">
angular.module('servoyApp').run(function($solutionSettings, $svyI18NService, $webSocket){
        if (jQuery.UNSAFE_restoreLegacyHtmlPrefilter) {
            jQuery.UNSAFE_restoreLegacyHtmlPrefilter();
        }
        else {        
            var rxhtmlTag = /<(?!area|br|col|embed|hr|img|input|link|meta|param)(([a-z][^\/\0>\x20\t\r\n\f]*)[^>]*)\/>/gi;
            jQuery.htmlPrefilter = function( html ) {
                return html.replace( rxhtmlTag, "<$1></$2>" );
            };
        }
		<#-- set pathname as received by the client, may be different from browser in case of url rewrite -->
		<#--refer to solutions/<solution>/index.html, we are in solutions/<solution>/<clientnr>/main/startup.js -->
		$webSocket.setPathname("${pathname?replace('[^/]*/[^/]*/startup.js$', 'index.html', 'r')}");
		$webSocket.setQueryString("${querystring}");
	
		var orientation = ${orientation};
		if (orientation == 2)
		{
			$solutionSettings.ltrOrientation = false;
		}
		else if (orientation == 3)
		{
			language = window.navigator.language?window.navigator.language:window.navigator.browserLanguage;
			language = language.split('-')[0];
			if (language == 'iw' || language == 'ar' ||language == 'fa' ||language == 'ur')
			{
				$solutionSettings.ltrOrientation = false;
			}
			else
			{
				$solutionSettings.ltrOrientation = true;
			}
		}
		else
		{
			$solutionSettings.ltrOrientation = true;
		}

	<#-- inject default tranlations -->
	$svyI18NService.addDefaultTranslations(JSON.parse("${defaultTranslations}"));	
	
});
window.servoy_remoteaddr = "${ipaddr}"
window.servoy_remotehost = "${hostaddr}"
