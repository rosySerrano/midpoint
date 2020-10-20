/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.schrodinger.component;

import java.io.File;
import java.io.IOException;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.schrodinger.component.common.Search;
import com.evolveum.midpoint.schrodinger.component.user.UsersPageTable;
import com.evolveum.midpoint.schrodinger.page.role.RolesPageTable;
import com.evolveum.midpoint.schrodinger.page.service.ServicesPageTable;
import com.evolveum.midpoint.testing.schrodinger.AbstractSchrodingerTest;

/**
 * Created by honchar
 */
public class SearchPanelTest extends AbstractSchrodingerTest {

    private static final String COMPONENT_RESOURCES_DIRECTORY = "./src/test/resources/component/";
    private static final String COMPONENT_OBJECTS_DIRECTORY = COMPONENT_RESOURCES_DIRECTORY + "objects/";
    private static final String COMPONENT_USERS_DIRECTORY = COMPONENT_OBJECTS_DIRECTORY + "users/";
    private static final String COMPONENT_ROLES_DIRECTORY = COMPONENT_OBJECTS_DIRECTORY + "roles/";

    private static final File SEARCH_BY_NAME_USER_FILE = new File(COMPONENT_USERS_DIRECTORY + "searchByNameUser.xml");
    private static final File SEARCH_BY_GIVEN_NAME_USER_FILE = new File(COMPONENT_USERS_DIRECTORY + "searchByGivenNameUser.xml");
    private static final File SEARCH_BY_FAMILY_NAME_USER_FILE = new File(COMPONENT_USERS_DIRECTORY + "searchByFamilyNameUser.xml");
    private static final File REQUESTABLE_ROLE_FILE = new File(COMPONENT_ROLES_DIRECTORY + "requestableRole.xml");
    private static final File DISABLED_ROLE_FILE = new File(COMPONENT_ROLES_DIRECTORY + "disabledRole.xml");

    private static final String NAME_ATTRIBUTE = "Name";
    private static final String GIVEN_NAME_ATTRIBUTE = "Given name";
    private static final String FAMILY_NAME_ATTRIBUTE = "Family name";
    private static final String REQUESTABLE_ATTRIBUTE = "Requestable";
    private static final String ADMINISTRATIVE_STATUS_ATTRIBUTE = "Administrative status";
    private static final String ROLE_MEMBERSHIP_ATTRIBUTE = "Role membership";

    @BeforeClass
    @Override
    public void beforeClass() throws IOException {
        super.beforeClass();
        importObject(SEARCH_BY_NAME_USER_FILE, true);
        importObject(SEARCH_BY_GIVEN_NAME_USER_FILE, true);
        importObject(SEARCH_BY_FAMILY_NAME_USER_FILE, true);
        importObject(REQUESTABLE_ROLE_FILE, true);
        importObject(DISABLED_ROLE_FILE, true);
    }

    @Test
    public void test0010defaultSearchOnListPage() {
        UsersPageTable table = basicPage.listUsers().table();
        Search<UsersPageTable> usersListSearch = (Search<UsersPageTable>) table.search();

        usersListSearch
                .byItemName(NAME_ATTRIBUTE)
                .inputValue("searchByNameUser")
                .updateSearch();
        Assert.assertTrue(table.containsText("searchByNameUser"));
        Assert.assertFalse(table.containsText("searchByGivenNameUser"));
        Assert.assertFalse(table.containsText("searchByFamilyNameUser"));
        usersListSearch.clearTextSearchItemByNameAndUpdate(NAME_ATTRIBUTE);

        usersListSearch
                .byItemName(GIVEN_NAME_ATTRIBUTE)
                .inputValue("searchByGivenNameUser")
                .updateSearch();
        Assert.assertFalse(table.containsText("searchByNameUser"));
        Assert.assertTrue(table.containsText("searchByGivenNameUser"));
        Assert.assertFalse(table.containsText("searchByFamilyNameUser"));
        usersListSearch.clearTextSearchItemByNameAndUpdate(GIVEN_NAME_ATTRIBUTE);

        usersListSearch
                .byItemName(FAMILY_NAME_ATTRIBUTE)
                .inputValue("searchByFamilyNameUser")
                .updateSearch();
        Assert.assertFalse(table.containsText("searchByNameUser"));
        Assert.assertFalse(table.containsText("searchByGivenNameUser"));
        Assert.assertTrue(table.containsText("searchByFamilyNameUser"));
        usersListSearch.clearTextSearchItemByNameAndUpdate(FAMILY_NAME_ATTRIBUTE);
    }

    @Test
    public void test0020addSearchAttributeByAddButtonClick() {
        RolesPageTable table = basicPage.listRoles().table();
        Search<RolesPageTable> search = (Search<RolesPageTable>) table.search();
        search.addSearchItemByAddButtonClick(REQUESTABLE_ATTRIBUTE);
        Assert.assertNotNull(search.byItemName(REQUESTABLE_ATTRIBUTE, false));
    }

    @Test
    public void test0030addSearchAttributeByNameLinkClick() {
        ServicesPageTable table = basicPage.listServices().table();
        Search<ServicesPageTable> search = (Search<ServicesPageTable>) table.search();
        search.addSearchItemByNameLinkClick(ROLE_MEMBERSHIP_ATTRIBUTE);
        Assert.assertNotNull(search.byItemName(ROLE_MEMBERSHIP_ATTRIBUTE, false));
    }

    @Test
    public void test0040booleanAttributeSearch() {
        RolesPageTable table = basicPage.listRoles().table();
        Search<RolesPageTable> search = (Search<RolesPageTable>) table.search();
        search.resetBasicSearch();
        Assert.assertNotEquals(1, table.countTableObjects());
        search.byItemName(REQUESTABLE_ATTRIBUTE)
                .inputDropDownValue("True")
                .updateSearch();
        Assert.assertEquals(1, table.countTableObjects());
    }

    @Test
    public void test0050enumAttributeSearch() {
        RolesPageTable table = basicPage.listRoles().table();
        Search<RolesPageTable> search = (Search<RolesPageTable>) table.search();
        search.resetBasicSearch();
        Assert.assertNotEquals(1, table.countTableObjects());
        search.byItemName(ADMINISTRATIVE_STATUS_ATTRIBUTE)
                .inputDropDownValue("disabled")
                .updateSearch();
        Assert.assertEquals(1, table.countTableObjects());
    }

}