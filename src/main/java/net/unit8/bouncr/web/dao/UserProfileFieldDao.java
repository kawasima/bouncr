package net.unit8.bouncr.web.dao;

import net.unit8.bouncr.web.DomaConfig;
import net.unit8.bouncr.web.entity.UserProfile;
import net.unit8.bouncr.web.entity.UserProfileField;
import org.seasar.doma.*;

import java.util.List;

@Dao(config = DomaConfig.class)
public interface UserProfileFieldDao {
    @Select
    List<UserProfileField> selectAll();

    @Select
    UserProfileField selectById(Long id);

    @Select
    List<UserProfile> selectValuesByUserId(Long userId);

    @Insert
    int insert(UserProfileField userProfileField);

    @Update
    int update(UserProfileField userProfileField);

    @Delete
    int delete(UserProfileField userProfileField);
}
