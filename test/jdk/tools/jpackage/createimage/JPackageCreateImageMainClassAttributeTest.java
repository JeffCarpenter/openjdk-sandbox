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

/*
 * @test
 * @summary jpackage create image with no main class arguments and with main-class attribute
 * @library ../helpers
 * @build JPackageHelper
 * @build JPackagePath
 * @modules jdk.jpackage
 * @run main/othervm -Xmx512m JPackageCreateImageMainClassAttributeTest
 */
public class JPackageCreateImageMainClassAttributeTest {
    private static final String app = JPackagePath.getApp();
    private static final String appOutput = JPackagePath.getAppOutputFile();
    private static final String appWorkingDir = JPackagePath.getAppWorkingDir();

    private static final String[] CMD = {
        "create-image",
        "--input", "input",
        "--output", "output",
        "--name", "test",
        "--main-jar", "hello.jar",
        "--force",
        "--files", "hello.jar"};

    private static void validateResult(String[] result) throws Exception {
        if (result.length != 2) {
            throw new AssertionError(
                   "Unexpected number of lines: " + result.length);
        }

        if (!result[0].trim().equals("jpackage test application")) {
            throw new AssertionError("Unexpected result[0]: " + result[0]);
        }

        if (!result[1].trim().equals("args.length: 0")) {
            throw new AssertionError("Unexpected result[1]: " + result[1]);
        }
    }

    private static void validate() throws Exception {
        int retVal = JPackageHelper.execute(null, app);
        if (retVal != 0) {
            throw new AssertionError(
                   "Test application exited with error: " + retVal);
        }

        File outfile = new File(appWorkingDir + File.separator + appOutput);
        if (!outfile.exists()) {
            throw new AssertionError(appOutput + " was not created");
        }

        String output = Files.readString(outfile.toPath());
        String[] result = output.split("\n");
        validateResult(result);
    }

    private static void testMainClassAttribute() throws Exception {
        JPackageHelper.executeCLI(true, CMD);
        validate();
    }

    private static void testMainClassAttributeToolProvider() throws Exception {
        JPackageHelper.executeToolProvider(true, CMD);
        validate();
    }

    public static void main(String[] args) throws Exception {
        JPackageHelper.createHelloImageJarWithMainClass();
        testMainClassAttribute();
        testMainClassAttributeToolProvider();
    }

}
