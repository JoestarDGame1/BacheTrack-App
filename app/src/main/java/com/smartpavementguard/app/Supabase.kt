package com.smartpavementguard.app

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import com.smartpavementguard.app.SupabaseManager
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest

object SupabaseManager {

    val client = createSupabaseClient(
        supabaseUrl = "https://jbalxjjzohyfvwgyvyof.supabase.co",
        supabaseKey = "sb_publishable_i56ZW1XnhmPLXZ_TLrmfXw_qxPa1yyE"
    ) {
        install(Postgrest)
    }

}