package com.hbrb.spider.service.impl;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;

import com.hbrb.exception.DataAccessException;
import com.hbrb.exception.ServiceException;
import com.hbrb.jdbc.MyDataSource;
import com.hbrb.spider.ConstantsHome;
import com.hbrb.spider.dao.DaoFactory;
import com.hbrb.spider.exception.TemplateParseException;
import com.hbrb.spider.model.task.TemplateTask;
import com.hbrb.spider.service.TemplateService;

public abstract class CachedJDBCTemplateService<T> implements TemplateService<T>{
	private Map<Integer, T> templates = new HashMap<>();

	@Override
	public <E extends TemplateTask> void cacheForTasks(List<E> tasks, Map<Integer, Integer> siteTaskIdTemplateIdMap)
			throws ServiceException {
		if (tasks.isEmpty()) {
			return;
		}

		Map<Integer, List<Integer>> templateIdSiteTaskIdMap = new HashMap<>();
		Set<Integer> ids = new HashSet<>(); // 去重用
		for (TemplateTask task : tasks) {
			int siteTaskId = task.getSiteTaskId();
			if (siteTaskId == 0) {
				// 正常不会出现 == 0
				continue;
			}
			if (!ids.add(siteTaskId)) {
				// 重复的
				continue;
			}
			if (!dependsOnTemplate(task.getType())) {
				// 不用模板的
				continue;
			}
			if (templates.get(siteTaskId) != null) {
				// 已经缓存了的
				continue;
			}

			Integer templateId = siteTaskIdTemplateIdMap.get(siteTaskId);
			if (null == templateId) {
				// 站点id和模板id一致
				if (templateIdSiteTaskIdMap.containsKey(siteTaskId)) {
					continue;
				}
				templateIdSiteTaskIdMap.put(siteTaskId, null);
			} else {
				// 站点id和模板id不一致
				List<Integer> siteTaskIds = templateIdSiteTaskIdMap.get(templateId);
				if (null != siteTaskIds) {
					siteTaskIds.add(siteTaskId);
				} else {
					siteTaskIds = new ArrayList<>();
					siteTaskIds.add(siteTaskId);
					templateIdSiteTaskIdMap.put(templateId, siteTaskIds);
				}
			}
		}

		cacheTemplates(templateIdSiteTaskIdMap);
	}
	
	abstract String getTableName();
	abstract T buildTemplate(Document doc) throws TemplateParseException;
	abstract boolean dependsOnTemplate(int type);

	@Override
	public void cacheTemplates(Map<Integer, List<Integer>> templateIdSiteTaskIdsMap)
			throws ServiceException {
		if (templateIdSiteTaskIdsMap.isEmpty()) {
			return;
		}
		StringBuilder sql = new StringBuilder("select c_id,c_content from ");
		sql.append(getTableName()).append(" where c_id in (");
		boolean isFirst = true;
		for (int templateId : templateIdSiteTaskIdsMap.keySet()) {
			if (isFirst) {
				isFirst = false;
			} else {
				sql.append(',');
			}
			sql.append(templateId);
		}
		sql.append(')');

		Map<Integer, String> freshTemplates;
		try {
			freshTemplates = DaoFactory.getTemplateDao().listTemplates(sql.toString());
			if (templateIdSiteTaskIdsMap.size() != freshTemplates.size()) {
				Set<Integer> keySet = freshTemplates.keySet();
				for (Integer templateId : templateIdSiteTaskIdsMap.keySet()) {
					if (!keySet.contains(templateId)) {
						throw new ServiceException("缺失模板 - " + templateId);
					}
				}
			}
		} catch (SQLException | DataAccessException e) {
			throw new ServiceException(sql.toString(), e);
		} finally {
			MyDataSource.releaseCurrentConnection();
		}

		for (Entry<Integer, String> freshTemplate : freshTemplates.entrySet()) {
			Integer templateId = freshTemplate.getKey();
			try {
				T template = buildTemplate(DocumentHelper.parseText(freshTemplate.getValue()));
				templates.put(templateId, template);
				List<Integer> siteTaskIds = templateIdSiteTaskIdsMap.get(templateId);
				if (null != siteTaskIds) {
					for (Integer siteTaskId : siteTaskIds) {
						templates.put(siteTaskId, template);
					}
				}
			} catch (DocumentException | TemplateParseException e) {
				throw new ServiceException("模板[" + templateId + "]解析错误", e);
			}
		}
	}

	@Override
	public T retrieveTemplate(int templateId) {
		return templates.get(templateId);
	}

	@Override
	public void cleanCache() {
		templates.clear();
	}
	
	@Override
	public void updateTemplatesByLocalNotice() throws ServiceException {
		File file = new File(ConstantsHome.USER_DIR + File.separatorChar + "update" + File.separatorChar + "template");
		if (!file.isFile()) {
			return;
		}
		List<String> ids;
		try {
			ids = FileUtils.readLines(file, Charset.defaultCharset());
		} catch (IOException e) {
			throw new ServiceException("模板更新文件读取失败", e);
		}
		if (null == ids || ids.isEmpty()) {
			return;
		}
		uncacheTemplates(ids);
		file.delete();
	}
	
	@Override
	public void uncacheTemplates(List<String> ids) {
		for (String id : ids) {
			templates.remove(Integer.valueOf(id));
		}
	}
}
