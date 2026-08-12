package com.uvrp.itsmantenimientoapp.helpers

import android.content.Intent
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import androidx.appcompat.app.ActionBarDrawerToggle
import com.uvrp.itsmantenimientoapp.BitacorasActivity
import com.uvrp.itsmantenimientoapp.HomeActivity
import com.uvrp.itsmantenimientoapp.MainActivity
import com.uvrp.itsmantenimientoapp.Nivel1Activity
import com.uvrp.itsmantenimientoapp.R
import com.uvrp.itsmantenimientoapp.SeleccionEquipoActivity
import com.uvrp.itsmantenimientoapp.TicketsActivity
import com.uvrp.itsmantenimientoapp.ReporteQrActivity
import com.uvrp.itsmantenimientoapp.iniciarPreoperacional
import com.uvrp.itsmantenimientoapp.IniciarCombustibleActivity
import com.uvrp.itsmantenimientoapp.InventarioCargarStockActivity
import com.uvrp.itsmantenimientoapp.InventarioConsultaUbicacionActivity
import com.uvrp.itsmantenimientoapp.InventarioSalidaActivity
import com.uvrp.itsmantenimientoapp.ExtrasActivity
import com.uvrp.itsmantenimientoapp.PpieFormatosActivity
import com.uvrp.itsmantenimientoapp.MeteoUfActivity
import com.uvrp.itsmantenimientoapp.ComprasSeguimientoActivity

object HeaderHelper {

    private var toggle: ActionBarDrawerToggle? = null

