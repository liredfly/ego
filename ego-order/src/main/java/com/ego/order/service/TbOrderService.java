package com.ego.order.service;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.ego.commons.pojo.EgoResult;
import com.ego.commons.pojo.TbItemChild;
import com.ego.order.pojo.MyOrderParam;

public interface TbOrderService {
	/**
	 * 显示订单确认页面
	 * @param id
	 * @param request
	 * @return
	 */
	List<TbItemChild> showOrderCart(List<Long> id,HttpServletRequest request);
	/**
	 * 订单三表新增
	 * @param param
	 * @param request
	 * @return
	 */
	EgoResult create(MyOrderParam param,HttpServletRequest request);
}
