client_tests folder is a standard npm package
TO RUN KARMA FORM COMMAND LINE: "node ./node_modules/karma/bin/karma start karma.conf.js"

PREREQUISITES:
 - node installed and available in path
 - npm installed and available in path (it is installed automatically with node)
 - git installed and available in path


In  runt_tests.xml there is a target to run the unit tests: 'run_tests'. By default it runs them once.
To use continous development mode, create a copy of "development_mode.properties.removeThisEXT" in the same folder
and remove it's extension'. The run once is used for one time run (jenkins).
The latter one can be used for development purposes, it runs all tests on any js file change and you can debug it in chrome.

client_tests folder structure:
client_tests   - main folder which contains the node module responsible for ngclient browser unit tests 
   package.json - defines the wc2 unit test module with it's dependencies to karma and helpers.
   node_modules - automatically created and populated  when running install_karma ant target (npm reads dependencies from package.json and resolves them) 
   karma.conf.js - contains configuration for karma test runner which runs only once , for jenkins or manual build(ex what folders to include  , what result output format , where to output the result)
   				   it is used when running  ant target 'run_tests'
   karma_run_on_file_change.conf.js  -  this is for development mode ,starts a test server and runs test for any js change.
   									    It is used from run_tests_continuously target
   runt_tests.xml - contains ant targets targets     									   
   development_mode.properties.removeThisEX - file that can be copied with extension removed to the same folder to enable development mode of karma.
   
   test - contains unit test code
   
To run karma from command line run :"node ../node_modules/karma/bin/karma start karma.conf.js" ; note you need the sablo path from karma.conf.js to be correct
To debug a test : http://stackoverflow.com/questions/15906903/debug-in-browser-using-testacular-now-karma       (if you need to debug more than one minute set 'singleRun' to false in karma.conf.js ,run karma  and open http://localhost:9876/debug.html)
To update check if a newer version of a depended library exists use "npm outdated"
To update to the latest stable version do change the string versions in the package.json to '*' and run  "npm update --save" , 
this will update to the latest version and writhe the actual dependent version in package json.
To install a new dependency automatically without needing to modify the package.json  run for example "npm install karma-ng-html2js-preprocessor --save"
