name: 'svy-label',
displayName: 'label',
definition: 'servoydefault/label/label.js',
model:
{
        background : 'color', 
        borderType : 'border', 
        dataProviderID : { 'type':'dataprovider', 'ondatachange': { 'onchange':'onDataChangeMethodID', 'callback':'onDataChangeCallback'}}, 
        enabled : 'boolean', 
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
        printable : 'boolean', 
        rolloverCursor : 'int', 
        rolloverImageMediaID : 'media', 
        showClick : 'boolean', 
        size : 'dimension', 
        styleClass : 'string', 
        tabSeq : 'tabseq', 
        text : 'tagstring', 
        textRotation : {type:'int', values:[0,90,180,270]}, 
        toolTipText : 'tagstring', 
        transparent : 'boolean', 
        verticalAlignment : {type:'int', values:[{DEFAULT:-1}, {TOP:1}, {CENTER:2} ,{BOTTOM:3}]}, 
        visible : 'boolean' 
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