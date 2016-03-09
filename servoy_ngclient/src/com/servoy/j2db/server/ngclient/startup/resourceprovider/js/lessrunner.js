
var less = createFromEnvironment(lessenv);

var convert = function (lessfilecontents) {    
    var result = null;
    less.render(lessfilecontents, function (e, output) {
	if (e) result = e;
	else result = output.css;
    });
    return result;
}
