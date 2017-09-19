package net.unit8.bouncr.web.dao;

import net.unit8.bouncr.web.DomaConfig;
import net.unit8.bouncr.web.entity.OtpKey;
import org.seasar.doma.Dao;
import org.seasar.doma.Delete;
import org.seasar.doma.Insert;

@Dao(config = DomaConfig.class)
public interface OtpKeyDao {
    @Insert
    int insert(OtpKey otpKey);

    @Delete
    int delete(OtpKey otpKey);
}
