package com.pointlion.sys.mvc.common.model.base;

import com.jfinal.plugin.activerecord.Model;
import com.jfinal.plugin.activerecord.IBean;

/**
 * Generated by JFinal, do not modify this file.
 */
@SuppressWarnings("serial")
public abstract class BaseGrapWebsite<M extends BaseGrapWebsite<M>> extends Model<M> implements IBean {

	public void setId(java.lang.String id) {
		set("id", id);
	}
	
	public java.lang.String getId() {
		return getStr("id");
	}

	public void setName(java.lang.String name) {
		set("name", name);
	}
	
	public java.lang.String getName() {
		return getStr("name");
	}

	public void setCreateTime(java.lang.String createTime) {
		set("create_time", createTime);
	}
	
	public java.lang.String getCreateTime() {
		return getStr("create_time");
	}

	public void setIfNeedLogin(java.lang.String ifNeedLogin) {
		set("if_need_login", ifNeedLogin);
	}
	
	public java.lang.String getIfNeedLogin() {
		return getStr("if_need_login");
	}

	public void setLoginImplClass(java.lang.String loginImplClass) {
		set("login_impl_class", loginImplClass);
	}
	
	public java.lang.String getLoginImplClass() {
		return getStr("login_impl_class");
	}

	public void setUrl(java.lang.String url) {
		set("url", url);
	}
	
	public java.lang.String getUrl() {
		return getStr("url");
	}

	public void setIfLoginNeedValicode(java.lang.String if_login_need_valicode) {
		set("if_login_need_valicode", if_login_need_valicode);
	}
	
	public java.lang.String getIfLoginNeedValicode() {
		return getStr("if_login_need_valicode");
	}
	
	public void setLoginUrl(java.lang.String login_url) {
		set("login_url", login_url);
	}
	
	public java.lang.String getLoginUrl() {
		return getStr("login_url");
	}
	
}
