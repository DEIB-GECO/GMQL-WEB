/*
 * Copyright (C) 2014 Francesco Venco
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package gql.services.rest;

import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

import java.io.*;
import java.security.InvalidKeyException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.xml.bind.JAXBException;

import it.polimi.genomics.core.DataStructures.IRDataSet;
import it.polimi.genomics.core.ParsingType;
import it.polimi.genomics.repository.FSRepository.DFSRepository;
import it.polimi.genomics.repository.FSRepository.RFSRepository;
import it.polimi.genomics.repository.GMQLRepository.GMQLSample;
import it.polimi.genomics.repository.GMQLRepository.GMQLSchemaTypes;
import orchestrator.entities.Attribute;
import orchestrator.entities.AttributeList;
import orchestrator.entities.GMQLSchema;
import orchestrator.entities.GMQLSchemaCollection;
import orchestrator.entities.GMQLSchemaField;
import orchestrator.repository.GMQLRepository;
import orchestrator.repository.RepositoryManagerV1;
import orchestrator.services.GQLServiceException;
import orchestrator.util.GQLFileUtils;
import orchestrator.util.Utilities;
import org.apache.commons.io.FileUtils;
import play.libs.Scala;
import scala.Tuple2;

/**
 * @author Francesco Venco
 */
@Path("/datasets/")
public class DataSetsManager {

    private static final Utilities ut = Utilities.getInstance();

    private static it.polimi.genomics.repository.GMQLRepository.GMQLRepository repository = new DFSRepository();
    //private final String tempFolderRoot = File.separator + "home" + File.separator + "gql_repository" + File.separator + "tmp";
    //private final String dataFolderRoot = File.separator + "home" + File.separator + "gql_repository" + File.separator + "data";
    public static final String tempFolderRoot = ut.GMQLHOME + File.separator + "tmp";
    public static final String dataFolderRoot = ut.GMQLHOME + File.separator + "data";

    @GET
    @Path("/listAll/{username}")
    public Response listAllDataSets(@PathParam("username") String sc) throws GQLServiceException, InvalidKeyException {
        System.out.println("username: " + sc);

        String user = sc;

//        RepositoryManagerV1 rm = new RepositoryManagerV1(user);

        List<Attribute> dataSetList = new ArrayList<>();
        for (IRDataSet ds : repository.ListAllDSs(user) /*rm.ListAllDataSets(user)*/) {
            dataSetList.add(new Attribute(ds.position()));
        }

        for (IRDataSet ds : repository.ListAllDSs("public") /*rm.ListAllDataSets(user)*/) {
            dataSetList.add(new Attribute("public." + ds.position()));
        }

        AttributeList datasets = new AttributeList(dataSetList);

        return Response.ok(datasets).build();
    }

    @GET
    @Path("/listDSSamples/{dataset}/{username}")
    public Response listDataSetSamples(@PathParam("dataset") String dataset, @PathParam("username") String sc) throws GQLServiceException, InvalidKeyException {

        String user = sc;
        RepositoryManagerV1 rm = new RepositoryManagerV1(user);

        List<Attribute> dataSetList = new ArrayList<>();
        for (ArrayList<String> temp : rm.ListDSSamples(dataset, user)) {
            Logger.getLogger(DataSetsManager.class.getName()).log(Level.INFO, "ListDSSamples name " + temp);
            dataSetList.add(new Attribute(temp.get(0), temp.get(1)));
        }

        AttributeList datasets = new AttributeList(dataSetList);
//        if (dataSetList.size() > 0)
            return Response.ok(datasets).build();
//        else
//            return Response.ok("empty DS").build();
    }


    @GET
    @Path("/registerUser/{username}")
    public Response registerUser(@PathParam("username") String sc) throws GQLServiceException, InvalidKeyException {

        String user = sc;
        RepositoryManagerV1 rm = new RepositoryManagerV1(user);

        if (rm.registerUser(user))
            return Response.ok("Registered").build();
        else return Response.serverError().build();
    }

