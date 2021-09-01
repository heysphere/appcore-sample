import SwiftUI

@main
struct UnicornApp: App {
    var body: some Scene {
        WindowGroup {
          TabView {
            NotificationList()
          }
        }
    }
}
