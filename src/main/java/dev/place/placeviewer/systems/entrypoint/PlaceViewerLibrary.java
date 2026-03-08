package dev.place.placeviewer.systems.entrypoint;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class PlaceViewerLibrary {

    private PlaceViewerLibrary() {}

    private static final boolean LOADED;

    public static boolean isLoaded() {
        return LOADED;
    }

    static {
        boolean loaded = false;
        try {
            tryLoadLibrary();
            loaded = true;
        } catch (final Throwable t) {
            PlaceViewer.LOGGER.error("Failed to load PlaceViewer native libraries", t);
        }
        LOADED = loaded;
    }

    @NotNull
    private static String nativeLibraryName() {
        final int bits = Integer.parseInt(System.getProperty("sun.arch.data.model"));
        if (bits != 64)
            throw new UnsupportedOperationException("Unsupported architecture (64-bit required)");

        final String osName = System.getProperty("os.name").toLowerCase();
        final String osArch = System.getProperty("os.arch").toLowerCase();

        final String arch;
        if (osArch.contains("arm") || osArch.contains("aarch64")) {
            arch = "aarch64";
        } else if (osArch.equals("x86_64") || osArch.equals("amd64")) {
            arch = "x86_64";
        } else {
            throw new UnsupportedOperationException("Unsupported architecture: " + osArch);
        }
        if (osName.contains("linux")) {
            return "libPlaceViewer-" + arch + ".so";
        }
        if (osName.contains("windows")) {
            return "PlaceViewer-" + arch + ".dll";
        }
        if (osName.contains("mac")) {
            return "libPlaceViewer-" + arch + ".dylib";
        }
        throw new UnsupportedOperationException("Unsupported OS: " + osName);
    }

    private static byte[] nativeLibrary(final String libName) throws IOException {
        try (
            final InputStream librariesRaw = Objects.requireNonNull(PlaceViewerLibrary.class.getClassLoader().getResourceAsStream("libraries.zip"));
            final ZipInputStream librariesZip = new ZipInputStream(librariesRaw)
        ) {
            ZipEntry entry;
            while ((entry = librariesZip.getNextEntry()) != null) {
                if (!entry.getName().equals(libName))
                    continue;

                final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                final byte[] buffer = new byte[4096];

                int read;
                while ((read = librariesZip.read(buffer)) != -1) {
                    byteStream.write(buffer, 0, read);
                }
                return byteStream.toByteArray();
            }
        }
        throw new IllegalStateException("Failed to find PlaceViewer library: " + libName);
    }

    private static void tryLoadLibrary() throws IOException {
        final String libraryName = nativeLibraryName();
        final byte[] libraryBytes = nativeLibrary(libraryName);

        final String[] split = libraryName.split("\\.");
        final Path tempFile = Files.createTempFile(split[0], "." + split[1]);

        try {
            Files.write(tempFile, libraryBytes);
            System.load(tempFile.toAbsolutePath().toString());
        } finally {
            try {
                Files.delete(tempFile);
            } catch (final IOException ignored) {

            }
            if (!tempFile.toFile().delete()) {
                tempFile.toFile().deleteOnExit();
            }
        }
    }

}
