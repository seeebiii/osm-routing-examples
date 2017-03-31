#!/usr/bin/env bash
# -----------------------------------------------
# This is an example start script to compile and
# run the osm-routing-examples project. It was
# written to work on Amazon Linux instances
# which is based on Red Hat Enterprise Linux and
# CentOS. This script is NOT designed to work
# out-of-the-box, because files like the
# Location Code List and Event Code List need to
# be provided as well (this is not done here!).
# It is more a collection of commands to avoid
# using Google every time as I'm no Linux expert.
#
# (C) 2017 Sebastian Hesse
# www.sebastianhesse.de
# -----------------------------------------------


### PREPARATION
# system requirements: Java, Git, Maven, OSM file

# install java
sudo yum install java-1.8.0-openjdk*
# set JAVA_HOME to jdk 8
export JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk

# install git
sudo yum install git-all

# install maven
wget http://mirror.netcologne.de/apache.org/maven/maven-3/3.3.9/binaries/apache-maven-3.3.9-bin.tar.gz
tar xzvf apache-maven-3.3.9-bin.tar.gz
# add maven to PATH
export PATH=/path/to/apache-maven-3.3.9/bin:$PATH

# download OSM file for Germany
wget http://download.geofabrik.de/europe/germany-latest.osm.pbf


## BUILD
git clone https://github.com/seeebiii/osm-routing-examples
cd osm-routing-examples
mvn clean install -DskipTests


## START SERVER
# without TMC
sudo java -Xmx26g  -jar target/osm-routing.jar server conf.yml germany-latest.osm.pbf > output.log 2>&1 &
# with TMC
sudo java -Xmx26g  -jar target/osm-routing.jar server conf.yml germany-latest.osm.pbf ~/lcl.csv ~/event_list.csv ~/cdat_files  > output.log 2>&1 &
