import AppCore
import SwiftUI

@main
struct UnicornApp: App {
  let sphereStore: SphereStore

  init() {
    ThrowableUtilsKt.installAppCoreUncaughtExceptionHandler()

    guard let token = ProcessInfo.processInfo.environment["gitHubAccessToken"] else {
      fatalError("GitHub Access token is not set in the environment")
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
