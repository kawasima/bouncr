import enkan.Env;
import enkan.config.EnkanSystemFactory;
import enkan.system.EnkanSystem;
import enkan.util.ReflectionUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;

public class BouncrAdminTest {
    static EnkanSystem system;

    @BeforeAll
    public static void createSystem() {
        System.out.println(Env.getInt("PORT", 3000));

        system = ReflectionUtils.tryReflection(() -> {
            Class<?> enkanSystemFactoryClass = Class.forName("net.unit8.bouncr.BouncrEnkanSystem");
            EnkanSystemFactory enkanSystemFactory = (EnkanSystemFactory) enkanSystemFactoryClass.newInstance();
            return enkanSystemFactory.create();
        });
    }

    @BeforeEach
    public void startSystem() {
        system.start();
    }

    @AfterEach
    public void stopSystem() {
        system.stop();
    }

    @Test
    public void userCanLoginByUsername() {
        open("http://localhost:3005/my/signIn");
        $(By.name("account")).setValue("admin");
        $(By.name("password")).setValue("admin");
        $("button[type=submit]").click();
        $("#submit").click();
    }
}
