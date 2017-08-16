package net.unit8.bouncr.web.dao;

import net.unit8.bouncr.web.DomaConfig;
import org.seasar.doma.Dao;
import org.seasar.doma.Insert;

@Dao(config = DomaConfig.class)
public interface PasswordCredentialDao {
    @Insert(sqlFile = true)
    int insert(Long id, String password, String salt);
}
