throw the webclient2.xml in this tomcat dir:

[tomcat8 install]/conf/Catalina/localhost

edit the docBase path in that file (docBase="C:/workspace_trunk/servoy_webclient2/war/")
to the location of the war dir of the project.
and change all the PostResources paths to the correct path.

start the database server of your servoy install so that we have a repostiory server and a udm server
That repository should contain "WebClient2.servoy" (in the launch dir)

the start tomcat in debug mode: ([tomcat8 install]/bin)

catalina jpda start

by default this will open a port 8000

Then you can start the "Remote Tomcat" launch that will connect to it.

now open http://localhost:8080/webclient2/index.html


Do notice if the server does start, and if you see class not found exceptions then add that jar (from shared/lib or server/lib) to the war\WEB-INF\lib.

The server starts with the servoy.properties file in the /war/WEB-INF/servoy.properties


