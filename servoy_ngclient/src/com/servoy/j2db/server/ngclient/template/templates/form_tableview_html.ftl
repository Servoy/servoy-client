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
<div ng-controller="${name}" ng-style="formStyle" svy-layout-update>
<#list parts as part>
	<div ng-style="${part.name}Style">
		<#if (part.baseComponents)??>
			<#list part.baseComponents as bc>
					<div ng-style="layout.${bc.name}" svy-layout-update="${bc.name}">
						<${bc.tagname} ${bc.multiple} name="${bc.name}" svy-model="model.${bc.name}" svy-api="api.${bc.name}" svy-handlers="handlers.${bc.name}" svy-apply="handlers.${bc.name}.svy_apply"/>
					</div>
			</#list>
		<#else>
		    <div class="gridStyle" ng-grid="grid${name}"></div>
		</#if>
	</div>
</#list>
</div>