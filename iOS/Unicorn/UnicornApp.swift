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

        let httpClient = AppCore.AgentHTTPClientImpl(defaultHeaders: [:], delegate: nil)
        httpClient.setEnvironment(
            environment: AppEnvironment(
                apiBaseUrl: "https://api.github.com",
                apiGqlWebSocketUrl: ""
            )
        )
        httpClient.setAuthToken(authToken: token)

        let logger = AppCore.Logger(
            isErrorEnabled: true,
            backend: AppCoreLoggingBackend()
        )
        let database = DefaultSqlDatabaseProvider(
            logger: logger,
            taskRunner: EmptyBackgroundTaskRunner()
        )
        let builder = AppCore.SphereStoreBuilder(
            storeActorBuilders: [
                Backend0StoreActorsBuilder(
                    httpClient: httpClient,
                    logger: logger
                )
            ],
            sqlDatabaseProvider: database,
            preferences: AppCorePreferenceStore(),
            connectivityMonitor: httpClient,
            logger: logger
        )

        self.sphereStore = builder.makeStore(gitHubAccessToken: token)
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
