name: 'svy-password',
displayName: 'Password field',
definition: 'servoydefault/password/password.js',
model:
{
        placeholderText : 'tagstring', 
        enabled : 'boolean', 
        visible : 'boolean', 
        styleClass : 'string', 
        text : 'tagstring', 
        margin : 'dimension', 
        printable : 'boolean', 
        valuelistID : { type: 'valuelist', for: 'dataProviderID'}, 
        verticalAlignment : {type:'int', values:[{DEFAULT:-1}, {TOP:1}, {CENTER:2} ,{BOTTOM:3}]}, 
        horizontalAlignment : {type:'int', values:[{DEFAULT:-1}, {LEFT:0}, {CENTER:2},{RIGHT:4}]}, 
        transparent : 'boolean', 
        tabSeq : 'tabseq', 
        selectOnEnter : 'boolean', 
        scrollbars : 'int', 
        location : 'point', 
        size : 'dimension', 
        useRTF : 'boolean', 
        format : {for:'dataProviderID' , type:'format'}, 
        fontType : 'font', 
        editable : 'boolean', 
        borderType : 'border', 
        dataProviderID : { 'type':'dataprovider', 'ondatachange': { 'onchange':'onDataChangeMethodID', 'callback':'onDataChangeCallback'}}, 
        toolTipText : 'tagstring', 
        foreground : 'color', 
        background : 'color' 
},
handlers:
{
        onRenderMethodID : 'function', 
        onRightClickMethodID : 'function', 
        onDataChangeMethodID : 'function', 
        onFocusLostMethodID : 'function', 
        onFocusGainedMethodID : 'function', 
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
        getLabelForElementNames:{
            returns: 'string []',
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
        getWidth:{
            returns: 'int',
                 }, 
        putClientProperty:{
            
            parameters:[{'key':'object'},{'value':'object'}]
        }, 
        requestFocus:{
            
            parameters:[{'mustExecuteOnFocusGainedMethod':'boolean','optional':'true'}]
        }, 
        setLocation:{
            
            parameters:[{'x':'int'},{'y':'int'}]
        }, 
        setSize:{
            
            parameters:[{'width':'int'},{'height':'int'}]
        } 
}