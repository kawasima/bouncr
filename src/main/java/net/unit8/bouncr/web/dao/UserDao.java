package net.unit8.bouncr.web.dao;

import enkan.security.UserPrincipal;
import net.unit8.bouncr.util.PasswordUtils;
import net.unit8.bouncr.web.DomaConfig;
import net.unit8.bouncr.web.entity.OtpKey;
import net.unit8.bouncr.web.entity.PasswordCredential;
import net.unit8.bouncr.web.entity.User;
import org.seasar.doma.*;
import org.seasar.doma.jdbc.Config;
import org.seasar.doma.jdbc.SelectOptions;
import org.seasar.doma.jdbc.builder.SelectBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

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
    List<User> selectByPrincipalScope(UserPrincipal principal, SelectOptions options);

    @Select
    List<User> selectByGroupId(Long groupId);

    @Select(ensureResult = true)
    User selectById(long id);

    @Select(ensureResult = true)
    User selectByAccount(String account);

    default User selectByPassword(String account, String password) {
        User user = selectByAccount(account);
        Config config = Config.get(this);
        SelectBuilder builder = SelectBuilder.newInstance(config);
        PasswordCredential credential = builder.sql("SELECT * ")
                .sql("FROM password_credentials ")
                .sql("WHERE user_id = ").param(Long.class, user.getId())
                .getEntitySingleResult(PasswordCredential.class);
        if (Arrays.equals(
                credential.getPassword(),
                PasswordUtils.pbkdf2(password, credential.getSalt(), 100))) {
            return user;
        }
        return null;
    }

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

    @Select
    OtpKey selectOtpKeyById(Long id);

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
