import SwiftUI
import AppCore
import Combine

final class NotificationListViewModel: ObservableObject {
  typealias State = PagingState<AppCoreObjC.Notification>
  @Published var notificationState = State(items: [], status: .loading)

  var shouldShowAll = false {
    didSet {
      reload()
    }
  }

  let sphereStore: SphereStore
  private var dataSource: PagingDataSource<AppCoreObjC.Notification>!
  private var subscriptions = Set<AnyCancellable>()

  init(sphereStore: SphereStore) {
    self.sphereStore = sphereStore
    reload()
  }

  func next() {
    DispatchQueue.main.async {
      self.dataSource.next()
    }
  }

  func reload() {
    self.dataSource = sphereStore.notificationListUseCase
      .notifications(shouldShowAll: shouldShowAll)
    publisher(for: dataSource.state)
      .receive(on: DispatchQueue.main)
      .eraseToAnyPublisher()
      .sink { [weak self] state in
        self?.notificationState = state
      }
      .store(in: &subscriptions)
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
        .navigationBarItems(
          trailing: FilterToggle(isShowingRead: $viewModel.shouldShowAll)
        )
        .navigationBarTitle("Notifications")
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
      .navigationBarItems(
        trailing: FilterToggle(isShowingRead: $viewModel.shouldShowAll)
      )
      .navigationBarTitle("Notifications")
    default:
      Text("Unknown")
    }
  }

  private func loadMore() {
    viewModel.next()
  }
}

private struct FilterToggle: View {
  @Binding var isShowingRead: Bool

  var body: some View {
    Toggle("Show read", isOn: _isShowingRead)
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
