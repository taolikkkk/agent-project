package cn.hollis.llm.mentor.know.engine.business.service.impl;

import cn.hollis.llm.mentor.know.engine.business.entity.UserInfo;
import cn.hollis.llm.mentor.know.engine.business.mapper.UserInfoMapper;
import cn.hollis.llm.mentor.know.engine.business.service.UserInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 客户信息表 Service 实现类
 */
@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements UserInfoService {
}
