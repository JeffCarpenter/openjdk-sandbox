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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.io.File;
import jdk.packager.internal.Arguments.CLIOptions;

public class SecondaryLauncherArguments {

    private final String filename;
    private Map<String, String> allArgs;
    private Map<String, ? super Object> bundleParams;

    public SecondaryLauncherArguments(String filename) {
        this.filename = filename;
    }

    private void initLauncherMap() {
        if (bundleParams != null) {
            return;
        }
        
        allArgs = Arguments.getPropertiesFromFile(filename);
        
        bundleParams = new HashMap<>();
        String mainClass = getOptionValue(CLIOptions.APPCLASS);
        String module = getOptionValue(CLIOptions.MODULE);

        if (module != null && mainClass != null) {
            putUnlessNull(bundleParams, Arguments.CLIOptions.MODULE.getId(),
                    module + "/" + mainClass);
        } else if (module != null) {
            putUnlessNull(bundleParams, Arguments.CLIOptions.MODULE.getId(),
                    module);
        } else if (mainClass != null) {
            putUnlessNull(bundleParams, Arguments.CLIOptions.APPCLASS.getId(),
                    mainClass); 
        }
        
        putUnlessNull(bundleParams, Arguments.CLIOptions.NAME.getId(),
                    getOptionValue(CLIOptions.NAME));
        putUnlessNull(bundleParams, Arguments.CLIOptions.VERSION.getId(),
                    getOptionValue(CLIOptions.VERSION));
        
        // 3 boolean values:
        putUnlessNull(bundleParams, Arguments.CLIOptions.WIN_MENU_HINT.getId(),
            getOptionValue(CLIOptions.WIN_MENU_HINT));
        putUnlessNull(bundleParams, Arguments.CLIOptions.WIN_SHORTCUT_HINT.getId(),
            getOptionValue(CLIOptions.WIN_SHORTCUT_HINT));
        putUnlessNull(bundleParams, Arguments.CLIOptions.SINGLETON.getId(),
            getOptionValue(CLIOptions.SINGLETON));

        String value = getOptionValue(CLIOptions.ICON);
        putUnlessNull(bundleParams, Arguments.CLIOptions.ICON.getId(),
                    (value == null) ? null : new File(value));
        
        String argumentStr = getOptionValue(CLIOptions.ARGUMENTS);
        putUnlessNullOrEmpty(bundleParams,
                CLIOptions.ARGUMENTS.getId(),
                Arguments.getArgumentList(argumentStr));

        String jvmargsStr = getOptionValue(CLIOptions.JVM_ARGS);
        putUnlessNullOrEmpty(bundleParams,
                CLIOptions.JVM_ARGS.getId(),
                Arguments.getArgumentList(jvmargsStr));

        String jvmUserArgsStr = getOptionValue(CLIOptions.USER_JVM_ARGS);
        putUnlessNullOrEmpty(bundleParams,
                CLIOptions.USER_JVM_ARGS.getId(),
                Arguments.getArgumentMap(jvmUserArgsStr));
    }

    private String getOptionValue(CLIOptions option) {
        if (option == null || allArgs == null) {
            return null;
        }

        String id = option.getId();

        if (allArgs.containsKey(id)) {
            return allArgs.get(id);
        }

        return null;
    }

    public Map<String, ? super Object> getLauncherMap() {
        initLauncherMap();
        return bundleParams;
    }

    private void putUnlessNull(Map<String, ? super Object> params,
            String param, Object value) {
        if (value != null) {
            params.put(param, value);
        }
    }

    private void putUnlessNullOrEmpty(Map<String, ? super Object> params,
            String param, Collection value) {
        if (value != null && !value.isEmpty()) {
            params.put(param, value);
        }
    }

    private void putUnlessNullOrEmpty(Map<String, ? super Object> params,
            String param, Map value) {
        if (value != null && !value.isEmpty()) {
            params.put(param, value);
        }
    }
}
