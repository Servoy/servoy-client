if (typeof require === 'undefined') {
    var loadedModules = {};
    var requireWrap = (
        function (__dir, __file, args) {
            return function (path) {
            	//java.lang.System.out.println("dir=" + __dir + "__file=" + __file + ",path=" + path);
                var fullPath;
                if (path === "promise")
                    fullPath = '/less-nashorn/promise-nashorn.js';
                else if (path === "fs")
                    fullPath = '/less-nashorn/fs.js';
                else if (path === "os")
                    fullPath = '/less-nashorn/os.js';
                else if (path === "path")
                    fullPath = '/less-nashorn/path.js';
                else if (path === "process")
                    fullPath = '/less-nashorn/process.js';
                else if (path === "console")
                    fullPath = '/less-nashorn/console.js';
                else if (__dir === '/less-nashorn/' && path === "./environment")
                    fullPath = '/less-nashorn/environment.js';
                else {
                	if (path.startsWith("./")) {
                		fullPath = __dir + path.substring(2);
                	}
                	else {
                        fullPath = new java.net.URI(__dir + path).normalize().toString();
                	}
                    if (fullPath.endsWith("/data") 
                    		|| (fullPath.endsWith("/environment") && !fullPath.endsWith("/environment/environment")) 
                    		|| fullPath.endsWith("/less") 
                    		|| fullPath.endsWith("/functions") 
                    		|| (fullPath.endsWith("/parser") && !fullPath.endsWith("/parser/parser"))
                    		|| fullPath.endsWith("/plugins")
                    		|| fullPath.endsWith("/tree")
                    		|| fullPath.endsWith("/visitors") 
                    		|| fullPath.endsWith("/less-nashorn")) {
                    	fullPath += "/index.js";
                    } else if (!fullPath.endsWith(".js")) {
                    	fullPath += ".js";
                    }
                }

                var loadedModule = loadedModules[fullPath.toString()];
                if (loadedModule) {
                    return loadedModule;
                }
                var scope = {
                    require: null,
                    module: {exports: {}},
                    exports: null,
                    __dir: fullPath.substring(0, fullPath.lastIndexOf('/')+1),
                    __file: fullPath,
                    arguments: args
                };
                scope.require = requireWrap(scope.__dir, scope.__file, scope.arguments);
                scope.exports = scope.module.exports;
				//java.lang.System.out.println('Path: ' + fullPath);
                var moduleCode = lessc4j.readJs(fullPath);
                try {
                    var call = eval("//# sourceURL="+fullPath+"\nfunction(require, module, exports, __dir, __file, arguments) { "+moduleCode+" }")
                    call.apply(scope, [scope.require, scope.module, scope.exports, scope.__dir, scope.__file, scope.arguments]);
                }
                catch (e) {
                    if (e instanceof java.lang.Throwable)
                        throw new Error(fullPath + (e.lineNumber ? " on line " + (e.lineNumber - 1):"") + " : " + e.toString());
                    throw new Error(fullPath + " on line " + (e.lineNumber - 1) + " : " + e.message);
                }
                loadedModules[fullPath.toString()] = scope.module.exports;
                return scope.module.exports;
            };
        });
    require = requireWrap(__dir, __file, arguments);
}