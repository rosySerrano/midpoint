/*
 * Copyright (c) 2013 Evolveum
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
package com.evolveum.midpoint.provisioning.impl;

import java.util.Collection;

import javax.xml.namespace.QName;

import net.sf.saxon.type.SchemaException;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.refinery.PropertyLimitations;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.PropertyAccessType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;

/**
 * @author Radovan Semancik
 *
 */
@Component
public class AccessChecker {
	
	public static final String OPERATION_NAME = AccessChecker.class.getName()+".accessCheck";
	private static final Trace LOGGER = TraceManager.getTrace(AccessChecker.class);

	public void checkAdd(ResourceType resource, PrismObject<ShadowType> shadow,
			RefinedObjectClassDefinition objectClassDefinition, OperationResult parentResult) throws SchemaException, SecurityViolationException {
		OperationResult result = parentResult.createMinorSubresult(OPERATION_NAME);
		ResourceAttributeContainer attributeCont = ShadowUtil.getAttributesContainer(shadow);
		
		for (ResourceAttribute<?> attribute: attributeCont.getAttributes()) {
			RefinedAttributeDefinition attrDef = objectClassDefinition.findAttributeDefinition(attribute.getName());
			// Need to check model layer, not schema. Model means IDM logic which can be overridden in schemaHandling,
			// schema layer is the original one. 
			PropertyLimitations limitations = attrDef.getLimitations(LayerType.MODEL);
			if (limitations == null) {
				continue;
			}
			// We cannot throw error here. At least not now. Provisioning will internally use ignored attributes
			// e.g. for simulated capabilities. This is not a problem for normal operations, but it is a problem
			// for delayed operations (e.g. consistency) that are passing through this code again.
			// TODO: we need to figure a way how to avoid this loop
//			if (limitations.isIgnore()) {
//				String message = "Attempt to create shadow with ignored attribute "+attribute.getName();
//				LOGGER.error(message);
//				throw new SchemaException(message);
//			}
			PropertyAccessType access = limitations.getAccess();
			if (access == null) {
				continue;
			}
			if (access.isCreate() == null || !access.isCreate()) {
				String message = "Attempt to add shadow with non-createable attribute "+attribute.getName();
				LOGGER.error(message);
				result.recordFatalError(message);
				throw new SecurityViolationException(message);
			}
		}
		result.recordSuccess();
	}

	public void checkModify(ResourceType resource, PrismObject<ShadowType> shadow,
			Collection<? extends ItemDelta> modifications, RefinedObjectClassDefinition objectClassDefinition,
			OperationResult parentResult) throws SecurityViolationException {
		
		OperationResult result = parentResult.createMinorSubresult(OPERATION_NAME);
		for (ItemDelta modification: modifications) {
			if (!(modification instanceof PropertyDelta<?>)) {
				continue;
			}
			PropertyDelta<?> attrDelta = (PropertyDelta<?>)modification;
			if (!SchemaConstants.PATH_ATTRIBUTES.equals(attrDelta.getParentPath())) {
				// Not an attribute
				continue;
			}
			QName attrName = attrDelta.getName();
			RefinedAttributeDefinition attrDef = objectClassDefinition.findAttributeDefinition(attrName);
			PropertyLimitations limitations = attrDef.getLimitations(LayerType.MODEL);
			if (limitations == null) {
				continue;
			}
			// We cannot throw error here. At least not now. Provisioning will internally use ignored attributes
			// e.g. for simulated capabilities. This is not a problem for normal operations, but it is a problem
			// for delayed operations (e.g. consistency) that are passing through this code again.
			// TODO: we need to figure a way how to avoid this loop
//			if (limitations.isIgnore()) {
//				String message = "Attempt to create shadow with ignored attribute "+attribute.getName();
//				LOGGER.error(message);
//				throw new SchemaException(message);
//			}
			PropertyAccessType access = limitations.getAccess();
			if (access == null) {
				continue;
			}
			if (access.isUpdate() == null || !access.isUpdate()) {
				String message = "Attempt to modify non-updateable attribute "+attrName;
				LOGGER.error(message);
				result.recordFatalError(message);
				throw new SecurityViolationException(message);
			}
		}
		result.recordSuccess();
		
	}

	public void filterGetAttributes(ResourceAttributeContainer attributeContainer, RefinedObjectClassDefinition objectClassDefinition, OperationResult parentResult) {
		OperationResult result = parentResult.createMinorSubresult(OPERATION_NAME);
		
		
		for (ResourceAttribute<?> attribute: attributeContainer.getAttributes()) {
			QName attrName = attribute.getName();
			RefinedAttributeDefinition attrDef = objectClassDefinition.findAttributeDefinition(attrName);
			// Need to check model layer, not schema. Model means IDM logic which can be overridden in schemaHandling,
			// schema layer is the original one. 
			PropertyLimitations limitations = attrDef.getLimitations(LayerType.MODEL);
			if (limitations == null) {
				continue;
			}
			// We cannot throw error here. At least not now. Provisioning will internally use ignored attributes
			// e.g. for simulated capabilities. This is not a problem for normal operations, but it is a problem
			// for delayed operations (e.g. consistency) that are passing through this code again.
			// TODO: we need to figure a way how to avoid this loop
//			if (limitations.isIgnore()) {
//				String message = "Attempt to create shadow with ignored attribute "+attribute.getName();
//				LOGGER.error(message);
//				throw new SchemaException(message);
//			}
			PropertyAccessType access = limitations.getAccess();
			if (access == null) {
				continue;
			}
			if (access.isRead() == null || !access.isRead()) {
				LOGGER.trace("Removing non-readable attribute {}", attrName);
				attributeContainer.remove(attribute);
			}
		}
		result.recordSuccess();
	}
	
	

}
