#!/usr/bin/env bash
set -euo pipefail
: "${NEARVOICE_GRADLE_LIB:?Set NEARVOICE_GRADLE_LIB to Gradle lib directory}"
root="$(cd "$(dirname "$0")/.." && pwd)"
classes="$(mktemp -d)"
trap 'rm -rf "$classes"' EXIT
classpath="$NEARVOICE_GRADLE_LIB/kotlin-stdlib-2.0.20.jar:$NEARVOICE_GRADLE_LIB/junit-4.13.2.jar:$NEARVOICE_GRADLE_LIB/hamcrest-core-1.3.jar"
java -cp "$NEARVOICE_GRADLE_LIB/*" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler -no-stdlib -no-reflect -classpath "$classpath" -d "$classes" "$root/app/src/main/java/com/multi0819/nearvoice/Reservation.kt" "$root/app/src/main/java/com/multi0819/nearvoice/Rules.kt" "$root/app/src/test/java/com/multi0819/nearvoice/RulesTest.kt" "$root/app/src/main/java/com/multi0819/nearvoice/MapsShare.kt" "$root/app/src/main/java/com/multi0819/nearvoice/GooglePlaceInfo.kt" "$root/app/src/test/java/com/multi0819/nearvoice/MapsShareTest.kt"
java -cp "$classes:$classpath" org.junit.runner.JUnitCore com.multi0819.nearvoice.RulesTest com.multi0819.nearvoice.MapsShareTest
