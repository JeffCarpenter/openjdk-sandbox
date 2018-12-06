/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jpackage.internal;

import jdk.jpackage.internal.*;
import jdk.jpackage.internal.ConfigException;
import jdk.jpackage.internal.Arguments;
import jdk.jpackage.internal.UnsupportedPlatformException;
import jdk.jpackage.internal.resources.WinResources;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static jdk.jpackage.internal.WindowsBundlerParam.*;

public class WinExeBundler extends AbstractBundler {

    private static final ResourceBundle I18N = ResourceBundle.getBundle(
            "jdk.jpackage.internal.resources.WinResources");

    public static final BundlerParamInfo<WinAppBundler> APP_BUNDLER =
            new WindowsBundlerParam<>(
            getString("param.exe-bundler.name"),
            getString("param.exe-bundler.description"),
            "win.app.bundler",
            WinAppBundler.class,
            params -> new WinAppBundler(),
            null);

    public static final BundlerParamInfo<File> CONFIG_ROOT =
            new WindowsBundlerParam<>(
            getString("param.config-root.name"),
            getString("param.config-root.description"),
            "configRoot",
            File.class,
            params -> {
                File imagesRoot =
                        new File(BUILD_ROOT.fetchFrom(params), "windows");
                imagesRoot.mkdirs();
                return imagesRoot;
            },
            (s, p) -> null);

    public static final BundlerParamInfo<File> EXE_IMAGE_DIR =
            new WindowsBundlerParam<>(
            getString("param.image-dir.name"),
            getString("param.image-dir.description"),
            "win.exe.imageDir",
            File.class,
            params -> {
                File imagesRoot = IMAGES_ROOT.fetchFrom(params);
                if (!imagesRoot.exists()) imagesRoot.mkdirs();
                return new File(imagesRoot, "win-exe.image");
            },
            (s, p) -> null);

    public static final BundlerParamInfo<File> WIN_APP_IMAGE =
            new WindowsBundlerParam<>(
            getString("param.app-dir.name"),
            getString("param.app-dir.description"),
            "win.app.image",
            File.class,
            null,
            (s, p) -> null);

    public static final BundlerParamInfo<UUID> UPGRADE_UUID =
            new WindowsBundlerParam<>(
            I18N.getString("param.upgrade-uuid.name"),
            I18N.getString("param.upgrade-uuid.description"),
            Arguments.CLIOptions.WIN_UPGRADE_UUID.getId(),
            UUID.class,
            params -> UUID.randomUUID(),
            (s, p) -> UUID.fromString(s));

    public static final StandardBundlerParam<Boolean> EXE_SYSTEM_WIDE  =
            new StandardBundlerParam<>(
            getString("param.system-wide.name"),
            getString("param.system-wide.description"),
            Arguments.CLIOptions.WIN_PER_USER_INSTALLATION.getId(),
            Boolean.class,
            params -> true, // default to system wide
            (s, p) -> (s == null || "null".equalsIgnoreCase(s))? null
                    : Boolean.valueOf(s)
            );
    public static final StandardBundlerParam<String> PRODUCT_VERSION =
            new StandardBundlerParam<>(
                    getString("param.product-version.name"),
                    getString("param.product-version.description"),
                    "win.msi.productVersion",
                    String.class,
                    VERSION::fetchFrom,
                    (s, p) -> s
            );

    public static final StandardBundlerParam<Boolean> MENU_HINT =
        new WindowsBundlerParam<>(
                getString("param.menu-shortcut-hint.name"),
                getString("param.menu-shortcut-hint.description"),
                Arguments.CLIOptions.WIN_MENU_HINT.getId(),
                Boolean.class,
                params -> false,
                (s, p) -> (s == null ||
                        "null".equalsIgnoreCase(s))? true : Boolean.valueOf(s)
        );

    public static final StandardBundlerParam<Boolean> SHORTCUT_HINT =
        new WindowsBundlerParam<>(
                getString("param.desktop-shortcut-hint.name"),
                getString("param.desktop-shortcut-hint.description"),
                Arguments.CLIOptions.WIN_SHORTCUT_HINT.getId(),
                Boolean.class,
                params -> false,
                (s, p) -> (s == null ||
                       "null".equalsIgnoreCase(s))? false : Boolean.valueOf(s)
        );

