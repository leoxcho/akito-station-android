package app.akitostation.android
import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
class AppLogoUiTest {
 @get:Rule val compose = createAndroidComposeRule<MainActivity>()
 @Test fun everyLogoUpdatesPersistsAndRetainsOneLauncher() {
  compose.onNodeWithText("Settings").performClick()
  for(logo in AppLogo.entries) {
   compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(logo.label, substring = true))
   compose.onNodeWithText(logo.label, substring = true).performClick()
   compose.waitUntil(5000) { StationSettings(compose.activity).logo == logo }
   for(other in AppLogo.entries) {
    val component = ComponentName(compose.activity, "app.akitostation.android.Logo${other.name}")
    assertEquals(if(other == logo) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED, compose.activity.packageManager.getComponentEnabledSetting(component))
   }
   compose.activityRule.scenario.recreate()
   compose.waitUntil(5000) { compose.onAllNodesWithText(logo.label + " ✓").fetchSemanticsNodes().isNotEmpty() }
   assertEquals(logo, StationSettings(compose.activity).logo)
  }
 }
}
