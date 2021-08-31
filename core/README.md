# Module kolasu-emf

Kolasu supplies the infrastructure to build a custom, possibly mutable, Abstract Syntax Tree (AST) using Kotlin.
In particular, it can be integrated easily with ANTLR, but it can also be used on its own.
Kolasu strives to be usable and idiomatic also in Java projects.

Kolasu stands for _**Ko**tlin_ _**La**nguage_ _**Su**pport_.

## Features

Extend your AST classes from `Node` to get these features:
* Navigation: utility methods to traverse, search, and modify the AST
* Printing: print the AST as XML, as JSON, as a parse tree 

Classes can have a *name*, and classes can *reference* a name.
Utilities for resolving these references are supplied.

Kolasu tries to be non-invasive and implements this functionality by introspecting the AST.
All properties, and therefore the whole tree structure, will be detected automatically. 
