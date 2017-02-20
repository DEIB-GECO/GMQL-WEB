/* 
 * Copyright (C) 2014 Massimo Quadrana <massimo.quadrana at polimi.it>
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

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.sun.jersey.multipart.FormDataParam;
import gql.services.rest.Orchestrator.*;
import it.polimi.genomics.manager.Exceptions.NoJobsFoundException;
import it.polimi.genomics.manager.GMQLContext;
import it.polimi.genomics.manager.GMQLExecute;
import it.polimi.genomics.manager.Launchers.GMQLSparkLauncher;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.security.InvalidKeyException;
import java.util.*;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.spark.SparkContext;
import it.polimi.genomics.core.*;
import utils.GMQL_Globals$;

/**
 * This class contains the set of REST resources to read, save, edit and run a
 * GQL query
 *
 * @author Massimo Quadrana <massimo.quadrana at polimi.it>
 */
@Path("/query")
public class QueryManager {

    private static SparkContext sparkContext;
    /**
     * Reads the content of query file and returns it as response to the
     * requester
     *
     * @param filekey Key of the query file
     * @return The text of the query
     * @throws java.security.InvalidKeyException
     * @throws java.io.IOException
     */
    @GET
    @Path("/read/{filekey}")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response readQuery(String user,//@Context SecurityContext sc,
                              @PathParam("filekey") String filekey) throws InvalidKeyException, IOException {
        //TODO: Check if the user has access to the requested file
        //TODO: Check if filekey is actually a query file
        //TODO: Return XML-JSON data
//        String user = sc.getUserPrincipal().getName();
        java.nio.file.Path queryPath = GMQLRepositoryV0.getInstance().getFilePathFromKey(filekey);
        String fileContent = Files.toString(queryPath.toFile(), Charsets.UTF_8);

        return Response.ok(fileContent).build();
    }

    /**
     * Deletes a query file from the remote repository
     *
     * @param filekey Key of the query file
     * @return
     * @throws InvalidKeyException
     */
    @GET
    @Path("/delete/{filekey}")
    public Response deleteQuery(String user,//@Context SecurityContext sc,
                                @PathParam("filekey") String filekey) throws InvalidKeyException {
        //TODO: Check if the user has access to the requested file
        //TODO: Check if filekey is actually a query file
        java.nio.file.Path removedFile = GMQLRepositoryV0.getInstance().removeEntry(filekey);
        //delete the file from the repository
        removedFile.toFile().delete();
        return Response.ok().build();
    }

    /**
     * Saves the text of the query into a query file
     *
     * @param queryText the text of the query
     * @param filename
     * @param filekey
     * @return The GQLFile where the query has been stored
     * @throws InvalidKeyException
     * @throws IOException
     */
    @POST
    @Path("/save")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response saveQueryAs(@FormDataParam("query") String queryText,
                                String user,//@Context SecurityContext sc,
                                @FormDataParam("filename") String filename,
                                @FormDataParam("filekey") String filekey) throws IOException, InvalidKeyException, GMQLServiceException {
        GMQLRepositoryV0 repository = GMQLRepositoryV0.getInstance();
//        String user = sc.getUserPrincipal().getName();
        if (!filename.toLowerCase().endsWith("." + GMQLFileTypes.QUERY.getExtension())) {
            filename = filename.concat("." + GMQLFileTypes.QUERY.getExtension());
        }
        if (!repository.isValidKey(filekey)) {
            //the file is not stored into the repository yet
            filekey = repository.addFileToRepository(filename, user, "queries");
        }
        saveQueryToFile(queryText, filekey, filename);

        return Response.ok(filekey).build();
    }

