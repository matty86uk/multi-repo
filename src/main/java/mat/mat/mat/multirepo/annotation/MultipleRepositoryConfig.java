package mat.mat.mat.multirepo.annotation;

import mat.mat.mat.multirepo.transformer.JpaTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Configuration
public class MultipleRepositoryConfig {

    @Autowired
    private ApplicationContext appContext;

    private final Logger logger = LoggerFactory.getLogger(MultipleRepositoryConfig.class);

    @Autowired
    private DefaultListableBeanFactory beanFactory;

    @Autowired
    private JpaTransformerCache transformerCache;

    @PostConstruct
    public Boolean setupMultiRepository() throws ClassNotFoundException {

        final ClassPathScanningCandidateComponentProvider scanner = new AnnotationScanningProvider(MultiRepository.class);
        final Set<BeanDefinition> candidates = scanner.findCandidateComponents("com.example.dualpersistsample");

        createBeans(candidates, beanFactory, transformerCache);

        return true;
    }

    private void createBeans(final Set<BeanDefinition> candidates, final DefaultListableBeanFactory beanFactory, final JpaTransformerCache transformerCache) throws ClassNotFoundException {

        for (final BeanDefinition bd : candidates){

            if (bd instanceof AnnotatedBeanDefinition) {
                final AnnotatedBeanDefinition abd = (AnnotatedBeanDefinition)bd;

                final AnnotationMetadata metadata = abd.getMetadata();
                Assert.isTrue(metadata.isInterface(), "@MultiRepository can only applied to Interfaces");

                final Class classToImplement = Class.forName(bd.getBeanClassName());
                final String beanName = classToImplement.getSimpleName();

                final List<JpaRepository> jpaRepositoryBeans = jpaRepositoriesFromAnnotation(MultiRepository.class, abd);
                final List<JpaTransformer> jpaTransformerBeans = jpaTransformersFromAnnotation(MultiRepository.class, abd);

                for (final JpaTransformer transformer : jpaTransformerBeans){
                    updateTransformerCache(transformer, transformerCache);
                }

                final NPersistInvocationHandler handler = new NPersistInvocationHandler(beanName, jpaRepositoryBeans, jpaTransformerBeans, classToImplement, transformerCache);

                final Object implementation = Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[] {classToImplement}, handler);

                beanFactory.initializeBean(implementation, beanName);
                beanFactory.autowireBeanProperties(implementation, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true);
                beanFactory.registerSingleton(beanName, implementation);


                final List<String> classNamesForLogging = jpaRepositoryBeanNamesFromAnnotation(MultiRepository.class, abd);
                logger.info("Registered MultiRepository [{}] Using {}", beanName, classNamesForLogging);
            }
        }
    }

    private List<String> jpaRepositoryBeanNamesFromAnnotation(final Class annotation, final AnnotatedBeanDefinition abd){
        final List<Object> classes = abd.getMetadata().getAllAnnotationAttributes(annotation.getName()).get("jpaRepositories");
        final List<Class> classesToFind = Arrays.asList((Class[])classes.get(0));
        final List<String> names = new ArrayList<>();
        for (final Class classToFind : classesToFind) {
            names.add(classToFind.getSimpleName());
        }
        return names;
    }

    private List<JpaRepository> jpaRepositoriesFromAnnotation(final Class annotation, final AnnotatedBeanDefinition abd){
        final List<Object> classes = abd.getMetadata().getAllAnnotationAttributes(annotation.getName()).get("jpaRepositories");
        final List<Class> classesToFind = Arrays.asList((Class[])classes.get(0));
        final List<JpaRepository> beans = new ArrayList<>();
        for (final Class classToFind : classesToFind){
            beans.add((JpaRepository)appContext.getBean(classToFind));
        }
        return beans;
    }

    private List<JpaTransformer> jpaTransformersFromAnnotation(final Class annotation, final AnnotatedBeanDefinition abd){
        final List<Object> classes = abd.getMetadata().getAllAnnotationAttributes(annotation.getName()).get("jpaTransformers");
        final List<Class> classesToFind = Arrays.asList((Class[])classes.get(0));
        final List<JpaTransformer> beans = new ArrayList<>();
        for (final Class classToFind : classesToFind){
            beans.add((JpaTransformer)appContext.getBean(classToFind));
        }
        return beans;
    }

    private class AnnotationScanningProvider extends ClassPathScanningCandidateComponentProvider {

        public AnnotationScanningProvider(Class clazz) {
            super(false);
            addIncludeFilter(new AnnotationTypeFilter(clazz, false));
        }

        @Override
        protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
            return beanDefinition.getMetadata().isInterface();
        }
    }

    private void updateTransformerCache(final JpaTransformer transformer, final JpaTransformerCache transformerCache){
        final Type[] genericInterfaces = transformer.getClass().getGenericInterfaces();
        final Type firstType = ((ParameterizedType)genericInterfaces[0]).getActualTypeArguments()[0];
        final Type secondType = ((ParameterizedType)genericInterfaces[0]).getActualTypeArguments()[1];
        final ClassToClass classToClass = ClassToClass.builder()
                .classA((Class)firstType)
                .classB((Class)secondType)
                .build();
        transformerCache.registerTransformer(classToClass, transformer);
    }
}
