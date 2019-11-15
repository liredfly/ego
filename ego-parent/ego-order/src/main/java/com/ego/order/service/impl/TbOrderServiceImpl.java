package com.ego.order.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.dubbo.config.annotation.Reference;
import com.ego.commons.pojo.EgoResult;
import com.ego.commons.pojo.TbItemChild;
import com.ego.commons.utils.CookieUtils;
import com.ego.commons.utils.HttpClientUtil;
import com.ego.commons.utils.IDUtils;
import com.ego.commons.utils.JsonUtils;
import com.ego.dubbo.service.TbItemDubboService;
import com.ego.dubbo.service.TbOrderDubboService;
import com.ego.order.pojo.MyOrderParam;
import com.ego.order.service.TbOrderService;
import com.ego.pojo.TbItem;
import com.ego.pojo.TbOrder;
import com.ego.pojo.TbOrderItem;
import com.ego.pojo.TbOrderShipping;
import com.ego.redis.dao.JedisDao;

@Service
public class TbOrderServiceImpl implements TbOrderService{
	@Resource
	private JedisDao jedisDaoImpl;
	@Value("${passport.url}")
	private String passportUrl;
	@Value("${cart.key}")
	private String cartKey;
	@Reference
	private TbItemDubboService tbItemDubboServiceImpl;
	@Reference
	private TbOrderDubboService tbOrderDubboServiceImpl;
	@Override
	public List<TbItemChild> showOrderCart(List<Long> ids, HttpServletRequest request) {
		String token = CookieUtils.getCookieValue(request, "TT_token");
		String erJson = HttpClientUtil.doPost(passportUrl+token);
		EgoResult er = JsonUtils.jsonToPojo(erJson, EgoResult.class);
		String key = cartKey+((LinkedHashMap)er.getData()).get("username");
		String json= jedisDaoImpl.get(key);
		//从redis中查出购物车中所有的商品信息
		List<TbItemChild> list = JsonUtils.jsonToList(json, TbItemChild.class);
		//newList用户存储订单确认页中的商品信息
		List<TbItemChild> newList = new ArrayList<>();
		for (TbItemChild child : list) {
			for (long id : ids) {
				//如果购物车中的商品在订单确认页面中存在，则存入到newList中
				if((long)child.getId()==(long)id){
					//从数据库中查询商品数量,并判断商品提交的数量是否大于库存数量
					TbItem item= tbItemDubboServiceImpl.selById(id);
					child.setInventory(item.getNum()>child.getNum()?true:false);
					newList.add(child);					
				}
			}
		}
		return newList;
	}
	
	
	@Override
	public EgoResult create(MyOrderParam param, HttpServletRequest request) {
		//订单表数据
		TbOrder order = new TbOrder();
		order.setPayment(param.getPayment());
		order.setPaymentType(param.getPaymentType());
		//订单表id
		long id = IDUtils.genItemId();
		order.setOrderId(id+"");
		Date date = new Date();
		order.setCreateTime(date);
		order.setUpdateTime(date);
		//获取用户信息
		String token = CookieUtils.getCookieValue(request, "TT_token");
		String erJson = HttpClientUtil.doPost(passportUrl+token);
		EgoResult er = JsonUtils.jsonToPojo(erJson, EgoResult.class);
		Map user = (LinkedHashMap)er.getData();
		long uid =Long.parseLong(user.get("id").toString());
		order.setUserId(uid);
		order.setBuyerNick(user.get("username").toString());
		order.setBuyerRate(0);
		//订单—商品表数据
		for(TbOrderItem item:param.getOrderItems()){
			item.setId(IDUtils.genItemId()+"");
			item.setItemId(id+"");
		}
		//收货人数据
		TbOrderShipping shipping = param.getOrderShipping();
		shipping.setOrderId(id+"");
		shipping.setCreated(date);
		shipping.setUpdated(date);
		
		EgoResult result = new EgoResult();
		try {
			int index =tbOrderDubboServiceImpl.insertTbOrder(order, param.getOrderItems(), shipping);
			if(index>0){
				result.setStatus(200);
				//删除redis中购物车中数据
				String key = cartKey+user.get("username");
				String json= jedisDaoImpl.get(key);
				if(json!=null&&!json.equals("")){
					List<TbItemChild> childList = JsonUtils.jsonToList(json, TbItemChild.class);
					List<TbItemChild> newList = null;
					for (TbItemChild child : childList) {
						for(TbOrderItem item:param.getOrderItems()){
							if(child.getId()==Long.parseLong(item.getItemId())){
								newList.add(child);
							}
						}
					}
					for (TbItemChild child : newList) {
						childList.remove(child);
					}
					jedisDaoImpl.set(key, JsonUtils.objectToJson(childList));
					
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
	
	
	

}

