client_tests folder is a standard npm package

PREREQUISITES:
 - node installed and available in path
 - npm installed and available in path (it is installed automatically with node)
 - git installed and available in path


In  runt_tests.xml There are two targets to run the unit tests: 'run_tests' and 'run_tests_continuously'. The first one is used for one time run (jenkins) .
The latter one can be used for development purposes , it runs all tests on any js file change.

client_tests folder structure:
client_tests   - main folder which contains the node module responsible for ngclient browser unit tests 
   package.json - defines the wc2 unit test module with it's dependencies to karma and helpers.
   node_modules - automatically created and populated  when running install_karma ant target (npm reads dependencies from package.json and resolves them) 
   karma.conf.js - contains configuration for karma test runner which runs only once , for jenkins or manual build(ex what folders to include  , what result output format , where to output the result)
   				   it is used when running  ant target 'run_tests'
   karma_run_on_file_change.conf.js  -  this is for development mode ,starts a test server and runs test for any js change.
   									    It is used from run_tests_continuously target
   runt_tests.xml - contains ant targets targets     									   

   
   test - contains unit test code
   
 
