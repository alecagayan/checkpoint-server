package server;

import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

@Configuration
@PropertySource("classpath:application.properties")
public class Config implements EnvironmentAware {

    private static Environment env;

    @Override
    public void setEnvironment(final Environment environment) {
        env = environment;
    }

    public static String getProperty(String name) {
        return env.getProperty(name);
    }    
    
}
