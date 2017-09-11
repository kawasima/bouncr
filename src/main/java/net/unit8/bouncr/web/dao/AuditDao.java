package net.unit8.bouncr.web.dao;

import net.unit8.bouncr.web.DomaConfig;
import net.unit8.bouncr.web.entity.ActionType;
import net.unit8.bouncr.web.entity.SignInHistory;
import net.unit8.bouncr.web.entity.UserAction;
import org.seasar.doma.Dao;
import org.seasar.doma.Insert;
import org.seasar.doma.Select;
import org.seasar.doma.jdbc.Config;
import org.seasar.doma.jdbc.SelectOptions;
import org.seasar.doma.jdbc.builder.SelectBuilder;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Dao(config = DomaConfig.class)
public interface AuditDao {
    @Select
    List<UserAction> selectForConditionalSearch(LocalDateTime startAt, LocalDateTime endAt,
                                                String account,
                                                SelectOptions options);

    @Select
    List<String> selectRecentSigninHistories(String account, SelectOptions options);

    @Insert(sqlFile = true)
    int insertUserAction(ActionType actionType, String account, String remoteAddress);
}
