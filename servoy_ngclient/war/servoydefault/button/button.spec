name: 'svy-button',
displayName: 'Button',
definition: 'servoydefault/button/button.js',
libraries: [],
model:
{
        background : 'color', 
        borderType : 'border', 
        dataProviderID : { 'type':'dataprovider', 'ondatachange': { 'onchange':'onDataChangeMethodID', 'callback':'onDataChangeCallback'}}, 
        enabled : {type:'boolean', default:true}, 
        fontType : 'font', 
        foreground : 'color', 
        format : {for:'dataProviderID' , type:'format'}, 
        horizontalAlignment : {type:'int', values:[{LEFT:2}, {CENTER:0},{RIGHT:4}], default: 0}, 
        imageMediaID : 'media', 
        location : 'point', 
        margin : 'dimension', 
        mediaOptions : 'mediaoptions', 
        mnemonic : 'string', 
        rolloverCursor : 'int', 
        rolloverImageMediaID : 'media', 
        size : {type:'dimension',  default: {width:80, height:20}}, 
        styleClass : { type:'styleclass', values:['btn','btn-default','btn-lg','btn-sm','btn-xs']}, 
        tabSeq : 'tabseq', 
        text : 'tagstring', 
        textRotation : {type:'int', values:[0,90,180,270]}, 
        toolTipText : 'tagstring', 
        transparent : 'boolean', 
        verticalAlignment : {type:'int', values:[{TOP:1}, {CENTER:0} ,{BOTTOM:3}], default: 0}, 
        visible : {type:'boolean', default:true} 
},
handlers:
{
        onActionMethodID : 'function', 
        onDoubleClickMethodID : 'function', 
        onRenderMethodID : 'function', 
        onRightClickMethodID : 'function' 
},
api:
{
        getThumbnailJPGImage: {
            returns: 'byte []',
            parameters:[{'width':'int','optional':'true'},{'height':'int','optional':'true'}]
        },
        requestFocus: {
            parameters:[{'mustExecuteOnFocusGainedMethod':'boolean','optional':'true'}],
            callOn: 1
        }
}
 
