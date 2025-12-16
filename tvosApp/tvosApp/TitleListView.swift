import SwiftUI

struct TitleListView: View {
    let titles: [String]
    @Binding var selectedTitle: String?
    let onTitleSelected: (String) -> Void

    var body: some View {
        ScrollView {
            LazyVStack(spacing: 16) {
                ForEach(titles, id: \.self) { title in
                    TitleButton(
                        title: title,
                        isSelected: selectedTitle == title,
                        onTap: { onTitleSelected(title) }
                    )
                }
            }
            .padding()
        }
    }
}

struct TitleButton: View {
    let title: String
    let isSelected: Bool
    let onTap: () -> Void

    @FocusState private var isFocused: Bool

    var body: some View {
        Button(action: onTap) {
            HStack {
                Rectangle()
                    .fill(isSelected ? Color.blue : Color.gray.opacity(0.5))
                    .frame(width: 6)
                    .cornerRadius(3)

                Text(title)
                    .font(.title3)
                    .foregroundColor(isFocused ? .blue : .primary)

                Spacer()

                Image(systemName: "play.circle.fill")
                    .font(.title2)
                    .foregroundColor(isFocused ? .blue : .gray)
            }
            .padding()
            .frame(maxWidth: .infinity)
            .background(
                RoundedRectangle(cornerRadius: 12)
                    .fill(isFocused ? Color.white.opacity(0.2) : Color.gray.opacity(0.1))
            )
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(isSelected ? Color.blue : Color.clear, lineWidth: 3)
            )
            .scaleEffect(isFocused ? 1.02 : 1.0)
            .animation(.easeInOut(duration: 0.2), value: isFocused)
        }
        .buttonStyle(.plain)
        .focused($isFocused)
    }
}

#Preview {
    TitleListView(
        titles: SampleData.getTitlesForTheme("Tatort"),
        selectedTitle: .constant(nil),
        onTitleSelected: { _ in }
    )
}
