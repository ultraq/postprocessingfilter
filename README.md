
Post-Processing Filter
======================

Common code for all resource processing filters.  Has objects, an abstract
filter, and a resource response wrapper to help manage, track, and cache
post-processed resources in a Java web application.


Requirements
------------

 - Java 7
 - A Servlet 3.0 compliant servlet container if you're taking advantage of
   servlet 3.0 annotations, otherwise a Servlet 2.5 compliant servlet container


Installation
------------

### Standalone distribution
1. Download a copy of of the pre-compiled JAR from [the Downloads section](post-processing-filter/downloads)
   or build the project from the source code here on GitHub.
2. Place the JAR in the `WEB-INF/lib` directory of your web application.

### For Maven and Maven-compatible dependency managers
Add a dependency to your project with the following co-ordinates:

 - GroupId: `nz.net.ultraq.web.filter`
 - ArtifactId: `post-processing-filter`
 - Version: `1.0.2`


Usage
-----

Extend the `ResourceProcessingFilter` filter (and optionally, the `Resource`
class) and implement the abstract methods.  Existing projects that make use of
this filter are the [LessCSS Filter](lesscss-filter) and [YUI Compressor Filter](yuicompressor-filter),
so look there for examples of how to use this project.


Changelog
---------

### 1.0.2
 - Set scope of javax.servlet-api dependency to 'provided'.
 - Minor fixes from the updated [maven-support](https://github.com/ultraq/gradle-support)
   Gradle script.

### 1.0.1
 - Added Gradle for builds and creating Maven artifacts.
 - Made project available from Maven Central.  Maven co-ordinates added to the
   [Installation](#installation) section.

### 1.0
 - Initial release.
