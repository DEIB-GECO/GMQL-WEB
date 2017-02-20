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

import com.google.common.collect.Sets;
import gmql.services.cache.ExperimentCache;
import gql.services.rest.Orchestrator.*;

import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * This class implements the cached version of the resources to browse metadata
 * files. Files are parsed and then temporarely stored in a cache to reduce the
 * response time. The behavior of the cache can be observed by looking at
 * {@code ExperimentCache} class.
 *
 * @author Massimo Quadrana <massimo.quadrana at polimi.it>
 */
@Path("/browse-c/meta")
@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
public class MetadataBrowserCached {

    /**
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

        ExperimentCache cache = ExperimentCache.getInstance();
        List<Attribute> attributesList = new ArrayList<>();
        for (String attr : cache.getAttributes(filePath)) {
            attributesList.add(new Attribute(attr));
        }
        AttributeList attributes = new AttributeList(attributesList);

        return Response.ok(attributes).build();
    }

    /**
     * Browses a metadata file to get the set of values for an attribute that it
     * contains.
     *
     * @param filekey Key of the experiment/annotation file to be explored
     * @param attributeName Name of the attribute to be explored
     * @return A {@code Response} containing a sorted list of unique values
     * @throws GMQLServiceException
     * @throws InvalidKeyException
     */
    @GET
    @Path("/{filekey}/{attributeName}")
    public Response browseAttribute(@PathParam("filekey") String filekey,
            @PathParam("attributeName") String attributeName) throws GMQLServiceException, InvalidKeyException {

        java.nio.file.Path filePath = GMQLFileUtils.getPathFromFileKey(filekey);

        ExperimentCache cache = ExperimentCache.getInstance();
        Set<String> valueSet = cache.getValuesFromAttribute(filePath, attributeName);

        if (valueSet != null) {
            List<Value> valueList = new ArrayList<>();
            for (String val : valueSet) {
                valueList.add(new Value(val));
            }
            ValueList values = new ValueList(valueList);
            return Response.ok(values).build();
        } else {
            throw new GMQLServiceException("Attribute " + attributeName + " not found.");
        }
    }

    /**
     * Browses a metadata file to get the set of unique identifiers of
     * experiments having a given metadata. Metadata is identified by the pair
     * (attributeName, valueName)
     *
     * @param filekey Key of the experiment/annotation file to be explored
     * @param attributeName Name of the attribute to be explored
     * @param valueName Name of the value to be explored
     * @return A {@code Response} containing a sorted list of unique ids
     * @throws GMQLServiceException
     * @throws InvalidKeyException
     */
    @GET
    @Path("/{filekey}/{attribute}/{value}")
    public Response browseAttributeValue(@PathParam("filekey") String filekey,
            @PathParam("attribute") String attributeName,
            @PathParam("value") String valueName) throws GMQLServiceException, InvalidKeyException {

        java.nio.file.Path filePath = GMQLFileUtils.getPathFromFileKey(filekey);

        ExperimentCache cache = ExperimentCache.getInstance();

        Metadata meta = new Metadata(attributeName, valueName);
        Set<String> idSet = cache.getIdsFromMetadata(filePath, meta);

        if (idSet != null) {
            ExperimentIdList ids = new ExperimentIdList(new ArrayList<>(idSet));
            return Response.ok(ids).build();
        } else {
            throw new GMQLServiceException("Metadata " + meta.toString() + " not found.");
        }
    }

