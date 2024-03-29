/*******************************************************************************
 * Copyright (c) 2009, 2013 Mountainminds GmbH & Co. KG and Contributors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Brock Janiczak - initial API and implementation
 *
 *******************************************************************************/
package uk.co.codemonkey.jacoco;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.data.ExecutionDataReader;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.SessionInfoStore;
import org.jacoco.report.DirectorySourceFileLocator;
import org.jacoco.report.FileMultiReportOutput;
import org.jacoco.report.IReportVisitor;
import org.jacoco.report.html.HTMLFormatter;

/**
 * This example creates a HTML report for eclipse like projects based on a
 * single execution data store called jacoco.exec. The report contains no
 * grouping information.
 *
 * The class files under test must be compiled with debug information, otherwise
 * source highlighting will not work.
 */
public class ReportGenerator {

    private final String title;

    private final File executionDataFile;
    private final File classesDirectory;
    private final File sourceDirectory;
    private final File reportDirectory;

    private ExecutionDataStore executionDataStore;
    private SessionInfoStore sessionInfoStore;

    /**
     * Create a new generator based for the given project.
     *

     */
    public ReportGenerator(String buildDir, String jacocoFile, String ab) throws IOException {
        this.title = "Jacoco Test";
        this.executionDataFile = new File( buildDir, jacocoFile);
        this.classesDirectory = new File( buildDir + "/classes");
        this.sourceDirectory = new File( "src/main/java");

        this.reportDirectory = new File(buildDir + "/reports", ab);
        FileUtils.deleteDirectory(reportDirectory);
    }

    /**
     * Create the report.
     *
     * @throws IOException
     */
    public void create() throws IOException {

        // Read the jacoco.exec file. Multiple data stores could be merged
        // at this point
        loadExecutionData();

        // Run the structure analyzer on a single class folder to build up
        // the coverage model. The process would be similar if your classes
        // were in a jar file. Typically you would create a bundle for each
        // class folder and each jar you want in your report. If you have
        // more than one bundle you will need to add a grouping node to your
        // report
        final IBundleCoverage bundleCoverage = analyzeStructure();

        createReport(bundleCoverage);

    }

    private void createReport(final IBundleCoverage bundleCoverage)
            throws IOException {

        // Create a concrete report visitor based on some supplied
        // configuration. In this case we use the defaults
        final HTMLFormatter htmlFormatter = new HTMLFormatter();
        final IReportVisitor visitor = htmlFormatter
                .createVisitor(new FileMultiReportOutput(reportDirectory));

        // Initialize the report with all of the execution and session
        // information. At this point the report doesn't know about the
        // structure of the report being created
        visitor.visitInfo(sessionInfoStore.getInfos(),
                executionDataStore.getContents());

        // Populate the report structure with the bundle coverage information.
        // Call visitGroup if you need groups in your report.
        visitor.visitBundle(bundleCoverage, new DirectorySourceFileLocator(
                sourceDirectory, "utf-8", 4));

        // Signal end of structure information to allow report to write all
        // information out
        visitor.visitEnd();

    }

    private void loadExecutionData() throws IOException {
        final FileInputStream fis = new FileInputStream(executionDataFile);
        final ExecutionDataReader executionDataReader = new ExecutionDataReader(
                fis);
        executionDataStore = new ExecutionDataStore();
        sessionInfoStore = new SessionInfoStore();

        executionDataReader.setExecutionDataVisitor(executionDataStore);
        executionDataReader.setSessionInfoVisitor(sessionInfoStore);

        while (executionDataReader.read()) {
        }

        fis.close();
    }

    private IBundleCoverage analyzeStructure() throws IOException {
        final CoverageBuilder coverageBuilder = new CoverageBuilder();
        final Analyzer analyzer = new Analyzer(executionDataStore,
                coverageBuilder);

        analyzer.analyzeAll(classesDirectory);

        return coverageBuilder.getBundle(title);
    }


}
