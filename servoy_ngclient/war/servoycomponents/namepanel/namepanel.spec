name: 'namepanel',
displayName: 'Name panel',
definition: 'servoycomponents/namepanel/namepanel.js',
model:
{
	bgcolor: 'color',
    firstNameDataprovider: 'dataprovider',
    firstNameTabsequence: 'tabseq',
    lastNameDataprovider: 'dataprovider',
    lastNameTabsequence: 'tabseq',
	buttontext: 'string',
	buttonClass : { type:'styleclass', values:['btn','btn-default','btn-lg','btn-sm','btn-xs']}, 
	tooltiptext: 'string',
	readOnly: 'boolean',
    firstNameFormat: {for:'firstNameDataprovider' , type:'format'}
},
handlers:
{
    onAction: 'function',
}

