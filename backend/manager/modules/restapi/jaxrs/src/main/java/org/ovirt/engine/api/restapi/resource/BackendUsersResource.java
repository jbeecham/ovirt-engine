package org.ovirt.engine.api.restapi.resource;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.ovirt.engine.api.model.BaseResource;
import org.ovirt.engine.api.model.User;
import org.ovirt.engine.api.model.Users;
import org.ovirt.engine.api.resource.UserResource;
import org.ovirt.engine.api.resource.UsersResource;
import org.ovirt.engine.api.restapi.resource.BackendUsersResourceBase.UserIdResolver;
import org.ovirt.engine.core.common.action.AddUserParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.AdUser;
import org.ovirt.engine.core.common.businessentities.DbUser;
import org.ovirt.engine.core.common.interfaces.SearchType;

public class BackendUsersResource extends BackendUsersResourceBase implements UsersResource {

    public BackendUsersResource() {
        super(User.class, DbUser.class, SUB_COLLECTIONS);
    }

    public BackendUsersResource(String id, BackendDomainResource parent) {
        super(id, parent);
    }

    @Override
    @SingleEntityResource
    public UserResource getUserSubResource(String id) {
        return inject(new BackendUserResource(id, this));
    }

    @Override
    public Users list() {
          return mapDbUserCollection(getBackendCollection(SearchType.DBUser, getSearchPattern()));
    }

    @Override
    public Response add(User user) {
        validateParameters(user, "userName");
        if (!isNameConatinsDomain(user)) {// user-name may contain the domain (e.g: oliel@xxx.yyy)
            validateParameters(user, "domain.id|name");
        }
        String domain = getDomain(user);
        AdUser adUser = getEntity(AdUser.class,
                                  SearchType.AdUser,
                                  getSearchPattern(user.getUserName(), domain));
        if (adUser == null) {
            return Response.status(Status.BAD_REQUEST)
                    .entity("No such user: " + user.getUserName() + " in domain " + domain)
                    .build();
        }
        AddUserParameters newUser = new AddUserParameters();
        newUser.setVdcUser(map(adUser));
        return performCreation(VdcActionType.AddUser, newUser, new UserIdResolver(adUser.getUserId()), BaseResource.class);
    }

    private boolean isNameConatinsDomain(User user) {
        return ((user.getUserName().contains("@")) && (user.getUserName().indexOf('@') != user.getUserName().length() - 1));
    }

}
