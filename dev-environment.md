Notes on Development Environment
================================

Here are just a few notes about how I set up a development environment to allow testing of the MusicRest service.

Mongo
-----

We still use MongoDB 2.4.14 which is a very old version (currently 4.x).  The mongo data is installed to /var/lib/mongodb and the development databases are named tunedb and tunedbtest.  We first add an overall admin user and then a normal user to each of these databases named musicrest with readWrite access to allow programmatic access. We do not use init.d services to start the server. Instead, to start the server with authentication enabled, use

```
sudo mongod --dbpath /var/lib/mongodb --auth
```

If we're lucky, the service will start and announce that it is waiting for HTTP connections. It may be that the server is locked, in which case, unock it with:

```
sudo rm /var/lib/mongodb mongod.lock
```

To administer Mongo or its databases, use: 

```
mongo --username <adminuser> --password <adminpassword> --authenticationDatabase admin
```

musicrest
---------

Start by typing

```
./musicrest start
```

from services/musicrest/bin.  This should connect to mongo and accept HTTP requests on:

````
http://192.168.0.113:8080/musicrest
````

configuring musicrest
---------------------

It is configured by musicrest.conf (even in the dev environment). Make sure that the database login and password for the appropriate database are set correctly. While testing, it is probably a good idea to configure the CORS responses like this:

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