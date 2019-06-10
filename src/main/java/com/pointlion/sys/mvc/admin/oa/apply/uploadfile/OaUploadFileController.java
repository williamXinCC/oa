package com.pointlion.sys.mvc.admin.oa.apply.uploadfile;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.jfinal.aop.Before;
import com.jfinal.kit.StrKit;
import com.jfinal.plugin.activerecord.Page;
import com.jfinal.plugin.activerecord.Record;
import com.jfinal.upload.UploadFile;
import com.pointlion.sys.interceptor.MainPageTitleInterceptor;
import com.pointlion.sys.mvc.common.base.BaseController;
import com.pointlion.sys.mvc.admin.oa.workflow.WorkFlowService;
import com.pointlion.sys.mvc.common.model.OaUploadFile;
import com.pointlion.sys.mvc.common.model.SysAttachment;
import com.pointlion.sys.mvc.common.model.SysUser;
import com.pointlion.sys.mvc.common.utils.UuidUtil;
import com.pointlion.sys.mvc.common.utils.DateUtil;
import com.pointlion.sys.plugin.shiro.ShiroKit;



@Before(MainPageTitleInterceptor.class)
public class OaUploadFileController extends BaseController {

	public static final OaUploadFileService service = OaUploadFileService.me;
	public static WorkFlowService wfservice = WorkFlowService.me;
	/***
	 * 列表页面
	 */
	public void getListPage(){
		setBread("稿件审核",this.getRequest().getServletPath(),"管理");
    	render("list.html");
    }

	/***
	 * 稿件上传
	 */
	public void getBusinessUploadListPage(){
		String view = getPara("view","");
		setAttr("view", view);
		render("businessUploadList.html");
	}

	/***
	 * 稿件上传 完成
	 */
	public void manuscriptUpload(){
		String moduelFrom = getPara("moduelFrom","manuscript");//来源
		String key = getPara("key","manuscript");
		String path = "/attachment/"+moduelFrom+"/"+key+"/"+DateUtil.getYear()+"/"+DateUtil.getMonth()+"/"+DateUtil.getDay();//保存路径
		UploadFile file = getFile("file",path);
		String savePath = file.getUploadPath();//实际保存的路径
		String bathPath = savePath.replace(path, "");//根路径upload目录
		String filename = file.getOriginalFileName();//文件实际名字
		String stuf = filename.substring(filename.lastIndexOf(".")+1);//扩展名
		String newUrl = path+"/"+UuidUtil.getUUID()+"."+stuf;//新文件相对路径

		// TODO
		bathPath = bathPath.replace("\\","/");
		//文件实际存储路径
		String newRealFileUrl = bathPath + newUrl;
		//文件重命名
		file.getFile().renameTo(new File(newRealFileUrl));
		OaUploadFile attachment = new OaUploadFile();
		attachment.setId(UuidUtil.getUUID());
		attachment.setUrl(newUrl);
		attachment.setRealUrl(newRealFileUrl);
		SysUser user = SysUser.dao.getById(ShiroKit.getUserId());
		attachment.setCreateUserId(user.getId());
		attachment.setCreateUserName(user.getName());
		attachment.setCreateTime(DateUtil.getTime());
		attachment.setFileSuffix(stuf);
		attachment.setFileName(filename);
		attachment.save();
		renderSuccess("上传成功");
	}

	/***
	 * 下载文件  完成
	 */
	public void downloadFile(){
		String id = getPara("id");
		OaUploadFile oaUploadFile = OaUploadFile.dao.getById(id);
		String fileUrl = oaUploadFile.getUrl();
		String baseUrl = this.getRequest().getSession().getServletContext().getRealPath("");
		File file = new File(baseUrl+"/upload"+fileUrl);
		renderFile(file);
	}


	/***
     * 获取分页数据
     **/
    public void listData(){
    	String curr = getPara("pageNumber");
    	String pageSize = getPara("pageSize");
    	Page<Record> page = service.getPage(Integer.valueOf(curr),Integer.valueOf(pageSize));
    	renderPage(page.getList(),"",page.getTotalRow());
    }


    /***
     * 保存
     */
    public void save(){
    	OaUploadFile o = getModel(OaUploadFile.class);
    	if(StrKit.notBlank(o.getId())){
    		o.update();
    	}else{
    		o.setId(UuidUtil.getUUID());
    		o.setCreateTime(DateUtil.getTime());
    		o.save();
    	}
    	renderSuccess();
    }


    /***
     * 获取编辑页面
     */
//    public void getEditPage(){
//    	setBread("功能名称",this.getRequest().getServletPath().substring(0,this.getRequest().getServletPath().lastIndexOf("/")+1)+"getListPage","功能名称");
//    	String id = getPara("id");
//    	String view = getPara("view");
//		setAttr("view", view);
//    	if(StrKit.notBlank(id)){//修改
//    		OaUploadFile o = service.getById(id);
//    		setAttr("o", o);
//    		//是否是查看详情页面
//    		if("detail".equals(view)){
//    			setAttr("view", view);
//    			if(StrKit.notBlank(o.getProcInsId())){
//    				setAttr("procInsId", o.getProcInsId());
//    				setAttr("defId", wfservice.getDefIdByInsId(o.getProcInsId()));
//    			}
//    		}
//    	}else{
//    		SysUser user = SysUser.dao.findById(ShiroKit.getUserId());
//    		SysOrg org = SysOrg.dao.findById(user.getOrgid());
//    		setAttr("user", user);
//    		setAttr("org", org);
//    	}
//    	setAttr("formModelName",StringUtil.toLowerCaseFirstOne(OaUploadFile.class.getSimpleName()));//模型名称
//    	render("edit.html");
//    }


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


//    /***
//     * 提交
//     */
//    public void startProcess(){
//    	String id = getPara("id");
//    	OaUploadFile o = OaUploadFile.dao.getById(id);
//    	o.setIfSubmit(Constants.IF_SUBMIT_YES);
//    	String insId = wfservice.startProcess(id, OAConstants.DEFKEY_PROJECT_CHANGE_MEMBER,o.getTitle(),null);
//    	o.setProcInsId(insId);
//    	o.update();
//    	renderSuccess("提交成功");
//    }


    /***
     * 撤回
     */
//    public void callBack(){
//    	String id = getPara("id");
//    	try{
//    		OaUploadFile o = OaUploadFile.dao.getById(id);
//        	wfservice.callBack(o.getProcInsId());
//        	o.setIfSubmit(Constants.IF_SUBMIT_NO);
//        	o.setProcInsId("");
//        	o.update();
//    		renderSuccess("撤回成功");
//    	}catch(Exception e){
//    		e.printStackTrace();
//    		renderError("撤回失败");
//    	}
//    }
    /**************************************************************************/
	public void setBread(String name,String url,String nowBread){
		Map<String,String> pageTitleBread = new HashMap<String,String>();
		pageTitleBread.put("pageTitle", name);
		pageTitleBread.put("url", url);
		pageTitleBread.put("nowBread", nowBread);
		this.setAttr("pageTitleBread", pageTitleBread);
	}
}