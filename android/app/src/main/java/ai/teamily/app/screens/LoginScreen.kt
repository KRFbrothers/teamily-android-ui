package ai.teamily.app.screens

import ai.teamily.app.Api
import ai.teamily.app.ApiException
import ai.teamily.app.R
import ai.teamily.app.ui.PrimaryButton
import ai.teamily.app.ui.T
import ai.teamily.app.ui.tc
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(api: Api) {
    var registering by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun submit() {
        busy = true
        error = null
        scope.launch {
            try {
                if (registering) api.register(email.trim(), name.trim(), password) else api.login(email.trim(), password)
            } catch (e: ApiException) {
                error = e.message
            } finally {
                busy = false
            }
        }
    }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().imePadding().padding(24.dp),
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(painterResource(R.drawable.teamily_mark), contentDescription = null, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(12.dp))
        Text(if (registering) "Create your account" else "Sign in to Teamily AI", style = T.title, color = tc.ink)
        Spacer(Modifier.height(24.dp))
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AuthField("Email", email, KeyboardType.Email) { email = it }
            if (registering) AuthField("Name", name, KeyboardType.Text) { name = it }
            AuthField("Password", password, KeyboardType.Password, hidden = true) { password = it }
            error?.let { Text(it, style = T.rowSub, color = tc.danger) }
            PrimaryButton(
                if (busy) "Please wait…" else if (registering) "Register" else "Sign in",
                onClick = { if (!busy) submit() }, modifier = Modifier.fillMaxWidth(),
            )
            Text(
                if (registering) "Have an account? Sign in" else "New here? Create an account",
                style = T.rowSub, color = tc.primaryInk,
                modifier = Modifier.align(Alignment.CenterHorizontally).clickable { registering = !registering; error = null }.padding(8.dp),
            )
        }
    }
}

@Composable
private fun AuthField(label: String, value: String, type: KeyboardType, hidden: Boolean = false, onChange: (String) -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column {
        Text(label, style = T.caption, color = tc.ink3, modifier = Modifier.padding(bottom = 4.dp))
        BasicTextField(
            value = value, onValueChange = onChange, singleLine = true,
            textStyle = T.body.copy(color = tc.ink), cursorBrush = SolidColor(tc.primary),
            keyboardOptions = KeyboardOptions(keyboardType = type),
            visualTransformation = if (hidden) PasswordVisualTransformation() else VisualTransformation.None,
            modifier = Modifier.fillMaxWidth().clip(shape).background(tc.field).border(1.dp, tc.fieldEdge, shape).padding(16.dp),
        )
    }
}
