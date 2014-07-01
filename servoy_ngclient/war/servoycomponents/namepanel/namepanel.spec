name: 'namepanel',
displayName: 'Name panel',
definition: 'servoycomponents/namepanel/namepanel.js',
model:
{
	bgcolor: 'color',
    firstNameDataprovider: 'dataprovider',
    firstNameTabsequence: {type: 'tabseq',scope: 'design'},
    lastNameDataprovider: 'dataprovider',
    lastNameTabsequence: {type: 'tabseq',scope: 'design'},
	buttontext: 'string',
	buttonClass : { type:'styleclass', values:['btn','btn-default','btn-lg','btn-sm','btn-xs']}, 
	tooltiptext: 'string',
	readOnly: 'boolean',
    firstNameFormat: {for:'firstNameDataprovider' , type:'format'},
    testruntime: { type: 'string', scope:'runtime'}
},
handlers:
{
    onAction: 'function',
}

