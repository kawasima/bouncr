package net.unit8.bouncr.web.dao;

import enkan.security.UserPrincipal;
import net.unit8.bouncr.authz.UserPermissionPrincipal;
import net.unit8.bouncr.web.DomaConfig;
import net.unit8.bouncr.web.entity.UserSession;
import org.seasar.doma.Dao;
import org.seasar.doma.Delete;
import org.seasar.doma.Insert;
import org.seasar.doma.Select;

import java.util.List;

@Dao(config = DomaConfig.class)
public interface UserSessionDao {
    @Select
    UserSession selectByToken(String token);

    @Select(ensureResult = true)
    UserSession selectById(Long id, UserPermissionPrincipal principal);

    @Select
    List<UserSession> selectByUserId(Long userId);

    @Insert
    int insert(UserSession userSession);

    @Delete
    int delete(UserSession userSession);
}
