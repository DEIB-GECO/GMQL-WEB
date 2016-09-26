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

import orchestrator.entities.Metadata;
import orchestrator.services.GQLServiceException;
import orchestrator.util.GQLFileUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/***
 * 
 * @author Massimo Quadrana
 * 
 */
public class ExperimentCache2 {
	
	private static ExperimentCache2 instance;
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
	
	private Cache<String, List<String>> attribute_list_cache;
	private Cache<String, List<String>> value_list_cache;
	private LoadingCache<String, Map<String, Set<Metadata>>> id_to_metadata_cache;
	private LoadingCache<String, Map<Metadata, Set<String>>> metadata_to_id_cache;
	private LoadingCache<String, Map<String, Set<String>>> attr_to_value_cache;
	
	private List<Cache<?,?>> cache_register;
	
	private final ScheduledExecutorService cleanUpService = Executors.newScheduledThreadPool(1);
	
	private ExperimentCache2(){
		
		cache_register = new ArrayList<>();
		
		attribute_list_cache = CacheBuilder.newBuilder()
				.maximumSize(MAXIMUM_CACHE_SIZE)
				.expireAfterAccess(EXPIRE_TIME, TimeUnit.SECONDS)
				.build();
		
		cache_register.add(attribute_list_cache);
		
		value_list_cache = CacheBuilder.newBuilder()
				.maximumSize(MAXIMUM_CACHE_SIZE)
				.expireAfterAccess(EXPIRE_TIME, TimeUnit.SECONDS)
				.build();
		
		cache_register.add(value_list_cache);
		
		id_to_metadata_cache = CacheBuilder.newBuilder()
				.maximumSize(MAXIMUM_CACHE_SIZE)
				.expireAfterAccess(EXPIRE_TIME, TimeUnit.SECONDS)
				.build(new CacheLoader<String, Map<String, Set<Metadata>>>(){
					@Override
					public Map<String, Set<Metadata>> load(String key)
							throws Exception {
						return GQLFileUtils.buildIdToMetadataMap(key);
					}					
				});
		
		cache_register.add(id_to_metadata_cache);
		
		metadata_to_id_cache = CacheBuilder.newBuilder()
				.maximumSize(MAXIMUM_CACHE_SIZE)
				.expireAfterAccess(EXPIRE_TIME, TimeUnit.SECONDS)
				.build(new CacheLoader<String, Map<Metadata, Set<String>>>(){
					@Override
					public Map<Metadata, Set<String>> load(String key)
							throws Exception {
						return GQLFileUtils.buildMetadataToIdMap(key);
					}					
				});
		
		cache_register.add(metadata_to_id_cache);
		
		attr_to_value_cache = CacheBuilder.newBuilder()
				.maximumSize(MAXIMUM_CACHE_SIZE)
				.expireAfterAccess(EXPIRE_TIME, TimeUnit.SECONDS)
				.build(new CacheLoader<String, Map<String, Set<String>>>(){
					@Override
					public Map<String, Set<String>> load(String key)
							throws Exception {
						return GQLFileUtils.buildAttributeToValueMap(key);
					}					
				});
		
		cache_register.add(attr_to_value_cache);
		
		//set up the scheduled clean-up service
		cleanUpService.scheduleAtFixedRate(new Runnable() {
			
			@Override
			public void run() {
				for(Cache<?,?> cache : cache_register)
					cache.cleanUp();
			}
		}, CLEANUP_DELAY, CLEANUP_DELAY, TimeUnit.SECONDS);
		
	}
	
	/***
	 * Returns the current instance of the class
	 * @return An instance of {@code ExperimentCache}
	 */
	public static ExperimentCache2 getInstance(){
		if(instance == null){
			instance = new ExperimentCache2();
		}
		return instance;
	}
	
	/***
	 * Returns the set of {@code Metadata} associated with an experimentId into an experiment file.
	 * If the file was not present into the cache when the function is called, then the experiment file is parsed and loaded into cache 
	 * before returning the result set.
	 * @param experimentFilePath {@code Path} instance of the experiment file to be used
	 * @param experimentId Id of the experiment to extracted
	 * @return A set of {@code Metadata} instances related with the experiment
	 * @throws GQLServiceException if the experimentFile or the experimentId cannot be found
	 */
	public Set<Metadata> getMetadataFromId(Path experimentFilePath, String experimentId) throws GQLServiceException {
		try{
			return id_to_metadata_cache.get(experimentFilePath.toString()).get(experimentId);
		}catch(ExecutionException e){
			throw new GQLServiceException(e.getMessage());
		}
	}
	
	/***
	 * Returns the set of unique ids associated with a {@code Metadata} into an experiment file.
	 * If the file was not present into the cache when the function is called, then the experiment file is parsed and loaded into cache 
	 * before returning the result set.
	 * @param experimentFilePath {@code Path} instance of the experiment file to be used
	 * @param meta Instance of {@code Metadata} to be searched
	 * @return Set of unique experiment ids associated with the {@code Metadata} meta
	 * @throws GQLServiceException if the experimenFile or the {@code Metadata} cannot be found
	 */
	public Set<String> getIdsFromMetadata(Path experimentFilePath, Metadata meta) throws GQLServiceException{
		try {
			return metadata_to_id_cache.get(experimentFilePath.toString()).get(meta);
		} catch (ExecutionException e) {
			throw new GQLServiceException(e.getMessage());
		}
	}
	
	/***
	 * Returns the set of unique attributes contained into an experiment file.
	 * If the file was not present into the cache when the function is called, then the experiment file is parsed and loaded into cache 
	 * before returning the result set. 
	 * @param experimentFilePath {@code Path} instance of the experiment file to be used
	 * @return Set of unique attributes conteined into an experiment file
	 * @throws GQLServiceException if the experiementFile cannot be found
	 */
	public Set<String> getAttributes(Path experimentFilePath) throws GQLServiceException{
		try{
			return attr_to_value_cache.get(experimentFilePath.toString()).keySet();
		}catch(ExecutionException e){
			throw new GQLServiceException(e.getMessage());
		}
	}
	
	/***
	 * Returns the set of unique values associated with an attribute
	 * If the file was not present into the cache when the function is called, then the experiment file is parsed and loaded into cache 
	 * before returning the result set.
	 * @param experimentFilePath {@code Path} instance of the experiment file to be used
	 * @param attributeName Name of the attribute to be extracted
	 * @return Set of unique values associated with the passed attributeName
	 * @throws GQLServiceException if the experimenFile or the attribute cannot be found
	 */
	public Set<String> getValuesFromAttribute(Path experimentFilePath, String attributeName) throws GQLServiceException{
		try{
			return attr_to_value_cache.get(experimentFilePath.toString()).get(attributeName);			
		}catch(ExecutionException e){
			throw new GQLServiceException(e.getMessage());
		}
	}
	
}
