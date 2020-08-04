import com.google.gson.Gson

object GsonSerializer {
    val gson = Gson()
    fun toJson(obj: Any): String {
        return gson.toJson(obj)
    }
    inline fun<reified T:Any> fromJson(jsonString: String):T
    {
        return gson.fromJson(jsonString, T::class.java)
    }
}