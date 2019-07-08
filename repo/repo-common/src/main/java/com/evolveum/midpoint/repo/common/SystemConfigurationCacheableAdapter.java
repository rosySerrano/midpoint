/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.common;

import com.evolveum.midpoint.CacheInvalidationContext;
import com.evolveum.midpoint.repo.api.Cacheable;
import com.evolveum.midpoint.repo.api.SystemConfigurationChangeDispatcher;
import com.evolveum.midpoint.repo.cache.CacheRegistry;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * @author mederly
 */
@Component
public class SystemConfigurationCacheableAdapter implements Cacheable {

	private static final Trace LOGGER = TraceManager.getTrace(SystemConfigurationCacheableAdapter.class);

	@Autowired private CacheRegistry cacheRegistry;
	@Autowired private SystemConfigurationChangeDispatcher systemConfigurationChangeDispatcher;

	@PostConstruct
	public void register() {
		cacheRegistry.registerCacheableService(this);
	}

	@PreDestroy
	public void unregister() {
		cacheRegistry.unregisterCacheableService(this);
	}

	@Override
	public void invalidate(Class<?> type, String oid, CacheInvalidationContext context) {
		if (type == null || SystemConfigurationType.class.isAssignableFrom(type)) {
			// We ignore OID by now, assuming there's only a single system configuration object
			try {
				OperationResult result = new OperationResult(SystemConfigurationCacheableAdapter.class.getName() + ".invalidate");
				systemConfigurationChangeDispatcher.dispatch(true, true, result);
			} catch (Throwable t) {
				LoggingUtils
						.logUnexpectedException(LOGGER, "Couldn't dispatch information about updated system configuration", t);
			}
		}
	}

}