import cn.daxpay.open.channel.wechat.config.WechatSdkConfig;
import cn.daxpay.open.channel.wechat.config.WechatSdkCredential;
import cn.daxpay.open.platform.core.exception.ChannelServiceException;
import cn.daxpay.open.platform.core.exception.ChannelErrorCode;
import com.github.binarywang.wxpay.config.WxPayConfig;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;

public class WechatSdkCertificateModeCheck {
    static WechatSdkCredential credential() {
        var c = new WechatSdkCredential();
        c.setWxMchId("1900000001");
        c.setWxAppId("wx-native-test");
        c.setApiKeyV3("0123456789abcdef0123456789abcdef");
        c.setPrivateKey("unused-until-client-init");
        c.setCertSerialNo("0123456789ABCDEF");
        return c;
    }
    static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
    static void rejects(WechatSdkCredential credential, boolean callback) {
        try {
            if (callback) WechatSdkConfig.buildCallbackService(credential);
            else WechatSdkConfig.buildService(credential);
            throw new AssertionError("Partial public-key configuration must fail closed");
        } catch (ChannelServiceException expected) {
            check(expected.getCode() == ChannelErrorCode.INVALID_CONFIG.getCode(), "Wrong configuration error code: " + expected.getCode());
        }
    }
    static String pem(String type, byte[] content) {
        return "-----BEGIN " + type + "-----\n" + Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(content) + "\n-----END " + type + "-----\n";
    }
    public static void main(String[] args) throws Exception {
        WxPayConfig cert = WechatSdkConfig.buildService(credential()).getConfig();
        check(!cert.isFullPublicKeyModel(), "Missing WeChat public key must select native certificate verification mode");
        var callback = credential(); callback.setWxAppId(null);
        check(!WechatSdkConfig.buildCallbackService(callback).getConfig().isFullPublicKeyModel(), "Callback certificate mode must not require AppID");
        System.out.println("PASS certificate-only mode (payment and callback)");
        for (boolean cb : new boolean[]{false, true}) {
            var idOnly = credential(); idOnly.setPublicKeyId("PUB_KEY_ID_TEST"); rejects(idOnly, cb);
            var keyOnly = credential(); keyOnly.setPublicKey("unused-public-key"); rejects(keyOnly, cb);
        }
        System.out.println("PASS partial public-key configuration rejected for payment and callback");
        var generator = KeyPairGenerator.getInstance("RSA"); generator.initialize(2048);
        var merchant = generator.generateKeyPair(); var platform = generator.generateKeyPair();
        var full = credential(); full.setPrivateKey(pem("PRIVATE KEY", merchant.getPrivate().getEncoded()));
        full.setPublicKeyId("PUB_KEY_ID_TEST"); full.setPublicKey(pem("PUBLIC KEY", platform.getPublic().getEncoded()));
        var config = WechatSdkConfig.buildService(full).getConfig();
        check(config.isFullPublicKeyModel(), "Complete public-key configuration must keep full public-key mode");
        var payload = "synthetic-response".getBytes(StandardCharsets.UTF_8);
        var signer = Signature.getInstance("SHA256withRSA"); signer.initSign(platform.getPrivate()); signer.update(payload);
        var signature = Base64.getEncoder().encodeToString(signer.sign());
        var verifier = config.getVerifier();
        check(verifier.verify("PUB_KEY_ID_TEST", payload, signature), "Valid platform signature must be accepted");
        check(!verifier.verify("PUB_KEY_ID_TEST", "tampered".getBytes(StandardCharsets.UTF_8), signature), "Tampered response must be rejected");
        config.getApiV3HttpClient().close();
        System.out.println("PASS full public-key mode validates signatures and rejects tampering");
    }
}
