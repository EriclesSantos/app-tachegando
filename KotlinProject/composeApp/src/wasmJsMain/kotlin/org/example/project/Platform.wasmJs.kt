package org.example.project

class WasmPlatform : Platform {
    override val name: String = "Web (Wasm)"
    override val embedsHtmlMap = true
}

actual fun getPlatform(): Platform = WasmPlatform()