    private final static String DEFAULT_EXE_PROJECT_TEMPLATE = "template.iss";
    private final static String DEFAULT_JRE_EXE_TEMPLATE = "template.jre.iss";
    private static final String TOOL_INNO_SETUP_COMPILER = "iscc.exe";

    public static final BundlerParamInfo<String>
            TOOL_INNO_SETUP_COMPILER_EXECUTABLE = new WindowsBundlerParam<>(
            getString("param.iscc-path.name"),
            getString("param.iscc-path.description"),
            "win.exe.iscc.exe",
            String.class,
            params -> {
                for (String dirString : (System.getenv("PATH")
                        + ";C:\\Program Files (x86)\\Inno Setup 5;"
                        + "C:\\Program Files\\Inno Setup 5").split(";")) {
                    File f = new File(dirString.replace("\"", ""),
                            TOOL_INNO_SETUP_COMPILER);
                    if (f.isFile()) {
                        return f.toString();
                    }
                }
                return null;
            },
            null);

    public WinExeBundler() {
        super();
        baseResourceLoader = WinResources.class;
    }

    @Override
    public String getName() {
        return getString("exe.bundler.name");
    }

    @Override
    public String getDescription() {
        return getString("exe.bundler.description");
    }

    @Override
    public String getID() {
        return "exe";
    }

    @Override
    public String getBundleType() {
        return "INSTALLER";
    }

    @Override
    public Collection<BundlerParamInfo<?>> getBundleParameters() {
        Collection<BundlerParamInfo<?>> results = new LinkedHashSet<>();
        results.addAll(WinAppBundler.getAppBundleParameters());
        results.addAll(getExeBundleParameters());
        return results;
    }

    public static Collection<BundlerParamInfo<?>> getExeBundleParameters() {
        return Arrays.asList(
                DESCRIPTION,
                COPYRIGHT,
                LICENSE_FILE,
                MENU_GROUP,
                MENU_HINT,
                SHORTCUT_HINT,
                EXE_SYSTEM_WIDE,
                TITLE,
                VENDOR,
                INSTALLDIR_CHOOSER
        );
    }

    @Override
    public File execute(
            Map<String, ? super Object> p, File outputParentDir) {
        return bundle(p, outputParentDir);
    }

    @Override
    public boolean supported() {
        return (Platform.getPlatform() == Platform.WINDOWS);
    }

    static class VersionExtractor extends PrintStream {
        double version = 0f;

        public VersionExtractor() {
            super(new ByteArrayOutputStream());
        }

        double getVersion() {
            if (version == 0f) {
                String content =
                        new String(((ByteArrayOutputStream) out).toByteArray());
                Pattern pattern = Pattern.compile("Inno Setup (\\d+.?\\d*)");
                Matcher matcher = pattern.matcher(content);
                if (matcher.find()) {
                    String v = matcher.group(1);
                    version = Double.parseDouble(v);
                }
            }
            return version;
        }
    }

    private static double findToolVersion(String toolName) {
        try {
            if (toolName == null || "".equals(toolName)) return 0f;

            ProcessBuilder pb = new ProcessBuilder(
                    toolName,
                    "/?");
            VersionExtractor ve = new VersionExtractor();
            IOUtils.exec(pb, Log.isDebug(), true, ve);
            // not interested in the output
            double version = ve.getVersion();
            Log.verbose(MessageFormat.format(
                    getString("message.tool-version"), toolName, version));
            return version;
        } catch (Exception e) {
            if (Log.isDebug()) {
                Log.verbose(e);
            }
            return 0f;
        }
    }