    @GET
    @Path("/unRegisterUser/{username}")
    public Response unRegisterUser(@PathParam("username") String sc) throws GQLServiceException, InvalidKeyException {

        String user = sc;
        RepositoryManagerV1 rm = new RepositoryManagerV1(user);

        if (rm.unregisterUser(user)) {
            return Response.ok("UnRegistered").build();
        } else return Response.ok("UserOld could not be deleted.").build();


    }


    @POST
    @Path("/delete/{dataSetName}/{username}")
    public Response deleteDataSet(@PathParam("username") String sc, @PathParam("dataSetName") String dataSetName)
            throws GQLServiceException, InvalidKeyException, InterruptedException, IOException {

        String user = sc;
        //RepositoryManagerV1 rm = new RepositoryManagerV1(user);
        if (dataSetName.startsWith("public")) {
            //TODO better signaling on errors... this is very rough!!!
            return Response.noContent().build();
        }

        //TODO remove on go with the clean version when permissions have been granted!
        //Runtime runtime = Runtime.getRuntime();
        //Process proc;
        //String Command = " repositoryManagerV1 deleteds " + dataSetName + " " + user;
        /*String Command = "sudo su " + user + " repositoryManagerV1 deleteds " + dataSetName;
         proc = runtime.exec(Command);
         proc.waitFor();*/
        repository.DeleteDS(dataSetName, user);
//        RepositoryManagerV1 rm = new RepositoryManagerV1(user);
//        boolean ret = rm.DeleteDS(dataSetName, user);
//        if (!ret) {
//            return Response.serverError().build();
//        }

        //now remove zip files in the tmp folder, and local files if any
        String directory = tempFolderRoot + File.separator + user + File.separator + dataSetName;
        File zipFile = new File(directory + ".zip");
        String localDir = checkOrCreateRegionsDirectory(user) + File.separator + dataSetName;
        File dirTemp = new File(directory);
        File dirRegions = new File(localDir);
        if (dirTemp.exists()) {
            FileUtils.deleteDirectory(dirTemp);
        }
        if (dirRegions.exists()) {
            try {
                FileUtils.deleteDirectory(dirRegions);
            } catch (Exception e) {
            }
        }
        zipFile.delete();

        return Response.ok("OK").build();
    }

    public static String prepareFile(String id, String user,
                                   String dataSetName) throws IOException, InvalidKeyException, InterruptedException {
        String directory = tempFolderRoot + id  + File.separator + user + File.separator + dataSetName;
        File dir = new File(directory);
        dir.mkdirs();

        if (dir.list() != null)
            repository.exportDsToLocal(dataSetName, user, directory);
        return directory;
    }


