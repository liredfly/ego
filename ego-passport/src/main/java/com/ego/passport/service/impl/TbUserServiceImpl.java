package com.ego.passport.service.impl;

import java.util.UUID;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Service;

import com.alibaba.dubbo.config.annotation.Reference;
import com.ego.commons.pojo.EgoResult;
import com.ego.commons.utils.CookieUtils;
import com.ego.commons.utils.JsonUtils;
import com.ego.dubbo.service.TbUserDubboService;
import com.ego.passport.service.TbUserService;
import com.ego.pojo.TbUser;
import com.ego.redis.dao.JedisDao;

@Service
public class TbUserServiceImpl implements TbUserService{
	@Reference
	private TbUserDubboService tbUserDubboServiceImpl;
	@Resource
	private JedisDao jedisDaoImpl;
	@Override
	public EgoResult selUser(TbUser user,HttpServletRequest request,HttpServletResponse response) {
		EgoResult er = new EgoResult();
		TbUser tbUser = tbUserDubboServiceImpl.selUser(user);
		if(tbUser!=null){
			er.setStatus(200);
			//用户登录成功后吧信息存入redis中
			String key = UUID.randomUUID().toString();
			jedisDaoImpl.set(key, JsonUtils.objectToJson(tbUser));
			jedisDaoImpl.expire(key, 60*60*24*7);
			//产生cookie
			CookieUtils.setCookie(request, response, "TT_TOKEN", key, 60*60*24*7);
		}else{
			er.setMsg("用户名或密码错误");
		}
		return er;
	}
	
	
	@Override
	public EgoResult getUserInfoByToken(String token) {
		EgoResult er = new EgoResult();
		String json = jedisDaoImpl.get(token);
		if(json!=null&&!json.equals("")){
			TbUser user = JsonUtils.jsonToPojo(json,TbUser.class);
			//处于安全考虑将密码设置为null
			user.setPassword("");
			er.setStatus(200);
			er.setMsg("获取用户信息成功");
			er.setData(user);
		}else{
			er.setMsg("获取用户信息失败");
		}
		return er;
	}


	@Override
	public EgoResult logout(String token, HttpServletRequest request, HttpServletResponse response) {
		//删除redis中的用户信息
		jedisDaoImpl.del(token);
		//删除cookie中的tt_token
		CookieUtils.deleteCookie(request, response, "TT_TOKEN");
		EgoResult er = new EgoResult();
		er.setStatus(200);
		er.setMsg("OK");
		return er;
	}
	

}
