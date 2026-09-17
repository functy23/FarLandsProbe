#!/bin/bash
# 启动指定版本的开发服务端,等待 mixin 加载完成(或超时),然后停掉。
#
# 为什么需要:静态审计只能确认"注入点存在",无法确认 Mixin 加载器接受这份配置
# (例如 @Mixin 的 class/interface 类型与目标不匹配、两个 mixin 冲突)。本脚本让
# 版本升级后的验证覆盖到运行期那一步。
#
# 用法: tools/mixin-audit/smoke-server.sh <版本号> <gradle run 任务名>
# 退出码:0 = 服务端成功启动到 Done(或至少没有 mixin 报错)
set -u

VERSION="$1"
RUN_TASK="$2"
RUN_DIR="${3:-run}"
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
LOG="$(mktemp -t farlandsprobe-smoke-${VERSION}-XXXXXX.log)"
TIMEOUT_SECONDS="${SMOKE_TIMEOUT_SECONDS:-600}"

cd "$ROOT" || exit 1

# 开发服务端会拒绝在没有 eula.txt 的目录里启动;run/ 目录本身是 gitignore 的构建产物,
# 这里按需创建,避免冒烟测试因为"没同意 EULA"而假失败。
mkdir -p "$RUN_DIR"
printf 'eula=true\n' > "$RUN_DIR/eula.txt"

echo "smoke-server: starting $RUN_TASK (Minecraft $VERSION), run dir=$RUN_DIR, log=$LOG"

./gradlew "$RUN_TASK" --console=plain > "$LOG" 2>&1 &
GRADLE_PID=$!

# 收到 TERM 时先停 gradle,再交给下面的清理逻辑收尾。
trap 'kill -TERM "$GRADLE_PID" 2>/dev/null' INT TERM

status=1
deadline=$((SECONDS + TIMEOUT_SECONDS))
while [ "$SECONDS" -lt "$deadline" ]; do
    if grep -q 'MixinApplyError\|InvalidMixinException\|Mixin prepare for mod .* failed\|Mixin transformation of .* failed' "$LOG"; then
        echo "smoke-server: MIXIN FAILURE for $VERSION"
        grep -A3 -m1 'MixinApplyError\|InvalidMixinException' "$LOG"
        status=1
        break
    fi
    if grep -q 'Done (.*)! For help, type' "$LOG"; then
        echo "smoke-server: server reached Done for $VERSION"
        status=0
        break
    fi
    if ! kill -0 "$GRADLE_PID" 2>/dev/null; then
        echo "smoke-server: gradle exited early for $VERSION"
        tail -30 "$LOG"
        status=1
        break
    fi
    sleep 2
done

if [ "$status" -ne 0 ] && [ "$SECONDS" -ge "$deadline" ]; then
    echo "smoke-server: TIMEOUT after ${TIMEOUT_SECONDS}s for $VERSION"
    tail -30 "$LOG"
fi

# 关掉整个进程组(gradle + 它拉起的服务端 JVM)。
kill -TERM "$GRADLE_PID" 2>/dev/null
sleep 3
pkill -TERM -f "farlandsprobe-.*-dev" 2>/dev/null
pkill -TERM -f "net.fabricmc.devlaunchinjector.Main" 2>/dev/null
wait "$GRADLE_PID" 2>/dev/null
sleep 1

echo "smoke-server: finished $VERSION with status $status"
exit "$status"
