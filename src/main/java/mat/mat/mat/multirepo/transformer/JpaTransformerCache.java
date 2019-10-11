package mat.mat.mat.multirepo.transformer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class JpaTransformerCache {

    private final Map<ClassToClass, JpaTransformer> jpaTransformerCache = new HashMap<>();

    public void registerTransformer(final ClassToClass clazz, final JpaTransformer transformer){
        jpaTransformerCache.put(clazz, transformer);
    }

    public Optional<JpaTransformer> transformerForClass(final ClassToClass clazz){
        return Optional.ofNullable(jpaTransformerCache.get(clazz));
    }

}
