// ARMOR-ANDROID-CONTROL - reading text from JSON the way a person means it.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor.model

import org.json.JSONArray
import org.json.JSONObject

/**
 * The text of a field, with a JSON null (or a missing field) read as the fallback. On Android `JSONObject.optString` turns a JSON null into the four letters "null" (the copy of org.json
 * the unit tests use gives an empty text, which is why the tests never saw it): a voice answer with `"confirmation_token": null` asked for a confirmation, a release with `"body": null`
 * showed "null" as its news. The server writes null for what it does not know, so every read of a text goes through here.
 */
fun JSONObject.optStr(key: String, fallback: String = ""): String = if (!has(key) || isNull(key)) fallback else optString(key, fallback)

/** The text at a position of an array, with a JSON null read as an empty text. */
fun JSONArray.optStr(index: Int): String = if (index < 0 || index >= length() || isNull(index)) "" else optString(index)
