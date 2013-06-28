/*
 * Copyright (c) 2010-2013 Evolveum
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

/**
 * 
 */
package com.evolveum.midpoint.provisioning.test.impl;

import static com.evolveum.midpoint.test.DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME;
import static com.evolveum.midpoint.test.DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME;
import static com.evolveum.midpoint.test.IntegrationTestTools.assertFailure;
import static com.evolveum.midpoint.test.IntegrationTestTools.assertProvisioningAccountShadow;
import static com.evolveum.midpoint.test.IntegrationTestTools.assertSuccess;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;
import org.w3c.dom.Element;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyGroup;
import com.evolveum.icf.dummy.resource.DummyPrivilege;
import com.evolveum.icf.dummy.resource.DummySyncStyle;
import com.evolveum.midpoint.common.monitor.InternalMonitor;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.DiffUtil;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.ProvisioningTestUtil;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.impl.ConnectorFactoryIcfImpl;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ConnectorTypeUtil;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.ObjectChecker;
import com.evolveum.midpoint.test.ProvisioningScriptSpec;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.CachingMetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.CapabilitiesType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.CapabilityCollectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.OperationProvisioningScriptsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ProvisioningScriptType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.XmlSchemaType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_2.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_2.CredentialsCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_2.ScriptCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_2.TestConnectionCapabilityType;

