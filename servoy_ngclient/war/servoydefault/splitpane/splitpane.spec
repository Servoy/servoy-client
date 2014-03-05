name: 'svy-splitpane',
displayName: 'Split Pane',
definition: 'servoydefault/splitpane/splitpane.js',
model:
{
        horizontalAlignment : {type:'int', values:[{DEFAULT:-1}, {LEFT:0}, {CENTER:2},{RIGHT:4}]}, 
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
            parameters:[{'vargs':'object []'}]
        }, 
        getAbsoluteFormLocationY:{
            returns: 'int',
                 }, 
        getClientProperty:{
            returns: 'object',
            parameters:[{'key':'object'}]
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
        getLeftForm:{
            returns: 'formscope',
                 }, 
        getLocationX:{
            returns: 'int',
                 }, 
        getLocationY:{
            returns: 'int',
                 }, 
        getMaxTabIndex:{
            returns: 'int',
                 }, 
        getMnemonicAt:{
            returns: 'string',
            parameters:[{'i':'int'}]
        }, 
        getName:{
            returns: 'string',
                 }, 
        getRightForm:{
            returns: 'formscope',
                 }, 
        getTabFGColorAt:{
            returns: 'string',
            parameters:[{'i':'int'}]
        }, 
        getTabFormNameAt:{
            returns: 'string',
            parameters:[{'i':'int'}]
        }, 
        getTabNameAt:{
            returns: 'string',
            parameters:[{'i':'int'}]
        }, 
        getTabRelationNameAt:{
            returns: 'string',
            parameters:[{'i':'int'}]
        }, 
        getTabTextAt:{
            returns: 'string',
            parameters:[{'i':'int'}]
        }, 
        getWidth:{
            returns: 'int',
                 }, 
        isTabEnabledAt:{
            returns: 'boolean',
            parameters:[{'i':'int'}]
        }, 
        putClientProperty:{
            
            parameters:[{'key':'object'},{'value':'object'}]
        }, 
        removeAllTabs:{
            returns: 'boolean',
                 }, 
        removeTabAt:{
            returns: 'boolean',
            parameters:[{'i':'int'}]
        }, 
        setLeftForm:{
            returns: 'boolean',
            parameters:[{'form':'object'},{'relation':'object','optional':'true'}]
        }, 
        setLocation:{
            
            parameters:[{'x':'int'},{'y':'int'}]
        }, 
        setMnemonicAt:{
            
            parameters:[{'index':'int'},{'text':'string'}]
        }, 
        setRightForm:{
            returns: 'boolean',
            parameters:[{'form':'object'},{'relation':'object','optional':'true'}]
        }, 
        setSize:{
            
            parameters:[{'width':'int'},{'height':'int'}]
        }, 
        setTabEnabledAt:{
            
            parameters:[{'i':'int'},{'b':'boolean'}]
        }, 
        setTabFGColorAt:{
            
            parameters:[{'i':'int'},{'clr':'string'}]
        }, 
        setTabTextAt:{
            
            parameters:[{'i':'int'},{'text':'string'}]
        } 
}