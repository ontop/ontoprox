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

We rely on SWI-prolog for Datalog expansions and we use the java interface JPL. 

###  OS X  

* Install swipl-prolog via homebrew:

```
$ brew  install swi-prolog --with-jpl
```

* install the library to the local maven repository

```
$ cd /usr/local/opt/swi-prolog/libexec/lib/swipl-7.2.2/lib
$ mvn install:install-file -Dfile=jpl.jar  -DgroupId=org.jpl7 -DartifactId=jpl -Dversion=7.0.1 -Dpackaging=jar
```


 ### Ubuntu Linux   
 
 * install swi-prolog via apt-get:
 
```
$ sudo apt-get install software-properties-common
$ sudo apt-add-repository ppa:swi-prolog/stable
$ sudo apt-get update
$ sudo apt-get install swi-prolog
$ sudo apt-get install swi-prolog-java
```
 
* install the library to the local maven repository
 
```
$ cd /usr/lib/swi-prolog/lib
$ mvn install:install-file -Dfile=jpl.jar  -DgroupId=org.jpl7 -DartifactId=jpl -Dversion=7.0.1 -Dpackaging=jar
```


## VW args

* OS X

`-Djava.library.path=/usr/local/Cellar/swi-prolog/7.2.2/libexec/lib/swipl-7.2.2/lib/x86_64-darwin14.4.0`

* Ubuntu

`-Djava.library.path=/usr/lib/swi-prolog/lib/amd64`





