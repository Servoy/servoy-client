name: 'svy-combobox',
displayName: 'Combobox ',
definition: 'servoydefault/combobox/combobox.js',
libraries: ['servoydefault/combobox/lib/select2-3.4.5/select2.js','servoydefault/combobox/lib/select2-3.4.5/select2.css','servoydefault/combobox/svy_select2.css'],
model:
{
        background : 'color', 
        borderType : 'border', 
        dataProviderID : { 'type':'dataprovider', 'ondatachange': { 'onchange':'onDataChangeMethodID', 'callback':'onDataChangeCallback'}}, 
        editable : {type:'boolean', default:true}, 
        enabled : {type:'boolean', default:true}, 
        fontType : 'font', 
        foreground : 'color', 
        format : {for:'dataProviderID' , type:'format'}, 
        horizontalAlignment : {type:'int', values:[{LEFT:2}, {CENTER:0},{RIGHT:4}],default: 2}, 
        location : 'point', 
        margin : 'dimension', 
        placeholderText : 'tagstring', 
        scrollbars : 'int', 
        size : {type:'dimension',  default: {width:140, height:20}}, 
        styleClass : { type:'styleclass', values:['form-control', 'input-sm', 'svy-padding-xs', 'select2-container-svy-xs']}, 
        tabSeq : 'tabseq', 
        text : 'tagstring', 
        toolTipText : 'tagstring', 
        transparent : 'boolean', 
        valuelistID : { type: 'valuelist', for: 'dataProviderID'}, 
        visible : {type:'boolean', default:true} 
},
handlers:
{
        onActionMethodID : 'function', 
        onDataChangeMethodID : 'function', 
        onFocusGainedMethodID : 'function', 
        onFocusLostMethodID : 'function', 
        onRenderMethodID : 'function', 
        onRightClickMethodID : 'function' 
},
api:
{
        requestFocus: {
            parameters:[{'mustExecuteOnFocusGainedMethod':'boolean','optional':'true'}]
        },
        setValueListItems: {
            parameters:[{'value':'object'}],
            callOn: 1
        }
}
 
