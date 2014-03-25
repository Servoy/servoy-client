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
name: 'svy-${name}',
displayName: '${displayName}',
definition: 'servoydefault/${name}/${name}.js',
libraries: [<#list libraries as lib>'${lib}'<#if lib_has_next>,</#if></#list>],
model:
{
    <#list model as prop>
        ${prop.name} : ${getPropTypeWithDefault(prop)}<#if prop_has_next>,</#if> 
    </#list>
},
handlers:
{
    <#list handlers as prop>
        ${prop.name} : 'function'<#if prop_has_next>,</#if> 
    </#list>
},
api:
{
    <#list apis as api>
        ${api.name}:{
            <#if (api.returnType != 'void')>returns: '${api.returnType}',</#if>
         <#if (api.parameters?size>0)>   parameters:[<#list api.parameters as param><#rt>
           {'${param.left}':'${param.right}'<#t>
            <#if (api.isOpitionalParameter(param.left)!='false')>,'optional':'${api.isOpitionalParameter(param.left)}'}<#else>}</#if><#t>
            <#if param_has_next>,</#if></#list>]<#lt>
         </#if>
        }<#if api_has_next>,</#if> 
    </#list>
}<#if types??>${types}</#if>
 
