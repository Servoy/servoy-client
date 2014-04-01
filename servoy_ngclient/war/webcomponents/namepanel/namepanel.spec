name: 'namepanel',
displayName: 'Name panel',
definition: 'webcomponents/namepanel/namepanel.js',
model:
{
	bgcolor: 'color',
    firstNameDataprovider: 'dataprovider',
    lastNameDataprovider: 'dataprovider',
	buttontext: 'string',
	tooltiptext: 'string',
	readOnly: 'boolean',
    firstNameFormat: {for:'firstNameDataprovider' , type:'format'}
},
handlers:
{
    onAction: 'function',
}

