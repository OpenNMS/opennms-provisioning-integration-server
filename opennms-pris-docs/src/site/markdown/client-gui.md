# Client GUI

### Starting the client-gui Vaadin app

Change to the ***client-gui*** directory and invoke the maven jetty goal:

    $ cd client-gui
    $ mvn jetty:run

Now, point your web-browser to the following address:

    http://localhost:8181

The application uses a file ***client-gui.properties*** which stores the path to the
***backup.xml*** file. You can change this path or you can create a new backup configuration
file at this location. After successfully configuring you can query the remote server
for a list of available backups.