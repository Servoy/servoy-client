name: 'svy-label',
displayName: 'label',
definition: 'servoydefault/label/label.js',
model:
{
        textRotation : {type:'int', values:[0,90,180,270]}, 
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
        horizontalAlignment : {type:'int', values:[{DEFAULT:-1}, {LEFT:0}, {CENTER:2},{RIGHT:4}]}, 
        verticalAlignment : {type:'int', values:[{DEFAULT:-1}, {TOP:1}, {CENTER:2} ,{BOTTOM:3}]}, 
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
                 }, 
        getClientProperty:{
            returns: 'object',
            parameters:[{'key':'object'}]
        }, 
        getDataProviderID:{
            returns: 'string',
                 }, 
        getDesignTimeProperty:{
            returns: 'object',
            parameters:[{'unnamed_0':'string'}]
        }, 
        getElementType:{
            returns: 'string',
                 }, 
        getHeight:{
            returns: 'int',
                 }, 
        getLabelForElementName:{
            returns: 'string',
                 }, 
        getLocationX:{
            returns: 'int',
                 }, 
        getLocationY:{
            returns: 'int',
                 }, 
        getName:{
            returns: 'string',
                 }, 
        getParameterValue:{
            returns: 'string',
            parameters:[{'param':'string'}]
        }, 
        getThumbnailJPGImage:{
            returns: 'byte []',
            parameters:[{'width':'int','optional':'true'},{'height':'int','optional':'true'}]
        }, 
        getWidth:{
            returns: 'int',
                 }, 
        putClientProperty:{
            
            parameters:[{'key':'object'},{'value':'object'}]
        }, 
        setLocation:{
            
            parameters:[{'x':'int'},{'y':'int'}]
        }, 
        setSize:{
            
            parameters:[{'width':'int'},{'height':'int'}]
        } 
}