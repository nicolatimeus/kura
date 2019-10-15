package org.eclipse.kura.web.client.ui.security;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.eclipse.kura.web.client.ui.AlertDialog;
import org.eclipse.kura.web.client.ui.Tab;
import org.eclipse.kura.web.client.util.request.RequestQueue;
import org.eclipse.kura.web.shared.model.GwtUserData;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.eclipse.kura.web.shared.service.GwtUserService;
import org.eclipse.kura.web.shared.service.GwtUserServiceAsync;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.Panel;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.html.Span;

import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;

public class UsersTabUi extends Composite implements Tab {

    private static UsersTabUiUiBinder uiBinder = GWT.create(UsersTabUiUiBinder.class);

    private final GwtSecurityTokenServiceAsync xsrfService = GWT.create(GwtSecurityTokenService.class);
    private final GwtUserServiceAsync userService = GWT.create(GwtUserService.class);

    interface UsersTabUiUiBinder extends UiBinder<Widget, UsersTabUi> {
    }

    @UiField
    Button refresh;
    @UiField
    Button add;
    @UiField
    Button delete;
    @UiField
    CellTable<GwtUserData> usersTable;
    @UiField
    Modal modal;
    @UiField
    Span modalText;
    @UiField
    TextBox modalInput;
    @UiField
    Button modalCancel;
    @UiField
    Button modalConfirm;
    @UiField
    AlertDialog alertDialog;
    @UiField
    Panel userDataPanel;

    private final SingleSelectionModel<GwtUserData> selectionModel = new SingleSelectionModel<>();
    private final ListDataProvider<GwtUserData> dataProvider = new ListDataProvider<>();

    private HandlerRegistration modalConfirmHandler;
    private HandlerRegistration modalCancelHandler;

    private List<String> definedPermissions;

    public UsersTabUi() {
        this.initWidget(uiBinder.createAndBindUi(this));

        refresh.addClickHandler(e -> this.refresh());
        add.addClickHandler(e -> this.addUser());
        delete.addClickHandler(e -> this.deleteUser());

        selectionModel.addSelectionChangeHandler(e -> {
            final GwtUserData selected = selectionModel.getSelectedObject();
            delete.setEnabled(selected != null);
            userDataPanel.clear();
            if (selected != null) {
                userDataPanel.add(new UserDataPanelUi(selected, new HashSet<>(definedPermissions)));
            }
        });

        this.dataProvider.addDataDisplay(usersTable);
        this.usersTable.setSelectionModel(selectionModel);

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
        RequestQueue.submit(c -> xsrfService
                .generateSecurityToken(c.callback(token -> userService.getUserData(token, c.callback(data -> {
                    dataProvider.getList().clear();
                    dataProvider.getList().addAll(data);
                    xsrfService.generateSecurityToken(
                            c.callback(token2 -> userService.getDefinedPermissions(token2, c.callback(permissions -> {
                                this.definedPermissions = new ArrayList<>(permissions);
                                this.definedPermissions.sort(Comparator.naturalOrder());
                                initTable();
                            }))));
                })))));

    }

    private void addUser() {
        showInputDialog("Create user", "Insert user name",
                Optional.of(userName -> RequestQueue.submit(c -> xsrfService.generateSecurityToken(
                        c.callback(token -> userService.createUser(token, userName, c.callback(ok -> refresh())))))),
                Optional.empty());
    }

    private void deleteUser() {
        final GwtUserData selected = selectionModel.getSelectedObject();

        if (selected == null) {
            return;
        }

        final String userName = selected.getUserName();

        alertDialog.show("Are you sure you want to delete the user " + userName + "?",
                ok -> RequestQueue.submit(c -> xsrfService.generateSecurityToken(
                        c.callback(token -> userService.deleteUser(token, userName, c.callback(ok2 -> refresh()))))));
    }

    private void initTable() {

        while (this.usersTable.getColumnCount() > 0) {
            this.usersTable.removeColumn(this.usersTable.getColumnCount() - 1);
        }

        final Column<GwtUserData, String> userName = new TextColumn<GwtUserData>() {

            @Override
            public String getValue(final GwtUserData object) {
                return object.getUserName();
            }
        };

        this.usersTable.addColumn(userName, "User Name");

        for (final String permission : this.definedPermissions) {
            final Column<GwtUserData, Boolean> column = new Column<GwtUserData, Boolean>(new CheckboxCell()) {

                @Override
                public Boolean getValue(final GwtUserData object) {
                    return object.getPermissions().contains(permission);
                }
            };

            this.usersTable.addColumn(column, permission);
        }

        this.dataProvider.refresh();
        this.usersTable.redraw();
    }

    private void showInputDialog(final String title, final String message, final Optional<Consumer<String>> onConfirm,
            final Optional<Consumer<Void>> onCancel) {
        this.modal.setTitle(title);
        this.modalText.setText(message);

        if (modalConfirmHandler != null) {
            modalConfirmHandler.removeHandler();
        }

        if (modalCancelHandler != null) {
            modalCancelHandler.removeHandler();
        }

        this.modalConfirmHandler = this.modalConfirm.addClickHandler(e -> {
            onConfirm.ifPresent(c -> c.accept(modalInput.getValue()));
            modal.hide();
        });

        this.modalCancelHandler = this.modalConfirm.addClickHandler(e -> {
            onCancel.ifPresent(c -> c.accept(null));
            modal.hide();
        });

        this.modal.show();
    }
}