    fun setupHeader(
        activity: AppCompatActivity,
        drawerLayout: DrawerLayout,
        navView: NavigationView
    ) {
        val toolbar: Toolbar = activity.findViewById(R.id.toolbar_home)
            ?: run {
                // Si no se encuentra el toolbar, simplemente retornamos sin mostrar toast
                // Esto puede pasar si el layout no incluye el header_global correctamente
                return
            }

        activity.setSupportActionBar(toolbar)

        toggle = ActionBarDrawerToggle(
            activity,
            drawerLayout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        ).also {
            drawerLayout.addDrawerListener(it)
            it.syncState()
        }

        toolbar.menu.clear()
        toolbar.inflateMenu(R.menu.menu_toolbar)

        val sharedPreferences = activity.getSharedPreferences("Sesion", AppCompatActivity.MODE_PRIVATE)
        val idRol = sharedPreferences.getInt("idRol", -1)
        val nombreUsu = sharedPreferences.getString("nombre", null)?.trim().orEmpty()

        // Cabecera compacta del drawer (una sola vez por NavigationView)
        // fitsSystemWindows=false: el inset lo aplica el spacer del header (más fiable en drawers)
        navView.fitsSystemWindows = false
        if (navView.headerCount == 0) {
            navView.inflateHeaderView(R.layout.nav_header_drawer)
        }
        navView.getHeaderView(0)?.let { header ->
            ajustarSpacerBarraEstado(header)
            header.findViewById<TextView>(R.id.nav_header_nombre)?.text =
                if (nombreUsu.isNotEmpty()) nombreUsu else "Usuario"
            header.findViewById<TextView>(R.id.nav_header_rol)?.text = etiquetaRol(idRol)
        }

        // Visibilidad por permisos
        val menu = navView.menu
        val tienePermisosITS = (idRol == 1 || idRol == 2)
        val tienePermisosMantenimiento = (idRol == 1 || idRol == 5 || idRol == 6)
        val puedeInventario = sharedPreferences.getBoolean("puede_inventario", tienePermisosITS)
        val mostrarInventario = tienePermisosITS && puedeInventario
        val puedePpie = sharedPreferences.getBoolean("puede_ppie", false)
        val puedeComprasSeguimiento = sharedPreferences.getBoolean("puede_compras_seguimiento", idRol == 1)

        menu.findItem(R.id.nav_its).isVisible = tienePermisosITS
        menu.findItem(R.id.nav_correctivo).isVisible = tienePermisosITS
        menu.findItem(R.id.nav_bitacoras).isVisible = tienePermisosMantenimiento
        menu.findItem(R.id.nav_tickets).isVisible = tienePermisosITS
        menu.findItem(R.id.nav_reporte_qr).isVisible = tienePermisosITS
        menu.findItem(R.id.nav_inv_cargar_stock).isVisible = mostrarInventario
        menu.findItem(R.id.nav_inv_salida).isVisible = mostrarInventario
        menu.findItem(R.id.nav_inv_consulta_ubicacion).isVisible = mostrarInventario
        menu.findItem(R.id.nav_extras).isVisible = tienePermisosITS
        menu.findItem(R.id.nav_ppie)?.isVisible = puedePpie
        menu.findItem(R.id.nav_compras)?.isVisible = puedeComprasSeguimiento
        menu.findItem(R.id.nav_meteo)?.isVisible = true

        // Ocultar sección Inventario completa si no aplica
        for (i in 0 until menu.size()) {
            val top = menu.getItem(i)
            if (top.hasSubMenu() && top.title == activity.getString(R.string.drawer_seccion_inventario)) {
                top.isVisible = mostrarInventario
            }
        }

        // Listener del menú del toolbar (Cerrar Sesión)
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_cerrar_sesion -> {
                    logout(activity) // Usamos la función centralizada
                    true
                }
                else -> false
            }
        }

        // Listener del menú lateral (NavigationView)
        navView.setNavigationItemSelectedListener { item ->
            // 2. Se simplifica el listener: ya no necesitamos comprobar el rol aquí.
            //    Si el ítem es visible, es porque el usuario tiene permiso.
            when (item.itemId) {
                R.id.nav_its -> navigateTo(activity, Nivel1Activity::class.java)
                R.id.nav_home -> navigateTo(activity, HomeActivity::class.java)
                R.id.nav_correctivo -> navigateTo(activity, SeleccionEquipoActivity::class.java)
                R.id.nav_preoperacional -> navigateTo(activity, iniciarPreoperacional::class.java)
                R.id.nav_combustible -> navigateTo(activity, IniciarCombustibleActivity::class.java)
                R.id.nav_bitacoras -> navigateTo(activity, BitacorasActivity::class.java)
                R.id.nav_tickets -> navigateTo(activity, TicketsActivity::class.java)
                R.id.nav_reporte_qr -> navigateTo(activity, ReporteQrActivity::class.java)
                R.id.nav_inv_cargar_stock -> navigateTo(activity, InventarioCargarStockActivity::class.java)
                R.id.nav_inv_salida -> navigateTo(activity, InventarioSalidaActivity::class.java)
                R.id.nav_inv_consulta_ubicacion -> navigateTo(activity, InventarioConsultaUbicacionActivity::class.java)
                R.id.nav_extras -> navigateTo(activity, ExtrasActivity::class.java)
                R.id.nav_ppie -> navigateTo(activity, PpieFormatosActivity::class.java)
                R.id.nav_compras -> navigateTo(activity, ComprasSeguimientoActivity::class.java)
                R.id.nav_meteo -> navigateTo(activity, MeteoUfActivity::class.java)
                R.id.nav_cerrarsesion -> logout(activity) // Usamos la función centralizada
            }
            // Cierra el menú lateral después de la selección
            drawerLayout.closeDrawers()
            true
        }
    }

    fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return toggle?.onOptionsItemSelected(item) ?: false
    }

    // 3. Función centralizada para navegar, evitando código repetido.
    private fun navigateTo(activity: AppCompatActivity, destination: Class<*>) {
        activity.startActivity(Intent(activity, destination))
    }

    // 4. Función centralizada para cerrar sesión, evitando código duplicado.
    private fun logout(activity: AppCompatActivity) {
        activity.getSharedPreferences("Sesion", AppCompatActivity.MODE_PRIVATE).edit {
            clear()
        }
        val intent = Intent(activity, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        activity.startActivity(intent)
        activity.finish()
    }

    private fun etiquetaRol(idRol: Int): String = when (idRol) {
        1 -> "Administrador"
        2 -> "ITS"
        5, 6 -> "Mantenimiento"
        else -> "Sesión activa"
    }

    /** Reserva espacio bajo hora/batería; no depende de insets del NavigationView. */
    private fun ajustarSpacerBarraEstado(header: View) {
        val spacer = header.findViewById<View>(R.id.nav_header_status_spacer) ?: return
        val applyHeight: (Int) -> Unit = { topPx ->
            val h = if (topPx > 0) topPx else alturaBarraEstadoFallback(header)
            val lp = spacer.layoutParams
            if (lp.height != h) {
                lp.height = h
                spacer.layoutParams = lp
            }
        }

        // 1) Altura del sistema (siempre disponible)
        applyHeight(alturaBarraEstadoFallback(header))

        // 2) Refinar con insets reales cuando lleguen (cutouts / notch)
        ViewCompat.setOnApplyWindowInsetsListener(header) { _, insets ->
            val top = insets.getInsets(
                WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout()
            ).top
            applyHeight(top)
            insets
        }
        ViewCompat.requestApplyInsets(header)

        // También escuchar en el NavigationView padre por si el header no recibe insets
        (header.parent as? View)?.let { parent ->
            ViewCompat.setOnApplyWindowInsetsListener(parent) { _, insets ->
                val top = insets.getInsets(
                    WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout()
                ).top
                applyHeight(top)
                insets
            }
            ViewCompat.requestApplyInsets(parent)
        }
    }

    private fun alturaBarraEstadoFallback(view: View): Int {
        val res = view.resources
        val id = res.getIdentifier("status_bar_height", "dimen", "android")
        if (id > 0) {
            return res.getDimensionPixelSize(id)
        }
        return (28f * res.displayMetrics.density).toInt()
    }
}