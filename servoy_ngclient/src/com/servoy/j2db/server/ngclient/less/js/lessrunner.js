var less = createFromEnvironment(lessenv);


function FileManager(fm) {
	less.AbstractFileManager.call(this);
	this.fm = fm;
}
FileManager.prototype = Object.create(less.AbstractFileManager.prototype);
FileManager.prototype.constructor = less.AbstractFileManager;

FileManager.prototype.supportsSync = function() {
    return true;
};

FileManager.prototype.supports = function() {
    return true;
};

FileManager.prototype.loadFile = function(path, currentDirectory, context, environment, func) {
	var contents = this.fm.load(path);
	func(null, {filename:path, contents:contents});
    return;
};

FileManager.prototype.loadFileSync = function(path, currentDirectory, context, environment) {
	var contents = this.fm.loadBytes(path);
	return  {filename:path, contents:contents};
};

var convert = function (lessfilecontents, fm) {    
	less.environment.addFileManager(new FileManager(fm));

	var result = null;
    less.render(lessfilecontents, function (e, output) {
    if (e) result = e;
	else result = output.css;
    });
    less.environment.clearFileManagers();
    return result;
}
