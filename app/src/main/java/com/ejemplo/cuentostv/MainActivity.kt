package com.ejemplo.cuentostv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.ejemplo.cuentostv.ui.CuentosApp
import com.ejemplo.cuentostv.ui.theme.CuentosTvTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CuentosTvTheme {
                CuentosApp()
            }
        }
    }
}
