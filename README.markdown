## _MusicRest_

This is a RESTful web service which allows you to upload traditional tunes in [ABC](http://abcnotation.com/) notation and then have them transcoded into a variety of formats.  In particular, you can use MusicRest to see a tune score in **pdf**, **ps** or **png** format or to listen to the tune (using a variety of tempi and instruments) in **wav** or **midi** format. It supports two different approaches for doing this:  

1. _Content Negotiation_.  You issue a request to the tune's URL and set the *Accept* request header to the required content type.

2. _Explicit URL_.  You append the name of the requested content type to the basic URL of the tune. For example, if you append */wav* it will be returned in *wav* format (irrespective of the contents of the Accept header).

Author: John Watson <john.watson@gmx.co.uk>

### _Usage_

#### _Genres and Rhythms_

Although MusicRest is potentially useful for any genre of music, the current release is restricted to five - _English, Irish, Scandi, Scottish_ and _Klezmer_.  Each genre is configured with a set of standard rhythms for the genre - for example, whilst Irish uses _jigs, reels, hornpipes_ etc., Scandi uses _polska, marsch, schottis_ etc.  Tunes in each genre are kept in separate collections; when posting a new tune, you must supply an appropriate rhythm (ABC's  _R_ header).

#### _URL Scheme_

URL path segments in italics represent fixed text; those in bold type are variable.  For example, the text 'genre' is fixed whilst **agenre** can be any of _irish, scandi, scottish or klezmer_. The following URLs are supported:

##### Tunes

*  GET / _musicrest_ / _genre_ - get a list of genres.

*  GET / _musicrest_ / _genre_ / **agenre** - get a list of rhythms appropriate for the genre.

*  GET / _musicrest_ / _genre_ / **agenre** / _tune_ - get a paged list of tunes.

*  POST / _musicrest_ / _genre_ / **agenre** / _tune_ - submit a tune in ABC format.

*  GET / _musicrest_ / _genre_ / **agenre** / _exists_ - return true if the genre exists

*  GET / _musicrest_ / _genre_ / **agenre** / _tune_ / **atune** - get a tune in the format suggested by the Accept header.

*  DELETE / _musicrest_ / _genre_  / **agenre** / _tune_ / **atune** - delete the tune from the database.

*  GET / _musicrest_ / _genre_ / **agenre** / _tune_ / **atune** / _exists_ - return true if the tune exists

*  GET / _musicrest_ / _genre_ / **agenre** / _tune_ / **atune** / **format** - get a tune the requested format (which can be abc, html, wav, pdf, ps or midi)

*  POST / _musicrest_ / _genre_ / **agenre** / _tune_ / **atune** / _abc_ - add an alternative title to the tune

*  GET / _musicrest_ / _genre_ / _search_ - get a paged list of tunes that correspond to the search parameters.

*  POST / _musicrest_ / _genre_ / _transcode_ - submit a tune to temporary storage to check the validity of the ABC

A user must be logged in before he can submit or test-transcode a tune. Only the original submitter or the *administrator* user is allowed to delete tunes.  

Production of wav output is still supported, but deprecated.  It is too expensive to produce server-side unless you have a powerful server and a large data cache.


##### Comments

*  GET / _musicrest_ / _genre_ / **agenre** / _tune_ / **atune** / _comments_ - get the comments attached to a tune

*  POST / _musicrest_ / _genre_ / **agenre** / _tune_ / **atune** / _comments_ - add or edit a comment and attach it to a tune

*  GET / _musicrest_ / _genre_ / **agenre** / _tune_ / **atune** / _comment_ / **auser** / **acommentid** - get a particular comment (written by the user with this id)

*  DELETE / _musicrest_ / _genre_ / **agenre** / _tune_ / **atune** / _comment_ / **auser** / **acommentid** - delete this comment

*  DELETE / _musicrest_ / _genre_ / **agenre** / _comments_  - delete all comments in the genre

##### Users

There is also a fairly conventional set of URLs for user maintenance.

#### _URL Parameters_

URLs that return lists take optional paging parameters indicating the number of entries on a page and the identity of the page to display. The URL that requests a tune in _wav_ format takes optional parameters indicating the tunes's instrument, tempo and transposition.

#### _Content Negotiation_

It is possible to request a tune in a format that corresponds to any of the following MIME types:

* text/plain
* text/xml
* text/html
* text/vnd.abc
* application/json
* audio/midi
* audio/wav
* application/postscript
* application/pdf
* image/png

Most lists may be obtained in xml, html or json format.

#### _Runtime Dependencies_

Linux is required for its transcoding services. MusicRest has been tested under Ubuntu 12.04. The following must be installed or present before the service can be run:

* a JVM (Java 1.8 or better)
* [MongoDB](http://www.mongodb.org/)  (2.4.14)
* [AbcMidi](http://abc.sourceforge.net/abcMIDI/) (3.10 or better)
* [Abcm2ps](http://abcplus.sourceforge.net/#abcm2ps) (5.9.25 or better)
* [ImageMagick](http://www.imagemagick.org/script/index.php) (8:6.6.9.7-5ubuntu or better)
* [Timidity](https://wiki.archlinux.org/index.php/Timidity)  (2.13.2 or better)

The _scripts_ directory contains shell scripts that invoke the transcoding services.  You must be sure to make these executable  ( _chmod +x_ )

#### _Configuration_

This is by means of _musicrest.conf_ which must be supplied as a JVM -D startup parameter. The settings are largely self explanatory.  The cache directory for transcode indicates that the transcoding scripts are file-based and the results of such scripts are allowed to remain in the file cache. Once the cache reaches its maximum allowed size for a time dependent on _cacheClearInterval_ (a time in minutes) then the cache is cleared. Setting this to zero disables cache clearance. Get tune requests are served from the cache where this is possible. If you are supporting the generation of _wav_ responses, you probably need to clear the cache periodically because of the large number of bulky .wav files that can be generated.

As from version 1.3.5, Musicrest assumes it is accessed via a reverse-proxy server. This allows the service to appear to live at `https://www.tradtunedb.org.uk/musicrest` and the proxy forwards traffic to  `http://www.tradtunedb.org.uk:8080/musicrest`.  This also obviates the need for CORS headers (see below).

Email is used simply to finish user registration or to remind users of passwords.  Email settings must of course be valid for the carrier in question.  The URL contained in the email to complete the registration process uses the proxy address.

_corsOrigins_ allows any of the nominated servers to make available scripts that use  XmlHttpRequest to access midi content in the MusicRest server.

    musicrest {
      server {
        host = "localhost"
        port = 8080
      }
      transcode {
        scriptDir = "scripts"
        cacheDir = "cache/main"
        cacheClearInterval = 60
        cacheMaxSizeMb = 2000
      }
      database {
        host   = "localhost"
        port   = 27017
        dbName = "tunedb"
        login = "musicrest"
        password = "changeit"
        poolSize = 60
      }
      paging {
        defaultSize = 10
      }
      mail {
        host = "smtp.gmail.com"
        port = "587"
        login = "youraccount@gmail.com"
        password = "change1t"
        fromAddress = "youraccount@gmail.com"
      }
      security {
        corsOrigins = ["http://localhost:9000", "http://www.otherhost.org"]
      }
    }

#### _Web Front End_

There is no web front end included within this project.  However [tradtunedb](http://tradtunedb.org.uk/) and [tunebank-frontend](http://tradtunedb.org.uk:8604/) use musicrest as a backend service.

### _Build and Deploy_

#### _Source Code_

Code is written in scala 2.11.7 and built with sbt (currently 1.8.0).   

```
   "io.spray"            %   "spray-can_2.11"     % "1.3.4",
   "io.spray"            %   "spray-routing_2.11" % "1.3.4",
   "io.spray"            %   "spray-caching_2.11" % "1.3.4",
   "io.spray"            %   "spray-testkit_2.11" % "1.3.4",
   "com.typesafe.akka"   %%  "akka-actor"         % "2.4.11",
   "com.typesafe.akka"   %%  "akka-testkit"       % "2.4.11",
   "org.scalaz"          %%  "scalaz-core"        % "7.1.6",
   "org.scalaz.stream"   %%  "scalaz-stream"      % "0.8",
   "org.mongodb"         %%  "casbah"             % "3.1.1",
   "net.liftweb"         %   "lift-webkit_2.11"   % "3.0-M8",
   "javax.mail"          %   "mail"               % "1.4.7",
   "org.specs2"          %%  "specs2"             % "2.5-scalaz-7.1.6" % "test",
   "io.argonaut"         %%  "argonaut"           % "6.1" % "test"
```

Spray uses logback for logging, but there is an unfortunate dependency on slf4j brought in by Casbah.

#### _Bootstrapping the Database

Firstly, create a MongoDB database, configure an admin user and determine the name of the database you wish to use (e.g. tunedb). The admin user needs the following permissions - userAdminAnyDatabase, readWriteAnyDatabase and dbAdminAnyDatabase.
Then set up a user within this database with readWrite permissions for use in the database section of the MusicRest configuration file shown above.

Before MusicRest can be used, you need to set an administrator user within MusicRest itself and also set up the genres and rhythms the system recognizes.  These are held on the database and can be set up by means of the scripts _userinsert.sh_ and _genreinsert.sh_. These scripts
should be run with mongod __without__ the --auth parameter (because of what seems to be a Mongo 2.4/Casbah 3.1.1 bug which disallows creating
indexes).  Once the scripts are complete and the user and genre collections inspected, then reboot with --auth in place.
There are also scripts to import tunes into or export tunes from the database in bulk.

#### _Build Instructions_

1. Install the runtime dependencies.  Administer Mongo with appropriate users. Start Mongo and configure MusicRest with the name of the Mongo database and the user credentials you wish to use

2. Launch sbt

3. _compile_ - compiles the source

4. _test_ - runs the tests (requires a valid Mongo connection)

5. _run_ and then choose the _Boot_ option to start the service.

Alternatively, you can build and then run a single jar assembly using:

1. sbt _assembly_

2. run.sh
