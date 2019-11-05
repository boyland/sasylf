package edu.cmu.cs.sasylf.util;

/**
 * To avoid making SASyLF depend on JUnit,
 * I include a simple replacement.
 */
public abstract class SimpleTestSuite {

	public SimpleTestSuite() { }

	private int passed, failed, mark;

	protected void assertTrue(String message, boolean x) {
		if (x) ++passed;
		else {
			++failed;
			System.err.println("Failed test: " + message);
		}
	}

	protected <T> void assertEqual(String description, T expected, T given) {
		assertTrue(description + ": expected " + expected + ", but got " + given, 
				expected == given || expected != null && expected.equals(given));
	}

	public final void run() {
		try {
			runTests();
		} catch (RuntimeException ex) {
			assertTrue("got exception " + ex.getMessage(),false);
			ex.printStackTrace();
		} finally {
			System.out.println("Tests passed: " + passed);
			System.out.println("Tests failed: " + failed);
		}
	}

	protected abstract void runTests();

	protected void mark() {
		mark = failed;
	}

	protected boolean failedSinceMark() {
		return failed > mark;
	}
}
