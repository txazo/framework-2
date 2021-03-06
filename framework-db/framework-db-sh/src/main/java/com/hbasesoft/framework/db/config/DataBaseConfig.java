/**************************************************************************************** 
 Copyright © 2003-2012 hbasesoft Corporation. All rights reserved. Reproduction or       <br>
 transmission in whole or in part, in any form or by any means, electronic, mechanical <br>
 or otherwise, is prohibited without the prior written consent of the copyright owner. <br>
 ****************************************************************************************/
package com.hbasesoft.framework.db.config;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.aopalliance.intercept.MethodInterceptor;
import org.hibernate.SessionFactory;
import org.hibernate.transform.ResultTransformer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.hbasesoft.framework.common.GlobalConstants;
import com.hbasesoft.framework.common.utils.PropertyHolder;
import com.hbasesoft.framework.db.core.ClusterDataSource;
import com.hbasesoft.framework.db.core.annotation.handler.SQLHandler;
import com.hbasesoft.framework.db.core.config.DaoConfig;
import com.hbasesoft.framework.db.hibernate.BaseHibernateDao;
import com.hbasesoft.framework.db.spring.AutoProxyBeanFactory;
import com.hbasesoft.framework.db.spring.SpringDaoHandler;

/**
 * <Description> <br>
 * 
 * @author 王伟<br>
 * @version 1.0<br>
 * @taskId <br>
 * @CreateDate 2016年3月23日 <br>
 * @since V1.0<br>
 * @see com.hbasesoft.framework.db.config <br>
 */
@Configuration
@EnableTransactionManagement
public class DataBaseConfig implements ApplicationContextAware {

    private ApplicationContext context;

    @Bean(name = "transactionManager")
    public PlatformTransactionManager registTransactonManger(SessionFactory sessionFactory) {
        HibernateTransactionManager manager = new HibernateTransactionManager();
        manager.setSessionFactory(sessionFactory);
        return manager;
    }

    @Bean(name = "sessionFactory")
    public LocalSessionFactoryBean createSessionFactory() {
        LocalSessionFactoryBean bean = new LocalSessionFactoryBean();
        bean.setDataSource(new ClusterDataSource("master"));
        Map<String, String> map = PropertyHolder.getProperties();
        Properties properties = new Properties();
        for (Entry<String, String> entry : map.entrySet()) {
            if (entry.getKey().startsWith("db.hibernate")) {
                properties.setProperty(entry.getKey().substring(3, entry.getKey().length()), entry.getValue());
            }
        }
        bean.setHibernateProperties(properties);
        bean.setPackagesToScan(getBasePackage());
        return bean;
    }

    @Bean
    public BaseHibernateDao registBaseHibernateDao(SessionFactory sessionFactory) {
        BaseHibernateDao dao = new BaseHibernateDao();
        dao.setSessionFactory(sessionFactory);
        return dao;
    }

    @Bean
    public DaoConfig registDaoConfig(@Value("${master.db.type}") String dbType) {

        // dao的配置
        DaoConfig dataConfig = new DaoConfig();
        dataConfig.setCache(true);
        dataConfig.setDbType(dbType);
        dataConfig.setBaseDao(context.getBean(BaseHibernateDao.class));
        dataConfig.setCallBackType(ResultTransformer.class);
        return dataConfig;
    }

    @Bean(name = "springDaoHandler")
    public MethodInterceptor registDaoHandler() {
        // dao处理类
        SpringDaoHandler daoHandler = new SpringDaoHandler();
        daoHandler.setDaoConfig(context.getBean(DaoConfig.class));
        daoHandler.setSqlExcutor(context.getBean(BaseHibernateDao.class));
        return daoHandler;
    }

    @Bean
    public AutoProxyBeanFactory registAutoProxyBeanFactory() {
        // sql处理类
        SQLHandler sqlHandler = new SQLHandler();
        sqlHandler.setDaoConfig(context.getBean(DaoConfig.class));

        AutoProxyBeanFactory beanFactory = new AutoProxyBeanFactory();
        beanFactory.setHandler(sqlHandler);
        beanFactory.setInterceptors("springDaoHandler");
        beanFactory.setPackagesToScan(getBasePackage());

        return beanFactory;
    }

    private String getBasePackage() {
        String className = this.getClass().getName();
        return className.substring(0,
            className.indexOf(GlobalConstants.PERIOD, className.indexOf(GlobalConstants.PERIOD) + 1)) + ".*";
    }

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        this.context = context;
    }
}
