package mat.mat.mat.multirepo.annotation;

import mat.mat.mat.multirepo.proxy.MultiRepositoryProxy;
import mat.mat.mat.multirepo.transformer.ClassToClass;
import mat.mat.mat.multirepo.transformer.JpaTransformer;
import mat.mat.mat.multirepo.transformer.JpaTransformerCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MultiRepositoryAutowiredAnnotationBeanPostProcessor extends AutowiredAnnotationBeanPostProcessor {

    private static final Logger logger = LoggerFactory.getLogger(MultiRepositoryAutowiredAnnotationBeanPostProcessor.class);

    private final ConfigurableListableBeanFactory beanFactory;
    private final JpaTransformerCache transformerCache;

    public MultiRepositoryAutowiredAnnotationBeanPostProcessor(final ConfigurableListableBeanFactory beanFactory, final JpaTransformerCache transformerCache){
        this.beanFactory = beanFactory;
        this.transformerCache = transformerCache;
    }

    @Override
    public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {

        final Field[] fields = beanClass.getDeclaredFields();

        for (final Field field : fields){
            if (hasMultiRepositoryAnnotationOnClass(field)){
                final Optional<Object> bean = getBeanForClass(field.getType());
                if (!bean.isPresent()){
                    try {
                        createImplementation(field);
                    } catch (Exception e){
                    }
                }
            }
        }
        return super.postProcessBeforeInstantiation(beanClass, beanName);
    }

    private boolean hasMultiRepositoryAnnotationOnClass(final Field field){
        final Annotation[] annotations = field.getType().getAnnotations();
        for (final Annotation annotation : annotations){
            if (annotation.annotationType().equals(MultiRepository.class)){
                return true;
            }
        }
        return false;
    }

    private Optional<Object> getBeanForClass(final Class clazz){
        try {
            final Object bean = beanFactory.getBean(clazz);
            return Optional.ofNullable(bean);
        }catch (NoSuchBeanDefinitionException e){
            return Optional.empty();
        }
    }

    private void createImplementation(final Field field) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        final Class clazzToImplement = field.getType();
        final Annotation annotation = clazzToImplement.getAnnotation(MultiRepository.class);

        final Method transformerMethod = annotation.annotationType().getMethod("jpaTransformers");
        final Method repositoryMethod = annotation.annotationType().getMethod("jpaRepositories");

        final Class[] listOfTransformers = (Class[]) transformerMethod.invoke(annotation);
        final Class[] listOfRepositories = (Class[]) repositoryMethod.invoke(annotation);


        final String beanName = clazzToImplement.getSimpleName();
        final List<JpaRepository> jpaRepositories = getRepositoryBeans(listOfRepositories);
        final List<JpaTransformer> jpaTransformers = getTransformerBeans(listOfTransformers);

        for (final JpaTransformer jpaTransformer : jpaTransformers){
            final Class first = getGenericArgument(jpaTransformer, 0);
            final Class second = getGenericArgument(jpaTransformer, 1);

            final ClassToClass classToClass = ClassToClass.builder()
                    .classA(first)
                    .classB(second)
                    .build();

            transformerCache.registerTransformer(classToClass, jpaTransformer);
        }

        final MultiRepositoryProxy proxy = new MultiRepositoryProxy(beanName, jpaRepositories, jpaTransformers, clazzToImplement, transformerCache);

        final Object implementation = Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[] {clazzToImplement}, proxy);

        logger.info("MultipleRepository Candidate {} - [{}]", beanName, toSimple(listOfRepositories));

        beanFactory.initializeBean(implementation, beanName);
        beanFactory.autowireBeanProperties(implementation, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true);
        beanFactory.registerSingleton(beanName, implementation);
    }

    private List<JpaRepository> getRepositoryBeans(final Class[] listOfRepositories){
        final List<JpaRepository> repositories = new ArrayList<>();
        for (final Class repository : listOfRepositories){
            repositories.add((JpaRepository)beanFactory.getBean(repository));
        }
        return repositories;
    }

    private List<JpaTransformer> getTransformerBeans(final Class[] listOfTransformers){
        final List<JpaTransformer> transformers = new ArrayList<>();
        for (final Class repository : listOfTransformers){
            transformers.add((JpaTransformer)beanFactory.getBean(repository));
        }
        return transformers;
    }

    private List<String> toSimple(final Class[] classes){
        final List<String> simple = new ArrayList<>();
        for (final Class clazz : classes){
            simple.add(clazz.getSimpleName());
        }
        return simple;
    }

    private Class getGenericArgument(final JpaTransformer jpaTransformer, final int generic){
        final Type[] genericInterfaces = jpaTransformer.getClass().getGenericInterfaces();
        final Type type = ((ParameterizedType)genericInterfaces[0]).getActualTypeArguments()[generic];
        return (Class)type;
    }

}
