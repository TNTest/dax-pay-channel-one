import cn.daxpay.open.channel.ChannelServiceApp;
import cn.daxpay.open.channel.mock.controller.MockPayController;
import cn.daxpay.open.channel.mock.sdk.MockGatewayClient;
import org.springframework.boot.SpringApplication;

public class MockChannelProfileCheck {
    public static void main(String[] args) {
        for (String mode : new String[]{"PROD", "DEV"}) {
            try (var app = SpringApplication.run(ChannelServiceApp.class,
                    "--spring.profiles.active=" + mode.toLowerCase(java.util.Locale.ROOT),
                    "--spring.main.web-application-type=none",
                    "--daxpay.platform.deployment.mode=" + mode,
                    "--daxpay.channel.transport-encrypt.key=0123456789abcdef0123456789abcdef")) {
                boolean enabled = mode.equals("DEV");
                for (var type : new Class<?>[]{MockGatewayClient.class, MockPayController.class}) {
                    int count = app.getBeanNamesForType(type).length;
                    if (count != (enabled ? 1 : 0)) {
                        throw new AssertionError(mode + ": expected " + type.getSimpleName()
                                + " bean count " + (enabled ? 1 : 0) + ", got " + count);
                    }
                }
            }
            System.out.println("PASS " + mode + " mock gateway and controller registration");
        }
    }
}
