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

1. install swi-prolog 

On Mac, use homebrew:

```
brew cask install swi-prolog
```

2. fix the library

```
$ otool -L /opt/homebrew-cask/Caskroom/swi-prolog/6.6.6/SWI-Prolog.app/Contents/swipl/lib/x86_64-darwin13.1.0/libjpl.dylib
/opt/homebrew-cask/Caskroom/swi-prolog/6.6.6/SWI-Prolog.app/Contents/swipl/lib/x86_64-darwin13.1.0/libjpl.dylib:
```

3. install the library to the local maven repository

```
$ mvn install:install-file -Dfile=/opt/homebrew-cask/Caskroom/swi-prolog/6.6.6/SWI-Prolog.app/Contents/swipl/lib/jpl.jar \
 -DgroupId=org.jpl -DartifactId=jpl -Dversion=6 -Dpackaging=jar
```

 



