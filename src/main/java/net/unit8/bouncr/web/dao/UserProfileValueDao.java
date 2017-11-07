package net.unit8.bouncr.web.dao;

import net.unit8.bouncr.web.DomaConfig;
import net.unit8.bouncr.web.entity.UserProfileValue;
import org.seasar.doma.Dao;
import org.seasar.doma.Delete;
import org.seasar.doma.Insert;
import org.seasar.doma.Update;

@Dao(config = DomaConfig.class)
public interface UserProfileValueDao {
    @Insert
    int insert(UserProfileValue userProfileValue);

    @Update
    int update(UserProfileValue userProfileValue);

    @Delete
    int delete(UserProfileValue userProfileValue);
}
