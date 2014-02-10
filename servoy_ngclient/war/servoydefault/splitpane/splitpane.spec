name: 'svy-splitpane',
displayName: 'Split Pane',
definition: 'servoydefault/splitpane/splitpane.js',
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
        addTab:{
            returns: 'boolean',
            parameters:[ {'form/formname':'object []','optional':'false'}, {'name':'object','optional':'true'}, {'tabText':'object','optional':'true'}, {'tooltip':'object','optional':'true'}, {'iconURL':'object','optional':'true'}, {'fg':'object','optional':'true'}, {'bg':'object','optional':'true'}, {'relatedfoundset/relationname':'object','optional':'true'}, {'index':'object','optional':'true'}]
        }, 
        getAbsoluteFormLocationY:{
            returns: 'int',
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
            returns: 'int',
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
        getMaxTabIndex:{
            returns: 'int',
            parameters:[]
        }, 
        getMnemonicAt:{
            returns: 'string',
            parameters:[ {'i':'int','optional':'false'}]
        }, 
        getName:{
            returns: 'string',
            parameters:[]
        }, 
        getSelectedTabFormName:{
            returns: 'string',
            parameters:[]
        }, 
        getTabBGColorAt:{
            returns: 'string',
            parameters:[ {'unnamed_0':'int','optional':'false'}]
        }, 
        getTabFGColorAt:{
            returns: 'string',
            parameters:[ {'i':'int','optional':'false'}]
        }, 
        getTabFormNameAt:{
            returns: 'string',
            parameters:[ {'i':'int','optional':'false'}]
        }, 
        getTabNameAt:{
            returns: 'string',
            parameters:[ {'i':'int','optional':'false'}]
        }, 
        getTabRelationNameAt:{
            returns: 'string',
            parameters:[ {'i':'int','optional':'false'}]
        }, 
        getTabTextAt:{
            returns: 'string',
            parameters:[ {'i':'int','optional':'false'}]
        }, 
        getWidth:{
            returns: 'int',
            parameters:[]
        }, 
        isTabEnabled:{
            returns: 'boolean',
            parameters:[ {'unnamed_0':'int','optional':'false'}]
        }, 
        isTabEnabledAt:{
            returns: 'boolean',
            parameters:[ {'i':'int','optional':'false'}]
        }, 
        putClientProperty:{
            returns: 'void',
            parameters:[ {'key':'object','optional':'false'}, {'value':'object','optional':'false'}]
        }, 
        removeAllTabs:{
            returns: 'boolean',
            parameters:[]
        }, 
        removeTabAt:{
            returns: 'boolean',
            parameters:[ {'index':'int','optional':'false'}]
        }, 
        setLocation:{
            returns: 'void',
            parameters:[ {'x':'int','optional':'false'}, {'y':'int','optional':'false'}]
        }, 
        setMnemonicAt:{
            returns: 'void',
            parameters:[ {'index':'int','optional':'false'}, {'text':'string','optional':'false'}]
        }, 
        setSize:{
            returns: 'void',
            parameters:[ {'width':'int','optional':'false'}, {'height':'int','optional':'false'}]
        }, 
        setTabBGColorAt:{
            returns: 'void',
            parameters:[ {'unnamed_0':'int','optional':'false'}, {'unnamed_1':'string','optional':'false'}]
        }, 
        setTabEnabled:{
            returns: 'void',
            parameters:[ {'unnamed_0':'int','optional':'false'}, {'unnamed_1':'boolean','optional':'false'}]
        }, 
        setTabEnabledAt:{
            returns: 'void',
            parameters:[ {'i':'int','optional':'false'}, {'b':'boolean','optional':'false'}]
        }, 
        setTabFGColorAt:{
            returns: 'void',
            parameters:[ {'i':'int','optional':'false'}, {'s':'string','optional':'false'}]
        }, 
        setTabTextAt:{
            returns: 'void',
            parameters:[ {'index':'int','optional':'false'}, {'text':'string','optional':'false'}]
        } 
}