#!/bin/sh
# Build the channel reactor with JDK 25 and Maven, then run: sh tests/verify.sh
set -eu
tests=$(CDPATH= cd "$(dirname "$0")" && pwd)
archive="$tests/../daxpay-channel-start/target/daxpay-channel-one.jar"
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
cd "$work"
jar xf "$archive" BOOT-INF
classpath='BOOT-INF/classes:BOOT-INF/lib/*'
java -cp "$classpath" "$tests/WechatSdkCertificateModeCheck.java"
LOG_PATH="$work/logs" java -cp "$classpath" "$tests/MockChannelProfileCheck.java"
