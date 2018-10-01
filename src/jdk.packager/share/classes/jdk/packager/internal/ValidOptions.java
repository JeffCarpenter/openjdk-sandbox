/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
package jdk.packager.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import jdk.packager.internal.Arguments.CLIOptions;


public class ValidOptions {

    private ValidOptions() {};

    // multimap that contains pairs of (mode, supported args)
    private static final Map<CLIOptions, Set<CLIOptions>> options =
            new HashMap<>();

    private static boolean argsInitialized = false;

    // initializing list of mandatory arguments
    private static void initArgs() {
        if (argsInitialized) {
            return;
        }

        // add options for CREATE_IMAGE   
        add(CLIOptions.CREATE_IMAGE, CLIOptions.INPUT);
        add(CLIOptions.CREATE_IMAGE, CLIOptions.OUTPUT); 
        add(CLIOptions.CREATE_IMAGE, CLIOptions.APPCLASS);
        add(CLIOptions.CREATE_IMAGE, CLIOptions.SINGLETON);
        add(CLIOptions.CREATE_IMAGE, CLIOptions.NAME);
        add(CLIOptions.CREATE_IMAGE, CLIOptions.IDENTIFIER);
        add(CLIOptions.CREATE_IMAGE, CLIOptions.VERBOSE);
        add(CLIOptions.CREATE_IMAGE, CLIOptions.FILES);
        add(CLIOptions.CREATE_IMAGE, CLIOptions.ARGUMENTS);
        add(CLIOptions.CREATE_IMAGE, CLIOptions.STRIP_NATIVE_COMMANDS);
        add(CLIOptions.CREATE_IMAGE, CLIOptions.ICON);
        add(CLIOptions.CREATE_IMAGE, CLIOptions.VERSION);
        add(CLIOptions.CREATE_IMAGE, CLIOptions.JVM_ARGS);
        add(CLIOptions.CREATE_IMAGE, CLIOptions.SECONDARY_LAUNCHER);
        add(CLIOptions.CREATE_IMAGE, CLIOptions.BUILD_ROOT);
        add(CLIOptions.CREATE_IMAGE, CLIOptions.ECHO_MODE);
        add(CLIOptions.CREATE_IMAGE, CLIOptions.PREDEFINED_RUNTIME_IMAGE);
        add(CLIOptions.CREATE_IMAGE, CLIOptions.MAIN_JAR);
        add(CLIOptions.CREATE_IMAGE, CLIOptions.MODULE);
        add(CLIOptions.CREATE_IMAGE, CLIOptions.ADD_MODULES);
        add(CLIOptions.CREATE_IMAGE, CLIOptions.MODULE_PATH);
        add(CLIOptions.CREATE_IMAGE, CLIOptions.LIMIT_MODULES);

        if (Platform.getPlatform() == Platform.MAC) {
            add(CLIOptions.CREATE_IMAGE, CLIOptions.MAC_SIGN);
            add(CLIOptions.CREATE_IMAGE, CLIOptions.MAC_BUNDLE_NAME);
            add(CLIOptions.CREATE_IMAGE, CLIOptions.MAC_BUNDLE_IDENTIFIER);
            add(CLIOptions.CREATE_IMAGE, CLIOptions.MAC_BUNDLE_SIGNING_PREFIX);
            add(CLIOptions.CREATE_IMAGE, CLIOptions.MAC_SIGNING_KEY_NAME);
            add(CLIOptions.CREATE_IMAGE, CLIOptions.MAC_SIGNING_KEYCHAIN);
            add(CLIOptions.CREATE_IMAGE, CLIOptions.CATEGORY);
            add(CLIOptions.CREATE_IMAGE, CLIOptions.COPYRIGHT);
        }

        if (Platform.getPlatform() == Platform.WINDOWS) {
            add(CLIOptions.CREATE_IMAGE, CLIOptions.DESCRIPTION);
            add(CLIOptions.CREATE_IMAGE, CLIOptions.VENDOR);
            add(CLIOptions.CREATE_IMAGE, CLIOptions.COPYRIGHT);
        }

        // add options for CREATE_INSTALLER

        // add all CREATE_IMAGE options for CREATE_JRE_INSTALLER
        Set<CLIOptions> imageOptions = options.get(CLIOptions.CREATE_IMAGE);
        imageOptions.forEach(o -> add(CLIOptions.CREATE_INSTALLER, o));

        add(CLIOptions.CREATE_INSTALLER, CLIOptions.LICENSE_FILE);
        add(CLIOptions.CREATE_INSTALLER, CLIOptions.FILE_ASSOCIATIONS);
        add(CLIOptions.CREATE_INSTALLER, CLIOptions.INSTALL_DIR);
        add(CLIOptions.CREATE_INSTALLER, CLIOptions.PREDEFINED_APP_IMAGE);

        if (Platform.getPlatform() == Platform.MAC) {
            add(CLIOptions.CREATE_INSTALLER, CLIOptions.MAC_APP_STORE_CATEGORY);
            add(CLIOptions.CREATE_INSTALLER,
                    CLIOptions.MAC_APP_STORE_ENTITLEMENTS);
        }

        if (Platform.getPlatform() == Platform.LINUX) {
            add(CLIOptions.CREATE_INSTALLER, CLIOptions.LINUX_BUNDLE_NAME);
            add(CLIOptions.CREATE_INSTALLER, CLIOptions.LINUX_DEB_MAINTAINER);
            add(CLIOptions.CREATE_INSTALLER, CLIOptions.LINUX_RPM_LICENSE_TYPE);
            add(CLIOptions.CREATE_INSTALLER,
                    CLIOptions.LINUX_PACKAGE_DEPENDENCIES);
            add(CLIOptions.CREATE_INSTALLER, CLIOptions.DESCRIPTION);
            add(CLIOptions.CREATE_INSTALLER, CLIOptions.VENDOR);
            add(CLIOptions.CREATE_INSTALLER, CLIOptions.CATEGORY);
            add(CLIOptions.CREATE_INSTALLER, CLIOptions.COPYRIGHT);
        }

        if (Platform.getPlatform() == Platform.WINDOWS) {
            add(CLIOptions.CREATE_INSTALLER, CLIOptions.WIN_MENU_HINT);
            add(CLIOptions.CREATE_INSTALLER, CLIOptions.WIN_MENU_GROUP);
            add(CLIOptions.CREATE_INSTALLER, CLIOptions.WIN_SHORTCUT_HINT);
            add(CLIOptions.CREATE_INSTALLER,
                    CLIOptions.WIN_PER_USER_INSTALLATION);
            add(CLIOptions.CREATE_INSTALLER, CLIOptions.WIN_DIR_CHOOSER);
            add(CLIOptions.CREATE_INSTALLER, CLIOptions.WIN_REGISTRY_NAME);
            add(CLIOptions.CREATE_INSTALLER, CLIOptions.WIN_MSI_UPGRADE_UUID);
            add(CLIOptions.CREATE_INSTALLER, CLIOptions.CATEGORY);
        }

        // add options for CREATE_JRE_INSTALLER
        
        add(CLIOptions.CREATE_JRE_INSTALLER, CLIOptions.INPUT);
        add(CLIOptions.CREATE_JRE_INSTALLER, CLIOptions.OUTPUT); 
        add(CLIOptions.CREATE_JRE_INSTALLER, CLIOptions.NAME);
        add(CLIOptions.CREATE_JRE_INSTALLER, CLIOptions.VERBOSE);        
        add(CLIOptions.CREATE_JRE_INSTALLER, CLIOptions.FILES);
        add(CLIOptions.CREATE_JRE_INSTALLER, CLIOptions.STRIP_NATIVE_COMMANDS);
        add(CLIOptions.CREATE_JRE_INSTALLER, CLIOptions.LICENSE_FILE);
        add(CLIOptions.CREATE_JRE_INSTALLER, CLIOptions.VERSION);
        add(CLIOptions.CREATE_JRE_INSTALLER, CLIOptions.BUILD_ROOT);
        add(CLIOptions.CREATE_JRE_INSTALLER, CLIOptions.INSTALL_DIR);
        add(CLIOptions.CREATE_JRE_INSTALLER, CLIOptions.ECHO_MODE);
        add(CLIOptions.CREATE_JRE_INSTALLER,
                    CLIOptions.PREDEFINED_RUNTIME_IMAGE);
        add(CLIOptions.CREATE_JRE_INSTALLER, CLIOptions.ADD_MODULES); 
        add(CLIOptions.CREATE_JRE_INSTALLER, CLIOptions.MODULE_PATH); 
        add(CLIOptions.CREATE_JRE_INSTALLER, CLIOptions.LIMIT_MODULES);

        if (Platform.getPlatform() == Platform.MAC) {
            add(CLIOptions.CREATE_JRE_INSTALLER, CLIOptions.MAC_SIGN);
            add(CLIOptions.CREATE_JRE_INSTALLER, CLIOptions.MAC_BUNDLE_NAME);
            add(CLIOptions.CREATE_JRE_INSTALLER,
                    CLIOptions.MAC_BUNDLE_IDENTIFIER);
            add(CLIOptions.CREATE_JRE_INSTALLER,
                    CLIOptions.MAC_BUNDLE_SIGNING_PREFIX);
            add(CLIOptions.CREATE_JRE_INSTALLER,
                    CLIOptions.MAC_SIGNING_KEY_NAME);
            add(CLIOptions.CREATE_JRE_INSTALLER,
                    CLIOptions.MAC_SIGNING_KEYCHAIN);
        }

        if (Platform.getPlatform() == Platform.WINDOWS) {
            add(CLIOptions.CREATE_JRE_INSTALLER,
                    CLIOptions.WIN_PER_USER_INSTALLATION);
            add(CLIOptions.CREATE_JRE_INSTALLER, CLIOptions.WIN_DIR_CHOOSER);
            add(CLIOptions.CREATE_JRE_INSTALLER,
                    CLIOptions.WIN_MSI_UPGRADE_UUID);
            add(CLIOptions.CREATE_JRE_INSTALLER, CLIOptions.DESCRIPTION);
            add(CLIOptions.CREATE_JRE_INSTALLER, CLIOptions.VENDOR);
        }        

        if (Platform.getPlatform() == Platform.LINUX) {
            add(CLIOptions.CREATE_JRE_INSTALLER, CLIOptions.LINUX_BUNDLE_NAME);
            add(CLIOptions.CREATE_JRE_INSTALLER,
                    CLIOptions.LINUX_DEB_MAINTAINER); 
            add(CLIOptions.CREATE_JRE_INSTALLER,
                    CLIOptions.LINUX_PACKAGE_DEPENDENCIES);
            add(CLIOptions.CREATE_JRE_INSTALLER, CLIOptions.DESCRIPTION);
            add(CLIOptions.CREATE_JRE_INSTALLER, CLIOptions.VENDOR);
            add(CLIOptions.CREATE_JRE_INSTALLER,
                    CLIOptions.LINUX_RPM_LICENSE_TYPE);
        }

        argsInitialized = true;
    }

    public static void add(CLIOptions mode, CLIOptions arg) {
        if (mode.equals(arg)) {
            return;
        }
        options.computeIfAbsent(mode,
                    k -> new HashSet<>()).add(arg);
    }

    public static boolean checkIfSupported(CLIOptions mode, CLIOptions arg) {
        if (mode.equals(arg)) {
            return true;
        }

        initArgs();
        Set<CLIOptions> set = options.get(mode);
        if (set != null) {
            return set.contains(arg);
        }
        return false;
    }
}
