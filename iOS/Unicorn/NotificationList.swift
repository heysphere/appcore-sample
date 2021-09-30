import SwiftUI
import AppCore
import Combine

final class NotificationListViewModel: ObservableObject {
  typealias State = PagingState<AppCoreObjC.Notification>
  @Published var notificationState = State(items: [], status: .loading)

  let sphereStore: SphereStore
  private var subscriptions = Set<AnyCancellable>()

  init(sphereStore: SphereStore) {
    self.sphereStore = sphereStore

    publisher(for: sphereStore.notificationListUseCase.notifications().state)
      .receive(on: DispatchQueue.main)
      .eraseToAnyPublisher()
      .assign(to: \.notificationState, on: self)
      .store(in: &subscriptions)
  }

  func next() {
    DispatchQueue.main.async {
      self.sphereStore.notificationListUseCase.notifications().next()
    }
  }
}

struct NotificationList: View {
  @State private var activeDetail: AppCoreObjC.Notification?
  @ObservedObject private var viewModel: NotificationListViewModel

  init(viewModel: NotificationListViewModel) {
    self.viewModel = viewModel
  }

  var body: some View {
    switch viewModel.notificationState.status {
    case .loading, .hasMore:
      ProgressView()
        .progressViewStyle(CircularProgressViewStyle())
    case .failed:
      Text("Failed to fetch details")
    case .endOfCollection where viewModel.notificationState.items.isEmpty:
      Text("No notifications")
    case .endOfCollection:
      List {
        ForEach(viewModel.notificationState.items) { notification in
          Button {
            activeDetail = notification
          } label: {
            NotificationRow(
              caption: notification.repositoryName,
              title: notification.title,
              trailingLabel: "#\(notification.subjectId)"
            )
          }
        }
        Button(action: loadMore) {
          Text("")
        }.hidden()
        .onAppear {
          DispatchQueue.global(qos: .background).asyncAfter(deadline: DispatchTime(uptimeNanoseconds: 10)) {
            self.loadMore()
          }
        }
      }
      .navigation($activeDetail) { info in
        NotificationInfo(
          viewModel: .init(
            sphereStore: viewModel.sphereStore,
            notificationId: info.notificationId
          )
        )
      }
      .navigationTitle(Text("Notifications"))
    default:
      Text("Unknown")
    }
  }

  private func loadMore() {
    viewModel.next()
  }
}

struct NotificationList_Previews: PreviewProvider {
  static var previews: some View {
    NavigationView {
      // TODO
//        NotificationList(
//            viewModel: .init(
//                useCase:
//            )
//        )
    }
  }
}

extension AppCoreObjC.Notification: Identifiable {}
