package com.likeminds.feedsx.branding.model

import android.graphics.Color

// responsible for all the branding-related things like colors and fonts
object LMBranding {

    private var headerColor: String = "#FFFFFF"
    private var buttonsColor: String = "#5046E5"
    private var textLinkColor: String = "#007AFF"
    private var fonts: LMFonts? = null

    fun setBranding(setBrandingRequest: SetBrandingRequest) {
        this.headerColor = setBrandingRequest.headerColor
        this.buttonsColor = setBrandingRequest.buttonsColor
        this.textLinkColor = setBrandingRequest.textLinkColor
        this.fonts = setBrandingRequest.fonts
    }

    // returns button color
    fun getButtonsColor(): Int {
        return Color.parseColor(buttonsColor)
    }

    // returns header color
    fun getHeaderColor(): Int {
        return Color.parseColor(headerColor)
    }

    // returns text link color
    fun getTextLinkColor(): Int {
        return Color.parseColor(textLinkColor)
    }

    // returns toolbar color
    fun getToolbarColor(): Int {
        return if (headerColor == "#FFFFFF") {
            Color.BLACK
        } else {
            Color.WHITE
        }
    }

    // returns paths of the current fonts
    fun getCurrentFonts(): LMFonts? {
        return fonts
    }
}