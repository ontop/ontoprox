How to build
==============

- Get the develop version of ontop and clipper and install them to local maven repo.

```
$ git clone https://github.com/ontop/ontop
$ cd ontop
$ git checkout version2
$ mvn install -DskipTests
$ cd ..
$ git clone https://github.com/ghxiao/clipper
$ cd clipper
$ git checkout develop
$ mvn install -DskipTests
$ cd ..
$ git clone git@gitlab.com:ghxiao/ontop-beyond-ql.git
$ cd ontop-beyond-ql
$ mvn install -DskipTests
```

## Configure SWI-prolog

We rely on SWI-prolog. The following is the instruction on Mac. 

### install swi-prolog 

On Mac, use homebrew:

```
brew  install swi-prolog
```

### install the library to the local maven repository

```
$ cd /usr/local/opt/swi-prolog/libexec/lib/swipl-7.2.2/lib
$ mvn install:install-file -Dfile=jpl.jar  -DgroupId=org.jpl7 -DartifactId=jpl -Dversion=7.0.1 -Dpackaging=jar
```
 



