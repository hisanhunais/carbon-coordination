/*
 * Copyright (c) 2017, WSO2 Inc. (http://wso2.com) All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.cluster.coordinator.rdbms.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.cluster.coordinator.commons.CoordinationStrategy;
import org.wso2.carbon.cluster.coordinator.rdbms.RDBMSCoordinationStrategy;
import org.wso2.carbon.cluster.coordinator.rdbms.beans.ClusterCoordinatorConfigurations;
import org.wso2.carbon.config.ConfigurationException;
import org.wso2.carbon.config.provider.ConfigProvider;
import org.wso2.carbon.datasource.core.api.DataSourceService;

/**
 * RDBMS cluster coordinator data service.
 */
@Component(
        name = "org.wso2.carbon.cluster.coordinator.rdbms.internal.RDBMSCoordinationServiceComponent",
        immediate = true
)
public class RDBMSCoordinationServiceComponent {

    private static final Log log = LogFactory.getLog(RDBMSCoordinationServiceComponent.class);

    /**
     * This is the activation method of RDBMSCoordinationServiceComponent. This will be called when its references are
     * satisfied.
     *
     * @param bundleContext the bundle context instance of this bundle.
     */
    @Activate
    protected void start(BundleContext bundleContext) {

        ClusterCoordinatorConfigurations clusterConfiguration;
        try {
            clusterConfiguration = RDBMSCoordinationServiceHolder.getConfigProvider().
                    getConfigurationObject(ClusterCoordinatorConfigurations.class);
            if (clusterConfiguration != null && clusterConfiguration.isEnabled()) {
                if (clusterConfiguration.getStrategyConfig().getHeartbeatInterval() < 5000) {
                    log.warn("It is recommended to have the heartbeatInterval value as " + 5000 +
                            " milliseconds or higher than the provided " +
                            clusterConfiguration.getStrategyConfig().getHeartbeatInterval() +
                            " milliseconds in cluster.config.strategyConfig");
                }
                if (clusterConfiguration.getStrategyConfig().getHeartbeatMaxRetry() < 3) {
                    log.warn("It is recommended to have the heartbeatMaxRetry value as " +
                            3 + " than provided value " +
                            clusterConfiguration.getStrategyConfig().getHeartbeatMaxRetry()
                            + " in cluster.config.strategyConfig");
                }
                RDBMSCoordinationServiceHolder.setClusterConfiguration(clusterConfiguration);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug(RDBMSCoordinationStrategy.class.getCanonicalName() + " will not be activated because " +
                            "no configurations found");
                }
                return;
            }
        } catch (ConfigurationException e) {
            if (log.isDebugEnabled()) {
                log.debug(RDBMSCoordinationStrategy.class.getCanonicalName() + " will not be activated because " +
                        "no configurations found");
            }
            return;
        }
        if (clusterConfiguration.isEnabled()) {
            if (clusterConfiguration.getCoordinationStrategyClass().equals(RDBMSCoordinationStrategy.class.
                    getCanonicalName())) {

                bundleContext.registerService(CoordinationStrategy.class, new RDBMSCoordinationStrategy(), null);
                log.info("RDBMS Coordination Service Component Activated");
            } else {
                log.warn("No such coordination strategy service found: " + clusterConfiguration.
                        getCoordinationStrategyClass());
            }
        } else {
            log.info("Cluster coordination has been disabled. Enable it in deployment.yaml " +
                    "to use the clustering service");
        }
    }

    /**
     * This is the deactivation method of RDBMSCoordinationServiceComponent. This will be called when this component
     * is being stopped or references are satisfied during runtime.
     */
    @Deactivate
    protected void stop() {
        RDBMSCoordinationServiceHolder.setClusterConfiguration(null);
    }

    @Reference(
            name = "carbon.config.provider",
            service = ConfigProvider.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unregisterConfigProvider"
    )
    protected void registerConfigProvider(ConfigProvider configProvider) throws ConfigurationException {
        RDBMSCoordinationServiceHolder.setConfigProvider(configProvider);
    }

    protected void unregisterConfigProvider(ConfigProvider configProvider) {
        RDBMSCoordinationServiceHolder.setConfigProvider(null);
    }

    @Reference(
            name = "org.wso2.carbon.datasource.DataSourceService",
            service = DataSourceService.class,
            cardinality = ReferenceCardinality.AT_LEAST_ONE,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unregisterDataSourceListener"
    )
    protected void registerDataSourceListener(DataSourceService dataSourceService) {
        RDBMSCoordinationServiceHolder.setDataSourceService(dataSourceService);
    }

    protected void unregisterDataSourceListener(DataSourceService dataSourceService) {
        RDBMSCoordinationServiceHolder.setDataSourceService(null);
    }
}
