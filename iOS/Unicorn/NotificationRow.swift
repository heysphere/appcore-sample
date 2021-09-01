import SwiftUI

struct NotificationRow: View {
  var body: some View {
    HStack {
      Circle()
        .fill(Color.purple)
        .frame(width: 16, height: 16)

      VStack(alignment: .leading) {
        Text("owner/repository")
          .font(.caption)

        Text("This is a very long title")
          .font(.body)
      }

      Spacer()

      Text("#1234")
        .font(.caption)
    }
    .padding(.vertical, 8)
    .frame(maxWidth: .infinity, alignment: .leading)
  }
}

struct NotificationRow_Previews: PreviewProvider {
  static var previews: some View {
    ScrollView {
      NotificationRow()
        .padding()
    }

  }
}
