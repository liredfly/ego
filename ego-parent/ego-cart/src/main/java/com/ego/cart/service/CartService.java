package com.ego.cart.service;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.ego.commons.pojo.EgoResult;
import com.ego.commons.pojo.TbItemChild;

public interface CartService {
	/**
	 * 向购物车中添加商品
	 * @param id
	 * @param num
	 */
	void addCart(long id,int num,HttpServletRequest request);
	
	/**
	 * 显示购物车商品
	 * @return
	 */
	List<TbItemChild> showCart(HttpServletRequest request);
	/**
	 * 购物车中修改商品数量
	 * @param id
	 * @param num
	 * @param request
	 * @return
	 */
	EgoResult update(long id,int num,HttpServletRequest request);
	/**
	 * 删除购物车中商品
	 * @param id
	 * @param request
	 * @return
	 */
	EgoResult dalete(long id,HttpServletRequest request);
}
