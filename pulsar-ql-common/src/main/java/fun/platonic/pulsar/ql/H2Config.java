package fun.platonic.pulsar.ql;

import org.h2.util.JdbcUtils;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Custom H2 config, must be called on both server and client side
 * */
public class H2Config {
    private static AtomicBoolean configured = new AtomicBoolean();

    public static void config() {
        if (configured.getAndSet(true)) {
            return;
        }

        // TODO: Failed to serialize DOM when serializeJavaObject enabled
        System.setProperty("h2.serializeJavaObject", "false"); // set SysProperties.serializeJavaObject;
        JdbcUtils.serializer = new PulsarObjectSerializer();

        String dataTypeHandler = System.getProperty("h2.customDataTypesHandler");
        if (dataTypeHandler == null) {
            JdbcUtils.customDataTypesHandler = new PulsarDataTypesHandler();
        }
    }
}
