# NearVoice Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** 목적지 접근과 지정 시간에 메시지를 읽고, 월별 달력에서 복수 날짜를 선택하는 안드로이드 앱을 배포한다.

**Architecture:** Kotlin 네이티브 UI와 기기 내부 예약 저장소를 사용한다. 순수 예약 규칙 엔진을 위치 포그라운드 서비스와 AlarmManager가 공유하고, 음성·진동·알림음을 직렬 재생한다.

**Tech Stack:** Kotlin, Android SDK, Jetpack Compose, DataStore, kotlinx.serialization, Android TextToSpeech, AlarmManager, LocationManager, 지도 WebView, GitHub Actions. Android 최소 8.0(API 26), 대상 SDK 및 빌드 도구 버전은 구현 시작 시 공식 문서로 확인하고 고정한다.

**Spec:** ../../../NearVoice-design.md (프로젝트 루트의 NearVoice-design.md).

## Global Constraints
- 한국어 UI, 어두운 배경, 청록색 강조.
- 도착 반경: 100·300·500·1000m 및 직접 입력.
- 위치/시간/둘 다 조건을 각각 독립적으로 실행한다.
- 날짜 지정·매일·선택 요일 반복을 제공한다.
- 집·회사 주소 버튼과 다른 장소의 지도 선택을 제공한다.
- 기기 내부 저장, 전체 OFF 시 위치·시간 예약 취소, 지나간 알림 몰아서 재생 금지.
- 저장소는 multi0819/NearVoice를 사용한다. 생성 기능이 없으면 빈 저장소 생성이 필요하다고 정확히 알린다.

## Review Focus
1. 월말·윤년·월 이동에서도 선택 날짜와 다음 알림이 정확해야 한다.
2. 같은 시각 여러 예약이 있으면 모두 한 번씩 순서대로 실행해야 한다.
3. GPS 오차·이탈 후 재진입에서 경계 흔들림으로 반복 재생되면 안 된다.
4. 권한 거부·무음·한국어 음성 없음은 감지 정상으로 잘못 표시하면 안 된다.
5. 수정·삭제·재부팅 이후 이전 예약이 남거나 완료 기록이 초기화되면 안 된다.

## File structure
프로젝트 루트: settings.gradle.kts, build.gradle.kts, gradle.properties, Gradle wrapper, README.md, .github/workflows/android.yml.
앱: app/build.gradle.kts, app/src/main/AndroidManifest.xml.
Kotlin 경로는 app/src/main/java/com/multi0819/nearvoice/ 아래이며, model/Reservation.kt, data/ReservationStore.kt, domain/ScheduleRules.kt, domain/EntryRules.kt, alarms/TimeScheduler.kt, alarms/AlarmReceiver.kt, alarms/BootReceiver.kt, location/LocationService.kt, playback/PlaybackService.kt, permissions/PermissionState.kt, ui/MainActivity.kt, ui/ReservationEditor.kt, ui/MultiDateCalendar.kt, ui/PlacePicker.kt, ui/SettingsScreen.kt로 책임을 나눈다.
테스트 경로는 app/src/test/java/com/multi0819/nearvoice/ 및 app/src/androidTest/java/com/multi0819/nearvoice/이다.

### Task 1: 예약 모델·저장·규칙 엔진
**Files:** model/Reservation.kt, data/ReservationStore.kt, domain/ScheduleRules.kt, domain/EntryRules.kt, 단위 테스트 ScheduleRulesTest.kt·EntryRulesTest.kt·ReservationStoreTest.kt 및 프로젝트 빌드 설정.
**Interfaces:** Reservation(id:String, title:String, message:String, enabled:Boolean, trigger:Trigger, place:Place?, radiusMeters:Double, dates:Set<LocalDate>, repeat:Repeat, weekdays:Set<DayOfWeek>, time:LocalTime?, voice:Boolean, vibration:Boolean, soundUri:String?, completed:Set<String>). Place(label:String, latitude:Double, longitude:Double). Trigger=LOCATION/TIME/BOTH, Repeat=ONCE/DATES/DAILY/WEEKDAYS.
ReservationStore.observe():Flow<AppState>, suspend upsert(Reservation), suspend delete(String), suspend setEnabled(Boolean), suspend markCompleted(id:String,key:String). AppState(enabled:Boolean,reservations:List<Reservation>,home:Place?,work:Place?). ScheduleRules.nextTime(r:Reservation,after:ZonedDateTime):ZonedDateTime?; EntryRules.evaluate(r:Reservation,distanceMeters:Double,date:LocalDate,wasInside:Boolean):EntryDecision.
- [ ] 테스트 작성: 2028-02-29 포함 날짜의 다음 실행, 선택 요일, 이미 완료된 날짜 제외, 시간·위치 완료 키 독립, 같은 날 두 예약 별도 보존, 경계 내 유지 재생 금지, 충분한 이탈 후 재진입 허용을 assert한다.
- [ ] Gradle testDebugUnitTest 실행해 구현 전 실패를 확인한다.
- [ ] 모델·저장소·규칙을 구현한다. 완료 키는 조건과 날짜를 포함하고, 예약별 상태를 보존한다. 이탈은 반경+max(50m,반경의 20%)를 기준으로 판단한다.
- [ ] 단위 테스트 성공을 확인하고 Task 1 변경을 커밋한다.

