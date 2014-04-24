### opennms-requisition Source
This source is reading a requisition via _HTTP_ from another _OpenNMS_ given by `requisition.url`. For authentication a username an password can be provided.

    source = requisition.source

| parameter            | required | description                      |
|----------------------|:--------:|---------------------------------:|
| requisition.url      | *        | OpenNMS requisition ReST URL, e.g. https://opennms.opennms-edu.net/opennms/rest/requisitions  |
| requisition.username |          | OpenNMS user name for access the requisition ReST URL |
| requisition.password |          | OpenNMS user password for access the requisition ReST URL|
