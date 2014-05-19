name: 'svy-password',
displayName: 'Password field',
definition: 'servoydefault/password/password.js',
libraries: [],
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
        selectOnEnter : 'boolean', 
        size : {type:'dimension',  default: {width:140, height:20}}, 
        styleClass : { type:'styleclass', values:['form-control', 'input-sm', 'svy-padding-xs']}, 
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
        }
}
 
