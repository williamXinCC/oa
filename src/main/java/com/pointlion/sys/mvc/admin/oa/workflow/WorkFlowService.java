package com.pointlion.sys.mvc.admin.oa.workflow;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.activiti.bpmn.converter.BpmnXMLConverter;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.editor.constants.ModelDataJsonConstants;
import org.activiti.editor.language.json.converter.BpmnJsonConverter;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.HistoryService;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.impl.RepositoryServiceImpl;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.impl.pvm.PvmActivity;
import org.activiti.engine.impl.pvm.PvmTransition;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.impl.pvm.process.ProcessDefinitionImpl;
import org.activiti.engine.impl.pvm.process.TransitionImpl;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.DeploymentBuilder;
import org.activiti.engine.repository.Model;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jfinal.aop.Before;
import com.jfinal.kit.StrKit;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Page;
import com.jfinal.plugin.activerecord.Record;
import com.jfinal.plugin.activerecord.tx.Tx;
import com.pointlion.sys.mvc.admin.oa.common.OAConstants;
import com.pointlion.sys.mvc.common.model.ActReModel;
import com.pointlion.sys.mvc.common.model.ActReProcdef;
import com.pointlion.sys.mvc.common.model.SysUser;
import com.pointlion.sys.mvc.common.model.VTasklist;
import com.pointlion.sys.mvc.common.utils.Constants;
import com.pointlion.sys.mvc.common.utils.DateUtil;
import com.pointlion.sys.plugin.activiti.ActivitiPlugin;
import com.pointlion.sys.plugin.shiro.ShiroKit;

public class WorkFlowService {
	public static final WorkFlowService me = new WorkFlowService();
	/**
	 * 创建新模型
	 * @throws UnsupportedEncodingException 
	 * */
	public void createModel(ProcessEngine pe,String name,String key) throws UnsupportedEncodingException{
		RepositoryService repositoryService = pe.getRepositoryService();
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode editorNode = objectMapper.createObjectNode();
        editorNode.put("id", "canvas");
        editorNode.put("resourceId", "canvas");
        ObjectNode stencilSetNode = objectMapper.createObjectNode();
        stencilSetNode.put("namespace", "http://b3mn.org/stencilset/bpmn2.0#");
        editorNode.put("stencilset", stencilSetNode);
        Model modelData = repositoryService.newModel();

        ObjectNode modelObjectNode = objectMapper.createObjectNode();
        modelObjectNode.put(ModelDataJsonConstants.MODEL_NAME, name);
        modelObjectNode.put(ModelDataJsonConstants.MODEL_REVISION, 1);
        String description = StringUtils.defaultString("模型描述信息");
        modelObjectNode.put(ModelDataJsonConstants.MODEL_DESCRIPTION, description);
        modelData.setMetaInfo(modelObjectNode.toString());
        modelData.setName(name);
        modelData.setKey(StringUtils.defaultString(key));

        repositoryService.saveModel(modelData);
        repositoryService.addModelEditorSource(modelData.getId(), editorNode.toString().getBytes("utf-8"));
	}
	
