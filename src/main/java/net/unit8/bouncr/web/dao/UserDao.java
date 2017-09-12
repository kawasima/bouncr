package net.unit8.bouncr.web.dao;

import net.unit8.bouncr.web.DomaConfig;
import net.unit8.bouncr.web.entity.User;
import org.seasar.doma.*;
import org.seasar.doma.jdbc.Config;
import org.seasar.doma.jdbc.builder.SelectBuilder;

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

    @Select
    List<User> selectByGroupId(Long groupId);

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

    @Select
    User selectByOAuth2(Long oauth2ProviderId, String oauth2UserName);


    default boolean isLock(String account) {
        Config config = Config.get(this);
        SelectBuilder builder = SelectBuilder.newInstance(config);
        int cnt = builder.sql("SELECT count(*) FROM user_locks UL")
                .sql(" JOIN users U ON U.user_id = UL.user_id")
                .sql(" WHERE")
                .sql(" account = ").param(String.class, account)
                .getScalarSingleResult(int.class);
        return cnt > 0;
    }

    @Insert(sqlFile = true)
    int lock(Long id);

    @Insert(sqlFile = true)
    int connectToOAuth2Provider(Long id, Long oauth2ProviderId, String oauth2UserName);

    @Delete(sqlFile = true)
    int unlock(Long id);


}
