package com.richardittou.qrcodenow.presentation.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.richardittou.qrcodenow.domain.model.AppTheme
import com.richardittou.qrcodenow.ui.theme.QRCodeNowTheme
import org.junit.Rule
import org.junit.Test

class CreditsScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun creatorCreditsAndLinksAreVisible() {
        composeRule.setContent { QRCodeNowTheme(AppTheme.LIGHT) { CreditsScreen(onBack = {}) } }
        composeRule.onNodeWithText("Criado por Richard Ittou").assertIsDisplayed()
        composeRule.onNodeWithText("Instagram").assertIsDisplayed()
        composeRule.onNodeWithText("GitHub").assertIsDisplayed()
    }
}
