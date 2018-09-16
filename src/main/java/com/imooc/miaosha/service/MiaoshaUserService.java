package com.imooc.miaosha.service;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.imooc.miaosha.dao.MiaoshaUserDao;
import com.imooc.miaosha.domain.MiaoshaUser;
import com.imooc.miaosha.exception.GlobalException;
import com.imooc.miaosha.redis.MiaoshaUserKey;
import com.imooc.miaosha.redis.RedisService;
import com.imooc.miaosha.result.CodeMsg;
import com.imooc.miaosha.util.MD5Util;
import com.imooc.miaosha.util.UUIDUtil;
import com.imooc.miaosha.vo.LoginVo;

@Service
public class MiaoshaUserService {

	public static final String COOKIE_NAME_TOKEN = "token";

	@Autowired
	private MiaoshaUserDao miaoshaUserDao;

	@Autowired
	private RedisService redisService;

	public MiaoshaUser getById(long id) {
		return miaoshaUserDao.getById(id);
	}

	public MiaoshaUser getByToken(HttpServletResponse response, String token) {
		if (StringUtils.isEmpty(token)) {
			return null;
		}

		MiaoshaUser user = redisService.get(MiaoshaUserKey.token, token, MiaoshaUser.class);
		if (user != null) {
			addCookie(response, token, user);
		}

		return user;

	}

	public boolean login(HttpServletResponse response, LoginVo loginVo) {

		if (loginVo == null) {
			throw new GlobalException(CodeMsg.SERVER_ERROR);
		}

		String mobile = loginVo.getMobile();
		String formPass = loginVo.getPassword();

		MiaoshaUser user = getById(Long.parseLong(mobile));

		if (user == null) {
			throw new GlobalException(CodeMsg.MOBILE_NOT_EXIST);
		}

		String dbPass = user.getPassword();
		String saltDB = user.getSalt();
		String calcPass = MD5Util.formPassToDBPass(formPass, saltDB);
		if (!calcPass.equals(dbPass)) {
			throw new GlobalException(CodeMsg.PASSWORD_ERROR);
		}

		String token = UUIDUtil.uuid();

		addCookie(response, token, user);

		return true;

	}

	public void addCookie(HttpServletResponse response, String token, MiaoshaUser user) {
		redisService.set(MiaoshaUserKey.token, token, user);

		Cookie cookie = new Cookie(COOKIE_NAME_TOKEN, token);
		cookie.setMaxAge(MiaoshaUserKey.token.expireSeconds());

		cookie.setPath("/");

		response.addCookie(cookie);
	}
}
