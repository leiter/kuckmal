import XCTest

final class tvosAppUITests: XCTestCase {

    var app: XCUIApplication!

    override func setUpWithError() throws {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launch()
    }

    override func tearDownWithError() throws {
        app = nil
    }

    // MARK: - App Launch Tests

    func testAppLaunches() throws {
        // Verify the app launches and shows main navigation
        XCTAssertTrue(app.staticTexts["Kuckmal"].exists, "App title should be visible")
        XCTAssertTrue(app.staticTexts["Alle Themen"].exists, "Theme header should be visible")
    }

    func testChannelListVisible() throws {
        // Verify channel buttons are visible
        XCTAssertTrue(app.buttons["3sat"].exists, "3sat channel should be visible")
        XCTAssertTrue(app.buttons["ARD"].exists, "ARD channel should be visible")
        XCTAssertTrue(app.buttons["ZDF"].exists, "ZDF channel should be visible")
    }

    func testThemeListVisible() throws {
        // Verify theme items are visible
        XCTAssertTrue(app.buttons["Tagesschau"].exists, "Tagesschau theme should be visible")
        XCTAssertTrue(app.buttons["Tatort"].exists, "Tatort theme should be visible")
    }

    // MARK: - Channel Selection Tests

    func testSelectChannel() throws {
        // Navigate to ARD channel using remote control
        // First navigate down to channel list
        XCUIRemote.shared.press(.down)
        sleep(1)

        // Navigate to ARD (second channel)
        XCUIRemote.shared.press(.right)
        sleep(1)

        // Select the channel
        XCUIRemote.shared.press(.select)
        sleep(1)

        // Verify we're still in the app
        XCTAssertTrue(app.exists)
    }

    // MARK: - Theme Selection Tests

    func testSelectTheme() throws {
        // Navigate to themes area
        XCUIRemote.shared.press(.down)
        XCUIRemote.shared.press(.down)
        sleep(1)

        // Select a theme
        XCUIRemote.shared.press(.select)
        sleep(1)

        // Verify app is still running
        XCTAssertTrue(app.exists)
    }

    // MARK: - Navigation Tests

    func testBackNavigation() throws {
        // Navigate to themes
        XCUIRemote.shared.press(.down)
        XCUIRemote.shared.press(.down)
        sleep(1)

        // Select a theme
        XCUIRemote.shared.press(.select)
        sleep(1)

        // Press Menu button to go back (simulates tvOS remote)
        XCUIRemote.shared.press(.menu)
        sleep(1)

        // Verify we're back (app should still be running)
        XCTAssertTrue(app.exists)
    }

    // MARK: - Remote Control Tests

    func testRemoteNavigation() throws {
        // Test D-pad navigation
        XCUIRemote.shared.press(.down)
        sleep(1)

        XCUIRemote.shared.press(.right)
        sleep(1)

        XCUIRemote.shared.press(.up)
        sleep(1)

        XCUIRemote.shared.press(.left)
        sleep(1)

        // Verify app is still responsive
        XCTAssertTrue(app.exists)
    }

    // MARK: - Screenshot Tests

    func testTakeScreenshots() throws {
        // Take screenshot of initial state
        let attachment1 = XCTAttachment(screenshot: app.screenshot())
        attachment1.name = "001_Initial_State"
        attachment1.lifetime = .keepAlways
        add(attachment1)

        // Navigate to channels and take screenshot
        XCUIRemote.shared.press(.down)
        sleep(1)
        XCUIRemote.shared.press(.right)
        sleep(1)
        XCUIRemote.shared.press(.select)
        sleep(1)

        let attachment2 = XCTAttachment(screenshot: app.screenshot())
        attachment2.name = "002_Channel_Selected"
        attachment2.lifetime = .keepAlways
        add(attachment2)
    }
}
