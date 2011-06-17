/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.common.test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

import java.io.File;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.evolveum.midpoint.common.diff.CalculateXmlDiff;
import com.evolveum.midpoint.common.patch.PatchXml;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;

/**
 *
 * @author Igor Farinic
 */
public class DiffPatchTest {

    public DiffPatchTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testDiffPatchAccount() throws Exception {
        ObjectModificationType changes = CalculateXmlDiff.calculateChanges(new File("src/test/resources/account-old.xml"), new File("src/test/resources/account-new.xml"));
        assertNotNull(changes);
        assertEquals(5, changes.getPropertyModification().size());
        assertEquals("12345", changes.getOid());

        String patchedXml = (new PatchXml()).applyDifferences(changes, new File("src/test/resources/account-old.xml"));
        XmlAsserts.assertPatch(new File("src/test/resources/account-new.xml"), patchedXml);
    }

    @Test
    public void testDiffPatchAccountWithResourceSchemaHandlingConfiguration() throws Exception {
        ObjectModificationType changes = CalculateXmlDiff.calculateChanges(new File("src/test/resources/account-full-old.xml"), new File("src/test/resources/account-full-new.xml"));
        assertNotNull(changes);
        assertEquals(5, changes.getPropertyModification().size());
        assertEquals("12345", changes.getOid());

        String patchedXml = (new PatchXml()).applyDifferences(changes, new File("src/test/resources/account-full-old.xml"));
        XmlAsserts.assertPatch(new File("src/test/resources/account-full-new.xml"), patchedXml);
    }

    @Test
    public void testDiffPatchUser() throws Exception {
        ObjectModificationType changes = CalculateXmlDiff.calculateChanges(new File("src/test/resources/user-old.xml"), new File("src/test/resources/user-new.xml"));
        assertNotNull(changes);
        assertEquals(6, changes.getPropertyModification().size());
        assertEquals("007", changes.getOid());

        String patchedXml = (new PatchXml()).applyDifferences(changes, new File("src/test/resources/user-old.xml"));
        XmlAsserts.assertPatch(new File("src/test/resources/user-new.xml"), patchedXml);
    }

    @Test
    public void testDiffPatchUserExtension() throws Exception {
        ObjectModificationType changes = CalculateXmlDiff.calculateChanges(new File("src/test/resources/user-extension-old.xml"), new File("src/test/resources/user-extension-new.xml"));
        assertNotNull(changes);
        assertEquals(1, changes.getPropertyModification().size());
        assertEquals("007", changes.getOid());

        String patchedXml = (new PatchXml()).applyDifferences(changes, new File("src/test/resources/user-extension-old.xml"));
        XmlAsserts.assertPatch(new File("src/test/resources/user-extension-new.xml"), patchedXml);
    }

    @Test
    public void testDiffPatchResource() throws Exception {
        ObjectModificationType changes = CalculateXmlDiff.calculateChanges(new File("src/test/resources/resource-old.xml"), new File("src/test/resources/resource-new.xml"));
        assertNotNull(changes);
        assertEquals(1, changes.getPropertyModification().size());
        assertEquals("ef2bc95b-76e0-48e2-86d6-3d4f02d3e1a2", changes.getOid());

        String patchedXml = (new PatchXml()).applyDifferences(changes, new File("src/test/resources/resource-old.xml"));
        XmlAsserts.assertPatch(new File("src/test/resources/resource-new.xml"), patchedXml);
    }

    @Test
    public void testDiffPatchAdvancedResource() throws Exception {
        ObjectModificationType changes = CalculateXmlDiff.calculateChanges(new File("src/test/resources/resource-advanced-old.xml"), new File("src/test/resources/resource-advanced-new.xml"));
        assertNotNull(changes);
        assertEquals(1, changes.getPropertyModification().size());
        assertEquals("ef2bc95b-76e0-48e2-86d6-3d4f02d3e1a2", changes.getOid());

        String patchedXml = (new PatchXml()).applyDifferences(changes, new File("src/test/resources/resource-advanced-old.xml"));
        XmlAsserts.assertPatch(new File("src/test/resources/resource-advanced-new.xml"), patchedXml);
    }

    @Test
    public void testDiffPatchResourceSchemaHandling() throws Exception {
        ObjectModificationType changes = CalculateXmlDiff.calculateChanges(new File("src/test/resources/resource-schemahandling-old.xml"), new File("src/test/resources/resource-schemahandling-new.xml"));
        assertNotNull(changes);
        assertEquals(1, changes.getPropertyModification().size());
        assertEquals("ef2bc95b-76e0-48e2-86d6-3d4f02d3e1a2", changes.getOid());

        String patchedXml = (new PatchXml()).applyDifferences(changes, new File("src/test/resources/resource-schemahandling-old.xml"));
        XmlAsserts.assertPatch(new File("src/test/resources/resource-schemahandling-new.xml"), patchedXml);
    }

    @Test
    public void testDiffPatchUserCredentials() throws Exception {
        ObjectModificationType changes = CalculateXmlDiff.calculateChanges(new File("src/test/resources/user-credentials-old.xml"), new File("src/test/resources/user-credentials-new.xml"));
        assertNotNull(changes);
        assertEquals(2, changes.getPropertyModification().size());
        assertEquals("d7f1f990-b1fc-4001-9003-2106bd289c5b", changes.getOid());

        String patchedXml = (new PatchXml()).applyDifferences(changes, new File("src/test/resources/user-credentials-old.xml"));
        XmlAsserts.assertPatch(new File("src/test/resources/user-credentials-new.xml"), patchedXml);
    }
}