	/***
	 * 部署模型
	 * @param id
	 * @return
	 */
	@Before(Tx.class)
	public String deploy(String id) {
		String message = "";
		try {
			ProcessEngine pe = ActivitiPlugin.buildProcessEngine();
			RepositoryService repositoryService = pe.getRepositoryService();
			org.activiti.engine.repository.Model modelData = repositoryService.getModel(id);
			BpmnJsonConverter jsonConverter = new BpmnJsonConverter();
			JsonNode editorNode = new ObjectMapper().readTree(repositoryService.getModelEditorSource(modelData.getId()));
			BpmnModel bpmnModel = jsonConverter.convertToBpmnModel(editorNode);
			BpmnXMLConverter xmlConverter = new BpmnXMLConverter();
			byte[] bpmnBytes = xmlConverter.convertToXML(bpmnModel,"ISO-8859-1");
			
			String processName = modelData.getName();
			if (!StringUtils.endsWith(processName, ".bpmn20.xml")){
				processName += ".bpmn20.xml";
			}
//			System.out.println("========="+processName+"============"+modelData.getName());
			ByteArrayInputStream in = new ByteArrayInputStream(bpmnBytes);
			Deployment deployment = repositoryService.createDeployment().name(modelData.getName())
					.addInputStream(processName, in).deploy();
//					.addString(processName, new String(bpmnBytes)).deploy();
			
			// 设置流程分类
			List<ProcessDefinition> list = repositoryService.createProcessDefinitionQuery().deploymentId(deployment.getId()).list();
			for (ProcessDefinition processDefinition : list) {
				repositoryService.setProcessDefinitionCategory(processDefinition.getId(), modelData.getCategory());
				message = "部署成功";
			}
			if (list.size() == 0){
				message = "部署失败，没有流程。";
			}
		} catch (Exception e) {
			throw new ActivitiException("设计模型图不正确，检查模型正确性", e);
		}
		return message;
	}
	/***
	 * 删除模型
	 */
	@Before(Tx.class)
	public void deleteModel(String id){
		ActivitiPlugin.buildProcessEngine().getRepositoryService().deleteModel(id);
	}
	/***
	 * 删除模型，留下版本最大的
	 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!该方法使用完，请注释掉!!!!!!!!!!!!!!!!!!!!!!
	 * @throws XMLStreamException 
	 * @throws UnsupportedEncodingException 
	 */
	@Before(Tx.class)
	public void deleteModelRemainMaxVersion() throws UnsupportedEncodingException, XMLStreamException{
		List<ActReModel> list = ActReModel.dao.find("select * from act_re_model");//所有的模型
		for(ActReModel r:list){
			if(!"Finance".equals(r.getKey())){
				me.deleteModel(r.getId());
			}
		}
		//所有模型只留一个
//		for(ActReModel o:list){
//			ActReModel max = ActReModel.dao.findFirst("select MAX(VERSION_) VERSION_ from act_re_model where KEY_='"+o.getKey()+"'");
//			if(!o.getVersion().equals(max.getVersion())){
//				me.deleteModel(o.getId());
//			}
//		}
//		Page<ActReModel> list2 = ActReModel.dao.getCustomModelPage(1, 999999);//自定义流程的模型
//		for(ActReModel o:list2.getList()){
//				me.deleteModel(o.getId());
//		}
//		//所有模型部署
//		for(ActReModel o:list){
//			me.deploy(o.getId());
//		}
		//所有模型删掉
//		for(ActReModel o:list){
//			me.deleteModel(o.getId());
//		}
//		//所有的流程定义转化为模型
//		List<Record> list2 = Db.find("select * from act_re_procdef");
//		for(Record r:list2){
//			me.convertToModel(r.getStr("ID_"));
//		}
		//所有流程定义删除
		List<Record> list2 = Db.find("select * from act_re_procdef");
		for(Record r:list2){
			me.deleteDeployment(r.getStr("DEPLOYMENT_ID_"));
		}
	}

	/***
	 * 挂起/激活
	 */
	@Before(Tx.class)
	public String updateState(String state,String procDefId){
		if (state.equals("active")) {
			ActivitiPlugin.buildProcessEngine().getRepositoryService().activateProcessDefinitionById(procDefId, true, null);
			return "激活成功";
		} else if (state.equals("suspend")) {
			ActivitiPlugin.buildProcessEngine().getRepositoryService().suspendProcessDefinitionById(procDefId, true, null);
			return "挂起成功";
		}
		return "无操作";
	}
	
