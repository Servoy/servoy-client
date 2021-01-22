var karmaBaseConfig = require('./karma.conf.js');

module.exports = function(config) {
	// apply basic configuration
	karmaBaseConfig(config);
	
	// change what is needed to nicely run them locally
	config.singleRun = false;
	config.browserNoActivityTimeout = 999999;
    config.browsers =['Chrome','Firefox', 'PhantomJS'];
//    config.browsers =['Chrome'];
//    config.browsers =['PhantomJS'];
//    config.browsers =['Firefox'];
    
    for (let i = 1; i < config.files.length; i++) {
    	if (config.files[i] == 'sablo/META-INF/resources/sablo/js/*.js') config.files[i] ='../../../sablo/sablo/META-INF/resources/sablo/js/*.js'
    	else if (config.files[i] == 'sablo/META-INF/resources/sablo/*.js') config.files[i] ='../../../sablo/sablo/META-INF/resources/sablo/*.js';
    }
}
