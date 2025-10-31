package lucee.runtime.exp;

import lucee.Info;
import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;

public class JavaxNeededException extends Exception {

	private static final long serialVersionUID = 9216442812616405905L;

	public JavaxNeededException(Throwable cause, String className) {
		super(createMessage(className), cause);
	}

	private static String createMessage(String className) {

		CFMLEngine eng = CFMLEngineFactory.getInstance();
		String major = "";
		if (eng != null) {
			Info info = eng.getInfo();
			if (info != null) {
				major = info.getVersion().getMajor() + "";
			}
		}

		// Missing javax classes - likely running on Jakarta/Tomcat 10+ with old extensions
		if (className.startsWith("javax")) {
			return "Lucee detected a missing javax class: " + className + "\n\n" + "This typically occurs when running Lucee " + major
					+ " on a Jakarta-based servlet container (for example Tomcat 10+) " + "with extensions that were compiled for javax.servlet APIs.\n\n"
					+ "SOLUTION: Update your extensions to Jakarta-compatible versions.\n"
					+ "Check for updated versions of your extensions (Redis, Lucene, etc.) that support Jakarta EE.";
		}

		// Missing jakarta classes - likely running on javax/Tomcat 9 without Jakarta APIs
		if (className.startsWith("jakarta")) {
			return "Lucee detected a missing jakarta class: " + className + "\n\n" + "Lucee " + major
					+ " requires Jakarta EE servlet APIs. This error occurs when running on a javax-based " + "servlet container (for example Tomcat 9 or earlier).\n\n"
					+ "RECOMMENDED SOLUTION: Upgrade to a Jakarta-based servlet container.\n" + "- Tomcat 10+ (recommended)\n" + "- Jetty 11+\n"
					+ "- Other Jakarta EE 9+ compatible containers\n\n" + "TEMPORARY WORKAROUND: Add Jakarta APIs to your current container.\n" + "This allows Lucee " + major
					+ " to run on older javax-based containers, but upgrading is the proper long-term solution.\n"
					+ "- Maven: https://mvnrepository.com/artifact/jakarta.servlet/jakarta.servlet-api\n" + "- Add the JAR to your servlet container's lib directory.";
		}

		// Fallback for other class loading issues
		return "Lucee encountered a class loading issue with: " + className + "\n\n" + "This may be related to the javax-to-jakarta migration in Lucee 7. " + "Please ensure:\n"
				+ "1. Your servlet container is compatible (Tomcat 9+ with Jakarta APIs, or Tomcat 10+)\n" + "2. All extensions are updated to Jakarta-compatible versions\n"
				+ "3. Required servlet APIs are in the classpath";
	}
}