	/***
	 * 转化为模型
	 */
	@Before(Tx.class)
	public Model convertToModel(String procDefId) throws UnsupportedEncodingException, XMLStreamException {
		RepositoryService repositoryService = ActivitiPlugin.buildProcessEngine().getRepositoryService();
		ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(procDefId).singleResult();
		InputStream bpmnStream = repositoryService.getResourceAsStream(processDefinition.getDeploymentId(),
		processDefinition.getResourceName());
		XMLInputFactory xif = XMLInputFactory.newInstance();
		InputStreamReader in = new InputStreamReader(bpmnStream, "UTF-8");
		XMLStreamReader xtr = xif.createXMLStreamReader(in);
		BpmnModel bpmnModel = new BpmnXMLConverter().convertToBpmnModel(xtr);
	
		BpmnJsonConverter converter = new BpmnJsonConverter();
		ObjectNode modelNode = converter.convertToJson(bpmnModel);
		org.activiti.engine.repository.Model modelData = repositoryService.newModel();
		modelData.setKey(processDefinition.getKey());
		modelData.setName(processDefinition.getName());
		//.getDeploymentId());
		modelData.setCategory(processDefinition.getCategory());
		modelData.setDeploymentId(processDefinition.getDeploymentId());
		modelData.setVersion(Integer.parseInt(String.valueOf(repositoryService.createModelQuery().modelKey(modelData.getKey()).count()+1)));
	
		ObjectNode modelObjectNode = new ObjectMapper().createObjectNode();
		modelObjectNode.put(ModelDataJsonConstants.MODEL_NAME, processDefinition.getName());
		modelObjectNode.put(ModelDataJsonConstants.MODEL_REVISION, modelData.getVersion());
		modelObjectNode.put(ModelDataJsonConstants.MODEL_DESCRIPTION, processDefinition.getDescription());
		modelData.setMetaInfo(modelObjectNode.toString());
	
		repositoryService.saveModel(modelData);
	
		repositoryService.addModelEditorSource(modelData.getId(), modelNode.toString().getBytes("utf-8"));
	
		return modelData;
	}
	
	/**
	 * 读取资源，通过部署ID
	 * @param processDefinitionId  流程定义ID
	 * @param processInstanceId 流程实例ID
	 * @param resourceType 资源类型(xml|image)
	 */
	public InputStream resourceRead(String procDefId, String proInsId, String resType) throws Exception {
		RuntimeService runtimeService = ActivitiPlugin.buildProcessEngine().getRuntimeService();
		RepositoryService repositoryService = ActivitiPlugin.buildProcessEngine().getRepositoryService();
		if (StringUtils.isBlank(procDefId)){
			ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(proInsId).singleResult();
			procDefId = processInstance.getProcessDefinitionId();
		}
		ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(procDefId).singleResult();
		
		String resourceName = "";
		if (resType.equals("image")) {
			resourceName = processDefinition.getDiagramResourceName();
		} else if (resType.equals("xml")) {
			resourceName = processDefinition.getResourceName();
		}
		InputStream resourceAsStream = repositoryService.getResourceAsStream(processDefinition.getDeploymentId(), resourceName);
		return resourceAsStream;
	}
	/***
	 * 删除正在运行的流程
	 */
	@Before(Tx.class)
	public void deleteDeployment(String deploymentId) {
		ActivitiPlugin.buildProcessEngine().getRepositoryService().deleteDeployment(deploymentId, true);
	}
	/***
	 * 删除正在运行的流程
	 */
	@Before(Tx.class)
	public void deleteAllDeployment() {
		List<Record> list = Db.find("select * from act_re_deployment");
		for(Record r:list){
			try{
				ActivitiPlugin.buildProcessEngine().getRepositoryService().deleteDeployment(r.getStr("ID_"), true);
			}catch(Exception e){
				
			}
		}
	}
	/**
	 * 启动流程
	 * @param id
	 * @param defKey
	 * @param title
	 * @param var
	 * @return
	 * @throws Exception 
	 */
	@SuppressWarnings("rawtypes")
	public String startProcess(String id,com.jfinal.plugin.activerecord.Model m,String title,Map<String, Object> var){
		// 模板对象名
		String modelName = m.getClass().getSimpleName();
		// 模板对象key
		String defkey = WorkFlowUtil.getDefkeyByModelName(modelName);
		if(StrKit.notBlank(defkey)){
			// 启动流程
			return startProcess(id,m,defkey,title,var,ShiroKit.getUsername());
		}else{
			throw new RuntimeException("根据modelName没有找到对应的流程定义Key，请往系统字典中添加，model表单对应的流程定义Key!");
		}
	}