    /**
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

        ExperimentCache cache = ExperimentCache.getInstance();

        Set<Metadata> metadataSet = cache.getMetadataFromId(filePath, id);

        if (metadataSet != null) {
            Experiment metadata_wrapped = new Experiment(new ArrayList<>(metadataSet));
            return Response.ok(metadata_wrapped).build();
        } else {
            throw new GMQLServiceException("Experiment id " + id + " not found.");
        }

    }
    
    
    @GET
    @Path("/id/orderattributes/{filekey}/{id}/{attributes}")
    public Response browseIdAndOrder(@PathParam("filekey") String filekey,
            @PathParam("id") String id,
            @PathParam("attributes") String attributes) throws GMQLServiceException, InvalidKeyException {

        java.nio.file.Path filePath = GMQLFileUtils.getPathFromFileKey(filekey);

        ExperimentCache cache = ExperimentCache.getInstance();

        Set<Metadata> metadataSet = cache.getMetadataFromId(filePath, id);

        if (metadataSet != null) {
            //System.out.println("TENETATIVO IN CORSO - attributi top sono: ");
            
            ArrayList<String> attributesList = new ArrayList<>();
            //System.out.println(attributes);
            Scanner s = new Scanner(attributes);
            s.useDelimiter("___");
            while(s.hasNext()){
                attributesList.add(s.next());
            }            
            for(String st : attributesList){
                System.out.println(st);
            }            
            Experiment metadata_wrapped = new Experiment(new ArrayList<>(metadataSet),attributesList);            
            return Response.ok(metadata_wrapped).build();
        } else {
            throw new GMQLServiceException("Experiment id " + id + " not found.");
        }

    }
    

    @GET
    @Path("/filter")
    public Response filterExperiments(@QueryParam("filekey") String filekey,
            @QueryParam("attribute") String attribute,
            @QueryParam("values") final List<String> values) throws InvalidKeyException, GMQLServiceException {

        java.nio.file.Path filePath = GMQLFileUtils.getPathFromFileKey(filekey);

        ExperimentCache cache = ExperimentCache.getInstance();

        Set<String> idSet = new HashSet<>();

        for (String value : values) {
            //possible null pointer exception!
            try {
                idSet.addAll(cache.getIdsFromMetadata(filePath, new Metadata(attribute, value)));
            } catch (Exception e) {              
            }
        }

        ExperimentIdList idList = new ExperimentIdList(new ArrayList<>(idSet));        
        if(idList.getCount() == 0){
            return Response.noContent().build();
        }
        
        
        return Response.ok(idList).build();
    }

    /**
     *
     * @param filekey
     * @param attributes A list of attributes.
     * @param values A list of values. Each position in the list corresponds to
     * an attribute in the attributes list.
     * @return
     * @throws InvalidKeyException
     * @throws GMQLServiceException
     */
    @GET
    @Path("/filtermany")
    public Response filtermanyExperiments(@QueryParam("filekey") String filekey,
            @QueryParam("attributes") final List<String> attributes,
            @QueryParam("values") final List<String> values,
            @QueryParam("valuesEachAttr") final List<Integer> numbers) throws InvalidKeyException, GMQLServiceException {

        java.nio.file.Path filePath = GMQLFileUtils.getPathFromFileKey(filekey);

        ExperimentCache cache = ExperimentCache.getInstance();

        //all IDs
        Set<String> idSet = cache.getIdsFromMetadata(filePath, null);
        Set<String> tempIdSet = new HashSet<>();
        int valueIndex = 0;

        for (int i = 0; i < attributes.size(); i++) {
            String attribute = attributes.get(i);
            tempIdSet = new HashSet<>();
            for (int j = 0; j < numbers.get(i); j++) {
                String value = values.get(j + valueIndex);
                try{
                tempIdSet.addAll(cache.getIdsFromMetadata(filePath, new Metadata(attribute, value)));
                }catch(Exception e) {}
            }
            valueIndex += numbers.get(i);
//            if (i == 0) {
//                idSet.addAll(tempIdSet);
//            } else {
                idSet = Sets.intersection(idSet, tempIdSet);
//            }
            if (idSet.isEmpty()) {
                break;
            }
        }


        ExperimentIdList idList = new ExperimentIdList(new ArrayList<>(idSet));
//        if(idList.getCount() == 0){
//            return Response.noContent().build();
//        }

        return Response.ok(idList).build();
    }    
}
