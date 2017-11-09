package backendexample;

import enkan.system.EnkanSystem;
import net.unit8.bouncr.BouncrEnkanSystem;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.config.HookPoint;
import net.unit8.bouncr.hook.WebHook;

public class WebHookApp {
    public static void main(String[] args) throws Exception {
        Class.forName("net.unit8.bouncr.authz.UserPermissionPrincipal");
        Class.forName("net.unit8.bouncr.sign.JwtHeader");
        Class.forName("net.unit8.bouncr.sign.JwtClaim");

        EnkanSystem system = new BouncrEnkanSystem().create();
        BouncrConfiguration config = (BouncrConfiguration) system.getComponent("config");

        config.getHookRepo().register(HookPoint.AFTER_SIGNUP,
                new WebHook("https://maker.ifttt.com/trigger/bouncr_after_signup/with/key/xxxxxx", "POST"));
        system.start();
    }

}
