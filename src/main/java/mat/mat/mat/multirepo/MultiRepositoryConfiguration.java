package mat.mat.mat.multirepo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.transaction.ChainedTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

public class MultiRepositoryConfiguration {

    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(final List<PlatformTransactionManager> transactionManagers){
        return new ChainedTransactionManager(transactionManagers.toArray(new PlatformTransactionManager[0]));
    }

}
