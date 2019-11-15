package com.ego.cart.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.dubbo.config.annotation.Reference;
import com.ego.cart.service.CartService;
import com.ego.commons.pojo.EgoResult;
import com.ego.commons.pojo.TbItemChild;
import com.ego.commons.utils.CookieUtils;
import com.ego.commons.utils.HttpClientUtil;
import com.ego.commons.utils.JsonUtils;
import com.ego.dubbo.service.TbItemDubboService;
import com.ego.pojo.TbItem;
import com.ego.redis.dao.JedisDao;

@Service
public class CartServiceImpl implements CartService{
	@Resource
	private JedisDao jedisDaoImpl;
	@Reference
	private TbItemDubboService tbItemDubboServiceImpl;
	@Value("${passport.url}")
	private String passportUrl;
	@Value("${cart.key}")
	private String cartKey;
	@Override
	public void addCart(long id, int num,HttpServletRequest request) {
		//集合中存放所有购物车信息
		List<TbItemChild> list = new ArrayList<>();
		//redis中的key
		String token = CookieUtils.getCookieValue(request, "TT_TOKEN");
		String json = HttpClientUtil.doPost(passportUrl+token);
		EgoResult er = JsonUtils.jsonToPojo(json, EgoResult.class);
		String key = cartKey+((LinkedHashMap)er.getData()).get("username");
		
		//如果redis中存在key
		if(jedisDaoImpl.exists(key)){
			String value = jedisDaoImpl.get(key);
			if(value!=null&&!value.equals("")){
				list = JsonUtils.jsonToList(value, TbItemChild.class);
				for (TbItemChild child : list) {
					//如果购物车中有该商品
					if((long)child.getId()==id){
						child.setNum(num+child.getNum());
						jedisDaoImpl.set(key, JsonUtils.objectToJson(list));
						return;
					}
				}
			}
		}
		//购物车没有该商品则添加入该商品信息
		TbItem tbItem = tbItemDubboServiceImpl.selById(id);
		TbItemChild child = new TbItemChild();
		
		child.setId(tbItem.getId());
		child.setTitle(child.getTitle());
		child.setImages(tbItem.getImage()==null||tbItem.getImage().equals("")?new String[1]:tbItem.getImage().split(","));
		child.setNum(num);
		child.setPrice(tbItem.getPrice());
		
		list.add(child);
		//将购物车商品信息添加入购物车
		jedisDaoImpl.set(key, JsonUtils.objectToJson(list));
	}
	@Override
	public List<TbItemChild> showCart(HttpServletRequest request) {
		//redis中的key
		String token = CookieUtils.getCookieValue(request, "TT_TOKEN");
		String json = HttpClientUtil.doPost(passportUrl+token);
		EgoResult er = JsonUtils.jsonToPojo(json, EgoResult.class);
		String key = cartKey+((LinkedHashMap)er.getData()).get("username");
		
		String jsonvalue = jedisDaoImpl.get(key);
		return JsonUtils.jsonToList(jsonvalue, TbItemChild.class);
	}
	
	
	@Override
	public EgoResult update(long id, int num, HttpServletRequest request) {
		//redis中的key
		String token = CookieUtils.getCookieValue(request, "TT_TOKEN");
		String json = HttpClientUtil.doPost(passportUrl+token);
		EgoResult er = JsonUtils.jsonToPojo(json, EgoResult.class);
		String key = cartKey+((LinkedHashMap)er.getData()).get("username");
			
		String jsonvalue = jedisDaoImpl.get(key);
		List<TbItemChild> jsonList = JsonUtils.jsonToList(jsonvalue, TbItemChild.class);
		for (TbItemChild child : jsonList) {
			if((long)child.getId()==id){
				child.setNum(num);
			}
		}
		String ok = jedisDaoImpl.set(key, JsonUtils.objectToJson(jsonList));
		EgoResult result = new EgoResult();
		if(ok.equals("OK")){
			result.setStatus(200);
		}
		return result;
	}
	@Override
	public EgoResult dalete(long id, HttpServletRequest request) {
		//redis中的key
		String token = CookieUtils.getCookieValue(request, "TT_TOKEN");
		String json = HttpClientUtil.doPost(passportUrl+token);
		EgoResult er = JsonUtils.jsonToPojo(json, EgoResult.class);
		String key = cartKey+((LinkedHashMap)er.getData()).get("username");
		
		String jsonvalue = jedisDaoImpl.get(key);
		TbItemChild tc = null;
		List<TbItemChild> jsonList = JsonUtils.jsonToList(jsonvalue, TbItemChild.class);
		for (TbItemChild child : jsonList) {
			if((long)child.getId()==id){
				tc=child;
			}
		}
		jsonList.remove(tc);
		String ok = jedisDaoImpl.set(key, JsonUtils.objectToJson(jsonList));
		EgoResult result = new EgoResult();
		if(ok.equals("OK")){
			result.setStatus(200);
		}
		return result;
	}
}
