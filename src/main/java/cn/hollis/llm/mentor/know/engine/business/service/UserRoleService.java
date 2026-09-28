package cn.hollis.llm.mentor.know.engine.business.service;

import cn.hollis.llm.mentor.know.engine.chat.entity.ChatParam;
import cn.hollis.llm.mentor.know.engine.rag.constant.RoleEnum;

public interface UserRoleService {

    public RoleEnum getUserRole(ChatParam chatParam);
}