### Task 2: 달력·예약·장소·설정 화면
**Files:** ui/ 아래 모든 화면, ui/MainActivity.kt, CalendarSelectionTest.kt, ReservationEditorTest.kt.
**Interfaces:** MultiDateCalendar(selected:Set<LocalDate>,onChange:(Set<LocalDate>)->Unit); ReservationEditor(initial:Reservation?,onSave:(Reservation)->Unit); PlacePicker(onSelected:(Place)->Unit). Task 1 저장소를 소비한다.
- [ ] 테스트 작성: 다른 월로 이동해도 10월5일·11월3일 선택 유지, 재탭 해제, 과거 신규 날짜 거부, 전체 해제, 빈 메시지와 잘못된 반경 저장 차단, 같은 날 복수 예약을 assert한다.
- [ ] 해당 테스트를 실행해 구현 전 실패를 확인한다.
- [ ] 화면을 구현한다. 집·회사 주소 검색 결과는 지도에서 확인 후 저장한다. 지도는 제공자 이용 조건과 표시 의무를 확인해 선택한다. WebView 브리지는 좌표만 수신하고 범위·호출 출처를 검사한다. 주소 검색은 백그라운드에서 수행하고 오류·빈 결과를 표시한다.
- [ ] 시스템 알림음 선택, 음성 미리 듣기, 삭제 확인, 날짜 목록, 반복 모드, 상태 표시를 연결한다.
- [ ] 테스트와 assembleDebug 성공을 확인하고 화면 변경을 커밋한다.

### Task 3: 시간 예약과 직렬 알림 재생
**Files:** alarms/TimeScheduler.kt·AlarmReceiver.kt·BootReceiver.kt, playback/PlaybackService.kt, SchedulerTest.kt·PlaybackQueueTest.kt.
**Interfaces:** TimeScheduler.reconcile(state:AppState,now:ZonedDateTime):Unit; PlaybackService.enqueue(context:Context,id:String,condition:String,date:LocalDate):Unit. Receiver는 ID로 최신 저장 데이터를 조회해 실행 여부를 재검사한다.
- [ ] 테스트 작성: 동일 시각 두 예약 모두 전달, 변경 이전 알람 무효, 삭제 후 전달 차단, 전체 OFF 차단, 재부팅 시 미래 알람만 등록, 한 조건 완료가 다른 조건을 취소하지 않음을 assert한다.
- [ ] 테스트의 구현 전 실패를 확인한다.
- [ ] AlarmManager와 고유 PendingIntent를 구현한다. 정확한 알람 권한이 없으면 상태 안내 및 허용 설정 이동을 제공한다. 재부팅·시간대 변경 시 미래 예약을 재계산한다.
- [ ] 알림 채널, foreground media playback 서비스, 한국어 TTS 준비·완료 콜백, 진동·시스템 선택음, 순차 큐와 자원 해제를 구현한다. 엔진 없음·음성 데이터 없음은 알림으로 안내하며 큐가 멈추지 않도록 한다.
- [ ] 테스트와 APK 빌드를 확인하고 커밋한다.

### Task 4: 화면 OFF 위치 감지와 권한 상태
**Files:** location/LocationService.kt, permissions/PermissionState.kt, LocationFlowTest.kt·PermissionStateTest.kt, AndroidManifest.xml.
**Interfaces:** LocationService.start(context:Context):Unit; stop(context:Context):Unit; PermissionState.read(context:Context):PermissionStatus. Task 1 규칙과 Task 3 재생기를 소비한다.
- [ ] 테스트 작성: 경계 흔들림 억제, 선택 날짜 밖에서 차단, 날짜별 최초 접근 한 번, 새 날짜 상태 갱신, 권한 거부·위치 OFF에서 실제 비활성 표시, OFF 직후 늦은 콜백 차단을 assert한다.
- [ ] 테스트의 구현 전 실패를 확인한다.
- [ ] 사용자가 켤 때 위치 foreground 서비스를 시작한다. 지속 알림·중지 버튼·권한 재검사·위치 상태를 연결한다. 과거 위치와 정확도가 불충분한 위치는 접근 판단에서 제외한다. 권한 및 foreground 서비스 제약은 최신 공식 문서를 확인해 반영한다.
- [ ] 서비스 복구 가능 여부와 재부팅 제한을 반영해 재개 안내를 제공한다.
- [ ] 단위 테스트와 manifest/lint 확인 후 커밋한다. 실제 화면 OFF·이동 테스트 여부는 별도 기록한다.

### Task 5: GitHub 업로드·APK 제공
**Files:** .github/workflows/android.yml, README.md, 배포 체크 기록.
**Interfaces:** CI 입력은 GitHub 커밋이며 출력은 검사 결과와 설치 가능한 APK이다.
- [ ] 연결 계정과 저장소 쓰기 권한을 확인한다. 저장소 생성 도구가 없으면 지원되는 GitHub 경로를 조사하고, 사용자 조작이 필요하면 빈 저장소 생성만 요청한다.
- [ ] Gradle wrapper와 의존성 버전을 고정한다. Actions에서 testDebugUnitTest·lintDebug·assembleDebug를 실행하고 APK를 업로드한다.
- [ ] 실제 CI 결과를 확인하고 실패 로그에 따라 수정한다. 테스트용 APK 서명과 업데이트용 서명 유지 방식을 README에 설명한다.
- [ ] 달력 선택·수정삭제·시간 알림·음성 테스트를 가능한 환경에서 검증한다. 실제 위치·절전·재부팅 확인은 검증하지 않았다면 분명히 표시한다.
- [ ] 최종 커밋, 설치 파일 및 GitHub 링크를 제공한다. 빌드 실패 상태를 완료로 보고하지 않는다.

## Execution choice
직접 구현 방식을 권장한다. 위 다섯 작업은 저장 모델과 알림 서비스 인터페이스를 공유하므로 한 작업 흐름에서 검증하는 것이 적합하다. 구현 전 이 계획의 사용자 검토가 필요하다.
