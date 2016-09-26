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

import orchestrator.repository.GMQLFileTypes;
import orchestrator.repository.GMQLRepository;
import orchestrator.entities.GMQLFile;
import java.io.FileNotFoundException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import orchestrator.entities.GMQLFile;

/**
 * This class contains the resources required to browse the GQL repository. It
 * offers resources to explore the repository, to add and remove experiments,
 * metadata and schemas.
 *
 * @author Massimo Quadrana <massimo.quadrana at polimi.it>
 */
@Path("/repo/browse/")
public class RepositoryBrowser {

    /**
     * Returns the tree of metadata files that are accessible to a given user.
     *
     * @param userId Current userId
     * @return A {@code Response} containing a {@code GQLFile} instance
     * representing the structure of the repository
     * @throws FileNotFoundException
     */
    @GET
    @Path("/meta/{username}")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response browseRepositoryMetadata(@PathParam("username") String sc) throws FileNotFoundException {
        String user = sc;
        GMQLRepository repository = GMQLRepository.getInstance();
        GMQLFile tree = repository.getRepositoryTree(user, GMQLFileTypes.METADATA);

        return Response.ok(tree).build();
    }

    /**
     * Returns the tree of schema files that are accessible to a given user.
     *
     * @param userId Current userId
     * @return A {@code Response} containing a {@code GQLFile} instance
     * representing the structure of the repository
     * @throws FileNotFoundException
     */
    @GET
    @Path("/schema/{username}")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response browseRepositorySchemas(@PathParam("username") String sc) throws FileNotFoundException {
        String user = sc;
        GMQLRepository repository = GMQLRepository.getInstance();
        GMQLFile tree = repository.getRepositoryTree(user, GMQLFileTypes.SCHEMA, true);

        return Response.ok(tree).build();

    }

    /**
     * Returns the tree of query files that are accessible to a given user.
     *
     * @param userId Current userId
     * @return A {@code Response} containing a {@code GQLFile} instance
     * representing the structure of the repository
     * @throws FileNotFoundException
     */
    @GET
    @Path("/query/{username}")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response browseRepositoryQueries(@PathParam("username") String sc) throws FileNotFoundException {
        String user = sc;
        GMQLRepository repository = GMQLRepository.getInstance();
        GMQLFile tree = repository.getRepositoryTree(user, GMQLFileTypes.QUERY);

        return Response.ok(tree).build();
    }

    /**
     * Returns the tree of all the files that are accessible to a given user.
     *
     * @param userId Current userId
     * @return A {@code Response} containing a {@code GQLFile} instance
     * representing the structure of the repository
     * @throws FileNotFoundException
     */
    @GET
    @Path("/all/{username}")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response browseRepositoryAll(@PathParam("username") String sc) throws FileNotFoundException {
        String user = sc;
        GMQLRepository repository = GMQLRepository.getInstance();
        GMQLFile tree = repository.getRepositoryTree(user, GMQLFileTypes.ANY);

        return Response.ok(tree).build();
    }
}
