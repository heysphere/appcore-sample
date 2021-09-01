import SwiftUI

@main
struct UnicornApp: App {
    var body: some Scene {
        WindowGroup {
          TabView {
            NavigationView {
              NotificationList()
            }
            .tabItem {
              Label("Notifications", systemImage: "sparkles")
            }
          }
        }
    }
}
