name: 'svy-portal',
displayName: 'Portal',
categoryName: 'Elements',
icon: 'servoydefault/portal/portal.gif',
definition: 'servoydefault/portal/portal.js',
libraries: ['servoydefault/portal/portal.css'],
model:
{
        background : 'color', 
        borderType : 'border', 
        childElements : { type: 'component[]', forFoundsetTypedProperty: 'relatedFoundset' }, 
        enabled : {type:'boolean', default:true}, 
        foreground : 'color', 
        initialSort : 'string', 
        intercellSpacing : 'dimension', 
        location : 'point', 
        multiLine : 'boolean', 
        relatedFoundset : 'foundset', 
        reorderable : 'boolean', 
        resizable : 'boolean', 
        resizeble : 'boolean', 
        rowBGColorCalculation : 'string', 
        rowHeight : 'int', 
        scrollbars : {type:'int', scope:'design'}, 
        showHorizontalLines : 'boolean', 
        showVerticalLines : 'boolean', 
        size : {type:'dimension',  default: {width:200, height:200}}, 
        sortable : 'boolean', 
        styleClass : 'string', 
        tabSeq : {type:'tabseq', scope:'design'}, 
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
        deleteRecord: {

        },
        duplicateRecord: {
            parameters:[{'addOnTop':'boolean','optional':'true'}]
        },
        getMaxRecordIndex: {
            returns: 'int'
        },
        getScrollX: {
            returns: 'int'
        },
        getScrollY: {
            returns: 'int'
        },
        getSelectedIndex: {
            returns: 'int'
        },
        getSortColumns: {
            returns: 'string'
        },
        newRecord: {
            parameters:[{'addOnTop':'boolean','optional':'true'}]
        },
        setScroll: {
            parameters:[{'x':'int'},{'y':'int'}]
        },
        setSelectedIndex: {
            parameters:[{'index':'int'}]
        }
}
 
