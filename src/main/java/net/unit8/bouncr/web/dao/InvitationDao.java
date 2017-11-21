package net.unit8.bouncr.web.dao;

import net.unit8.bouncr.web.DomaConfig;
import net.unit8.bouncr.web.entity.GroupInvitation;
import net.unit8.bouncr.web.entity.Invitation;
import net.unit8.bouncr.web.entity.OidcInvitation;
import org.seasar.doma.Dao;
import org.seasar.doma.Insert;
import org.seasar.doma.Select;
import org.seasar.doma.jdbc.Config;
import org.seasar.doma.jdbc.builder.DeleteBuilder;

import java.util.List;

@Dao(config = DomaConfig.class)
public interface InvitationDao {
    @Select
    Invitation selectByCode(String code);

    @Select
    List<GroupInvitation> selectGroupInvitations(Long invitationId);

    @Select
    OidcInvitation selectOidcInvitation(Long invitationId);

    @Insert
    int insert(Invitation invitation);

    @Insert
    int insert(OidcInvitation oidcInvitation);

    @Insert
    int insertGroupInvitation(GroupInvitation groupInvitation);

    default int delete(Invitation invitation) {
        Config config = Config.get(this);
        DeleteBuilder.newInstance(config)
                .sql("DELETE FROM group_invitations ")
                .sql("WHERE invitation_id = ")
                .param(long.class, invitation.getId())
                .execute();

        DeleteBuilder.newInstance(config)
                .sql("DELETE FROM oidc_invitations ")
                .sql("WHERE invitation_id = ")
                .param(long.class, invitation.getId())
                .execute();

        return DeleteBuilder.newInstance(config)
                .sql("DELETE FROM invitations ")
                .sql("WHERE invitation_id = ")
                .param(long.class, invitation.getId())
                .execute();
    }
}
