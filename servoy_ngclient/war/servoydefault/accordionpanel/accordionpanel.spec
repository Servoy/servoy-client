name: 'svy-accordionpanel',
displayName: 'AccordionPanel',
definition: 'servoydefault/accordionpanel/accordionpanel.js',
model:
{
        horizontalAlignment : {type:'number', values:[{DEFAULT:-1}, {LEFT:0}, {CENTER:2},{RIGHT:4}]}, 
        enabled : 'boolean', 
        visible : 'boolean', 
        tabSeq : 'tabseq', 
        closeOnTabs : 'boolean', 
        scrollTabs : 'boolean', 
        styleClass : 'string', 
        transparent : 'boolean', 
        selectedTabColor : 'color', 
        fontType : 'font', 
        printable : 'boolean', 
        borderType : 'border', 
        size : 'dimension', 
        tabOrientation : 'int', 
        location : 'point', 
        foreground : 'color', 
        background : 'color' 
},
handlers:
{
        onChangeMethodID : 'function', 
        onTabChangeMethodID : 'function' 
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