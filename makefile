JAVA_HOME = /opt/jdk1.8.0_91 
 
JAVAS1=$(shell ls src/org/aventinus/usb/*.java) 
JAVAS2=$(shell ls src/org/aventinus/json/*.java) 
JAVAS3=$(shell ls src/org/aventinus/gui/*.java) 
JAVAS4=$(shell ls src/org/aventinus/database/*.java) 
JAVAS5=$(shell ls src/org/aventinus/util/*.java) 
JAVAS=$(JAVAS1) $(JAVAS2) $(JAVAS3) $(JAVAS4) $(JAVAS5)

JAR=jar/common.jar 
 
JPATH := ./lib/jna-platform-4.0.0.jar:./lib/jna-4.0.0.jar 
 
all: $(JAR) 

$(JAR): jar $(JAVAS) 
	rm -rf classes
	mkdir classes
	$(JAVA_HOME)/bin/javac -g -Xlint -d classes -classpath classes:$(JPATH) $(JAVAS) 
	$(JAVA_HOME)/bin/jar -cvf $@ -C classes .
 
jar: 
	mkdir jar 
