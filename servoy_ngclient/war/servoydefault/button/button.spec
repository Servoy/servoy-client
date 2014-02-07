name: 'svy-button',
displayName: 'Button',
definition: 'servoydefault/button/button.js',
model:
{
        textRotation : {type:'number', values:[0,90,180,270]}, 
        format : {for:'dataProviderID' , type:'format'}, 
        enabled : 'boolean', 
        visible : 'boolean', 
        mnemonic : 'string', 
        showFocus : 'boolean', 
        showClick : 'boolean', 
        styleClass : 'string', 
        rolloverCursor : 'number', 
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
            returns: 'number',
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
            returns: 'number',
            parameters:[]
        }, 
        getLocationX:{
            returns: 'number',
            parameters:[]
        }, 
        getLocationY:{
            returns: 'number',
            parameters:[]
        }, 
        getName:{
            returns: 'string',
            parameters:[]
        }, 
        getThumbnailJPGImage:{
            returns: 'byte []',
            parameters:[ {'width':'number','optional':'true'}, {'height':'number','optional':'true'}]
        }, 
        getWidth:{
            returns: 'number',
            parameters:[]
        }, 
        putClientProperty:{
            returns: 'void',
            parameters:[ {'key':'object','optional':'false'}, {'value':'object','optional':'false'}]
        }, 
        requestFocus:{
            returns: 'void',
            parameters:[ {'mustExecuteOnFocusGainedMethod':'boolean','optional':'true'}]
        }, 
        setLocation:{
            returns: 'void',
            parameters:[ {'x':'number','optional':'false'}, {'y':'number','optional':'false'}]
        }, 
        setSize:{
            returns: 'void',
            parameters:[ {'width':'number','optional':'false'}, {'height':'number','optional':'false'}]
        } 
}