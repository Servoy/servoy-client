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
<html ng-app="servoy" ng-controller="MainController">
	<head>
	  <base href="${context}/">
	  <title ng-bind="solutionSettings.solutionTitle">Servoy NGClient</title>
	  <title>{{solutionSettings.solutionTitle}}</title>
	  <!-- base 3th party libraries -->
      <link rel="stylesheet" href="//netdna.bootstrapcdn.com/bootstrap/3.1.0/css/bootstrap.min.css">
      <link rel="stylesheet" href="css/ng-grid.css">
	  <script src="https://ajax.googleapis.com/ajax/libs/angularjs/1.2.11/angular.js"></script>
	  <!-- <script src="https://ajax.googleapis.com/ajax/libs/angularjs/1.2.11/angular.min.js"></script> -->
	  <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.0/jquery.min.js"></script>
	  <script src="js/angular-webstorage.js"></script>
      <script src="js/angularui/ui-bootstrap-tpls-0.10.0.js"></script>
      <script src="js/angularui/ng-grid-2.0.7.debug.js"></script>
      <script src="js/numeral.js"></script>
      
      <!-- base servoy libraries -->
      <link rel="stylesheet" href="css/servoy.css">
	  <script src="js/servoy.js"></script>
	  <script src="js/servoyformat.js"></script>
	  <script src="js/servoytooltip.js"></script>
        
	  <!-- list of all the beans/components maybe having 1 file that includes them all -->
	  <!-- standard "servoydefault.jar , should  be generated" beans -->
	  <script src="servoydefault/label/label.js"></script>
      <script src="servoydefault/button/button.js"></script>
      <script src="servoydefault/textfield/textfield.js"></script>
      <script src="servoydefault/password/password.js"></script>
      <script src="servoydefault/combobox/combobox.js"></script>
      <script src="servoydefault/radiogroup/radiogroup.js"></script>
      <script src="servoydefault/radio/radio.js"></script>
      <script src="servoydefault/checkgroup/checkgroup.js"></script>
      <script src="servoydefault/check/check.js"></script>
      <script src="servoydefault/calendar/calendar.js"></script>
      <script src="servoydefault/typeahead/typeahead.js"></script>
      <script src="servoydefault/tabpanel/tabpanel.js"></script>
      <script src="servoydefault/navigator/navigator.js"></script>
      <script src="servoydefault/textarea/textarea.js"></script>
      <script src="servoydefault/listbox/listbox.js"></script>
      <script src="servoydefault/htmlview/htmlview.js"></script>
      <script src="servoydefault/navigator/default_navigator_container.js"></script> <!-- not a component-->
      <!-- "webcomponents.jar" beans, should  be generated -->
      <script src="webcomponents/namepanel/namepanel.js"></script>

      <!-- split pane using bg-splitter -->
      <script src="servoydefault/splitpane/bg-splitter/js/splitter.js"></script>
      <link rel="stylesheet" href="servoydefault/splitpane/bg-splitter/css/style.css">
      <script src="servoydefault/splitpane/splitpane.js"></script>
            
      <!-- external js/css used by beans, should  be generated -->
      <script src="webcomponents/signaturefield/js/jquery.signaturepad.min.js"></script>
      <script src="webcomponents/signaturefield/signaturefield.js"></script>
      <link rel="stylesheet" href="webcomponents/signaturefield/css/jquery.signaturepad.css">
        
        <!-- inlineeditfield commponent/bean -->
      <script src="webcomponents/inlineeditfield/inlineeditfield.js"></script>
      <link rel="stylesheet" href="webcomponents/inlineeditfield/css/inlineeditfield.css">  

       
        
	  <!-- -list of all the form controller scripts, maybe one solution so have all the controllers as once in 1 file -->
	  <#list formScriptReferences as formScript>
	  	 <script src="${formScript}"></script>
	  </#list>
	</head>
<body >

	<div ng-if="solutionSettings.navigatorForm.templateURL" ng-style="{'width':solutionSettings.navigatorForm.width+'px'}"
	 ng-include="solutionSettings.navigatorForm.templateURL"></div>
	<div ng-include="solutionSettings.mainForm.templateURL"
		ng-style="{'position':'absolute','top':'0px','right':'0px','bottom':'0px','left':solutionSettings.navigatorForm.width+'px'}" ></div>
</body>
</html>
