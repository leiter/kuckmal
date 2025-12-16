import SwiftUI

struct ThemeListView: View {
    let themes: [String]
    @Binding var selectedTheme: String?
    let onThemeSelected: (String) -> Void

    var body: some View {
        ScrollView {
            LazyVStack(spacing: 16) {
                ForEach(themes, id: \.self) { theme in
                    ThemeButton(
                        theme: theme,
                        isSelected: selectedTheme == theme,
                        onTap: { onThemeSelected(theme) }
                    )
                }
            }
            .padding()
        }
    }
}

struct ThemeButton: View {
    let theme: String
    let isSelected: Bool
    let onTap: () -> Void

    @FocusState private var isFocused: Bool

    var body: some View {
        Button(action: onTap) {
            HStack {
                Rectangle()
                    .fill(Color.blue.opacity(0.6))
                    .frame(width: 6)
                    .cornerRadius(3)

                Text(theme)
                    .font(.title3)
                    .foregroundColor(isFocused ? .blue : .primary)

                Spacer()
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
    ThemeListView(
        themes: SampleData.allThemes,
        selectedTheme: .constant(nil),
        onThemeSelected: { _ in }
    )
}
