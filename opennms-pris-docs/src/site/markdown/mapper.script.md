### Script mapper
This mapper is used to give you the flexibility building your own mapper without the need compiling from source.
The source uses [JSR-223 Scripting Engine](https://www.jcp.org/en/jsr/detail?id=223).
The used language can be changed by setting the property `mapper.lang` in your `requisition.properties` file.
The following example runs your script in the the [JavaScript Rhino](http://en.wikipedia.org/wiki/Rhino_%28JavaScript_engine%29) engine:

    file: requisition.properties
    ---
    ### SOURCE ###
    ## Use Script Source
    source = ...

    ### MAPPER ###
    ## Run a no operation mapper
    mapper = echo.mapper
    mapper.lang=javascript
    mapper.file = myGroovySource.js


If you don't set the language `lang` property _Groovy_ will be used instead.

| parameter   | required | description                                        |
|-------------|:--------:|---------------------------------------------------:|
| mapper      | * |`script` to use JSR-223 Script Engine as source            |
| mapper.file | * |Path to script source relative to `requisition.properties` |
| mapper.lang |   |JSR-223 Script language by name                            |

You can find a working example in _Groovy_ in the `examples/script.mapper` directory.
