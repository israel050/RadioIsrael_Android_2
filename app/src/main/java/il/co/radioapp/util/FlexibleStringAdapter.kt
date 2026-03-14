package il.co.radioapp.util

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter

/**
 * Gson TypeAdapter that reads both JSON strings and numbers as Kotlin String.
 * Needed because the stations JSON uses integer IDs for regular stations
 * (e.g. categories: [1, 2, 3]) but string IDs for KCM ("kcm-10").
 */
class FlexibleStringAdapter : TypeAdapter<String>() {
    override fun write(out: JsonWriter, value: String?) {
        if (value == null) out.nullValue() else out.value(value)
    }

    override fun read(reader: JsonReader): String? {
        return when (reader.peek()) {
            JsonToken.NULL   -> { reader.nextNull(); null }
            JsonToken.NUMBER -> {
                val raw = reader.nextString()       // read as string, no parsing
                raw
            }
            JsonToken.STRING -> reader.nextString()
            JsonToken.BOOLEAN-> reader.nextBoolean().toString()
            else             -> { reader.skipValue(); null }
        }
    }
}



