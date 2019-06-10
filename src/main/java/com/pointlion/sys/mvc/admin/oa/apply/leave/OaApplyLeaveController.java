package com.pointlion.sys.mvc.admin.oa.apply.leave;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import com.jfinal.aop.Before;
import com.jfinal.kit.StrKit;
import com.jfinal.plugin.activerecord.Page;
import com.jfinal.plugin.activerecord.Record;
import com.pointlion.sys.interceptor.MainPageTitleInterceptor;
import com.pointlion.sys.mvc.admin.oa.workflow.WorkFlowUtil;
import com.pointlion.sys.mvc.common.base.BaseController;
import com.pointlion.sys.mvc.admin.oa.workflow.WorkFlowService;
import com.pointlion.sys.mvc.common.utils.StringUtil;
import com.pointlion.sys.mvc.common.model.OaLeave;
import com.pointlion.sys.mvc.common.model.SysUser;
import com.pointlion.sys.mvc.common.model.SysOrg;
import com.pointlion.sys.mvc.common.utils.UuidUtil;
import com.pointlion.sys.mvc.common.utils.Constants;
import com.pointlion.sys.mvc.common.utils.DateUtil;
import com.pointlion.sys.plugin.shiro.ShiroKit;


@Before(MainPageTitleInterceptor.class)
public class OaApplyLeaveController extends BaseController {

	public static final OaApplyLeaveService service = OaApplyLeaveService.me;
	public static WorkFlowService wfservice = WorkFlowService.me;


	/***
	 * 列表页面
	 */
	public void getListPage(){
		setBread("请假申请",this.getRequest().getServletPath(),"管理");
    	render("list.html");
    }

    // /admin/oa/apply/leave/getListPage

	/***
     * 获取分页数据
     **/
    public void listData() throws UnsupportedEncodingException {
		String curr = getPara("pageNumber");
		String pageSize = getPara("pageSize");
		String endTime = getPara("endTime","");
		String startTime = getPara("startTime","");
		String name = java.net.URLDecoder.decode(getPara("name",""),"UTF-8");
		String c = getPara("c","");
		if(StrKit.notBlank(c)){
			if(c.equals("myCC")){
				Page<Record> page = wfservice.getFlowCCPage(Integer.valueOf(curr),Integer.valueOf(pageSize), WorkFlowUtil.getDefkeyByModelName(OaLeave.class.getSimpleName()), ShiroKit.getUserId(), service.getQuerySql("", name, startTime, endTime));
				renderPage(page.getList(),"",page.getTotalRow());
			}else if(c.equals("myTask")){
				Page<Record> page = wfservice.getToDoPageByKey(Integer.valueOf(curr),Integer.valueOf(pageSize), WorkFlowUtil.getDefkeyByModelName(OaLeave.class.getSimpleName()), ShiroKit.getUsername(), service.getQuerySql("", name, startTime, endTime));
				renderPage(page.getList(),"",page.getTotalRow());
			}else if(c.equals("myHaveDone")){
				Page<Record> page = wfservice.getHavedonePage(Integer.valueOf(curr),Integer.valueOf(pageSize), WorkFlowUtil.getDefkeyByModelName(OaLeave.class.getSimpleName()), ShiroKit.getUsername(), service.getQuerySql("", name, startTime, endTime));
				renderPage(page.getList(),"",page.getTotalRow());
			}else{
				Page<Record> page = service.getPage(Integer.valueOf(curr),Integer.valueOf(pageSize),WorkFlowUtil.getDefkeyByModelName(OaLeave.class.getSimpleName()),name,startTime,endTime);
				renderPage(page.getList(),"",page.getTotalRow());
			}
		}else{
			Page<Record> page = service.getPage(Integer.valueOf(curr),Integer.valueOf(pageSize),"",name,startTime,endTime);
			renderPage(page.getList(),"",page.getTotalRow());
		}
    }


    /***
     * 保存
     */
    public void save(){
		String userId = ShiroKit.getUserId();
		OaLeave o = getModel(OaLeave.class);
    	if(StrKit.notBlank(o.getId())){
    		o.update();
    	}else{
    		o.setUserid(userId);
    		o.setId(UuidUtil.getUUID());
    		o.setCreateTime(DateUtil.getTime());
    		o.save();
    	}
    	renderSuccess();
    }


    /***
     * 获取编辑页面
     */
    public void getEditPage(){
    	setBread("功能名称",this.getRequest().getServletPath().substring(0,this.getRequest().getServletPath().lastIndexOf("/")+1)+"getListPage","功能名称");
    	String id = getPara("id");
    	String view = getPara("view");
		setAttr("view", view);
		//修改
    	if(StrKit.notBlank(id)){
    		OaLeave o = service.getById(id);
    		setAttr("o", o);
    		//是否是查看详情页面
    		if("detail".equals(view)){
    			setAttr("view", view);
    			if(StrKit.notBlank(o.getProcInsId())){
    				setAttr("procInsId", o.getProcInsId());
    				setAttr("defId", wfservice.getDefIdByInsId(o.getProcInsId()));
    			}
    		}
    	}else{
    		SysUser user = SysUser.dao.findById(ShiroKit.getUserId());
    		SysOrg org = SysOrg.dao.findById(user.getOrgid());
    		setAttr("user", user);
    		setAttr("org", org);
    	}
		//模型名称
    	setAttr("formModelName",StringUtil.toLowerCaseFirstOne(OaLeave.class.getSimpleName()));
    	render("edit.html");
    }


    /***
     * 删除
     * @throws Exception
     */
    public void delete() throws Exception{
		String ids = getPara("ids");
    	//执行删除
		service.deleteByIds(ids);
    	renderSuccess("删除成功!");
    }


    /***
     * 提交
     */
    public void startProcess(){
    	String id = getPara("id");
    	OaLeave o = OaLeave.dao.getById(id);
    	o.setIfSubmit(Constants.IF_SUBMIT_YES);
    	// ***
    	// 设定流程变量
		SysUser user = SysUser.dao.findById(ShiroKit.getUserId());
		SysOrg org = SysOrg.dao.findById(user.getOrgid());
		// *** 添加流程变量
		Map<String,Object> var = new HashMap<>();
		var.put("aproveSubmit","1");
		// ***
//    	String insId = wfservice.startProcess(id,o,o.getTitle(),null);
		String insId = wfservice.startProcess(id,o,o.getTitle(),var);
    	o.setProcInsId(insId);
    	o.update();
    	renderSuccess("提交成功");
    }


    /***
     * 撤回
     */
    public void callBack(){
    	String id = getPara("id");
    	try{
    		OaLeave o = OaLeave.dao.getById(id);
        	wfservice.callBack(o.getProcInsId());
        	o.setIfSubmit(Constants.IF_SUBMIT_NO);
        	o.setProcInsId("");
        	o.update();
    		renderSuccess("撤回成功");
    	}catch(Exception e){
    		e.printStackTrace();
    		renderError("撤回失败");
    	}
    }
    /**************************************************************************/
	public void setBread(String name,String url,String nowBread){
		Map<String,String> pageTitleBread = new HashMap<String,String>();
		pageTitleBread.put("pageTitle", name);
		pageTitleBread.put("url", url);
		pageTitleBread.put("nowBread", nowBread);
		this.setAttr("pageTitleBread", pageTitleBread);
	}
}