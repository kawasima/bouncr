package net.unit8.bouncr;

import enkan.Env;
import enkan.config.EnkanSystemFactory;
import enkan.system.EnkanSystem;
import enkan.util.ReflectionUtils;

public class Main {
    private static final String DEFAULT_ENKAN_SYSTEM_FACTORY = "net.unit8.bouncr.BouncrEnkanSystem";

    public static void main(String[] args) {
        System.setProperty("enkan.env", "production");
        String enkanSystemFactoryName = Env.getString("ENKAN_SYSTEM_FACTORY", DEFAULT_ENKAN_SYSTEM_FACTORY);
        final EnkanSystem system = ReflectionUtils.tryReflection(() -> {
            Class<?> enkanSystemFactoryClass = Class.forName(enkanSystemFactoryName);
            EnkanSystemFactory enkanSystemFactory = (EnkanSystemFactory) enkanSystemFactoryClass.newInstance();
            return enkanSystemFactory.create();
        });

        Runtime.getRuntime().addShutdownHook(new Thread(system::stop));
        system.start();
    }
}
