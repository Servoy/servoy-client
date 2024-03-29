var karmaBaseConfig = require('./karma.conf.js');

module.exports = function(config) {
	// apply basic configuration
	karmaBaseConfig(config);
	
	// change what is needed to nicely run them locally
	config.singleRun = false;
	config.browserNoActivityTimeout = 999999;
    config.browsers =['Chrome','Firefox'];
//    config.browsers =['Chrome'];
//    config.browsers =['Firefox'];
    
    for (let i = 1; i < config.files.length; i++) {
        config.files[i] = config.files[i].replace('sablo/META-INF', '../../../sablo/sablo/META-INF');
    }
}
