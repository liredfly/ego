package com.ego.dubbo.service;

import com.ego.pojo.TbUser;

public interface TbUserDubboService {
	/**
	 * 用户登录验证
	 * @param user
	 * @return
	 */
	TbUser selUser(TbUser user);
}
