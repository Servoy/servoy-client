name: 'svy-htmlview',
displayName: 'Html View',
categoryName: 'Elements',
definition: 'servoydefault/htmlview/htmlview.js',
libraries: [],
model:
{
        background : 'color', 
        borderType : 'border', 
        dataProviderID : { type:'dataprovider', 'ondatachange': { 'onchange':'onDataChangeMethodID', 'callback':'onDataChangeCallback', 'parsehtml':true }}, 
        enabled : {type:'boolean', default:true}, 
        fontType : 'font', 
        foreground : 'color', 
        horizontalAlignment : {type:'int', scope:'design', values:[{LEFT:2}, {CENTER:0},{RIGHT:4}],default: -1}, 
        location : 'point', 
        margin : {type:'insets', scope:'design'}, 
        scrollbars : {type:'int', scope:'design'}, 
        size : {type:'dimension',  default: {width:140, height:140}}, 
        styleClass : { type:'styleclass', scope:'design', values:[]}, 
        tabSeq : {type:'tabseq', scope:'design'}, 
        text : 'tagstring', 
        toolTipText : 'tagstring', 
        transparent : 'boolean', 
        visible : {type:'boolean', default:true} 
},
handlers:
{
        onActionMethodID : 'function', 
        onDataChangeMethodID : 'function', 
        onFocusGainedMethodID : 'function', 
        onFocusLostMethodID : 'function', 
        onRenderMethodID : 'function', 
        onRightClickMethodID : 'function' 
},
api:
{
        getAsPlainText: {
            returns: 'string'
        },
        getScrollX: {
            returns: 'int'
        },
        getScrollY: {
            returns: 'int'
        },
        getSelectedText: {
            returns: 'string'
        },
        replaceSelectedText: {
            parameters:[{'s':'string'}]
        },
        requestFocus: {
            parameters:[{'mustExecuteOnFocusGainedMethod':'boolean','optional':'true'}]
        },
        selectAll: {

        },
        setScroll: {
            parameters:[{'x':'int'},{'y':'int'}]
        }
}
 
