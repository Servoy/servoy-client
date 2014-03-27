name: 'svy-navigator',
displayName: 'Servoy default navigator ',
definition: 'servoydefault/navigator/navigator.js',
libraries: ['servoydefault/navigator/js/jquery-ui.slider.min.js','servoydefault/navigator/ui-slider/slider.js', 'servoydefault/navigator/css/jquery-ui.slider.min.css', 'servoydefault/navigator/css/navigator.css'],
model:
{
    currentIndex: 'long',
    maxIndex: 'long',
},
handlers:
{
    setSelectedIndex: 'function'
}
