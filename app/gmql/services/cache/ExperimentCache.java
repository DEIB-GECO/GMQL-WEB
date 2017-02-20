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
package gmql.services.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import gql.services.rest.Orchestrator.GMQLServiceException;
import gql.services.rest.Orchestrator.Metadata;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Provides a way to cache metadata that is extracted from an experiment or 
 * annotation file to speed up the browsing process.
 * @author Massimo Quadrana
 *
 */
public class ExperimentCache {

    private static ExperimentCache instance;
    /**
     * Expire time in seconds
     */
    private final long EXPIRE_TIME = 300L;
    /**
     * Delay for cache clean up service in seconds
     */
    private final long CLEANUP_DELAY = 60L;
    /**
     * Maximum cache size in MB
     */
    private final int MAXIMUM_CACHE_SIZE = 500;
    //TODO:Optimize memory footprint of maps
    private LoadingCache<String, Map<String, Set<Metadata>>> id_to_metadata_cache;
    private LoadingCache<String, Map<Metadata, Set<String>>> metadata_to_id_cache;
    private LoadingCache<String, Map<String, Set<String>>> attr_to_value_cache;
    private final ScheduledExecutorService cleanUpService = Executors.newScheduledThreadPool(1);
    private List<Cache<?, ?>> cacheRegister = null;