    @POST
    @Path("/prepare/{dataSetName}/{clean}/{username}")
    public Response prepareFileZip(@PathParam("username") String sc,
                                   @PathParam("dataSetName") String dataSetName,
                                   @PathParam("clean") String clean) throws IOException, InvalidKeyException, InterruptedException {

        String user = sc;
        checkOrCreateTempDirectory(user);
        String directory = tempFolderRoot + File.separator + user + File.separator + dataSetName;

        File dir = new File(directory);
        File zipFile = new File(directory + ".zip");

        if (clean.equals("true")) {
            FileUtils.deleteDirectory(dir);
            zipFile.delete();
        }

        //is the zipFile already there? Is the process of copiing already ongoing?

        if (zipFile.exists() && dir.list() == null) {
            return Response.ok("oldZip").build();
        } else if (dir.list() != null) {
            return Response.ok("inProgress").build();
        }

        prepareFile("", user, dataSetName);
//        repository.exportDsToLocal(dataSetName, user, directory);
//        RepositoryManagerV1 rm = new RepositoryManagerV1(user);
//        rm.CopyDSSamplesToLocal(dataSetName, directory, user);


        //create vocabulary
        String[] fileNames = new File(directory).list();
        HashMap<String, HashSet<String>> vocabulary = new HashMap<>();
        for (String fileName : fileNames) {
            if (!fileName.endsWith(".meta")) {
                continue;
            }
            File metaFile = new File(directory + File.separator + fileName);
            try (
                    BufferedReader br = new BufferedReader(new FileReader(metaFile))) {
                String line = br.readLine();
                while (line != null) {
                    Scanner lineS = new Scanner(line);
                    String name = lineS.next();
                    String value = lineS.next();
                    if (vocabulary.containsKey(name)) {
                        vocabulary.get(name).add(value);
                    } else {
                        HashSet<String> values = new HashSet<>();
                        values.add(value);
                        vocabulary.put(name, values);
                    }
                    line = br.readLine();
                }
                br.close();
            }
        }
        if (!vocabulary.isEmpty()) {
            try (PrintWriter writer = new PrintWriter(tempFolderRoot + File.separator + user + File.separator + dataSetName + File.separator + dataSetName + ".vocabulary.txt", "UTF-8")) {
                //order keys
                SortedSet<String> names = new TreeSet(vocabulary.keySet());
                for (String name : names) {
                    writer.println(name);
                    //order values
                    SortedSet<Object> values = new TreeSet(vocabulary.get(name));
                    for (Object value : values) {
                        writer.println("\t" + value);
                    }
                }
                writer.close();
            }
        }


        //copy also the schema - to facilitate a new upload in case
        try {
            String _user = user;
            String _name = dataSetName;
            if (dataSetName.startsWith("public.")) {
                _name = dataSetName.replace("public.", "");
                _user = "public";
            }
            File schemaOrigin = new File(dataFolderRoot + File.separator + _user + File.separator + "schema" + File.separator + _name + ".schema");
            File schemaDest = new File(tempFolderRoot + File.separator + user + File.separator + dataSetName + File.separator + _name + ".schema");
            FileUtils.copyFile(schemaOrigin, schemaDest);
        } catch (Exception e) {
        }


        fileNames = new File(directory).list();
        if (fileNames == null || fileNames.length < 2) {
            try {
                FileUtils.deleteDirectory(dir);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            return Response.noContent().build();
        }

        zipFile.createNewFile();
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            int read;
            byte[] cb = new byte[4096];
            for (String fileName : fileNames) {
                if (fileName.endsWith(".crc")) {
                    continue;
                }
                File requestedFile = new File(directory + File.separator + fileName);
                FileInputStream fis = new FileInputStream(requestedFile);
                ZipEntry zipEntry = new ZipEntry(requestedFile.getName());
                zos.putNextEntry(zipEntry);
                while ((read = fis.read(cb)) != -1) {
                    zos.write(cb, 0, read);
                }
                fis.close();
                zos.closeEntry();
            }
            zos.close();
        }

        //remove temp files
        try {
            FileUtils.deleteDirectory(dir);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }


        return Response.ok("ok").build();
        //Set the header and send the response*/

    }

    @GET
    @Path("/zip/{dataSetName}/{username}")
    public Response downloadFileZip(@PathParam("username") String sc,
                                    @PathParam("dataSetName") String dataSetName) throws FileNotFoundException, IOException, InvalidKeyException, InterruptedException {
        String user = sc;
        checkOrCreateTempDirectory(user);
        String directory = tempFolderRoot + File.separator + user + File.separator + dataSetName + ".zip";
        File zipFile = new File(directory);
        Logger.getLogger(DataSetsManager.class.getName()).log(Level.INFO, "ZipFile " + zipFile.getAbsolutePath());
        //Set the header and send the response
        return Response.ok((Object) zipFile)
                .header("Content-Disposition", "attachment; filename=" + zipFile.getName())
                .build();
    }

