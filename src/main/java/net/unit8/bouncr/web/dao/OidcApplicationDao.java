package net.unit8.bouncr.web.dao;

import net.unit8.bouncr.web.DomaConfig;
import net.unit8.bouncr.web.entity.OidcApplication;
import net.unit8.bouncr.web.entity.Permission;
import org.seasar.doma.*;

import java.util.List;

@Dao(config= DomaConfig.class)
public interface OidcApplicationDao {
    @Select
    List<OidcApplication> selectAll();

    @Select(ensureResult = true)
    OidcApplication selectById(Long id);

    @Select(ensureResult = true)
    OidcApplication selectByClientId(String clientId);

    @Select
    List<Permission> selectPermissionsById(Long id);

    @Insert
    int insert(OidcApplication oidcApplication);

    @Insert(sqlFile = true)
    int addScope(OidcApplication oidcApplication, Permission permission);

    @Update
    int update(OidcApplication oidcApplication);

    @Delete
    int delete(OidcApplication oidcApplication);

    @Delete(sqlFile = true)
    int clearScopes(OidcApplication oidcApplication);
}