	/***
	 * 启动流程
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws ClassNotFoundException 
	 */
	@Before(Tx.class)
	@SuppressWarnings("rawtypes")
	public String startProcess(String id,com.jfinal.plugin.activerecord.Model m,String defKey,String title,Map<String, Object> var){
		return startProcess(id,m,defKey,title,var,ShiroKit.getUsername());
	}


	/***
	 * 启动流程,移动端，无法使用shiro---手机端使用
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws ClassNotFoundException 
	 */
	@Before(Tx.class)
	@SuppressWarnings("rawtypes")
	public String startProcess(String id,com.jfinal.plugin.activerecord.Model m,String defKey,String title,Map<String, Object> var,String username){

		if(var==null){
			var = new HashMap<String, Object>();
		}
		if(StrKit.notBlank(title)){
			var.put(OAConstants.WORKFLOW_VAR_APPLY_TITLE, title);
		}
		var = getVar(m, var, id, username, defKey);
		ProcessInstance procIns = ActivitiPlugin.buildProcessEngine().getRuntimeService().startProcessInstanceByKey(defKey,id,var);
		return procIns.getId();
	}
	
	
	/***
	 * 完成任务
	 */
	@Before(Tx.class)
	public void completeTask(String taskid,String username,Map<String,Object> var){
		completeTask(taskid,username,"",var);
	}


	/**
	 * 完成任务
	 * @author     xinchuang
	 * @param taskid :
	 * @param username :
	 * @param comment :
	 * @param var :
	 * @return : void
	 */
	@Before(Tx.class)
	public void completeTask(String taskid,String username,String comment,Map<String,Object> var){
		TaskService service = ActivitiPlugin.buildProcessEngine().getTaskService();

		VTasklist task = VTasklist.dao.getTaskRecord(taskid);
		String insid = task.getStr("INSID");
		if(var==null){
			var = new HashMap<String,Object>();
		}
		String pass = String.valueOf(var.get("pass"));
		if(comment==null){comment="";}
		//如果同意
		if(Constants.FLOW_IF_AGREE_YES.equals(pass)){
			comment = "[同意]"+comment;
			//如果不同意
		}else if(Constants.FLOW_IF_AGREE_NO.equals(pass)){
			comment = "[不同意]"+comment;
		}
		if(StrKit.notBlank(insid)&&StrKit.notBlank(comment)){
			service.addComment(taskid, insid, comment);
		}
		service.claim(taskid, username);
		service.complete(taskid, var);
		//发送短信
//		mobileMessageService.sendMessage(insid);
	}


	/***
	 * 查询某人的所有公文待办
	 * --首页使用
	 */
	public List<Record> getToDoListByKey(String tableName ,String key,String username){
		String sql = "select * from v_tasklist t ,"+tableName+" b where t.INSID=b.proc_ins_id and  t.DEFKEY='"+key+"'";
		if(StrKit.notBlank(username)){
			sql = sql + " and (t.ASSIGNEE='"+username+"' or t.CANDIDATE='"+username+"')";
		}
		return Db.find(sql);
	}


