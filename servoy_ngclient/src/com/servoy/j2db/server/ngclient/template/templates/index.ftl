<#--
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

 This program is free software; you can redistribute it and/or modify it under
 the terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along
 with this program; if not, see http://www.gnu.org/licenses or write to the Free
 Software Foundation,Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
-->
<!DOCTYPE html>
<html ng-app="servoyApp" ng-controller="MainController">
	<head>
		<base href="${context}/">
		<title ng-bind="solutionSettings.solutionTitle"></title>
		<!-- base 3th party libraries -->
		<link rel="stylesheet" href="//netdna.bootstrapcdn.com/bootstrap/3.1.0/css/bootstrap.min.css"/>
		<link rel="stylesheet" href="css/ng-grid.css"/>
		<link rel="stylesheet" href="js/bootstrap-window/css/bootstrap-window.css"/>
		
		<script src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.0/jquery.js"></script>
		<script src="http://netdna.bootstrapcdn.com/bootstrap/3.1.0/js/bootstrap.min.js"></script>
		<script src="https://ajax.googleapis.com/ajax/libs/angularjs/1.2.11/angular.js"></script>
		<!-- <script src="https://ajax.googleapis.com/ajax/libs/angularjs/1.2.11/angular.min.js"></script> -->
		<script src="js/angular-webstorage.js"></script>
		<script src="js/angularui/ui-bootstrap-tpls-0.10.0.js"></script>
		<script src="js/angularui/ng-grid-2.0.7.debug.js"></script>
		<script src="js/numeral.js"></script>
		<script src="js/angular-file-upload/dist/angular-file-upload.min.js"></script>
		<script src="js/bootstrap-window/js/Window.js"></script>    <!-- TODO replace these with bootstrap-window.min.js --> 
		<script src="js/bootstrap-window/js/WindowManager.js"></script>	<!-- see ^ -->
		<script src="js/bindonce.js"></script>	
		
		
		<!-- base servoy libraries -->
		<link rel="stylesheet" href="css/servoy.css"/>
		<script src="js/servoy.js"></script>
		<script src="js/servoyWindowManager.js"></script>
		<script src="js/servoyformat.js"></script>
		<script src="js/servoytooltip.js"></script>
		<script src="js/fileupload.js"></script>
		<script src="js/servoy-components.js"></script>
		<script src="js/websocket.js"></script>
		<script src="js/servoy_app.js"></script>
		<script src="js/jquery.maskedinput.js"></script>

		<#list componentReferences as componentScript>
			<script src="${componentScript}"></script>
		</#list>
		<#list componentCssReferences as componentCss>
			<link rel="stylesheet" href="${componentCss}"/>
		</#list>
		<#list componentJsReferences as componentJs>
			<script src="${componentJs}"></script>
		</#list>
		
		<!-- -list of all the form controller scripts, maybe one solution so have all the controllers as once in 1 file -->
		<#list formScriptReferences as formScript>
			<script src="${formScript}"></script>
		</#list>
		

		<link rel="stylesheet" ng-href='{{solutionSettings.styleSheetPath}}'/>
	</head>
	<body >
		<div ng-if="solutionSettings.navigatorForm.templateURL" ng-style="{'width':solutionSettings.navigatorForm.width+'px'}"
			ng-include="getNavigatorFormUrl()"></div>
		<div ng-include="getMainFormUrl()" ng-style="{'position':'absolute','top':'0px','right':'0px','bottom':'0px','left':solutionSettings.navigatorForm.width+'px'}"></div>
		<div ng-include="updatingFormUrl" style="position:absolute;visibility:hidden;"></div>
	</body>
</html>
