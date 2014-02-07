name: 'svy-portal',
displayName: 'Portal',
definition: 'servoydefault/portal/portal.js',
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
        verticalAlignment : {type:'number', values:[{DEFAULT:-1}, {TOP:1}, {CENTER:2} ,{BOTTOM:3}]}, 
        horizontalAlignment : {type:'number', values:[{DEFAULT:-1}, {LEFT:0}, {CENTER:2},{RIGHT:4}]}, 
        transparent : 'boolean', 
        tabSeq : 'tabseq', 
        selectOnEnter : 'boolean', 
        scrollbars : 'number', 
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
        deleteRecord:{
            returns: 'void',
            parameters:[]
        }, 
        duplicateRecord:{
            returns: 'void',
            parameters:[ {'addOnTop':'boolean','optional':'true'}]
        }, 
        getAbsoluteFormLocationY:{
            returns: 'number',
            parameters:[]
        }, 
        getClientProperty:{
            returns: 'object',
            parameters:[ {'key':'object','optional':'false'}]
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
        getMaxRecordIndex:{
            returns: 'number',
            parameters:[]
        }, 
        getName:{
            returns: 'string',
            parameters:[]
        }, 
        getScrollX:{
            returns: 'number',
            parameters:[]
        }, 
        getScrollY:{
            returns: 'number',
            parameters:[]
        }, 
        getSelectedIndex:{
            returns: 'number',
            parameters:[]
        }, 
        getSortColumns:{
            returns: 'string',
            parameters:[]
        }, 
        getWidth:{
            returns: 'number',
            parameters:[]
        }, 
        newRecord:{
            returns: 'void',
            parameters:[ {'addOnTop':'boolean','optional':'true'}]
        }, 
        putClientProperty:{
            returns: 'void',
            parameters:[ {'key':'object','optional':'false'}, {'value':'object','optional':'false'}]
        }, 
        setLocation:{
            returns: 'void',
            parameters:[ {'x':'number','optional':'false'}, {'y':'number','optional':'false'}]
        }, 
        setScroll:{
            returns: 'void',
            parameters:[ {'x':'number','optional':'false'}, {'y':'number','optional':'false'}]
        }, 
        setSelectedIndex:{
            returns: 'void',
            parameters:[ {'index':'number','optional':'false'}]
        }, 
        setSize:{
            returns: 'void',
            parameters:[ {'width':'number','optional':'false'}, {'height':'number','optional':'false'}]
        } 
}