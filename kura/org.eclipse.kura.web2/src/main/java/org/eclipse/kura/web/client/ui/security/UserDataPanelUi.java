package org.eclipse.kura.web.client.ui.security;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.eclipse.kura.web.client.ui.AlertDialog;
import org.eclipse.kura.web.shared.model.GwtUserData;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.eclipse.kura.web.shared.service.GwtUserService;
import org.eclipse.kura.web.shared.service.GwtUserServiceAsync;
import org.gwtbootstrap3.client.ui.CheckBox;
import org.gwtbootstrap3.client.ui.FormGroup;
import org.gwtbootstrap3.client.ui.Panel;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class UserDataPanelUi extends Composite {

    private static UsersDataPanelUiUiBinder uiBinder = GWT.create(UsersDataPanelUiUiBinder.class);

    private final GwtSecurityTokenServiceAsync xsrfService = GWT.create(GwtSecurityTokenService.class);
    private final GwtUserServiceAsync userService = GWT.create(GwtUserService.class);

    interface UsersDataPanelUiUiBinder extends UiBinder<Widget, UserDataPanelUi> {
    }

    @UiField
    Panel permissionsPanel;
    @UiField
    AlertDialog alertDialog;

    private final GwtUserData userData;
    private final List<String> definedPermissions;

    public UserDataPanelUi(final GwtUserData userData, final Set<String> definedPermissions) {
        initWidget(uiBinder.createAndBindUi(this));

        this.definedPermissions = new ArrayList<>(definedPermissions);
        this.definedPermissions.sort(Comparator.naturalOrder());
        this.userData = userData;

        initPermissionsList();
    }

    private void initPermissionsList() {
        this.permissionsPanel.clear();

        final Set<String> currentPermissions = userData.getPermissions();

        for (final String permission : definedPermissions) {
            final FormGroup group = new FormGroup();
            final CheckBox checkbox = new CheckBox();
            checkbox.setText(permission);
            checkbox.setValue(currentPermissions.contains(permission));
            checkbox.addChangeHandler(e -> {
                if (Boolean.TRUE.equals(checkbox.getValue())) {
                    currentPermissions.add(permission);
                } else {
                    currentPermissions.remove(permission);
                }
            });
            group.add(checkbox);
            permissionsPanel.add(group);
        }
    }

}
