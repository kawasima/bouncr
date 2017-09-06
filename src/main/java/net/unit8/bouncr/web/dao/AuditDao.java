package net.unit8.bouncr.web.dao;

import net.unit8.bouncr.web.DomaConfig;
import net.unit8.bouncr.web.entity.SignInHistory;
import org.seasar.doma.Dao;
import org.seasar.doma.Insert;
import org.seasar.doma.Select;
import org.seasar.doma.jdbc.SelectOptions;

import java.time.LocalDateTime;
import java.util.List;

@Dao(config = DomaConfig.class)
public interface AuditDao {
    @Select
    List<SignInHistory> selectForConditionalSearch(LocalDateTime startAt, LocalDateTime endAt,
                                                   String account, Boolean successful,
                                                   SelectOptions options);

    @Insert(sqlFile = true)
    int signIn(String account, Boolean successful);
}
