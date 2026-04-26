# RemoteOn

RemoteOn은 Android 앱에서 로컬 네트워크 안의 Windows PC를 Wake-on-LAN(WoL)으로 켜고, 현재 온라인 상태를 확인하는 예제 프로젝트입니다.

## 핵심 목표

- 여러 PC를 이름, MAC 주소, IP 주소, 브로드캐스트 주소로 저장
- RecyclerView 기반 리스트에서 각 PC를 관리
- WoL Magic Packet을 UDP 9번 포트로 브로드캐스트 전송
- TCP connect 기반 온라인/오프라인 확인
- 저장 데이터는 `SharedPreferences(MODE_PRIVATE)`에 보관
- 필요 시 AES-256 기반 암호화 적용
- 외부 포트포워딩 없이 LAN 내부에서만 동작
- 시스템 다크모드와 밝은 모드를 모두 지원하고, 앱 메뉴에서 직접 전환 가능
- IP와 서브넷 마스크로 브로드캐스트 주소 자동 계산

## 프로젝트 구조

```text
RemoteOn/
  app/
    src/main/
      AndroidManifest.xml
      java/com/kkamangnya/remoteon/
        MainActivity.kt
        MainViewModel.kt
        MainViewModelFactory.kt
        RemotePc.kt
        RemotePcAdapter.kt
        PcRepository.kt
        PcStore.kt
        PrefsCrypto.kt
        NetworkTools.kt
      res/
        layout/
          activity_main.xml
          item_remote_pc.xml
          dialog_remote_pc.xml
        values/
          colors.xml
          strings.xml
          themes.xml
  build.gradle
  settings.gradle
  gradle.properties
```

## Windows 설정

WoL이 실제로 동작하려면 Windows PC에서도 아래 설정이 필요합니다.

1. BIOS/UEFI에서 Wake-on-LAN 활성화
2. 네트워크 어댑터 전원 관리에서 `Allow this device to wake the computer` 활성화
3. 어댑터 고급 설정에서 `Wake on Magic Packet` 활성화
4. Fast Startup 비활성화가 필요한 경우 해제

## 빌드 방법

1. Android Studio에서 이 저장소를 연다.
2. JDK 17을 사용한다.
3. Gradle Sync 후 실행한다.
4. `copyDebugApk` 태스크를 실행하면 짧은 설치 파일이 `app/build/outputs/install/RemoteOn.apk`에 만들어진다.
5. 앱 상단의 테마 메뉴에서 시스템, 밝게, 어둡게를 직접 고를 수 있다.

## 구현 메모

- 온라인 체크는 ICMP 대신 TCP connect 테스트를 선택했다.
- 기본 포트는 RDP용 3389이며, 필요하면 `NetworkTools.kt`에서 바꿀 수 있다.
- 저장 암호화는 Android Keystore의 AES/GCM 예제를 사용한다.
- 소스 코드는 GitHub에 올라가 있고, 빌드 산출물은 `.gitignore`로 제외한다.
