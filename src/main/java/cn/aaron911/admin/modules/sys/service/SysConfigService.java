package cn.aaron911.admin.modules.sys.service;

import java.util.Arrays;
import java.util.Map;

import cn.aaron911.admin.common.utils.PageUtils;
import cn.aaron911.admin.common.utils.Query;
import cn.aaron911.admin.modules.sys.redis.SysConfigRedis;
import cn.aaron911.common.exception.FailedException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.gson.Gson;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.aaron911.admin.modules.sys.dao.SysConfigDao;
import cn.aaron911.admin.modules.sys.entity.SysConfigEntity;
import org.springframework.transaction.annotation.Transactional;

/**
 * 系统配置信息
 *
 */
@Service
public class SysConfigService extends ServiceImpl<SysConfigDao ,SysConfigEntity> {

	@Autowired
	private SysConfigRedis sysConfigRedis;

	public PageUtils queryPage(Map<String, Object> params) {
		String paramKey = (String)params.get("paramKey");

		IPage<SysConfigEntity> page = this.page(
				new Query<SysConfigEntity>().getPage(params),
				new QueryWrapper<SysConfigEntity>()
						.like(StringUtils.isNotBlank(paramKey),"param_key", paramKey)
						.eq("status", 1)
		);

		return new PageUtils(page);
	}
	
	/**
	 * 保存配置信息
	 */
	public void saveConfig(SysConfigEntity config) {
		this.save(config);
		sysConfigRedis.saveOrUpdate(config);
	}
	
	
	/**
	 * 更新配置信息
	 */
	@Transactional(rollbackFor = Exception.class)
	public void update(SysConfigEntity config) {
		this.updateById(config);
		sysConfigRedis.saveOrUpdate(config);
	}
	
	
	/**
	 * 根据key，更新value
	 */
	public void updateValueByKey(String key, String value) {
		baseMapper.updateValueByKey(key, value);
		sysConfigRedis.delete(key);
	}
	
	/**
	 * 删除配置信息
	 */
	public void deleteBatch(Long[] ids) {
		for(Long id : ids){
			SysConfigEntity config = this.getById(id);
			sysConfigRedis.delete(config.getParamKey());
		}

		this.removeByIds(Arrays.asList(ids));
	}
	
	/**
	 * 根据key，获取配置的value值
	 * 
	 * @param key           key
	 */
	public String getValue(String key) {
		SysConfigEntity config = sysConfigRedis.get(key);
		if(config == null){
			config = baseMapper.queryByKey(key);
			sysConfigRedis.saveOrUpdate(config);
		}

		return config == null ? null : config.getParamValue();
	}
	
	
	/**
	 * 根据key，获取value的Object对象
	 * @param key    key
	 * @param clazz  Object对象
	 */
	public <T> T getConfigObject(String key, Class<T> clazz) {
		String value = getValue(key);
		if(StringUtils.isNotBlank(value)){
			return new Gson().fromJson(value, clazz);
		}

		try {
			return clazz.newInstance();
		} catch (Exception e) {
			throw new FailedException("获取参数失败");
		}
	}
	
}
