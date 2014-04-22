name: 'svy-portal',
displayName: 'Portal',
definition: 'servoydefault/portal/portal.js',
libraries: [],
model:
{
        background : 'color', 
        borderType : 'border', 
        enabled : {type:'boolean', default:true}, 
        foreground : 'color', 
        initialSort : 'string', 
        intercellSpacing : 'dimension', 
        location : 'point', 
        multiLine : 'boolean', 
        relationName : 'string', 
        reorderable : 'boolean', 
        resizable : 'boolean', 
        resizeble : 'boolean', 
        rowBGColorCalculation : 'string', 
        rowHeight : 'int', 
        scrollbars : 'int', 
        showHorizontalLines : 'boolean', 
        showVerticalLines : 'boolean', 
        size : 'dimension', 
        sortable : 'boolean', 
        styleClass : 'string', 
        tabSeq : 'tabseq', 
        transparent : 'boolean', 
        visible : {type:'boolean', default:true} 
},
handlers:
{
        onDragEndMethodID : 'function', 
        onDragMethodID : 'function', 
        onDragOverMethodID : 'function', 
        onDropMethodID : 'function', 
        onRenderMethodID : 'function' 
},
api:
{
        deleteRecord:{
            
                 }, 
        duplicateRecord:{
            
            parameters:[{'addOnTop':'boolean','optional':'true'}]
        }, 
        getMaxRecordIndex:{
            returns: 'int',
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
        newRecord:{
            
            parameters:[{'addOnTop':'boolean','optional':'true'}]
        }, 
        setScroll:{
            
            parameters:[{'x':'int'},{'y':'int'}]
        }, 
        setSelectedIndex:{
            
            parameters:[{'index':'int'}]
        } 
}
 
