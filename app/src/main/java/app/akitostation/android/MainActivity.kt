package app.akitostation.android

import android.os.Bundle
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.activity.viewModels
import androidx.compose.runtime.mutableStateOf

class MainActivity : ComponentActivity() {
 private val model: StationViewModel by viewModels()
 private val controllerName = mutableStateOf("Touch / keyboard")
 private var lastAxis = 0L
 override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT), navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
  val delegate = window.callback
  window.callback = object : android.view.Window.Callback by delegate {
   override fun dispatchKeyEvent(event: KeyEvent): Boolean {
    val mapped = if(event.device?.supportsSource(InputDevice.SOURCE_GAMEPAD) == true) when(event.keyCode) { KeyEvent.KEYCODE_BUTTON_A -> KeyEvent.KEYCODE_DPAD_CENTER; KeyEvent.KEYCODE_BUTTON_B -> KeyEvent.KEYCODE_BACK; else -> null } else null
    return delegate.dispatchKeyEvent(if(mapped == null) event else KeyEvent(event.downTime, event.eventTime, event.action, mapped, event.repeatCount, event.metaState, event.deviceId, event.scanCode, event.flags, InputDevice.SOURCE_DPAD))
   }
  }
  setContent { StationApp(model, controllerName.value) } }
 override fun onResume() { super.onResume(); model.refresh(); controllerName.value = InputDevice.getDeviceIds().toList().mapNotNull(InputDevice::getDevice).firstOrNull { it.supportsSource(InputDevice.SOURCE_GAMEPAD) || it.supportsSource(InputDevice.SOURCE_JOYSTICK) }?.name ?: "Touch / keyboard" }
 override fun onGenericMotionEvent(event: MotionEvent): Boolean {
  if(event.isFromSource(InputDevice.SOURCE_JOYSTICK) && event.action == MotionEvent.ACTION_MOVE) {
   val code = ControllerInput.direction(event.getAxisValue(MotionEvent.AXIS_X), event.getAxisValue(MotionEvent.AXIS_Y), event.getAxisValue(MotionEvent.AXIS_HAT_X), event.getAxisValue(MotionEvent.AXIS_HAT_Y))
   if(code != null && event.eventTime - lastAxis >= 180) {
    lastAxis = event.eventTime
    window.callback.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code)); window.callback.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code)); return true
   }
  }
  return super.onGenericMotionEvent(event)
 }
}
object ControllerInput {
 fun direction(x: Float, y: Float, hatX: Float = 0f, hatY: Float = 0f): Int? {
  val dx = if(kotlin.math.abs(hatX) > .5f) hatX else x
  val dy = if(kotlin.math.abs(hatY) > .5f) hatY else y
  return when { dx < -.6f -> KeyEvent.KEYCODE_DPAD_LEFT; dx > .6f -> KeyEvent.KEYCODE_DPAD_RIGHT; dy < -.6f -> KeyEvent.KEYCODE_DPAD_UP; dy > .6f -> KeyEvent.KEYCODE_DPAD_DOWN; else -> null }
 }
}
