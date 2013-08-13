## _MusicRest_ 

This is a RESTful web service which allows you to upload traditional tunes in [ABC](http://abcnotation.com/) notation and then have them transcoded into a variety of formats.  In particular, you can use MusicRest to see a tune score in **pdf**, **ps** or **png** format or to listen to the tune (using a variety of tempi and instruments) in **wav** or **midi** format. It supports two different approaches for doing this:  

1. _Content Negotiation_.  You issue a request to the tune's URL and set the *Accept* request header to the required content type.

2. _Explicit URL_.  You append the name of the requested content type to the basic URL of the tune. For example, if you append */wav* it will be returned in *wav* format (irrespective of the contents of the Accept header).

Author: John Watson <john.watson@gmx.co.uk>

### _Usage_

#### _Genres and Rhythms_

Although MusicRest is potentially useful for any genre of music, the current release is restricted to four - _Irish, Scandi, Scottish_ and _Klezmer_.  Each genre is configured with a set of standard rhythms for the genre - for example, whilst Irish uses _jigs, reels, hornpipes_ etc., Scandi uses _polska, marsch, schottis_ etc.  Tunes in each genre are kept in separate collections; when posting a new tune, you must supply an appropriate rhythm (ABC's  _M_ header).

#### _URL Scheme_

URL path segments in italics represent fixed text; those in bold type are variable.  For example, the text 'genre' is fixed whilst **agenre** can be any of _irish, scandi, scottish or klezmer_. The following URLs access or maintain music:

*  GET / _musicrest_ / _genre_ - get a list of genres.

*  GET / _musicrest_ / _genre_ / **agenre** - get a list of rhythms appropriate for the genre.

*  GET / _musicrest_ / _genre_ / **agenre** / _tune_ - get a paged list of tunes.

*  POST / _musicrest_ / _genre_ / **agenre** / _tune_ - submit a tune in ABC format.

*  GET / _musicrest_ / _genre_ / **agenre** / _exists_ - return true if the genre exists

*  GET / _musicrest_ / _genre_ / **agenre** / _tune_ / **atune** - get a tune in the format suggested by the Accept header.

*  DELETE / _musicrest_ / _genre_  / **agenre** / _tune_ / **atune** - delete the tune from the database.

*  GET / _musicrest_ / _genre_ / **agenre** / _tune_ / **atune** / _exists_ - return true if the tune exists
 
*  GET / _musicrest_ / _genre_ / **agenre** / _tune_ / **atune** / _abc_ - get a tune in ABC (plain text) format

*  GET / _musicrest_ / _genre_ / **agenre** / _tune_ / **atune** / _html_ - get a tune in ABC html format

*  POST / _musicrest_ / _genre_ / **agenre** / _tune_ / **atune** / _abc_ - add an alternative title to the tune

*  GET / _musicrest_ / _genre_ / **agenre** / _tune_ / **atune** / _wav_ - get a tune in wav format

*  GET / _musicrest_ / _genre_ / **agenre** / _tune_ / **atune** / _ps_ - get a tune in Postscript format

*  GET / _musicrest_ / _genre_ / **agenre** / _tune_ / **atune**/ _pdf_ - get a tune in pdf format

*  GET / _musicrest_ / _genre_ / **agenre** / _tune_ / **atune** / _midi_ - get a tune in midi format

*  GET / _musicrest_ / _genre_ / _search_ - get a paged list of tunes that correspond to the search parameters.

*  POST / _musicrest_ / _genre_ / _transcode_ - submit a tune to temporary storage to check the validity of the ABC

A user must be logged in before he can submit or test-transcode a tune.  Only the original submitter or the *administrator* user is allowed to delete tunes. There is also a fairly conventional set of URLs for user maintenance.

##### URL Parameters

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

Linux is required for its transcoding services. MusicRest has been tested under Ubuntu 12.4. The following must be installed or present before the service can be run:

* a JVM (Java 1.6 or better)
* [MongoDB](http://www.mongodb.org/)  (2.0.4 or better)
* [AbcMidi](http://abc.sourceforge.net/abcMIDI/) (3.10 or better)
* [Abcm2ps](http://abcplus.sourceforge.net/#abcm2ps) (5.9.25 or better)
* [ImageMagick](http://www.imagemagick.org/script/index.php) (8:6.6.9.7-5ubuntu or better)
* [Timidity](https://wiki.archlinux.org/index.php/Timidity)  (2.13.2 or better)

The _scripts_ directory contains shell scripts that invoke the transcoding services.  You must be sure to make these executable  ( _chmod +x_ ) 

#### _Configuration_

This is by means of _musicrest.conf_ which must be supplied as a JVM -D startup parameter. The settings are largely self explanatory.  The cache directory for transcode indicates that the transcoding scripts are file-based and the results of such scripts are allowed to remain in the file cache for a time dependent on _cacheClearInterval_ (a time in minutes). Setting this to zero disables cache clearance. Get tune requests are served from the cache where this is possible. You probably need to clear the cache periodically because a large number of .wav files are generated.

Email is used simply to finish user registration or to remind them of passwords.  Email settings must of course be valid for the carrier in question.

    musicrest {
      server {
        host = "localhost"
        port = 8080
      }
      transcode {
        scriptDir = "scripts"
        cacheDir = "cache/main"
        cacheClearInterval = 60
      }
      database {
        host   = "localhost"
        port   = 27017
        dbName = "tunedb"
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
    }

#### _Web Front End_

There is no web front end included within this project.  However [tradtunedb](http://tradtunedb.org.uk/) uses musicrest as a backend service.

### _Build and Deploy_

#### _Source Code_

Code is written in scala and built with sbt.  Here are the major dependencies:
    
    "io.spray"            %   "spray-can"          % "1.2-M8",
    "io.spray"            %   "spray-routing"      % "1.2-M8",
    "io.spray"            %   "spray-caching"      % "1.2-M8",
    "io.spray"            %   "spray-testkit"      % "1.2-M8",
    "com.typesafe.akka"   %%  "akka-actor"         % "2.2.0-RC1",
    "com.typesafe.akka"   %%  "akka-testkit"       % "2.2.0-RC1",
    "org.scalaz"          %   "scalaz-core_2.10"   % "7.0.0",
    "org.mongodb"         %%  "casbah"             % "2.6.2",
    "net.liftweb"         %%  "lift-json"          % "2.5",
    "javax.mail"          %   "mail"               % "1.4",
    "org.specs2"          %%  "specs2"             % "1.14" % "test"

Spray uses logback for logging, but there is an unfortunate dependency on slf4j brought in by Casbah.

#### _Bootstrapping the Database_

Before MusicRest can be used, you need to set an administrator user and also set up the genres and rhythms the system recognizes.  These are held on the database and can be set up by means of the scripts _userinsert.sh_ and _genreinsert.sh_. There are also scripts to import tunes into or export tunes from the database in bulk.

#### _Build Instructions_

1. Install the runtime dependencies.  Start Mongo and configure MusicRest with the name of the Mongo database you wish to use

2. Launch sbt

3. _compile_ - compiles the source

4. _test_ - runs the tests (requires a valid Mongo connection)

5. _run_ and then choose the _Boot_ option to start the service.

Alternatively, you can build and then run a single jar assembly using:

1. sbt _assembly_

2. run.sh

### _Other Links_

*  [Blog](http://myelucubrations.blogspot.co.uk/)
*  [Spray](http://spray.io/documentation/1.2-M8/)

