package com.byd.apidoc.parser.javadoc.stub

import com.byd.apidoc.model.ApiConfig

/**
 * Generates temporary source stubs used only by the JDK javadoc parser.
 *
 * These sources are added to javadoc -sourcepath so javac can resolve known
 * missing compile-time types. They must not be collected as documentation
 * source files and must not be emitted as API pages.
 */
class JavadocStubGenerator {

    File generate(ApiConfig config) {
        File root = resolveStubRoot(config)
        write(root, "com/foundation/spi/Spi.java", spiStub())
        write(root, "com/foundation/spi/config/SpiConfig.java", spiConfigStub())
        return root
    }

    private static File resolveStubRoot(ApiConfig config) {
        String stubDir = config?.generatedStubDir ?: "build/apidoc/generated-stubs"
        File root = new File(stubDir)
        if (!root.isAbsolute()) {
            String projectRoot = config?.projectRootDir ?: "."
            root = new File(projectRoot, stubDir)
        }
        return root
    }

    private static void write(File root, String relativePath, String content) {
        File target = new File(root, relativePath)
        target.parentFile.mkdirs()
        target.setText(content, "UTF-8")
    }

    private static String spiStub() {
        return '''package com.foundation.spi;

import android.content.Context;
import com.foundation.spi.config.SpiConfig;

public final class Spi {

    public static Spi get(String tag) {
        return null;
    }

    public static void init(String tag, SpiConfig config) {
    }

    public <T> T getService(Class<T> serviceClass) {
        return null;
    }

    public <T> T getService(Context context, Class<T> serviceClass) {
        return null;
    }
}
'''
    }

    private static String spiConfigStub() {
        return '''package com.foundation.spi.config;

public class SpiConfig {

    public static class Builder {
        public SpiConfig build() {
            return new SpiConfig();
        }
    }
}
'''
    }
}