    private ExperimentCache() {

        cacheRegister = new ArrayList<>();

        id_to_metadata_cache = CacheBuilder.newBuilder().maximumSize(MAXIMUM_CACHE_SIZE).expireAfterAccess(EXPIRE_TIME, TimeUnit.SECONDS).build(new CacheLoader<String, Map<String, Set<Metadata>>>() {

            @Override
            public Map<String, Set<Metadata>> load(String key)
                    throws Exception {
                return buildIdToMetadataMap(key);
            }
        });

        cacheRegister.add(id_to_metadata_cache);

        metadata_to_id_cache = CacheBuilder.newBuilder().maximumSize(MAXIMUM_CACHE_SIZE).expireAfterAccess(EXPIRE_TIME, TimeUnit.SECONDS).build(new CacheLoader<String, Map<Metadata, Set<String>>>() {

            @Override
            public Map<Metadata, Set<String>> load(String key)
                    throws Exception {
                return buildMetadataToIdMap(key);
            }
        });

        cacheRegister.add(metadata_to_id_cache);

        attr_to_value_cache = CacheBuilder.newBuilder().maximumSize(MAXIMUM_CACHE_SIZE).expireAfterAccess(EXPIRE_TIME, TimeUnit.SECONDS).build(new CacheLoader<String, Map<String, Set<String>>>() {

            @Override
            public Map<String, Set<String>> load(String key)
                    throws Exception {
                return buildAttributeToValueMap(key);
            }
        });

        cacheRegister.add(attr_to_value_cache);
        //set up the scheduled clean-up service
        cleanUpService.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                for (Cache<?, ?> c : cacheRegister) {
                    c.cleanUp();
                }
            }
        }, CLEANUP_DELAY, CLEANUP_DELAY, TimeUnit.SECONDS);

    }

    /**
     * *
     * Returns the current instance of the class
     *
     * @return An instance of {@code ExperimentCache}
     */
    public static ExperimentCache getInstance() {
        if (instance == null) {
            instance = new ExperimentCache();
        }
        return instance;
    }

    /**
     * *
     * Returns the set of {@code Metadata} associated with an experimentId into
     * an experiment file. If the file was not present into the cache when the
     * function is called, then the experiment file is parsed and loaded into
     * cache before returning the result set.
     *
     * @param experimentFilePath {@code Path} instance of the experiment file to
     * be used
     * @param experimentId Id of the experiment to extracted
     * @return A set of {@code Metadata} instances related with the experiment
     * @throws GMQLServiceException if the experimentFile or the experimentId
     * cannot be found
     */
    public Set<Metadata> getMetadataFromId(Path experimentFilePath, String experimentId) throws GMQLServiceException {
        try {
            return id_to_metadata_cache.get(experimentFilePath.toString()).get(experimentId);
        } catch (ExecutionException e) {
            throw new GMQLServiceException(e.getMessage());
        }
    }

    /**
     * *
     * Returns the set of unique ids associated with a {@code Metadata} into an
     * experiment file. If the file was not present into the cache when the
     * function is called, then the experiment file is parsed and loaded into
     * cache before returning the result set.
     *
     * @param experimentFilePath {@code Path} instance of the experiment file to
     * be used
     * @param meta Instance of {@code Metadata} to be searched
     * @return Set of unique experiment ids associated with the {@code Metadata}
     * meta
     * @throws GMQLServiceException if the experimenFile or the {@code Metadata}
     * cannot be found
     */
    public Set<String> getIdsFromMetadata(Path experimentFilePath, Metadata meta) throws GMQLServiceException {
        try {
            if(meta == null)
                return id_to_metadata_cache.get(experimentFilePath.toString()).keySet();
            else
                return metadata_to_id_cache.get(experimentFilePath.toString()).get(meta);
        } catch (ExecutionException e) {
            throw new GMQLServiceException(e.getMessage());
        }
    }

    /**
     * *
     * Returns the set of unique attributes contained into an experiment file.
     * If the file was not present into the cache when the function is called,
     * then the experiment file is parsed and loaded into cache before returning
     * the result set.
     *
     * @param experimentFilePath {@code Path} instance of the experiment file to
     * be used
     * @return Set of unique attributes conteined into an experiment file
     * @throws GMQLServiceException if the experiementFile cannot be found
     */
    public Set<String> getAttributes(Path experimentFilePath) throws GMQLServiceException {
        try {
            return attr_to_value_cache.get(experimentFilePath.toString()).keySet();
        } catch (ExecutionException e) {
            throw new GMQLServiceException(e.getMessage());
        }
    }

    /**
     * *
     * Returns the set of unique values associated with an attribute If the file
     * was not present into the cache when the function is called, then the
     * experiment file is parsed and loaded into cache before returning the
     * result set.
     *
     * @param experimentFilePath {@code Path} instance of the experiment file to
     * be used
     * @param attributeName Name of the attribute to be extracted
     * @return Set of unique values associated with the passed attributeName
     * @throws GMQLServiceException if the experimenFile or the attribute cannot
     * be found
     */
    public Set<String> getValuesFromAttribute(Path experimentFilePath, String attributeName) throws GMQLServiceException {
        try {
            return attr_to_value_cache.get(experimentFilePath.toString()).get(attributeName);
        } catch (ExecutionException e) {
            throw new GMQLServiceException(e.getMessage());
        }
    }

    private Map<String, Set<Metadata>> buildIdToMetadataMap(String experimentFilePath) throws GMQLServiceException {

        Map<String, Set<Metadata>> id_to_metadata = new HashMap<>();
        String line = null;
        try {
            BufferedReader br = new BufferedReader(new FileReader(experimentFilePath));
            final Pattern pattern = Pattern.compile("\t");

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
            throw new GMQLServiceException("File " + Paths.get(experimentFilePath).getFileName() + " not found in the repository.");
        } catch (NumberFormatException | IOException e) {
            throw new GMQLServiceException("An error has occurred while parsing file " + Paths.get(experimentFilePath).getFileName() + ".");
        }
        return id_to_metadata;
    }

    private Map<Metadata, Set<String>> buildMetadataToIdMap(String experimentFilePath) throws GMQLServiceException {

        Map<Metadata, Set<String>> metadata_to_id = new HashMap<>();
        String line = null;
        try {
            BufferedReader br = new BufferedReader(new FileReader(experimentFilePath));
            final Pattern pattern = Pattern.compile("\t");

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
            throw new GMQLServiceException("File " + Paths.get(experimentFilePath).getFileName() + " not found in the repository.");
        } catch (NumberFormatException | IOException e) {
            throw new GMQLServiceException("An error has occurred while parsing file " + Paths.get(experimentFilePath).getFileName() + ".");
        }
        return metadata_to_id;
    }

    private Map<String, Set<String>> buildAttributeToValueMap(String experimentFilePath) throws GMQLServiceException {

        Map<String, Set<String>> attr_to_value = new HashMap<>();
        String line = null;
        try {
            BufferedReader br = new BufferedReader(new FileReader(experimentFilePath));
            final Pattern pattern = Pattern.compile("\t");

            //experiments' metadata are stored in the format: id[\t]attribute[\t]value
            //precompile the pattern to speed-up the parsing		
            while ((line = br.readLine()) != null) {
                //split each file line
                String[] fields = pattern.split(line);
                //update the attr_to_value map
                Set<String> values = attr_to_value.get(fields[1]);
                if (values == null) {
                    values = new HashSet<>();
                    attr_to_value.put(fields[1].trim(), values);
                }
                values.add(fields[2].trim());

            }
            br.close();
        } catch (FileNotFoundException e) {
            throw new GMQLServiceException("File " + Paths.get(experimentFilePath).getFileName() + " not found in the repository.");
        } catch (NumberFormatException | IOException e) {
            throw new GMQLServiceException("An error has occurred while parsing file " + Paths.get(experimentFilePath).getFileName() + ".");
        }
        return attr_to_value;
    }
}
