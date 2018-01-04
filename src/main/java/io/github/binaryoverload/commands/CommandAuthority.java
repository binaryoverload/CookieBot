package io.github.binaryoverload.commands;

import io.github.binaryoverload.CookieBot;
import io.github.binaryoverload.util.Constants;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;

public enum CommandAuthority {

    EVERYONE(null),
    STAFF(Constants.STAFF_ID),
    ADMIN(Constants.DEVELOPER_ID);

    private String roleID;

    CommandAuthority(String roleID) {
        this.roleID = roleID;
    }

    CommandAuthority(long roleID) {
        this.roleID = String.valueOf(roleID);
    }

    public boolean hasPerm(User user) {
        Member member = CookieBot.getInstance().getGuildById(Constants.OFFICIAL_GUILD).getMember(user);
        return (this.roleID == null || member != null)
                && (this.roleID == null || member.getRoles().stream().filter(r -> r.getId().equals(roleID)).count() == 1);
    }

}
