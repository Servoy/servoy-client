name: 'svy-portal',
displayName: 'Portal',
definition: 'servoydefault/portal/portal.js',
model:
{
        resizable : 'boolean', 
        enabled : 'boolean', 
        visible : 'boolean', 
        tabSeq : 'tabseq', 
        styleClass : 'string', 
        rowBGColorCalculation : 'string', 
        transparent : 'boolean', 
        scrollbars : 'int', 
        initialSort : 'string', 
        printable : 'boolean', 
        intercellSpacing : 'dimension', 
        showHorizontalLines : 'boolean', 
        showVerticalLines : 'boolean', 
        rowHeight : 'int', 
        multiLine : 'boolean', 
        resizeble : 'boolean', 
        reorderable : 'boolean', 
        relationName : 'string', 
        size : 'dimension', 
        location : 'point', 
        borderType : 'border', 
        background : 'color', 
        foreground : 'color', 
        sortable : 'boolean' 
},
handlers:
{
        onRenderMethodID : 'function', 
        onDragEndMethodID : 'function', 
        onDropMethodID : 'function', 
        onDragOverMethodID : 'function', 
        onDragMethodID : 'function' 
},
api:
{
        deleteRecord:{
            returns: 'void',
            parameters:[]
        }, 
        duplicateRecord:{
            returns: 'void',
            parameters:[ {'addOnTop':'boolean','optional':'true'}]
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
        getMaxRecordIndex:{
            returns: 'int',
            parameters:[]
        }, 
        getName:{
            returns: 'string',
            parameters:[]
        }, 
        getScrollX:{
            returns: 'int',
            parameters:[]
        }, 
        getScrollY:{
            returns: 'int',
            parameters:[]
        }, 
        getSelectedIndex:{
            returns: 'int',
            parameters:[]
        }, 
        getSortColumns:{
            returns: 'string',
            parameters:[]
        }, 
        getWidth:{
            returns: 'int',
            parameters:[]
        }, 
        newRecord:{
            returns: 'void',
            parameters:[ {'addOnTop':'boolean','optional':'true'}]
        }, 
        putClientProperty:{
            returns: 'void',
            parameters:[ {'key':'object','optional':'false'}, {'value':'object','optional':'false'}]
        }, 
        setLocation:{
            returns: 'void',
            parameters:[ {'x':'int','optional':'false'}, {'y':'int','optional':'false'}]
        }, 
        setScroll:{
            returns: 'void',
            parameters:[ {'x':'int','optional':'false'}, {'y':'int','optional':'false'}]
        }, 
        setSelectedIndex:{
            returns: 'void',
            parameters:[ {'index':'int','optional':'false'}]
        }, 
        setSize:{
            returns: 'void',
            parameters:[ {'width':'int','optional':'false'}, {'height':'int','optional':'false'}]
        } 
}