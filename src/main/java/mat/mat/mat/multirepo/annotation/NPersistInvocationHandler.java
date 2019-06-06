package mat.mat.mat.multirepo.annotation;

import lombok.RequiredArgsConstructor;
import mat.mat.mat.multirepo.transformer.JpaTransformer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class NPersistInvocationHandler implements InvocationHandler {

    private static List<String> readMethods = Arrays.asList("get", "find");

    private static int FIRST_GENERIC = 0;
    private static int SECOND_GENERIC = 1;

    private final String name;
    private final List<JpaRepository> targetRepositories;
    private final List<JpaTransformer> targetTransformers;
    private final Class sourceInterface;
    private final JpaTransformerCache transformerCache;

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        if (isToString(method)){
            return name;
        }

        if (isModifying(method)){
            //hande write
            return handleWrite(proxy, method, args);
        }else{
            //handle read
            return handleRead(proxy, method, args);
        }
    }

    private Boolean isToString(final Method method){
        return method.getName().equals("toString");
    }

    private Boolean isModifying(final Method method){
        return !readMethods.stream().anyMatch(method.getName()::contains);
    }

    private Object handleRead(final Object proxy, final Method method, final Object[] args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        //Read. assumption only use the first Repo/Transformer in the list.

        final JpaRepository theRepository = targetRepositories.get(0);
        final JpaTransformer theTransformer = targetTransformers.get(0);
        final Class targetInterface = getInterface(theRepository);
        final Method sourceMethod = sourceInterface.getMethod(method.getName(), method.getParameterTypes());
        final Method targetMethod = targetInterface.getMethod(method.getName(), method.getParameterTypes());

        final Object result = targetMethod.invoke(theRepository, args);

        if (result == null){
            return Optional.empty();
        }

        if (!hasDifferentReturnTypes(sourceMethod, targetMethod)){
            if (compatibleJpaTransformer(theTransformer, sourceMethod.getReturnType(), targetMethod.getReturnType())){
                return theTransformer.transformBackward(result, transformerCache);
            }
        }

        return result;
    }

    private Object handleWrite(final Object proxy, final Method method, final Object[] args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        //Write. Iterate over list of repositories and save.

        int index=0;
        for (final JpaRepository repository : targetRepositories){
            handleWrite(proxy, method, args, repository, targetTransformers.get(index));
            index++;
        }

        return null;
    }

    private Object handleWrite(final Object proxy, final Method method, final Object[] args, final JpaRepository repository, final JpaTransformer transformer) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        final Class targetInterface = getInterface(repository);
        final Method targetMethod = targetInterface.getMethod(method.getName(), method.getParameterTypes());
        final Object[] transformedArgs = transformArgs(args, transformer);
        return targetMethod.invoke(repository, transformedArgs);
    }

    private Class getInterface(final JpaRepository jpaRepository){
        return jpaRepository.getClass().getInterfaces()[0];
    }

    private boolean hasDifferentReturnTypes(final Method sourceMethod, final Method targetMethod){
        return sourceMethod.getReturnType().equals(targetMethod.getReturnType());
    }

    private boolean compatibleJpaTransformer(final JpaTransformer jpaTransformer, final Class source, final Class target){

        final Class first = getGenericArgument(jpaTransformer, FIRST_GENERIC);
        final Class second = getGenericArgument(jpaTransformer, SECOND_GENERIC);

        if (source.equals(first) && target.equals(second)){
            return true;
        }

        return false;
    }

    private Class getGenericArgument(final JpaTransformer jpaTransformer, final int generic){
        final Type[] genericInterfaces = jpaTransformer.getClass().getGenericInterfaces();
        final Type type = ((ParameterizedType)genericInterfaces[0]).getActualTypeArguments()[generic];
        return (Class)type;
    }

    private Object[] transformArgs(final Object[] args, final JpaTransformer transformer){

        final List<Object> transformedArgs = new ArrayList<>();

        final Class transformerInput = getGenericArgument(transformer, FIRST_GENERIC);

        for (final Object arg : args){

            if (arg.getClass().equals(transformerInput)) {
                transformedArgs.add(transformer.transformForward(arg, transformerCache));
            } else{
                transformedArgs.add(arg);
            }

        }

        return transformedArgs.toArray(new Object[0]);
    }


}
