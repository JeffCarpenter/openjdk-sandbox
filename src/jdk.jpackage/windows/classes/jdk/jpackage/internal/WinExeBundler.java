/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.*;

public class WinExeBundler extends AbstractBundler {

    static {
        System.loadLibrary("jpackage");
    }

    private static final ResourceBundle I18N = ResourceBundle.getBundle(
            "jdk.jpackage.internal.resources.WinResources");

    public static final BundlerParamInfo<WinAppBundler> APP_BUNDLER
            = new WindowsBundlerParam<>(
                    "win.app.bundler",
                    WinAppBundler.class,
                    params -> new WinAppBundler(),
                    null);

    public static final BundlerParamInfo<File> EXE_IMAGE_DIR
            = new WindowsBundlerParam<>(
                    "win.exe.imageDir",
                    File.class,
                    params -> {
                        File imagesRoot = IMAGES_ROOT.fetchFrom(params);
                        if (!imagesRoot.exists()) {
                            imagesRoot.mkdirs();
                        }
                        return new File(imagesRoot, "win-exe.image");
                    },
                    (s, p) -> null);

    private final static String EXE_WRAPPER_NAME = "msiwrapper.exe";

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
        return new WinMsiBundler().getBundleParameters();
    }

    @Override
    public File execute(Map<String, ? super Object> params,
            File outputParentDir) throws PackagerException {
        return bundle(params, outputParentDir);
    }

    @Override
    public boolean supported(boolean platformInstaller) {
        return (Platform.getPlatform() == Platform.WINDOWS);
    }

    @Override
    public boolean validate(Map<String, ? super Object> params)
            throws UnsupportedPlatformException, ConfigException {
        return new WinMsiBundler().validate(params);
    }

    public File bundle(Map<String, ? super Object> params, File outdir)
            throws PackagerException {

        File exeImageDir = EXE_IMAGE_DIR.fetchFrom(params);

        // Write msi to temporary directory.
        File msi = new WinMsiBundler().bundle(params, exeImageDir);

        try {
            return buildEXE(msi, outdir);
        } catch (IOException ex) {
            Log.verbose(ex);
            throw new PackagerException(ex);
        }
    }

    private File buildEXE(File msi, File outdir)
            throws IOException {

        Log.verbose(MessageFormat.format(
                getString("message.outputting-to-location"),
                outdir.getAbsolutePath()));

        // Copy template msi wrapper next to msi file
        String exePath = msi.getAbsolutePath();
        exePath = exePath.substring(0, exePath.lastIndexOf('.')) + ".exe";
        try (InputStream is = getResourceAsStream(EXE_WRAPPER_NAME)) {
            Files.copy(is, Path.of(exePath));
        }
        // Embed msi in msi wrapper exe.
        embedMSI(exePath, msi.getAbsolutePath());

        Path dstExePath = Paths.get(outdir.getAbsolutePath(), Path.of(exePath).getFileName().toString());
        Files.deleteIfExists(dstExePath);

        Files.copy(Path.of(exePath), dstExePath);

        Log.verbose(MessageFormat.format(
                getString("message.output-location"),
                outdir.getAbsolutePath()));

        return dstExePath.toFile();
    }

    private static String getString(String key)
            throws MissingResourceException {
        return I18N.getString(key);
    }

    private static native int embedMSI(String exePath, String msiPath);
}
