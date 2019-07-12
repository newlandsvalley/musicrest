Notes on Dev Server Installations
=================================

Mongo
-----

We still use MongoDB 2.4.14 which is a very old version (currently 4.x).  The mongo data is installed to /var/lib/mongodb and the development database is named tunedb.  To start the server, at the moment the mongo service in /etc is not installed and so use

```
sudo mongod --dbpath /var/lib/mongodb 
```

If we're lucky, the service will start and announce that it is waiting for HTTP connections. It may be that the server is locked, in which case, unock it with:

```
sudo rm /var/lib/mongodb mongod.lock
```

musicrest
---------

Start by typing

```
musicrest start
```

from services/musicrest/bin.  This should connect to mongo and accept HTTP requests on:

````
http://192.168.0.113:8080/musicrest
````

configuring musicrest
---------------------

It is configured by musicrest.conf (even in the dev environment). While testing, it is probably a good idea to configure the CORS responses like this:

```
  security {
    corsOrigins = ["*"]
  }
```

and then later to replace the star with our server (http://192.168.0.113:80)


building musicrest 
------------------

from home/develop,ent/workspace/musicrest

```
   sbt compile
   sbt test
   sbt assembly
   etc
```

It seems as if (in our current environment) between 4 and 7 tests regularly miss the Spray timeout period of 1 second.  Mitigate at the moment by removing the test step in the SBT assembly stage:

```
test in assembly := {}
```