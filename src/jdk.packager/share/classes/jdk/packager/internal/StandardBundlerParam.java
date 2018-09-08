/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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

import jdk.packager.internal.bundlers.BundleParams;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.HashSet;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import jdk.packager.internal.builders.AbstractAppImageBuilder;

public class StandardBundlerParam<T> extends BundlerParamInfo<T> {

    private static final ResourceBundle I18N = ResourceBundle.getBundle(
            "jdk.packager.internal.resources.StandardBundlerParam");
    private static final String JAVABASEJMOD = "java.base.jmod";

    public StandardBundlerParam(String name, String description, String id,
            Class<T> valueType,
            Function<Map<String, ? super Object>, T> defaultValueFunction,
            BiFunction<String, Map<String, ? super Object>, T> stringConverter)
    {
        this.name = name;
        this.description = description;
        this.id = id;
        this.valueType = valueType;
        this.defaultValueFunction = defaultValueFunction;
        this.stringConverter = stringConverter;
    }

    public static final StandardBundlerParam<RelativeFileSet> APP_RESOURCES =
            new StandardBundlerParam<>(
                    I18N.getString("param.app-resources.name"),
                    I18N.getString("param.app-resource.description"),
                    BundleParams.PARAM_APP_RESOURCES,
                    RelativeFileSet.class,
                    null, // no default.  Required parameter
                    null  // no string translation,
                          // tool must provide complex type
            );

    @SuppressWarnings("unchecked")
    public static final
            StandardBundlerParam<List<RelativeFileSet>> APP_RESOURCES_LIST =
            new StandardBundlerParam<>(
                    I18N.getString("param.app-resources-list.name"),
                    I18N.getString("param.app-resource-list.description"),
                    BundleParams.PARAM_APP_RESOURCES + "List",
                    (Class<List<RelativeFileSet>>) (Object) List.class,
                    // Default is appResources, as a single item list
                    p -> new ArrayList<>(Collections.singletonList(
                            APP_RESOURCES.fetchFrom(p))),
                    StandardBundlerParam::createAppResourcesListFromString
            );

    @SuppressWarnings("unchecked")
    public static final StandardBundlerParam<String> SOURCE_DIR =
            new StandardBundlerParam<>(
                    I18N.getString("param.source-dir.name"),
                    I18N.getString("param.source-dir.description"),
                    Arguments.CLIOptions.INPUT.getId(),
                    String.class,
                    p -> null,
                    (s, p) -> {
                        String value = String.valueOf(s);
                        if (value.charAt(value.length() - 1) ==
                                File.separatorChar) {
                            return value.substring(0, value.length() - 1);
                        }
                        else {
                            return value;
                        }
                    }
            );

    public static final StandardBundlerParam<List<File>> SOURCE_FILES =
            new StandardBundlerParam<>(
                    I18N.getString("param.source-files.name"),
                    I18N.getString("param.source-files.description"),
                    Arguments.CLIOptions.FILES.getId(),
                    (Class<List<File>>) (Object) List.class,
                    p -> null,
                    (s, p) -> null
            );

    // note that each bundler is likely to replace this one with
    // their own converter
    public static final StandardBundlerParam<RelativeFileSet> MAIN_JAR =
            new StandardBundlerParam<>(
                    I18N.getString("param.main-jar.name"),
                    I18N.getString("param.main-jar.description"),
                    Arguments.CLIOptions.MAIN_JAR.getId(),
                    RelativeFileSet.class,
                    params -> {
                        extractMainClassInfoFromAppResources(params);
                        return (RelativeFileSet) params.get("mainJar");
                    },
                    (s, p) -> getMainJar(s, p)
            );

    // TODO: remove it
    public static final StandardBundlerParam<String> CLASSPATH =
            new StandardBundlerParam<>(
                    I18N.getString("param.classpath.name"),
                    I18N.getString("param.classpath.description"),
                    "classpath",
                    String.class,
                    params -> {
                        extractMainClassInfoFromAppResources(params);
                        String cp = (String) params.get("classpath");
                        return cp == null ? "" : cp;
                    },
                    (s, p) -> s.replace(File.pathSeparator, " ")
            );

