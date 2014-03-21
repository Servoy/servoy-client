name: 'svy-htmlarea',
displayName: 'Html Area',
definition: 'servoydefault/htmlarea/htmlarea.js',
libraries:["servoydefault/htmlarea/lib/tinymce/tinymce.min.js","servoydefault/htmlarea/lib/ui-tinymce.js"],
model:
{
        background : 'color', 
        borderType : 'border', 
        dataProviderID : { 'type':'dataprovider', 'ondatachange': { 'onchange':'onDataChangeMethodID', 'callback':'onDataChangeCallback'}}, 
        editable : 'boolean', 
        enabled : 'boolean', 
        fontType : 'font', 
        foreground : 'color', 
        format : {for:'dataProviderID' , type:'format'}, 
        horizontalAlignment : {type:'int', values:[{DEFAULT:-1}, {LEFT:0}, {CENTER:2},{RIGHT:4}]}, 
        location : 'point', 
        margin : 'dimension', 
        placeholderText : 'tagstring', 
        scrollbars : 'int', 
        selectOnEnter : 'boolean', 
        size : 'dimension', 
        styleClass : 'string', 
        tabSeq : 'tabseq', 
        text : 'tagstring', 
        toolTipText : 'tagstring', 
        transparent : 'boolean', 
        valuelistID : { type: 'valuelist', for: 'dataProviderID'}, 
        verticalAlignment : {type:'int', values:[{DEFAULT:-1}, {TOP:1}, {CENTER:2} ,{BOTTOM:3}]}, 
        visible : 'boolean' 
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
        getAbsoluteFormLocationY:{
            returns: 'int',
                 }, 
        getAsPlainText:{
            returns: 'string',
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
        getScrollX:{
            returns: 'int',
                 }, 
        getScrollY:{
            returns: 'int',
                 }, 
        getSelectedText:{
            returns: 'string',
                 }, 
        getWidth:{
            returns: 'int',
                 }, 
        putClientProperty:{
            
            parameters:[{'key':'object'},{'value':'object'}]
        }, 
        replaceSelectedText:{
            
            parameters:[{'s':'string'}]
        }, 
        requestFocus:{
            
            parameters:[{'mustExecuteOnFocusGainedMethod':'boolean','optional':'true'}]
        }, 
        selectAll:{
            
                 }, 
        setLocation:{
            
            parameters:[{'x':'int'},{'y':'int'}]
        }, 
        setScroll:{
            
            parameters:[{'x':'int'},{'y':'int'}]
        }, 
        setSize:{
            
            parameters:[{'width':'int'},{'height':'int'}]
        } 
}