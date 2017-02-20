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

import com.google.common.io.Files;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;
import gql.services.rest.Orchestrator.GMQLFileTypes;
import gql.services.rest.Orchestrator.GMQLFileUtils;
import gql.services.rest.Orchestrator.GMQLRepositoryV0;
import gql.services.rest.Orchestrator.GMQLServiceException;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

/**
 * This class contains all the resources to upload files on the repository.
 *
 * @author Massimo Quadrana <massimo.quadrana at polimi.it>
 */
@Path("/repo/upload")
public class RepositoryUploader {

    /**
     * Uploads a query file to the repository.
     *
     * @param uploadedInputStream
     * @param fileDetails
     * @return
     * @throws GMQLServiceException
     */
    @POST
    @Path("/query/{username}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadQueryFile(@PathParam("username") String sc,
            @FormDataParam("file") InputStream uploadedInputStream,
            @FormDataParam("file") FormDataContentDisposition fileDetails) throws GMQLServiceException {
        
        String user = sc;

        if (checkFile(uploadedInputStream, fileDetails)) {
            GMQLRepositoryV0 repository = GMQLRepositoryV0.getInstance();
            String filename = fileDetails.getFileName();
            
            if(!GMQLFileTypes.QUERY.getExtension().equals(Files.getFileExtension(filename))&&Files.getFileExtension(filename)!=""){
                filename = new java.io.File(filename).getName().substring(0,new java.io.File(filename).getName().lastIndexOf(".")).concat("."+GMQLFileTypes.QUERY.getExtension());
            }                            
            //get the key for the new file
            String file_key = repository.addFileToRepository(filename, user, "queries");

            try {
                //write the file to disk
                java.nio.file.Path uploadedFileLocation = repository.getFilePathFromKey(file_key);
                GMQLFileUtils.writeToFile(uploadedInputStream, uploadedFileLocation);
            } catch (InvalidKeyException | IOException ex) {
                throw new GMQLServiceException(ex.getMessage());
            }
            return Response.ok().build();
        } else {
            throw new GMQLServiceException("Uploaded file is not valid!");
        }
    }

    /**
     * Checks the correctness of the uploaded file.
     *
     * @param uploadedInputStream
     * @param fileDetails
     * @return
     */
    private boolean checkFile(InputStream uploadedInputStream,
            FormDataContentDisposition fileDetails) {
        //TODO: check md5 sum of the uploaded file
        return true;
    }
}