    public static final StandardBundlerParam<String> MAIN_CLASS =
            new StandardBundlerParam<>(
                    I18N.getString("param.main-class.name"),
                    I18N.getString("param.main-class.description"),
                    Arguments.CLIOptions.APPCLASS.getId(),
                    String.class,
                    params -> {
                        if (Arguments.CREATE_JRE_INSTALLER.fetchFrom(params)) {
                            return null;
                        } 
                        extractMainClassInfoFromAppResources(params);
                        String s = (String) params.get(
                                BundleParams.PARAM_APPLICATION_CLASS);
                        if (s == null) {
                            s = JLinkBundlerHelper.getMainClass(params);
                        }
                        return s;
                    },
                    (s, p) -> s
            );

    public static final StandardBundlerParam<String> APP_NAME =
            new StandardBundlerParam<>(
                    I18N.getString("param.app-name.name"),
                    I18N.getString("param.app-name.description"),
                    Arguments.CLIOptions.NAME.getId(),
                    String.class,
                    params -> {
                        String s = MAIN_CLASS.fetchFrom(params);
                        if (s == null) return null;

                        int idx = s.lastIndexOf(".");
                        if (idx >= 0) {
                            return s.substring(idx+1);
                        }
                        return s;
                    },
                    (s, p) -> s
            );

    private static Pattern TO_FS_NAME = Pattern.compile("\\s|[\\\\/?:*<>|]");
            // keep out invalid/undesireable filename characters

    public static final StandardBundlerParam<String> APP_FS_NAME =
            new StandardBundlerParam<>(
                    I18N.getString("param.app-fs-name.name"),
                    I18N.getString("param.app-fs-name.description"),
                    "name.fs",
                    String.class,
                    params -> TO_FS_NAME.matcher(
                            APP_NAME.fetchFrom(params)).replaceAll(""),
                    (s, p) -> s
            );

    public static final StandardBundlerParam<File> ICON =
            new StandardBundlerParam<>(
                    I18N.getString("param.icon-file.name"),
                    I18N.getString("param.icon-file.description"),
                    Arguments.CLIOptions.ICON.getId(),
                    File.class,
                    params -> null,
                    (s, p) -> new File(s)
            );

    public static final StandardBundlerParam<String> VENDOR =
            new StandardBundlerParam<>(
                    I18N.getString("param.vendor.name"),
                    I18N.getString("param.vendor.description"),
                    Arguments.CLIOptions.VENDOR.getId(),
                    String.class,
                    params -> I18N.getString("param.vendor.default"),
                    (s, p) -> s
            );

    public static final StandardBundlerParam<String> CATEGORY =
            new StandardBundlerParam<>(
                    I18N.getString("param.category.name"),
                    I18N.getString("param.category.description"),
                   Arguments.CLIOptions.CATEGORY.getId(),
                    String.class,
                    params -> I18N.getString("param.category.default"),
                    (s, p) -> s
            );

    public static final StandardBundlerParam<String> DESCRIPTION =
            new StandardBundlerParam<>(
                    I18N.getString("param.description.name"),
                    I18N.getString("param.description.description"),
                    Arguments.CLIOptions.DESCRIPTION.getId(),
                    String.class,
                    params -> params.containsKey(APP_NAME.getID())
                            ? APP_NAME.fetchFrom(params)
                            : I18N.getString("param.description.default"),
                    (s, p) -> s
            );

    public static final StandardBundlerParam<String> COPYRIGHT =
            new StandardBundlerParam<>(
                    I18N.getString("param.copyright.name"),
                    I18N.getString("param.copyright.description"),
                    Arguments.CLIOptions.COPYRIGHT.getId(),
                    String.class,
                    params -> MessageFormat.format(I18N.getString(
                            "param.copyright.default"), new Date()),
                    (s, p) -> s
            );

