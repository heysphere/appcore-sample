import SwiftUI

struct NotificationRow: View {
  let caption: String
  let title: String
  let trailingLabel: String

  var body: some View {
    HStack {
      Circle()
        .fill(Color.purple)
        .frame(width: 16, height: 16)

      VStack(alignment: .leading) {
        Text(caption)
          .font(.caption)

        Text(title)
          .font(.body)
      }

      Spacer()

      Text(trailingLabel)
        .font(.caption)
    }
    .padding(.vertical, 8)
    .frame(maxWidth: .infinity, alignment: .leading)
  }
}

struct NotificationRow_Previews: PreviewProvider {
  static var previews: some View {
    ScrollView {
      NotificationRow(
        caption: "owner/repository",
        title: "This is a very long title",
        trailingLabel: "#1234"
      )
      .padding()
    }

  }
}
