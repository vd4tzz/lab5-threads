package com.example.threadslite.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.threadslite.ui.MainViewModel
import com.example.threadslite.util.UiState

@Composable
fun AuthScreen(vm: MainViewModel) {
    var isRegisterMode by remember { mutableStateOf(false) }
    var email          by remember { mutableStateOf("") }
    var password       by remember { mutableStateOf("") }
    var username       by remember { mutableStateOf("") }

    val authState by vm.authState.collectAsStateWithLifecycle()
    val isLoading = authState is UiState.Loading
    val errorMsg  = (authState as? UiState.Error)?.message

    LaunchedEffect(isRegisterMode) { vm.resetAuthState() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .padding(horizontal = 28.dp)
    ) {
        Spacer(Modifier.height(64.dp))

        Text(
            text       = "Threads\nLite.",
            fontSize   = 44.sp,
            fontWeight = FontWeight.Black,
            lineHeight = 50.sp,
            color      = Color.Black
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text     = if (isRegisterMode) "Create your account" else "Welcome back",
            fontSize = 15.sp,
            color    = Color(0xFF9E9E9E)
        )

        Spacer(Modifier.height(48.dp))

        AnimatedVisibility(visible = isRegisterMode) {
            Column {
                AuthField(
                    value         = username,
                    onValueChange = { username = it },
                    label         = "Username",
                    enabled       = !isLoading
                )
                Spacer(Modifier.height(24.dp))
            }
        }

        AuthField(
            value           = email,
            onValueChange   = { email = it },
            label           = "Email",
            enabled         = !isLoading,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction    = ImeAction.Next
            )
        )
        Spacer(Modifier.height(24.dp))
        AuthField(
            value           = password,
            onValueChange   = { password = it },
            label           = "Password",
            enabled         = !isLoading,
            isPassword      = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction    = ImeAction.Done
            )
        )

        AnimatedVisibility(visible = errorMsg != null) {
            Text(
                text     = errorMsg ?: "",
                color    = MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 10.dp)
            )
        }

        Spacer(Modifier.height(44.dp))

        Button(
            onClick = {
                if (isRegisterMode) vm.register(email, password, username)
                else vm.login(email, password)
            },
            enabled  = !isLoading,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape    = RoundedCornerShape(50.dp),
            colors   = ButtonDefaults.buttonColors(
                containerColor         = Color.Black,
                contentColor           = Color.White,
                disabledContainerColor = Color(0xFFDDDDDD),
                disabledContentColor   = Color(0xFF9E9E9E)
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color       = Color.White,
                    modifier    = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text          = if (isRegisterMode) "Create account" else "Sign in",
                    fontWeight    = FontWeight.Bold,
                    fontSize      = 15.sp,
                    letterSpacing = 0.3.sp
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        TextButton(
            onClick  = {
                isRegisterMode = !isRegisterMode
                email = ""; password = ""; username = ""
            },
            enabled  = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text     = if (isRegisterMode) "Already have an account? Sign in"
                           else "New here? Create account",
                fontSize = 14.sp,
                color    = Color(0xFF757575)
            )
        }
    }
}

@Composable
private fun AuthField(
    value:           String,
    onValueChange:   (String) -> Unit,
    label:           String,
    enabled:         Boolean,
    isPassword:      Boolean         = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    TextField(
        value                = value,
        onValueChange        = onValueChange,
        label                = { Text(label, fontSize = 13.sp) },
        enabled              = enabled,
        singleLine           = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions      = keyboardOptions,
        modifier             = Modifier.fillMaxWidth(),
        colors               = TextFieldDefaults.colors(
            focusedContainerColor   = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor  = Color.Transparent,
            focusedIndicatorColor   = Color.Black,
            unfocusedIndicatorColor = Color(0xFFDDDDDD),
            disabledIndicatorColor  = Color(0xFFEEEEEE),
            cursorColor             = Color.Black,
            focusedLabelColor       = Color.Black,
            unfocusedLabelColor     = Color(0xFF9E9E9E)
        )
    )
}
