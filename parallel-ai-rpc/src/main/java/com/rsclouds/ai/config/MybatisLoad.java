package com.rsclouds.ai.config;

import com.baomidou.mybatisplus.spring.MybatisMapperRefresh;
import com.baomidou.mybatisplus.spring.MybatisSqlSessionFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author lishun
 * @Description: TODO
 * @date 2018/2/1
 */
@Configuration()
@Profile("dev")
public class MybatisLoad {
	@Value("${mybatis.mapper-locations}")
	public String mapperLocation;

	@Value("${mybatis.type-aliases-package}")
	public String typeAliasesPackage;

	@Autowired
	public DataSource dataSource;
	@Bean
	public MybatisSqlSessionFactoryBean initMybatisSqlSessionFactoryBean(){
		MybatisSqlSessionFactoryBean mybatisSqlSessionFactoryBean = new MybatisSqlSessionFactoryBean();
		mybatisSqlSessionFactoryBean.setDataSource(dataSource);
		mybatisSqlSessionFactoryBean.setMapperLocations(resolveMapperLocations(new String[]{mapperLocation}));
		mybatisSqlSessionFactoryBean.setTypeAliasesPackage(typeAliasesPackage);
		return mybatisSqlSessionFactoryBean;
	}
	@Bean
	public MybatisMapperRefresh initMybatisMapperRefresh(){
		Resource[] resources = resolveMapperLocations(new String[]{mapperLocation});
		MybatisMapperRefresh mybatisMapperRefresh = null;
		try {
			mybatisMapperRefresh = new
					MybatisMapperRefresh(resources, initMybatisSqlSessionFactoryBean().getObject(), 10 , 2, true);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return mybatisMapperRefresh;
	}
	public Resource[] resolveMapperLocations(String[] mapperLocations) {
		ResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();
		List<Resource> resources = new ArrayList();
		if (mapperLocations != null) {
			String[] var3 = mapperLocations;
			int var4 = var3.length;

			for(int var5 = 0; var5 < var4; ++var5) {
				String mapperLocation = var3[var5];

				try {
					Resource[] mappers = resourceResolver.getResources(mapperLocation);
					resources.addAll(Arrays.asList(mappers));
				} catch (IOException var8) {
					;
				}
			}
		}

		return (Resource[])resources.toArray(new Resource[resources.size()]);
	}
}
