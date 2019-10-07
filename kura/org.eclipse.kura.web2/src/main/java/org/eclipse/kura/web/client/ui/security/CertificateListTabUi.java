/*******************************************************************************
 * Copyright (c) 2019 Eurotech and/or its affiliates and others
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

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.EntryClassUi;
import org.eclipse.kura.web.client.ui.Tab;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.model.GwtCertificate;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtCertificatesService;
import org.eclipse.kura.web.shared.service.GwtCertificatesServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.ModalBody;
import org.gwtbootstrap3.client.ui.ModalFooter;
import org.gwtbootstrap3.client.ui.gwt.CellTable;
import org.gwtbootstrap3.client.ui.html.Span;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;

public class CertificateListTabUi extends Composite implements Tab {

    private static CertificateListTabUiUiBinder uiBinder = GWT.create(CertificateListTabUiUiBinder.class);
    private static final Logger logger = Logger.getLogger(CertificateListTabUi.class.getSimpleName());

    interface CertificateListTabUiUiBinder extends UiBinder<Widget, CertificateListTabUi> {
    }

    private static final Messages MSGS = GWT.create(Messages.class);

    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
    private final GwtCertificatesServiceAsync gwtCertificatesService = GWT.create(GwtCertificatesService.class);

    @UiField
    Button refresh;
    @UiField
    Button uninstall;

    @UiField
    CellTable<GwtCertificate> certificatesGrid = new CellTable<>();

    GwtCertificate selected;
    final SingleSelectionModel<GwtCertificate> selectionModel = new SingleSelectionModel<>();

    private final ListDataProvider<GwtCertificate> certificatesDataProvider = new ListDataProvider<>();

    public CertificateListTabUi() {
        logger.log(Level.FINER, "Initiating CertificatesTabUI...");
        initWidget(uiBinder.createAndBindUi(this));
        initTable();
        this.certificatesGrid.setSelectionModel(this.selectionModel);

        initInterfaceButtons();
    }

    @Override
    public void setDirty(boolean flag) {
    }

    @Override
    public boolean isDirty() {
        return false;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void refresh() {
        EntryClassUi.showWaitModal();
        this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(ex);
            }

            @Override
            public void onSuccess(GwtXSRFToken token) {
                CertificateListTabUi.this.gwtCertificatesService
                        .listCertificates(new AsyncCallback<List<GwtCertificate>>() {

                            @Override
                            public void onFailure(Throwable ex) {
                                EntryClassUi.hideWaitModal();
                                FailureHandler.handle(ex);
                            }

                            @Override
                            public void onSuccess(List<GwtCertificate> result) {
                                CertificateListTabUi.this.certificatesDataProvider.getList().clear();
                                for (GwtCertificate pair : result) {
                                    CertificateListTabUi.this.certificatesDataProvider.getList().add(pair);
                                }
                                int snapshotsDataSize = CertificateListTabUi.this.certificatesDataProvider.getList()
                                        .size();
                                if (snapshotsDataSize == 0) {
                                    CertificateListTabUi.this.certificatesGrid.setVisible(false);
                                } else {
                                    CertificateListTabUi.this.certificatesGrid.setVisibleRange(0, snapshotsDataSize);
                                    CertificateListTabUi.this.certificatesGrid.setVisible(true);
                                }
                                CertificateListTabUi.this.certificatesDataProvider.flush();
                                EntryClassUi.hideWaitModal();
                            }
                        });
            }

        });
    }

    private void initTable() {

        TextColumn<GwtCertificate> col1 = new TextColumn<GwtCertificate>() {

            @Override
            public String getValue(GwtCertificate object) {
                return String.valueOf(object.getAlias());
            }
        };
        col1.setCellStyleNames("status-table-row");
        this.certificatesGrid.addColumn(col1, MSGS.certificateAlias());

        TextColumn<GwtCertificate> col2 = new TextColumn<GwtCertificate>() {

            @Override
            public String getValue(GwtCertificate object) {
                return String.valueOf(object.getType().toString());
            }
        };
        col2.setCellStyleNames("status-table-row");
        this.certificatesGrid.addColumn(col2, MSGS.certificateType());

        this.certificatesDataProvider.addDataDisplay(this.certificatesGrid);
    }

    private void initInterfaceButtons() {
        this.refresh.setText(MSGS.refresh());
        this.refresh.addClickHandler(event -> refresh());

        this.uninstall.setText(MSGS.packageDeleteButton());
        this.uninstall.addClickHandler(event -> {
            this.selected = this.selectionModel.getSelectedObject();
            if (this.selected != null) {
                final Modal modal = new Modal();
                ModalBody modalBody = new ModalBody();
                ModalFooter modalFooter = new ModalFooter();
                modal.setClosable(true);
                modal.setTitle(MSGS.confirm());
                modalBody.add(new Span(MSGS.securityUninstallCertificate(this.selected.getAlias())));
                modalFooter.add(new Button(MSGS.noButton(), event11 -> modal.hide()));
                modalFooter.add(new Button(MSGS.yesButton(), event12 -> {
                    modal.hide();
                    uninstall(this.selected);
                }));

                modal.add(modalBody);
                modal.add(modalFooter);
                modal.show();
            }
        });
    }

    private void uninstall(final GwtCertificate selected) {

        EntryClassUi.showWaitModal();
        this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(ex);
            }

            @Override
            public void onSuccess(GwtXSRFToken token) {
                CertificateListTabUi.this.gwtCertificatesService.removeCertificate(token, selected,
                        new AsyncCallback<Void>() {

                            @Override
                            public void onFailure(Throwable caught) {
                                EntryClassUi.hideWaitModal();
                                FailureHandler.handle(caught);
                            }

                            @Override
                            public void onSuccess(Void result) {
                                EntryClassUi.hideWaitModal();
                                refresh();
                            }
                        });
            }

        });
    }
}
