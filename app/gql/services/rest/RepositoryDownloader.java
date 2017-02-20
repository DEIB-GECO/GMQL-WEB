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

import gql.services.rest.Orchestrator.GMQLFileUtils;
import gql.services.rest.Orchestrator.GMQLServiceException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

/**
 * This class contains all the resources to download files from the repository.
 *
 * @author Massimo Quadrana <massimo.quadrana at polimi.it>
 */
@Path("/repo/download")
public class RepositoryDownloader {

    /**
     * Resource to download a file from the repository. The file is requested by
     * an user identified by its own {@code user_id}. If present in the
     * repository, the file with key {@code file_key} is attached to the
     * {@code Response}. A {@code GMQLServiceException} is thrown in case of
     * missing file or unauthorized access by the user.
     *
     * @param userId
     * @param filekey
     * @return A {@code Response} with the requested file attached
     * @throws GMQLServiceException if the file cannot be found or the user has
     * no grants to access to it
     */
    @GET
    @Path("/{user}/{filekey}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response downloadFile(@PathParam("user") String userId,
            @PathParam("filekey") String filekey) throws GMQLServiceException {
        try {
            //TODO Check if the user can accces to the requested file
            File requestedFile = GMQLFileUtils.getPathFromFileKey(filekey).toFile();

            //Set the header and send the response
            return Response.ok((Object) requestedFile)
                    .header("Content-Disposition", "attachment; filename=" + requestedFile.getName())
                    .build();
        } catch (InvalidKeyException ex) {
            throw new GMQLServiceException(ex.getMessage());
        }
    }

    /**
     * Creates and sends a zip file with the requested files
     *
     * @param userId
     * @param filename
     * @param filekeys
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     * @throws InvalidKeyException
     */
    @GET
    @Path("/zip/{username}")
    public Response downloadFileZip(@PathParam("username") String sc,
            @QueryParam("filename") String filename,
            @QueryParam("files") List<String> filekeys) throws FileNotFoundException, IOException, InvalidKeyException {
        
        File zipFile = File.createTempFile(filename, ".zip");
        zipFile.createNewFile();
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            int read;
            byte[] cb = new byte[4096];

            for (String filekey : filekeys) {
                //TODO Check if the user can accces to the requested file
                File requestedFile = GMQLFileUtils.getPathFromFileKey(filekey).toFile();
                FileInputStream fis = new FileInputStream(requestedFile);
                ZipEntry zipEntry = new ZipEntry(requestedFile.getName());

                zos.putNextEntry(zipEntry);
                while ((read = fis.read(cb)) != -1) {
                    zos.write(cb, 0, read);
                }
                zos.closeEntry();
            }
        }

        //Set the header and send the response
        return Response.ok((Object) zipFile)
                .header("Content-Disposition", "attachment; filename=" + zipFile.getName())
                .build();
    }

}
