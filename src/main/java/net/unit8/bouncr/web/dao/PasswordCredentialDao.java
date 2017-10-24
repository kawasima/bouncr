package net.unit8.bouncr.web.dao;

import net.unit8.bouncr.web.DomaConfig;
import net.unit8.bouncr.web.entity.PasswordCredential;
import org.seasar.doma.*;

@Dao(config = DomaConfig.class)
public interface PasswordCredentialDao {
    @Insert
    int insert(PasswordCredential passwordCredential);

    @Delete
    int delete(PasswordCredential passwordCredential);

    @Update
    int update(PasswordCredential passwordCredential);
}
