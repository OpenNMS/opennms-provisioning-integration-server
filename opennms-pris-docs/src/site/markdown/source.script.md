### Script source
This source is build to give you the flexibility building your own source without the need compiling from source.
The source uses [JSR-223 Scripting Engine](https://www.jcp.org/en/jsr/detail?id=223).
The used language can be changed by setting the property `source.lang` in your `requisition.properties` file.
The following example runs your script in the the [JavaScript Rhino](http://en.wikipedia.org/wiki/Rhino_%28JavaScript_engine%29) engine:

    file: requisition.properties
    ---
    ### SOURCE ###
    ## Use Script Source
    source = script

    ## Source file ##
    source.lang=javascript
    source.file = myGroovySource.js

    ### MAPPER ###
    ## Run a no operation mapper
    mapper = echo.mapper

If you don't set the language `lang` property _Groovy_ will be used instead.

| parameter   | required | description                                        |
|-------------|:--------:|---------------------------------------------------:|
| source      | * |`script` to use JSR-223 Script Engine as source            |
| source.file | * |Path to script source relative to `requisition.properties` |
| source.lang |   |JSR-223 Script language by name                            |

You can find a working example in _Groovy_ in the `examples/script.source` directory.