    @Override
    public boolean validate(Map<String, ? super Object> p)
            throws UnsupportedPlatformException, ConfigException {
        try {
            if (p == null) throw new ConfigException(
                      getString("error.parameters-null"),
                      getString("error.parameters-null.advice"));

            // run basic validation to ensure requirements are met
            // we are not interested in return code, only possible exception
            APP_BUNDLER.fetchFrom(p).validate(p);

            // make sure some key values don't have newlines
            for (BundlerParamInfo<String> pi : Arrays.asList(
                    APP_NAME,
                    COPYRIGHT,
                    DESCRIPTION,
                    MENU_GROUP,
                    TITLE,
                    VENDOR,
                    VERSION)
            ) {
                String v = pi.fetchFrom(p);
                if (v.contains("\n") | v.contains("\r")) {
                    throw new ConfigException("Parmeter '" + pi.getID() +
                            "' cannot contain a newline.",
                            " Change the value of '" + pi.getID() +
                            " so that it does not contain any newlines");
                }
            }

            // exe bundlers trim the copyright to 100 characters,
            // tell them this will happen
            if (COPYRIGHT.fetchFrom(p).length() > 100) {
                throw new ConfigException(
                        getString("error.copyright-is-too-long"),
                        getString("error.copyright-is-too-long.advice"));
            }

            double innoVersion = findToolVersion(
                    TOOL_INNO_SETUP_COMPILER_EXECUTABLE.fetchFrom(p));

            //Inno Setup 5+ is required
            double minVersion = 5.0f;

            if (innoVersion < minVersion) {
                Log.error(MessageFormat.format(
                        getString("message.tool-wrong-version"),
                        TOOL_INNO_SETUP_COMPILER, innoVersion, minVersion));
                throw new ConfigException(
                        getString("error.iscc-not-found"),
                        getString("error.iscc-not-found.advice"));
            }

            /********* validate bundle parameters *************/

            // only one mime type per association, at least one file extension
            List<Map<String, ? super Object>> associations =
                    FILE_ASSOCIATIONS.fetchFrom(p);
            if (associations != null) {
                for (int i = 0; i < associations.size(); i++) {
                    Map<String, ? super Object> assoc = associations.get(i);
                    List<String> mimes = FA_CONTENT_TYPE.fetchFrom(assoc);
                    if (mimes.size() > 1) {
                        throw new ConfigException(MessageFormat.format(
                                getString("error.too-many-content-"
                                + "types-for-file-association"), i),
                                getString("error.too-many-content-"
                                + "types-for-file-association.advice"));
                    }
                }
            }

            // validate license file, if used, exists in the proper place
            if (p.containsKey(LICENSE_FILE.getID())) {
                List<RelativeFileSet> appResourcesList =
                        APP_RESOURCES_LIST.fetchFrom(p);
                for (String license : LICENSE_FILE.fetchFrom(p)) {
                    boolean found = false;
                    for (RelativeFileSet appResources : appResourcesList) {
                        found = found || appResources.contains(license);
                    }
                    if (!found) {
                        throw new ConfigException(
                            MessageFormat.format(getString(
                               "error.license-missing"), license),
                            MessageFormat.format(getString(
                               "error.license-missing.advice"), license));
                    }
                }
            }

            return true;
        } catch (RuntimeException re) {
            if (re.getCause() instanceof ConfigException) {
                throw (ConfigException) re.getCause();
            } else {
                throw new ConfigException(re);
            }
        }
    }

