package edu.cmu.cs.sasylf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Obtain the current SASyLF version.
 */
public class Version {

  private final String versionString;
  
  private Version() {
    versionString = Version.getVersionString();
  }
  
  @Override
  public String toString() { return versionString; }
  
  /**
   * Compute the version string by trying to find the file "README.TXT"
   * and reading the first line. 
   * @return
   */
  private static String getVersionString() {
    String version = "SASyLF version ???";
    // Rather than include the version here, we look for README.TXT
    // This is rather more complex than I hoped; if we are packed up in a JAR,
    // it's easy to find, but otherwise, we have to go hunting.
    InputStream s = Main.class.getClassLoader().getResourceAsStream("README.TXT");
    if (s == null) {
      URL execdir = Main.class.getClassLoader().getResource(".");
      URI uri;
      try {
        uri = execdir.toURI();
      } catch (URISyntaxException e) {
        // muffle exception
        return version;
      }
      if (uri.getScheme().equals("file")) {
        File dir = new File(uri.getPath());
        File rfile = new File(dir.getParentFile(),"README.TXT");
        try {
          s = new FileInputStream(rfile);
        } catch (FileNotFoundException ex) {
          // muffle exception
          return version;
        }
      }
    }
    try {
      BufferedReader br = new BufferedReader(new InputStreamReader(s));
      version = br.readLine();
    } catch (IOException e) {
      // muffle exception
    }
    return version;
  }

  private static Version instance = null;
  
  /**
   * Get the current version, which can then be printed/displayed.
   * @return current version (never null)
   */
  public static Version getInstance() {
    synchronized (Version.class) {
      if (instance == null) {
        instance = new Version();
      }
    }
    return instance;
  }
}
