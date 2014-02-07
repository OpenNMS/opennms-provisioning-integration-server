# Scheduling automatic backups

### Linux

#### Prerequisites

OpenNMS installation, e.g.:

    /opt/opennms
    
OpenNMS BRU installation, e.g.:

    /opt/opennms-bru

Working backup.xml file pointing to the right locations for pg_dump and pg_restore utilities. For example:
    
    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
    <local-backup-config>
        <backup-service-security-token>9710f7c0-61ea-11e3-949a-0800200c9a66</backup-service-security-token>
        <backup-service-url>http://bru.opennms.com:8080/backup-war/endpoint/backups</backup-service-url>
        <base-directory>/opt/opennms</base-directory>
        <database-host>localhost</database-host>
        <database-name>opennms</database-name>
        <database-password>opennms</database-password>
        <database-port>5432</database-port>
        <database-username>opennms</database-username>
        <directories>
            <directory>etc</directory>
            <directory>share</directory>
            <directory>dbdump</directory>
        </directories>
        <max-concurrent-uploads>8</max-concurrent-uploads>
        <pg-dump-location>/usr/bin/pg_dump</pg-dump-location>
        <pg-restore-location>/usr/bin/pg_restore</pg-restore-location>
    </local-backup-config>

#### Ubuntu setup

If not done before issue the following command to select your favourite editor. In the following example we choose ***vim.basic***.

    user@opennms:~$ sudo select-editor 

    Select an editor.  To change later, run 'select-editor'.
       1. /bin/ed
       2. /bin/nano        <---- easiest
       3. /usr/bin/vim.basic
       4. /usr/bin/vim.tiny

    Choose 1-4 [2]: 3
    
Now edit the ***crontab*** for the root user:

    user@opennms:~$ sudo crontab -e

The previous selected editor opens the ***crontab*** for the ***root*** user. Now, you can add an entry for the new task:

    # Edit this file to introduce tasks to be run by cron.
    # 
    # Each task to run has to be defined through a single line
    # indicating with different fields when the task will be run
    # and what command to run for the task
    # 
    # To define the time you can provide concrete values for
    # minute (m), hour (h), day of month (dom), month (mon),
    # and day of week (dow) or use '*' in these fields (for 'any').# 
    # Notice that tasks will be started based on the cron's system
    # daemon's notion of time and timezones.
    # 
    # Output of the crontab jobs (including errors) is sent through
    # email to the user the crontab file belongs to (unless redirected).
    # 
    # For example, you can run a backup of all your user accounts
    # at 5 a.m every week with:
    # 0 5 * * 1 tar -zcf /var/backups/home.tgz /home/
    # 
    # For more information see the manual pages of crontab(5) and cron(8)
    # 
    # m h  dom mon dow   command
    15 13 * * * /opt/opennms-bru/client-cli.sh backup >> /opt/opennms-bru/client-cli.log 2>&1

In this example the backup will run daily at 1:15am. To learn more about schedules and the syntax of the ***crontab*** file take a look at its manpage:

    man 5 crontab
    
After you exit from the editor, the modified ***crontab*** ist validated and installed automatically.

#### Redhat setup

Login as ***root*** and check whether the ***cron*** daemon is installed:

    # rpm -qa | grep cron
    cronie-1.4.4-2.el6.x86_64
    cronie-anacron-1.4.4-2.el6.x86_64
    crontabs-1.10-32.1.el6.noarch
    
If, for some reason, it is not installed you can issue the following command to install it:

    # yum install -y cronie cronie-anacron crontabs
    
Now, you can edit the ***crontab*** by invoking the following command:

    # crontab -e

Add the following line to the end of the file:

    15 13 * * * /opt/opennms-bru/client-cli.sh backup >> /opt/opennms-bru/client-cli.log 2>&1

In this example the backup will run daily at 1:15am. To learn more about schedules and the syntax of the ***crontab*** file take a look at its manpage:

    man 5 crontab
    
After you exit from the editor, the modified ***crontab*** ist validated and installed automatically.
    
### Windows 

#### Prerequisites

OpenNMS installation, e.g.:

    C:\Program Files\OpenNMS

OpenNMS BRU installation, e.g.:

    C:\Program Files\OpenNMS BRU
    
Working ***backup.xml*** file pointing to the right locations for ***pg_dump*** and ***pg_restore*** utilities.
For example:

    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
    <local-backup-config>
        <backup-service-security-token>9710f7c0-61ea-11e3-949a-0800200c9a66</backup-service-security-token>
        <backup-service-url>http://bru.opennms.com:8080/backup-war/endpoint/backups</backup-service-url>
        <base-directory>C:\Program Files\OpenNMS</base-directory>
        <database-host>localhost</database-host>
        <database-name>opennms</database-name>
        <database-password>opennms</database-password>
        <database-port>5432</database-port>
        <database-username>opennms</database-username>
        <directories>
            <directory>etc</directory>
            <directory>share</directory>
            <directory>dbdump</directory>
        </directories>
        <max-concurrent-uploads>8</max-concurrent-uploads>
        <pg-dump-location>C:\Program Files\PostgreSQL\9.3\bin\pg_dump.exe</pg-dump-location>
        <pg-restore-location>C:\Program Files\PostgreSQL\9.3\bin\pg_restore.exe</pg-restore-location>
    </local-backup-config>

#### Windows 2012 Server setup

> Search and open the Windows ***Task Scheduler*** and hit ***Create Basic Task*** in the upper-right panel ***Actions***.

![Bla Bla Blupp](images/w2012-step1.png "Create Basic Task")

> Enter the name for this task, e.g. ***OpenNMS Backup***. Proceed to the next page by invoking the ***Next*** button.

![Bla Bla Blupp](images/w2012-step2.png "Titel")

> Select ***Daily*** and proceed to the next page by invoking the ***Next*** button.

![Bla Bla Blupp](images/w2012-step3.png "Titel")

> Enter the start date and time for this task and
proceed to the next page by invoking the ***Next*** button.

![Bla Bla Blupp](images/w2012-step4.png "Titel")

> Select ***Start a program*** and proceed to the next page by invoking the ***Next*** button.

![Bla Bla Blupp](images/w2012-step5.png "Titel")

> Browse to the ***client-cli.bat*** file in your
***OpenNMS BRU*** installation directory. Enter ***backup*** in the ***Add argument*** input field. Also enter the path 
***C:\Program Files\OpenNMS BRU*** in the ***Start in*** input field. Proceed to the next page by invoking the ***Next*** button.

![Bla Bla Blupp](images/w2012-step6.png "Titel")

> Select the ***Open the Properties dialog for this task when I click Finish*** checkbox and close the wizard by
invoking the ***Finish*** button.

![Bla Bla Blupp](images/w2012-step7.png "Titel")

> Select ***Run whether user is logged on or not*** and hit the ***OK*** button. If you are asked for username and password enter your ***Administrator*** credentials.



