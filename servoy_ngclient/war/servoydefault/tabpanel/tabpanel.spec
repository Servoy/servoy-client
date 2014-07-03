name: 'svy-tabpanel',
displayName: 'Tab panel',
definition: 'servoydefault/tabpanel/tabpanel.js',
libraries: ['servoydefault/tabpanel/accordionpanel.css'],
model:
{
        background : 'color', 
        borderType : 'border', 
        enabled : {type:'boolean', default:true}, 
        fontType : 'font', 
        foreground : 'color', 
        horizontalAlignment : {type:'int', scope:'design', values:[{LEFT:2}, {CENTER:0},{RIGHT:4}],default: -1}, 
        location : 'point', 
        readOnly : 'boolean', 
        selectedTabColor : 'color', 
        size : {type:'dimension',  default: {width:300, height:300}}, 
        styleClass : { type:'styleclass', scope:'design', values:[]}, 
        tabIndex : 'int', 
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
            parameters:[{'form/formname':'object []'},{'name':'object','optional':'true'},{'tabText':'object','optional':'true'},{'tooltip':'object','optional':'true'},{'iconURL':'object','optional':'true'},{'fg':'object','optional':'true'},{'bg':'object','optional':'true'},{'relatedfoundset/relationname':'object','optional':'true'},{'index':'object','optional':'true'}]
        },
        getMaxTabIndex: {
            returns: 'int'
        },
        getMnemonicAt: {
            returns: 'string',
            parameters:[{'i':'int'}]
        },
        getSelectedTabFormName: {
            returns: 'string'
        },
        getTabBGColorAt: {
            returns: 'string',
            parameters:[{'unnamed_0':'int'}]
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
        isTabEnabled: {
            returns: 'boolean',
            parameters:[{'unnamed_0':'int'}]
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
            parameters:[{'index':'int'}]
        },
        setMnemonicAt: {
            parameters:[{'index':'int'},{'text':'string'}]
        },
        setTabBGColorAt: {
            parameters:[{'unnamed_0':'int'},{'unnamed_1':'string'}]
        },
        setTabEnabled: {
            parameters:[{'unnamed_0':'int'},{'unnamed_1':'boolean'}]
        },
        setTabEnabledAt: {
            parameters:[{'i':'int'},{'b':'boolean'}]
        },
        setTabFGColorAt: {
            parameters:[{'i':'int'},{'s':'string'}]
        },
        setTabTextAt: {
            parameters:[{'index':'int'},{'text':'string'}]
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
  		imageMediaID: 'string',
  		mnemonic: 'string'
  	}
  }
}
 