    @SuppressWarnings("unchecked")
    public static final StandardBundlerParam<List<String>> ARGUMENTS =
            new StandardBundlerParam<>(
                    I18N.getString("param.arguments.name"),
                    I18N.getString("param.arguments.description"),
                    Arguments.CLIOptions.ARGUMENTS.getId(),
                    (Class<List<String>>) (Object) List.class,
                    params -> Collections.emptyList(),
                    (s, p) -> splitStringWithEscapes(s)
            );

    @SuppressWarnings("unchecked")
    public static final StandardBundlerParam<List<String>> JVM_OPTIONS =
            new StandardBundlerParam<>(
                    I18N.getString("param.jvm-options.name"),
                    I18N.getString("param.jvm-options.description"),
                    Arguments.CLIOptions.JVM_ARGS.getId(),
                    (Class<List<String>>) (Object) List.class,
                    params -> Collections.emptyList(),
                    (s, p) -> Arrays.asList(s.split("\n\n"))
            );

    @SuppressWarnings("unchecked")
    public static final
            StandardBundlerParam<Map<String, String>> JVM_PROPERTIES =
            new StandardBundlerParam<>(
                    I18N.getString("param.jvm-system-properties.name"),
                    I18N.getString("param.jvm-system-properties.description"),
                    "jvmProperties",
                    (Class<Map<String, String>>) (Object) Map.class,
                    params -> Collections.emptyMap(),
                    (s, params) -> {
                        Map<String, String> map = new HashMap<>();
                        try {
                            Properties p = new Properties();
                            p.load(new StringReader(s));
                            for (Map.Entry<Object,
                                    Object> entry : p.entrySet()) {
                                map.put((String)entry.getKey(),
                                        (String)entry.getValue());
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return map;
                    }
            );

    @SuppressWarnings("unchecked")
    public static final
            StandardBundlerParam<Map<String, String>> USER_JVM_OPTIONS =
            new StandardBundlerParam<>(
                    I18N.getString("param.user-jvm-options.name"),
                    I18N.getString("param.user-jvm-options.description"),
                    Arguments.CLIOptions.USER_JVM_ARGS.getId(),
                    (Class<Map<String, String>>) (Object) Map.class,
                    params -> Collections.emptyMap(),
                    (s, params) -> {
                        Map<String, String> map = new HashMap<>();
                        try {
                            Properties p = new Properties();
                            p.load(new StringReader(s));
                            for (Map.Entry<Object, Object> entry :
                                    p.entrySet()) {
                                map.put((String)entry.getKey(),
                                        (String)entry.getValue());
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return map;
                    }
            );

    public static final StandardBundlerParam<String> TITLE =
            new StandardBundlerParam<>(
                    I18N.getString("param.title.name"),
                    I18N.getString("param.title.description"),
                    BundleParams.PARAM_TITLE,
                    String.class,
                    APP_NAME::fetchFrom,
                    (s, p) -> s
            );

    // note that each bundler is likely to replace this one with
    // their own converter
    public static final StandardBundlerParam<String> VERSION =
            new StandardBundlerParam<>(
                    I18N.getString("param.version.name"),
                    I18N.getString("param.version.description"),
                    Arguments.CLIOptions.VERSION.getId(),
                    String.class,
                    params -> I18N.getString("param.version.default"),
                    (s, p) -> s
            );

    @SuppressWarnings("unchecked")
    public static final StandardBundlerParam<List<String>> LICENSE_FILE =
            new StandardBundlerParam<>(
                    I18N.getString("param.license-file.name"),
                    I18N.getString("param.license-file.description"),
                    Arguments.CLIOptions.LICENSE_FILE.getId(),
                    (Class<List<String>>)(Object)List.class,
                    params -> Collections.<String>emptyList(),
                    (s, p) -> Arrays.asList(s.split(","))
            );

    public static final StandardBundlerParam<File> BUILD_ROOT =
            new StandardBundlerParam<>(
                    I18N.getString("param.build-root.name"),
                    I18N.getString("param.build-root.description"),
                    Arguments.CLIOptions.BUILD_ROOT.getId(),
                    File.class,
                    params -> {
                        try {
                            return Files.createTempDirectory(
                                    "jdk.packager").toFile();
                        } catch (IOException ioe) {
                            return null;
                        }
                    },
                    (s, p) -> new File(s)
            );

    public static final StandardBundlerParam<String> IDENTIFIER =
            new StandardBundlerParam<>(
                    I18N.getString("param.identifier.name"),
                    I18N.getString("param.identifier.description"),
                    Arguments.CLIOptions.IDENTIFIER.getId(),
                    String.class,
                    params -> {
                        String s = MAIN_CLASS.fetchFrom(params);
                        if (s == null) return null;

                        int idx = s.lastIndexOf(".");
                        if (idx >= 1) {
                            return s.substring(0, idx);
                        }
                        return s;
                    },
                    (s, p) -> s
            );

    public static final StandardBundlerParam<String> PREFERENCES_ID =
            new StandardBundlerParam<>(
                    I18N.getString("param.preferences-id.name"),
                    I18N.getString("param.preferences-id.description"),
                    "preferencesID",
                    String.class,
                    p -> Optional.ofNullable(IDENTIFIER.fetchFrom(p)).
                             orElse("").replace('.', '/'),
                    (s, p) -> s
            );

    // TODO: remove it?
    public static final StandardBundlerParam<String> PRELOADER_CLASS =
            new StandardBundlerParam<>(
                    I18N.getString("param.preloader.name"),
                    I18N.getString("param.preloader.description"),
                    "preloader",
                    String.class,
                    p -> null,
                    null
            );

    public static final StandardBundlerParam<Boolean> VERBOSE  =
            new StandardBundlerParam<>(
                    I18N.getString("param.verbose.name"),
                    I18N.getString("param.verbose.description"),
                    Arguments.CLIOptions.VERBOSE.getId(),
                    Boolean.class,
                    params -> false,
                    // valueOf(null) is false, and we actually do want null
                    (s, p) -> (s == null || "null".equalsIgnoreCase(s)) ?
                            true : Boolean.valueOf(s)
            );

    public static final StandardBundlerParam<File> DROP_IN_RESOURCES_ROOT =
            new StandardBundlerParam<>(
                    I18N.getString("param.drop-in-resources-root.name"),
                    I18N.getString("param.drop-in-resources-root.description"),
                    "dropinResourcesRoot",
                    File.class,
                    params -> new File("."),
                    (s, p) -> new File(s)
            );

    public static final BundlerParamInfo<String> INSTALL_DIR =
            new StandardBundlerParam<>(
                    I18N.getString("param.install-dir.name"),
                    I18N.getString("param.install-dir.description"),
                    Arguments.CLIOptions.INSTALL_DIR.getId(),
                    String.class,
                     params -> null,
                    (s, p) -> s
    );

    public static final StandardBundlerParam<File> PREDEFINED_APP_IMAGE =
            new StandardBundlerParam<>(
            I18N.getString("param.predefined-app-image.name"),
            I18N.getString("param.predefined-app-image.description"),
            Arguments.CLIOptions.PREDEFINED_APP_IMAGE.getId(),
            File.class,
            params -> null,
            (s, p) -> new File(s));

    public static final StandardBundlerParam<File> PREDEFINED_RUNTIME_IMAGE =
            new StandardBundlerParam<>(
            I18N.getString("param.predefined-runtime-image.name"),
            I18N.getString("param.predefined-runtime-image.description"),
            Arguments.CLIOptions.PREDEFINED_RUNTIME_IMAGE.getId(),
            File.class,
            params -> null,
            (s, p) -> new File(s));

    @SuppressWarnings("unchecked")
    public static final StandardBundlerParam<List<Map<String, ? super Object>>> SECONDARY_LAUNCHERS =
            new StandardBundlerParam<>(
                    I18N.getString("param.secondary-launchers.name"),
                    I18N.getString("param.secondary-launchers.description"),
                    Arguments.CLIOptions.SECONDARY_LAUNCHER.getId(),
                    (Class<List<Map<String, ? super Object>>>) (Object)
                            List.class,
                    params -> new ArrayList<>(1),
                    // valueOf(null) is false, and we actually do want null
                    (s, p) -> null
            );

    @SuppressWarnings("unchecked")
    public static final StandardBundlerParam
            <List<Map<String, ? super Object>>> FILE_ASSOCIATIONS =
            new StandardBundlerParam<>(
                    I18N.getString("param.file-associations.name"),
                    I18N.getString("param.file-associations.description"),
                    Arguments.CLIOptions.FILE_ASSOCIATIONS.getId(),
                    (Class<List<Map<String, ? super Object>>>) (Object)
                            List.class,
                    params -> new ArrayList<>(1),
                    // valueOf(null) is false, and we actually do want null
                    (s, p) -> null
            );

    @SuppressWarnings("unchecked")
    public static final StandardBundlerParam<List<String>> FA_EXTENSIONS =
            new StandardBundlerParam<>(
                    I18N.getString("param.fa-extension.name"),
                    I18N.getString("param.fa-extension.description"),
                    "fileAssociation.extension",
                    (Class<List<String>>) (Object) List.class,
                    params -> null, // null means not matched to an extension
                    (s, p) -> Arrays.asList(s.split("(,|\\s)+"))
            );

    @SuppressWarnings("unchecked")
    public static final StandardBundlerParam<List<String>> FA_CONTENT_TYPE =
            new StandardBundlerParam<>(
                    I18N.getString("param.fa-content-type.name"),
                    I18N.getString("param.fa-content-type.description"),
                    "fileAssociation.contentType",
                    (Class<List<String>>) (Object) List.class,
                    params -> null,
                            // null means not matched to a content/mime type
                    (s, p) -> Arrays.asList(s.split("(,|\\s)+"))
            );

    public static final StandardBundlerParam<String> FA_DESCRIPTION =
            new StandardBundlerParam<>(
                    I18N.getString("param.fa-description.name"),
                    I18N.getString("param.fa-description.description"),
                    "fileAssociation.description",
                    String.class,
                    params -> APP_NAME.fetchFrom(params) + " File",
                    null
            );

    public static final StandardBundlerParam<File> FA_ICON =
            new StandardBundlerParam<>(
                    I18N.getString("param.fa-icon.name"),
                    I18N.getString("param.fa-icon.description"),
                    "fileAssociation.icon",
                    File.class,
                    ICON::fetchFrom,
                    (s, p) -> new File(s)
            );

    @SuppressWarnings("unchecked")
    public static final BundlerParamInfo<List<Path>> MODULE_PATH =
            new StandardBundlerParam<>(
                    I18N.getString("param.module-path.name"),
                    I18N.getString("param.module-path.description"),
                    Arguments.CLIOptions.MODULE_PATH.getId(),
                    (Class<List<Path>>) (Object)List.class,
                    p -> { return getDefaultModulePath(); },
                    (s, p) -> {
                        List<Path> modulePath = Arrays.asList(s
                                .split(File.pathSeparator)).stream()
                                .map(ss -> new File(ss).toPath())
                                .collect(Collectors.toList());
                        Path javaBasePath = null;
                        if (modulePath != null) {
                            javaBasePath = JLinkBundlerHelper
                                    .findPathOfModule(modulePath, JAVABASEJMOD);
                        }
                        else {
                            modulePath = new ArrayList();
                        }

                        // Add the default JDK module path to the module path.
                        if (javaBasePath == null) {
                            List<Path> jdkModulePath = getDefaultModulePath();

                            if (jdkModulePath != null) {
                                modulePath.addAll(jdkModulePath);
                                javaBasePath =
                                        JLinkBundlerHelper.findPathOfModule(
                                        modulePath, JAVABASEJMOD);
                            }
                        }

                        if (javaBasePath == null ||
                                !Files.exists(javaBasePath)) {
                            jdk.packager.internal.Log.info(
                                String.format(I18N.getString(
                                        "warning.no.jdk.modules.found")));
                        }

                        return modulePath;
                    });

    @SuppressWarnings("unchecked")
    public static final BundlerParamInfo<String> MODULE =
            new StandardBundlerParam<>(
                    I18N.getString("param.main.module.name"),
                    I18N.getString("param.main.module.description"),
                    Arguments.CLIOptions.MODULE.getId(),
                    String.class,
                    p -> null,
                    (s, p) -> {
                        return String.valueOf(s);
                    });

    @SuppressWarnings("unchecked")
    public static final BundlerParamInfo<Set<String>> ADD_MODULES =
            new StandardBundlerParam<>(
                    I18N.getString("param.add-modules.name"),
                    I18N.getString("param.add-modules.description"),
                    Arguments.CLIOptions.ADD_MODULES.getId(),
                    (Class<Set<String>>) (Object) Set.class,
                    p -> new LinkedHashSet(),
                    (s, p) -> new LinkedHashSet<>(Arrays.asList(s.split(",")))
            );

    @SuppressWarnings("unchecked")
    public static final BundlerParamInfo<Set<String>> LIMIT_MODULES =
            new StandardBundlerParam<>(
                    I18N.getString("param.limit-modules.name"),
                    I18N.getString("param.limit-modules.description"),
                    Arguments.CLIOptions.LIMIT_MODULES.getId(),
                    (Class<Set<String>>) (Object) Set.class,
                    p -> new LinkedHashSet(),
                    (s, p) -> new LinkedHashSet<>(Arrays.asList(s.split(",")))
            );

    @SuppressWarnings("unchecked")
    public static final BundlerParamInfo<Boolean> STRIP_NATIVE_COMMANDS =
            new StandardBundlerParam<>(
                    I18N.getString("param.strip-executables.name"),
                    I18N.getString("param.strip-executables.description"),
                    Arguments.CLIOptions.STRIP_NATIVE_COMMANDS.getId(),
                    Boolean.class,
                    p -> Boolean.FALSE,
                    (s, p) -> Boolean.valueOf(s)
            );

    public static final BundlerParamInfo<Boolean> SINGLETON =
           new StandardBundlerParam<> (
                    I18N.getString("param.singleton.name"),
                    I18N.getString("param.singleton.description"),
                    Arguments.CLIOptions.SINGLETON.getId(),
                    Boolean.class,
                    params -> Boolean.FALSE,
                    (s, p) -> Boolean.valueOf(s)
    );

    public static final BundlerParamInfo<Boolean> ECHO_MODE =
            new StandardBundlerParam<> (
                    I18N.getString("param.echo-mode.name"),
                    I18N.getString("param.echo-mode.description"),
                    Arguments.CLIOptions.ECHO_MODE.getId(),
                    Boolean.class,
                    params -> Boolean.FALSE,
                    (s, p) -> Boolean.valueOf(s)
    );

    public static File getPredefinedAppImage(Map<String, ? super Object> p) {
        File applicationImage = null;
        if (PREDEFINED_APP_IMAGE.fetchFrom(p) != null) {
            applicationImage = PREDEFINED_APP_IMAGE.fetchFrom(p);
            Log.debug("Using App Image from " + applicationImage);
            if (!applicationImage.exists()) {
                throw new RuntimeException(
                        MessageFormat.format(I18N.getString(
                                "message.app-image-dir-does-not-exist"),
                                PREDEFINED_APP_IMAGE.getID(),
                                applicationImage.toString()));
            }
        }
        return applicationImage;
    }

    public static void copyPredefinedRuntimeImage(
            Map<String, ? super Object> p,
            AbstractAppImageBuilder appBuilder)
            throws IOException , ConfigException {
        File image = PREDEFINED_RUNTIME_IMAGE.fetchFrom(p);
        if (!image.exists()) {
            throw new ConfigException(
                    MessageFormat.format(I18N.getString(
                    "message.runtime-image-dir-does-not-exist"),
                    PREDEFINED_RUNTIME_IMAGE.getID(),
                    image.toString()),
                    MessageFormat.format(I18N.getString(
                    "message.runtime-image-dir-does-not-exist.advice"),
                    PREDEFINED_RUNTIME_IMAGE.getID()));
        }
        IOUtils.copyRecursive(image.toPath(), appBuilder.getRoot());
        appBuilder.prepareApplicationFiles();
    }

    public static void extractMainClassInfoFromAppResources(
            Map<String, ? super Object> params) {
        boolean hasMainClass = params.containsKey(MAIN_CLASS.getID());
        boolean hasMainJar = params.containsKey(MAIN_JAR.getID());
        boolean hasMainJarClassPath = params.containsKey(CLASSPATH.getID());
        boolean hasModule = params.containsKey(MODULE.getID());
        boolean jreInstaller =
                params.containsKey(Arguments.CREATE_JRE_INSTALLER.getID());

        if (hasMainClass && hasMainJar && hasMainJarClassPath || hasModule ||
                jreInstaller) {
            return;
        }

        // it's a pair.
        // The [0] is the srcdir [1] is the file relative to sourcedir
        List<String[]> filesToCheck = new ArrayList<>();

        if (hasMainJar) {
            RelativeFileSet rfs = MAIN_JAR.fetchFrom(params);
            for (String s : rfs.getIncludedFiles()) {
                filesToCheck.add(
                        new String[] {rfs.getBaseDirectory().toString(), s});
            }
        } else if (hasMainJarClassPath) {
            for (String s : CLASSPATH.fetchFrom(params).split("\\s+")) {
                if (APP_RESOURCES.fetchFrom(params) != null) {
                    filesToCheck.add(
                            new String[] {APP_RESOURCES.fetchFrom(params)
                            .getBaseDirectory().toString(), s});
                }
            }
        } else {
            List<RelativeFileSet> rfsl = APP_RESOURCES_LIST.fetchFrom(params);
            if (rfsl == null || rfsl.isEmpty()) {
                return;
            }
            for (RelativeFileSet rfs : rfsl) {
                if (rfs == null) continue;

                for (String s : rfs.getIncludedFiles()) {
                    filesToCheck.add(
                            new String[]{rfs.getBaseDirectory().toString(), s});
                }
            }
        }

        // presume the set iterates in-order
        for (String[] fnames : filesToCheck) {
            try {
                // only sniff jars
                if (!fnames[1].toLowerCase().endsWith(".jar")) continue;

                File file = new File(fnames[0], fnames[1]);
                // that actually exist
                if (!file.exists()) continue;

                try (JarFile jf = new JarFile(file)) {
                    Manifest m = jf.getManifest();
                    Attributes attrs = (m != null) ?
                            m.getMainAttributes() : null;

                    if (attrs != null) {
                        if (!hasMainJar) {
                            if (fnames[0] == null) {
                                fnames[0] = file.getParentFile().toString();
                            }
                            params.put(MAIN_JAR.getID(), new RelativeFileSet(
                                    new File(fnames[0]),
                                    new LinkedHashSet<>(Collections
                                    .singletonList(file))));
                        }
                        if (!hasMainJarClassPath) {
                            String cp =
                                    attrs.getValue(Attributes.Name.CLASS_PATH);
                            params.put(CLASSPATH.getID(),
                                    cp == null ? "" : cp);
                        }
                        break;
                    }
                }
            } catch (IOException ignore) {
                ignore.printStackTrace();
            }
        }
    }

    public static void validateMainClassInfoFromAppResources(
            Map<String, ? super Object> params) throws ConfigException {
        boolean hasMainClass = params.containsKey(MAIN_CLASS.getID());
        boolean hasMainJar = params.containsKey(MAIN_JAR.getID());
        boolean hasMainJarClassPath = params.containsKey(CLASSPATH.getID());
        boolean hasModule = params.containsKey(MODULE.getID());
        boolean hasAppImage = params.containsKey(PREDEFINED_APP_IMAGE.getID());
        boolean jreInstaller =
                params.containsKey(Arguments.CREATE_JRE_INSTALLER.getID());

        if (hasMainClass && hasMainJar && hasMainJarClassPath ||
               hasModule || jreInstaller || hasAppImage) {
            return;
        }

        extractMainClassInfoFromAppResources(params);

        if (!params.containsKey(MAIN_CLASS.getID())) {
            if (hasMainJar) {
                throw new ConfigException(
                        MessageFormat.format(I18N.getString(
                        "error.no-main-class-with-main-jar"),
                        MAIN_JAR.fetchFrom(params)),
                        MessageFormat.format(I18N.getString(
                        "error.no-main-class-with-main-jar.advice"),
                        MAIN_JAR.fetchFrom(params)));
            } else if (hasMainJarClassPath) {
                throw new ConfigException(
                        I18N.getString("error.no-main-class-with-classpath"),
                        I18N.getString(
                        "error.no-main-class-with-classpath.advice"));
            } else {
                throw new ConfigException(
                        I18N.getString("error.no-main-class"),
                        I18N.getString("error.no-main-class.advice"));
            }
        }
    }


    private static List<String> splitStringWithEscapes(String s) {
        List<String> l = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        boolean escaped = false;
        for (char c : s.toCharArray()) {
            if (escaped) {
                current.append(c);
            } else if ('"' == c) {
                quoted = !quoted;
            } else if (!quoted && Character.isWhitespace(c)) {
                l.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        l.add(current.toString());
        return l;
    }

    private static List<RelativeFileSet>
            createAppResourcesListFromString(String s,
            Map<String, ? super Object> objectObjectMap) {
        List<RelativeFileSet> result = new ArrayList<>();
        for (String path : s.split("[:;]")) {
            File f = new File(path);
            if (f.getName().equals("*") || path.endsWith("/") ||
                    path.endsWith("\\")) {
                if (f.getName().equals("*")) {
                    f = f.getParentFile();
                }
                Set<File> theFiles = new HashSet<>();
                try {
                    Files.walk(f.toPath())
                            .filter(Files::isRegularFile)
                            .forEach(p -> theFiles.add(p.toFile()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                result.add(new RelativeFileSet(f, theFiles));
            } else {
                result.add(new RelativeFileSet(f.getParentFile(),
                        Collections.singleton(f)));
            }
        }
        return result;
    }

    private static RelativeFileSet getMainJar(
            String moduleName, Map<String, ? super Object> params) {
        for (RelativeFileSet rfs : APP_RESOURCES_LIST.fetchFrom(params)) {
            File appResourcesRoot = rfs.getBaseDirectory();
            File mainJarFile = new File(appResourcesRoot, moduleName);

            if (mainJarFile.exists()) {
                return new RelativeFileSet(appResourcesRoot,
                     new LinkedHashSet<>(Collections.singletonList(
                     mainJarFile)));
            }
            else {
                List<Path> modulePath = MODULE_PATH.fetchFrom(params);
                Path modularJarPath = JLinkBundlerHelper.findPathOfModule(
                        modulePath, moduleName);

                if (modularJarPath != null && Files.exists(modularJarPath)) {
                    return new RelativeFileSet(appResourcesRoot,
                            new LinkedHashSet<>(Collections.singletonList(
                            modularJarPath.toFile())));
                }
            }
        }

        throw new IllegalArgumentException(
                new ConfigException(MessageFormat.format(I18N.getString(
                        "error.main-jar-does-not-exist"),
                        moduleName), I18N.getString(
                        "error.main-jar-does-not-exist.advice")));
    }

    public static List<Path> getDefaultModulePath() {
        List<Path> result = new ArrayList();
        Path jdkModulePath = Paths.get(
                System.getProperty("java.home"), "jmods").toAbsolutePath();

        if (jdkModulePath != null && Files.exists(jdkModulePath)) {
            result.add(jdkModulePath);
        }
        else {
            // On a developer build the JDK Home isn't where we expect it
            // relative to the jmods directory. Do some extra
            // processing to find it.
            Map<String, String> env = System.getenv();

            if (env.containsKey("JDK_HOME")) {
                jdkModulePath = Paths.get(env.get("JDK_HOME"),
                        ".." + File.separator + "images"
                        + File.separator + "jmods").toAbsolutePath();

                if (jdkModulePath != null && Files.exists(jdkModulePath)) {
                    result.add(jdkModulePath);
                }
            }
        }

        return result;
    }
}
