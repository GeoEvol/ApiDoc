#!/bin/bash
# DiLinkSDK Javadoc 一体化生成脚本
# 包含: 扫描源文件 -> 生成 Javadoc -> 应用 Modern 主题

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

# 配置
OUTPUT_DIR="docs/Javadoc/api"
SOURCE_LIST_FILE="sources.txt"
CLASSPATH="api/libs/dilink-frameworkb.jar;api/libs/frameworkb.jar;core/libs/frameworkb.jar;D:/Program Files/Android-Sdk-Windows-28_to_35/Sdk/platforms/android-35/android.jar"

echo "========================================"
echo "DiLinkSDK Javadoc 生成工具"
echo "========================================"

# 步骤 1: 清理旧文档
if [ -d "$OUTPUT_DIR" ]; then
    echo "[1/4] 清理旧文档目录..."
    rm -rf "$OUTPUT_DIR"
fi

# 步骤 2: 扫描源文件
echo "[2/4] 扫描 Java 源文件..."
find api/src/main/java core/src/main/java -name "*.java" -type f 2>/dev/null | sort > "$SOURCE_LIST_FILE"
FILE_COUNT=$(wc -l < "$SOURCE_LIST_FILE")
echo "      找到 $FILE_COUNT 个文件，已生成 $SOURCE_LIST_FILE"

# 步骤 3: 生成 Javadoc
echo "[3/4] 生成 Javadoc 文档..."
javadoc -d "$OUTPUT_DIR" \
    @${SOURCE_LIST_FILE} \
    -encoding UTF-8 \
    -charset UTF-8 \
    -docencoding UTF-8 \
    -locale zh_CN \
    -protected \
    -use \
    -splitindex \
    -version \
    -author \
    -windowtitle "DiLink API SDK 0.0.3-SNAPSHOT" \
    -doctitle "DiLink API SDK API Reference" \
    -html5 \
    -notimestamp \
    -Xdoclint:none \
    -tag "date:a:Date:" \
    -classpath "$CLASSPATH"

if [ $? -ne 0 ]; then
    echo "      Javadoc 生成失败!"
    exit 1
fi
echo "      Javadoc 生成成功!"

# 步骤 4: 应用 Modern 主题
echo "[4/4] 应用 Modern Javadoc 主题..."

# 替换 CSS
cp modern-javadoc.css "$OUTPUT_DIR/stylesheet.css"

# 复制 JS
cp modern-javadoc.js "$OUTPUT_DIR/modern-javadoc.js"

# 注入 JS 到所有 HTML 文件
find "$OUTPUT_DIR" -name "*.html" -type f | while read html_file; do
    if ! grep -q "modern-javadoc.js" "$html_file"; then
        sed -i 's|</body>|<script type="text/javascript" src="modern-javadoc.js"></script>\n</body>|' "$html_file"
    fi
done

echo "      主题应用成功!"

echo "========================================"
echo "完成! 请打开 $OUTPUT_DIR/index.html 查看"
echo "========================================"