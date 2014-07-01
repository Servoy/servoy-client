name: 'svy-splitpane',
displayName: 'Split Pane',
definition: 'servoydefault/splitpane/splitpane.js',
libraries: ['servoydefault/splitpane/bg-splitter/js/splitter.js','servoydefault/splitpane/bg-splitter/css/style.css'],
model:
{
        background : 'color', 
        borderType : 'border', 
        enabled : {type:'boolean', default:true}, 
        fontType : 'font', 
        foreground : 'color', 
        horizontalAlignment : {type:'int', scope:'design', values:[{LEFT:2}, {CENTER:0},{RIGHT:4}],default: 2}, 
        location : 'point', 
        readOnly : 'boolean', 
        selectedTabColor : 'color', 
        size : {type:'dimension',  default: {width:300, height:300}}, 
        styleClass : { type:'styleclass', scope:'design', values:[]}, 
        tabOrientation : {type:'int', scope:'design', values:[{DEFAULT:0}, {TOP:1}, {HIDE:-1}]}, 
        tabSeq : {type:'tabseq', scope:'design'}, 
        tabs : 'tab[]', 
        transparent : 'boolean', 
        visible : {type:'boolean', default:true} 
},
handlers:
{
        onChangeMethodID : 'function', 
        onTabChangeMethodID : 'function' 
},
api:
{
        addTab: {
            returns: 'boolean',
            parameters:[{'vargs':'object []'}]
        },
        getContinuousLayout: {
            returns: 'boolean'
        },
        getDividerLocation: {
            returns: 'double'
        },
        getDividerSize: {
            returns: 'int'
        },
        getLeftForm: {
            returns: 'formscope'
        },
        getLeftFormMinSize: {
            returns: 'int'
        },
        getMaxTabIndex: {
            returns: 'int'
        },
        getMnemonicAt: {
            returns: 'string',
            parameters:[{'i':'int'}]
        },
        getResizeWeight: {
            returns: 'double'
        },
        getRightForm: {
            returns: 'formscope'
        },
        getRightFormMinSize: {
            returns: 'int'
        },
        getTabFGColorAt: {
            returns: 'string',
            parameters:[{'i':'int'}]
        },
        getTabFormNameAt: {
            returns: 'string',
            parameters:[{'i':'int'}]
        },
        getTabNameAt: {
            returns: 'string',
            parameters:[{'i':'int'}]
        },
        getTabRelationNameAt: {
            returns: 'string',
            parameters:[{'i':'int'}]
        },
        getTabTextAt: {
            returns: 'string',
            parameters:[{'i':'int'}]
        },
        isTabEnabledAt: {
            returns: 'boolean',
            parameters:[{'i':'int'}]
        },
        removeAllTabs: {
            returns: 'boolean'
        },
        removeTabAt: {
            returns: 'boolean',
            parameters:[{'i':'int'}]
        },
        setContinuousLayout: {
            parameters:[{'b':'boolean'}]
        },
        setDividerLocation: {
            parameters:[{'location':'double'}]
        },
        setDividerSize: {
            parameters:[{'size':'int'}]
        },
        setLeftForm: {
            returns: 'boolean',
            parameters:[{'form':'object'},{'relation':'object','optional':'true'}]
        },
        setLeftFormMinSize: {
            parameters:[{'minSize':'int'}]
        },
        setMnemonicAt: {
            parameters:[{'index':'int'},{'text':'string'}]
        },
        setResizeWeight: {
            parameters:[{'resizeWeight':'double'}]
        },
        setRightForm: {
            returns: 'boolean',
            parameters:[{'form':'object'},{'relation':'object','optional':'true'}]
        },
        setRightFormMinSize: {
            parameters:[{'minSize':'int'}]
        },
        setTabEnabledAt: {
            parameters:[{'i':'int'},{'b':'boolean'}]
        },
        setTabFGColorAt: {
            parameters:[{'i':'int'},{'clr':'string'}]
        },
        setTabTextAt: {
            parameters:[{'i':'int'},{'text':'string'}]
        }
},
types: {
  tab: {
  	model: {
  		name: 'string',
  		containsFormId: 'form',
  		text: 'tagstring',
  		relationName: 'relation',
  		active: 'boolean',
  		foreground: 'color',
  		disabled: 'boolean',
  		mnemonic: 'string'
  	}
  }
}
 
