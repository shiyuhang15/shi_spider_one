package com.hbrb.spider.pageprocessor;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.hbrb.exception.LogicError;
import com.hbrb.exception.ServiceException;
import com.hbrb.spider.ConstantsHome;
import com.hbrb.spider.Spider;
import com.hbrb.spider.exception.PageProcessException;
import com.hbrb.spider.model.Region;
import com.hbrb.spider.model.article.ArticleImg;
import com.hbrb.spider.model.article.FieldName;
import com.hbrb.spider.model.article.NewsArticle;
import com.hbrb.spider.model.article.SourceType;
import com.hbrb.spider.model.page.HtmlPage;
import com.hbrb.spider.model.task.TargetTask;
import com.hbrb.spider.model.template.ArticleField;
import com.hbrb.spider.model.template.FieldExtractRule;
import com.hbrb.spider.model.template.PageTemplate;
import com.hbrb.spider.pipeline.FilePipeline;
import com.hbrb.spider.service.FillerService;
import com.hbrb.spider.service.RegionService;
import com.hbrb.spider.service.ServiceFactory;
import com.hbrb.spider.service.TemplateService;
import com.hbrb.util.ProcessUtils;
import com.hbrb.util.TaskUtils;

public class TargetPageProcessor extends HtmlPageProcessor<TargetTask> {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TargetPageProcessor.class);
	private static final org.slf4j.Logger articlesLogger = org.slf4j.LoggerFactory
			.getLogger("pageprocessor.ArticlesLogger");
	private static final org.slf4j.Logger targetLogger = org.slf4j.LoggerFactory
			.getLogger("com.cmcc.yuqing.collector.statistics.target");
	private static final TemplateService<PageTemplate> templateService = ServiceFactory.getPageTemplateService();
	private static final FillerService fillerService = ServiceFactory.getFillerService();
	private static final RegionService regionService = ServiceFactory.getRegionService();
	private final FilePipeline pipeline;
	private final static Pattern PA_PORT_80_URL = Pattern.compile("^https?://[^/:]+(:80)([?/#]|$)");

	public TargetPageProcessor(FilePipeline pipeline) {
		this.pipeline = pipeline;
	}
	
	private void onExtractFailed(String log, String fieldName, TargetTask task) {
		logger.warn(log, fieldName, task.getUrl());
		if (FieldName.TITLE == fieldName
				|| (task.getSourceType() != SourceType.BBS && FieldName.CONTENT == fieldName)) {
			targetLogger.info("{}\t{}\t{}\t{}\t{}", task.getSiteTaskId(), 0, 0, 0, 1);
		}
	}
	
	private NewsArticle createNewsArticle(int siteTaskId, String url, int sourceType, boolean isHofp, Region region) {
		NewsArticle article = new NewsArticle(sourceType);
		article.setUrl(url);
		article.setCreateTime(new Date());
		
		if (null != region) {
			List<String> sourceRegion = new ArrayList<>(3);
			sourceRegion.add(region.getProvince());
			String city = region.getCity();
			if (null != city) {
				sourceRegion.add(city);
				String county = region.getCounty();
				if (null != county) {
					sourceRegion.add(county);
				}
			}
			article.setSourceRegion(sourceRegion);
			article.addModule(1);
		}
		if (isHofp) {
			Integer level = fillerService.retrieveSiteLevel(siteTaskId);
			if (null == level) {
				logger.warn("level is null");
			}
			article.setLevel(level);
			article.addModule(2);
		}
		if (1 == siteTaskId) {
			article.addModule(3);
		}
		return article;
	}
	
	private String extractField(Element ele, FieldExtractRule rule){
		switch (rule.getMethod()) {
		case 0:
			return ele.ownText().trim();
		case 1:
			return ele.text().trim();
		case 2:
			return ele.outerHtml();
		default:
			throw new LogicError("未知抽取方式 - " + rule.getMethod());
		}
	}
	
	private boolean isRedirected(String taskUrl, String currentUrl) {
		if (currentUrl.equalsIgnoreCase(taskUrl)) {
			// 相等
			return false;
		} else {
			// 不相等
			Matcher ma = PA_PORT_80_URL.matcher(taskUrl);
			if (ma.find()) {
				// 带80端口号
				int start = ma.start(1);
				if (currentUrl.equalsIgnoreCase(taskUrl.substring(0, start) + taskUrl.substring(start + 3))) {
					// 去掉80端口号后相等
					return false;
				} else {
					// 去掉80端口号后不相等
					return true;
				}
			} else {
				// 不带80端口号
				return true;
			}
		}
	}

	@Override
	public void process(HtmlPage<TargetTask> page) throws PageProcessException {
		TargetTask currentTask = page.getRequestTask();
		String taskUrl = currentTask.getUrl();
		String currentUrl = page.getDocument().location();
		if (isRedirected(taskUrl, currentUrl)) {
			logger.warn("redirect to - {}", currentUrl);
			return;
		}

		// 获取所匹配的模板
		int siteTaskId = currentTask.getSiteTaskId();
		PageTemplate matchedTemplate = null;
		if (TaskUtils.dependsOnPageTemplate(currentTask.getType())) {
			matchedTemplate = templateService.retrieveTemplate(siteTaskId);
			if (null == matchedTemplate) {
				throw new LogicError("没有缓存模板 - " + siteTaskId);
			}
		}

		int sourceType = currentTask.getSourceType();
		Region region = currentTask.getRegion();
		if (null == region) {
			region = fillerService.retrieveSiteRegion(siteTaskId);
		}
		NewsArticle article = createNewsArticle(siteTaskId, taskUrl, sourceType, currentTask.isHofp(), region);
		if (siteTaskId > 0) {
			try {
				article.setSourceName(fillerService.retrieveSiteName(siteTaskId));
			} catch (ServiceException e) {
				throw new LogicError("站点任务[" + siteTaskId + "]名称没有缓存", e);
			}
		} else {
			article.setSourceName(null);
		}

		Document doc = page.getDocument();
		// 抽取图片
		if (null != matchedTemplate) {
			FieldExtractRule[] imgsExtractRules = matchedTemplate.getImgsExtractRules();
			if (null != imgsExtractRules && imgsExtractRules.length != 0) {
				List<ArticleImg> imgs = new ArrayList<>();
				for (FieldExtractRule rule : imgsExtractRules) {
					Elements eles = doc.select(rule.getLocation());
					int size = eles.size();
					int index = rule.getIndex();
					List<Element> selNodes = null;
					if (index >= size) {
						logger.warn("field[{}] rule[{}] index[{}] >= size[{}]", FieldName.IMGS, rule.getLocation(),
								index, size);
					} else if (index >= 0) {
						selNodes = new ArrayList<>(1);
						selNodes.add(eles.get(index));
					} else if (size > 0) {
						selNodes = eles;
					}
					if (null != selNodes) {
						for (Element sel : selNodes) {
							Elements imgEles = sel.getElementsByTag("img");
							if (!imgEles.isEmpty()) {
								String imgSrc = imgEles.get(0).attr("src");
								if (!imgSrc.isEmpty()) {
									ArticleImg img = new ArticleImg();
									imgs.add(img);
									img.setSrc(imgSrc);
									String desc = sel.text();
									if (!desc.isEmpty()) {
										img.setDesc(desc);
									}
								}
							}
						}
					}
				}
				if (!imgs.isEmpty()) {
					article.setImgs(imgs);
				}
			}
		}
		
		// 先用自动化抽取
//		article.setTitle(currentTask.getTitle());
		if (SourceType.BBS == sourceType) {
			ProcessUtils.extractPost(doc, article);
		} else if (SourceType.PAPER == sourceType) {
			ProcessUtils.extractPaper(doc, article);
		}
		ProcessUtils.extractArticle(doc, article);
		// 没有自动抽取到标题的话，使用导航页上文章链接的标题
		if (null == article.getTitle()) {
			article.setTitle(currentTask.getTitle());
		}
		
		// 判断是否配了抽取规则
		boolean titleConfiged = false, contentConfiged = false, pubTimeConfiged = false, pubSourceConfiged = false,
				keywordsConfiged = false, descriptionConfiged = false, authorsConfiged = false,
				pretitleConfiged = false, subtitleConfiged = false, channelConfiged = false;
		if (null != matchedTemplate && null != matchedTemplate.getFields() && matchedTemplate.getFields().length != 0) {
			// 配了就用规则抽取
			ArticleField[] fields = matchedTemplate.getFields();
			for (ArticleField field : fields) {
				String name = field.getName();
				FieldExtractRule[] rules = field.getExtractRules();
				boolean disableAuto = field.isDisableAuto();
				boolean haveRules = false;
				String result = null;
				if (null != rules && rules.length != 0) {
					haveRules = true;
					// 判断这些字段有没有配抽取规则
					switch (name) {
					case FieldName.TITLE:
						titleConfiged = true;
						break;
					case FieldName.CONTENT:
						contentConfiged = true;
						break;
					case FieldName.PUBTIME:
						pubTimeConfiged = true;
						break;
					case FieldName.PUBSOURCE:
						pubSourceConfiged = true;
						break;
					case FieldName.KEYWORDS:
						keywordsConfiged = true;
						break;
					case FieldName.DESCRIPTION:
						descriptionConfiged = true;
						break;
					case FieldName.AUTHORS:
						authorsConfiged = true;
						break;
					case FieldName.PRETITLE:
						pretitleConfiged = true;
						break;
					case FieldName.SUBTITLE:
						subtitleConfiged = true;
						break;
					case FieldName.CHANNEL:
						channelConfiged = true;
						break;
					default:
						break;
					}
					
					// 用规则抽取
					for (FieldExtractRule rule : rules) {
						Elements nodes = doc.select(rule.getLocation());
						int size = nodes.size();
						int index = rule.getIndex();
						if (index >= size) {
							logger.warn("field[{}] rule[{}] index[{}] >= size[{}]", name, rule.getLocation(), index, size);
						} else if (index >= 0) {
							result = extractField(nodes.get(index), rule);
						} else if (size > 1) {
							StringBuilder sb = new StringBuilder();
							for (Element node : nodes) {
								sb.append(extractField(node, rule));
								if (FieldName.CHANNEL == name) {
									sb.append('>');
								}
							}
							if (FieldName.CHANNEL == name) {
								sb.deleteCharAt(sb.length()-1);
							}
							result = sb.toString();
						} else if (size == 1) {
							result = extractField(nodes.get(0), rule);
						}
						if (null != result && !result.isEmpty()) {
							Pattern pa = rule.getPattern();
							if (null == pa) {
								break;
							} else {
								Matcher ma = pa.matcher(result);
								result = null;
								if (ma.find()) {
									result = ma.group(rule.getGroup());
								}
								if (null != result && !result.isEmpty()) {
									break;
								}
							}
						}
					}
				}
				
//				boolean required = field.isRequired();
				
				// 检验抽取的结果
				if (null != result && !result.isEmpty()) {
					// 用规则抽取成功就给相应字段赋值
					switch (name) {
					case FieldName.TITLE:
						article.setTitle(result);
						break;
					case FieldName.CONTENT:
						article.setContent(result);
						break;
					case FieldName.PUBTIME:
						try {
							article.setPubTime(ProcessUtils.parseDate(result));
						} catch (ParseException e) {
							logger.warn("unparseable pubTime : \"{}\" - {}", result, taskUrl);
						}
						break;
					case FieldName.PUBSOURCE:
						String pubSource = ProcessUtils.extractPubSource(result);
						if (null != pubSource && !pubSource.isEmpty()) {
							result = pubSource;
						}
						article.setPubSource(result);
						break;
					case FieldName.KEYWORDS:
						article.setKeywords(result);
						break;
					case FieldName.DESCRIPTION:
						article.setDescription(result);
						break;
					case FieldName.AUTHORS:
						String authors = ProcessUtils.extractAuthors(result);
						if (null != authors && !authors.isEmpty()) {
							result = authors;
						}
						article.setAuthors(result);
						break;
					case FieldName.TAGS:
						article.setTags(result);
						break;
					case FieldName.PRETITLE:
						article.setPretitle(result);
						break;
					case FieldName.SUBTITLE:
						article.setSubtitle(result);
						break;
					case FieldName.CHANNEL:
						article.setChannel(result);
						break;
					default:
						break;
					}
				} else {
					// 无规则 或 用规则抽取失败 
					String failLog = null;
					if (disableAuto) {
						// 如果禁用了自动化辅助就清空自动化抽取的结果
						switch (name) {
						case FieldName.TITLE:
							titleConfiged = true;
							article.setTitle(null);
							break;
						case FieldName.CONTENT:
							contentConfiged = true;
							article.setContent(null);
							break;
						case FieldName.PUBTIME:
							pubTimeConfiged = true;
							article.setPubTime(null);
							break;
						case FieldName.PUBSOURCE:
							pubSourceConfiged = true;
							article.setPubSource(null);
							break;
						case FieldName.KEYWORDS:
							keywordsConfiged = true;
							article.setKeywords(null);
							break;
						case FieldName.DESCRIPTION:
							descriptionConfiged = true;
							article.setDescription(null);
							break;
						case FieldName.AUTHORS:
							authorsConfiged = true;
							article.setAuthors(null);
							break;
						case FieldName.PRETITLE:
							pretitleConfiged = true;
							article.setPretitle(null);
							break;
						case FieldName.SUBTITLE:
							subtitleConfiged = true;
							article.setSubtitle(null);
							break;
						case FieldName.CHANNEL:
							channelConfiged = true;
							article.setChannel(null);
							break;
						default:
						}
						if (haveRules) {
							failLog = "{} extract failed - {}";
						}
					} else {
						// 没有配disableAuto，所以肯定配了抽取规则，所以这里是用规则抽取失败
						// 再看看用自动化抽取的结果
						switch (name) {
						case FieldName.TITLE:
							result = article.getTitle();
							break;
						case FieldName.CONTENT:
							result = article.getContent();
							break;
						case FieldName.PUBTIME:
							Date createDate = article.getPubTime();
							if (null != createDate) {
								result = createDate.toString();
							}
							break;
						case FieldName.PUBSOURCE:
							result = article.getPubSource();
							break;
						case FieldName.KEYWORDS:
							result = article.getKeywords();
							break;
						case FieldName.DESCRIPTION:
							result = article.getDescription();
							break;
						case FieldName.AUTHORS:
							result = article.getAuthors();
							break;
						case FieldName.PRETITLE:
							result = article.getPretitle();
							break;
						case FieldName.SUBTITLE:
							result = article.getSubtitle();
							break;
						case FieldName.CHANNEL:
							result = article.getChannel();
							break;
						default:
							break;
						}
						
						if (null != result && !result.isEmpty()) {
							continue;
						}
						failLog = "{} mix extract failed - {}";
					}
					if (null != failLog) {
						onExtractFailed(failLog, name, currentTask);
					}
					if (field.isRequired()) {
						return;
					}
				}
			}
		}

		// 上面是模板里配了的字段
		// 下面验证没有配的字段
		String log = "{} auto extract failed - {}";
		if (!titleConfiged) {
			String title = article.getTitle();
			if (null == title || title.isEmpty()) {
				onExtractFailed(log, FieldName.TITLE, currentTask);
				return;
			}
		}
		if (!contentConfiged) {
			String content = article.getContent();
			if (null == content || content.isEmpty()) {
				onExtractFailed(log, FieldName.CONTENT, currentTask);
				if (SourceType.BBS != sourceType) {
					return;
				}
			}
		}
		StringBuilder sb = new StringBuilder();
		if (!pubTimeConfiged) {
			if (null == article.getPubTime()) {
				sb.append(FieldName.PUBTIME).append(',');
//				onExtractFailed(log, FieldName.PUBTIME, currentTask);
			}
		}
		if (!pubSourceConfiged) {
			String source = article.getPubSource();
			if (null == source || source.isEmpty()) {
				sb.append(FieldName.PUBSOURCE).append(',');
			}
		}
		if (!keywordsConfiged) {
			String keywords = article.getKeywords();
			if (null == keywords || keywords.isEmpty()) {
				sb.append(FieldName.KEYWORDS).append(',');
			}
		}
		if (!descriptionConfiged) {
			String description = article.getDescription();
			if (null == description || description.isEmpty()) {
				sb.append(FieldName.DESCRIPTION).append(',');
			}
		}
		if (!authorsConfiged) {
			String authors = article.getAuthors();
			if (null == authors || authors.isEmpty()) {
				sb.append(FieldName.AUTHORS).append(',');
			}
		}
		if (!pretitleConfiged) {
			String pretitle = article.getPretitle();
			if (null == pretitle || pretitle.isEmpty()) {
				sb.append(FieldName.PRETITLE).append(',');
			}
		}
		if (!subtitleConfiged) {
			String subtitle = article.getSubtitle();
			if (null == subtitle || subtitle.isEmpty()) {
				sb.append(FieldName.SUBTITLE).append(',');
			}
		}
		if (!channelConfiged) {
			String channel = article.getChannel();
			if (null == channel || channel.isEmpty()) {
				sb.append(FieldName.CHANNEL).append(',');
			}
		}
		if (sb.length() != 0) {
			onExtractFailed(log, sb.toString(), currentTask);
		}
		
		// 过期判断
		Date pubtime = article.getPubTime();
		if (null == pubtime) {
			article.setPubTime(article.getCreateTime());
		} else if (System.currentTimeMillis() - pubtime.getTime() > ConstantsHome.EFFECTIVE_PERIOD_TARGET) {
			targetLogger.info("{}\t{}\t{}", siteTaskId, 0, 1);
			logger.info("target expired [{}] - {}", pubtime.getTime(), taskUrl);
			if (!Spider.inTestMode()) {
				return;
			}
		} else if (pubtime.getTime() > System.currentTimeMillis()){
			article.setPubTime(article.getCreateTime());
		}

		// 过滤无效标题
		String title = article.getTitle();
		if (ProcessUtils.isInvalidTitle(title)) {
			logger.info("{} - invalid title - {}", title, taskUrl);
			return;
		}
		if (null != matchedTemplate) {
			Pattern[] pas = matchedTemplate.getIgnoreTitlePatterns();
			if (null != pas && pas.length != 0) {
				for (Pattern pa : pas) {
					Matcher ma = pa.matcher(title);
					if (ma.matches()) {
						logger.info("{} - invalid title - {}", title, taskUrl);
						return;
					}
				}
			}
		}
		
		if (Spider.inTestMode()) {
			logger.info("raw content - {}", Jsoup.parse(article.getContent()).text());
			articlesLogger.info(article.toString());
		}
		
		logger.info("pipe start - {}", taskUrl);
		if (pipeline.pipe(false, siteTaskId, article)) {
			regionService.countRegion(article);
		}
	}
}