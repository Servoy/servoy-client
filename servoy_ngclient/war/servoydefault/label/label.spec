name: 'svy-label',
displayName: 'label',
definition: 'servoydefault/label/label.js',
model:
{
        textRotation : {type:'number', values:[0,90,180,270]}, 
        format : {for:'dataProviderID' , type:'format'}, 
        enabled : 'boolean', 
        visible : 'boolean', 
        mnemonic : 'string', 
        labelFor : 'bean', 
        showFocus : 'boolean', 
        showClick : 'boolean', 
        styleClass : 'string', 
        rolloverCursor : 'int', 
        tabSeq : 'tabseq', 
        mediaOptions : 'mediaoptions', 
        margin : 'dimension', 
        printable : 'boolean', 
        rolloverImageMediaID : 'media', 
        imageMediaID : 'media', 
        transparent : 'boolean', 
        borderType : 'border', 
        horizontalAlignment : {type:'number', values:[{DEFAULT:-1}, {LEFT:0}, {CENTER:2},{RIGHT:4}]}, 
        verticalAlignment : {type:'number', values:[{DEFAULT:-1}, {TOP:1}, {CENTER:2} ,{BOTTOM:3}]}, 
        text : 'tagstring', 
        location : 'point', 
        size : 'dimension', 
        fontType : 'font', 
        foreground : 'color', 
        background : 'color', 
        dataProviderID : { 'type':'dataprovider', 'ondatachange': { 'onchange':'onDataChangeMethodID', 'callback':'onDataChangeCallback'}}, 
        toolTipText : 'tagstring' 
},
handlers:
{
        onRenderMethodID : 'function', 
        onRightClickMethodID : 'function', 
        onDoubleClickMethodID : 'function', 
        onActionMethodID : 'function' 
},
api:
{
        getAbsoluteFormLocationY:{
            returns: 'int',
            parameters:[]
        }, 
        getClientProperty:{
            returns: 'object',
            parameters:[ {'key':'object','optional':'false'}]
        }, 
        getDataProviderID:{
            returns: 'string',
            parameters:[]
        }, 
        getDesignTimeProperty:{
            returns: 'object',
            parameters:[ {'unnamed_0':'string','optional':'false'}]
        }, 
        getElementType:{
            returns: 'string',
            parameters:[]
        }, 
        getHeight:{
            returns: 'int',
            parameters:[]
        }, 
        getLabelForElementName:{
            returns: 'string',
            parameters:[]
        }, 
        getLocationX:{
            returns: 'int',
            parameters:[]
        }, 
        getLocationY:{
            returns: 'int',
            parameters:[]
        }, 
        getName:{
            returns: 'string',
            parameters:[]
        }, 
        getParameterValue:{
            returns: 'string',
            parameters:[ {'param':'string','optional':'false'}]
        }, 
        getThumbnailJPGImage:{
            returns: 'byte []',
            parameters:[ {'width':'int','optional':'true'}, {'height':'int','optional':'true'}]
        }, 
        getWidth:{
            returns: 'int',
            parameters:[]
        }, 
        putClientProperty:{
            returns: 'void',
            parameters:[ {'key':'object','optional':'false'}, {'value':'object','optional':'false'}]
        }, 
        setLocation:{
            returns: 'void',
            parameters:[ {'x':'int','optional':'false'}, {'y':'int','optional':'false'}]
        }, 
        setSize:{
            returns: 'void',
            parameters:[ {'width':'int','optional':'false'}, {'height':'int','optional':'false'}]
        } 
}