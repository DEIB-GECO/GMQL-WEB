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

import gql.services.rest.Orchestrator.*;

import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * This class implements the basic version of the resources to browse metadata
 * files. Each file is parsed every time a feature is requested, to keep the
 * lowest data in memory at the cost of response speed.
 *
 * @author Massimo Quadrana <massimo.quadrana at polimi.it>
 */
@Path("/browse/meta")
@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
public class MetadataBrowserBasic {

    /**
     * *
     * Browses a metadata file to get the set of unique attributes it contains.
     *
     * @param filekey Key of the experiment/annotation file to be explored
     * @return A [@code Response} containing a sorted list of unique attributes
     * @throws GMQLServiceException
     * @throws InvalidKeyException
     */
    @GET
    @Path("/{filekey}")
    public Response browseResourceFile(@PathParam("filekey") String filekey) throws GMQLServiceException, InvalidKeyException {

        java.nio.file.Path filePath = GMQLFileUtils.getPathFromFileKey(filekey);

        Map<String, Set<String>> attr_to_value_map = GMQLFileUtils.buildAttributeToValueMap(filePath.toString());

        List<Attribute> attributeList = new ArrayList<>();
        for (String attr : attr_to_value_map.keySet()) {
            attributeList.add(new Attribute(attr));
        }

        AttributeList attributes = new AttributeList(attributeList);

        return Response.ok(attributes).build();
    }

    /**
     * *
     * Browses a metadata file to get the set of values for an attribute that it
     * contains.
     *
     * @param filekey Key of the experiment/annotation file to be explored
     * @param attribute Name of the attribute to be explored
     * @return A {@code Response} containing a sorted list of unique values
     * @throws GMQLServiceException
     * @throws InvalidKeyException
     */
    @GET
    @Path("/{filekey}/{attribute}")
    public Response browseAttribute(@PathParam("filekey") String filekey,
            @PathParam("attribute") String attribute) throws GMQLServiceException, InvalidKeyException {

        java.nio.file.Path filePath = GMQLFileUtils.getPathFromFileKey(filekey);

        Map<String, Set<String>> attr_to_value_map = GMQLFileUtils.buildAttributeToValueMap(filePath.toString());
        Set<String> valueSet = attr_to_value_map.get(attribute);

        if (valueSet != null) {
            List<Value> valueList = new ArrayList<>(valueSet.size());
            for (String val : valueSet) {
                valueList.add(new Value(val));
            }
            ValueList values = new ValueList(valueList);
            return Response.ok(values).build();
        } else {
            throw new GMQLServiceException("Attribute " + attribute + " not found.");
        }
    }

    /**
     * *
     * Browses a metadata file to get the set of unique identifiers of
     * experiments having a given metadata. Metadata is identified by the pair
     * (attributeName, valueName)
     *
     * @param filekey Key of the experiment/annotation file to be explored
     * @param attribute Name of the attribute to be explored
     * @param value Name of the value to be explored
     * @return A {@code Response} containing a sorted list of unique ids
     * @throws GMQLServiceException
     * @throws InvalidKeyException
     */
    @GET
    @Path("/{filekey}/{attribute}/{value}")
    public Response browseAttributeValue(@PathParam("filekey") String filekey,
            @PathParam("attribute") String attribute,
            @PathParam("value") String value) throws GMQLServiceException, InvalidKeyException {

        java.nio.file.Path filePath = GMQLFileUtils.getPathFromFileKey(filekey);

        Map<Metadata, Set<String>> metadata_to_id_map = GMQLFileUtils.buildMetadataToIdMap(filePath.toString());

        Metadata meta = new Metadata(attribute, value);
        Set<String> idSet = metadata_to_id_map.get(meta);

        if (idSet != null) {
            ExperimentIdList ids = new ExperimentIdList(new ArrayList<>(idSet));
            return Response.ok(ids).build();
        } else {
            throw new GMQLServiceException("Metadata " + meta.toString() + " not found.");
        }
    }

    /**
     * *
     * Browses a metadata file to get the set of metadata of a given experiment
     * id.
     *
     * @param filekey Key of the experiment/annotation file to be explored
     * @param id Experiment id to be browsed
     * @return A {@code Response} containing the set of metadata
     * @throws GMQLServiceException
     * @throws InvalidKeyException
     */
    @GET
    @Path("/id/{filekey}/{id}")
    public Response browseId(@PathParam("filekey") String filekey,
            @PathParam("id") String id) throws GMQLServiceException, InvalidKeyException {

        java.nio.file.Path filePath = GMQLFileUtils.getPathFromFileKey(filekey);

        Map<String, Set<Metadata>> id_to_metadata_map = GMQLFileUtils.buildIdToMetadataMap(filePath.toString());

        Set<Metadata> metadataSet = id_to_metadata_map.get(id);

        if (metadataSet != null) {
            Experiment metadata_wrapped = new Experiment(new ArrayList<>(metadataSet));
            return Response.ok(metadata_wrapped).build();
        } else {
            throw new GMQLServiceException("Experiment id " + id + " not found.");
        }

    }

    /**
     * Returns all the experiments having a given metadata (attribute, value)
     *
     * @param filekey
     * @param attribute
     * @param value
     * @return
     * @throws InvalidKeyException
     * @throws GMQLServiceException
     */
    @GET
    @Path("/all/{filekey}/{attribute}/{value}")
    public Response getAllExperiments(@PathParam("filekey") String filekey,
            @PathParam("attribute") String attribute,
            @PathParam("value") String value) throws InvalidKeyException, GMQLServiceException {

        java.nio.file.Path filePath = GMQLFileUtils.getPathFromFileKey(filekey);

        //TODO: Unefficient: the file is parsed twice
        Map<Metadata, Set<String>> metadata_to_id_map = GMQLFileUtils.buildMetadataToIdMap(filePath.toString());
        Map<String, Set<Metadata>> id_to_metadata_map = GMQLFileUtils.buildIdToMetadataMap(filePath.toString());

        List<Experiment> experiments = new ArrayList<>();
        Metadata meta = new Metadata(attribute, value);
        //retrieve experiment ids having such metadata
        Set<String> experimentIds = metadata_to_id_map.get(meta);
        if (experimentIds == null) {
            throw new GMQLServiceException("Metadata not found: " + meta.toString());
        }
        //retrieve metadata for each experiment
        for (String id : experimentIds) {
            experiments.add(new Experiment(new ArrayList<>(id_to_metadata_map.get(id))));
        }

        return Response.ok(new ExperimentList(experiments)).build();
    }
    
    @GET
    @Path("/filter/{filekey}/{attribute}")
    public Response filterExperiments(@PathParam("filekey") String filekey,
            @PathParam("attribute") String attribute,
            @QueryParam("value_list") final List<String> values){
        
        
        return Response.ok().build();
    }
}
