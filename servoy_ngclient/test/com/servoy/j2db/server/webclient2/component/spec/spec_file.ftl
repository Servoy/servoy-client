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
name: '${name}',
displayName: '${displayName}',
definition: 'servoydefault/${name}/${name}.js',
model:
{
    <#list model as prop>
        ${prop.name} : ${getPropType(prop)}<#if prop_has_next>,</#if> 
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
            returns: '${api.returnType}',
            parameters:[<#list api.parameters as param> {'${param.left}':'${param.right}','optional':'${api.isOpitionalParameter(param.left)}'}<#if param_has_next>,</#if></#list>]
        }<#if api_has_next>,</#if> 
    </#list>
}