package mat.mat.mat.multirepo.annotation;

import mat.mat.mat.multirepo.transformer.JpaTransformer;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class JpaTransformerCache {

    private final Map<ClassToClass, JpaTransformer> jpaTransformerCache = new HashMap<>();

    public void registerTransformer(final ClassToClass clazz, final JpaTransformer transformer){
        jpaTransformerCache.put(clazz, transformer);
    }

    public Optional<JpaTransformer> transformerForClass(final ClassToClass clazz){
        return Optional.ofNullable(jpaTransformerCache.get(clazz));
    }

}
