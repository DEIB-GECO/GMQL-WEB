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

import orchestrator.entities.GMQLSchemaCollection;
import orchestrator.util.GQLFileUtils;
import play.api.Logger;

import java.io.FileNotFoundException;
import java.security.InvalidKeyException;
import java.util.logging.Level;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;

/**
 * Resources to browse schemas
 *
 * @author Massimo Quadrana <massimo.quadrana at polimi.it>
 */
@Path("/browse/schema")
@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
public class SchemaBrowser {

    /**
     * Returns the list of schemas stored into a schema file
     *
     * @param filekey Key to the schema file
     * @return The list of schema names available
     * @throws java.security.InvalidKeyException if the filekey is not valid
     * @throws java.io.FileNotFoundException if the requested file is not found in the repository
     * @throws javax.xml.bind.JAXBException if the schema xml file cannot be parsed correctly
     */
    @GET
    @Path("/{filekey}")
    public Response getAvailableSchemas(@PathParam("filekey") String filekey) throws InvalidKeyException, FileNotFoundException, JAXBException {

        java.nio.file.Path schemaFilePath = GQLFileUtils.getPathFromFileKey(filekey);
        java.util.logging.Logger.getLogger(DataSetsManager.class.getName()).log(Level.INFO, "schemaFilePath " + schemaFilePath);
        GMQLSchemaCollection schemaList = GMQLSchemaCollection.parseGQLSchemaCollection(schemaFilePath);
        return Response.ok(schemaList).build();
    }
}
