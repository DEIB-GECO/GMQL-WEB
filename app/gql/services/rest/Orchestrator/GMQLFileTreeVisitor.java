package gql.services.rest.Orchestrator;

/**
 * Created by abdulrahman on 14/02/2017.
 */

        import com.google.common.collect.BiMap;
        import com.google.common.io.Files;
        import java.io.IOException;
        import java.nio.file.FileVisitResult;
        import java.nio.file.Path;
        import java.nio.file.SimpleFileVisitor;
        import java.nio.file.attribute.BasicFileAttributes;
        import java.util.Objects;
        import org.slf4j.Logger;
        import org.slf4j.LoggerFactory;

/**
 * Browses the repository to look for all files of a passed {@code GMQLFileTypes}
 * that can be accessed by a given user.
 *
 * @author Massimo Quadrana <massimo.quadrana at polimi.it>
 */
public class GMQLFileTreeVisitor extends SimpleFileVisitor<Path> {

    private final static Logger logger = LoggerFactory.getLogger(SimpleFileVisitor.class);
    private BiMap<String, Path> key_to_file = null;
    private GMQLFile root = null;
    private GMQLFile current = null;
    //TODO make the authentication work here!
    private String userName;
    private final GMQLFileTypes fileType;
    private boolean skipQueriesFolder = false;

    /**
     *
     * @param root
     * @param key_to_file
     * @param userId
     * @param fileType
     */
    public GMQLFileTreeVisitor(GMQLFile root,
                               BiMap<String, Path> key_to_file,
                               String userId,
                               GMQLFileTypes fileType) {
        this.root = root;
        this.current = this.root;
        this.key_to_file = key_to_file;
        this.userName = userId;
        this.fileType = fileType;
    }

    public GMQLFileTreeVisitor(GMQLFile root,
                               BiMap<String, Path> key_to_file,
                               String userId,
                               GMQLFileTypes fileType,
                               boolean skipQueriesFolder) {
        this.root = root;
        this.current = this.root;
        this.key_to_file = key_to_file;
        this.userName = userId;
        this.fileType = fileType;
        this.skipQueriesFolder = skipQueriesFolder;
    }

    /**
     *
     * @param dir
     * @param attrs
     * @return
     * @throws IOException
     */
    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
            throws IOException {
        GMQLFile gqlDir = new GMQLFile(dir.getFileName().toString(), "0", true);

//        logger.info("Pre visi directory " + dir);
        //this check avoids tto explore the root folder twice
        if (!Objects.equals(gqlDir.getFilename(), root.getFilename())) {
            //check if the user has access to this folder
            if (current == root
                    && !(Objects.equals(gqlDir.getFilename(), "public")
                    || Objects.equals(gqlDir.getFilename(), userName))) {
                return FileVisitResult.SKIP_SUBTREE;
            }
            if (skipQueriesFolder && Objects.equals(gqlDir.getFilename(), "queries")) {
                return FileVisitResult.SKIP_SUBTREE;
            }

            if (Objects.equals(gqlDir.getFilename(), "results")) {
                return FileVisitResult.SKIP_SUBTREE;
            }
            if (Objects.equals(gqlDir.getFilename(), "regions")) {
                return FileVisitResult.SKIP_SUBTREE;
            }

            //Once in zone accessible the user, look for the requested folder
            /*if(current != root &&
             fileType != GMQLFileTypes.ANY &&
             !Objects.equals(gqlDir.getFilename(), fileType.getExtension()))
             return FileVisitResult.SKIP_SUBTREE;
             */
            gqlDir.setParent(current);
            current.addChild(gqlDir);
            current = gqlDir;
        }
        return FileVisitResult.CONTINUE;
    }

    /**
     *
     * @param dir
     * @param exc
     * @return
     * @throws IOException
     */
    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc)
            throws IOException {
        GMQLFile visited = current;
        current = visited.getParent();
        //hide empty directories

        if (visited.isEmpty()) {
            try{ current.removeChild(visited);

            }catch(Exception ex)
            {
                logger.error(ex.getMessage(),ex);
            }
        }

        return FileVisitResult.CONTINUE;
    }

    /**
     *
     * @param file
     * @param attrs
     * @return
     * @throws IOException
     */
    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
            throws IOException {

        if (fileType == GMQLFileTypes.ANY || fileType.getExtension().equals(Files.getFileExtension(file.getFileName().toString()))) {
            if (key_to_file.inverse().get(file) == null){
                logger.warn("File not found in the memory list : "+file.toString()+"\n\tUpdating the List....");
                GMQLRepositoryV0 gmqlRepo = GMQLRepositoryV0.getInstance();
                gmqlRepo.addEntry(file);
            }
            GMQLFile gqlFile = new GMQLFile(file.getFileName().toString(),
                    key_to_file.inverse().get(file).toString(), false);

            gqlFile.setParent(current);
            current.addChild(gqlFile);
            return FileVisitResult.CONTINUE;
        } else {
            return FileVisitResult.SKIP_SUBTREE;
        }

    }
}
