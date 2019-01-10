/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class JPackageCreateInstallerPackageDepsBase {

    private static String TEST_NAME;
    private static String DEP_NAME;
    private static String EXT;
    private static String OUTPUT;
    private static String OUTPUT_DEP;
    private static String[] CMD;
    private static String[] CMD_DEP;

    private static void copyResults() throws Exception {
        List<String> files = new ArrayList<>();
        files.add(OUTPUT.toLowerCase());
        files.add(OUTPUT_DEP.toLowerCase());
        JPackageInstallerHelper.copyTestResults(files);
    }

    private static final String infoResult = "infoResult.txt";
    private static void validatePackage() throws Exception {
        if (EXT.equals("rpm")) {
            int retVal = JPackageHelper.execute(new File(infoResult),"rpm",
                    "--query", "--package", "--requires", OUTPUT.toLowerCase());
            if (retVal != 0) {
                throw new AssertionError("rpm exited with error: " + retVal);
            }
        } else {
            int retVal = JPackageHelper.execute(new File(infoResult), "dpkg",
                    "--info", OUTPUT.toLowerCase());
            if (retVal != 0) {
                throw new AssertionError("dpkg exited with error: " + retVal);
            }
        }

        File outfile = new File(infoResult);
        if (!outfile.exists()) {
            throw new AssertionError(infoResult + " was not created");
        }

        String output = Files.readString(outfile.toPath());
        if (!output.contains(DEP_NAME.toLowerCase())) {
            throw new AssertionError("Unexpected result: " + output);
        }
    }

    private static void testCreateInstaller() throws Exception {
        JPackageHelper.executeCLI(true, CMD);
        JPackageHelper.executeCLI(true, CMD_DEP);
        JPackageInstallerHelper.validateOutput(OUTPUT);
        JPackageInstallerHelper.validateOutput(OUTPUT_DEP);
        validatePackage();
        copyResults();
    }

    private static void verifyInstall() throws Exception {
        String app = JPackagePath.getLinuxInstalledApp(TEST_NAME);
        JPackageInstallerHelper.validateApp(app);

        app = JPackagePath.getLinuxInstalledApp(DEP_NAME);
        JPackageInstallerHelper.validateApp(app);
    }

    private static void verifyUnInstall() throws Exception {
        String folderPath = JPackagePath.getLinuxInstallFolder(TEST_NAME);
        File folder = new File(folderPath);
        if (folder.exists()) {
            throw new AssertionError("Error: " + folder.getAbsolutePath() + " exist");
        }

        folderPath = JPackagePath.getLinuxInstallFolder(DEP_NAME);
        folder = new File(folderPath);
        if (folder.exists()) {
            throw new AssertionError("Error: " + folder.getAbsolutePath() + " exist");
        }
    }

    private static void init(String name, String ext) {
        TEST_NAME = name;
        DEP_NAME = name + "Dep";
        EXT = ext;
        if (EXT.equals("rpm")) {
            OUTPUT = "output" + File.separator + TEST_NAME + "-1.0-1.x86_64." + EXT;
            OUTPUT_DEP = "output" + File.separator + DEP_NAME + "-1.0-1.x86_64." + EXT;
        } else {
            OUTPUT = "output" + File.separator + TEST_NAME + "-1.0." + EXT;
            OUTPUT_DEP = "output" + File.separator + DEP_NAME + "-1.0." + EXT;
        }
        CMD = new String[]{
            "create-installer",
            EXT,
            "--input", "input",
            "--output", "output",
            "--name", TEST_NAME,
            "--main-jar", "hello.jar",
            "--class", "Hello",
            "--force",
            "--files", "hello.jar",
            "--linux-package-deps", DEP_NAME.toLowerCase()};
        CMD_DEP = new String[]{
            "create-installer",
            EXT,
            "--input", "input",
            "--output", "output",
            "--name", DEP_NAME,
            "--main-jar", "hello.jar",
            "--class", "Hello",
            "--force",
            "--files", "hello.jar"};
    }

    public static void run(String name, String ext) throws Exception {
        init(name, ext);

        if (JPackageInstallerHelper.isVerifyInstall()) {
            verifyInstall();
        } else if (JPackageInstallerHelper.isVerifyUnInstall()) {
            verifyUnInstall();
        } else {
            JPackageHelper.createHelloInstallerJar();
            testCreateInstaller();
        }
    }
}
