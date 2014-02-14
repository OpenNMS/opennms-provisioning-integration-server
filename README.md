# OpenNMS Provisioning Integration Server
The provisioning integration server (pris) is a software which provides the ability to get external information from your inventory into an OpenNMS requisition model. The output from pris is provided as XML over HTTP and can be used in OpenNMS Provisiond to import and discover nodes from.

The project is divided in three modules:

* `parent` module ties documentation and the server itself together. Maintains also version numbers
* `opennms-pris` is the server code itself.
* `opennms-pris-docs` the documentation for the software and sources

The documentation will be automatically build from Markdown to HTML and you can browse the docs in the target directory.

# Structure of the git repository
Check out the source code from [GitHub] with the following command:

    https://github.com/OpenNMS/opennms-provisioning-integration-server.git

You have several branches you can build now. The master branch is the latest functional release. The stable releases are tagged. To show specific releases change in to the source directory and use the command

    git tag -l

It will give you a list of all tagged releases. There are several other branches which follow the [nvie] branch pattern and has the following naming convention

* `master` a production-ready state
* `release-*` a stable release
* `hotfix-*` fix for bugs
* `feature-*` enhance the software with a new feature
* `develop` reflects a state with the latest delivered development changes for the next release.

You can get special branches with the command

    git checkout -b feature-vmware-source origin/feature-vmware-source

If you want to get a special tagged version you can use the command

    git checkout
    git checkout tags/<tag_name>

It will switch your source to the given tagged version.

# Compiling from source
This guide describes how you can checkout the source code from GitHub and how you can compile from source. The following parts are required:

* [OpenJDK] or [Oracle Java Development Kit] with javac
* Apache [Maven]
* [git-scm]
* `java`, `javac`, `git` and `mvn` should be in your search path
* Internet connection to download maven dependencies

In your source directory run the command

    mvn clean package

It make sure cleaning everything from previous builds, compiles the code and build everything as a runnable jar in target directories.

# General information
We don't use the issue tracking from GitHub, we use instead the main OpenNMS project issue tracker, cause this part is developed within the OpenNMS Project.

* Issues: http://issues.opennms.org/browse/PRIS
* IRC: irc://freenode.org/#opennms
* License: GPLv3
* Illustrations created with [yED]

[GitHub]: https://github.com/OpenNMS/opennms-provisioning-integration-server.git
[OpenJDK]: http://openjdk.java.net/
[Oracle Java Development Kit]: http://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html
[Maven]: http://maven.apache.org/
[git-scm]: http://git-scm.com/
[nvie]: http://nvie.com/posts/a-successful-git-branching-model/
[yED]: http://www.yworks.com/en/products_yed_about.html


[old doku]
http://www.opennms.org/wiki/OCS_Integration

