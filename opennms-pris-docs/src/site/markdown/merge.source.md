### merge-source
The merge source allows to merge two requisitions. You can also use provided resources by pris recursively.

    source = requisitionMerge.source

| parameter                    | required | description             |
|------------------------------|:--------:|------------------------:|
| `requisition.A.url`          | *        |URL to the requisition A |
| `requisition.A.username`     |          |username for access      |
| `requisition.A.password`     |          |password for access      |
|                              |:        :|                        :|
| `requisition.B.url`          | *        |URL to the requisition B |
| `requisition.B.username`     |          |username for access      |
| `requisition.B.password`     |          |password for access      |
|                              |:        :|                        :|
| `requisition.merge.keepAllA` |          | if this parameters is present in the config all nodes from requisition A will be present in the resulting requisition. |
| `requisition.merge.keepAllB` |          | if this parameters is present in the config all nodes from requisition B will be present in the resulting requisition. |

This source is reading two already defined requisitions via _HTTP_ and merges them into one new requisition. By default the resulting requisition will contain all nodes that are present in both requisitions, identified by the `foreignId`. The A-Node (from requisition A) is enriched with the data from B-Node.