    @POST
    @Path("/uploadSamples/{dataSetName}/{status}/{username}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadSample(@PathParam("username") String sc,
                                 @FormDataParam("file") InputStream uploadedInputStream,
                                 @PathParam("dataSetName") String dataSetName,
                                 @PathParam("status") String status,
                                 @FormDataParam("file") FormDataContentDisposition fileDetails) {

        String responce = "ok";
        String user = sc;
        checkOrCreateTempDirectory(user);
        String tempDirPath = tempFolderRoot + File.separator + user + File.separator + "upload" + File.separator + dataSetName;
        //String localDataSetPath = checkOrCreateRegionsDirectory(user) + File.separator + dataSetName;
        File regionsDir = new File(tempDirPath);

        try {
            if (!regionsDir.getParentFile().exists()) {
                regionsDir.getParentFile().mkdir();
            }

            if (status.equals("first")) {
                //cleand and make directory again
                if (regionsDir.exists() || regionsDir.isDirectory()) {
                    try {
                        FileUtils.cleanDirectory(regionsDir);
                        FileUtils.deleteDirectory(regionsDir);
                    } catch (IOException ex) {
                        //Logger.getLogger(DataSetsManager.class.getName()).log(Level.SEVERE, null, ex);
                    }

                }
                regionsDir.mkdir();
            }

            if (uploadedInputStream == null) {
                System.out.println("uploadedInputStream is null");
            } else {
                System.out.println(uploadedInputStream.toString());
            }

            if (fileDetails == null) {
                System.out.println("File details is null");
            } else {
                System.out.println(fileDetails.toString());
            }

            //upload the file
            if (checkFile(uploadedInputStream, fileDetails)) {

                File newFile;
                if (status.equals("schema")) {
                    newFile = new File(tempDirPath + ".schema");
                } else {
                    newFile = new File(tempDirPath + File.separator + fileDetails.getFileName());
                }

                GQLFileUtils.writeToFile(uploadedInputStream, newFile.toPath());
                //Runtime.getRuntime().exec("chmod 777 " + newFile.getAbsolutePath()).waitFor();
            } else {
                //roll back
                FileUtils.deleteDirectory(regionsDir);
                throw new GQLServiceException("Uploaded file is not valid!");
            }

            return Response.ok(responce).build();
        } catch (Exception e) {
            //roolback
            if (regionsDir.exists() || regionsDir.isDirectory()) {
                try {
                    FileUtils.deleteDirectory(regionsDir);
                } catch (IOException ex) {
                    Logger.getLogger(DataSetsManager.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            return Response.ok("error " + e.getMessage()).build();
        }
    }

    @POST
    @Path("/addSamples/{dataSetName}/{username}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response addSamples(@PathParam("username") String sc,
                               @PathParam("dataSetName") String dataSetName) {

        String responce = "ok";
        String user = sc;
        checkOrCreateTempDirectory(user);
        String tempDirPath = tempFolderRoot + File.separator + user + File.separator + "upload" + File.separator + dataSetName;
        File tempDir = new File(tempDirPath);

        String _localDataSetPath = checkOrCreateRegionsDirectory(user) + File.separator + dataSetName;
        String localDataSetPath = _localDataSetPath + File.separator + "addedSamples1";
        File addedSamplesDir = new File(localDataSetPath);
        int count = 2;
        while (true) {
            if (addedSamplesDir.exists()) {
                localDataSetPath = _localDataSetPath + File.separator + "addedSamples" + count;
                ++count;
                addedSamplesDir = new File(localDataSetPath);
            } else {
                addedSamplesDir.mkdir();
                break;
            }
        }


        //move all files now
        File[] files = tempDir.listFiles();
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            if (file.getName().endsWith(".schema")) {
                continue;
            }
            File dist = new File(localDataSetPath + File.separator + file.getName());
            file.renameTo(dist);
        }


        files = addedSamplesDir.listFiles();
        for (File f : files) {
            try {
                if (f.getName().endsWith(".meta")) {
                    continue;
                }
                RepositoryManagerV1 rm = new RepositoryManagerV1(user);
                rm.AddSampleToDS(dataSetName, f.getAbsolutePath(), user);

            } catch (Exception ex) {
                try {
                    FileUtils.deleteDirectory(addedSamplesDir);
                    FileUtils.deleteDirectory(tempDir);
                } catch (IOException ex1) {
                    Logger.getLogger(DataSetsManager.class.getName()).log(Level.SEVERE, null, ex1);
                }

                Logger.getLogger(DataSetsManager.class.getName()).log(Level.SEVERE, null, ex);
                return Response.serverError().build();
            }
        }
        if (tempDir.exists() || tempDir.isDirectory()) {
            try {
                FileUtils.deleteDirectory(tempDir);
                responce = responce + " temp folder deleted";
            } catch (IOException ex) {
                Logger.getLogger(DataSetsManager.class.getName()).log(Level.SEVERE, null, ex);
                return Response.serverError().build();
            }
        }

        return Response.ok(responce).build();
    }

    @POST
    @Path("/createDataSet/{dataSetName}/{username}")
    public Response createDataSet(@PathParam("username") String sc,
                                  @PathParam("dataSetName") String dataSetName,
                                  @FormParam("schema") String schema,
                                  @FormParam("names") List<String> names,
                                  @FormParam("types") List<String> types) throws JAXBException, FileNotFoundException {
//        try {

            String user = sc;
            String tempDirPath = tempFolderRoot + File.separator + user + File.separator + "upload" + File.separator + dataSetName;
            checkOrCreateTempDirectory(user);
            //move everything from the temp dir path
            String localDataSetPath = checkOrCreateRegionsDirectory(user) + File.separator + dataSetName;

            File tempDir = new File(tempDirPath);
            File regionsDir = new File(localDataSetPath);
            if (regionsDir.exists()) {
                try {
                    FileUtils.deleteDirectory(regionsDir);
                } catch (Exception ex) {
                    Logger.getLogger(DataSetsManager.class.getName()).log(Level.SEVERE, null, ex);
                }
                //return Response.serverError().build();
            }
            regionsDir.mkdir();

            //check for correctness
            HashSet<String> namesChecker = new HashSet<>(names);
            boolean badNames = namesChecker.size() != names.size();
            if (!badNames && schema.equals("gtf")) {
                badNames = namesChecker.contains("score") || namesChecker.contains("feature") || namesChecker.contains("source") || namesChecker.contains("frame");
            }

            int counterStrand = 0;
            for (String s : types) {
                if (s.equals("STRAND")) {
                    counterStrand++;
                }
            }

            boolean moreStrands = counterStrand > 1;

            if (badNames || moreStrands) {
                try {
                    FileUtils.deleteDirectory(new File(localDataSetPath));
                } catch (IOException ex) {
                    Logger.getLogger(DataSetsManager.class.getName()).log(Level.SEVERE, null, ex);
                }
                return Response.ok("errorBadSchema").build();
            }

            if (schema.equals("gtf") || schema.equals("tab")) {

                List<GMQLSchemaField> fields = new ArrayList<>();
                //standard fields for gtf file
                if (schema.equals("gtf")) {
                    GMQLSchemaField source = new GMQLSchemaField();
                    source.setFieldName("source");
                    source.setFieldType("STRING");
                    GMQLSchemaField feature = new GMQLSchemaField();
                    feature.setFieldName("feature");
                    feature.setFieldType("STRING");
                    GMQLSchemaField score = new GMQLSchemaField();
                    score.setFieldName("score");
                    score.setFieldType("FLOAT");
                    GMQLSchemaField frame = new GMQLSchemaField();
                    frame.setFieldName("frame");
                    frame.setFieldType("INTEGER");
                    fields.add(source);
                    fields.add(feature);
                    fields.add(score);
                    fields.add(frame);
                }


                for (int i = 0; i < names.size(); i++) {
                    GMQLSchemaField field = new GMQLSchemaField();
                    field.setFieldName(names.get(i));
                    field.setFieldType(types.get(i));
                    if (field.getFieldType().equals("STRAND")) {
                        field.setFieldName("strand");
                    }
                    fields.add(field);
                }

                GMQLSchema newSchema = new GMQLSchema();
                newSchema.setSchemaName(dataSetName);
                newSchema.setSchemaType(schema);
                newSchema.setFields(fields);
                ArrayList<GMQLSchema> schemaList = new ArrayList<>();
                schemaList.add(newSchema);

                GMQLSchemaCollection outputSchemaCollection = new GMQLSchemaCollection();
                outputSchemaCollection.setCollectionName("GLOBAL_SCHEMAS");
                outputSchemaCollection.setSchemaList(schemaList);

                File resultSchemaFile = new File(tempDirPath + ".schema");
                outputSchemaCollection.bindGMQLSchemaCollection(resultSchemaFile.toPath());

                schema = resultSchemaFile.getAbsolutePath();

            }
            //here is is supposed that the schema has been uploaded yet in the right folder with the right name
            switch (schema) {
                case "UPLOAD":
                    schema = tempDirPath + ".schema";
                    break;
                case "bed":
                    schema = ut.GMQLHOME + File.separator + "conf" + File.separator + "BED.schema";
                    break;
                case "NarrowPeak":
                    schema = ut.GMQLHOME + File.separator + "conf" + File.separator + "NARROWPEAK.schema";
                    break;
                case "BroadPeak":
                    schema = ut.GMQLHOME + File.separator + "conf" + File.separator + "BROADPEAK.schema";
                    break;
                case "bedGraph":
                    schema = ut.GMQLHOME + File.separator + "conf" + File.separator + "BEDGRAPH.schema";
                    break;
                case "vcf":
                    schema = ut.GMQLHOME + File.separator + "conf" + File.separator + "VCF.schema";
                    break;
            }

            //move all files now
            File[] allFiles = tempDir.listFiles();
            for (int i = 0; i < allFiles.length; i++) {
                File file = allFiles[i];
                if (file.getName().endsWith(".schema")) {
                    continue;
                }
                File dist = new File(localDataSetPath + File.separator + file.getName());
                file.renameTo(dist);
            }

//            List<Tuple2<String, scala.Enumeration.Value>> dd= new LinkedList<Tuple2<String, scala.Enumeration.Value>>() ;
//            IRDataSet ds = new IRDataSet(dataSetName,dd);
            File folder = new File(localDataSetPath);
            File[] listOfFiles = folder.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return !name.toLowerCase().endsWith(".meta") && !name.toLowerCase().endsWith(".schema");
                }
            });

