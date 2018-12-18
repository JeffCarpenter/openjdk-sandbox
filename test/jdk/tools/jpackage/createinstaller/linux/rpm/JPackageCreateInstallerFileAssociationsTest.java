/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @summary jpackage create installer test
 * @library ../../../helpers
 * @library ../base
 * @build JPackageHelper
 * @build JPackagePath
 * @build JPackageInstallerHelper
 * @build JPackageCreateInstallerFileAssociationsBase
 * @requires (os.family == "linux")
 * @modules jdk.jpackage
 * @run main/othervm -Xmx512m JPackageCreateInstallerFileAssociationsTest
 */
public class JPackageCreateInstallerFileAssociationsTest {
    private static final String TEST_NAME = "JPackageCreateInstallerFileAssociationsTest";
    private static final String EXT = "rpm";

    public static void main(String[] args) throws Exception {
        JPackageCreateInstallerFileAssociationsBase.run(TEST_NAME, EXT);
    }
}
