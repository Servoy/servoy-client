name: 'svy-password',
displayName: 'Password field',
definition: 'servoydefault/password/password.js',
libraries: [],
model:
{
        background : 'color', 
        borderType : 'border', 
        dataProviderID : { 'type':'dataprovider', scope:'design', 'ondatachange': { 'onchange':'onDataChangeMethodID', 'callback':'onDataChangeCallback'}}, 
        editable : {type:'boolean', default:true}, 
        enabled : {type:'boolean', default:true}, 
        fontType : 'font', 
        foreground : 'color', 
        horizontalAlignment : {type:'int', scope:'design', values:[{LEFT:2}, {CENTER:0},{RIGHT:4}],default: -1}, 
        location : 'point', 
        margin : {type:'insets', scope:'design'}, 
        placeholderText : 'tagstring', 
        selectOnEnter : {type:'boolean', scope:'design'}, 
        size : {type:'dimension',  default: {width:140, height:20}}, 
        styleClass : { type:'styleclass', scope:'design', values:['form-control', 'input-sm', 'svy-padding-xs']}, 
        tabSeq : {type:'tabseq', scope:'design'}, 
        text : 'tagstring', 
        toolTipText : 'tagstring', 
        transparent : 'boolean', 
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
 
