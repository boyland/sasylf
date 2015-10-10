# Installation #

You can get SASyLF in one of two downloads: the whole zip file or just the Eclipse plugin.
Both are available on the downloads page:
https://sourceforge.net/projects/sasylf/files/Releases/
  * `SASyLF-`_version_`.zip` This includes the source and the SASyLF JAR file (see below)
  * `org.sasylf_`_version_`.v`_date_`.jar` The Eclipse plugin JAR file
You may also get the project directly out of SVN to get the latest bleeding edge version.
In this case, you will need to use the `Makefile` to produce the JAR files.
The SVN project is a legal Eclipse plugin project; you will need to use Eclipse to help build the Eclipse plugin JAR.

To use the Eclipse plugin, you need Eclipse, version 4.2.2 (Juno) or later.
See http://www.eclipse.org for how to download this free software.
Many people find the plethora of download possibilities confusing.
See http://www.eclipse.org/downloads/moreinfo/compare.php
for a comparison.  If you're just interested in SASyLF and maybe Java programming, get the "Eclipse IDE for Java Developers".

Previously, through SASyLF 1.2.3, you would also need to install the "Graphical Editing Framework GEF SDK" plugin before using SASyLF.  Use the Help>Install New Software dialog, and use the "Juno - http://downloads.eclipse.org/releases/juno" site (or the equivalent for kepler, luna etc...).  The GEF is under category "Modeling".
Starting with release 1.2.4, the SASyLF plugin no longer depends on GEF.

The Eclipse plugin JAR file should be placed in the `dropins/` directory of your Eclipse installation.
Then restart Eclipse.
Once this is done, you can create files with extension `.slf` and SASyLF>Check Proofs
to check proofs.  Proofs are also checked when the file is saved.

The zip file includes the following parts:
  * `README.TXT` Release notes
  * `SASyLF.jar` JAR file needed for command-line checking of proofs.
  * `sasylf` A shell script for Linux/MacOSX command-line checking of SASyLF files.
  * `sasylf.bat` A WIndows script for the same purpose.
  * `examples` A directory of example proofs written in SASyLF
  * `src` The complete source
  * other files / test cases / exercises
The JAR files are all that are needed to run SASyLF.  Indeed the Eclipse plugin JAR includes the capabilities of `SASyLF.jar` and so you only need that JAR file (but it is larger).

See `README.TXT` for more information.