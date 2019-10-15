/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.shared.service;

import java.util.Map;
import java.util.Set;

import org.eclipse.kura.web.server.RequiredPermissions;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.KuraPermission;
import org.eclipse.kura.web.shared.model.GwtUserData;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("users")
@RequiredPermissions(KuraPermission.USER_ADMIN)
public interface GwtUserService extends RemoteService {

    public void createUser(GwtXSRFToken token, final String userName) throws GwtKuraException;

    public void deleteUser(GwtXSRFToken token, final String userName) throws GwtKuraException;

    public void setUserPassword(GwtXSRFToken token, final String userName, final String password)
            throws GwtKuraException;

    public Set<String> getDefinedPermissions(GwtXSRFToken token) throws GwtKuraException;

    public Set<GwtUserData> getUserData(GwtXSRFToken token) throws GwtKuraException;

    public void setUserData(GwtXSRFToken token, Map<String, GwtUserData> userData) throws GwtKuraException;
}