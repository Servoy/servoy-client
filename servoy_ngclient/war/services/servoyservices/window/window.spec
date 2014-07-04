name: 'window',
displayName: 'Servoy Window plugin',
definition: 'services/servoyservices/window/window.js',
libraries: [],
model:
{
 	shortcuts : 'shortcut[]',
 	popupform: 'popupform' 
},
api:
{
 	 createShortcut: {
            returns: 'boolean',
            parameters:[{'shortcut':'string'},{'callback':'function'},{'contextFilter':'string','optional':'true'},{'arguments':'object []','optional':'true'}]
        },
     removeShortcut: {
            returns: 'boolean',
            parameters:[{'shortcut':'string'},{'contextFilter':'string','optional':'true'}]
        },
     showFormPopup: {
            parameters:[{'component':'component'},{'form':'form'},{'scope':'object'},{'dataProviderID':'string'},{'width':'int','optional':'true'},{'height':'int','optional':'true'}]
        },
     closeFormPopup: {
     		parameters:[{'retval':'object'}]
     	},
     cancelFormPopup: {
     	},
     createPopupMenu: {
            returns: 'popup',
        },
},
types: {
  shortcut: {
  	model: {
  		shortcut: 'string',
  		method: 'string',
  		contextFilter: 'string',
  		arguments: 'object[]',
  	}
  },
  popupform: {
  	model: {
  		visible: boolean,
  		componentLocation: 'string',
  		form: 'string',
  		scope: 'string',
  		dataProviderID: 'string',
  		width: 'int',
  		height: 'int'
  	}
  },
  checkbox: {
  	model: {
  	}
  },
  radiobutton: {
  	model: {
  	}
  },
   menuitem: {
  	model: {
  	}
  },
  menu: {
  	model: {
  	}
  },
  popup: {
  	model: {
  	}
  },
}

