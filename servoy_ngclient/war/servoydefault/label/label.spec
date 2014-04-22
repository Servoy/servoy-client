name: 'svy-label',
displayName: 'label',
definition: 'servoydefault/label/label.js',
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
        horizontalAlignment : {type:'int', values:[{DEFAULT:-1}, {LEFT:0}, {CENTER:2},{RIGHT:4}]}, 
        imageMediaID : 'media', 
        labelFor : 'bean', 
        location : 'point', 
        margin : 'dimension', 
        mediaOptions : 'mediaoptions', 
        mnemonic : 'string', 
        rolloverCursor : 'int', 
        rolloverImageMediaID : 'media', 
        size : 'dimension', 
        styleClass : { type:'styleclass', values:[]}, 
        tabSeq : 'tabseq', 
        text : 'tagstring', 
        textRotation : {type:'int', values:[0,90,180,270]}, 
        toolTipText : 'tagstring', 
        transparent : 'boolean', 
        verticalAlignment : {type:'int', values:[{DEFAULT:-1}, {TOP:1}, {CENTER:2} ,{BOTTOM:3}]}, 
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
        getLabelForElementName:{
            returns: 'string',
                 }, 
        getParameterValue:{
            returns: 'string',
            parameters:[{'param':'string'}]
        }, 
        getThumbnailJPGImage:{
            returns: 'byte []',
            parameters:[{'width':'int','optional':'true'},{'height':'int','optional':'true'}]
        } 
}
 
