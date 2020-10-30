/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.common.table;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.assignmentholder.AssignmentHolderObjectListTable;
import com.evolveum.midpoint.schrodinger.component.modal.ConfirmationModal;
import com.evolveum.midpoint.schrodinger.component.modal.FocusSetAssignmentsModal;
import com.evolveum.midpoint.schrodinger.component.table.TableHeaderDropDownMenu;
import com.evolveum.midpoint.schrodinger.page.BasicPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import com.evolveum.midpoint.schrodinger.util.Utils;

import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by matus on 5/2/2018.
 */
public abstract class TableWithPageRedirect<T> extends Table<T> {

    public TableWithPageRedirect(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public abstract <E extends BasicPage> E clickByName(String name);

    public abstract TableWithPageRedirect<T> selectCheckboxByName(String name);

    protected abstract <P extends TableWithPageRedirect<T>> TableHeaderDropDownMenu<P> clickHeaderActionDropDown();

    protected SelenideElement clickAndGetHeaderDropDownMenu() {

        $(By.tagName("thead"))
                .$(Schrodinger.byDataId("inlineMenuPanel"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S)
                .click();

        SelenideElement dropDownMenu = $(Schrodinger.byElementAttributeValue("ul", "class", "dropdown-menu pull-right"));

        return dropDownMenu;
    }

    protected  void clickMenu(String columnTitleKey, String rowValue, String menuItemKey) {
        clickMenuItem(columnTitleKey, rowValue, menuItemKey);
    }

    protected  <P extends TableWithPageRedirect<T>> ConfirmationModal<P> clickMenuItemWithConfirmation(String columnTitleKey, String rowValue, String menuItemKey) {
        clickMenuItem(columnTitleKey, rowValue, menuItemKey);
        return new ConfirmationModal<P>((P) this, Utils.getModalWindowSelenideElement());
    }

    protected  <P extends TableWithPageRedirect> FocusSetAssignmentsModal<P> clickMenuItemWithFocusSetAssignmentsModal(String columnTitleKey, String rowValue, String menuItemKey) {
        clickMenuItem(columnTitleKey, rowValue, menuItemKey);
        return new FocusSetAssignmentsModal<P>((P) this, Utils.getModalWindowSelenideElement());
    }

    /**
     * click menu item for the row specified by columnTitleKey and rowValue
     * or click menu item from header menu drop down if no row is specified
     * @param columnTitleKey
     * @param rowValue
     * @param menuItemKey
     */
    private void clickMenuItem(String columnTitleKey, String rowValue, String menuItemKey){
        if (columnTitleKey == null && rowValue == null) {
            clickAndGetHeaderDropDownMenu()
                    .$(Schrodinger.byDataResourceKey(menuItemKey))
                    .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S)
                    .click();
        } else {
            rowByColumnResourceKey(columnTitleKey, rowValue)
                    .getInlineMenu()
                    .clickItemByKey(menuItemKey);

        }
    }

}
