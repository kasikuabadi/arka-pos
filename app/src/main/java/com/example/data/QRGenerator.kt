package com.example.data

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

object QRGenerator {
    
    /**
     * Translates the JavaScript QRIS formulation into native Kotlin.
     * Takes the static QR template and appends dynamic amount (Tag 54) and signs with the CRC16 checksum.
     */
    fun generateQRISPayload(amount: Double): String {
        val rawStaticPayload = "00020101021126570011ID.DANA.WWW011893600915320019582002092001958200303UMI51440014ID.CO.QRIS.WWW0215ID10211270090610303UMI5204581253033605802ID5921WARUNG BAKSO MAS TOTO6011Kota Serang61054211463043C4F"
        val amtInt = amount.toInt()
        if (amtInt <= 0) return rawStaticPayload
        
        return try {
            val parts = rawStaticPayload.split("6304")
            var tubuhData = parts[0]
            
            // Ubah tipe static pembayaran (010211) menjadi dinamis (010212)
            tubuhData = tubuhData.replace("010211", "010212")
            
            val amtStr = amtInt.toString()
            val tag54Baru = "54" + String.format("%02d", amtStr.length) + amtStr
            val targetTag53 = "5303360"
            
            if (tubuhData.contains(targetTag53)) {
                tubuhData = tubuhData.replace(targetTag53, targetTag53 + tag54Baru)
            } else {
                return rawStaticPayload
            }
            
            val payloadToSign = tubuhData + "6304"
            val newCRC = makeCRC16(payloadToSign)
            tubuhData + "6304" + newCRC
        } catch (e: Exception) {
            rawStaticPayload
        }
    }

    private fun makeCRC16(str: String): String {
        var crc = 0xFFFF
        for (i in 0 until str.length) {
            val c = str[i].code
            crc = crc xor (c shl 8)
            for (j in 0 until 8) {
                if ((crc and 0x8000) != 0) {
                    crc = (crc shl 1) xor 0x1021
                } else {
                    crc = crc shl 1
                }
            }
        }
        crc = crc and 0xFFFF
        return String.format("%04X", crc)
    }

    /**
     * Generates a square Android Bitmap from the string payload.
     * Ready to be converted to ImageBitmap for Jetpack Compose.
     */
    fun generateQRCodeBitmap(text: String, width: Int = 400, height: Int = 400): Bitmap? {
        return try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, width, height)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            null
        }
    }
}
