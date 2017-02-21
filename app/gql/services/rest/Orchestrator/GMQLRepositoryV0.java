package gql.services.rest.Orchestrator;

/**
 * Created by abdulrahman on 14/02/2017.
 */
import com.google.common.base.Charsets;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.InvalidKeyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GMQLRepositoryV0 {

    //TODO: Add WatchService to catch changes on repository
    private static final Logger logger = LoggerFactory.getLogger(GMQLRepositoryV0.class);
    private static GMQLRepositoryV0 instance = null;
    private BiMap<String, Path> key_to_file = null;

    private final GlobalFileVisitor fileVisitor;

    //private final String ROOT_DIR = "C:\\GMQLWebAppTest";
    private String ROOT_DIR;
    //private final String ROOT_DIR = "/home/gql_repository/data/";
    private final Path rootPath;
    private static final HashFunction hashFun = Hashing.md5();

    private GMQLRepositoryV0() {
        ROOT_DIR = utils.GMQL_Globals.apply().ut().RepoDir();
        key_to_file = HashBiMap.create();
        fileVisitor = new GlobalFileVisitor(key_to_file);
        rootPath = Paths.get(ROOT_DIR);
        try {
            //visit the entire repository
            Files.walkFileTree(rootPath, fileVisitor);
//            System.out.println("Done waking through the tree : " +ROOT_DIR);
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    /**
     * Returns the current instance of the GMQLRepository
     *
     * @return An instance of {@code GMQLRepository}
     */
    public static GMQLRepositoryV0 getInstance() {
        if (instance == null) {
            instance = new GMQLRepositoryV0();
        }
        return instance;
    }

    /**
     * Checks if a given filekey is valid (i.e. it is actually associated with a
     * file)
     *
     * @param filekey the key of the file
     * @return true if the the filekey is valid, false otherwise
     */
    public synchronized boolean isValidKey(String filekey) {
        return key_to_file.containsKey(filekey);
    }

    /**
     * Adds an entry to the table of files. Given the path of a file that have
     * been added to the repository, this method adds the file to table of files
     * and assign it a new key that can be used as an unique identifier of the
     * file by the REST resources
     *
     * @param file The {@code Path} to be added
     * @return The key of the new file
     */
    public synchronized String addEntry(Path file) {
        String key = computeFileKey(file);
        Path prev = key_to_file.put(key, file);
        if (prev != null) //TODO Handle possible filekey collisions!
        {
            logger.warn("Key collision has occurred");
        }
        return key;
    }

    /**
     * Removes an entry from the table of files Give a key, it removes the
     * associated file from the table of files. Once the entry has been removed,
     * the key is not valid until a new file will be assigned to it.
     *
     * @param key The key of the file to be removed
     * @return
     * @throws InvalidKeyException
     */
    public synchronized Path removeEntry(String key) throws InvalidKeyException {
        Path prev = key_to_file.remove(key);
        if (prev == null) {
            throw new InvalidKeyException();
        }
        return prev;
    }

    /**
     * Updates an entry into the table of files.
     *
     * @param key key of the file
     * @param file path to the file
     * @throws java.security.InvalidKeyException
     */
    public synchronized void updateEntry(String key, Path file) throws InvalidKeyException {
        if (!key_to_file.containsKey(key)) {
            throw new InvalidKeyException();
        }
        //overwrite old entry
        key_to_file.forcePut(key, file);
    }

    /**
     * Returns the key of file stored in the repository.
     *
     * @param file The {@code Path} of the file in the repository
     * @return The key of the file
     * @throws FileNotFoundException
     */
    public synchronized String getKeyFromFilePath(Path file) throws FileNotFoundException {
        String key = key_to_file.inverse().get(file);
        if (key == null) {
            throw new FileNotFoundException("File not found in the repository");
        }
        return key;
    }

    /**
     * Returns the {@code Path} of the file associated with a given key
     *
     * @param key Key of the file
     * @return An instance of {@code Path}
     * @throws InvalidKeyException
     */
    public synchronized Path getFilePathFromKey(String key) throws InvalidKeyException {
        Path filePath = key_to_file.get(key);
        if (filePath == null) {
            throw new InvalidKeyException("File key not found in the repository");
        }
        return filePath;
    }

    /**
     * Adds/replaces a file to the repository, returning the key of the file.
     * Given the user_id, the {@code DirType} of the file and the filename, the
     * {@code Path} of the file is computed and a new entry is inserted into the
     * file table. If a file with the same {@code Path} was already stored into
     * the repository, the old key is returned. The real path of the added or
     * replaced file can be accessed by passing the returned file key to the
     * {@code getFilePathFromKey} method.
     *
     * @param filename Name of the file to be added or replaced
     * @param dirName
     * @return The key of the file
     */
    public synchronized String addFileToRepository(String filename, String name, String dirName) {
        Path filePath = Paths.get(ROOT_DIR,
                name,
                dirName,
                filename);

        //check if the parent directory exists
        File parentDir = filePath.getParent().toFile();
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }

        String key = key_to_file.inverse().get(filePath);
        if (key == null) {
            key = addEntry(filePath);
        }
        return key;

    }

    /**
     * Computes the tree of folders and files in the repository that are
     * accessible to a give user.
     *
     * @param userId The id of the user
     * @param dirType
     * @return An instance of {@code GQLFile}
     */
    public synchronized GMQLFile getRepositoryTree(String userId, GMQLFileTypes dirType) {

        GMQLFile gql_file_root
                = new GMQLFile(rootPath.getFileName().toString(),
                "0", true);
        try {
            logger.info("walking the three " + rootPath);
            logger.info(userId);
            logger.info(dirType.toString());
            logger.info(gql_file_root.getFilename());
            Files.walkFileTree(rootPath,
                    new GMQLFileTreeVisitor(gql_file_root, key_to_file, userId, dirType));
            logger.info("done");
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
        }
        return gql_file_root;
    }


    /**
     * Computes the tree of folders and files in the repository that are
     * accessible to a give user.
     *
     * @param userId The id of the user
     * @param dirType
     * @return An instance of {@code GQLFile}
     */
    public synchronized GMQLFile getRepositoryTree(String userId, GMQLFileTypes dirType, boolean skipQueriesFolder) {

        GMQLFile gql_file_root
                = new GMQLFile(rootPath.getFileName().toString(),
                "0", true);
        logger.info("\n\n\n\n skdjnaklsd \n\n\n\n");
        try {
            Files.walkFileTree(rootPath,
                    new GMQLFileTreeVisitor(gql_file_root, key_to_file, userId, dirType,skipQueriesFolder));
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
        }
        return gql_file_root;
    }

    /**
     * Computes the key for a given file {@code Path}
     *
     * @param file The {@code Path} of the file
     * @return The key of the file
     */
    public static String computeFileKey(Path file) {
        return hashFun.newHasher()
                .putString(file.toString(), Charsets.UTF_8)
                .hash().toString();
    }

    /**
     *
     * @author Massimo
     *
     */
    private static class GlobalFileVisitor extends SimpleFileVisitor<Path> {

        BiMap<String, Path> key_to_file = null;

        public GlobalFileVisitor(BiMap<String, Path> key_to_file) {
            this.key_to_file = key_to_file;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                throws IOException {
            key_to_file.put(computeFileKey(file), file);
            System.out.println(file.toString());
            return FileVisitResult.CONTINUE;
        }
    }
}