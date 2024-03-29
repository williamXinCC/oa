package com.pointlion.sys.mvc.common.model;

import java.util.ArrayList;
import java.util.List;

import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Page;
import com.jfinal.plugin.activerecord.Record;
import com.pointlion.sys.mvc.common.dto.ZtreeNode;
import com.pointlion.sys.mvc.common.model.base.BaseSysMenu;

/**
 * Generated by JFinal.
 */
@SuppressWarnings("serial")
public class SysMenu extends BaseSysMenu<SysMenu> {


	public static final SysMenu dao = new SysMenu();
	/***
	 * 根据主键查询
	 */
	public SysMenu getById(String id){
		return SysMenu.dao.findById(id);
	}
	/***
	 * 根据id 查询孩子
	 * @param id
	 * @return
	 */
	public List<SysMenu> getChildrenByPid(String id){
		return SysMenu.dao.find("select * from sys_menu m where m.parent_id='"+id+"' order by m.sort");
	}
	/***
	 * 根据id 查询孩子分页
	 * @param id
	 * @return
	 */
	public Page<Record> getChildrenPageByPid(int pnum,int psize, String pid){
		return Db.paginate(pnum, psize, "select m.* , o.name oname , o.url url ", " from sys_menu m LEFT JOIN sys_operate o ON m.operatorid = o.id WHERE m.parent_id='"+pid+"' order by m.sort");
	}
	/***
	 * 菜单转成ZtreeNode
	 * @param menu
	 * @return
	 */
	public ZtreeNode toZTreeNode(SysMenu menu){
		ZtreeNode node = new ZtreeNode();
		node.setId(menu.getId());
		node.setName(menu.getName());
		return node;
	}
	/***
	 * 菜单转成ZTreeNode
	 * @param menu
	 * @return
	 */
	public List<ZtreeNode> toZTreeNode(List<SysMenu> menuList,Boolean open){
		List<ZtreeNode> list = new ArrayList<ZtreeNode>();
		for(SysMenu menu : menuList){
			ZtreeNode node = toZTreeNode(menu);
			if(menu.getChildren()!=null&&menu.getChildren().size()>0){
				node.setChildren(toZTreeNode(menu.getChildren(),open));
			}
			node.setOpen(open);
			list.add(node);
		}
		return list;
	}
	/***
	 * 获取所有菜单
	 * @return
	 */
	public List<SysMenu> getAllMenu(){
		List<SysMenu> list =  getChildrenAll("#root");
		return list;
	}
	/***
	 * 递归
	 * 查询孩子
	 * @param id
	 * @return
	 */
	public List<SysMenu> getChildrenAll(String id){
		List<SysMenu> menuList =  getChildrenByPid(id);//根据id查询孩子
		for(SysMenu m : menuList){
			m.setChildren(getChildrenAll(m.getId()));
		}
		return menuList;
	}


}
