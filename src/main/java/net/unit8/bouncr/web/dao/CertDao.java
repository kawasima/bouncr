package net.unit8.bouncr.web.dao;

import net.unit8.bouncr.web.DomaConfig;
import net.unit8.bouncr.web.entity.Cert;
import org.seasar.doma.*;

import java.util.List;

@Dao(config = DomaConfig.class)
public interface CertDao {
    @Select
    List<Cert> selectByUserId(Long userId);

    @Insert
    int insert(Cert cert);

    @Update
    int update(Cert cert);

    @Delete
    int delete(Cert cert);

}
