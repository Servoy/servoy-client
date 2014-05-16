name: 'svy-htmlview',
displayName: 'Html View',
definition: 'servoydefault/htmlview/htmlview.js',
libraries: [],
model:
{
        background : 'color', 
        borderType : 'border', 
        dataProviderID : { 'type':'dataprovider', 'ondatachange': { 'onchange':'onDataChangeMethodID', 'callback':'onDataChangeCallback', 'parsehtml':true }}, 
        enabled : {type:'boolean', default:true}, 
        fontType : 'font', 
        foreground : 'color', 
        horizontalAlignment : {type:'int', values:[{LEFT:2}, {CENTER:0},{RIGHT:4}],default: 2}, 
        location : 'point', 
        margin : 'dimension', 
        scrollbars : 'int', 
        size : {type:'dimension',  default: {width:140, height:140}}, 
        styleClass : { type:'styleclass', values:[]}, 
        tabSeq : 'tabseq', 
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
            returns: 'string',
            callOn: 1
        },
        getScrollX: {
            returns: 'int',
            callOn: 1
        },
        getScrollY: {
            returns: 'int',
            callOn: 1
        },
        getSelectedText: {
            returns: 'string',
            callOn: 1
        },
        replaceSelectedText: {
            parameters:[{'s':'string'}],
            callOn: 1
        },
        requestFocus: {
            parameters:[{'mustExecuteOnFocusGainedMethod':'boolean','optional':'true'}],
            callOn: 1
        },
        selectAll: {

        },
        setScroll: {
            parameters:[{'x':'int'},{'y':'int'}],
            callOn: 1
        }
}
 
