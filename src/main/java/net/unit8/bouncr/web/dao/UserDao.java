package net.unit8.bouncr.web.dao;

import net.unit8.bouncr.web.DomaConfig;
import net.unit8.bouncr.web.entity.User;
import org.seasar.doma.*;

import java.util.List;

/**
 * A data access object for user entity.
 *
 * @author kawasima
 */
@Dao(config = DomaConfig.class)
public interface UserDao {
    @Select
    List<User> selectAll();

    @Select(ensureResult = true)
    User selectById(long id);

    @Select(ensureResult = true)
    User selectByAccount(String account);

    @Select
    User selectByPassword(String account, String password);

    @Select
    List<User> selectForIncrementalSearch(String word);

    @Insert
    int insert(User user);

    @Update
    int update(User user);

    @Delete
    int delete(User user);

}
