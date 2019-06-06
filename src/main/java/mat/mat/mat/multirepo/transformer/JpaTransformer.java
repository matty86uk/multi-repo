package mat.mat.mat.multirepo.transformer;

import mat.mat.mat.multirepo.annotation.ClassToClass;
import mat.mat.mat.multirepo.annotation.JpaTransformerCache;
import org.springframework.beans.BeanUtils;

import javax.persistence.Entity;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface JpaTransformer<A, B> {

    default B transformForward(final A a, final JpaTransformerCache transformerCache) {
        try {
            final B b = buildB();
            copy(a,b, transformerCache);
            return b;
        }
        catch (Exception e){
          throw new JpaTransformerException(e);
        }
    }

    default A transformBackward(final B b, final JpaTransformerCache transformerCache){
        try {
            final A a = buildA();
            BeanUtils.copyProperties(b, a);
            return a;
        }
        catch (Exception e){
            throw new JpaTransformerException(e);
        }
    }

    default B buildB() throws IllegalAccessException, InstantiationException {
        final Type[] genericInterfaces = this.getClass().getGenericInterfaces();
        final Type actualType = ((ParameterizedType)genericInterfaces[0]).getActualTypeArguments()[1];
        return (B)((Class)actualType).newInstance();
    }

    default A buildA() throws IllegalAccessException, InstantiationException {
        final Type[] genericInterfaces = this.getClass().getGenericInterfaces();
        final Type actualType = ((ParameterizedType)genericInterfaces[0]).getActualTypeArguments()[0];
        return (A) ((Class)actualType).newInstance();
    }

    default void copy(final Object a, final Object b, final JpaTransformerCache transformerCache) throws Exception {

        //copy using standard bean copy
        BeanUtils.copyProperties(a, b);

        //look for fields that are @Entity, or Collections of an @Entity
        final Field[] aFields = a.getClass().getDeclaredFields();

        for (final Field field : aFields){
            field.setAccessible(true);
            if (field.get(a) instanceof Collection){

                final Field bField = b.getClass().getDeclaredField(field.getName());
                final Type aListType = ((ParameterizedType)field.getGenericType()).getActualTypeArguments()[0];
                final Type bListType = ((ParameterizedType)bField.getGenericType()).getActualTypeArguments()[0];

                //from Domain to an Entity (a -> b, forward)
                if (!typeHasEntityAnnotation(aListType) && typeHasEntityAnnotation(bListType)){
                    final ClassToClass aToB = ClassToClass.builder().classA((Class)aListType).classB((Class)bListType).build();
                    final Optional<JpaTransformer> transformerForType = transformerCache.transformerForClass(aToB);

                    if (transformerForType.isPresent()){
                        bField.setAccessible(true);
                        final Collection collection = convertCollectionForward((Collection)field.get(a), (Collection) bField.get(b), transformerForType.get(), transformerCache);
                        bField.set(b, collection);
                        bField.setAccessible(false);
                    }else{
                        throw new Exception("Unable to convert, please provide a transformer for this conversion");
                    }


                }//from Entity to an Domain (b -> a, backward)
                else if (typeHasEntityAnnotation(aListType) && !typeHasEntityAnnotation(bListType)){
                    System.out.println("backward");
                }

            }else{
                typeHasEntityAnnotation(field.getType());
            }
            field.setAccessible(false);
        }
    }

    default boolean typeHasEntityAnnotation(final Type type){
        final Annotation[] annotations = ((Class)type).getAnnotations();

        if (annotations.length > 0){
            return annotationsContainEntity(annotations);
        }
        return false;
    }

    default boolean annotationsContainEntity(final Annotation[] annotations){
        for (final Annotation annotation : annotations){
            if (annotation.annotationType() == Entity.class){
                return true;
            }
        }
        return false;
    }

    default Collection convertCollectionForward(final Collection sourceACollection, final Collection existingBCollection, final JpaTransformer transformer, final JpaTransformerCache transformerCache) throws Exception {

        Collection newBCollection = null;

        if (existingBCollection instanceof List){
            newBCollection = new ArrayList<>();
        }else{
            throw new Exception("Unknown collection type");
        }

        for (final Object obj : sourceACollection){
            newBCollection.add(transformer.transformForward(obj, transformerCache));
        }

        return newBCollection;
    }
}
