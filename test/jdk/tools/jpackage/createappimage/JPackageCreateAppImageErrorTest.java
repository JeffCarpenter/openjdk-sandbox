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

 /*
 * @test
 * @summary jpackage create app image error test
 * @library ../helpers
 * @build JPackageHelper
 * @build JPackagePath
 * @build JPackageCreateAppImageBase
 * @modules jdk.jpackage
 * @run main/othervm -Xmx512m JPackageCreateAppImageErrorTest
 */
import java.util.*;
import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.nio.file.attribute.*;

public class JPackageCreateAppImageErrorTest {

    private static final String OUTPUT = "output";

    private static final String ARG1 = "--no-such-argument";
    private static final String EXPECTED1 =
            "Invalid Option: [--no-such-argument]";
    private static final String ARG2 = "--output";
    private static final String EXPECTED2 = "Mode is not specified";

    private static final String [] CMD1 = {
        "create-app-image",
        "--input", "input",
        "--output", OUTPUT,
        "--name", "test",
        "--main-jar", "non-existant.jar",
    };
    private static final String EXP1 = "main jar does not exist";

    private static final String [] CMD2 = {
        "create-app-image",
        "--input", "input",
        "--output", OUTPUT,
        "--name", "test",
        "--main-jar", "hello.jar",
    };
    private static final String EXP2 = "class was not specified nor was";

    private static void validate(String output, String expected, boolean single)
            throws Exception {
        String[] result = output.split("\n");
        if (single && result.length != 1) {
            System.err.println(output);
            throw new AssertionError("Unexpected multiple lines of output: "
                    + output);
        }

        if (!result[0].trim().contains(expected)) {
            throw new AssertionError("Unexpected output: " + result[0]
                    + " - expected output to contain: " + expected);
        }
    }


    public static void main(String[] args) throws Exception {
        JPackageHelper.createHelloImageJar();

        validate(JPackageHelper.executeToolProvider(false, ARG1), EXPECTED1, true);
        validate(JPackageHelper.executeToolProvider(false, ARG2), EXPECTED2, true);

        JPackageHelper.deleteOutputFolder(OUTPUT);
        validate(JPackageHelper.executeToolProvider(false, CMD1), EXP1, false);

        JPackageHelper.deleteOutputFolder(OUTPUT);
        validate(JPackageHelper.executeToolProvider(false, CMD2), EXP2, false);

    }

}
