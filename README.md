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

 



