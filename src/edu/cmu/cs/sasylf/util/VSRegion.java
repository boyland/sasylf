package edu.cmu.cs.sasylf.util;

/**
 * This simulates the IRegion class in Eclipse. It represents a text region
 * within the document.
 */

public class VSRegion {
  private final int offset;
  private final int length;

  public VSRegion(int offset, int length) {
    this.offset = offset;
    this.length = length;
  }

  public int getOffset() { return offset; }

  public int getLength() { return length; }
}
