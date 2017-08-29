package net.unit8.bouncr.web.dao;

import net.unit8.bouncr.web.DomaConfig;
import org.seasar.doma.Dao;
import org.seasar.doma.Insert;
import org.seasar.doma.Update;

@Dao(config = DomaConfig.class)
public interface PasswordCredentialDao {
    @Insert(sqlFile = true)
    int insert(Long id, String password, String salt);

    @Update(sqlFile = true)
    int update(Long id, String password, String salt);

}
