package mat.mat.mat.multirepo;

import mat.mat.mat.multirepo.annotation.MultiRepository;
import mat.mat.mat.multirepo.annotation.MultiRepositoryAutowiredAnnotationBeanPostProcessor;
import mat.mat.mat.multirepo.transformer.JpaTransformerCache;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass({MultiRepository.class})
public class MultiRepositoryAnnotationConfiguration {

    @Bean
    public JpaTransformerCache transformerCache(){
        return new JpaTransformerCache();
    }

    @Bean
    public MultiRepositoryAutowiredAnnotationBeanPostProcessor multiRepositoryAutowiredAnnotationBeanPostProcessor(final ConfigurableListableBeanFactory beanFactory, final JpaTransformerCache transformerCache){
        return new MultiRepositoryAutowiredAnnotationBeanPostProcessor(beanFactory, transformerCache);
    }

    @Bean
    public MultiRepositoryAnnotationConfig multipleRepository() {
        return new MultiRepositoryAnnotationConfig();
    }


}
