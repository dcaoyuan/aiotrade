
AIOTrade
========

### Requirement
    
* Java 1.7+ (Java 1.6 is not supported any more)
* JavaFX 2.2.3+
* Maven 2.x/3.x
* NetBeans 7.1+
* Scala 2.10.0 (Will be downloaded automatically by maven)

### Source code --- Git

    > mkdir aiotrade.git
    > cd aiotrade.git
    > git clone git://github.com/dcaoyuan/aiotrade.git
    > git clone git://github.com/dcaoyuan/circumflex.git
    > git clone git://github.com/dcaoyuan/configgy.git

### Directory Structure:
    +-- aiotrade.git
        +-- aiotrade
        +-- circumflex
        +-- configgy

### Build --- Maven
    Set environment variable:
    MAVEN_OPTS="-Xmx1024m -XX:MaxPermSize=256m"

    > cd aiotrade.git

    # Build opensource aiotrade client
    > cd ../aiotrade
    > mvn clean install  

### Code -> Build -> Run Cycle

    > cd ../aiotrade
    > mvn install  
    > suite/application/target/aiotrade/bin/aiotrade

### Zipped application package:

    ..../aiotrade.git/aiotrade/suite/application/target/platform-application-1.0-SNAPSHOT.zip
