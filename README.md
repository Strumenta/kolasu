# Kolasu

[![Build Status](https://travis-ci.org/ftomassetti/kolasu.svg?branch=master)](https://travis-ci.org/ftomassetti/kolasu)

Kolasu is a library that can be used for writing Abstract Syntax Trees and AST processing. It is particularly suited for working with ANTLR.

## Origin

It stands for Kotlin Language Support. 

It was born as a small framework to support building languages using ANTLR and Kotlin. 

## Using Kolasu in your project

Snapshots are published on JitPack.

```
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    compile "com.github.strumenta:kolasu:20fb9cd239"
}
```

Releases are published on GitHub registry. 

```
repositories {
    maven { url 'https://maven.pkg.github.com/Strumenta/kolasu' }
}

dependencies {
    compile "com.github.strumenta:kolasu:v0.2.2"
}
```

## Publishing

To publish releases you need to set the environment variables GPR_USER and GPR_API_KEY.