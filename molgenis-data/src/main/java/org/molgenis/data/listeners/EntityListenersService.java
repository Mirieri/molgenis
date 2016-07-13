package org.molgenis.data.listeners;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import org.molgenis.data.Entity;
import org.molgenis.data.MolgenisDataException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

@Service
public final class EntityListenersService
{
	private final Logger LOG = LoggerFactory.getLogger(EntityListenersService.class);
	private final ConcurrentHashMap<String, SetMultimap<Object, EntityListener>> entityListeners = new ConcurrentHashMap<>();

	/**
	 * Register a repository to the entity listeners service once
	 *
	 * @param repoFullName
	 * @return
	 */
	void register(String repoFullName)
	{
		if (!entityListeners.containsKey(requireNonNull(repoFullName)))
		{
			entityListeners.put(repoFullName, HashMultimap.create());
		}
	}

	/**
	 * Update all registered listeners of the entities
	 *
	 * @param repoFullName
	 * @param entities
	 * @return Stream<Entity>
	 */
	Stream<Entity> updateEntities(String repoFullName, Stream<Entity> entities)
	{
		verifyRepoRegistered(repoFullName);
		SetMultimap<Object, EntityListener> entityListeners = this.entityListeners.get(repoFullName);
		return entities.filter(entity -> {
			Set<EntityListener> entityEntityListeners = entityListeners.get(entity.getIdValue());
			entityEntityListeners.forEach(entityListener -> {
				entityListener.postUpdate(entity);
			});
			return true;
		});
	}

	/**
	 * Update all registered listeners of an entity
	 *
	 * @param repoFullName
	 * @param entity
	 */
	void updateEntity(String repoFullName, Entity entity)
	{
		verifyRepoRegistered(repoFullName);
		SetMultimap<Object, EntityListener> entityListeners = this.entityListeners.get(repoFullName);
		Set<EntityListener> entityEntityListeners = entityListeners.get(entity.getIdValue());
		entityEntityListeners.forEach(entityListener -> {
			entityListener.postUpdate(entity);
		});
	}

	/**
	 * add a listener to a repository
	 *
	 * @param repoFullName
	 * @param entityListener
	 */
	void addEntityListener(String repoFullName, EntityListener entityListener)
	{
		verifyRepoRegistered(repoFullName);
		SetMultimap<Object, EntityListener> entityListeners = this.entityListeners.get(repoFullName);
		synchronized (entityListeners)
		{
			entityListeners.put(entityListener.getEntityId(), entityListener);
		}
	}

	/**
	 * remove a listener from a repository
	 *
	 * @param repoFullName
	 * @param entityListener
	 * @return
	 */
	boolean removeEntityListener(String repoFullName, EntityListener entityListener)
	{
		verifyRepoRegistered(repoFullName);
		SetMultimap<Object, EntityListener> entityListeners = this.entityListeners.get(repoFullName);
		synchronized (entityListeners)
		{
			if (entityListeners.containsKey(entityListener.getEntityId()))
			{
				entityListeners.remove(entityListener.getEntityId(), entityListener);
				return true;
			}
			return false;
		}
	}

	/**
	 * Check if a repository has no listeners
	 * Repository must be registered
	 *
	 * @param repoFullName
	 * @return boolean
	 */
	boolean isEmpty(String repoFullName)
	{
		verifyRepoRegistered(repoFullName);
		return entityListeners.get(repoFullName).isEmpty();
	}

	/**
	 * Verify that the repository is registered
	 *
	 * @param repoFullName
	 */
	protected void verifyRepoRegistered(String repoFullName)
	{
		if (!entityListeners.containsKey(requireNonNull(repoFullName)))
		{
			LOG.error("Repository [" + repoFullName + "] is not registered in the entity listeners service");
			throw new MolgenisDataException(
					"Repository [" + repoFullName + "] is not registered, please contact your administrator");
		}
	}
}

