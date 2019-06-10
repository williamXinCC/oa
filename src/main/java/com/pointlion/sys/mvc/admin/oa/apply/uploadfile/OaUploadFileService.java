package com.pointlion.sys.mvc.admin.oa.apply.uploadfile;

import com.jfinal.aop.Before;
import com.jfinal.kit.StrKit;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Page;
import com.jfinal.plugin.activerecord.Record;
import com.jfinal.plugin.activerecord.tx.Tx;
import com.pointlion.sys.mvc.admin.sys.dataauth.SysDataAuthTranslator;
import com.pointlion.sys.mvc.common.model.OaUploadFile;

public class OaUploadFileService{
	public static final OaUploadFileService me = new OaUploadFileService();
	public static final String TABLE_NAME = OaUploadFile.tableName;
	
	/***
	 * 根据主键查询
	 */
	public OaUploadFile getById(String id){
		return OaUploadFile.dao.findById(id);
	}
	
	/***
	 * 获取分页
	 */
	public Page<Record> getPage(int pnum,int psize,String type,String name,String startTime,String endTime){
		String dataAuth = SysDataAuthTranslator.me.translator(TABLE_NAME);
		String sql  = " from "+dataAuth+" o LEFT JOIN act_hi_procinst p ON o.proc_ins_id=p.ID_  where 1=1 ";
		if(StrKit.notBlank(type)){
		sql = sql + " and o.type='"+type+"' ";
		}
		sql = sql + getQuerySql(type,name,startTime,endTime);
		return Db.paginate(pnum, psize, " select o.*,p.PROC_DEF_ID_ defid ", sql);
	}

	/***
	 * 获取分页
	 */
	public Page<Record> getPage(int pnum,int psize){
		String dataAuth = SysDataAuthTranslator.me.translator(TABLE_NAME);
		String sql  = " from " + dataAuth + " o where 1=1 ";
		return Db.paginate(pnum, psize, " select o.* ", sql);
	}

	/****
	* 获取查询sql
	* @param type
	* @param name
	* @param startTime
	* @param endTime
	* @return
	*/
	public String  getQuerySql(String type,String name,String startTime,String endTime){
		String sql = " ";
		if(StrKit.notBlank(type)){
		sql = sql + " and o.type='"+type+"' ";
		}
		if(StrKit.notBlank(name)){
		sql = sql + " and (o.applyer_name like '%"+name+"%' or o.org_name like '%"+name+"%')";
		}
		if(StrKit.notBlank(startTime)){
		sql = sql + "  and o.create_time >= '"+startTime+" 00:00:00'";
		}
		if(StrKit.notBlank(endTime)){
		sql = sql + "  and o.create_time <= '"+endTime+" 23:59:59'";
		}
		return sql;
	}
	
	/***
	 * 删除
	 * @param ids
	 */
	@Before(Tx.class)
	public void deleteByIds(String ids){
    	String idarr[] = ids.split(",");
    	for(String id : idarr){
    		OaUploadFile o = me.getById(id);
    		o.delete();
    	}
	}
	
}