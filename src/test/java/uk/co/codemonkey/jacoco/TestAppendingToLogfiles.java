package uk.co.codemonkey.jacoco;

import org.jacoco.agent.AgentJar;
import org.junit.Test;
import uk.co.codemonkey.jacoco.CoverageServer;
import uk.co.codemonkey.jacoco.ReportGenerator;

import java.io.*;

import static java.lang.System.getProperty;

/**
 *
 *
 */

public class TestAppendingToLogfiles {
    private static final String JACOCO_FILE = "jacocoServer.exec";
    private static final String BUILD_DIR = "build";

    @Test
    public void shouldAppendCoverageStatistics() throws IOException, InterruptedException {
        File jacocoFile = new File(BUILD_DIR, JACOCO_FILE);
        jacocoFile.delete();
        runTestPhase("MainA", jacocoFile, "report-Alpha");
        runTestPhase("MainB", jacocoFile, "report-Beta");
    }

    private void runTestPhase(String processClass, File coverageFile, String reportDir) throws IOException, InterruptedException {
        CoverageServer coverageServer = new CoverageServer(coverageFile);
        execute(processClass);
        new ReportGenerator(BUILD_DIR, JACOCO_FILE,reportDir).create();
        coverageServer.stop();
    }

    private void execute(String clazz) throws IOException, InterruptedException {
        File file = AgentJar.extractToTempLocation();
        file.deleteOnExit();

        ProcessBuilder command = new ProcessBuilder()
                .command("java", "-cp", getProperty("java.class.path"),
                        "-javaagent:" + file.getCanonicalPath() + "=output=tcpclient,address=localhost,port=9092",
                        "uk.co.codemonkey.example." + clazz);

        Process start = command.start();
        start.waitFor();

        printStream(start.getErrorStream());
        printStream(start.getInputStream());
    }

    private void printStream(InputStream inputStream) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
        String inputLine;
        while ((inputLine = in.readLine()) != null)
            System.out.println(inputLine);
        in.close();
    }
}
