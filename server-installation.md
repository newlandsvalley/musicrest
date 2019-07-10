Notes on Dev Server Installations
=================================

Mongo
-----

We still use MongoDB 2.4.14 which is a very old version (currently 4.x).  The mongo data is installed to /var/lib/mongodb and the development database is named tunedb.  To start the server, at the moment the mongo service in /etc is not installed and so use

```
mongod --dbpath /var/lib/mongodb 
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

building musicrest 
------------------

from home/develop,ent/workspace/musicrest

```
   sbt compile
   sbt test
   sbt assembly
   etc
```