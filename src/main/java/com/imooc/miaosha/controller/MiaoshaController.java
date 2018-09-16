package com.imooc.miaosha.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.imooc.miaosha.domain.MiaoshaOrder;
import com.imooc.miaosha.domain.MiaoshaUser;
import com.imooc.miaosha.domain.OrderInfo;
import com.imooc.miaosha.redis.RedisService;
import com.imooc.miaosha.result.CodeMsg;
import com.imooc.miaosha.service.GoodsService;
import com.imooc.miaosha.service.MiaoshaService;
import com.imooc.miaosha.service.MiaoshaUserService;
import com.imooc.miaosha.service.OrderService;
import com.imooc.miaosha.vo.GoodsVo;

@Controller
@RequestMapping("/miaosha")
public class MiaoshaController {

	@Autowired
	private MiaoshaUserService userService;

	@Autowired
	private RedisService redisService;

	@Autowired
	private GoodsService goodsService;

	@Autowired
	private OrderService orderService;

	@Autowired
	private MiaoshaService miaoshaService;

	@RequestMapping("/do_miaosha")
	public String list(Model model, MiaoshaUser user, @RequestParam("goodsId") long goodsId) {
		model.addAttribute("user", user);

		GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);

		int stock = goods.getStockCount();

		if (stock <= 0) {
			model.addAttribute("errmg", CodeMsg.MIAO_SHA_OVER.getMsg());

			return "miaosha_fail";
		}

		MiaoshaOrder order = orderService.getMiaoshaOrderByUserIdGoodsId(user.getId(), goodsId);

		if (order != null) {
			model.addAttribute("errms", CodeMsg.REPEATE_MIAOSHA.getMsg());

			return "miaosha_fail";
		}

		OrderInfo orderInfo = miaoshaService.miaosha(user, goods);
		model.addAttribute("orderInfo", orderInfo);
		model.addAttribute("goods", goods);

		return "order_detail";

	}
}
