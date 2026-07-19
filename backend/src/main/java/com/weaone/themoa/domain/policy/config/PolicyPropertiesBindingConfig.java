package com.weaone.themoa.domain.policy.config;

import com.weaone.themoa.domain.policy.policy.service.RegionRebuildProperties;
import com.weaone.themoa.domain.policy.rag.config.RagProperties;
import com.weaone.themoa.domain.policy.region.config.RegionSyncProperties;
import com.weaone.themoa.domain.policy.youthcenter.config.YouthCenterApiProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        YouthCenterApiProperties.class,
        RagProperties.class,
        RegionSyncProperties.class,
        RegionRebuildProperties.class
})
public class PolicyPropertiesBindingConfig {
}
