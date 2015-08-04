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
{
	"name": "servoydefault-${name}",
	"displayName": "${displayName}",
	"version": ${version},
	<#if icon??>"icon": "servoydefault/${name}/${icon}",
	</#if>"definition": "servoydefault/${name}/${name}.js",
	<#if serverScript??>"serverscript": "${serverScript}",
	</#if>"libraries": [<#list libraries as lib>${lib}<#if lib_has_next>,</#if></#list>],
	"model":
	{
	    <#list model as prop>
	        "${prop.name}" : ${getPropTypeWithDefault(name, prop)}<#if prop_has_next>,</#if> 
	    </#list>
	},
	"handlers":
	{
	    <#list handlers as prop>
	        "${prop.name}" : {
	        <#if (prop.returnType?? && prop.returnType != "void")>  "returns": "${prop.returnType}"<#if (prop.parameters?size>0 || prop.hints??)>, </#if>
	        </#if> <#if (prop.parameters?? && prop.parameters?size>0)>	
	        	"parameters":[
								<#list prop.parameters as param><#rt>					
								{
						          "name":"${param.left}",
								  "type":"${param.right}"<#if (prop.isOptionalParameter(param.left)!="false")>,
								  "optional":${prop.isOptionalParameter(param.left)}
								}<#else>
								}</#if><#if param_has_next>,</#if> 
								</#list>
<#t>							 ]<#if prop.hints??>,</#if>

						</#if>
							<#if prop.hints??>
							<#list prop.hints as hint>${hint}<#if hint_has_next>,</#if>
				            </#list>
			            </#if>
	        }<#if prop_has_next>,</#if> 
	    </#list>
	},
	"api":
	{
	    <#list apis as api>
	        "${api.name}": {
	<#if (api.returnType != "void")>            "returns": "${api.returnType}"<#if (api.parameters?size>0 || api.hints??)>,
	</#if></#if><#if (api.parameters?size>0)>			"parameters":[
			<#list api.parameters as param><#rt>					{
                                                                 
 								"name":"${param.left}",
								"type":"${param.right}"<#if (api.isOptionalParameter(param.left)!="false")>,
			            		"optional":${api.isOptionalParameter(param.left)}
			            		}<#else>
			                	}</#if><#if param_has_next>,
</#if>             </#list>
<#t>							 ]<#if api.hints??>,</#if>

	</#if><#if api.hints??><#list api.hints as hint>			${hint}<#if hint_has_next>,</#if>
			            </#list></#if>

	        }<#if api_has_next>,</#if>
	    </#list>
	}<#if types??>${types}</#if>
	 
}