/**
 * The test of Provisioning service on the API level. The test is using dummy
 * resource for speed and flexibility.
 * 
 * @author Radovan Semancik
 * 
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestDummy extends AbstractDummyTest {

	private static final String BLACKBEARD_USERNAME = "blackbeard";
	private static final String DRAKE_USERNAME = "drake";

	private static final Trace LOGGER = TraceManager.getTrace(TestDummy.class);
	private static final long VALID_FROM_MILLIS = 12322342345435L;
	private static final long VALID_TO_MILLIS = 3454564324423L;
	
//	private Task syncTask = null;
	private CachingMetadataType capabilitiesCachingMetadataType;
	private String drakeAccountOid;

	@Test
	public void test000Integrity() throws ObjectNotFoundException, SchemaException {
		TestUtil.displayTestTile("test000Integrity");

		display("Dummy resource instance", dummyResource.toString());

		assertNotNull("Resource is null", resource);
		assertNotNull("ResourceType is null", resourceType);

		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ ".test000Integrity");

		ResourceType resource = repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, result)
				.asObjectable();
		String connectorOid = resource.getConnectorRef().getOid();
		ConnectorType connector = repositoryService.getObject(ConnectorType.class, connectorOid, result).asObjectable();
		assertNotNull(connector);
		display("Dummy Connector", connector);
		
		result.computeStatus();
		display("getObject result", result);
		assertSuccess(result);
		
		

		// Check connector schema
		ProvisioningTestUtil.assertConnectorSchemaSanity(connector, prismContext);
	}

	/**
	 * Check whether the connectors were discovered correctly and were added to
	 * the repository.
	 * 
	 * @throws SchemaException
	 * 
	 */
	@Test
	public void test001Connectors() throws SchemaException {
		TestUtil.displayTestTile("test001Connectors");
		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ ".test001Connectors");

		// WHEN
		List<PrismObject<ConnectorType>> connectors = repositoryService.searchObjects(ConnectorType.class,
				new ObjectQuery(), result);

		// THEN
		result.computeStatus();
		display("searchObjects result", result);
		assertSuccess(result);

		assertFalse("No connector found", connectors.isEmpty());
		for (PrismObject<ConnectorType> connPrism : connectors) {
			ConnectorType conn = connPrism.asObjectable();
			display("Found connector " + conn, conn);

			display("XML " + conn, PrismTestUtil.serializeObjectToString(connPrism));

			XmlSchemaType xmlSchemaType = conn.getSchema();
			assertNotNull("xmlSchemaType is null", xmlSchemaType);
			Element connectorXsdSchemaElement = ConnectorTypeUtil.getConnectorXsdSchema(conn);
			assertNotNull("No schema", connectorXsdSchemaElement);

			// Try to parse the schema
			PrismSchema schema = PrismSchema.parse(connectorXsdSchemaElement, true, "connector schema " + conn, prismContext);
			assertNotNull("Cannot parse schema", schema);
			assertFalse("Empty schema", schema.isEmpty());

			display("Parsed connector schema " + conn, schema);

			QName configurationElementQname = new QName(conn.getNamespace(), ResourceType.F_CONNECTOR_CONFIGURATION.getLocalPart());
			PrismContainerDefinition configurationContainer = schema
					.findContainerDefinitionByElementName(configurationElementQname);
			assertNotNull("No " + configurationElementQname + " element in schema of " + conn, configurationContainer);
			PrismContainerDefinition definition = schema.findItemDefinition(ResourceType.F_CONNECTOR_CONFIGURATION.getLocalPart(),
					PrismContainerDefinition.class);
			assertNotNull("Definition of <configuration> property container not found", definition);
			PrismContainerDefinition pcd = (PrismContainerDefinition) definition;
			assertFalse("Empty definition", pcd.isEmpty());
		}
	}

	/**
	 * Running discovery for a second time should return nothing - as nothing
	 * new was installed in the meantime.
	 */
	@Test
	public void test002ConnectorRediscovery() {
		TestUtil.displayTestTile("test002ConnectorRediscovery");
		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ ".test002ConnectorRediscovery");

		// WHEN
		Set<ConnectorType> discoverLocalConnectors = connectorManager.discoverLocalConnectors(result);

		// THEN
		result.computeStatus();
		display("discoverLocalConnectors result", result);
		assertSuccess("discoverLocalConnectors failed", result);
		assertTrue("Rediscovered something", discoverLocalConnectors.isEmpty());
	}

	/**
	 * This should be the very first test that works with the resource.
	 * 
	 * The original repository object does not have resource schema. The schema
	 * should be generated from the resource on the first use. This is the test
	 * that executes testResource and checks whether the schema was generated.
	 */
	@Test
	public void test003Connection() throws ObjectNotFoundException, SchemaException {
		TestUtil.displayTestTile("test003Connection");
		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ ".test003Connection");
		
		// Some connector initialization and other things might happen in previous tests.
		// The monitor is static, not part of spring context, it will not be cleared
		rememberConnectorSchemaFetchCount();
		rememberConnectorInitializationCount();
		rememberResourceSchemaParseCount();
		rememberResourceCacheStats();
		
		// Check that there is no schema before test (pre-condition)
		PrismObject<ResourceType> resourceBefore = repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, result);
		ResourceType resourceTypeBefore = resourceBefore.asObjectable();
		rememberResourceVersion(resourceBefore.getVersion());
		assertNotNull("No connector ref", resourceTypeBefore.getConnectorRef());
		assertNotNull("No connector ref OID", resourceTypeBefore.getConnectorRef().getOid());
		ConnectorType connector = repositoryService.getObject(ConnectorType.class,
				resourceTypeBefore.getConnectorRef().getOid(), result).asObjectable();
		assertNotNull(connector);
		XmlSchemaType xmlSchemaTypeBefore = resourceTypeBefore.getSchema();
		Element resourceXsdSchemaElementBefore = ResourceTypeUtil.getResourceXsdSchema(resourceTypeBefore);
		AssertJUnit.assertNull("Found schema before test connection. Bad test setup?", resourceXsdSchemaElementBefore);

		// WHEN
		OperationResult testResult = provisioningService.testResource(RESOURCE_DUMMY_OID);

		// THEN
		display("Test result", testResult);
		assertSuccess("Test resource failed (result)", testResult);

		PrismObject<ResourceType> resourceRepoAfter = repositoryService.getObject(ResourceType.class,
				RESOURCE_DUMMY_OID, result);
		ResourceType resourceTypeRepoAfter = resourceRepoAfter.asObjectable();
		display("Resource after test", resourceTypeRepoAfter);

		XmlSchemaType xmlSchemaTypeAfter = resourceTypeRepoAfter.getSchema();
		assertNotNull("No schema after test connection", xmlSchemaTypeAfter);
		Element resourceXsdSchemaElementAfter = ResourceTypeUtil.getResourceXsdSchema(resourceTypeRepoAfter);
		assertNotNull("No schema after test connection", resourceXsdSchemaElementAfter);

		String resourceXml = prismContext.getPrismDomProcessor().serializeObjectToString(resourceRepoAfter);
		display("Resource XML", resourceXml);

		CachingMetadataType cachingMetadata = xmlSchemaTypeAfter.getCachingMetadata();
		assertNotNull("No caching metadata", cachingMetadata);
		assertNotNull("No retrievalTimestamp", cachingMetadata.getRetrievalTimestamp());
		assertNotNull("No serialNumber", cachingMetadata.getSerialNumber());

		Element xsdElement = ObjectTypeUtil.findXsdElement(xmlSchemaTypeAfter);
		ResourceSchema parsedSchema = ResourceSchema.parse(xsdElement, resourceTypeBefore.toString(), prismContext);
		assertNotNull("No schema after parsing", parsedSchema);

		// schema will be checked in next test
		
		assertConnectorSchemaFetchIncrement(1);
		assertConnectorInitializationCountIncrement(1);
		assertResourceSchemaParseCountIncrement(1);
		// One increment for availablity status, the other for schema
		assertResourceVersionIncrement(resourceRepoAfter, 2);
	
	}

	@Test
	public void test004Configuration() throws ObjectNotFoundException, CommunicationException, SchemaException,
			ConfigurationException, SecurityViolationException {
		TestUtil.displayTestTile("test004Configuration");
		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ ".test004Configuration");

		// WHEN
		resource = provisioningService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, result);
		resourceType = resource.asObjectable();
		
		// THEN
		result.computeStatus();
		display("getObject result", result);
		assertSuccess(result);

		// There may be one parse. Previous test have changed the resource version
		// Schema for this version will not be re-parsed until getObject is tried
		assertResourceSchemaParseCountIncrement(1);
		assertResourceCacheMissesIncrement(1);
		
		PrismContainer<Containerable> configurationContainer = resource.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
		assertNotNull("No configuration container", configurationContainer);
		PrismContainerDefinition confContDef = configurationContainer.getDefinition();
		assertNotNull("No configuration container definition", confContDef);
		PrismContainer confingurationPropertiesContainer = configurationContainer
				.findContainer(ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_CONFIGURATION_PROPERTIES_ELEMENT_QNAME);
		assertNotNull("No configuration properties container", confingurationPropertiesContainer);
		PrismContainerDefinition confPropsDef = confingurationPropertiesContainer.getDefinition();
		assertNotNull("No configuration properties container definition", confPropsDef);
		List<PrismProperty<?>> configurationProperties = confingurationPropertiesContainer.getValue().getItems();
		assertFalse("No configuration properties", configurationProperties.isEmpty());
		for (PrismProperty<?> confProp : configurationProperties) {
			PrismPropertyDefinition confPropDef = confProp.getDefinition();
			assertNotNull("No definition for configuration property " + confProp, confPropDef);
			assertFalse("Configuration property " + confProp + " is raw", confProp.isRaw());
		}
		
		// The useless configuration variables should be reflected to the resource now
		assertEquals("Wrong useless string", "Shiver me timbers!", dummyResource.getUselessString());
		assertEquals("Wrong guarded useless string", "Dead men tell no tales", dummyResource.getUselessGuardedString());
		
		resource.checkConsistence();
		
		rememberSchemaMetadata(resource);
		rememberConnectorInstance(resource);
		
		assertSteadyResource();
	}

	@Test
	public void test005ParsedSchema() throws ObjectNotFoundException, CommunicationException, SchemaException,
			ConfigurationException {
		TestUtil.displayTestTile("test005ParsedSchema");
		// GIVEN

		// THEN
		// The returned type should have the schema pre-parsed
		assertNotNull(RefinedResourceSchema.hasParsedSchema(resourceType));

		// Also test if the utility method returns the same thing
		ResourceSchema returnedSchema = RefinedResourceSchema.getResourceSchema(resourceType, prismContext);

		display("Parsed resource schema", returnedSchema);

		// Check whether it is reusing the existing schema and not parsing it
		// all over again
		// Not equals() but == ... we want to really know if exactly the same
		// object instance is returned
		assertTrue("Broken caching",
				returnedSchema == RefinedResourceSchema.getResourceSchema(resourceType, prismContext));

		assertSchemaSanity(returnedSchema, resourceType);
		
		rememberResourceSchema(returnedSchema);
		assertSteadyResource();
	}

	@Test
	public void test006RefinedSchema() throws ObjectNotFoundException, CommunicationException, SchemaException,
			ConfigurationException {
		TestUtil.displayTestTile("test006RefinedSchema");
		// GIVEN

		// WHEN
		RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(resourceType, prismContext);
		display("Refined schema", refinedSchema);

		// Check whether it is reusing the existing schema and not parsing it
		// all over again
		// Not equals() but == ... we want to really know if exactly the same
		// object instance is returned
		assertTrue("Broken caching",
				refinedSchema == RefinedResourceSchema.getRefinedSchema(resourceType, prismContext));

		RefinedObjectClassDefinition accountDef = refinedSchema.getDefaultRefinedDefinition(ShadowKindType.ACCOUNT);
		assertNotNull("Account definition is missing", accountDef);
		assertNotNull("Null identifiers in account", accountDef.getIdentifiers());
		assertFalse("Empty identifiers in account", accountDef.getIdentifiers().isEmpty());
		assertNotNull("Null secondary identifiers in account", accountDef.getSecondaryIdentifiers());
		assertFalse("Empty secondary identifiers in account", accountDef.getSecondaryIdentifiers().isEmpty());
		assertNotNull("No naming attribute in account", accountDef.getNamingAttribute());
		assertFalse("No nativeObjectClass in account", StringUtils.isEmpty(accountDef.getNativeObjectClass()));

		RefinedAttributeDefinition uidDef = accountDef.findAttributeDefinition(ConnectorFactoryIcfImpl.ICFS_UID);
		assertEquals(1, uidDef.getMaxOccurs());
		assertEquals(0, uidDef.getMinOccurs());
		assertFalse("No UID display name", StringUtils.isBlank(uidDef.getDisplayName()));
		assertFalse("UID has create", uidDef.canCreate());
		assertFalse("UID has update", uidDef.canUpdate());
		assertTrue("No UID read", uidDef.canRead());
		assertTrue("UID definition not in identifiers", accountDef.getIdentifiers().contains(uidDef));

		RefinedAttributeDefinition nameDef = accountDef.findAttributeDefinition(ConnectorFactoryIcfImpl.ICFS_NAME);
		assertEquals(1, nameDef.getMaxOccurs());
		assertEquals(1, nameDef.getMinOccurs());
		assertFalse("No NAME displayName", StringUtils.isBlank(nameDef.getDisplayName()));
		assertTrue("No NAME create", nameDef.canCreate());
		assertTrue("No NAME update", nameDef.canUpdate());
		assertTrue("No NAME read", nameDef.canRead());
		assertTrue("NAME definition not in identifiers", accountDef.getSecondaryIdentifiers().contains(nameDef));

		RefinedAttributeDefinition fullnameDef = accountDef.findAttributeDefinition("fullname");
		assertNotNull("No definition for fullname", fullnameDef);
		assertEquals(1, fullnameDef.getMaxOccurs());
		assertEquals(1, fullnameDef.getMinOccurs());
		assertTrue("No fullname create", fullnameDef.canCreate());
		assertTrue("No fullname update", fullnameDef.canUpdate());
		assertTrue("No fullname read", fullnameDef.canRead());

		assertNull("The _PASSSWORD_ attribute sneaked into schema",
				accountDef.findAttributeDefinition(new QName(ConnectorFactoryIcfImpl.NS_ICF_SCHEMA, "password")));
		
		rememberRefinedResourceSchema(refinedSchema);
		assertSteadyResource();
	}

	@Test
	public void test007Capabilities() throws Exception {
		final String TEST_NAME = "test007Capabilities";
		TestUtil.displayTestTile(TEST_NAME);

		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ "." + TEST_NAME);

		// WHEN
		PrismObject<ResourceType> resource = provisioningService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, result);
		ResourceType resourceType = resource.asObjectable();

		// THEN
		result.computeStatus();
		display("getObject result", result);
		assertSuccess(result);

		// Check native capabilities
		CapabilityCollectionType nativeCapabilities = resourceType.getCapabilities().getNative();
		display("Native capabilities", PrismTestUtil.marshalWrap(nativeCapabilities));
		display("Resource", resourceType);
		List<Object> nativeCapabilitiesList = nativeCapabilities.getAny();
		assertFalse("Empty capabilities returned", nativeCapabilitiesList.isEmpty());
		CredentialsCapabilityType capCred = CapabilityUtil.getCapability(nativeCapabilitiesList,
				CredentialsCapabilityType.class);
		assertNotNull("password native capability not present", capCred.getPassword());
		ActivationCapabilityType capAct = CapabilityUtil.getCapability(nativeCapabilitiesList,
				ActivationCapabilityType.class);
		assertNotNull("native activation capability not present", capAct);
		assertNotNull("native activation status capability not present", capAct.getStatus());
		TestConnectionCapabilityType capTest = CapabilityUtil.getCapability(nativeCapabilitiesList,
				TestConnectionCapabilityType.class);
		assertNotNull("native test capability not present", capTest);
		ScriptCapabilityType capScript = CapabilityUtil.getCapability(nativeCapabilitiesList,
				ScriptCapabilityType.class);
		assertNotNull("native script capability not present", capScript);
		assertNotNull("No host in native script capability", capScript.getHost());
		assertFalse("No host in native script capability", capScript.getHost().isEmpty());
		// TODO: better look inside
		
		capabilitiesCachingMetadataType = resourceType.getCapabilities().getCachingMetadata();
		assertNotNull("No capabilities caching metadata", capabilitiesCachingMetadataType);
		assertNotNull("No capabilities caching metadata timestamp", capabilitiesCachingMetadataType.getRetrievalTimestamp());
		assertNotNull("No capabilities caching metadata serial number", capabilitiesCachingMetadataType.getSerialNumber());

		// Check effective capabilites
		capCred = ResourceTypeUtil.getEffectiveCapability(resourceType, CredentialsCapabilityType.class);
		assertNotNull("password capability not found", capCred.getPassword());
		// Although connector does not support activation, the resource
		// specifies a way how to simulate it.
		// Therefore the following should succeed
		capAct = ResourceTypeUtil.getEffectiveCapability(resourceType, ActivationCapabilityType.class);
		assertNotNull("activation capability not found", capCred.getPassword());

		List<Object> effectiveCapabilities = ResourceTypeUtil.getEffectiveCapabilities(resourceType);
		for (Object capability : effectiveCapabilities) {
			System.out.println("Capability: " + CapabilityUtil.getCapabilityDisplayName(capability) + " : "
					+ capability);
		}
		
		assertSteadyResource();
	}
	
	/**
	 * Check if the cached native capabilities were properly stored in the repo 
	 */
	@Test
	public void test008CapabilitiesRepo() throws Exception {
		final String TEST_NAME = "test008CapabilitiesRepo";
		TestUtil.displayTestTile(TEST_NAME);

		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ "." + TEST_NAME);

		// WHEN
		PrismObject<ResourceType> resource = repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, result);;

		// THEN
		result.computeStatus();
		display("getObject result", result);
		assertSuccess(result);

		// Check native capabilities
		ResourceType resourceType = resource.asObjectable();
		CapabilitiesType capabilitiesType = resourceType.getCapabilities();
		assertNotNull("No capabilities in repo, the capabilities were not cached", capabilitiesType);
		CapabilityCollectionType nativeCapabilities = capabilitiesType.getNative();
		System.out.println("Native capabilities: " + PrismTestUtil.marshalWrap(nativeCapabilities));
		System.out.println("resource: " + resourceType.asPrismObject().dump());
		List<Object> nativeCapabilitiesList = nativeCapabilities.getAny();
		assertFalse("Empty capabilities returned", nativeCapabilitiesList.isEmpty());
		CredentialsCapabilityType capCred = CapabilityUtil.getCapability(nativeCapabilitiesList,
				CredentialsCapabilityType.class);
		assertNotNull("password native capability not present", capCred.getPassword());
		ActivationCapabilityType capAct = CapabilityUtil.getCapability(nativeCapabilitiesList,
				ActivationCapabilityType.class);
		assertNotNull("native activation capability not present", capAct);
		assertNotNull("native activation status capability not present", capAct.getStatus());
		TestConnectionCapabilityType capTest = CapabilityUtil.getCapability(nativeCapabilitiesList,
				TestConnectionCapabilityType.class);
		assertNotNull("native test capability not present", capTest);
		ScriptCapabilityType capScript = CapabilityUtil.getCapability(nativeCapabilitiesList,
				ScriptCapabilityType.class);
		assertNotNull("native script capability not present", capScript);
		assertNotNull("No host in native script capability", capScript.getHost());
		assertFalse("No host in native script capability", capScript.getHost().isEmpty());
		// TODO: better look inside

		CachingMetadataType repoCapabilitiesCachingMetadataType = capabilitiesType.getCachingMetadata();
		assertNotNull("No repo capabilities caching metadata", repoCapabilitiesCachingMetadataType);
		assertNotNull("No repo capabilities caching metadata timestamp", repoCapabilitiesCachingMetadataType.getRetrievalTimestamp());
		assertNotNull("No repo capabilities caching metadata serial number", repoCapabilitiesCachingMetadataType.getSerialNumber());
		assertEquals("Repo capabilities caching metadata timestamp does not match previously returned value", 
				capabilitiesCachingMetadataType.getRetrievalTimestamp(), repoCapabilitiesCachingMetadataType.getRetrievalTimestamp());
		assertEquals("Repo capabilities caching metadata serial does not match previously returned value", 
				capabilitiesCachingMetadataType.getSerialNumber(), repoCapabilitiesCachingMetadataType.getSerialNumber());

		assertSteadyResource();
	}

	@Test
	public void test010ResourceAndConnectorCaching() throws Exception {
		TestUtil.displayTestTile("test010ResourceAndConnectorCaching");

		// GIVEN
		OperationResult result = new OperationResult(TestOpenDJ.class.getName()
				+ ".test010ResourceAndConnectorCaching");
		ConnectorInstance configuredConnectorInstance = connectorManager.getConfiguredConnectorInstance(
				resource, false, result);
		assertNotNull("No configuredConnectorInstance", configuredConnectorInstance);
		ResourceSchema resourceSchema = RefinedResourceSchema.getResourceSchema(resource, prismContext);
		assertNotNull("No resource schema", resourceSchema);

		// WHEN
		PrismObject<ResourceType> resourceAgain = provisioningService.getObject(ResourceType.class, RESOURCE_DUMMY_OID,
				null, result);

		// THEN
		result.computeStatus();
		display("getObject result", result);
		assertSuccess(result);

		ResourceType resourceTypeAgain = resourceAgain.asObjectable();
		assertNotNull("No connector ref", resourceTypeAgain.getConnectorRef());
		assertNotNull("No connector ref OID", resourceTypeAgain.getConnectorRef().getOid());

		PrismContainer<Containerable> configurationContainer = resource.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
		PrismContainer<Containerable> configurationContainerAgain = resourceAgain
				.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
		assertTrue("Configurations not equivalent", configurationContainer.equivalent(configurationContainerAgain));

		// Check resource schema caching
		ResourceSchema resourceSchemaAgain = RefinedResourceSchema.getResourceSchema(resourceAgain, prismContext);
		assertNotNull("No resource schema (again)", resourceSchemaAgain);
		assertTrue("Resource schema was not cached", resourceSchema == resourceSchemaAgain);
		
		// Check capabilities caching
		
		CapabilitiesType capabilitiesType = resourceType.getCapabilities();
		assertNotNull("No capabilities fetched from provisioning", capabilitiesType);
		CachingMetadataType capCachingMetadataType = capabilitiesType.getCachingMetadata();
		assertNotNull("No capabilities caching metadata fetched from provisioning", capCachingMetadataType);
		CachingMetadataType capCachingMetadataTypeAgain = resourceTypeAgain.getCapabilities().getCachingMetadata();
		assertEquals("Capabilities caching metadata serial number has changed", capCachingMetadataType.getSerialNumber(), 
				capCachingMetadataTypeAgain.getSerialNumber());
		assertEquals("Capabilities caching metadata timestamp has changed", capCachingMetadataType.getRetrievalTimestamp(), 
				capCachingMetadataTypeAgain.getRetrievalTimestamp());
		
		// Rough test if everything is fine
		resource.asObjectable().setFetchResult(null);
		resourceAgain.asObjectable().setFetchResult(null);
		ObjectDelta<ResourceType> dummyResourceDiff = DiffUtil.diff(resource, resourceAgain);
        display("Dummy resource diff", dummyResourceDiff);
        assertTrue("The resource read again is not the same as the original. diff:"+dummyResourceDiff, dummyResourceDiff.isEmpty());

		// Now we stick our nose deep inside the provisioning impl. But we need
		// to make sure that the
		// configured connector is properly cached
		ConnectorInstance configuredConnectorInstanceAgain = connectorManager.getConfiguredConnectorInstance(
				resourceAgain, false, result);
		assertNotNull("No configuredConnectorInstance (again)", configuredConnectorInstanceAgain);
		assertTrue("Connector instance was not cached", configuredConnectorInstance == configuredConnectorInstanceAgain);

		// Check if the connector still works.
		OperationResult testResult = new OperationResult(TestOpenDJ.class.getName()
				+ ".test010ResourceAndConnectorCaching.test");
		configuredConnectorInstanceAgain.test(testResult);
		testResult.computeStatus();
		assertSuccess("Connector test failed", testResult);
		
		// Test connection should also refresh the connector by itself. So check if it has been refreshed
		
		ConnectorInstance configuredConnectorInstanceAfterTest = connectorManager.getConfiguredConnectorInstance(
				resourceAgain, false, result);
		assertNotNull("No configuredConnectorInstance (again)", configuredConnectorInstanceAfterTest);
		assertTrue("Connector instance was not cached", configuredConnectorInstanceAgain == configuredConnectorInstanceAfterTest);
		
		assertSteadyResource();
	}

	@Test
	public void test011ResourceAndConnectorCachingForceFresh() throws Exception {
		TestUtil.displayTestTile("test011ResourceAndConnectorCachingForceFresh");

		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ ".test011ResourceAndConnectorCachingForceFresh");
		ConnectorInstance configuredConnectorInstance = connectorManager.getConfiguredConnectorInstance(
				resource, false, result);
		assertNotNull("No configuredConnectorInstance", configuredConnectorInstance);
		ResourceSchema resourceSchema = RefinedResourceSchema.getResourceSchema(resource, prismContext);
		assertNotNull("No resource schema", resourceSchema);

		// WHEN
		PrismObject<ResourceType> resourceAgain = provisioningService.getObject(ResourceType.class, RESOURCE_DUMMY_OID,
				null, result);

		// THEN
		result.computeStatus();
		display("getObject result", result);
		assertSuccess(result);

		ResourceType resourceTypeAgain = resourceAgain.asObjectable();
		assertNotNull("No connector ref", resourceTypeAgain.getConnectorRef());
		assertNotNull("No connector ref OID", resourceTypeAgain.getConnectorRef().getOid());

		PrismContainer<Containerable> configurationContainer = resource.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
		PrismContainer<Containerable> configurationContainerAgain = resourceAgain
				.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
		assertTrue("Configurations not equivalent", configurationContainer.equivalent(configurationContainerAgain));

		ResourceSchema resourceSchemaAgain = RefinedResourceSchema.getResourceSchema(resourceAgain, prismContext);
		assertNotNull("No resource schema (again)", resourceSchemaAgain);
		assertTrue("Resource schema was not cached", resourceSchema == resourceSchemaAgain);

		// Now we stick our nose deep inside the provisioning impl. But we need
		// to make sure that the configured connector is properly refreshed
		// forceFresh = true
		ConnectorInstance configuredConnectorInstanceAgain = connectorManager.getConfiguredConnectorInstance(
				resourceAgain, true, result);
		assertNotNull("No configuredConnectorInstance (again)", configuredConnectorInstanceAgain);
		assertFalse("Connector instance was not refreshed", configuredConnectorInstance == configuredConnectorInstanceAgain);

		// Check if the connector still works
		OperationResult testResult = new OperationResult(TestOpenDJ.class.getName()
				+ ".test011ResourceAndConnectorCachingForceFresh.test");
		configuredConnectorInstanceAgain.test(testResult);
		testResult.computeStatus();
		assertSuccess("Connector test failed", testResult);
		
		assertConnectorInitializationCountIncrement(1);
		rememberConnectorInstance(configuredConnectorInstanceAgain);
		
		assertSteadyResource();
	}

	
	@Test
	public void test020ApplyDefinitionShadow() throws Exception {
		final String TEST_NAME = "test020ApplyDefinitionShadow";
		TestUtil.displayTestTile(TEST_NAME);

		// GIVEN
		OperationResult result = new OperationResult(TestOpenDJ.class.getName()
				+ "." + TEST_NAME);

		PrismObject<ShadowType> account = PrismTestUtil.parseObject(new File(ACCOUNT_WILL_FILENAME));

		// WHEN
		provisioningService.applyDefinition(account, result);

		// THEN
		result.computeStatus();
		display("applyDefinition result", result);
		assertSuccess(result);

		account.checkConsistence(true, true);
		ShadowUtil.checkConsistence(account, TEST_NAME);
		assertSuccess("applyDefinition(account) result", result);
		
		assertSteadyResource();
	}

	@Test
	public void test021ApplyDefinitionAddShadowDelta() throws Exception {
		final String TEST_NAME = "test021ApplyDefinitionAddShadowDelta";
		TestUtil.displayTestTile(TEST_NAME);

		// GIVEN
		OperationResult result = new OperationResult(TestOpenDJ.class.getName()
				+ "." + TEST_NAME);

		PrismObject<ShadowType> account = PrismTestUtil.parseObject(new File(ACCOUNT_WILL_FILENAME));

		ObjectDelta<ShadowType> delta = account.createAddDelta();

		// WHEN
		provisioningService.applyDefinition(delta, result);

		// THEN
		result.computeStatus();
		display("applyDefinition result", result);
		assertSuccess(result);

		delta.checkConsistence(true, true, true);
		assertSuccess("applyDefinition(add delta) result", result);
		
		assertSteadyResource();
	}
	
	@Test
	public void test022ApplyDefinitionResource() throws Exception {
		final String TEST_NAME = "test022ApplyDefinitionResource";
		TestUtil.displayTestTile(TEST_NAME);

		// GIVEN
		OperationResult result = new OperationResult(TestOpenDJ.class.getName()
				+ "." + TEST_NAME);

		PrismObject<ResourceType> resource = PrismTestUtil.parseObject(new File(RESOURCE_DUMMY_FILENAME));
		// Transplant connector OID. The freshly-parsed resource does have only the fake one.
		resource.asObjectable().getConnectorRef().setOid(this.resourceType.getConnectorRef().getOid());
		// Make sure this object has a different OID than the one already loaded. This avoids caching
		// and other side-effects
		resource.setOid(RESOURCE_DUMMY_NONEXISTENT_OID);

		// WHEN
		provisioningService.applyDefinition(resource, result);

		// THEN
		result.computeStatus();
		display("applyDefinition result", result);
		assertSuccess(result);

		resource.checkConsistence(true, true);
		assertSuccess("applyDefinition(resource) result", result);
		
		assertSteadyResource();
	}
	
	@Test
	public void test023ApplyDefinitionAddResourceDelta() throws Exception {
		final String TEST_NAME = "test023ApplyDefinitionAddResourceDelta";
		TestUtil.displayTestTile(TEST_NAME);

		// GIVEN
		OperationResult result = new OperationResult(TestOpenDJ.class.getName()
				+ "." + TEST_NAME);

		PrismObject<ResourceType> resource = PrismTestUtil.parseObject(new File(RESOURCE_DUMMY_FILENAME));
		// Transplant connector OID. The freshly-parsed resource does have only the fake one.
		resource.asObjectable().getConnectorRef().setOid(this.resourceType.getConnectorRef().getOid());
		ObjectDelta<ResourceType> delta = resource.createAddDelta();
		// Make sure this object has a different OID than the one already loaded. This avoids caching
		// and other side-effects
		resource.setOid(RESOURCE_DUMMY_NONEXISTENT_OID);

		// WHEN
		provisioningService.applyDefinition(delta, result);

		// THEN
		result.computeStatus();
		display("applyDefinition result", result);
		assertSuccess(result);

		delta.checkConsistence(true, true, true);
		assertSuccess("applyDefinition(add delta) result", result);
		
		assertSteadyResource();
	}

	// The account must exist to test this with modify delta. So we postpone the
	// test when the account actually exists

	@Test
	public void test100AddAccount() throws Exception {
		final String TEST_NAME = "test100AddAccount";
		TestUtil.displayTestTile(TEST_NAME);
		// GIVEN
		Task syncTask = taskManager.createTaskInstance(TestDummy.class.getName()
				+ "." + TEST_NAME);
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ "." + TEST_NAME);
		syncServiceMock.reset();

		PrismObject<ShadowType> account = prismContext.parseObject(new File(ACCOUNT_WILL_FILENAME));
		account.checkConsistence();

		display("Adding shadow", account);

		// WHEN
		String addedObjectOid = provisioningService.addObject(account, null, null, syncTask, result);

		// THEN
		result.computeStatus();
		display("add object result", result);
		assertSuccess("addObject has failed (result)", result);
		assertEquals(ACCOUNT_WILL_OID, addedObjectOid);

		account.checkConsistence();

		PrismObject<ShadowType> accountRepo = repositoryService.getObject(ShadowType.class, ACCOUNT_WILL_OID, result);
		display("Account repo", accountRepo);
		ShadowType accountTypeRepo = accountRepo.asObjectable();
		PrismAsserts.assertEqualsPolyString("Name not equal", ACCOUNT_WILL_USERNAME, accountTypeRepo.getName());
		assertEquals("Wrong kind (repo)", ShadowKindType.ACCOUNT, accountTypeRepo.getKind());
		assertAttribute(accountTypeRepo, ConnectorFactoryIcfImpl.ICFS_NAME, getWillRepoIcfUid());
		assertAttribute(accountTypeRepo, ConnectorFactoryIcfImpl.ICFS_UID, getWillRepoIcfUid());
		ActivationType activationRepo = accountTypeRepo.getActivation();
		assertNotNull("No activation in "+accountRepo+" (repo)", activationRepo);
		assertEquals("Wrong activation enableTimestamp in "+accountRepo+" (repo)", ACCOUNT_WILL_ENABLE_TIMESTAMP, activationRepo.getEnableTimestamp());
		
		syncServiceMock.assertNotifySuccessOnly();

		PrismObject<ShadowType> accountProvisioning = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, null, result);
		display("Account provisioning", accountProvisioning);
		ShadowType accountTypeProvisioning = accountProvisioning.asObjectable();
		display("account from provisioning", accountTypeProvisioning);
		PrismAsserts.assertEqualsPolyString("Name not equal", ACCOUNT_WILL_USERNAME, accountTypeProvisioning.getName());
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, accountTypeProvisioning.getKind());
		assertAttribute(accountTypeProvisioning, ConnectorFactoryIcfImpl.ICFS_NAME, getWillRepoIcfUid());
		assertAttribute(accountTypeProvisioning, ConnectorFactoryIcfImpl.ICFS_UID, getWillRepoIcfUid());
		ActivationType activationProvisioning = accountTypeProvisioning.getActivation();
		assertNotNull("No activation in "+accountProvisioning+" (provisioning)", activationProvisioning);
		assertEquals("Wrong activation administrativeStatus in "+accountProvisioning+" (provisioning)", ActivationStatusType.ENABLED, activationProvisioning.getAdministrativeStatus());
		IntegrationTestTools.assertEqualsTimestamp("Wrong activation enableTimestamp in "+accountProvisioning+" (provisioning)", ACCOUNT_WILL_ENABLE_TIMESTAMP, activationProvisioning.getEnableTimestamp());

		assertNull("The _PASSSWORD_ attribute sneaked into shadow", ShadowUtil.getAttributeValues(
				accountTypeProvisioning, new QName(ConnectorFactoryIcfImpl.NS_ICF_SCHEMA, "password")));

		// Check if the account was created in the dummy resource

		DummyAccount dummyAccount = dummyResource.getAccountByUsername(ACCOUNT_WILL_USERNAME);
		assertNotNull("No dummy account", dummyAccount);
		assertEquals("Fullname is wrong", "Will Turner", dummyAccount.getAttributeValue("fullname"));
		assertTrue("The account is not enabled", dummyAccount.isEnabled());
		assertEquals("Wrong password", "3lizab3th", dummyAccount.getPassword());

		// Check if the shadow is still in the repo (e.g. that the consistency or sync haven't removed it)
		PrismObject<ShadowType> shadowFromRepo = repositoryService.getObject(ShadowType.class,
				addedObjectOid, result);
		assertNotNull("Shadow was not created in the repository", shadowFromRepo);
		display("Repository shadow", shadowFromRepo.dump());

		ProvisioningTestUtil.checkRepoAccountShadow(shadowFromRepo);

		checkConsistency(accountProvisioning);
		assertSteadyResource();
	}

	@Test
	public void test101AddAccountWithoutName() throws Exception {
		TestUtil.displayTestTile("test101AddAccountWithoutName");
		// GIVEN
		Task syncTask = taskManager.createTaskInstance(TestDummy.class.getName()
				+ ".test101AddAccountWithoutName");
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ ".test101AddAccountWithoutName");
		syncServiceMock.reset();

		ShadowType account = parseObjectTypeFromFile(ACCOUNT_MORGAN_FILENAME, ShadowType.class);

		display("Adding shadow", account.asPrismObject());

		// WHEN
		String addedObjectOid = provisioningService.addObject(account.asPrismObject(), null, null, syncTask, result);

		// THEN
		result.computeStatus();
		display("add object result", result);
		assertSuccess("addObject has failed (result)", result);
		assertEquals(ACCOUNT_MORGAN_OID, addedObjectOid);

		ShadowType accountType = repositoryService
				.getObject(ShadowType.class, ACCOUNT_MORGAN_OID, result).asObjectable();
		PrismAsserts.assertEqualsPolyString("Account name was not generated (repository)", ACCOUNT_MORGAN_NAME, accountType.getName());
		
		syncServiceMock.assertNotifySuccessOnly();

		ShadowType provisioningAccountType = provisioningService.getObject(ShadowType.class,
				ACCOUNT_MORGAN_OID, null, result).asObjectable();
		display("account from provisioning", provisioningAccountType);
		PrismAsserts.assertEqualsPolyString("Account name was not generated (provisioning)", ACCOUNT_MORGAN_NAME,
				provisioningAccountType.getName());

		assertNull("The _PASSSWORD_ attribute sneaked into shadow", ShadowUtil.getAttributeValues(
				provisioningAccountType, new QName(ConnectorFactoryIcfImpl.NS_ICF_SCHEMA, "password")));

		// Check if the account was created in the dummy resource
		DummyAccount dummyAccount = dummyResource.getAccountByUsername(ACCOUNT_MORGAN_NAME);
		assertNotNull("No dummy account", dummyAccount);
		assertEquals("Fullname is wrong", "Captain Morgan", dummyAccount.getAttributeValue("fullname"));
		assertTrue("The account is not enabled", dummyAccount.isEnabled());
		assertEquals("Wrong password", "sh1verM3T1mb3rs", dummyAccount.getPassword());

		// Check if the shadow is in the repo
		PrismObject<ShadowType> shadowFromRepo = repositoryService.getObject(ShadowType.class,
				addedObjectOid, result);
		assertNotNull("Shadow was not created in the repository", shadowFromRepo);
		display("Repository shadow", shadowFromRepo.dump());

		ProvisioningTestUtil.checkRepoAccountShadow(shadowFromRepo);

		checkConsistency(account.asPrismObject());
		
		assertSteadyResource();
	}

	@Test
	public void test102GetAccount() throws Exception {
		final String TEST_NAME = "test102GetAccount";
		TestUtil.displayTestTile(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ "." + TEST_NAME);

		// WHEN
		ShadowType shadow = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null,
				result).asObjectable();

		// THEN
		result.computeStatus();
		display("getObject result", result);
		assertSuccess(result);

		display("Retrieved account shadow", shadow);

		assertNotNull("No dummy account", shadow);

		checkAccountWill(shadow, result);

		checkConsistency(shadow.asPrismObject());
		
		assertSteadyResource();
	}

	private void checkAccountWill(ShadowType shadow, OperationResult result) {
		checkAccountShadow(shadow, result);
		Collection<ResourceAttribute<?>> attributes = ShadowUtil.getAttributes(shadow);
		assertEquals("Unexpected number of attributes", 6, attributes.size());
		assertAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "Flying Dutchman");
		assertAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, "Sword", "LOVE");
		assertAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOOT_NAME, 42);
	}

	@Test
	public void test103GetAccountNoFetch() throws Exception {
		final String TEST_NAME="test103GetAccountNoFetch";
		TestUtil.displayTestTile(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ "."+TEST_NAME);

		GetOperationOptions options = new GetOperationOptions();
		options.setNoFetch(true);

		// WHEN
		ShadowType shadow = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, options,
				result).asObjectable();

		// THEN
		result.computeStatus();
		display("getObject result", result);
		assertSuccess(result);

		display("Retrieved account shadow", shadow);

		assertNotNull("No dummy account", shadow);

		checkAccountShadow(shadow, result, false);

		checkConsistency(shadow.asPrismObject());
		
		assertSteadyResource();
	}

	@Test
	public void test105ApplyDefinitionModifyDelta() throws Exception {
		TestUtil.displayTestTile("test105ApplyDefinitionModifyDelta");

		// GIVEN
		OperationResult result = new OperationResult(TestOpenDJ.class.getName()
				+ ".test105ApplyDefinitionModifyDelta");

		ObjectModificationType changeAddRoleCaptain = PrismTestUtil.unmarshalObject(new File(FILENAME_MODIFY_ACCOUNT),
				ObjectModificationType.class);
		ObjectDelta<ShadowType> accountDelta = DeltaConvertor.createObjectDelta(changeAddRoleCaptain,
				ShadowType.class, prismContext);

		// WHEN
		provisioningService.applyDefinition(accountDelta, result);

		// THEN
		result.computeStatus();
		display("applyDefinition result", result);
		assertSuccess(result);

		accountDelta.checkConsistence(true, true, true);
		assertSuccess("applyDefinition(modify delta) result", result);
		
		assertSteadyResource();
	}

	@Test
	public void test112SeachIterative() throws Exception {
		TestUtil.displayTestTile("test112SeachIterative");
		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ ".test112SeachIterative");

		// Make sure there is an account on resource that the provisioning has
		// never seen before, so there is no shadow
		// for it yet.
		DummyAccount newAccount = new DummyAccount("meathook");
		newAccount.addAttributeValues("fullname", "Meathook");
		newAccount.setEnabled(true);
		newAccount.setPassword("parrotMonster");
		dummyResource.addAccount(newAccount);

		ObjectQuery query = ObjectQueryUtil.createResourceAndAccountQuery(RESOURCE_DUMMY_OID, new QName(ResourceTypeUtil.getResourceNamespace(resourceType),
						ConnectorFactoryIcfImpl.ACCOUNT_OBJECT_CLASS_LOCAL_NAME), prismContext); 

		final List<PrismObject<ShadowType>> foundObjects = new ArrayList<PrismObject<ShadowType>>();
		ResultHandler<ShadowType> handler = new ResultHandler<ShadowType>() {

			@Override
			public boolean handle(PrismObject<ShadowType> object, OperationResult parentResult) {
				foundObjects.add(object);

				ObjectType objectType = object.asObjectable();
				assertTrue(objectType instanceof ShadowType);
				ShadowType shadow = (ShadowType) objectType;
				checkAccountShadow(shadow, parentResult);
				return true;
			}
		};

		// WHEN
		provisioningService.searchObjectsIterative(ShadowType.class, query, handler, result);

		// THEN
		result.computeStatus();
		display("searchObjectsIterative result", result);
		assertSuccess(result);

		assertEquals(4, foundObjects.size());
		checkConsistency(foundObjects);
		assertProtected(foundObjects, 1);

		// And again ...

		foundObjects.clear();

		// WHEN
		provisioningService.searchObjectsIterative(ShadowType.class, query, handler, result);

		// THEN

		assertEquals(4, foundObjects.size());

		checkConsistency(foundObjects);
		assertProtected(foundObjects, 1);
		
		assertSteadyResource();
	}

	private <T extends ShadowType> void assertProtected(List<PrismObject<T>> shadows, int expectedNumberOfProtectedShadows) {
		int actual = countProtected(shadows);
		assertEquals("Unexpected number of protected shadows", expectedNumberOfProtectedShadows, actual);
	}

	private <T extends ShadowType> int countProtected(List<PrismObject<T>> shadows) {
		int count = 0;
		for (PrismObject<T> shadow: shadows) {
			if (shadow.asObjectable().isProtectedObject() != null && shadow.asObjectable().isProtectedObject()) {
				count ++;
			}
		}
		return count;
	}

	@Test
	public void test113SearchAllShadowsInRepository() throws Exception {
		TestUtil.displayTestTile("test113SearchAllShadowsInRepository");
		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ ".test113SearchAllShadowsInRepository");
		ObjectQuery query = IntegrationTestTools.createAllShadowsQuery(resourceType, prismContext);
		display("All shadows query", query);

		// WHEN
		List<PrismObject<ShadowType>> allShadows = repositoryService.searchObjects(ShadowType.class,
				query, result);
		
		// THEN
		result.computeStatus();
		display("searchObjects result", result);
		assertSuccess(result);
		
		display("Found " + allShadows.size() + " shadows");

		assertFalse("No shadows found", allShadows.isEmpty());
		assertEquals("Wrong number of results", 4, allShadows.size());
		
		assertSteadyResource();
	}

	@Test
	public void test114SearchAllShadows() throws Exception {
		final String TEST_NAME = "test114SearchAllShadows";
		TestUtil.displayTestTile(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ "." + TEST_NAME);
		ObjectQuery query = IntegrationTestTools.createAllShadowsQuery(resourceType,
				SchemaTestConstants.ICF_ACCOUNT_OBJECT_CLASS_LOCAL_NAME, prismContext);
		display("All shadows query", query);

		// WHEN
		List<PrismObject<ShadowType>> allShadows = provisioningService.searchObjects(ShadowType.class,
				query, result);
		
		// THEN
		result.computeStatus();
		display("searchObjects result", result);
		assertSuccess(result);
		
		display("Found " + allShadows.size() + " shadows");

		assertFalse("No shadows found", allShadows.isEmpty());
		assertEquals("Wrong number of results", 4, allShadows.size());
		
		checkConsistency(allShadows);
		assertProtected(allShadows, 1);
		
		assertSteadyResource();
	}

	@Test
	public void test115countAllShadows() throws Exception {
		TestUtil.displayTestTile("test115countAllShadows");
		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ ".test115countAllShadows");
		ObjectQuery query = IntegrationTestTools.createAllShadowsQuery(resourceType,
				SchemaTestConstants.ICF_ACCOUNT_OBJECT_CLASS_LOCAL_NAME, prismContext);
		display("All shadows query", query);

		// WHEN
		int count = provisioningService.countObjects(ShadowType.class, query, result);
		
		// THEN
		result.computeStatus();
		display("countObjects result", result);
		assertSuccess(result);
		
		display("Found " + count + " shadows");

		assertEquals("Wrong number of results", 4, count);
		
		assertSteadyResource();
	}

	@Test
	public void test116SearchNullQueryResource() throws Exception {
		final String TEST_NAME = "test116SearchNullQueryResource";
		TestUtil.displayTestTile(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ "." + TEST_NAME);

		// WHEN
		List<PrismObject<ResourceType>> allResources = provisioningService.searchObjects(ResourceType.class,
				new ObjectQuery(), result);
		
		// THEN
		result.computeStatus();
		display("searchObjects result", result);
		assertSuccess(result);
		
		display("Found " + allResources.size() + " resources");

		assertFalse("No resources found", allResources.isEmpty());
		assertEquals("Wrong number of results", 1, allResources.size());
		
		assertSteadyResource();
	}

	@Test
	public void test117CountNullQueryResource() throws Exception {
		TestUtil.displayTestTile("test117CountNullQueryResource");
		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ ".test117CountNullQueryResource");

		// WHEN
		int count = provisioningService.countObjects(ResourceType.class, new ObjectQuery(), result);
		
		// THEN
		result.computeStatus();
		display("countObjects result", result);
		assertSuccess(result);
		
		display("Counted " + count + " resources");

		assertEquals("Wrong count", 1, count);
		
		assertSteadyResource();
	}

	



	@Test
	public void test123ModifyObjectReplace() throws Exception {
		final String TEST_NAME = "test123ModifyObjectReplace";
		TestUtil.displayTestTile(TEST_NAME);

		Task task = taskManager.createTaskInstance(TestDummy.class.getName()
				+ "." + TEST_NAME);
		OperationResult result = task.getResult();
		
		syncServiceMock.reset();

		ObjectDelta<ShadowType> delta = ObjectDelta.createModificationReplaceProperty(ShadowType.class, 
				ACCOUNT_WILL_OID, dummyResourceCtl.getAttributeFullnamePath(), prismContext, "Pirate Will Turner");
		display("ObjectDelta", delta);
		delta.checkConsistence();

		// WHEN
		provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
				new OperationProvisioningScriptsType(), null, task, result);

		// THEN
		result.computeStatus();
		display("modifyObject result", result);
		assertSuccess(result);
		
		delta.checkConsistence();
		assertDummyAccountAttributeValues(ACCOUNT_WILL_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Pirate Will Turner");
		
		syncServiceMock.assertNotifySuccessOnly();
		
		assertSteadyResource();
	}

	@Test
	public void test124ModifyObjectAddPirate() throws Exception {
		TestUtil.displayTestTile("test124ModifyObjectAddPirate");

		Task syncTask = taskManager.createTaskInstance(TestDummy.class.getName()
				+ ".test124ModifyObjectAddPirate");
		OperationResult result = new OperationResult(TestOpenDJ.class.getName()
				+ ".test124ModifyObjectAddPirate");
		syncServiceMock.reset();

		ObjectDelta<ShadowType> delta = ObjectDelta.createModificationAddProperty(ShadowType.class, 
				ACCOUNT_WILL_OID, 
				dummyResourceCtl.getAttributePath(DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME), 
				prismContext, "Pirate");
		display("ObjectDelta", delta);
		delta.checkConsistence();

		// WHEN
		provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
				new OperationProvisioningScriptsType(), null, syncTask, result);

		// THEN
		result.computeStatus();
		display("modifyObject result", result);
		assertSuccess(result);
		
		delta.checkConsistence();
		// check if attribute was changed
		assertDummyAccountAttributeValues(ACCOUNT_WILL_USERNAME, DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Pirate");
		
		syncServiceMock.assertNotifySuccessOnly();
		
		assertSteadyResource();
	}
	
	@Test
	public void test125ModifyObjectAddCaptain() throws Exception {
		TestUtil.displayTestTile("test125ModifyObjectAddCaptain");

		Task syncTask = taskManager.createTaskInstance(TestDummy.class.getName()
				+ ".test125ModifyObjectAddCaptain");
		OperationResult result = new OperationResult(TestOpenDJ.class.getName()
				+ ".test125ModifyObjectAddCaptain");
		syncServiceMock.reset();

		ObjectDelta<ShadowType> delta = ObjectDelta.createModificationAddProperty(ShadowType.class, 
				ACCOUNT_WILL_OID, 
				dummyResourceCtl.getAttributePath(DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME),
				prismContext, "Captain");
		display("ObjectDelta", delta);
		delta.checkConsistence();

		// WHEN
		provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
				new OperationProvisioningScriptsType(), null, syncTask, result);

		// THEN
		result.computeStatus();
		display("modifyObject result", result);
		assertSuccess(result);
		
		delta.checkConsistence();
		// check if attribute was changed
		assertDummyAccountAttributeValues(ACCOUNT_WILL_USERNAME, DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Pirate", "Captain");
		
		syncServiceMock.assertNotifySuccessOnly();
		
		assertSteadyResource();
	}

	@Test
	public void test126ModifyObjectDeletePirate() throws Exception {
		TestUtil.displayTestTile("test126ModifyObjectDeletePirate");

		Task syncTask = taskManager.createTaskInstance(TestDummy.class.getName()
				+ ".test126ModifyObjectDeletePirate");
		OperationResult result = new OperationResult(TestOpenDJ.class.getName()
				+ ".test126ModifyObjectDeletePirate");
		syncServiceMock.reset();

		ObjectDelta<ShadowType> delta = ObjectDelta.createModificationDeleteProperty(ShadowType.class, 
				ACCOUNT_WILL_OID, dummyResourceCtl.getAttributePath(DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME), prismContext, "Pirate");
		display("ObjectDelta", delta);
		delta.checkConsistence();

		// WHEN
		provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
				new OperationProvisioningScriptsType(), null, syncTask, result);

		// THEN
		result.computeStatus();
		display("modifyObject result", result);
		assertSuccess(result);
		
		delta.checkConsistence();
		// check if attribute was changed
		assertDummyAccountAttributeValues(ACCOUNT_WILL_USERNAME, DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Captain");
		
		syncServiceMock.assertNotifySuccessOnly();
		
		assertSteadyResource();
	}
	
	/**
	 * Try to add the same value that the account attribute already has. Resources that do not tolerate this will fail
	 * unless the mechanism to compensate for this works properly.
	 */
	@Test
	public void test127ModifyObjectAddCaptainAgain() throws Exception {
		final String TEST_NAME = "test127ModifyObjectAddCaptainAgain";
		TestUtil.displayTestTile(TEST_NAME);

		Task task = taskManager.createTaskInstance(TestDummy.class.getName()
				+ "." + TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();

		ObjectDelta<ShadowType> delta = ObjectDelta.createModificationAddProperty(ShadowType.class, 
				ACCOUNT_WILL_OID, dummyResourceCtl.getAttributePath(DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME), prismContext, "Captain");
		display("ObjectDelta", delta);
		delta.checkConsistence();

		// WHEN
		provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
				new OperationProvisioningScriptsType(), null, task, result);

		// THEN
		result.computeStatus();
		display("modifyObject result", result);
		assertSuccess(result);
		
		delta.checkConsistence();
		// check if attribute was changed
		assertDummyAccountAttributeValues(ACCOUNT_WILL_USERNAME, DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Captain");
		
		syncServiceMock.assertNotifySuccessOnly();
		
		assertSteadyResource();
	}
	
	/**
	 * Set a null value to the (native) dummy attribute. The UCF layer should filter that out.
	 */
	@Test
	public void test128NullAttributeValue() throws Exception {
		final String TEST_NAME = "test128NullAttributeValue";
		TestUtil.displayTestTile(TEST_NAME);

		Task task = taskManager.createTaskInstance(TestDummy.class.getName()
				+ "." + TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();

		DummyAccount willDummyAccount = dummyResource.getAccountByUsername(ACCOUNT_WILL_USERNAME);
		willDummyAccount.replaceAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, null);

		// WHEN
		PrismObject<ShadowType> accountWill = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, result);

		// THEN
		result.computeStatus();
		display("getObject result", result);
		assertSuccess(result);
		
		ResourceAttributeContainer attributesContainer = ShadowUtil.getAttributesContainer(accountWill);
		ResourceAttribute<Object> titleAttribute = attributesContainer.findAttribute(new QName(ResourceTypeUtil.getResourceNamespace(resourceType), DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME));
		assertNull("Title attribute sneaked in", titleAttribute);
		
		accountWill.checkConsistence();
		
		assertSteadyResource();
	}

	@Test
	public void test131AddScript() throws Exception {
		final String TEST_NAME = "test131AddScript";
		TestUtil.displayTestTile(TEST_NAME);
		// GIVEN
		Task task = taskManager.createTaskInstance(TestDummy.class.getName()
				+ "." + TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();
		dummyResource.purgeScriptHistory();

		ShadowType account = parseObjectTypeFromFile(FILENAME_ACCOUNT_SCRIPT, ShadowType.class);
		display("Account before add", account);

		OperationProvisioningScriptsType scriptsType = unmarshallJaxbFromFile(FILE_SCRIPTS, OperationProvisioningScriptsType.class);
		display("Provisioning scripts", PrismTestUtil.marshalWrap(scriptsType));

		// WHEN
		String addedObjectOid = provisioningService.addObject(account.asPrismObject(), scriptsType, null, task, result);

		// THEN
		result.computeStatus();
		display("add object result", result);
		assertSuccess("addObject has failed (result)", result);
		assertEquals(ACCOUNT_NEW_SCRIPT_OID, addedObjectOid);

		ShadowType accountType = repositoryService.getObject(ShadowType.class, ACCOUNT_NEW_SCRIPT_OID,
				result).asObjectable();
		PrismAsserts.assertEqualsPolyString("Wrong name", "william", accountType.getName());
		
		syncServiceMock.assertNotifySuccessOnly();

		ShadowType provisioningAccountType = provisioningService.getObject(ShadowType.class,
				ACCOUNT_NEW_SCRIPT_OID, null, result).asObjectable();
		PrismAsserts.assertEqualsPolyString("Wrong name", "william", provisioningAccountType.getName());

		// Check if the account was created in the dummy resource

		DummyAccount dummyAccount = dummyResource.getAccountByUsername("william");
		assertNotNull("No dummy account", dummyAccount);
		assertEquals("Fullname is wrong", "William Turner", dummyAccount.getAttributeValue("fullname"));
		assertTrue("The account is not enabled", dummyAccount.isEnabled());
		assertEquals("Wrong password", "3lizab3th123", dummyAccount.getPassword());
		
		ProvisioningScriptSpec beforeScript = new ProvisioningScriptSpec("In the beginning ...");
		beforeScript.addArgSingle("HOMEDIR", "jbond");
		ProvisioningScriptSpec afterScript = new ProvisioningScriptSpec("Hello World");
		afterScript.addArgSingle("which", "this");
		afterScript.addArgSingle("when", "now");
		IntegrationTestTools.assertScripts(dummyResource.getScriptHistory(), beforeScript, afterScript);
		
		assertSteadyResource();
	}

	// MID-1113
	@Test
	public void test132ModifyScript() throws Exception {
		final String TEST_NAME = "test132ModifyScript";
		TestUtil.displayTestTile(TEST_NAME);
		// GIVEN
		Task task = taskManager.createTaskInstance(TestDummy.class.getName()
				+ "." + TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();
		dummyResource.purgeScriptHistory();

		OperationProvisioningScriptsType scriptsType = unmarshallJaxbFromFile(FILE_SCRIPTS, OperationProvisioningScriptsType.class);
		display("Provisioning scripts", PrismTestUtil.marshalWrap(scriptsType));
		
		ObjectDelta<ShadowType> delta = ObjectDelta.createModificationReplaceProperty(ShadowType.class, 
				ACCOUNT_NEW_SCRIPT_OID, dummyResourceCtl.getAttributeFullnamePath(), prismContext, "Will Turner");
		display("ObjectDelta", delta);
		delta.checkConsistence();

		// WHEN
		provisioningService.modifyObject(ShadowType.class, ACCOUNT_NEW_SCRIPT_OID, delta.getModifications(), 
				scriptsType, null, task, result);

		// THEN
		result.computeStatus();
		display("modifyObject result", result);
		assertSuccess("modifyObject has failed (result)", result);
		syncServiceMock.assertNotifySuccessOnly();

		// Check if the account was modified in the dummy resource

		DummyAccount dummyAccount = dummyResource.getAccountByUsername("william");
		assertNotNull("No dummy account", dummyAccount);
		assertEquals("Fullname is wrong", "Will Turner", dummyAccount.getAttributeValue("fullname"));
		assertTrue("The account is not enabled", dummyAccount.isEnabled());
		assertEquals("Wrong password", "3lizab3th123", dummyAccount.getPassword());
		
		ProvisioningScriptSpec beforeScript = new ProvisioningScriptSpec("Where am I?");
		ProvisioningScriptSpec afterScript = new ProvisioningScriptSpec("Still here");
		afterScript.addArgMulti("status", "dead", "alive");
		IntegrationTestTools.assertScripts(dummyResource.getScriptHistory(), beforeScript, afterScript);
		
		assertSteadyResource();
	}
	
	/**
	 * This test modifies account shadow property that does NOT result in account modification
	 * on resource. The scripts must not be executed.
	 */
	@Test
	public void test133ModifyScriptNoExec() throws Exception {
		final String TEST_NAME = "test133ModifyScriptNoExec";
		TestUtil.displayTestTile(TEST_NAME);
		// GIVEN
		Task task = taskManager.createTaskInstance(TestDummy.class.getName()
				+ "." + TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();
		dummyResource.purgeScriptHistory();

		OperationProvisioningScriptsType scriptsType = unmarshallJaxbFromFile(FILE_SCRIPTS, OperationProvisioningScriptsType.class);
		display("Provisioning scripts", PrismTestUtil.marshalWrap(scriptsType));
		
		ObjectDelta<ShadowType> delta = ObjectDelta.createModificationReplaceProperty(ShadowType.class, 
				ACCOUNT_NEW_SCRIPT_OID, ShadowType.F_DESCRIPTION, prismContext, "Blah blah");
		display("ObjectDelta", delta);
		delta.checkConsistence();

		// WHEN
		provisioningService.modifyObject(ShadowType.class, ACCOUNT_NEW_SCRIPT_OID, delta.getModifications(), 
				scriptsType, null, task, result);

		// THEN
		result.computeStatus();
		display("modifyObject result", result);
		assertSuccess("modifyObject has failed (result)", result);
		syncServiceMock.assertNotifySuccessOnly();

		// Check if the account was modified in the dummy resource

		DummyAccount dummyAccount = dummyResource.getAccountByUsername("william");
		assertNotNull("No dummy account", dummyAccount);
		assertEquals("Fullname is wrong", "Will Turner", dummyAccount.getAttributeValue("fullname"));
		assertTrue("The account is not enabled", dummyAccount.isEnabled());
		assertEquals("Wrong password", "3lizab3th123", dummyAccount.getPassword());
		
		IntegrationTestTools.assertScripts(dummyResource.getScriptHistory());
		
		assertSteadyResource();
	}
	
	@Test
	public void test134DeleteScript() throws Exception {
		final String TEST_NAME = "test134DeleteScript";
		TestUtil.displayTestTile(TEST_NAME);
		// GIVEN
		Task task = taskManager.createTaskInstance(TestDummy.class.getName()
				+ "." + TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();
		dummyResource.purgeScriptHistory();

		OperationProvisioningScriptsType scriptsType = unmarshallJaxbFromFile(FILE_SCRIPTS, OperationProvisioningScriptsType.class);
		display("Provisioning scripts", PrismTestUtil.marshalWrap(scriptsType));
		
		// WHEN
		provisioningService.deleteObject(ShadowType.class, ACCOUNT_NEW_SCRIPT_OID, null, scriptsType,
				task, result);

		// THEN
		result.computeStatus();
		display("modifyObject result", result);
		assertSuccess("modifyObject has failed (result)", result);
		syncServiceMock.assertNotifySuccessOnly();

		// Check if the account was modified in the dummy resource

		DummyAccount dummyAccount = dummyResource.getAccountByUsername("william");
		assertNull("Dummy account not gone", dummyAccount);
		
		ProvisioningScriptSpec beforeScript = new ProvisioningScriptSpec("Goodbye World");
		beforeScript.addArgMulti("what", "cruel");
		ProvisioningScriptSpec afterScript = new ProvisioningScriptSpec("R.I.P.");
		IntegrationTestTools.assertScripts(dummyResource.getScriptHistory(), beforeScript, afterScript);
		
		assertSteadyResource();
	}
	
	@Test
	public void test135ExecuteScript() throws Exception {
		final String TEST_NAME = "test135ExecuteScript";
		TestUtil.displayTestTile(TEST_NAME);
		// GIVEN
		Task task = taskManager.createTaskInstance(TestDummy.class.getName()
				+ "." + TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();
		dummyResource.purgeScriptHistory();

		OperationProvisioningScriptsType scriptsType = unmarshallJaxbFromFile(FILE_SCRIPTS, OperationProvisioningScriptsType.class);
		display("Provisioning scripts", PrismTestUtil.marshalWrap(scriptsType));
		
		ProvisioningScriptType script = scriptsType.getScript().get(0);
		
		// WHEN
		provisioningService.executeScript(RESOURCE_DUMMY_OID, script, task, result);

		// THEN
		result.computeStatus();
		display("executeScript result", result);
		assertSuccess("executeScript has failed (result)", result);
		
		ProvisioningScriptSpec expectedScript = new ProvisioningScriptSpec("Where to go now?");
		expectedScript.addArgMulti("direction", "left", "right");
		IntegrationTestTools.assertScripts(dummyResource.getScriptHistory(), expectedScript);
		
		assertSteadyResource();
	}

	@Test
	public void test150DisableAccount() throws Exception {
		final String TEST_NAME = "test150DisableAccount";
		TestUtil.displayTestTile(TEST_NAME);
		// GIVEN

		Task task = taskManager.createTaskInstance(TestDummy.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		ShadowType accountType = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null,
				result).asObjectable();
		assertNotNull(accountType);

		display("Retrieved account shadow", accountType);

		DummyAccount dummyAccount = dummyResource.getAccountByUsername(ACCOUNT_WILL_USERNAME);
		assertTrue(dummyAccount.isEnabled());
		
		syncServiceMock.reset();

		ObjectDelta<ShadowType> delta = ObjectDelta.createModificationReplaceProperty(ShadowType.class,
				ACCOUNT_WILL_OID, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, prismContext,
				ActivationStatusType.DISABLED);
		display("ObjectDelta", delta);
		delta.checkConsistence();

		// WHEN
		provisioningService.modifyObject(ShadowType.class, delta.getOid(),
				delta.getModifications(), new OperationProvisioningScriptsType(), null, task, result);

		// THEN
		result.computeStatus();
		display("modifyObject result", result);
		assertSuccess(result);
		
		delta.checkConsistence();
		// check if activation was changed
		dummyAccount = dummyResource.getAccountByUsername(ACCOUNT_WILL_USERNAME);
		assertFalse("Dummy account "+ACCOUNT_WILL_USERNAME+" is enabled, expected disabled", dummyAccount.isEnabled());
		
		syncServiceMock.assertNotifySuccessOnly();
		
		assertSteadyResource();
	}
	
	@Test
	public void test152EnableAccount() throws Exception {
		final String TEST_NAME = "test152EnableAccount";
		TestUtil.displayTestTile(TEST_NAME);
		// GIVEN

		Task task = taskManager.createTaskInstance(TestDummy.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		ShadowType accountType = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null,
				result).asObjectable();
		assertNotNull(accountType);
		display("Retrieved account shadow", accountType);

		DummyAccount dummyAccount = dummyResource.getAccountByUsername(ACCOUNT_WILL_USERNAME);
		assertFalse("Account is not disabled", dummyAccount.isEnabled());
		
		syncServiceMock.reset();

		ObjectDelta<ShadowType> delta = ObjectDelta.createModificationReplaceProperty(ShadowType.class,
				ACCOUNT_WILL_OID, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, prismContext,
				ActivationStatusType.ENABLED);
		display("ObjectDelta", delta);
		delta.checkConsistence();

		// WHEN
		provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
				new OperationProvisioningScriptsType(), null, task, result);

		// THEN
		result.computeStatus();
		display("modifyObject result", result);
		assertSuccess(result);
		
		delta.checkConsistence();
		// check if activation was changed
		dummyAccount = dummyResource.getAccountByUsername(ACCOUNT_WILL_USERNAME);
		assertTrue("Dummy account "+ACCOUNT_WILL_USERNAME+" is disabled, expected enabled", dummyAccount.isEnabled());
		
		syncServiceMock.assertNotifySuccessOnly();
		
		assertSteadyResource();
	}
	
	@Test
	public void test155SetValidFrom() throws Exception {
		final String TEST_NAME = "test155SetValidFrom";
		TestUtil.displayTestTile(TEST_NAME);
		// GIVEN

		Task task = taskManager.createTaskInstance(TestDummy.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		ShadowType accountType = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null,
				result).asObjectable();
		assertNotNull(accountType);

		display("Retrieved account shadow", accountType);

		DummyAccount dummyAccount = dummyResource.getAccountByUsername(ACCOUNT_WILL_USERNAME);
		assertTrue(dummyAccount.isEnabled());
		
		syncServiceMock.reset();

		long millis = VALID_FROM_MILLIS;
		
		ObjectDelta<ShadowType> delta = ObjectDelta.createModificationReplaceProperty(ShadowType.class,
				ACCOUNT_WILL_OID, SchemaConstants.PATH_ACTIVATION_VALID_FROM, prismContext,
				XmlTypeConverter.createXMLGregorianCalendar(VALID_FROM_MILLIS));
		delta.checkConsistence();

		// WHEN
		provisioningService.modifyObject(ShadowType.class, delta.getOid(),
				delta.getModifications(), new OperationProvisioningScriptsType(), null, task, result);

		// THEN
		result.computeStatus();
		display("modifyObject result", result);
		assertSuccess(result);
		
		delta.checkConsistence();
		// check if activation was changed
		dummyAccount = dummyResource.getAccountByUsername(ACCOUNT_WILL_USERNAME);
		assertEquals("Wrong account validFrom in account "+ACCOUNT_WILL_USERNAME, new Date(VALID_FROM_MILLIS), dummyAccount.getValidFrom());
		assertTrue("Dummy account "+ACCOUNT_WILL_USERNAME+" is disabled, expected enabled", dummyAccount.isEnabled());
		
		syncServiceMock.assertNotifySuccessOnly();
		
		assertSteadyResource();
	}
	
	@Test
	public void test156SetValidTo() throws Exception {
		final String TEST_NAME = "test156SetValidTo";
		TestUtil.displayTestTile(TEST_NAME);
		// GIVEN

		Task task = taskManager.createTaskInstance(TestDummy.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		ShadowType accountType = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null,
				result).asObjectable();
		assertNotNull(accountType);

		display("Retrieved account shadow", accountType);

		DummyAccount dummyAccount = dummyResource.getAccountByUsername(ACCOUNT_WILL_USERNAME);
		assertTrue(dummyAccount.isEnabled());
		
		syncServiceMock.reset();

		long millis = VALID_TO_MILLIS;
		
		ObjectDelta<ShadowType> delta = ObjectDelta.createModificationReplaceProperty(ShadowType.class,
				ACCOUNT_WILL_OID, SchemaConstants.PATH_ACTIVATION_VALID_TO, prismContext,
				XmlTypeConverter.createXMLGregorianCalendar(VALID_TO_MILLIS));
		delta.checkConsistence();

		// WHEN
		provisioningService.modifyObject(ShadowType.class, delta.getOid(),
				delta.getModifications(), new OperationProvisioningScriptsType(), null, task, result);

		// THEN
		result.computeStatus();
		display("modifyObject result", result);
		assertSuccess(result);
		
		delta.checkConsistence();
		// check if activation was changed
		dummyAccount = dummyResource.getAccountByUsername(ACCOUNT_WILL_USERNAME);
		assertEquals("Wrong account validFrom in account "+ACCOUNT_WILL_USERNAME, new Date(VALID_FROM_MILLIS), dummyAccount.getValidFrom());
		assertEquals("Wrong account validTo in account "+ACCOUNT_WILL_USERNAME, new Date(VALID_TO_MILLIS), dummyAccount.getValidTo());
		assertTrue("Dummy account "+ACCOUNT_WILL_USERNAME+" is disabled, expected enabled", dummyAccount.isEnabled());
		
		syncServiceMock.assertNotifySuccessOnly();
		
		assertSteadyResource();
	}

	@Test
	public void test158GetAccount() throws Exception {
		final String TEST_NAME = "test158GetAccount";
		TestUtil.displayTestTile(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ "." + TEST_NAME);

		// WHEN
		PrismObject<ShadowType> shadow = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, result);
		ShadowType shadowType = shadow.asObjectable();

		// THEN
		result.computeStatus();
		display("getObject result", result);
		assertSuccess(result);

		display("Retrieved account shadow", shadowType);

		assertNotNull("No dummy account", shadowType);
		
		PrismAsserts.assertPropertyValue(shadow, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, 
				ActivationStatusType.ENABLED);

		checkAccountWill(shadowType, result);

		checkConsistency(shadowType.asPrismObject());
		
		assertSteadyResource();
	}
	
	@Test
	public void test200AddGroup() throws Exception {
		final String TEST_NAME = "test200AddGroup";
		TestUtil.displayTestTile(TEST_NAME);
		// GIVEN
		Task task = taskManager.createTaskInstance(TestDummy.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();

		PrismObject<ShadowType> group = prismContext.parseObject(new File(GROUP_PIRATES_FILENAME));
		group.checkConsistence();

		display("Adding group", group);

		// WHEN
		String addedObjectOid = provisioningService.addObject(group, null, null, task, result);

		// THEN
		result.computeStatus();
		display("add object result", result);
		assertSuccess("addObject has failed (result)", result);
		assertEquals(GROUP_PIRATES_OID, addedObjectOid);

		group.checkConsistence();

		ShadowType groupRepoType = repositoryService.getObject(ShadowType.class, GROUP_PIRATES_OID, result)
				.asObjectable();
		PrismAsserts.assertEqualsPolyString("Name not equal.", GROUP_PIRATES_NAME, groupRepoType.getName());
		assertEquals("Wrong kind (repo)", ShadowKindType.ENTITLEMENT, groupRepoType.getKind());
		
		syncServiceMock.assertNotifySuccessOnly();

		ShadowType groupProvisioningType = provisioningService.getObject(ShadowType.class,
				GROUP_PIRATES_OID, null, result).asObjectable();
		display("group from provisioning", groupProvisioningType);
		checkGroupPirates(groupProvisioningType, result);

		// Check if the group was created in the dummy resource

		DummyGroup dummyAccount = dummyResource.getGroupByName(GROUP_PIRATES_NAME);
		assertNotNull("No dummy group "+GROUP_PIRATES_NAME, dummyAccount);
		assertEquals("Description is wrong", "Scurvy pirates", dummyAccount.getAttributeValue("description"));
		assertTrue("The group is not enabled", dummyAccount.isEnabled());

		// Check if the shadow is still in the repo (e.g. that the consistency or sync haven't removed it)
		PrismObject<ShadowType> shadowFromRepo = repositoryService.getObject(ShadowType.class,
				addedObjectOid, result);
		assertNotNull("Shadow was not created in the repository", shadowFromRepo);
		display("Repository shadow", shadowFromRepo.dump());

		ProvisioningTestUtil.checkRepoEntitlementShadow(shadowFromRepo);

		checkConsistency(group);
		
		assertSteadyResource();
	}

	@Test
	public void test202GetGroup() throws Exception {
		final String TEST_NAME = "test200AddGroup";
		TestUtil.displayTestTile(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ "." + TEST_NAME);

		// WHEN
		ShadowType shadow = provisioningService.getObject(ShadowType.class, GROUP_PIRATES_OID, null,
				result).asObjectable();

		// THEN
		result.computeStatus();
		display("getObject result", result);
		assertSuccess(result);

		display("Retrieved group shadow", shadow);

		assertNotNull("No dummy group", shadow);

		checkGroupPirates(shadow, result);

		checkConsistency(shadow.asPrismObject());
		
		assertSteadyResource();
	}

	private void checkGroupPirates(ShadowType shadow, OperationResult result) {
		checkGroupShadow(shadow, result);
		PrismAsserts.assertEqualsPolyString("Name not equal.", GROUP_PIRATES_NAME, shadow.getName());
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ENTITLEMENT, shadow.getKind());
		assertAttribute(shadow, DummyResourceContoller.DUMMY_GROUP_ATTRIBUTE_DESCRIPTION, "Scurvy pirates");
		Collection<ResourceAttribute<?>> attributes = ShadowUtil.getAttributes(shadow);
		assertEquals("Unexpected number of attributes", 3, attributes.size());
		
		assertNull("The _PASSSWORD_ attribute sneaked into shadow", ShadowUtil.getAttributeValues(
				shadow, new QName(ConnectorFactoryIcfImpl.NS_ICF_SCHEMA, "password")));
	}

	@Test
	public void test203GetGroupNoFetch() throws Exception {
		final String TEST_NAME="test203GetGroupNoFetch";
		TestUtil.displayTestTile(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ "."+TEST_NAME);

		GetOperationOptions options = new GetOperationOptions();
		options.setNoFetch(true);

		// WHEN
		ShadowType shadow = provisioningService.getObject(ShadowType.class, GROUP_PIRATES_OID, options,
				result).asObjectable();

		// THEN
		result.computeStatus();
		display("getObject result", result);
		assertSuccess(result);

		display("Retrieved group shadow", shadow);

		assertNotNull("No dummy group", shadow);

		checkGroupShadow(shadow, result, false);

		checkConsistency(shadow.asPrismObject());
		
		assertSteadyResource();
	}
	
	@Test
	public void test205ModifyGroupReplace() throws Exception {
		final String TEST_NAME = "test205ModifyGroupReplace";
		TestUtil.displayTestTile(TEST_NAME);

		Task task = taskManager.createTaskInstance(TestDummy.class.getName()
				+ "." + TEST_NAME);
		OperationResult result = task.getResult();
		
		syncServiceMock.reset();

		ObjectDelta<ShadowType> delta = ObjectDelta.createModificationReplaceProperty(ShadowType.class, 
				GROUP_PIRATES_OID, 
				dummyResourceCtl.getAttributePath(DummyResourceContoller.DUMMY_GROUP_ATTRIBUTE_DESCRIPTION),
				prismContext, "Bloodthirsty pirates");
		display("ObjectDelta", delta);
		delta.checkConsistence();

		// WHEN
		provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
				new OperationProvisioningScriptsType(), null, task, result);

		// THEN
		result.computeStatus();
		display("modifyObject result", result);
		assertSuccess(result);
		
		delta.checkConsistence();
		DummyGroup group = dummyResource.getGroupByName(GROUP_PIRATES_NAME);
		assertDummyAttributeValues(group, DummyResourceContoller.DUMMY_GROUP_ATTRIBUTE_DESCRIPTION, "Bloodthirsty pirates");
		
		syncServiceMock.assertNotifySuccessOnly();
		assertSteadyResource();
	}
	
	@Test
	public void test210AddPrivilege() throws Exception {
		final String TEST_NAME = "test210AddPrivilege";
		TestUtil.displayTestTile(TEST_NAME);
		// GIVEN
		Task task = taskManager.createTaskInstance(TestDummy.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();

		PrismObject<ShadowType> priv = prismContext.parseObject(new File(PRIVILEGE_PILLAGE_FILENAME));
		priv.checkConsistence();

		display("Adding priv", priv);

		// WHEN
		String addedObjectOid = provisioningService.addObject(priv, null, null, task, result);

		// THEN
		result.computeStatus();
		display("add object result", result);
		assertSuccess("addObject has failed (result)", result);
		assertEquals(PRIVILEGE_PILLAGE_OID, addedObjectOid);

		priv.checkConsistence();

		ShadowType groupRepoType = repositoryService.getObject(ShadowType.class, PRIVILEGE_PILLAGE_OID, result)
				.asObjectable();
		PrismAsserts.assertEqualsPolyString("Name not equal.", PRIVILEGE_PILLAGE_NAME, groupRepoType.getName());
		assertEquals("Wrong kind (repo)", ShadowKindType.ENTITLEMENT, groupRepoType.getKind());
		
		syncServiceMock.assertNotifySuccessOnly();

		ShadowType privProvisioningType = provisioningService.getObject(ShadowType.class,
				PRIVILEGE_PILLAGE_OID, null, result).asObjectable();
		display("priv from provisioning", privProvisioningType);
		checkPrivPillage(privProvisioningType, result);

		// Check if the group was created in the dummy resource

		DummyPrivilege dummyPriv = dummyResource.getPrivilegeByName(PRIVILEGE_PILLAGE_NAME);
		assertNotNull("No dummy priv "+PRIVILEGE_PILLAGE_NAME, dummyPriv);

		// Check if the shadow is still in the repo (e.g. that the consistency or sync haven't removed it)
		PrismObject<ShadowType> shadowFromRepo = repositoryService.getObject(ShadowType.class,
				addedObjectOid, result);
		assertNotNull("Shadow was not created in the repository", shadowFromRepo);
		display("Repository shadow", shadowFromRepo.dump());

		ProvisioningTestUtil.checkRepoEntitlementShadow(shadowFromRepo);

		checkConsistency(priv);
		assertSteadyResource();
	}
	
	@Test
	public void test212GetPriv() throws Exception {
		final String TEST_NAME = "test212GetPriv";
		TestUtil.displayTestTile(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ "." + TEST_NAME);

		// WHEN
		ShadowType shadow = provisioningService.getObject(ShadowType.class, PRIVILEGE_PILLAGE_OID, null,
				result).asObjectable();

		// THEN
		result.computeStatus();
		display("getObject result", result);
		assertSuccess(result);

		display("Retrieved priv shadow", shadow);

		assertNotNull("No dummy priv", shadow);

		checkPrivPillage(shadow, result);

		checkConsistency(shadow.asPrismObject());
		
		assertSteadyResource();
	}
	
	private void checkPrivPillage(ShadowType shadow, OperationResult result) {
		checkEntitlementShadow(shadow, result, OBJECTCLAS_PRIVILEGE_LOCAL_NAME, true);
		PrismAsserts.assertEqualsPolyString("Name not equal.", PRIVILEGE_PILLAGE_NAME, shadow.getName());
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ENTITLEMENT, shadow.getKind());
		Collection<ResourceAttribute<?>> attributes = ShadowUtil.getAttributes(shadow);
		assertEquals("Unexpected number of attributes", 2, attributes.size());
		
		assertNull("The _PASSSWORD_ attribute sneaked into shadow", ShadowUtil.getAttributeValues(
				shadow, new QName(ConnectorFactoryIcfImpl.NS_ICF_SCHEMA, "password")));
	}
	
	@Test
	public void test220EntitleAccountWillPirates() throws Exception {
		final String TEST_NAME = "test220EntitleAccountWillPirates";
		TestUtil.displayTestTile(TEST_NAME);

		Task task = taskManager.createTaskInstance(TestDummy.class.getName()
				+ "." + TEST_NAME);
		OperationResult result = task.getResult();
		
		syncServiceMock.reset();

		ObjectDelta<ShadowType> delta = ProvisioningTestUtil.createEntitleDelta(ACCOUNT_WILL_OID, 
				dummyResourceCtl.getAttributeQName(DummyResourceContoller.DUMMY_ENTITLEMENT_GROUP_NAME),
				GROUP_PIRATES_OID, prismContext);
		display("ObjectDelta", delta);
		delta.checkConsistence();

		// WHEN
		provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
				new OperationProvisioningScriptsType(), null, task, result);

		// THEN
		result.computeStatus();
		display("modifyObject result", result);
		assertSuccess(result);
		
		delta.checkConsistence();
		DummyGroup group = dummyResource.getGroupByName(GROUP_PIRATES_NAME);
		assertMember(group, getWillRepoIcfUid());
		
		syncServiceMock.assertNotifySuccessOnly();
		
		assertSteadyResource();
	}
	
	/**
	 * Reads the will accounts, checks that the entitlement is there.
	 */
	@Test
	public void test221GetPirateWill() throws Exception {
		final String TEST_NAME = "test221GetPirateWill";
		TestUtil.displayTestTile(TEST_NAME);

		Task task = taskManager.createTaskInstance(TestDummy.class.getName()
				+ "." + TEST_NAME);
		OperationResult result = task.getResult();
		
		syncServiceMock.reset();

		// WHEN
		PrismObject<ShadowType> account = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, result);

		// THEN
		result.computeStatus();
		display("Account", account);
		
		display(result);
		assertSuccess(result);
		
		assertEntitlement(account, GROUP_PIRATES_OID);
		
		// Just make sure nothing has changed
		DummyGroup group = dummyResource.getGroupByName(GROUP_PIRATES_NAME);
		assertMember(group, getWillRepoIcfUid());
		
		assertSteadyResource();
	}
	
	@Test
	public void test223EntitleAccountWillPillage() throws Exception {
		final String TEST_NAME = "test223EntitleAccountWillPillage";
		TestUtil.displayTestTile(TEST_NAME);

		Task task = taskManager.createTaskInstance(TestDummy.class.getName()
				+ "." + TEST_NAME);
		OperationResult result = task.getResult();
		
		syncServiceMock.reset();

		ObjectDelta<ShadowType> delta = ProvisioningTestUtil.createEntitleDelta(ACCOUNT_WILL_OID, 
				dummyResourceCtl.getAttributeQName(DummyResourceContoller.DUMMY_ENTITLEMENT_PRIVILEGE_NAME),
				PRIVILEGE_PILLAGE_OID, prismContext);
		display("ObjectDelta", delta);
		delta.checkConsistence();

		// WHEN
		provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
				new OperationProvisioningScriptsType(), null, task, result);

		// THEN
		result.computeStatus();
		display("modifyObject result", result);
		assertSuccess(result);
		
		DummyAccount dummyAccount = dummyResource.getAccountByUsername(ACCOUNT_WILL_USERNAME);
		assertNotNull("Account will is gone!", dummyAccount);
		Set<String> accountProvileges = dummyAccount.getAttributeValues(DummyAccount.ATTR_PRIVILEGES_NAME, String.class);
		PrismAsserts.assertSets("account privileges", accountProvileges, PRIVILEGE_PILLAGE_NAME);
		
		// Make sure that privilege object is still there
		DummyPrivilege priv = dummyResource.getPrivilegeByName(PRIVILEGE_PILLAGE_NAME);
		assertNotNull("Privilege object is gone!", priv);
		
		delta.checkConsistence();
		
		// Make sure that the groups is still there and will is a member
		DummyGroup group = dummyResource.getGroupByName(GROUP_PIRATES_NAME);
		assertMember(group, getWillRepoIcfUid());
		
		syncServiceMock.assertNotifySuccessOnly();
		
		assertSteadyResource();
	}
	
	/**
	 * Reads the will accounts, checks that both entitlements are there.
	 */
	@Test
	public void test224GetPillagingPirateWill() throws Exception {
		final String TEST_NAME = "test224GetPillagingPirateWill";
		TestUtil.displayTestTile(TEST_NAME);

		Task task = taskManager.createTaskInstance(TestDummy.class.getName()
				+ "." + TEST_NAME);
		OperationResult result = task.getResult();
		
		syncServiceMock.reset();

		// WHEN
		PrismObject<ShadowType> account = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, result);

		// THEN
		result.computeStatus();
		display("Account", account);
		
		display(result);
		assertSuccess(result);
		
		assertEntitlement(account, GROUP_PIRATES_OID);
		assertEntitlement(account, PRIVILEGE_PILLAGE_OID);
		
		// Just make sure nothing has changed
		DummyAccount dummyAccount = dummyResource.getAccountByUsername(ACCOUNT_WILL_USERNAME);
		assertNotNull("Account will is gone!", dummyAccount);
		Set<String> accountProvileges = dummyAccount.getAttributeValues(DummyAccount.ATTR_PRIVILEGES_NAME, String.class);
		PrismAsserts.assertSets("Wrong account privileges", accountProvileges, PRIVILEGE_PILLAGE_NAME);
		
		// Make sure that privilege object is still there
		DummyPrivilege priv = dummyResource.getPrivilegeByName(PRIVILEGE_PILLAGE_NAME);
		assertNotNull("Privilege object is gone!", priv);
		
		DummyGroup group = dummyResource.getGroupByName(GROUP_PIRATES_NAME);
		assertMember(group, getWillRepoIcfUid());
		
		assertSteadyResource();
	}
		
	@Test
	public void test225DetitleAccountWillPirates() throws Exception {
		final String TEST_NAME = "test225DetitleAccountWillPirates";
		TestUtil.displayTestTile(TEST_NAME);

		Task task = taskManager.createTaskInstance(TestDummy.class.getName()
				+ "." + TEST_NAME);
		OperationResult result = task.getResult();
		
		syncServiceMock.reset();

		ObjectDelta<ShadowType> delta = ProvisioningTestUtil.createDetitleDelta(ACCOUNT_WILL_OID,
				dummyResourceCtl.getAttributeQName(DummyResourceContoller.DUMMY_ENTITLEMENT_GROUP_NAME),
				GROUP_PIRATES_OID, prismContext);
		display("ObjectDelta", delta);
		delta.checkConsistence();

		// WHEN
		provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
				new OperationProvisioningScriptsType(), null, task, result);

		// THEN
		result.computeStatus();
		display("modifyObject result", result);
		assertSuccess(result);
		
		delta.checkConsistence();
		DummyGroup group = dummyResource.getGroupByName(GROUP_PIRATES_NAME);
		assertNoMember(group, getWillRepoIcfUid());
		
		// Make sure that account is still there and it has the privilege
		DummyAccount dummyAccount = dummyResource.getAccountByUsername(ACCOUNT_WILL_USERNAME);
		assertNotNull("Account will is gone!", dummyAccount);
		Set<String> accountProvileges = dummyAccount.getAttributeValues(DummyAccount.ATTR_PRIVILEGES_NAME, String.class);
		PrismAsserts.assertSets("Wrong account privileges", accountProvileges, PRIVILEGE_PILLAGE_NAME);
		
		// Make sure that privilege object is still there
		DummyPrivilege priv = dummyResource.getPrivilegeByName(PRIVILEGE_PILLAGE_NAME);
		assertNotNull("Privilege object is gone!", priv);
		
		syncServiceMock.assertNotifySuccessOnly();
		assertSteadyResource();
	}
	
	@Test
	public void test228DetitleAccountWillPillage() throws Exception {
		final String TEST_NAME = "test228DetitleAccountWillPillage";
		TestUtil.displayTestTile(TEST_NAME);

		Task task = taskManager.createTaskInstance(TestDummy.class.getName()
				+ "." + TEST_NAME);
		OperationResult result = task.getResult();
		
		syncServiceMock.reset();

		ObjectDelta<ShadowType> delta = ProvisioningTestUtil.createDetitleDelta(ACCOUNT_WILL_OID, 
				dummyResourceCtl.getAttributeQName(DummyResourceContoller.DUMMY_ENTITLEMENT_PRIVILEGE_NAME),
				PRIVILEGE_PILLAGE_OID, prismContext);
		display("ObjectDelta", delta);
		delta.checkConsistence();

		// WHEN
		provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
				new OperationProvisioningScriptsType(), null, task, result);

		// THEN
		result.computeStatus();
		display("modifyObject result", result);
		assertSuccess(result);
		
		delta.checkConsistence();
		DummyGroup group = dummyResource.getGroupByName(GROUP_PIRATES_NAME);
		assertNoMember(group, getWillRepoIcfUid());
		
		// Make sure that account is still there and it has the privilege
		DummyAccount dummyAccount = dummyResource.getAccountByUsername(ACCOUNT_WILL_USERNAME);
		assertNotNull("Account will is gone!", dummyAccount);
		Set<String> accountProvileges = dummyAccount.getAttributeValues(DummyAccount.ATTR_PRIVILEGES_NAME, String.class);
		assertTrue("Unexpected account privileges: "+accountProvileges, accountProvileges == null || accountProvileges.isEmpty());
		
		// Make sure that privilege object is still there
		DummyPrivilege priv = dummyResource.getPrivilegeByName(PRIVILEGE_PILLAGE_NAME);
		assertNotNull("Privilege object is gone!", priv);
		
		syncServiceMock.assertNotifySuccessOnly();
		assertSteadyResource();
	}
	
	/**
	 * LeChuck has both group and priv entitlement. Let's add him together with these entitlements.
	 */
	@Test
	public void test230AddAccountLeChuck() throws Exception {
		final String TEST_NAME = "test230AddAccountLeChuck";
		TestUtil.displayTestTile(TEST_NAME);
		// GIVEN
		Task task = taskManager.createTaskInstance(TestDummy.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();

		PrismObject<ShadowType> account = prismContext.parseObject(new File(ACCOUNT_LECHUCK_FILENAME));
		account.checkConsistence();

		display("Adding shadow", account);

		// WHEN
		String addedObjectOid = provisioningService.addObject(account, null, null, task, result);

		// THEN
		result.computeStatus();
		display("add object result", result);
		assertSuccess("addObject has failed (result)", result);
		assertEquals(ACCOUNT_LECHUCK_OID, addedObjectOid);

		account.checkConsistence();
		
		// Check if the account was created in the dummy resource and that it has the entitlements

		DummyAccount dummyAccount = dummyResource.getAccountByUsername(ACCOUNT_LECHUCK_NAME);
		assertNotNull("No dummy account", dummyAccount);
		assertEquals("Fullname is wrong", "LeChuck", dummyAccount.getAttributeValue(DummyAccount.ATTR_FULLNAME_NAME));
		assertTrue("The account is not enabled", dummyAccount.isEnabled());
		assertEquals("Wrong password", "und3ad", dummyAccount.getPassword());

		Set<String> accountProvileges = dummyAccount.getAttributeValues(DummyAccount.ATTR_PRIVILEGES_NAME, String.class);
		PrismAsserts.assertSets("account privileges", accountProvileges, PRIVILEGE_PILLAGE_NAME);
		
		// Make sure that privilege object is still there
		DummyPrivilege priv = dummyResource.getPrivilegeByName(PRIVILEGE_PILLAGE_NAME);
		assertNotNull("Privilege object is gone!", priv);
		
		DummyGroup group = dummyResource.getGroupByName(GROUP_PIRATES_NAME);
		assertMember(group, ACCOUNT_LECHUCK_NAME);

		ShadowType accountType = repositoryService.getObject(ShadowType.class, ACCOUNT_LECHUCK_OID, result)
				.asObjectable();
		PrismAsserts.assertEqualsPolyString("Name not equal", ACCOUNT_LECHUCK_NAME, accountType.getName());
		assertEquals("Wrong kind (repo)", ShadowKindType.ACCOUNT, accountType.getKind());
		assertAttribute(accountType, ConnectorFactoryIcfImpl.ICFS_NAME, ACCOUNT_LECHUCK_NAME);
		assertAttribute(accountType, ConnectorFactoryIcfImpl.ICFS_UID, ACCOUNT_LECHUCK_NAME);
		
		syncServiceMock.assertNotifySuccessOnly();

		PrismObject<ShadowType> provisioningAccount = provisioningService.getObject(ShadowType.class,
				ACCOUNT_LECHUCK_OID, null, result);
		ShadowType provisioningAccountType = provisioningAccount.asObjectable();
		display("account from provisioning", provisioningAccountType);
		PrismAsserts.assertEqualsPolyString("Name not equal", ACCOUNT_LECHUCK_NAME, provisioningAccountType.getName());
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, provisioningAccountType.getKind());
		assertAttribute(provisioningAccountType, ConnectorFactoryIcfImpl.ICFS_NAME, ACCOUNT_LECHUCK_NAME);
		assertAttribute(provisioningAccountType, ConnectorFactoryIcfImpl.ICFS_UID, ACCOUNT_LECHUCK_NAME);
		
		assertEntitlement(account, GROUP_PIRATES_OID);
		assertEntitlement(account, PRIVILEGE_PILLAGE_OID);

		assertNull("The _PASSSWORD_ attribute sneaked into shadow", ShadowUtil.getAttributeValues(
				provisioningAccountType, new QName(ConnectorFactoryIcfImpl.NS_ICF_SCHEMA, "password")));

		checkConsistency(account);
		
		assertSteadyResource();
	}
	
	/**
	 * LeChuck has both group and priv entitlement. If deleted it should be correctly removed from all
	 * the entitlements.
	 */
	@Test
	public void test235DeleteAccountLeChuck() throws Exception {
		final String TEST_NAME = "test235DeleteAccountLeChuck";
		TestUtil.displayTestTile(TEST_NAME);
		// GIVEN
		Task task = taskManager.createTaskInstance(TestDummy.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();

		// WHEN
		provisioningService.deleteObject(ShadowType.class, ACCOUNT_LECHUCK_OID, null, null, task, result);

		// THEN
		result.computeStatus();
		display("add object result", result);
		assertSuccess("addObject has failed (result)", result);
		syncServiceMock.assertNotifySuccessOnly();
		
		// Check if the account is gone and that group membership is gone as well

		DummyAccount dummyAccount = dummyResource.getAccountByUsername(ACCOUNT_LECHUCK_NAME);
		assertNull("Dummy account is NOT gone", dummyAccount);
		
		// Make sure that privilege object is still there
		DummyPrivilege priv = dummyResource.getPrivilegeByName(PRIVILEGE_PILLAGE_NAME);
		assertNotNull("Privilege object is gone!", priv);
		
		DummyGroup group = dummyResource.getGroupByName(GROUP_PIRATES_NAME);
		assertNoMember(group, ACCOUNT_LECHUCK_NAME);

		try {
			repositoryService.getObject(ShadowType.class, ACCOUNT_LECHUCK_OID, result);
			
			AssertJUnit.fail("Shadow (repo) is not gone");
		} catch (ObjectNotFoundException e) {
			// This is expected
		}
		
		try {
			provisioningService.getObject(ShadowType.class, ACCOUNT_LECHUCK_OID, null, result);
			
			AssertJUnit.fail("Shadow (provisioning) is not gone");
		} catch (ObjectNotFoundException e) {
			// This is expected
		}
		
		assertSteadyResource();
	}
	
	@Test
	public void test238DeletePrivPillage() throws Exception {
		final String TEST_NAME = "test238DeletePrivPillage";
		TestUtil.displayTestTile(TEST_NAME);

		Task task = taskManager.createTaskInstance(TestDummy.class.getName()
				+ "." + TEST_NAME);
		OperationResult result = task.getResult();
		
		syncServiceMock.reset();

		// WHEN
		provisioningService.deleteObject(ShadowType.class, PRIVILEGE_PILLAGE_OID, null, null, task, result);

		// THEN
		result.computeStatus();
		display(result);
		assertSuccess(result);
		
		syncServiceMock.assertNotifySuccessOnly();
		
		try {
			repositoryService.getObject(ShadowType.class, PRIVILEGE_PILLAGE_OID, result);
			AssertJUnit.fail("Priv shadow is not gone (repo)");
		} catch (ObjectNotFoundException e) {
			// This is expected
		}
		
		try {
			provisioningService.getObject(ShadowType.class, PRIVILEGE_PILLAGE_OID, null, result);
			AssertJUnit.fail("Priv shadow is not gone (provisioning)");
		} catch (ObjectNotFoundException e) {
			// This is expected
		}

		DummyPrivilege priv = dummyResource.getPrivilegeByName(PRIVILEGE_PILLAGE_NAME);
		assertNull("Privilege object NOT is gone", priv);
		
		assertSteadyResource();
	}
	
	@Test
	public void test239DeleteGroupPirates() throws Exception {
		final String TEST_NAME = "test239DeleteGroupPirates";
		TestUtil.displayTestTile(TEST_NAME);

		Task task = taskManager.createTaskInstance(TestDummy.class.getName()
				+ "." + TEST_NAME);
		OperationResult result = task.getResult();
		
		syncServiceMock.reset();

		// WHEN
		provisioningService.deleteObject(ShadowType.class, GROUP_PIRATES_OID, null, null, task, result);

		// THEN
		result.computeStatus();
		display(result);
		assertSuccess(result);
		
		syncServiceMock.assertNotifySuccessOnly();
		
		try {
			repositoryService.getObject(ShadowType.class, GROUP_PIRATES_OID, result);
			AssertJUnit.fail("Group shadow is not gone (repo)");
		} catch (ObjectNotFoundException e) {
			// This is expected
		}
		
		try {
			provisioningService.getObject(ShadowType.class, GROUP_PIRATES_OID, null, result);
			AssertJUnit.fail("Group shadow is not gone (provisioning)");
		} catch (ObjectNotFoundException e) {
			// This is expected
		}

		DummyGroup dummyAccount = dummyResource.getGroupByName(GROUP_PIRATES_NAME);
		assertNull("Dummy group '"+GROUP_PIRATES_NAME+"' is not gone from dummy resource", dummyAccount);
		
		assertSteadyResource();
	}
	
	@Test
	public void test300AccountRename() throws Exception {
		final String TEST_NAME = "test300AccountRename";
		TestUtil.displayTestTile(TEST_NAME);

		Task task = taskManager.createTaskInstance(TestDummy.class.getName()
				+ "." + TEST_NAME);
		OperationResult result = task.getResult();
		
		syncServiceMock.reset();

		ObjectDelta<ShadowType> delta = ObjectDelta.createModificationReplaceProperty(ShadowType.class, 
				ACCOUNT_MORGAN_OID, SchemaTestConstants.ICFS_NAME_PATH, prismContext, "cptmorgan");
		provisioningService.applyDefinition(delta, result);
		display("ObjectDelta", delta);
		delta.checkConsistence();
		
		// WHEN
		provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
				new OperationProvisioningScriptsType(), null, task, result);

		// THEN
		result.computeStatus();
		display("modifyObject result", result);
		assertSuccess(result);
		
		delta.checkConsistence();
		assertDummyAccountAttributeValues("cptmorgan", DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Captain Morgan");
		
		PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, ACCOUNT_MORGAN_OID, result);
		assertShadowRepo(repoShadow, ACCOUNT_MORGAN_OID, "cptmorgan", resourceType);
		PrismAsserts.assertPropertyValue(repoShadow, SchemaTestConstants.ICFS_UID_PATH, "cptmorgan");
		
		syncServiceMock.assertNotifySuccessOnly();
		
		assertSteadyResource();
	}

	@Test
	public void test500AddProtectedAccount() throws ObjectNotFoundException, CommunicationException, SchemaException,
			ConfigurationException, SecurityViolationException, ObjectAlreadyExistsException {
		TestUtil.displayTestTile("test500AddProtectedAccount");
		// GIVEN
		Task syncTask = taskManager.createTaskInstance(TestDummy.class.getName()
				+ ".test500AddProtectedAccount");
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ ".test500AddProtectedAccount");
		syncServiceMock.reset();

		ResourceSchema resourceSchema = RefinedResourceSchema.getResourceSchema(resource, prismContext);
		ObjectClassComplexTypeDefinition defaultAccountDefinition = resourceSchema.findDefaultObjectClassDefinition(ShadowKindType.ACCOUNT);
		ShadowType shadowType = new ShadowType();
		PrismTestUtil.getPrismContext().adopt(shadowType);
		shadowType.setName(PrismTestUtil.createPolyStringType(ACCOUNT_DAVIEJONES_USERNAME));
		ObjectReferenceType resourceRef = new ObjectReferenceType();
		resourceRef.setOid(resource.getOid());
		shadowType.setResourceRef(resourceRef);
		shadowType.setObjectClass(defaultAccountDefinition.getTypeName());
		PrismObject<ShadowType> shadow = shadowType.asPrismObject();
		PrismContainer<Containerable> attrsCont = shadow.findOrCreateContainer(ShadowType.F_ATTRIBUTES);
		PrismProperty<String> icfsNameProp = attrsCont.findOrCreateProperty(ConnectorFactoryIcfImpl.ICFS_NAME);
		icfsNameProp.setRealValue(ACCOUNT_DAVIEJONES_USERNAME);

		// WHEN
		try {
			provisioningService.addObject(shadow, null, null, syncTask, result);
			AssertJUnit.fail("Expected security exception while adding 'daviejones' account");
		} catch (SecurityViolationException e) {
			// This is expected
			display("Expected exception", e);
		}
		
		result.computeStatus();
		display("addObject result (expected failure)", result);
		assertFailure(result);
		
		syncServiceMock.assertNotifyFailureOnly();

