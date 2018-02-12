package com.hbrb.routine;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import com.hbrb.exception.LogicError;
import com.hbrb.jdbc.MyDataSource;

public class TaskJoiner {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TaskJoiner.class);

	public static void main(String[] args) {
		MyDataSource.init();
		try {
			launch();
		} catch (Throwable e) {
			logger.error("异常退出", e);
		} finally {
			MyDataSource.destroy();
		}
	}

	private static void launch() {
		// key是临时表的id，value是插入到基础表后新生成的id
		Map<Integer, Integer> idMap = new HashMap<>();
		Connection conn = MyDataSource.connect();
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(
					"select tsu.*,ttp.c_content as c_content_t_p from (select tts.*,ttu.c_content as c_content_t_u from (select ttst.c_id,ttst.c_name,ttst.c_type_source,ttst.c_url_start,ttst.c_type,ttst.c_province,ttst.c_city,ttst.c_county,ttst.c_level,ttst.c_id_template_page from t_task_site_temp as ttst where ttst.c_id_spider = 0) as tts left join t_template_urls_temp as ttu on tts.c_id = ttu.c_id) as tsu left join t_template_page_temp as ttp on tsu.c_id = ttp.c_id;");
			try (PreparedStatement siteTaskPstmt = conn.prepareStatement(
					"insert into t_task_site(c_name,c_type_source,c_url_start,c_type,c_province,c_city,c_county,c_level,c_id_template_page) values (?,?,?,?,?,?,?,?,?)",
					Statement.RETURN_GENERATED_KEYS);
					PreparedStatement urlsTemplatePstmt = conn
							.prepareStatement("insert into t_template_urls(c_id,c_content) values (?,?)");
					PreparedStatement pageTemplatePstmt = conn
							.prepareStatement("insert into t_template_page(c_id,c_content) values (?,?)");) {
				while (rs.next()) {
					int oriId = rs.getInt(1);
					siteTaskPstmt.setString(1, rs.getString(2));
					siteTaskPstmt.setInt(2, rs.getInt(3));
					siteTaskPstmt.setString(3, rs.getString(4));
					siteTaskPstmt.setInt(4, rs.getInt(5));
					siteTaskPstmt.setString(5, rs.getString(6));
					siteTaskPstmt.setString(6, rs.getString(7));
					siteTaskPstmt.setString(7, rs.getString(8));
					siteTaskPstmt.setInt(8, rs.getInt(9));
					int c_id_template_page = rs.getInt(10);
					if (c_id_template_page != 0) {
						Integer newTemplatePageId = idMap.get(c_id_template_page);
						if (null == newTemplatePageId) {
							throw new LogicError("c_id_template_page对应模板不存在 - " + c_id_template_page);
						}
						c_id_template_page = newTemplatePageId;
					}
					siteTaskPstmt.setInt(9, c_id_template_page);
					siteTaskPstmt.executeUpdate();
					ResultSet idrs = siteTaskPstmt.getGeneratedKeys();
					int newId;
					if (idrs.next()) {
						newId = idrs.getInt(1);
						idMap.put(oriId, newId);
					} else {
						throw new Error("获取自动生成的id失败");
					}

					String urlsTemplateContent = rs.getString(11);
					if (null != urlsTemplateContent && !urlsTemplateContent.isEmpty()) {
						urlsTemplatePstmt.setInt(1, newId);
						urlsTemplatePstmt.setString(2, urlsTemplateContent);
						urlsTemplatePstmt.addBatch();
					}
					String pageTemplateContent = rs.getString(12);
					if (null != pageTemplateContent && !pageTemplateContent.isEmpty()) {
						pageTemplatePstmt.setInt(1, newId);
						pageTemplatePstmt.setString(2, pageTemplateContent);
						pageTemplatePstmt.addBatch();
					}
				}
				urlsTemplatePstmt.executeBatch();
				pageTemplatePstmt.executeBatch();
			}
		} catch (SQLException e) {
			throw new Error("合并失败", e);
		} finally {
			MyDataSource.release(rs, stmt, conn);
		}
	}
}
