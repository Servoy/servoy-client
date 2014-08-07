name: 'svy-htmlarea',
displayName: 'Html Area',
definition: 'servoydefault/htmlarea/htmlarea.js',
libraries: ['servoydefault/htmlarea/lib/tinymce/tinymce.min.js','servoydefault/htmlarea/lib/ui-tinymce.js'],
model:
{
        background : 'color', 
        borderType : 'border', 
        dataProviderID : { 'type':'dataprovider', scope:'design', 'ondatachange': { 'onchange':'onDataChangeMethodID', 'callback':'onDataChangeCallback'}}, 
        editable : {type:'boolean', default:true}, 
        enabled : {type:'boolean', default:true}, 
        fontType : 'font', 
        foreground : 'color', 
        format : {for:'dataProviderID' , type:'format'}, 
        horizontalAlignment : {type:'int', scope:'design', values:[{LEFT:2}, {CENTER:0},{RIGHT:4}],default: -1}, 
        location : 'point', 
        margin : {type:'insets', scope:'design'}, 
        placeholderText : 'tagstring', 
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
 
