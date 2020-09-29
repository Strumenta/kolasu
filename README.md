# Kolasu

[![Build Status](https://github.com/Strumenta/kolasu/workflows/BuildAndTest/badge.svg)](https://github.com/Strumenta/kolasu/actions)
[![Gitter](https://badges.gitter.im/lang-eng/community.svg)](https://gitter.im/lang-eng/community?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)

Kolasu supplies the infrastructure to build a custom, possibly mutable, Abstract Syntax Tree (AST) using Kotlin.
In particular it can be integrated easily with ANTLR, but it can also be used without.

It stands for _**Ko**tlin_ _**La**nguage_ _**Su**pport_.

## Features

Extend your AST classes from `Node` to get these features:
* Navigation: utility methods to traverse, search, and modify the AST
* Printing: print the AST as XML, as JSON, as a parse tree 

Classes can have a *name*, and classes can *reference* a name.
Utilities for resolving these references are supplied.

Kolasu tries to be non-invasive and implements this functionality by introspecting the AST.
All properties, and therefore the whole tree structure, will be detected automatically. 

## Origin

Kolasu was born as a small framework to support building languages using ANTLR and Kotlin. 

## Using Kolasu in your project

Releases are published on Bintray: 

```
repositories {
    maven { url 'https://maven.pkg.github.com/strumenta/strumenta-oss-maven' }
}

dependencies {
    compile "com.strumenta:kolasu:1.0.5"
}
```

Snapshots are published on JitPack.

```
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    compile "com.github.strumenta:kolasu:master"
}
```

## How to format code

Run:

```
./gradlew ktlintFormat
```

## Projects using Kolasu

Kolasu is used in several internal and commercial projects developed at [Strumenta](https://strumenta.com).

It is also used in an open-source project named [Jariko](https://github.com/smeup/jariko). 
Jariko is an interpreter for RPG running on the JVM.

## Publishing

To publish releases you need to set the environment variables GPR_USER and GPR_API_KEY.
