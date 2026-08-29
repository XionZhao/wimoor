package com.wimoor.erp.finance.service.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wimoor.common.GeneralUtil;
import com.wimoor.common.mvc.BizException;
import com.wimoor.common.user.UserInfo;
import com.wimoor.erp.finance.mapper.FinJournalAccountMapper;
import com.wimoor.erp.finance.mapper.FinanceProjectMapper;
import com.wimoor.erp.finance.pojo.entity.FinJournalAccount;
import com.wimoor.erp.finance.pojo.entity.FinanceProject;
import com.wimoor.erp.finance.service.IFinanceProjectService;

import lombok.RequiredArgsConstructor;
@Service("financeProjectService")
@RequiredArgsConstructor
public class FinanceProjectServiceImpl  extends ServiceImpl<FinanceProjectMapper,FinanceProject> implements IFinanceProjectService {
   final FinJournalAccountMapper finJournalAccountMapper;
	public List<FinanceProject> findProject(String shopid) {
		List<FinanceProject> list = this.baseMapper.findProject(shopid);
		if (list.size() > 0 && list != null) {
			return list;
		} else {
			return null;
		}
	}
	

	public Map<String, Object> saveProject(String name, Integer feetype, UserInfo user) {
		Map<String, Object> map = new HashMap<String, Object>();
		if ("商品运费".equals(name) || "采购商品".equals(name)) {
			throw new BizException("添加失败!该公司下已有该项目!");
		}
		QueryWrapper<FinanceProject> queryWrapper=new QueryWrapper<FinanceProject>();
		queryWrapper.eq("shopid", user.getCompanyid());
		queryWrapper.eq("name", name);
		List<FinanceProject> list =  this.baseMapper.selectList(queryWrapper);
		if (list.size() > 0 && list != null) {
			throw new BizException("添加失败!该公司下已有该项目!");
		} else {
			FinanceProject project = new FinanceProject();
			Date nowdate = new Date();
			project.setCreatedate(nowdate);
			project.setCreator(user.getId());
			project.setIssys(false);
			project.setName(name);
			// 费用类型默认归属供应商款项(0)，未传时取默认值
			project.setFeetype(feetype == null ? 0 : feetype);
			project.setOperator(user.getId());
			project.setOpttime(nowdate);
			project.setShopid(user.getCompanyid());
			int result =  this.baseMapper.insert(project);
			if (result > 0) {
				map.put("msg", "添加成功!");
			} else {
				map.put("msg", "添加失败!");
			}
			map.put("id", project.getId());
		}
		return map;
	}

	public Map<String, Object> updateProject(String id, String name, Boolean isdefault, Integer feetype, UserInfo user) {
		Map<String, Object> map = new HashMap<String, Object>();
		if (GeneralUtil.isEmpty(id)) {
			map.put("msg", "更新失败!");
			return map;
		}
		FinanceProject oldpro = this.baseMapper.selectById(id);
		if (oldpro == null) {
			map.put("msg", "更新失败!");
			return map;
		}
		// 仅更新isdefault的情况
		if (isdefault != null && GeneralUtil.isEmpty(name)) {
			// 系统项目不允许修改isdefault
			if (!Boolean.TRUE.equals(oldpro.getIssys())) {
				oldpro.setIsdefault(isdefault);
			}
			if (feetype != null) {
				oldpro.setFeetype(feetype);
			}
			oldpro.setOperator(user.getId());
			oldpro.setOpttime(new Date());
			int result = this.baseMapper.updateById(oldpro);
			map.put("msg", result > 0 ? "更新成功!" : "更新失败!");
			return map;
		}
		// 更新名称的情况
		if (GeneralUtil.isNotEmpty(name)) {
			if ("商品运费".equals(name) || "采购商品".equals(name)) {
				throw new BizException("添加失败!该公司下已有该项目!");
			}
			QueryWrapper<FinanceProject> queryWrapper=new QueryWrapper<FinanceProject>();
			queryWrapper.eq("shopid", user.getCompanyid());
			queryWrapper.eq("name", name);
			List<FinanceProject> list =  this.baseMapper.selectList(queryWrapper);
			if (list != null && list.size() > 0 && !list.get(0).getId().equals(id)) {
				throw new BizException( "更新失败!该公司下已有该项目!");
			}
			oldpro.setName(name);
		}
		if (isdefault != null) {
			oldpro.setIsdefault(isdefault);
		}
		if (feetype != null) {
			oldpro.setFeetype(feetype);
		}
		oldpro.setOperator(user.getId());
		oldpro.setOpttime(new Date());
		int result = this.baseMapper.updateById(oldpro);
		map.put("msg", result > 0 ? "更新成功!" : "更新失败!");
		return map;
	}

	public Map<String, Object> delProject(String id) {
		Map<String, Object> map = new HashMap<String, Object>();
		if (GeneralUtil.isNotEmpty(id)) {
			FinanceProject finpro =  this.baseMapper.selectById(id);
			QueryWrapper<FinJournalAccount> queryWrapper=new QueryWrapper<FinJournalAccount>();
			queryWrapper.eq("projectid", finpro.getId());
			List<FinJournalAccount> oldlist =  finJournalAccountMapper.selectList(queryWrapper);
			if (oldlist != null && oldlist.size() > 0) {
				throw new BizException("删除失败!该项目已存在记录！");
			} else {
				int result =  this.baseMapper.deleteById(finpro.getId());
				if (result > 0) {
					map.put("msg", "删除成功!");
					map.put("isok", true);
				} else {
					map.put("msg", "删除失败!");
					map.put("isok", false);
				}
			}
		} else {
			map.put("msg", "删除失败!");
			map.put("isok", false);
		}
		return map;
	}


}
