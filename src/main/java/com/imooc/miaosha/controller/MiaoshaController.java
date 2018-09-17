package com.imooc.miaosha.controller;

import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.imooc.miaosha.access.AccessLimit;
import com.imooc.miaosha.domain.MiaoshaOrder;
import com.imooc.miaosha.domain.MiaoshaUser;
import com.imooc.miaosha.domain.OrderInfo;
import com.imooc.miaosha.rabbitmq.MQSender;
import com.imooc.miaosha.rabbitmq.MiaoshaMessage;
import com.imooc.miaosha.redis.GoodsKey;
import com.imooc.miaosha.redis.RedisService;
import com.imooc.miaosha.result.CodeMsg;
import com.imooc.miaosha.result.Result;
import com.imooc.miaosha.service.GoodsService;
import com.imooc.miaosha.service.MiaoshaService;
import com.imooc.miaosha.service.MiaoshaUserService;
import com.imooc.miaosha.service.OrderService;
import com.imooc.miaosha.vo.GoodsVo;

@Controller
@RequestMapping("/miaosha")
public class MiaoshaController implements InitializingBean {

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

	@Autowired
	private MQSender sender;

	private HashMap<Long, Boolean> localOverMap = new HashMap<Long, Boolean>();

	@RequestMapping(value = "/do_miaosha", method = RequestMethod.POST)
	@ResponseBody
	public Result<Integer> miaosha(Model model, MiaoshaUser user, @RequestParam("goodsId") long goodsId) {
		model.addAttribute("user", user);
		if (user == null) {
			return Result.error(CodeMsg.SESSION_ERROR);
		}
		// 判断库存
		long stock = redisService.decr(GoodsKey.getMiaoshaGoodsStock, "" + goodsId);
		if (stock <= 0) {
			return Result.error(CodeMsg.MIAO_SHA_OVER);
		}
		// 判断是否已经秒杀到了
		MiaoshaOrder order = orderService.getMiaoshaOrderByUserIdGoodsId(user.getId(), goodsId);
		if (order != null) {
			return Result.error(CodeMsg.REPEATE_MIAOSHA);
		}
		MiaoshaMessage mm = new MiaoshaMessage();

		mm.setUser(user);
		mm.setGoodsId(goodsId);
		sender.sendMiaoshaMessage(mm);

		return Result.success(0);

	}

	@Override
	public void afterPropertiesSet() throws Exception {
		// TODO Auto-generated method stub

		List<GoodsVo> goodsList = goodsService.listGoodsVo();

		if (goodsList == null) {
			return;
		}
		for (GoodsVo goods : goodsList) {
			redisService.set(GoodsKey.getMiaoshaGoodsStock, "" + goods.getId(), goods.getStockCount());
			localOverMap.put(goods.getId(), false);
		}
	}

	@RequestMapping(value = "/result", method = RequestMethod.GET)
	@ResponseBody
	public Result<Long> miaoshaResult(Model model, MiaoshaUser user, @RequestParam("goodsId") long goodsId) {
		model.addAttribute("user", user);
		if (user == null) {
			return Result.error(CodeMsg.SESSION_ERROR);
		}
		long result = miaoshaService.getMiaoshaResult(user.getId(), goodsId);
		return Result.success(result);
	}

	@AccessLimit(seconds = 5, maxCount = 5, needLogin = true)
	@RequestMapping(value = "/path", method = RequestMethod.GET)
	@ResponseBody
	public Result<String> getMiaoshaPath(HttpServletRequest request, MiaoshaUser user,
			@RequestParam("goodsId") long goodsId,
			@RequestParam(value = "verifyCode", defaultValue = "0") int verifyCode) {
		if (user == null) {
			return Result.error(CodeMsg.SESSION_ERROR);
		}
		boolean check = miaoshaService.checkVerifyCode(user, goodsId, verifyCode);
		if (!check) {
			return Result.error(CodeMsg.REQUEST_ILLEGAL);
		}
		String path = miaoshaService.createMiaoshaPath(user, goodsId);
		return Result.success(path);
	}

	@RequestMapping(value = "/verifyCode", method = RequestMethod.GET)
	@ResponseBody
	public Result<String> getMiaoshaVerifyCod(HttpServletResponse response, MiaoshaUser user,
			@RequestParam("goodsId") long goodsId) {
		if (user == null) {
			return Result.error(CodeMsg.SESSION_ERROR);
		}
		try {
			BufferedImage image = miaoshaService.createVerifyCode(user, goodsId);
			OutputStream out = response.getOutputStream();
			ImageIO.write(image, "JPEG", out);
			out.flush();
			out.close();
			return null;
		} catch (Exception e) {
			e.printStackTrace();
			return Result.error(CodeMsg.MIAOSHA_FAIL);
		}
	}
}
