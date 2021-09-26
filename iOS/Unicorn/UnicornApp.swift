import AppCore
import SwiftUI

@main
struct UnicornApp: App {
    let sphereStore: SphereStore

    init() {
        let httpClient = AppCore.AgentHTTPClientImpl(defaultHeaders: [:], delegate: nil)

        let logger = AppCore.Logger(
            isErrorEnabled: true,
            backend: AppCoreLoggingBackend()
        )
        let database = DefaultSqlDatabaseProvider(
            logger: logger,
            taskRunner: UIApplication.shared
        )
        let builder = AppCore.SphereStoreBuilder(
            environmentType: .production,
            storeActorBuilders: [],
            sqlDatabaseProvider: database,
            preferences: AppCorePreferenceStore(),
            connectivityMonitor: httpClient,
            logger: logger
        )
        self.sphereStore = builder.makeStore(gitHubAccessToken: "123") // TODO
    }

    var body: some Scene {
        WindowGroup {
          TabView {
            NavigationView {
              NotificationList(
                viewModel: .init(
                    useCase: sphereStore.notificationListUseCase
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
