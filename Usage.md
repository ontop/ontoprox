How to use ontop-beyond-ql on Ubuntu Linux

 
1. install swi-prolog via apt-get:
 
```
$ sudo apt-get install software-properties-common
$ sudo apt-add-repository ppa:swi-prolog/stable
$ sudo apt-get update
$ sudo apt-get install swi-prolog
$ sudo apt-get install swi-prolog-java
```

2. How to use the jar

```
$ java -Djava.library.path=<swipl-lib-directory> \
 -Djava.ext.dirs=<jdbc-directory> -jar ontop-beyond-ql-1.15.0-jar-with-dependencies.jar \
  <ontology.owl> <mapping.obda> <newMapping.obda>
```  

For instance, on my machine,

```
java -Djava.library.path=/usr/local/Cellar/swi-prolog/7.2.2/libexec/lib/swipl-7.2.2/lib/x86_64-darwin14.4.0 \
-Djava.ext.dirs=/Users/xiao/.m2/repository/mysql/mysql-connector-java/5.1.35/ \
-jar ontop-beyond-ql-1.15.0-jar-with-dependencies.jar  ../src/test/resources/uobm/univ-bench-dl.owl \ 
../src/test/resources/uobm/univ-bench-dl.obda newUOBM.obda
```