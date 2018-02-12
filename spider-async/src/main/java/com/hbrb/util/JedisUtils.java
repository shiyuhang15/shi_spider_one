package com.hbrb.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Transaction;

/**
 * 程序启动时initPool，程序结束时closePool。
 */
public class JedisUtils {
	public final static String STATUS_CODE_OK = "OK";
	
	private static JedisPool pool;
	public static JedisCluster jc;
	
	/**
	 * 初始化数据源
	 */
	public static void init() {
		File propFile = new File(System.getProperty("user.dir") + File.separatorChar
							+ "redis.properties");
		if (!propFile.isFile()) {
			propFile = new File(propFile.getParentFile().getParentFile(), "redis.properties");
		}
		if (!propFile.isFile()) {
			throw new Error("redis配置文件不存在");
		}
		try {
			Properties ppts = new Properties();
			FileInputStream in = new FileInputStream(propFile);
			ppts.load(in);
			in.close();
			String host = ppts.getProperty("host");
			if (null == host || host.length() == 0) {
				throw new Exception("host为空");
			}
			int port = Integer.parseInt(ppts
					.getProperty("port"));
			initPool(host, port);
		} catch (Exception e) {
			throw new Error("redis配置初始化错误", e);
		}
	}

	/**
	 * 池创建后，在程序退出前要调用closePool()方法
	 * @param host
	 * @param port
	 */
	public static void initPool(String host, int port) {
		if (null != pool && null != jc) {
			return;
		}
		JedisPoolConfig config = new JedisPoolConfig();
		config.setMaxTotal(300);
		config.setMaxIdle(10);
		config.setMaxWaitMillis(1000 * 10);
		config.setTestOnBorrow(true);
		if (null == pool) {
			if (null != host && -1 != port) {
				pool = new JedisPool(config, host, port, 30000);
			}
		}
		if (null == jc) {
			File conf = new File(System.getProperty("user.dir") + File.separatorChar + "redisClusterNodes.conf");
			if (!conf.isFile()) {
				conf = new File(conf.getParentFile().getParentFile(), "redisClusterNodes.conf");
			}
			if (conf.isFile()) {
				List<String> lines;
				try {
					lines = FileUtils.readLines(conf, Charset.defaultCharset());
				} catch (IOException e) {
					throw new Error(e);
				}
				Set<HostAndPort> jedisClusterNodes = new HashSet<HostAndPort>(lines.size());
				for (String line : lines) {
					String[] split = line.split(":");
					jedisClusterNodes.add(new HostAndPort(split[0], Integer.parseInt(split[1])));
				}
				jc = new JedisCluster(jedisClusterNodes, 30000, config);
			}
		}
	}
	
	public static Jedis createJedis(){
		if (null == pool) {
			return null;
		}
		return pool.getResource();
	}
	
	/**
	 * 见AtomicInteger的compareAndSet方法
	 * @param key
	 * @param expect
	 * @param update
	 * @return
	 */
	public static boolean compareAndSet(String key, String expect, String update){
		if (null == key || key.length() == 0) {
			throw new IllegalArgumentException("key不能为空");
		}
		try (Jedis jedis = JedisUtils.createJedis()) {
			List<Object> exec = null;
			while (null == exec) {
				jedis.watch(key);
				String value = jedis.get(key);
				boolean match = false;
				if (null == value) {
					if (null == expect) {
						match = true;
					}
				} else {
					if (null != expect && expect.equals(value)) {
						match = true;
					}
				}
				if (!match) {
					return false;
				}
				Transaction multi = jedis.multi();
				if (update == null) {
					multi.del(key);
				} else {
					multi.set(key, update);
				}
				exec = multi.exec();
			}
			return true;
		}
	}
	
	/**
	 * 给key对应的value加1，超过max置0，返回加之前的值。
	 * @param key 
	 * @param max
	 * @return
	 */
	public static int getAndIncrement(String key, int max){
		if (null == key || key.length() == 0) {
			throw new IllegalArgumentException("key不能为空");
		}
		if (max < 0) {
			throw new IllegalArgumentException("最大值不能小于0");
		}
		
		try (Jedis jedis = JedisUtils.createJedis()) {
			List<Object> exec = null;
			int index = 0;
			while (null == exec) {
				jedis.watch(key);
				String strIndex = jedis.get(key);
				if (null != strIndex) {
					int parseInt = Integer.parseInt(strIndex);
					if (parseInt <= max) {
						index = parseInt;
					}
				}
				Transaction multi = jedis.multi();
				if (index >= max) {
					multi.set(key, "0");
				} else {
					multi.set(key, String.valueOf(index + 1));
				}
				exec = multi.exec();
			}
			return index;
		}
	}
	
	public static void closePool(){
		if (null != pool) {
			pool.destroy();
		}
		if (null != jc) {
			try {
				jc.close();
			} catch (IOException e) {
				throw new Error(e);
			}
		}
	}
}
