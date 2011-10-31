.SUFFIXES = .java .class
SHELL=bash
SOURCES	= $(wildcard *.java)
CLASSFILES	= $(SOURCES:.java=.class)

%.class: %.java
	javac -source 1.5 -g $<

all:	thisdir subdirs

thisdir:	$(CLASSFILES)

subdirs:
	cd parser && $(MAKE)
	cd ast && $(MAKE)
	cd term && $(MAKE)
	cd backend && $(MAKE)
	cd test && $(MAKE)

clean:
	rm *.class
	cd test && $(MAKE) clean
	cd parser && $(MAKE) clean
	cd term && $(MAKE) clean
	cd ast && $(MAKE) clean
	cd backend && $(MAKE) clean
