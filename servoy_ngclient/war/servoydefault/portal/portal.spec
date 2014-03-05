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
            
                 }, 
        duplicateRecord:{
            
            parameters:[{'addOnTop':'boolean','optional':'true'}]
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
        getLocationX:{
            returns: 'int',
                 }, 
        getLocationY:{
            returns: 'int',
                 }, 
        getMaxRecordIndex:{
            returns: 'int',
                 }, 
        getName:{
            returns: 'string',
                 }, 
        getScrollX:{
            returns: 'int',
                 }, 
        getScrollY:{
            returns: 'int',
                 }, 
        getSelectedIndex:{
            returns: 'int',
                 }, 
        getSortColumns:{
            returns: 'string',
                 }, 
        getWidth:{
            returns: 'int',
                 }, 
        newRecord:{
            
            parameters:[{'addOnTop':'boolean','optional':'true'}]
        }, 
        putClientProperty:{
            
            parameters:[{'key':'object'},{'value':'object'}]
        }, 
        setLocation:{
            
            parameters:[{'x':'int'},{'y':'int'}]
        }, 
        setScroll:{
            
            parameters:[{'x':'int'},{'y':'int'}]
        }, 
        setSelectedIndex:{
            
            parameters:[{'index':'int'}]
        }, 
        setSize:{
            
            parameters:[{'width':'int'},{'height':'int'}]
        } 
}