            List<it.polimi.genomics.repository.GMQLRepository.GMQLSample> samples = new LinkedList<it.polimi.genomics.repository.GMQLRepository.GMQLSample>();
            for (int i = 0; i < listOfFiles.length; i++) {
                samples.add(new GMQLSample(listOfFiles[i].toString(), listOfFiles[i].toString() + ".meta", null));
            }

            Logger.getLogger(DataSetsManager.class.getName()).log(Level.INFO, "Schema: " + schema);
            repository.importDs(dataSetName, user, samples, schema);
//            RepositoryManagerV1 rm = new RepositoryManagerV1(user);
//            rm.CreateDS(user, dataSetName, schema, "MAPREDUCE", localDataSetPath);


            String toRet = "ok ";
            toRet = toRet + schema + " " + names + " " + types;

            try {
                FileUtils.deleteDirectory(tempDir);
                new File(localDataSetPath + ".schema").delete();
            } catch (Exception ex) {
                Logger.getLogger(DataSetsManager.class.getName()).log(Level.SEVERE, null, ex);
            }
            return Response.ok(toRet).build();
//        } catch (Exception ex) {
//            Logger.getLogger(DataSetsManager.class.getName()).log(Level.SEVERE, null, ex);
//            return Response.serverError().build();
//        }
    }

    private boolean checkFile(InputStream uploadedInputStream,
                              FormDataContentDisposition fileDetails) {
        //TODO: check md5 sum of the uploaded file
        return true;
    }

    private static void checkOrCreateTempDirectory(String user) {
        String tempUserFolderPath = tempFolderRoot + File.separator + user;
        File d = new File(tempUserFolderPath);
        if (!d.exists()) {
            d.mkdir();
        }
    }

    private String checkOrCreateRegionsDirectory(String user) {
        String regionsUserFolderPath = ut.GMQLHOME + File.separator + "data" + File.separator + user + File.separator + "regions";
        File d = new File(regionsUserFolderPath);
        if (!d.exists()) {
            d.mkdir();
        }
        return regionsUserFolderPath;
    }
}