	/***
	 * 查询所有待办
	 */
	public List<Record> getToDoListByUsername(String username){
		String sql = "select u.UserName,tt.* from (SELECT e.BUSINESS_KEY_ businessId,v.TEXT_ username,t.* FROM v_tasklist t,act_ru_execution e, act_ru_variable v WHERE	e.PROC_INST_ID_ = t.INSID AND t.INSID = v.PROC_INST_ID_ AND v.NAME_ = '"+OAConstants.WORKFLOW_VAR_APPLY_USERNAME+"'";
		if(StrKit.notBlank(username)){
			sql = sql + " and (t.ASSIGNEE='"+username+"' or t.CANDIDATE='"+username+"')";
		}
		sql = sql + " ORDER BY t.DEFNAME) tt LEFT JOIN sys_user u on u.username=tt.username ";
		return Db.find(sql);
	}


//	/***
//	 * 查询某人的待办条目
//	 * --管理页面使用
//	 */
//	public Page<Record> getToDoPageByKey(int pnum,int psize,String tableName,String key ,String username){
//		String sql = " from v_tasklist t ,"+tableName+" b where t.INSID=b.proc_ins_id ";
//		if(StrKit.notBlank(key)){
//			sql = sql + "and  t.DEFKEY='"+key+"'";
//		}
//		if(StrKit.notBlank(username)){
//			sql = sql + " and (t.ASSIGNEE='"+username+"' or t.CANDIDATE='"+username+"')";
//		}
//		return Db.paginate(pnum, psize, "select * ",sql);
//	}

	/***
	 * 查询某人的待办条目
	 * --管理页面使用
	 */
	public Page<Record> getToDoPageByKey(int pnum,int psize,String key ,String username,String sqlEXT){
		String tableName = WorkFlowUtil.getTablenameByDefkey(key);
		String sql = " from v_tasklist t ,"+tableName+" o,act_hi_procinst p where o.proc_ins_id=p.ID_ and t.INSID=o.proc_ins_id ";
		if(StrKit.notBlank(key)){
			sql = sql + "and  t.DEFKEY='"+key+"'";
		}
		if(StrKit.notBlank(username)){
			sql = sql + " and (t.ASSIGNEE='"+username+"' or t.CANDIDATE='"+username+"')";
		}
		if(StrKit.notBlank(sqlEXT)){
			sql = sql + sqlEXT;
		}
		sql = sql +" order by o.create_time desc";
		return Db.paginate(pnum, psize, "select *,p.PROC_DEF_ID_ defid  ",sql);
	}


