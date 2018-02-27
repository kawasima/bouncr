import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.WebDriverRunner;
import enkan.Env;
import enkan.config.EnkanSystemFactory;
import enkan.system.EnkanSystem;
import enkan.util.ReflectionUtils;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Proxy;

import java.util.Optional;

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

        Optional.ofNullable(Env.get("http_proxy"))
                .ifPresent(proxy -> WebDriverManager.chromedriver().proxy(proxy));

        Configuration.browser = WebDriverRunner.CHROME;
        Configuration.headless = true;
        Configuration.reportsFolder = "target/integration-test/reports";
        Configuration.browserSize = "1024x768";

        final Proxy proxy = new Proxy();
        Optional.ofNullable(Env.get("http_proxy"))
                .ifPresent(p -> proxy.setHttpProxy(p));
        Optional.ofNullable(Env.get("https_proxy"))
                .ifPresent(p -> proxy.setSslProxy(p));
        String noProxy = Env.get("no_proxy");
        if (noProxy != null && !noProxy.isEmpty()) {
            proxy.setNoProxy(noProxy);
        } else {
            proxy.setNoProxy("localhost");
        }
        if (proxy.getHttpProxy() != null || proxy.getSslProxy() != null) {
            WebDriverRunner.setProxy(proxy);
        }
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
    }
}
