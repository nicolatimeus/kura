/*******************************************************************************
 * Copyright (c) 2019 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.client.ui.security;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.Tab;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.eclipse.kura.web.shared.service.GwtSecurityService;
import org.eclipse.kura.web.shared.service.GwtSecurityServiceAsync;
import org.eclipse.kura.web2.ext.WidgetFactory;
import org.gwtbootstrap3.client.ui.NavTabs;
import org.gwtbootstrap3.client.ui.TabContent;
import org.gwtbootstrap3.client.ui.TabListItem;
import org.gwtbootstrap3.client.ui.TabPane;
import org.gwtbootstrap3.client.ui.html.Paragraph;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;

public class SecurityPanelUi extends Composite {

    private static SecurityPanelUiUiBinder uiBinder = GWT.create(SecurityPanelUiUiBinder.class);
    private static final Logger logger = Logger.getLogger(SecurityPanelUi.class.getSimpleName());
    private static final Messages MSGS = GWT.create(Messages.class);

    private final GwtSecurityServiceAsync gwtSecurityService = GWT.create(GwtSecurityService.class);

    interface SecurityPanelUiUiBinder extends UiBinder<Widget, SecurityPanelUi> {
    }

    GwtSession session;

    @UiField
    CertificateListTabUi certificateListPanel;

    @UiField
    ApplicationCertsTabUi appCertPanel;

    @UiField
    ServerCertsTabUi serverCertPanel;

    @UiField
    DeviceCertsTabUi deviceCertPanel;

    @UiField
    HttpsServerCertsTabUi httpsServerCertPanel;
    @UiField
    HttpsUserCertsTabUi httpsUserCertPanel;

    @UiField
    SecurityTabUi securityPanel;

    @UiField
    TabListItem certificateList;
    @UiField
    TabListItem appCert;
    @UiField
    TabListItem serverCert;
    @UiField
    TabListItem deviceCert;
    @UiField
    TabListItem httpsServerCert;
    @UiField
    TabListItem httpsUserCert;
    @UiField
    TabListItem security;
    @UiField
    TabContent tabContent;
    @UiField
    NavTabs navTabs;

    @UiField
    HTMLPanel securityIntro;

    public SecurityPanelUi() {
        logger.log(Level.FINER, "Initiating SecurityPanelUI...");

        initWidget(uiBinder.createAndBindUi(this));
        Paragraph description = new Paragraph();
        description.setText(MSGS.securityIntro());
        this.securityIntro.add(description);

        this.serverCert.setVisible(true);

        AsyncCallback<Boolean> callback = new AsyncCallback<Boolean>() {

            @Override
            public void onFailure(Throwable caught) {
                SecurityPanelUi.this.appCert.setVisible(false);
                SecurityPanelUi.this.security.setVisible(false);
                SecurityPanelUi.this.httpsUserCert.setVisible(false);
            }

            @Override
            public void onSuccess(Boolean result) {
                SecurityPanelUi.this.appCert.setVisible(result);
                SecurityPanelUi.this.security.setVisible(result);
                SecurityPanelUi.this.httpsUserCert.setVisible(result);
            }
        };
        this.gwtSecurityService.isSecurityServiceAvailable(callback);

        this.certificateList.addClickHandler(new Tab.RefreshHandler(this.certificateListPanel));
        this.serverCert.addClickHandler(new Tab.RefreshHandler(this.serverCertPanel));
        this.deviceCert.addClickHandler(new Tab.RefreshHandler(this.deviceCertPanel));
        this.httpsServerCert.addClickHandler(new Tab.RefreshHandler(this.httpsServerCertPanel));
        this.httpsUserCert.addClickHandler(new Tab.RefreshHandler(this.httpsUserCertPanel));
        this.security.addClickHandler(new Tab.RefreshHandler(this.securityPanel));
    }

    public void load() {
        if (!this.certificateListPanel.isDirty()) {
            this.certificateListPanel.refresh();
        }
    }

    public void setSession(GwtSession currentSession) {
        this.session = currentSession;
    }

    public boolean isDirty() {
        boolean certListDirty = this.certificateListPanel.isDirty();
        boolean appCertDirty = this.appCertPanel.isDirty();
        boolean serverCertDirty = this.serverCertPanel.isDirty();
        boolean deviceCertDirty = this.deviceCertPanel.isDirty();
        boolean httpsServerCertDirty = this.httpsServerCertPanel.isDirty();
        boolean httpsUserCertDirty = this.httpsUserCertPanel.isDirty();
        boolean securityDirty = this.securityPanel.isDirty();

        return certListDirty || appCertDirty || serverCertDirty || deviceCertDirty || securityDirty || httpsUserCertDirty || httpsServerCertDirty;
    }

    public void addTab(final String name, final WidgetFactory widgetFactory) {

        final TabPane tabPane = new TabPane();
        tabPane.setId("__extension__" + name);

        final TabListItem item = new TabListItem(name);
        item.setDataTarget("#__extension__" + name);

        item.addClickHandler(e -> {
            tabPane.clear();
            tabPane.add(widgetFactory.buildWidget());
        });

        this.navTabs.add(item);
        this.tabContent.add(tabPane);
    }

    public void setDirty(boolean b) {
        this.certificateListPanel.setDirty(b);
        this.appCertPanel.setDirty(b);
        this.serverCertPanel.setDirty(b);
        this.deviceCertPanel.setDirty(b);
        this.securityPanel.setDirty(b);
        this.httpsServerCertPanel.setDirty(b);
        this.httpsUserCertPanel.setDirty(b);
    }
}
