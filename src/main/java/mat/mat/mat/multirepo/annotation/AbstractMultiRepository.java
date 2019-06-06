package mat.mat.mat.multirepo.annotation;

import mat.mat.mat.multirepo.transformer.JpaTransformer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public class AbstractMultiRepository {

    private final List<JpaRepository> repositories;
    private final List<JpaTransformer> transformers;

    public AbstractMultiRepository(final List<JpaRepository> repositories,
                                   final List<JpaTransformer> transformers){
        this.repositories = repositories;
        this.transformers = transformers;
    }

}
