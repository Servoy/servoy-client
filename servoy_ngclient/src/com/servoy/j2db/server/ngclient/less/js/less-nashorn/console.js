if (typeof console === 'undefined') {
    console = {
        log: function(msg) {
        	java.lang.System.err.println('console register: ' + msg);
        },
        info: function(msg) {
        	java.lang.System.err.println('console register: ' + msg);
        },
        warn: function(msg) {
        	java.lang.System.err.println('console register: ' + msg);
        },
        error: function(msg) {
            java.lang.System.err.println('ERROR: ' + msg);
        },
        dir: function(msg) {
        	java.lang.System.err.println('console register: ' + msg);
        },
    };
}