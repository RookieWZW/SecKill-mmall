package com.imooc.miaosha.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.StringUtils;

import com.imooc.miaosha.util.ValidatorUtil;

public class IsMobileValidator implements ConstraintValidator<IsMobile, String> {

	private boolean required = false;

	@Override
	public void initialize(IsMobile constraintAnnotation) {
		// TODO Auto-generated method stub
		required = constraintAnnotation.required();
	}

	@Override
	public boolean isValid(String value, ConstraintValidatorContext arg1) {
		// TODO Auto-generated method stub
		if (required) {
			return ValidatorUtil.isMobile(value);
		} else {
			if (StringUtils.isEmpty(value)) {
				return true;
			} else {
				return ValidatorUtil.isMobile(value);
			}
		}
	}

}