    /**
     *
     * @param queryText
     * @param filekey
     * @throws InvalidKeyException
     * @throws IOException
     */
    private java.nio.file.Path saveQueryToFile(String queryText, String filekey, String filename) throws InvalidKeyException, IOException, GMQLServiceException {
        GMQLRepositoryV0 repository = GMQLRepositoryV0.getInstance();
        //create a StringReader to read the query text
        StringReader sr = new StringReader(queryText);
        //get the file to be overwritten
        java.nio.file.Path filePath = repository.getFilePathFromKey(filekey);
        //rename the file if necessary
        File queryFile = filePath.toFile();
        if (!queryFile.getName().equals(filename)) {

            java.nio.file.Path renamedFilePath = filePath.getParent().resolve(filename);
            File renamedFile = renamedFilePath.toFile();
            if (renamedFile.exists()) {
                throw new GMQLServiceException("File " + filename + " already exists! Please use another name.");
            }
            queryFile.renameTo(renamedFile);
            repository.updateEntry(filekey, renamedFilePath);
            //write the query text into the file
            GMQLFileUtils.writeToFile(sr, renamedFilePath);
            return renamedFilePath;
        } else {
            //write the query text into the file
            GMQLFileUtils.writeToFile(sr, filePath);
            return filePath;
        }
    }

    @GET
    @Path("/runv2/{filekey}/{gtfoutput}/{exectype}")
    @Produces({MediaType.TEXT_PLAIN})
    public Response runQueryV2File(String user,//@Context SecurityContext sc,
                                   @PathParam("filekey") String filekey,@PathParam("gtfoutput") String GTFoutput,
                                   @PathParam("exectype") String execType) throws InvalidKeyException, InvalidGMQLJobException {


//        String user = sc.getUserPrincipal().getName();
        java.nio.file.Path queryFilePath = GMQLRepositoryV0.getInstance().getFilePathFromKey(filekey);


        Logger.getLogger(this.getClass().getName()).log(Level.INFO,
                "\n" + user + " : " + queryFilePath.toString() + "\n " + execType + "\n\n\n\n");
        try {
            StringBuilder st = new StringBuilder();
            for(Iterator<String> i =  Files.readLines(new File(queryFilePath.toString()),Charsets.UTF_8).iterator(); i.hasNext();){
                st.append(i.next());
            }
            Logger.getLogger(this.getClass().getName()).log(Level.INFO,"\n" + user + " hhee: " +st.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

        it.polimi.genomics.manager.GMQLExecute server = it.polimi.genomics.manager.GMQLExecute.apply();

        boolean gtf = false;
        if(GTFoutput.trim().equals("true")) gtf = true;

        String script = "";
        try {
            script = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(queryFilePath.toString())));
        }catch(IOException ioe){
            ioe.printStackTrace();
        }
        int outputFormat ;

        if(gtf)
            outputFormat = GMQLOutputFormat.GTF().id();
        else
            outputFormat = GMQLOutputFormat.TAB().id();

        GMQLScript gmqlScript = new GMQLScript(script, queryFilePath.toString());
        BinSize binSize = new BinSize(5000,5000,1000);
        GMQLContext gmqlContext = new GMQLContext (ImplementationPlatform.SPARK(), GMQL_Globals$.MODULE$.apply().repository(),GMQLOutputFormat.apply(outputFormat),binSize,user,sparkContext);

        it.polimi.genomics.manager.GMQLJob job = server.registerJob(gmqlScript,gmqlContext,"");
        server.execute(job.jobId(),new GMQLSparkLauncher(job));


        return Response.ok(job.jobId()).build();
    }

