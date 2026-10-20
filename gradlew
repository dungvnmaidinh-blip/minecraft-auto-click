#!/bin/sh
# Gradle start-up script (rút gọn). Cần gradle/wrapper/gradle-wrapper.jar,
# file này được workflow GitHub Actions tự tạo bằng "gradle wrapper".
APP_HOME=$(cd "$(dirname "$0")" && pwd -P)
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then JAVACMD="$JAVA_HOME/bin/java"; else JAVACMD=java; fi
if [ ! -f "$CLASSPATH" ]; then
  echo "Thiếu gradle/wrapper/gradle-wrapper.jar. Chạy: gradle wrapper --gradle-version 8.9" >&2
  exit 1
fi
exec "$JAVACMD" -Xmx64m -Xms64m -Dorg.gradle.appname=gradlew \
  -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
