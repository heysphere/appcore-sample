import AppCore
import SwiftUI

@main
struct UnicornApp: App {
  let sphereStore: SphereStore

  init() {
    ThrowableUtilsKt.installAppCoreUncaughtExceptionHandler()

    guard let token = Bundle.main.object(forInfoDictionaryKey: "GITHUB_ACCESS_TOKEN") as? String else {
      fatalError("GitHub Access token is not set in `Info.plist`")
    }

    self.sphereStore = SphereStoreBuilder.makeStore(gitHubAccessToken: token)
  }

  var body: some Scene {
    WindowGroup {
      TabView {
        NavigationView {
          NotificationList(
            viewModel: .init(
              sphereStore: sphereStore
            )
          )
        }
        .tabItem {
          Label("Notifications", systemImage: "sparkles")
        }
      }
    }
  }
}
