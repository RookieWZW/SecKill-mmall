package com.imooc.miaosha.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.imooc.miaosha.domain.MiaoshaUser;
import com.imooc.miaosha.domain.OrderInfo;
import com.imooc.miaosha.vo.GoodsVo;

@Service
public class MiaoshaService {

	@Autowired
	private GoodsService goodsService;

	@Autowired
	private OrderService orderService;

	@Transactional
	public OrderInfo miaosha(MiaoshaUser user, GoodsVo goodsVo) {
		goodsService.reduceStock(goodsVo);

		return orderService.createOrder(user, goodsVo);
	}
}
