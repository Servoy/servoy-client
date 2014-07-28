name: 'svy-radio',
displayName: 'Radio',
definition: 'servoydefault/radio/radio.js',
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
        margin : {type:'dimension', scope:'design'}, 
        placeholderText : 'tagstring', 
        size : {type:'dimension',  default: {width:140, height:20}}, 
        styleClass : { type:'styleclass', scope:'design', values:['radio']}, 
        tabSeq : {type:'tabseq', scope:'design'}, 
        text : 'tagstring', 
        toolTipText : 'tagstring', 
        transparent : 'boolean', 
        valuelistID : { type: 'valuelist', scope:'design', for: 'dataProviderID'}, 
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
            parameters:[{'value':'dataset'}]
        }
}
 
