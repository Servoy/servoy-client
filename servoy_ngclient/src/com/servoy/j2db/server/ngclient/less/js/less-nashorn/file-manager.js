var Paths = Java.type('java.nio.file.Paths'),
    Files = Java.type('java.nio.file.Files'),
    PromiseConstructor,
    AbstractFileManager = require("../less/environment/abstract-file-manager");

try {
    PromiseConstructor = typeof Promise === 'undefined' ? require('promise') : Promise;
} catch(e) {
}

var FileManager = function() {
};

FileManager.prototype = new AbstractFileManager();

FileManager.prototype.supports = function(filename, currentDirectory, options, environment) {
    return true;
};
FileManager.prototype.supportsSync = function(filename, currentDirectory, options, environment) {
    return true;
};

FileManager.prototype.loadFile = function(filename, currentDirectory, options, environment, callback) {
    var data;

    options = options || {};

    data = this.loadFileSync(filename, currentDirectory, options, environment, 'utf-8');
    callback(data.error, data);
};

FileManager.prototype.loadFileSync = function(filename, currentDirectory, options, environment, encoding) {
	//console.log("dir="+currentDirectory);
	//console.log("filename="+filename);
	//console.log(JSON.stringify(options));
	fullFilename = currentDirectory + filename;

	data = lessc4j.readLess(fullFilename, encoding?encoding:null);
	if (!data) {
		if (options && options.paths) {
			for (path in options.paths) {
				fullFilename = currentDirectory + filename;
				data = lessc4j.readLess(fullFilename, encoding);
				if (data) {
					break;
				}
			}
		}
		if (!data) {
			err = { type: 'File', message: "'" + fullFilename + "' failed reading"};
			return {error : err}
		}
	}
    return {contents: data, filename: fullFilename};
};

module.exports = FileManager;