	/***
	 * 获取已办分页
	 * --首页和管理页面使用
	 */
	public Page<Record> getHavedonePage(int pnum,int psize,String defkey,String username,String sqlEXT){
		String tableName = WorkFlowUtil.getTablenameByDefkey(defkey);
		String sql = "FROM "+tableName+" o, ( SELECT DISTINCT p.BUSINESS_KEY_, d.ID_ defid FROM act_hi_taskinst t, act_hi_procinst p, act_re_procdef d WHERE t.ASSIGNEE_='"+username+"' AND p.PROC_DEF_ID_ = d.ID_ AND d.KEY_ = '"+defkey+"' AND t.END_TIME_ is not NULL AND t.DELETE_REASON_='completed' AND t.PROC_INST_ID_ = p.ID_) tt WHERE tt.BUSINESS_KEY_ = o.id ";
//		String sql  = " from "+tableName+" o , (select BUSINESS_KEY_,d.ID_ defid from act_hi_identitylink i,act_hi_procinst p,act_re_procdef d where i.TYPE_='participant' and p.ID_=i.PROC_INST_ID_ and p.PROC_DEF_ID_=d.ID_ and d.KEY_='"+defkey+"' and i.USER_ID_='"+username+"') tt where tt.BUSINESS_KEY_=o.id";
		if(StrKit.notBlank(sqlEXT)){
			sql = sql + sqlEXT;
		}
		sql = sql +" order by o.create_time desc";
		return Db.paginate(pnum, psize, " select o.*,defid ", sql);
	}
	/***
	 * 查询抄送给我的
	 * --管理页面使用
	 */
	public Page<Record> getFlowCCPage(int pnum,int psize,String key ,String userid,String sqlEXT){
		String tableName = WorkFlowUtil.getTablenameByDefkey(key);
		String sql = " from "+tableName+" o, oa_flow_carbon_c  cc, act_hi_procinst p  where o.proc_ins_id=p.ID_ and cc.business_id = o.id and cc.user_id='"+userid+"' and cc.defkey='"+key+"' ";
		if(StrKit.notBlank(sqlEXT)){
			sql = sql + sqlEXT;
		}
		sql = sql +" order by o.create_time desc";
		return Db.paginate(pnum, psize, "select o.*,p.PROC_DEF_ID_ defid  ",sql);
	}
	/***
	 * 获取流转历史
	 */
	public List<Record> getHisTaskList(String insid){
		//oracle版本遇到comment为event问题，用如下sql解决
		//select  t.NAME_ taskName,t.ASSIGNEE_ assignee,t.EXECUTION_ID_ exeId,t.ID_ taskId,t.END_TIME_ endTime,c.MESSAGE_ message from act_hi_taskinst t LEFT JOIN (select * from ACT_HI_COMMENT dd where dd.TYPE_!='event')c  ON c.TASK_ID_=t.ID_ where t.proc_inst_id_ = '18fb7c365c99425da16cfdb2c1a643f9' order by t.end_time_ asc
		List<Record> taskList = Db.find("select  t.NAME_ taskName,t.ASSIGNEE_ assignee,t.EXECUTION_ID_ exeId,t.ID_ taskId,t.END_TIME_ endTime,c.MESSAGE_ message from act_hi_taskinst t LEFT JOIN act_hi_comment c  ON c.TASK_ID_=t.ID_ where t.proc_inst_id_ = '"+insid+"' order by t.end_time_ is null,t.end_time_ asc");
		for(Record r : taskList){
			String assignee = r.getStr("assignee");
			if(StrKit.notBlank(assignee)){
				SysUser user  = SysUser.dao.getByUsername(assignee);
				if(user!=null){
					r.set("assignee", user.getName()+"["+assignee+"]");
				}else{
					r.set("assignee", "无用户["+assignee+"]");
				}
			}else{
				List<VTasklist> tl = VTasklist.dao.find("select * from v_tasklist t where t.TASKID='"+r.getStr("taskId")+"'");
				List<String> cl = new ArrayList<String>();
				for(VTasklist t : tl){
					String c = t.getCANDIDATE();
					if(StrKit.notBlank(c)){
						SysUser u = SysUser.dao.getByUsername(c);
						if(u!=null){
							cl.add(u.getName()+"["+c+"]");
						}else{
							cl.add("无用户["+c+"]");
						}
					}else{
						cl.add("无用户["+c+"]");
					}
					
				}
				r.set("assignee", StringUtils.join(cl,","));
			}
			String message = r.getStr("message");
			if(message==null){
				r.set("message","");
			}
			String endtime = r.getStr("endTime");
			r.set("endTime", StrKit.notBlank(endtime)?endtime.substring(0,endtime.indexOf(".")):"");
		}
		return taskList;
//		return Db.find("SELECT t.assignee_,	u.name,	t.name_,	t.end_time_,	c.message_ FROM	sys_user u ,	act_hi_taskinst t LEFT JOIN act_hi_comment c ON t.id_ = c.task_id_ where u.username=t.ASSIGNEE_ AND t.proc_inst_id_ = '"+insid+"' ORDER BY t.end_time_ desc ");
	}
	
	/***
	 * 获取流程经办人数据
	 */
	public List<Record> getHisTaskParter(String insid){
		return Db.find("select i.*,u.name from act_hi_identitylink i,sys_user u where u.username=i.USER_ID_ AND PROC_INST_ID_='"+insid+"'");
	}
	
