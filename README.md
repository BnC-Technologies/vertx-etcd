[![Build Status](https://travis-ci.org/BnC-Technologies/vertx-etcd.svg?branch=master)](https://travis-ci.org/BnC-Technologies/vertx-etcd) 
[ ![Download](https://api.bintray.com/packages/bnc-technologies/Maven/vertx-etcd/images/download.svg) ](https://bintray.com/bnc-technologies/Maven/vertx-etcd/_latestVersion)

# Overview

Vertx etcd is an etcd V2 client wrote upon vertx.

It's wrote in scala but is compitable with the java version of vertx.

There is also a scala version of it.

It requires java 8 for the java version

# Setup

Jar s are stored in a bintray repository

## Maven

Add to your pom the following code to resolve the dependecies

```xml
<repositories>
    <repository>
        <snapshots>
            <enabled>false</enabled>
        </snapshots>
        <id>bintray-bnc-technologies-Maven</id>
        <name>bintray</name>
        <url>http://dl.bintray.com/bnc-technologies/Maven</url>
    </repository>
</repositories>
```

### Java

If you only want to use in java add the following dependency

```xml
<dependecy>
      <artifactId>vertx-etcd</artifactId>
      <groupId>com.bnctech</groupId>
      <version>1.0.0</version>
</dependecy>
```

### Scala

If you only want to use in scala add the following dependency

```xml
<dependecy>
      <artifactId>vertx-etcd-scala_2.12</artifactId>
      <groupId>com.bnctech</groupId>
      <version>1.0.0</version>
</dependecy>
```

# Limitation

The current implementation does not work with secured etcd client (https).

This feature will come in a future version

# Usage
