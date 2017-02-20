package gql.services.rest.Orchestrator;

import com.google.common.base.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;

import java.io.*;
import java.nio.file.*;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.InvalidKeyException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Created by abdulrahman on 14/02/2017.
 */
public final class GMQLFileUtils {

    private final static String FIX_COMMAND = "dos2unix";

    /**
     * Processes an experiment file to extract the mapping from unique
     * attributes to their set of values
     *
     * @param experimentFilePath {@code String} full path to the experiment file
     * @return A {@code Map} from {@code String} attribute name to
     * {@code Set<String>} of value names
     * @throws gql.services.rest.Orchestrator.GMQLServiceException if the experiment file cannot be found
     */
    public static Map<String, Set<String>> buildAttributeToValueMap(final String experimentFilePath) throws gql.services.rest.Orchestrator.GMQLServiceException {

        Map<String, Set<String>> attr_to_value = new HashMap<>();
        String line = null;
        final Pattern pattern = Pattern.compile("\t");

        try (BufferedReader br = new BufferedReader(new FileReader(experimentFilePath))) {

            //experiments' metadata are stored in the format: id[\t]attribute[\t]value
            //precompile the pattern to speed-up the parsing
            while ((line = br.readLine()) != null) {
                //split each file line
                String[] fields = pattern.split(line);
                //update the attr_to_value map
                Set<String> values = attr_to_value.get(fields[1]);
                if (values == null) {
                    values = new HashSet<>();
                    attr_to_value.put(fields[1], values);
                }
                values.add(fields[2]);

            }
            br.close();
        } catch (FileNotFoundException e) {
            throw new gql.services.rest.Orchestrator.GMQLServiceException("File " + Paths.get(experimentFilePath).getFileName() + " not found in the repository.");
        } catch (NumberFormatException | IOException e) {
            throw new gql.services.rest.Orchestrator.GMQLServiceException("An error has occurred while parsing file " + Paths.get(experimentFilePath).getFileName() + ".");
        }
        return attr_to_value;
    }

    /**
     * Processes an experiment file to extract the mapping from {@code Metadata}
     * to experiments' ids
     *
     * @param experimentFilePath {@code String} full path to the experiment file
     * @return A {@code Map} from {@code Metadata} to {@code Set<String>}
     * representing the set of experiments' ids having such {@code Metadata}
     * @throws gql.services.rest.Orchestrator.GMQLServiceException if the experiment file cannot be found
     */
    public static Map<Metadata, Set<String>> buildMetadataToIdMap(final String experimentFilePath) throws gql.services.rest.Orchestrator.GMQLServiceException {

        Map<Metadata, Set<String>> metadata_to_id = new HashMap<>();
        String line = null;
        final Pattern pattern = Pattern.compile("\t");

        try (BufferedReader br = new BufferedReader(new FileReader(experimentFilePath))) {
            //experiments' metadata are stored in the format: id[\t]attribute[\t]value
            //precompile the pattern to speed-up the parsing
            while ((line = br.readLine()) != null) {
                //split each file line
                String[] fields = pattern.split(line);
                String id = fields[0];
                Metadata metadata = new Metadata(fields[1], fields[2]);

                //update metadata_to_id map
                Set<String> id_set = metadata_to_id.get(metadata);
                if (id_set == null) {
                    id_set = new HashSet<>();
                    metadata_to_id.put(metadata, id_set);
                }
                id_set.add(id);

            }
            br.close();
        } catch (FileNotFoundException e) {
            throw new gql.services.rest.Orchestrator.GMQLServiceException("File " + Paths.get(experimentFilePath).getFileName() + " not found in the repository.");
        } catch (NumberFormatException | IOException e) {
            throw new gql.services.rest.Orchestrator.GMQLServiceException("An error has occurred while parsing file " + Paths.get(experimentFilePath).getFileName() + ".");
        }
        return metadata_to_id;
    }

    /**
     * Processes an experiment file to extract the mapping from experiments' ids
     * to their {@code Metadata}
     *
     * @param experimentFilePath {@code String} full path to the experiment file
     * @return A {@code Map} from {@code String} experiments' ids to
     * {@code Set<Metadata>} of their metadata
     * @throws gql.services.rest.Orchestrator.GMQLServiceException if the experiment file cannot be found
     */
    public static Map<String, Set<Metadata>> buildIdToMetadataMap(final String experimentFilePath) throws gql.services.rest.Orchestrator.GMQLServiceException {

        Map<String, Set<Metadata>> id_to_metadata = new HashMap<>();
        String line = null;
        final Pattern pattern = Pattern.compile("\t");

        try (BufferedReader br = new BufferedReader(new FileReader(experimentFilePath))) {
            //experiments' metadata are stored in the format: id[\t]attribute[\t]value
            //precompile the pattern to speed-up the parsing
            while ((line = br.readLine()) != null) {
                //split each file line
                String[] fields = pattern.split(line);
                String id = fields[0];
                Metadata metadata = new Metadata(fields[1], fields[2]);

                //update id_to_metadata map
                Set<Metadata> metadata_set = id_to_metadata.get(id);
                if (metadata_set == null) {
                    metadata_set = new HashSet<>();
                    id_to_metadata.put(id, metadata_set);
                }
                metadata_set.add(metadata);

            }
            br.close();
        } catch (FileNotFoundException e) {
            throw new gql.services.rest.Orchestrator.GMQLServiceException("File " + Paths.get(experimentFilePath).getFileName() + " not found in the repository.");
        } catch (NumberFormatException | IOException e) {
            throw new gql.services.rest.Orchestrator.GMQLServiceException("An error has occurred while parsing file " + Paths.get(experimentFilePath).getFileName() + ".");
        }
        return id_to_metadata;
    }