	/***
	 * 根据流程实例id获取流程定义ID
	 * @param insid
	 * @return
	 */
	public String getDefIdByInsId(String insid){
		Record proc = Db.findFirst("select * from act_hi_procinst p where p.PROC_INST_ID_=?",insid);
		return proc.getStr("PROC_DEF_ID_");
	}
	
	/***
	 * 根据流程defkey获取流程定义名称
	 * @param defKey
	 * @return
	 */
	public String getDefNameByDefKey(String defKey){
		ActReProcdef def = ActReProcdef.dao.findFirst("select * from act_re_procdef p where p.KEY_='"+defKey+"' ORDER BY VERSION_ DESC");
		if(def!=null){
			return def.getName();
		}else{
			return "";
		}
	}
	
	/***
	 * 删除流程实例
	 */
	public void deleteIns(String procid){
		ProcessEngine pe = ActivitiPlugin.buildProcessEngine();
		Record run = Db.findFirst("select * from act_ru_execution t where t.PROC_INST_ID_='"+procid+"'");
		if(run!=null){
//			try{
				pe.getRuntimeService().deleteProcessInstance(procid, "删除流程实例");
//			}catch(Exception e){
//				e.printStackTrace();
//			}
		}
		Record his = Db.findFirst("select * from act_hi_procinst t where t.PROC_INST_ID_='"+procid+"'");
		if(his!=null){
//			try{
				pe.getHistoryService().deleteHistoricProcessInstance(procid);
//			}catch(Exception e){
//				e.printStackTrace();
//			}
		}
	}
	
	/***
	 * 撤回流程
	 */
	public void callBack(String procid){
		deleteIns(procid);
	}
	public void updateIfCompleteAndIfAgree(String tableName,String pass,String id){
		String ifAgree = Constants.IF_AGREE_NO;//不同意
		if(Constants.SUBMIT_PASS_YES.equals(pass)){//同意
			ifAgree = Constants.IF_AGREE_YES;
		}
		Db.update("UPDATE "+tableName+" SET if_complete = '"+Constants.IF_COMPLETE_YES+"' WHERE id = '"+id+"' ");//更新为已经完成
		Db.update("UPDATE "+tableName+" SET if_agree = '"+ifAgree+"' WHERE id = '"+id+"' ");//是否同意
	}
	
	
	//---------------- mobile  zhouzhongyan --------------------
	/**
	 * 获取待办list
	 * @author 28995
	 * @param username
	 * @return
	 */
	/**
	 * 获取待办数目
	 * @author 28995
	 * @param username
	 * @return
	 */
	public int getTodoNum(String username){
		String sql = "select count(1) NUM from v_tasklist where 1=1 ";
		if(StrKit.notBlank(username)){
			sql = sql + " and ASSIGNEE='"+username+"' or CANDIDATE='"+username+"'";
		}
		Record result =  Db.findFirst(sql);
		return Integer.parseInt(result.getStr("NUM"));
	}

	public int getHaveDoneNum(String username){
		String sql = "select COUNT(1) NUM FROM oa_contract_apply o, ( SELECT DISTINCT p.BUSINESS_KEY_, d.ID_ defid FROM act_hi_taskinst t,"
				+ " act_hi_procinst p, act_re_procdef d WHERE t.ASSIGNEE_='"+username+"' AND p.PROC_DEF_ID_ = d.ID_ AND t.END_TIME_ is not NULL"
				+ " AND t.DELETE_REASON_='completed' AND t.PROC_INST_ID_ = p.ID_) tt WHERE tt.BUSINESS_KEY_ = o.id  order by o.create_time desc";

		Record re = Db.findFirst(sql);
		return Integer.parseInt(re.getStr("NUM"));
	}
	