//		checkConsistency();
		
		assertSteadyResource();
	}

	@Test
	public void test501GetProtectedAccountShadow() throws ObjectNotFoundException, CommunicationException,
			SchemaException, ConfigurationException, SecurityViolationException {
		TestUtil.displayTestTile("test501GetProtectedAccount");
		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ ".test501GetProtectedAccount");

		// WHEN
		PrismObject<ShadowType> account = provisioningService.getObject(ShadowType.class, ACCOUNT_DAEMON_OID, null, result);

		assertEquals(""+account+" is not protected", Boolean.TRUE, account.asObjectable().isProtectedObject());
		checkConsistency(account);
		
		result.computeStatus();
		display("getObject result", result);
		assertSuccess(result);
		
		assertSteadyResource();
	}

	@Test
	public void test502ModifyProtectedAccountShadow() throws Exception {
		TestUtil.displayTestTile("test502ModifyProtectedAccountShadow");
		// GIVEN
		Task syncTask = taskManager.createTaskInstance(TestDummy.class.getName()
				+ ".test502ModifyProtectedAccountShadow");
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ ".test502ModifyProtectedAccountShadow");
		syncServiceMock.reset();

		Collection<? extends ItemDelta> modifications = new ArrayList<ItemDelta>(1);
		ResourceSchema resourceSchema = RefinedResourceSchema.getResourceSchema(resource, prismContext);
		ObjectClassComplexTypeDefinition defaultAccountDefinition = resourceSchema.findDefaultObjectClassDefinition(ShadowKindType.ACCOUNT);
		ResourceAttributeDefinition fullnameAttrDef = defaultAccountDefinition.findAttributeDefinition("fullname");
		ResourceAttribute fullnameAttr = fullnameAttrDef.instantiate();
		PropertyDelta fullnameDelta = fullnameAttr.createDelta(new ItemPath(ShadowType.F_ATTRIBUTES,
				fullnameAttrDef.getName()));
		fullnameDelta.setValueToReplace(new PrismPropertyValue<String>("Good Daemon"));
		((Collection) modifications).add(fullnameDelta);

		// WHEN
		try {
			provisioningService.modifyObject(ShadowType.class, ACCOUNT_DAEMON_OID, modifications, null, null, syncTask, result);
			AssertJUnit.fail("Expected security exception while modifying 'daemon' account");
		} catch (SecurityViolationException e) {
			// This is expected
			display("Expected exception", e);
		}
		
		result.computeStatus();
		display("modifyObject result (expected failure)", result);
		assertFailure(result);
		
		syncServiceMock.assertNotifyFailureOnly();

