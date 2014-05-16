name: 'svy-portal',
displayName: 'Portal',
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
        scrollbars : 'int', 
        showHorizontalLines : 'boolean', 
        showVerticalLines : 'boolean', 
        size : {type:'dimension',  default: {width:200, height:200}}, 
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
        deleteRecord: {

        },
        duplicateRecord: {
            parameters:[{'addOnTop':'boolean','optional':'true'}]
        },
        getMaxRecordIndex: {
            returns: 'int'
        },
        getScrollX: {
            returns: 'int',
            callOn: 1
        },
        getScrollY: {
            returns: 'int',
            callOn: 1
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
            parameters:[{'x':'int'},{'y':'int'}],
            callOn: 1
        },
        setSelectedIndex: {
            parameters:[{'index':'int'}]
        }
}
 
