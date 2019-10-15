package org.eclipse.kura.web.server;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.web.Console;
import org.eclipse.kura.web.UserConfiguration;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtUserData;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtUserService;

public class GwtUserServiceImpl extends OsgiRemoteServiceServlet implements GwtUserService {

    private static final long serialVersionUID = 6065248347373180366L;

    @Override
    public void createUser(final GwtXSRFToken token, final String userName) throws GwtKuraException {
        checkXSRFToken(token);

        updateUserConfiguration(c -> c.createUser(userName));
    }

    @Override
    public void deleteUser(final GwtXSRFToken token, final String userName) throws GwtKuraException {
        checkXSRFToken(token);

        updateUserConfiguration(c -> c.deleteUser(userName));

    }

    @Override
    public void setUserPassword(final GwtXSRFToken token, final String userName, final String password)
            throws GwtKuraException {
        checkXSRFToken(token);

        updateUserConfiguration(c -> c.setPassword(userName, password));
    }

    @Override
    public Set<String> getDefinedPermissions(final GwtXSRFToken token) throws GwtKuraException {
        checkXSRFToken(token);
        return Console.instance().getRegisteredPermissions();
    }

    @Override
    public Set<GwtUserData> getUserData(final GwtXSRFToken token) throws GwtKuraException {
        checkXSRFToken(token);
        return withUserConfiguration(c -> new HashSet<>(c.getUserData().values()));
    }

    @Override
    public void setUserData(final GwtXSRFToken token, final Map<String, GwtUserData> userData) throws GwtKuraException {
        checkXSRFToken(token);

        updateUserConfiguration(c -> c.setUserData(userData));
    }

    private <T> T withUserConfiguration(final Function<UserConfiguration, T> func) {
        return func.apply(Console.getConsoleOptions().getUserConfiguration());
    }

    private void updateUserConfiguration(final Consumer<UserConfiguration> consumer) throws GwtKuraException {
        final UserConfiguration userConfiguration = Console.getConsoleOptions().getUserConfiguration();
        consumer.accept(userConfiguration);
        store(userConfiguration);
    }

    private void store(final UserConfiguration userConfiguration) throws GwtKuraException {
        ServiceLocator.applyToServiceOptionally(ConfigurationService.class, configurationService -> {
            userConfiguration.store(configurationService);
            return (Void) null;
        });
    }
}