//		checkConsistency();
		
		assertSteadyResource();
	}

	@Test
	public void test503DeleteProtectedAccountShadow() throws ObjectNotFoundException, CommunicationException,
			SchemaException, ConfigurationException, SecurityViolationException {
		TestUtil.displayTestTile("test503DeleteProtectedAccountShadow");
		// GIVEN
		Task syncTask = taskManager.createTaskInstance(TestDummy.class.getName()
				+ ".test503DeleteProtectedAccountShadow");
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ ".test503DeleteProtectedAccountShadow");
		syncServiceMock.reset();

		// WHEN
		try {
			provisioningService.deleteObject(ShadowType.class, ACCOUNT_DAEMON_OID, null, null, syncTask, result);
			AssertJUnit.fail("Expected security exception while deleting 'daemon' account");
		} catch (SecurityViolationException e) {
			// This is expected
			display("Expected exception", e);
		}
		
		result.computeStatus();
		display("deleteObject result (expected failure)", result);
		assertFailure(result);
		
		syncServiceMock.assertNotifyFailureOnly();

//		checkConsistency();
		
		assertSteadyResource();
	}

	static Task syncTokenTask = null;
	
	@Test
	public void test800LiveSyncInit() throws ObjectNotFoundException, CommunicationException, SchemaException,
			com.evolveum.icf.dummy.resource.ObjectAlreadyExistsException, ConfigurationException,
			SecurityViolationException {
		TestUtil.displayTestTile("test800LiveSyncInit");
		syncTokenTask = taskManager.createTaskInstance(TestDummy.class.getName() + ".syncTask");

		dummyResource.setSyncStyle(DummySyncStyle.DUMB);
		syncServiceMock.reset();

		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ ".test800LiveSyncInit");

		// Dry run to remember the current sync token in the task instance.
		// Otherwise a last sync token whould be used and
		// no change would be detected
		provisioningService.synchronize(RESOURCE_DUMMY_OID, ProvisioningTestUtil.getDefaultAccountObjectClass(resourceType), 
				syncTokenTask, result);

		// THEN
		result.computeStatus();
		display("modifyObject result", result);
		assertSuccess(result);

		// No change, no fun
		syncServiceMock.assertNoNotifyChange();

		checkAllShadows();
		
		assertSteadyResource();
	}

	@Test
	public void test801LiveSyncAddBlackbeard() throws Exception {
		TestUtil.displayTestTile("test801LiveSyncAddBlackbeard");
		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ ".test801LiveSyncAddBlackbeard");

		syncServiceMock.reset();
		dummyResource.setSyncStyle(DummySyncStyle.DUMB);
		DummyAccount newAccount = new DummyAccount(BLACKBEARD_USERNAME);
		newAccount.addAttributeValues("fullname", "Edward Teach");
		newAccount.setEnabled(true);
		newAccount.setPassword("shiverMEtimbers");
		dummyResource.addAccount(newAccount);

		display("Resource before sync", dummyResource.dump());

		// WHEN
		provisioningService.synchronize(RESOURCE_DUMMY_OID, ProvisioningTestUtil.getDefaultAccountObjectClass(resourceType), 
				syncTokenTask, result);

		// THEN
		result.computeStatus();
		display("Synchronization result", result);
		assertSuccess("Synchronization result is not OK", result);

		syncServiceMock.assertNotifyChange();

		ResourceObjectShadowChangeDescription lastChange = syncServiceMock.getLastChange();
		display("The change", lastChange);

		PrismObject<? extends ShadowType> oldShadow = lastChange.getOldShadow();
		assertNotNull("Old shadow missing", oldShadow);
		assertNotNull("Old shadow does not have an OID", oldShadow.getOid());
		
		assertNull("Delta present when not expecting it", lastChange.getObjectDelta());
		ShadowType currentShadowType = lastChange.getCurrentShadow().asObjectable();
		assertNotNull("Current shadow missing", lastChange.getCurrentShadow());
		assertTrue("Wrong type of current shadow: " + currentShadowType.getClass().getName(),
				currentShadowType instanceof ShadowType);

		ResourceAttributeContainer attributesContainer = ShadowUtil
				.getAttributesContainer(currentShadowType);
		assertNotNull("No attributes container in current shadow", attributesContainer);
		Collection<ResourceAttribute<?>> attributes = attributesContainer.getAttributes();
		assertFalse("Attributes container is empty", attributes.isEmpty());
		assertEquals("Unexpected number of attributes", 3, attributes.size());
		ResourceAttribute<?> fullnameAttribute = attributesContainer.findAttribute(new QName(ResourceTypeUtil
				.getResourceNamespace(resourceType), "fullname"));
		assertNotNull("No fullname attribute in current shadow", fullnameAttribute);
		assertEquals("Wrong value of fullname attribute in current shadow", "Edward Teach",
				fullnameAttribute.getRealValue());

		checkAllShadows();
		
		assertSteadyResource();
	}

	@Test
	public void test802LiveSyncModifyBlackbeard() throws Exception {
		TestUtil.displayTestTile("test802LiveSyncModifyBlackbeard");
		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ ".test802LiveSyncModifyBlackbeard");

		syncServiceMock.reset();

		DummyAccount dummyAccount = dummyResource.getAccountByUsername(BLACKBEARD_USERNAME);
		dummyAccount.replaceAttributeValue("fullname", "Captain Blackbeard");

		display("Resource before sync", dummyResource.dump());

		// WHEN
		provisioningService.synchronize(RESOURCE_DUMMY_OID, ProvisioningTestUtil.getDefaultAccountObjectClass(resourceType), 
				syncTokenTask, result);

		// THEN
		result.computeStatus();
		display("Synchronization result", result);
		assertSuccess("Synchronization result is not OK", result);

		syncServiceMock.assertNotifyChange();

		ResourceObjectShadowChangeDescription lastChange = syncServiceMock.getLastChange();
		display("The change", lastChange);

		PrismObject<? extends ShadowType> oldShadow = lastChange.getOldShadow();
		assertNotNull("Old shadow missing", oldShadow);
		assertNotNull("Old shadow does not have an OID", oldShadow.getOid());
		PrismAsserts.assertClass("old shadow", ShadowType.class, oldShadow);
		ShadowType oldShadowType = oldShadow.asObjectable();
		ResourceAttributeContainer attributesContainer = ShadowUtil
				.getAttributesContainer(oldShadowType);
		assertNotNull("No attributes container in old shadow", attributesContainer);
		Collection<ResourceAttribute<?>> attributes = attributesContainer.getAttributes();
		assertFalse("Attributes container is empty", attributes.isEmpty());
		assertEquals("Unexpected number of attributes", 2, attributes.size());
		ResourceAttribute<?> icfsNameAttribute = attributesContainer.findAttribute(ConnectorFactoryIcfImpl.ICFS_NAME);
		assertNotNull("No ICF name attribute in old  shadow", icfsNameAttribute);
		assertEquals("Wrong value of ICF name attribute in old  shadow", BLACKBEARD_USERNAME,
				icfsNameAttribute.getRealValue());
		
		assertNull("Delta present when not expecting it", lastChange.getObjectDelta());
		ShadowType currentShadowType = lastChange.getCurrentShadow().asObjectable();
		assertNotNull("Current shadow missing", lastChange.getCurrentShadow());
		assertTrue("Wrong type of current shadow: " + currentShadowType.getClass().getName(),
				currentShadowType instanceof ShadowType);

		attributesContainer = ShadowUtil
				.getAttributesContainer(currentShadowType);
		assertNotNull("No attributes container in current shadow", attributesContainer);
		attributes = attributesContainer.getAttributes();
		assertFalse("Attributes container is empty", attributes.isEmpty());
		assertEquals("Unexpected number of attributes", 3, attributes.size());
		ResourceAttribute<?> fullnameAttribute = attributesContainer.findAttribute(new QName(ResourceTypeUtil
				.getResourceNamespace(resourceType), "fullname"));
		assertNotNull("No fullname attribute in current shadow", fullnameAttribute);
		assertEquals("Wrong value of fullname attribute in current shadow", "Captain Blackbeard",
				fullnameAttribute.getRealValue());

		PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, currentShadowType.getOid(), result);
		// TODO: check the shadow

		
		checkAllShadows();
		
		assertSteadyResource();
	}

	@Test
	public void test803LiveSyncAddDrake() throws Exception {
		final String TEST_NAME = "test803LiveSyncAddDrake";
		TestUtil.displayTestTile(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ "." + TEST_NAME);


		syncServiceMock.reset();
		dummyResource.setSyncStyle(DummySyncStyle.DUMB);
		DummyAccount newAccount = new DummyAccount(DRAKE_USERNAME);
		newAccount.addAttributeValues("fullname", "Sir Francis Drake");
		newAccount.setEnabled(true);
		newAccount.setPassword("avast!");
		dummyResource.addAccount(newAccount);

		display("Resource before sync", dummyResource.dump());

		// WHEN
		provisioningService.synchronize(RESOURCE_DUMMY_OID, ProvisioningTestUtil.getDefaultAccountObjectClass(resourceType), 
				syncTokenTask, result);

		// THEN
		result.computeStatus();
		display("Synchronization result", result);
		assertSuccess("Synchronization result is not OK", result);

		syncServiceMock.assertNotifyChange();

		ResourceObjectShadowChangeDescription lastChange = syncServiceMock.getLastChange();
		display("The change", lastChange);

		PrismObject<? extends ShadowType> oldShadow = lastChange.getOldShadow();
		assertNotNull("Old shadow missing", oldShadow);
		assertNotNull("Old shadow does not have an OID", oldShadow.getOid());
		
		assertNull("Delta present when not expecting it", lastChange.getObjectDelta());
		
		ShadowType currentShadowType = lastChange.getCurrentShadow().asObjectable();
		assertNotNull("Current shadow missing", lastChange.getCurrentShadow());
		PrismAsserts.assertClass("current shadow", ShadowType.class, currentShadowType);

		ResourceAttributeContainer attributesContainer = ShadowUtil
				.getAttributesContainer(currentShadowType);
		assertNotNull("No attributes container in current shadow", attributesContainer);
		Collection<ResourceAttribute<?>> attributes = attributesContainer.getAttributes();
		assertFalse("Attributes container is empty", attributes.isEmpty());
		assertEquals("Unexpected number of attributes", 3, attributes.size());
		ResourceAttribute<?> fullnameAttribute = attributesContainer.findAttribute(new QName(ResourceTypeUtil
				.getResourceNamespace(resourceType), "fullname"));
		assertNotNull("No fullname attribute in current shadow", fullnameAttribute);
		assertEquals("Wrong value of fullname attribute in current shadow", "Sir Francis Drake",
				fullnameAttribute.getRealValue());
		
		drakeAccountOid = currentShadowType.getOid();
		PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, drakeAccountOid, result);
		// TODO: check the shadow

		checkAllShadows();
		
		assertSteadyResource();
	}
	
	@Test
	public void test809LiveSyncDeleteDrake() throws Exception {
		final String TEST_NAME = "test809LiveSyncDeleteDrake";
		TestUtil.displayTestTile(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ "." + TEST_NAME);

		syncServiceMock.reset();
		dummyResource.setSyncStyle(DummySyncStyle.DUMB);
		dummyResource.deleteAccount(DRAKE_USERNAME);

		display("Resource before sync", dummyResource.dump());

		// WHEN
		provisioningService.synchronize(RESOURCE_DUMMY_OID, ProvisioningTestUtil.getDefaultAccountObjectClass(resourceType), 
				syncTokenTask, result);

		// THEN
		result.computeStatus();
		display("Synchronization result", result);
		assertSuccess("Synchronization result is not OK", result);

		syncServiceMock.assertNotifyChange();

		ResourceObjectShadowChangeDescription lastChange = syncServiceMock.getLastChange();
		display("The change", lastChange);

		PrismObject<? extends ShadowType> oldShadow = lastChange.getOldShadow();
		assertNotNull("Old shadow missing", oldShadow);
		assertNotNull("Old shadow does not have an OID", oldShadow.getOid());
		PrismAsserts.assertClass("old shadow", ShadowType.class, oldShadow);
		ShadowType oldShadowType = oldShadow.asObjectable();
		ResourceAttributeContainer attributesContainer = ShadowUtil
				.getAttributesContainer(oldShadowType);
		assertNotNull("No attributes container in old shadow", attributesContainer);
		Collection<ResourceAttribute<?>> attributes = attributesContainer.getAttributes();
		assertFalse("Attributes container is empty", attributes.isEmpty());
		assertEquals("Unexpected number of attributes", 2, attributes.size());
		ResourceAttribute<?> icfsNameAttribute = attributesContainer.findAttribute(ConnectorFactoryIcfImpl.ICFS_NAME);
		assertNotNull("No ICF name attribute in old  shadow", icfsNameAttribute);
		assertEquals("Wrong value of ICF name attribute in old  shadow", DRAKE_USERNAME,
				icfsNameAttribute.getRealValue());
		
		ObjectDelta<? extends ShadowType> objectDelta = lastChange.getObjectDelta();
		assertNotNull("Delta missing", objectDelta);
		assertEquals("Wrong delta changetype", ChangeType.DELETE, objectDelta.getChangeType());
		PrismAsserts.assertClass("delta", ShadowType.class, objectDelta);
		assertNotNull("No OID in delta", objectDelta.getOid());
		
		assertNull("Unexpected current shadow",lastChange.getCurrentShadow());
		
		try {
			// The shadow should be gone
			PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, drakeAccountOid, result);
			
			AssertJUnit.fail("The shadow "+repoShadow+" is not gone from repo");
		} catch (ObjectNotFoundException e) {
			// This is expected
		}
		
		checkAllShadows();
		
		assertSteadyResource();
	}

	@Test
	public void test810LiveSyncModifyProtectedAccount() throws Exception {
		TestUtil.displayTestTile("test810LiveSyncModifyProtectedAccount");
		// GIVEN
		Task syncTask = taskManager.createTaskInstance(TestDummy.class.getName()
				+ ".test810LiveSyncModifyProtectedAccount");
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ ".test810LiveSyncModifyProtectedAccount");

		syncServiceMock.reset();

		DummyAccount dummyAccount = dummyResource.getAccountByUsername(ACCOUNT_DAEMON_USERNAME);
		dummyAccount.replaceAttributeValue("fullname", "Maxwell deamon");

		// WHEN
		provisioningService.synchronize(RESOURCE_DUMMY_OID, ProvisioningTestUtil.getDefaultAccountObjectClass(resourceType), 
				syncTask, result);

		// THEN
		result.computeStatus();
		display("Synchronization result", result);
		assertSuccess("Synchronization result is not OK", result);

		ResourceObjectShadowChangeDescription lastChange = syncServiceMock.getLastChange();
		display("The change", lastChange);

		syncServiceMock.assertNoNotifyChange();

		checkAllShadows();
		
		assertSteadyResource();
	}

	@Test
	public void test901FailResourceNotFound() throws FileNotFoundException, JAXBException,
			ObjectAlreadyExistsException, SchemaException, CommunicationException, ObjectNotFoundException,
			ConfigurationException, SecurityViolationException {
		TestUtil.displayTestTile("test901FailResourceNotFound");
		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ ".test901FailResourceNotFound");

		// WHEN
		try {
			PrismObject<ResourceType> object = provisioningService.getObject(ResourceType.class, NOT_PRESENT_OID, null,
					result);
			AssertJUnit.fail("Expected ObjectNotFoundException to be thrown, but getObject returned " + object
					+ " instead");
		} catch (ObjectNotFoundException e) {
			// This is expected
		}
		
		result.computeStatus();
		display("getObject result (expected failure)", result);
		assertFailure(result);
		
		assertSteadyResource();
	}
	
	private void checkAccountShadow(ShadowType shadow, OperationResult parentResult) {
		checkAccountShadow(shadow, parentResult, true);
	}

	private void checkAccountShadow(ShadowType shadowType, OperationResult parentResult, boolean fullShadow) {
		ObjectChecker<ShadowType> checker = createShadowChecker(fullShadow);
		ShadowUtil.checkConsistence(shadowType.asPrismObject(), parentResult.getOperation());
		IntegrationTestTools.checkAccountShadow(shadowType, resourceType, repositoryService, checker, prismContext, parentResult);
	}

	private void checkGroupShadow(ShadowType shadow, OperationResult parentResult) {
		checkEntitlementShadow(shadow, parentResult, SchemaTestConstants.ICF_GROUP_OBJECT_CLASS_LOCAL_NAME, true);
	}
	
	private void checkGroupShadow(ShadowType shadow, OperationResult parentResult, boolean fullShadow) {
		checkEntitlementShadow(shadow, parentResult, SchemaTestConstants.ICF_GROUP_OBJECT_CLASS_LOCAL_NAME, fullShadow);
	}

	private void checkEntitlementShadow(ShadowType shadowType, OperationResult parentResult, String objectClassLocalName, boolean fullShadow) {
		ObjectChecker<ShadowType> checker = createShadowChecker(fullShadow);
		ShadowUtil.checkConsistence(shadowType.asPrismObject(), parentResult.getOperation());
		IntegrationTestTools.checkEntitlementShadow(shadowType, resourceType, repositoryService, checker, objectClassLocalName, prismContext, parentResult);
	}

	private void checkAllShadows() throws SchemaException, ObjectNotFoundException, CommunicationException,
			ConfigurationException {
		ObjectChecker<ShadowType> checker = null;
		IntegrationTestTools.checkAllShadows(resourceType, repositoryService, checker, prismContext);
	}

	private ObjectChecker<ShadowType> createShadowChecker(final boolean fullShadow) {
		return new ObjectChecker<ShadowType>() {
			@Override
			public void check(ShadowType shadow) {
				String icfName = ShadowUtil.getSingleStringAttributeValue(shadow,
						SchemaTestConstants.ICFS_NAME);
				assertNotNull("No ICF NAME", icfName);
				assertEquals("Wrong shadow name ("+shadow.getName()+")", StringUtils.lowerCase(icfName), StringUtils.lowerCase(shadow.getName().getOrig()));
				assertNotNull("No kind in "+shadow, shadow.getKind());

				if (shadow.getKind() == ShadowKindType.ACCOUNT) {
					if (fullShadow) {
						assertNotNull(
								"Missing fullname attribute",
								ShadowUtil.getSingleStringAttributeValue(shadow,
										new QName(ResourceTypeUtil.getResourceNamespace(resourceType), "fullname")));
						assertNotNull("no activation", shadow.getActivation());
						assertNotNull("no activation status", shadow.getActivation().getAdministrativeStatus());
						assertEquals("not enabled", ActivationStatusType.ENABLED, shadow.getActivation().getAdministrativeStatus());
					}
	
					assertProvisioningAccountShadow(shadow.asPrismObject(), resourceType, RefinedAttributeDefinition.class);
				}
			}

		};
	}

}