	/***
	 * 流程取回
	 */
	public void callBackTask(String taskId){
		ProcessEngine pe =  ActivitiPlugin.buildProcessEngine();
		HistoryService historyService = pe.getHistoryService();
		RuntimeService runTimeService = pe.getRuntimeService();
		RepositoryService repositoryService = pe.getRepositoryService();
		TaskService taskService = pe.getTaskService();
		try {
            Map<String, Object> variables;
            // 取得当前任务
            HistoricTaskInstance currTask =historyService.createHistoricTaskInstanceQuery().taskId(taskId).singleResult();
            // 取得流程实例
            ProcessInstance instance = runTimeService.createProcessInstanceQuery().processInstanceId(currTask.getProcessInstanceId()).singleResult();
            if (instance == null) {
//                log.error("流程已经结束");
            }
            variables=instance.getProcessVariables();
            // 取得流程定义
            ProcessDefinitionEntity definition = (ProcessDefinitionEntity) ((RepositoryServiceImpl) repositoryService).getDeployedProcessDefinition(currTask.getProcessDefinitionId());
            if (definition == null) {
//                log.error("流程定义未找到");
            }
            // 取得下一步活动
            ActivityImpl currActivity = ((ProcessDefinitionImpl) definition).findActivity(currTask.getTaskDefinitionKey());
            List<PvmTransition> nextTransitionList = currActivity.getOutgoingTransitions();
            for (PvmTransition nextTransition : nextTransitionList) {
                PvmActivity nextActivity = nextTransition.getDestination();
                List<HistoricTaskInstance> completeTasks = historyService.createHistoricTaskInstanceQuery().processInstanceId(instance.getId()).taskDefinitionKey(nextActivity.getId()).finished().list();
                int finished = completeTasks.size();
                if (finished > 0) {
//                    log.error("存在已经完成的下一步，流程不能取回");
                }
                List<Task> nextTasks = taskService.createTaskQuery().processInstanceId(instance.getId()).taskDefinitionKey(nextActivity.getId()).list();
                for (Task nextTask : nextTasks) {
                    //取活动，清除活动方向
                    List<PvmTransition> oriPvmTransitionList = new ArrayList<PvmTransition>();
                    List<PvmTransition> pvmTransitionList = nextActivity.getOutgoingTransitions();
                    for (PvmTransition pvmTransition : pvmTransitionList) {
                        oriPvmTransitionList.add(pvmTransition);
                    }
                    pvmTransitionList.clear();
                    //建立新方向
                    ActivityImpl nextActivityImpl = ((ProcessDefinitionImpl) definition).findActivity(nextTask.getTaskDefinitionKey());
                    TransitionImpl newTransition = nextActivityImpl.createOutgoingTransition();
                    newTransition.setDestination(currActivity);
                    //完成任务
                    taskService.complete(nextTask.getId(), variables);
                    historyService.deleteHistoricTaskInstance(nextTask.getId());
                    //恢复方向
                    currActivity.getIncomingTransitions().remove(newTransition);
                    List<PvmTransition> pvmTList = nextActivity.getOutgoingTransitions();
                    pvmTList.clear();
                    for (PvmTransition pvmTransition : oriPvmTransitionList) {
                        pvmTransitionList.add(pvmTransition);
                    }
                }
            }
            historyService.deleteHistoricTaskInstance(currTask.getId());
        } catch (Exception e) {
        	e.printStackTrace();
        }
	}
	
	
	/***
	 * 组织必要的流程变量
	 * @param var
	 * @param username
	 * @return
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	@SuppressWarnings("rawtypes")
	public Map<String, Object> getVar(com.jfinal.plugin.activerecord.Model m ,Map<String, Object> var,String id,String username,String defkey){
		var.put(OAConstants.WORKFLOW_VAR_APPLY_USERNAME, username);
		var.put(OAConstants.WORKFLOW_VAR_PROCESS_INSTANCE_START_TIME, DateUtil.getTime());
		var.put(OAConstants.WORKFLOW_VAR_APPLY_BUSINESS_CLASSNAME, m.getClass().getSimpleName());
		return var;
	}
}