    private boolean prepareProto(Map<String, ? super Object> p)
                throws IOException {
        File appImage = StandardBundlerParam.getPredefinedAppImage(p);
        File appDir = null;

        // we either have an application image or need to build one
        if (appImage != null) {
            appDir = new File(
                    EXE_IMAGE_DIR.fetchFrom(p), APP_NAME.fetchFrom(p));
            // copy everything from appImage dir into appDir/name
            IOUtils.copyRecursive(appImage.toPath(), appDir.toPath());
        } else {
            appDir = APP_BUNDLER.fetchFrom(p).doBundle(p,
                    EXE_IMAGE_DIR.fetchFrom(p), true);
        }

        if (appDir == null) {
            return false;
        }

        p.put(WIN_APP_IMAGE.getID(), appDir);

        List<String> licenseFiles = LICENSE_FILE.fetchFrom(p);
        if (licenseFiles != null) {
            // need to copy license file to the root of win.app.image
            outerLoop:
            for (RelativeFileSet rfs : APP_RESOURCES_LIST.fetchFrom(p)) {
                for (String s : licenseFiles) {
                    if (rfs.contains(s)) {
                        File lfile = new File(rfs.getBaseDirectory(), s);
                        File destFile =
                            new File(appDir.getParentFile(), lfile.getName());
                        IOUtils.copyFile(lfile, destFile);
                        ensureByMutationFileIsRTF(destFile);
                        break outerLoop;
                    }
                }
            }
        }

        // copy file association icons
        List<Map<String, ? super Object>> fileAssociations =
                FILE_ASSOCIATIONS.fetchFrom(p);

        for (Map<String, ? super Object> fa : fileAssociations) {
            File icon = FA_ICON.fetchFrom(fa); // TODO FA_ICON_ICO
            if (icon == null) {
                continue;
            }

            File faIconFile = new File(appDir, icon.getName());

            if (icon.exists()) {
                try {
                    IOUtils.copyFile(icon, faIconFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return true;
    }

    public File bundle(Map<String, ? super Object> p, File outdir) {
        if (!outdir.isDirectory() && !outdir.mkdirs()) {
            throw new RuntimeException(MessageFormat.format(
                    getString("error.cannot-create-output-dir"),
                    outdir.getAbsolutePath()));
        }
        if (!outdir.canWrite()) {
            throw new RuntimeException(MessageFormat.format(
                    getString("error.cannot-write-to-output-dir"),
                    outdir.getAbsolutePath()));
        }

        if (WindowsDefender.isThereAPotentialWindowsDefenderIssue()) {
            Log.error(MessageFormat.format(
                    getString("message.potential.windows.defender.issue"),
                    WindowsDefender.getUserTempDirectory()));
        }

        // validate we have valid tools before continuing
        String iscc = TOOL_INNO_SETUP_COMPILER_EXECUTABLE.fetchFrom(p);
        if (iscc == null || !new File(iscc).isFile()) {
            Log.error(getString("error.iscc-not-found"));
            Log.error(MessageFormat.format(
                    getString("message.iscc-file-string"), iscc));
            return null;
        }

        File imageDir = EXE_IMAGE_DIR.fetchFrom(p);
        try {
            imageDir.mkdirs();

            boolean menuShortcut = MENU_HINT.fetchFrom(p);
            boolean desktopShortcut = SHORTCUT_HINT.fetchFrom(p);
            if (!menuShortcut && !desktopShortcut) {
                // both can not be false - user will not find the app
                Log.verbose(getString("message.one-shortcut-required"));
                p.put(MENU_HINT.getID(), true);
            }

            if (prepareProto(p) && prepareProjectConfig(p)) {
                File configScript = getConfig_Script(p);
                if (configScript.exists()) {
                    Log.verbose(MessageFormat.format(
                            getString("message.running-wsh-script"),
                            configScript.getAbsolutePath()));
                    IOUtils.run("wscript", configScript, VERBOSE.fetchFrom(p));
                }
                return buildEXE(p, outdir);
            }
            return null;
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        } finally {
            try {
                if (imageDir != null &&
                        PREDEFINED_APP_IMAGE.fetchFrom(p) == null &&
                        (PREDEFINED_RUNTIME_IMAGE.fetchFrom(p) == null ||
                        !Arguments.CREATE_JRE_INSTALLER.fetchFrom(p)) &&
                        !Log.isDebug() &&
                        !Log.isVerbose()) {
                    IOUtils.deleteRecursive(imageDir);
                } else if (imageDir != null) {
                    Log.verbose(MessageFormat.format(
                            I18N.getString("message.debug-working-directory"),
                            imageDir.getAbsolutePath()));
                }
            } catch (IOException ex) {
                // noinspection ReturnInsideFinallyBlock
                Log.debug(ex.getMessage());
                return null;
            }
        }
    }

    // name of post-image script
    private File getConfig_Script(Map<String, ? super Object> p) {
        return new File(EXE_IMAGE_DIR.fetchFrom(p),
                APP_NAME.fetchFrom(p) + "-post-image.wsf");
    }

    private String getAppIdentifier(Map<String, ? super Object> p) {
        String nm = UPGRADE_UUID.fetchFrom(p).toString();

        // limitation of innosetup
        if (nm.length() > 126) {
            Log.error(getString("message-truncating-id"));
            nm = nm.substring(0, 126);
        }

        return nm;
    }

    private String getLicenseFile(Map<String, ? super Object> p) {
        List<String> licenseFiles = LICENSE_FILE.fetchFrom(p);
        if (licenseFiles == null || licenseFiles.isEmpty()) {
            return "";
        } else {
            return licenseFiles.get(0);
        }
    }

    void validateValueAndPut(Map<String, String> data, String key,
                BundlerParamInfo<String> param,
                Map<String, ? super Object> p) throws IOException {
        String value = param.fetchFrom(p);
        if (value.contains("\r") || value.contains("\n")) {
            throw new IOException("Configuration Parameter " +
                     param.getID() + " cannot contain multiple lines of text");
        }
        data.put(key, innosetupEscape(value));
    }

    private String innosetupEscape(String value) {
        if (value.contains("\"") || !value.trim().equals(value)) {
            value = "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    boolean prepareMainProjectFile(Map<String, ? super Object> p)
            throws IOException {
        Map<String, String> data = new HashMap<>();
        data.put("PRODUCT_APP_IDENTIFIER",
                innosetupEscape(getAppIdentifier(p)));


        validateValueAndPut(data, "INSTALLER_NAME", APP_NAME, p);
        validateValueAndPut(data, "APPLICATION_VENDOR", VENDOR, p);
        validateValueAndPut(data, "APPLICATION_VERSION", VERSION, p);
        validateValueAndPut(data, "INSTALLER_FILE_NAME",
                INSTALLER_FILE_NAME, p);

        data.put("LAUNCHER_NAME",
                innosetupEscape(WinAppBundler.getAppName(p)));

        data.put("APPLICATION_LAUNCHER_FILENAME",
                innosetupEscape(WinAppBundler.getLauncherName(p)));

        data.put("APPLICATION_DESKTOP_SHORTCUT",
                SHORTCUT_HINT.fetchFrom(p) ? "returnTrue" : "returnFalse");
        data.put("APPLICATION_MENU_SHORTCUT",
                MENU_HINT.fetchFrom(p) ? "returnTrue" : "returnFalse");
        validateValueAndPut(data, "APPLICATION_GROUP", MENU_GROUP, p);
        validateValueAndPut(data, "APPLICATION_COMMENTS", TITLE, p);
        validateValueAndPut(data, "APPLICATION_COPYRIGHT", COPYRIGHT, p);

        data.put("APPLICATION_LICENSE_FILE",
                innosetupEscape(getLicenseFile(p)));
        data.put("DISABLE_DIR_PAGE",
                INSTALLDIR_CHOOSER.fetchFrom(p) ? "No" : "Yes");

        Boolean isSystemWide = EXE_SYSTEM_WIDE.fetchFrom(p);

        if (isSystemWide) {
            data.put("APPLICATION_INSTALL_ROOT", "{pf}");
            data.put("APPLICATION_INSTALL_PRIVILEGE", "admin");
        } else {
            data.put("APPLICATION_INSTALL_ROOT", "{localappdata}");
            data.put("APPLICATION_INSTALL_PRIVILEGE", "lowest");
        }

        if (BIT_ARCH_64.fetchFrom(p)) {
            data.put("ARCHITECTURE_BIT_MODE", "x64");
        } else {
            data.put("ARCHITECTURE_BIT_MODE", "");
        }
        validateValueAndPut(data, "RUN_FILENAME", APP_NAME, p);

        validateValueAndPut(data, "APPLICATION_DESCRIPTION",
                DESCRIPTION, p);

        data.put("APPLICATION_SERVICE", "returnFalse");
        data.put("APPLICATION_NOT_SERVICE", "returnFalse");
        data.put("APPLICATION_APP_CDS_INSTALL", "returnFalse");
        data.put("START_ON_INSTALL", "");
        data.put("STOP_ON_UNINSTALL", "");
        data.put("RUN_AT_STARTUP", "");

        StringBuilder secondaryLaunchersCfg = new StringBuilder();
        for (Map<String, ? super Object>
                launcher : SECONDARY_LAUNCHERS.fetchFrom(p)) {
            String application_name = APP_NAME.fetchFrom(launcher);
            if (MENU_HINT.fetchFrom(launcher)) {
                // Name: "{group}\APPLICATION_NAME";
                // Filename: "{app}\APPLICATION_NAME.exe";
                // IconFilename: "{app}\APPLICATION_NAME.ico"
                secondaryLaunchersCfg.append("Name: \"{group}\\");
                secondaryLaunchersCfg.append(application_name);
                secondaryLaunchersCfg.append("\"; Filename: \"{app}\\");
                secondaryLaunchersCfg.append(application_name);
                secondaryLaunchersCfg.append(".exe\"; IconFilename: \"{app}\\");
                secondaryLaunchersCfg.append(application_name);
                secondaryLaunchersCfg.append(".ico\"\r\n");
            }
            if (SHORTCUT_HINT.fetchFrom(launcher)) {
                // Name: "{commondesktop}\APPLICATION_NAME";
                // Filename: "{app}\APPLICATION_NAME.exe";
                // IconFilename: "{app}\APPLICATION_NAME.ico"
                secondaryLaunchersCfg.append("Name: \"{commondesktop}\\");
                secondaryLaunchersCfg.append(application_name);
                secondaryLaunchersCfg.append("\"; Filename: \"{app}\\");
                secondaryLaunchersCfg.append(application_name);
                secondaryLaunchersCfg.append(".exe\";  IconFilename: \"{app}\\");
                secondaryLaunchersCfg.append(application_name);
                secondaryLaunchersCfg.append(".ico\"\r\n");
            }
        }
        data.put("SECONDARY_LAUNCHERS", secondaryLaunchersCfg.toString());

        StringBuilder registryEntries = new StringBuilder();
        String regName = APP_REGISTRY_NAME.fetchFrom(p);
        List<Map<String, ? super Object>> fetchFrom =
                FILE_ASSOCIATIONS.fetchFrom(p);
        for (int i = 0; i < fetchFrom.size(); i++) {
            Map<String, ? super Object> fileAssociation = fetchFrom.get(i);
            String description = FA_DESCRIPTION.fetchFrom(fileAssociation);
            File icon = FA_ICON.fetchFrom(fileAssociation); //TODO FA_ICON_ICO

            List<String> extensions = FA_EXTENSIONS.fetchFrom(fileAssociation);
            String entryName = regName + "File";
            if (i > 0) {
                entryName += "." + i;
            }

            if (extensions == null) {
                Log.verbose(getString(
                        "message.creating-association-with-null-extension"));
            } else {
                for (String ext : extensions) {
                    if (isSystemWide) {
                        // "Root: HKCR; Subkey: \".myp\";
                        // ValueType: string; ValueName: \"\";
                        // ValueData: \"MyProgramFile\";
                        // Flags: uninsdeletevalue"
                        registryEntries.append("Root: HKCR; Subkey: \".")
                                .append(ext)
                                .append("\"; ValueType: string;"
                                + " ValueName: \"\"; ValueData: \"")
                                .append(entryName)
                                .append("\"; Flags: uninsdeletevalue\r\n");
                    } else {
                        registryEntries.append(
                                "Root: HKCU; Subkey: \"Software\\Classes\\.")
                                .append(ext)
                                .append("\"; ValueType: string;"
                                + " ValueName: \"\"; ValueData: \"")
                                .append(entryName)
                                .append("\"; Flags: uninsdeletevalue\r\n");
                    }
                }
            }

            if (extensions != null && !extensions.isEmpty()) {
                String ext = extensions.get(0);
                List<String> mimeTypes =
                        FA_CONTENT_TYPE.fetchFrom(fileAssociation);
                for (String mime : mimeTypes) {
                    if (isSystemWide) {
                        // "Root: HKCR;
                        // Subkey: HKCR\\Mime\\Database\\
                        //         Content Type\\application/chaos;
                        // ValueType: string;
                        // ValueName: Extension;
                        // ValueData: .chaos;
                        // Flags: uninsdeletevalue"
                        registryEntries.append("Root: HKCR; Subkey: " +
                                 "\"Mime\\Database\\Content Type\\")
                            .append(mime)
                            .append("\"; ValueType: string; ValueName: " +
                                 "\"Extension\"; ValueData: \".")
                            .append(ext)
                            .append("\"; Flags: uninsdeletevalue\r\n");
                    } else {
                        registryEntries.append(
                                "Root: HKCU; Subkey: \"Software\\" +
                                "Classes\\Mime\\Database\\Content Type\\")
                                .append(mime)
                                .append("\"; ValueType: string; " +
                                "ValueName: \"Extension\"; ValueData: \".")
                                .append(ext)
                                .append("\"; Flags: uninsdeletevalue\r\n");
                    }
                }
            }

            if (isSystemWide) {
                // "Root: HKCR;
                // Subkey: \"MyProgramFile\";
                // ValueType: string;
                // ValueName: \"\";
                // ValueData: \"My Program File\";
                // Flags: uninsdeletekey"
                registryEntries.append("Root: HKCR; Subkey: \"")
                    .append(entryName)
                    .append(
                    "\"; ValueType: string; ValueName: \"\"; ValueData: \"")
                    .append(removeQuotes(description))
                    .append("\"; Flags: uninsdeletekey\r\n");
            } else {
                registryEntries.append(
                    "Root: HKCU; Subkey: \"Software\\Classes\\")
                    .append(entryName)
                    .append(
                    "\"; ValueType: string; ValueName: \"\"; ValueData: \"")
                    .append(removeQuotes(description))
                    .append("\"; Flags: uninsdeletekey\r\n");
            }

            if (icon != null && icon.exists()) {
                if (isSystemWide) {
                    // "Root: HKCR;
                    // Subkey: \"MyProgramFile\\DefaultIcon\";
                    // ValueType: string;
                    // ValueName: \"\";
                    // ValueData: \"{app}\\MYPROG.EXE,0\"\n" +
                    registryEntries.append("Root: HKCR; Subkey: \"")
                            .append(entryName)
                            .append("\\DefaultIcon\"; ValueType: string; " +
                            "ValueName: \"\"; ValueData: \"{app}\\")
                            .append(icon.getName())
                            .append("\"\r\n");
                } else {
                    registryEntries.append(
                            "Root: HKCU; Subkey: \"Software\\Classes\\")
                            .append(entryName)
                            .append("\\DefaultIcon\"; ValueType: string; " +
                            "ValueName: \"\"; ValueData: \"{app}\\")
                            .append(icon.getName())
                            .append("\"\r\n");
                }
            }

            if (isSystemWide) {
                // "Root: HKCR;
                // Subkey: \"MyProgramFile\\shell\\open\\command\";
                // ValueType: string;
                // ValueName: \"\";
                // ValueData: \"\"\"{app}\\MYPROG.EXE\"\" \"\"%1\"\"\"\n"
                registryEntries.append("Root: HKCR; Subkey: \"")
                        .append(entryName)
                        .append("\\shell\\open\\command\"; ValueType: " +
                        "string; ValueName: \"\"; ValueData: \"\"\"{app}\\")
                        .append(APP_NAME.fetchFrom(p))
                        .append("\"\" \"\"%1\"\"\"\r\n");
            } else {
                registryEntries.append(
                        "Root: HKCU; Subkey: \"Software\\Classes\\")
                        .append(entryName)
                        .append("\\shell\\open\\command\"; ValueType: " +
                        "string; ValueName: \"\"; ValueData: \"\"\"{app}\\")
                        .append(APP_NAME.fetchFrom(p))
                        .append("\"\" \"\"%1\"\"\"\r\n");
            }
        }
        if (registryEntries.length() > 0) {
            data.put("FILE_ASSOCIATIONS",
                    "ChangesAssociations=yes\r\n\r\n[Registry]\r\n" +
                    registryEntries.toString());
        } else {
            data.put("FILE_ASSOCIATIONS", "");
        }

        // TODO - alternate template for JRE installer
        String iss = Arguments.CREATE_JRE_INSTALLER.fetchFrom(p) ?
                DEFAULT_JRE_EXE_TEMPLATE : DEFAULT_EXE_PROJECT_TEMPLATE;

        Writer w = new BufferedWriter(new FileWriter(
                getConfig_ExeProjectFile(p)));

        String content = preprocessTextResource(
                WinAppBundler.WIN_BUNDLER_PREFIX +
                getConfig_ExeProjectFile(p).getName(),
                getString("resource.inno-setup-project-file"),
                iss, data, VERBOSE.fetchFrom(p),
                DROP_IN_RESOURCES_ROOT.fetchFrom(p));
        w.write(content);
        w.close();
        return true;
    }

    private final static String removeQuotes(String s) {
        if (s.length() > 2 && s.startsWith("\"") && s.endsWith("\"")) {
            // special case for '"XXX"' return 'XXX' not '-XXX-'
            // note '"' and '""' are excluded from this special case
            s = s.substring(1, s.length() - 1);
        }
        // if there interior double quotes replace them with '-'
        return s.replaceAll("\"", "-");
    }

    private final static String DEFAULT_INNO_SETUP_ICON =
            "icon_inno_setup.bmp";

    private boolean prepareProjectConfig(Map<String, ? super Object> p)
            throws IOException {
        prepareMainProjectFile(p);

        // prepare installer icon
        File iconTarget = getConfig_SmallInnoSetupIcon(p);
        fetchResource(WinAppBundler.WIN_BUNDLER_PREFIX + iconTarget.getName(),
                getString("resource.setup-icon"),
                DEFAULT_INNO_SETUP_ICON,
                iconTarget,
                VERBOSE.fetchFrom(p),
                DROP_IN_RESOURCES_ROOT.fetchFrom(p));

        fetchResource(WinAppBundler.WIN_BUNDLER_PREFIX +
                getConfig_Script(p).getName(),
                getString("resource.post-install-script"),
                (String) null,
                getConfig_Script(p),
                VERBOSE.fetchFrom(p),
                DROP_IN_RESOURCES_ROOT.fetchFrom(p));
        return true;
    }

    private File getConfig_SmallInnoSetupIcon(
            Map<String, ? super Object> p) {
        return new File(EXE_IMAGE_DIR.fetchFrom(p),
                APP_NAME.fetchFrom(p) + "-setup-icon.bmp");
    }

    private File getConfig_ExeProjectFile(Map<String, ? super Object> p) {
        return new File(EXE_IMAGE_DIR.fetchFrom(p),
                APP_NAME.fetchFrom(p) + ".iss");
    }


    private File buildEXE(Map<String, ? super Object> p, File outdir)
             throws IOException {
        Log.verbose(MessageFormat.format(
             getString("message.outputting-to-location"),
             outdir.getAbsolutePath()));

        outdir.mkdirs();

        // run Inno Setup
        ProcessBuilder pb = new ProcessBuilder(
                TOOL_INNO_SETUP_COMPILER_EXECUTABLE.fetchFrom(p),
                "/q",    // turn off inno setup output
                "/o"+outdir.getAbsolutePath(),
                getConfig_ExeProjectFile(p).getAbsolutePath());
        pb = pb.directory(EXE_IMAGE_DIR.fetchFrom(p));
        IOUtils.exec(pb, VERBOSE.fetchFrom(p));

        Log.verbose(MessageFormat.format(
                getString("message.output-location"),
                outdir.getAbsolutePath()));

        // presume the result is the ".exe" file with the newest modified time
        // not the best solution, but it is the most reliable
        File result = null;
        long lastModified = 0;
        File[] list = outdir.listFiles();
        if (list != null) {
            for (File f : list) {
                if (f.getName().endsWith(".exe") &&
                        f.lastModified() > lastModified) {
                    result = f;
                    lastModified = f.lastModified();
                }
            }
        }

        return result;
    }

   public static void ensureByMutationFileIsRTF(File f) {
        if (f == null || !f.isFile()) return;

        try {
            boolean existingLicenseIsRTF = false;

            try (FileInputStream fin = new FileInputStream(f)) {
                byte[] firstBits = new byte[7];

                if (fin.read(firstBits) == firstBits.length) {
                    String header = new String(firstBits);
                    existingLicenseIsRTF = "{\\rtf1\\".equals(header);
                }
            }

            if (!existingLicenseIsRTF) {
                List<String> oldLicense = Files.readAllLines(f.toPath());
                try (Writer w = Files.newBufferedWriter(
                        f.toPath(), Charset.forName("Windows-1252"))) {
                    w.write("{\\rtf1\\ansi\\ansicpg1252\\deff0\\deflang1033"
                            + "{\\fonttbl{\\f0\\fnil\\fcharset0 Arial;}}\n"
                            + "\\viewkind4\\uc1\\pard\\sa200\\sl276"
                            + "\\slmult1\\lang9\\fs20 ");
                    oldLicense.forEach(l -> {
                        try {
                            for (char c : l.toCharArray()) {
                                if (c < 0x10) {
                                    w.write("\\'0");
                                    w.write(Integer.toHexString(c));
                                } else if (c > 0xff) {
                                    w.write("\\ud");
                                    w.write(Integer.toString(c));
                                    w.write("?");
                                } else if ((c < 0x20) || (c >= 0x80) ||
                                        (c == 0x5C) || (c == 0x7B) ||
                                        (c == 0x7D)) {
                                    w.write("\\'");
                                    w.write(Integer.toHexString(c));
                                } else {
                                    w.write(c);
                                }
                            }
                            if (l.length() < 1) {
                                w.write("\\par");
                            } else {
                                w.write(" ");
                            }
                            w.write("\r\n");
                        } catch (IOException e) {
                            Log.verbose(e);
                        }
                    });
                    w.write("}\r\n");
                }
            }
        } catch (IOException e) {
            Log.verbose(e);
        }
    }

    private static String getString(String key)
            throws MissingResourceException {
        return I18N.getString(key);
    }
}
