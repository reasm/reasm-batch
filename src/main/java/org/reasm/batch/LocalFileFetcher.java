package org.reasm.batch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.reasm.FileFetcher;
import org.reasm.source.SourceFile;

/**
 * Fetches files from the local file system, relative to the directory of the main source file.
 *
 * @author Francis Gagné
 */
public class LocalFileFetcher implements FileFetcher {

    private final Path mainFileDirectoryPath;

    /**
     * Initializes a new LocalFileFetcher.
     *
     * @param mainFilePath
     *            the path to the main source file
     */
    public LocalFileFetcher(String mainFilePath) {
        final Path mainFileDirectoryPath = Paths.get(mainFilePath).getParent();
        if (mainFileDirectoryPath != null && !Files.isDirectory(mainFileDirectoryPath)) {
            throw new IllegalArgumentException("mainFilePath");
        }

        this.mainFileDirectoryPath = mainFileDirectoryPath;
    }

    @Override
    public byte[] fetchBinaryFile(String filePath) throws IOException {
        return this.fetchFile(filePath);
    }

    @Override
    public SourceFile fetchSourceFile(String filePath) throws IOException {
        return new SourceFile(new String(this.fetchFile(filePath), "UTF-8"), filePath);
    }

    private byte[] fetchFile(String filePath) throws IOException {
        return Files.readAllBytes(this.resolveInclude(filePath));
    }

    private Path resolveInclude(String filePath) {
        if (this.mainFileDirectoryPath != null) {
            return this.mainFileDirectoryPath.resolve(filePath).toAbsolutePath();
        }

        return Paths.get(filePath).toAbsolutePath();
    }

}
