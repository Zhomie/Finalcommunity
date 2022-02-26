package com.homie.community.dao;

import com.homie.community.entity.LoginTicket;
import org.apache.ibatis.annotations.*;

@Mapper
/*该主键不推荐使用了*/
@Deprecated
public interface LoginTicketMapper {
    @Insert({
            "insert into login_ticket(user_id,ticket,status,expired) ",
            "values(#{userId},#{ticket},#{status},#{expired}) "
    })
    @Options(useGeneratedKeys = true,keyProperty = "id")
    public int insertLoginTicket(LoginTicket loginTicket);

    @Select({"select id,user_id,ticket,status,expired ",
            "from login_ticket ",
            "where ticket = #{ticket}"
    })
    LoginTicket selectByTicket(String ticket);
    @Update({"update login_ticket  set status=#{status} where ticket = #{ticket} "})
    int updateStatus(String ticket,int status);
}