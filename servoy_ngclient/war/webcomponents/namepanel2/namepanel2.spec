name: 'namepanel2',
displayName: 'Name panel',
definition: 'webcomponents/namepanel2/namepanel2.js',
model:
{
	bgcolor: 'color',
	buttontext: 'string',
	tooltiptext: 'string',
	complexmodel: 'complextype'
},
handlers:
{
    onAction: 'function',
},
types: {
  complextype: {
  	model: {
  		firstNameDataprovider: { 'type':'dataprovider', 'ondatachange': { 'onchange':'onAction', 'callback':'onAction'}},
  		lastNameDataprovider: 'tagstring',
  	}
  }
}
 