    @GET
    @Path("/compilev2/{filekey}/{exectype}")
    @Produces({MediaType.TEXT_PLAIN})
    public Response runCompileV2(String user,//@Context SecurityContext sc,
                                 @PathParam("filekey") String filekey,
                                 @PathParam("exectype") String execType) throws InvalidKeyException, InvalidGMQLJobException {

//        String user = sc.getUserPrincipal().getName();

        java.nio.file.Path queryFilePath = GMQLRepositoryV0.getInstance().getFilePathFromKey(filekey);
        it.polimi.genomics.manager.GMQLExecute server = it.polimi.genomics.manager.GMQLExecute.apply();
        String script = "";
        try {
            script = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(queryFilePath.toString())));
        }catch(IOException ioe){
            ioe.printStackTrace();
        }


        GMQLScript gmqlScript = new GMQLScript(queryFilePath.toString(),script);
        BinSize binSize = new BinSize(5000,5000,1000);
        GMQLContext gmqlContext = new GMQLContext (ImplementationPlatform.SPARK(), GMQL_Globals$.MODULE$.apply().repository(),GMQLOutputFormat.TAB(),binSize,user,sparkContext);

        it.polimi.genomics.manager.GMQLJob job = server.registerJob(gmqlScript,gmqlContext,"");

        return Response.ok(job.jobId()).build();
    }

    @GET
    @Path("/jobsV2")
    public Response getJobsv2(String user//@Context SecurityContext sc
    ) throws NoJobsFoundException {
        GMQLExecute server = GMQLExecute.apply();
//        String user = sc.getUserPrincipal().getName();
        List<String> jobs = server.getUserJobs(user);
        JobList jobList = new JobList(jobs);
        return Response.ok(jobList).build();
    }
    @GET
    @Path("/logPath/{jobid}")
    public Response getLogPath(String user,//@Context SecurityContext sc,
                               @PathParam("jobid") String jobId) throws NoJobsFoundException {
        GMQLExecute server = GMQLExecute.apply();
        String jobLogPath = server.getJobLogPath(user,jobId);
        return Response.ok(jobLogPath).build();
    }

    @GET
    @Path("/log/{jobid}")
    public Response getLog(String user,//@Context SecurityContext sc,
                           @PathParam("jobid") String jobId) throws NoJobsFoundException {
        GMQLExecute server = GMQLExecute.apply();

        List<String>  jobLog = server.getJobLog(user,jobId);
        JobList jobLogList = new JobList(jobLog);
        return Response.ok(jobLogList).build();
    }

/*  MOVED TO OTHER CLASS
    @GET
    @Path("/kill/{jobid}")
    public Response killJob(String user,
                           @PathParam("jobid") String jobId) throws NoJobsFoundException {
        GMQLExecute server = GMQLExecute.apply();
        it.polimi.genomics.manager.GMQLJob job = server.getGMQLJob(user, jobId);
        job.submitHandle().killJob();

        return Response.ok().build();
    }*/

    @GET
    @Path("/tracev2/{jobid}")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response traceJobv2(String user,//@Context SecurityContext sc,
                               @PathParam("jobid") String jobId) throws InvalidGMQLJobException {

        GMQLExecute server = GMQLExecute.apply();
//        String user = sc.getUserPrincipal().getName();
        it.polimi.genomics.manager.GMQLJob job = server.getGMQLJob(user, jobId);
        String elapsed =/* "Compilation Time: "+job.getCompileTime()+"\n"+*/"Execution Time: "+job.getExecutionTime()/*+"\nCreate Result DataSet Time: "+job.getDSCreationTime()*/;

        if(elapsed ==null)
        {
            System.out.println("\n\nNo exec Time preduced yet\n\n");
            elapsed="";
        }

        List<String> datasets;String DSnames="";
        StringBuilder Datasetsnames = new StringBuilder();
        List<String> DSs = new LinkedList<String>();
        try {
            DSs = server.getJobDatasets(jobId);
            if (!(datasets = server.getJobDatasets(jobId)).isEmpty()) {
                for (String ds : datasets) {
                    Datasetsnames.append("," + ds);
                }
                try {
                    DSnames = Datasetsnames.toString().substring(1, Datasetsnames.toString().length());
                } catch (Exception ex) {
                    System.out.println("There is no result to show " + ex.getMessage());
                }
            }
        }catch (Exception ex){
            System.out.println (ex.getMessage());
        }

        System.out.println("Datasets: "+DSnames);
        GMQLJobStatusXML jobStateXml = new GMQLJobStatusXML(
                new Date(),
                job.getJobStatus().toString(),
                job.getMessage(),
                DSs,
                DSnames,
                elapsed
        );

        return Response.ok(jobStateXml).build();
    }
}