    /**
     * Returns the actual path for a given filekey, looking at the current state
     * of the {@code GMQLRepository}
     *
     * @param filekey Key of the file
     * @return An instance of {@code java.nio.file.Path}
     * @throws InvalidKeyException
     */
    public static java.nio.file.Path getPathFromFileKey(String filekey) throws InvalidKeyException {
        return GMQLRepositoryV0.getInstance().getFilePathFromKey(filekey);
    }

    /**
     * Writes the file to a specified location
     *
     * @param inputStream The instance of {@code InputStream} to be written
     * @param fileLocation The path where the file will be saved
     * @throws java.io.IOException
     */
    public static void writeToFile(InputStream inputStream,
                                   java.nio.file.Path fileLocation) throws IOException {

        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, Charsets.UTF_8));
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileLocation.toFile()), Charsets.UTF_8));
        int read;
        char[] cb = new char[1024];
        while ((read = br.read(cb)) != -1) {
            bw.write(cb, 0, read);
        }
        bw.flush();
        bw.close();
        inputStream.close();

        //fix file format to be UNIX like
        gql.services.rest.Orchestrator.GMQLFileUtils.fixFileFormat(fileLocation);
    }

    /**
     * Writes a string to a specified file
     *
     * @param sr The instance of {@code StringReader} to be written
     * @param fileLocation The path where the file will be saved
     * @throws java.io.IOException
     */
    public static void writeToFile(StringReader sr,
                                   java.nio.file.Path fileLocation) throws IOException {

        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileLocation.toFile()), Charsets.UTF_8))) {
            int read;
            char[] cb = new char[1024];
            while ((read = sr.read(cb)) != -1) {
                bw.write(cb, 0, read);
            }
            bw.flush();
            bw.close();
            sr.close();
        }

        //fix file format to be UNIX like
        gql.services.rest.Orchestrator.GMQLFileUtils.fixFileFormat(fileLocation);

    }

    /**
     * Converts generic array to string
     *
     * @param <T>
     * @param array
     * @param separator
     * @return
     */
    public static <T> String arrayToString(T[] array, String separator) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < array.length - 1; ++i) {
            sb.append(array[i].toString())
                    .append(separator);
        }
        sb.append(array[array.length - 1]);
        return sb.toString();
    }

    /**
     * Converts a text file to an UNIX ASCII text file
     *
     * @param filePath The path of the file
     */
    public static void fixFileFormat(Path filePath) {
        try {
//            String[] cmdArray = {FIX_COMMAND, filePath.toString()};
//            Runtime.getRuntime().exec(cmdArray);

            String DisStr = filePath.toFile().toString() + "1";
            File dis = Paths.get(DisStr).toFile();

            try (PrintWriter writer = new PrintWriter(dis, "UTF-8")) {
                BufferedReader reader = new BufferedReader(new FileReader(filePath.toFile()));
                String line = null;
                while ((line = reader.readLine()) != null) {
                    line = line.replaceAll("\r\n", "\n");
                    line = line.replaceAll("\r", "\n");
                    writer.println(line);
                }
            }

            Files.delete(filePath);
            dis.renameTo(filePath.toFile());
        } catch (IOException ex) {
            Logger.getLogger(GMQLFileUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Deletes a directory from the local file system
     *
     * @param dirPath Path to the directory
     * @param recursive Set true for recursive delete, false otherwise
     * @throws IOException
     */
    public static void deleteLocalDirectory(Path dirPath, boolean recursive) throws IOException {
        if (recursive) {
            Files.walkFileTree(dirPath, new GMQLFileUtils.DeleteLocalDirectoryRecursive());
        } else {
            Files.delete(dirPath);
        }
    }

    private static class DeleteLocalDirectoryRecursive extends SimpleFileVisitor<Path> {

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            Files.delete(dir);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
        }

    }

    private static final Configuration conf = new Configuration();

    static {
        conf.addResource(
                new org.apache.hadoop.fs.Path("/usr/local/hadoop/etc/hadoop/core-site.xml"));
    }

    /**
     * Creates a directory under the HDFS
     *
     * @param path Path to the directory
     * @return true if the directory has been successfully created, false
     * otherwise
     * @throws IOException
     */
    public static boolean mkdirHDFS(String path) throws IOException {
        org.apache.hadoop.fs.FileSystem fs = org.apache.hadoop.fs.FileSystem.get(conf);
        org.apache.hadoop.fs.Path dirPath = new org.apache.hadoop.fs.Path(path);
        return fs.mkdirs(dirPath);
    }

    /**
     * Copies a directory from the HDFS to the local file system
     *
     * @param sourcePath Path to the source directory
     * @param destPath Path to the destination directory
     * @throws IOException
     */
    public static void copyDirectoryToLocal(String sourcePath, String destPath) throws IOException {
        org.apache.hadoop.fs.FileSystem fs = org.apache.hadoop.fs.FileSystem.get(conf);
        org.apache.hadoop.fs.Path sourceDirPath = new org.apache.hadoop.fs.Path(sourcePath);
        org.apache.hadoop.fs.Path destDirPath = new org.apache.hadoop.fs.Path(destPath);
        fs.copyToLocalFile(sourceDirPath, destDirPath);
    }

    //TODO: Move to configuration file

    /**
     *
     */
    public final static String RESULT_DIR_LOCAL = utils.GMQL_Globals.apply().ut().GMQLHOME()+"/data/user_%s/queries/results/%s";

    /**
     *
     */
    public final static String RESULT_DIR_MAPREDUCE = utils.GMQL_Globals.apply().ut().HDFSRepoDir()+"/results/user_%s/%s";

    /**
     *
     */
    public final static String PIG_PART_FILE_PATTERN = "part-*";

    private static final String HADOOP_CMD = "hadoop";
    private static final String HADOOP_FS_FLAG = "fs";

    /**
     * Creates a directory under the HDFS with command line command
     *
     * @param path Path to the directory
     * @return true if the directory has been successfully created, false
     * otherwise
     * @throws IOException
     * @throws java.lang.InterruptedException
     */
    public static int mkdirHDFScmd(String path) throws IOException, InterruptedException {
        String cmdArray[] = {
                HADOOP_CMD,
                HADOOP_FS_FLAG,
                "-mkdir",
                path
        };

        return runCmd(cmdArray);
    }

    /**
     * Copies a directory from the HDFS to the local file system with command
     * line command
     *
     * @param sourcePath Path to the source directory
     * @param destPath Path to the destination directory
     * @return
     * @throws IOException
     * @throws java.lang.InterruptedException
     */
    public static int copyDirectoryToLocalcmd(String sourcePath, String destPath) throws IOException, InterruptedException {
        String cmdArray[] = {
                HADOOP_CMD,
                HADOOP_FS_FLAG,
                "-copyToLocal",
                sourcePath,
                destPath
        };
        Logger.getLogger(gql.services.rest.Orchestrator.GMQLFileUtils.class.getName()).log(Level.INFO, arrayToString(cmdArray, " "));

        return runCmd(cmdArray);
    }

    /**
     *
     * @param srcDir
     * @param srcPattern
     * @param destFile
     * @param deleteSrcFiles
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static void mergeFilesLocal(File srcDir, final String srcPattern, File destFile, boolean deleteSrcFiles) throws FileNotFoundException, IOException {
        File[] fileList = srcDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.matches(srcPattern.replace(".", "\\.").replace("*", ".*"));

            }
        });

        OutputStream os = new FileOutputStream(destFile, true);
        for (File f : fileList) {
            IOUtils.copy(new FileInputStream(f), os);
        }
        //remove source files
        if (deleteSrcFiles) {
            for (File f : fileList) {
                f.delete();
            }
        }

    }

    /**
     *
     * @param srcDir
     * @param srcPattern
     * @param destFile
     * @param deleteSrcFiles
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public static int mergeFilesHDFScmd(String srcDir, String srcPattern, File destFile, boolean deleteSrcFiles) throws IOException, InterruptedException {
        String cmdArray[] = {
                HADOOP_CMD,
                HADOOP_FS_FLAG,
                "-cat",
                srcDir + srcPattern
        };
        System.out.println(arrayToString(cmdArray, " "));
        int exitCode;
        try (OutputStream outputOs = new FileOutputStream(destFile)) {
            exitCode = runCmd(cmdArray, outputOs, null);
        }
        if (exitCode == 0 && deleteSrcFiles) {
            //call the remove command
            cmdArray[2] = "-rm";
            runCmd(cmdArray);
        }
        return exitCode;
    }

    private static int runCmd(String[] cmdArray) throws IOException, InterruptedException {
        return runCmd(cmdArray, null, null);
    }

    private static int runCmd(String[] cmdArray, OutputStream outputOs, OutputStream errorOs) throws IOException, InterruptedException {
        Runtime runtime = Runtime.getRuntime();
        Process proc = runtime.exec(cmdArray);
        StreamGobbler errorGobbler = new StreamGobbler(proc.getErrorStream(),"ERROR >" ,errorOs);
        StreamGobbler outputGobbler = new StreamGobbler(proc.getInputStream(),"GMQL >", outputOs);

        // run two threads to flush output and error streams
        // This will cause the process to hang if the streams are not properly flushed!!!
        errorGobbler.start();
        outputGobbler.start();
        int procExitCode = proc.waitFor();
        errorGobbler.join();
        outputGobbler.join();

        return procExitCode;
    }

}
